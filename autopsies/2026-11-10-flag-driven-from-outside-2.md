# Autopsy 2026-11-10-flag-driven-from-outside-2 (flag-driven-from-outside)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `flag-driven-from-outside` that went away (written by cerco):

- `emulator/src/main/java/com/fpetrola/z80/cpu/OOZ80.java|com.fpetrola.z80.cpu.OOZ80.doInt()|state.setIff1(false)`
- `emulator/src/main/java/com/fpetrola/z80/cpu/OOZ80.java|com.fpetrola.z80.cpu.OOZ80.doInt()|state.setIff2(false)`
- `emulator/src/main/java/com/fpetrola/z80/cpu/OOZ80.java|com.fpetrola.z80.cpu.OOZ80.execute()|state.setActiveNMI(false)`
- `emulator/src/main/java/com/fpetrola/z80/cpu/OOZ80.java|com.fpetrola.z80.cpu.OOZ80.nmi()|state.setIff1(false)`

## Cause

Same fix as 2026-11-10-ask-then-tell, seen by the project-wide rule: OOZ80 switched State's interrupt flags off
with bare setters because State had no operation for the transitions. On a maskable interrupt it wrote
`setIff1(false); setIff2(false)` beside the `resetInterrupt()` that DI already used; on an NMI it cleared IFF1 after
copying it to IFF2; and before taking an NMI it cleared the latch it had just read. Three transitions of the
interrupt flip-flops and the NMI latch, each written by the CPU loop instead of being named by the state that holds
them. They are now `resetInterrupt()`, `enterNMI()` and `takeNMI()` on State.

## Context

- emulator/src/main/java/com/fpetrola/z80/cpu/State.java:157 `resetInterrupt()` existed before this fix and was not reused by doInt
- emulator/src/main/java/com/fpetrola/z80/instructions/impl/DI.java:33 the one caller that already used it
- emulator/src/main/java/com/fpetrola/z80/instructions/impl/RetN.java:35 the other half of the NMI protocol, `leaveNMI()` now

## Same cause elsewhere

- emulator/src/main/java/com/fpetrola/z80/cpu/OOZ80.java:83 the INT line raised and lowered by the CPU around its own acceptance
- emulator/src/test/java/net/emustudio/plugins/cpu/zilogZ80/CpuImpl.java:104 a test harness resetting State flag by flag, a power-on State does not name

## Principle

One owner per concept: the interrupt flip-flops and the NMI latch are State's, and so are their transitions; a
caller switching them one by one keeps a copy of a protocol it does not own.

## Candidate rule

same-as: 2026-11-10-ask-then-tell
