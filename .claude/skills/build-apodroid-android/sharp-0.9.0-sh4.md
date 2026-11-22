# SH4: the device pass of sharp-0.9.0.md's Testing 2 and 3

(rosie, pipeline run 154, 2026-11-17, on the Android 14 emulator `emulator-5564`, 1080×2400 at 420
dpi; the tablet is pascaline's until 17 Nov. dev's debug build 78568cd, 0.9.0. The PNGs, the
JSON lines and the logs are in `.coord/runs/154-sh4/tablet/`, not committed.)

**Result: 151 of 151 pixel cases PASS, 4 of 4 download cases PASS, the update from 0.8.0 PASS; no
FAIL, so no SH4f package. One DIFFERENT: no real host answers the probe with 200.** Fix round 1
(the audit): the grid scanned through a cell's middle, every case judged again, all PASS (How). No crash
(`logcat -b crash` empty, `exit-info` no CRASH).

## How (the scripts, Python 3 stdlib only)

- `scripts/sh4_pictures.py`: the made-up pictures, PNGs whose pixels say where they came from:
  R = x·255/(w−1), B = y·255/(h−1) (so the four corners have their own colours: top left black, top
  right red, bottom left blue, bottom right magenta), G a checkerboard of 16 px cells (0 / 64) with
  a 1 px grid line (255) every 16 px. Dark (mean grey about 60), edges not black (so the blurred
  background).
- `scripts/sh4_check.py`: reads a PNG (its own decoder, all five filters) and checks a set's
  `debug-set.png` against the layout written from sharp-0.9.0.md's Terms alone (not from `:core`):
  along a row and a column near the dest's middle, each through the middle of a grid cell (source
  ≡ 8 mod 16, so the scan crosses the grid lines), the source position from R and B, a straight
  line fitted to it (the scale ±1.5%, the frame at the dest's edges within the gradient's
  resolution, about 4 px at s 2), the direction (the turn: which corner is where), the grid's
  period (= s × 16 ± 1 px) and phase (each line within 1.5 px of where the expected frame puts it:
  the position to the pixel), the dest's edges (where the pixels stop matching the expected
  mapping, ± 2 px), the bands (blurred, not black). Grid checks only at s ≥ 1: shrunk, a 1 px line
  falls across two pixels below the threshold (seen at 0.667, every other line missed: a checker
  artifact, first reported as 2 FAILs, then fixed; the gradient still measures the scale there).
  At s ≥ 1 no grid found is a FAIL. A wrong position by 0.05 FAILs (checked by hand); a frame 2 px
  off FAILs by the grid alone (`scripts/sh4_check_test.py`, stdlib unittest: the exact layout, 2 px
  off, no grid).
- **Fix round 1** (the audit): the first pass scanned the dest's middle row and column, which for
  some layouts lie on a grid line (tiny 640×480 at s 2: screen y 1200 is source y 240 = 15 × 16,
  x 540 source x 320), so G was 255 the whole way, no peak showed and the grid check was skipped
  without a word: those cases (the tiny ones with the limit or Whole, `sq_1150` y,
  `w4_2400x600_CENTRE_sharp1` y; `grid -/-` or `-/32.0` at s ≥ 1) were checked by the R/B fit
  alone. Now every one of the 151 cases is judged again from its pulled PNG (`--recheck`): all
  PASS, and every bitmap case at s ≥ 1 has the grid on both axes (phase 0.0–1.2 px, median 0.0;
  the 1.2 at `w105_630x600_AUTO_sharp0_whole0` x, s 3.81). A `grid -/-` below is a case at s < 1,
  checked by the R/B fit and the dest's edges only.
- `scripts/sh4_run.py`: per case, after `run-as kill -9`, writes APOD's post into the prefs
  (dated US Eastern's today + 1, published 00:05 Eastern, since B34; 2026-11-18 before, so the job's fetch of the real post finds it older and keeps the
  fake; `sizeChecked` = its URL, so no probe; `darken` off, so the darkening doesn't move a plain Fill
  off `setStream`; Size, `sharp`, `where`, ⛶'s part: `whole`, `cropPosition`, `sideways`), the
  picture as `files/apod.jpg`, then sets it by one of three paths: **job** (the screens' records
  forgotten, `run -f 1`), **turn** (the records name another picture stamped at yesterday's 06:00,
  the post out at 06:05 today: a change time passed, `run -f 1`), **set** (the records name this
  post, so the page's load sets nothing; the page's "Set as wallpaper" tapped). Then the set's log
  line, `dumpsys wallpaper`'s crop hints, the subject notes and `debug-set.png`. The expectation:
  the orientation by the rule (the pill, else Auto + APOD + landscape + `hasUp` false), the
  position (the part's, else Auto's `framePosition` of the noted subject for that orientation and,
  where the limit bites, the limited frame's, else 0.5; the noted subject as it is since B34, as 0.9.1's
  app: before it the judge applied 0.9.0's 0.2 fallback and faked `autoFallback`), the limit (2.0 ×
  1.03). A layout the limit doesn't bite, upright, not whole: no `debug-set.png` (0.8.0's
  `setStream`) and the crop hint = Fill's frame ± 1.5 px. Also the log: the screens, ", sideways",
  ", sharp ×…", ", auto p". `--recheck <dir>` judges again in parallel, `--report` writes the lines.
- `scripts/sh4_download.py probe`: Testing 3 (below).
- Texts: "gal" = title "A spiral galaxy and its nebulae in a field of stars" (`hasUp` false), "up"
  = "Trees and a lake below the horizon at sunset" (true).
