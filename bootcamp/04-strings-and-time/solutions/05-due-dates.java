// ─────────────────────────────────────────────────────────────────────────
//  05 · due dates — SOLUTION                                ★☆☆ warm-up
//  concepts: LocalDate · plusDays · ChronoUnit.DAYS.between · DayOfWeek
//  run: java -cp ../../_lib/out 05-due-dates.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  LocalDate is a date with no time and no time zone — exactly what a due
//  date is. It is immutable: plusDays returns a NEW date, and
//  `borrowed.plusDays(14);` on its own line does nothing at all (a common
//  first bug; the result has to be returned or assigned).
//  plusDays knows month lengths and leap years, so January 25 + 14 lands
//  in February and 2024-02-20 + 14 counts February 29.
//  ChronoUnit.DAYS.between(start, end) counts whole days and is negative
//  when end is before start — clamp with Math.max(0, ...) because "early"
//  is not "minus three days overdue". (due.until(returned, DAYS) is the
//  same thing spelled from the other side.)
//  isWeekend compares DayOfWeek enum constants. Never compare
//  getDayOfWeek().getValue() with magic numbers or, worse, a day name
//  string — the name depends on the locale, the enum does not.
//  Note what is missing: no LocalDate.now(). These methods take the dates
//  they need as parameters, which is what makes them testable (exercise
//  07 injects a Clock for the cases that really need "today").

import static bootcamp.Check.*;

import java.time.*;
import java.time.temporal.ChronoUnit;

class Exercise {

    static LocalDate dueDate(LocalDate borrowed, int loanDays) {
        return borrowed.plusDays(loanDays);
    }

    static long daysOverdue(LocalDate due, LocalDate returned) {
        return Math.max(0, ChronoUnit.DAYS.between(due, returned));
    }

    static boolean isWeekend(LocalDate day) {
        var dow = day.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("a due date in the same month", () -> eq(dueDate(LocalDate.of(2024, 1, 1), 14), LocalDate.of(2024, 1, 15)));
        test("a due date across a month end", () -> eq(dueDate(LocalDate.of(2024, 1, 25), 14), LocalDate.of(2024, 2, 8)));
        test("a due date across a leap day", () -> eq(dueDate(LocalDate.of(2024, 2, 20), 14), LocalDate.of(2024, 3, 5)));
        test("returned late counts the days", () ->
            eq(daysOverdue(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 4)), 3));
        test("returned on time or early is zero", () -> {
            eq(daysOverdue(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 1)), 0);
            eq(daysOverdue(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 2, 27)), 0);
        });
        test("overdue across a year end", () ->
            eq(daysOverdue(LocalDate.of(2023, 12, 30), LocalDate.of(2024, 1, 2)), 3));
        test("knows the weekend", () -> {
            eq(isWeekend(LocalDate.of(2024, 3, 2)), true);
            eq(isWeekend(LocalDate.of(2024, 3, 3)), true);
            eq(isWeekend(LocalDate.of(2024, 3, 4)), false);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
