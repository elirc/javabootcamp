// ─────────────────────────────────────────────────────────────────────────
//  02 · the average that rounds itself                      ★☆☆ warm-up
//  concepts: int division · casts · Math.round · overflow
//  run: java -cp ../../_lib/out 02-integer-division.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A quiz engine shows each player's average score and a percentage.
//  Write average (the mean of the scores, fraction kept; no scores → 0.0)
//  and percent (part out of whole, rounded to the nearest whole percent,
//  halves rounding up; a whole of 0 → 0).
//
//      average(new int[] {1, 2})          → 1.5
//      average(new int[] {})              → 0.0
//      percent(2, 3)                      → 67
//      percent(1, 8)                      → 13     (12.5 rounds up)
//      percent(30_000_000, 60_000_000)    → 50
//
//  hint: int / int is decided before any assignment sees the result.

import static bootcamp.Check.*;

class Exercise {

    static double average(int[] scores) {
        throw new UnsupportedOperationException("TODO");
    }

    static int percent(int part, int whole) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("average keeps the fraction", () -> eq(average(new int[] {1, 2}), 1.5));
        test("average of one score is that score", () -> eq(average(new int[] {7}), 7.0));
        test("average of no scores is zero", () -> eq(average(new int[] {}), 0.0));
        test("average of thirds", () -> approx(average(new int[] {1, 1, 0}), 2.0 / 3, 1e-12));
        test("percent rounds to the nearest whole", () -> {
            eq(percent(2, 3), 67);
            eq(percent(1, 3), 33);
        });
        test("percent rounds a half up", () -> eq(percent(1, 8), 13));
        test("percent of a zero whole is zero", () -> eq(percent(5, 0), 0));
        test("percent survives big counts", () -> eq(percent(30_000_000, 60_000_000), 50));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
