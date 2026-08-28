#!/usr/bin/env python3
"""Build a JSON catalogue of the RZX Archive.

The site has no API and is no longer updated, so the whole thing is fetched once from its
27 browse pages and written out as data. Each entry carries the Spectrum Computing id, which
is the same id the ZXInfo API uses — that is what makes the catalogue joinable with the game
metadata the emulator already fetches.
"""

import html
import json
import os
import re
import sys
import time
import urllib.request

BASE = "https://www.rzxarchive.co.uk"
PAGES = ["0"] + [chr(c) for c in range(ord("a"), ord("z") + 1)]
AGENT = "oozx-rzx-catalog/1.0 (personal emulator project; one-off archive fetch)"
DELAY = 1.0

ROW = re.compile(r'<a NAME="([^"]+)"></a>(.*?)(?=<a NAME="|</table>)', re.S | re.I)
CELL = re.compile(r"<td[^>]*>(.*?)(?=<td|</tr>)", re.S | re.I)
DOWNLOAD = re.compile(r'<A HREF="([^"]+)">Download</A>\s*<font size=1>\((\d+)KB\)', re.I)
SPECCOMP = re.compile(r'spectrumcomputing\.co\.uk/index\.php\?cat=\d+&id=(\d+)', re.I)
WOS = re.compile(r'href="(https://worldofspectrum\.org/[^"]+)"', re.I)
VIDEO = re.compile(r'href="(videos/[^"]+)"', re.I)


def text_of(fragment):
    """Strip tags and collapse whitespace, keeping the words."""
    fragment = re.sub(r"<br\s*/?>", " — ", fragment, flags=re.I)
    fragment = re.sub(r"<[^>]*>", "", fragment)
    return re.sub(r"\s+", " ", html.unescape(fragment)).strip(" — \t")


def fetch(page, cache_dir):
    path = os.path.join(cache_dir, page + ".html")
    if os.path.exists(path):
        return open(path, encoding="utf-8", errors="replace").read()

    request = urllib.request.Request(BASE + "/" + page + ".php", headers={"User-Agent": AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        body = response.read().decode("utf-8", errors="replace")
    open(path, "w", encoding="utf-8").write(body)
    time.sleep(DELAY)          # one page a second; the whole archive is 27 requests
    return body


def parse(page, body):
    entries = []
    for anchor, rest in ROW.findall(body):
        # The anchor sits inside the first cell, so the title is whatever follows it up to
        # the next <td>; the cells found after that are the submitter and the links.
        title_cell, _, tail = rest.partition("<td")
        cells = CELL.findall("<td" + tail) if tail else []

        title = text_of(title_cell)
        note = None
        if " — " in title:
            title, _, note = title.partition(" — ")
            title, note = title.strip(), note.strip()

        submitter = text_of(cells[0]) if cells else None
        links_cell = cells[1] if len(cells) > 1 else ""

        downloads = [{"url": BASE + url if url.startswith("/") else BASE + "/" + url,
                      "sizeKb": int(kb)}
                     for url, kb in DOWNLOAD.findall(links_cell)]

        spec_comp = SPECCOMP.search(links_cell)
        wos = WOS.search(links_cell)
        video = VIDEO.search(links_cell)

        entries.append({
            "id": anchor,
            "title": title,
            "note": note,
            "submitter": submitter or None,
            "page": page,
            "sourceUrl": "%s/%s.php#%s" % (BASE, page, anchor),
            # Same id space as the ZXInfo API: this is the join key.
            "spectrumComputingId": int(spec_comp.group(1)) if spec_comp else None,
            "worldOfSpectrumUrl": wos.group(1) if wos else None,
            "videoUrl": BASE + "/" + video.group(1) if video else None,
            "downloads": downloads,
            "distributionDenied": "Distribution Denied" in links_cell,
        })
    return entries


def main():
    cache_dir = sys.argv[1] if len(sys.argv) > 1 else "rzx-cache"
    out = sys.argv[2] if len(sys.argv) > 2 else "rzx-archive.json"
    os.makedirs(cache_dir, exist_ok=True)

    entries = []
    for page in PAGES:
        body = fetch(page, cache_dir)
        found = parse(page, body)
        entries.extend(found)
        print("  %-2s %4d entries" % (page, len(found)), file=sys.stderr)

    entries.sort(key=lambda e: (e["title"].lower(), e["id"]))
    # Empty fields are dropped rather than written as nulls: most entries have no note and no
    # video, and the archive is big enough that carrying them costs a megabyte for nothing.
    entries = [{k: v for k, v in e.items() if v not in (None, [], False)} for e in entries]
    json.dump({"source": BASE, "count": len(entries), "entries": entries},
              open(out, "w", encoding="utf-8"), ensure_ascii=False,
              separators=(",", ":"))

    joinable = sum(1 for e in entries if e.get("spectrumComputingId"))
    downloadable = sum(1 for e in entries if e.get("downloads"))
    print("\n%d entries, %d with a Spectrum Computing id, %d downloadable -> %s"
          % (len(entries), joinable, downloadable, out), file=sys.stderr)


if __name__ == "__main__":
    main()
