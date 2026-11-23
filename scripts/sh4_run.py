#!/usr/bin/env python3
"""SH4's device driver (sharp-0.9.0.md, Testing 2): fakes APOD's post with one of sh4_pictures.py's
pictures, sets it by the job (`run -f 1`, the record forgotten) or by Set (the page's button),
pulls `files/debug-set.png` and checks it with sh4_check.py. Python 3 stdlib only; a debug build
(run-as). Each case appends one JSON line to <out>/results.jsonl and keeps its PNG as
<out>/png/<case>.png.

Usage: sh4_run.py <pictures dir> <out dir> <case spec> ...
A case spec: picture:size:sharp:whole:where:path[:text[:pos[:pill]]]
  size CENTRE|AUTO, sharp 0|1, whole 0|1, where BOTH|HOME|LOCK, path job|turn|set (job: the
  record forgotten; turn: a change time passed since the record's stamp; set: the page's Set),
  text gal|up (default gal: hasUp false), pos a dragged part's position (- none),
  pill 0|1 (- none: ⛶'s Sideways pill not noted).
"""
import json
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from datetime import datetime, time as dtime, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo

sys.path.insert(0, str(Path(__file__).resolve().parent))
import sh4_check  # noqa: E402
from sh4_pictures import PICTURES  # noqa: E402

P = "io.github.buerlino.apodroid"
# a day ahead: the real post (today's) is older and ignored; US Eastern's today, as the app counts
_TODAY = datetime.now(ZoneInfo("America/New_York")).date()
DATE = (_TODAY + timedelta(days=1)).isoformat()
PUBLISHED = str(int(datetime.combine(_TODAY + timedelta(days=1), dtime(0, 5),
                                     ZoneInfo("America/New_York")).timestamp() * 1000))
TEXTS = {
    "gal": ("A spiral galaxy and its nebulae in a field of stars",
            "See Explanation. Clicking on the picture will download the highest resolution version available.",
            "A spiral galaxy and its nebulae in a field of stars, from Hubble."),
    "up": ("Trees and a lake below the horizon at sunset",
           "Trees and a lake below the horizon at sunset.",
           "Trees and a lake below the horizon at sunset, a landscape."),
}
RECORD = ["wallpaperPicture", "wallpaperDate", "wallpaperUrl"]


def adb(*args, check=True, binary=False):
    r = subprocess.run(["adb", *args], capture_output=True)
    if check and r.returncode:
        raise RuntimeError(f"adb {args}: {r.stderr.decode()}")
    return r.stdout if binary else r.stdout.decode(errors="replace")


def sh(cmd, **kw):
    return adb("shell", cmd, **kw)


def kill(drop_task=False):
    if drop_task:
        # Android 10 restores the page's saved draft after a plain kill, and Set saves it over the pushed prefs
        sh(f"am force-stop {P}", check=False)
        time.sleep(0.5)
        return
    pid = sh(f"pidof {P}", check=False).strip()
    if pid:
        sh(f"run-as {P} kill -9 {pid}", check=False)
        time.sleep(0.5)
        # Android 10: run-as kill does nothing; the emulator's shell can kill as root
        if sh(f"pidof {P}", check=False).strip():
            sh(f"su 0 kill -9 {pid} || su root kill -9 {pid}", check=False)
            time.sleep(0.5)


def read_prefs():
    return ET.fromstring(sh(f"run-as {P} cat shared_prefs/apodroid.xml"))


def put(root, kind, name, value):
    for e in root.findall(f"*[@name='{name}']"):
        root.remove(e)
    if value is None:
        return
    e = ET.SubElement(root, kind, {"name": name})
    if kind == "string":
        e.text = value
    else:
        e.set("value", str(value).lower() if kind == "boolean" else str(value))


def get(root, name):
    e = root.find(f"*[@name='{name}']")
    if e is None:
        return None
    return e.text if e.tag == "string" else e.get("value")


def write_prefs(root):
    data = b"<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n" + ET.tostring(root, encoding="utf-8")
    subprocess.run(["adb", "shell", f"run-as {P} sh -c 'cat > shared_prefs/apodroid.xml; rm -f shared_prefs/apodroid.xml.bak'"],
                   input=data, check=True)


