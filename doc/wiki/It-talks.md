# It talks: the µSpeech

Plug it in. It talks. Clip the Currah µSpeech onto a 48K and the Spectrum says every key you press, or
whatever you write for it, in English or in Spanish. It needs its two ROMs, which you supply.

<video src="https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/clips/speech.mp4" controls width="100%"></video>

*Plug it in. It talks.* A 48K comes up saying "Speech System © CURRAH 1983". Keys are typed and each one is said; a sentence in Spanish is written in *Say*, and the Spectrum says it. With sound.

The µSpeech was a small box [Currah sold in 1983](https://spectrumcomputing.co.uk/entry/1000081/Hardware/Currah_MicroSpeech):
a speech chip, and a ROM that made the Spectrum say its keys.

## Using it

1. `Emulator → Equipment → Currah µSpeech`, and clip its window onto a 48K. The machine restarts with the
   µSpeech's message at the top of the screen.
2. **Type.** The Spectrum says each key as you press it. *Speak keys* on the window turns that off and on:
   it types the µSpeech's own setting for it, `LET keys=0` or `LET keys=1`.
3. **Make it speak from BASIC.** `LET s$="hel(oo)"` says hello. The letters are sounds, brackets mark the
   sounds that take more than one letter, and capitals raise the pitch.
4. **Or just write.** The *Say* line saves you learning that spelling. Write in English or in Spanish, press
   Enter, and the window types the `LET s$=...` for you, already spelt by its sounds. "Hello world!" goes in
   as `hel(oo) w(er)ld.` and "calle" as `ka(sh)e`.
5. **Or pick a sound.** *Allophone* has the chip's 64 sounds, one by one, at normal or high pitch.

In Spanish it says *calle* the way I do: the *elle* is the one of the Río de la Plata.

## Setting it up

- **Two ROMs, which you supply.** It needs the µSpeech's own ROM and the speech chip's, 2K each. They are not
  mine to give away, so the *ROMs...* button on its window takes them from your disk. Without them the window
  says `no ROM` and which ones are missing.
- **Its volume and its meter** are on its window, as on everything else that makes a sound.
- **Its state, in one line**: whether its ROM is in or out, the last sound it said, and the pitch.

## How it makes the sound

1. A program writes the number of a sound to address `0x1000`. Bit 0 of the same address says whether the chip
   still has one waiting.
2. The chip is an SP0256-AL2: a small sequencer that reads its instructions a few bits at a time from its own
   ROM, and a twelve-pole filter those instructions load. The filter gives out a sample every few hundred
   T-states. Its clock is the machine's.
3. The pitch is the crystal: writing to `0x3000` or `0x3001` changes it.
4. The 2K ROM flips in and out on every access to `0x0038`. That is how it takes the interrupt of every frame
   and hands it on to the Spectrum's own ROM.
5. The samples go into the mix as one more source.

Two things stand behind it. The chip is compared with the one in Fuse: the same script goes through both, and
they have to agree sample for sample. And the spelling has its own tests, word by word.

English is spelt with the letter-to-sound rules of the Naval Research Laboratory. Spanish has a few rules of
its own: the *elle* of the Río de la Plata, the *c* of *cielo* as an *s*, a silent *h*.

That is the last page of this tour. The next one goes [under the hood](Why-it-all-fits).
