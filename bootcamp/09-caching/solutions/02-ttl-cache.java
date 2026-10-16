// ─────────────────────────────────────────────────────────────────────────
//  02 · entries that expire — SOLUTION                      ★☆☆ warm-up
//  concepts: TTL expiry · injected clock · lazy vs eager purge
//  run: java -cp ../../_lib/out 02-ttl-cache.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Store the moment an entry dies, not the moment it was born: with
//  expiresAt = now + ttl, "is it alive?" is one comparison, now <
//  expiresAt, and the boundary is unambiguous. At exactly expiresAt the
//  entry is dead, so it lived for ttl milliseconds, not ttl + 1.
//  Expiry here is lazy: get notices a dead entry and removes it, and
//  purgeExpired sweeps the rest (a real cache runs that sweep on a timer
//  or piggybacks it on writes). size() must not count corpses, so it
//  filters by the same alive() rule rather than returning map.size().
//  The wrong turns: refreshing expiresAt on get (that is "expire after
//  access", a different policy, and it keeps a hot stale value alive
//  forever); using <= so entries live one millisecond too long; and
//  reading System.currentTimeMillis() directly, which makes every test
//  either slow (sleep) or flaky. Injecting the clock as a LongSupplier
//  is what lets the tests jump to t = 99 and t = 100 exactly.
//  values().removeIf deletes in place without a
//  ConcurrentModificationException; counting first gives the return.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.LongSupplier;

class Exercise {

    static final class TtlCache<K, V> {
        private record Entry<V>(V value, long expiresAt) {}

        private final long ttlMillis;
        private final LongSupplier clock;
        private final Map<K, Entry<V>> entries = new HashMap<>();

        TtlCache(long ttlMillis, LongSupplier clock) {
            if (ttlMillis < 1) throw new IllegalArgumentException("ttl must be at least 1 ms: " + ttlMillis);
            this.ttlMillis = ttlMillis;
            this.clock = clock;
        }

        void put(K key, V value) {
            entries.put(key, new Entry<>(value, clock.getAsLong() + ttlMillis));
        }

        Optional<V> get(K key) {
            var e = entries.get(key);
            if (e == null) return Optional.empty();
            if (!alive(e)) {
                entries.remove(key);
                return Optional.empty();
            }
            return Optional.of(e.value());
        }

        int size() {
            return (int) entries.values().stream().filter(this::alive).count();
        }

        int purgeExpired() {
            int before = entries.size();
            entries.values().removeIf(e -> !alive(e));
            return before - entries.size();
        }

        private boolean alive(Entry<V> e) {
            return clock.getAsLong() < e.expiresAt();
        }
    }

    static final class FakeClock implements LongSupplier {
        long now;

        public long getAsLong() {
            return now;
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("returns a fresh entry and misses an unknown key", () -> {
            var clock = new FakeClock();
            var c = new TtlCache<String, String>(100, clock);
            c.put("k", "v");
            eq(c.get("k"), Optional.of("v"));
            eq(c.get("other"), Optional.empty());
        });
        test("an entry lives for exactly ttl milliseconds", () -> {
            var clock = new FakeClock();
            var c = new TtlCache<String, String>(100, clock);
            clock.now = 1_000;
            c.put("k", "v");
            clock.now = 1_099;
            eq(c.get("k"), Optional.of("v"));
            clock.now = 1_100;
            eq(c.get("k"), Optional.empty());
        });
        test("a get does not extend the lifetime", () -> {
            var clock = new FakeClock();
            var c = new TtlCache<String, String>(100, clock);
            c.put("k", "v");
            clock.now = 90;
            c.get("k");
            clock.now = 100;
            eq(c.get("k"), Optional.empty());
        });
        test("putting again restarts the clock for that key", () -> {
            var clock = new FakeClock();
            var c = new TtlCache<String, String>(100, clock);
            c.put("k", "old");
            clock.now = 80;
            c.put("k", "new");
            clock.now = 179;
            eq(c.get("k"), Optional.of("new"));
            clock.now = 180;
            eq(c.get("k"), Optional.empty());
        });
        test("size counts only live entries", () -> {
            var clock = new FakeClock();
            var c = new TtlCache<String, String>(100, clock);
            c.put("a", "1");
            clock.now = 50;
            c.put("b", "2");
            eq(c.size(), 2);
            clock.now = 100;
            eq(c.size(), 1);
            clock.now = 150;
            eq(c.size(), 0);
        });
        test("purgeExpired removes and counts the dead entries", () -> {
            var clock = new FakeClock();
            var c = new TtlCache<String, String>(100, clock);
            c.put("a", "1");
            c.put("b", "2");
            clock.now = 50;
            c.put("c", "3");
            clock.now = 120;
            eq(c.purgeExpired(), 2);
            eq(c.get("c"), Optional.of("3"));
            eq(c.purgeExpired(), 0);
            clock.now = 150;
            eq(c.purgeExpired(), 1);
        });
        test("reading an expired entry removes it", () -> {
            var clock = new FakeClock();
            var c = new TtlCache<String, String>(100, clock);
            c.put("a", "1");
            clock.now = 200;
            eq(c.get("a"), Optional.empty());
            eq(c.purgeExpired(), 0);
        });
        test("refuses a ttl below one millisecond", () -> {
            var clock = new FakeClock();
            raises(IllegalArgumentException.class, () -> new TtlCache<String, String>(0, clock));
            raises(IllegalArgumentException.class, () -> new TtlCache<String, String>(-5, clock));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
