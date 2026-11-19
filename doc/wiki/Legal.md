# Legal

What is mine, what is somebody else's, and on whose terms each thing is here. The full text is in `NOTICE`
and `LICENSE`, in each repository, and in `README.copyright` beside the ROMs.

## The emulator

OOZX is free software: Copyright © 2023-2026 Fernando Damian Petrola, under the GNU General Public License,
version 3 or later. That covers the emulator and its plugins.

## Games

This project distributes no games. A game is fetched when you ask for it, from the public archives the
catalogue points to. The translator's Java sources are not published, because each one carries the game it
came from.

## ROMs

**Those that ship**, each on its owner's terms, which are not the same terms:

| ROMs | Whose, and how |
|---|---|
| the 48K, the 128, the +2, the +3, the TC2048 | © Amstrad, who allow them to be distributed and keep the copyright. They may not be sold, nor put into hardware |
| the TC2068, the TS2068 | derived from Amstrad's, so the same; Timex's own changes are in the public domain |
| the +3e | derived from Amstrad's; the changes are © Garry Lancaster |
| the Spectrum SE | © Nine Tiles Networks, with changes by Andrew Owen, under the GNU GPL version 2 or later |
| the DISCiPLE, the +D | © Datel Microelectronics, who allow them to be distributed |

**Those that do not ship** are not here in any form. For a machine's ROM that somebody publishes, the
emulator knows where and what its bytes must be, asks you, and fetches it from them. For a peripheral's ROM
that it knows no source for, you supply your own. → [Peripherals](Peripherals)

## The catalogues

| | |
|---|---|
| **ZXDB** | the part that ships is a database derived from [ZXDB](https://github.com/zxdb/ZXDB), open under the same terms as ZXDB itself, the Open Database License 1.0 |
| **ZXInfo** | [ZXInfo.dk](https://zxinfo.dk) is asked when you search; nothing of it is copied here |
| **The RZX Archive** | its catalogue ships, 4,291 entries: titles, notes, who submitted each one. It is © the [RZX Archive](https://www.rzxarchive.co.uk/) and the people who made the recordings. It is the catalogue and not the recordings: no recording is here, and the entries the archive marks as not to be distributed are recorded as such |

## The pokes

3,683 listings ship, for 3,612 published games, so that pokes work with nothing to download. They are the work of
the people who found them, over decades. No file says who wrote it and none carries a licence, and this
project is not their author. They were taken from the
[AllTipshopPokes](https://github.com/ladyeklipse/all-tipshop-pokes) collection made by Lady Eklipse, which
gathered them from [the Tipshop](http://www.the-tipshop.co.uk/), run by Gerard Sweeney, where they were
published.

## Other people's work inside

| | |
|---|---|
| **xBRZ** | the scaler. Zenju's algorithm in Stanio's Java port, GNU GPL version 3, used unmodified |
| **The Spec256 palette** | © Jiri Svoboda, from [GZX](https://github.com/jxsvoboda/gzx), under its MIT-style licence. One entry was changed: colour 255 is white |
| **The Z80 test vectors** | © Philip Kendall and contributors, GNU GPL version 2 or later, taken unmodified from Fuse. They reach the tests and nothing else |
| **The buttons' pictures** | fifteen icons from [OpenMoji](https://openmoji.org/), CC BY-SA 4.0 |
| **The ZX Spectrum logotype** | Sinclair Research's, as Wikimedia Commons carries it. That is about copyright and not about trademark |

The names ZX Spectrum, Sinclair, Amstrad, Timex and the rest belong to whoever owns them. This project is
not theirs and is not endorsed by them.

## Thanks, by name

The [Fuse](https://fuse-emulator.sourceforge.net/) authors, for the test vectors, the timing model and the
core that settles my arguments. Cristian Dinu, for the
[decoding of the Z80 opcode byte](http://www.z80.info/decoding.htm). Peter Jakubčo's
[emuStudio](https://www.emustudio.net/), for its Z80 test suite. [ZXInfo.dk](https://zxinfo.dk/),
[ZXDB](https://github.com/zxdb/ZXDB) and the [RZX Archive](https://www.rzxarchive.co.uk/), for the
catalogues. The authors of GZX and ZX-Poly, whose code I read as the specification of Spec256.
