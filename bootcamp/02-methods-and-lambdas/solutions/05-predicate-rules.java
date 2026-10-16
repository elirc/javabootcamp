// ─────────────────────────────────────────────────────────────────────────
//  05 · validation rules as values — SOLUTION               ★★☆ core
//  concepts: Predicate · and/negate · higher-order functions · LinkedHashMap
//  run: java -cp ../../_lib/out 05-predicate-rules.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A rule is just a Predicate<String>, so rules can be stored in lists and
//  maps, passed around and combined — validation becomes data instead of
//  a staircase of ifs.
//  notBlank must handle null BEFORE calling isBlank, or the rule itself
//  throws on the very input it exists to reject. maxLength(n) returns a
//  lambda that captures n: a function that builds functions.
//  allOf folds the list with Predicate::and, starting from the identity
//  for "and", which is a predicate that always says true:
//  rules.stream().reduce(x -> true, Predicate::and). That starting value
//  is why "no rules" passes — the same reason an empty sum is 0.
//  failures walks the map's entries and keeps the keys whose rule fails.
//  The order guarantee comes from the MAP, not from you: a HashMap would
//  return names in hash order. The tests pass a LinkedHashMap (insertion
//  order); TreeMap would sort by name. Pick the map that states the order
//  you promise.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    static Predicate<String> notBlank() {
        return s -> s != null && !s.isBlank();
    }

    static Predicate<String> maxLength(int n) {
        return s -> s != null && s.length() <= n;
    }

    static <T> Predicate<T> allOf(List<Predicate<T>> rules) {
        return rules.stream().reduce(x -> true, Predicate::and);
    }

    static <T> List<String> failures(T value, Map<String, Predicate<T>> rules) {
        var failed = new ArrayList<String>();
        for (var rule : rules.entrySet()) {
            if (!rule.getValue().test(value)) failed.add(rule.getKey());
        }
        return failed;
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
