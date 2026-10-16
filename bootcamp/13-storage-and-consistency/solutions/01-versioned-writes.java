// ─────────────────────────────────────────────────────────────────────────
//  01 · versioned writes — SOLUTION                         ★☆☆ warm-up
//  concepts: optimistic locking · versions · compare-and-set · lost updates
//  run: java -cp ../../_lib/out 01-versioned-writes.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Optimistic locking takes no lock while you think: you read a value
//  together with its version, compute, and write back "only if the
//  version is still the one I read". That last step is a compare-and-set
//  (in SQL: UPDATE ... SET v = ?, version = version + 1 WHERE id = ? AND
//  version = ?, then check the row count). A mismatch means somebody
//  wrote in between, and the right move is to re-read and recompute,
//  never to write your stale result anyway: that is the lost update.
//  update() is that loop: read, apply the change to the FRESH value,
//  compare-and-set, and give up after maxAttempts by rethrowing the last
//  conflict. The classic wrong turn is computing the new value once and
//  retrying only the put, which re-submits a value built from old data.
//  The tombstone is the subtle part. If delete removed the entry, the
//  version would restart at 0, and a writer still holding version 2
//  from before the delete could succeed once the key is recreated and
//  bumped back to 2 (the ABA problem). Keeping the entry with a null
//  value and a bumped version makes versions monotonic per key, so a
//  version number can never mean two different states.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.UnaryOperator;

class Exercise {

    record Versioned(String value, long version) {}

    static final class ConflictException extends RuntimeException {
        ConflictException(String message) {
            super(message);
        }
    }

    static final class VersionedStore {
        private final Map<String, Versioned> rows = new HashMap<>();

        Versioned get(String key) {
            return rows.getOrDefault(key, new Versioned(null, 0));
        }

        long put(String key, String value, long expectedVersion) {
            var current = checkVersion(key, expectedVersion);
            var next = current.version() + 1;
            rows.put(key, new Versioned(value, next));
            return next;
        }

        long delete(String key, long expectedVersion) {
            var current = get(key);
            if (current.value() == null) throw new IllegalStateException("nothing to delete at " + key);
            checkVersion(key, expectedVersion);
            var next = current.version() + 1;
            rows.put(key, new Versioned(null, next));
            return next;
        }

        int update(String key, UnaryOperator<String> change, int maxAttempts) {
            ConflictException last = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                var seen = get(key);
                var proposed = change.apply(seen.value());
                try {
                    put(key, proposed, seen.version());
                    return attempt;
                } catch (ConflictException e) {
                    last = e;
                }
            }
            throw last;
        }

        private Versioned checkVersion(String key, long expectedVersion) {
            var current = get(key);
            if (current.version() != expectedVersion) {
                throw new ConflictException(key + ": expected version " + expectedVersion
                    + " but it is " + current.version());
            }
            return current;
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("an unknown key has no value and version 0", () -> {
            var store = new VersionedStore();
            eq(store.get("cart"), new Versioned(null, 0));
        });
        test("creating needs version 0 and every write bumps the version", () -> {
            var store = new VersionedStore();
            eq(store.put("cart", "tea", 0), 1);
            eq(store.put("cart", "tea,cake", 1), 2);
            eq(store.get("cart"), new Versioned("tea,cake", 2));
        });
        test("a stale version is rejected and changes nothing", () -> {
            var store = new VersionedStore();
            store.put("cart", "tea", 0);
            store.put("cart", "tea,cake", 1);
            raises(ConflictException.class, () -> store.put("cart", "coffee", 1), "cart");
            raises(ConflictException.class, () -> store.put("cart", "coffee", 3), "cart");
            eq(store.get("cart"), new Versioned("tea,cake", 2));
        });
        test("delete leaves a tombstone with a higher version", () -> {
            var store = new VersionedStore();
            store.put("cart", "tea", 0);
            eq(store.delete("cart", 1), 2);
            eq(store.get("cart"), new Versioned(null, 2));
            raises(ConflictException.class, () -> store.put("cart", "new", 0));
            eq(store.put("cart", "new", 2), 3);
        });
        test("a version from before a delete can never write again", () -> {
            var store = new VersionedStore();
            store.put("cart", "a", 0);
            store.put("cart", "b", 1);
            store.delete("cart", 2);
            store.put("cart", "c", 3);
            raises(ConflictException.class, () -> store.put("cart", "stale", 2));
            eq(store.get("cart"), new Versioned("c", 4));
        });
        test("deleting with no value is an IllegalStateException", () -> {
            var store = new VersionedStore();
            raises(IllegalStateException.class, () -> store.delete("cart", 0));
            store.put("cart", "tea", 0);
            store.delete("cart", 1);
            raises(IllegalStateException.class, () -> store.delete("cart", 2));
            raises(ConflictException.class, () -> { store.put("cart", "x", 2); store.delete("cart", 2); });
        });
        test("update applies the change and reports one attempt", () -> {
            var store = new VersionedStore();
            store.put("n", "1", 0);
            eq(store.update("n", v -> String.valueOf(Integer.parseInt(v) + 1), 3), 1);
            eq(store.get("n"), new Versioned("2", 2));
        });
        test("update re-reads after a concurrent write and loses nothing", () -> {
            var store = new VersionedStore();
            store.put("n", "1", 0);
            var calls = new int[1];
            int attempts = store.update("n", v -> {
                if (calls[0]++ == 0) store.put("n", "10", 1);
                return String.valueOf(Integer.parseInt(v) + 1);
            }, 3);
            eq(attempts, 2);
            eq(store.get("n"), new Versioned("11", 3));
        });
        test("update gives up after maxAttempts with the conflict", () -> {
            var store = new VersionedStore();
            store.put("n", "start", 0);
            var calls = new int[1];
            raises(ConflictException.class, () -> store.update("n", v -> {
                calls[0]++;
                store.put("n", "rival", store.get("n").version());
                return "mine";
            }, 3));
            eq(calls[0], 3);
            eq(store.get("n"), new Versioned("rival", 4));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
