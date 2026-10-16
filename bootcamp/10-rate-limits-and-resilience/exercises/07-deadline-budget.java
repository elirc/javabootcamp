// ─────────────────────────────────────────────────────────────────────────
//  07 · deadlines and time budgets                          ★★★ stretch
//  concepts: deadlines · timeout budgets · propagation · clock skew
//  run: java -cp ../../_lib/out 07-deadline-budget.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A request has ONE deadline; every call inside it gets what is left.
//  Finish Deadline: after(clock, budget) ends at now + budget (budget < 0
//  → IllegalArgumentException); remaining() is the ms left, never below
//  0; expired() is true once remaining() is 0; timeoutFor(t) is
//  min(t, remaining()); child(b) ends at the earlier of now + b and this
//  deadline. header() is "grpc-timeout: <remaining>m"; fromHeader(clock,
//  h) builds a deadline of that many ms on the GIVEN clock (anything not
//  of that exact form → IllegalArgumentException). Then runChain: for
//  each step, allowed = timeoutFor(step timeout); allowed 0 → "skipped
//  name" (no time passes); cost <= allowed → advance cost, "ok name";
//  else advance allowed, "timeout name". Keep going to the last step.
//
//      budget 1000, steps a(700, t/o 800) b(500, 800) c(10, 800)
//        → [ok a, timeout b, skipped c], clock at 1000
//
//  hint: a deadline is an instant; everything else is derived from it.

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
        private final LongSupplier clock;
        private final long at;

        private Deadline(LongSupplier clock, long at) {
            this.clock = clock;
            this.at = at;
        }

        static Deadline after(LongSupplier clock, long budgetMillis) {
            throw new UnsupportedOperationException("TODO");
        }

        long remaining() {
            throw new UnsupportedOperationException("TODO");
        }

        boolean expired() {
            throw new UnsupportedOperationException("TODO");
        }

        long timeoutFor(long callTimeoutMillis) {
            throw new UnsupportedOperationException("TODO");
        }

        Deadline child(long budgetMillis) {
            throw new UnsupportedOperationException("TODO");
        }

        String header() {
            throw new UnsupportedOperationException("TODO");
        }

        static Deadline fromHeader(LongSupplier clock, String header) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static List<String> runChain(Deadline deadline, FakeClock clock, List<Step> steps) {
        throw new UnsupportedOperationException("TODO");
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
