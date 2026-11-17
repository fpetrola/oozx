# Autopsy 2026-11-10-flag-driven-from-outside-4 (flag-driven-from-outside)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `flag-driven-from-outside` that went away (written by cerco):

- `emulator/src/main/java/com/fpetrola/z80/registers/Composed16BitRegister.java|com.fpetrola.z80.registers.Composed16BitRegister.update8Bits(int, R)|Register8Bits.setListening(false)`

## Cause

A register pair that is written as a whole tells its own listeners, and then has to write each half without the
half telling its listeners again. ObservableRegister had no such write: it only had a `listening` switch, so
Composed16BitRegister.update8Bits borrowed the half's mode from outside (read the switch, turn it off, write, put it
back) for every write of a pair while listening. A write nobody hears is an operation of the observable register,
which owns both the switch and the listeners; it was spelled as three calls on its switch by the one class that
needed it. It is now `writeUnheard(int)` on ObservableRegister.
model: protocol-by-convention

## Context

- emulator/src/main/java/com/fpetrola/z80/spy/ObservableRegister.java:87 the write its listeners do not hear, now the register's
- emulator/src/main/java/com/fpetrola/z80/registers/Composed16BitRegister.java:120 the pair switches both halves along with itself
- translation/translator/src/main/java/com/fpetrola/z80/minizx/emulation/finders/Z80Rewinder.java:87 the one place that turns listening on, for the rewinder

## Same cause elsewhere

- machine/ui/src/main/java/com/fpetrola/oozx/speccy/windows/AttachedFrame.java:330 `compactHeight()` hides the detail list, measures and shows it again: the same borrow of another object's mode, though there the object is a Swing component that cannot be given the operation, so the measure would have to be asked another way
- translation/translator/src/main/java/com/fpetrola/z80/minizx/emulation/finders/Z80Rewinder.java:147 commented-out loops that switched every register on and off around a run

## Principle

Tell, don't ask: an object whose mode a caller saves, changes and restores around one call is missing that call as
an operation of its own, and every caller that needs it keeps a copy of the save-and-restore.

## Candidate rule

rule: borrowed-mode
A local initialized from another object's getter (`boolean was = r.isX();` or `T was = r.getX();`, receiver not
empty, `this` or `super`), later in the same block a call `r.setX(...)` with something else, and after it
`r.setX(was)`: the caller borrows the object's mode for a moment, which the object should offer as one operation.
One finding per borrow, keyed by enclosing member and the getter call. It would have caught the old
emulator/src/main/java/com/fpetrola/z80/registers/Composed16BitRegister.java:69 and finds today
machine/ui/src/main/java/com/fpetrola/oozx/speccy/windows/AttachedFrame.java:330.
