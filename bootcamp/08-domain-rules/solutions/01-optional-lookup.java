// ─────────────────────────────────────────────────────────────────────────
//  01 · maybe a book — SOLUTION                             ★☆☆ warm-up
//  concepts: Optional · map · orElse · ofNullable · flatMap
//  run: java -cp ../../_lib/out 01-optional-lookup.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Optional<T> is a return type that says "there might be no answer" in
//  the signature, so the caller cannot forget the case the way they forget
//  to null-check. findByIsbn is stream().filter(...).findFirst(), which
//  already returns an Optional.
//  Then you stay inside the Optional instead of unwrapping it: map
//  transforms a present value (Book → title) and passes empty through;
//  orElse supplies the fallback. titleOrUnknown is one expression with no
//  if and no null in sight.
//  borrowerOf meets a nullable FIELD inside an optional value. map(Book::
//  borrower) would produce Optional<String> holding... nothing — map
//  treats a null result as empty, which works, but says less than it
//  should. The explicit tool is flatMap(b -> Optional.ofNullable(
//  b.borrower())): ofNullable is the bridge from "maybe null" to
//  Optional, and flatMap avoids an Optional<Optional<String>>.
//  The trap the tests check: Optional.of(b.borrower()) throws
//  NullPointerException for a book on the shelf. Use of only for values
//  you know are non-null.
//  Optional rules of thumb: return it, don't take it as a parameter or
//  store it in a field; never call get() without knowing it's present
//  (orElseThrow says the same thing honestly); never return null from a
//  method declared to return Optional.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Book(String isbn, String title, String borrower) {}

    static Optional<Book> findByIsbn(List<Book> books, String isbn) {
        return books.stream().filter(b -> b.isbn().equals(isbn)).findFirst();
    }

    static String titleOrUnknown(List<Book> books, String isbn) {
        return findByIsbn(books, isbn).map(Book::title).orElse("unknown");
    }

    static Optional<String> borrowerOf(List<Book> books, String isbn) {
        return findByIsbn(books, isbn).flatMap(b -> Optional.ofNullable(b.borrower()));
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
