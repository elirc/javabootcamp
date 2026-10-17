// ─────────────────────────────────────────────────────────────────────────
//  04 · everybody is a VIP                                  ★★☆ core
//  concepts: debugging · aliasing · shared mutable state · defensive copies
//  run: java -cp ../../_lib/out 04-shared-tags.java
// ─────────────────────────────────────────────────────────────────────────
//
//  DEBUG HUNT: the code below is complete and compiles; it has bugs. Run
//  it, read the failing tests, find and fix the bugs. Do not edit tests.
//  Intended: every User owns its own tags. create(name) starts with
//  ["new"]; create(name, tags) starts with a copy of tags (any List,
//  immutable ones too); tag(user, t) adds t once, to that user only;
//  tagsOf(user) is an unmodifiable snapshot that later tags don't change.
//  Bug reports:
//    "We tagged one new signup 'vip'. Every signup since is born VIP."
//    "The import job builds a batch of users from one list. They all
//     end up with each other's tags."
//    "A report sorted tagsOf(user) and the user's tags got reordered."
//
//      u = create("ada"); tag(u, "vip"); tagsOf(create("bob")) → [new]
//
//  hint: count how many List objects exist. Then count how many you need.

import static bootcamp.Check.*;

import java.util.*;

class Exercise {

    static final List<String> DEFAULT_TAGS = new ArrayList<>(List.of("new"));

    static final class User {
        final String name;
        final List<String> tags;

        User(String name, List<String> tags) {
            this.name = name;
            this.tags = tags;
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
        return user.tags;
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
