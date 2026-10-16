// ─────────────────────────────────────────────────────────────────────────
//  04 · the order of things                                 ★★☆ core
//  concepts: Comparator.comparing · thenComparing · nullsLast · case order
//  run: java -cp ../../_lib/out 04-comparators.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A library tracker lists books newest first. Write newestFirst: by year
//  descending, then by title; books with an unknown (null) year go last;
//  the input list is left untouched. Then write alphabetical: titles in
//  case-insensitive order, and for titles equal ignoring case, uppercase
//  first. The record Book is defined above the code you edit.
//
//      newestFirst([Emma 1815, Dune 1965, Notes null, Ubik 1969])
//          → [Ubik 1969, Dune 1965, Emma 1815, Notes null]
//      alphabetical(["apple", "Banana", "cherry"]) → [apple, Banana, cherry]
//      alphabetical(["b", "B"])                    → [B, b]
//
//  hint: Comparator.nullsLast wraps the comparator for the key, not the book.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Book(String title, Integer year) {}

    static List<Book> newestFirst(List<Book> books) {
        throw new UnsupportedOperationException("TODO");
    }

    static List<String> alphabetical(List<String> titles) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("newestFirst sorts by year, newest first", () -> eq(
            newestFirst(List.of(new Book("Emma", 1815), new Book("Dune", 1965), new Book("Ubik", 1969))),
            List.of(new Book("Ubik", 1969), new Book("Dune", 1965), new Book("Emma", 1815))));
        test("newestFirst breaks ties by title", () -> eq(
            newestFirst(List.of(new Book("Zed", 2000), new Book("Abe", 2000))),
            List.of(new Book("Abe", 2000), new Book("Zed", 2000))));
        test("unknown years go last", () -> eq(
            newestFirst(List.of(new Book("Notes", null), new Book("Emma", 1815), new Book("Aside", null))),
            List.of(new Book("Emma", 1815), new Book("Aside", null), new Book("Notes", null))));
        test("newestFirst leaves its input alone", () -> {
            var books = new ArrayList<>(List.of(new Book("Emma", 1815), new Book("Dune", 1965)));
            newestFirst(books);
            eq(books.get(0).title(), "Emma");
        });
        test("alphabetical ignores case", () ->
            eq(alphabetical(List.of("cherry", "Banana", "apple")), List.of("apple", "Banana", "cherry")));
        test("alphabetical puts uppercase first on a tie", () -> eq(alphabetical(List.of("b", "B", "a")), List.of("a", "B", "b")));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
