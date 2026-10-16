// ─────────────────────────────────────────────────────────────────────────
//  05 · all or nothing — SOLUTION                           ★★★ stretch
//  concepts: atomic updates · check-then-apply · defensive copies · maps
//  run: java -cp ../../_lib/out 05-stock-reservation.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Subtracting line by line and stopping at the first shortfall leaves a
//  half-reserved order: TEA taken, CAKE not, and nobody remembers to put
//  the TEA back. In a database you would use a transaction; in memory the
//  equivalent is two passes. Pass one validates every line (quantities
//  positive, enough stock) and touches nothing. Only if the whole order
//  passes does pass two apply the changes — and pass two cannot fail.
//  That shape, "validate everything, then apply everything", is how you
//  make a multi-step change atomic without rollbacks: nothing can go
//  wrong after the first write.
//  Validation of quantities happens even before availability, so a
//  malformed order is an IllegalArgumentException (caller bug), while
//  "not enough stock" is an ordinary false (business outcome).
//  merge(sku, -qty, Integer::sum) subtracts in place; getOrDefault(sku,
//  0) makes an unknown sku simply "none available", so it fails the
//  availability check without special-casing.
//  The constructor copies the map (new HashMap<>(initial)): the caller's
//  map must neither change our stock later nor be changed by our
//  reservations. (In a multithreaded server, reserve would also need to
//  be synchronized — two orders checking at the same time could both see
//  the last CAKE. Single-threaded here.)

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class Inventory {
        private final Map<String, Integer> stock;

        Inventory(Map<String, Integer> initial) {
            this.stock = new HashMap<>(initial);
        }

        int available(String sku) {
            return stock.getOrDefault(sku, 0);
        }

        boolean reserve(Map<String, Integer> order) {
            requirePositive(order);
            for (var line : order.entrySet()) {
                if (available(line.getKey()) < line.getValue()) return false;
            }
            for (var line : order.entrySet()) stock.merge(line.getKey(), -line.getValue(), Integer::sum);
            return true;
        }

        void release(Map<String, Integer> order) {
            requirePositive(order);
            for (var line : order.entrySet()) stock.merge(line.getKey(), line.getValue(), Integer::sum);
        }

        private static void requirePositive(Map<String, Integer> order) {
            for (var line : order.entrySet()) {
                if (line.getValue() < 1) throw new IllegalArgumentException("quantity must be at least 1: " + line);
            }
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
