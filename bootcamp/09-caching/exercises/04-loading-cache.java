// ─────────────────────────────────────────────────────────────────────────
//  04 · a read-through cache with stats                     ★★☆ core
//  concepts: read-through loading · hit rate · eviction counts · LRU
//  run: java -cp ../../_lib/out 04-loading-cache.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Callers should not care whether a value came from the cache: they call
//  get(key), and on a miss the cache itself calls the loader and keeps
//  the result. Finish the nested class LoadingCache (LRU-bounded, like
//  01). Every get is exactly one hit or one miss. loads counts loader
//  calls that returned a value; evictions counts entries dropped for
//  capacity. If the loader throws, the exception propagates; if it
//  returns null, throw IllegalStateException; either way nothing is
//  cached. invalidate(key) removes a key and is NOT an eviction.
//  hitRate() = hits / (hits + misses), and 0.0 before any request.
//  A capacity below 1 is an IllegalArgumentException.
//
//      cap 2: get a, get a        → stats (hits 1, misses 1, loads 1, ev 0)
//      get a x4                   → hitRate 0.75
//
//  hint: count the hit or miss before you call the loader.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.Function;

class Exercise {

    record Stats(long hits, long misses, long loads, long evictions) {}

    static final class LoadingCache<K, V> {
        private final Function<K, V> loader;
        private final LinkedHashMap<K, V> entries;
        private long hits, misses, loads, evictions;

        LoadingCache(int capacity, Function<K, V> loader) {
            throw new UnsupportedOperationException("TODO");
        }

        V get(K key) {
            throw new UnsupportedOperationException("TODO");
        }

        void invalidate(K key) {
            throw new UnsupportedOperationException("TODO");
        }

        Stats stats() {
            throw new UnsupportedOperationException("TODO");
        }

        double hitRate() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class Backend implements Function<String, String> {
        final List<String> calls = new ArrayList<>();

        public String apply(String key) {
            calls.add(key);
            if (key.equals("boom")) throw new IllegalArgumentException("backend failed for boom");
            if (key.equals("ghost")) return null;
            return key.toUpperCase(Locale.ROOT);
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("a miss loads once, the next get is a hit", () -> {
            var backend = new Backend();
            var c = new LoadingCache<String, String>(2, backend);
            eq(c.get("a"), "A");
            eq(c.get("a"), "A");
            eq(backend.calls, List.of("a"));
            eq(c.stats(), new Stats(1, 1, 1, 0));
        });
        test("evicts the least recently used key and counts it", () -> {
            var backend = new Backend();
            var c = new LoadingCache<String, String>(2, backend);
            c.get("a");
            c.get("b");
            c.get("a");
            c.get("c");
            c.get("a");
            c.get("b");
            eq(backend.calls, List.of("a", "b", "c", "b"));
            eq(c.stats(), new Stats(2, 4, 4, 2));
        });
        test("a failing loader propagates and caches nothing", () -> {
            var backend = new Backend();
            var c = new LoadingCache<String, String>(2, backend);
            raises(IllegalArgumentException.class, () -> c.get("boom"), "boom");
            raises(IllegalArgumentException.class, () -> c.get("boom"));
            eq(backend.calls, List.of("boom", "boom"));
            eq(c.stats(), new Stats(0, 2, 0, 0));
        });
        test("a null from the loader is an IllegalStateException", () -> {
            var backend = new Backend();
            var c = new LoadingCache<String, String>(2, backend);
            raises(IllegalStateException.class, () -> c.get("ghost"));
            raises(IllegalStateException.class, () -> c.get("ghost"));
            eq(backend.calls.size(), 2);
            eq(c.stats(), new Stats(0, 2, 0, 0));
        });
        test("hitRate is hits over requests", () -> {
            var c = new LoadingCache<String, String>(2, new Backend());
            c.get("a");
            c.get("a");
            c.get("a");
            c.get("a");
            approx(c.hitRate(), 0.75, 1e-9);
        });
        test("hitRate is 0.0 before any request", () -> {
            var c = new LoadingCache<String, String>(2, new Backend());
            eq(c.hitRate(), 0.0);
            eq(c.stats(), new Stats(0, 0, 0, 0));
        });
        test("invalidate forces a reload and is not an eviction", () -> {
            var backend = new Backend();
            var c = new LoadingCache<String, String>(2, backend);
            c.get("a");
            c.invalidate("a");
            c.invalidate("never-loaded");
            c.get("a");
            eq(backend.calls, List.of("a", "a"));
            eq(c.stats(), new Stats(0, 2, 2, 0));
        });
        test("refuses a capacity below one", () -> {
            raises(IllegalArgumentException.class, () -> new LoadingCache<String, String>(0, new Backend()));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
