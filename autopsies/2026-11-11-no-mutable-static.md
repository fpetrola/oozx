# Autopsy 2026-11-11-no-mutable-static (no-mutable-static)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `no-mutable-static` that went away (written by cerco):

- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/SwingKeyboard.java|com.fpetrola.oozx.speccy.desktop.SwingKeyboard.unicodeKeysymsHash`

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

SwingKeyboard's table from host character to Spectrum key was a static HashMap that every new keyboard filled
again from a constant array, and end() cleared it for every keyboard at once: shared mutable state standing in for a
constant. The table never changes - it is Swing's characters against the Spectrum's keys - so it was a value built
at the wrong time and owned by nobody, with a clear that could leave every other machine window typing nothing. Both
lookup tables are now built once, unmodifiable, from the arrays that already said them, and the end markers that
only served the fill loops are gone.

model: knowledge-without-one-owner


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/SwingKeyboard.java:232 the two tables made once from
  the arrays.
- machine/app/src/main/java/com/fpetrola/oozx/speccy/peripherals/SpeccyEmulatorCore.java:144 a keyboard is made per
  machine window, which is how one window's end() could empty another's characters.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

none in machine/app: searching for `private static Map` and `static .* = new HashMap` in machine/*/src/main finds
no other static map filled by instances.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

A constant is built once and cannot change; state shared through a static field belongs to no one and anyone can break it.


## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause; a person files it with mvn cerco:approve, and
      while one waits every other autopsy proposes a rule
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->

rule: no-mutable-static
A static field that is not final, or a final static collection that is mutated. It flagged
machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/SwingKeyboard.java:37 before this fix; cerco has it.

