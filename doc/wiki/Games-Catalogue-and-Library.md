# Games: the catalogue and your own library

![The game browser](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/game-browser.png)

*Searching ZXInfo from inside the emulator: what came back, each with its own loading screen, whether it
has a recording, and the filters down the left.*

## The catalogue, inside the emulator

`Emulator → Game Browser` (Ctrl+B) searches [ZXInfo.dk](https://zxinfo.dk) — the whole ZXDB, from the
emulator, with no browser and no files.

- **Search** on Enter or the button; an X next to the box clears it, results and all.
- **Filters**: machine and genre are asked of the server; RZX / Map / Loadable are applied here over what
  came back.
- **Rows** carry the title, year, publisher and the game's own screenshots.
- **Right-click**: *Load Game* — into a machine you pick, or the one the file says — *Load Version*,
  *View Details*, *Add to Favorites*, *Download*, *Play Recording*.

**Loading a title** fetches the archive, unzips it, and chooses the file to run by score: `.rzx` before a
snapshot, a snapshot before a tape, a tape before a `.csw`, a bonus for "48" in the name, a penalty for a
name that says "alternate", a heavy one for anything under `/denied/`. The machine comes from the TZX's
hardware block if it has one, else from the name, else it is a 48K. Titles the archive is not allowed to
serve turned out to be in TOSEC all along, and the ZXInfo entry already said in which archive and with which
MD5 — so those load too.

**Details** is nine tabs: General, Technical, Publishers, Authors, Description, Screenshots, Game map,
Releases, Downloads.

## Your own collection, identified

Point it at the files you already have and each one says **which game it is** — by what is inside it, not by
its name.

A file is cut into pieces **where its own bytes say to cut**, by a rolling hash, and each piece becomes one
64-bit number. Cutting by content rather than by position is the whole trick: a trainer, a different loader,
a TZX carrying the blocks a TAP carries bare, or a snapshot holding the same code at another address damage
only the pieces they touch, and the cuts fall back into step a few bytes later. One chunk in four is kept,
chosen by its own hash so both copies keep the same ones.

The catalogue that ships with the build is **4,922 games fingerprinted** — the top of ZXInfo's own ranking —
with title, year, publisher, screenshot and whether it has a map. So:

- `RENE256.SNA` is *Renegade* for everything that asks afterwards: its pokes, its details, its map.
- What could not be identified stays in the library **as unknown**, rather than being dropped: it is still a
  game somebody has.
- Identification is written down once and re-asked when the catalogue grows.
- A game with no screenshot is drawn the way every Spectrum game looked at that moment: as a loading screen,
  with the border a real Spectrum has, which is not the same at the sides as at the top.

The honest limit is documented where it happens: a 128 K snapshot can share a long stretch of memory with an
unrelated game, because whatever left that memory behind is not the game. The exact half of the question — an
MD5 against ZXInfo's catalogue — needs the network and answers nothing when the copy is not byte-identical.

## Pokes

3,683 games' worth of cheats ship with the emulator, in one file, keyed by ZXInfo id rather than matched by
name. Open the pokes window on a machine and it offers the ones for the game that is loaded: a checkbox per
modification, *Apply* and *Clear*, and un-applying puts the original bytes back. What you applied is
remembered per machine and comes back with the session.

![The pokes window](https://raw.githubusercontent.com/fpetrola/oozx/main/doc/wiki/img/pokes.png)

*Manic Miner's cheats, each saying what it writes and where. What you switch on moves to the "Applied"
section of the same window, and un-applying puts the original bytes back.*

## Favourites

Games and recordings, remembering which entry inside a zip it was, in a window of their own.

## Where this lives

`machine/zxinfo` (the API client, `GameFingerprint`, `GameLibrary`, `catalogue.json`), `machine/pokes`
(`pokes.json`), `machine/app/.../media` (`CatalogueBuilder`, `LocalGames`) and the browser windows in
`machine/app/.../desktop`.
