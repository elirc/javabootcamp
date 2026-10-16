// ─────────────────────────────────────────────────────────────────────────
//  03 · sliding window log                                  ★★☆ core
//  concepts: sliding windows · request logs · Retry-After · deques
//  run: java -cp ../../_lib/out 03-sliding-window-log.java
// ─────────────────────────────────────────────────────────────────────────
//
//  The fixed window lets 2 x limit through around a boundary. Finish
//  SlidingWindowLog, which is exact: it remembers the time of every
//  ALLOWED request per client. A request allowed at t counts while
//  now < t + window. allow(client) returns true and logs now if fewer
//  than limit requests count, else false; a refused request is NOT
//  logged. retryAfter(client) is 0 if a request right now would be
//  allowed, otherwise the milliseconds until the oldest counted request
//  stops counting. Clients are independent. A limit or window below 1 is
//  an IllegalArgumentException.
//
//      limit 3, window 1000: 3 allowed at t=999
//      allow at t=1000 → false  ...  allow at t=1999 → true
//      limit 2: allowed at t=0 and t=300; retryAfter at t=400 → 600
//
//  hint: timestamps arrive in order, so the oldest is always at the front.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.LongSupplier;

class Exercise {

    static final class SlidingWindowLog {
        private final int limit;
        private final long windowMillis;
        private final LongSupplier clock;
        private final Map<String, ArrayDeque<Long>> logs = new HashMap<>();

        SlidingWindowLog(int limit, long windowMillis, LongSupplier clock) {
            throw new UnsupportedOperationException("TODO");
        }

        boolean allow(String client) {
            throw new UnsupportedOperationException("TODO");
        }

        long retryAfter(String client) {
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
            var rl = new SlidingWindowLog(2, 1_000, new FakeClock());
            eq(rl.allow("alice"), true);
            eq(rl.allow("alice"), true);
            eq(rl.allow("alice"), false);
            eq(rl.allow("bob"), true);
        });
        test("a request stops counting exactly one window later", () -> {
            var clock = new FakeClock();
            var rl = new SlidingWindowLog(1, 1_000, clock);
            clock.now = 200;
            rl.allow("alice");
            clock.now = 1_199;
            eq(rl.allow("alice"), false);
            clock.now = 1_200;
            eq(rl.allow("alice"), true);
        });
        test("no burst at a window boundary", () -> {
            var clock = new FakeClock();
            var rl = new SlidingWindowLog(3, 1_000, clock);
            clock.now = 999;
            for (int i = 0; i < 3; i++) rl.allow("alice");
            clock.now = 1_000;
            eq(rl.allow("alice"), false);
            clock.now = 1_998;
            eq(rl.allow("alice"), false);
            clock.now = 1_999;
            eq(rl.allow("alice"), true);
        });
        test("refused requests are not logged", () -> {
            var clock = new FakeClock();
            var rl = new SlidingWindowLog(1, 1_000, clock);
            rl.allow("alice");
            clock.now = 500;
            eq(rl.allow("alice"), false);
            clock.now = 900;
            eq(rl.allow("alice"), false);
            clock.now = 1_000;
            eq(rl.allow("alice"), true);
        });
        test("old requests expire one by one", () -> {
            var clock = new FakeClock();
            var rl = new SlidingWindowLog(3, 1_000, clock);
            rl.allow("alice");
            clock.now = 100;
            rl.allow("alice");
            clock.now = 200;
            rl.allow("alice");
            clock.now = 1_050;
            eq(rl.allow("alice"), true);
            eq(rl.allow("alice"), false);
            clock.now = 1_100;
            eq(rl.allow("alice"), true);
        });
        test("retryAfter says when the oldest counted request expires", () -> {
            var clock = new FakeClock();
            var rl = new SlidingWindowLog(2, 1_000, clock);
            rl.allow("alice");
            clock.now = 300;
            rl.allow("alice");
            clock.now = 400;
            eq(rl.retryAfter("alice"), 600);
            clock.now = 999;
            eq(rl.retryAfter("alice"), 1);
            clock.now = 1_000;
            eq(rl.retryAfter("alice"), 0);
            clock.now = 1_300;
            eq(rl.retryAfter("alice"), 0);
        });
        test("retryAfter is 0 for a client with room or never seen", () -> {
            var clock = new FakeClock();
            var rl = new SlidingWindowLog(2, 1_000, clock);
            eq(rl.retryAfter("nobody"), 0);
            rl.allow("alice");
            eq(rl.retryAfter("alice"), 0);
            eq(rl.allow("alice"), true);
        });
        test("refuses bad settings", () -> {
            var clock = new FakeClock();
            raises(IllegalArgumentException.class, () -> new SlidingWindowLog(0, 1_000, clock));
            raises(IllegalArgumentException.class, () -> new SlidingWindowLog(5, 0, clock));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
