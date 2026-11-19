# Network and privacy

What the emulator contacts, and when. It has no account, asks for no name, and reports nothing of what you
do. Nearly everything it fetches is something you asked for by name. The rest is one list when it starts,
and what a card or a page you opened needs to show itself.

## What it contacts

| When | What it asks | Of whom |
|---|---|---|
| **When it starts** | the list of published plugins, so it can offer what you do not have | GitHub, the releases of oozx-plugins |
| **You search the game browser** | the search you typed, with its filters | [ZXInfo.dk](https://zxinfo.dk) |
| **A game's screens and pictures are shown** | each picture | ZXInfo, Spectrum Computing, World of Spectrum or the Internet Archive, wherever the catalogue says it is |
| **A game's card is open** | its smaller files, in the background, so they are there when you click | the same |
| **You load a game, or open a file from its card** | that file | the same, wherever the catalogue says it is |
| **You watch a recording** | that recording | the [RZX Archive](https://www.rzxarchive.co.uk/), or where ZXDB lists it |
| **A machine needs a ROM that does not ship** | that ROM, **after asking you** | whoever publishes it; the address is shown before you answer |
| **You install or update a plugin** | its jar | GitHub, the releases of oozx-plugins |
| **A page of the guide has a picture** | the picture | GitHub, where these pages live |

When it asks for plugins and ROMs it says who is asking: `oozx (ZX Spectrum emulator)`.

Every one of those servers sees what any server sees of a request: your address, and what was asked for.
What you search for in the game browser goes to ZXInfo as you typed it.

## What works with no network

- Everything already on your disk: your files, the games fetched before, your session.
- The catalogue of the 5,000 most voted games, with their files, releases, maps and recordings listed, and
  recognising a file as one of 4,922 games.
- Pokes, machines whose ROMs ship, every peripheral that needs no ROM, and these pages, without their
  pictures.

What does not: searching beyond those 5,000, fetching a game you do not have, and fetching a ROM or a
plugin.

## What it keeps

Everything is under `.oozx` in your home folder, and nothing is kept anywhere else.

| | |
|---|---|
| `config.json` | your settings |
| `session/` | the machines you left open, as snapshots |
| `downloads/` | what was fetched for you |
| `roms/` | ROMs fetched for you, or put there by you |
| `plugins/`, `plugin-cache/` | plugins |
| `library.json` | which game each of your files turned out to be |

Delete the folder and it starts as new.
