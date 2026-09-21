# Architecture

The whole project is one bet: that a Spectrum modelled as plain objects, with nothing hidden in a global and
nothing special-cased, buys more than it costs — and that when it costs speed, the speed can be bought back
*from the model itself* rather than by giving it up.

## The Z80 is made of objects

- **76 instruction classes** under `emulator/.../instructions/impl`, grouped by shape rather than by opcode:
  `TargetInstruction`, `SourceInstruction`, `TargetSourceInstruction`, `ConditionalInstruction<C>`,
  `BlockInstruction`, `RepeatingInstruction`, `BitOperation`, and the parameterised ALU ones. Each has
  `execute()`, `getLength()` and `accept(InstructionVisitor)`.
- **Operands are objects too.** A `Register` is an `OpcodeReference`; so are `Memory8/16BitReference`,
  `IndirectMemory8/16BitReference`, `MemoryPlusRegister8BitReference` (that is `(IX+d)`),
  `ConstantOpcodeReference`. Conditions are objects: `Condition`, `ConditionFlag`, `ConditionAlwaysTrue`,
  `BNotZeroCondition`.
- **Decoding** follows Cristian Dinu's decomposition of the opcode byte, as tables of prototype instructions,
  prefixes included. `InstructionCache` clones a prototype per PC, and a memory-write listener invalidates it
  when a program writes over its own code.
- **One visitor**, `InstructionVisitor<R>`, with about a hundred methods. Everything that walks the program is
  one: the cloners, the MEMPTR updater, the timing phase processor. A disassembler or a debugger is one more.

Because an instruction is an object you can hold, clone, inspect and run on your own terms, a game's code can
be *analysed* and not only executed — which is what [Games as Java programs](Games-as-Java-Programs) is.

## Nothing foreign inside the processor

Two things that every emulator puts in the middle of its Z80 are not in this one:

- **Timing and contention.** `PhaseProcessor` is an `InstructionVisitor<Integer>` and an `ExecutionListener`,
  attached from outside through three seams (the executor, the memory listeners, an `AddStatesIO` decorator).
  Its own Javadoc: *"the only knowledge of the Spectrum's timing in the emulator; the Z80 model does not know
  this class exists."*
- **MEMPTR.** An ordinary `Register`, kept up to date by a spy attached the same way. Leave it off and the
  processor never notices.

Both are lateral aspects. That is not a stylistic point: it is precisely what makes the model a *fixed object
graph with the aspects woven in*, which is the shape a partial evaluator can flatten — see
[The Generated Core](The-Generated-Core).

## Every part behind a contract, no global state

Processor, memory, I/O, sound and display are each an interface, and each has actually been replaced:

| Contract | Replaced by |
|---|---|
| `Core` | `OopCore`, the generated core, and `Spec256Core` (nine processors) — switchable **while the machine runs** |
| `IO` | the RZX player's, which answers every `IN` from a recording |
| `Memory` | the test harness's instrumented one; a follower's, in Spec256 |
| `SoundCard` | `SilentSoundDevice`, which is what every test runs on |

A machine is an object graph built by a composition root (`Speccy.create()`, Guice), so several machines are
several graphs in one process — which is why the desktop can hold five Spectrums with no static anywhere to
fight over.

## Hardware from outside

A device answers ports on a bus that merges answers the way the real one does: a wired AND with a *driven*
flag, the floating bus for undriven bits, plugged-in devices answering before the machine's own chips, and a
per-port cache of who answers. A device may also watch the program counter to page its ROM in.

**The core never names a device.** Devices are found on the classpath through two `ServiceLoader`s
(`Extension` for the machine, `Equipment` for the desktop). See [Peripherals](Peripherals) and
[Extending OOZX](Extending-OOZX).

## One concept, one owner, one module

The rule the code is written to, and the compiler enforces it because the modules can only depend one way:

```
emulator  →  machine/core  →  machine/machines, machine/media, machine/host
                           →  machine/ui  →  machine/devices  →  machine/app
```

| Module | Owns | Size |
|---|---|---|
| `emulator` | the Z80: instructions, registers, decoding, the visitor | 229 classes |
| `machine/core` | the Spectrum: memory bus, ULA, display, ports, sound mixing, keyboard matrix, scheduler, configuration. No Swing in it | 88 |
| `machine/machines` | the 24 models, plus `ay`, `disk`, `memory`, `scld`, `ula`, `ulaplus` | 65 |
| `machine/media` | tapes, snapshots, recordings as media | 16 |
| `machine/host` | the other side of the glass: PC keyboard layouts, joysticks, the audio line | 18 |
| `machine/ui` | windows and the screen. **Depends on nothing of the emulator** | 20 |
| `machine/devices` | one module per peripheral, twenty-four of them | 133 |
| `machine/app` | the desktop | 34 |
| `machine/generated` | the generated core | 11 |
| `machine/bridge` | the reference emulator's real core behind the same interface | 31 |
| `machine/zxinfo`, `machine/pokes` | the catalogue, the fingerprints, the cheats | 28 |
| `zx-rzx` | recordings | 18 |
| `translation/*` | a game turned into Java | 199 |

Concretely, "one owner" means: everything that depends on a tape format lives in `Tape` and nowhere else —
no `switch` on `tap`/`tzx`/`csw` anywhere outside it. The same for every other concept. New code goes where
its concept lives, not next to whoever calls it.

Features arrive as new modules, and the code base is expected to *shrink* as it improves. Spec256 is the
measured example: 1,689 lines of new module, and 258 lines added to the rest of the tree across 23 files,
with no special case in any of them.

## Configuration

One JSON at `~/.oozx/config.json`, a `@Section` per class — the class *is* the schema. Unknown sections are
preserved rather than dropped. A test (`SectionsAreDeclaredTest`) holds the file and the classes together,
and the settings window is built from what each part declares about itself rather than from a hand-written
list of tabs.

## Where to start reading

`emulator/src/main/java/com/fpetrola/z80/` — `instructions/impl`, `opcodes/decoder/table`, `registers`,
`tstates/PhaseProcessor`. Then `machine/core/src/main/java/com/fpetrola/oozx/Speccy.java`, which is where a
machine is assembled.
