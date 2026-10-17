// ─────────────────────────────────────────────────────────────────────────
//  06 · adapter: a legacy bank API                          ★★☆ core
//  concepts: adapter pattern · anti-corruption layer · money in cents
//  run: java -cp ../../_lib/out 06-adapter-gateway.java
// ─────────────────────────────────────────────────────────────────────────
//
//  The shop speaks PaymentGateway; the bank's old SDK, LegacyBankApi
//  (given, do not change it), speaks strings. Finish BankAdapter, which
//  implements PaymentGateway on top of it. charge(customer, amount, cur):
//    1. checks, before any bank call: an unknown customer →
//       IllegalArgumentException "unknown customer: <id>"; an amount that
//       is not positive, or has more than 2 decimals once trailing zeros
//       are dropped, → IllegalArgumentException; a currency (any case)
//       other than USD, EUR or GBP → IllegalArgumentException
//    2. sends "ACCT=<account>;CENTS=<whole cents>;CUR=<upper case>"
//    3. maps the reply: "OK|<ref>" (ref not empty) → approved with ref;
//       "ERR|51" → declined "insufficient funds"; "ERR|54" → declined
//       "expired card"; "ERR|<other code>" → "declined (code <code>)";
//       anything else → IllegalStateException "unexpected bank response:
//       <reply>". A declined result has a null reference.
//
//      charge("c1", 12.5, "eur") sends "ACCT=NO-1;CENTS=1250;CUR=EUR"
//
//  hint: movePointRight(2) turns 12.5 into 1250 without a double.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.util.*;

class Exercise {

    // The bank's old SDK (given; do not change it). It replies with the
    // scripted responses in order and records every request it receives.
    static final class LegacyBankApi {
        final List<String> requests = new ArrayList<>();
        private final Deque<String> replies;

        LegacyBankApi(String... replies) {
            this.replies = new ArrayDeque<>(List.of(replies));
        }

        String submit(String request) {
            requests.add(request);
            return replies.isEmpty() ? "ERR|96" : replies.poll();
        }
    }

    record PaymentResult(boolean approved, String reference, String declineReason) {}

    interface PaymentGateway {
        PaymentResult charge(String customerId, BigDecimal amount, String currency);
    }

    static final Set<String> CURRENCIES = Set.of("USD", "EUR", "GBP");

    static final class BankAdapter implements PaymentGateway {
        private final LegacyBankApi bank;
        private final Map<String, String> accounts;

        BankAdapter(LegacyBankApi bank, Map<String, String> accounts) {
            this.bank = bank;
            this.accounts = Map.copyOf(accounts);
        }

        @Override
        public PaymentResult charge(String customerId, BigDecimal amount, String currency) {
            throw new UnsupportedOperationException("TODO");
        }

    }

    static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    static final Map<String, String> ACCOUNTS = Map.of("c1", "NO-1", "c2", "GB-22");

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("an approved charge returns the bank's reference", () -> {
            var bank = new LegacyBankApi("OK|REF-778");
            eq(new BankAdapter(bank, ACCOUNTS).charge("c2", d("9.99"), "GBP"), new PaymentResult(true, "REF-778", null));
        });
        test("the request carries the account, whole cents and an upper-case currency", () -> {
            var bank = new LegacyBankApi("OK|R1", "OK|R2", "OK|R3");
            var gateway = new BankAdapter(bank, ACCOUNTS);
            gateway.charge("c1", d("12.5"), "eur");
            gateway.charge("c1", d("12.500"), "EUR");
            gateway.charge("c2", d("1000"), "usd");
            eq(bank.requests, List.of("ACCT=NO-1;CENTS=1250;CUR=EUR", "ACCT=NO-1;CENTS=1250;CUR=EUR",
                "ACCT=GB-22;CENTS=100000;CUR=USD"));
        });
        test("decline codes become readable reasons", () -> {
            var gateway = new BankAdapter(new LegacyBankApi("ERR|51", "ERR|54", "ERR|05"), ACCOUNTS);
            eq(gateway.charge("c1", d("5"), "EUR"), new PaymentResult(false, null, "insufficient funds"));
            eq(gateway.charge("c1", d("5"), "EUR"), new PaymentResult(false, null, "expired card"));
            eq(gateway.charge("c1", d("5"), "EUR"), new PaymentResult(false, null, "declined (code 05)"));
        });
        test("a reply of unknown shape is an IllegalStateException, not a decline", () -> {
            var gateway = new BankAdapter(new LegacyBankApi("HELLO", "OK|", "ERR|"), ACCOUNTS);
            var e = raises(IllegalStateException.class, () -> gateway.charge("c1", d("5"), "EUR"));
            eq(e.getMessage(), "unexpected bank response: HELLO");
            raises(IllegalStateException.class, () -> gateway.charge("c1", d("5"), "EUR"));
            raises(IllegalStateException.class, () -> gateway.charge("c1", d("5"), "EUR"));
        });
        test("an unknown customer is rejected before calling the bank", () -> {
            var bank = new LegacyBankApi("OK|R1");
            var e = raises(IllegalArgumentException.class, () -> new BankAdapter(bank, ACCOUNTS).charge("c9", d("5"), "EUR"));
            eq(e.getMessage(), "unknown customer: c9");
            eq(bank.requests, List.of());
        });
        test("amounts must be positive with at most two decimals", () -> {
            var bank = new LegacyBankApi("OK|R1");
            var gateway = new BankAdapter(bank, ACCOUNTS);
            raises(IllegalArgumentException.class, () -> gateway.charge("c1", d("0"), "EUR"));
            raises(IllegalArgumentException.class, () -> gateway.charge("c1", d("-3"), "EUR"));
            raises(IllegalArgumentException.class, () -> gateway.charge("c1", d("1.005"), "EUR"));
            eq(bank.requests, List.of());
        });
        test("only USD, EUR and GBP are accepted", () -> {
            var bank = new LegacyBankApi("OK|R1");
            var gateway = new BankAdapter(bank, ACCOUNTS);
            raises(IllegalArgumentException.class, () -> gateway.charge("c1", d("5"), "JPY"));
            eq(bank.requests, List.of());
            eq(gateway.charge("c1", d("5"), "gbp").approved(), true);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
