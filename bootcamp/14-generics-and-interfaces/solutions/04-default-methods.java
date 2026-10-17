// ─────────────────────────────────────────────────────────────────────────
//  04 · default and static methods — SOLUTION               ★★☆ core
//  concepts: default methods · static interface methods · diamond rule
//  run: java -cp ../../_lib/out 04-default-methods.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Since Java 8 an interface can carry behaviour: STATIC methods are
//  factories that belong to the interface (PricingRule.percentOff(10)
//  reads better than a PricingRules utility class), and DEFAULT methods
//  are combinators every implementation inherits for free. Because
//  PricingRule has one abstract method, each factory just returns a
//  lambda, and each default wraps `this` in a new lambda. This is the
//  exact shape of Comparator.thenComparing and Function.andThen.
//  Order matters in andThen: "this first, then next". Swapping the two
//  calls compiles and gives 85.50 instead of 85.00 on a 100.00 basket, a
//  bug that only a test with real numbers notices.
//  Rounding happens once per rule, HALF_UP to two places, with
//  divide(HUNDRED, 2, HALF_UP); a bare divide() can throw on a
//  non-terminating result.
//  The DIAMOND rule: when two interfaces give the same default, the class
//  does not compile until it overrides the method. Inside the override,
//  Greeter.super.greeting() calls a specific interface's version.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

class Exercise {

    static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @FunctionalInterface
    interface PricingRule {
        BigDecimal apply(BigDecimal price);

        static PricingRule percentOff(int percent) {
            if (percent < 0 || percent > 100) throw new IllegalArgumentException("percent must be 0..100, was " + percent);
            var keep = BigDecimal.valueOf(100 - percent);
            return price -> price.multiply(keep).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        }

        static PricingRule amountOff(BigDecimal amount) {
            return price -> price.subtract(amount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }

        static PricingRule identity() {
            return price -> price;
        }

        static PricingRule all(List<PricingRule> rules) {
            var result = identity();
            for (var rule : rules) result = result.andThen(rule);
            return result;
        }

        default PricingRule andThen(PricingRule next) {
            return price -> next.apply(apply(price));
        }

        default PricingRule floorAt(BigDecimal minimum) {
            return price -> apply(price).max(minimum);
        }
    }

    interface Greeter {
        default String greeting() {
            return "Hello";
        }
    }

    interface Localized {
        default String greeting() {
            return "Hola";
        }
    }

    static final class Kiosk implements Greeter, Localized {
        @Override
        public String greeting() {
            return Greeter.super.greeting() + " / " + Localized.super.greeting();
        }
    }

    static BigDecimal money(String s) {
        return new BigDecimal(s);
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("percentOff takes a percentage off and rounds half up to cents", () -> {
            eq(PricingRule.percentOff(10).apply(money("19.99")).toPlainString(), "17.99");
            eq(PricingRule.percentOff(15).apply(money("0.10")).toPlainString(), "0.09");
            eq(PricingRule.percentOff(100).apply(money("42.00")).toPlainString(), "0.00");
        });
        test("percentOff rejects a percentage outside 0..100", () -> {
            raises(IllegalArgumentException.class, () -> PricingRule.percentOff(101));
            raises(IllegalArgumentException.class, () -> PricingRule.percentOff(-1));
        });
        test("amountOff subtracts but never goes below zero", () -> {
            eq(PricingRule.amountOff(money("5")).apply(money("12.5")).toPlainString(), "7.50");
            eq(PricingRule.amountOff(money("5")).apply(money("3.00")).toPlainString(), "0.00");
        });
        test("andThen applies this rule first, then the next one", () -> {
            var percentFirst = PricingRule.percentOff(10).andThen(PricingRule.amountOff(money("5")));
            var amountFirst = PricingRule.amountOff(money("5")).andThen(PricingRule.percentOff(10));
            eq(percentFirst.apply(money("100")).toPlainString(), "85.00");
            eq(amountFirst.apply(money("100")).toPlainString(), "85.50");
        });
        test("floorAt keeps a rule's result from dropping under a minimum", () -> {
            var rule = PricingRule.amountOff(money("50")).floorAt(money("20"));
            eq(rule.apply(money("60")), money("20"));
            eq(rule.apply(money("100")), money("50"));
        });
        test("all chains rules in list order, and an empty list is identity", () -> {
            var rule = PricingRule.all(List.of(PricingRule.percentOff(50), PricingRule.amountOff(money("1"))));
            eq(rule.apply(money("10")).toPlainString(), "4.00");
            eq(PricingRule.all(List.of()).apply(money("7.77")).toPlainString(), "7.77");
            eq(PricingRule.identity().apply(money("1.5")).toPlainString(), "1.5");
        });
        test("Kiosk resolves the clashing defaults as Greeter, then Localized", () ->
            eq(new Kiosk().greeting(), "Hello / Hola"));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
