// ─────────────────────────────────────────────────────────────────────────
//  07 · least frequently used                               ★★★ stretch
//  concepts: LFU eviction · frequency buckets · LRU tie-break · O(1)
//  run: java -cp ../../_lib/out 07-lfu-cache.java
// ─────────────────────────────────────────────────────────────────────────
//
//  LRU forgets a key used 1000 times the moment a scan walks past it; LFU
//  keeps what is used often. Finish the nested class LfuCache. A new key
//  starts at frequency 1; every get hit and every put on an existing key
//  (which replaces the value, evicting nothing) adds 1. When put brings a
//  NEW key into a full cache, first evict the key with the lowest
//  frequency; among equals, the one least recently used (got or put).
//  frequency(key) is 0 for an absent key, and an evicted key forgets its
//  count. A capacity below 1 is an IllegalArgumentException.
//
//      cap 2: put a, put b, get a, put c         → b evicted (1 < 2)
//      cap 2: put a, put b, get b, get a, put c  → b evicted (tie, older use)
//
//  Aim for O(1) per operation (the tests don't time it).
//  hint: group keys by frequency, and remember the smallest group.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class LfuCache<K, V> {
        private final int capacity;
        private final Map<K, V> values = new HashMap<>();
        private final Map<K, Integer> freqs = new HashMap<>();
        private final Map<Integer, LinkedHashSet<K>> buckets = new HashMap<>();
        private int minFreq;

        LfuCache(int capacity) {
            throw new UnsupportedOperationException("TODO");
        }

        Optional<V> get(K key) {
            throw new UnsupportedOperationException("TODO");
        }

        void put(K key, V value) {
            throw new UnsupportedOperationException("TODO");
        }

        int frequency(K key) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("a miss is empty and has frequency 0", () -> {
            var c = new LfuCache<String, Integer>(2);
            eq(c.get("a"), Optional.empty());
            eq(c.frequency("a"), 0);
            c.put("a", 1);
            eq(c.frequency("a"), 1);
            eq(c.get("a"), Optional.of(1));
            eq(c.frequency("a"), 2);
        });
        test("evicts the least frequently used key", () -> {
            var c = new LfuCache<String, Integer>(2);
            c.put("a", 1);
            c.put("b", 2);
            c.get("a");
            c.put("c", 3);
            eq(c.frequency("b"), 0);
            eq(c.get("a"), Optional.of(1));
            eq(c.get("c"), Optional.of(3));
        });
        test("ties go to the least recently used, not the oldest insert", () -> {
            var c = new LfuCache<String, Integer>(2);
            c.put("a", 1);
            c.put("b", 2);
            c.get("b");
            c.get("a");
            c.put("c", 3);
            eq(c.frequency("b"), 0);
            eq(c.frequency("a"), 2);
        });
        test("a newcomer is the next victim even when others are hot", () -> {
            var c = new LfuCache<String, Integer>(2);
            c.put("a", 1);
            c.get("a");
            c.get("a");
            c.put("b", 2);
            c.put("c", 3);
            eq(c.frequency("b"), 0);
            c.put("d", 4);
            eq(c.frequency("c"), 0);
            eq(c.frequency("a"), 3);
            eq(c.frequency("d"), 1);
        });
        test("the minimum moves up when its last key is promoted", () -> {
            var c = new LfuCache<String, Integer>(2);
            c.put("a", 1);
            c.put("b", 2);
            c.get("a");
            c.get("b");
            c.get("a");
            c.put("c", 3);
            eq(c.frequency("b"), 0);
            eq(c.frequency("a"), 3);
            eq(c.frequency("c"), 1);
        });
        test("emptying a bucket above the minimum leaves the minimum alone", () -> {
            var c = new LfuCache<String, Integer>(2);
            c.put("a", 1);
            c.get("a");
            c.put("b", 2);
            c.get("a");
            c.put("c", 3);
            eq(c.frequency("b"), 0);
            eq(c.frequency("a"), 3);
            eq(c.frequency("c"), 1);
        });
        test("put on an existing key replaces it and counts as a use", () -> {

            var c = new LfuCache<String, Integer>(2);
            c.put("a", 1);
            c.put("b", 2);
            c.put("a", 9);
            eq(c.frequency("a"), 2);
            c.put("c", 3);
            eq(c.get("a"), Optional.of(9));
            eq(c.frequency("b"), 0);
        });
        test("an evicted key starts over at frequency one", () -> {
            var c = new LfuCache<String, Integer>(1);
            c.put("a", 1);
            c.get("a");
            c.get("a");
            c.put("b", 2);
            eq(c.frequency("a"), 0);
            c.put("a", 5);
            eq(c.frequency("a"), 1);
            eq(c.get("b"), Optional.empty());
        });
        test("refuses a capacity below one", () -> {
            raises(IllegalArgumentException.class, () -> new LfuCache<String, Integer>(0));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
