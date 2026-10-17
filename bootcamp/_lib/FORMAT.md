# Java bootcamp authoring format — the contract

Every module in `bootcamp/` follows this format exactly. It is optimized for
offline solo study: every exercise is a single self-contained file, runnable
with plain `java <file>.java`, readable top to bottom without opening
anything else.

## Why one file works at all

Since Java 11 the `java` launcher runs a **single source file** directly: it
compiles the file in memory and calls the `main` of its first class. No
build tool, no `pom.xml`, no project. The only thing an exercise needs from
outside is the test harness, which is compiled once:

```
javac --release 17 -encoding UTF-8 -d bootcamp/_lib/out bootcamp/_lib/check/bootcamp/*.java
```

and then put on the class path when you run a file (from its folder):

```
java -cp ../../_lib/out 01-count-vowels.java
```

That is the whole build system. No Maven, no Gradle, no JUnit, nothing
downloaded. (`java bootcamp/Verify.java` builds `_lib/out` for you, so
running Verify once is the other way to get started.)

> **Windows, Java 17:** the single-file launcher reads source in the
> platform charset (often windows-1252). The harness is UTF-8-safe, and the
> rule below keeps every exercise's *code* ASCII, so this never bites — but
> if you add `"€"` to a string yourself, run with `java -Dfile.encoding=UTF-8`.

## Module layout

```
bootcamp/NN-topic-name/
├── README.md          the lesson (read first, ~10 min)
├── exercises/
│   ├── 01-short-name.java
│   └── ...
└── solutions/
    ├── 01-short-name.java   same tests, working code, + walkthrough comment
    └── ...
```

- Module directories match `^\d{2}-[a-z0-9-]+$`.
- Two-digit numbering, kebab-case names, identical filenames between
  `exercises/` and `solutions/`.
- Zero dependencies. The JDK only — if it is not in `java.base`, it is not
  used. Java 17 language level (`--release 17`): records, switch
  expressions, text blocks, `instanceof` patterns, sealed types; NOT
  record patterns or pattern `switch` (those are 21).
- Files starting with `_` are ignored by the tools.
- LF line endings, UTF-8 without BOM, spaces not tabs.

## Exercise file template

```java
// ─────────────────────────────────────────────────────────────────────────
//  01 · count the vowels                                    ★☆☆ warm-up
//  concepts: strings · loops · char
//  run: java -cp ../../_lib/out 01-count-vowels.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Real-world setup in one or two sentences, then exactly what to build.
//  Show concrete input → output examples:
//
//      countVowels("hello")   → 2
//
//  hint: one line, last in the block, never the whole approach.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static int countVowels(String text) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("counts lowercase vowels", () -> eq(countVowels("hello"), 2));
        // ...4–9 focused tests total
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
```

Rules:

1. **Header block** (lines 1–5, exactly): rule, `NN · name` + two or more
   spaces + difficulty stars + tier, `concepts:` line (`·`-separated),
   `run:` line, rule. Difficulty: `★☆☆ warm-up`, `★★☆ core`,
   `★★★ stretch`. This is the csbootcamp header grammar byte for byte, so
   one parser reads both tracks.
2. **Prompt**: the `//` lines after the header, up to the first non-comment
   line. 3–18 lines. Concrete examples with `→` arrows. One `hint:` line,
   placed last. Never reveal the approach.
3. **The class is always `Exercise`**, package-less and not `public` (so
   the file name can be `01-count-vowels.java`). Everything lives inside
   it: the methods you write are `static`; the types an exercise needs
   (records, enums, classes) are nested `static` types declared ABOVE the
   tests. If the thing being built IS a type (module 05 onwards), the
   prompt says which nested type to edit.
4. **Starter code**: real parameter names and
   `throw new UnsupportedOperationException("TODO");` as the body — the
   harness reports that as `☐ todo`, not as a failure. One TODO per method
   or constructor. Fields a learner will need are declared in the stub.
