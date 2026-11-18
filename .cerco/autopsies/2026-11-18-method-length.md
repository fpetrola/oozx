# Autopsy 2026-11-18-method-length (method-length)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
.cerco/autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `method-length` that went away (written by cerco):

- `machine/host/input/src/main/java/com/fpetrola/oozx/speccy/modules/keyboard/PcLayout.java|com.fpetrola.oozx.speccy.modules.keyboard.PcLayout.PcLayout()`

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

The PC layout was written as one constructor listing every key, one line each, mixing four different
kinds of fact: letters and digits, which are the same key on both keyboards and follow a rule; the symbols
a Spectrum types with symbol shift; the PC's modifiers; and the cursor keys. With no step named for each
kind, the rule behind letters and digits was spelled out key by key, and the list only grew.

model: procedure-instead-of-model

## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/host/input/src/main/java/com/fpetrola/oozx/speccy/modules/keyboard/PcLayout.java:37 keys(): letters
  and digits now come from their rule, by the enum constants' names, instead of 36 lines one per key.
- machine/host/input/src/main/java/com/fpetrola/oozx/speccy/modules/keyboard/PcLayout.java:108 symbol(), the
  one shape every symbol-shift entry has, so that list says only which host key goes with which key.
- machine/host/input/src/main/java/com/fpetrola/oozx/speccy/modules/input/Input.java:303 where a layout's
  combination is pressed: the reason missing entries (the double quote, the open bracket) typed nothing.

## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

none: looked at the other KeyLayout, RecreatedLayout, which has two put calls and no long list, and searched
machine/host/input for other constructors filling an EnumMap line by line; there are none.

## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

Cohesion: a method does one thing and names it; a rule that covers many cases is written once as the rule,
not repeated as one line per case.

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

covered-by: method-length
The rule flagged the constructor once the symbols the typed text needed were added; splitting it by what each
group of keys is showed the letters and digits were a rule written out by hand, which is the missing model.
