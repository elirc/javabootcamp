// ─────────────────────────────────────────────────────────────────────────
//  07 · the breaker that stays open — SOLUTION              ★★☆ core
//  concepts: debugging · circuit breaker · state machines · injected clock
//  run: java -cp ../../_lib/out 07-breaker-stuck-open.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  1. Reproduce report 1: open the breaker, then call every 100 ms with a
//     healthy dependency. It never half-opens. Narrow: log openedAt after
//     each rejected call; it moves forward every time. The reject branch
//     resets openedAt = now, so as long as calls arrive faster than the
//     cooldown, the cooldown never completes: a busy service is locked
//     out longest, exactly when it needs the dependency most. Fix: a
//     rejection changes nothing; only OPENING sets openedAt. Pinned by
//     "the cooldown counts from opening, not from the last rejected
//     call". (A test that waits once for the full cooldown with no calls
//     in between would pass, which is how this ships.)
//  2. Reproduce report 2: open, recover with a good trial, then fail
//     once: it opens immediately. failures is never reset, so it still
//     says 3 after the trial. Same root cause in CLOSED: fail, fail,
//     succeed, fail opens a threshold-3 breaker, because "in a row" was
//     never implemented. Fix: failures = 0 on every success. Pinned by
//     "a success resets the failure count" and "a successful trial
//     closes it with a fresh count".
//  The state machine is three states and four transitions; write them as
//  a table (module 08) and test each arrow, including the arrows that
//  must NOT fire. Module 10, exercise 05 builds the breaker from scratch.

import static bootcamp.Check.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

class Exercise {

    enum State { CLOSED, OPEN, HALF_OPEN }

    static final class CircuitBreaker {
        private final int threshold;
        private final long cooldownMs;
        private final LongSupplier clock;
        private State state = State.CLOSED;
        private int failures;
        private long openedAt;

        CircuitBreaker(int threshold, long cooldownMs, LongSupplier clock) {
            this.threshold = threshold;
            this.cooldownMs = cooldownMs;
            this.clock = clock;
        }

        <T> T call(Callable<T> action) throws Exception {
            long now = clock.getAsLong();
            if (state == State.OPEN) {
                if (now - openedAt >= cooldownMs) {
                    state = State.HALF_OPEN;
                } else {
                    throw new IllegalStateException("circuit open");
                }
            }
            try {
                T result = action.call();
                state = State.CLOSED;
                failures = 0;
                return result;
            } catch (Exception e) {
                failures++;
                if (state == State.HALF_OPEN || failures >= threshold) {
                    state = State.OPEN;
                    openedAt = now;
                }
                throw e;
            }
        }

        State state() {
            return state;
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        Callable<String> ok = () -> "ok";
        Callable<String> down = () -> {
            throw new IOException("down");
        };
        test("stays closed below the threshold and passes results through", () -> {
            long[] now = {0};
            var b = new CircuitBreaker(3, 1000, () -> now[0]);
            raises(IOException.class, () -> b.call(down));
            raises(IOException.class, () -> b.call(down));
            eq(b.state(), State.CLOSED);
            eq(b.call(ok), "ok");
        });
        test("opens after threshold failures in a row and rejects without running", () -> {
            long[] now = {0};
            var b = new CircuitBreaker(2, 1000, () -> now[0]);
            raises(IOException.class, () -> b.call(down));
            raises(IOException.class, () -> b.call(down));
            eq(b.state(), State.OPEN);
            int[] runs = {0};
            raises(IllegalStateException.class, () -> b.call(() -> ++runs[0]), "circuit open");
            eq(runs[0], 0);
        });
        test("a success resets the failure count", () -> {
            long[] now = {0};
            var b = new CircuitBreaker(3, 1000, () -> now[0]);
            raises(IOException.class, () -> b.call(down));
            raises(IOException.class, () -> b.call(down));
            b.call(ok);
            raises(IOException.class, () -> b.call(down));
            raises(IOException.class, () -> b.call(down));
            eq(b.state(), State.CLOSED);
        });
        test("the cooldown counts from opening, not from the last rejected call", () -> {
            long[] now = {0};
            var b = new CircuitBreaker(2, 1000, () -> now[0]);
            raises(IOException.class, () -> b.call(down));
            raises(IOException.class, () -> b.call(down));
            for (long t = 100; t < 1000; t += 100) {
                now[0] = t;
                raises(IllegalStateException.class, () -> b.call(ok));
            }
            now[0] = 1000;
            eq(b.call(ok), "ok");
            eq(b.state(), State.CLOSED);
        });
        test("a successful trial closes it with a fresh count", () -> {
            long[] now = {0};
            var b = new CircuitBreaker(3, 1000, () -> now[0]);
            for (int i = 0; i < 3; i++) raises(IOException.class, () -> b.call(down));
            now[0] = 1000;
            b.call(ok);
            raises(IOException.class, () -> b.call(down));
            eq(b.state(), State.CLOSED);
        });
        test("a failed trial opens it again with a new cooldown", () -> {
            long[] now = {0};
            var b = new CircuitBreaker(3, 1000, () -> now[0]);
            for (int i = 0; i < 3; i++) raises(IOException.class, () -> b.call(down));
            now[0] = 1500;
            raises(IOException.class, () -> b.call(down));
            eq(b.state(), State.OPEN);
            now[0] = 2400;
            raises(IllegalStateException.class, () -> b.call(ok));
            now[0] = 2500;
            eq(b.call(ok), "ok");
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
