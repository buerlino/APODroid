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
  - **On**: always three sources to browse (the title dropdown and the swipe since S6; tabs
    before), whatever is ticked; the shown source's title, explanation and (F4) credit; the
    settings as in "Set applies the setup" below. Named "Extended API mode" since S5.
  - Each mode keeps its own settings: "Where" belongs to APOD mode, the list to the other.
    The first time the switch goes on, the list starts from "Where" (APOD ticked under Home,
    Lock or both). "If APOD is a video" is one setting for both modes.
  - From 0.5.0: APOD ticked alone → APOD mode, all else unchanged; **anything else → on**
    (third review: 0.5.0 lets NASA or Earth be ticked alone, `MainActivity.kt:991`, and such a
    user must keep their source), with the ticked sources in the columns "Where" named (Both:
    both columns).
  - It's part of the setup: Set applies it (S4), and the job is rescheduled (its period
    depends on the mode). Switching it off leaves a screen "Where" doesn't name alone, also
    when the list had put NASA's or Earth's picture there (user, 2026-10-19: as 0.5.0 when
    "Where" goes from Both to Home; "Both" puts APOD there).
- **The list** (only with the switch on; the grid until S2): per source a tick box, its short
  name and a dropdown Home screen / Lock screen / Both. An unticked source's dropdown is greyed
  out and keeps the screens it had last. Underneath it's two columns, `home` and `lock`.
  - Both screens due the same picture: one set with both flags, one wallpaper, as today. An
    empty column: that screen is left alone. The last tick can't be removed.
  - A column with two or more ticks takes turns. "Change every" shows while a column has two
    or more. **One shared clock**: both screens change at the same moment. It restarts when the
    turn ends and on Set (S4), not when one screen changes early (an untick, a source with no
    picture yet, an APOD video with "Skip it"): a source that comes in early gets the rest of
    the current turn, even if short, so the screens stay in step.
  - Each screen's record notes the turn it belongs to (the clock's value; third review). A
    screen with turns whose record isn't from the current turn is due its next source; after
    its set (or when no other source has a picture) the record takes the current value. So a
    failed set on one screen is retried without moving the other on again.
  - **Set forgets both records** and restarts the clock (`restartAndForget`, S4): the picture
    goes, whose turn it is stays, and the screens on the current turn take the new value, so
    each screen gets what the setup says afresh (a changed Fit or "my picture" too).
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
  - Its control is a dropdown (it closes after a choice and opens again with a tap), after
    "Size" (S2), greyed out with the switch on while APOD has no tick (so the page doesn't move,
    as U4 decided), shown only while the draft's daily switch is on (S5). Choosing "Use my
    picture" with nothing picked opens the photo picker at once; "Change picture" beside the
    dropdown afterwards.
- **The page with the switch on:** the "On wallpaper" chip becomes "On home screen", "On lock
  screen" or "On wallpaper" (both), on the tab of the picture's source (a stand-in NASA picture:
  NASA's tab, not APOD's); the status line has a line per screen while they differ
  ("Home screen changed 16 Oct, 06:12 · NASA Image of the Day"); the video notice also shows
  with turns (it tells the user it was a video day); with APOD ticked nowhere it only says the
  APOD is a video (nothing takes its place). APOD mode with "Where" = Lock reads the lock
  screen's record (after the migration home's is empty there).
- **Fill / Fit (F1): one setting for both screens** (user, 2026-10-19), as `features.md` says,
  so "one set when both screens are due the same picture" holds.
- **Data:** with the switch on the page fetches all three sources (three fresh pictures); the job
  fetches only what the screens need: the ticked ones and the stand-in on an APOD video day
  (since S1 nothing is held by hand), only while APOD is
  ticked on some screen (or in APOD mode). The page offers, and fetches, the sources of its
  draft's mode (S4), so a source ticked in the draft only has a picture to show. The page-open hour rule (WP7c) applies to all
  three.

All texts above are placeholders; the user approves them on the device.

## Set applies the setup (user, 2026-10-20 evening; S4–S6)

