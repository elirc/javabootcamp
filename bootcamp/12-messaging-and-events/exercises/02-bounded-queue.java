// ─────────────────────────────────────────────────────────────────────────
//  02 · a bounded queue with backpressure                   ★☆☆ warm-up
//  concepts: backpressure · overflow policies · ArrayDeque · load shedding
//  run: java -cp ../../_lib/out 02-bounded-queue.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A producer can always outrun a consumer, so a real buffer has a limit
//  and a policy for when it is full. Finish the nested class
//  BoundedQueue(capacity, policy) (capacity < 1 →
//  IllegalArgumentException). offer(msg) returns whether msg is now in the
//  queue. When the queue is full: REJECT refuses msg (false, counted by
//  rejected(): the producer still holds it and must slow down);
//  DROP_OLDEST throws away the head and accepts msg (true); DROP_NEWEST
//  throws msg away (false). Thrown-away messages are counted by dropped().
//  poll() takes the head (Optional.empty() when empty). pollBatch(max)
//  takes up to max messages in FIFO order (max < 1 →
//  IllegalArgumentException). highWater() is the largest size reached.
//
//      capacity 2, DROP_OLDEST:  offer(a) offer(b) offer(c)  → queue [b, c]
//      capacity 2, REJECT:       offer(c)                   → false
//      pollBatch(5) on [b, c]                               → [b, c]
//
//  hint: ArrayDeque gives you both ends; the policy only matters when full.

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
            throw new UnsupportedOperationException("TODO");
        }

        boolean offer(String msg) {
            throw new UnsupportedOperationException("TODO");
        }

        Optional<String> poll() {
            throw new UnsupportedOperationException("TODO");
        }

        List<String> pollBatch(int max) {
            throw new UnsupportedOperationException("TODO");
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
        });
        test("refuses a capacity below one", () -> {
            raises(IllegalArgumentException.class, () -> new BoundedQueue(0, Policy.REJECT));
            eq(new BoundedQueue(1, Policy.DROP_NEWEST).offer("x"), true);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
