# System design: from building blocks to whole systems

Modules 09–13 each build small, exact models of one family of ideas:
caches, rate limits and failure handling, partitioning and ids, messaging,
and storage. Real systems are never one of those ideas. A URL shortener is
an id scheme, a cache, a partitioned store and a rate limiter wearing one
trench coat. This chapter is about the trench coat: how to approach a
design question, how the blocks combine into the classic designs, how to
size them on the back of an envelope, and which trade-off you are making
each time you choose one block over another.

It contains no exercise answers. It points at the exercises that let you
feel each idea under your fingers; do them, then come back and reread the
design that uses them.

Read it once before module 09 for the map, and again after module 13,
when every box in the diagrams will be something you have built.

---

## 1 · How to approach a design question

A design question ("design a ticket booking system") is not a request for
the one right diagram. It is a request to make good decisions in a
sensible order and to say out loud what each decision costs. The order
that works, in an interview or at a whiteboard with your team:

| step | question you are answering | output | typical time (45 min) |
| --- | --- | --- | --- |
| 1 · scope | what must it do; what is explicitly out? | 3–5 functional requirements | 5 min |
| 2 · qualities | how fast, how big, how available, how consistent? | numbers and adjectives you can design against | 3 min |
| 3 · estimate | how many requests, how much data, how many machines? | QPS (peak), storage per year, bandwidth | 5 min |
| 4 · interface | what does a client call? | 3–6 API calls with inputs and outputs | 4 min |
| 5 · data | what is stored, keyed by what, read by which query? | entities, keys, access patterns | 5 min |
| 6 · sketch | which boxes and arrows serve the happy path? | one diagram | 8 min |
| 7 · deep dive | where does it break first? | the bottleneck and its fix, with the trade-off | 12 min |
| 8 · failure | what happens when each box dies or is slow? | timeouts, retries, idempotency, degradation | 3 min |

Some habits that separate a good answer from a list of buzzwords:

- **Requirements decide the design.** "Exactly one person gets seat 14C"
  and "the like count can be a few seconds stale" lead to completely
  different storage choices. Ask, then write the answers down.
- **Name the access pattern before the database.** "Look up by short code,
  10,000 times a second, almost never written" points at a key-value store
  and a cache; "list a user's bookings by date" points at an index or a
  range partition. The database is a consequence, not a starting point.
- **Draw the happy path first, then attack it.** One request, end to end.
  Then ask: what if this is 100 times bigger? What if this box is slow?
  What if this message arrives twice?
- **Every box you add is a trade.** A cache buys latency and costs
  staleness. A queue buys decoupling and costs ordering and immediacy. A
  second replica buys availability and costs consistency. Say which side
  of the trade the requirements put you on.
- **Prefer boring.** One relational database with a read replica and a
  cache handles more than most products ever need. Reach for sharding,
  event sourcing or consensus when the numbers force you, and show the
  numbers.

---

## 2 · Back-of-the-envelope estimation

Estimation is not about precision. It is about the order of magnitude:
does this fit on one machine, or do we need forty?

### Numbers worth remembering (rough, order of magnitude)

| thing | approximate cost |
| --- | --- |
| main memory reference | 100 ns |
| read 1 MB sequentially from memory | 10 µs |
| SSD random read | 100 µs |
| read 1 MB sequentially from SSD | 0.5–1 ms |
| round trip inside one data centre | 0.5 ms |
| disk seek (spinning) | 5–10 ms |
| round trip across a continent | 50–80 ms |
| round trip across the world | 150 ms |

| conversion | value |
| --- | --- |
| seconds in a day | 86,400 ≈ 10^5 |
| requests per day → per second | divide by 10^5 |
| 1 million requests a day | ≈ 12 per second |
| 1 billion requests a day | ≈ 12,000 per second |
| peak / average | assume 2–3x (more for spiky products) |
| one modest app server | 1,000–10,000 simple requests a second |
| one relational primary | thousands of writes, tens of thousands of indexed reads a second |
| one Redis node | ~100,000 simple operations a second |

### A worked estimate

"A link shortener with 100 million new links a month, read 100 times more
often than written."

