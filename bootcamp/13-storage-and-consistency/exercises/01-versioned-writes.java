// ─────────────────────────────────────────────────────────────────────────
//  01 · versioned writes                                    ★☆☆ warm-up
//  concepts: optimistic locking · versions · compare-and-set · lost updates
//  run: java -cp ../../_lib/out 01-versioned-writes.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Two people edit the same cart. Finish VersionedStore so neither can
//  silently overwrite the other. get returns (value, version); an unknown
//  key is (null, 0). put(key, value, expected) writes only if the
//  current version equals expected, then returns version + 1; otherwise
//  it throws ConflictException naming the key and changes nothing.
//  delete(key, expected) leaves a TOMBSTONE (null value, version + 1),
//  so versions never restart; deleting a key with no value is an
//  IllegalStateException (checked before the version).
//  update(key, change, maxAttempts) is the client loop: read, apply
//  change to the value read, put with the version read; on a conflict
//  start over; return the attempt that succeeded (1-based), or after
//  maxAttempts conflicts rethrow the last ConflictException.
//
//      put("cart", "tea", 0)  → 1        put("cart", "cake", 0)  → Conflict
//      delete("cart", 1)      → 2        get("cart") → (null, 2)
//
//  hint: a retry must call change again on the fresh value.

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
            throw new UnsupportedOperationException("TODO");
        }

        long put(String key, String value, long expectedVersion) {
            throw new UnsupportedOperationException("TODO");
        }

        long delete(String key, long expectedVersion) {
            throw new UnsupportedOperationException("TODO");
        }

        int update(String key, UnaryOperator<String> change, int maxAttempts) {
            throw new UnsupportedOperationException("TODO");
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
            raises(IllegalStateException.class, () -> store.delete("cart", 7));
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
