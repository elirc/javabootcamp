// ─────────────────────────────────────────────────────────────────────────
//  07 · a type-safe heterogeneous map                       ★★★ stretch
//  concepts: typed keys · Class<T>.cast · type tokens · raw types
//  run: java -cp ../../_lib/out 07-typesafe-container.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A request context holds values of many types, each under a typed key,
//  so ctx.require(PORT) is an Integer with no cast at the call site.
//  Finish Key's compact constructor (a primitive type such as int.class
//  throws IllegalArgumentException) and Context:
//
//    put(key, v)        store v; a null v REMOVES the key; a value that is
//                       not really a T (smuggled in through a raw Key)
//                       throws ClassCastException and stores nothing
//    get(key)           the value, or empty
//    getOrDefault(k, f) the value, or f
//    require(key)       the value, or NoSuchElementException whose
//                       message contains the key's name
//    names()            "name:SimpleTypeName" per stored key, sorted
//
//  Two keys are equal when name AND type match, so ("timeout", Integer)
//  and ("timeout", String) are different keys.
//
//      put(PORT, 8080); require(PORT)  → 8080
//      names()                         → [host:String, port:Integer]
//
//  hint: a Class<T> object can check a value as well as name a type.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Key<T>(String name, Class<T> type) {
        Key {
            throw new UnsupportedOperationException("TODO");
        }

        static <T> Key<T> of(String name, Class<T> type) {
            return new Key<>(name, type);
        }
    }

    static final class Context {
        private final Map<Key<?>, Object> values = new HashMap<>();

        <T> void put(Key<T> key, T value) {
            throw new UnsupportedOperationException("TODO");
        }

        <T> Optional<T> get(Key<T> key) {
            throw new UnsupportedOperationException("TODO");
        }

        <T> T getOrDefault(Key<T> key, T fallback) {
            throw new UnsupportedOperationException("TODO");
        }

        <T> T require(Key<T> key) {
            throw new UnsupportedOperationException("TODO");
        }

        List<String> names() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("stores values of different types and returns them typed", () -> {
            var port = Key.of("port", Integer.class);
            var host = Key.of("host", String.class);
            var ctx = new Context();
            ctx.put(port, 8080);
            ctx.put(host, "example.org");
            int p = ctx.require(port);
            String h = ctx.require(host);
            eq(p, 8080);
            eq(h, "example.org");
        });
        test("a missing key gives empty, or the fallback", () -> {
            var ctx = new Context();
            var retries = Key.of("retries", Integer.class);
            eq(ctx.get(retries), Optional.empty());
            eq(ctx.getOrDefault(retries, 3), 3);
            ctx.put(retries, 5);
            eq(ctx.getOrDefault(retries, 3), 5);
        });
        test("keys with one name but different types are different keys", () -> {
            var ctx = new Context();
            var asNumber = Key.of("timeout", Integer.class);
            var asText = Key.of("timeout", String.class);
            ctx.put(asNumber, 30);
            ctx.put(asText, "30s");
            eq(ctx.get(asNumber), Optional.of(30));
            eq(ctx.get(asText), Optional.of("30s"));
            eq(ctx.get(Key.of("timeout", Integer.class)), Optional.of(30));
        });
        test("require on a missing key throws NoSuchElementException naming it", () -> {
            var ctx = new Context();
            var e = raises(NoSuchElementException.class, () -> ctx.require(Key.of("tenant", String.class)));
            ok(e.getMessage().contains("tenant"), "message: " + e.getMessage());
        });
        test("a wrong-typed value smuggled through a raw key is refused and not stored", () -> {
            var ctx = new Context();
            var port = Key.of("port", Integer.class);
            @SuppressWarnings("rawtypes") Key raw = port;
            raises(ClassCastException.class, () -> ctx.put(raw, "not a number"));
            eq(ctx.get(port), Optional.empty());
            eq(ctx.names(), List.of());
        });
        test("putting null removes the key", () -> {
            var ctx = new Context();
            var user = Key.of("user", String.class);
            ctx.put(user, "ana");
            ctx.put(user, null);
            eq(ctx.get(user), Optional.empty());
            eq(ctx.names(), List.of());
        });
        test("Key rejects primitive types such as int.class", () -> {
            raises(IllegalArgumentException.class, () -> Key.of("port", int.class));
            eq(Key.of("port", Integer.class).name(), "port");
        });
        test("names lists name:Type for every stored key, sorted", () -> {
            var ctx = new Context();
            ctx.put(Key.of("timeout", Integer.class), 30);
            ctx.put(Key.of("host", String.class), "h");
            ctx.put(Key.of("timeout", String.class), "30s");
            eq(ctx.names(), List.of("host:String", "timeout:Integer", "timeout:String"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
