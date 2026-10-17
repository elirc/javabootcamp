// ─────────────────────────────────────────────────────────────────────────
//  07 · template method vs composition                      ★★☆ core
//  concepts: template method · hook methods · composition over inheritance
//  run: java -cp ../../_lib/out 07-template-vs-composition.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Sales reports export as CSV or Markdown. First by inheritance: the
//  TEMPLATE export(rows) writes header(), formatRow(r) per row and
//  totals(rows), each followed by the hook lineEnd() (default "\n"):
//    CsvExporter       "item,qty,amount"; rows "<item>,<qty>,<amount>";
//                      "TOTAL,<qty sum>,<amount sum>"; lineEnd "\r\n". An
//                      item holding , " CR or LF is wrapped in quotes with
//                      each " doubled
//    MarkdownExporter  "| item | qty | amount |", "| --- | ---: | ---: |"
//                      (two header lines); rows "| <item> | <qty> | <amt> |"
//                      with each | in an item written as \|; totals
//                      "| **total** | <qty sum> | <amount sum> |"
//  Amounts always have 2 decimals (HALF_UP). Then by composition: finish
//  exportWith(format, rows) for an ExportFormat record of parts, and
//  csvFormat(), which must give exactly CsvExporter's output.
//
//      csv of [Row(bolt, 3, 2.5)] → "item,qty,amount\r\nbolt,3,2.50\r\n..."
//
//  hint: the subclasses and csvFormat() can share small static helpers.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.*;

class Exercise {

    record Row(String item, int qty, BigDecimal amount) {}

    abstract static class ReportExporter {
        final String export(List<Row> rows) {
            throw new UnsupportedOperationException("TODO");
        }

        abstract String header();

        abstract String formatRow(Row row);

        abstract String totals(List<Row> rows);

        String lineEnd() {
            return "\n";
        }
    }

    static final class CsvExporter extends ReportExporter {
        String header() {
            throw new UnsupportedOperationException("TODO");
        }

        String formatRow(Row row) {
            throw new UnsupportedOperationException("TODO");
        }

        String totals(List<Row> rows) {
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        String lineEnd() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    static final class MarkdownExporter extends ReportExporter {
        String header() {
            throw new UnsupportedOperationException("TODO");
        }

        String formatRow(Row row) {
            throw new UnsupportedOperationException("TODO");
        }

        String totals(List<Row> rows) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    record ExportFormat(String header, Function<Row, String> row, Function<List<Row>, String> totals, String lineEnd) {}

    static String exportWith(ExportFormat format, List<Row> rows) {
        throw new UnsupportedOperationException("TODO");
    }

    static ExportFormat csvFormat() {
        throw new UnsupportedOperationException("TODO");
    }

    static final List<Row> SALES = List.of(
        new Row("bolt", 3, new BigDecimal("2.5")),
        new Row("nut", 10, new BigDecimal("0.333")),
        new Row("washer", 1, new BigDecimal("4")));

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("CSV writes header, rows and totals, each ending in CRLF", () ->
            eq(new CsvExporter().export(SALES),
                "item,qty,amount\r\nbolt,3,2.50\r\nnut,10,0.33\r\nwasher,1,4.00\r\nTOTAL,14,6.83\r\n"));
        test("CSV quotes an item holding a comma, a quote or a line break", () -> {
            var rows = List.of(new Row("5\" bolt, zinc", 1, BigDecimal.ONE), new Row("two\nlines", 2, BigDecimal.TEN),
                new Row("cr\rhere", 0, BigDecimal.ZERO));
            eq(new CsvExporter().export(rows),
                "item,qty,amount\r\n\"5\"\" bolt, zinc\",1,1.00\r\n\"two\nlines\",2,10.00\r\n\"cr\rhere\",0,0.00\r\nTOTAL,3,11.00\r\n");
        });
        test("amounts round half up to two decimals", () -> {
            var rows = List.of(new Row("a", 1, new BigDecimal("0.005")), new Row("b", 1, new BigDecimal("0.004")));
            eq(new CsvExporter().export(rows), "item,qty,amount\r\na,1,0.01\r\nb,1,0.00\r\nTOTAL,2,0.01\r\n");
        });
        test("Markdown keeps the default line end and writes a table", () ->
            eq(new MarkdownExporter().export(SALES.subList(0, 1)),
                "| item | qty | amount |\n| --- | ---: | ---: |\n| bolt | 3 | 2.50 |\n| **total** | 3 | 2.50 |\n"));
        test("Markdown escapes a pipe inside an item", () ->
            eq(new MarkdownExporter().export(List.of(new Row("in|out", 2, BigDecimal.ONE))),
                "| item | qty | amount |\n| --- | ---: | ---: |\n| in\\|out | 2 | 1.00 |\n| **total** | 2 | 1.00 |\n"));
        test("an empty report still has its header and a zero total", () -> {
            eq(new CsvExporter().export(List.of()), "item,qty,amount\r\nTOTAL,0,0.00\r\n");
            eq(exportWith(csvFormat(), List.of()), "item,qty,amount\r\nTOTAL,0,0.00\r\n");
        });
        test("csvFormat built from parts matches the template byte for byte", () -> {
            var tricky = new ArrayList<>(SALES);
            tricky.add(new Row("say \"hi\"", 4, new BigDecimal("1.255")));
            eq(exportWith(csvFormat(), tricky), new CsvExporter().export(tricky));
        });
        test("a new format by composition needs no subclass", () -> {
            var plain = new ExportFormat("ITEMS", Row::item, rows -> rows.size() + " rows", "\n");
            eq(exportWith(plain, SALES), "ITEMS\nbolt\nnut\nwasher\n3 rows\n");
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
