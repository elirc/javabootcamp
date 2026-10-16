// ─────────────────────────────────────────────────────────────────────────
//  03 · counters that remember — SOLUTION                   ★★☆ core
//  concepts: closures · effectively final · IntSupplier · capture in loops
//  run: java -cp ../../_lib/out 03-closures-and-capture.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A lambda captures local variables BY VALUE, and Java only lets it
//  capture ones that are effectively final — never reassigned after
//  initialisation. So `int n = start; return () -> n++;` does not compile.
//  That rule is deliberate: it rules out the data races and the "which
//  value did I capture?" confusion other languages allow.
//  To keep mutable state you capture a final reference to something
//  mutable: a one-element array (`int[] n = {start}` then `n[0]++`), an
//  AtomicInteger (`n.getAndIncrement()`), or a small class with a field.
//  Each call to counter(...) creates a NEW box, so two counters are
//  independent — the state lives per closure, not per method.
//  In adders the loop variable `k` of a classic for loop IS reassigned (by
//  k++), so `x -> x + k` does not compile. Copy it first: `int add = k;`
//  — a fresh effectively-final local each iteration. C# and JavaScript
//  programmers know the bug this prevents: every lambda seeing the final
//  value of the loop variable. Java turns it into a compile error.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    static IntSupplier counter(int start) {
        int[] next = {start};
        return () -> next[0]++;
    }

    static List<IntUnaryOperator> adders(int n) {
        var result = new ArrayList<IntUnaryOperator>();
        for (int k = 0; k < n; k++) {
            int add = k;
            result.add(x -> x + add);
        }
        return result;
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
