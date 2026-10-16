// ─────────────────────────────────────────────────────────────────────────
//  05 · a catalogue on disk                                 ★★☆ core
//  concepts: buffered I/O · header lines · split limit · line numbers
//  run: java -cp ../../_lib/out 05-csv-store.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A library tracker stores its catalogue as CSV: a header line
//  "isbn,year,title", then one book per line. The title goes LAST so it
//  may contain commas. Write save and load. load refuses a missing or
//  wrong header and any bad line with IllegalArgumentException whose
//  message names the line number ("line 3: ...", counting the header as
//  line 1). Blank lines are skipped. The record Book is above the tests.
//
//      save(file, List.of(new Book("9780441172719", 1965, "Dune")))
//          → file text: "isbn,year,title\n9780441172719,1965,Dune\n"
//      load(file) with "9780,soon,Dune" on line 2
//          → IllegalArgumentException: line 2: ...
//
//  hint: split(",", 3) stops after the second comma.

import static bootcamp.Check.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

class Exercise {

    record Book(String isbn, int year, String title) {}

    static final String HEADER = "isbn,year,title";

    static void save(Path file, List<Book> books) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    static List<Book> load(Path file) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    interface DirTest {
        void run(Path dir) throws Exception;
    }

    static void inTempDir(DirTest body) throws Exception {
        var dir = Files.createTempDirectory("bootcamp-");
        try {
            body.run(dir);
        } finally {
            try (var paths = Files.walk(dir)) {
                for (var p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
            }
        }
    }

    static final Book DUNE = new Book("9780441172719", 1965, "Dune");
    static final Book COMMAS = new Book("9780060935467", 1960, "Kill a Mockingbird, To");

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("writes a header and one line per book", () -> inTempDir(dir -> {
            var file = dir.resolve("books.csv");
            save(file, List.of(DUNE));
            eq(Files.readString(file, StandardCharsets.UTF_8), "isbn,year,title\n9780441172719,1965,Dune\n");
        }));
        test("round-trips a catalogue", () -> inTempDir(dir -> {
            var file = dir.resolve("books.csv");
            save(file, List.of(DUNE, COMMAS));
            eq(load(file), List.of(DUNE, COMMAS));
        }));
        test("an empty catalogue is just a header", () -> inTempDir(dir -> {
            var file = dir.resolve("books.csv");
            save(file, List.of());
            eq(load(file), List.of());
        }));
        test("skips blank lines", () -> inTempDir(dir -> {
            var file = dir.resolve("books.csv");
            Files.writeString(file, "isbn,year,title\n\n9780441172719,1965,Dune\n\n", StandardCharsets.UTF_8);
            eq(load(file), List.of(DUNE));
        }));
        test("refuses a missing header", () -> inTempDir(dir -> {
            var file = dir.resolve("books.csv");
            Files.writeString(file, "9780441172719,1965,Dune\n", StandardCharsets.UTF_8);
            raises(IllegalArgumentException.class, () -> load(file), "line 1");
        }));
        test("names the line of a bad year", () -> inTempDir(dir -> {
            var file = dir.resolve("books.csv");
            Files.writeString(file, "isbn,year,title\n9780441172719,1965,Dune\n9780,soon,Emma\n", StandardCharsets.UTF_8);
            raises(IllegalArgumentException.class, () -> load(file), "line 3");
        }));
        test("names the line with too few fields", () -> inTempDir(dir -> {
            var file = dir.resolve("books.csv");
            Files.writeString(file, "isbn,year,title\n9780441172719\n", StandardCharsets.UTF_8);
            raises(IllegalArgumentException.class, () -> load(file), "line 2");
        }));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
