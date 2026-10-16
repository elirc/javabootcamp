// ─────────────────────────────────────────────────────────────────────────
//  07 · late fees, on a clock you control                   ★★☆ core
//  concepts: Clock · LocalDate.now(clock) · time zones · testability
//  run: java -cp ../../_lib/out 07-injected-clock.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A library tracker charges late fees as of TODAY. Finish the nested
//  class LateFees: it is built from a Clock, a fee per day in cents and a
//  cap. isOverdue(due) is true once today is after the due date; feeFor
//  (due) is days late times the daily fee, never more than the cap, and 0
//  when not overdue. "Today" is the clock's date in the clock's zone.
//
//      clock at 2024-03-10 (UTC), 25 cents a day, cap 500
//      feeFor(2024-03-07)   → 75
//      feeFor(2024-01-01)   → 500
//      feeFor(2024-03-10)   → 0
//
//  hint: LocalDate.now() reads the real clock; there is an overload.

import static bootcamp.Check.*;

import java.time.*;
import java.time.temporal.ChronoUnit;

class Exercise {

    static final class LateFees {
        private final Clock clock;
        private final int centsPerDay;
        private final int capCents;

        LateFees(Clock clock, int centsPerDay, int capCents) {
            this.clock = clock;
            this.centsPerDay = centsPerDay;
            this.capCents = capCents;
        }

        boolean isOverdue(LocalDate due) {
            throw new UnsupportedOperationException("TODO");
        }

        int feeFor(LocalDate due) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static Clock at(String instant, String zone) {
        return Clock.fixed(Instant.parse(instant), ZoneId.of(zone));
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        var noon = at("2024-03-10T12:00:00Z", "UTC");
        test("charges per day late", () -> eq(new LateFees(noon, 25, 500).feeFor(LocalDate.of(2024, 3, 7)), 75));
        test("never charges more than the cap", () -> eq(new LateFees(noon, 25, 500).feeFor(LocalDate.of(2024, 1, 1)), 500));
        test("due today costs nothing", () -> eq(new LateFees(noon, 25, 500).feeFor(LocalDate.of(2024, 3, 10)), 0));
        test("due in the future costs nothing", () -> eq(new LateFees(noon, 25, 500).feeFor(LocalDate.of(2024, 4, 1)), 0));
        test("isOverdue starts the day after the due date", () -> {
            var fees = new LateFees(noon, 25, 500);
            eq(fees.isOverdue(LocalDate.of(2024, 3, 10)), false);
            eq(fees.isOverdue(LocalDate.of(2024, 3, 9)), true);
        });
        test("today is the date in the clock's zone", () -> {
            var tokyo = at("2024-03-10T23:30:00Z", "Asia/Tokyo");
            var london = at("2024-03-10T23:30:00Z", "Europe/London");
            eq(new LateFees(tokyo, 25, 500).feeFor(LocalDate.of(2024, 3, 10)), 25);
            eq(new LateFees(london, 25, 500).feeFor(LocalDate.of(2024, 3, 10)), 0);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
