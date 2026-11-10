# Autopsy 2026-11-10-flag-driven-from-outside-3 (flag-driven-from-outside)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `flag-driven-from-outside` that went away (written by cerco):

- `machine/app/src/main/java/com/fpetrola/oozx/speccy/peripherals/SpeccyEmulatorCore.java|com.fpetrola.oozx.speccy.peripherals.SpeccyEmulatorCore.resumeEmulation()|speccy.loop.setPaused(false)`

## Cause

Pausing the machine is a change of MachineLoop's state that the desk has to hear about, but the loop does not
announce it: whoever pauses also has to tell the listeners. So SpeccyEmulatorCore wrote the pair "set the loop's
flag, then notifyPauseStateChange" three times (toggle, resume, the `pause` option), and every other place that
pauses the loop does only the first half and nobody hears it: Cpu.stopHere for a breakpoint, the SZX session when it
restores a machine paused, the debugger plugin. The announcement was not part of the change, so it lived with each
caller. The three copies are now one `paused(boolean)` in SpeccyEmulatorCore; the loop itself still does not announce,
which is the part of the model still missing.
model: protocol-by-convention

## Context

- machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/z80/MachineLoop.java:67 `setPaused` changes the flag and tells no one
- machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/z80/Cpu.java:250 a breakpoint pauses the loop without announcing it
- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/MockEmulatorCore.java:228 the listeners live in the core, not in the loop
- machine/app/src/main/java/com/fpetrola/oozx/speccy/peripherals/SpeccyEmulatorCore.java:171 the one place that now sets and announces

## Same cause elsewhere

- machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/z80/Cpu.java:248 `stopHere()` pauses silently
- machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/z80/Cpu.java:235 Cpu keeps a second `emulatorPaused` flag that nothing in this repository reads: two owners of one concept
- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/MockEmulatorCore.java:55 the mock sets its own flag and announces it, the right shape, because there the owner is the one announcing

## Principle

A change of state and its announcement are one operation of the object that owns the state; observers told by the
callers hear only the callers that remember to tell them.

## Candidate rule

rule: notified-by-caller
A statement that calls a setter on another object (a receiver that is not empty, `this` or `super`) followed directly
by a statement that calls a `notify*`, `fire*` or `publish*` method of the caller itself: the caller announces a
change in state that is not its own, so every other caller that makes the same change stays silent. One finding per
pair, keyed by enclosing member and the setter call. Before this fix it would have found the three copies in
machine/app/src/main/java/com/fpetrola/oozx/speccy/peripherals/SpeccyEmulatorCore.java:163, and today it still finds
the one left at machine/app/src/main/java/com/fpetrola/oozx/speccy/peripherals/SpeccyEmulatorCore.java:172 until the
loop announces its own pauses.
