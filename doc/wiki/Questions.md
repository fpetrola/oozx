# Questions

Short answers, as things stand on 19 November 2026.

## Getting it running

**Does it run on Windows, macOS and Linux?**
Yes: it is one jar, and it needs Java 21 or newer. There is no installer yet.

**Do I need a JDK?**
No. A Java runtime is enough. A JDK is only for [building it from source](Building-from-source).

**Can I open a game by double-clicking it, or from a command line?**
Not yet. Start the emulator and drop the file on it, or use `File → Open`.

**Is there a key for the help?**
No. `Help → Guide...` opens these pages inside the emulator. F1 belongs to the Spectrum.

## Games

**Can I drag a file onto it?**
Yes. A tape, a snapshot, a zip or a recording, dropped anywhere on the desk. → [Your files](Your-files)

**Does it recognise my collection?**
Yes, by what is inside each file and not by its name: 4,922 games today. A file it cannot name is still
loaded, and stays in your library as unknown.

**Can I watch a tape load as it did?**
Yes. *Fast Loading* ships switched on, so a tape is over in a moment. Untick it under
`Emulator → Settings... → Machine` and the tape plays at its own speed, stripes and noise included. → [Tape, real and virtual](Tape-real-and-virtual)

**Does it work without the network?**
The catalogue does, for the 5,000 most voted games, and so does everything you already have on disk.
Fetching a game needs the network. → [Network and privacy](Network-and-privacy)

**Does it record my games?**
It replays other people's and lets you take over. There is no button to record yours yet.
→ [Recordings and Take Over](Recordings-and-Take-Over)

**Where do the pokes come from?**
From the people who found them, gathered at the Tipshop. → [Legal](Legal)

## Machines and peripherals

**Why does a machine ask me before it starts?**
Its ROM is not mine to ship. It tells you who publishes it, and fetches it once you say yes.

**Why does a peripheral say `no ROM`?**
Some peripherals had a ROM of their own that does not ship. Those you supply. Which ones, and how, is on
[Peripherals](Peripherals).

**Does my gamepad work?**
A gamepad is the Kempston joystick of the machine in front. It is read with a library that exists for the
three systems; I have not tried it on all of them.

**ZX81? Spectrum Next? SAM Coupé?**
No. Twenty-four Spectrums and their clones. → [Machines](Machines)

## Speed

**Is it faster than other emulators?**
Than Fuse, replaying recordings: 25 % faster on average over nine games, at the same fixed clock and with
nothing drawn on either side. I have not measured it against any other emulator, nor the loop you play on.
→ [How fast, and how we know](How-fast-and-how-we-know)

**Why does it run at 100 % if it can do three hundred times that?**
Because a Spectrum does. The rocket on a machine's bar lets go of it.

## Your things

**Where does it keep my things?**
Under `.oozx` in your home folder, and nowhere else. What each part of it is, is on
[Network and privacy](Network-and-privacy).

**How do I start again from nothing?**
Close it and move `~/.oozx` away.

**How do I update?**
The emulator: download the new jar. The plugins: `Emulator → Plugins... → Update`.

## The project

**Who makes it?**
Fernando Damian Petrola. It is free software, under the GNU General Public License, version 3 or later.

**Can I write something for it?**
Yes, and it is meant to be small work. → [Build a peripheral](Build-a-peripheral)

**Where do I say something is wrong?**
[The project's issues](https://github.com/fpetrola/oozx/issues), or the page of [Limits](Limits) if it is
already known.
