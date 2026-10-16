// ─────────────────────────────────────────────────────────────────────────
//  02 · entries that expire                                 ★☆☆ warm-up
//  concepts: TTL expiry · injected clock · lazy vs eager purge
//  run: java -cp ../../_lib/out 02-ttl-cache.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A session cache must not serve yesterday's data, so every entry gets
//  a time to live. Finish the nested class TtlCache. The clock is an
//  injected LongSupplier of milliseconds. An entry put at time t is alive
//  while now < t + ttl and dead from t + ttl on. get returns Optional and
//  does NOT extend the lifetime; a get that finds a dead entry removes
//  it. put on an existing key replaces it and restarts its lifetime.
//  size() counts live entries only; purgeExpired() removes every dead
//  entry and returns how many it removed. A ttl below 1 is an
//  IllegalArgumentException.
//
//      ttl 100, put("k") at t=1000:   get at t=1099 → present
//                                     get at t=1100 → empty
//
//  hint: store when the entry dies, not when it was born.

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
            throw new UnsupportedOperationException("TODO");
        }

        void put(K key, V value) {
            throw new UnsupportedOperationException("TODO");
        }

        Optional<V> get(K key) {
            throw new UnsupportedOperationException("TODO");
        }

        int size() {
            throw new UnsupportedOperationException("TODO");
        }

        int purgeExpired() {
            throw new UnsupportedOperationException("TODO");
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
