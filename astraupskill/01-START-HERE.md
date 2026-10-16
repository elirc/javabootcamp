# Start here: how this repository works

This bootcamp exists because the same handful of bugs appear in nearly
every beginner Java command-line project: a to-do manager that loses a
task when you remove item 0, a bank simulator whose balance ends in
.30000000000000004, a library tracker that crashes on the first unknown
ISBN, an expense importer that gives up at the first bad line. Each
exercise isolates one of those bugs, asks you to write the code that
avoids it, and proves you did with tests.

## One file is one exercise

Every exercise is a single `.java` file with three parts. At the top, a
comment block: a header (number, title, difficulty, concepts) and a prompt
with concrete examples and one hint. In the middle, the code you edit:
methods or nested types whose bodies throw
`UnsupportedOperationException("TODO")`. At the bottom, between the
`// ─── tests ───` and `// ─── end tests ───` markers, the `main` method
that runs the tests and calls `System.exit(report())`.

The tests are the specification. Their names are written as sentences
("refuses to overdraw", "a duplicate delivery charges nothing") so that
reading them tells you what to build before you read a single line of
the prompt twice. A test that throws the TODO stub shows as `☐ todo`, not
as a failure: an untouched file is a to-do list, not a wall of red.

## Running a file

The JDK's launcher can run a single source file directly. The only extra
ingredient is the tiny test harness in `bootcamp/_lib/check`, which you
compile once into `bootcamp/_lib/out` (running `java bootcamp/Verify.java`
also does it). From then on, inside any `exercises/` or `solutions/`
folder:

```
java -cp ../../_lib/out 01-count-vowels.java
```

The output ends with a `#done passed=N failed=N todo=N` line. Green means
every test passed and the next file is waiting.

## Solutions are for after

Every exercise has a twin in `solutions/` with the same tests, a working
implementation and a `Walkthrough:` comment explaining the approach, the
classic wrong turn, and the Java-specific trap. Reading it before you have
tried turns a lesson into a fact you forget by the weekend. Reading it
right after you went green is where most of the learning happens: you
compare two working answers and decide which you would rather maintain.

## Verify is the maintainer's safety net

`java bootcamp/Verify.java` compiles every exercise and solution with the
exact settings a grader uses, runs each one through the grading launcher,
and checks that every solution passes, every exercise is todo-only, and
each pair shares a byte-identical tests block. If you add an exercise of
your own, follow `bootcamp/_lib/FORMAT.md` and run Verify before you
trust it.

## How to study

Read a module's README first; it names the traps the exercises are built
around. Do the warm-ups and core exercises in order and leave a stretch
for a second pass if you must. When you get stuck, use the hint, then the
README, then the walkthrough — in that order, and only after a real
attempt. Finally, take each lesson home: the point is not 104 green files,
it is a to-do manager, a bank simulator and an order system of your own
that no longer have these bugs.
