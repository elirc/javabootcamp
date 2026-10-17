// ─────────────────────────────────────────────────────────────────────────
//  02 · PECS wildcards — SOLUTION                           ★☆☆ warm-up
//  concepts: wildcards · PECS · ? extends · ? super · wildcard capture
//  run: java -cp ../../_lib/out 02-wildcards-pecs.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  PECS (Bloch, Effective Java item 31): a parameter you only READ from
//  is a producer, so it is "? extends T"; one you only WRITE into is a
//  consumer, so it is "? super T". That is why copyAll can take a
//  List<Integer> as src and a List<Number> or List<Object> as dst, and
//  why Collections.copy has exactly this signature.
//  copyAll returns the number of adds that returned true. Returning
//  src.size() is the classic wrong turn: a Set refuses duplicates and
//  the count lies.
//  sum reads through Number.doubleValue(), the one method every Number
//  has; it works for Integer, Long, Double and BigDecimal alike.
//  swapEnds shows WILDCARD CAPTURE. Through a List<?> you can read
//  Objects but never set anything (the element type is unknown). A
//  private generic helper swap(List<T>) "captures" the unknown type as T,
//  and then get/set type-check. The size guard is not just tidiness: it
//  means an immutable List.of(7) is never written to, so it never throws.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.*;

class Exercise {

    static <T> int copyAll(Collection<? super T> dst, Collection<? extends T> src) {
        int accepted = 0;
        for (T item : src) {
            if (dst.add(item)) accepted++;
        }
        return accepted;
    }

    static double sum(Collection<? extends Number> numbers) {
        double total = 0.0;
        for (Number n : numbers) total += n.doubleValue();
        return total;
    }

    static void addRange(List<? super Integer> sink, int from, int toExclusive) {
        if (toExclusive < from) throw new IllegalArgumentException("range " + from + ".." + toExclusive + " is backwards");
        for (int i = from; i < toExclusive; i++) sink.add(i);
    }

    static <T> int copyMatching(Collection<? extends T> src, Predicate<? super T> keep, Collection<? super T> dst) {
        int accepted = 0;
        for (T item : src) {
            if (keep.test(item) && dst.add(item)) accepted++;
        }
        return accepted;
    }

    static void swapEnds(List<?> list) {
        swap(list);
    }

    private static <T> void swap(List<T> list) {
        if (list.size() < 2) return;
        int last = list.size() - 1;
        T first = list.get(0);
        list.set(0, list.get(last));
        list.set(last, first);
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
