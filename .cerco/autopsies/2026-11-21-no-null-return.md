# Autopsy 2026-11-21-no-null-return (no-null-return)

## Symptom

Findings of `no-null-return` that went away (written by cerco):

- `machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Desk.java|com.fpetrola.oozx.speccy.devices.Desk.configuration()|return null`

## Cause

"There is no desk" was said by a null configuration instead of by the desk nobody runs, which already exists as
Desk.NOBODY. Every window that asks the desk for what it remembers (favourites, history) assumed a real desk and
broke anywhere else: drawn alone at build time for the catalogue, its constructor threw a NullPointerException. The
absent desk is a state that already has an object; its answer has to be an object too: nothing remembered yet.

model: state-without-an-object

## Context

- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Desk.java:56 — NOBODY, the desk when no
  desk runs: the null object was there, but its configuration() answered null instead of an empty one.
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:768 — the only real
  answer, the person's saved configuration; an empty OOZxConfiguration is what a new person starts with too.

## Same cause elsewhere

- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Desk.java:147 coreOf answers null for
  nobody's desk as well; machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Desk.java:95 show too.
  Found by reading every default of Desk that returns null.

## Principle

A null object answers every question with an object of its own (introduce null object), so callers never branch on
whether the collaborator is really there.

## Candidate rule

same-as: 2026-11-11-no-null-return
