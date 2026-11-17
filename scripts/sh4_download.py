#!/usr/bin/env python3
"""SH4's download checks on real URLs (sharp-0.9.0.md, Testing 3), Python 3 stdlib only, a debug build.

`probe <out> <rendition png>`: for each real APOD original below, fakes a post stored by 0.8.0 (today's
date, so a page open skips the fetch and runs the check of 0.8.0's post; the rendition URL; no
`sizeChecked`; a stand-in file), opens the page, then reads the log's `Refresh:` lines and the stored
file's size. One JSON line per URL to <out>/download.jsonl.
"""
import json
import struct
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from sh4_run import P, adb, sh, kill, read_prefs, put, write_prefs, get  # noqa: E402

BASE = "https://assets.science.nasa.gov/dynamicimage/assets/science/cds/apod/apod/"
# name: (path under BASE, what the probe should do)
URLS = {
    "7nov_1500x1022": ("2026/november/GerlacheKocherMassif_Sans.jpg", "original"),
    "pct20_1600x1067": ("2026/october/Harvest%20Moon%20Trail%20With%20Venus%20Belt%20and%20Erupting%20Mount%20Etna%204%20LD.jpg", "original"),
    "tiff_jupiter": ("2026/january/jupiter2026.tif", "rendition"),
    "big_4000x4000": ("2025/december/AS17-149-22859-2v2SmlWmk.jpg", "rendition"),
}


def image_size(b):
    """The stored file's size from its header: PNG's IHDR or JPEG's first SOFn."""
    if b[:8] == b"\x89PNG\r\n\x1a\n":
        return struct.unpack(">II", b[16:24])
    if b[:2] == b"\xff\xd8":
        i = 2
        while i < len(b) - 9:
            if b[i] != 0xFF:
                i += 1
                continue
            m = b[i + 1]
            if 0xC0 <= m <= 0xCF and m not in (0xC4, 0xC8, 0xCC):
                h, w = struct.unpack(">HH", b[i + 5:i + 9])
                return w, h
            i += 2 + struct.unpack(">H", b[i + 2:i + 4])[0]
    return None


def stored():
    return image_size(adb("exec-out", f"run-as {P} cat files/apod.jpg", binary=True, check=False))


def probe(out, standin):
    today = time.strftime("%Y-%m-%d")
    for name, (path, want) in URLS.items():
        url = BASE + path
        sh("input keyevent KEYCODE_HOME")
        kill()
        root = read_prefs()
        for kind, k, v in [("string", "date", today), ("string", "imageUrl", url), ("string", "pageUrl", url),
                           ("string", "title", f"SH4 probe {name}"), ("boolean", "isVideo", False),
                           ("string", "sizeChecked", None), ("string", "where", "BOTH"), ("boolean", "daily", True)]:
            put(root, kind, k, v)
        for k in ("wallpaperDate", "wallpaperUrl", "lock.wallpaperDate", "lock.wallpaperUrl"):
            put(root, "string", k, today if "Date" in k else url)
        write_prefs(root)
        with open(standin, "rb") as f:
            subprocess.run(["adb", "shell", f"run-as {P} sh -c 'cat > files/apod.jpg'"], stdin=f, check=True)
        before = stored()
        adb("logcat", "-c")
        sh(f"monkey -p {P} -c android.intent.category.LAUNCHER 1", check=False)
        log = ""
        for _ in range(40):
            time.sleep(1)
            log = adb("logcat", "-d", "-s", "APODroid", check=False)
            if "Refresh: APOD" in log and ("replaced" in log or "rendition" in log or "failed" in log):
                time.sleep(3)
                log = adb("logcat", "-d", "-s", "APODroid", check=False)
                break
        lines = [l.split("APODroid: ", 1)[1] for l in log.splitlines() if "APODroid: " in l and ("Refresh" in l or "Subject" in l or "probe" in l.lower())]
        after = stored()
        checked = get(read_prefs(), "sizeChecked")
        crash = adb("logcat", "-d", "-b", "crash", check=False).strip()
        if want == "original":
            ok = after is not None and after != before and any("original" in l and "smaller" in l for l in lines) and any("replaced" in l for l in lines)
        else:
            ok = after == before and any("rendition" in l or "probe failed" in l or "no size" in l for l in lines)
        ok = ok and checked == url and not crash
        res = {"case": name, "url": url, "want": want, "before": before, "after": after, "log": lines,
               "sizeChecked": checked == url, "crash": crash[:300], "verdict": "PASS" if ok else "FAIL"}
        with open(Path(out) / "download.jsonl", "a") as f:
            f.write(json.dumps(res) + "\n")
        print(f"{res['verdict']:5} {name}: {before} -> {after}; {lines}", flush=True)


if __name__ == "__main__":
    if sys.argv[1] == "probe":
        probe(sys.argv[2], sys.argv[3])
