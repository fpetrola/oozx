# Autopsy 2026-11-22-unpromised-member (unpromised-member)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
.cerco/autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `unpromised-member` that went away (written by cerco):

- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/RomNotShippedDialog.java|RomNotShippedDialog.boolean readyFor(RomFiles, Object)`

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
Being ready to start a machine was a public static on the dialog that asks the person, RomNotShippedDialog.readyFor,
promised by no interface, because the only thing that knew how to ask lived in the app module and every caller had to
reach it there. The question is about what a machine or board is made with, which the Roms interface already stands
for, so it is now Roms#readyFor, answered by RomFiles, and the dialog only keeps the Consent it already implemented.
model: behaviour-not-on-owner

## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->
- machine/core/src/main/java/com/fpetrola/oozx/speccy/machine/Roms.java:37 — readyFor, promised where the machine and
  its devices already ask for their images.
- machine/core/src/main/java/com/fpetrola/oozx/Speccy.java:163 — select, which now asks it before becoming a model.
- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/DeviceFrame.java:99 — plugging a board in, which
  could not see the dialog's static at all.

## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->
none: grepped `public static` over machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop for other questions about
the machine answered by a window; RomNotShippedDialog.asking() remains, which only hands out the Consent instance.

## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->
A class keeps the promises of the interfaces it implements: what others may ask of it is declared by the type that
stands for the concept, not by whichever class happened to be able to answer.

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
covered-by: unpromised-member — the rule asked which interface promised readyFor, and the answer was that the Roms
interface, which already stands for what a machine and its devices are made with, was the missing promise; adding it
there is what let the kit module ask it without seeing the dialog, so the existing rule led to the model.
