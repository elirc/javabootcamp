// ─────────────────────────────────────────────────────────────────────────
//  01 · an in-process pub/sub bus                           ★☆☆ warm-up
//  concepts: publish/subscribe · topics · wildcards · failure isolation
//  run: java -cp ../../_lib/out 01-pubsub-bus.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Services announce facts ("orders.created") without knowing who listens.
//  Finish the nested class EventBus and its Subscription.matches.
//  subscribe(pattern, handler) returns a Runnable that unsubscribes
//  exactly that subscription (running it again does nothing). A pattern
//  is an exact topic, or ends in ".*" and matches every topic that starts
//  with the part before the "*" (prefix plus dot):
//  "orders.*" matches "orders.created" and "orders.eu.created", but not
//  "orders" or "ordersx.created". publish(topic, message) calls every
//  matching handler in subscription order and returns how many completed
//  normally. A handler that throws is skipped, counted by failures(), and
//  does not stop the rest. Subscribing the same handler twice delivers
//  twice.
//
//      subscribe("orders.*", log::add)
//      publish("orders.created", "#17")   → 1
//      publish("payments.failed", "#17")  → 0
//
//  hint: keep one object per subscription; unsubscribe removes that one.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    static final class EventBus {
        private static final class Subscription {
            final String pattern;
            final Consumer<String> handler;

            Subscription(String pattern, Consumer<String> handler) {
                this.pattern = pattern;
                this.handler = handler;
            }

            boolean matches(String topic) {
                throw new UnsupportedOperationException("TODO");
            }
        }

        private final List<Subscription> subscriptions = new ArrayList<>();
        private int failures;

        Runnable subscribe(String pattern, Consumer<String> handler) {
            throw new UnsupportedOperationException("TODO");
        }

        int publish(String topic, String message) {
            throw new UnsupportedOperationException("TODO");
        }

        int failures() {
            return failures;
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("delivers to the topic's subscribers in subscription order", () -> {
            var bus = new EventBus();
            var log = new ArrayList<String>();
            bus.subscribe("orders.created", m -> log.add("billing " + m));
            bus.subscribe("orders.created", m -> log.add("email " + m));
            eq(bus.publish("orders.created", "#17"), 2);
            eq(log, List.of("billing #17", "email #17"));
        });
        test("exact topics match only themselves", () -> {
            var bus = new EventBus();
            var log = new ArrayList<String>();
            bus.subscribe("orders", log::add);
            eq(bus.publish("orders.created", "a"), 0);
            eq(bus.publish("payments", "b"), 0);
            eq(bus.publish("orders", "c"), 1);
            eq(log, List.of("c"));
        });
        test("a wildcard matches deeper topics but not the bare prefix", () -> {
            var bus = new EventBus();
            var log = new ArrayList<String>();
            bus.subscribe("orders.*", log::add);
            bus.publish("orders.created", "1");
            bus.publish("orders.eu.created", "2");
            bus.publish("orders", "3");
            bus.publish("ordersx.created", "4");
            eq(log, List.of("1", "2"));
        });
        test("a failing subscriber does not stop the others", () -> {
            var bus = new EventBus();
            var log = new ArrayList<String>();
            bus.subscribe("t", m -> log.add("first"));
            bus.subscribe("t", m -> { throw new IllegalStateException("broken"); });
            bus.subscribe("t", m -> log.add("third"));
            eq(bus.publish("t", "x"), 2);
            eq(log, List.of("first", "third"));
            eq(bus.failures(), 1);
        });
        test("unsubscribing stops delivery and is safe to repeat", () -> {
            var bus = new EventBus();
            var log = new ArrayList<String>();
            var stop = bus.subscribe("t", m -> log.add("a " + m));
            bus.subscribe("t", m -> log.add("b " + m));
            bus.publish("t", "1");
            stop.run();
            stop.run();
            bus.publish("t", "2");
            eq(log, List.of("a 1", "b 1", "b 2"));
        });
        test("the same handler subscribed twice gets every message twice", () -> {
            var bus = new EventBus();
            var log = new ArrayList<String>();
            Consumer<String> handler = log::add;
            bus.subscribe("t", handler);
            bus.subscribe("t", handler);
            eq(bus.publish("t", "x"), 2);
            eq(log, List.of("x", "x"));
        });
        test("unsubscribe removes exactly its own subscription", () -> {
            var bus = new EventBus();
            var log = new ArrayList<String>();
            Consumer<String> handler = log::add;
            bus.subscribe("t", handler);
            var second = bus.subscribe("t.*", handler);
            second.run();
            bus.publish("t", "kept");
            bus.publish("t.sub", "gone");
            eq(log, List.of("kept"));
        });
        test("publishing with no subscribers delivers nothing", () -> {
            var bus = new EventBus();
            eq(bus.publish("nobody.listens", "x"), 0);
            eq(bus.failures(), 0);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
