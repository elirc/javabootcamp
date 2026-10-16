// ─────────────────────────────────────────────────────────────────────────
//  01 · remove the right thing                              ★☆☆ warm-up
//  concepts: List.remove overloads · removeIf · copies vs in-place
//  run: java -cp ../../_lib/out 01-list-remove.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A to-do manager stores task ids in a List<Integer> and task lines in a
//  List<String>. Write removeTask (remove the first task with that ID, not
//  the task at that position; an unknown id changes nothing), withoutDone
//  (a NEW list without the lines starting "[x]", input unchanged) and
//  dropDone (the same filter, but in place on the given list).
//
//      ids = [5, 0, 7];  removeTask(ids, 0)      → ids is [5, 7]
//      withoutDone(["[x] milk", "[ ] eggs"])     → ["[ ] eggs"]
//
//  hint: List<Integer> has both remove(int index) and remove(Object o).

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static void removeTask(List<Integer> ids, int id) {
        throw new UnsupportedOperationException("TODO");
    }

    static List<String> withoutDone(List<String> tasks) {
        throw new UnsupportedOperationException("TODO");
    }

    static void dropDone(List<String> tasks) {
        throw new UnsupportedOperationException("TODO");
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
