// ─────────────────────────────────────────────────────────────────────────
//  04 · observer: stock alerts — SOLUTION                   ★★☆ core
//  concepts: observer pattern · subscriptions · snapshot iteration
//  run: java -cp ../../_lib/out 04-observer-stock.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  OBSERVER decouples the thing that changes (Inventory) from the things
//  that react (alerts, dashboards, reorder jobs): Inventory only knows a
//  StockListener interface. The pattern is simple; its failure modes are
//  in the notification loop.
//  1. Mutation during notification. A listener that cancels itself while
//  the loop walks the live list makes an index loop skip the next one,
//  and a for-each throw ConcurrentModificationException or quietly stop
//  early (it does here). A snapshot (List.copyOf) fixes both: the round
//  goes to exactly the subscribers present when it started.
//  2. One bad listener. If a listener's exception escapes, the listeners
//  after it never hear the change and the caller's set() fails for a
//  reason it cannot handle. Catch per listener, record, and continue.
//  3. Identity. A Subscription object per subscribe() call means "the same
//  listener twice" is two independent subscriptions, and cancel() removes
//  exactly one. Removing by listener would drop both.
//  lowStockAlert is EDGE-triggered: it compares old and new and fires on
//  the crossing, not on every change while low. Level-triggered alerts
//  page someone on every single sale once stock is low.

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
                subs.remove(this);
            }
        }

        Subscription subscribe(StockListener listener) {
            var sub = new Sub(listener);
            subs.add(sub);
            return sub;
        }

        void set(String sku, int qty) {
            if (qty < 0) throw new IllegalArgumentException("negative quantity for " + sku + ": " + qty);
            int old = quantity(sku);
            stock.put(sku, qty);
            if (old == qty) return;
            for (var sub : List.copyOf(subs)) {
                try {
                    sub.listener.onChange(sku, old, qty);
                } catch (RuntimeException e) {
                    failures.add(sku + ": " + e.getMessage());
                }
            }
        }

        int quantity(String sku) {
            return stock.getOrDefault(sku, 0);
        }

        List<String> failures() {
            return List.copyOf(failures);
        }
    }

    static StockListener lowStockAlert(int threshold, List<String> alerts) {
        return (sku, oldQty, newQty) -> {
            if (oldQty >= threshold && newQty < threshold) alerts.add("LOW " + sku + "=" + newQty);
        };
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
            for (int qty : new int[] {10, 4, 2, 8, 3, 5, 6, 5, 4}) inv.set("bolt", qty);
            eq(alerts, List.of("LOW bolt=4", "LOW bolt=3", "LOW bolt=4"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
