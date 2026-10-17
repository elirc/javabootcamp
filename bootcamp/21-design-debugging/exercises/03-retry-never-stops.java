// ─────────────────────────────────────────────────────────────────────────
//  03 · the retry that never stops                          ★★☆ core
//  concepts: debugging · retry loops · exponential backoff · shift overflow
//  run: java -cp ../../_lib/out 03-retry-never-stops.java
// ─────────────────────────────────────────────────────────────────────────
//
//  DEBUG HUNT: the code below is complete and compiles; it has bugs. Run
//  it, read the failing tests, find and fix the bugs. Do not edit tests.
//  Intended: retry(call, maxAttempts, baseMs, capMs, sleeper) calls at
//  most maxAttempts times IN TOTAL and returns the first success. When
//  attempts run out it rethrows the last exception. Before retry number
//  k (k = 1, 2, ...) it sleeps min(capMs, baseMs * 2^(k-1)): never
//  negative, never above the cap, however many attempts.
//  (The tests' sleeper gives up with "runaway retry" after 100 sleeps,
//  so a loop that never ends fails instead of hanging.)
//  Bug reports:
//    "When the payment API went down, one checkout retried until the
//     pod was killed. maxAttempts is 3."
//    "After a 2-hour outage the retry delays went negative."
//
//      fails twice, then "ok", 5 attempts, base 100 → sleeps [100, 200]
//
//  hint: count the calls and print each delay; compare with the spec.

import static bootcamp.Check.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

class Exercise {

    interface Sleeper {
        void sleep(long ms);
    }

    static <T> T retry(Callable<T> call, int maxAttempts, long baseMs, long capMs, Sleeper sleeper) throws Exception {
        while (true) {
            int attempt = 1;
            try {
                return call.call();
            } catch (Exception e) {
                if (attempt > maxAttempts) throw e;
                sleeper.sleep(Math.min(capMs, baseMs << (attempt - 1)));
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
