# Plan for 0.6.0 (APOD first; all NASA pictures; the features from 0.1.1's review)

Planned 2026-10-19; done, released 2026-11-02 as 0.6.0 (versionCode 10) after its release gate:
the tablet passes V1–V12f, then V7 and P on the final tree, then W (`CLAUDE.md`, Next features,
0.6.0's release gate). Each package wrote its decisions into `CLAUDE.md`, the source of truth;
here each finished package is one line, and the full text (specs, checks, results, the run
commands) is in git: `git log -- .claude/skills/build-apodroid-android/plan-0.6.0.md`. The
headings stay (`night.md`'s queue rows and `scripts/pipeline.test.sh` resolve them). Still open:
E, postponed to 0.7.0.

## The request

From the user looking at 0.5.0 (2026-10-19): different pictures on home and lock; "Use my
picture" on video days with turns too; APOD first, the other sources a mode to turn on, with all
three to look at. Plus `features.md` items 2, 4, 6, 7 and 8 (Fill / Fit, Wi-Fi only, Share, the
credit, Save every picture). What came of each: `CLAUDE.md`, Next features (0.6.0: APOD first;
From a review of 0.1.1).

## Decisions (user, 2026-10-19)

Rewritten after the release (2026-11-02): each decision where `CLAUDE.md` now holds it, or what
replaced it. Rejected on the way: a "Lock screen: one source" row, a first-boot screen
(`CLAUDE.md`, Next features, 0.6.0: APOD first).
- **Two modes**, a switch, default off ("All NASA pictures"; "More NASA pictures" since S5, at the
  top of Setup since J3); each mode keeps its settings ("Where" APOD mode's, the list the other's);
  off leaves a screen "Where" doesn't name alone (as 0.5.0 when "Where" went from Both to Home):
  `CLAUDE.md`, The app, Settings: More NASA pictures.
- **From 0.5.0:** APOD ticked alone → APOD mode, anything else → on, the sources in the columns
  "Where" named (0.5.0 let NASA or Earth be ticked alone; such a user keeps their source):
  `CLAUDE.md`, Stack, Migrations.
- **The list** (the grid until S2), the columns `home` and `lock`, an empty column left alone, the
  last tick kept: `CLAUDE.md`, Settings: The list. Its "one shared clock", restarted by Set:
  replaced by the change times by the clock (J1, J2; Turns and the picture time).
- **A record per screen** and a screen with no record: `CLAUDE.md`, Background update, Turns and
  the picture time; equal columns change together since B2.
- **Video days: the stand-in takes APOD's place on every screen where APOD is ticked**, "Skip it"
  passes the turn, one choice for both modes, a dropdown: `CLAUDE.md`, Settings: If APOD is a
  video; Turns. "A stand-in may show twice in a row": replaced by not twice (B15, B22).
- **The page with the switch on** (the chip per screen, a status line per screen, the video
  notice with turns): `CLAUDE.md`, The title and the swipe; Problems and status lines; Video days
  on the page. The tabs: replaced by the title dropdown and the swipe (S6).
- **Fill / Fit, one setting for both screens** (F1): dropped (user, 2026-10-22; A3, A4), the
  whole picture per post instead (`CLAUDE.md`, Next features, 0.6.0: the picture).
- **Data:** the page fetches the sources it offers, the job what the screens need (the ticked
  ones and the stand-in on an APOD video day): `CLAUDE.md`, The page; Background update.

## Set applies the setup (user, 2026-10-20 evening; S4–S6)

From the user's mockup of four phone screens (https://claude.ai/artifact/BqfZJYLPEChxiY6MGKf6Nc,
approved the same day), replacing S1's "a set by hand changes the settings" and S3. Why: a change
took effect at once, "but maybe I'm not done"; one system for both modes, though in APOD mode a
change of "Where" alone would be safe to set at once. Where each part lives now (`CLAUDE.md`, The
app, Settings, unless named):
- **A draft until Set**, kept over a rotation and the system killing the app; "my picture" a
  draft file until Set: Set applies the setup. The daily switch acts at once, both ways (S4f2).
- **⛶'s part in the draft**, ✓ setting nothing: replaced by U0 (2026-11-02: ✓ sets it at once;
  Choosing the part).
