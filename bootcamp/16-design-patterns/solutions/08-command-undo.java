// ─────────────────────────────────────────────────────────────────────────
//  08 · command: a cart with undo — SOLUTION                ★★★ stretch
//  concepts: command pattern · undo/redo stacks · macros · all-or-nothing
//  run: java -cp ../../_lib/out 08-command-undo.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  COMMAND turns an action into an object. Once "add 2 bolts" is a value
//  it can be queued, logged, replayed, sent over a wire, or, as here,
//  undone. Text editors, drawing tools and database migrations all keep
//  a stack of commands for exactly this reason.
//  The rule that makes undo correct: each command SAVES THE PRIOR STATE
//  in execute and restores it in undo. Computing an inverse ("undo of
//  add 2 is remove 2") breaks as soon as commands interact: set A to 5
//  after adding 2 cannot be undone by "set A to 2" unless you recorded
//  the 2. RemoveItem saves the quantity it removed, SetQuantity the
//  quantity it replaced (or that the line was absent).
//  History keeps two stacks. run pushes on undo and CLEARS redo, because
//  redoing a branch you abandoned would apply commands to a state they
//  never saw. A command that throws is not recorded: undoing something
//  that never happened corrupts the cart.
//  Macro is the composite: an all-or-nothing group. On a failure it rolls
//  back what it already did, newest first, so the cart is exactly as
//  before; its undo is also newest first. Undoing oldest first is the
//  classic bug: with "add 1 x A" then "set A to 5", undoing the add first
//  restores 0, and the set's undo then puts back 1.

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
            if (n < 1) throw new IllegalArgumentException("add needs a positive quantity: " + n);
            before = cart.qty(sku);
            cart.put(sku, before + n);
        }

        public void undo(Cart cart) {
            cart.put(sku, before);
        }

        public String describe() {
            return "add " + n + " x " + sku;
        }
    }

    static final class RemoveItem implements Command {
        private final String sku;
        private int removed;

        RemoveItem(String sku) {
            this.sku = sku;
        }

        public void execute(Cart cart) {
            if (!cart.has(sku)) throw new IllegalStateException(sku + " is not in the cart");
            removed = cart.qty(sku);
            cart.put(sku, 0);
        }

        public void undo(Cart cart) {
            cart.put(sku, removed);
        }

        public String describe() {
            return "remove " + sku;
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
            if (n < 0) throw new IllegalArgumentException("quantity cannot be negative: " + n);
            before = cart.qty(sku);
            cart.put(sku, n);
        }

        public void undo(Cart cart) {
            cart.put(sku, before);
        }

        public String describe() {
            return "set " + sku + " to " + n;
        }
    }

    static final class Macro implements Command {
        private final List<Command> steps;

        Macro(List<Command> steps) {
            this.steps = List.copyOf(steps);
        }

        public void execute(Cart cart) {
            var done = new ArrayDeque<Command>();
            try {
                for (var step : steps) {
                    step.execute(cart);
                    done.push(step);
                }
            } catch (RuntimeException e) {
                while (!done.isEmpty()) done.pop().undo(cart);
                throw e;
            }
        }

        public void undo(Cart cart) {
            for (int i = steps.size() - 1; i >= 0; i--) steps.get(i).undo(cart);
        }

        public String describe() {
            var parts = new ArrayList<String>();
            for (var step : steps) parts.add(step.describe());
            return "macro(" + String.join(", ", parts) + ")";
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
            c.execute(cart);
            undo.push(c);
            redo.clear();
        }

        boolean undo() {
            if (undo.isEmpty()) return false;
            var c = undo.pop();
            c.undo(cart);
            redo.push(c);
            return true;
        }

        boolean redo() {
            if (redo.isEmpty()) return false;
            var c = redo.pop();
            c.execute(cart);
            undo.push(c);
            return true;
        }

        List<String> log() {
            var out = new ArrayList<String>();
            undo.descendingIterator().forEachRemaining(c -> out.add(c.describe()));
            return out;
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
