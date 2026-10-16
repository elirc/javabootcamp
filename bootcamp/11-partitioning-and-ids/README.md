# 11 · Partitioning and IDs

One machine stops being enough long before you expect it: the table no
longer fits in RAM, one disk can't take the writes, one primary is one
outage. The fix is always the same sentence ("split the data across
machines") and the hard part is always the same three questions: *which
machine owns this key*, *what happens when machines come and go*, and
*how do machines that never talk to each other hand out unique names*.
This module answers them with the small, exact algorithms that real
databases, caches and CDNs run: modulo sharding, range partitions, the
consistent-hash ring, rendezvous hashing, Snowflake ids, base62 codes,
Bloom filters and quorum replication.

## The mental model

### 1 · Partitioning is a function from key to owner

Every scheme in this module is a pure function `owner(key, cluster)`.
What separates them is what the function preserves and what it costs
when the cluster changes:

| scheme | owner(key) | range scans | keys moved when N → N+1 | seen in |
| --- | --- | --- | --- | --- |
| hash mod N | `floorMod(hash(key), N)` | no | about N/(N+1): almost all | hand-rolled app sharding, early memcached clients |
| range | how many split points ≤ key | **yes** | only the range being split | Bigtable, HBase, CockroachDB, Spanner |
| consistent ring | first point clockwise of `hash(key)` | no | about 1/(N+1), all to the new node | Dynamo, Cassandra, Riak, ketama |
| rendezvous (HRW) | node with the highest `hash(node, key)` | no | about 1/(N+1), all to the new node | Apache Ignite, Tahoe-LAFS |

```java
Math.floorMod(key.hashCode(), shards)                 // never % on a hash
var hit = ring.ceilingEntry(hash(key));               // first point >= h
(hit != null ? hit : ring.firstEntry()).getValue();   // ... or wrap
nodes.stream().min(byScoreDescThenName(key))          // rendezvous
```

### 2 · Count the keys that move

Rebalancing cost is the number to put on the whiteboard. With mod-N a
key stays put only when `h mod N == h mod (N+1)`, so growing 4 → 5
shards moves about 80% of the data (exercise 01 measures 814 of 1000).
That is 80% of the rows copied over the network and 80% of the cache
entries suddenly missing, all at once. Consistent and rendezvous
hashing move about 1/N, and every moved key goes to the node that
joined:

```
mod N, 4 → 5 shards            ring / HRW, 4 → 5 nodes
A B C D A B C D A B ...        A A B B C C D D ...
A B C D E A B C D E ...        A A B E C C D D ...   ← only E's arc moved
  ↑ almost every key moves
```

### 3 · Virtual nodes and tie-breaks make it fair and deterministic

A ring with one point per node has arcs of wildly different lengths;
one node can own half the keys. Give every node 100-256 points and the
arcs average out (exercise 04 checks the spread), and when a node
leaves, its load spreads over everyone instead of one neighbour.
Rendezvous hashing needs no virtual nodes, but its comparator must be
**total** (score, then name): two routers that list the nodes in a
different order must still agree on the owner.

### 4 · IDs without a coordinator

A central sequence is a single point of failure and a round trip per
insert. Snowflake packs everything a unique id needs into 64 bits:

```
 0 | 41 bits: ms since custom epoch | 10 bits: worker | 12 bits: sequence
       (about 69 years)               (1024 workers)    (4096 ids per ms)
```

Time in the high bits makes ids sort by creation time (friendly to
B-tree indexes); the worker id makes two machines' ids disjoint; the
sequence counts within one millisecond. Base62 is the presentation
layer: the same number written with `0-9a-zA-Z` is a short, URL-safe
code, and `capacity(7)` = 62^7 ≈ 3.5 trillion is the back-of-envelope
answer to "how long do short links need to be?". (Sequential ids give
guessable codes; real shorteners scramble the id first, for example by
multiplying by a large odd constant modulo 62^7.)

### 5 · Ask "maybe?" cheaply, and ask enough replicas

A **Bloom filter** is a bit array that answers *definitely not* or
*maybe*. LSM stores (LevelDB, RocksDB, Cassandra) keep one per file so
a lookup for a missing key skips nearly every disk read. Sizing is
arithmetic, not guesswork:

```
false-positive rate ≈ (1 - e^(-k·n/m))^k        best k = (m/n)·ln 2
10 bits per key  →  k = 7  →  about 0.8% false positives
```

**Quorums** decide how many of N copies a write and a read must touch.
If `R + W > N`, every read set overlaps every write set, so at least
one replica the read contacts has the latest version:

```
N=3, W=2, R=2      write lands on {0,1}   read asks {1,2}   overlap at 1 ✓
N=3, W=1, R=1      write lands on {0}     read asks {1}     no overlap ✗ stale
```

## The details that bite

1. **`%` keeps the sign.** `hashCode()` is negative half the time and
   `-3 % 10 == -3`. `Math.abs` is not the fix:
   `Math.abs(Integer.MIN_VALUE) < 0`, and `"polygenelubricants"`
   hashes to exactly that. Use `Math.floorMod` (exercises 01 and 07).
2. **A boundary key belongs to the range it starts.** With split point
   `"g"`, `partitionOf("g")` is 1, not 0. `Collections.binarySearch`
   returns the index of an exact hit, so the partition is `index + 1`.
3. **Scan ends are exclusive.** `[a, g)` touches only partition 0;
   counting partition 1 because `"g"` starts it makes every range query
   fan out one partition too far.
4. **`ceilingEntry`, not `higherEntry`, and wrap around.** A key whose
   hash equals a point belongs to that point; a key past the last point
   belongs to the first. Forgetting the wrap returns `null`.
5. **Comparators on hashes: `Long.compare`, never subtraction.**
   `(int) (a - b)` overflows for hashes near the ends of the long range
   and silently picks the wrong owner. Then break ties on the name.
6. **A full sequence must wait, not wrap or spill.** The 4097th id in
   one millisecond would either repeat sequence 0 (a duplicate) or carry
   into the worker bits (someone else's id). Wait for the clock to tick.
7. **Clocks go backwards.** NTP steps and VM pauses happen. Issuing ids
   from a rewound clock can repeat ids already handed out; refuse.
8. **Overflow is silent.** `long * 62` wraps, so a too-long base62 code
   decodes to a negative id. `Math.multiplyExact`/`addExact` throw.
9. **Integer division hides in formulas.** `bits / items` with ints is
   `6` for 10000/1500, and the optimal `k` comes out 4 instead of 5.
10. **"Highest version", not "first replica that answered".** Arrival
    order is not a timeline. Take the newest version among the replies,
    then read-repair the contacted replicas that are behind.

## Cheat table

| question | reach for |
| --- | --- |
| which shard, N fixed forever | `Math.floorMod(hash, N)` |
| range scans, ordered keys, splitting hot spots | sorted split points + binary search |
| nodes join and leave often | consistent ring (`TreeMap` + vnodes) or rendezvous |
| N replicas per key in a stable order | rendezvous: sort nodes by score, take the top N |
| unique, time-ordered ids with no coordinator | Snowflake bit packing |
| short public codes | base62 of an id |
| "is it definitely absent?" in a few bits per key | Bloom filter |
| "will my read see my write?" | is `R + W > N`? |

## How this connects

- **09-caching** puts a cache in front of one store. Sharding a cache
  tier with mod-N turns every resize into a mass of misses, which is
  why cache clients moved to consistent hashing.
- **10-rate-limits-and-resilience**: per-key limits and idempotency
  stores only work when every client computes the same owner for a key.
- **12-messaging-and-events**: Kafka assigns a message to a partition
  by hashing its key modulo the partition count. Ordering is per
  partition, so adding partitions to a live topic reshuffles keys and
  breaks per-key order, the exact cost exercise 01 measures.
- **13-storage-and-consistency**: quorums give overlap; vector clocks,
  LWW registers and CRDTs decide what "newest" means when versions
  conflict, and the LSM exercise is where Bloom filters earn their keep.

## Exercises

| # | file | ★ | what you build |
| --- | --- | --- | --- |
| 01 | `01-hash-sharding.java` | ★☆☆ | `floorMod` sharding and the cost of changing N |
| 02 | `02-base62-codes.java` | ★☆☆ | URL-shortener codes with overflow-safe decoding |
| 03 | `03-range-partitions.java` | ★★☆ | sorted ranges, range scans, hot-range splits |
| 04 | `04-consistent-hash-ring.java` | ★★☆ | a hash ring with virtual nodes on a `TreeMap` |
| 05 | `05-rendezvous-hashing.java` | ★★☆ | highest-random-weight owners and replica lists |
| 06 | `06-snowflake-ids.java` | ★★☆ | 64-bit time-ordered ids with an injected clock |
| 07 | `07-bloom-filter.java` | ★★★ | a double-hashed Bloom filter and its sizing math |
| 08 | `08-quorum-replication.java` | ★★★ | N/R/W replication, stale reads and read repair |

Do the warm-ups and core in order. Stretch if time allows.

---

**Next:** `bootcamp/12-messaging-and-events`
