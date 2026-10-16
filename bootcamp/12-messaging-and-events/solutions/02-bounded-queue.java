// ─────────────────────────────────────────────────────────────────────────
//  02 · a bounded queue with backpressure — SOLUTION        ★☆☆ warm-up
//  concepts: backpressure · overflow policies · ArrayDeque · load shedding
//  run: java -cp ../../_lib/out 02-bounded-queue.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  An unbounded queue does not remove overload, it postpones it until the
//  heap runs out, and meanwhile every message waits longer. A bounded
//  queue forces the choice that exists anyway: when full, who loses?
//  REJECT is backpressure proper: the producer is told "not now" and keeps
//  the message, so nothing is lost but the producer must slow down or
//  buffer upstream (TCP windows, a full BlockingQueue, HTTP 429/503).
//  DROP_OLDEST keeps the freshest data and suits telemetry and live
//  prices, where a newer reading supersedes an old one. DROP_NEWEST
//  protects work already accepted and suits best-effort streams such as
//  debug logs. Akka Streams, Reactor's onBackpressureDrop/Latest and
//  Kafka producers' buffer.memory are this same menu.
//  Counting matters as much as the policy: rejected() and dropped() are
//  the metrics that tell an operator the system is shedding load, and
//  they are different things (a rejected message still exists; a dropped
//  one is gone). highWater() is the classic queue-depth gauge; a
//  high-water mark that sits at capacity means the consumer is too slow.
//  The usual bug is ordering: under DROP_OLDEST, pollFirst() the head
//  BEFORE offerLast(msg), or the queue briefly holds capacity + 1 and the
//  high-water mark lies.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    enum Policy { REJECT, DROP_OLDEST, DROP_NEWEST }

    static final class BoundedQueue {
        private final int capacity;
        private final Policy policy;
        private final ArrayDeque<String> items = new ArrayDeque<>();
        private int rejected;
        private int dropped;
        private int highWater;

        BoundedQueue(int capacity, Policy policy) {
            if (capacity < 1) throw new IllegalArgumentException("capacity must be at least 1: " + capacity);
            this.capacity = capacity;
            this.policy = policy;
        }

        boolean offer(String msg) {
            if (items.size() == capacity) {
                switch (policy) {
                    case REJECT -> {
                        rejected++;
                        return false;
                    }
                    case DROP_NEWEST -> {
                        dropped++;
                        return false;
                    }
                    case DROP_OLDEST -> {
                        items.pollFirst();
                        dropped++;
                    }
                }
            }
            items.offerLast(msg);
            highWater = Math.max(highWater, items.size());
            return true;
        }

        Optional<String> poll() {
            return Optional.ofNullable(items.pollFirst());
        }

        List<String> pollBatch(int max) {
            if (max < 1) throw new IllegalArgumentException("max must be at least 1: " + max);
            var out = new ArrayList<String>();
            while (out.size() < max && !items.isEmpty()) out.add(items.pollFirst());
            return out;
        }

        int size() {
            return items.size();
        }

        int rejected() {
            return rejected;
        }

        int dropped() {
            return dropped;
        }

        int highWater() {
            return highWater;
        }
    }

    static BoundedQueue full(Policy policy) {
        var q = new BoundedQueue(2, policy);
        q.offer("a");
        q.offer("b");
        return q;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("hands messages out in arrival order", () -> {
            var q = new BoundedQueue(3, Policy.REJECT);
            eq(q.offer("a"), true);
            eq(q.offer("b"), true);
            eq(q.poll(), Optional.of("a"));
            eq(q.poll(), Optional.of("b"));
            eq(q.poll(), Optional.empty());
        });
        test("REJECT refuses when full and keeps what it has", () -> {
            var q = full(Policy.REJECT);
            eq(q.offer("c"), false);
            eq(q.rejected(), 1);
            eq(q.dropped(), 0);
            eq(q.pollBatch(5), List.of("a", "b"));
        });
        test("REJECT accepts again once there is room", () -> {
            var q = full(Policy.REJECT);
            q.offer("c");
            q.poll();
            eq(q.offer("c"), true);
            eq(q.pollBatch(5), List.of("b", "c"));
        });
        test("DROP_OLDEST throws away the head to make room", () -> {
            var q = full(Policy.DROP_OLDEST);
            eq(q.offer("c"), true);
            eq(q.offer("d"), true);
            eq(q.dropped(), 2);
            eq(q.rejected(), 0);
            eq(q.pollBatch(5), List.of("c", "d"));
        });
        test("DROP_NEWEST throws away the incoming message", () -> {
            var q = full(Policy.DROP_NEWEST);
            eq(q.offer("c"), false);
            eq(q.dropped(), 1);
            eq(q.rejected(), 0);
            eq(q.pollBatch(5), List.of("a", "b"));
        });
        test("pollBatch takes at most max, oldest first", () -> {
            var q = new BoundedQueue(5, Policy.REJECT);
            for (var m : List.of("a", "b", "c")) q.offer(m);
            eq(q.pollBatch(2), List.of("a", "b"));
            eq(q.size(), 1);
            eq(q.pollBatch(2), List.of("c"));
            eq(q.pollBatch(2), List.of());
            raises(IllegalArgumentException.class, () -> q.pollBatch(0));
        });
        test("the high-water mark is the peak size, never above capacity", () -> {
            var q = full(Policy.DROP_OLDEST);
            q.offer("c");
            q.pollBatch(2);
            q.offer("d");
            eq(q.highWater(), 2);
            eq(q.size(), 1);
            var roomy = new BoundedQueue(5, Policy.REJECT);
            for (var m : List.of("a", "b", "c")) roomy.offer(m);
            eq(roomy.highWater(), 3);
        });
        test("refuses a capacity below one", () -> {
            raises(IllegalArgumentException.class, () -> new BoundedQueue(0, Policy.REJECT));
            eq(new BoundedQueue(1, Policy.DROP_NEWEST).offer("x"), true);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
