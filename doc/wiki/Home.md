# OOZX

**A ZX Spectrum emulation environment, object-oriented all the way down to the Z80.**

![Several Spectrums, the game browser and the snapshot history on one desktop](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/zxenv2.gif)

A desktop where you run as many Spectrums as you like, plug real hardware into them by hand, pull games
straight from the catalogue, and run them at hundreds of times real time. Underneath, a machine model made
entirely of objects — which is what makes all of that cheap to build, possible to extend, and, unexpectedly,
fast: a second processor core is *generated* from the model and runs 4.3× the model it came from.

---

## Start here

| | |
|---|---|
| [Getting Started](Getting-Started) | Build it, run it, load your first game |
| [The Desktop](The-Desktop) | Many machines on one desk, windows you clip together, a session that comes back |
| [Machines](Machines) | Twenty-four models, from a 16K to a Pentagon 1024 |
| [Peripherals](Peripherals) | Twenty-four devices, each a window you clip onto a machine to plug it in |

## What the machine does

| | |
|---|---|
| [Video](Video) | The picture as it came down the lead: RGB, Scart, composite, aerial, phosphor, scalers |
| [Sound](Sound) | Beeper, AY, Melodik, Fuller, Covox, SpecDrum — and sound as the thing that sets the pace |
| [Input and Keyboard](Input-and-Keyboard) | The matrix, the on-screen 48K, joysticks, a gamepad, the Kempston mouse |
| [Tapes, Disks and Snapshots](Tapes-Disks-and-Snapshots) | Every format it reads and writes, the cassette deck, a real cassette through the sound card |
| [Spec256 — 256 colours](Spec256-256-Colours) | Nine Z80s in lockstep, so a 1984 game plays in 256 colours |
| [Games: catalogue and library](Games-Catalogue-and-Library) | ZXInfo inside the emulator, 4,922 games fingerprinted, 3,683 pokes |
| [Recordings (RZX)](Recordings-RZX) | Watch somebody else's playthrough, then take the machine over mid-replay |

## How it is built

| | |
|---|---|
| [Architecture](Architecture) | Objects down to the instruction; one concept, one owner, one module |
| [The Generated Core](The-Generated-Core) | A partial evaluator flattens the model into a second core |
| [Performance](Performance) | 15,256 fps on a laptop, with sound and screen on — every number and how it was taken |
| [Correctness and Tests](Correctness-and-Tests) | 1,355 vectors, 131,072 ALU combinations, and Fuse's real core behind the same interface |
| [Games as Java programs](Games-as-Java-Programs) | Symbolic execution turns a game into Java that runs with no emulator under it |
| [Extending OOZX](Extending-OOZX) | A peripheral is a few dozen lines and two service files |
| [Tools and Debugging](Tools-and-Debugging) | Debugger, sprite viewer, keyboard monitor, oscilloscope |
| [Limits and what is not there](Limits-and-what-is-not-there) | The honest list |

---

## What you can do here that you cannot do elsewhere

- **Run several Spectrums at once.** Each window is its own machine, with its own model, speed, sound and
  pokes. Quit, reopen, and they come back running where they were.

  ![Three machines at three speeds](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/desk-tiled.png)
- **Plug hardware in by hand.** Two dozen peripherals, each a window. Clip it onto a machine to connect it,
  unclip to disconnect. Drive bays with motor lights, disks you insert, flip and write-protect, the Multiface's
  red button, the ZX Printer's paper, the Interface 1's RS-232 terminal.
- **Play a 1984 game in 256 colours.** Spec256 graphics packs, run by nine processors in lockstep, over
  photographic backgrounds — the same game, the same machine, twice:

  ![Jet Set Willy in Sinclair colours and in 256](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/jsw-side-by-side.gif)
- **Play from the catalogue, no files.** Search ZXInfo.dk, filter by machine and genre, load a title into a
  new machine. It picks the right file out of the archive and the right model from the tape.
- **Let it recognise what you already have.** Point it at your collection and every file says which game it
  is — by content, not by filename — so pokes, maps and details follow a file called `RENE256.SNA`.
- **Watch a recorded playthrough, then take over.** Recordings from ZXInfo and the RZX Archive, several
  playing side by side, and a button that hands you the machine mid-replay.
- **Run at 300 times real time**, with sound and screen on, on a laptop. A speed dial from 25 % to 40,000 %,
  paced by the sound card so it never stutters.
- **Load from a real cassette player** through the sound card, with a live oscilloscope. Or use the tape deck
  per machine: audible loading, auto-load at 20,000 %.
- **See the picture as it came through the lead:** RGB, Scart, composite or aerial, with phosphor,
  persistence and scan lines, up to xBRZ scaling, saved as named profiles.
- **Change the processor while the machine runs.** Object model, generated core, or the nine of Spec256 —
  the same machine, mid-game.

## And everything you expect

| | |
|---|---|
| Machines | 16K, 48K PAL and NTSC, 128K, +2, +2A, +3, +3e, Pentagon 128/512/1024, Timex TC2048/TC2068/TS2068, Spectrum SE, Scorpion ZS 256, and seven clones |
| Tapes and disks | TAP, TZX, CSW; DSK, TRD, SCL, MGT, IMG, OPD, D40/D80; HDF and SD images; Microdrive and ROM cartridges |
| Snapshots and recordings | Z80, SNA, SZX, SP read and written; RZX read, written and replayed |
| Sound | beeper, AY-3-8912 with band-limited synthesis, Melodik, Fuller Box, Covox, SpecDrum |
| Video | contention per T-state, attribute and border changes drawn where the beam was, Timex hi-res and hi-colour, ULAplus |
| Input | Cursor, Kempston, Sinclair, Timex and Fuller joysticks, physical gamepads, Kempston Mouse, Recreated ZX |
| Storage and tools | Beta 128, +D, DISCiPLE, Opus, Didaktik, DivIDE, DivMMC, ZXATASP, ZXCF, ZXMMC, Interface 1 and 2, Multiface One/128/3, ZX Printer, parallel port |

## Why it can do that

Three sentences, each with a page behind it:

1. **The Z80 is made of objects** — every instruction, operand and register is an object you can execute,
   inspect, clone or replace on its own. That is why a game's code can be *analysed* and not only run, and why
   tracing, profiling and a debugger come for free. → [Architecture](Architecture)
2. **A fixed object graph is exactly what a partial evaluator wants.** The model is read by a generator that
   flattens it into one class with no objects on the hot path, cached and re-generated when the model changes,
   passing the same test batteries. → [The Generated Core](The-Generated-Core)
3. **One concept, one owner, one module.** A peripheral is a jar with a service file; the core never names a
   device; the compiler enforces who may know whom. Twenty-four devices landed that way, most of them in two
   days. → [Extending OOZX](Extending-OOZX)

---

Licensed under the GNU General Public License, version 3 or later.
Source: [github.com/fpetrola/oozx](https://github.com/fpetrola/oozx)
