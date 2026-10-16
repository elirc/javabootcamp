// ─────────────────────────────────────────────────────────────────────────
//  04 · a Money type                                        ★★☆ core
//  concepts: value objects · normalised scale · currency checks · records
//  run: java -cp ../../_lib/out 04-money-record.java
// ─────────────────────────────────────────────────────────────────────────
//
//  The bank simulator stops passing bare BigDecimals around. Finish the
//  nested record Money: its compact constructor requires a currency of
//  three capital letters and normalises the amount to exactly two decimals
//  (more decimals than that → IllegalArgumentException), so 12.5 EUR and
//  12.50 EUR are EQUAL records. plus and minus refuse a different currency
//  (IllegalArgumentException); times multiplies by a whole quantity;
//  toString is "12.50 EUR".
//
//      Money.of("12.5", "EUR").equals(Money.of("12.50", "EUR")) → true
//      Money.of("1", "EUR").plus(Money.of("1", "USD"))
//                                           → IllegalArgumentException
//      Money.of("2.25", "EUR").times(3).toString()   → "6.75 EUR"
//
//  hint: setScale(2, RoundingMode.UNNECESSARY) throws if it would round.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

class Exercise {

    record Money(BigDecimal amount, String currency) {
        Money {
            throw new UnsupportedOperationException("TODO");
        }

        static Money of(String amount, String currency) {
            return new Money(new BigDecimal(amount), currency);
        }

        Money plus(Money other) {
            throw new UnsupportedOperationException("TODO");
        }

        Money minus(Money other) {
            throw new UnsupportedOperationException("TODO");
        }

        Money times(int quantity) {
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        public String toString() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("normalises to two decimals", () -> eq(Money.of("12.5", "EUR").amount().toPlainString(), "12.50"));
        test("equal amounts are equal records", () -> {
            ok(Money.of("12.5", "EUR").equals(Money.of("12.50", "EUR")), "12.5 EUR should equal 12.50 EUR");
            eq(Money.of("12.5", "EUR").hashCode(), Money.of("12.50", "EUR").hashCode());
        });
        test("refuses a bad currency", () -> {
            raises(IllegalArgumentException.class, () -> Money.of("1", "eur"));
            raises(IllegalArgumentException.class, () -> Money.of("1", "EURO"));
        });
        test("refuses fractions of a cent", () -> raises(IllegalArgumentException.class, () -> Money.of("1.005", "EUR")));
        test("adds and subtracts", () -> {
            eq(Money.of("10", "EUR").plus(Money.of("2.5", "EUR")).toString(), "12.50 EUR");
            eq(Money.of("10", "EUR").minus(Money.of("12.25", "EUR")).toString(), "-2.25 EUR");
        });
        test("refuses to mix currencies", () -> {
            raises(IllegalArgumentException.class, () -> Money.of("1", "EUR").plus(Money.of("1", "USD")));
            raises(IllegalArgumentException.class, () -> Money.of("1", "EUR").minus(Money.of("1", "USD")));
        });
        test("multiplies by a quantity", () -> eq(Money.of("2.25", "EUR").times(3).toString(), "6.75 EUR"));
        test("prints with two decimals", () -> eq(Money.of("7", "USD").toString(), "7.00 USD"));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