- **Two kinds of state on the page**; **the job follows the stored setup only.**
- **One "Set as wallpaper"**, both modes, no menu, the label kept; its toasts.
- **At once:** "Save every picture", "Largest original" and ★ ("Wi-Fi only" removed, B10).
- **The highlight** in `tertiary` until Set (not ⛶ since U0).
- **"Change every"** [24 h | 12 h | 6 h | 3 h]; the name **"More NASA pictures"**.
- **The title dropdown and the swipe** instead of the tabs (S6): The title and the swipe. **No
  Share** (S6).
- **The layout, Setup's opening and its summary:** Setup and Advanced as rows (S5, U0), the mode
  switch and "New picture at" in Setup (J3), no Size row (A4), the summary grouped by screen (U0).

## Packages

The index, in the order the packages were planned (model and effort as run). The run commands and
the devices' rules are in git; `night.md` has the night shift's.

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
| B1 | The status line after Set with "Skip it" on a video day (regression 2) | Sonnet, medium |
| S4f2 | The daily switch applies at once, both ways | Opus, medium |
| S4f3 | Set doesn't stop a running job (pass 5's side effect) | Opus, medium |
| S5 | Settings: Setup and Advanced settings, the highlight, "24 h", "More NASA pictures" | Opus, medium |
| F6 | A size limit for the job's saves ("Largest original", default 20 MB) | Opus, medium |
| B2 | Equal columns change together (regression 3) | Opus, medium |
| B3 | Two texts: " · My picture" in APOD mode, the ⓘ for saving (by day) | Sonnet, medium |
| B4 | The draft after a second start (S5's tablet check) | Opus, medium |
| B5 | A failed run keeps its retry (pass 6) | Opus, medium |
| B6 | Quick taps on the daily switch set once (pass 6) | Opus, medium |
| S6 | The picture side: the title dropdown, swipe between sources, no Share | Opus, high |
| B7 | The job keeps its retry: a stop during the saves, a Set after a failed run (pass 8, regression 4) | Opus, medium |
| B8 | The draft's picture survives a second page (B4's edge, pass 8) | Opus, medium |
| B9 | A no-break space before ▾ and ▸ (V3, font scale 2.0) | Sonnet, medium |
| B10 | Remove "Wi-Fi only" (user, 2026-10-21) | Sonnet, medium |
| B11 | A Set from a second page keeps the stored "my picture" (pass 11) | Opus, medium |
| B12 | Only the first retry is waited for (review of B7) | Opus, medium |
| V  | Tablet pass of everything (debug and R8), the update from 0.5.0: V1–V6 and V8 by night, V7 a night of its own (by hand) | Opus, high |
| V9 | Brute test 1: what the system does to the app (rosie) | Opus, high |
| V10 | Brute test 2: what a user does to the app (rosie) | Opus, high |
| CR | Code review of the whole app, by reading | Opus, high |
| B13 | The brute test's bugs | Opus, high |
| B14 | The code review's bugs | Opus, high |
| J0 | Android check: which timed run lands with the screen off, on battery (done) | Opus, medium |
| J1 | `:core`: the picture time and the three rules | Opus, high |
| J2 | `Store` and `DailyJob`: alarms at the change times, the job simplified | Opus, high |
| J3 | Settings: New picture at, "More NASA pictures" into Setup, the pause hint | Opus, medium |
| A1 | `:core`: the background, black or blurred | Opus, medium |
| A3 | `Store`: the whole picture per post, on black or blurred | Opus, medium |
| A4 | ⛶'s Whole picture toggle; Size goes | Opus, medium |
| B15 | The window by publish time, the 09:00 check, the stand-in shown twice, the video notice | Opus, high |
| B16 | The job: a passed check kept on re-arming, one run at a time, a stale pending run | Opus, high |
| B17 | The APOD-mode video notice follows `standInReady` | Opus, medium |
| CR2 | Code review of what changed since 910bfeb, read as a whole | Opus, high |
| B18 | CR2's bugs | Opus, high |
| V11a | Final pass 1: what the system does to the new job | Opus, high |
| V11d | Final pass 2: R8 and the update from 0.5.0 without opening the page | Opus, high |
| V11b | Final pass 3: the rules on a faked clock | Opus, high |
| V11c | Final pass 4: what a user does to the new UI | Opus, high |
| B19 | The job: a killed run keeps its retry; the switch's stop isn't logged as a failure | Opus, high |
| B20 | Set says when its setup couldn't be saved | Opus, medium |
| B21 | One source after a day offline: the newest goes on | Opus, high |
| V12 | The final pass again (V12a–d: V11a–d on the tree with B19–B21) | Opus, high |
| B22 | The stand-in not twice with APOD alone too; the video notice says so | Opus, high |
| B23 | No "kept" while something takes APOD's place | Opus, medium |
| B24 | A run's bookkeeping before `jobFinished` | Opus, medium |
| V12e | V12a's rest 1: Doze and battery saver | Opus, high |
| V12f | V12a's rest 2: reboot, the clock, what happens during a run | Opus, high |
| SIM | `:core`: the rules over random months (test code only) | Opus, high |
| B25 | The pause hint's unused line on Android 12 (E's setup); the weekday dialog's false sentence | Sonnet, medium |
| V7 | Turns over hours, by hand: rosie's default (Sonnet, medium), as its commands below | Sonnet, medium |
| P  | Phone pass on the FP6 (release-signed), after the version bump | Opus, high |
| W  | A week on the FP6 before the tag: real mornings, a weekend, the US DST end, the Zurich case | Opus, medium |
| E  | Android 10, 12, 13 and 14 in the emulator: E0 by day, E1a–E4 one night (`emulator.md`) | Opus, high (E0), medium |
| R  | Docs, changelog, store texts, screenshots | Opus, medium |
| T  | Commit and tag, when the user says so | Sonnet, low |

