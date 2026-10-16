// ─────────────────────────────────────────────────────────────────────────
//  02 · short links in base62 — SOLUTION                    ★☆☆ warm-up
//  concepts: base conversion · URL shorteners · overflow · Math.multiplyExact
//  run: java -cp ../../_lib/out 02-base62-codes.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Base62 is ordinary base conversion with a 62-character digit set: take
//  id % 62 for the last digit, divide, repeat, then reverse (or insert at
//  the front). 0 needs its own case, or the loop never runs and you return
//  "". Decoding is Horner's rule: value = value * 62 + digit, left to right.
//  Why 62? Those are the characters that are safe in a URL path without
//  escaping. bit.ly, TinyURL and every "design a URL shortener" interview
//  answer use it; capacity() is the back-of-envelope step that tells you 7
//  characters cover 3.5 trillion links, so a 64-bit counter never runs out
//  before the code length has to grow.
//  The classic bug is silent overflow in decode. A long wraps around on
//  overflow, so "aZl8N0y58M8" (one past Long.MAX_VALUE) would decode to a
//  large negative id and fetch someone else's link, or crash later far
//  from the cause. Math.multiplyExact/addExact throw ArithmeticException
//  on overflow; translating that into IllegalArgumentException keeps the
//  error at the boundary where the bad input arrived.
//  indexOf(ch) == -1 rejects foreign characters; it also makes the decode
//  case-sensitive for free, because 'a' and 'A' are different digits.
//  Note what base62 does NOT give you: sequential ids produce guessable
//  sequential codes. Real shorteners shuffle ids first (see the README).

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    static String encode(long id) {
        if (id < 0) throw new IllegalArgumentException("negative id: " + id);
        if (id == 0) return "0";
        var out = new StringBuilder();
        for (long rest = id; rest > 0; rest /= 62) out.append(ALPHABET.charAt((int) (rest % 62)));
        return out.reverse().toString();
    }

    static long decode(String code) {
        if (code == null || code.isEmpty()) throw new IllegalArgumentException("empty code");
        long value = 0;
        try {
            for (int i = 0; i < code.length(); i++) {
                int digit = ALPHABET.indexOf(code.charAt(i));
                if (digit < 0) throw new IllegalArgumentException("not a base62 digit: " + code.charAt(i));
                value = Math.addExact(Math.multiplyExact(value, 62L), digit);
            }
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("code too large for a long: " + code, e);
        }
        return value;
    }

    static long capacity(int length) {
        if (length < 1) throw new IllegalArgumentException("length must be at least 1: " + length);
        long total = 1;
        try {
            for (int i = 0; i < length; i++) total = Math.multiplyExact(total, 62L);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("62^" + length + " does not fit in a long", e);
        }
        return total;
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("small ids are one digit", () -> {
            eq(encode(0), "0");
            eq(encode(9), "9");
            eq(encode(10), "a");
            eq(encode(36), "A");
            eq(encode(61), "Z");
        });
        test("larger ids carry into more digits", () -> {
            eq(encode(62), "10");
            eq(encode(3843), "ZZ");
            eq(encode(3844), "100");
            eq(encode(Long.MAX_VALUE), "aZl8N0y58M7");
        });
        test("decode reverses encode", () -> {
            for (long id : new long[] {0, 1, 61, 62, 125, 999_999_937L, 3_521_614_606_207L, Long.MAX_VALUE}) {
                eq(decode(encode(id)), id);
            }
        });
        test("decode is case-sensitive and allows leading zeros", () -> {
            eq(decode("a"), 10);
            eq(decode("A"), 36);
            eq(decode("0a"), 10);
            eq(decode("000"), 0);
        });
        test("refuses negative ids", () ->
            raises(IllegalArgumentException.class, () -> encode(-1)));
        test("refuses empty codes and foreign characters", () -> {
            raises(IllegalArgumentException.class, () -> decode(""));
            raises(IllegalArgumentException.class, () -> decode("ab-c"));
            raises(IllegalArgumentException.class, () -> decode("abc def"));
        });
        test("refuses a code too big for a long instead of wrapping", () -> {
            raises(IllegalArgumentException.class, () -> decode("aZl8N0y58M8"));
            raises(IllegalArgumentException.class, () -> decode("zzzzzzzzzzzz"));
            raises(IllegalArgumentException.class, () -> decode("w0000000000"));
        });
        test("capacity is 62 to the power of the length", () -> {
            eq(capacity(1), 62);
            eq(capacity(2), 3844);
            eq(capacity(7), 3_521_614_606_208L);
            eq(capacity(10), 839_299_365_868_340_224L);
        });
        test("capacity refuses lengths that are empty or overflow", () -> {
            raises(IllegalArgumentException.class, () -> capacity(0));
            raises(IllegalArgumentException.class, () -> capacity(11));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
