// ─────────────────────────────────────────────────────────────────────────
//  06 · a token bucket on an injected clock (fill-in) — SOLUTION  ★★☆ core
//  concepts: fill-in · token bucket · injected clock · fractional refill
//  run: java -cp ../../_lib/out 06-token-bucket-fill-in.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  The four holes:
//    added    (now - last) * refillPerSecond / 1000.0
//    tokens   Math.min(capacity, tokens + added)
//    enough   tokens >= n
//    wait     (n - tokens) * 1000 / refillPerSecond
//  A token bucket refills LAZILY: nothing ticks in the background; every
//  call first credits the time since the last call, then decides. That
//  is why `last = now` follows every refill, and why the clock is
//  injected: tests move time by assignment instead of sleeping.
//  Units are the classic slip. The clock is in ms and the rate per
//  second, so the credit divides by 1000.0. Integer arithmetic here
//  ((now - last) / 1000 * rate) rounds every short gap down to 0 tokens,
//  and because last still moves forward, a client calling every 250 ms
//  never earns a token at all. Keeping tokens as a double keeps the
//  fractions; Math.min caps the burst at capacity after a long idle.
//  The check-then-take is all or nothing: a request for 2 when only 1
//  is there must leave that 1 alone. Module 10 builds the same bucket
//  from scratch; module 21 has a buggy one to hunt.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    static final class TokenBucket {
        private final long capacity;
        private final double refillPerSecond;
        private final LongSupplier clockMs;
        private double tokens;
        private long last;

        TokenBucket(long capacity, double refillPerSecond, LongSupplier clockMs) {
            this.capacity = capacity;
            this.refillPerSecond = refillPerSecond;
            this.clockMs = clockMs;
            this.tokens = capacity;
            this.last = clockMs.getAsLong();
        }

        private void refill() {
            long now = clockMs.getAsLong();
            double added = (now - last) * refillPerSecond / 1000.0;
            tokens = Math.min(capacity, tokens + added);
            last = now;
        }

        boolean tryAcquire(int n) {
            refill();
            if (tokens >= n) {
                tokens -= n;
                return true;
            }
            return false;
        }

        long waitMsFor(int n) {
            refill();
            if (n > capacity) throw new IllegalArgumentException("never: " + n + " > capacity " + capacity);
            if (tokens >= n) return 0;
            return (long) Math.ceil((n - tokens) * 1000 / refillPerSecond);
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("starts full and refuses once empty", () -> {
            long[] now = {0};
            var b = new TokenBucket(3, 2, () -> now[0]);
            ok(b.tryAcquire(1) && b.tryAcquire(1) && b.tryAcquire(1), "three tokens to start");
            ok(!b.tryAcquire(1), "then empty");
        });
        test("refills at the configured rate", () -> {
            long[] now = {0};
            var b = new TokenBucket(3, 2, () -> now[0]);
            b.tryAcquire(3);
            now[0] = 500;
            ok(b.tryAcquire(1), "one token after 500 ms at 2/s");
            ok(!b.tryAcquire(1), "and only one");
        });
        test("keeps fractional progress across calls", () -> {
            long[] now = {0};
            var b = new TokenBucket(1, 1, () -> now[0]);
            b.tryAcquire(1);
            for (int i = 1; i <= 3; i++) {
                now[0] = 250L * i;
                ok(!b.tryAcquire(1), "only " + (i * 0.25) + " of a token at " + now[0] + " ms");
            }
            now[0] = 1000;
            ok(b.tryAcquire(1), "a whole token after 1000 ms in four steps");
        });
        test("never holds more than capacity after a long idle", () -> {
            long[] now = {0};
            var b = new TokenBucket(2, 10, () -> now[0]);
            b.tryAcquire(2);
            now[0] = 3_600_000;
            ok(b.tryAcquire(2), "full again");
            ok(!b.tryAcquire(1), "but no more than capacity");
        });
        test("a request for several tokens takes all or nothing", () -> {
            long[] now = {0};
            var b = new TokenBucket(5, 1, () -> now[0]);
            b.tryAcquire(4);
            ok(!b.tryAcquire(2), "only one token there");
            ok(b.tryAcquire(1), "the failed request took nothing");
        });
        test("waitMsFor says how long until n tokens", () -> {
            long[] now = {0};
            var b = new TokenBucket(3, 4, () -> now[0]);
            b.tryAcquire(3);
            eq(b.waitMsFor(1), 250);
            now[0] = 125;
            eq(b.waitMsFor(3), 625);
            now[0] = 375;
            eq(b.waitMsFor(1), 0);
            raises(IllegalArgumentException.class, () -> b.waitMsFor(4));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
