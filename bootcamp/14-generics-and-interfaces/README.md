# 14 · Generics and Interfaces

The system-design block taught you what to build. This block is about
building it so that it is easy to keep changing. The first tool is the
type system. Generics let you write a cache, a repository or a pipeline
once and have the compiler check every use of it: a
`Repository<Customer, Long>` cannot be handed a `String` id, and a
`Key<Integer>` hands back an `Integer` with no cast. Interfaces with
default and static methods let you ship behaviour along with the
contract, the way `Comparator.thenComparing` and `Function.andThen` do.
Most "generics are hard" moments come from four ideas: bounds,
wildcards, inference and erasure. This module drills each one on code
shaped like what you write at work.

## The mental model

### 1 · A type parameter is a promise checked at compile time

```java
static <T extends Comparable<? super T>> T max(Collection<? extends T> items)
```

Read it right to left. `items` produces Ts (or subtypes). T must be
comparable with *something at or above it*, which is why `Beta extends
Version` works even though Beta is a `Comparable<Version>`, not a
`Comparable<Beta>`. This is the exact signature of `Collections.max`.
A **generic method** declares its own `<T>` before the return type, and
each call binds it again. A **generic class** binds it once per instance:
`new InMemoryRepository<Customer, Long>()`.

### 2 · PECS: producer extends, consumer super

```java
static <T> int copyAll(Collection<? super T> dst, Collection<? extends T> src)
```

| you only... | declare it as | example |
| --- | --- | --- |
| read Ts out of it | `? extends T` | `sum(Collection<? extends Number>)` |
| write Ts into it | `? super T` | `addRange(List<? super Integer>, ...)` |
| both read and write | plain `T` | `swap(List<T>)` |
| neither (size, clear) | `?` | `swapEnds(List<?>)` + a capture helper |

A `List<Integer>` is **not** a `List<Number>`. If it were, you could add
a `Double` to a list of Integers through the `List<Number>` view.
Wildcards are how you say "a list of some subtype, read-only for me".

### 3 · Interfaces carry behaviour

```java
interface PricingRule {
    BigDecimal apply(BigDecimal price);                  // the contract
    static PricingRule percentOff(int p) { ... }         // a factory
    default PricingRule andThen(PricingRule next) {      // a combinator
        return price -> next.apply(apply(price));
    }
}
```

A single abstract method makes it a functional interface, so lambdas
implement it. Default methods compose instances into new instances, and
static methods name the common cases. `Comparator` is the model citizen:
`comparing(...).thenComparing(..., reverseOrder())` reads like the
sentence it implements.

### 4 · Erasure: generics vanish at runtime

`List<String>` and `List<Integer>` are the same class at runtime. So you
cannot write `new T[]`, `instanceof List<String>` or `T.class`, and a
raw type can smuggle a wrong value past the compiler with only a
warning. When you need the type at runtime, carry it yourself: a
`Class<T>` **type token** (`Key.of("port", Integer.class)`) can
`cast(...)` and check a value.

```
compile time:  Key<Integer> port   ctx.put(port, "x")   ✘ does not compile
runtime:       Key raw = port;     ctx.put(raw, "x")    only type.cast(...) stops it
```

## The details that bite

1. **`T extends Comparable<T>` is too strict.** It rejects subclasses
   that inherit `compareTo`. Use `Comparable<? super T>` (exercise 01).
2. **`>=` versus `>` changes which tie wins.** "First of equal maxima"
   needs a strict comparison (exercise 01).
3. **Return what the collection accepted, not what you offered.**
   `Set.add` returns false on a duplicate (exercise 02).
4. **You cannot write into a `List<?>`.** Capture the wildcard in a
   private `<T>` helper. Never call `set` on a list that does not need
   changing, because it may be immutable (exercise 02).
5. **Map choice is a promise about order.** HashMap scrambles,
   LinkedHashMap keeps insertion order, TreeMap sorts (exercises 03, 06).
6. **Lazy means you stop pulling.** Copying an `Iterable` into a list
   before searching reads the whole source (exercise 03).
7. **`andThen` order is "this, then next".** Swapping the calls still
   compiles (exercise 04).
8. **Two interfaces with the same default force an override.** Call a
   specific one with `Greeter.super.greeting()` (exercise 04).
9. **`.reversed()` reverses the whole chain, and the nulls with it.**
   Pass the direction for ONE key: `thenComparing(key, reverseOrder())`,
   `nullsLast(reverseOrder())` (exercise 05).
10. **Long ids sort as numbers only if you keep them as Longs.**
    `"10" < "9"` as strings (exercise 06).
11. **Validate the whole batch before writing any of it** (exercise 06).
12. **Check on write, not only on read.** A raw key puts a bad value in
    the map; it fails much later unless `put` casts (exercise 07).
13. **`int.class.cast(5)` throws.** Type tokens must be wrapper classes
    (exercise 07).
14. **`Optional.of(f(x))` throws on null; `optional.map(f)` drops it**
    (exercise 08).

## Cheat table

| want | write |
| --- | --- |
| largest of anything comparable | `<T extends Comparable<? super T>> T max(Collection<? extends T>)` |
| copy into a wider collection | `<T> void copy(Collection<? super T> dst, Collection<? extends T> src)` |
| explicit type arguments | `Exercise.<Number, Number>zip(ints, doubles)` |
| group into lists | `map.computeIfAbsent(k, x -> new ArrayList<>()).add(v)` |
| sort by key, one key descending | `comparing(A::x).thenComparing(A::y, reverseOrder())` |
| nulls last, descending | `comparing(A::y, nullsLast(reverseOrder()))` |
| call a specific default | `Greeter.super.greeting()` |
| runtime type check | `type.cast(value)` with a `Class<T>` |

## Exercises

| # | file | ★ | what you build |
| --- | --- | --- | --- |
| 01 | `01-bounded-max.java` | ★☆☆ | `max`, `clamp` and `topN` with `Comparable<? super T>` |
| 02 | `02-wildcards-pecs.java` | ★☆☆ | copy, sum and range helpers with `? extends` / `? super`, wildcard capture |
| 03 | `03-generic-methods.java` | ★★☆ | `invert`, `zip`, `indexBy` and a lazy `firstMatch` |
| 04 | `04-default-methods.java` | ★★☆ | composable pricing rules on an interface, and the diamond rule |
| 05 | `05-comparator-composition.java` | ★★☆ | multi-key, nulls-last, explicit-order and case-aware comparators |
| 06 | `06-generic-repository.java` | ★★☆ | one `InMemoryRepository<T extends Entity<ID>, ID>` for every entity |
| 07 | `07-typesafe-container.java` | ★★★ | a typed-key context map that a raw type cannot corrupt |
| 08 | `08-functional-pipeline.java` | ★★★ | an immutable `Pipeline<I, O>` of map, filter, flatMap and peek steps |

Do the warm-ups and core in order. Stretch if time allows. Exercise 06
is the shape behind Spring Data's `CrudRepository<T, ID>`. The keys in
07 are how frameworks store request attributes. The pipeline in 08 is a
small `Stream`, and the next module's `Result<T>` is built the same way.

---

**Next:** `bootcamp/15-sealed-types-and-patterns`
