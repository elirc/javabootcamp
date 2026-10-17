// ─────────────────────────────────────────────────────────────────────────
//  01 · sealed shapes and records                           ★☆☆ warm-up
//  concepts: sealed interfaces · records · instanceof patterns · guards
//  run: java -cp ../../_lib/out 01-sealed-shapes.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A floor-plan tool knows exactly three shapes: Shape is sealed and
//  permits Circle, Rect and Triangle, each a record. Finish:
//
//    the compact constructors   every size must be > 0 (NaN is rejected
//                               too), and a Triangle's sides must close:
//                               each side SHORTER than the other two
//                               together; else IllegalArgumentException
//    area(s), perimeter(s)      triangle area by Heron's formula
//    describe(s)                "circle r=2.0", "square 3.0" for a Rect
//                               with equal sides, "rect 2.0x3.0",
//                               "triangle 3.0/4.0/5.0"
//    permittedNames()           Shape's permitted subclasses' simple
//                               names, sorted, read at runtime
//
//      area(new Rect(2, 3))      → 6.0
//      describe(new Rect(3, 3))  → "square 3.0"
//
//  hint: `s instanceof Rect r && r.width() == r.height()` is one test.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    sealed interface Shape permits Circle, Rect, Triangle {}

    record Circle(double radius) implements Shape {
        Circle {
            throw new UnsupportedOperationException("TODO");
        }
    }

    record Rect(double width, double height) implements Shape {
        Rect {
            throw new UnsupportedOperationException("TODO");
        }
    }

    record Triangle(double a, double b, double c) implements Shape {
        Triangle {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static double area(Shape s) {
        throw new UnsupportedOperationException("TODO");
    }

    static double perimeter(Shape s) {
        throw new UnsupportedOperationException("TODO");
    }

    static String describe(Shape s) {
        throw new UnsupportedOperationException("TODO");
    }

    static List<String> permittedNames() {
        throw new UnsupportedOperationException("TODO");
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
