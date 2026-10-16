// ─────────────────────────────────────────────────────────────────────────
//  01 · any number of arguments                             ★☆☆ warm-up
//  concepts: varargs · overloading · static methods · StringBuilder
//  run: java -cp ../../_lib/out 01-varargs-and-overloads.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A to-do manager lets you sum estimates and print hashtags. Write sum,
//  which takes any number of ints (none at all → 0), and tags, which takes
//  at least one tag and then any number more, and renders them as
//  "#a #b #c" — tags are trimmed, and blank ones are skipped.
//
//      sum()                         → 0
//      sum(1, 2, 3)                  → 6
//      sum(new int[] {5, 5})         → 10
//      tags("java")                  → "#java"
//      tags("java", " cli ", " ")    → "#java #cli"
//
//  hint: inside the method, int... values is just an int[].

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static int sum(int... values) {
        throw new UnsupportedOperationException("TODO");
    }

    static String tags(String first, String... rest) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("sum of nothing is zero", () -> eq(sum(), 0));
        test("sum of one value", () -> eq(sum(4), 4));
        test("sum of several values", () -> eq(sum(1, 2, 3), 6));
        test("sum accepts an existing array", () -> eq(sum(new int[] {5, 5}), 10));
        test("tags renders one tag", () -> eq(tags("java"), "#java"));
        test("tags renders several, space-separated", () -> eq(tags("java", "cli", "todo"), "#java #cli #todo"));
        test("tags trims and skips blanks", () -> eq(tags("java", " cli ", " ", ""), "#java #cli"));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
