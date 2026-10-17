// ─────────────────────────────────────────────────────────────────────────
//  05 · comparator composition — SOLUTION                   ★★☆ core
//  concepts: Comparator · thenComparing · nullsLast · reversed · stability
//  run: java -cp ../../_lib/out 05-comparator-composition.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Comparator is built for composition: comparing(key), then
//  thenComparing(key) for tie-breaks, each optionally with its own
//  comparator for that key. So "level high to low" is
//  thenComparing(Employee::level, Comparator.reverseOrder()), which
//  reverses ONE key. Calling .reversed() on the chain instead reverses
//  the dept and name order too: the classic composition bug.
//  The same trap hides in nulls: nullsLast(naturalOrder()).reversed()
//  moves the nulls to the FRONT, because reversed flips the whole
//  comparator. Put the direction inside: nullsLast(reverseOrder()).
//  explicitOrder ranks each item by its index and gives every unlisted
//  item the same rank (Integer.MAX_VALUE). Because List.sort is a stable
//  merge sort, equal-rank items keep their original order, which is the
//  behaviour the tests pin.
//  String.CASE_INSENSITIVE_ORDER.thenComparing(naturalOrder()) is a total
//  order; case-insensitive alone calls "Apple" and "apple" equal, and a
//  TreeSet built on it would silently drop one of them.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    // salary is null when it is not known yet
    record Employee(String name, String dept, int level, Integer salary) {}

    static Comparator<Employee> byDeptThenSeniority() {
        return Comparator.comparing(Employee::dept)
            .thenComparing(Employee::level, Comparator.reverseOrder())
            .thenComparing(Employee::name);
    }

    static Comparator<Employee> bySalaryNullsLast(boolean descending) {
        Comparator<Integer> direction = descending ? Comparator.reverseOrder() : Comparator.naturalOrder();
        return Comparator.comparing(Employee::salary, Comparator.nullsLast(direction));
    }

    static <T> Comparator<T> explicitOrder(List<? extends T> order) {
        var rank = new HashMap<Object, Integer>();
        for (int i = 0; i < order.size(); i++) rank.putIfAbsent(order.get(i), i);
        return Comparator.comparingInt(item -> rank.getOrDefault(item, Integer.MAX_VALUE));
    }

    static Comparator<String> caseInsensitiveThenNatural() {
        return String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder());
    }

    static <T> boolean isSorted(List<? extends T> items, Comparator<? super T> cmp) {
        for (int i = 1; i < items.size(); i++) {
            if (cmp.compare(items.get(i - 1), items.get(i)) > 0) return false;
        }
        return true;
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
