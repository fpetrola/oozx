# Tools and Debugging

Everything here is a device or a window you clip onto a machine — so a tool looks at *that* machine, and two
machines can be under two tools at once without sharing a thing.

## Debugger

`Emulator → Equipment → Debugger`. Position, memory, breakpoints, step / run / stop, over the machine the
window is clipped to. All of its state is per instance — **two machines debugged at once do not share
breakpoints or registers**, which falls out of there being no static state anywhere in the model.

The instruction list is rendered from the instruction objects themselves (`Z80InstructionRenderer`); changed
values are highlighted as they change.

![The debugger over a running game](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/debugger.png)

*Routines on the left, the instructions where it is, the registers with their flags, and the memory below —
over Manic Miner, still running at 10,215 %.*

## Sprite viewer

`Emulator → Equipment → Sprites`. The machine's memory read as sprites: a byte is eight pixels, a sprite is
so many bytes across and so many rows down, laid out from an address onwards. Pan, and a zoom that keeps
what is under the pointer under the pointer.

![The sprite viewer showing Jet Set Willy's sprites](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/sprite-viewer.png)

*Jet Set Willy's sprite table, read straight out of the machine's memory at `BE00`: Willy's four walking
frames, the items, and the guardians. The machine beside it is the same game in 256 colours.*

Useful for exactly what it sounds like: finding where a game keeps its graphics.

## The keyboard window

A photograph of a 48K showing **the matrix the machine actually reads**. When typing does not arrive, it
says whose fault it is: a key lit here and ignored is the game's business; a key that never lights never got
this far. See [Input and Keyboard](Input-and-Keyboard).

## The oscilloscope

`Emulator → Real Cassette (audio in)` shows the sound card's input as a waveform, with a line selector, zoom
and follow-the-head. Made for loading from a real cassette player; useful for looking at any signal.

## The bridge

`machine/bridge` runs **the reference emulator's own C core** through JNA, behind the same interface this
emulator's core implements. It is how a disagreement about timing gets settled: run both machines over the
same program and compare step by step. It is also the oracle for the tape and snapshot readers, against
libspectrum.

## Screens without a window

A machine can be built, run and photographed with no interface at all — that is how the Spectrum screens in
this wiki were made, and how the regression tests dump the screen when a hash moves:

```java
Speccy speccy = silentMachine();
select(speccy, speccy.machine.model(Spec48.class));
speccy.picture.active = true;
Snapshots.of(speccy).load("manicminer.z80");
runFrames(speccy, 120);
// speccy.picture.pixels is the picture, RGB, border included
```

Frames are counted by the machine, never by watching the clock go backwards — a program whose loop divides
the frame leaves the clock standing still, and that once hung a build for ten minutes.

## Other things that behave as instruments

- **An RZX recording** is a determinism test: replay it and count the fetches per frame.
- **The configuration document** is checked against the classes that declare it, so the documentation of the
  settings cannot drift from the settings.
- **The freshness test** says the generated core in the tree is what the model produces today.
- **A profile** is a JFR recording; the performance diary quotes them with their sample counts.

## Where this lives

`devices/debugger` (debugger and sprite viewer), `machine/app/.../KeyboardInternalFrame` and
`AudioInInternalFrame`, `machine/bridge`, `model.harness.MachineTest` for the headless harness.
