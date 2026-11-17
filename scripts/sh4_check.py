#!/usr/bin/env python3
"""SH4's pixel check (sharp-0.9.0.md, Testing 2): Python 3 stdlib only, reads a PNG (no PIL).

Reads a set's `files/debug-set.png` of one of sh4_pictures.py's pictures (R and B encode the
source pixel, G a 16 px checkerboard with a 1 px grid) and checks it against the expected layout,
written here from sharp-0.9.0.md's Terms alone (not from `:core`):
  the oriented picture W×H (upright w×h, or sideways h×w, its top at the screen's right);
  Fill's e = max(sw / W, sh / H); the whole picture's fit f = min(sw / W, sh / H);
  the scale s = e (or f) when there's no limit or it's at most L × 1.03, else L;
  per axis: longer than the screen at s → the visible length screen / s slides by the position
  (0 the left or top of the oriented picture) and the dest spans the screen; else the whole
  length, centred, the rest the background (blurred for these pictures, whose edges aren't black).
Measured: along the middle row and the middle column, the source position from R and B, a
straight line fitted to it (the scale, and where the frame stands at the dest's edges), the
turn by the signs (which corner is where), the grid's period (= s × 16, ± 1 px), the dest's
edges by the checkerboard's contrast (the bands), the bands' pixels (not the sharp picture).

Usage as a module: check(png_path, w, h, sideways, whole, limit, position) -> dict.
CLI: sh4_check.py <png> <w> <h> <sideways 0|1> <whole 0|1> <limit|none> <position>
"""
import json
import struct
import sys
import zlib

SW, SH = 1080, 2400
TOL = 1.03
CELL = 16


def read_png(path):
    data = open(path, "rb").read()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "not a PNG"
    pos, idat, w = 8, bytearray(), None
    while pos < len(data):
        n, kind = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + n]
        if kind == b"IHDR":
            w, h, depth, ctype, _, _, interlace = struct.unpack(">IIBBBBB", body)
            assert depth == 8 and ctype in (2, 6) and interlace == 0, (depth, ctype, interlace)
        elif kind == b"IDAT":
            idat += body
        pos += 12 + n
    bpp = 4 if ctype == 6 else 3
    raw = zlib.decompress(bytes(idat))
    stride = w * bpp
    rows, prev = [], bytearray(stride)
    for y in range(h):
        f = raw[y * (stride + 1)]
        cur = bytearray(raw[y * (stride + 1) + 1:(y + 1) * (stride + 1)])
        if f == 1:
            for i in range(bpp, stride):
                cur[i] = (cur[i] + cur[i - bpp]) & 255
        elif f == 2:
            cur = bytearray((a + b) & 255 for a, b in zip(cur, prev))
        elif f == 3:
            for i in range(stride):
                left = cur[i - bpp] if i >= bpp else 0
                cur[i] = (cur[i] + ((left + prev[i]) >> 1)) & 255
        elif f == 4:
            for i in range(stride):
                a = cur[i - bpp] if i >= bpp else 0
                b = prev[i]
                c = prev[i - bpp] if i >= bpp else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                cur[i] = (cur[i] + (a if pa <= pb and pa <= pc else (b if pb <= pc else c))) & 255
        rows.append(bytes(cur))
        prev = cur
    return w, h, bpp, rows


def expected(w, h, sideways, whole, limit, position, sw=SW, sh=SH):
    W, H = (h, w) if sideways else (w, h)
    if whole:
        base = min(sw / W, sh / H)
    else:
        base = max(sw / W, sh / H)
    s = base if limit is None or base <= limit * TOL else limit
    axes = []
    for length, screen in ((W, sw), (H, sh)):
        drawn = length * s
        if drawn > screen + 0.5 and not whole:
            visible = screen / s
            axes.append({"off": position * (length - visible), "d0": 0.0, "d1": float(screen), "band": False})
        else:
            drawn = min(drawn, screen)
            d0 = (screen - drawn) / 2
            axes.append({"off": 0.0, "d0": d0, "d1": d0 + drawn, "band": drawn < screen - 2})
    return {"W": W, "H": H, "e": base, "s": s, "bites": s != base, "x": axes[0], "y": axes[1]}


def fit(points):
    n = len(points)
    mx = sum(p[0] for p in points) / n
    my = sum(p[1] for p in points) / n
    sxx = sum((p[0] - mx) ** 2 for p in points)
    sxy = sum((p[0] - mx) * (p[1] - my) for p in points)
    a = sxy / sxx
    return a, my - a * mx


