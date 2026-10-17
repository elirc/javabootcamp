// ─────────────────────────────────────────────────────────────────────────
//  06 · read JSON                                           ★★★ stretch
//  concepts: recursive descent · lookahead · escapes · error positions
//  run: java -cp ../../_lib/out 06-json-reader.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Write parse(json) for the subset of exercise 05, as a recursive-
//  descent parser. Results: Map (LinkedHashMap, keys in input order),
//  List, String, Long, Boolean, null.
//  - whitespace (space, tab, LF, CR) may surround any token; after the
//    one top-level value only whitespace may follow;
//  - strings: escapes \" \\ \/ \n \r \t and backslash-u + 4 hex digits
//    (either case); a raw char below U+0020 inside a string is an error;
//  - numbers: optional "-", then "0" or digits not starting with 0; no
//    fraction or exponent; beyond long: "number out of range at N"
//    (N = where the number starts);
//  - a key that repeats in one object: "duplicate key \"k\" at N"
//    (N = its opening quote).
//  Every other error: IllegalArgumentException "unexpected 'c' at N",
//  or "unexpected end at N" when the text runs out (N = its length).
//  Offsets are 0-based.
//
//      parse("{\"a\": [1, true]}") → {a=[1, true]}
//      parse("[1 2]")              → "unexpected '2' at 3"
//
//  hint: one method per grammar rule; each reads from a shared position.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static Object parse(String json) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("reads the scalars", () -> {
            eq(parse("null"), null);
            eq(parse(" true"), true);
            eq(parse("false \n"), false);
            ok(parse("42") instanceof Long, "a number is a Long");
            eq(parse("-7"), -7L);
            eq(parse("\"hi\""), "hi");
        });
        test("reads nested arrays and objects, keeping key order", () -> {
            var v = parse("{ \"z\" : [1, 2, {\"b\":null}],\r\n\t\"a\":\"x\", \"e\": {}, \"l\": [] }");
            eq(v, Map.of("z", List.of(1, 2, Collections.singletonMap("b", null)), "a", "x", "e", Map.of(), "l", List.of()));
            eq(new ArrayList<>(((Map<?, ?>) v).keySet()), List.of("z", "a", "e", "l"));
        });
        test("decodes every escape", () -> {
            eq(parse("\"a\\\"b\\\\c\\/d\\n\\r\\t\\u00e9\\u20AC\""), "a\"b\\c/d\n\r\t\u00e9\u20ac");
        });
        test("numbers: no leading zeros, no fractions, and within long", () -> {
            eq(parse("0"), 0L);
            eq(parse("-9223372036854775808"), Long.MIN_VALUE);
            raises(IllegalArgumentException.class, () -> parse("01"), "unexpected '1' at 1");
            raises(IllegalArgumentException.class, () -> parse("[1.5]"), "unexpected '.' at 2");
            raises(IllegalArgumentException.class, () -> parse(" 9223372036854775808"), "number out of range at 1");
            raises(IllegalArgumentException.class, () -> parse("-a"), "unexpected 'a' at 1");
            raises(IllegalArgumentException.class, () -> parse("+1"), "unexpected '+' at 0");
        });
        test("an unexpected char is reported at its offset", () -> {
            raises(IllegalArgumentException.class, () -> parse("[1 2]"), "unexpected '2' at 3");
            raises(IllegalArgumentException.class, () -> parse("{\"a\" 1}"), "unexpected '1' at 5");
            raises(IllegalArgumentException.class, () -> parse("[1,]"), "unexpected ']' at 3");
            raises(IllegalArgumentException.class, () -> parse("{1:2}"), "unexpected '1' at 1");
            raises(IllegalArgumentException.class, () -> parse("[trux]"), "unexpected 'x' at 4");
        });
        test("running out of text is reported at its end", () -> {
            raises(IllegalArgumentException.class, () -> parse(""), "unexpected end at 0");
            raises(IllegalArgumentException.class, () -> parse("[1,"), "unexpected end at 3");
            raises(IllegalArgumentException.class, () -> parse("\"abc"), "unexpected end at 4");
            raises(IllegalArgumentException.class, () -> parse("{\"a\":1"), "unexpected end at 6");
            raises(IllegalArgumentException.class, () -> parse("-"), "unexpected end at 1");
            raises(IllegalArgumentException.class, () -> parse("nul"), "unexpected end at 3");
        });
        test("refuses trailing text, raw control chars and bad escapes", () -> {
            raises(IllegalArgumentException.class, () -> parse("1 x"), "unexpected 'x' at 2");
            raises(IllegalArgumentException.class, () -> parse("{} {}"), "unexpected '{' at 3");
            raises(IllegalArgumentException.class, () -> parse("\"a\tb\""), "at 2");
            raises(IllegalArgumentException.class, () -> parse("\"\\q\""), "unexpected 'q' at 2");
            raises(IllegalArgumentException.class, () -> parse("\"\\u12G4\""), "unexpected 'G' at 5");
        });
        test("refuses a repeated key, pointing at its opening quote", () -> {
            raises(IllegalArgumentException.class, () -> parse("{\"a\":1,\"a\":2}"), "duplicate key \"a\" at 7");
            eq(parse("{\"a\":{\"a\":1}}"), Map.of("a", Map.of("a", 1)));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
