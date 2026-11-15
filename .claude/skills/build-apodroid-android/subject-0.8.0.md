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
  brightness). The same 152 pictures in a new random order, the old picks hidden, the choices
  unlabelled and shuffled per picture: the middle, the brightest area, each shippable method's
  frame (identical frames merged into one choice, so "same" is visible), Whole, and "No
  preference". About 35 min. **Measures:** the repeat agreement on the old picks (where the
  middle and the brightest area both stand, how often the same one is picked again; Fill vs
  whole again), and each method's wins where it moves the frame. Written into this file.
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
| SA2 | `:core`: the map (if by hand), the position from the map, tests | Opus, high | winston, no device | SA0, SA1 |
| SA3 | `Store`: the position noted once per post, `Store.set` and ⛶ start from it, the setting if any | Opus, high | winston, tablet | SA2 |
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

### [ ] SA1. The runtime check (Opus, medium; only if a library is still in the race)

- A throwaway build of the app with the runtime added (`.coord/probe-sa/`, not merged): the
  unsigned release APK's size against 0.7.0's, per ABI and with `abiFilters`.
- F-Droid: whether its scanner and build server take the library (its docs and fdroiddata's
  recipes of apps that use it); whether the build stays reproducible (two builds, `diff -r`).
- **Decides** with the user: the library, the hand-written inference, or no model.

### [ ] SA2. `:core`: the position from the map (Opus, high)

- If by hand: the model's layers in plain Kotlin, its weights read from the asset; tests against
  the desktop's outputs for a few real pictures (the outputs in `private/`, as the real posts).
- `subjectPosition(map, w, h, screenW, screenH): Float?`: the frame's place with the largest sum
  of the map (SA0's pull to the middle), null when the map is flat (nothing stands out: the middle).
- **Done when:** the tests pass; `CLAUDE.md` and the skill's "Where things are".

### [ ] SA3. `Store`: the position once per post (Opus, high)

- `refresh` notes `<source>.subjectPosition` after a stored download (on a failure nothing);
  `Store.set` and ⛶'s start (`startCrop`) use it where `forPost` finds no part for that post.
- The setting, if the user wants one (its text a placeholder until the user approves it).
- **Tablet:** a picture whose subject is off the middle (from SA0's list, faked into `files/` as
  the skill says), set by `run -f 1`: the frame on the subject; ⛶ opens there; ✓ elsewhere wins;
  a new post starts afresh; a broken picture or model gives the middle; the smoke list.
- **Done when:** the tablet passes.

### [ ] SA4. Tablet pass, then the FP6 with the user (Opus, high)

R8 build (the runtime or the inference under R8 isn't covered by unit tests), an update from
0.7.0's build, a week of real pictures read each morning; then the user's look on the FP6.
