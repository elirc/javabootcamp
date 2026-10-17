// ─────────────────────────────────────────────────────────────────────────
//  01 · lost updates, replayed                              ★☆☆ warm-up
//  concepts: race conditions · read-modify-write · interleavings · CAS
//  run: java -cp ../../_lib/out 01-lost-updates.java
// ─────────────────────────────────────────────────────────────────────────
//
//  counter++ is two steps: READ the counter into a register, then WRITE
//  register + 1. Replay it by hand: in a schedule like "ABAB" each letter
//  is a worker (capital letters only, else IllegalArgumentException)
//  taking its next step, and each worker alternates READ, WRITE, READ...
//
//    runPlain(s)     the final counter with plain writes
//    lostUpdates(s)  completed writes minus the final counter (plain)
//    runCas(s)       CasRun(counter, failedWrites): a WRITE is a
//                    compare-and-set that only succeeds if the counter
//                    still holds the value READ; a failed WRITE is
//                    counted and that worker's next step is a READ again
//    outcomes(n)     for two workers doing n increments each (4n steps),
//                    how often each final value occurs over EVERY
//                    interleaving, as a sorted map
//
//      runPlain("AABB") → 2   runPlain("ABAB") → 1   outcomes(1) → {1=4, 2=2}
//
//  hint: a worker that has read but not yet written holds a register.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record CasRun(int counter, int failedWrites) {}

    static int runPlain(String schedule) {
        throw new UnsupportedOperationException("TODO");
    }

    static int lostUpdates(String schedule) {
        throw new UnsupportedOperationException("TODO");
    }

    static CasRun runCas(String schedule) {
        throw new UnsupportedOperationException("TODO");
    }

    static Map<Integer, Integer> outcomes(int incrementsEach) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("schedules that never interleave lose nothing", () -> {
            eq(runPlain("AABB"), 2);
            eq(runPlain("AABBAA"), 3);
            eq(runPlain(""), 0);
        });
        test("read, read, write, write loses an update", () -> {
            eq(runPlain("ABAB"), 1);
            eq(runPlain("ABBA"), 1);
            eq(runPlain("ABCABC"), 1);
        });
        test("a read without its write never completes an increment", () -> {
            eq(runPlain("AAB"), 1);
            eq(runPlain("BAAAB"), 1);
        });
        test("lostUpdates counts the completed writes that were overwritten", () -> {
            eq(lostUpdates("ABAB"), 1);
            eq(lostUpdates("ABCABC"), 2);
            eq(lostUpdates("AABB"), 0);
            eq(lostUpdates("ABA"), 0);
        });
        test("with compare-and-set the stale write fails and is counted", () -> {
            eq(runCas("ABAB"), new CasRun(1, 1));
            eq(runCas("AABB"), new CasRun(2, 0));
        });
        test("a worker whose CAS failed reads again and then succeeds", () -> {
            eq(runCas("ABABBB"), new CasRun(2, 1));
            eq(runCas("ABCABCBCC"), new CasRun(2, 2));
        });
        test("outcomes counts the final value of every interleaving", () -> {
            eq(outcomes(1), Map.of(1, 4, 2, 2));
            eq(outcomes(2), Map.of(2, 32, 3, 32, 4, 6));
            eq(new ArrayList<>(outcomes(2).keySet()), List.of(2, 3, 4));
        });
        test("a schedule with anything but capital letters is rejected", () -> {
            raises(IllegalArgumentException.class, () -> runPlain("AbA"));
            raises(IllegalArgumentException.class, () -> runCas("A1"));
            raises(IllegalArgumentException.class, () -> lostUpdates("A B"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
