# 16 · Design Patterns

A design pattern is a named answer to a problem that keeps coming back:
"the price rule depends on the customer", "five things must react when
stock changes", "this old SDK does not fit our interface". The names
matter as much as the code. Saying "wrap it in a decorator" in a review
replaces ten minutes of whiteboard. This module builds the eight
patterns you will meet most often in business code, each in a small
realistic scenario, and in the shape they take in **modern** Java:
lambdas instead of one-method classes, records instead of hand-written
value classes, and composition preferred over inheritance.

## The mental model

### 1 · Most patterns are "program to an interface, vary behind it"

```
           ┌──────────────────────┐
caller ───►│ interface (one role) │◄── strategy, decorator, adapter, command,
           └──────────────────────┘    observer listener, factory product
                     ▲
        ┌────────────┼──────────────┐
   impl A      impl B wrapping A    impl C adapting a legacy API
```

The caller depends on a role (`ShippingStrategy`, `PriceService`,
`PaymentGateway`). Patterns differ in **who** supplies the
implementation and **how** implementations combine.

### 2 · The eight, by the question they answer

| question | pattern | the move |
| --- | --- | --- |
| which algorithm today? | **strategy** | pass the behaviour in (`ShippingStrategy` lambdas) |
| how do I build a valid immutable object with many optional parts? | **builder** | mutable builder, one `build()` that checks everything |
| which class do I instantiate, given data? | **factory** | static factories + a scheme → maker registry |
| who needs to know when this changes? | **observer** | subscribe/notify through a listener interface |
| how do I add caching/logging/retry without touching the class? | **decorator** | same interface, wrap the inner one |
| this API does not fit mine | **adapter** | one class that translates both ways |
| fixed skeleton, variable steps? | **template method** vs **composition** | final skeleton + abstract steps, or a record of functions |
| can I undo it, queue it, log it? | **command** | the action as an object with `execute`/`undo` |

### 3 · Lambdas collapsed half the catalogue

When the role has one method, the pattern is often just a function:

```java
ShippingStrategy flat = parcel -> new BigDecimal("4.99");      // strategy
StockListener alert = (sku, old, now) -> { ... };              // observer
f.register("sms", Exercise::sms);                              // factory maker
```

Use a class when the implementation has state (`Caching`), several
methods (`Command` needs `undo`), or a name worth showing in a stack
trace.

### 4 · Composition over inheritance

Inheritance fixes the relationship at compile time and exposes the
parent's internals. Composition (decorators, strategies, a record of
functions) lets you combine behaviour at runtime:

```java
new Caching(new Retrying(new Logging(remote), 3))   // each layer: one job
```

The template method is the inheritance-based pattern that survives,
for a skeleton that must not change (exercise 07 builds both forms and
proves they match).

## The details that bite

1. **"Free over 50" includes 50.** `compareTo(...) >= 0`, and never
   `equals` on `BigDecimal` (exercise 01).
2. **Round weight UP for shipping.** `CEILING`, not `HALF_UP`
   (exercise 01).
3. **A builder that copies a map of lists shallowly leaks.** The built
   object changes when the builder does (exercise 02).
4. **Cross-field rules belong in `build()`;** single-value rules fail
   at once (exercise 02).
5. **Lower-case the key on both register and lookup**, and split a uri
   at the FIRST colon (exercise 03).
6. **Notify a snapshot.** A listener that cancels itself during the
   loop skips the next listener or throws
   `ConcurrentModificationException` (exercise 04).
7. **One failing listener must not silence the rest** (exercise 04).
8. **Edge-trigger alerts.** Fire on the crossing, not on every change
   while low (exercise 04).
9. **Never cache a failure, and rethrow the LAST error after retries**
   (exercise 05).
10. **Decorator order is behaviour:** logging outside or inside the
    cache logs different things (exercise 05).
11. **Validate before calling the legacy system,** and treat an
    unknown response as an error, not a decline (exercise 06).
12. **Money crosses the boundary as exact cents,** via
    `movePointRight(2).longValueExact()` (exercise 06).
13. **CSV: quote fields holding a comma, quote or line break, and
    double the quotes** (exercise 07).
14. **Undo restores saved state; a new command clears redo; a macro
    undoes newest first** (exercise 08).

## Cheat table

| pattern | JDK / framework example |
| --- | --- |
| strategy | `Comparator`, `java.util.concurrent.RejectedExecutionHandler` |
| builder | `HttpRequest.newBuilder()`, `StringBuilder`, Lombok `@Builder` |
| factory | `List.of`, `DriverManager.getConnection("jdbc:...")`, `ServiceLoader` |
| observer | `PropertyChangeListener`, Spring `ApplicationListener`, UI event listeners |
| decorator | `BufferedReader(new InputStreamReader(in))`, `Collections.unmodifiableList` |
| adapter | `Arrays.asList`, `InputStreamReader` (bytes → chars) |
| template method | `AbstractList`, `HttpServlet.service` → `doGet` |
| command | `Runnable` in an executor queue, undo stacks in editors |

## Exercises

| # | file | ★ | what you build |
| --- | --- | --- | --- |
| 01 | `01-strategy-shipping.java` | ★☆☆ | shipping-rate strategies as lambdas, composed and chosen at checkout |
| 02 | `02-builder-request.java` | ★☆☆ | an immutable HTTP request with a validating builder and `toBuilder()` |
| 03 | `03-factory-notifiers.java` | ★☆☆ | validated static factories and a uri-scheme factory registry |
| 04 | `04-observer-stock.java` | ★★☆ | stock listeners with subscriptions, isolation and edge-triggered alerts |
| 05 | `05-decorator-prices.java` | ★★☆ | caching, logging, retry and fallback decorators, in the right order |
| 06 | `06-adapter-gateway.java` | ★★☆ | a typed payment gateway over a string-based legacy bank SDK |
| 07 | `07-template-vs-composition.java` | ★★☆ | the same CSV export as a template method and as composed functions |
| 08 | `08-command-undo.java` | ★★★ | cart commands with undo/redo stacks and all-or-nothing macros |

Do the warm-ups and core in order. Stretch if time allows. The
decorators in 05 are module 09 (caching) and module 10 (retries) behind
one interface, and the observer in 04 is module 12's pub/sub bus inside
one process. The next module asks what happens to these objects when
several threads use them at once.

---

**Next:** `bootcamp/17-deterministic-concurrency`
