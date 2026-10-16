// ─────────────────────────────────────────────────────────────────────────
//  05 · == is not equals                                    ★☆☆ warm-up
//  concepts: String.equals · equalsIgnoreCase · Objects.equals · string pool
//  run: java -cp ../../_lib/out 05-string-equality.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A quiz engine compares a player's typed answer with the expected one.
//  Write isCorrect: ignore surrounding spaces and letter case; no answer
//  (null) is simply wrong. Then write sameText: exact, case-sensitive
//  content equality that is safe when either side is null.
//
//      isCorrect("  paris ", "Paris")    → true
//      isCorrect(null, "Paris")          → false
//      sameText(new String("abc"), "abc") → true
//      sameText(null, null)              → true
//
//  hint: == on two Strings asks whether they are the same object.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static boolean isCorrect(String given, String expected) {
        throw new UnsupportedOperationException("TODO");
    }

    static boolean sameText(String a, String b) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("accepts the exact answer", () -> eq(isCorrect("Paris", "Paris"), true));
        test("ignores spaces and case", () -> eq(isCorrect("  paris ", "Paris"), true));
        test("accepts an answer built at runtime", () ->
            eq(isCorrect(new StringBuilder("Par").append("is").toString(), "Paris"), true));
        test("rejects a wrong answer", () -> eq(isCorrect("Lyon", "Paris"), false));
        test("no answer is wrong", () -> eq(isCorrect(null, "Paris"), false));
        test("sameText compares content, not identity", () -> eq(sameText(new String("abc"), "abc"), true));
        test("sameText is null-safe", () -> {
            eq(sameText(null, null), true);
            eq(sameText(null, "a"), false);
            eq(sameText("a", null), false);
        });
        test("sameText is case-sensitive", () -> eq(sameText("A", "a"), false));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
