// ─────────────────────────────────────────────────────────────────────────
//  04 · four kinds of :: — SOLUTION                         ★☆☆ warm-up
//  concepts: method references · Comparator · constructor refs · streams
//  run: java -cp ../../_lib/out 04-method-references.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A method reference is a lambda whose body is one call, written by name.
//  There are four shapes: static (Integer::parseInt, x -> parseInt(x)),
//  bound instance (System.out::println, x -> out.println(x)), unbound
//  instance (String::length, s -> s.length() — the first argument becomes
//  the receiver), and constructor (Book::new, t -> new Book(t)).
//  parseAll is map(Integer::parseInt).toList(). Stream.toList() (Java 16+)
//  returns an unmodifiable list, which is what you want to hand back.
//  byLength needs a two-level ordering: Comparator.comparingInt(String::
//  length).thenComparing(Comparator.naturalOrder()). Sorting must happen
//  on a COPY — `words.sort(...)` on a List.of throws
//  UnsupportedOperationException, and on a mutable list it silently
//  reorders the caller's data. stream().sorted(...) copies for you.
//  comparingInt (not comparing) avoids boxing every length into Integer.
//  toBooks is map(Book::new): the compiler sees a Function<String, Book>
//  is wanted and finds the one-argument constructor.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Book(String title) {}

    static List<Integer> parseAll(List<String> raw) {
        return raw.stream().map(Integer::parseInt).toList();
    }

    static List<String> byLength(List<String> words) {
        return words.stream()
            .sorted(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()))
            .toList();
    }

    static List<Book> toBooks(List<String> titles) {
        return titles.stream().map(Book::new).toList();
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
