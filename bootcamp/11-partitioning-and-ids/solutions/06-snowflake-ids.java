// ─────────────────────────────────────────────────────────────────────────
//  06 · snowflake ids — SOLUTION                            ★★☆ core
//  concepts: distributed ids · bit packing · shifts and masks · clocks
//  run: java -cp ../../_lib/out 06-snowflake-ids.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A database sequence is one machine everyone must ask. Snowflake moves
//  id generation into every process: 41 bits of milliseconds (about 69
//  years from a custom epoch), 10 bits of worker id (assigned once per
//  process, so two workers never collide), and 12 bits of sequence for up
//  to 4096 ids per worker per millisecond. Because time is in the high
//  bits, ids sort by creation time, which keeps B-tree inserts appending
//  at the right edge instead of splitting pages all over the index.
//  The sequence rules are the heart of it. Same millisecond: count up. New
//  millisecond: reset to 0. Ran out (the 4097th id in one ms): you must
//  NOT wrap to 0 in the same ms (a duplicate) and must NOT let 4096 spill
//  into the worker bits (a different worker's id). Snowflake waits for the
//  next millisecond; with an injected clock that is a loop that keeps
//  reading it. Reading the clock exactly once per normal call is part of
//  the contract so tests can script time.
//  Clocks do move backwards (NTP corrections, VM migration). Issuing ids
//  then could repeat ones already handed out, so refuse loudly; real
//  deployments alert on it or wait out the gap.
//  parse is the reverse: >>> 22 for the time (unsigned shift, though ids
//  stay positive for 69 years), then (id >>> 12) & 0x3FF and id & 0xFFF.
//  The clock is a LongSupplier, never System.currentTimeMillis(), so the
//  tests are deterministic.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    record Parts(long timestampMs, int worker, int sequence) {}

    static final class Snowflake {
        private final long epochMs;
        private final int worker;
        private final LongSupplier clock;
        private long lastMs = -1;
        private int sequence;

        Snowflake(long epochMs, int worker, LongSupplier clock) {
            if (worker < 0 || worker > 1023) throw new IllegalArgumentException("worker must be 0..1023: " + worker);
            this.epochMs = epochMs;
            this.worker = worker;
            this.clock = clock;
        }

        long next() {
            long now = clock.getAsLong();
            if (now < lastMs) throw new IllegalStateException("clock moved backwards: " + now + " < " + lastMs);
            if (now == lastMs) {
                sequence++;
                if (sequence > 4095) {
                    while (now <= lastMs) now = clock.getAsLong();
                    sequence = 0;
                }
            } else {
                sequence = 0;
            }
            lastMs = now;
            return (now - epochMs) << 22 | (long) worker << 12 | sequence;
        }

        static Parts parse(long id, long epochMs) {
            return new Parts((id >>> 22) + epochMs, (int) ((id >>> 12) & 0x3FF), (int) (id & 0xFFF));
        }
    }

    /** A scripted clock: returns the given times in order, repeating the last one forever. */
    static LongSupplier script(long... times) {
        var i = new int[1];
        return () -> times[Math.min(i[0]++, times.length - 1)];
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("packs time, worker and sequence into one long", () -> {
            var g = new Snowflake(0, 7, script(5));
            eq(g.next(), (5L << 22) | (7L << 12));
            eq(new Snowflake(1_000, 1, script(1_003)).next(), (3L << 22) | (1L << 12));
        });
        test("ids in the same millisecond count up the sequence", () -> {
            var g = new Snowflake(0, 7, script(5));
            g.next();
            eq(g.next() & 0xFFF, 1);
            eq(g.next() & 0xFFF, 2);
        });
        test("a new millisecond resets the sequence to zero", () -> {
            var g = new Snowflake(0, 7, script(5, 5, 5, 6));
            g.next();
            g.next();
            g.next();
            eq(Snowflake.parse(g.next(), 0), new Parts(6, 7, 0));
        });
        test("parse takes an id apart", () -> {
            var epoch = 1_288_834_974_657L;
            var g = new Snowflake(epoch, 1023, script(epoch + 86_400_000L));
            g.next();
            eq(Snowflake.parse(g.next(), epoch), new Parts(epoch + 86_400_000L, 1023, 1));
        });
        test("ids keep increasing as time moves on", () -> {
            var g = new Snowflake(0, 3, script(10, 10, 11, 15, 15, 15, 40));
            long previous = -1;
            for (int i = 0; i < 7; i++) {
                long id = g.next();
                ok(id > previous, "id " + i + " did not increase");
                previous = id;
            }
        });
        test("a used-up millisecond waits for the clock to move on", () -> {
            var reads = new int[1];
            var g = new Snowflake(0, 2, () -> reads[0]++ < 4100 ? 1_000L : 1_001L);
            long previous = -1;
            for (int i = 0; i < 4096; i++) {
                long id = g.next();
                ok(id > previous, "id " + i + " did not increase");
                previous = id;
            }
            eq(Snowflake.parse(previous, 0), new Parts(1_000, 2, 4095));
            long overflow = g.next();
            eq(Snowflake.parse(overflow, 0), new Parts(1_001, 2, 0));
            eq(reads[0], 4101);
        });
        test("refuses a clock that moved backwards", () -> {
            var g = new Snowflake(0, 1, script(50, 49));
            g.next();
            raises(IllegalStateException.class, g::next);
        });
        test("refuses a worker id outside 0..1023", () -> {
            raises(IllegalArgumentException.class, () -> new Snowflake(0, -1, script(1)));
            raises(IllegalArgumentException.class, () -> new Snowflake(0, 1024, script(1)));
            eq(Snowflake.parse(new Snowflake(0, 1023, script(1)).next(), 0), new Parts(1, 1023, 0));
        });
        test("two workers never collide in the same millisecond", () -> {
            var a = new Snowflake(0, 1, script(9));
            var b = new Snowflake(0, 2, script(9));
            var seen = new HashSet<Long>();
            for (int i = 0; i < 100; i++) {
                ok(seen.add(a.next()));
                ok(seen.add(b.next()));
            }
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
