// ─────────────────────────────────────────────────────────────────────────
//  03 · books by author — SOLUTION                          ★★☆ core
//  concepts: Collectors.groupingBy · partitioningBy · toMap merge · TreeMap
//  run: java -cp ../../_lib/out 03-grouping.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  groupingBy(classifier, mapFactory, downstream) is the general form:
//  classifier picks the key (Book::author), TreeMap::new makes the keys
//  sorted, and the downstream collector decides what each group becomes —
//  mapping(Book::title, toList()) turns the group's books into titles.
//  Sorting the titles happens after collecting; collectingAndThen can do
//  it inside the collector, or sort each list in a second pass.
//  partitioningBy is groupingBy for a boolean key, with one extra promise
//  that the tests check: the result ALWAYS has both true and false, even
//  for an empty input. groupingBy(b -> b.year() < cutoff) would return {}
//  for an empty list, and the caller's get(true) would be null.
//  counting() produces a Long.
//  toMap(key, value) throws IllegalStateException ("Duplicate key") the
//  moment two books share an author — which is every real catalogue. The
//  three-argument form takes a merge function for collisions; Math::max
//  keeps the newest year. A fourth argument (TreeMap::new) picks the map.

import static bootcamp.Check.*;

import java.util.*;
import java.util.stream.*;

class Exercise {

    record Book(String title, String author, int year) {}

    static Map<String, List<String>> titlesByAuthor(List<Book> books) {
        return books.stream().collect(Collectors.groupingBy(
            Book::author,
            TreeMap::new,
            Collectors.collectingAndThen(
                Collectors.mapping(Book::title, Collectors.toList()),
                titles -> titles.stream().sorted().toList())));
    }

    static Map<Boolean, Long> classicSplit(List<Book> books, int cutoffYear) {
        return books.stream().collect(Collectors.partitioningBy(b -> b.year() < cutoffYear, Collectors.counting()));
    }

    static Map<String, Integer> newestByAuthor(List<Book> books) {
        return books.stream().collect(Collectors.toMap(Book::author, Book::year, Math::max, TreeMap::new));
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
        test("classicSplit counts both sides", () -> eq(classicSplit(SHELF, 1950), Map.of(true, 2L, false, 2L)));
        test("classicSplit always has both keys", () -> eq(classicSplit(List.of(), 1950), Map.of(true, 0L, false, 0L)));
        test("newestByAuthor keeps the newest year", () ->
            eq(newestByAuthor(SHELF), Map.of("Austen", 1817, "Herbert", 1965, "Morrison", 1987)));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
