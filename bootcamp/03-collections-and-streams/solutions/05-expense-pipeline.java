// ─────────────────────────────────────────────────────────────────────────
//  05 · where did the money go — SOLUTION                   ★★☆ core
//  concepts: stream pipelines · filter/map/reduce · summingInt · OptionalInt
//  run: java -cp ../../_lib/out 05-expense-pipeline.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A stream pipeline reads like the sentence that specifies it: from the
//  expenses, keep the ones not refunded, group by category, sum the
//  cents. Write the filter FIRST so every later step only sees what
//  counts — a category whose only expense was refunded then never
//  appears at all, which is what the tests expect.
//  groupingBy(Expense::category, TreeMap::new, summingInt(Expense::cents))
//  does grouping, ordering and summing in one collector.
//  topCategories reuses totalsByCategory (don't re-implement the filter
//  twice — that is how the two numbers drift apart) and then sorts entries
//  by value descending, name ascending, limits, and maps to keys.
//  largest shows the primitive streams: mapToInt avoids boxing and gives
//  max(), sum(), average() directly. max() returns OptionalInt because an
//  empty stream HAS no maximum; orElse(0) states the business rule ("no
//  expenses, largest is zero") explicitly instead of hiding it in a
//  sentinel like Integer.MIN_VALUE.

import static bootcamp.Check.*;

import java.util.*;
import java.util.stream.*;

class Exercise {

    record Expense(String category, int cents, boolean refunded) {}

    static Map<String, Integer> totalsByCategory(List<Expense> expenses) {
        return expenses.stream()
            .filter(e -> !e.refunded())
            .collect(Collectors.groupingBy(Expense::category, TreeMap::new, Collectors.summingInt(Expense::cents)));
    }

    static List<String> topCategories(List<Expense> expenses, int n) {
        return totalsByCategory(expenses).entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()))
            .limit(n)
            .map(Map.Entry::getKey)
            .toList();
    }

    static int largest(List<Expense> expenses) {
        return expenses.stream().filter(e -> !e.refunded()).mapToInt(Expense::cents).max().orElse(0);
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
