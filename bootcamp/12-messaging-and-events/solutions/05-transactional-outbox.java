// ─────────────────────────────────────────────────────────────────────────
//  05 · the transactional outbox — SOLUTION                 ★★☆ core
//  concepts: dual writes · outbox pattern · relays · at-least-once publishing
//  run: java -cp ../../_lib/out 05-transactional-outbox.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  The dual-write problem: "save to the database, then publish to the
//  broker" loses the event when the process dies between the two, and
//  "publish, then save" announces orders that never existed. No ordering
//  of two independent writes is safe. The transactional outbox makes it
//  one write: the event is a row in the SAME database transaction as the
//  order, so both exist or neither does. A separate relay (a poller, or
//  change-data-capture such as Debezium) reads the outbox and publishes.
//  The relay's rule is publish first, mark second. If it crashes between
//  the two, the event is sent again on restart: at-least-once, which the
//  consumers' dedupe (exercise 03) absorbs. Marking first would be
//  at-most-once: a crash after marking loses the event forever, which is
//  the bug the outbox exists to remove.
//  Stopping at the first failure keeps events in order. Skipping a failed
//  event and sending the next would let "OrderShipped" overtake
//  "OrderPlaced" downstream. The next relay starts again from the oldest
//  pending row, so the failed one is retried first.
//  Validation happens before either write, which is what "one unit"
//  means here; in SQL it is BEGIN ... COMMIT around both INSERTs.
//  purgePublished is the housekeeping job: outbox tables grow forever
//  unless published rows are deleted, but only published ones.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    static final class Row {
        final String event;
        boolean published;

        Row(String event) {
            this.event = event;
        }
    }

    static final class OrderService {
        private final Map<String, Long> orders = new LinkedHashMap<>();
        private final List<Row> outbox = new ArrayList<>();

        void placeOrder(String id, long cents) {
            if (orders.containsKey(id)) throw new IllegalArgumentException("order exists: " + id);
            if (cents < 1) throw new IllegalArgumentException("amount must be positive: " + cents);
            orders.put(id, cents);
            outbox.add(new Row("OrderPlaced " + id + " " + cents));
        }

        int relay(Consumer<String> publisher, int batchSize) {
            if (batchSize < 1) throw new IllegalArgumentException("batchSize must be at least 1: " + batchSize);
            int sent = 0;
            for (var row : outbox) {
                if (row.published) continue;
                if (sent == batchSize) break;
                try {
                    publisher.accept(row.event);
                } catch (RuntimeException e) {
                    break;
                }
                row.published = true;
                sent++;
            }
            return sent;
        }

        List<String> pending() {
            var out = new ArrayList<String>();
            for (var row : outbox) if (!row.published) out.add(row.event);
            return out;
        }

        int purgePublished() {
            int before = outbox.size();
            outbox.removeIf(row -> row.published);
            return before - outbox.size();
        }

        Map<String, Long> orders() {
            return Collections.unmodifiableMap(orders);
        }
    }

    static OrderService withOrders(String... ids) {
        var s = new OrderService();
        for (var id : ids) s.placeOrder(id, 100);
        return s;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("an order and its event are written together", () -> {
            var s = new OrderService();
            s.placeOrder("o1", 500);
            eq(s.orders(), Map.of("o1", 500L));
            eq(s.pending(), List.of("OrderPlaced o1 500"));
        });
        test("a rejected order writes neither the order nor the event", () -> {
            var s = withOrders("o1");
            raises(IllegalArgumentException.class, () -> s.placeOrder("o1", 900));
            raises(IllegalArgumentException.class, () -> s.placeOrder("o2", 0));
            eq(s.orders(), Map.of("o1", 100L));
            eq(s.pending(), List.of("OrderPlaced o1 100"));
        });
        test("relay publishes pending events in order and marks them", () -> {
            var s = withOrders("o1", "o2");
            var sent = new ArrayList<String>();
            eq(s.relay(sent::add, 10), 2);
            eq(sent, List.of("OrderPlaced o1 100", "OrderPlaced o2 100"));
            eq(s.pending(), List.of());
        });
        test("relay sends at most batchSize events per call", () -> {
            var s = withOrders("o1", "o2", "o3");
            var sent = new ArrayList<String>();
            eq(s.relay(sent::add, 2), 2);
            eq(s.pending(), List.of("OrderPlaced o3 100"));
            eq(s.relay(sent::add, 2), 1);
            eq(sent.size(), 3);
        });
        test("a failed publish stops the batch and stays pending", () -> {
            var s = withOrders("o1", "o2", "o3");
            var sent = new ArrayList<String>();
            Consumer<String> flaky = e -> {
                if (e.contains("o2")) throw new IllegalStateException("broker down");
                sent.add(e);
            };
            eq(s.relay(flaky, 10), 1);
            eq(sent, List.of("OrderPlaced o1 100"));
            eq(s.pending(), List.of("OrderPlaced o2 100", "OrderPlaced o3 100"));
        });
        test("the next relay retries from the failed event, in order", () -> {
            var s = withOrders("o1", "o2", "o3");
            s.relay(e -> { if (e.contains("o2")) throw new IllegalStateException("down"); }, 10);
            var sent = new ArrayList<String>();
            eq(s.relay(sent::add, 10), 2);
            eq(sent, List.of("OrderPlaced o2 100", "OrderPlaced o3 100"));
        });
        test("published events are never sent again", () -> {
            var s = withOrders("o1");
            s.relay(e -> {}, 10);
            var sent = new ArrayList<String>();
            eq(s.relay(sent::add, 10), 0);
            eq(sent, List.of());
        });
        test("purge deletes only published rows", () -> {
            var s = withOrders("o1", "o2", "o3");
            s.relay(e -> {}, 2);
            eq(s.purgePublished(), 2);
            eq(s.purgePublished(), 0);
            eq(s.pending(), List.of("OrderPlaced o3 100"));
            var sent = new ArrayList<String>();
            s.relay(sent::add, 10);
            eq(sent, List.of("OrderPlaced o3 100"));
        });
        test("refuses a batch size below one", () ->
            raises(IllegalArgumentException.class, () -> withOrders("o1").relay(e -> {}, 0)));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
