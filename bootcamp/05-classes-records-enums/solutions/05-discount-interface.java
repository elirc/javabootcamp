// ─────────────────────────────────────────────────────────────────────────
//  05 · one interface, three discounts — SOLUTION           ★★☆ core
//  concepts: interfaces · records implementing interfaces · polymorphism
//  run: java -cp ../../_lib/out 05-discount-interface.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  An interface names a capability — "can turn a price into a discounted
//  price" — and lets unrelated types provide it. The caller holds a
//  Discount and calls apply; WHICH apply runs is decided at runtime by the
//  object's class. That is polymorphism, and it is what lets best() treat
//  every discount kind identically and lets you add a BuyOneGetOne next
//  month without touching best().
//  Records can implement interfaces, which makes small strategy objects
//  like these one-liners with free equals and toString.
//  PercentOff stays in integer cents: the discount is cents * percent /
//  100, rounded half up as (cents * percent + 50) / 100. Doing it in
//  double (cents * 0.15) works until it doesn't — 0.15 is not exactly
//  representable, and some prices round the wrong way.
//  AmountOff clamps with Math.max(0, ...): a 5.00 voucher on a 3.00 item
//  makes it free, not -2.00.
//  best is a min-by over apply: a loop keeping the best so far (strict <
//  keeps the first on a tie) or stream().min(comparingLong(d ->
//  d.apply(cents))).orElse(new NoDiscount()). Seed the loop with the
//  first CANDIDATE, not with NoDiscount: a list holding only AmountOff(0)
//  must answer AmountOff(0). An empty list returning NoDiscount is the
//  "null object" pattern — the caller never has to check for null.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    interface Discount {
        long apply(long cents);
    }

    record NoDiscount() implements Discount {
        public long apply(long cents) {
            return cents;
        }
    }

    record PercentOff(int percent) implements Discount {
        public long apply(long cents) {
            long off = (cents * percent + 50) / 100;
            return cents - off;
        }
    }

    record AmountOff(long amount) implements Discount {
        public long apply(long cents) {
            return Math.max(0, cents - amount);
        }
    }

    static Discount best(List<Discount> discounts, long cents) {
        Discount best = null;
        long bestPrice = 0;
        for (var d : discounts) {
            long price = d.apply(cents);
            if (best == null || price < bestPrice) {
                best = d;
                bestPrice = price;
            }
        }
        return best == null ? new NoDiscount() : best;
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
        test("best keeps the first on a tie", () -> {
            eq(best(List.of(new PercentOff(10), new AmountOff(200)), 2000), new PercentOff(10));
            eq(best(List.of(new AmountOff(0)), 2000), new AmountOff(0));
        });
        test("best of nothing is no discount", () -> eq(best(List.of(), 2000).apply(2000), 2000));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
