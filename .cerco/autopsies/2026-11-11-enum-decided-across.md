# Autopsy 2026-11-11-enum-decided-across (enum-decided-across)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `enum-decided-across` that went away (written by cerco):

- `machine/host/input/src/main/java/com/fpetrola/oozx/speccy/modules/keyboard/RecreatedLayout.java|com.fpetrola.oozx.speccy.modules.keyboard.RecreatedLayout.pressed(InputKey)|InputKey|INPUT_KEY_NONE`

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

pressed() compared what it found with INPUT_KEY_NONE to tell whether the letter was a release, because with two
tables the only way to know which one answered was to ask the first and fall back on the second when it said
nothing: NONE stood for "not in this table". With one table whose entries say whether they press or let go, the
release is read off the entry, and NONE is left only as what a letter that means nothing produces. The comparison was
the seam between the two halves of one table.

model: concept-without-a-class


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/host/input/src/main/java/com/fpetrola/oozx/speccy/modules/keyboard/RecreatedLayout.java:35 the entry that
  says press or release.
- machine/host/input/src/main/java/com/fpetrola/oozx/speccy/modules/input/Input.java:292 where the layout is chosen
  for the Recreated keyboard.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

- machine/host/input/src/main/java/com/fpetrola/oozx/speccy/modules/input/Input.java:268 useShiftedArrowKeys
  decides over four InputKey constants, flagged by the same rule.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

A sentinel constant used to mean 'the other table answers' hides a missing distinction that the data should carry itself.


## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause; a person files it with mvn cerco:approve, and
      while one waits every other autopsy proposes a rule
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->

same-as: 2026-11-10-no-switch-2

