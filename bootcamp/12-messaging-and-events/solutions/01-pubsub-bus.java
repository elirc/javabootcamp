// ─────────────────────────────────────────────────────────────────────────
//  01 · an in-process pub/sub bus — SOLUTION                ★☆☆ warm-up
//  concepts: publish/subscribe · topics · wildcards · failure isolation
//  run: java -cp ../../_lib/out 01-pubsub-bus.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Pub/sub decouples the code that notices something from the code that
//  reacts to it: the order service publishes "orders.created" and never
//  learns that billing, email and analytics subscribe. Guava's EventBus,
//  Spring application events, Redis PUBLISH/PSUBSCRIBE and RabbitMQ topic
//  exchanges are all this shape; Kafka and SNS are the same idea across
//  processes. Wildcard rules differ: in RabbitMQ "orders.*" matches exactly
//  one more word and "orders.#" any number, so this bus's ".*" behaves
//  like RabbitMQ's ".#" (minus the zero-word case).
//  Three details make a bus trustworthy. First, ordering: a List of
//  subscriptions gives deterministic delivery order. Second, isolation:
//  one buggy subscriber must not stop the others, so each call gets its
//  own try/catch, and the failure is counted (in production: logged and
//  metered) rather than swallowed silently. Third, unsubscribe must
//  remove exactly the subscription it came from. Removing "the first
//  entry whose handler equals this one" goes wrong when one handler is
//  subscribed twice; holding the Subscription object in the returned
//  Runnable and removing by identity cannot.
//  The wildcard check is startsWith(prefix + "."): without the dot,
//  "orders.*" would also match "ordersx.created", and "orders" alone is
//  not an event about an order. Iterate over a copy of the list so a
//  handler that unsubscribes during publish cannot break the loop.

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
                if (pattern.endsWith(".*")) return topic.startsWith(pattern.substring(0, pattern.length() - 1));
                return pattern.equals(topic);
            }
        }

        private final List<Subscription> subscriptions = new ArrayList<>();
        private int failures;

        Runnable subscribe(String pattern, Consumer<String> handler) {
            var sub = new Subscription(pattern, handler);
            subscriptions.add(sub);
            return () -> subscriptions.remove(sub);
        }

        int publish(String topic, String message) {
            int delivered = 0;
            for (var sub : List.copyOf(subscriptions)) {
                if (!sub.matches(topic)) continue;
                try {
                    sub.handler.accept(message);
                    delivered++;
                } catch (RuntimeException e) {
                    failures++;
                }
            }
            return delivered;
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
