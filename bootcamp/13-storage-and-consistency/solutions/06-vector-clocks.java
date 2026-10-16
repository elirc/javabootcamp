// ─────────────────────────────────────────────────────────────────────────
//  06 · who saw what first — SOLUTION                       ★★☆ core
//  concepts: vector clocks · happens-before · concurrency · siblings
//  run: java -cp ../../_lib/out 06-vector-clocks.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Wall clocks on different machines disagree, so "later timestamp wins"
//  (exercise 05) can throw away a write that nobody had seen. A vector
//  clock records causality instead: one counter per node, ticked by that
//  node on each write it makes, and merged (per-node max) when a node
//  learns another's state. Then x happened before y exactly when every
//  counter of x is <= y's and at least one is smaller. If each clock has
//  a counter the other lacks, neither saw the other: the writes are
//  CONCURRENT, and only the application can merge them (Dynamo's
//  shopping cart keeps both as "siblings").
//  compare walks the union of both key sets, treating a missing node as
//  0. The classic slip is iterating only this clock's keys, which makes
//  {a:1} look equal to {a:1, b:1}. Another is answering BEFORE as soon
//  as one counter is smaller, without checking that none is larger.
//  reconcile keeps a version unless some other version is strictly
//  AFTER it (that one already includes it), and keeps only the first of
//  several EQUAL versions. What survives is the set of concurrent
//  siblings, in input order.
//  Clocks are immutable values: tick and merge return new clocks, so a
//  clock attached to a stored version can never change behind it.

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
            var next = new HashMap<>(counts);
            next.merge(node, 1, Integer::sum);
            return new VClock(next);
        }

        VClock merge(VClock other) {
            var next = new HashMap<>(counts);
            other.counts.forEach((node, n) -> next.merge(node, n, Math::max));
            return new VClock(next);
        }

        Order compare(VClock other) {
            var nodes = new HashSet<>(counts.keySet());
            nodes.addAll(other.counts.keySet());
            boolean smaller = false, larger = false;
            for (var node : nodes) {
                if (get(node) < other.get(node)) smaller = true;
                if (get(node) > other.get(node)) larger = true;
            }
            if (smaller && larger) return Order.CONCURRENT;
            if (smaller) return Order.BEFORE;
            if (larger) return Order.AFTER;
            return Order.EQUAL;
        }
    }

    record Versioned(String value, VClock clock) {}

    static List<Versioned> reconcile(List<Versioned> versions) {
        var kept = new ArrayList<Versioned>();
        for (int i = 0; i < versions.size(); i++) {
            var mine = versions.get(i).clock();
            boolean drop = false;
            for (int j = 0; j < versions.size() && !drop; j++) {
                var order = mine.compare(versions.get(j).clock());
                drop = order == Order.BEFORE || (order == Order.EQUAL && j < i);
            }
            if (!drop) kept.add(versions.get(i));
        }
        return kept;
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
