// ─────────────────────────────────────────────────────────────────────────
//  06 · who saw what first                                  ★★☆ core
//  concepts: vector clocks · happens-before · concurrency · siblings
//  run: java -cp ../../_lib/out 06-vector-clocks.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A Dynamo-style store must tell "this write replaced that one" from
//  "these two writes never saw each other". Finish the immutable VClock:
//  tick(node) returns a copy with node's counter + 1; merge(other)
//  returns a copy holding each node's maximum; compare(other) returns
//  EQUAL (all counters equal), BEFORE (all <= and one <), AFTER (the
//  mirror), or CONCURRENT (some < and some >). A node missing from a
//  clock counts as 0. Then write reconcile(versions): drop every version
//  whose clock is BEFORE another version's clock, and of versions with
//  EQUAL clocks keep only the first; keep the rest in input order.
//
//      {a:1}      vs {a:1, b:1}  → BEFORE
//      {a:2}      vs {a:1, b:1}  → CONCURRENT
//      reconcile[{a:1}, {a:2}, {a:1,b:1}] → [{a:2}, {a:1,b:1}]
//
//  hint: compare over the union of both clocks' nodes.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    enum Order { BEFORE, AFTER, EQUAL, CONCURRENT }

    static final class VClock {
        private final Map<String, Integer> counts;

        VClock(Map<String, Integer> counts) {
            this.counts = Map.copyOf(counts);
        }

        int get(String node) {
            return counts.getOrDefault(node, 0);
        }

        VClock tick(String node) {
            throw new UnsupportedOperationException("TODO");
        }

        VClock merge(VClock other) {
            throw new UnsupportedOperationException("TODO");
        }

        Order compare(VClock other) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    record Versioned(String value, VClock clock) {}

    static List<Versioned> reconcile(List<Versioned> versions) {
        throw new UnsupportedOperationException("TODO");
    }

    static VClock vc(String... nodeThenCount) {
        var m = new HashMap<String, Integer>();
        for (int i = 0; i < nodeThenCount.length; i += 2) m.put(nodeThenCount[i], Integer.parseInt(nodeThenCount[i + 1]));
        return new VClock(m);
    }

    static List<String> values(List<Versioned> versions) {
        return versions.stream().map(Versioned::value).toList();
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("tick adds one to that node only and returns a new clock", () -> {
            var c = vc("a", "1", "b", "2");
            var t = c.tick("a").tick("c");
            eq(List.of(t.get("a"), t.get("b"), t.get("c")), List.of(2, 2, 1));
            eq(c.get("a"), 1);
            eq(c.get("c"), 0);
        });
        test("a clock is before its own tick", () -> {
            var c = vc("a", "1");
            eq(c.compare(c.tick("b")), Order.BEFORE);
            eq(c.tick("b").compare(c), Order.AFTER);
        });
        test("equal clocks are EQUAL and a missing node counts as zero", () -> {
            eq(vc("a", "1").compare(vc("a", "1")), Order.EQUAL);
            eq(vc("a", "1").compare(vc("a", "1", "b", "0")), Order.EQUAL);
            eq(vc().compare(vc()), Order.EQUAL);
        });
        test("every counter must be <= for BEFORE", () -> {
            eq(vc("a", "1", "b", "5").compare(vc("a", "2", "b", "5")), Order.BEFORE);
            eq(vc("a", "1", "b", "5").compare(vc("a", "2", "b", "4")), Order.CONCURRENT);
            eq(vc("a", "1").compare(vc("a", "1", "b", "1")), Order.BEFORE);
        });
        test("writes on two nodes that never talked are concurrent", () -> {
            var start = vc("a", "1");
            var left = start.tick("a");
            var right = start.tick("b");
            eq(left.compare(right), Order.CONCURRENT);
            eq(right.compare(left), Order.CONCURRENT);
        });
        test("merge takes per-node maxima and a tick after it follows both", () -> {
            var x = vc("a", "3", "b", "1");
            var y = vc("b", "4", "c", "2");
            var m = x.merge(y);
            eq(List.of(m.get("a"), m.get("b"), m.get("c")), List.of(3, 4, 2));
            eq(m.tick("a").compare(x), Order.AFTER);
            eq(m.tick("a").compare(y), Order.AFTER);
            eq(x.get("c"), 0);
        });
        test("reconcile drops versions another version already includes", () -> {
            var v1 = new Versioned("tea", vc("a", "1"));
            var v2 = new Versioned("tea,cake", vc("a", "2"));
            var v3 = new Versioned("tea,cake,jam", vc("a", "2", "b", "1"));
            eq(values(reconcile(List.of(v1, v2, v3))), List.of("tea,cake,jam"));
            eq(values(reconcile(List.of(v3, v1))), List.of("tea,cake,jam"));
        });
        test("reconcile keeps concurrent siblings in input order", () -> {
            var base = vc("a", "1");
            var left = new Versioned("milk", base.tick("a"));
            var right = new Versioned("sugar", base.tick("b"));
            var old = new Versioned("none", base);
            eq(values(reconcile(List.of(right, old, left))), List.of("sugar", "milk"));
        });
        test("reconcile keeps the first of equal versions", () -> {
            var a = new Versioned("first", vc("a", "1", "b", "1"));
            var b = new Versioned("second", vc("b", "1", "a", "1"));
            var c = new Versioned("other", vc("c", "1"));
            eq(values(reconcile(List.of(a, c, b))), List.of("first", "other"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
