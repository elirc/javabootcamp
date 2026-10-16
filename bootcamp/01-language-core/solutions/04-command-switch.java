// ─────────────────────────────────────────────────────────────────────────
//  04 · the command parser — SOLUTION                       ★★☆ core
//  concepts: switch expressions · enums · String.split · Locale.ROOT
//  run: java -cp ../../_lib/out 04-command-switch.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Since Java 14 `switch` can be an expression: `var c = switch (word) {
//  case "add", "a", "new" -> Command.ADD; ... default -> Command.UNKNOWN; };`
//  Arrow arms never fall through, several labels share one arm, and the
//  whole thing produces a value — no `break`, no forgotten assignment.
//  Normalise before you switch: trim, split on runs of whitespace (\\s+,
//  so two spaces between words do not make an empty first word), and
//  lowercase with Locale.ROOT. Plain toLowerCase() uses the default
//  locale, and on a Turkish machine "QUIT".toLowerCase() is "quıt" — the
//  dotless i — which matches nothing.
//  describe switches over the ENUM, and here the expression form earns its
//  keep: with no `default`, the compiler insists every constant has an arm.
//  Add a Command.UNDO next month and this method stops compiling until you
//  describe it. A `default ->` arm would silently swallow the new constant;
//  leave it out on purpose when you switch over your own enum.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    enum Command { ADD, LIST, DONE, REMOVE, QUIT, UNKNOWN }

    static Command parseCommand(String input) {
        if (input == null || input.isBlank()) return Command.UNKNOWN;
        var word = input.trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
        return switch (word) {
            case "add", "a", "new" -> Command.ADD;
            case "list", "ls" -> Command.LIST;
            case "done", "finish", "x" -> Command.DONE;
            case "rm", "remove", "delete" -> Command.REMOVE;
            case "quit", "exit", "q" -> Command.QUIT;
            default -> Command.UNKNOWN;
        };
    }

    static String describe(Command command) {
        return switch (command) {
            case ADD -> "add a task";
            case LIST -> "list the tasks";
            case DONE -> "mark a task done";
            case REMOVE -> "remove a task";
            case QUIT -> "leave the program";
            case UNKNOWN -> "unknown command";
        };
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("reads a plain command", () -> eq(parseCommand("list"), Command.LIST));
        test("ignores case and surrounding spaces", () -> eq(parseCommand("  LS "), Command.LIST));
        test("understands the aliases", () -> {
            eq(parseCommand("q"), Command.QUIT);
            eq(parseCommand("delete"), Command.REMOVE);
            eq(parseCommand("x"), Command.DONE);
            eq(parseCommand("new"), Command.ADD);
        });
        test("only the first word counts", () -> eq(parseCommand("add  buy milk"), Command.ADD));
        test("blank and null input is UNKNOWN", () -> {
            eq(parseCommand("   "), Command.UNKNOWN);
            eq(parseCommand(null), Command.UNKNOWN);
        });
        test("an unknown word is UNKNOWN", () -> eq(parseCommand("fly"), Command.UNKNOWN));
        test("describes every command", () -> {
            var lines = new ArrayList<String>();
            for (var c : Command.values()) lines.add(describe(c));
            eq(lines, List.of("add a task", "list the tasks", "mark a task done",
                "remove a task", "leave the program", "unknown command"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
