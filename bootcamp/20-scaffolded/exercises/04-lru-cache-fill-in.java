// ─────────────────────────────────────────────────────────────────────────
//  04 · an LRU cache on LinkedHashMap (fill-in)             ★★☆ core
//  concepts: fill-in · LinkedHashMap access order · removeEldestEntry · LRU
//  run: java -cp ../../_lib/out 04-lru-cache-fill-in.java
// ─────────────────────────────────────────────────────────────────────────
//
//  FILL-IN: the cache is complete except for 3 holes. Replace each
//  `/* ___ */ hole()` with the expression its comment asks for; delete
//  hole() when you are done.
//  LruCache(capacity) keeps at most capacity entries. get(key) and
//  put(key, value) make a key the most recently used; when a put grows
//  the cache past capacity, the LEAST recently used key is evicted and
//  recorded in evicted(). contains(key) only looks: it must NOT change
//  which key is evicted next. keys() lists keys oldest-use first.
//
//      cap 2: put a, put b, get a, put c → evicted [b], keys [a, c]
//      cap 2: put a, put b, contains a, put c → evicted [a]
//
//  hint: read the javadoc of LinkedHashMap's three-argument constructor.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    // a hole: each "/* ___ */ hole()" below is yours to replace
    static <T> T hole() {
        throw new UnsupportedOperationException("TODO");
    }

    static final class LruCache<K, V> {
        private final int capacity;
        private final List<K> evicted = new ArrayList<>();
        private final LinkedHashMap<K, V> map;

        LruCache(int capacity) {
            this.capacity = capacity;
            // the last argument picks insertion order (false) or access order (true)
            this.map = new LinkedHashMap<>(16, 0.75f, /* ___ */ hole()) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    boolean evict = /* ___ */ hole(); // has this put pushed us past capacity?
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
            return /* ___ */ hole(); // look without touching the access order
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
