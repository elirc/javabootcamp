// ─────────────────────────────────────────────────────────────────────────
//  07 · find every export                                   ★★☆ core
//  concepts: Files.walk · closing streams · relativize · portable paths
//  run: java -cp ../../_lib/out 07-walk-the-tree.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An expense tracker scatters CSV exports through nested folders. Write
//  findByExtension: every regular file under root (at any depth) whose
//  extension matches (case-insensitively), as paths RELATIVE to root with
//  "/" separators on every OS, sorted. Then write totalSize: the bytes in
//  all regular files under root (folders themselves count for nothing).
//
//      root/2024/jan.csv, root/2024/notes.txt, root/feb.CSV
//      findByExtension(root, "csv")   → ["2024/jan.csv", "feb.CSV"]
//      totalSize(empty folder)        → 0
//
//  hint: Path.toString() uses the OS separator — "\" on Windows.

import static bootcamp.Check.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

class Exercise {

    static List<String> findByExtension(Path root, String ext) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    static long totalSize(Path root) throws IOException {
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

    static void file(Path root, String relative, String text) throws IOException {
        var p = root.resolve(relative);
        Files.createDirectories(p.getParent());
        Files.writeString(p, text, StandardCharsets.UTF_8);
    }

    static void sampleTree(Path root) throws IOException {
        file(root, "2024/jan.csv", "a,b\n");
        file(root, "2024/notes.txt", "hello");
        file(root, "2024/q1/mar.csv", "x\n");
        file(root, "feb.CSV", "1234567890");
        file(root, "mycsv", "no extension");
        Files.createDirectories(root.resolve("empty.csv"));
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("finds matching files at every depth, sorted", () -> inTempDir(dir -> {
            sampleTree(dir);
            eq(findByExtension(dir, "csv"), List.of("2024/jan.csv", "2024/q1/mar.csv", "feb.CSV"));
        }));
        test("matches the extension, not the name", () -> inTempDir(dir -> {
            sampleTree(dir);
            eq(findByExtension(dir, "txt"), List.of("2024/notes.txt"));
        }));
        test("uses / separators on every OS", () -> inTempDir(dir -> {
            file(dir, "a/b/c.csv", "");
            eq(findByExtension(dir, "CSV"), List.of("a/b/c.csv"));
        }));
        test("an empty folder has no matches", () -> inTempDir(dir -> eq(findByExtension(dir, "csv"), List.of())));
        test("totalSize adds up every file", () -> inTempDir(dir -> {
            sampleTree(dir);
            eq(totalSize(dir), 4 + 5 + 2 + 10 + 12);
        }));
        test("totalSize of an empty folder is zero", () -> inTempDir(dir -> eq(totalSize(dir), 0)));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
