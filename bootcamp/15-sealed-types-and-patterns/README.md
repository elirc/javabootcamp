# 15 · Sealed Types and Patterns

Most domain bugs are not wrong arithmetic. They are a case nobody
handled: the refund path for an order that was paid but not shipped,
the payment method added last quarter that one report forgot. Sealed
types make the set of cases a fact the compiler and the runtime both
know. Records make each case plain data, and patterns take a value
apart safely. Together they are the Java version of what functional
languages call algebraic data types, and they change how you model a
domain: instead of one class with nullable fields and a `type` string,
you get one small record per case and code that cannot confuse them.

This module is written for **Java 17**: sealed types, records and
`instanceof` patterns are final there. Pattern `switch` and record
patterns arrived in Java 21. Each section shows what 17 gives you and
what 21 adds, so you can read both kinds of codebase.

## The mental model

### 1 · Sealed = a closed list of cases

```java
sealed interface Shape permits Circle, Rect, Triangle {}
record Circle(double radius) implements Shape {}
record Rect(double width, double height) implements Shape {}
```

Only the listed types may implement `Shape`. A permitted subtype must
be `final` (records are), `sealed` itself, or `non-sealed` (open again,
on purpose). The list can be read at runtime:
`Shape.class.getPermittedSubclasses()`.

### 2 · Patterns: test, cast and bind in one step

```java
if (s instanceof Rect r && r.width() == r.height()) return "square";
if (!(v instanceof String text)) throw new IllegalArgumentException();
return text.strip();                       // flow scoping: text is bound here
```

Java 17 has `instanceof` patterns with `&&` guards. Java 21 adds:

```java
return switch (s) {                        // Java 21: checked for exhaustiveness
    case Circle c -> Math.PI * c.radius() * c.radius();
    case Rect(double w, double h) -> w * h; // a record pattern
    case Triangle t -> heron(t);
};                                         // no default: a missing case does not compile
```

In 17 an `instanceof` chain ends with `throw new AssertionError(...)`,
because the compiler cannot prove the chain is complete.

### 3 · Two ways to add an operation: visitor or sealed dispatch

| | adding an operation | adding a case | where you see it |
| --- | --- | --- | --- |
| visitor (`accept` + `Visitor<R>`) | new class, no node changes | every visitor changes | javac tree API, ASM, ANTLR |
| sealed + patterns | new static method | every method changes; Java 21 tells you where | modern domain code |
| plain subclass methods | every class changes | one new class | classic OO |

This trade-off is the "expression problem". Sealed types choose "cases
are fixed, operations grow", which suits domains like payments, order
states and JSON, where the cases rarely change and new reports and
rules arrive every sprint.

### 4 · Errors and states as values

```
Result<T>  = Ok(value) | Err(message)                    parse, validate, import
OrderState = Draft(items) | Paid(items, paymentId) | Shipped(..., tracking) | ...
Json       = JNull | JBool | JNum | JStr | JArr | JObj
```

Each case carries exactly the data that exists in that case. A
`Shipped` has a tracking number; a `Draft` cannot have one, so no code
can read one by mistake. Transitions are pure functions
`(state, event) -> state`, which is what makes replay and testing easy.

## The details that bite

1. **Validate with `!(x > 0)`, not `x <= 0`.** NaN fails every
   comparison, so `<=` lets it in (exercise 01).
2. **"Sides must close" is strict.** 1, 2, 3 is a flat line
   (exercise 01).
3. **Order the checks: specific before general.** A blank-text check
   after the any-text check never runs (exercise 02).
4. **Narrow only after a range check.** `(int) 3_000_000_000L` is
   negative; `Integer.parseInt("99999999999")` throws (exercise 02).
5. **Use `Math.addExact` and friends.** Plain `+` wraps silently
   (exercise 03).
6. **Rewrite bottom-up.** Simplify the children first, or `x + 2*0`
   stays unsimplified (exercise 03).
7. **`map` on an `Err` must not call `f`, and `sequence` returns the
   FIRST error** (exercise 04).
8. **`"AB,".split(",")` drops the empty field.** Use `split(",", -1)`
   (exercise 04).
9. **Pre-order versus post-order.** Collect a link before the links
   nested inside it (exercise 05).
10. **Escape your own syntax characters** in Markdown and in JSON
    strings (exercises 05, 07).
11. **Refuse every transition you did not list.** A silently accepted
    "cancel after ship" is a lost parcel (exercise 06).
12. **Whole doubles print as `3.0`.** Print them as longs, but only
    within the exact range (exercise 07).
13. **Merge patch recurses into objects;** replacing the whole object
    loses its other fields (exercise 07).
14. **The nearest handler wins.** A leaf's own handler beats its sealed
    parent's, which beats the root's (exercise 08).

## Cheat table

| want | Java 17 | Java 21 |
| --- | --- | --- |
| closed set of cases | `sealed interface X permits A, B` | same |
| case with data | `record A(int n) implements X` | same |
| test and bind | `x instanceof A a` | same, plus `case A a ->` |
| guard | `x instanceof A a && a.n() > 0` | `case A a when a.n() > 0 ->` |
| destructure | `a.n()` accessors | `case A(int n) ->` |
| exhaustiveness | runtime check via `getPermittedSubclasses()` | the compiler, in `switch` |
| one-off singleton case | `enum JNull implements Json { INSTANCE }` | same |

## Exercises

| # | file | ★ | what you build |
| --- | --- | --- | --- |
| 01 | `01-sealed-shapes.java` | ★☆☆ | sealed records with validation, area, a guarded `describe` |
| 02 | `02-instanceof-patterns.java` | ★☆☆ | safe narrowing and classification of untyped config values |
| 03 | `03-expression-evaluator.java` | ★★☆ | eval, show and simplify over an expression tree |
| 04 | `04-result-type.java` | ★★☆ | a sealed `Result<T>` with map/flatMap, and an order-line parser |
| 05 | `05-visitor-vs-sealed.java` | ★★☆ | the same document operations as visitors and as sealed dispatch |
| 06 | `06-sealed-state-machine.java` | ★★☆ | an order lifecycle where each state carries its own data |
| 07 | `07-json-adt.java` | ★★★ | a JSON value type: render, path lookup, merge patch |
| 08 | `08-exhaustive-dispatch.java` | ★★★ | a dispatcher that refuses to build with an unhandled case |

Do the warm-ups and core in order. Stretch if time allows. Exercise 06
is module 08's state machine with types instead of an enum, and its
`replay` is the event-sourcing fold from module 12. Exercise 04's
`Result` is built the same way as 14's pipeline. The next module uses
these types inside design patterns.

---

**Next:** `bootcamp/16-design-patterns`
