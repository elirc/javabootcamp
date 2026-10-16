# 01 · Language Core

Java is a big platform with a small core language. This module is that
core: what `==` really compares, why `int` silently wraps, how a boxed
`Integer` turns a missing value into a crash, and how a `switch` became an
expression that the compiler can check for you.

None of it is exotic. All of it shows up in the first hour of any
beginner CLI project — the to-do manager, the quiz engine, the bank
simulator — and every item here is a bug someone shipped this week.

## The mental model

### 1 · Primitives hold values, everything else holds a reference

```java
int a = 1;  int b = a;  b = 99;          // a is still 1 — b got a copy

int[] xs = {1};  int[] ys = xs;  ys[0] = 99;   // xs[0] is now 99 — same array
```

The eight primitives (`int`, `long`, `double`, `boolean`, `char`, `byte`,
`short`, `float`) are values. Everything else — `String`, arrays,
`Integer`, every class and record — is a reference to an object, and `==`
on references asks *"same object?"*, never *"same contents?"*.

### 2 · The operand types decide, not the variable you assign to

```java
double avg = 7 / 2;          // 3.0 — int division happened first
double ok  = (double) 7 / 2; // 3.5
long secs  = days * 86_400;  // int multiply, overflow, THEN widen
long right = days * 86_400L; // long multiply
```

### 3 · Boxing is invisible until it isn't

`int` ↔ `Integer` conversion is automatic. That is convenient right up to
the two places it bites: `==` between two `Integer`s (reference
comparison, which *happens* to work for -128..127 because of a cache), and
unboxing `null` (a `NullPointerException` on a line with no dot in it).

```java
Integer x = 1000, y = 1000;
x == y;                       // false
x.equals(y);                  // true
int n = map.get("missing");   // NullPointerException
```

### 4 · `switch` is an expression now

```java
var cmd = switch (word) {
    case "add", "a" -> Command.ADD;
    case "quit", "q" -> Command.QUIT;
    default -> Command.UNKNOWN;
};
```

Arrow arms never fall through, several labels share one arm, and the
whole thing produces a value. Switch over your own enum with *no*
`default`, and the compiler tells you when a new constant goes unhandled.

## The details that bite

1. **`==` on Strings compares objects.** Literals are interned, so tests
   with literals pass; strings read from input are not. Use `equals`, or
   `Objects.equals(a, b)` when either may be null.
2. **`int` overflow is silent.** `Integer.MAX_VALUE + 1` is
   `Integer.MIN_VALUE`. Use `long`, or `Math.addExact` /
   `multiplyExact` where a wrap would be a bug.
3. **`toLowerCase()` uses the default locale.** On a Turkish machine
   `"QUIT".toLowerCase()` contains a dotless `ı`. Say
   `toLowerCase(Locale.ROOT)` for keys and commands.
4. **`Math.round` returns `long`** and rounds halves toward positive
   infinity: `Math.round(-2.5)` is `-2`.
5. **`0.0 / 0` is `NaN`, not an exception** — and `NaN` is not equal to
   anything, itself included. Guard empty inputs before you divide.
6. **Arrays are references.** A method that writes into its array
   argument changes the caller's data. Return a new array instead.
7. **`max = 0` is not "nothing seen yet".** Start from the first element,
   or from `Integer.MIN_VALUE`, or an all-negative input breaks you.

## Cheat table

| type | size | range / use | literal |
| --- | --- | --- | --- |
| `int` | 32 bit | ±2.1 billion — counts, ids, indexes | `42`, `1_000` |
| `long` | 64 bit | ±9.2 quintillion — cents, millis, totals | `42L` |
| `double` | 64 bit | measurements, ratios — never money | `0.5`, `1e-9` |
| `char` | 16 bit | one UTF-16 unit; arithmetic as int | `'A'` |
| `Integer` etc. | object | nullable box; needed for generics | `Integer.valueOf(7)` |

## Exercises

| # | file | ★ | what you build |
| --- | --- | --- | --- |
| 01 | `01-count-vowels.java` | ★☆☆ | a string walk, and your first meeting with locale |
| 02 | `02-integer-division.java` | ★☆☆ | an average that keeps its fraction, a percent that doesn't overflow |
| 03 | `03-boxed-equality.java` | ★★☆ | Integer ids compared properly, and a map read that can't NPE |
| 04 | `04-command-switch.java` | ★★☆ | a to-do command parser with switch expressions |
| 05 | `05-string-equality.java` | ★☆☆ | quiz-answer checking without `==` |
| 06 | `06-overflow.java` | ★★☆ | cents that don't wrap: long, addExact, widening order |
| 07 | `07-running-totals.java` | ★☆☆ | arrays: a fresh result array, and a max that starts right |
| 08 | `08-column-names.java` | ★★★ | spreadsheet column names, base 26 with no zero |

Do the warm-ups and core in order. Stretch if time allows.

```
java -cp ../../_lib/out 01-count-vowels.java   # from exercises/ — yours
java -cp ../../_lib/out 01-count-vowels.java   # from solutions/ — the answer
java bootcamp/Verify.java 01                   # from the repo root — the module
```

---

**Next:** `bootcamp/02-methods-and-lambdas`
