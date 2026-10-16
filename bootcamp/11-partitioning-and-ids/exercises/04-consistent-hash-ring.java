// ─────────────────────────────────────────────────────────────────────────
//  04 · consistent hashing ring                             ★★☆ core
//  concepts: consistent hashing · virtual nodes · TreeMap · ceilingEntry
//  run: java -cp ../../_lib/out 04-consistent-hash-ring.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Dynamo, Cassandra and many cache clients place nodes on a hash ring.
//  Finish the nested class Ring(vnodes, hash): addNode(name) puts vnodes
//  points on the ring, at hash(name + "#" + i) for i = 0..vnodes-1, and
//  nodeFor(key) walks clockwise from hash(key): the owner is the node of
//  the first point >= hash(key) (a point equal to it counts), wrapping to
//  the lowest point when hash(key) is past the last one. Hashes compare as
//  plain signed longs. removeNode(name) takes all its points off.
//  nodes() lists node names in sorted order. Adding a node twice or
//  removing an unknown one → IllegalArgumentException, vnodes < 1 →
//  IllegalArgumentException, nodeFor on an empty ring →
//  IllegalStateException. Use the given hash64 for real keys.
//
//      points A#0=100, B#0=200, C#0=300
//      nodeFor(key hashing to 150)   → "B"
//      nodeFor(key hashing to 200)   → "B"      (on the point)
//      nodeFor(key hashing to 301)   → "A"      (wraps around)
//      after removeNode("B"): 150    → "C"
//
//  hint: TreeMap.ceilingEntry(h), then firstEntry() when it returns null.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

class Exercise {

    /** FNV-1a 64 followed by the splitmix64 finalizer: a stable, well-spread string hash. */
    static long hash64(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        h ^= h >>> 30;
        h *= 0xbf58476d1ce4e5b9L;
        h ^= h >>> 27;
        h *= 0x94d049bb133111ebL;
        return h ^ (h >>> 31);
    }

    static final class Ring {
        private final int vnodes;
        private final ToLongFunction<String> hash;
        private final TreeMap<Long, String> points = new TreeMap<>();
        private final Set<String> nodes = new TreeSet<>();

        Ring(int vnodes, ToLongFunction<String> hash) {
            throw new UnsupportedOperationException("TODO");
        }

        void addNode(String name) {
            throw new UnsupportedOperationException("TODO");
        }

        void removeNode(String name) {
            throw new UnsupportedOperationException("TODO");
        }

        String nodeFor(String key) {
            throw new UnsupportedOperationException("TODO");
        }

        List<String> nodes() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static Ring toyRing(int vnodes, String... nodes) {
        var table = new HashMap<String, Long>(Map.of(
            "A#0", 100L, "B#0", 200L, "C#0", 300L, "A#1", 250L, "B#1", 50L,
            "k50", 50L, "k150", 150L, "k200", 200L, "k301", 301L, "k220", 220L));
        var ring = new Ring(vnodes, table::get);
        for (var n : nodes) ring.addNode(n);
        return ring;
    }

    static List<String> keys(int count) {
        return IntStream.range(0, count).mapToObj(i -> "key-" + i).collect(Collectors.toList());
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("a key belongs to the first point clockwise", () -> {
            var r = toyRing(1, "A", "B", "C");
            eq(r.nodeFor("k50"), "A");
            eq(r.nodeFor("k150"), "B");
            eq(r.nodeFor("k220"), "C");
        });
        test("a key exactly on a point belongs to that point", () ->
            eq(toyRing(1, "A", "B", "C").nodeFor("k200"), "B"));
        test("keys past the last point wrap to the first", () ->
            eq(toyRing(1, "A", "B", "C").nodeFor("k301"), "A"));
        test("virtual nodes give a node several points", () -> {
            var r = toyRing(2, "A", "B");
            eq(r.nodeFor("k50"), "B");
            eq(r.nodeFor("k150"), "B");
            eq(r.nodeFor("k220"), "A");
            eq(r.nodeFor("k301"), "B");
            eq(r.nodes(), List.of("A", "B"));
        });
        test("removing a node hands its keys to the next point only", () -> {
            var r = toyRing(1, "A", "B", "C");
            r.removeNode("B");
            eq(r.nodeFor("k150"), "C");
            eq(r.nodeFor("k200"), "C");
            eq(r.nodeFor("k50"), "A");
            eq(r.nodes(), List.of("A", "C"));
        });
        test("refuses bad vnode counts, duplicate and unknown nodes", () -> {
            raises(IllegalArgumentException.class, () -> new Ring(0, Exercise::hash64));
            var r = toyRing(1, "A");
            raises(IllegalArgumentException.class, () -> r.addNode("A"));
            raises(IllegalArgumentException.class, () -> r.removeNode("Z"));
        });
        test("an empty ring refuses lookups", () -> {
            var r = new Ring(8, Exercise::hash64);
            raises(IllegalStateException.class, () -> r.nodeFor("x"));
            r.addNode("solo");
            eq(r.nodeFor("x"), "solo");
            r.removeNode("solo");
            raises(IllegalStateException.class, () -> r.nodeFor("x"));
        });
        test("adding a node moves keys only onto the new node", () -> {
            var r = new Ring(100, Exercise::hash64);
            for (var n : List.of("A", "B", "C")) r.addNode(n);
            var before = new HashMap<String, String>();
            for (var k : keys(2000)) before.put(k, r.nodeFor(k));
            r.addNode("D");
            int moved = 0;
            for (var k : keys(2000)) {
                if (!r.nodeFor(k).equals(before.get(k))) {
                    eq(r.nodeFor(k), "D");
                    moved++;
                }
            }
            ok(moved > 300 && moved < 700, "about a quarter of 2000 keys should move, moved " + moved);
        });
        test("many virtual nodes spread keys evenly", () -> {
            var r = new Ring(200, Exercise::hash64);
            for (var n : List.of("A", "B", "C", "D")) r.addNode(n);
            var load = new TreeMap<String, Integer>();
            for (var k : keys(4000)) load.merge(r.nodeFor(k), 1, Integer::sum);
            eq(load.keySet(), Set.of("A", "B", "C", "D"));
            for (int n : load.values()) ok(n > 700 && n < 1300, "uneven load " + load);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
