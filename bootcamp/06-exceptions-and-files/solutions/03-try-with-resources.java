// ─────────────────────────────────────────────────────────────────────────
//  03 · close everything, always — SOLUTION                 ★★☆ core
//  concepts: try-with-resources · AutoCloseable · close order · suppressed
//  run: java -cp ../../_lib/out 03-try-with-resources.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  try-with-resources — `try (var in = ...; var out = ...) { ... }` —
//  closes every declared resource when the block exits, normally or by
//  exception, in REVERSE order of opening: out, then in. Reverse order
//  matters because later resources often depend on earlier ones (a writer
//  wrapping a stream).
//  The subtle part is two failures at once. If the body throws and then a
//  close() also throws, which exception should the caller see? The body's
//  — it is the real problem — and try-with-resources attaches the close
//  failure to it with addSuppressed. Nothing is lost.
//  Compare the hand-written version: try { body } finally { out.close();
//  in.close(); }. If out.close() throws, in is never closed (a leak), and
//  the close exception REPLACES the body's exception (the real cause is
//  gone). Getting this right by hand takes nested try/finally blocks and
//  explicit addSuppressed calls — which is why Java 7 added the syntax.
//  When only close() fails, there is nothing to suppress it under, so it
//  propagates as the main exception, and in is still closed.
//  Any class implementing AutoCloseable (one method: close) works in the
//  header — files, streams, sockets, locks, your own types.

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
        try (var in = new Tracked("in", log, false);
             var out = new Tracked("out", log, failOnClose)) {
            log.add("copy");
            if (failInCopy) throw new IOException("copy failed");
        }
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
