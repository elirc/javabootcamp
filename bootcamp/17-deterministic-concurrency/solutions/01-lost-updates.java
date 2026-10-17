// ─────────────────────────────────────────────────────────────────────────
//  01 · lost updates, replayed — SOLUTION                   ★☆☆ warm-up
//  concepts: race conditions · read-modify-write · interleavings · CAS
//  run: java -cp ../../_lib/out 01-lost-updates.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Every concurrency bug in this module starts here. counter++ compiles to
//  a read, an add and a write, and the scheduler may run another thread
//  between them. "ABAB" is the lost update: both read 0, both write 1,
//  one increment vanishes. A test that runs real threads hits this only
//  sometimes, which is why this exercise replays the interleavings by hand
//  instead: the same schedule always gives the same answer.
//  outcomes(n) makes the scale of the problem concrete. With one increment
//  each, 4 of the 6 interleavings lose an update. With two each, only 6 of
//  70 give the right answer of 4. Real threads usually run each worker's
//  steps back to back, so a threaded test mostly passes anyway.
//  COMPARE-AND-SET is how AtomicInteger fixes it without a lock. The write
//  says "set to r + 1 only if the value is still r". When another worker
//  got there first, the CAS fails, nothing is overwritten, and the worker
//  reads again and retries. updateAndGet() is exactly that retry loop;
//  incrementAndGet() means the same, though the JIT may use one atomic
//  add instruction (LOCK XADD on x86). Under CAS no completed increment
//  is ever lost; contention costs retries instead.
//  The register map is the detail: a worker with a pending READ holds a
//  stale copy, and the bug is writing from it.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record CasRun(int counter, int failedWrites) {}

    static int runPlain(String schedule) {
        check(schedule);
        int counter = 0;
        var registers = new HashMap<Character, Integer>();
        for (char worker : schedule.toCharArray()) {
            Integer read = registers.remove(worker);
            if (read == null) registers.put(worker, counter);
            else counter = read + 1;
        }
        return counter;
    }

    static int lostUpdates(String schedule) {
        check(schedule);
        int counter = 0, writes = 0;
        var registers = new HashMap<Character, Integer>();
        for (char worker : schedule.toCharArray()) {
            Integer read = registers.remove(worker);
            if (read == null) {
                registers.put(worker, counter);
            } else {
                counter = read + 1;
                writes++;
            }
        }
        return writes - counter;
    }

    static CasRun runCas(String schedule) {
        check(schedule);
        int counter = 0, failed = 0;
        var registers = new HashMap<Character, Integer>();
        for (char worker : schedule.toCharArray()) {
            Integer read = registers.remove(worker);
            if (read == null) registers.put(worker, counter);
            else if (read == counter) counter = read + 1;
            else failed++;
        }
        return new CasRun(counter, failed);
    }

    static Map<Integer, Integer> outcomes(int incrementsEach) {
        var counts = new TreeMap<Integer, Integer>();
        interleave("", 2 * incrementsEach, 2 * incrementsEach, counts);
        return counts;
    }

    private static void check(String schedule) {
        for (char c : schedule.toCharArray()) {
            if (c < 'A' || c > 'Z') throw new IllegalArgumentException("not a worker: '" + c + "'");
        }
    }

    private static void interleave(String prefix, int aLeft, int bLeft, Map<Integer, Integer> counts) {
        if (aLeft == 0 && bLeft == 0) {
            counts.merge(runPlain(prefix), 1, Integer::sum);
            return;
        }
        if (aLeft > 0) interleave(prefix + "A", aLeft - 1, bLeft, counts);
        if (bLeft > 0) interleave(prefix + "B", aLeft, bLeft - 1, counts);
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
