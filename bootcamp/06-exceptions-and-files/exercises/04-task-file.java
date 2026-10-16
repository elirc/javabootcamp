// ─────────────────────────────────────────────────────────────────────────
//  04 · save the to-do list                                 ★☆☆ warm-up
//  concepts: java.nio.file.Files · Path · UTF-8 · missing files
//  run: java -cp ../../_lib/out 04-task-file.java
// ─────────────────────────────────────────────────────────────────────────
//
//  The to-do manager keeps tasks in a text file, one per line. Write
//  saveTasks (UTF-8, every line ending in "\n" on every OS, replacing
//  any previous content, creating missing parent directories) and
//  loadTasks (trimmed lines, blank lines skipped; a file that does not
//  exist yet means no tasks, not an error).
//
//      saveTasks(dir.resolve("todo.txt"), List.of("milk", "eggs"))
//      loadTasks(dir.resolve("todo.txt"))          → [milk, eggs]
//      loadTasks(dir.resolve("never-saved.txt"))   → []
//
//  hint: Files.write(path, lines) ends lines the way the OS does.

import static bootcamp.Check.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

class Exercise {

    static void saveTasks(Path file, List<String> tasks) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    static List<String> loadTasks(Path file) throws IOException {
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
