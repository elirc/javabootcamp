// ─────────────────────────────────────────────────────────────────────────
//  02 · builder: an HTTP request                            ★☆☆ warm-up
//  concepts: builder pattern · immutability · defensive copies · validation
//  run: java -cp ../../_lib/out 02-builder-request.java
// ─────────────────────────────────────────────────────────────────────────
//
//  An API client builds immutable HttpRequest records with a Builder:
//    request(url)        defaults: GET, no headers, no body, 30 s timeout
//    method(m)           stored upper-case; not GET, POST, PUT, PATCH,
//                        DELETE or HEAD → IllegalArgumentException
//    header(name, v)     name lower-cased (Locale.ROOT); a repeated name
//                        appends its value, in call order
//    body(b)             the body text
//    timeout(d)          zero or negative → IllegalArgumentException now
//    build()             IllegalStateException if the url does not start
//                        with http:// or https://, or a GET or HEAD has a
//                        body ("GET cannot have a body"). The request is
//                        fully immutable, header lists too, and later
//                        builder calls never change it
//    toBuilder()         a Builder with a copy of the request, to vary it
//
//      request(u).header("Accept","a").header("ACCEPT","b") → {accept=[a, b]}
//
//  hint: copying the map is not enough when its values are lists.

import static bootcamp.Check.*;

import java.time.Duration;
import java.util.*;

class Exercise {

    static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD");

    record HttpRequest(String method, String url, Map<String, List<String>> headers,
                       Optional<String> body, Duration timeout) {

        Builder toBuilder() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static Builder request(String url) {
        throw new UnsupportedOperationException("TODO");
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
            throw new UnsupportedOperationException("TODO");
        }

        Builder header(String name, String value) {
            throw new UnsupportedOperationException("TODO");
        }

        Builder body(String b) {
            throw new UnsupportedOperationException("TODO");
        }

        Builder timeout(Duration d) {
            throw new UnsupportedOperationException("TODO");
        }

        HttpRequest build() {
            throw new UnsupportedOperationException("TODO");
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
