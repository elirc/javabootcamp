// ─────────────────────────────────────────────────────────────────────────
//  05 · merge without asking — SOLUTION                     ★☆☆ warm-up
//  concepts: CRDTs · G-counter · last-writer-wins · idempotent merge
//  run: java -cp ../../_lib/out 05-crdt-merge.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Replicas that accept writes while cut off from each other need a merge
//  that always agrees, whatever order the gossip arrives in and however
//  many times it is repeated. A CRDT gets that by making merge
//  commutative, associative and idempotent. The grow-only counter does it
//  by giving every node its own slot that only that node increments;
//  merge takes the per-node MAXIMUM and the value is the sum of slots.
//  The tempting wrong merge is adding the two counters: merging the same
//  state twice (which gossip does all the time) would count it twice.
//  Max is idempotent: max(x, x) = x.
//  merge returns a new counter and copies its inputs' maps, so a replica
//  never shares mutable state with the one it merged from.
//  A last-writer-wins register keeps the write with the higher timestamp.
//  Equal timestamps are the trap: "keep mine" makes two replicas each
//  keep their own value forever. Breaking ties by a total order every
//  replica agrees on (here the larger node id) makes merge(a, b) and
//  merge(b, a) identical. LWW is simple but lossy: the losing concurrent
//  write is silently discarded, which is why vector clocks (exercise 06)
//  exist.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class GCounter {
        private final Map<String, Long> slots = new HashMap<>();

        long countOf(String node) {
            return slots.getOrDefault(node, 0L);
        }

        void increment(String node, long by) {
            if (by < 1) throw new IllegalArgumentException("increments must be positive: " + by);
            slots.merge(node, by, Long::sum);
        }

        long value() {
            return slots.values().stream().mapToLong(Long::longValue).sum();
        }

        GCounter merge(GCounter other) {
            var out = new GCounter();
            out.slots.putAll(slots);
            other.slots.forEach((node, n) -> out.slots.merge(node, n, Math::max));
            return out;
        }
    }

    record LwwRegister(String value, long timestamp, String node) {}

    static LwwRegister merge(LwwRegister a, LwwRegister b) {
        if (a.timestamp() != b.timestamp()) return a.timestamp() > b.timestamp() ? a : b;
        return a.node().compareTo(b.node()) >= 0 ? a : b;
    }

    static GCounter counter(String node, long n) {
        var c = new GCounter();
        c.increment(node, n);
        return c;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("a counter sums every node's increments", () -> {
            var c = new GCounter();
            c.increment("a", 2);
            c.increment("a", 3);
            c.increment("b", 1);
            eq(c.value(), 6);
            eq(c.countOf("a"), 5);
        });
        test("increments must be positive", () -> {
            var c = new GCounter();
            raises(IllegalArgumentException.class, () -> c.increment("a", 0));
            raises(IllegalArgumentException.class, () -> c.increment("a", -4));
            eq(c.value(), 0);
        });
        test("merge takes each node's maximum, not the sum", () -> {
            var x = counter("a", 5);
            x.increment("b", 1);
            var y = counter("a", 3);
            y.increment("b", 4);
            var m = x.merge(y);
            eq(m.countOf("a"), 5);
            eq(m.countOf("b"), 4);
            eq(m.value(), 9);
        });
        test("merging the same state again changes nothing", () -> {
            var x = counter("a", 5);
            var y = counter("b", 2);
            var once = x.merge(y);
            eq(once.merge(y).value(), 7);
            eq(once.merge(once).value(), 7);
        });
        test("merge order does not matter", () -> {
            var x = counter("a", 1);
            var y = counter("b", 2);
            var z = counter("c", 4);
            z.increment("a", 3);
            eq(x.merge(y).merge(z).value(), 9);
            eq(z.merge(y.merge(x)).value(), 9);
            eq(y.merge(z).merge(x).countOf("a"), 3);
        });
        test("merge returns a new counter and leaves both inputs alone", () -> {
            var x = counter("a", 1);
            var y = counter("b", 2);
            var m = x.merge(y);
            m.increment("a", 10);
            eq(x.value(), 1);
            eq(y.value(), 2);
            eq(m.value(), 13);
        });
        test("a register keeps the later timestamp in either order", () -> {
            var older = new LwwRegister("tea", 10, "n1");
            var newer = new LwwRegister("cake", 11, "n0");
            eq(merge(older, newer), newer);
            eq(merge(newer, older), newer);
        });
        test("a timestamp tie goes to the larger node id on every replica", () -> {
            var a = new LwwRegister("tea", 10, "n1");
            var b = new LwwRegister("cake", 10, "n2");
            eq(merge(a, b), b);
            eq(merge(b, a), b);
            eq(merge(a, a), a);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
