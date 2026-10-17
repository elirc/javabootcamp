// ─────────────────────────────────────────────────────────────────────────
//  08 · an event-sourced account (fill-in)                  ★★★ stretch
//  concepts: fill-in · event sourcing · replay · decide vs apply
//  run: java -cp ../../_lib/out 08-event-sourced-account-fill-in.java
// ─────────────────────────────────────────────────────────────────────────
//
//  FILL-IN: the account is complete except for 6 holes. Replace each
//  `/* ___ */ hole()` with the expression its comment asks for; delete
//  hole() when you are done.
//  State is never stored, only events. apply(state, event) returns the
//  NEXT state: Opened starts an open account (balance 0, version 1);
//  every later event adds 1 to version; Deposited/Withdrawn move the
//  balance; Closed marks it closed. replay(events) folds them from null.
//  Commands DECIDE, they never change state: decideWithdraw refuses
//  (IllegalStateException "insufficient funds") when the balance would
//  drop below -overdraftLimit; decideClose refuses unless the balance is
//  0. Both refuse on a closed account ("closed"); amounts must be > 0.
//
//      replay([Opened(a, 0), Deposited(500), Withdrawn(200)])
//                                  → Account[a, 300, 0, open, version 3]
//      decideWithdraw(that, 301)   → IllegalStateException
//
//  hint: a record has no setters; each hole builds a new Account.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    sealed interface Event permits Opened, Deposited, Withdrawn, Closed {}
    record Opened(String id, long overdraftLimit) implements Event {}
    record Deposited(long cents) implements Event {}
    record Withdrawn(long cents) implements Event {}
    record Closed() implements Event {}

    record Account(String id, long balance, long overdraftLimit, boolean open, int version) {}

    // a hole: each "/* ___ */ hole()" below is yours to replace
    static <T> T hole() {
        throw new UnsupportedOperationException("TODO");
    }

    static Account apply(Account s, Event e) {
        if (e instanceof Opened o) {
            if (s != null) throw new IllegalStateException("already opened");
            return /* ___ */ hole(); // a new open account: balance 0, version 1
        }
        if (s == null) throw new IllegalStateException("history must start with Opened");
        if (!s.open()) throw new IllegalStateException("event after Closed");
        if (e instanceof Deposited d) return /* ___ */ hole(); // balance up, version + 1
        if (e instanceof Withdrawn w) return /* ___ */ hole(); // balance down, version + 1
        if (e instanceof Closed) return /* ___ */ hole(); // not open any more, version + 1
        throw new IllegalArgumentException("unknown event " + e);
    }

    static Account replay(List<Event> events) {
        Account state = null;
        for (var e : events) state = apply(state, e);
        if (state == null) throw new IllegalStateException("no events");
        return state;
    }

    static List<Event> decideWithdraw(Account s, long cents) {
        if (cents <= 0) throw new IllegalArgumentException("amount must be positive");
        if (!s.open()) throw new IllegalStateException("closed");
        if (/* ___ */ hole()) throw new IllegalStateException("insufficient funds"); // below the limit?
        return List.of(new Withdrawn(cents));
    }

    static List<Event> decideClose(Account s) {
        if (!s.open()) throw new IllegalStateException("closed");
        if (/* ___ */ hole()) throw new IllegalStateException("balance must be 0 to close"); // money left?
        return List.of(new Closed());
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("replays a history into the current state", () -> {
            eq(replay(List.of(new Opened("a1", 0), new Deposited(500), new Withdrawn(200))),
                new Account("a1", 300, 0, true, 3));
        });
        test("the version counts the events", () -> {
            eq(replay(List.of(new Opened("a1", 0))).version(), 1);
            eq(replay(List.of(new Opened("a1", 0), new Deposited(1), new Deposited(1), new Deposited(1))).version(), 4);
        });
        test("an overdraft limit lets the balance go negative", () -> {
            eq(replay(List.of(new Opened("a", 1000), new Withdrawn(800))).balance(), -800);
        });
        test("decideWithdraw allows down to the limit and refuses below it", () -> {
            var s = replay(List.of(new Opened("a", 100), new Deposited(50)));
            eq(decideWithdraw(s, 150), List.of(new Withdrawn(150)));
            raises(IllegalStateException.class, () -> decideWithdraw(s, 151), "insufficient funds");
            raises(IllegalArgumentException.class, () -> decideWithdraw(s, 0));
            eq(s.balance(), 50);
        });
        test("decided events replay into the new state", () -> {
            var history = new ArrayList<Event>(List.of(new Opened("a", 0), new Deposited(500)));
            history.addAll(decideWithdraw(replay(history), 120));
            eq(replay(history), new Account("a", 380, 0, true, 3));
        });
        test("closing needs a zero balance and ends the account", () -> {
            var rich = replay(List.of(new Opened("a", 0), new Deposited(300)));
            raises(IllegalStateException.class, () -> decideClose(rich), "balance must be 0");
            var history = new ArrayList<Event>(List.of(new Opened("a", 0), new Deposited(300), new Withdrawn(300)));
            history.addAll(decideClose(replay(history)));
            var closed = replay(history);
            eq(closed, new Account("a", 0, 0, false, 4));
            raises(IllegalStateException.class, () -> decideWithdraw(closed, 1), "closed");
            history.add(new Deposited(5));
            raises(IllegalStateException.class, () -> replay(history));
        });
        test("a history starts with exactly one Opened", () -> {
            raises(IllegalStateException.class, () -> replay(List.of(new Deposited(5))));
            raises(IllegalStateException.class, () -> replay(List.of()));
            raises(IllegalStateException.class, () -> replay(List.of(new Opened("a", 0), new Opened("b", 0))), "already");
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