## Done

M0–M4 and F2: the night shifts of 2026-10-20 (`night.md`), approved by the user that morning;
their notes are the report commits.

### [x] M0. Android check: home and lock set separately (Opus, medium)

PASS on the tablet (`CLAUDE.md`, Wallpaper).

### [x] M1. `:core`: screens, columns, the stand-in and the hold (Opus, high)

`Screens.kt`: `dueWallpapers`, `planByHand`, the stand-in, the hold (two fix rounds); the hold went
in S1, `planByHand` in S4.

### [x] M2. `Store`: mode, grid, a record per screen, migration, fetch rules (Opus, high)

The mode, the columns, a record per screen, `Migration.kt` (`CLAUDE.md`, Stack, Migrations). M2f1:
the video-day choice forgets the records.

### [x] M3. Settings: the mode switch, the grid, the dropdown, the set menu (Opus, high)

Both modes' settings; the grid and the set menu replaced by S2 and S4 (`CLAUDE.md`, The app,
Settings).

### [x] M4. The page: three tabs, chip, status lines, video notice, problem box (Opus, medium)

The page per screen; the tabs replaced by S6. M4f1: "Skip it" passes APOD's turn at once.

### [x] F2. Wi-Fi only (Sonnet, medium)

The switch and the job's unmetered constraint; removed by B10 (2026-10-21).

### [x] The morning (2026-10-20, a day session)

