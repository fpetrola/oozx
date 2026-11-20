# Limits

I keep a list of what this cannot do, and I check it. What follows is that list as it stood on 19 November
2026: a statement of fact, not a plan. Where a thing is known to be broken rather than merely missing, it
says so.

## Not there

| | |
|---|---|
| **An installer** | none: one jar, and a Java 21 runtime |
| **Opening from outside** | nothing opens from a command line or from a double click on a file: the arguments the program is started with are ignored |
| **A key for the help** | none: `Help → Guide...` is in the menu, and F1 goes to the Spectrum |
| **Recording your own game** | the format is written as well as read, but there is no button |
| **Rewind** | no |
| **WAV tapes** | no: a tape is a TAP, TZX, CSW or PZX file, or a real cassette through the sound card |
| **Some disk images** | UDI, FDI, TD0 and SAD are recognised and refused: *"images are not read yet"* |
| **Stereo from the 128's sound chip** | its three channels come out equally in both ears |
| **Snow** | the effect is not modelled |
| **Following your system's light or dark** | the look is chosen by hand, and remembered |
| **Other computers** | no ZX81, no Next, no SAM Coupé |

Three peripherals are named and not written: SpeccyBoot, Spectranet and the TTX2000S.

## Known to be wrong

- **The snapshot history stays empty.** The window is there, under `Emulator → Snapshot History...`, and what
  should fill it is never started.
- **The game browser's *Download* says the feature is coming**, although loading a game does fetch it.
- **The fast core tells nobody about memory.** It reports what happens on the ports, but anything that
  watches reads and writes of memory is watching the object model instead.
- **The Opus Discovery has never been run on the fast core under a test.** Its tests run on the object
  model, so nothing says it works on the other.
- **Some of what the catalogue knows is not shown**: a game's reviews in magazines, its videos, the other
  machines it was made for.

## Things that are true and easy to misread

- **Some peripherals need a ROM you supply.** The Beta 128, the Interface 1, the Multiface, the Opus, the
  Didaktik, the DivIDE, the DivMMC, the µSpeech and the µSource had ROMs of their own that are not mine to
  ship. → [Peripherals](Peripherals)
- **Its speed has been compared with one other emulator, Fuse, and only replaying recordings**: 24 % faster
  on nine recordings, tied on one of them, at a fixed 2 GHz with no picture and no sound on either side. The
  loop you play on has not been compared, and the frames-a-second figures on
  [How fast, and how we know](How-fast-and-how-we-know) are from September and have not been taken again on a
  released jar.
- **The speed you get depends on your computer.** The figures were taken on a notebook, pinned to two cores.
  On one harness the same code ran at 830 frames a second with its thread wandering between cores, and at
  1,360 pinned to one.
- **The desk's keys are not a machine's.** While a machine has the focus, Ctrl and Alt are the Spectrum's
  SYMBOL SHIFT, so Ctrl+T and the rest do nothing until another window has the keyboard.
  → [Shortcuts](Shortcuts)
- **The mouse and a program can part company.** A program that shuts its pointer inside a menu, or stops
  reading the mouse while it is busy, is no longer under your hand until your pointer leaves the picture and
  comes back. → [Sound and controls](Sound-and-controls)
- **A peripheral coming back with your session** has not been checked one by one. The machines come back
  running; whether each peripheral comes back holding what it held, I have not gone through.
- **The scene's own test programs have not been run**: zexall, z80test and the games known to be hard.
  → [How right, and how we know](How-right-and-how-we-know)
- **The map is a prototype**, and the translator is a separate tool that does not plug into the desk.

## Why this page exists

In September 2026 the whole tree was gone through, seven passes, each answering with the path of a file
as its evidence, and a list was made of everything the project said of itself that the code did not have.
That list became this page, and the rest was rewritten to claim only what was found. It is in
`doc/environment-inventory.md` if you want to check any of this against the code yourself.

This is the page that goes stale first. Each line above was checked against the file that decides it on the
date at the top.
