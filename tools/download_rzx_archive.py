#!/usr/bin/env python3
"""Fetch every recording the RZX catalogue lists, into one directory.

Resumable: a file already on disk is skipped, so an interrupted run continues where it left
off and a finished run costs nothing to repeat. Zipped recordings are unpacked, since what the
player wants is the .rzx inside and not the wrapper.

Deliberately unhurried. This is one small volunteer archive and four thousand requests, so
there is a pause between them and no concurrency at all.

  usage: download_rzx_archive.py <catalogue.json> <target dir> [seconds between requests]
"""

import io
import json
import os
import sys
import time
import urllib.error
import urllib.request
import zipfile

AGENT = "oozx-rzx-catalog/1.0 (personal emulator project; archiving for local playback)"
RETRIES = 3


def fetch(url):
    last = None
    for attempt in range(RETRIES):
        try:
            request = urllib.request.Request(url, headers={"User-Agent": AGENT})
            with urllib.request.urlopen(request, timeout=60) as response:
                return response.read()
        except (urllib.error.URLError, OSError) as e:
            last = e
            time.sleep(2 * (attempt + 1))
    raise last


def save(target, name, body):
    """Writes the recording, unwrapping a zip to the .rzx it holds."""
    if not body[:2] == b"PK":
        path = os.path.join(target, name + ".rzx")
        open(path, "wb").write(body)
        return [os.path.basename(path)]

    written = []
    with zipfile.ZipFile(io.BytesIO(body)) as archive:
        members = [m for m in archive.namelist() if m.lower().endswith(".rzx")]
        if not members:
            # Nothing recognisable inside; keep the zip rather than throw the download away.
            path = os.path.join(target, name + ".zip")
            open(path, "wb").write(body)
            return [os.path.basename(path)]

        for index, member in enumerate(members):
            suffix = "" if index == 0 else "-%d" % (index + 1)
            path = os.path.join(target, name + suffix + ".rzx")
            open(path, "wb").write(archive.read(member))
            written.append(os.path.basename(path))
    return written


def main():
    catalogue, target = sys.argv[1], sys.argv[2]
    delay = float(sys.argv[3]) if len(sys.argv) > 3 else 0.5
    os.makedirs(target, exist_ok=True)

    entries = [e for e in json.load(open(catalogue, encoding="utf-8"))["entries"]
               if e.get("downloads")]
    index_path = os.path.join(target, "index.json")
    index = json.load(open(index_path, encoding="utf-8")) if os.path.exists(index_path) else {}

    failures, fetched, skipped = [], 0, 0
    for n, entry in enumerate(entries, 1):
        name = entry["id"]
        if name in index and all(os.path.exists(os.path.join(target, f)) for f in index[name]["files"]):
            skipped += 1
            continue

        url = entry["downloads"][0]["url"]
        try:
            files = save(target, name, fetch(url))
            index[name] = {"title": entry["title"], "files": files, "url": url,
                           "spectrumComputingId": entry.get("spectrumComputingId"),
                           "submitter": entry.get("submitter")}
            fetched += 1
        except Exception as e:
            failures.append({"id": name, "url": url, "error": str(e)})
            print("  FAILED %-40s %s" % (name, e), file=sys.stderr, flush=True)

        if fetched % 50 == 0 and fetched:
            json.dump(index, open(index_path, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
            print("  %4d/%d fetched, %d skipped, %d failed"
                  % (n, len(entries), skipped, len(failures)), file=sys.stderr, flush=True)
        time.sleep(delay)

    json.dump(index, open(index_path, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
    if failures:
        json.dump(failures, open(os.path.join(target, "failures.json"), "w"), indent=1)

    print("\ndone: %d fetched, %d already there, %d failed, %d recordings on disk"
          % (fetched, skipped, len(failures), sum(len(v["files"]) for v in index.values())),
          file=sys.stderr)


if __name__ == "__main__":
    main()
