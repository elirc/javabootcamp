// ─────────────────────────────────────────────────────────────────────────
//  05 · is this a valid deposit? — SOLUTION                 ★☆☆ warm-up
//  concepts: validation · shape checks · error lists · stripTrailingZeros
//  run: java -cp ../../_lib/out 05-validate-amount.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Validation has two layers. First the SHAPE: is this text a number in
//  the format we accept? Then the RULES: is the number acceptable? Mixing
//  them produces messages like "must be positive" for "abc".
//  BigDecimal's parser is generous — "1e3", "+5", ".5" all parse — so
//  "it parsed" is not the same as "it is what a user should type into a
//  money field". A regex states the accepted shape exactly:
//  -?\d+(\.\d+)?. Anything else stops at "not a number"; later rules
//  would only add noise.
//  Once parsed, the rules are independent, so collect all that apply:
//  users fix everything in one round instead of playing whack-a-mole.
//  "At most 2 decimals" is about meaningful digits: 1.500 is 1.5 with
//  padding, and stripTrailingZeros().scale() tells the real number of
//  decimals (for 100, stripTrailingZeros gives 1E+2 with a NEGATIVE
//  scale, which is fine — it is at most 2).
//  Comparisons use compareTo against constants (BigDecimal.ZERO, a static
//  final LIMIT), never equals, so "10000.0" and "10000.00" behave alike.
//  Return an empty list for valid input: callers check isEmpty(), and
//  there is no null to forget.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.util.*;

class Exercise {

    static List<String> validateAmount(String raw) {
        if (raw == null || raw.isBlank()) return List.of("required");
        var text = raw.trim();
        if (!text.matches("-?\\d+(\\.\\d+)?")) return List.of("not a number");
        var amount = new BigDecimal(text);
        var errors = new ArrayList<String>();
        if (amount.signum() <= 0) errors.add("must be positive");
        if (amount.stripTrailingZeros().scale() > 2) errors.add("at most 2 decimals");
        if (amount.compareTo(LIMIT) > 0) errors.add("at most 10000.00");
        return errors;
    }

    private static final BigDecimal LIMIT = new BigDecimal("10000.00");

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
