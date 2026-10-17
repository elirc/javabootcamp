# 17 · Deterministic Concurrency

Concurrency bugs are the ones that pass every test on your laptop and
corrupt a balance in production once a week. They depend on timing:
which thread ran between someone else's read and write. A test that
starts threads and hopes to hit the bad timing is a test that passes by
luck. This module teaches the ideas (lost updates, atomics, immutable
snapshots, executors, futures, queues, deadlocks) **and** the habit
that makes concurrent code testable: separate the logic from the
scheduling, then drive the scheduling yourself. Every test here gives
the same answer on every run and finishes well under a second.

## The mental model

### 1 · A race is an interleaving you did not plan for

```
counter = 0        A: read 0          B: read 0
                   A: write 1         B: write 1      → counter = 1, one increment lost
```

`counter++` is read, add, write. Any step of another thread can land
between them. With two workers doing two increments each there are 70
interleavings, and only 6 give the right answer (exercise 01 counts
them). That is why "it passed 50 times" proves nothing.

### 2 · Three ways to be safe, from simplest to hardest

| approach | how | used for | exercises |
| --- | --- | --- | --- |
| don't share mutable state | immutable snapshots, swap one reference | config, prices, routing tables | 03 |
| make each update one atomic step | `AtomicInteger`, `LongAdder`, CAS loops, `ConcurrentHashMap.merge/compute` | counters, stock, caches | 01, 02, 04 |
| hand work between threads through a safe channel | executors, `CompletableFuture`, `BlockingQueue` | pipelines, fan-out/fan-in | 05, 06, 07 |
| locks | `synchronized`, `ReentrantLock`, always in one global order | multi-object invariants | 08 |

Prefer the higher rows. Every lock you add is a lock that can be taken
in the wrong order.

### 3 · Compare-and-set is the primitive under the atomics

```java
while (true) {
    int now = left.get();
    if (now == 0) return false;                     // the condition
    if (left.compareAndSet(now, now - 1)) return true;  // only if nobody moved first
}                                                   // someone did: re-read, retry
```

`updateAndGet` and `accumulateAndGet` are this loop, written for you
(`incrementAndGet` too, in effect), and the function you pass may run
more than once, so it must have no side effects.
`ConcurrentHashMap.compute`, `merge` and `computeIfAbsent` work
differently: they lock that key's bin and run the function once, so
keep the function short and never touch the same map from inside it.

### 4 · Make the scheduler a parameter

```
production:  new EmailBatcher(Executors.newFixedThreadPool(8), smtp)
test:        new EmailBatcher(manualExecutor, fakeMailer)   // nothing runs until runAll()
test:        new EmailBatcher(Runnable::run, fakeMailer)    // everything runs inline
```

The same goes for futures (complete them by hand in the test), for
time (inject a `Clock`, module 04 and the system-design block), and for
the interleaving itself: replay a schedule string (exercises 01 and 07),
or hand the code a rival that acts between its read and its write
(exercises 02 to 04), so a race fails on every run, not once a week.
Real threads appear in a few tests here only where the RESULT is
independent of the order: exact totals, FIFO output from a single
consumer, results collected by index.

## The details that bite

1. **Read-modify-write is not atomic.** `x++`, `map.put(k, map.get(k) + 1)`
   and `if (get() > 0) decrement()` all lose updates (exercises 01, 02, 04).
2. **A failed CAS must re-read.** Retrying from the stale value makes
   the same mistake again (exercise 01).
3. **`getAndDecrement() > 0` answers right and leaves the count
   negative** (exercise 02).
4. **"Keep the max" is a read-modify-write too.** Use
   `accumulateAndGet(v, Math::max)` (exercise 02).
5. **`Map.copyOf` of a map of lists shares the lists.** Immutable means
   all the way down (exercise 03).
6. **`getAndUpdate` returns the OLD value; `updateAndGet` the new**
   (exercise 03).
7. **A remapping function returning `null` removes the key;**
   `computeIfPresent` never creates one (exercise 04).
8. **`computeIfAbsent` does not cache `null`** (exercise 04).
9. **A test executor must be FIFO, survive failing tasks and refuse to
   spin forever** (exercise 05).
10. **`Future.get` wraps the task's exception; report `getCause()`**
    (exercises 05, 06).
11. **`allOf` is not fail-fast, and `anyOf` is not first-success**
    (exercise 06).
12. **A producer whose offer failed must keep the item,** and an idle
    producer is not waiting (exercise 07).
13. **Stop a consumer with a poison pill that cannot be real data**
    (exercise 07).
14. **Only locks still held create ordering edges, and reentrant
    acquisition is not a self-deadlock** (exercise 08).

## Cheat table

| want | write |
| --- | --- |
| count from many threads | `LongAdder.increment()`, read with `sum()` |
| conditional atomic update | CAS loop on `AtomicInteger`, or `updateAndGet` with a pure function |
| add to a map entry | `map.merge(k, n, Integer::sum)` |
| update-or-remove a map entry | `map.computeIfPresent(k, (k, v) -> ... null to remove)` |
| load once per key | `map.computeIfAbsent(k, loader)` |
| publish a new immutable snapshot | `AtomicReference.updateAndGet(old -> old.withX(...))` |
| join two async results | `a.thenCombine(b, (x, y) -> ...)` |
| failure as a default value | `f.exceptionally(e -> fallback)` |
| bounded hand-off between threads | `ArrayBlockingQueue.put` / `take` + a poison pill |
| never deadlock on two locks | acquire in one global order (sorted ids) |

## Exercises

| # | file | ★ | what you build |
| --- | --- | --- | --- |
| 01 | `01-lost-updates.java` | ★☆☆ | replayable interleavings: lost updates, CAS retries, outcome counts |
| 02 | `02-atomic-counters.java` | ★☆☆ | lock-free request metrics and a ticket pool with CAS loops |
| 03 | `03-immutable-snapshots.java` | ★☆☆ | a deeply immutable config published through an `AtomicReference` |
| 04 | `04-concurrent-map-ops.java` | ★★☆ | stock reservations with `merge`/`computeIfPresent`, a `computeIfAbsent` loader |
| 05 | `05-manual-executor.java` | ★★☆ | a test executor you step by hand, and code that takes an `Executor` |
| 06 | `06-completable-futures.java` | ★★☆ | quote, fail-fast all, first-success and a deadline, completed by hand |
| 07 | `07-producer-consumer.java` | ★★☆ | a steppable bounded-buffer model, then a real `BlockingQueue` pipeline |
| 08 | `08-deadlock-detector.java` | ★★★ | lock-order graphs from traces, cycle finding, and a safe global lock order |

Do the warm-ups and core in order. Stretch if time allows. Exercise 04
is module 09's single-flight cache loader in one line, 07 is module 12's
bounded queue with real threads, and 08's "lock in one global order" is
the standard fix for the transfer deadlock between two bank accounts.

---

**Next:** `bootcamp/18-parsing-and-serialization`
