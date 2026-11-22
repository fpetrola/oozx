# Autopsy 2026-11-22-absence-as-state (absence-as-state)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
.cerco/autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `absence-as-state` that went away (written by cerco):

- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/RomNotShippedDialog.java|com.fpetrola.oozx.speccy.desktop.RomNotShippedDialog.window`

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
RomNotShippedDialog.window was tested for null in the methods the download called back while it arrived,
because those callbacks could come before the window was open or after it closed. The progress callbacks existed
only because RomFiles fetched on its own; once fetching moved to DownloadAndUnzip and the progress callbacks went,
the window is only touched between open() and close(), so the phase it encoded is no longer asked about.
model: state-without-an-object


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/RomNotShippedDialog.java:54 — bring(), which opens the
  window, waits on it and closes it in one place.
- machine/core/src/main/java/com/fpetrola/oozx/config/RomFiles.java:75 — Consent.bring, the one moment the dialog is
  asked for anything now.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/RomNotShippedDialog.java:119 — close() still checks
  it, since done() may run after the window was never shown; found by reading every use of window in the file.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->
An object is whole: a collaborator that exists only during one phase belongs to an object of that phase,
not to a field that is null the rest of the time.


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
covered-by: absence-as-state — the rule named the window field as a phase kept in null, which led to asking why
anything touched the window outside open() and close(); the answer was the progress callbacks of a second fetcher,
and removing that fetcher removed the phase checks without any further rule.
