// ─────────────────────────────────────────────────────────────────────────
//  07 · producer and consumer                               ★★☆ core
//  concepts: bounded buffers · backpressure · BlockingQueue · poison pill
//  run: java -cp ../../_lib/out 07-producer-consumer.java
// ─────────────────────────────────────────────────────────────────────────
//
//  First a model you can step by hand, then the real thing. Finish:
//    BoundedBuffer(cap)  cap < 1 → IllegalArgumentException; offer(x)
//                        adds at the back or returns false when full;
//                        poll() takes from the front, empty if none
//    simulate(cap, items, schedule)  a producer offers 1, 2, ... items
//                        through a BoundedBuffer. 'P': offer the next; if
//                        the buffer is full that is a producer wait (the
//                        item stays next); once every item is in, 'P' does
//                        nothing. 'C' is a consumer step: poll; empty is a
//                        consumer wait. Others → IllegalArgumentException
//    pipeline(input, cap, work)  a producer thread puts every input in an
//                        ArrayBlockingQueue(cap), then a poison pill; a
//                        consumer thread applies work until the pill; both
//                        joined; returns the outputs in input order
//
//      simulate(2, 3, "PPPCPCC") → SimResult([1, 2, 3], 1, 0)
//
//  hint: Optional.empty() makes a pill no real item can be confused with.

import static bootcamp.Check.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

class Exercise {

    static final class BoundedBuffer<T> {
        private final int capacity;
        private final Deque<T> items = new ArrayDeque<>();

        BoundedBuffer(int capacity) {
            throw new UnsupportedOperationException("TODO");
        }

        boolean offer(T item) {
            throw new UnsupportedOperationException("TODO");
        }

        Optional<T> poll() {
            throw new UnsupportedOperationException("TODO");
        }

        int size() {
            return items.size();
        }
    }

    record SimResult(List<Integer> consumed, int producerWaits, int consumerWaits) {}

    static SimResult simulate(int capacity, int items, String schedule) {
        throw new UnsupportedOperationException("TODO");
    }

    static List<Integer> pipeline(List<Integer> input, int capacity, IntUnaryOperator work) throws InterruptedException {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("the buffer holds up to its capacity and hands items out FIFO", () -> {
            var b = new BoundedBuffer<String>(2);
            ok(b.offer("a"));
            ok(b.offer("b"));
            ok(!b.offer("c"));
            eq(b.poll(), Optional.of("a"));
            ok(b.offer("c"));
            eq(b.poll(), Optional.of("b"));
            eq(b.poll(), Optional.of("c"));
            eq(b.poll(), Optional.empty());
        });
        test("a capacity below 1 is refused", () -> {
            raises(IllegalArgumentException.class, () -> new BoundedBuffer<String>(0));
            eq(new BoundedBuffer<String>(1).offer("x"), true);
        });
        test("a fast producer waits while the buffer is full, keeping its item", () ->
            eq(simulate(2, 3, "PPPCPCC"), new SimResult(List.of(1, 2, 3), 1, 0)));
        test("a fast consumer waits while the buffer is empty", () ->
            eq(simulate(1, 1, "CPCC"), new SimResult(List.of(1), 0, 2)));
        test("producer steps after the last item are not waits", () -> {
            eq(simulate(1, 1, "PPPC"), new SimResult(List.of(1), 0, 0));
            eq(simulate(3, 2, "PPPPCCC"), new SimResult(List.of(1, 2), 0, 1));
        });
        test("a step that is not P or C is rejected", () ->
            raises(IllegalArgumentException.class, () -> simulate(1, 1, "PXC")));
        test("pipeline runs a real producer and consumer and keeps the order", () -> {
            var input = new ArrayList<Integer>();
            var doubled = new ArrayList<Integer>();
            for (int i = 1; i <= 1_000; i++) {
                input.add(i);
                doubled.add(2 * i);
            }
            eq(pipeline(input, 1, x -> x * 2), doubled);
        });
        test("pipeline with no input returns an empty list", () ->
            eq(pipeline(List.of(), 3, x -> x), List.of()));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
