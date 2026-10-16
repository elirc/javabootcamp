// ─────────────────────────────────────────────────────────────────────────
//  06 · tags in common — SOLUTION                           ★☆☆ warm-up
//  concepts: HashSet · LinkedHashSet · retainAll · set algebra
//  run: java -cp ../../_lib/out 06-sets.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Sets do the algebra for you — retainAll is intersection, addAll is
//  union, removeAll is difference — but every one of them MUTATES the set
//  it is called on. `a.retainAll(b); return a;` returns the right answer
//  and quietly deletes tags from the caller's book. Copy first:
//  `var both = new HashSet<>(a); both.retainAll(b);`.
//  onlyInOne (the symmetric difference) is union minus intersection, or
//  (a - b) + (b - a). Both are two copies and a couple of bulk calls.
//  uniqueInOrder is the LinkedHashSet trick: a set that remembers
//  insertion order, so `new ArrayList<>(new LinkedHashSet<>(items))`
//  dedupes and keeps first-seen order in one line. A plain HashSet would
//  dedupe and scramble; a TreeSet would dedupe and sort — neither is what
//  was asked. stream().distinct() is the stream spelling of the same
//  thing (it also keeps encounter order).
//  Set membership uses equals and hashCode, which is why records and
//  Strings work as set elements out of the box — and why a class that
//  overrides only equals breaks them (module 05).

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static Set<String> commonTags(Set<String> a, Set<String> b) {
        var both = new HashSet<>(a);
        both.retainAll(b);
        return both;
    }

    static Set<String> onlyInOne(Set<String> a, Set<String> b) {
        var all = new HashSet<>(a);
        all.addAll(b);
        all.removeAll(commonTags(a, b));
        return all;
    }

    static <T> List<T> uniqueInOrder(List<T> items) {
        return new ArrayList<>(new LinkedHashSet<>(items));
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("commonTags finds the overlap", () ->
            eq(commonTags(Set.of("java", "cli"), Set.of("cli", "web")), Set.of("cli")));
        test("commonTags leaves both inputs alone", () -> {
            var a = new HashSet<>(Set.of("java", "cli"));
            var b = new HashSet<>(Set.of("cli", "web"));
            commonTags(a, b);
            eq(a, Set.of("java", "cli"));
            eq(b, Set.of("cli", "web"));
        });
        test("no overlap is an empty set", () -> eq(commonTags(Set.of("a"), Set.of("b")), Set.of()));
        test("onlyInOne is the symmetric difference", () ->
            eq(onlyInOne(Set.of("java", "cli"), Set.of("cli", "web")), Set.of("java", "web")));
        test("onlyInOne leaves its inputs alone", () -> {
            var a = new HashSet<>(Set.of("x", "y"));
            onlyInOne(a, Set.of("y"));
            eq(a, Set.of("x", "y"));
        });
        test("uniqueInOrder keeps first appearances", () ->
            eq(uniqueInOrder(List.of("b", "a", "b", "c", "a")), List.of("b", "a", "c")));
        test("uniqueInOrder of an empty list is empty", () -> eq(uniqueInOrder(List.of()), List.of()));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
