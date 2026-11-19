# Your files: drop anything

Any name. The right game. Drop a file on the desk and it answers with the machine that game needs and
everything there is about it, because it looked at what is inside the file and not at what it is called.

> **Clip** · *Any name. The right game.* A file called `cinta3.tzx` is dragged out of a folder and let go of
> over the desk. A machine starts loading it, and beside it a card fills in: ThunderCats, its screens, its
> maps, its pokes. (`any-name`, filmed)

## The gesture

Drag a file from your file manager and let go of it over the desk. A tape, a snapshot, a zip with one of
them inside, or a recording.

Two things happen at once:

- **A machine starts**, the one that game was made for, with the game going into it.
- **A card opens** at the edge of the desk. It is there at once, saying it is looking, and fills in as the
  answers arrive: first from the catalogue that ships, then from ZXInfo.

`File → Open` takes the same files and recognises them the same way, without opening the card. The card of
whatever a machine is running is one button away, on that machine's bar: *View Game Details*.

## The card

> **Picture** · the card of a recognised game, whole: the heading with the file's name and the game's, the
> turning screens, and the wall of tiles. (to take)

The heading says the two things that matter, one above the other: what your file was called, and the game it
turned out to be, "recognised by what is inside it". Then the publisher, the year, the machine, the authors,
its score and its genre, and the game's own screens turning one after another, pixel for pixel.

Below the heading is a wall of tiles, one for each kind of thing there is about that game, with how many
there are and the first of them as its picture:

| Tile | What is inside |
|---|---|
| **About** | what the catalogue says of it: releases, publishers, authors, genre, machine, score |
| **Play it** | every file the catalogue has for this game, each with a *Load* button that starts it in a machine |
| **Pokes** | its pokes, chosen and applied to the game that is running → [Pokes, and finding yours](Pokes-and-finding-yours) |
| **Recordings** | other people's recordings of it, each with a *Watch* button → [Recordings and Take Over](Recordings-and-Take-Over) |
| **Editions** | who published it again, where and when |
| **Screens, Inlays, Maps, Adverts** | galleries: click a picture and it opens in a window where you can zoom with the wheel and drag to look around |
| **Music** | an `.ay` tune opens a player with the list of its songs: it is played by a 128K running the tune's own code |
| **Instructions** and the rest | text opens in a window of the desk; what the desk cannot show opens with your computer's own program |

A tile is a plugin's to bring. The card does not know what a poke is: the pokes plugin brings that tile, its
count and its page, and taking the plugin out takes the tile with it.

What the card fetches it keeps, under `~/.oozx/downloads`, so nothing is fetched twice. Small files are
brought in the background as soon as the card opens, so most of what you click is already there.

## What happened when you dropped it

1. **It took the file's fingerprint.** The file is cut into pieces where its own bytes say to cut, and each
   piece becomes one number. Cutting by content rather than by position is the whole trick: a trainer, a
   different loader, a TZX carrying the blocks a TAP carries bare, or a snapshot holding the same code at
   another address damage only the pieces they touch.
2. **It asked the catalogue that ships** whose fingerprint that is. Of its 5,000 games, the most voted
   ones, 4,922 have a fingerprint.
3. **It chose the machine** from what the catalogue says that game was made for.
4. **It opened the card** from what ships, and asked ZXInfo for the rest.

What ships is not small. The catalogue in the jar knows 5,000 games and 56,892 files related to them, of 49
kinds: eleven a game, on average. Of Jet Set Willy alone it lists 38 files of 15 kinds, among them nine maps,
five sets of instructions and three recordings, besides its pokes and its four editions. All that, for
something that was called `cinta3.tzx`.

## When it does not know

- **A file it cannot name** is still loaded. Its card says "I do not know this one" and asks which game it
  is: type the name and it looks that up instead.
- **A 128K snapshot can fool it.** Two snapshots of unrelated games were measured sharing 16,257 identical
  bytes in a row, which is enough for one to be taken for the other. Whatever left that memory behind is not
  the game.
- **It knows the games whose fingerprint ships**, not every game there is.

## Your whole collection

The game browser does the same for a folder. *Add folder...* at the foot of its left side reads your files,
and each one says which game it is: `RENE256.SNA` is Renegade, with its pokes and its card. What could not be
named stays in your library as unknown rather than being dropped, and *Only unknown* shows just those.

Next: [Pokes, and finding yours](Pokes-and-finding-yours).
