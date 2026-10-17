// ─────────────────────────────────────────────────────────────────────────
//  02 · atomic counters and CAS loops                       ★☆☆ warm-up
//  concepts: AtomicInteger · AtomicLong · LongAdder · compare-and-set loops
//  run: java -cp ../../_lib/out 02-atomic-counters.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A web server's request metrics and a concert's ticket pool, both safe
//  to call from many threads without a lock. Finish:
//
//    Metrics.record(ms)   count one request and add its ms (ms < 0 →
//                         IllegalArgumentException, nothing recorded);
//                         requests(), totalMillis(), slowest() (0 before
//                         any request), averageMillis() (0.0 before any)
//    TicketPool(n)        n < 0 → IllegalArgumentException
//    tryTake()            take one ticket if any is left; the count never
//                         goes below zero
//    takeUpTo(n)          atomically take min(n, left) and return how many
//                         were taken; n < 0 → IllegalArgumentException
//    left()               tickets left
//
//  Two tests run real threads (4 x 10,000 records; 8 threads racing for
//  1,000 tickets); correct code gives the exact totals every time.
//
//  hint: compareAndSet(expected, next) fails if someone else moved first.

import static bootcamp.Check.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

class Exercise {

    static final class Metrics {
        private final LongAdder requests = new LongAdder();
        private final LongAdder totalMillis = new LongAdder();
        private final AtomicLong slowest = new AtomicLong();

        void record(long millis) {
            throw new UnsupportedOperationException("TODO");
        }

        long requests() {
            throw new UnsupportedOperationException("TODO");
        }

        long totalMillis() {
            throw new UnsupportedOperationException("TODO");
        }

        long slowest() {
            throw new UnsupportedOperationException("TODO");
        }

        double averageMillis() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class TicketPool {
        private final AtomicInteger left;

        TicketPool(int tickets) {
            throw new UnsupportedOperationException("TODO");
        }

        boolean tryTake() {
            throw new UnsupportedOperationException("TODO");
        }

        int takeUpTo(int n) {
            throw new UnsupportedOperationException("TODO");
        }

        int left() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("record counts requests and adds up their time", () -> {
            var m = new Metrics();
            m.record(120);
            m.record(30);
            eq(m.requests(), 2);
            eq(m.totalMillis(), 150);
        });
        test("slowest keeps the maximum in any arrival order", () -> {
            var m = new Metrics();
            eq(m.slowest(), 0);
            for (long ms : new long[] {5, 90, 12, 90, 3}) m.record(ms);
            eq(m.slowest(), 90);
        });
        test("averageMillis is 0.0 before any request, then exact; negatives are refused", () -> {
            var m = new Metrics();
            approx(m.averageMillis(), 0.0);
            m.record(1);
            m.record(2);
            approx(m.averageMillis(), 1.5);
            raises(IllegalArgumentException.class, () -> m.record(-1));
            eq(m.requests(), 2);
        });
        test("tryTake hands out exactly the tickets there are, never below zero", () -> {
            var pool = new TicketPool(2);
            ok(pool.tryTake());
            ok(pool.tryTake());
            ok(!pool.tryTake());
            ok(!pool.tryTake());
            eq(pool.left(), 0);
        });
        test("takeUpTo takes what is left when asked for more", () -> {
            var pool = new TicketPool(5);
            eq(pool.takeUpTo(3), 3);
            eq(pool.takeUpTo(3), 2);
            eq(pool.takeUpTo(3), 0);
            eq(pool.left(), 0);
            raises(IllegalArgumentException.class, () -> pool.takeUpTo(-1));
            raises(IllegalArgumentException.class, () -> new TicketPool(-1));
        });
        test("four threads recording 10,000 times each lose no update", () -> {
            var m = new Metrics();
            eq(m.requests(), 0);
            var pool = Executors.newFixedThreadPool(4);
            try {
                var jobs = new ArrayList<Callable<Void>>();
                for (int t = 0; t < 4; t++) jobs.add(() -> {
                    for (int i = 1; i <= 10_000; i++) m.record(i % 100);
                    return null;
                });
                for (var f : pool.invokeAll(jobs)) f.get();
            } finally {
                pool.shutdownNow();
            }
            eq(m.requests(), 40_000);
            eq(m.totalMillis(), 4 * 495_000);
            eq(m.slowest(), 99);
        });
        test("eight threads racing for 1,000 tickets take exactly 1,000", () -> {
            var tickets = new TicketPool(1_000);
            var taken = new AtomicInteger();
            var pool = Executors.newFixedThreadPool(8);
            try {
                var jobs = new ArrayList<Callable<Void>>();
                for (int t = 0; t < 8; t++) jobs.add(() -> {
                    while (tickets.tryTake()) taken.incrementAndGet();
                    return null;
                });
                for (var f : pool.invokeAll(jobs)) f.get();
            } finally {
                pool.shutdownNow();
            }
            eq(taken.get(), 1_000);
            eq(tickets.left(), 0);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
