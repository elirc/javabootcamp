// ─────────────────────────────────────────────────────────────────────────
//  08 · command: a cart with undo                           ★★★ stretch
//  concepts: command pattern · undo/redo stacks · macros · all-or-nothing
//  run: java -cp ../../_lib/out 08-command-undo.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A shopping cart edited through Commands, with undo and redo. Each
//  command remembers, in execute, what it needs to undo itself exactly:
//    AddItem(sku, n)      adds n (n < 1 → IllegalArgumentException)
//    RemoveItem(sku)      removes the line; absent → IllegalStateException
//    SetQuantity(sku, n)  sets n, and 0 removes the line; n < 0 →
//                         IllegalArgumentException
//    Macro(commands)      runs them in order; if one throws, undoes the
//                         ones already run, newest first, and rethrows;
//                         its undo undoes all of them, newest first
//  describe(): "add 2 x A1", "remove A1", "set A1 to 5",
//  "macro(add 1 x A, remove B)". History(cart): run(c) executes c and
//  pushes it on the undo stack (a command that throws is not recorded)
//  and clears the redo stack; undo() and redo() return false when there
//  is nothing to do; log() is describe() of the undo stack, oldest first.
//
//      run(add 2 x A); run(set A to 5); undo() → A is 2 again
//
//  hint: undo must restore what was there, not compute an inverse.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class Cart {
        private final Map<String, Integer> lines = new TreeMap<>();

        int qty(String sku) {
            return lines.getOrDefault(sku, 0);
        }

        boolean has(String sku) {
            return lines.containsKey(sku);
        }

        void put(String sku, int qty) {
            if (qty == 0) lines.remove(sku);
            else lines.put(sku, qty);
        }

        Map<String, Integer> snapshot() {
            return Map.copyOf(lines);
        }
    }

    interface Command {
        void execute(Cart cart);
        void undo(Cart cart);
        String describe();
    }

    static final class AddItem implements Command {
        private final String sku;
        private final int n;
        private int before;

        AddItem(String sku, int n) {
            this.sku = sku;
            this.n = n;
        }

        public void execute(Cart cart) {
            throw new UnsupportedOperationException("TODO");
        }

        public void undo(Cart cart) {
            throw new UnsupportedOperationException("TODO");
        }

        public String describe() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class RemoveItem implements Command {
        private final String sku;
        private int removed;

        RemoveItem(String sku) {
            this.sku = sku;
        }

        public void execute(Cart cart) {
            throw new UnsupportedOperationException("TODO");
        }

        public void undo(Cart cart) {
            throw new UnsupportedOperationException("TODO");
        }

        public String describe() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class SetQuantity implements Command {
        private final String sku;
        private final int n;
        private int before;

        SetQuantity(String sku, int n) {
            this.sku = sku;
            this.n = n;
        }

        public void execute(Cart cart) {
            throw new UnsupportedOperationException("TODO");
        }

        public void undo(Cart cart) {
            throw new UnsupportedOperationException("TODO");
        }

        public String describe() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class Macro implements Command {
        private final List<Command> steps;

        Macro(List<Command> steps) {
            this.steps = List.copyOf(steps);
        }

        public void execute(Cart cart) {
            throw new UnsupportedOperationException("TODO");
        }

        public void undo(Cart cart) {
            throw new UnsupportedOperationException("TODO");
        }

        public String describe() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class History {
        private final Cart cart;
        private final Deque<Command> undo = new ArrayDeque<>();
        private final Deque<Command> redo = new ArrayDeque<>();

        History(Cart cart) {
            this.cart = cart;
        }

        void run(Command c) {
            throw new UnsupportedOperationException("TODO");
        }

        boolean undo() {
            throw new UnsupportedOperationException("TODO");
        }

        boolean redo() {
            throw new UnsupportedOperationException("TODO");
        }

        List<String> log() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("run executes a command and undo restores the cart", () -> {
            var cart = new Cart();
            var h = new History(cart);
            h.run(new AddItem("A", 2));
            h.run(new AddItem("A", 3));
            eq(cart.snapshot(), Map.of("A", 5));
            ok(h.undo());
            eq(cart.snapshot(), Map.of("A", 2));
            ok(h.undo());
            eq(cart.snapshot(), Map.of());
        });
        test("undo and redo walk back and forth, and say false when there is nothing", () -> {
            var cart = new Cart();
            var h = new History(cart);
            ok(!h.undo());
            ok(!h.redo());
            h.run(new AddItem("A", 1));
            h.run(new SetQuantity("A", 5));
            ok(h.undo());
            eq(cart.qty("A"), 1);
            ok(h.redo());
            eq(cart.qty("A"), 5);
            ok(!h.redo());
        });
        test("running a new command clears the redo stack", () -> {
            var cart = new Cart();
            var h = new History(cart);
            h.run(new AddItem("A", 1));
            h.undo();
            h.run(new AddItem("B", 1));
            ok(!h.redo());
            eq(cart.snapshot(), Map.of("B", 1));
        });
        test("RemoveItem and SetQuantity restore exactly what they replaced", () -> {
            var cart = new Cart();
            var h = new History(cart);
            h.run(new AddItem("A", 7));
            h.run(new RemoveItem("A"));
            eq(cart.has("A"), false);
            h.undo();
            eq(cart.qty("A"), 7);
            h.run(new SetQuantity("A", 0));
            eq(cart.has("A"), false);
            h.undo();
            eq(cart.qty("A"), 7);
            h.run(new SetQuantity("N", 3));
            h.undo();
            eq(cart.has("N"), false);
        });
        test("a command that throws changes nothing and is not recorded", () -> {
            var cart = new Cart();
            var h = new History(cart);
            h.run(new AddItem("A", 1));
            raises(IllegalStateException.class, () -> h.run(new RemoveItem("ghost")));
            raises(IllegalArgumentException.class, () -> h.run(new AddItem("A", 0)));
            raises(IllegalArgumentException.class, () -> h.run(new SetQuantity("A", -2)));
            eq(h.log(), List.of("add 1 x A"));
            eq(cart.snapshot(), Map.of("A", 1));
        });
        test("a macro is all or nothing: a failing step rolls back the earlier ones", () -> {
            var cart = new Cart();
            var h = new History(cart);
            h.run(new AddItem("A", 1));
            var bad = new Macro(List.of(new AddItem("A", 2), new SetQuantity("A", 9), new AddItem("B", 2), new RemoveItem("ghost")));
            raises(IllegalStateException.class, () -> h.run(bad));
            eq(cart.snapshot(), Map.of("A", 1));
            eq(h.log(), List.of("add 1 x A"));
        });
        test("a macro is undone newest step first, as one history entry", () -> {
            var cart = new Cart();
            var h = new History(cart);
            h.run(new Macro(List.of(new AddItem("A", 1), new SetQuantity("A", 5), new AddItem("B", 2))));
            eq(cart.snapshot(), Map.of("A", 5, "B", 2));
            ok(h.undo());
            eq(cart.snapshot(), Map.of());
            ok(!h.undo());
        });
        test("log describes the undo stack, oldest first", () -> {
            var cart = new Cart();
            var h = new History(cart);
            h.run(new AddItem("A1", 2));
            h.run(new SetQuantity("A1", 5));
            h.run(new Macro(List.of(new AddItem("B", 1), new RemoveItem("A1"))));
            eq(h.log(), List.of("add 2 x A1", "set A1 to 5", "macro(add 1 x B, remove A1)"));
            h.undo();
            eq(h.log(), List.of("add 2 x A1", "set A1 to 5"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
