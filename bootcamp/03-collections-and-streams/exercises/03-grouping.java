// ─────────────────────────────────────────────────────────────────────────
//  03 · books by author                                     ★★☆ core
//  concepts: Collectors.groupingBy · partitioningBy · toMap merge · TreeMap
//  run: java -cp ../../_lib/out 03-grouping.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A library tracker builds three summaries of its catalogue (the record
//  Book is defined above the code you edit):
//
//    titlesByAuthor  author → that author's titles, sorted; authors sorted
//    classicSplit    true → how many books are from before the cutoff
//                    year, false → how many are not (a book FROM the
//                    cutoff year is not older); BOTH keys always present
//    newestByAuthor  author → the year of their newest book
//
//      titlesByAuthor([Emma/Austen, Dune/Herbert, Persuasion/Austen])
//          → {Austen=[Emma, Persuasion], Herbert=[Dune]}
//      classicSplit([], 1950)            → {false=0, true=0}
//
//  hint: Collectors.toMap throws on a duplicate key unless you merge.

import static bootcamp.Check.*;

import java.util.*;
import java.util.stream.*;

class Exercise {

    record Book(String title, String author, int year) {}

    static Map<String, List<String>> titlesByAuthor(List<Book> books) {
        throw new UnsupportedOperationException("TODO");
    }

    static Map<Boolean, Long> classicSplit(List<Book> books, int cutoffYear) {
        throw new UnsupportedOperationException("TODO");
    }

    static Map<String, Integer> newestByAuthor(List<Book> books) {
        throw new UnsupportedOperationException("TODO");
    }

    static final List<Book> SHELF = List.of(
        new Book("Persuasion", "Austen", 1817),
        new Book("Dune", "Herbert", 1965),
        new Book("Emma", "Austen", 1815),
        new Book("Beloved", "Morrison", 1987));

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("groups titles by author, sorted", () -> eq(titlesByAuthor(SHELF), Map.of(
            "Austen", List.of("Emma", "Persuasion"),
            "Herbert", List.of("Dune"),
            "Morrison", List.of("Beloved"))));
        test("authors come out alphabetically", () ->
            eq(new ArrayList<>(titlesByAuthor(SHELF).keySet()), List.of("Austen", "Herbert", "Morrison")));
        test("an empty shelf groups to nothing", () -> eq(titlesByAuthor(List.of()), Map.of()));
        test("classicSplit counts both sides", () -> {
            eq(classicSplit(SHELF, 1950), Map.of(true, 2L, false, 2L));
            eq(classicSplit(SHELF, 1965), Map.of(true, 2L, false, 2L));
        });
        test("classicSplit always has both keys", () -> eq(classicSplit(List.of(), 1950), Map.of(true, 0L, false, 0L)));
        test("newestByAuthor keeps the newest year", () ->
            eq(newestByAuthor(SHELF), Map.of("Austen", 1817, "Herbert", 1965, "Morrison", 1987)));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
