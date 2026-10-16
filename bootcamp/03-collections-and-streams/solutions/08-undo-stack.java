// ─────────────────────────────────────────────────────────────────────────
//  08 · undo, in order — SOLUTION                           ★★★ stretch
//  concepts: ArrayDeque as a stack · command history · unmodifiable views
//  run: java -cp ../../_lib/out 08-undo-stack.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Undo is a stack: the last change is the first one reverted. In Java,
//  the stack you want is ArrayDeque used through the Deque interface —
//  push, pop, isEmpty. The old java.util.Stack class is synchronized,
//  extends Vector, and is kept only for compatibility.
//  What goes on the stack? Not snapshots of the whole list (that costs
//  O(n) per change) but a small record of the inverse: "remove the task at
//  index i" for an add, "insert this task at index i" for a remove. Here
//  that is a record Change(boolean wasAdd, int index, String task); a
//  Deque<Runnable> of ready-made inverse actions works just as well.
//  The position matters: undoing a remove must put the task back where it
//  was, not at the end, so remove records indexOf BEFORE removing.
//  A remove that found nothing changed nothing, so it must not push a
//  history entry — otherwise the next undo "reverts" a no-op and the user
//  has to press undo twice.
//  tasks() returns Collections.unmodifiableList(tasks): a read-only VIEW
//  that reflects later changes, and throws UnsupportedOperationException
//  on add. Handing out the real list would let callers bypass the history.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class TodoList {
        private final List<String> tasks = new ArrayList<>();
        private record Change(boolean wasAdd, int index, String task) {}

        private final Deque<Change> history = new ArrayDeque<>();

        void add(String task) {
            tasks.add(task);
            history.push(new Change(true, tasks.size() - 1, task));
        }

        boolean remove(String task) {
            int index = tasks.indexOf(task);
            if (index < 0) return false;
            tasks.remove(index);
            history.push(new Change(false, index, task));
            return true;
        }

        boolean undo() {
            if (history.isEmpty()) return false;
            var change = history.pop();
            if (change.wasAdd()) tasks.remove(change.index());
            else tasks.add(change.index(), change.task());
            return true;
        }

        List<String> tasks() {
            return Collections.unmodifiableList(tasks);
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
