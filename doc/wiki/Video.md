# Video

Two different things, kept apart on purpose: **what the machine drew**, which is the Spectrum's business,
and **how that picture reached your eyes**, which is the television's.

## What the machine drew

- **The beam draws it.** Each cell is painted as the beam passes over it, so a program that changes an
  attribute halfway down the screen gets what it asked for. The border is kept as a list of changes by beam
  position — the rainbow border of a loading screen is not a special effect here, it is what happens.
- **Contention per T-state.** Per model, from the ULA's own measured tables. `MachineTimings` records the
  frame as four spans — border, picture, border, retrace — the length of `/INT`, and the T-state of the first
  pixel as measured rather than computed.
- **Floating bus** where the machine has one (not on +2A, +3 or Pentagon).
- **Flash** every sixteen frames.
- **Timex modes.** The SCLD's second display file, 512×192 with no attributes at all, and 8×1 colour. A
  column is worth as many pixels as the machine says it is, and the picture is as wide as that makes it —
  including the border, which does not know it grew.
- **ULAplus**: sixty-four colours a program chooses for itself.
- **Spec256**: 256 colours per pixel, from eight planes — [its own page](Spec256-256-Colours).

A column of the screen is painted by whoever says its pixels are theirs, and by the machine's own rules when
nobody does. That is the seam that lets a Timex, a Pentagon and a Spec256 game all be "the screen" without
anyone holding a list of machines.

## How the picture reaches you

The picture is `int[]` that a window wraps with no copy. On top of that, a chain of effects with knobs that
describe themselves — the settings window is *generated* from them, so a new knob appears without anyone
writing a dialog:

| Knob | What it does |
|---|---|
| **Scaler** | Nearest neighbour, Bilinear, Sharp bilinear, Scale2x, Scale3x, Edge directed, xBRZ |
| **Lead** | How the picture arrives: RGB monitor, Scart, Composite, Aerial (RF) |
| **Show border** | Border on or off |
| **Scan lines** | Drawn by where a row falls in a machine line, not by whether it is odd |
| **Phosphor layout / depth** | The mask a tube has |
| **Persistence** | How long a pixel takes to let go |
| **Tint** | The colour the whole thing leans to |
| **Brightness** | How bright, all over |
| **Colour depth** | How much colour survives |

Settings are per machine — each window has its own — and can be **saved as named profiles**, with *Use as
default* and *Restore default*. `Options → TV` has the lead and the scan lines in the menu as well.

### Which lead it came down

The same frame of Manic Miner, the same machine, four leads. A composite signal carries colour with a
quarter of the bandwidth it carries brightness, so colour smears sideways and brightness does not; an aerial
adds the modulation on top of that.

| RGB monitor | Scart |
|---|---|
| ![Manic Miner through an RGB monitor](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/lead-rgb.png) | ![Manic Miner through Scart](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/lead-scart.png) |
| **Composite** | **Aerial (RF)** |
| ![Manic Miner through composite video](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/lead-composite.png) | ![Manic Miner through an aerial](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/lead-aerial.png) |

Where it shows most is on text, enlarged here from the same four pictures:

| | |
|---|---|
| RGB | ![Text through RGB](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/lead-rgb-detail.png) |
| Scart | ![Text through Scart](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/lead-scart-detail.png) |
| Composite | ![Text through composite](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/lead-composite-detail.png) |
| Aerial | ![Text through an aerial](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/lead-aerial-detail.png) |

### The scalers

96 by 72 pixels of the Central Cavern, asked of each scaler at four times the size:

| Nearest neighbour — exact, and uneven at odd sizes | Bilinear — even, and soft |
|---|---|
| ![Nearest neighbour](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/scaler-nearest-neighbour.png) | ![Bilinear](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/scaler-bilinear.png) |
| **Sharp bilinear** — sharp where the picture lands on whole pixels | **Scale2x** — only colours that were already there |
| ![Sharp bilinear](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/scaler-sharp-bilinear.png) | ![Scale2x](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/scaler-scale2x.png) |
| **Edge directed** — this project's own | **xBRZ ×4** — the one that actually rounds a diagonal off |
| ![Edge directed](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/scaler-edge-directed.png) | ![xBRZ](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/scaler-xbrz-4x.png) |

**Edge directed** is this project's own: it doubles like Scale2x, and where a corner sits inside a diagonal
run it mixes the two colours rather than choosing one, so a staircase becomes a slope. It is a smaller idea
than what hqx and xBR do — they look at a wider neighbourhood and weight the blend by how sure they are.
**xBRZ** is somebody else's work (Zenju's algorithm, Stanio's Java port), used unmodified as a dependency
and credited in `NOTICE`. Measured on a frame of Ping Pong it turned nine colours into sixty-eight, and cost
2.3 ms a frame against Scale2x's 2.1.

## The picture as a value

Because the picture is just a buffer and the display just fills it, a screen can be taken with no window at
all: the three Spectrum screenshots in this wiki were produced by a headless test that builds a machine,
loads a snapshot, runs 120 frames and writes the buffer to a PNG. That is also how the regression tests
work — a hash mismatch dumps the screen under `target/regression-screens` so you can look at what changed.

## Where this lives

`machine/core/.../modules/display` (`Display`, `Picture`, the beam), `machine/core/.../modules/ula` (`Ula`,
the contention tables), `machine/ui/.../screen` (`ScreenSettings`, `Scalers`, `TvScreen`, `ScreenEffects`,
`SpeccyScreen`). The ui module depends on nothing of the emulator.
