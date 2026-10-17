// ─────────────────────────────────────────────────────────────────────────
//  04 · observer: stock alerts                              ★★☆ core
//  concepts: observer pattern · subscriptions · snapshot iteration
//  run: java -cp ../../_lib/out 04-observer-stock.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A warehouse Inventory tells listeners when a quantity changes. Finish:
//    subscribe(l)   returns a Subscription; subscribing one listener twice
//                   makes two subscriptions (two calls per change)
//    cancel()       ends that one subscription; cancelling again is harmless
//    set(sku, qty)  qty < 0 → IllegalArgumentException, nobody notified; an
//                   unchanged qty notifies nobody; otherwise every listener
//                   subscribed when set() began hears onChange(sku, old,
//                   new), in subscription order (an unknown sku's old qty
//                   is 0), even if one cancels during the round
//    failures()     a listener that throws does not stop the others; each
//                   failure is recorded as "<sku>: <message>"
//    quantity(sku)  the current quantity, 0 if unknown
//  Then lowStockAlert(threshold, alerts): a listener that adds "LOW
//  <sku>=<qty>" only when a change CROSSES from >= threshold to below it.
//
//      threshold 5: 10 → 4 alerts; 4 → 2 does not; 2 → 8 → 3 alerts again
//
//  hint: iterate over a copy of the subscriptions, not the live list.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    interface StockListener {
        void onChange(String sku, int oldQty, int newQty);
    }

    interface Subscription {
        void cancel();
    }

    static final class Inventory {
        private final Map<String, Integer> stock = new HashMap<>();
        private final List<Sub> subs = new ArrayList<>();
        private final List<String> failures = new ArrayList<>();

        private final class Sub implements Subscription {
            final StockListener listener;

            Sub(StockListener listener) {
                this.listener = listener;
            }

            public void cancel() {
                throw new UnsupportedOperationException("TODO");
            }
        }

        Subscription subscribe(StockListener listener) {
            throw new UnsupportedOperationException("TODO");
        }

        void set(String sku, int qty) {
            throw new UnsupportedOperationException("TODO");
        }

        int quantity(String sku) {
            throw new UnsupportedOperationException("TODO");
        }

        List<String> failures() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static StockListener lowStockAlert(int threshold, List<String> alerts) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("listeners hear old and new quantities, in subscription order", () -> {
            var inv = new Inventory();
            var heard = new ArrayList<String>();
            inv.subscribe((sku, o, n) -> heard.add("A " + sku + " " + o + "->" + n));
            inv.subscribe((sku, o, n) -> heard.add("B " + sku + " " + o + "->" + n));
            inv.set("bolt", 10);
            inv.set("bolt", 7);
            eq(heard, List.of("A bolt 0->10", "B bolt 0->10", "A bolt 10->7", "B bolt 10->7"));
            eq(inv.quantity("bolt"), 7);
            eq(inv.quantity("nut"), 0);
        });
        test("setting the same quantity again notifies nobody", () -> {
            var inv = new Inventory();
            var calls = new int[1];
            inv.subscribe((sku, o, n) -> calls[0]++);
            inv.set("bolt", 3);
            inv.set("bolt", 3);
            eq(calls[0], 1);
        });
        test("one listener subscribed twice is called twice, and cancel ends one", () -> {
            var inv = new Inventory();
            var calls = new int[1];
            StockListener counter = (sku, o, n) -> calls[0]++;
            var first = inv.subscribe(counter);
            inv.subscribe(counter);
            inv.set("bolt", 1);
            eq(calls[0], 2);
            first.cancel();
            first.cancel();
            inv.set("bolt", 2);
            eq(calls[0], 3);
        });
        test("a listener that cancels itself mid-round does not stop the others", () -> {
            var inv = new Inventory();
            var heard = new ArrayList<String>();
            var self = new Subscription[1];
            self[0] = inv.subscribe((sku, o, n) -> { heard.add("once"); self[0].cancel(); });
            inv.subscribe((sku, o, n) -> heard.add("always " + n));
            inv.set("bolt", 1);
            inv.set("bolt", 2);
            eq(heard, List.of("once", "always 1", "always 2"));
        });
        test("a failing listener is recorded and the rest still hear the change", () -> {
            var inv = new Inventory();
            var heard = new ArrayList<Integer>();
            inv.subscribe((sku, o, n) -> { throw new IllegalStateException("dashboard down"); });
            inv.subscribe((sku, o, n) -> heard.add(n));
            inv.set("bolt", 4);
            eq(heard, List.of(4));
            eq(inv.failures(), List.of("bolt: dashboard down"));
            eq(inv.quantity("bolt"), 4);
        });
        test("a negative quantity is rejected and nobody is notified", () -> {
            var inv = new Inventory();
            var calls = new int[1];
            inv.subscribe((sku, o, n) -> calls[0]++);
            inv.set("bolt", 2);
            raises(IllegalArgumentException.class, () -> inv.set("bolt", -1));
            eq(calls[0], 1);
            eq(inv.quantity("bolt"), 2);
        });
        test("lowStockAlert fires only when a change crosses below the threshold", () -> {
            var inv = new Inventory();
            var alerts = new ArrayList<String>();
            inv.subscribe(lowStockAlert(5, alerts));
            for (int qty : new int[] {10, 4, 2, 8, 3, 5, 6}) inv.set("bolt", qty);
            eq(alerts, List.of("LOW bolt=4", "LOW bolt=3"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
