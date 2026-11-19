# How fast, and how we know

An hour of Manic Miner in twelve seconds, with sound and picture on. This page is how that was measured,
and what has not been measured yet.

![The speed dial going from 100 % to 22,000 %](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/turbo.gif)

> **Clip** · *An hour in twelve seconds.* Manic Miner is dropped on the desk and the rocket is pressed: the
> caverns go by at more than twenty thousand per cent, and it comes back to 100 %. (`an-hour`, filmed)

## The numbers

A ZX Spectrum draws 50 frames a second.

| | frames a second | Spectrums' worth | an hour of it takes |
|---|---|---|---|
| Manic Miner, 48K, sound and picture on | **15,256** | about 305 | 12 seconds |
| Jet Set Willy, 128K, sound and picture on | **12,867** | about 257 | 14 seconds |
| The generated core against the object model it comes from | **4.3 times** | | |

That is not a stripped benchmark mode. It is the emulator's own loop, the one you use, with the picture
being drawn and the sound being made.

The conditions, as the diary has them: 3 September 2026, a notebook with an i5-1335U, JDK 21, pinned to two
cores, measured in blocks of one second, taking the plateau of the last four. **These figures are from the
build of that day. I have not taken them again on a released jar.**

## What the speed is for

Nobody needs to play three hundred times faster. What the speed buys is everything else:

- **Tapes with no tricks.** With *Fast Loading* on, a tape is over in a moment, and so is a game's own
  loader, because the machine really runs it. → [Tape, real and virtual](Tape-real-and-virtual)
- **Nine processors at once.** Spec256 runs nine Z80s in step and is still faster than a Spectrum.
  → [256 colours](256-colours)
- **Work in the background.** The map plays your session again on a second machine while you go on playing.
  → [The map draws itself](The-map-draws-itself)
- **Whole games as tests.** A complete recording of Jet Set Willy replays at 21,186 % of its speed, so
  replaying somebody's whole game is something a test can afford. → [How right, and how we know](How-right-and-how-we-know)

## Using it

The dial on each machine goes from 25 % to 40,000 %, with the useful range in the middle of the slider. The
bar at the foot of the machine says what it is actually reaching. At a Spectrum's own speed the sound card
sets the pace; above it the sound is dropped rather than stretched. And the processor can be changed while
the machine runs: the object model, the generated core, or Spec256's nine.

## How it got there

The processor was only the first half. Once the generated core made the processor cheap, each remaining
layer was measured, tuned in its own class, and measured again. None of it touched the model.

| Change | Measured |
|---|---|
| Memory timing applied in runs instead of one lookup at a time | Manic Miner +8 to +16 % |
| The clock without its timeout check | Manic Miner +3 to +6 %, Jet Set Willy +10 % |
| One question per write instead of five | +3 %, +7 % |
| Memory pages as plain byte arrays | **Jet Set Willy +48 %**, and it exposed a bug in a signed comparison |
| Ports and keyboard without allocating | Jet Set Willy +7 %; garbage down from 951 to 671 bytes a frame |
| The screen redone, cell by cell | its own cost fell from 17.4 % to 0.9 %, **and the frames a second did not move**, so it was thrown away |
| The sound chip synthesised from event to event | **Jet Set Willy +9 %** |
| Sound as the pacer | the rule that stayed |

From start to finish that was 13,333 and 7,939 frames a second, for Manic Miner and Jet Set Willy, becoming
15,256 and 12,867: 14 % and 62 % more, after the 4.3 times the generated core had already given.

Where the time goes now, by a profile of a Jet Set Willy replay: the **whole generated core is 8.8 %** of a
frame. Making the processor infinitely fast from here would buy about a tenth. The rest is memory, timing,
the screen and the sound.

## How these numbers were taken

This part matters more than the numbers.

- **Pinned, and taking the plateau.** On the harness of one afternoon the same code ran at 830 frames a
  second with its thread wandering between cores, and at 1,360 pinned to one: the instrument was broken, not
  the code. Pinned, and taking the mean of the last blocks, the spread inside a run went from 12 % either way
  to about 1 %.
- **Runs of A and B interleaved**, three to five rounds in both orders, best and mean reported.
- **The same work each time.** The variants of one experiment all executed exactly 29,097,384 instructions.
- **The noise is declared**, harness by harness: about 1 % pinned on the emulator's own loop, 3 % on the
  recording harness, 8 % on the small benchmark.
- **Three harnesses gave three answers to the same change**, and the diary says so rather than picking the
  flattering one.
- Changes that measured neutral were **reverted**, including ones that were more elegant.

Every number is in `doc/plan-superar-zxspin.md`, `doc/plan-nucleo-generado.md`,
`doc/plan-nucleo-generado-jvm.md` and `doc/plan-nucleo-generado-modulo.md`, with its conditions beside it.

## What has not been measured

**I have not measured any other emulator.** The one comparison in the diaries, that the generated core ran
at the speed of the reference emulator and another emulator a fifth faster, is the premise the work started
from and not a result. I have not found another emulator's published figure to put beside these, either.

The way to settle it is already in the repository: `machine/bridge` runs Fuse's own core, the real one,
behind the same interface as this one. Putting both behind the same harness and the same recording is the
measurement, and I have not run it.

Until then, what I stand behind is what I can show: 15,256 frames a second with sound and picture on, and
4.3 times the object model, from a model that was not bent to get there and is still the reference.

The translator is a separate question with the same answer: a translated game is not emulated, it runs, and
how fast it runs has not been measured either. → [The translator](The-translator)

## Why Java is not the story

There are other Spectrum emulators in Java, so "in Java" is not news by itself. The mechanism is. This one
is not fast in spite of its objects: the object graph is what the generator flattens.
→ [The fast core](The-fast-core)

The diaries hold what somebody who knows the JVM would want to read: methods split sixteen opcodes at a time
so that none crosses HotSpot's limit for inlining; memory pages as byte arrays, worth 48 %; garbage brought
down from 951 to 671 bytes a frame.

## Doing it yourself

```bash
taskset -c 2,3 java -jar oozx-<version>.jar
```

Load a game, turn the dial to its maximum, and read the percentage at the foot of the machine. The harnesses
that run a fixed amount of work and report a rate are `InstructionsPerSecond` under `machine/app/src/test`
and `LoopCoreMeasurement` under `machine/machines/src/test`.

Next: [How right, and how we know](How-right-and-how-we-know).
