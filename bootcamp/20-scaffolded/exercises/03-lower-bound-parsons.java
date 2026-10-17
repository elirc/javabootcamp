// ─────────────────────────────────────────────────────────────────────────
//  03 · binary search for the first match (Parsons)         ★★☆ core
//  concepts: Parsons · binary search · half-open ranges · loop invariants
//  run: java -cp ../../_lib/out 03-lower-bound-parsons.java
// ─────────────────────────────────────────────────────────────────────────
//
//  PARSONS: arrange the SCRAMBLED lines into the body of lowerBound. Lines
//  marked (?) come in two pairs; of each pair exactly ONE belongs.
//  lowerBound(sorted, target) returns the FIRST index whose value is >=
//  target, or sorted.length when every value is smaller. With duplicates
//  that is the leftmost one. O(log n): no scanning.
//
//      if (sorted[mid] < target) {
//      return lo;
//      hi = mid - 1;                                            (?)
//      int lo = 0;
//      } else {
//      int hi = sorted.length;                                  (?)
//      lo = mid + 1;
//      while (lo < hi) {
//      }
//      hi = mid;                                                (?)
//      int mid = lo + (hi - lo) / 2;
//      int hi = sorted.length - 1;                              (?)
//      }
//
//      lowerBound([1, 3, 3, 3, 8], 3) → 1     lowerBound([1, 3, 8], 9) → 3
//
//  hint: keep the answer inside [lo, hi] at every step, hi included.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static int lowerBound(int[] sorted, int target) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("finds the leftmost of several equal values", () -> {
            eq(lowerBound(new int[] {1, 3, 3, 3, 8}, 3), 1);
            eq(lowerBound(new int[] {2, 2, 2, 2}, 2), 0);
        });
        test("returns the insertion point for a missing value", () -> {
            eq(lowerBound(new int[] {1, 3, 8, 13}, 5), 2);
            eq(lowerBound(new int[] {1, 3, 8, 13}, 9), 3);
        });
        test("a target below every value gives 0", () -> {
            eq(lowerBound(new int[] {5, 6, 7}, -100), 0);
        });
        test("a target above every value gives the length", () -> {
            eq(lowerBound(new int[] {1, 3, 8}, 9), 3);
            eq(lowerBound(new int[] {4}, 5), 1);
        });
        test("an empty array gives 0", () -> {
            eq(lowerBound(new int[0], 1), 0);
        });
        test("agrees with a linear scan on many arrays", () -> {
            var rnd = new Random(7);
            for (int t = 0; t < 500; t++) {
                var a = new int[rnd.nextInt(12)];
                for (int i = 0; i < a.length; i++) a[i] = rnd.nextInt(6);
                Arrays.sort(a);
                int target = rnd.nextInt(8) - 1;
                int expected = 0;
                while (expected < a.length && a[expected] < target) expected++;
                eq(lowerBound(a, target), expected);
            }
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
