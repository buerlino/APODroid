# Plan for 0.6.0 (APOD first; all NASA pictures; the features from 0.1.1's review)

Planned 2026-10-19. Release target: 0.6.0 (versionCode 10), no date set. Work through the
packages in order, one at a time: build, show it on the device, wait for the user. Tick them off
and note where the work differed from the plan. Each package writes its decisions into
`CLAUDE.md` (the source of truth) when it lands; this file is the how. Finished packages become
one line each here; git keeps their notes.

## The request

From the user looking at 0.5.0 (2026-10-19):
1. Why can't one screen show APOD and the other NASA's picture? "Where" sets one picture on
   home, lock or both.
2. Why does "Use my picture" for video days only work with APOD ticked alone? With turns a video
   APOD just skips its turn. The user wants their own picture there too: it shows at once that
   it was a video day.
3. The app is called APODroid: APOD comes first. The plain APOD app stays as it is; the other
   sources become a mode the user turns on, with all three tabs to browse (today a tab shows
   only for a ticked source, so an unticked Earth picture can't be looked at or set).

The rest of 0.6.0 is `features.md` items 2, 4, 6, 7 and 8 (moved there 2026-10-16).

## Decisions (user, 2026-10-19)

Discussed in one session; rejected on the way: a single "Lock screen: one source" row under
"Where" (covers only a fixed lock source), a first-boot screen to choose the mode (an extra
screen, asked before the user has seen the app; updaters never get a first boot).

- **Two modes, a switch "All NASA pictures"** at the end of the settings, default off.
  - **Off (APOD mode)**: the app as 0.4.0/0.5.0 with APOD alone: one picture, no tabs (except
    the video-day source's on a video day, as now), "Where", "If APOD is a video", the set
    button.
  - **On**: always three tabs (APOD | NASA | Earth), whatever is ticked; the shown tab's
    title, explanation and (F4) credit; the settings as in the layout below.
  - Each mode keeps its own settings: "Where" belongs to APOD mode, the grid to the other.
    The first time the switch goes on, the grid starts from "Where" (APOD ticked under Home,
    Lock or both). "If APOD is a video" is one setting for both modes.
  - From 0.5.0: APOD ticked alone → APOD mode, all else unchanged; **anything else → on**
    (third review: 0.5.0 lets NASA or Earth be ticked alone, `MainActivity.kt:991`, and such a
    user must keep their source), with the ticked sources in the columns "Where" named (Both:
    both columns).
  - Turning it on or off while the daily switch is on works like a tick: whatever the new mode
    puts on a screen is set at once, and the job is rescheduled (its period depends on the
    mode). It clears every hold (the new mode's settings decide what shows). Switching it off
    leaves a screen "Where" doesn't name alone, also when the grid had put NASA's or Earth's
    picture there (user, 2026-10-19: as 0.5.0 when "Where" goes from Both to Home; "Both" puts
    APOD there).
- **The grid** (only with the switch on): a row per source (full names), a tick box per screen
  (Home, Lock). It replaces the chips and "Where".
  - Both screens due the same picture: one set with both flags, one wallpaper, as today (with
    equal columns that's the normal case; a picture set by hand on one screen makes them differ
    until its hold ends). An empty column: that screen is left alone. The last tick can't be
    removed.
  - A column with two or more ticks takes turns. "Change every" shows while a column has two
    or more. **One shared clock**: both screens change at the same moment. It restarts only
    when the turn ends, or on a set by hand that touches a screen with turns (user,
    2026-10-19, second and third reviews: a set on a screen without turns leaves the clock
    alone), not when one screen changes early (an untick, a source with no picture yet, an
    APOD video with "Skip it"): a source that comes in early gets the rest of the current
    turn, even if short, so the screens stay in step. Ticking a second source into a column
    restarts it only when no column took turns before (so the tick doesn't end the current
    picture at once, as today).
  - Each screen's record notes the turn it belongs to (the clock's value; third review). A
    screen with turns whose record isn't from the current turn is due its next source; after
    its set (or when no other source has a picture) the record takes the current value. So a
    failed set on one screen is retried without moving the other on again. A restart by hand
    writes the new value only into the screens with turns whose record had the current value
    (fourth review: a screen still due a retry keeps its old value, so it isn't held back a
    whole turn).
  - **Forgetting a record** (user, 2026-10-19, fourth review): a changed setting, the daily
    switch going on and the mode switch drop the screen's picture and hold, but keep whose turn
    it is and the stamp, so the normal set puts the current source's picture on again instead
    of moving the screen on one. The daily switch going on still restarts the clock (as now,
    Background update, Turns), and the screens on the current turn take the new value.
  - **A screen with no record** (the lock screen after migrating "Home", a column's first
    tick, a fresh install): it starts on the other screen's source if that source is in its
    column, else on the first ticked source with a picture, and takes the current stamp.
  - A picture goes on the screens its source is ticked for.
- **Video days: the stand-in takes APOD's place everywhere.** "If APOD is a video" keeps its
  four choices in both modes and is never greyed out: "Skip it" (was "Keep the previous
  picture"), NASA Image of the Day, Earth Observatory, "Use my picture". "Skip it": a screen
  with APOD alone keeps its picture; a screen with turns gives the turn to the next source. The
  others fill APOD's turn on every screen where APOD is ticked, with turns too. A stand-in also
  ticked on that screen may show twice in a row: allowed.
  - Its control is a dropdown (it closes after a choice and opens again with a tap). With the
    switch on it sits right under APOD's grid row ("If it's a video"), greyed out while APOD has
    no tick (so the page doesn't move, as U4 decided); in APOD mode under "Where". Choosing "Use
    my picture" with nothing picked opens the photo picker at once; "Change picture" beside the
    dropdown afterwards.
- **"Set as wallpaper" stays.** In APOD mode as now, at the end of the Wallpaper section
  ("Where" says where). With the switch on it sits under "Change every" (the layout below) and
  opens a small menu **Home screen / Lock screen / Both**, since the shown tab may be ticked
  nowhere; ✓ in crop mode opens the same menu.
  **A set by hand changes the picture, not whose turn it is** (user, 2026-10-19, third
  review): when the hold ends, the screen carries on where its turns were, so two screens
  with equal columns become one wallpaper again. The cost, accepted: the source set by hand
  may come round again at the next turn.
  **Changing a setting is not a set by hand** (third review): a "Where" change, a video-day
  choice, "Change picture", the mode switch, and (F1) Fit forget the affected screens'
  records and run the normal set; no hold, no clock restart. ("Change picture" needs this
  anyway: the record stays the same while the file changes.)
  **The hold** (user, 2026-10-19, after the plan's review): a picture set by hand stays until
  that screen's next normal change: on a screen with turns its turn ends; on a screen with one
  ticked source that source posts a new picture (e.g. tomorrow's APOD). Meanwhile a newer post
  of the held picture's own source is set at once and keeps the hold (as for the source on
  the wallpaper today). **Any change in that screen's column ends its hold** (a tick or an
  untick; user, 2026-10-19, fourth review: the user is reconfiguring that screen), and the
  screen gets what its settings say at once. A held APOD whose next post is a video follows
  the video-day rule (the stand-in in its place; "Skip it" keeps the held picture), and the
  hold runs to its normal end (user, 2026-10-19). No hold when there's nothing to hold
  against: a picture set on a screen with no ticks stays for good (that screen is left
  alone), and a hand set of a one-source screen's own ticked source is its normal picture
  (fourth review: else "ends when it posts" and "its newer post keeps the hold" collide).
  On APOD's video tab the button (and ✓) sets the stand-in; with "Skip it" nothing ("Video day:
  wallpaper kept", as now). **Held, it counts as APOD's picture** (user, 2026-10-19): it
  stands in APOD's place, so APOD's next post replaces it (as on any video day) and NASA's
  next post doesn't touch it.
- **The page with the switch on:** the "On wallpaper" chip becomes "On home screen", "On lock
  screen" or "On wallpaper" (both), on the tab of the picture's source (a stand-in NASA picture:
  NASA's tab, not APOD's); the status line has a line per screen while they differ
  ("Home screen changed 16 Oct, 06:12 · NASA Image of the Day"); the video notice also shows
  with turns (it tells the user it was a video day); with APOD ticked nowhere it only says the
  APOD is a video (nothing takes its place). APOD mode with "Where" = Lock reads the lock
  screen's record (after the migration home's is empty there).
- **Fill / Fit (F1): one setting for both screens** (user, 2026-10-19), as `features.md` says,
  so "one set when both screens are due the same picture" holds.
- **Data:** with the switch on the page fetches all three sources (three fresh tabs); the job
  fetches only what the screens need: the ticked ones, the source of each picture held by
  hand (its newer post is set), and the stand-in on an APOD video day, only while APOD is
  ticked on some screen (or in APOD mode). The page-open hour rule (WP7c) applies to all
  three.

All texts above are placeholders; the user approves them on the device.

## Settings layout afterwards

```
APOD mode (switch off)                      All NASA pictures (switch on)

Wallpaper                                   Wallpaper
[switch] Change wallpaper automatically     [switch] Change wallpaper automatically
         Wallpaper changed 19 Oct, 06:12             Home screen changed 19 Oct, 06:12 · NASA …
         [switch] Wi-Fi only        (F2)             Lock screen changed 19 Oct, 05:05 · APOD …
         [switch] Save every picture ⓘ (F5)          [switch] Wi-Fi only        (F2)
Where   [Home screen | Lock screen | Both]           [switch] Save every picture ⓘ (F5)
Fit     [Fill | Fit]                (F1)    Fit     [Fill | Fit]                (F1)
If APOD is a video  [Skip it ▾]             ──────────
[Set as wallpaper now]                      Pictures from                      Home   Lock
──────────                                    Astronomy Picture of the Day      [✓]    [✓]
[switch] All NASA pictures                       If it's a video [Use my picture ▾] Change picture
                                              NASA Image of the Day             [✓]    [ ]
                                              Earth Observatory                 [ ]    [ ]
                                            Change every  [Day | 12 h | 6 h | 3 h]
                                            [Set as wallpaper ▾]
                                            ──────────
                                            [switch] All NASA pictures
```

The lines under the daily switch show only while it's on, as now. Where F1, F2 and F5 sit is
`features.md`'s proposal; confirm with the user in their packages.

## Packages

| WP | What | Model, effort |
|----|------|---------------|
| M0 | Android check: home and lock set separately (tablet, 0.5.0 as installed) | Opus, medium |
| M1 | `:core`: screens, columns, the stand-in and the hold, with tests | Opus, high |
| M2 | `Store`: mode, grid, a record per screen, migration, fetch rules | Opus, high |
| M3 | Settings: the mode switch, the grid, the dropdown, the set menu | Opus, high |
| M4 | The page: three tabs, chip, status lines, video notice, problem box | Opus, medium |
| F1 | Fill / Fit | Opus, medium |
| F2 | Wi-Fi only | Sonnet, medium |
| F3 | Share | Sonnet, medium |
| F4 | Image credit | Opus, medium |
| F5 | Save every picture | Opus, medium |
| F1f1 | Fit decodes at the size it's drawn (review of F1) | Opus, medium |
| F5f1 | A stopped save: the smaller copy next time (review of F5) | Opus, medium |
| C  | Early look on the tablet: the glyph and the texts, on the user's go-ahead | Opus, medium |
| V  | Tablet pass of everything (debug and R8), the update from 0.5.0: V1–V6 by night, V7 by day | Opus, high |
| P  | Phone pass on the FP6 (release-signed), after the version bump | Opus, high |
| R  | Docs, changelog, store texts, screenshots | Opus, medium |
| T  | Commit and tag, when the user says so | Sonnet, low |

Each package runs in a fresh session started from the repo folder with its model and effort.
Run `git status` first and build on what's there; don't revert or reformat files you didn't
touch. **When a package is done (tested on the device and reported), end the report with the
command for the next one**, copied from here:

```
claude --model opus --effort medium "Do M0 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort high "Do M1 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort high "Do M2 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort high "Do M3 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort medium "Do M4 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort medium "Do F1 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model sonnet --effort medium "Do F2 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model sonnet --effort medium "Do F3 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort medium "Do F4 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort medium "Do F5 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort medium "Do F1f1 and F5f1 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort medium "Do C in .claude/skills/build-apodroid-android/plan-0.6.0.md"
scripts/night.sh --until <HH:MM>    # V1–V6: the queue in night.md
claude --agent rosie --model sonnet --effort medium "Do V7 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort high "Do P in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort medium "Do R in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model sonnet --effort low "Do T in .claude/skills/build-apodroid-android/plan-0.6.0.md"
```

Devices: the skill, Working on the phone. The tablet is free for testing; the FP6 is the
user's own phone, touched only in P. The build check:
`ANDROID_HOME=~/Android/Sdk ./gradlew :core:test :app:lintRelease :app:assembleRelease`.

## Done

Built by the night shift of 2026-10-20 (`night.md`; the report `.coord/reports/2026-10-20-night.md`
is gitignored), merged on `dev`, approved by the user that morning (texts and night defaults from
the tablet's screenshots) and fast-forwarded into master. Their notes: the report commits. (The
headings stay: night.md's queue rows and `scripts/pipeline.test.sh` resolve them.)

### [x] M0. Android check: home and lock set separately (Opus, medium)

PASS on the tablet (`CLAUDE.md`, Wallpaper).

### [x] M1. `:core`: screens, columns, the stand-in and the hold (Opus, high)

`Screens.kt`: `dueWallpapers`, `planByHand`, the stand-in, the hold (two fix rounds).

### [x] M2. `Store`: mode, grid, a record per screen, migration, fetch rules (Opus, high)

The mode, the columns, a record per screen, `Migration.kt`. M2f1: the video-day choice forgets the records.

### [x] M3. Settings: the mode switch, the grid, the dropdown, the set menu (Opus, high)

Both modes' settings, as `CLAUDE.md` (The app, Settings) describes them.

### [x] M4. The page: three tabs, chip, status lines, video notice, problem box (Opus, medium)

The page per screen. M4f1: "Skip it" passes APOD's turn at once.

### [x] F2. Wi-Fi only (Sonnet, medium)

The switch and the job's unmetered constraint (`CLAUDE.md`, Background update).

### [x] The morning (2026-10-20, a day session)

The user's answers (in `CLAUDE.md`: the grid and the status line by short names, the problem
box, the mode switch with the daily switch off, Wi-Fi only waiting, the tab pick after a kill;
F1, F3, F4 and F5 below) and the fix package (the short names; `chooseVideoDays`' comment,
which still said "Skip it" sets nothing).

## Remaining

F1, F3, F4 and F5 are day work (user, 2026-10-20), one at a time; their questions are answered
below. The tablet was busy that day (pascaline): a package is built and unit-tested first, and
checked on the tablet once the user says it's free.

**The order from here** (user, 2026-10-20 afternoon, after a review of F1, F4 and F5 by
reading; their build check passed, 108 tests with `private/`):
1. By day, no device: F3's commit (its review: the `Row` around ★ not re-indented; otherwise
   fine), then **F1f1** and **F5f1** (found by the review), each committed when the user says
   so. Done 2026-10-20 (three commits).
2. **C**, the early look, only when the user gives the go-ahead (the tablet is pascaline's
   until then); its text changes are made and committed before the night.
3. **V1–V6 by night** (user): rosie on the tablet, the queue in `night.md`. F1–F5's own
   device bullets are run there, not separately (user: V reruns them on the final tree
   anyway). If C hasn't run by then, V runs anyway and the user approves from V1's and V2's
   screenshots.
4. **V7 by day** after the night (turns over hours: the night's later rows and the
   regression reinstall and change the tablet).
5. P on the FP6 with the user, then R and T.
Left as is (2026-10-20): the compiler warning on `JobInfo.networkType` in `DailyJob.ensure`
(deprecated since API 28; it works, and `requiredNetwork` would mean comparing
`NetworkRequest`s).

### [ ] F1. Fill / Fit (Opus, medium)

`features.md` item 2 has the how. Settled: one setting for both screens (2026-10-19); changing
it while the daily switch is on sets the wallpaper again at once, like "Where" (user,
2026-10-20).
**Status (2026-10-20, day):** built and unit-tested (`fitFrame`, 4 tests; the build check
passes); the texts approved by the user in C (the heading "Size" over [Fill | Fit]); the device
checks are open (V1/V2).
- The pref `fit` (default off: Fill, today's behaviour), a segmented row "Fit" [Fill | Fit]
  (placeholder) in the Wallpaper section in both modes (the layout: APOD mode under "Where",
  with the switch on after the switches under the daily one).
- Fit: the picture decoded upright and sampled (`decodeUpright`), drawn centred on a black
  bitmap of the screen's portrait size, `setBitmap(bitmap, null, true, flags)`. Every picture,
  the user's own too; both screens alike, so one picture is still one set. The rectangle it's
  drawn into comes from `:core` (`Frame.kt`, beside `wallpaperFrame`), with tests (wide, tall,
  the screen's own shape). No ⛶ with Fit; choosing Fit closes crop mode.
- A change is a changed setting, never a set by hand: `Store.settingChanged` with a new
  `Changed.FIT` that affects both screens' records (forgotten, then `setDue`; no hold, the clock
  stays). With the daily switch off nothing is set; the next set uses it.
- Files: `core/.../Frame.kt` and `FrameTest`, `Store.kt`, `MainActivity.kt`, `CLAUDE.md` (The
  app, Wallpaper and Settings).
- Device (the tablet): a wide picture (an Earth map if one is stored, else a made-up one in
  `files/`) and a tall one with Fit, on home and lock (`dumpsys wallpaper`, screenshots), in both
  modes; a change with the daily switch on sets at once and `turnStartedAt` stays; no ⛶ with Fit.
- Done when: the tests pass, the device checks pass, and `CLAUDE.md` says how it works.

### [x] F1f1. Fit decodes at the size it's drawn (Opus, medium)

**Status (2026-10-20, day):** built (`fitted` in `Store.kt`: `setTargetSize` to `fitFrame`'s
size, drawn 1:1; the build check passes). `info.size` and `setTargetSize` are the upright size
on Android 10 (`SkAndroidCodec` made with `ExifOrientationBehavior::kRespect`) and 11+
(`swapWidthHeight`), read in AOSP's `ImageDecoder.cpp`. No new pure helper, so no new test. The
device check is V1's.

Found by reading (review of F1, 2026-10-20); the fix settled by the user the same day.
`fitted()` in `Store.kt` decodes with `decodeUpright(file, screen)`, whose sample rule is
Fill's (halve while both sides stay at least the screen's). With Fit only one side has to: a
wide picture comes in at the stored rendition's full size (a 3:2 post at `?h=2484` is
3726×2484, 37 MB) and `drawBitmap` with `FILTER_BITMAP_FLAG` shrinks it about 3.3× in one
bilinear step, which can make fine detail (stars) jagged or drop it.
- The fix: the frame from the picture's upright bounds (`fitFrame`), then decode straight to
  the frame's size (`ImageDecoder`'s `setTargetSize(frame width, frame height)`) and draw it
  1:1 into the frame on the black bitmap. A smaller picture (Earth's 720 px) is scaled up as
  before. Fill's path and `decodeUpright`'s other caller stay as they are.
- EXIF: `ImageDecoder` turns the picture upright itself. Whether `info.size` (and so the
  frame) is the size before or after the turn: find out from Android's docs or source while
  building, say which in the code's comment, and leave the check to V1 (a made-up JPEG with
  orientation 6 as "my picture"; the skill, Put a file in place).
