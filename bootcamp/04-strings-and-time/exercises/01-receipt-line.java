// ─────────────────────────────────────────────────────────────────────────
//  01 · a receipt line                                      ★☆☆ warm-up
//  concepts: StringBuilder · String.repeat · cents to text · sign handling
//  run: java -cp ../../_lib/out 01-receipt-line.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An expense tracker prints receipts. Write formatCents: whole cents as
//  text with exactly two decimals (never via double). Then write
//  receiptLine: the item on the left, the price on the right, dots between,
//  exactly `width` characters long. There is always at least one dot; when
//  the item is too long, cut it short to make room.
//
//      formatCents(250)                    → "2.50"
//      formatCents(-5)                     → "-0.05"
//      receiptLine("Milk", 250, 16)        → "Milk........2.50"
//      receiptLine("Chocolate", 450, 10)   → "Choco.4.50"
//
//  hint: -5 / 100 is 0, and 0 has no sign.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static String formatCents(int cents) {
        throw new UnsupportedOperationException("TODO");
    }

    static String receiptLine(String item, int cents, int width) {
        throw new UnsupportedOperationException("TODO");
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
