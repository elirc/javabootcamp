// ─────────────────────────────────────────────────────────────────────────
//  08 · try, try again — SOLUTION                           ★★★ stretch
//  concepts: Supplier · exceptions as values · suppressed exceptions · loops
//  run: java -cp ../../_lib/out 08-retry.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A Supplier<T> is "a computation you can run later", which is what makes
//  retry possible at all: the caller hands over the WORK, not its result.
//  The loop collects every failure in a list. After the loop, the last
//  one is what the caller sees — usually the most relevant — and every
//  earlier one is attached to it with addSuppressed, oldest first. The
//  tempting shortcut, e.addSuppressed(previous) inside the catch, builds
//  a CHAIN instead (3rd holds 2nd, 2nd holds 1st): nothing is lost, but
//  getSuppressed() on the thrown exception shows only one of them.
//  Why not just log and drop the earlier ones? Because the first failure
//  is often the real cause (a timeout) and later ones are noise (a
//  connection pool now exhausted). Suppressed exceptions show up in the
//  stack trace under "Suppressed:", so nothing is lost.
//  Validate attempts BEFORE the loop: with attempts = 0 the loop never
//  runs, the list is empty, and failures.get(-1) crashes with a confusing
//  IndexOutOfBoundsException. Fail fast with IllegalArgumentException.
//  Real retry adds a delay between attempts (with backoff) and only
//  retries errors that are worth retrying — never a validation error.
//  Here there is no sleep, so the tests stay fast and deterministic.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    static <T> T retry(int attempts, Supplier<T> action) {
        if (attempts < 1) throw new IllegalArgumentException("attempts must be at least 1: " + attempts);
        var failures = new ArrayList<RuntimeException>();
        for (int i = 0; i < attempts; i++) {
            try {
                return action.get();
            } catch (RuntimeException e) {
                failures.add(e);
            }
        }
        var last = failures.get(failures.size() - 1);
        for (var earlier : failures.subList(0, failures.size() - 1)) last.addSuppressed(earlier);
        throw last;
    }

    static Supplier<Integer> failsThenReturns(int failures, int value, int[] calls) {
        return () -> {
            calls[0]++;
            if (calls[0] <= failures) throw new IllegalStateException("fail " + calls[0]);
            return value;
        };
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("returns the first success with one call", () -> {
            var calls = new int[1];
            eq(retry(3, failsThenReturns(0, 7, calls)), 7);
            eq(calls[0], 1);
        });
        test("keeps trying until it works", () -> {
            var calls = new int[1];
            eq(retry(3, failsThenReturns(2, 42, calls)), 42);
            eq(calls[0], 3);
        });
        test("stops after the allowed attempts", () -> {
            var calls = new int[1];
            raises(IllegalStateException.class, () -> retry(3, failsThenReturns(10, 0, calls)));
            eq(calls[0], 3);
        });
        test("rethrows the last failure", () -> {
            var calls = new int[1];
            var e = raises(IllegalStateException.class, () -> retry(3, failsThenReturns(10, 0, calls)));
            eq(e.getMessage(), "fail 3");
        });
        test("keeps every earlier failure as suppressed, oldest first", () -> {
            var calls = new int[1];
            var e = raises(IllegalStateException.class, () -> retry(3, failsThenReturns(10, 0, calls)));
            var earlier = new ArrayList<String>();
            for (var s : e.getSuppressed()) earlier.add(s.getMessage());
            eq(earlier, List.of("fail 1", "fail 2"));
        });
        test("refuses fewer than one attempt", () ->
            raises(IllegalArgumentException.class, () -> retry(0, () -> "never")));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
