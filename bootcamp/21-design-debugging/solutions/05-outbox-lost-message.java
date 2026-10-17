// ─────────────────────────────────────────────────────────────────────────
//  05 · the outbox that loses messages — SOLUTION           ★★☆ core
//  concepts: debugging · transactional outbox · at-least-once · ordering
//  run: java -cp ../../_lib/out 05-outbox-lost-message.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  1. Reproduce "message 7 never delivered": one message, a broker that
//     throws, then a healthy broker. The second relay publishes
//     nothing. Narrow: print sent after the first run; it already holds
//     seq 1. The relay marks BEFORE it publishes, so a failed publish
//     leaves the message marked and it is skipped forever. The whole
//     point of an outbox is at-least-once delivery: mark only after the
//     broker has accepted. Fix: move sent.add after publish. (If the
//     relay crashes between publish and mark, the message goes out
//     twice; that is the at-least-once price, and why consumers
//     deduplicate, module 12 exercise 03.)
//  2. Re-run: "a failure stops the run" still fails. On an exception the
//     loop continues, so 3 is published while 2 waits for the next run:
//     consumers see 3 before 2. Fix: stop the run (break or return) at
//     the first failure. Strict per-key ordering costs throughput when
//     the head message is stuck; real systems order per partition or
//     per aggregate, not globally, to limit that.
//  Fixing only bug 1 delivers everything, just out of order, which is
//  the harder bug to spot in production. The TreeMap keeps seq order;
//  a HashMap would only look ordered for small seqs.

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
                try {
                    broker.publish(m);
                } catch (Exception e) {
                    break;
                }
                sent.add(m.seq());
                published++;
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
