// ─────────────────────────────────────────────────────────────────────────
//  08 · undo, in order                                      ★★★ stretch
//  concepts: ArrayDeque as a stack · command history · unmodifiable views
//  run: java -cp ../../_lib/out 08-undo-stack.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A to-do manager gets an undo key. Finish the nested class TodoList:
//  add appends a task; remove deletes the first matching task and returns
//  whether it found one; undo reverts the most recent add or SUCCESSFUL
//  remove (a removed task returns to the position it had) and returns
//  false when there is nothing to undo; tasks returns a read-only view.
//
//      add("milk"); add("eggs"); remove("milk");   tasks → [eggs]
//      undo()                                      tasks → [milk, eggs]
//      undo()                                      tasks → [milk]
//
//  hint: remember what to reverse, not what the list looked like.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class TodoList {
        private final List<String> tasks = new ArrayList<>();

        void add(String task) {
            throw new UnsupportedOperationException("TODO");
        }

        boolean remove(String task) {
            throw new UnsupportedOperationException("TODO");
        }

        boolean undo() {
            throw new UnsupportedOperationException("TODO");
        }

        List<String> tasks() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("add appends tasks", () -> {
            var todo = new TodoList();
            todo.add("milk");
            todo.add("eggs");
            eq(todo.tasks(), List.of("milk", "eggs"));
        });
        test("undo reverts an add", () -> {
            var todo = new TodoList();
            todo.add("milk");
            todo.add("eggs");
            eq(todo.undo(), true);
            eq(todo.tasks(), List.of("milk"));
        });
        test("undo puts a removed task back where it was", () -> {
            var todo = new TodoList();
            todo.add("milk");
            todo.add("eggs");
            todo.add("bread");
            eq(todo.remove("eggs"), true);
            todo.undo();
            eq(todo.tasks(), List.of("milk", "eggs", "bread"));
        });
        test("undoes in last-in, first-out order", () -> {
            var todo = new TodoList();
            todo.add("milk");
            todo.add("eggs");
            todo.remove("milk");
            todo.undo();
            eq(todo.tasks(), List.of("milk", "eggs"));
            todo.undo();
            eq(todo.tasks(), List.of("milk"));
        });
        test("nothing to undo returns false", () -> eq(new TodoList().undo(), false));
        test("a failed remove leaves no history", () -> {
            var todo = new TodoList();
            todo.add("milk");
            eq(todo.remove("tea"), false);
            todo.undo();
            eq(todo.tasks(), List.of());
        });
        test("tasks is read-only", () -> {
            var todo = new TodoList();
            todo.add("milk");
            raises(UnsupportedOperationException.class, () -> todo.tasks().add("sneaky"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
