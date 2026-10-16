// ─────────────────────────────────────────────────────────────────────────
//  03 · split the bill to the cent — SOLUTION               ★★☆ core
//  concepts: allocation · integer cents · remainders · movePointRight
//  run: java -cp ../../_lib/out 03-split-the-bill.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Rounding each share independently is the classic bug: 10.00 / 3 rounds
//  to 3.33, three times 3.33 is 9.99, and a cent vanishes from the ledger.
//  Rounding up instead gives 10.02. No rounding mode fixes it, because the
//  problem is not rounding, it is allocation.
//  Allocation works in whole cents: total.movePointRight(2) turns 10.00
//  into 1000, and longValueExact() insists there is no fractional part
//  left — it throws ArithmeticException for 10.005, which we translate to
//  IllegalArgumentException. Then base = cents / people, remainder =
//  cents % people, and the first `remainder` people get base + 1. The
//  parts now sum to the total by construction, not by luck.
//  Back to money with BigDecimal.valueOf(cents, 2) — "this many
//  hundredths" — which also gives every part scale 2 ("3.33", "0.00").
//  Who gets the extra cents is a policy; first-come is common, largest
//  remainder (Hamilton's method) when splitting by unequal weights. The
//  invariant — parts sum to the total — is not negotiable.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.util.*;

class Exercise {

    static List<BigDecimal> split(BigDecimal total, int people) {
        if (people < 1) throw new IllegalArgumentException("need at least one person: " + people);
        if (total.signum() < 0) throw new IllegalArgumentException("total must not be negative: " + total);
        long cents;
        try {
            cents = total.movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("total has more than two decimals: " + total, e);
        }
        long base = cents / people;
        long extra = cents % people;
        var parts = new ArrayList<BigDecimal>();
        for (int i = 0; i < people; i++) parts.add(BigDecimal.valueOf(base + (i < extra ? 1 : 0), 2));
        return parts;
    }

    static List<String> plain(List<BigDecimal> parts) {
        return parts.stream().map(BigDecimal::toPlainString).toList();
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("an even split", () -> eq(plain(split(new BigDecimal("9.00"), 3)), List.of("3.00", "3.00", "3.00")));
        test("the first people pay the extra cents", () ->
            eq(plain(split(new BigDecimal("10.00"), 3)), List.of("3.34", "3.33", "3.33")));
        test("the parts always add up to the total", () -> {
            var parts = split(new BigDecimal("100.01"), 7);
            eq(parts.stream().reduce(BigDecimal.ZERO, BigDecimal::add), new BigDecimal("100.01"));
            eq(parts.size(), 7);
        });
        test("fewer cents than people", () -> eq(plain(split(new BigDecimal("0.05"), 2)), List.of("0.03", "0.02")));
        test("one person pays it all", () -> eq(plain(split(new BigDecimal("12.5"), 1)), List.of("12.50")));
        test("a zero bill splits into zeros", () -> eq(plain(split(BigDecimal.ZERO, 2)), List.of("0.00", "0.00")));
        test("refuses nonsense", () -> {
            raises(IllegalArgumentException.class, () -> split(new BigDecimal("10.00"), 0));
            raises(IllegalArgumentException.class, () -> split(new BigDecimal("-1.00"), 2));
            raises(IllegalArgumentException.class, () -> split(new BigDecimal("10.005"), 2));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
