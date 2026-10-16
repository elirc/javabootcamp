# 03 · Collections and Streams

Every CLI project in this track is, underneath, a collection with a menu
on top: a list of tasks, a map of books by ISBN, a set of tags, a history
of transactions. Picking the right container and knowing which of its
methods *mutate* is most of the job. Streams are the other half: a way to
say "from these, keep those, group by this, sum that" that reads like the
spec.

## The mental model

### 1 · Interfaces in the signature, implementations at `new`

```java
List<String> tasks = new ArrayList<>();          // ordered, indexed
Map<String, Book> byIsbn = new HashMap<>();      // key → value, no order
Set<String> tags = new LinkedHashSet<>();        // unique, insertion order
Deque<Change> history = new ArrayDeque<>();      // stack AND queue
```

The ORDER you get out is a property of the implementation:

| need | List | Set | Map |
| --- | --- | --- | --- |
| no particular order | `ArrayList` | `HashSet` | `HashMap` |
| insertion order | `ArrayList` | `LinkedHashSet` | `LinkedHashMap` |
| sorted | sort it | `TreeSet` | `TreeMap` |

### 2 · Mutable, unmodifiable, and views

`List.of`, `Set.of`, `Map.of`, `stream().toList()` and `List.copyOf` give
you **unmodifiable** collections — `add` throws. `new ArrayList<>(x)` gives
you a mutable copy. `Collections.unmodifiableList(x)` is a read-only
**view**: it can't be changed through, but it shows later changes to `x`.

### 3 · A stream is a pipeline, not a container

```java
Map<String, Integer> totals = expenses.stream()
    .filter(e -> !e.refunded())
    .collect(groupingBy(Expense::category, TreeMap::new, summingInt(Expense::cents)));
```

Nothing runs until the terminal operation (`collect`, `toList`, `count`,
`forEach`…). A stream is single-use, and it never modifies its source.

### 4 · Comparators compose

```java
Comparator.comparing(Book::year, nullsLast(reverseOrder()))
          .thenComparing(Book::title)
```

## The details that bite

1. **`list.remove(0)` on a `List<Integer>` removes by INDEX.** Use
   `remove(Integer.valueOf(0))` to remove by value.
2. **Removing inside a for-each throws `ConcurrentModificationException`**
   (usually). Use `Iterator.remove()` or `removeIf`.
3. **`retainAll`, `removeAll`, `addAll`, `sort` mutate the receiver.**
   Copy first if the collection was handed to you.
4. **`Collectors.toMap` throws on duplicate keys** unless you pass a merge
   function.
5. **`groupingBy` on a boolean can lose a key; `partitioningBy` never does.**
6. **`"x".split(regex)` with a leading delimiter yields a leading `""`**;
   `"".split(...)` yields `[""]`.
7. **`Map.get` returns `null` for "missing" AND for "present with null"** —
   `containsKey` tells them apart.
8. **`java.util.Stack` is legacy.** Use `ArrayDeque` via `Deque`.

## Exercises

| # | file | ★ | what you build |
| --- | --- | --- | --- |
| 01 | `01-list-remove.java` | ★☆☆ | remove by id not index; filter as copy vs in place |
| 02 | `02-word-counts.java` | ★☆☆ | word frequencies with merge, TreeMap, top-N |
| 03 | `03-grouping.java` | ★★☆ | a library catalogue grouped, partitioned, merged |
| 04 | `04-comparators.java` | ★★☆ | newest-first with nulls last, case-insensitive titles |
| 05 | `05-expense-pipeline.java` | ★★☆ | an expense report as one stream pipeline |
| 06 | `06-sets.java` | ★☆☆ | tag algebra that doesn't touch its inputs |
| 07 | `07-iterate-and-remove.java` | ★★☆ | returning overdue loans without a CME |
| 08 | `08-undo-stack.java` | ★★★ | undo for a to-do list with an ArrayDeque |

Do the warm-ups and core in order. Stretch if time allows.

---

**Next:** `bootcamp/04-strings-and-time`
