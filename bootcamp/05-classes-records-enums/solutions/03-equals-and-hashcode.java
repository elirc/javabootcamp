// ─────────────────────────────────────────────────────────────────────────
//  03 · same book, different dashes — SOLUTION              ★★☆ core
//  concepts: equals · hashCode · HashSet/HashMap contracts · normalisation
//  run: java -cp ../../_lib/out 03-equals-and-hashcode.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  HashSet and HashMap find things in two steps: hashCode picks a bucket,
//  then equals compares within it. The contract: equal objects MUST have
//  equal hash codes. Override only equals and two equal Isbns land in
//  different buckets — contains says false, the set holds "duplicates",
//  and map.get with an equal key returns null. Every test here that uses
//  a set or map catches that.
//  Normalise once, in the constructor, and base both methods on the
//  normalised field. Then equality is cheap and unambiguous: digits.equals
//  (other.digits), and hashCode is digits.hashCode().
//  equals(Object) has a fixed shape: same reference → true; not an Isbn
//  (including null) → false; then compare fields. `o instanceof Isbn
//  other` does the type check, the null check and the cast in one step.
//  The parameter type must be Object: writing equals(Isbn other) is an
//  OVERLOAD, not an override — collections never call it. @Override on
//  both methods turns that mistake into a compile error.
//  Why not a record? A record Isbn(String digits) would get equals and
//  hashCode free, but the compact constructor can only reassign the
//  parameter — which works too. A class is used here so you write the
//  contract by hand once.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class Isbn {
        private final String digits;

        Isbn(String raw) {
            var only = raw.replaceAll("[^0-9]", "");
            if (only.length() != 13) throw new IllegalArgumentException("an ISBN has 13 digits: " + raw);
            this.digits = only;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof Isbn other && digits.equals(other.digits);
        }

        @Override
        public int hashCode() {
            return digits.hashCode();
        }

        @Override
        public String toString() {
            return digits;
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("ignores dashes and spaces", () ->
            eq(new Isbn("978-0-441-17271-9").equals(new Isbn("978 0441172719")), true));
        test("different digits are different", () ->
            eq(new Isbn("9780441172719").equals(new Isbn("9780141439518")), false));
        test("is never equal to null or a String", () -> {
            var isbn = new Isbn("9780441172719");
            eq(isbn.equals(null), false);
            eq(isbn.equals("9780441172719"), false);
        });
        test("equal isbns hash alike", () ->
            eq(new Isbn("978-0-441-17271-9").hashCode(), new Isbn("9780441172719").hashCode()));
        test("a set keeps one copy", () -> {
            var set = new HashSet<Isbn>();
            set.add(new Isbn("978-0-441-17271-9"));
            set.add(new Isbn("9780441172719"));
            eq(set.size(), 1);
        });
        test("a map finds a book by an equal isbn", () -> {
            var titles = new HashMap<Isbn, String>();
            titles.put(new Isbn("978-0-441-17271-9"), "Dune");
            eq(titles.get(new Isbn("9780441172719")), "Dune");
        });
        test("refuses the wrong number of digits", () -> {
            raises(IllegalArgumentException.class, () -> new Isbn("978-0"));
            raises(IllegalArgumentException.class, () -> new Isbn("97804411727190"));
        });
        test("prints as plain digits", () -> eq(new Isbn("978-0-441-17271-9").toString(), "9780441172719"));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
