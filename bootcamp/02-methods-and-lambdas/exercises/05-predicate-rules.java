// ─────────────────────────────────────────────────────────────────────────
//  05 · validation rules as values                          ★★☆ core
//  concepts: Predicate · and/negate · higher-order functions · LinkedHashMap
//  run: java -cp ../../_lib/out 05-predicate-rules.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A quiz engine validates new question text with small named rules.
//  Write notBlank (null or whitespace-only fails), maxLength(n), allOf
//  (passes when every rule passes; no rules at all → passes) and failures,
//  which returns the NAMES of the rules a value breaks, in the map's order.
//
//      notBlank().test("  ")                         → false
//      maxLength(3).test("abcd")                     → false
//      allOf(List.of(notBlank(), maxLength(5)))
//          .test("hello")                            → true
//      failures("", {"required"=notBlank(), "short"=maxLength(3)})
//                                                    → ["required"]
//
//  hint: Predicate already has and(...), and an empty "and" is true.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    static Predicate<String> notBlank() {
        throw new UnsupportedOperationException("TODO");
    }

    static Predicate<String> maxLength(int n) {
        throw new UnsupportedOperationException("TODO");
    }

    static <T> Predicate<T> allOf(List<Predicate<T>> rules) {
        throw new UnsupportedOperationException("TODO");
    }

    static <T> List<String> failures(T value, Map<String, Predicate<T>> rules) {
        throw new UnsupportedOperationException("TODO");
    }

    static Map<String, Predicate<String>> questionRules() {
        var rules = new LinkedHashMap<String, Predicate<String>>();
        rules.put("required", notBlank());
        rules.put("short", maxLength(3));
        rules.put("ends with ?", s -> s != null && s.endsWith("?"));
        return rules;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("notBlank accepts text", () -> eq(notBlank().test("a"), true));
        test("notBlank rejects whitespace and null", () -> {
            eq(notBlank().test("  "), false);
            eq(notBlank().test(null), false);
        });
        test("maxLength is inclusive", () -> {
            eq(maxLength(3).test("abc"), true);
            eq(maxLength(3).test("abcd"), false);
        });
        test("allOf needs every rule", () -> {
            var rule = allOf(List.of(notBlank(), maxLength(5)));
            eq(rule.test("hello"), true);
            eq(rule.test("hello!"), false);
            eq(rule.test(" "), false);
        });
        test("allOf with no rules passes", () -> eq(allOf(List.<Predicate<String>>of()).test("x"), true));
        test("failures names broken rules in order", () ->
            eq(failures("", questionRules()), List.of("required", "ends with ?")));
        test("failures is empty for a valid value", () -> eq(failures("Hi?", questionRules()), List.of()));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
