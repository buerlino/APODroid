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
  - From 0.5.0: APOD ticked alone → APOD mode, all else unchanged; two or more ticked → on,
    with the ticked sources in the columns "Where" named (Both: both columns).
  - Turning it on or off while the daily switch is on works like a tick: whatever the new mode
    puts on a screen is set at once, and the job is rescheduled (its period depends on the
    mode). It clears every hold (the new mode's settings decide what shows).
- **The grid** (only with the switch on): a row per source (full names), a tick box per screen
  (Home, Lock). It replaces the chips and "Where".
  - Both screens due the same picture: one set with both flags, one wallpaper, as today (with
    equal columns that's the normal case; a picture set by hand on one screen makes them differ
    until its hold ends). An empty column: that screen is left alone. The last tick can't be
    removed.
  - A column with two or more ticks takes turns. "Change every" shows while a column has two
    or more. **One shared clock**: both screens change at the same moment. It restarts only
    when the turn ends or on a set by hand (user, 2026-10-19, second review), not when one
    screen changes early (an untick, a source with no picture yet, an APOD video with "Skip
    it"): a source that comes in early gets the rest of the current turn, even if short, so
    the screens stay in step. Ticking a second source into a column restarts it only when no
    column took turns before (so the tick doesn't end the current picture at once, as today).
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
- **"Set as wallpaper" stays**, at the end of the settings' wallpaper part. In APOD mode as now
  ("Where" says where). With the switch on it opens a small menu **Home screen / Lock screen /
  Both**, since the shown tab may be ticked nowhere; ✓ in crop mode opens the same menu.
  **The hold** (user, 2026-10-19, after the plan's review): a picture set by hand stays until
  that screen's next normal change: on a screen with turns its turn ends; on a screen with one
  ticked source that source posts a new picture (e.g. tomorrow's APOD). Meanwhile a newer post
  of the held picture's own source is set at once and keeps the hold (as for the source on
  the wallpaper today). Unticking a source on that screen ends the hold and changes the screen
  at once. A held APOD whose next post is a video follows the video-day rule (the stand-in in
  its place; "Skip it" keeps the held picture), and the hold runs to its normal end (user,
  2026-10-19). A picture set on a screen with no ticks stays for good: that screen is left
  alone, so it needs no hold.
  On APOD's video tab the button (and ✓) sets the stand-in; with "Skip it" nothing ("Video day:
  wallpaper kept", as now).
- **The page with the switch on:** the "On wallpaper" chip becomes "On home screen", "On lock
  screen" or "On wallpaper" (both), on the tab of the picture's source (a stand-in NASA picture:
  NASA's tab, not APOD's); the status line has a line per screen while they differ
  ("Home screen changed 16 Oct, 06:12 · NASA Image of the Day"); the video notice also shows
  with turns (it tells the user it was a video day).
- **Data:** with the switch on the page fetches all three sources (three fresh tabs); the job
  fetches only what the screens need (the ticked ones, and the stand-in on an APOD video day).
  The page-open hour rule (WP7c) applies to all three.

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
| M5 | Tablet pass of M1–M4 (debug and R8), the update from 0.5.0 | Opus, high |
| F1 | Fill / Fit | Opus, medium |
| F2 | Wi-Fi only | Sonnet, medium |
| F3 | Share | Sonnet, medium |
| F4 | Image credit | Opus, medium |
| F5 | Save every picture | Opus, medium |
| R  | Docs, version, changelog, store texts, screenshots | Opus, medium |
| P  | Phone pass on the FP6 (release-signed) | Opus, high |
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
claude --model opus --effort high "Do M5 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort medium "Do F1 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model sonnet --effort medium "Do F2 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model sonnet --effort medium "Do F3 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort medium "Do F4 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort medium "Do F5 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort medium "Do R in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort high "Do P in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model sonnet --effort low "Do T in .claude/skills/build-apodroid-android/plan-0.6.0.md"
```

Devices: the skill, Working on the phone. The tablet is free for testing; the FP6 is the
user's own phone, touched only in P. The build check:
`ANDROID_HOME=~/Android/Sdk ./gradlew :core:test :app:lintRelease :app:assembleRelease`.

## Remaining

### [ ] M0. Android check: home and lock set separately (Opus, medium)

First, because M1's grouping rests on it (second review, 2026-10-19). No new code: 0.5.0's
"Where" already sets with `FLAG_SYSTEM`, `FLAG_LOCK` or both. On the tablet only (user,
2026-10-19: not on the FP6, which runs Android 16; P sees it there). Note the tablet's
wallpapers first (`dumpsys wallpaper`, screenshots of both screens) and put them back after.
1. "Both", set a picture: one wallpaper, no separate lock wallpaper in `dumpsys wallpaper`.
2. "Home screen", set another: the lock screen keeps the first picture (Android copies the
   shared one to the lock screen first).
3. "Lock screen", set a third: the home screen keeps the second.
4. "Both" again, set one: both screens show it, and `dumpsys wallpaper` shows one shared
   wallpaper again (the separate lock one gone).
Screenshots of both screens after each step. If a screen goes blank or changes when it
shouldn't, stop and report: the grouping in M1 has to follow what Android does. Done when the
four results are reported and `CLAUDE.md` (Wallpaper) notes them.

### [ ] M1. `:core`: screens, columns, the stand-in and the hold (Opus, high)

Pure logic first, so M2 only wires prefs and files to it. Files: a new
`core/.../Screens.kt` (or `Turn.kt`, whichever reads better), `Turn.kt`, `TurnTest.kt`, a new
test file.
- `Screen` { HOME, LOCK }. The columns: `home: List<Source>`, `lock: List<Source>`; in APOD
  mode they come from "Where" (`listOf(APOD)` in the columns it names).
- **One reading of the clock** (review, 2026-10-19): the due picture of every screen is worked
  out from the same `turnStartedAt` and `now` before anything is set. Today `setWallpaper`
  restarts the clock whenever the source changes (`Store.kt:473`); worked out one screen after
  the other, Home's change would restart it before Lock is checked, and Lock would keep its
  picture a whole extra turn. So the function returns all screens' due pictures and whether
  the clock restarts: once, only when the turn ends (Decisions: not when a screen changes
  early) or on a set by hand.
- **Grouping by the due picture, not by the columns** (review): both screens due the same
  picture and both needing it → one set with both flags; otherwise each screen that needs a
  set on its own. Equal columns alone don't make one wallpaper: a hand-set picture on Home only
  must not be overwritten on Lock. An empty column is left alone.
- **Whose turn per screen:** `nextTurn` per screen with the shared clock, plus one input: the
  screen's **hand-set mark** (the hold). Today `nextTurn` ends a turn at once when the current
  source isn't ticked; an unticked source now stays while the mark is set. The mark can't be
  the clock (ending one screen's turn that way would end the other's). The hold ends at the
  screen's next normal change (Decisions): on a screen with turns when the clock's turn ends;
  on a screen with one ticked source when that source's stored post is newer than the one
  noted in the mark (so the mark notes it). A newer post of the held source itself is due at
  once and keeps the mark. Unticking a source on that screen clears the mark (M2/M3), and so
  does a mode switch. A held APOD whose next post is a video: the stand-in rule fills it
  ("Skip it" keeps the held picture), the mark stays. No mark on a screen with no ticks: it's
  left alone, so its picture stays anyway (say so in the doc, so nobody adds one).
- **The stand-in:** on an APOD video day, APOD's turn is filled by the video-day choice:
  skip (a screen with APOD alone keeps its picture; with turns the next source with a picture),
  NASA's or Earth's stored picture, or the user's own. Whose turn it is (APOD) and which
  picture shows (the stand-in's) are two things now: the screen's record keeps both (M2), so
  NASA standing in doesn't end APOD's turn. Today `sourceOf(file)` counts the user's own as
  APOD and NASA's file as NASA; the new function returns the turn's owner and the post (or
  "own") separately.
- Tests: grouping (equal columns, different ones, one empty, both from "Where", equal columns
  with a hold on one screen → two sets); two turning screens whose turns end together → both
  change in the same run and the clock restarts once; a hand-set unticked source held, then
  replaced when the turn ends, and on a single-source screen when that source posts; a newer
  post of the held source during the hold; unticking clears the hold; the stand-in
  with APOD alone and with turns for each choice; "skip" on a screen with APOD alone (null:
  keep) and with turns (next source); a stand-in that's also ticked; an early change on one
  screen (untick, no picture yet, "Skip it") doesn't restart the clock, and the source that
  comes in ends with the turn; a second tick restarts the clock only when no column took turns
  before; a held APOD whose next post is a video (stand-in shown, mark kept; "Skip it": held
  picture kept).
- Done when: the tests pass and the functions' docs say the rules above.

### [ ] M2. `Store`: mode, grid, a record per screen, migration, fetch rules (Opus, high)

Files: `Store.kt`, `DailyJob.kt` (the period: turns when a column has two or more),
`CLAUDE.md`.
- **Prefs:** `allSources` (the mode switch); `home`, `lock` (the grid's columns, comma lists);
  `where` stays APOD mode's; `videoDays` shared, `KEEP` now reads "Skip it" (the name may
  stay). A record per screen: home keeps today's unprefixed keys (`wallpaperSource` = whose
  turn, `wallpaperDate`, `wallpaperUrl`, `wallpaperSetAt`, plus new keys for the picture's
  source or "own" and the hand-set mark with the post it notes, M1), lock the same under
  `lock.`. A set on both screens writes both records. No copy when the screens split: each
  record already says what its screen shows (review: grouping by the due picture makes it
  unnecessary).
- **Migration from 0.5.0: a one-time write** at the first start of 0.6.0 (the first `Store`
  use, page or job; when `allSources` is missing), not a fallback on each read (second
  review, 2026-10-19: home's record *is* the unprefixed keys, so a read can't make it
  "unknown" with `where` = Lock; and columns falling back to `where` on every read would make
  a later "Where" change in APOD mode change the grid too). In one `prefs.edit`:
  - `allSources` = `sources` has two or more;
  - with it on, `home` and `lock` = `sources` in the columns `where` names (Both: both); with
    it off, nothing (the grid is written from "Where" the first time the switch goes on, M3);
  - the records by `where`: **Both**: copy the unprefixed record to `lock.*`. **Home**: leave
    home's, no lock record (unknown: the app never set the lock screen). **Lock**: copy the
    unprefixed record to `lock.*`, then clear home's (unknown).
  "Unknown" means the first run that needs that screen sets it. A missing picture-source key =
  `wallpaperSource`; no mark = not held (these two can stay read fallbacks). List the migration
  in `CLAUDE.md`'s Stack (drop two releases after F-Droid ships past 0.6.0); `CLAUDE.md`'s
  0.6.0 block has it until then.
- **`update`/`setDue`:** M1's function once, from one reading of the clock; then the sets it
  asks for (one with both flags when both screens are due the same picture, else
  `FLAG_SYSTEM` / `FLAG_LOCK` each) under the wallpaper lock, the records written after each,
  and the clock restarted once at the end if M1 says so. `setWallpaper` no longer restarts the
  clock itself. `forgetWallpaper` (switch on) forgets both records and the marks.
  `setWallpaper(file, screens)` for the button and ✓: a hand set, it sets the mark(s) and
  restarts the clock.
- **Fetch:** the sources in use for the job are the ticked ones of both columns, plus the
  stand-in on an APOD video day; the page with `allSources` on fetches all three.
- The Android behaviour this rests on was checked in M0; if M0 differed from the plan, follow
  M0's notes.
- Device (tablet, debug build, prefs by `run-as` since M3's UI isn't there yet; back up
  `shared_prefs/apodroid.xml` first and restore it): APOD on lock, NASA on home → two sets, each
  screen its picture; equal columns → one set, and with a hand set on Home only the lock
  screen keeps its picture through the next run; a faked APOD video day with "my picture" on a
  turning screen → the own picture in APOD's turn; the update from a 0.5.0 prefs file for each
  `where`, with one and with two sources → no set at the first run.
- Done when: the checks pass and `CLAUDE.md` (Background update, Turns; The app, Settings) says
  how it works now.

### [ ] M3. Settings: the mode switch, the grid, the dropdown, the set menu (Opus, high)

Files: `MainActivity.kt` (`Settings`, `tick`, `chooseVideoDays`, `setWallpaperNow`, `applyNow`),
`CLAUDE.md`.
- The layout above. The mode switch at the end of the settings, in both modes. APOD mode: as
  now, the dropdown in place of the four radios. Switch on: the grid replaces the chips and
  "Where".
- **The grid:** rows with `Source.label`, two columns of `Checkbox` (material3 draws its own
  check; no icon library) under the heads "Home" and "Lock". Screen-reader label per box, e.g.
  "APOD on the home screen". The last tick in the grid is greyed out. Ticking NASA or Earth when
  it wasn't ticked anywhere shows the weekday dialog, as now. Check at font scale 1.5 and 2.0 on
  the FP6's width (`wm density 344` on the tablet): the names wrap, the boxes stay in their
  columns.
- **The dropdown:** `ExposedDropdownMenuBox` (material3) or a `DropdownMenu` under a
  button-like field, whichever needs less code; the four choices; greyed while APOD has no tick
  (switch on). "Use my picture" with nothing picked opens the picker at once (if the user backs
  out of the picker, the choice falls back to the one before).
- **The set button:** switch off as now; switch on a `DropdownMenu` with Home screen / Lock
  screen / Both, then the set (M2's `setWallpaper(file, screens)`, a hand set: the hold). On
  APOD's video tab it sets the stand-in; with "Skip it" nothing, and the toast "Video day:
  wallpaper kept" (second review). The mode switch writes the grid from "Where" the first time
  it goes on (no `home`/`lock` yet) and clears every hold.
- **Changes while the daily switch is on:** a tick that changes what a screen shows sets it at
  once (as unticking the source on the wallpaper does now); one that only adds to a turning
  screen waits for its turn. An untick on a screen clears its hold. "Where" in APOD mode as
  now. The mode switch works like a tick (Decisions) and reschedules the job.
- Device (tablet, debug build): every control in both modes, switching modes back and forth
  (each keeps its settings), the grid's first start from "Where", the picker path, the menu.
- Done when: the user has seen the settings on the tablet and approved the texts, and
  `CLAUDE.md` (The app, Settings) describes them.

### [ ] M4. The page: three tabs, chip, status lines, video notice, problem box (Opus, medium)

Files: `MainActivity.kt` (`sources`, `shown`, `Tabs`, `Picture`, `statusText`, `VideoNotice`,
`problem` choice), `CLAUDE.md`.
- Switch on: always three tabs, opening on the source of the picture on the home screen (else
  the lock screen's, else APOD) until the user picks one. Switch off: as now.
- The chip: "On home screen" / "On lock screen" / "On wallpaper", from the records. Both the
  chip and the opening tab go by the **picture's** source, not by whose turn it is (second
  review): with NASA standing in for APOD, the chip is on NASA's tab; the user's own picture
  has no tab, so no chip.
- The status line: one line per screen while they differ.
- The video notice also with turns; its texts follow the stand-in rule (placeholder: "This
  APOD is a video, so your own picture takes its place.").
- The problem box: today the first source in use with a problem. With all three fetched,
  propose to the user: the shown tab's problem, else the first ticked source's.
- ✓ in crop mode: the same menu as the set button (switch on).
- `Store.watch` (review): add the `lock.*` record keys (today it reacts to `wallpaperSetAt`
  only), so the open page follows the job's sets on the lock screen too.
- Done when: the user has seen it on the tablet and approved the texts, and `CLAUDE.md` (The
  app, Tabs, Problems) is updated.

### [ ] M5. Tablet pass of M1–M4 (Opus, high)

Debug and R8 builds on the tablet: every check of M2–M4 again on the final tree, turns on two
screens over a few hours (`turn.sh`), the hold ending, a real or faked video day with each
choice in both modes, the update from 0.5.0 for each `where` with one and with two sources: a
debug build of 0.5.0 (`git worktree` at `v0.5.0`) under a debug build of 0.6.0, so `run-as`
can read the prefs before and after (a release build can't be read); then once an R8 build of
0.5.0 under an R8 build of 0.6.0 (both signed with the debug key, as the skill says),
checking only on screen, rotation and two panes, font scale 2.0. Report what was and wasn't
tested; the skill's "Still untested" follows.

### [ ] F1–F5. The features from 0.1.1's review

The how is in `features.md` (items 2, 4, 6, 7, 8). Device checks on the tablet (user,
2026-10-19): where `features.md` says "on the phone" (items 2 and 7), read the tablet; the FP6
only in P. Their open questions, to ask the user at the start of each:
- **F1 Fill / Fit:** does changing it set the wallpaper again while the switch is on (as
  "Where" does)? One setting for both screens (`features.md` says so) or per screen?
- **F2 Wi-Fi only:** none (the page and buttons still use any network).
- **F3 Share:** the glyph, shown on the phone; with the switch on it shares the shown tab.
- **F4 Image credit:** the user wants it under the title with the switch on; parse it for NASA
  and Earth too (not checked yet: look at `private/`'s feed and posts first), or APOD only?
- **F5 Save every picture:** every source in use, or APOD only? With the switch on and all
  three fetched by the page: only what the screens use. The ⓘ text's figures in `features.md`
  (2–10 MB a picture, 100–300 MB a month) are APOD's; NASA's originals are 4.7 MB typically
  and up to 97 MB, and saving every source means more than one large download within the
  job's roughly 10 minutes. Settle the figures and the job's time with this question.

### [ ] R. Docs, version, changelog, store texts, screenshots (Opus, medium)

`versionCode` 10, `versionName` 0.6.0; `changelogs/10.txt` (max 500 characters; say that a
video-day choice saved while it was greyed out in 0.5.0, e.g. "Use my picture" with turns, now
works with turns, second review); `README.md`,
`full_description.txt` (APOD first, the switch); `features.md` and this file ticked; the
screenshots on the FP6 by the user (APOD mode first, as `README.md` embeds `1.png`).

### [ ] P. Phone pass on the FP6 (Opus, high)

As 0.5.0's WP8: the release-signed build over the user's Obtainium install (the skill, Testing
an update on the user's phone), the update keeping the settings, both modes, the grid at font
scale 1.5 and 2.0, a lock screen with its own picture, landscape. Report what was tested.

### [ ] T. Commit and tag (Sonnet, low)

Only when the user says so; the user pushes.
