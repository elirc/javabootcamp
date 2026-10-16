// ─────────────────────────────────────────────────────────────────────────
//  08 · the monthly statement — SOLUTION                    ★★★ stretch
//  concepts: ledgers · rejecting vs applying · formatted lines · enums
//  run: java -cp ../../_lib/out 08-ledger-statement.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A ledger is a fold: state (the balance) plus an ordered list of events
//  (transactions) gives a new state. A plain for loop with an index is the
//  clearest fold here, because the 1-based position appears in the
//  rejection messages.
//  Decide first, then apply. For each transaction compute whether it is
//  allowed against the CURRENT balance; only an accepted one changes the
//  balance and produces a statement line. That keeps rejected
//  transactions truly side-effect free — the same "check, then act" rule
//  as BankAccount.withdraw in module 05.
//  The kinds differ in sign and in rules, and a switch expression over
//  the enum states both compactly: DEPOSIT adds, WITHDRAWAL and FEE
//  subtract, but only WITHDRAWAL is checked against the balance. With no
//  default arm, adding a new Kind (INTEREST) is a compile error until it
//  is handled.
//  Format with setScale(2, UNNECESSARY).toPlainString(): the amounts are
//  already cents-precise, and UNNECESSARY will shout if one is not. Never
//  toString() a BigDecimal for display — 1E+3 is a legal toString of a
//  thousand.
//  The Statement record bundles three results; returning it beats
//  out-parameters or printing from inside the loop, and makes the whole
//  run testable.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

class Exercise {

    enum Kind { DEPOSIT, WITHDRAWAL, FEE }

    record Tx(Kind kind, BigDecimal amount) {}

    record Statement(List<String> lines, BigDecimal closing, List<String> rejected) {}

    static Statement run(BigDecimal opening, List<Tx> txs) {
        var balance = opening;
        var lines = new ArrayList<String>();
        var rejected = new ArrayList<String>();
        for (int i = 0; i < txs.size(); i++) {
            var tx = txs.get(i);
            int n = i + 1;
            if (tx.amount().signum() <= 0) {
                rejected.add("#" + n + ": amount must be positive");
                continue;
            }
            if (tx.kind() == Kind.WITHDRAWAL && tx.amount().compareTo(balance) > 0) {
                rejected.add("#" + n + ": insufficient funds");
                continue;
            }
            balance = switch (tx.kind()) {
                case DEPOSIT -> balance.add(tx.amount());
                case WITHDRAWAL, FEE -> balance.subtract(tx.amount());
            };
            lines.add(tx.kind() + " " + money(tx.amount()) + " -> " + money(balance));
        }
        return new Statement(lines, balance, rejected);
    }

    private static String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    static Tx tx(Kind kind, String amount) {
        return new Tx(kind, new BigDecimal(amount));
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("applies transactions in order", () -> {
            var s = run(new BigDecimal("100.00"), List.of(tx(Kind.DEPOSIT, "50"), tx(Kind.WITHDRAWAL, "30.25")));
            eq(s.lines(), List.of("DEPOSIT 50.00 -> 150.00", "WITHDRAWAL 30.25 -> 119.75"));
            eq(s.closing(), new BigDecimal("119.75"));
        });
        test("rejects an overdraft and carries on", () -> {
            var s = run(new BigDecimal("100.00"),
                List.of(tx(Kind.DEPOSIT, "50"), tx(Kind.WITHDRAWAL, "500"), tx(Kind.FEE, "2.5")));
            eq(s.lines(), List.of("DEPOSIT 50.00 -> 150.00", "FEE 2.50 -> 147.50"));
            eq(s.rejected(), List.of("#2: insufficient funds"));
            eq(s.closing(), new BigDecimal("147.50"));
        });
        test("can withdraw the whole balance", () ->
            eq(run(new BigDecimal("10"), List.of(tx(Kind.WITHDRAWAL, "10"))).closing(), BigDecimal.ZERO));
        test("fees apply even into the negative", () -> {
            var s = run(new BigDecimal("1.00"), List.of(tx(Kind.FEE, "5")));
            eq(s.lines(), List.of("FEE 5.00 -> -4.00"));
            eq(s.rejected(), List.of());
        });
        test("rejects non-positive amounts", () -> {
            var s = run(new BigDecimal("10"), List.of(tx(Kind.DEPOSIT, "0"), tx(Kind.FEE, "-1")));
            eq(s.rejected(), List.of("#1: amount must be positive", "#2: amount must be positive"));
            eq(s.lines(), List.of());
            eq(s.closing(), new BigDecimal("10"));
        });
        test("no transactions, no change", () ->
            eq(run(new BigDecimal("42.00"), List.of()), new Statement(List.of(), new BigDecimal("42.00"), List.of())));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