From the user's look at S2 on the tablet and a mockup of four phone screens, approved the same
day (https://claude.ai/artifact/BqfZJYLPEChxiY6MGKf6Nc; everything it shows is written out
below, so nobody needs to open it). It replaces the afternoon's "a set by hand changes the
settings" (S1) and S3; the gaps are settled from a review of the plan by reading (same
evening). S4 writes it into `CLAUDE.md` (The app, Settings; Background update; Next features)
when it lands; until then it lives here (S2 changes the same lines of `CLAUDE.md`). The texts
are placeholders.

Why: a change took effect at once, "but maybe I'm not done". One system for both modes, though
in APOD mode a change of "Where" alone would be safe to set at once.

- **A draft until Set.** The daily switch, "Where", the list (ticks and screen dropdowns),
  "Change every", "Size", "If APOD is a video", a newly picked "my picture", ⛶'s part and
  "Extended API mode" change only the page. The job keeps the setup last set. A rotation and
  the system killing the app keep the draft (Android's saved state); closing the app drops it.
  - **The daily switch is part of the draft** (the user's choice, asked directly). It sets
    nothing by itself any more. Kept after the review pointed out that a switch usually acts
    when flipped: off, then leaving the app, keeps the job running and reopens with the switch
    on. The alternative, should the user want it after seeing S4: off applies at once (the
    safe way), on waits for Set.
  - **"My picture"** is a file: picking one copies it to `files/fallback-draft.jpg`; Set renames
    it to `fallback.jpg`. A start without a saved draft deletes a leftover draft file.
  - **⛶'s part:** ✓ closes crop mode and keeps the part in the draft (per source: the post's
    date and picture URL, the position); Set stores it, the job uses only the stored one; a
    new post drops a draft part, as a new picture starts centred. ✓ no longer sets the
    wallpaper (it did since 0.3.1).
- **Two kinds of state on the page.** The controls and the sources the page offers (the title
  dropdown and the swipe; the tabs until S6) follow the draft: turning "Extended API mode" on
  in the draft offers all three at once and fetches them. Everything that reports what is set
  follows the stored setup: the status lines (only while the stored daily switch is on), the
  chip, the video notice, the problem box, the battery hint, the Setup summary.
- **The job follows the stored setup only:** `onResume`'s `DailyJob.ensure`, the reschedule
  after "Wi-Fi only" and every other scheduling read `store.daily` and the stored mode and
  columns, never the draft. Only Set schedules or cancels by the draft.
- **"Set as wallpaper"**, one button, both modes, the daily switch on or off, no Home / Lock /
  Both menu, always shown. It saves the whole draft in one commit, fetches what a newly used
  source needs, sets both screens afresh (the clock restarts, both records forgotten) and
  schedules or cancels the job as the daily switch says. With the daily switch off a one-off
  set of the same setup. The shown picture decides nothing: to put Earth on a screen, tick
  Earth. (0.5.0 could set the shown picture; dropped with the user's "apply the setup, no
  menu". Kept after the review asked whether the label should be "Apply"; the user approved
  the mockup with "Set as wallpaper".)
  - A source whose fetch fails at Set: the setup is saved anyway, and each screen gets what
    has a picture by the rules of Turns (the source with nothing yet is skipped, as an early
    change). Toasts: "Wallpaper set" when a screen was set; "Setup saved. Nothing to set yet."
    when none was (an APOD video with "Skip it" alone, or nothing stored and the fetch failed:
    the problem box says why); "Couldn't set the wallpaper" when a set failed. With the daily
    switch on the job sets the rest later.
- **At once, as before:** "Wi-Fi only" and "Save every picture" (they don't change a picture;
  their reschedule reads the stored daily switch), and ★.
- **Unset changes stand out:** a changed setting's heading and control take the theme's
  `tertiary` colour until Set, then turn back to normal (orange stays the problem box's). No
  extra line. ⛶ too, while its part differs from the stored one.
- **"Change every"** [24 h | 12 h | 6 h | 3 h] ("Day" becomes "24 h"; the pref stays `DAY`).
- **"Extended API mode"** is the new name of the switch "All NASA pictures" (the pref stays
  `allSources`).
- **The title instead of the tabs:** the shown source's short name ("APOD", "NASA", "Earth")
  above the picture, large; with two or more sources a dropdown ("APOD ▾", ✓ before the shown
  one). With one source the title alone. **Swiping** left or right on the title and the
  picture shows the next or the previous source, sliding with the finger and stopping at the
  ends; in crop mode a drag moves the frame instead. Only the title and the picture slide; the
  boxes and the caption below show the page that has settled (so the height doesn't jump while
  dragging).
- **No Share:** the ⤴ button goes (F3's code removed).

**The page, in order** (one column on the phone; from 600 dp the picture side left, the
settings right, as now):

```
APOD ▾                                    the shown source; ▾ and its menu with two or more
┌──────────────────────────────┐          ← swipe →: the previous / next source (title and
│ On home screen               │            picture slide, stopping at the ends)
│                              │
│ ⛶                         ☆ │          the chip top left; ⛶ and ★ at the bottom
└──────────────────────────────┘
(the boxes: problem, video notice, battery hint)
Astronomy Picture of the Day · 20 October 2026     (with two or more sources; else the date
Title                                                under the title)
Credit & Copyright: …
The explanation's first two lines, cut off …
more
──────────
Wallpaper
[switch] Change wallpaper automatically
         Home screen changed 20 Oct, 06:12 · NASA   the status lines: what was set, while the
         Lock screen changed 20 Oct, 05:05 · APOD   stored daily switch is on
Setup ▸                                           closed: the header and the summary of what
APOD on both screens, NASA on home screen · turns every 24 h · Fill      is set
  (open, Setup ▾:)
  Where   [Home screen | Lock screen | Both]       APOD mode
  Pictures from                                    Extended API mode
    [✓] APOD    [Both ▾]
    [✓] NASA    [Home screen ▾]
    [ ] Earth   [Both ▾]                           greyed out, the screens it had last
  Change every  [24 h | 12 h | 6 h | 3 h]          draft's daily switch on, a screen takes turns
  Size    [Fill | Fit]
  If APOD is a video  [Skip it ▾]                  draft's daily switch on only
[        Set as wallpaper        ]                 always
Advanced settings ▸                                closed by default
  [switch] Wi-Fi only                              draft's daily switch on
  [switch] Save every picture ⓘ                    draft's daily switch on
  [switch] Extended API mode
```

- **Setup** opens by itself while the draft differs from the store (the mockup opens it with a
  change), and a set that worked closes it. Turning "Extended API mode" opens it too, since
  "Where" and the list swap.
- **The summary** says what is stored, not the draft: "APOD on both screens" (APOD mode), else
  each ticked source "<short name> on <home screen | lock screen | both screens>", joined by
  ", "; then " · turns every <interval>" while a screen takes turns and the daily switch is on;
  then " · Fill" or " · Fit".
- **The highlight** in the mockup: the daily switch's label and track, a segmented row's chosen
  segment (`tertiaryContainer` / `onTertiaryContainer`) and its borders, a list row's tick box,
  name and dropdown, the video dropdown's border and text, the "Setup" header while anything in
  it differs, "Advanced settings" while the mode does.

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
| S1 | `:core` and `Store`: a set by hand changes the settings, no holds | Opus, high |
| S2 | Settings: the list instead of the grid, the order, the daily switch off | Opus, medium |
| S3 | The page: the set button under the picture, its menu, the highlight (dropped) | Opus, medium |
| S4 | Set applies the setup: the draft, `Store.saveSetup`, the old set paths gone | Opus, high |
| S5 | Settings: Setup and Advanced settings, the highlight, "24 h", "Extended API mode" | Opus, medium |
| S6 | The picture side: the title dropdown, swipe between sources, no Share | Opus, high |
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
scripts/pipeline.sh S2 --no-worker --tablet no    # S2's review and merge (S1 is merged)
scripts/pipeline.sh S4    # then S5, then S6: by day, one at a time, the user looks at each
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
3a. **S1, S2, then S4–S6** (user, 2026-10-20: S1 and S2 in the afternoon, a set by hand that
   changes the settings; S3 dropped in the evening for "Set applies the setup"). S1 merged.
   S2 by pipeline review-only without its tablet check (`--no-worker --tablet no`: that check
   tests ticks that apply at once and a layout S4 and S5 replace). Then S4, S5 and S6 by day,
   one at a time by pipeline, the user looking at each on the tablet before the next. V3, V5
   and V6 wait for S6 (by night), as they test the final tree.
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

### [ ] S1. `:core` and `Store`: a set by hand changes the settings, no holds (Opus, high)

The decisions are above ('"Set as wallpaper" changes the settings'). This package builds the
rules and their wiring. The page keeps today's controls (the grid, the buttons where they
are) and only redraws the settings a set changed. The list is S2; the button under the
picture, its menu in APOD mode and the highlight are S3.
- **`:core` (`Screens.kt`):** a pure function for the set, in place of `setByHand`. It takes
  the shown tab's source (APOD's video tab and APOD mode's stand-in tab count as APOD), the
  chosen screens, the mode, the columns, the clock, the records, `Stored` and `now`. It
  returns the new settings (APOD mode: the new "Where"; else the new columns) and a `Plan`:
  - **The settings.** APOD mode: APOD on exactly the chosen screens. With the switch on: the
    source in exactly the chosen screens' columns (and out of the other's), the other sources
    unchanged.
  - **The clock** restarts (to `now`) when a chosen screen's new column has two or more.
    - The chosen screens' records get the source as owner and the new stamp.
    - The other screen's record takes the new stamp when it had the current one (as today's
      restart by hand) and keeps its owner and picture.
    - If the other screen lost the source, the rules of a tick apply (`columnsChanged`: its
      owner unticked, an early change).
    - A column that goes from empty to the source starts on it.
  - **Settings already so:** a chosen screen that takes turns and shows another source starts
    the source's turn now. One already on the source, or a one-source screen, only gets its
    due picture set again (e.g. ✓ with a new part).
  - **Nothing to set** (the tab's picture is null: "Skip it" on APOD's video tab, or nothing
    stored): no change at all, and the caller says so, as today.

  The caller then writes the settings and the plan and runs the normal set (`setDue`), which
  puts the source's picture on the chosen screens (on an APOD video day, the stand-in).
