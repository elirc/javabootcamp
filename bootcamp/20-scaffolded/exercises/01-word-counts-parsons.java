// ─────────────────────────────────────────────────────────────────────────
//  01 · count words (Parsons)                               ★☆☆ warm-up
//  concepts: Parsons problem · Map.merge · regex split · TreeMap
//  run: java -cp ../../_lib/out 01-word-counts-parsons.java
// ─────────────────────────────────────────────────────────────────────────
//
//  PARSONS: every line you need is below, SCRAMBLED. Arrange them into the
//  body of wordCounts (indent as you like). The two lines marked (?) are a
//  pair: exactly ONE of them belongs; the other is a plausible mistake.
//  wordCounts(text) counts words case-insensitively; a word is a run of
//  letters a-z (after lower-casing), everything else separates words.
//  Keys come out in alphabetical order.
//
//      }
//      return counts;
//      counts.put(word, 1);                                     (?)
//      var counts = new TreeMap<String, Integer>();
//      if (word.isEmpty()) continue;
//      for (var word : text.toLowerCase(Locale.ROOT).split("[^a-z]+")) {
//      counts.merge(word, 1, Integer::sum);                     (?)
//
//      wordCounts("The cat, the hat.") → {cat=1, hat=1, the=2}
//
//  hint: what does split return for text that starts with a space?

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static Map<String, Integer> wordCounts(String text) {
        throw new UnsupportedOperationException("TODO");
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
