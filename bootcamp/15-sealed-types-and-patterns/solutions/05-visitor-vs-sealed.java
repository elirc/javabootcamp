// ─────────────────────────────────────────────────────────────────────────
//  05 · visitor versus sealed dispatch — SOLUTION           ★★☆ core
//  concepts: visitor pattern · double dispatch · sealed dispatch · trees
//  run: java -cp ../../_lib/out 05-visitor-vs-sealed.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  The VISITOR pattern (Gang of Four) gives a closed set of node types an
//  open set of operations without instanceof: each node's accept calls
//  the one visitX method for its own type ("double dispatch"), and each
//  operation is a class implementing Visitor<R>. Adding NodeCounter
//  touched no node class. The cost: adding a node type (Image, say) means
//  a new visitImage on the interface and in every visitor.
//  A sealed hierarchy with instanceof (and Java 21's pattern switch) gets
//  the same open set of operations with less ceremony: depth and mapText
//  are plain static methods. Visitors still earn their keep when the
//  operations are many and stateful, when the hierarchy predates sealed
//  types (javac's own tree API, ASM, ANTLR), or when a library wants
//  callers to plug in operations without seeing the internals.
//  The details: LinkCollector adds the url BEFORE visiting children
//  (pre-order), so the outer link comes first. Markdown must escape the
//  characters it uses as syntax, or a price like "2*3" turns bold.
//  mapText rebuilds containers around the mapped children and keeps
//  the url; records make "same structure" a single equals check.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    interface Visitor<R> {
        R visitText(Text t);
        R visitBold(Bold b);
        R visitLink(Link l);
    }

    sealed interface Node permits Text, Bold, Link {
        <R> R accept(Visitor<R> v);
    }

    record Text(String text) implements Node {
        public <R> R accept(Visitor<R> v) { return v.visitText(this); }
    }

    record Bold(List<Node> children) implements Node {
        public <R> R accept(Visitor<R> v) { return v.visitBold(this); }
    }

    record Link(String url, List<Node> children) implements Node {
        public <R> R accept(Visitor<R> v) { return v.visitLink(this); }
    }

    static final class PlainText implements Visitor<String> {
        public String visitText(Text t) {
            return t.text();
        }

        public String visitBold(Bold b) {
            return join(b.children());
        }

        public String visitLink(Link l) {
            return join(l.children());
        }

        private String join(List<Node> nodes) {
            var sb = new StringBuilder();
            for (var n : nodes) sb.append(n.accept(this));
            return sb.toString();
        }
    }

    static final class Markdown implements Visitor<String> {
        public String visitText(Text t) {
            return t.text().replaceAll("([*\\[\\]])", "\\\\$1");
        }

        public String visitBold(Bold b) {
            return "**" + join(b.children()) + "**";
        }

        public String visitLink(Link l) {
            return "[" + join(l.children()) + "](" + l.url() + ")";
        }

        private String join(List<Node> nodes) {
            var sb = new StringBuilder();
            for (var n : nodes) sb.append(n.accept(this));
            return sb.toString();
        }
    }

    static final class LinkCollector implements Visitor<List<String>> {
        public List<String> visitText(Text t) {
            return List.of();
        }

        public List<String> visitBold(Bold b) {
            return collect(b.children());
        }

        public List<String> visitLink(Link l) {
            var urls = new ArrayList<String>();
            urls.add(l.url());
            urls.addAll(collect(l.children()));
            return urls;
        }

        private List<String> collect(List<Node> nodes) {
            var urls = new ArrayList<String>();
            for (var n : nodes) urls.addAll(n.accept(this));
            return urls;
        }
    }

    static final class NodeCounter implements Visitor<Integer> {
        public Integer visitText(Text t) {
            return 1;
        }

        public Integer visitBold(Bold b) {
            return 1 + count(b.children());
        }

        public Integer visitLink(Link l) {
            return 1 + count(l.children());
        }

        private int count(List<Node> nodes) {
            int total = 0;
            for (var n : nodes) total += n.accept(this);
            return total;
        }
    }

    static int depth(Node n) {
        if (n instanceof Text) return 1;
        List<Node> children = n instanceof Bold b ? b.children() : ((Link) n).children();
        int deepest = 0;
        for (var c : children) deepest = Math.max(deepest, depth(c));
        return 1 + deepest;
    }

    static Node mapText(Node n, UnaryOperator<String> f) {
        if (n instanceof Text t) return new Text(f.apply(t.text()));
        if (n instanceof Bold b) return new Bold(mapAll(b.children(), f));
        if (n instanceof Link l) return new Link(l.url(), mapAll(l.children(), f));
        throw new AssertionError("unhandled " + n);
    }

    private static List<Node> mapAll(List<Node> nodes, UnaryOperator<String> f) {
        return nodes.stream().map(c -> mapText(c, f)).toList();
    }

    static final Node ARTICLE = new Bold(List.of(
        new Text("Read "),
        new Link("/faq", List.of(new Text("the FAQ "), new Link("/faq#refunds", List.of(new Text("on refunds"))))),
        new Text(" or "),
        new Link("/contact", List.of(new Text("ask us")))));

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("PlainText joins the text and drops the formatting", () ->
            eq(ARTICLE.accept(new PlainText()), "Read the FAQ on refunds or ask us"));
        test("Markdown renders bold and links, nested", () -> {
            var doc = new Bold(List.of(new Text("hi "), new Link("u", List.of(new Text("there")))));
            eq(doc.accept(new Markdown()), "**hi [there](u)**");
            eq(new Link("/a", List.of()).accept(new Markdown()), "[](/a)");
        });
        test("Markdown escapes the characters it uses as syntax", () ->
            eq(new Text("2*3 [x]").accept(new Markdown()), "2\\*3 \\[x\\]"));
        test("LinkCollector lists urls in document order, outer before inner", () ->
            eq(ARTICLE.accept(new LinkCollector()), List.of("/faq", "/faq#refunds", "/contact")));
        test("NodeCounter is a new operation that needed no change to the nodes", () -> {
            eq(ARTICLE.accept(new NodeCounter()), 9);
            eq(new Text("x").accept(new NodeCounter()), 1);
        });
        test("depth is the deepest nesting, and an empty container counts as 1", () -> {
            eq(depth(ARTICLE), 4);
            eq(depth(new Text("x")), 1);
            eq(depth(new Bold(List.of())), 1);
        });
        test("mapText rewrites every Text and keeps the structure and urls", () -> {
            var doc = new Bold(List.of(new Text("a"), new Link("/x", List.of(new Text("b")))));
            eq(mapText(doc, s -> s.toUpperCase(Locale.ROOT)),
                new Bold(List.of(new Text("A"), new Link("/x", List.of(new Text("B"))))));
            eq(mapText(ARTICLE, s -> s).equals(ARTICLE), true);
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
