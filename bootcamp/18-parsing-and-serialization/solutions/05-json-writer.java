// ─────────────────────────────────────────────────────────────────────────
//  05 · write JSON — SOLUTION                               ★★☆ core
//  concepts: serialization · string escaping · recursion · type dispatch
//  run: java -cp ../../_lib/out 05-json-writer.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A serializer is a recursive walk with one case per supported type,
//  appending to one shared StringBuilder (building strings per level and
//  concatenating them is quadratic for deep data). Containers recurse;
//  scalars print; the final else refuses, so an unexpected type deep in
//  a structure fails loudly instead of being written as its toString()
//  ("java.lang.Object@1b6d3586" is valid inside quotes and still wrong).
//  Escaping is where hand-written JSON breaks. A string must escape the
//  quote and the backslash (or the string ends early), the common
//  controls get their short forms, and every other char below U+0020 is
//  illegal raw inside a JSON string, so it gets the 6-char escape
//  (backslash, "u", 4 hex digits). Non-ASCII text is legal as-is.
//  Map keys are strings in JSON, so they go through the same escaping;
//  a non-String key is refused rather than silently turned into "1".
//  Order: JSON objects are unordered, but writing in the map's own
//  iteration order keeps output stable for a LinkedHashMap or TreeMap.
//  Java detail: test Integer and Long explicitly; a check like
//  "instanceof Number" would let Double through and print 1.0E10.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static String toJson(Object value) {
        var sb = new StringBuilder();
        write(value, sb);
        return sb.toString();
    }

    static void write(Object value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
            sb.append(value);
        } else if (value instanceof String s) {
            writeString(s, sb);
        } else if (value instanceof List<?> list) {
            sb.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(',');
                write(list.get(i), sb);
            }
            sb.append(']');
        } else if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (var e : map.entrySet()) {
                if (!(e.getKey() instanceof String key)) throw unsupported(e.getKey());
                if (!first) sb.append(',');
                first = false;
                writeString(key, sb);
                sb.append(':');
                write(e.getValue(), sb);
            }
            sb.append('}');
        } else {
            throw unsupported(value);
        }
    }

    static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    static IllegalArgumentException unsupported(Object value) {
        return new IllegalArgumentException("unsupported type: " + (value == null ? "null" : value.getClass().getSimpleName()));
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("writes the scalars", () -> {
            eq(toJson(null), "null");
            eq(toJson(true), "true");
            eq(toJson(42), "42");
            eq(toJson(-7_000_000_000L), "-7000000000");
            eq(toJson("hi"), "\"hi\"");
        });
        test("escapes quotes, backslashes, newlines, CRs and tabs", () -> {
            eq(toJson("say \"hi\""), "\"say \\\"hi\\\"\"");
            eq(toJson("C:\\tmp"), "\"C:\\\\tmp\"");
            eq(toJson("a\nb\rc\td"), "\"a\\nb\\rc\\td\"");
        });
        test("writes other control chars as 4-hex-digit escapes and keeps the rest", () -> {
            eq(toJson("\u0001x\u001f"), "\"\\u0001x\\u001f\"");
            eq(toJson("caf\u00e9 \u20ac /"), "\"caf\u00e9 \u20ac /\"");
        });
        test("writes lists, nested and empty, with no spaces", () -> {
            eq(toJson(List.of(1, List.of(), "a", List.of(false))), "[1,[],\"a\",[false]]");
            eq(toJson(Arrays.asList(1, null)), "[1,null]");
        });
        test("writes maps in their own iteration order", () -> {
            var m = new LinkedHashMap<String, Object>();
            m.put("b", 1);
            m.put("a", Map.of());
            m.put("c", null);
            eq(toJson(m), "{\"b\":1,\"a\":{},\"c\":null}");
            eq(toJson(new TreeMap<>(Map.of("y", "2", "x", List.of()))), "{\"x\":[],\"y\":\"2\"}");
        });
        test("escapes map keys like any string", () -> {
            eq(toJson(Map.of("a\"b", 1)), "{\"a\\\"b\":1}");
        });
        test("refuses unsupported types at any depth, naming the type", () -> {
            raises(IllegalArgumentException.class, () -> toJson(1.5), "unsupported type: Double");
            raises(IllegalArgumentException.class, () -> toJson(List.of(1, (short) 2)), "unsupported type: Short");
            raises(IllegalArgumentException.class, () -> toJson(Map.of("k", List.of(new Object()))), "unsupported type: Object");
            raises(IllegalArgumentException.class, () -> toJson(Map.of(1, 2)), "unsupported type: Integer");
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
