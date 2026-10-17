// ─────────────────────────────────────────────────────────────────────────
//  03 · the retry that never stops — SOLUTION               ★★☆ core
//  concepts: debugging · retry loops · exponential backoff · shift overflow
//  run: java -cp ../../_lib/out 03-retry-never-stops.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Three bugs, found in this order because each hides the next.
//  1. Reproduce "retried until killed": a call that always fails,
//     maxAttempts 3. The test's sleeper gives up at 100 sleeps, and all
//     100 delays are 100 ms. Narrow: print attempt in the catch; it is 1
//     every time. It is declared INSIDE the while loop, so every pass
//     starts over. Fix: declare it once, before the loop.
//  2. Re-run: now 4 calls for maxAttempts 3. The check runs after the
//     call that failed, so "attempt > maxAttempts" lets one extra call
//     through. Fix: attempt >= maxAttempts. Pinned by "stops after
//     maxAttempts calls in total" and "maxAttempts 1 means one call".
//  3. Re-run: the 80-attempt test fails. baseMs << (k - 1) shifts bits
//     off the top of a long: 100 << 57 is already negative, and shift
//     distances wrap modulo 64, so later delays come back tiny. Math.min
//     with the cap does not help once the value is negative. Fix: double
//     a running delay and stop at the cap, so it never overflows.
//  A real retry loop also needs: only retry RETRYABLE failures, add
//  jitter, and respect an overall deadline (module 10, exercises 04 and
//  07). Every one of those is a counter or a clock that must live
//  OUTSIDE the loop body.

import static bootcamp.Check.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

class Exercise {

    interface Sleeper {
        void sleep(long ms);
    }

    static <T> T retry(Callable<T> call, int maxAttempts, long baseMs, long capMs, Sleeper sleeper) throws Exception {
        int attempt = 1;
        long delay = Math.min(capMs, baseMs);
        while (true) {
            try {
                return call.call();
            } catch (Exception e) {
                if (attempt >= maxAttempts) throw e;
                sleeper.sleep(delay);
                delay = delay > capMs / 2 ? capMs : delay * 2;
                attempt++;
            }
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("returns the first success without sleeping", () -> {
            var sleeps = new ArrayList<Long>();
            eq(retry(() -> "ok", 3, 100, 10_000, sleeps::add), "ok");
            eq(sleeps, List.of());
        });
        test("retries until the call succeeds, doubling the delay", () -> {
            var sleeps = new ArrayList<Long>();
            int[] calls = {0};
            Sleeper sleeper = ms -> {
                if (sleeps.size() >= 100) throw new IllegalStateException("runaway retry");
                sleeps.add(ms);
            };
            var result = retry(() -> {
                if (++calls[0] < 3) throw new IOException("down");
                return "ok";
            }, 5, 100, 10_000, sleeper);
            eq(result, "ok");
            eq(calls[0], 3);
            eq(sleeps, List.of(100L, 200L));
        });
        test("stops after maxAttempts calls in total and rethrows", () -> {
            var sleeps = new ArrayList<Long>();
            int[] calls = {0};
            Sleeper sleeper = ms -> {
                if (sleeps.size() >= 100) throw new IllegalStateException("runaway retry");
                sleeps.add(ms);
            };
            raises(IOException.class, () -> retry(() -> {
                calls[0]++;
                throw new IOException("down #" + calls[0]);
            }, 3, 100, 10_000, sleeper), "down #3");
            eq(calls[0], 3);
            eq(sleeps, List.of(100L, 200L));
        });
        test("maxAttempts 1 means one call and no sleep", () -> {
            var sleeps = new ArrayList<Long>();
            int[] calls = {0};
            Sleeper sleeper = ms -> {
                if (sleeps.size() >= 100) throw new IllegalStateException("runaway retry");
                sleeps.add(ms);
            };
            raises(IOException.class, () -> retry(() -> {
                calls[0]++;
                throw new IOException("down");
            }, 1, 100, 10_000, sleeper));
            eq(calls[0], 1);
            eq(sleeps, List.of());
        });
        test("the delay doubles up to the cap and stays there", () -> {
            var sleeps = new ArrayList<Long>();
            Sleeper sleeper = ms -> {
                if (sleeps.size() >= 100) throw new IllegalStateException("runaway retry");
                sleeps.add(ms);
            };
            raises(IOException.class, () -> retry(() -> {
                throw new IOException("down");
            }, 6, 100, 500, sleeper));
            eq(sleeps, List.of(100L, 200L, 400L, 500L, 500L));
        });
        test("a very long outage never sleeps a negative or uncapped time", () -> {
            var sleeps = new ArrayList<Long>();
            Sleeper sleeper = ms -> {
                if (sleeps.size() >= 100) throw new IllegalStateException("runaway retry");
                sleeps.add(ms);
            };
            raises(IOException.class, () -> retry(() -> {
                throw new IOException("down");
            }, 80, 100, 30_000, sleeper));
            eq(sleeps.size(), 79);
            ok(sleeps.stream().allMatch(ms -> ms >= 100 && ms <= 30_000), "every delay in [100, 30000]: " + sleeps);
            eq(sleeps.get(78), 30_000L);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