- Files: `Store.kt`, `CLAUDE.md` (The app, Wallpaper: Fit). `:core`'s `fitFrame` has its tests
  already; a new pure helper gets one.
- Device: in V1 (a star field with Fit; an EXIF-rotated "my picture" with Fit, upright).
- Done when: the build check passes and `CLAUDE.md` says how Fit decodes.

### [ ] F3. Share (Sonnet, medium)

**Status (2026-10-20, day):** built (`ShareButton`, `share` in `MainActivity.kt`; the build
check passes); the glyph ⤴ approved by the user in C, at 28 sp (★ stays 24 sp); the device
checks are open (V1/V2).

`features.md` item 6 has the how. Settled: the glyph ⤴ (U+2934; user, 2026-10-20, from ⤴, ↗, ⇪
and a drawn Material icon), to be seen on a device.
- ⤴ in the same dark circle as ★, left of ★ at the picture's bottom right; on video days (no ★)
  at the ▶ tile's bottom right, as the link works. Screen readers "Share". In crop mode as ★.
- A tap: `Intent.createChooser` of an `ACTION_SEND` `text/plain` with `"<title>\n<page link>"`
  (no `FileProvider`, no picture). With tabs the shown tab's post.
- Files: `MainActivity.kt`, `CLAUDE.md` (The app).
- Device (the tablet): the share sheet opens with the text, on a picture and on a video tile,
  APOD's and NASA's tabs; a screenshot of ⤴ beside ★ for the user. If the font draws it badly,
  ↗ and ⇪ in the same place for the user to compare.
