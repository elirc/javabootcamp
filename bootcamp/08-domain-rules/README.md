# 08 · Domain Rules

The last module is where the earlier ones pay off. A library, a bank, an
order system and a quiz are not interesting because of their loops; they
are interesting because of their *rules*: an order can't be cancelled
once shipped, an account never goes negative, a payment is charged
exactly once, a book on the shelf has no borrower. This module is about
writing those rules down so the code cannot break them.

## The mental model

### 1 · "Maybe" belongs in the type

```java
Optional<Book> findByIsbn(String isbn)       // the signature admits "no answer"

findByIsbn(isbn).map(Book::title).orElse("unknown");
member.flatMap(m -> Optional.ofNullable(m.loanId()))   // nullable field → Optional
```

Return `Optional`; don't store it in fields or take it as a parameter;
never `get()` without knowing.

### 2 · A state machine is a table

```java
Set<Status> next() {
    return switch (this) {
        case NEW -> EnumSet.of(PAID, CANCELLED);
        case PAID -> EnumSet.of(SHIPPED, CANCELLED);
        case SHIPPED -> EnumSet.of(DELIVERED);
        case DELIVERED, CANCELLED -> EnumSet.noneOf(Status.class);
    };
}
```

Write the table once; derive `canMoveTo`, `moveTo`, `isFinal` from it.
Expose actions (`start()`, `finish()`), not `setState(...)`.

### 3 · Invariants: guard first, change last

Every public method either keeps the object's invariants or refuses —
with `IllegalArgumentException` for a bad argument and
`IllegalStateException` for "not now" — and a refusal changes nothing.

### 4 · Multi-step changes: validate everything, then apply everything

Two passes make an all-or-nothing update without rollbacks: the first
only checks, the second only writes and cannot fail.

### 5 · Idempotency: remember what you've done

Requests arrive twice. Give each one an id, record the id when (and only
when) the effect happens, and turn a repeat into a reported no-op.

## The details that bite

1. **`Optional.of(null)` throws.** Use `ofNullable` at the boundary with
   nullable data.
2. **`map` vs `flatMap`**: a step that itself returns `Optional` needs
   `flatMap`, or you get `Optional<Optional<T>>`.
3. **Enum constants can't reference later constants** in their constructor
   arguments — keep transition tables in a method.
4. **Walk the authoritative collection.** Loop over the quiz's questions,
   not the player's answers, or unanswered questions vanish.
5. **`answers.get(id) == correct`** unboxes; a missing answer makes it
   throw. `Objects.equals` is null-safe.
6. **Record a payment id only after the charge succeeds**, or a
   temporary decline becomes permanent.
7. **Expose history read-only.** Returning the internal list lets callers
   rewrite the past.
8. **Prefer derived data to a second counter** that can drift out of sync.

## Exercises

| # | file | ★ | what you build |
| --- | --- | --- | --- |
| 01 | `01-optional-lookup.java` | ★☆☆ | book lookups with map, orElse, ofNullable, flatMap |
| 02 | `02-optional-chain.java` | ★★☆ | member → loan → due date with no null checks |
| 03 | `03-order-states.java` | ★★☆ | an order status state machine on an enum |
| 04 | `04-account-invariants.java` | ★★☆ | an account with frozen/closed rules that always hold |
| 05 | `05-stock-reservation.java` | ★★★ | all-or-nothing stock reservation |
| 06 | `06-quiz-scoring.java` | ★★☆ | quiz scoring with missed questions and a percentage |
| 07 | `07-task-lifecycle.java` | ★★☆ | a task's lifecycle with a read-only audit history |
| 08 | `08-idempotent-payments.java` | ★★★ | payments charged exactly once, retries welcome |

Do the warm-ups and core in order. Stretch if time allows.

---

**Done with the fundamentals?** Go back to the project you started with and
apply one rule from each module to it. Then start the System Design
section (modules 09–13). **Next:** `bootcamp/09-caching`
