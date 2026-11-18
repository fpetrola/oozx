# Autopsy 2026-11-18-maps-keyed-alike (maps-keyed-alike)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
.cerco/autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `maps-keyed-alike` that went away (written by cerco):

- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/SwingKeyboard.java|com.fpetrola.oozx.speccy.desktop.SwingKeyboard|maps keyed by Integer`

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

SwingKeyboard kept two tables keyed by Integer: one from Swing key codes, one from characters. The second
said nothing Swing-specific: each of its 92 entries mapped a character to the InputKey whose own value is
that same character, so it repeated a fact the enum already owned. Because the enum had no way to answer
"which key types this character", the host keyboard wrote the answer again as a map beside the real one.

model: knowledge-without-one-owner

## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/host/input/src/main/java/com/fpetrola/oozx/speccy/modules/input/Input.java:244 InputKey.typing,
  the enum answering from its own values: every InputKey already carried the character code it stands for.
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/SwingKeyboard.java:137 the table that stays,
  keyed by Swing key codes, which really are Swing's numbers and belong to the host.
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/SwingKeyboard.java:159 where both lookups met.

## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

none: looked for other tables from characters or codes to InputKey with
grep "Map<Integer, Input.InputKey>|HostKey(" over machine/ and oozx-plugins/; only the key code table is left,
and that one is the host's knowledge, not the enum's.

## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

Single owner of a concept: the character each key types is the key's own fact, so the key answers it and no
table beside it repeats it, where the two could drift apart.

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

covered-by: maps-keyed-alike
The rule flagged SwingKeyboard for two maps keyed alike by Integer; asking what the second key meant showed
it was the enum's own value, so the missing owner was InputKey itself and the map went away entirely.
