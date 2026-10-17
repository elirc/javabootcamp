// ─────────────────────────────────────────────────────────────────────────
//  05 · the outbox that loses messages                      ★★☆ core
//  concepts: debugging · transactional outbox · at-least-once · ordering
//  run: java -cp ../../_lib/out 05-outbox-lost-message.java
// ─────────────────────────────────────────────────────────────────────────
//
//  DEBUG HUNT: the code below is complete and compiles; it has bugs. Run
//  it, read the failing tests, find and fix the bugs. Do not edit tests.
//  Intended: the service writes each event into an Outbox (add) in the
//  same transaction as its data. relay(broker) is run by a background
//  job: it publishes the unsent messages in seq order, marks a message
//  sent only AFTER the broker accepted it, and STOPS at the first
//  failure, so nothing overtakes the failed message and the next run
//  starts with it. relay returns how many it published; unsent() lists
//  the unsent seqs in order. A message is never published twice by a
//  relay that saw it succeed.
//  Bug reports:
//    "The broker had a 2-second blip. Message 7 was never delivered,
//     though the relay has run hundreds of times since."
//    "Consumers saw order 8 shipped before order 7 was created."
//
//      broker fails once on seq 2: relay → 1 (published [1]); relay → 2
//                                  (published [2, 3]); broker saw 1, 2, 3
//
//  hint: what does the outbox believe after an exception from publish?

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Message(long seq, String payload) {}

    interface Broker {
        void publish(Message m) throws Exception;
    }

    static final class Outbox {
        private final Map<Long, Message> messages = new TreeMap<>();
        private final Set<Long> sent = new HashSet<>();

        void add(Message m) {
            messages.put(m.seq(), m);
        }

        int relay(Broker broker) {
            int published = 0;
            for (var m : messages.values()) {
                if (sent.contains(m.seq())) continue;
                sent.add(m.seq());
                try {
                    broker.publish(m);
                    published++;
                } catch (Exception e) {
                    continue;
                }
            }
            return published;
        }

        List<Long> unsent() {
            return messages.keySet().stream().filter(s -> !sent.contains(s)).toList();
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("publishes every message in seq order", () -> {
            var box = new Outbox();
            box.add(new Message(3, "c"));
            box.add(new Message(1, "a"));
            box.add(new Message(2, "b"));
            var seen = new ArrayList<Long>();
            eq(box.relay(m -> seen.add(m.seq())), 3);
            eq(seen, List.of(1L, 2L, 3L));
            eq(box.unsent(), List.of());
        });
        test("a second run publishes nothing again", () -> {
            var box = new Outbox();
            box.add(new Message(1, "a"));
            var seen = new ArrayList<Long>();
            box.relay(m -> seen.add(m.seq()));
            eq(box.relay(m -> seen.add(m.seq())), 0);
            eq(seen, List.of(1L));
        });
        test("a message the broker rejected is published on the next run", () -> {
            var box = new Outbox();
            box.add(new Message(1, "a"));
            var seen = new ArrayList<Long>();
            boolean[] down = {true};
            Broker flaky = m -> {
                if (down[0]) throw new Exception("broker unavailable");
                seen.add(m.seq());
            };
            eq(box.relay(flaky), 0);
            eq(box.unsent(), List.of(1L));
            down[0] = false;
            eq(box.relay(flaky), 1);
            eq(seen, List.of(1L));
        });
        test("a failure stops the run so nothing overtakes it", () -> {
            var box = new Outbox();
            for (long s = 1; s <= 3; s++) box.add(new Message(s, "m" + s));
            var seen = new ArrayList<Long>();
            int[] failuresLeft = {1};
            Broker flaky = m -> {
                if (m.seq() == 2 && failuresLeft[0]-- > 0) throw new Exception("blip");
                seen.add(m.seq());
            };
            eq(box.relay(flaky), 1);
            eq(box.unsent(), List.of(2L, 3L));
            eq(box.relay(flaky), 2);
            eq(seen, List.of(1L, 2L, 3L));
        });
        test("messages added after a failure queue behind the failed one", () -> {
            var box = new Outbox();
            box.add(new Message(1, "a"));
            var seen = new ArrayList<Long>();
            boolean[] down = {true};
            Broker flaky = m -> {
                if (down[0]) throw new Exception("down");
                seen.add(m.seq());
            };
            box.relay(flaky);
            box.add(new Message(2, "b"));
            down[0] = false;
            box.relay(flaky);
            eq(seen, List.of(1L, 2L));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
