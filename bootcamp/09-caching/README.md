# 09 · Caching

Every fast system you have used is a slow system with a cache in front of
it. Your CPU has three levels of them, your browser has one, the CDN has
one at the edge, the API gateway has one, the service keeps a Caffeine map
in its heap, and Redis sits between the service and Postgres. Caching is
the cheapest performance win in system design and the most common source
of "but I already fixed that, why does the page still show the old value?"
This module builds the moving parts in memory, one file each, so you can
see exactly where the bugs hide before you meet them in Redis.

This is the first module of the **System Design** block (09–13). The style
changes a little from 01–08: each exercise is a small in-process model of
a production idea. The Java is still plain; the point is the *behaviour*,
and the tests pin it down to the millisecond.

## The mental model

### 1 · A cache is a bet on locality

```
   request ──► cache ──hit──► answer            (microseconds)
                 │
                miss
                 ▼
              database ──► answer + remember    (milliseconds)
```

The bet: what was asked for recently (temporal locality) or often
(frequency) will be asked for again. The number that tells you whether the
bet pays is the **hit rate**, `hits / (hits + misses)`. A 99 % hit rate
means the database sees 1 request in 100; a 50 % hit rate means you added
a network hop to half your requests for a 2x saving. Measure it (04).

### 2 · Memory is finite: something must leave

Three questions decide every cache: *how big*, *who leaves when it's
full*, and *how long may an answer live*.

| policy | evicts | good at | fooled by | seen in |
| --- | --- | --- | --- | --- |
| LRU | least recently used | recency-heavy traffic | one big scan flushes it | Memcached, `LinkedHashMap` |
| LFU | least frequently used | stable hot set | newcomers struggle; old fame lingers | Redis `allkeys-lfu` |
| TTL | anything older than *t* | bounded staleness | nothing: it is a freshness rule, not a size rule | every HTTP `max-age` |
| W-TinyLFU | rarely-seen newcomers | mixed workloads | (state of the art) | Caffeine |

Size rules and freshness rules combine: a real cache is "LRU, max 10 000
entries, each entry at most 5 minutes old" (06).

```java
new LinkedHashMap<K, V>(16, 0.75f, true) {          // true = access order
    protected boolean removeEldestEntry(Map.Entry<K, V> e) {
        return size() > capacity;                    // an LRU cache in 4 lines
    }
};
```

### 3 · Writes are where caches lie

The cache is a *copy*. The database is the truth. On every write you must
decide what happens to the copy:

| policy | on write | next read | risk |
| --- | --- | --- | --- |
| cache-aside (lazy) | write db, **delete** cache key | miss → reload | one extra load per write |
| write-through | write db, **set** cache key | hit | caches values nobody reads |
| write-behind | set cache, write db later | hit | lose writes if the cache dies |

Two rules hold for all of them: **the database goes first** (if it fails,
the cache must still agree with it), and **prefer delete to set** when two
writers may race, because a delete can only cost a reload while a late set
can pin an old value forever (03).

### 4 · Misses are traffic too

Two kinds of miss hurt more than ordinary ones:

- **Misses for things that don't exist.** A cache that only remembers hits
  sends every "user not found" to the database. Cache the absence, with a
  shorter TTL, like DNS does with NXDOMAIN (05).
- **Many misses for the same key at once.** A hot key expires and 500
  requests miss in the same millisecond: a *stampede*. Collapse them into
  one load and let everyone wait on it: *single-flight* (08).

```
 t0  get(k) ─┐
 t1  get(k) ─┼──► ONE loader call ──► every caller gets the same answer
 t2  get(k) ─┘
```

### 5 · Time is an input

A TTL cache that calls `System.currentTimeMillis()` can only be tested by
sleeping. Inject the clock (`LongSupplier`, `java.time.Clock`) and a test
can stand at t = 99 and t = 100 and check the boundary exactly. Every
exercise with time in it takes a `FakeClock` you move by hand.

## The details that bite

