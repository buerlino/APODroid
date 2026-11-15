# 0.8.0: Sideways, a wide picture turned 90° (GitHub issue #4)

## The request

Issue #4 (Ludmuterol, 2026-11-09, "ability to rotate landscape images"): the 9 Nov 2026 APOD
(Rigel and the Witch Head Nebula) is landscape; Fill shows a portrait strip of it, "a fairly small
area of the total image, which makes the wallpaper low resolution". Asked: rotate landscape
pictures automatically, or let the user rotate the selection. "Since we are talking about images
of space here orientation is irrelevant anyway."

## What the pictures say (measured 2026-11-15)

- **9 Nov:** the original (`content/dam`) 3000×1990; the rendition the app downloads
  (`?h=2408`, the FP6's height) 3630×2408, upscaled by NASA's server. Fill's frame on a 1080×2408
  screen holds about 893×1990 of the original's pixels, drawn 1.21× larger: 30% of the picture,
  soft. Turned 90°, the frame holds about 1345×3000, drawn smaller: 67% of the picture, sharp.
- **The gain** for a picture a:1 wide (a = width ÷ height) is a² in both the share shown and the
  real pixels, while a ≤ the screen's height ÷ width (2.23 on the FP6); above that the turned
  picture is taller than the screen and the frame slides up and down.
- **253 APODs** (27 Dec 2025 to 1 Oct 2026, `private/pictures/`, 252 decodable): 117 at 1.2:1
  or wider (gain ≥ 1.44×), 88 at 1.4:1 or wider (≥ 2×), 65 at 1.5:1 or wider, 5 at 2:1 or wider.
  Portrait and square pictures lose when turned.
- **The stored rendition is enough** (by the maths, not measured on a phone): a landscape
  rendition H high is H·a ≥ H wide, so turned it is at least the screen's height and the app
  never draws it larger. No download change. `?w=` would fit a turned picture exactly but make
  the upright Fill, the page and ★'s fallback a× softer, and the turn is chosen after the
  download. NASA's images-assets.nasa.gov pictures (`?h=` ignored, 1920×1280) and APOD's 1280 px
  fallback get better turned too.
- **Not every picture is free of an up:** APODs with a horizon, the Moon, text; most of NASA's
  Image of the Day (people, rockets); Earth Observatory's maps (north up). 0.6.0's picks found no
  rule that tells them apart without ML (CLAUDE.md, 0.6.0: the picture).

## Decided (user, 2026-11-15)

1. **A first, then C; B only if A and C aren't good enough** (the user doubts it). A: a toggle
   per picture in ⛶ mode, as "Whole picture". C, later: a switch in Setup that makes the toggle
   default to on for wide pictures (the override per picture stays). B: automatic by a rule,
   no setting.
2. **The text** "Sideways", "✓ Sideways" while on (as "Whole picture").
3. **Clockwise only:** the picture's top goes to the screen's right. One Boolean.
4. **With Live too**, as "Whole picture": ⛶ decides what the screen shows; a sideways 3:2
   picture pans about a third of its width over the day.
5. **0.8.0** (0.7.1 stays for the FP6 week's fixes).

## How it fits (by reading, ed9148b, 2026-11-15; line numbers drift)

- **The name:** "turn" and "rotation" are taken (the sources' turns, `Rotation`, the EXIF
  `needsTurning`). The field, the pref and the notes are `sideways`.
- **`setStream` can't turn** (it takes a crop only; `Store.setFile`). A sideways set goes the
  darkened picture's way: the frame decoded alone at the screen's size, drawn into a bitmap of
  the screen's portrait size, `setBitmap`, the hint all of it. An upright picture keeps every
  path as it is, byte for byte (`setStream`, the EXIF photo's `setBitmap`, the whole picture).
- **The frame** in the upright picture's pixels (`:core`): with `F = wallpaperFrame(height,
  width, sw, sh, p)` in the turned picture's pixels, the band is `Frame(F.top, height − F.right,
  F.bottom, height − F.left)` for clockwise. So `decodeFrame`'s crop is a band of whole rows
  (horizontal), and position 0 is the turned picture's left edge, which is the original's
  bottom band. Every user of the position (Fill's set, the measure, Live's pan, B27's keep, ⛶'s
  frame) must agree on that: one `:core` function, tested against the invariant.
- **Every decode in `Drawing.kt` takes a `File` and decodes it upright** (`decodeFrame`,
  `frameGrey`, `pictureGrey`, `wholeGrey`, `wholePicture`/`drawWhole`, `blurred`): each gets
  the flag. Draw the decoded band through a rotating `Matrix` straight into the screen bitmap
  (no turned copy: about 22 MB peak as the darkened set, not 33). `hasBlackEdges` can stay
  upright (the strip is the same on all four sides, give or take rounding).
  `BitmapRegionDecoder` is no gain (no EXIF, powers of two only: jagged stars); keep
  `ImageDecoder`.
- **The darkening** measures what the screen shows: the turned frame's top third (Fill), the
  turned whole picture's (Whole), Live's turned picture's (`pictureGrey`, `wholeGrey`).
- **Live:** `copyForLive` notes `live.sideways` beside `live.whole` and `live.darken` in the
  hand-over's commit; the engine keys its picture by it, decodes at the screen's portrait
  height with the axes swapped (`liveDecodeSize` on (h, w)), keeps one turned copy (a turned
  landscape picture is at most H²·4 bytes, about 23 MB, within `LIVE_DECODE_BUDGET`) and pans
  it as now. B27's keep set (`Store`, the lock screen keeps Live's copy) honours
  `live.sideways`.
- **The part:** `Part` gets `sideways: Boolean = false`, the pref `<source>.sideways`
  (`sideways`, `nasa.sideways`, `earth.sideways`, as `whole`), keyed by `cropDate`/`cropUrl`
  as the part, so a new post drops it. `savePart`/`setPart` carry it through `set` and
  `copyForLive`. `setupChanged` and `orderAlone` are unaffected (parts aren't in `Setup`'s
  comparison by the draft; check `orderAlone`'s data-class equality covers the new field).
- **⛶ mode** (`MainActivity`): a pill "Sideways" at the picture's top right (the top left has
  "Whole picture"; two pills there crowd a narrow pane at large font sizes), only on a landscape
  picture (width > height), the chip's dark style, `tertiary` while it differs from the stored
  one, a switch for screen readers ("On"/"Off", ", not set yet"). On and Whole off: the frame
  is a horizontal band across the picture, dragged up and down (the drag's sign: p = 0 is the
  bottom band); the page's picture stays as published. On with Whole: the whole preview drawn
  sideways. With Live (the stored Size Live, Live on home, the source in home's column): the
  band at the clock's position, no drag, as now. ✓ saves and sets it with the part, Back drops
  it, a rotation drops it (closes ⛶), a Set from open crop mode stores it, reopening ⛶ shows
  the stored one. The saved state carries it (`toBundle`/`readDraft`).
- **The log:** ", sideways" in `set`'s note (the job's, the page's, Set's and `Part:` lines,
  and the hand-over's `wallpaper shown live` line), so the maps can check it from the log.
- **Debug dumps:** `files/debug-set.png` is written by every sideways set (the bitmap as set),
  `files/debug-whole.png` for a sideways whole picture.

## Packages

| WP | What | Model, effort | Who, device | After |
|----|------|---------------|-------------|-------|
| SW1 | `:core`: the sideways frame, tests | Sonnet, medium | winston, no device | — |
| SW2 | `:app`: the sideways set, the measure, Whole, Live, ⛶'s pill; the maps; `CLAUDE.md` | Opus, high | winston, tablet | SW1 |
| SW3 | Option C: the Setup switch, its texts placeholders | Opus, high | winston, tablet | SW2 (D1, VS1 before it in the night's order) |

The FP6's look (1080×2408, the reporter's screen) with the user on 17 Nov (`release-0.8.0.md`):
9 Nov's picture sideways on both screens, Fill and Live, and the switch.

### [ ] SW1. `:core`: the sideways frame (Sonnet, medium)

- `Frame.kt`: `sidewaysFrame(width, height, screenWidth, screenHeight, position): Frame`, the
  part of an upright `width`×`height` picture the screen shows when the picture is turned 90°
  clockwise, in the upright picture's pixels (How it fits, The frame). KDoc says which edge
  position 0 is. Optionally `sidewaysHelps(width, height) = width > height` (the pill's rule).
- **Tests** (`FrameTest`, its screen): 9 Nov's 3630×2408 at positions 0, 0.5 and 1 (the band's
  rows and that it spans the full width); the invariant: the band mapped into the turned
  picture equals `wallpaperFrame(height, width, …)` at the same position for a set of sizes and
  positions; a panorama wider than the screen's height ÷ width (the turned picture is taller
  than the screen, so the frame fills the turned picture's width and slides along its height:
  in the upright picture a strip of whole columns' height, moving left to right); a square
  picture; rounding never leaves the picture.
- **Done when:** the tests pass; `CLAUDE.md` names `sidewaysFrame` under The app, Choosing the
  part (one line, "built, the page in SW2").

### [ ] SW2. `:app`: the sideways set, the measure, Whole, Live, ⛶'s pill (Opus, high)

- **Store:** `Part.sideways`, its pref, `putPart` and `parts`; `set` → `setFile(…, sideways)`:
  the whole branch, the measure and the bitmap branch (`factor < 1f || sideways`) turned; the
  EXIF and `setStream` branches only upright. `copyForLive` measures turned and notes
  `live.sideways` in the hand-over's commit; a `liveSideways` getter; B27's keep set uses it.
  The log note ", sideways".
- **Drawing.kt:** the flag through `decodeFrame`, `frameGrey`, `pictureGrey`, `wholeGrey`,
  `wholePicture`/`drawWhole`, `blurred`; the band drawn through a rotating `Matrix` into the
  screen bitmap; `drawPanned` and `topThirdGrey` unchanged.
- **LiveWallpaper.kt:** the picture's key and decode take `live.sideways` (How it fits, Live).
- **MainActivity.kt:** the pill (How it fits, ⛶ mode), its state, `keepPart`, the no-change
  check of ✓ comparing `sideways` too, `startCrop`, the band in `shadeFrame` and `cropFrame`
  (and the Live branch), `WholePreview`'s keys, the saved state.
- **The text** "Sideways" / "✓ Sideways" is approved (user, 2026-11-15). No other new text.
- **Maps** (`interactions/`, the README's Keeping it current): `page.md` (⛶: the pill on a
  landscape picture and not on a portrait or square one; Sideways ✓ → "Wallpaper set", the log
  note, `debug-set.png` turned; off ✓ back upright; with Whole; Back and a rotation drop it;
  with Live the band at the clock's position, no drag; the chip's corner); `live.md` (⛶ with
  Live: `Part: wallpaper shown live … , sideways`, `Live: decoded W×H` portrait-shaped,
  `live.sideways` in the prefs grep); `background.md` (the hand-over notes `live.sideways`,
  B27's keep honours it; the job's set of a sideways part).
- **CLAUDE.md:** The app, Choosing the part (Sideways, as Whole picture's paragraph), Wallpaper
  (the sideways set, its log note, the dumps), Live (`live.sideways`); Next features: a short
  0.8.0 entry pointing here.
- **Tablet** (rosie; a landscape picture: today's or 9 Nov's faked into `files/` with the
  skill's recipe; a portrait one for the pill's absence):
  - ⛶ on the landscape picture: the pill at the top right; on the portrait one none;
  - Sideways ✓ with Size Fill: "Wallpaper set", the log's ", sideways", `debug-set.png` portrait
    with the picture turned clockwise (the original's top at the right), the band's drag moving
    it; both screens (one set, both flags) and the lock screen alone (APOD mode, Where Lock);
  - Sideways + Whole: the preview and the set turned, on its background;
  - a bright landscape picture sideways: darkened (the log's factor) and turned;
  - Sideways off ✓: upright again by `setStream` (no dump, no ", sideways");
  - Back and a rotation in ⛶ drop an unsaved Sideways; a Set from open crop mode stores it;
  - with Live on home: ✓ hands it over (`live.sideways=true`), the engine's decode
    portrait-shaped, the pan moving across the turned picture; a minute tick moves ⛶'s band;
  - the job's next run (`run -f 1`) sets it sideways again; a new post (faked date) drops it;
  - screenshots of each for the user; then the smoke list.
- **Done when:** `./gradlew :core:test :app:lintRelease :app:assembleRelease` passes, the tablet
  passes, the maps and `CLAUDE.md` updated.

### [ ] SW3. Option C: a Setup switch for wide pictures (Opus, high)

(Brought forward to the night of 15 Nov for the 17 Nov deadline, user, 2026-11-15; the texts
placeholders, the user approves them from rosie's screenshots on 16 Nov.)
- **The switch** in Setup after "Darken bright pictures", with a small ⓘ, part of the draft as
  "Darken bright pictures" (`Setup.sideways`, `saveSetup`, the saved state, the highlight, "On,
  not set yet"), always shown, never put back; the pref `wideSideways` (`sideways` is home's
  part's, SW2). Default off (an update from 0.7.0 changes nothing, no migration). Setup's
  summary doesn't name it. Set sets each screen afresh, so a change shows at once.
- **Placeholder texts:** the label "Wide pictures sideways"; the ⓘ "About Wide pictures
  sideways", its dialog "Turns a wide picture a quarter turn, so more of it fills the screen.
  ⛶ › Sideways changes one picture."
- **The rule** (`:core`, `sidewaysByDefault(width, height)`): width ≥ 1.5 × height (night
  default 3:2; the alternative 1.4:1, where the gain reaches 2×: 88 of 252 APODs instead of 65).
  Every source, "my picture" never (it has no part, Fill centred). Under Live too (night
  default, the user's recommendation pending: one rule, one sentence).
- **The part becomes three-valued:** `Part.sideways: Boolean?`, null the switch's default (an
  absent pref, read with `contains`; the bundle with three values). The effective value: the
  part's when set, else the rule while the stored switch is on, else upright. `set`,
  `copyForLive`, B27's keep and ⛶ use the effective one, read from the picture's size
  (`imageBounds`) where they need it. ⛶'s pill shows the effective value; ✓ notes an explicit
  on or off only when the pill was touched (else null stays, so a later switch change still
  applies).
- **Tablet:** the switch on and Set: a 3:2 picture sideways on both screens, a 1.2:1 one
  upright, a portrait one upright; the log's ", sideways"; ⛶ shows the pill on, off ✓ upright
  for that picture only, the next post sideways again; the switch off and Set: upright, an
  explicit ⛶ on stays sideways; with Live on home: the hand-over notes `live.sideways`; a
  rotation keeps the draft's switch; the highlight; screenshots of the switch, its ⓘ and the
  pill for the user; the smoke list.
- **Done when:** the tests (the rule, the three values) and the tablet pass; `CLAUDE.md` (The
  app, Settings: the switch; Choosing the part); the maps (`setup.md` a section as "Darken
  bright pictures"'s; `page.md`'s pill with the switch; `background.md` the job's set by the
  rule).
