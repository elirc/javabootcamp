// ─────────────────────────────────────────────────────────────────────────
//  06 · CompletableFuture, completed by hand — SOLUTION     ★★☆ core
//  concepts: CompletableFuture · thenCombine · exceptionally · fan-in
//  run: java -cp ../../_lib/out 06-completable-futures.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  CompletableFuture is a value that will exist later, plus callbacks to
//  run when it does. When the test thread calls complete(...), every
//  dependent stage runs right there, on the test thread, in a fixed
//  order. That is the trick of this exercise: no executor, no sleep, no
//  timeout, and the same result every run. Production code passes in
//  real futures from HTTP clients; the logic is the same.
//  thenCombine joins two futures; exceptionally turns a failure into a
//  value, which is how "no discount service, no discount" stays a
//  business rule instead of an outage. A dependent stage that fails
//  because its input failed holds a CompletionException wrapping the
//  original cause; join() throws it, and getCause() is what you report.
//  allOf is NOT "fail fast": it completes only when every input has
//  completed, even if the first one failed a second ago. allInOrder
//  builds its own result future and completes it exceptionally on the
//  first failure. Collect values by INDEX, not in arrival order.
//  anyOf takes the first to complete, success or failure, so it cannot
//  express "first success". Counting failures with an AtomicInteger and
//  failing only on the last is the honest version. withDeadline models a
//  timeout without a clock: in production the deadline future comes from
//  orTimeout or a scheduler; here the test fires it.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

class Exercise {

    static CompletableFuture<BigDecimal> quote(CompletableFuture<BigDecimal> price,
                                               CompletableFuture<BigDecimal> taxRate,
                                               CompletableFuture<BigDecimal> discount) {
        return price.thenCombine(taxRate, (p, t) -> p.multiply(BigDecimal.ONE.add(t)))
            .thenCombine(discount.exceptionally(e -> BigDecimal.ZERO),
                (gross, d) -> gross.subtract(d).setScale(2, RoundingMode.HALF_UP));
    }

    static <T> CompletableFuture<List<T>> allInOrder(List<CompletableFuture<T>> futures) {
        var result = new CompletableFuture<List<T>>();
        if (futures.isEmpty()) {
            result.complete(List.of());
            return result;
        }
        var values = new ArrayList<T>(Collections.nCopies(futures.size(), null));
        var remaining = new AtomicInteger(futures.size());
        for (int i = 0; i < futures.size(); i++) {
            int index = i;
            futures.get(i).whenComplete((value, error) -> {
                if (error != null) {
                    result.completeExceptionally(unwrap(error));
                } else {
                    synchronized (values) {
                        values.set(index, value);
                    }
                    if (remaining.decrementAndGet() == 0) result.complete(values);
                }
            });
        }
        return result;
    }

    static <T> CompletableFuture<T> firstSuccess(List<CompletableFuture<T>> futures) {
        var result = new CompletableFuture<T>();
        if (futures.isEmpty()) {
            result.completeExceptionally(new IllegalStateException("all failed"));
            return result;
        }
        var failures = new AtomicInteger();
        for (var f : futures) {
            f.whenComplete((value, error) -> {
                if (error == null) result.complete(value);
                else if (failures.incrementAndGet() == futures.size()) result.completeExceptionally(new IllegalStateException("all failed"));
            });
        }
        return result;
    }

    static <T> CompletableFuture<T> withDeadline(CompletableFuture<T> work, CompletableFuture<Void> deadline, T fallback) {
        var result = new CompletableFuture<T>();
        work.whenComplete((value, error) -> {
            if (error != null) result.completeExceptionally(unwrap(error));
            else result.complete(value);
        });
        deadline.whenComplete((ignored, error) -> result.complete(fallback));
        return result;
    }

    private static Throwable unwrap(Throwable t) {
        return t instanceof CompletionException && t.getCause() != null ? t.getCause() : t;
    }

