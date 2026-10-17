// ─────────────────────────────────────────────────────────────────────────
//  06 · ghost writes after a crash                          ★★★ stretch
//  concepts: debugging · write-ahead log · crash recovery · commit records
//  run: java -cp ../../_lib/out 06-wal-ghost-writes.java
// ─────────────────────────────────────────────────────────────────────────
//
//  DEBUG HUNT: the code below is complete and compiles; it has bugs. Run
//  it, read the failing tests, find and fix the bugs. Do not edit tests.
//  Intended: recover(log) rebuilds the key-value state from a write-
//  ahead log of "BEGIN tx", "SET tx key value", "DEL tx key",
//  "COMMIT tx", "ABORT tx" (well-formed; transactions may interleave).
//  A transaction's SET/DEL take effect, in their log order, at the
//  moment its COMMIT is read, and only then. A transaction that aborted,
//  or was still open when the log ends (the crash), leaves no trace.
//  Bug reports:
//    "After the crash, a transfer that never committed shows up: the
//     money left account A and never reached B."
//    "A transaction that was ABORTED is visible after recovery, but only
//     when another transaction committed while it was open."
//
//      BEGIN t1, SET t1 a 1, COMMIT t1, BEGIN t2, SET t2 b 2 → {a=1}
//
//  hint: for each line of a failing log, write which writes are applied.

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
                    for (var ops : pending.values()) {
                        for (var op : ops) applyOp(state, op);
                    }
                    pending.remove(f[1]);
                }
                case "ABORT" -> pending.remove(f[1]);
                default -> throw new IllegalStateException("bad record: " + line);
            }
        }
        // flush whatever was still in flight when the log ended
        for (var ops : pending.values()) {
            for (var op : ops) applyOp(state, op);
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
        test("DEL removes a key an earlier transaction committed", () -> {
            eq(recover(List.of("BEGIN t1", "SET t1 a 1", "SET t1 b 2", "COMMIT t1",
                "BEGIN t2", "DEL t2 a", "COMMIT t2")), Map.of("b", "2"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
