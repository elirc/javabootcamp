// ─────────────────────────────────────────────────────────────────────────
//  07 · event sourcing and projections                      ★★★ stretch
//  concepts: event sourcing · replay · optimistic concurrency · projections
//  run: java -cp ../../_lib/out 07-event-sourcing.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Store what HAPPENED, derive what IS. Events are Opened(owner),
//  Deposited(cents) and Withdrawn(cents). replay(events) folds a stream
//  into Account(owner, balance, version), version = events applied; an
//  empty stream, or one whose first event is not Opened (or that opens
//  twice) → IllegalStateException. EventStore.append(stream, expected,
//  events) adds events only if the stream currently has exactly
//  `expected` events, else IllegalStateException and nothing is added; it
//  returns the new length and also records each event in a global log as
//  Recorded(stream, version, event), version counting from 1.
//  events(stream) is a read-only list (empty when unknown). The commands
//  open, deposit and withdraw replay the stream, validate, and append ONE
//  event with the version they read: open needs an empty stream; deposit
//  and withdraw need an open account (else IllegalStateException), cents
//  >= 1 (else IllegalArgumentException), and withdraw needs enough money
//  (else IllegalStateException). balanceByOwner(log) projects the global
//  log into owner → total balance across that owner's accounts.
//
//      open(s, "a1", "ada"); deposit(s, "a1", 500); withdraw(s, "a1", 200)
//      replay(s.events("a1"))    → Account[owner=ada, balance=300, version=3]
//      s.append("a1", 2, ...)    → IllegalStateException (stream is at 3)
//
//  hint: replay with one instanceof per event; a command only ever appends.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    sealed interface Event permits Opened, Deposited, Withdrawn {}
    record Opened(String owner) implements Event {}
    record Deposited(long cents) implements Event {}
    record Withdrawn(long cents) implements Event {}

    record Account(String owner, long balance, int version) {}
    record Recorded(String stream, int version, Event event) {}

    static final class EventStore {
        private final Map<String, List<Event>> streams = new HashMap<>();
        private final List<Recorded> log = new ArrayList<>();

        int append(String stream, int expectedVersion, List<Event> events) {
            throw new UnsupportedOperationException("TODO");
        }

        List<Event> events(String stream) {
            throw new UnsupportedOperationException("TODO");
        }

        List<Recorded> log() {
            return Collections.unmodifiableList(log);
        }
    }

    static Account replay(List<Event> events) {
        throw new UnsupportedOperationException("TODO");
    }

    static void open(EventStore store, String id, String owner) {
        throw new UnsupportedOperationException("TODO");
    }

    static void deposit(EventStore store, String id, long cents) {
        throw new UnsupportedOperationException("TODO");
    }

    static void withdraw(EventStore store, String id, long cents) {
        throw new UnsupportedOperationException("TODO");
    }

    static Map<String, Long> balanceByOwner(List<Recorded> log) {
        throw new UnsupportedOperationException("TODO");
    }

    static EventStore bank() {
        var s = new EventStore();
        open(s, "acc-1", "ada");
        deposit(s, "acc-1", 500);
        withdraw(s, "acc-1", 200);
        return s;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("replay folds a stream into the current state", () -> {
            eq(replay(List.of(new Opened("ada"), new Deposited(500), new Withdrawn(200))),
               new Account("ada", 300, 3));
            eq(replay(List.of(new Opened("bob"))), new Account("bob", 0, 1));
        });
        test("replaying a prefix shows the past", () -> {
            var events = bank().events("acc-1");
            eq(events.size(), 3);
            eq(replay(events.subList(0, 2)), new Account("ada", 500, 2));
            eq(replay(events), new Account("ada", 300, 3));
        });
        test("refuses streams that are empty or do not start with Opened", () -> {
            raises(IllegalStateException.class, () -> replay(List.of()));
            raises(IllegalStateException.class, () -> replay(List.of(new Deposited(5))));
            raises(IllegalStateException.class, () -> replay(List.of(new Opened("a"), new Opened("b"))));
        });
        test("append checks the expected version", () -> {
            var s = new EventStore();
            eq(s.append("x", 0, List.of(new Opened("ada"), new Deposited(5))), 2);
            raises(IllegalStateException.class, () -> s.append("x", 1, List.of(new Deposited(7))));
            eq(s.events("x"), List.of(new Opened("ada"), new Deposited(5)));
            eq(s.append("x", 2, List.of(new Deposited(7))), 3);
            eq(s.events("unknown"), List.of());
        });
        test("of two writers that read the same version, the second loses", () -> {
            var s = bank();
            int seen = replay(s.events("acc-1")).version();
            s.append("acc-1", seen, List.of(new Withdrawn(300)));
            raises(IllegalStateException.class, () -> s.append("acc-1", seen, List.of(new Withdrawn(300))));
            eq(replay(s.events("acc-1")).balance(), 0);
        });
        test("commands validate against replayed state and append one event", () -> {
            var s = bank();
            raises(IllegalStateException.class, () -> withdraw(s, "acc-1", 301));
            raises(IllegalArgumentException.class, () -> deposit(s, "acc-1", 0));
            raises(IllegalStateException.class, () -> deposit(s, "nope", 10));
            raises(IllegalStateException.class, () -> open(s, "acc-1", "eve"));
            eq(s.events("acc-1").size(), 3);
            withdraw(s, "acc-1", 300);
            eq(s.events("acc-1").get(3), new Withdrawn(300));
        });
        test("the global log records every event in append order", () -> {
            var s = bank();
            open(s, "acc-2", "bob");
            eq(s.log(), List.of(
                new Recorded("acc-1", 1, new Opened("ada")),
                new Recorded("acc-1", 2, new Deposited(500)),
                new Recorded("acc-1", 3, new Withdrawn(200)),
                new Recorded("acc-2", 1, new Opened("bob"))));
        });
        test("a projection totals balances per owner from the log", () -> {
            var s = bank();
            open(s, "acc-2", "bob");
            open(s, "acc-3", "ada");
            deposit(s, "acc-3", 50);
            deposit(s, "acc-2", 70);
            eq(balanceByOwner(s.log()), Map.of("ada", 350L, "bob", 70L));
            eq(balanceByOwner(s.log().subList(0, 2)), Map.of("ada", 500L));
        });
        test("streams are read-only to callers", () -> {
            var s = bank();
            raises(UnsupportedOperationException.class, () -> s.events("acc-1").add(new Deposited(1)));
            eq(replay(s.events("acc-1")).balance(), 300);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
