// Verify.java -- the maintainer check. Compiles and runs every file and
// enforces the authoring contract (bootcamp/_lib/FORMAT.md):
//
//     java bootcamp/Verify.java          every module   (from the repo root)
//     java bootcamp/Verify.java 03       one module     (prefix match)
//
//   every file   ->  header parses (the csbootcamp grammar), LF only, no BOM,
//                   one `// --- tests ---` ... `// --- end tests ---` block
//                   holding 4-9 test(...) calls, compiles with
//                   javac --release 17 -encoding UTF-8
//   solutions/   ->  0 failed AND 0 todo AND >0 passed, no TODO stub left,
//                   header title ends " -- SOLUTION", has a Walkthrough:
//   exercises/   ->  0 failed AND 0 passed AND >0 todo (a starter must be
//                   untouched, and no test may pass against the stub)
//                   -- two teaching methods change the starter rule
//                   (FORMAT.md, "Debug hunts and scaffolded drills"):
//     a module whose directory name contains "debug" (debug hunts):
//                   the starter is complete, buggy code with no stub and
//                   no hole; it must compile and have >=1 FAILED test and
//                   0 todo (passing tests are allowed)
//     a module whose directory name contains "scaffold" (Parsons and
//                   fill-in drills): the starter has a TODO stub or a
//                   `/* ___ */` hole; then the ordinary rule: 0 failed
//                   AND 0 passed AND >0 todo
//   solutions/   ->  always all green, in every module, and no `/* ___ */`
//                   hole left
//   the pair     ->  same header (number, title, stars, tier, concepts) and a
//                   BYTE-IDENTICAL tests block
//
// Every run goes through bootcamp.BqMain with a fresh nonce, so this also
// proves the grading channel: the result file must exist, carry the nonce,
// and agree with the `#done` line on stdout.
//
// Exit code 1 if anything is broken. Compiles in-process, then runs the
// files two at a time (the machine this was built on is small).

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.ToolProvider;

public class Verify {

    static final Pattern MODULE = Pattern.compile("^\\d{2}-[a-z0-9-]+$");
    static final Pattern RULE = Pattern.compile("^// \u2500{20,}\\s*$");
    static final Pattern TITLE = Pattern.compile("^//\\s+(\\d{2}) \u00b7 (.+?)\\s{2,}([\u2605\u2606]{3})\\s+(\\S.*?)\\s*$");
    static final Pattern CONCEPTS = Pattern.compile("^//\\s+concepts:\\s*(.+?)\\s*$");
    static final Pattern RUN = Pattern.compile("^//\\s+run:");
    static final Pattern TESTS_OPEN = Pattern.compile("^\\s*// \u2500\u2500\u2500 tests \u2500+\\s*$");
    static final Pattern TESTS_CLOSE = Pattern.compile("^\\s*// \u2500\u2500\u2500 end tests \u2500+\\s*$");
    static final Pattern TEST_CALL = Pattern.compile("^\\s*test\\(\"", Pattern.MULTILINE);
    static final Pattern DONE = Pattern.compile("#done passed=(\\d+) failed=(\\d+) todo=(\\d+)");
    static final String STUB = "throw new UnsupportedOperationException(\"TODO\")";
    static final String HOLE = "/* ___ */";
    static final long TIMEOUT_MS = 10_000;
    static final long SLOW_MS = 2_000;


    record Job(String module, String kind, String file, Path path) {}

    /** debug hunts: the starter is complete code carrying real bugs */
    static boolean debugModule(String module) {
        return module.contains("debug");
    }

    /** Parsons and fill-in drills: the starter has stubs or holes */
    static boolean scaffoldedModule(String module) {
        return module.contains("scaffold");
    }

    record Outcome(int passed, int failed, int todo, long ms, String broken, String detail) {}

    static final class Broken extends Exception {
        Broken(String m) { super(m); }
    }

