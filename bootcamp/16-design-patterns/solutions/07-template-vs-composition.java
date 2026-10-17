// ─────────────────────────────────────────────────────────────────────────
//  07 · template method vs composition — SOLUTION           ★★☆ core
//  concepts: template method · hook methods · composition over inheritance
//  run: java -cp ../../_lib/out 07-template-vs-composition.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  TEMPLATE METHOD fixes the skeleton of an algorithm in a final method
//  of a base class and lets subclasses fill in the steps. export() is
//  final so no subclass can break the order "header, rows, totals"; the
//  abstract methods are the steps; lineEnd() is a HOOK, a step with a
//  default that a subclass may override (CSV per RFC 4180 wants CRLF).
//  It works, and InputStream, AbstractList and HttpServlet are built
//  this way. Its cost is inheritance itself: one parent, steps that call
//  each other in ways only the base class documents, and a new class for
//  every variation.
//  COMPOSITION passes the steps in as values instead: ExportFormat is a
//  record of a header, a row function, a totals function and a line
//  end, and one plain method runs the same skeleton. A new format is a
//  new value, built in a test or at runtime, and the parts are reusable
//  functions. That is why csvFormat() can share the escaping and money
//  helpers with CsvExporter and produce byte-identical output.
//  Use the template when the skeleton is the stable, valuable part and
//  variations are few; use composition when variations multiply or need
//  to be mixed. The CSV detail that bites: quote a field holding a comma,
//  quote, CR or LF, and double the quotes inside it, or a product named
//  '5" bolt' shifts every column after it.

import static bootcamp.Check.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.*;

class Exercise {

    record Row(String item, int qty, BigDecimal amount) {}

    abstract static class ReportExporter {
        final String export(List<Row> rows) {
            var sb = new StringBuilder();
            sb.append(header()).append(lineEnd());
            for (var row : rows) sb.append(formatRow(row)).append(lineEnd());
            sb.append(totals(rows)).append(lineEnd());
            return sb.toString();
        }

        abstract String header();

        abstract String formatRow(Row row);

        abstract String totals(List<Row> rows);

        String lineEnd() {
            return "\n";
        }
    }

    static String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    static int totalQty(List<Row> rows) {
        return rows.stream().mapToInt(Row::qty).sum();
    }

    static BigDecimal totalAmount(List<Row> rows) {
        return rows.stream().map(Row::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static String csvField(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\r") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    static String csvRow(Row r) {
        return csvField(r.item()) + "," + r.qty() + "," + money(r.amount());
    }

    static String csvTotals(List<Row> rows) {
        return "TOTAL," + totalQty(rows) + "," + money(totalAmount(rows));
    }

    static final class CsvExporter extends ReportExporter {
        String header() {
            return "item,qty,amount";
        }

        String formatRow(Row row) {
            return csvRow(row);
        }

        String totals(List<Row> rows) {
            return csvTotals(rows);
        }

        @Override
        String lineEnd() {
            return "\r\n";
        }
    }

    static final class MarkdownExporter extends ReportExporter {
        String header() {
            return "| item | qty | amount |\n| --- | ---: | ---: |";
        }

        String formatRow(Row row) {
            return "| " + row.item().replace("|", "\\|") + " | " + row.qty() + " | " + money(row.amount()) + " |";
        }

        String totals(List<Row> rows) {
            return "| **total** | " + totalQty(rows) + " | " + money(totalAmount(rows)) + " |";
        }
    }

    record ExportFormat(String header, Function<Row, String> row, Function<List<Row>, String> totals, String lineEnd) {}

    static String exportWith(ExportFormat format, List<Row> rows) {
        var sb = new StringBuilder();
        sb.append(format.header()).append(format.lineEnd());
        for (var row : rows) sb.append(format.row().apply(row)).append(format.lineEnd());
        sb.append(format.totals().apply(rows)).append(format.lineEnd());
        return sb.toString();
    }

    static ExportFormat csvFormat() {
        return new ExportFormat("item,qty,amount", Exercise::csvRow, Exercise::csvTotals, "\r\n");
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
