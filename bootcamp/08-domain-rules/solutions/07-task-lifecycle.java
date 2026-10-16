// ─────────────────────────────────────────────────────────────────────────
//  07 · a task remembers its story — SOLUTION               ★★☆ core
//  concepts: lifecycle methods · audit history · read-only views · guards
//  run: java -cp ../../_lib/out 07-task-lifecycle.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Each public method names a business action (start, finish, reopen)
//  rather than exposing setState(...). That keeps the transition rules
//  inside the class: no caller can jump TODO → DONE, because no method
//  does that.
//  The three methods share a shape — require a state, change it, record
//  it — so a private move(expected, next) does all three; each public
//  method is one line. The check comes first, so a refused move records
//  nothing and changes nothing.
//  History is an audit trail: append-only from inside, read-only from
//  outside. Returning Collections.unmodifiableList(history) gives callers
//  a live, read-only view; List.copyOf(history) would give a snapshot.
//  Either is fine here — returning the ArrayList itself is not, because a
//  caller could rewrite the past.
//  timesReopened is DERIVED data: it could be counted from history
//  (entries equal to "DONE -> TODO"). Storing a separate counter is
//  faster but is one more thing to keep in sync; deriving it can never
//  disagree with the history. For small lists, derive.
//  The message names the current state ("cannot finish a TODO task")
//  because the caller already knows what they tried — what they need is
//  why it failed.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    enum State { TODO, DOING, DONE }

    static final class Task {
        private final String title;
        private State state = State.TODO;
        private final List<String> history = new ArrayList<>();

        Task(String title) {
            if (title == null || title.isBlank()) throw new IllegalArgumentException("a task needs a title");
            this.title = title.strip();
        }

        String title() {
            return title;
        }

        State state() {
            return state;
        }

        void start() {
            move(State.TODO, State.DOING, "start");
        }

        void finish() {
            move(State.DOING, State.DONE, "finish");
        }

        void reopen() {
            move(State.DONE, State.TODO, "reopen");
        }

        List<String> history() {
            return Collections.unmodifiableList(history);
        }

        long timesReopened() {
            return history.stream().filter("DONE -> TODO"::equals).count();
        }

        private void move(State expected, State next, String action) {
            if (state != expected) throw new IllegalStateException("cannot " + action + " a " + state + " task");
            history.add(state + " -> " + next);
            state = next;
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("a new task is TODO with no history", () -> {
            var t = new Task("write report");
            eq(t.state(), State.TODO);
            eq(t.history(), List.of());
        });
        test("requires a title", () -> raises(IllegalArgumentException.class, () -> new Task("  ")));
        test("records every move", () -> {
            var t = new Task("write report");
            t.start();
            t.finish();
            t.reopen();
            eq(t.history(), List.of("TODO -> DOING", "DOING -> DONE", "DONE -> TODO"));
            eq(t.state(), State.TODO);
        });
        test("cannot skip a step", () -> {
            var t = new Task("write report");
            raises(IllegalStateException.class, t::finish, "TODO");
            raises(IllegalStateException.class, t::reopen);
        });
        test("a refused move records nothing", () -> {
            var t = new Task("write report");
            t.start();
            raises(IllegalStateException.class, t::start);
            eq(t.history(), List.of("TODO -> DOING"));
        });
        test("history is read-only", () -> {
            var t = new Task("write report");
            t.start();
            raises(UnsupportedOperationException.class, () -> t.history().clear());
        });
        test("counts reopens", () -> {
            var t = new Task("write report");
            for (int i = 0; i < 2; i++) {
                t.start();
                t.finish();
                t.reopen();
            }
            eq(t.timesReopened(), 2);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
