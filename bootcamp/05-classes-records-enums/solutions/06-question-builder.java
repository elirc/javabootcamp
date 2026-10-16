// ─────────────────────────────────────────────────────────────────────────
//  06 · building a quiz question — SOLUTION                 ★★☆ core
//  concepts: builder pattern · fluent API · validation · List.copyOf
//  run: java -cp ../../_lib/out 06-question-builder.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A record with five components and a constructor full of positional
//  arguments is unreadable at the call site — new Question("2+2?",
//  List.of("3","4"), 1, 1): which 1 is which? A builder names each part,
//  lets optional parts default, and gathers validation in ONE place:
//  build().
//  Each setter stores its value and returns this, so calls chain. The
//  builder is mutable on purpose; the thing it builds is not.
//  Why validate in build() and not in each setter? Because some rules
//  involve several fields (the answer index depends on how many options
//  there are), and options arrive one call at a time. IllegalStateException
//  fits: no argument was bad on its own, the builder is in a state that
//  cannot produce a valid Question.
//  The built Question copies the options with List.copyOf — an
//  unmodifiable snapshot. Without the copy, a caller who keeps using the
//  builder (adding another option) would change an already-built
//  question behind its back. (Records' compact constructors are a good
//  place for that copy, so even code that bypasses the builder is safe.)

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
            this.text = text;
            return this;
        }

        Builder option(String option) {
            options.add(option);
            return this;
        }

        Builder answer(int index) {
            this.answer = index;
            return this;
        }

        Builder points(int points) {
            this.points = points;
            return this;
        }

        Question build() {
            if (text == null || text.isBlank()) throw new IllegalStateException("question text is required");
            if (options.size() < 2) throw new IllegalStateException("a question needs at least 2 options");
            if (answer < 0 || answer >= options.size()) throw new IllegalStateException("answer must index an option");
            if (points < 1) throw new IllegalStateException("points must be at least 1");
            return new Question(text, List.copyOf(options), answer, points);
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
