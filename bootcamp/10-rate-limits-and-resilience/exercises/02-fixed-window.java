// ─────────────────────────────────────────────────────────────────────────
//  02 · fixed window counter                                ★☆☆ warm-up
//  concepts: fixed windows · per-client counters · aligned windows
//  run: java -cp ../../_lib/out 02-fixed-window.java
// ─────────────────────────────────────────────────────────────────────────
//
//  "At most 100 requests per client per minute." Finish the nested class
//  FixedWindowLimiter. Time (milliseconds, from the injected clock) is
//  cut into windows aligned to the clock: [0, w), [w, 2w), ... A request
//  at time t belongs to window t / w. allow(client) returns true and
//  counts the request if that client has made fewer than limit requests
//  in the current window, else false. Each client has its own count.
//  remaining(client) is how many more requests the client may make in
//  the current window (limit for a client never seen). A limit or window
//  below 1 is an IllegalArgumentException.
//
//      limit 1, window 1000: allow at t=0 → true, t=999 → false
//                            allow at t=1000 → true (new window)
//
//  hint: per client, remember which window the count belongs to.

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
            throw new UnsupportedOperationException("TODO");
        }

        boolean allow(String client) {
            throw new UnsupportedOperationException("TODO");
        }

        int remaining(String client) {
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
