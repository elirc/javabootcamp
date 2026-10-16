// ─────────────────────────────────────────────────────────────────────────
//  03 · a table that lines up                               ★☆☆ warm-up
//  concepts: String.format · width and precision · Locale.ROOT · grouping
//  run: java -cp ../../_lib/out 03-fixed-width-table.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An order report prints fixed-width rows. Write row: the name left-
//  aligned in 10 columns (cut at 10), the quantity right-aligned in 4, the
//  unit price right-aligned in 8 with 2 decimals, separated by "|". Then
//  write total: cents as an amount with thousands separators. Both must
//  look the same on every machine, whatever its locale.
//
//      row("Tea", 3, 2.5)          → "Tea       |   3|    2.50"
//      row("Earl Grey Tea", 12, 10) → "Earl Grey |  12|   10.00"
//      total(123456789)            → "1,234,567.89"
//
//  hint: String.format without a Locale uses the machine's default one.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static String row(String name, int qty, double price) {
        throw new UnsupportedOperationException("TODO");
    }

    static String total(long cents) {
        throw new UnsupportedOperationException("TODO");
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
