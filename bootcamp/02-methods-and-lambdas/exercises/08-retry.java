// ─────────────────────────────────────────────────────────────────────────
//  08 · try, try again                                      ★★★ stretch
//  concepts: Supplier · exceptions as values · suppressed exceptions · loops
//  run: java -cp ../../_lib/out 08-retry.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A bank simulator talks to a flaky ledger service. Write retry: call the
//  action up to `attempts` times and return the first successful result.
//  If every attempt throws, rethrow the LAST RuntimeException, with each
//  earlier failure attached DIRECTLY to it as a suppressed exception,
//  oldest first. attempts below 1 is a programming error:
//  IllegalArgumentException.
//
//      retry(3, () -> "ok")                    → "ok" (one call)
//      retry(3, failsTwiceThenReturns(42))     → 42   (three calls)
//      retry(3, alwaysFails)                   → throws the 3rd failure;
//                                    its suppressed: [1st failure, 2nd]
//
//  hint: Throwable has addSuppressed(...) and getSuppressed().

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    static <T> T retry(int attempts, Supplier<T> action) {
        throw new UnsupportedOperationException("TODO");
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
