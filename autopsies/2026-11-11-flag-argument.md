# Autopsy 2026-11-11-flag-argument (flag-argument)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `flag-argument` that went away (written by cerco):

- `machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/sound/blip/BlipBuffer.java|com.fpetrola.oozx.speccy.modules.sound.blip.BlipBuffer.clear(boolean)|entireBuffer`
- `machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/sound/blip/BlipBuffer.java|com.fpetrola.oozx.speccy.modules.sound.blip.BlipBuffer.readSamples(int[], int, boolean)|stereo`

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

BlipBuffer came over from Blip_Buffer with the choices its C original offers every program that links it: mono
or stereo output, a partial or a whole clear. This emulator only ever makes one of each choice - every caller asks
for stereo and every clear is whole - so the flags stood for variation the model does not have, and each call
carried a literal that meant nothing to the caller. Without the flags the buffer says what it does here: it reads
interleaved stereo and it clears all of itself. The flag rule saw the parameter; what it could not say is that
every caller passes the same constant, which is what made this dead rather than a mode.

model: variation-as-data


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/host/sound/src/main/java/com/fpetrola/oozx/speccy/modules/sound/Dac.java:62 and
  machine/machines/ula/src/main/java/com/fpetrola/oozx/speccy/devices/ula/Beeper.java:81 the two callers in machine,
  both asking for stereo; the published AY plugin asks for stereo too, which is why BlipSynth keeps the old signature.
- machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/sound/blip/BlipSynth.java:70 the read every sound
  device goes through, stereo only now.
- machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/sound/blip/BlipBuffer.java:124 clear, called only to
  empty the whole buffer (at :56 and :76, and by SpeedChangeKeepsSoundTest).


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

none in machine: every flag-argument finding left in the baseline was checked for its callers; the others are
given a value that changes (a toggle's state, whether a device is pluggable, hard or soft reset), not one literal.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

Say only the variation the model has: a parameter every caller sets the same way is not a choice, it is noise in each call.


## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause; a person files it with mvn cerco:approve, and
      while one waits every other autopsy proposes a rule
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->

rule: flag-argument
A boolean parameter that decides between two behaviours inside the method. It flagged
machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/sound/blip/BlipBuffer.java:87 before this fix; cerco
0.15.0 has it. It would tell more by saying when every call in the project passes the same literal.

