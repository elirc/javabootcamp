// ─────────────────────────────────────────────────────────────────────────
//  02 · functions as values                                 ★☆☆ warm-up
//  concepts: lambdas · Function · UnaryOperator · Predicate · generics
//  run: java -cp ../../_lib/out 02-functional-interfaces.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Three tiny utilities every later exercise leans on. applyTwice runs a
//  function on a value, then on the result. countMatching counts the items
//  a predicate accepts. pipe glues two functions into one: first, then
//  second.
//
//      applyTwice(x -> x * 3, 2)                        → 18
//      countMatching(List.of(1, 5, 10), n -> n > 3)     → 2
//      pipe(String::trim, String::length).apply(" ab ") → 2
//      pipe(x -> x + 1, x -> x * 2).apply(3)            → 8
//
//  hint: a Function's method is apply; a Predicate's is test.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    static <T> T applyTwice(UnaryOperator<T> f, T value) {
        throw new UnsupportedOperationException("TODO");
    }

    static <T> int countMatching(List<T> items, Predicate<T> test) {
        throw new UnsupportedOperationException("TODO");
    }

    static <A, B, C> Function<A, C> pipe(Function<A, B> first, Function<B, C> second) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("applyTwice applies the function twice", () -> eq(applyTwice(x -> x * 3, 2), 18));
        test("applyTwice works on any type", () -> eq(applyTwice(s -> s + "!", "hi"), "hi!!"));
        test("countMatching counts accepted items", () -> eq(countMatching(List.of(1, 5, 10), n -> n > 3), 2));
        test("countMatching on an empty list is zero", () -> eq(countMatching(List.<String>of(), s -> true), 0));
        test("pipe runs both functions", () -> eq(pipe(String::trim, String::length).apply(" ab "), 2));
        test("pipe runs first, then second", () -> eq(pipe((Integer x) -> x + 1, x -> x * 2).apply(3), 8));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
