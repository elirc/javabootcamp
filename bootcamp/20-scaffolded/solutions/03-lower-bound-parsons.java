// ─────────────────────────────────────────────────────────────────────────
//  03 · binary search for the first match (Parsons) — SOLUTION  ★★☆ core
//  concepts: Parsons · binary search · half-open ranges · loop invariants
//  run: java -cp ../../_lib/out 03-lower-bound-parsons.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  The invariant decides both pairs: the answer is always in [lo, hi],
//  with hi INCLUDED, and hi = sorted.length is a legal answer ("after
//  everything"). So hi starts at sorted.length, not length - 1: with
//  length - 1 a target above every value can never return the length.
//  In the loop, if sorted[mid] < target then mid and everything left of
//  it are too small: lo = mid + 1. Otherwise mid MIGHT be the answer (it
//  is >= target), so it must stay in range: hi = mid. hi = mid - 1
//  throws that candidate away and misses the leftmost duplicate.
//  The loop runs while lo < hi and ends with lo == hi, the answer. Each
//  step shrinks the range because mid < hi, so it cannot loop forever.
//  lo + (hi - lo) / 2 instead of (lo + hi) / 2 avoids int overflow for
//  arrays past a billion elements, the bug that sat in the JDK's own
//  binarySearch for nine years.
//  Arrays.binarySearch returns SOME matching index for duplicates, not
//  the first; lowerBound is what you want for ranges ("all orders from
//  this date").

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static int lowerBound(int[] sorted, int target) {
        int lo = 0;
        int hi = sorted.length;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (sorted[mid] < target) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
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
