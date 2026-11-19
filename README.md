# OOZX

**The ZX Spectrum desk.** As many Spectrums as you like, forty years of hardware you plug in by hand, and
the whole catalogue a search away.

[![Build and Deploy](https://github.com/fpetrola/oozx/actions/workflows/maven.yml/badge.svg)](https://github.com/fpetrola/oozx/actions/workflows/maven.yml)

![Several Spectrums and the game browser on one desk](doc/zxenv2.gif)

OOZX is a ZX Spectrum emulator built as a desk. Machines, drives, printers and tape decks are windows on it,
and where a window sits is what it is plugged into. It knows which game you are running, from what is in the
file and not from its name, so the pokes, the maps and other people's recordings come with it. Underneath,
the Spectrum is made of objects down to the Z80, and the fast core is generated from that model.

## Five things to see first

1. **Any name. The right game.** Drop a file called `cinta3.tzx` on the desk: the machine it needs starts,
   and a card unfolds with everything there is about that game.
2. **Type a name. Play.** The game browser, `dizzy`, a double click. `LOAD ""` types itself.
3. **Drag it on. It is plugged in.** A peripheral is a window; dragging it against a machine plugs it in.
4. **Somebody else's playthrough, and then yours.** Watch a recording and take the machine over at any frame.
5. **An hour in twelve seconds.** Manic Miner at three hundred times its speed, with sound and picture on.

## Run it

A Java 21 runtime, or newer, and one file: `oozx-<version>.jar` from the
[latest release](https://github.com/fpetrola/oozx/releases/latest). Everything is inside.

```bash
java -jar oozx-<version>.jar
```

## Read on

**[The guide](https://github.com/fpetrola/oozx/wiki)** has three ways in:
[Play](https://github.com/fpetrola/oozx/wiki/Your-first-five-minutes),
[The desk](https://github.com/fpetrola/oozx/wiki/The-desk) and
[Under the hood](https://github.com/fpetrola/oozx/wiki/Why-it-all-fits). It is also inside the emulator,
under `Help → Guide...`.

What it cannot do is on one page: [Limits](https://github.com/fpetrola/oozx/wiki/Limits).

## Build it

JDK 21 or newer and Maven.

```bash
git clone https://github.com/fpetrola/oozx.git
cd oozx
mvn -DskipTests install
java -jar machine/app/target/app-<version>-all.jar
```

The rest is on [Building from source](https://github.com/fpetrola/oozx/wiki/Building-from-source). What
plugs in, the machines that are not the 48K, the peripherals and the tools, is
[oozx-plugins](https://github.com/fpetrola/oozx-plugins).

## Credits and license

The [Fuse](https://fuse-emulator.sourceforge.net/) authors, for the test vectors, the timing model and the
core the bridge runs; Cristian Dinu, for the [decoding of the Z80 opcode byte](http://www.z80.info/decoding.htm);
Peter Jakubčo's [emuStudio](https://www.emustudio.net/), for its Z80 test suite; [ZXInfo.dk](https://zxinfo.dk/),
[ZXDB](https://github.com/zxdb/ZXDB) and the [RZX Archive](https://www.rzxarchive.co.uk/), for the
catalogues. The part of ZXDB that ships with the emulator is a derived database, open under the same terms
as ZXDB itself (ODbL 1.0).

Licensed under the GNU General Public License, version 3 or later. See `LICENSE`.
