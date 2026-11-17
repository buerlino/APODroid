#!/usr/bin/env python3
"""SH4's made-up test pictures (sharp-0.9.0.md, Testing 2): PNGs, Python 3 stdlib only.

Each picture encodes where every pixel came from, so a set's bitmap can be read back:
  R = round(x * 255 / (w - 1))   (0 at the left edge, 255 at the right)
  B = round(y * 255 / (h - 1))   (0 at the top, 255 at the bottom)
  G = a checkerboard of 16 px cells (0 / 64) with a 1 px grid line (255) at every x or y % 16 == 0.
So the four corners have their own colours (top left black, top right red, bottom left blue,
bottom right magenta), the grid's period on the screen is the scale × 16, and R and B give the
source position of any pixel (sh4_check.py).

Usage: sh4_pictures.py <out dir> [name ...]   (no names: all of PICTURES)
"""
import struct
import sys
import zlib
from pathlib import Path

CELL = 16

# name: (width, height). The screen is the emulator's 1080×2400 (e = max(1080 / W, 2400 / H)).
PICTURES = {
    "p23_400x600": (400, 600),        # portrait 2:3, e 4
    "p920_2160x4800": (2160, 4800),   # portrait, e 0.5
    "sq_1200": (1200, 1200),          # square, e exactly 2 (L)
    "sq_1180": (1180, 1180),          # square, e 2.034 (within the tolerance)
    "sq_1150": (1150, 1150),          # square, e 2.087 (just above)
    "w105_630x600": (630, 600),       # 1.05:1, e 4; sideways 1.71
    "w147_1764x1200": (1764, 1200),   # 1.47:1, e 2; sideways 1.36
    "w32_1500x1022": (1500, 1022),    # 7 Nov's size, e 2.35; sideways 1.60
    "w32_900x600": (900, 600),        # 3:2, e 4; sideways 2.67
    "w32_3600x2400": (3600, 2400),    # 3:2, e 1; sideways 0.67
    "w4_2400x600": (2400, 600),       # 4:1, e 4; sideways 1.8
    "w4_1200x300": (1200, 300),       # 4:1, e 8; sideways 3.6
    "tiny_640x480": (640, 480),       # e 5; sideways 3.75
    "huge_8000x6000": (8000, 6000),   # e 0.4; sideways 0.3
}


def png(path: Path, w: int, h: int) -> None:
    r = bytes(round(x * 255 / (w - 1)) for x in range(w))
    g_even = bytes(255 if x % CELL == 0 else (64 if (x // CELL) % 2 else 0) for x in range(w))
    g_odd = bytes(255 if x % CELL == 0 else (0 if (x // CELL) % 2 else 64) for x in range(w))
    g_line = bytes([255]) * w
    comp = zlib.compressobj(6)
    out = bytearray()
    row = bytearray(3 * w)
    row[0::3] = r
    for y in range(h):
        row[1::3] = g_line if y % CELL == 0 else (g_odd if (y // CELL) % 2 else g_even)
        row[2::3] = bytes([round(y * 255 / (h - 1))]) * w
        out += comp.compress(b"\x00" + row)
    out += comp.flush()

    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    path.write_bytes(
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0))
        + chunk(b"IDAT", bytes(out))
        + chunk(b"IEND", b"")
    )


def main() -> None:
    out = Path(sys.argv[1])
    out.mkdir(parents=True, exist_ok=True)
    for name in sys.argv[2:] or PICTURES:
        w, h = PICTURES[name]
        png(out / f"{name}.png", w, h)
        print(name, w, h)


if __name__ == "__main__":
    main()
