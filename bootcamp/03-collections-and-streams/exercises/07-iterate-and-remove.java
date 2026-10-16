// ─────────────────────────────────────────────────────────────────────────
//  07 · remove while you walk                               ★★☆ core
//  concepts: Iterator.remove · fail-fast iterators · Map.replaceAll
//  run: java -cp ../../_lib/out 07-iterate-and-remove.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A library tracker closes the day. Write returnOverdue: remove from the
//  (mutable) list every loan whose dueDay is before today, and return the
//  removed loans in their original order; a loan due today is not overdue.
//  Then write renewAll: push every due day in the map later by `days`, in
//  place. The record Loan is defined above the code you edit.
//
//      loans = [L1 due 3, L2 due 9, L3 due 4];  returnOverdue(loans, 5)
//          → returns [L1, L3], loans is now [L2]
//      renewAll({A=3, B=10}, 7)  → the map is now {A=10, B=17}
//
//  hint: a for-each loop cannot survive list.remove(...) inside it.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Loan(String isbn, int dueDay) {}

    static List<Loan> returnOverdue(List<Loan> loans, int today) {
        throw new UnsupportedOperationException("TODO");
    }

    static void renewAll(Map<String, Integer> dueByIsbn, int days) {
        throw new UnsupportedOperationException("TODO");
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
