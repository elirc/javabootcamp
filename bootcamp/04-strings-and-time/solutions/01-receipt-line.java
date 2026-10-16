// ─────────────────────────────────────────────────────────────────────────
//  01 · a receipt line — SOLUTION                           ★☆☆ warm-up
//  concepts: StringBuilder · String.repeat · cents to text · sign handling
//  run: java -cp ../../_lib/out 01-receipt-line.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Money stays in integer cents until the last moment, and the last moment
//  is this method: split into whole units (cents / 100) and the remainder
//  (cents % 100), then zero-pad the remainder to two digits.
//  The sign is the trap. For -5 cents, -5 / 100 is 0 and -5 % 100 is -5,
//  so the naive "units + "." + pad(rest)" prints "0.-5" or "0.05" — the
//  minus sign is lost because 0 cannot carry it. Take the sign first,
//  then work with the absolute value. Use a long for that absolute value:
//  Math.abs(Integer.MIN_VALUE) is still negative in int.
//  String.format(Locale.ROOT, "%d.%02d", ...) does the padding; ROOT keeps
//  digits ASCII whatever the machine's locale is.
//  receiptLine is arithmetic on lengths: dots = width - item - price. If
//  that is under 1, shorten the item to width - price - 1 characters.
//  "x".repeat(n) (Java 11+) builds the dots; StringBuilder joins the
//  three parts without creating intermediate strings — for three parts
//  plain + is just as good, and the compiler emits a StringBuilder anyway.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static String formatCents(int cents) {
        long abs = Math.abs((long) cents);
        var sign = cents < 0 ? "-" : "";
        return String.format(Locale.ROOT, "%s%d.%02d", sign, abs / 100, abs % 100);
    }

    static String receiptLine(String item, int cents, int width) {
        var price = formatCents(cents);
        int room = width - price.length() - 1;
        var name = item.length() > room ? item.substring(0, Math.max(0, room)) : item;
        int dots = width - name.length() - price.length();
        return new StringBuilder(name).append(".".repeat(dots)).append(price).toString();
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("formats whole cents with two decimals", () -> {
            eq(formatCents(250), "2.50");
            eq(formatCents(100000), "1000.00");
        });
        test("pads small amounts", () -> {
            eq(formatCents(5), "0.05");
            eq(formatCents(0), "0.00");
        });
        test("keeps the sign of a small refund", () -> {
            eq(formatCents(-5), "-0.05");
            eq(formatCents(-150), "-1.50");
        });
        test("lines up item and price", () -> eq(receiptLine("Milk", 250, 16), "Milk........2.50"));
        test("a line is exactly the width", () -> eq(receiptLine("Tea", 99, 20).length(), 20));
        test("keeps at least one dot", () -> eq(receiptLine("Bread", 450, 10), "Bread.4.50"));
        test("shortens an item that does not fit", () -> eq(receiptLine("Chocolate", 450, 10), "Choco.4.50"));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
