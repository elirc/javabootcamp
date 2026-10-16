// ─────────────────────────────────────────────────────────────────────────
//  07 · deadlines and time budgets — SOLUTION               ★★★ stretch
//  concepts: deadlines · timeout budgets · propagation · clock skew
//  run: java -cp ../../_lib/out 07-deadline-budget.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Per-call timeouts alone do not bound a request. Three calls with a
//  2 s timeout each can take 6 s while the user gave up after 1 s, and
//  the server keeps working for nobody. A deadline is ONE instant for the
//  whole request; every call asks "how long may I take?" and gets
//  min(my own timeout, time left). When nothing is left, the call is not
//  made at all: skipping is cheaper than a call that is doomed to time out.
//  A child deadline (a sub-task with its own budget) is min(parent, now +
//  budget): a child can be stricter than its parent, never looser.
//  Propagation is where systems get it wrong. Sending the absolute
//  deadline ("finish by 12:00:01.250") to another machine assumes both
//  clocks agree; they don't, by milliseconds to seconds. gRPC sends the
//  REMAINING time instead (the grpc-timeout header, "700m" = 700 ms) and
//  the receiver re-anchors it on its own clock. The tests use two clocks
//  5 s apart to prove only the duration crosses the wire.
//  remaining() clamps at 0 so callers never see a negative budget, and
//  expired() is remaining() == 0, which makes "exactly at the deadline"
//  expired: a call with 0 ms to run cannot do anything useful.
//  runChain is the simulation: each step either fits (spends its cost),
//  times out (spends exactly the timeout it was given), or is skipped.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

class Exercise {

    static final class FakeClock implements LongSupplier {
        long now;

        public long getAsLong() {
            return now;
        }

        void advance(long millis) {
            now += millis;
        }
    }

    record Step(String name, long costMillis, long timeoutMillis) {}

    static final class Deadline {
        private static final Pattern HEADER = Pattern.compile("grpc-timeout: (\\d{1,18})m");

        private final LongSupplier clock;
        private final long at;

        private Deadline(LongSupplier clock, long at) {
            this.clock = clock;
            this.at = at;
        }

        static Deadline after(LongSupplier clock, long budgetMillis) {
            if (budgetMillis < 0) throw new IllegalArgumentException("budget cannot be negative: " + budgetMillis);
            return new Deadline(clock, clock.getAsLong() + budgetMillis);
        }

        long remaining() {
            return Math.max(0, at - clock.getAsLong());
        }

        boolean expired() {
            return remaining() == 0;
        }

        long timeoutFor(long callTimeoutMillis) {
            return Math.min(callTimeoutMillis, remaining());
        }

        Deadline child(long budgetMillis) {
            var own = after(clock, budgetMillis);
            return own.at < at ? own : this;
        }

        String header() {
            return "grpc-timeout: " + remaining() + "m";
        }

        static Deadline fromHeader(LongSupplier clock, String header) {
            var m = HEADER.matcher(header);
            if (!m.matches()) throw new IllegalArgumentException("not a grpc-timeout header: " + header);
            return after(clock, Long.parseLong(m.group(1)));
        }
    }

    static List<String> runChain(Deadline deadline, FakeClock clock, List<Step> steps) {
        var log = new ArrayList<String>();
        for (var step : steps) {
            long allowed = deadline.timeoutFor(step.timeoutMillis());
            if (allowed <= 0) {
                log.add("skipped " + step.name());
            } else if (step.costMillis() <= allowed) {
                clock.advance(step.costMillis());
                log.add("ok " + step.name());
            } else {
                clock.advance(allowed);
                log.add("timeout " + step.name());
            }
        }
        return log;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("remaining counts down and never goes negative", () -> {
            var clock = new FakeClock();
            var d = Deadline.after(clock, 1_000);
            eq(d.remaining(), 1_000);
            clock.advance(300);
            eq(d.remaining(), 700);
            clock.advance(5_000);
            eq(d.remaining(), 0);
        });
        test("expired exactly when the budget is used up", () -> {
            var clock = new FakeClock();
            var d = Deadline.after(clock, 1_000);
            clock.advance(999);
            eq(d.expired(), false);
            clock.advance(1);
            eq(d.expired(), true);
            eq(Deadline.after(clock, 0).expired(), true);
            raises(IllegalArgumentException.class, () -> Deadline.after(clock, -1));
        });
        test("timeoutFor is the smaller of the call's timeout and the time left", () -> {
            var clock = new FakeClock();
            var d = Deadline.after(clock, 1_000);
            eq(d.timeoutFor(200), 200);
            clock.advance(900);
            eq(d.timeoutFor(200), 100);
            clock.advance(100);
            eq(d.timeoutFor(200), 0);
        });
        test("a child deadline is never looser than its parent", () -> {
            var clock = new FakeClock();
            var parent = Deadline.after(clock, 500);
            eq(parent.child(1_000).remaining(), 500);
            eq(parent.child(200).remaining(), 200);
            clock.advance(400);
            eq(parent.child(200).remaining(), 100);
        });
        test("a chain that fits runs every step and spends its cost", () -> {
            var clock = new FakeClock();
            var d = Deadline.after(clock, 1_000);
            var log = runChain(d, clock, List.of(new Step("auth", 50, 100), new Step("db", 300, 500)));
            eq(log, List.of("ok auth", "ok db"));
            eq(clock.now, 350);
        });
        test("a slow step times out at its own timeout and the chain goes on", () -> {
            var clock = new FakeClock();
            var d = Deadline.after(clock, 1_000);
            var log = runChain(d, clock, List.of(new Step("search", 900, 200), new Step("ads", 100, 300)));
            eq(log, List.of("timeout search", "ok ads"));
            eq(clock.now, 300);
        });
        test("the deadline caps a step; later steps are skipped for free", () -> {
            var clock = new FakeClock();
            var d = Deadline.after(clock, 1_000);
            var log = runChain(d, clock, List.of(
                new Step("a", 700, 800), new Step("b", 500, 800), new Step("c", 10, 800)));
            eq(log, List.of("ok a", "timeout b", "skipped c"));
            eq(clock.now, 1_000);
        });
        test("a step costing exactly its timeout succeeds", () -> {
            var clock = new FakeClock();
            var d = Deadline.after(clock, 300);
            eq(runChain(d, clock, List.of(new Step("a", 100, 100), new Step("b", 200, 500))),
                List.of("ok a", "ok b"));
            eq(d.expired(), true);
        });
        test("the header carries the time left, not the clock", () -> {
            var client = new FakeClock();
            var d = Deadline.after(client, 1_000);
            client.advance(300);
            eq(d.header(), "grpc-timeout: 700m");
            var server = new FakeClock();
            server.now = 5_000;
            var there = Deadline.fromHeader(server, d.header());
            eq(there.remaining(), 700);
            server.advance(700);
            eq(there.expired(), true);
            raises(IllegalArgumentException.class, () -> Deadline.fromHeader(server, "grpc-timeout: soon"));
            raises(IllegalArgumentException.class, () -> Deadline.fromHeader(server, "700m"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
