// ─────────────────────────────────────────────────────────────────────────
//  04 · save the to-do list — SOLUTION                      ★☆☆ warm-up
//  concepts: java.nio.file.Files · Path · UTF-8 · missing files
//  run: java -cp ../../_lib/out 04-task-file.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  java.nio.file is the modern file API: a Path names a file, and the
//  static methods on Files do the work — readAllLines, readString, write,
//  writeString, exists, createDirectories, newBufferedReader. Prefer it to
//  the old java.io.File, whose methods return false instead of telling you
//  WHY something failed.
//  Always say the charset. Files.readAllLines/write default to UTF-8, but
//  FileReader/FileWriter and new String(bytes) on Java 17 use the platform
//  default — windows-1252 on many Windows machines — and "café" comes back
//  as "cafÃ©". Passing StandardCharsets.UTF_8 everywhere makes the file
//  format a decision instead of an accident.
//  Files.write(path, lines, UTF_8) looks perfect and has one catch: it
//  ends each line with System.lineSeparator() — "\r\n" on Windows — so
//  the same program writes different bytes on different machines. Build
//  the text yourself (String.join("\n", tasks) + "\n") and writeString it.
//  Both create or truncate the file (default options CREATE,
//  TRUNCATE_EXISTING, WRITE), so a second save replaces the first. Neither
//  creates parent directories — that is Files.createDirectories(parent),
//  which is a no-op when they already exist.
//  A missing file on load is an expected state on first run, so check
//  Files.exists and return an empty list; any OTHER IOException (no
//  permission, a directory with that name) is a real error and should
//  propagate — which is why both methods declare `throws IOException`.

import static bootcamp.Check.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

class Exercise {

    static void saveTasks(Path file, List<String> tasks) throws IOException {
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        var text = tasks.isEmpty() ? "" : String.join("\n", tasks) + "\n";
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    static List<String> loadTasks(Path file) throws IOException {
        if (!Files.exists(file)) return List.of();
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream()
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .toList();
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

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("saves and loads tasks", () -> inTempDir(dir -> {
            var file = dir.resolve("todo.txt");
            saveTasks(file, List.of("milk", "eggs"));
            eq(loadTasks(file), List.of("milk", "eggs"));
        }));
        test("a second save replaces the first", () -> inTempDir(dir -> {
            var file = dir.resolve("todo.txt");
            saveTasks(file, List.of("a", "b", "c"));
            saveTasks(file, List.of("d"));
            eq(loadTasks(file), List.of("d"));
        }));
        test("ends every line with a newline on every OS", () -> inTempDir(dir -> {
            var file = dir.resolve("todo.txt");
            saveTasks(file, List.of("milk", "eggs"));
            eq(Files.readString(file, StandardCharsets.UTF_8), "milk\neggs\n");
        }));
        test("a missing file means no tasks", () -> inTempDir(dir ->
            eq(loadTasks(dir.resolve("never-saved.txt")), List.of())));
        test("skips blank lines and trims", () -> inTempDir(dir -> {
            var file = dir.resolve("todo.txt");
            Files.writeString(file, "  milk \n\n   \neggs\n", StandardCharsets.UTF_8);
            eq(loadTasks(file), List.of("milk", "eggs"));
        }));
        test("keeps non-ASCII text intact", () -> inTempDir(dir -> {
            var file = dir.resolve("todo.txt");
            saveTasks(file, List.of("caf\u00e9", "\u20ac5 for na\u00efve tea"));
            eq(loadTasks(file), List.of("caf\u00e9", "\u20ac5 for na\u00efve tea"));
            eq(Files.readAllBytes(file).length, 26);
        }));
        test("creates missing parent folders", () -> inTempDir(dir -> {
            var file = dir.resolve("data").resolve("lists").resolve("todo.txt");
            saveTasks(file, List.of("milk"));
            eq(loadTasks(file), List.of("milk"));
        }));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
