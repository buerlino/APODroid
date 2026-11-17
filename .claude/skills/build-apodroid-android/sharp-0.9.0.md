# 0.9.0: Sharp pictures, Auto's turn, and Earth's picture link

(Read in full by every package of this file; the main checkout's copy is
`/home/nordmu/Documents/source99/APODroid/.claude/skills/build-apodroid-android/sharp-0.9.0.md`. The data it
names is in `/home/nordmu/Documents/source99/APODroid/private/`, readable from any worktree, never committed.)

## The request

A report (2026-11-16): the pictures often look a bit blurry (the 7 Nov APOD among them). The idea
with it: check the resolution of the portrait crop, and below a minimum load a picture from
another source instead.

**Decided** (user, 2026-11-16 and 17):
1. **No replacing:** "this is APODroid, it's about APOD and nothing else in principle." The
   sources stay APOD, NASA and Earth.
2. **Keep pictures sharp:** a switch in Setup, **default off**, label "Keep pictures sharp", ⓘ
   "A small picture is enlarged at most 2×; the rest of the screen shows it blurred." (approved).
   **L = 2×** with a **3% tolerance** (from the user's 40 picks, S0: 2× reproduced 29 of 38).
   It changes the size only, never the orientation.
3. **Auto turns by content** (2026-11-17, after the user's 300 labels, S1): Size Auto turns an
   APOD landscape picture sideways when NASA's text for it doesn't say it has an "up" (a
   horizon, ground, trees, people, Saturn, the Moon, text…): a **word table** in `:core`, the
   **cautious threshold** (never turned a picture the user wanted upright, 0 of 86 under
   leave-one-out; turned 147 of the 196 that are OK sideways). Centre never turns; Live never
   turns; ⛶'s Sideways pill still decides one picture. NASA's and Earth's pictures and "my
   picture" never turn by Auto.
4. **"Wide pictures sideways" goes:** Auto's turn replaces it (one mode, the user's wish).
5. **The order on a set:** first the orientation (⛶'s pill, else Auto's turn), then the
   position (⛶'s drag, else Auto's subject with "Centre when unsure"'s 0.2, else the middle),
   then the size (Fill, or with "Keep pictures sharp" at most 2× with blurred bands).
6. **Earth's picture link** (below) **fixed in 0.9.0.**
7. **The review's changes** (an independent check of the plan by an agent, 2026-11-16,
   approved): the Earth fallback only on a missing picture; the original downloaded when it's
   smaller than the screen; the tolerance; ✓ notes the orientation with a dragged position; the
   oracle by another agent, more cases in the table.

## What the pictures say (measured 2026-11-16; the review re-measured and confirmed them)

- **7 Nov ("De Gerlache-Kocher Massif"):** the original (`content/dam`) is 1500×1022. The app
  downloads `?h=2484` (the FP6's height): 3646×2484, **enlarged by NASA's server**. Fill's
  portrait frame holds 459×1022 of the original's pixels, drawn 2.43× larger.
- **Not the download:** there is nothing larger. apod.nasa.gov's image paths answer 301 to
  `science.nasa.gov/apod/`; `content/dam` is the largest copy. The server's JPEG renditions are
  quality 80 (`q=`, `quality=` ignored) and are enlarged past the original (`fit=max`,
  `fit=clip` ignored). A PNG's rendition stays PNG: 5 Nov's 982×575 PNG (549 KB) comes as a
  4242×2484 PNG of 6.5 MB. Quality 80 isn't the blur; the enlargement is.
- **How often** (294 APOD picture posts, 27 Dec 2025 to 16 Nov 2026, originals measured by
  their headers; `private/apod-original-sizes-2026-11-16.jsonl`): original height p10 812, p25
  1194, median 2000 px; 182 of 294 shorter than 2484 px. Fill's enlargement on the FP6
  (1116×2484):

  | Enlargement | Upright (0.8.0's Fill) | Sideways where landscape |
  |---|---|---|
  | ≤ 1× (sharp) | 38% | 47% |
  | 1–1.5× | 18% | 22% |
  | 1.5–2× (soft) | 16% | 11% |
  | > 2× (blurry) | 29% | 19% |

  On 1080×2400 about the same; on 720×1600 9% are over 2×.
- **NASA's feed** (60 items): median original height 3916 px, 8 of 60 over 1.5×. **Earth's**
  originals (39 of 40): median height 1253, 31 of 39 over 1.5×.
- **The rendition can't tell the original's size:** it is always as tall as the screen. The API
  has no size either (`featured_image` has none; the hero's `<img>` says `width="1280"
  height="1280"` for every post). A `Range` request on the original works (HTTP 206 on
  assets.science.nasa.gov and www.nasa.gov, not compressed; `dynamicimage` ignores `Range` and
  answers 200), and its first 64 KB held the size for 293 of 294 (a TIFF, `jupiter2026.tif`,
  has none; 14 Oct's "Jesličky" is 1500×1000 once its URL is encoded).

## Earth's picture link (a bug, since between 16 Oct and 16 Nov)

`featured_image_url` changed its form on all 40 newest Earth posts:
`…/content/dam/…/puncakjaya_oli2_20261019_th.jpg/jcr:content/renditions/cq5dam.web.1280.1280.jpeg`
(was `…/dynamicimage/assets/…/x_th.jpg?w=720&h=480&fit=clip&crop=faces%2Cfocalpoint`). The
`thumbnailSuffix` regex (`_th(\.\w+)$`, `Post.kt`) no longer matches, so `imageUrl` is the AEM
rendition of the **thumbnail** (at most 1280 px), which `renditionUrl` doesn't scale
(`content/dam` isn't a scaling host). The `_th` files against the real originals (8 newest):

| Post | The `_th` file | The original |
|---|---|---|
| puncakjaya_oli2_20261019 | 1440×960 (the app gets its 1280×853 rendition) | 1440×960 |
| burmamangroves_oli2_20260202 | 720×480 | 800×722 |
| navajoreservoir_oli2_20261107 | 720×480 | 2160×2187 |
| ISS075-E-188830 | 720×480 | 2160×1440 |
| akrotirisaltlake_oli_20261010 | 720×480 | 1880×1253 |

The thumbnail is also a 3:2 crop of a square picture (Navajo), and ★ saves it (`originalUrl`
keeps the rendition URL). One post (`ozone_geos5_20260918`) has no file without `_th` (HTTP 404
on `content/dam` and `dynamicimage`; its `_th` file's `?h=` answers 200). Before the change such
a post failed as `HttpStatus(404)` (not `badUrl`: a 404 isn't `SiteChanged`): the problem box's
"error 404" and a failed run, retried hourly all day. The API has no cleaner field
(`_fields=featured_image` returns nothing on `posts`). `private/eo-last40-2026-11-16.json`
holds the new form. `content/dam` → `dynamicimage/assets` with `_th` dropped gives exactly the
old form's `imageUrl` (the review: 19 overlapping posts, all 40 old forms).

## S1: what turns (the user's 300 labels, 2026-11-17)

- The last 300 landscape APODs (6 Jun 2025 to 16 Nov 2026), each shown upright and sideways on a
  phone screen; the question "would the sideways one be OK as your wallpaper, the way you hold
  your phone?" (https://claude.ai/code/artifact/6a1ed1b4-86a2-48a4-ba05-a690da7f7143, private).
  Answers: 196 OK sideways, 86 keep upright, 18 not sure (`private/up-labels-2026-11-17.json`;
  the posts' texts and sizes `private/apod-landscape-300-2026-11-16.json`).
- **A keyword rule failed:** v1, frozen before the labels (`private/up-rule-v1.py`), agreed on
  74% of the 282 sure answers, against 70% for "always sideways": NASA's alt text before about
  Dec 2025 is only "See Explanation. Clicking on the picture…", and words like "field" (of
  stars) matched.
- **A learned word table works:** Bernoulli naive Bayes on the distinct words of the title, the
  alt text (unless the placeholder) and the explanation's first 600 characters, all words, the
  threshold −2: under leave-one-out 0 of 86 wrongly turned, 147 of 196 turned (75%); other
  random splits the same. Without dates, seasons and places (the table's tokenizer drops them)
  it holds. Strongest words for "up": landscape, horizon, trees, ground, aurora, sunset,
  Saturn, peak, lake; for "OK sideways": NGC, galaxies, nebula, Hubble, supernova, spiral.
- Kept upright though OK sideways (the price of caution): eclipses, Mars rovers, Artemis II,
  the 7 Nov Moon massif.
- Built by `private/up-words-build.py` into `private/up-words-2026-11-17.tsv` (3915 words, 67
  KB); scripts `private/up-model-nb-*.py`.

## Terms

- **The file's size:** the stored picture's upright size (`uprightSize`). After SH2b it is the
  original's when the original is smaller than the screen, else the screen-tall rendition's.
- **e**, Fill's enlargement in the orientation used: `max(sw ÷ W, sh ÷ H)`, W×H the picture as
  it goes on (upright w×h, or turned h×w), sw×sh the screen's portrait size.
- **L = 2.0**, the limit; **the tolerance 1.03**: e ≤ 2.06 is plain Fill.
- **The limited layout:** the picture drawn at `s = min(e, L)` (s = e within the tolerance).
  Along an axis where it's larger than the screen, a frame slides as now (the position); along
  an axis where it's smaller, it's centred and the rest of the screen is the whole picture's
  background (black when `edgesBlack`, else `blurBackground`). With `s = e` it is exactly Fill
  (`wallpaperFrame`/`sidewaysFrame`, the whole screen).

## The rules

**Orientation** (`:core`'s `sidewaysOf`, replacing SW3's): ⛶'s pill when it noted one (true or
false, under every Size, as now); else **sideways when the stored Size is Auto, the source is
APOD, the picture is landscape (W > H by its upright size) and `hasUp(title, alt, explanation)`
is false**; else upright. The pref `wideSideways` is no longer read (left in the prefs, no
migration; an update from 0.8.0 with it on: Centre's pictures go upright again, Auto's by
content; the changelog says so).

**`hasUp`** (`:core`, `UpWords.kt`): the text is the title, then the alt text unless it starts
with "See Explanation" (case-insensitive), then the explanation's first 600 characters, joined
by spaces. The words: lower-cased runs of `a`–`z` (`[a-z]+` on the lower-cased text), longer
than 2 letters, without the stop words and the dropped words (both lists in the table builder,
`private/up-words-build.py`, copied exactly into `UpWords.kt`), each counted once. The score:
the table's prior (−0.817296) plus the weight of each distinct word in the table (unknown words
0). **Has an up when the score is above −2.0.** The table is a resource of `:core`
(`core/src/main/resources/io/github/buerlino/apodroid/core/up-words.tsv`, a copy of
`private/up-words-2026-11-17.tsv`: a `#` header line with the prior and the threshold, then
`word<TAB>weight`), read once, lazily. Expected scores (the builder's, ±1e-4): "Trees and a lake
below the horizon at sunset" 10.3718 (up); "A spiral galaxy and its nebulae in a field of stars"
−8.5074 (not); "Saturn and its rings" 2.1526 (up); "" −0.8173 (up: no text, stays upright);
"Zzzz qqq" −0.8173 (up).

**Position** (unchanged): ⛶'s dragged part's, else with Auto the subject's for the orientation
used (and for the limited frame's length when the limit bites, SH2c), with "Centre when unsure"'s
0.2, else the middle.

**Size** (`:core`'s `limitedLayout`): with "Keep pictures sharp" on (the stored `sharp`) and the
stored Size Centre or Auto, the limited layout at L; else Fill. The whole picture (⛶'s "Whole
picture") is limited too: its fit scale at most L (with the tolerance), centred, the rest its
background as now. "My picture": limited by its own file's size, never turned. Live: unchanged
(neither the limit nor Auto's turn; as SW3f1).

**✓ notes the orientation with a dragged position** (the review, FP1's kind): when ✓ notes a
position after a drag, it also notes `Part.sideways` as the orientation the drag was made in,
so a later change of Size or of NASA's text can't move the noted position to the other axis.

## The download and the size (SH2b)

- At a new picture's download in `refresh`, first a GET with `Range: bytes=0-65535` (then
  `0-524287` when the size isn't in the first 64 KB) of the original (`originalUrl`: APOD's and
  Earth's `content/dam`, NASA's enclosure), on its own path (`:core`'s `get()` throws on
  anything but 200: a probe takes 206 or 200, reads at most the asked bytes, then closes).
  `:core`'s `imageSize(bytes)`: JPEG (the first SOF0–SOF15 marker but DHT/JPG/DAC, after any APPn,
  skipping each segment by its length), PNG (IHDR), WebP (VP8, VP8L, VP8X), GIF; else null (TIFF,
  garbage, truncated).
- **When the original wouldn't cover the screen at 1:1** (`max(sw ÷ w, sh ÷ h) ≥ 1` for its w×h
  as read, upright), the original is downloaded instead of the rendition (as the rendition is:
  `.part`, decodable, then renamed); else the rendition as now. A failed probe: the rendition as
  now, logged (no limit can bite on a screen-tall rendition, so nothing is wrong). So the stored
  file carries its true size and the limit reads it from the file. Saves traffic too (5 Nov's
  549 KB PNG instead of a 6.5 MB enlarged PNG).
- **A post stored by 0.8.0** (a rendition): at the next refresh, page open or Set that sees it
  (the hooks T3f1 gave `noteSubject`), once per post (`<source>.sizeChecked` = the picture URL,
  committed first), the probe; when the original is smaller than the screen, it is downloaded
  and replaces the stored file (same post, same key; the subject noted again, since it was noted
  from the old file). A failure never fails the refresh.
- Logged: `Refresh: APOD 2026-11-07 original 1500×1022 (smaller than the screen)` or `…
  rendition (original 4000×3000)` or `… probe failed: …`.
- Traffic (CLAUDE.md, Traffic): one request of at most 64 KB (rarely 512 KB) per new picture per
  source; one re-download per source after the update.
- URLs as `download()` takes them (they may already hold `%20`; never encode twice).

## Testing (rigorous: the user's ask)

1. **`:core`, the rules as tables** (SH0 writes the oracle, SH1 the code):
   - `limitedLayout` and the limited fit against **an independent naive implementation written
     by SH0 from this file only** (before SH1 exists), over a generated table: orientation
     {upright, sideways} × shapes {portrait 2:3, square, 1.05:1, 1.47:1, 3:2, 4:1, tiny 640×480,
     huge 8000×6000} × e {0.5, 1, exactly L, L×1.03, just above, far above} × position {0, 0.5,
     1} × limit {null, 2.0} on 1116×2484 and 1080×2400; ±1 px.
   - **Properties** over ≥ 100,000 seeded random sizes and positions: the limit holds (s ≤ L ×
     1.03) whenever on; limit null is `wallpaperFrame`/`sidewaysFrame` exactly with the whole
     screen as dest; e within the tolerance is Fill exactly; the frame inside the picture; the dest
     inside the screen and centred on a banded axis; frame and dest of the same shape (± 1 px);
     a larger L never covers less; positions 0 and 1 reach the picture's edges.
   - `sidewaysOf`'s truth table: pill {true, false, none} × Size {Centre, Auto, Live} × source
     {APOD, NASA, Earth, my picture} × shape {portrait, square, landscape} × up {true, false} —
     each case against the rule above.
   - `hasUp`: the expected scores above; the tokenizer's edge cases (case, digits, apostrophes,
     the placeholder alt); with `private/` present (skipped without, as `readsEveryRealPost`):
     the 300 labelled posts score as the builder says (in-sample: 0 wrongly turned, 162 turned).
   - `imageSize`: made-up headers (a JPEG with a 70 KB APP1 before SOF, progressive SOF2, a
     DHT before the SOF, PNG, the three WebPs, GIF, truncated and garbage → null); the real
     originals in `private/` (skipped without).
   - Earth's parser: both link forms (made-up), every post in `eo-last40-2026-10-16.json` and
     `-11-16.json` gives a `dynamicimage` URL without `_th`, equal to the old form's.
2. **The pixels on the device** (SH4; the Android 14 emulator `emulator-5564`, the tablet when
   free): made-up test pictures (a checkerboard with a 1 px grid and four coloured corners) of the
   example sizes stored as APOD's post by faking the prefs (title, alt, explanation chosen so
   `hasUp` says up or not), then Set, then `files/debug-set.png` pulled and checked by a script:
   where the bands are, the grid's period (= s × the cell, ± 1 px: the scale is measured), which
   corner is where (the turn's direction), the frame's position. Each case × Centre and Auto ×
   the switch on and off × Whole on and off × Home, Lock, Both; the job's set (a faked change
   time) as well as Set's. A layout without a limit must take 0.8.0's paths (a plain Fill
   `setStream`, no `debug-set.png`).
3. **The download on real URLs** (SH4): the probe's 206 and 200, the TIFF, a name with `%20`,
   the original chosen when smaller than the screen; **the update from 0.8.0** with Earth's
   thumbnail stored (one re-download, `Refresh: … new picture`, ★ and ⛶'s part reset: expected)
   and an APOD rendition stored (checked once, the original then).
4. **SIM** (`RulesSimulationTest`) untouched: these change how a picture is drawn, not which.
5. **The interaction maps** (each package its own): `setup.md` (the switch, its draft and
   highlight, hidden under Live; the row "Wide pictures sideways" gone), `page.md` (⛶'s pill under
   Auto's turn, the limited frame, ✓'s orientation note, the whole preview), `background.md` (the
   probe, the original download, the check of 0.8.0's post, the log lines).
6. **The FP6 with the user** (after the night): 7 Nov's post faked, every combination, screenshots.

## Packages

| WP | What | Model, effort | Device | After |
|----|------|---------------|--------|-------|
| EF1 | Earth's picture link | sonnet, medium | emulator | — |
| SH0 | The oracle tests for `limitedLayout`, from this file only | opus, high | no | — |
| SH1 | `:core`: `imageSize`, `UpWords`/`hasUp`, the new `sidewaysOf`, `limitedLayout`, the tests | opus, high | no | SH0 |
| SH2a | Auto's turn in the app; "Wide pictures sideways" removed; ✓'s orientation note | opus, high | emulator | SH1 |
| SH2b | The download: the probe, the original when smaller, the check of 0.8.0's post | opus, high | emulator | SH1, EF1 |
| SH2c | Keep pictures sharp: the set, Auto's limited position, the switch, ⛶ | opus, high | emulator | SH2a, SH2b |
| SH4 | The device pass of Testing 2 and 3 (rosie) | opus, high | — | SH2c |

Rules for every package: `release-0.9.0.md`'s ("as 0.8.0's"): one package, one branch,
`CLAUDE.md` updated in the same package (the main checkout's `CLAUDE.md` has uncommitted 0.9.0
lines in "Next features" and "Other sources"; write your facts where they belong in your
worktree's copy), its map updated, no new dependency or permission, no text but those this file
gives. Read this whole file first.

### [ ] EF1. Earth's picture link (Sonnet, medium)

- `earthPosts`: drop the query, then a `/jcr:content/…` tail, then `_th` before the extension;
  `content/dam` → `dynamicimage/assets`, so `renditionUrl` scales it. Both forms parse, and give
  the old form's `imageUrl`.
- `downloadPicture`: for Earth, **only when the picture is missing** (HTTP 404 or 410, or not an
  image), the `_th` file (`:core`'s `earthThumbnail(url)`, its `?h=` rendition) before failing;
  never on a timeout, the network or another HTTP error (else a slow connection would store the
  thumbnail under the picture's key and never fetch the real one). The fallback is noted
  (`earth.thumbFallback`, the picture URL) and the real file is tried again at the next refresh
  while it's noted. `imageUrl` stays the key (parts, ★, the subject) whichever file came.
- `originalUrl` for Earth gives `content/dam` of the picture once the parser is fixed; ★'s
  fallback to the stored picture covers a 404.
- The update from 0.8.0: a stored thumbnail's `imageUrl` changes once (one re-download, the log's
  "new picture", ★ and ⛶'s part reset): expected, written in CLAUDE.md.
- **Device** (emulator): Earth's stored picture 2484 px high after a refresh; with the network
  off, nothing is stored and no thumbnail.
- **Done when:** the tests (Testing 1, Earth's parser) pass; CLAUDE.md's Other sources has the
  new form and the fallback (and drops "Fix: EF1").

### [ ] SH0. The oracle tests for `limitedLayout` (Opus, high)

Write **only tests**, from this file's Terms and Rules, before any implementation exists: a
naive reference implementation inside the test (plain loops and doubles, written for clarity,
not speed) and the generated table of Testing 1's first bullet, comparing it with `:core`'s
`limitedLayout` and `limitedFit`, which SH1 writes to these exact signatures (put them as stubs
that throw `NotImplementedError` in `core/src/main/kotlin/io/github/buerlino/apodroid/core/Limit.kt`,
so the tests compile and fail):

```kotlin
const val SHARP_LIMIT = 2.0
const val SHARP_TOLERANCE = 1.03
/** [frame]: the part of the upright picture shown, in its pixels; [dest]: where it's drawn on the screen. */
data class Layout(val frame: Frame, val dest: Frame, val sideways: Boolean, val scale: Double)
fun limitedLayout(width: Int, height: Int, screenWidth: Int, screenHeight: Int, sideways: Boolean, position: Float, limit: Double?): Layout
/** The whole picture's place on the screen ([fitFrame]) at a scale of at most [limit] (with the tolerance), centred. */
fun limitedFit(width: Int, height: Int, screenWidth: Int, screenHeight: Int, limit: Double?): Frame
```

Semantics to test: `limit` null, or e ≤ limit × 1.03: `frame` is `wallpaperFrame` (upright) or
`sidewaysFrame` (sideways) at `position`, `dest` the whole screen, `scale` e. Else `scale` =
limit; the oriented picture (W×H = w×h, or h×w sideways, its top to the screen's right) drawn
`W·s × H·s`; per axis: larger than the screen → the visible length `screen ÷ s` slides by
`position` (x: 0 the left edge; y: 0 the top, as `wallpaperFrame`) and the dest spans the
screen; else the whole length, the dest centred (`(screen − len·s) ÷ 2`, floor). The frame
mapped back to the upright picture as `sidewaysFrame` maps (position 0 the bottom band).
`limitedFit`: `fitFrame` when the fit scale ≤ limit × 1.03 (or limit null), else the picture at
`limit`, centred. Also the properties of Testing 1 (100,000 seeded cases, ±1 px). Tests only:
mark each test class `@Ignore("SH1 implements limitedLayout")`, so `./gradlew :core:test` passes
and the pipeline can merge; SH1 removes the `@Ignore`s. The reviewer approves when the tests
compile, run (and fail) against the stubs with the `@Ignore`s taken out locally, and are right by
this file.

### [ ] SH1. `:core`: the size, the words, the orientation, the layout (Opus, high)

- `Limit.kt`: `limitedLayout`, `limitedFit` to SH0's signatures; remove SH0's `@Ignore`s; its tests must pass
  unchanged (a test that's wrong by this file: fix it in a separate commit, say why).
- `imageSize(bytes: ByteArray): Pair<Int, Int>?` (The download) with its tests.
- `UpWords.kt`: the table resource (copy `private/up-words-2026-11-17.tsv`), the tokenizer,
  `upScore(title, alt, explanation): Double`, `hasUp(...)` (> −2.0); its tests (Testing 1).
- `sidewaysOf` by the new rule (its callers in `:app` keep compiling: SH2a wires the new
  arguments; until then pass what keeps 0.8.0's behaviour, documented).
- `Post.alt: String?` and its parsing for APOD (`featured_image.alt`, entities decoded, tags
  stripped; null for NASA and Earth), tested on made-up JSON.
- **Done when:** `./gradlew :core:test` passes; CLAUDE.md names the new functions where they
  belong (one line each; "the app in SH2a–c").

### [ ] SH2a. Auto's turn in the app (Opus, high)

- `Store`: `<source>.alt` written with the post (refresh stores a changed alt for the same date,
  as a changed title); `turnsSideways` by the new `sidewaysOf` (stored Size, source, upright
  size, `hasUp` of the stored title, alt, explanation); everywhere `wideSideways` was read
  (`set`, `copyForLive`, the page's `turnsSideways`, ⛶'s pill, B27's keep) by it.
- Setup: the row "Wide pictures sideways" and its ⓘ removed; `Setup.wideSideways`, its draft,
  highlight and saved-state line removed; `saveSetup` no longer writes it.
- ⛶'s pill opens on the value used (the stored Size's turn).
- ✓ notes `Part.sideways` with a dragged position (The rules).
- The log's set line keeps ", sideways"; Auto's turn adds ", auto turn" when Auto (not a pill)
  turned it.
- CLAUDE.md: Settings (the row gone, Auto's turn), Wallpaper (Auto, the subject; the sideways
  set), Choosing the part (Sideways' three values now: the pill's, else Auto's turn), Next
  features 0.9.0. The maps (Testing 5).
- **Device** (emulator): a faked APOD post 3000×1000 with a galaxy text goes sideways with Auto,
  upright with Centre; the same with a horizon text upright with both; ⛶'s pill overrides.

### [ ] SH2b. The download (Opus, high)

The section "The download and the size": the probe, the original when smaller than the screen,
the check of 0.8.0's post, the log lines, Traffic in CLAUDE.md, the maps.
- **Device** (emulator): APOD's 7 Nov post stored by a fresh refresh is 1500×1022 (`apod.jpg`);
  a big post stays screen-tall; a faked 0.8.0 state (the rendition, no `sizeChecked`) is checked
  once at a page open.

### [ ] SH2c. Keep pictures sharp (Opus, high)

- `Store.setFile`: the limited layout when it bites (the stored `sharp`, Size Centre or Auto,
  "my picture" too): a bitmap of the screen's size, the background (black when `edgesBlack`,
  else `blurBackground` of the picture's middle in the screen's shape, as `wholePicture`), the
  frame decoded alone (`decodeFrame`'s way, upright, at its dest size) drawn into `dest`, turned
  when sideways (`drawTurned`'s way), darkened by the measure of the drawn bitmap's top third (as
  the whole picture); `setBitmap`, the hint all of it. When it doesn't bite every path stays
  byte for byte (Fill's `setStream`, the EXIF photo, the darkened and sideways bitmaps, Whole).
  The whole picture with `limitedFit`.
- Auto's position for the limited frame: `noteSubject` also notes the positions for the limited
  frame's length (`subjectPositionSharp`, `subjectSidewaysSharp`: `subjectPosition` with the
  limited frame's shape as its "screen"; 0.5 when no axis moves) when the limit would bite for
  the file; a post noted by 0.8.0 is noted again once. ⛶'s dragged position applies as it is.
- The log: ", sharp ×2.00 (Fill ×2.43)" after ", sideways" and before ", darker …".
- Setup: the switch "Keep pictures sharp" with its ⓘ ("About Keep pictures sharp"; the dialog
  text of Decided 2) after "Darken bright pictures", the pref `sharp`, default off; part of the
  draft as "Darken bright pictures" (`Setup.sharp`, `saveSetup`, the saved state, the highlight,
  "On, not set yet"); shown with the daily switch off too; **hidden while the draft's Size is
  Live** (back to the stored value, as UI1 did for sideways). Setup's summary doesn't name it.
- ⛶: the frame shown is the limited layout's frame for the **stored** setup (as ⛶ reads the
  stored Size); along an axis that doesn't move, no drag; the whole preview by `limitedFit`.
- CLAUDE.md (Settings, Wallpaper: the limited set, Choosing the part, Next features), the maps.
- **Device** (emulator): Testing 2's cases for one example each way (the full table is SH4's).

### [ ] EF1f1. Earth's real picture replaces a stored thumbnail on the screens too (Sonnet, medium)

From Opus pass 50 (2026-11-17): when the real picture finally replaces a stored thumbnail
(`earth.thumbFallback` noted, the next refresh gets the real file), the URL stays the key, so the
screens keep the thumbnail until the next post, and the subject noted from the thumbnail (a 3:2
crop) stays too. On that swap, forget the post's subject note and the records of the screens
showing it (as Set forgets a record: the picture dropped, the owner and stamp kept), so the next
run puts the real picture on at its own subject. Logged `Refresh: EARTH <date> real picture
instead of the thumbnail`. A unit test where the rule can be in `:core`; device (emulator): a
faked `thumbFallback` with the thumbnail on the lock screen, a refresh, the real picture set.

### [ ] SH4. The device pass (rosie; Opus, high)

Testing 2 and 3 on `emulator-5564` (and the tablet if attached and free): write the pixel-check
script under `scripts/` (Python 3 stdlib only, reads a PNG; no PIL), the made-up pictures, the
faked prefs; every case, a PASS / FAIL / DIFFERENT line each with the `debug-set.png` pulled.
A FAIL becomes a `### [ ] SH4f<n>` package in this file, with the case and the log.

## Open

None (S0's and S1's questions decided). After SH4: the FP6 with the user (Testing 6), then the
release (`release-0.9.0.md`, R4, T4).
