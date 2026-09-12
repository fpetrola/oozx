# tdd

A part of the emulator, built again from nothing: one fact about the machine at a time, the
smallest code that answers it, and a class appearing only where a test could not be made to pass
without it. The point is not the code that comes out - the emulator already has code - but the
difference: which pieces of a design a test can earn, and which are there because something else
asks for them.

## What came of the first one

The memory model was derived here in twenty-two red-green cycles. Four of its classes came out
identical to the emulator's own without looking at them, and it ended smaller in two places, both
of which are now in `machine/core`:

- the contention is a property of the bus rather than an argument every access carries, so the
  memory package no longer names the ULA;
- a `Range` is a value, and the bus makes the one for its own slots.

Its 24 tests moved to `machine/core` as `model.tests.memory.SpectrumMemoryTest`, where they are
the only tests those eight classes have, and the model itself was deleted rather than left here
as a second implementation of one concept.

What the derivation could **not** reach is the part worth keeping in mind for the next one:

- `sealed` - no fact about a Spectrum asks for the set of memories to be closed. The generator
  does, to write one branch per kind.
- the flat `Covering` and its cached `contended` flag - they change no answer, so no test of
  behaviour can ask for them. The generated core reads them straight out of the table.
- rebuilding only the pages that moved - `changed` speaks only to a value, so the only thing that
  could count its work is the class itself, and opening it up to be watched would be deforming
  the model to measure it. That one belongs to a benchmark.

So the decisions in a model come in three kinds: what a fact forces, what a stub can force by
making a call that must not happen observable - at the price of a test that guards an
implementation - and what only a measurement or a requirement from outside can force.

## The ports

Twenty-one facts, in `ports`, done against the emulator's `speccy.ports`. It matched the
emulator on the mask/value decoding, the AND of the answers, the floating bus as a collaborator
and even on `Answer(value, driven)` against `BusAnswer(value, driven)`, and differed in two
places that are both in `machine/core` now: the wiring left `PortHandler` and is given when a
device is plugged in, so a chip does not know what it was soldered to, and two devices
answering one IN are both on the lines, where the arbiter used to stop at the first. Seventeen
of its facts are core's `SpectrumPortsTest` against the real bus; the four that needed more are
`machines`' `PortsOnTheMachineTest` - when in the processor's cycle the bus is asked, and that a
port nobody answers is held up like any other - and one more in core, the bus that remembers
against a fresh one on every port. The model was deleted from here, as the others were.

## The ULA

Ten facts, in `ula`, done against the emulator's `Ula`, which was 155 lines doing four jobs:
filling contention tables, being the 0xFE port, applying the waits, and holding the speaker.
Two classes came out, a `Ula` that works the answer out and a `TabulatedUla` that remembers it,
held to each other by an equivalence over the whole frame and by nothing else - the fourth
instance of that layer, after the memory's, the ports' and the picture's.

It was never brought home as one piece, because its facts did not belong to one piece. The beam
went with the picture's derivation, the waits with the machine's - `Spectrum.Waits`, a pattern
per model, the Pentagon's empty - and the port with the device that is the port, in
`machines/ula`. What is left in `machine/core` as `Ula` is the port contention arithmetic and the
table that memoises the model's answer, which is what the derivation said would be left, and
whether that table earns itself is a measurement, not a fact. Its facts are in `machines`:
`ScreenFollowsTheBeamTest`, `SpectrumTest`, `PentagonTest`, `UlaTimingsOverAFrameTest`,
`BorderComesFromThePortTest`, `BeeperMakesSoundTest`, `KeyboardTest`, `UlaIdlePortValueTest`,
`TapeDoesNotSilenceTheKeyboardTest` and `TheInterruptLineTest`. The model was deleted from here,
as the others were.

## The paging

Fourteen facts, in `paging`, done against the emulator's `RamInfo` - a class of six fields, two
of them the bytes last written to 0x7ffd and 0x1ffd and the other four copies of their bits, with
two empty subclasses. The fact that undid the copies was the snapshot's: what it keeps is the
byte, and putting it back has to give the same map, so the map became readings of the byte and
the copies had nothing left to say. Three classes came out, one per way of behaving, and the 48K's
has no state. What a `RamInfo` per machine was for, no fact could ask.

