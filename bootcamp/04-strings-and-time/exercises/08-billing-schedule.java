// ─────────────────────────────────────────────────────────────────────────
//  08 · billed on the 31st                                  ★★★ stretch
//  concepts: plusMonths · month-end clamping · drift · Period
//  run: java -cp ../../_lib/out 08-billing-schedule.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A bank simulator bills monthly on the day the account opened. Write
//  billingDates: the first `count` billing dates starting at `first`. A
//  short month bills on its last day, but the next month goes back to the
//  original day — the schedule never drifts. Then write describeGap: the
//  calendar distance between two dates as years, months and days, leaving
//  out zero parts, singular or plural; no difference at all → "0 days".
//
//      billingDates(2024-01-31, 4)
//          → [2024-01-31, 2024-02-29, 2024-03-31, 2024-04-30]
//      describeGap(2024-01-31, 2024-03-01)   → "1 month 1 day"
//      describeGap(2023-01-15, 2025-01-18)   → "2 years 3 days"
//
//  hint: compute each date from `first`, not from the previous date.

import static bootcamp.Check.*;

import java.time.*;
import java.util.*;

class Exercise {

    static List<LocalDate> billingDates(LocalDate first, int count) {
        throw new UnsupportedOperationException("TODO");
    }

    static String describeGap(LocalDate from, LocalDate to) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("bills on the same day each month", () -> eq(
            billingDates(LocalDate.of(2024, 1, 15), 3),
            List.of(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 2, 15), LocalDate.of(2024, 3, 15))));
        test("a month-end schedule never drifts", () -> eq(
            billingDates(LocalDate.of(2024, 1, 31), 4),
            List.of(LocalDate.of(2024, 1, 31), LocalDate.of(2024, 2, 29), LocalDate.of(2024, 3, 31), LocalDate.of(2024, 4, 30))));
        test("February outside a leap year", () -> eq(
            billingDates(LocalDate.of(2023, 1, 30), 3),
            List.of(LocalDate.of(2023, 1, 30), LocalDate.of(2023, 2, 28), LocalDate.of(2023, 3, 30))));
        test("crosses a year end", () -> eq(
            billingDates(LocalDate.of(2024, 11, 30), 3),
            List.of(LocalDate.of(2024, 11, 30), LocalDate.of(2024, 12, 30), LocalDate.of(2025, 1, 30))));
        test("zero dates is an empty schedule", () -> eq(billingDates(LocalDate.of(2024, 1, 1), 0), List.of()));
        test("describes a gap in months and days", () ->
            eq(describeGap(LocalDate.of(2024, 1, 31), LocalDate.of(2024, 3, 1)), "1 month 1 day"));
        test("leaves out zero parts and pluralises", () -> {
            eq(describeGap(LocalDate.of(2023, 1, 15), LocalDate.of(2025, 1, 18)), "2 years 3 days");
            eq(describeGap(LocalDate.of(2024, 5, 1), LocalDate.of(2024, 7, 1)), "2 months");
        });
        test("no gap is zero days", () -> eq(describeGap(LocalDate.of(2024, 5, 1), LocalDate.of(2024, 5, 1)), "0 days"));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