def check(path, w, h, sideways, whole, limit, position):
    exp = expected(w, h, sideways, whole, limit, position)
    pw, ph, bpp, rows = read_png(path)
    res = {"expected": {"s": round(exp["s"], 4), "e": round(exp["e"], 4), "bites": exp["bites"],
                        "dest": [round(exp["x"]["d0"]), round(exp["y"]["d0"]), round(exp["x"]["d1"]), round(exp["y"]["d1"])]},
           "problems": []}
    if (pw, ph) != (SW, SH):
        res["problems"].append(f"bitmap {pw}x{ph}, not the screen's {SW}x{SH}")
        res["verdict"] = "FAIL"
        return res
    s = exp["s"]

    def px(x, y):
        o = x * bpp
        return rows[y][o], rows[y][o + 1], rows[y][o + 2]

    def oriented(r, b, axis):
        # The source position along the oriented axis: x (axis 0) or y (axis 1).
        sx, sy = r * (w - 1) / 255, b * (h - 1) / 255
        if not sideways:
            return sx if axis == 0 else sy
        return (h - 1 - sy) if axis == 0 else sx

    for axis, name in ((0, "x"), (1, "y")):
        ax, other = exp[name], exp["y" if axis == 0 else "x"]
        mid = round((other["d0"] + other["d1"]) / 2)
        lo, hi = int(ax["d0"]) + 3, int(ax["d1"]) - 4
        pts = []
        for t in range(lo, hi + 1):
            r, g, b = px(t, mid) if axis == 0 else px(mid, t)
            pts.append((t + 0.5, oriented(r, b, axis)))
        a, c = fit(pts)
        # Which way the source runs on the screen: the turn (sideways: R down the screen, B leftwards).
        chan = [px(t, mid) if axis == 0 else px(mid, t) for t in (lo, hi)]
        if not sideways:
            ci = 0 if axis == 0 else 2
            rising = chan[1][ci] > chan[0][ci]
        else:
            ci = 2 if axis == 0 else 0
            rising = (chan[1][ci] < chan[0][ci]) if axis == 0 else (chan[1][ci] > chan[0][ci])
        if not rising:
            res["problems"].append(f"{name}: the picture runs the wrong way (turn or mirror)")
        s_meas = 1 / a if a else float("inf")
        length = exp["W"] if axis == 0 else exp["H"]
        level = (((h if (sideways and axis == 0) or (not sideways and axis == 1) else w) - 1) / 255) * s
        tol = max(2.0, 0.6 * level) + 1
        worst = 0.0
        for t in (ax["d0"], ax["d1"]):
            want = ax["off"] + (t - ax["d0"]) / s
            got = a * t + c
            worst = max(worst, abs(got - want) * s)
        res[name] = {"s": round(s_meas, 4), "edge_err_px": round(worst, 2), "tol_px": round(tol, 2),
                     "start": round(a * ax["d0"] + c, 1), "start_want": round(ax["off"], 1)}
        if abs(s_meas / s - 1) > 0.015:
            res["problems"].append(f"{name}: scale {s_meas:.4f}, want {s:.4f}")
        if worst > tol:
            res["problems"].append(f"{name}: frame off by {worst:.1f} px at the dest's edge (tol {tol:.1f})")
        # The grid's period: G's line peaks along the line.
        # Only at 1:1 or larger: shrunk, a 1 px line falls across two pixels below the threshold
        # (seen at 0.667: every other line missed); there the gradient's fit measures the scale.
        if s >= 1:
            peaks, run = [], []
            for t in range(lo, hi + 1):
                g = (px(t, mid) if axis == 0 else px(mid, t))[1]
                if g > 150:
                    run.append(t)
                elif run:
                    peaks.append(sum(run) / len(run))
                    run = []
            gaps = sorted(b - a for a, b in zip(peaks, peaks[1:]))
            if len(gaps) >= 3:
                period = gaps[len(gaps) // 2]
                res[name]["grid"] = round(period, 2)
                if abs(period - CELL * s) > 1.01:
                    res["problems"].append(f"{name}: grid period {period:.2f}, want {CELL * s:.2f}")
                # The grid's phase: each line where the expected frame puts a multiple of 16 (± 1.5 px):
                # the frame's position to the pixel, finer than R and B's 256 levels.
                phase = []
                for t in peaks:
                    src = ax["off"] + (t + 0.5 - ax["d0"]) / s - 0.5
                    if sideways and axis == 0:
                        src = h - 1 - src  # the upright row: its lines are at multiples of 16
                    phase.append(abs(src - CELL * round(src / CELL)) * s)
                phase.sort()
                res[name]["grid_phase_px"] = round(phase[len(phase) // 2], 2)
                if phase[len(phase) // 2] > 1.5:
                    res["problems"].append(f"{name}: grid lines {phase[len(phase) // 2]:.1f} px off the expected frame")
        # The dest's edges: from the middle outwards while the pixels match the expected mapping
        # (a band's blurred background doesn't); the expected dest ± 2 px.
        n = SW if axis == 0 else SH
        err = lambda t: abs(oriented(*(lambda q: (q[0], q[2]))(px(t, mid) if axis == 0 else px(mid, t)), axis)
                            - (ax["off"] + (t + 0.5 - ax["d0"]) / s - 0.5)) * s
        centre = int((ax["d0"] + ax["d1"]) / 2)
        lim = tol + 2
        t0 = centre
        while t0 > 0 and (err(t0 - 1) <= lim or (t0 > 1 and err(t0 - 2) <= lim)):
            t0 -= 1
        t1 = centre
        while t1 < n - 1 and (err(t1 + 1) <= lim or (t1 < n - 2 and err(t1 + 2) <= lim)):
            t1 += 1
        res[name]["dest_seen"] = [t0, t1 + 1]
        if abs(t0 - ax["d0"]) > 2 or abs(t1 + 1 - ax["d1"]) > 2:
            res["problems"].append(f"{name}: the picture seen {t0}..{t1 + 1}, want {ax['d0']:.0f}..{ax['d1']:.0f}")
        if ax["band"]:
            t = int(ax["d0"] / 2)
            g = [(px(u, mid) if axis == 0 else px(mid, u)) for u in range(max(0, t - 8), t + 8)]
            spread = max(p[1] for p in g) - min(p[1] for p in g)
            mean = sum(sum(p) for p in g) / (3 * len(g))
            res[name]["band_mean"] = round(mean, 1)
            if spread > 60:
                res["problems"].append(f"{name}: the band at {t} isn't blurred (G spread {spread})")
            if mean < 3:
                res["problems"].append(f"{name}: the band at {t} is black (these pictures' edges aren't)")
    res["verdict"] = "FAIL" if res["problems"] else "PASS"
    return res


def main():
    a = sys.argv[1:]
    limit = None if a[5] == "none" else float(a[5])
    print(json.dumps(check(a[0], int(a[1]), int(a[2]), a[3] == "1", a[4] == "1", limit, float(a[6])), indent=1))


if __name__ == "__main__":
    main()
