// ─────────────────────────────────────────────────────────────────────────
//  07 · a closed set of commands                            ★★★ stretch
//  concepts: sealed interfaces · records · instanceof patterns · immutability
//  run: java -cp ../../_lib/out 07-sealed-commands.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A to-do manager's commands form a CLOSED family: the sealed interface
//  Command permits exactly Add, Complete and Rename (records, above the
//  tests). Write apply: given the current tasks and a command, return the
//  NEW list of tasks (the input list is never changed). Add gets the next
//  id (highest id + 1; 1 for an empty list); Complete marks a task done;
//  Rename changes a title. Complete or Rename of an unknown id →
//  IllegalArgumentException. Then write describe, a one-line summary.
//
//      apply([], new Add("milk"))     → [Task[id=1, title=milk, done=false]]
//      apply(tasks, new Complete(1))        → task 1 now done
//      describe(new Rename(2, "eggs"))      → "rename #2 to eggs"
//
//  hint: `if (c instanceof Add add)` tests, casts and names in one step.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Task(int id, String title, boolean done) {}

    sealed interface Command permits Add, Complete, Rename {}

    record Add(String title) implements Command {}

    record Complete(int id) implements Command {}

    record Rename(int id, String title) implements Command {}

    static List<Task> apply(List<Task> tasks, Command command) {
        throw new UnsupportedOperationException("TODO");
    }

    static String describe(Command command) {
        throw new UnsupportedOperationException("TODO");
    }

    static final List<Task> TWO = List.of(new Task(1, "milk", false), new Task(4, "eggs", false));

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("add to an empty list starts at id 1", () ->
            eq(apply(List.of(), new Add("milk")), List.of(new Task(1, "milk", false))));
        test("add uses the highest id plus one", () -> eq(apply(TWO, new Add("bread")).get(2), new Task(5, "bread", false)));
        test("complete marks one task done", () ->
            eq(apply(TWO, new Complete(4)), List.of(new Task(1, "milk", false), new Task(4, "eggs", true))));
        test("rename changes only the title", () ->
            eq(apply(TWO, new Rename(1, "oat milk")), List.of(new Task(1, "oat milk", false), new Task(4, "eggs", false))));
        test("an unknown id is an error", () -> {
            raises(IllegalArgumentException.class, () -> apply(TWO, new Complete(9)));
            raises(IllegalArgumentException.class, () -> apply(TWO, new Rename(9, "x")));
        });
        test("the input list never changes", () -> {
            var tasks = new ArrayList<>(TWO);
            apply(tasks, new Add("bread"));
            apply(tasks, new Complete(1));
            eq(tasks, TWO);
        });
        test("describes each command", () -> {
            eq(describe(new Add("milk")), "add milk");
            eq(describe(new Complete(3)), "complete #3");
            eq(describe(new Rename(2, "eggs")), "rename #2 to eggs");
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
