# Autopsy 2026-11-11-no-ignored-result (no-ignored-result)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `no-ignored-result` that went away (written by cerco):

- `machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/EmulatorCore.java|com.fpetrola.oozx.speccy.peripherals.EmulatorCore.get(String)|isTurboMode()`

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

The default branch of the switch in the anonymous Values.get answered isTurboMode() for any name that was not one of
the other three, so reading turbo was spelled as a fall-through rather than as the turbo setting's own reading: the
call stood as the value of a switch arm that the rule read as a result left alone. It was a symptom of the same missing
object as the switches: with a Property per setting, turbo's reading is this::isTurboMode in its own entry and no
default branch catches it.

model: concept-without-a-class


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/EmulatorCore.java:55 turbo's reading
  is its own entry now.
- machine/core/src/main/java/com/fpetrola/oozx/config/Settings.java:176 Values.get answers from the property named,
  with no default case.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

none in machine: searching for switch default branches that call a getter for one
remaining case (`default -> is`/`default -> get`) over machine/ found only this one, now gone.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

Every case is said by its own name: a default branch that quietly stands for one particular case hides it.


## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause; a person files it with mvn cerco:approve, and
      while one waits every other autopsy proposes a rule
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->

same-as: 2026-11-11-no-switch

