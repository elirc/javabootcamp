// ─────────────────────────────────────────────────────────────────────────
//  02 · replay the log — SOLUTION                           ★★☆ core
//  concepts: write-ahead log · crash recovery · commit records · torn writes
//  run: java -cp ../../_lib/out 02-write-ahead-log.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A database appends every change to a log and forces it to disk BEFORE
//  touching its data files; after a crash it rebuilds state by replaying
//  the log. The rule that makes this atomic is that only the COMMIT
//  record makes a transaction real. So replay keeps a buffer of pending
//  operations per open transaction and applies a transaction's buffer,
//  in order, at the moment its COMMIT line is read. Begin order does not
//  matter, commit order does: two transactions that wrote the same key
//  land in the order they committed. ABORT and "never committed" are the
//  same outcome: the buffer is dropped.
//  The classic wrong turn is applying SET lines as they are read; then a
//  crash halfway through a transaction leaves half of it in the data.
//  The other is treating every unparsable line alike. A crash can only
//  tear the LAST write, so a broken last line is expected and ignored
//  (if it was the COMMIT, that transaction simply did not commit). A
//  broken line with valid lines after it cannot come from a crash: that
//  is corruption, and silently skipping it would lose data, so recovery
//  refuses with the line number. A record for a transaction that is not
//  open is corruption of the same kind.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static Map<String, String> recover(List<String> log) {
        var state = new HashMap<String, String>();
        var open = new HashMap<String, List<String[]>>();
        for (int i = 0; i < log.size(); i++) {
            var f = log.get(i).split(" ", -1);
            if (!wellFormed(f)) {
                if (i == log.size() - 1) break;
                throw new IllegalStateException("corrupt log at line " + (i + 1));
            }
            var tx = f[1];
            if (f[0].equals("BEGIN")) {
                if (open.containsKey(tx)) throw new IllegalStateException("line " + (i + 1) + ": " + tx + " already open");
                open.put(tx, new ArrayList<>());
                continue;
            }
            var ops = open.get(tx);
            if (ops == null) throw new IllegalStateException("line " + (i + 1) + ": " + tx + " is not open");
            switch (f[0]) {
                case "SET", "DEL" -> ops.add(f);
                case "ABORT" -> open.remove(tx);
                default -> {
                    for (var op : open.remove(tx)) {
                        if (op[0].equals("SET")) state.put(op[2], op[3]);
                        else state.remove(op[2]);
                    }
                }
            }
        }
        return state;
    }

    private static boolean wellFormed(String[] f) {
        for (var part : f) if (part.isEmpty()) return false;
        return switch (f[0]) {
            case "BEGIN", "COMMIT", "ABORT" -> f.length == 2;
            case "SET" -> f.length == 4;
            case "DEL" -> f.length == 3;
            default -> false;
        };
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("replays a committed transaction", () -> {
            eq(recover(List.of("BEGIN t1", "SET t1 a 1", "SET t1 b 2", "COMMIT t1")), Map.of("a", "1", "b", "2"));
        });
        test("an empty log or an uncommitted transaction recovers nothing", () -> {
            eq(recover(List.of()), Map.of());
            eq(recover(List.of("BEGIN t1", "SET t1 a 1", "BEGIN t2", "SET t2 b 2", "COMMIT t2")), Map.of("b", "2"));
        });
        test("an aborted transaction is dropped", () -> {
            eq(recover(List.of("BEGIN t1", "SET t1 a 1", "ABORT t1", "BEGIN t2", "SET t2 b 2", "COMMIT t2")),
                Map.of("b", "2"));
        });
        test("transactions apply in commit order, not begin order", () -> {
            eq(recover(List.of("BEGIN t1", "BEGIN t2", "SET t1 k first", "SET t2 k second",
                "COMMIT t2", "COMMIT t1")), Map.of("k", "first"));
        });
        test("within a transaction the last write to a key wins", () -> {
            eq(recover(List.of("BEGIN t1", "SET t1 a 1", "SET t1 a 2", "SET t1 b 3", "DEL t1 b", "COMMIT t1")),
                Map.of("a", "2"));
        });
        test("DEL removes a key an earlier transaction set", () -> {
            eq(recover(List.of("BEGIN t1", "SET t1 a 1", "SET t1 b 2", "COMMIT t1",
                "BEGIN t2", "DEL t2 a", "DEL t2 nope", "COMMIT t2")), Map.of("b", "2"));
        });
        test("a torn last line is ignored, even a torn COMMIT", () -> {
            eq(recover(List.of("BEGIN t1", "SET t1 a 1", "COMMIT t1", "BEGIN t2", "SET t2 b")), Map.of("a", "1"));
            eq(recover(List.of("BEGIN t1", "SET t1 a 1", "COMMIT")), Map.of());
            eq(recover(List.of("BEGIN t1", "SET t1 a 1", "COMMIT t1", "")), Map.of("a", "1"));
        });
        test("a broken line before the end is corruption, with its line number", () -> {
            raises(IllegalStateException.class,
                () -> recover(List.of("BEGIN t1", "SET t1 a", "COMMIT t1")), "line 2");
            raises(IllegalStateException.class,
                () -> recover(List.of("BEGIN t1", "PUT t1 a 1", "COMMIT t1")), "line 2");
        });
        test("a record for a transaction that is not open is corruption", () -> {
            raises(IllegalStateException.class, () -> recover(List.of("SET t9 a 1", "BEGIN t1")));
            raises(IllegalStateException.class, () -> recover(List.of("BEGIN t1", "COMMIT t1", "SET t1 a 1")));
            raises(IllegalStateException.class, () -> recover(List.of("BEGIN t1", "BEGIN t1", "COMMIT t1")));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
