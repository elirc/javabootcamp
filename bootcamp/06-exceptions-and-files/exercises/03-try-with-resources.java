// ─────────────────────────────────────────────────────────────────────────
//  03 · close everything, always                            ★★☆ core
//  concepts: try-with-resources · AutoCloseable · close order · suppressed
//  run: java -cp ../../_lib/out 03-try-with-resources.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A file copier opens an input and an output. Tracked (above the tests)
//  is a stand-in resource that logs "open", "close" and can fail on close.
//  Write copy: open "in" then "out" (out fails on close when failOnClose
//  is set), log "copy", and when failInCopy is set throw IOException
//  ("copy failed"). Both resources must ALWAYS be closed, newest first,
//  and no exception may be lost.
//
//      copy(log, false, false)  → log: [open in, open out, copy,
//                                       close out, close in]
//      copy(log, true, true)    → throws IOException "copy failed" whose
//                                 suppressed[0] is the close failure
//
//  hint: declare both resources in ONE try (...) header.

import static bootcamp.Check.*;

import java.io.IOException;
import java.util.*;

class Exercise {

    static final class Tracked implements AutoCloseable {
        private final String name;
        private final List<String> log;
        private final boolean failOnClose;

        Tracked(String name, List<String> log, boolean failOnClose) {
            this.name = name;
            this.log = log;
            this.failOnClose = failOnClose;
            log.add("open " + name);
        }

        @Override
        public void close() {
            log.add("close " + name);
            if (failOnClose) throw new IllegalStateException("closing " + name + " failed");
        }
    }

    static void copy(List<String> log, boolean failInCopy, boolean failOnClose) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("opens, copies and closes in reverse order", () -> {
            var log = new ArrayList<String>();
            copy(log, false, false);
            eq(log, List.of("open in", "open out", "copy", "close out", "close in"));
        });
        test("closes both when the copy fails", () -> {
            var log = new ArrayList<String>();
            raises(IOException.class, () -> copy(log, true, false));
            eq(log, List.of("open in", "open out", "copy", "close out", "close in"));
        });
        test("the copy failure reaches the caller", () -> {
            var log = new ArrayList<String>();
            raises(IOException.class, () -> copy(log, true, false), "copy failed");
        });
        test("a close failure during a failed copy is suppressed, not lost", () -> {
            var log = new ArrayList<String>();
            var e = raises(IOException.class, () -> copy(log, true, true));
            eq(e.getSuppressed().length, 1);
            eq(e.getSuppressed()[0].getMessage(), "closing out failed");
        });
        test("a close failure alone is the exception", () -> {
            var log = new ArrayList<String>();
            raises(IllegalStateException.class, () -> copy(log, false, true), "closing out failed");
        });
        test("in is closed even when out fails to close", () -> {
            var log = new ArrayList<String>();
            raises(IllegalStateException.class, () -> copy(log, false, true));
            eq(log.get(log.size() - 1), "close in");
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
