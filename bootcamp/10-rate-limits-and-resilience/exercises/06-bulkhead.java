// ─────────────────────────────────────────────────────────────────────────
//  06 · bulkhead                                            ★★☆ core
//  concepts: bulkheads · concurrency permits · bounded queues · fail fast
//  run: java -cp ../../_lib/out 06-bulkhead.java
// ─────────────────────────────────────────────────────────────────────────
//
//  One slow dependency must not eat every worker. Finish Bulkhead, a
//  model of "at most maxConcurrent calls at once, at most maxQueue
//  waiting" (no real threads: tasks are ids). submit(id) returns RUNNING
//  if a permit is free, else QUEUED if the queue has room, else REJECTED
//  (and the id is not remembered). finish(id) on a running task frees its
//  permit and promotes the OLDEST queued task, returning its id (empty if
//  nobody waited); on a queued task it cancels it and promotes nobody.
//  running() lists running ids in the order they started; queued() lists
//  waiters oldest first. IllegalArgumentException for: submitting an id
//  that is running or queued, finishing an unknown id, maxConcurrent < 1,
//  maxQueue < 0 (a queue of 0 is allowed).
//
//      Bulkhead(1, 2): submit t1 → RUNNING, t2 → QUEUED, t3 → QUEUED,
//                      t4 → REJECTED;  finish(t1) → Optional[t2]
//
//  hint: a free permit and a waiting task should never exist together.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    enum Admission { RUNNING, QUEUED, REJECTED }

    static final class Bulkhead {
        private final int maxConcurrent, maxQueue;
        private final Set<String> running = new LinkedHashSet<>();
        private final Deque<String> queue = new ArrayDeque<>();

        Bulkhead(int maxConcurrent, int maxQueue) {
            throw new UnsupportedOperationException("TODO");
        }

        Admission submit(String taskId) {
            throw new UnsupportedOperationException("TODO");
        }

        Optional<String> finish(String taskId) {
            throw new UnsupportedOperationException("TODO");
        }

        List<String> running() {
            throw new UnsupportedOperationException("TODO");
        }

        List<String> queued() {
            throw new UnsupportedOperationException("TODO");
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
