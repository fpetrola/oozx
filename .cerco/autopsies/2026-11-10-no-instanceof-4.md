# Autopsy 2026-11-10-no-instanceof-4 (no-instanceof)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `no-instanceof` that went away (written by cerco):

- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.opensFor(java.io.File)|instanceof Opens`

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
-->

opensFor tested twice for Opens: once to find an empty player among the windows of the kind that opens the file,
once to hand that window back as a player. The second test existed only because opensFor returned a player while
its twin openFor returned the window, so each narrowed on its own. Now opensFor returns the window and the openers
narrow it where they use it. What is missing underneath is a desk that keeps its players typed: a window comes from
Equipment.open() as a MachineFrame and the plugins' players say what they are only by implementing Opens, so every
place that needs a player asks the type.

model: dispatch-from-outside


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/WhatIsPluggedIn.java:101 who opens a file is now asked
  of what is plugged in, once; before, the desk walked its equipment for it in three places.
- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Equipment.java:34 a kind hands out its window as
  a MachineFrame, never as the player it is, which is why the desk has to narrow it.
- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Opens.java:41 empty() is what makes a window
  reusable for a new file, and only a player answers it.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:434 opening a chosen file
  narrows the empty window to a player.
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:785 and :986 the same
  question asked of other windows. All of them go away together when a MachineFrame can hand itself over as a player,
  which the published players would have to implement.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

Polymorphism over type tests: a window hands itself over as what it is instead of being asked its type.


## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause; a person files it with mvn cerco:approve, and
      while one waits every other autopsy proposes a rule
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->

same-as: 2026-11-10-no-instanceof-3

