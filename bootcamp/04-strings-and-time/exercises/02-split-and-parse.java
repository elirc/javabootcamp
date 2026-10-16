// ─────────────────────────────────────────────────────────────────────────
//  02 · one line, one book                                  ★★☆ core
//  concepts: String.split · regex metacharacters · parseInt · round-trips
//  run: java -cp ../../_lib/out 02-split-and-parse.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A library tracker saves books as "title|author|year" lines. Write
//  parseLine: exactly three fields, each trimmed; a blank title or author,
//  a year that is not a whole number, or the wrong number of fields throws
//  IllegalArgumentException (for a bad year, the message mentions "year").
//  Then write toLine, the inverse — refusing any field that contains "|".
//  The record Book is defined above the code you edit.
//
//      parseLine("Dune | Frank Herbert | 1965")
//          → Book[title=Dune, author=Frank Herbert, year=1965]
//      parseLine("Dune|Herbert")         → IllegalArgumentException
//      toLine(new Book("Emma", "Austen", 1815)) → "Emma|Austen|1815"
//
//  hint: split takes a REGEX, and | means "or" in a regex.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Book(String title, String author, int year) {}

    static Book parseLine(String line) {
        throw new UnsupportedOperationException("TODO");
    }

    static String toLine(Book book) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("parses a well-formed line", () -> eq(parseLine("Dune|Herbert|1965"), new Book("Dune", "Herbert", 1965)));
        test("trims around the fields", () ->
            eq(parseLine(" Dune | Frank Herbert | 1965 "), new Book("Dune", "Frank Herbert", 1965)));
        test("refuses the wrong number of fields", () -> {
            raises(IllegalArgumentException.class, () -> parseLine("Dune|Herbert"));
            raises(IllegalArgumentException.class, () -> parseLine("Dune|Herbert|1965|extra"));
            raises(IllegalArgumentException.class, () -> parseLine("Dune|Herbert|"));
        });
        test("refuses a blank author", () -> raises(IllegalArgumentException.class, () -> parseLine("Dune| |1965")));
        test("says which field is wrong for a bad year", () ->
            raises(IllegalArgumentException.class, () -> parseLine("Dune|Herbert|soon"), "year"));
        test("toLine writes the fields in order", () -> eq(toLine(new Book("Emma", "Austen", 1815)), "Emma|Austen|1815"));
        test("toLine round-trips through parseLine", () -> {
            var book = new Book("The Left Hand of Darkness", "Le Guin", 1969);
            eq(parseLine(toLine(book)), book);
        });
        test("toLine refuses a field containing the separator", () ->
            raises(IllegalArgumentException.class, () -> toLine(new Book("A|B", "C", 2000))));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
