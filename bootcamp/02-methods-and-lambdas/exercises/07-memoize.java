// ─────────────────────────────────────────────────────────────────────────
//  07 · remember the answer                                 ★★★ stretch
//  concepts: higher-order functions · HashMap cache · null values · closures
//  run: java -cp ../../_lib/out 07-memoize.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An order system looks up shipping quotes through a slow function. Write
//  memoize: it takes a function and returns a new one that gives the same
//  answers but calls the original at most once per distinct input. A null
//  answer counts as an answer — it is remembered too. Two memoized
//  functions never share a cache.
//
//      var quote = memoize(slowQuote);    // slowQuote counts its calls
//      quote.apply("10115");
//      quote.apply("10115");              → slowQuote ran once
//
//  hint: Map.computeIfAbsent does not store null results.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    static <K, V> Function<K, V> memoize(Function<K, V> f) {
        throw new UnsupportedOperationException("TODO");
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
