// ─────────────────────────────────────────────────────────────────────────
//  06 · score the quiz — SOLUTION                           ★★☆ core
//  concepts: domain records · foreign-key checks · Integer vs int · rounding
//  run: java -cp ../../_lib/out 06-quiz-scoring.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Which collection drives the loop decides which cases you can miss.
//  Walking the answers map would never visit an unanswered question, so
//  it would silently be neither earned nor missed. Walking the questions —
//  the authoritative list — visits every one exactly once, in quiz order,
//  and "missing answer" falls out as answers.get(id) == null.
//  Integer vs int again: answers.get(id) is an Integer that may be null;
//  compare with Objects.equals(answer, q.correct()) or check null first.
//  `answers.get(id) == q.correct()` happens to work here (unboxing),
//  until the answer is missing and unboxing null throws.
//  Validate foreign keys up front: an answer for a question id that does
//  not exist means the client and server disagree about the quiz — a bug
//  to surface, not a zero to award. A Set of known ids makes it one
//  containsAll check, with the bad ids in the message.
//  percent is integer arithmetic with half-up rounding: (earned * 100 * 2
//  + possible) / (possible * 2), or Math.round(100.0 * earned / possible)
//  — doubles are fine for a display percentage, never for money. Guard
//  the zero denominator first.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Question(String id, int correct, int points) {}

    record Score(int earned, int possible, List<String> missed) {}

    static Score score(List<Question> questions, Map<String, Integer> answers) {
        var known = new HashSet<String>();
        for (var q : questions) known.add(q.id());
        var unknown = new TreeSet<>(answers.keySet());
        unknown.removeAll(known);
        if (!unknown.isEmpty()) throw new IllegalArgumentException("answers for unknown questions: " + unknown);

        int earned = 0, possible = 0;
        var missed = new ArrayList<String>();
        for (var q : questions) {
            possible += q.points();
            if (Objects.equals(answers.get(q.id()), q.correct())) earned += q.points();
            else missed.add(q.id());
        }
        return new Score(earned, possible, missed);
    }

    static int percent(Score score) {
        if (score.possible() == 0) return 0;
        return (int) Math.round(100.0 * score.earned() / score.possible());
    }

    static final List<Question> QUIZ = List.of(
        new Question("q1", 2, 1),
        new Question("q2", 0, 3),
        new Question("q3", 1, 2));

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("a perfect score", () -> eq(score(QUIZ, Map.of("q1", 2, "q2", 0, "q3", 1)), new Score(6, 6, List.of())));
        test("wrong answers earn nothing and are listed", () ->
            eq(score(QUIZ, Map.of("q1", 2, "q2", 1, "q3", 1)), new Score(3, 6, List.of("q2"))));
        test("unanswered questions are missed too, in quiz order", () ->
            eq(score(QUIZ, Map.of("q2", 0)), new Score(3, 6, List.of("q1", "q3"))));
        test("an answer to an unknown question is an error", () ->
            raises(IllegalArgumentException.class, () -> score(QUIZ, Map.of("q1", 2, "q9", 0)), "q9"));
        test("percent rounds to a whole number", () -> {
            eq(percent(new Score(1, 4, List.of())), 25);
            eq(percent(new Score(1, 3, List.of())), 33);
            eq(percent(new Score(2, 3, List.of())), 67);
        });
        test("percent rounds a half up", () -> eq(percent(new Score(1, 8, List.of())), 13));
        test("percent of an empty quiz is zero", () -> eq(percent(score(List.of(), Map.of())), 0));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
