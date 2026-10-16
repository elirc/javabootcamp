// ─────────────────────────────────────────────────────────────────────────
//  05 · the transactional outbox                            ★★☆ core
//  concepts: dual writes · outbox pattern · relays · at-least-once publishing
//  run: java -cp ../../_lib/out 05-transactional-outbox.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Saving an order and publishing "OrderPlaced" are two writes, and either
//  can fail alone. Finish the nested class OrderService. placeOrder(id,
//  cents) stores the order AND appends the event "OrderPlaced <id>
//  <cents>" to its outbox, as one unit: a known id or cents < 1 →
//  IllegalArgumentException and neither is written. relay(publisher,
//  batchSize) sends the oldest unpublished events, at most batchSize, in
//  order, through publisher.accept(event), marking each published only
//  after accept returns. If accept throws, relay stops right there (the
//  event stays unpublished, the exception does not escape) and returns
//  how many it published this call. batchSize < 1 →
//  IllegalArgumentException. pending() lists unpublished events, oldest
//  first. purgePublished() deletes published rows and returns how many.
//
//      placeOrder("o1", 500)   pending() → ["OrderPlaced o1 500"]
//      relay(broker::send, 10) → 1        pending() → []
//      a broker that is down:  relay(...) → 0, the event stays pending
//
//  hint: a Row holds an event and a published flag; never remove on send.

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
            throw new UnsupportedOperationException("TODO");
        }

        int relay(Consumer<String> publisher, int batchSize) {
            throw new UnsupportedOperationException("TODO");
        }

        List<String> pending() {
            throw new UnsupportedOperationException("TODO");
        }

        int purgePublished() {
            throw new UnsupportedOperationException("TODO");
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