- Done when: the user has seen the glyph and approved it, and `CLAUDE.md` describes it.

### [ ] F4. Image credit (Opus, medium)

**Status (2026-10-20, day):** built and unit-tested (`credit`, 5 tests in `ApodTest`, all 100
real posts in `readsEveryRealPost`; the build check passes); the text approved by the user in C
(one line closed, was two); the device checks are open (V1/V2). Differed from the plan: " :" (one post, "Processing & License :")
and ",;" (a line ending in a comma, the 254-character one) are tidied too; `parseApod` reads
through `apodPosts`, as NASA's and Earth's parsers do, so the test reads all 100.
`features.md` item 7 has the parsing. Settled: **APOD only** (user, 2026-10-20; NASA's feed
names a credit in 1 of 60 items, Earth's is in `content` only, about 100× the data per check,
and mostly "NASA Earth Observatory").
- `:core`: `Post.credit` (null for NASA and Earth, and when an APOD post has none), parsed in
  `parseApod` as `features.md` says. The label: the post's own (`Credit`, `Credit & Copyright`,
  `Credit & License`, `Color Credit`), ending in one colon, since "Copyright" says something
  "Credit" doesn't; the text to approve on the device.
- `Store`: APOD's pref `credit`, written with the post. A post stored by 0.5.0 has none:
  nothing shows until the next post or a pull (`refresh` stores what it fetched; no
  migration).
