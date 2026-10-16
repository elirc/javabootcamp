// ─────────────────────────────────────────────────────────────────────────
//  08 · a saga with compensation — SOLUTION                 ★★★ stretch
//  concepts: sagas · compensating actions · partial failure · retries
//  run: java -cp ../../_lib/out 08-saga-compensation.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A saga replaces one distributed transaction (which needs every service
//  to hold locks and agree, as in two-phase commit, module 13) with a
//  sequence of LOCAL transactions, each paired with a compensating action
//  that semantically undoes it: cancel the flight, release the hotel
//  room. If step k fails, run the compensations of steps k-1 down to 1.
//  Uber's Cadence/Temporal, AWS Step Functions and Axon all orchestrate
//  sagas this way.
//  Reverse order matters because later steps may depend on earlier ones
//  (refund the charge before releasing the reservation it paid for). The
//  failed step is NOT compensated: it did not happen, or it rolled itself
//  back locally, and "undoing" it could cancel something it never did.
//  A stack (ArrayDeque push/pop) of succeeded steps captures both rules.
//  Compensations fail too: the hotel API times out like any other. So
//  they are retried, which means they must be idempotent: releasing a
//  room twice must be harmless. When retries run out the saga is STUCK:
//  the system is inconsistent and a human must step in, so the status
//  says so loudly instead of pretending to be COMPENSATED. Even then, the
//  other compensations still run; giving up on one is no reason to leave
//  the flight booked as well.
//  Sagas give up isolation: between "do hotel" and "undo hotel" other
//  users can see the booked room. That trade is the price of no locks.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Step(String name, Runnable action, Runnable compensation) {}

    enum Status { COMPLETED, COMPENSATED, STUCK }

    record Result(Status status, List<String> log) {}

    static Result run(List<Step> steps, int compensationAttempts) {
        if (compensationAttempts < 1) throw new IllegalArgumentException("need at least one attempt");
        var log = new ArrayList<String>();
        var done = new ArrayDeque<Step>();
        for (var step : steps) {
            try {
                step.action().run();
            } catch (RuntimeException e) {
                log.add("fail " + step.name() + ": " + e.getMessage());
                boolean stuck = false;
                while (!done.isEmpty()) stuck |= !undo(done.pop(), compensationAttempts, log);
                return new Result(stuck ? Status.STUCK : Status.COMPENSATED, log);
            }
            log.add("do " + step.name());
            done.push(step);
        }
        return new Result(Status.COMPLETED, log);
    }

    private static boolean undo(Step step, int attempts, List<String> log) {
        for (int i = 0; i < attempts; i++) {
            try {
                step.compensation().run();
                log.add("undo " + step.name());
                return true;
            } catch (RuntimeException e) {
                log.add("undo " + step.name() + " failed: " + e.getMessage());
            }
        }
        return false;
    }

    /** A tiny world: booked resources, and steps that book and release them. */
    static Step book(Set<String> booked, String name) {
        return new Step(name, () -> booked.add(name), () -> booked.remove(name));
    }

    static Step broken(String name, String message) {
        return new Step(name, () -> { throw new IllegalStateException(message); },
                        () -> { throw new AssertionError("a failed step must not be undone"); });
    }

    /** A compensation that fails `failures` times, then works. */
    static Runnable flaky(int failures, Runnable then) {
        var left = new int[] {failures};
        return () -> {
            if (left[0]-- > 0) throw new IllegalStateException("timeout");
            then.run();
        };
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("all steps succeed: COMPLETED and nothing undone", () -> {
            var booked = new HashSet<String>();
            var r = run(List.of(book(booked, "flight"), book(booked, "hotel")), 1);
            eq(r.status(), Status.COMPLETED);
            eq(r.log(), List.of("do flight", "do hotel"));
            eq(booked, Set.of("flight", "hotel"));
        });
        test("a failure undoes the succeeded steps, newest first", () -> {
            var booked = new HashSet<String>();
            var r = run(List.of(book(booked, "flight"), book(booked, "hotel"), broken("car", "sold out")), 1);
            eq(r.status(), Status.COMPENSATED);
            eq(r.log(), List.of("do flight", "do hotel", "fail car: sold out", "undo hotel", "undo flight"));
            eq(booked, Set.of());
        });
        test("steps after the failure never run", () -> {
            var booked = new HashSet<String>();
            var r = run(List.of(book(booked, "flight"), broken("hotel", "full"), book(booked, "car")), 1);
            eq(r.log(), List.of("do flight", "fail hotel: full", "undo flight"));
            eq(booked, Set.of());
        });
        test("a failing first step has nothing to undo", () -> {
            var r = run(List.of(broken("flight", "no seats"), book(new HashSet<>(), "hotel")), 3);
            eq(r.status(), Status.COMPENSATED);
            eq(r.log(), List.of("fail flight: no seats"));
        });
        test("a flaky compensation is retried until it works", () -> {
            var booked = new HashSet<String>();
            var hotel = new Step("hotel", () -> booked.add("hotel"), flaky(2, () -> booked.remove("hotel")));
            var r = run(List.of(hotel, broken("car", "sold out")), 3);
            eq(r.status(), Status.COMPENSATED);
            eq(r.log(), List.of("do hotel", "fail car: sold out",
                                "undo hotel failed: timeout", "undo hotel failed: timeout", "undo hotel"));
            eq(booked, Set.of());
        });
        test("a compensation out of attempts leaves the saga STUCK", () -> {
            var booked = new HashSet<String>();
            var hotel = new Step("hotel", () -> booked.add("hotel"), flaky(5, () -> booked.remove("hotel")));
            var r = run(List.of(book(booked, "flight"), hotel, broken("car", "sold out")), 2);
            eq(r.status(), Status.STUCK);
            eq(r.log(), List.of("do flight", "do hotel", "fail car: sold out",
                                "undo hotel failed: timeout", "undo hotel failed: timeout", "undo flight"));
            eq(booked, Set.of("hotel"));
        });
        test("an empty saga completes", () -> {
            var r = run(List.of(), 1);
            eq(r.status(), Status.COMPLETED);
            eq(r.log(), List.of());
        });
        test("refuses fewer than one compensation attempt", () ->
            raises(IllegalArgumentException.class, () -> run(List.of(), 0)));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
