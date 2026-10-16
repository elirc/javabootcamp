// ─────────────────────────────────────────────────────────────────────────
//  08 · import what you can, report the rest                ★★★ stretch
//  concepts: collecting errors · fail-fast vs report-all · records as results
//  run: java -cp ../../_lib/out 08-import-report.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An expense tracker imports lines like "food,12.50". One bad line must
//  not lose the other 9,999, so importLines never throws for bad DATA: it
//  returns the good expenses and a list of errors, one per bad line, as
//  "line N: reason". Blank lines and lines starting with "#" are skipped
//  (they still count for numbering). Rules: exactly two comma-separated
//  fields; a non-blank category (trimmed); an amount of digits with at most
//  two decimals, greater than zero, stored in cents.
//
//      importLines(["food,12.50", "rent,abc", "# note", ",3"])
//          → imported [Expense[category=food, cents=1250]]
//            errors   ["line 2: bad amount 'abc'",
//                      "line 4: blank category"]
//
//  hint: a regex like \d+(\.\d{1,2})? describes the amount exactly.

import static bootcamp.Check.*;

import java.util.*;
import java.util.regex.*;

class Exercise {

    record Expense(String category, long cents) {}

    record ImportResult(List<Expense> imported, List<String> errors) {}

    static ImportResult importLines(List<String> lines) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("imports good lines in cents", () -> eq(
            importLines(List.of("food,12.50", "rent , 900", "bus,2.5")),
            new ImportResult(List.of(new Expense("food", 1250), new Expense("rent", 90000), new Expense("bus", 250)), List.of())));
        test("reports a bad amount with its line number", () ->
            eq(importLines(List.of("food,12.50", "rent,abc")).errors(), List.of("line 2: bad amount 'abc'")));
        test("keeps going after a bad line", () ->
            eq(importLines(List.of("rent,abc", "food,1")).imported(), List.of(new Expense("food", 100))));
        test("skips blanks and comments but counts them", () ->
            eq(importLines(List.of("# header", "", "x,1.234")).errors(), List.of("line 3: bad amount '1.234'")));
        test("reports every kind of problem", () -> eq(
            importLines(List.of("food", ",3", "a,b,c", "fee,0.00", "tip,-1")).errors(),
            List.of("line 1: expected category,amount", "line 2: blank category",
                "line 3: expected category,amount", "line 4: amount must be greater than zero",
                "line 5: bad amount '-1'")));
        test("an empty file imports nothing and reports nothing", () ->
            eq(importLines(List.of()), new ImportResult(List.of(), List.of())));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
