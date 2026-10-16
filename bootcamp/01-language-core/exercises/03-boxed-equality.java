// ─────────────────────────────────────────────────────────────────────────
//  03 · two ids that look equal                             ★★☆ core
//  concepts: Integer vs int · == vs equals · unboxing null · Map.getOrDefault
//  run: java -cp ../../_lib/out 03-boxed-equality.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A library tracker keeps member ids as Integer (they come out of maps and
//  may be missing). Write sameMember: true when both ids are present and
//  hold the same number; a missing id never matches anything, not even
//  another missing id. Then write loansFor: how many loans the map records
//  for a member, 0 when the member is not in the map.
//
//      sameMember(7, 7)          → true
//      sameMember(1000, 1000)    → true      (two separate Integer objects!)
//      sameMember(null, null)    → false
//      loansFor({7=2}, 7)        → 2
//      loansFor({7=2}, 8)        → 0
//
//  hint: == on two Integers asks "same object?", not "same number?".

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static boolean sameMember(Integer a, Integer b) {
        throw new UnsupportedOperationException("TODO");
    }

    static int loansFor(Map<Integer, Integer> loans, int memberId) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("equal small ids match", () -> eq(sameMember(7, 7), true));
        test("equal large ids match", () -> eq(sameMember(Integer.valueOf(1000), Integer.valueOf(1000)), true));
        test("different ids do not match", () -> eq(sameMember(7, 8), false));
        test("a missing id never matches", () -> {
            eq(sameMember(null, 5), false);
            eq(sameMember(5, null), false);
        });
        test("two missing ids are not a match", () -> eq(sameMember(null, null), false));
        test("loansFor reads a member's count", () -> eq(loansFor(Map.of(7, 2, 9, 5), 9), 5));
        test("loansFor is zero for an unknown member", () -> eq(loansFor(Map.of(7, 2), 8), 0));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
