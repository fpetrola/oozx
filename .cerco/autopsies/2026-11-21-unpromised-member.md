# Autopsy 2026-11-21-unpromised-member (unpromised-member)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
.cerco/autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `unpromised-member` that went away (written by cerco):

- `machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/OOSpectrumLauncher.java|OOSpectrumLauncher.init/0`

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

The launcher was written as an object that is built and then told to start: `new OOSpectrumLauncher().init()`,
two steps that only its own `main` ever takes. `init` was left public as if something else could start a desk on
an existing launcher, but nothing does and no interface promises it: starting the desk is the entry point's own
business, and the files a desk starts with arrive through `main`'s arguments. Now `init` takes those files and is
private, which is what it always was in use.
model: protocol-by-convention

## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/OOSpectrumLauncher.java:43 main, the only caller of init, which now passes it the files it was started with
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/OOSpectrumLauncher.java:99 inItsWindow, where a file from the arguments, a dropped one and one from the system's file manager take the same way in
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:401 open(String), the menu's way in that a file arriving from outside reuses; the desk opens files, the launcher only starts it

## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/OOSpectrumLauncher.java:113 createBareSpeccy is public and called only from inside the launcher, at line 73; looked with grep over this tree, ../oozx-plugins and ../plugdesk

## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

Information hiding: a member is published only when something outside needs it, so the class's surface says
what it really offers and a two-step start that only the class itself performs stays inside it.

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
The rule marked init as a public member that no type promises, and following that question - who outside calls it -
is what showed the missing model: nobody does, the start is the entry point's own two steps, and the files it starts
with belong to main's arguments. Making it private and giving it the files is the fix the rule's question led to.
