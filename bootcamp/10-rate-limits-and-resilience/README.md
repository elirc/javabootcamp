# 10 · Rate Limits and Resilience

Module 09 made the happy path fast. This one is about the unhappy path:
too many requests, a dependency that is slow, a dependency that is down,
a client that retries. Every large outage post-mortem you will read has
the same plot: something got slow, callers waited, callers retried, the
retries made it slower, and the failure spread to services that had
nothing to do with the original problem. The patterns here are how
systems say **no** early, **wait** politely, and **stop** before the
damage spreads. Each one is a small state machine driven by an injected
clock, so the tests can stand at the exact millisecond where it changes
its mind.

## The mental model

### 1 · Two sides of the same wire

```
   client side                              server side
   ───────────                              ───────────
   retry + backoff + jitter   (04)          rate limit: who gets in (01–03)
   circuit breaker: stop calling (05)       bulkhead: how many at once (06)
   deadline: how long in total  (07)        idempotency keys: retries safe (08)
```

Protecting the server (limits, bulkheads) and being a good client
(backoff, breakers, deadlines) are the same discipline from two ends.
Retries only work when the server makes them safe (08), and a breaker
only helps when the server refuses quickly instead of hanging.

### 2 · Rate limiting: count something per client per unit of time

| algorithm | state per client | bursts | exact? | where |
| --- | --- | --- | --- | --- |
| token bucket | tokens + last refill time | up to capacity, then the steady rate | yes | AWS API Gateway, Guava `RateLimiter`, Stripe |
| fixed window | window index + count | 2 x limit across a boundary | no | Redis `INCR` + `EXPIRE`, GitHub's hourly quota |
| sliding log | one timestamp per allowed request | none | yes | Redis sorted sets, small strict limits |
| sliding counter | two counters, weighted | small | approximately | Cloudflare's rate limiting |

(nginx's `limit_req` is the fifth classic, a *leaky bucket*: requests
drain at a fixed rate and a burst waits in a short queue instead of
going straight through.)

The token bucket is the usual default because its two numbers say what
people actually mean: *"10 per second on average, bursts of 30 are
fine."* Refill is computed lazily from elapsed time, so there is no timer:

```java
tokens = Math.min(capacity, tokens + elapsedMs * ratePerSecond / 1000.0);
```

When you refuse, say when to come back: `429 Too Many Requests` with a
`Retry-After` header (03 computes it).

### 3 · Retries: exponential, capped, jittered, selective

```
attempt 1 ✗ ── sleep ~100 ── attempt 2 ✗ ── sleep ~200 ── attempt 3 ✗ ── give up
                  ▲ random in [0, cap]: spreads the herd out
```

- **Exponential** so a dead server sees fewer requests over time.
- **Capped** so the 20th retry does not sleep for a week.
- **Jittered** so 10 000 clients that failed together don't retry
  together. Without jitter the outage comes back in synchronised waves.
- **Selective**: retry timeouts and 503s, never a 400. A cap on
  attempts (say 3) bounds the extra load per request to a known
  multiple; a fleet-wide *retry budget* (retries capped at a fraction
  of live traffic, as in Finagle and Envoy) stops them amplifying an
  outage.

### 4 · Circuit breaker: stop calling what is broken

```
          threshold consecutive failures
  CLOSED ─────────────────────────────────►  OPEN  (fail fast, no calls)
    ▲                                          │ cool-down elapsed
    │ trial succeeds                           ▼
    └──────────────────────────────────── HALF_OPEN ── trial fails ──► OPEN
                                                        (fresh cool-down)
```

The breaker turns a 30-second timeout into a 0-millisecond refusal, which
frees the caller's threads and gives the dependency room to recover.

### 5 · Bulkheads and deadlines: bound the damage