The user's answers to the night's questions (in `CLAUDE.md`) and a fix package (the short names;
`chooseVideoDays`' comment).

## Remaining

How the work after the first night was ordered, each step the user's; the packages follow.

1. F3's commit, then F1f1 and F5f1 (found by a review of F1 and F5): done 2026-10-20.
2. C, the early look on the tablet, on the user's go-ahead: done 2026-10-20.
3. V1–V6 by night (rosie, the queue in `night.md`); F1–F5's device checks run there.
3a. S1, S2, then S4–S6 (user, 2026-10-20; S3 dropped that evening for "Set applies the setup").
3b. The evening of 2026-10-20: S4's texts and three night defaults approved; new B1, S4f2, S4f3
   and F6. Only APODroid's nights use the tablet.
3c. The night of 2026-10-20: B1, S4f2, S4f3, S5 and F6 merged; S5's and F6's texts approved;
   B2–B6 decided.
3d. The next night (`.coord/reports/2026-10-20-night.md`): B2, B4–B6, S6, V3, V5, V6 and V8
   merged with tablet PASS; S6's texts approved; to fix B7–B9, and B10 removes "Wi-Fi only".
3e. The night of 2026-10-21: B7–B10 merged (master 0b9c744); B11 and B12 by day.
4. V7, a night of its own on the final tree; a B package after it that touches the job or turns
   reruns its part.
4a. The order to a stable 0.6.0 (user, 2026-10-21 evening): V9, V10, CR, B13, B14, then V7.
4b. The night of 2026-10-21: CR and B13 merged (master 910bfeb); crop mode closing on a rotation
   left as is; B14's scope set.
4c. The picture time (user, 2026-10-22 evening): J0, then J1 → J2 → J3, then V7 (`CLAUDE.md`,
   Next features, 0.6.0: the picture time).
4d. The picture (user, 2026-10-22 evening): always Fill, the whole picture per post; A1 → A3 →
   A4 (`CLAUDE.md`, Next features, 0.6.0: the picture).
4e. The final plan (user, 2026-10-23 night): the three rules, one time, one clock; what it dropped:
   `CLAUDE.md`, Next features, 0.6.0: the picture time.
4f. The night of 2026-10-23: J1, A1, J2, A3, A4 and J3 merged (master 84404ed); B15 and B16 to fix.
4g. The release gate (user, 2026-10-24 evening): a code freeze; CR2 and B18, then V11a–d, then V7
   and P (`CLAUDE.md`, Next features, 0.6.0's release gate).
4h. The night of 2026-10-24/25: B17, CR2, B18 and V11a–d merged (V11a's STOP was Android); to fix
   B19–B21, then V12a–d.
4i. The night of 2026-10-25: B19–B21 merged, V12b–d PASS, V12a 3 items (master 654848c); B22–B24
   decided, V12a's rest as V12e and V12f.
4j. More testing before the tag (user, 2026-10-26 afternoon): W, a week on the FP6 to Mon 2 Nov;
   SIM; E in the emulator (postponed to 0.7.0, 4n).
4k. The evening of 2026-10-26: B22–B24 and SIM merged (master db78d0c, versionCode 10 from
   423f082); P's install over the real 0.5.0 started W; V7's session 1.
4l. Tue 27 Oct: V7, V12e and V12f PASS; W's first morning PASS.
4m. Wed 28 to Fri 30 Oct: W PASS each day; ZuiMemoryCleaner force-stopped the tablet's app twice
   (`on-time-0.7.0.md`); APOD's day-first title prefix (`CLAUDE.md`, Open questions 4).
4n. Mon 2 Nov: W done with no `BUG:`; E postponed to 0.7.0 (user); B25, P, U0, R and T that day.
5. P on the FP6 with the user, then R and T: done 2026-11-02.

### [x] F1. Fill / Fit (Opus, medium)

The Size row [Fill | Fit] (`fitFrame`, `Frame.kt`); Fit removed by A3 and A4, the whole picture
per post instead (`CLAUDE.md`, Next features, 0.6.0: the picture).

### [x] F1f1. Fit decodes at the size it's drawn (Opus, medium)

Decoded straight to `fitFrame`'s size (`setTargetSize`), drawn 1:1; the whole picture keeps it
(`CLAUDE.md`, Wallpaper: The whole picture).

### [x] F3. Share (Sonnet, medium)

⤴ beside ★, the title and link as text; removed by S6 (`CLAUDE.md`, Next features, From a review
of 0.1.1).

### [x] F4. Image credit (Opus, medium)

`Post.credit`, APOD only, one line under the date (`CLAUDE.md`, Data source: Credit; The app:
Credit).

### [x] F5. Save every picture (Opus, medium)

`sourcesInUse` and `toSave` (`Saving.kt`, `SavingTest`), the job's saves after each run
(`CLAUDE.md`, Save every picture).

### [x] F5f1. A stopped save: the smaller copy next time (Opus, medium)

One try of the original per post (`<source>.saveTried`), then the smaller copy (`CLAUDE.md`, Save
every picture).

### [x] C. Early look on the tablet (Opus, medium; on the user's go-ahead)

The user saw ⤴, the Size row, the credit and the ⓘ text on the tablet (2026-10-20); the answers
built (`.coord/early-look/`).

### [x] S1. `:core` and `Store`: a set by hand changes the settings, no holds (Opus, high)

The holds removed; a set by hand changing the settings, itself replaced by S4 ("Set applies the
setup").

### [x] S2. Settings: the list instead of the grid, the order, the daily switch off (Opus, medium)

The list instead of the grid, "If APOD is a video" under its own heading (`CLAUDE.md`, The app,
Settings: The list).

### [–] S3. (dropped) The page: the set button under the picture, its menu, the highlight (Opus, medium)

Dropped (user, 2026-10-20 evening): replaced by S4–S6 ("Set applies the setup").

### [x] S4. Set applies the setup: the draft, `Store.saveSetup`, the old set paths gone (Opus, high)

The draft until Set, `Store.saveSetup`, one "Set as wallpaper"; `setByHand` and the old set paths
gone (`CLAUDE.md`, The app, Settings: Set applies the setup).

### [x] B1. The status line after Set with "Skip it" on a video day (Sonnet, medium)

`videoDayKept` counts a forgotten record as kept when the screen was set before (`wallpaperSetAt`;
`CLAUDE.md`, Problems and status lines).

### [x] S4f2. The daily switch applies at once, both ways (Opus, medium)

Off cancels at once, on sets the stored setup afresh (`Store.startAfresh`; `CLAUDE.md`, The app,
Settings: The daily switch).

### [x] S4f3. Set doesn't stop a running job (Opus, medium)

No reschedule stops a running run (`JobRuns.going`; `CLAUDE.md`, Background update); its notes
went in J2.

### [x] S5. Settings: Setup and Advanced settings, the highlight, "24 h", "More NASA pictures" (Opus, medium)

Setup and Advanced settings, the highlight, "24 h", the name "More NASA pictures" (`CLAUDE.md`,
The app, Settings).

### [x] F6. A size limit for the job's saves (Opus, medium)

"Largest original", default 20 MB: `download`'s limit, `TooLarge` (`CLAUDE.md`, Save every
picture: A size limit).

### [x] B2. Equal columns change together (Opus, medium)

The lock screen takes home's record in every run, so one set (`dueWallpapers`; `CLAUDE.md`, Turns:
Equal columns).

### [x] B3. Two texts: " · My picture" in APOD mode, the ⓘ for saving (Sonnet, medium)

`pictureSuffix` in both modes, and the ⓘ text for F6's limit (`CLAUDE.md`, Problems and status
lines; Save every picture).

### [x] B4. The draft after a second start (Opus, medium)

`launchMode="singleTop"`: another start while the page is on top keeps the draft (`CLAUDE.md`,
Settings: One page).

### [x] B5. A failed run keeps its retry (Opus, medium)

A reschedule noted during a failed run no longer replaced its backoff retry; the notes went in J2,
the alarms retry since (`CLAUDE.md`, Retries by the alarms).

### [x] B6. Quick taps on the daily switch set once (Opus, medium)

On, off, on set once (`:core`'s `SwitchRuns`, `SwitchRunsTest`; `CLAUDE.md`, Background update).

### [x] S6. The picture side: the title dropdown, swipe between sources, no Share (Opus, high)

The title dropdown and the swipe (`HorizontalPager`) instead of the tabs; Share removed
(`CLAUDE.md`, The title and the swipe).

### [x] B7. The job keeps its retry: a stop during the saves, a Set after a failed run (Opus, medium)

A stop during the saves counted as failed; a Set after a failed run waited for one retry at most;
off left no retry due (`JobRuns`' generation, kept by J2).

### [x] B8. The draft's picture survives a second page (Opus, medium)

A page's start deletes `fallback-draft.jpg` only while no other page is alive (`MainActivity`'s
`pages`; `CLAUDE.md`, Settings: One page).

### [x] B9. A no-break space before ▾ and ▸ (Sonnet, medium)

The glyph stays with the last word (`CLAUDE.md`, Settings: If APOD is a video).

### [x] B10. Remove "Wi-Fi only" (Sonnet, medium)

The job always on any network (`CLAUDE.md`, Next features, From a review of 0.1.1; `features.md`
item 4).

### [x] B11. A Set from a second page keeps the stored "my picture" (Opus, medium)

`saveSetup` renames the draft's picture only when the Set's own draft holds the pick
(`draftFallback`; `CLAUDE.md`, Settings: One page).

### [x] B12. Only the first retry is waited for (Opus, medium)

A failed retry no longer kept every Set waiting for Android's growing backoff; the retry due went
with the notes in J2.

### [x] V. Tablet pass of everything (Opus, high)

The V rows' common rules for rosie (results, the time box: a `progress:` commit per item, none new
after 40 min); V3–V10 the complete passes of 21–22 Oct (`CLAUDE.md`, 0.6.0's release gate).

### [x] V1. F1, F3 and F1f1 on the tablet

Merged (`.coord/runs/011-v1/`); on the tree before S4–S6, so V8 repeated it.

### [x] V2. F4, F5 and F5f1 on the tablet

Merged (`.coord/runs/012-v2/`); on the tree before S4–S6, so V8 repeated it.

### [x] V3. M2–M4 again on the final tree

PASS (Remaining 3d); its font-scale find became B9.

### [x] V4. What the night of 2026-10-20 didn't check

Merged (`.coord/runs/013-v4/`); V8 repeated it on the final tree.

### [x] V5. The update from 0.5.0, debug builds

PASS (Remaining 3d); the migration: `CLAUDE.md`, Stack, Migrations.

### [x] V6. R8 builds

PASS (Remaining 3d).

### [x] V7. Turns over hours (a night of its own, after V12)

PASS 2026-10-27, the tablet's real night on battery: two sets, each on time, nothing twice
(`.coord/runs/v7/report.md`).

### [x] V8. V1, V2 and V4 again where S4–S6 changed them

PASS (Remaining 3d).

### [x] V9. Brute test 1: what the system does to the app

FIX: its `BUG:` lines fixed by B13, its results merged by hand (Remaining 4b).

### [x] V10. Brute test 2: what a user does to the app

FIX: its `BUG:` lines fixed by B13, its results merged by hand (Remaining 4b).

### [x] CR. Code review of the whole app (Opus, high)

`review-0.6.0.md` (deleted 2026-10-26, in git); its bugs fixed by B14.

### [x] B13. The brute test's bugs (Opus, high)

V9's and V10's bugs, among them Set's and the switch's runs keeping their end over a rotation
(`CLAUDE.md`, Set as wallpaper); tablet PASS.

### [x] B14. The code review's bugs (Opus, high)

CR's findings 1, 3, 4, 5 and 2's rest; a hidden draft value goes back to the stored one; the lock
line during a Set (`CLAUDE.md`, Settings); tablet PASS.

### [x] B15. The window by publish time, the 09:00 check, the stand-in shown twice, the video notice (Opus, high)

`Post.published` and the window by it, the check at its end, the stand-in not twice with turns
(`CLAUDE.md`, Turns and the picture time; Fetch).

### [x] B16. The job: a passed check kept on re-arming, one run at a time, a stale pending run (Opus, high)

A passed check armed at once (`lastRunAt`), one run at a time, a stale pending id 2 replaced, the
interrupt flag cleared before `ranJob` (`CLAUDE.md`, Background update).

### [x] J0. Android check: which timed run lands with the screen off, on battery (Opus, medium)

Only a wake-up alarm lands in Doze; the pair fired at T + 10 min three of three
(`.coord/runs/j0/report.md`; `CLAUDE.md`, Next features, 0.6.0: the picture time, Android first).

### [x] J1. `:core`: the picture time and the three rules (Opus, high)

`PictureTime.kt`: the change times and the three rules, by the clock (`PictureTimeTest`;
`CLAUDE.md`, Turns and the picture time).

### [x] J2. `Store` and `DailyJob`: alarms at the change times, the job simplified (Opus, high)

The pair of alarms starts job 2 (`AlarmReceiver.kt`), job 1 a 6 h fallback, retries by the alarms;
the notes removed (`CLAUDE.md`, Background update).

### [x] J3. Settings: New picture at, "More NASA pictures" into Setup, the pause hint (Opus, medium)

"New picture at" and the mode switch in Setup, the Unrestricted line's reason (`CLAUDE.md`,
Settings; The pause hint).

### [x] A1. `:core`: the background, black or blurred (Opus, medium)

`Background.kt`: `edgesBlack`, `blurBackground` (`BackgroundTest`; `CLAUDE.md`, Next features,
0.6.0: the picture).

### [x] A2. The picking page (by day, with the user, 2026-10-22)

An artifact page with a database for the picks: 253 APODs × six versions, 148 clear picks, always
Fill (`CLAUDE.md`, Next features, 0.6.0: the picture).

### [x] A3. `Store`: the whole picture per post, on black or blurred (Opus, medium)

`<source>.whole`, set on black or blurred; F1's `fit` gone (`CLAUDE.md`, Wallpaper: The whole
picture).

