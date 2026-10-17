// ─────────────────────────────────────────────────────────────────────────
//  07 · the accountant's cent — SOLUTION                    ★★☆ core
//  concepts: debugging · BigDecimal equals vs compareTo · rounding modes
//  run: java -cp ../../_lib/out 07-invoice-money.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  1. Reproduce "0.52": tax(10.50, "0.05"). Narrow: the product is
//     exactly 0.5250, a half. HALF_EVEN (banker's rounding) sends a half
//     to the even neighbour, 0.52; invoices use HALF_UP, 0.53. Fix the
//     mode. Pinned by "tax rounds half up to cents" (0.10 x 5% = 0.005
//     is the sharpest case). Halves are rare in random data, which is
//     why this survives casual testing.
//  2. Reproduce "0.00 went to checkout": isFree(new BigDecimal("0.00")).
//     BigDecimal.equals compares value AND scale: 0.00 (scale 2) is not
//     equal to 0 (scale 0). Fix: compareTo(...) == 0, or signum() == 0.
//     Pinned by "any zero total is free, whatever its scale".
//  3. Reproduce "3 distinct prices": the same equals, inside HashSet
//     (and hashCode also depends on the scale). Fix: a TreeSet, which
//     uses compareTo, or strip trailing zeros before hashing. Pinned by
//     "distinct prices compare by value, not by scale".
//  Lesson (module 07): for BigDecimal, equals means "same number AND
//  same representation"; almost every business question wants
//  compareTo. Watch for it in equals(), HashSet, HashMap keys and
//  distinct() on streams.

import static bootcamp.Check.*;

import java.math.*;
import java.util.*;

class Exercise {

    static BigDecimal lineTotal(String unitPrice, int qty) {
        return new BigDecimal(unitPrice).multiply(BigDecimal.valueOf(qty));
    }

    static BigDecimal tax(BigDecimal amount, String rate) {
        return amount.multiply(new BigDecimal(rate)).setScale(2, RoundingMode.HALF_UP);
    }

    static boolean isFree(BigDecimal total) {
        return total.compareTo(BigDecimal.ZERO) == 0;
    }

    static int distinctPrices(List<BigDecimal> prices) {
        return new TreeSet<>(prices).size();
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
