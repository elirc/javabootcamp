// ─────────────────────────────────────────────────────────────────────────
//  05 · decorator: a price service                          ★★☆ core
//  concepts: decorator pattern · wrapping · composition order · caching
//  run: java -cp ../../_lib/out 05-decorator-prices.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A remote PriceService is slow and flaky. Wrap it in decorators, each a
//  PriceService that holds an inner one:
//    Caching    remembers each sku's price after the first SUCCESS;
//               failures are not cached; misses() counts inner calls
//    Logging    appends "price(<sku>) = <value>" or "price(<sku>) failed:
//               <message>" to its log, then returns or rethrows
//    Retrying   makes up to `attempts` calls in total, retrying on any
//               RuntimeException; when all fail, rethrows the LAST one;
//               attempts < 1 → IllegalArgumentException
//    Fallback   the inner price, or the fixed fallback if inner throws
//  Scripted (given) is a fake inner service: each call takes its next
//  scripted outcome, a BigDecimal to return or a String to throw as a
//  RuntimeException with that message.
//
//      new Caching(new Retrying(flaky, 3))  → one cached price from a
//                                             service that failed twice
//
//  hint: each decorator only knows `inner`; order is decided by the caller.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.util.*;

class Exercise {

    interface PriceService {
        BigDecimal price(String sku);
    }

    static final class Caching implements PriceService {
        private final PriceService inner;
        private final Map<String, BigDecimal> cache = new HashMap<>();
        private int misses;

        Caching(PriceService inner) {
            this.inner = inner;
        }

        public BigDecimal price(String sku) {
            throw new UnsupportedOperationException("TODO");
        }

        int misses() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class Logging implements PriceService {
        private final PriceService inner;
        final List<String> log = new ArrayList<>();

        Logging(PriceService inner) {
            this.inner = inner;
        }

        public BigDecimal price(String sku) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class Retrying implements PriceService {
        private final PriceService inner;
        private final int attempts;

        Retrying(PriceService inner, int attempts) {
            throw new UnsupportedOperationException("TODO");
        }

        public BigDecimal price(String sku) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class Fallback implements PriceService {
        private final PriceService inner;
        private final BigDecimal fallback;

        Fallback(PriceService inner, BigDecimal fallback) {
            this.inner = inner;
            this.fallback = fallback;
        }

        public BigDecimal price(String sku) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    // A fake remote service: each call takes the next outcome.
    static final class Scripted implements PriceService {
        private final Deque<Object> outcomes;
        final List<String> calls = new ArrayList<>();

        Scripted(Object... outcomes) {
            this.outcomes = new ArrayDeque<>(List.of(outcomes));
        }

        public BigDecimal price(String sku) {
            calls.add(sku);
            var next = outcomes.poll();
            if (next == null) throw new IllegalStateException("script exhausted");
            if (next instanceof BigDecimal value) return value;
            throw new RuntimeException((String) next);
        }
    }

    static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("Caching calls the inner service once per sku", () -> {
            var remote = new Scripted(d("9.99"), d("4.50"));
            var cached = new Caching(remote);
            eq(cached.price("A"), d("9.99"));
            eq(cached.price("A"), d("9.99"));
            eq(cached.price("B"), d("4.50"));
            eq(cached.price("B"), d("4.50"));
            eq(remote.calls, List.of("A", "B"));
            eq(cached.misses(), 2);
        });
        test("Caching does not cache failures", () -> {
            var remote = new Scripted("timeout", d("9.99"));
            var cached = new Caching(remote);
            raises(RuntimeException.class, () -> cached.price("A"));
            eq(cached.price("A"), d("9.99"));
            eq(cached.price("A"), d("9.99"));
            eq(cached.misses(), 2);
        });
        test("Logging records successes and failures, and rethrows", () -> {
            var logged = new Logging(new Scripted(d("1.50"), "503 from upstream"));
            eq(logged.price("A"), d("1.50"));
            var e = raises(RuntimeException.class, () -> logged.price("B"));
            eq(e.getMessage(), "503 from upstream");
            eq(logged.log, List.of("price(A) = 1.50", "price(B) failed: 503 from upstream"));
        });
        test("Retrying makes up to `attempts` calls and rethrows the last failure", () -> {
            var remote = new Scripted("first", "second", d("2.00"));
            eq(new Retrying(remote, 3).price("A"), d("2.00"));
            eq(remote.calls.size(), 3);
            var failing = new Scripted("one", "two", "three", d("5"));
            var e = raises(RuntimeException.class, () -> new Retrying(failing, 3).price("A"));
            eq(e.getMessage(), "three");
            eq(failing.calls.size(), 3);
        });
        test("Retrying needs at least one attempt", () -> {
            raises(IllegalArgumentException.class, () -> new Retrying(new Scripted(), 0));
            var once = new Scripted("down");
            raises(RuntimeException.class, () -> new Retrying(once, 1).price("A"));
            eq(once.calls.size(), 1);
        });
        test("Fallback hides a failure behind a fixed price", () -> {
            var safe = new Fallback(new Scripted(d("3.00"), "down"), d("0.00"));
            eq(safe.price("A"), d("3.00"));
            eq(safe.price("A"), d("0.00"));
        });
        test("order matters: Logging outside Caching sees every call, inside only misses", () -> {
            var outside = new Logging(new Caching(new Scripted(d("1"))));
            outside.price("A");
            outside.price("A");
            eq(outside.log.size(), 2);
            var inner = new Logging(new Scripted(d("1")));
            var cached = new Caching(inner);
            cached.price("A");
            cached.price("A");
            eq(inner.log, List.of("price(A) = 1"));
        });
        test("Retrying inside Caching turns two failures into one cached price", () -> {
            var remote = new Scripted("blip", "blip", d("7.25"));
            var service = new Caching(new Retrying(remote, 3));
            eq(service.price("A"), d("7.25"));
            eq(service.price("A"), d("7.25"));
            eq(remote.calls.size(), 3);
            eq(service.misses(), 1);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
