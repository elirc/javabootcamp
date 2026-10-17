// ─────────────────────────────────────────────────────────────────────────
//  08 · exhaustive handling, checked — SOLUTION             ★★★ stretch
//  concepts: exhaustiveness · getPermittedSubclasses · isSealed · registries
//  run: java -cp ../../_lib/out 08-exhaustive-dispatch.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  With an enum, a switch expression is checked: leave out a constant and
//  javac refuses. With sealed types in Java 17 there is no such check
//  yet: an instanceof chain that forgets a case compiles, and the bug
//  shows up as the AssertionError at its end. Java 21's pattern switch
//  closes the gap at compile time. This exercise closes it at startup:
//  the permits lists are readable by reflection, so build() can walk the
//  hierarchy and refuse to create a dispatcher with a hole in it. Failing
//  when the application starts, with the missing names, is the next best
//  thing to failing at compile time, and it is how frameworks validate
//  handler registries and serializers.
//  The walk is recursive: a sealed type's permitted subclasses are leaves
//  or more sealed types, and the handler for a leaf is its own, or the
//  nearest one inherited on the way down. Resolving every leaf once in
//  build() makes dispatch a single map lookup on value.getClass().
//  The registry stores Function<Object, R> wrappers that type.cast(...)
//  the value. The generic on(Class<S>, Function<? super S, ...>) has
//  already guaranteed the cast cannot fail, and a raw call will fail at
//  the cast instead of deep inside a handler.

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
        var names = new TreeSet<String>();
        collectLeaves(root, names);
        return new ArrayList<>(names);
    }

    private static void collectLeaves(Class<?> type, Set<String> names) {
        if (!type.isSealed()) {
            names.add(type.getSimpleName());
            return;
        }
        for (var sub : type.getPermittedSubclasses()) collectLeaves(sub, names);
    }

    static final class Dispatcher<B, R> {
        private final Map<Class<?>, Function<Object, ? extends R>> byLeaf;

        private Dispatcher(Map<Class<?>, Function<Object, ? extends R>> byLeaf) {
            this.byLeaf = byLeaf;
        }

        static <B, R> Builder<B, R> forSealed(Class<B> root) {
            if (!root.isSealed()) throw new IllegalArgumentException(root.getSimpleName() + " is not sealed");
            return new Builder<>(root);
        }

        R dispatch(B value) {
            var handler = byLeaf.get(value.getClass());
            if (handler == null) throw new IllegalArgumentException("no handler for " + value.getClass().getSimpleName());
            return handler.apply(value);
        }

        static final class Builder<B, R> {
            private final Class<B> root;
            private final Map<Class<?>, Function<Object, ? extends R>> handlers = new HashMap<>();

            private Builder(Class<B> root) {
                this.root = root;
            }

            <S extends B> Builder<B, R> on(Class<S> type, Function<? super S, ? extends R> handler) {
                if (handlers.containsKey(type)) throw new IllegalStateException("already handled: " + type.getSimpleName());
                handlers.put(type, value -> handler.apply(type.cast(value)));
                return this;
            }

            Dispatcher<B, R> build() {
                var resolved = new HashMap<Class<?>, Function<Object, ? extends R>>();
                var missing = new TreeSet<String>();
                resolve(root, null, resolved, missing);
                if (!missing.isEmpty()) throw new IllegalStateException("unhandled: " + String.join(", ", missing));
                return new Dispatcher<>(resolved);
            }

            private void resolve(Class<?> type, Function<Object, ? extends R> inherited,
                                 Map<Class<?>, Function<Object, ? extends R>> resolved, Set<String> missing) {
                var own = handlers.containsKey(type) ? handlers.get(type) : inherited;
                if (!type.isSealed()) {
                    if (own == null) missing.add(type.getSimpleName());
                    else resolved.put(type, own);
                    return;
                }
                for (var sub : type.getPermittedSubclasses()) resolve(sub, own, resolved, missing);
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
