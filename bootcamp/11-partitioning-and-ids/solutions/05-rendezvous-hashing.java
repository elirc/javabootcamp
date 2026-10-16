// ─────────────────────────────────────────────────────────────────────────
//  05 · rendezvous hashing — SOLUTION                       ★★☆ core
//  concepts: highest random weight · replica placement · comparators
//  run: java -cp ../../_lib/out 05-rendezvous-hashing.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Rendezvous hashing (Thaler & Ravishankar, 1996) gets the same "only
//  1/N of the keys move" property as a ring with far less machinery: no
//  sorted structure, no virtual nodes. Each node's score for a key is an
//  independent pseudo-random number, so each node wins about 1/N of the
//  keys. Remove a node and only the keys it was winning need a new owner
//  (their runner-up); every other key's winner still has the highest
//  score. Add a node and it only steals the keys where it now scores
//  highest. The cost is O(N) hashing per lookup, which is nothing for the
//  tens of nodes most clusters have.
//  replicas() is where it shines: sorting all nodes by score gives a
//  per-key preference list. Take the top 3 and you have replica placement
//  that stays stable as nodes come and go; when the primary dies, the
//  next node in the list is already a replica. Apache Ignite's default
//  affinity function places partitions exactly this way.
//  The comparator must be TOTAL. Comparing scores alone lets two nodes
//  with equal scores come out in input order, so two servers holding the
//  node list in different orders would disagree about the owner, which is
//  exactly what a placement function must never do. Breaking ties on the
//  name makes the answer a function of the set, not the list. Use
//  Long.compare (not subtraction, which overflows on large hashes), and
//  reverse only the score part.

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

    static Comparator<String> byScore(String key, ToLongFunction<String> hash) {
        Comparator<String> score = Comparator.comparingLong(node -> hash.applyAsLong(node + "|" + key));
        return score.reversed().thenComparing(Comparator.naturalOrder());
    }

    static String owner(String key, Collection<String> nodes, ToLongFunction<String> hash) {
        if (nodes.isEmpty()) throw new IllegalArgumentException("no nodes");
        return nodes.stream().min(byScore(key, hash)).orElseThrow();
    }

    static List<String> replicas(String key, Collection<String> nodes, int n, ToLongFunction<String> hash) {
        if (nodes.isEmpty()) throw new IllegalArgumentException("no nodes");
        if (n < 1) throw new IllegalArgumentException("n must be at least 1: " + n);
        return nodes.stream().sorted(byScore(key, hash)).limit(n).collect(Collectors.toList());
    }

    static ToLongFunction<String> toy() {
        var table = Map.of("A|k", 10L, "B|k", 30L, "C|k", 20L,
                           "A|t", 7L, "B|t", 7L, "C|t", -50L,
                           "A|m", Long.MIN_VALUE, "B|m", Long.MAX_VALUE, "C|m", 0L);
        return table::get;
    }

    static List<String> keys(int count) {
        return IntStream.range(0, count).mapToObj(i -> "key-" + i).collect(Collectors.toList());
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("the node with the highest score owns the key", () -> {
            eq(owner("k", List.of("A", "B", "C"), toy()), "B");
            eq(owner("m", List.of("A", "B", "C"), toy()), "B");
        });
        test("a tied score goes to the alphabetically smaller name", () -> {
            eq(owner("t", List.of("B", "A", "C"), toy()), "A");
            eq(replicas("t", List.of("B", "C", "A"), 3, toy()), List.of("A", "B", "C"));
        });
        test("the order of the node list does not matter", () -> {
            eq(owner("k", List.of("C", "B", "A"), toy()), "B");
            eq(owner("k", List.of("B", "C", "A"), toy()), "B");
        });
        test("replicas are the best-scoring nodes, best first", () -> {
            eq(replicas("k", List.of("C", "A", "B"), 2, toy()), List.of("B", "C"));
            eq(replicas("k", List.of("A", "B", "C"), 1, toy()), List.of("B"));
            eq(replicas("m", List.of("A", "B", "C"), 3, toy()), List.of("B", "C", "A"));
        });
        test("asking for more replicas than nodes returns them all", () ->
            eq(replicas("k", List.of("A", "B", "C"), 9, toy()), List.of("B", "C", "A")));
        test("refuses an empty node list and n < 1", () -> {
            raises(IllegalArgumentException.class, () -> owner("k", List.of(), toy()));
            raises(IllegalArgumentException.class, () -> replicas("k", List.of(), 2, toy()));
            raises(IllegalArgumentException.class, () -> replicas("k", List.of("A"), 0, toy()));
        });
        test("removing a node only moves the keys it owned", () -> {
            var all = List.of("n1", "n2", "n3", "n4", "n5");
            var fewer = List.of("n1", "n2", "n4", "n5");
            int moved = 0;
            for (var k : keys(2000)) {
                var before = owner(k, all, Exercise::hash64);
                var after = owner(k, fewer, Exercise::hash64);
                if (!before.equals(after)) {
                    eq(before, "n3");
                    eq(after, replicas(k, all, 2, Exercise::hash64).get(1));
                    moved++;
                }
            }
            ok(moved > 250 && moved < 550, "about a fifth of 2000 keys should move, moved " + moved);
        });
        test("adding a node only steals keys for itself", () -> {
            var old = List.of("n1", "n2", "n3");
            var grown = List.of("n1", "n2", "n3", "n4");
            for (var k : keys(2000)) {
                var after = owner(k, grown, Exercise::hash64);
                if (!after.equals(owner(k, old, Exercise::hash64))) eq(after, "n4");
            }
        });
        test("spreads keys roughly evenly", () -> {
            var load = new TreeMap<String, Integer>();
            for (var k : keys(3000)) load.merge(owner(k, List.of("x", "y", "z"), Exercise::hash64), 1, Integer::sum);
            eq(load.keySet(), Set.of("x", "y", "z"));
            for (int n : load.values()) ok(n > 800 && n < 1200, "uneven load " + load);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
