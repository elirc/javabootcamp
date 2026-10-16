// ─────────────────────────────────────────────────────────────────────────
//  03 · split the bill to the cent                          ★★☆ core
//  concepts: allocation · integer cents · remainders · movePointRight
//  run: java -cp ../../_lib/out 03-split-the-bill.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An expense tracker splits a shared bill. Write split: divide total
//  among `people` so the parts add up to EXACTLY the total; when cents are
//  left over, the first people pay one cent more. Every part has two
//  decimals. A total with more than two decimals, a negative total, or
//  fewer than one person → IllegalArgumentException.
//
//      split(10.00, 3)    → [3.34, 3.33, 3.33]
//      split(0.05, 2)     → [0.03, 0.02]
//      split(10.00, 0)    → IllegalArgumentException
//
//  hint: rounding each share can't add up; distribute the remainder.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.util.*;

class Exercise {

    static List<BigDecimal> split(BigDecimal total, int people) {
        throw new UnsupportedOperationException("TODO");
    }

    static List<String> plain(List<BigDecimal> parts) {
        return parts.stream().map(BigDecimal::toPlainString).toList();
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("an even split", () -> eq(plain(split(new BigDecimal("9.00"), 3)), List.of("3.00", "3.00", "3.00")));
        test("the first people pay the extra cents", () ->
            eq(plain(split(new BigDecimal("10.00"), 3)), List.of("3.34", "3.33", "3.33")));
        test("the parts always add up to the total", () -> {
            var parts = split(new BigDecimal("100.01"), 7);
            eq(parts.stream().reduce(BigDecimal.ZERO, BigDecimal::add), new BigDecimal("100.01"));
            eq(parts.size(), 7);
        });
        test("fewer cents than people", () -> eq(plain(split(new BigDecimal("0.05"), 2)), List.of("0.03", "0.02")));
        test("one person pays it all", () -> eq(plain(split(new BigDecimal("12.5"), 1)), List.of("12.50")));
        test("a zero bill splits into zeros", () -> eq(plain(split(BigDecimal.ZERO, 2)), List.of("0.00", "0.00")));
        test("refuses nonsense", () -> {
            raises(IllegalArgumentException.class, () -> split(new BigDecimal("10.00"), 0));
            raises(IllegalArgumentException.class, () -> split(new BigDecimal("-1.00"), 2));
            raises(IllegalArgumentException.class, () -> split(new BigDecimal("10.005"), 2));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
