// ─────────────────────────────────────────────────────────────────────────
//  04 · snapshot reads                                      ★★★ stretch
//  concepts: MVCC · snapshot isolation · commit timestamps · write conflicts
//  run: java -cp ../../_lib/out 04-mvcc-snapshots.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Finish Mvcc, a multi-version store. begin() snapshots at the last
//  commit timestamp (0 before any). write/delete buffer in tx.writes
//  (delete = null). read returns tx's own buffered write if any, else the
//  value of the newest version with commitTs <= tx.startTs (null if none
//  or deleted). commit: a finished tx → IllegalStateException; any key
//  in tx.writes with a version committed AFTER tx.startTs →
//  ConflictException naming the key, nothing written; otherwise append a
//  Version per written key stamped ++lastCommitTs and return it. Either
//  way the tx is finished. gc(horizon): per key, keep the newest version
//  with commitTs <= horizon and every newer one; drop the rest; return
//  how many were dropped.
//
//      a = begin(); b = begin(); write(a, x, 1); commit(a) → 1
//      read(b, x) → null          write(b, x, 2); commit(b) → Conflict
//
//  hint: a snapshot is just a timestamp; a version is just (ts, value).

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Version(long commitTs, String value) {}

    static final class ConflictException extends RuntimeException {
        ConflictException(String message) {
            super(message);
        }
    }

    static final class Tx {
        final long startTs;
        final Map<String, String> writes = new LinkedHashMap<>();   // null value = delete
        boolean finished;

        Tx(long startTs) {
            this.startTs = startTs;
        }
    }

    static final class Mvcc {
        private final Map<String, List<Version>> versions = new HashMap<>();   // oldest first
        private long lastCommitTs;

        int versionCount(String key) {
            return versions.getOrDefault(key, List.of()).size();
        }

        Tx begin() {
            throw new UnsupportedOperationException("TODO");
        }

        void write(Tx tx, String key, String value) {
            throw new UnsupportedOperationException("TODO");
        }

        void delete(Tx tx, String key) {
            throw new UnsupportedOperationException("TODO");
        }

        String read(Tx tx, String key) {
            throw new UnsupportedOperationException("TODO");
        }

        long commit(Tx tx) {
            throw new UnsupportedOperationException("TODO");
        }

        int gc(long horizon) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static Mvcc seeded(String key, String value) {
        var db = new Mvcc();
        var tx = db.begin();
        db.write(tx, key, value);
        db.commit(tx);
        return db;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("a transaction reads its own writes; others do not see them", () -> {
            var db = new Mvcc();
            var a = db.begin();
            var b = db.begin();
            db.write(a, "x", "1");
            eq(db.read(a, "x"), "1");
            eq(db.read(b, "x"), null);
            eq(db.versionCount("x"), 0);
        });
        test("commit timestamps count up from 1 and later snapshots see the commit", () -> {
            var db = new Mvcc();
            var a = db.begin();
            db.write(a, "x", "1");
            eq(db.commit(a), 1);
            var b = db.begin();
            eq(db.read(b, "x"), "1");
            db.write(b, "y", "2");
            eq(db.commit(b), 2);
        });
        test("a snapshot keeps reading the value it started with", () -> {
            var db = seeded("x", "old");
            var reader = db.begin();
            var writer = db.begin();
            db.write(writer, "x", "new");
            db.commit(writer);
            eq(db.read(reader, "x"), "old");
            eq(db.read(db.begin(), "x"), "new");
        });
        test("a delete is a version: older snapshots still see the value", () -> {
            var db = seeded("x", "1");
            var before = db.begin();
            var deleter = db.begin();
            db.delete(deleter, "x");
            eq(db.read(deleter, "x"), null);
            db.commit(deleter);
            eq(db.read(before, "x"), "1");
            eq(db.read(db.begin(), "x"), null);
            eq(db.versionCount("x"), 2);
        });
        test("the first committer wins and the loser writes nothing", () -> {
            var db = seeded("x", "0");
            var a = db.begin();
            var b = db.begin();
            db.write(a, "x", "a");
            db.write(b, "y", "b");
            db.write(b, "x", "b");
            db.commit(a);
            raises(ConflictException.class, () -> db.commit(b), "x");
            eq(db.read(db.begin(), "x"), "a");
            eq(db.versionCount("y"), 0);
        });
        test("concurrent writes to different keys both commit", () -> {
            var db = seeded("x", "0");
            var a = db.begin();
            var b = db.begin();
            eq(db.read(b, "x"), "0");
            db.write(a, "x", "a");
            db.write(b, "y", "b");
            eq(db.commit(a), 2);
            eq(db.commit(b), 3);
        });
        test("a transaction cannot commit twice, even after a conflict", () -> {
            var db = seeded("x", "0");
            var a = db.begin();
            db.commit(a);
            raises(IllegalStateException.class, () -> db.commit(a));
            var b = db.begin();
            var c = db.begin();
            db.write(b, "x", "b");
            db.write(c, "x", "c");
            db.commit(b);
            raises(ConflictException.class, () -> db.commit(c));
            raises(IllegalStateException.class, () -> db.commit(c));
        });
        test("gc keeps the version the oldest snapshot still reads", () -> {
            var db = new Mvcc();
            for (var v : List.of("1", "2", "3", "4")) {
                var tx = db.begin();
                db.write(tx, "x", v);
                db.commit(tx);
            }
            var oldest = new Tx(2);
            eq(db.gc(2), 1);
            eq(db.versionCount("x"), 3);
            eq(db.read(oldest, "x"), "2");
            eq(db.gc(2), 0);
            eq(db.gc(9), 2);
            eq(db.read(db.begin(), "x"), "4");
            var gaps = seeded("x", "1");
            for (var key : List.of("y", "x")) {
                var tx = gaps.begin();
                gaps.write(tx, key, "later");
                gaps.commit(tx);
            }
            eq(gaps.gc(2), 0);
            eq(gaps.read(new Tx(2), "x"), "1");
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
