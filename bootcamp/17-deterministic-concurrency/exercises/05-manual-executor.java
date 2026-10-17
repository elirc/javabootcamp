// ─────────────────────────────────────────────────────────────────────────
//  05 · a manual executor for tests                         ★★☆ core
//  concepts: Executor injection · deterministic scheduling · ExecutorService
//  run: java -cp ../../_lib/out 05-manual-executor.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Code that takes an Executor can be tested with one YOU drive. Finish:
//    ManualExecutor   execute(task) queues it (null → NullPointerException)
//                     and runs nothing; pending() counts queued tasks;
//                     runNext() runs the oldest, false if none; a task's
//                     RuntimeException is caught and its message added to
//                     failures(); runAll() runs until the queue is empty,
//                     including tasks queued meanwhile, and returns how
//                     many ran; if 10,000 have run and more are still
//                     queued → IllegalStateException
//    EmailBatcher     sendAll(recipients) queues one task per recipient,
//                     in order; a send that throws is queued ONCE more at
//                     the back; failing again adds it to gaveUp()
//    runInOrder(single, tasks)  run Callables on a given ExecutorService
//                     and return each result in INPUT order, or
//                     "error: <cause's message>" for a task that threw
//
//      sendAll([ana, bo, cy]) with bo failing once → sent ana, cy, bo
//
//  hint: invokeAll returns the futures in the order of the tasks.

import static bootcamp.Check.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

class Exercise {

    static final class ManualExecutor implements Executor {
        static final int RUNAWAY = 10_000;
        private final Deque<Runnable> queue = new ArrayDeque<>();
        private final List<String> failures = new ArrayList<>();

        @Override
        public void execute(Runnable task) {
            throw new UnsupportedOperationException("TODO");
        }

        int pending() {
            throw new UnsupportedOperationException("TODO");
        }

        boolean runNext() {
            throw new UnsupportedOperationException("TODO");
        }

        int runAll() {
            throw new UnsupportedOperationException("TODO");
        }

        List<String> failures() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class EmailBatcher {
        private final Executor executor;
        private final Consumer<String> mailer;
        private final List<String> gaveUp = new ArrayList<>();

        EmailBatcher(Executor executor, Consumer<String> mailer) {
            this.executor = executor;
            this.mailer = mailer;
        }

        void sendAll(List<String> recipients) {
            throw new UnsupportedOperationException("TODO");
        }

        List<String> gaveUp() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static List<String> runInOrder(ExecutorService single, List<Callable<String>> tasks) throws InterruptedException {
        throw new UnsupportedOperationException("TODO");
    }

    // A fake mailer: fails for each name as many times as listed.
    static Consumer<String> flaky(List<String> sent, String... failures) {
        var left = new ArrayList<>(List.of(failures));
        return to -> {
            if (left.remove(to)) throw new IllegalStateException("smtp refused " + to);
            sent.add(to);
        };
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("nothing runs until the test says so, then tasks run oldest first", () -> {
            var ex = new ManualExecutor();
            var log = new ArrayList<String>();
            ex.execute(() -> log.add("a"));
            ex.execute(() -> log.add("b"));
            eq(log, List.of());
            eq(ex.pending(), 2);
            ok(ex.runNext());
            eq(log, List.of("a"));
            ok(ex.runNext());
            ok(!ex.runNext());
            eq(log, List.of("a", "b"));
        });
        test("tasks queued by a running task go to the back, and runAll runs them", () -> {
            var ex = new ManualExecutor();
            var log = new ArrayList<String>();
            ex.execute(() -> { log.add("parent"); ex.execute(() -> log.add("child")); });
            ex.execute(() -> log.add("sibling"));
            eq(ex.runAll(), 3);
            eq(log, List.of("parent", "sibling", "child"));
            eq(ex.pending(), 0);
            raises(NullPointerException.class, () -> ex.execute(null));
        });
        test("a failing task is recorded and does not stop runAll", () -> {
            var ex = new ManualExecutor();
            var log = new ArrayList<String>();
            ex.execute(() -> { throw new IllegalStateException("disk full"); });
            ex.execute(() -> log.add("after"));
            eq(ex.runAll(), 2);
            eq(log, List.of("after"));
            eq(ex.failures(), List.of("disk full"));
        });
        test("runAll refuses to spin forever on a task that requeues itself", () -> {
            var ex = new ManualExecutor();
            var runs = new int[1];
            var forever = new Runnable[1];
            forever[0] = () -> { runs[0]++; ex.execute(forever[0]); };
            ex.execute(forever[0]);
            raises(IllegalStateException.class, ex::runAll);
            eq(runs[0], ManualExecutor.RUNAWAY);
        });
        test("EmailBatcher retries a failed send once, at the back of the queue", () -> {
            var ex = new ManualExecutor();
            var sent = new ArrayList<String>();
            var batcher = new EmailBatcher(ex, flaky(sent, "bo"));
            batcher.sendAll(List.of("ana", "bo", "cy"));
            eq(ex.pending(), 3);
            eq(ex.runAll(), 4);
            eq(sent, List.of("ana", "cy", "bo"));
            eq(batcher.gaveUp(), List.of());
        });
        test("EmailBatcher gives up after the second failure", () -> {
            var ex = new ManualExecutor();
            var sent = new ArrayList<String>();
            var batcher = new EmailBatcher(ex, flaky(sent, "bo", "bo"));
            batcher.sendAll(List.of("ana", "bo"));
            ex.runAll();
            eq(sent, List.of("ana"));
            eq(batcher.gaveUp(), List.of("bo"));
        });
        test("with a direct executor everything happens inside sendAll", () -> {
            Executor direct = Runnable::run;
            var sent = new ArrayList<String>();
            var batcher = new EmailBatcher(direct, flaky(sent, "bo"));
            batcher.sendAll(List.of("ana", "bo", "cy"));
            eq(sent, List.of("ana", "bo", "cy"));
        });
        test("runInOrder keeps input order and reports failures in place", () -> {
            var single = Executors.newSingleThreadExecutor();
            try {
                List<Callable<String>> tasks = List.of(() -> "one", () -> { throw new IllegalStateException("boom"); }, () -> "three");
                eq(runInOrder(single, tasks), List.of("one", "error: boom", "three"));
            } finally {
                single.shutdownNow();
            }
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
