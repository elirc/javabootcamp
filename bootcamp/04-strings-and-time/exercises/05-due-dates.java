// ─────────────────────────────────────────────────────────────────────────
//  05 · due dates                                           ★☆☆ warm-up
//  concepts: LocalDate · plusDays · ChronoUnit.DAYS.between · DayOfWeek
//  run: java -cp ../../_lib/out 05-due-dates.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A library tracker lends books for a number of days. Write dueDate
//  (borrowed plus the loan period), daysOverdue (how many days after the
//  due date the book came back; on time or early → 0) and isWeekend.
//
//      dueDate(2024-01-25, 14)             → 2024-02-08
//      dueDate(2024-02-20, 14)             → 2024-03-05   (leap year!)
//      daysOverdue(2024-03-01, 2024-03-04) → 3
//      daysOverdue(2024-03-01, 2024-02-27) → 0
//      isWeekend(2024-03-02)               → true   (a Saturday)
//
//  hint: LocalDate does the calendar for you; never add days by hand.

import static bootcamp.Check.*;

import java.time.*;
import java.time.temporal.ChronoUnit;

class Exercise {

    static LocalDate dueDate(LocalDate borrowed, int loanDays) {
        throw new UnsupportedOperationException("TODO");
    }

    static long daysOverdue(LocalDate due, LocalDate returned) {
        throw new UnsupportedOperationException("TODO");
    }

    static boolean isWeekend(LocalDate day) {
        throw new UnsupportedOperationException("TODO");
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
