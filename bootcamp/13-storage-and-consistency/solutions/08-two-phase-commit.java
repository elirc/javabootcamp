// ─────────────────────────────────────────────────────────────────────────
//  08 · all or none, across machines — SOLUTION             ★★★ stretch
//  concepts: two-phase commit · coordinator log · presumed abort · recovery
//  run: java -cp ../../_lib/out 08-two-phase-commit.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Two-phase commit makes several databases agree on one outcome. Phase
//  one asks everyone "can you commit?"; a YES is a promise (the
//  participant has made the change durable and will not back out). Any
//  NO, or silence, dooms the transaction, so asking the rest is wasted
//  work. Phase two tells everyone the decision.
//  The decision is real the moment it is written to the coordinator's
//  log, and that write must happen BEFORE the first commit message. If
//  the coordinator sent commits first and crashed, the log would hold no
//  decision, recovery would presume abort, and the participants that had
//  already committed would disagree with the rest forever.
//  After the decision, failures change nothing: a participant that is
//  down simply has not heard yet. The coordinator keeps going, skips the
//  DONE record, and recovery re-sends the decision until every call
//  succeeds; that is why participants must treat a repeated commit or
//  abort as a no-op. Recovery sends to EVERY participant, because the
//  log does not say who voted YES. A transaction with BEGIN but no
//  decision crashed during phase one; nobody can have committed, so
//  aborting it is safe ("presumed abort").
//  The weakness to remember: a participant that voted YES is stuck
//  holding locks until the coordinator answers. That blocking is why
//  sagas (module 12) exist.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    enum Vote { YES, NO, NO_RESPONSE }

    enum Outcome { COMMITTED, ABORTED }

    static final class Participant {
        final String name;
        final Vote vote;
        final List<String> events;
        int failingCalls;   // the next this-many commit/abort calls throw

        Participant(String name, Vote vote, List<String> events) {
            this.name = name;
            this.vote = vote;
            this.events = events;
        }

        Vote prepare(String tx) {
            events.add(name + " prepare " + tx);
            return vote;
        }

        void commit(String tx) {
            finish("commit", tx);
        }

        void abort(String tx) {
            finish("abort", tx);
        }

        private void finish(String what, String tx) {
            if (failingCalls > 0) {
                failingCalls--;
                events.add(name + " " + what + " " + tx + " FAILED");
                throw new IllegalStateException(name + " is down");
            }
            events.add(name + " " + what + " " + tx);
        }
    }

    static final class Coordinator {
        private final List<Participant> participants;
        private final List<String> log;

        Coordinator(List<Participant> participants, List<String> log) {
            this.participants = participants;
            this.log = log;
        }

        Outcome run(String tx) {
            log.add(tx + " BEGIN");
            var yes = new ArrayList<Participant>();
            boolean commit = true;
            for (var p : participants) {
                if (p.prepare(tx) != Vote.YES) {
                    commit = false;
                    break;
                }
                yes.add(p);
            }
            log.add(tx + (commit ? " COMMIT" : " ABORT"));
            if (deliver(tx, commit, commit ? participants : yes)) log.add(tx + " DONE");
            return commit ? Outcome.COMMITTED : Outcome.ABORTED;
        }

        List<String> recover() {
            var decisions = new LinkedHashMap<String, String>();   // tx -> COMMIT / ABORT / null
            var done = new HashSet<String>();
            for (var line : log) {
                var f = line.split(" ");
                switch (f[1]) {
                    case "BEGIN" -> decisions.put(f[0], null);
                    case "DONE" -> done.add(f[0]);
                    default -> decisions.put(f[0], f[1]);
                }
            }
            var handled = new ArrayList<String>();
            decisions.forEach((tx, decision) -> {
                if (done.contains(tx)) return;
                if (decision == null) {
                    decision = "ABORT";
                    log.add(tx + " ABORT");
                }
                handled.add(tx);
                if (deliver(tx, decision.equals("COMMIT"), participants)) log.add(tx + " DONE");
            });
            return handled;
        }

        private boolean deliver(String tx, boolean commit, List<Participant> targets) {
            boolean allOk = true;
            for (var p : targets) {
                try {
                    if (commit) p.commit(tx);
                    else p.abort(tx);
                } catch (RuntimeException e) {
                    allOk = false;
                }
            }
            return allOk;
        }
    }

    static List<Participant> cluster(List<String> events, Vote... votes) {
        var ps = new ArrayList<Participant>();
        for (int i = 0; i < votes.length; i++) ps.add(new Participant("p" + (i + 1), votes[i], events));
        return ps;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("all YES commits everywhere, and the decision is logged first", () -> {
            var shared = new ArrayList<String>();
            var c = new Coordinator(cluster(shared, Vote.YES, Vote.YES), shared);
            eq(c.run("t1"), Outcome.COMMITTED);
            eq(shared, List.of("t1 BEGIN", "p1 prepare t1", "p2 prepare t1", "t1 COMMIT",
                "p1 commit t1", "p2 commit t1", "t1 DONE"));
        });
        test("one NO aborts: prepare stops there and only YES voters hear abort", () -> {
            var events = new ArrayList<String>();
            var log = new ArrayList<String>();
            var c = new Coordinator(cluster(events, Vote.YES, Vote.NO, Vote.YES), log);
            eq(c.run("t1"), Outcome.ABORTED);
            eq(events, List.of("p1 prepare t1", "p2 prepare t1", "p1 abort t1"));
            eq(log, List.of("t1 BEGIN", "t1 ABORT", "t1 DONE"));
        });
        test("a participant that does not answer counts as a NO", () -> {
            var events = new ArrayList<String>();
            var log = new ArrayList<String>();
            var c = new Coordinator(cluster(events, Vote.NO_RESPONSE, Vote.YES), log);
            eq(c.run("t1"), Outcome.ABORTED);
            eq(events, List.of("p1 prepare t1"));
            eq(log, List.of("t1 BEGIN", "t1 ABORT", "t1 DONE"));
        });
        test("a failure in phase two keeps the decision but writes no DONE", () -> {
            var events = new ArrayList<String>();
            var log = new ArrayList<String>();
            var ps = cluster(events, Vote.YES, Vote.YES, Vote.YES);
            ps.get(1).failingCalls = 1;
            eq(new Coordinator(ps, log).run("t1"), Outcome.COMMITTED);
            eq(events.subList(3, events.size()), List.of("p1 commit t1", "p2 commit t1 FAILED", "p3 commit t1"));
            eq(log, List.of("t1 BEGIN", "t1 COMMIT"));
        });
        test("recovery re-sends an unfinished commit to everyone, then marks it DONE", () -> {
            var events = new ArrayList<String>();
            var log = new ArrayList<String>();
            var ps = cluster(events, Vote.YES, Vote.YES);
            ps.get(1).failingCalls = 1;
            var c = new Coordinator(ps, log);
            c.run("t1");
            events.clear();
            eq(c.recover(), List.of("t1"));
            eq(events, List.of("p1 commit t1", "p2 commit t1"));
            eq(log, List.of("t1 BEGIN", "t1 COMMIT", "t1 DONE"));
        });
        test("recovery presumes abort when no decision was logged", () -> {
            var events = new ArrayList<String>();
            var log = new ArrayList<>(List.of("t9 BEGIN"));
            var c = new Coordinator(cluster(events, Vote.YES, Vote.YES), log);
            eq(c.recover(), List.of("t9"));
            eq(events, List.of("p1 abort t9", "p2 abort t9"));
            eq(log, List.of("t9 BEGIN", "t9 ABORT", "t9 DONE"));
        });
        test("recovery re-sends a logged abort to every participant", () -> {
            var events = new ArrayList<String>();
            var log = new ArrayList<>(List.of("t3 BEGIN", "t3 ABORT"));
            var c = new Coordinator(cluster(events, Vote.YES, Vote.NO), log);
            eq(c.recover(), List.of("t3"));
            eq(events, List.of("p1 abort t3", "p2 abort t3"));
            eq(log, List.of("t3 BEGIN", "t3 ABORT", "t3 DONE"));
        });
        test("recovery skips finished transactions and works in log order", () -> {
            var events = new ArrayList<String>();
            var log = new ArrayList<>(List.of("t1 BEGIN", "t1 COMMIT", "t1 DONE",
                "t2 BEGIN", "t3 BEGIN", "t4 BEGIN", "t4 COMMIT", "t3 ABORT"));
            var c = new Coordinator(cluster(events, Vote.YES), log);
            eq(c.recover(), List.of("t2", "t3", "t4"));
            eq(events, List.of("p1 abort t2", "p1 abort t3", "p1 commit t4"));
            eq(log.subList(8, log.size()), List.of("t2 ABORT", "t2 DONE", "t3 DONE", "t4 DONE"));
            eq(c.recover(), List.of());
        });
        test("a recovery that fails again leaves the work for next time", () -> {
            var events = new ArrayList<String>();
            var log = new ArrayList<>(List.of("t5 BEGIN", "t5 COMMIT"));
            var ps = cluster(events, Vote.YES, Vote.YES);
            ps.get(0).failingCalls = 1;
            var c = new Coordinator(ps, log);
            eq(c.recover(), List.of("t5"));
            eq(events, List.of("p1 commit t5 FAILED", "p2 commit t5"));
            eq(log, List.of("t5 BEGIN", "t5 COMMIT"));
            eq(c.recover(), List.of("t5"));
            eq(log, List.of("t5 BEGIN", "t5 COMMIT", "t5 DONE"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
