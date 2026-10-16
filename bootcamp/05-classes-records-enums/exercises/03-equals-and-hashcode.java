// ─────────────────────────────────────────────────────────────────────────
//  03 · same book, different dashes                         ★★☆ core
//  concepts: equals · hashCode · HashSet/HashMap contracts · normalisation
//  run: java -cp ../../_lib/out 03-equals-and-hashcode.java
// ─────────────────────────────────────────────────────────────────────────
//
//  ISBNs arrive as "978-0-441-17271-9", "978 0441172719" or plain digits.
//  Finish the nested class Isbn: the constructor keeps only the digits
//  (fewer or more than 13 → IllegalArgumentException); two Isbns are equal
//  when their digits are; equal Isbns must hash alike, so sets and maps
//  treat them as one key; toString is the 13 digits.
//
//      new Isbn("978-0-441-17271-9").equals(new Isbn("9780441172719"))
//                                      → true
//      both added to one HashSet       → size 1
//      new Isbn("978-0")               → IllegalArgumentException
//
//  hint: override BOTH equals(Object) and hashCode, from the same field.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class Isbn {
        private final String digits;

        Isbn(String raw) {
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        public boolean equals(Object o) {
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        public String toString() {
            throw new UnsupportedOperationException("TODO");
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
