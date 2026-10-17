# 18 · Parsing and Serialization

Every system boundary is a parser. The HTTP request, the CSV export from
finance, the config file, the message on a queue, the row written by
last year's version of your code: each arrives as text or bytes and has
to become data you can trust, and each has to be written back so the
next reader gets exactly what you meant. Most production bugs at
boundaries are not exotic. A comma in a name splits a field, a trailing
empty field disappears, a quote is not escaped, a line arrives in two
TCP reads, or an old record has one field fewer than the reader expects.
This module builds the small parsers and writers you will otherwise
reach for `split` to fake: a lexer, a quoted-CSV reader, a validating
config loader, a line-protocol framer, a JSON writer and reader, a
versioned record format, and a property test that proves a codec round
trips.

## The mental model

### 1 · Lex, then parse, and always know where you are

A parser reads with ONE position that only moves forward. Splitting the
work in two keeps both halves simple: the **lexer** turns characters into
tokens (and throws away spaces), and the **parser** turns tokens into
structure. Whoever finds a problem knows the offset, so the error can say
*where*:

```
tokenize("x <= 3.5")   → IDENT x @0 · OP <= @2 · NUMBER 3.5 @5
parse("[1 2]")         → "unexpected '2' at 3"
                                       ^ the reader can find this
```

Two habits make lexers correct. **Maximal munch**: always take the
longest token (`<=` is one operator, not `<` then `=`). And **decide by
looking at one char**: a digit starts a number, a `"` starts a string,
a `{` starts an object. Recursive descent is the same idea for nested
data: one method per grammar rule, each calling the others.

### 2 · Quoting is a state machine, not a split

`line.split(",")` is correct until the first customer called
"Smith, J". Real formats have a quoting rule, and the parser has to know
at every char whether it is inside quotes:

```
 7,"Smith, J","say ""hi"""
 │ └─ quoted: commas are data, "" is one "
 └─ unquoted: runs to the next comma
```

The same applies on the way out. A writer that builds JSON with string
concatenation produces broken output for the first name containing a
quote, and a security hole for the first one containing `","admin":true`.
**Escape on write, unescape on read, and do both in one place.**

### 3 · Framing: where does one message end?

TCP and files give you a stream of bytes, not messages. One read can
hold half a message or three of them. Protocols pick a framing rule:

| framing | how a message ends | seen in |
| --- | --- | --- |
| delimiter | a line end (`\n` or `\r\n`) | SMTP, Redis RESP, IRC, NDJSON logs |
| length prefix | the header says how many bytes follow | HTTP `Content-Length`, Kafka, gRPC |
| self-describing | the parser knows when the value is complete | JSON in a stream, CBOR |

A decoder keeps the unfinished frame BETWEEN reads, and it needs a
maximum frame length, or a client that never sends `\n` makes you buffer
forever (Netty's `LineBasedFrameDecoder` takes `maxLength` for exactly
this reason).

### 4 · Data outlives code: version everything you store

The bytes written today will be read by code written next year. Put a
version in every record, have the writer write only the latest version,
and have the reader **migrate step by step** (v1 → v2 → v3). Each step
knows only its neighbour, so v4 is one more step, not a rewrite.

```
"1;Ada Lovelace;ada@x"   ─v1→v2─►  "2;Ada Lovelace;ada@x;N"  ─v2→v3─►  User(Ada, Lovelace, ada@x, false)
```

Protobuf, Avro and database migrations are this idea with tooling.

### 5 · The one law: decode(encode(x)) == x

Every codec has a round-trip law, and **property-based testing** checks
it on hundreds of generated inputs instead of the five you thought of. A
seeded generator makes every failure replayable; **shrinking** cuts a
random failure down to the smallest input that still fails, which usually
names the bug by itself (`[]` encodes like `[""]`; `[";"]` is not
escaped).

## The details that bite

