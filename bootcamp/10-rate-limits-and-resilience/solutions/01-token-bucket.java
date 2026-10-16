// ─────────────────────────────────────────────────────────────────────────
//  01 · token bucket — SOLUTION                             ★☆☆ warm-up
//  concepts: token bucket · bursts vs sustained rate · lazy refill · clock
//  run: java -cp ../../_lib/out 01-token-bucket.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  The bucket encodes two limits in two numbers: capacity is the largest
//  burst, refillPerSecond is the sustained rate. Nobody drips tokens in on
//  a timer; refill is computed lazily from the elapsed time whenever the
//  bucket is asked, which costs O(1) and needs no thread.
//  Two traps live in that one line of arithmetic:
//  1. Capping: after a long idle period the bucket holds capacity tokens,
//     not elapsed * rate. Forget Math.min and a client that slept for an
//     hour gets an hour of traffic in one burst.
//  2. Fractions: at 2 tokens/s, 250 ms is half a token. Integer math
//     (elapsed * rate / 1000) rounds it to 0 and then moves "last" to now,
//     so the half token is thrown away and a client polling every 250 ms
//     is starved forever. Keep a double, or advance last only by the time
//     you actually converted into tokens.
//  A refused request takes nothing: partial grants would make a 3-token
//  request silently cost 1. And a request larger than capacity can never
//  succeed, so it is a caller bug (IllegalArgumentException), not false.
//  This is the algorithm behind AWS API Gateway throttling, Guava's
//  RateLimiter (a close cousin) and most "N requests/s, burst M" configs.

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
            if (capacity < 1) throw new IllegalArgumentException("capacity must be at least 1: " + capacity);
            if (!(refillPerSecond > 0)) throw new IllegalArgumentException("refill rate must be positive: " + refillPerSecond);
            this.capacity = capacity;
            this.refillPerSecond = refillPerSecond;
            this.clock = clock;
            this.tokens = capacity;
            this.lastRefill = clock.getAsLong();
        }

        boolean tryAcquire(int n) {
            if (n < 1 || n > capacity) throw new IllegalArgumentException("cannot ever take " + n + " tokens");
            refill();
            if (tokens < n) return false;
            tokens -= n;
            return true;
        }

        double available() {
            refill();
            return tokens;
        }

        private void refill() {
            long now = clock.getAsLong();
            tokens = Math.min(capacity, tokens + (now - lastRefill) * refillPerSecond / 1000.0);
            lastRefill = now;
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
