// ─────────────────────────────────────────────────────────────────────────
//  04 · enums that know things — SOLUTION                   ★★☆ core
//  concepts: enum fields · values() · EnumMap · ordinal order
//  run: java -cp ../../_lib/out 04-priority-enum.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  An enum is a class with a fixed set of instances, so it can have
//  fields, a constructor and methods. Data that belongs to each constant
//  (code, daysToDue) goes in the constructor call next to the constant,
//  not in a switch somewhere else — adding a URGENT constant then forces
//  you to supply its data right there.
//  fromCode loops over values() and compares with equalsIgnoreCase. A
//  switch or a static Map<String, Priority> built once in a static block
//  would also do; for three constants the loop is clearest. Do NOT use
//  valueOf(code) here — valueOf matches the constant NAME ("MEDIUM"), and
//  throws for "m".
//  raise uses declaration order: values()[ordinal() + 1], clamped at the
//  last one. ordinal() is fine INSIDE the enum, where the order is
//  defined; outside, never persist or compute with ordinals — reordering
//  the constants silently changes every stored number.
//  countAll wants an EnumMap: a map specialised for enum keys, backed by
//  an array, iterating in declaration order. Pre-filling every key with 0
//  means callers never meet a null for "no tasks of that priority".

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
            for (var p : values()) {
                if (p.code.equalsIgnoreCase(code)) return p;
            }
            throw new IllegalArgumentException("unknown priority code: " + code);
        }

        Priority raise() {
            var all = values();
            return all[Math.min(ordinal() + 1, all.length - 1)];
        }
    }

    static Map<Priority, Integer> countAll(List<Priority> priorities) {
        var counts = new EnumMap<Priority, Integer>(Priority.class);
        for (var p : Priority.values()) counts.put(p, 0);
        for (var p : priorities) counts.merge(p, 1, Integer::sum);
        return counts;
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
