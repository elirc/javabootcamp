// ─────────────────────────────────────────────────────────────────────────
//  01 · strategy: shipping rates — SOLUTION                 ★☆☆ warm-up
//  concepts: strategy pattern · lambdas as strategies · composite
//  run: java -cp ../../_lib/out 01-strategy-shipping.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  STRATEGY: one interface for "how to do X", several interchangeable
//  implementations, and a context (Checkout) that holds whichever one the
//  situation needs. Because ShippingStrategy has a single method, every
//  strategy here is a lambda returned by a small factory method. That is
//  the modern Java shape of the pattern; a class per strategy is only
//  worth it when a strategy has real state or several methods.
//  freeOver and cheapest are strategies built from other strategies
//  (decorator and composite in miniature). They are why strategies beat a
//  switch on a "shippingType" string: new rules compose from old ones
//  without touching Checkout.
//  The traps are in the numbers. "Rounded up to whole kilograms" is
//  setScale(0, CEILING); HALF_UP charges 2.4 kg as 2 kg. "Free over 50"
//  in a marketing email nearly always means 50.00 itself is free, so the
//  comparison is compareTo(...) >= 0. Never use equals on BigDecimal for
//  this: 50.0 and 50.00 are not equals().
//  Checkout uses a LinkedHashMap, so quoteAll lists options in the order
//  the shop registered them, which is the order the page shows them.

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
        var cost = amount.setScale(2, RoundingMode.HALF_UP);
        return parcel -> cost;
    }

    static ShippingStrategy byWeight(BigDecimal perKg, BigDecimal minimum) {
        return parcel -> {
            var kilos = parcel.weightKg().setScale(0, RoundingMode.CEILING);
            return kilos.multiply(perKg).max(minimum).setScale(2, RoundingMode.HALF_UP);
        };
    }

    static ShippingStrategy freeOver(BigDecimal limit, ShippingStrategy otherwise) {
        return parcel -> parcel.orderTotal().compareTo(limit) >= 0
            ? BigDecimal.ZERO.setScale(2)
            : otherwise.cost(parcel);
    }

    static ShippingStrategy cheapest(List<ShippingStrategy> options) {
        if (options.isEmpty()) throw new IllegalArgumentException("no shipping options");
        var copy = List.copyOf(options);
        return parcel -> copy.stream().map(s -> s.cost(parcel)).min(Comparator.naturalOrder()).orElseThrow();
    }

    static final class Checkout {
        private final Map<String, ShippingStrategy> options = new LinkedHashMap<>();

        void register(String name, ShippingStrategy strategy) {
            if (options.containsKey(name)) throw new IllegalArgumentException("option already registered: " + name);
            options.put(name, strategy);
        }

        BigDecimal quote(String name, Parcel parcel) {
            var strategy = options.get(name);
            if (strategy == null) throw new IllegalArgumentException("unknown shipping option: " + name);
            return strategy.cost(parcel);
        }

        Map<String, BigDecimal> quoteAll(Parcel parcel) {
            var out = new LinkedHashMap<String, BigDecimal>();
            options.forEach((name, strategy) -> out.put(name, strategy.cost(parcel)));
            return out;
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
            checkout.register("standard", flatRate(d("4.99")));
            checkout.register("express", byWeight(d("6"), d("9")));
            checkout.register("budget", freeOver(d("30"), flatRate(d("2.50"))));
            var quotes = checkout.quoteAll(parcel("2.2", "20"));
            eq(new ArrayList<>(quotes.keySet()), List.of("standard", "express", "budget"));
            eq(new ArrayList<>(quotes.values()), List.of(d("4.99"), d("18.00"), d("2.50")));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
