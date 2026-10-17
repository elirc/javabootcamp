// ─────────────────────────────────────────────────────────────────────────
//  03 · an expression evaluator                             ★★☆ core
//  concepts: algebraic data types · recursion · exact arithmetic · rewriting
//  run: java -cp ../../_lib/out 03-expression-evaluator.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A pricing-formula engine stores formulas as a sealed tree: Num, Var,
//  Add, Mul and Neg records. Finish four recursive functions:
//
//    eval(e, env)   the long value; an unknown Var throws
//                   NoSuchElementException naming it; overflow throws
//                   ArithmeticException
//    show(e)        infix text: "a + b", "a * b", "-x"; an Add operand of a
//                   Mul gets parentheses; a Neg operand that is not a Num
//                   or Var gets parentheses; nothing else does
//    simplify(e)    children first, then: Num op Num folds to a Num;
//                   x + 0 and 0 + x → x; x * 1 and 1 * x → x; x * 0 and
//                   0 * x → 0; -(Num n) → Num(-n); -(-x) → x
//    variables(e)   each Var name once, sorted
//
//      show(Mul(Add(a, 1), b))                 → "(a + 1) * b"
//      simplify(Add(Var x, Mul(Num 2, Num 0))) → Var x
//
//  hint: Math.addExact and friends throw on overflow so you do not have to.

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
        throw new UnsupportedOperationException("TODO");
    }

    static String show(Expr e) {
        throw new UnsupportedOperationException("TODO");
    }

    static Expr simplify(Expr e) {
        throw new UnsupportedOperationException("TODO");
    }

    static List<String> variables(Expr e) {
        throw new UnsupportedOperationException("TODO");
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
