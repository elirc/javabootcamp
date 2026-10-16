// ─────────────────────────────────────────────────────────────────────────
//  01 · ten cents plus twenty cents — SOLUTION              ★☆☆ warm-up
//  concepts: BigDecimal · String constructor · valueOf · compareTo vs equals
//  run: java -cp ../../_lib/out 01-bigdecimal-basics.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  double is binary floating point: 0.1 has no exact binary
//  representation, so 0.1 + 0.2 is 0.30000000000000004. For money that
//  is not a rounding nuisance, it is a wrong ledger. BigDecimal stores an
//  unscaled integer and a scale (12345 and 2 means 123.45), so decimal
//  fractions are exact.
//  How you CREATE one matters. new BigDecimal("0.10") is exactly 0.10.
//  new BigDecimal(0.1) faithfully converts the double's binary value and
//  gives 0.1000000000000000055511151231257827021181583404541015625 — the
//  error made visible. When a double is all you have, BigDecimal.valueOf
//  (d) goes through Double.toString, which yields the shortest decimal
//  that round-trips: "0.1".
//  Sum with reduce(BigDecimal.ZERO, BigDecimal::add): ZERO is the identity,
//  so the empty list is handled for free. BigDecimal is immutable —
//  `total.add(x);` on its own line does nothing.
//  equals compares value AND scale: 2.50 has scale 2, 2.5 has scale 1, so
//  they are not equals — and a HashSet would hold both. compareTo ignores
//  scale; "same amount" is compareTo(...) == 0. (This bootcamp's eq()
//  compares BigDecimals with compareTo too, which is why the tests check
//  toPlainString() when the scale matters.)

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.util.*;

class Exercise {

    static BigDecimal sumPrices(List<String> prices) {
        return prices.stream().map(BigDecimal::new).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static BigDecimal fromDouble(double value) {
        return BigDecimal.valueOf(value);
    }

    static boolean sameAmount(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) == 0;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("adds decimal prices exactly", () -> eq(sumPrices(List.of("0.10", "0.20")).toPlainString(), "0.30"));
        test("adds many prices", () -> eq(sumPrices(List.of("19.99", "0.01", "5.50")).toPlainString(), "25.50"));
        test("no prices sum to zero", () -> eq(sumPrices(List.of()), BigDecimal.ZERO));
        test("fromDouble gives the decimal a human expects", () -> {
            eq(fromDouble(0.1).toPlainString(), "0.1");
            eq(fromDouble(2.675).toPlainString(), "2.675");
        });
        test("sameAmount ignores scale", () -> eq(sameAmount(new BigDecimal("2.50"), new BigDecimal("2.5")), true));
        test("sameAmount still tells amounts apart", () -> eq(sameAmount(new BigDecimal("2.50"), new BigDecimal("2.51")), false));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
