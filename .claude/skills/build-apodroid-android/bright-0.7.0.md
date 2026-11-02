# Bright pictures darker (0.7.0)

(User, 2026-11-02: GitHub issue #2, "in 0.7.0". Planned by reading and measuring the same day,
no code, no device. Nothing is decided beyond the release: the questions under [Open](#open) are
the user's, asked one at a time with a recommendation. The packages write their decisions into
`CLAUDE.md` when they land; this file is the how. A third track beside `on-time-0.7.0.md` and
`live-0.7.0.md`, much smaller; [Seams](#seams-with-tracks-o-and-l) says where it meets them.)

## The request

Issue #2 (2026-11-02, the same user as #1, whose idea became 0.5.0's sources): bright pictures
("daytime landscapes, bright phenomena, as well as the recent test pilot from NASA") wash out
home-screen widgets; their screenshot (1080×2424) shows the clock barely visible against a blue
sky. Asked for: "a simple luminance check + modest ColorMatrix darkening when the image is
brighter than a target", "leaving already-dark astronomy images untouched". Muzei had a setting
that kept every picture's brightness lower (about 90%), "always a compromise"; this one should be
dynamic, with Android's own classes (`ColorMatrix`), "to stay lightweight". The issue's title says
"Optional".

The user's reply on the issue: "v0.7.0 I will add your suggestion".

**It revisits a decision:** 2026-10-22's "Not wanted: … a dim setting" (`CLAUDE.md`, 0.6.0: the
picture). That was Muzei's kind, every picture dimmed; this one leaves dark pictures as they are.

## What the pictures say (measured 2026-11-02)

The measure: the part of the picture the screen shows (Fill's frame: the middle in the FP6's
1116×2484 shape), shrunk to 90×200, its mean grey 0–255 by `gray`'s weights (`Background.kt`).
The script: `bright/measure.py` in that day's scratchpad (gone with it; a few lines of Pillow).

**APOD** (the 253 of `private/pictures/`, 27 Dec 2025 to 1 Oct 2026, `?h=640`):
- Median 68; p75 88; p90 114; p95 130; p99 176. Astronomy is dark, as the issue says.
- Over 100: 46 (18%); over 110: 31 (12%); over 120: 21 (8%); over 140: 9 (4%); over 160: 5.
- The brightest: "Cosmic Latte: The Average Color of the Universe" (28 Sep, one flat colour, 246),
  "Robert Goddard and Nell" (28 Mar, 192), "Flying Saucer Crash Lands in Utah Desert" (19 Jul,
  189), "Io in True Color" (20 Jan, 165), "B93: A Dark Interstellar Ghost" (20 Feb, 161), the Sun
  (6 Aug, 149), "AR 4478: Giant Sunspot Group" (28 Jun, 147), "NGC 1514: The Crystal Ball Nebula"
  (28 May, 146).
- The top third (where the lock screen's clock and most widgets sit) can differ from the whole:
  "Chance Triple Alignment: Plane, Space Station, Sun" (22 Sep) is 138 overall, 58 at the top;
  "Buck Moon and Belt of Venus" (1 Aug) 130 overall, 162 at the top. Over 120 at the top: 24.

**NASA and Earth are much brighter** (fetched 2026-11-02 into the scratchpad, not kept: NASA's
feed, 56 of its 60 items at `?h=640`, 6 Aug to 30 Oct 2026; Earth's 40 newest posts'
`featured_image_url`, 720 or 1440 px):
- **NASA:** median 90, p75 130, p90 152. Over 110: 23 (41%); over 120: 18 (32%); over 140: 12.
  Launches, landings, engine tests, aircraft in daylight: "Crew-12 Returns to Earth" 198, "NASA's
  Nancy Grace Roman Space Telescope Launches" 192, "RS-25 Engine Tests at NASA Stennis" 181.
- **Earth:** median 128, p75 156, p90 191. Over 110: 25 (62%); over 120: 23 (57%); over 140: 14.
  Ice, clouds, storms and maps: "The Human Fingerprint on Earth's Water Cycle" 243, "Arctic Melt
  Season Length Levels Off" 237, "Antarctic Sea Ice Grows to Its 2026 Maximum" 220.
- **The issue's picture** is NASA's "NASA's X-59 Aircraft and Test Pilot" (26 Oct 2026): 130
  over the whole frame, **163 in the top third** (the blue sky behind the clock). Darkened to a
  target of 110 by the whole frame it gets ×0.85, by the top third ×0.67.

## How it fits (by reading `Store.kt`, 2026-11-02)

**Today's set paths** (`Store.set`, every set: the job's, the page's, Set's, ⛶'s ✓):
1. Fill: `setStream(file, crop)`. Android decodes the rendition and keeps the frame as the hint;
   the app never has the pixels.
2. A photo with an EXIF orientation ("my picture"): `decodeUpright`, then `setBitmap` with the
   frame as the hint.
3. ⛶'s whole picture: `whole()`, a bitmap of the screen's size (on black or the blurred
   background, which is already 35% darker), then `setBitmap`.

**What it changes:**
- **The measure** is of what the screen will show: Fill's frame from a small copy (only the
  frame's pixels, e.g. `BitmapRegionDecoder` with a sample size, or the copy `hasBlackEdges`
  already decodes, cut to the frame); the whole picture's bitmap as `whole()` made it, background
  included.
- **The factor** comes from `:core` (testable): 1 up to the target, else target ÷ mean, never
  below a floor ("modest"). Figures from BR0; placeholders: target 110, floor 0.6 (at most 40%
  darker).
- **A dark picture keeps its path**, byte for byte what 0.6.0 sets ("untouched").
- **A bright picture goes through a bitmap:** Fill's frame decoded at the screen's size (the
  rendition is the screen's height, so the frame is about the screen's pixels: about 11 MB on the
  FP6, as the whole picture's bitmap), drawn with a `Paint` holding
  `ColorMatrixColorFilter(ColorMatrix().apply { setScale(k, k, k, 1f) })`, then `setBitmap` with
  all of it as the hint. Paths 2 and 3 draw their bitmap through the same `Paint`. One linear
  scale of R, G and B: the hues stay, black stays black.
- **Not changed:** the rules, the records, the turns, the job's timing; the page shows the picture
  as it is (it's the picture, not the wallpaper); ★ and "Save every picture" save the originals.
- **Taking effect:** at each set. A changed setting (if there is one) through Set, which sets each
  screen afresh.
- **Logged** with the set: `Daily job: wallpaper set: … on HOME+LOCK, darker ×0.78 (grey 141)`.
- **Wallpaper colours:** Android computes them from what's set, so a darkened day gives a darker
  palette (unchecked: the tablet and the FP6 have wallpaper colours off, U1).
- **Android's own dimming** isn't open to apps: `WallpaperManager.setWallpaperDimAmount` is a
  system API (not in API 37's public stubs, checked 2026-11-02).

**With Live** (track L): the engine draws the screen's copy each minute at a moving frame. One
factor per picture, from the whole picture (measured once when the copy is written, noted beside
it as `live.<screen>.whole` is), so the brightness doesn't change as the frame moves; L4's drawing
function takes it as a `Paint`, so the engine, the still whole picture and L7's preview darken
alike.

## Open

For the user, one at a time, each with a recommendation:
1. **A switch or automatic.** Recommended: a switch in Setup, "Darken bright pictures"
   (placeholder), default on, part of the draft (Set sets the screens afresh with it). Why a
   switch: users would really disagree (the picture as NASA made it against a readable clock), and
   the issue asks for it optional. Why on: most people never open Setup, and the darkening is
   modest; iOS dims the wallpaper in dark mode by default. Against on: at a target of 110 it
   touches 1 APOD in 8, but 4 of NASA's pictures in 10 and 6 of Earth's (white ice and clouds turn
   grey at the floor). Others: automatic with no switch (as the background's edges, 2026-10-23); a
   switch default off (0.6.0's updaters see no change).
2. **The target and the floor:** from BR0's picking page with the user.
3. **What's measured:** the whole frame (one sentence, "brighter than X") or its top third (where
   the clock sits on most lock screens; widgets can be anywhere). The issue's own picture argues
   for the top third: its sky is 163 there, 130 over the whole frame, so the whole frame darkens
   it by only 15%. BR0 shows both.
4. **"My picture":** the same rule (recommended: one rule for every picture) or as it is (the
   user's own choice).
5. **The whole picture:** measured with its background (recommended: that's what the screen
   shows; the blur is already 35% darker).

## Packages (draft)

After O1–O3 (the declutter every track waits for). BR2 changes `Store.set`, which L4 moves into
`Drawing.kt`: BR2 before L4, so L4's pixel-identical check carries a darkened picture too (or BR2
after L4, on `Drawing.kt`). One tablet: BR2's check one session at a time with the O and L checks.

| WP | What | Model, effort | Who, device | After |
|----|------|---------------|-------------|-------|
| BR0 | The picking page: the target, the floor and the measure with the user | Opus, medium | a day session, **the user** | — |
| BR1 | `:core`: the measure and the factor, tests | Sonnet, medium | winston, no device | BR0, Open 1–5 |
| BR2 | `Store.set`: a bright picture through a darkened bitmap; the switch in Setup if any | Opus, high | winston, tablet | BR1, O1–O3; before L4 |
| BR3 | Live: the engine and the preview darken by the picture's factor | Opus, medium | winston, tablet | BR2, L5 (or folded into L5 and L7) |

The FP6's look goes into L10 or O17's phone pass with the user.

### [ ] BR0. The picking page (by day, with the user; Opus, medium)

A2's way (`plan-0.6.0.md`, A2): an artifact page with a database for the picks, private.
- **The pictures:** the 40 brightest frames of the 253 APODs and of NASA's and Earth's recent
  pictures, and 10 near the line (grey 90–110) to see where it should start.
- **Each picture** in the FP6's shape with a white lock-screen clock and a row of white widget
  labels drawn over it, at five strengths: as is, and darkened to targets 140, 120, 100 and 80
  (the floor 0.6 shown where it stops one).
- **The user picks** the lightest one where the clock reads well and the picture still looks
  right, or "as is".
- **Decides:** the target (the picks' median), the floor (the strongest darkening picked), the
  measure (whole frame or top third: which predicts the picks better). Written into this file;
  then Open 1, 4 and 5 asked.
- **Done when:** the figures and the user's answers in this file; the page's link noted here.

### [ ] BR1. `:core`: the measure and the factor (Sonnet, medium)

- `Brightness.kt`: the mean grey of a grey copy (with `gray`), over all of it or its top third as
  BR0 decided; `darkening(mean): Float` (1 up to the target, else target ÷ mean, at least the
  floor); the constants with their reason, as `Background.kt`'s.
- **Tests:** a dark picture 1; at the target 1; just over it; Cosmic Latte's flat 246 (the
  floor); an even picture stays even; the top third if chosen.
- **Done when:** the tests pass; `CLAUDE.md` (0.7.0: bright pictures darker: the figures) and the
  skill's "Where things are".

### [ ] BR2. `Store.set`: a bright picture through a darkened bitmap (Opus, high)

- Each set measures what the screen will show; with a factor below 1: Fill's frame decoded at the
  screen's size and `setBitmap`; the EXIF photo and the whole picture drawn through the darkening
  `Paint`. A factor of 1 leaves every path as it is.
- The switch, if the user wants one: the pref (name winston's), `Setup`, `saveSetup`, the draft,
  its saved state and the highlight, as "Where"; placed after Size (L6) or, before L6, after "If
  APOD is a video". The text a placeholder until the user approves it from a screenshot.
- The log line with the factor.
- **Files:** `Store.kt`, `MainActivity.kt` (the switch), `CLAUDE.md` (The app, Wallpaper: the
  darkening; Settings, if a switch), the skill.
- **A debug dump** as L4's: in a debuggable build `Store.set` writes the bitmap it sets to
  `files/debug-set.png`, so a check can compare pixels, not screenshots.
- **Tablet:** a bright picture (Cosmic Latte or a sunlit landscape faked into `files/` with the
  skill's recipe) and a dark one, each on both screens by `run -f 1` and by Set:
  - the bright one darkened (the log's factor, `debug-set.png`'s mean grey at the target), Fill at
    the stored part and in the middle, whole on blur;
  - the dark one by `setStream` as before (no `darker` in the log);
  - ⛶'s ✓ on the bright one sets it darkened at the new part;
  - the switch off (if any): Set puts the bright one on as it is;
  - "my picture" bright, as Open 4 decided;
  - screenshots of each for the user; then the smoke list.
- **Done when:** the tablet passes; the texts listed.

### [ ] BR3. Live: the engine and the preview darken by the picture's factor (Opus, medium)

(After L5; small enough to fold into L5 and L7 if those aren't built yet: then this row goes.)
- The factor measured once per picture on the whole picture when `setDue` writes the screen's
  copy, noted beside it; the engine and L7's preview pass it to L4's drawing function.
- **Tablet:** Live on with a bright picture: the frame at 06:00, 12:00 and 23:30 (the clock
  stepped forward, the skill's how) equally darkened; a dark one untouched.
- **Done when:** the tablet passes.

## Seams with tracks O and L

- **`Store.set`** is changed by BR2, by L4 (the drawing moves to `Drawing.kt`) and by L5
  (`setDue` hands live screens to the engine): BR2 first, then L4 and L5 build on it.
- **L4's check** (the whole picture pixel-identical before and after the move) gains a darkened
  whole picture.
- **L6's Size row** and BR2's switch, if any, sit together in Setup.
- **L7's preview** shows the darkening (BR3), so ⛶ mode shows what the screen will.
- Track O is untouched: the darkening is at set time, the job's runs and timing don't change.
