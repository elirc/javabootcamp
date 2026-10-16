// ─────────────────────────────────────────────────────────────────────────
//  04 · a read-through cache with stats — SOLUTION          ★★☆ core
//  concepts: read-through loading · hit rate · eviction counts · LRU
//  run: java -cp ../../_lib/out 04-loading-cache.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Read-through moves the "on a miss, load and remember" logic from every
//  caller into the cache itself: callers just say get(key), and the
//  loader is the only code that knows about the slow backend. That is the
//  shape of Guava's LoadingCache and Caffeine's get(key, loader).
//  The stats are what make a cache operable. A dashboard shows hitRate;
//  if it is 0.2 the cache is mostly overhead, and a rising evictions
//  count says the capacity is too small for the working set.
//  The counting rules: every get is exactly one hit or one miss, decided
//  before the loader runs; loads counts only loads that produced a value,
//  so a failing backend shows up as misses without loads. A loader that
//  throws or returns null caches NOTHING: caching an exception would turn
//  one hiccup into an outage for that key, and null is not a value.
//  invalidate is a deliberate removal, not an eviction, so it is not
//  counted: evictions measures capacity pressure only.
//  hitRate guards the 0 / 0 case; (double) hits / requests with 0
//  requests is NaN, which then poisons every average it touches.
//  Eviction reuses the LinkedHashMap access-order trick from exercise 01,
//  with the counter bumped inside removeEldestEntry.

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
            if (capacity < 1) throw new IllegalArgumentException("capacity must be at least 1: " + capacity);
            this.loader = loader;
            this.entries = new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    if (size() <= capacity) return false;
                    evictions++;
                    return true;
                }
            };
        }

        V get(K key) {
            var hit = entries.get(key);
            if (hit != null) {
                hits++;
                return hit;
            }
            misses++;
            var loaded = loader.apply(key);
            if (loaded == null) throw new IllegalStateException("loader returned null for " + key);
            loads++;
            entries.put(key, loaded);
            return loaded;
        }

        void invalidate(K key) {
            entries.remove(key);
        }

        Stats stats() {
            return new Stats(hits, misses, loads, evictions);
        }

        double hitRate() {
            long requests = hits + misses;
            return requests == 0 ? 0.0 : (double) hits / requests;
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
