// ─────────────────────────────────────────────────────────────────────────
//  04 · the command parser                                  ★★☆ core
//  concepts: switch expressions · enums · String.split · Locale.ROOT
//  run: java -cp ../../_lib/out 04-command-switch.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A to-do manager reads commands typed at a prompt. Write parseCommand:
//  only the first word counts, surrounding spaces and case are ignored,
//  and each command has aliases. Anything else — including null or blank
//  input — is UNKNOWN. Then write describe, which returns the help line
//  shown here for each command (the enum Command is already defined):
//
//      add | a | new          → ADD       "add a task"
//      list | ls              → LIST      "list the tasks"
//      done | finish | x      → DONE      "mark a task done"
//      rm | remove | delete   → REMOVE    "remove a task"
//      quit | exit | q        → QUIT      "leave the program"
//      anything else          → UNKNOWN   "unknown command"
//
//      parseCommand("  LS ")           → LIST
//      parseCommand("add buy milk")    → ADD
//
//  hint: a switch EXPRESSION can list several labels per arm.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    enum Command { ADD, LIST, DONE, REMOVE, QUIT, UNKNOWN }

    static Command parseCommand(String input) {
        throw new UnsupportedOperationException("TODO");
    }

    static String describe(Command command) {
        throw new UnsupportedOperationException("TODO");
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
