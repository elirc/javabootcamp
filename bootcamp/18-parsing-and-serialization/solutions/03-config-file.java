// ─────────────────────────────────────────────────────────────────────────
//  03 · load a config file — SOLUTION                       ★★☆ core
//  concepts: key=value · typed schema · error collection · line numbers
//  run: java -cp ../../_lib/out 03-config-file.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Parsing that stops at the first error is fine for a compiler run in a
//  loop, but a config file is fixed by a tired operator at 3 a.m.: give
//  them every problem at once. So nothing here throws; each check appends
//  "line N: ..." to a list and moves on, and the good entries are kept.
//  The line number is the index + 1 of the RAW line, counted before blank
//  lines and comments are skipped, so it matches what an editor shows.
//  Split at the FIRST "=" (indexOf, not split("=")): values like URLs
//  contain "=" too. A map from key to the line where it first appeared
//  gives the duplicate message its "(first on line M)" and also answers
//  "did this key appear?" for the required-key check, which is why a key
//  with a bad value is not reported missing as well.
//  Java traps: Integer.parseInt throws NumberFormatException (catch it,
//  do not let one bad line kill the load); Boolean.parseBoolean never
//  throws and turns "yes" into false, so compare with "true"/"false"
//  yourself; and Set.of has no order, so sort the missing keys.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    enum Type { INT, BOOL, STRING }

    record Config(Map<String, Object> values, List<String> errors) {}

    static Config load(List<String> lines, Map<String, Type> schema, Set<String> required) {
        var values = new HashMap<String, Object>();
        var errors = new ArrayList<String>();
        var firstLine = new HashMap<String, Integer>();
        for (int i = 0; i < lines.size(); i++) {
            int n = i + 1;
            var line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            var key = eq < 0 ? "" : line.substring(0, eq).trim();
            if (key.isEmpty()) {
                errors.add("line " + n + ": expected key=value");
                continue;
            }
            var raw = line.substring(eq + 1).trim();
            var type = schema.get(key);
            if (type == null) {
                errors.add("line " + n + ": unknown key '" + key + "'");
                continue;
            }
            var first = firstLine.putIfAbsent(key, n);
            if (first != null) {
                errors.add("line " + n + ": duplicate key '" + key + "' (first on line " + first + ")");
                continue;
            }
            switch (type) {
                case INT -> {
                    try {
                        values.put(key, Integer.parseInt(raw));
                    } catch (NumberFormatException e) {
                        errors.add("line " + n + ": '" + key + "' must be an integer, got '" + raw + "'");
                    }
                }
                case BOOL -> {
                    if (raw.equals("true") || raw.equals("false")) values.put(key, raw.equals("true"));
                    else errors.add("line " + n + ": '" + key + "' must be true or false, got '" + raw + "'");
                }
                case STRING -> values.put(key,
                    raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"") ? raw.substring(1, raw.length() - 1) : raw);
            }
        }
        required.stream().filter(k -> !firstLine.containsKey(k)).sorted()
            .forEach(k -> errors.add("missing required key '" + k + "'"));
        return new Config(values, errors);
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
