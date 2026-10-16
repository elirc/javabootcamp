// ─────────────────────────────────────────────────────────────────────────
//  02 · member, loan, due date                              ★★☆ core
//  concepts: Optional chains · flatMap · filter · map lookups
//  run: java -cp ../../_lib/out 02-optional-chain.java
// ─────────────────────────────────────────────────────────────────────────
//
//  A library tracker answers "when is this member's book due?" by
//  following member → loan → due date through two maps. Any missing link
//  (unknown member, a member with no loan id, a loan id with no loan)
//  means there is no answer. Write dueDateFor, dueText ("due 2024-03-01"
//  or "no active loan") and isOverdue (a due date exists AND today is
//  after it). The records Member and Loan are above the tests.
//
//      dueDateFor("m1", members, loans)          → Optional[2024-03-01]
//      dueDateFor("m-unknown", members, loans)   → Optional.empty
//      dueText("m2", members, loans)             → "no active loan"
//      isOverdue("m1", ..., 2024-03-02)          → true
//
//  hint: each step that MAY be missing is a flatMap.

import static bootcamp.Check.*;

import java.time.LocalDate;
import java.util.*;

class Exercise {

    record Member(String id, String loanId) {}

    record Loan(String id, LocalDate due) {}

    static Optional<LocalDate> dueDateFor(String memberId, Map<String, Member> members, Map<String, Loan> loans) {
        throw new UnsupportedOperationException("TODO");
    }

    static String dueText(String memberId, Map<String, Member> members, Map<String, Loan> loans) {
        throw new UnsupportedOperationException("TODO");
    }

    static boolean isOverdue(String memberId, Map<String, Member> members, Map<String, Loan> loans, LocalDate today) {
        throw new UnsupportedOperationException("TODO");
    }

    static final Map<String, Member> MEMBERS = Map.of(
        "m1", new Member("m1", "L1"),
        "m2", new Member("m2", null),
        "m3", new Member("m3", "L-gone"));
    static final Map<String, Loan> LOANS = Map.of("L1", new Loan("L1", LocalDate.of(2024, 3, 1)));

    // ─── tests ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        test("follows member to loan to due date", () ->
            eq(dueDateFor("m1", MEMBERS, LOANS), Optional.of(LocalDate.of(2024, 3, 1))));
        test("an unknown member has no due date", () -> eq(dueDateFor("nobody", MEMBERS, LOANS), Optional.empty()));
        test("a member with no loan has no due date", () -> eq(dueDateFor("m2", MEMBERS, LOANS), Optional.empty()));
        test("a dangling loan id has no due date", () -> eq(dueDateFor("m3", MEMBERS, LOANS), Optional.empty()));
        test("dueText says when or says none", () -> {
            eq(dueText("m1", MEMBERS, LOANS), "due 2024-03-01");
            eq(dueText("m2", MEMBERS, LOANS), "no active loan");
        });
        test("overdue only after the due date", () -> {
            eq(isOverdue("m1", MEMBERS, LOANS, LocalDate.of(2024, 3, 1)), false);
            eq(isOverdue("m1", MEMBERS, LOANS, LocalDate.of(2024, 3, 2)), true);
        });
        test("no loan is never overdue", () -> eq(isOverdue("m2", MEMBERS, LOANS, LocalDate.of(2030, 1, 1)), false));
        System.exit(report());
    }
    // ─── end tests ───────────────────────────────────────────────────────
}