- **Gone:**
  - `Hold`, its branch in `dueOn`, its clearing in `columnsChanged`, and `Record.hold`.
  - The hold's keys in `Store`. A dev build's leftover keys are ignored; 0.6.0 isn't released,
    so there's no migration.
  - The held pictures' sources in `sourcesInUse` (the job's fetch and Save every picture).
  - `hold?.source` in `settingChanged(VIDEO_DAYS)` (the owner alone decides).
- **`Store.setByHand(tab, screens)`:**
  - With the daily switch on: the new settings (`where`, or the columns as `setColumns` writes
    them), the plan, then `setDue`, under the wallpaper lock. It returns whether it set and
    what setting changed (the source or "Where"; S3 lights it up).
  - With the daily switch off: a one-off set on the chosen screens. The records are noted so
    the chip shows it; the settings stay untouched.
- **The page (`MainActivity.kt`, only what this needs):** after a set, read the settings again
  (`where`, `columns`, `interval`), so the grid, "Where" and "Change every" show the change.
  APOD mode's button keeps setting the screens "Where" names until S3's menu.
- **Tests (`ScreensTest`):** the hold's tests go. New ones cover each rule above, plus the cases
  the simulation of 2026-10-20 found, each as the settings after the set and the due pictures
  over the next posts:
  - Home NASA, Lock APOD; APOD's tab → Both: Home takes turns APOD/NASA with APOD now, and
    Lock is APOD.
  - Home all three, Lock NASA + Earth, 6 h; Earth → Home only: Earth now with a new turn; Lock's
    picture stays, with the new stamp.
  - Home Earth, Lock APOD, stand-in NASA; APOD → Both, then an APOD video: Home has APOD ticked,
    so its APOD turn shows NASA as the stand-in, by the settings.
  - APOD mode, "Where" = Lock; Home chosen: "Where" is Home, and the lock screen is left alone
    with its picture.
  - APOD mode on a video day, the stand-in's tab → Both, then the video-day choice changed to
    Earth: Earth goes on. This is the case that failed.
  - "Skip it" on APOD's video tab: nothing changes.
