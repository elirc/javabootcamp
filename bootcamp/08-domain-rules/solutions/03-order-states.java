// ─────────────────────────────────────────────────────────────────────────
//  03 · an order's life — SOLUTION                          ★★☆ core
//  concepts: state machines · enums · EnumSet · IllegalStateException
//  run: java -cp ../../_lib/out 03-order-states.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A state machine is a table: for each state, the set of states it may
//  move to. Encode THAT, once, and derive the rest — canMoveTo is a set
//  lookup, isFinal is "the set is empty", moveTo is canMoveTo plus a
//  throw. Scattering if (status == PAID && next == SHIPPED || ...) across
//  the codebase is how an order ends up shipped after being cancelled.
//  In an enum, a switch expression over `this` returning an EnumSet is a
//  compact, compiler-checked table: with no default arm, adding a
//  RETURNED status refuses to compile until you say where it can go.
//  EnumSet is a bit-vector Set for enum constants: fast, ordered by
//  declaration, and EnumSet.noneOf(Status.class) is the empty set for
//  the final states.
//  (An enum constant can't refer to a LATER constant in its own
//  constructor arguments — PAID(SHIPPED) is an illegal forward reference —
//  which is why the table is a method, not a constructor field.)
//  An illegal move is IllegalStateException: the argument is a perfectly
//  good Status, the CURRENT state is what forbids it. Put both states in
//  the message; "illegal transition" alone is useless in a log.
//  moveTo returns the new status instead of mutating anything — enums are
//  immutable singletons, and the owning Order decides what to store.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    enum Status {
        NEW, PAID, SHIPPED, DELIVERED, CANCELLED;

        Set<Status> next() {
            return switch (this) {
                case NEW -> EnumSet.of(PAID, CANCELLED);
                case PAID -> EnumSet.of(SHIPPED, CANCELLED);
                case SHIPPED -> EnumSet.of(DELIVERED);
                case DELIVERED, CANCELLED -> EnumSet.noneOf(Status.class);
            };
        }

        boolean canMoveTo(Status target) {
            return next().contains(target);
        }

        Status moveTo(Status target) {
            if (!canMoveTo(target)) throw new IllegalStateException("illegal move " + this + " -> " + target);
            return target;
        }

        boolean isFinal() {
            return next().isEmpty();
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
