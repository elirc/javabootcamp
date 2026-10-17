// ─────────────────────────────────────────────────────────────────────────
//  02 · the missing first page — SOLUTION                   ★☆☆ warm-up
//  concepts: debugging · off-by-one · 1-based pages · rounding up
//  run: java -cp ../../_lib/out 02-pagination.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Two off-by-ones, which always live where two counting systems meet:
//  humans count pages from 1, list indices start at 0.
//  1. Reproduce "page 1 shows 11-20": page(results, 1, 10). Narrow it by
//     printing from and to: from is 10. page * size is the start of the
//     page AFTER this one when pages are 1-based. Fix: (page - 1) * size.
//     Pinned by "page 1 holds the first results"; "page and size start
//     at 1" (page 25 of size 1 is result 24) catches the same slip.
//  2. Reproduce "2 pages for 25 results": 25 / 10 is 2 in integer
//     division; the partial page is thrown away, and hasNext on page 2
//     is computed from the wrong total. Fix: round UP, (n + size - 1) /
//     size, which is also 0 for no results. Pinned by "a partial last
//     page counts as a page" and, for the boundary on the other side,
//     "an exact multiple has no extra page" (Math.ceil on doubles is the
//     other fix; the integer form avoids floating point entirely).
//  Habit: for any range code, check three cases by hand: the first
//  page, the last partial page, and an exact multiple. Most off-by-ones
//  fail one of the three.

import static bootcamp.Check.*;

import java.util.*;
import java.util.stream.*;

class Exercise {

    record Page<T>(List<T> items, int page, int totalPages, boolean hasNext) {}

    static <T> Page<T> page(List<T> all, int page, int size) {
        if (page < 1 || size < 1) throw new IllegalArgumentException("page and size start at 1");
        int totalPages = (all.size() + size - 1) / size;
        int from = (page - 1) * size;
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
