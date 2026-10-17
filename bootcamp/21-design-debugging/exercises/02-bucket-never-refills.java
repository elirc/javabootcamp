// ─────────────────────────────────────────────────────────────────────────
//  02 · the bucket that never refills                       ★★☆ core
//  concepts: debugging · token bucket · integer division · burst cap
//  run: java -cp ../../_lib/out 02-bucket-never-refills.java
// ─────────────────────────────────────────────────────────────────────────
//
//  DEBUG HUNT: the code below is complete and compiles; it has bugs. Run
//  it, read the failing tests, find and fix the bugs. Do not edit tests.
//  Intended: TokenBucket(capacity, refillPerSecond, clockMs) starts full
//  and earns refillPerSecond tokens per second CONTINUOUSLY, however
//  the calls are spaced, never holding more than capacity. tryAcquire()
//  takes one token if there is one.
//  Bug reports:
//    "Our mobile app polls every 250 ms. After its first burst, every
//     request is rejected, forever. Clients that poll once a second
//     are fine."
//    "After the overnight lull, one client fired 30,000 requests in a
//     second and all were allowed. The limit is 10 per second."
//
//      cap 2, 1/s: drain, then a call every 250 ms → allowed at 1000 ms
//
//  hint: compute what one call 250 ms after the last adds, by hand.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    static final class TokenBucket {
        private final long capacity;
        private final long refillPerSecond;
        private final LongSupplier clockMs;
        private long tokens;
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
            tokens = tokens + (now - last) / 1000 * refillPerSecond;
            last = now;
            if (tokens > 0) {
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
