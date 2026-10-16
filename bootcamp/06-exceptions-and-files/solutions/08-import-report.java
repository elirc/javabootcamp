// ─────────────────────────────────────────────────────────────────────────
//  08 · import what you can, report the rest — SOLUTION     ★★★ stretch
//  concepts: collecting errors · fail-fast vs report-all · records as results
//  run: java -cp ../../_lib/out 08-import-report.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Fail-fast (throw on the first problem) is right for a programming
//  error or a single value. For BATCH input from humans it is hostile:
//  the user fixes line 3, reruns, learns about line 9, reruns... Collect
//  every problem and report them together, and import what is valid —
//  or, if all-or-nothing matters, import nothing but still report all.
//  The result is a record of two lists: a value object for "what
//  happened". Returning it beats throwing an exception that carries a
//  list, because a partly-successful import is not exceptional.
//  Per line: skip blanks and comments first, then split(",", -1) so a
//  trailing empty field is visible, then validate field by field and
//  record the FIRST problem with its line number. A small helper that
//  returns either an Expense or an error string keeps the loop flat.
//  The amount: validate the shape with a regex BEFORE converting, so
//  "1e3", "12.", "-4" and "12.345" are all caught by one readable rule.
//  Then convert without doubles: split on "." and compute whole * 100 +
//  fraction padded to two digits ("12.5" means 1250, not 1205). Module 07
//  does the same with BigDecimal.

import static bootcamp.Check.*;

import java.util.*;
import java.util.regex.*;

class Exercise {

    record Expense(String category, long cents) {}

    record ImportResult(List<Expense> imported, List<String> errors) {}

    private static final Pattern AMOUNT = Pattern.compile("(\\d+)(?:\\.(\\d{1,2}))?");

    static ImportResult importLines(List<String> lines) {
        var imported = new ArrayList<Expense>();
        var errors = new ArrayList<String>();
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            var parts = line.split(",", -1);
            String problem;
            if (parts.length != 2) {
                problem = "expected category,amount";
            } else if (parts[0].isBlank()) {
                problem = "blank category";
            } else {
                var m = AMOUNT.matcher(parts[1].trim());
                if (!m.matches()) {
                    problem = "bad amount '" + parts[1].trim() + "'";
                } else {
                    var fraction = m.group(2) == null ? "00" : (m.group(2) + "0").substring(0, 2);
                    long cents = Long.parseLong(m.group(1)) * 100 + Integer.parseInt(fraction);
                    if (cents == 0) {
                        problem = "amount must be greater than zero";
                    } else {
                        imported.add(new Expense(parts[0].trim(), cents));
                        continue;
                    }
                }
            }
            errors.add("line " + (i + 1) + ": " + problem);
        }
        return new ImportResult(imported, errors);
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
