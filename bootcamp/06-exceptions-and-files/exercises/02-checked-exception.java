// ─────────────────────────────────────────────────────────────────────────
//  02 · declined, with details                              ★★☆ core
//  concepts: checked exceptions · throws clauses · custom exceptions · catch
//  run: java -cp ../../_lib/out 02-checked-exception.java
// ─────────────────────────────────────────────────────────────────────────
//
//  The bank simulator declines withdrawals with a CHECKED exception, so
//  no caller can forget the case. InsufficientFundsException (above the
//  tests) is done. Write withdraw: return the new balance, throw
//  InsufficientFundsException when the amount exceeds the balance, and
//  IllegalArgumentException for a non-positive amount. Then write
//  tryWithdraw, which never throws for a decline and reports in text.
//
//      withdraw(1000, 300)       → 700
//      withdraw(200, 500)        → InsufficientFundsException(500, 200)
//      tryWithdraw(1000, 300)    → "ok: 700"
//      tryWithdraw(200, 500)     → "declined: need 500, have 200"
//
//  hint: a method that can throw a checked exception must say so.

import static bootcamp.Check.*;

class Exercise {

    static final class InsufficientFundsException extends Exception {
        private static final long serialVersionUID = 1L;
        final long requested;
        final long available;

        InsufficientFundsException(long requested, long available) {
            super("need " + requested + ", have " + available);
            this.requested = requested;
            this.available = available;
        }
    }

    static long withdraw(long balance, long amount) throws InsufficientFundsException {
        throw new UnsupportedOperationException("TODO");
    }

    static String tryWithdraw(long balance, long amount) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("returns the new balance", () -> eq(withdraw(1000, 300), 700));
        test("can empty the account", () -> eq(withdraw(1000, 1000), 0));
        test("declines with a checked exception", () ->
            raises(InsufficientFundsException.class, () -> withdraw(200, 500)));
        test("the exception carries the amounts", () -> {
            var e = raises(InsufficientFundsException.class, () -> withdraw(200, 500));
            eq(e.requested, 500);
            eq(e.available, 200);
        });
        test("a non-positive amount is a programming error", () -> {
            raises(IllegalArgumentException.class, () -> withdraw(1000, 0));
            raises(IllegalArgumentException.class, () -> withdraw(1000, -5));
        });
        test("tryWithdraw reports success", () -> eq(tryWithdraw(1000, 300), "ok: 700"));
        test("tryWithdraw reports a decline", () -> eq(tryWithdraw(200, 500), "declined: need 500, have 200"));
        test("tryWithdraw does not hide programming errors", () ->
            raises(IllegalArgumentException.class, () -> tryWithdraw(1000, -1)));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
