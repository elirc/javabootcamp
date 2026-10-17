// ─────────────────────────────────────────────────────────────────────────
//  07 · a JSON value type — SOLUTION                        ★★★ stretch
//  concepts: algebraic data types · enum singleton · escaping · merge patch
//  run: java -cp ../../_lib/out 07-json-adt.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  JSON is a textbook sum type: a value is exactly one of six cases, so a
//  sealed interface of five records and one enum models it with nothing
//  left over. JNull is an enum constant because there is only one null;
//  an enum can implement a sealed interface like any class can.
//  render is a recursive walk with one branch per case. The bugs are in
//  the leaves. Double.toString(3.0) is "3.0", which some consumers parse
//  as a float, so whole numbers print as longs (only below 1e15, where a
//  double still holds every integer exactly). Strings need escaping of
//  the quote, the backslash and every control character, or the output
//  is not JSON. This is why you never build JSON by concatenation.
//  at returns Optional because a path is data from outside: a missing
//  field, a bad index and the wrong container type are all "not there",
//  not exceptions. Check the index bound before get, or an
//  IndexOutOfBoundsException escapes.
//  merge is RFC 7386, the format of HTTP PATCH with
//  application/merge-patch+json. Recursing into nested objects is the
//  point: patching {"user":{"city":"Lima"}} must keep the user's name.
//  A LinkedHashMap copy of the base keeps field order stable.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    sealed interface Json permits JNull, JBool, JNum, JStr, JArr, JObj {}

    enum JNull implements Json { INSTANCE }

    record JBool(boolean value) implements Json {}

    record JNum(double value) implements Json {
        JNum {
            if (!Double.isFinite(value)) throw new IllegalArgumentException("JSON has no " + value);
        }
    }

    record JStr(String value) implements Json {}

    record JArr(List<Json> items) implements Json {
        JArr { items = List.copyOf(items); }
    }

    record JObj(Map<String, Json> fields) implements Json {
        JObj { fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields)); }
    }

    static String render(Json j) {
        var sb = new StringBuilder();
        write(j, sb);
        return sb.toString();
    }

    private static void write(Json j, StringBuilder sb) {
        if (j instanceof JNull) sb.append("null");
        else if (j instanceof JBool b) sb.append(b.value());
        else if (j instanceof JNum n) sb.append(number(n.value()));
        else if (j instanceof JStr s) quote(s.value(), sb);
        else if (j instanceof JArr a) {
            sb.append('[');
            for (int i = 0; i < a.items().size(); i++) {
                if (i > 0) sb.append(',');
                write(a.items().get(i), sb);
            }
            sb.append(']');
        } else if (j instanceof JObj o) {
            sb.append('{');
            var first = true;
            for (var e : o.fields().entrySet()) {
                if (!first) sb.append(',');
                first = false;
                quote(e.getKey(), sb);
                sb.append(':');
                write(e.getValue(), sb);
            }
            sb.append('}');
        } else {
            throw new AssertionError("unhandled " + j);
        }
    }

    private static String number(double v) {
        return v == Math.rint(v) && Math.abs(v) < 1e15 ? Long.toString((long) v) : Double.toString(v);
    }

    private static void quote(String s, StringBuilder sb) {
        sb.append('"');
        for (char c : s.toCharArray()) {
            if (c == '"') sb.append("\\\"");
            else if (c == '\\') sb.append("\\\\");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\t') sb.append("\\t");
            else if (c < 0x20) sb.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
            else sb.append(c);
        }
        sb.append('"');
    }

    static Optional<Json> at(Json root, String path) {
        if (path.isEmpty()) return Optional.of(root);
        Json current = root;
        for (var segment : path.split("\\.", -1)) {
            if (current instanceof JObj o && o.fields().containsKey(segment)) {
                current = o.fields().get(segment);
            } else if (current instanceof JArr a && segment.matches("\\d{1,9}") && Integer.parseInt(segment) < a.items().size()) {
                current = a.items().get(Integer.parseInt(segment));
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(current);
    }

    static Json merge(Json base, Json patch) {
        if (!(patch instanceof JObj p)) return patch;
        var result = base instanceof JObj b ? new LinkedHashMap<>(b.fields()) : new LinkedHashMap<String, Json>();
        for (var e : p.fields().entrySet()) {
            if (e.getValue() == JNull.INSTANCE) result.remove(e.getKey());
            else result.put(e.getKey(), merge(result.getOrDefault(e.getKey(), JNull.INSTANCE), e.getValue()));
        }
        return new JObj(result);
    }

    // helpers for the tests: obj("k1", v1, "k2", v2, ...)
    static JObj obj(Object... kv) {
        var m = new LinkedHashMap<String, Json>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], (Json) kv[i + 1]);
        return new JObj(m);
    }

    static JArr arr(Json... items) { return new JArr(List.of(items)); }
    static JStr str(String s) { return new JStr(s); }
    static JNum num(double d) { return new JNum(d); }
    static final JNull NULL = JNull.INSTANCE;

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("render writes null, booleans, and whole or fractional numbers", () -> {
            eq(render(NULL), "null");
            eq(render(new JBool(true)), "true");
            eq(render(num(3.0)), "3");
            eq(render(num(-0.0)), "0");
            eq(render(num(-2.5)), "-2.5");
            eq(render(num(1e20)), "1.0E20");
        });
        test("JNum rejects NaN and the infinities", () -> {
            raises(IllegalArgumentException.class, () -> num(Double.NaN));
            raises(IllegalArgumentException.class, () -> num(Double.POSITIVE_INFINITY));
            eq(num(0.5).value(), 0.5);
        });
        test("render escapes quotes, backslashes and control characters", () -> {
            eq(render(str("a\"b\\c\nd\te\u0001")), "\"a\\\"b\\\\c\\nd\\te\\u0001\"");
            eq(render(str("\r\u001f")), "\"\\u000d\\u001f\"");
            var accented = "caf" + (char) 0xE9;
            eq(render(str(accented)), "\"" + accented + "\"");
        });
        test("render keeps object fields in insertion order and nests arrays", () -> {
            var order = obj("id", num(7), "tags", arr(str("a"), str("b")), "ok", new JBool(false), "note", NULL);
            eq(render(order), "{\"id\":7,\"tags\":[\"a\",\"b\"],\"ok\":false,\"note\":null}");
            eq(render(obj("empty", arr(), "nested", obj())), "{\"empty\":[],\"nested\":{}}");
        });
        test("at follows a dot path through fields and array indexes", () -> {
            var doc = obj("customer", obj("name", str("Ana")), "orders", arr(obj("sku", str("A1")), obj("sku", str("B2"))));
            eq(at(doc, "orders.1.sku"), Optional.of(str("B2")));
            eq(at(doc, "customer.name"), Optional.of(str("Ana")));
            eq(at(doc, ""), Optional.of(doc));
        });
        test("at is empty for a missing field, a bad index or the wrong container", () -> {
            var doc = obj("customer", obj("name", str("Ana")), "orders", arr(obj("sku", str("A1"))));
            eq(at(doc, "orders.5"), Optional.empty());
            eq(at(doc, "orders.x"), Optional.empty());
            eq(at(doc, "orders.-1"), Optional.empty());
            eq(at(doc, "customer.0"), Optional.empty());
            eq(at(doc, "nope.deeper"), Optional.empty());
            eq(at(doc, "customer.name.first"), Optional.empty());
        });
        test("merge replaces, adds, and removes fields patched with null", () -> {
            var merged = merge(obj("a", num(1), "b", num(2)), obj("b", num(3), "a", NULL, "c", num(4)));
            eq(render(merged), "{\"b\":3,\"c\":4}");
            eq(merge(obj("a", num(1)), str("x")), str("x"));
        });
        test("merge recurses into objects and replaces anything else whole", () -> {
            var base = obj("user", obj("name", str("Ana"), "city", str("Oslo")), "tags", arr(str("x")));
            var patch = obj("user", obj("city", str("Lima")), "tags", arr(str("y"), str("z")));
            eq(render(merge(base, patch)), "{\"user\":{\"name\":\"Ana\",\"city\":\"Lima\"},\"tags\":[\"y\",\"z\"]}");
            eq(render(merge(num(1), obj("a", NULL, "b", obj("c", NULL)))), "{\"b\":{}}");
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
