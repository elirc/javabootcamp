// ─────────────────────────────────────────────────────────────────────────
//  02 · count the words — SOLUTION                          ★☆☆ warm-up
//  concepts: HashMap vs TreeMap · Map.merge · String.split · sorting entries
//  run: java -cp ../../_lib/out 02-word-counts.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Counting is the canonical map loop, and merge makes it one line:
//  counts.merge(word, 1, Integer::sum) — insert 1 if absent, otherwise
//  combine the old value with 1. (The pre-Java-8 version, get / null
//  check / put, is three lines and one forgotten null away from an NPE.)
//  The ORDER of a map's keys is a property of the map type: HashMap has
//  none you can rely on, LinkedHashMap keeps insertion order, TreeMap
//  sorts keys. "Iterates alphabetically" means TreeMap.
//  split has a famous edge: "The cat".split("[^a-z0-9]+") is fine, but a
//  string that STARTS with a delimiter (" hello") yields a leading empty
//  string, and the empty input "" yields [""] — one empty word. Filter out
//  empty tokens rather than special-casing each.
//  topWords sorts the entries with a two-level comparator: count
//  descending, then word ascending. Map.Entry.comparingByValue(
//  Comparator.reverseOrder()) is the first level; thenComparing(
//  Map.Entry.comparingByKey()) the tie-break. Lowercase with Locale.ROOT,
//  always, for keys.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static Map<String, Integer> wordCounts(String text) {
        var counts = new TreeMap<String, Integer>();
        for (var word : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (!word.isEmpty()) counts.merge(word, 1, Integer::sum);
        }
        return counts;
    }

    static List<String> topWords(String text, int n) {
        return wordCounts(text).entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()))
            .limit(n)
            .map(Map.Entry::getKey)
            .toList();
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("counts each word", () -> eq(wordCounts("a b a"), Map.of("a", 2, "b", 1)));
        test("ignores case and punctuation", () ->
            eq(wordCounts("The cat; the HAT."), Map.of("cat", 1, "hat", 1, "the", 2)));
        test("handles leading punctuation and the empty string", () -> {
            eq(wordCounts("  ...hello"), Map.of("hello", 1));
            eq(wordCounts(""), Map.of());
        });
        test("keys come out alphabetically", () ->
            eq(new ArrayList<>(wordCounts("pear apple fig apple").keySet()), List.of("apple", "fig", "pear")));
        test("topWords orders by frequency", () -> eq(topWords("b a b c a b", 2), List.of("b", "a")));
        test("topWords breaks ties alphabetically", () -> eq(topWords("z y x y z", 3), List.of("y", "z", "x")));
        test("topWords with n past the end returns everything", () -> eq(topWords("one two", 10), List.of("one", "two")));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
