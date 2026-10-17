// ─────────────────────────────────────────────────────────────────────────
//  03 · immutable snapshots, published                      ★☆☆ warm-up
//  concepts: immutability · defensive copies · AtomicReference · publication
//  run: java -cp ../../_lib/out 03-immutable-snapshots.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A router's configuration is read on every request and replaced now and
//  then. Make it safe by design: immutable snapshots, swapped atomically.
//    Config(name, hosts, routes)  copies its inputs DEEPLY: later changes
//                      to the caller's list, map or a route's list never
//                      show up inside the Config
//    hosts(), routes() read-only views: any change through them, even to
//                      a route's list, throws UnsupportedOperationException
//    withHost(h)       a new Config with h appended; this one is unchanged
//    withRoute(p, ts)  a new Config with route p set to ts (copied)
//    ConfigHolder      current() returns the published snapshot;
//                      update(change) applies change to it atomically and
//                      returns the NEW snapshot. If another update lands
//                      while change runs, change runs AGAIN on the newer
//                      snapshot. A reader keeps the snapshot it holds
//  One test runs 4 threads x 250 withHost updates: all 1,000 must arrive.
//
//      holder.update(c -> c.withHost("h2"))  → the new Config, hosts [h1, h2]
//
//  hint: List.copyOf copies one level; a map of lists needs two.

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
            throw new UnsupportedOperationException("TODO");
        }

        String name() {
            return name;
        }

        List<String> hosts() {
            throw new UnsupportedOperationException("TODO");
        }

        Map<String, List<String>> routes() {
            throw new UnsupportedOperationException("TODO");
        }

        Config withHost(String host) {
            throw new UnsupportedOperationException("TODO");
        }

        Config withRoute(String path, List<String> targets) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class ConfigHolder {
        private final AtomicReference<Config> current;

        ConfigHolder(Config initial) {
            this.current = new AtomicReference<>(initial);
        }

        Config current() {
            throw new UnsupportedOperationException("TODO");
        }

        Config update(UnaryOperator<Config> change) {
            throw new UnsupportedOperationException("TODO");
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
        test("an update overtaken by another runs its change again on the newer snapshot", () -> {
            var holder = new ConfigHolder(sample());
            var calls = new int[1];
            var result = holder.update(c -> {
                if (calls[0]++ == 0) holder.update(other -> other.withHost("rival"));
                return c.withHost("mine");
            });
            eq(result.hosts(), List.of("h1", "rival", "mine"));
            ok(holder.current() == result);
            eq(calls[0], 2);
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
