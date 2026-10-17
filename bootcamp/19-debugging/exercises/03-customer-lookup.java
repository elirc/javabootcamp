// ─────────────────────────────────────────────────────────────────────────
//  03 · works in staging                                    ★☆☆ warm-up
//  concepts: debugging · Integer cache · == vs equals · string identity
//  run: java -cp ../../_lib/out 03-customer-lookup.java
// ─────────────────────────────────────────────────────────────────────────
//
//  DEBUG HUNT: the code below is complete and compiles; it has bugs. Run
//  it, read the failing tests, find and fix the bugs. Do not edit tests.
//  Intended: parse("id, customerId, status") reads an order line,
//  trimming each field; countPaid(orders, customerId) counts the orders
//  of that customer whose status is PAID.
//  Bug reports:
//    "In staging (customers 1-100) the counts are right. In production
//     customer 4521 has 0 paid orders, and we can see three."
//    "Orders loaded from the nightly CSV are never counted as paid."
//
//      countPaid([o1(7, PAID), o2(7, NEW)], 7)      → 1
//      countPaid([parse("o1, 4521, PAID")], 4521)   → 1
//
//  hint: the staging data and the production data differ in one way.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Order(String id, Integer customerId, String status) {}

    static Order parse(String line) {
        var f = line.split(",");
        return new Order(f[0].trim(), Integer.valueOf(f[1].trim()), f[2].trim());
    }

    static long countPaid(List<Order> orders, Integer customerId) {
        return orders.stream()
            .filter(o -> o.customerId() == customerId && o.status() == "PAID")
            .count();
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("counts a small customer's paid orders built in code", () -> {
            eq(countPaid(List.of(new Order("o1", 7, "PAID"), new Order("o2", 7, "NEW"), new Order("o3", 8, "PAID")), 7), 1);
        });
        test("counts a large customer's paid orders", () -> {
            var orders = List.of(new Order("o1", 4521, "PAID"), new Order("o2", 4521, "PAID"), new Order("o3", 4521, "NEW"));
            eq(countPaid(orders, 4521), 2);
            eq(countPaid(orders, Integer.valueOf("4521")), 2);
        });
        test("counts orders parsed from CSV lines", () -> {
            var orders = List.of(parse("o1, 7, PAID"), parse("o2,7,PAID"), parse("o3, 7 ,REFUNDED"));
            eq(countPaid(orders, 7), 2);
        });
        test("ignores other customers and statuses", () -> {
            var orders = List.of(new Order("o1", 1, "NEW"), new Order("o2", 2, "PAID"), new Order("o3", 1, "paid"));
            eq(countPaid(orders, 1), 0);
            eq(countPaid(List.of(), 1), 0);
        });
        test("parse trims every field", () -> {
            eq(parse(" o9 , 12 , NEW "), new Order("o9", 12, "NEW"));
            eq(parse("o1,100000,PAID").customerId(), 100000);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
