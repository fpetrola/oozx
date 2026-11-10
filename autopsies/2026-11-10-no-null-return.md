# Autopsy 2026-11-10-no-null-return (no-null-return)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `no-null-return` that went away (written by cerco):

- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.getActiveEmulator()|return null`

## Cause

Same fix as 2026-11-10-no-instanceof-2, seen by no-null-return: the null came from getActiveEmulator, the second notion of the
machine in use, which walked the desk's frames, tested each for EmulatorInternalFrame, cast it after the test and
returned null when there was none. With the desk keeping its machines and answering one machineBeingUsed, the
method is gone: the list already holds the right type, so there is nothing to cast, and its one null went with it.
model: structure-walked-from-outside

## Context

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:877 the list of machines that makes the casts unnecessary
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:1489 the method that replaced getActiveEmulator for every caller
- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Desk.java:147 `coreOf` as the plugins ask it, answered from the list now

## Same cause elsewhere

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:1275 `window instanceof Opens holds ? holds : null`: a test, a binding and a null where a desk that kept its players would answer directly

## Principle

One owner per concept: the desk owns which of its windows are machines, so the type of each is known where it is
kept, not recovered by every reader with a test, a cast and a null.

## Candidate rule

same-as: 2026-11-10-no-instanceof-2
