// ─────────────────────────────────────────────────────────────────────────
//  03 · generic methods and inference — SOLUTION            ★★☆ core
//  concepts: generic methods · type inference · type witness · laziness
//  run: java -cp ../../_lib/out 03-generic-methods.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A generic METHOD declares its own type parameters before the return
//  type (static <K, V> Map<V, List<K>> invert(...)), so each call can
//  bind them freely; the compiler infers them from the arguments and the
//  target type. When inference picks something you did not want, a TYPE
//  WITNESS states it: Exercise.<Number, Number>zip(ints, doubles). The
//  parameters are "? extends A" so a List<Integer> can be an A = Number.
//  invert and indexBy build LinkedHashMaps: a HashMap would scramble the
//  order of the report, and a TreeMap would sort it, which is a different
//  promise. computeIfAbsent(v, k -> new ArrayList<>()).add(key) is the
//  idiom for "group into lists".
//  indexBy refuses duplicates loudly. Map.put would silently keep the last
//  item, and Collectors.toMap throws only with a message you do not
//  control; checking put's return value gives a precise error.
//  firstMatch walks the iterator and returns on the first hit. Copying the
//  Iterable into a list first, or streaming it all and filtering, pulls
//  every item: on a database cursor or an infinite source that is a bug,
//  not a slowdown.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

class Exercise {

    record Pair<A, B>(A first, B second) {}

    record Sku(String code, String name) {}

    static <K, V> Map<V, List<K>> invert(Map<K, V> map) {
        var out = new LinkedHashMap<V, List<K>>();
        map.forEach((k, v) -> out.computeIfAbsent(v, x -> new ArrayList<>()).add(k));
        return out;
    }

    static <A, B> List<Pair<A, B>> zip(List<? extends A> as, List<? extends B> bs) {
        int n = Math.min(as.size(), bs.size());
        var out = new ArrayList<Pair<A, B>>(n);
        for (int i = 0; i < n; i++) out.add(new Pair<>(as.get(i), bs.get(i)));
        return out;
    }

    static <T, K> Map<K, T> indexBy(Collection<? extends T> items, Function<? super T, ? extends K> key) {
        var out = new LinkedHashMap<K, T>();
        for (T item : items) {
            K k = key.apply(item);
            if (out.containsKey(k)) throw new IllegalStateException("duplicate key: " + k);
            out.put(k, item);
        }
        return out;
    }

    static <T> Optional<T> firstMatch(Iterable<? extends T> items, Predicate<? super T> p) {
        for (T item : items) {
            if (p.test(item)) return Optional.of(item);
        }
        return Optional.empty();
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
            var skus = List.of(new Sku("B2", "bolt"), new Sku("A1", "anchor"), new Sku("C3", "cable"));
            var byCode = indexBy(skus, Sku::code);
            eq(byCode.get("A1").name(), "anchor");
            eq(new ArrayList<>(byCode.keySet()), List.of("B2", "A1", "C3"));
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
