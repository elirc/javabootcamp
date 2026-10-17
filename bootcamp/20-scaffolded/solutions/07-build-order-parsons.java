// ─────────────────────────────────────────────────────────────────────────
//  07 · a build order (Parsons) — SOLUTION                  ★★★ stretch
//  concepts: Parsons · topological sort · Kahn's algorithm · PriorityQueue
//  run: java -cp ../../_lib/out 07-build-order-parsons.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Kahn's algorithm: count, for every task, how many tasks it still
//  waits for. Tasks at 0 are ready. Repeatedly take a ready task, emit
//  it, and tell everything it unlocks that one fewer dependency is
//  left; whatever drops to 0 becomes ready. This is how Maven, Gradle,
//  make and package managers order a build.
//  The pair is the tie-break. A PriorityQueue always polls the smallest
//  ready task; an ArrayDeque polls in the order tasks BECAME ready, so
//  {b: [a], z: []} would give a, z, b instead of a, b, z. Without a rule
//  like this the order depends on HashMap iteration and changes between
//  runs, which turns into "flaky build" tickets.
//  Cycle detection is free: in a cycle every task waits for another one
//  in it, so none ever reaches 0. The loop ends with tasks unemitted,
//  and order.size() < waiting.size() says so. A self-dependency is the
//  smallest cycle. Map.merge returns the NEW count, which is why the
//  "== 0" test can sit on the same line as the decrement.
//  Java detail: the forEach lambda can add to `ready` because the
//  variable is effectively final; only the queue's contents change.

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
        var waiting = waitingCounts(deps);
        var unlocks = unlocks(deps);
        var ready = new PriorityQueue<String>();
        waiting.forEach((task, n) -> { if (n == 0) ready.add(task); });
        var order = new ArrayList<String>();
        while (!ready.isEmpty()) {
            var t = ready.poll();
            order.add(t);
            for (var next : unlocks.getOrDefault(t, List.of())) {
                if (waiting.merge(next, -1, Integer::sum) == 0) ready.add(next);
            }
        }
        if (order.size() < waiting.size()) {
            throw new IllegalStateException("cycle");
        }
        return order;
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
