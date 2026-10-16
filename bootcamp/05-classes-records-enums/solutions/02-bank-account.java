// ─────────────────────────────────────────────────────────────────────────
//  02 · a balance nobody can poke — SOLUTION                ★☆☆ warm-up
//  concepts: encapsulation · private fields · guard clauses · exceptions
//  run: java -cp ../../_lib/out 02-bank-account.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Encapsulation is not "add getters". It is: the object owns its state,
//  and the ONLY way to change it is through methods that keep it valid.
//  A public balance field (or a setBalance) lets any caller write -1; a
//  private field with deposit/withdraw makes negative balances impossible
//  by construction.
//  Two kinds of "no": a non-positive amount is the CALLER's bug — a bad
//  argument — so IllegalArgumentException. Withdrawing more than you have
//  is a perfectly well-formed request that the current STATE can't honour
//  — IllegalStateException. Picking the right one tells the caller whether
//  to fix their code or to handle a business outcome.
//  Order inside each method: validate everything, THEN mutate. If you
//  subtract first and check after, a failed withdraw has already damaged
//  the balance by the time it throws. "Check, then act" keeps a failed
//  call side-effect free, which the last test checks.
//  Cents as long: integer arithmetic, no rounding, and long gives room
//  for big balances. Module 07 moves to BigDecimal for rates and interest.

import static bootcamp.Check.*;

class Exercise {

    static final class BankAccount {
        private final String owner;
        private long balanceCents;

        BankAccount(String owner) {
            if (owner == null || owner.isBlank()) throw new IllegalArgumentException("owner is required");
            this.owner = owner.strip();
        }

        String owner() {
            return owner;
        }

        long balance() {
            return balanceCents;
        }

        void deposit(long cents) {
            if (cents <= 0) throw new IllegalArgumentException("deposit must be positive: " + cents);
            balanceCents += cents;
        }

        void withdraw(long cents) {
            if (cents <= 0) throw new IllegalArgumentException("withdrawal must be positive: " + cents);
            if (cents > balanceCents) throw new IllegalStateException("insufficient funds");
            balanceCents -= cents;
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
