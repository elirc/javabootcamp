// ─────────────────────────────────────────────────────────────────────────
//  06 · when int runs out                                   ★★☆ core
//  concepts: int vs long · overflow · Math.addExact · widening
//  run: java -cp ../../_lib/out 06-overflow.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A bank simulator keeps amounts in cents. Write three helpers:
//  totalCents sums an array of amounts without ever overflowing; addCents
//  adds two int amounts but throws ArithmeticException instead of quietly
//  wrapping around; secondsIn converts a loan period in days to seconds.
//
//      totalCents(new int[] {MAX, MAX})   → 4294967294L
//      addCents(2, 3)                     → 5
//      addCents(Integer.MAX_VALUE, 1)     → ArithmeticException
//      secondsIn(30_000)                  → 2592000000L
//
//  hint: in `long x = a * b;` the multiplication happens in int.

import static bootcamp.Check.*;

class Exercise {

    static long totalCents(int[] amounts) {
        throw new UnsupportedOperationException("TODO");
    }

    static int addCents(int a, int b) {
        throw new UnsupportedOperationException("TODO");
    }

    static long secondsIn(int days) {
        throw new UnsupportedOperationException("TODO");
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
