// ─────────────────────────────────────────────────────────────────────────
//  04 · a Money type — SOLUTION                             ★★☆ core
//  concepts: value objects · normalised scale · currency checks · records
//  run: java -cp ../../_lib/out 04-money-record.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A Money type ends three bug classes at once: adding euros to dollars,
//  amounts with a random scale, and "is this cents or units?". The record
//  gives value equality; the compact constructor makes every instance
//  valid.
//  The equality detail: a record's equals calls BigDecimal.equals on the
//  amount, and BigDecimal.equals compares scale — so without
//  normalisation new Money(12.5, EUR) and new Money(12.50, EUR) would be
//  different, and different in a HashMap. Normalising the scale in the
//  constructor makes equals mean what people mean.
//  setScale(2, RoundingMode.UNNECESSARY) is the strict normaliser: it pads
//  12.5 to 12.50 and THROWS ArithmeticException for 12.345 instead of
//  silently rounding money away. Translate that into
//  IllegalArgumentException with a message.
//  plus/minus check the currency first — a mismatch is a programming
//  error at that point, so unchecked. Results go through the constructor
//  again, so they are normalised for free. times(int) is exact: an amount
//  with scale 2 times an integer still has scale 2.
//  A static factory, Money.of("12.5", "EUR"), keeps call sites readable
//  and makes the String constructor of BigDecimal the default path.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

class Exercise {

    record Money(BigDecimal amount, String currency) {
        Money {
            if (currency == null || !currency.matches("[A-Z]{3}")) {
                throw new IllegalArgumentException("currency must be 3 capital letters: " + currency);
            }
            try {
                amount = amount.setScale(2, RoundingMode.UNNECESSARY);
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("more than two decimals: " + amount, e);
            }
        }

        static Money of(String amount, String currency) {
            return new Money(new BigDecimal(amount), currency);
        }

        Money plus(Money other) {
            requireSameCurrency(other);
            return new Money(amount.add(other.amount), currency);
        }

        Money minus(Money other) {
            requireSameCurrency(other);
            return new Money(amount.subtract(other.amount), currency);
        }

        Money times(int quantity) {
            return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
        }

        @Override
        public String toString() {
            return amount.toPlainString() + " " + currency;
        }

        private void requireSameCurrency(Money other) {
            if (!currency.equals(other.currency)) {
                throw new IllegalArgumentException("currency mismatch: " + currency + " vs " + other.currency);
            }
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