### [x] A4. ⛶'s Whole picture toggle; Size goes (Opus, medium)

The toggle in ⛶ mode; the Size row gone (`CLAUDE.md`, Choosing the part).

### [x] B17. The APOD-mode video notice follows `standInReady` (Opus, medium)

`VideoNotice` branches on `standInReady` in APOD mode too (`CLAUDE.md`, Video days on the page).

### [x] CR2. Code review of what changed since 910bfeb, read as a whole (Opus, high)

`review-0.6.0-cr2.md` (deleted 2026-10-26, in git); its bugs fixed by B18, finding 3 by B21,
finding 4 accepted.

### [x] B18. CR2's bugs (Opus, high)

An alarm in a going run arms the retry again; a stop read from `stoppedBySystem`, not the
interrupt flag alone (`CLAUDE.md`, Retries by the alarms).

### [x] V11a. Final pass 1: what the system does to the new job (Opus, high)

STOP for Android, no app bug; its findings 2–4 became B19 and B20 (Remaining 4h).

### [x] V11d. Final pass 2: R8 and the update from 0.5.0 without opening the page (Opus, high)

Merged (Remaining 4h).

### [x] V11b. Final pass 3: the rules on a faked clock (Opus, high)

Merged (Remaining 4h).

### [x] V11c. Final pass 4: what a user does to the new UI (Opus, high)