1. **Insertion order is not recency.** A plain `LinkedHashMap` is FIFO:
   reads move nothing and the hottest key goes first. Pass
   `accessOrder = true` (01).
2. **Replacing is not growing.** `put` on an existing key must not evict
   anyone, because the size did not change (01, 07).
3. **`<` versus `<=` at the expiry instant.** Store `expiresAt = put + ttl`
   and treat `now < expiresAt` as alive; at exactly `expiresAt` it is gone
   (02, 05, 06).
4. **A read does not extend a TTL** unless you deliberately chose
   "expire after access". Refreshing on get keeps a stale hot value alive
   forever (02).
5. **`size()` must not count the dead.** Lazy expiry leaves corpses in the
   map until someone touches them (02).
6. **Cache first, database second is backwards.** If the db write then
   fails, the cache serves a value that was never stored (03).
7. **An absent row is not a value.** Don't cache it by accident (03); cache
   it on purpose, with its own TTL, and overwrite it when the row is
   created (05).
8. **An exception is not "not found".** Caching a timeout turns a blip into
   a confident lie for every caller (04, 05).
9. **`0 / 0` is NaN.** `hitRate()` before the first request must be 0.0 (04).
10. **Dead entries leave before live ones are evicted.** A size eviction
    while an expired entry still holds a slot throws away a good value for
    nothing (06).
11. **Listener code is someone else's code.** Its exception must not
    abort the put halfway (06).
12. **LFU's minimum moves.** Promote the last key out of the lowest bucket
    and `minFreq` goes up; insert a new key and it drops back to 1 (07).
13. **Register cleanup after publishing.** `whenComplete` on an already
    completed future runs immediately, before your `put`. Order them the
    other way and the finished flight stays in the map forever (08).
14. **Never hand out the shared future.** Anyone holding a
    `CompletableFuture` can `complete` or `cancel` it for everyone; give
    each caller `copy()` (08).

## Where you will meet this

| idea | production form |
| --- | --- |
| LRU / LFU / TTL | Caffeine, Guava `CacheBuilder`, Redis `maxmemory-policy`, Memcached |
| read-through + stats | `LoadingCache`, Spring `@Cacheable`, Micrometer cache metrics |
| cache-aside | the default Redis pattern in front of SQL |
| negative caching | DNS NXDOMAIN TTL, CDN caching of 404s |
| removal listeners | Caffeine `RemovalListener`, closing pooled resources |
| single-flight | Go `singleflight`, Caffeine `AsyncLoadingCache`, CDN request collapsing |

Links forward: a cache absorbs load, but when the load still exceeds what
the backend can take, **10 · rate limits and resilience** decides who gets
turned away. Distributed caches shard keys with the hashing in **11**, and
invalidation across services travels as events in **12**. Module 08's
idempotent payments is the cousin of single-flight: both make a repeated
request take effect once.

## Exercises

| # | file | ★ | what you build |
| --- | --- | --- | --- |
| 01 | `01-lru-cache.java` | ★☆☆ | an LRU cache on `LinkedHashMap` access order |
| 02 | `02-ttl-cache.java` | ★☆☆ | per-entry expiry with an injected clock and a purge |
| 03 | `03-write-policies.java` | ★★☆ | cache-aside vs write-through, including a failing db |
| 04 | `04-loading-cache.java` | ★★☆ | a read-through cache with hit/miss/load/eviction stats |
| 05 | `05-negative-caching.java` | ★★☆ | caching "not found" with its own TTL, but never errors |
| 06 | `06-removal-listener.java` | ★★☆ | TTL + LRU with removal causes and a safe listener |
| 07 | `07-lfu-cache.java` | ★★★ | O(1) LFU with frequency buckets and an LRU tie-break |
| 08 | `08-single-flight.java` | ★★★ | stampede protection: one load per key in flight |

Do the warm-ups and core in order. Stretch if time allows.

---

**Next:** `bootcamp/10-rate-limits-and-resilience`
