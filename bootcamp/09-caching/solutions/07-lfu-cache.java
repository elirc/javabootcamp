// ─────────────────────────────────────────────────────────────────────────
//  07 · least frequently used — SOLUTION                    ★★★ stretch
//  concepts: LFU eviction · frequency buckets · LRU tie-break · O(1)
//  run: java -cp ../../_lib/out 07-lfu-cache.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  LRU is fooled by a scan: one pass over a million cold keys flushes
//  every hot key. LFU keeps what is used OFTEN, which survives scans; the
//  price is that frequency must be tracked and ties must be broken.
//  The O(1) design keeps three maps: key → value, key → frequency, and
//  frequency → LinkedHashSet of keys with that frequency, in the order
//  they reached it. A use moves the key from bucket f to the TAIL of
//  bucket f + 1, so the head of each bucket is that bucket's least
//  recently used key. minFreq names the lowest non-empty bucket, so the
//  victim is always buckets.get(minFreq).iterator().next().
//  minFreq is where the bugs live. When a use empties the minFreq bucket,
//  minFreq goes up by exactly one (the key just moved there). A brand
//  new key resets minFreq to 1, even if every other key is hotter: the
//  newcomer is now the coldest thing in the cache, and the next newcomer
//  evicts it. That is LFU's known weakness (new keys struggle to stay);
//  real systems use aging or W-TinyLFU's admission window.
//  Eviction happens BEFORE inserting the new key, otherwise the newcomer
//  (frequency 1, most recent) could be chosen as its own victim. Evicted
//  keys forget their history: coming back means starting at 1.

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
            if (capacity < 1) throw new IllegalArgumentException("capacity must be at least 1: " + capacity);
            this.capacity = capacity;
        }

        Optional<V> get(K key) {
            if (!values.containsKey(key)) return Optional.empty();
            touch(key);
            return Optional.of(values.get(key));
        }

        void put(K key, V value) {
            if (values.containsKey(key)) {
                values.put(key, value);
                touch(key);
                return;
            }
            if (values.size() == capacity) {
                var coldest = buckets.get(minFreq);
                var victim = coldest.iterator().next();
                coldest.remove(victim);
                values.remove(victim);
                freqs.remove(victim);
            }
            values.put(key, value);
            freqs.put(key, 1);
            buckets.computeIfAbsent(1, f -> new LinkedHashSet<>()).add(key);
            minFreq = 1;
        }

        int frequency(K key) {
            return freqs.getOrDefault(key, 0);
        }

        private void touch(K key) {
            int f = freqs.get(key);
            var bucket = buckets.get(f);
            bucket.remove(key);
            if (bucket.isEmpty() && f == minFreq) minFreq++;
            freqs.put(key, f + 1);
            buckets.computeIfAbsent(f + 1, n -> new LinkedHashSet<>()).add(key);
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
