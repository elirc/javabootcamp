// ─────────────────────────────────────────────────────────────────────────
//  04 · order ids, anchored — SOLUTION                      ★★☆ core
//  concepts: regex · Pattern/Matcher · matches vs find · groups · \b
//  run: java -cp ../../_lib/out 04-order-ids.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Compile a regex once into a static final Pattern; Pattern.compile is
//  the expensive part, and String.matches(regex) recompiles every call.
//  Matcher.matches() must match the ENTIRE input, so no ^ and $ are
//  needed there. find() looks for the next match ANYWHERE, which is what
//  you want for scanning text and exactly wrong for validating: a
//  validator built on find() accepts "xxORD-2024-000123xx".
//  For scanning, the boundaries move into the pattern: \b is a word
//  boundary (between a word char and a non-word char), so
//  \bORD-\d{4}-\d{6}\b refuses "XORD-..." and a seven-digit sequence —
//  both would otherwise yield a match hiding inside a longer token.
//  Groups pull pieces out: in ORD-(\d{4})-(\d{6}), group(2) is the
//  sequence. Integer.parseInt("000123") is 123; leading zeros are fine.
//  In a Java string literal every regex backslash is doubled: "\\d{4}".
//  (Java 15 text blocks don't change that.) And a classic false friend:
//  "." in a regex is ANY character — a literal dot is "\\.".

import static bootcamp.Check.*;

import java.util.*;
import java.util.regex.*;

class Exercise {

    private static final Pattern ID = Pattern.compile("ORD-(\\d{4})-(\\d{6})");
    private static final Pattern ID_IN_TEXT = Pattern.compile("\\bORD-\\d{4}-\\d{6}\\b");

    static boolean isOrderId(String s) {
        return s != null && ID.matcher(s).matches();
    }

    static int sequenceOf(String id) {
        var m = ID.matcher(id);
        if (!m.matches()) throw new IllegalArgumentException("not an order id: " + id);
        return Integer.parseInt(m.group(2));
    }

    static List<String> findOrderIds(String text) {
        var found = new ArrayList<String>();
        var m = ID_IN_TEXT.matcher(text);
        while (m.find()) found.add(m.group());
        return found;
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
