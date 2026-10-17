// ─────────────────────────────────────────────────────────────────────────
//  08 · the reminder that moved — SOLUTION                  ★★★ stretch
//  concepts: debugging · time zones · DST · week-based year
//  run: java -cp ../../_lib/out 08-daily-reminders.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Three bugs, each a different meaning of "a day".
//  1. Reproduce: an order at 01:30Z on 6 March, store in New York. Print
//     placedAt.atZone(...): it shows 01:30Z, the UTC day. The storeZone
//     parameter is accepted and never used. Fix: atZone(storeZone). The
//     Tokyo test proves it is not "subtract a few hours": the error goes
//     the other way east of UTC. Pinned by the two local-day tests.
//  2. Reproduce: 9:00 on 9 March in New York, plus 24 hours. 10 March is
//     only 23 hours long (clocks jump 2:00 → 3:00), so 24 hours later is
//     10:00; in November the day is 25 hours and you land at 8:00.
//     plusHours is a Duration (elapsed time); plusDays is a Period
//     (calendar time) and keeps the wall-clock time. Fix: plusDays(1).
//     Pinned by the March and November tests; the ordinary-day test
//     passes either way, which is why this ships.
//  3. Reproduce: label(30 Dec 2024). "YYYY" is the WEEK-BASED year: 30
//     December 2024 sits in week 1 of 2025. "yyyy" is the calendar year
//     (strictly, year-of-era; "uuuu" is the proleptic year). It only goes
//     wrong in the last days of December or the first of January, so it
//     passes every test written in June. Pinned by "labels use the
//     calendar year".
//  Module 04 (exercises 05-08): keep instants for "when", a ZoneId for
//  "where", and convert at the edge.

import static bootcamp.Check.*;

import java.time.*;
import java.time.format.*;
import java.util.*;

class Exercise {

    static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);

    static LocalDate reportDay(Instant placedAt, ZoneId storeZone) {
        return placedAt.atZone(storeZone).toLocalDate();
    }

    static ZonedDateTime nextReminder(ZonedDateTime last) {
        return last.plusDays(1);
    }

    static String label(LocalDate day) {
        return LABEL.format(day);
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        var ny = ZoneId.of("America/New_York");
        var tokyo = ZoneId.of("Asia/Tokyo");
        test("an evening order in New York is reported on its local day", () -> {
            eq(reportDay(Instant.parse("2024-03-06T01:30:00Z"), ny), LocalDate.of(2024, 3, 5));
        });
        test("a breakfast order in Tokyo is reported on its local day", () -> {
            eq(reportDay(Instant.parse("2024-03-05T20:00:00Z"), tokyo), LocalDate.of(2024, 3, 6));
        });
        test("a midday order is on the same day everywhere", () -> {
            eq(reportDay(Instant.parse("2024-03-05T12:00:00Z"), ZoneOffset.UTC), LocalDate.of(2024, 3, 5));
            eq(reportDay(Instant.parse("2024-03-05T16:00:00Z"), ny), LocalDate.of(2024, 3, 5));
        });
        test("the next reminder on an ordinary day", () -> {
            var last = ZonedDateTime.of(2024, 6, 1, 9, 0, 0, 0, ny);
            eq(nextReminder(last), ZonedDateTime.of(2024, 6, 2, 9, 0, 0, 0, ny));
        });
        test("the next reminder keeps 9:00 across the March change", () -> {
            var last = ZonedDateTime.of(2024, 3, 9, 9, 0, 0, 0, ny);
            eq(nextReminder(last).toLocalDateTime(), LocalDateTime.of(2024, 3, 10, 9, 0));
        });
        test("the next reminder keeps 9:00 across the November change", () -> {
            var last = ZonedDateTime.of(2024, 11, 2, 9, 0, 0, 0, ny);
            eq(nextReminder(last).toLocalDateTime(), LocalDateTime.of(2024, 11, 3, 9, 0));
            eq(nextReminder(last).getZone(), ny);
        });
        test("labels use the calendar year", () -> {
            eq(label(LocalDate.of(2024, 6, 1)), "2024-06-01");
            eq(label(LocalDate.of(2024, 12, 30)), "2024-12-30");
            eq(label(LocalDate.of(2027, 1, 1)), "2027-01-01");
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
