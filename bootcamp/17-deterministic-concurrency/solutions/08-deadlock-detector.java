// ─────────────────────────────────────────────────────────────────────────
//  08 · a lock-order deadlock detector — SOLUTION           ★★★ stretch
//  concepts: deadlock · lock ordering · cycle detection · topological sort
//  run: java -cp ../../_lib/out 08-deadlock-detector.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A deadlock needs a cycle: T1 holds a and waits for b while T2 holds b
//  and waits for a. It depends on timing, so tests that run threads find
//  it by luck, and the JVM's ThreadMXBean.findDeadlockedThreads() only
//  sees one that has already happened. Lock-order checkers (Linux's
//  lockdep) use a GRAPH instead: "b acquired while holding a" is an edge
//  a -> b, and a cycle means some interleaving CAN deadlock.
//  The graph rules carry the subtlety. Only locks still HELD create
//  edges; a released lock no longer orders anything. Reentrant
//  re-acquisition must not add a self-edge (a -> a would be a fake
//  deadlock) and must be counted, so the lock stays held until the last
//  release. A release of a lock not held is a bug in the trace, reported
//  with the thread's name.
//  findCycle is depth-first search with an "on the current path" set: an
//  edge back into the path closes a cycle. Normalising it to start at its
//  smallest lock makes the answer stable wherever the search entered.
//  safeOrder is the cure: a topological sort (Kahn's algorithm, with a
//  TreeSet or PriorityQueue for the alphabetical tie-break). If every
//  thread takes locks in that one global order, no cycle can form; that
//  is why "always lock accounts by ascending id" fixes transfer deadlocks.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static Map<String, Set<String>> lockOrderGraph(Map<String, List<String>> traces) {
        var graph = new TreeMap<String, Set<String>>();
        for (var entry : new TreeMap<>(traces).entrySet()) {
            var thread = entry.getKey();
            var held = new LinkedHashMap<String, Integer>();
            for (var op : entry.getValue()) {
                if (!op.matches("[+-][A-Za-z0-9_]+")) throw new IllegalArgumentException("bad op: " + op);
                var lock = op.substring(1);
                graph.computeIfAbsent(lock, k -> new TreeSet<>());
                if (op.charAt(0) == '+') {
                    if (held.containsKey(lock)) {
                        held.merge(lock, 1, Integer::sum);
                    } else {
                        for (var h : held.keySet()) graph.get(h).add(lock);
                        held.put(lock, 1);
                    }
                } else {
                    Integer count = held.get(lock);
                    if (count == null) throw new IllegalStateException(thread + " releases " + lock + " it does not hold");
                    if (count == 1) held.remove(lock);
                    else held.put(lock, count - 1);
                }
            }
        }
        return graph;
    }

    static Optional<List<String>> findCycle(Map<String, Set<String>> graph) {
        var done = new HashSet<String>();
        for (var start : new TreeSet<>(graph.keySet())) {
            var path = new ArrayList<String>();
            var cycle = dfs(start, graph, path, new HashSet<>(), done);
            if (cycle.isPresent()) return cycle;
        }
        return Optional.empty();
    }

    private static Optional<List<String>> dfs(String node, Map<String, Set<String>> graph,
                                              List<String> path, Set<String> onPath, Set<String> done) {
        if (onPath.contains(node)) {
            var cycle = new ArrayList<>(path.subList(path.indexOf(node), path.size()));
            int smallest = cycle.indexOf(Collections.min(cycle));
            Collections.rotate(cycle, -smallest);
            return Optional.of(cycle);
        }
        if (done.contains(node)) return Optional.empty();
        path.add(node);
        onPath.add(node);
        for (var next : new TreeSet<>(graph.getOrDefault(node, Set.of()))) {
            var found = dfs(next, graph, path, onPath, done);
            if (found.isPresent()) return found;
        }
        path.remove(path.size() - 1);
        onPath.remove(node);
        done.add(node);
        return Optional.empty();
    }

    static List<String> safeOrder(Map<String, Set<String>> graph) {
        var cycle = findCycle(graph);
        if (cycle.isPresent()) {
            var names = new ArrayList<>(cycle.get());
            names.add(names.get(0));
            throw new IllegalStateException("deadlock possible: " + String.join(" -> ", names));
        }
        var indegree = new TreeMap<String, Integer>();
        for (var node : graph.keySet()) indegree.putIfAbsent(node, 0);
        for (var targets : graph.values()) for (var t : targets) indegree.merge(t, 1, Integer::sum);
        var ready = new TreeSet<String>();
        indegree.forEach((node, n) -> { if (n == 0) ready.add(node); });
        var order = new ArrayList<String>();
        while (!ready.isEmpty()) {
            var node = ready.pollFirst();
            order.add(node);
            for (var t : graph.getOrDefault(node, Set.of())) {
                if (indegree.merge(t, -1, Integer::sum) == 0) ready.add(t);
            }
        }
        return order;
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
