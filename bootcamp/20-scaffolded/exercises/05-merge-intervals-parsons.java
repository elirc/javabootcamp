// ─────────────────────────────────────────────────────────────────────────
//  05 · merge booked intervals (Parsons)                    ★★☆ core
//  concepts: Parsons · sorting · sweep · intervals · defensive copies
//  run: java -cp ../../_lib/out 05-merge-intervals-parsons.java
// ─────────────────────────────────────────────────────────────────────────
//
//  PARSONS: arrange the SCRAMBLED lines into the body of merge. Lines
//  marked (?) come in two pairs; of each pair exactly ONE belongs.
//  merge(intervals) returns the busy periods of a calendar: intervals
//  (closed, start <= end) that overlap or touch ([1,3] and [3,5]) become
//  one, sorted by start. The input may be unsorted or immutable and must
//  not be modified.
//
//      merged.add(next);
//      var sorted = intervals;                                  (?)
//      } else {
//      return merged;
//      var merged = new ArrayList<Interval>();
//      int end = next.end();                                    (?)
//      for (var next : sorted) {
//      var last = merged.isEmpty() ? null : merged.get(merged.size() - 1);
//      }
//      sorted.sort(Comparator.comparingInt(Interval::start));
//      if (last != null && next.start() <= last.end()) {
//      var sorted = new ArrayList<>(intervals);                 (?)
//      }
//      merged.set(merged.size() - 1, new Interval(last.start(), end));
//      int end = Math.max(last.end(), next.end());              (?)
//
//      merge([[8,10], [1,3], [2,6], [15,18]]) → [[1,6], [8,10], [15,18]]
//
//  hint: what if the next interval sits entirely inside the last one?

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Interval(int start, int end) {}

    static List<Interval> merge(List<Interval> intervals) {
        throw new UnsupportedOperationException("TODO");
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
