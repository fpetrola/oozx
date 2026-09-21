# Recordings (RZX)

An RZX is somebody's whole playthrough: not a video, but the machine's inputs, frame by frame. Replaying one
means running the real machine again and feeding it what the player fed it — which only works if the
emulator is deterministic to the fetch.

## Watching one

`Emulator → RZX Player` opens the player, or open an `.rzx` (or a zip holding one — it asks which part) from
`File → Open`. The recording gets a machine of its own, named *Spectrum #N*, and the player window clips onto
it: the block table with progress, Play, Stop, Loop, Favorite.

**Several at once.** Each recording is a machine, so a handful can play side by side, each at its own speed.

![Taking over a recording of Exolon](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/rzx-takeover.gif)

*Exolon replaying from somebody's RZX — and then the button: the player says "Taken over — the machine is
yours, playing on from here", and the keys are ours from that frame on.*

**Take Over.** The button that makes the feature: at any point during a replay, take the machine. The
recording stops feeding it inputs, the keyboard becomes yours, and you carry on from exactly where the
recording had got to.

## Where the recordings come from

Two catalogues, joined: ZXDB's own downloads, and the **RZX Archive**, catalogued here as data — about 4,291
recordings with title, note, submitter, the archive's page and the game's ZXInfo id, which is what makes the
two joinable. The 243 entries the archive marks as distribution-denied are recorded as denied rather than
dropped. The browser offers a game's recordings from either source.

## How the replay works

- `RzxPlayback` closes a frame by **counting M1 fetches** through the R register's delta — that is what the
  format's frames are measured in.
- `RZXPlayerIO` replaces the machine's I/O: every `IN` the program does is answered from the recording, in
  order, and the player keeps sync statistics as it goes.
- `goLive()` is Take Over: it hands the real keyboard back.

Writing exists too: `RzxWriter` can extend, splice and prepend, and `goLive()` can record. There is no
recording button in the interface yet, and no interactive rewind.

## Why this is a test and not only a feature

A recording is a determinism oracle. If the emulator is off by a single fetch anywhere, the replay
desynchronises and says so. `zx-rzx` has twenty tests that replay recordings and check the fetches per
frame, and those tests are part of the gate — see [Correctness and Tests](Correctness-and-Tests).

They were also a performance measurement: `jsw-full.rzx` replayed at 21,186 % of real time before a
refactor and 20,773 % after, which is how "did this cost anything" gets answered around here.

## Where this lives

`machine/media/rzx` and `zx-rzx` (`RzxParser`, `RzxPlayback`, `RzxWriter`, `RZXPlayerIO`, `RzxSession`,
`RzxArchive`), with the player window in `machine/media/rzx`.
