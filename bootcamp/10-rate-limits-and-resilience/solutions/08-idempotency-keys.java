// ─────────────────────────────────────────────────────────────────────────
//  08 · idempotency keys with replay — SOLUTION             ★★★ stretch
//  concepts: idempotency keys · response replay · fingerprints · TTL
//  run: java -cp ../../_lib/out 08-idempotency-keys.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Retries (04) are only safe if repeating a request cannot repeat its
//  effect. For "charge the card" that needs help from the server: the
//  client sends an Idempotency-Key header, and the server stores the
//  RESPONSE under that key. A retry gets the stored response replayed,
//  byte for byte, without running the handler again. Stripe made this
//  design famous; module 08's payments remembered ids, this remembers
//  answers.
//  The store keys on more than the key:
//  - Fingerprint. The same key with a different body is a client bug
//    (a reused UUID). Replaying the old answer would silently confirm a
//    request that never ran, so it is refused (422 in the IETF
//    Idempotency-Key draft).
//  - In progress. The key is claimed BEFORE the handler runs, so a
//    second request with that key while the first is still running gets
//    a refusal (409) instead of a second execution. In a real server
//    that second request comes from another thread; here the handler
//    re-enters execute, which exercises the same check.
//  - Only final answers are stored. 2xx and 4xx are what the request
//    deserved and are replayed; a 5xx or a thrown exception means "we
//    failed, try again", so the claim is released and nothing is stored.
//    That assumes a 5xx left no effect behind (the handler rolled back);
//    Stripe cannot assume it, so it replays a 500 once the handler began.
//    The try/finally-style release is the trap: forget it on the error
//    path and the key is stuck "in progress" forever.
//  Stored answers expire after the TTL (Stripe keeps them 24 hours).

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

class Exercise {

    record Response(int status, String body) {}

    record Outcome(Response response, boolean replayed) {}

    static final class IdempotencyStore {
        private record Stored(String fingerprint, Response response, long expiresAt) {}

        private final long ttlMillis;
        private final LongSupplier clock;
        private final Map<String, Stored> stored = new HashMap<>();
        private final Set<String> inProgress = new HashSet<>();

        IdempotencyStore(long ttlMillis, LongSupplier clock) {
            if (ttlMillis < 1) throw new IllegalArgumentException("ttl must be at least 1 ms: " + ttlMillis);
            this.ttlMillis = ttlMillis;
            this.clock = clock;
        }

        Outcome execute(String key, String fingerprint, Supplier<Response> handler) {
            if (key == null || key.isBlank()) throw new IllegalArgumentException("an idempotency key is required");
            var previous = stored.get(key);
            if (previous != null && clock.getAsLong() >= previous.expiresAt()) {
                stored.remove(key);
                previous = null;
            }
            if (previous != null) {
                if (!previous.fingerprint().equals(fingerprint)) {
                    throw new IllegalStateException("key " + key + " was used with a different fingerprint");
                }
                return new Outcome(previous.response(), true);
            }
            if (!inProgress.add(key)) throw new IllegalStateException("key " + key + " is in progress");
            Response response;
            try {
                response = handler.get();
            } finally {
                inProgress.remove(key);
            }
            if (response.status() < 500) {
                stored.put(key, new Stored(fingerprint, response, clock.getAsLong() + ttlMillis));
            }
            return new Outcome(response, false);
        }
    }

    static final class FakeClock implements LongSupplier {
        long now;

        public long getAsLong() {
            return now;
        }
    }

    static final class Charges {
        int runs;

