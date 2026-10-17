// ─────────────────────────────────────────────────────────────────────────
//  03 · factory: notification channels                      ★☆☆ warm-up
//  concepts: static factory methods · factory registry · validation
//  run: java -cp ../../_lib/out 03-factory-notifiers.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Alerts go out by email, SMS or Slack, chosen by a uri such as
//  "sms:+4712345678"; callers only see Notifier. Finish the static
//  factories, which return the record or throw IllegalArgumentException
//  "bad <kind>: <value>" (kind is email, sms or slack):
//    email(address)  exactly one '@', with text on both sides
//    sms(number)     '+' then 7 to 15 digits
//    slack(channel)  '#' then 1+ lower-case letters, digits or '-'
//  and NotifierFactory, which maps a case-insensitive scheme to a maker:
//    register(s, maker)  a scheme registered twice → IllegalStateException
//    create(uri)         split at the FIRST ':'. No ':' → IAE "not a
//                        notifier uri: <uri>"; unknown scheme → IAE
//                        "unknown scheme '<s>'; known: a, b, c" (sorted)
//    withDefaults()      a factory with email, sms and slack
//  (IAE = IllegalArgumentException)
//
//      create("SLACK:#ops") → new SlackNotifier("#ops")
//
//  hint: a TreeMap's keys come out sorted.

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
        throw new UnsupportedOperationException("TODO");
    }

    static Notifier sms(String number) {
        throw new UnsupportedOperationException("TODO");
    }

    static Notifier slack(String channel) {
        throw new UnsupportedOperationException("TODO");
    }

    static final class NotifierFactory {
        private final Map<String, Function<String, Notifier>> makers = new TreeMap<>();

        void register(String scheme, Function<String, Notifier> maker) {
            throw new UnsupportedOperationException("TODO");
        }

        Notifier create(String uri) {
            throw new UnsupportedOperationException("TODO");
        }

        static NotifierFactory withDefaults() {
            throw new UnsupportedOperationException("TODO");
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