5. **Tests**: the block from `// ─── tests ───` to `// ─── end tests ───`
   (both lines included) is `main`, and nothing else. 4–9 `test(...)`
   calls, one per line start. Names read as a spec ("is case-insensitive",
   not "test 2"). Cover the happy path, at least one edge case, and the
   tricky case that catches the naive solution. Every test must exercise
   the learner's code — Verify fails an exercise where any test passes
   against the untouched stub.
6. **`System.exit(report());`** is the last statement of `main`. It prints
   the summary and becomes the process exit code.
7. Available via `import static bootcamp.Check.*;`: `test, eq, ok, raises,
   approx, report` (and `deepEquals`, `show`). `test` takes a
   `ThrowingRunnable`, so a body may throw checked exceptions (handy for
   file tests). See "Equality" below for what `eq` means.
8. **Deterministic, fast, offline.** No wall clock (`LocalDate.now()`,
   `Instant.now()`, `System.currentTimeMillis()` are injected as a `Clock`
   or a parameter instead), no unseeded randomness, no network, no
   default-locale or default-timezone dependence. File tests create a
   `Files.createTempDirectory(...)` and delete it before the test returns.
   A file runs in well under 2 s.
9. **Code is ASCII.** Non-ASCII characters go in comments only; in code use
   `\uXXXX` escapes (`"€"` for €). This keeps the Java 17 single-file
   launcher happy on any platform charset. Verify enforces it.
10. Comment lines in the header block ≤ 78 chars. Code: modern Java 17 —
    `var` where the type is obvious, records for data, switch expressions,
    streams where they read better than a loop (and loops where they
    don't).
11. Imports the tests need are in the starter's import list (the tests
    block cannot add imports). Keep `import java.util.*;` and friends.

## Equality — what `eq` means

`eq(actual, expected)` is deep and structural:

- arrays, `List`s and other ordered collections compare element by
  element (an `int[]` equals a `List<Integer>` with the same items);
- `Set`s compare as sets, `Map`s as maps — iteration order never matters;
- records compare component by component, recursively;
- `Optional`s compare their contents;
- **`BigDecimal` compares with `compareTo`**: `2.50` equals `2.5`. A test
  that cares about the scale (money formatting) asserts it explicitly:
  `eq(total.scale(), 2)` or `eq(total.toPlainString(), "2.50")`;
- whole numbers compare by value across `Integer`/`Long`/`Short`/`Byte`/
  `BigInteger`, so `eq(someLong, 3)` works. `double`/`float` compare like
  `Double.equals` and never equal an integer; for computed floating-point
  results use `approx(actual, expected, eps)`;
- everything else uses `equals`.

## Solution file template

Identical file, with:

- ` — SOLUTION` appended to the header name line,
- a `Walkthrough:` comment block (6–20 lines) right after the header,
  explaining the approach and WHY, naming the concept it drills, the
  classic wrong turn, and — where it exists — the Java-specific trap,
- each TODO replaced by a clean, idiomatic implementation,
- the SAME tests block, byte for byte.

Both files must compile with `javac --release 17 -encoding UTF-8`:

- exercise file → every test reports `todo`, 0 passed, 0 failed
- solution file → every test passes

## Debug hunts and scaffolded drills

Two teaching methods change what a STARTER looks like. Everything else
in this contract (header, tests block, 4–9 tests, Walkthrough, ASCII,
byte-identical tests) holds unchanged, and **a solution is always all
green**, with no TODO stub and no `/* ___ */` hole left.

**Debug hunts** — any module whose directory name contains `debug`
(`19-debugging`, `21-design-debugging`):

- The starter is COMPLETE, realistic code that carries 1–3 bugs. It has
  no TODO stub.
