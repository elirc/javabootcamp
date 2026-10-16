// ─────────────────────────────────────────────────────────────────────────
//  04 · the command parser                                  ★★☆ core
//  concepts: switch expressions · enums · String.split · Locale.ROOT
//  run: java -cp ../../_lib/out 04-command-switch.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A to-do manager reads commands typed at a prompt. Write parseCommand:
//  only the first word counts, surrounding spaces and case are ignored,
//  and each command has aliases. Anything else — including null or blank
//  input — is UNKNOWN. Then write describe, one help line per command.
//
//      add | a | new          → ADD        list | ls       → LIST
//      done | finish | x      → DONE       rm | remove | delete → REMOVE
//      quit | exit | q        → QUIT
//
//      parseCommand("  LS ")           → LIST
//      parseCommand("add buy milk")    → ADD
//      describe(Command.DONE)          → "mark a task done"
//
//  The enum Command is defined above the code you edit.
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
