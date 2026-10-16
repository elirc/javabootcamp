// ─────────────────────────────────────────────────────────────────────────
//  05 · circuit breaker — SOLUTION                          ★★☆ core
//  concepts: circuit breaker · fail fast · half-open trial · state machine
//  run: java -cp ../../_lib/out 05-circuit-breaker.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Retries (04) help with blips; against a dependency that is DOWN they
//  multiply the load on it and make every caller wait for a timeout. A
//  breaker notices the pattern and fails fast instead:
//    CLOSED    calls go through; count consecutive failures
//    OPEN      calls are refused at once, the dependency gets to recover
//    HALF_OPEN after the cool-down, one trial call decides: close or reopen
//  HALF_OPEN is not stored; it is derived. An open breaker whose openedAt
//  + openMillis has arrived IS half-open, so state() and call() both ask
//  the same question of the clock and can never disagree. (Storing it
//  would need a timer to flip it.)
//  Details the tests pin:
//  - Consecutive means consecutive: one success in CLOSED resets the
//    count, or a service failing 1 call in 10 would trip it eventually.
//  - A failed trial reopens with a FRESH cool-down from now, not from the
//    original openedAt (which is already in the past: it would be
//    half-open again immediately).
//  - A successful trial resets the count too; otherwise the next single
//    failure would reopen it.
//  - The original exception is rethrown; the breaker observes, it does
//    not translate. Only rejections are a CircuitOpenException.
//  Resilience4j adds sliding-window failure RATES and several
//  trial calls, but the state machine is exactly this one.

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
            if (failureThreshold < 1) throw new IllegalArgumentException("threshold must be at least 1: " + failureThreshold);
            if (openMillis < 1) throw new IllegalArgumentException("open time must be at least 1 ms: " + openMillis);
            this.failureThreshold = failureThreshold;
            this.openMillis = openMillis;
            this.clock = clock;
        }

        State state() {
            if (!open) return State.CLOSED;
            return clock.getAsLong() >= openedAt + openMillis ? State.HALF_OPEN : State.OPEN;
        }

        <T> T call(Supplier<T> op) {
            var state = state();
            if (state == State.OPEN) {
                rejected++;
                throw new CircuitOpenException();
            }
            T result;
            try {
                result = op.get();
            } catch (RuntimeException e) {
                if (state == State.HALF_OPEN || ++consecutiveFailures >= failureThreshold) trip();
                throw e;
            }
            open = false;
            consecutiveFailures = 0;
            return result;
        }

        int rejected() {
            return rejected;
        }

        private void trip() {
            open = true;
            openedAt = clock.getAsLong();
            consecutiveFailures = 0;
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