- The page: one line under the date (APOD mode: title, date, credit) or, with tabs, under the
  title on APOD's tab; at most two lines with an ellipsis, all of it while the explanation is
  open. Smaller and greyer than the explanation.
- **Seen in `private/last100.json` (2026-10-20, before building):** all 100 have a credit row;
  the `<th>` is "Credit" 45×, "Credit &amp; Copyright:" 27×, "Credit &amp; Copyright" 14×,
  "Credit:" 11×, "Color Credit:", "Credit &amp; License", "Credit and Copyright:" once each
  (the other rows are "Date", "Authors &amp; editors:", "A service of:", "Hat tip:"). Under a
  plain "Credit" the value often carries its own label: "Image Credit &amp; Copyright: Guy
  Bardon", "Image Credit: JAXA , ISAS", "Video Credit: …" (35 posts), so show such a value as
  is and prefix the `<th>`'s label only when the value has none. Tags stripped leave spaces
  before punctuation ("JAXA , ISAS", "( MTU )"): tidy " ," " ;" " )" "( ". `<br>` → "; " gives
  "; ;" and ";;" twice (empty lines): collapse them. 21 have a "; " part (Processing, Text,
  Music); lengths 4 ("NASA") to 255 characters.
- Tests: each label shape, a value with its own label, a `<br>` joined with "; ", the tidying,
  entities, none; every APOD post in `private/last100.json` has one (`readsEveryRealPost`).
