// ─────────────────────────────────────────────────────────────────────────
//  07 · a type-safe heterogeneous map — SOLUTION            ★★★ stretch
//  concepts: typed keys · Class<T>.cast · type tokens · raw types
//  run: java -cp ../../_lib/out 07-typesafe-container.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  This is Bloch's "typesafe heterogeneous container" (Effective Java
//  item 33). The map itself is Map<Key<?>, Object>, so the compiler
//  cannot help inside it; the type safety lives in the KEY. A Key<T>
//  carries a Class<T> type token, and every read goes through
//  key.type().cast(value), which returns a T without an unchecked cast.
//  The same cast on WRITE is what makes the container honest. Generics
//  are erased at runtime, so a raw Key lets a caller put a String under
//  an Integer key with only a compiler warning. Without the check the
//  bad value sits in the map and blows up later, far from the bug, with
//  a ClassCastException at some innocent call site. Casting in put fails
//  at the line that caused it. Collections.checkedList does the same.
//  Primitive keys are refused because int.class.cast(8080) throws: the
//  value is boxed to Integer, and Integer is not an int. The type token
//  must be the wrapper class.
//  Records give Key a correct equals/hashCode for free, and because the
//  type takes part in equality, one name can hold two typed values.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Key<T>(String name, Class<T> type) {
        Key {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(type, "type");
            if (type.isPrimitive()) throw new IllegalArgumentException("use the wrapper class, not " + type);
        }

        static <T> Key<T> of(String name, Class<T> type) {
            return new Key<>(name, type);
        }
    }

    static final class Context {
        private final Map<Key<?>, Object> values = new HashMap<>();

        <T> void put(Key<T> key, T value) {
            if (value == null) {
                values.remove(key);
                return;
            }
            values.put(key, key.type().cast(value));
        }

        <T> Optional<T> get(Key<T> key) {
            return Optional.ofNullable(key.type().cast(values.get(key)));
        }

        <T> T getOrDefault(Key<T> key, T fallback) {
            return get(key).orElse(fallback);
        }

        <T> T require(Key<T> key) {
            return get(key).orElseThrow(() -> new NoSuchElementException("missing: " + key.name()));
        }

        List<String> names() {
            return values.keySet().stream()
                .map(k -> k.name() + ":" + k.type().getSimpleName())
                .sorted()
                .toList();
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
