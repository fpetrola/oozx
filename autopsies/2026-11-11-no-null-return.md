# Autopsy 2026-11-11-no-null-return (no-null-return)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `no-null-return` that went away (written by cerco):

- `machine/core/src/main/java/com/fpetrola/oozx/speccy/config/OOZxConfiguration.java|com.fpetrola.oozx.speccy.config.OOZxConfiguration.compressAndEncode(byte[])|return null`
- `machine/core/src/main/java/com/fpetrola/oozx/speccy/config/OOZxConfiguration.java|com.fpetrola.oozx.speccy.config.OOZxConfiguration.decodeAndDecompress(String)|return null`
- `machine/core/src/main/java/com/fpetrola/oozx/speccy/config/OOZxConfiguration.java|com.fpetrola.oozx.speccy.config.OOZxConfiguration.packSnapshot(byte[])|return null`
- `machine/core/src/main/java/com/fpetrola/oozx/speccy/config/OOZxConfiguration.java|com.fpetrola.oozx.speccy.config.OOZxConfiguration.unpackSnapshot(String)|return null`

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

The deleted helpers answered failure with null after printing the error - packSnapshot, unpackSnapshot,
compressAndEncode, decodeAndDecompress - which is how a free function with no caller to tell says 'it went wrong'.
They returned null because they were procedures with nowhere to report to, and nobody noticed since nobody called
them. Removing the dead procedures removes the null returns; there was no live path to fix.

model: procedure-instead-of-model


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/core/src/main/java/com/fpetrola/oozx/speccy/config/OOZxConfiguration.java:376 the configuration keeps a
  packed snapshot as the string it was given (getSnapshot), and packs nothing itself any more.
- machine/spectrum/src/main/java/com/fpetrola/emulation/helpers/snapshots/SnapshotSaver.java:133 snapshots are written
  as bytes; the compressed-base64 variants beside them had no callers.
- machine/media/snapshot/src/main/java/com/fpetrola/oozx/speccy/modules/snapshot/Snapshots.java:124 packed(), the
  machine's own snapshots packing what they write, which is what replaced the helpers.
- searched for callers: this tree (every module), oozx-plugins, the traduccion branch (git grep), the research tools in
  ~/detodo/spectrum/investigacion and the strings of the 200 published plugin jars; each name appeared only where it
  was defined.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

none: the same search found no caller of any of the removed helpers anywhere.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

A failure is answered to someone: an absent value is a type or an exception, not a null printed and handed back.


## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause; a person files it with mvn cerco:approve, and
      while one waits every other autopsy proposes a rule
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->

rule: no-null-return
A method of the project that returns null. It flagged the removed OOZxConfiguration.packSnapshot and still flags
machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:776; cerco has it.

