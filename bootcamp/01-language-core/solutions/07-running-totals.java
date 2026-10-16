// ─────────────────────────────────────────────────────────────────────────
//  07 · running totals — SOLUTION                           ★☆☆ warm-up
//  concepts: arrays · for loops · copying · sentinels
//  run: java -cp ../../_lib/out 07-running-totals.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Arrays have a fixed length known up front, so runningTotal allocates
//  `new int[daily.length]` and fills it in one pass, carrying the sum in a
//  local. Writing the totals back into `daily` would pass the first test
//  and break the caller, who still thinks they hold the per-day numbers —
//  mutating an argument is a side effect nobody asked for.
//  (Arrays.copyOf + Arrays.parallelPrefix(copy, Integer::sum) is the
//  library one-liner; the loop is what you should be able to write.)
//  indexOfMax is about the starting value. Starting `max = 0` works on
//  every test with a positive number in it and silently returns the wrong
//  index for an all-negative array. Start from the first ELEMENT instead
//  (index 0), after handling the empty array, and use a strict `>` so the
//  first of several equal maxima wins. -1 as "not found" is the same
//  convention as String.indexOf and List.indexOf.

import static bootcamp.Check.*;

class Exercise {

    static int[] runningTotal(int[] daily) {
        var totals = new int[daily.length];
        int sum = 0;
        for (int i = 0; i < daily.length; i++) {
            sum += daily[i];
            totals[i] = sum;
        }
        return totals;
    }

    static int indexOfMax(int[] values) {
        if (values.length == 0) return -1;
        int best = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] > values[best]) best = i;
        }
        return best;
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
