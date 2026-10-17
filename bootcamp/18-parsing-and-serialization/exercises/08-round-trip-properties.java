// ─────────────────────────────────────────────────────────────────────────
//  08 · check round trips with properties                   ★★★ stretch
//  concepts: escaping · property tests · seeded generators · shrinking
//  run: java -cp ../../_lib/out 08-round-trip-properties.java
// ─────────────────────────────────────────────────────────────────────────
//
//  1) A codec for a list of strings: encode writes each item with "\" →
//  "\\" and ";" → "\;", followed by ";" ([] → "", [""] → ";"). decode
//  inverts it; IllegalArgumentException, offsets 0-based: a "\" at the
//  very end: "dangling escape at N"; "\" + any other char: "bad escape
//  at N" (N = the "\"); text after the last ";": "unterminated item at
//  N" (N = where that item starts).
//  2) findCounterexample(enc, dec, seed, trials) hunts for an input
//  where dec(enc(x)) is not x (or either throws a RuntimeException).
//  Generate with new Random(seed): per trial size = nextInt(5), then per
//  item its length = nextInt(4), then per char one of the 4 chars a ;
//  backslash b, picked by nextInt(4) (in Java: "a;\\b"). On the
//  first failure, SHRINK: try removing each item (index ascending), then
//  each char of each item (item, then char, ascending); take the first
//  candidate that still fails and start over; stop when none fails.
//  Return the shrunk input, or empty if all trials pass.
//
//      encode(List.of("a", "b;c")) → "a;b\;c;"
//      findCounterexample(join-with-";" codec, ...) → Optional[[]]
//
//  hint: a failing case is only useful once it is small enough to read.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

class Exercise {

    static String encode(List<String> items) {
        throw new UnsupportedOperationException("TODO");
    }

    static List<String> decode(String text) {
        throw new UnsupportedOperationException("TODO");
    }

    static Optional<List<String>> findCounterexample(Function<List<String>, String> enc,
            Function<String, List<String>> dec, long seed, int trials) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("encode escapes and terminates every item", () -> {
            eq(encode(List.of("a", "b;c", "d\\")), "a;b\\;c;d\\\\;");
            eq(encode(List.of()), "");
            eq(encode(List.of("")), ";");
            eq(encode(List.of("", "")), ";;");
        });
        test("decode inverts encode, telling [] and [\"\"] apart", () -> {
            eq(decode("a;b\\;c;d\\\\;"), List.of("a", "b;c", "d\\"));
            eq(decode(""), List.of());
            eq(decode(";"), List.of(""));
            eq(decode("\\\\\\;;"), List.of("\\;"));
        });
        test("decode reports malformed text with its offset", () -> {
            raises(IllegalArgumentException.class, () -> decode("a\\"), "dangling escape at 1");
            raises(IllegalArgumentException.class, () -> decode("a\\x;"), "bad escape at 1");
            raises(IllegalArgumentException.class, () -> decode("a;bc"), "unterminated item at 2");
            raises(IllegalArgumentException.class, () -> decode("x;\\;"), "unterminated item at 2");
        });
        test("the codec survives 2000 random lists", () -> {
            var rnd = new Random(42);
            for (int t = 0; t < 2000; t++) {
                var x = new ArrayList<String>();
                for (int i = rnd.nextInt(6); i > 0; i--) {
                    var sb = new StringBuilder();
                    for (int j = rnd.nextInt(5); j > 0; j--) sb.append("ab;\\ ".charAt(rnd.nextInt(5)));
                    x.add(sb.toString());
                }
                eq(decode(encode(x)), x);
            }
        });
        test("finds nothing for a correct codec", () -> {
            var store = new ArrayList<List<String>>();
            Function<List<String>, String> enc = x -> {
                store.add(List.copyOf(x));
                return String.valueOf(store.size() - 1);
            };
            eq(findCounterexample(enc, s -> store.get(Integer.parseInt(s)), 7, 300), Optional.empty());
            ok(store.size() >= 300, "it ran every trial");
        });
        test("shrinks a join-with-; failure to the empty list", () -> {
            eq(findCounterexample(x -> String.join(";", x), s -> List.of(s.split(";", -1)), 1, 200),
                Optional.of(List.of()));
        });
        test("shrinks an unescaped codec's failure to [;]", () -> {
            Function<List<String>, String> enc = x -> x.stream().map(s -> s + ";").collect(Collectors.joining());
            Function<String, List<String>> dec = s -> s.isEmpty() ? List.of() : List.of(s.substring(0, s.length() - 1).split(";", -1));
            eq(findCounterexample(enc, dec, 3, 200), Optional.of(List.of(";")));
            eq(findCounterexample(enc, dec, 99, 200), Optional.of(List.of(";")));
        });
        test("a throwing codec fails too; shrinks chars as well as items; obeys trials", () -> {
            var store = new ArrayList<List<String>>();
            Function<List<String>, String> enc = x -> {
                store.add(List.copyOf(x));
                return String.valueOf(store.size() - 1);
            };
            Function<String, List<String>> dec = s -> {
                var x = store.get(Integer.parseInt(s));
                if (x.size() == 4) throw new IllegalStateException("boom");
                return x;
            };
            eq(findCounterexample(enc, dec, 5, 200), Optional.of(List.of("", "", "", "")));
            store.clear();
            eq(findCounterexample(enc, dec, 5, 0), Optional.empty());
            eq(store.size(), 0);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
