# 06 · Exceptions and Files

A beginner CLI program works until the first unexpected input or the
first full disk. Then it either crashes with a stack trace nobody can
read, or — worse — swallows the error and carries on with corrupted
data. This module is about failing *well*: precise exceptions with
context, resources that always get closed, files that are never left
half-written, and batch imports that report every problem at once.

## The mental model

### 1 · Two kinds of exceptions, two kinds of problems

| | checked (`Exception`) | unchecked (`RuntimeException`) |
| --- | --- | --- |
| compiler | must catch or declare `throws` | no requirement |
| means | a correct program must still expect this | a bug, or invalid input |
| examples | `IOException`, a declined payment | `IllegalArgumentException`, `IllegalStateException`, NPE |

Wrap with a cause so nothing is lost:

```java
catch (NumberFormatException e) {
    throw new IllegalArgumentException("line 7: bad year '" + raw + "'", e);
}
```

### 2 · try-with-resources closes everything, in reverse, always

```java
try (var in = Files.newBufferedReader(src, UTF_8);
     var out = Files.newBufferedWriter(dst, UTF_8)) {
    ...
}   // out.close(), then in.close() — even on exception;
    // a close() failure is attached to the body's exception as "suppressed"
```

### 3 · `java.nio.file`: `Path` names it, `Files` does it

```java
Files.readString(p, UTF_8)        Files.writeString(p, text, UTF_8)
Files.readAllLines(p, UTF_8)      Files.createDirectories(dir)
Files.walk(root) / Files.list(dir) — Streams: CLOSE them
Files.move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING)
```

### 4 · Write-then-rename

Write the new content to a temp file in the same folder, then atomically
move it over the target. Readers see the old file or the new one — never
half of one.

## The details that bite

1. **Always pass a charset.** `FileReader`, `FileWriter`,
   `new String(bytes)` use the platform default on Java 17 —
   windows-1252 on many Windows machines.
2. **`Files.write(path, lines)` ends lines with `System.lineSeparator()`**:
   `\r\n` on Windows. Build the text yourself if the bytes matter.
3. **`Path.toString()` uses `\` on Windows.** Join name elements with
   `/` for anything you store or compare.
4. **`Files.walk`, `Files.list`, `Files.lines` hold OS handles.** Use
   them in try-with-resources.
5. **Never `catch (Exception e) {}`.** At minimum rethrow with context;
   `catch (Exception e)` also swallows your bugs.
6. **Throwing a new exception without the cause** deletes the root cause
   from every log.
7. **A hand-written `finally { a.close(); b.close(); }`** leaks `b` when
   `a.close()` throws, and replaces the real exception.
8. **Fail-fast is for bugs; batch input wants a report** of every bad
   line with its number.

## Exercises

| # | file | ★ | what you build |
| --- | --- | --- | --- |
| 01 | `01-parse-or-explain.java` | ★☆☆ | parsing with messages that say what was wrong, cause kept |
| 02 | `02-checked-exception.java` | ★★☆ | a declined withdrawal as a checked exception with data |
| 03 | `03-try-with-resources.java` | ★★☆ | close order and suppressed exceptions, proven by a log |
| 04 | `04-task-file.java` | ★☆☆ | save/load a to-do file: UTF-8, `\n`, missing file = empty |
| 05 | `05-csv-store.java` | ★★☆ | a CSV catalogue with a header and line-numbered errors |
| 06 | `06-atomic-save.java` | ★★★ | write-then-rename: never half a ledger, no temp litter |
| 07 | `07-walk-the-tree.java` | ★★☆ | find exports recursively with portable relative paths |
| 08 | `08-import-report.java` | ★★★ | import what's valid, report every bad line |

Do the warm-ups and core in order. Stretch if time allows. The file
exercises create a temporary folder per test and delete it afterwards.

---

**Next:** `bootcamp/07-money-and-validation`