- **`CLAUDE.md`:** the new rules, with the date and the why, in place of the hold's:
  - The app, Settings: "Set as wallpaper now".
  - Background update: Turns' "A set by hand", and the sources in use.
  - Save every picture: the set in use.
  - Next features: 0.6.0's "Set as wallpaper" bullet.

  Also the skill's "Where things are" (`Screens.kt`, `Store.setByHand`).
- **Files:** `core/.../Screens.kt`, `Saving.kt`, their tests, `Store.kt`, `MainActivity.kt` (the
  re-read), `CLAUDE.md`, the skill's `SKILL.md`.
- **Device (tablet, debug build):**
  - With the switch on, Home NASA, Lock APOD (the grid), APOD's tab → Both: the grid shows APOD
    on both and NASA on Home, and "Change every" appears. Check `dumpsys wallpaper` and the
    prefs (`home`, `lock`, `turnStartedAt` now, both records).
  - Earth → Home only, with turns on the lock screen: the lock screen's picture is unchanged and
    `lock.wallpaperStamp` is the new clock.
  - APOD mode: the button still sets the screens "Where" names, with no setting changed.
  - A faked APOD video day with "Skip it", APOD's tab: the toast, and the prefs unchanged.
  - The daily switch off: a set changes no setting.
  - Then `run -f` sets nothing new. No crash (`logcat`, `exit-info`).
