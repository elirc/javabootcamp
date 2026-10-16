// ─────────────────────────────────────────────────────────────────────────
//  02 · replay the log                                      ★★☆ core
//  concepts: write-ahead log · crash recovery · commit records · torn writes
//  run: java -cp ../../_lib/out 02-write-ahead-log.java
// ─────────────────────────────────────────────────────────────────────────
//
//  The store crashed; all that survived is its write-ahead log. Write
//  recover(log) to rebuild the key-value state. Records (single spaces):
//  "BEGIN tx", "SET tx key value", "DEL tx key", "COMMIT tx", "ABORT tx".
//  A transaction's SET/DEL take effect, in log order, only when its
//  COMMIT is read; aborted and never-committed ones leave no trace. DEL
//  of an absent key is fine. A line that does not parse (unknown verb,
//  wrong field count, an empty field) is a TORN WRITE if it is the last
//  line: ignore it. Anywhere else it is corruption: IllegalStateException
//  whose message contains "line N" (1-based). Also corruption: any record
//  for a transaction that is not open (never begun, already committed or
//  aborted), and BEGIN of a transaction that is already open.
//
//      BEGIN t1, BEGIN t2, SET t1 k a, SET t2 k b, COMMIT t2, COMMIT t1
//                                                     → {k=a}
//      BEGIN t1, SET t1 a 1, COMMIT              → {}  (torn COMMIT)
//
//  hint: buffer each open transaction's operations until its COMMIT.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static Map<String, String> recover(List<String> log) {
        throw new UnsupportedOperationException("TODO");
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
