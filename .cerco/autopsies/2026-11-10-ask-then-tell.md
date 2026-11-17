# Autopsy 2026-11-10-ask-then-tell (ask-then-tell)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `ask-then-tell` that went away (written by cerco):

- `emulator/src/main/java/com/fpetrola/z80/cpu/OOZ80.java|com.fpetrola.z80.cpu.OOZ80.execute()|state.setActiveNMI(false)`

## Cause

The NMI pin is an edge the Z80 latches and consumes once, and taking an NMI is a transition of the interrupt
flip-flops (IFF1 kept in IFF2, IFF1 cleared, and RETN bringing it back). State had none of that as operations: it
exposed `activeNMI`, `iff1` and `iff2` as bare properties, so OOZ80.execute asked whether the latch was set and
cleared it itself, OOZ80.nmi copied IFF1 into IFF2 and cleared IFF1 by hand, OOZ80.doInt cleared both flip-flops one
by one although State.resetInterrupt already did exactly that (DI uses it), and RetN copied IFF2 back into IFF1. The
protocol of the flip-flops lived in three callers and only one of them reused what State had. Now State owns
`takeNMI()`, `enterNMI()` and `leaveNMI()`, doInt calls `resetInterrupt()`, and what the INT and NMI acceptance
share (wake from HALT, R incremented, PC pushed) is one `accept()` in OOZ80 instead of two copies.
model: behaviour-not-on-owner

## Context

- emulator/src/main/java/com/fpetrola/z80/cpu/State.java:162 the latch and the flip-flop transitions, now State's
- emulator/src/main/java/com/fpetrola/z80/instructions/impl/RetN.java:35 RETN was `state.setIff1(state.isIff2())`
- emulator/src/main/java/com/fpetrola/z80/instructions/impl/DI.java:33 DI already used `resetInterrupt()`, which doInt duplicated
- emulator/src/main/java/com/fpetrola/z80/bytecode/RegistersBase.java:186 `triggerNMI()` sets the latch: the edge comes in here
- emulator/src/main/java/com/fpetrola/z80/cpu/OOZ80.java:111 the NMI acceptance, now `accept()` plus `state.enterNMI()`

## Same cause elsewhere

- emulator/src/main/java/com/fpetrola/z80/cpu/OOZ80.java:83 `interruption()` raises the INT line, accepts, and lowers it: the CPU drives a pin that belongs to the ULA, so the line is a property anyone sets instead of an input State is told about
- machine/spectrum/src/main/java/com/fpetrola/emulation/helpers/snapshots/SnapshotZ80.java:566 `spectrum.setEnabledAY(spectrum.isEnabledAYon48k())`: a caller moving one property of an object into another of the same object, the same shape as the old RETN
- emulator/src/main/java/com/fpetrola/z80/cpu/State.java:222 `isActiveNMI`/`setActiveNMI` stay public for the snapshots, which restore the latch as it was; anyone can still drive it

## Principle

Tell, don't ask, and one owner per concept: a transition between two pieces of an object's own state belongs to that
object, under a name; a caller that reads one property to write it into another is doing the object's job.

## Candidate rule

rule: property-shuffle
A call `x.setA(x.getB())` or `x.setA(x.isB())` whose receiver x is the same expression on both sides, is not empty,
`this` or `super`, and where A and B differ: the caller moves state between two properties of one object, which is a
transition that object should own and name. One finding per call, keyed by enclosing member and call text. It would
have caught the old RETN (`state.setIff1(state.isIff2())`, emulator/src/main/java/com/fpetrola/z80/instructions/impl/RetN.java:35)
and the old NMI (`state.setIff2(state.isIff1())`), and today it finds
machine/spectrum/src/main/java/com/fpetrola/emulation/helpers/snapshots/SnapshotZ80.java:566.
