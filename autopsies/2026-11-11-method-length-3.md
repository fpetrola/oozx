# Autopsy 2026-11-11-method-length-3 (method-length)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `method-length` that went away (written by cerco):

- `machine/spectrum/src/main/java/com/fpetrola/emulation/helpers/machine/MachineTypes.java|com.fpetrola.emulation.helpers.machine.MachineTypes.MachineTypes(int)`

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

The constructor MachineTypes(int) was long because it was a switch with six cases of fifteen assignments each,
the same fields in the same order with different numbers: a table spelled out as code. Most of the table was unread,
and what was read fits the four-argument constructor the other constants already use, so the long method disappears
rather than being split.

model: shape-repeated


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/core/src/main/java/com/fpetrola/oozx/speccy/machine/MachineTimings.java:38 where a machine's frame, its
  lines and its interrupt are measured now, which is what left MachineTypes' copies of them unread.
- machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/machine/Machine.java:119 what a snapshot's model is still
  asked for: which machine claims it, through its code model.
- searched for readers of every removed field and getter in this tree, oozx-plugins, the traduccion branch and the
  200 published plugin jars: none outside MachineTypes.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

none: the remaining constructors and methods of MachineTypes are one to three lines.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

A table is data: written as repeated assignments in a switch it hides both its shape and which columns matter.


## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause; a person files it with mvn cerco:approve, and
      while one waits every other autopsy proposes a rule
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->

same-as: 2026-11-11-method-length

