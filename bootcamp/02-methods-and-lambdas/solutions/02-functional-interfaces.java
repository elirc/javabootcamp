// ─────────────────────────────────────────────────────────────────────────
//  02 · functions as values — SOLUTION                      ★☆☆ warm-up
//  concepts: lambdas · Function · UnaryOperator · Predicate · generics
//  run: java -cp ../../_lib/out 02-functional-interfaces.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Java has no function TYPES — it has functional INTERFACES: any interface
//  with exactly one abstract method. A lambda or method reference is an
//  instance of whichever one the context asks for. java.util.function
//  gives you the standard shapes: Function<A, B> (apply), UnaryOperator<T>
//  (a Function<T, T>), Predicate<T> (test, returns boolean), Supplier<T>
//  (get), Consumer<T> (accept), plus primitive versions like IntPredicate
//  that avoid boxing.
//  applyTwice is f.apply(f.apply(value)). countMatching is a loop with a
//  test, or items.stream().filter(test).count() — note count() returns a
//  long, so the int return type needs a cast (or Math.toIntExact).
//  pipe returns a NEW function: a lambda that closes over first and
//  second. The JDK already has it — first.andThen(second) — and the order
//  trap is exactly the 4th example: andThen runs first-then-second;
//  compose runs the ARGUMENT first. 3 + 1 then * 2 is 8; the other order
//  gives 7.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    static <T> T applyTwice(UnaryOperator<T> f, T value) {
        return f.apply(f.apply(value));
    }

    static <T> int countMatching(List<T> items, Predicate<T> test) {
        return (int) items.stream().filter(test).count();
    }

    static <A, B, C> Function<A, C> pipe(Function<A, B> first, Function<B, C> second) {
        return a -> second.apply(first.apply(a));
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
