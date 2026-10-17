// ─────────────────────────────────────────────────────────────────────────
//  05 · write JSON                                          ★★☆ core
//  concepts: serialization · string escaping · recursion · type dispatch
//  run: java -cp ../../_lib/out 05-json-writer.java
// ─────────────────────────────────────────────────────────────────────────
//
//  "{\"name\":\"" + name + "\"}" works until a user is called Bob "The
//  Builder". Write toJson(value) for a fixed subset, compact (no spaces):
//    null → null    Boolean → true/false    Integer, Long → 42
//    String → "..." with escapes: " → \"   \ → \\   newline → \n
//             CR → \r   tab → \t   any other char below U+0020 → the
//             6-char escape: backslash, "u", 4 lowercase hex digits
//             (U+001F → backslash u001f); every other char as-is
//    List → [a,b]   Map with String keys → {"k":v} in the map's own
//             iteration order (do not sort)
//  Anything else (Double, Short, a non-String key, ...), at any depth:
//  IllegalArgumentException "unsupported type: <SimpleName>".
//
//      toJson(Map.of("ok", List.of(1, true)))  → {"ok":[1,true]}
//      toJson("say \"hi\"")                    → "say \"hi\""
//
//  hint: one recursive method; a StringBuilder passed down the recursion.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static String toJson(Object value) {
        throw new UnsupportedOperationException("TODO");
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
