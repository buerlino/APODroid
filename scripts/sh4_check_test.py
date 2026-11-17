#!/usr/bin/env python3
"""sh4_check.py's own tests (Python 3 stdlib, unittest): python3 scripts/sh4_check_test.py

Builds a set's bitmap the way the expected layout says (nearest neighbour, the band one flat grey)
from sh4_pictures.py's tiny 640x480 at the limit 2 (s 2, dest y 720-1680), the case whose middle
row and column fell on grid lines (fix round 1 of SH4: the grid check found no peaks and was
skipped without a word, the case passing on R and B alone).
"""
import os
import shutil
import sys
import tempfile
import unittest
import zlib
import struct

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sh4_check  # noqa: E402

W, H, CELL = 640, 480, 16


def source(x, y, grid=True):
    r = round(x * 255 / (W - 1))
    b = round(y * 255 / (H - 1))
    if not grid:
        g = 64
    elif x % CELL == 0 or y % CELL == 0:
        g = 255
    else:
        g = 64 if ((x // CELL) + (y // CELL)) % 2 else 0
    return r, g, b


def write_png(path, rows, w, h):
    raw = b"".join(b"\x00" + bytes(r) for r in rows)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw, 1)) + chunk(b"IEND", b""))


def render(path, shift=0, grid=True):
    """The expected layout of tiny 640x480, upright, Fill limited to 2, position 0.5; shifted right by `shift` px."""
    exp = sh4_check.expected(W, H, False, False, 2.0, 0.5)
    s, ax, ay = exp["s"], exp["x"], exp["y"]
    sw, sh = sh4_check.SW, sh4_check.SH
    xs = [min(W - 1, max(0, round(ax["off"] + (X - shift + 0.5 - ax["d0"]) / s - 0.5))) for X in range(sw)]
    band = bytes([40, 40, 40]) * sw
    rows = []
    for Y in range(sh):
        if Y < ay["d0"] or Y >= ay["d1"]:
            rows.append(band)
            continue
        y = min(H - 1, max(0, round(ay["off"] + (Y + 0.5 - ay["d0"]) / s - 0.5)))
        row = bytearray()
        for x in xs:
            row += bytes(source(x, y, grid))
        rows.append(row)
    write_png(path, rows, sw, sh)


class GridTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.dir = tempfile.mkdtemp()
        cls.addClassCleanup(shutil.rmtree, cls.dir)

    def check(self, name, **kw):
        path = os.path.join(self.dir, name + ".png")
        render(path, **kw)
        return sh4_check.check(path, W, H, False, False, 2.0, 0.5)

    def test_the_exact_layout_passes_with_the_grid_on_both_axes(self):
        res = self.check("exact")
        self.assertEqual(res["problems"], [])
        self.assertEqual(res["x"]["grid"], 32)
        self.assertEqual(res["y"]["grid"], 32)
        self.assertLessEqual(res["x"]["grid_phase_px"], 0.5)
        self.assertLessEqual(res["y"]["grid_phase_px"], 0.5)

    def test_a_frame_2_px_off_fails_by_the_grid(self):
        # 2 px is well within the R/B fit's tolerance (about 4 px at s 2; 3 px is at its edge): only the grid's phase catches it.
        res = self.check("shifted", shift=2)
        self.assertEqual(res["verdict"], "FAIL")
        self.assertTrue(any("grid lines" in p for p in res["problems"]), res["problems"])

    def test_no_grid_at_s_1_or_more_is_a_problem(self):
        res = self.check("nogrid", grid=False)
        self.assertEqual(res["verdict"], "FAIL")
        self.assertTrue(any("no grid found" in p for p in res["problems"]), res["problems"])


if __name__ == "__main__":
    unittest.main()
