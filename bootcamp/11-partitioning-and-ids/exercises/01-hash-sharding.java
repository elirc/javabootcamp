// ─────────────────────────────────────────────────────────────────────────
//  01 · hash mod N sharding                                 ★☆☆ warm-up
//  concepts: sharding · hashCode · Math.floorMod · rebalancing cost
//  run: java -cp ../../_lib/out 01-hash-sharding.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A user service has outgrown one database, so rows are split across N
//  shards by hashing the key. Write three static methods:
//  shardOf(key, shards) is the key's hashCode() modulo shards, always in
//  0..shards-1 (a hash of -3 with 10 shards is shard 7, never -3);
//  shards < 1 → IllegalArgumentException. distribute(keys, shards) maps
//  EVERY shard number 0..shards-1 to its keys in input order (an unused
//  shard maps to an empty list). movedKeys(keys, from, to) counts the keys
//  whose shard changes when the shard count goes from `from` to `to`.
//
//      shardOf("a", 10)                     → 7      ("a".hashCode() is 97)
//      shardOf("polygenelubricants", 10)    → 2      (hashCode is MIN_VALUE)
//      distribute(["a", "b"], 3)            → {0=[], 1=[a], 2=[b]}
//      movedKeys(1000 keys, 4, 5)           → most of them
//
//  hint: Math.abs(Integer.MIN_VALUE) is still negative.

import static bootcamp.Check.*;

import java.util.*;
import java.util.stream.*;

class Exercise {

    static int shardOf(String key, int shards) {
        throw new UnsupportedOperationException("TODO");
    }

    static Map<Integer, List<String>> distribute(List<String> keys, int shards) {
        throw new UnsupportedOperationException("TODO");
    }

    static int movedKeys(List<String> keys, int from, int to) {
        throw new UnsupportedOperationException("TODO");
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
