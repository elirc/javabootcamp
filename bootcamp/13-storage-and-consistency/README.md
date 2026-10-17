# 13 · Storage and Consistency

Every system-design diagram ends in a box labelled "DB", and every hard
question is hiding in that box. What happens to a write if the power
fails halfway through it? How can a database take 100,000 writes a second
on disks that hate random writes? How do two users edit the same row
without one silently erasing the other? How do three replicas agree when
the network between them is cut? And how do two databases commit "both or
neither"? This module builds each answer in miniature: the write-ahead
log, the LSM tree, optimistic locking, MVCC, CRDTs, vector clocks,
leases and two-phase commit. They are small enough to fit in one file
each, and they are the same ideas you will find inside PostgreSQL,
RocksDB, Cassandra, DynamoDB, etcd and Kafka.

## The mental model

### 1 · Durability is a log you can replay

A disk only promises that data has landed once a flush (`fsync`) of it
has returned, and flushing one sequential file is far cheaper than
flushing pages scattered across the data files. So a database appends
every change to a **write-ahead log**, flushes it, and updates its real
data structures later. After a crash it replays the log. A
transaction becomes real at exactly one moment: when its COMMIT record is
durable.

```
BEGIN t1 | SET t1 a 1 | SET t1 b 2 | COMMIT t1 | BEGIN t2 | SET t2 c 3 ✂ crash
                                    └── t1 survives       └── t2 never happened
```

A crash can only tear the last record. A broken record with good ones
after it is not a crash, it is corruption, and recovery must refuse
rather than guess.

### 2 · Turn random writes into sequential ones (LSM)

```
put/delete ──► memtable (sorted, in memory)
                  │ full
                  ▼ flush (write once, never modify)
               run 3  (newest)    get: memtable → run 3 → run 2 → run 1
               run 2              first hit wins, even a tombstone
               run 1  (oldest)
                  │ compaction: merge runs, newest wins, drop dead entries
```

LevelDB, RocksDB, Cassandra, HBase and ScyllaDB all work this way. Writes
are cheap appends; reads may check several runs (bloom filters, module 11,
let them skip most); compaction pays the cleanup bill in the background.
A delete cannot remove anything from an immutable run, so it writes a
**tombstone** that shadows the older value.

| | B-tree (PostgreSQL, InnoDB) | LSM tree (RocksDB, Cassandra) |
| --- | --- | --- |
| write | update a page in place | append to memtable, flush whole runs |
| read | one tree walk | memtable + several runs (bloom filters help) |
| where writes cost extra | a whole page rewritten for a small change | the same data rewritten by each compaction |
| good for | read-heavy, predictable read latency | write-heavy, time series, logs |

### 3 · Concurrency without waiting: versions

**Optimistic locking**: read a row with its version, compute, write back
only if the version has not changed (`UPDATE ... WHERE id = ? AND
version = ?`). On a conflict, re-read and recompute. Good when conflicts
are rare; under heavy contention the retries waste work and a real lock
is better.

**MVCC** keeps every committed version stamped with its commit
timestamp. A transaction reads the snapshot at its start timestamp, so
readers never block writers and writers never block readers. At commit,
"first committer wins": if a key you wrote was committed by someone else
after your snapshot, you abort.

```
x:  (ts 1, "old")  (ts 3, "new")
tx started at ts 2 reads "old"  ← forever, however many commits follow
```

| isolation level | what it still allows |
| --- | --- |
| read committed | the same query returning different rows twice in one transaction |
| snapshot isolation (MVCC) | write skew: two transactions read the same rows and write different ones |
| serializable | nothing: it behaves as if transactions ran one at a time |

### 4 · Replicas disagree; decide how they converge

When replicas accept writes during a network partition (the "A" choice
in CAP), they will diverge. Three tools, from simplest to richest:

- **Last-writer-wins**: keep the write with the higher timestamp, ties
  broken by node id. Simple, but a concurrent write is silently lost,
  and clocks on different machines drift.
- **CRDTs**: data types whose merge is commutative, associative and
  idempotent, so replicas converge whatever order gossip arrives in. A
  G-counter gives each node its own slot and merges by per-node max.
