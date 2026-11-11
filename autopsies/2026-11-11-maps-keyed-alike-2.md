# Autopsy 2026-11-11-maps-keyed-alike-2 (maps-keyed-alike)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `maps-keyed-alike` that went away (written by cerco):

- `machine/host/input/src/main/java/com/fpetrola/oozx/speccy/modules/keyboard/RecreatedLayout.java|com.fpetrola.oozx.speccy.modules.keyboard.RecreatedLayout|maps keyed by Integer`

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

The Recreated keyboard sends two letters for every Spectrum key: one when it goes down and another when it comes
up. The layout kept that as two maps from letter code to Spectrum key, one for presses and one for releases, filled
eighty lines apart, so the one fact - this key is pressed by that letter and let go by that other - was split in two
and a key's pair could only be checked by finding it in both tables. One map from letter code to what it means (a key,
pressed or let go of), filled by one row per Spectrum key with both of its letters, says the protocol as it is, and
pressed() looks the letter up once.

model: concept-without-a-class


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/host/input/src/main/java/com/fpetrola/oozx/speccy/modules/keyboard/RecreatedLayout.java:35 what one
  letter means, and :88 the row that gives a key its two letters.
- machine/host/input/src/main/java/com/fpetrola/oozx/speccy/modules/input/Input.java:292 the input that picks this
  layout for the Recreated keyboard.
- machine/host/input/src/test/java/model/tests/input/RecreatedKeysTest.java written first against the two maps, it
  pins what all eighty letters do, shifted and not.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/SwingKeyboard.java:37 a static map from unicode to
  key beside a map from key code to key, flagged by the same rule; not changed here.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

Say one fact in one place: a key's press and release letters are one row, not entries in two tables kept in step by hand.


## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause; a person files it with mvn cerco:approve, and
      while one waits every other autopsy proposes a rule
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->

same-as: 2026-11-11-maps-keyed-alike

