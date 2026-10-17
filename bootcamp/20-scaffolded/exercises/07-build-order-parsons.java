// ─────────────────────────────────────────────────────────────────────────
//  07 · a build order (Parsons)                             ★★★ stretch
//  concepts: Parsons · topological sort · Kahn's algorithm · PriorityQueue
//  run: java -cp ../../_lib/out 07-build-order-parsons.java
// ─────────────────────────────────────────────────────────────────────────
//
//  PARSONS: arrange the SCRAMBLED lines into the body of buildOrder. The
//  pair marked (?): exactly ONE belongs. The helpers are written for you.
//  deps maps a task to the tasks it needs first. buildOrder(deps) lists
//  EVERY task (keys and values) so each comes after all it needs; among
//  tasks that are ready at the same time, the alphabetically smallest
//  goes first. A cycle: IllegalStateException "cycle".
//
//      }
//      var unlocks = unlocks(deps);
//      if (order.size() < waiting.size()) {
//      for (var next : unlocks.getOrDefault(t, List.of())) {
//      var ready = new ArrayDeque<String>();                    (?)
//      order.add(t);
//      var order = new ArrayList<String>();
//      throw new IllegalStateException("cycle");
//      return order;
//      var waiting = waitingCounts(deps);
//      if (waiting.merge(next, -1, Integer::sum) == 0) ready.add(next);
//      while (!ready.isEmpty()) {
//      }
//      }
//      var t = ready.poll();
//      var ready = new PriorityQueue<String>();                 (?)
//      waiting.forEach((task, n) -> { if (n == 0) ready.add(task); });
//
//      buildOrder({app: [lib, util], lib: [util], test: [app]})
//                                         → [util, lib, app, test]
//
//  hint: a task is ready when nothing it waits for is left unbuilt.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    /** every task (keys and values), with how many tasks it waits for */
    static Map<String, Integer> waitingCounts(Map<String, List<String>> deps) {
        var waiting = new TreeMap<String, Integer>();
        deps.forEach((task, needs) -> {
            waiting.merge(task, needs.size(), Integer::sum);
            needs.forEach(n -> waiting.putIfAbsent(n, 0));
        });
        return waiting;
    }

    /** for each task, the tasks that wait for it */
    static Map<String, List<String>> unlocks(Map<String, List<String>> deps) {
        var unlocks = new HashMap<String, List<String>>();
        deps.forEach((task, needs) -> needs.forEach(n -> unlocks.computeIfAbsent(n, k -> new ArrayList<>()).add(task)));
        return unlocks;
    }

    static List<String> buildOrder(Map<String, List<String>> deps) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("puts a chain in dependency order", () -> {
            eq(buildOrder(Map.of("c", List.of("b"), "b", List.of("a"))), List.of("a", "b", "c"));
        });
        test("orders a small project", () -> {
            eq(buildOrder(Map.of("app", List.of("lib", "util"), "lib", List.of("util"), "test", List.of("app"))),
                List.of("util", "lib", "app", "test"));
        });
        test("among ready tasks the alphabetically smallest goes first", () -> {
            eq(buildOrder(Map.of("b", List.of("a"), "z", List.of())), List.of("a", "b", "z"));
            eq(buildOrder(Map.of("m", List.of(), "k", List.of(), "d", List.of("m", "k"))), List.of("k", "m", "d"));
        });
        test("includes tasks that appear only as dependencies", () -> {
            eq(buildOrder(Map.of("x", List.of("y", "w"))), List.of("w", "y", "x"));
        });
        test("a cycle is refused", () -> {
            raises(IllegalStateException.class, () -> buildOrder(Map.of("a", List.of("b"), "b", List.of("a"))), "cycle");
            raises(IllegalStateException.class, () -> buildOrder(Map.of("a", List.of("a"))), "cycle");
            raises(IllegalStateException.class,
                () -> buildOrder(Map.of("ok", List.of(), "p", List.of("q"), "q", List.of("r"), "r", List.of("p"))), "cycle");
        });
        test("no tasks, empty order", () -> {
            eq(buildOrder(Map.of()), List.of());
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
