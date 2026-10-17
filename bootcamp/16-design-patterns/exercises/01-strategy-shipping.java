// ─────────────────────────────────────────────────────────────────────────
//  01 · strategy: shipping rates                            ★☆☆ warm-up
//  concepts: strategy pattern · lambdas as strategies · composite
//  run: java -cp ../../_lib/out 01-strategy-shipping.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A web shop offers several ways to price shipping and picks one per
//  checkout. Each way is a ShippingStrategy. Every cost is scale 2.
//
//    flatRate(a)              always a
//    byWeight(perKg, min)     the weight rounded UP to whole kg, times
//                             perKg, but never less than min
//    freeOver(limit, other)   0.00 when the order total is >= limit,
//                             else whatever other charges
//    cheapest(options)        the lowest of the options' costs; an empty
//                             list throws IllegalArgumentException
//  Checkout keeps named options in registration order: register (a name
//  used twice → IllegalArgumentException), quote(name, parcel) (unknown
//  name → IllegalArgumentException "unknown shipping option: <name>"),
//  and quoteAll(parcel), every option's cost in registration order.
//
//      byWeight(3.00, 5.00) on 2.1 kg → 9.00;  freeOver(50, ..) at 50 → 0.00
//
//  hint: RoundingMode.CEILING rounds 2.1 up to 3.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

class Exercise {

    record Parcel(BigDecimal weightKg, BigDecimal orderTotal) {}

    interface ShippingStrategy {
        BigDecimal cost(Parcel parcel);
    }

    static ShippingStrategy flatRate(BigDecimal amount) {
        throw new UnsupportedOperationException("TODO");
    }

    static ShippingStrategy byWeight(BigDecimal perKg, BigDecimal minimum) {
        throw new UnsupportedOperationException("TODO");
    }

    static ShippingStrategy freeOver(BigDecimal limit, ShippingStrategy otherwise) {
        throw new UnsupportedOperationException("TODO");
    }

    static ShippingStrategy cheapest(List<ShippingStrategy> options) {
        throw new UnsupportedOperationException("TODO");
    }

    static final class Checkout {
        private final Map<String, ShippingStrategy> options = new LinkedHashMap<>();

        void register(String name, ShippingStrategy strategy) {
            throw new UnsupportedOperationException("TODO");
        }

        BigDecimal quote(String name, Parcel parcel) {
            throw new UnsupportedOperationException("TODO");
        }

        Map<String, BigDecimal> quoteAll(Parcel parcel) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    static Parcel parcel(String kg, String total) {
        return new Parcel(d(kg), d(total));
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("flatRate charges the same for every parcel, at scale 2", () -> {
            var flat = flatRate(d("4.9"));
            eq(flat.cost(parcel("0.1", "10")).toPlainString(), "4.90");
            eq(flat.cost(parcel("30", "999")).toPlainString(), "4.90");
        });
        test("byWeight rounds the weight up to whole kilograms", () -> {
            var heavy = byWeight(d("3.00"), d("0"));
            eq(heavy.cost(parcel("2.1", "10")).toPlainString(), "9.00");
            eq(heavy.cost(parcel("2.0", "10")).toPlainString(), "6.00");
            eq(heavy.cost(parcel("2.4", "10")).toPlainString(), "9.00");
        });
        test("byWeight never charges less than the minimum", () ->
            eq(byWeight(d("3.00"), d("5")).cost(parcel("0.2", "10")).toPlainString(), "5.00"));
        test("freeOver is free from the limit up, inclusive", () -> {
            var promo = freeOver(d("50"), flatRate(d("4.99")));
            eq(promo.cost(parcel("1", "50.00")).toPlainString(), "0.00");
            eq(promo.cost(parcel("1", "120")).toPlainString(), "0.00");
            eq(promo.cost(parcel("1", "49.99")).toPlainString(), "4.99");
        });
        test("cheapest picks the lowest cost per parcel and needs an option", () -> {
            var best = cheapest(List.of(flatRate(d("7.50")), byWeight(d("2.00"), d("3.00"))));
            eq(best.cost(parcel("1.5", "10")), d("4.00"));
            eq(best.cost(parcel("5", "10")), d("7.50"));
            raises(IllegalArgumentException.class, () -> cheapest(List.of()));
        });
        test("Checkout quotes a named option and rejects unknown or duplicate names", () -> {
            var checkout = new Checkout();
            checkout.register("standard", flatRate(d("4.99")));
            eq(checkout.quote("standard", parcel("1", "10")), d("4.99"));
            var e = raises(IllegalArgumentException.class, () -> checkout.quote("drone", parcel("1", "10")));
            eq(e.getMessage(), "unknown shipping option: drone");
            raises(IllegalArgumentException.class, () -> checkout.register("standard", flatRate(d("1"))));
        });
        test("quoteAll lists every option in registration order", () -> {
            var checkout = new Checkout();
            checkout.register("pickup", flatRate(d("0")));
            checkout.register("standard", flatRate(d("4.99")));
            checkout.register("express", byWeight(d("6"), d("9")));
            checkout.register("budget", freeOver(d("30"), flatRate(d("2.50"))));
            var quotes = checkout.quoteAll(parcel("2.2", "20"));
            eq(new ArrayList<>(quotes.keySet()), List.of("pickup", "standard", "express", "budget"));
            eq(new ArrayList<>(quotes.values()), List.of(d("0.00"), d("4.99"), d("18.00"), d("2.50")));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
