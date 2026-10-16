# Java Bootcamp — offline, test-graded, self-paced

A personal coding bootcamp for levelling up Java fundamentals, built around
what the beginner Java CLI projects actually need: a to-do manager, a
library tracker, a quiz engine, a bank simulator, an expense tracker, an
order system. It runs **fully offline** — every exercise is a single
self-contained file graded by its own tests, runnable with nothing but a
JDK 17.

**Start here → [`FLIGHTPLAN.md`](FLIGHTPLAN.md)** — the plan of attack.

## Quick start (one minute)

```bash
# once, from the repo root: build the tiny test harness (or just run Verify)
javac --release 17 -encoding UTF-8 -d bootcamp/_lib/out bootcamp/_lib/check/bootcamp/*.java

# then, from an exercises/ folder, run any exercise
cd bootcamp/01-language-core/exercises
java -cp ../../_lib/out 01-count-vowels.java
```

An exercise file is a prompt in comments, stubs that throw, and tests
underneath. You write the code and re-run the file until:

```
  ✔ counts lowercase vowels
  ✔ is case-insensitive
  ✔ handles the empty string

  6 passed
  all green — next file!
```

No Maven, no Gradle, no JUnit, no IDE required — `java <file>.java` runs a
single source file directly (Java 11+). (`☐ todo` = not written yet ·
`✘` = failing · `✔` = done.)

## What's here

```
FLIGHTPLAN.md        ← the plan. Read it first.
bootcamp/            ← the main event: 8 modules, 64 exercises
  01-language-core/           primitives vs references, int division and
                              overflow, boxed equality, switch expressions,
                              string equality, arrays, char arithmetic
  02-methods-and-lambdas/     varargs, functional interfaces, closures,
                              method references, predicate rules,
                              recursion, memoize, retry
  03-collections-and-streams/ List/Set/Map and their traps, word counts,
                              grouping, comparators, stream pipelines,
                              iterator removal, an undo stack
  04-strings-and-time/        formatting with a locale, split and regex,
                              LocalDate, strict parsing, an injected Clock,
                              month-end schedules
  05-classes-records-enums/   records with validation, encapsulation,
                              equals/hashCode, enums with data, interfaces,
                              builders, sealed types, immutability
  06-exceptions-and-files/    exceptions with context, checked vs unchecked,
                              try-with-resources, java.nio.file, CSV,
                              atomic save, walking trees, import reports
  07-money-and-validation/    BigDecimal, rounding modes, exact bill
                              splitting, a Money type, input validation,
                              compound interest, checkout totals, ledgers
  08-domain-rules/            Optional, state machines, invariants,
                              all-or-nothing updates, scoring, lifecycles,
                              idempotent payments
  _lib/check/bootcamp/        the test harness (Check, BqMain; JDK only)
  _lib/FORMAT.md              the authoring contract
  Verify.java                 maintainer check: every file, both ways
astraupskill/        ← how to study with this repo, and the big ideas
```

Each module: `README.md` (the lesson — read first), `exercises/` (yours),
`solutions/` (reference implementations with teaching walkthroughs — peek
only after a real attempt).

## Verify

```bash
java bootcamp/Verify.java        # every module, from the repo root
java bootcamp/Verify.java 05     # one module (prefix match)
```

Verify builds the harness, compiles every exercise and solution with
`javac --release 17 -encoding UTF-8`, runs each one, and checks the
contract: every solution all green, every exercise todo-only (nothing
failing, nothing accidentally passing), headers that parse, and a tests
block that is byte-identical between an exercise and its solution. It
ends with a census and `… files · … tests · 0 broken`.

## How grading works (for tools)

Learners read the `#done passed=N failed=N todo=N` line. Graders don't
trust stdout: they launch the compiled class through `bootcamp.BqMain`,
which reads and deletes a nonce file before any exercise code loads, and
`report()` writes a `#bq-result <nonce> <base64 json>` line to a result
file. See [`bootcamp/_lib/FORMAT.md`](bootcamp/_lib/FORMAT.md) for the
protocol and for the authoring rules every exercise follows.

## Requirements

JDK 17 or newer (`java -version`). That's all. Every exercise is
deterministic: no network, no wall clock, no unseeded randomness, no
dependence on your machine's locale or time zone; file exercises work in a
temporary folder and clean up after themselves.
