// ─────────────────────────────────────────────────────────────────────────
//  03 · memtable and sorted runs — SOLUTION                 ★★☆ core
//  concepts: LSM tree · memtable flush · tombstones · compaction
//  run: java -cp ../../_lib/out 03-lsm-tree.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A log-structured merge tree (LevelDB, RocksDB, Cassandra, HBase) never
//  updates a file in place. Writes go to a small sorted in-memory table;
//  when it fills, it is written out whole as an immutable sorted run
//  (an SSTable), and the memtable starts empty. Writes stay cheap and
//  sequential; the price is paid on reads and in background compaction.
//  A read therefore looks in the newest place first: memtable, then runs
//  from newest to oldest, and the FIRST entry found is the answer, even
//  if that entry is a tombstone. That is why delete must write a marker
//  instead of removing anything: the old value still sits in an older
//  run, and only a newer tombstone can hide it. Removing the key from
//  the memtable alone would make the old value reappear.
//  Compaction merges runs oldest-first into one map, so newer entries
//  overwrite older ones, and only then drops tombstones: with every run
//  merged nothing older is left for a tombstone to hide. (Real systems
//  compacting only SOME runs must keep tombstones, which is a classic
//  source of "deleted data came back" bugs.)
//  Optional<String> as the stored value gives the tombstone a type:
//  Optional.empty() is "deleted here", a missing key is "not here".

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
            write(key, Optional.of(value));
        }

        void delete(String key) {
            write(key, Optional.empty());
        }

        private void write(String key, Optional<String> value) {
            memtable.put(key, value);
            if (memtable.size() >= memtableLimit) {
                runs.add(new TreeMap<>(memtable));
                memtable.clear();
            }
        }

        Optional<String> get(String key) {
            if (memtable.containsKey(key)) return memtable.get(key);
            for (int i = runs.size() - 1; i >= 0; i--) {
                var run = runs.get(i);
                if (run.containsKey(key)) return run.get(key);
            }
            return Optional.empty();
        }

        void compact() {
            var merged = new TreeMap<String, Optional<String>>();
            for (var run : runs) merged.putAll(run);
            merged.values().removeIf(Optional::isEmpty);
            runs.clear();
            if (!merged.isEmpty()) runs.add(merged);
        }

        List<String> scan(String from, String toExclusive) {
            var view = new TreeMap<String, Optional<String>>();
            for (var run : runs) view.putAll(run.subMap(from, toExclusive));
            view.putAll(memtable.subMap(from, toExclusive));
            var out = new ArrayList<String>();
            view.forEach((k, v) -> v.ifPresent(value -> out.add(k + "=" + value)));
            return out;
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
