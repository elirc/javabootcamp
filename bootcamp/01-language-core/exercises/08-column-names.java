// ─────────────────────────────────────────────────────────────────────────
//  08 · spreadsheet column names                            ★★★ stretch
//  concepts: char arithmetic · StringBuilder · base conversion · validation
//  run: java -cp ../../_lib/out 08-column-names.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An expense tracker exports to a spreadsheet and must name its columns
//  the way spreadsheets do. Write columnName (1 → "A", 26 → "Z", 27 → "AA")
//  and its inverse columnNumber, which is case-insensitive. Both refuse
//  nonsense with IllegalArgumentException: a number below 1, or a name
//  that is empty or contains anything but letters.
//
//      columnName(28)        → "AB"
//      columnName(702)       → "ZZ"
//      columnName(703)       → "AAA"
//      columnNumber("az")    → 52
//      columnNumber("A1")    → IllegalArgumentException
//
//  hint: this is base 26 with no zero digit — shift by one before each %.

import static bootcamp.Check.*;

class Exercise {

    static String columnName(int number) {
        throw new UnsupportedOperationException("TODO");
    }

    static int columnNumber(String name) {
        throw new UnsupportedOperationException("TODO");
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
