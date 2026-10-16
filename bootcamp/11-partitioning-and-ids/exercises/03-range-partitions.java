// ─────────────────────────────────────────────────────────────────────────
//  03 · range partitions                                    ★★☆ core
//  concepts: range partitioning · binary search · range scans · splits
//  run: java -cp ../../_lib/out 03-range-partitions.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Bigtable, HBase and CockroachDB keep keys in sorted ranges. Finish the
//  nested class RangePartitioner: split points ["g", "p"] make partition
//  0 = everything below "g", 1 = ["g", "p"), 2 = "p" and above (a split
//  point starts its partition). The constructor copies the points and
//  throws IllegalArgumentException unless they are strictly ascending.
//  partitionOf(key) finds a key's partition. partitionsFor(from, to) lists,
//  ascending, the partitions a scan of [from, to) touches (to excluded;
//  from == to is an empty scan → []; from > to → IllegalArgumentException).
//  split(at) adds a split point to relieve a hot partition (an existing
//  point → IllegalArgumentException); splitPoints() is a read-only view.
//  Static evenSplits(sortedSample, n) picks n - 1 split points from a
//  sorted key sample: the elements at index i * size / n for i = 1..n-1
//  (n < 1 → IllegalArgumentException).
//
//      partitionOf("kiwi") → 1     partitionOf("p") → 2
//      partitionsFor("f", "h") → [0, 1]    partitionsFor("a", "g") → [0]
//      evenSplits([a, b, c, d, e, f, g, h], 4) → [c, e, g]
//
//  hint: the partition of a key is how many split points are <= it.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class RangePartitioner {
        private final List<String> points;

        RangePartitioner(List<String> splitPoints) {
            throw new UnsupportedOperationException("TODO");
        }

        int count() {
            throw new UnsupportedOperationException("TODO");
        }

        int partitionOf(String key) {
            throw new UnsupportedOperationException("TODO");
        }

        List<Integer> partitionsFor(String from, String toExclusive) {
            throw new UnsupportedOperationException("TODO");
        }

        void split(String at) {
            throw new UnsupportedOperationException("TODO");
        }

        List<String> splitPoints() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static List<String> evenSplits(List<String> sortedSample, int partitions) {
        throw new UnsupportedOperationException("TODO");
    }

    static RangePartitioner gp() {
        return new RangePartitioner(List.of("g", "p"));
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("a key goes to the range that holds it", () -> {
            var r = gp();
            eq(r.count(), 3);
            eq(r.partitionOf("apple"), 0);
            eq(r.partitionOf("kiwi"), 1);
            eq(r.partitionOf("zebra"), 2);
        });
        test("a split point belongs to the partition it starts", () -> {
            var r = gp();
            eq(r.partitionOf("g"), 1);
            eq(r.partitionOf("p"), 2);
            eq(r.partitionOf("fzzz"), 0);
            eq(r.partitionOf(""), 0);
        });
        test("refuses split points that are not strictly ascending", () -> {
            raises(IllegalArgumentException.class, () -> new RangePartitioner(List.of("p", "g")));
            raises(IllegalArgumentException.class, () -> new RangePartitioner(List.of("g", "g")));
            eq(new RangePartitioner(List.of()).partitionOf("anything"), 0);
        });
        test("a range scan touches only the partitions it overlaps", () -> {
            var r = gp();
            eq(r.partitionsFor("h", "k"), List.of(1));
            eq(r.partitionsFor("f", "h"), List.of(0, 1));
            eq(r.partitionsFor("a", "z"), List.of(0, 1, 2));
        });
        test("the end of a scan is exclusive", () -> {
            var r = gp();
            eq(r.partitionsFor("a", "g"), List.of(0));
            eq(r.partitionsFor("g", "p"), List.of(1));
            eq(r.partitionsFor("a", "gg"), List.of(0, 1));
            eq(r.partitionsFor("k", "k"), List.of());
        });
        test("refuses a backwards scan", () ->
            raises(IllegalArgumentException.class, () -> gp().partitionsFor("z", "a")));
        test("splitting a hot partition", () -> {
            var r = gp();
            r.split("m");
            eq(r.count(), 4);
            eq(r.splitPoints(), List.of("g", "m", "p"));
            eq(r.partitionOf("kiwi"), 1);
            eq(r.partitionOf("mango"), 2);
            eq(r.partitionOf("zebra"), 3);
            raises(IllegalArgumentException.class, () -> r.split("g"));
        });
        test("keeps its own copy and exposes it read-only", () -> {
            var input = new ArrayList<>(List.of("g", "p"));
            var r = new RangePartitioner(input);
            input.add("z");
            eq(r.splitPoints(), List.of("g", "p"));
            raises(UnsupportedOperationException.class, () -> r.splitPoints().add("x"));
        });
        test("even splits from a sorted sample", () -> {
            var sample = List.of("a", "b", "c", "d", "e", "f", "g", "h");
            eq(evenSplits(sample, 4), List.of("c", "e", "g"));
            eq(evenSplits(sample, 3), List.of("c", "f"));
            eq(evenSplits(sample, 1), List.of());
            raises(IllegalArgumentException.class, () -> evenSplits(sample, 0));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
