# Autopsy 2026-11-10-method-length (method-length)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `method-length` that went away (written by cerco):

- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/OOSpectrumLauncher.java|com.fpetrola.oozx.speccy.desktop.OOSpectrumLauncher.init()`

## Cause

Every machine is driven by a SpeccyEmulatorCore built from it, and the core kept the machine in a private field and
never said so. So whoever built a core also had to register the pair with the desk (`registerMachine(core,
speccy)`), the launcher and Desk.show both did, the desk kept a map from core to machine and had to forget it on
close, and a machine's own window asked the desk for its machine by handing over its core. The map was a copy of
what the key already held. Now EmulatorCore answers `machine()` (none for the defaults' core and for test cores),
SpeccyEmulatorCore gives its own, the window asks its core, and the map, the registration in the launcher and in
show(), and machineOf are gone; OOSpectrumLauncher.init lost a line and came under its length ceiling.

## Context

- machine/app/src/main/java/com/fpetrola/oozx/speccy/peripherals/SpeccyEmulatorCore.java:42 the machine the core always held
- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/EmulatorCore.java:31 the question every core now answers
- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/EmulatorWindow.java:25 what the plugins ask a machine's window
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:90 the window asks its core instead of the desk

## Same cause elsewhere

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/OOSpectrumLauncher.java:60 `speccy.control = core` right after building the core from the speccy: the machine is told its core from outside, a second half of the same link
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:1011 Desk.show builds the core and sets `machine.control` the same way, a copy of the launcher's

## Principle

One owner per fact: when an object is built from another and holds it, the link is the object's to answer; a map
from the one to the other kept by a third party is a second copy that has to be filled and forgotten in step.

## Candidate rule

rule: key-holds-value
A local built with `new T(..., v, ...)` from some value v, and later in the same block a call `x.put(k, v)` or
`x.register*(k, v)` with that local as the key and the same v as the value: the map keeps a link the key already
holds, because it was built from it. One finding per call, keyed by enclosing member and call text. It would have
caught `registerMachine(core, machine)` in the old Desk.show at machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:1011 and the launcher's
machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/OOSpectrumLauncher.java:59.
