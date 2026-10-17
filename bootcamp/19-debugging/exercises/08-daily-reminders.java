// ─────────────────────────────────────────────────────────────────────────
//  08 · the reminder that moved                             ★★★ stretch
//  concepts: debugging · time zones · DST · week-based year
//  run: java -cp ../../_lib/out 08-daily-reminders.java
// ─────────────────────────────────────────────────────────────────────────
//
//  DEBUG HUNT: the code below is complete and compiles; it has bugs. Run
//  it, read the failing tests, find and fix the bugs. Do not edit tests.
//  Intended: reportDay(placedAt, storeZone) is the calendar day in the
//  STORE's zone when the order was placed. nextReminder(last) is the
//  same wall-clock time on the next calendar day in the same zone, even
//  across a daylight-saving change. label(day) is "yyyy-MM-dd" with the
//  calendar year.
//  Bug reports:
//    "Orders placed after 7 pm in New York land on the next day's
//     report. Tokyo's breakfast orders land on the day before."
//    "The 9:00 reminder fired at 10:00 after the clocks changed in
//     March, and at 8:00 after November."
//    "The report for 30 December 2024 was filed as 2025-12-30."
//
//      reportDay(2024-03-06T01:30Z, America/New_York) → 2024-03-05
//      nextReminder(2024-03-09T09:00 New York)        → 2024-03-10T09:00
//
//  hint: for each report, print the value right after every conversion.

import static bootcamp.Check.*;

import java.time.*;
import java.time.format.*;
import java.util.*;

class Exercise {

    static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("YYYY-MM-dd", Locale.US);

    static LocalDate reportDay(Instant placedAt, ZoneId storeZone) {
        return placedAt.atZone(ZoneOffset.UTC).toLocalDate();
    }

    static ZonedDateTime nextReminder(ZonedDateTime last) {
        return last.plusHours(24);
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
