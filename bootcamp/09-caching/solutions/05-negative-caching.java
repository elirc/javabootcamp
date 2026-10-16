// ─────────────────────────────────────────────────────────────────────────
//  05 · remembering "not found" — SOLUTION                  ★★☆ core
//  concepts: negative caching · two TTLs · errors vs absence · TTL
//  run: java -cp ../../_lib/out 05-negative-caching.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A cache that only remembers hits is useless against the most expensive
//  traffic: lookups for things that do not exist (a scraper probing
//  random ids, a typo'd username refreshed in a loop). Every one of them
//  falls through to the database. DNS solved this decades ago: an
//  NXDOMAIN answer is cached too, with its own, shorter TTL.
//  So the entry stores the Optional itself, present or empty, plus its
//  expiry, and the TTL is picked by which one it is. The negative TTL is
//  shorter because absence is the answer most likely to change: someone
//  signs up with that name a second later.
//  That is also why onCreated exists. Without it, a freshly created user
//  is "not found" for up to negativeTtl after signing up; writing the new
//  value straight into the cache overwrites the remembered miss.
//  The trap is treating an exception as absence. "The database timed
//  out" is not "the user does not exist"; caching it would turn a blip
//  into a confident lie for every caller. Exceptions propagate, nothing
//  is stored, and the next find asks again.
//  Expiry follows exercise 02: alive while now < expiresAt.

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
            if (positiveTtl < 1 || negativeTtl < 1) {
                throw new IllegalArgumentException("ttls must be at least 1 ms: " + positiveTtl + ", " + negativeTtl);
            }
            this.backend = backend;
            this.positiveTtl = positiveTtl;
            this.negativeTtl = negativeTtl;
            this.clock = clock;
        }

        Optional<String> find(String key) {
            var e = entries.get(key);
            if (e != null && clock.getAsLong() < e.expiresAt()) return e.value();
            var answer = backend.apply(key);
            remember(key, answer);
            return answer;
        }

        void onCreated(String key, String value) {
            remember(key, Optional.of(value));
        }

        void invalidate(String key) {
            entries.remove(key);
        }

        private void remember(String key, Optional<String> answer) {
            long ttl = answer.isPresent() ? positiveTtl : negativeTtl;
            entries.put(key, new Entry(answer, clock.getAsLong() + ttl));
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