- **Done when:** the tests pass (the hold gone, the new rules and the simulation's cases
  covered), the device checks pass, and `CLAUDE.md` says the new rules and no longer the
  hold.

### [ ] S2. Settings: the list instead of the grid, the order, the daily switch off (Opus, medium)

After S1. The layout is "Settings layout afterwards" above; the texts are placeholders.
- **The list.** With the switch on, "Pictures from" is a list with a row per source:
  - Each row has a tick box, the short name ("APOD", "NASA", "Earth") and a dropdown Home
    screen / Lock screen / Both: an outlined button with ▾ and a `DropdownMenu`, like "If APOD
    is a video"'s.
  - Ticking puts the source on its dropdown's screens, unticking takes it off both, and a new
    dropdown choice for a ticked source moves it. Each change goes through
    `Store.setColumns`, so the rules of a tick hold (the page's update sets what changes).
  - An unticked row's dropdown is greyed out and shows the screens the source had last (a pref
    per source; Both when it never had any). Ticking it again puts the source there.
  - The last ticked row can't be unticked (greyed out, as the last tick today).
  - The weekday dialog stays as today: ticking NASA or Earth while it's on neither screen.
  - Screen readers: the tick box "NASA", the dropdown "NASA on: Home screen".
- **"If APOD is a video"** leaves APOD's row. In both modes it comes after "Size", under that
  heading; with the switch on it's greyed out while APOD isn't ticked (so the page doesn't
  move).
- **The order while the daily switch is on:**
  1. The status lines.
  2. "Where" (APOD mode), or the list and "Change every" (while a screen takes turns).
  3. "Size", then "If APOD is a video", "Wi-Fi only" and "Save every picture".
  4. The divider, then "All NASA pictures".

  The set buttons stay until S3: APOD mode's at the end of its section, the menu after "Change
  every".
- **While the daily switch is off:** only "Size" under the switch, then the divider and "All
  NASA pictures". "Where", the list, "Change every" and "If APOD is a video" are hidden, as
  "Wi-Fi only" and "Save every picture" already are. The set buttons stay visible in both
  states until S3 moves them to the picture side.
