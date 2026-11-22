# Autopsy 2026-11-22-no-static-method (no-static-method)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
.cerco/autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `no-static-method` that went away (written by cerco):

- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/RomNotShippedDialog.java|com.fpetrola.oozx.speccy.desktop.RomNotShippedDialog.readyFor(RomFiles, Object)`

## Cause

<!-- Not what the rule says, but why the code was written that way: the concept, responsibility or
abstraction that was missing or in the wrong place. In your own words, not copied from another autopsy.
Then, on a line of its own, `model: <id>` with the one that fits best of:
  behaviour-not-on-owner: the object holding the data lacks the behaviour
  variation-as-data: a variant is encoded as a value that others decide on
  dispatch-from-outside: the hierarchy exists but the operation lives outside it
  structure-walked-from-outside: the structure does not know how to walk itself
  concept-without-a-class: data that travels together has no type
  value-without-a-type: a primitive or a string plays a role of the domain
  state-without-an-object: a mode or a phase is kept in flags or in null
  shape-repeated: the same shape is written again with one part varying
  knowledge-without-one-owner: the same fact or decision lives in several places
  procedure-instead-of-model: a procedure is narrated step by step
  protocol-by-convention: a collaboration works only if callers do things in order
  hierarchy-without-is-a: a type inherits what it does not do, or stands between others doing nothing
-->
Whether a machine could be started — what it is missing, asking, and bringing it — lived as a static
procedure on the dialog, RomNotShippedDialog.readyFor(roms, machine), so only code that could see the app module
could ask it. Boards in machine/devices/kit could not, and a board that needed a ROM was plugged in and stayed
without one. Being ready is a question about the ROMs a thing is made with, so it belongs to RomFiles, and how
the person watches the bringing belongs to the Consent the dialog already was.
model: behaviour-not-on-owner


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->
- machine/core/src/main/java/com/fpetrola/oozx/config/RomFiles.java:86 — readyFor, now on the owner of the ROMs.
- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/DeviceFrame.java:99 — plugging a board in, which
  could not reach the static on the dialog and now asks the machine's RomFiles.
- machine/app/src/main/java/com/fpetrola/oozx/speccy/peripherals/SpeccyEmulatorCore.java:303 — starting a machine,
  the caller that used the static.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->
none in this project: grepped missingFor( and bring( over machine/ and every caller now goes through readyFor;
the plugins repository has two hand-written walks of missingFor and bring, in its mappings and machine-pictures
modules, which are outside this project's root.

## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->
Behaviour lives with the data it works on: the object that knows what a machine is made with answers whether
it can be started, and the UI only decides how the asking looks.


## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause; a person files it with mvn cerco:approve, and
      while one waits every other autopsy proposes a rule
covered-by: <id of a rule this cerco has>  when that rule had marked this code and what it told already
      led to the missing model: say why, in 150 characters or more. Not the way out when the rule
      found the symptom and something else was the cause
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->
covered-by: no-static-method — the rule flagged readyFor as a procedure on the dialog taking the RomFiles it works
on as a parameter, which is exactly the hint that the behaviour belongs to RomFiles; following it is what let boards
in the kit module ask the same question, so no new rule is needed.
