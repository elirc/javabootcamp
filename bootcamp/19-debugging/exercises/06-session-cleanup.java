// ─────────────────────────────────────────────────────────────────────────
//  06 · the sessions that would not die                     ★★☆ core
//  concepts: debugging · removing while iterating · List.remove overloads
//  run: java -cp ../../_lib/out 06-session-cleanup.java
// ─────────────────────────────────────────────────────────────────────────
//
//  DEBUG HUNT: the code below is complete and compiles; it has bugs. Run
//  it, read the failing tests, find and fix the bugs. Do not edit tests.
//  Intended: removeExpired(sessions, now) removes, in place, every
//  session with expiresAt <= now and keeps the rest in order.
//  logout(activeUserIds, userId) removes that user id from the list
//  (ids are unique); an id that is not there changes nothing.
//  Bug reports:
//    "When two sessions expire at the same moment, one survives."
//    "Logging out user 3 logged out somebody else."
//    "Logging out user 42 crashed with IndexOutOfBoundsException."
//
//      removeExpired([s(1, 5), s(2, 5), s(3, 9)], 5) → [s(3, 9)]
//      logout([10, 3, 7], 3)                       → [10, 7]
//
//  hint: trace the loop on paper for two expired sessions side by side.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    record Session(int userId, long expiresAt) {}

    static void removeExpired(List<Session> sessions, long now) {
        for (int i = 0; i < sessions.size(); i++) {
            if (sessions.get(i).expiresAt() <= now) {
                sessions.remove(i);
            }
        }
    }

    static void logout(List<Integer> activeUserIds, int userId) {
        activeUserIds.remove(userId);
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
