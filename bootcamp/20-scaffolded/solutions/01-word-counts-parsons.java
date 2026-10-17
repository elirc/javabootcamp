// ─────────────────────────────────────────────────────────────────────────
//  01 · count words (Parsons) — SOLUTION                    ★☆☆ warm-up
//  concepts: Parsons problem · Map.merge · regex split · TreeMap
//  run: java -cp ../../_lib/out 01-word-counts-parsons.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  The order is forced by what each line needs: the map must exist before
//  the loop, the skip must come first inside the loop, and the return
//  goes after it. Of the pair, merge(word, 1, Integer::sum) is right: it
//  puts 1 for a new key and adds 1 to an existing one. put(word, 1)
//  overwrites, so every count would be 1 ("the" three times → 1).
//  Why the isEmpty skip: "  The cat".split("[^a-z]+") is ["", "the",
//  "cat"]. split drops TRAILING empty strings but keeps a LEADING one, so
//  text that starts with a separator yields an empty "word" first.
//  Lower-casing with Locale.ROOT keeps "I" → "i" on every machine (the
//  Turkish locale maps it to a dotless i, which [a-z] would not match).
//  TreeMap gives the alphabetical key order for free; a HashMap would
//  pass every test except the ordering one.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static Map<String, Integer> wordCounts(String text) {
        var counts = new TreeMap<String, Integer>();
        for (var word : text.toLowerCase(Locale.ROOT).split("[^a-z]+")) {
            if (word.isEmpty()) continue;
            counts.merge(word, 1, Integer::sum);
        }
        return counts;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("counts each word once per appearance", () -> {
            eq(wordCounts("the cat the hat the end"), Map.of("the", 3, "cat", 1, "hat", 1, "end", 1));
        });
        test("is case-insensitive", () -> {
            eq(wordCounts("Go GO go"), Map.of("go", 3));
        });
        test("punctuation and leading spaces separate words and are not words", () -> {
            eq(wordCounts("  The cat, the hat."), Map.of("the", 2, "cat", 1, "hat", 1));
            eq(wordCounts("...!"), Map.of());
        });
        test("empty text has no words", () -> {
            eq(wordCounts(""), Map.of());
        });
        test("keys come out in alphabetical order", () -> {
            eq(new ArrayList<>(wordCounts("pear apple fig apple").keySet()), List.of("apple", "fig", "pear"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
