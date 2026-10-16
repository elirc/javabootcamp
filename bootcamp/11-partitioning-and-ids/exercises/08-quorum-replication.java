// ─────────────────────────────────────────────────────────────────────────
//  08 · quorum reads and writes                             ★★★ stretch
//  concepts: replication · R + W > N · versions · read repair
//  run: java -cp ../../_lib/out 08-quorum-replication.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Dynamo-style stores keep N copies and let you pick R and W. Finish the
//  nested class Cluster(n, r, w) (unless 1 <= r <= n and 1 <= w <= n →
//  IllegalArgumentException); replicas are numbered 0..n-1. Static
//  isStrong(n, r, w) is r + w > n. write(key, value, reachable): fewer
//  than w reachable → IllegalStateException, nothing stored and no
//  version used; otherwise the next version (1, 2, 3... cluster-wide) is
//  stored on EVERY reachable replica and returned. read(key, reachable):
//  fewer than r reachable → IllegalStateException; otherwise contact only
//  the r LOWEST-numbered reachable replicas and return the value with the
//  highest version among them (Optional.empty() if none has the key),
//  then read repair: copy that newest Versioned onto each contacted
//  replica that is missing it or older. A reachable index outside 0..n-1 →
//  IllegalArgumentException. stored(replica, key) peeks at one replica.
//
//      N=3 R=1 W=1:  write("x","a",{0,1,2}), write("x","b",{0})
//      read("x", {1, 2})        → "a"     (stale: R + W <= N)
//      N=3 R=2 W=2:  any read overlaps the last write → "b"
//
//  hint: the newest version wins, wherever it was found.

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
            throw new UnsupportedOperationException("TODO");
        }

        static boolean isStrong(int n, int r, int w) {
            throw new UnsupportedOperationException("TODO");
        }

        long write(String key, String value, Set<Integer> reachable) {
            throw new UnsupportedOperationException("TODO");
        }

        Optional<String> read(String key, Set<Integer> reachable) {
            throw new UnsupportedOperationException("TODO");
        }

        Optional<Versioned> stored(int replica, String key) {
            throw new UnsupportedOperationException("TODO");
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
