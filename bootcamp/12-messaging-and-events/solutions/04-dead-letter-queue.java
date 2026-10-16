// ─────────────────────────────────────────────────────────────────────────
//  04 · retries and a dead-letter queue — SOLUTION          ★★☆ core
//  concepts: retries · poison messages · dead-letter queues · redrive
//  run: java -cp ../../_lib/out 04-dead-letter-queue.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A "poison message" is one that fails every time: bad JSON, a reference
//  to a deleted row, a bug that only that payload triggers. Retried in
//  place at the head of the queue, it blocks everything behind it
//  forever. Two rules fix that, and SQS, RabbitMQ and every job framework
//  (Sidekiq, Celery, Hangfire) implement both: a failed message goes to
//  the BACK of the queue so others get their turn, and after maxAttempts
//  it moves to a dead-letter queue where a human (or an alert) looks at
//  it. SQS calls the limit maxReceiveCount.
//  The attempt count belongs to the queued ENTRY, not to the message
//  text and not to the queue: a small Entry(msg, attempts) record is
//  enough. Counting attempts in a Map<String, Integer> keyed by the text
//  breaks the moment the same text is submitted twice.
//  Off-by-one is the classic bug: with maxAttempts = 3 the handler runs
//  three times and the third failure dead-letters (attempts == max), not
//  the fourth. maxAttempts = 1 means "no retries at all".
//  Keep the LAST error in the dead letter: the first error may have been
//  a transient timeout, while the last is what finally gave up.
//  redrive is the operator's tool after a fix is deployed: replay the
//  dead letters with fresh counters. Without resetting the count, a
//  redriven message that hits one more blip goes straight back to the
//  dead letters.

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
            if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be at least 1: " + maxAttempts);
            this.maxAttempts = maxAttempts;
        }

        void submit(String msg) {
            queue.addLast(new Entry(msg, 0));
        }

        String processNext(Consumer<String> handler) {
            var entry = queue.pollFirst();
            if (entry == null) throw new NoSuchElementException("the queue is empty");
            try {
                handler.accept(entry.msg());
                return "ok " + entry.msg();
            } catch (RuntimeException e) {
                int attempts = entry.attempts() + 1;
                if (attempts < maxAttempts) {
                    queue.addLast(new Entry(entry.msg(), attempts));
                    return "retry " + entry.msg() + " (attempt " + attempts + ")";
                }
                dead.add(new Dead(entry.msg(), attempts, e.getMessage()));
                return "dead " + entry.msg() + " after " + attempts + " attempts";
            }
        }

        List<String> drain(Consumer<String> handler) {
            var results = new ArrayList<String>();
            while (!queue.isEmpty()) results.add(processNext(handler));
            return results;
        }

        List<Dead> deadLetters() {
            return Collections.unmodifiableList(dead);
        }

        int redrive() {
            int moved = dead.size();
            for (var d : dead) queue.addLast(new Entry(d.message(), 0));
            dead.clear();
            return moved;
        }

        int pending() {
            return queue.size();
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
