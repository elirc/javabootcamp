// ─────────────────────────────────────────────────────────────────────────
//  07 · one leader at a time — SOLUTION                     ★★☆ core
//  concepts: leases · leader election · fencing tokens · injected clocks
//  run: java -cp ../../_lib/out 07-leader-lease.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  A lease is a lock with an expiry: whoever holds it is leader until
//  acquiredAt + ttl, and must renew before then. If the leader crashes,
//  nobody has to detect it; the lease simply runs out and another node
//  takes over. (ZooKeeper sessions, etcd leases, Kubernetes Lease
//  objects and Chubby all work this way.) The boundary matters: the
//  lease is live while now < expiresAt, so at exactly expiresAt it is
//  gone, and acquire, renew and leader() must all agree on that.
//  Renewing an expired lease is the classic bug: the old leader wakes up
//  from a long pause, renews, and now two nodes believe they lead.
//  Expiry alone cannot prevent that, because the paused node does not
//  know time passed. The fix lives in the STORAGE: each new acquisition
//  gets a larger fencing token, every write carries it, and the store
//  rejects tokens lower than the highest it has seen. A deposed leader's
//  late write then bounces, however confused it is. Equal tokens pass:
//  the current leader writes many times.
//  The clock is a LongSupplier so tests move time by hand; with
//  System.currentTimeMillis() this would need real sleeps and would
//  still be flaky.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.LongSupplier;

class Exercise {

    static final class LeaseLock {
        private final LongSupplier clock;
        private final long ttlMillis;
        private String holder;
        private long token;
        private long expiresAt;
        private long lastToken;

        LeaseLock(LongSupplier clock, long ttlMillis) {
            this.clock = clock;
            this.ttlMillis = ttlMillis;
        }

        private boolean live() {
            return holder != null && clock.getAsLong() < expiresAt;
        }

        OptionalLong tryAcquire(String node) {
            if (live() && !holder.equals(node)) return OptionalLong.empty();
            if (!live()) {
                holder = node;
                token = ++lastToken;
            }
            expiresAt = clock.getAsLong() + ttlMillis;
            return OptionalLong.of(token);
        }

        boolean renew(String node, long token) {
            if (!live() || !holder.equals(node) || this.token != token) return false;
            expiresAt = clock.getAsLong() + ttlMillis;
            return true;
        }

        void release(String node, long token) {
            if (holder != null && holder.equals(node) && this.token == token) holder = null;
        }

        Optional<String> leader() {
            return live() ? Optional.of(holder) : Optional.empty();
        }
    }

    static final class FencedStore {
        private long highestToken;
        private final List<String> writes = new ArrayList<>();

        boolean write(long token, String value) {
            if (token < highestToken) return false;
            highestToken = token;
            writes.add(value);
            return true;
        }

        List<String> writes() {
            return List.copyOf(writes);
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("the first node gets the lease with token 1", () -> {
            long[] now = {1000};
            var lock = new LeaseLock(() -> now[0], 100);
            eq(lock.tryAcquire("A"), OptionalLong.of(1));
            eq(lock.leader(), Optional.of("A"));
        });
        test("others are refused while the lease is live; the holder keeps its token", () -> {
            long[] now = {0};
            var lock = new LeaseLock(() -> now[0], 100);
            lock.tryAcquire("A");
            now[0] = 50;
            eq(lock.tryAcquire("B"), OptionalLong.empty());
            eq(lock.tryAcquire("A"), OptionalLong.of(1));
            now[0] = 149;
            eq(lock.tryAcquire("B"), OptionalLong.empty());
        });
        test("the lease ends exactly at acquire time + ttl", () -> {
            long[] now = {0};
            var lock = new LeaseLock(() -> now[0], 100);
            lock.tryAcquire("A");
            now[0] = 99;
            eq(lock.leader(), Optional.of("A"));
            now[0] = 100;
            eq(lock.leader(), Optional.empty());
            eq(lock.tryAcquire("B"), OptionalLong.of(2));
            eq(lock.leader(), Optional.of("B"));
        });
        test("renew extends the lease from now", () -> {
            long[] now = {0};
            var lock = new LeaseLock(() -> now[0], 100);
            long t = lock.tryAcquire("A").getAsLong();
            now[0] = 90;
            eq(lock.renew("A", t), true);
            now[0] = 189;
            eq(lock.leader(), Optional.of("A"));
            now[0] = 190;
            eq(lock.leader(), Optional.empty());
        });
        test("an expired lease cannot be renewed, even if nobody took it", () -> {
            long[] now = {0};
            var lock = new LeaseLock(() -> now[0], 100);
            long t = lock.tryAcquire("A").getAsLong();
            now[0] = 100;
            eq(lock.renew("A", t), false);
            eq(lock.leader(), Optional.empty());
            eq(lock.tryAcquire("A"), OptionalLong.of(2));
        });
        test("a stale token or another node cannot renew or release", () -> {
            long[] now = {0};
            var lock = new LeaseLock(() -> now[0], 100);
            lock.tryAcquire("A");
            now[0] = 100;
            long t2 = lock.tryAcquire("B").getAsLong();
            eq(lock.renew("A", 1), false);
            eq(lock.renew("B", 1), false);
            eq(lock.renew("A", t2), false);
            lock.release("A", 1);
            lock.release("A", t2);
            eq(lock.leader(), Optional.of("B"));
        });
        test("release lets another node in at once with a new token", () -> {
            long[] now = {0};
            var lock = new LeaseLock(() -> now[0], 100);
            long t = lock.tryAcquire("A").getAsLong();
            now[0] = 10;
            lock.release("A", t);
            eq(lock.leader(), Optional.empty());
            eq(lock.tryAcquire("B"), OptionalLong.of(2));
        });
        test("the store fences off a deposed leader's late write", () -> {
            long[] now = {0};
            var lock = new LeaseLock(() -> now[0], 100);
            var store = new FencedStore();
            long a = lock.tryAcquire("A").getAsLong();
            eq(store.write(a, "A1"), true);
            now[0] = 500;
            long b = lock.tryAcquire("B").getAsLong();
            eq(store.write(b, "B1"), true);
            eq(store.write(b, "B2"), true);
            eq(store.write(a, "A2-late"), false);
            eq(store.writes(), List.of("A1", "B1", "B2"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
