// ─────────────────────────────────────────────────────────────────────────
//  03 · a table that lines up — SOLUTION                    ★☆☆ warm-up
//  concepts: String.format · width and precision · Locale.ROOT · grouping
//  run: java -cp ../../_lib/out 03-fixed-width-table.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Format specifiers are %[flags][width][.precision]conversion. "%-10s"
//  left-aligns a string in 10 columns, and adding a precision to a string
//  conversion TRUNCATES it: "%-10.10s" pads to 10 and cuts at 10. "%4d"
//  right-aligns an int in 4; "%8.2f" right-aligns a number in 8 with 2
//  decimals; "%,d" inserts grouping separators.
//  The locale is the trap. String.format(fmt, ...) uses the default
//  locale, so on a German machine 2.5 prints as "2,50" and 1234567 groups
//  as "1.234.567" — your report changes depending on where it runs, and a
//  test that passed on your laptop fails in CI. Pass Locale.ROOT for
//  anything machine-readable or test-pinned; pass the user's locale on
//  purpose when the output is for a human in that locale.
//  The tests switch the default locale to Germany for a moment (and put
//  it back in a finally) to prove the output does not move.
//  total keeps money in integer cents: "%,d.%02d" with cents / 100 and
//  cents % 100 — no double ever touches it. (Prices here are doubles only
//  because this exercise is about formatting; module 07 does money
//  properly.)

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static String row(String name, int qty, double price) {
        return String.format(Locale.ROOT, "%-10.10s|%4d|%8.2f", name, qty, price);
    }

    static String total(long cents) {
        return String.format(Locale.ROOT, "%,d.%02d", cents / 100, cents % 100);
    }

    static String inGermany(java.util.function.Supplier<String> body) {
        var saved = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
        try {
            return body.get();
        } finally {
            Locale.setDefault(saved);
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("pads each column", () -> eq(row("Tea", 3, 2.5), "Tea       |   3|    2.50"));
        test("cuts a long name at ten characters", () -> eq(row("Earl Grey Tea", 12, 10), "Earl Grey |  12|   10.00"));
        test("a row is always 24 characters", () -> eq(row("x", 1000, 99999.99).length(), 24));
        test("row ignores the machine's locale", () -> eq(inGermany(() -> row("Tea", 3, 2.5)), "Tea       |   3|    2.50"));
        test("total groups thousands", () -> eq(total(123456789), "1,234,567.89"));
        test("total of a few cents", () -> eq(total(5), "0.05"));
        test("total ignores the machine's locale", () -> eq(inGermany(() -> total(123456789)), "1,234,567.89"));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
