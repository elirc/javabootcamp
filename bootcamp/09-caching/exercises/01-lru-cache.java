// ─────────────────────────────────────────────────────────────────────────
//  01 · least recently used                                 ★☆☆ warm-up
//  concepts: LRU eviction · LinkedHashMap access order · bounded caches
//  run: java -cp ../../_lib/out 01-lru-cache.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A cache is a small, fast map in front of something slow, and when it
//  is full it must forget something. LRU forgets the entry used longest
//  ago. Finish the nested class LruCache: get(key) returns an Optional
//  and a hit counts as a use; put(key, value) inserts or replaces (also a
//  use) and, only if the size now exceeds capacity, evicts the least
//  recently used key. A miss changes nothing. keysOldestFirst() returns a
//  copy of the keys from least to most recently used. A capacity below 1
//  is an IllegalArgumentException.
//
//      cap 2: put a, put b, get a, put c   → b is evicted
//      keysOldestFirst()                   → [a, c]
//      cap 2 holding a, b: put(a, 9)       → nothing evicted, [b, a]
//
//  hint: LinkedHashMap has a constructor flag and a hook made for this.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class LruCache<K, V> {
        private final int capacity;
        private final LinkedHashMap<K, V> entries;

        LruCache(int capacity) {
            throw new UnsupportedOperationException("TODO");
        }

        Optional<V> get(K key) {
            throw new UnsupportedOperationException("TODO");
        }

        void put(K key, V value) {
            throw new UnsupportedOperationException("TODO");
        }

        int size() {
            throw new UnsupportedOperationException("TODO");
        }

        List<K> keysOldestFirst() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("a miss is empty, a hit returns the value", () -> {
            var c = new LruCache<String, Integer>(2);
            eq(c.get("a"), Optional.empty());
            c.put("a", 1);
            eq(c.get("a"), Optional.of(1));
            eq(c.size(), 1);
        });
        test("evicts the least recently used key when full", () -> {
            var c = new LruCache<String, Integer>(2);
            c.put("a", 1);
            c.put("b", 2);
            c.put("c", 3);
            eq(c.size(), 2);
            eq(c.keysOldestFirst(), List.of("b", "c"));
        });
        test("a get counts as a use", () -> {
            var c = new LruCache<String, Integer>(2);
            c.put("a", 1);
            c.put("b", 2);
            c.get("a");
            c.put("c", 3);
            eq(c.keysOldestFirst(), List.of("a", "c"));
            eq(c.get("b"), Optional.empty());
        });
        test("replacing a value evicts nothing and counts as a use", () -> {
            var c = new LruCache<String, Integer>(2);
            c.put("a", 1);
            c.put("b", 2);
            c.put("a", 9);
            eq(c.size(), 2);
            eq(c.keysOldestFirst(), List.of("b", "a"));
            c.put("a", 9);
            eq(c.keysOldestFirst(), List.of("b", "a"));
            c.put("c", 3);
            eq(c.keysOldestFirst(), List.of("a", "c"));
            eq(c.get("a"), Optional.of(9));
        });
        test("a miss changes nothing", () -> {
            var c = new LruCache<String, Integer>(3);
            c.put("a", 1);
            c.put("b", 2);
            c.get("zzz");
            eq(c.size(), 2);
            eq(c.keysOldestFirst(), List.of("a", "b"));
        });
        test("capacity one keeps only the latest key", () -> {
            var c = new LruCache<String, Integer>(1);
            c.put("a", 1);
            c.put("b", 2);
            eq(c.keysOldestFirst(), List.of("b"));
            eq(c.get("a"), Optional.empty());
        });
        test("keysOldestFirst is a copy", () -> {
            var c = new LruCache<String, Integer>(2);
            c.put("a", 1);
            var keys = c.keysOldestFirst();
            c.put("b", 2);
            eq(keys, List.of("a"));
            eq(c.size(), 2);
        });
        test("refuses a capacity below one", () -> {
            raises(IllegalArgumentException.class, () -> new LruCache<String, Integer>(0));
            raises(IllegalArgumentException.class, () -> new LruCache<String, Integer>(-3));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
