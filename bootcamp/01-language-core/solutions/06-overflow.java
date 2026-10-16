// ─────────────────────────────────────────────────────────────────────────
//  06 · when int runs out — SOLUTION                        ★★☆ core
//  concepts: int vs long · overflow · Math.addExact · widening
//  run: java -cp ../../_lib/out 06-overflow.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  An int is 32 bits: about ±2.1 billion. Past that it does not fail, it
//  WRAPS — Integer.MAX_VALUE + 1 is Integer.MIN_VALUE — and nothing tells
//  you. For money in cents, 2.1 billion cents is only $21 million.
//  totalCents accumulates into a long. Declaring `long sum` is enough:
//  `sum += amount` widens each int before adding. Summing into an int and
//  returning it as long is the wrong turn — the damage is already done.
//  addCents keeps the int type but asks the JDK to check: Math.addExact
//  (and multiplyExact, subtractExact, toIntExact) throw ArithmeticException
//  on overflow. Use them at the boundaries where a wrap would be a bug, not
//  a feature.
//  secondsIn is the subtle one. `long s = days * 86_400;` multiplies two
//  ints, overflows, and only THEN widens the garbage to long. Make one
//  operand long first: `days * 86_400L`. Same rule as int division — the
//  operand types decide, not the variable you assign to.

import static bootcamp.Check.*;

class Exercise {

    static long totalCents(int[] amounts) {
        long sum = 0;
        for (int amount : amounts) sum += amount;
        return sum;
    }

    static int addCents(int a, int b) {
        return Math.addExact(a, b);
    }

    static long secondsIn(int days) {
        return days * 86_400L;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("totals small amounts", () -> eq(totalCents(new int[] {100, 250}), 350));
        test("totals past the int limit", () ->
            eq(totalCents(new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE}), 4294967294L));
        test("an empty total is zero", () -> eq(totalCents(new int[] {}), 0));
        test("addCents adds, refunds included", () -> {
            eq(addCents(2, 3), 5);
            eq(addCents(-5, 3), -2);
        });
        test("addCents refuses to overflow", () ->
            raises(ArithmeticException.class, () -> addCents(Integer.MAX_VALUE, 1)));
        test("addCents refuses to underflow", () ->
            raises(ArithmeticException.class, () -> addCents(Integer.MIN_VALUE, -1)));
        test("secondsIn one day", () -> eq(secondsIn(1), 86_400));
        test("secondsIn a very long loan", () -> eq(secondsIn(30_000), 2_592_000_000L));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
