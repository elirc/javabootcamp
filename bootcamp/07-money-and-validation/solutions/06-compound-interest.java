// ─────────────────────────────────────────────────────────────────────────
//  06 · interest, month by month — SOLUTION                 ★★★ stretch
//  concepts: compounding · rounding per period · HALF_EVEN · schedules
//  run: java -cp ../../_lib/out 06-compound-interest.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  The closed-form formula P(1 + r/12)^n is what a textbook computes; a
//  bank's ledger computes month by month, rounds each credit to the cent
//  (a cent is the smallest thing an account can hold) and compounds on
//  the ROUNDED balance. After a few years the two disagree by cents, and
//  the ledger is the one that is right — so simulate it.
//  Per month: interest = balance × rate ÷ 1200, divided with a scale and
//  a rounding mode in ONE step: divide(BigDecimal.valueOf(1200), 2,
//  RoundingMode.HALF_EVEN). Splitting it into ÷100 then ÷12 risks
//  ArithmeticException (5 / 1200 never terminates) or an extra rounding.
//  HALF_EVEN matters exactly at the tie: 1005.00 × 0.5% is 5.025, which
//  HALF_UP would make 5.03 and HALF_EVEN makes 5.02. The tests sit on
//  that tie on purpose.
//  schedule records each new balance; balanceAfter is the last element,
//  or the principal for zero months. Writing balanceAfter in terms of
//  schedule keeps one source of truth for the arithmetic.
//  setScale(2) on the principal up front makes every balance print with
//  two decimals, even when nothing has been added.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

class Exercise {

    static List<BigDecimal> schedule(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        if (months < 0) throw new IllegalArgumentException("months must not be negative: " + months);
        var balances = new ArrayList<BigDecimal>();
        var balance = principal.setScale(2, RoundingMode.HALF_EVEN);
        var twelveHundred = BigDecimal.valueOf(1200);
        for (int m = 0; m < months; m++) {
            var interest = balance.multiply(annualRatePercent).divide(twelveHundred, 2, RoundingMode.HALF_EVEN);
            balance = balance.add(interest);
            balances.add(balance);
        }
        return balances;
    }

    static BigDecimal balanceAfter(BigDecimal principal, BigDecimal annualRatePercent, int months) {
        var all = schedule(principal, annualRatePercent, months);
        return all.isEmpty() ? principal.setScale(2, RoundingMode.HALF_EVEN) : all.get(all.size() - 1);
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
