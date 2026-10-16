// ─────────────────────────────────────────────────────────────────────────
//  05 · one interface, three discounts                      ★★☆ core
//  concepts: interfaces · records implementing interfaces · polymorphism
//  run: java -cp ../../_lib/out 05-discount-interface.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An order system supports several discount kinds behind one interface,
//  Discount (above the tests), whose apply(cents) returns the new price.
//  Finish the three records: NoDiscount; PercentOff(percent) takes that
//  percent off, the discount rounded to the nearest cent, halves up;
//  AmountOff(cents) takes a fixed amount off but never goes below zero.
//  Then write best: the discount giving the lowest price (the first one
//  wins a tie; an empty list means no discount).
//
//      new PercentOff(15).apply(999)    → 849     (149.85 rounds to 150)
//      new AmountOff(500).apply(300)    → 0
//      best(List.of(pct10, off300), 2000).apply(2000)   → 1700
//
//  hint: a half-up integer rounding of a / 100 is (a + 50) / 100 for a ≥ 0.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    interface Discount {
        long apply(long cents);
    }

    record NoDiscount() implements Discount {
        public long apply(long cents) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    record PercentOff(int percent) implements Discount {
        public long apply(long cents) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    record AmountOff(long amount) implements Discount {
        public long apply(long cents) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static Discount best(List<Discount> discounts, long cents) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("no discount keeps the price", () -> eq(new NoDiscount().apply(1234), 1234));
        test("percent off takes the percentage", () -> eq(new PercentOff(10).apply(2000), 1800));
        test("percent off rounds the discount half up", () -> {
            eq(new PercentOff(15).apply(999), 849);
            eq(new PercentOff(50).apply(1), 0);
        });
        test("amount off subtracts", () -> eq(new AmountOff(300).apply(2000), 1700));
        test("amount off never goes below zero", () -> eq(new AmountOff(500).apply(300), 0));
        test("best picks the lowest price", () -> {
            List<Discount> offers = List.of(new PercentOff(10), new AmountOff(300));
            eq(best(offers, 2000), new AmountOff(300));
            eq(best(offers, 5000), new PercentOff(10));
        });
        test("best keeps the first on a tie", () ->
            eq(best(List.of(new PercentOff(10), new AmountOff(200)), 2000), new PercentOff(10)));
        test("best of nothing is no discount", () -> eq(best(List.of(), 2000).apply(2000), 2000));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
