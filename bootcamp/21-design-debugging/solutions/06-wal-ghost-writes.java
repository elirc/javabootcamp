// ─────────────────────────────────────────────────────────────────────────
//  06 · ghost writes after a crash — SOLUTION               ★★★ stretch
//  concepts: debugging · write-ahead log · crash recovery · commit records
//  run: java -cp ../../_lib/out 06-wal-ghost-writes.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Both bugs apply work that never committed, from two directions.
//  1. Reproduce report 1: BEGIN t1, SET t1 from 90, SET t1 to 110, and
//     the log ends (the crash). Recovery returns both writes. Narrow: the
//     loop applies nothing (no COMMIT), so it is the code AFTER the loop:
//     a "flush whatever was in flight" step. That flush is exactly what a
//     WAL must never do. A transaction is real only once its COMMIT
//     record is durable; anything still open at the end of the log
//     belongs to a transaction the crash interrupted. Fix: delete the
//     flush. Pinned by "a transaction still open at the crash leaves no
//     trace".
//  2. Reproduce report 2: t1 and t2 open together, t1 commits, t2
//     aborts. t2's writes are visible. Narrow: print state right after
//     COMMIT t1; it already holds t2's writes. The COMMIT branch loops
//     over EVERY pending transaction instead of the one committing. Fix:
//     apply only pending.remove(tx). Pinned by "an abort after another
//     transaction's commit leaves no trace".
//  Notice why bug 2 survived: with one transaction at a time, "apply all
//  pending" and "apply this one" are the same. Only interleaving shows
//  the difference, and real logs interleave constantly. Module 13,
//  exercise 02 adds torn-write and corruption handling on top.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static Map<String, String> recover(List<String> log) {
        var state = new HashMap<String, String>();
        var pending = new LinkedHashMap<String, List<String[]>>();
        for (var line : log) {
            var f = line.split(" ");
            switch (f[0]) {
                case "BEGIN" -> pending.put(f[1], new ArrayList<>());
                case "SET", "DEL" -> pending.get(f[1]).add(f);
                case "COMMIT" -> {
                    for (var op : pending.remove(f[1])) applyOp(state, op);
                }
                case "ABORT" -> pending.remove(f[1]);
                default -> throw new IllegalStateException("bad record: " + line);
            }
        }
        return state;
    }

    static void applyOp(Map<String, String> state, String[] op) {
        if (op[0].equals("SET")) state.put(op[2], op[3]);
        else state.remove(op[2]);
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("replays committed transactions", () -> {
            eq(recover(List.of("BEGIN t1", "SET t1 a 1", "SET t1 b 2", "COMMIT t1")), Map.of("a", "1", "b", "2"));
            eq(recover(List.of()), Map.of());
        });
        test("a transaction still open at the crash leaves no trace", () -> {
            eq(recover(List.of("BEGIN t1", "SET t1 a 1", "COMMIT t1", "BEGIN t2", "SET t2 b 2")), Map.of("a", "1"));
            eq(recover(List.of("BEGIN t1", "SET t1 from 90", "SET t1 to 110")), Map.of());
        });
        test("an aborted transaction leaves no trace", () -> {
            eq(recover(List.of("BEGIN t1", "SET t1 a 1", "ABORT t1")), Map.of());
        });
        test("an abort after another transaction's commit leaves no trace", () -> {
            eq(recover(List.of("BEGIN t1", "BEGIN t2", "SET t1 k mine", "SET t2 k theirs", "SET t2 x 1",
                "COMMIT t1", "ABORT t2")), Map.of("k", "mine"));
        });
        test("interleaved transactions apply in commit order", () -> {
            eq(recover(List.of("BEGIN t1", "BEGIN t2", "SET t2 k second", "SET t1 k first",
                "COMMIT t2", "COMMIT t1")), Map.of("k", "first"));
        });
        test("a transaction's writes apply in their log order", () -> {
            eq(recover(List.of("BEGIN t1", "SET t1 k 1", "SET t1 k 2", "DEL t1 j", "SET t1 j 3", "SET t1 x 0",
                "DEL t1 x", "COMMIT t1")), Map.of("k", "2", "j", "3"));
        });
        test("DEL removes a key an earlier transaction committed", () -> {
            eq(recover(List.of("BEGIN t1", "SET t1 a 1", "SET t1 b 2", "COMMIT t1",
                "BEGIN t2", "DEL t2 a", "COMMIT t2")), Map.of("b", "2"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
