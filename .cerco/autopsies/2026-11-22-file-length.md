# Autopsy 2026-11-22-file-length (file-length)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
.cerco/autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `file-length` that went away (written by cerco):

- `machine/core/src/main/java/com/fpetrola/oozx/config/RomFiles.java|file`

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
RomFiles carried a second HTTP client of its own: download() opened an HttpClient, said who was asking,
read in pieces to tell a progress bar, and mapped failures to its exception, while DownloadAndUnzip already
owned fetching whatever is at a URL for the rest of the emulator. Fetching a published file had two owners,
and the copy inside RomFiles is what pushed it over its length. RomFiles now asks DownloadAndUnzip for the
bytes and keeps only what is about ROMs: which source, the piece inside the image, the digest, the kept copy.
model: knowledge-without-one-owner


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->
- machine/core/src/main/java/com/fpetrola/oozx/speccy/media/DownloadAndUnzip.java:290 — downloadFile, the
  fetcher everything else used; it now says who is asking, which was the one thing RomFiles' copy had that it lacked.
- machine/core/src/main/java/com/fpetrola/oozx/config/RomFiles.java:336 — fetched(), which now asks DownloadAndUnzip
  and keeps the ROM-specific checks.
- machine/app/src/main/java/com/fpetrola/oozx/speccy/desktop/RomNotShippedDialog.java:54 — the only listener of the
  progress the copy reported; ROMs are 2 to 64 KB, so an indeterminate bar says the same.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->
- machine/core/src/main/java/com/fpetrola/oozx/speccy/media/DownloadAndUnzip.java:290 is itself duplicated by the
  games tool's DownloadViewer in oozx-plugins (tools/games, DownloadViewer.java:141, its own openConnection). Found
  by grepping HttpClient, HttpURLConnection and openConnection over machine/, oozx-plugins and plugdesk.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->
One owner per concept: fetching what is published at a URL is one responsibility, and every kind of file
the emulator brings down goes through it instead of growing a fetcher of its own.


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
covered-by: file-length — the length ceiling on RomFiles is what made adding two methods impossible and sent me
looking for what in the file was not about ROMs; the answer was a whole HTTP client that DownloadAndUnzip already
owned, so the rule that exists already led to the missing owner without a new one.
