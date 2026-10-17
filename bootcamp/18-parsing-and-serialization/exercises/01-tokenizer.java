// ─────────────────────────────────────────────────────────────────────────
//  01 · tokenize an expression                              ★☆☆ warm-up
//  concepts: lexing · maximal munch · token positions · error offsets
//  run: java -cp ../../_lib/out 01-tokenizer.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Every parser starts with a lexer: it turns characters into tokens so
//  the parser never thinks about spaces again. Write tokenize(src):
//    NUMBER  digits, optionally "." and at least one more digit ("3.14")
//    IDENT   a letter or "_", then letters, digits or "_"
//    OP      + - * / ( ) = < >  and the two-char  == <= >= !=
//  Spaces, tabs and newlines separate tokens and are skipped. pos is the
//  0-based offset of the token's first char. Take the LONGEST operator
//  ("<=" is one token). Any other char, a lone "!", a "." that does not
//  sit between digits of ONE number: IllegalArgumentException "unexpected
//  'c' at N" (N is that char's offset). "12ab" is a NUMBER then an IDENT.
//
//      tokenize("x <= 3.5") → [IDENT "x" @0, OP "<=" @2, NUMBER "3.5" @5]
//      tokenize("a + $b")   → IllegalArgumentException "unexpected '$' at 4"
//
//  hint: one index, one loop; decide the token kind from the current char.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    enum Kind { NUMBER, IDENT, OP }

    record Token(Kind kind, String text, int pos) {}

    static List<Token> tokenize(String src) {
        throw new UnsupportedOperationException("TODO");
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
