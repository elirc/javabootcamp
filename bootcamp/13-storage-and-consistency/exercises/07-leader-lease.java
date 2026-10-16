// ─────────────────────────────────────────────────────────────────────────
//  07 · one leader at a time                                ★★☆ core
//  concepts: leases · leader election · fencing tokens · injected clocks
//  run: java -cp ../../_lib/out 07-leader-lease.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Elect a leader with a lease, the way etcd and ZooKeeper do. A lease
//  taken at time t is live while now < t + ttl. tryAcquire(node): if the
//  lease is live and held by another node → empty; if live and held by
//  node → extend it to now + ttl, same token; otherwise node takes it
//  with a NEW fencing token (1, 2, 3, ...) expiring at now + ttl.
//  renew(node, token) extends to now + ttl only if the lease is live, is
//  node's, and token is its current one; else false and nothing changes.
//  release(node, token) ends the lease immediately under the same match;
//  otherwise it does nothing. leader() is the holder while live.
//  FencedStore.write(token, value) rejects (false) a token lower than the
//  highest it has accepted; equal or higher tokens are appended.
//
//      ttl 100: t=0 acquire(A) → 1   t=50 acquire(B) → empty
//               t=100 acquire(B) → 2   write(1, ...) after write(2) → false
//
//  hint: time comes only from the clock you were given.

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

        OptionalLong tryAcquire(String node) {
            throw new UnsupportedOperationException("TODO");
        }

        boolean renew(String node, long token) {
            throw new UnsupportedOperationException("TODO");
        }

        void release(String node, long token) {
            throw new UnsupportedOperationException("TODO");
        }

        Optional<String> leader() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class FencedStore {
        private long highestToken;
        private final List<String> writes = new ArrayList<>();

        boolean write(long token, String value) {
            throw new UnsupportedOperationException("TODO");
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
            now[0] = 200;
            eq(lock.tryAcquire("A"), OptionalLong.of(3));
            lock.release("A", 1);
            eq(lock.renew("A", 1), false);
            eq(lock.leader(), Optional.of("A"));
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