Merged (Remaining 4h).

### [x] B19. The job: a killed run keeps its retry; the switch's stop isn't logged as a failure (Opus, high)

A killed run (`runGoing`) keeps its retry and counts as failed; the switch's stop logs an info
line (`CLAUDE.md`, Retries by the alarms).

### [x] B20. Set says when its setup couldn't be saved (Opus, medium)

"Couldn't save the setup. Check the free space." when `saveSetup`'s commit wasn't written
(`CLAUDE.md`, Set as wallpaper).

### [x] B21. One source after a day offline: the newest goes on (Opus, high)

A post stored over 24 h after the last good check is marked (`afterGap`) and goes on
(`CLAUDE.md`, Turns, rule 2).

### [x] V12. The final pass again, on the tree with B19–B21 (Opus, high)

V12b–d PASS (master 654848c), V12a 3 items, its rest as V12e and V12f; no `BUG:` line.

### [x] B22. The stand-in not twice with APOD alone too; the video notice says so (Opus, high)

`Stored.standInShown` with one source too; the notice by the same check (`standInSkipped`;
`CLAUDE.md`, Turns; Video days on the page).

### [x] B23. No "kept" while something takes APOD's place (Opus, medium)

`videoDayKept` only when nothing will take APOD's place (`CLAUDE.md`, Problems and status lines).