- Device (the tablet): a short credit and the longest of the 100 (251 characters; fake it by
  prefs on a debug build), APOD mode and with tabs, the explanation open and closed.
- Done when: the tests pass, the user has seen it and approved the text, and `CLAUDE.md` (Data
  source, The app) has it.

### [ ] F5. Save every picture (Opus, medium)

**Status (2026-10-20, day):** built and unit-tested (`sourcesInUse` and `toSave` in
`core/.../Saving.kt`, 7 tests in `SavingTest`; the build check passes); the ⓘ text approved by the
user in C (the per-source sizes dropped); the device checks are open (V1/V2). As planned; `update` asks `sourcesInUse` for each
source in turn, so APOD's refresh still decides the stand-in.
`features.md` item 8 has the switch and the ⓘ. Settled (user, 2026-10-20): **every source in
use, not APOD only** (APOD only would favour it), and on a failed original the smaller copy, as
★ does.
- The switch "Save every picture" with a small ⓘ (screen readers "About saving") under "Wi-Fi
  only", shown while the daily switch is on, default off (the pref `saveEvery`). The ⓘ opens a
  small dialog (placeholder): full-size pictures in Pictures/APODroid; APOD's 2–10 MB,
  NASA's up to about 100 MB, Earth's up to 3 MB; a few hundred MB a month or more.
