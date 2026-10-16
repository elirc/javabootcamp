// ─────────────────────────────────────────────────────────────────────────
//  05 · circuit breaker                                     ★★☆ core
//  concepts: circuit breaker · fail fast · half-open trial · state machine
//  run: java -cp ../../_lib/out 05-circuit-breaker.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Finish CircuitBreaker. CLOSED: call(op) runs op; a success resets the
//  consecutive-failure count; failureThreshold consecutive failures open
//  the breaker at that moment. OPEN: call throws CircuitOpenException
//  WITHOUT running op and counts it in rejected(). From openedAt +
//  openMillis on, state() is HALF_OPEN and the next call is a trial: a
//  success closes the breaker (count back to 0); a failure opens it
//  again with a fresh cool-down from now. op's own exception (any
//  RuntimeException) is always rethrown unchanged. A threshold or
//  openMillis below 1 is an IllegalArgumentException.
//
//      threshold 3: fail, fail, ok, fail, fail → still CLOSED
//      opened at t=500, openMillis 1000: t=1499 → OPEN, t=1500 → HALF_OPEN
//
//  hint: HALF_OPEN does not need storing; it can be worked out from time.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

class Exercise {

    enum State { CLOSED, OPEN, HALF_OPEN }

    static final class CircuitOpenException extends RuntimeException {
        CircuitOpenException() {
            super("circuit open");
        }
    }

    static final class CircuitBreaker {
        private final int failureThreshold;
        private final long openMillis;
        private final LongSupplier clock;
        private int consecutiveFailures;
        private boolean open;
        private long openedAt;
        private int rejected;

        CircuitBreaker(int failureThreshold, long openMillis, LongSupplier clock) {
            throw new UnsupportedOperationException("TODO");
        }

        State state() {
            throw new UnsupportedOperationException("TODO");
        }

        <T> T call(Supplier<T> op) {
            throw new UnsupportedOperationException("TODO");
        }

        int rejected() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class FakeClock implements LongSupplier {
        long now;

        public long getAsLong() {
            return now;
        }
    }

    static final class Service {
        int calls;
        boolean down;

        String ping() {
            calls++;
            if (down) throw new IllegalStateException("503 from inventory");
            return "pong";
        }
    }

    static void failTimes(CircuitBreaker cb, Service s, int n) {
        s.down = true;
        for (int i = 0; i < n; i++) {
            try {
                cb.call(s::ping);
            } catch (IllegalStateException | CircuitOpenException expected) {
                // the breaker's or the service's: both are fine here
            }
        }
        s.down = false;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("closed: results and failures pass straight through", () -> {
            var s = new Service();
            var cb = new CircuitBreaker(3, 1_000, new FakeClock());
            eq(cb.call(s::ping), "pong");
            s.down = true;
            raises(IllegalStateException.class, () -> cb.call(s::ping), "503");
            raises(IllegalStateException.class, () -> cb.call(s::ping), "503");
            eq(cb.state(), State.CLOSED);
        });
        test("opens after threshold consecutive failures", () -> {
            var s = new Service();
            var cb = new CircuitBreaker(3, 1_000, new FakeClock());
            failTimes(cb, s, 2);
            eq(cb.state(), State.CLOSED);
            failTimes(cb, s, 1);
            eq(cb.state(), State.OPEN);
        });
        test("a success resets the consecutive count", () -> {
            var s = new Service();
            var cb = new CircuitBreaker(3, 1_000, new FakeClock());
            failTimes(cb, s, 2);
            cb.call(s::ping);
            failTimes(cb, s, 2);
            eq(cb.state(), State.CLOSED);
        });
        test("open: refuses without calling the dependency", () -> {
            var s = new Service();
            var cb = new CircuitBreaker(2, 1_000, new FakeClock());
            failTimes(cb, s, 2);
            int before = s.calls;
            raises(CircuitOpenException.class, () -> cb.call(s::ping));
            raises(CircuitOpenException.class, () -> cb.call(s::ping));
            eq(s.calls, before);
            eq(cb.rejected(), 2);
        });
        test("half-open exactly when the cool-down ends", () -> {
            var clock = new FakeClock();
            var s = new Service();
            var cb = new CircuitBreaker(1, 1_000, clock);
            clock.now = 500;
            failTimes(cb, s, 1);
            clock.now = 1_499;
            eq(cb.state(), State.OPEN);
            clock.now = 1_500;
            eq(cb.state(), State.HALF_OPEN);
        });
        test("a successful trial closes it and the count starts over", () -> {
            var clock = new FakeClock();
            var s = new Service();
            var cb = new CircuitBreaker(2, 1_000, clock);
            failTimes(cb, s, 2);
            clock.now = 1_000;
            eq(cb.call(s::ping), "pong");
            eq(cb.state(), State.CLOSED);
            failTimes(cb, s, 1);
            eq(cb.state(), State.CLOSED);
        });
        test("a failed trial reopens with a fresh cool-down", () -> {
            var clock = new FakeClock();
            var s = new Service();
            var cb = new CircuitBreaker(3, 1_000, clock);
            failTimes(cb, s, 3);
            clock.now = 1_500;
            s.down = true;
            raises(IllegalStateException.class, () -> cb.call(s::ping));
            eq(cb.state(), State.OPEN);
            clock.now = 2_499;
            raises(CircuitOpenException.class, () -> cb.call(s::ping));
            clock.now = 2_500;
            eq(cb.state(), State.HALF_OPEN);
        });
        test("refuses bad settings", () -> {
            var clock = new FakeClock();
            raises(IllegalArgumentException.class, () -> new CircuitBreaker(0, 1_000, clock));
            raises(IllegalArgumentException.class, () -> new CircuitBreaker(3, 0, clock));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
