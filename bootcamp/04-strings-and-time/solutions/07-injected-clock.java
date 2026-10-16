// ─────────────────────────────────────────────────────────────────────────
//  07 · late fees, on a clock you control — SOLUTION        ★★☆ core
//  concepts: Clock · LocalDate.now(clock) · time zones · testability
//  run: java -cp ../../_lib/out 07-injected-clock.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Code that calls LocalDate.now() cannot be tested: the answer changes
//  every day, and you cannot make "today" be a leap day or a year end on
//  demand. java.time's answer is Clock — an object that answers "what
//  instant is it, and in which zone?" Every now() has a now(Clock)
//  overload. Production passes Clock.systemDefaultZone(); tests pass
//  Clock.fixed(instant, zone) and get the same answer forever.
//  The class takes the Clock in its constructor and keeps it in a final
//  field: dependency injection without a framework.
//  The zone matters, and the last test proves it. The instant
//  2024-03-10T23:30Z is still March 10 in London but already March 11 in
//  Tokyo. LocalDate.now(clock) uses the clock's zone, so a Tokyo clock
//  makes a book due March 10 one day late. That is the right answer for a
//  library in Tokyo, and the reason "today" is never a bare instant.
//  Fee arithmetic: days late from ChronoUnit.DAYS.between(due, today);
//  long times int is long; Math.min against the cap; Math.toIntExact to
//  come back to int loudly rather than by truncation.

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
            return LocalDate.now(clock).isAfter(due);
        }

        int feeFor(LocalDate due) {
            long daysLate = ChronoUnit.DAYS.between(due, LocalDate.now(clock));
            if (daysLate <= 0) return 0;
            return Math.toIntExact(Math.min(daysLate * centsPerDay, capCents));
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
