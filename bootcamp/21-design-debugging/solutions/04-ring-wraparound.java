// ─────────────────────────────────────────────────────────────────────────
//  04 · the hash ring with a seam — SOLUTION                ★★☆ core
//  concepts: debugging · consistent hashing · TreeMap navigation · wraparound
//  run: java -cp ../../_lib/out 04-ring-wraparound.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Both bugs are in nodeFor, and both are one TreeMap method name.
//  1. Reproduce report 1: points 100, 200, 300 and a key at 350. It goes
//     to C (the largest point) instead of wrapping to A. Narrow: the
//     fallback after "no entry clockwise" is lastEntry(). On a RING,
//     past the end is the beginning: firstEntry(). With lastEntry the
//     arc above the largest point is added to the wrong node, which is
//     exactly the imbalance in the report: the largest point's node
//     gains that arc, and the smallest point's node loses it.
//  2. Reproduce report 2: a key at exactly 200. higherEntry is STRICTLY
//     greater, so it skips the point at 200 and returns C. The ring's
//     rule is "first point at or after", which is ceilingEntry. Two
//     components that use different rules (here, the router and a
//     rebalancer) route the same key to different nodes.
//  TreeMap's navigation methods, for a key k: floor (<= k), lower (< k),
//  ceiling (>= k), higher (> k). The off-by-one between ceiling and
//  higher is the same slip as < versus <= at a window edge.
//  The wraparound bug hides in tests with one node, or with keys that
//  all hash below the largest point. Always test a key above the last
//  point. Module 11, exercise 04 builds the ring with real hashes.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    static final class Ring {
        private final TreeMap<Integer, String> points = new TreeMap<>();
        private final ToIntFunction<String> hash;
        private final int vnodes;

        Ring(ToIntFunction<String> hash, int vnodes) {
            this.hash = hash;
            this.vnodes = vnodes;
        }

        void add(String node) {
            for (int i = 0; i < vnodes; i++) points.put(hash.applyAsInt(node + "#" + i), node);
        }

        void remove(String node) {
            for (int i = 0; i < vnodes; i++) points.remove(hash.applyAsInt(node + "#" + i));
        }

        String nodeFor(String key) {
            if (points.isEmpty()) throw new IllegalStateException("no nodes");
            var e = points.ceilingEntry(hash.applyAsInt(key));
            if (e == null) e = points.firstEntry();
            return e.getValue();
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        var table = Map.of("A#0", 100, "B#0", 200, "C#0", 300, "A#1", 250, "B#1", 50, "C#1", 150);
        ToIntFunction<String> hash = s -> s.startsWith("k") ? Integer.parseInt(s.substring(1)) : table.get(s);
        test("routes a key to the next point clockwise", () -> {
            var r = new Ring(hash, 1);
            r.add("A");
            r.add("B");
            r.add("C");
            eq(r.nodeFor("k150"), "B");
            eq(r.nodeFor("k201"), "C");
            eq(r.nodeFor("k0"), "A");
        });
        test("a key past the largest point wraps to the smallest", () -> {
            var r = new Ring(hash, 1);
            r.add("A");
            r.add("B");
            r.add("C");
            eq(r.nodeFor("k350"), "A");
            eq(r.nodeFor("k301"), "A");
        });
        test("a key exactly on a point belongs to that point's node", () -> {
            var r = new Ring(hash, 1);
            r.add("A");
            r.add("B");
            r.add("C");
            eq(r.nodeFor("k100"), "A");
            eq(r.nodeFor("k200"), "B");
            eq(r.nodeFor("k300"), "C");
        });
        test("removing a node moves only its keys, to the next node", () -> {
            var r = new Ring(hash, 1);
            r.add("A");
            r.add("B");
            r.add("C");
            r.remove("B");
            eq(r.nodeFor("k150"), "C");
            eq(r.nodeFor("k50"), "A");
            eq(r.nodeFor("k250"), "C");
            r.remove("C");
            eq(r.nodeFor("k250"), "A");
        });
        test("virtual nodes interleave the nodes around the ring", () -> {
            var r = new Ring(hash, 2);
            r.add("A");
            r.add("B");
            r.add("C");
            eq(r.nodeFor("k10"), "B");
            eq(r.nodeFor("k120"), "C");
            eq(r.nodeFor("k220"), "A");
            eq(r.nodeFor("k270"), "C");
            eq(r.nodeFor("k310"), "B");
        });
        test("an empty ring refuses lookups", () -> {
            var r = new Ring(hash, 1);
            raises(IllegalStateException.class, () -> r.nodeFor("k1"));
            r.add("A");
            eq(r.nodeFor("k999"), "A");
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
