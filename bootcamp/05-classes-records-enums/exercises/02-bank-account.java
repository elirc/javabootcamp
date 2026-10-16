// ─────────────────────────────────────────────────────────────────────────
//  02 · a balance nobody can poke                           ★☆☆ warm-up
//  concepts: encapsulation · private fields · guard clauses · exceptions
//  run: java -cp ../../_lib/out 02-bank-account.java
// ─────────────────────────────────────────────────────────────────────────
//
//  The bank simulator's first class. Finish the nested class BankAccount:
//  the owner name is required (IllegalArgumentException when blank); a new
//  account starts at 0; deposit and withdraw take positive cent amounts
//  (anything else → IllegalArgumentException); withdrawing more than the
//  balance → IllegalStateException and the balance is unchanged.
//
//      var acct = new BankAccount("Ada");
//      acct.deposit(5_000);  acct.withdraw(1_250);   acct.balance() → 3750
//      acct.withdraw(10_000)                          → IllegalStateException
//
//  hint: check everything first, change state last.

import static bootcamp.Check.*;

class Exercise {

    static final class BankAccount {
        private final String owner;
        private long balanceCents;

        BankAccount(String owner) {
            throw new UnsupportedOperationException("TODO");
        }

        String owner() {
            return owner;
        }

        long balance() {
            return balanceCents;
        }

        void deposit(long cents) {
            throw new UnsupportedOperationException("TODO");
        }

        void withdraw(long cents) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("a new account is empty", () -> eq(new BankAccount("Ada").balance(), 0));
        test("requires an owner", () -> {
            raises(IllegalArgumentException.class, () -> new BankAccount(" "));
            raises(IllegalArgumentException.class, () -> new BankAccount(null));
        });
        test("deposits and withdrawals move the balance", () -> {
            var acct = new BankAccount("Ada");
            acct.deposit(5_000);
            acct.withdraw(1_250);
            eq(acct.balance(), 3_750);
        });
        test("refuses non-positive amounts", () -> {
            var acct = new BankAccount("Ada");
            raises(IllegalArgumentException.class, () -> acct.deposit(0));
            raises(IllegalArgumentException.class, () -> acct.deposit(-5));
            raises(IllegalArgumentException.class, () -> acct.withdraw(-5));
        });
        test("refuses to overdraw", () -> {
            var acct = new BankAccount("Ada");
            acct.deposit(100);
            raises(IllegalStateException.class, () -> acct.withdraw(101));
        });
        test("a refused withdrawal changes nothing", () -> {
            var acct = new BankAccount("Ada");
            acct.deposit(100);
            raises(IllegalStateException.class, () -> acct.withdraw(500));
            eq(acct.balance(), 100);
        });
        test("can withdraw everything", () -> {
            var acct = new BankAccount("Ada");
            acct.deposit(100);
            acct.withdraw(100);
            eq(acct.balance(), 0);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
