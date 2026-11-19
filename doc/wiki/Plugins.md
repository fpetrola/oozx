# Plugins

Almost everything is a plugin: seventy-five of them ride inside the jar, and the emulator proper is a 48K
and the means of finding the rest. This page is what ships, how to add and remove them, and every way in a
plugin of your own can answer to.

## The window

`Emulator → Plugins...` has three tabs.

| Tab | What it shows |
|---|---|
| **Plugins** | what is installed and what the catalogue publishes that you do not have. *Install* and *Remove* move one across; *Check* asks the catalogue again; *Update* brings what is published now of what you have, and takes effect the next time you start |
| **Roles** | what there is of each kind: who answers to each way in |
| **Dependencies** | which plugin needs which |

The catalogue is the list of releases of [oozx-plugins](https://github.com/fpetrola/oozx-plugins), one jar
each. A plugin you do not have still shows what it would offer in `Discover → What do you want to do?...`:
choose that, and it is fetched and then does it.

A jar of your own goes in `~/.oozx/plugins/`. That is all installing one is.

## What ships

| Kind | Which |
|---|---|
| **Machines** (12 families) | the 128 and +2; Amstrad's +2A, +3 and +3e; the 16K and the NTSC 48K; the SE; the Chloes; the Chrome; the Czerweny, the Inves and the Microdigital clones; the Pentagons; the Scorpion; the Timex machines |
| **What machines are made of** (8) | the sound chip, the disk drive and controllers, memory paging, the Timex display chip, the shared hard disk code, snapshots, the tape, the tape formats |
| **Peripherals, and the debugger** (27) | → [Peripherals](Peripherals) |
| **Tools** (22) | the game browser and its catalogue, pokes and the poke finder, the recording player, the cassette deck and the real cassette, the map, the heatmap, frames, calls, trace, ports, the memory map, the keyboard, a machine's controls and settings, the Timex dock, the guide, and what a session is written with |
| **The desk's own** (6) | the looks, the menus' suggestions, the toolbar, window arranging, docking, and help |

## The ways in

A way in is an interface. A plugin is a jar with classes that implement some of them, and the build writes
into the jar which classes answer to which. Nothing is registered by hand, and nothing in the emulator names
a plugin's classes. → [Build a peripheral](Build-a-peripheral)

**Into a machine**

| Interface | What it lets you add |
|---|---|
| `Extension` | anything that binds itself into every machine that is built: a peripheral, a machine, a part of one |
| `RomsOfItsOwn` | the ROMs your machines and peripherals run on: which ship in your jar, and where the rest are published, with their SHA-256 |
| `StartsAMachineOn` | a kind of file a machine can be started on: which machine it wants, and what goes on happening afterwards, such as typing `LOAD ""` |
| `SnapshotFormat`, `SnapshotFile` | a snapshot format, read into a machine and written out of one |
| `TapeFormat` | a tape format, read into a cassette or written out |
| `Mapping` | what a file format carries of a part that is not the machine's own, so a peripheral's state can be saved without the peripheral knowing about files |

**Onto the desk**

| Interface | What it lets you add |
|---|---|
| `Equipment` | a window for a machine, listed under `Emulator → Equipment`. Clipping it on is what plugs it in |
| `DeskEquipment` | a window of the desk rather than of a machine: a browser, a library |
| `MachineTool` | a button on every machine's bar |
| `Explains` | a window under `Help` |
| `Session` | how the desk is written down when you quit, and brought back |
| `Action`, `Actions` | something a person can do: an item in a menu, its keys, a button. The desk has no menus of its own; they are made of these |
| `Part` | something beside the windows, such as a bar of buttons |
| `Look` | a family of looks the desk can wear |
| `WindowInside` | what a window of yours holds, kept when the desk closes and given back when it opens |

**About games**

| Interface | What it lets you add |
|---|---|
| `KnowsTheGames` | a catalogue: what is called what, when, by whom, and where its files are |
| `WhatGameThisIs` | who says which game a file is, and shows what is known of it |
| `AboutAGame` | a tile on a game's card: what you call it, how many there are for that game, and the page where they are seen and used. Pokes come this way |
| `BesideTheGame` | something that travels next to a game's file. Spec256's colours are this |

**On the picture**

| Interface | What it lets you add |
|---|---|
| `ScreenEffect` | one thing done to the picture on its way to the window, with the knobs that configure it |
| `Scaler` | a whole way of making 256 by 192 into a windowful |

## When yours needs another

Say so in your project as an ordinary dependency. The build writes it into your jar, and bringing your
plugin brings what it cannot work without. That is how the +D brings the parallel printer it has a port
for.

## What cannot be taken out

One machine: the 48K. Without a machine there is nothing to plug anything into.

## Where they are

The plugins are in [fpetrola/oozx-plugins](https://github.com/fpetrola/oozx-plugins), built against the
emulator and published a jar at a time: `devices/` for machines and peripherals, `tools/` for the rest. The
desk's own are in plugdesk, the base the desk is built on. Yours does not belong in either: a plugin is a jar
of your own, and it is found the same way those are.
