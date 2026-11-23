# Autopsy 2026-11-23-unpromised-member (unpromised-member)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
.cerco/autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `unpromised-member` that went away (written by cerco):

- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java|ZXSpectrumDesktopApp.createNewEmulator/2`

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

The ways of building a machine window grew one public overload per combination of what came with it (a file, a
game), each forwarding to the full one, although the only caller of the game overload sits in the same class and
already has both values at hand. The window has one way of being made, with what it was opened from; the overload
named a combination instead of a thing, and nobody outside asked for it, so it was a promise nobody needed.

model: shape-repeated


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:1132 — the only caller,
  which now passes the game to the full method directly.
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:1473 — the full method the
  overloads forward to, which takes the file and the game together.
- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Desk.java:53 — Desk.Game, what a window
  opened from a game carries; the desk interface plugins use offers no way to build a machine window with it, so
  the overload was never part of what plugins can ask.
- No caller in oozx-plugins: searched for `createNewEmulator(` there and in machine/, outside target and worktrees.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:1465 and :1469, the two
  remaining forwarding overloads; they have several callers in the class, so they are the next ones to fold.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

A class promises only what others need of it: one entry point taking what the operation needs, rather than one
public overload per combination of its arguments.


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

covered-by: unpromised-member
The rule already marked this overload as a public member nobody outside the class asked for; following what it says, make it private or drop it, is what led to folding the overload into its only caller, so no new rule is needed here.