    static Throwable failure(CompletableFuture<?> f) {
        try {
            f.join();
            return null;
        } catch (CompletionException e) {
            return e.getCause();
        }
    }

    static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("quote waits for all three inputs, in any completion order", () -> {
            var price = new CompletableFuture<BigDecimal>();
            var tax = new CompletableFuture<BigDecimal>();
            var discount = new CompletableFuture<BigDecimal>();
            var q = quote(price, tax, discount);
            tax.complete(d("0.25"));
            discount.complete(d("1.00"));
            ok(!q.isDone());
            price.complete(d("19.99"));
            eq(q.join().toPlainString(), "23.99");
        });
        test("a failed discount counts as zero", () -> {
            var discount = new CompletableFuture<BigDecimal>();
            var q = quote(CompletableFuture.completedFuture(d("10")), CompletableFuture.completedFuture(d("0.1")), discount);
            discount.completeExceptionally(new IllegalStateException("promo service down"));
            eq(q.join().toPlainString(), "11.00");
        });
        test("a failed price fails the quote with the same cause", () -> {
            var price = new CompletableFuture<BigDecimal>();
            var q = quote(price, CompletableFuture.completedFuture(d("0.1")), CompletableFuture.completedFuture(d("0")));
            var cause = new IllegalStateException("pricing down");
            price.completeExceptionally(cause);
            ok(q.isCompletedExceptionally());
            ok(failure(q) == cause);
        });
        test("allInOrder keeps input order whatever the completion order", () -> {
            var a = new CompletableFuture<String>();
            var b = new CompletableFuture<String>();
            var c = new CompletableFuture<String>();
            var all = allInOrder(List.of(a, b, c));
            c.complete("C");
            a.complete("A");
            ok(!all.isDone());
            b.complete("B");
            eq(all.join(), List.of("A", "B", "C"));
            eq(allInOrder(List.<CompletableFuture<String>>of()).join(), List.of());
        });
        test("allInOrder fails as soon as one input fails", () -> {
            var a = new CompletableFuture<String>();
            var b = new CompletableFuture<String>();
            var all = allInOrder(List.of(a, b));
            var cause = new IllegalArgumentException("bad sku");
            b.completeExceptionally(cause);
            ok(all.isCompletedExceptionally());
            ok(failure(all) == cause);
        });
        test("firstSuccess takes the first value and skips earlier failures", () -> {
            var a = new CompletableFuture<String>();
            var b = new CompletableFuture<String>();
            var c = new CompletableFuture<String>();
            var first = firstSuccess(List.of(a, b, c));
            a.completeExceptionally(new IllegalStateException("eu down"));
            ok(!first.isDone());
            c.complete("from us");
            b.complete("from asia");
            eq(first.join(), "from us");
        });
        test("firstSuccess fails only when every input has failed", () -> {
            var a = new CompletableFuture<String>();
            var b = new CompletableFuture<String>();
            var first = firstSuccess(List.of(a, b));
            a.completeExceptionally(new IllegalStateException("one"));
            ok(!first.isDone());
            b.completeExceptionally(new IllegalStateException("two"));
            eq(failure(first).getClass(), IllegalStateException.class);
            eq(failure(first).getMessage(), "all failed");
            eq(failure(firstSuccess(List.<CompletableFuture<String>>of())).getMessage(), "all failed");
        });
        test("withDeadline gives the fallback if the deadline fires first, else the value", () -> {
            var slow = new CompletableFuture<String>();
            var deadline = new CompletableFuture<Void>();
            var r = withDeadline(slow, deadline, "cached");
            deadline.complete(null);
            slow.complete("fresh");
            eq(r.join(), "cached");
            var fast = new CompletableFuture<String>();
            var later = new CompletableFuture<Void>();
            var r2 = withDeadline(fast, later, "cached");
            fast.complete("fresh");
            later.complete(null);
            eq(r2.join(), "fresh");
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
