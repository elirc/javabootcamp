// ─────────────────────────────────────────────────────────────────────────
//  06 · bulkhead — SOLUTION                                 ★★☆ core
//  concepts: bulkheads · concurrency permits · bounded queues · fail fast
//  run: java -cp ../../_lib/out 06-bulkhead.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A ship's bulkheads keep one flooded compartment from sinking the ship.
//  In a service, the flood is a slow dependency: every request thread
//  ends up blocked on it and the requests that never touch it starve
//  too. A bulkhead gives each dependency its own fixed number of permits
//  (the size of a thread pool or a Semaphore) and a small waiting room.
//  The three outcomes are the whole design:
//    RUNNING   a permit was free
//    QUEUED    no permit, but the bounded queue has room (FIFO)
//    REJECTED  both full: fail fast, remember nothing
//  The bound on the queue is the point. An unbounded queue is not a
//  bulkhead, it is a memory leak with latency: work waits longer than the
//  caller is willing to wait, then runs anyway for nobody. maxQueue = 0 is
//  legal and common: "run now or refuse".
//  finish() keeps the invariant "a free permit and a waiting task never
//  coexist": when a running task ends, the OLDEST waiter is promoted in
//  the same step. When a queued task is cancelled, no permit was freed,
//  so nobody is promoted (the classic bug promotes anyway and runs
//  maxConcurrent + 1 tasks). A LinkedHashSet keeps start order and gives
//  O(1) membership; an ArrayDeque is the FIFO queue.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    enum Admission { RUNNING, QUEUED, REJECTED }

    static final class Bulkhead {
        private final int maxConcurrent, maxQueue;
        private final Set<String> running = new LinkedHashSet<>();
        private final Deque<String> queue = new ArrayDeque<>();

        Bulkhead(int maxConcurrent, int maxQueue) {
            if (maxConcurrent < 1) throw new IllegalArgumentException("need at least one permit: " + maxConcurrent);
            if (maxQueue < 0) throw new IllegalArgumentException("queue size cannot be negative: " + maxQueue);
            this.maxConcurrent = maxConcurrent;
            this.maxQueue = maxQueue;
        }

        Admission submit(String taskId) {
            if (running.contains(taskId) || queue.contains(taskId)) {
                throw new IllegalArgumentException("already submitted: " + taskId);
            }
            if (running.size() < maxConcurrent) {
                running.add(taskId);
                return Admission.RUNNING;
            }
            if (queue.size() < maxQueue) {
                queue.addLast(taskId);
                return Admission.QUEUED;
            }
            return Admission.REJECTED;
        }

        Optional<String> finish(String taskId) {
            if (running.remove(taskId)) {
                var next = queue.pollFirst();
                if (next != null) running.add(next);
                return Optional.ofNullable(next);
            }
            if (queue.remove(taskId)) return Optional.empty();
            throw new IllegalArgumentException("unknown task: " + taskId);
        }

        List<String> running() {
            return List.copyOf(running);
        }

        List<String> queued() {
            return List.copyOf(queue);
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("runs up to maxConcurrent tasks", () -> {
            var b = new Bulkhead(2, 1);
            eq(b.submit("t1"), Admission.RUNNING);
            eq(b.submit("t2"), Admission.RUNNING);
            eq(b.running(), List.of("t1", "t2"));
        });
        test("then queues up to maxQueue, then rejects", () -> {
            var b = new Bulkhead(1, 2);
            b.submit("t1");
            eq(b.submit("t2"), Admission.QUEUED);
            eq(b.submit("t3"), Admission.QUEUED);
            eq(b.submit("t4"), Admission.REJECTED);
            eq(b.queued(), List.of("t2", "t3"));
        });
        test("finishing promotes the oldest waiter", () -> {
            var b = new Bulkhead(1, 2);
            b.submit("t1");
            b.submit("t2");
            b.submit("t3");
            eq(b.finish("t1"), Optional.of("t2"));
            eq(b.running(), List.of("t2"));
            eq(b.queued(), List.of("t3"));
        });
        test("finishing with nobody waiting frees a permit", () -> {
            var b = new Bulkhead(2, 1);
            b.submit("t2");
            b.submit("t3");
            eq(b.finish("t2"), Optional.empty());
            eq(b.submit("t1"), Admission.RUNNING);
            eq(b.running(), List.of("t3", "t1"));
        });
        test("cancelling a queued task promotes nobody", () -> {
            var b = new Bulkhead(1, 2);
            b.submit("t1");
            b.submit("t2");
            b.submit("t3");
            eq(b.finish("t3"), Optional.empty());
            eq(b.running(), List.of("t1"));
            eq(b.queued(), List.of("t2"));
            eq(b.submit("t4"), Admission.QUEUED);
        });
        test("a rejected task is not remembered", () -> {
            var b = new Bulkhead(1, 0);
            b.submit("t1");
            eq(b.submit("t2"), Admission.REJECTED);
            raises(IllegalArgumentException.class, () -> b.finish("t2"));
            b.finish("t1");
            eq(b.submit("t2"), Admission.RUNNING);
        });
        test("refuses duplicate and unknown task ids", () -> {
            var b = new Bulkhead(1, 1);
            b.submit("t1");
            b.submit("t2");
            raises(IllegalArgumentException.class, () -> b.submit("t1"));
            raises(IllegalArgumentException.class, () -> b.submit("t2"));
            raises(IllegalArgumentException.class, () -> b.finish("nope"));
            eq(b.queued(), List.of("t2"));
        });
        test("refuses bad settings, but a zero queue is fine", () -> {
            raises(IllegalArgumentException.class, () -> new Bulkhead(0, 1));
            raises(IllegalArgumentException.class, () -> new Bulkhead(1, -1));
            eq(new Bulkhead(1, 0).submit("t1"), Admission.RUNNING);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
