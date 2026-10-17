// ─────────────────────────────────────────────────────────────────────────
//  06 · CompletableFuture, completed by hand                ★★☆ core
//  concepts: CompletableFuture · thenCombine · exceptionally · fan-in
//  run: java -cp ../../_lib/out 06-completable-futures.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A checkout asks three services at once. Tests complete every future BY
//  HAND, in chosen orders, so nothing depends on timing. Finish:
//    quote(price, tax, discount)  price * (1 + tax) - discount, scale 2
//                       HALF_UP, once all three are done; a failed discount
//                       counts as 0; a failed price or tax fails the quote
//                       with that same cause
//    allInOrder(fs)     all values in INPUT order once all succeed; fails
//                       with the cause as soon as ANY input fails, without
//                       waiting for the rest; an empty list → []
//    firstSuccess(fs)   the first value to arrive; failures are skipped
//                       until every input has failed, then it fails with
//                       IllegalStateException "all failed" (also for [])
//    withDeadline(work, deadline, fallback)  work's value, or fallback if
//                       the deadline future completes first
//
//      quote(19.99, 0.25, 1.00) → 23.99
//
//  hint: completeExceptionally on your own result future reports early.

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
        throw new UnsupportedOperationException("TODO");
    }

    static <T> CompletableFuture<List<T>> allInOrder(List<CompletableFuture<T>> futures) {
        throw new UnsupportedOperationException("TODO");
    }

    static <T> CompletableFuture<T> firstSuccess(List<CompletableFuture<T>> futures) {
        throw new UnsupportedOperationException("TODO");
    }

    static <T> CompletableFuture<T> withDeadline(CompletableFuture<T> work, CompletableFuture<Void> deadline, T fallback) {
        throw new UnsupportedOperationException("TODO");
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
