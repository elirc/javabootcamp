// ─────────────────────────────────────────────────────────────────────────
//  06 · the sessions that would not die — SOLUTION          ★★☆ core
//  concepts: debugging · removing while iterating · List.remove overloads
//  run: java -cp ../../_lib/out 06-session-cleanup.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  1. Reproduce "one survives": two sessions expiring at 5, cleanup at 5.
//     Trace i: at i = 0 the first is removed, the second slides down to
//     index 0, then i++ makes i = 1 and the loop never looks at it.
//     Every removal skips the element after it, so a lone expired
//     session works and neighbours do not. Fix: removeIf, which is
//     written for exactly this (an Iterator with it.remove() also
//     works; so does a loop that walks backwards). Pinned by "removes
//     expired sessions that sit side by side".
//  2. Reproduce "logout 3 removed someone else": [10, 3, 7, 1], logout
//     3 removes 1, the element at INDEX 3. List<Integer> has two remove
//     methods, remove(int index) and remove(Object o). An int argument
//     picks remove(int) because overload resolution prefers no boxing;
//     with 42 that index does not exist, hence the crash. Fix: box it,
//     remove(Integer.valueOf(userId)). Pinned by "logout removes that
//     user id, not the one at that index".
//  Both bugs are in module 03 (exercises 01 and 07). Code review smell:
//  any list.remove(i) inside an index loop, and any remove(x) on a List
//  of Integer.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Session(int userId, long expiresAt) {}

    static void removeExpired(List<Session> sessions, long now) {
        sessions.removeIf(s -> s.expiresAt() <= now);
    }

    static void logout(List<Integer> activeUserIds, int userId) {
        activeUserIds.remove(Integer.valueOf(userId));
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("removes a lone expired session and keeps the rest in order", () -> {
            var s = new ArrayList<>(List.of(new Session(1, 9), new Session(2, 3), new Session(3, 8)));
            removeExpired(s, 5);
            eq(s, List.of(new Session(1, 9), new Session(3, 8)));
        });
        test("a session expiring exactly now is expired", () -> {
            var s = new ArrayList<>(List.of(new Session(1, 5), new Session(2, 6)));
            removeExpired(s, 5);
            eq(s, List.of(new Session(2, 6)));
        });
        test("removes expired sessions that sit side by side", () -> {
            var s = new ArrayList<>(List.of(new Session(1, 1), new Session(2, 2), new Session(3, 9),
                new Session(4, 3), new Session(5, 4)));
            removeExpired(s, 5);
            eq(s, List.of(new Session(3, 9)));
        });
        test("removes everything when everything expired", () -> {
            var s = new ArrayList<>(List.of(new Session(1, 1), new Session(2, 1), new Session(3, 1)));
            removeExpired(s, 1);
            eq(s, List.of());
        });
        test("logout removes that user id, not the one at that index", () -> {
            var ids = new ArrayList<>(List.of(10, 3, 7, 1));
            logout(ids, 3);
            eq(ids, List.of(10, 7, 1));
            logout(ids, 1);
            eq(ids, List.of(10, 7));
        });
        test("logout of an id that is not active changes nothing", () -> {
            var ids = new ArrayList<>(List.of(1, 2));
            logout(ids, 42);
            eq(ids, List.of(1, 2));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
