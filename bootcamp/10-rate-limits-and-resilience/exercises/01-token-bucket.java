// ─────────────────────────────────────────────────────────────────────────
//  01 · token bucket                                        ★☆☆ warm-up
//  concepts: token bucket · bursts vs sustained rate · lazy refill · clock
//  run: java -cp ../../_lib/out 01-token-bucket.java
// ─────────────────────────────────────────────────────────────────────────
//
//  "10 requests per second, bursts of up to 3." Finish TokenBucket. It
//  starts full (capacity tokens, at the clock's time when it is built)
//  and gains refillPerSecond tokens per second continuously, fractions
//  included, never holding more than capacity. tryAcquire(n) takes n
//  tokens and returns true, or returns false and takes NOTHING if fewer
//  than n are there. available() returns the current (refilled) count
//  without taking any. The clock is a LongSupplier of milliseconds.
//  IllegalArgumentException for n < 1, n > capacity, capacity < 1, or a
//  refill rate that is not positive.
//
//      capacity 3, 10/s: 3 x tryAcquire(1) → true; 4th → false
//      100 ms later: tryAcquire(1) → true  (1 token came back)
//      capacity 1, 2/s, empty: +250 ms → false;  +250 ms more → true
//
//  hint: refill lazily on every call, from the time since the last one.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.LongSupplier;

class Exercise {

    static final class TokenBucket {
        private final long capacity;
        private final double refillPerSecond;
        private final LongSupplier clock;
        private double tokens;
        private long lastRefill;

        TokenBucket(long capacity, double refillPerSecond, LongSupplier clock) {
            throw new UnsupportedOperationException("TODO");
        }

        boolean tryAcquire(int n) {
            throw new UnsupportedOperationException("TODO");
        }

        double available() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class FakeClock implements LongSupplier {
        long now;

        public long getAsLong() {
            return now;
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("starts full and allows a burst up to capacity", () -> {
            var b = new TokenBucket(3, 1, new FakeClock());
            eq(b.tryAcquire(1), true);
            eq(b.tryAcquire(1), true);
            eq(b.tryAcquire(1), true);
            eq(b.tryAcquire(1), false);
        });
        test("refills at the configured rate", () -> {
            var clock = new FakeClock();
            var b = new TokenBucket(3, 10, clock);
            b.tryAcquire(3);
            clock.now = 100;
            eq(b.tryAcquire(1), true);
            eq(b.tryAcquire(1), false);
            clock.now = 300;
            eq(b.tryAcquire(2), true);
            eq(b.tryAcquire(1), false);
        });
        test("never refills beyond capacity", () -> {
            var clock = new FakeClock();
            var b = new TokenBucket(3, 10, clock);
            b.tryAcquire(3);
            clock.now = 60_000;
            approx(b.available(), 3.0, 1e-9);
            eq(b.tryAcquire(3), true);
            eq(b.tryAcquire(1), false);
        });
        test("fractions of a token are not lost", () -> {
            var clock = new FakeClock();
            var b = new TokenBucket(1, 2, clock);
            b.tryAcquire(1);
            clock.now = 250;
            eq(b.tryAcquire(1), false);
            clock.now = 500;
            eq(b.tryAcquire(1), true);
        });
        test("available shows the refill without taking anything", () -> {
            var clock = new FakeClock();
            var b = new TokenBucket(5, 2, clock);
            b.tryAcquire(5);
            clock.now = 1_250;
            approx(b.available(), 2.5, 1e-9);
            approx(b.available(), 2.5, 1e-9);
        });
        test("a refused request takes nothing", () -> {
            var b = new TokenBucket(5, 1, new FakeClock());
            eq(b.tryAcquire(4), true);
            eq(b.tryAcquire(3), false);
            approx(b.available(), 1.0, 1e-9);
            eq(b.tryAcquire(1), true);
        });
        test("starts full relative to its creation time", () -> {
            var clock = new FakeClock();
            clock.now = 5_000;
            var b = new TokenBucket(2, 1, clock);
            approx(b.available(), 2.0, 1e-9);
            b.tryAcquire(2);
            clock.now = 5_999;
            eq(b.tryAcquire(1), false);
            clock.now = 6_000;
            eq(b.tryAcquire(1), true);
        });
        test("refuses impossible requests and bad settings", () -> {
            var clock = new FakeClock();
            var b = new TokenBucket(3, 1, clock);
            raises(IllegalArgumentException.class, () -> b.tryAcquire(0));
            raises(IllegalArgumentException.class, () -> b.tryAcquire(4));
            raises(IllegalArgumentException.class, () -> new TokenBucket(0, 1, clock));
            raises(IllegalArgumentException.class, () -> new TokenBucket(3, 0, clock));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
