// ─────────────────────────────────────────────────────────────────────────
//  02 · the average that rounds itself — SOLUTION           ★☆☆ warm-up
//  concepts: int division · casts · Math.round · overflow
//  run: java -cp ../../_lib/out 02-integer-division.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Java decides the type of an arithmetic expression from its operands,
//  not from where the result is going. `double avg = sum / n;` with two
//  ints divides as ints (1 / 2 is 0), THEN widens 0 to 0.0. The fix is to
//  make one operand a double before the division: `(double) sum / n`.
//  Casting the result — `(double) (sum / n)` — is the classic wrong turn;
//  it converts an already-truncated number.
//  percent has a second trap: `part * 100 / whole` multiplies two ints
//  first, and 30 million times 100 is past Integer.MAX_VALUE (about 2.1
//  billion), so it silently wraps negative. Writing `100.0 * part` makes
//  the whole expression double from the first operation.
//  Math.round(double) returns a long and rounds halves UP (toward positive
//  infinity), which is exactly "nearest, halves up" for positive numbers;
//  the (int) cast is safe because a percentage fits.
//  The empty-array case is not a detail: 0 / 0 in doubles is NaN, which
//  then poisons every comparison it touches. Guard it explicitly. (A
//  stream alternative: IntStream.of(scores).average().orElse(0.0).)

import static bootcamp.Check.*;

class Exercise {

    static double average(int[] scores) {
        if (scores.length == 0) return 0.0;
        long sum = 0;
        for (int score : scores) sum += score;
        return (double) sum / scores.length;
    }

    static int percent(int part, int whole) {
        if (whole == 0) return 0;
        return (int) Math.round(100.0 * part / whole);
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
