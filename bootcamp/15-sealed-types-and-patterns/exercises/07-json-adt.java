// ─────────────────────────────────────────────────────────────────────────
//  07 · a JSON value type                                   ★★★ stretch
//  concepts: algebraic data types · enum singleton · escaping · merge patch
//  run: java -cp ../../_lib/out 07-json-adt.java
// ─────────────────────────────────────────────────────────────────────────
//
//  JSON as a sealed type: JNull (an enum singleton), JBool, JNum, JStr,
//  JArr and JObj (fields keep insertion order). Finish:
//
//    JNum(v)     NaN or an infinity → IllegalArgumentException
//    render(j)   compact JSON. A whole number below 1e15 in size prints as
//                a long ("3"; -0.0 as "0"), others by Double.toString
//                ("1.0E20"). In strings escape " and \ with a backslash,
//                newline as \n, tab as \t, other chars below 0x20 as a
//                backslash, "u" and 4 lowercase hex digits ("u0001")
//    at(j, path) follow a dot path ("orders.1.sku"): a field name in a
//                JObj, a 0-based index in a JArr; else empty; "" → the root
//    merge(base, patch)  JSON Merge Patch: a non-object patch replaces
//                base; an object patch applies field by field, recursing;
//                a JNull value removes the field; new fields go last
//
//      merge({"a":1,"b":2}, {"b":3,"a":null,"c":4})  → {"b":3,"c":4}
//
//  hint: the recursion in merge is on the field's old value and new value.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    sealed interface Json permits JNull, JBool, JNum, JStr, JArr, JObj {}

    enum JNull implements Json { INSTANCE }

    record JBool(boolean value) implements Json {}

    record JNum(double value) implements Json {
        JNum {
            throw new UnsupportedOperationException("TODO");
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
        throw new UnsupportedOperationException("TODO");
    }

    static Optional<Json> at(Json root, String path) {
        throw new UnsupportedOperationException("TODO");
    }

    static Json merge(Json base, Json patch) {
        throw new UnsupportedOperationException("TODO");
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
