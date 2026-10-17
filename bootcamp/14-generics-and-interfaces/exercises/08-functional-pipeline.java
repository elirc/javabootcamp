// ─────────────────────────────────────────────────────────────────────────
//  08 · a typed functional pipeline                         ★★★ stretch
//  concepts: Function composition · Optional · immutability · wildcards
//  run: java -cp ../../_lib/out 08-functional-pipeline.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An import job cleans each raw line through a chain of steps. Build
//  Pipeline<I, O>: input type I, current output type O. It wraps a
//  Function<I, Optional<O>>; an empty Optional means "dropped". Finish:
//
//    start()        a Pipeline<T, T> passing input through; null → empty
//    map(f)         transform; if f returns null the item is dropped
//    filter(p)      drop items p rejects
//    flatMap(f)     f returns an Optional; empty drops the item
//    peek(action)   run action on each item that REACHES this step
//    then(next)     run another pipeline on what survives this one
//    run(input)     the Optional result for one input
//    runAll(inputs) the survivors' results, in input order
//
//  A dropped item never reaches later steps, and every step returns a NEW
//  pipeline: the one you called it on is unchanged.
//
//      start().map(String::trim).map(Integer::parseInt).run(" 42 ") → 42
//
//  hint: Optional already has map, filter and flatMap with these rules.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    static final class Pipeline<I, O> {
        private final Function<I, Optional<O>> steps;

        private Pipeline(Function<I, Optional<O>> steps) {
            this.steps = steps;
        }

        static <T> Pipeline<T, T> start() {
            throw new UnsupportedOperationException("TODO");
        }

        <R> Pipeline<I, R> map(Function<? super O, ? extends R> f) {
            throw new UnsupportedOperationException("TODO");
        }

        Pipeline<I, O> filter(Predicate<? super O> p) {
            throw new UnsupportedOperationException("TODO");
        }

        <R> Pipeline<I, R> flatMap(Function<? super O, Optional<? extends R>> f) {
            throw new UnsupportedOperationException("TODO");
        }

        Pipeline<I, O> peek(Consumer<? super O> action) {
            throw new UnsupportedOperationException("TODO");
        }

        <R> Pipeline<I, R> then(Pipeline<? super O, ? extends R> next) {
            throw new UnsupportedOperationException("TODO");
        }

        Optional<O> run(I input) {
            throw new UnsupportedOperationException("TODO");
        }

        List<O> runAll(Collection<? extends I> inputs) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static Optional<Integer> parseInt(String s) {
        return s.matches("-?\\d+") ? Optional.of(Integer.parseInt(s)) : Optional.empty();
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("start passes its input through, and a null input gives empty", () -> {
            eq(Pipeline.<String>start().run("x"), Optional.of("x"));
            eq(Pipeline.<String>start().run(null), Optional.empty());
        });
        test("map steps change the type one step at a time", () -> {
            Pipeline<String, Integer> doubled = Pipeline.<String>start()
                .map(String::trim).map(Integer::parseInt).map(n -> n * 2);
            eq(doubled.run(" 21 "), Optional.of(42));
        });
        test("filter drops items and later steps never see them", () -> {
            var seen = new ArrayList<Integer>();
            var positives = Pipeline.<Integer>start().filter(n -> n > 0).peek(seen::add).map(n -> 100 / n);
            eq(positives.run(0), Optional.empty());
            eq(positives.run(-4), Optional.empty());
            eq(positives.run(4), Optional.of(25));
            eq(seen, List.of(4));
        });
        test("a map that returns null drops the item", () -> {
            var prices = Map.of("A1", 250, "B2", 99);
            var lookup = Pipeline.<String>start().map(prices::get).map(cents -> cents / 100.0);
            eq(lookup.run("B2"), Optional.of(0.99));
            eq(lookup.run("ZZ"), Optional.empty());
        });
        test("flatMap drops items whose function returns empty", () -> {
            var parse = Pipeline.<String>start().map(String::strip).flatMap(Exercise::parseInt);
            eq(parse.run(" 17 "), Optional.of(17));
            eq(parse.run("17a"), Optional.empty());
        });
        test("every step returns a new pipeline, so branches share a prefix safely", () -> {
            var base = Pipeline.<String>start().map(String::strip);
            var upper = base.map(s -> s.toUpperCase(Locale.ROOT));
            var length = base.map(String::length);
            eq(base.run(" ab "), Optional.of("ab"));
            eq(upper.run(" ab "), Optional.of("AB"));
            eq(length.run(" ab "), Optional.of(2));
        });
        test("then runs a second pipeline on what survives the first", () -> {
            Pipeline<Object, String> describe = Pipeline.<Object>start()
                .filter(o -> !o.toString().isEmpty()).map(o -> "<" + o + ">");
            var parseThenDescribe = Pipeline.<String>start().flatMap(Exercise::parseInt).then(describe);
            eq(parseThenDescribe.run("7"), Optional.of("<7>"));
            eq(parseThenDescribe.run("x"), Optional.empty());
        });
        test("runAll keeps the survivors in input order", () -> {
            var quantities = Pipeline.<String>start().flatMap(Exercise::parseInt).filter(n -> n > 0);
            eq(quantities.runAll(List.of("3", "x", "10", "0", "-2", "1")), List.of(3, 10, 1));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
