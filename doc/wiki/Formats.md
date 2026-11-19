# Formats

Every kind of file that is read and written, and how the readers are checked. Using them is on
[Tape, real and virtual](Tape-real-and-virtual) and [Your files](Your-files).

## Tapes

| Format | Read | Written |
|---|---|---|
| **TAP** | yes | no |
| **TZX** | yes, including the awkward blocks: generalized data, signal level, loop start and end, jump, direct recording, pure data | blocks `0x18` (CSW) and `0x15` (direct recording) |
| **CSW** | yes | inside a TZX |
| **PZX** | yes | no |
| **WAV** | no | no |

A tape can also be a real cassette, through the sound card.

## Disks

| Format | |
|---|---|
| **MGT, IMG, OPD/OPU** | read and written |
| **TRD, SCL** | read and written |
| **D40 / D80** | read and written |
| **DSK** (CPC and extended) | read |
| **UDI, FDI, TD0, SAD** | recognised and refused: *"images are not read yet"* |

The controllers are the uPD765 in the +3, and a WD1793 in everything of that family: the Beta 128, the +D,
the DISCiPLE, the Opus Discovery and the Didaktik. The drive itself is modelled: the motor, the heads, the
track under each head, write protection, and the gaps and marks each system writes when it formats.

There are also HDF and memory card images for the hard disk and card interfaces, Microdrive cartridges for
the Interface 1, and ROM cartridges for the Interface 2 and the Timex.

## Snapshots and recordings

| | |
|---|---|
| **Z80, SNA, SZX, SP** | read **and** written |
| **RZX** | read, written and replayed → [Recordings and Take Over](Recordings-and-Take-Over) |
| **AY** | from a game's card: a tune is played by a 128K running its own code → [Your files](Your-files) |
| **SCR** | from a game's card: a screen is shown as the Spectrum drew it |

A snapshot goes back onto the machine it was taken on. It carries its colours back if the machine had
sixty-four of them, and it says where it came from, which is how a Spec256 game finds its colour file.

The session is made of the same thing: each machine you leave open is kept as a snapshot.

## How the readers are held to it

The readers of tapes are checked against **libspectrum**, the library Fuse reads its files with, through the
bridge. Its own collection of awkward files is in the tree: six invalid TZXs, the loop and jump ones, the
turbo with no pilot, the pure-data block with zero bits used, an empty direct recording, a TAP with a broken
offset.

That comparison found three real things and closed them: the TZX signature was being counted as a block, a
TAP's blocks had no id where the format says `0x10`, and malformed files were being read as if nothing were
wrong. Today the eleven valid files give exactly the same blocks in the same order as the reference, and
five of the six invalid ones are rejected the same way. The sixth lies about the symbol counts *inside* a
generalized data block; catching that is a job and not a patch.

Readers arrive as plugins like everything else: a format is one of the ways in on [Plugins](Plugins).
