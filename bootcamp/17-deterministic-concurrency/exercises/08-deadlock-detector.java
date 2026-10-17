// ─────────────────────────────────────────────────────────────────────────
//  08 · a lock-order deadlock detector                      ★★★ stretch
//  concepts: deadlock · lock ordering · cycle detection · topological sort
//  run: java -cp ../../_lib/out 08-deadlock-detector.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Find deadlocks without running a thread. Each thread's trace is a list
//  of ops: "+a" acquires lock a, "-a" releases it. Finish:
//    lockOrderGraph(traces)  an edge held → acquired for every lock held
//          when another is first acquired. Locks are reentrant: "+a" while
//          holding a adds no edge and needs one more "-a". Every lock seen
//          is a key (sorted map, sorted sets). Releasing a lock not held →
//          IllegalStateException "<thread> releases <lock> it does not
//          hold"; an op not shaped +name or -name → IllegalArgumentException
//    findCycle(graph)  a cycle as its locks, starting from its smallest
//          lock, without repeating it, or empty. Search from each lock in
//          sorted order, neighbours in sorted order; report the first found
//    safeOrder(graph)  every lock once, each edge's source before its
//          target, smallest available lock first; on a cycle,
//          IllegalStateException "deadlock possible: a -> b -> a"
//
//      T1 [+a, +b], T2 [+b, +a] → {a=[b], b=[a]} → findCycle → [a, b]
//
//  hint: a cycle in "acquired while holding" is a possible deadlock.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static Map<String, Set<String>> lockOrderGraph(Map<String, List<String>> traces) {
        throw new UnsupportedOperationException("TODO");
    }

    static Optional<List<String>> findCycle(Map<String, Set<String>> graph) {
        throw new UnsupportedOperationException("TODO");
    }

    static List<String> safeOrder(Map<String, Set<String>> graph) {
        throw new UnsupportedOperationException("TODO");
    }

    static Map<String, Set<String>> graph(String... edges) {
        var g = new TreeMap<String, Set<String>>();
        for (var e : edges) {
            var parts = e.split(">");
            g.computeIfAbsent(parts[0], k -> new TreeSet<>());
            if (parts.length > 1) {
                g.get(parts[0]).add(parts[1]);
                g.computeIfAbsent(parts[1], k -> new TreeSet<>());
            }
        }
        return g;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("nested acquisitions create held -> acquired edges", () ->
            eq(lockOrderGraph(Map.of("T1", List.of("+a", "+b", "+c", "-c", "-b", "-a"))),
                Map.of("a", Set.of("b", "c"), "b", Set.of("c"), "c", Set.of())));
        test("a released lock creates no edge", () ->
            eq(lockOrderGraph(Map.of("T1", List.of("+a", "-a", "+b", "-b"))), Map.of("a", Set.of(), "b", Set.of())));
        test("re-acquiring a held lock adds no edge and needs as many releases", () -> {
            eq(lockOrderGraph(Map.of("T1", List.of("+a", "+a", "-a", "+b", "-b", "-a"))),
                Map.of("a", Set.of("b"), "b", Set.of()));
            var e = raises(IllegalStateException.class, () -> lockOrderGraph(Map.of("T9", List.of("+a", "-a", "-a"))));
            eq(e.getMessage(), "T9 releases a it does not hold");
        });
        test("an op that is not + or - and a name is rejected", () -> {
            raises(IllegalArgumentException.class, () -> lockOrderGraph(Map.of("T1", List.of("a"))));
            raises(IllegalArgumentException.class, () -> lockOrderGraph(Map.of("T1", List.of("*a"))));
            raises(IllegalArgumentException.class, () -> lockOrderGraph(Map.of("T1", List.of("+"))));
        });
        test("two threads taking two locks in opposite orders form a cycle", () -> {
            var g = lockOrderGraph(Map.of("T1", List.of("+a", "+b", "-b", "-a"), "T2", List.of("+b", "+a", "-a", "-b")));
            eq(g, Map.of("a", Set.of("b"), "b", Set.of("a")));
            eq(findCycle(g), Optional.of(List.of("a", "b")));
        });
        test("findCycle starts the cycle at its smallest lock, or finds none", () -> {
            eq(findCycle(graph("a>d", "d>b", "b>c", "c>d")), Optional.of(List.of("b", "c", "d")));
            eq(findCycle(graph("x>y", "y>z", "x>z")), Optional.empty());
            eq(findCycle(graph("q>q")), Optional.of(List.of("q")));
        });
        test("safeOrder respects every edge, smallest available lock first", () -> {
            eq(safeOrder(graph("a>c", "b>c", "c>d", "e")), List.of("a", "b", "c", "d", "e"));
            eq(safeOrder(graph("z>a", "m")), List.of("m", "z", "a"));
        });
        test("safeOrder refuses a graph with a cycle and names it", () -> {
            var e = raises(IllegalStateException.class, () -> safeOrder(graph("b>a", "a>b", "c")));
            eq(e.getMessage(), "deadlock possible: a -> b -> a");
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
