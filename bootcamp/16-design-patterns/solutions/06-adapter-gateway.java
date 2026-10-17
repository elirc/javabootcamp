// ─────────────────────────────────────────────────────────────────────────
//  06 · adapter: a legacy bank API — SOLUTION               ★★☆ core
//  concepts: adapter pattern · anti-corruption layer · money in cents
//  run: java -cp ../../_lib/out 06-adapter-gateway.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  An ADAPTER makes an existing class fit an interface it was not
//  designed for. The shop's code depends only on PaymentGateway, typed
//  and readable; LegacyBankApi speaks semicolon strings and numeric
//  codes. BankAdapter is the one class that knows both, so the bank's
//  quirks never leak into the domain. Domain-driven design calls a larger
//  version of this an anti-corruption layer.
//  Translation is the whole job, and it must be exact. Amounts become
//  whole cents with movePointRight(2).longValueExact(): no double
//  arithmetic, and a value like 1.005 is refused instead of silently
//  rounded, because the bank cannot charge half a cent.
//  stripTrailingZeros() first, so 12.500 still counts as 12.50.
//  Validate before calling the bank: a request you know is wrong must not
//  leave the building, and the test checks that no request was sent.
//  Map every reply explicitly. Known codes become readable reasons;
//  unknown codes stay declines but keep their number for support. A reply
//  that matches no known shape is NOT a decline: treating garbage as "no"
//  hides an outage behind a pile of refused payments. Fail loudly with
//  the raw text instead.

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
            var account = accounts.get(customerId);
            if (account == null) throw new IllegalArgumentException("unknown customer: " + customerId);
            if (amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 2) {
                throw new IllegalArgumentException("bad amount: " + amount);
            }
            var cur = currency.toUpperCase(Locale.ROOT);
            if (!CURRENCIES.contains(cur)) throw new IllegalArgumentException("unsupported currency: " + currency);
            long cents = amount.movePointRight(2).longValueExact();
            var reply = bank.submit("ACCT=" + account + ";CENTS=" + cents + ";CUR=" + cur);
            return translate(reply);
        }

        private static PaymentResult translate(String reply) {
            if (reply.startsWith("OK|") && reply.length() > 3) return new PaymentResult(true, reply.substring(3), null);
            if (reply.matches("ERR\\|\\d+")) {
                var code = reply.substring(4);
                var reason = switch (code) {
                    case "51" -> "insufficient funds";
                    case "54" -> "expired card";
                    default -> "declined (code " + code + ")";
                };
                return new PaymentResult(false, null, reason);
            }
            throw new IllegalStateException("unexpected bank response: " + reply);
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
            var bank = new LegacyBankApi("OK|R1", "OK|R2", "OK|R3", "OK|R4");
            var gateway = new BankAdapter(bank, ACCOUNTS);
            gateway.charge("c1", d("12.5"), "eur");
            gateway.charge("c1", d("12.500"), "EUR");
            gateway.charge("c2", d("1000"), "usd");
            gateway.charge("c2", d("4.35"), "Gbp");
            eq(bank.requests, List.of("ACCT=NO-1;CENTS=1250;CUR=EUR", "ACCT=NO-1;CENTS=1250;CUR=EUR",
                "ACCT=GB-22;CENTS=100000;CUR=USD", "ACCT=GB-22;CENTS=435;CUR=GBP"));
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
