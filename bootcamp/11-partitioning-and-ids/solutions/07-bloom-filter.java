// ─────────────────────────────────────────────────────────────────────────
//  07 · bloom filter — SOLUTION                             ★★★ stretch
//  concepts: probabilistic sets · double hashing · BitSet · false positives
//  run: java -cp ../../_lib/out 07-bloom-filter.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A Bloom filter answers "definitely not here" or "maybe here" using a
//  few bits per key. An LSM storage engine keeps one per SSTable: a read
//  for a missing key skips every file whose filter says no, which is most
//  of them. A "maybe" costs one real lookup; a "no" is always right,
//  because adding a key sets all of its bits and bits are never cleared.
//  That is also why a plain Bloom filter cannot delete.
//  k independent hash functions are expensive; double hashing derives
//  them from one 64-bit hash: g_i = h1 + i * h2. Kirsch and Mitzenmacher
//  proved this keeps the same false-positive rate. The traps are Java's:
//  h1 + i * h2 overflows int (fine, it is meant to wrap) and can be
//  negative, so the index needs Math.floorMod; Math.abs would give
//  different positions (and a negative one for MIN_VALUE). And i must
//  start at 0, so the first position is h1 itself.
//  The formulas are the design tool. With m bits, n items and k hashes the
//  false-positive rate is about (1 - e^(-kn/m))^k, minimised at
//  k = (m/n) ln 2. At 10 bits per key that is k = 7 and under 1 percent;
//  the test measures it on 10,000 keys the filter never saw. Integer
//  division in optimalHashes (10_000 / 1_500 = 6) silently changes k,
//  so divide as doubles.

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
            if (bits < 1 || hashes < 1) throw new IllegalArgumentException("bits and hashes must be at least 1");
            this.bits = bits;
            this.hashes = hashes;
            this.set = new BitSet(bits);
        }

        int[] positions(String key) {
            long h = hash64(key);
            int h1 = (int) h;
            int h2 = (int) (h >>> 32);
            var out = new int[hashes];
            for (int i = 0; i < hashes; i++) out[i] = Math.floorMod(h1 + i * h2, bits);
            return out;
        }

        void add(String key) {
            for (int p : positions(key)) set.set(p);
        }

        boolean mightContain(String key) {
            for (int p : positions(key)) {
                if (!set.get(p)) return false;
            }
            return true;
        }

        int bitsSet() {
            return set.cardinality();
        }
    }

    static int optimalHashes(int bits, int items) {
        if (items < 1) throw new IllegalArgumentException("items must be at least 1: " + items);
        return (int) Math.max(1, Math.round((double) bits / items * Math.log(2)));
    }

    static double falsePositiveRate(int bits, int hashes, int items) {
        return Math.pow(1 - Math.exp(-(double) hashes * items / bits), hashes);
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
