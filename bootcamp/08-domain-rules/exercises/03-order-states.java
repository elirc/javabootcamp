// ─────────────────────────────────────────────────────────────────────────
//  03 · an order's life                                     ★★☆ core
//  concepts: state machines · enums · EnumSet · IllegalStateException
//  run: java -cp ../../_lib/out 03-order-states.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An order management tool moves orders through statuses. The allowed
//  moves: NEW → PAID or CANCELLED; PAID → SHIPPED or CANCELLED;
//  SHIPPED → DELIVERED. DELIVERED and CANCELLED are final. Finish the enum
//  Status (above the tests): next() lists where a status may go,
//  canMoveTo checks one move, moveTo returns the new status or throws
//  IllegalStateException naming the move ("SHIPPED -> PAID"), and isFinal
//  says whether nothing can follow.
//
//      Status.NEW.next()                → [PAID, CANCELLED]
//      Status.PAID.moveTo(SHIPPED)      → SHIPPED
//      Status.SHIPPED.moveTo(PAID)      → IllegalStateException
//                                         "... SHIPPED -> PAID"
//
//  hint: write the transition table once; derive everything else from it.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    enum Status {
        NEW, PAID, SHIPPED, DELIVERED, CANCELLED;

        Set<Status> next() {
            throw new UnsupportedOperationException("TODO");
        }

        boolean canMoveTo(Status target) {
            throw new UnsupportedOperationException("TODO");
        }

        Status moveTo(Status target) {
            throw new UnsupportedOperationException("TODO");
        }

        boolean isFinal() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("lists the next statuses", () -> {
            eq(Status.NEW.next(), Set.of(Status.PAID, Status.CANCELLED));
            eq(Status.SHIPPED.next(), Set.of(Status.DELIVERED));
        });
        test("allows the happy path", () ->
            eq(Status.NEW.moveTo(Status.PAID).moveTo(Status.SHIPPED).moveTo(Status.DELIVERED), Status.DELIVERED));
        test("can cancel before shipping", () -> {
            eq(Status.NEW.canMoveTo(Status.CANCELLED), true);
            eq(Status.PAID.canMoveTo(Status.CANCELLED), true);
        });
        test("cannot cancel once shipped", () -> eq(Status.SHIPPED.canMoveTo(Status.CANCELLED), false));
        test("cannot go backwards or stay put", () -> {
            eq(Status.SHIPPED.canMoveTo(Status.PAID), false);
            eq(Status.PAID.canMoveTo(Status.PAID), false);
        });
        test("an illegal move names both statuses", () ->
            raises(IllegalStateException.class, () -> Status.SHIPPED.moveTo(Status.PAID), "SHIPPED -> PAID"));
        test("delivered and cancelled are final", () -> {
            eq(Status.DELIVERED.isFinal(), true);
            eq(Status.CANCELLED.isFinal(), true);
            eq(Status.NEW.isFinal(), false);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
