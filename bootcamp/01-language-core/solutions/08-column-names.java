// ─────────────────────────────────────────────────────────────────────────
//  08 · spreadsheet column names — SOLUTION                 ★★★ stretch
//  concepts: char arithmetic · StringBuilder · base conversion · validation
//  run: java -cp ../../_lib/out 08-column-names.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  It looks like base 26, and it nearly is: in base 26 the digits are 0-25,
//  but column letters run 1-26 (A=1 ... Z=26) with no zero. Plain
//  `n % 26` therefore maps 26 to 0 and produces "A@" or skips Z. The fix
//  is to subtract one BEFORE taking each digit: n--, letter = 'A' + n % 26,
//  n /= 26. Letters come out least-significant first, so build them in a
//  StringBuilder and reverse() at the end.
//  `(char) ('A' + k)` is char arithmetic: 'A' is the int 65, the sum is an
//  int, and the cast turns it back into a char. That is fine; Java chars
//  are UTF-16 code units and A-Z are contiguous.
//  columnNumber is the easy direction: Horner's rule, n = n * 26 + digit,
//  with digit = letter - 'A' + 1 after uppercasing. Character.toUpperCase
//  is locale-independent (String.toUpperCase() is not).
//  The round-trip test over 1..2000 is the cheapest way to catch an
//  off-by-one at every boundary at once — a pattern worth stealing for
//  any encode/decode pair you write.

import static bootcamp.Check.*;

class Exercise {

    static String columnName(int number) {
        if (number < 1) throw new IllegalArgumentException("column numbers start at 1: " + number);
        var letters = new StringBuilder();
        int n = number;
        while (n > 0) {
            n--;
            letters.append((char) ('A' + n % 26));
            n /= 26;
        }
        return letters.reverse().toString();
    }

    static int columnNumber(String name) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("empty column name");
        int n = 0;
        for (int i = 0; i < name.length(); i++) {
            char c = Character.toUpperCase(name.charAt(i));
            if (c < 'A' || c > 'Z') throw new IllegalArgumentException("not a column name: " + name);
            n = n * 26 + (c - 'A' + 1);
        }
        return n;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("single letters", () -> {
            eq(columnName(1), "A");
            eq(columnName(26), "Z");
        });
        test("rolls over to two letters", () -> {
            eq(columnName(27), "AA");
            eq(columnName(28), "AB");
        });
        test("gets the Z boundaries right", () -> {
            eq(columnName(52), "AZ");
            eq(columnName(702), "ZZ");
            eq(columnName(703), "AAA");
        });
        test("refuses zero and negatives", () -> {
            raises(IllegalArgumentException.class, () -> columnName(0));
            raises(IllegalArgumentException.class, () -> columnName(-4));
        });
        test("reads names back", () -> {
            eq(columnNumber("A"), 1);
            eq(columnNumber("AA"), 27);
            eq(columnNumber("ZZ"), 702);
        });
        test("columnNumber is case-insensitive", () -> eq(columnNumber("az"), 52));
        test("columnNumber refuses non-letters and empty names", () -> {
            raises(IllegalArgumentException.class, () -> columnNumber("A1"));
            raises(IllegalArgumentException.class, () -> columnNumber(""));
        });
        test("round-trips every column up to 2000", () -> {
            for (int n = 1; n <= 2000; n++) eq(columnNumber(columnName(n)), n);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
