// ─────────────────────────────────────────────────────────────────────────
//  01 · the shrinking average                               ★☆☆ warm-up
//  concepts: debugging · int overflow · integer division · numeric types
//  run: java -cp ../../_lib/out 01-order-stats.java
// ─────────────────────────────────────────────────────────────────────────
//
//  DEBUG HUNT: the code below is complete and compiles; it has bugs. Run
//  it, read the failing tests, find and fix the bugs. Do not edit tests.
//  Intended: totalCents(orders) is the exact sum of every order's cents
//  (refunds are negative); averageCents(orders) is the exact mean as a
//  double; an empty list gives 0 and 0.0.
//  Bug reports:
//    "Our biggest client's monthly total is negative."
//    "Two orders of 100 and 201 cents average to 150.0."
//
//      totalCents([100, 201])                       → 301
//      averageCents([100, 201])                     → 150.5
//      totalCents([1_500_000_000 x 3])              → 4_500_000_000
//
//  hint: shrink each report to the smallest call; check each value's type.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Order(String id, int cents) {}

    static long totalCents(List<Order> orders) {
        int total = 0;
        for (var o : orders) {
            total += o.cents();
        }
        return total;
    }

    static double averageCents(List<Order> orders) {
        if (orders.isEmpty()) return 0.0;
        return totalCents(orders) / orders.size();
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
