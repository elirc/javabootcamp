// ─────────────────────────────────────────────────────────────────────────
//  01 · least recently used — SOLUTION                      ★☆☆ warm-up
//  concepts: LRU eviction · LinkedHashMap access order · bounded caches
//  run: java -cp ../../_lib/out 01-lru-cache.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  An LRU cache needs two things at once: O(1) lookup by key and an order
//  of "how recently was this used". A HashMap gives the first, a linked
//  list the second; LinkedHashMap is exactly that pair. Built with
//  accessOrder = true, every get and every put of an existing key moves
//  the entry to the tail, so the head is always the least recently used.
//  Overriding removeEldestEntry turns it into a bounded cache: put calls
//  it after inserting, and returning true drops the head.
//  The classic wrong turn is the default insertion order (FIFO): reads
//  never move anything, and the hottest key is the first one evicted.
//  The second is evicting BEFORE the put: replacing a key on a full cache
//  then throws away an innocent entry although the size never grows.
//  Java trap: inside the anonymous subclass, size() is the map's own
//  size, and capacity must be a final field (or effectively final local)
//  for the override to see it. keysOldestFirst returns a copy; handing
//  out keySet() would let callers remove entries behind the cache's back.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class LruCache<K, V> {
        private final int capacity;
        private final LinkedHashMap<K, V> entries;

        LruCache(int capacity) {
            if (capacity < 1) throw new IllegalArgumentException("capacity must be at least 1: " + capacity);
            this.capacity = capacity;
            this.entries = new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    return size() > LruCache.this.capacity;
                }
            };
        }

        Optional<V> get(K key) {
            return Optional.ofNullable(entries.get(key));
        }

        void put(K key, V value) {
            entries.put(key, value);
        }

        int size() {
            return entries.size();
        }

        List<K> keysOldestFirst() {
            return new ArrayList<>(entries.keySet());
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
