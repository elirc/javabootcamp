// ─────────────────────────────────────────────────────────────────────────
//  08 · charge exactly once — SOLUTION                      ★★★ stretch
//  concepts: idempotency · processed-id sets · events · retries
//  run: java -cp ../../_lib/out 08-idempotent-payments.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Networks deliver "at least once": a client that times out cannot know
//  whether its request arrived, so it sends it again. If the server
//  charges every request it receives, the customer pays twice. The fix is
//  idempotency: every request carries an id chosen by the CLIENT, and the
//  server remembers which ids it has already applied. Re-applying a known
//  id is a no-op that reports what happened.
//  WHEN to record the id is the whole design. Record it only after a
//  successful charge. A declined payment changed nothing, so the client
//  must be able to retry the same id after topping up; recording declined
//  ids would turn a temporary "no funds" into a permanent refusal.
//  Order inside apply: validate (unknown account and bad amounts are bugs
//  → exceptions), then the duplicate check (cheap and final), then the
//  funds check, then the change plus recording the id — two writes that
//  must happen together. In a real system they live in one database
//  transaction; here, nothing between them can fail.
//  Returning an event string instead of a boolean gives the caller (and a
//  log) the full story: which of the three outcomes happened, for which
//  id. Real systems return the ORIGINAL response for a duplicate, so the
//  retrying client sees exactly what the first attempt would have shown.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Payment(String id, String account, long cents) {}

    static final class Payments {
        private final Map<String, Long> balances = new HashMap<>();
        private final Set<String> charged = new HashSet<>();

        void topUp(String account, long cents) {
            balances.merge(account, cents, Long::sum);
        }

        long balance(String account) {
            return balances.getOrDefault(account, 0L);
        }

        String apply(Payment p) {
            if (!balances.containsKey(p.account())) throw new IllegalArgumentException("unknown account " + p.account());
            if (p.cents() <= 0) throw new IllegalArgumentException("amount must be positive: " + p.cents());
            if (charged.contains(p.id())) return "duplicate " + p.id();
            if (balance(p.account()) < p.cents()) return "declined " + p.id();
            balances.merge(p.account(), -p.cents(), Long::sum);
            charged.add(p.id());
            return "charged " + p.id();
        }
    }

    static Payments withBalance(long cents) {
        var payments = new Payments();
        payments.topUp("acct", cents);
        return payments;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("charges a payment", () -> {
            var p = withBalance(500);
            eq(p.apply(new Payment("pay-1", "acct", 300)), "charged pay-1");
            eq(p.balance("acct"), 200);
        });
        test("a duplicate delivery charges nothing", () -> {
            var p = withBalance(500);
            p.apply(new Payment("pay-1", "acct", 300));
            eq(p.apply(new Payment("pay-1", "acct", 300)), "duplicate pay-1");
            eq(p.balance("acct"), 200);
        });
        test("different ids are different payments", () -> {
            var p = withBalance(500);
            p.apply(new Payment("pay-1", "acct", 100));
            eq(p.apply(new Payment("pay-2", "acct", 100)), "charged pay-2");
            eq(p.balance("acct"), 300);
        });
        test("declines without enough money and changes nothing", () -> {
            var p = withBalance(500);
            eq(p.apply(new Payment("pay-9", "acct", 900)), "declined pay-9");
            eq(p.balance("acct"), 500);
        });
        test("a declined id can be retried after a top-up", () -> {
            var p = withBalance(500);
            p.apply(new Payment("pay-9", "acct", 900));
            p.topUp("acct", 500);
            eq(p.apply(new Payment("pay-9", "acct", 900)), "charged pay-9");
            eq(p.balance("acct"), 100);
        });
        test("refuses unknown accounts and bad amounts", () -> {
            var p = withBalance(500);
            raises(IllegalArgumentException.class, () -> p.apply(new Payment("x", "nobody", 1)));
            raises(IllegalArgumentException.class, () -> p.apply(new Payment("x", "acct", 0)));
        });
        test("can spend the balance exactly", () -> {
            var p = withBalance(500);
            eq(p.apply(new Payment("all", "acct", 500)), "charged all");
            eq(p.balance("acct"), 0);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
