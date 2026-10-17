# 21 · Design Debugging

The system-design block (modules 09-13) built caches, rate limiters,
retries, hash rings, outboxes, logs, breakers and idempotency stores
from scratch. This module hands you eight of them already written, as
they might look after a busy sprint, each with one to three bugs taken
from real incident reports. The code compiles, most tests pass, and the
prompt tells you what the component SHOULD do and what users saw when
it did not. As in module 19, it never says where the bug is.

Design bugs are different from the bugs in module 19. The code is
rarely "wrong" line by line; it breaks an INVARIANT ("only committed
work is visible", "nothing overtakes a failed message", "the cooldown
counts from opening") under one specific timeline: a crash at the wrong
moment, a client that polls faster than expected, two transactions that
overlap. Finding them is about building that timeline on purpose.

## The mental model

### 1 · Write the invariant down first

Before reading the code, turn the prompt into one or two sentences that
must ALWAYS hold:

| component | invariant |
| --- | --- |
| LRU cache | the victim is the key used longest ago, reads included |
| token bucket | tokens = min(capacity, earned - spent), whatever the call spacing |
| retry | at most N calls, each delay in [base, cap] |
| hash ring | a key belongs to the first point at or after it, wrapping |
| outbox | marked sent ⇒ the broker accepted it; order is never inverted |
| write-ahead log | visible ⇔ its COMMIT record was read |
| circuit breaker | open ⇒ rejected until cooldown after OPENING |
| idempotency store | same (client, key) ⇒ same final answer; temporary errors are not final |

A failing test is a counterexample to the invariant. Your job is to find
the line that lets it happen.

### 2 · Build the timeline that breaks it

These bugs need a specific sequence of events. The tests give you one;
shrink it to the shortest, then write it as a table. Here, for a session
that should expire 30 minutes after its LAST request:

```
t (min)  event     lastSeen  expires at   expected
   0     request   0         30
  20     request   0         30           lastSeen 20, expires at 50   ← first disagreement
  35     request   -         expired      still logged in
```

The row where your expectation and the code disagree is where to look.

### 3 · Fakes make the timeline reproducible

Every component here takes its clock, sleeper, hash function or broker
as a parameter, so a test can script time, failures and placement
exactly. That is what makes a "once in a while in production" bug
reproducible on the first run. When the real component does not allow
this, that is the first fix: inject the dependency, then hunt.

### 4 · One bug often hides another

Fix the first bug, run everything, and expect a different test to fail.
A retry that never stops hides the fact that it also stops one call too
late; an outbox that loses messages hides the fact that it also
reorders them. You are done when ALL tests pass, not when the first
report is explained.

## The bug catalogue

The families these hunts come from, and where the track built the
correct version:

| family | typical slip | module |
| --- | --- | --- |
| recency and eviction | the wrong end of a list, reads that do not count | 09 |
| rates and time | integer division of elapsed time, no cap on a credit | 10 |
| loops that must end | a counter reset inside the loop, `>` vs `>=`, shift overflow | 10 |
| placement | `higher` vs `ceiling`, wrapping to the wrong end | 11 |
| delivery | marking before the side effect, continuing past a failure | 12 |
| durability | applying work without its commit, "flushing" at the end | 13 |
| state machines | a timer reset by the wrong transition, a counter never reset | 10 |
| keys | concatenated composite keys, caching temporary failures | 10, 11 |

## The details that bite

These are not mapped to exercises on purpose: in a hunt, recognising
which one applies is the work.

1. **A side effect and its bookkeeping have an order.** Record "done"
   only after the thing is done; otherwise a crash or exception between
   them loses work.
2. **Integer division of a duration truncates**, and moving `last`
   forward throws the remainder away for good.
3. **Every credit, delay and count needs an upper bound.**
4. **State that must persist across iterations is declared outside the
   loop.**
5. **`1L << n` is not `2^n` for large n**: bits fall off, and the shift
   distance wraps modulo 64.
6. **A ring has no end.** "Nothing after this point" means "the first
   point".
7. **`higherEntry` is `>`, `ceilingEntry` is `>=`.** Decide which your
   rule says, once, and use it everywhere.
8. **A timer measures from the event the spec names.** Resetting it on
   other events can stop it from ever firing.
9. **"A success resets the count" is a transition**, and every
   transition needs code and a test.
10. **`a + b` is not a unique key for the pair (a, b).** Use a record.
11. **Never cache a temporary failure as a final answer.**

## Exercises

| # | file | ★ | the report |
| --- | --- | --- | --- |
| 01 | `01-lru-wrong-victim.java` | ★☆☆ | the page just loaded is evicted first; the hottest key keeps disappearing |
| 02 | `02-bucket-never-refills.java` | ★★☆ | fast clients are locked out forever; an idle client bursts thousands |
| 03 | `03-retry-never-stops.java` | ★★☆ | a checkout retried until the pod died; delays went negative |
| 04 | `04-ring-wraparound.java` | ★★☆ | one node gets far more keys; router and rebalancer disagree |
| 05 | `05-outbox-lost-message.java` | ★★☆ | a message was never delivered; events arrived out of order |
| 06 | `06-wal-ghost-writes.java` | ★★★ | after a crash, uncommitted and aborted writes are visible |
| 07 | `07-breaker-stuck-open.java` | ★★☆ | the breaker stayed open for an hour; one error re-opens it |
| 08 | `08-idempotency-crossed-keys.java` | ★★★ | a customer got someone else's receipt; retries replay a 503 |

Do the warm-ups and core in order. Stretch if time allows. If a hunt
stalls, go back to the module that built the component correctly and
compare the two line by line: the diff is usually short.

---

**Next:** back to `astraupskill/03-SYSTEM-DESIGN.md`, and design the
whole system these components live in.
