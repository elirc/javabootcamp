// ─────────────────────────────────────────────────────────────────────────
//  08 · idempotency keys with replay                        ★★★ stretch
//  concepts: idempotency keys · response replay · fingerprints · TTL
//  run: java -cp ../../_lib/out 08-idempotency-keys.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Clients retry POSTs; the server must answer a retry with the SAME
//  response and not run it twice. Finish IdempotencyStore.execute(key,
//  fingerprint, handler). A stored answer for key (alive while now <
//  storedAt + ttl) with the same fingerprint is replayed (replayed =
//  true, handler not run); with a different fingerprint →
//  IllegalStateException mentioning "fingerprint". Otherwise claim the
//  key, run the handler and return its response (replayed = false); a
//  status below 500 is stored, a 5xx is not. While a key's handler is
//  running, execute with that key → IllegalStateException mentioning
//  "in progress". A handler exception propagates, stores nothing and
//  frees the key. Blank or null key, ttl < 1 → IllegalArgumentException.
//
//      execute(k1, "pay 10", charge)  → (201 "charge #1", replayed false)
//      execute(k1, "pay 10", charge)  → (201 "charge #1", replayed true)
//
//  hint: claim before running, release on every way out.

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
            throw new UnsupportedOperationException("TODO");
        }

        Outcome execute(String key, String fingerprint, Supplier<Response> handler) {
            throw new UnsupportedOperationException("TODO");
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
