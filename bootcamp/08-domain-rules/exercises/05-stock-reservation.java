// ─────────────────────────────────────────────────────────────────────────
//  05 · all or nothing                                      ★★★ stretch
//  concepts: atomic updates · check-then-apply · defensive copies · maps
//  run: java -cp ../../_lib/out 05-stock-reservation.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An order management tool reserves stock for a whole order at once.
//  Finish the nested class Inventory: it copies its starting stock;
//  available(sku) is 0 for an unknown sku; reserve(order) takes every
//  line's quantity out of stock and returns true — or, if ANY line can't
//  be covered, changes nothing at all and returns false; release(order)
//  puts quantities back. Any quantity below 1 → IllegalArgumentException
//  (before anything changes).
//
//      stock {TEA=5, CAKE=1}
//      reserve({TEA=2, CAKE=1})   → true,  stock {TEA=3, CAKE=0}
//      reserve({TEA=1, CAKE=1})   → false, stock unchanged
//
//  hint: a first pass that only checks, then a second that only changes.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class Inventory {
        private final Map<String, Integer> stock;

        Inventory(Map<String, Integer> initial) {
            throw new UnsupportedOperationException("TODO");
        }

        int available(String sku) {
            throw new UnsupportedOperationException("TODO");
        }

        boolean reserve(Map<String, Integer> order) {
            throw new UnsupportedOperationException("TODO");
        }

        void release(Map<String, Integer> order) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static Inventory shop() {
        return new Inventory(Map.of("TEA", 5, "CAKE", 1));
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("reserves a whole order", () -> {
            var inv = shop();
            eq(inv.reserve(Map.of("TEA", 2, "CAKE", 1)), true);
            eq(inv.available("TEA"), 3);
            eq(inv.available("CAKE"), 0);
        });
        test("a short order changes nothing", () -> {
            var inv = shop();
            inv.reserve(Map.of("CAKE", 1));
            eq(inv.reserve(Map.of("TEA", 1, "CAKE", 1)), false);
            eq(inv.available("TEA"), 5);
        });
        test("an unknown sku has nothing available", () -> {
            var inv = shop();
            eq(inv.available("COFFEE"), 0);
            eq(inv.reserve(Map.of("TEA", 1, "COFFEE", 1)), false);
            eq(inv.available("TEA"), 5);
        });
        test("can reserve right down to zero", () -> {
            var inv = shop();
            eq(inv.reserve(Map.of("TEA", 5)), true);
            eq(inv.reserve(Map.of("TEA", 1)), false);
        });
        test("release puts stock back", () -> {
            var inv = shop();
            inv.reserve(Map.of("TEA", 4));
            inv.release(Map.of("TEA", 4));
            eq(inv.available("TEA"), 5);
        });
        test("a bad quantity is refused before anything changes", () -> {
            var inv = shop();
            raises(IllegalArgumentException.class, () -> inv.reserve(Map.of("TEA", 1, "CAKE", 0)));
            eq(inv.available("TEA"), 5);
            raises(IllegalArgumentException.class, () -> inv.release(Map.of("TEA", -2)));
        });
        test("keeps its own copy of the starting stock", () -> {
            var start = new HashMap<>(Map.of("TEA", 5));
            var inv = new Inventory(start);
            start.put("TEA", 0);
            inv.reserve(Map.of("TEA", 1));
            eq(inv.available("TEA"), 4);
            eq(start.get("TEA"), 0);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
