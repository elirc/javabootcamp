// ─────────────────────────────────────────────────────────────────────────
//  04 · ConcurrentHashMap compute and merge — SOLUTION      ★★☆ core
//  concepts: ConcurrentHashMap · merge · computeIfPresent · computeIfAbsent
//  run: java -cp ../../_lib/out 04-concurrent-map-ops.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  ConcurrentHashMap is safe for concurrent use, but only call by call.
//  `map.put(k, map.get(k) + n)` is two calls with a gap between them:
//  two threads both read 3, both write 4, and one delivery disappears.
//  The fix is to hand the map a FUNCTION and let it apply it atomically
//  for that key: merge(k, n, Integer::sum) for "add", and compute or
//  computeIfPresent for anything with a condition.
//  reserve shows the full power of the remapping function. It sees the
//  current value, may refuse (return the same value), may update, and may
//  return null to REMOVE the entry, all while no other thread can touch
//  that key. "Reserve only if enough" cannot be expressed with get/put
//  safely at all. computeIfPresent also gives "unknown sku" for free: the
//  function never runs, and nothing is created.
//  computeIfAbsent is the cache-loading idiom: the loader runs at most
//  once per key even when many threads ask at the same moment, and a null
//  result is not stored. Keep the function short and never touch the same
//  map from inside it; that can deadlock or throw.
//  The RivalMap tests run a rival's call inside your get(), the exact gap
//  a get-then-put leaves, so that bug fails on every run. wordCounts and
//  the 8-thread test use real threads and check only totals, which do
//  not depend on the order the threads ran in.

import static bootcamp.Check.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

class Exercise {

    // Given, for the tests (do not change it): a ConcurrentHashMap that lets
    // a rival thread act right after the next read through get(), which
    // getOrDefault and containsKey use too. A get-then-put then loses the
    // rival's update on every run; one atomic map call never reads that way.
    static final class RivalMap<V> extends ConcurrentHashMap<String, V> {
        private Runnable rival;
        boolean rivalRan;

        void rivalAfterNextRead(Runnable action) {
            rival = action;
            rivalRan = false;
        }

        void calm() {
            rival = null;
        }

        @Override
        public V get(Object key) {
            V seen = super.get(key);
            var action = rival;
            if (action != null) {
                rival = null;
                rivalRan = true;
                action.run();
            }
            return seen;
        }
    }

    static final class Stock {
        private final ConcurrentHashMap<String, Integer> onHand;

        Stock() {
            this(new ConcurrentHashMap<>());
        }

        Stock(ConcurrentHashMap<String, Integer> onHand) {
            this.onHand = onHand;
        }

        void receive(String sku, int n) {
            if (n < 1) throw new IllegalArgumentException("receive needs a positive quantity: " + n);
            onHand.merge(sku, n, Integer::sum);
        }

        boolean reserve(String sku, int n) {
            if (n < 1) throw new IllegalArgumentException("reserve needs a positive quantity: " + n);
            var reserved = new boolean[1];
            onHand.computeIfPresent(sku, (k, have) -> {
                if (have < n) return have;
                reserved[0] = true;
                int left = have - n;
                return left == 0 ? null : left;
            });
            return reserved[0];
        }

        int onHand(String sku) {
            return onHand.getOrDefault(sku, 0);
        }

        Set<String> skus() {
            return new TreeSet<>(onHand.keySet());
        }
    }

    static final class Loader {
        private final ConcurrentHashMap<String, String> cache;
        private final Function<String, String> source;
        private final AtomicInteger loads = new AtomicInteger();

        Loader(Function<String, String> source) {
            this(source, new ConcurrentHashMap<>());
        }

        Loader(Function<String, String> source, ConcurrentHashMap<String, String> cache) {
            this.source = source;
            this.cache = cache;
        }

        String get(String key) {
            return cache.computeIfAbsent(key, k -> {
                loads.incrementAndGet();
                return source.apply(k);
            });
        }

        int loads() {
            return loads.get();
        }
    }

