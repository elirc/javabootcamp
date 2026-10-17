// ─────────────────────────────────────────────────────────────────────────
//  02 · builder: an HTTP request — SOLUTION                 ★☆☆ warm-up
//  concepts: builder pattern · immutability · defensive copies · validation
//  run: java -cp ../../_lib/out 02-builder-request.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A BUILDER separates "collecting the parts" from "having a valid
//  object". The record HttpRequest is immutable and always valid; the
//  Builder is mutable, accepts its parts in any order, has sensible
//  defaults, and checks the whole thing once in build(). Cross-field
//  rules (a GET has no body) can only live there, because no single
//  setter sees both fields. Rules about one value (a positive timeout)
//  fail at once, at the line that caused them.
//  Immutability is where builders usually leak. Map.copyOf(headers) copies
//  the map but not the lists inside it, so the built request and the
//  builder still share the same ArrayLists: a later header(...) call on
//  the builder would change a request that was already sent. Copy each
//  list (List.copyOf) inside a copied map, and make the map unmodifiable
//  too. A TreeMap keeps header names sorted, so two equal requests also
//  print the same.
//  toBuilder() is the other half: to vary an immutable object you copy it
//  into a fresh builder (deep copy again), change what differs and build
//  a new one. The original never changes. This is how Lombok's
//  @Builder(toBuilder = true) and the JDK's HttpRequest.newBuilder(...)
//  work.

import static bootcamp.Check.*;

import java.time.Duration;
import java.util.*;

class Exercise {

    static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD");

    record HttpRequest(String method, String url, Map<String, List<String>> headers,
                       Optional<String> body, Duration timeout) {

        Builder toBuilder() {
            var b = new Builder(url).method(method).timeout(timeout);
            headers.forEach((name, values) -> values.forEach(v -> b.header(name, v)));
            body.ifPresent(b::body);
            return b;
        }
    }

    static Builder request(String url) {
        return new Builder(url);
    }

    static final class Builder {
        private final String url;
        private String method = "GET";
        private final Map<String, List<String>> headers = new TreeMap<>();
        private String body;
        private Duration timeout = Duration.ofSeconds(30);

        Builder(String url) {
            this.url = url;
        }

        Builder method(String m) {
            var upper = m.toUpperCase(Locale.ROOT);
            if (!METHODS.contains(upper)) throw new IllegalArgumentException("unknown method " + m);
            method = upper;
            return this;
        }

        Builder header(String name, String value) {
            headers.computeIfAbsent(name.toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(value);
            return this;
        }

        Builder body(String b) {
            body = b;
            return this;
        }

        Builder timeout(Duration d) {
            if (d.isZero() || d.isNegative()) throw new IllegalArgumentException("timeout must be positive: " + d);
            timeout = d;
            return this;
        }

        HttpRequest build() {
            if (!url.startsWith("http://") && !url.startsWith("https://")) throw new IllegalStateException("not an http url: " + url);
            if (body != null && (method.equals("GET") || method.equals("HEAD"))) throw new IllegalStateException(method + " cannot have a body");
            var copy = new TreeMap<String, List<String>>();
            headers.forEach((name, values) -> copy.put(name, List.copyOf(values)));
            return new HttpRequest(method, url, Collections.unmodifiableMap(copy), Optional.ofNullable(body), timeout);
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("defaults: GET, no headers, no body, a 30 second timeout", () -> {
            var r = request("https://api.example.org/orders").build();
            eq(r.method(), "GET");
            eq(r.headers(), Map.of());
            eq(r.body(), Optional.empty());
            eq(r.timeout(), Duration.ofSeconds(30));
        });
        test("method is upper-cased and checked", () -> {
            eq(request("http://x.io").method("post").body("{}").build().method(), "POST");
            raises(IllegalArgumentException.class, () -> request("http://x.io").method("FETCH"));
        });
        test("header names are case-insensitive and values build up in order", () -> {
            var r = request("https://x.io").header("Accept", "a").header("ACCEPT", "b").header("X-Id", "7").build();
            eq(r.headers(), Map.of("accept", List.of("a", "b"), "x-id", List.of("7")));
        });
        test("build refuses a body on GET or HEAD, and a url that is not http", () -> {
            var e = raises(IllegalStateException.class, () -> request("https://x.io").body("{}").build());
            eq(e.getMessage(), "GET cannot have a body");
            raises(IllegalStateException.class, () -> request("https://x.io").method("HEAD").body("x").build());
            raises(IllegalStateException.class, () -> request("ftp://x.io").build());
            eq(request("https://x.io").method("DELETE").body("why").build().body(), Optional.of("why"));
        });
        test("a timeout that is not positive fails at once", () -> {
            raises(IllegalArgumentException.class, () -> request("https://x.io").timeout(Duration.ZERO));
            raises(IllegalArgumentException.class, () -> request("https://x.io").timeout(Duration.ofSeconds(-1)));
            eq(request("https://x.io").timeout(Duration.ofMillis(250)).build().timeout(), Duration.ofMillis(250));
        });
        test("a built request is immutable, header lists included", () -> {
            var r = request("https://x.io").header("accept", "a").build();
            raises(UnsupportedOperationException.class, () -> r.headers().put("x", List.of()));
            raises(UnsupportedOperationException.class, () -> r.headers().get("accept").add("b"));
        });
        test("changing the builder after build never changes the built request", () -> {
            var b = request("https://x.io").header("accept", "a");
            var first = b.build();
            b.header("accept", "b").header("x-new", "1");
            eq(first.headers(), Map.of("accept", List.of("a")));
            eq(b.build().headers().get("accept"), List.of("a", "b"));
        });
        test("toBuilder copies a request so it can be varied", () -> {
            var get = request("https://x.io/items").header("accept", "json").timeout(Duration.ofSeconds(5)).build();
            var post = get.toBuilder().method("POST").header("accept", "xml").body("{}").build();
            eq(post.headers(), Map.of("accept", List.of("json", "xml")));
            eq(post.timeout(), Duration.ofSeconds(5));
            eq(post.url(), "https://x.io/items");
            eq(get.headers(), Map.of("accept", List.of("json")));
            eq(get.method(), "GET");
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
