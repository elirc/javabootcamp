// ─────────────────────────────────────────────────────────────────────────
//  04 · everybody is a VIP — SOLUTION                       ★★☆ core
//  concepts: debugging · aliasing · shared mutable state · defensive copies
//  run: java -cp ../../_lib/out 04-shared-tags.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Three reports, two bugs, one idea: a reference is not a copy.
//  Reproduce report 1: create, tag "vip", create again. Narrow by
//  asking whether two users' lists are the same object: a.tags ==
//  b.tags is true. Every create(name) hands out DEFAULT_TAGS itself, so
//  one tag() edits the shared default. Report 2 is the same bug with the
//  caller's list: create(name, batch) stores batch, so every user of the
//  batch (and the caller) shares one list. And List.of input crashes on
//  the first tag(), because that list is immutable.
//  Fix 1, at the ONE place that owns the data: the User constructor
//  copies (new ArrayList<>(tags)). Fixing each caller instead would
//  leave the next caller broken. Making DEFAULT_TAGS a List.of also
//  turns any future aliasing of it into a loud exception.
//  Report 3 is the way out: tagsOf returned the internal list, so the
//  report's sort reordered the user. Fix 2: return List.copyOf, an
//  unmodifiable snapshot. Collections.unmodifiableList would stop the
//  sort but is a live VIEW: it would still change after a later tag().
//  Pinned by "users built from one list do not share tags" and "tagsOf
//  is an unmodifiable snapshot" (module 03, module 05 exercise 08).

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final List<String> DEFAULT_TAGS = List.of("new");

    static final class User {
        final String name;
        final List<String> tags;

        User(String name, List<String> tags) {
            this.name = name;
            this.tags = new ArrayList<>(tags);
        }
    }

    static User create(String name) {
        return new User(name, DEFAULT_TAGS);
    }

    static User create(String name, List<String> tags) {
        return new User(name, tags);
    }

    static void tag(User user, String tag) {
        if (!user.tags.contains(tag)) user.tags.add(tag);
    }

    static List<String> tagsOf(User user) {
        return List.copyOf(user.tags);
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("a new user starts with the default tag", () -> {
            eq(tagsOf(create("ada")), List.of("new"));
        });
        test("tagging one new user does not tag the next one", () -> {
            var ada = create("ada");
            tag(ada, "vip");
            eq(tagsOf(ada), List.of("new", "vip"));
            eq(tagsOf(create("bob")), List.of("new"));
        });
        test("users built from one list do not share tags", () -> {
            var batch = new ArrayList<>(List.of("imported"));
            var a = create("a", batch);
            var b = create("b", batch);
            tag(a, "x");
            eq(tagsOf(b), List.of("imported"));
            batch.add("late");
            eq(tagsOf(a), List.of("imported", "x"));
        });
        test("an immutable list is a fine starting point", () -> {
            var c = create("c", List.of("t"));
            tag(c, "u");
            eq(tagsOf(c), List.of("t", "u"));
        });
        test("tagsOf is an unmodifiable snapshot", () -> {
            var u = create("u", List.of("b", "a"));
            var snapshot = tagsOf(u);
            raises(UnsupportedOperationException.class, () -> snapshot.sort(null));
            tag(u, "c");
            eq(snapshot, List.of("b", "a"));
            eq(tagsOf(u), List.of("b", "a", "c"));
        });
        test("a tag is added once", () -> {
            var u = create("u");
            tag(u, "x");
            tag(u, "x");
            tag(u, "new");
            eq(tagsOf(u), List.of("new", "x"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
