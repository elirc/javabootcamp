// ─────────────────────────────────────────────────────────────────────────
//  08 · billed on the 31st — SOLUTION                       ★★★ stretch
//  concepts: plusMonths · month-end clamping · drift · Period
//  run: java -cp ../../_lib/out 08-billing-schedule.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  plusMonths clamps: January 31 plus one month is February 29 (in 2024),
//  because there is no February 31. That is the right answer for ONE step.
//  The trap is chaining: date = date.plusMonths(1) in a loop gives Jan 31,
//  Feb 29, Mar 29, Apr 29... — once clamped, the schedule has forgotten
//  its anchor day and drifts for the rest of its life. Computing every
//  date from the anchor, first.plusMonths(i), clamps each month
//  independently and lands back on the 31st whenever the month has one.
//  The general lesson: in a recurrence, derive each step from the origin,
//  not from the last (possibly rounded) step. Money schedules, retries and
//  animation timers all have the same bug.
//  Period is the calendar-based amount of time — years, months, days —
//  as opposed to Duration, which is exact seconds. Period.between(a, b)
//  counts whole months first, then the leftover days, so Jan 31 to Mar 1
//  is 1 month and 1 day. (It does NOT normalise "14 months" into a year:
//  between gives you already-normalised parts, but Period.ofMonths(14)
//  stays 14 months until you call normalized().)
//  describeGap is then small string assembly: a part per non-zero field,
//  pluralised, joined with spaces, with "0 days" for the empty case.

import static bootcamp.Check.*;

import java.time.*;
import java.util.*;

class Exercise {

    static List<LocalDate> billingDates(LocalDate first, int count) {
        var dates = new ArrayList<LocalDate>();
        for (int i = 0; i < count; i++) dates.add(first.plusMonths(i));
        return dates;
    }

    static String describeGap(LocalDate from, LocalDate to) {
        var p = Period.between(from, to);
        var parts = new ArrayList<String>();
        if (p.getYears() != 0) parts.add(plural(p.getYears(), "year"));
        if (p.getMonths() != 0) parts.add(plural(p.getMonths(), "month"));
        if (p.getDays() != 0) parts.add(plural(p.getDays(), "day"));
        return parts.isEmpty() ? "0 days" : String.join(" ", parts);
    }

    private static String plural(int n, String unit) {
        return n + " " + unit + (Math.abs(n) == 1 ? "" : "s");
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
