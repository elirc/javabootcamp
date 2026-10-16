// ─────────────────────────────────────────────────────────────────────────
//  08 · an order nobody can tamper with — SOLUTION          ★★★ stretch
//  concepts: immutability · defensive copies · unmodifiable lists · withers
//  run: java -cp ../../_lib/out 08-immutable-order.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  `private final List<Line> lines` does not make an Order immutable. final
//  only means the FIELD can't be reassigned; the list it points at can
//  still change. There are two leaks to plug.
//  Leak 1, in: if the constructor stores the caller's list, the caller
//  still holds a reference and can add lines afterwards. Copy on the way
//  in: List.copyOf(lines) makes an unmodifiable snapshot (and throws on
//  null elements, a bonus check).
//  Leak 2, out: if lines() returns the internal list and it is mutable,
//  any reader can change the order. Because the field already holds an
//  unmodifiable copy, returning it directly is safe — that is the second
//  payoff of copying with List.copyOf rather than new ArrayList<>().
//  Validate the COPY, not the original: validating the caller's list and
//  then copying leaves a gap where another thread could change it in
//  between (time-of-check to time-of-use).
//  "Changing" an immutable object means building a new one: withLine
//  copies the current lines, adds one, and calls the constructor — which
//  re-validates. The old Order is untouched, so anyone holding it (a
//  receipt, an audit log) still sees what they were given.
//  Line is a record of immutable values, so sharing Line instances between
//  orders is safe; a mutable element type would need copying too.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Line(String sku, int quantity, long unitCents) {}

    static final class Order {
        private final List<Line> lines;

        Order(List<Line> lines) {
            var copy = List.copyOf(lines);
            if (copy.isEmpty()) throw new IllegalArgumentException("an order needs at least one line");
            for (var line : copy) {
                if (line.quantity() < 1) throw new IllegalArgumentException("quantity must be at least 1: " + line);
            }
            this.lines = copy;
        }

        List<Line> lines() {
            return lines;
        }

        Order withLine(Line line) {
            var more = new ArrayList<>(lines);
            more.add(line);
            return new Order(more);
        }

        long total() {
            return lines.stream().mapToLong(l -> l.quantity() * l.unitCents()).sum();
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
