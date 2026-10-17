// ─────────────────────────────────────────────────────────────────────────
//  08 · a typed functional pipeline — SOLUTION              ★★★ stretch
//  concepts: Function composition · Optional · immutability · wildcards
//  run: java -cp ../../_lib/out 08-functional-pipeline.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  The pipeline is a function I -> Optional<O>, and each step returns a
//  new Pipeline wrapping a bigger function: map(f) is
//  in -> run.apply(in).map(f). Nothing runs until run(...) is called;
//  building the chain only composes functions. Because the field is final
//  and every step builds a new object, a shared prefix can be branched
//  safely: base.map(a) and base.map(b) never see each other.
//  Optional carries the "dropped" rule for free. Optional.map turns a null
//  result into empty, filter empties on a rejection, and once a value is
//  empty, map/filter/flatMap/ifPresent skip their function. That is why a
//  peek after a filter never sees dropped items, with no if-statement
//  anywhere. Optional.of(f.apply(x)) in map is the classic slip: it
//  throws NullPointerException instead of dropping.
//  The wildcards are what make the steps reusable: map takes a
//  Function<? super O, ? extends R>, so a Function<Object, String> or a
//  method returning a subtype both fit. then(Pipeline<? super O, ?
//  extends R>) lets a validation pipeline written for a supertype run
//  after this one.

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
            return new Pipeline<T, T>(Optional::ofNullable);
        }

        <R> Pipeline<I, R> map(Function<? super O, ? extends R> f) {
            return new Pipeline<I, R>(in -> steps.apply(in).map(f));
        }

        Pipeline<I, O> filter(Predicate<? super O> p) {
            return new Pipeline<>(in -> steps.apply(in).filter(p));
        }

        <R> Pipeline<I, R> flatMap(Function<? super O, Optional<? extends R>> f) {
            return new Pipeline<I, R>(in -> steps.apply(in).flatMap(f));
        }

        Pipeline<I, O> peek(Consumer<? super O> action) {
            return new Pipeline<>(in -> {
                var out = steps.apply(in);
                out.ifPresent(action);
                return out;
            });
        }

        <R> Pipeline<I, R> then(Pipeline<? super O, ? extends R> next) {
            return new Pipeline<I, R>(in -> steps.apply(in).flatMap(next::run));
        }

        Optional<O> run(I input) {
            return steps.apply(input);
        }

        List<O> runAll(Collection<? extends I> inputs) {
            var out = new ArrayList<O>();
            for (I in : inputs) run(in).ifPresent(out::add);
            return out;
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
