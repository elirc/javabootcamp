// ─────────────────────────────────────────────────────────────────────────
//  05 · a catalogue on disk — SOLUTION                      ★★☆ core
//  concepts: buffered I/O · header lines · split limit · line numbers
//  run: java -cp ../../_lib/out 05-csv-store.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Writing: Files.newBufferedWriter(file, UTF_8) in try-with-resources,
//  write the header, then one line per book, each ending in "\n". The
//  buffered writer batches small writes into big ones; try-with-resources
//  flushes and closes it even when something throws halfway.
//  Reading: Files.newBufferedReader and readLine() in a loop, counting
//  lines as you go — that counter is what makes an error message useful.
//  "bad year" sends someone hunting through a 10,000-line file;
//  "line 7312: bad year 'soon'" sends them straight there.
//  The field-order trick: put the free-text field last and split with a
//  LIMIT: line.split(",", 3) produces at most three parts, so every comma
//  after the second stays inside the title. That is how many real formats
//  (HTTP headers, key=value configs) dodge quoting. Full CSV, with quoted
//  fields and escaped quotes, needs a real parser — don't hand-roll one
//  for production data.
//  Wrap the NumberFormatException with the line number and keep it as the
//  cause. Throwing IllegalArgumentException (unchecked) for bad CONTENT
//  while IOException (checked) covers bad I/O keeps the two failure modes
//  distinguishable for the caller.

import static bootcamp.Check.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

class Exercise {

    record Book(String isbn, int year, String title) {}

    static final String HEADER = "isbn,year,title";

    static void save(Path file, List<Book> books) throws IOException {
        try (var out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write(HEADER + "\n");
            for (var b : books) out.write(b.isbn() + "," + b.year() + "," + b.title() + "\n");
        }
    }

    static List<Book> load(Path file) throws IOException {
        var books = new ArrayList<Book>();
        try (var in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            var header = in.readLine();
            if (!HEADER.equals(header)) throw new IllegalArgumentException("line 1: expected header " + HEADER);
            int lineNo = 1;
            for (String line; (line = in.readLine()) != null; ) {
                lineNo++;
                if (line.isBlank()) continue;
                var parts = line.split(",", 3);
                if (parts.length != 3) throw new IllegalArgumentException("line " + lineNo + ": expected isbn,year,title");
                try {
                    books.add(new Book(parts[0], Integer.parseInt(parts[1]), parts[2]));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("line " + lineNo + ": bad year '" + parts[1] + "'", e);
                }
            }
        }
        return books;
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
