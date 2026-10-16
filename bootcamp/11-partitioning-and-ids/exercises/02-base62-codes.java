// ─────────────────────────────────────────────────────────────────────────
//  02 · short links in base62                               ★☆☆ warm-up
//  concepts: base conversion · URL shorteners · overflow · Math.multiplyExact
//  run: java -cp ../../_lib/out 02-base62-codes.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A URL shortener stores each link under a numeric id and shows it as a
//  base62 code with the digits of ALPHABET (0-9, then a-z, then A-Z, so
//  'a' is 10 and 'A' is 36). encode(id) writes id >= 0 in base62 with no
//  leading zeros (0 is "0"); a negative id → IllegalArgumentException.
//  decode(code) reverses it (leading zeros allowed, case matters); an
//  empty code, a character outside ALPHABET, or a value too big for a long
//  → IllegalArgumentException. capacity(length) is how many distinct codes
//  of exactly that many characters exist, counting leading zeros (62 to
//  the power length); length < 1 or a result that overflows a long →
//  IllegalArgumentException.
//
//      encode(61)      → "Z"            encode(62)     → "10"
//      decode("A")     → 36             decode("0a")   → 10
//      encode(Long.MAX_VALUE)           → "aZl8N0y58M7"
//      decode("aZl8N0y58M8")            → IllegalArgumentException
//      capacity(7)     → 3521614606208  (3.5 trillion links)
//
//  hint: Math.multiplyExact and Math.addExact throw instead of wrapping.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    static String encode(long id) {
        throw new UnsupportedOperationException("TODO");
    }

    static long decode(String code) {
        throw new UnsupportedOperationException("TODO");
    }

    static long capacity(int length) {
        throw new UnsupportedOperationException("TODO");
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
