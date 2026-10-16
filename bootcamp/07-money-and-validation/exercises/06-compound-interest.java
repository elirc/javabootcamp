// ─────────────────────────────────────────────────────────────────────────
//  06 · interest, month by month                            ★★★ stretch
//  concepts: compounding · rounding per period · HALF_EVEN · schedules
//  run: java -cp ../../_lib/out 06-compound-interest.java
// ─────────────────────────────────────────────────────────────────────────
//
//  The bank simulator credits savings interest monthly, like a real bank:
//  each month's interest is balance × annualRate% ÷ 12, rounded to the
//  cent with banker's rounding, and ADDED before the next month is
//  computed. Write schedule (the balance after each month, two decimals
//  each) and balanceAfter (the balance after the last month; 0 months →
//  the principal). Negative months → IllegalArgumentException.
//
//      schedule(1000.00, 6, 3)          → [1005.00, 1010.02, 1015.07]
//      balanceAfter(1000.00, 5, 12)     → 1051.16
//      balanceAfter(1000.00, 0, 24)     → 1000.00
//
//  hint: month 2 earns 5.025, which banker's rounding makes 5.02.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

class Exercise {

    static List<BigDecimal> schedule(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        throw new UnsupportedOperationException("TODO");
    }

    static BigDecimal balanceAfter(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        throw new UnsupportedOperationException("TODO");
    }

    static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    static List<String> plain(List<BigDecimal> xs) {
        return xs.stream().map(BigDecimal::toPlainString).toList();
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("compounds on the rounded balance", () ->
            eq(plain(schedule(d("1000.00"), d("6"), 3)), List.of("1005.00", "1010.02", "1015.07")));
        test("a rate that never divides evenly", () ->
            eq(balanceAfter(d("1000.00"), d("5"), 12).toPlainString(), "1051.16"));
        test("a fractional rate over six months", () -> eq(
            plain(schedule(d("2500.00"), d("3.5"), 6)),
            List.of("2507.29", "2514.60", "2521.93", "2529.29", "2536.67", "2544.07")));
        test("zero months leaves the principal", () -> {
            eq(balanceAfter(d("1000"), d("6"), 0).toPlainString(), "1000.00");
            eq(schedule(d("1000"), d("6"), 0), List.of());
        });
        test("a zero rate changes nothing", () -> eq(balanceAfter(d("1000.00"), d("0"), 24).toPlainString(), "1000.00"));
        test("refuses negative months", () ->
            raises(IllegalArgumentException.class, () -> schedule(d("1000.00"), d("6"), -1)));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
