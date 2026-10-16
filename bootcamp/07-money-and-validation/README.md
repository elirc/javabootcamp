# 07 · Money and Validation

Half of the owner's beginner Java projects handle money — a bank
simulator, an expense tracker, an order system — and every one of them
started with `double balance`. This module is the fix: `BigDecimal` for
amounts, a named rounding rule for every division, allocation that never
loses a cent, and validation that tells a user everything wrong with their
input in one go.

## The mental model

### 1 · `double` is binary; money is decimal

```java
0.1 + 0.2                                      // 0.30000000000000004
new BigDecimal("0.1").add(new BigDecimal("0.2"))   // 0.3, exactly
new BigDecimal(0.1)        // 0.1000000000000000055511151231257827…
BigDecimal.valueOf(0.1)    // 0.1   (if a double is all you have)
```

A `BigDecimal` is an unscaled integer plus a scale: `12345` scale `2` is
`123.45`. It is immutable — `total.add(x)` returns a new value.

### 2 · Every rounding is a named business rule

```java
x.setScale(2, RoundingMode.HALF_UP)      // invoices: 0.005 → 0.01
x.setScale(2, RoundingMode.HALF_EVEN)    // banking: 2.345 → 2.34, 2.355 → 2.36
x.setScale(2, RoundingMode.UNNECESSARY)  // "must already fit" — throws otherwise
a.divide(b, 2, RoundingMode.HALF_UP)     // divide ALWAYS takes a scale
```

### 3 · `equals` sees scale, `compareTo` doesn't

`new BigDecimal("2.50").equals(new BigDecimal("2.5"))` is **false**;
`compareTo` says `0`. Normalise the scale in your Money type's
constructor and both agree.

### 4 · Validate the shape, then the rules — and report all of it

```java
if (!text.matches("-?\\d+(\\.\\d+)?")) return List.of("not a number");
var errors = new ArrayList<String>();
if (amount.signum() <= 0) errors.add("must be positive");
if (amount.stripTrailingZeros().scale() > 2) errors.add("at most 2 decimals");
```

## The details that bite

1. **`new BigDecimal(double)`** captures binary error. Use the `String`
   constructor or `BigDecimal.valueOf`.
2. **`divide` without a scale throws** `ArithmeticException` for
   10 / 3.
3. **Rounding each share doesn't add up.** Allocate whole cents and hand
   out the remainder.
4. **Round at the steps a receipt shows, not in between** — and round
   once per step.
5. **`toString()` can print `1E+3`.** Display with `toPlainString()`.
6. **`new BigDecimal("1e3")` parses.** Shape-check user input with a regex
   first.
7. **Order of operations is policy**: discount, coupon, tax — in the
   order the business states, with a clamp at zero.
8. **Interest compounds on the rounded balance**, month by month, the way
   the ledger does — not by the textbook formula.

## Cheat table

| want | write |
| --- | --- |
| from text | `new BigDecimal("12.50")` |
| from cents | `BigDecimal.valueOf(1250, 2)` |
| to cents | `x.movePointRight(2).longValueExact()` |
| percent of | `x.multiply(rate).movePointLeft(2)` |
| negative? | `x.signum() < 0` |
| clamp at zero | `x.max(BigDecimal.ZERO)` |
| same amount? | `a.compareTo(b) == 0` |

## Exercises

| # | file | ★ | what you build |
| --- | --- | --- | --- |
| 01 | `01-bigdecimal-basics.java` | ★☆☆ | exact sums, valueOf vs new, compareTo vs equals |
| 02 | `02-rounding-modes.java` | ★★☆ | tax with HALF_UP, banker's rounding, safe division |
| 03 | `03-split-the-bill.java` | ★★☆ | a split whose parts always add up to the total |
| 04 | `04-money-record.java` | ★★☆ | a Money record with normalised scale and currency checks |
| 05 | `05-validate-amount.java` | ★☆☆ | a deposit validator that lists every problem |
| 06 | `06-compound-interest.java` | ★★★ | month-by-month interest on the rounded balance |
| 07 | `07-checkout-total.java` | ★★☆ | discount → coupon → tax, with guards and clamps |
| 08 | `08-ledger-statement.java` | ★★★ | a statement: apply, reject, and format a ledger |

Do the warm-ups and core in order. Stretch if time allows.

---

**Next:** `bootcamp/08-domain-rules`