def parse(spec):
    f = spec.split(":") + ["gal", "-", "-"][max(0, len(spec.split(":")) - 6):]
    pic, size, sharp, whole, where, path, text, pos, pill = f[:9]
    return {"picture": pic, "size": size, "sharp": sharp == "1", "whole": whole == "1", "where": where,
            "path": path, "text": text, "pos": None if pos == "-" else float(pos),
            "pill": None if pill == "-" else pill == "1", "spec": spec}


pushed = {"picture": None}


def wallpaper_state():
    d = sh("dumpsys wallpaper")
    sys_part, _, rest = d.partition("Lock wallpaper state:")
    lock_part = rest.partition("Fallback wallpaper state:")[0]

    def hint(part):
        m = re.search(r"mCropHint=Rect\((-?\d+), (-?\d+) - (-?\d+), (-?\d+)\)", part)
        i = re.search(r"id=(\d+)", part)
        return {"id": int(i.group(1)) if i else None, "crop": [int(v) for v in m.groups()] if m else None}

    return {"system": hint(sys_part), "lock": hint(lock_part) if "id=" in lock_part else None}


def find_set_button():
    for _ in range(6):
        sh("uiautomator dump /sdcard/sh4-ui.xml", check=False)
        x = sh("cat /sdcard/sh4-ui.xml", check=False)
        m = re.search(r'text="Set as wallpaper"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', x)
        if m and int(m.group(4)) < 2300:
            a, b, c, d = map(int, m.groups())
            return (a + c) // 2, (b + d) // 2
        sh("input swipe 540 2000 540 900 400")
        time.sleep(1)
    raise RuntimeError("no Set as wallpaper button")


def run_case(c, pictures, out):
    name = c["picture"]
    w, h = PICTURES[name]
    url = f"https://apod.nasa.gov/sh4/{name}.png"
    sh("input keyevent KEYCODE_HOME")
    kill(drop_task=c["path"] == "set")
    root = read_prefs()
    title, alt, expl = TEXTS[c["text"]]
    for kind, k, v in [
        ("string", "date", DATE), ("string", "title", f"{title}"), ("string", "alt", alt),
        ("string", "explanation", expl), ("string", "imageUrl", url), ("string", "pageUrl", url),
        ("boolean", "isVideo", False), ("long", "published", PUBLISHED), ("string", "sizeChecked", url),
        ("boolean", "afterGap", False), ("string", "size", c["size"]), ("boolean", "sharp", c["sharp"]),
        ("boolean", "darken", False), ("string", "where", c["where"]),
        ("boolean", "daily", True), ("boolean", "allSources", False),
    ]:
        put(root, kind, k, v)
    part = c["whole"] or c["pos"] is not None or c["pill"] is not None
    put(root, "string", "cropDate", DATE if part else None)
    put(root, "string", "cropUrl", url if part else None)
    put(root, "boolean", "whole", c["whole"] if part else None)
    put(root, "float", "cropPosition", c["pos"] if part and c["pos"] is not None else None)
    put(root, "boolean", "sideways", c["pill"] if part and c["pill"] is not None else None)
    screens = {"BOTH": ["", "lock."], "HOME": [""], "LOCK": ["lock."]}[c["where"]]
    for prefix in ["", "lock."]:
        for k in RECORD:
            if c["path"] == "job" and prefix in screens:
                put(root, "string", prefix + k, None)
            elif c["path"] == "turn" and prefix in screens:
                # A change time since the record's stamp: the record names another picture, stamped at
                # yesterday's 06:00; the post came out at 06:05 today, in the window of today's 06:00.
                put(root, "string", prefix + k, {"wallpaperPicture": "APOD", "wallpaperDate": "2026-11-16",
                                                 "wallpaperUrl": "https://apod.nasa.gov/sh4/yesterday.png"}[k])
                put(root, "long", prefix + "wallpaperStamp", 1794805200000)
                put(root, "string", prefix + "wallpaperSource", "APOD")
                put(root, "long", "published", 1794891900000)
            elif c["path"] == "set" and prefix in screens:
                # The record says this post is on: the page's load sets nothing, Set sets it afresh.
                put(root, "string", prefix + k, {"wallpaperPicture": "APOD", "wallpaperDate": DATE, "wallpaperUrl": url}[k])
    write_prefs(root)
    if pushed["picture"] != name:
        with open(Path(pictures) / f"{name}.png", "rb") as f:
            subprocess.run(["adb", "shell", f"run-as {P} sh -c 'cat > files/apod.jpg'"], stdin=f, check=True)
        pushed["picture"] = name
    sh(f"run-as {P} rm -f files/debug-set.png files/debug-whole.png", check=False)
    adb("logcat", "-c")
    t0 = time.time()
    if c["path"] in ("job", "turn"):
        sh(f"cmd jobscheduler run -f {P} 1")
        done = r"jobFinished \(job 1\)"
    else:
        sh(f"monkey -p {P} -c android.intent.category.LAUNCHER 1", check=False)
        time.sleep(4)
        x, y = find_set_button()
        adb("logcat", "-c")
        sh(f"input tap {x} {y}")
        done = r"Set: (wallpaper set|nothing|couldn)|Set: .*failed|Couldn't set"
    log = ""
    while time.time() - t0 < 60:
        log = adb("logcat", "-d", "-s", "APODroid", check=False)
        if re.search(done, log):
            break
        time.sleep(0.7)
    time.sleep(0.8)
    log = adb("logcat", "-d", "-s", "APODroid", check=False)
    crash = adb("logcat", "-d", "-b", "crash", check=False)
    sets = [l.split("APODroid: ", 1)[1] for l in log.splitlines() if "wallpaper set" in l]
    prefs = read_prefs()
    subject = {k: get(prefs, k) for k in ["subjectDate", "subjectUrl", "subjectPosition", "subjectSideways",
                                          "subjectPositionSharp", "subjectSidewaysSharp"]}
    wp = wallpaper_state()
    png = Path(out) / "png" / f"{case_id(c)}.png"
    png.parent.mkdir(parents=True, exist_ok=True)
    data = adb("exec-out", f"run-as {P} cat files/debug-set.png", check=False, binary=True)
    has_png = data[:8] == b"\x89PNG\r\n\x1a\n"
    if has_png:
        png.write_bytes(data)
    res = {"case": case_id(c), **c, "w": w, "h": h, "sets": sets, "wallpaper": wp, "subject": subject,
           "debug_set": has_png, "secs": round(time.time() - t0, 1), "crash": crash.strip()[:500]}
    judge(res, png)
    with open(Path(out) / "results.jsonl", "a") as f:
        f.write(json.dumps(res) + "\n")
    print(f"{res['verdict']:9} {res['case']}  {'; '.join(res['problems'])}  [{sets[-1] if sets else 'no set'}]", flush=True)
    return res


