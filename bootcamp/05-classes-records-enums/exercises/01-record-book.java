// ─────────────────────────────────────────────────────────────────────────
//  01 · a record that refuses nonsense                      ★☆☆ warm-up
//  concepts: records · compact constructors · validation · withers
//  run: java -cp ../../_lib/out 01-record-book.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A library tracker's Book is a record. Finish it (the nested record
//  Book, above the tests): its compact constructor trims the title and
//  refuses, with IllegalArgumentException, a null or blank isbn or title
//  and a year outside 1450..2100. withTitle returns a COPY with a new
//  title — validated like any other book.
//
//      new Book("9780441172719", " Dune ", 1965).title()  → "Dune"
//      new Book("9780441172719", "  ", 1965)    → IllegalArgumentException
//      dune.withTitle("Dune Messiah")           → a new Book; dune unchanged
//
//  hint: in a compact constructor you assign the PARAMETER, not this.title.

import static bootcamp.Check.*;

class Exercise {

    record Book(String isbn, String title, int year) {
        Book {
            throw new UnsupportedOperationException("TODO");
        }

        Book withTitle(String newTitle) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final String ISBN = "9780441172719";

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("builds a valid book", () -> {
            var dune = new Book(ISBN, "Dune", 1965);
            eq(dune.isbn(), ISBN);
            eq(dune.year(), 1965);
        });
        test("trims the title", () -> eq(new Book(ISBN, " Dune ", 1965).title(), "Dune"));
        test("refuses a missing or blank title", () -> {
            raises(IllegalArgumentException.class, () -> new Book(ISBN, "  ", 1965));
            raises(IllegalArgumentException.class, () -> new Book(ISBN, null, 1965));
        });
        test("refuses a missing or blank isbn", () -> {
            raises(IllegalArgumentException.class, () -> new Book("", "Dune", 1965));
            raises(IllegalArgumentException.class, () -> new Book(null, "Dune", 1965));
        });
        test("refuses an implausible year", () -> {
            raises(IllegalArgumentException.class, () -> new Book(ISBN, "Dune", 1200));
            raises(IllegalArgumentException.class, () -> new Book(ISBN, "Dune", 3000));
        });
        test("equal books are equal and hash alike", () -> {
            var a = new Book(ISBN, "Dune", 1965);
            var b = new Book(ISBN, " Dune", 1965);
            eq(a, b);
            eq(a.hashCode(), b.hashCode());
        });
        test("withTitle returns a changed copy", () -> {
            var dune = new Book(ISBN, "Dune", 1965);
            var messiah = dune.withTitle("Dune Messiah");
            eq(messiah.title(), "Dune Messiah");
            eq(dune.title(), "Dune");
        });
        test("withTitle validates too", () ->
            raises(IllegalArgumentException.class, () -> new Book(ISBN, "Dune", 1965).withTitle(" ")));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
