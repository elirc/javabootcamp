// ─────────────────────────────────────────────────────────────────────────
//  06 · read JSON — SOLUTION                                ★★★ stretch
//  concepts: recursive descent · lookahead · escapes · error positions
//  run: java -cp ../../_lib/out 06-json-reader.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Recursive descent mirrors the grammar: value() looks at ONE char and
//  dispatches to object(), array(), string(), number() or a literal;
//  object() and array() call value() for their elements, which is all
//  the recursion there is. Every method reads from one shared position,
//  so whoever detects a problem knows exactly where it is: an error
//  message "at 37" is worth more than any amount of "invalid JSON".
//  Two helpers keep the error rules in one place: peek() reports
//  "unexpected end" when the text has run out, and expect(c) reports
//  "unexpected 'x'" at the offending char.
//  The traps are at the edges. The top-level value must be followed by
//  nothing but whitespace, or "{} {}" and "1 x" parse as valid. A number
//  stops at the first non-digit, so "01" and "1.5" are caught by
//  whoever reads the NEXT char. Long.parseLong throws on overflow; turn
//  that into an error at the number's start instead of letting a
//  NumberFormatException escape. A repeated key is ambiguous (parsers
//  disagree on which one wins, which attackers exploit), so refuse it.
//  Java detail: Map.of refuses null values; a JSON null value needs a
//  LinkedHashMap, which also keeps the keys in input order.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static Object parse(String json) {
        var p = new Parser(json);
        p.skipWs();
        var value = p.value();
        p.skipWs();
        if (p.pos < json.length()) throw p.unexpected();
        return value;
    }

    static final class Parser {
        final String s;
        int pos;

        Parser(String s) {
            this.s = s;
        }

        IllegalArgumentException unexpected() {
            return pos >= s.length()
                ? new IllegalArgumentException("unexpected end at " + pos)
                : new IllegalArgumentException("unexpected '" + s.charAt(pos) + "' at " + pos);
        }

        char peek() {
            if (pos >= s.length()) throw unexpected();
            return s.charAt(pos);
        }

        void expect(char c) {
            if (peek() != c) throw unexpected();
            pos++;
        }

        void skipWs() {
            while (pos < s.length() && " \t\n\r".indexOf(s.charAt(pos)) >= 0) pos++;
        }

        Object value() {
            char c = peek();
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return string();
            if (c == '-' || (c >= '0' && c <= '9')) return number();
            if (c == 't') return literal("true", true);
            if (c == 'f') return literal("false", false);
            if (c == 'n') return literal("null", null);
            throw unexpected();
        }

        Map<String, Object> object() {
            var map = new LinkedHashMap<String, Object>();
            expect('{');
            skipWs();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWs();
                int at = pos;
                if (peek() != '"') throw unexpected();
                var key = string();
                if (map.containsKey(key)) throw new IllegalArgumentException("duplicate key \"" + key + "\" at " + at);
                skipWs();
                expect(':');
                skipWs();
                map.put(key, value());
                skipWs();
                if (peek() == '}') {
                    pos++;
                    return map;
                }
                expect(',');
            }
        }

        List<Object> array() {
            var list = new ArrayList<Object>();
            expect('[');
            skipWs();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                skipWs();
                list.add(value());
                skipWs();
                if (peek() == ']') {
                    pos++;
                    return list;
                }
                expect(',');
            }
        }

        String string() {
            expect('"');
            var sb = new StringBuilder();
            while (true) {
                char c = peek();
                if (c == '"') {
                    pos++;
                    return sb.toString();
                }
                if (c < 0x20) throw unexpected();
                pos++;
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                char e = peek();
                switch (e) {
                    case '"', '\\', '/' -> sb.append(e);
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        int code = 0;
                        for (int k = 0; k < 4; k++) {
                            pos++;
                            int d = "0123456789abcdef".indexOf(Character.toLowerCase(peek()));
                            if (d < 0) throw unexpected();
                            code = code * 16 + d;
                        }
                        sb.append((char) code);
                    }
                    default -> throw unexpected();
                }
                pos++;
            }
        }

        Long number() {
            int start = pos;
            if (peek() == '-') pos++;
            char c = peek();
            if (c < '0' || c > '9') throw unexpected();
            if (c == '0') {
                pos++;
            } else {
                while (pos < s.length() && s.charAt(pos) >= '0' && s.charAt(pos) <= '9') pos++;
            }
            try {
                return Long.parseLong(s.substring(start, pos));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("number out of range at " + start);
            }
        }

        Object literal(String word, Object result) {
            for (int k = 0; k < word.length(); k++) {
                if (peek() != word.charAt(k)) throw unexpected();
                pos++;
            }
            return result;
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("reads the scalars", () -> {
            eq(parse("null"), null);
            eq(parse(" true"), true);
            eq(parse("false \n"), false);
            ok(parse("42") instanceof Long, "a number is a Long");
            eq(parse("-7"), -7L);
            eq(parse("\"hi\""), "hi");
        });
        test("reads nested arrays and objects, keeping key order", () -> {
            var v = parse("{ \"z\" : [1, 2, {\"b\":null}],\r\n\t\"a\":\"x\", \"e\": {}, \"l\": [] }");
            eq(v, Map.of("z", List.of(1, 2, Collections.singletonMap("b", null)), "a", "x", "e", Map.of(), "l", List.of()));
            eq(new ArrayList<>(((Map<?, ?>) v).keySet()), List.of("z", "a", "e", "l"));
        });
        test("decodes every escape", () -> {
            eq(parse("\"a\\\"b\\\\c\\/d\\n\\r\\t\\u00e9\\u20AC\""), "a\"b\\c/d\n\r\t\u00e9\u20ac");
        });
        test("numbers: no leading zeros, no fractions, and within long", () -> {
            eq(parse("0"), 0L);
            eq(parse("-9223372036854775808"), Long.MIN_VALUE);
            raises(IllegalArgumentException.class, () -> parse("01"), "unexpected '1' at 1");
            raises(IllegalArgumentException.class, () -> parse("[1.5]"), "unexpected '.' at 2");
            raises(IllegalArgumentException.class, () -> parse(" 9223372036854775808"), "number out of range at 1");
            raises(IllegalArgumentException.class, () -> parse("-a"), "unexpected 'a' at 1");
            raises(IllegalArgumentException.class, () -> parse("+1"), "unexpected '+' at 0");
        });
        test("an unexpected char is reported at its offset", () -> {
            raises(IllegalArgumentException.class, () -> parse("[1 2]"), "unexpected '2' at 3");
            raises(IllegalArgumentException.class, () -> parse("{\"a\" 1}"), "unexpected '1' at 5");
            raises(IllegalArgumentException.class, () -> parse("[1,]"), "unexpected ']' at 3");
            raises(IllegalArgumentException.class, () -> parse("{1:2}"), "unexpected '1' at 1");
            raises(IllegalArgumentException.class, () -> parse("[trux]"), "unexpected 'x' at 4");
        });
        test("running out of text is reported at its end", () -> {
            raises(IllegalArgumentException.class, () -> parse(""), "unexpected end at 0");
            raises(IllegalArgumentException.class, () -> parse("[1,"), "unexpected end at 3");
            raises(IllegalArgumentException.class, () -> parse("\"abc"), "unexpected end at 4");
            raises(IllegalArgumentException.class, () -> parse("{\"a\":1"), "unexpected end at 6");
            raises(IllegalArgumentException.class, () -> parse("-"), "unexpected end at 1");
            raises(IllegalArgumentException.class, () -> parse("nul"), "unexpected end at 3");
        });
        test("refuses trailing text, raw control chars and bad escapes", () -> {
            raises(IllegalArgumentException.class, () -> parse("1 x"), "unexpected 'x' at 2");
            raises(IllegalArgumentException.class, () -> parse("{} {}"), "unexpected '{' at 3");
            raises(IllegalArgumentException.class, () -> parse("\"a\tb\""), "at 2");
            raises(IllegalArgumentException.class, () -> parse("\"\\q\""), "unexpected 'q' at 2");
            raises(IllegalArgumentException.class, () -> parse("\"\\u12G4\""), "unexpected 'G' at 5");
        });
        test("refuses a repeated key, pointing at its opening quote", () -> {
            raises(IllegalArgumentException.class, () -> parse("{\"a\":1,\"a\":2}"), "duplicate key \"a\" at 7");
            eq(parse("{\"a\":{\"a\":1}}"), Map.of("a", Map.of("a", 1)));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
