# Getting Started

## What you need

- **JDK 21 or newer.** A JDK, not a JRE: the build generates and compiles the fast core, and it needs a
  compiler to do it.
- **Maven.**
- Nothing else. The ROMs a 48K, a 128K, a +2, a +3, a Timex and an SE need are in the source tree; so are
  3,683 pokes, a catalogue of 4,922 games and one of 4,291 recordings.

## Build and run

```bash
git clone https://github.com/fpetrola/oozx.git
cd oozx
mvn -DskipTests install
java -jar machine/app/target/app-0.0.2-alu-SNAPSHOT-all.jar
```

The first build takes a while because it writes the generated core (about 11 s) and compiles it (about 2 s)
on top of everything else. After that the build only re-writes it when the model it came from has changed —
it hashes the model in under a second and skips the work otherwise. See [The Generated Core](The-Generated-Core).

## The first minutes

![Three machines at three speeds](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/desk-tiled.png)

1. **A machine.** `Emulator → New` (Ctrl+N) opens one. The combo in its status bar says which model it is;
   change it there and the machine becomes that machine. The window is the machine — not a picture of one:
   close it and that Spectrum is gone.
2. **A game, without files.** `Emulator → Game Browser` (Ctrl+B) searches [ZXInfo.dk](https://zxinfo.dk).
   Right-click a title → *Load Game* and pick a model, or let the tape say which one it wants. See
   [Games: catalogue and library](Games-Catalogue-and-Library).
3. **A game you already have.** `File → Open` (Ctrl+O) takes tapes, snapshots, disks and recordings; the
   file decides where it goes — a recording opens the RZX player, everything else opens a machine.
4. **Speed.** The rocket on the machine's toolbar turns turbo on; right-click it for the slider, 25 % to
   40,000 %. With sound on, the sound card sets the pace, so it does not stutter.
5. **Hardware.** `Emulator → Equipment` lists the devices. Open one and **clip its window onto the
   machine's window**: that is what plugs it in. See [Peripherals](Peripherals).
6. **Quit.** `File → Quit` (Ctrl+Q). Everything that was open comes back next time, machines included,
   running where they were.

## Where things are kept

| | |
|---|---|
| `~/.oozx/config.json` | every setting, the open windows, and the machines themselves as packed snapshots |
| `~/.oozx/` | ROMs fetched on demand, and whatever a device saves |
| `machine/core/src/main/resources/roms/` | the ROMs this build ships, with `README.copyright` saying whose each one is |

Some real machines' ROMs are not ours to ship. For those, the configuration records where the ROM is
published and what its bytes must hash to; the emulator asks before fetching one, fetches it from whoever
publishes it, shows a window with the download as it arrives, and keeps the copy under your home directory
rather than in the source tree. A kept copy is believed only while it still matches.

## Keyboard shortcuts

| | | | |
|---|---|---|---|
| Ctrl+N | New machine | Ctrl+O | Open a file |
| Ctrl+B | Game browser | Ctrl+1..0 | Recent files |
| Ctrl+S / Ctrl+L | Save / load state | Ctrl+Space | Pause |
| Ctrl+M | Mute | Ctrl+T | Cassette browser |
| Ctrl+W | Close window | Alt+1 / Alt+2 | Cascade / tile |
| Esc | Leave fullscreen | F1 | The README, in a window |

## Running it from source

```bash
mvn -o -q -DskipTests install          # the whole reactor
mvn -o -pl machine/core test           # one module's tests
mvn -o -pl machine/generated test      # the generated core against the same batteries
```

The reactor is 63 `pom.xml` files: `emulator` (the Z80), `machine/*` (the Spectrum, the devices, the
desktop), `zx-rzx` (recordings), `translation/*` (a game turned into Java), `prototypes/*`. What each one
owns is in [Architecture](Architecture).