- **Files:** `MainActivity.kt` (`Settings`; `Grid` becomes the list), `Store.kt` (the remembered
  screens per source), `CLAUDE.md` (The app, Settings: the list, the order, the daily switch
  off), the skill's "Where things are".
- **Device (tablet):**
  - The list in both orientations and in two panes.
  - Font scale 2.0 at `wm density 344`: no word broken inside, and the dropdowns fit.
  - Ticks and dropdown choices change the prefs `home` and `lock` and the wallpaper, as a tick
    does today.
  - The greyed-out rows, and the weekday dialog.
  - The daily switch off: only "Size" and the mode switch.
  - The labels in the accessibility tree (`uiautomator dump`).
  - A screenshot of each state, for the user.
- **Done when:** the device checks pass, the screenshots are there for the texts, and
  `CLAUDE.md` says the layout.

### [–] S3. (dropped) The page: the set button under the picture, its menu, the highlight (Opus, medium)

Dropped (user, 2026-10-20 evening): replaced by S4–S6 ("Set applies the setup"); git keeps
its text.

### [ ] S4. Set applies the setup: the draft, `Store.saveSetup`, the old set paths gone (Opus, high)

After S2. The behaviour of "Set applies the setup" above; S2's layout mostly stays (S5
rearranges it, S6 the picture side), so the tablet sees the new rules first.
- **`Store`:**
  - `Setup(daily, allSources, where, list, interval, fit, videoDays, parts)` and
    `saveSetup(setup)`: one commit with the settings (`putColumns` for the list, so
    `lastScreens` follows) and the parts, the draft "my picture" renamed into place, then
    `restartAndForget` (the clock restarts, both records forgotten: each screen gets what the
    setup says afresh, a changed Fit or "my picture" too, with the same picture).
  - `list`: the list's columns whatever the mode (`home`, `lock`; before the first time on,
    APOD on "Where"'s screens), for the page's draft; `columns` uses it with the mode on.
  - `update(force, page, show, set)`: `show`, the sources the page offers, fetched too (a
    source ticked in the draft only); `set`, also with the daily switch off (the button). It
    returns what Set's toast needs: a screen set, nothing to set, a set failed.
  - Gone: `setByHand` and `HandSet`, `settingChanged` and `Changed`, `setAllSources`,
    `setColumns`, `forgetWallpaper`; in `:core` `setByHand` (`planByHand`) and
    `columnsChanged` with their tests and the comments that name them. Check that what
    `Changed.FIT` and "Change picture" did (forget the records, then set) is now Set's
    `restartAndForget`, and that nothing else called them.
- **`MainActivity`:**
  - **The draft:** `daily`, `where`, `list`, `interval`, `fit`, `videoDays`, the draft "my
    picture", the parts and `allSources` change only the page's state (no `store.x =`, no
    `applyNow`), kept in `onSaveInstanceState`; `columns` and `ticked` computed from it;
    `lastScreens` follows the draft as now. `load()` passes `show` = the sources the draft's
    mode offers.
  - **The stored setup**, read from `Store` (on start, after Set, from the prefs listener),
    drives the status lines (while the stored `daily` is on), the chip, the video notice, the
    problem box, the battery hint and the tab the page opens on.
  - **The job reads only the store:** `onResume`'s `if (daily && !DailyJob.ensure(...))` and
    `rescheduleJob` use `store.daily`; "Wi-Fi only" writes its pref at once and reschedules only
    when `store.daily` is on.
  - **"My picture":** the picker copies to `files/fallback-draft.jpg` and the draft notes it;
    "Use my picture" with no picture in either file opens the picker, as now. `onCreate`
    without a saved state deletes a leftover draft file.
  - **⛶'s part:** ✓ closes crop mode ("Done" for screen readers) and keeps the part in the
    draft; reopening ⛶ shows the draft's part. Back still closes it without keeping.
  - **One "Set as wallpaper"** at the end of the Wallpaper section, where APOD mode's button is
    now, in both modes and with the daily switch on or off (S5 moves it after "Setup"). It
    runs `saveSetup`, then `DailyJob.schedule` or `cancel` as the draft's daily switch says,
    `update(page = true, show, set = true)` off the main thread, then re-reads the stored
    setup; the toasts of "Set applies the setup". `SetMenu`, `SetChoices`, `canSetNow`,
    `readSettings`, `applyNow`, `switchAllSources`, `chooseFit`, `chooseVideoDays`,
    `chooseInterval` (as setters) and `switchDaily`'s set go.
  - **With the daily switch off**, "Where" (APOD mode) or the list (the switch on) shows again
    (S2 hid them), so a one-off Set uses settings the user can see. "Change every" and "If APOD
    is a video" stay hidden then, as in the layout.
  - **"Extended API mode"** in the draft swaps "Where" and the list at once (as now) and the
    sources the page offers.