Its facts are `devices/all`'s `PagingTest`, fourteen against the real machines, and
`machine/core` has `Paging`, `Paging128` and `PagingPlus3` in the derived shape. The model was
deleted from here, as the others were.

## The machine

Thirteen facts, done against the emulator's `Spectrum` and the eight machines under it. One
class came out, and a model is its numbers and two rules; no fact asked for a subclass. What it
found: a +2A/+3 floating bus that no test asked for, with a `game` flag wired to 1, a static last
value shared by every machine, and three Amstrad machines that answered differently from each
other; a contention arithmetic carried whole where two numbers per model say the same thing;
the idle value written three times; and an array of line times that was one number.

Its tests are `machines`' `SpectrumTest`, 26 cases against the real machines, and `Spectrum`
in `machine/core` now has the derived shape: a `Waits` record per model, one `unattachedPort`
that asks the display for the bytes, one idle rule, and `lineStart(line)` from one number. The
quirk is gone. The model was deleted from here, as the memory model was, rather than left as a
second implementation of one concept.

## The picture

Eight facts, done against the emulator's `Display`, which was 238 lines doing five jobs. Five
classes came out of them: where the bytes are, what a pixel is right now, the picture as far as
the beam has got, the border's stripes, and the layer that remembers the addresses. Four of those
are `machine/core`'s `ScreenLayout`, `Colouring`, `Painting`, `Border` and `DirtyCells`, each
with its own test, and the facts that need a beam are `machines`' `ScreenFollowsTheBeamTest`.
What is left in `Display` is the fifth job - plotting up to where the beam is - and it has the
derived shape. The model was deleted from here, as the other two were.

## The timings

Eight facts, in `timings`, done against the emulator's `MachineTimings`, `TimingsHandler` and
the late-timings arithmetic in `Machine`. Two records came out, 53 lines: a `Span` of four parts
that the line in T-states and the frame in lines both turned out to be, and a `Timings` that is
a model's numbers and the four sums the emulator asks. What the emulator had instead was the same
numbers copied from one class to a second through a third - fifteen static getters with `null`
guards for a machine that does not exist, three fields nobody reads, four frame tables for
machines that are not here, and an AY clock carried by every machine and asked by none.

Its facts are `machines`' `TimingsTest`, the four that `SpectrumTest` did not already state, and
`MachineTimings` in `machine/core` is now that record with `Span` inside it; `TimingsHandler` is
gone. The one decision that moved is the late unit: `late()` on the timings whose number it
changes, with the flag staying the unit's. The model was deleted from here, as the others were.

## The sound

Seventeen facts, in `sound`, done against the emulator's `Sound`, its mixer. Five things came
out, 236 lines: a `Mixer` of 117, a `Source` and a `Card` that are what it needs of each, a
`Colouring` that is what a speaker does, and a `Synth` that holds a level until the next,
because smoothing the steps is a library's work and no fact asks for it. The shape they agreed
on with the emulator was the seam: a source asked once a frame, and the mix the sum.

Tried on the real machine, the facts found four things wrong rather than three: the synth ran
at twice the clock into a card opened mono, so a frame was 441 samples written twice each; every
model was sized at the 48K's clock; unpausing was a new machine and cleared the sources; and the
card was handed nothing when no source was in the list. All four are fixed in `machine/core`'s
`Sound`, which now has the derived shape, with `Colouring` beside it. One fact the derivation had
wrong: a write past the frame's end is kept only as far as the library's margin, some four
hundred T-states - measured, and stated as measured in `machines`' `SoundTest`, where the
fourteen facts that the beeper's, the chip's and the speed's tests did not already state now
live. The model was deleted from here, as the others were.

## The next one

A new package here, its own tests, and nothing depended on: the derivation has to start without
seeing the destination or it is not one. When it lands, the same question is what to compare,
and the end is the same: its facts move to the emulator's own tests, the emulator takes the
shape they earned, and the model is deleted from here. Nothing stays: as of the sound, this
module has no sources and is the record of seven derivations.
