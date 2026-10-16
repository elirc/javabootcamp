// ─────────────────────────────────────────────────────────────────────────
//  07 · remember the answer — SOLUTION                      ★★★ stretch
//  concepts: higher-order functions · HashMap cache · null values · closures
//  run: java -cp ../../_lib/out 07-memoize.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  memoize is a function that returns a function, and the returned lambda
//  closes over a cache that memoize created. Each call to memoize builds a
//  new HashMap, which is exactly why two memoized functions are
//  independent — the cache's lifetime is the closure's.
//  The tempting one-liner is `return k -> cache.computeIfAbsent(k, f);`.
//  It is right for most inputs and wrong for this spec: computeIfAbsent
//  treats a null result as "no mapping" and calls f again next time. A
//  lookup that legitimately answers "nothing" (no quote for that zip)
//  would hit the slow service on every call.
//  The fix separates "absent" from "present with value null":
//  containsKey first, then get — or store Optional values. That
//  distinction (missing key vs null value) is a recurring Java theme;
//  Map.get alone cannot tell them apart.
//  Two more real-world caveats: this cache never evicts, so it grows
//  forever (fine for a bounded key space), and a HashMap is not
//  thread-safe — a shared memoizer needs ConcurrentHashMap, which in turn
//  refuses null values outright. Also never call computeIfAbsent
//  recursively on the same HashMap; since Java 9 it throws
//  ConcurrentModificationException.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    static <K, V> Function<K, V> memoize(Function<K, V> f) {
        var cache = new HashMap<K, V>();
        return key -> {
            if (cache.containsKey(key)) return cache.get(key);
            var value = f.apply(key);
            cache.put(key, value);
            return value;
        };
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("gives the same answers as the function", () -> {
            var len = memoize(String::length);
            eq(len.apply("abc"), 3);
            eq(len.apply("abcde"), 5);
        });
        test("calls the function once per input", () -> {
            var calls = new int[1];
            var quote = memoize((String zip) -> { calls[0]++; return zip.length(); });
            quote.apply("10115");
            quote.apply("10115");
            quote.apply("10115");
            eq(calls[0], 1);
        });
        test("computes each distinct input", () -> {
            var calls = new int[1];
            var square = memoize((Integer x) -> { calls[0]++; return x * x; });
            eq(square.apply(3), 9);
            eq(square.apply(4), 16);
            eq(square.apply(3), 9);
            eq(calls[0], 2);
        });
        test("remembers a null answer", () -> {
            var calls = new int[1];
            var lookup = memoize((String zip) -> { calls[0]++; return (Integer) null; });
            eq(lookup.apply("00000"), null);
            eq(lookup.apply("00000"), null);
            eq(calls[0], 1);
        });
        test("two memoized functions keep separate caches", () -> {
            var calls = new int[1];
            Function<String, Integer> slow = s -> { calls[0]++; return s.length(); };
            var a = memoize(slow);
            var b = memoize(slow);
            a.apply("x");
            b.apply("x");
            eq(calls[0], 2);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
