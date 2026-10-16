# 02 · Methods and Lambdas

Every beginner CLI project starts as one long `main` and gets better the
day it is split into methods that take arguments and return answers. This
module is about that split, and about the move after it: passing
*behaviour* around — a rule, a formatter, a retry policy — as a value.

Java got lambdas late (Java 8) and bolted them onto interfaces. Once you
see that a lambda is "an object implementing a one-method interface", the
whole `java.util.function` package stops being a zoo.

## The mental model

### 1 · A lambda is an instance of a functional interface

```java
Predicate<String> notBlank = s -> !s.isBlank();     // test(String) -> boolean
Function<String, Integer> len = String::length;     // apply(String) -> Integer
Supplier<LocalDate> today = () -> LocalDate.of(2024, 1, 1);  // get()
```

There is no "function type". The *target type* — the variable, parameter
or return type — decides which interface the lambda implements.

| interface | method | shape |
| --- | --- | --- |
| `Function<A, B>` | `apply` | A → B |
| `UnaryOperator<T>` | `apply` | T → T |
| `BiFunction<A, B, C>` | `apply` | (A, B) → C |
| `Predicate<T>` | `test` | T → boolean |
| `Supplier<T>` | `get` | () → T |
| `Consumer<T>` | `accept` | T → void |
| `IntUnaryOperator`, `IntSupplier`, … | | primitive versions, no boxing |

### 2 · Lambdas capture values, and only effectively-final ones

```java
int limit = 3;
Predicate<String> shortEnough = s -> s.length() <= limit;  // fine
limit = 4;   // now the lambda above no longer compiles
```

To keep *mutable* state in a closure, capture a final reference to
something mutable — an `int[1]`, an `AtomicInteger`, a field.

### 3 · Functions that build functions

```java
static Predicate<String> maxLength(int n) { return s -> s.length() <= n; }
var rule = maxLength(10).and(s -> s.endsWith("?"));
```

`and`, `or`, `negate`, `andThen`, `compose`, `Comparator.comparing(...)`
— the JDK's own higher-order functions. Learn their order rules once.

### 4 · Method references are lambdas with a name

`Integer::parseInt` (static), `String::length` (unbound — the first
argument is the receiver), `System.out::println` (bound), `Book::new`
(constructor).

## The details that bite

1. **`f.andThen(g)` runs f first; `f.compose(g)` runs g first.**
2. **A `for (int i…)` variable can't be captured** — copy it into a local
   inside the loop body. (This is the compiler preventing the classic
   "every callback sees the last i" bug.)
3. **`stream().count()` returns `long`.** Cast or use `Math.toIntExact`.
4. **`List.of(...)` is immutable.** `sort`, `add`, `remove` on it throw
   `UnsupportedOperationException`. Sort a copy.
5. **`computeIfAbsent` never stores `null`** — a null result is
   recomputed every time.
6. **`Math.abs(Integer.MIN_VALUE)` is negative.** Two's complement has one
   more negative number than positive.
7. **Overload resolution happens at compile time** and prefers widening,
   then boxing, then varargs.
8. **No tail calls.** Deep recursion overflows the stack; recurse on
   structure, loop on size.

## Exercises

| # | file | ★ | what you build |
| --- | --- | --- | --- |
| 01 | `01-varargs-and-overloads.java` | ★☆☆ | sum of anything, and "at least one" tags |
| 02 | `02-functional-interfaces.java` | ★☆☆ | applyTwice, countMatching, pipe |
| 03 | `03-closures-and-capture.java` | ★★☆ | independent counters, and lambdas in a loop |
| 04 | `04-method-references.java` | ★☆☆ | the four kinds of `::` in a library import |
| 05 | `05-predicate-rules.java` | ★★☆ | named validation rules, combined and reported |
| 06 | `06-recursion.java` | ★★☆ | digit sums (and the abs trap), flattening nested lists |
| 07 | `07-memoize.java` | ★★★ | a cache wrapper that remembers null too |
| 08 | `08-retry.java` | ★★★ | retry with suppressed exceptions |

Do the warm-ups and core in order. Stretch if time allows.

---

**Next:** `bootcamp/03-collections-and-streams`
