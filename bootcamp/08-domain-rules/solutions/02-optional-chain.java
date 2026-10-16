// ─────────────────────────────────────────────────────────────────────────
//  02 · member, loan, due date — SOLUTION                   ★★☆ core
//  concepts: Optional chains · flatMap · filter · map lookups
//  run: java -cp ../../_lib/out 02-optional-chain.java
// ─────────────────────────────────────────────────────────────────────────
//
//  Walkthrough:
//  The null-check staircase — if member != null, if member.loanId() !=
//  null, if loan != null — is what Optional chains flatten. Start with
//  Optional.ofNullable(members.get(id)) (Map.get returns null for a
//  missing key), then for each further step that may be missing,
//  flatMap into another ofNullable: the loan id field, then the loans map
//  lookup. A step that cannot be missing (Loan::due) is a plain map.
//  If ANY step comes up empty, every later step is skipped and the result
//  is empty — no branch per failure, no NullPointerException.
//  dueText ends the chain with map(d -> "due " + d).orElse(...). LocalDate
//  toString is ISO (2024-03-01), so no formatter is needed.
//  isOverdue uses filter: keep the date only if today.isAfter(due), then
//  isPresent(). It reads as the rule: "there is a due date, and it has
//  passed". A member with no loan is not overdue — the empty Optional
//  falls straight through to false.
//  Chains get unreadable past four or five steps; that is the cue to
//  extract a named method (loanOf(member)) rather than nest lambdas.

import static bootcamp.Check.*;

import java.time.LocalDate;
import java.util.*;

class Exercise {

    record Member(String id, String loanId) {}

    record Loan(String id, LocalDate due) {}

    static Optional<LocalDate> dueDateFor(String memberId, Map<String, Member> members, Map<String, Loan> loans) {
        return Optional.ofNullable(members.get(memberId))
            .flatMap(m -> Optional.ofNullable(m.loanId()))
            .flatMap(loanId -> Optional.ofNullable(loans.get(loanId)))
            .map(Loan::due);
    }

    static String dueText(String memberId, Map<String, Member> members, Map<String, Loan> loans) {
        return dueDateFor(memberId, members, loans).map(d -> "due " + d).orElse("no active loan");
    }

    static boolean isOverdue(String memberId, Map<String, Member> members, Map<String, Loan> loans, LocalDate today) {
        return dueDateFor(memberId, members, loans).filter(today::isAfter).isPresent();
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
