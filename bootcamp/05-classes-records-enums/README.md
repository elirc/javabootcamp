# 05 · Classes, Records and Enums

Up to now the exercises were static methods: data in, answer out. Real
projects are made of *types* — a `Book`, a `BankAccount`, an `Order`, a
`Priority` — that own their data and refuse to be put in an invalid
state. This module is about designing those types so that bugs become
impossible rather than merely unlikely.

From here on the thing you edit is often a nested type above the tests.
The prompt always says which one.

## The mental model

### 1 · A record is a named tuple with free equality

```java
record Book(String isbn, String title, int year) {
    Book {                                   // compact constructor
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title");
        title = title.strip();               // reassign the PARAMETER
    }
}
```

Records give you the constructor, private final fields, accessors
(`title()`), `equals`, `hashCode` and `toString`. Use them for values:
things identified by their contents.

### 2 · A class owns state and guards it

```java
final class BankAccount {
    private long balanceCents;               // nobody else can write this
    void withdraw(long cents) {
        if (cents <= 0) throw new IllegalArgumentException(...);   // bad argument
        if (cents > balanceCents) throw new IllegalStateException(...); // bad timing
        balanceCents -= cents;               // check first, change last
    }
}
```

### 3 · Enums are classes with a fixed set of instances

```java
enum Priority {
    LOW("L", 7), HIGH("H", 1);
    private final String code; private final int daysToDue;
    Priority(String code, int days) { this.code = code; this.daysToDue = days; }
}
```

Per-constant data goes next to the constant. `EnumMap` and `EnumSet` are
the fast, ordered collections for enum keys.

### 4 · Interfaces name a capability; sealed interfaces close the family

```java
interface Discount { long apply(long cents); }
record PercentOff(int percent) implements Discount { ... }

sealed interface Command permits Add, Complete, Rename {}
```

## The details that bite

1. **Override `equals` and you must override `hashCode`**, from the same
   fields — or `HashSet`/`HashMap` silently break.
2. **`equals(Isbn other)` is an overload, not an override.** The
   parameter must be `Object`. Put `@Override` on it.
3. **`final` stops reassignment, not mutation.** A `final List` can still
   be added to. Copy in (`List.copyOf`) and don't leak mutable state out.
4. **`valueOf("m")` matches constant NAMES**, case-sensitively. Look up
   your own codes with a loop or a map.
5. **Never persist `ordinal()`.** Reordering constants changes it.
6. **`IllegalArgumentException` = the caller passed garbage;
   `IllegalStateException` = the object can't do that right now.**
7. **Validate in the constructor**, and every instance is valid — no
   `isValid()` to forget.
8. **Java 17 has `instanceof` patterns, not pattern `switch`** (that is
   Java 21). Dispatch on a sealed family with an if-chain.

## Exercises

| # | file | ★ | what you build |
| --- | --- | --- | --- |
| 01 | `01-record-book.java` | ★☆☆ | a Book record that validates and trims itself |
| 02 | `02-bank-account.java` | ★☆☆ | an encapsulated account: deposit, withdraw, no overdraft |
| 03 | `03-equals-and-hashcode.java` | ★★☆ | an Isbn value class that works in sets and maps |
| 04 | `04-priority-enum.java` | ★★☆ | an enum with codes, raise(), and an EnumMap count |
| 05 | `05-discount-interface.java` | ★★☆ | three discount strategies behind one interface |
| 06 | `06-question-builder.java` | ★★☆ | a fluent builder that validates at build() |
| 07 | `07-sealed-commands.java` | ★★★ | to-do commands as a sealed family of records |
| 08 | `08-immutable-order.java` | ★★★ | an Order with no way in or out for mutation |

Do the warm-ups and core in order. Stretch if time allows.

---

**Next:** `bootcamp/06-exceptions-and-files`
