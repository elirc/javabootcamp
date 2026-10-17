// ─────────────────────────────────────────────────────────────────────────
//  08 · an event-sourced account (fill-in) — SOLUTION       ★★★ stretch
//  concepts: fill-in · event sourcing · replay · decide vs apply
//  run: java -cp ../../_lib/out 08-event-sourced-account-fill-in.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Event sourcing splits behaviour in two. DECIDE looks at the current
//  state and a command and returns the events that should happen (or
//  refuses); it changes nothing. APPLY takes a state and one event and
//  returns the next state; it never refuses a valid history, because
//  the event already happened. The current state is just a left fold
//  of apply over the history, which is why it can be rebuilt any time,
//  for any point in the past.
//  The holes in apply each build a NEW Account (records are immutable),
//  copying every field and bumping version, which then equals the
//  number of events applied: the optimistic-locking version of module
//  13. The decide holes are the business rules: s.balance() - cents <
//  -s.overdraftLimit() (the limit itself is allowed, hence <) and
//  s.balance() != 0.
//  The classic wrong turn is putting the checks in apply. Then replaying
//  an old history fails the day the rule changes (a lower overdraft
//  limit would make last year's withdrawals "invalid"). Rules belong in
//  decide, at the time of the command; apply records facts. The other
//  is mutating state inside decide: the test that checks s.balance() is
//  still 50 after a decision catches it. Module 12 (exercise 07) replays
//  events the same way.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    sealed interface Event permits Opened, Deposited, Withdrawn, Closed {}
    record Opened(String id, long overdraftLimit) implements Event {}
    record Deposited(long cents) implements Event {}
    record Withdrawn(long cents) implements Event {}
    record Closed() implements Event {}

    record Account(String id, long balance, long overdraftLimit, boolean open, int version) {}

    static Account apply(Account s, Event e) {
        if (e instanceof Opened o) {
            if (s != null) throw new IllegalStateException("already opened");
            return new Account(o.id(), 0, o.overdraftLimit(), true, 1);
        }
        if (s == null) throw new IllegalStateException("history must start with Opened");
        if (!s.open()) throw new IllegalStateException("event after Closed");
        if (e instanceof Deposited d) return new Account(s.id(), s.balance() + d.cents(), s.overdraftLimit(), true, s.version() + 1);
        if (e instanceof Withdrawn w) return new Account(s.id(), s.balance() - w.cents(), s.overdraftLimit(), true, s.version() + 1);
        if (e instanceof Closed) return new Account(s.id(), s.balance(), s.overdraftLimit(), false, s.version() + 1);
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
        if (s.balance() - cents < -s.overdraftLimit()) throw new IllegalStateException("insufficient funds");
        return List.of(new Withdrawn(cents));
    }

    static List<Event> decideClose(Account s) {
        if (!s.open()) throw new IllegalStateException("closed");
        if (s.balance() != 0) throw new IllegalStateException("balance must be 0 to close");
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
            var owing = replay(List.of(new Opened("a", 100), new Withdrawn(40)));
            raises(IllegalStateException.class, () -> decideClose(owing), "balance must be 0");
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
