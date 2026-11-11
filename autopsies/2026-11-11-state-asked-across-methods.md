# Autopsy 2026-11-11-state-asked-across-methods (state-asked-across-methods)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `state-asked-across-methods` that went away (written by cerco):

- `machine/ui/src/main/java/com/fpetrola/oozx/speccy/windows/Edge.java|com.fpetrola.oozx.speccy.windows.Edge.flat`

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

Edge kept whether it runs flat (top, bottom) or upright (left, right) as a boolean, and every measure asked it:
along, run, across, thickness and the rectangle laid on it each chose between x and y, or width and height, with the
same `flat ?`. The two cases are one geometry seen two ways - an upright border is a flat one with the rectangle
turned over its diagonal - but nothing said so, so the turning was written out per measure. Edge.Axis says it once:
FLAT sees a rectangle as it is, UPRIGHT sees it transposed, and every measure reads the flat answer off what the
axis sees.

model: state-without-an-object


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/ui/src/main/java/com/fpetrola/oozx/speccy/windows/Edge.java:30 the axis, the one place that turns a
  rectangle.
- machine/ui/src/main/java/com/fpetrola/oozx/speccy/windows/AttachedFrame.java:365 the docking asks flat() to size a
  window along the frame, the reason flat() stays public.
- machine/ui/src/test/java/model/tests/ui/EveryEdgeMeasuresAsBeforeTest.java written first against the boolean, it
  pins every measure of every edge.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

- machine/ui/src/main/java/com/fpetrola/oozx/speccy/windows/AttachedFrame.java:71 Dock asks flat() of its edge and
  is itself decided as FREE in eight members: the docked or free mode of a window, kept as a constant others test.
- machine/ui/src/main/java/com/fpetrola/oozx/speccy/windows/AttachedFrame.java:91 compact, a boolean asked across
  its methods, flagged by the same rule.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

A mode is an object: what changes with it is asked of it once, not decided again in every method that reads the flag.


## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause; a person files it with mvn cerco:approve, and
      while one waits every other autopsy proposes a rule
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->

rule: state-asked-across-methods
A field of a type decided on in three or more of its methods. It flagged Edge.flat
(machine/ui/src/main/java/com/fpetrola/oozx/speccy/windows/Edge.java:30 is where the axis now stands) and still flags
machine/ui/src/main/java/com/fpetrola/oozx/speccy/windows/AttachedFrame.java:91; cerco 0.15.0 has it.

