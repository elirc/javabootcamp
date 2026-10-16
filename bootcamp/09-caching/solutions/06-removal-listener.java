// ─────────────────────────────────────────────────────────────────────────
//  06 · who left and why — SOLUTION                         ★★☆ core
//  concepts: removal callbacks · eviction causes · TTL + size · isolation
//  run: java -cp ../../_lib/out 06-removal-listener.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Caches that hold resources (open files, pooled connections, rendered
//  images) need to know when an entry leaves so they can close it, and
//  operators want to know WHY: lots of SIZE evictions means the cache is
//  too small, lots of EXPIRED means the TTL is short, REPLACED means hot
//  keys are being rewritten. Caffeine's RemovalCause has these four,
//  plus COLLECTED for entries lost to weak or soft references.
//  Every exit path goes through one notify method, so no path can forget
//  to report. The interesting rules are in put:
//  1. An old entry for the same key is REPLACED only if it was still
//     alive; a dead one was already gone, so it is reported EXPIRED.
//  2. When the cache overflows, dead entries leave first. Evicting a live
//     entry for SIZE while a corpse still occupies a slot throws away a
//     useful value for nothing (a pure-LRU sweep makes exactly that
//     mistake when the corpse was recently read).
//  3. Only then, if still too big, evict least recently used. The entry
//     just written is the most recently used, so it never goes.
//  Listener code is someone else's code. If its exception escaped, a put
//  would fail halfway (entry stored, eviction skipped) and the cache would
//  stay over capacity. So notify catches RuntimeException, counts it, and
//  moves on; Caffeine does the same and logs it.

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
            var e = entries.get(key);
            if (e == null) return Optional.empty();
            if (!alive(e)) {
                entries.remove(key);
                notify(key, e.value(), Cause.EXPIRED);
                return Optional.empty();
            }
            return Optional.of(e.value());
        }

        void put(K key, V value) {
            var old = entries.put(key, new Entry<>(value, clock.getAsLong() + ttlMillis));
            if (old != null) notify(key, old.value(), alive(old) ? Cause.REPLACED : Cause.EXPIRED);
            if (entries.size() <= capacity) return;
            var dead = new ArrayList<K>();
            entries.forEach((k, e) -> {
                if (!alive(e)) dead.add(k);
            });
            for (var k : dead) notify(k, entries.remove(k).value(), Cause.EXPIRED);
            var it = entries.entrySet().iterator();
            while (entries.size() > capacity) {
                var eldest = it.next();
                it.remove();
                notify(eldest.getKey(), eldest.getValue().value(), Cause.SIZE);
            }
        }

        void invalidate(K key) {
            var e = entries.remove(key);
            if (e != null) notify(key, e.value(), alive(e) ? Cause.EXPLICIT : Cause.EXPIRED);
        }

        int listenerErrors() {
            return listenerErrors;
        }

        private boolean alive(Entry<V> e) {
            return clock.getAsLong() < e.expiresAt();
        }

        private void notify(K key, V value, Cause cause) {
            try {
                listener.onRemoval(key, value, cause);
            } catch (RuntimeException ex) {
                listenerErrors++;
            }
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
        test("a put that fits leaves dead entries alone", () -> {
            var log = new Log();
            var clock = new FakeClock();
            var c = new ListeningCache<String, Integer>(3, 100, clock, log);
            c.put("a", 1);
            clock.now = 100;
            c.put("b", 2);
            c.put("c", 3);
            eq(log.events, List.of());
            eq(c.get("a"), Optional.empty());
            eq(log.events, List.of("a=1 EXPIRED"));
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
