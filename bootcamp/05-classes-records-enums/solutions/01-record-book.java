// ─────────────────────────────────────────────────────────────────────────
//  01 · a record that refuses nonsense — SOLUTION           ★☆☆ warm-up
//  concepts: records · compact constructors · validation · withers
//  run: java -cp ../../_lib/out 01-record-book.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A record declares its data once — `record Book(String isbn, String
//  title, int year)` — and gets the constructor, private final fields,
//  accessors (title(), not getTitle()), equals, hashCode and toString for
//  free. What it does NOT get for free is validity; that is the compact
//  constructor's job.
//  The compact form `Book { ... }` has no parameter list: it runs BEFORE
//  the fields are assigned, with the parameters in scope, and the fields
//  are assigned from the parameters automatically at the end. So to
//  normalise you reassign the parameter — `title = title.strip();` — and
//  the field gets the cleaned value. Writing this.title = ... is a
//  compile error there.
//  Validate in the constructor and the invariant holds for EVERY Book in
//  the program, including ones built by withTitle, deserialisers and
//  tests. That is the payoff: no "isValid()" method to forget to call.
//  Records are immutable (shallowly), so "changing" one means building a
//  new one: a wither, withTitle, calls the canonical constructor, and so
//  is validated for free.
//  Note the check order: null and blank before strip(), or strip() throws
//  NullPointerException with a useless message.

import static bootcamp.Check.*;

class Exercise {

    record Book(String isbn, String title, int year) {
        Book {
            if (isbn == null || isbn.isBlank()) throw new IllegalArgumentException("isbn is required");
            if (title == null || title.isBlank()) throw new IllegalArgumentException("title is required");
            if (year < 1450 || year > 2100) throw new IllegalArgumentException("implausible year: " + year);
            title = title.strip();
        }

        Book withTitle(String newTitle) {
            return new Book(isbn, newTitle, year);
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
