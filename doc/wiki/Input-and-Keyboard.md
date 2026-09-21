# Input and Keyboard

## The keyboard, as the machine sees it

Five concepts, one class each, and the machine's half knows nothing about PCs:

| | |
|---|---|
| `SpectrumKey` | the forty keys, each with its half-row, its bit and its label |
| `KeyMatrix` | the eight half-rows and what a port reads; the 256-entry lookup table is its own and private |
| `Combination` | what one press puts down, and how to apply it to the matrix |
| `KeyLayout` | the interface: what a key of whoever is typing produces |
| `PcLayout` | a PC keyboard |
| `RecreatedLayout` | the **Recreated ZX**, which reads what came before and releases by pressing |

The machine reads the matrix on port `0xFE` with a single array index. Issue 2 and issue 3 behaviour is a
setting on the machine.

Keys go to **the machine in front, or to the machine the front window is clipped to** — so typing into a
cassette deck reaches the Spectrum it belongs to. While a text field, a combo or a modal dialog has the
focus, no machine hears anything.

## The on-screen 48K

![The keyboard window](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/keyboard-only.png)

*The window itself, with CAPS SHIFT, M, SPACE and ENTER held down on the machine: each one is sunk into the
photograph while the matrix has it.*

`Emulator → Keyboard` opens a photograph of a 48K with **every key that is down on the machine shown pressed
into the picture** — the matrix the machine itself reads, not the keys your PC sent.

It is the place to look when the typing does not arrive. A key lit here and ignored by the game is the
game's business; a key that never lights is your key not getting that far. It is also the whole keyboard a
Spectrum has, for whoever is sitting at a PC: which key of the machine a shifted key of yours turns into.

## Joysticks

A joystick is **a kind, not a flag**: the Spectrum had no port of its own, so every maker solved it
differently — some added a port, some wired the stick to keys of the machine's own keyboard.

| | |
|---|---|
| **Kempston** | its own port |
| **Sinclair 1 / 2** | keys of the machine |
| **Cursor** | keys of the machine |
| **Timex 1 / 2** | keys of the machine |
| **Fuller** | its own port, on the Fuller Box |

A **physical gamepad** (through Jamepad/SDL) is the Kempston of the machine in front. The Joystick window
shows direction and fire as the Kempston port reads them, and names the pad doing it.

One detail that took a bug to find: a joystick kind says *whether it took the push*, so a direction that
went to the stick does not also go to the keyboard — and the Kempston only takes the space bar while the
game is actually reading its port.

## Mouse

The Kempston Mouse is a device with a window of its own: your desk mouse moves over the picture, *Hold*
captures the pointer, and there are sensitivity, swap and record controls.

## Where this lives

`machine/core/.../modules/keyboard` (the machine's half), `machine/host/input` (the host's half: `Input`,
`PcLayout`, `RecreatedLayout`, the joysticks), `machine/app/.../SwingKeyboard` (AWT codes and nothing else),
`machine/devices/joystick`, `machine/devices/mouse`. The design notes are in `machine/doc/teclado.md`.
