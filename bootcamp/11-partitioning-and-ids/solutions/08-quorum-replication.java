// ─────────────────────────────────────────────────────────────────────────
//  08 · quorum reads and writes — SOLUTION                  ★★★ stretch
//  concepts: replication · R + W > N · versions · read repair
//  run: java -cp ../../_lib/out 08-quorum-replication.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  With N copies, a write that waits for W acknowledgements and a read
//  that asks R replicas are guaranteed to overlap in at least one replica
//  when R + W > N (pigeonhole: the two sets have R + W members among N
//  slots). That one shared replica holds the latest version, and taking
//  the highest version among the replies finds it. Cassandra's QUORUM
//  (R = W = majority) and Dynamo's tunable N/R/W are this inequality.
//  Choose R + W <= N (say R = W = 1) and you get lower latency and higher
//  availability, but a read can miss the last write entirely. The tests
//  make that visible by having reads contact the lowest-numbered reachable
//  replicas, so the stale case is deterministic instead of unlucky.
//  Versions, not arrival order, decide what is newest: a replica list is
//  not a timeline. Real systems use timestamps or vector clocks (module
//  13); a cluster-wide counter is the simplest stand-in.
//  Two failure rules matter. A write that cannot reach W replicas must
//  fail BEFORE storing anything here (real systems may leave partial
//  writes behind, which is why reads must tolerate them). And a read that
//  cannot reach R replicas fails rather than quietly returning less.
//  Read repair is how replicas converge without a background job: the
//  read already knows which contacted replicas are behind, so it writes
//  the newest version back to them. Only contacted replicas are repaired.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Versioned(String value, long version) {}

    static final class Cluster {
        private final int n;
        private final int r;
        private final int w;
        private final List<Map<String, Versioned>> replicas = new ArrayList<>();
        private long lastVersion;

        Cluster(int n, int r, int w) {
            if (r < 1 || r > n || w < 1 || w > n) {
                throw new IllegalArgumentException("need 1 <= r, w <= n: n=" + n + " r=" + r + " w=" + w);
            }
            this.n = n;
            this.r = r;
            this.w = w;
            for (int i = 0; i < n; i++) replicas.add(new HashMap<>());
        }

        static boolean isStrong(int n, int r, int w) {
            return r + w > n;
        }

        long write(String key, String value, Set<Integer> reachable) {
            checkIndexes(reachable);
            if (reachable.size() < w) {
                throw new IllegalStateException("only " + reachable.size() + " of " + w + " replicas reachable");
            }
            var stamped = new Versioned(value, ++lastVersion);
            for (int i : reachable) replicas.get(i).put(key, stamped);
            return stamped.version();
        }

        Optional<String> read(String key, Set<Integer> reachable) {
            checkIndexes(reachable);
            if (reachable.size() < r) {
                throw new IllegalStateException("only " + reachable.size() + " of " + r + " replicas reachable");
            }
            var contacted = reachable.stream().sorted().limit(r).toList();
            Versioned newest = null;
            for (int i : contacted) {
                var v = replicas.get(i).get(key);
                if (v != null && (newest == null || v.version() > newest.version())) newest = v;
            }
            if (newest == null) return Optional.empty();
            for (int i : contacted) {
                var v = replicas.get(i).get(key);
                if (v == null || v.version() < newest.version()) replicas.get(i).put(key, newest);
            }
            return Optional.of(newest.value());
        }

        Optional<Versioned> stored(int replica, String key) {
            return Optional.ofNullable(replicas.get(replica).get(key));
        }

        private void checkIndexes(Set<Integer> reachable) {
            for (int i : reachable) {
                if (i < 0 || i >= n) throw new IllegalArgumentException("no replica " + i);
            }
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("R + W > N is the overlap rule", () -> {
            eq(Cluster.isStrong(3, 2, 2), true);
            eq(Cluster.isStrong(3, 1, 2), false);
            eq(Cluster.isStrong(3, 1, 3), true);
            eq(Cluster.isStrong(5, 2, 3), false);
            eq(Cluster.isStrong(5, 3, 3), true);
        });
        test("refuses quorums outside 1..N and unknown replicas", () -> {
            raises(IllegalArgumentException.class, () -> new Cluster(3, 0, 2));
            raises(IllegalArgumentException.class, () -> new Cluster(3, 2, 4));
            var c = new Cluster(3, 1, 1);
            raises(IllegalArgumentException.class, () -> c.write("x", "a", Set.of(3)));
            raises(IllegalArgumentException.class, () -> c.read("x", Set.of(-1)));
        });
        test("a write lands on every reachable replica", () -> {
            var c = new Cluster(3, 2, 2);
            eq(c.write("x", "a", Set.of(0, 1, 2)), 1);
            for (int i = 0; i < 3; i++) eq(c.stored(i, "x"), Optional.of(new Versioned("a", 1)));
            eq(c.write("y", "b", Set.of(0, 2)), 2);
            eq(c.stored(1, "y"), Optional.empty());
        });
        test("a write that cannot reach W replicas stores nothing", () -> {
            var c = new Cluster(3, 2, 2);
            raises(IllegalStateException.class, () -> c.write("x", "a", Set.of(2)));
            eq(c.stored(2, "x"), Optional.empty());
            eq(c.write("x", "b", Set.of(0, 1)), 1);
        });
        test("with R + W > N every read sees the latest write", () -> {
            var c = new Cluster(3, 2, 2);
            c.write("x", "a", Set.of(0, 1, 2));
            c.write("x", "b", Set.of(0, 1));
            eq(c.read("x", Set.of(1, 2)), Optional.of("b"));
            eq(c.read("x", Set.of(0, 2)), Optional.of("b"));
        });
        test("with R + W <= N a read can be stale", () -> {
            var c = new Cluster(3, 1, 1);
            c.write("x", "a", Set.of(0, 1, 2));
            c.write("x", "b", Set.of(0));
            eq(c.read("x", Set.of(1, 2)), Optional.of("a"));
            eq(c.read("x", Set.of(0, 1, 2)), Optional.of("b"));
        });
        test("the highest version wins, not the lowest replica", () -> {
            var c = new Cluster(3, 3, 1);
            c.write("x", "a", Set.of(0, 1, 2));
            c.write("x", "b", Set.of(2));
            eq(c.read("x", Set.of(0, 1, 2)), Optional.of("b"));
        });
        test("read repair fixes only the replicas it contacted", () -> {
            var c = new Cluster(4, 2, 2);
            c.write("x", "a", Set.of(0, 1));
            eq(c.read("x", Set.of(1, 2, 3)), Optional.of("a"));
            eq(c.stored(2, "x"), Optional.of(new Versioned("a", 1)));
            eq(c.stored(3, "x"), Optional.empty());
            c.write("x", "b", Set.of(2, 3));
            eq(c.read("x", Set.of(0, 2)), Optional.of("b"));
            eq(c.stored(0, "x"), Optional.of(new Versioned("b", 2)));
            eq(c.stored(1, "x"), Optional.of(new Versioned("a", 1)));
        });
        test("a read needs R replicas and may find nothing", () -> {
            var c = new Cluster(3, 2, 2);
            raises(IllegalStateException.class, () -> c.read("x", Set.of(1)));
            eq(c.read("missing", Set.of(0, 1)), Optional.empty());
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
