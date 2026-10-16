// ─────────────────────────────────────────────────────────────────────────
//  04 · retry with backoff and jitter — SOLUTION            ★★☆ core
//  concepts: exponential backoff · jitter · retryable errors · overflow
//  run: java -cp ../../_lib/out 04-retry-backoff.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Retrying is how clients survive blips, and how they turn a struggling
//  server into a dead one. Three rules keep retries polite:
//  1. Back off exponentially (100, 200, 400 ms ...) with a cap, so a
//     server that is down sees fewer and fewer requests, not a constant
//     hammering.
//  2. Add jitter. Without it, every client that failed at the same moment
//     retries at the same moment: the "thundering herd" comes back in
//     waves. Full jitter (uniform in [0, cap]) is what the AWS
//     Architecture Blog found works best; it is one nextLong(cap + 1).
//  3. Only retry what can succeed next time. A timeout may; a 400 Bad
//     Request never will, so it is thrown immediately.
//  Counting is where implementations go wrong: maxAttempts counts
//  attempts, not retries, so 3 attempts means at most 2 sleeps, and there
//  is no sleep after the final failure (that would only delay the error).
//  Earlier failures are attached with addSuppressed so the log shows the
//  whole story, not just the last line.
//  Java trap: base << (retry - 1) or base * 2^n silently overflows into a
//  negative number around retry 57, and sleep(-x) either throws or skips
//  the wait. Doubling with an "already over half the cap?" check cannot.
//  The sleeper and jitter are injected so the tests never actually wait.

import static bootcamp.Check.*;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.IntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.Predicate;

class Exercise {

    interface Sleeper {
        void sleep(long millis);
    }

    static final class Retry {
        private final int maxAttempts;
        private final long baseDelayMillis, maxDelayMillis;
        private final LongUnaryOperator jitter;
        private final Sleeper sleeper;

        Retry(int maxAttempts, long baseDelayMillis, long maxDelayMillis, LongUnaryOperator jitter, Sleeper sleeper) {
            if (maxAttempts < 1) throw new IllegalArgumentException("need at least one attempt: " + maxAttempts);
            if (baseDelayMillis < 1 || maxDelayMillis < baseDelayMillis) {
                throw new IllegalArgumentException("need 1 <= base <= max: " + baseDelayMillis + ", " + maxDelayMillis);
            }
            this.maxAttempts = maxAttempts;
            this.baseDelayMillis = baseDelayMillis;
            this.maxDelayMillis = maxDelayMillis;
            this.jitter = jitter;
            this.sleeper = sleeper;
        }

        long backoff(int retry) {
            if (retry < 1) throw new IllegalArgumentException("retries are numbered from 1: " + retry);
            long delay = baseDelayMillis;
            for (int i = 1; i < retry; i++) {
                if (delay > maxDelayMillis / 2) return maxDelayMillis;
                delay *= 2;
            }
            return Math.min(delay, maxDelayMillis);
        }

        <T> T call(Callable<T> op, Predicate<Exception> retryable) throws Exception {
            var failures = new ArrayList<Exception>();
            for (int attempt = 1; ; attempt++) {
                try {
                    return op.call();
                } catch (Exception e) {
                    if (!retryable.test(e) || attempt == maxAttempts) {
                        for (var earlier : failures) e.addSuppressed(earlier);
                        throw e;
                    }
                    failures.add(e);
                    sleeper.sleep(jitter.applyAsLong(backoff(attempt)));
                }
            }
        }

        static LongUnaryOperator fullJitter(Random random) {
            return cap -> random.nextLong(cap + 1);
        }
    }

    /** Fails the first `failures` calls with `error.apply(n)`, then returns "ok". */
    static final class Flaky implements Callable<String> {
        final int failures;
        final IntFunction<Exception> error;
        int attempts;

        Flaky(int failures, IntFunction<Exception> error) {
            this.failures = failures;
            this.error = error;
        }

        public String call() throws Exception {
            attempts++;
            if (attempts <= failures) throw error.apply(attempts);
            return "ok";
        }
    }

    static final class Timeout extends Exception {
        Timeout(String message) {
            super(message);
        }
    }