    public static void main(String[] args) throws Exception {
        System.setOut(new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out), true, StandardCharsets.UTF_8));
        var root = findRoot();
        if (root == null) {
            System.err.println("run me from the repo root: java bootcamp/Verify.java");
            System.exit(1);
        }
        var filter = args.length > 0 ? args[0] : null;
        var started = System.nanoTime();
        var lib = root.resolve("_lib").resolve("out");
        var work = root.getParent().resolve(".verify-out");
        deleteTree(work);

        // 1 | the harness itself
        var compiler = ToolProvider.getSystemJavaCompiler();
        var harness = new ArrayList<String>(List.of("--release", "17", "-encoding", "UTF-8", "-Xlint:all", "-Werror", "-d", lib.toString()));
        try (var s = Files.list(root.resolve("_lib").resolve("check").resolve("bootcamp"))) {
            s.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> harness.add(p.toString()));
        }
        var err = new ByteArrayOutputStream();
        if (compiler.run(null, null, err, harness.toArray(String[]::new)) != 0) {
            System.out.println("the harness does not compile:\n" + err.toString(StandardCharsets.UTF_8));
            System.exit(1);
        }

        // 2 | collect the files
        var jobs = new ArrayList<Job>();
        List<Path> modules;
        try (var s = Files.list(root)) {
            modules = s.filter(Files::isDirectory).sorted().collect(Collectors.toList());
        }
        var layoutProblems = new ArrayList<String>();
        for (var moduleDir : modules) {
            var module = moduleDir.getFileName().toString();
            if (module.startsWith("_") || module.startsWith(".")) continue;
            if (filter != null && !module.toLowerCase(Locale.ROOT).startsWith(filter.toLowerCase(Locale.ROOT))) continue;
            if (!MODULE.matcher(module).matches()) {
                layoutProblems.add(module + ": directory name is not NN-kebab-case");
                continue;
            }
            if (!Files.exists(moduleDir.resolve("README.md"))) layoutProblems.add(module + ": no README.md");
            var ex = list(moduleDir.resolve("exercises"));
            var sol = list(moduleDir.resolve("solutions"));
            for (var f : ex) if (!sol.contains(f)) layoutProblems.add(module + ": solutions/" + f + " is missing");
            for (var f : sol) if (!ex.contains(f)) layoutProblems.add(module + ": exercises/" + f + " is missing");
            if (ex.isEmpty()) layoutProblems.add(module + ": no exercises");
            for (var kind : List.of("exercises", "solutions")) {
                for (var f : kind.equals("exercises") ? ex : sol) {
                    jobs.add(new Job(module, kind, f, moduleDir.resolve(kind).resolve(f)));
                }
            }
        }
        if (jobs.isEmpty()) {
            System.err.println("no exercise files found under " + root + (filter == null ? "" : " matching '" + filter + "'"));
            System.exit(1);
        }

        System.out.println("compiling and running " + jobs.size() + " files (parallel 2)\u2026");

        // 3 | static checks + compile (sequential, in-process), then run (2 at a time)
        var outcomes = new Outcome[jobs.size()];
        var classDirs = new Path[jobs.size()];
        for (int i = 0; i < jobs.size(); i++) {
            var job = jobs.get(i);
            try {
                staticChecks(job);
                var out = work.resolve(job.module()).resolve(job.kind()).resolve(job.file().replace(".java", ""));
                Files.createDirectories(out);
                var cerr = new ByteArrayOutputStream();
                int rc = compiler.run(null, null, cerr, "--release", "17", "-encoding", "UTF-8",
                    "-cp", lib.toString(), "-d", out.toString(), job.path().toString());
                if (rc != 0) throw new Broken("does not compile\n" + cerr.toString(StandardCharsets.UTF_8));
                classDirs[i] = out;
            } catch (Broken b) {
                var msg = b.getMessage();
                var nl = msg.indexOf('\n');
                outcomes[i] = new Outcome(0, 0, 0, 0, nl < 0 ? msg : msg.substring(0, nl), nl < 0 ? "" : msg.substring(nl + 1));
            }
        }

        ExecutorService pool = Executors.newFixedThreadPool(2);
        var futures = new ArrayList<Future<Outcome>>();
        for (int i = 0; i < jobs.size(); i++) {
            final int k = i;
            futures.add(outcomes[k] != null ? null : pool.submit(() -> run(jobs.get(k), lib, classDirs[k], work)));
        }
        for (int i = 0; i < jobs.size(); i++) {
            if (futures.get(i) != null) outcomes[i] = futures.get(i).get();
        }
        pool.shutdown();

        // 4 | the report
        String lastModule = null;
        int broken = 0, tests = 0, slow = 0;
        var census = new java.util.LinkedHashMap<String, Integer>();
        for (int i = 0; i < jobs.size(); i++) {
            var job = jobs.get(i);
            var o = outcomes[i];
            if (!job.module().equals(lastModule)) {
                System.out.println();
                System.out.println(job.module());
                lastModule = job.module();
            }
            if (job.kind().equals("exercises")) census.merge(job.module(), 1, Integer::sum);
            tests += o.passed() + o.failed() + o.todo();
            var timing = o.ms() > SLOW_MS ? "  (slow: " + o.ms() + "ms)" : "";
            if (o.ms() > SLOW_MS) slow++;
            if (o.broken() == null) {
                System.out.println("  \u2714 " + job.kind() + "/" + job.file() + "  ok" + timing);
            } else {
                broken++;
                System.out.println("  \u2718 " + job.kind() + "/" + job.file() + "  BROKEN \u2014 " + o.broken() + timing);
                var lines = o.detail().split("\n");
                for (int j = Math.max(0, lines.length - 12); j < lines.length; j++) {
                    if (!lines[j].isBlank()) System.out.println("      \u2502 " + lines[j].stripTrailing());
                }
            }
        }
        for (var p : layoutProblems) {
            broken++;
            System.out.println("  \u2718 layout: " + p);
        }
        deleteTree(work);

        var secs = (System.nanoTime() - started) / 1e9;
        System.out.println();
        System.out.println("census: " + census.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(" \u00b7 ")) + "  (" + census.values().stream().mapToInt(Integer::intValue).sum() + " exercises)");
        System.out.println(String.format(Locale.ROOT, "%d files \u00b7 %d tests \u00b7 %d broken \u00b7 %d slow \u00b7 %.1fs",
            jobs.size(), tests, broken, slow, secs));
        System.exit(broken > 0 ? 1 : 0);
    }

    // -- static checks -----------------------------------------------------

    static void staticChecks(Job job) throws Broken, IOException {
        var bytes = Files.readAllBytes(job.path());
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xEF && (bytes[1] & 0xff) == 0xBB) throw new Broken("starts with a BOM");
        var src = new String(bytes, StandardCharsets.UTF_8);
        if (src.indexOf('\r') >= 0) throw new Broken("has CR characters (LF only)");
        if (src.indexOf('\t') >= 0) throw new Broken("has tab characters (spaces only)");

        for (var line : src.split("\n", -1)) {
            if (!line.stripLeading().startsWith("//") && !line.chars().allMatch(ch -> ch < 128)) {
                throw new Broken("non-ASCII outside a comment (use \\uXXXX escapes): " + line.strip());
            }
        }
        var header = parseHeader(src, job.file());
        if (!job.file().startsWith(header.number() + "-")) throw new Broken("header number " + header.number() + " does not match the filename");
        var solution = job.kind().equals("solutions");
        if (solution != header.rawTitleIsSolution) throw new Broken(solution ? "solution title does not end \" \u2014 SOLUTION\"" : "exercise title ends \" \u2014 SOLUTION\"");
        if (!List.of("\u2605\u2606\u2606 warm-up", "\u2605\u2605\u2606 core", "\u2605\u2605\u2605 stretch").contains(stars(header.stars()) + " " + header.tier())) {
            throw new Broken("difficulty must be one of \u2605\u2606\u2606 warm-up / \u2605\u2605\u2606 core / \u2605\u2605\u2605 stretch");
        }
        for (var line : src.split("\n", -1)) {
            if (line.startsWith("//") && line.length() > 78) throw new Broken("comment line over 78 chars: " + line.substring(0, 40) + "\u2026");
            if (!line.startsWith("//")) break;
        }
        var split = splitTests(src);
        int count = countMatches(TEST_CALL, split[1]);
        if (count < 4 || count > 9) throw new Broken(count + " test(...) calls (want 4\u20139)");
        if (!split[1].contains("System.exit(report());")) throw new Broken("tests block does not end with System.exit(report());");
        if (!split[0].contains("class Exercise")) throw new Broken("no `class Exercise` before the tests");

        if (solution) {
            if (!header.body().startsWith("Walkthrough:")) throw new Broken("solution has no Walkthrough: block");
            if (src.contains(STUB)) throw new Broken("solution still contains a TODO stub");
            if (src.contains(HOLE)) throw new Broken("solution still contains a `" + HOLE + "` hole");
            var exPath = job.path().getParent().getParent().resolve("exercises").resolve(job.file());
            if (Files.exists(exPath)) {
                var exSrc = Files.readString(exPath, StandardCharsets.UTF_8);
                var exHeader = parseHeader(exSrc, "exercise");
                if (!exHeader.same(header)) throw new Broken("header (number/title/stars/tier/concepts) differs from the exercise");
                if (!splitTests(exSrc)[1].equals(split[1])) throw new Broken("tests block is not byte-identical to the exercise's");
            }
        } else {
            if (scaffoldedModule(job.module())) {
                if (!src.contains(STUB) && !src.contains(HOLE)) {
                    throw new Broken("scaffolded exercise has no `" + STUB + "` stub and no `" + HOLE + "` hole");
                }
            } else if (debugModule(job.module())) {
                if (src.contains(STUB)) throw new Broken("a debug-hunt starter is complete code but contains a `" + STUB + "` stub");
                if (src.contains(HOLE)) throw new Broken("a debug-hunt starter is complete code but contains a `" + HOLE + "` hole");
            } else {
                if (!src.contains(STUB)) throw new Broken("exercise has no `" + STUB + "` stub");
            }
            if (!header.body().contains("\u2192")) throw new Broken("prompt has no \u2192 example");
            if (!header.body().contains("hint:")) throw new Broken("prompt has no hint: line");
        }
    }

    static String stars(int n) {
        return "\u2605".repeat(n) + "\u2606".repeat(3 - n);
    }

    /** [head, tests block (open marker line through close marker line), tail] */
    static String[] splitTests(String src) throws Broken {
        var lines = src.split("\n", -1);
        int open = -1, close = -1;
        for (int i = 0; i < lines.length; i++) {
            if (TESTS_OPEN.matcher(lines[i]).matches()) {
                if (open >= 0) throw new Broken("two `// \u2500\u2500\u2500 tests \u2500\u2500\u2500` markers");
                open = i;
            } else if (TESTS_CLOSE.matcher(lines[i]).matches()) {
                if (close >= 0) throw new Broken("two `// \u2500\u2500\u2500 end tests \u2500\u2500\u2500` markers");
                close = i;
            }
        }
        if (open < 0 || close < 0 || close < open) throw new Broken("no `// \u2500\u2500\u2500 tests \u2500\u2500\u2500` \u2026 `// \u2500\u2500\u2500 end tests \u2500\u2500\u2500` block");
        var nl = "\n";
        return new String[] {
            String.join(nl, java.util.Arrays.asList(lines).subList(0, open)) + nl,
            String.join(nl, java.util.Arrays.asList(lines).subList(open, close + 1)) + nl,
            String.join(nl, java.util.Arrays.asList(lines).subList(close + 1, lines.length)),
        };
    }

    static final class ParsedHeader {
        final String number, title, tier, body;
        final int stars;
        final List<String> concepts;
        final boolean rawTitleIsSolution;

        ParsedHeader(String number, String rawTitle, int stars, String tier, List<String> concepts, String body) {
            this.number = number;
            this.rawTitleIsSolution = Pattern.compile("\\s+\u2014\\s+SOLUTION$").matcher(rawTitle).find();
            this.title = rawTitle.replaceAll("\\s+\u2014\\s+SOLUTION$", "").trim();
            this.stars = stars;
            this.tier = tier;
            this.concepts = concepts;
            this.body = body;
        }

        String number() { return number; }
        int stars() { return stars; }
        String tier() { return tier; }
        String body() { return body; }

        boolean same(ParsedHeader o) {
            return number.equals(o.number) && title.equals(o.title) && stars == o.stars
                && tier.equals(o.tier) && concepts.equals(o.concepts);
        }
    }

    /** A line-for-line port of csbootcamp's parseHeader (tools/seed/src/csbootcamp/source.ts). */
    static ParsedHeader parseHeader(String src, String label) throws Broken {
        var lines = src.split("\n", -1);
        if (lines.length < 5 || !RULE.matcher(lines[0]).matches()) throw new Broken(label + ": line 1 is not the header rule");
        Matcher m = TITLE.matcher(lines[1]);
        if (!m.matches()) throw new Broken(label + ": line 2 is not \"NN \u00b7 name   \u2605\u2605\u2606 tier\"");
        Matcher c = CONCEPTS.matcher(lines[2]);
        if (!c.matches()) throw new Broken(label + ": line 3 is not a concepts: line");
        if (!RUN.matcher(lines[3]).find()) throw new Broken(label + ": line 4 is not a run: line");
        if (!RULE.matcher(lines[4]).matches()) throw new Broken(label + ": line 5 is not the closing header rule");
        var body = new ArrayList<String>();
        for (int i = 5; i < lines.length; i++) {
            var l = lines[i];
            if (!l.startsWith("//")) break;
            body.add(l.replaceFirst("^// ?( ?)", ""));
        }
        while (!body.isEmpty() && body.get(0).isBlank()) body.remove(0);
        while (!body.isEmpty() && body.get(body.size() - 1).isBlank()) body.remove(body.size() - 1);
        if (body.isEmpty()) throw new Broken(label + ": empty prompt/walkthrough");
        int stars = (int) m.group(3).chars().filter(ch -> ch == '\u2605').count();
        var concepts = Stream.of(c.group(1).split("\u00b7")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        return new ParsedHeader(m.group(1), m.group(2), stars, m.group(4), concepts, String.join("\n", body));
    }

    static int countMatches(Pattern p, String s) {
        int n = 0;
        var m = p.matcher(s);
        while (m.find()) n++;
        return n;
    }

    // -- running a file through BqMain -------------------------------------

    static Outcome run(Job job, Path lib, Path classes, Path work) {
        long t0 = System.nanoTime();
        try {
            var dir = Files.createTempDirectory(work, "run");
            var nonceFile = dir.resolve("nonce");
            var resultFile = dir.resolve("result");
            var nonce = UUID.randomUUID().toString().replace("-", "");
            Files.writeString(nonceFile, nonce, StandardCharsets.UTF_8);
            var java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
            var pb = new ProcessBuilder(java, "-Xshare:auto", "-XX:TieredStopAtLevel=1",
                "-Dbq.nonce.file=" + nonceFile, "-Dbq.result.file=" + resultFile,
                "-cp", lib + File.pathSeparator + classes, "bootcamp.BqMain", "Exercise");
            pb.directory(job.path().getParent().toFile());
            pb.environment().put("NO_COLOR", "1");
            pb.redirectErrorStream(true);
            var p = pb.start();
            var outBuf = new ByteArrayOutputStream();
            var reader = new Thread(() -> copy(p.getInputStream(), outBuf));
            reader.start();
            boolean done = p.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!done) p.destroyForcibly();
            reader.join(2000);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            var stdout = outBuf.toString(StandardCharsets.UTF_8);
            if (!done) return new Outcome(0, 0, 0, ms, "timed out after " + TIMEOUT_MS + "ms", stdout);
            if (Files.exists(nonceFile)) return new Outcome(0, 0, 0, ms, "BqMain did not delete the nonce file", stdout);

            var done2 = DONE.matcher(stdout);
            if (!done2.find()) return new Outcome(0, 0, 0, ms, "crashed (no #done line)", stdout);
            int passed = Integer.parseInt(done2.group(1)), failed = Integer.parseInt(done2.group(2)), todo = Integer.parseInt(done2.group(3));

            if (!Files.exists(resultFile)) return new Outcome(passed, failed, todo, ms, "no result file (grading channel broken)", stdout);
            var line = Files.readString(resultFile, StandardCharsets.UTF_8).trim().split(" ");
            if (line.length != 3 || !line[0].equals("#bq-result") || !line[1].equals(nonce)) {
                return new Outcome(passed, failed, todo, ms, "result file has the wrong shape or nonce", stdout);
            }
            var json = new String(Base64.getDecoder().decode(line[2]), StandardCharsets.UTF_8);
            var expect = "{\"passed\":" + passed + ",\"failed\":" + failed + ",\"todo\":" + todo + ",";
            if (!json.startsWith(expect)) return new Outcome(passed, failed, todo, ms, "result file disagrees with #done", json);

            int total = passed + failed + todo;
            String broken = null;
            if (total == 0) broken = "no tests ran";
            else if (job.kind().equals("solutions") && failed > 0) broken = failed + " failing test(s)";
            else if (job.kind().equals("solutions") && todo > 0) broken = todo + " TODO left in a solution";
            else if (job.kind().equals("exercises") && debugModule(job.module())) {
                if (failed == 0) broken = "a debug-hunt starter must fail at least one test (no bug a test can see)";
                else if (todo > 0) broken = todo + " todo in a debug-hunt starter (it is complete code: no test may reach a TODO)";
            }
            // scaffolded starters fall through: like any starter, every test must
            // reach a stub or a hole (todo), none may pass and none may fail
            else if (job.kind().equals("exercises") && failed > 0) broken = failed + " failing test(s) in a starter";
            else if (job.kind().equals("exercises") && passed > 0) broken = passed + " test(s) pass against the untouched stub";
            else if (job.kind().equals("exercises") && todo == 0) broken = "nothing left to do \u2014 is the stub missing?";
            return new Outcome(passed, failed, todo, ms, broken, broken == null ? "" : stdout);
        } catch (Exception e) {
            return new Outcome(0, 0, 0, (System.nanoTime() - t0) / 1_000_000, "verify could not run it: " + e, "");
        }
    }

    // -- helpers -----------------------------------------------------------

    static void copy(InputStream in, ByteArrayOutputStream out) {
        try (in) {
            in.transferTo(out);
        } catch (IOException ignored) {
            // process killed
        }
    }

    static List<String> list(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return List.of();
        try (var s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString())
                .filter(f -> f.endsWith(".java") && !f.startsWith("_"))
                .sorted().collect(Collectors.toList());
        }
    }

    static Path findRoot() {
        var dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve("_lib").resolve("check"))) return dir;
            if (Files.isDirectory(dir.resolve("bootcamp").resolve("_lib").resolve("check"))) return dir.resolve("bootcamp");
            dir = dir.getParent();
        }
        return null;
    }

    static void deleteTree(Path p) throws IOException {
        if (!Files.exists(p)) return;
        try (var s = Files.walk(p)) {
            for (var f : s.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) Files.deleteIfExists(f);
        }
    }
}
