# Flight plan

Thirteen modules, 104 exercises. At roughly 10–20 minutes per warm-up,
20–40 per core exercise and up to an hour per stretch, blocks 1–4
(modules 01–08) are about 25–30 focused hours. Block 5, system design
(modules 09–13), is denser: budget 30–45 minutes per core exercise and
60–90 per stretch, about 25–30 more hours. The whole bootcamp is about
50–60 focused hours. Do it in this order.

## Every session

1. Read the module `README.md` (10 minutes). Don't skip "The details
   that bite" — every exercise has one of them as its trap.
2. Open the next exercise in `exercises/`. Read the prompt and the TESTS
   before writing anything: the test names are the spec.
3. Run it (`java -cp ../../_lib/out NN-name.java`). Write the smallest
   thing that turns one `☐` into a `✔`. Repeat.
4. When it's all green, open the same file in `solutions/` and read the
   `Walkthrough:`. If your solution differs, decide which you'd rather
   maintain, and why.
5. Stuck for more than 15 minutes? Re-read the `hint:` line, then the
   module README, then the solution's walkthrough — the walkthrough
   explains the idea without forcing you to read the code.

## The route

| block | modules | why this order |
| --- | --- | --- |
| 1 · foundations | 01 language core, 02 methods and lambdas | every later file uses these without comment |
| 2 · data | 03 collections and streams, 04 strings and time | what a CLI app holds, reads and prints |
| 3 · design | 05 classes/records/enums, 06 exceptions and files | turning scripts into types that refuse bad states |
| 4 · business | 07 money and validation, 08 domain rules | the rules that make a to-do list, a bank or a shop correct |
| 5 · system design | 09 caching, 10 rate limits and resilience, 11 partitioning and ids, 12 messaging and events, 13 storage and consistency | the building blocks of real services, each as a small model you can test |

Within a module: warm-ups (★☆☆) and core (★★☆) in order; stretch (★★★)
when you have time and energy. A stretch exercise left for a second pass
is fine; a skipped README is not.

Block 5 assumes blocks 1–4, especially injected clocks (04), invariants
and idempotency (08). Read
[`astraupskill/03-SYSTEM-DESIGN.md`](astraupskill/03-SYSTEM-DESIGN.md)
once before module 09 for the map of where each block is going, and
again after module 13.

## When you're done

Pick one of your own CLI projects and apply one lesson from each module:
`BigDecimal` for its money, a `Clock` for its "today", an atomic save for
its data file, a state machine for its statuses, a validator that reports
every problem. That is the real exam.

For block 5 the exam is a whiteboard: take a design from
`astraupskill/03-SYSTEM-DESIGN.md` you have not drawn before, run the
eight steps against a 45-minute timer, and label every box with the
exercise that built it.
