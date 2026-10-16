# 12 · Messaging and Events

The moment a system has two services, it has messages: "order placed",
"payment failed", "send the reminder in an hour". Messaging is how
services stay decoupled (the sender doesn't know who listens), absorb
bursts (a queue soaks up a spike) and survive each other's outages (the
message waits). It is also where most production incidents in
distributed systems come from: duplicates, lost events, poison
messages, retry storms and half-finished workflows. This module builds
the patterns that keep all of that under control, small enough to hold
in your head and exact enough to test.

## The mental model

### 1 · Delivery guarantees are a choice between duplicates and loss

A network can't tell "the consumer crashed before acking" from "the
ack got lost". So every broker picks one side:

| guarantee | what can go wrong | how it's built | you add |
| --- | --- | --- | --- |
| at-most-once | messages lost | ack before processing | nothing (accept loss) |
| at-least-once | messages duplicated | ack after processing; redeliver on doubt | an **idempotent consumer** |
| "exactly-once" | nothing, *inside one system* | at-least-once + dedupe, or a transaction spanning offset and effect | dedupe or transactions |

SQS standard queues, Google Pub/Sub, RabbitMQ with manual acks and a
Kafka consumer that commits offsets after processing are all
at-least-once.
"Exactly-once" in practice means exactly-once **effect**:

```java
if (seen.contains(msg.id())) return "duplicate";   // 1 check
effect.accept(msg);                                // 2 do it (may throw)
seen.add(msg.id());                                // 3 only now remember
```

### 2 · Queues need a limit and a policy

A queue between a fast producer and a slow consumer either grows
without bound (the outage just arrives later, as an OOM) or makes a
decision when full:

```
                 ┌─────────── capacity ───────────┐
producer ──▶ offer │ m1 │ m2 │ m3 │ m4 │ m5 │ m6 │ poll ──▶ consumer
                 └────────────────────────────────┘
full?  REJECT       → producer told "not now" (backpressure, nothing lost)
       DROP_OLDEST  → head discarded (freshest data wins: telemetry, prices)
       DROP_NEWEST  → new message discarded (protect accepted work)
```

