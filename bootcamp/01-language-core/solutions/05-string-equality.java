// ─────────────────────────────────────────────────────────────────────────
//  05 · == is not equals — SOLUTION                         ★☆☆ warm-up
//  concepts: String.equals · equalsIgnoreCase · Objects.equals · string pool
//  run: java -cp ../../_lib/out 05-string-equality.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  `==` on objects compares references. String literals are interned
//  (every "abc" literal in a program is the same object), so `"abc" ==
//  "abc"` is true and lulls you into thinking == compares text. Any string
//  built at runtime — read from input, concatenated, StringBuilder'd,
//  `new String(...)` — is a fresh object, and == says false.
//  Strings compare by content with equals, and case-insensitively with
//  equalsIgnoreCase, which folds char by char and does NOT depend on the
//  default locale (unlike toLowerCase()). trim() handles the spaces.
//  isCorrect must check null before calling a method on `given`. Calling
//  `expected.equalsIgnoreCase(given)` would also be null-safe (equals on a
//  non-null receiver returns false for a null argument) — a common idiom
//  is putting the known-non-null value on the left: "yes".equals(input).
//  sameText is exactly Objects.equals(a, b): true if both null, false if
//  one is null, otherwise a.equals(b). Reach for it whenever either side
//  may be null.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static boolean isCorrect(String given, String expected) {
        return given != null && given.trim().equalsIgnoreCase(expected.trim());
    }

    static boolean sameText(String a, String b) {
        return Objects.equals(a, b);
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
