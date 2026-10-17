// ─────────────────────────────────────────────────────────────────────────
//  07 · producer and consumer — SOLUTION                    ★★☆ core
//  concepts: bounded buffers · backpressure · BlockingQueue · poison pill
//  run: java -cp ../../_lib/out 07-producer-consumer.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Producer-consumer is the shape of every queue in a system: request
//  handlers feeding a worker pool, a log shipper, the message brokers of
//  module 12. The BOUNDED buffer is the important word. When consumers
//  fall behind, a full buffer makes producers wait (backpressure) instead
//  of eating memory until the process dies.
//  The model makes backpressure visible and repeatable. A schedule of P
//  and C steps is one possible interleaving of two threads, and counting
//  waits tells you who is the bottleneck: producer waits mean the
//  consumer is too slow, consumer waits mean it is idle. Note that a
//  producer whose offer failed keeps the item; dropping it would be a
//  lost message. A producer with nothing left does no work, so its steps
//  are not waits.
//  pipeline is the same idea with real threads. ArrayBlockingQueue.put
//  blocks while full and take blocks while empty, so no polling and no
//  sleep. Shutdown is the classic problem: the consumer cannot tell
//  "empty for now" from "finished". A POISON PILL, a special item put
//  last, says "no more". Optional.empty() as the pill cannot collide with
//  a real value, unlike a magic -1. With one consumer FIFO order is kept,
//  so the result is deterministic even with real threads; several
//  consumers need one pill each, and their output order is not fixed.

import static bootcamp.Check.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

class Exercise {

    static final class BoundedBuffer<T> {
        private final int capacity;
        private final Deque<T> items = new ArrayDeque<>();

        BoundedBuffer(int capacity) {
            if (capacity < 1) throw new IllegalArgumentException("capacity must be >= 1, was " + capacity);
            this.capacity = capacity;
        }

        boolean offer(T item) {
            if (items.size() >= capacity) return false;
            items.addLast(item);
            return true;
        }

        Optional<T> poll() {
            return Optional.ofNullable(items.pollFirst());
        }

        int size() {
            return items.size();
        }
    }

    record SimResult(List<Integer> consumed, int producerWaits, int consumerWaits) {}

    static SimResult simulate(int capacity, int items, String schedule) {
        var buffer = new BoundedBuffer<Integer>(capacity);
        var consumed = new ArrayList<Integer>();
        int next = 1, producerWaits = 0, consumerWaits = 0;
        for (char step : schedule.toCharArray()) {
            if (step == 'P') {
                if (next > items) continue;
                if (buffer.offer(next)) next++;
                else producerWaits++;
            } else if (step == 'C') {
                var item = buffer.poll();
                if (item.isPresent()) consumed.add(item.get());
                else consumerWaits++;
            } else {
                throw new IllegalArgumentException("unknown step '" + step + "'");
            }
        }
        return new SimResult(consumed, producerWaits, consumerWaits);
    }

    static List<Integer> pipeline(List<Integer> input, int capacity, IntUnaryOperator work) throws InterruptedException {
        var queue = new ArrayBlockingQueue<Optional<Integer>>(capacity);
        var out = Collections.synchronizedList(new ArrayList<Integer>());
        var producer = new Thread(() -> {
            try {
                for (var x : input) queue.put(Optional.of(x));
                queue.put(Optional.empty());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        var consumer = new Thread(() -> {
            try {
                while (true) {
                    var next = queue.take();
                    if (next.isEmpty()) return;
                    out.add(work.applyAsInt(next.get()));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        return new ArrayList<>(out);
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
