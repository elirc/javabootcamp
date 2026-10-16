// ─────────────────────────────────────────────────────────────────────────
//  01 · hash mod N sharding — SOLUTION                      ★☆☆ warm-up
//  concepts: sharding · hashCode · Math.floorMod · rebalancing cost
//  run: java -cp ../../_lib/out 01-hash-sharding.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  hash(key) mod N is the simplest partitioning there is: no lookup table,
//  any client can compute the owner, and keys spread as evenly as the hash
//  does. Memcached client libraries, early Redis sharding and countless
//  "user_id % 16" schemes work exactly like this.
//  The Java trap is the sign. hashCode() is any int, including negatives,
//  and Java's % keeps the sign of the dividend: -3 % 10 is -3, an index
//  that does not exist. Math.abs looks like the fix but is wrong twice:
//  it maps -3 to shard 3 (a different answer from every client that uses
//  floorMod), and Math.abs(Integer.MIN_VALUE) overflows back to
//  MIN_VALUE, so "polygenelubricants" would still land on shard -8.
//  Math.floorMod always returns 0..N-1.
//  The real lesson is movedKeys. Going from 4 to 5 shards moves about
//  4/5 of all keys, because a key stays put only when h mod 4 == h mod 5.
//  Doubling from 4 to 8 moves about half. Every moved key is a row to
//  copy and a cache entry to miss, which is why growing a mod-N cluster
//  is an outage-sized event, and why the next exercises (range
//  partitions, consistent hashing, rendezvous hashing) exist.
//  distribute pre-fills every shard so callers can iterate 0..N-1 without
//  null checks, and it keeps input order by appending as it walks.

import static bootcamp.Check.*;

import java.util.*;
import java.util.stream.*;

class Exercise {

    static int shardOf(String key, int shards) {
        if (shards < 1) throw new IllegalArgumentException("shards must be at least 1: " + shards);
        return Math.floorMod(key.hashCode(), shards);
    }

    static Map<Integer, List<String>> distribute(List<String> keys, int shards) {
        var out = new TreeMap<Integer, List<String>>();
        for (int s = 0; s < shards; s++) out.put(s, new ArrayList<>());
        for (var key : keys) out.get(shardOf(key, shards)).add(key);
        return out;
    }

    static int movedKeys(List<String> keys, int from, int to) {
        int moved = 0;
        for (var key : keys) {
            if (shardOf(key, from) != shardOf(key, to)) moved++;
        }
        return moved;
    }

    static List<String> users(int count) {
        return IntStream.range(0, count).mapToObj(i -> "user-" + i).collect(Collectors.toList());
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("a key's shard is its hash code mod the shard count", () -> {
            eq(shardOf("a", 10), 7);
            eq(shardOf("b", 10), 8);
            eq(shardOf("a", 4), 1);
        });
        test("the same key always lands on the same shard", () ->
            eq(shardOf("user-42", 16), shardOf("user-42", 16)));
        test("a negative hash code still gives a shard in range", () -> {
            eq(shardOf("polygenelubricants", 10), 2);
            eq(shardOf("negative-hash", 10), 6);
        });
        test("refuses fewer than one shard", () -> {
            raises(IllegalArgumentException.class, () -> shardOf("a", 0));
            raises(IllegalArgumentException.class, () -> shardOf("a", -4));
        });
        test("distribute lists every shard, keys in input order", () ->
            eq(distribute(List.of("a", "b", "k", "e"), 4),
               Map.of(0, List.of(), 1, List.of("a", "e"), 2, List.of("b"), 3, List.of("k"))));
        test("one shard holds every key", () ->
            eq(distribute(List.of("x", "y", "z"), 1), Map.of(0, List.of("x", "y", "z"))));
        test("growing from 4 to 5 shards moves most keys", () -> {
            eq(movedKeys(users(1000), 4, 5), 814);
            ok(movedKeys(users(1000), 4, 5) > 700);
        });
        test("doubling from 4 to 8 shards moves about half", () ->
            eq(movedKeys(users(1000), 4, 8), 501));
        test("keeping the shard count moves nothing", () ->
            eq(movedKeys(users(1000), 6, 6), 0));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
