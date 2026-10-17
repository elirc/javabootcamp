// ─────────────────────────────────────────────────────────────────────────
//  02 · the missing first page                              ★☆☆ warm-up
//  concepts: debugging · off-by-one · 1-based pages · rounding up
//  run: java -cp ../../_lib/out 02-pagination.java
// ─────────────────────────────────────────────────────────────────────────
//
//  DEBUG HUNT: the code below is complete and compiles; it has bugs. Run
//  it, read the failing tests, find and fix the bugs. Do not edit tests.
//  Intended: page(all, page, size) shows result page `page`, counted
//  from 1. totalPages counts a partial last page (0 for no results);
//  hasNext is page < totalPages; a page past the end has no items.
//  page < 1 or size < 1: IllegalArgumentException.
//  Bug reports:
//    "Page 1 shows results 11-20. Results 1-10 never appear."
//    "With 25 results the pager says 2 pages, and there is no Next
//     button on page 2."
//
//      page(results 0..24, 1, 10) → items 0..9, totalPages 3, hasNext true
//      page(results 0..24, 3, 10) → items 20..24, hasNext false
//
//  hint: write out the indices for page 1 and the last page by hand.

import static bootcamp.Check.*;

import java.util.*;
import java.util.stream.*;

class Exercise {

    record Page<T>(List<T> items, int page, int totalPages, boolean hasNext) {}

    static <T> Page<T> page(List<T> all, int page, int size) {
        if (page < 1 || size < 1) throw new IllegalArgumentException("page and size start at 1");
        int totalPages = all.size() / size;
        int from = page * size;
        int to = Math.min(from + size, all.size());
        List<T> items = from >= all.size() ? List.of() : List.copyOf(all.subList(from, to));
        return new Page<>(items, page, totalPages, page < totalPages);
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        var results = IntStream.range(0, 25).boxed().toList();
        test("page 1 holds the first results", () -> {
            eq(page(results, 1, 10).items(), IntStream.range(0, 10).boxed().toList());
            eq(page(results, 2, 10).items(), IntStream.range(10, 20).boxed().toList());
        });
        test("the last page holds the remainder", () -> {
            eq(page(results, 3, 10).items(), List.of(20, 21, 22, 23, 24));
        });
        test("a partial last page counts as a page", () -> {
            eq(page(results, 1, 10).totalPages(), 3);
            ok(page(results, 2, 10).hasNext(), "page 2 of 3 has a next page");
            ok(!page(results, 3, 10).hasNext(), "page 3 of 3 has no next page");
        });
        test("an exact multiple has no extra page", () -> {
            var twenty = results.subList(0, 20);
            eq(page(twenty, 2, 10).totalPages(), 2);
            ok(!page(twenty, 2, 10).hasNext(), "page 2 of 2 has no next page");
            eq(page(twenty, 3, 10).items(), List.of());
        });
        test("no results means no pages", () -> {
            var p = page(List.<Integer>of(), 1, 10);
            eq(p.items(), List.of());
            eq(p.totalPages(), 0);
            ok(!p.hasNext(), "nothing next");
        });
        test("page and size start at 1", () -> {
            raises(IllegalArgumentException.class, () -> page(results, 0, 10));
            raises(IllegalArgumentException.class, () -> page(results, 1, 0));
            eq(page(results, 25, 1).items(), List.of(24));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
