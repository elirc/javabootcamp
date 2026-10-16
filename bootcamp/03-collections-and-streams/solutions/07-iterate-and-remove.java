// ─────────────────────────────────────────────────────────────────────────
//  07 · remove while you walk — SOLUTION                    ★★☆ core
//  concepts: Iterator.remove · fail-fast iterators · Map.replaceAll
//  run: java -cp ../../_lib/out 07-iterate-and-remove.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  `for (var loan : loans) if (...) loans.remove(loan);` compiles and
//  then throws ConcurrentModificationException — usually. The for-each
//  loop runs an Iterator, and ArrayList's iterator is fail-fast: any
//  structural change made behind its back is detected on the next call.
//  (Sometimes it doesn't throw — removing the second-to-last element ends
//  the loop early instead, silently skipping the last item. Fail-fast is
//  best-effort, which is worse than always failing.)
//  The tools that work: an explicit Iterator and it.remove() (the one
//  removal the iterator knows about), or removeIf when you don't need the
//  removed items. Here we need them, so the explicit iterator it is:
//  collect into `returned`, then it.remove().
//  Map values can be changed in place without touching the key set:
//  replaceAll((key, value) -> value + days) rewrites every value in one
//  pass. Calling put for an EXISTING key while iterating entrySet is
//  allowed too (it is not a structural change), but replaceAll says it
//  better. Adding or removing keys mid-iteration is what throws.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Loan(String isbn, int dueDay) {}

    static List<Loan> returnOverdue(List<Loan> loans, int today) {
        var returned = new ArrayList<Loan>();
        for (var it = loans.iterator(); it.hasNext(); ) {
            var loan = it.next();
            if (loan.dueDay() < today) {
                returned.add(loan);
                it.remove();
            }
        }
        return returned;
    }

    static void renewAll(Map<String, Integer> dueByIsbn, int days) {
        dueByIsbn.replaceAll((isbn, due) -> due + days);
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("returns the overdue loans in order", () -> {
            var loans = new ArrayList<>(List.of(new Loan("L1", 3), new Loan("L2", 9), new Loan("L3", 4)));
            eq(returnOverdue(loans, 5), List.of(new Loan("L1", 3), new Loan("L3", 4)));
        });
        test("keeps the loans that are not overdue", () -> {
            var loans = new ArrayList<>(List.of(new Loan("L1", 3), new Loan("L2", 9), new Loan("L3", 4)));
            returnOverdue(loans, 5);
            eq(loans, List.of(new Loan("L2", 9)));
        });
        test("a loan due today is not overdue", () -> {
            var loans = new ArrayList<>(List.of(new Loan("L1", 5)));
            eq(returnOverdue(loans, 5), List.of());
            eq(loans.size(), 1);
        });
        test("handles overdue loans next to each other", () -> {
            var loans = new ArrayList<>(List.of(new Loan("a", 1), new Loan("b", 2), new Loan("c", 3), new Loan("d", 9)));
            eq(returnOverdue(loans, 5).size(), 3);
            eq(loans, List.of(new Loan("d", 9)));
        });
        test("renewAll pushes every due day", () -> {
            var due = new HashMap<>(Map.of("A", 3, "B", 10));
            renewAll(due, 7);
            eq(due, Map.of("A", 10, "B", 17));
        });
        test("renewAll on an empty map is fine", () -> {
            var due = new HashMap<String, Integer>();
            renewAll(due, 7);
            eq(due, Map.of());
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
