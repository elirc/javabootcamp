// ─────────────────────────────────────────────────────────────────────────
//  08 · the monthly statement                               ★★★ stretch
//  concepts: ledgers · rejecting vs applying · formatted lines · enums
//  run: java -cp ../../_lib/out 08-ledger-statement.java
// ─────────────────────────────────────────────────────────────────────────
//
//  The bank simulator prints a statement. Write run: start from the
//  opening balance and apply each transaction in order. A non-positive
//  amount is rejected ("#N: amount must be positive"); a WITHDRAWAL larger
//  than the balance is rejected ("#N: insufficient funds"); a FEE always
//  applies, even into the negative. Rejected transactions change nothing.
//  Each applied one adds a line "KIND amount -> balance", amounts with two
//  decimals. N counts from 1. Kind, Tx and Statement are above the tests.
//
//      run(100.00, [DEPOSIT 50, WITHDRAWAL 500, FEE 2.5])
//          → lines    ["DEPOSIT 50.00 -> 150.00", "FEE 2.50 -> 147.50"]
//            closing  147.50
//            rejected ["#2: insufficient funds"]
//
//  hint: decide accept/reject BEFORE touching the balance.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

class Exercise {

    enum Kind { DEPOSIT, WITHDRAWAL, FEE }

    record Tx(Kind kind, BigDecimal amount) {}

    record Statement(List<String> lines, BigDecimal closing, List<String> rejected) {}

    static Statement run(BigDecimal opening, List<Tx> txs) {
        throw new UnsupportedOperationException("TODO");
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
