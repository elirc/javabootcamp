// ─────────────────────────────────────────────────────────────────────────
//  03 · load a config file                                  ★★☆ core
//  concepts: key=value · typed schema · error collection · line numbers
//  run: java -cp ../../_lib/out 03-config-file.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A service refuses to start on a bad config, and the operator wants
//  EVERY mistake in one go, each with its line number (N is 1-based).
//  Write load(lines, schema, required) → Config(values, errors):
//  - trim each line; skip blank lines and lines starting with "#";
//  - split at the FIRST "="; trim key and value. No "=" or an empty key:
//    "line N: expected key=value";
//  - key not in schema: "line N: unknown key 'k'"; a key seen before:
//    "line N: duplicate key 'k' (first on line M)";
//  - INT (Integer.parseInt): "line N: 'k' must be an integer, got 'v'";
//    BOOL, exactly true/false: "line N: 'k' must be true or false, got 'v'";
//    STRING: the value; if it is wrapped in "...", drop those two quotes;
//  - then each required key that NEVER appeared, sorted:
//    "missing required key 'k'". A key with a bad value did appear.
//  values holds only the good entries; errors are in line order.
//
//      ["port = 80", "debug=yes"] → values {port=80}, errors
//          ["line 2: 'debug' must be true or false, got 'yes'"]
//
//  hint: one pass over the lines, a map from key to its first line number.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    enum Type { INT, BOOL, STRING }

    record Config(Map<String, Object> values, List<String> errors) {}

    static Config load(List<String> lines, Map<String, Type> schema, Set<String> required) {
        throw new UnsupportedOperationException("TODO");
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        var schema = Map.of("port", Type.INT, "debug", Type.BOOL, "name", Type.STRING, "url", Type.STRING);
        test("loads typed values", () -> {
            var c = load(List.of("port=8080", "debug=true", "name=api"), schema, Set.of());
            eq(c.values(), Map.of("port", 8080, "debug", true, "name", "api"));
            eq(c.errors(), List.of());
            ok(c.values().get("port") instanceof Integer, "port is an Integer");
        });
        test("skips blanks and comments and trims key and value", () -> {
            var c = load(List.of("  # the port", "", "   ", "  port =  -80  ", "debug= false"), schema, Set.of());
            eq(c.values(), Map.of("port", -80, "debug", false));
            eq(c.errors(), List.of());
        });
        test("splits at the first = and unwraps one pair of quotes", () -> {
            var c = load(List.of("url=http://h/?a=b", "name=\"  x # y \""), schema, Set.of());
            eq(c.values(), Map.of("url", "http://h/?a=b", "name", "  x # y "));
            eq(load(List.of("name=", "url=a # b"), schema, Set.of()).values(), Map.of("name", "", "url", "a # b"));
            eq(load(List.of("name=\"\"x\"\"", "url=\"half"), schema, Set.of()).values(),
                Map.of("name", "\"x\"", "url", "\"half"));
        });
        test("a line without a key and an = is reported with its number", () -> {
            eq(load(List.of("# c", "just text", "=5", " = "), schema, Set.of()).errors(), List.of(
                "line 2: expected key=value", "line 3: expected key=value", "line 4: expected key=value"));
        });
        test("reports unknown keys and badly typed values", () -> {
            var c = load(List.of("prot=80", "port=80x", "debug=True", "port=99999999999"), schema, Set.of());
            eq(c.errors(), List.of("line 1: unknown key 'prot'", "line 2: 'port' must be an integer, got '80x'",
                "line 3: 'debug' must be true or false, got 'True'",
                "line 4: duplicate key 'port' (first on line 2)"));
            eq(c.values(), Map.of());
        });
        test("a duplicate names the line where the key first appeared", () -> {
            var c = load(List.of("name=a", "", "port=1", "name=b", "name=c"), schema, Set.of());
            eq(c.errors(), List.of("line 4: duplicate key 'name' (first on line 1)",
                "line 5: duplicate key 'name' (first on line 1)"));
            eq(c.values(), Map.of("name", "a", "port", 1));
        });
        test("lists missing required keys after the line errors, sorted", () -> {
            var c = load(List.of("debug=maybe"), schema, Set.of("url", "port", "debug"));
            eq(c.errors(), List.of("line 1: 'debug' must be true or false, got 'maybe'",
                "missing required key 'port'", "missing required key 'url'"));
            eq(load(List.of(), schema, Set.of("name")).errors(), List.of("missing required key 'name'"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
