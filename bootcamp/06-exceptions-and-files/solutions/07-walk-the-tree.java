// ─────────────────────────────────────────────────────────────────────────
//  07 · find every export — SOLUTION                        ★★☆ core
//  concepts: Files.walk · closing streams · relativize · portable paths
//  run: java -cp ../../_lib/out 07-walk-the-tree.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Files.walk(root) returns a lazy Stream<Path> over the whole tree, root
//  included. That stream holds open directory handles, so it MUST be
//  closed: try (var paths = Files.walk(root)) { ... }. Forget it and a
//  long-running program leaks file handles; on Windows, an unclosed walk
//  can even stop you deleting the folder. (Same for Files.list and
//  Files.lines — every Files method returning a Stream.)
//  Filter with Files::isRegularFile to drop directories (and the root).
//  For the result, root.relativize(path) gives "2024/jan.csv"... or
//  "2024\jan.csv" on Windows, because Path.toString() uses the platform
//  separator. A path you store, compare or show in a report should not
//  depend on the OS: join the name elements with "/" yourself —
//  for (Path part : relative) gives each name — or replace
//  File.separatorChar. The test expects "/" everywhere, so a solution that
//  "works on my Mac" fails on Windows, which is exactly the bug class.
//  Extension matching: lowercase the file NAME with Locale.ROOT and check
//  endsWith("." + ext). Comparing whole names avoids "mycsv" matching.
//  totalSize: mapToLong over Files.size(p) — but Files.size throws a
//  checked IOException, which a lambda can't throw. Use a plain loop, or
//  wrap in UncheckedIOException and unwrap outside.

import static bootcamp.Check.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

class Exercise {

    static List<String> findByExtension(Path root, String ext) throws IOException {
        var suffix = "." + ext.toLowerCase(Locale.ROOT);
        var found = new ArrayList<String>();
        try (var paths = Files.walk(root)) {
            for (var p : paths.filter(Files::isRegularFile).toList()) {
                if (p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(suffix)) {
                    var parts = new ArrayList<String>();
                    for (var name : root.relativize(p)) parts.add(name.toString());
                    found.add(String.join("/", parts));
                }
            }
        }
        Collections.sort(found);
        return found;
    }

    static long totalSize(Path root) throws IOException {
        long total = 0;
        try (var paths = Files.walk(root)) {
            for (var p : paths.filter(Files::isRegularFile).toList()) total += Files.size(p);
        }
        return total;
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
