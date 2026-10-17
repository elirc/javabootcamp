// ─────────────────────────────────────────────────────────────────────────
//  03 · generic methods and inference                       ★★☆ core
//  concepts: generic methods · type inference · type witness · laziness
//  run: java -cp ../../_lib/out 03-generic-methods.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A small generic toolkit for reports. Finish four methods:
//
//    invert(map)        value → keys with that value; the result's values
//                       appear in first-seen order, each key list in the
//                       map's iteration order
//    zip(as, bs)        Pairs of same-index items, stopping at the shorter
//    indexBy(items, f)  key → item in encounter order; two items with the
//                       same key throw IllegalStateException whose message
//                       contains the key
//    firstMatch(items, p)  the first item p accepts, or empty; it must
//                       stop pulling items from the Iterable once found
//
//      invert({ana=red, bob=blue, cy=red})  → {red=[ana, cy], blue=[bob]}
//      zip([1, 2, 3], [a, b])               → [Pair(1, a), Pair(2, b)]
//      Exercise.<Number, Number>zip(...)    → explicit type arguments
//
//  hint: of HashMap, LinkedHashMap and TreeMap, one keeps insertion order.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

class Exercise {

    record Pair<A, B>(A first, B second) {}

    record Sku(String code, String name) {}

    static <K, V> Map<V, List<K>> invert(Map<K, V> map) {
        throw new UnsupportedOperationException("TODO");
    }

    static <A, B> List<Pair<A, B>> zip(List<? extends A> as, List<? extends B> bs) {
        throw new UnsupportedOperationException("TODO");
    }

    static <T, K> Map<K, T> indexBy(Collection<? extends T> items, Function<? super T, ? extends K> key) {
        throw new UnsupportedOperationException("TODO");
    }

    static <T> Optional<T> firstMatch(Iterable<? extends T> items, Predicate<? super T> p) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("invert groups the keys that share a value", () -> {
            var colours = new LinkedHashMap<String, String>();
            colours.put("ana", "red");
            colours.put("bob", "blue");
            colours.put("cy", "red");
            var inverted = invert(colours);
            eq(inverted, Map.of("red", List.of("ana", "cy"), "blue", List.of("bob")));
            eq(new ArrayList<>(inverted.keySet()), List.of("red", "blue"));
        });
        test("invert keeps values in first-seen order, not sorted", () -> {
            var stock = new LinkedHashMap<String, Integer>();
            stock.put("x", 30);
            stock.put("y", 4);
            stock.put("z", 30);
            stock.put("w", 17);
            eq(new ArrayList<>(invert(stock).keySet()), List.of(30, 4, 17));
            eq(invert(Map.of()), Map.of());
        });
        test("zip pairs items up and stops at the shorter list", () -> {
            eq(zip(List.of(1, 2, 3), List.of("a", "b")), List.of(new Pair<>(1, "a"), new Pair<>(2, "b")));
            eq(zip(List.of(), List.of("a")), List.of());
        });
        test("zip with a type witness pairs Integers and Doubles as Numbers", () -> {
            List<Pair<Number, Number>> pairs = Exercise.<Number, Number>zip(List.of(1, 2), List.of(2.5, 0.5));
            eq(pairs, List.of(new Pair<Number, Number>(1, 2.5), new Pair<Number, Number>(2, 0.5)));
        });
        test("indexBy keys items by the function, in encounter order", () -> {
            var skus = List.of(new Sku("Q7", "quill"), new Sku("A1", "anchor"), new Sku("M3", "mallet"));
            var byCode = indexBy(skus, Sku::code);
            eq(byCode.get("A1").name(), "anchor");
            eq(new ArrayList<>(byCode.keySet()), List.of("Q7", "A1", "M3"));
        });
        test("indexBy throws IllegalStateException naming a duplicate key", () -> {
            var skus = List.of(new Sku("A1", "anchor"), new Sku("Q7", "quill"), new Sku("A1", "awl"));
            var e = raises(IllegalStateException.class, () -> indexBy(skus, Sku::code));
            ok(e.getMessage().contains("A1"), "message names the key: " + e.getMessage());
        });
        test("firstMatch returns the first accepted item, or empty", () -> {
            eq(firstMatch(List.of(3, 8, 12), n -> n % 2 == 0), Optional.of(8));
            eq(firstMatch(List.of(3, 5), n -> n % 2 == 0), Optional.empty());
            Predicate<Object> isText = o -> o instanceof String;
            eq(firstMatch(List.<Object>of(1, "two", "three"), isText), Optional.of("two"));
        });
        test("firstMatch stops pulling items once it finds a match", () -> {
            var pulled = new ArrayList<Integer>();
            Iterable<Integer> source = () -> Stream.of(3, 8, 12, 15).peek(pulled::add).iterator();
            eq(firstMatch(source, n -> n % 2 == 0), Optional.of(8));
            eq(pulled, List.of(3, 8));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
