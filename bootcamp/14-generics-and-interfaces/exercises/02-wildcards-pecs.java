// ─────────────────────────────────────────────────────────────────────────
//  02 · PECS wildcards                                      ★☆☆ warm-up
//  concepts: wildcards · PECS · ? extends · ? super · wildcard capture
//  run: java -cp ../../_lib/out 02-wildcards-pecs.java
// ─────────────────────────────────────────────────────────────────────────
//
//  "Producer extends, consumer super": read from a Collection<? extends T>,
//  write into a Collection<? super T>. Finish five helpers:
//
//    copyAll(dst, src)       add every src item to dst in order; return how
//                            many adds dst accepted (a Set may refuse some)
//    sum(numbers)            the total of any Numbers as a double; 0.0 if
//                            empty
//    addRange(sink, a, b)    add the Integers a, a+1, ... b-1 to sink;
//                            b < a throws IllegalArgumentException
//    copyMatching(src, keep, dst)  like copyAll, only items keep accepts
//    swapEnds(list)          swap the first and last item of a List<?>;
//                            a list with under 2 items is not touched
//
//      copyAll(List<Number> [1.5], List.of(1, 2))  → 2, dst [1.5, 1, 2]
//      addRange(sink, 3, 6)                        → adds 3, 4, 5
//
//  hint: you cannot set into a List<?>, but a generic helper can.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.*;

class Exercise {

    static <T> int copyAll(Collection<? super T> dst, Collection<? extends T> src) {
        throw new UnsupportedOperationException("TODO");
    }

    static double sum(Collection<? extends Number> numbers) {
        throw new UnsupportedOperationException("TODO");
    }

    static void addRange(List<? super Integer> sink, int from, int toExclusive) {
        throw new UnsupportedOperationException("TODO");
    }

    static <T> int copyMatching(Collection<? extends T> src, Predicate<? super T> keep, Collection<? super T> dst) {
        throw new UnsupportedOperationException("TODO");
    }

    static void swapEnds(List<?> list) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("copyAll copies Integers into a List<Number> and returns the count", () -> {
            List<Number> dst = new ArrayList<>(List.of(1.5));
            eq(copyAll(dst, List.of(1, 2)), 2);
            eq(dst, List.of(1.5, 1, 2));
        });
        test("copyAll into a Set counts only the items it accepted", () -> {
            Set<Object> dst = new HashSet<>(Set.of("a"));
            eq(copyAll(dst, List.of("a", "b", "b")), 1);
            eq(dst, Set.of("a", "b"));
        });
        test("sum adds any mix of Number subtypes, and 0.0 when empty", () -> {
            approx(sum(List.of(1, 2.5, 3L)), 6.5);
            approx(sum(List.of(new BigDecimal("0.25"), new BigDecimal("0.50"))), 0.75);
            approx(sum(List.<Integer>of()), 0.0);
        });
        test("addRange fills a List<Object>, from inclusive, to exclusive", () -> {
            List<Object> sink = new ArrayList<>(List.of("start"));
            addRange(sink, 3, 6);
            eq(sink, List.of("start", 3, 4, 5));
            addRange(sink, 2, 2);
            eq(sink.size(), 4);
        });
        test("addRange rejects a backwards range and adds nothing", () -> {
            List<Number> sink = new ArrayList<>();
            raises(IllegalArgumentException.class, () -> addRange(sink, 5, 4));
            eq(sink, List.of());
        });
        test("copyMatching takes a Predicate<Object> and keeps source order", () -> {
            Predicate<Object> longName = o -> o.toString().length() > 1;
            List<CharSequence> dst = new ArrayList<>();
            List<String> src = List.of("a", "bb", "c", "ddd");
            eq(copyMatching(src, longName, dst), 2);
            eq(dst, List.of("bb", "ddd"));
            eq(src, List.of("a", "bb", "c", "ddd"));
        });
        test("swapEnds swaps the first and last item of any List<?>", () -> {
            var words = new ArrayList<>(List.of("a", "b", "c"));
            swapEnds(words);
            eq(words, List.of("c", "b", "a"));
            var pair = new ArrayList<>(List.of(1, 2));
            swapEnds(pair);
            eq(pair, List.of(2, 1));
        });
        test("swapEnds never writes to a list with under two items", () -> {
            swapEnds(List.of());
            swapEnds(List.of(7));
            var one = new ArrayList<>(List.of("solo"));
            swapEnds(one);
            eq(one, List.of("solo"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
