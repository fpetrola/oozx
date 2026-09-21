# Machines

Twenty-four models. Not twenty-four special cases: a model is a handful of declarations — how it pages,
which chip draws it, how its ULA measures a frame, which ROM it runs — and the machine is built from them.
The Pentagon was added "mostly to see what adding a machine costs"; the clones came in a single night
after that.

![Manic Miner on a 48K, drawn as the beam passes](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/manic-miner.png)

## Sinclair and Amstrad

| Model | What it is here |
|---|---|
| **Spectrum 16K** | The one Sinclair sold first: a 48K with three quarters of its memory missing, so everything above `0x7FFF` is bus and nothing else |
| **Spectrum 48K** | The machine. Contention per T-state, floating bus, issue 2 / issue 3 keyboard |
| **Spectrum 48K (NTSC)** | The same, where the mains gives sixty cycles: a shorter frame |
| **Spectrum 128K** | Paging on `0x7FFD`, the AY, the second ROM |
| **Spectrum +2** | The 128 in grey, with its own ROMs |
| **Spectrum +2A / +3** | The Amstrad pair: `0x1FFD` on top of `0x7FFD`, the special all-RAM modes, no floating bus, and on the +3 a real uPD765 with `.dsk` images in the drive |
| **Spectrum +3e** | The +3 with the IDE-oriented ROMs |

## Timex, SE, and the Eastern clones

| Model | What it is here |
|---|---|
| **Timex TC2048** | A 48K's memory and ROM socket with the SCLD doing the drawing: a second display file, 512×192 hi-res with no attributes at all, and 8×1 colour |
| **Timex TC2068** | A TC2048 with an AY on two ports of its own and a cartridge slot that covers the machine 8 K at a time |
| **Timex TS2068** | The same machine sold in America: fifty lines fewer, a slightly faster crystal, its own ROM |
| **Spectrum SE** | A 128 with a Timex display chip and cartridge slots holding RAM, so both kinds of paging are live at once and meet in one odd page at the top |
| **Chloe 140SE / 280SE** | The SE as its designer went on with it: ULAplus's sixty-four colours, and a register that asks the machine to run faster than it was built to |
| **Chrome** | A 128 with two more pages, two more ROMs and a port of its own — and one bit that puts all of it away and leaves a 128 behind |
| **Pentagon** | Contends nothing, decodes its ports fully, has no floating bus |
| **Pentagon 512K** | Five bits of page number where the port has three: the two missing ones are bits a 128 never decoded |
| **Pentagon 1024K** | A megabyte, by four more bits and a second port — which also says how to read the first, so the machine can become a later revision of itself |
| **Scorpion ZS 256** | A quarter of a megabyte through the two ports a +3 has, read differently: one bit puts page zero at the bottom, one picks a ROM of its own, one is simply a fourth page bit |

## The ones somebody's country made

| Model | What it is here |
|---|---|
| **Microdigital TK90X / TK95** | Brazil. A 48K with a ROM of its own, where the accents and the different messages live |
| **Czerweny CZ Spectrum / CZ Spectrum Plus** | Argentina. A 48K down to the ROM. Here because a machine that was sold is a machine |
| **Inves Spectrum+** | Spain. A 48K "in the way a copy of a drawing is the drawing": 64 K of RAM with the ROM over the first 16, so a program reads the ROM and writes underneath it; no contention anywhere; an unanswered port reads as all ones |

![The model combo, open](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/model-combo.png)

*Changing the machine is choosing it in the status bar. The list is the machines themselves, asked what they
are called.*

## Each one on its own ROM

Every one of these booted for 320 frames and had its picture taken, with nothing loaded and nobody typing:

