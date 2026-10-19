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
    mode). It clears every hold (the new mode's settings decide what shows).
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
| V  | Tablet pass of everything (debug and R8), the update from 0.5.0 | Opus, high |
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
claude --model opus --effort medium "Do F1 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model sonnet --effort medium "Do F2 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model sonnet --effort medium "Do F3 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort medium "Do F4 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort medium "Do F5 in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort high "Do V in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort medium "Do R in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort high "Do P in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model sonnet --effort low "Do T in .claude/skills/build-apodroid-android/plan-0.6.0.md"
```

Devices: the skill, Working on the phone. The tablet is free for testing; the FP6 is the
user's own phone, touched only in P. The build check:
`ANDROID_HOME=~/Android/Sdk ./gradlew :core:test :app:lintRelease :app:assembleRelease`.

## Remaining

### [ ] M0. Android check: home and lock set separately (Opus, medium)

A device package: rosie is its worker (night.md, the queue's Agent column). First, because
M1's grouping rests on it (second review, 2026-10-19). No new code: 0.5.0's "Where" already
sets with `FLAG_SYSTEM`, `FLAG_LOCK` or both. On the tablet only (user, 2026-10-19: not on the
FP6, which runs Android 16; P sees it there).
- **Build:** the debug build of the branch (`ANDROID_HOME=~/Android/Sdk ./gradlew
  :app:assembleDebug`; the code is 0.5.0's), installed as night.md says.
- **Before:** note `dumpsys wallpaper` and a screenshot of the home screen and of the lock
  screen (press Power twice: off, then on without unlocking), and the app's prefs and files as
  night.md says. At the end put the app's prefs and files back. The tablet's own wallpapers:
  if they were the app's pictures (`dumpsys wallpaper` names the app's package as the last
  setter, or the ids match a set from it), set them back with the app; otherwise say in the
  report that they changed (the tablet is a test device).
- **Setup:** in the app, the daily switch off (so no job sets anything), tick APOD, NASA and
  Earth (three tabs, three different pictures; `ui.sh` gives the coordinates). Each step below:
  choose "Where", show the tab, tap "Set as wallpaper now".
- **Android check first** (the four steps, a `dumpsys wallpaper` and both screenshots after
  each, named `m0-<step>-home.png` / `m0-<step>-lock.png`):
  1. "Both", APOD's tab: one wallpaper on both screens, no separate lock wallpaper in
     `dumpsys wallpaper`.
  2. "Home screen", NASA's tab: home shows NASA's picture, the lock screen still APOD's
     (Android copies the shared one to the lock screen first).
  3. "Lock screen", Earth's tab: the lock screen shows Earth's, home still NASA's.
  4. "Both", APOD's tab: both screens show APOD's, and `dumpsys wallpaper` shows one shared
     wallpaper again (no separate lock one left).
- **Verdict:** PASS when all four hold. STOP when a screen goes blank or shows something it
  shouldn't (the grouping in M1 has to follow what Android does; the user decides). NOT RUN
  without the tablet.
- Done when: the four results, with the `dumpsys wallpaper` lines that show them, are in
  `CLAUDE.md`'s Wallpaper paragraph (one or two sentences: what Android does with each flag,
  dated, "tablet, Android 15; the FP6 unchecked"), committed with any verdict, and the report
  commit names the screenshots.

### [ ] M1. `:core`: screens, columns, the stand-in and the hold (Opus, high)

Pure logic first, so M2 only wires prefs and files to it. Files: a new
`core/.../Screens.kt` (or `Turn.kt`, whichever reads better), `Turn.kt`, `TurnTest.kt`, a new
test file. One function takes the columns, the clock, both screens' records, the stored posts
(and which pictures exist), the video-day choice and `now`, and returns per screen what is due
(the turn's owner, the picture: a source's post or "own", the turn value to note) and whether
the clock restarts.
- `Screen` { HOME, LOCK }. The columns: `home: List<Source>`, `lock: List<Source>`; in APOD
  mode they come from "Where" (`listOf(APOD)` in the columns it names).
- **One reading of the clock** (reviews, 2026-10-19): every screen's due picture is worked out
  from the same `turnStartedAt` and `now` before anything is set. Today `setWallpaper`
  restarts the clock whenever the source changes (`Store.kt:473`); worked out one screen after
  the other, Home's change would restart it before Lock is checked.
- **The clock and the turn stamp** (Decisions): when the turn is over (`nextTurn`'s rule:
  `turnSlack`, a `startedAt` in the future counts as over) the clock restarts, first. Each
  screen's record notes the turn value it belongs to; a screen with turns whose stamp isn't
  the current value is due its next source (from its turn's owner, in order, with a picture),
  and gets the current value once it's set, or at once when no other source has a picture.
  An early change (an untick, a source with no picture, "Skip it") changes the owner but not
  the stamp: it ends with the turn. The clock never restarts for that. A second tick restarts
  it only when no column took turns before.
- **Grouping by the due picture, not by the columns:** both screens due the same picture and
  both needing it → one set with both flags; otherwise each screen that needs a set on its
  own. Equal columns alone don't make one wallpaper: a hand-set picture on Home only must not
  be overwritten on Lock. An empty column is left alone. (Fill / Fit is one setting, F1, so
  the same picture is the same set on both screens.)
- **The hold:** a set by hand changes the screen's **picture**, not its turn's owner (user,
  third review): the record notes the hand-set picture and a mark. While the mark is set the
  picture stays even when its source isn't ticked there. It ends at the screen's next normal
  change: on a screen with turns when its turn ends (the screen then goes on from its owner,
  so equal columns meet again); on a screen with one ticked source when that source's stored
  post is newer than the one the mark notes. A newer post of the held picture's own source is
  due at once and keeps the mark. Any change in that screen's column (a tick or an untick)
  clears the mark, and so does a mode switch (M2/M3). A held APOD whose next post is a video:
  the stand-in fills it ("Skip it" keeps the held picture), the mark stays. **A hand set from
  APOD's video tab** puts the stand-in on and is held as APOD's picture: APOD's next post (a
  picture) ends it, the stand-in source's next post doesn't. **No mark** on a screen with no
  ticks (it's left alone, so its picture stays anyway) and for a hand set of a one-source
  screen's own ticked source (that's its normal picture); say both in the doc, so nobody adds
  one. A hand set touching a screen with turns restarts the clock and writes the new value
  into the screens with turns whose stamp was current (not one still due a retry); one on
  screens without turns leaves the clock alone.
- **Forgetting and starting** (Decisions): `forget` drops a screen's picture and mark but
  keeps its owner and stamp, so the next run sets the owner's current picture, not the next
  source's. A screen with no record starts on the other screen's source if that source is in
  its column, else on the first ticked source with a picture, with the current stamp.
- **The stand-in:** on an APOD video day, APOD's turn is filled by the video-day choice:
  skip (a screen with APOD alone keeps its picture; with turns the next source with a
  picture, an early change), NASA's or Earth's stored picture, or the user's own. Whose turn it
  is (APOD) and which picture shows (the stand-in's) are two things: the record keeps both, so
  NASA standing in doesn't end APOD's turn. Today `sourceOf(file)` counts the user's own as
  APOD and NASA's file as NASA; the new function returns owner and picture separately.
- Tests: grouping (equal columns, different ones, one empty, both from "Where", equal columns
  with a hold on one screen → two sets); two screens with turns whose turn ends → both change
  in the same run, the clock restarts once; **a failed set**: Home's record has the new stamp,
  Lock's the old → the retry sets Lock only and Home doesn't move again; a turn ends and no
  other source has a picture → the stamp moves, nothing is set; an early change keeps the
  stamp and ends with the turn; a second tick restarts the clock only when no column took
  turns before; **the hold**: Earth set by hand on Home where Home and Lock turn through all
  three and show APOD → at the turn's end Home goes on from APOD, like Lock (one wallpaper
  again); an unticked source held on a one-source screen until that source posts; a newer post
  of the held source; a tick and an untick in its column each clear it; a hand set on a
  screen without turns leaves the clock and the other screen alone; a hand set while the other
  screen is due a retry → that screen keeps its old stamp and is set on the next run; a hand
  set from APOD's video tab (NASA standing in) → NASA's next post leaves it, APOD's next
  picture replaces it; a hand set of a one-source screen's own source → no mark; a forgotten
  record with turns → the same owner's picture, not the next source's; a screen with no record
  → the other screen's source when ticked there, else the first with a picture; a held APOD whose next post is a video (stand-in, "Skip
  it"); **the stand-in** with APOD alone and with turns for each choice, "skip" with APOD alone
  (keep) and with turns (next source), a stand-in also ticked.
- Done when: the tests pass and the functions' docs say the rules above.

### [ ] M2. `Store`: mode, grid, a record per screen, migration, fetch rules (Opus, high)

Files: `Store.kt`, `DailyJob.kt` (the period: turns when a column has two or more),
`MainActivity.kt` (only the set paths: `applyNow` and its callers, `set`, `setWallpaperNow`,
which the button and ✓ call), `CLAUDE.md`.
- **Prefs:** `allSources` (the mode switch); `home`, `lock` (the grid's columns, comma lists);
  `where` stays APOD mode's; `videoDays` shared, `KEEP` now reads "Skip it" (the name may
  stay). A record per screen: home keeps today's unprefixed keys (`wallpaperSource` = whose
  turn, `wallpaperDate`, `wallpaperUrl`, `wallpaperSetAt`), plus new keys for the picture's
  source or "own", the turn stamp and the hand-set mark with the post it notes (M1); lock the
  same under `lock.`. A set on both screens writes both records. No copy when the screens
  split: each record already says what its screen shows.
- **Migration from 0.5.0: a one-time write** at the first start of 0.6.0 (the first `Store`
  use, page or job; when `allSources` is missing), not a fallback on each read (second
  review: home's record *is* the unprefixed keys, so a read can't make it "unknown" with
  `where` = Lock; and columns falling back to `where` on every read would make a later "Where"
  change in APOD mode change the grid too). **Under a lock** (third review: the page and the
  job can both start first, and the Lock case run twice would copy home's already-cleared
  record over lock's): a lock object shared by every `Store`, the `allSources` check inside
  it, everything in one `prefs.edit(commit = true)`:
  - `allSources` = `sources` is anything but APOD alone (Decisions);
  - with it on, `home` and `lock` = `sources` in the columns `where` names (Both: both); with
    it off, nothing (the grid is written from "Where" the first time the switch goes on, M3);
  - the records by `where`: **Both**: copy the unprefixed record to `lock.*`. **Home**: leave
    home's, no lock record (unknown: the app never set the lock screen). **Lock**: copy the
    unprefixed record to `lock.*`, then clear home's (unknown);
  - the picture key: "own" when `wallpaperSource` is APOD, `wallpaperUrl` is empty and the
    stored APOD post of `wallpaperDate` is a video (0.5.0 noted the user's own picture as
    APOD's; third review), else `wallpaperSource`; the turn stamp = `turnStartedAt`; no mark;
  - the owner (whose turn): **APOD in APOD mode**, whatever `wallpaperSource` says (fourth
    review: with APOD alone and NASA standing in on a video day, 0.5.0 stored
    `wallpaperSource` = NASA, which would make NASA the owner of a screen whose column is just
    APOD); with the switch on, `wallpaperSource` (0.5.0 had no stand-in with turns);
  - **a fresh install** runs it too (`allSources` is missing there as well): no records, no
    `sources`, so it writes `allSources` = false and nothing else; it must not fail on the
    missing keys, and writing `allSources` keeps it from running again.
  "Unknown" means the first run that needs that screen sets it. List the migration in
  `CLAUDE.md`'s Stack (drop two releases after F-Droid ships past 0.6.0); `CLAUDE.md`'s 0.6.0
  block has it until then.
- **`update`/`setDue`:** M1's function once, from one reading of the clock; the clock written
  first if it restarts; then the sets it asks for (one with both flags when both screens are
  due the same picture, else `FLAG_SYSTEM` / `FLAG_LOCK` each) under the wallpaper lock, each
  screen's record (with its stamp) written right after its own set. A failed set leaves that
  record as it was, so the retry sets that screen only. `setWallpaper` no longer restarts the
  clock itself. `forgetWallpaper` (switch on) restarts the clock, then forgets both records
  as M1 says (picture and mark dropped, owner and stamp kept, the screens on the current turn
  take the new stamp), so the current sources' pictures go on again.
- **Two ways to set:** `setByHand(file, screens)` for the button and ✓ (the mark, the clock
  restart when a screen has turns, M1). Wire `setWallpaperNow` and `set` to it here (fourth
  review: once `setWallpaper` stops restarting the clock, the button would otherwise lose its
  restart and its hold until M3); in APOD mode the screens are those "Where" names, with the
  switch on Home and Lock both until M3 adds the menu (a night default to list). A changed
  setting is **not** a set by hand (third review: `applyNow`, `MainActivity.kt:499`, today
  goes through `setWallpaper` with `startTurn`, so every "Where" change, video-day choice or
  "Change picture" would hold the picture and restart the clock): it forgets the affected
  screens' records (M1: owner and stamp kept), then runs `setDue`. Rewire `applyNow` and its
  callers that way here.
- **Fetch:** the job's sources are the ticked ones of both columns, the source of each picture
  held by hand (so its newer post is set; third review), and the stand-in on an APOD video day
  only while APOD is ticked on some screen (or in APOD mode); the page with `allSources` on
  fetches all three.
- The Android behaviour this rests on was checked in M0; if M0 differed from the plan, follow
  M0's notes.
- Device (tablet, debug build, prefs by `run-as` since M3's UI isn't there yet; back up
  `shared_prefs/apodroid.xml` first and restore it): APOD on lock, NASA on home → two sets, each
  screen its picture; equal columns → one set, and with a hand set on Home only the lock
  screen keeps its picture through the next run; a faked APOD video day with "my picture" on a
  turning screen → the own picture in APOD's turn; the update from a 0.5.0 prefs file for each
  `where`, with APOD alone, NASA alone and two sources → no set at the first run (also on a
  faked video day with "my picture", and with APOD alone and NASA standing in: the owner is
  APOD, nothing set), and a second start doesn't migrate again; a fresh install (`pm clear`)
  → no crash, `allSources` false; "Set as wallpaper now" with turns → `turnStartedAt` fresh,
  the hold mark set; a "Where" change with the switch on → the picture set again,
  `turnStartedAt` unchanged.
- Done when: the checks pass and `CLAUDE.md` (Background update, Turns; The app, Settings) says
  how it works now.

### [ ] M3. Settings: the mode switch, the grid, the dropdown, the set menu (Opus, high)

Files: `MainActivity.kt` (`Settings`, `tick`, `chooseVideoDays`, `setWallpaperNow`), `CLAUDE.md`.
- The layout above. The mode switch at the end of the settings, in both modes. APOD mode: as
  now, the dropdown in place of the four radios, the set button at the end of the Wallpaper
  section. Switch on: the grid replaces the chips and "Where"; the set button under "Change
  every".
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
  screen / Both, then M2's `setByHand`. On APOD's video tab it sets the stand-in; with "Skip
  it" nothing, and the toast "Video day: wallpaper kept".
- **The mode switch** writes the grid from "Where" the first time it goes on (no `home`/`lock`
  yet), clears every hold and works like a tick: M2's forget-and-`setDue` path, the job
  rescheduled.
- **Changes while the daily switch is on** (M2's forget-and-`setDue` path, never `setByHand`): a
  tick that changes what a screen shows sets it at once (as unticking the source on the
  wallpaper does now); one that only adds to a turning screen waits for its turn. Any tick or untick in
  a screen's column clears that screen's hold (Decisions). "Where", the video-day choice and "Change picture" as now.
- Device (tablet, debug build): every control in both modes, switching modes back and forth
  (each keeps its settings), the grid's first start from "Where", the picker path, the menu; a
  "Where" change and "Change picture" with turns don't restart the clock (`turnStartedAt`
  unchanged); a picture held on Home, then a tick in Home's column → the hold gone, Home
  set from its settings; a hand set from APOD's video tab (faked video day, NASA as stand-in)
  → NASA's picture on the chosen screens.
- Done when: the user has seen the settings on the tablet and approved the texts, and
  `CLAUDE.md` (The app, Settings) describes them.

### [ ] M4. The page: three tabs, chip, status lines, video notice, problem box (Opus, medium)

Files: `MainActivity.kt` (`sources`, `shown`, `Tabs`, `Picture`, `readStatus`, `statusText`,
`VideoNotice`, `problem` choice), `Store.kt` (`watch`), `CLAUDE.md`.
- Switch on: always three tabs, opening on the source of the picture on the home screen (else
  the lock screen's, else APOD) until the user picks one.
- **Switch off reads the right record** (third review): the page reads home's keys today
  (`MainActivity.kt:317-320`); after the migration with "Where" = Lock home's record is empty,
  so the status line and the chip would vanish. APOD mode reads the record of a screen "Where"
  names (Lock → `lock.*`).
- The chip: "On home screen" / "On lock screen" / "On wallpaper", from the records. Both the
  chip and the opening tab go by the **picture's** source, not by whose turn it is: with NASA
  standing in for APOD, the chip is on NASA's tab; the user's own picture has no tab, so no
  chip.
- The status line: one line per screen while they differ.
- The video notice also with turns; its texts follow the stand-in rule (placeholder: "This
  APOD is a video, so your own picture takes its place."); with the switch on and APOD ticked
  nowhere it only says the APOD is a video (third review).
- The problem box: today the first source in use with a problem. With all three fetched,
  propose to the user: the shown tab's problem, else the first ticked source's.
- ✓ in crop mode: the same menu as the set button (switch on).
- `Store.watch`: add the `lock.*` record keys (today it reacts to `wallpaperSetAt` only), so the
  open page follows the job's sets on the lock screen too.
- Device (tablet, debug build): the switch on with each tab opening as above; the chip on
  each screen's picture (Home and Lock showing different sources, then the same); a faked
  APOD video day with NASA standing in (the chip on NASA's tab) and with "my picture" (no
  chip); the status lines with the screens apart and together; APOD mode with "Where" = Lock
  (status line and chip still there); the open page following a `run -f` that changes only the
  lock screen; one screenshot per screen state for the texts.
- Done when: the user has seen it on the tablet and approved the texts (the morning), and
  `CLAUDE.md` (The app, Tabs, Problems) is updated.

### [ ] F1–F5. The features from 0.1.1's review

The how is in `features.md` (items 2, 4, 6, 7, 8). Device checks on the tablet (user,
2026-10-19): where `features.md` says "on the phone" (items 2 and 7), read the tablet; the FP6
only in P. Their open questions, to ask the user at the start of each:
- **F1 Fill / Fit:** one setting for both screens (decided, 2026-10-19). Still open: does
  changing it set the wallpaper again while the switch is on? If so, M2's forget-and-`setDue`
  path, never a set by hand.
- **F2 Wi-Fi only:** no open question (the page and the buttons still use any network). The
  switch's place: under the daily switch, shown while it's on (the layout above). Files:
  `Store.kt` (a pref `wifiOnly`, default off), `DailyJob.kt` (`NETWORK_TYPE_UNMETERED` when on;
  `ensure` treats a changed network type as another timing, so the job is rescheduled),
  `MainActivity.kt` (the switch, rescheduling while the daily switch is on), `CLAUDE.md`.
  - Device (tablet, debug build): with the daily switch on, Wi-Fi only on → `dumpsys
    jobscheduler` shows the unmetered constraint for job 1; off → any network again. With
    Wi-Fi off (note the setting first and put it back) the job doesn't run.
  - Done when: the switch is built, the device checks pass, and `CLAUDE.md` (Background
    update) has the setting and the job's constraint.
- **F3 Share:** the glyph, shown on the tablet; with the switch on it shares the shown tab.
- **F4 Image credit:** the user wants it under the title with the switch on; parse it for NASA
  and Earth too (not checked yet: look at `private/`'s feed and posts first), or APOD only?
- **F5 Save every picture:** every source in use, or APOD only? With the switch on and all
  three fetched by the page: only what the screens use. The ⓘ text's figures in `features.md`
  (2–10 MB a picture, 100–300 MB a month) are APOD's; NASA's originals are 4.7 MB typically
  and up to 97 MB, and saving every source means more than one large download within the
  job's roughly 10 minutes. Settle the figures and the job's time with this question.

### [ ] V. Tablet pass of everything (Opus, high)

After F5, so it tests the final code (third review: F1 changes the set path, F5 the job).
Debug and R8 builds on the tablet: every check of M2–M4 and F1–F5 again on the final tree,
turns on two screens over a few hours (`turn.sh`), a failed set on one screen (proxy) and its
retry, the hold ending both ways, a real or faked video day with each choice in both modes, the
update from 0.5.0 for each `where` with APOD alone, NASA alone and two sources: a debug build of
0.5.0 (`git worktree` at `v0.5.0`) under a debug build of 0.6.0, so `run-as` can read the prefs
before and after (a release build can't be read); then once an R8 build of 0.5.0 under an R8
build of 0.6.0 (both signed with the debug key, as the skill says), checking only on screen;
rotation and two panes, font scale 2.0. Report what was and wasn't tested; the skill's "Still
untested" follows.

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
