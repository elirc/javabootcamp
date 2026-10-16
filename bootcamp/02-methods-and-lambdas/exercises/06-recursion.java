// ─────────────────────────────────────────────────────────────────────────
//  06 · methods that call themselves                        ★★☆ core
//  concepts: recursion · base case · instanceof patterns · Math.abs trap
//  run: java -cp ../../_lib/out 06-recursion.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Two recursive helpers. sumDigits adds up the decimal digits of a long
//  (the sign is ignored). flatten turns arbitrarily nested lists into one
//  flat list, in reading order; empty inner lists simply vanish.
//
//      sumDigits(1234)                                → 10
//      sumDigits(-56)                                 → 11
//      sumDigits(Long.MIN_VALUE)                      → 89
//      flatten(List.of(1, List.of(2, List.of(3)), 4)) → [1, 2, 3, 4]
//
//  hint: Math.abs(Long.MIN_VALUE) is still negative.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static int sumDigits(long n) {
        throw new UnsupportedOperationException("TODO");
    }

    static List<Object> flatten(List<?> nested) {
        throw new UnsupportedOperationException("TODO");
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
