// ─────────────────────────────────────────────────────────────────────────
//  05 · is this a valid deposit?                            ★☆☆ warm-up
//  concepts: validation · shape checks · error lists · stripTrailingZeros
//  run: java -cp ../../_lib/out 05-validate-amount.java
// ─────────────────────────────────────────────────────────────────────────
//
//  The bank simulator's deposit prompt validates what the user typed and
//  lists EVERY problem, in this order. Blank or missing → just
//  ["required"]. Not plain digits with an optional decimal part (so no
//  "1e3", no "+5", no "12,50") → just ["not a number"]. Otherwise collect:
//  "must be positive" (zero or less), "at most 2 decimals" (trailing zeros
//  don't count: 1.500 is fine), "at most 10000.00" (more than that).
//
//      validateAmount("12.50")    → []
//      validateAmount("1e3")      → ["not a number"]
//      validateAmount("-0.001")
//          → ["must be positive", "at most 2 decimals"]
//
//  hint: new BigDecimal("1e3") succeeds — check the shape first.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.util.*;

class Exercise {

    static List<String> validateAmount(String raw) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("accepts a normal amount", () -> {
            eq(validateAmount("12.50"), List.of());
            eq(validateAmount(" 7 "), List.of());
        });
        test("requires something", () -> {
            eq(validateAmount(""), List.of("required"));
            eq(validateAmount(null), List.of("required"));
        });
        test("refuses text that is not a plain number", () -> {
            eq(validateAmount("abc"), List.of("not a number"));
            eq(validateAmount("1e3"), List.of("not a number"));
            eq(validateAmount("12,50"), List.of("not a number"));
        });
        test("refuses zero and negatives", () -> {
            eq(validateAmount("0"), List.of("must be positive"));
            eq(validateAmount("-5"), List.of("must be positive"));
        });
        test("allows trailing zeros but not extra decimals", () -> {
            eq(validateAmount("1.500"), List.of());
            eq(validateAmount("0.001"), List.of("at most 2 decimals"));
        });
        test("lists every problem in order", () ->
            eq(validateAmount("-0.001"), List.of("must be positive", "at most 2 decimals")));
        test("caps the amount", () -> {
            eq(validateAmount("10000.00"), List.of());
            eq(validateAmount("10000.01"), List.of("at most 10000.00"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
