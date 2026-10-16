// ─────────────────────────────────────────────────────────────────────────
//  02 · fixed window counter — SOLUTION                     ★☆☆ warm-up
//  concepts: fixed windows · per-client counters · aligned windows
//  run: java -cp ../../_lib/out 02-fixed-window.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  The cheapest limiter there is: one counter per client per window. The
//  window a request falls in is just now / windowMillis (integer
//  division), so windows are aligned to the clock (0-999, 1000-1999, ...)
//  and every server computing it agrees without talking to each other.
//  In Redis this is INCR on a key like "rl:alice:1718000" plus EXPIRE.
//  Per client we keep (window, count). When a request arrives in a newer
//  window the count starts over; there is no timer and no cleanup pass.
//  remaining() must apply the same "is this a new window?" rule, or it
//  reports the previous window's leftovers.
//  The wrong turn is starting each client's window at its FIRST request
//  (t = 900 → window 900-1899). That is a different algorithm, it needs
//  per-client timers, and it disagrees with every other node.
//  The known weakness, pinned by a test on purpose: a client can spend
//  the full limit at t = 999 and again at t = 1000, which is 2 x limit in
//  two milliseconds. If that matters, use the sliding log in 03 (exact,
//  memory per request) or a token bucket (01).

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.LongSupplier;

class Exercise {

    static final class FixedWindowLimiter {
        private record Window(long index, int count) {}

        private final int limit;
        private final long windowMillis;
        private final LongSupplier clock;
        private final Map<String, Window> windows = new HashMap<>();

        FixedWindowLimiter(int limit, long windowMillis, LongSupplier clock) {
            if (limit < 1) throw new IllegalArgumentException("limit must be at least 1: " + limit);
            if (windowMillis < 1) throw new IllegalArgumentException("window must be at least 1 ms: " + windowMillis);
            this.limit = limit;
            this.windowMillis = windowMillis;
            this.clock = clock;
        }

        boolean allow(String client) {
            var w = current(client);
            if (w.count() >= limit) return false;
            windows.put(client, new Window(w.index(), w.count() + 1));
            return true;
        }

        int remaining(String client) {
            return limit - current(client).count();
        }

        private Window current(String client) {
            long index = clock.getAsLong() / windowMillis;
            var w = windows.get(client);
            return w != null && w.index() == index ? w : new Window(index, 0);
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
        test("allows up to the limit, then refuses", () -> {
            var rl = new FixedWindowLimiter(3, 1_000, new FakeClock());
            eq(rl.allow("alice"), true);
            eq(rl.allow("alice"), true);
            eq(rl.allow("alice"), true);
            eq(rl.allow("alice"), false);
            eq(rl.allow("alice"), false);
        });
        test("a new window starts at the exact multiple", () -> {
            var clock = new FakeClock();
            var rl = new FixedWindowLimiter(1, 1_000, clock);
            rl.allow("alice");
            clock.now = 999;
            eq(rl.allow("alice"), false);
            clock.now = 1_000;
            eq(rl.allow("alice"), true);
        });
        test("windows are aligned to the clock, not to the first request", () -> {
            var clock = new FakeClock();
            var rl = new FixedWindowLimiter(1, 1_000, clock);
            clock.now = 900;
            eq(rl.allow("alice"), true);
            clock.now = 1_000;
            eq(rl.allow("alice"), true);
        });
        test("clients are counted separately", () -> {
            var rl = new FixedWindowLimiter(1, 1_000, new FakeClock());
            eq(rl.allow("alice"), true);
            eq(rl.allow("bob"), true);
            eq(rl.allow("alice"), false);
        });
        test("remaining reports what is left in the current window", () -> {
            var clock = new FakeClock();
            var rl = new FixedWindowLimiter(3, 1_000, clock);
            eq(rl.remaining("alice"), 3);
            rl.allow("alice");
            rl.allow("alice");
            eq(rl.remaining("alice"), 1);
            rl.allow("alice");
            rl.allow("alice");
            eq(rl.remaining("alice"), 0);
            clock.now = 2_500;
            eq(rl.remaining("alice"), 3);
        });
        test("the boundary burst: 2 x limit across two windows", () -> {
            var clock = new FakeClock();
            var rl = new FixedWindowLimiter(3, 1_000, clock);
            clock.now = 999;
            int allowed = 0;
            for (int i = 0; i < 3; i++) if (rl.allow("alice")) allowed++;
            clock.now = 1_000;
            for (int i = 0; i < 5; i++) if (rl.allow("alice")) allowed++;
            eq(allowed, 6);
        });
        test("refuses bad settings", () -> {
            var clock = new FakeClock();
            raises(IllegalArgumentException.class, () -> new FixedWindowLimiter(0, 1_000, clock));
            raises(IllegalArgumentException.class, () -> new FixedWindowLimiter(5, 0, clock));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
