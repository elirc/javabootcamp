// ─────────────────────────────────────────────────────────────────────────
//  06 · tags in common                                      ★☆☆ warm-up
//  concepts: HashSet · LinkedHashSet · retainAll · set algebra
//  run: java -cp ../../_lib/out 06-sets.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A library tracker tags books. Write commonTags (tags on both books),
//  onlyInOne (tags on exactly one of the two) and uniqueInOrder (a list
//  with duplicates removed, keeping the FIRST time each item appeared).
//  The input collections must not change.
//
//      commonTags({java, cli}, {cli, web})          → {cli}
//      onlyInOne({java, cli}, {cli, web})           → {java, web}
//      uniqueInOrder([b, a, b, c, a])               → [b, a, c]
//
//  hint: retainAll and removeAll mutate the set you call them on.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static Set<String> commonTags(Set<String> a, Set<String> b) {
        throw new UnsupportedOperationException("TODO");
    }

    static Set<String> onlyInOne(Set<String> a, Set<String> b) {
        throw new UnsupportedOperationException("TODO");
    }

    static <T> List<T> uniqueInOrder(List<T> items) {
        throw new UnsupportedOperationException("TODO");
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
