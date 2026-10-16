// ─────────────────────────────────────────────────────────────────────────
//  02 · which way is half                                   ★★☆ core
//  concepts: setScale · RoundingMode · divide with a scale
//  run: java -cp ../../_lib/out 02-rounding-modes.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An order system computes tax and splits costs. Write tax (net times a
//  percentage rate, to cents, halves rounded UP), roundBankers (to cents,
//  halves rounded to the EVEN neighbour) and perPerson (a total divided by
//  a head count, to cents, halves up). Every result has exactly two
//  decimal places.
//
//      tax(19.99, 7.5)        → 1.50     (1.49925)
//      tax(0.10, 5)           → 0.01     (0.005, half up)
//      roundBankers(2.345)    → 2.34
//      roundBankers(2.355)    → 2.36
//      perPerson(10.00, 3)    → 3.33
//
//  hint: divide(x) with no scale throws when the result never ends.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

class Exercise {

    static BigDecimal tax(BigDecimal net, BigDecimal ratePercent) {
        throw new UnsupportedOperationException("TODO");
    }

    static BigDecimal roundBankers(BigDecimal amount) {
        throw new UnsupportedOperationException("TODO");
    }

    static BigDecimal perPerson(BigDecimal total, int people) {
        throw new UnsupportedOperationException("TODO");
    }

    static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("tax rounds to cents", () -> eq(tax(d("19.99"), d("7.5")).toPlainString(), "1.50"));
        test("tax keeps two decimals on a round number", () -> eq(tax(d("10"), d("20")).toPlainString(), "2.00"));
        test("tax rounds an exact half up", () -> eq(tax(d("0.10"), d("5")).toPlainString(), "0.01"));
        test("banker's rounding goes to the even cent", () -> {
            eq(roundBankers(d("2.345")).toPlainString(), "2.34");
            eq(roundBankers(d("2.355")).toPlainString(), "2.36");
        });
        test("banker's rounding is ordinary away from a half", () -> eq(roundBankers(d("2.3451")).toPlainString(), "2.35"));
        test("perPerson survives a never-ending division", () -> eq(perPerson(d("10.00"), 3).toPlainString(), "3.33"));
        test("perPerson rounds a half up", () -> eq(perPerson(d("1"), 8).toPlainString(), "0.13"));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
