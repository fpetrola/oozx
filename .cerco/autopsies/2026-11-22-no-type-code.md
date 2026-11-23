# Autopsy 2026-11-22-no-type-code (no-type-code)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
.cerco/autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `no-type-code` that went away (written by cerco):

- `machine/core/src/main/java/com/fpetrola/oozx/speccy/peripherals/PeripheralRegistry.java|com.fpetrola.oozx.speccy.peripherals.PeripheralRegistry.register(Peripheral)|registry.put(peripheral.getClass(), new Registration(peripheral))`

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

The registry learned which kind each device was by asking the instance for its class, because the
only thing an Extension said about its devices was a Guice binding into Set<Peripheral>: the kinds
reached the registry already made, as anonymous members of a set, and the class was the only key
left. Nobody owned the fact "this jar brings these devices" as data, so a machine made before a jar
arrived could never learn it. Extension#devices now says it, and the registry makes each kind it is
told about with the machine's own injector, keyed by the kind it was given.
model: knowledge-without-one-owner


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/core/src/main/java/com/fpetrola/oozx/Speccy.java:189 - init() used to hand the injected Set<Peripheral> to the registry one instance at a time; the set existed only to be passed on.
- machine/core/src/main/java/com/fpetrola/oozx/Extension.java:36 - the extension had no way to say what it brings except binding it, which only an injector being made can hear.
- machine/devices/kit/src/main/java/com/fpetrola/oozx/speccy/devices/DeviceFrame.java:65 - the window asks the registry by kind; with the kind coming from the instance, a kind whose jar arrived later was simply absent.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

- machine/core/src/main/java/com/fpetrola/oozx/speccy/peripherals/PeripheralRegistry.java:154 - update() still switches devices on by device.peripheral.getClass(); the Registration could keep the kind it was made for and answer it.
- machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/machine/Machine.java:68 - the machine models still arrive only as a Set<Spectrum> binding, so a machine plugin that arrives later has the same blindness for models.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

Single owner of a concept: what a jar brings is said once, as data, by the jar, and everything that needs it - the build and a machine that was already running - asks that one place.


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

covered-by: no-type-code
The rule marked registry.put(peripheral.getClass(), ...) and told that a kind recovered from an instance stands for a type of the project that is missing at the boundary where things arrive. Following that is what led here: the kind is now said where devices come in (Extension#devices) and handed to the registry, so nothing has to ask an instance what it is.

