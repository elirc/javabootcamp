// ─────────────────────────────────────────────────────────────────────────
//  03 · an expression evaluator — SOLUTION                  ★★☆ core
//  concepts: algebraic data types · recursion · exact arithmetic · rewriting
//  run: java -cp ../../_lib/out 03-expression-evaluator.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  This is the classic algebraic data type: a sealed interface with one
//  record per case, and functions that recurse over the tree. Each
//  function is an instanceof chain with one branch per case, so adding a
//  new operation (show, simplify, variables...) touches no existing
//  class. Adding a new CASE touches every function: that is the
//  expression problem, and the visitor exercise next looks at the other
//  side of it.
//  eval uses Math.addExact, multiplyExact and negateExact, which throw
//  ArithmeticException on overflow. Plain + wraps silently to a negative
//  price, the worst kind of bug in a pricing engine.
//  show puts parentheses only where precedence needs them: a sum inside a
//  product, and a compound operand of a negation. Parenthesising
//  everything is correct but unreadable; never parenthesising is wrong.
//  simplify must work BOTTOM-UP: simplify the children, then look at the
//  node. Top-down misses rewrites that only appear after a child has been
//  simplified: x + (2 * 0) only becomes x + 0, and then x, if the product
//  is folded first. Records make the rebuilt tree cheap to compare in
//  the tests: two trees are equal when their components are.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    sealed interface Expr permits Num, Var, Add, Mul, Neg {}

    record Num(long value) implements Expr {}

    record Var(String name) implements Expr {}

    record Add(Expr left, Expr right) implements Expr {}

    record Mul(Expr left, Expr right) implements Expr {}

    record Neg(Expr operand) implements Expr {}

    static long eval(Expr e, Map<String, Long> env) {
        if (e instanceof Num n) return n.value();
        if (e instanceof Var v) {
            Long value = env.get(v.name());
            if (value == null) throw new NoSuchElementException("unknown variable: " + v.name());
            return value;
        }
        if (e instanceof Add a) return Math.addExact(eval(a.left(), env), eval(a.right(), env));
        if (e instanceof Mul m) return Math.multiplyExact(eval(m.left(), env), eval(m.right(), env));
        if (e instanceof Neg n) return Math.negateExact(eval(n.operand(), env));
        throw new AssertionError("unhandled " + e);
    }

    static String show(Expr e) {
        if (e instanceof Num n) return Long.toString(n.value());
        if (e instanceof Var v) return v.name();
        if (e instanceof Add a) return show(a.left()) + " + " + show(a.right());
        if (e instanceof Mul m) return factor(m.left()) + " * " + factor(m.right());
        if (e instanceof Neg n) {
            var o = n.operand();
            return "-" + (o instanceof Num || o instanceof Var ? show(o) : "(" + show(o) + ")");
        }
        throw new AssertionError("unhandled " + e);
    }

    private static String factor(Expr e) {
        return e instanceof Add ? "(" + show(e) + ")" : show(e);
    }

    private static boolean isNum(Expr e, long value) {
        return e instanceof Num n && n.value() == value;
    }

    static Expr simplify(Expr e) {
        if (e instanceof Add a) {
            var l = simplify(a.left());
            var r = simplify(a.right());
            if (l instanceof Num x && r instanceof Num y) return new Num(Math.addExact(x.value(), y.value()));
            if (isNum(l, 0)) return r;
            if (isNum(r, 0)) return l;
            return new Add(l, r);
        }
        if (e instanceof Mul m) {
            var l = simplify(m.left());
            var r = simplify(m.right());
            if (l instanceof Num x && r instanceof Num y) return new Num(Math.multiplyExact(x.value(), y.value()));
            if (isNum(l, 0) || isNum(r, 0)) return new Num(0);
            if (isNum(l, 1)) return r;
            if (isNum(r, 1)) return l;
            return new Mul(l, r);
        }
        if (e instanceof Neg n) {
            var o = simplify(n.operand());
            if (o instanceof Num x) return new Num(Math.negateExact(x.value()));
            if (o instanceof Neg inner) return inner.operand();
            return new Neg(o);
        }
        return e;
    }

    static List<String> variables(Expr e) {
        var names = new TreeSet<String>();
        collect(e, names);
        return new ArrayList<>(names);
    }

    private static void collect(Expr e, Set<String> names) {
        if (e instanceof Var v) names.add(v.name());
        else if (e instanceof Add a) { collect(a.left(), names); collect(a.right(), names); }
        else if (e instanceof Mul m) { collect(m.left(), names); collect(m.right(), names); }
        else if (e instanceof Neg n) collect(n.operand(), names);
    }

    static Num num(long v) { return new Num(v); }
    static Var v(String name) { return new Var(name); }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("eval computes numbers, variables, sums, products and negation", () -> {
            var env = Map.of("x", 4L, "y", -2L);
            eq(eval(new Add(num(2), new Mul(v("x"), num(3))), env), 14);
            eq(eval(new Neg(new Mul(v("x"), v("y"))), env), 8);
        });
        test("eval of an unknown variable throws NoSuchElementException naming it", () -> {
            var e = raises(NoSuchElementException.class, () -> eval(new Add(v("x"), v("rate")), Map.of("x", 1L)));
            ok(e.getMessage().contains("rate"), "message: " + e.getMessage());
        });
        test("eval reports overflow as ArithmeticException", () -> {
            raises(ArithmeticException.class, () -> eval(new Mul(num(Long.MAX_VALUE), num(2)), Map.of()));
            raises(ArithmeticException.class, () -> eval(new Add(num(Long.MAX_VALUE), num(1)), Map.of()));
            raises(ArithmeticException.class, () -> eval(new Neg(num(Long.MIN_VALUE)), Map.of()));
        });
        test("show writes infix and brackets a sum inside a product", () -> {
            eq(show(new Add(num(1), new Mul(v("x"), num(2)))), "1 + x * 2");
            eq(show(new Mul(new Add(v("a"), num(1)), v("b"))), "(a + 1) * b");
            eq(show(new Mul(v("b"), new Add(v("a"), num(1)))), "b * (a + 1)");
            eq(show(new Add(new Add(v("a"), v("b")), v("c"))), "a + b + c");
            eq(show(new Add(v("a"), new Add(v("b"), v("c")))), "a + b + c");
            eq(show(new Mul(new Mul(v("a"), v("b")), new Neg(v("c")))), "a * b * -c");
        });
        test("show brackets a compound operand of Neg only", () -> {
            eq(show(new Neg(v("x"))), "-x");
            eq(show(new Neg(new Add(v("a"), num(1)))), "-(a + 1)");
            eq(show(new Neg(new Neg(v("x")))), "-(-x)");
            eq(show(new Add(v("a"), new Neg(v("b")))), "a + -b");
        });
        test("simplify folds constants and removes identity elements", () -> {
            eq(simplify(new Add(new Mul(v("x"), num(1)), num(0))), v("x"));
            eq(simplify(new Add(num(2), num(3))), num(5));
            eq(simplify(new Mul(num(0), v("y"))), num(0));
            eq(simplify(new Mul(v("a"), v("b"))), new Mul(v("a"), v("b")));
        });
        test("simplify works bottom-up, so one rewrite exposes the next", () -> {
            eq(simplify(new Add(v("x"), new Mul(num(2), num(0)))), v("x"));
            eq(simplify(new Mul(new Add(num(1), num(-1)), v("z"))), num(0));
            eq(simplify(new Neg(new Neg(v("x")))), v("x"));
            eq(simplify(new Neg(new Add(num(1), num(3)))), num(-4));
        });
        test("variables lists each name once, sorted", () -> {
            eq(variables(new Add(v("b"), new Mul(v("a"), new Neg(v("b"))))), List.of("a", "b"));
            eq(variables(num(3)), List.of());
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
