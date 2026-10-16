// ─────────────────────────────────────────────────────────────────────────
//  05 · remembering "not found"                             ★★☆ core
//  concepts: negative caching · two TTLs · errors vs absence · TTL
//  run: java -cp ../../_lib/out 05-negative-caching.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Lookups for things that don't exist are the ones a plain cache never
//  helps with. Finish the nested class NegativeCache. find(key) answers
//  from a live cache entry, otherwise asks the backend and caches its
//  answer: a found value for positiveTtl, an empty Optional ("not found")
//  for negativeTtl. Entries are alive while now < storedAt + ttl. If the
//  backend throws, the exception propagates and nothing is cached.
//  onCreated(key, value) caches a value the system just created (for
//  positiveTtl), replacing a remembered miss. invalidate(key) drops any
//  entry. A ttl below 1 is an IllegalArgumentException.
//
//      positive 1000, negative 100:
//      find(bob) at t=0 → empty (backend asked)   at t=99 → empty (cached)
//      find(bob) at t=100                         → backend asked again
//
//  hint: the cached thing can be the Optional itself.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.LongSupplier;

class Exercise {

    static final class NegativeCache {
        private record Entry(Optional<String> value, long expiresAt) {}

        private final Function<String, Optional<String>> backend;
        private final long positiveTtl, negativeTtl;
        private final LongSupplier clock;
        private final Map<String, Entry> entries = new HashMap<>();

        NegativeCache(Function<String, Optional<String>> backend, long positiveTtl, long negativeTtl,
                      LongSupplier clock) {
            throw new UnsupportedOperationException("TODO");
        }

        Optional<String> find(String key) {
            throw new UnsupportedOperationException("TODO");
        }

        void onCreated(String key, String value) {
            throw new UnsupportedOperationException("TODO");
        }

        void invalidate(String key) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class Users implements Function<String, Optional<String>> {
        final Map<String, String> rows = new HashMap<>();
        int calls;
        boolean down;

        public Optional<String> apply(String name) {
            calls++;
            if (down) throw new IllegalStateException("users db timed out");
            return Optional.ofNullable(rows.get(name));
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
        test("a found value is cached for the positive ttl", () -> {
            var users = new Users();
            users.rows.put("ada", "Ada Lovelace");
            var clock = new FakeClock();
            var c = new NegativeCache(users, 1_000, 100, clock);
            eq(c.find("ada"), Optional.of("Ada Lovelace"));
            clock.now = 999;
            eq(c.find("ada"), Optional.of("Ada Lovelace"));
            eq(users.calls, 1);
            clock.now = 1_000;
            c.find("ada");
            eq(users.calls, 2);
        });
        test("a miss is cached for the shorter negative ttl", () -> {
            var users = new Users();
            var clock = new FakeClock();
            var c = new NegativeCache(users, 1_000, 100, clock);
            eq(c.find("bob"), Optional.empty());
            clock.now = 99;
            eq(c.find("bob"), Optional.empty());
            eq(users.calls, 1);
            clock.now = 100;
            c.find("bob");
            eq(users.calls, 2);
        });
        test("a key that appears later is found once the miss expires", () -> {
            var users = new Users();
            var clock = new FakeClock();
            var c = new NegativeCache(users, 1_000, 100, clock);
            c.find("bob");
            users.rows.put("bob", "Bob");
            clock.now = 50;
            eq(c.find("bob"), Optional.empty());
            clock.now = 100;
            eq(c.find("bob"), Optional.of("Bob"));
        });
        test("a backend error propagates and is not cached", () -> {
            var users = new Users();
            users.rows.put("ada", "Ada Lovelace");
            var clock = new FakeClock();
            var c = new NegativeCache(users, 1_000, 100, clock);
            users.down = true;
            raises(IllegalStateException.class, () -> c.find("ada"), "timed out");
            users.down = false;
            eq(c.find("ada"), Optional.of("Ada Lovelace"));
            eq(users.calls, 2);
        });
        test("onCreated replaces a remembered miss", () -> {
            var users = new Users();
            var clock = new FakeClock();
            var c = new NegativeCache(users, 1_000, 100, clock);
            c.find("bob");
            users.rows.put("bob", "Bob");
            c.onCreated("bob", "Bob");
            eq(c.find("bob"), Optional.of("Bob"));
            eq(users.calls, 1);
        });
        test("onCreated values last the positive ttl", () -> {
            var users = new Users();
            var clock = new FakeClock();
            var c = new NegativeCache(users, 1_000, 100, clock);
            clock.now = 10;
            c.onCreated("eve", "Eve");
            clock.now = 1_009;
            eq(c.find("eve"), Optional.of("Eve"));
            eq(users.calls, 0);
        });
        test("invalidate drops either kind of entry", () -> {
            var users = new Users();
            users.rows.put("ada", "Ada Lovelace");
            var clock = new FakeClock();
            var c = new NegativeCache(users, 1_000, 100, clock);
            c.find("ada");
            c.find("bob");
            c.invalidate("ada");
            c.invalidate("bob");
            c.find("ada");
            c.find("bob");
            eq(users.calls, 4);
        });
        test("refuses a ttl below one millisecond", () -> {
            var clock = new FakeClock();
            raises(IllegalArgumentException.class, () -> new NegativeCache(new Users(), 0, 100, clock));
            raises(IllegalArgumentException.class, () -> new NegativeCache(new Users(), 100, 0, clock));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
