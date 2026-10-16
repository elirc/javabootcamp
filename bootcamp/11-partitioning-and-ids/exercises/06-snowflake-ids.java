// ─────────────────────────────────────────────────────────────────────────
//  06 · snowflake ids                                       ★★☆ core
//  concepts: distributed ids · bit packing · shifts and masks · clocks
//  run: java -cp ../../_lib/out 06-snowflake-ids.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Twitter's Snowflake hands out unique, time-ordered 64-bit ids with no
//  coordination. Finish the nested class Snowflake(epochMs, worker, clock):
//  an id is (now - epochMs) << 22 | worker << 12 | sequence, with worker
//  in 0..1023 (else IllegalArgumentException) and sequence in 0..4095.
//  next() reads the clock exactly once. A new millisecond starts the
//  sequence at 0; the same millisecond as the previous id adds 1. When the
//  sequence would pass 4095, keep reading the clock until it moves past
//  that millisecond, then use sequence 0 there. A clock that moved back
//  behind the previous id's millisecond → IllegalStateException. Static
//  parse(id, epochMs) splits an id into Parts (timestamp in absolute ms).
//
//      epoch 0, worker 7, clock 5:   next() → (5 << 22) | (7 << 12) | 0
//      same millisecond again:       next() → ... | 1
//      parse(that id, 0)             → Parts[timestampMs=5, worker=7,
//                                            sequence=1]
//
//  hint: mask with 0x3FF (10 bits) and 0xFFF (12 bits) after shifting.

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
            throw new UnsupportedOperationException("TODO");
        }

        long next() {
            throw new UnsupportedOperationException("TODO");
        }

        static Parts parse(long id, long epochMs) {
            throw new UnsupportedOperationException("TODO");
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
