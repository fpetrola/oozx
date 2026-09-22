# Tapes, Disks and Snapshots

## Tapes

| Format | Read | Write |
|---|---|---|
| **TAP** | yes | — |
| **TZX** | yes, including the awkward blocks: generalized data, signal level, loop start and end, jump, direct recording, pure data | blocks `0x18` (CSW) and `0x15` (direct recording) |
| **CSW** | yes | inside a TZX |

TZX block `0x19` (generalized data) is listed and skipped. PZX and WAV are not read.

![A tape loading](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/tape-loading.gif)

*Opening a tape: a machine appears, `LOAD ""` types itself, the border stripes, and Dizzy's loading screen
arrives a line at a time while the deck says which block it is on. Filmed at four times real speed; the
auto-loader normally does it at twenty thousand.*

### The deck

Each machine has a tape deck of its own — a window you clip onto it. It shows the blocks with progress and
Play/Pause/Stop, and it works **with no machine at all**, so you can look inside a tape before deciding what
to do with it. Loading is audible if you want it: you hear the tape, the way it was.

- **Auto-load** types `LOAD ""` for you, runs the load at 20,000 % and drops back to 100 % when it is done.
- A TZX that declares its hardware gets that hardware: the tape picks the machine.
- Changing the speed mid-load used to leave the next tape edge waiting for a T-state that had already gone
  past. It does not any more.

### A real cassette

`Emulator → Real Cassette (audio in)` opens an oscilloscope on the sound card's input: choose the line, zoom,
follow the head, and *Hear* what is coming in. It is the same piece of equipment as the cassette deck beside
it, with a different cassette in it — one made of air rather than of a file — so it clips onto a machine the
same way, and while it is clipped on **what comes in the sound card drives that machine's ear line**. Carry
it away and the machine goes back to reading its own tape.

## Disks

| Format | |
|---|---|
| **MGT, IMG, OPD/OPU** | read and written |
| **TRD, SCL** | read and written |
| **D40 / D80** | read and written |
| **DSK (CPC, EXTENDED)** | read |
| **UDI, FDI, TD0, SAD** | recognised, and they say so: *"images are not read yet"* |

Controllers: the uPD765 in the +3, and a WD1793 in everything that shares that family — Beta 128 (TR-DOS,
built into the Pentagon), +D, DISCiPLE, Opus Discovery, Didaktik 40/80. The drive itself is modelled: the
motor, the heads, the track under each head, write protection, and the gaps and marks each system writes
when it formats.

There are also HDF and SD card images for the IDE and MMC boards, Microdrive cartridges for the Interface 1,
and ROM cartridges for the Interface 2 and the Timex.

## Snapshots

| | |
|---|---|
| **Z80, SNA, SZX, SP** | read **and** written |
| **RZX** | read, written and replayed — [its own page](Recordings-RZX) |

A snapshot is loaded onto the machine it was taken on. It also carries its colours back if the machine had
sixty-four of them, and it says where it came from — which is how a Spec256 game finds its `.gfx` without
anything else in the tree knowing what a `.gfx` is.

The session uses the same machinery: `SnapshotUnicodePacker` packs a gzipped, Base64 snapshot into the
configuration JSON, which is why your machines come back running after a restart.

## How the readers are held to it

The formats are checked against **libspectrum**, the reference library, through the JNA binding in
`machine/bridge`. Its own regression corpus is in the tree — six invalid TZXs, the loop and jump ones, the
turbo with no pilot, the pure-data block with zero bits used, an empty direct recording, a TAP with a broken
offset — and `ReferenceOracle` walks a tape with the reference while `WhatJavaMakesOfTheCorpusTest` prints
what each one sees, file by file.

That comparison found three real things and closed them: the TZX signature was being counted as a block, a
TAP's blocks had no id where the format says `0x10`, and malformed files were being read as if nothing were
wrong. Today the eleven valid files give exactly the same blocks in the same order with the same ids as the
reference, and five of the six invalid ones are rejected the same way. The sixth, `invalid-gdb.tzx`, lies
about the symbol counts *inside* a generalized data block; catching that means implementing the block, which
is a job and not a patch.

## Where this lives

`machine/media/tape`, `machine/media/snapshot`, `machine/media/rzx`, `machine/machines/disk`, and the disk
interfaces under `devices/*`. The working notes are `machine/doc/formatos.md`.
