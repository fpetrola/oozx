# Autopsy 2026-11-11-condition-repeated (condition-repeated)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `condition-repeated` that went away (written by cerco):

- `machine/core/src/main/java/com/fpetrola/oozx/speccy/machine/Spectrum.java|com.fpetrola.oozx.speccy.machine.Spectrum.unattachedPort(int)|line < 0 || line >= _.HEIGHT || into >= getTimings().frame().line().picture()`
- `machine/core/src/main/java/com/fpetrola/oozx/speccy/machine/Spectrum.java|com.fpetrola.oozx.speccy.machine.Spectrum.waitAt(long, Waits)|line < 0 || line >= _.HEIGHT || into >= getTimings().frame().line().picture()`

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

Both the contention wait and the floating bus work out where the beam is from a T-state - so many lines and so
many T-states into the line after the first pixel - and then ask whether that place is in the picture. The question
had no name, so each wrote the three-part condition out, and a change to what counts as the picture (the line's
picture span, the display's height) had to be made twice and kept equal by hand. Spectrum.onThePicture(line, into)
says it once. The position itself is still computed twice; a Beam value would hold it, which is left until it can be
measured on the contended path, where every memory access goes.

model: knowledge-without-one-owner


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/core/src/main/java/com/fpetrola/oozx/speccy/machine/Spectrum.java:216 waitAt, the contended path asking it.
- machine/core/src/main/java/com/fpetrola/oozx/speccy/machine/Spectrum.java:236 unattachedPort, the floating bus
  asking it.
- machine/core/src/main/java/com/fpetrola/oozx/speccy/machine/MachineTimings.java:38 the frame whose line's picture
  span bounds the question.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

none: searching machine/ (sources, not generated) for `line < 0 || line >=` and for `>= display.HEIGHT` or
`>= Display.HEIGHT` finds nothing left after the fix.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

One owner for one fact: a condition that means something is written once under the name of what it means.


## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause; a person files it with mvn cerco:approve, and
      while one waits every other autopsy proposes a rule
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->

rule: condition-repeated
The same condition over project objects in two or more members. It flagged
machine/core/src/main/java/com/fpetrola/oozx/speccy/machine/Spectrum.java:221 before this fix; cerco 0.15.0 has it.

