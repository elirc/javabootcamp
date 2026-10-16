// ─────────────────────────────────────────────────────────────────────────
//  03 · at-least-once, applied once — SOLUTION              ★★☆ core
//  concepts: at-least-once delivery · idempotent consumers · dedupe windows
//  run: java -cp ../../_lib/out 03-dedupe-consumer.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Exactly-once delivery across a network is not on offer: the broker
//  cannot tell "the consumer crashed before acking" from "the ack got
//  lost", so it redelivers. What you can build is exactly-once EFFECT:
//  at-least-once delivery plus a consumer that recognises repeats by
//  message id (the "idempotent consumer" / inbox pattern).
//  Order inside handle is the whole design. Check the id; run the effect;
//  only then remember the id. Remembering first turns a crash or a thrown
//  exception into a message that is silently never processed: the
//  redelivery arrives, looks like a duplicate, and is skipped. With the
//  effect first, the worst case is a duplicate effect after a crash, which
//  is the case the dedupe exists for. (In a real service, the effect and
//  the "seen" row go into one database transaction, which closes even
//  that gap; module 08's payments made the same choice.)
//  Memory is not free, so the seen-set is a window. A LinkedHashSet keeps
//  first-insertion order, so the eldest id is iterator().next(). The
//  window must be wider than the broker's redelivery horizon (SQS's
//  visibility timeout, a Kafka consumer's rewind after a rebalance):
//  anything older than the window is applied again, as one of the tests
//  shows. A duplicate does not re-add the id, so it keeps its original
//  place in line and ages out on schedule.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    record Message(String id, long cents) {}

    static final class DedupingConsumer {
        private final int window;
        private final Consumer<Message> effect;
        private final LinkedHashSet<String> seen = new LinkedHashSet<>();

        DedupingConsumer(int window, Consumer<Message> effect) {
            if (window < 1) throw new IllegalArgumentException("window must be at least 1: " + window);
            this.window = window;
            this.effect = effect;
        }

        String handle(Message msg) {
            if (seen.contains(msg.id())) return "duplicate " + msg.id();
            effect.accept(msg);
            seen.add(msg.id());
            if (seen.size() > window) seen.remove(seen.iterator().next());
            return "applied " + msg.id();
        }

        List<String> remembered() {
            return List.copyOf(seen);
        }
    }

    static Message m(String id) {
        return new Message(id, 100);
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("applies a new message", () -> {
            var applied = new ArrayList<String>();
            var c = new DedupingConsumer(10, msg -> applied.add(msg.id()));
            eq(c.handle(m("m1")), "applied m1");
            eq(applied, List.of("m1"));
        });
        test("a redelivered message is a duplicate and has no effect", () -> {
            var applied = new ArrayList<String>();
            var c = new DedupingConsumer(10, msg -> applied.add(msg.id()));
            c.handle(m("m1"));
            eq(c.handle(m("m1")), "duplicate m1");
            eq(c.handle(m("m1")), "duplicate m1");
            eq(applied, List.of("m1"));
        });
        test("a failed effect is not remembered, so redelivery retries", () -> {
            var attempts = new int[1];
            var c = new DedupingConsumer(10, msg -> {
                if (attempts[0]++ == 0) throw new IllegalStateException("db down");
            });
            raises(IllegalStateException.class, () -> c.handle(m("m1")));
            eq(c.remembered(), List.of());
            eq(c.handle(m("m1")), "applied m1");
            eq(attempts[0], 2);
        });
        test("remembers only the last window ids, oldest first", () -> {
            var c = new DedupingConsumer(2, msg -> {});
            c.handle(m("m1"));
            c.handle(m("m2"));
            eq(c.remembered(), List.of("m1", "m2"));
            c.handle(m("m3"));
            eq(c.remembered(), List.of("m2", "m3"));
        });
        test("a duplicate does not refresh its place in the window", () -> {
            var c = new DedupingConsumer(2, msg -> {});
            c.handle(m("a"));
            c.handle(m("b"));
            c.handle(m("a"));
            c.handle(m("c"));
            eq(c.remembered(), List.of("b", "c"));
        });
        test("an id older than the window is applied again", () -> {
            var applied = new ArrayList<String>();
            var c = new DedupingConsumer(2, msg -> applied.add(msg.id()));
            for (var id : List.of("m1", "m2", "m3")) c.handle(m(id));
            eq(c.handle(m("m1")), "applied m1");
            eq(applied, List.of("m1", "m2", "m3", "m1"));
        });
        test("a redelivery storm moves each balance exactly once", () -> {
            var balance = new long[1];
            var c = new DedupingConsumer(100, msg -> balance[0] += msg.cents());
            var deliveries = List.of(new Message("p1", 500), new Message("p2", 250), new Message("p1", 500),
                                     new Message("p3", 125), new Message("p2", 250), new Message("p1", 500));
            var outcomes = new ArrayList<String>();
            for (var d : deliveries) outcomes.add(c.handle(d));
            eq(balance[0], 875);
            eq(outcomes, List.of("applied p1", "applied p2", "duplicate p1",
                                 "applied p3", "duplicate p2", "duplicate p1"));
        });
        test("refuses a window below one", () ->
            raises(IllegalArgumentException.class, () -> new DedupingConsumer(0, msg -> {})));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
