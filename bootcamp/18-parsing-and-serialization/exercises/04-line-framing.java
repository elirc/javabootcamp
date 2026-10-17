// ─────────────────────────────────────────────────────────────────────────
//  04 · frame a line protocol                               ★★☆ core
//  concepts: framing · partial reads · CRLF · max frame length
//  run: java -cp ../../_lib/out 04-line-framing.java
// ─────────────────────────────────────────────────────────────────────────
//
//  TCP delivers bytes, not messages: one read can hold half a line or
//  three lines. Redis, SMTP and IRC frame messages with line ends. Build
//  LineDecoder(maxLength):
//  - feed(chunk) returns the frames COMPLETED by this chunk, in order;
//    a frame ends at "\n", which is not part of it; a "\r" right before
//    the "\n" is dropped too, even if it came in an earlier chunk; any
//    other "\r" is data. An empty line is the frame "".
//  - a frame longer than maxLength (not counting that CR) is DROPPED:
//    discard everything up to and including its "\n", add 1 to dropped(),
//    and never hold more than maxLength + 1 chars while you do.
//  - buffered() is how many chars are held for the unfinished frame
//    (0 while discarding).
//  encode(frame) returns frame + "\n"; a frame holding "\n" or "\r":
//  IllegalArgumentException.
//
//      feed("hel") → []    feed("lo\r") → []    feed("\nhi\n") → [hello, hi]
//
//  hint: count the over-long frame when you START discarding it.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final class LineDecoder {
        LineDecoder(int maxLength) {
            throw new UnsupportedOperationException("TODO");
        }

        List<String> feed(String chunk) {
            throw new UnsupportedOperationException("TODO");
        }

        int buffered() {
            throw new UnsupportedOperationException("TODO");
        }

        int dropped() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static String encode(String frame) {
        throw new UnsupportedOperationException("TODO");
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