A **bulkhead** gives each dependency a fixed number of concurrent
permits and a *bounded* queue, so one slow dependency can exhaust only
its own compartment. A **deadline** is one instant for the whole request;
every downstream call gets `min(its own timeout, time left)`, and calls
with no time left are not made at all. Pass the deadline on as a
*duration* (gRPC's `grpc-timeout: 700m`), never as a wall-clock instant:
the other machine's clock is not yours.

### 6 · Idempotency keys: make retries safe

A retried `POST /charges` must not charge twice. The client sends an
`Idempotency-Key`; the server stores the *response* under it and replays
it for repeats. The key is claimed before the handler runs (so a
concurrent duplicate is refused), and only final answers (2xx, 4xx) are
stored; a 5xx or a crash releases the key so a retry can run for real.
That last rule is a policy, not a law: it is safe only when a 5xx means
nothing took effect (the handler rolled back). Stripe, which cannot be
sure, stores and replays a 500 too once the handler has started.

## The details that bite

1. **Cap the refill.** Without `Math.min(capacity, ...)` a client idle for
   an hour gets an hour of traffic in one burst (01).
2. **Fractions of a token are real.** Integer refill math plus
   `last = now` throws away half-tokens forever; at 2/s polled every
   250 ms, nothing ever refills (01).
3. **A refused request takes nothing**, in a bucket and in a log. Logging
   refused requests locks a retrying client out forever (01, 03).
4. **Align fixed windows to the clock** (`t / w`), not to each client's
   first request, or nodes disagree about which window it is (02).
5. **Expiry boundaries**: a request at `t` counts while `now < t + w`.
   `<` versus `<=` is the difference between a limit of N and N + 1 (02, 03).
6. **Attempts are not retries.** 3 attempts means 2 sleeps, and no sleep
   after the last failure (04).
7. **`base * 2^n` overflows** to a negative delay around n = 57 with no
   warning. Stop doubling at half the cap (04).
8. **Don't swallow the history.** Attach earlier failures with
   `addSuppressed` so the final exception tells the whole story (04).
9. **Consecutive means consecutive.** One success resets a breaker's
   failure count (05).
10. **A failed half-open trial restarts the cool-down from now**, not from
    when the breaker first opened (05).
11. **Cancelling a waiter frees no permit.** Promote someone only when a
    running task finishes (06).
12. **A child deadline can only be stricter** than its parent (07).
13. **Zero time left means don't call**, not "call with timeout 0" (07).
14. **Claim the key before running, release it on every way out.** A
    handler exception that skips the release leaves the key "in progress"
    forever (08).
15. **Same key, different body is a bug, not a replay.** Refuse it rather
    than confirming a request that never ran (08).

## Where you will meet this

| idea | production form |
| --- | --- |
| token bucket / windows | API gateways, Envoy and nginx rate limits, Redis + Lua limiters, Bucket4j |
| backoff + jitter | AWS SDK retry modes, gRPC retry policy, Resilience4j `Retry` |
| circuit breaker | Resilience4j, Polly (.NET), Envoy outlier detection, Hystrix (retired) |
| bulkhead | thread-pool per dependency, `Semaphore`, Resilience4j `Bulkhead` |
| deadlines | gRPC deadlines, Go `context.WithTimeout`, Finagle |
| idempotency keys | Stripe, Adyen, PayPal APIs; the IETF `Idempotency-Key` header draft |

Links: this module's idempotency store is module 08's "charge exactly
once" lifted to the HTTP layer. Caches from **09** are often the first
thing to fail open when a breaker trips ("serve stale"). **12 · messaging
and events** applies the same ideas asynchronously: consumer retries,
dead-letter queues and dedupe are backoff, circuit breaking and
idempotency on a queue instead of a socket.

## Exercises

| # | file | ★ | what you build |
| --- | --- | --- | --- |
| 01 | `01-token-bucket.java` | ★☆☆ | a lazily refilled token bucket with bursts and fractions |
| 02 | `02-fixed-window.java` | ★☆☆ | per-client fixed window counters, aligned to the clock |
| 03 | `03-sliding-window-log.java` | ★★☆ | an exact sliding log with `Retry-After` |
| 04 | `04-retry-backoff.java` | ★★☆ | capped exponential backoff, jitter, selective retries |
| 05 | `05-circuit-breaker.java` | ★★☆ | CLOSED / OPEN / HALF_OPEN on an injected clock |
| 06 | `06-bulkhead.java` | ★★☆ | concurrency permits with a bounded FIFO queue |
| 07 | `07-deadline-budget.java` | ★★★ | deadlines, child budgets and `grpc-timeout` propagation |
| 08 | `08-idempotency-keys.java` | ★★★ | idempotency keys with replay, fingerprints and TTL |

Do the warm-ups and core in order. Stretch if time allows.

---

**Next:** `bootcamp/11-partitioning-and-ids`
