// ─────────────────────────────────────────────────────────────────────────
//  01 · bounded max and clamp                               ★☆☆ warm-up
//  concepts: bounded type parameters · Comparable<? super T> · stable sort
//  run: java -cp ../../_lib/out 01-bounded-max.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Three generic helpers with the bound T extends Comparable<? super T>,
//  so they accept Integer, String, and also Beta, which inherits compareTo
//  from Version. Finish them:
//
//    max(items)        the largest item; on equal maxima, the FIRST one
//                      met; an empty collection throws
//                      IllegalArgumentException
//    clamp(v, lo, hi)  v pinned into [lo, hi], both ends inclusive; lo
//                      above hi throws IllegalArgumentException
//    topN(items, n)    a new list of the n largest, largest first, equal
//                      items in the order met; n above the size gives
//                      them all; n < 0 throws IllegalArgumentException
//
//      max(List.of(3, 9, 4))         → 9
//      clamp(42, 1, 10)              → 10
//      topN(List.of(5, 1, 7, 3), 2)  → [7, 5]
//
//  hint: List.sort is stable, and Comparator has a reverseOrder().

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    // A release version, ordered by major then minor. The label is only a
    // name: it takes no part in the ordering.
    static class Version implements Comparable<Version> {
        final int major;
        final int minor;
        final String label;

        Version(int major, int minor, String label) {
            this.major = major;
            this.minor = minor;
            this.label = label;
        }

        @Override
        public int compareTo(Version other) {
            int byMajor = Integer.compare(major, other.major);
            return byMajor != 0 ? byMajor : Integer.compare(minor, other.minor);
        }

        @Override
        public String toString() {
            return label;
        }
    }

    // A Beta IS a Version: it inherits Comparable<Version>, not Comparable<Beta>.
    static final class Beta extends Version {
        Beta(int major, int minor, String label) {
            super(major, minor, label);
        }
    }

    static <T extends Comparable<? super T>> T max(Collection<? extends T> items) {
        throw new UnsupportedOperationException("TODO");
    }

    static <T extends Comparable<? super T>> T clamp(T value, T lo, T hi) {
        throw new UnsupportedOperationException("TODO");
    }

    static <T extends Comparable<? super T>> List<T> topN(Collection<? extends T> items, int n) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("max finds the largest Integer and the largest String", () -> {
            eq(max(List.of(3, 9, 4)), 9);
            eq(max(Set.of("pear", "apple", "zoo")), "zoo");
        });
        test("max of an empty collection throws IllegalArgumentException", () ->
            raises(IllegalArgumentException.class, () -> max(List.<Integer>of())));
        test("max returns the first of equal maxima", () -> {
            var first = new Version(2, 0, "first");
            var second = new Version(2, 0, "second");
            ok(max(List.of(new Version(1, 9, "old"), first, second)) == first);
        });
        test("max works on Betas, whose Comparable comes from Version", () -> {
            List<Beta> betas = List.of(new Beta(1, 2, "1.2-beta"), new Beta(1, 10, "1.10-beta"), new Beta(0, 99, "0.99-beta"));
            Beta newest = max(betas);
            eq(newest.label, "1.10-beta");
        });
        test("clamp keeps a value inside the range and pins one outside it", () -> {
            eq(clamp(5, 1, 10), 5);
            eq(clamp(-3, 1, 10), 1);
            eq(clamp(42, 1, 10), 10);
            eq(clamp(10, 1, 10), 10);
            eq(clamp("m", "c", "k"), "k");
        });
        test("clamp rejects lo above hi", () ->
            raises(IllegalArgumentException.class, () -> clamp(5, 10, 1)));
        test("topN gives the n largest, largest first, ties in encounter order", () -> {
            var picked = topN(List.of(new Version(1, 0, "a"), new Version(3, 0, "b"),
                new Version(2, 0, "c"), new Version(3, 0, "d")), 3);
            eq(picked.stream().map(v -> v.label).toList(), List.of("b", "d", "c"));
        });
        test("topN copes with n of zero or above the size, rejects n < 0, and leaves its input alone", () -> {
            var input = new ArrayList<>(List.of(1, 3, 2));
            eq(topN(input, 5), List.of(3, 2, 1));
            eq(topN(input, 0), List.of());
            eq(input, List.of(1, 3, 2));
            raises(IllegalArgumentException.class, () -> topN(input, -1));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
