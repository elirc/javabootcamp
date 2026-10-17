// ─────────────────────────────────────────────────────────────────────────
//  04 · ConcurrentHashMap compute and merge                 ★★☆ core
//  concepts: ConcurrentHashMap · merge · computeIfPresent · computeIfAbsent
//  run: java -cp ../../_lib/out 04-concurrent-map-ops.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A warehouse shared by many request threads. Every update must be ONE
//  atomic map call, never a get followed by a put. Finish:
//    receive(sku, n)   add n to what is on hand (n < 1 →
//                      IllegalArgumentException)
//    reserve(sku, n)   take n only if at least n are on hand, and say
//                      whether it did; taking the last unit REMOVES the
//                      sku; an unknown sku → false and nothing is created;
//                      n < 1 → IllegalArgumentException
//    onHand(sku), skus()   current quantity (0 if absent), stocked skus
//    Loader.get(key)   load through computeIfAbsent: the source runs once
//                      per key; if it returns null nothing is cached, so
//                      the next get tries again; loads() counts source calls
//    wordCounts(lines) counts of each space-separated word, built from a
//                      PARALLEL stream into a ConcurrentHashMap
//  Two tests hand Stock and Loader the given RivalMap, which runs a rival's
//  call right after your next get(): a get-then-put fails on every run.
//
//      receive(bolt, 3); reserve(bolt, 5) → false; reserve(bolt, 3) → true
//
//  hint: a remapping function that returns null removes the entry.

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
            throw new UnsupportedOperationException("TODO");
        }

        boolean reserve(String sku, int n) {
            throw new UnsupportedOperationException("TODO");
        }

        int onHand(String sku) {
            throw new UnsupportedOperationException("TODO");
        }

        Set<String> skus() {
            throw new UnsupportedOperationException("TODO");
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
            throw new UnsupportedOperationException("TODO");
        }

        int loads() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static Map<String, Long> wordCounts(List<String> lines) {
        throw new UnsupportedOperationException("TODO");
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
