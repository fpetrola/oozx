# Peripherals

Every peripheral, with the machines it fits and the ROM it needs. What each one was and what it does here
is on [The catalogue of peripherals](The-catalogue-of-peripherals).

All of them are under `Emulator → Equipment`, and clipping a peripheral's window onto a machine is what
plugs it in.

## Which machine, which ROM

A peripheral says which machines it fits, and it will not plug into another. Three rules cover most of them:

- **48K-like**: a machine without the 128's paging, such as the 16K, the 48K and its clones.
- **Not on the Amstrad ones**: any machine except those that page through `0x1FFD` (the +2A, the +3, the +3e,
  the Scorpion) and those that decode their ports fully (the Pentagons).
- **Any**: every machine.

Some peripherals had a ROM of their own. Where that ROM can be shipped, it is. Where it cannot, you supply
it: either from a button on the peripheral's window, or by putting the file in `~/.oozx/roms/` under the name
given here. A window that is missing its ROM says `no ROM` and which file it wants.

| Peripheral | Fits | Its ROM |
|---|---|---|
| **ZX Printer** | 48K-like | none |
| **Parallel printer** | the Amstrad ones, and machines with the 128's sound chip | none |
| **Kempston Mouse** | any | none |
| **Joystick** | any | none |
| **Currah µSpeech** | 48K-like | two of 2K, yours, chosen on its window |
| **Currah µSource** | not on the Amstrad ones | 8K, yours, chosen on its window |
| **Fuller Box** | 48K-like | none |
| **SpecDrum** | not on the Amstrad ones | none |
| **Covox** | the Pentagons | none |
| **Melodik** | machines with no sound chip of their own | none. Fitted under `Emulator → Settings... → Audio`, not from a window |
| **Interface 1** | not on the Amstrad ones | yours: `if1-2.rom` |
| **Interface 2** | not on the Amstrad ones | none; the cartridge is the ROM |
| **Beta 128** | not on the Amstrad ones. Built into the Pentagon | yours: `trdos.rom` |
| **+D** | not on the Amstrad ones | ships |
| **DISCiPLE** | 48K-like | ships |
| **Opus Discovery** | not on the Amstrad ones | yours: `opus.rom` |
| **Didaktik 40 / 80** | 48K-like | yours: `didaktik80.rom` |
| **DivIDE** | not on the Amstrad ones | an 8K EPROM, yours, chosen on its window |
| **DivMMC** | not on the Amstrad ones | an 8K EPROM, yours, chosen on its window |
| **ZXMMC**, **Simple 8-bit IDE** | any | none |
| **ZXATASP**, **ZXCF** | not on the Amstrad ones | none |
| **Multiface One** | 48K-like | 8K, yours, chosen on its window |
| **Multiface 128** | not on the Amstrad ones | 8K, yours, chosen on its window |
| **Multiface 3** | the Amstrad ones | 8K, yours, chosen on its window |
| **ULAplus** | any. Built into the Chloe machines | none |
| **Spec256** | any; its games are 48K snapshots | none → [256 colours](256-colours) |

## The fronts

A peripheral's window is the front of the thing, built from a few shapes they share:

- **A drive bay** for each drive: a slot with a motor lamp, *Insert*, *Eject*, *New*, *Save*, *Flip* and
  *Write-protect*, a lamp for the paged ROM, the peripheral's own button, and an unfolded view with the track
  under each head.
- **A hard disk or card bay** for each disk, where *Save* is what commits.
- **A sound front** with a level meter and a volume knob.
- **A bar** that stays when the window is folded, and the rest under it.

## Their settings

`Emulator → Settings...` has what does not belong on a front, a tab at a time:

| Tab | What is there |
|---|---|
| **Audio** | the volume of the Covox, the SpecDrum and the µSpeech; whether the Melodik is fitted |
| **Storage** | the tape's ear; the +3's drives; the Beta 128's *Boot on 48K* and *Auto boot*; write protection and disks of the DivIDE, the DivMMC, the ZXATASP and the ZXCF |
| **Peripherals** | the Interface 1's Microdrive length, handshake and network; the Multiface's *Stealth* |
| **Devices** | the disks of the Simple 8-bit IDE and the ZXMMC |

Clipped onto a machine, the settings are that machine's. Left free, they are the defaults.
→ [The desk](The-desk)

## What a peripheral is

A peripheral answers ports on a bus that merges answers the way the real one does: every line that somebody
pulls low is low, a line nobody drives floats, and what is plugged in answers before the machine's own
chips. It may also watch the program counter, which is how a ROM pages itself in when the processor reaches
an address. Whatever its window does reaches the machine on the emulator's own thread.

Writing one is [Build a peripheral](Build-a-peripheral).
