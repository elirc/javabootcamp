// ─────────────────────────────────────────────────────────────────────────
//  07 · the checkout total — SOLUTION                       ★★☆ core
//  concepts: pricing pipelines · order of operations · clamping · guards
//  run: java -cp ../../_lib/out 07-checkout-total.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A pricing pipeline is a sequence of small, named steps, and the ORDER
//  is part of the business rule: discount-then-coupon-then-tax gives a
//  different number from coupon-then-discount, and tax is (almost always)
//  charged on the discounted price. Code each step as its own line with
//  its own rounding so the code reads like the policy document.
//  Round at each money-producing step that a receipt would show: the
//  discounted price and the tax are both printed amounts, so both are
//  rounded to cents with HALF_UP. 19.99 × 85% = 16.9915 → 16.99; tax at
//  7% is 1.1893 → 1.19; total 18.18.
//  The coupon clamp is max(ZERO, price − coupon): a coupon bigger than
//  the cart makes it free, never negative — and the tax on zero is zero.
//  Guards first, all of them, before any arithmetic: an invalid discount
//  of 150% is a caller bug and must not produce a negative price that
//  something downstream happily charges. Use signum() < 0 for "negative"
//  on BigDecimal; it reads better than compareTo(ZERO) < 0.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

class Exercise {

    static BigDecimal finalPrice(BigDecimal subtotal, int discountPercent, BigDecimal coupon, BigDecimal taxPercent) {
        if (subtotal.signum() < 0) throw new IllegalArgumentException("negative subtotal");
        if (discountPercent < 0 || discountPercent > 100) throw new IllegalArgumentException("discount must be 0..100");
        if (coupon.signum() < 0) throw new IllegalArgumentException("negative coupon");
        if (taxPercent.signum() < 0) throw new IllegalArgumentException("negative tax rate");

        var keep = BigDecimal.valueOf(100 - discountPercent).movePointLeft(2);
        var discounted = subtotal.multiply(keep).setScale(2, RoundingMode.HALF_UP);
        var afterCoupon = discounted.subtract(coupon).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        var tax = afterCoupon.multiply(taxPercent).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
        return afterCoupon.add(tax);
    }

    static String price(String subtotal, int discount, String coupon, String tax) {
        return finalPrice(new BigDecimal(subtotal), discount, new BigDecimal(coupon), new BigDecimal(tax)).toPlainString();
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("tax only", () -> eq(price("50", 0, "0", "20"), "60.00"));
        test("discount, then coupon, then tax", () -> eq(price("100.00", 10, "5.00", "10"), "93.50"));
        test("rounds the discount and the tax to cents", () -> eq(price("19.99", 15, "0", "7"), "18.18"));
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