        Response charge(int status) {
            runs++;
            return new Response(status, "charge #" + runs);
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("a repeat is replayed without running the handler", () -> {
            var charges = new Charges();
            var store = new IdempotencyStore(60_000, new FakeClock());
            var first = store.execute("k1", "pay 10", () -> charges.charge(201));
            var again = store.execute("k1", "pay 10", () -> charges.charge(201));
            eq(first, new Outcome(new Response(201, "charge #1"), false));
            eq(again, new Outcome(new Response(201, "charge #1"), true));
            eq(charges.runs, 1);
        });
        test("different keys are different requests", () -> {
            var charges = new Charges();
            var store = new IdempotencyStore(60_000, new FakeClock());
            store.execute("k1", "pay 10", () -> charges.charge(201));
            var other = store.execute("k2", "pay 10", () -> charges.charge(201));
            eq(other.replayed(), false);
            eq(charges.runs, 2);
        });
        test("the same key with a different fingerprint is refused", () -> {
            var charges = new Charges();
            var store = new IdempotencyStore(60_000, new FakeClock());
            store.execute("k1", "pay 10", () -> charges.charge(201));
            raises(IllegalStateException.class, () -> store.execute("k1", "pay 99", () -> charges.charge(201)), "fingerprint");
            eq(charges.runs, 1);
        });
        test("a 4xx answer is final and replayed too", () -> {
            var charges = new Charges();
            var store = new IdempotencyStore(60_000, new FakeClock());
            store.execute("k1", "pay 10", () -> charges.charge(402));
            var again = store.execute("k1", "pay 10", () -> charges.charge(201));
            eq(again, new Outcome(new Response(402, "charge #1"), true));
        });
        test("a 5xx answer is not stored, so a retry runs again", () -> {
            var charges = new Charges();
            var store = new IdempotencyStore(60_000, new FakeClock());
            var failed = store.execute("k1", "pay 10", () -> charges.charge(503));
            eq(failed, new Outcome(new Response(503, "charge #1"), false));
            var retried = store.execute("k1", "pay 10", () -> charges.charge(201));
            eq(retried, new Outcome(new Response(201, "charge #2"), false));
            eq(store.execute("k1", "pay 10", () -> charges.charge(201)).replayed(), true);
        });
        test("a handler that throws stores nothing and frees the key", () -> {
            var charges = new Charges();
            var store = new IdempotencyStore(60_000, new FakeClock());
            raises(IllegalArgumentException.class, () -> store.execute("k1", "pay 10", () -> {
                throw new IllegalArgumentException("card network down");
            }));
            var retried = store.execute("k1", "pay 10", () -> charges.charge(201));
            eq(retried.replayed(), false);
            eq(charges.runs, 1);
        });
        test("a key reused while its request is still running is refused", () -> {
            var charges = new Charges();
            var store = new IdempotencyStore(60_000, new FakeClock());
            var inner = new ArrayList<String>();
            var outer = store.execute("k1", "pay 10", () -> {
                try {
                    store.execute("k1", "pay 10", () -> charges.charge(201));
                } catch (IllegalStateException e) {
                    inner.add(e.getMessage().contains("in progress") ? "refused" : e.getMessage());
                }
                return charges.charge(201);
            });
            eq(inner, List.of("refused"));
            eq(outer.response(), new Response(201, "charge #1"));
            eq(store.execute("k1", "pay 10", () -> charges.charge(201)).replayed(), true);
        });
        test("stored answers expire after the ttl", () -> {
            var clock = new FakeClock();
            var charges = new Charges();
            var store = new IdempotencyStore(1_000, clock);
            store.execute("k1", "pay 10", () -> charges.charge(201));
            clock.now = 999;
            eq(store.execute("k1", "pay 10", () -> charges.charge(201)).replayed(), true);
            clock.now = 1_000;
            eq(store.execute("k1", "pay 99", () -> charges.charge(201)).replayed(), false);
            eq(charges.runs, 2);
        });
        test("refuses a blank key and a ttl below one", () -> {
            var store = new IdempotencyStore(1_000, new FakeClock());
            raises(IllegalArgumentException.class, () -> store.execute(" ", "x", () -> new Response(200, "")));
            raises(IllegalArgumentException.class, () -> store.execute(null, "x", () -> new Response(200, "")));
            raises(IllegalArgumentException.class, () -> new IdempotencyStore(0, new FakeClock()));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
