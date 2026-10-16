// ─────────────────────────────────────────────────────────────────────────
//  08 · charge exactly once                                 ★★★ stretch
//  concepts: idempotency · processed-id sets · events · retries
//  run: java -cp ../../_lib/out 08-idempotent-payments.java
// ─────────────────────────────────────────────────────────────────────────
//
//  The bank simulator receives payment requests that may be DELIVERED
//  TWICE (a client retries after a timeout). Finish the nested class
//  Payments so each payment id takes effect at most once. apply returns
//  one event: "charged <id>" (balance reduced), "duplicate <id>" (this id
//  was already charged: nothing happens), or "declined <id>" (not enough
//  money: nothing happens, and the SAME id may be retried later). Unknown
//  accounts and non-positive amounts → IllegalArgumentException.
//
//      topUp("acct", 500)
//      apply(pay-1, acct, 300)  → "charged pay-1"     balance 200
//      apply(pay-1, acct, 300)  → "duplicate pay-1"   balance 200
//      apply(pay-2, acct, 900)  → "declined pay-2"    (retry after a topUp)
//
//  hint: remember an id only once it has actually been charged.

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
            throw new UnsupportedOperationException("TODO");
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
