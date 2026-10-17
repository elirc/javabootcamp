// ─────────────────────────────────────────────────────────────────────────
//  02 · instanceof patterns and scope — SOLUTION            ★☆☆ warm-up
//  concepts: instanceof patterns · guards · flow scoping · check order
//  run: java -cp ../../_lib/out 02-instanceof-patterns.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  An instanceof PATTERN tests and binds in one step: `v instanceof Long
//  l` gives you l as a Long, no cast. A pattern can carry a GUARD with &&
//  (`v instanceof CharSequence cs && cs.toString().isBlank()`), and the
//  order of the checks is part of the logic: test the special case
//  (blank) before the general one (any CharSequence), exactly like the
//  order of catch blocks.
//  FLOW SCOPING is the less known half. After `if (!(v instanceof String
//  s)) throw ...;` the compiler knows that s is definitely bound on the
//  lines below, so requireText reads like straight-line code.
//  Narrowing is where the bugs live. (int) 3_000_000_000L silently wraps
//  to a negative number, so check the range first. A Double is whole when
//  d == Math.rint(d); NaN fails that test (NaN equals nothing) and
//  infinities fail the range check. Parsing a String with
//  Integer.parseInt throws on "99999999999"; matching the shape first,
//  then parsing as a long and range-checking, turns every bad input into
//  an empty Optional rather than an exception.
//  In Java 21 `switch (v) { case Long l when ... -> }` does the same with
//  the same rule: specific cases first.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.util.*;

class Exercise {

    static Optional<Integer> asInt(Object v) {
        if (v instanceof Integer i) return Optional.of(i);
        if (v instanceof Long l) return fits(l) ? Optional.of((int) (long) l) : Optional.empty();
        if (v instanceof Double d && d == Math.rint(d) && fits(d)) return Optional.of((int) (double) d);
        if (v instanceof String s) {
            var t = s.strip();
            if (!t.matches("-?\\d{1,18}")) return Optional.empty();
            long n = Long.parseLong(t);
            return fits(n) ? Optional.of((int) n) : Optional.empty();
        }
        return Optional.empty();
    }

    private static boolean fits(double n) {
        return n >= Integer.MIN_VALUE && n <= Integer.MAX_VALUE;
    }

    static String kind(Object v) {
        if (v == null) return "null";
        if (v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte) return "integer";
        if (v instanceof Double || v instanceof Float || v instanceof BigDecimal) return "decimal";
        if (v instanceof CharSequence cs && cs.toString().isBlank()) return "blank text";
        if (v instanceof CharSequence) return "text";
        if (v instanceof Collection<?> c) return "collection of " + c.size();
        if (v instanceof Map<?, ?> m) return "map of " + m.size();
        return "other: " + v.getClass().getSimpleName();
    }

    static String requireText(Object v, String field) {
        if (!(v instanceof String s) || s.isBlank()) throw new IllegalArgumentException(field + " must be text");
        return s.strip();
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("asInt passes Integers through and narrows Longs only when they fit", () -> {
            eq(asInt(42), Optional.of(42));
            eq(asInt(7L), Optional.of(7));
            eq(asInt((long) Integer.MIN_VALUE), Optional.of(Integer.MIN_VALUE));
            eq(asInt(3_000_000_000L), Optional.empty());
        });
        test("asInt accepts whole Doubles in range only", () -> {
            eq(asInt(3.0), Optional.of(3));
            eq(asInt(-0.0), Optional.of(0));
            eq(asInt(3.5), Optional.empty());
            eq(asInt(1e10), Optional.empty());
            eq(asInt(Double.NaN), Optional.empty());
        });
        test("asInt parses stripped digit strings and rejects the rest", () -> {
            eq(asInt(" 12 "), Optional.of(12));
            eq(asInt("-5"), Optional.of(-5));
            eq(asInt("12a"), Optional.empty());
            eq(asInt(""), Optional.empty());
            eq(asInt("99999999999"), Optional.empty());
            eq(asInt("+4"), Optional.empty());
        });
        test("asInt is empty for null, Booleans and other types", () -> {
            eq(asInt(null), Optional.empty());
            eq(asInt(true), Optional.empty());
            eq(asInt(List.of(1)), Optional.empty());
        });
        test("kind checks the blank case before the general text case", () -> {
            eq(kind("  "), "blank text");
            eq(kind(""), "blank text");
            eq(kind("hi"), "text");
            eq(kind(new StringBuilder("x")), "text");
        });
        test("kind names numbers, collections, maps and everything else", () -> {
            eq(kind(5), "integer");
            eq(kind(5L), "integer");
            eq(kind(2.5), "decimal");
            eq(kind(new BigDecimal("1.5")), "decimal");
            eq(kind(List.of(1, 2)), "collection of 2");
            eq(kind(Set.of()), "collection of 0");
            eq(kind(Map.of("a", 1)), "map of 1");
            eq(kind(null), "null");
            eq(kind(new Object()), "other: Object");
        });
        test("requireText returns stripped text and rejects anything else", () -> {
            eq(requireText("  ana ", "name"), "ana");
            var e = raises(IllegalArgumentException.class, () -> requireText(42, "name"));
            eq(e.getMessage(), "name must be text");
            raises(IllegalArgumentException.class, () -> requireText("   ", "city"));
            raises(IllegalArgumentException.class, () -> requireText(null, "city"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
