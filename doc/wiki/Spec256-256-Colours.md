# Spec256 — a 1984 game in 256 colours

Spec256 was a DOS emulator from 1999 with one very good idea: leave the game alone, and give its *graphics*
eight bits per pixel. The coloured graphics travel through the game's own code, unmodified, so the game
plays exactly as it always did — and comes out in 256 colours, with no colour clash, over photographic
backgrounds.

OOZX runs that format. Not as a special case bolted onto the emulator: as **nine Z80s in lockstep**, in a
module the rest of the tree does not know exists.

![Jet Set Willy running side by side: Sinclair colours on the left, Spec256 on the right](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/jsw-side-by-side.gif)

*Jet Set Willy, the same room and the same ten seconds, twice: the machine as Sinclair sold it, and the same
machine with a `jsw.gfx` file lying beside the snapshot. Both were filmed from the emulator with no window
open, frame by frame.*

| Jet Set Willy, as Sinclair shipped it | The same room, the same moment, in 256 colours |
|---|---|
| ![JSW in Sinclair colours](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/jsw-sinclair-playing.png) | ![JSW in Spec256 colours](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/jsw-spec256-playing.png) |

Both pictures come from the same emulator, the same snapshot and the same 120 frames; the only difference is
that `jsw.gfx`.

And the title screen, where a background file replaces what the game left black:

| | |
|---|---|
| ![The JSW title screen as Sinclair shipped it](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/jsw-sinclair.png) | ![The JSW title screen in 256 colours over a photographic background](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/jsw-spec256.png) |

The stripes down the sides are the game's own border effect, drawn where the beam was when it happened —
the emulator is not doing anything special for the occasion.

## How to use it

Put the game's colour file next to the game:

```
mygame.sna        the game, a 48K snapshot
mygame.gfx        393,216 bytes: eight colour bytes for every address of RAM
mygame.cfg        optional: EmuZWin's mixing rules for this game
mygame.B00 ...    optional: 320×200 backgrounds, one byte per pixel
rom0.gfx          optional: the same for the ROM
```

Open the snapshot as usual. A snapshot says where it came from, the Spec256 device looks beside it, and if
the colours are there **the machine moves itself onto the nine processors** and opens a window showing the
game in 256 of them. Load anything else and it goes back to where it was. In the game browser, the titles
that have colours beside them have a filter of their own.

## How it works

The `.GFX` is eight bytes of colour for each of the 49,152 addresses of RAM — one byte per pixel, the
rightmost pixel first. Those eight bytes are not stored as bytes: they are **sliced into eight planes**.
Plane *v* holds, at each address, a byte whose bit *w* is bit *v* of the colour of pixel *w*. A plane is
therefore a 64 K memory shaped exactly like a Spectrum's, and that is the whole idea.

So: run the game's code on the machine, and run **the same code, at the same time, on each of the eight
planes**. Whatever the game does to its graphics — shift them, mask them, blit them, animate them — happens
to the colours too, because the colours are in the same shape as the bytes.

- `Spec256Core` is one more implementation of `Core`: the ordinary processor, nine times over.
- `LockstepZ80` is a follower: it goes where the machine goes. It has no ports (what a port answers is never
  a colour), and a `.CFG` says which of its registers are aligned with the machine's.
- `Planes` holds the eight memories, `Permutations` the tables that move bits between them,
  `Rules` what a game's own file says about how its colours meet the machine's.
- The picture is painted from the planes, and a column of the screen is painted **by whoever says its pixels
  are theirs** — by the machine's three rules when nobody does.

Eleven classes, 1,689 lines, nine test classes with 1,548 more.

## What it cost the rest of the tree: 258 lines

That is the part worth reading if you care about the design. Twenty-three files outside the module changed,
none of them by more than 33 lines, and **not one of them has a special case for Spec256 in it**. Six seams,
each of which is a thing the model was missing anyway:

| Seam | For what |
|---|---|
| `Core.wrapping(Memory)` | a core may wrap the memory its processor runs over — how a follower sees what the machine writes |
| `FilesOfItsOwn` + `Snapshots` | "where did this snapshot come from", said once to whoever wants to hear it, so the device can look beside it without anyone else knowing what a `.gfx` is |
| the instruction factory | a processor can be built with instructions that are not the ordinary ones, and can say where it reads the instruction's own bytes — which is what lets eight planes execute in step without a second Z80 |
| `Picture.paintPair` | two pixels in colours that are in no palette, for whoever worked them out himself |
| `ContendedMemory.peek`/`poke` | reading and writing without counting T-states, for whoever looks at memory from outside the clock |
| `OOZ80(OOZ80)` | a copy constructor, to wrap a processor without rebuilding it |

One line was *removed*: a static `WRITTEN_THIS_FRAME` array in `Ram`, when one rule ate the other three.

## How right is it

Measured on 14 September 2026 against the 29 games of the public Spec256 collection that ship a `.SNA` and a
`.GFX`, 300 frames each with everything at its default, compared **pixel by pixel** against the title-screen
capture each game carries, over all 49,152 pixels:

| Game | Identical | |
|---|---|---|
| Atic Atac, Phantis | 100.00 % | |
| Dizzy 1 | 99.31 % | |
| Scooby Doo | 99.08 % | |
| Jetpac | 99.02 % | was 94.50 % until its `ROM0.GFX` was loaded |
| Cybernoid | 98.91 % | the rest is within 16 per channel: this machine's dim white against the capture's |
| Solomon's Key | 98.63 % | 100 % within 16 |
| Knight Lore | 97.92 % | with background 0, the right one |
| Army Moves, Sabre Wulf | 95.9 %, 95.6 % | |
| Abu Simbel Profanation | 90.02 % | |
| Cybernoid 2 | 83.97 % | the frame's rope is animated: what differs is pixels on against off, not tones |
| Underwurlde | 36.17 % | the capture shows a green wall that is not in this copy of the game at all |

None of the twenty-nine hangs or comes up black.

## What it costs to run

| | ms per frame | times real time |
|---|---|---|
| Generated core, picture off | 0.10 | ~200 |
| Object core, picture off | 0.35–0.38 | ~55 |
| Nine in lockstep, picture off | 6.08 | 3.3 |
| **Nine in lockstep, a real game painting itself** | **17–19** | **~1.1** |

Nine processors cost about nine processors, which is the honest price of the idea. It is still real time,
and it is only paid while a Spec256 game is loaded: the core never names Spec256, so the core never pays
for it.

## Where the knowledge came from

The original Spec256 is closed and DOS; EmuZWin, which inherited the format, is closed and Windows; ZEsarUX
does not implement it. Two open implementations exist: GZX's (which says of itself "not 100 % done") and
ZX-Poly's, which is the complete one. Both were **read as a specification** and checked against a real
game's files; no line was copied. The palette is GZX's 768 numbers — declared in `NOTICE`, with entry 255
white rather than red, which is what the rules treat as ink.

Where this project's reading differed from ZX-Poly's, it was measured rather than argued: the scoreboard of
*Renegade*, for one, turned out to be right here and wrong there, and the pack's own capture says so.

## Where this lives

`machine/devices/spec256`. The full working notes — every rule, every measurement, every hypothesis that
died — are in `doc/plan-spec256.md`.