    static Map<String, Long> wordCounts(List<String> lines) {
        var counts = new ConcurrentHashMap<String, Long>();
        lines.parallelStream()
            .flatMap(line -> Arrays.stream(line.split(" ")))
            .filter(w -> !w.isEmpty())
            .forEach(w -> counts.merge(w, 1L, Long::sum));
        return counts;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("receive adds to what is on hand, starting from nothing", () -> {
            var stock = new Stock();
            stock.receive("bolt", 3);
            stock.receive("bolt", 4);
            eq(stock.onHand("bolt"), 7);
            eq(stock.onHand("nut"), 0);
        });
        test("reserve takes stock only when there is enough", () -> {
            var stock = new Stock();
            stock.receive("bolt", 3);
            ok(!stock.reserve("bolt", 5));
            eq(stock.onHand("bolt"), 3);
            ok(stock.reserve("bolt", 2));
            eq(stock.onHand("bolt"), 1);
        });
        test("reserving the last unit removes the sku; an unknown sku is false and creates nothing", () -> {
            var stock = new Stock();
            stock.receive("bolt", 2);
            stock.receive("nut", 1);
            ok(stock.reserve("bolt", 2));
            eq(stock.skus(), Set.of("nut"));
            ok(!stock.reserve("ghost", 1));
            eq(stock.skus(), Set.of("nut"));
        });
        test("quantities must be positive", () -> {
            var stock = new Stock();
            raises(IllegalArgumentException.class, () -> stock.receive("bolt", 0));
            raises(IllegalArgumentException.class, () -> stock.reserve("bolt", -1));
            eq(stock.skus(), Set.of());
        });
        test("the loader calls the source once per key and does not cache a null", () -> {
            var loader = new Loader(k -> k.startsWith("x") ? null : k.toUpperCase(Locale.ROOT));
            eq(loader.get("a"), "A");
            eq(loader.get("a"), "A");
            eq(loader.get("x1"), null);
            eq(loader.get("x1"), null);
            eq(loader.loads(), 3);
        });
        test("receive and reserve lose nothing when a rival acts between a read and a write", () -> {
            var map = new RivalMap<Integer>();
            var stock = new Stock(map);
            stock.receive("bolt", 1);
            map.rivalAfterNextRead(() -> stock.receive("bolt", 5));
            stock.receive("bolt", 3);
            map.calm();
            eq(stock.onHand("bolt"), map.rivalRan ? 9 : 4);
            stock.receive("nut", 2);
            var rivalGot = new boolean[1];
            map.rivalAfterNextRead(() -> rivalGot[0] = stock.reserve("nut", 2));
            boolean mine = stock.reserve("nut", 2);
            map.calm();
            eq((mine ? 1 : 0) + (rivalGot[0] ? 1 : 0), 1);
            eq(stock.onHand("nut"), 0);
        });
        test("the loader never loads a key twice, even with a rival between check and load", () -> {
            var map = new RivalMap<String>();
            var loader = new Loader(k -> k.toUpperCase(Locale.ROOT), map);
            var rivalSaw = new String[1];
            map.rivalAfterNextRead(() -> rivalSaw[0] = loader.get("k"));
            eq(loader.get("k"), "K");
            map.calm();
            eq(loader.loads(), 1);
            if (map.rivalRan) eq(rivalSaw[0], "K");
        });
        test("word counts from a parallel stream are exact", () -> {
            var lines = new ArrayList<String>();
            for (int i = 0; i < 2_000; i++) lines.add(i % 2 == 0 ? "to be or not to be" : "be quick");
            eq(wordCounts(lines), Map.of("to", 2_000L, "be", 3_000L, "or", 1_000L, "not", 1_000L, "quick", 1_000L));
        });
        test("eight threads reserving one unit at a time never oversell", () -> {
            var stock = new Stock();
            stock.receive("ticket", 500);
            var sold = new AtomicInteger();
            var pool = Executors.newFixedThreadPool(8);
            try {
                var jobs = new ArrayList<Callable<Void>>();
                for (int t = 0; t < 8; t++) jobs.add(() -> {
                    for (int i = 0; i < 100; i++) if (stock.reserve("ticket", 1)) sold.incrementAndGet();
                    return null;
                });
                for (var f : pool.invokeAll(jobs)) f.get();
            } finally {
                pool.shutdownNow();
            }
            eq(sold.get(), 500);
            eq(stock.onHand("ticket"), 0);
            eq(stock.skus(), Set.of());
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
