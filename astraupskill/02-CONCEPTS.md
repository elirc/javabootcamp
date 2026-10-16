# The ideas that keep coming back

Sixty-four exercises look like sixty-four topics. They are really seven
ideas, each met several times in different clothes. Recognising the idea
is what lets you avoid the bug in code that looks nothing like the
exercise.

## 1 · Identity is not equality

`==` on objects asks whether two references point at the same object.
Strings built at runtime, `Integer`s above 127, two `Isbn`s parsed from
differently formatted text — all equal in meaning, all different objects.
Equality has to be defined (`equals`), defined consistently with hashing
(`hashCode`), and sometimes defined more loosely than the default:
`BigDecimal.equals` treats 2.50 and 2.5 as different because their scale
differs, so "same amount" is `compareTo`. Whenever you compare, ask which
of the three you mean: same object, same value, or same value after
normalising.

## 2 · Operand types decide, not the destination

`double avg = sum / count` divides integers before the double ever sees
the result. `long seconds = days * 86_400` overflows in `int` before
widening. A percentage computed as `part * 100 / whole` wraps negative for
large counts. Java evaluates an expression from its operands outward, so
the fix is always to change an operand (`(double) sum`, `86_400L`,
`100.0 * part`), never the variable you assign to.

## 3 · Hidden inputs make code untestable

`LocalDate.now()`, the default locale, the default charset, the platform
line separator and the file-system separator are all inputs your method
reads without declaring. Each one makes the same program behave
differently on another machine, another day, or in another country.
The cure is the same every time: make the input explicit. Pass a `Clock`,
pass `Locale.ROOT`, pass `StandardCharsets.UTF_8`, write `"\n"` yourself,
join path names with `/`. Then a test can pin the input and the result
stops moving.

## 4 · Check, then act

A withdrawal that subtracts first and validates second leaves a damaged
balance when it throws. An order reservation that takes stock line by
line leaves half an order reserved when the third line is short. A save
that writes straight into the target leaves half a file when the disk
fills. Each is the same mistake: changing state before knowing the whole
change is allowed. Validate everything first; then apply changes in a
step that cannot fail — a second pass, an atomic move, a single
assignment at the end.

## 5 · Copies, views and ownership

`final` stops a field being reassigned, not the list it points at being
changed. A constructor that stores the caller's list lets the caller
change your object later; a getter that returns your internal list lets
any reader change it now. Own your state: copy on the way in
(`List.copyOf`), return unmodifiable views or copies on the way out, and
prefer immutable values (records, `LocalDate`, `BigDecimal`) that can be
shared freely because nobody can change them.

## 6 · Name the rule, then encode it once

Rounding mode, transition table, validation rule, discount order: each is
a business decision. Code that leaves the decision implicit
(`setScale(2)` with no mode, `if` chains scattered across classes) hides
it where nobody can review it. Code that names it — `RoundingMode.HALF_EVEN`,
a `next()` table in the enum, a `Map<String, Predicate<T>>` of named
rules — puts it in one place where the compiler and the reader can both
check it. Switch expressions over your own enums with no `default` arm
turn "we added a state and forgot a case" into a compile error.

## 7 · Failure is information

A good exception says what was expected, what was received and where,
and keeps its cause. A checked exception tells every caller that a
decline or a missing file is part of normal life. A suppressed exception
keeps the second failure without hiding the first. A batch import
reports every bad line with its number instead of stopping at the first.
An idempotent operation reports "duplicate" instead of silently charging
twice. In each case the program does not just survive the failure; it
tells the next person exactly what happened.