| | |
|---|---|
| **Spectrum 48K** | **Spectrum 128K** |
| ![A 48K booting](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/boot-spec48.png) | ![A 128K booting](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/boot-spec128.png) |
| **Spectrum +2** | **Spectrum +2A** |
| ![A +2 booting](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/boot-specplus2.png) | ![A +2A booting](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/boot-specplus2a.png) |
| **Spectrum +3** — *Drives A:, B: and M: available* | **Spectrum +3e** — Garry Lancaster's loader, and the IDE count |
| ![A +3 booting](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/boot-specplus3.png) | ![A +3e booting](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/boot-specplus3e.png) |
| **Pentagon** — a 128's menu on a machine that contends nothing | **Spectrum SE** |
| ![A Pentagon booting](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/boot-pentagon.png) | ![An SE booting](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/boot-specse.png) |
| **Timex TC2048** | **Timex TC2068** — its own screen, behind a cartridge slot |
| ![A TC2048 booting](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/boot-tc2048.png) | ![A TC2068 booting](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/boot-tc2068.png) |

The 16K boots the 48K's screen with three quarters of its memory missing, and the Pentagon 512 and 1024 the
Pentagon's; the Chloe machines boot the SE's. The five that are not here — Scorpion, TK90X, TK95, Inves and
Chrome, and the CZ Spectrum Plus, which runs the same Spanish ROM Investronica put in theirs — stop with
*"couldn't find ROM"* until somebody fetches one, which is the next section.

## What a model actually is

Four declarations and no switch statements anywhere else:

- **How it pages.** `Paging128` (`0x7FFD`), `PagingPlus3` (`0x1FFD`), or a pager of the machine's own. The
  memory is a 2 KB page table with a `contended` flag per page, so "banking" is mutating that table.
- **Which chip draws it.** A ULA with partial decoding, one with full decoding (Pentagon), or an SCLD —
  which brings a second display file, hi-res and hi-colour. What the Timex and the Pentagon share turned out
  not to be a device but *a class of painting*: the one with no attributes in it.
- **How its ULA measures a frame.** `MachineTimings` is a record: the processor's clock in Hz and a `Frame`
  — a line as four spans (border, picture, border, retrace), a frame as lines the same way, how long `/INT`
  is held, and the T-state of the first pixel *as measured*, not as computed.
- **Which ROMs.** Per machine, and per **ROM set**: a 48K in English or Spanish, a +2 in French, a +3 at
  version 4.0 or 4.1, a TK90X in Portuguese. The same machine with other ROMs in it is not another machine,
  so it is a list of its own.

Every machine is then asked the same questions by the same tests — what it is called, which ROMs it runs,
which ports answer on it, what its contention table says, whether the screen follows the beam, whether a
snapshot picks it correctly. That is `machine/machines`: 65 classes and 44 test classes.

## ROMs this build cannot ship

![The emulator asking before fetching a ROM](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/rom-asking.png)

![Switching to a Scorpion, which fetches its ROMs and boots on them](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/rom-fetch.gif)

*Choosing the Scorpion in the model box: it says which three ROMs it needs and where they are published,
fetches them once you say yes, and the machine comes up on its own ROM — "© 1992 Scorpion ZS 256".*

Some machines' ROMs are not ours to distribute. For those the configuration declares **where the ROM is
published, what its SHA-256 must be**, and, for archives that publish a whole chip image, where inside it
the ROM starts. The emulator asks first, fetches from whoever publishes it, shows the download as it
arrives, and keeps the copy under your home directory. A kept copy is trusted only while it still matches
its hash. Twenty-seven such sources are declared.

## Switching machines

The model combo in a machine's status bar changes the machine. Putting a machine in **is** switching it on:
it finds its own memory rather than the last one's. A snapshot says which machine it was taken on and that
machine is what it loads onto; a TZX that declares its hardware gets that hardware.

## Where this lives

`machine/machines` (the models), `machine/core/.../speccy/machine` (`AbstractSpectrumMachine`, `Paging*`,
`MachineTimings`, `Roms`), `machine/core/src/main/resources/roms` (the ROMs and `README.copyright`).
