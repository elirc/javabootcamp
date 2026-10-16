// ─────────────────────────────────────────────────────────────────────────
//  03 · at-least-once, applied once                         ★★☆ core
//  concepts: at-least-once delivery · idempotent consumers · dedupe windows
//  run: java -cp ../../_lib/out 03-dedupe-consumer.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Brokers like SQS and Kafka deliver AT LEAST once, so a consumer sees
//  repeats. Finish the nested class DedupingConsumer(window, effect)
//  (window < 1 → IllegalArgumentException). handle(msg) runs
//  effect.accept(msg) and returns "applied <id>" the first time an id is
//  seen, or returns "duplicate <id>" without running the effect if the id
//  is remembered. If the effect throws, the exception propagates and the
//  id is NOT remembered, so a redelivery tries again. Only the last
//  `window` applied ids are remembered: when a new id would exceed it, the
//  oldest-applied id is forgotten (a duplicate does not refresh an id's
//  place). remembered() lists the remembered ids, oldest first.
//
//      window 2:  handle(m1) → "applied m1"   handle(m1) → "duplicate m1"
//                 handle(m2), handle(m3)       remembered() → [m2, m3]
//                 handle(m1) → "applied m1"   (forgotten, so applied again)
//
//  hint: a LinkedHashSet remembers insertion order and answers contains.

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
            throw new UnsupportedOperationException("TODO");
        }

        String handle(Message msg) {
            throw new UnsupportedOperationException("TODO");
        }

        List<String> remembered() {
            throw new UnsupportedOperationException("TODO");
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
