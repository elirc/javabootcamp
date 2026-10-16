// ─────────────────────────────────────────────────────────────────────────
//  02 · declined, with details — SOLUTION                   ★★☆ core
//  concepts: checked exceptions · throws clauses · custom exceptions · catch
//  run: java -cp ../../_lib/out 02-checked-exception.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Java splits exceptions in two. Checked exceptions (Exception but not
//  RuntimeException) must be either caught or declared with `throws` on
//  every method they pass through — the compiler enforces it. Unchecked
//  ones (RuntimeException, Error) need neither.
//  The rule of thumb: checked for conditions a correct program must still
//  expect and handle — a declined payment, a missing file — and unchecked
//  for programming errors — a negative amount, a null argument. So
//  withdraw declares `throws InsufficientFundsException` but merely throws
//  IllegalArgumentException for amount <= 0.
//  A custom exception earns its keep by carrying DATA: requested and
//  available as fields, so a caller can show "you are 3.00 short" without
//  parsing a message string.
//  tryWithdraw is the boundary where the checked exception is handled:
//  catch it, turn it into the user-facing text, and the method's own
//  signature stays clean. Catch the specific type — `catch (Exception e)`
//  would also swallow bugs like the IllegalArgumentException, which
//  should still crash loudly.

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
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive: " + amount);
        if (amount > balance) throw new InsufficientFundsException(amount, balance);
        return balance - amount;
    }

    static String tryWithdraw(long balance, long amount) {
        try {
            return "ok: " + withdraw(balance, amount);
        } catch (InsufficientFundsException e) {
            return "declined: " + e.getMessage();
        }
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
