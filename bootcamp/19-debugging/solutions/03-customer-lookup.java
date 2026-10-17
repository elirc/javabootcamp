// ─────────────────────────────────────────────────────────────────────────
//  03 · works in staging — SOLUTION                         ★☆☆ warm-up
//  concepts: debugging · Integer cache · == vs equals · string identity
//  run: java -cp ../../_lib/out 03-customer-lookup.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Both bugs are == on objects: it compares IDENTITY (same object), not
//  value. They hide because Java often hands you the same object anyway.
//  1. Reproduce "customer 4521 has 0": two orders built in code for 4521
//     and a query for 4521. Narrow: the same test with customer 7 passes,
//     so the logic is right and the VALUE matters. Autoboxing calls
//     Integer.valueOf, which caches -128..127: small ids are the same
//     object, 4521 is a new object every time. Fix: Objects.equals (or
//     .equals, or compare int values). Pinned by "counts a large
//     customer's paid orders" (staging ids were all under 128).
//  2. Reproduce "CSV orders are never paid": the literal "PAID" in code
//     is interned, so code-built orders pass, but split() and trim()
//     build NEW strings. Fix: "PAID".equals(status), which is also
//     null-safe. Pinned by "counts orders parsed from CSV lines".
//  Lesson: == on a reference type is almost never what you mean (module
//  01, exercises 03 and 05). Tests built only from literals and small
//  numbers can pass forever; include data that comes from parsing and
//  numbers above 127.

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
            .filter(o -> Objects.equals(o.customerId(), customerId) && "PAID".equals(o.status()))
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
