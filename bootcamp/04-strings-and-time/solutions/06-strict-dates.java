// ─────────────────────────────────────────────────────────────────────────
//  06 · dates people type — SOLUTION                        ★★☆ core
//  concepts: DateTimeFormatter · ResolverStyle.STRICT · uuuu vs yyyy · Locale
//  run: java -cp ../../_lib/out 06-strict-dates.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  DateTimeFormatter.ofPattern("dd/MM/uuuu") alone parses "31/02/2024"
//  without complaint and returns 2024-02-29: the default resolver style is
//  SMART, which clamps an out-of-range day to the end of the month. For
//  user input that is a silent data change. withResolverStyle(
//  ResolverStyle.STRICT) makes it an error.
//  STRICT has a famous sting: with "yyyy" it fails on EVERY date. In
//  java.time "y" is year-of-ERA, which needs an era (AD/BC) to resolve
//  strictly, and the pattern has none. "u" is the proleptic year, which
//  needs nothing else. Use uuuu with STRICT; yyyy with STRICT is a
//  formatter that rejects everything.
//  Build the formatters once, as static finals: they are immutable and
//  thread-safe (unlike the old SimpleDateFormat, which was neither).
//  For label, EEE and MMM are day and month NAMES, which come from a
//  locale. ofPattern without one uses the default locale, so a French
//  machine prints "lun. 05 févr. 2024". Pin it with
//  ofPattern(pattern, Locale.ENGLISH) whenever the text is part of a file
//  format, a log line, or a test.

import static bootcamp.Check.*;

import java.time.*;
import java.time.format.*;
import java.util.*;

class Exercise {

    private static final DateTimeFormatter INPUT =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("EEE dd MMM uuuu", Locale.ENGLISH);

    static LocalDate parseDate(String text) {
        return LocalDate.parse(text, INPUT);
    }

    static String label(LocalDate date) {
        return date.format(LABEL);
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("parses day/month/year", () -> eq(parseDate("05/02/2024"), LocalDate.of(2024, 2, 5)));
        test("accepts a real leap day", () -> eq(parseDate("29/02/2024"), LocalDate.of(2024, 2, 29)));
        test("refuses an impossible date instead of fixing it", () -> {
            raises(DateTimeParseException.class, () -> parseDate("31/02/2024"));
            raises(DateTimeParseException.class, () -> parseDate("29/02/2023"));
        });
        test("refuses other layouts", () -> {
            raises(DateTimeParseException.class, () -> parseDate("2024-02-05"));
            raises(DateTimeParseException.class, () -> parseDate("5/2/2024"));
        });
        test("labels a date in English", () -> eq(label(LocalDate.of(2024, 2, 5)), "Mon 05 Feb 2024"));
        test("the label ignores the machine's locale", () -> {
            var saved = Locale.getDefault();
            Locale.setDefault(Locale.FRANCE);
            try {
                eq(label(LocalDate.of(2024, 12, 25)), "Wed 25 Dec 2024");
            } finally {
                Locale.setDefault(saved);
            }
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
