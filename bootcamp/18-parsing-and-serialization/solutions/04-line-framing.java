// ─────────────────────────────────────────────────────────────────────────
//  04 · frame a line protocol — SOLUTION                    ★★☆ core
//  concepts: framing · partial reads · CRLF · max frame length
//  run: java -cp ../../_lib/out 04-line-framing.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A stream socket gives you bytes in whatever pieces the network chose,
//  so a decoder keeps state BETWEEN reads: the unfinished frame. Each
//  char is either "\n" (the frame is done: emit it, minus one trailing
//  "\r") or data (append it). Because the "\r" is only stripped when the
//  "\n" arrives, a CRLF split across two reads still works, and a "\r"
//  in the middle of a line stays data.
//  The second job is defence. Without a limit, a client that never sends
//  "\n" makes the server buffer forever: that is a memory-exhaustion
//  attack, and why Netty's LineBasedFrameDecoder takes a maxLength. The
//  moment the content passes maxLength, clear the buffer, count the
//  frame as dropped ONCE, and switch to discarding until the next "\n".
//  That keeps memory bounded at maxLength + 1 (room for a final CR).
//  The classic wrong turn is checking the length only when the "\n"
//  arrives: correct output, unbounded memory. The other is counting the
//  drop per chunk instead of per frame.
//  encode is the other half of the contract: a frame that contains a line
//  break would be split in two by the reader, so it is refused.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class LineDecoder {
        private final int maxLength;
        private final StringBuilder buf = new StringBuilder();
        private boolean discarding;
        private int dropped;

        LineDecoder(int maxLength) {
            this.maxLength = maxLength;
        }

        List<String> feed(String chunk) {
            var frames = new ArrayList<String>();
            for (int i = 0; i < chunk.length(); i++) {
                char c = chunk.charAt(i);
                if (c == '\n') {
                    if (!discarding) {
                        int end = buf.length() > 0 && buf.charAt(buf.length() - 1) == '\r' ? buf.length() - 1 : buf.length();
                        frames.add(buf.substring(0, end));
                    }
                    buf.setLength(0);
                    discarding = false;
                } else if (!discarding) {
                    buf.append(c);
                    int content = c == '\r' ? buf.length() - 1 : buf.length();
                    if (content > maxLength) {
                        buf.setLength(0);
                        discarding = true;
                        dropped++;
                    }
                }
            }
            return frames;
        }

        int buffered() {
            return buf.length();
        }

        int dropped() {
            return dropped;
        }
    }

    static String encode(String frame) {
        if (frame.indexOf('\n') >= 0 || frame.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("a frame cannot contain a line break");
        }
        return frame + "\n";
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("returns each complete line without its line end", () -> {
            var d = new LineDecoder(100);
            eq(d.feed("hello\nworld\n"), List.of("hello", "world"));
            eq(d.buffered(), 0);
        });
        test("buffers a partial line until its newline arrives", () -> {
            var d = new LineDecoder(100);
            eq(d.feed("hel"), List.of());
            eq(d.buffered(), 3);
            eq(d.feed("lo\nwo"), List.of("hello"));
            eq(d.buffered(), 2);
            eq(d.feed("rld\n"), List.of("world"));
        });
        test("drops the CR of a CRLF even when it is split across chunks", () -> {
            var d = new LineDecoder(100);
            eq(d.feed("a\r"), List.of());
            eq(d.feed("\nb\r\n"), List.of("a", "b"));
            eq(d.feed("x\ry\r"), List.of());
            eq(d.feed("\r\n"), List.of("x\ry\r"));
        });
        test("an empty line is an empty frame", () -> {
            eq(new LineDecoder(10).feed("\n\r\n\n"), List.of("", "", ""));
        });
        test("a frame of exactly maxLength is kept, with or without CR", () -> {
            var d = new LineDecoder(3);
            eq(d.feed("abc\nxyz\r"), List.of("abc"));
            eq(d.feed("\n"), List.of("xyz"));
            eq(d.dropped(), 0);
        });
        test("an over-long frame is dropped up to its newline and counted once", () -> {
            var d = new LineDecoder(3);
            eq(d.feed("abcd"), List.of());
            eq(d.buffered(), 0);
            eq(d.feed("efgh"), List.of());
            eq(d.feed("ij\nok\nabc\r\r\nz\n"), List.of("ok", "z"));
            eq(d.dropped(), 2);
        });
        test("never holds more than maxLength + 1 chars", () -> {
            var d = new LineDecoder(5);
            for (int i = 0; i < 200; i++) {
                d.feed("xxxxxxx");
                ok(d.buffered() <= 6, "buffered " + d.buffered());
            }
            eq(d.feed("\nhi\n"), List.of("hi"));
            eq(d.dropped(), 1);
        });
        test("encode appends a newline and refuses embedded line breaks", () -> {
            eq(encode("PING"), "PING\n");
            eq(encode(""), "\n");
            raises(IllegalArgumentException.class, () -> encode("a\nb"));
            raises(IllegalArgumentException.class, () -> encode("a\r"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