    static final Predicate<Exception> ON_TIMEOUT = e -> e instanceof Timeout;

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        test("returns at once when the first attempt works", () -> {
            var sleeps = new ArrayList<Long>();
            var retry = new Retry(3, 100, 10_000, d -> d, sleeps::add);
            var op = new Flaky(0, n -> new Timeout("fail " + n));
            eq(retry.call(op, ON_TIMEOUT), "ok");
            eq(op.attempts, 1);
            eq(sleeps, List.of());
        });
        test("retries a retryable failure with doubling delays", () -> {
            var sleeps = new ArrayList<Long>();
            var retry = new Retry(4, 100, 10_000, d -> d, sleeps::add);
            var op = new Flaky(3, n -> new Timeout("fail " + n));
            eq(retry.call(op, ON_TIMEOUT), "ok");
            eq(op.attempts, 4);
            eq(sleeps, List.of(100L, 200L, 400L));
        });
        test("gives up after maxAttempts without a final sleep", () -> {
            var sleeps = new ArrayList<Long>();
            var retry = new Retry(3, 100, 10_000, d -> d, sleeps::add);
            var op = new Flaky(99, n -> new Timeout("fail " + n));
            raises(Timeout.class, () -> retry.call(op, ON_TIMEOUT), "fail 3");
            eq(op.attempts, 3);
            eq(sleeps, List.of(100L, 200L));
        });
        test("the final failure carries the earlier ones as suppressed", () -> {
            var retry = new Retry(3, 100, 10_000, d -> d, ms -> { });
            var op = new Flaky(99, n -> new Timeout("fail " + n));
            var ex = raises(Timeout.class, () -> retry.call(op, ON_TIMEOUT));
            var earlier = new ArrayList<String>();
            for (var s : ex.getSuppressed()) earlier.add(s.getMessage());
            eq(earlier, List.of("fail 1", "fail 2"));
        });
        test("a non-retryable failure is thrown at once", () -> {
            var sleeps = new ArrayList<Long>();
            var retry = new Retry(5, 100, 10_000, d -> d, sleeps::add);
            var op = new Flaky(1, n -> new IllegalArgumentException("bad request"));
            raises(IllegalArgumentException.class, () -> retry.call(op, ON_TIMEOUT), "bad request");
            eq(op.attempts, 1);
            eq(sleeps, List.of());
        });
        test("backoff doubles up to the cap and never overflows", () -> {
            var retry = new Retry(3, 100, 1_000, d -> d, ms -> { });
            eq(List.of(retry.backoff(1), retry.backoff(2), retry.backoff(4), retry.backoff(5)),
                List.of(100L, 200L, 800L, 1_000L));
            eq(retry.backoff(70), 1_000);
            var huge = new Retry(3, 100, Long.MAX_VALUE, d -> d, ms -> { });
            eq(huge.backoff(62), Long.MAX_VALUE);
            eq(huge.backoff(200), Long.MAX_VALUE);
            raises(IllegalArgumentException.class, () -> retry.backoff(0));
        });
        test("jitter decides each actual sleep", () -> {
            var sleeps = new ArrayList<Long>();
            var retry = new Retry(3, 100, 10_000, d -> d / 2, sleeps::add);
            retry.call(new Flaky(2, n -> new Timeout("fail " + n)), ON_TIMEOUT);
            eq(sleeps, List.of(50L, 100L));
        });
        test("full jitter is seeded and stays within [0, cap]", () -> {
            var jitter = Retry.fullJitter(new Random(42));
            var reference = new Random(42);
            for (long cap : new long[] {1_000, 1, 0, 50, 7_000}) {
                long d = jitter.applyAsLong(cap);
                eq(d, reference.nextLong(cap + 1));
                ok(d >= 0 && d <= cap, "within [0, cap]");
            }
        });
        test("refuses bad settings", () -> {
            raises(IllegalArgumentException.class, () -> new Retry(0, 100, 1_000, d -> d, ms -> { }));
            raises(IllegalArgumentException.class, () -> new Retry(3, 0, 1_000, d -> d, ms -> { }));
            raises(IllegalArgumentException.class, () -> new Retry(3, 500, 100, d -> d, ms -> { }));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
