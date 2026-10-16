// ─────────────────────────────────────────────────────────────────────────
//  02 · count the words                                     ★☆☆ warm-up
//  concepts: HashMap vs TreeMap · Map.merge · String.split · sorting entries
//  run: java -cp ../../_lib/out 02-word-counts.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A quiz engine checks which words its questions overuse. Write
//  wordCounts: lowercase words (runs of a-z and 0-9) to how often they
//  appear, as a map whose keys iterate alphabetically. Then write
//  topWords: the n most frequent words, ties broken alphabetically.
//
//      wordCounts("The cat; the HAT.")      → {cat=1, hat=1, the=2}
//      wordCounts("")                       → {}
//      topWords("b a b c a b", 2)           → [b, a]
//
//  hint: split on a leading delimiter gives you an empty first token.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static Map<String, Integer> wordCounts(String text) {
        throw new UnsupportedOperationException("TODO");
    }

    static List<String> topWords(String text, int n) {
        throw new UnsupportedOperationException("TODO");
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
