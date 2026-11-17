# Autopsy 2026-11-11-no-type-code (no-type-code)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `no-type-code` that went away (written by cerco):

- `machine/ui/src/main/java/com/fpetrola/oozx/speccy/screen/Scalers.java|com.fpetrola.oozx.speccy.screen.Scalers.all()|written.contains(one.getClass())`

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

Scalers.all() merged the scalers written into the build with the ones that arrived as plugins and dropped
duplicates by their class, as if a scaler were its class. A scaler is what it is called: byName finds one by its
label, and the three xBRZ scalers are one class with three factors, so the class could neither tell them apart nor
say that two copies of one are the same. Deduplicating by label says what makes two scalers the same one.

model: value-without-a-type


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/ui/src/main/java/com/fpetrola/oozx/speccy/screen/Scalers.java:55 the merge, by label now.
- machine/ui/src/main/java/com/fpetrola/oozx/speccy/screen/ScreenSettings.java:62 the scalers that arrived as
  plugins, which the merge adds to the written ones.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

- machine/ui/src/main/java/com/fpetrola/oozx/speccy/screen/Scalers.java:60 byName still accepts the class's
  simple name as well as the label, for settings written with class names.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

An object's identity is said by the model (its name), not by which class happens to implement it.


## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause; a person files it with mvn cerco:approve, and
      while one waits every other autopsy proposes a rule
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->

rule: no-type-code
A comparison of getClass() or of a type field used to tell objects apart. It flagged Scalers.all() before this fix and
still flags machine/core/src/main/java/com/fpetrola/oozx/speccy/peripherals/AbstractPeripheral.java:52; cerco has
it.

