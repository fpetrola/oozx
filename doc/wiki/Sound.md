# Sound

## What makes it

| | |
|---|---|
| **Beeper** | The one bit of port `0xFE`, and the tape's ear with it |
| **AY-3-8912** | The 128, +2 and +3 chip, with a timestamped write queue so a write lands at the T-state it happened |
| **Melodik** | An AY for a machine that has none: a 48K with 128K music |
| **Fuller Box** | Its own AY, on `0x3F` and `0x5F` |
| **Covox** | A DAC on a port, with a level meter and a volume knob on its window |
| **SpecDrum** | Cheetah's drum machine, the same way |

Everything that makes a sound is an `AudioSource` that mixes into the same buffer; a device that wants to be
heard implements it and is heard. Synthesis is band-limited (`BlipBuffer`/`BlipSynth`), 44,100 Hz, with
equalisation presets.

The AY here is **mono**: its three channels are mixed equally into both ears. The mixer is stereo — two
sources can sound different in each ear — but the chip does not pan its own channels. The code says so where
it happens.

## Sound sets the pace

This is the part worth knowing. At normal speed, the emulator is not paced by a timer: **the sound card
is the clock**. A frame is emitted when there is room for it, so the machine runs exactly as fast as the
audio line consumes it, and the picture follows. No stutter, no drift, no sleeping on a millisecond timer
that the operating system rounds.

It also turned out to be the fastest arrangement measured: making the sound the pacer was step 9 of the
performance diary, and it stayed.

Above 100 %, the sound is dropped rather than stretched (`dropWhenAhead`), so turbo does not turn into a
siren. `Ctrl+M` mutes; right-click the mute button for a volume slider. At very low speeds the beeper's
one-second buffer is the limit, which is a thing that was found by hanging the emulator at 0 % and fixing it.

## Where this lives

`machine/core/.../modules/sound` (`Sound`, `AudioSource`, `AudioOutput`, `blip/*`),
`machine/machines/ay` (the chip), `machine/host/sound` (`JavaSoundDevice`, `Dac`, the line itself, and the
`SilentSoundDevice` every test runs on so a crash inside the platform's audio server cannot take the build
down). The devices are in `devices/{melodik,fuller,covox,specdrum}`.
