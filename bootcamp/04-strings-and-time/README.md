# 04 · Strings and Time

Text and dates are where "works on my machine" lives. A formatted price
that prints `2,50` in Berlin, a parsed date that quietly turns 31 February
into 29 February, a due date computed from `LocalDate.now()` that no test
can pin down — none of these crash. They just make the program wrong
somewhere else, later. This module is about making text and time
*deterministic*.

## The mental model

### 1 · Locale is an input — pass it

```java
String.format("%.2f", 2.5);                  // "2,50" on a German machine
String.format(Locale.ROOT, "%.2f", 2.5);     // "2.50" everywhere
"QUIT".toLowerCase(Locale.ROOT);             // never the Turkish dotless ı
DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
```

Anything machine-readable (files, logs, ids, tests) uses `Locale.ROOT` or
an explicit locale. Only human-facing text uses the user's locale — on
purpose, never by default.

### 2 · Pick the time type that matches the fact

| type | is | use it for |
| --- | --- | --- |
| `LocalDate` | a calendar date, no zone | due dates, birthdays, billing days |
| `LocalDateTime` | date + wall-clock time, no zone | rarely right on its own |
| `Instant` | a point on the global timeline | timestamps, logs |
| `ZonedDateTime` | instant + zone rules | "9am in Tokyo" |
| `Period` | years/months/days | "one month later" |
| `Duration` | exact seconds | timeouts, elapsed time |

All of them are **immutable**: `date.plusDays(1)` returns a new date and
leaves `date` alone.

### 3 · "Now" is a dependency

```java
LocalDate.now();          // untestable — the answer changes daily
LocalDate.now(clock);     // production: Clock.systemDefaultZone()
                          // tests: Clock.fixed(Instant.parse(...), zone)
```

### 4 · Parse strictly, format explicitly

```java
var IN = DateTimeFormatter.ofPattern("dd/MM/uuuu")
                          .withResolverStyle(ResolverStyle.STRICT);
```

## The details that bite

1. **`split` takes a regex.** `"a|b".split("|")` splits between every
   character. Escape: `split("\\|")`.
2. **`split` drops trailing empty strings** unless you pass a negative
   limit: `split("\\|", -1)`.
3. **`Matcher.find()` is not anchored**; `matches()` is. Validate with
   `matches`, scan with `find` and `\b`.
4. **The default resolver is SMART**: `31/02/2024` parses as
   `2024-02-29`. Use `ResolverStyle.STRICT` for user input.
5. **With STRICT, `yyyy` rejects every date.** `y` is year-of-era; use
   `uuuu`.
6. **`plusMonths` clamps, and chained clamps drift.** Compute each date
   in a schedule from the anchor: `first.plusMonths(i)`.
7. **`-5 / 100` is `0`**, and `0` has no sign: format negatives from the
   absolute value.
8. **`"%-10s"` pads but doesn't cut**; `"%-10.10s"` does both.

## Exercises

| # | file | ★ | what you build |
| --- | --- | --- | --- |
| 01 | `01-receipt-line.java` | ★☆☆ | cents as text, and a receipt line padded with dots |
| 02 | `02-split-and-parse.java` | ★★☆ | a `title|author|year` reader/writer that round-trips |
| 03 | `03-fixed-width-table.java` | ★☆☆ | report rows that look the same in every locale |
| 04 | `04-order-ids.java` | ★★☆ | validate, extract and scan order ids with regex |
| 05 | `05-due-dates.java` | ★☆☆ | due dates, overdue days, weekends with LocalDate |
| 06 | `06-strict-dates.java` | ★★☆ | parse typed dates strictly, label them in English |
| 07 | `07-injected-clock.java` | ★★☆ | late fees against an injected Clock, zones included |
| 08 | `08-billing-schedule.java` | ★★★ | month-end billing that never drifts, and Period text |

Do the warm-ups and core in order. Stretch if time allows.

---

**Next:** `bootcamp/05-classes-records-enums`