- Not covered by the cases: "my picture" limited (Testing 2 doesn't list it), the darkening with
  the limit (the cases run with `darken` off; BR2's path is unchanged), a real picture (the FP6's
  Testing 6), Live (unchanged by SH2c).

## Testing 2: the pixels

Matrix A (the job, Both): 14 pictures (portrait 2:3 e 4, portrait e 0.5, square at e exactly 2,
2.034 within the tolerance and 2.087 just above, 1.05:1, 1.47:1, 7 Nov's 1500×1022, 3:2 at e 4 and
e 1, 4:1 at e 4 and 8, tiny 640×480, huge 8000×6000) × Centre / Auto × the switch off / on ×
Whole off / on: 112 PASS. Then 39 more: Home and Lock alone, Set's path (Both, Home, Lock), the
faked change time, dragged parts at 0 and 1, the pill both ways under Centre and Auto, the "up"
text under Auto. Each line: the case, the expectation (sideways, position, Fill's e), the
measurement (the scale along x/y and the expected s, where the picture lies on the screen, the
grid's period and phase), the set's log note.

### Matrix A

- PASS `p23_400x600_CENTRE_sharp0_whole0_BOTH_job`: sideways False, position 0.50, e 4.0; setStream, crop [65, 0, 335, 600]; log `HOME+LOCK`
- PASS `p23_400x600_CENTRE_sharp0_whole1_BOTH_job`: sideways False, position 0.50, e 4.0; s 2.7001/2.7 (want 2.7), dest [0, 1080]x[390, 2010], grid 43.0/43.5 phase 0.15/0.15 px; log `HOME+LOCK`
- PASS `p23_400x600_CENTRE_sharp1_whole0_BOTH_job`: sideways False, position 0.50, e 4.0; s 2.0001/2.0 (want 2.0), dest [140, 940]x[600, 1800], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sharp ×2.00 (Fill ×4.00)`
- PASS `p23_400x600_CENTRE_sharp1_whole1_BOTH_job`: sideways False, position 0.50, e 4.0; s 2.0001/2.0 (want 2.0), dest [140, 940]x[600, 1800], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sharp ×2.00 (whole ×2.70)`
- PASS `p23_400x600_AUTO_sharp0_whole0_BOTH_job`: sideways False, position 1.00, e 4.0; setStream, crop [130, 0, 400, 600]; log `HOME+LOCK, auto 1.00`
- PASS `p23_400x600_AUTO_sharp0_whole1_BOTH_job`: sideways False, position 1.00, e 4.0; s 2.7001/2.7 (want 2.7), dest [0, 1080]x[390, 2010], grid 43.0/43.5 phase 0.15/0.15 px; log `HOME+LOCK`
- PASS `p23_400x600_AUTO_sharp1_whole0_BOTH_job`: sideways False, position 0.50, e 4.0; s 2.0001/2.0 (want 2.0), dest [140, 940]x[600, 1800], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sharp ×2.00 (Fill ×4.00)`
- PASS `p23_400x600_AUTO_sharp1_whole1_BOTH_job`: sideways False, position 1.00, e 4.0; s 2.0001/2.0 (want 2.0), dest [140, 940]x[600, 1800], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sharp ×2.00 (whole ×2.70)`
- PASS `p920_2160x4800_CENTRE_sharp0_whole0_BOTH_job`: sideways False, position 0.50, e 0.5; setStream, crop [0, 0, 2160, 4800]; log `HOME+LOCK`
- PASS `p920_2160x4800_CENTRE_sharp0_whole1_BOTH_job`: sideways False, position 0.50, e 0.5; s 0.5/0.5 (want 0.5), dest [0, 1080]x[0, 2400], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `p920_2160x4800_CENTRE_sharp1_whole0_BOTH_job`: sideways False, position 0.50, e 0.5; setStream, crop [0, 0, 2160, 4800]; log `HOME+LOCK`
- PASS `p920_2160x4800_CENTRE_sharp1_whole1_BOTH_job`: sideways False, position 0.50, e 0.5; s 0.5/0.5 (want 0.5), dest [0, 1080]x[0, 2400], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `p920_2160x4800_AUTO_sharp0_whole0_BOTH_job`: sideways False, position 0.50, e 0.5; setStream, crop [0, 0, 2160, 4800]; log `HOME+LOCK`
- PASS `p920_2160x4800_AUTO_sharp0_whole1_BOTH_job`: sideways False, position 0.50, e 0.5; s 0.5/0.5 (want 0.5), dest [0, 1080]x[0, 2400], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `p920_2160x4800_AUTO_sharp1_whole0_BOTH_job`: sideways False, position 0.50, e 0.5; setStream, crop [0, 0, 2160, 4800]; log `HOME+LOCK`
- PASS `p920_2160x4800_AUTO_sharp1_whole1_BOTH_job`: sideways False, position 0.50, e 0.5; s 0.5/0.5 (want 0.5), dest [0, 1080]x[0, 2400], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `sq_1200_CENTRE_sharp0_whole0_BOTH_job`: sideways False, position 0.50, e 2.0; setStream, crop [330, 0, 870, 1200]; log `HOME+LOCK`
- PASS `sq_1200_CENTRE_sharp0_whole1_BOTH_job`: sideways False, position 0.50, e 2.0; s 0.9/0.9 (want 0.9), dest [0, 1080]x[660, 1740], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `sq_1200_CENTRE_sharp1_whole0_BOTH_job`: sideways False, position 0.50, e 2.0; setStream, crop [330, 0, 870, 1200]; log `HOME+LOCK`
- PASS `sq_1200_CENTRE_sharp1_whole1_BOTH_job`: sideways False, position 0.50, e 2.0; s 0.9/0.9 (want 0.9), dest [0, 1080]x[660, 1740], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `sq_1200_AUTO_sharp0_whole0_BOTH_job`: sideways False, position 1.00, e 2.0; setStream, crop [660, 0, 1200, 1200]; log `HOME+LOCK, auto 1.00`
- PASS `sq_1200_AUTO_sharp0_whole1_BOTH_job`: sideways False, position 1.00, e 2.0; s 0.9/0.9 (want 0.9), dest [0, 1080]x[660, 1740], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `sq_1200_AUTO_sharp1_whole0_BOTH_job`: sideways False, position 1.00, e 2.0; setStream, crop [660, 0, 1200, 1200]; log `HOME+LOCK, auto 1.00`
- PASS `sq_1200_AUTO_sharp1_whole1_BOTH_job`: sideways False, position 1.00, e 2.0; s 0.9/0.9 (want 0.9), dest [0, 1080]x[660, 1740], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `sq_1180_CENTRE_sharp0_whole0_BOTH_job`: sideways False, position 0.50, e 2.0339; setStream, crop [325, 0, 856, 1180]; log `HOME+LOCK`
- PASS `sq_1180_CENTRE_sharp0_whole1_BOTH_job`: sideways False, position 0.50, e 2.0339; s 0.9153/0.9153 (want 0.9153), dest [0, 1080]x[660, 1740], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `sq_1180_CENTRE_sharp1_whole0_BOTH_job`: sideways False, position 0.50, e 2.0339; setStream, crop [325, 0, 856, 1180]; log `HOME+LOCK`
- PASS `sq_1180_CENTRE_sharp1_whole1_BOTH_job`: sideways False, position 0.50, e 2.0339; s 0.9153/0.9153 (want 0.9153), dest [0, 1080]x[660, 1740], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `sq_1180_AUTO_sharp0_whole0_BOTH_job`: sideways False, position 0.87, e 2.0339; setStream, crop [566, 0, 1097, 1180]; log `HOME+LOCK, auto 0.87`
- PASS `sq_1180_AUTO_sharp0_whole1_BOTH_job`: sideways False, position 0.87, e 2.0339; s 0.9153/0.9153 (want 0.9153), dest [0, 1080]x[660, 1740], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `sq_1180_AUTO_sharp1_whole0_BOTH_job`: sideways False, position 0.87, e 2.0339; setStream, crop [566, 0, 1097, 1180]; log `HOME+LOCK, auto 0.87`
- PASS `sq_1180_AUTO_sharp1_whole1_BOTH_job`: sideways False, position 0.87, e 2.0339; s 0.9153/0.9153 (want 0.9153), dest [0, 1080]x[660, 1740], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `sq_1150_CENTRE_sharp0_whole0_BOTH_job`: sideways False, position 0.50, e 2.087; setStream, crop [317, 0, 834, 1150]; log `HOME+LOCK`
- PASS `sq_1150_CENTRE_sharp0_whole1_BOTH_job`: sideways False, position 0.50, e 2.087; s 0.9392/0.9392 (want 0.9391), dest [0, 1080]x[660, 1740], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `sq_1150_CENTRE_sharp1_whole0_BOTH_job`: sideways False, position 0.50, e 2.087; s 2.0002/2.0001 (want 2.0), dest [0, 1080]x[50, 2350], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sharp ×2.00 (Fill ×2.09)`
- PASS `sq_1150_CENTRE_sharp1_whole1_BOTH_job`: sideways False, position 0.50, e 2.087; s 0.9392/0.9392 (want 0.9391), dest [0, 1080]x[660, 1740], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `sq_1150_AUTO_sharp0_whole0_BOTH_job`: sideways False, position 1.00, e 2.087; setStream, crop [633, 0, 1150, 1150]; log `HOME+LOCK, auto 1.00`
- PASS `sq_1150_AUTO_sharp0_whole1_BOTH_job`: sideways False, position 1.00, e 2.087; s 0.9392/0.9392 (want 0.9391), dest [0, 1080]x[660, 1740], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `sq_1150_AUTO_sharp1_whole0_BOTH_job`: sideways False, position 1.00, e 2.087; s 2.0/2.0001 (want 2.0), dest [0, 1080]x[50, 2350], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, auto 1.00, sharp ×2.00 (Fill ×2.09)`
- PASS `sq_1150_AUTO_sharp1_whole1_BOTH_job`: sideways False, position 1.00, e 2.087; s 0.9392/0.9392 (want 0.9391), dest [0, 1080]x[660, 1740], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `w105_630x600_CENTRE_sharp0_whole0_BOTH_job`: sideways False, position 0.50, e 4.0; setStream, crop [180, 0, 450, 600]; log `HOME+LOCK`
- PASS `w105_630x600_CENTRE_sharp0_whole1_BOTH_job`: sideways False, position 0.50, e 4.0; s 1.7143/1.7132 (want 1.7143), dest [0, 1080]x[686, 1714], grid 27.5/27.5 phase 0.14/0.14 px; log `HOME+LOCK`
- PASS `w105_630x600_CENTRE_sharp1_whole0_BOTH_job`: sideways False, position 0.50, e 4.0; s 2.0/2.0 (want 2.0), dest [0, 1080]x[600, 1800], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sharp ×2.00 (Fill ×4.00)`
- PASS `w105_630x600_CENTRE_sharp1_whole1_BOTH_job`: sideways False, position 0.50, e 4.0; s 1.7143/1.7132 (want 1.7143), dest [0, 1080]x[686, 1714], grid 27.5/27.5 phase 0.14/0.14 px; log `HOME+LOCK`
- PASS `w105_630x600_AUTO_sharp0_whole0_BOTH_job`: sideways True, position 0.16, e 3.8095; s 3.8164/3.8096 (want 3.8095), dest [0, 1080]x[0, 2400], grid 61.0/61.0 phase 1.2/0.26 px; log `HOME+LOCK, auto 0.16, sideways, auto turn`
- PASS `w105_630x600_AUTO_sharp0_whole1_BOTH_job`: sideways True, position 0.16, e 3.8095; s 1.8/1.8 (want 1.8), dest [0, 1080]x[633, 1767], grid 29.0/29.0 phase 0.2/0.2 px; log `HOME+LOCK, sideways, auto turn`
- PASS `w105_630x600_AUTO_sharp1_whole0_BOTH_job`: sideways True, position 0.50, e 3.8095; s 2.0/2.0 (want 2.0), dest [0, 1080]x[570, 1830], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sideways, auto turn, sharp ×2.00 (Fill ×3.81)`
- PASS `w105_630x600_AUTO_sharp1_whole1_BOTH_job`: sideways True, position 0.16, e 3.8095; s 1.8/1.8 (want 1.8), dest [0, 1080]x[633, 1767], grid 29.0/29.0 phase 0.2/0.2 px; log `HOME+LOCK, sideways, auto turn`
- PASS `w147_1764x1200_CENTRE_sharp0_whole0_BOTH_job`: sideways False, position 0.50, e 2.0; setStream, crop [612, 0, 1152, 1200]; log `HOME+LOCK`
- PASS `w147_1764x1200_CENTRE_sharp0_whole1_BOTH_job`: sideways False, position 0.50, e 2.0; s 0.6122/0.6116 (want 0.6122), dest [0, 1080]x[833, 1567], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `w147_1764x1200_CENTRE_sharp1_whole0_BOTH_job`: sideways False, position 0.50, e 2.0; setStream, crop [612, 0, 1152, 1200]; log `HOME+LOCK`
- PASS `w147_1764x1200_CENTRE_sharp1_whole1_BOTH_job`: sideways False, position 0.50, e 2.0; s 0.6122/0.6116 (want 0.6122), dest [0, 1080]x[833, 1567], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `w147_1764x1200_AUTO_sharp0_whole0_BOTH_job`: sideways True, position 0.50, e 1.3605; s 1.3619/1.3605 (want 1.3605), dest [0, 1080]x[0, 2400], grid 22.0/22.0 phase 0.7/0.15 px; log `HOME+LOCK, sideways, auto turn`
- PASS `w147_1764x1200_AUTO_sharp0_whole1_BOTH_job`: sideways True, position 0.50, e 1.3605; s 0.9/0.8997 (want 0.9), dest [0, 1080]x[406, 1993], grid -/- phase -/- px; log `HOME+LOCK, sideways, auto turn`
- PASS `w147_1764x1200_AUTO_sharp1_whole0_BOTH_job`: sideways True, position 0.50, e 1.3605; s 1.3619/1.3605 (want 1.3605), dest [0, 1080]x[0, 2400], grid 22.0/22.0 phase 0.7/0.15 px; log `HOME+LOCK, sideways, auto turn`
- PASS `w147_1764x1200_AUTO_sharp1_whole1_BOTH_job`: sideways True, position 0.50, e 1.3605; s 0.9/0.8997 (want 0.9), dest [0, 1080]x[406, 1993], grid -/- phase -/- px; log `HOME+LOCK, sideways, auto turn`
- PASS `w32_1500x1022_CENTRE_sharp0_whole0_BOTH_job`: sideways False, position 0.50, e 2.3483; setStream, crop [521, 0, 980, 1022]; log `HOME+LOCK`
- PASS `w32_1500x1022_CENTRE_sharp0_whole1_BOTH_job`: sideways False, position 0.50, e 2.3483; s 0.7201/0.7185 (want 0.72), dest [0, 1080]x[832, 1567], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `w32_1500x1022_CENTRE_sharp1_whole0_BOTH_job`: sideways False, position 0.50, e 2.3483; s 2.0005/1.998 (want 2.0), dest [0, 1080]x[178, 2222], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sharp ×2.00 (Fill ×2.35)`
- PASS `w32_1500x1022_CENTRE_sharp1_whole1_BOTH_job`: sideways False, position 0.50, e 2.3483; s 0.7201/0.7185 (want 0.72), dest [0, 1080]x[832, 1567], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `w32_1500x1022_AUTO_sharp0_whole0_BOTH_job`: sideways True, position 0.00, e 1.6; s 1.5984/1.6001 (want 1.6), dest [0, 1080]x[0, 2400], grid 25.5/25.5 phase 0.1/0.1 px; log `HOME+LOCK, auto 0.00, sideways, auto turn`
- PASS `w32_1500x1022_AUTO_sharp0_whole1_BOTH_job`: sideways True, position 0.00, e 1.6; s 1.0557/1.0567 (want 1.0568), dest [0, 1080]x[407, 1992], grid 17.0/17.0 phase 0.26/0.45 px; log `HOME+LOCK, sideways, auto turn`
- PASS `w32_1500x1022_AUTO_sharp1_whole0_BOTH_job`: sideways True, position 0.00, e 1.6; s 1.5984/1.6001 (want 1.6), dest [0, 1080]x[0, 2400], grid 25.5/25.5 phase 0.1/0.1 px; log `HOME+LOCK, auto 0.00, sideways, auto turn`
- PASS `w32_1500x1022_AUTO_sharp1_whole1_BOTH_job`: sideways True, position 0.00, e 1.6; s 1.0557/1.0567 (want 1.0568), dest [0, 1080]x[407, 1992], grid 17.0/17.0 phase 0.26/0.45 px; log `HOME+LOCK, sideways, auto turn`
- PASS `w32_900x600_CENTRE_sharp0_whole0_BOTH_job`: sideways False, position 0.50, e 4.0; setStream, crop [315, 0, 585, 600]; log `HOME+LOCK`
- PASS `w32_900x600_CENTRE_sharp0_whole1_BOTH_job`: sideways False, position 0.50, e 4.0; s 1.2/1.2 (want 1.2), dest [0, 1080]x[840, 1560], grid 19.0/19.0 phase 0.3/0.3 px; log `HOME+LOCK`
- PASS `w32_900x600_CENTRE_sharp1_whole0_BOTH_job`: sideways False, position 0.50, e 4.0; s 1.9999/2.0 (want 2.0), dest [0, 1080]x[600, 1800], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sharp ×2.00 (Fill ×4.00)`
- PASS `w32_900x600_CENTRE_sharp1_whole1_BOTH_job`: sideways False, position 0.50, e 4.0; s 1.2/1.2 (want 1.2), dest [0, 1080]x[840, 1560], grid 19.0/19.0 phase 0.3/0.3 px; log `HOME+LOCK`
- PASS `w32_900x600_AUTO_sharp0_whole0_BOTH_job`: sideways True, position 0.25, e 2.6667; s 2.6667/2.6667 (want 2.6667), dest [0, 1080]x[0, 2400], grid 43.0/43.0 phase 0.67/0.17 px; log `HOME+LOCK, auto 0.25, sideways, auto turn`
- PASS `w32_900x600_AUTO_sharp0_whole1_BOTH_job`: sideways True, position 0.25, e 2.6667; s 1.8/1.8 (want 1.8), dest [0, 1080]x[390, 2010], grid 29.0/29.0 phase 0.2/0.2 px; log `HOME+LOCK, sideways, auto turn`
- PASS `w32_900x600_AUTO_sharp1_whole0_BOTH_job`: sideways True, position 0.50, e 2.6667; s 2.0/2.0 (want 2.0), dest [0, 1080]x[300, 2100], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sideways, auto turn, sharp ×2.00 (Fill ×2.67)`
- PASS `w32_900x600_AUTO_sharp1_whole1_BOTH_job`: sideways True, position 0.25, e 2.6667; s 1.8/1.8 (want 1.8), dest [0, 1080]x[390, 2010], grid 29.0/29.0 phase 0.2/0.2 px; log `HOME+LOCK, sideways, auto turn`
- PASS `w32_3600x2400_CENTRE_sharp0_whole0_BOTH_job`: sideways False, position 0.50, e 1.0; setStream, crop [1260, 0, 2340, 2400]; log `HOME+LOCK`
- PASS `w32_3600x2400_CENTRE_sharp0_whole1_BOTH_job`: sideways False, position 0.50, e 1.0; s 0.3/0.3 (want 0.3), dest [0, 1080]x[840, 1560], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `w32_3600x2400_CENTRE_sharp1_whole0_BOTH_job`: sideways False, position 0.50, e 1.0; setStream, crop [1260, 0, 2340, 2400]; log `HOME+LOCK`
- PASS `w32_3600x2400_CENTRE_sharp1_whole1_BOTH_job`: sideways False, position 0.50, e 1.0; s 0.3/0.3 (want 0.3), dest [0, 1080]x[840, 1560], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `w32_3600x2400_AUTO_sharp0_whole0_BOTH_job`: sideways True, position 0.50, e 0.6667; s 0.6667/0.6667 (want 0.6667), dest [0, 1080]x[0, 2400], grid -/- phase -/- px; log `HOME+LOCK, sideways, auto turn`
- PASS `w32_3600x2400_AUTO_sharp0_whole1_BOTH_job`: sideways True, position 0.50, e 0.6667; s 0.45/0.45 (want 0.45), dest [0, 1080]x[390, 2010], grid -/- phase -/- px; log `HOME+LOCK, sideways, auto turn`
- PASS `w32_3600x2400_AUTO_sharp1_whole0_BOTH_job`: sideways True, position 0.50, e 0.6667; s 0.6667/0.6667 (want 0.6667), dest [0, 1080]x[0, 2400], grid -/- phase -/- px; log `HOME+LOCK, sideways, auto turn`
- PASS `w32_3600x2400_AUTO_sharp1_whole1_BOTH_job`: sideways True, position 0.50, e 0.6667; s 0.45/0.45 (want 0.45), dest [0, 1080]x[390, 2010], grid -/- phase -/- px; log `HOME+LOCK, sideways, auto turn`
- PASS `w4_2400x600_CENTRE_sharp0_whole0_BOTH_job`: sideways False, position 0.50, e 4.0; setStream, crop [1065, 0, 1335, 600]; log `HOME+LOCK`
- PASS `w4_2400x600_CENTRE_sharp0_whole1_BOTH_job`: sideways False, position 0.50, e 4.0; s 0.45/0.4499 (want 0.45), dest [0, 1080]x[1065, 1335], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `w4_2400x600_CENTRE_sharp1_whole0_BOTH_job`: sideways False, position 0.50, e 4.0; s 1.9996/2.0 (want 2.0), dest [0, 1080]x[600, 1800], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sharp ×2.00 (Fill ×4.00)`
- PASS `w4_2400x600_CENTRE_sharp1_whole1_BOTH_job`: sideways False, position 0.50, e 4.0; s 0.45/0.4499 (want 0.45), dest [0, 1080]x[1065, 1335], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `w4_2400x600_AUTO_sharp0_whole0_BOTH_job`: sideways True, position 1.00, e 1.8; s 1.8/1.8005 (want 1.8), dest [0, 1080]x[0, 2400], grid 29.0/29.0 phase 0.1/0.3 px; log `HOME+LOCK, auto 1.00, sideways, auto turn`
- PASS `w4_2400x600_AUTO_sharp0_whole1_BOTH_job`: sideways True, position 1.00, e 1.8; s 1.0/1.0 (want 1.0), dest [240, 840]x[0, 2400], grid 16.0/16.0 phase 0.0/0.0 px; log `HOME+LOCK, sideways, auto turn`
- PASS `w4_2400x600_AUTO_sharp1_whole0_BOTH_job`: sideways True, position 1.00, e 1.8; s 1.8/1.8005 (want 1.8), dest [0, 1080]x[0, 2400], grid 29.0/29.0 phase 0.1/0.3 px; log `HOME+LOCK, auto 1.00, sideways, auto turn`
- PASS `w4_2400x600_AUTO_sharp1_whole1_BOTH_job`: sideways True, position 1.00, e 1.8; s 1.0/1.0 (want 1.0), dest [240, 840]x[0, 2400], grid 16.0/16.0 phase 0.0/0.0 px; log `HOME+LOCK, sideways, auto turn`
- PASS `w4_1200x300_CENTRE_sharp0_whole0_BOTH_job`: sideways False, position 0.50, e 8.0; setStream, crop [533, 0, 668, 300]; log `HOME+LOCK`
- PASS `w4_1200x300_CENTRE_sharp0_whole1_BOTH_job`: sideways False, position 0.50, e 8.0; s 0.9/0.8998 (want 0.9), dest [0, 1080]x[1065, 1335], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `w4_1200x300_CENTRE_sharp1_whole0_BOTH_job`: sideways False, position 0.50, e 8.0; s 2.0002/1.9999 (want 2.0), dest [0, 1080]x[900, 1500], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sharp ×2.00 (Fill ×8.00)`
- PASS `w4_1200x300_CENTRE_sharp1_whole1_BOTH_job`: sideways False, position 0.50, e 8.0; s 0.9/0.8998 (want 0.9), dest [0, 1080]x[1065, 1335], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `w4_1200x300_AUTO_sharp0_whole0_BOTH_job`: sideways True, position 0.85, e 3.6; s 3.5998/3.6037 (want 3.6), dest [0, 1080]x[0, 2400], grid 58.0/58.0 phase 0.3/0.62 px; log `HOME+LOCK, auto 0.85, sideways, auto turn`
- PASS `w4_1200x300_AUTO_sharp0_whole1_BOTH_job`: sideways True, position 0.85, e 3.6; s 1.9999/2.0 (want 2.0), dest [240, 840]x[0, 2400], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sideways, auto turn`
- PASS `w4_1200x300_AUTO_sharp1_whole0_BOTH_job`: sideways True, position 0.50, e 3.6; s 1.9999/2.0 (want 2.0), dest [240, 840]x[0, 2400], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sideways, auto turn, sharp ×2.00 (Fill ×3.60)`
- PASS `w4_1200x300_AUTO_sharp1_whole1_BOTH_job`: sideways True, position 0.85, e 3.6; s 1.9999/2.0 (want 2.0), dest [240, 840]x[0, 2400], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sideways, auto turn`
- PASS `tiny_640x480_CENTRE_sharp0_whole0_BOTH_job`: sideways False, position 0.50, e 5.0; setStream, crop [212, 0, 428, 480]; log `HOME+LOCK`
- PASS `tiny_640x480_CENTRE_sharp0_whole1_BOTH_job`: sideways False, position 0.50, e 5.0; s 1.6877/1.6877 (want 1.6875), dest [0, 1080]x[795, 1605], grid 27.0/27.0 phase 0.16/0.16 px; log `HOME+LOCK`
- PASS `tiny_640x480_CENTRE_sharp1_whole0_BOTH_job`: sideways False, position 0.50, e 5.0; s 2.0002/2.0002 (want 2.0), dest [0, 1080]x[720, 1680], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sharp ×2.00 (Fill ×5.00)`
- PASS `tiny_640x480_CENTRE_sharp1_whole1_BOTH_job`: sideways False, position 0.50, e 5.0; s 1.6877/1.6877 (want 1.6875), dest [0, 1080]x[795, 1605], grid 27.0/27.0 phase 0.16/0.16 px; log `HOME+LOCK`
- PASS `tiny_640x480_AUTO_sharp0_whole0_BOTH_job`: sideways True, position 0.00, e 3.75; s 3.7505/3.7504 (want 3.75), dest [0, 1080]x[0, 2400], grid 60.5/60.5 phase 0.37/0.13 px; log `HOME+LOCK, auto 0.00, sideways, auto turn`
- PASS `tiny_640x480_AUTO_sharp0_whole1_BOTH_job`: sideways True, position 0.00, e 3.75; s 2.2502/2.2503 (want 2.25), dest [0, 1080]x[480, 1920], grid 36.0/36.0 phase 0.12/0.12 px; log `HOME+LOCK, sideways, auto turn`
- PASS `tiny_640x480_AUTO_sharp1_whole0_BOTH_job`: sideways True, position 0.50, e 3.75; s 2.0002/2.0003 (want 2.0), dest [60, 1020]x[560, 1840], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sideways, auto turn, sharp ×2.00 (Fill ×3.75)`
- PASS `tiny_640x480_AUTO_sharp1_whole1_BOTH_job`: sideways True, position 0.00, e 3.75; s 2.0002/2.0003 (want 2.0), dest [60, 1020]x[560, 1840], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sideways, auto turn, sharp ×2.00 (whole ×2.25)`
- PASS `huge_8000x6000_CENTRE_sharp0_whole0_BOTH_job`: sideways False, position 0.50, e 0.4; setStream, crop [2650, 0, 5350, 6000]; log `HOME+LOCK`
- PASS `huge_8000x6000_CENTRE_sharp0_whole1_BOTH_job`: sideways False, position 0.50, e 0.4; s 0.1351/0.135 (want 0.135), dest [0, 1080]x[795, 1605], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `huge_8000x6000_CENTRE_sharp1_whole0_BOTH_job`: sideways False, position 0.50, e 0.4; setStream, crop [2650, 0, 5350, 6000]; log `HOME+LOCK`
- PASS `huge_8000x6000_CENTRE_sharp1_whole1_BOTH_job`: sideways False, position 0.50, e 0.4; s 0.1351/0.135 (want 0.135), dest [0, 1080]x[795, 1605], grid -/- phase -/- px; log `HOME+LOCK`
- PASS `huge_8000x6000_AUTO_sharp0_whole0_BOTH_job`: sideways True, position 0.50, e 0.3; s 0.3/0.3001 (want 0.3), dest [0, 1080]x[0, 2400], grid -/- phase -/- px; log `HOME+LOCK, sideways, auto turn`
- PASS `huge_8000x6000_AUTO_sharp0_whole1_BOTH_job`: sideways True, position 0.50, e 0.3; s 0.18/0.18 (want 0.18), dest [0, 1080]x[480, 1920], grid -/- phase -/- px; log `HOME+LOCK, sideways, auto turn`
- PASS `huge_8000x6000_AUTO_sharp1_whole0_BOTH_job`: sideways True, position 0.50, e 0.3; s 0.3/0.3001 (want 0.3), dest [0, 1080]x[0, 2400], grid -/- phase -/- px; log `HOME+LOCK, sideways, auto turn`
- PASS `huge_8000x6000_AUTO_sharp1_whole1_BOTH_job`: sideways True, position 0.50, e 0.3; s 0.18/0.18 (want 0.18), dest [0, 1080]x[480, 1920], grid -/- phase -/- px; log `HOME+LOCK, sideways, auto turn`

### Screens, Set, the change time, parts, the pill, the up text

- PASS `sq_1150_AUTO_sharp1_whole0_HOME_job`: sideways False, position 1.00, e 2.087; s 2.0/2.0001 (want 2.0), dest [0, 1080]x[50, 2350], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME, auto 1.00, sharp ×2.00 (Fill ×2.09)`
- PASS `sq_1150_AUTO_sharp1_whole0_LOCK_job`: sideways False, position 1.00, e 2.087; s 2.0/2.0001 (want 2.0), dest [0, 1080]x[50, 2350], grid 32.0/32.0 phase 0.0/0.0 px; log `LOCK, auto 1.00, sharp ×2.00 (Fill ×2.09)`
- PASS `w32_1500x1022_AUTO_sharp1_whole0_HOME_job`: sideways True, position 0.00, e 1.6; s 1.5984/1.6001 (want 1.6), dest [0, 1080]x[0, 2400], grid 25.5/25.5 phase 0.1/0.1 px; log `HOME, auto 0.00, sideways, auto turn`
- PASS `w32_1500x1022_AUTO_sharp1_whole0_LOCK_job`: sideways True, position 0.00, e 1.6; s 1.5984/1.6001 (want 1.6), dest [0, 1080]x[0, 2400], grid 25.5/25.5 phase 0.1/0.1 px; log `LOCK, auto 0.00, sideways, auto turn`
- PASS `w32_1500x1022_CENTRE_sharp1_whole0_LOCK_job`: sideways False, position 0.50, e 2.3483; s 2.0005/1.998 (want 2.0), dest [0, 1080]x[178, 2222], grid 32.0/32.0 phase 0.0/0.0 px; log `LOCK, sharp ×2.00 (Fill ×2.35)`
- PASS `w4_1200x300_AUTO_sharp1_whole0_HOME_job`: sideways True, position 0.50, e 3.6; s 1.9999/2.0 (want 2.0), dest [240, 840]x[0, 2400], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME, sideways, auto turn, sharp ×2.00 (Fill ×3.60)`
- PASS `w4_1200x300_AUTO_sharp1_whole0_LOCK_job`: sideways True, position 0.50, e 3.6; s 1.9999/2.0 (want 2.0), dest [240, 840]x[0, 2400], grid 32.0/32.0 phase 0.0/0.0 px; log `LOCK, sideways, auto turn, sharp ×2.00 (Fill ×3.60)`
- PASS `tiny_640x480_CENTRE_sharp1_whole1_LOCK_job`: sideways False, position 0.50, e 5.0; s 1.6877/1.6877 (want 1.6875), dest [0, 1080]x[795, 1605], grid 27.0/27.0 phase 0.16/0.16 px; log `LOCK`
- PASS `tiny_640x480_AUTO_sharp1_whole0_HOME_job`: sideways True, position 0.50, e 3.75; s 2.0002/2.0003 (want 2.0), dest [60, 1020]x[560, 1840], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME, sideways, auto turn, sharp ×2.00 (Fill ×3.75)`
- PASS `w32_3600x2400_CENTRE_sharp0_whole0_HOME_job`: sideways False, position 0.50, e 1.0; setStream, crop [1260, 0, 2340, 2400]; log `HOME`
- PASS `w32_3600x2400_CENTRE_sharp0_whole0_LOCK_job`: sideways False, position 0.50, e 1.0; setStream, crop [1260, 0, 2340, 2400]; log `LOCK`
- PASS `huge_8000x6000_AUTO_sharp1_whole1_LOCK_job`: sideways True, position 0.50, e 0.3; s 0.18/0.18 (want 0.18), dest [0, 1080]x[480, 1920], grid -/- phase -/- px; log `LOCK, sideways, auto turn`
- PASS `w32_1500x1022_CENTRE_sharp1_whole0_BOTH_set`: sideways False, position 0.50, e 2.3483; s 2.0005/1.998 (want 2.0), dest [0, 1080]x[178, 2222], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sharp ×2.00 (Fill ×2.35)`
- PASS `w32_1500x1022_CENTRE_sharp0_whole0_BOTH_set`: sideways False, position 0.50, e 2.3483; setStream, crop [521, 0, 980, 1022]; log `HOME+LOCK`
- PASS `sq_1150_AUTO_sharp1_whole0_BOTH_set`: sideways False, position 1.00, e 2.087; s 2.0/2.0001 (want 2.0), dest [0, 1080]x[50, 2350], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, auto 1.00, sharp ×2.00 (Fill ×2.09)`
- PASS `tiny_640x480_AUTO_sharp1_whole1_BOTH_set`: sideways True, position 0.00, e 3.75; s 2.0002/2.0003 (want 2.0), dest [60, 1020]x[560, 1840], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sideways, auto turn, sharp ×2.00 (whole ×2.25)`
- PASS `w4_1200x300_AUTO_sharp1_whole0_BOTH_set`: sideways True, position 0.50, e 3.6; s 1.9999/2.0 (want 2.0), dest [240, 840]x[0, 2400], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sideways, auto turn, sharp ×2.00 (Fill ×3.60)`
- PASS `huge_8000x6000_CENTRE_sharp0_whole0_BOTH_set`: sideways False, position 0.50, e 0.4; setStream, crop [2650, 0, 5350, 6000]; log `HOME+LOCK`
- PASS `p920_2160x4800_CENTRE_sharp1_whole0_BOTH_set`: sideways False, position 0.50, e 0.5; setStream, crop [0, 0, 2160, 4800]; log `HOME+LOCK`
- PASS `w32_900x600_AUTO_sharp1_whole0_LOCK_set`: sideways True, position 0.50, e 2.6667; s 2.0/2.0 (want 2.0), dest [0, 1080]x[300, 2100], grid 32.0/32.0 phase 0.0/0.0 px; log `LOCK, sideways, auto turn, sharp ×2.00 (Fill ×2.67)`
- PASS `w32_900x600_CENTRE_sharp1_whole1_HOME_set`: sideways False, position 0.50, e 4.0; s 1.2/1.2 (want 1.2), dest [0, 1080]x[840, 1560], grid 19.0/19.0 phase 0.3/0.3 px; log `HOME`
- PASS `w32_1500x1022_CENTRE_sharp1_whole0_BOTH_job_gal_p0.0_pill-`: sideways False, position 0.00, e 2.3483; s 2.0002/1.998 (want 2.0), dest [0, 1080]x[178, 2222], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sharp ×2.00 (Fill ×2.35)`
- PASS `w32_1500x1022_CENTRE_sharp1_whole0_BOTH_job_gal_p1.0_pill-`: sideways False, position 1.00, e 2.3483; s 2.0002/1.998 (want 2.0), dest [0, 1080]x[178, 2222], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sharp ×2.00 (Fill ×2.35)`
- PASS `w32_1500x1022_AUTO_sharp1_whole0_BOTH_job_up_p-_pill-`: sideways False, position 1.00, e 2.3483; s 2.0002/1.998 (want 2.0), dest [0, 1080]x[178, 2222], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, auto 1.00, sharp ×2.00 (Fill ×2.35)`
- PASS `w4_2400x600_AUTO_sharp1_whole0_BOTH_job_up_p-_pill-`: sideways False, position 1.00, e 4.0; s 2.0/2.0 (want 2.0), dest [0, 1080]x[600, 1800], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, auto 1.00, sharp ×2.00 (Fill ×4.00)`
- PASS `w4_2400x600_AUTO_sharp0_whole0_BOTH_job_up_p-_pill-`: sideways False, position 1.00, e 4.0; setStream, crop [2130, 0, 2400, 600]; log `HOME+LOCK, auto 1.00`
- PASS `w32_900x600_AUTO_sharp1_whole0_BOTH_job_gal_p-_pill0`: sideways False, position 1.00, e 4.0; s 2.0001/2.0 (want 2.0), dest [0, 1080]x[600, 1800], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, auto 1.00, sharp ×2.00 (Fill ×4.00)`
- PASS `w32_900x600_CENTRE_sharp1_whole0_BOTH_job_gal_p0.0_pill1`: sideways True, position 0.00, e 2.6667; s 2.0/2.0 (want 2.0), dest [0, 1080]x[300, 2100], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sideways, sharp ×2.00 (Fill ×2.67)`
- PASS `w32_900x600_CENTRE_sharp1_whole0_BOTH_job_gal_p1.0_pill1`: sideways True, position 1.00, e 2.6667; s 2.0/2.0 (want 2.0), dest [0, 1080]x[300, 2100], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sideways, sharp ×2.00 (Fill ×2.67)`
- PASS `w32_900x600_AUTO_sharp1_whole0_BOTH_job_gal_p0.0_pill-`: sideways True, position 0.00, e 2.6667; s 2.0/2.0 (want 2.0), dest [0, 1080]x[300, 2100], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sideways, auto turn, sharp ×2.00 (Fill ×2.67)`
- PASS `w32_900x600_AUTO_sharp1_whole0_BOTH_job_gal_p1.0_pill-`: sideways True, position 1.00, e 2.6667; s 2.0/2.0 (want 2.0), dest [0, 1080]x[300, 2100], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sideways, auto turn, sharp ×2.00 (Fill ×2.67)`
- PASS `tiny_640x480_AUTO_sharp1_whole0_BOTH_job_gal_p0.0_pill-`: sideways True, position 0.00, e 3.75; s 2.0002/2.0003 (want 2.0), dest [60, 1020]x[560, 1840], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sideways, auto turn, sharp ×2.00 (Fill ×3.75)`
- PASS `sq_1150_CENTRE_sharp1_whole0_BOTH_job_gal_p1.0_pill-`: sideways False, position 1.00, e 2.087; s 2.0/2.0001 (want 2.0), dest [0, 1080]x[50, 2350], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sharp ×2.00 (Fill ×2.09)`
- PASS `w4_1200x300_CENTRE_sharp1_whole0_BOTH_job_gal_p0.0_pill1`: sideways True, position 0.00, e 3.6; s 1.9999/2.0 (want 2.0), dest [240, 840]x[0, 2400], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME+LOCK, sideways, sharp ×2.00 (Fill ×3.60)`
- PASS `w32_1500x1022_AUTO_sharp0_whole0_BOTH_job_gal_p0.0_pill-`: sideways True, position 0.00, e 1.6; s 1.5984/1.6001 (want 1.6), dest [0, 1080]x[0, 2400], grid 25.5/25.5 phase 0.1/0.1 px; log `HOME+LOCK, sideways, auto turn`
- PASS `w32_1500x1022_AUTO_sharp1_whole0_BOTH_turn`: sideways True, position 0.00, e 1.6; s 1.5984/1.6001 (want 1.6), dest [0, 1080]x[0, 2400], grid 25.5/25.5 phase 0.1/0.1 px; log `HOME+LOCK, auto 0.00, sideways, auto turn`
- PASS `tiny_640x480_CENTRE_sharp1_whole1_LOCK_turn`: sideways False, position 0.50, e 5.0; s 1.6877/1.6877 (want 1.6875), dest [0, 1080]x[795, 1605], grid 27.0/27.0 phase 0.16/0.16 px; log `LOCK`
- PASS `w32_3600x2400_CENTRE_sharp0_whole0_BOTH_turn`: sideways False, position 0.50, e 1.0; setStream, crop [1260, 0, 2340, 2400]; log `HOME+LOCK`
- PASS `w4_1200x300_AUTO_sharp1_whole0_HOME_turn`: sideways True, position 0.50, e 3.6; s 1.9999/2.0 (want 2.0), dest [240, 840]x[0, 2400], grid 32.0/32.0 phase 0.0/0.0 px; log `HOME, sideways, auto turn, sharp ×2.00 (Fill ×3.60)`

## Testing 3: the download on real URLs

**The probe on real URLs** (`sh4_download.py probe`): each a post as 0.8.0 stored it (today's
date, the rendition's URL, no `sizeChecked`, a 1200×1200 stand-in file), a page open (the check of
0.8.0's post; the fetch skipped as today's), then the log and the stored file's header:

- PASS `7nov_1500x1022`: want the original; stored [1200, 1200] → [1500, 1022]; sizeChecked True; log: `Refresh: APOD 2026-11-17 original 1500×1022 (smaller than the screen)`; `Refresh: APOD 2026-11-17 stored picture replaced by the original`
- PASS `pct20_1600x1067`: want the original; stored [1200, 1200] → [1600, 1067]; sizeChecked True; log: `Refresh: APOD 2026-11-17 original 1600×1067 (smaller than the screen)`; `Refresh: APOD 2026-11-17 stored picture replaced by the original`
- PASS `tiff_jupiter`: want the rendition; stored [1200, 1200] → [1200, 1200]; sizeChecked True; log: `Refresh: APOD 2026-11-17 probe failed: no size in the original's first bytes`
- PASS `big_4000x4000`: want the rendition; stored [1200, 1200] → [1200, 1200]; sizeChecked True; log: `Refresh: APOD 2026-11-17 rendition (original 4000×4000)`

- **The probe's 206:** every real host answers the original's `Range: bytes=0-65535` with 206
  (curl, 2026-11-17: `assets.science.nasa.gov/content/dam` for 7 Nov, the TIFF and the `%20` name;
  three `www.nasa.gov/wp-content/uploads` enclosures of today's feed). The app's four probes above
  went through it.
- **DIFFERENT: the probe's 200** can't be seen on a real URL: only `dynamicimage` ignores the
  range (200, the whole 135 KB rendition for 7 Nov), and the app never probes it (no probe where
  the rendition is the original). Covered by `:core`'s `ProbeTest.aServerIgnoringTheRangeIsReadOnlyThatFar`
  (a local server) only.
- **A new post's original** (the bootstrap, the real refresh of 17 Nov's APOD on 0.9.0):
  `Refresh: APOD 2026-11-17 original 960×1200 (smaller than the screen)`, `apod.jpg` 960×1200.
- **The update from 0.8.0** (a real one: v0.8.0's debug build from the tag, then dev's 0.9.0
  debug build with `install -r`; More NASA pictures on, home and lock APOD + Earth, the daily switch
  on). 0.8.0 stored Earth's thumbnail (`earth.imageUrl` `…/ISS075-E-250951_th.jpg/jcr:content/renditions/cq5dam.web.1280.1280.jpeg`,
  `earth.jpg` 720×480) and APOD's rendition (`apod.jpg` 1920×2400 of a 960×1200 original); an
  Earth ⛶ part (position 0.3) and ★'s notes faked for that URL. After the update
  (`MY_PACKAGE_REPLACED, arming again`) and one `run -f 1`: `Refresh: APOD 2026-11-17 original
  960×1200 (smaller than the screen)`, `… stored picture replaced by the original` (`apod.jpg`
  960×1200, no set: the screens keep the rendition until their next set), `Refresh: EARTH
  2026-11-17 original 2160×1440 (smaller than the screen)`, `Refresh: EARTH 2026-11-17 new
  picture` (`earth.imageUrl` the `dynamicimage` form without `_th`, `earth.jpg` 2160×1440; the part's
  and ★'s `cropUrl`/`savedUrl` still the old URL, so both reset: expected). A second run: `same
  post` for both, no probe (checked once). Before the re-download the subject was noted once more
  for the old thumbnail (0.8.0's note had no sharp positions): harmless, about 0.2 s.
