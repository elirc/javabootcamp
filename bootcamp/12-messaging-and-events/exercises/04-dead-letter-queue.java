// ─────────────────────────────────────────────────────────────────────────
//  04 · retries and a dead-letter queue                     ★★☆ core
//  concepts: retries · poison messages · dead-letter queues · redrive
//  run: java -cp ../../_lib/out 04-dead-letter-queue.java
// ─────────────────────────────────────────────────────────────────────────
//
//  One malformed message must not wedge a queue forever. Finish the nested
//  class RetryQueue(maxAttempts) (maxAttempts < 1 →
//  IllegalArgumentException). submit(msg) appends to the queue.
//  processNext(handler) takes the head (empty queue →
//  NoSuchElementException) and calls handler.accept(msg). Success returns
//  "ok <msg>". A throw counts one attempt: below maxAttempts the message
//  goes to the BACK of the queue and the result is "retry <msg> (attempt
//  N)"; at maxAttempts it moves to the dead-letter list as Dead(msg,
//  attempts, the exception's message) and the result is "dead <msg> after
//  N attempts". drain(handler) calls processNext until the queue is empty
//  and returns the results in order. deadLetters() is read-only.
//  redrive() moves every dead letter, in order, back to the end of the
//  queue with a fresh attempt count, empties the list, and returns how
//  many moved. pending() is the queue length.
//
//      maxAttempts 2, handler fails on "p":   submit(p) submit(x)
//      drain(h) → [retry p (attempt 1), ok x, dead p after 2 attempts]
//
//  hint: a retry re-queues a NEW Entry carrying attempts + 1.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    record Dead(String message, int attempts, String lastError) {}

    static final class RetryQueue {
        private record Entry(String msg, int attempts) {}

        private final int maxAttempts;
        private final ArrayDeque<Entry> queue = new ArrayDeque<>();
        private final List<Dead> dead = new ArrayList<>();

        RetryQueue(int maxAttempts) {
            throw new UnsupportedOperationException("TODO");
        }

        void submit(String msg) {
            throw new UnsupportedOperationException("TODO");
        }

        String processNext(Consumer<String> handler) {
            throw new UnsupportedOperationException("TODO");
        }

        List<String> drain(Consumer<String> handler) {
            throw new UnsupportedOperationException("TODO");
        }

        List<Dead> deadLetters() {
            throw new UnsupportedOperationException("TODO");
        }

        int redrive() {
            throw new UnsupportedOperationException("TODO");
        }

        int pending() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    /** A handler that fails every message in `poison`, numbering its errors "boom 1", "boom 2"... */
    static Consumer<String> failing(Set<String> poison) {
        var count = new int[1];
        return msg -> {
            if (poison.contains(msg)) throw new IllegalStateException("boom " + ++count[0]);
        };
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("a message that succeeds is done after one attempt", () -> {
            var q = new RetryQueue(3);
            q.submit("a");
            eq(q.processNext(failing(Set.of())), "ok a");
            eq(q.pending(), 0);
        });
        test("a failure goes to the back of the queue", () -> {
            var q = new RetryQueue(3);
            q.submit("a");
            q.submit("b");
            var flaky = new HashSet<>(Set.of("a"));
            Consumer<String> handler = msg -> {
                if (flaky.remove(msg)) throw new IllegalStateException("timeout");
            };
            eq(q.drain(handler), List.of("retry a (attempt 1)", "ok b", "ok a"));
        });
        test("gives up after maxAttempts and keeps the last error", () -> {
            var q = new RetryQueue(3);
            q.submit("a");
            eq(q.drain(failing(Set.of("a"))),
               List.of("retry a (attempt 1)", "retry a (attempt 2)", "dead a after 3 attempts"));
            eq(q.deadLetters(), List.of(new Dead("a", 3, "boom 3")));
        });
        test("a poison message does not block the others", () -> {
            var q = new RetryQueue(2);
            for (var m : List.of("p", "x", "y")) q.submit(m);
            eq(q.drain(failing(Set.of("p"))),
               List.of("retry p (attempt 1)", "ok x", "ok y", "dead p after 2 attempts"));
        });
        test("maxAttempts 1 means no retries", () -> {
            var q = new RetryQueue(1);
            q.submit("p");
            eq(q.processNext(failing(Set.of("p"))), "dead p after 1 attempts");
            eq(q.pending(), 0);
        });
        test("the same text submitted twice is two messages", () -> {
            var q = new RetryQueue(2);
            q.submit("p");
            q.submit("p");
            eq(q.drain(failing(Set.of("p"))),
               List.of("retry p (attempt 1)", "retry p (attempt 1)",
                       "dead p after 2 attempts", "dead p after 2 attempts"));
        });
        test("redrive replays dead letters with fresh attempt counts", () -> {
            var q = new RetryQueue(2);
            q.submit("p");
            q.submit("q");
            q.drain(failing(Set.of("p", "q")));
            eq(q.redrive(), 2);
            eq(q.deadLetters(), List.of());
            eq(q.pending(), 2);
            eq(q.processNext(failing(Set.of("p"))), "retry p (attempt 1)");
            eq(q.processNext(failing(Set.of())), "ok q");
        });
        test("refuses bad settings and an empty queue", () -> {
            raises(IllegalArgumentException.class, () -> new RetryQueue(0));
            var q = new RetryQueue(2);
            raises(NoSuchElementException.class, () -> q.processNext(failing(Set.of())));
            eq(q.drain(failing(Set.of())), List.of());
        });
        test("dead letters are read-only", () -> {
            var q = new RetryQueue(1);
            q.submit("p");
            q.drain(failing(Set.of("p")));
            raises(UnsupportedOperationException.class, () -> q.deadLetters().clear());
            eq(q.deadLetters().size(), 1);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
