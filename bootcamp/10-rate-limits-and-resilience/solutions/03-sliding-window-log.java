// ─────────────────────────────────────────────────────────────────────────
//  03 · sliding window log — SOLUTION                       ★★☆ core
//  concepts: sliding windows · request logs · Retry-After · deques
//  run: java -cp ../../_lib/out 03-sliding-window-log.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  The sliding log answers the exact question "how many requests did
//  this client make in the last w milliseconds?" by keeping the
//  timestamps. Timestamps arrive in increasing order, so an ArrayDeque
//  per client works as a queue: expire from the head, append at the tail,
//  and the head is always the oldest request still counting.
//  Expiry must match the fixed window's convention: a request at t counts
//  while now < t + w, so it is dropped once t <= now - w. Off by one here
//  and a limit of 1 per second lets the next request in at t + w + 1.
//  Two behaviours make or break it:
//  1. Refused requests are NOT logged. If they were, a client that keeps
//     retrying while limited keeps pushing its own window forward and is
//     locked out forever: the limiter punishes retries, not traffic.
//  2. retryAfter is derived from the head: the client may go again when
//     the oldest counted request expires, at head + w. That is the value
//     an HTTP 429 puts in its Retry-After header.
//  The price is memory: one long per allowed request per client. Redis
//  implementations use a sorted set (ZADD, ZREMRANGEBYSCORE, ZCARD). The
//  "sliding window counter" approximation keeps two counters instead and
//  weights the previous window, trading exactness for O(1) memory.

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
            if (limit < 1) throw new IllegalArgumentException("limit must be at least 1: " + limit);
            if (windowMillis < 1) throw new IllegalArgumentException("window must be at least 1 ms: " + windowMillis);
            this.limit = limit;
            this.windowMillis = windowMillis;
            this.clock = clock;
        }

        boolean allow(String client) {
            long now = clock.getAsLong();
            var log = expire(client, now);
            if (log.size() >= limit) return false;
            log.addLast(now);
            return true;
        }

        long retryAfter(String client) {
            long now = clock.getAsLong();
            var log = expire(client, now);
            return log.size() < limit ? 0 : log.peekFirst() + windowMillis - now;
        }

        private ArrayDeque<Long> expire(String client, long now) {
            var log = logs.computeIfAbsent(client, c -> new ArrayDeque<>());
            while (!log.isEmpty() && log.peekFirst() <= now - windowMillis) log.removeFirst();
            return log;
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