def case_id(c):
    pos = "-" if c["pos"] is None else c["pos"]
    pill = "-" if c["pill"] is None else int(c["pill"])
    return f"{c['picture']}_{c['size']}_sharp{int(c['sharp'])}_whole{int(c['whole'])}_{c['where']}_{c['path']}_{c['text']}_p{pos}_pill{pill}"


def judge(r, png):
    """The expectation from sharp-0.9.0.md's rules, then the pixels."""
    w, h, probs = r["w"], r["h"], []
    if r["pill"] is not None:
        sideways = r["pill"]
    else:
        sideways = r["size"] == "AUTO" and w > h and r["text"] == "gal"
    limit = 2.0 if r["sharp"] else None
    W, H = (h, w) if sideways else (w, h)
    e = max(1080 / W, 2400 / H)
    bites = limit is not None and not r["whole"] and e > limit * 1.03
    subj = r["subject"]
    url = f"https://apod.nasa.gov/sh4/{r['picture']}.png"
    raw = None
    if subj["subjectUrl"] == url and subj["subjectDate"] == DATE:
        key = {(True, True): "subjectSidewaysSharp", (True, False): "subjectPositionSharp",
               (False, True): "subjectSideways", (False, False): "subjectPosition"}[(bites, sideways)]
        raw = None if subj[key] is None else float(subj[key])
    elif r["size"] == "AUTO" and r["pos"] is None:
        probs.append("the subject wasn't noted for this picture")
    if r["pos"] is not None:
        position = r["pos"]
    elif r["size"] == "AUTO" and raw is not None:
        position = raw
    else:
        position = 0.5
    r["expect"] = {"sideways": sideways, "bites": bites, "e": round(e, 4), "position": position}
    if r["crash"]:
        probs.append("a crash in logcat")
    if not r["sets"]:
        probs.append("no set logged")
    line = r["sets"][-1] if r["sets"] else ""
    want_screens = {"BOTH": "HOME+LOCK", "HOME": "on HOME", "LOCK": "on LOCK"}[r["where"]]
    if line and want_screens not in line:
        probs.append(f"set on the wrong screens (want {want_screens})")
    if line and ((", sideways" in line) != sideways):
        probs.append(f"log says sideways={', sideways' in line}, want {sideways}")
    if line and ((", sharp ×" in line) != (bites or (r["whole"] and limit is not None and min(1080 / W, 2400 / H) > limit * 1.03))):
        probs.append("the log's sharp note doesn't match the expected limit")
    m = re.search(r", auto (\d\.\d\d)", line)
    if m and abs(float(m.group(1)) - position) > 0.006:
        probs.append(f"log's auto {m.group(1)}, want {position:.2f}")
    plain = not sideways and not r["whole"] and not bites
    if plain:
        # 0.8.0's path: setStream with Fill's crop, no debug-set.png.
        if r["debug_set"]:
            probs.append("debug-set.png written: not the plain setStream path")
        crops = [r["wallpaper"]["system"]] if r["where"] != "LOCK" else [r["wallpaper"]["lock"]]
        # Fill's frame by the Terms: the screen's shape, as large as fits, slid by the position.
        if w * 2400 > h * 1080:
            fw = h * 1080 / 2400
            want = [(w - fw) * position, 0, (w - fw) * position + fw, h]
        else:
            fh = w * 2400 / 1080
            want = [0, (h - fh) * position, w, (h - fh) * position + fh]
        for cr in crops:
            if not cr or not cr["crop"] or max(abs(a - b) for a, b in zip(cr["crop"], want)) > 1.5:
                probs.append(f"crop hint {cr and cr['crop']}, want {[round(v) for v in want]}")
        r["pixels"] = {"crop": crops[0]["crop"] if crops and crops[0] else None}
    else:
        if not r["debug_set"]:
            probs.append("no debug-set.png (a bitmap set expected)")
        elif os.environ.get("SH4_DEFER"):
            probs.append("PENDING: the pixel check deferred (--recheck)")
        else:
            px = sh4_check.check(str(png), w, h, sideways, r["whole"], limit, position)
            r["pixels"] = px
            probs += px["problems"]
    r["problems"] = probs
    r["verdict"] = "FAIL" if probs else "PASS"


