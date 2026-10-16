// ─────────────────────────────────────────────────────────────────────────
//  06 · never half a file — SOLUTION                        ★★★ stretch
//  concepts: atomic move · temp files · cleanup on failure · Files.move
//  run: java -cp ../../_lib/out 06-atomic-save.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Writing straight into the target opens a window where the file is
//  truncated or half-written; a crash, a full disk or an exception in the
//  middle leaves garbage where good data used to be. The standard fix is
//  write-then-rename: produce the whole content in a temp file, then
//  rename it over the target. A rename within one file system is atomic —
//  any reader sees either the old file or the new one, never a mix.
//  Three details make it work:
//  * the temp file goes in the SAME directory (Files.createTempFile(dir,
//    ...)). A move across file systems is a copy plus delete, which is not
//    atomic and fails with ATOMIC_MOVE;
//  * Files.move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING) — atomic and
//    allowed to replace an existing file;
//  * the writer is closed (try-with-resources) BEFORE the move, so every
//    byte is flushed into the temp file first.
//  On failure, delete the temp file in a catch (or finally guarded by a
//  "moved" flag) and rethrow. deleteIfExists makes the cleanup itself
//  unable to fail on an already-missing file. A save that leaves .tmp
//  litter behind on every failure eventually fills the disk.
//  (For durability against power loss you would also fsync — FileChannel
//  .force(true) — before the move. Atomicity is the part that matters most
//  and the part people skip.)

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
        var dir = target.toAbsolutePath().getParent();
        var tmp = Files.createTempFile(dir, ".save-", ".tmp");
        try {
            try (var out = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                content.writeTo(out);
            }
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }
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
