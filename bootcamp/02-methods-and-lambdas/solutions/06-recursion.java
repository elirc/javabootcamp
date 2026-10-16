// ─────────────────────────────────────────────────────────────────────────
//  06 · methods that call themselves — SOLUTION             ★★☆ core
//  concepts: recursion · base case · instanceof patterns · Math.abs trap
//  run: java -cp ../../_lib/out 06-recursion.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Every recursion is a base case plus a step that makes the problem
//  smaller. For sumDigits the base case is 0 (no digits left) and the step
//  peels off the last digit: |n % 10| + sumDigits(n / 10). Division moves
//  toward zero, so negative numbers terminate too.
//  The trap is the "obvious" first move, n = Math.abs(n). Two's complement
//  has one more negative number than positive ones, so Math.abs(Long.
//  MIN_VALUE) overflows back to Long.MIN_VALUE — still negative — and the
//  digit loop produces nonsense. Taking the absolute value of each
//  REMAINDER (always between -9 and 9) side-steps the edge entirely.
//  (Math.absExact would at least throw instead of lying.)
//  flatten recurses on structure rather than numbers: for each element,
//  if it is itself a List, flatten it and add everything; otherwise add
//  the element. `if (item instanceof List<?> inner)` is the Java 16+
//  pattern form — test and cast in one step, no (List<?>) cast. Passing an
//  accumulator list down (instead of allocating and concatenating at each
//  level) keeps it linear.
//  Java has no tail-call optimisation: recursion depth is bounded by the
//  stack (a few thousand frames). Fine for digits and nesting; use a loop
//  for anything that grows with the input size.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static int sumDigits(long n) {
        if (n == 0) return 0;
        return (int) Math.abs(n % 10) + sumDigits(n / 10);
    }

    static List<Object> flatten(List<?> nested) {
        var out = new ArrayList<Object>();
        flattenInto(nested, out);
        return out;
    }

    private static void flattenInto(List<?> nested, List<Object> out) {
        for (var item : nested) {
            if (item instanceof List<?> inner) flattenInto(inner, out);
            else out.add(item);
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("sums the digits", () -> eq(sumDigits(1234), 10));
        test("zero has digit sum zero", () -> eq(sumDigits(0), 0));
        test("ignores the sign", () -> eq(sumDigits(-56), 11));
        test("survives the most negative long", () -> {
            eq(sumDigits(Long.MAX_VALUE), 88);
            eq(sumDigits(Long.MIN_VALUE), 89);
        });
        test("flattens nested lists in order", () ->
            eq(flatten(List.of(1, List.of(2, List.of(3)), 4)), List.of(1, 2, 3, 4)));
        test("a flat list stays the same", () -> eq(flatten(List.of("a", "b")), List.of("a", "b")));
        test("empty inner lists vanish", () ->
            eq(flatten(List.of(List.of(), 1, List.of(List.of()))), List.of(1)));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
