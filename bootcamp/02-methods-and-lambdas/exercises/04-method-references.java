// ─────────────────────────────────────────────────────────────────────────
//  04 · four kinds of ::                                    ★☆☆ warm-up
//  concepts: method references · Comparator · constructor refs · streams
//  run: java -cp ../../_lib/out 04-method-references.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A library tracker imports raw text. Write parseAll (strings to ints; a
//  bad number throws NumberFormatException), byLength (shortest first,
//  alphabetical among equal lengths, the input list left untouched) and
//  toBooks (one Book per title). The record Book is defined above the code
//  you edit. Use method references where they fit.
//
//      parseAll(List.of("1", "22"))                  → [1, 22]
//      byLength(List.of("ccc", "bb", "a", "aa"))     → [a, aa, bb, ccc]
//      toBooks(List.of("Dune"))                      → [Book[title=Dune]]
//
//  hint: List.of(...) is immutable — sorting it in place throws.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Book(String title) {}

    static List<Integer> parseAll(List<String> raw) {
        throw new UnsupportedOperationException("TODO");
    }

    static List<String> byLength(List<String> words) {
        throw new UnsupportedOperationException("TODO");
    }

    static List<Book> toBooks(List<String> titles) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("parseAll converts every string", () -> eq(parseAll(List.of("1", "22", "-3")), List.of(1, 22, -3)));
        test("parseAll of nothing is empty", () -> eq(parseAll(List.of()), List.of()));
        test("parseAll refuses a bad number", () ->
            raises(NumberFormatException.class, () -> parseAll(List.of("1", "two"))));
        test("byLength sorts shortest first, then alphabetically", () ->
            eq(byLength(List.of("ccc", "bb", "a", "aa")), List.of("a", "aa", "bb", "ccc")));
        test("byLength leaves a mutable input alone", () -> {
            var words = new ArrayList<>(List.of("bb", "a"));
            byLength(words);
            eq(words, List.of("bb", "a"));
        });
        test("toBooks makes one book per title", () ->
            eq(toBooks(List.of("Dune", "Emma")), List.of(new Book("Dune"), new Book("Emma"))));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
