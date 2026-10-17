// ─────────────────────────────────────────────────────────────────────────
//  06 · a state machine of sealed states                    ★★☆ core
//  concepts: sealed states · records carry state data · transitions
//  run: java -cp ../../_lib/out 06-sealed-state-machine.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Sealed order states hold only the data each state has: Draft(items),
//  Placed(items), Paid(items, paymentId), Shipped(items, paymentId,
//  tracking), Cancelled(reason, refundDue). apply(state, event) gives:
//
//    Draft + AddItem(sku) → Draft with sku appended
//    Draft + Place → Placed (no items: "cannot place an empty order")
//    Placed + Pay(id) → Paid;  Paid + Ship(t) → Shipped
//    Draft or Placed + Cancel(r) → Cancelled(r, false)
//    Paid + Cancel(r) → Cancelled(r, true)
//  Refusals are IllegalStateExceptions; any other pair gives "cannot
//  <Event> when <State>" in simple class names: "cannot Ship when Placed".
//  replay(events) applies each from an empty Draft. allowedEvents(state)
//  names the events apply accepts, in the order AddItem, Place, Pay,
//  Ship, Cancel (Draft lists Place even when empty).
//
//      replay([AddItem a, Place, Pay p1]) → Paid([a], p1)
//
//  hint: check the pair (state, event); one instanceof per side.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    sealed interface OrderState permits Draft, Placed, Paid, Shipped, Cancelled {}

    record Draft(List<String> items) implements OrderState {
        Draft { items = List.copyOf(items); }
    }

    record Placed(List<String> items) implements OrderState {}

    record Paid(List<String> items, String paymentId) implements OrderState {}

    record Shipped(List<String> items, String paymentId, String tracking) implements OrderState {}

    record Cancelled(String reason, boolean refundDue) implements OrderState {}

    sealed interface Event permits AddItem, Place, Pay, Ship, Cancel {}

    record AddItem(String sku) implements Event {}

    record Place() implements Event {}

    record Pay(String paymentId) implements Event {}

    record Ship(String tracking) implements Event {}

    record Cancel(String reason) implements Event {}

    static OrderState apply(OrderState state, Event event) {
        throw new UnsupportedOperationException("TODO");
    }

    static OrderState replay(List<Event> events) {
        throw new UnsupportedOperationException("TODO");
    }

    static List<String> allowedEvents(OrderState state) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("the happy path walks Draft to Shipped, carrying the data along", () ->
            eq(replay(List.of(new AddItem("a"), new AddItem("b"), new Place(), new Pay("p1"), new Ship("t9"))),
                new Shipped(List.of("a", "b"), "p1", "t9")));
        test("AddItem appends and leaves the old state unchanged", () -> {
            var before = new Draft(List.of("a"));
            eq(apply(before, new AddItem("b")), new Draft(List.of("a", "b")));
            eq(before, new Draft(List.of("a")));
        });
        test("an empty order cannot be placed", () -> {
            var e = raises(IllegalStateException.class, () -> apply(new Draft(List.of()), new Place()));
            eq(e.getMessage(), "cannot place an empty order");
        });
        test("cancelling before payment owes no refund; after payment it does", () -> {
            eq(apply(new Draft(List.of("a")), new Cancel("changed mind")), new Cancelled("changed mind", false));
            eq(apply(new Placed(List.of("a")), new Cancel("too slow")), new Cancelled("too slow", false));
            eq(apply(new Paid(List.of("a"), "p1"), new Cancel("duplicate")), new Cancelled("duplicate", true));
        });
        test("an illegal event names the event and the state", () -> {
            var e = raises(IllegalStateException.class, () -> apply(new Placed(List.of("a")), new Ship("t1")));
            eq(e.getMessage(), "cannot Ship when Placed");
            var late = raises(IllegalStateException.class, () -> apply(new Shipped(List.of("a"), "p", "t"), new Cancel("late")));
            eq(late.getMessage(), "cannot Cancel when Shipped");
        });
        test("items can only be added to a draft, and a cancelled order accepts nothing", () -> {
            var e = raises(IllegalStateException.class, () -> apply(new Placed(List.of("a")), new AddItem("b")));
            eq(e.getMessage(), "cannot AddItem when Placed");
            raises(IllegalStateException.class, () -> apply(new Cancelled("x", false), new AddItem("b")));
            raises(IllegalStateException.class, () -> apply(new Cancelled("x", false), new Cancel("again")));
        });
        test("replay stops at the first illegal event with its error", () -> {
            var e = raises(IllegalStateException.class, () -> replay(List.of(new AddItem("a"), new Pay("p"), new Place())));
            eq(e.getMessage(), "cannot Pay when Draft");
        });
        test("allowedEvents lists the legal next events per state, in event order", () -> {
            eq(allowedEvents(new Draft(List.of())), List.of("AddItem", "Place", "Cancel"));
            eq(allowedEvents(new Placed(List.of("a"))), List.of("Pay", "Cancel"));
            eq(allowedEvents(new Paid(List.of("a"), "p")), List.of("Ship", "Cancel"));
            eq(allowedEvents(new Shipped(List.of("a"), "p", "t")), List.of());
            eq(allowedEvents(new Cancelled("x", true)), List.of());
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
