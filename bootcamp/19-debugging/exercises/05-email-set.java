// ─────────────────────────────────────────────────────────────────────────
//  05 · the newsletter went out twice                       ★★☆ core
//  concepts: debugging · equals · hashCode · overload vs override
//  run: java -cp ../../_lib/out 05-email-set.java
// ─────────────────────────────────────────────────────────────────────────
//
//  DEBUG HUNT: the code below is complete and compiles; it has bugs. Run
//  it, read the failing tests, find and fix the bugs. Do not edit tests.
//  Intended: two Email values are equal when their addresses match
//  ignoring case and surrounding spaces, everywhere Java compares
//  objects: equals, HashSet, HashMap keys, List.contains.
//  uniqueRecipients(raw) counts distinct addresses; isSubscribed(list,
//  address) says whether the address is in the list.
//  Bug reports:
//    "Ada@X.com and ada@x.com each got the newsletter."
//    "Unsubscribe is broken: isSubscribed says false for an address we
//     can see in the list."
//
//      uniqueRecipients(["ada@x.com", "Ada@X.com", " ada@x.com "]) → 1
//      isSubscribed([Email("Bob@y.org")], "bob@y.org")            → true
//
//  hint: for each failing check, ask which method Java actually calls.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class Email {
        private final String address;

        Email(String address) {
            this.address = address.trim();
        }

        public boolean equals(Email other) {
            return other != null && address.equalsIgnoreCase(other.address);
        }

        @Override
        public int hashCode() {
            return address.hashCode();
        }

        @Override
        public String toString() {
            return address;
        }
    }

    static int uniqueRecipients(List<String> raw) {
        var set = new HashSet<Email>();
        for (var r : raw) set.add(new Email(r));
        return set.size();
    }

    static boolean isSubscribed(Collection<Email> list, String address) {
        return list.contains(new Email(address));
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("equal ignoring case and spaces, also through Object", () -> {
            var a = new Email("A@x.io");
            var b = new Email(" a@X.IO ");
            ok(a.equals(b), "a.equals(b)");
            Object o = b;
            ok(a.equals(o), "a.equals((Object) b)");
        });
        test("equal emails have equal hash codes", () -> {
            eq(new Email("Ada@X.com").hashCode(), new Email("ada@x.com ").hashCode());
        });
        test("a set keeps one per address", () -> {
            eq(uniqueRecipients(List.of("ada@x.com", "Ada@X.com", " ada@x.com ")), 1);
            eq(uniqueRecipients(List.of("ada@x.com", "bob@x.com", "BOB@X.COM")), 2);
        });
        test("contains finds an address written differently", () -> {
            ok(isSubscribed(List.of(new Email("Bob@y.org")), "bob@y.org"), "found in a List");
            ok(isSubscribed(Set.of(new Email("bob@y.org")), "BOB@Y.ORG "), "found in a Set");
        });
        test("different addresses stay different", () -> {
            var a = new Email("a@x.io");
            ok(!a.equals(new Email("b@x.io")), "different address");
            ok(!a.equals((Object) "a@x.io"), "not equal to a String");
            ok(!a.equals((Object) null), "not equal to null");
            ok(!isSubscribed(List.of(a), "b@x.io"), "not subscribed");
        });
        test("works as a HashMap key", () -> {
            var opens = new HashMap<Email, Integer>();
            opens.merge(new Email("Ada@X.com"), 1, Integer::sum);
            opens.merge(new Email("ada@x.com"), 1, Integer::sum);
            eq(opens.size(), 1);
            eq(opens.get(new Email(" ADA@x.com")), 2);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
