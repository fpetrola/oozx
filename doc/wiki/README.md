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

## Keeping them true

Every page says at the end where the thing it describes lives. When a page states a number, the number came
from the tree on the day it was written — counts of modules, tests, lines and games, and the measurements in
the diaries under `doc/`. `Limits-and-what-is-not-there.md` is the list of what the environment does *not*
do, and it is the page most likely to go stale first: it is the one to re-check against
`doc/environment-inventory.md` when something lands.
