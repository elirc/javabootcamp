// ─────────────────────────────────────────────────────────────────────────
//  01 · maybe a book                                        ★☆☆ warm-up
//  concepts: Optional · map · orElse · ofNullable · flatMap
//  run: java -cp ../../_lib/out 01-optional-lookup.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A library tracker looks books up by ISBN. A book's borrower is null
//  while it sits on the shelf. Write findByIsbn (the book, if any),
//  titleOrUnknown (its title, or "unknown") and borrowerOf (who has it —
//  empty when the book is missing OR on the shelf). The record Book is
//  above the tests.
//
//      findByIsbn(books, "missing")      → Optional.empty
//      titleOrUnknown(books, "missing")  → "unknown"
//      borrowerOf(books, "on-shelf")     → Optional.empty
//      borrowerOf(books, "lent-out")     → Optional[Ada]
//
//  hint: Optional.of(null) throws; there is a method for maybe-null values.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Book(String isbn, String title, String borrower) {}

    static Optional<Book> findByIsbn(List<Book> books, String isbn) {
        throw new UnsupportedOperationException("TODO");
    }

    static String titleOrUnknown(List<Book> books, String isbn) {
        throw new UnsupportedOperationException("TODO");
    }

    static Optional<String> borrowerOf(List<Book> books, String isbn) {
        throw new UnsupportedOperationException("TODO");
    }

    static final List<Book> SHELF = List.of(
        new Book("on-shelf", "Dune", null),
        new Book("lent-out", "Emma", "Ada"));

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("finds a book that exists", () -> eq(findByIsbn(SHELF, "lent-out"), Optional.of(SHELF.get(1))));
        test("finds nothing for an unknown isbn", () -> eq(findByIsbn(SHELF, "missing"), Optional.empty()));
        test("titleOrUnknown gives the title", () -> eq(titleOrUnknown(SHELF, "on-shelf"), "Dune"));
        test("titleOrUnknown falls back", () -> eq(titleOrUnknown(SHELF, "missing"), "unknown"));
        test("a lent-out book has a borrower", () -> eq(borrowerOf(SHELF, "lent-out"), Optional.of("Ada")));
        test("a book on the shelf has no borrower", () -> eq(borrowerOf(SHELF, "on-shelf"), Optional.empty()));
        test("a missing book has no borrower", () -> eq(borrowerOf(SHELF, "missing"), Optional.empty()));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
