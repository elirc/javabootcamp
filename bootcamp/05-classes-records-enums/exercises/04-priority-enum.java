// ─────────────────────────────────────────────────────────────────────────
//  04 · enums that know things                              ★★☆ core
//  concepts: enum fields · values() · EnumMap · ordinal order
//  run: java -cp ../../_lib/out 04-priority-enum.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A to-do manager's Priority enum carries a one-letter code and how many
//  days a task of that priority may wait. Finish its methods (the enum is
//  above the tests; its constructor is done): fromCode looks a code up
//  case-insensitively (unknown → IllegalArgumentException); raise returns
//  the next more urgent priority (HIGH stays HIGH); countAll counts a list
//  of priorities into a map that has EVERY priority as a key, in enum
//  order.
//
//      Priority.fromCode("m")              → MEDIUM
//      Priority.LOW.raise()                → MEDIUM
//      countAll(List.of(HIGH, HIGH))       → {LOW=0, MEDIUM=0, HIGH=2}
//
//  hint: values() returns the constants in declaration order.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    enum Priority {
        LOW("L", 7), MEDIUM("M", 3), HIGH("H", 1);

        private final String code;
        private final int daysToDue;

        Priority(String code, int daysToDue) {
            this.code = code;
            this.daysToDue = daysToDue;
        }

        int daysToDue() {
            return daysToDue;
        }

        static Priority fromCode(String code) {
            throw new UnsupportedOperationException("TODO");
        }

        Priority raise() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static Map<Priority, Integer> countAll(List<Priority> priorities) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("fromCode finds a priority by its code", () -> eq(Priority.fromCode("H"), Priority.HIGH));
        test("fromCode ignores case", () -> eq(Priority.fromCode("m"), Priority.MEDIUM));
        test("fromCode refuses an unknown code", () -> {
            raises(IllegalArgumentException.class, () -> Priority.fromCode("X"));
            raises(IllegalArgumentException.class, () -> Priority.fromCode("MEDIUM"));
        });
        test("raise moves one step more urgent", () -> {
            eq(Priority.LOW.raise(), Priority.MEDIUM);
            eq(Priority.MEDIUM.raise(), Priority.HIGH);
        });
        test("raise stops at HIGH", () -> eq(Priority.HIGH.raise(), Priority.HIGH));
        test("countAll counts and keeps every key", () ->
            eq(countAll(List.of(Priority.HIGH, Priority.LOW, Priority.HIGH)),
                Map.of(Priority.LOW, 1, Priority.MEDIUM, 0, Priority.HIGH, 2)));
        test("countAll iterates in enum order", () ->
            eq(new ArrayList<>(countAll(List.of()).keySet()), List.of(Priority.LOW, Priority.MEDIUM, Priority.HIGH)));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