1. **`"a;b;;".split(";")` returns `[a, b]`.** `split` drops trailing
   empty strings. Use `split(";", -1)` whenever empty fields are legal
   (exercise 07).
2. **Split at the FIRST separator** when the value may contain it:
   `url=http://h/?a=b` has two `=` (`indexOf`, not `split`) (exercise
   03).
3. **Line numbers count every line**, blank lines and comments too, or
   they do not match the operator's editor (exercise 03).
4. **`Boolean.parseBoolean("yes")` is `false` and never throws.**
   Validate booleans yourself (exercise 03).
5. **A `"` after a closing quote is malformed, not data.** Accepting
   `"ab"c` hides broken exports (exercise 02).
6. **Never split a CSV into lines first**: a quoted field may contain a
   newline (exercise 02).
7. **Strip the `\r` of a CRLF only when the `\n` arrives**; it may come
   in the previous read (exercise 04).
8. **Check the frame length as you buffer**, not when the line ends, or
   memory is unbounded (exercise 04).
9. **Every char below U+0020 must be escaped in a JSON string**, not
   just `\n` and `\t` (exercise 05).
10. **`instanceof Number` lets `Double` in**: dispatch on the exact types
    you support and refuse the rest (exercise 05).
11. **After the top-level value only whitespace may follow**, or `{} {}`
    parses (exercise 06).
12. **`Long.parseLong` overflow is a `NumberFormatException`**; turn it
    into an error with a position (exercise 06).
13. **Terminate, don't separate**: with separators `[]` and `[""]` are
    the same text (exercise 08).
14. **A codec that throws is failing**, not passing, in a property test
    (exercise 08).
15. **Java parses `\u` escapes even in comments.** A comment containing
    a backslash, `u` and then non-hex characters is a compile error.
    Write "backslash-u" in prose.

## Cheat table

| task | reach for | not |
| --- | --- | --- |
| split keeping empty fields | `s.split(";", -1)` | `s.split(";")` |
| split once | `int i = s.indexOf('='); s.substring(0, i)` | `s.split("=")[1]` |
| parse an int, collecting errors | `try { Integer.parseInt(v) } catch (NumberFormatException e)` | letting it escape |
| build output | one `StringBuilder` passed down | `+` in a recursive method |
| four hex digits | `String.format("\\u%04x", (int) c)` | `Integer.toHexString(c)` (no padding) |
| ordered map from parsing | `LinkedHashMap` | `HashMap` (order lost), `Map.of` (no nulls) |
| reproducible random input | `new Random(seed)` | `new Random()`, `Math.random()` |

## Exercises

| # | file | ★ | what you build |
| --- | --- | --- | --- |
| 01 | `01-tokenizer.java` | ★☆☆ | a lexer with maximal munch and error offsets |
| 02 | `02-csv-records.java` | ★★☆ | an RFC 4180 CSV reader: quotes, doubled quotes, newlines in fields |
| 03 | `03-config-file.java` | ★★☆ | a typed key=value config loader that reports every error with its line |
| 04 | `04-line-framing.java` | ★★☆ | a line-protocol decoder over partial reads, CRLF and a max frame length |
| 05 | `05-json-writer.java` | ★★☆ | a JSON writer for a fixed subset with correct escaping |
| 06 | `06-json-reader.java` | ★★★ | a recursive-descent JSON reader with exact error positions |
| 07 | `07-versioned-records.java` | ★★☆ | a versioned record format with step-by-step migration |
| 08 | `08-round-trip-properties.java` | ★★★ | an escaping codec plus a seeded property checker that shrinks failures |

Do the warm-ups and core in order. Stretch if time allows. 05 and 06 are
a pair: write JSON, then read it back. 06 and 01 are the same technique
at two sizes. If you did module 06 (`05-csv-store`), compare its naive
CSV with exercise 02; if you did module 12, exercise 04 is how the bytes
of a message become a message, and exercise 07 is how a consumer reads
an event written by an older producer.

---

**Next:** `bootcamp/19-debugging`
