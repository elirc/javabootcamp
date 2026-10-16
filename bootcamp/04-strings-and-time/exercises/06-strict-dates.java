// ─────────────────────────────────────────────────────────────────────────
//  06 · dates people type                                   ★★☆ core
//  concepts: DateTimeFormatter · ResolverStyle.STRICT · uuuu vs yyyy · Locale
//  run: java -cp ../../_lib/out 06-strict-dates.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An expense tracker accepts dates typed as day/month/year. Write
//  parseDate: exactly "dd/MM/uuuu", and an impossible date like 31/02 is
//  an error, never quietly "corrected" (DateTimeParseException either way).
//  Then write label: a date as "Mon 05 Feb 2024", in English on every
//  machine.
//
//      parseDate("05/02/2024")    → 2024-02-05
//      parseDate("31/02/2024")    → DateTimeParseException
//      parseDate("2024-02-05")    → DateTimeParseException
//      label(2024-02-05)          → "Mon 05 Feb 2024"
//
//  hint: the default resolver turns 31 February into 29 February.

import static bootcamp.Check.*;

import java.time.*;
import java.time.format.*;
import java.util.*;

class Exercise {

    static LocalDate parseDate(String text) {
        throw new UnsupportedOperationException("TODO");
    }

    static String label(LocalDate date) {
        throw new UnsupportedOperationException("TODO");
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
