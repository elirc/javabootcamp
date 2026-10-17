// ─────────────────────────────────────────────────────────────────────────
//  05 · a manual executor for tests — SOLUTION              ★★☆ core
//  concepts: Executor injection · deterministic scheduling · ExecutorService
//  run: java -cp ../../_lib/out 05-manual-executor.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  The surest way to make concurrent code testable is to not create
//  threads in it. EmailBatcher receives an Executor instead of starting
//  threads, so production passes a thread pool and the test passes a
//  ManualExecutor: nothing runs until the test calls runNext() or
//  runAll(), and then it runs in a known order on the test's own thread.
//  No sleeps, no timeouts, no flakiness. Executor direct = Runnable::run
//  is the other extreme: run immediately, inline.
//  The details of the manual executor are exactly what makes it useful.
//  FIFO order (a deque used as a queue, not a stack). Tasks that schedule
//  tasks go to the back and still run within runAll. A failing task is
//  recorded, not thrown into the test that happened to call runAll, and
//  a runaway task that keeps rescheduling itself fails loudly instead of
//  hanging the test.
//  The retry in EmailBatcher is queued at the BACK, after the recipients
//  already waiting, which is how a real queue treats a retry. Running it
//  inline would hide that order.
//  runInOrder shows that a real ExecutorService can also be deterministic
//  in its RESULT: invokeAll returns futures in task order, whatever order
//  the work ran in. Future.get() wraps a task's exception in an
//  ExecutionException; report its getCause().

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
            queue.addLast(Objects.requireNonNull(task, "task"));
        }

        int pending() {
            return queue.size();
        }

        boolean runNext() {
            var task = queue.pollFirst();
            if (task == null) return false;
            try {
                task.run();
            } catch (RuntimeException e) {
                failures.add(e.getMessage());
            }
            return true;
        }

        int runAll() {
            int ran = 0;
            while (runNext()) {
                if (++ran >= RUNAWAY && !queue.isEmpty()) throw new IllegalStateException("runaway: " + ran + " tasks and counting");
            }
            return ran;
        }

        List<String> failures() {
            return List.copyOf(failures);
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
            for (var r : recipients) executor.execute(() -> attempt(r, true));
        }

        private void attempt(String recipient, boolean mayRetry) {
            try {
                mailer.accept(recipient);
            } catch (RuntimeException e) {
                if (mayRetry) executor.execute(() -> attempt(recipient, false));
                else gaveUp.add(recipient);
            }
        }

        List<String> gaveUp() {
            return List.copyOf(gaveUp);
        }
    }

    static List<String> runInOrder(ExecutorService single, List<Callable<String>> tasks) throws InterruptedException {
        var out = new ArrayList<String>();
        for (var f : single.invokeAll(tasks)) {
            try {
                out.add(f.get());
            } catch (ExecutionException e) {
                out.add("error: " + e.getCause().getMessage());
            }
        }
        return out;
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
