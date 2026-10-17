// ─────────────────────────────────────────────────────────────────────────
//  08 · check round trips with properties — SOLUTION        ★★★ stretch
//  concepts: escaping · property tests · seeded generators · shrinking
//  run: java -cp ../../_lib/out 08-round-trip-properties.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Every serializer has one law: decode(encode(x)) == x. Example tests
//  check the cases you thought of; a PROPERTY test checks the law on
//  hundreds of generated inputs, including the ones you did not think
//  of. This is what jqwik and QuickCheck do, in thirty lines.
//  The codec itself: TERMINATE each item instead of separating them, so
//  [] ("") and [""] (";") are different texts, and escape the two special
//  chars, backslash first in the reader's eyes: "\" always starts a
//  two-char escape. The join codec in the tests shows the classic bug:
//  [] and [""] both become "", and a ";" inside an item splits it.
//  The generator uses a SEEDED Random and a tiny alphabet that is mostly
//  special chars, so bugs show up in a few trials and a failure can be
//  replayed exactly from its seed. Shrinking is what makes the result
//  readable: a random failure like ["b;a", "\\a", ";"] is greedily cut
//  down (drop an item, else drop a char, keep any cut that still fails)
//  until no single cut fails. That minimal case ([] or [";"]) names the
//  bug by itself. Wrong turns: shrinking a copy you then throw away,
//  restarting the generator instead of the shrink loop, and treating an
//  exception from the codec as a pass instead of a failure.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

class Exercise {

    static String encode(List<String> items) {
        var sb = new StringBuilder();
        for (var item : items) {
            for (int i = 0; i < item.length(); i++) {
                char c = item.charAt(i);
                if (c == '\\' || c == ';') sb.append('\\');
                sb.append(c);
            }
            sb.append(';');
        }
        return sb.toString();
    }

    static List<String> decode(String text) {
        var items = new ArrayList<String>();
        var item = new StringBuilder();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                if (i + 1 == text.length()) throw new IllegalArgumentException("dangling escape at " + i);
                char next = text.charAt(i + 1);
                if (next != '\\' && next != ';') throw new IllegalArgumentException("bad escape at " + i);
                item.append(next);
                i++;
            } else if (c == ';') {
                items.add(item.toString());
                item.setLength(0);
                start = i + 1;
            } else {
                item.append(c);
            }
        }
        if (start < text.length()) throw new IllegalArgumentException("unterminated item at " + start);
        return items;
    }

    static final String ALPHABET = "a;\\b";

    static Optional<List<String>> findCounterexample(Function<List<String>, String> enc,
            Function<String, List<String>> dec, long seed, int trials) {
        var rnd = new Random(seed);
        for (int t = 0; t < trials; t++) {
            var x = new ArrayList<String>();
            for (int i = rnd.nextInt(5); i > 0; i--) {
                var sb = new StringBuilder();
                for (int j = rnd.nextInt(4); j > 0; j--) sb.append(ALPHABET.charAt(rnd.nextInt(4)));
                x.add(sb.toString());
            }
            if (fails(x, enc, dec)) return Optional.of(shrink(x, enc, dec));
        }
        return Optional.empty();
    }

    static boolean fails(List<String> x, Function<List<String>, String> enc, Function<String, List<String>> dec) {
        try {
            return !x.equals(dec.apply(enc.apply(x)));
        } catch (RuntimeException e) {
            return true;
        }
    }

    static List<String> shrink(List<String> x, Function<List<String>, String> enc, Function<String, List<String>> dec) {
        var current = List.copyOf(x);
        boolean progress = true;
        while (progress) {
            progress = false;
            for (var candidate : candidates(current)) {
                if (fails(candidate, enc, dec)) {
                    current = candidate;
                    progress = true;
                    break;
                }
            }
        }
        return current;
    }

    static List<List<String>> candidates(List<String> x) {
        var out = new ArrayList<List<String>>();
        for (int i = 0; i < x.size(); i++) {
            var c = new ArrayList<>(x);
            c.remove(i);
            out.add(List.copyOf(c));
        }
        for (int i = 0; i < x.size(); i++) {
            var item = x.get(i);
            for (int j = 0; j < item.length(); j++) {
                var c = new ArrayList<>(x);
                c.set(i, item.substring(0, j) + item.substring(j + 1));
                out.add(List.copyOf(c));
            }
        }
        return out;
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