- **Vector clocks**: one counter per node. They answer "did x happen
  before y, or did neither see the other?" Concurrent versions are kept
  as siblings for the application to merge (Dynamo's shopping cart).

```
{a:2, b:1} vs {a:2, b:3}  → BEFORE      (every counter <=, one <)
{a:3, b:1} vs {a:2, b:3}  → CONCURRENT  (some <, some >)
```

### 5 · Agreement: leases and atomic commit

A **lease** is a lock that expires, so a dead leader frees it without
anyone noticing the death. Because a paused leader does not know its
lease ran out, every lease carries an increasing **fencing token**, and
storage refuses writes with a token lower than the highest it has seen.

**Two-phase commit** makes several databases commit together: prepare
everyone (a YES is a durable promise), log the decision, then tell
everyone. The decision record in the coordinator's log is the commit
point; everything after it can be retried.

```
coordinator          p1          p2
   │── prepare ─────►│           │
   │── prepare ─────────────────►│
   │◄─ YES ──────────│◄─ YES ────│
   │ log "COMMIT"   ← point of no return
   │── commit ──────►│── commit ►│
   │ log "DONE"
```

2PC blocks: a participant that voted YES holds its locks until it hears
the decision. That is why services that cannot wait use sagas (module
12) and why consensus systems (Raft, Paxos) replicate the coordinator
itself.

## The details that bite

1. **Retry the whole read-modify-write, not just the write.** Re-sending
   a value computed from stale data is the lost update you were trying to
   prevent (exercise 01).
2. **Never reset a version.** Deleting a row and restarting its version
   at 0 lets an old writer's version match again (ABA). Keep a tombstone
   with a higher version (exercise 01).
3. **Apply a transaction at its COMMIT record, not as you read its
   operations**, and apply them in commit order, not begin order
   (exercise 02).
4. **Only the last record can be torn.** Ignoring a broken line in the
   middle of a log silently loses data; refuse with the line number
   (exercise 02).
5. **Deletes write tombstones.** Removing the key from the memtable lets
   the older run's value come back (exercise 03).
6. **Compact oldest first, then drop tombstones** — and only when every
   older run is part of the merge (exercise 03).
7. **`>` versus `>=` on timestamps.** A snapshot at ts 2 sees commits
   with ts <= 2; a conflict is a commit with ts > startTs (exercise 04).
8. **Garbage collection must keep the newest version at or below the
   horizon**; that is the one the oldest snapshot is reading
   (exercise 04).
9. **Merge with max, not plus.** Gossip repeats; an additive merge counts
   the same increments twice (exercise 05).
10. **Ties need a global tie-break.** "Keep mine" on equal timestamps
    leaves replicas disagreeing forever (exercise 05).
11. **Compare vector clocks over the union of nodes**, with missing
    counters as 0, and check BOTH directions before saying BEFORE
    (exercise 06).
12. **An expired lease is gone at exactly `acquiredAt + ttl`**, and it
    cannot be renewed back to life (exercise 07).
13. **Fence the storage, not the leader.** Only the store can reject a
    deposed leader's late write (exercise 07).
14. **Log the 2PC decision before the first commit message**, and after
    that never change it; failures only delay delivery (exercise 08).

## Cheat table

| problem | tool | real-world home |
| --- | --- | --- |
| survive a crash | write-ahead log + replay | PostgreSQL WAL, MySQL redo log, Kafka |
| fast writes on disk | LSM tree | RocksDB, Cassandra, LevelDB |
| two editors, one row | optimistic locking (versions / ETags) | JPA `@Version`, DynamoDB conditional writes, HTTP `If-Match` |
| readers vs writers | MVCC snapshots | PostgreSQL, Oracle, InnoDB |
| replicas merging counters | CRDT (G-counter, PN-counter) | Riak, Redis CRDTs, collaborative editors |
| detecting concurrent writes | vector clocks / version vectors | Dynamo, Riak, Voldemort |
| one active leader | lease + fencing token | etcd, ZooKeeper, Kubernetes Lease |
| commit across databases | two-phase commit | XA transactions, Spanner (2PC over Paxos) |

## Exercises

| # | file | ★ | what you build |
| --- | --- | --- | --- |
| 01 | `01-versioned-writes.java` | ★☆☆ | optimistic locking with versions, tombstones and a retry loop |
| 02 | `02-write-ahead-log.java` | ★★☆ | crash recovery from a write-ahead log, torn writes and corruption |
| 03 | `03-lsm-tree.java` | ★★☆ | a memtable, sorted runs, tombstones, compaction and range scans |
| 04 | `04-mvcc-snapshots.java` | ★★★ | MVCC snapshot reads, first-committer-wins and version GC |
| 05 | `05-crdt-merge.java` | ★☆☆ | a G-counter and a last-writer-wins register that always converge |
| 06 | `06-vector-clocks.java` | ★★☆ | vector clocks and sibling reconciliation |
| 07 | `07-leader-lease.java` | ★★☆ | leader election with leases and fencing tokens on an injected clock |
| 08 | `08-two-phase-commit.java` | ★★★ | a two-phase commit coordinator with a decision log and recovery |

Do the warm-ups and core in order. Stretch if time allows. Exercises
01–04 are one machine; 05–08 are several. If you have done modules 11
(quorums, partitioning) and 12 (outbox, sagas), notice how 02 is the
same log as the outbox relay, and 08 is the all-or-nothing that a saga
gives up on purpose.

---

**Done with the system-design block?** Read the guide that ties modules
09–13 together into whole designs, `astraupskill/03-SYSTEM-DESIGN.md`,
then start the next block on writing code that stays easy to change.

**Next:** `bootcamp/14-generics-and-interfaces`
