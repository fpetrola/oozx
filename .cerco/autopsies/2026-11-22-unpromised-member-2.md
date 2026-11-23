# Autopsy 2026-11-22-unpromised-member-2 (unpromised-member)

<!-- Findings went away. That is the symptom gone, not the cause understood. Read the code around the fix
and answer: what was missing from the model that let this shape appear, where else it shows, and what
rule would stop it next time. cerco stays red until every section is filled; then it files this form in
.cerco/autopsies/ by itself. Places are written path:line, relative to the project root, and must exist. -->

## Symptom

Findings of `unpromised-member` that went away (written by cerco):

- `machine/core/src/main/java/com/fpetrola/oozx/Speccy.java|Speccy.Speccy(SpectrumZ80Clock, EmulationSession, MemoryBus, SpectrumMemory, Display, KeyMatrix, PeripheralRegistry, MachinePortBus, Sound, Ula, Scheduler, Machine, Cpu, Picture, Speed, dev.plugdesk.app.Configuration, com.fpetrola.oozx.config.RomFiles, java.util.Set<Peripheral>, Timer, com.fpetrola.oozx.speccy.modules.z80.Processors, com.fpetrola.oozx.speccy.modules.z80.MachineLoop)`
- `machine/core/src/main/java/com/fpetrola/oozx/speccy/peripherals/PeripheralRegistry.java|PeripheralRegistry.PeripheralRegistry(Backplane, MemoryBus, Provider<Machine>)`

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

The registry's constructor was public although nothing but the injector ever builds a registry: no
code anywhere calls new PeripheralRegistry, and every machine gets its one registry through Guice. It
was public by habit, which made it an entrance to the core module that nobody had agreed to. It is
package-private now, and Guice still builds it; Speccy's constructor, built the same way and only
from Speccy.create, went the same way.
model: knowledge-without-one-owner


## Context

<!-- At least two places you read, path:line, one of them outside the fixed file(s): callers, the
hierarchy, the data that flows in, the code that forced the shape. Say what each one showed. -->

- machine/core/src/main/java/com/fpetrola/oozx/speccy/peripherals/PeripheralRegistry.java:64 - the constructor, annotated @Inject; searching oozx, oozx-plugins and the translator found no `new PeripheralRegistry(`.
- machine/core/src/main/java/com/fpetrola/oozx/Speccy.java:103 - the only way a machine is assembled is injector.getInstance(Speccy.class), which is what builds the registry too.
- machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/machine/Machine.java:68 - the Machine receives the registry from the injector as well, never builds one.


## Same cause elsewhere

<!-- Other places (path:line) where the same missing model shows, even under a different rule or no rule
at all; they are the next fixes. Or `none` and how you looked. -->

- machine/core/src/main/java/com/fpetrola/oozx/speccy/modules/machine/Machine.java:68 - Machine's constructor is public and only Guice calls it.
- none further checked: I looked only at the constructors around the registry (Speccy, Machine, PeripheralRegistry), not the whole module.


## Principle

<!-- The design principle at stake: tell don't ask, command-query separation, single owner of a concept,
polymorphism over type tests, information hiding... -->

Information hiding: what only the container builds is not an entrance anyone else may use; the module's entrances are the ones a person agreed to.


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
The rule marked PeripheralRegistry's public constructor as an entrance the seal does not have and told to propose it or keep it out of reach. Checking who builds it showed only the injector does, so keeping it out of reach was the answer and the constructor is package-private; the same reading applied to Speccy's constructor.

