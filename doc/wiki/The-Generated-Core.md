# The Generated Core

The object model is the reference implementation. It is also, literally, the *source* of the fast one: a
partial evaluator reads the model — its Java source **and** the live object graph of a machine built for the
occasion — and writes out one class in which the instructions, their operands and the lateral aspects have
been flattened away. No objects on the hot path, no virtual calls, no tables of prototypes: just code.

It is 4.3× the model it came from, and it passes the same test batteries.

## What is woven in

`GeneratedCores` builds a silent `Speccy` on the model core and hands the generator four things:

1. **The instruction instances** of the decoder tables — not classes, *instances*, with their operands
   already bound.
2. **The MEMPTR aspect** (`MemptrUpdater`, a visitor, resolved by double dispatch against each instance).
3. **The contention aspect** (`PhaseProcessor`, recording what it would do at each syntactic point).
4. **The machine's memory**, as `read`/`write` helpers.

The specialiser then applies eight rules: configuration fields become literals, collaborator calls are
inlined recursively, `instanceof` probes are folded, lambda fields are resolved through the constructor
assignment, ALU operations go through the delegate's `calculate*`. The tables become nested `switch`es,
split into methods of sixteen opcodes so that no method crosses HotSpot's 8,000-byte inlining limit.

The result, today:

| | |
|---|---|
| 27,376 lines, 957,600 bytes | 1,792 `case 0x` |
| 231 `decode*` methods | 1,037 `read(` and 605 `write(` sites |
| 931 mentions of MEMPTR | 6 contention helpers (1×1, 1×3, 2×1, 4×1, 5×1, 7×1) |

Its first line says which model it came from: `// Written by the build from the model, not by hand. Model: c8c6983d...`

## The boundary: what is frozen and what is read

The rule, stated once and applied everywhere:

> Freeze what cannot change while this core instance lives; read, on every access, everything the machine
> mutates.

Frozen as `final` constructor fields: `ram`, `mapRead`, `mapWrite`, `ula`, `clock`, `display`, `io`,
`noMreqRun2..7`. Read every time: the page table, a page's fields, the contention tables, the T-state
counter — because **the whole banking model of this machine is "mutate the page table"**, so the page table
can never be baked in.

## When it is written

It is committed, as a source file of `machine/generated`, and the build owns it:

- At `process-classes`, `GeneratedCores.main` runs in a forked JVM and compares the model's hash — a SHA-256
  over the model sources and the generator — with the one on the file's first line.
- Same hash: nothing happens.
- Different: it writes the file (about 11 s, 956 KB of source) and compiles that one file into
  `target/classes` (about 2 s, 326 KB of classes) — itself, because the module's compiler has already run by
  then and would otherwise leave a stale class next to the new source.
- At runtime a machine does `Class.forName`. **Nothing is generated or compiled while the emulator runs.**

## How a machine gets it

A service file (`META-INF/services/com.fpetrola.oozx.Extension`) → `GeneratedCores` binds `Core` through
Guice's `OptionalBinder.setBinding()`. The emulator's default is `OopCore`, and the emulator does not depend
on the module at all: if the jar is not there, you get the model.

The object core is used instead when:

- the wiring counts its own T-states (the test harness's `CountingWiring` — a core that reads the memory
  tables itself never reaches those listeners),
- the build carries no generated core,
- or *Fast Core* is off in the settings.

A running machine can be moved from one to the other **mid-game** (`Z80.useProcessor`), which is also how the
two are compared on the same machine, in the same session.

## How it is kept honest

| Gate | What it holds |
|---|---|
| `EmulatorOnTheGeneratedCoreTests` | the CPU vector battery, the ALU reference and emuStudio's suite — 1,592 tests — run against the generated core through a `ServiceLoader` seam, without knowing which core they are on |
| `MachineOnTheGeneratedCoreTests` | the whole machine suite, on the generated core |
| `GeneratedMachineCoreTest` | boots a ROM for 300 frames on both cores and compares `AF..R`, MEMPTR, the T-state count and **all of RAM**; then 150 + 150 frames switching processor in the middle |
| `GeneratedSpectrumZ80IsCurrentTest` | text equality against the reference copy: the artefact in the tree is what the model produces *today* |

## Why the model allows it

This is the part that is not an accident:

- *"The model does not adapt to the tool."* The generator exists because the model has the shape a
  specialiser wants, not the other way round.
- The instance graph is **fixed** once a machine is built — the situation a partial evaluator is for.
- The aspects are **visitors**, which is the shape a specialiser resolves best.
- There are no per-instruction templates anywhere: one mechanism, applied to a graph.
- *"What the generator inlines arrives with its identities resolved; what the JIT inlines does not."*

## Known holes

- `GeneratedSpectrumZ80.read/write` index the page table with no guard for a `DevicePage`, so the Opus
  Discovery's paged device memory is not reached on the generated core — and no gate sees it, because
  `OpusTest` and the generated-core runs never cross.
- The generated core reports no memory-read / memory-write / memory-contend events, so anything that watches
  the bus watches the model instead.
- One planned step, "C" (fetch and executor), was never done.

## Where this lives

`machine/generated` — `com/fpetrola/z80/generate/{Specializer,CoreGenerator,Simplifier,Folder,SourceIndex}`
and `com/fpetrola/oozx/generated/{GeneratedCores,GeneratedSpectrumZ80}`. The diaries are
`doc/plan-nucleo-generado.md`, `doc/plan-nucleo-generado-jvm.md` and `doc/plan-nucleo-generado-modulo.md`.
