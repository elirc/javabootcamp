// ─────────────────────────────────────────────────────────────────────────
//  04 · the hash ring with a seam                           ★★☆ core
//  concepts: debugging · consistent hashing · TreeMap navigation · wraparound
//  run: java -cp ../../_lib/out 04-ring-wraparound.java
// ─────────────────────────────────────────────────────────────────────────
//
//  DEBUG HUNT: the code below is complete and compiles; it has bugs. Run
//  it, read the failing tests, find and fix the bugs. Do not edit tests.
//  Intended: Ring(hash, vnodes) is a consistent-hash ring. add(node)
//  places vnodes points, hash(node + "#" + i) for i in 0..vnodes-1.
//  nodeFor(key) walks CLOCKWISE from hash(key) to the first point at or
//  after it; past the largest point it wraps around to the smallest.
//  remove(node) takes its points away. An empty ring:
//  IllegalStateException. (The tests inject the hash so the points are
//  easy to read: A#0 → 100, B#0 → 200, C#0 → 300, key "k150" → 150.)
//  Bug reports:
//    "The node with the largest point gets far more keys than the
//     others, and the node with the smallest gets fewer."
//    "The rebalancing tool and the router disagree about a few keys:
//     always ones whose hash equals a node's point exactly."
//
//      points A 100, B 200, C 300: k150 → B   k350 → A   k200 → B
//
//  hint: list which keys each node gets for hashes 0, 100, 150, 300, 350.

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
            var e = points.higherEntry(hash.applyAsInt(key));
            if (e == null) e = points.lastEntry();
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
