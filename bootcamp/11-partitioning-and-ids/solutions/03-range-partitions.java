// ─────────────────────────────────────────────────────────────────────────
//  03 · range partitions — SOLUTION                         ★★☆ core
//  concepts: range partitioning · binary search · range scans · splits
//  run: java -cp ../../_lib/out 03-range-partitions.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Range partitioning keeps neighbouring keys together, which buys the one
//  thing hash partitioning cannot: a range scan ("all orders for customer
//  42", "events between 10:00 and 11:00") touches only the partitions that
//  overlap it. The price is hot spots: sequential keys (timestamps, auto
//  ids) all land in the last partition. Systems answer with split(): when
//  a range grows hot or large, cut it in two and move one half.
//  The partition of a key = the number of split points <= key, which is a
//  binary search. Collections.binarySearch returns -(insertion point) - 1
//  for a missing key; for a key equal to a split point it returns its
//  index, and the key belongs to the partition AFTER it (index + 1),
//  because a split point starts its range. Getting that boundary wrong is
//  the classic bug, and the tests pin both sides of it.
//  partitionsFor uses the same function on both ends, and the exclusive
//  end matters: a scan of [a, g) must not touch partition 1 just because
//  "g" is where partition 1 starts. So the scan ends in partitionOf(to),
//  minus one when `to` is exactly a split point (then no key of that
//  partition is below `to`).
//  evenSplits is how operators pre-split a table: sample the keys, take
//  evenly spaced quantiles, and every partition starts with the same load.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class RangePartitioner {
        private final List<String> points;

        RangePartitioner(List<String> splitPoints) {
            for (int i = 1; i < splitPoints.size(); i++) {
                if (splitPoints.get(i - 1).compareTo(splitPoints.get(i)) >= 0) {
                    throw new IllegalArgumentException("split points must be strictly ascending: " + splitPoints);
                }
            }
            this.points = new ArrayList<>(splitPoints);
        }

        int count() {
            return points.size() + 1;
        }

        int partitionOf(String key) {
            int at = Collections.binarySearch(points, key);
            return at >= 0 ? at + 1 : -at - 1;
        }

        List<Integer> partitionsFor(String from, String toExclusive) {
            int cmp = from.compareTo(toExclusive);
            if (cmp > 0) throw new IllegalArgumentException("backwards range: " + from + " > " + toExclusive);
            var out = new ArrayList<Integer>();
            if (cmp == 0) return out;
            int first = partitionOf(from);
            int last = partitionOf(toExclusive);
            if (Collections.binarySearch(points, toExclusive) >= 0) last--;
            for (int p = first; p <= last; p++) out.add(p);
            return out;
        }

        void split(String at) {
            int where = Collections.binarySearch(points, at);
            if (where >= 0) throw new IllegalArgumentException("already a split point: " + at);
            points.add(-where - 1, at);
        }

        List<String> splitPoints() {
            return Collections.unmodifiableList(points);
        }
    }

    static List<String> evenSplits(List<String> sortedSample, int partitions) {
        if (partitions < 1) throw new IllegalArgumentException("partitions must be at least 1: " + partitions);
        var out = new ArrayList<String>();
        for (int i = 1; i < partitions; i++) out.add(sortedSample.get(i * sortedSample.size() / partitions));
        return out;
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
