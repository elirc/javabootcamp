// ─────────────────────────────────────────────────────────────────────────
//  07 · the checkout total                                  ★★☆ core
//  concepts: pricing pipelines · order of operations · clamping · guards
//  run: java -cp ../../_lib/out 07-checkout-total.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An order system prices a cart in a fixed order: first a percentage
//  discount (the discounted PRICE rounded to the cent, halves up), then a
//  coupon amount (the price never drops below zero), then tax on what is
//  left (rounded to the cent, halves up). Write finalPrice. A discount
//  outside 0..100, a negative coupon, a negative tax rate or a negative
//  subtotal → IllegalArgumentException. The result always has two
//  decimals.
//
//      finalPrice(100.00, 10, 5.00, 10)   → 93.50    (90.00 → 85.00 → +8.50)
//      finalPrice(3.00, 0, 5.00, 20)      → 0.00
//      finalPrice(19.99, 15, 0, 7)        → 18.18    (16.99 → +1.19)
//
//  hint: write the steps in the order the business states them.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

class Exercise {

    static BigDecimal finalPrice(BigDecimal subtotal, int discountPercent, BigDecimal coupon, BigDecimal taxPercent) {
        throw new UnsupportedOperationException("TODO");
    }

    static String price(String subtotal, int discount, String coupon, String tax) {
        return finalPrice(new BigDecimal(subtotal), discount, new BigDecimal(coupon), new BigDecimal(tax)).toPlainString();
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("tax only", () -> eq(price("50", 0, "0", "20"), "60.00"));
        test("discount, then coupon, then tax", () -> eq(price("100.00", 10, "5.00", "10"), "93.50"));
        test("rounds the discounted price and the tax to cents", () -> {
            eq(price("19.99", 15, "0", "7"), "18.18");
            eq(price("0.05", 10, "0", "0"), "0.05");
        });
        test("a big coupon makes it free, not negative", () -> eq(price("3.00", 0, "5.00", "20"), "0.00"));
        test("a full discount is free", () -> eq(price("42.00", 100, "0", "20"), "0.00"));
        test("refuses a discount outside 0..100", () -> {
            raises(IllegalArgumentException.class, () -> price("10", 101, "0", "0"));
            raises(IllegalArgumentException.class, () -> price("10", -1, "0", "0"));
        });
        test("refuses negative coupons, taxes and subtotals", () -> {
            raises(IllegalArgumentException.class, () -> price("10", 0, "-1", "0"));
            raises(IllegalArgumentException.class, () -> price("10", 0, "0", "-1"));
            raises(IllegalArgumentException.class, () -> price("-10", 0, "0", "0"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
