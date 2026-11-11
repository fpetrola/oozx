# Autopsy 2026-11-11-reached-through (reached-through)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `reached-through` that went away (written by cerco):

- `machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/z80/Disassembly.java|com.fpetrola.oozx.speccy.modules.z80.Disassembly.byteAt(int)|machine.memory`
- `machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/z80/Disassembly.java|com.fpetrola.oozx.speccy.modules.z80.Disassembly.read(int, int)|machine.memory`

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

Disassembly kept the whole machine although all it ever does with it is read memory, so each read went
machine.memory.peek: a reader of instructions knowing that a Speccy holds a bus. What it collaborates with is the
memory bus, and that bus is final on the machine, so the right thing to hold is the bus itself, taken once when the
listing is made. The constructor still takes the machine because the plugins build listings that way; what changed
is what the listing keeps and talks to.

model: behaviour-not-on-owner


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/core/src/main/java/com/fpetrola/oozx/Speccy.java:65 memory is a final field, so taking it once in the
  constructor sees the same bus a later read would.
- machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/z80/Disassembly.java:49 the listing now holds the bus
  and reads it directly (:55 and :68).


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

- machine/bridge/src/main/java/com/fpetrola/oozx/speccy/bridge/LocalLibretroCore.java:255 the bridge reaches
  speccy.memory in four members; it translates the machine for Fuse, so holding the bus there would be the same fix.
- machine/app/src/main/java/com/fpetrola/oozx/speccy/peripherals/SpeccyEmulatorCore.java:81 reaches processors,
  roms, sound and loop through speccy: an adapter that talks to the machine's parts rather than to the machine.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

Know only what you collaborate with: an object holds the part it talks to, not the whole that contains it.


## Candidate rule

<!-- One of:
rule: <new-rule-id>  then what it would detect, what it would tell the agent, and an example path:line
none: why no deterministic check could catch this cause; a person files it with mvn cerco:approve, and
      while one waits every other autopsy proposes a rule
same-as: <id of a filed autopsy>  when one already proposed the rule; the sections above still describe
this case -->

rule: reached-through
A path `a.b.m()` through another object's field repeated in two or more members. It flagged
machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/z80/Disassembly.java:55 before this fix; cerco 0.15.0
has it.

