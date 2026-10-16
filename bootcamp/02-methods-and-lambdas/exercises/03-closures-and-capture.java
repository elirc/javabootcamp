// ─────────────────────────────────────────────────────────────────────────
//  03 · counters that remember                              ★★☆ core
//  concepts: closures · effectively final · IntSupplier · capture in loops
//  run: java -cp ../../_lib/out 03-closures-and-capture.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A quiz engine numbers its questions with a counter object, and builds a
//  row of "add k" functions for bonus points. Write counter: each call to
//  the returned supplier gives the next number, starting at start; two
//  counters never share a count. Then write adders: adders(n) is a list of
//  n functions where the k-th one adds k.
//
//      var next = counter(1);
//      next.getAsInt(), next.getAsInt(), next.getAsInt()   → 1, 2, 3
//      adders(3).get(2).applyAsInt(10)                     → 12
//
//  hint: a lambda may only capture locals that are never reassigned.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    static IntSupplier counter(int start) {
        throw new UnsupportedOperationException("TODO");
    }

    static List<IntUnaryOperator> adders(int n) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("a counter counts up from its start", () -> {
            var next = counter(1);
            eq(List.of(next.getAsInt(), next.getAsInt(), next.getAsInt()), List.of(1, 2, 3));
        });
        test("a counter can start anywhere", () -> eq(counter(-1).getAsInt(), -1));
        test("two counters do not share a count", () -> {
            var a = counter(10);
            var b = counter(10);
            a.getAsInt();
            a.getAsInt();
            eq(b.getAsInt(), 10);
            eq(a.getAsInt(), 12);
        });
        test("adders makes n functions", () -> eq(adders(3).size(), 3));
        test("each adder adds its own index", () -> {
            var fs = adders(3);
            eq(fs.get(0).applyAsInt(10), 10);
            eq(fs.get(1).applyAsInt(10), 11);
            eq(fs.get(2).applyAsInt(10), 12);
        });
        test("adders(0) is empty", () -> eq(adders(0), List.of()));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
