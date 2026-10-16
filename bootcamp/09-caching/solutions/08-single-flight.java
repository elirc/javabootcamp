// ─────────────────────────────────────────────────────────────────────────
//  08 · one load per stampede — SOLUTION                    ★★★ stretch
//  concepts: cache stampede · single-flight · CompletableFuture · cleanup
//  run: java -cp ../../_lib/out 08-single-flight.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A hot key expires and 500 requests miss at the same instant; without
//  coordination all 500 hit the database for the same row (a "cache
//  stampede" or "dog-pile"). Single-flight, named after Go's
//  golang.org/x/sync/singleflight, lets the first caller start the load
//  and makes everyone who arrives while it is running wait on the same
//  result. It is not a cache: once the flight lands the entry is gone,
//  and the cache in front of it stores the value.
//  The map holds the one shared future per key. Two details make it
//  correct:
//  1. Cleanup is registered AFTER the put. whenComplete on an already
//     completed future runs immediately, so registering it first would
//     remove nothing and then the put would leave a finished flight in
//     the map forever: every later get would return the stale result.
//     remove(key, fresh) only removes that exact flight.
//  2. Every caller gets fresh.copy(), not fresh. A CompletableFuture is
//     writable by whoever holds it; hand out the shared one and a single
//     caller's cancel() or complete(...) decides the answer for all.
//  Failures are shared too (each waiter sees the same exception) and are
//  never remembered, so the next get starts a new flight. Java trap:
//  doing this inside HashMap.computeIfAbsent breaks when the cleanup runs
//  synchronously, because the map is modified during its own compute.

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
            var shared = flights.get(key);
            if (shared == null) {
                var fresh = loader.apply(key);
                flights.put(key, fresh);
                fresh.whenComplete((value, error) -> flights.remove(key, fresh));
                shared = fresh;
            }
            return shared.copy();
        }

        int inFlight() {
            return flights.size();
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
