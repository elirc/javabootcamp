// ─────────────────────────────────────────────────────────────────────────
//  06 · score the quiz                                      ★★☆ core
//  concepts: domain records · foreign-key checks · Integer vs int · rounding
//  run: java -cp ../../_lib/out 06-quiz-scoring.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A quiz engine scores a player's answers (question id → chosen option).
//  Write score: a right answer earns the question's points; a wrong or
//  missing answer earns 0 and its id is listed in `missed`, in question
//  order; `possible` is the sum of all points. An answer for a question
//  that is not in the quiz → IllegalArgumentException whose message names
//  that id. Then write percent:
//  earned out of possible, rounded to a whole percent (halves up); 0 when
//  nothing was possible. Question and Score are above the tests.
//
//      questions q1 (answer 2, 1 pt), q2 (answer 0, 3 pts)
//      score(questions, {q1=2, q2=1})
//          → Score[earned=1, possible=4, missed=[q2]]
//      percent(that score)              → 25
//
//  hint: walk the QUESTIONS, not the answers, so nothing unanswered is lost.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Question(String id, int correct, int points) {}

    record Score(int earned, int possible, List<String> missed) {}

    static Score score(List<Question> questions, Map<String, Integer> answers) {
        throw new UnsupportedOperationException("TODO");
    }

    static int percent(Score score) {
        throw new UnsupportedOperationException("TODO");
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
