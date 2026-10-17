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
//  wordCounts runs on a parallel stream to prove the point: merge gives
//  the exact count under real concurrency, and the test checks totals,
//  which do not depend on the order threads ran in.

import static bootcamp.Check.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

class Exercise {

    static final class Stock {
        private final ConcurrentHashMap<String, Integer> onHand = new ConcurrentHashMap<>();

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
        private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
        private final Function<String, String> source;
        private final AtomicInteger loads = new AtomicInteger();

        Loader(Function<String, String> source) {
            this.source = source;
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
        test("reserving the last unit removes the sku", () -> {
            var stock = new Stock();
            stock.receive("bolt", 2);
            stock.receive("nut", 1);
            ok(stock.reserve("bolt", 2));
            eq(stock.skus(), Set.of("nut"));
        });
        test("reserving an unknown sku is false and creates nothing", () -> {
            var stock = new Stock();
            ok(!stock.reserve("ghost", 1));
            eq(stock.skus(), Set.of());
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