def report(paths):
    """One line per case of results.jsonl files: verdict, case, what was measured, the set's log note."""
    for path in paths:
        for line in open(path):
            r = json.loads(line)
            px = r.get("pixels") or {}
            if "crop" in px:
                got = f"setStream, crop {px['crop']}"
            elif "x" in px:
                got = (f"s {px['x']['s']}/{px['y']['s']} (want {px['expected']['s']}), "
                       f"dest {px['x'].get('dest_seen')}x{px['y'].get('dest_seen')}, "
                       f"grid {px['x'].get('grid', '-')}/{px['y'].get('grid', '-')} phase {px['x'].get('grid_phase_px', '-')}/{px['y'].get('grid_phase_px', '-')} px")
            else:
                got = "-"
            note = r["sets"][-1].split(" on ", 1)[-1] if r["sets"] else "no set"
            e = r["expect"]
            print(f"- {r['verdict']} `{r['case']}`: sideways {e['sideways']}, position {e['position']:.2f}, e {e['e']}; "
                  f"{got}; log `{note}`" + (f"; **{'; '.join(r['problems'])}**" if r["problems"] else ""))


def recheck_one(args):
    r, out = args
    judge(r, Path(out) / "png" / f"{r['case']}.png")
    return r


def recheck(out):
    """Judges every case of <out>/results.jsonl again (the pixel checks in parallel) into results-final.jsonl."""
    from multiprocessing import Pool
    rows = [json.loads(l) for l in open(Path(out) / "results.jsonl")]
    with Pool() as pool:
        done = pool.map(recheck_one, [(r, out) for r in rows])
    with open(Path(out) / "results-final.jsonl", "w") as f:
        for r in done:
            f.write(json.dumps(r) + "\n")
            print(f"{r['verdict']:9} {r['case']}  {'; '.join(r['problems'])}")


def main():
    if sys.argv[1] == "--recheck":
        recheck(sys.argv[2])
        return
    if sys.argv[1] == "--report":
        report(sys.argv[2:])
        return
    pictures, out = sys.argv[1], sys.argv[2]
    Path(out).mkdir(parents=True, exist_ok=True)
    for spec in sys.argv[3:]:
        try:
            run_case(parse(spec), pictures, out)
        except Exception as ex:  # one broken case doesn't stop the rest
            print(f"ERROR     {spec}  {ex}", flush=True)


if __name__ == "__main__":
    main()
