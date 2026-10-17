// ─────────────────────────────────────────────────────────────────────────
//  01 · the cache that evicts the wrong key                 ★☆☆ warm-up
//  concepts: debugging · LRU eviction · recency on read · deque ends
//  run: java -cp ../../_lib/out 01-lru-wrong-victim.java
// ─────────────────────────────────────────────────────────────────────────
//
//  DEBUG HUNT: the code below is complete and compiles; it has bugs. Run
//  it, read the failing tests, find and fix the bugs. Do not edit tests.
//  Intended: LruCache(capacity) holds at most capacity keys. get and put
//  both make a key the most recently used. A put of a NEW key into a
//  full cache first evicts the LEAST recently used key and records it in
//  evicted(). Updating an existing key never evicts.
//  Bug reports:
//    "The product page we loaded a moment ago is the first thing the
//     cache throws away."
//    "Our hottest key, read on every request, keeps getting evicted."
//
//      cap 2: put a, put b, put c        → evicted [a]
//      cap 2: put a, put b, get a, put c → evicted [b]
//
//  hint: write the deque's contents next to each step of a failing test.

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
            return Optional.ofNullable(values.get(key));
        }

        void put(K key, V value) {
            if (values.size() == capacity && !values.containsKey(key)) {
                K victim = order.pollLast();
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