```
writes: 100M / month  ≈ 100M / (30 × 10^5 s) ≈ 40 / s     peak ≈ 100 / s
reads:  100 × writes                       ≈ 4,000 / s   peak ≈ 10,000 / s
row:    code 7 B + url ~100 B + metadata ~100 B ≈ 250 B
data:   100M × 12 × 250 B ≈ 300 GB / year           → 5 years ≈ 1.5 TB
codes:  62^7 ≈ 3.5 × 10^12 possible codes            → never runs out
cache:  reads are skewed (a small share of links gets most clicks);
        size the cache in hot LINKS, not reads:
        ~4 million hot links × 250 B ≈ 1 GB    → fits in one cache node
```

Conclusions you can now defend: writes are trivial for one database;
reads want a cache in front; storage after five years is large for one
disk but easy to partition by code; 7 base62 characters are plenty.

### Little's law

**Concurrency = throughput × latency.** A service answering 2,000
requests a second at 50 ms each holds about 100 requests in flight. That
is how many threads, connections or permits you need, and it is why a
slow dependency is dangerous: if its latency goes from 50 ms to 5 s, the
same throughput needs 10,000 slots, and a server with 200 threads stops
answering everything. Bulkheads and timeouts (module 10) exist because
of this one multiplication.

---

## 3 · The building blocks

Each module in the system-design block is a toolbox. Here is what each
tool buys, what it costs, and the knob you will be asked about.

### Caching (module 09)

```
client ──► app ──► cache ──(miss)──► database
                    ▲                    │
                    └────── fill ────────┘
```

| strategy | how it works | buys | costs |
| --- | --- | --- | --- |
| cache-aside | app reads cache, on miss reads DB and fills | simple; cache failure is survivable | first read is slow; stale until TTL or invalidation |
| read-through | the cache itself loads misses | one code path for loading | the cache must know the DB |
| write-through | write cache and DB together | reads after writes are fresh | every write pays both |
| write-behind | write cache, flush to DB later | very fast writes | data loss if the cache dies first |

Knobs: **eviction** (LRU keeps recent, LFU keeps popular), **TTL**
(staleness bound), **negative caching** (remember "not found" so a
missing key does not hammer the DB), **stampede protection** (when a hot
key expires, let one caller reload while the rest wait for its result).
The hard part is invalidation: decide whether stale data for N seconds is
acceptable, and write that number down.

### Rate limits and resilience (module 10)

| tool | protects against | knob |
| --- | --- | --- |
| token bucket | bursts beyond an average rate | capacity (burst), refill rate (average) |
| fixed / sliding window | more than N requests per period | window size; boundary behaviour |
| retry with backoff + jitter | transient failures | max attempts, base delay, jitter range |
| circuit breaker | hammering a dependency that is down | failure threshold, open duration, half-open probe |
| bulkhead | one slow dependency eating every thread | permits per dependency |
| timeout / deadline | waiting forever | budget per call, propagated downstream |
| idempotency key | a retried request doing the work twice | key lifetime, what is stored with it |

These compose in layers. One common arrangement on an outbound call is
**deadline → retry (with backoff) → circuit breaker → bulkhead → the
call**, so every attempt passes the breaker and, once it opens, each
retry fails fast instead of reaching the dependency (libraries differ;
choose the order on purpose and say why). On the server side: **rate
limit → idempotency check → work**. Retries without idempotency turn
one payment into two; retries without backoff and jitter turn a blip
into a self-inflicted outage.

### Partitioning and ids (module 11)

| technique | where a key goes | moving to N+1 nodes moves |
| --- | --- | --- |
| hash mod N | `hash(key) % N` | almost every key |
| consistent hashing (ring + virtual nodes) | next node clockwise on a ring | about 1/(N+1) of keys |
| rendezvous (highest random weight) | the node with the top score for this key | about 1/(N+1) of keys |
| range partitioning | the node owning that key range | one range split at a time |

Range partitioning keeps neighbours together (good for "all of a user's
orders this month") but invites hot spots (all of today's writes hit the
newest range). Hashing spreads load but makes range queries scatter
across every node.

