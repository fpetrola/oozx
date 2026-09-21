# The Desktop

One window holds a desk. Everything on it — a Spectrum, a cassette deck, a disk interface, the settings —
is a window on that desk, and where a window *is* means something.

![Three machines at once, at three different speeds](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/desk-tiled.png)

*Three Spectrums tiled on one desk: a bare 48K at the BASIC prompt doing 100 %, Jet Set Willy in 256 colours
on the nine-processor core doing 309 %, and Manic Miner on the generated core doing 9,775 %.*

## A window is a machine

`Emulator → New` (Ctrl+N) opens a Spectrum. Open five and you have five Spectrums: each with its own model,
its own speed, its own sound, its own pokes, its own screen settings, its own tape. They are separate
machines sharing one process, which is what an object graph per machine buys — see [Architecture](Architecture).

- The **status bar** says the speed it is actually reaching and which model it is; change the model there and
  the machine becomes that machine.
- The **toolbar** of each machine: turbo (right-click for the 25 %–40,000 % slider), border on/off,
  play/pause, mute (right-click for volume), pokes, game details, fullscreen (Esc leaves), zoom 1×/2×/3×,
  save a `.z80`, screen settings, favourite.
- `Alt+1` cascades, `Alt+2` tiles, `Ctrl+W` closes one and `Ctrl+Shift+W` closes all.

The keyboard goes to **the machine in front, or to the machine that the window in front is clipped to** —
so typing into a cassette deck types into the Spectrum it belongs to. While a text field, a combo or a modal
dialog has the focus, the machine hears nothing.

## Clipping windows together

A satellite window — a tape deck, a printer, a drive bay, the RZX player, the settings — can be **clipped
onto a machine's window**. Drag it near an edge and a glow line previews the snap; let go and it is attached:
it follows that machine when the machine moves, stays in front of it, and closes with it. Drag it off and it
is a free window again; drag it onto another machine and it belongs to that one now.

This is not decoration. For a peripheral, **clipping the window on is what plugs the device in**, and
unclipping or closing it is what unplugs it ([Peripherals](Peripherals)). For the settings window, what it is
clipped to is what it configures — let go of every machine and it configures what the *next* machine will
start with, and says so, in orange, across the top.

![The settings window clipped onto a machine](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/settings-tabs.png)

*The settings, clipped onto a machine: "Configuring this machine: Spectrum 48K" across the top, a tab per
part, and the Video tab built from the knobs the screen declares about itself.*

Windows that share an edge re-share it when one is resized. A window folds between compact and expanded.
Sliders live under the buttons. All of it is `AttachedFrame` in `machine/ui`, with tests of its own
(`AttachedFrameTest`, `PrinterDockingTest`, `RzxPlayerDockingTest`, `CollapseSizesTheFrameTest`).

## The session comes back

Quit and reopen: the desk is as you left it. The main window's bounds, which windows were open, and the
machines themselves — each one packed as a snapshot embedded in the configuration — so a Spectrum comes back
*running where it was*, with its pokes re-applied, its turbo, its mute and its pause as they were. One JSON
file, `~/.oozx/config.json`, a section per part, and unknown sections are preserved rather than dropped.

## The windows

| Window | What it is |
|---|---|
| **Machine** | A Spectrum: screen, toolbar, status bar, model combo |
| **Game Browser** | ZXInfo search, filters, thumbnails, and your own collection as a gallery |
| **Game Details** | Nine tabs: General, Technical, Publishers, Authors, Description, Screenshots, Game map, Releases, Downloads |
| **Snapshot History** | Every snapshot taken, double-click to load, *View Details* asks ZXInfo who it was |
| **Favorites** | Games and recordings, remembering which entry inside a zip it was |
| **Pokes** | Search, a checkbox per modification, Apply/Clear; un-applying puts the original bytes back |
| **Cassette Browser** | The blocks of a tape with progress, Play/Pause/Stop — works with no machine at all |
| **RZX Player** | Play a recording, loop it, and *Take Over* the machine mid-replay |
| **Real Cassette** | An oscilloscope on the sound card's input, with line selector and zoom |
| **Joystick** | What the Kempston port is reading, and which gamepad is doing it |
| **Keyboard** | A photograph of a 48K with the keys going down where the machine reads them |
| **Screen settings** | Self-describing knobs, named profiles, *Use as default* |
| **Settings** | A tab per peripheral, built from what each one declares |
| **Equipment** | Two dozen device windows — see [Peripherals](Peripherals) |
| **Debugger / Sprites** | See [Tools and Debugging](Tools-and-Debugging) |
| **README / About / Downloads** | F1 shows the README in a window, rendered |

## Themes

Dozens, under `Window → Look&Feel`, grouped by the family each comes from: FlatLaf and its IntelliJ themes
(Arc, Carbon, Cobalt2, Dracula, Gruvbox, Monokai Pro, Nord, One Dark, Solarized, Xcode Dark and more),
Material Theme UI Lite, Darklaf, JGoodies and Radiance — whichever of them is on the classpath.

Picking one tries it on and asks: *"Keep this look?"*, with a countdown that puts the old one back by itself
if nothing is confirmed. Which is the right way round for a list this long: an unreadable theme cannot trap
you in itself. The choice is saved and re-applied at startup. There is no automatic light/dark detection.

## Where this lives

`machine/app` is the desktop (34 classes): `ZXSpectrumDesktopApp` is the frame and the menus,
`OOSpectrumLauncher` the entry point. `machine/ui` (20 classes) holds the window mechanics and the screen,
and depends on nothing of the emulator — see [Architecture](Architecture).
