// ─────────────────────────────────────────────────────────────────────────
//  05 · merge booked intervals (Parsons) — SOLUTION         ★★☆ core
//  concepts: Parsons · sorting · sweep · intervals · defensive copies
//  run: java -cp ../../_lib/out 05-merge-intervals-parsons.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Sort by start, then sweep once: every interval either extends the
//  last merged one (it starts at or before that one's end) or starts a
//  new busy period. Sorting is what makes one pass enough: after it, an
//  interval can only overlap the LAST merged one, never an earlier one.
//  Pair 1: copy before sorting. "var sorted = intervals" sorts the
//  caller's list in place (a surprise side effect), and on a List.of
//  input sort throws UnsupportedOperationException.
//  Pair 2: the merged end is the max of both ends. "next.end()" is right
//  for a partial overlap but SHRINKS the period when next sits inside
//  last ([1,10] then [2,3] would become [1,3]).
//  "<=" rather than "<" is the touching rule: [1,3] and [3,5] share the
//  instant 3. Whether touching intervals merge is a domain decision
//  (meeting rooms booked 9-10 and 10-11 are back to back, not double
//  booked); the code makes it in one comparison. Sort-then-sweep is the
//  shape of most range problems: free/busy calendars, range partitions
//  (module 11), merging sorted runs.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Interval(int start, int end) {}

    static List<Interval> merge(List<Interval> intervals) {
        var sorted = new ArrayList<>(intervals);
        sorted.sort(Comparator.comparingInt(Interval::start));
        var merged = new ArrayList<Interval>();
        for (var next : sorted) {
            var last = merged.isEmpty() ? null : merged.get(merged.size() - 1);
            if (last != null && next.start() <= last.end()) {
                int end = Math.max(last.end(), next.end());
                merged.set(merged.size() - 1, new Interval(last.start(), end));
            } else {
                merged.add(next);
            }
        }
        return merged;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("merges overlapping intervals", () -> {
            eq(merge(new ArrayList<>(List.of(new Interval(1, 3), new Interval(2, 6), new Interval(8, 10)))),
                List.of(new Interval(1, 6), new Interval(8, 10)));
        });
        test("merges intervals that only touch", () -> {
            eq(merge(new ArrayList<>(List.of(new Interval(1, 3), new Interval(3, 5)))), List.of(new Interval(1, 5)));
            eq(merge(new ArrayList<>(List.of(new Interval(1, 3), new Interval(4, 5)))),
                List.of(new Interval(1, 3), new Interval(4, 5)));
        });
        test("sorts unsorted input by start", () -> {
            eq(merge(new ArrayList<>(List.of(new Interval(8, 10), new Interval(1, 3), new Interval(2, 6), new Interval(15, 18)))),
                List.of(new Interval(1, 6), new Interval(8, 10), new Interval(15, 18)));
        });
        test("an interval inside another disappears into it", () -> {
            eq(merge(new ArrayList<>(List.of(new Interval(1, 10), new Interval(2, 3), new Interval(4, 12)))),
                List.of(new Interval(1, 12)));
            eq(merge(new ArrayList<>(List.of(new Interval(1, 10), new Interval(2, 3)))), List.of(new Interval(1, 10)));
        });
        test("accepts immutable input and leaves the input unchanged", () -> {
            var input = List.of(new Interval(5, 6), new Interval(1, 2));
            eq(merge(input), List.of(new Interval(1, 2), new Interval(5, 6)));
            var mutable = new ArrayList<>(input);
            merge(mutable);
            eq(mutable, input);
        });
        test("no intervals, no busy periods", () -> {
            eq(merge(List.of()), List.of());
            eq(merge(List.of(new Interval(4, 4))), List.of(new Interval(4, 4)));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
