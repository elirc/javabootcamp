// ─────────────────────────────────────────────────────────────────────────
//  06 · a generic repository — SOLUTION                     ★★☆ core
//  concepts: generic classes · recursive bounds · Entity<ID> · paging
//  run: java -cp ../../_lib/out 06-generic-repository.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  The class signature does the design work:
//  InMemoryRepository<T extends Entity<ID>, ID extends Comparable<? super
//  ID>>. "T extends Entity<ID>" ties the row type to its id type, so a
//  repository of Customers only accepts Long ids in findById; "ID extends
//  Comparable" is what lets a TreeMap keep the rows sorted by id. One
//  class, checked for every entity type, with no casts: that is the point
//  of generics over a repository of Object.
//  Long ids sort numerically (9 before 10 before 100) only because the
//  TreeMap compares Longs. Keying by id.toString() would give "10" < "9",
//  a bug you meet in real code that builds cache keys from strings.
//  saveAll validates the whole batch before touching the map, which is
//  the in-memory version of a transaction: half a batch saved is worse
//  than none, because the caller cannot tell which half.
//  page computes a window over findAll(). Clamp both ends to the size:
//  from = min(i * size, n), to = min(from + size, n), so a page past the
//  end is an empty list rather than an IndexOutOfBoundsException.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    interface Entity<ID> {
        ID id();
    }

    record Customer(Long id, String name, String city) implements Entity<Long> {}

    record Sku(String id, int stock) implements Entity<String> {}

    static final class InMemoryRepository<T extends Entity<ID>, ID extends Comparable<? super ID>> {
        private final Map<ID, T> rows = new TreeMap<>();

        Optional<T> save(T entity) {
            if (entity.id() == null) throw new IllegalArgumentException("entity has no id: " + entity);
            return Optional.ofNullable(rows.put(entity.id(), entity));
        }

        void saveAll(Collection<? extends T> entities) {
            for (T e : entities) {
                if (e.id() == null) throw new IllegalArgumentException("entity has no id: " + e);
            }
            for (T e : entities) rows.put(e.id(), e);
        }

        Optional<T> findById(ID id) {
            return Optional.ofNullable(rows.get(id));
        }

        List<T> findAll() {
            return new ArrayList<>(rows.values());
        }

        List<T> findWhere(Predicate<? super T> p) {
            return rows.values().stream().filter(p).toList();
        }

        int count() {
            return rows.size();
        }

        boolean deleteById(ID id) {
            return rows.remove(id) != null;
        }

        <R> List<R> project(Function<? super T, ? extends R> f) {
            var out = new ArrayList<R>();
            for (T row : rows.values()) out.add(f.apply(row));
            return out;
        }

        List<T> page(int index, int size) {
            if (index < 0 || size < 1) throw new IllegalArgumentException("bad page " + index + " of size " + size);
            var all = findAll();
            int from = (int) Math.min((long) index * size, all.size());
            int to = Math.min(from + size, all.size());
            return new ArrayList<>(all.subList(from, to));
        }
    }

    static InMemoryRepository<Customer, Long> customers(long... ids) {
        var repo = new InMemoryRepository<Customer, Long>();
        for (long id : ids) repo.save(new Customer(id, "c" + id, id % 2 == 0 ? "Oslo" : "Lima"));
        return repo;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("save inserts, then replaces and returns the previous row", () -> {
            var repo = new InMemoryRepository<Customer, Long>();
            eq(repo.save(new Customer(1L, "Ana", "Oslo")), Optional.empty());
            eq(repo.save(new Customer(1L, "Ana B", "Lima")), Optional.of(new Customer(1L, "Ana", "Oslo")));
            eq(repo.findById(1L), Optional.of(new Customer(1L, "Ana B", "Lima")));
            eq(repo.count(), 1);
            eq(repo.findById(2L), Optional.empty());
        });
        test("save rejects an entity with a null id", () -> {
            var repo = new InMemoryRepository<Customer, Long>();
            raises(IllegalArgumentException.class, () -> repo.save(new Customer(null, "Nobody", "Oslo")));
            eq(repo.count(), 0);
        });
        test("findAll and findWhere are in id order, numeric for Long ids", () -> {
            var repo = customers(10, 9, 100, 2);
            eq(repo.project(Customer::id), List.of(2L, 9L, 10L, 100L));
            eq(repo.findWhere(c -> c.city().equals("Oslo")).stream().map(Customer::id).toList(), List.of(2L, 10L, 100L));
            eq(repo.findAll().size(), 4);
        });
        test("the same class serves String ids, in A-Z order", () -> {
            var repo = new InMemoryRepository<Sku, String>();
            repo.save(new Sku("B-7", 3));
            repo.save(new Sku("A-9", 0));
            repo.save(new Sku("A-10", 5));
            eq(repo.project(Sku::id), List.of("A-10", "A-9", "B-7"));
            eq(repo.findById("A-9").map(Sku::stock), Optional.of(0));
        });
        test("saveAll is all or nothing when one entity has a null id", () -> {
            var repo = customers(1);
            var batch = List.of(new Customer(2L, "Bo", "Oslo"), new Customer(null, "?", "Lima"), new Customer(3L, "Cy", "Lima"));
            raises(IllegalArgumentException.class, () -> repo.saveAll(batch));
            eq(repo.project(Customer::id), List.of(1L));
            repo.saveAll(List.of(new Customer(3L, "Cy", "Lima"), new Customer(2L, "Bo", "Oslo")));
            eq(repo.project(Customer::id), List.of(1L, 2L, 3L));
        });
        test("deleteById reports whether a row was removed", () -> {
            var repo = customers(1, 2);
            ok(repo.deleteById(1L));
            ok(!repo.deleteById(1L));
            ok(!repo.deleteById(99L));
            eq(repo.count(), 1);
        });
        test("project accepts a Function over a supertype", () -> {
            var repo = customers(3, 1);
            Function<Object, String> show = o -> o.getClass().getSimpleName();
            eq(repo.project(show), List.of("Customer", "Customer"));
            List<Object> ids = repo.<Object>project(Customer::id);
            eq(ids, List.of(1L, 3L));
        });
        test("page slices id order; past the end is empty; bad arguments throw", () -> {
            var repo = customers(5, 3, 1, 4, 2);
            eq(repo.page(0, 2).stream().map(Customer::id).toList(), List.of(1L, 2L));
            eq(repo.page(1, 2).stream().map(Customer::id).toList(), List.of(3L, 4L));
            eq(repo.page(2, 2).stream().map(Customer::id).toList(), List.of(5L));
            eq(repo.page(3, 2), List.of());
            raises(IllegalArgumentException.class, () -> repo.page(0, 0));
            raises(IllegalArgumentException.class, () -> repo.page(-1, 2));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
