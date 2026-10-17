# 19 · Debugging

Most of a working programmer's week is not writing new code. It is
finding out why existing code does the wrong thing, usually code that
passed its tests, shipped, and broke for one customer. This module flips
the exercises around: every starter is COMPLETE, compiles and mostly
works, and carries one to three real bugs of the kind modules 01-08
warned you about. Your job is the one you will do at work: read the bug
report, reproduce it, find the line, fix it, and leave a test behind so
it never comes back.

## How a debug hunt works

- The starter runs. Some tests pass, some fail (✘). The failing test
  names and the bug reports in the prompt are your symptoms.
- The prompt states the INTENDED behaviour. It never says where the bug
  is.
- You are done when every test passes. Do not edit the tests; the
  grader uses its own copy anyway.
- A hunt can hold more than one bug. Fixing one may turn some tests
  green and leave others red. Keep going until all are green.

## The mental model

### 1 · Reproduce: make it fail on purpose

A bug you cannot trigger is a bug you cannot fix, or know you have
fixed. Turn the report into the SMALLEST call that shows it:

```
"Our biggest client's total is negative"
   → totalCents(three orders of 1_500_000_000)   // fails every time
```

The failing tests are reproductions someone already wrote for you. Read
the expected and received values: the DIFFERENCE is a clue. Off by one
page? Off by exactly 2^32? Rounded the other way? Each points at a
different family of bug.

### 2 · Narrow: halve the search space

Between the input and the wrong output there are a few steps. Check the
value after each one (a print, or a debugger breakpoint) until you find
the first step whose output is wrong. That step holds the bug.

```
"A Gold member's 100.00 order came to -900.00"
  tierOf(customer)        = GOLD     ok
  discountFor(GOLD)       = 10       ok: a percent, as documented
  price * (1 - discount)  = -900     <- first wrong value: the bug is here
```

Ask "what is different between the case that works and the one that
fails?" One customer versus another, a short name versus a long one, a
weekday versus a weekend: the difference is usually the bug's name.

### 3 · Fix the cause, not the symptom

Fix it where the wrong value is born. If a missing name crashes three
screens, validate it where the object is built (one place), not in every
screen. If one half of a pair is wrong (`encode`/`decode`,
`open`/`close`), check its partner too. After the fix, run ALL the
tests: a fix that turns one red test green and another green test red
was a guess.

### 4 · Pin it with a test

The bug got in because no test covered that case. Add one, named after
the behaviour ("a partial last page counts as a page"), not the bug.
In this module the tests are already there; in real life, write the
failing test FIRST, watch it fail, then fix.

## The bug catalogue

The usual suspects, and where the track taught each one:

| symptom | likely bug | module |
| --- | --- | --- |
| a big number turns negative | `int` overflow before widening | 01 |
| a fraction disappears | integer division (`long / int`) | 01 |
| works for small ids, fails for big ones | `==` on `Integer` (cache -128..127) | 01 |
| works for literals, fails for parsed text | `==` on `String` | 01 |
| one page, item or day missing or extra | off-by-one: 0- vs 1-based, `<` vs `<=`, round down vs up | 01, 03 |
| every user shares a change | aliasing a shared mutable list | 03, 05 |
| one of two neighbours is skipped | `remove(i)` inside an index loop | 03 |
| the wrong element disappears | `List<Integer>.remove(int)` removes by index | 03 |
| the set holds duplicates | `equals` overloaded not overridden, or `hashCode` disagrees | 05 |
| 0.00 is not zero; 2.5 and 2.50 differ | `BigDecimal.equals` compares scale | 07 |
| off by one cent on halves | wrong `RoundingMode` | 07 |
| events land on the wrong day | converting in UTC instead of the user's zone | 04 |
| an hour off twice a year | `plusHours(24)` instead of `plusDays(1)` | 04 |
| wrong year in the last days of December | `YYYY` (week-based year) instead of `yyyy` | 04 |

## The details that bite

These are the habits that find bugs fastest. They are deliberately not
mapped to exercises: in a hunt, finding out WHICH one applies is the work.

1. **A wider return type does not widen the arithmetic inside.**
   `long f() { int t = ...; return t; }` overflows in `t`.
2. **Write out the indices for the first page, the last partial page and
   an exact multiple.** Most off-by-ones fail one of the three.
3. **Tests built only from literals and small numbers hide `==` bugs.**
   Use parsed input and numbers above 127.
4. **A reference is not a copy.** Copy what you store, and return copies
   or unmodifiable snapshots of what you own.
5. **`@Override` on every `equals`.** Without it, `equals(MyType)` is a
   silent overload that collections never call.
6. **Equal objects must have equal hash codes**, including for
   case-insensitive equality.
7. **Use `removeIf` (or an `Iterator`) to remove while iterating**.
8. **`list.remove(3)` on a `List<Integer>` removes index 3**.
9. **`BigDecimal`: compare with `compareTo`**, and know which rounding
   mode the business wants.
10. **A parameter that is never used is a bug report waiting to happen**.
11. **Time bugs need dates that break them**: an evening in a western
    zone, a DST weekend, 30 December.

## Exercises

| # | file | ★ | the report |
| --- | --- | --- | --- |
| 01 | `01-order-stats.java` | ★☆☆ | a negative monthly total and an average that lost its fraction |
| 02 | `02-pagination.java` | ★☆☆ | page 1 shows the second page; a partial last page is missing |
| 03 | `03-customer-lookup.java` | ★☆☆ | right in staging, zero orders in production |
| 04 | `04-shared-tags.java` | ★★☆ | one user tagged VIP, every new user born VIP |
| 05 | `05-email-set.java` | ★★☆ | the newsletter went out twice; unsubscribe does not find the address |
| 06 | `06-session-cleanup.java` | ★★☆ | expired sessions survive; logout removes the wrong user |
| 07 | `07-invoice-money.java` | ★★☆ | one cent off on tax; a free order goes to checkout |
| 08 | `08-daily-reminders.java` | ★★★ | orders on the wrong day, a reminder an hour late, the wrong year |

Do the warm-ups and core in order. Stretch if time allows. Before you
change a line, say out loud (or write in a comment) what you think the
bug is and which test will turn green. Being wrong about that is the
most useful part of the exercise. Module 21 runs the same kind of hunt
on system-design code: caches, rate limiters, retries and logs.

---

**Next:** `bootcamp/20-scaffolded`
