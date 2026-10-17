// ─────────────────────────────────────────────────────────────────────────
//  06 · a token bucket on an injected clock (fill-in)       ★★☆ core
//  concepts: fill-in · token bucket · injected clock · fractional refill
//  run: java -cp ../../_lib/out 06-token-bucket-fill-in.java
// ─────────────────────────────────────────────────────────────────────────
//
//  FILL-IN: the limiter is complete except for 4 holes. Replace each
//  `/* ___ */ hole()` with the expression its comment asks for; delete
//  hole() when you are done.
//  TokenBucket(capacity, refillPerSecond, clockMs) starts FULL. Tokens
//  drip in continuously at refillPerSecond, fractions included, and never
//  pile up above capacity. tryAcquire(n) takes n tokens if there are at
//  least n (true) and otherwise takes nothing (false). waitMsFor(n) is
//  how many whole ms until n tokens are there (0 if they are now),
//  rounded up; n above capacity: IllegalArgumentException.
//
//      cap 3, 2/s: 3 × tryAcquire(1) → true; 4th → false; +500 ms → true
//      cap 3, 4/s, empty: waitMsFor(1) → 250
//
//  hint: tokens is a double for a reason; follow the units (ms vs s).

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    // a hole: each "/* ___ */ hole()" below is yours to replace
    static <T> T hole() {
        throw new UnsupportedOperationException("TODO");
    }

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
            double added = /* ___ */ hole(); // tokens earned between last and now
            tokens = /* ___ */ hole(); // add them, but never above capacity
            last = now;
        }

        boolean tryAcquire(int n) {
            refill();
            if (/* ___ */ hole()) { // are there enough tokens?
                tokens -= n;
                return true;
            }
            return false;
        }

        long waitMsFor(int n) {
            refill();
            if (n > capacity) throw new IllegalArgumentException("never: " + n + " > capacity " + capacity);
            if (tokens >= n) return 0;
            return (long) Math.ceil(/* ___ */ hole()); // ms until the missing tokens have dripped in
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
