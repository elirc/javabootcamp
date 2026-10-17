// ─────────────────────────────────────────────────────────────────────────
//  02 · print a bank statement (fill-in)                    ★☆☆ warm-up
//  concepts: fill-in · long cents · % and / · running totals · String.format
//  run: java -cp ../../_lib/out 02-bank-statement-fill-in.java
// ─────────────────────────────────────────────────────────────────────────
//
//  FILL-IN: the program is complete except for 4 holes. Each hole is a
//  `/* ___ */ hole()` with a comment saying what belongs there; replace
//  the whole `/* ___ */ hole()` with your expression. Nothing else needs
//  to change, and hole() goes away when you are done.
//  Money is in long cents. money(cents) prints "-3.50" style (a minus
//  only for negative amounts, always two decimals). statement(opening,
//  txs) prints one line per transaction with the balance AFTER it, then
//  "closing balance: X". totalSpent(txs) is the sum of the withdrawals
//  (negative transactions), as a positive number.
//
//      money(-5)                              → "-0.05"
//      statement(10000, [Tx(.., -350)]) last  → "closing balance: 96.50"
//
//  hint: for -5 cents, what are Math.abs(-5) / 100 and Math.abs(-5) % 100?

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Tx(String date, String desc, long cents) {}

    // a hole: each "/* ___ */ hole()" below is yours to replace
    static <T> T hole() {
        throw new UnsupportedOperationException("TODO");
    }

    static String money(long cents) {
        long abs = Math.abs(cents);
        String sign = /* ___ */ hole(); // "-" for a negative amount, "" otherwise
        long rest = /* ___ */ hole(); // the cents part, 0..99
        return sign + abs / 100 + "." + String.format("%02d", rest);
    }

    static List<String> statement(long opening, List<Tx> txs) {
        var lines = new ArrayList<String>();
        long balance = opening;
        for (var tx : txs) {
            balance = /* ___ */ hole(); // the balance after this transaction
            lines.add(String.format("%-10s %-12s %9s %9s", tx.date(), tx.desc(), money(tx.cents()), money(balance)));
        }
        lines.add("closing balance: " + money(balance));
        return lines;
    }

    static long totalSpent(List<Tx> txs) {
        return -txs.stream().filter(/* ___ */ hole()).mapToLong(Tx::cents).sum(); // which transactions?
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
