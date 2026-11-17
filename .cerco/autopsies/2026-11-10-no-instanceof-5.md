# Autopsy 2026-11-10-no-instanceof-5 (no-instanceof)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `no-instanceof` that went away (written by cerco):

- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.openFor(java.io.File)|instanceof Opens`
- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.opensFor(java.io.File)|instanceof Opens`
- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.playInto(MachineFrame, String, java.util.concurrent.Callable)|instanceof Opens`
- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|com.fpetrola.oozx.speccy.desktop.ZXSpectrumDesktopApp.playingInto(JInternalFrame)|instanceof Opens`

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

The desk had no way to know a window as a player: Equipment.open() hands a MachineFrame out, and the plugins' players
(the cassette deck, the RZX player, the dock) say what they are only by implementing Opens. So every place of the desk
that needed a player asked each window its type: the free one for a new file, the one a recording is asked into, the
one clipped onto a machine. MachineFrame now answers player() itself. The type test is left once, inside it, as the
bridge for the players already published; a player that overrides player() needs no test at all.

model: dispatch-from-outside


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/MachineFrame.java:48 the window answers whether
  it is a player, which the desk used to work out.
- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Equipment.java:34 a kind hands its window out as a
  MachineFrame, the reason the desk never had the player typed.
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:434 and :976 the openers now ask
  the window for its player.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

none in machine: a search for `instanceof Opens` and `(Opens)` over machine/ finds only the bridge. In oozx-plugins the
three players (DockFrame, RzxFrame, CassetteFrame) can override player() and return themselves once they build against
this kit, and then the bridge can go.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

Polymorphism over type tests: an object says what role it plays; its users do not test its type to find out.


## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause; a person files it with mvn cerco:approve, and
      while one waits every other autopsy proposes a rule
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->

same-as: 2026-11-10-no-instanceof-3

