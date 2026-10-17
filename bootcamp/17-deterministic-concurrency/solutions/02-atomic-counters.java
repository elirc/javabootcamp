// ─────────────────────────────────────────────────────────────────────────
//  02 · atomic counters and CAS loops — SOLUTION            ★☆☆ warm-up
//  concepts: AtomicInteger · AtomicLong · LongAdder · compare-and-set loops
//  run: java -cp ../../_lib/out 02-atomic-counters.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  java.util.concurrent.atomic gives single variables that update without
//  a lock: incrementAndGet and addAndGet are read-modify-write in one
//  indivisible step. LongAdder is the better counter when many threads
//  write and few read: it spreads increments over cells and adds them up
//  in sum(), which is why metrics libraries use it.
//  "Keep the maximum" is a read-modify-write too: get() then set() can let
//  a smaller value overwrite a bigger one that arrived in between.
//  accumulateAndGet(ms, Math::max) runs the CAS retry loop for you.
//  The ticket pool needs a condition ("only if one is left"), and no
//  single atomic method says that, so you write the CAS loop yourself:
//  read, decide, compareAndSet(read, read - 1), retry if it failed. The
//  tempting `if (left.get() > 0) left.decrementAndGet()` is check-then-
//  act: two threads both see 1 and both decrement. getAndDecrement() > 0
//  is wrong a different way: it returns the right answer but leaves the
//  count negative.
//  The count sits behind a Counter so a test can play the rival thread:
//  RivalCounter takes a ticket right after your read, the moment a check-
//  then-act or a stale write breaks, so those bugs fail on EVERY run. The
//  threaded tests only show the totals; a racy max in slowest() can still
//  pass them by luck, which is why you check the design, not the luck.

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
            if (millis < 0) throw new IllegalArgumentException("negative duration: " + millis);
            requests.increment();
            totalMillis.add(millis);
            slowest.accumulateAndGet(millis, Math::max);
        }

        long requests() {
            return requests.sum();
        }

        long totalMillis() {
            return totalMillis.sum();
        }

        long slowest() {
            return slowest.get();
        }

        double averageMillis() {
            long n = requests.sum();
            return n == 0 ? 0.0 : (double) totalMillis.sum() / n;
        }
    }

    // The pool's shared count, behind an interface so a test can play the
    // rival thread. Counter.atomic(n) is the real thing: an AtomicInteger.
    interface Counter {
        int get();
        void set(int value);
        boolean compareAndSet(int expected, int next);
        int getAndDecrement();
        int decrementAndGet();
        int addAndGet(int delta);

        static Counter atomic(int initial) {
            var a = new AtomicInteger(initial);
            return new Counter() {
                public int get() { return a.get(); }
                public void set(int value) { a.set(value); }
                public boolean compareAndSet(int expected, int next) { return a.compareAndSet(expected, next); }
                public int getAndDecrement() { return a.getAndDecrement(); }
                public int decrementAndGet() { return a.decrementAndGet(); }
                public int addAndGet(int delta) { return a.addAndGet(delta); }
            };
        }
    }

    // Given, for the tests (do not change it): a Counter where a rival thread
    // takes one ticket right after each of your first `rivalTakes` reads that
    // see a ticket. That is the exact moment a check-then-act or a stale
    // write goes wrong, and here it happens on every run.
    static final class RivalCounter implements Counter {
        private final Counter real;
        private int rivalTakes;
        int rivalTook;

        RivalCounter(int initial, int rivalTakes) {
            this.real = Counter.atomic(initial);
            this.rivalTakes = rivalTakes;
        }

        public int get() {
            int seen = real.get();
            if (rivalTakes > 0 && seen > 0) {
                rivalTakes--;
                rivalTook++;
                real.decrementAndGet();
            }
            return seen;
        }

        public void set(int value) { real.set(value); }
        public boolean compareAndSet(int expected, int next) { return real.compareAndSet(expected, next); }
        public int getAndDecrement() { return real.getAndDecrement(); }
        public int decrementAndGet() { return real.decrementAndGet(); }
        public int addAndGet(int delta) { return real.addAndGet(delta); }
    }

    static final class TicketPool {
        private final Counter left;

        TicketPool(int tickets) {
            if (tickets < 0) throw new IllegalArgumentException("negative tickets: " + tickets);
            left = Counter.atomic(tickets);
        }

        TicketPool(Counter left) {
            this.left = left;
        }

        boolean tryTake() {
            while (true) {
                int now = left.get();
                if (now == 0) return false;
                if (left.compareAndSet(now, now - 1)) return true;
            }
        }

        int takeUpTo(int n) {
            if (n < 0) throw new IllegalArgumentException("negative request: " + n);
            while (true) {
                int now = left.get();
                int take = Math.min(now, n);
                if (left.compareAndSet(now, now - take)) return take;
            }
        }

        int left() {
            return left.get();
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
        test("tryTake never oversells when a rival takes the last ticket after your read", () -> {
            var last = new RivalCounter(1, 1);
            boolean mine = new TicketPool(last).tryTake();
            eq((mine ? 1 : 0) + last.rivalTook, 1);
            eq(last.get(), 0);
            var three = new RivalCounter(3, 1);
            var pool = new TicketPool(three);
            ok(pool.tryTake());
            eq(three.rivalTook + 1 + pool.left(), 3);
        });
        test("takeUpTo re-reads after a rival moves first, and never goes below zero", () -> {
            var two = new RivalCounter(2, 1);
            int mine = new TicketPool(two).takeUpTo(3);
            eq(mine + two.rivalTook, 2);
            eq(two.get(), 0);
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
