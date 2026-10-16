// ─────────────────────────────────────────────────────────────────────────
//  08 · an order nobody can tamper with                     ★★★ stretch
//  concepts: immutability · defensive copies · unmodifiable lists · withers
//  run: java -cp ../../_lib/out 08-immutable-order.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An order management tool passes Orders between components, so an Order
//  must be truly immutable. Finish the nested final class Order: the
//  constructor refuses an empty list and any line with quantity below 1
//  (IllegalArgumentException) and keeps its OWN copy of the lines; lines()
//  can't be used to change the order; withLine returns a new Order with
//  one more line and leaves this one alone; total is the sum of quantity
//  times unit price, in cents.
//
//      var lines = new ArrayList<>(List.of(new Line("TEA", 2, 450)));
//      var order = new Order(lines);
//      lines.add(new Line("CAKE", 1, 300));     order.lines().size() → 1
//      order.withLine(new Line("CAKE", 1, 300)).total()             → 1200
//
//  hint: final fields stop reassignment, not mutation of what they point to.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Line(String sku, int quantity, long unitCents) {}

    static final class Order {
        private final List<Line> lines;

        Order(List<Line> lines) {
            throw new UnsupportedOperationException("TODO");
        }

        List<Line> lines() {
            throw new UnsupportedOperationException("TODO");
        }

        Order withLine(Line line) {
            throw new UnsupportedOperationException("TODO");
        }

        long total() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final Line TEA = new Line("TEA", 2, 450);
    static final Line CAKE = new Line("CAKE", 1, 300);

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("totals quantity times unit price", () -> eq(new Order(List.of(TEA, CAKE)).total(), 1200));
        test("refuses an empty order", () -> raises(IllegalArgumentException.class, () -> new Order(List.of())));
        test("refuses a line with no quantity", () ->
            raises(IllegalArgumentException.class, () -> new Order(List.of(TEA, new Line("X", 0, 100)))));
        test("keeps its own copy of the lines", () -> {
            var lines = new ArrayList<>(List.of(TEA));
            var order = new Order(lines);
            lines.add(CAKE);
            eq(order.lines(), List.of(TEA));
        });
        test("lines cannot be changed through the getter", () ->
            raises(UnsupportedOperationException.class, () -> new Order(List.of(TEA)).lines().add(CAKE)));
        test("withLine returns a bigger order", () -> {
            var bigger = new Order(List.of(TEA)).withLine(CAKE);
            eq(bigger.lines(), List.of(TEA, CAKE));
            eq(bigger.total(), 1200);
        });
        test("withLine leaves the original alone", () -> {
            var order = new Order(List.of(TEA));
            order.withLine(CAKE);
            eq(order.lines(), List.of(TEA));
        });
        test("withLine validates the new line", () ->
            raises(IllegalArgumentException.class, () -> new Order(List.of(TEA)).withLine(new Line("X", -1, 5))));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
