// ─────────────────────────────────────────────────────────────────────────
//  02 · which way is half — SOLUTION                        ★★☆ core
//  concepts: setScale · RoundingMode · divide with a scale
//  run: java -cp ../../_lib/out 02-rounding-modes.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Rounding is a business decision, so BigDecimal makes you name it.
//  setScale(2, RoundingMode.HALF_UP) is "school rounding" (0.005 → 0.01),
//  which is what most invoices and tax authorities expect. HALF_EVEN
//  ("banker's rounding") sends exact halves to the even neighbour — 2.345
//  → 2.34 but 2.355 → 2.36 — so over millions of transactions the
//  rounding errors cancel instead of drifting upward. Neither is "right";
//  the spec is.
//  Do the arithmetic first and round ONCE at the end: tax = net × rate ÷
//  100 is exact in BigDecimal (multiply never loses digits, and dividing
//  by 100 just moves the point), then one setScale. Rounding intermediate
//  steps compounds the error.
//  divide is the trap. 10.00 / 3 has no finite decimal expansion, so
//  `total.divide(BigDecimal.valueOf(3))` throws ArithmeticException
//  ("Non-terminating decimal expansion"). Always give divide a scale and
//  a rounding mode — divide(divisor, 2, HALF_UP) — or a MathContext.
//  Results with scale 2 print as "1.50", not "1.5": the scale IS the
//  formatting, which is why the tests check toPlainString().

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

class Exercise {

    static BigDecimal tax(BigDecimal net, BigDecimal ratePercent) {
        return net.multiply(ratePercent).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal roundBankers(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    static BigDecimal perPerson(BigDecimal total, int people) {
        return total.divide(BigDecimal.valueOf(people), 2, RoundingMode.HALF_UP);
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
