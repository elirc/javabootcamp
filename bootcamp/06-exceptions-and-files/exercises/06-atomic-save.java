// ─────────────────────────────────────────────────────────────────────────
//  06 · never half a file                                   ★★★ stretch
//  concepts: atomic move · temp files · cleanup on failure · Files.move
//  run: java -cp ../../_lib/out 06-atomic-save.java
// ─────────────────────────────────────────────────────────────────────────
//
//  The bank simulator saves its ledger. If the program dies mid-save, the
//  OLD ledger must survive intact — never a half-written file. Write save:
//  let the writer produce the content into a temporary file in the same
//  folder, then move it over the target in one atomic step. If the writer
//  throws, its exception propagates, the target is exactly as it was (or
//  still absent), and no temporary file is left behind.
//
//      save(ledger, w -> w.write("balance=100\n"))   → ledger holds it
//      save(ledger, w -> { w.write("bal"); throw new IOException("disk"); })
//          → IOException "disk"; ledger unchanged; folder holds only ledger
//
//  hint: Files.move with StandardCopyOption.ATOMIC_MOVE.

import static bootcamp.Check.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

class Exercise {

    interface Content {
        void writeTo(BufferedWriter out) throws IOException;
    }

    static void save(Path target, Content content) throws IOException {
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

    static List<String> names(Path dir) throws IOException {
        try (var s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("creates the file", () -> inTempDir(dir -> {
            var ledger = dir.resolve("ledger.txt");
            save(ledger, w -> w.write("balance=100\n"));
            eq(Files.readString(ledger, StandardCharsets.UTF_8), "balance=100\n");
        }));
        test("replaces an existing file", () -> inTempDir(dir -> {
            var ledger = dir.resolve("ledger.txt");
            Files.writeString(ledger, "balance=1\n", StandardCharsets.UTF_8);
            save(ledger, w -> w.write("balance=2\n"));
            eq(Files.readString(ledger, StandardCharsets.UTF_8), "balance=2\n");
        }));
        test("leaves no temporary files after a save", () -> inTempDir(dir -> {
            save(dir.resolve("ledger.txt"), w -> w.write("x"));
            eq(names(dir), List.of("ledger.txt"));
        }));
        test("a failed save passes the error on", () -> inTempDir(dir ->
            raises(IOException.class, () -> save(dir.resolve("ledger.txt"), w -> {
                w.write("bal");
                throw new IOException("disk full");
            }), "disk full")));
        test("a failed save keeps the old content", () -> inTempDir(dir -> {
            var ledger = dir.resolve("ledger.txt");
            Files.writeString(ledger, "balance=100\n", StandardCharsets.UTF_8);
            raises(IOException.class, () -> save(ledger, w -> {
                w.write("bal");
                throw new IOException("disk full");
            }));
            eq(Files.readString(ledger, StandardCharsets.UTF_8), "balance=100\n");
            eq(names(dir), List.of("ledger.txt"));
        }));
        test("a failed first save creates nothing", () -> inTempDir(dir -> {
            raises(IllegalStateException.class, () -> save(dir.resolve("ledger.txt"), w -> {
                w.write("bal");
                throw new IllegalStateException("bug in the writer");
            }));
            eq(names(dir), List.of());
        }));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
