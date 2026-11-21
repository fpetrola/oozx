# Autopsy 2026-11-20-no-instanceof (no-instanceof)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
.cerco/autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `no-instanceof` that went away (written by cerco):

- `machine/core/src/main/java/com/fpetrola/oozx/speccy/media/DownloadAndUnzip.java|com.fpetrola.oozx.speccy.media.DownloadAndUnzip.downloadFile(URL)|instanceof HttpURLConnection`

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

A download treated "where it comes from" as two kinds of connection: one over the web, whose answer has a status, and
one from a file on disk, which has none. Rather than asking the connection what it answered - which every
URLConnection can say, through the status line it was given or the lack of one - the code checked which subclass it
had in hand and only asked the web one. The answer a source gives was the missing question on the common type: the
JDK already had it, the code reached past it to a subtype. The status is now read from the connection itself, the same
way for both, and a file is simply a source that answered nothing and is there.
model: dispatch-from-outside


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/core/src/main/java/com/fpetrola/oozx/speccy/media/DownloadAndUnzip.java:291 downloadFile, now connecting and asking the status of any connection
- machine/core/src/main/java/com/fpetrola/oozx/speccy/media/DownloadAndUnzip.java:326 statusOf: the status line a web server answers with, none from a file
- machine/core/src/main/java/com/fpetrola/oozx/speccy/media/DownloadAndUnzip.java:98 fetchAll writes whatever downloadFile brings, for web addresses and for file: ones alike
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/ZXSpectrumDesktopApp.java:1090 a file on disk handed to fetchAll as a file: URL, which is why both kinds arrive at the same door


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

- machine/core/src/main/java/com/fpetrola/oozx/speccy/media/DownloadAndUnzip.java:354 cannotReach tells an unknown host from a timeout by testing the exception's class, where the failure could say what it means for the person
- machine/core/src/main/java/com/fpetrola/oozx/config/RomFiles.java:391 an InterruptedException found by a type test among the causes of a failed download


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

Polymorphism over type tests: ask the common type the question every subtype can answer, instead of inspecting which
subtype it is and asking only that one.


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

covered-by: no-instanceof
The rule marked `connection instanceof HttpURLConnection` and its advice - the object decides, the caller asks the
common type - is exactly what led here: URLConnection already answers its status line for every kind of source, so the
fix was to ask it and drop the subtype test. No other rule was needed to find the missing model.

