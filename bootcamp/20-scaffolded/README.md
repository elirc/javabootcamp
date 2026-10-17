# 20 · Scaffolded Practice

A blank file is the hardest way to learn a new idea: you have to recall
the API, invent the structure and get every detail right at once.
Scaffolded exercises take some of that away on purpose, so your
attention goes to the one decision that matters. This module uses two
formats from computing-education research, each four times, on ideas
you have met in modules 03-13: word counts, money formatting, binary
search, an LRU cache, interval merging, a token bucket, a build order and
an event-sourced account.

## The two formats

### Parsons problems: arrange, don't type

The prompt lists every line of the solution, SCRAMBLED. You put them in
order (and indent them) inside the method. Some lines come in a marked
pair `(?)`: exactly one of the pair belongs, and the other is a mistake
real programmers make. Choosing between them is the lesson.

```
//      hi = mid - 1;              (?)
//      int lo = 0;
//      hi = mid;                  (?)
//      return lo;
```

How to solve one:

1. **Find the frame first.** Declarations go at the top, the `return`
   at the bottom, and every `{` needs its `}`.
2. **Ask what each line needs.** A line that uses `mid` comes after the
   line that defines it; a line that uses `last` must sit inside the
   loop.
3. **Decide each pair by a test case.** Run both candidates in your
   head on the smallest input that tells them apart (an empty list, a
   duplicate, an interval inside another).
4. **Then run the tests.** A wrong order usually fails a specific test,
   and its name tells you which decision to revisit.

Research on Parsons problems (Parsons and Haden, 2006; Ericson et al.,
2017) found they teach about as much as writing the code from scratch,
in less time, because you spend no effort on syntax and recall.

### Fill-in: complete the program

The program is finished except for a few holes. Each hole is written
`/* ___ */ hole()` with a comment that says what belongs there:

```java
long rest = /* ___ */ hole(); // the cents part, 0..99
```

`hole()` is a helper in the starter that throws the TODO marker, so an
unfilled hole shows up as ☐ todo. Replace the whole `/* ___ */ hole()`
with your expression; when every hole is filled, delete `hole()`.
Fill-ins teach READING code as much as writing it: you have to
understand the surrounding program to know what the hole must be.

How to solve one:

1. **Read the whole program before filling anything.** The holes are
   connected; the variables around a hole tell you its type and units.
2. **Fill one hole, run, repeat.** Tests go from todo to pass (or ✘)
   one hole at a time.
3. **Respect the comment's constraint.** "Look without touching the
   access order" or "never above capacity" is the part that is easy to
   get almost right.

## The details that bite

1. **`split` keeps a LEADING empty string**, so text that starts with a
   separator gives you an empty first token.
2. **`Map.merge(k, 1, Integer::sum)` counts; `put(k, 1)` resets.**
3. **In Java, `-5 % 100` is `-5`** (the remainder has the dividend's
   sign). Format money from the absolute value and add the sign once.
4. **Binary search bounds**: decide whether `hi` is included, and keep
   that invariant in every line.
5. **`LinkedHashMap(cap, load, true)` is access-ordered**, and in that
   mode `get` and `getOrDefault` count as an access; `containsKey`
   does not.
6. **Copy before you sort a parameter.** Sorting the caller's list is a
   side effect, and `List.of` lists cannot be sorted at all.
7. **Mind the units.** A clock in milliseconds and a rate per second
   meet in exactly one division by 1000.0, not 1000.
8. **Ties need a rule.** Without a PriorityQueue (or another explicit
   order), a topological sort's output depends on hash order.
9. **Records have no setters.** Each state change builds a new value.

## Exercises

| # | file | ★ | format | what you build |
| --- | --- | --- | --- | --- |
| 01 | `01-word-counts-parsons.java` | ★☆☆ | Parsons | case-insensitive word counts in alphabetical order |
| 02 | `02-bank-statement-fill-in.java` | ★☆☆ | fill-in | a bank statement from long cents with a running balance |
| 03 | `03-lower-bound-parsons.java` | ★★☆ | Parsons | binary search for the first index >= target |
| 04 | `04-lru-cache-fill-in.java` | ★★☆ | fill-in | an LRU cache on an access-ordered LinkedHashMap |
| 05 | `05-merge-intervals-parsons.java` | ★★☆ | Parsons | merging overlapping and touching intervals |
| 06 | `06-token-bucket-fill-in.java` | ★★☆ | fill-in | a token bucket with fractional refill on an injected clock |
| 07 | `07-build-order-parsons.java` | ★★★ | Parsons | a deterministic topological sort (Kahn) with cycle detection |
| 08 | `08-event-sourced-account-fill-in.java` | ★★★ | fill-in | an event-sourced account: decide, apply, replay |

Do the warm-ups and core in order. Stretch if time allows. When a
format feels easy, close the prompt and write the same method from a
blank file: that is the check that the idea stuck. Exercises 04 and 06
are the scaffolded versions of modules 09 and 10; module 21 has buggy
versions of both to hunt.

---

**Next:** `bootcamp/21-design-debugging`
