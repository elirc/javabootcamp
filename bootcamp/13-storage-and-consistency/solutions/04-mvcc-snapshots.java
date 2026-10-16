// ─────────────────────────────────────────────────────────────────────────
//  04 · snapshot reads — SOLUTION                           ★★★ stretch
//  concepts: MVCC · snapshot isolation · commit timestamps · write conflicts
//  run: java -cp ../../_lib/out 04-mvcc-snapshots.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Multi-version concurrency control (PostgreSQL, Oracle, InnoDB) lets
//  readers and writers stop blocking each other by never overwriting:
//  each commit appends a NEW version stamped with its commit timestamp.
//  A transaction remembers the timestamp it started at and reads, for
//  each key, the newest version whose commitTs <= startTs. Later commits
//  exist but are invisible to it, so its reads repeat exactly. A delete
//  is just a version whose value is null, so a snapshot from before the
//  delete still sees the value.
//  Writes are buffered privately (read-your-own-writes checks the buffer
//  first) and become versions only at commit. The commit check is "first
//  committer wins": if any key I wrote has a version committed after my
//  snapshot, someone changed it under me and I must abort, writing
//  nothing. Keys I only read are not checked; that gap is exactly write
//  skew, the anomaly that separates snapshot isolation from serializable.
//  Garbage collection is where people lose data: for each key keep the
//  newest version at or below the horizon (the oldest live snapshot
//  still reads it) and everything newer; only versions older than that
//  one are unreachable. Dropping everything <= horizon breaks that
//  snapshot.

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
            return new Tx(lastCommitTs);
        }

        void write(Tx tx, String key, String value) {
            tx.writes.put(key, value);
        }

        void delete(Tx tx, String key) {
            tx.writes.put(key, null);
        }

        String read(Tx tx, String key) {
            if (tx.writes.containsKey(key)) return tx.writes.get(key);
            String seen = null;
            for (var v : versions.getOrDefault(key, List.of())) {
                if (v.commitTs() <= tx.startTs) seen = v.value();
            }
            return seen;
        }

        long commit(Tx tx) {
            if (tx.finished) throw new IllegalStateException("transaction already finished");
            tx.finished = true;
            for (var key : tx.writes.keySet()) {
                var list = versions.getOrDefault(key, List.of());
                if (!list.isEmpty() && list.get(list.size() - 1).commitTs() > tx.startTs) {
                    throw new ConflictException(key + " changed after the snapshot");
                }
            }
            long ts = ++lastCommitTs;
            tx.writes.forEach((key, value) ->
                versions.computeIfAbsent(key, k -> new ArrayList<>()).add(new Version(ts, value)));
            return ts;
        }

        int gc(long horizon) {
            int dropped = 0;
            for (var list : versions.values()) {
                int keepFrom = 0;
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).commitTs() <= horizon) keepFrom = i;
                }
                list.subList(0, keepFrom).clear();
                dropped += keepFrom;
            }
            return dropped;
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
            db.write(b, "x", "b");
            db.write(b, "y", "b");
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
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