- **Files:** `Store.kt`, `MainActivity.kt`, `core/.../Screens.kt`, `ScreensTest`, `CLAUDE.md`
  ("Set applies the setup" into The app, Settings and the button, ✓, "my picture"; Background
  update and Turns: no set by hand, the clock restarts on Set; Next features, 0.6.0; remove
  what it replaces: the holds' last traces, "a set by hand changes the settings", "turning the
  switch on sets today's picture again", the mode switch working like a tick), the skill's
  "Where things are".
- **Tablet (debug build):**
  - A draft change sets nothing: `dumpsys wallpaper` ids, the prefs and `dumpsys jobscheduler`
    unchanged, for each setting, the daily switch too (on → no job; off → the job stays).
  - Set puts on each screen what the setup says: APOD mode with each "Where"; Extended API mode
    with a column that takes turns (`turnStartedAt` now, both records fresh); the daily switch
    off: a one-off set, the job cancelled; on: the job scheduled.
  - Set after changing only Fit: both screens set again with the new size.
  - A rotation and `am kill` in the background keep the draft; `am force-stop` and a restart
    drop it (and the draft "my picture" file is gone).
  - ✓ sets nothing; Set afterwards uses the part.
  - Set with a newly ticked source while its host is blocked (the proxy in
    `private/wp7-handoff/proxy/`): the toast, the problem box, the setup saved. If the proxy
    can't be set up, say so under Not tested.
  - "Wi-Fi only" with the stored daily switch off schedules nothing.
  - No crash (`logcat`, `exit-info`).
- **Done when:** the checks pass, the tests pass (the removed `:core` functions' tests gone,
  nothing left that names them), the tablet passes and `CLAUDE.md` says the new rules.

### [ ] S5. Settings: Setup and Advanced settings, the highlight, "24 h", "Extended API mode" (Opus, medium)

After S4. The layout, the summary and the highlight of "Set applies the setup" above.
- "Setup": a header row ("Setup ▸/▾", the summary under it while closed) that opens and closes
  the area (`rememberSaveable`); it opens by itself while the draft differs from the store in
  it, and closes after a set that worked.
- "Set as wallpaper" moves after "Setup", always shown.
- "Advanced settings": a header row, closed by default (`rememberSaveable`), with "Wi-Fi only"
  and "Save every picture" (draft's daily switch on) and "Extended API mode"; turning that
  opens "Setup". The divider before the mode switch goes.
- The highlight: each setting compared with the store; while it differs its heading and its
  control in `tertiary` (segmented buttons' active colours `tertiaryContainer` /
  `onTertiaryContainer` and their border, the checkbox and the switch `tertiary`, the outlined
  dropdowns' border and text); the list per row; ⛶ while its part differs; the "Setup" header
  while anything in it differs, "Advanced settings" while the mode does.
- "If APOD is a video" only while the draft's daily switch is on. "Day" → "24 h". The switch's
  label "Extended API mode" (and the comments that name it).
- Screen readers: the headers as buttons with "expanded"/"collapsed" (`Role.Button`,
  `stateDescription`); a highlighted setting adds "not set yet" to its state.
- **Files:** `MainActivity.kt`, `CLAUDE.md` (Settings), the skill's "Where things are".
- **Tablet:** screenshots for the user of each state (closed with its summary, open, a change
  highlighted, after Set, Advanced open, the daily switch off), both modes, two panes and a
  phone-width window; font scale 2.0 at `wm density 344` (no word broken inside); the
  accessibility tree (`uiautomator dump`).
- **Done when:** the checks pass, the tablet passes, the screenshots are there for the user.

### [ ] S6. The picture side: the title dropdown, swipe between sources, no Share (Opus, high)

After S5 (both change `MainActivity.kt`).
- `Tabs()` → `SourceTitle()`: the short name at `headlineSmall`; with two or more sources "▾"
  and a `DropdownMenu`, ✓ before the shown one; screen readers "Picture from: NASA Image of
  the Day" and its role.
- Swipe: the title and the picture (or ▶ tile, with ⛶, ★ and the chip) in a `HorizontalPager`
  over the sources the page offers; the boxes and the caption below it show the settled page
  only. The settled page is the shown source (`tab`); the dropdown moves the pager
  (`animateScrollToPage`). A neighbour shows while dragging, so each page draws its own
  source's stored post: the posts and the decoded pictures per source (`decodeForScreen`, at
  most three in memory, about 9 MB each for a rendition, up to 32 MB for a tall PNG), loaded as
  `show()` loads one now. `userScrollEnabled = !cropping`, so a drag in crop mode moves the
  frame. Pull to refresh still covers the picture side; two panes keep it in the left one. The
  pick survives rotation as now.
- Share goes: `ShareButton`, `share()`, the ⤴ beside ★ and on the ▶ tile.
- **Files:** `MainActivity.kt`, `CLAUDE.md` (The app: the title, the swipe; Share removed),
  the skill's "Where things are".
- **Tablet:** `input swipe` left and right in both directions, stopping at APOD and at Earth,
  the title following; the dropdown; crop mode (a drag moves the frame, no page change); the ▶
  tile on a page; a rotation keeps the page; one source (APOD mode): no ▾, no swipe; no ⤴;
  the accessibility tree; screenshots for the user; no crash and no memory warning in
  `logcat` while swiping back and forth.
- **Done when:** the checks pass, the tablet passes, the screenshots are there for the user.

### [ ] V. Tablet pass of everything (Opus, high)

After F5, F1f1, F5f1 and C (V3, V5 and V6 also after S1, S2 and S4–S6), so it tests the final code (third review: F1 changes the set path,
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

- Set (S4) in both modes, with the daily switch on and off: a draft sets nothing, Set puts on
  each screen what the setup says and starts a fresh turn; then the job's next run sets what
  the settings say, and nothing is held.
- A faked APOD video day with each of the four choices ("Skip it", NASA, Earth, my picture) in
  both modes, with turns on one screen: the stand-in in APOD's place, "Skip it" passing the turn.
- A set that fails on one screen and its retry setting only that screen: if nothing on the
  device makes one set fail, say so under Not tested (`ScreensTest` covers the rule).
- Rotation and two panes; font scale 2.0 at `wm density 344` (the phone's width): the list and
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
- On the R8 build of 0.6.0: the smoke list (`night.md`), then both modes, Fit set, the credit shown, a save by the job, ★ by hand; no crash (`logcat`, `exit-info`). R8
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
on the user's phone), the update keeping the settings, both modes, the list at font scale 1.5
and 2.0, a lock screen with its own picture, landscape; home and lock set separately on Android
16 (M0 checked the tablet only); the shown source after a swipe-away from Recents (expected:
back to the opening tab; after the system kills the app it stays, `CLAUDE.md`, Tabs); Wi-Fi
only on mobile data (the tablet has no SIM); the title dropdown and the swipe (S6); and Fit on the phone's screen (2026-10-20, review). After V1–V7 and their `B<n>`
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
