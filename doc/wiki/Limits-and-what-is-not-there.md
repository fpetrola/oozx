# Limits, and what is not there

This project keeps a list of what it cannot do, and checks it. What follows is that list as of September
2026 — not a roadmap, a statement of fact. Where a thing is known to be broken rather than merely missing,
it says so.

## Not implemented

| | |
|---|---|
| **Disk images** | UDI, FDI, TD0 and SAD are recognised and refused: *"images are not read yet"* |
| **Tape** | TZX block `0x19` (generalized data) is skipped. No PZX, no WAV |
| **AY stereo** | the three channels are mixed equally into both ears. The mixer is stereo — two *sources* can differ per ear — but the chip does not pan its own channels |
| **Snow effect** | not modelled |
| **RZX recording** | writing exists at file level (`RzxWriter`, with extend/splice/prepend) and `goLive()` can record, but there is no button for it |
| **Rewind** | no interactive rewind. Cloning the state is what it would be built on |
| **Drag and drop** | no |
| **Command line** | `OOSpectrumLauncher.main` ignores its arguments: nothing opens from a shell |
| **Themes** | no automatic light/dark detection; the choice is manual and remembered |

## Known defects

- **The Opus Discovery does not work on the generated core.** `GeneratedSpectrumZ80.read/write` index the
  page table with no guard for a `DevicePage`, so its paged memory is never reached — and no gate sees it,
  because the Opus tests and the generated-core runs never cross.
- **The generated core reports no bus events** (memory read / write / contend), so anything watching the bus
  is watching the model instead.
- **Ctrl+T does two things**: Cassette Browser and Toggle Turbo.
- **CI builds on JDK 18** while `machine/generated` compiles with 21.
- The browser's *Download* context item still says the feature is coming, although loading a game does fetch
  it, TOSEC included.
- Some ZXInfo fields are carried but never populated or shown (description, cover image, per-model
  compatibility, votes, YouTube links, magazine references).

## Things that are true and easy to misread

- **The performance comparison with other emulators has not been measured.** The numbers in
  [Performance](Performance) are this emulator's own, taken with stated conditions and noise floors. No
  measurement of another emulator exists in this repository; the sentence about being faster than emulators
  written in low-level languages is a design claim, and the way to settle it — Fuse's real core through the
  bridge — is in the tree and has not been run.
- **Fingerprinting can be fooled by a 128 K snapshot**: two snapshots of unrelated games were measured
  sharing 16,257 contiguous identical bytes, which is enough for one to claim the other. Whatever left that
  memory behind is not the game, and no amount of counting chunks can tell the difference.
- **The machine's speed depends on the machine you run it on.** The headline figures were taken on an
  i5-1335U notebook, pinned to two cores, with the plateau method. Unpinned, the same code measured 830 and
  1,360 frames a second on the same afternoon.

## Planned, with nothing written yet

Five peripherals are named and have no directory: Currah µSpeech, Currah µSource, SpeccyBoot, Spectranet,
TTX2000S.

## Why this page exists

An inventory of the whole environment was taken in September 2026 — seven read-only passes over the tree,
each answering with paths as evidence, plus a list of everything the README claimed that the code did not
have. That list became this page, and the README was rewritten to claim only what was found.

It is in `doc/environment-inventory.md`, appendices and all, if you want to check any of this against the
code yourself.
