// ─────────────────────────────────────────────────────────────────────────
//  08 · exhaustive handling, checked                        ★★★ stretch
//  concepts: exhaustiveness · getPermittedSubclasses · isSealed · registries
//  run: java -cp ../../_lib/out 08-exhaustive-dispatch.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Java 17 cannot check that an instanceof chain covers every case. Build
//  a Dispatcher that checks it when it is built, from the permits lists.
//  Payments: Card, BankTransfer and Wallet, where Wallet is itself sealed
//  (ApplePay, PayPal). A permitted type that is not sealed is a LEAF.
//
//    leaves(root)       simple names of every leaf under root, sorted
//    forSealed(root)    a Builder; a root that is not sealed throws
//                       IllegalArgumentException
//    on(type, handler)  register; the same type twice → IllegalStateException
//    build()            each leaf is covered by its own handler or, if it
//                       has none, by the NEAREST registered ancestor
//                       (Wallet, or the root itself); uncovered leaves →
//                       IllegalStateException "unhandled: A, B" (sorted)
//    dispatch(value)    run the handler that covers value's class
//
//      on(Card) on(Wallet) on(BankTransfer) → build OK; PayPal → Wallet's
//
//  hint: Class has isSealed() and getPermittedSubclasses() since Java 17.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    sealed interface Payment permits Card, BankTransfer, Wallet {}

    record Card(String last4) implements Payment {}

    record BankTransfer(String iban) implements Payment {}

    sealed interface Wallet extends Payment permits ApplePay, PayPal {}

    record ApplePay(String device) implements Wallet {}

    record PayPal(String email) implements Wallet {}

    static List<String> leaves(Class<?> root) {
        throw new UnsupportedOperationException("TODO");
    }

    static final class Dispatcher<B, R> {
        private final Map<Class<?>, Function<Object, ? extends R>> byLeaf;

        private Dispatcher(Map<Class<?>, Function<Object, ? extends R>> byLeaf) {
            this.byLeaf = byLeaf;
        }

        static <B, R> Builder<B, R> forSealed(Class<B> root) {
            throw new UnsupportedOperationException("TODO");
        }

        R dispatch(B value) {
            throw new UnsupportedOperationException("TODO");
        }

        static final class Builder<B, R> {
            private final Class<B> root;
            private final Map<Class<?>, Function<Object, ? extends R>> handlers = new HashMap<>();

            private Builder(Class<B> root) {
                this.root = root;
            }

            <S extends B> Builder<B, R> on(Class<S> type, Function<? super S, ? extends R> handler) {
                throw new UnsupportedOperationException("TODO");
            }

            Dispatcher<B, R> build() {
                throw new UnsupportedOperationException("TODO");
            }

        }
    }

    static final List<Payment> SAMPLES = List.of(
        new Card("4242"), new BankTransfer("NO93 8601"), new ApplePay("watch"), new PayPal("ana@example.org"));

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("leaves lists every concrete payment type, through nested sealed types", () -> {
            eq(leaves(Payment.class), List.of("ApplePay", "BankTransfer", "Card", "PayPal"));
            eq(leaves(Wallet.class), List.of("ApplePay", "PayPal"));
        });
        test("a dispatcher with a handler per leaf routes each value", () -> {
            var fee = Dispatcher.<Payment, String>forSealed(Payment.class)
                .on(Card.class, c -> "card " + c.last4())
                .on(BankTransfer.class, b -> "bank")
                .on(ApplePay.class, a -> "apple " + a.device())
                .on(PayPal.class, p -> "paypal")
                .build();
            eq(SAMPLES.stream().map(fee::dispatch).toList(), List.of("card 4242", "bank", "apple watch", "paypal"));
        });
        test("a handler for a sealed sub-interface covers all its leaves", () -> {
            var kind = Dispatcher.<Payment, String>forSealed(Payment.class)
                .on(Card.class, c -> "card")
                .on(BankTransfer.class, b -> "bank")
                .on(Wallet.class, w -> "wallet")
                .build();
            eq(SAMPLES.stream().map(kind::dispatch).toList(), List.of("card", "bank", "wallet", "wallet"));
        });
        test("a leaf's own handler beats its sealed parent's", () -> {
            var kind = Dispatcher.<Payment, String>forSealed(Payment.class)
                .on(Card.class, c -> "card")
                .on(BankTransfer.class, b -> "bank")
                .on(Wallet.class, w -> "wallet")
                .on(PayPal.class, p -> "paypal")
                .build();
            eq(kind.dispatch(new PayPal("x")), "paypal");
            eq(kind.dispatch(new ApplePay("x")), "wallet");
        });
        test("a root handler is a fallback that specific handlers override", () -> {
            var risk = Dispatcher.<Payment, Integer>forSealed(Payment.class)
                .on(Payment.class, p -> 1)
                .on(Card.class, c -> 5)
                .build();
            eq(SAMPLES.stream().map(risk::dispatch).toList(), List.of(5, 1, 1, 1));
            var tiered = Dispatcher.<Payment, Integer>forSealed(Payment.class)
                .on(Payment.class, p -> 1)
                .on(Wallet.class, w -> 2)
                .on(Card.class, c -> 5)
                .build();
            eq(SAMPLES.stream().map(tiered::dispatch).toList(), List.of(5, 1, 2, 2));
        });
        test("build refuses missing cases and names them, sorted", () -> {
            var builder = Dispatcher.<Payment, String>forSealed(Payment.class).on(Card.class, c -> "card");
            var e = raises(IllegalStateException.class, builder::build);
            eq(e.getMessage(), "unhandled: ApplePay, BankTransfer, PayPal");
        });
        test("registering one type twice is an error", () -> {
            var builder = Dispatcher.<Payment, String>forSealed(Payment.class).on(Card.class, c -> "a");
            raises(IllegalStateException.class, () -> builder.on(Card.class, c -> "b"));
        });
        test("forSealed refuses a type that is not sealed", () -> {
            raises(IllegalArgumentException.class, () -> Dispatcher.<Card, String>forSealed(Card.class));
            raises(IllegalArgumentException.class, () -> Dispatcher.<String, String>forSealed(String.class));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
