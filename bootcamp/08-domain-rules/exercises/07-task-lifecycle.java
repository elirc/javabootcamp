// ─────────────────────────────────────────────────────────────────────────
//  07 · a task remembers its story                          ★★☆ core
//  concepts: lifecycle methods · audit history · read-only views · guards
//  run: java -cp ../../_lib/out 07-task-lifecycle.java
// ─────────────────────────────────────────────────────────────────────────
//
//  The to-do manager's Task moves TODO → DOING → DONE, and can be
//  reopened from DONE back to TODO. Finish the nested class Task: start,
//  finish and reopen each perform exactly one of those moves (anything
//  else → IllegalStateException naming the current state), and every
//  successful move is appended to history as "TODO -> DOING". A blank
//  title → IllegalArgumentException. history() can't be modified by
//  callers. timesReopened counts reopen calls.
//
//      var t = new Task("write report");
//      t.start(); t.finish(); t.reopen();
//      t.history()   → [TODO -> DOING, DOING -> DONE, DONE -> TODO]
//      t.finish()    → IllegalStateException (it's TODO)
//
//  hint: one private move(from, to) can do the check AND the record.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    enum State { TODO, DOING, DONE }

    static final class Task {
        private final String title;
        private State state = State.TODO;
        private final List<String> history = new ArrayList<>();

        Task(String title) {
            throw new UnsupportedOperationException("TODO");
        }

        String title() {
            return title;
        }

        State state() {
            return state;
        }

        void start() {
            throw new UnsupportedOperationException("TODO");
        }

        void finish() {
            throw new UnsupportedOperationException("TODO");
        }

        void reopen() {
            throw new UnsupportedOperationException("TODO");
        }

        List<String> history() {
            throw new UnsupportedOperationException("TODO");
        }

        long timesReopened() {
            throw new UnsupportedOperationException("TODO");
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
