// ─────────────────────────────────────────────────────────────────────────
//  07 · running totals                                      ★☆☆ warm-up
//  concepts: arrays · for loops · copying · sentinels
//  run: java -cp ../../_lib/out 07-running-totals.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An expense tracker shows spending day by day. Write runningTotal: a NEW
//  array where each slot is the sum of the input up to and including that
//  day (the input array must not change). Then write indexOfMax: the index
//  of the largest value, the first one if several tie, -1 for no values.
//
//      runningTotal(new int[] {3, 1, 4})   → [3, 4, 8]
//      indexOfMax(new int[] {5, 1, 5})     → 0
//      indexOfMax(new int[] {-3, -1, -2})  → 1
//      indexOfMax(new int[] {})            → -1
//
//  hint: what should "the biggest so far" start as, before you look?

import static bootcamp.Check.*;

class Exercise {

    static int[] runningTotal(int[] daily) {
        throw new UnsupportedOperationException("TODO");
    }

    static int indexOfMax(int[] values) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("running total accumulates", () -> eq(runningTotal(new int[] {3, 1, 4}), new int[] {3, 4, 8}));
        test("an empty week totals to an empty array", () -> eq(runningTotal(new int[] {}), new int[] {}));
        test("leaves the input alone", () -> {
            int[] daily = {1, 2};
            int[] totals = runningTotal(daily);
            eq(daily, new int[] {1, 2});
            ok(totals != daily, "should return a new array");
        });
        test("indexOfMax finds the biggest", () -> eq(indexOfMax(new int[] {3, 9, 2}), 1));
        test("indexOfMax picks the first of a tie", () -> eq(indexOfMax(new int[] {5, 1, 5}), 0));
        test("indexOfMax works when everything is negative", () -> eq(indexOfMax(new int[] {-3, -1, -2}), 1));
        test("indexOfMax of nothing is -1", () -> eq(indexOfMax(new int[] {}), -1));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
