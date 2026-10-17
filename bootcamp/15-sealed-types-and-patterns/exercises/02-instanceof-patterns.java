// ─────────────────────────────────────────────────────────────────────────
//  02 · instanceof patterns and scope                       ★☆☆ warm-up
//  concepts: instanceof patterns · guards · flow scoping · check order
//  run: java -cp ../../_lib/out 02-instanceof-patterns.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A config loader hands you plain Objects. Finish three helpers:
//
//    asInt(v)   Integer → itself; Long → only if it fits an int; Double →
//               only if whole and within int range; String → stripped
//               text of an optional "-" and digits, if it fits an int;
//               anything else (null, Boolean, lists...) → empty
//    kind(v)    "null"; "integer" for Integer/Long/Short/Byte; "decimal"
//               for Double/Float/BigDecimal; "blank text" for a blank
//               CharSequence; "text" for any other CharSequence;
//               "collection of N" / "map of N" with the size; otherwise
//               "other: " + the class's simple name
//    requireText(v, field)  v as stripped text; if v is not a String or
//               is blank, IllegalArgumentException "<field> must be text"
//
//      asInt(3_000_000_000L)  → empty       asInt(" -12 ") → -12
//      kind("  ")             → "blank text"
//
//  hint: `if (!(v instanceof String s)) throw ...;` keeps s in scope below.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.util.*;

class Exercise {

    static Optional<Integer> asInt(Object v) {
        throw new UnsupportedOperationException("TODO");
    }

    static String kind(Object v) {
        throw new UnsupportedOperationException("TODO");
    }

    static String requireText(Object v, String field) {
        throw new UnsupportedOperationException("TODO");
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
