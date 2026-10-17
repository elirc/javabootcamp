// ─────────────────────────────────────────────────────────────────────────
//  01 · the shrinking average — SOLUTION                    ★☆☆ warm-up
//  concepts: debugging · int overflow · integer division · numeric types
//  run: java -cp ../../_lib/out 01-order-stats.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Two bugs, both about types, not logic.
//  1. Reproduce "the total is negative": three orders of 1.5 billion
//     cents. The method RETURNS long, which looks safe, but the
//     accumulator is an int: it wraps past 2,147,483,647 long before the
//     return widens it. Narrow it by printing total after each add; the
//     sign flips on the second order. Fix: accumulate in a long. Pinned
//     by "a big month totals without wrapping around".
//  2. Reproduce "150.0": average of 100 and 201. long / int is integer
//     division; the fraction is gone BEFORE the result becomes a double,
//     so declaring the method double does not help. Fix: divide as
//     double, (double) total / size. Pinned by "the average keeps its
//     fraction".
//  Lesson: a wider RETURN type does not widen the arithmetic inside. The
//  type of an expression is decided by its operands (module 01,
//  exercises 02 and 06). Math.addExact would have thrown at the first
//  overflow instead of returning garbage; use it where wrapping is a
//  bug rather than a feature.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Order(String id, int cents) {}

    static long totalCents(List<Order> orders) {
        long total = 0;
        for (var o : orders) {
            total += o.cents();
        }
        return total;
    }

    static double averageCents(List<Order> orders) {
        if (orders.isEmpty()) return 0.0;
        return (double) totalCents(orders) / orders.size();
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("totals a few small orders", () -> {
            eq(totalCents(List.of(new Order("a", 100), new Order("b", 250), new Order("c", 5))), 355);
        });
        test("an empty list totals 0 and averages 0.0", () -> {
            eq(totalCents(List.of()), 0);
            eq(averageCents(List.of()), 0.0);
        });
        test("refunds count as negative cents", () -> {
            eq(totalCents(List.of(new Order("a", 500), new Order("r", -200))), 300);
            eq(averageCents(List.of(new Order("a", 500), new Order("r", -200))), 150.0);
        });
        test("the average keeps its fraction", () -> {
            eq(averageCents(List.of(new Order("a", 100), new Order("b", 201))), 150.5);
            approx(averageCents(List.of(new Order("a", 1), new Order("b", 1), new Order("c", 2))), 4.0 / 3, 1e-9);
        });
        test("a big month totals without wrapping around", () -> {
            var big = new Order("x", 1_500_000_000);
            eq(totalCents(List.of(big, big, big)), 4_500_000_000L);
        });
        test("big orders average correctly", () -> {
            var big = new Order("x", 2_000_000_000);
            eq(averageCents(List.of(big, big)), 2.0e9);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
