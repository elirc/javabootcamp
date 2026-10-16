// ─────────────────────────────────────────────────────────────────────────
//  01 · remove the right thing — SOLUTION                   ★☆☆ warm-up
//  concepts: List.remove overloads · removeIf · copies vs in-place
//  run: java -cp ../../_lib/out 01-list-remove.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  List has two remove methods: remove(int index) and remove(Object o).
//  With a List<Integer> and an int argument, overload resolution picks
//  remove(int) — an exact match beats boxing — so `ids.remove(id)` removes
//  by POSITION. With ids [5, 0, 7], removing task 0 deletes the 5. It even
//  "works" when id happens to be out of range... by throwing
//  IndexOutOfBoundsException. Box explicitly: ids.remove(Integer.valueOf
//  (id)), which removes the first equal element and returns false if
//  there is none.
//  withoutDone and dropDone are the same rule with two contracts. A
//  method that RETURNS a list should not touch its argument: stream +
//  filter + toList (or copy then removeIf). A method that returns void and
//  is named like a command is expected to mutate: removeIf(predicate) is
//  the one-call, iterator-safe way to filter in place.
//  Naming the contract matters more than the code here. When a reader sees
//  `var open = withoutDone(tasks);` they assume tasks is unchanged; break
//  that and you have a bug three files away.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static void removeTask(List<Integer> ids, int id) {
        ids.remove(Integer.valueOf(id));
    }

    static List<String> withoutDone(List<String> tasks) {
        return tasks.stream().filter(t -> !t.startsWith("[x]")).toList();
    }

    static void dropDone(List<String> tasks) {
        tasks.removeIf(t -> t.startsWith("[x]"));
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("removeTask removes by id", () -> {
            var ids = new ArrayList<>(List.of(10, 20, 30));
            removeTask(ids, 20);
            eq(ids, List.of(10, 30));
        });
        test("removeTask removes by id even when the id is a valid index", () -> {
            var ids = new ArrayList<>(List.of(5, 0, 7));
            removeTask(ids, 0);
            eq(ids, List.of(5, 7));
        });
        test("removeTask ignores an unknown id", () -> {
            var ids = new ArrayList<>(List.of(1, 2));
            removeTask(ids, 99);
            eq(ids, List.of(1, 2));
        });
        test("withoutDone filters finished tasks", () ->
            eq(withoutDone(List.of("[x] milk", "[ ] eggs", "[x] bread")), List.of("[ ] eggs")));
        test("withoutDone leaves its input alone", () -> {
            var tasks = new ArrayList<>(List.of("[x] milk", "[ ] eggs"));
            withoutDone(tasks);
            eq(tasks, List.of("[x] milk", "[ ] eggs"));
        });
        test("dropDone filters in place", () -> {
            var tasks = new ArrayList<>(List.of("[x] milk", "[ ] eggs", "[x] bread"));
            dropDone(tasks);
            eq(tasks, List.of("[ ] eggs"));
        });
        test("dropDone can empty the list", () -> {
            var tasks = new ArrayList<>(List.of("[x] a", "[x] b"));
            dropDone(tasks);
            eq(tasks, List.of());
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
