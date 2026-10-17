// ─────────────────────────────────────────────────────────────────────────
//  01 · sealed shapes and records — SOLUTION                ★☆☆ warm-up
//  concepts: sealed interfaces · records · instanceof patterns · guards
//  run: java -cp ../../_lib/out 01-sealed-shapes.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A SEALED interface lists every implementation up front (permits ...),
//  so the set of shapes is closed: nobody can add a Hexagon behind your
//  back, and code that handles all three really handles all of them.
//  Records are the natural partner: each case is plain data with
//  equals, hashCode and toString for free, plus a compact constructor
//  for its invariants.
//  Java 17 dispatches with instanceof patterns: `if (s instanceof Circle
//  c) return Math.PI * c.radius() * c.radius();`. The binding c exists
//  only where the test is true, and `&&` can extend it into a guard, as
//  describe does for squares. The chain ends with `throw new
//  AssertionError(...)`: Java 17 cannot prove an if-chain exhaustive. Java
//  21's `switch (s) { case Circle c -> ... }` can, and then the compiler
//  flags a missing case when a shape is added.
//  Validate with !(radius > 0), not radius <= 0: every comparison with
//  NaN is false, so "<= 0" lets NaN through. The triangle check must be
//  strict: sides 1, 2, 3 lie flat and enclose no area.
//  getPermittedSubclasses() (Java 17) reads the permits list by
//  reflection, which the last exercise of this module builds on.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    sealed interface Shape permits Circle, Rect, Triangle {}

    record Circle(double radius) implements Shape {
        Circle {
            if (!(radius > 0)) throw new IllegalArgumentException("radius must be > 0, was " + radius);
        }
    }

    record Rect(double width, double height) implements Shape {
        Rect {
            if (!(width > 0) || !(height > 0)) throw new IllegalArgumentException("sides must be > 0: " + width + "x" + height);
        }
    }

    record Triangle(double a, double b, double c) implements Shape {
        Triangle {
            if (!(a > 0) || !(b > 0) || !(c > 0)) throw new IllegalArgumentException("sides must be > 0");
            if (a >= b + c || b >= a + c || c >= a + b) throw new IllegalArgumentException("sides do not close: " + a + "/" + b + "/" + c);
        }
    }

    static double area(Shape s) {
        if (s instanceof Circle c) return Math.PI * c.radius() * c.radius();
        if (s instanceof Rect r) return r.width() * r.height();
        if (s instanceof Triangle t) {
            double p = (t.a() + t.b() + t.c()) / 2;
            return Math.sqrt(p * (p - t.a()) * (p - t.b()) * (p - t.c()));
        }
        throw new AssertionError("unknown shape " + s);
    }

    static double perimeter(Shape s) {
        if (s instanceof Circle c) return 2 * Math.PI * c.radius();
        if (s instanceof Rect r) return 2 * (r.width() + r.height());
        if (s instanceof Triangle t) return t.a() + t.b() + t.c();
        throw new AssertionError("unknown shape " + s);
    }

    static String describe(Shape s) {
        if (s instanceof Circle c) return "circle r=" + c.radius();
        if (s instanceof Rect r && r.width() == r.height()) return "square " + r.width();
        if (s instanceof Rect r) return "rect " + r.width() + "x" + r.height();
        if (s instanceof Triangle t) return "triangle " + t.a() + "/" + t.b() + "/" + t.c();
        throw new AssertionError("unknown shape " + s);
    }

    static List<String> permittedNames() {
        return Arrays.stream(Shape.class.getPermittedSubclasses()).map(Class::getSimpleName).sorted().toList();
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("area of each shape", () -> {
            approx(area(new Circle(2)), Math.PI * 4, 1e-9);
            approx(area(new Rect(2, 3)), 6.0, 1e-9);
            approx(area(new Triangle(3, 4, 5)), 6.0, 1e-9);
        });
        test("perimeter of each shape", () -> {
            approx(perimeter(new Circle(1)), 2 * Math.PI, 1e-9);
            approx(perimeter(new Rect(2, 3)), 10.0, 1e-9);
            approx(perimeter(new Triangle(3, 4, 5)), 12.0, 1e-9);
        });
        test("records reject sizes that are not positive, NaN included", () -> {
            raises(IllegalArgumentException.class, () -> new Circle(0));
            raises(IllegalArgumentException.class, () -> new Circle(Double.NaN));
            raises(IllegalArgumentException.class, () -> new Rect(2, -1));
            raises(IllegalArgumentException.class, () -> new Triangle(0, 1, 1));
        });
        test("a Triangle's sides must close, strictly", () -> {
            raises(IllegalArgumentException.class, () -> new Triangle(1, 2, 3));
            raises(IllegalArgumentException.class, () -> new Triangle(1, 5, 1));
            approx(area(new Triangle(2, 2, 3)), Math.sqrt(3.5 * 1.5 * 1.5 * 0.5), 1e-9);
        });
        test("describe calls an equal-sided Rect a square", () -> {
            eq(describe(new Rect(3, 3)), "square 3.0");
            eq(describe(new Rect(2, 3)), "rect 2.0x3.0");
        });
        test("describe covers circles and triangles", () -> {
            eq(describe(new Circle(2)), "circle r=2.0");
            eq(describe(new Triangle(3, 4, 5)), "triangle 3.0/4.0/5.0");
        });
        test("permittedNames reads the sealed hierarchy at runtime", () ->
            eq(permittedNames(), List.of("Circle", "Rect", "Triangle")));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
