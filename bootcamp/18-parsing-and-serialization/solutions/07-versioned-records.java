// ─────────────────────────────────────────────────────────────────────────
//  07 · migrate versioned records — SOLUTION                ★★☆ core
//  concepts: schema versions · migrations · split limits · round trips
//  run: java -cp ../../_lib/out 07-versioned-records.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Stored data outlives the code that wrote it, so every record carries
//  its version and the reader migrates OLD to NEW one step at a time:
//  v1 → v2 → v3. Each step is small and knows only its neighbour, so
//  adding v4 later means one more step, not a rewrite of every reader.
//  The writer only ever writes the latest version; "upgrade" is just
//  decode then encode, which is how a background job rewrites old rows.
//  The Java trap is String.split: "1;Prince;".split(";") drops trailing
//  empty strings and returns 2 fields, so a legal record with an empty
//  email looks malformed (or worse, a short record looks fine). The
//  limit -1 keeps every field. Count fields per version BEFORE indexing.
//  The migration trap is splitting the name with split(" "): "Grace
//  Brewster Hopper" would lose a part. indexOf(' ') splits once, and the
//  "no space" case (Cher) needs an explicit last name of "".
//  Finally, the writer guards the format: a ";" inside a field would
//  shift every later field when read back, so encode refuses it, and the
//  round-trip test decode(encode(u)) == u pins the whole contract.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record User(String first, String last, String email, boolean optIn) {}

    static User decode(String line) {
        var f = line.split(";", -1);
        var version = f[0];
        int expected = switch (version) {
            case "1" -> 3;
            case "2" -> 4;
            case "3" -> 5;
            default -> throw new IllegalArgumentException("unknown version '" + version + "'");
        };
        if (f.length != expected) {
            throw new IllegalArgumentException("version " + version + " expects " + expected + " fields, got " + f.length);
        }
        if (version.equals("1")) f = new String[] {"2", f[1], f[2], "N"};
        if (f[0].equals("2")) {
            int space = f[1].indexOf(' ');
            var first = space < 0 ? f[1] : f[1].substring(0, space);
            var last = space < 0 ? "" : f[1].substring(space + 1);
            f = new String[] {"3", first, last, f[2], f[3]};
        }
        return new User(f[1], f[2], f[3], optIn(f[4]));
    }

    static boolean optIn(String s) {
        return switch (s) {
            case "Y" -> true;
            case "N" -> false;
            default -> throw new IllegalArgumentException("bad opt-in '" + s + "'");
        };
    }

    static String encode(User user) {
        for (var field : List.of(user.first(), user.last(), user.email())) {
            if (field.contains(";") || field.contains("\n") || field.contains("\r")) {
                throw new IllegalArgumentException("a field cannot contain ';' or a line break: " + field);
            }
        }
        return String.join(";", "3", user.first(), user.last(), user.email(), user.optIn() ? "Y" : "N");
    }

    static String upgrade(String line) {
        return encode(decode(line));
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
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
