# Autopsy 2026-11-11-no-static-method-2 (no-static-method)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `no-static-method` that went away (written by cerco):

- `machine/core/src/main/java/com/fpetrola/oozx/speccy/config/OOZxConfiguration.java|com.fpetrola.oozx.speccy.config.OOZxConfiguration.compressAndEncode(byte[])`
- `machine/core/src/main/java/com/fpetrola/oozx/speccy/config/OOZxConfiguration.java|com.fpetrola.oozx.speccy.config.OOZxConfiguration.decodeAndDecompress(String)`
- `machine/core/src/main/java/com/fpetrola/oozx/speccy/config/OOZxConfiguration.java|com.fpetrola.oozx.speccy.config.OOZxConfiguration.packSnapshot(byte[])`
- `machine/core/src/main/java/com/fpetrola/oozx/speccy/config/OOZxConfiguration.java|com.fpetrola.oozx.speccy.config.OOZxConfiguration.unpackSnapshot(String)`
- `machine/spectrum/src/main/java/com/fpetrola/emulation/helpers/snapshots/SnapshotSaver.java|com.fpetrola.emulation.helpers.snapshots.SnapshotSaver.getSnapshotAsCompressedBase64(RegistersGetter, State)`
- `machine/spectrum/src/main/java/com/fpetrola/emulation/helpers/snapshots/SnapshotSaver.java|com.fpetrola.emulation.helpers.snapshots.SnapshotSaver.loadSnapshotFromCompressedBase64(String)`

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

Six static helpers - pack and unpack a snapshot, compress and encode, decode and decompress, and two compressed
base64 forms of a saved state - were left behind by earlier ways of keeping snapshots in the configuration, each
written as a free function because none belonged to an object that still used it. Packing moved to whoever
writes the snapshot (Snapshots.packed()), and these stayed public and static, so nothing showed they were dead.

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

- machine/spectrum/src/main/java/com/fpetrola/z80/minizx/emulation/MiniZXWithEmulationBase.java:37
  loadSnapshotFromBase64 has no caller in any of those places either, but it is protected in the translation runtime,
  where a translated game may call it unqualified; left for whoever owns that runtime.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

Dead code is deleted: a function nothing calls only makes the model look larger and older than it is.


## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause; a person files it with mvn cerco:approve, and
      while one waits every other autopsy proposes a rule
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->

same-as: 2026-11-11-no-static-method

