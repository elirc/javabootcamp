// ─────────────────────────────────────────────────────────────────────────
//  08 · the receipt that went to the wrong customer — SOLUTION  ★★★ stretch
//  concepts: debugging · idempotency keys · composite keys · caching failures
//  run: java -cp ../../_lib/out 08-idempotency-crossed-keys.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  1. Reproduce report 1: client "ab" with key "c", then client "a" with
//     key "bc". The second gets the first one's receipt. Narrow: print
//     the map's keys: both requests were stored under "abc". Gluing two
//     strings together without a separator is not a unique key; the
//     same holds for user + "/" + file when names may contain "/". Fix:
//     a record key, new StoreKey(clientId, key), whose equals compares
//     the parts. (A separator works only if neither part can contain
//     it.) Pinned by "ids that run together do not collide"; the
//     "different clients may use the same key" test passes either way,
//     which is why the concatenation looked fine.
//  2. Reproduce report 2: a 503, then a retry. The retry returns the
//     stored 503 and the payment never runs. The store records EVERY
//     response, including temporary failures, so the idempotency layer
//     turns a 30-second outage into a permanent one for those keys. Fix:
//     store only final answers (status < 500); a 4xx such as "declined"
//     IS final and must replay, or a retry could charge a card that was
//     declined a moment ago. Pinned by the 5xx and 4xx tests.
//  Real stores also keep a request fingerprint (the same key with a
//  different body is a client bug) and an in-progress marker for
//  concurrent duplicates. Module 10, exercise 08 builds the basic store.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    record Response(int status, String body) {}

    record StoreKey(String clientId, String key) {}

    static final class IdempotencyStore {
        private final Map<StoreKey, Response> done = new HashMap<>();

        Response handle(String clientId, String key, Supplier<Response> operation) {
            var storeKey = new StoreKey(clientId, key);
            var cached = done.get(storeKey);
            if (cached != null) return cached;
            var response = operation.get();
            if (response.status() < 500) done.put(storeKey, response);
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
