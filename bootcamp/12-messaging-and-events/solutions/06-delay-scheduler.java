// ─────────────────────────────────────────────────────────────────────────
//  06 · a delay and priority scheduler — SOLUTION           ★★☆ core
//  concepts: delayed jobs · priorities · fixed-rate timers · injected clocks
//  run: java -cp ../../_lib/out 06-delay-scheduler.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Every queue that offers delayed delivery (SQS DelaySeconds, RabbitMQ's
//  delayed-message plugin, Sidekiq's scheduled set, a ScheduledExecutor)
//  is this: a set of (runAt, task) entries and a loop that runs what is
//  due. Two orderings matter. Among due tasks, priority decides; ties go
//  to the earlier run time, then to FIFO by scheduling order, so the
//  result is deterministic. A single PriorityQueue ordered by priority
//  alone would run a high-priority task before its time, so first filter
//  by runAt <= now, then sort the due ones.
//  Reading the clock ONCE per runDue gives the batch a consistent "now".
//  The recurring task is where the bugs live. Fixed DELAY (next = now +
//  period) drifts later every time a run is late. Fixed RATE (next =
//  runAt + period) keeps the schedule anchored, but if the process was
//  paused for many periods, a naive loop fires every missed slot at once:
//  a thundering burst. Skipping slots that are already <= now gives "run
//  once, then get back on schedule", which is how cron-like schedulers and
//  java.util.Timer users usually want it. Note the task keeps its first
//  scheduling order for tie-breaks, so a timer does not lose its place.
//  cancel and the "already pending" check both look the name up, so a
//  Map from name to entry keeps them O(1); the clock is a LongSupplier so
//  the tests can move time.

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
            if (delayMs < 0) throw new IllegalArgumentException("negative delay: " + delayMs);
            add(new Task(name, priority, clock.getAsLong() + delayMs, 0, scheduled));
        }

        void every(String name, int priority, long periodMs) {
            if (periodMs < 1) throw new IllegalArgumentException("period must be at least 1: " + periodMs);
            add(new Task(name, priority, clock.getAsLong() + periodMs, periodMs, scheduled));
        }

        private void add(Task task) {
            if (pending.containsKey(task.name)) throw new IllegalArgumentException("already pending: " + task.name);
            pending.put(task.name, task);
            scheduled++;
        }

        List<String> runDue() {
            long now = clock.getAsLong();
            var due = new ArrayList<Task>();
            for (var t : pending.values()) if (t.runAt <= now) due.add(t);
            due.sort(Comparator.comparingInt((Task t) -> -t.priority)
                .thenComparingLong(t -> t.runAt)
                .thenComparingLong(t -> t.order));
            var ran = new ArrayList<String>();
            for (var t : due) {
                ran.add(t.name);
                if (t.periodMs == 0) {
                    pending.remove(t.name);
                } else {
                    long next = t.runAt + t.periodMs;
                    while (next <= now) next += t.periodMs;
                    t.runAt = next;
                }
            }
            return ran;
        }

        boolean cancel(String name) {
            return pending.remove(name) != null;
        }

        Optional<Long> nextRunAt() {
            return pending.values().stream().map(t -> t.runAt).min(Long::compare);
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
            s.schedule("zed", 1, 10);
            s.schedule("amy", 1, 10);
            now[0] = 100;
            eq(s.runDue(), List.of("zed", "amy", "late"));
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
            now[0] = 700;
            eq(s.runDue(), List.of("tick"));
            eq(s.nextRunAt(), Optional.of(800L));
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
            now[0] = 0;
            var t = new Scheduler(() -> now[0]);
            t.every("tick", 0, 100);
            now[0] = 50;
            t.schedule("b", 0, 150);
            now[0] = 100;
            eq(t.runDue(), List.of("tick"));
            now[0] = 200;
            eq(t.runDue(), List.of("tick", "b"));
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
