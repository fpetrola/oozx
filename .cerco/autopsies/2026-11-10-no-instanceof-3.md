# Autopsy 2026-11-10-no-instanceof-3 (no-instanceof)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `no-instanceof` that went away (written by cerco):

- `machine/ui/src/main/java/com/fpetrola/oozx/speccy/screen/ScreenSettings.java|com.fpetrola.oozx.speccy.screen.ScreenSettings.settings()|instanceof Scaler`
- `machine/ui/src/main/java/com/fpetrola/oozx/speccy/screen/ScreenSettings.java|com.fpetrola.oozx.speccy.screen.ScreenSettings.steps(ScreenEffect.When)|instanceof Scaler`

## Cause

What arrives in the plugin jars is one flat list of ScreenEffect, and two kinds of thing travel in it: scalers, of
which one is picked to make a windowful, and steps, which are stacked on their side of the scaler. The effect knew
which kind it was (a Scaler is a ScreenEffect that scales), but nothing let it say so when it arrived, so
ScreenSettings kept the list as it came and split it every time it was asked, by asking each element its type: once
to list the scalers (isInstance and cast), once to list the steps of a side, once to collect the knobs of the steps.
The missing operation was the effect taking its own place among what arrived: a double dispatch, where ScreenEffect
goes among the steps by default and Scaler overrides it to go among the scalers, into WaysOfDrawing, which keeps both
lists already sorted.

model: dispatch-from-outside

## Context

- machine/ui/src/main/java/com/fpetrola/oozx/speccy/screen/ScreenSettings.java:57 thereAre received the flat list
  and kept it as it came; every reader had to split it again.
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:1318 the desk hands over what
  the plugins brought, mixed, so the split cannot happen before ScreenSettings.
- machine/ui/src/main/java/com/fpetrola/oozx/speccy/screen/Scalers.java:57 the scalers that arrived are joined with
  the built-in ones, the only reader that wanted the scalers alone.
- machine/ui/src/main/java/com/fpetrola/oozx/speccy/screen/Scaler.java:35 Scaler extends ScreenEffect and already
  overrides apply, so it is the natural place to say where it goes.

## Same cause elsewhere

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:987 and :1258 the desk finds
  the players among its windows with `instanceof Opens`: a mixed collection split by asking each element its type,
  where the window could say what it is to the desk.
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:786 the same test once more.

## Principle

Polymorphism over type tests, through double dispatch: an element of a mixed collection says what it is by calling
back the right method of whoever receives it, instead of being asked its type by every reader.

## Candidate rule

rule: hierarchy-asked-from-outside
A hierarchy of the project (Scaler and ScreenEffect, with their many implementations) whose types are tested with
instanceof or casts from two or more members outside it; it is the candidate already in cerco 0.14.0, and this case
is one of the two it found in machine, at machine/ui/src/main/java/com/fpetrola/oozx/speccy/screen/ScreenSettings.java:62.
