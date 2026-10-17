// ─────────────────────────────────────────────────────────────────────────
//  04 · default and static methods                          ★★☆ core
//  concepts: default methods · static interface methods · diamond rule
//  run: java -cp ../../_lib/out 04-default-methods.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A checkout's pricing rules are one-method interfaces that compose.
//  Finish PricingRule's static factories and default combinators:
//
//    percentOff(p)   price * (100 - p) / 100, rounded HALF_UP to cents;
//                    p outside 0..100 throws IllegalArgumentException
//    amountOff(a)    price - a, never below 0.00, scale 2
//    identity()      returns the price unchanged
//    andThen(next)   apply this rule, then next to its result
//    floorAt(min)    this rule's result, but never under min
//    all(rules)      the rules chained in list order; empty = identity
//  Then finish Kiosk.greeting(): both interfaces define a default
//  greeting(), so Kiosk must choose. Return Greeter's, " / ", Localized's.
//
//      percentOff(10).apply(19.99)                     → 17.99
//      percentOff(10).andThen(amountOff(5)).apply(100) → 85.00
//      amountOff(5).andThen(percentOff(10)).apply(100) → 85.50
//
//  hint: inside a default method, `this` is the rule being extended.

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
            throw new UnsupportedOperationException("TODO");
        }

        static PricingRule amountOff(BigDecimal amount) {
            throw new UnsupportedOperationException("TODO");
        }

        static PricingRule identity() {
            throw new UnsupportedOperationException("TODO");
        }

        static PricingRule all(List<PricingRule> rules) {
            throw new UnsupportedOperationException("TODO");
        }

        default PricingRule andThen(PricingRule next) {
            throw new UnsupportedOperationException("TODO");
        }

        default PricingRule floorAt(BigDecimal minimum) {
            throw new UnsupportedOperationException("TODO");
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
            throw new UnsupportedOperationException("TODO");
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
