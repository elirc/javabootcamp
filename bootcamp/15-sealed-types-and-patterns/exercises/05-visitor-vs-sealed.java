// ─────────────────────────────────────────────────────────────────────────
//  05 · visitor versus sealed dispatch                      ★★☆ core
//  concepts: visitor pattern · double dispatch · sealed dispatch · trees
//  run: java -cp ../../_lib/out 05-visitor-vs-sealed.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A help-centre article is a tree: Text, Bold(children) and Link(url,
//  children). Each node already has accept(Visitor). Write the operations
//  two ways. As Visitors:
//
//    PlainText       all text, formatting dropped
//    Markdown        Bold → **inner**, Link → [inner](url); in Text, put a
//                    backslash before each * [ and ]
//    LinkCollector   every url in document order (a link before the links
//                    nested inside it)
//    NodeCounter     the number of nodes, containers included
//  As sealed dispatch with instanceof:
//    depth(n)        Text is 1; a container is 1 + its deepest child (1
//                    when it has no children)
//    mapText(n, f)   the same tree with f applied to every Text
//
//      Markdown: Bold[Text "hi ", Link(u, [Text "x"])] → **hi [x](u)**
//
//  hint: a visitor method can call accept on children with `this`.

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
            throw new UnsupportedOperationException("TODO");
        }

        public String visitBold(Bold b) {
            throw new UnsupportedOperationException("TODO");
        }

        public String visitLink(Link l) {
            throw new UnsupportedOperationException("TODO");
        }

    }

    static final class Markdown implements Visitor<String> {
        public String visitText(Text t) {
            throw new UnsupportedOperationException("TODO");
        }

        public String visitBold(Bold b) {
            throw new UnsupportedOperationException("TODO");
        }

        public String visitLink(Link l) {
            throw new UnsupportedOperationException("TODO");
        }

    }

    static final class LinkCollector implements Visitor<List<String>> {
        public List<String> visitText(Text t) {
            throw new UnsupportedOperationException("TODO");
        }

        public List<String> visitBold(Bold b) {
            throw new UnsupportedOperationException("TODO");
        }

        public List<String> visitLink(Link l) {
            throw new UnsupportedOperationException("TODO");
        }

    }

    static final class NodeCounter implements Visitor<Integer> {
        public Integer visitText(Text t) {
            throw new UnsupportedOperationException("TODO");
        }

        public Integer visitBold(Bold b) {
            throw new UnsupportedOperationException("TODO");
        }

        public Integer visitLink(Link l) {
            throw new UnsupportedOperationException("TODO");
        }

    }

    static int depth(Node n) {
        throw new UnsupportedOperationException("TODO");
    }

    static Node mapText(Node n, UnaryOperator<String> f) {
        throw new UnsupportedOperationException("TODO");
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
