// ─────────────────────────────────────────────────────────────────────────
//  04 · a Result type for errors — SOLUTION                 ★★☆ core
//  concepts: sealed generics · Result/Either · map/flatMap · error values
//  run: java -cp ../../_lib/out 04-result-type.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Exceptions are for the unexpected; a bad line in an import file is
//  expected, and the caller wants to collect or report it. A Result type
//  makes the failure part of the return type, so it cannot be ignored by
//  accident. Java has no built-in one; Rust's Result, Scala's Either and
//  Vavr's Try are the same idea.
//  Result<T> is sealed with two record cases, and its operations are
//  default methods that dispatch with `this instanceof Ok<T> ok`. Java 17
//  allows the generic pattern because the check from Result<T> to Ok<T>
//  is safe. map and flatMap leave an Err untouched and never call f: that
//  short-circuit is what lets a chain of steps stop at the first failure
//  without any if-statements.
//  sequence turns a List<Result<T>> into a Result<List<T>>, returning on
//  the first Err. Keeping the last error instead reports the wrong line.
//  The parse trap: "AB,".split(",") gives ["AB"], one part, so the error
//  would be the wrong one; split(",", -1) keeps the empty quantity and
//  reports "bad quantity: ". The line number is added with mapError per
//  line, before sequencing, so the message knows which line it came from.

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
            try {
                return new Ok<>(work.get());
            } catch (RuntimeException e) {
                return new Err<>(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }
        }

        static <T> Result<List<T>> sequence(List<Result<T>> results) {
            var values = new ArrayList<T>();
            for (var r : results) {
                if (r instanceof Err<T> err) return new Err<>(err.error());
                if (r instanceof Ok<T> ok) values.add(ok.value());
            }
            return new Ok<>(values);
        }

        default <R> Result<R> map(Function<? super T, ? extends R> f) {
            if (this instanceof Ok<T> ok) return new Ok<>(f.apply(ok.value()));
            if (this instanceof Err<T> err) return new Err<>(err.error());
            throw new AssertionError(this);
        }

        default <R> Result<R> flatMap(Function<? super T, Result<R>> f) {
            if (this instanceof Ok<T> ok) return f.apply(ok.value());
            if (this instanceof Err<T> err) return new Err<>(err.error());
            throw new AssertionError(this);
        }

        default Result<T> mapError(UnaryOperator<String> f) {
            if (this instanceof Err<T> err) return new Err<>(f.apply(err.error()));
            return this;
        }

        default T orElse(T fallback) {
            return this instanceof Ok<T> ok ? ok.value() : fallback;
        }
    }

    record Ok<T>(T value) implements Result<T> {}

    record Err<T>(String error) implements Result<T> {}

    record OrderLine(String sku, int qty) {}

    static Result<OrderLine> parseLine(String line) {
        var parts = line.split(",", -1);
        if (parts.length != 2) return Result.err("expected sku,qty: " + line);
        var sku = parts[0].strip();
        var qtyText = parts[1].strip();
        if (sku.isEmpty()) return Result.err("missing sku");
        return Result.<Integer>attempt(() -> Integer.parseInt(qtyText))
            .mapError(e -> "bad quantity: " + qtyText)
            .flatMap(q -> q > 0 ? Result.ok(new OrderLine(sku, q)) : Result.err("quantity must be positive: " + q));
    }

    static Result<List<OrderLine>> parseOrder(List<String> lines) {
        var parsed = new ArrayList<Result<OrderLine>>();
        for (int i = 0; i < lines.size(); i++) {
            int lineNo = i + 1;
            parsed.add(parseLine(lines.get(i)).mapError(e -> "line " + lineNo + ": " + e));
        }
        return Result.sequence(parsed);
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
