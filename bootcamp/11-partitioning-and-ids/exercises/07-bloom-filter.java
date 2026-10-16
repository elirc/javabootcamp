// ─────────────────────────────────────────────────────────────────────────
//  07 · bloom filter                                        ★★★ stretch
//  concepts: probabilistic sets · double hashing · BitSet · false positives
//  run: java -cp ../../_lib/out 07-bloom-filter.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Cassandra and LevelDB check a Bloom filter before reading a file from
//  disk. Finish the nested class Bloom(bits, hashes) (either < 1 →
//  IllegalArgumentException). With h = hash64(key), h1 = (int) h and
//  h2 = (int) (h >>> 32), positions(key) is, for i = 0..hashes-1,
//  Math.floorMod(h1 + i * h2, bits) computed in int arithmetic (overflow
//  wraps). add(key) sets those bits; mightContain(key) is true only if all
//  are set; bitsSet() counts set bits. Static optimalHashes(bits, items) =
//  Math.round(bits / items * ln 2) as a double division, at least 1
//  (items < 1 → IllegalArgumentException). Static falsePositiveRate(bits,
//  hashes, items) = (1 - e^(-hashes * items / bits)) ^ hashes.
//
//      var f = new Bloom(1000, 4);  f.add("apple");
//      f.mightContain("apple")           → true   (always, once added)
//      f.mightContain("banana")          → false  (almost always)
//      optimalHashes(10_000, 1_000)      → 7
//      falsePositiveRate(10_000, 7, 1_000) → about 0.0082
//
//  hint: two hashes are enough; Kirsch and Mitzenmacher showed why.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    /** FNV-1a 64 followed by the splitmix64 finalizer: a stable, well-spread string hash. */
    static long hash64(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        h ^= h >>> 30;
        h *= 0xbf58476d1ce4e5b9L;
        h ^= h >>> 27;
        h *= 0x94d049bb133111ebL;
        return h ^ (h >>> 31);
    }

    static final class Bloom {
        private final int bits;
        private final int hashes;
        private final BitSet set;

        Bloom(int bits, int hashes) {
            throw new UnsupportedOperationException("TODO");
        }

        int[] positions(String key) {
            throw new UnsupportedOperationException("TODO");
        }

        void add(String key) {
            throw new UnsupportedOperationException("TODO");
        }

        boolean mightContain(String key) {
            throw new UnsupportedOperationException("TODO");
        }

        int bitsSet() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static int optimalHashes(int bits, int items) {
        throw new UnsupportedOperationException("TODO");
    }

    static double falsePositiveRate(int bits, int hashes, int items) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("an empty filter contains nothing", () -> {
            var f = new Bloom(1000, 4);
            eq(f.mightContain("apple"), false);
            eq(f.bitsSet(), 0);
        });
        test("positions are double-hashed indexes", () -> {
            eq(new Bloom(1000, 4).positions("apple"), new int[] {449, 574, 995, 120});
            eq(new Bloom(97, 3).positions("user-1"), new int[] {11, 54, 62});
        });
        test("an added key is always reported", () -> {
            var f = new Bloom(10_000, 7);
            for (int i = 0; i < 1000; i++) f.add("user-" + i);
            for (int i = 0; i < 1000; i++) ok(f.mightContain("user-" + i), "lost user-" + i);
        });
        test("adding sets the key's bits once", () -> {
            var f = new Bloom(1000, 4);
            f.add("apple");
            var distinct = new HashSet<Integer>();
            for (int p : f.positions("apple")) distinct.add(p);
            eq(f.bitsSet(), distinct.size());
            f.add("apple");
            eq(f.bitsSet(), distinct.size());
        });
        test("a key with any bit unset is definitely absent", () -> {
            var f = new Bloom(1000, 4);
            f.add("apple");
            eq(f.mightContain("apple"), true);
            eq(f.mightContain("banana"), false);
            eq(f.mightContain("Apple"), false);
        });
        test("the measured false-positive rate is near the formula", () -> {
            var f = new Bloom(10_000, 7);
            for (int i = 0; i < 1000; i++) f.add("user-" + i);
            int fp = 0;
            for (int i = 0; i < 10_000; i++) if (f.mightContain("ghost-" + i)) fp++;
            eq(fp, 85);
            ok(fp > 20 && fp < 200, "false positives: " + fp);
        });
        test("the optimal hash count is bits per item times ln 2", () -> {
            eq(optimalHashes(10_000, 1_000), 7);
            eq(optimalHashes(10_000, 1_500), 5);
            eq(optimalHashes(100, 1_000), 1);
            raises(IllegalArgumentException.class, () -> optimalHashes(100, 0));
        });
        test("the false-positive formula", () -> {
            approx(falsePositiveRate(10_000, 7, 1_000), 0.008193, 1e-5);
            approx(falsePositiveRate(1_000, 1, 1_000), 1 - Math.exp(-1), 1e-12);
            approx(falsePositiveRate(1_000, 4, 0), 0.0, 1e-12);
        });
        test("refuses empty sizes", () -> {
            raises(IllegalArgumentException.class, () -> new Bloom(0, 3));
            raises(IllegalArgumentException.class, () -> new Bloom(100, 0));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
