// ─────────────────────────────────────────────────────────────────────────
//  04 · order ids, anchored                                 ★★☆ core
//  concepts: regex · Pattern/Matcher · matches vs find · groups · \b
//  run: java -cp ../../_lib/out 04-order-ids.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An order management tool uses ids like ORD-2024-000123: "ORD", a dash,
//  a four-digit year, a dash, a six-digit sequence. Write isOrderId (the
//  WHOLE string must be an id), sequenceOf (the sequence number of a valid
//  id; anything else → IllegalArgumentException) and findOrderIds (every
//  id mentioned in free text, in order, ignoring ids glued to other
//  letters or digits).
//
//      isOrderId("ORD-2024-000123")     → true
//      isOrderId(" ORD-2024-000123")    → false
//      sequenceOf("ORD-2024-000123")    → 123
//      findOrderIds("re ORD-2024-000001, ORD-2023-999999.")
//          → [ORD-2024-000001, ORD-2023-999999]
//
//  hint: Matcher.matches() is anchored at both ends; find() is not.

import static bootcamp.Check.*;

import java.util.*;
import java.util.regex.*;

class Exercise {

    static boolean isOrderId(String s) {
        throw new UnsupportedOperationException("TODO");
    }

    static int sequenceOf(String id) {
        throw new UnsupportedOperationException("TODO");
    }

    static List<String> findOrderIds(String text) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("accepts a well-formed id", () -> eq(isOrderId("ORD-2024-000123"), true));
        test("rejects the wrong case and the wrong digit counts", () -> {
            eq(isOrderId("ord-2024-000123"), false);
            eq(isOrderId("ORD-24-000123"), false);
            eq(isOrderId("ORD-2024-0001234"), false);
        });
        test("the whole string must be the id", () -> {
            eq(isOrderId(" ORD-2024-000123"), false);
            eq(isOrderId("ORD-2024-000123 x"), false);
            eq(isOrderId("xxORD-2024-000123"), false);
        });
        test("sequenceOf reads the sequence number", () -> eq(sequenceOf("ORD-2024-000123"), 123));
        test("sequenceOf refuses an invalid id", () ->
            raises(IllegalArgumentException.class, () -> sequenceOf("ORD-2024-12")));
        test("finds every id in free text", () -> eq(
            findOrderIds("re ORD-2024-000001, ORD-2023-999999."),
            List.of("ORD-2024-000001", "ORD-2023-999999")));
        test("ignores ids glued to other characters", () ->
            eq(findOrderIds("XORD-2024-000001 ORD-2024-0000012 ORD-2024-000009"), List.of("ORD-2024-000009")));
        test("no ids in the text is an empty list", () -> eq(findOrderIds("nothing here"), List.of()));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
