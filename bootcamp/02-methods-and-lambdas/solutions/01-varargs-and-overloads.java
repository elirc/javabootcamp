// ─────────────────────────────────────────────────────────────────────────
//  01 · any number of arguments — SOLUTION                  ★☆☆ warm-up
//  concepts: varargs · overloading · static methods · StringBuilder
//  run: java -cp ../../_lib/out 01-varargs-and-overloads.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  `int... values` is syntax sugar for an int[] parameter: the CALLER may
//  list values, pass nothing (an empty array, never null), or hand over an
//  existing array — all three end up as the same int[] inside. So sum is
//  an ordinary loop over an array.
//  tags shows the idiom for "at least one": a normal parameter first, then
//  the varargs. `tags()` with no arguments simply does not compile, which
//  beats a runtime check. Only the LAST parameter may be varargs.
//  Building output in a loop wants a StringBuilder (or a StringJoiner /
//  String.join over a filtered list), not `s += ...`, which copies the
//  whole string each time. Here the blank-tag rule makes the separator
//  fiddly with a raw StringBuilder; StringJoiner(" ") puts separators only
//  BETWEEN the parts it actually receives.
//  Overloading note: Java picks an overload at COMPILE time and prefers
//  widening, then boxing, then varargs — so a varargs overload is the last
//  resort, and adding a sum(int, int) later would silently take over every
//  two-argument call.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static int sum(int... values) {
        int total = 0;
        for (int v : values) total += v;
        return total;
    }

    static String tags(String first, String... rest) {
        var out = new StringJoiner(" ");
        var all = new ArrayList<String>();
        all.add(first);
        all.addAll(Arrays.asList(rest));
        for (var tag : all) {
            if (!tag.isBlank()) out.add("#" + tag.trim());
        }
        return out.toString();
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