Failures need a policy too: a message that fails goes to the **back**
of the queue (so it can't block the others), and after N attempts to a
**dead-letter queue** for a human to inspect and redrive.

### 3 · Never dual-write: the outbox

"Save the order, then publish OrderPlaced" loses the event if the
process dies in between; "publish, then save" announces orders that
don't exist. The fix is to make it one write:

```
BEGIN
  INSERT INTO orders  (id, cents)          VALUES ('o1', 500);
  INSERT INTO outbox  (seq, event, sent)   VALUES (41, 'OrderPlaced o1 500', false);
COMMIT
                         relay: SELECT ... WHERE NOT sent ORDER BY seq
                                publish(event); then UPDATE outbox SET sent = true
```

Publish-then-mark makes the relay at-least-once, which the consumers'
dedupe absorbs. Mark-then-publish is at-most-once, the bug you started
with.

### 4 · Events as the source of truth

Event sourcing stores the facts (`Opened`, `Deposited`, `Withdrawn`) and
derives state by folding them. Writes use **optimistic concurrency**:
"append these events if the stream is still at version 3". Reads use
**projections**: separate state built by replaying the log, which can
be dropped and rebuilt any time.

```java
Account state = null;
for (var e : events) state = apply(state, e);       // replay
store.append(id, state.version(), List.of(new Withdrawn(200)));  // or conflict
```

### 5 · Long workflows: sagas, not distributed transactions

A trip booking touches flights, hotels and cars, each with its own
database. A saga runs each local step, and on failure runs the
**compensations** of the steps that succeeded, newest first:

```
do flight ✓ → do hotel ✓ → do car ✗ "sold out"
                          undo hotel ✓ → undo flight ✓      = COMPENSATED
```

Compensations can fail, so they're retried (and must be idempotent);
when retries run out, the saga is **STUCK** and says so.

## The details that bite

1. **Remember a message id only after the effect succeeded.**
   Remembering first turns a crash into a message silently skipped on
   redelivery (exercise 03).
2. **A dedupe window must outlast the redelivery horizon.** An id that
   has aged out is applied again; size the window from the broker's
   redelivery behaviour, not from memory you'd like to save.
3. **Unsubscribe by subscription, not by handler.** The same handler
   can be subscribed twice; removing "the first equal handler" removes
   the wrong one (exercise 01).
4. **Isolate subscribers.** One throwing handler must not stop delivery
   to the rest; count the failure, keep going.
5. **Off by one on attempts.** With `maxAttempts = 3` the handler runs
   three times; the third failure dead-letters. Count attempts per
   queued *entry*, not per message text (exercise 04).
6. **Keep the last error, reset the count on redrive.** The last error
   is the one that gave up; a redriven message with an old count dies
   on the first blip.
7. **Stop the relay at the first failed publish.** Skipping ahead lets
   `OrderShipped` overtake `OrderPlaced` downstream (exercise 05).
8. **Fixed rate, not fixed delay, and no catch-up burst.** `next =
   runAt + period` keeps a timer anchored; then skip slots already in
   the past so a late timer runs once, not ten times (exercise 06).
9. **Optimistic concurrency needs the version you READ.** Appending
   with "whatever the version is now" defeats the check; two
   withdrawals of the last 100 both succeed (exercise 07).
10. **Don't compensate the step that failed, and don't stop unwinding
    when one compensation gives up** (exercise 08).

## Cheat table

| problem | pattern | real-world names |
| --- | --- | --- |
| tell many services something happened | pub/sub topic | SNS, Kafka topics, RabbitMQ topic exchanges, Redis PUBLISH |
| producer faster than consumer | bounded queue + overflow policy | BlockingQueue, Reactor `onBackpressure*`, Akka `OverflowStrategy` |
| broker delivers twice | idempotent consumer / inbox table | message-id dedupe, a transactional inbox table |
| a message always fails | retry to the back, then DLQ | SQS `maxReceiveCount` + redrive, RabbitMQ DLX |
| DB write + publish must agree | transactional outbox + relay | Debezium CDC, polling publisher |
| "do this later" | delay queue / scheduler | SQS `DelaySeconds`, Sidekiq scheduled set, `ScheduledExecutorService` |
| audit trail, rebuildable read models | event sourcing + projections | EventStoreDB, Axon, ledgers |
| multi-service workflow | saga with compensations | Temporal, AWS Step Functions |

## How this connects

- **10-rate-limits-and-resilience**: retries with backoff and jitter,
  circuit breakers and idempotency keys are the producer side of the
  same problems; a DLQ is what's left when retries run out.
- **11-partitioning-and-ids**: Kafka orders messages per partition and
  picks the partition by hashing the key, so "all events for order 17
  in order" is a partitioning decision. Snowflake ids make good message
  ids for dedupe.
- **13-storage-and-consistency**: the outbox leans on a local
  transaction, event sourcing is a write-ahead log you keep forever,
  and a saga is the alternative to the two-phase commit coordinator
  built there.

## Exercises

| # | file | ★ | what you build |
| --- | --- | --- | --- |
| 01 | `01-pubsub-bus.java` | ★☆☆ | topics, wildcards, isolated subscribers, unsubscribe |
| 02 | `02-bounded-queue.java` | ★☆☆ | backpressure with REJECT / DROP_OLDEST / DROP_NEWEST |
| 03 | `03-dedupe-consumer.java` | ★★☆ | an idempotent consumer with a bounded dedupe window |
| 04 | `04-dead-letter-queue.java` | ★★☆ | retries to the back, dead letters, redrive |
| 05 | `05-transactional-outbox.java` | ★★☆ | orders + outbox in one unit, an ordered relay |
| 06 | `06-delay-scheduler.java` | ★★☆ | delayed and recurring jobs with priorities and a fake clock |
| 07 | `07-event-sourcing.java` | ★★★ | replay, optimistic appends, commands, a projection |
| 08 | `08-saga-compensation.java` | ★★★ | a saga that unwinds, retries undos, and admits STUCK |

Do the warm-ups and core in order. Stretch if time allows.

---

**Next:** `bootcamp/13-storage-and-consistency`
