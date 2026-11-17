# Autopsy 2026-11-11-named-by-text (named-by-text)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `named-by-text` that went away (written by cerco):

- `machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/EmulatorCore.java|com.fpetrola.oozx.speccy.peripherals.EmulatorCore.choicesFor(String)|"model"`
- `machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/EmulatorCore.java|com.fpetrola.oozx.speccy.peripherals.EmulatorCore.choicesFor(String)|"processor"`
- `machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/EmulatorCore.java|com.fpetrola.oozx.speccy.peripherals.EmulatorCore.choicesFor(String)|"romSet"`
- `machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/EmulatorCore.java|com.fpetrola.oozx.speccy.peripherals.EmulatorCore.get(String)|"model"`
- `machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/EmulatorCore.java|com.fpetrola.oozx.speccy.peripherals.EmulatorCore.get(String)|"processor"`
- `machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/EmulatorCore.java|com.fpetrola.oozx.speccy.peripherals.EmulatorCore.get(String)|"romSet"`
- `machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/EmulatorCore.java|com.fpetrola.oozx.speccy.peripherals.EmulatorCore.saidAbout(String)|"model"`
- `machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/EmulatorCore.java|com.fpetrola.oozx.speccy.peripherals.EmulatorCore.saidAbout(String)|"processor"`
- `machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/EmulatorCore.java|com.fpetrola.oozx.speccy.peripherals.EmulatorCore.saidAbout(String)|"romSet"`
- `machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/EmulatorCore.java|com.fpetrola.oozx.speccy.peripherals.EmulatorCore.set(String, Object)|"model"`
- `machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/EmulatorCore.java|com.fpetrola.oozx.speccy.peripherals.EmulatorCore.set(String, Object)|"processor"`
- `machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/EmulatorCore.java|com.fpetrola.oozx.speccy.peripherals.EmulatorCore.set(String, Object)|"romSet"`
- `machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/EmulatorCore.java|com.fpetrola.oozx.speccy.peripherals.EmulatorCore.typeOf(String)|"turbo"`

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

The names "model", "romSet", "processor" and "turbo" were the only thing that held each of the machine's own
settings together: four methods of an anonymous Described and Values each matched the same names to say a different
thing about the same setting. The name was standing in for an object, so the knowledge of one setting was spread over
every method that took a property name. A Settings.Property per setting carries that knowledge in one place; the name
is now written once, where the property is made, and only looked up by the map Settings.of keeps.

model: concept-without-a-class


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/core/src/main/java/com/fpetrola/oozx/config/Settings.java:190 SaidWhole answers Described's name-keyed
  questions from the properties, the one place that looks a property up by its name.
- machine/core/src/main/java/com/fpetrola/oozx/config/Settings.java:48 Described asks everything by property name,
  the interface the plugins' devices implement and the reason the names cannot simply go.
- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/EmulatorCore.java:55 each name written once.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/peripherals/MockEmulatorCore.java:116 screen options
  told by name ("scanlines", "smoothing"...) in a chain of equals.
- machine/app/src/main/java/com/fpetrola/oozx/speccy/peripherals/SpeccyEmulatorCore.java:181 the general options
  ("turbo", "speed", "mute", "pause") told by name the same way.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

A name is not a model: what is asked of a thing by its name in many places belongs to an object that is that thing.


## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause; a person files it with mvn cerco:approve, and
      while one waits every other autopsy proposes a rule
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->

same-as: 2026-11-11-no-switch

