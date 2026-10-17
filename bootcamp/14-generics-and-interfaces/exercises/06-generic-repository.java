// ─────────────────────────────────────────────────────────────────────────
//  06 · a generic repository                                ★★☆ core
//  concepts: generic classes · recursive bounds · Entity<ID> · paging
//  run: java -cp ../../_lib/out 06-generic-repository.java
// ─────────────────────────────────────────────────────────────────────────
//
//  One repository class serves Customers (Long ids) and Skus (String
//  ids). Finish InMemoryRepository. Every list it returns is in id order:
//  numeric for Long, A-Z for String.
//
//    save(e)          insert or replace; returns the previous row, or
//                     empty; a null id throws IllegalArgumentException
//    saveAll(es)      all or nothing: if any id is null, throw
//                     IllegalArgumentException and save none of them
//    findById, findAll, findWhere(p), count()
//    deleteById(id)   true if a row was removed
//    project(f)       f applied to every row
//    page(i, size)    rows i*size .. i*size+size-1 (i from 0); past the
//                     end gives []; i < 0 or size < 1 throws
//                     IllegalArgumentException
//
//      ids saved as 10, 9, 100   → findAll ids [9, 10, 100]
//
//  hint: check every entity before you store the first one.

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
            throw new UnsupportedOperationException("TODO");
        }

        void saveAll(Collection<? extends T> entities) {
            throw new UnsupportedOperationException("TODO");
        }

        Optional<T> findById(ID id) {
            throw new UnsupportedOperationException("TODO");
        }

        List<T> findAll() {
            throw new UnsupportedOperationException("TODO");
        }

        List<T> findWhere(Predicate<? super T> p) {
            throw new UnsupportedOperationException("TODO");
        }

        int count() {
            throw new UnsupportedOperationException("TODO");
        }

        boolean deleteById(ID id) {
            throw new UnsupportedOperationException("TODO");
        }

        <R> List<R> project(Function<? super T, ? extends R> f) {
            throw new UnsupportedOperationException("TODO");
        }

        List<T> page(int index, int size) {
            throw new UnsupportedOperationException("TODO");
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
