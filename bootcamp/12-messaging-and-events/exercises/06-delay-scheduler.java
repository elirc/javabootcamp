// ─────────────────────────────────────────────────────────────────────────
//  06 · a delay and priority scheduler                      ★★☆ core
//  concepts: delayed jobs · priorities · fixed-rate timers · injected clocks
//  run: java -cp ../../_lib/out 06-delay-scheduler.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Delayed jobs ("retry in 30 s", "send the reminder tomorrow") need a
//  scheduler. Finish the nested class Scheduler(clock), clock giving ms.
//  schedule(name, priority, delayMs) runs name once at now + delayMs.
//  every(name, priority, periodMs) runs at now + period, then again each
//  period at FIXED RATE: the next run is the previous run time + period,
//  skipping any slot that is already <= now when it runs (so a late timer
//  runs once, not in a burst). runDue() reads the clock once and runs
//  every task whose time is <= now, in order: higher priority first, then
//  earlier run time, then the order the tasks were first scheduled; it
//  returns their names. cancel(name) removes a pending task (false if
//  there is none). nextRunAt() is the earliest pending run time. Negative
//  delay, period < 1, or a name already pending →
//  IllegalArgumentException.
//
//      now 0: schedule("a", 1, 50), every("tick", 0, 100)
//      now 100: runDue() → [a, tick]     nextRunAt() → 200
//      now 450: runDue() → [tick]        nextRunAt() → 500  (not 300, 550)
//
//  hint: next = runAt + period; while next <= now, add another period.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    static final class Task {
        final String name;
        final int priority;
        final long periodMs;   // 0 = runs once
        final long order;      // when it was first scheduled
        long runAt;

        Task(String name, int priority, long runAt, long periodMs, long order) {
            this.name = name;
            this.priority = priority;
            this.runAt = runAt;
            this.periodMs = periodMs;
            this.order = order;
        }
    }

    static final class Scheduler {
        private final LongSupplier clock;
        private final Map<String, Task> pending = new LinkedHashMap<>();
        private long scheduled;

        Scheduler(LongSupplier clock) {
            this.clock = clock;
        }

        void schedule(String name, int priority, long delayMs) {
            throw new UnsupportedOperationException("TODO");
        }

        void every(String name, int priority, long periodMs) {
            throw new UnsupportedOperationException("TODO");
        }

        List<String> runDue() {
            throw new UnsupportedOperationException("TODO");
        }

        boolean cancel(String name) {
            throw new UnsupportedOperationException("TODO");
        }

        Optional<Long> nextRunAt() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("a task runs at its time, not before", () -> {
            var now = new long[] {0};
            var s = new Scheduler(() -> now[0]);
            s.schedule("a", 0, 50);
            now[0] = 49;
            eq(s.runDue(), List.of());
            now[0] = 50;
            eq(s.runDue(), List.of("a"));
            eq(s.runDue(), List.of());
        });
        test("due tasks run highest priority first", () -> {
            var now = new long[] {0};
            var s = new Scheduler(() -> now[0]);
            s.schedule("low", 1, 10);
            s.schedule("high", 9, 30);
            s.schedule("mid", 5, 20);
            now[0] = 100;
            eq(s.runDue(), List.of("high", "mid", "low"));
        });
        test("equal priority runs by time, then first come first served", () -> {
            var now = new long[] {0};
            var s = new Scheduler(() -> now[0]);
            s.schedule("late", 1, 40);
            s.schedule("first", 1, 10);
            s.schedule("second", 1, 10);
            now[0] = 100;
            eq(s.runDue(), List.of("first", "second", "late"));
        });
        test("cancel removes a pending task", () -> {
            var now = new long[] {0};
            var s = new Scheduler(() -> now[0]);
            s.schedule("a", 0, 10);
            s.schedule("b", 0, 10);
            eq(s.cancel("a"), true);
            eq(s.cancel("a"), false);
            eq(s.cancel("nope"), false);
            now[0] = 10;
            eq(s.runDue(), List.of("b"));
            eq(s.cancel("b"), false);
        });
        test("a recurring task runs once per period", () -> {
            var now = new long[] {0};
            var s = new Scheduler(() -> now[0]);
            s.every("tick", 0, 100);
            eq(s.nextRunAt(), Optional.of(100L));
            now[0] = 100;
            eq(s.runDue(), List.of("tick"));
            now[0] = 150;
            eq(s.runDue(), List.of());
            now[0] = 200;
            eq(s.runDue(), List.of("tick"));
            eq(s.nextRunAt(), Optional.of(300L));
        });
        test("a late recurring task runs once and keeps its rate", () -> {
            var now = new long[] {0};
            var s = new Scheduler(() -> now[0]);
            s.every("tick", 0, 100);
            now[0] = 450;
            eq(s.runDue(), List.of("tick"));
            eq(s.nextRunAt(), Optional.of(500L));
            now[0] = 500;
            eq(s.runDue(), List.of("tick"));
        });
        test("a mix of one-off and recurring tasks", () -> {
            var now = new long[] {0};
            var s = new Scheduler(() -> now[0]);
            s.schedule("a", 1, 50);
            s.every("tick", 0, 100);
            now[0] = 100;
            eq(s.runDue(), List.of("a", "tick"));
            eq(s.nextRunAt(), Optional.of(200L));
            s.cancel("tick");
            eq(s.nextRunAt(), Optional.empty());
        });
        test("refuses negative delays, empty periods and duplicate names", () -> {
            var s = new Scheduler(() -> 0);
            raises(IllegalArgumentException.class, () -> s.schedule("a", 0, -1));
            raises(IllegalArgumentException.class, () -> s.every("b", 0, 0));
            s.schedule("a", 0, 0);
            raises(IllegalArgumentException.class, () -> s.schedule("a", 0, 5));
            raises(IllegalArgumentException.class, () -> s.every("a", 0, 5));
        });
        test("a finished task's name can be scheduled again", () -> {
            var now = new long[] {0};
            var s = new Scheduler(() -> now[0]);
            s.schedule("a", 0, 0);
            eq(s.runDue(), List.of("a"));
            s.schedule("a", 0, 5);
            now[0] = 5;
            eq(s.runDue(), List.of("a"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
