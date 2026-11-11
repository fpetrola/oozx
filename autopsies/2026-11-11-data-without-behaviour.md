# Autopsy 2026-11-11-data-without-behaviour (data-without-behaviour)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `data-without-behaviour` that went away (written by cerco):

- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.open(Chosen, boolean)|Chosen`

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

Chosen held a recording that was picked (the file, and the URL or zip plus entry it lasts as) but did nothing
with it: both places that hand a recording to a player took it apart field by field to call Opens.open with the
three values. The record was a bag the desk unpacked, so how a chosen recording goes into a player was written at
every hand-over instead of once by the thing that knows its own parts. Chosen.playIn(player) says it once; the desk
passes the recording and the player, and Chosen moved to its own file as the concept it is.

model: behaviour-not-on-owner


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Opens.java:36 the player's open with a source
  and an entry, the call both hand-overs spelled out from Chosen's fields.
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:435 and :980 the two
  hand-overs, now chosen::playIn.
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/Chosen.java:25 the recording playing itself into a
  player.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Desk.java:224 OOZxConfiguration.WindowState is
  filled and read field by field by the desk when windows are kept and brought back.
- machine/media/snapshot/src/main/java/com/fpetrola/oozx/speccy/modules/snapshot/Snapshots.java:237 SpectrumState is
  unpacked into the machine by Snapshots, the largest case of the same shape.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

Tell, don't ask: the object that holds the parts does the thing with them, instead of handing them out one by one.


## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause; a person files it with mvn cerco:approve, and
      while one waits every other autopsy proposes a rule
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->

rule: data-without-behaviour
A type with fields and no behaviour whose fields are read by members of other types. It flagged
machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:434 before this fix; cerco 0.15.0
has it.

