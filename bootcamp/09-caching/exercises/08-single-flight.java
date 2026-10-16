// ─────────────────────────────────────────────────────────────────────────
//  08 · one load per stampede                               ★★★ stretch
//  concepts: cache stampede · single-flight · CompletableFuture · cleanup
//  run: java -cp ../../_lib/out 08-single-flight.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A hot key expires and hundreds of requests miss at once. Finish
//  SingleFlight so that while a load for a key is running ("in flight"),
//  further get(key) calls do NOT call the loader again but wait for the
//  same result. When the flight completes (value or exception) it is
//  forgotten: the next get starts a new load, and failures are shared by
//  all waiters but never remembered. This includes a loader whose future
//  is already complete when it returns. Each caller gets its OWN future:
//  cancelling or completing it must not affect the flight or the other
//  callers. inFlight() counts keys with a load running.
//
//      get(k), get(k)            → loader called once, inFlight() → 1
//      backend finishes k        → both futures → "row k", inFlight() → 0
//
//  The tests complete the backend's futures by hand: no threads, no sleep.
//  hint: CompletableFuture.copy() exists, and when whenComplete runs matters.

import static bootcamp.Check.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

class Exercise {

    static final class SingleFlight<K, V> {
        private final Function<K, CompletableFuture<V>> loader;
        private final Map<K, CompletableFuture<V>> flights = new HashMap<>();

        SingleFlight(Function<K, CompletableFuture<V>> loader) {
            this.loader = loader;
        }

        CompletableFuture<V> get(K key) {
            throw new UnsupportedOperationException("TODO");
        }

        int inFlight() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class Backend implements Function<String, CompletableFuture<String>> {
        final List<String> calls = new ArrayList<>();
        final Map<String, CompletableFuture<String>> pending = new HashMap<>();
        boolean instant;

        public CompletableFuture<String> apply(String key) {
            calls.add(key);
            if (instant) return CompletableFuture.completedFuture("row " + key);
            var f = new CompletableFuture<String>();
            pending.put(key, f);
            return f;
        }

        void finish(String key) {
            pending.remove(key).complete("row " + key);
        }

        void fail(String key) {
            pending.remove(key).completeExceptionally(new IllegalStateException("db timeout"));
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("gets during a flight call the loader once", () -> {
            var db = new Backend();
            var sf = new SingleFlight<String, String>(db);
            sf.get("k");
            sf.get("k");
            sf.get("k");
            eq(db.calls, List.of("k"));
            eq(sf.inFlight(), 1);
        });
        test("every waiter gets the result when the flight lands", () -> {
            var db = new Backend();
            var sf = new SingleFlight<String, String>(db);
            var a = sf.get("k");
            var b = sf.get("k");
            eq(a.isDone(), false);
            db.finish("k");
            eq(a.isDone() && b.isDone(), true);
            eq(a.join(), "row k");
            eq(b.join(), "row k");
        });
        test("different keys fly independently", () -> {
            var db = new Backend();
            var sf = new SingleFlight<String, String>(db);
            var a = sf.get("a");
            var b = sf.get("b");
            eq(db.calls, List.of("a", "b"));
            eq(sf.inFlight(), 2);
            db.finish("b");
            eq(b.join(), "row b");
            eq(a.isDone(), false);
            eq(sf.inFlight(), 1);
        });
        test("it is not a cache: after landing the next get loads again", () -> {
            var db = new Backend();
            var sf = new SingleFlight<String, String>(db);
            sf.get("k");
            db.finish("k");
            eq(sf.inFlight(), 0);
            sf.get("k");
            eq(db.calls, List.of("k", "k"));
        });
        test("a failure reaches every waiter and the next get retries", () -> {
            var db = new Backend();
            var sf = new SingleFlight<String, String>(db);
            var a = sf.get("k");
            var b = sf.get("k");
            db.fail("k");
            eq(a.isCompletedExceptionally() && b.isCompletedExceptionally(), true);
            var ex = raises(CompletionException.class, a::join);
            eq(ex.getCause().getMessage(), "db timeout");
            raises(CompletionException.class, b::join);
            eq(sf.inFlight(), 0);
            sf.get("k");
            eq(db.calls.size(), 2);
        });
        test("a loader that answers instantly leaves nothing in flight", () -> {
            var db = new Backend();
            db.instant = true;
            var sf = new SingleFlight<String, String>(db);
            eq(sf.get("k").join(), "row k");
            eq(sf.inFlight(), 0);
            eq(sf.get("k").join(), "row k");
            eq(db.calls, List.of("k", "k"));
        });
        test("one caller cancelling does not cancel the flight", () -> {
            var db = new Backend();
            var sf = new SingleFlight<String, String>(db);
            var patient = sf.get("k");
            var impatient = sf.get("k");
            impatient.cancel(true);
            eq(sf.inFlight(), 1);
            db.finish("k");
            eq(patient.isDone(), true);
            eq(patient.join(), "row k");
        });
        test("one caller completing its future does not decide for others", () -> {
            var db = new Backend();
            var sf = new SingleFlight<String, String>(db);
            var meddler = sf.get("k");
            var other = sf.get("k");
            meddler.complete("forged");
            eq(sf.inFlight(), 1);
            db.finish("k");
            eq(other.isDone(), true);
            eq(other.join(), "row k");

        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
