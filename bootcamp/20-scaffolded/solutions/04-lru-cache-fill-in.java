// ─────────────────────────────────────────────────────────────────────────
//  04 · an LRU cache on LinkedHashMap (fill-in) — SOLUTION  ★★☆ core
//  concepts: fill-in · LinkedHashMap access order · removeEldestEntry · LRU
//  run: java -cp ../../_lib/out 04-lru-cache-fill-in.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  The three holes:
//    accessOrder  true
//    evict        size() > capacity
//    contains     map.containsKey(key)
//  LinkedHashMap is a hash map threaded with a linked list. With
//  accessOrder = false (the default) the list is INSERTION order, which
//  makes a FIFO cache: a hot key read a thousand times is still evicted
//  first. true moves an entry to the end on every get and put, so the
//  head of the list is always the least recently used key.
//  removeEldestEntry is called after every put, with the head entry;
//  returning true removes it. size() is already the new size, so the
//  test is size() > capacity; >= would keep the cache one short.
//  contains is the subtle one: in an access-ordered map, get (and even
//  getOrDefault) counts as an access, so "map.get(key) != null" would
//  silently refresh the key and change which one is evicted next.
//  containsKey does not touch the order. Module 09 builds the same cache
//  by hand with a HashMap and a doubly linked list.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class LruCache<K, V> {
        private final int capacity;
        private final List<K> evicted = new ArrayList<>();
        private final LinkedHashMap<K, V> map;

        LruCache(int capacity) {
            this.capacity = capacity;
            this.map = new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    boolean evict = size() > capacity;
                    if (evict) evicted.add(eldest.getKey());
                    return evict;
                }
            };
        }

        Optional<V> get(K key) {
            return Optional.ofNullable(map.get(key));
        }

        void put(K key, V value) {
            map.put(key, value);
        }

        boolean contains(K key) {
            return map.containsKey(key);
        }

        List<K> keys() {
            return List.copyOf(map.keySet());
        }

        List<K> evicted() {
            return List.copyOf(evicted);
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("stores and returns values", () -> {
            var c = new LruCache<String, Integer>(3);
            c.put("a", 1);
            c.put("b", 2);
            eq(c.get("a"), Optional.of(1));
            eq(c.get("zzz"), Optional.empty());
        });
        test("evicts the least recently used key when full", () -> {
            var c = new LruCache<String, Integer>(2);
            c.put("a", 1);
            c.put("b", 2);
            c.put("c", 3);
            eq(c.evicted(), List.of("a"));
            eq(c.keys(), List.of("b", "c"));
        });
        test("get makes a key recent", () -> {
            var c = new LruCache<String, Integer>(2);
            c.put("a", 1);
            c.put("b", 2);
            c.get("a");
            c.put("c", 3);
            eq(c.evicted(), List.of("b"));
            eq(c.keys(), List.of("a", "c"));
        });
        test("putting an existing key refreshes it and evicts nothing", () -> {
            var c = new LruCache<String, Integer>(2);
            c.put("a", 1);
            c.put("b", 2);
            c.put("a", 10);
            eq(c.evicted(), List.of());
            eq(c.get("a"), Optional.of(10));
            c.put("c", 3);
            eq(c.evicted(), List.of("b"));
        });
        test("contains looks without making a key recent", () -> {
            var c = new LruCache<String, Integer>(2);
            c.put("a", 1);
            c.put("b", 2);
            ok(c.contains("a"), "a is cached");
            ok(!c.contains("x"), "x is not");
            c.put("c", 3);
            eq(c.evicted(), List.of("a"));
        });
        test("a capacity of 1 keeps only the latest key", () -> {
            var c = new LruCache<String, Integer>(1);
            c.put("a", 1);
            c.put("b", 2);
            c.put("c", 3);
            eq(c.evicted(), List.of("a", "b"));
            eq(c.keys(), List.of("c"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
