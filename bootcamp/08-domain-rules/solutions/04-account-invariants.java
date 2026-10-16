// ─────────────────────────────────────────────────────────────────────────
//  04 · rules an account never breaks — SOLUTION            ★★☆ core
//  concepts: invariants · guard methods · lifecycle flags
//  run: java -cp ../../_lib/out 04-account-invariants.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  An invariant is a statement about the object that is true between any
//  two method calls: "balance >= 0", "closed implies balance == 0". The
//  class's whole job is to keep its invariants; every public method
//  either preserves them or refuses.
//  Guard first, mutate last, in every method. A small private helper —
//  requireOpen() — states the shared precondition once, and each method
//  reads as a list of its rules: requireOpen(); requirePositive(cents);
//  if (frozen) throw...; then the change. Ordering the guards so that the
//  most fundamental one (closed) comes first gives the most useful error.
//  The two exception types carry meaning: IllegalArgumentException for
//  "you passed a bad amount" (caller bug), IllegalStateException for "this
//  account, right now, can't" (a business state). The tests check which
//  one, not just that something was thrown.
//  Deposits into a frozen account are allowed on purpose: freezing stops
//  money LEAVING (fraud holds work this way). Encoding that as a separate
//  rule in withdraw, not a blanket "frozen = read-only", is the kind of
//  detail a state flag gets wrong if you don't read the spec.
//  Booleans for frozen/closed are fine at this size; with more states,
//  an enum lifecycle (module 08/03) scales better.

import static bootcamp.Check.*;

class Exercise {

    static final class Account {
        private long balance;
        private boolean frozen;
        private boolean closed;

        long balance() {
            return balance;
        }

        boolean isClosed() {
            return closed;
        }

        void deposit(long cents) {
            requireOpen();
            requirePositive(cents);
            balance += cents;
        }

        void withdraw(long cents) {
            requireOpen();
            requirePositive(cents);
            if (frozen) throw new IllegalStateException("account is frozen");
            if (cents > balance) throw new IllegalStateException("insufficient funds");
            balance -= cents;
        }

        void freeze() {
            requireOpen();
            frozen = true;
        }

        void unfreeze() {
            requireOpen();
            frozen = false;
        }

        void close() {
            requireOpen();
            if (balance != 0) throw new IllegalStateException("balance must be zero to close");
            closed = true;
        }

        private void requireOpen() {
            if (closed) throw new IllegalStateException("account is closed");
        }

        private static void requirePositive(long cents) {
            if (cents <= 0) throw new IllegalArgumentException("amount must be positive: " + cents);
        }
    }

    static Account funded(long cents) {
        var acct = new Account();
        acct.deposit(cents);
        return acct;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("deposits and withdraws", () -> {
            var acct = funded(100);
            acct.withdraw(30);
            eq(acct.balance(), 70);
        });
        test("never goes negative", () -> {
            var acct = funded(100);
            raises(IllegalStateException.class, () -> acct.withdraw(101));
            eq(acct.balance(), 100);
        });
        test("a frozen account refuses withdrawals but takes deposits", () -> {
            var acct = funded(100);
            acct.freeze();
            raises(IllegalStateException.class, () -> acct.withdraw(10));
            acct.deposit(5);
            eq(acct.balance(), 105);
            acct.unfreeze();
            acct.withdraw(5);
            eq(acct.balance(), 100);
        });
        test("only an empty account can close", () -> {
            var acct = funded(100);
            raises(IllegalStateException.class, acct::close);
            eq(acct.isClosed(), false);
            acct.withdraw(100);
            acct.close();
            eq(acct.isClosed(), true);
        });
        test("a closed account refuses everything", () -> {
            var acct = new Account();
            acct.close();
            raises(IllegalStateException.class, () -> acct.deposit(1));
            raises(IllegalStateException.class, () -> acct.withdraw(1));
            raises(IllegalStateException.class, acct::freeze);
            raises(IllegalStateException.class, acct::close);
        });
        test("bad amounts are argument errors", () -> {
            var acct = funded(100);
            raises(IllegalArgumentException.class, () -> acct.deposit(0));
            raises(IllegalArgumentException.class, () -> acct.withdraw(-1));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
