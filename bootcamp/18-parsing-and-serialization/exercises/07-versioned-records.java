// ─────────────────────────────────────────────────────────────────────────
//  07 · migrate versioned records                           ★★☆ core
//  concepts: schema versions · migrations · split limits · round trips
//  run: java -cp ../../_lib/out 07-versioned-records.java
// ─────────────────────────────────────────────────────────────────────────
//
//  The user file has lived through three formats, and old lines never get
//  rewritten. Fields are ";"-separated; the first field is the version:
//    v1  "1;fullName;email"
//    v2  "2;fullName;email;optIn"         optIn is "Y" or "N"
//    v3  "3;first;last;email;optIn"
//  decode(line) returns a v3 User by migrating step by step: v1 → v2
//  sets optIn N; v2 → v3 splits fullName at the FIRST space (first =
//  before it, last = everything after; no space: last is ""). Empty
//  fields are legal, trailing ones too ("1;Prince;" has email "").
//  encode(user) always writes v3; upgrade(line) = encode(decode(line)).
//  Errors (IllegalArgumentException): "unknown version 'x'";
//  "version N expects M fields, got K" (counting the version field);
//  "bad opt-in 'x'"; encode of a field holding ";" or a line break.
//
//      decode("1;Ada Lovelace;ada@x") → User[Ada, Lovelace, ada@x, false]
//      upgrade("2;Cher;c@x;Y")        → "3;Cher;;c@x;Y"
//
//  hint: what does "a;b;;".split(";") return? Read split's javadoc.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record User(String first, String last, String email, boolean optIn) {}

    static User decode(String line) {
        throw new UnsupportedOperationException("TODO");
    }

    static String encode(User user) {
        throw new UnsupportedOperationException("TODO");
    }

    static String upgrade(String line) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("decodes the current version", () -> {
            eq(decode("3;Ada;Lovelace;ada@x;Y"), new User("Ada", "Lovelace", "ada@x", true));
            eq(decode("3;Alan;Turing;at@x;N"), new User("Alan", "Turing", "at@x", false));
        });
        test("migrates v1: opt-in defaults to N and the name is split", () -> {
            eq(decode("1;Ada Lovelace;ada@x"), new User("Ada", "Lovelace", "ada@x", false));
        });
        test("migrates v2, splitting the name at the first space only", () -> {
            eq(decode("2;Grace Brewster Hopper;g@x;Y"), new User("Grace", "Brewster Hopper", "g@x", true));
            eq(decode("2; Lead;l@x;N"), new User("", "Lead", "l@x", false));
        });
        test("keeps empty fields, trailing ones included", () -> {
            eq(decode("2;Cher;c@x;N"), new User("Cher", "", "c@x", false));
            eq(decode("1;Prince;"), new User("Prince", "", "", false));
            eq(decode("3;;;;Y"), new User("", "", "", true));
        });
        test("encode writes the latest version and upgrade rewrites old lines", () -> {
            eq(encode(new User("Ada", "Lovelace", "ada@x", true)), "3;Ada;Lovelace;ada@x;Y");
            eq(upgrade("1;Ada Lovelace;ada@x"), "3;Ada;Lovelace;ada@x;N");
            eq(upgrade("2;Cher;c@x;Y"), "3;Cher;;c@x;Y");
            eq(upgrade("3;A;B;c;N"), "3;A;B;c;N");
        });
        test("decode(encode(u)) gives u back", () -> {
            for (var u : List.of(new User("Ada", "Lovelace", "a@x", true), new User("", "", "", false),
                    new User("Mary Ann", "Evans", "e@x", true), new User("x", "", "", true))) {
                eq(decode(encode(u)), u);
            }
        });
        test("refuses unknown versions and wrong field counts", () -> {
            raises(IllegalArgumentException.class, () -> decode("9;a;b"), "unknown version '9'");
            raises(IllegalArgumentException.class, () -> decode("Ada;x"), "unknown version 'Ada'");
            raises(IllegalArgumentException.class, () -> decode("2;Ada;a@x"), "version 2 expects 4 fields, got 3");
            raises(IllegalArgumentException.class, () -> decode("1;A;b;"), "version 1 expects 3 fields, got 4");
            raises(IllegalArgumentException.class, () -> decode("3;A;B;c;N;"), "version 3 expects 5 fields, got 6");
        });
        test("refuses a bad opt-in, and encode refuses separators in fields", () -> {
            raises(IllegalArgumentException.class, () -> decode("2;Ada;a@x;yes"), "bad opt-in 'yes'");
            raises(IllegalArgumentException.class, () -> decode("3;A;B;c;"), "bad opt-in ''");
            raises(IllegalArgumentException.class, () -> encode(new User("A;B", "C", "d", true)));
            raises(IllegalArgumentException.class, () -> encode(new User("A", "C", "d\ne", true)));
            raises(IllegalArgumentException.class, () -> encode(new User("A", "C\r", "d", true)));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
