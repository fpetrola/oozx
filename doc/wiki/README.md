# The wiki pages

The GitHub wiki of this project, kept here so it is versioned with the code it describes.

Every `.md` file in this directory is one wiki page, named as the page is named
(`The-Generated-Core.md` → *The Generated Core*). `_Sidebar.md` and `_Footer.md` are the navigation GitHub
renders around every page. This file is not a page; it is the note about the others.

## Publishing

`.github/workflows/wiki-pages.yml` copia estas páginas a la wiki de GitHub
(`oozx.wiki.git`, que es otro repositorio) en cada push a `main` que toque `doc/wiki/`. `README.md` queda
fuera: es esta nota, no una página. Necesita un secret `WIKI_TOKEN` — un PAT con permiso de escritura sobre
el repositorio, porque el `GITHUB_TOKEN` del workflow no alcanza para la wiki.

A mano, si hiciera falta:

```bash
git clone https://github.com/fpetrola/oozx.wiki.git /tmp/oozx.wiki
cp doc/wiki/*.md /tmp/oozx.wiki/          # README.md excluded, it is this note
rm /tmp/oozx.wiki/README.md
cd /tmp/oozx.wiki && git add -A && git commit -m "Wiki" && git push
```

## Publicándola como sitio (docsify)

`index.html` sirve este mismo directorio con [docsify](https://docsify.js.org): no hay build ni copia, lee
los `.md` tal cual, usa `Home.md` de portada y `_Sidebar.md` de navegación. Para verla en local:

```bash
python3 -m http.server -d doc/wiki 3000   # http://localhost:3000
```

`.github/workflows/wiki-pages.yml` la publica en GitHub Pages en cada push a `main` que toque `doc/wiki/`.
Hay que habilitarlo una vez en *Settings → Pages → Source: GitHub Actions*.

## The pictures

The pages point at images by raw URL on the `main` branch, so nothing is duplicated into the wiki
repository:

```
https://raw.githubusercontent.com/fpetrola/oozx/main/doc/...
```

They therefore appear once `main` carries them. `img/` holds the pictures made for the wiki:

| | |
|---|---|
| `manic-miner.png`, `jsw-sinclair*.png`, `jsw-spec256*.png` | Spectrum screens, produced headlessly: build a machine, load a snapshot, run 120 frames, write `picture.pixels` to a PNG at 3× |
| `desktop-overview.png`, `game-browser.png`, `pokes.png`, `game-details.png` | frames cut out of `doc/zxenv1.gif` |

The animations under `doc/` (`zxenv1.gif`, `zxenv2.gif`, `jsw1.gif`, `dan-95%.gif`, `wally-90%.gif`) were
already in the repository.

## How they are laid out

`Home.md` is the front page. `_Sidebar.md` is the order of everything else: three tours that are read from
top to bottom (*Play*, *The desk*, *Under the hood*) and a layer of pages to look things up in. A tour page
starts with what the reader can do; where a thing lives in the code is on `Building-from-source.md`.

One word for each thing: a *peripheral* is what plugs in (*device* is the code's word), *clip* is the gesture
and *plugged in* the result, *the desk* is the place, *a machine* is a Spectrum that is open, a *recording*
is a recorded game (RZX is its format), *pokes* are pokes, the *processor* is the Z80 and a *core* is one
implementation of it.

## Where a clip or a picture is still to come

A quoted line that begins `> **Clip**` or `> **Picture**` marks the place of one that is not in the page yet,
and says what it shows. A clip is named by the script that films it, under `doc/clips/`, and says whether it
has been filmed. `grep -n '^> \*\*\(Clip\|Picture\)' *.md` lists them all.

## Keeping them true

When a page states a number, the number came from the tree on the day it was written, and the page says the
day where the number is of the kind that moves. `Limits.md` is the list of what this does *not* do, and it is
the page most likely to go stale first: it is the one to check again when something lands. The same pages
ride inside the jar as the guide, which opens on `Your-first-five-minutes.md`.
