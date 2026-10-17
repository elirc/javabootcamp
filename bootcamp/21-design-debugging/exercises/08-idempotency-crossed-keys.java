// ─────────────────────────────────────────────────────────────────────────
//  08 · the receipt that went to the wrong customer         ★★★ stretch
//  concepts: debugging · idempotency keys · composite keys · caching failures
//  run: java -cp ../../_lib/out 08-idempotency-crossed-keys.java
// ─────────────────────────────────────────────────────────────────────────
//
//  DEBUG HUNT: the code below is complete and compiles; it has bugs. Run
//  it, read the failing tests, find and fix the bugs. Do not edit tests.
//  Intended: handle(clientId, key, operation) makes a payment API safe
//  to retry. The first request for a (clientId, key) PAIR runs the
//  operation; a repeat of that same pair returns the stored response
//  without running it. Different clients may use the same key. Only
//  final answers are stored: a response with status < 500 is stored; a
//  5xx (a temporary server error) is returned but NOT stored, so the
//  client's retry runs the operation again. An operation that throws
//  stores nothing.
//  Bug reports:
//    "Now and then a customer receives a receipt for somebody else's
//     payment. Client ids and keys are short lowercase strings."
//    "During the database failover clients retried as told, but kept
//     getting the same 503 for an hour, and the payments never ran."
//
//      handle("acme", "k1", pay) twice    → pay runs once, same response
//      then handle("zeta", "k1", pay2)  → pay2 runs: zeta's own receipt
//
//  hint: print the map's keys after each call of a failing test.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    record Response(int status, String body) {}

    static final class IdempotencyStore {
        private final Map<String, Response> done = new HashMap<>();

        Response handle(String clientId, String key, Supplier<Response> operation) {
            var storeKey = clientId + key;
            var cached = done.get(storeKey);
            if (cached != null) return cached;
            var response = operation.get();
            done.put(storeKey, response);
            return response;
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("the first request runs the operation", () -> {
            var store = new IdempotencyStore();
            int[] runs = {0};
            eq(store.handle("acme", "k1", () -> new Response(201, "paid #" + ++runs[0])), new Response(201, "paid #1"));
            eq(runs[0], 1);
        });
        test("a repeat of the same client and key replays without running", () -> {
            var store = new IdempotencyStore();
            int[] runs = {0};
            Supplier<Response> pay = () -> new Response(201, "paid #" + ++runs[0]);
            store.handle("acme", "k1", pay);
            eq(store.handle("acme", "k1", pay), new Response(201, "paid #1"));
            eq(runs[0], 1);
        });
        test("different clients may use the same key", () -> {
            var store = new IdempotencyStore();
            eq(store.handle("acme", "k1", () -> new Response(201, "acme paid")), new Response(201, "acme paid"));
            eq(store.handle("zeta", "k1", () -> new Response(201, "zeta paid")), new Response(201, "zeta paid"));
        });
        test("ids that run together do not collide", () -> {
            var store = new IdempotencyStore();
            eq(store.handle("ab", "c", () -> new Response(201, "ab's receipt")), new Response(201, "ab's receipt"));
            eq(store.handle("a", "bc", () -> new Response(201, "a's receipt")), new Response(201, "a's receipt"));
            eq(store.handle("ab", "c", () -> new Response(201, "again")), new Response(201, "ab's receipt"));
        });
        test("a 5xx is not stored, so the retry runs again", () -> {
            var store = new IdempotencyStore();
            int[] runs = {0};
            Supplier<Response> flaky = () -> ++runs[0] == 1 ? new Response(503, "try later") : new Response(201, "paid");
            eq(store.handle("acme", "k9", flaky).status(), 503);
            eq(store.handle("acme", "k9", flaky), new Response(201, "paid"));
            eq(store.handle("acme", "k9", flaky), new Response(201, "paid"));
            eq(runs[0], 2);
        });
        test("a 4xx is a final answer and is stored", () -> {
            var store = new IdempotencyStore();
            int[] runs = {0};
            Supplier<Response> declined = () -> new Response(402, "declined #" + ++runs[0]);
            store.handle("acme", "k2", declined);
            eq(store.handle("acme", "k2", declined), new Response(402, "declined #1"));
            eq(runs[0], 1);
        });
        test("an operation that throws stores nothing", () -> {
            var store = new IdempotencyStore();
            raises(IllegalStateException.class, () -> store.handle("acme", "k3", () -> {
                throw new IllegalStateException("db down");
            }));
            eq(store.handle("acme", "k3", () -> new Response(201, "paid")), new Response(201, "paid"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
