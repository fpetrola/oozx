# Autopsy 2026-11-22-walked-from-outside (walked-from-outside)

## Symptom

Findings of `walked-from-outside` that went away (written by cerco):

- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.doWhatIsOffered(dev.crystal.plugins.api.Offer)|equipment()`
- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.offers()|equipment()`
- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.openWhatTheMachineUses()|equipment()`

## Cause

Doing what a brought plugin offered walked the lists of what is plugged in, one per kind (desk windows, equipment,
and now machines), matching each by class name and repeating for each kind how it is opened. But every one of them
is already an action of the desk that says which extension offered it and knows what choosing it does: the desk's
offered actions are the structure, and they were being walked from outside instead of asked. Asking the desk for the
action whose offeredBy is the offer's extension, and choosing it, does it for every kind at once.

model: structure-walked-from-outside

## Context

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:455 — doWhatIsOffered, now one
  question to the desk's actions; only the bar tools stay, which are no action of their own.
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:607 — offers() makes each
  equipment into the desk's action that shows it: the same knowledge doWhatIsOffered repeated by hand.
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/WhatIsPluggedIn.java:96 — equipment(), the list that was
  walked from three members of the desk.

## Same cause elsewhere

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:458 — the bar tools are still
  walked and matched by class name, because a tool with no key has no action of the desk; giving every tool one would
  remove it.
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:1383 — openWhatTheMachineUses
  walks equipment to find what a machine uses. Found by reading every use of has.equipment(), has.tools() and
  has.deskWindows() in the file.

## Principle

Tell, don't ask: the structure that already knows what each offered thing is and how it is done is asked to do it,
instead of being walked and re-decided from outside.

## Candidate rule

same-as: 2026-11-10-no-null-return-2
