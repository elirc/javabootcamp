// ─────────────────────────────────────────────────────────────────────────
//  04 · the order of things — SOLUTION                      ★★☆ core
//  concepts: Comparator.comparing · thenComparing · nullsLast · case order
//  run: java -cp ../../_lib/out 04-comparators.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Comparator.comparing(keyExtractor, keyComparator) sorts by a key with a
//  custom order for that key. The year key needs two adjustments: reverse
//  order (newest first), and nulls at the end. nullsLast(reverseOrder())
//  does both: it handles nulls itself and delegates the rest. The classic
//  wrong turn is comparing(Book::year).reversed() — it throws
//  NullPointerException the moment it meets a null year, and reversed()
//  applied to the WHOLE chain would also reverse the tie-break.
//  thenComparing(Book::title) breaks ties. Each then... only runs when
//  everything before it said "equal".
//  Strings' natural order is UTF-16 code-unit order: every uppercase
//  letter sorts before every lowercase one, so "Banana" < "apple".
//  String.CASE_INSENSITIVE_ORDER fixes that, and thenComparing(natural)
//  makes the result deterministic for "b" vs "B" — without it, they
//  compare equal and keep input order (sort is stable), which a test can
//  pin down but a reader cannot guess. For human-facing sorting in a real
//  language, java.text.Collator is the grown-up tool.
//  Sort a copy: stream().sorted(...).toList(), or new ArrayList<>(books)
//  then sort.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Book(String title, Integer year) {}

    static List<Book> newestFirst(List<Book> books) {
        return books.stream()
            .sorted(Comparator.comparing(Book::year, Comparator.nullsLast(Comparator.<Integer>reverseOrder()))
                .thenComparing(Book::title))
            .toList();
    }

    static List<String> alphabetical(List<String> titles) {
        return titles.stream()
            .sorted(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()))
            .toList();
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
