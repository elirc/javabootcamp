// ─────────────────────────────────────────────────────────────────────────
//  03 · memtable and sorted runs                            ★★☆ core
//  concepts: LSM tree · memtable flush · tombstones · compaction
//  run: java -cp ../../_lib/out 03-lsm-tree.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Build the storage engine behind RocksDB and Cassandra in miniature.
//  Finish Lsm: put and delete write into the memtable (delete writes a
//  TOMBSTONE, Optional.empty()); when the memtable then holds
//  memtableLimit distinct keys it is flushed: copied as a new run
//  appended to runs (oldest first) and cleared. get looks in the
//  memtable, then runs newest to oldest; the first entry found decides
//  (a tombstone means empty). compact merges all runs into one, newer
//  entries winning, drops tombstones, and leaves zero runs if nothing is
//  alive; the memtable is untouched. scan(from, toExclusive) returns
//  live "key=value" strings in key order, merged across every level.
//
//      limit 2: put(a,1) put(b,2)  → 1 run      delete(a) → get(a) empty
//      scan("a", "c")              → [a=1, b=2]  (before the delete)
//
//  hint: a delete must leave something behind, or older runs win.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class Lsm {
        private final int memtableLimit;
        private final TreeMap<String, Optional<String>> memtable = new TreeMap<>();
        private final List<TreeMap<String, Optional<String>>> runs = new ArrayList<>(); // oldest first

        Lsm(int memtableLimit) {
            this.memtableLimit = memtableLimit;
        }

        int runCount() {
            return runs.size();
        }

        int entriesOnDisk() {
            return runs.stream().mapToInt(Map::size).sum();
        }

        void put(String key, String value) {
            throw new UnsupportedOperationException("TODO");
        }

        void delete(String key) {
            throw new UnsupportedOperationException("TODO");
        }

        Optional<String> get(String key) {
            throw new UnsupportedOperationException("TODO");
        }

        void compact() {
            throw new UnsupportedOperationException("TODO");
        }

        List<String> scan(String from, String toExclusive) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("reads its own writes from the memtable", () -> {
            var lsm = new Lsm(3);
            lsm.put("a", "1");
            lsm.put("a", "2");
            eq(lsm.get("a"), Optional.of("2"));
            eq(lsm.get("zz"), Optional.empty());
            eq(lsm.runCount(), 0);
        });
        test("flushes when the memtable holds limit distinct keys", () -> {
            var lsm = new Lsm(2);
            lsm.put("a", "1");
            lsm.put("a", "1b");
            eq(lsm.runCount(), 0);
            lsm.put("b", "2");
            eq(lsm.runCount(), 1);
            eq(lsm.entriesOnDisk(), 2);
            eq(lsm.get("a"), Optional.of("1b"));
        });
        test("a newer run shadows an older one", () -> {
            var lsm = new Lsm(2);
            lsm.put("a", "old");
            lsm.put("b", "x");
            lsm.put("a", "new");
            lsm.put("c", "y");
            eq(lsm.runCount(), 2);
            eq(lsm.get("a"), Optional.of("new"));
            eq(lsm.get("b"), Optional.of("x"));
        });
        test("the memtable shadows every run", () -> {
            var lsm = new Lsm(2);
            lsm.put("a", "disk");
            lsm.put("b", "disk");
            lsm.put("a", "memory");
            eq(lsm.runCount(), 1);
            eq(lsm.get("a"), Optional.of("memory"));
        });
        test("a delete hides an older value, in memory and after a flush", () -> {
            var lsm = new Lsm(2);
            lsm.put("a", "1");
            lsm.put("b", "2");
            lsm.delete("a");
            eq(lsm.get("a"), Optional.empty());
            lsm.put("c", "3");
            eq(lsm.runCount(), 2);
            eq(lsm.get("a"), Optional.empty());
            eq(lsm.entriesOnDisk(), 4);
        });
        test("compaction keeps the newest values and drops tombstones", () -> {
            var lsm = new Lsm(2);
            lsm.put("a", "1");
            lsm.put("b", "2");
            lsm.put("a", "3");
            lsm.delete("b");
            lsm.put("c", "4");
            lsm.put("d", "5");
            lsm.compact();
            eq(lsm.runCount(), 1);
            eq(lsm.entriesOnDisk(), 3);
            eq(lsm.get("a"), Optional.of("3"));
            eq(lsm.get("b"), Optional.empty());
        });
        test("compaction leaves no run when nothing is alive, and keeps the memtable", () -> {
            var lsm = new Lsm(2);
            lsm.put("a", "1");
            lsm.put("b", "2");
            lsm.delete("a");
            lsm.delete("b");
            lsm.put("m", "mem");
            lsm.compact();
            eq(lsm.runCount(), 0);
            eq(lsm.get("m"), Optional.of("mem"));
        });
        test("scan merges every level in key order, end exclusive", () -> {
            var lsm = new Lsm(2);
            lsm.put("d", "1");
            lsm.put("b", "2");
            lsm.put("c", "3");
            lsm.delete("d");
            lsm.put("a", "4");
            lsm.put("b", "5");
            lsm.put("bb", "6");
            eq(lsm.scan("b", "e"), List.of("b=5", "bb=6", "c=3"));
            eq(lsm.scan("a", "c"), List.of("a=4", "b=5", "bb=6"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