- **What it saves:** after each job run (not the page's: a page closes, and its saves would
  compete with ★), also after one that failed (a failing source mustn't stop the others'
  saves), not after a stop: each source in use, in order APOD → NASA → Earth. The set in use is
  the one `update` fetches (the ticked ones, a held picture's source, the stand-in on an APOD
  video day); factor it out of `update` into one function both use. Skipped: a video, a post
  without a stored picture, one whose `savedDate`/`savedUrl` match (no URI check: a picture
  deleted in the gallery isn't saved again; ★ still checks the URI). Edge cases that follow:
  APOD mode saves APOD, and on a video day the stand-in (NASA or Earth); "Skip it" or "my
  picture" on a video day saves nothing; weekends find Friday's posts saved already.
- **How:** `Store.save` as ★ (the original, else the stored picture), one at a time; while ★
  saves (`BUSY`) the job skips it and the next run tries. A stop must end it: `save` today
  catches every `IOException` and falls back to the stored picture, also for an interrupted
  download (`isStop()`); rethrow that. A failed save is logged and doesn't make the job retry
  (the next run tries again).
- The pure choice (which posts to save, from the posts, the set in use and the noted saves) in
  `:core`, with tests: a video, no picture, noted, the order, the stand-in.
- Files: `core/...` (the choice and its test), `Store.kt`, `DailyJob.kt`, `MainActivity.kt`,
  `CLAUDE.md` (Save, Background update).
- Device (the tablet): the switch on, `run -f` → each source in use's original in the gallery
  (`content query` on MediaStore); a second run saves nothing; a picture deleted in the gallery
  isn't saved again; a faked APOD video day with NASA standing in (NASA saved, not APOD); a stop
  during a save (`cmd jobscheduler timeout`) leaves no file and the next run saves the stored
  picture, the smaller copy (F5f1). Delete the test files afterwards.
- Done when: the tests pass, the device checks pass, the user has approved the ⓘ text, and
  `CLAUDE.md` says how it works.

### [x] F5f1. A stopped save: the smaller copy next time (Opus, medium)

**Status (2026-10-20, day):** built and unit-tested (`toSave` returns `ToSave(post, original)`
from the noted tries, 2 new tests in `SavingTest`, 110 with `private/`; the build check
passes). Differed from the plan: `save`'s flag is `tryOriginal` (`original` is its temp file),
the note is written with `commit = true` before the download, and any save that returns clears
it (also `BUSY`, which downloaded nothing). The device check is V2's.

Found by reading (review of F5, 2026-10-20); the fix settled by the user the same day. A stop
during the original's download (the job's roughly 10 minutes run out, or the network goes)
rethrows, as F5 meant, but nothing is noted: `onStopJob` returns true, so the job is retried
with its backoff, and each retry (and each later run) downloads the original from zero. On a
slow connection a 97 MB NASA original may never finish and repeats all day, on mobile data
too. `CLAUDE.md`'s "a 97 MB NASA original is tried once" held only for a failure, which falls
back to the stored picture and is noted.
- **Settled (user, 2026-10-20): one try of the original per post.** Before the job's download
  of an original, note the try (`<source>.saveTried` = the post's picture URL, as `badUrl`); a
  later job save of the same post saves the stored picture at once (the smaller copy), as after
  a failure. A post with another picture URL tries its original again. A finished save clears
  the note. ★ by hand always tries the original (no time limit on the page) and ignores the
  note. (Rejected: two tries, so a short Wi-Fi drop wouldn't leave the smaller copy for good;
  one is simpler, and deleting the copy in the gallery lets ★ save the original.)
- The choice in `:core` (`Saving.kt`): `toSave` also says per post whether to try the original,
  from the noted try, with tests in `SavingTest`: a first try, a tried post, a new post after a
  tried one. A test that fails on F5's code.
- `Store`: `save(post, original = true)` (★ as now); `saveInUse` writes the note before the
  download and passes the flag. Nothing else in `save` changes: a stop still rethrows.
- Files: `core/.../Saving.kt`, `SavingTest`, `Store.kt`, `CLAUDE.md` (Save every picture; Next
  features, Save every picture: "tried once", now also after a stop), F5's device bullet above
  (already says the smaller copy).
- Device: in V2.
- Done when: the tests pass, the build check passes, and `CLAUDE.md` says it.

### [x] C. Early look on the tablet (Opus, medium; on the user's go-ahead)

**Status (2026-10-20, day):** the user saw the screenshots and answered; built (winston, run 10):
⤴ kept, drawn at 28 sp (↗ and ⇪ rejected); the heading "Size" over [Fill | Fit] in both modes;
the credit one line with an ellipsis while the explanation is closed; the ⓘ text "Each new
picture is saved full size to Pictures/APODroid: a few hundred MB a month or more.". In
`CLAUDE.md`.

User, 2026-10-20: the glyph and the texts are seen before V, as a change there is cheap now and
costs a rerun later; but only when the user says the tablet is free (pascaline's until then).
On a debug build of master after F3, F1f1 and F5f1. Screenshots only, with rosie's device rules
(night.md, For rosie: note what you change and set it back), into `.coord/early-look/`
(gitignored), each named for what it shows; show them to the user.
1. ⤴ beside ★ on a picture and at the ▶ tile's corner (a faked video day). Then ↗ and ⇪ in the
   same place, from a temporary build with the glyph swapped (not committed; put ⤴ back).
2. The Fit row in APOD mode and with "All NASA pictures" on. Propose the heading "Size" over
   [Fill | Fit] (the heading "Fit" repeats one of its choices); the user decides.
3. A short credit and the longest of the 100 (251 characters, faked by prefs), the explanation
   closed and open, in APOD mode and with tabs.
4. The ⓘ dialog of "Save every picture".
- The user approves or changes each; the changes go into `MainActivity.kt` and `CLAUDE.md`,
  then the build check, committed when the user says so, before the night.
- Done when: the user has seen all four and their answers are built and in `CLAUDE.md` (F3:
  the glyph; F1, F4, F5: the texts).

### [ ] V. Tablet pass of everything (Opus, high)

After F5, F1f1, F5f1 and C, so it tests the final code (third review: F1 changes the set path,
F5 the job). **By night** (user, 2026-10-20), and a device package gets one rosie session of at
most 50 minutes (`night.md`, For rosie; `pipeline.sh` gives it 3600 s), so V is six queue
rows, V1–V6, each bundled as `plan V + plan V<k>`, and V7 by day after the night (turns over
hours: the night's later rows and the regression reinstall and change the tablet). Every row:
- The final tree: master with everything above committed (`night.sh` refuses an uncommitted
  plan and brings `dev` up to master). Build what the row needs in your worktree:
  `./gradlew :app:assembleDebug`, and for R8 `:app:assembleRelease` signed with the debug key
  (the skill, Working on the phone).
- Rosie's device rules (`night.md`, For rosie): note each setting and the app's prefs and files
  first, set them back at the end. Text evidence first (`dumpsys`, `run-as cat`, `logcat`);
  screenshots for the user into your folder, named for what they show. The how-tos: the skill,
  Working on the phone; `private/wp7-handoff/` (`turn.sh`, `swap.sh`, `ui.sh`, `proxy/`).
- Your verdict: PASS when everything you ran works; FIX for a bug in our app (the steps, what
  happened, the logcat line); STOP for Android doing what the plan didn't expect. The morning
  makes each bug a `B<n>` package.
- Results: in your results commit, the skill's "Still untested" (its 0.6.0 bullet) says what
  this row checked and what is still open; then the report commit ("Your report": Tests, what
  ran on the tablet; Not tested, what didn't and why). The plan's checkboxes stay `[ ]`.
- Done when: each of the row's bullets ran or is under Not tested with the reason, and the
  results are committed.

### [ ] V1. F1, F3 and F1f1 on the tablet

- F1's device bullet (Fit with a wide and a tall picture, home and lock, both modes; a change
  with the daily switch on sets at once and `turnStartedAt` stays; no ⛶ with Fit).
- F1f1: a star field with Fit (APOD's stored picture if it is one, else a made-up one in
  `files/`): a screenshot of the home screen and one of Fill for comparison, for the user; a
  made-up JPEG with EXIF orientation 6 as "my picture" with Fit comes out upright, not
  stretched.
- F3's device bullet (the share sheet with "<title>\n<page link>" on a picture and on the ▶
  tile, from APOD's and NASA's tabs), with screenshots of ⤴ beside ★ for the user (only ⤴: C
  compared the others).

### [ ] V2. F4, F5 and F5f1 on the tablet

- F4's device bullet (a short credit and the longest, 251 characters by prefs; APOD mode and
  with tabs; the explanation open and closed), screenshots for the user.
- F5's device bullet, with F5f1: after a stop during a save (`cmd jobscheduler timeout`) no file
  is left, `<source>.saveTried` holds the URL, and the next run saves the stored picture (the
  smaller copy), not the original again; a post with another picture URL tries its original.
  The ⓘ dialog's screenshot. Delete the test files from the gallery afterwards.

### [ ] V3. M2–M4 again on the final tree

- The hold ending both ways (its turn ends; a tick in its screen's column), and a newer post of
  the held picture's source keeping it.
- A faked APOD video day with each of the four choices ("Skip it", NASA, Earth, my picture) in
  both modes, with turns on one screen: the stand-in in APOD's place, "Skip it" passing the turn.
- A set that fails on one screen and its retry setting only that screen: if nothing on the
  device makes one set fail, say so under Not tested (`ScreensTest` covers the rule).
- Rotation and two panes; font scale 2.0 at `wm density 344` (the phone's width): the grid and
  the status lines with the short names, the Fit row, the credit.

### [ ] V4. What the night of 2026-10-20 didn't check

- The problem box with "All NASA pictures" on when only NASA fails (the proxy stalling
  `www.nasa.gov` only; APOD and Earth are on science.nasa.gov), on APOD's tab and on NASA's.
- The stand-in on a turning screen; the job fetching only the sources in use (the proxy's log).
- The page and the job migrating at the same moment (a 0.5.0 state by `run-as`, then the page
  opened while `run -f` runs): one migration, no lost keys.
- The open page following a job run that changes only the lock screen.
- The photo picker: "Use my picture" with none (the picker opens; backing out keeps the choice
  before; picking sets it at once on a video day with the daily switch on), "Change picture", a
  rotation while the picker is open.
- The chip in APOD mode with "Where" = Lock.

### [ ] V5. The update from 0.5.0, debug builds

- A debug build of 0.5.0 (`git worktree add <your folder>/v050 v0.5.0`, `:app:assembleDebug`
  there) under a debug build of 0.6.0, so `run-as` reads the prefs before and after (a release
  build can't be read). For each `where` (Home, Lock, Both) with APOD alone, NASA alone and two
  sources: the prefs before and after against `CLAUDE.md`'s migration (Stack, Migrations; Next
  features, Migration), the first run sets nothing, the page opens on the right mode and tab.
  Nine states; a script helps. Those not reached go under Not tested. Remove the worktree after.

### [ ] V6. R8 builds

- Once: an R8 build of 0.5.0 under an R8 build of 0.6.0 (both signed with the debug key),
  checked on screen only: the settings kept, the page and the job work.
- On the R8 build of 0.6.0: the smoke list (`night.md`), then both modes, Fit set, the share
  sheet, the credit shown, a save by the job, ★ by hand; no crash (`logcat`, `exit-info`). R8
  isn't covered by the unit tests.

### [ ] V7. Turns over hours (by day, after the night)

- Two short rosie sessions by hand (`claude --agent rosie`, this section), at least 6 hours
  apart, the tablet left alone between them. Setup: the debug build of master, "All NASA
  pictures" on, Home APOD + NASA, Lock NASA + Earth, "Change every" 3 h, daily on; note
  `turnStartedAt`, both records and the job's window. Read-out: `dumpsys jobscheduler`
  (`Last successful run`), `logcat -d -s APODroid`, `dumpsys wallpaper`, the prefs: each turn
  within its window (period and flex), both screens on the one clock, no screen set twice for
  one turn. Real timing, so no `turn.sh`. Then set the tablet back.

### [ ] P. Phone pass on the FP6 (Opus, high)

Before R (user, 2026-10-19, the last pass: a bug P finds would otherwise mean redoing R's
changelog and screenshots). First the version: `versionCode` 10, `versionName` 0.6.0 in
`app/build.gradle.kts`, so the phone gets the real build (and its User-Agent). Then as 0.5.0's
WP8: the release-signed build over the user's Obtainium install (the skill, Testing an update
on the user's phone), the update keeping the settings, both modes, the grid at font scale 1.5
and 2.0, a lock screen with its own picture, landscape; home and lock set separately on Android
16 (M0 checked the tablet only); the picked tab after a swipe-away from Recents (expected:
back to the opening tab; after the system kills the app it stays, `CLAUDE.md`, Tabs); Wi-Fi
only on mobile data (the tablet has no SIM); ⤴ again (the FP6's font may draw it unlike the
tablet's) and Fit on the phone's screen (2026-10-20, review). After V1–V7 and their `B<n>`
fixes. Report what was tested; a bug becomes a
`B<n>` package (night.md, The morning, step 6) before R.

### [ ] R. Docs, changelog, store texts, screenshots (Opus, medium)

After P, on the build that passed. `changelogs/10.txt` (max 500 characters; say that a
video-day choice saved while it was greyed out in 0.5.0, e.g. "Use my picture" with turns, now
works with turns, second review); `README.md`, `full_description.txt` (APOD first, the
switch); `features.md` and this file ticked; the screenshots on the FP6 by the user (APOD mode
first, as `README.md` embeds `1.png`).

### [ ] T. Commit and tag (Sonnet, low)

Only when the user says so; the user pushes.
