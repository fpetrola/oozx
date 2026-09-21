# Performance

A ZX Spectrum runs at 50 frames a second. This one runs a game at **15,256 frames a second with the sound
and the screen on**, on a laptop, on two pinned cores — about **305 times real time**. That is not a
stripped benchmark mode: it is the emulator's own loop, the same one you use, with the picture being drawn
and the audio being synthesised.

## The numbers

| | frames per second | times real time |
|---|---|---|
| Manic Miner, 48K, sound and screen on | **15,256** | ~305 |
| Jet Set Willy, 128K, sound and screen on | **12,867** | ~257 |
| The most a machine reaches while the sound card paces it | 14,177 | 28,355 % |
| Generated core vs the object model, as the app runs it | **4.3×** | |
| Generated core vs the object model, pure CPU, one JVM each | 4.5× | |

Conditions, verbatim from the diary: 3 September 2026, the app's own loop, generated core, JDK 21,
`taskset -c 2,3`, one-second blocks, the plateau of the last four. The machine is an i5-1335U notebook —
*not* a workstation.

## How it got there

The processor was only the first half. Once the generated core made the CPU cheap, each remaining layer was
measured, tuned in its own class, and measured again — none of them touching the model:

| Step | Change | Measured |
|---|---|---|
| §1 | Contention applied in **runs** instead of five lookups | MM +8 to +16 % |
| §2 | The clock without its timeout check | MM +3 to +6 %, JSW +10 %; garbage 951 → 756 bytes/frame |
| §3a | **One question per write** instead of five | MM +3 %, JSW +7 % |
| §3b | `byte[]` pages | **JSW +48 %** (and it exposed a signed-comparison bug) |
| §4 | Ports and keyboard without allocating | JSW +7 %; garbage down to 671 bytes/frame |
| §5 | The screen redone | `dirtySinclair` 17.4 % → 0.9 % of own time — **and fps unchanged**, so the per-cell redesign was discarded |
| §6 | The AY synthesised event to event | **JSW +9 %**; synthesis 16 % → ~1 % of the frame |
| §7 | The loop per instruction | neutral; kept the simpler one |
| §9 | **Sound as the pacer** | the pacing rule that stayed |

Start to finish that was 13,333 / 7,939 fps (MM / JSW) → **15,256 / 11,896**, and then 12,867 on JSW after
the AY: +14 % and +50 % over the starting point, after the 4.3× the generated core had already given.

Where the time goes now, by JFR on a JSW replay: the **whole generated core is 8.8 %** of the frame. Which
is the honest way of saying that making the processor infinitely fast from here would buy about 10 %. The
remaining cost is memory, contention, the screen and the sound — which is why the diary reads the way it
does.

## How these numbers were taken

This part matters more than the numbers:

- **Pinned, with the plateau method.** Unpinned, the same code measured 830 and 1,360 fps on the same
  afternoon: the instrument was broken, not the code. Pinned to one P-core, with one-second blocks and the
  mean of the last ones, dispersion went from ±12 % to about 1 %.
- **Interleaved A/B runs**, files swapped with `git checkout`, three to five rounds in both orders, best and
  mean reported.
- **The same work each time**: the variants of one experiment all executed *exactly* 29,097,384 instructions.
- **Declared noise floors**: ±12 % inside one unpinned run, ~1 % pinned, ±3 % on the RZX harness, ±8 % on the
  micro-benchmark, ±15 % between equal runs on this notebook. Two identical runs of one harness differed by
  12 %, and that is written down too.
- **Three harnesses gave three answers to the same change**, and the diary says so rather than picking the
  flattering one.
- Changes that measured neutral were **reverted**, including ones that were more elegant.

Every number above is in `doc/plan-superar-zxspin.md`, `doc/plan-nucleo-generado.md`,
`doc/plan-nucleo-generado-jvm.md` and `doc/plan-nucleo-generado-modulo.md`, with its conditions next to it.

![The speed dial going from 100 % to 22,000 %](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/turbo.gif)

*The same machine, the dial going up in steps: 100 %, 800 %, 6,648 %, 21,990 %. Manic Miner goes through the
Ore Refinery, the Bank, the Wacky Amoebatrons and the Solar Power Generator while it climbs.*

## Speed you can use

- The dial goes from **25 % to 40,000 %**, with the knee in the middle of the slider so the useful range is
  reachable.
- With sound on, **the sound card sets the pace** — the machine runs as fast as the audio line drains, so
  nothing stutters. Above 100 % the sound is dropped rather than stretched.
- Auto-loading a tape runs at 20,000 % and drops back when the load finishes.
- The processor can be changed **while the machine runs**: object model, generated core, or Spec256's nine.

## How does this compare with other emulators?

Straight answer: **this repository contains no measurement of any other emulator.** The one comparison that
appears in the diaries — "the generated core runs at the speed of the reference emulator; ZXSpin runs 20 %
faster than it" — is the *premise* the document started from, not a result. Three structural advantages over
the reference emulator's C were reasoned by reading its source (contention in runs; one question per write;
dirty-per-cell), and the third one was measured and **discarded**.

What can be said with the evidence in the tree:

- 15,256 fps with sound and screen on, and 4.3× over the object model, are measured here, with the
  conditions and the noise floor stated.
- The way to settle the rest is already in the repository: `machine/bridge` runs **the reference emulator's
  real core** (Fuse's, through JNA to `fuse_libretro.so`) behind the same interface as this one. Putting both
  behind the same harness and the same workload is the measurement, and nobody has run it yet.

Until that is run, the claim this project stands behind is the one it can back: *speed reached by design
rather than against it* — from an object model that was not compromised to get it, and that is still there,
still the reference, still switchable at runtime.

## Reproducing it

```bash
mvn -o -q -DskipTests install
taskset -c 2,3 java -jar machine/app/target/app-0.0.2-alu-SNAPSHOT.jar
```

Load a game, set the speed dial to its maximum, and read the percentage in the machine's status bar. For the
harness versions — the ones that run a fixed number of frames and report fps — see the measurement classes
under `machine/generated/src/test` and `machine/bridge`.