Ids: **snowflake-style ids** (time bits + machine bits + sequence bits)
are unique without coordination and roughly time-ordered; **base62
codes** make short, URL-safe strings; random ids avoid guessability but
lose ordering. **Bloom filters** answer "definitely not here" cheaply,
which lets an LSM tree or a cache skip pointless lookups.
**Quorums**: with N replicas, writes acknowledged by W and reads from R,
`R + W > N` guarantees a read overlaps the latest write.

### Messaging and events (module 12)

| tool | what it gives you | the catch |
| --- | --- | --- |
| pub/sub topic | one event, many independent consumers | consumers must keep up or lag |
| at-least-once + dedupe | no lost messages | duplicates are normal; consumers must be idempotent |
| bounded queue + backpressure | a producer cannot drown a consumer | someone must wait or drop |
| transactional outbox | DB change and event published "together" | a relay process and at-least-once delivery |
| dead-letter queue | a poison message stops blocking the rest | someone must look at the DLQ |
| event sourcing + projections | the full history; rebuildable read models | schema evolution, replay time |
| saga with compensation | a multi-service "transaction" without locks | intermediate states are visible; compensations must exist |
| delay / priority scheduling | "do this later", "do this first" | starvation of low priority |

"Exactly once" delivery does not exist across a network. What exists is
at-least-once delivery plus idempotent processing, which is
indistinguishable from exactly-once to the outside world.

### Storage and consistency (module 13)

| tool | answers | real-world home |
| --- | --- | --- |
| write-ahead log | "what survives a crash?" | PostgreSQL WAL, Kafka |
| LSM tree | "how do I take a flood of writes?" | RocksDB, Cassandra |
| optimistic locking | "two editors, one row" | JPA `@Version`, ETags, DynamoDB conditions |
| MVCC | "readers and writers without blocking" | PostgreSQL, InnoDB |
| CRDTs / LWW | "replicas merged after a partition" | Riak, collaborative editing |
| vector clocks | "which write saw which?" | Dynamo, Riak |
| lease + fencing token | "exactly one leader, safely" | etcd, ZooKeeper |
| two-phase commit | "commit in two databases or neither" | XA, Spanner |

---

## 4 · Classic designs

Each design below lists the requirements that shape it, a sketch, the
blocks it is made of, and the deep-dive questions that come up. The
module numbers tell you where you built each block.

### 4.1 · URL shortener

**Requirements.** Create a short code for a long URL; redirect a code to
its URL fast; optionally custom aliases and expiry. Read-heavy (100:1),
latency-sensitive on redirects, tolerant of a few seconds of staleness.

```
POST /links {url}        ──► API ──► id generator ──► base62 ──► KV store (partitioned by code)
GET  /{code}  ──► edge/CDN ──► API ──► cache ──(miss)──► KV store
                                          │
                                          └─ "not found" cached too
```

**Blocks.** Unique ids without a central counter (11: snowflake-style
ids); turning a number into a short code (11: base62); a cache in front
of the store with a TTL and negative caching so random probing of bad
codes cannot hammer the database (09); a partitioned key-value store
keyed by code (11: consistent hashing); a rate limit on link creation
per user or IP to stop abuse (10).

**Deep dive.**
- *Counter vs random vs hash of the URL.* A counter encoded in base62 is
  short and collision-free but guessable and needs coordination (a
  snowflake id or pre-allocated ranges per server removes that). Random
  codes are unguessable but need a collision check. Hashing the URL
  gives the same code for the same URL (dedupe for free) but needs
  collision handling and leaks "this URL was already shortened".
- *301 vs 302.* A permanent redirect is cached by browsers (fewer hits,
  no click analytics); a temporary one comes back to you every time.
- *Hot links.* One viral link can be millions of reads a minute: a cache
  and a CDN absorb it; stampede protection stops the expiry moment from
  becoming a database spike (09).
- *Click analytics* belong on an asynchronous path: publish a click
  event and count it later (12), never on the redirect's critical path.

### 4.2 · Rate-limited public API

**Requirements.** Each API key gets, say, 100 requests a minute with
short bursts allowed; limits hold across many API servers; clients get a
clear `429` with a retry hint; the limiter must not become the outage.

