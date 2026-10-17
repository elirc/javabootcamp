// ─────────────────────────────────────────────────────────────────────────
//  01 · the cache that evicts the wrong key — SOLUTION      ★☆☆ warm-up
//  concepts: debugging · LRU eviction · recency on read · deque ends
//  run: java -cp ../../_lib/out 01-lru-wrong-victim.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  1. Reproduce report 1 with the smallest case: capacity 2, put a, b, c.
//     The cache evicts c's predecessor b, not a. Narrow: write the deque
//     after each put: [a], [a, b]. The comment says least recent FIRST,
//     and the eviction takes pollLast(), the MOST recent end. Fix:
//     pollFirst(). Pinned by "a full cache evicts the least recently
//     used key".
//  2. Re-run. "reading a key makes it recent" still fails: put a, b,
//     get a, put c evicts a. get() reads the map and never touches the
//     deque, so recency only counts writes; a key read on every request
//     ages like one never read. That is a FIFO cache, not an LRU. Fix:
//     on a hit, move the key to the back of the deque. Pinned by the
//     same test and "a hot key survives a stream of one-off keys".
//  Note that fixing only bug 1 makes the cache look right for writes
//  alone, which is how a cache like this passes review. ArrayDeque's
//  remove(Object) is O(n); fine for a small cache, and the reason real
//  LRUs use a hash map of doubly linked nodes, which is what
//  LinkedHashMap in access order is (module 09 exercise 01, module 20
//  exercise 04).

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class LruCache<K, V> {
        private final int capacity;
        private final Map<K, V> values = new HashMap<>();
        private final Deque<K> order = new ArrayDeque<>(); // least recent first
        private final List<K> evicted = new ArrayList<>();

        LruCache(int capacity) {
            this.capacity = capacity;
        }

        Optional<V> get(K key) {
            if (values.containsKey(key)) {
                order.remove(key);
                order.addLast(key);
            }
            return Optional.ofNullable(values.get(key));
        }

        void put(K key, V value) {
            if (values.size() == capacity && !values.containsKey(key)) {
                K victim = order.pollFirst();
                values.remove(victim);
                evicted.add(victim);
            }
            values.put(key, value);
            order.remove(key);
            order.addLast(key);
        }

        List<K> evicted() {
            return List.copyOf(evicted);
        }

        Set<K> keys() {
            return Set.copyOf(values.keySet());
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("stores and returns values", () -> {
            var c = new LruCache<String, Integer>(2);
            c.put("a", 1);
            c.put("b", 2);
            eq(c.get("a"), Optional.of(1));
            eq(c.get("nope"), Optional.empty());
            eq(c.evicted(), List.of());
        });
        test("a full cache evicts the least recently used key", () -> {
            var c = new LruCache<String, Integer>(2);
            c.put("a", 1);
            c.put("b", 2);
            c.put("c", 3);
            eq(c.evicted(), List.of("a"));
            eq(c.keys(), Set.of("b", "c"));
        });
        test("reading a key makes it recent", () -> {
            var c = new LruCache<String, Integer>(2);
            c.put("a", 1);
            c.put("b", 2);
            c.get("a");
            c.put("c", 3);
            eq(c.evicted(), List.of("b"));
            eq(c.get("a"), Optional.of(1));
        });
        test("updating a key makes it recent and evicts nothing", () -> {
            var c = new LruCache<String, Integer>(2);
            c.put("a", 1);
            c.put("b", 2);
            c.put("a", 10);
            eq(c.evicted(), List.of());
            c.put("c", 3);
            eq(c.evicted(), List.of("b"));
            eq(c.get("a"), Optional.of(10));
        });
        test("a hot key survives a stream of one-off keys", () -> {
            var c = new LruCache<String, Integer>(3);
            c.put("hot", 0);
            for (int i = 0; i < 20; i++) {
                c.get("hot");
                c.put("k" + i, i);
            }
            eq(c.get("hot"), Optional.of(0));
            eq(c.evicted().size(), 18);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
