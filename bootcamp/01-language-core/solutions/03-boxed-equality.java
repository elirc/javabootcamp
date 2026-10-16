// ─────────────────────────────────────────────────────────────────────────
//  03 · two ids that look equal — SOLUTION                  ★★☆ core
//  concepts: Integer vs int · == vs equals · unboxing null · Map.getOrDefault
//  run: java -cp ../../_lib/out 03-boxed-equality.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Integer is an object, so == compares references. It APPEARS to work in
//  small tests because Integer.valueOf (what autoboxing calls) caches the
//  values -128..127 — two boxed 7s are literally the same object. Two
//  boxed 1000s are not, and `a == b` is false. That is the whole trap:
//  code that passes every hand-written test with small ids and fails in
//  production the day ids pass 127.
//  `a.equals(b)` compares values; it needs a non-null `a`. Here the rule
//  also says two nulls are not a match, so the explicit null check comes
//  first and then equals. (Objects.equals(a, b) would say null == null is
//  a match, which is the wrong rule for THIS domain — read the spec.)
//  loansFor has the mirror-image trap: `int n = loans.get(id);` compiles,
//  and when the key is missing get returns null, and auto-UNBOXING null
//  throws NullPointerException on a line with no visible dereference.
//  getOrDefault(id, 0) says what you mean in one call.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static boolean sameMember(Integer a, Integer b) {
        return a != null && b != null && a.equals(b);
    }

    static int loansFor(Map<Integer, Integer> loans, int memberId) {
        return loans.getOrDefault(memberId, 0);
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
