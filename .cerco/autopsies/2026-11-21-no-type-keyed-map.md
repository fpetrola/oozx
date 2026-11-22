# Autopsy 2026-11-21-no-type-keyed-map (no-type-keyed-map)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
.cerco/autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `no-type-keyed-map` that went away (written by cerco):

- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.ZXSpectrumDesktopApp(java.util.function.BiFunction, Function)|java.util.Map<Class<?>, String>`

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

Where each kind of offered thing goes in the menus was decided by the desk in a table from role class to menu, as if
the desk owned that knowledge, while the roles themselves said nothing about where they go. Each role is the one that
knows: an Equipment is something plugged into a machine and so lives under Emulator/Equipment, a page that explains
goes under Help. Moving that fact onto each role, as an annotation plugdesk reads, leaves the desk only naming which
roles it shows, and a role brought by a plugin no longer needs the desk to learn a new key.
model: knowledge-without-one-owner

## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Equipment.java:30 the role now says its own menu, the fact the table held for it
- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Explains.java:24 Explains extends DeskEquipment and still goes elsewhere, so the order of the roles list decides as the table's order did
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:534 the desk now passes the roles only; plugdesk's Offered reads each one's place, and the Look role carries its own in plugdesk

## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:611 machines are placed by the desk naming "Emulator/Machines", since a machine model is not a plugin role the annotation can sit on yet; looked with grep for maps keyed by Class in this tree

## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

Single owner of a concept: the place a kind of thing has in the application is a fact about that kind, so it is said
once on the kind and read from there, not kept in a lookup table by whoever happens to build the menus.

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

covered-by: no-type-keyed-map
The rule marked the map from role class to menu and asked for the behaviour to live on the type; asking which type
knows where it goes is what showed that each role owns its place, and the annotation each role carries, read by
Offered, is that fact moved to its owner with no table left behind.
