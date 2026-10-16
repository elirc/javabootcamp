// ─────────────────────────────────────────────────────────────────────────
//  01 · ten cents plus twenty cents                         ★☆☆ warm-up
//  concepts: BigDecimal · String constructor · valueOf · compareTo vs equals
//  run: java -cp ../../_lib/out 01-bigdecimal-basics.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An expense tracker adds up prices typed as text. Write sumPrices (exact
//  decimal sum; no prices → 0), fromDouble (turn a double that came from
//  somewhere else into the BigDecimal a human would expect) and sameAmount
//  (true when two amounts are numerically equal, whatever their scale).
//
//      sumPrices(List.of("0.10", "0.20")).toPlainString()  → "0.30"
//      fromDouble(0.1).toPlainString()                      → "0.1"
//      sameAmount(new BigDecimal("2.50"), new BigDecimal("2.5")) → true
//
//  hint: new BigDecimal(0.1) is not 0.1, and 2.50 does not equals() 2.5.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.util.*;

class Exercise {

    static BigDecimal sumPrices(List<String> prices) {
        throw new UnsupportedOperationException("TODO");
    }

    static BigDecimal fromDouble(double value) {
        throw new UnsupportedOperationException("TODO");
    }

    static boolean sameAmount(BigDecimal a, BigDecimal b) {
        throw new UnsupportedOperationException("TODO");
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
