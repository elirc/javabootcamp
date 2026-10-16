// ─────────────────────────────────────────────────────────────────────────
//  06 · who left and why                                    ★★☆ core
//  concepts: removal callbacks · eviction causes · TTL + size · isolation
//  run: java -cp ../../_lib/out 06-removal-listener.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Finish ListeningCache: LRU, at most capacity entries, each alive while
//  now < putTime + ttl. Every removal calls listener.onRemoval(key,
//  value, cause) with the value that left:
//    get of a dead entry            → removes it, EXPIRED, returns empty
//    invalidate                     → EXPLICIT if alive, EXPIRED if dead,
//                                     nothing if absent
//    put over an existing key       → old value REPLACED if alive,
//                                     EXPIRED if dead
//    put that overflows capacity    → first EVERY dead entry leaves
//      (EXPIRED, least recently used first); then, if still too many,
//      least recently used live entries leave (SIZE)
//  get hits and puts count as uses. A listener that throws a
//  RuntimeException must not break the cache: count it (listenerErrors).
//
//      cap 2: put a, put b, get a, put c   → [b=2 SIZE]
//
//  hint: route every removal through one private notify method.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.LongSupplier;

class Exercise {

    enum Cause { EXPLICIT, REPLACED, EXPIRED, SIZE }

    interface RemovalListener<K, V> {
        void onRemoval(K key, V value, Cause cause);
    }

    static final class ListeningCache<K, V> {
        private record Entry<V>(V value, long expiresAt) {}

        private final int capacity;
        private final long ttlMillis;
        private final LongSupplier clock;
        private final RemovalListener<K, V> listener;
        private final LinkedHashMap<K, Entry<V>> entries = new LinkedHashMap<>(16, 0.75f, true);
        private int listenerErrors;

        ListeningCache(int capacity, long ttlMillis, LongSupplier clock, RemovalListener<K, V> listener) {
            this.capacity = capacity;
            this.ttlMillis = ttlMillis;
            this.clock = clock;
            this.listener = listener;
        }

        Optional<V> get(K key) {
            throw new UnsupportedOperationException("TODO");
        }

        void put(K key, V value) {
            throw new UnsupportedOperationException("TODO");
        }

        void invalidate(K key) {
            throw new UnsupportedOperationException("TODO");
        }

        int listenerErrors() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class FakeClock implements LongSupplier {
        long now;

        public long getAsLong() {
            return now;
        }
    }

    static final class Log implements RemovalListener<String, Integer> {
        final List<String> events = new ArrayList<>();

        public void onRemoval(String key, Integer value, Cause cause) {
            events.add(key + "=" + value + " " + cause);
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("invalidate reports EXPLICIT with the value", () -> {
            var log = new Log();
            var c = new ListeningCache<String, Integer>(2, 100, new FakeClock(), log);
            c.put("a", 1);
            c.invalidate("a");
            c.invalidate("a");
            c.invalidate("never");
            eq(log.events, List.of("a=1 EXPLICIT"));
            eq(c.get("a"), Optional.empty());
        });
        test("replacing reports REPLACED with the old value", () -> {
            var log = new Log();
            var c = new ListeningCache<String, Integer>(2, 100, new FakeClock(), log);
            c.put("a", 1);
            c.put("a", 2);
            eq(log.events, List.of("a=1 REPLACED"));
            eq(c.get("a"), Optional.of(2));
        });
        test("overflow evicts the least recently used with SIZE", () -> {
            var log = new Log();
            var c = new ListeningCache<String, Integer>(2, 100, new FakeClock(), log);
            c.put("a", 1);
            c.put("b", 2);
            c.get("a");
            c.put("c", 3);
            eq(log.events, List.of("b=2 SIZE"));
            eq(c.get("a"), Optional.of(1));
            eq(c.get("c"), Optional.of(3));
        });
        test("dead entries leave before anyone is evicted for size", () -> {
            var log = new Log();
            var clock = new FakeClock();
            var c = new ListeningCache<String, Integer>(2, 100, clock, log);
            c.put("a", 1);
            clock.now = 50;
            c.put("b", 2);
            clock.now = 60;
            c.get("a");
            clock.now = 110;
            c.put("c", 3);
            eq(log.events, List.of("a=1 EXPIRED"));
            eq(c.get("b"), Optional.of(2));
            eq(c.get("c"), Optional.of(3));
        });
        test("several dead entries are reported least recently used first", () -> {
            var log = new Log();
            var clock = new FakeClock();
            var c = new ListeningCache<String, Integer>(2, 100, clock, log);
            c.put("a", 1);
            c.put("b", 2);
            c.get("a");
            clock.now = 500;
            c.put("c", 3);
            eq(log.events, List.of("b=2 EXPIRED", "a=1 EXPIRED"));
        });
        test("touching an expired entry reports EXPIRED", () -> {
            var log = new Log();
            var clock = new FakeClock();
            var c = new ListeningCache<String, Integer>(2, 100, clock, log);
            c.put("a", 1);
            c.put("b", 2);
            clock.now = 100;
            eq(c.get("a"), Optional.empty());
            eq(c.get("a"), Optional.empty());
            c.invalidate("b");
            eq(log.events, List.of("a=1 EXPIRED", "b=2 EXPIRED"));
        });
        test("putting over an expired key reports EXPIRED, not REPLACED", () -> {
            var log = new Log();
            var clock = new FakeClock();
            var c = new ListeningCache<String, Integer>(2, 100, clock, log);
            c.put("a", 1);
            clock.now = 100;
            c.put("a", 2);
            eq(log.events, List.of("a=1 EXPIRED"));
            eq(c.get("a"), Optional.of(2));
        });
        test("a throwing listener does not break the cache", () -> {
            var c = new ListeningCache<String, Integer>(1, 100, new FakeClock(),
                (k, v, cause) -> { throw new IllegalStateException("listener bug"); });
            c.put("a", 1);
            c.put("b", 2);
            c.put("b", 3);
            eq(c.get("a"), Optional.empty());
            eq(c.get("b"), Optional.of(3));
            eq(c.listenerErrors(), 2);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