```
client ──► load balancer ──► API server ──► limiter check ──► handler ──► dependencies
                                   │             │                              │
                                   │      shared counter store          bulkhead + breaker
                                   │      (partitioned by API key)      + timeout + retry
                                   └── Idempotency-Key header → stored response replay
```

**Blocks.** Token bucket or sliding window per key (10); the counters
live in a shared store partitioned by API key so one hot customer hits
one shard (11); idempotency keys so a client's retry after a timeout
replays the stored response instead of repeating the side effect (10);
circuit breakers, bulkheads and deadlines on everything the handler
calls (10).

**Deep dive.**
- *Algorithm choice.* Fixed windows are cheap but allow up to double the
  limit across a window boundary. Sliding logs are exact but store every
  timestamp. Sliding-window counters approximate cheaply. Token buckets
  express "average rate plus burst" directly.
- *Local vs global.* Counting in each server's memory is fast and free
  but lets a client get N times the limit across N servers. A shared
  store is accurate but adds a network hop to every request. A common
  middle: local buckets with a share of the global rate, synced
  periodically.
- *Fail open or fail closed?* If the counter store is down, rejecting
  everything turns a limiter outage into a total outage. Most public APIs
  fail open (allow, log loudly) and protect themselves with bulkheads.
- *Client side.* Well-behaved clients retry with exponential backoff and
  jitter, and honour `Retry-After`; without jitter, every client that
  failed at the same moment retries at the same moment.

### 4.3 · Feed and notifications

**Requirements.** Users follow others; a user's home feed shows recent
posts from people they follow; followers get notified. Feed reads are
far more frequent than posts; a few seconds of delay is fine; celebrity
accounts have millions of followers.

```
post ──► posts store ──► outbox ──► relay ──► topic "posted"
                                                  ├──► fan-out worker ──► per-user feed lists (cache)
                                                  ├──► notification worker ──► push / email (retries, DLQ)
                                                  └──► search indexer, analytics, ...
read feed ──► feed cache ──(miss / celebrity posts)──► merge at read time
```

**Blocks.** Saving the post and announcing it must not drift apart: a
transactional outbox and a relay (12). Fan-out runs on a pub/sub topic
with several independent consumers (12), each at-least-once with dedupe
because duplicates will arrive (12). Failing notifications retry with
backoff and land in a dead-letter queue after N attempts (10, 12). Feeds
are cached lists keyed by user (09), partitioned by user id (11).

