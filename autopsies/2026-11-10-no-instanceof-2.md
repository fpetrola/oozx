# Autopsy 2026-11-10-no-instanceof-2 (no-instanceof)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `no-instanceof` that went away (written by cerco):

- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.coreOf(java.awt.Component)|instanceof EmulatorInternalFrame`
- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.getActiveEmulator()|instanceof EmulatorInternalFrame`
- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.machineBeingUsed()|instanceof EmulatorInternalFrame`
- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.machineOf(JInternalFrame)|instanceof EmulatorInternalFrame`
- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.putMachinesAway()|instanceof EmulatorInternalFrame`
- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.refillMachineToolBars()|instanceof EmulatorInternalFrame`
- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.setScreenDefault(String, String)|instanceof EmulatorInternalFrame`

## Cause

The desk holds many kinds of window (machines, clipped equipment, players, settings) in one Swing JDesktopPane, and
it never kept which of them are machines. So every question about the machines was answered by walking all the
frames of the pane and testing each one for EmulatorInternalFrame: the tool bars refilled, a screen default applied,
the machines put away at exit, the window behind a core, and twice "the machine in use", which therefore existed as
two different answers (getActiveEmulator: selected or first visible; machineBeingUsed: selected or the machine the
selected window is clipped onto), so a tool shortcut and a key could reach different machines. The desk now keeps
its machines in a list, joined and left in onTheDesk, and has one machineBeingUsed for tools, keys and equipment.
model: structure-walked-from-outside

## Context

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:877 the machines the desk keeps, beside machinesByCore which already tracked their cores
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:1489 the one machine in use, front-most when nothing in front stands for one
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:1549 where a machine's window joins and leaves the desk
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:1641 backToFront, the pane's own order, still used for keeping every window
- machine/ui/src/main/java/com/fpetrola/oozx/speccy/windows/AttachedFrame.java:643 a clipped window looks for machines the same way, among all the pane's components

## Same cause elsewhere

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:1274 the players are found by testing every open window for Opens, as the machines were
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:786 a recording behind a machine found by testing a window for Opens
- machine/ui/src/main/java/com/fpetrola/oozx/speccy/windows/AttachedFrame.java:638 `machinesAround()` filters the desk's components for MachineWindow: the ui module has no desk to ask

## Principle

A container that needs its parts by kind keeps them by kind: re-deriving the kind with a type test each time it is
asked spreads one fact (which windows are machines) over every question, and lets the answers drift apart.

## Candidate rule

rule: refiltered-by-type
In one file, two or more for-each loops over the same call (the same text, e.g. `desktop.getAllFrames()`) whose body
tests the loop variable with `instanceof` of the same type: the class keeps asking a mixed collection for one kind
of element it should hold itself. One finding per (collection call, type), at the first such loop. It would have
caught the three loops over `desktop.getAllFrames()` testing EmulatorInternalFrame in the old
machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:483; today it finds nothing in oozx.
