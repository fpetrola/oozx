# Autopsy 2026-11-23-no-null-return (no-null-return)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
.cerco/autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `no-null-return` that went away (written by cerco):

- `machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Desk.java|com.fpetrola.oozx.speccy.devices.Desk.keep(Session, JInternalFrame, File, int)|return null`
- `machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Session.java|com.fpetrola.oozx.speccy.devices.Session.bringBack(java.util.Map)|return null`

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

"No plugin writes sessions" was said by a null session, and every window paid for it: with tool-mappings not
installed Desk.keep returned null for each window, so not only the machines but the catalogues and every other
desk window were forgotten at close. Keeping a window by its name is the desk's own job and never needed the SZX
writer; only machines do. The absent session is a state that deserved an object: a session that writes no
machines and keeps every other window by the name the desk gave it, which is also what the interface's
defaults now do instead of answering nothing.

model: state-without-an-object


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:1546 — session() was
  orElse(null): the person's ~/.oozx has ten plugins and no tool-mappings, so every close kept nothing
  (config.json openWindows empty, no session folder).
- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Desk.java:220 — keep returned null before
  looking at the window; the non-machine branch only asked the session for Desk.inside(window).
- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/Desk.java:243 — bringBack checked the same
  null twice.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:1601 putMachinesAway
  passed the same null session; it gets the same object now. Found by reading every caller of session().


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

A collaborator that may be missing is replaced by a null object that answers as much as can be answered without
it (introduce null object), so the callers never branch on whether it is there.


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

same-as: 2026-11-11-no-null-return

