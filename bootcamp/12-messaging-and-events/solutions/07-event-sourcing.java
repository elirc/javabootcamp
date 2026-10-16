// ─────────────────────────────────────────────────────────────────────────
//  07 · event sourcing and projections — SOLUTION           ★★★ stretch
//  concepts: event sourcing · replay · optimistic concurrency · projections
//  run: java -cp ../../_lib/out 07-event-sourcing.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  In an event-sourced system the append-only list of events IS the
//  database; the current balance is a fold over it. You get an audit log
//  for free, can rebuild any past state (replay a prefix), and can build
//  new read models later by replaying history into them. Banks' ledgers,
//  EventStoreDB, Axon and Kafka-as-a-log architectures work this way.
//  replay is a left fold with a null "no account yet" state. Validating
//  the stream shape (Opened first, exactly once) catches corrupt or
//  mis-routed streams early instead of producing a balance for nobody.
//  Commands follow one rhythm: load (replay), decide (validate against
//  the replayed state), append with expectedVersion = the version you
//  loaded. That last argument is optimistic concurrency: if another
//  writer appended in between, the stream length no longer matches and
//  the append fails instead of silently interleaving. Two withdrawals of
//  the last 100 can then never both succeed; the loser reloads and is
//  refused. A command must append nothing when validation fails, so
//  validate everything before touching the store.
//  The projection reads the GLOBAL log, in append order, and keeps its own
//  state (stream → owner, owner → total). It never peeks at accounts, so
//  it could live in another process fed by a subscription (exercise 01)
//  and be thrown away and rebuilt at any time. Java 17 has no pattern
//  switch, so dispatch with instanceof patterns, one branch per event.

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
            var list = streams.computeIfAbsent(stream, k -> new ArrayList<>());
            if (list.size() != expectedVersion) {
                throw new IllegalStateException("conflict on " + stream + ": expected version "
                    + expectedVersion + " but it is " + list.size());
            }
            for (var e : events) {
                list.add(e);
                log.add(new Recorded(stream, list.size(), e));
            }
            return list.size();
        }

        List<Event> events(String stream) {
            return Collections.unmodifiableList(streams.getOrDefault(stream, List.of()));
        }

        List<Recorded> log() {
            return Collections.unmodifiableList(log);
        }
    }

    static Account replay(List<Event> events) {
        Account state = null;
        for (var e : events) {
            if (e instanceof Opened o) {
                if (state != null) throw new IllegalStateException("opened twice");
                state = new Account(o.owner(), 0, 1);
            } else if (state == null) {
                throw new IllegalStateException("first event is not Opened: " + e);
            } else if (e instanceof Deposited d) {
                state = new Account(state.owner(), state.balance() + d.cents(), state.version() + 1);
            } else if (e instanceof Withdrawn w) {
                state = new Account(state.owner(), state.balance() - w.cents(), state.version() + 1);
            }
        }
        if (state == null) throw new IllegalStateException("no such account");
        return state;
    }

    static void open(EventStore store, String id, String owner) {
        if (!store.events(id).isEmpty()) throw new IllegalStateException("already open: " + id);
        store.append(id, 0, List.of(new Opened(owner)));
    }

    static void deposit(EventStore store, String id, long cents) {
        if (cents < 1) throw new IllegalArgumentException("amount must be positive: " + cents);
        var account = replay(store.events(id));
        store.append(id, account.version(), List.of(new Deposited(cents)));
    }

    static void withdraw(EventStore store, String id, long cents) {
        if (cents < 1) throw new IllegalArgumentException("amount must be positive: " + cents);
        var account = replay(store.events(id));
        if (account.balance() < cents) throw new IllegalStateException("insufficient funds in " + id);
        store.append(id, account.version(), List.of(new Withdrawn(cents)));
    }

    static Map<String, Long> balanceByOwner(List<Recorded> log) {
        var ownerOf = new HashMap<String, String>();
        var totals = new TreeMap<String, Long>();
        for (var r : log) {
            if (r.event() instanceof Opened o) {
                ownerOf.put(r.stream(), o.owner());
                totals.putIfAbsent(o.owner(), 0L);
            } else if (r.event() instanceof Deposited d) {
                totals.merge(ownerOf.get(r.stream()), d.cents(), Long::sum);
            } else if (r.event() instanceof Withdrawn w) {
                totals.merge(ownerOf.get(r.stream()), -w.cents(), Long::sum);
            }
        }
        return totals;
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
