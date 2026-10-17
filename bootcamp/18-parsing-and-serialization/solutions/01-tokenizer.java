// ─────────────────────────────────────────────────────────────────────────
//  01 · tokenize an expression — SOLUTION                   ★☆☆ warm-up
//  concepts: lexing · maximal munch · token positions · error offsets
//  run: java -cp ../../_lib/out 01-tokenizer.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A lexer is one index walking the string. The current char decides the
//  token kind: a digit starts a NUMBER, a letter or "_" an IDENT, an
//  operator char an OP. Each branch consumes as many chars as belong to
//  the token and records where it STARTED, so later errors can point at
//  the source ("line 3, col 7" in a real compiler is the same offset).
//  Operators use maximal munch: look one char ahead and take the
//  two-char operator if it exists. That is why "<=" is one token and
//  "a=<b" is "=", "<". "!" exists only as the start of "!=".
//  The classic wrong turn is accepting "3." or a second "." as part of a
//  number; then "1.2.3" lexes as one token and the parser downstream gets
//  garbage. A "." is only consumed when a digit follows it, and only
//  once per number; any other "." falls through to the error branch,
//  which reports the char and its offset.
//  Java detail: Character.isDigit accepts non-ASCII digits like '٣';
//  a real lexer compares with '0'..'9' as here.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    enum Kind { NUMBER, IDENT, OP }

    record Token(Kind kind, String text, int pos) {}

    static final Set<String> TWO_CHAR_OPS = Set.of("==", "<=", ">=", "!=");
    static final String ONE_CHAR_OPS = "+-*/()=<>";

    static List<Token> tokenize(String src) {
        var tokens = new ArrayList<Token>();
        int i = 0;
        while (i < src.length()) {
            char c = src.charAt(i);
            int start = i;
            if (c == ' ' || c == '\t' || c == '\n') {
                i++;
            } else if (isDigit(c)) {
                while (i < src.length() && isDigit(src.charAt(i))) i++;
                if (i + 1 < src.length() && src.charAt(i) == '.' && isDigit(src.charAt(i + 1))) {
                    i++;
                    while (i < src.length() && isDigit(src.charAt(i))) i++;
                }
                tokens.add(new Token(Kind.NUMBER, src.substring(start, i), start));
            } else if (Character.isLetter(c) || c == '_') {
                while (i < src.length() && (Character.isLetterOrDigit(src.charAt(i)) || src.charAt(i) == '_')) i++;
                tokens.add(new Token(Kind.IDENT, src.substring(start, i), start));
            } else if (i + 1 < src.length() && TWO_CHAR_OPS.contains(src.substring(i, i + 2))) {
                tokens.add(new Token(Kind.OP, src.substring(i, i + 2), start));
                i += 2;
            } else if (ONE_CHAR_OPS.indexOf(c) >= 0) {
                tokens.add(new Token(Kind.OP, String.valueOf(c), start));
                i++;
            } else {
                throw new IllegalArgumentException("unexpected '" + c + "' at " + i);
            }
        }
        return tokens;
    }

    static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("splits an assignment into tokens with their kinds", () -> {
            eq(tokenize("x=1+y2"), List.of(new Token(Kind.IDENT, "x", 0), new Token(Kind.OP, "=", 1),
                new Token(Kind.NUMBER, "1", 2), new Token(Kind.OP, "+", 3), new Token(Kind.IDENT, "y2", 4)));
        });
        test("records each token's 0-based start, skipping all whitespace", () -> {
            eq(tokenize("  ab\t*\n(c_1 )"), List.of(new Token(Kind.IDENT, "ab", 2), new Token(Kind.OP, "*", 5),
                new Token(Kind.OP, "(", 7), new Token(Kind.IDENT, "c_1", 8), new Token(Kind.OP, ")", 12)));
            eq(tokenize(""), List.of());
            eq(tokenize(" \n "), List.of());
        });
        test("reads a decimal as one number and splits 12ab in two", () -> {
            eq(tokenize("3.14*r"), List.of(new Token(Kind.NUMBER, "3.14", 0), new Token(Kind.OP, "*", 4),
                new Token(Kind.IDENT, "r", 5)));
            eq(tokenize("12ab"), List.of(new Token(Kind.NUMBER, "12", 0), new Token(Kind.IDENT, "ab", 2)));
            eq(tokenize("_9-007"), List.of(new Token(Kind.IDENT, "_9", 0), new Token(Kind.OP, "-", 2),
                new Token(Kind.NUMBER, "007", 3)));
        });
        test("takes the longest operator", () -> {
            eq(tokenize("a<=b"), List.of(new Token(Kind.IDENT, "a", 0), new Token(Kind.OP, "<=", 1),
                new Token(Kind.IDENT, "b", 3)));
            eq(tokenize("a==b!=c>=d").stream().map(Token::text).toList(),
                List.of("a", "==", "b", "!=", "c", ">=", "d"));
            eq(tokenize("a=<b>/=").stream().map(Token::text).toList(), List.of("a", "=", "<", "b", ">", "/", "="));
        });
        test("an unexpected character is reported with its offset", () -> {
            raises(IllegalArgumentException.class, () -> tokenize("a + $b"), "unexpected '$' at 4");
            raises(IllegalArgumentException.class, () -> tokenize("!x"), "unexpected '!' at 0");
            raises(IllegalArgumentException.class, () -> tokenize("ok;"), "unexpected ';' at 2");
        });
        test("a dot must be followed by a digit", () -> {
            raises(IllegalArgumentException.class, () -> tokenize("3."), "unexpected '.' at 1");
            raises(IllegalArgumentException.class, () -> tokenize("x 3.y"), "unexpected '.' at 3");
            raises(IllegalArgumentException.class, () -> tokenize(".5"), "unexpected '.' at 0");
        });
        test("a number has at most one decimal point", () -> {
            eq(tokenize("1.5").size(), 1);
            raises(IllegalArgumentException.class, () -> tokenize("1.2.3"), "unexpected '.' at 3");
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
