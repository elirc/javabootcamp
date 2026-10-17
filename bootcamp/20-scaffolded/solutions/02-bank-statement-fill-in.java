// ─────────────────────────────────────────────────────────────────────────
//  02 · print a bank statement (fill-in) — SOLUTION         ★☆☆ warm-up
//  concepts: fill-in · long cents · % and / · running totals · String.format
//  run: java -cp ../../_lib/out 02-bank-statement-fill-in.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  The four holes, in order:
//    sign    cents < 0 ? "-" : ""
//    rest    abs % 100
//    balance balance + tx.cents()
//    filter  tx -> tx.cents() < 0
//  The trap is the sign of small negative amounts. -5 / 100 is 0 and
//  -5 % 100 is -5 in Java (the remainder takes the sign of the dividend),
//  so formatting cents / 100 and cents % 100 directly prints "0.-5", and
//  for -350 prints "-3.-50". Working on Math.abs(cents) and putting the
//  sign in front separately is why the starter computes abs first; it
//  also covers -5, where the whole-units part is 0 and carries no sign
//  of its own. Long cents (module 07) keep every amount exact; only the
//  printing turns them into a decimal string.
//  The running balance is updated BEFORE the line is printed, so each
//  line shows the balance after its transaction, and the closing line
//  reuses the final value. totalSpent negates a sum of negatives so the
//  caller gets a positive number.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Tx(String date, String desc, long cents) {}

    static String money(long cents) {
        long abs = Math.abs(cents);
        String sign = cents < 0 ? "-" : "";
        long rest = abs % 100;
        return sign + abs / 100 + "." + String.format("%02d", rest);
    }

    static List<String> statement(long opening, List<Tx> txs) {
        var lines = new ArrayList<String>();
        long balance = opening;
        for (var tx : txs) {
            balance = balance + tx.cents();
            lines.add(String.format("%-10s %-12s %9s %9s", tx.date(), tx.desc(), money(tx.cents()), money(balance)));
        }
        lines.add("closing balance: " + money(balance));
        return lines;
    }

    static long totalSpent(List<Tx> txs) {
        return -txs.stream().filter(tx -> tx.cents() < 0).mapToLong(Tx::cents).sum();
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("money prints two decimals", () -> {
            eq(money(9650), "96.50");
            eq(money(5), "0.05");
            eq(money(0), "0.00");
            eq(money(123456), "1234.56");
        });
        test("money puts a minus in front of a negative amount", () -> {
            eq(money(-350), "-3.50");
            eq(money(-5), "-0.05");
            eq(money(-100), "-1.00");
        });
        test("the statement shows the balance after each transaction", () -> {
            var lines = statement(10000, List.of(new Tx("2024-01-02", "Coffee", -350), new Tx("2024-01-03", "Salary", 250000)));
            eq(lines.get(0), "2024-01-02 Coffee           -3.50     96.50");
            eq(lines.get(1), "2024-01-03 Salary         2500.00   2596.50");
        });
        test("the statement ends with the closing balance", () -> {
            var lines = statement(10000, List.of(new Tx("2024-01-02", "Coffee", -350)));
            eq(lines.size(), 2);
            eq(lines.get(1), "closing balance: 96.50");
            eq(statement(4200, List.of()), List.of("closing balance: 42.00"));
        });
        test("the balance may go negative", () -> {
            var lines = statement(100, List.of(new Tx("2024-02-01", "Rent", -90000)));
            eq(lines.get(1), "closing balance: -899.00");
        });
        test("totalSpent adds up the withdrawals as a positive number", () -> {
            eq(totalSpent(List.of(new Tx("d", "a", -350), new Tx("d", "b", 1000), new Tx("d", "c", -150))), 500);
            eq(totalSpent(List.of(new Tx("d", "b", 1000))), 0);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
