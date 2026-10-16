// ─────────────────────────────────────────────────────────────────────────
//  05 · merge without asking                                ★☆☆ warm-up
//  concepts: CRDTs · G-counter · last-writer-wins · idempotent merge
//  run: java -cp ../../_lib/out 05-crdt-merge.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Three replicas count page views while the network is split, then
//  gossip their states in any order, any number of times. Finish the
//  grow-only counter GCounter: increment(node, by) adds to that node's
//  slot (by < 1 → IllegalArgumentException); value() is the sum of all
//  slots; merge(other) returns a NEW counter whose slot for each node is
//  the larger of the two (neither input changes, nor shares its map).
//  Then write merge(a, b) for LwwRegister: the higher timestamp wins; on
//  a tie the register whose node id is larger (String.compareTo) wins,
//  so every replica picks the same one.
//
//      x = {a:5, b:1}   y = {a:3, b:4}   x.merge(y) → {a:5, b:4}, value 9
//      merge((tea,10,n1), (cake,10,n2))  → (cake,10,n2)
//
//  hint: gossip repeats itself; merging a state twice must be harmless.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class GCounter {
        private final Map<String, Long> slots = new HashMap<>();

        long countOf(String node) {
            return slots.getOrDefault(node, 0L);
        }

        void increment(String node, long by) {
            throw new UnsupportedOperationException("TODO");
        }

        long value() {
            throw new UnsupportedOperationException("TODO");
        }

        GCounter merge(GCounter other) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    record LwwRegister(String value, long timestamp, String node) {}

    static LwwRegister merge(LwwRegister a, LwwRegister b) {
        throw new UnsupportedOperationException("TODO");
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
