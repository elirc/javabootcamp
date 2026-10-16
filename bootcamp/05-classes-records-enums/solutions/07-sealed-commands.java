// ─────────────────────────────────────────────────────────────────────────
//  07 · a closed set of commands — SOLUTION                 ★★★ stretch
//  concepts: sealed interfaces · records · instanceof patterns · immutability
//  run: java -cp ../../_lib/out 07-sealed-commands.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A sealed interface lists its implementations: `sealed interface Command
//  permits Add, Complete, Rename`. Nobody else can implement it, so code
//  that handles those three handles every command — the type system now
//  knows the family is closed. Records make each command a one-line data
//  carrier.
//  Java 17 dispatches on such a family with instanceof patterns: `if (c
//  instanceof Add add) { ... add.title() ... }`. (Java 21 adds pattern
//  `switch` with exhaustiveness checking — `case Add a ->` — which is the
//  real payoff of sealed types; on 17 the final `throw` after the if-chain
//  is the "can't happen" arm.)
//  The tasks are immutable records, so "completing" a task means building
//  a replacement — a small wither helps — and the list is rebuilt with
//  stream().map(...), replacing the one matching id. Returning a new list
//  leaves the caller's list untouched, which is what makes undo, history
//  and testing trivial.
//  Unknown ids: check that some task matched (anyMatch first, or count
//  replacements) and throw IllegalArgumentException — silently ignoring a
//  command for a task that does not exist hides bugs in the caller.
//  The next id is max(id) + 1, with 0 as the max of nothing: mapToInt(
//  Task::id).max().orElse(0) + 1. Using size() + 1 breaks the moment a
//  task is ever deleted.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Task(int id, String title, boolean done) {}

    sealed interface Command permits Add, Complete, Rename {}

    record Add(String title) implements Command {}

    record Complete(int id) implements Command {}

    record Rename(int id, String title) implements Command {}

    static List<Task> apply(List<Task> tasks, Command command) {
        if (command instanceof Add add) {
            int next = tasks.stream().mapToInt(Task::id).max().orElse(0) + 1;
            var result = new ArrayList<>(tasks);
            result.add(new Task(next, add.title(), false));
            return List.copyOf(result);
        }
        if (command instanceof Complete complete) {
            return replace(tasks, complete.id(), t -> new Task(t.id(), t.title(), true));
        }
        if (command instanceof Rename rename) {
            return replace(tasks, rename.id(), t -> new Task(t.id(), rename.title(), t.done()));
        }
        throw new IllegalArgumentException("unknown command: " + command);
    }

    static String describe(Command command) {
        if (command instanceof Add add) return "add " + add.title();
        if (command instanceof Complete complete) return "complete #" + complete.id();
        if (command instanceof Rename rename) return "rename #" + rename.id() + " to " + rename.title();
        throw new IllegalArgumentException("unknown command: " + command);
    }

    private static List<Task> replace(List<Task> tasks, int id, java.util.function.UnaryOperator<Task> change) {
        if (tasks.stream().noneMatch(t -> t.id() == id)) throw new IllegalArgumentException("no task #" + id);
        return tasks.stream().map(t -> t.id() == id ? change.apply(t) : t).toList();
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
