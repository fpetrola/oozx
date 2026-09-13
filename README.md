# OOZX

**A ZX Spectrum emulation environment, object-oriented all the way down to the Z80.**

[![Build and Deploy](https://github.com/fpetrola/oozx/actions/workflows/maven.yml/badge.svg)](https://github.com/fpetrola/oozx/actions/workflows/maven.yml)

![Several Spectrums, the game browser and the snapshot history on one desktop](doc/zxenv2.gif)

A desktop where you run as many Spectrums as you like, plug real hardware into them by hand, pull games
straight from the catalogue, and run them at hundreds of times real time. Underneath, a machine model made
entirely of objects, which is what makes all of that cheap to build and possible to extend.

## What you can do here that you cannot do elsewhere

- **Run several Spectrums at once.** Each window is its own machine, with its own model, speed, sound and
  pokes. Quit, reopen, and they come back running where they were.
- **Plug hardware in by hand.** Twenty-two peripherals, each a window. Clip it onto a machine to connect it,
  unclip to disconnect. Drive bays with motor lights, disks you insert, flip and write-protect, the Multiface's
  red button, the ZX Printer's paper, the Interface 1's RS-232 terminal.
- **Play from the catalogue, no files.** Search ZXInfo.dk, filter by machine and genre, and load a title into a
  new machine. It picks the right file out of the archive and the right model from the tape.
- **Watch a recorded playthrough, then take over.** Recordings from ZXInfo and the RZX Archive, several playing
  side by side, and a button that hands you the machine mid-replay.
- **Run at 300 times real time**, with sound and screen on, on a laptop. A speed dial from 25 % to 40,000 %,
  paced by the sound card so it never stutters.
- **Load from a real cassette player** through the sound card, with a live oscilloscope. Or use the tape deck
  per machine: audible loading, auto-load at 20,000 %.
- **See the picture as it came through the lead:** RGB, Scart, composite or aerial, with phosphor,
  persistence and scan lines, up to xBRZ scaling, saved as named profiles.
- **3,683 pokes** built in and matched to the game you loaded.

## And everything you expect

| | |
|---|---|
| Machines | 48K PAL and NTSC, 128K, +2, +2A, +3, +3e, Pentagon |
| Tapes and disks | TAP, TZX, CSW; DSK, TRD, SCL, MGT, IMG, OPD, D40/D80; HDF and SD images; Microdrive and ROM cartridges |
| Snapshots and recordings | Z80, SNA, SZX, SP read and written; RZX read, written and replayed |
| Sound | beeper, AY-3-8912 with band-limited synthesis, Melodik, Fuller Box, Covox, SpecDrum |
| Video | contention per T-state, attribute and border changes drawn where the beam was |
| Input | Cursor, Kempston, Sinclair, Timex and Fuller joysticks, physical gamepads, Kempston Mouse |
| Storage and tools | Beta 128, +D, DISCiPLE, Opus, Didaktik, DivIDE, DivMMC, ZXATASP, ZXCF, ZXMMC, Interface 1 and 2, Multiface One/128/3, ZX Printer, parallel port |

## Why it can do that

- **The Z80 is made of objects.** Every instruction, operand and register is an object you can execute,
  inspect, clone or replace on its own. That is why the code of a game can be analysed: symbolic execution runs
  a game with nobody playing, finds its routines, detects self-modifying code and emits Java. Jet Set Willy and
  Manic Miner run as Java programs today. The same hooks give tracing and profiling for free.
- **Nothing foreign inside it.** Memory timing belongs to the Spectrum and MEMPTR is optional, so both are
  aspects attached from outside. Either can be left off; the processor never knows.
- **Every part behind a contract, no global state.** Processor, memory, I/O, sound and display are each
  swappable, and each has been swapped: RZX playback is the I/O replaced by the recording; the test harness is
  an instrumented memory; a machine is an object graph, so many of them share one process.
- **Hardware from outside.** A device answers ports on a bus that merges answers like the real one, watches the
  program counter to page its ROM in, and is found on the classpath. The core never names a device. The smallest
  peripheral is a hundred lines and comes with a complete window; most of the twenty-two landed within two days.
- **Machines as configurations.** A model is a choice of pager, ULA and timing. The Pentagon was added mostly
  to see what adding a machine costs.
- **Incremental by construction.** One concept, one owner, one module, and the compiler enforces who may know
  whom. Features arrive as new modules; the code base shrinks as it improves.
- **Correctness travels with the model.** Fuse's 1,355 test vectors, an ALU reference of 131,072 combinations
  per instruction that caught bugs Fuse misses, emuStudio's suite. The same batteries run unchanged against any
  implementation of the processor, the machine was compared step by step with Fuse's real core, and recorded
  games replay deterministically.

## Speed by design

The object model is a fixed graph that a program can read, so a second core is **generated from it**: a partial
evaluator flattens the instructions, their operands and the timing aspects into one class with no objects on
the hot path. It is built on the first run, cached, regenerated when the model changes, and it passes the same
tests as the model it came from. With the processor out of the way, each remaining layer was tuned in its own
class, measured before and after, none of them touching the model.

| | frames per second | times real time |
|---|---|---|
| Manic Miner, 48K, sound and screen on | 15,256 | about 305 |
| Jet Set Willy, 128K, sound and screen on | 12,867 | about 257 |
| Generated core against the object-oriented core | 4.3× | |

Speed beyond emulators written in low-level languages with hand-optimised inner loops, reached by design
rather than against it, and switchable at runtime: the object-oriented core stays the reference. The
measurement diary is in `doc/`.

## What it opens next

- A new machine or peripheral costs what a module costs; Timex, Scorpion and SE timings are already declared.
- A debugger or a disassembler is one more visitor over the same instructions.
- Rewind is cloning the state. A different generation target is a different back end for the same generator.

## Build and run

JDK 21 or newer and Maven. A JDK, not a JRE: the fast core is compiled on the first run.

```bash
git clone https://github.com/fpetrola/oozx.git
cd oozx
mvn -DskipTests install
java -jar machine/app/target/app-0.0.2-alu-SNAPSHOT.jar
```

Where things live: `emulator` is the Z80; `machine/core` the Spectrum, with no Swing in it; `machine/devices`
one module per peripheral; `machine/generated` the fast core; `machine/app` the desktop; `machine/bridge` the
comparison with Fuse; `zx-rzx` the recordings; `translation/*` the translation of a game to Java.

## Credits and license

The [Fuse](https://fuse-emulator.sourceforge.net/) authors, for the test vectors, the timing model and the core
the bridge runs; Cristian Dinu, for the [decoding of the Z80 opcode byte](http://www.z80.info/decoding.htm);
Peter Jakubčo's [emuStudio](https://www.emustudio.net/), for its Z80 test suite; [ZXInfo.dk](https://zxinfo.dk/)
and the [RZX Archive](https://www.rzxarchive.co.uk/), for the catalogues.

Licensed under the Apache License, Version 2.0. See `LICENSE`.
