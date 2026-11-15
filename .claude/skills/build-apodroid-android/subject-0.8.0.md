# Fill's frame on the subject (0.8.0)

(User, 2026-11-02: GitHub issue #3, "for 0.8.0". Planned by reading the same day, no code, no
device. Nothing is decided beyond the release: the questions under [Open](#open) are the user's,
asked one at a time with a recommendation, after SA0 has shown whether it's worth building. The
packages write their decisions into `CLAUDE.md` when they land; this file is the how.)

## The request

Issue #3 (2026-11-02, the same user as #1 and #2): "Optionally run a tiny on-device saliency /
content-aware model once when preparing the wallpaper, then use the highest-attention region as
the default crop window (still editable with the existing frame tool)." Named:
[Glimpse](https://github.com/the-super-toys/glimpse-android) ("a very small model (~148 KB) plus
TFLite", "total added size still well under 1 MB") and UniSal-style LiteRT models (~6.5 MB fp16).
Its terms: off by default; once per picture, when the wallpaper is prepared; the middle when the
model is missing or fails; as small as practical; easy to override, as astronomy pictures can
trick general saliency models. "Not a must-have."

The user's reply on the issue: centring on the highest brightness was tried for an auto crop, and
on average the centre crop was still better; "I will see if I can add it for v0.8.0".

## What's known (2026-11-02)

**It's 0.6.0's question again, with a learned map.** `CLAUDE.md`, Next features, 0.6.0: the
picture:
- The smart Auto (planned 2026-10-22, dropped 2026-10-23) had a hand-made map of what matters
  (brightness smoothed so one star doesn't win, detail, colour, a pull to the centre; `:core`, no
  library) deciding Fill, a zoom or Fit, and where the frame goes. Request 2 is its last part only:
  where Fill's frame goes. Fill or whole stays the user's per picture (⛶'s "Whole picture").
- Fill's part [Middle | Brightest area] was dropped the same night (`brightestPart` never built).
- **The user's picks** (A2, 2026-10-22; the picking page
  https://claude.ai/artifact/EHeAzSEZHyvBd9qyo7RzA8, private, the picks in its database; the
  pictures in `private/pictures/`, 253 APODs of 27 Dec 2025 to 1 Oct 2026 at `?h=640`): of 148
  clear picks, Fill 133 (middle 81, brightest area 52), Fit 15. Middle against brightest area: no
  pattern near the centre (28 to 21); a brightest area at the picture's edge was mostly the wrong
  subject (middle 22 to 6). That's the bar: a saliency frame must beat the middle on these picks
  before anything is built.
- How much the frame's place matters: on a 1080×2400 screen Fill keeps 36% of the width for the
  median picture, 21% at the widest (0.6.0's mock-up of 89 posts). None was taller than the
  screen's shape, so the frame moves left–right only.

**Glimpse** (its GitHub repo read through the API, 2026-11-02):
- Apache-2.0; created Feb 2019, last push 2 Feb 2020 (unmaintained); 589 stars.
- The model: `glimpse-core/src/main/assets/model.tflite`, 147,824 bytes. Its architecture and
  training data aren't documented in the README (to read from the file: SA0).
- Depends on `org.tensorflow:tensorflow-lite:0.0.0-nightly`; published on JitPack; the README
  recommends `abiFilters 'armeabi-v7a', 'arm64-v8a'` because of TFLite's native libraries.

**The size is the runtime, not the model.** The 148 KB is the model; TensorFlow Lite (now
LiteRT) is a native library per ABI on top of 0.6.0's 2.2 MB APK. The issue's "well under 1 MB"
counts the model only. Unmeasured: SA1 measures the APK with it.

**What it touches in `CLAUDE.md`'s rules:**
- Stack: few libraries, "before any other dependency, check whether the JDK or Android already
  covers it", ask the user first. No proprietary dependencies: TFLite/LiteRT itself is Apache-2.0,
  but the Play Services build of it and ML Kit are out.
- F-Droid builds the app from source: a prebuilt native library from Maven (LiteRT is on Google's
  Maven) may or may not pass its scanner (unchecked: SA1).
- Reproducible builds: a prebuilt `.so` is the same bytes each time (likely fine, unchecked).
- Android has no on-device model runtime for apps without a library (NNAPI is deprecated since
  Android 15 and needs a model compiled for it anyway).

**Ways without a library** (a simpler idea, for the user to weigh against the issue's):
1. **Glimpse's model run by hand-written inference in `:core`.** If the model is a small
   convolutional net (148 KB is about 37,000 float weights), its layers are a few hundred lines of
   plain Kotlin, run once per picture on a small copy, tested on the JVM against TFLite's own
   output on the desktop. No native code, no new dependency; the model file goes in the APK's
   assets (its licence and training data checked first).
2. **A saliency method without a model**, e.g. the spectral residual (Hou and Zhang, CVPR 2007):
   an FFT of a 64×64 grey copy, a few dozen lines in `:core`. Built for natural scenes; on star
   fields untested.
Both stay inside "no library". SA0 tests them beside the model itself.

## How it would fit (by reading, 2026-11-02)

- **The position only.** ⛶'s part is a position 0–1 along the one direction the frame moves
  (`wallpaperFrame`, `Part.position`, looked up by `parts.forPost(post)` since O2); 0.5 is the
  middle. The subject's frame is a default position per post, nothing else: the user's part when
  ⛶ moved it (`forPost`), else the subject's position, else 0.5.
- **Once per post**, when the picture is stored (`Store.refresh`, after the download): the map
  from a small copy, the position from the map (`:core`: the frame's place whose sum of the map is
  the largest, maybe with a pull to the middle, which SA0 tunes), noted per source beside the
  part (`<source>.subjectPosition`, keyed by the post's date and picture URL, as `cropDate` and
  `cropUrl`). A failure or no map: nothing noted, the middle. The job and the page only read it.
- **⛶** opens at that position; ✓ saves it as the user's part, as now.
- **Live** (0.7.0) pans across the whole width by the clock: the subject's position doesn't apply
  there. The whole picture has no frame.
- **"My picture"** is Fill, centred, by decision (0.6.0); whether the subject's frame applies to
  it is a question.
- **The page** shows the picture whole, as now; the chip and texts don't change.
- **The rules, the records, the job:** unchanged.

## Open

For the user, after SA0's numbers (recommendations then):
1. Build it at all: only if SA0's frames beat the middle on the user's picks.
2. Which method: the issue's model with TFLite/LiteRT (a native library, its size, F-Droid's
   view), the same model by hand-written inference, or no model.
3. A setting (the issue: off by default; "a setting only when users would really disagree and no
   default can serve both", one sentence) or the new default with no setting when it clearly wins.
4. Whether "my picture" gets it too.
5. The issue's joke: "powered by AI" in the store listing.

## For the 17 Nov deadline (2026-11-15 evening)

The user: everything planned for 0.8.0 starts now, done by 17 Nov (`release-0.8.0.md`). So:
- **SA0 tonight by an agent** on the desktop (no device): the methods' frames against A2's 152
  picks (read from the picking page's database 2026-11-15 into the session's scratchpad), and
  the new picking page ready for the user on the morning of 16 Nov.
- **The re-run of the 152, combined with SA0** (user, 2026-11-15: "another run on the 152 to
  confirm they are accurate"; combined, the user's choice): A2's page kept only the picks, its
  images are gone and its "brightest area" rule wasn't written down, so the frames are rebuilt
  (the brightest area approximated: the frame position with the largest sum of a smoothed
  brightness). The same 152 pictures in a new random order, the old picks hidden, **three
  Fill frames ranked 1, 2, 3** (user, 2026-11-15: "i would need to say 1,2,3 so that we know if
  auto is still better than center"): the middle, the brightest area and "auto" (the one
  shippable method SA0 judges best), unlabelled and shuffled per picture; frames that look the
  same are one phone and rank as a tie. No Whole (Fill was 133 of 148 clear picks; whole stays
  ⛶'s per picture). About 35 min. **Measures:** the repeat agreement on the old picks (where
  the old first choice, middle or brightest, ranks above the other again), and auto against the
  middle by rank, where auto moves the frame. Written into this file.
- **No library** (night default, `release-0.8.0.md`'s rules): SA1 skipped; Glimpse's model is
  run on the desktop only as a reference, to see whether a learned map would win. The methods
  that can ship by 17 Nov: the spectral residual and a smoothed brightness-and-detail map in
  plain `:core` Kotlin. Glimpse by hand-written inference only if its layers turn out to be a
  few plain convolutions (SA0 says) and it wins clearly; else it moves to 0.9.0.
- **Build only if a shippable method beats the middle** on the user's new picks (Open 1);
  otherwise issue #3 moves to 0.9.0 and SA2–SA4 leave the queue.
- **With Sideways:** the map is one per picture; the position is taken along the frame's one
  direction, upright and sideways (`sidewaysFrame`), and SA3 notes both.
- **Open 3's default if built** (the user decides in the morning): no setting when it clearly
  wins (the middle stays the fallback, ⛶ moves it), else not built.
- **SA4** folds into T3 (`release-0.8.0.md`) and the FP6 on 17 Nov; the week of real pictures
  becomes the FP6's week after the tag.

## Packages (draft)

Built after 0.7.0 ships. A package's IDs and models are a draft until the user has answered the
questions it depends on.

| WP | What | Model, effort | Who, device | After |
|----|------|---------------|-------------|-------|
| SA0 | The test on the desktop: each method's frame against the user's picks, and a new picking round | Opus, high | a day session, **the user** | 0.7.0 shipped |
| SA1 | The runtime check, only if a library is still in the race: APK size, F-Droid, reproducible build | Opus, medium | a day session, no device | SA0, the user's answer to 2 |
| SA2 | `:core`: the map (if by hand), the position from the map, tests | Opus, high | winston, no device | SA0, the user (SA1 skipped) |
| SA3 | `Store`: the position once per post; Size [Centre | Auto | Live]; Advanced's fallback switch | Opus, high | winston, the emulator | SA2, SW3 |
| SA4 | Tablet pass, then the FP6 with the user | Opus, high | rosie, then a day session with **the user** | SA3 |

### [ ] SA0. The test on the desktop (Opus, high; a day session with the user)

No app code. In a venv in the scratchpad (TFLite's Python interpreter, numpy, Pillow), the
pictures from `private/pictures/` and, for NASA and Earth, small copies fetched as the plan for
0.7.0's brightness did.
- **Read the model:** `model.tflite`'s ops, input size and output (a map, or Glimpse's focal
  points); its licence and what it was trained on (the repo, its blog post or paper, if any).
- **The frames:** for each of the 253 APODs the position from the middle, the brightest area
  (A2's), Glimpse's model, the spectral residual and, if cheap to run, a UniSal LiteRT model.
- **Against the old picks:** for the 133 Fill picks, which method's frame lands nearer the frame
  the user picked (middle or brightest area). Where the methods agree with the middle they add
  nothing: count only the pictures where a method moves the frame.
- **A new picking round** (an artifact page like A2's, with a database for the picks): each
  picture's frame from the middle and from the best one or two methods, side by side in the
  screen's shape, unlabelled, in random order; the user picks or says "same".
- **Decides:** whether to build (the user's picks: a method must win clearly where it moves the
  frame, and must not lose badly on star fields), and which method. Written into this file with
  the numbers; the user answers Open 1–3.
- **Done when:** the numbers and the user's answers are in this file; the page's link noted.

**SA0's results so far** (2026-11-15 evening, an agent on the desktop; the scripts and outputs
in the session's scratchpad `sa0/`, the ranking page
https://claude.ai/code/artifact/ad836ce0-373f-476d-8bc6-935aacaa81c2, private, its database
collection `ranks`). Every map on a copy 128 px high, summed over the frame's full height; the
position is the frame with the largest sum; "pull": score = sum ÷ sum(middle) − 0.10·(2(p−0.5))²
(an edge frame needs 10% more than the middle; set before looking at the picks).
- **brightest:** grey blurred by a Gaussian of σ = 5% of the short side (no pull; the old page's
  approximation: within 0.1 of the middle on 55 of the 133 Fill picks; near the middle the
  picks were 33 : 22, far out 27 : 11, as 2026-10-22's 28 : 21 and 22 : 6).
- **importance** (the page's "auto"): 0.4 smoothed brightness + 0.4 smoothed Sobel gradient
  magnitude + 0.2 saturation × value, each normalised to its 99.5th percentile, then σ = 4% of
  the short side; pull on. Correlates 0.81 with brightest (the same side of the middle 85%).
- **spectral residual:** not shippable: frames at the picture's edge on 31% (18% with the pull),
  on single stars and the FFT's wrap-around; with a Hann window a mere centre prior.
- **Glimpse** (reference, run with ai-edge-litert): input 176×176 RGB, output 22×22; about 39k
  weights in 12 convolutions with instance norm (hand inference about 200 lines, inferred).
- **Against the old picks** (only where a method moves the frame ≥ 0.1): every method at about
  coin-flip agreement (0.43–0.62); mean distance to the picked frame on the 78 picks where the
  old pair differed: middle 0.107, importance_pull 0.132, glimpse_pull 0.150, brightest 0.219.
  None beats the middle; the comparison favours it (only middle and brightest were offered).
- **The ranking** (123 of the 152 pictures; 29 had all three frames the same): blind, 1-2-3,
  ties for frames that look the same. Decides Open 1: build only if auto ranks above the middle
  clearly where it moves the frame. Expected (SA0's agent): build nothing, issue #3 to 0.9.0.
- **Round 1's result** (the user, 2026-11-15 evening; "They look the same" means no subject, or
  frames so alike that ⛶ is needed anyway, so those are left out, user): 123 ranked, 25 "same".
  Auto (importance) vs middle 40 : 40; middle vs brightest 61 : 35; auto vs brightest 49 : 16.
  By how far auto moves: |p − 0.5| < 0.2 auto 23 : 31, ≥ 0.2 auto 17 : 9 (≥ 0.35 3 : 1). The
  repeat of 2026-10-22: where middle and brightest differ, 71 of 94 picks the same as then
  (76%); 11 of the 13 old Fit picks now "same". So auto ties the middle; the threshold rule
  "auto when it moves ≥ 0.2, else the middle" is tuned on these pictures.
- **Round 2** (the user: "another run, only between auto and center"): auto (unchanged) against
  the middle on fresh pictures (the 101 of `private/pictures/` not picked in October and the
  APODs since 2 Oct), only those where the two differ; tests the threshold rule on data it
  didn't come from. Decides Open 1.
- **Round 2's result** (the user, 2026-11-15 evening; https://claude.ai/code/artifact/e6e83bc4-8067-4879-84d1-28e615c2d360,
  private): 92 fresh pictures (67 old, 25 from 2 Oct to 15 Nov), the split fixed before: auto
  moves ≥ 0.2 on 29, < 0.2 on 63. "Same" left out: ≥ 0.2 auto 18 : 6 (two-sided p 0.02),
  < 0.2 23 : 20 (p 0.76), all 41 : 26. The threshold rule held on data it didn't come from.
- **Decided** (the user, 2026-11-15 evening: "ship with setting but default enabled"; Open 1–3):
  build it for 0.8.0; the method the importance map unchanged with the pull, used only when it
  moves the frame by 0.2 or more of the travel, else the middle; no library (the setting: the
  next bullet). ⛶ overrides it. Its texts placeholders until the user approves them.
  SA0's script, positions and the round-2 pictures are in `private/sa0/` and
  `private/pictures-oct-nov/` (gitignored), for SA2's tests.
- **Then, the same evening** (the user: "why do we not just do the auto mode full … then we have
  3 options, center, auto and live?"; decided from two questions): **no switch in Setup: Size
  becomes [Centre | Auto | Live]**, replacing [Fill | Live]; **Auto the default** (an update's
  `FILL` reads as Auto, the same change as the switch's default on); the segments' texts
  "Centre", "Auto", "Live" chosen by the user. Auto keeps the fallback (the middle when the
  frame would move less than 0.2: small moves were 46 : 51 for the middle over both rounds,
  clear ones 35 : 15 for auto), and **a switch in Advanced settings turns the fallback off**
  ("only auto, no fallback to center on any picture", the user).

### [ ] SA1. The runtime check (Opus, medium; only if a library is still in the race)

- A throwaway build of the app with the runtime added (`.coord/probe-sa/`, not merged): the
  unsigned release APK's size against 0.7.0's, per ABI and with `abiFilters`.
- F-Droid: whether its scanner and build server take the library (its docs and fdroiddata's
  recipes of apps that use it); whether the build stays reproducible (two builds, `diff -r`).
- **Decides** with the user: the library, the hand-written inference, or no model.

### [ ] SA2. `:core`: the position from the map (Opus, high)

Read first: this file's "How it would fit", "For the 17 Nov deadline" and SA0's results and
"Decided" above, and release-0.8.0.md's "Rules for every 0.8.0 package".
- **The map, exactly SA0's importance** (`private/sa0/compute.py`, its `importance` and
  `position`; port it, don't retune): on a copy `WORK_H` = 128 px high (the long side for a tall
  picture), RGB 0–1; brightness = the grey Gaussian-smoothed (σ as `compute.py`), detail = the
  Sobel gradient magnitude of the grey, smoothed, saturation × value; each shifted to 0 and
  divided by its 99.5th percentile, clipped to 0–1; the map = Gaussian(0.4 · bright + 0.4 ·
  detail + 0.2 · sat, σ = 0.04 × the short side). Plain Kotlin (a separable Gaussian, a 3×3
  Sobel, a percentile by sort): no library. The input an Android-free `IntArray` of ARGB pixels
  with its width and height (`:app`'s `Drawing.kt` decodes the copy, SA3).
- `subjectPosition(map, mapW, mapH, screenW, screenH, sideways): Float`: `position()`'s score per
  frame place (the map's column or row sums, the frame's share against the middle frame's, minus
  `PULL` = 0.10 × (2(p − 0.5))²), its best p, **without** the fallback (a flat or empty map:
  0.5). `autoPosition(p, fallback: Boolean)`: with `fallback`, 0.5 when |p − 0.5| <
  `SUBJECT_MIN_MOVE` = 0.2 (its reason: both rounds' numbers), else p. The raw p is what's
  stored, so Advanced's switch applies at the set.
  `sideways`: the map turned clockwise, (h, w), position 0 the original's bottom band, so it
  agrees with `sidewaysFrame` (Frame.kt) and SW2's band.
- **Tests:** a map with its peak left, right, in the middle, flat; the pull; the threshold
  (`autoPosition(0.65, true)` → 0.5, `(0.75, true)` → 0.75, `(0.65, false)` → 0.65); sideways against `sidewaysFrame`'s
  invariant; **the desktop's positions**: for every picture in `private/pictures/` and
  `private/pictures-oct-nov/` (`assumeTrue` on the folders), decoded on the JVM (`ImageIO`, test
  code only) to the same 128 px copy, `importance_pull` within 0.03 of `private/sa0/positions-
  253.csv` / `positions-round2.csv` for at least 95% of them (report the misses; JPEG decoders
  and resampling differ a little).
- **Done when:** the tests pass; `CLAUDE.md` (Next features, 0.8.0's subject frame: the method,
  the threshold and both rounds' numbers) and the skill's "Where things are".

### [ ] SA3. `Store`: the position once per post; Size [Centre | Auto | Live]; Advanced's switch (Opus, high)

Read first: this file's "How it would fit", "For the 17 Nov deadline", "Decided" and "Then, the
same evening", and release-0.8.0.md's "Rules for every 0.8.0 package". After SA2 and SW3.
- `refresh`, after a stored download (~869-897, after the `renameTo`), decodes a copy 128 px
  high (`Drawing.kt`, upright) and notes both raw positions (`<source>.subjectPosition` and
  `<source>.subjectSideways`, keyed by the post's date and picture URL as `cropDate`/`cropUrl`);
  on a failure nothing (logged; the middle). Logged `Subject: APOD 2026-11-16 p=0.78 (sideways
  0.50) in <n> ms`. A post stored before 0.8.0 gets none (no migration): the middle until the
  next post.
- **Size [Centre | Auto | Live]** (the user's texts, 2026-11-15): the segmented row L6 built,
  three segments, the pref `size` `CENTRE`, `AUTO`, `LIVE`; **default `AUTO`**; a stored `FILL`
  (0.7.0) reads as `AUTO` (no write, no migration code; `:core`'s reading of the pref, tested).
  Everything L6 did for "not Live" (Fill: every set still, `opensPicker`, `stillOrLive`,
  `sharedLockFollows`, Setup's summary " · Live", the status line, the highlight, the saved
  state, "the order alone") holds for Centre and Auto alike: grep every `Size.FILL` and
  `== LIVE`/`!= LIVE`. Setup's summary doesn't name Centre or Auto. The segments wrap or shrink
  in the tablet's and the FP6's landscape panes as "Change every"'s do (equal heights).
- **Advanced settings: the fallback switch** (acts at once, not part of the draft, never
  highlighted; while the daily switch is on, as Advanced's others; after "Save every picture"'s
  group, before "Keep running"), the pref `autoFallback`, **default on**. Placeholder texts:
  the label "Auto: centre when unsure", a small ⓘ "About Auto: centre when unsure", its dialog
  "With Size Auto, a picture whose subject is only slightly off the middle stays centred. Off:
  Auto places every frame." Add the ⓘ to `CLAUDE.md`'s list of allowed help texts. Night
  default: it takes effect at the next set (a change time, Set, ✓), no set at once (the
  alternative: a set like ⛶'s ✓ at once).
- **Where the position applies** with the stored Size Auto (the user's part from ⛶ always wins;
  Centre: 0.5, as 0.7.0's Fill): `autoPosition(raw, autoFallback)` in `Store.set` (`part?.position
  ?: 0.5f`, ~1565), the measure (`frameGrey` gets the same position), ⛶'s start (`startCrop`
  ~1405), and `setPart`'s no-change check (~1377: compare with that position, else ✓ without a
  drag saves and sets again). Not Live (`copyForLive`: Live pans by the clock). The effective
  sideways value (SW3) picks which raw position; toggling the pill moves an undragged frame to
  that orientation's position. ⛶ follows the stored Size (night default).
- **Device** (rosie, the emulator): a picture whose subject is clearly off the middle
  (`private/sa0/positions-round2.csv`, `importance_pull` ≥ 0.75 or ≤ 0.25) and one with a small
  move (0.55–0.65), faked into `files/` as the skill says: with Auto and `run -f 1` the first's
  frame on the subject, the second's in the middle (`debug-set.png`, the `Subject:` line);
  Advanced's switch off and the next set: the second moves too; Centre and Set: both in the
  middle; ⛶ opens at the used position, ✓ elsewhere wins; sideways uses the sideways position;
  Live as before; an update from 0.7.0's Fill shows Auto selected and nothing set; a broken
  picture gives the middle; screenshots of the row (portrait and the landscape pane), the
  Advanced switch and its ⓘ; the smoke list.
- **Done when:** the tests and the device pass; `CLAUDE.md` (The app, Settings: Size and
  Advanced, the help-text list; Wallpaper and Choosing the part; Next features); the maps
  (`setup.md`'s Size section, `page.md` K for ⛶'s start and Advanced's switch,
  `background.md` the job's set).

### [ ] SA4. Tablet pass, then the FP6 with the user (Opus, high)

R8 build (the runtime or the inference under R8 isn't covered by unit tests), an update from
0.7.0's build, a week of real pictures read each morning; then the user's look on the FP6.