- The prompt states the intended behaviour and a symptom report ("users
  say…"). It never says where the bug is.
- Verify: the starter compiles and **at least one test FAILS** (✘).
  Passing tests are expected (the code mostly works); a todo is allowed.
- The solution is the fixed code. Its Walkthrough says how to find each
  bug: reproduce, narrow, fix, and the test that pins it.
- A starter must never hang or crash the run: a bug that would loop
  forever is bounded by the tests (a fake sleeper that gives up), so the
  starter still ends in a test verdict.

**Scaffolded drills** — any module whose directory name contains
`scaffold` (`20-scaffolded`):

- **Parsons**: the prompt lists the solution's lines SCRAMBLED; the
  starter method throws the usual TODO stub; the learner arranges the
  lines. Lines marked `(?)` are candidates that may be extra (distractors
  come in marked pairs). A Parsons prompt may run past 18 lines (up to
  ~30) because it carries the lines.
- **Fill-in**: the starter is a complete program with holes. Each hole
  is a `/* ___ */` marker in a slot that compiles, usually
  `/* ___ */ hole()`, where `hole()` is a starter helper that throws
  `UnsupportedOperationException("TODO")`, so an untouched hole reports
  todo. The solution fills every hole and drops the helper.
- Verify: the starter has a TODO stub or a `/* ___ */` hole, **0 tests
  pass**, and every test is todo or failed (as for an ordinary starter,
  each test must reach a stub or a hole).

## Grading = learner head + canonical tests + learner tail

A file splits into three parts: the **head** (everything above the
`// ─── tests ───` line), the **tests block** (that line through the
`// ─── end tests ───` line) and the **tail** (the rest — usually just the
class's closing `}`). A grader builds the program it runs from the
learner's head and tail around the EXERCISE's tests block, so editing the
tests never changes a grade.

## The grading channel (BqMain)

Learners run files directly and read `#done passed=N failed=N todo=N` on
stdout. A grader must not trust stdout (the code under test can print
anything), so it launches the compiled class through `bootcamp.BqMain`:

```
java -Dbq.nonce.file=<F> -Dbq.result.file=<R> \
     -cp <_lib/out><sep><classes> bootcamp.BqMain Exercise
```

1. BqMain reads the nonce from `F` (`[A-Za-z0-9_-]+`) and **deletes `F`
   before the exercise class is loaded**.
2. It loads and initialises `Exercise` (static initialisers run here;
   anything they register with `Check` is discarded), arms `Check`, and
   invokes `main`.
3. `report()` prints the summary as usual and also writes one line to `R`:
   `#bq-result <nonce> <base64 of UTF-8 JSON>` where the JSON is
   `{"passed":N,"failed":N,"todo":N,"tests":[{"name":…,"status":"pass"|"fail"|"todo","detail"?:…}]}`.

No result file, or a wrong nonce, means a crash — never a pass. The pass
rule is `passed > 0 && failed == 0 && todo == 0`; the exit code is never
trusted. Known limit: learner code that itself calls `report()` and then
`System.exit` mid-run produces a result with fewer tests — a grader that
cares compares the reported test names with the canonical tests block.
Without `-Dbq.nonce.file`, BqMain simply runs `main`, and `report()` writes
nothing.

Exit codes of BqMain before `main` runs: 2 usage, 3 nonce file unreadable
or undeletable, 4 class or `main` missing; an uncaught exception out of
`main` exits 1.

## Module README.md (the lesson)

Structure, in order — total read ~8–12 minutes:

```md
# NN · Topic Name

One-paragraph "why this matters" hook.

## The mental model
The 2–4 core ideas, each with a tiny snippet.

## The details that bite
Numbered gotchas with one-line code proof each.

## Cheat table (optional)
Small table if the topic is API-shaped.

## Exercises
| # | file | ★ | what you build |

Then: "Do the warm-ups and core in order. Stretch if time allows."
```

Tone: direct, second person, no fluff. A sharp senior explaining to a
junior at a whiteboard. Last line, after a `---` rule:
`**Next:** \`bootcamp/NN-next\``.

## Verification (mandatory before you finish)

```
java bootcamp/Verify.java        every module   (from the repo root)
java bootcamp/Verify.java 07     one module     (prefix match)
```

Verify compiles the harness, then every file with
`javac --release 17 -encoding UTF-8`, runs each through `BqMain` with a
fresh nonce, and enforces everything above: header grammar, the
exercise/solution pairing, byte-identical tests blocks, 4–9 tests, ASCII
code, every solution all green, every exercise todo-only (debug hunts:
compiles and fails at least one test; scaffolded: nothing passes). It ends
`N files · N tests · 0 broken`. Fix before finishing; do not report done
with red files.