**Deep dive.**
- *Fan-out on write vs on read.* Push (write each post into every
  follower's feed) makes reads trivial and writes expensive: a celebrity
  post becomes millions of writes. Pull (assemble the feed at read time)
  makes writes trivial and reads expensive. The usual answer is hybrid:
  push for normal accounts, pull for celebrities, merge at read time.
- *Ordering.* Posts need a sortable id; time-ordered ids (11) give a
  feed order without a central clock.
- *Backpressure.* When a celebrity posts, the fan-out queue spikes. A
  bounded queue makes producers slow down instead of exhausting memory
  (12); priority scheduling keeps "your friend replied" ahead of bulk
  fan-out (12).
- *Notification storms.* Batch and rate-limit per recipient (10);
  "someone and 40 others liked your post" is a design decision.

### 4.4 · Ticket booking with holds

**Requirements.** Many people try to buy the same seats at the same
moment; a seat is sold at most once; a user can hold seats for ten
minutes while paying; unpaid holds expire; payment must not be charged
twice.

```
select seats ──► hold service ──► seats store (version per seat / per section)
                      │                 └─ hold: seat → (user, expiresAt)
                      ▼
pay ──► payment service (Idempotency-Key) ──► confirm: hold → sold (compare-and-set)
                      │
expired holds ──► delay scheduler ──► release (only if still held by that hold)
```

**Blocks.** "At most once" for a seat is a compare-and-set on its
version (13: optimistic locking): two buyers read the same free seat,
only the first write wins, the second re-reads and sees it taken. The
hold is a lease with an expiry on an injected clock (13: leases); its
expiry is a delayed job (12: delay scheduler). Payment is idempotent so
a retried "pay" never double-charges (10: idempotency keys). Booking
across seat inventory and payment is a saga: hold, charge, confirm, and
on failure release the hold and refund (12: saga with compensation).
Flash sales put a rate limit and a queue in front of the whole thing
(10, 12).

**Deep dive.**
- *Optimistic vs pessimistic.* Optimistic locking shines when conflicts
  are rare. At a popular on-sale moment every buyer conflicts on the
  same few sections, retries pile up, and a short pessimistic lock or a
  single-writer queue per event can be better. Say which regime you are
  in.
- *Expiry races.* The hold expires at exactly the moment the payment
  confirms. The confirm must be a conditional write ("still held by
  this hold id"), and the release must be one too, or the expiry job
  frees a seat that was just sold. Fencing tokens (13) solve the same
  race for leaders.
- *Why not two-phase commit with the payment provider?* External
  providers do not take part in your transactions, and 2PC blocks while
  the coordinator is unsure. Sagas trade isolation (others can briefly
  see "held") for availability.

### 4.5 · Job queue

**Requirements.** Producers submit jobs; workers run each job; jobs
survive crashes; failed jobs retry with backoff; some jobs run later or
first; a broken job must not block the queue; a job should run once,
or at least look as if it did.

```
producer ──► enqueue (DB row + outbox, or a log) ──► scheduler (delay + priority)
                                                            │ lease a job (visibility timeout)
                                                            ▼
                                                   worker ──► run ──► ack
                                                     │ crash / timeout → lease expires → redelivered
                                                     └ N failures → dead-letter queue
```

**Blocks.** Durable enqueue via a log or an outbox (13: write-ahead
log; 12: outbox). Delay and priority ordering (12). A worker leases a
job for a visibility timeout; if it dies the lease lapses and the job is
redelivered (13: leases), which makes delivery at-least-once, so jobs
need idempotency keys or dedupe (10, 12). Retries with exponential
backoff and jitter, then a dead-letter queue (10, 12). Bounded queues
and per-tenant limits stop one customer's flood from starving everyone
(10, 12).

**Deep dive.**
- *Visibility timeout length.* Too short and slow jobs are redelivered
  while still running (two workers, one job); too long and a crashed
  worker's job sits idle. Heartbeat-extended leases fix both, and a
  fencing token stops the slow first worker from committing a stale
  result (13).
- *Partitioning work.* Jobs keyed by tenant or entity, hashed to
  partitions, give per-key ordering and let workers scale out (11).
- *Poison messages.* Without a retry cap and a dead-letter queue, one
  job that always crashes is retried forever and blocks its partition.

### 4.6 · Key-value store (Dynamo-style)

**Requirements.** `get(key)` and `put(key, value)` at very high volume,
millisecond latency, always writable even during network partitions,
data on several machines so a node loss loses nothing.

```
client ──► any node (coordinator for this request)
              │ hash(key) on the ring → N replicas (next N distinct nodes)
              ├─ put: send to N, succeed after W acks   (vector clock attached)
              └─ get: ask N, answer after R replies; siblings if concurrent
each node: write-ahead log ──► memtable ──► sorted runs (LSM) + bloom filters
```

**Blocks.** Consistent hashing with virtual nodes to place keys and
rebalance cheaply (11). Replication with quorums, `R + W > N` for
read-your-latest-write, or smaller R and W for lower latency (11).
"Always writable" needs a sloppy quorum: when the key's home replicas
are unreachable, other nodes accept the write and hand it back later
(hinted handoff), which is exactly when concurrent versions appear. Each
node's storage engine is a write-ahead log in front of an LSM tree (13);
bloom filters let a read skip runs that cannot hold the key (11).
Concurrent writes during a partition are detected with vector clocks and
returned as siblings, or merged automatically with CRDTs or
last-writer-wins (13). Hot keys get a cache (09).

**Deep dive.**
- *Choosing N, R, W.* N=3, W=2, R=2 is the classic balance. W=1 makes
  writes fast and risks losing the newest write if that one node dies;
  R=1 makes reads fast and possibly stale.
- *LWW vs siblings.* Last-writer-wins is simple and loses concurrent
  writes silently; siblings preserve them and push the merge to the
  application; CRDTs make the merge automatic for data types that fit.
- *Deletes.* Deleting must leave a tombstone, on disk (13: LSM) and
  across replicas, or a replica that missed the delete resurrects the
  value during repair.
- *Strong consistency instead?* Then you want a leader per partition
  chosen with leases and a consensus log (etcd, Spanner), and you give up
  writes on the minority side of a partition.

---

## 5 · Trade-off tables

Every design decision is one row of a table like these. In an interview,
saying which column you chose and why is worth more than the choice.

### Consistency and availability

| | strong (linearizable) | eventual |
| --- | --- | --- |
| a read sees | the latest committed write | some recent write; converges later |
| during a partition | the minority side refuses (CP) | both sides accept writes (AP) |
| cost | a leader, consensus, cross-replica round trips | conflict resolution (LWW, siblings, CRDTs) |
| use for | money, inventory, uniqueness (usernames, seats) | likes, view counts, feeds, presence |

PACELC adds the everyday half of CAP: even with no partition, you trade
**latency** against **consistency** on every request (waiting for more
replicas is safer and slower).

### Where to do the work

| choice | option A | option B |
| --- | --- | --- |
| fan-out | on write: fast reads, write amplification | on read: cheap writes, slow reads |
| processing | synchronous: simple, fresh, coupled | asynchronous (queue): decoupled, resilient, delayed |
| aggregation | precompute (materialized view, projection) | compute on demand |
| validation | at the edge (cheap, may be bypassed) | at the data (authoritative, later) |

### Concurrency control

| | optimistic (versions, CAS) | pessimistic (locks) |
| --- | --- | --- |
| best when | conflicts are rare | conflicts are frequent |
| on conflict | retry the read-modify-write | wait for the lock |
| failure mode | retry storms under contention | deadlocks, lock held by a dead client |
| needs | a version column, a retry loop | lock timeouts or leases |

### Delivery guarantees

| guarantee | how | cost |
| --- | --- | --- |
| at most once | send, never retry | messages lost on failure |
| at least once | retry until acked | duplicates; consumers must be idempotent |
| effectively once | at least once + dedupe / idempotency keys | storage for seen ids, a retention window |

### Multi-step changes across services

| | two-phase commit | saga |
| --- | --- | --- |
| atomicity | all or nothing | eventually all, or compensated |
| isolation | intermediate states hidden | intermediate states visible |
| availability | blocks while the coordinator is unsure | each step commits locally and moves on |
| needs | every participant supports prepare | a compensation for every step |

### Ids

| scheme | unique without coordination | sortable by time | short | unguessable |
| --- | --- | --- | --- | --- |
| database sequence | no (one counter) | yes | yes | no |
| snowflake-style | yes (machine bits) | roughly | medium | no |
| random (UUID v4) | yes (probabilistically) | no | no | yes |
| hash of content | yes (dedupes) | no | depends | depends |

---

## 6 · Which exercise practices this

Use this map in both directions: when a design leans on an idea, go and
build the block; when you finish a module, find the designs that use it.
Modules 09–12 list their exact files in each module README's exercise
table; module 13's are named here.

| idea | module | used in design |
| --- | --- | --- |
| LRU / LFU eviction | 09-caching | URL shortener, KV store hot keys |
| TTL with an injected clock | 09-caching | URL shortener, feed cache |
| cache-aside, read-through, write-through | 09-caching | URL shortener, feed |
| stampede protection (single-flight) | 09-caching | URL shortener (viral links) |
| negative caching | 09-caching | URL shortener (bad codes) |
| token bucket, fixed and sliding windows | 10-rate-limits-and-resilience | rate-limited API, notifications, booking on-sale |
| retry with exponential backoff and jitter | 10-rate-limits-and-resilience | job queue, notifications, API clients |
| circuit breaker, bulkhead, deadlines | 10-rate-limits-and-resilience | rate-limited API (dependencies) |
| idempotency key with replay | 10-rate-limits-and-resilience | booking payments, API retries, job queue |
| hash sharding and rebalancing cost | 11-partitioning-and-ids | KV store, limiter counters |
| consistent hashing, virtual nodes, rendezvous | 11-partitioning-and-ids | KV store, URL shortener storage |
| snowflake ids, base62 codes | 11-partitioning-and-ids | URL shortener, feed ordering |
| range partitioning | 11-partitioning-and-ids | bookings by date, time-series data |
| bloom filter | 11-partitioning-and-ids | KV store reads (with the LSM tree) |
| quorum math (R + W > N) | 11-partitioning-and-ids | KV store replication |
| pub/sub, at-least-once with dedupe | 12-messaging-and-events | feed fan-out, notifications |
| bounded queue, backpressure | 12-messaging-and-events | feed fan-out spikes, job queue |
| transactional outbox | 12-messaging-and-events | feed (post → event), job enqueue |
| dead-letter after N retries | 12-messaging-and-events | job queue, notifications |
| event sourcing and projections | 12-messaging-and-events | feed read models, audit trails |
| saga with compensation | 12-messaging-and-events | ticket booking (hold, pay, confirm) |
| delay and priority scheduling | 12-messaging-and-events | hold expiry, job queue |
| optimistic locking with versions | 13 · `01-versioned-writes.java` | ticket booking (seat CAS) |
| write-ahead log, crash recovery | 13 · `02-write-ahead-log.java` | KV store node, job queue durability |
| memtable, sorted runs, compaction | 13 · `03-lsm-tree.java` | KV store node |
| MVCC snapshot reads | 13 · `04-mvcc-snapshots.java` | any relational store under the booking system |
| G-counter, last-writer-wins | 13 · `05-crdt-merge.java` | like/view counts, KV store conflict merge |
| vector clocks, siblings | 13 · `06-vector-clocks.java` | KV store concurrent writes |
| leases and fencing tokens | 13 · `07-leader-lease.java` | seat holds, job visibility timeouts, partition leaders |
| two-phase commit and recovery | 13 · `08-two-phase-commit.java` | the alternative a booking saga avoids |

---

## 7 · Mistakes that sink a design answer

1. **Starting with technology.** "We'll use Kafka and Cassandra" before
   anyone knows the read/write ratio. Requirements, numbers, access
   patterns, then technology.
2. **No numbers.** A design for 10 requests a second and one for 100,000
   look different. Do the envelope even when nobody asks.
3. **Ignoring the write path of a cache.** Every cache answer needs a
   sentence on invalidation and on how stale is acceptable.
4. **Retries everywhere, idempotency nowhere.** Each retry you add needs
   a story for "what if the first attempt actually succeeded?"
5. **"Exactly once."** Say at-least-once plus idempotent consumers, and
   name where the dedupe state lives and how long it is kept.
6. **Wall-clock time as truth across machines.** Clocks drift and jump.
   Use leases with margins, fencing tokens, logical or vector clocks, or
   a single authority for ordering.
7. **A single point of failure hiding in plain sight**: the one counter
   that issues ids, the one Redis that holds every rate limit, the one
   coordinator in a 2PC. Name it and say what happens when it dies.
8. **Forgetting deletes.** Deletes in caches (invalidate), in LSM trees
   and replicas (tombstones), in event logs (compensating events) and in
   ids (never reuse).
9. **Over-engineering.** Sharding a dataset that fits on one machine
   costs you joins, transactions and your weekends. Show the number that
   forces each piece of complexity.

---

## 8 · How to study this section

- Do modules 09 → 13 in order; each later module quietly uses earlier
  ones (an idempotency key from 10 reappears as the dedupe set in 12; a
  lease from 13 is the visibility timeout of a job queue).
- After each module, pick one design from section 4 and redraw it from
  memory, labelling every box with the exercise that built it.
- Practise out loud. Take a design you have not seen (a chat app, a
  leaderboard, a parking-lot reservation system, a metrics pipeline) and
  run the eight steps of section 1 against a 45-minute timer. Then list
  every trade-off you made, one row per table in section 5.
- When a design question asks "what happens if...", the answer is almost
  always one of the details that bite in modules 09–13. Re-reading those
  lists is the fastest review there is.
