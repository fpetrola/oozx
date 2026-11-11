# Autopsy 2026-11-11-maps-keyed-alike (maps-keyed-alike)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `maps-keyed-alike` that went away (written by cerco):

- `machine/host/input/src/main/java/com/fpetrola/oozx/speccy/modules/joystick/Joystick.java|com.fpetrola.oozx.speccy.modules.joystick.Joystick|maps keyed by JoystickType`

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

Joystick kept two maps keyed by JoystickType: one with the joystick of each kind, and one with when the machine
last read each of them. The second was a property of the joysticks with ports - the machine reading a Kempston is the
Kempston being read - kept beside them in a parallel map because the joysticks had nowhere to hold it, and every
read had to put the time in one map and the value from the other. A PortedJoystick now remembers when it was read
and says whether that was a moment ago; Joystick keeps one map and asks the joysticks which one is in use.

model: knowledge-without-one-owner


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/host/input/src/main/java/com/fpetrola/oozx/speccy/modules/joystick/PortedJoystick.java:75 the joystick
  saying whether it was read lately, and :33 the moment it remembers.
- machine/host/input/src/main/java/com/fpetrola/oozx/speccy/modules/joystick/Joystick.java:135 inUse asks the
  joysticks instead of a second map.
- machine/host/input/src/main/java/com/fpetrola/oozx/speccy/modules/joystick/JoystickKind.java:25 the kinds without a
  port are never read, so they answer no by default.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

- machine/host/input/src/main/java/com/fpetrola/oozx/speccy/modules/keyboard/RecreatedLayout.java:1 and
  machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/SwingKeyboard.java:1 keep maps keyed alike by key code,
  flagged by the same rule; not yet read closely enough to say they are one map each.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

One owner for one fact: what is known about a thing is kept by that thing, not in a map beside the map of things.


## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause; a person files it with mvn cerco:approve, and
      while one waits every other autopsy proposes a rule
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->

rule: maps-keyed-alike
Two or more maps in one type keyed by the same type. It flagged
machine/host/input/src/main/java/com/fpetrola/oozx/speccy/modules/joystick/Joystick.java:1 before this fix; cerco
0.15.0 has it.

