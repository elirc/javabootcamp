// ─────────────────────────────────────────────────────────────────────────
//  07 · the breaker that stays open                         ★★☆ core
//  concepts: debugging · circuit breaker · state machines · injected clock
//  run: java -cp ../../_lib/out 07-breaker-stuck-open.java
// ─────────────────────────────────────────────────────────────────────────
//
//  DEBUG HUNT: the code below is complete and compiles; it has bugs. Run
//  it, read the failing tests, find and fix the bugs. Do not edit tests.
//  Intended: CircuitBreaker(threshold, cooldownMs, clock). CLOSED: calls
//  go through; `threshold` failures IN A ROW open it (a success resets
//  the count). OPEN: calls are rejected with IllegalStateException
//  "circuit open" without running, until cooldownMs have passed since it
//  OPENED; then the next call is a trial (HALF_OPEN). A successful trial
//  closes it with a fresh count; a failed trial opens it again (a new
//  cooldown from that moment). The action's exception is rethrown.
//  Bug reports:
//    "The dependency recovered after 30 s, but our breaker (cooldown
//     10 s) stayed open for an hour. We call it once a second."
//    "Since the incident, a single error opens the breaker. The
//     threshold is 3."
//
//      threshold 2, cooldown 1000: fail, fail → OPEN; calls at 500 and
//      900 rejected; call at 1000 runs as a trial
//
//  hint: log state and openedAt after every call of a failing test.

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
                    openedAt = now;
                    throw new IllegalStateException("circuit open");
                }
            }
            try {
                T result = action.call();
                state = State.CLOSED;
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
