# Limits, and what is not there

This project keeps a list of what it cannot do, and checks it. What follows is that list, checked line by line against the code
on 18 November 2026 — not a roadmap, a statement of fact. Where a thing is known to be broken rather than merely missing,
it says so.

## Not implemented

| | |
|---|---|
| **Disk images** | UDI, FDI, TD0 and SAD are recognised and refused: *"images are not read yet"* |
| **Tape** | No WAV files: a tape is a TAP, TZX, CSW or PZX file, or a real cassette through the sound card |
| **AY stereo** | the three channels are mixed equally into both ears. The mixer is stereo — two *sources* can differ per ear — but the chip does not pan its own channels |
| **Snow effect** | not modelled |
| **RZX recording** | writing exists at file level (`RzxWriter`, with extend/splice/prepend) and `goLive()` can record, but there is no button for it |
| **Rewind** | no interactive rewind. Cloning the state is what it would be built on |
| **Drag and drop** | no |
| **Installer** | none: one jar and a Java 21 runtime |
| **A key for the help** | none: `Help → Guide` is a menu item, and F1 goes to the Spectrum |
| **Command line** | `OOSpectrumLauncher.main` ignores its arguments: nothing opens from a shell |
| **Themes** | no automatic light/dark detection; the choice is manual and remembered |

## Known defects

- **The Opus Discovery has never been run on the generated core under a test.** The cause this page used to
  give is gone — the generated core now reaches a device's paged memory — but the Opus tests run on the object
  model and the generated-core runs do not include it, so nothing says it works.
- **The generated core reports no memory events** (read / write / contend); it does report the ports. Anything
  watching memory on the bus is watching the model instead.
- The browser's *Download* context item still says the feature is coming, although loading a game does fetch
  it, TOSEC included.
- Some ZXInfo fields are carried but never populated or shown (description, cover image, per-model
  compatibility, YouTube links, magazine references). Votes are used to rank the search, and not shown.

## Things that are true and easy to misread

- **The performance comparison with other emulators has not been measured.** The numbers in
  [Performance](Performance) are this emulator's own, taken with stated conditions and noise floors. No
  measurement of another emulator exists in this repository. The README used to say it was faster than
  emulators written in low-level languages; that sentence is gone until somebody measures it, and the way to
  settle it — Fuse's real core through the bridge — is in the tree and has not been run.
- **Fingerprinting can be fooled by a 128 K snapshot**: two snapshots of unrelated games were measured
  sharing 16,257 contiguous identical bytes, which is enough for one to claim the other. Whatever left that
  memory behind is not the game, and no amount of counting chunks can tell the difference.
- **The machine's speed depends on the machine you run it on.** The headline figures were taken on an
  i5-1335U notebook, pinned to two cores, with the plateau method. Unpinned, the same code measured 830 and
  1,360 frames a second on the same afternoon.

## Planned, with nothing written yet

Three peripherals are named and have no directory: SpeccyBoot, Spectranet, TTX2000S. (The Currah µSpeech and
µSource were on this list in September; both exist now.)

## Why this page exists

An inventory of the whole environment was taken in September 2026 — seven read-only passes over the tree,
each answering with paths as evidence, plus a list of everything the README claimed that the code did not
have. That list became this page, and the README was rewritten to claim only what was found.

It is in `doc/environment-inventory.md`, appendices and all, if you want to check any of this against the
code yourself. The inventory is September's; the lines above were checked again in November, each against the
file that decides it.
