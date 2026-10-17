// ─────────────────────────────────────────────────────────────────────────
//  07 · the accountant's cent                               ★★☆ core
//  concepts: debugging · BigDecimal equals vs compareTo · rounding modes
//  run: java -cp ../../_lib/out 07-invoice-money.java
// ─────────────────────────────────────────────────────────────────────────
//
//  DEBUG HUNT: the code below is complete and compiles; it has bugs. Run
//  it, read the failing tests, find and fix the bugs. Do not edit tests.
//  Intended: lineTotal(price, qty) = price x qty. tax(amount, rate)
//  rounds amount x rate to cents, HALF_UP (0.005 → 0.01), scale 2.
//  isFree(total) is true for ANY zero amount (0, 0.00, 0.000).
//  distinctPrices(prices) counts numerically different prices, so 2.5
//  and 2.50 count once.
//  Bug reports:
//    "Tax on 10.50 at 5% shows 0.52. Our accountant says 0.53."
//    "An order paid fully by voucher (total 0.00) went to checkout."
//    "The price report says 3 distinct prices for 2.5, 2.50 and 3."
//
//      tax(10.50, "0.05")            → 0.53
//      isFree(lineTotal("0.00", 2))  → true
//
//  hint: BigDecimal remembers more than the number. Print the scale.

import static bootcamp.Check.*;

import java.math.*;
import java.util.*;

class Exercise {

    static BigDecimal lineTotal(String unitPrice, int qty) {
        return new BigDecimal(unitPrice).multiply(BigDecimal.valueOf(qty));
    }

    static BigDecimal tax(BigDecimal amount, String rate) {
        return amount.multiply(new BigDecimal(rate)).setScale(2, RoundingMode.HALF_EVEN);
    }

    static boolean isFree(BigDecimal total) {
        return total.equals(BigDecimal.ZERO);
    }

    static int distinctPrices(List<BigDecimal> prices) {
        return new HashSet<>(prices).size();
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("a line total is price times quantity", () -> {
            eq(lineTotal("2.50", 3).toPlainString(), "7.50");
            eq(lineTotal("19.99", 0), BigDecimal.ZERO);
        });
        test("tax rounds half up to cents", () -> {
            eq(tax(new BigDecimal("10.50"), "0.05"), new BigDecimal("0.53"));
            eq(tax(new BigDecimal("0.10"), "0.05"), new BigDecimal("0.01"));
            eq(tax(new BigDecimal("10.50"), "0.05").scale(), 2);
        });
        test("tax that is not on a half rounds to the nearest cent", () -> {
            eq(tax(new BigDecimal("19.99"), "0.08").toPlainString(), "1.60");
            eq(tax(new BigDecimal("10.00"), "0.0726").toPlainString(), "0.73");
            eq(tax(new BigDecimal("10.00"), "0.0721").toPlainString(), "0.72");
        });
        test("any zero total is free, whatever its scale", () -> {
            ok(isFree(BigDecimal.ZERO), "0");
            ok(isFree(new BigDecimal("0.00")), "0.00");
            ok(isFree(lineTotal("0.00", 2)), "0.00 x 2");
            ok(isFree(new BigDecimal("5.00").subtract(new BigDecimal("5.00"))), "5.00 - 5.00");
        });
        test("a non-zero total is not free", () -> {
            ok(!isFree(new BigDecimal("0.01")), "0.01");
            ok(!isFree(new BigDecimal("-1")), "-1");
        });
        test("distinct prices compare by value, not by scale", () -> {
            eq(distinctPrices(List.of(new BigDecimal("2.5"), new BigDecimal("2.50"), new BigDecimal("3"))), 2);
            eq(distinctPrices(List.of(new BigDecimal("1.00"), new BigDecimal("1.0"), new BigDecimal("1"))), 1);
            eq(distinctPrices(List.of()), 0);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
