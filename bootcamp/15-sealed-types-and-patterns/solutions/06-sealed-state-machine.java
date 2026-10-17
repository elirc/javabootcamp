// ─────────────────────────────────────────────────────────────────────────
//  06 · a state machine of sealed states — SOLUTION         ★★☆ core
//  concepts: sealed states · records carry state data · transitions
//  run: java -cp ../../_lib/out 06-sealed-state-machine.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  An enum state machine (module 08) knows WHICH state an order is in. A
//  sealed hierarchy of records also knows what the order HAS in that
//  state: only Paid and Shipped carry a paymentId, only Shipped a
//  tracking number, only Cancelled a refund flag. An "order" class with
//  nullable fields for all of them lets code read a tracking number that
//  cannot exist yet; here the type system rules it out.
//  apply is a pure function (state, event) -> state. It never mutates:
//  Draft's compact constructor copies the list, and AddItem builds a new
//  list. Pure transitions are what make replay (event sourcing, module
//  12) and testing trivial: the same events always give the same state.
//  Every pair not listed is refused with one uniform message. The trap
//  is a transition that "works" silently: a cancel after shipping that
//  returns Cancelled would lose the parcel. Paid + Cancel is the one
//  place that sets refundDue: the payment has to be reversed.
//  allowedEvents is derived from apply itself, by trying each event, so
//  the UI's buttons can never disagree with the rules. Java 21 would let
//  you write apply as one switch over a record pattern of the pair.

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
        if (state instanceof Draft d && event instanceof AddItem a) {
            var items = new ArrayList<>(d.items());
            items.add(a.sku());
            return new Draft(items);
        }
        if (state instanceof Draft d && event instanceof Place) {
            if (d.items().isEmpty()) throw new IllegalStateException("cannot place an empty order");
            return new Placed(d.items());
        }
        if (state instanceof Placed p && event instanceof Pay pay) return new Paid(p.items(), pay.paymentId());
        if (state instanceof Paid p && event instanceof Ship s) return new Shipped(p.items(), p.paymentId(), s.tracking());
        if ((state instanceof Draft || state instanceof Placed) && event instanceof Cancel c) return new Cancelled(c.reason(), false);
        if (state instanceof Paid && event instanceof Cancel c) return new Cancelled(c.reason(), true);
        throw new IllegalStateException("cannot " + event.getClass().getSimpleName() + " when " + state.getClass().getSimpleName());
    }

    static OrderState replay(List<Event> events) {
        OrderState state = new Draft(List.of());
        for (var e : events) state = apply(state, e);
        return state;
    }

    static List<String> allowedEvents(OrderState state) {
        var probes = List.of(new AddItem("probe"), new Place(), new Pay("probe"), new Ship("probe"), new Cancel("probe"));
        var allowed = new ArrayList<String>();
        for (var e : probes) {
            if (e instanceof Place && state instanceof Draft) {
                allowed.add("Place");
                continue;
            }
            try {
                apply(state, e);
                allowed.add(e.getClass().getSimpleName());
            } catch (IllegalStateException refused) {
                // not allowed here
            }
        }
        return allowed;
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