### [x] B24. A run's bookkeeping before `jobFinished` (Opus, medium)

`ranJob` before `jobFinished`, so a death between them no longer counts a failure (`CLAUDE.md`,
Retries by the alarms: The order at a run's end).

### [x] V12e. V12a's rest 1: Doze and battery saver (Opus, high)

PASS 2026-10-27; battery saver holds an Optimized app's network (`on-time-0.7.0.md`).

### [x] V12f. V12a's rest 2: reboot, the clock, and what happens during a run (Opus, high)

PASS 2026-10-27 (Remaining 4l).

### [x] SIM. `:core`: the rules over random months (Opus, high)

`RulesSimulationTest`: 240 seeds, no bug, each invariant shown able to fail; a second picture on
one date left for 0.7.0 (`CLAUDE.md`, Open questions 3).

### [x] B25. The pause hint's unused line on Android 12; the weekday dialog's false sentence (Sonnet, medium)

Both texts (`CLAUDE.md`, The pause hint; Weekday dialog); the Android 12 line unseen on a device
(E postponed).

### [x] P. Phone pass on the FP6 (Opus, high)

PASS 2026-11-02 with the user, no `BUG:` (`.coord/runs/p/report.md`); what it didn't see: the
skill's Still untested.

### [x] W. A week on the FP6 before the tag (Opus, medium)

PASS: every morning Tue 27 Oct to Mon 2 Nov on the FP6 and the tablet, the Zurich case included,
no `BUG:` (`.coord/runs/w/report.md`).

### [ ] E. Android 10, 12, 13 and 14 in the emulator (Opus; E0 high, E1a–E4 medium)

**Postponed to 0.7.0** (user, 2026-11-02: "i want to release 0.6.0 asap"); 0.6.0 ships 10–14 untested.

(User, 2026-10-29: before the tag, a baseline for 0.7.0; Remaining 4j.) The findings, the
decisions, the setup and the packages are in `emulator.md`: the emulators `apodroid-29`, `-31`,
`-33`, `-34` (set up 2026-10-29); **E0** by day (the night shift learns an emulator), then
**E1a** and **E1b** (10), **E4** (14), **E2** (12), **E3** (13) in one night, on W's candidate as
root. Its
setup found **B25** (the pause hint's unused line on Android 12; draft in `emulator.md`).
- **Done when:** E0, E1a, E1b and E2–E4 done there (each list ran or is under Not tested, each bug a `BUG:` line);
  the skill's Still untested Android 10–14 line updated with what the emulator can't tell
  (`emulator.md`).

### [x] U0. Setup row, lean texts, ⛶'s ✓ (Opus, medium): done 2026-11-02

Setup and Advanced as Settings rows, the mockup's texts, ⛶'s ✓ sets at once (`Store.savePart`,
`setPart`); fa4b45c (`CLAUDE.md`, Settings; Choosing the part).

### [x] R. Docs, changelog, store texts, screenshots (Opus, medium): done 2026-11-02

`changelogs/10.txt`, `1.png`; the tag's build (fa4b45c's tree, release-signed) passed on the FP6
over W's candidate (the skill, Releasing). sha256, kept for F-Droid's build: unsigned
`f1caca75d68f49cbe56678f19ef85fc72969b1f61c5af15ee546b0b77cb90019`, signed `apodroid-v0.6.0.apk`
`aee1779bc4f86179077fb74eb12656218f488590298451ded33cb1378a91578d`.

### [x] T. Commit and tag (Sonnet, low): done 2026-11-02

One commit on master (the changelog, `1.png`, the docs), then the annotated tag `v0.6.0`.
