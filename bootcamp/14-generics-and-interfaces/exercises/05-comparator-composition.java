// ─────────────────────────────────────────────────────────────────────────
//  05 · comparator composition                              ★★☆ core
//  concepts: Comparator · thenComparing · nullsLast · reversed · stability
//  run: java -cp ../../_lib/out 05-comparator-composition.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An HR screen sorts people several ways. Build each Comparator by
//  composing Comparator's own factories:
//
//    byDeptThenSeniority()      dept A-Z, then level HIGH to low, then
//                               name A-Z
//    bySalaryNullsLast(desc)    by salary, ascending or descending;
//                               a null salary (unknown) is ALWAYS last
//    explicitOrder(order)       items in the list's order; items not in
//                               the list come after all listed ones and
//                               compare equal to each other
//    caseInsensitiveThenNatural()  ignore case first; on a tie, natural
//                               order (so "Apple" before "apple")
//    isSorted(items, cmp)       true if no item compares above the next
//
//      sort [new, zeta, wip] by explicitOrder([new, wip]) → [new, wip, zeta]
//      [banana, Apple, apple]  → [Apple, apple, banana]
//
//  hint: reversed() reverses everything before it, nulls included.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    // salary is null when it is not known yet
    record Employee(String name, String dept, int level, Integer salary) {}

    static Comparator<Employee> byDeptThenSeniority() {
        throw new UnsupportedOperationException("TODO");
    }

    static Comparator<Employee> bySalaryNullsLast(boolean descending) {
        throw new UnsupportedOperationException("TODO");
    }

    static <T> Comparator<T> explicitOrder(List<? extends T> order) {
        throw new UnsupportedOperationException("TODO");
    }

    static Comparator<String> caseInsensitiveThenNatural() {
        throw new UnsupportedOperationException("TODO");
    }

    static <T> boolean isSorted(List<? extends T> items, Comparator<? super T> cmp) {
        throw new UnsupportedOperationException("TODO");
    }

    static List<String> names(List<Employee> people) {
        return people.stream().map(Employee::name).toList();
    }

    static final List<Employee> STAFF = List.of(
        new Employee("Uma", "ops", 2, 70),
        new Employee("Bo", "dev", 1, null),
        new Employee("Ana", "dev", 3, 95),
        new Employee("Cy", "dev", 3, 90),
        new Employee("Eli", "ops", 4, null),
        new Employee("Dee", "dev", 1, 60));

    static List<Employee> sorted(Comparator<Employee> cmp) {
        var copy = new ArrayList<>(STAFF);
        copy.sort(cmp);
        return copy;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("byDeptThenSeniority sorts by dept, then level high to low, then name", () ->
            eq(names(sorted(byDeptThenSeniority())), List.of("Ana", "Cy", "Bo", "Dee", "Eli", "Uma")));
        test("bySalaryNullsLast ascending puts unknown salaries last", () ->
            eq(names(sorted(bySalaryNullsLast(false))), List.of("Dee", "Uma", "Cy", "Ana", "Bo", "Eli")));
        test("bySalaryNullsLast descending still puts unknown salaries last", () ->
            eq(names(sorted(bySalaryNullsLast(true))), List.of("Ana", "Cy", "Uma", "Dee", "Bo", "Eli")));
        test("explicitOrder follows the list and keeps unlisted items after, in original order", () -> {
            var statuses = new ArrayList<>(List.of("done", "zeta", "new", "alpha", "wip", "new"));
            statuses.sort(explicitOrder(List.of("new", "wip", "done")));
            eq(statuses, List.of("new", "new", "wip", "done", "zeta", "alpha"));
        });
        test("explicitOrder works as a Comparator<Object> over mixed types", () -> {
            var items = new ArrayList<Object>(List.of(3, "b", 1, "a"));
            items.sort(Exercise.<Object>explicitOrder(List.of("a", 1)));
            eq(items, List.of("a", 1, 3, "b"));
        });
        test("caseInsensitiveThenNatural ignores case, then puts upper case first", () -> {
            var words = new ArrayList<>(List.of("banana", "apple", "Banana", "cherry", "Apple"));
            words.sort(caseInsensitiveThenNatural());
            eq(words, List.of("Apple", "apple", "Banana", "banana", "cherry"));
            var unique = new TreeSet<>(caseInsensitiveThenNatural());
            unique.addAll(words);
            eq(unique.size(), 5);
        });
        test("isSorted accepts equal neighbours and rejects a descent", () -> {
            ok(isSorted(List.of(1, 2, 2, 3), Comparator.naturalOrder()));
            ok(!isSorted(List.of(1, 3, 2), Comparator.naturalOrder()));
            ok(isSorted(List.<Integer>of(), Comparator.naturalOrder()));
            ok(isSorted(List.of("a", "bb"), Comparator.<Object>comparingInt(o -> o.toString().length())));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
