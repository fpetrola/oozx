# Autopsy 2026-11-10-flag-driven-from-outside (flag-driven-from-outside)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `flag-driven-from-outside` that went away (written by cerco):

- `emulator/src/main/java/com/fpetrola/z80/cpu/OOZ80.java|com.fpetrola.z80.cpu.OOZ80.doInt()|state.setHalted(false)`
- `emulator/src/main/java/com/fpetrola/z80/cpu/OOZ80.java|com.fpetrola.z80.cpu.OOZ80.nmi()|state.setHalted(false)`
- `emulator/src/main/java/com/fpetrola/z80/instructions/impl/Halt.java|com.fpetrola.z80.instructions.impl.Halt.execute()|state.setHalted(true)`
- `emulator/src/main/java/com/fpetrola/z80/tstates/UncontendedTiming.java|com.fpetrola.z80.tstates.UncontendedTiming.costWith(int[], int, int)|state.setHalted(false)`

## Cause

<!-- Not what the rule says, but why the code was written that way: the concept, responsibility or
abstraction that was missing or in the wrong place. In your own words, not copied from another autopsy. -->

HALT is a state of the CPU with its own way in and its own way out, but the model only had a boolean with a
setter. Going in (HALT keeps PC on itself) lived in the Halt instruction; going out (clear the flag and step PC past
the HALT) was written twice, once for a maskable interrupt in OOZ80.doInt and once for the NMI in OOZ80.nmi, and a
third caller, the scratch machine that prices instructions in UncontendedTiming, cleared it by hand to reset itself.
Each caller asked State whether it was halted and then told it what to be, so the transition belonged to whoever
needed it and nobody owned it. State now has halt() and wake(): wake does both halves of leaving HALT, so neither
interrupt path can forget the PC step.

## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- emulator/src/main/java/com/fpetrola/z80/cpu/State.java:120 setHalted also switches runState, so the flag was
  never bare data: State already owned a consequence of halting, just not the transition itself.
- emulator/src/main/java/com/fpetrola/z80/cpu/RegistersSetter.java:88 the public setter exists for snapshots, which
  restore the flag as it was (machine/spectrum/src/main/java/com/fpetrola/emulation/helpers/snapshots/SnapshotLoader.java:141);
  that is why it stays: restoring is not a transition.
- machine/generated/src/main/java/com/fpetrola/oozx/generated/GeneratedSpectrumZ80.java:1454 the generated core copies
  Halt.execute, so the fix reaches it by regenerating, not by hand.

## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

- emulator/src/main/java/com/fpetrola/z80/cpu/OOZ80.java:52 the same ask-then-tell on the NMI: the CPU asks
  isActiveNMI and switches it off; taking a pending NMI is State's transition.
- emulator/src/main/java/com/fpetrola/z80/cpu/OOZ80.java:94 and emulator/src/main/java/com/fpetrola/z80/cpu/OOZ80.java:119
  accepting an interrupt rewrites IFF1/IFF2 from outside, differently for INT and NMI, and
  emulator/src/main/java/com/fpetrola/z80/instructions/impl/RetN.java:35 undoes it: the interrupt-enable protocol
  is spread over three classes like HALT was.
- emulator/src/main/java/com/fpetrola/z80/cpu/OOZ80.java:84 interruption() raises and lowers the INT line around
  doInt, a line the machine owns.

## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

Tell, don't ask: an object that holds a state owns its transitions. A caller that reads another object's state to
decide what to set on it has taken the decision away from the owner, and every caller writes its own copy.

## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->

rule: ask-then-tell
An `if` whose condition reads a property of another object through its getter (`o.isX()` / `o.getX()`, negated or
compared or not) and whose branch calls that same object's setter for that property (`o.setX(...)`). The caller
decides another object's transition. Example: emulator/src/main/java/com/fpetrola/z80/cpu/OOZ80.java:52
(`if (state.isActiveNMI()) { state.setActiveNMI(false); ... }`); before this fix, OOZ80.doInt and Halt.execute had
the same shape with isHalted/setHalted. The rule would tell: move the transition into the owner as a method named
for what happens (wake, takeNmi), and call that. Calls on `this` (or without a receiver) are the owner deciding for
itself and are left alone.
