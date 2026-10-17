// ─────────────────────────────────────────────────────────────────────────
//  04 · a Result type for errors                            ★★☆ core
//  concepts: sealed generics · Result/Either · map/flatMap · error values
//  run: java -cp ../../_lib/out 04-result-type.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An order import reports bad lines as values, not exceptions. Result<T>
//  is sealed: Ok(value) or Err(error message). Finish:
//
//    map(f), flatMap(f)  f applied to an Ok; an Err passes through, f unused
//    mapError(f)         rewrite an Err's message; an Ok passes through
//    orElse(fallback)    the Ok's value, or the fallback for an Err
//    attempt(work)       Ok of work's result; if it throws, Err of the
//                        message, or of the exception's simple class name
//                        when the message is null
//    sequence(results)   Ok of all values in order, or the FIRST Err
//    parseLine(line)     "sku,qty", spaces around either part allowed; else
//                        Err "expected sku,qty: <line>" (not 2 parts),
//                        "missing sku", "bad quantity: <q>" (not an int)
//                        or "quantity must be positive: <q>"
//    parseOrder(lines)   Ok of all lines, or the first Err as "line N: ..."
//
//      parseOrder(["A,1", "B,x"])  → Err("line 2: bad quantity: x")
//  hint: split(",", -1) keeps trailing empty parts; split(",") drops them.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    sealed interface Result<T> permits Ok, Err {

        static <T> Result<T> ok(T value) {
            return new Ok<>(value);
        }

        static <T> Result<T> err(String error) {
            return new Err<>(error);
        }

        static <T> Result<T> attempt(Supplier<? extends T> work) {
            throw new UnsupportedOperationException("TODO");
        }

        static <T> Result<List<T>> sequence(List<Result<T>> results) {
            throw new UnsupportedOperationException("TODO");
        }

        default <R> Result<R> map(Function<? super T, ? extends R> f) {
            throw new UnsupportedOperationException("TODO");
        }

        default <R> Result<R> flatMap(Function<? super T, Result<R>> f) {
            throw new UnsupportedOperationException("TODO");
        }

        default Result<T> mapError(UnaryOperator<String> f) {
            throw new UnsupportedOperationException("TODO");
        }

        default T orElse(T fallback) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    record Ok<T>(T value) implements Result<T> {}

    record Err<T>(String error) implements Result<T> {}

    record OrderLine(String sku, int qty) {}

    static Result<OrderLine> parseLine(String line) {
        throw new UnsupportedOperationException("TODO");
    }

    static Result<List<OrderLine>> parseOrder(List<String> lines) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("map transforms an Ok and never calls f on an Err", () -> {
            var calls = new int[1];
            Function<Integer, Integer> times10 = x -> { calls[0]++; return x * 10; };
            eq(Result.ok(2).map(times10), new Ok<>(20));
            eq(Result.<Integer>err("boom").map(times10), new Err<>("boom"));
            eq(calls[0], 1);
        });
        test("flatMap chains steps that can fail", () -> {
            Function<Integer, Result<Integer>> atLeast5 = x -> x >= 5 ? Result.ok(x) : Result.err("too small: " + x);
            eq(Result.ok(10).flatMap(atLeast5), new Ok<>(10));
            eq(Result.ok(1).flatMap(atLeast5), new Err<>("too small: 1"));
            eq(Result.<Integer>err("earlier").flatMap(atLeast5), new Err<>("earlier"));
        });
        test("mapError rewrites only errors, and orElse unwraps with a fallback", () -> {
            eq(Result.<Integer>err("x").mapError(String::toUpperCase), new Err<>("X"));
            eq(Result.ok(3).mapError(String::toUpperCase), new Ok<>(3));
            eq(Result.ok(3).orElse(0), 3);
            eq(Result.<Integer>err("x").orElse(0), 0);
        });
        test("attempt turns an exception into an Err with its message or class name", () -> {
            eq(Result.attempt(() -> 5), new Ok<>(5));
            eq(Result.<Integer>attempt(() -> Integer.parseInt("x")), new Err<>("For input string: \"x\""));
            eq(Result.<Integer>attempt(() -> { throw new IllegalStateException(); }), new Err<>("IllegalStateException"));
        });
        test("sequence gives every value in order, or the first error", () -> {
            eq(Result.sequence(List.of(Result.ok(1), Result.ok(2))), new Ok<>(List.of(1, 2)));
            eq(Result.sequence(List.of(Result.ok(1), Result.<Integer>err("a"), Result.<Integer>err("b"))), new Err<>("a"));
            eq(Result.sequence(List.<Result<Integer>>of()), new Ok<>(List.of()));
        });
        test("parseLine accepts sku,qty with spaces around both parts", () -> {
            eq(parseLine(" AB-1 , 3 "), new Ok<>(new OrderLine("AB-1", 3)));
            eq(parseLine("Z,12"), new Ok<>(new OrderLine("Z", 12)));
        });
        test("parseLine names exactly what is wrong", () -> {
            eq(parseLine("AB-1"), new Err<>("expected sku,qty: AB-1"));
            eq(parseLine("a,b,c"), new Err<>("expected sku,qty: a,b,c"));
            eq(parseLine(" ,3"), new Err<>("missing sku"));
            eq(parseLine("AB,x"), new Err<>("bad quantity: x"));
            eq(parseLine("AB,"), new Err<>("bad quantity: "));
            eq(parseLine("AB,0"), new Err<>("quantity must be positive: 0"));
        });
        test("parseOrder reports the first bad line with its number", () -> {
            eq(parseOrder(List.of("A,1", "B,x", "C,0")), new Err<>("line 2: bad quantity: x"));
            eq(parseOrder(List.of("A,1", "B, 2")), new Ok<>(List.of(new OrderLine("A", 1), new OrderLine("B", 2))));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
