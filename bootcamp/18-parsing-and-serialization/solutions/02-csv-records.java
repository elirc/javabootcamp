// ─────────────────────────────────────────────────────────────────────────
//  02 · parse quoted CSV — SOLUTION                         ★★☆ core
//  concepts: CSV (RFC 4180) · quoting · state machines · error offsets
//  run: java -cp ../../_lib/out 02-csv-records.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  CSV is not "split on commas"; it is a tiny state machine. At the start
//  of every field one char decides everything: a " means a quoted field,
//  anything else an unquoted one. A quoted field is read char by char to
//  the closing quote, turning "" into one " and treating commas and
//  newlines as plain data; an unquoted field runs to the next comma or
//  line end. After the field the next char is the separator: "," starts
//  another field, a line end closes the record, the end of text closes
//  everything.
//  Two wrong turns. Splitting the text into lines FIRST breaks every
//  quoted field that contains a newline (addresses, comments), so the
//  record split has to happen inside the same loop. And forgetting the
//  check after a closing quote silently accepts "ab"c as abc; RFC 4180
//  says it is malformed, and a parser that guesses hides bad exports.
//  Errors carry the offset where things went wrong (for an unterminated
//  quote the OPENING quote, because that is where the reader must look).
//  Edge cases worth a test of their own: "" (no records), "\n" (one empty
//  field), a trailing comma (one more empty field).

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static List<List<String>> parse(String text) {
        var records = new ArrayList<List<String>>();
        if (text.isEmpty()) return records;
        int n = text.length();
        int i = 0;
        var record = new ArrayList<String>();
        var field = new StringBuilder();
        while (true) {
            if (i < n && text.charAt(i) == '"') {
                int open = i++;
                while (true) {
                    if (i >= n) throw new IllegalArgumentException("unterminated quote at " + open);
                    char c = text.charAt(i);
                    if (c != '"') {
                        field.append(c);
                        i++;
                    } else if (i + 1 < n && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i += 2;
                    } else {
                        i++;
                        break;
                    }
                }
                if (i < n && !atSeparator(text, i)) {
                    throw new IllegalArgumentException("unexpected '" + text.charAt(i) + "' at " + i);
                }
            } else {
                while (i < n && !atSeparator(text, i)) {
                    if (text.charAt(i) == '"') throw new IllegalArgumentException("unexpected '\"' at " + i);
                    field.append(text.charAt(i++));
                }
            }
            record.add(field.toString());
            field.setLength(0);
            if (i >= n) {
                records.add(record);
                return records;
            }
            if (text.charAt(i) == ',') {
                i++;
                continue;
            }
            i += text.charAt(i) == '\r' ? 2 : 1;
            records.add(record);
            record = new ArrayList<>();
            if (i >= n) return records;
        }
    }

    static boolean atSeparator(String text, int i) {
        char c = text.charAt(i);
        return c == ',' || c == '\n' || text.startsWith("\r\n", i);
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
