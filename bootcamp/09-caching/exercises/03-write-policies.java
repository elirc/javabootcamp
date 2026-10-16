// ─────────────────────────────────────────────────────────────────────────
//  03 · cache-aside vs write-through                        ★★☆ core
//  concepts: cache-aside · write-through · invalidation · failure order
//  run: java -cp ../../_lib/out 03-write-policies.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Two ways to keep a cache in front of a database. Both READ the same
//  way: a cache hit returns it; a miss reads the db and caches the row if
//  there is one (an absent row is never cached). They WRITE differently:
//    CacheAside.write    → db.write, then REMOVE the key from the cache
//    WriteThrough.write  → db.write, then PUT the value in the cache
//  The db always goes first: if db.write throws, the exception propagates
//  and the cache is left exactly as it was. Finish read and cached in
//  CachedStore and write in both subclasses; cached(key) shows what the
//  cache holds without touching the db.
//
//      cache-aside:   read u1, write(u1, grace) → cached(u1) → empty
//      write-through: write(u2, linus)          → cached(u2) → linus
//
//  hint: ask what the cache holds if the second line of write never runs.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class Database {
        private final Map<String, String> rows = new HashMap<>();
        int reads;
        int writes;
        boolean failNextWrite;

        Optional<String> read(String key) {
            reads++;
            return Optional.ofNullable(rows.get(key));
        }

        void write(String key, String value) {
            if (failNextWrite) {
                failNextWrite = false;
                throw new IllegalStateException("db down");
            }
            writes++;
            rows.put(key, value);
        }
    }

    interface Store {
        Optional<String> read(String key);

        void write(String key, String value);

        Optional<String> cached(String key);
    }

    static abstract class CachedStore implements Store {
        final Database db;
        final Map<String, String> cache = new HashMap<>();

        CachedStore(Database db) {
            this.db = db;
        }

        public Optional<String> read(String key) {
            throw new UnsupportedOperationException("TODO");
        }

        public Optional<String> cached(String key) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class CacheAside extends CachedStore {
        CacheAside(Database db) {
            super(db);
        }

        public void write(String key, String value) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class WriteThrough extends CachedStore {
        WriteThrough(Database db) {
            super(db);
        }

        public void write(String key, String value) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static Database seeded() {
        var db = new Database();
        db.rows.put("user:1", "ada");
        return db;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("the first read loads from the db, the second is a hit", () -> {
            for (Store s : List.of(new CacheAside(seeded()), new WriteThrough(seeded()))) {
                var db = ((CachedStore) s).db;
                eq(s.read("user:1"), Optional.of("ada"));
                eq(s.read("user:1"), Optional.of("ada"));
                eq(db.reads, 1);
                eq(s.cached("user:1"), Optional.of("ada"));
            }
        });
        test("cache-aside: a write invalidates instead of updating", () -> {
            var db = seeded();
            var s = new CacheAside(db);
            s.read("user:1");
            s.write("user:1", "grace");
            eq(s.cached("user:1"), Optional.empty());
            eq(s.read("user:1"), Optional.of("grace"));
            eq(db.reads, 2);
            eq(db.writes, 1);
        });
        test("cache-aside: writing an unread key caches nothing", () -> {
            var s = new CacheAside(seeded());
            s.write("user:2", "linus");
            eq(s.cached("user:2"), Optional.empty());
        });
        test("write-through: a write fills the cache, the next read is a hit", () -> {
            var db = seeded();
            var s = new WriteThrough(db);
            s.write("user:2", "linus");
            eq(s.cached("user:2"), Optional.of("linus"));
            eq(s.read("user:2"), Optional.of("linus"));
            eq(db.reads, 0);
            eq(db.writes, 1);
        });
        test("a missing row is not cached", () -> {
            for (Store s : List.of(new CacheAside(seeded()), new WriteThrough(seeded()))) {
                var db = ((CachedStore) s).db;
                eq(s.read("nobody"), Optional.empty());
                eq(s.read("nobody"), Optional.empty());
                eq(db.reads, 2);
                eq(s.cached("nobody"), Optional.empty());
            }
        });
        test("cache-aside: a failed db write leaves cache and db agreeing", () -> {
            var db = seeded();
            var s = new CacheAside(db);
            s.read("user:1");
            db.failNextWrite = true;
            raises(IllegalStateException.class, () -> s.write("user:1", "grace"), "db down");
            eq(s.cached("user:1"), Optional.of("ada"));
            eq(db.rows.get("user:1"), "ada");
        });
        test("write-through: a failed db write never reaches the cache", () -> {
            var db = seeded();
            var s = new WriteThrough(db);
            s.read("user:1");
            db.failNextWrite = true;
            raises(IllegalStateException.class, () -> s.write("user:1", "grace"), "db down");
            eq(s.cached("user:1"), Optional.of("ada"));
            db.failNextWrite = true;
            raises(IllegalStateException.class, () -> s.write("user:9", "eve"));
            eq(s.cached("user:9"), Optional.empty());
        });
        test("both: a read after a write sees the new value", () -> {
            for (Store s : List.of(new CacheAside(seeded()), new WriteThrough(seeded()))) {
                s.read("user:1");
                s.write("user:1", "grace");
                eq(s.read("user:1"), Optional.of("grace"));
            }
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
