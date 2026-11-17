# Autopsy 2026-11-10-no-instanceof (no-instanceof)

## Symptom

Findings of `no-instanceof` that went away (written by cerco):

- `emulator/src/main/java/com/fpetrola/z80/cpu/OOZ80.java|com.fpetrola.z80.cpu.OOZ80.execute()|instanceof EI`

## Cause

The "EI shadow" (an interrupt is only accepted after the instruction that follows EI) is a property of the
instruction, but the model had no place for it, so the CPU loop recognised EI by its class. Worse, the protocol
behind it has no owner at all: a flag `pendingEI` lives in State, the CPU loop reads it and switches it off, the
snapshot code copies it, and nothing in normal execution ever switches it on - EI.execute only sets iff1/iff2. The
type test was guarding a state machine that only exists half-written, spread over four classes.
model: dispatch-from-outside

## Context

- emulator/src/main/java/com/fpetrola/z80/cpu/State.java:128 `enableInterrupt()` sets iff1 and iff2 and nothing
  else: this is everything EI does, so it never marks the shadow as pending.
- emulator/src/main/java/com/fpetrola/z80/cpu/State.java:192 `setPendingEI` is a bare setter: whoever wants the
  protocol drives it from outside.
- emulator/src/main/java/com/fpetrola/z80/cpu/OOZ80.java:57 the interrupt is accepted when `!isPendingEI()`,
  which in practice is always, so an interrupt can land right after EI.
- emulator/src/main/java/com/fpetrola/z80/cpu/OOZ80.java:69 the only writer of the flag during execution, and it
  only ever writes false.

## Same cause elsewhere

- machine/spectrum/src/main/java/com/fpetrola/emulation/helpers/snapshots/SnapshotLoader.java:146 and
  machine/spectrum/src/main/java/com/fpetrola/emulation/helpers/snapshots/SnapshotSaver.java:101 copy the flag in
  and out: the only way it is ever true is a snapshot that says so.
- emulator/src/test/java/net/emustudio/plugins/cpu/zilogZ80/CpuImpl.java:110 a test harness that also drives it
  from outside.
- emulator/src/main/java/com/fpetrola/z80/tstates/PhaseProcessor.java:87 `instanceof LdAI || instanceof LdAR`:
  another property of an instruction (its timing / the P/V quirk of LD A,I and LD A,R) recognised by class from
  outside instead of being asked to the instruction.

## Principle

Information expert and single owner of a concept: the instruction knows whether it defers interrupts, and one
object owns the interrupt-acceptance protocol (pending, accept, end) instead of a public flag every class pokes.

## Candidate rule

rule: flag-driven-from-outside
A boolean field whose setter is called with literal `true` or `false` from classes other than its owner, or only
ever with one of the two literals, is a protocol without an owner (a state machine written as a public switch).
The rule would report the setter and every outside caller and ask for the transition to become a method of the
owner (`deferInterrupts()`, `acceptInterrupt()`). Example: emulator/src/main/java/com/fpetrola/z80/cpu/OOZ80.java:69
calls `setPendingEI(false)` on State, and no code path ever calls it with `true`.
