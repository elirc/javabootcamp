// ─────────────────────────────────────────────────────────────────────────
//  01 · fail with a reason                                  ★☆☆ warm-up
//  concepts: exceptions · wrapping with a cause · messages · unchecked
//  run: java -cp ../../_lib/out 01-parse-or-explain.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An order system reads quantities typed by people. Write parseQuantity:
//  trim the text and return it as an int from 1 to 999. Every failure is
//  an IllegalArgumentException whose message says what was wrong: missing
//  input ("required"), not a number ("not a number: 'abc'", with the
//  original NumberFormatException kept as the CAUSE), or out of range
//  ("between 1 and 999").
//
//      parseQuantity(" 12 ")   → 12
//      parseQuantity("abc")    → IllegalArgumentException
//                                "not a number: 'abc'"
//      parseQuantity("0")      → IllegalArgumentException
//                                "... between 1 and 999"
//
//  hint: new IllegalArgumentException(message, cause) keeps the original.

import static bootcamp.Check.*;

class Exercise {

    static int parseQuantity(String raw) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("parses a quantity", () -> eq(parseQuantity("12"), 12));
        test("trims spaces", () -> eq(parseQuantity(" 999 "), 999));
        test("explains a missing quantity", () -> {
            raises(IllegalArgumentException.class, () -> parseQuantity(null), "required");
            raises(IllegalArgumentException.class, () -> parseQuantity("  "), "required");
        });
        test("explains text that is not a number", () ->
            raises(IllegalArgumentException.class, () -> parseQuantity("abc"), "not a number: 'abc'"));
        test("keeps the original exception as the cause", () -> {
            var e = raises(IllegalArgumentException.class, () -> parseQuantity("1.5"));
            ok(e.getCause() instanceof NumberFormatException, "cause should be the NumberFormatException");
        });
        test("explains an out-of-range quantity", () -> {
            raises(IllegalArgumentException.class, () -> parseQuantity("0"), "between 1 and 999");
            raises(IllegalArgumentException.class, () -> parseQuantity("1000"), "between 1 and 999");
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
