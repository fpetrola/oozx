# Autopsy 2026-11-11-no-static-method (no-static-method)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `no-static-method` that went away (written by cerco):

- `machine/ui/src/main/java/com/fpetrola/oozx/speccy/screen/SpeccyScreen.java|com.fpetrola.oozx.speccy.screen.SpeccyScreen.getScaledImage(BufferedImage, int, int, boolean)`

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

SpeccyScreen.getScaledImage was a static helper left from before the screen scaled its picture through the
effects and scalers in WaysOfDrawing: nothing in the project or in any published plugin called it any more. Being
static and public it had no owner to notice it was dead, so it stayed, with a static method standing where an object's operation now does the work. Removing it removes the symptom
and its cause together: there was no model missing, only an old procedure the model had already replaced.

model: procedure-instead-of-model


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/ui/src/main/java/com/fpetrola/oozx/speccy/screen/SpeccyScreen.java:324 the screen draws its picture through
  the effects and the scalers, never through the removed method.
- machine/ui/src/main/java/com/fpetrola/oozx/speccy/screen/WaysOfDrawing.java:1 where the ways of scaling a picture
  live now, which is what made the old helper dead.
- the 200 published plugin jars in ~/.oozx/plugin-cache were searched for the method's name and none calls it.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

none: searching machine/ for callers of every remaining public static method of SpeccyScreen finds none left.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

Dead code is deleted, not kept: what nothing calls only hides what the model really does.


## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause; a person files it with mvn cerco:approve, and
      while one waits every other autopsy proposes a rule
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->

rule: no-static-method
A static method of the project that is not a factory. It flagged SpeccyScreen.getScaledImage before this fix and
still flags machine/ui/src/main/java/com/fpetrola/oozx/speccy/screen/SpeccyScreen.java:239; cerco has it.

