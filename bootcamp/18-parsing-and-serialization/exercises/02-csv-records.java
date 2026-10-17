// ─────────────────────────────────────────────────────────────────────────
//  02 · parse quoted CSV                                    ★★☆ core
//  concepts: CSV (RFC 4180) · quoting · state machines · error offsets
//  run: java -cp ../../_lib/out 02-csv-records.java
// ─────────────────────────────────────────────────────────────────────────
//
//  line.split(",") breaks on the first customer called "Smith, J". Write
//  parse(text) → records → fields, the RFC 4180 way:
//  - fields are separated by ","; spaces are kept, nothing is trimmed;
//  - a field that STARTS with " is quoted: it runs to the closing quote,
//    "" inside it is one ", and commas and newlines in it are literal;
//    right after the closing quote must come ",", a line end, or the end;
//  - a record ends at "\n" or "\r\n" outside quotes; a final line end
//    adds no record; "" has no records; an empty line is one empty field.
//  Errors, IllegalArgumentException with a 0-based offset into text:
//    "unterminated quote at N"  (N = the opening quote)
//    "unexpected 'c' at N"      (after a closing quote, or a " inside an
//                                unquoted field)
//
//      parse("id,name\n7,\"Smith, J\"") → [[id, name], [7, Smith, J]]
//      parse("\"say \"\"hi\"\"\",x")    → [[say "hi", x]]
//
//  hint: at each field start, look at one char: quoted or not decides all.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static List<List<String>> parse(String text) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("splits a line on commas and keeps spaces", () -> {
            eq(parse("a,b,c"), List.of(List.of("a", "b", "c")));
            eq(parse(" a , b"), List.of(List.of(" a ", " b")));
        });
        test("keeps empty fields, including a trailing one", () -> {
            eq(parse("a,,b,"), List.of(List.of("a", "", "b", "")));
            eq(parse(","), List.of(List.of("", "")));
        });
        test("a quoted field keeps commas, spaces and newlines", () -> {
            eq(parse("7,\"Smith, J\""), List.of(List.of("7", "Smith, J")));
            eq(parse("\"two\nlines\",\" x \"\nnext"), List.of(List.of("two\nlines", " x "), List.of("next")));
            eq(parse("\"\",\"\""), List.of(List.of("", "")));
        });
        test("a doubled quote inside quotes is one quote", () -> {
            eq(parse("\"say \"\"hi\"\"\",x"), List.of(List.of("say \"hi\"", "x")));
            eq(parse("\"\"\"\""), List.of(List.of("\"")));
        });
        test("records end at LF or CRLF, and a final line end adds none", () -> {
            eq(parse("a,b\r\nc\n"), List.of(List.of("a", "b"), List.of("c")));
            eq(parse("x\n\ny"), List.of(List.of("x"), List.of(""), List.of("y")));
            eq(parse("\"q\"\r\n"), List.of(List.of("q")));
            eq(parse(""), List.of());
            eq(parse("\n"), List.of(List.of("")));
            eq(parse("a\rb,c"), List.of(List.of("a\rb", "c")));
        });
        test("an unterminated quote is reported at its opening offset", () -> {
            raises(IllegalArgumentException.class, () -> parse("a,\"bc"), "unterminated quote at 2");
            raises(IllegalArgumentException.class, () -> parse("x\n\"a\"\""), "unterminated quote at 2");
        });
        test("text after a closing quote is an error at that offset", () -> {
            raises(IllegalArgumentException.class, () -> parse("\"ab\"c,d"), "unexpected 'c' at 4");
            raises(IllegalArgumentException.class, () -> parse("x,\"a\" ,b"), "unexpected ' ' at 5");
        });
        test("a quote inside an unquoted field is an error", () -> {
            raises(IllegalArgumentException.class, () -> parse("ab\"c"), "unexpected '\"' at 2");
            raises(IllegalArgumentException.class, () -> parse("ok\n5\""), "unexpected '\"' at 4");
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
