# Peripherals

Twenty-four devices. Each one is a Maven module, a jar and two one-line service files; each one is a window
on the desk; and **clipping that window onto a machine's window is what plugs the device in**. Unclip it and
it is unplugged. Carry it to another machine and it is that machine's now.

![Dragging the Kempston Mouse window onto a machine plugs it in](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/clipping.gif)

*The Kempston Mouse, dragged across the desk and let go against the machine. Watch its label: "not plugged
into a machine" becomes "over the machine's picture" the moment it snaps.*

![A machine with the printer, the mouse and a Covox clipped onto it](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/peripherals-clipped.png)

*Manic Miner running with three boards plugged in: the ZX Printer with its paper, the Kempston Mouse with
its Hold and its sensitivity, and a Covox with its level meter.*

The whole set is 8,305 lines of Java, licence headers not counted. The smallest, ZXMMC, is 44 lines in three
files. Most of the historical expansion hardware landed in a single sweep of about two days.

## What is there

### Disk and tape interfaces

| | |
|---|---|
| **Beta 128** | TR-DOS on a WD1793. Built into the Pentagon, pluggable on a 48K or 128K. Its *Boot* button resets into TR-DOS with the 48 BASIC underneath and boots from drive A |
| **+D** | The MGT interface, with its NMI button for the snapshot menu, and a printer on its port |
| **DISCiPLE** | The same family, its own ROM, its own NMI button |
| **Opus Discovery** | `.opd` and `.opu` images, on what every WD interface shares |
| **Didaktik 40 / 80** | The Czech interface, with the *SNAP* button: an NMI its ROM takes over to save what is running |
| **Interface 1** | Eight Microdrives with motor lights, the shadow-ROM lamp, an RS-232 terminal on the desk, and the ZX Net plugs. 1,163 lines, the largest device here |

### Storage cards

| | |
|---|---|
| **DivIDE** | The automapper's EPROM and RAM, an ATA channel over HDF files, the write-protect jumper and the NMI button |
| **DivMMC** | A card the machine talks to a byte at a time |
| **ZXMMC** | The same idea, 44 lines of it |
| **Simple 8-bit IDE**, **ZXATASP**, **ZXCF** | The three IDE/CompactFlash boards, with bays where you insert a disk image and *Save* is the commit |

### Sound

| | |
|---|---|
| **Melodik** | An AY for a machine that has none, so a 48K can hear 128K music |
| **Fuller Box** | Its AY on `0x3F`/`0x5F` and its joystick on `0x7F`, with a socket and a write light |
| **Covox** | A DAC on a port, with a level meter and volume |
| **SpecDrum** | Cheetah's drum machine, the same way |

![The ZX Printer printing a screen](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/printing.gif)

*`PRINT "OOZX"`, `PLOT 0,0`, `DRAW 255,175`, `COPY` — typed into the machine, and the ZX Printer bringing it
out a line at a time, drawn as a burn rather than as a bitmap.*

### Printers and pointers

| | |
|---|---|
| **ZX Printer** | The belt and the paper, the printout drawn as a burn rather than a bitmap, zoomable, tear off, save as PNG |
| **Parallel printer** | Continuous paper, text out |
| **Kempston Mouse** | Your desk mouse moves over the picture; *Hold* captures the pointer; sensitivity, swap, record |
| **Joystick** | Cursor, Kempston, Sinclair 1/2, Timex 1/2 and Fuller, plus a physical gamepad |

### Cartridges, freezers and the rest

| | |
|---|---|
| **Interface 2** | A cartridge takes the place of the ROM, and two joystick sockets |
| **Multiface One / 128 / 3** | The red button, and what the processor needed for it to work. Three entries in the menu over one window |
| **ULAplus** | Sixty-four colours a program picks for itself. Not a module of its own — it comes with the kit, and it is a machine's palette rather than a board, so plugging it in is what gives a machine those colours |
| **Spec256** | Nine processors in lockstep and 256 colours — [its own page](Spec256-256-Colours) |
| **Debugger / Sprite viewer** | [Tools and Debugging](Tools-and-Debugging) |

![The Interface 1 with its eight microdrive bays](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/interface1-microdrives.png)

*The Interface 1, expanded: eight microdrive bays with their motor lamps, and the RxD, TxD and ZX Net files
for the RS-232 and the network. The machine above it is at its boot screen because plugging in a board that
brings a ROM resets it — as it would.*

## The front panels

A device window is not a settings dialog. It is the front of the thing:

- A **drive bay** per drive: a media slot with a motor LED, *Insert*, *Eject*, *New*, *Save*, *Flip*,
  *Write-protect*, a lamp for the paged ROM, the board's own button (*Boot*, *SNAP*, *NMI*, the red one), and
  an expanded view showing the track under each head.
- An **IDE bay** per disk, where *Save* is the commit.
- A **DAC** front with a level meter and a volume knob.
- The Interface 1 with its eight Microdrives and a terminal window for the RS-232.

Everything a panel does reaches the machine on the emulator's own thread (`z80.later(...)`), never from Swing.

## How a device is found

Two independent `ServiceLoader`s, two one-line files in each jar:

- `Extension` — a Guice module. It contributes the `Peripheral` to a multibinder and, if it has settings, a
  configuration section. This is what makes the device exist inside a machine.
- `Equipment` — `name()` and `open()`. This is what puts it in the `Emulator → Equipment` menu and opens its
  window.

`machine/devices/all` has no source at all: it depends on the twenty-four jars so that the classpath carries
every service file, and `machine/app` depends only on it. **The core never names a device.** A device, for
its part, may use the ULA, the mixer and the clock, and nothing else.

What a machine brings stock — the 128's pager, both ULAs, the +3's FDC, the 128's AY — stays in the core:
that is not plugged in, that *is* the computer.

## The contract

```java
public interface Peripheral {
  void activate(Machine machine);
  void deactivate();
  Ports getPorts();
  boolean fitsOn(Machine machine);
  boolean isWanted();
  boolean hasHardReset();
}
```

Plus `Pluggable` (`plugIn(connected)`, `isPluggedIn()`) for the ones the window plugs and unplugs. Ports are
answered on a bus that merges answers the way the real one does — a wired AND with a "driven" flag, the
floating bus for undriven bits, and plugged-in devices answering before the machine's own chips.

Writing one is [Extending OOZX](Extending-OOZX).

## Where this lives

`machine/devices/<name>` — one module each. `machine/devices/kit` holds the window shapes
(`DriveBayFrame`, `IdeBayFrame`, `DacFrame`) and `machine/devices/ide` the shared ATA code: 2,150 lines of
infrastructure that the twenty-four share.
