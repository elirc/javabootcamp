// ─────────────────────────────────────────────────────────────────────────
//  05 · where did the money go                              ★★☆ core
//  concepts: stream pipelines · filter/map/reduce · summingInt · OptionalInt
//  run: java -cp ../../_lib/out 05-expense-pipeline.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An expense tracker summarises a month. Refunded expenses never count.
//  Write totalsByCategory (category → total cents, categories sorted),
//  topCategories (the n categories with the highest totals, ties by name)
//  and largest (the biggest single non-refunded expense in cents, 0 when
//  there is none). The record Expense is defined above the code you edit.
//
//      totalsByCategory([food 500, rent 9000, food 250, food 100 refunded])
//          → {food=750, rent=9000}
//      topCategories(..., 1)     → [rent]
//      largest([])               → 0
//
//  hint: mapToInt(...).max() gives an OptionalInt, not an int.

import static bootcamp.Check.*;

import java.util.*;
import java.util.stream.*;

class Exercise {

    record Expense(String category, int cents, boolean refunded) {}

    static Map<String, Integer> totalsByCategory(List<Expense> expenses) {
        throw new UnsupportedOperationException("TODO");
    }

    static List<String> topCategories(List<Expense> expenses, int n) {
        throw new UnsupportedOperationException("TODO");
    }

    static int largest(List<Expense> expenses) {
        throw new UnsupportedOperationException("TODO");
    }

    static final List<Expense> MONTH = List.of(
        new Expense("food", 500, false),
        new Expense("rent", 9000, false),
        new Expense("food", 250, false),
        new Expense("food", 100, true),
        new Expense("fun", 12000, true),
        new Expense("bus", 750, false));

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("totals per category", () -> eq(totalsByCategory(MONTH), Map.of("food", 750, "rent", 9000, "bus", 750)));
        test("categories come out sorted", () ->
            eq(new ArrayList<>(totalsByCategory(MONTH).keySet()), List.of("bus", "food", "rent")));
        test("a fully refunded category does not appear", () ->
            ok(!totalsByCategory(MONTH).containsKey("fun"), "fun was refunded"));
        test("topCategories orders by total, ties by name", () ->
            eq(topCategories(MONTH, 3), List.of("rent", "bus", "food")));
        test("topCategories(0) is empty", () -> eq(topCategories(MONTH, 0), List.of()));
        test("largest skips refunds", () -> eq(largest(MONTH), 9000));
        test("largest of nothing is zero", () -> eq(largest(List.of()), 0));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
