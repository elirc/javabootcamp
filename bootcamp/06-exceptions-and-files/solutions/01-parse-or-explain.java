// ─────────────────────────────────────────────────────────────────────────
//  01 · fail with a reason — SOLUTION                       ★☆☆ warm-up
//  concepts: exceptions · wrapping with a cause · messages · unchecked
//  run: java -cp ../../_lib/out 01-parse-or-explain.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Integer.parseInt already throws on "abc" — so why wrap it? Because its
//  message is "For input string: \"abc\"", which tells the user nothing
//  about WHICH field was wrong or what is allowed. A good exception says
//  what was expected, what was received, and in what context.
//  Wrapping with a cause — new IllegalArgumentException(msg, e) — keeps
//  the original exception attached: the stack trace prints "Caused by:
//  NumberFormatException..." so nothing is lost for whoever debugs it.
//  Throwing a new exception WITHOUT the cause is the classic wrong turn;
//  the root cause vanishes from every log.
//  Check null first: " 12 ".trim() is fine, null.trim() throws a
//  NullPointerException with no hint that the input was simply missing.
//  The range check comes after parsing, so "0" and "1000" produce the
//  range message, not the number message.
//  All of these are unchecked (RuntimeException subclasses): a bad
//  quantity is a caller or input error the caller may validate for, not a
//  recoverable environmental failure like a missing file. Exercise 02 uses
//  checked exceptions for the latter.

import static bootcamp.Check.*;

class Exercise {

    static int parseQuantity(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("quantity is required");
        int qty;
        try {
            qty = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a number: '" + raw.trim() + "'", e);
        }
        if (qty < 1 || qty > 999) throw new IllegalArgumentException("quantity must be between 1 and 999: " + qty);
        return qty;
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
