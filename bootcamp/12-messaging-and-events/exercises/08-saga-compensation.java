// ─────────────────────────────────────────────────────────────────────────
//  08 · a saga with compensation                            ★★★ stretch
//  concepts: sagas · compensating actions · partial failure · retries
//  run: java -cp ../../_lib/out 08-saga-compensation.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Booking a trip touches three services and none of them share a
//  transaction. Write run(steps, compensationAttempts) (attempts < 1 →
//  IllegalArgumentException). It runs each Step's action in order,
//  logging "do <name>" after each success. When an action throws, it logs
//  "fail <name>: <message>", runs no later steps, and undoes the steps
//  that SUCCEEDED, newest first (the failed step is not undone). Each
//  compensation is tried up to compensationAttempts times: every failed
//  try logs "undo <name> failed: <message>", a success logs "undo
//  <name>". A compensation that never succeeds is given up on, and the
//  remaining compensations still run. The Result is COMPLETED (nothing
//  failed), COMPENSATED (a step failed and every undo succeeded) or STUCK
//  (some undo was given up on), plus the log.
//
//      steps flight, hotel, car; car's action throws "sold out"
//      → COMPENSATED, [do flight, do hotel, fail car: sold out,
//                      undo hotel, undo flight]
//
//  hint: push each succeeded step on a stack; unwind it on failure.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Step(String name, Runnable action, Runnable compensation) {}

    enum Status { COMPLETED, COMPENSATED, STUCK }

    record Result(Status status, List<String> log) {}

    static Result run(List<Step> steps, int compensationAttempts) {
        throw new UnsupportedOperationException("TODO");
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
