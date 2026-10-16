// ─────────────────────────────────────────────────────────────────────────
//  04 · retry with backoff and jitter                       ★★☆ core
//  concepts: exponential backoff · jitter · retryable errors · overflow
//  run: java -cp ../../_lib/out 04-retry-backoff.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Finish Retry. call(op, retryable) runs op up to maxAttempts times in
//  total. A failure that retryable rejects is rethrown at once. After
//  failed attempt k (if another attempt remains) it sleeps
//  jitter.applyAsLong(backoff(k)) via the injected Sleeper; there is no
//  sleep after the last attempt. When attempts run out, the last failure
//  is thrown with the earlier ones added as suppressed, oldest first.
//  backoff(k) = min(max, base * 2^(k-1)) for k >= 1, never overflowing
//  (k < 1 → IllegalArgumentException). fullJitter(random) returns
//  cap -> random.nextLong(cap + 1). IllegalArgumentException for
//  maxAttempts < 1, base < 1, or max < base.
//
//      base 100, max 10000, no jitter, fails 3x then ok, 4 attempts
//        → sleeps [100, 200, 400], returns "ok"
//      base 100, max 1000: backoff(5) → 1000, backoff(70) → 1000
//
//  hint: stop doubling once the delay passes half the cap.

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
            throw new UnsupportedOperationException("TODO");
        }

        long backoff(int retry) {
            throw new UnsupportedOperationException("TODO");
        }

        <T> T call(Callable<T> op, Predicate<Exception> retryable) throws Exception {
            throw new UnsupportedOperationException("TODO");
        }

        static LongUnaryOperator fullJitter(Random random) {
            throw new UnsupportedOperationException("TODO");
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
