// ─────────────────────────────────────────────────────────────────────────
//  04 · rules an account never breaks                       ★★☆ core
//  concepts: invariants · guard methods · lifecycle flags
//  run: java -cp ../../_lib/out 04-account-invariants.java
// ─────────────────────────────────────────────────────────────────────────
//
//  The bank simulator's Account has rules that must hold after EVERY call:
//  the balance is never negative; a frozen account accepts deposits but
//  refuses withdrawals until unfreeze(); a closed account refuses
//  everything (deposit, withdraw, freeze, close again); only an account
//  with a zero balance can be closed. Every refusal is an
//  IllegalStateException and changes nothing. Non-positive amounts →
//  IllegalArgumentException. Finish the nested class Account.
//
//      deposit(100); freeze(); withdraw(10)    → IllegalStateException
//      close() with a balance of 100           → IllegalStateException
//      withdraw(100); close(); deposit(1)      → IllegalStateException
//
//  hint: one private requireOpen() saves you five copies of the same if.

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
            throw new UnsupportedOperationException("TODO");
        }

        void withdraw(long cents) {
            throw new UnsupportedOperationException("TODO");
        }

        void freeze() {
            throw new UnsupportedOperationException("TODO");
        }

        void unfreeze() {
            throw new UnsupportedOperationException("TODO");
        }

        void close() {
            throw new UnsupportedOperationException("TODO");
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
