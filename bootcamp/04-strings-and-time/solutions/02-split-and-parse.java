// ─────────────────────────────────────────────────────────────────────────
//  02 · one line, one book — SOLUTION                       ★★☆ core
//  concepts: String.split · regex metacharacters · parseInt · round-trips
//  run: java -cp ../../_lib/out 02-split-and-parse.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  String.split's argument is a regular expression, and "|" is the regex
//  for "empty OR empty" — it matches between every character, so
//  "a|b".split("|") is [a, |, b]. Escape it: split("\\|"), or quote it
//  with Pattern.quote("|"). The same goes for ".", "+", "*", "$", "?".
//  split also silently DROPS trailing empty strings: "Dune|Herbert|"
//  .split("\\|") has two parts, not three. Passing a negative limit,
//  split("\\|", -1), keeps them, so a missing year shows up as a blank
//  field you can report precisely instead of a vague "wrong count".
//  Integer.parseInt throws NumberFormatException — which IS an
//  IllegalArgumentException subclass, but its message ("For input string:
//  ...") says nothing about which field was wrong. Catch it and rethrow
//  with context, passing the original as the cause so the stack trace
//  keeps the detail.
//  toLine must refuse a "|" inside a field: writing it would produce a
//  line that parses back as four fields. Any format you write, you must
//  be able to read back — the round-trip test pins that down.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Book(String title, String author, int year) {}

    static Book parseLine(String line) {
        var parts = line.split("\\|", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("expected 3 fields, got " + parts.length + ": " + line);
        }
        var title = parts[0].trim();
        var author = parts[1].trim();
        if (title.isEmpty() || author.isEmpty()) throw new IllegalArgumentException("blank title or author: " + line);
        try {
            return new Book(title, author, Integer.parseInt(parts[2].trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bad year '" + parts[2].trim() + "' in: " + line, e);
        }
    }

    static String toLine(Book book) {
        for (var field : List.of(book.title(), book.author())) {
            if (field.contains("|")) throw new IllegalArgumentException("a field may not contain '|': " + field);
        }
        return book.title() + "|" + book.author() + "|" + book.year();
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
