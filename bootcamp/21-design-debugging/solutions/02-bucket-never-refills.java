// ─────────────────────────────────────────────────────────────────────────
//  02 · the bucket that never refills — SOLUTION            ★★☆ core
//  concepts: debugging · token bucket · integer division · burst cap
//  run: java -cp ../../_lib/out 02-bucket-never-refills.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  1. Reproduce report 1: drain the bucket, call every 250 ms. Narrow by
//     printing the credit per call: (250 / 1000) * 1 is 0 in long
//     arithmetic. Then `last = now` throws the 250 ms away, so the next
//     call starts from zero again: a client that calls more often than
//     once per token interval NEVER earns a token, while one that waits
//     a full second does. That asymmetry (slow clients fine, fast
//     clients locked out) is the signature of truncation. Fix: keep
//     tokens as a double and credit (now - last) * rate / 1000.0. The
//     test then has to ask for a WHOLE token (tokens >= 1), or 0.25 of
//     a token would pass "> 0". (An all-integer fix also works: advance
//     last only by the time the earned whole tokens account for.)
//     Pinned by "a client polling every 250 ms still earns tokens" and
//     "fast polling at 10/s allows about 10 per second".
//  2. Reproduce report 2: drain, idle 8 hours, then fire. Nothing caps
//     the credit, so the bucket holds 288,000 tokens. The capacity IS
//     the maximum burst; Math.min(capacity, ...) after the credit. Pinned
//     by "a long idle refills only up to capacity".
//  Fixing only the cap leaves the polling client locked out; fixing
//  only the arithmetic lets the overnight burst through. Module 10,
//  exercise 01 and module 20, exercise 06 build this bucket correctly.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    static final class TokenBucket {
        private final long capacity;
        private final long refillPerSecond;
        private final LongSupplier clockMs;
        private double tokens;
        private long last;

        TokenBucket(long capacity, long refillPerSecond, LongSupplier clockMs) {
            this.capacity = capacity;
            this.refillPerSecond = refillPerSecond;
            this.clockMs = clockMs;
            this.tokens = capacity;
            this.last = clockMs.getAsLong();
        }

        boolean tryAcquire() {
            long now = clockMs.getAsLong();
            tokens = Math.min(capacity, tokens + (now - last) * refillPerSecond / 1000.0);
            last = now;
            if (tokens >= 1) {
                tokens--;
                return true;
            }
            return false;
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("starts full and refuses when empty", () -> {
            long[] now = {0};
            var b = new TokenBucket(3, 1, () -> now[0]);
            ok(b.tryAcquire() && b.tryAcquire() && b.tryAcquire(), "three to start");
            ok(!b.tryAcquire(), "then empty");
        });
        test("a client that waits a whole second gets a token", () -> {
            long[] now = {0};
            var b = new TokenBucket(1, 1, () -> now[0]);
            b.tryAcquire();
            now[0] = 1000;
            ok(b.tryAcquire(), "one token after one second");
            ok(!b.tryAcquire(), "and only one");
        });
        test("a client polling every 250 ms still earns tokens", () -> {
            long[] now = {0};
            var b = new TokenBucket(2, 1, () -> now[0]);
            b.tryAcquire();
            b.tryAcquire();
            for (int t = 250; t < 1000; t += 250) {
                now[0] = t;
                ok(!b.tryAcquire(), "no whole token yet at " + t + " ms");
            }
            now[0] = 1000;
            ok(b.tryAcquire(), "a token at 1000 ms");
        });
        test("fast polling at 10/s allows about 10 per second", () -> {
            long[] now = {0};
            var b = new TokenBucket(1, 10, () -> now[0]);
            b.tryAcquire();
            int allowed = 0;
            for (int t = 50; t <= 2000; t += 50) {
                now[0] = t;
                if (b.tryAcquire()) allowed++;
            }
            eq(allowed, 20);
        });
        test("a long idle refills only up to capacity", () -> {
            long[] now = {0};
            var b = new TokenBucket(10, 10, () -> now[0]);
            for (int i = 0; i < 10; i++) b.tryAcquire();
            now[0] = 8 * 3_600_000L;
            int allowed = 0;
            for (int i = 0; i < 1000; i++) if (b.tryAcquire()) allowed++;
            eq(allowed, 10);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
