// ─────────────────────────────────────────────────────────────────────────
//  03 · immutable snapshots, published — SOLUTION           ★☆☆ warm-up
//  concepts: immutability · defensive copies · AtomicReference · publication
//  run: java -cp ../../_lib/out 03-immutable-snapshots.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  The simplest thread-safe object is one that never changes. An
//  immutable Config can be read by any number of threads with no locks,
//  and a reader can never see half an update, because there are no
//  updates: a change builds a NEW Config and swaps the reference. Make
//  the class final, every field private final, and copy everything
//  mutable on the way in and out.
//  Defensive copying is where it goes wrong. Storing the caller's list
//  lets them change your "immutable" object later. List.copyOf and
//  Map.copyOf make unmodifiable copies, but a Map<String, List<String>>
//  copied with Map.copyOf still shares the inner lists: copy each value.
//  SAFE PUBLICATION is the second half. A plain field written by one
//  thread may never become visible to another. AtomicReference (like a
//  volatile field) guarantees that a reader that sees the new reference
//  also sees the fully built object behind it, and final fields add the
//  same guarantee for the object's contents.
//  update uses updateAndGet, a CAS retry loop: if two threads update at
//  once, one retries its function on the newer snapshot, so no update is
//  lost. That only works because the function is pure (withHost has no
//  side effects); a function that logs or charges money would run twice.
//  getAndUpdate returns the OLD snapshot, the classic slip.

import static bootcamp.Check.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

class Exercise {

    static final class Config {
        private final String name;
        private final List<String> hosts;
        private final Map<String, List<String>> routes;

        Config(String name, List<String> hosts, Map<String, List<String>> routes) {
            this.name = name;
            this.hosts = List.copyOf(hosts);
            var copy = new TreeMap<String, List<String>>();
            routes.forEach((path, targets) -> copy.put(path, List.copyOf(targets)));
            this.routes = Collections.unmodifiableMap(copy);
        }

        String name() {
            return name;
        }

        List<String> hosts() {
            return hosts;
        }

        Map<String, List<String>> routes() {
            return routes;
        }

        Config withHost(String host) {
            var more = new ArrayList<>(hosts);
            more.add(host);
            return new Config(name, more, routes);
        }

        Config withRoute(String path, List<String> targets) {
            var more = new TreeMap<>(routes);
            more.put(path, targets);
            return new Config(name, hosts, more);
        }
    }

    static final class ConfigHolder {
        private final AtomicReference<Config> current;

        ConfigHolder(Config initial) {
            this.current = new AtomicReference<>(initial);
        }

        Config current() {
            return current.get();
        }

        Config update(UnaryOperator<Config> change) {
            return current.updateAndGet(change);
        }
    }

    static Config sample() {
        return new Config("edge", List.of("h1"), Map.of("/api", List.of("svc-a")));
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("the constructor copies its inputs, so later changes do not leak in", () -> {
            var hosts = new ArrayList<>(List.of("h1"));
            var routes = new HashMap<String, List<String>>();
            routes.put("/api", new ArrayList<>(List.of("svc-a")));
            var c = new Config("edge", hosts, routes);
            hosts.add("evil");
            routes.put("/admin", List.of("svc-x"));
            eq(c.hosts(), List.of("h1"));
            eq(c.routes().keySet(), Set.of("/api"));
        });
        test("a route's list is copied too", () -> {
            var targets = new ArrayList<>(List.of("svc-a"));
            var c = new Config("edge", List.of(), Map.of("/api", targets));
            targets.add("svc-evil");
            eq(c.routes().get("/api"), List.of("svc-a"));
        });
        test("the getters hand out read-only views, all the way down", () -> {
            var c = sample();
            raises(UnsupportedOperationException.class, () -> c.hosts().add("h2"));
            raises(UnsupportedOperationException.class, () -> c.routes().put("/x", List.of()));
            raises(UnsupportedOperationException.class, () -> c.routes().get("/api").add("svc-b"));
        });
        test("withHost and withRoute return new snapshots and leave the original alone", () -> {
            var base = sample();
            var bigger = base.withHost("h2").withRoute("/img", List.of("cdn"));
            eq(bigger.hosts(), List.of("h1", "h2"));
            eq(bigger.routes(), Map.of("/api", List.of("svc-a"), "/img", List.of("cdn")));
            eq(base.hosts(), List.of("h1"));
            eq(base.routes(), Map.of("/api", List.of("svc-a")));
        });
        test("update applies the change and returns the NEW snapshot", () -> {
            var holder = new ConfigHolder(sample());
            var next = holder.update(c -> c.withHost("h2"));
            eq(next.hosts(), List.of("h1", "h2"));
            ok(holder.current() == next);
        });
        test("a reader holding an old snapshot keeps seeing it after an update", () -> {
            var holder = new ConfigHolder(sample());
            var seenByRequest = holder.current();
            holder.update(c -> c.withRoute("/api", List.of("svc-b")));
            eq(seenByRequest.routes().get("/api"), List.of("svc-a"));
            eq(holder.current().routes().get("/api"), List.of("svc-b"));
        });
        test("four threads updating through one holder lose no update", () -> {
            var holder = new ConfigHolder(new Config("edge", List.of(), Map.of()));
            eq(holder.current().hosts(), List.of());
            var pool = Executors.newFixedThreadPool(4);
            try {
                var jobs = new ArrayList<Callable<Void>>();
                for (int t = 0; t < 4; t++) {
                    int worker = t;
                    jobs.add(() -> {
                        for (int i = 0; i < 250; i++) {
                            var host = "w" + worker + "-" + i;
                            holder.update(c -> c.withHost(host));
                        }
                        return null;
                    });
                }
                for (var f : pool.invokeAll(jobs)) f.get();
            } finally {
                pool.shutdownNow();
            }
            var hosts = holder.current().hosts();
            eq(hosts.size(), 1_000);
            eq(new HashSet<>(hosts).size(), 1_000);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
