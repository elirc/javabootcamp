// ─────────────────────────────────────────────────────────────────────────
//  06 · building a quiz question                            ★★☆ core
//  concepts: builder pattern · fluent API · validation · List.copyOf
//  run: java -cp ../../_lib/out 06-question-builder.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A quiz engine's Question has text, options, the index of the right
//  option and a point value. Finish the nested Builder: each setter
//  returns the builder so calls chain; option(...) adds one option;
//  points default to 1. build() checks everything at once and throws
//  IllegalStateException for: blank text, fewer than 2 options, an answer
//  index that is not one of the options, or points below 1. The built
//  Question's option list can never change.
//
//      Question.builder().text("2 + 2?").option("3").option("4")
//          .answer(1).build()                  → points 1, options [3, 4]
//      ...with one option only                  → IllegalStateException
//
//  hint: "return this;" is what makes a setter chainable.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Question(String text, List<String> options, int answer, int points) {
        static Builder builder() {
            return new Builder();
        }
    }

    static final class Builder {
        private String text;
        private final List<String> options = new ArrayList<>();
        private int answer = -1;
        private int points = 1;

        Builder text(String text) {
            throw new UnsupportedOperationException("TODO");
        }

        Builder option(String option) {
            throw new UnsupportedOperationException("TODO");
        }

        Builder answer(int index) {
            throw new UnsupportedOperationException("TODO");
        }

        Builder points(int points) {
            throw new UnsupportedOperationException("TODO");
        }

        Question build() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static Builder twoPlusTwo() {
        return Question.builder().text("2 + 2?").option("3").option("4");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("builds a question from chained calls", () ->
            eq(twoPlusTwo().answer(1).points(2).build(), new Question("2 + 2?", List.of("3", "4"), 1, 2)));
        test("points default to one", () -> eq(twoPlusTwo().answer(1).build().points(), 1));
        test("requires text", () ->
            raises(IllegalStateException.class, () -> Question.builder().option("a").option("b").answer(0).build()));
        test("requires at least two options", () ->
            raises(IllegalStateException.class, () -> Question.builder().text("?").option("a").answer(0).build()));
        test("the answer must be one of the options", () -> {
            raises(IllegalStateException.class, () -> twoPlusTwo().answer(2).build());
            raises(IllegalStateException.class, () -> twoPlusTwo().build());
        });
        test("refuses zero points", () -> raises(IllegalStateException.class, () -> twoPlusTwo().answer(1).points(0).build()));
        test("a built question does not change when the builder does", () -> {
            var builder = twoPlusTwo().answer(1);
            var q = builder.build();
            builder.option("5");
            eq(q.options(), List.of("3", "4"));
        });
        test("a built question's options are read-only", () ->
            raises(UnsupportedOperationException.class, () -> twoPlusTwo().answer(1).build().options().add("x")));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
