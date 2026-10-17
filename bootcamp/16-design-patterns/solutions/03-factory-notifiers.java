// ─────────────────────────────────────────────────────────────────────────
//  03 · factory: notification channels — SOLUTION           ★☆☆ warm-up
//  concepts: static factory methods · factory registry · validation
//  run: java -cp ../../_lib/out 03-factory-notifiers.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  Two meanings of "factory" meet here. A STATIC FACTORY METHOD
//  (email(...), like List.of or Duration.ofSeconds) is a named
//  constructor: it can validate, it can return any subtype, and its name
//  says what you get. A FACTORY REGISTRY (create(uri)) picks WHICH class
//  to build from data, so callers never write `new SlackNotifier(...)`
//  and never import it; they hold a Notifier.
//  That indirection is what makes the registry open for extension:
//  register("test", ...) adds a channel without editing create(). This is
//  the shape of JDBC's DriverManager ("jdbc:postgresql:...") and of
//  java.util.ServiceLoader.
//  The details that matter: lower-case the scheme both when registering
//  and when looking up, or "EMAIL:" and "email:" disagree and a duplicate
//  slips in. Split at the FIRST colon only, because targets may contain
//  colons themselves. An unknown scheme should say what IS known; that
//  error message saves the next person a trip to the source.
//  Validation lives in the static factories, so a bad address fails the
//  same way whether it came through create() or a direct call.

import static bootcamp.Check.*;

import java.util.*;
import java.util.function.*;

class Exercise {

    interface Notifier {
        String send(String message);
    }

    record EmailNotifier(String address) implements Notifier {
        public String send(String message) { return "email to " + address + ": " + message; }
    }

    record SmsNotifier(String number) implements Notifier {
        public String send(String message) { return "sms to " + number + ": " + message; }
    }

    record SlackNotifier(String channel) implements Notifier {
        public String send(String message) { return "slack " + channel + ": " + message; }
    }

    static Notifier email(String address) {
        int at = address.indexOf('@');
        if (at <= 0 || at != address.lastIndexOf('@') || at == address.length() - 1) throw new IllegalArgumentException("bad email: " + address);
        return new EmailNotifier(address);
    }

    static Notifier sms(String number) {
        if (!number.matches("\\+\\d{7,15}")) throw new IllegalArgumentException("bad sms: " + number);
        return new SmsNotifier(number);
    }

    static Notifier slack(String channel) {
        if (!channel.matches("#[a-z0-9-]+")) throw new IllegalArgumentException("bad slack: " + channel);
        return new SlackNotifier(channel);
    }

    static final class NotifierFactory {
        private final Map<String, Function<String, Notifier>> makers = new TreeMap<>();

        void register(String scheme, Function<String, Notifier> maker) {
            var key = scheme.toLowerCase(Locale.ROOT);
            if (makers.containsKey(key)) throw new IllegalStateException("scheme already registered: " + key);
            makers.put(key, maker);
        }

        Notifier create(String uri) {
            int colon = uri.indexOf(':');
            if (colon < 0) throw new IllegalArgumentException("not a notifier uri: " + uri);
            var scheme = uri.substring(0, colon).toLowerCase(Locale.ROOT);
            var maker = makers.get(scheme);
            if (maker == null) {
                throw new IllegalArgumentException("unknown scheme '" + scheme + "'; known: " + String.join(", ", makers.keySet()));
            }
            return maker.apply(uri.substring(colon + 1));
        }

        static NotifierFactory withDefaults() {
            var f = new NotifierFactory();
            f.register("email", Exercise::email);
            f.register("sms", Exercise::sms);
            f.register("slack", Exercise::slack);
            return f;
        }
    }

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("email accepts one @ with text on both sides", () -> {
            eq(email("ana@example.org"), new EmailNotifier("ana@example.org"));
            var e = raises(IllegalArgumentException.class, () -> email("ana"));
            eq(e.getMessage(), "bad email: ana");
            raises(IllegalArgumentException.class, () -> email("a@b@c"));
            raises(IllegalArgumentException.class, () -> email("@example.org"));
            raises(IllegalArgumentException.class, () -> email("ana@"));
        });
        test("sms needs + and 7 to 15 digits; slack needs # and a lower-case name", () -> {
            eq(sms("+4712345678"), new SmsNotifier("+4712345678"));
            raises(IllegalArgumentException.class, () -> sms("4712345678"));
            raises(IllegalArgumentException.class, () -> sms("+123456"));
            eq(slack("#ops-alerts"), new SlackNotifier("#ops-alerts"));
            raises(IllegalArgumentException.class, () -> slack("#Ops"));
            raises(IllegalArgumentException.class, () -> slack("#"));
        });
        test("create picks the maker by scheme, case-insensitively", () -> {
            var f = NotifierFactory.withDefaults();
            eq(f.create("EMAIL:ana@x.org"), new EmailNotifier("ana@x.org"));
            eq(f.create("SLACK:#ops"), new SlackNotifier("#ops"));
            eq(f.create("sms:+4712345678"), new SmsNotifier("+4712345678"));
        });
        test("create passes everything after the FIRST colon to the maker", () -> {
            var f = new NotifierFactory();
            f.register("echo", target -> message -> target + " <- " + message);
            eq(f.create("echo:a:b").send("hi"), "a:b <- hi");
        });
        test("an unknown scheme names the known ones, sorted", () -> {
            var f = NotifierFactory.withDefaults();
            var e = raises(IllegalArgumentException.class, () -> f.create("fax:+4712345678"));
            eq(e.getMessage(), "unknown scheme 'fax'; known: email, slack, sms");
        });
        test("a uri without a colon is rejected, and bad targets still fail", () -> {
            var f = NotifierFactory.withDefaults();
            var e = raises(IllegalArgumentException.class, () -> f.create("ana@x.org"));
            eq(e.getMessage(), "not a notifier uri: ana@x.org");
            raises(IllegalArgumentException.class, () -> f.create("sms:12"));
        });
        test("registering a scheme twice is an error, in any letter case", () -> {
            var f = NotifierFactory.withDefaults();
            raises(IllegalStateException.class, () -> f.register("Email", Exercise::email));
        });
        test("callers see only Notifier: send works the same for every channel", () -> {
            var f = NotifierFactory.withDefaults();
            var sent = new ArrayList<String>();
            for (var uri : List.of("email:ops@x.org", "sms:+4799999999", "slack:#oncall")) sent.add(f.create(uri).send("disk full"));
            eq(sent, List.of("email to ops@x.org: disk full", "sms to +4799999999: disk full", "slack #oncall: disk full"));
        });
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
