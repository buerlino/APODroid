# Plan for 0.6.0 (APOD first; all NASA pictures; the features from 0.1.1's review)

Planned 2026-10-19. Release target: 0.6.0 (versionCode 10), not before Tue 3 Nov (W, user
2026-10-26). Work through the
packages in order, one at a time: build, show it on the device, wait for the user. Tick them off
and note where the work differed from the plan. Each package writes its decisions into
`CLAUDE.md` (the source of truth) when it lands; this file is the how. Finished packages become
one line each here; git keeps their notes.

**The Decisions and "Set applies the setup" below are those of 19–20 Oct** (every night bundle
carries them). Since then: the change times by the clock, and Set doesn't restart them (J1–J2);
no Size row (A3–A4); no "Wi-Fi only" (B10); "More NASA pictures" and "New picture at" at the top
of Setup (J3); a stand-in not twice in a row (B15). Where they differ, `CLAUDE.md` is right.

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
    settings as in "Set applies the setup" below. Named "More NASA pictures" since S5.
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
    as U4 decided), shown only while the daily switch is on (S5). Choosing "Use my
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

- **A draft until Set.** "Where", the list (ticks and screen dropdowns),
  "Change every", "Size", "If APOD is a video", a newly picked "my picture", ⛶'s part and
  "More NASA pictures" change only the page. The job keeps the setup last set. A rotation and
  the system killing the app keep the draft (Android's saved state); closing the app drops it.
  - **The daily switch applies at once, both ways** (user, 2026-10-20 evening, after S4's
    night: "a setting that impacts how the entire app works should be instant"; S4f2; S4 had
    it in the draft). Off cancels the job at once. On schedules it and sets the stored setup
    at once (the clock restarts, both records forgotten, as 0.5.0's switch did); the other
    settings' draft changes stay a draft until Set, still highlighted.
  - **"My picture"** is a file: picking one copies it to `files/fallback-draft.jpg`; Set renames
    it to `fallback.jpg`. A start without a saved draft deletes a leftover draft file.
  - **⛶'s part:** ✓ closes crop mode and keeps the part in the draft (per source: the post's
    date and picture URL, the position); Set stores it, the job uses only the stored one; a
    new post drops a draft part, as a new picture starts centred. ✓ no longer sets the
    wallpaper (it did since 0.3.1).
- **Two kinds of state on the page.** The controls and the sources the page offers (the title
  dropdown and the swipe; the tabs until S6) follow the draft: turning "More NASA pictures" on
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
- **"More NASA pictures"** (user, 2026-10-20 evening, after a review: the mockup's "Extended
  API mode" is a developer's word, and the switch is the only way to NASA and Earth) is the
  new name of the switch "All NASA pictures" (the pref stays
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
  Pictures from                                    More NASA pictures
    [✓] APOD    [Both ▾]
    [✓] NASA    [Home screen ▾]
    [ ] Earth   [Both ▾]                           greyed out, the screens it had last
  Change every  [24 h | 12 h | 6 h | 3 h]          daily switch on, a screen takes turns
  Size    [Fill | Fit]
  If APOD is a video  [Skip it ▾]                  daily switch on only
[        Set as wallpaper        ]                 always
Advanced settings ▸                                closed by default
  [switch] Wi-Fi only                              daily switch on
  [switch] Save every picture ⓘ                    daily switch on
    Largest original  [20 MB ▾]                    "Save every picture" on (F6)
  [switch] More NASA pictures
```

- **Setup** opens by itself while the draft differs from the store (the mockup opens it with a
  change), and a set that worked closes it. Turning "More NASA pictures" opens it too, since
  "Where" and the list swap.
- **The summary** says what is stored, not the draft: "APOD on both screens" (APOD mode), else
  each ticked source "<short name> on <home screen | lock screen | both screens>", joined by
  ", "; then " · turns every <interval>" while a screen takes turns and the daily switch is on;
  then " · Fill" or " · Fit".
- **The highlight** in the mockup: a segmented row's chosen segment (`tertiaryContainer` /
  `onTertiaryContainer`) and its borders, a list row's tick box, name and dropdown, the video
  dropdown's border and text, the "Setup" header while anything in it differs, "Advanced
  settings" while the mode does. Not the daily switch: it acts at once (S4f2).

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
scripts/night.sh --until <HH:MM>    # B1, S4f2, S4f3, S5, F6: the queue in night.md
scripts/night.sh --until <HH:MM>    # after the user's look at S5: B2, B4–B6, S6, then V3, V5, V6, V8
scripts/night.sh --until <HH:MM>    # B10, B7, B8, B9, then the regression: the queue in night.md
scripts/pipeline.sh B12; scripts/pipeline.sh B11    # by day, 2026-10-22
scripts/night.sh --until <HH:MM>    # J1, A1, J2, A3, A4, J3, then the regression (2026-10-23)
scripts/night.sh --until <HH:MM>    # B15, B16, then the regression (2026-10-24)
scripts/night.sh --until <HH:MM>    # B17, CR2, B18, V11a, V11d, V11b, V11c, then the regression (2026-10-25)
scripts/night.sh --until 07:30      # B19, B20, B21, V12a, V12d, V12b, V12c, then the regression (the night of 2026-10-25)
scripts/pipeline.sh B22    # by day, 2026-10-26, one at a time: then B23, B24 (V12e, V12f: Remaining 4j)
ANDROID_SERIAL=HA24BZ2Y claude --agent rosie --model sonnet --effort medium "Do V7 in .claude/skills/build-apodroid-android/plan-0.6.0.md"    # session 1 at bedtime, the night after
ANDROID_SERIAL=HA24BZ2Y claude --agent rosie --model sonnet --effort medium "Do V7's second session in .claude/skills/build-apodroid-android/plan-0.6.0.md"    # ≥ 6 h later, before pascaline
scripts/pipeline.sh V12e; scripts/pipeline.sh V12f    # after V7's second session, if not run before it
scripts/pipeline.sh SIM    # after B23, by day or night (no tablet)
scripts/pipeline.sh B25    # by day or night (no tablet); then its emulator check by hand on apodroid-31
claude --model opus --effort high "Do P in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model opus --effort medium "Do W's start in .claude/skills/build-apodroid-android/plan-0.6.0.md"    # with P's install
claude --model opus --effort medium "Do W's morning read in .claude/skills/build-apodroid-android/plan-0.6.0.md"    # each morning, Tue 27 to Mon 2 Nov
claude --model opus --effort high "Do E0 in .claude/skills/build-apodroid-android/emulator.md"    # by day; then E1a, E1b, E4, E2, E3 one night (emulator.md, Packages)
claude --model opus --effort medium "Do R in .claude/skills/build-apodroid-android/plan-0.6.0.md"
claude --model sonnet --effort low "Do T in .claude/skills/build-apodroid-android/plan-0.6.0.md"
```

Devices: the skill, Working on the phone. The tablet is free for testing; the FP6 is the
user's own phone, touched only in P. The build check:
`ANDROID_HOME=~/Android/Sdk ./gradlew :core:test :app:lintRelease :app:assembleRelease`.

## Done

Built by the night shifts of 2026-10-20 (`night.md`; the reports
`.coord/reports/2026-10-20-morning-shift.md` and `2026-10-20-evening-shift.md` are gitignored), merged on `dev`, approved by the user that morning (texts and night defaults from
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
3b. **The evening of 2026-10-20** (user, after the night's report): S4's texts kept ("Set as
   wallpaper", ✓'s "Done", "Setup saved. Nothing to set yet."); `dev` fast-forwarded into
   master. S5 and S6 by night after all (the user), the user's look between them. Three of
   S4's night defaults asked and approved: a failed set wins the toast, Set in crop mode keeps
   the dragged part, a screen whose column is empty after Set keeps its record. The rest of
   2a47f00's list (technical: the order of scheduling and update, when the list is saved, the
   offered sources) stands as pass 4 checked it, not asked.
   New: **B1** (the regression's status line), **S4f2** (the daily switch at once, both ways),
   **S4f3** (Set doesn't stop a running job: pass 5's side effect, fixed, not accepted),
   **F6** (a size limit for the job's saves).
   **Only APODroid's nights use the tablet** (user, 2026-10-20 evening: pascaline's rules
   also name HA24BZ2Y, with their own device lock this repo can't see; the user tells
   pascaline's nights to leave it). **The next night:** B1 → S4f2 →
   S4f3 → S5 → F6. The user looks at S5 (and the button that moves after Set, the report's question)
   in the morning; S6 waits for that look (night.md: an After naming the user). **The night
   after:** S6, then V3, V5, V6.
3c. **The night of 2026-10-20 to 21** (user, after the night's report): B1, S4f2, S4f3, S5 and
   F6 merged with tablet PASS, `dev` fast-forwarded into master. Approved: S5's texts, colours
   and night defaults; F6's label, default and MB; the ⓘ text (B3). Decided: B2 (equal columns
   change together), B3 (" · My picture" in APOD mode; done by day with the ⓘ text), B5 (a
   failed run keeps its retry, instead of S4f3's night default), B6 (quick taps set once);
   B4 (the lost draft: reproduced by day, a second page on top; fixed by night).
   `scripts/pipeline.sh` now takes a row's own ID first (S4f2 had run on S4's row and
   section). A review of this plan (same night): V5 no longer waits for S6, B5 drops a kept
   note on a direct reschedule, B6 on Opus with its edge, V3 without B2's and B4's checks
   (their own rows test them). **The next night:** B2 → B4 → B5 → B6 → S6 → V3 → V5 → V6 →
   V8, then the regression. S6 no longer waits for the user (S5 approved); the user
   looks at it in the morning, and a change there reruns the parts of V3 and V6 it touches.
3d. **The night of 2026-10-20 to 21** (user, 2026-10-21 morning, after the night's report
   `.coord/reports/2026-10-20-night.md`): B2, B4, B5, B6, S6, V3, V5, V6 and V8 merged with
   tablet PASS (B6 and S6 after one fix round each), regression 4 clean; `dev` fast-forwarded
   into master. Approved: S6's texts ("APOD ▾", the short names in the menu, "Picture from: …")
   and its night defaults (the name alone with one source, the failure text on the shown page
   only, a fresh pager when the list changes). Kept as built: B6's off and on 0.35–1 s apart
   setting the same picture twice (the end state is right); the pick of Earth surviving the
   mode switch off and on in the draft (as a rotation); no Compose UI tests. To fix: **B7**
   (the job keeps its retry: a stop during the saves counts as failed, pass 8's QUESTION 2;
   a Set just after a failed run notes its reschedule instead of replacing the retry,
   regression 4), **B8** (B4's edge: the draft's picture survives a second page, pass 8's
   QUESTION 1), **B9** (a no-break space before ▾ and ▸, V3's font scale 2.0). After a review
   of B7's spec (same morning): its tests that B7 changes are named, off during a run leaves
   no retry due, and a note waits for one retry at most (asked; the user's answer: remove
   "Wi-Fi only" instead of letting it wait, **B10**, and one retry at most). **Tonight:**
   B10 → B7 → B8 → B9, then the regression; B10's tablet check tidies the tablet first.
3e. **The night of 2026-10-21** (user, 2026-10-22 morning, after the report
   `.coord/reports/2026-10-21-night.md`): B10, B7, B8 and B9 merged with tablet PASS (B10 after
   one fix round), regression 5 clean; master fast-forwarded to `dev` 0b9c744. B9's wrap seen
   ("NASA Image of the / Day ▾"). To fix, **by day** with `scripts/pipeline.sh`, one at a time:
   **B11** (pass 11's QUESTION: a Set from a second page stores the first page's unset pick)
   and **B12** (a review of B7's code: a retry due kept after a failed retry made every Set wait
   for Android's growing backoff while a source kept failing). **Tonight:** V7 alone.
4. **V7** (user, 2026-10-21; the night of 2026-10-22, after B11 and B12): a night of its own, nothing else queued:
   the user starts session 1 by hand at bedtime and session 2 in the morning, at least 6 hours
   later and before pascaline starts. It tests the final tree, so a B package after it that
   touches the job or turns reruns its part.
4a. **The order to a stable 0.6.0** (user, 2026-10-21 evening, after B11, B12 and B12f1 merged:
   "brute test now, then V7, then code review, then P, then release"): **V9 and V10** (the
   brute test, rosie), then the **code review** of the whole app by reading (CR, Opus, no
   device), then their bugs fixed (**B13**, **B14**, winston: clear bugs only, decisions as
   questions), then the regression: **all in that night's shift** (user, 22:50: "put as much
   into the night shift as possible, we have the tablet all night and about 8h"). **V7** moves
   to the next night, alone, on the fixed tree (it needs the tablet untouched for 6 hours).
4b. **The night of 2026-10-21** (user, 2026-10-22 morning, after the report
   `.coord/reports/2026-10-21-night.md`): CR and B13 merged (B13 tablet PASS), regression 6
   clean; V9's and V10's results merged by hand (their FIX was the bugs B13 fixed); master
   fast-forwarded to `dev` 910bfeb. Decided: crop mode and an open dropdown close on a rotation
   or a resize, left as is (`CLAUDE.md`, Choosing the part); B14's scope (its section): CR's
   findings 1–5, finding 7 as "a hidden draft value goes back to the stored one", the lock line
   during a Set; the job's +6 h after a Set into APOD mode checked in B14's tablet check and in
   P before any doc changes. **By day:** B14 with `scripts/pipeline.sh B14`. **Tonight:** V7
   alone (it tests the final tree, B14 included: finding 1 touches turns).
4c. **The picture time** (user, 2026-10-22 evening, after the FP6's job waited until 10:22
   for a 06:05 APOD; `CLAUDE.md`, Next features, 0.6.0: the picture time): three settings
   (New picture at, default 06:00; Turns start at, default 06:00; Opening the app, default the
   wallpaper waits), built in 0.6.0 before P. **J0 done** (2026-10-23 morning: the pair of
   alarms, J0 and J2). **That night: J0** by hand, two rosie sessions as
   V7's, the tablet on battery (instead of V7). Then J1 → J2 → J3 by day or night, the user
   approving J3's texts on the tablet, then **V7** on the new job (its setup takes the picture
   time too), then P, R, T. The probe for J0 is built by day first (J0, The probe).
4d. **The picture** (user, 2026-10-22 evening, after the mock-up and the user's 148 picks;
   `CLAUDE.md`, Next features, 0.6.0: the picture): always Fill (middle by default), a
   per-picture "Whole picture" toggle in ⛶ mode, Background [Blurred | Black], Fill's part
   [Middle | Brightest area]. The smart Auto and its learning were tried and dropped (always
   Fill matched 90% of the picks, no rule did better). Order: A1 → A3 → A4, beside J1–J3 (A3
   and J2 both touch `Store`: one at a time), then V7 and P. Live (the day's pan) with the
   preview: 0.7.0, right after.
4e. **The final plan** (user, 2026-10-23 night, after the FP6 again waited for a page open: the
   6 h job's windows at 04:22 and 10:22 missed the 06:05 APOD; two reviews of the day's plan in
   another session): **simplified** to the three rules, one time, one clock (`CLAUDE.md`, Next
   features, 0.6.0: the picture time; the new settings rule under How the user works). Two new
   controls ("New picture at" in Setup, ⛶'s "Whole picture"), the background automatic by the
   picture's edges, "More NASA pictures" at the top of Setup, the job's note and retry rules
   removed with the fixed times (J2). Dropped: "Turns start at", the ↑ ↓ order, "When nothing
   is new", the "Opening the app" setting, "As soon as it's out", the per-source posting times,
   the Background and Fill's part settings, "my picture"'s whole switch.
   Then the third review (clock rules with a 3-hour window, the alarms retrying, the order).
   **Tonight (Fri 23 Oct):** J1 → A1 → J2 → A3 → A4 → J3 by night shift (night.md, the queue;
   A3 after A1 only, A4 after A3 only), then the regression (J3 and the regression may slip to
   Saturday: the night is tight); J3's and A4's texts are placeholders the user approves in the morning
   from the screenshots. **Sat 24:** the user's look, fixes by day. **Sun 25 to Mon 26:** V7
   (by hand, two rosie sessions). **Mon 26 evening:** P starts with the update on the FP6 and
   its real night to Tue 27 (P, the picture time on the FP6), the rest of P with the user.
   Then R and T. The real 06:05 case (APOD after the picture time) happens only from 2 Nov
   (the US switches on 1 Nov): J2's faked clock is enough (user, 2026-10-23 night).
4f. **The night of 2026-10-23** (user, 2026-10-24 morning, after the report
   `.coord/reports/2026-10-23-night.md` and two reviews of `dev` by reading, J1's with scratch
   tests): J1, A1, J2, A3, A4 and J3 merged with tablet PASS, regression 7 PASS; master
   fast-forwarded to `dev` 84404ed. Approved: A1's thresholds (from A3's real sets), A4's and
   J3's texts and J3's two night defaults, the traffic (4–8 checks a day, hourly retries),
   B14's toast default. Decided: the window by publish time with a check at its end, Los
   Angeles with turns accepted, the APOD-mode video notice "… takes its place" (`CLAUDE.md`).
   To fix: **B15** (`:core` and `Store`: the window, the stand-in, the notice) and **B16** (the
   job: the reviews' findings 1–4). **Tonight (Sat 24):** B15 → B16, then the regression.
   **Sun 25 to Mon 26:** V7 (by hand), also looking at `BOOT_COMPLETED`. **Mon 26 evening:** P.
4g. **The release gate** (user, 2026-10-24 evening, from a review of the plan; `CLAUDE.md`,
   0.6.0's release gate): a code freeze, then **CR2** (a review by reading of everything since
   910bfeb) and **B18** (its bugs) before **V11a–d**, the final tablet pass. Then V7 and P on
   that tree. The night of 24 Oct runs B15, B16 (and B16f1); right after it, a second shift:
   B17 → CR2 → B18 → V11a → V11d → V11b → V11c (riskiest first, user) → the regression. What
   doesn't fit runs on Sunday by day. **Sun 25 to Mon 26:** V7. **Mon 26 evening:** P. Then
   R and T.
4h. **The night of 2026-10-24/25** (user, 2026-10-25 morning, after the report
   `.coord/reports/2026-10-24-night.md`): B17, CR2, B18 and V11a–d merged (V11a's STOP was
   Android, no app bug; its results merged by hand), regression 9 PASS; master fast-forwarded.
   Accepted (`CLAUDE.md`, 0.6.0's release gate): the RARE bucket's quota, the menu's swallowed
   tap, CR2's finding 4, the 97% edge threshold. To fix (user: "fix all bugs and retest"):
   **B19** (a killed run keeps its retry; the switch's stop not logged as a failure), **B20**
   (Set says when its setup couldn't be saved), **B21** (one source after a day offline), then
   the final pass again in full, **V12a–d**. **Tonight (Sun 25):** B19 → B20, B21, then V12a →
   V12d → V12b → V12c → the regression. **Mon 26 to Tue 27:** V7 (tablet, by hand) and P's real
   night (FP6: the release build installed in the evening without opening the page), side by
   side. Then the rest of P by day, R and T.
4i. **The night of 2026-10-25** (2026-10-26 morning, after the report
   `.coord/reports/2026-10-25-night.md`): B19, B20, B21 merged (B19 and B20 tablet PASS; B21
   by V12b), V12b, V12c and V12d PASS, regression 10 PASS, no bug; master fast-forwarded to
   654848c, the shift's tree. V12a ran 3 of its items in its 50 minutes (the 15 real minutes
   twice), so the gate waits for the rest (V12, Status). Decided (user, 2026-10-26, from the
   report's questions): the stand-in not twice with APOD alone too, the notice by the same check
   (**B22**); no "kept" while something takes APOD's place (**B23**); a run's bookkeeping before
   `jobFinished` (**B24**, pass 23's note); ZuiMemoryCleaner no 0.6.0 change (noted in
   `on-time-0.7.0.md`). **Today (Mon 26), by day, one at a time:** B22 → B23 → B24, then V12a's
   rest as **V12e** and **V12f** (rosie, the tablet), so V7 and P's real night test the final
   tree. **Mon 26 to Tue 27:** V7 and P's real night, as 4h. Then the rest of P, R and T.
4j. **More testing before the tag** (user, 2026-10-26 afternoon, from a report on the repo's
   state: the tested surface converges, V12 found no `BUG:` line, but no real night has run the
   new job, the job and the rules gave 13 of the 24 fix packages B2–B24, and the case most
   European users meet each morning starts only on 1 Nov). **W**, a week on the FP6 from P's
   install to the morning of Mon 2 Nov; the tag not before its read-out. **SIM**, a seeded
   random test of the rules over months in `:core` (test code only; a finding becomes a
   `B<n>`). **E**, Android 10, 12 and 13 in the emulator: only on the user's yes (`CLAUDE.md`
   says no emulator); without it Android 10–14 ships untested, an accepted risk. The order:
   **Mon 26 by day** B22 → B23 → B24; **Mon 26 evening** V7's session 1 and P's install
   (versionCode 10), which starts W, on the tree with B22–B24, also when V12e and V12f haven't
   run: they change no code, so they run on **Tue 27 by day** after V7's second session (the
   tablet is V7's overnight). **Tue 27 to Thu 29** SIM; the rest of P by day. **Tue 27 to Mon
   2 Nov** W's morning reads; a `B<n>` from any of them as the gate says (a fix to the job or
   the rules restarts W's count, W). **Tue 3 Nov** R, then T.
4k. **The evening of 2026-10-26**: B22, B23 and B24 merged with tablet PASS, Opus pass 26 clean
   (210 tests; its question, B22's guard within a turn, kept by the user); SIM merged early
   (240 seeds, no bug; each invariant shown able to fail; its question, a second picture on the
   same date, left for 0.7.0, `CLAUDE.md`, Open questions); master db78d0c, versionCode 10 from
   423f082. P's install: by mistake first fresh (0.5.0 uninstalled), then redone as planned:
   0.5.0 back through Obtainium and set up, 0.6.0 (sha256 21e494b6…) over it at 20:24 without
   opening the page; the migration and the alarms by `MY_PACKAGE_REPLACED` in the log, the
   wallpaper untouched (`.coord/runs/w/start.md`). The FP6 charges overnight (pascaline's
   night), so W's first night has no Doze; its log is recorded on the PC
   (`.coord/runs/w/logcat-night.txt`: the phone's 256 KB buffer rolled over within 15 min).
   V7's session 1 at 20:31, the tablet on battery (`.coord/runs/v7/start.md`). **Tue 27:** V7's
   second session (about 09:30), W's first read, then V12e and V12f.
4l. **Tue 27 Oct**: V7 PASS (lock: Tuesday's Earth at 05:34, the first waiting check after 05:00;
   home: Tuesday's APOD at 06:19; one set per screen; the log before 06:19 rolled over, the
   fallback pushed 35 h ahead by earlier `run -f`s), V12e PASS (battery saver holds an Optimized
   app's network: `on-time-0.7.0.md`), V12f PASS. W's first morning PASS (`.coord/runs/w/report.md`:
   06:12 NASA's Monday post, NASA's turn by the 12 h turns; the window's checks set nothing) and
   the migration on the FP6 confirmed by the user in Setup. Left for the gate: the rest of P (with
   the user), W's mornings to Mon 2 Nov; then R and T, Tue 3 Nov at the earliest.
4m. **Wed 28 to Fri 30 Oct**: W's FP6 reads PASS each day (every change at 06:00 and 18:00 as the
   rules say; the log rolls over by the read, so the times come from the page and the wallpaper
   ids). The tablet's half: the rules and the retries PASS (Thu: Earth's 502 morning, a Wi-Fi gap,
   airplane mode), but ZuiMemoryCleaner force-stopped the app twice (Wed 13:57, Fri 01:34), each
   time nothing until the page opened (`on-time-0.7.0.md`, Lenovo; no 0.6.0 change). Thu: APOD's
   day-first title prefix (`CLAUDE.md`, Open questions 4: 0.7.0).
5. P on the FP6 with the user (the release build over the real 0.5.0 install: it also covers
   R8 and the update on the final tree), then W, R and T.

### [x] F1. Fill / Fit (Opus, medium)

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

### [x] F3. Share (Sonnet, medium)

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

### [x] F4. Image credit (Opus, medium)

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

### [x] F5. Save every picture (Opus, medium)

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

### [x] S1. `:core` and `Store`: a set by hand changes the settings, no holds (Opus, high)

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

### [x] S2. Settings: the list instead of the grid, the order, the daily switch off (Opus, medium)

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

### [x] S4. Set applies the setup: the draft, `Store.saveSetup`, the old set paths gone (Opus, high)

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
  - **"More NASA pictures"** in the draft swaps "Where" and the list at once (as now) and the
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
  - Set puts on each screen what the setup says: APOD mode with each "Where"; More NASA pictures
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

### [x] B1. The status line after Set with "Skip it" on a video day (Sonnet, medium)

From regression 2 (2026-10-20 night; the user: a bug, its own package before S5).
- Steps: fake an APOD video day, the daily switch on, APOD alone, "If APOD is a video" = "Skip
  it", press Set.
- Happened: the status line reads "Wallpaper changed 20 Oct, 19:28", no source (evidence:
  `.coord/regressions/regression-2/tablet/s27-video-skip.png`, `s28-video-skip-after-job.png`).
  Should: "Video day, wallpaper kept" (nothing was set). The cause (read in the code,
  2026-10-20): Set forgets the record's picture (`restartAndForget` → `Record.forget`, owner
  kept), "Skip it" alone sets nothing, so the picture stays null, and `videoDayKept` returns
  false on `record?.picture ?: return false`.
- **The fix** (from a review of this plan, 2026-10-20 evening): the record alone can't tell
  "forgotten by Set" from "never set": on a fresh install's first video day with "Skip it",
  `dueWallpapers` writes the same record (owner APOD, no picture) though nothing is set. What
  tells them apart is the screen's `wallpaperSetAt`: Set doesn't clear it, a fresh install
  has none. So `videoDayKept` takes whether the screen was ever set (`Store.screenState` passes
  `wallpaperSetAt != null`), and a record with no picture counts as kept when it was set
  before (APOD ticked, APOD's turn, nothing standing in: the rest of the rule as now).
  **`ScreensTest.kt:328`** asserts the opposite for `Record(APOD)` ("Never set, or forgotten:
  the next set decides"): split it into "never set: not kept" (stays `assertFalse`) and
  "forgotten after a set: kept" (now `assertTrue`), and add a fresh-install test (the record
  `dueWallpapers` writes on a first video day with "Skip it", never set: not kept). S4f2's
  switch-on forgets the records the same way, so the same fix covers it.
- Also: the KDoc above `chooseVideoDays` in `MainActivity.kt` still says "Skip it" sets nothing
  (from pass 3); make it say what it does since S4.
- **Files:** `core/.../Screens.kt` (`videoDayKept`), `ScreensTest`, `Store.kt`
  (`screenState`), `MainActivity.kt` (the KDoc), `CLAUDE.md` (Problems and status line: the
  "kept" rule).
- **Tablet:** the steps above: the line says "Video day, wallpaper kept", before and after the
  job's next run; the same with turns on one screen (no "kept" line there: the turn passes);
  a fresh install (`pm clear`) on a faked video day with "Skip it" and Set: no "kept" line
  (nothing was ever set; then set the tablet's prefs back).
- **Done when:** a regression test that fails on the old code, the fix, the tablet passes.

### [x] S4f2. The daily switch applies at once, both ways (Opus, medium)

After B1 (both touch `Store.kt`). The user's decision in "Set applies the setup" (2026-10-20
evening): the daily switch leaves the draft.
- **Off:** writes `daily` = false and cancels the job at once; the status lines and the
  battery hint go (they follow the stored switch).
- **On:** writes `daily` = true, then as 0.5.0's switch did: the clock restarts and both
  records are forgotten (`restartAndForget`), `update(page = true, set = true)` off the main
  thread sets the **stored** setup, then the job is scheduled (after the set, as Set does:
  `DailyJob.schedule` with turns, else `ensure`). The other settings' draft changes stay a
  draft, still highlighted after S5. Toasts as Set's ("Wallpaper set", "Couldn't set the
  wallpaper"); none when nothing was set (the status line says it). Turning it on while a
  Set or an update runs: one more run after it, as a tick did.
- **Set** keeps applying the rest of the draft; it reads the stored switch for scheduling or
  cancelling (no change to it), and with the switch off is still a one-off set.
- The draft drops `daily` (`Setup`, `draft()`, the saved state's bundle key); what showed by
  "the draft's daily switch" (the hidden rows) now follows the one switch.
- **Files:** `MainActivity.kt` (`switchDaily`, the draft, Set), `Store.kt` (`Setup`,
  `saveSetup`), `CLAUDE.md` (The app, Settings: the switch out of the draft list, the
  decision with its why; Background update: turning the switch on), the skill's "Where things
  are".
- **Tablet:** off → `dumpsys jobscheduler` has no job at once, prefs `daily` false, the
  status lines gone; on → the job scheduled, both screens set to the stored setup (with a
  pending draft change, e.g. "Size" or a tick: not applied, still the stored one), the clock
  restarted; a rotation and `am kill` keep the switch's state (it's a pref now); APOD mode and
  More NASA pictures; no crash.
- **Done when:** the checks pass, the tablet passes, `CLAUDE.md` says the new rule.

### [x] S4f3. Set doesn't stop a running job (Opus, medium)

After S4f2 (both change how Set and the switch schedule). Pass 5's side effect (2026-10-20
night), which the user wants fixed (the same evening): Set with turns calls
`DailyJob.schedule`, and scheduling the job's id again stops a running run, so its saves
("Save every picture") wait for the next run. The same holds for `ensure` when the timing
changed (Set without turns after a change of the list, "Wi-Fi only", the switch going on).
- **The rule:** no reschedule stops a running run. While the job runs (a process-wide flag in
  `DailyJob`, set in `onStartJob`, cleared when the run ends; the page and the job share one
  process), a wanted `schedule` or `ensure` is noted instead, and the run does it itself
  right after `jobFinished`. Turning the daily switch off still cancels at once, also a
  running run (off is off).
- If the process dies before the noted reschedule, `onResume`'s `ensure` covers a changed
  timing; a fresh period for the same timing is then lost (turns stay on their clock, the job
  may come up to a period off): accept, say so in `CLAUDE.md`. Night default if a simpler way
  fits better (e.g. a pref instead of the flag), with the alternative in the report.
- **Files:** `DailyJob.kt`, `MainActivity.kt` (the calls at Set, the switch, "Wi-Fi only"),
  `CLAUDE.md` (Background update: the rule; drop pass 5's "known side effect" wherever it is
  written), the skill's "Where things are".
- **Tablet:** a long run (`cmd jobscheduler run -f`, with the proxy in
  `private/wp7-handoff/proxy/` stalling one source so it lasts), Set with turns during it: the
  run isn't stopped (`logcat`: no "stopped", its saves logged at the end with "Save every
  picture" on), then `dumpsys jobscheduler` shows the job scheduled afresh with the new
  timing; Set with no run going still reschedules at once; the switch off during a run stops
  it. If the proxy can't be set up, say so under Not tested.
- **Done when:** the checks pass, the tablet passes, `CLAUDE.md` says the rule.

### [x] S5. Settings: Setup and Advanced settings, the highlight, "24 h", "More NASA pictures" (Opus, medium)

After S4f3 (S4f2: the daily switch at once). The layout, the summary and the highlight of "Set
applies the setup" above.
- "Setup": a header row ("Setup ▸/▾", the summary under it while closed) that opens and closes
  the area (`rememberSaveable`); it opens by itself while the draft differs from the store in
  it, and closes after a set that worked.
- "Set as wallpaper" moves after "Setup", always shown.
- "Advanced settings": a header row, closed by default (`rememberSaveable`), with "Wi-Fi only"
  and "Save every picture" (the daily switch on) and "More NASA pictures"; turning that
  opens "Setup". The divider before the mode switch goes.
- The highlight: each setting compared with the store; while it differs its heading and its
  control in `tertiary` (segmented buttons' active colours `tertiaryContainer` /
  `onTertiaryContainer` and their border, the checkbox and the switch `tertiary`, the outlined
  dropdowns' border and text); the list per row; ⛶ while its part differs; the "Setup" header
  while anything in it differs, "Advanced settings" while the mode does.
- "If APOD is a video" only while the daily switch is on. "Day" → "24 h". The switch's
  label "More NASA pictures" (and the comments that name it).
- Screen readers: the headers as buttons with "expanded"/"collapsed" (`Role.Button`,
  `stateDescription`); a highlighted setting adds "not set yet" to its state.
- **Files:** `MainActivity.kt`, `CLAUDE.md` (Settings), the skill's "Where things are".
- **Tablet:** screenshots for the user of each state (closed with its summary, open, a change
  highlighted, after Set, Advanced open, the daily switch off), both modes, two panes and a
  phone-width window; font scale 2.0 at `wm density 344` (no word broken inside); the
  accessibility tree (`uiautomator dump`).
- **Done when:** the checks pass, the tablet passes, the screenshots are there for the user.

### [x] F6. A size limit for the job's saves (Opus, medium)

After S5 (its row is in "Advanced settings"). The user, 2026-10-20 evening, after the night's
heads-up that today's NASA original is a 208 MB PNG: "Save every picture" may not download
anything that size every day; a setting, the user decides, limited by default.
- **"Largest original"** (placeholder): a dropdown [10 MB | 20 MB | 50 MB | 100 MB | No
  limit], **default 20 MB**, under "Save every picture" in "Advanced settings", shown while
  that switch is on. At once, as "Save every picture" (not part of the draft). The pref
  `saveLimit` (MB; 0 = no limit).
- **The job only:** an original over the limit isn't downloaded in full; the job saves the
  stored picture instead (the smaller copy, as after a failure: `save(post, tryOriginal =
  false)`'s path). ★ by hand ignores the limit and always tries the original.
- **How the size is known:** the download's `Content-Length` before the body is read (NASA's
  feed also has the enclosure's `length`; use it if it is already parsed, don't add parsing for
  it); without a length, stop once the bytes pass the limit and throw the `.part` away. Either
  way it's a fall-back, not a failure (logged, no retry, `saveTried` noted as now).
- `:core`: the download takes a byte limit; a typed result or exception for "over the limit",
  with tests (a length over it, no length and a body over it, under it, no limit).
- The ⓘ dialog's text ("Each new picture is saved full size to Pictures/APODroid: a few
  hundred MB a month or more.", approved in C) may no longer be true at 20 MB: propose a new
  text in the report, don't change it.
- **Files:** `core/.../Post.kt` (the download), its tests, `Saving.kt` if the choice moves
  there, `Store.kt` (`save`, `saveInUse`, the pref), `MainActivity.kt` (the row), `CLAUDE.md`
  (Save every picture; Next features, From a review of 0.1.1), the skill's "Where things
  are".
- **Tablet:** never today's 208 MB NASA original (a full download may not finish in the
  job's ~10 minutes): a post whose original is 10–50 MB (a real one from the feed in
  `private/`, faked by prefs), with the limit 10 MB: the job saves the stored picture, no full
  download (`logcat`, the gallery's file size); with the limit 50 MB (or "No limit") the
  original; ★ saves the original despite the limit;
  the row hidden while "Save every picture" is off; font scale 2.0; the test files deleted
  from the gallery afterwards.
- **Done when:** the tests pass, the checks pass, the tablet passes, `CLAUDE.md` says it.

### [x] B2. Equal columns change together (Opus, medium)

From regression 3 (2026-10-20 night; rosie read it as by design; the user, after it: a bug, as
`CLAUDE.md` says equal columns are one wallpaper).
- Steps: "More NASA pictures" on, APOD on Home screen, NASA on Lock screen, Set (home APOD,
  lock NASA). Then both on Both, "Change every" 6 h, Set.
- Happened: home keeps APOD, lock keeps NASA, and at each turn both move on from their own
  owner (home NASA and lock APOD, then the other way round): the screens never match again.
  The cause (read in the code, 2026-10-20 night): `restartAndForget` keeps each record's owner
  (`Record.forget`), and `dueOn` moves each screen on from its own owner; only a screen with no
  record takes the other's (`dueWallpapers`). The same can happen without Set: a screen whose
  set failed retries later, when another source may have got a picture, and picks another
  next source than the other screen did.
- **Should:** with the same sources on both screens (equal columns, both with at least one),
  the lock screen follows home's owner and stamp, so both are due the same picture and
  `Plan.sets` makes it one set with both flags. In `dueWallpapers` (one reading of the clock, so
  it holds after Set, the daily switch on and every run), not only in `restartAndForget`.
  Home's owner wins (night default if another fits better, with the alternative). The
  stand-in, "Skip it" and an early change follow, as the lock screen takes home's due record.
- Columns that differ keep today's rules (each screen its own owner, one shared clock).
- **Files:** `core/.../Screens.kt` (`dueWallpapers`), `ScreensTest` (the steps above as a test
  that fails on the old code: after Set and at the next two turns both screens due one
  picture; the failed-retry case; columns that differ unchanged), `CLAUDE.md` (Next features,
  0.6.0: drop B2's "not built yet", and reword "not by equal columns: a screen still due a
  retry may differ for a while", which is no longer true for equal columns; Turns: the rule).
- **Tablet:** the steps above: after Set `dumpsys wallpaper` shows one shared wallpaper
  (`mWhich=3`), then `turn.sh` ends the turn and both screens change to the same picture;
  then columns that differ (APOD Both, NASA Home) still take turns on home only.
- **Done when:** a regression test that fails on the old code, the fix, the tablet passes.

### [x] B3. Two texts: " · My picture" in APOD mode, the ⓘ for saving (Sonnet, medium)

Done by day (2026-10-20, a day session, not by night): `pictureSuffix` and the ⓘ text in
`MainActivity.kt`. Tablet: APOD mode, a faked APOD video day, "Use my picture", Set: "Wallpaper
changed 20 Oct, 23:41 · My picture" (logcat "Set: wallpaper set: Own on HOME"); the ⓘ dialog
shows the new text. Not checked: font scale 2.0.

From the night's report (2026-10-20 night; the user's answers after it).
- **" · My picture" in APOD mode:** with "Use my picture" on an APOD video day the status line
  read "Wallpaper changed 20 Oct, 22:55" with nothing after it (regression 3), while a stand-in
  shows " · NASA". `pictureSuffix` in `MainActivity.kt` adds it only with `stored.allSources`:
  add it in both modes; the KDoc above it says so.
- **The ⓘ dialog** of "Save every picture" (F6 made the old text untrue at 20 MB): "Each new
  picture is saved to Pictures/APODroid: full size up to the largest original, else a
  screen-sized copy. A few hundred MB a month or more." (as `CLAUDE.md`, Save every picture).
- **Files:** `MainActivity.kt`, `CLAUDE.md` (drop the two "B3, not built yet").
- **Tablet:** APOD mode, a faked APOD video day, "Use my picture" with a picture, Set: the
  status line ends " · My picture" (a screenshot); with the switch on as before; the ⓘ
  dialog (a screenshot; font scale 2.0 too).
- **Done when:** the checks pass, the tablet passes, the screenshots are there for the user.

### [x] B4. The draft after a second start (Opus, medium)

From S5's tablet check (2026-10-20 night, rosie): a draft change, a tap on the picture (the
browser), back to the app: the draft was gone. **Reproduced by day** (2026-10-20, the tablet,
dev's build): `MainActivity` has no `launchMode`, so a start whose intent doesn't match the one
that opened the task puts a **second** page on top. It opens without the draft (Setup closed,
the stored setup), and its `onCreate` deletes `files/fallback-draft.jpg`, which the page under
it still uses. Seen: a task opened by `am start -n …/.MainActivity` (rosie's way), then a
launcher-style start → two pages (`dumpsys activity activities`: `* Hist #1`, `#0`, the
bottom one's intent without `act`/`cat`); the reverse too (launcher first, then `am start -n`).
Not affected, with the task opened from the launcher: the browser and back by Recents, and by
the launcher icon (Olauncher): one page, the draft kept. (WebLibre's Back stays in the browser.)
So users rarely hit it (a launcher or shortcut that starts the app another way), but every
night's tablet check does.
- **The fix:** one page, one instance: `android:launchMode="singleTop"` on the activity (a
  start while the page is on top reuses it; `onNewIntent` does nothing), or `singleTask` if
  rosie finds a way with another activity on top (App info, the photo picker) that still
  stacks a page; a night default with the why. Nothing else changes: closing the app (a swipe
  from Recents) still drops the draft, the system killing it keeps it.
- **Files:** `app/src/main/AndroidManifest.xml`, `MainActivity.kt` only if needed, `CLAUDE.md`
  (The app, Settings, "A draft until Set": what keeps the draft), the skill's "Where things
  are" and its how-to for starting the app (prefer the launcher intent: `monkey -p
  io.github.buerlino.apodroid -c android.intent.category.LAUNCHER 1`).
- **Tablet** (on B4's build; the old build's behaviour is above, no need to reproduce it): from
  a force-stop, open with `am start -n io.github.buerlino.apodroid/.MainActivity`, make a draft
  change (Size → Fit) and pick a "my picture" into the draft, then start again with the
  launcher intent (`monkey … 1`): one `MainActivity` in `dumpsys activity activities`, Fit
  still chosen, `fallback-draft.jpg` still there (`run-as ls files`). The reverse order the
  same. The picture → the browser → back by Recents and by the launcher: the draft kept. A
  swipe from Recents still drops it.
- **Done when:** the checks pass, the tablet passes (each start named with its result).

### [x] B5. A failed run keeps its retry (Opus, medium)

The user's answer to pass 6's question (2026-10-20 night): S4f3's night default, a reschedule
noted during a failed run replacing its 30 s backoff retry, is fixed, not kept.
- Now: `DailyJob.rescheduleNoted` runs right after `jobFinished(params, true)`; `schedule`
  (and `ensure` with another timing) replaces the job and with it the retry, so with turns the
  retry waits up to a period.
- **Should:** after a failed run, a noted reschedule stays noted and is done at the end of the
  next run that doesn't fail (the retry, usually within a minute). Off still cancels and drops
  it. The process dying in between loses it, as now (`onResume`'s `ensure` covers a changed
  timing). `onResume`'s own `ensure` while no run goes stays as now. **A `schedule` or
  `ensure` done directly, with no run going, drops a kept note** (from a review, 2026-10-20
  night): else a Set made later would be followed, after the next run that doesn't fail, by
  the old note, restarting the job's period at a random time. A test or a log line shows it
  if the code allows; else say how it was checked.
- **Files:** `DailyJob.kt`, `CLAUDE.md` (Background update: drop B5's "not built yet").
- **Tablet:** a run that fails (the proxy in `private/wp7-handoff/proxy/` failing one source)
  with "More NASA pictures" on and turns, Set during it: `logcat` shows no "rescheduled after
  the run" after the failed run, the retry runs (`dumpsys jobscheduler`: the backoff), and the
  reschedule comes after the retry's end once the proxy lets it through. Then: a failed run
  with a note kept, a Set with no run going (schedules at once), the next good run: no
  "rescheduled after the run" line. If the proxy can't be set up, say so under Not tested.
- **Done when:** the checks pass, the tablet passes, `CLAUDE.md` says it.

### [x] B6. Quick taps on the daily switch set once (Opus, medium)

The user's answer to pass 6's question (2026-10-20 night): rosie on S4f2 switched on, off, on
within about 0.3 s and got two switch-on sets of the same picture, 0.6 s apart; fix it.
- Now: `switchDaily(true)` while the first switch-on's run (`switchOn`) still goes queues
  another run (`switchAgain`, `runNext`), which starts the stored setup afresh again
  (`startAfresh` forgets the records) and sets the same picture again.
- **Should:** on again while a switch-on run goes adds no second run when that run will set
  (it got past its `store.daily` check); only when it gave up (the switch was off when it
  started) does the last on run once more. Off is still off. One set and one toast in all.
  The edge (from a review, 2026-10-20 night): the first run's `store.daily` check falling
  inside the short off must not end the taps with the switch on and nothing set (with turns
  nothing would then be set until the next period). Opus, as S4f2 needed a fix round in the
  same code.
- **Files:** `MainActivity.kt` (`switchDaily`, `switchOn`), `CLAUDE.md` (Background update:
  drop B6's "not built yet").
- **Tablet:** on, off, on in one `adb shell` command (three `input tap`s): `logcat` shows one
  "Switch on:" set, `wallpaperSetAt` written once; on, off with a pause: off cancels the job;
  a slow on, off, on (a second apart) still ends on with the job scheduled and the wallpaper
  set; several fast triples (on, off, on with 50–300 ms between taps): every one ends on,
  set once, the job scheduled.
- **Done when:** the checks pass, the tablet passes.

### [x] S6. The picture side: the title dropdown, swipe between sources, no Share (Opus, high)

After S5 (both change `MainActivity.kt`), and after B2 and B4–B6 in the queue (B4 and B6 change it too).
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

### [x] B7. The job keeps its retry: a stop during the saves, a Set after a failed run (Opus, medium)

The user's answers to the night of 2026-10-20/21 (pass 8's QUESTION 2, regression 4): two cases
B5 left open, both fixed, not accepted; and a limit (user, 2026-10-21, after a review of this
spec): a note waits for **one retry at most**. After B10 (both change `DailyJob.kt`).
- **A stop during the saves.** Now: `cmd jobscheduler timeout` (or Android's deadline) while
  `saveInUse` runs, after a good update: `onStopJob` returns `true` (the system's retry), but
  the worker then calls `rescheduleNoted(failed = false)`, so a reschedule noted during the run
  (a Set with turns) is done at once and replaces that retry; the saves wait up to a period.
  **Should:** a run that `onStopJob` stopped counts as failed for `rescheduleNoted` (a
  `@Volatile` flag set in `onStopJob`, or the like), wherever the stop came.
- **A Set just after a failed run.** Now (regression 4): the run has ended, so `runs.note`
  sees no run going and `schedule` replaces the job at once, its backoff retry with it; with
  turns the period restarts, so the failing source waits up to a period (Set's own update is
  one try, but a Set that fails too has no retry). **Should:** after a failed run, until the
  next run starts, a retry is due (`JobRuns` keeps it: `finished(failed = true)` sets it,
  `started()` clears it), and `note` treats it as a run going: the reschedule is noted
  (afresh wins). The process dying loses it, as a note now (then `schedule` acts at once).
  `onResume`'s `ensure` with the same timing changes nothing, as now.
- **One retry at most.** A note kept by a failed run is done at the end of the next run (the
  retry), good or failed; it never waits for a second retry. Why: a run fails when any source
  fails, and a source can fail every run for days (a NASA picture that isn't one, all
  weekend), which would hold back a Set's fresh period or a new "Change every" that long. The
  cost: a second failure's backoff is replaced by the fresh schedule; the next period retries.
- **Off leaves no retry due** (from the review): the daily switch off during a run calls
  `cancel` → `drop()`, and the stopped run then ends as failed (`onStopJob`'s flag) after
  `drop()`. It must not set "retry due": a cancelled job has no retry, so every later
  `schedule`/`ensure` (the switch on again, B6's quick taps, `onResume`) would only be noted
  and the switch would stay on with no job until the process dies. So a run going when
  `drop()` comes ends without a retry due and without a note, also when a new run started
  after the off (the switch on again) is still going: e.g. a generation that `drop()` bumps and
  each run takes at `started()`, a run of an older generation doing nothing at its end. Night
  default with the why if another fits better.
- **Tests** (`JobRunsTest`): **replace** `aDirectRescheduleDropsAKeptNote` (it asserts that a
  reschedule right after a failed run is done at once, which B7 changes: now noted, done at
  the retry's end) and `aFailedRunKeepsTheNoteForTheRetry` (it lets the note wait through two
  failed runs: now done at the first retry's end, failed or not); say so in the report, as the
  audit will see them rewritten. New: a failed run's end, then a note: noted, done at the next
  run's end (good, and failed); off during a run, the run ends as failed, then `note` returns
  `false` (schedules at once); off, on and a new run started, then the old run ends failed: no
  retry due, the new run's end behaves as normal. Each new case fails on the old code.
- **Files:** `core/.../JobRuns.kt`, `JobRunsTest`, `DailyJob.kt` (the stop flag; the KDoc of
  the class, `schedule`, `rescheduleNoted`), `CLAUDE.md` (Background update: B5's "drops a kept
  note" sentence and "B7 changes this" become the rule as built), the skill's "Where things
  are" (`DailyJob`, `JobRuns`).
- **Tablet:** "More NASA pictures" on, turns on one screen, daily on. Wi-Fi off, `run -f`: the
  run fails (`Daily job failed`), `dumpsys jobscheduler` shows the backoff; a Set on the page
  (fails offline too) logs no reschedule and the job keeps its backoff; Wi-Fi on: the retry
  runs, then "rescheduled after the run (schedule)". One retry at most: Wi-Fi off, `run -f`
  (fails), a Set, `run -f` again still offline (the retry, fails): "rescheduled after the run"
  at its end. Off: `run -f` with a slow save (below), the daily switch off during it, then on:
  `dumpsys jobscheduler` lists the job at once. The stop: "Save every picture" on, "Largest
  original" "No limit", a large original faked by prefs (V8's 27 MB one), `run -f`, a Set
  while the saves run, `cmd jobscheduler timeout` a few seconds in: no "rescheduled after the
  run", the retry comes (`Last successful run`), then the reschedule. The tablet's VPN makes
  the test proxy and the firewall miss the app (regression 4): use Wi-Fi off, not the proxy.
- **Done when:** the tests (each new one failing on the old code), the fix, the tablet passes,
  `CLAUDE.md` says it.

### [x] B8. The draft's picture survives a second page (Opus, medium)

B4's edge (pass 8, 2026-10-21; the user: close it). With another app's screen on top of the
page (the photo picker opened by "Change picture", App info), a start with another intent (the
launcher's after `am start -n`, or the reverse) still puts a second `MainActivity` on top
(`singleTop` only reuses a page that is itself on top). The new page has no saved draft, so its
`onCreate` deletes `files/fallback-draft.jpg`, which the page below still uses.
- **Should:** a page's start deletes the draft's picture only when no other page is alive (a
  process-wide count of live pages, `onCreate` to `onDestroy`, or the like). The second page
  still opens without the draft (it has none); `singleTask` stays out (it would end a pick).
  A pick made in the second page while the first lives overwrites the one file: say what
  happens then (night default), don't build more for it.
- **Files:** `MainActivity.kt`, `CLAUDE.md` (The app, Settings, "A draft until Set": drop B8's
  "not built yet").
- **Tablet:** from a force-stop, `am start -n …/.MainActivity`, a "my picture" into the draft
  (a faked APOD video day), "Change picture" (the picker open), then the launcher intent
  (`monkey … 1`): two pages (as before), `fallback-draft.jpg` still there (`run-as ls -l
  files`, same size); Back to the first page: the draft and its picture, Set renames it to
  `fallback.jpg`. A start with no other page (a swipe from Recents, then the launcher) still
  deletes a leftover draft file. Recents: swipe away only APODroid's task.
- **Done when:** the checks pass, the tablet passes (each start named with its result).

### [x] B9. A no-break space before ▾ and ▸ (Sonnet, medium)

V3 (2026-10-21, font scale 2.0 at `wm density 344`): the video dropdown's "▾" wrapped alone
onto a second line under "NASA Image of the Day" (`.coord/runs/029-v3/tablet/v3-13-…-08`).
- **Should:** `"\u00A0▾"` (and `▸`) instead of `" ▾"` in the five places that add the glyph in
  `MainActivity.kt` (the title, the headers, the list's dropdowns, the video dropdown, "Largest
  original"), so the glyph stays with the last word. No text changes; screen-reader labels as
  they are (they don't read the glyph).
- **Files:** `MainActivity.kt`, `CLAUDE.md` (Settings, If APOD is a video: drop B9's "not built
  yet").
- **Tablet:** font scale 2.0 at `wm density 344`, "If APOD is a video" on NASA's and on Earth's
  choice, Setup and Advanced open and closed, the list: no "▾" or "▸" alone on a line
  (screenshots for the user); font scale back, `wm density reset`.
- **Done when:** the checks pass, the tablet passes, the screenshots are there for the user.

### [x] B10. Remove "Wi-Fi only" (Sonnet, medium)

The user, 2026-10-21 (asked how long a reschedule may wait while a source fails): "Wi-Fi only"
(F2) goes; whoever wants the picture wants it at once, not after days of waiting for Wi-Fi.
Never released (0.6.0 only), so no migration and no changelog line.
- **Should:** the job always requires `NETWORK_TYPE_ANY`. Gone: the switch row in "Advanced
  settings" (`wifiOnly`, `switchWifiOnly`, its row and KDoc mentions in `MainActivity.kt`),
  `Store.wifiOnly`, the `setRequiredNetworkType` choice in `DailyJob.job`, and `ensure`'s
  `pending.networkType == job.networkType` (with it the deprecated-API warning; no released
  build had another network type). A `wifiOnly` key left in a dev build's prefs is ignored,
  not removed. "Advanced settings" while the daily switch is on: "Save every picture" (with
  "Largest original" while on), then "More NASA pictures"; its order otherwise unchanged.
- **Files:** `MainActivity.kt`, `Store.kt`, `DailyJob.kt`, `CLAUDE.md` (every "Wi-Fi only":
  Settings, the highlight's "not" list, Background update's F2 paragraph and "being removed",
  "Save every picture"'s "under "Wi-Fi only"", Next features, Open question 1's "and the Wi-Fi
  only switch (F2)"; Next features keeps one line that it was built and removed, with the
  why), `features.md` (item 4: removed, the why), the skill's "Where things are" (`ensure`).
- **Tablet:** first, once, the tidying the user asked for after the night of 2026-10-20/21
  (before noting the tablet's state): delete the old test pictures in `Pictures/APODroid/`
  (`APOD_…`, `NASA_…`, `EO_…`, with their "(1)"/"(2)" copies; `content query --uri
  content://media/external/images/media` to find them, `content delete` each), then, after
  installing, one Set so the wallpaper matches the records. The firewall chain (chain3) stays
  off (Android's default); note that it is. Then: "Advanced settings" open with the daily
  switch on and off: no "Wi-Fi only" (a screenshot); after a Set `dumpsys jobscheduler` shows
  the job without an unmetered constraint; the smoke list.
- **Done when:** the checks pass (no new warning), the tablet passes, `CLAUDE.md` has no "Wi-Fi
  only" left but the removal line.

### [x] B11. A Set from a second page keeps the stored "my picture" (Opus, medium)

Pass 11's QUESTION (2026-10-21 night; the user, 2026-10-22: fix it). B8's night default: with
two pages alive (the photo picker or App info on top of the first, then a start with another
intent), a Set in the second page, which has no pick of its own, renames the first page's
`fallback-draft.jpg` to `fallback.jpg` (`Store.saveSetup`, which renames whenever the file
exists), so the stored "my picture" is replaced without the user choosing it.
- **Should:** `saveSetup` renames the draft's picture only when the Set's own draft holds a
  pick (`draftFallback`, passed in or part of the saved `Setup`; night default with the why);
  otherwise `fallback.jpg` stays and the draft file stays for the page that owns it. A Set in
  the first page, with its pick, renames it as now. A pick in the second page still replaces
  the one file (B8, as built).
- **Files:** `Store.kt` (`saveSetup`), `MainActivity.kt` (`setAsWallpaper`), `CLAUDE.md` (The
  app, Settings: B11's "not built yet" becomes the rule as built).
- **Tablet:** as B8's check: `am start -n …/.MainActivity`, a faked APOD video day, "Use my
  picture" with a stored `fallback.jpg` (note its size), a new pick into the draft, "Change
  picture" (the picker open), the launcher intent (`monkey … 1`): two pages. A Set in the second
  page (it has none of its own): `fallback.jpg` unchanged (same size), `fallback-draft.jpg`
  still there; Back to the first page, Set: renamed (`fallback.jpg` the pick's size, the draft
  file gone). Recents: swipe away only APODroid's task.
- **Done when:** the checks pass, the tablet passes (each step named with its result),
  `CLAUDE.md` says it.

### [x] B12. Only the first retry is waited for (Opus, medium)

From a review of B7's code (2026-10-22 morning; the user: fix it). `JobRuns.finished` keeps a
retry due after a failed retry with nothing noted (`retryDue = failed && wanted == null`, B7's
night default). So while a source keeps failing (a NASA picture that isn't one, all weekend),
every Set's or `onResume`'s reschedule is only noted and waits for the next retry, which
Android delays twice as long after each failure, up to 5 h: not the minute B7's rule assumed.
- **Should:** only the first retry of a failing series is waited for. After a retry's end
  (good or failed) no retry is due; a later `schedule` or `ensure` is done at once (it replaces
  the job's own backoff with a fresh schedule; the next period retries, and Set's own update is
  a try). A failed run after a good one starts a new series, as now.
- **Tests** (`JobRunsTest`): **replace** `aFailedRetryWithoutANoteLeavesItsRetryDue` (it
  asserts the old rule; say so in the report): after a failed run and a failed retry with
  nothing noted, `note` returns `false`. Keep the others; a failed run after that again makes a
  note wait for its retry.
- **Files:** `core/.../JobRuns.kt` (the line and the KDoc), `JobRunsTest`, `DailyJob.kt` (the
  KDoc if it says otherwise), `CLAUDE.md` (Background update: B12's "not built yet").
- **No tablet check** (a `:core` rule; B7's tablet check covers the code path).
- **Done when:** the replaced test fails on the old code, the fix, `CLAUDE.md` says it.

### [ ] V. Tablet pass of everything (Opus, high)

After F5, F1f1, F5f1 and C (V3, V5 and V6 also after S1, S2 and S4–S6), so it tests the final code (third review: F1 changes the set path,
F5 the job). **By night** (user, 2026-10-20), and a device package gets one rosie session of at
most 50 minutes (`night.md`, For rosie; `pipeline.sh` gives it 3600 s), so V is queue
rows, V1–V6 and V8, each bundled as `plan V + plan V<k>`, and V7 by hand on a night of its own (turns over
hours: the night's later rows and the regression reinstall and change the tablet). **The
complete test before the stable release** (user, 2026-10-20 night): V3, V5, V6 and V8 on the
final tree (after S6 and B2–B6), the night's regression after them, then V7 and P. V1, V2 and
V4 ran on trees before S4–S6; V8 repeats what those changed. Every row:
- The final tree: master with everything above committed (`night.sh` refuses an uncommitted
  plan and brings `dev` up to master). Build what the row needs in your worktree:
  `./gradlew :app:assembleDebug`, and for R8 `:app:assembleRelease` signed with the debug key
  (the skill, Working on the phone).
- Rosie's device rules (`night.md`, For rosie): note each setting and the app's prefs and files
  first, set them back at the end. Text evidence first (`dumpsys`, `run-as cat`, `logcat`);
  screenshots for the user into your folder, named for what they show. The how-tos: the skill,
  Working on the phone (a change time passed: its "End a turn"); `private/wp7-handoff/`
  (`swap.sh`, `ui.sh`, `proxy/`; `turn.sh` does nothing since J1).
- Your verdict: PASS when everything you ran works; FIX for a bug in our app (the steps, what
  happened, the logcat line); STOP for Android doing what the plan didn't expect. The morning
  makes each bug a `B<n>` package.
- Results: in your results commit, the skill's "Still untested" loses the gaps this row closed
  and gets one line, naming the row, for each it leaves open (gaps only since 2026-10-26: what
  it checked goes in the report commit); then the report commit ("Your report": Tests, what
  ran on the tablet; Not tested, what didn't and why). The plan's checkboxes stay `[ ]`.
- Done when: each of the row's bullets ran or is under Not tested with the reason, and the
  results are committed.
- **Time box** (2026-10-21 night, from a review: a session is killed at 3600 s with nothing
  saved, no reply and no commit): run `date` at the start and before each bullet. After each
  bullet commit its notes on your branch, `git commit --allow-empty -m '<row> progress:
  <bullet>' -m '<its BUG: lines>'` (B13 reads them there). At 40 minutes start no new bullet:
  set the tablet back, then the results commit and the report commit; the rest goes under Not
  tested.

### [x] V1. F1, F3 and F1f1 on the tablet

- F1's device bullet (Fit with a wide and a tall picture, home and lock, both modes; a change
  with the daily switch on sets at once and `turnStartedAt` stays; no ⛶ with Fit).
- F1f1: a star field with Fit (APOD's stored picture if it is one, else a made-up one in
  `files/`): a screenshot of the home screen and one of Fill for comparison, for the user; a
  made-up JPEG with EXIF orientation 6 as "my picture" with Fit comes out upright, not
  stretched.
- F3's device bullet (the share sheet with "<title>\n<page link>" on a picture and on the ▶
  tile, from APOD's and NASA's tabs), with screenshots of ⤴ beside ★ for the user (only ⤴: C
  compared the others).

### [x] V2. F4, F5 and F5f1 on the tablet

- F4's device bullet (a short credit and the longest, 251 characters by prefs; APOD mode and
  with tabs; the explanation open and closed), screenshots for the user.
- F5's device bullet, with F5f1: after a stop during a save (`cmd jobscheduler timeout`) no file
  is left, `<source>.saveTried` holds the URL, and the next run saves the stored picture (the
  smaller copy), not the original again; a post with another picture URL tries its original.
  The ⓘ dialog's screenshot. Delete the test files from the gallery afterwards.

### [x] V3. M2–M4 again on the final tree

- Set (S4) in both modes, with the daily switch on and off: a draft sets nothing, Set puts on
  each screen what the setup says and starts a fresh turn; then the job's next run sets what
  the settings say, and nothing is held.
- The daily switch at once (S4f2): off cancels the job with a draft change pending (the draft
  stays), on sets the stored setup, not the draft.
- The title dropdown and the swipe (S6) with each source's own post, in both modes.
- A faked APOD video day with each of the four choices ("Skip it", NASA, Earth, my picture) in
  both modes, with turns on one screen: the stand-in in APOD's place, "Skip it" passing the turn.
- A set that fails on one screen and its retry setting only that screen: if nothing on the
  device makes one set fail, say so under Not tested (`ScreensTest` covers the rule).
- Rotation and two panes; font scale 2.0 at `wm density 344` (the phone's width): the list and
  the status lines with the short names, the Fit row, the credit.
- From the night of 2026-10-20 (its report): B1's "kept" line after the daily switch goes on
  (S4f2 forgets the records as Set does: a faked video day, APOD alone, "Skip it", switch off
  and on), and with turns on one screen (no "kept" line there); a draft change made while a
  Set runs (Setup closes after
  the Set: the change still highlighted, the header too); pull to refresh with a faked old
  date fetches again (`logcat`).

### [x] V4. What the night of 2026-10-20 didn't check

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

### [x] V5. The update from 0.5.0, debug builds

- A debug build of 0.5.0 (`git worktree add <your folder>/v050 v0.5.0`, `:app:assembleDebug`
  there) under a debug build of 0.6.0, so `run-as` reads the prefs before and after (a release
  build can't be read). For each `where` (Home, Lock, Both) with APOD alone, NASA alone and two
  sources: the prefs before and after against `CLAUDE.md`'s migration (Stack, Migrations; Next
  features, Migration), the first run sets nothing, the page opens on the right mode and tab.
  Nine states; a script helps. Those not reached go under Not tested. Remove the worktree after.

### [x] V6. R8 builds

- Once: an R8 build of 0.5.0 under an R8 build of 0.6.0 (both signed with the debug key),
  checked on screen only: the settings kept, the page and the job work.
- On the R8 build of 0.6.0: the smoke list (`night.md`), then both modes, Fit set, the credit
  shown, a save by the job (one over "Largest original" saving the smaller copy, F6), ★ by
  hand on an original over the limit (10 MB and a 20–50 MB original faked by prefs, never
  today's 208 MB one: ★ saves the original anyway; the file deleted after), the swipe between
  sources (S6); no crash (`logcat`, `exit-info`). R8 isn't covered by the unit tests.

### [x] V7. Turns over hours (a night of its own, after V12)

**Moved after J1–J3** (user, 2026-10-22 evening): it tests the new job. Its setup then also
takes the picture time and the three rules (`CLAUDE.md`, Next features): Home APOD alone,
Lock NASA + Earth every 3 h, "New picture at" 06:00 (change times 06, 09, …, 00, 03). On
battery (below). **The night of Monday 26 to Tuesday 27 Oct** (user, 2026-10-25: moved a night for
B19–B21 and V12; was Sunday 25 to Monday 26, user, 2026-10-23: a weekday morning brings new NASA
and Earth posts; the DST night is faked in J2's check instead). Session 1 still writes the
night's own expectations from the stored posts.
Expect, Zurich time (EU on winter time, the US not until 1 Nov: APOD at 05:05, Earth at 05:00):
- Home: Tuesday's APOD on at 06:00 plus the offset and the alarm's window (about 06:00–06:20).
  The Zurich case (yesterday's still on at 06:00) can't happen before 1 Nov; J2's faked clock
  covers it, and W's Mon 2 Nov morning on the FP6 on a real clock.
- Lock: at Set its source stays (or Monday's other picture); at 00:00 Monday's picture not yet
  shown goes on (rule 1), if any; then nothing new, so it waits and nothing is set at 03:00;
  Tuesday's Earth goes on at the first check after 05:00 (rule 3, a waiting screen's hourly
  checks); the 06:00 and 09:00 change times find nothing new (NASA posts in the afternoon), so
  nothing is set.
- Session 1 writes the expected sets with their times into `start.md` from the stored posts.

(User, 2026-10-21: after B7–B12 are merged, a night with nothing else queued; session 1 at
bedtime, session 2 in the morning before pascaline starts.) Two short rosie sessions by hand
(`ANDROID_SERIAL=HA24BZ2Y claude --agent rosie`, this section: the FP6 may be attached too), at
least 6 hours apart, the tablet left alone between them. Real timing, so no `turn.sh` and no
`run -f` (each pushes the job's window a period on). The folder for both: `.coord/runs/v7/`.
- **Session 1 (setup, about 20 min; "Do V7"):** note the tablet's state as "For rosie" says
  (settings, the app's prefs and files) into `.coord/runs/v7/backup/`. Install the debug build of
  master. "More NASA pictures" on, Home APOD alone, Lock NASA + Earth, "Change every" 3 h,
  "New picture at" 06:00, daily on, Set.
  Write `.coord/runs/v7/start.md`: the time, both records (`run-as cat` the prefs), the app's
  alarms (`dumpsys alarm`), `dumpsys wallpaper`'s ids, the job's period, flex and next window
  (`dumpsys jobscheduler`), the battery level. **Don't set the tablet back**: the setup must run.
  **On battery** (user, 2026-10-22: a phone is mostly not on power; the plan had it charging):
  the user unplugs it after the session (Wi-Fi on, the screen locked, the app in the background,
  Home). End with
  what session 2 should expect (each boundary's set or keep, with its time, as above). Also note any
  `BOOT_COMPLETED` in `logcat -s APODroid` without a reboot (J2's tablet check saw one after
  `am kill`; B16 thinks it's Android 15 ending the app's stopped state: say what came before it).
- **Session 2 (read-out, about 20 min; "Do V7's second session"):** read `start.md`, then
  `dumpsys jobscheduler` (`Last successful run`, the next window), `logcat -d -s APODroid` (it
  may have rolled over: the prefs are the evidence), `dumpsys wallpaper`, the prefs, the
  battery level, `dumpsys deviceidle` (the Doze state now) and the job's `Constraint history`.
  Check: each turn came within its window (period and flex; on battery Doze may push it to a
  maintenance window: say by how much, and whether the app was `EXEMPTED` or in another
  standby bucket), the alarms' times (`dumpsys alarm`) against the runs, each change on its
  change time or at the check that saw a new post, no screen set twice for one change time, no
  set of a picture the screen had shown, each change to the next source with a new picture. Results and verdict
  (PASS, FIX, STOP) in `.coord/runs/v7/report.md` (Tests, Not tested); the morning updates the
  skill's "Still untested" (V7's line out, a line per gap left). **Then** set the tablet back
  from `backup/`.

### [x] V8. V1, V2 and V4 again where S4–S6 changed them

- Fit (V1, F1): set by Set now (S4), not at once: Fit with a wide and a tall picture, home and
  lock, both modes; no ⛶ with Fit; a made-up JPEG with EXIF orientation 6 as "my picture" with
  Fit comes out upright. No ⤴ (S6 removed Share).
- The credit (V2, F4) under the title with S6's title and swipe: a short one and the longest
  by prefs, APOD mode and "More NASA pictures", the explanation open and closed.
- The job's saves (V2, F5, F5f1, F6): "Save every picture" on, a forced run saves each source
  in use; a stop during a save (`cmd jobscheduler timeout`) leaves no file and `saveTried`, the
  next run saves the smaller copy; with the limit at 10 MB a 20–50 MB original (faked by prefs)
  saves the smaller copy, with "No limit" the original. The ⓘ dialog's new text (B3). The
  test files deleted from the gallery after.
- The photo picker in the draft (V4): "Use my picture" with none opens the picker; backing out
  keeps the choice before; a picked picture is a draft (`fallback-draft.jpg`) until Set
  renames it; "Change picture"; a rotation while the picker is open; closing the app drops the
  draft file.
- V4's other bullets on the final tree: the problem box with "More NASA pictures" on when only
  NASA fails (the proxy), on APOD's page and on NASA's (S6's title and swipe); the chip in APOD
  mode with "Where" = Lock; the open page following a job run that changes only the lock
  screen.

### [x] V9. Brute test 1: what the system does to the app

(User, 2026-10-21 evening: "a complete check … bugs that might occur when doing weird stuff or
edge cases or even just normal operation"; a stable 0.6.0. None of this was checked on any
device before.) The final tree (master with B7–B12). **Hunt, don't confirm:** go through the
whole list even after a bug; each bug a `BUG:` line as the regression's (steps, expected, got,
evidence); FIX when there is one. For each item note what the page, the prefs, `dumpsys
wallpaper` and `dumpsys jobscheduler` say after it, and look for a crash (`exit-info`, `logcat`).
"More NASA pictures" on with turns on one screen unless an item says otherwise.
- **Reboot** (`adb reboot`, then wait for `sys.boot_completed`): the daily switch on, with and
  without turns: the job is still listed (`setPersisted`) with the same timing, runs at its
  window, the page opens on the stored setup; the draft dropped. After the reboot the tablet
  may ask for a PIN: if so, NOT RUN for the rest and say so.
- **Airplane mode during a download** (`cmd connectivity airplane-mode enable`/`disable`): a
  pull with NASA's stored date faked a day back (its rendition is about 0.8 MB), airplane mode
  on within a second: no `.part` left in `files/`, the old picture kept, the problem box; off
  again and a pull: the new picture. The same during a Set and during `run -f`: the job's
  retry (B7) comes after airplane mode goes off.
- **The clock and the time zone**: `cmd alarm set-timezone` to another zone (e.g.
  `Pacific/Auckland`, then `America/New_York`) with the page open and after a `run -f`: the
  status line's times, "today" (US Eastern) unchanged, nothing set twice. The clock set back and
  forward a few hours if the shell may (`cmd alarm set-time`; else Not tested): a turn's clock
  in the future counts as over (WP7g), nothing stuck. Set it all back (auto time and zone on).
- **Battery saver and Doze**: `cmd power set-mode 1` (saver) and `dumpsys deviceidle
  force-idle`, then `dumpsys deviceidle step` through to a maintenance window: the job waits and
  runs then (not with `run -f`); the page and Set still work with saver on. Undo both.
- **Split screen and resize** (the tablet's multi-window, `am start --windowingMode 6` or the
  Recents menu): the page below 600 dp in one pane, then two panes again; a draft, crop mode
  and an open dropdown across the resize; Set from split screen.
- **Theme and fonts while open**: dark/light (`cmd uimode night yes|no`), font scale changed
  with the page open: recreated, the draft, Setup's state and the shown source kept.
- **Process death in the middle**: `am kill` (press Home first) during a Set, during a pull
  and during a ★ save; `am force-stop` during a forced run: after reopening the prefs agree
  with `dumpsys wallpaper` (or the next Set puts them right, say which), no `.part` or draft
  file left that shouldn't be, the job scheduled again (pref `daily`), ★ not stuck dimmed.
- **An app update while the job runs**: `run -f`, then `install -r` the same APK during it:
  the next start fine, the job listed, no half-written records.
- **Low storage** (the skill's full-disk recipe, about 200 KB left): a Set, a pull, ★, "Save
  every picture": the right message, nothing half-written; free the space after.
- **Done when:** each bullet ran or is under Not tested with the reason, the tablet set back,
  and every `BUG:` line of your reply also in your report commit's message (B13 reads them there).

### [x] V10. Brute test 2: what a user does to the app

As V9 (the final tree; hunt, don't confirm; every bug a `BUG:` line).
- **Mashing:** Set tapped three times fast; Set and the daily switch and a pull together
  (`input tap`s in one shell line); the daily switch on/off five times fast; ★ twice fast, ★
  then a swipe, ★ then rotate; swipes back and forth during a load; the title dropdown during a
  swipe; ⛶ then a swipe (the pager must not move), ⛶ then rotate, ⛶ then Set.
- **Rotation during every async action:** Set, pull, the daily switch's run, a ★ save, the
  photo picker open, a dialog open (weekday, ⓘ, "Don't show again"'s confirmation).
- **Every combination once, quickly:** APOD mode "Where" × Size × the four video choices on a
  faked video day, each with Set (`dumpsys wallpaper`); the list: each source on Home, Lock,
  Both, unticked, down to the last tick; the mode off and on between Sets; "Change every" each
  value with Set (the job's period, `dumpsys jobscheduler`).
- **The picker's pictures:** a huge one (a 50 MP JPEG or a large PNG made with ImageMagick and
  pushed to the gallery), a tiny one (16×16), a panorama (10:1), a tall one (1:5), a WebP, a
  GIF, one with EXIF orientation 8; each with Fill and Fit, on both screens: set, upright, no
  crash, no out-of-memory; cancel and Back in the picker.
- **Every button nobody pressed lately:** the video notice's and the battery hint's "Don't
  show again" (with its dialog) and "App info", the weekday dialog's both buttons, the problem
  box's Dismiss across a new day (faked), "Try again" in the empty state (`pm clear`, offline,
  then online), the long presses (the clipboard), Back in every state (crop mode, a dropdown, a
  dialog, the picker).
- **A fresh install** (`pm clear`, then open): APOD mode, the defaults, the first Set, the
  first daily switch on; then the same with "More NASA pictures" turned on first.
- **Long use:** 50 swipes and 20 Sets in a row, then `dumpsys meminfo` (no climb), `logcat` (no
  `OutOfMemory`, no ANR: `dumpsys activity processes | grep -i anr`, `/data/anr` if readable).
- **Done when:** each bullet ran or is under Not tested with the reason, the tablet set back,
  the test files deleted from the gallery, and every `BUG:` line of your reply also in your
  report commit's message (B13 reads them there).

### [x] CR. Code review of the whole app (Opus, high)

(User, 2026-10-21 evening: find the bugs still in the code, for a stable 0.6.0.) By reading,
no device, no code changes. Not a diff: every file in `core/src/main` and `app/src/main`
(Kotlin, the manifest, the resources), against `CLAUDE.md`. Look for what the packages' audits
miss because each saw one diff:
- races between the page, the job and the prefs listener (two `Store`s, the locks, `commit` vs
  `apply`, a read between two writes, main-thread work);
- the lifecycle: a kill or a rotation between two steps (`saveSetup` then `update`, a rename then
  the prefs, the draft's file), `onResume` while a Set or a save runs, a second page (B8);
- the prefs and the files disagreeing (a record without its file, `fallback.jpg` missing, a
  `.part` left, `badUrl`, `saveTried`), and what the next run does then;
- failure paths: each `catch`, each `runCatching` that swallows a stop or an error, a toast or a
  log that says the wrong thing, a retry that never comes or never ends;
- time: US Eastern dates, the turn clock, a time zone or clock change, 12/24 h;
- memory and big inputs: decoding (the picker's huge or odd pictures, a 97 MB PNG), bitmaps
  held, the three pictures of the pager;
- `CLAUDE.md` contradicted by the code, or a rule written there that no code enforces.
Write `.claude/skills/build-apodroid-android/review-0.6.0.md`: one numbered finding each,
worst first: severity (crash or data loss / wrong wallpaper or wrong state / wrong text / tidy),
`file:line`, the failure (concrete steps or inputs → what goes wrong), the fix in one or two
lines, and whether it is **a bug** (the app contradicts `CLAUDE.md`, crashes, loses data or
gets stuck) or **a decision** (it changes what `CLAUDE.md` decided, a text or the UI). Only
what you verified in the code; say how sure. Commit it (and the report commit). Don't pad:
an empty section is fine.
- **The audit** checks the form (every file listed; each finding with its severity, file:line,
  steps, fix, and bug or decision) and that the cited lines exist. A finding it thinks wrong
  is not a FIX: B14 checks each finding again before building it.
- **Done when:** every file read (list them in the report), the findings committed.

### [x] B13. The brute test's bugs (Opus, high)

The `BUG:` lines of V9 and V10, in their report commits' messages (merged on `dev`, or on the
kept branches `wp/v9`, `wp/v10` when a FIX left them unmerged; a session cut off at its
time limit leaves only its `progress:` commits, which carry them too): `git log --all --grep=V9
--grep=V10 --format='%h %s%n%b' | grep -B2 -A1 'BUG:'` from your worktree. Their results commits
(the skill's "Still untested") say what was checked. For each:
- **a bug** (the app contradicts `CLAUDE.md`, crashes, loses data, sets a wrong wallpaper or
  gets stuck) with a clear fix: fix it, with a test that fails on the old code (`:core`) or a
  `Device (tablet …)` bullet in your report that shows it; `CLAUDE.md` updated where it says
  what happens;
- **a decision** (a text, the UI, a rule `CLAUDE.md` set) or a fix you aren't sure of: not
  built; a `QUESTION:` line in your report with your recommendation;
- Android doing something we can't change: say so, not built.
At most six fixes; the rest listed for the morning. None found: say so and commit the report only.
- **Files:** as each fix needs; `CLAUDE.md`; the skill's "Where things are" if a function moved.
- **Tablet:** each fixed bug's steps again (from rosie's `BUG:` line), then the smoke list.
- **Done when:** each `BUG:` line fixed, a QUESTION, or named as Android's; the checks pass; the
  tablet passes.

### [x] B14. The code review's bugs (Opus, high)

**Status (2026-10-22, by day, pipeline run 44):** merged on `dev` 1008276, master fast-forwarded;
129 `:core` tests (1 new), audit APPROVED, tablet PASS (CR 1, 2's rest, 3, 5, 7a, 7b; files in
`.coord/runs/044-b14/tablet-0/`). Not seen on the tablet: CR 1's toast text (the wallpaper and
the log show no set), the mixed status lines during a Set (rosie's polling began after it),
whether the load set before Set in CR 5 (the toast was right). The job's timing: a Set into
APOD mode 3 min after a run, the job already 6 h / flex 6 h, kept its earliest run (+5 h 57 min)
and didn't run at once; the timing was unchanged, so `ensure` most likely scheduled nothing:
it doesn't show what Android does with a changed timing (turns to none, V9's case): P.
Night defaults (accepted with the merge): Set's toast counts any run's sets since its commit
(`setsSinceSetup`), not a queue; the draft list follows the store only when it was the
store's derived one and unedited; a hidden newly picked "my picture" drops from the draft, its
file kept (B8); an owed follow is dropped when a load, Set or switch-on starts.

The findings of `review-0.6.0.md` (CR) marked **a bug**, worst first, as B13: fix with a test
that fails on the old code (or a Device bullet), at most six; a finding marked **a decision**,
or one you find isn't a bug after all (say why), not built: a `QUESTION:` line. After B13 (both
may touch the same files).
**The scope** (user, 2026-10-22 morning, after reading CR's findings; seven items, so B13's
"at most six" doesn't apply): findings **1, 3, 4, 5** and **2's rest** (a pick whose copy,
`saveFallback`, runs during a rotation: the choice and `draftFallback` kept), plus two
decisions:
- **Finding 7: a hidden draft value goes back to the stored one** (user: so Set never saves
  what the page doesn't show, and "Setup" highlights only what it shows). When "Change every"
  hides (no column with two or more, or the daily switch off), the draft's `interval` becomes
  `store.interval`; when "If APOD is a video" hides (the daily switch off), `videoDays` (and a
  newly picked "my picture" with it) becomes the stored choice. `CLAUDE.md`, Settings and the
  highlight.
- **The lock line during a Set** (V9, B13's QUESTION; user: fix it): `follow()` skips while
  `setting`, as it does while the switch-on runs, so the page redraws at Set's end only. With
  finding 4 (an owed follow run at the active one's end) it is one change to `follow()`.
- **Files, Tablet, Done when:** as B13 (the tablet checks the fixed findings' failures), and one
  more tablet bullet (user, 2026-10-22; V9 and B13's QUESTION): a Set into APOD mode (one
  source, no turns) with the daily switch on, once a few minutes after a job run and once with
  no run that day if the tablet allows (else say so): `dumpsys jobscheduler` for the app's job
  (earliest run, period, flex, `Last successful run`) right after the Set, and whether it ran
  at once. Only a report: whether Android keeps a periodic job's timing when the same id is
  scheduled again. No doc change from it before P repeats it on the FP6.

### [x] B15. The window by publish time, the 09:00 check, the stand-in shown twice, the video notice (Opus, high)

From a review of J1 on `dev` by reading and scratch tests (2026-10-24); the user's decisions are
in `CLAUDE.md` (Next features, 0.6.0: the picture time: the window by publish time; the video
notice under Video days on the page).

- **The publish time:** `Post` gets `published: Instant?`.
  - APOD and Earth: the WordPress `date` with its time (US Eastern local time, e.g.
    `2026-10-23T00:05:00`). NASA: the feed's `pubDate` (GMT).
  - Tests in `ApodTest` and `SourcesTest`, `readsEveryRealPost` included.
  - `Store` writes `<source>.published` with the post. A post stored before B15 has none and
    counts as published at its date's 00:00 US Eastern (no migration).
- **One source (review finding 2):** from each change time T (the last one ≤ now), the target
  is the newest stored picture published before T + 3 h. It's set whenever a run sees it and
  it's newer (by publish time) than what's on; a screen never goes back to an older picture.
  The decision is a function of the clock and the posts: a failed or offline check moves
  nothing.
- **A pull and Set:** they still put the newest on at once (also one published after T + 3 h).
  The next change time's target then decides, and it never goes back.
- **Several sources (finding 1):** rule 2's replacement takes a newer picture of the source on
  the screen when it was published before T + 3 h, whenever it's seen. So a failed set retried
  after 09:00 (finding 6) still happens. Rules 1 and 3 ("hasn't shown", waiting) as built.
- **The stand-in (finding 3):** APOD's turn with a stand-in counts as new only while the
  stand-in's picture hasn't been shown on that screen. Otherwise the turn passes on, as with
  "Skip it".
- **The window's end (finding 5):** `nextRun` also checks at T + 3 h (09:00), for half-hour
  zones (Tehran, APOD at 08:35).
- **The video notice in APOD mode** (`MainActivity.VideoNotice`): "… is the wallpaper." →
  "… takes its place."; "… becomes the wallpaper once it has loaded." → "… takes its place once
  it has loaded."; "your own picture is the wallpaper." → "your own picture takes its place."
- **Tests** (`PictureTimeTest`, `ScreensTest`):
  - Zurich, today's APOD first seen at 09:01: set.
  - Zurich, every check failing 06:00–09:00, then online at 10:00: set.
  - Zurich, a page opened offline at 07:00, then a run at 10:00: set.
  - Los Angeles, a pull at 21:30, then the next morning: no change back, and nothing at 06:00
    since the 21:05 picture is on.
  - A stored post without a publish time.
  - Tehran, 08:35: set at the 09:00 check.
  - The stand-in already shown on that screen.
  - A failed set retried after 09:00.
  - The review's uncovered cases:
    - picture time 02:00 on 25 Oct 2026 and on 28 Mar 2027, through `dueWallpapers` and
      `nextRun`;
    - US 1 Nov and Sydney 4 Oct, through the rules;
    - equal columns with the lock screen's set failing inside the window and retried after it;
    - a time zone change between runs.
- **Files:** `Post.kt` and its tests, `Screens.kt`, `PictureTime.kt`, `Store.kt`,
  `MainActivity.kt` (the texts only), `ScreensTest.kt`, `PictureTimeTest.kt`, `CLAUDE.md`
  (Turns and the picture time; Fetch: `published`).
- **Tablet** (rosie, `auto_time` off and back on as J2's check):
  - yesterday's APOD on, today's stored with a publish time of 06:05 (faked prefs), the clock
    faked to 09:30: set at the next check;
  - the same with the publish time 09:05: not set until the next day's change time;
  - the APOD-mode notice on a faked video day with "Use my picture" and with NASA;
  - the smoke list.
- **Done when:** the tests pass, the tablet passes.

### [x] B16. The job: a passed check kept on re-arming, one run at a time, a stale pending run (Opus, high)

After B15 (both change `PictureTime.kt` and `Store.kt`). From a review of J2 on `dev` by reading
(2026-10-24).

- **A passed check kept (finding 1):** `nextRun`/`nextAlarm` drop check times before `now`, and
  every re-arm replaces the pair armed before. So a page open, a Set or a `TIME_SET` (network
  time) between 06:07 (the armed check) and its alarm's delivery (up to 06:17) re-armed for
  06:22, and the 06:07 check never ran. The same happened at a run's end when the run did its
  update before the check time.
  - Fix: store each run's start (`lastRunAt`). A check time after it that has passed is armed at
    `now`, instead of being dropped.
  - Tests in `PictureTimeTest`.
- **One run at a time (finding 2):** the periodic job (id 1) could start while the alarm's run
  (id 2) went. Both share the one `DailyJob` instance's `worker` and `stoppedBySystem`, so one
  `onStopJob` interrupted the other's thread, and both runs counted as failed.
  - Fix: a job of either id that starts while a run goes ends at once (`return false` from
    `onStartJob`). The going run's end re-arms, and finding 1's rule covers what it missed.
  - `stoppedBySystem` is reset at each start.
- **A stale pending run (finding 3):** after the process dies during a run, Android may
  reschedule id 2 with its own backoff. While that is pending, `runNow` did nothing and no alarm
  was armed (in Doze: until a maintenance window).
  - Fix: `runNow` schedules id 2 unless a run goes in this process; a pending one is replaced
    (harmless while it isn't running).
  - Each run arms the alarms 15 min ahead at its start, so a run that never reaches its end (a
    frozen or killed process) is retried by them. Its end re-arms as now.
- **The interrupt flag (finding 4):** `Store.update` leaves the thread interrupted after a stop,
  so `ranJob`'s `commit()` skipped the prefs listeners. Fix: clear it (`Thread.interrupted()`)
  before `ranJob`.
- **`BOOT_COMPLETED` after `am kill`** (rosie, J2): likely Android 15+ delivering it when an
  app leaves the stopped state (a force stop, `pm clear`, an install). Harmless: it only
  re-arms. Note it in `CLAUDE.md` as seen; no code.
- **Files:** `PictureTime.kt`, `Store.kt`, `DailyJob.kt`, `JobRuns.kt` if needed, their tests,
  `CLAUDE.md` (Background update), the skill.
- **Tablet** (rosie, `auto_time` off and back on):
  - alarms armed for a check a few minutes ahead (faked clock); a page open after that time but
    before the alarm fired: `dumpsys alarm` shows the pair at once, the run comes;
  - `cmd jobscheduler run -f io.github.buerlino.apodroid 1` while an alarm's run goes: logcat
    shows id 1 ending at once, no interrupted download;
  - `run-as … kill -9` during a run: the alarms bring a run within about 15 min (`dumpsys
    alarm`, logcat);
  - the smoke list.
- **Done when:** the tests pass, the tablet passes.

### [x] J0. Android check: which timed run lands with the screen off, on battery (Opus, medium)

**Result (the night of 2026-10-22/23, rosie, tablet on battery 22:37–06:54, 80% → 79%; deep
Doze all night, maintenance windows at 02:29 and 06:29; `.coord/runs/j0/report.md`, the raw
logs beside it; PASS).** T = 00:36, 04:00, 06:00:
- Allow-listed (`.unr`, Unrestricted, bucket `EXEMPTED`): the alarms (c) and (d) fired at
  T + 10 min three times of three, `idle=true`, and the job each started ran 1 s later with the
  network (`http=200`). The one-off jobs (a) (b) and the periodic (e) waited for a maintenance
  window (+30 min to +2½ h); (b)'s deadline changed nothing.
- Optimized (`.opt`, bucket `rare`): (c) fired at T + 10 min, but its job waited for the next
  maintenance window (+30 min to +2½ h): no network for it in Doze. (d) `setWindow` without
  allow-while-idle: once at the 02:29 window, twice never (still pending at 06:54). (a) (b)
  (e) at the windows.
- **Read with care (the morning's review, 2026-10-23):** (c) `setAndAllowWhileIdle` has a
  window of **+1 h** (session 1's `dumpsys alarm`: `window=+1h0m0s0ms`); it fired at +10 min
  because the probe also armed (d) `setWindow(T, 10 min)` at the same T and Android delivers
  overlapping alarms together at the tightest window's end (opt's (c) likely rode along with
  `.unr`'s). So what was measured is **the pair**; (c) alone may come up to 1 h late (not
  measured). For an allow-listed app (d) alone is allowed in Doze (`flags=0x8`).
- Not measured: whether the alarm receiver's own 10 s temporary allow-list
  (`temporaryAppAllowlistDuration=10000`) lets an Optimized app fetch from the receiver.
- The probes uninstalled and the tablet set back by rosie (her notes; the tablet was gone by
  the morning's review, so not rechecked on it).

(User, 2026-10-22 evening; `CLAUDE.md`, Next features, 0.6.0: the picture time.) The new job
must run at a local time (06:00, or right after APOD's 00:05 US Eastern) on a sleeping phone.
Known: a one-off job's delay alarm didn't wake the sleeping tablet (0.5.0, WP5c); the FP6's
periodic job runs at its window's end, which wakes it. Measure it, don't guess.
- **The probe** (built by day before the night, not part of the app): a throwaway app in
  `.coord/probe-j0/` (gitignored; its own `settings.gradle.kts`, built with the repo's wrapper
  `./gradlew -p .coord/probe-j0`; plain Android, no Compose, no library), installed twice with
  `-Pprobe=unr` and `-Pprobe=opt` as `io.github.buerlino.apodroid.probe.unr` and `.probe.opt`.
  **Built** (2026-10-22 evening, `Probe.kt`; the APKs `.coord/probe-j0/probe-unr.apk` and
  `probe-opt.apk`, untested on a device: the tablet was pascaline's). `adb shell am start -n
  <pkg>/io.github.buerlino.apodroid.probe.Arm --es at "HH:MM,HH:MM,…"` (an activity with no
  window, so the app leaves its stopped state; arming again cancels the probe's jobs first)
  arms, for each local time T:
  (a) a one-off job, `setMinimumLatency` to T, any network; (b) the same with
  `setOverrideDeadline` T + 10 min; (c) `AlarmManager.setAndAllowWhileIdle(RTC_WAKEUP, T)`;
  (d) `AlarmManager.setWindow(RTC_WAKEUP, T, 10 min)`; (c) and (d) start a one-off job with no
  delay and any network from their receiver. Plus (e) a periodic job, 6 h, flex 6 h, as today's
  (the control). Each job run (and each alarm) appends a line to `files/log.txt`: what, T, the
  time it ran, `isDeviceIdleMode`, the battery level and plugged state, and whether an HTTPS
  `HEAD https://science.nasa.gov/` worked (Doze cuts the network for apps not allow-listed).
  No permission beyond `INTERNET`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`.
- **Session 1 (bedtime, about 20 min; "Do J0"):** the tablet's state noted as "For rosie" says
  (into `.coord/runs/j0/backup/`). Install both probes. **First a trial** (screen on, plugged
  in, 5 min): arm `.unr` for 3 min ahead; `run-as io.github.buerlino.apodroid.probe.unr cat
  files/log.txt` must show `ARMED`, then by about T + 1 min `ALARM c`, `ALARM d` and a `JOB`
  line for each of a–d with `http=200` (e's runs at once). A probe bug: STOP, the user fixes it
  by day (no night run on a broken probe). Then the night: `.unr` allow-listed
  (`cmd deviceidle whitelist +…probe.unr`: what the pause hint asks for, Unrestricted), `.opt`
  left Optimized and put in the `rare` standby bucket (`am set-standby-bucket … rare`: a user
  who seldom opens the app, before the restricted bucket). Arm both for three times: about 2 h
  after the session, 04:00, and 06:00. `.coord/runs/j0/start.md`: the time, the battery level,
  `dumpsys deviceidle` (state), `dumpsys jobscheduler` and `dumpsys alarm` for both. Then the
  user unplugs the tablet (Wi-Fi on, screen locked, left alone). APODroid itself: daily off for
  the night, so it doesn't add its own job (set back in session 2).
- **Session 2 (morning, about 20 min; "Do J0's second session"):** both `log.txt`
  (`run-as`), `dumpsys deviceidle`, `dumpsys jobscheduler` (the probes' `Constraint history`),
  `dumpsys alarm` (`Past-due`, the batch history), `dumpsys battery`. A table per probe:
  mechanism × T → the time it ran (late by how much), idle or not, the network. Uninstall both
  probes, set the tablet back. `.coord/runs/j0/report.md`; the verdict is a recommendation:
  which mechanism J2 uses (the simplest that lands within about 15 min for `.unr`, and what
  `.opt` then gets), or STOP if none does without a new permission (then the user decides,
  e.g. `SCHEDULE_EXACT_ALARM`).
- **Done when:** both sessions ran, the table and the recommendation written, the tablet set
  back, the probes uninstalled.

### [x] J1. `:core`: the picture time and the three rules (Opus, high)

Pure Kotlin, tested with JUnit (`PictureTimeTest` new, `ScreensTest`, `TurnTest`), no Android.
The rules are `CLAUDE.md`'s (Next features, 0.6.0: the picture time; user, 2026-10-23 night,
the simplification): one time, one clock, three rules.
- **The values:** `pictureAt` (an hour 0–23, default 6) and the existing `interval` (24, 12,
  6, 3 h); a per-device offset of 0–10 min (stored once by J2, passed in).
- **Change times:** `pictureAt` + k × interval in local time; a column with one source changes
  every 24 h. On the clock's hours through a DST change (a 23 h or a 25 h day). 0.6.0's Set clock
  goes: `turnStartedAt`, `isTurnOver`, `turnSlack` and the clock restart in `restartAndForget`.
  A record's stamp becomes the change time it belongs to. A stamp that isn't a change time
  (M2's migrated 0.5.0 clock, or 0.6.0's own on a test device) counts as the current one, so
  the update sets nothing until the next change time.
- **The rules, by the clock** (user, 2026-10-23 night, after the third review: never by what's
  on the screen, so the past can't shift them):
  - **At a change time** a screen changes: with one source to its newest picture (no set when
    it's on already); with several to the next source after its current one (APOD → NASA →
    Earth, round to itself) among its ticked sources whose newest picture this screen hasn't
    shown. A per-screen note of the last picture shown per source (date and URL) is kept in the
    record.
  - **The window:** a newer picture of the source that went on, out in the 3 hours after the
    change time, still replaces it when a check sees it (the change time kept). After the
    window it waits for the next change time.
  - **Nothing new** (several sources only): the picture stays and the screen **waits**: the
    first new picture of a ticked source goes on as soon as a check sees it (the next in order
    first when two are), and it takes that change time. A screen with one source never waits.
- **Video days** as 0.6.0 (Turns): the stand-in fills APOD's turn where APOD is ticked, and
  "new" is APOD's post being new. A stand-in shown counts as shown for its own source too, so
  NASA isn't put on twice.
- **Equal columns** change together (B2): the lock screen follows home's decision.
- **Set** (`restartAndForget` without the clock): each record's picture is forgotten, so each
  screen is put on again with the new setup. A screen whose owner is still ticked keeps it;
  otherwise the next source by the change rule, else the next with any picture. Each screen
  takes the last change time as its stamp, so the next run doesn't move it on. The shown notes
  stay; the change times don't move.
- **The next run** (`nextRun(now, zone, failed, …)`): the earliest of the next change time plus
  the offset; within the 3-hour window 15 min after the change time, then hourly (06:00, 06:15,
  07:15, 08:15: the :05 posts of every whole-hour zone); hourly while a screen waits; after a
  failed or stopped run 15 min later, then hourly (the alarms retry, not Android's backoff;
  J2). No model of each source's posting time (user).
- **Tests:**
  - Zones: Zurich in summer, in winter and in the week of 25 Oct–1 Nov (APOD at 05:05); Los
    Angeles; Sydney; UTC.
  - The DST days of 2026 (EU 25 Oct, US 1 Nov, Sydney 4 Oct), with every interval.
  - One source: Zurich with today's APOD at 06:05 (on at the 06:15 check); Zurich **after a
    missed morning** (yesterday's at 06:00, today's at 06:15 all the same, not a day late);
    Zurich **after a video day** with "Skip it"; Los Angeles (the 21:05 post on at 06:00,
    nothing at 21:05); Los Angeles **after an evening Set** (06:00 next day as usual, the 21:05
    post never replaces outside the window); Helsinki (07:05, on at 07:15).
  - Several sources: a Monday in Zurich every 6 h; a weekend (APOD stays, nothing set at NASA's
    and Earth's change times); every 12 h with APOD + NASA in winter Zurich (today's APOD at
    about 06:15, not a day late); every 24 h in Los Angeles on APOD's day (no change at 21:05).
  - A failed run's next check (15 min, then hourly); the DST hours: a picture time of 02:00 on
    25 Oct 2026 (the hour happens twice: one change) and on 28 Mar 2027 (it doesn't exist: the
    change at 03:00).
  - A Set mid-turn, with the owner still ticked and unticked; equal columns with a failed set on
    one screen; the stand-in counted as shown; the migrated stamp; `nextRun` with and without a
    waiting screen; the offset in range.
- **Files:** `core/…/core/PictureTime.kt` (new) and its test; `Screens.kt`, `Turn.kt` (what's
  left of it) and their tests; `CLAUDE.md` (Background update, Turns, rewritten to the rules);
  the skill's "End a turn" how-to (`turnStartedAt` and `turn.sh` stop working: fake the clock
  or a record's stamp instead).
- **Done when:** the tests pass, the rules in `CLAUDE.md`.

### [x] J2. `Store` and `DailyJob`: alarms at the change times, the job simplified (Opus, high)

After J1.
- **The mechanism (J0):** at each next run T (J1's `nextRun`), two alarms:
  `AlarmManager.setWindow(RTC_WAKEUP, T, 10 min)` and `setAndAllowWhileIdle(RTC_WAKEUP, T)`,
  with one receiver (new) and two `PendingIntent`s with different request codes (one code
  would make the second replace the first; the J0 probe used two). The first that fires
  schedules the run: a one-off job (id 2, the same `DailyJob` service, no delay, any network).
  The second finds it pending or running and does nothing.
  - **Never schedule id 2 while it's pending or running** (`getPendingJob`, plus a
    process-wide flag while a run goes): scheduling an id again stops its run.
  - Expect: Unrestricted, about T + 10 min. Optimized: the alarm on time, but the network only
    at Doze's next maintenance window (accepted by the user, 2026-10-23).
- **Re-armed:**
  - after each run (either id), Set, the daily switch on, and `onResume`'s `ensure`;
  - at boot (`RECEIVE_BOOT_COMPLETED`, granted; on a phone with a PIN only after the first
    unlock: seen in P);
  - after a time zone or clock change (`ACTION_TIMEZONE_CHANGED`, `ACTION_TIME_CHANGED`; both
    still reach a manifest receiver. The alarms are absolute times, so "06:00" armed before a
    change is wrong after it);
  - after the app's update (`ACTION_MY_PACKAGE_REPLACED`: the first 0.6.0 morning mustn't wait
    for a page open).

  No new permission. Off cancels both alarms and both jobs.
- **Retries by the alarms** (user, 2026-10-23 night): a failed or stopped run ends with
  `jobFinished(params, false)` and `onStopJob` returns false, for both ids, so Android keeps no
  backoff retry (in Doze it would wait for a maintenance window and, pending, block the
  alarms). The run's end arms the alarms by J1's `nextRun` with `failed`: 15 min later, then
  hourly. `stoppedBySystem` feeds `failed`.
- **The periodic job** (id 1) is the fallback: always every 6 h, with a 6 h flex (0.4.0's).
  `ensure` keeps comparing the timing, so a 0.5.0 or 0.6.0-test install's turns job (3 h, a
  15 min flex) is replaced once at the first start; it schedules only while no run goes (the
  process-wide flag), and a run's end calls `ensure` too. Since the timing never changes after
  that, nothing stops a running run. Set and the daily switch on no longer schedule it afresh.
- **The cleanup** (user, 2026-10-23 night; `CLAUDE.md`, the job simplified with it):
  - `JobRuns` keeps only the generation: `started` and `drop` (a run from before an off ends
    without effect).
  - Removed, with their tests: the notes (`note`, `Reschedule`, `rescheduleNoted`), the retry
    due and the failing series (S4f3, B5, B7, B12, B12f1).
  - `stoppedBySystem` stays: a stopped run counts as failed, and the alarms retry it (above).
  - `JobRunsTest` keeps the generation's tests.
  - The KDoc of `DailyJob` and `JobRuns`, and `CLAUDE.md`'s Background update, say only the
    rules that remain.
  - `MainActivity`'s Set and switch paths call the new arming in place of `scheduleJob`'s
    afresh/ensure choice. `SwitchRuns` (B6, the page's quick taps) stays.
- **Setting:** `setDue()` applies J1's rules. `update(page = true)` fetches and shows, and sets
  only what's due by the times (the wallpaper waits when the page opens; no setting). Set and a
  pull set at once.
- **Prefs:**
  - `pictureAt` (default 6), the offset (random once), each screen's shown notes in its record
    (home unprefixed, lock `lock.`).
  - `turnStartedAt` is no longer read (left in the prefs, no migration).
  - `Store.setup` and `saveSetup` carry `pictureAt` (part of the draft, J3).
  - An update from 0.5.0 gets the defaults.
- **Traffic:** `CLAUDE.md`'s Traffic section rewritten: a check at each change time, 15 min
  after it, and hourly while a screen waits; a zone's bursts spread by the offset.
- **Files:** `Store.kt`, `DailyJob.kt`, the alarms' receiver (new), `AndroidManifest.xml`,
  `MainActivity.kt` (the scheduling calls only), `JobRuns.kt` and `JobRunsTest.kt`,
  `CLAUDE.md`, the skill's "Where things are" and its job how-tos (`run -f` still runs id 1;
  id 2 by `cmd jobscheduler run -f io.github.buerlino.apodroid 2` while it's pending, the
  alarms by `dumpsys alarm`).
- **Tablet** (rosie):
  - `auto_time` off for the check (`settings put global auto_time 0`, else the network time
    undoes `cmd alarm set-time`). At the end, back to 1 with the time zone Europe/Zurich
    (rosie's rules reset both), and the tablet's clock checked against the desktop's `date`.
  - With the clock faked near the times:
    - the run at `pictureAt` (`dumpsys alarm` shows both alarms for T);
    - the Zurich case: yesterday's APOD on at 06:00 and today's faked out at 06:05, set at the
      06:15 check; the same after a missed morning (the 06:00 alarm left out by faking the
      clock past it);
    - a waiting screen's hourly check (several sources, nothing new);
    - a failed run (Wi-Fi off at 06:00): no backoff job pending (`dumpsys jobscheduler`), the
      alarms armed for 15 min later, the set at that check once Wi-Fi is back;
    - the alarms' two request codes (`dumpsys alarm` lists two);
    - a 0.5.0-style turns job (3 h, 15 min flex) replaced by the 6 h one at the first start;
    - a page open before the time sets nothing, after it sets what's due;
    - Set and a pull at once;
    - a Set while a run goes: the run isn't stopped (`logcat`: it ends with `jobFinished`);
    - off during a run: nothing set after.
  - The **DST night**, faked: the clock to 2026-10-25 01:50 Europe/Zurich, every 3 h from
    00:00. The next change comes at the clock's 03:00, four real hours after 00:00.
  - Re-armed (`dumpsys alarm`) after a zone change (`cmd alarm set-timezone America/New_York`
    and back) for the new zone's 06:00, after a reboot, and after `install -r` of the same
    build without opening the page.
  - Then the smoke list.
- **Done when:** the checks pass, the tablet passes.

### [x] J3. Settings: New picture at, "More NASA pictures" into Setup, the pause hint (Opus, medium)

After J2, and last of the night (after A4: both change the settings in `MainActivity.kt`).
Placeholders until the user approves them on the tablet.
- **Setup:** "New picture at" [00:00 … 23:00], 06:00 by default.
  - A dropdown as "Largest original": ✓ before the chosen one, screen readers "New picture
    at: 06:00", 12/24 h as the system says (B13's `hours24`).
  - Part of the draft (the highlight, Set). Shown while the daily switch is on, after "Where"
    or the list and before "Change every".
- **"More NASA pictures"** moves to the top of Setup (user, 2026-10-23: Setup is the draft,
  Advanced acts at once). Advanced keeps "Save every picture" and "Largest original" and is
  hidden while the daily switch is off (it's empty then).
- **The summary** under "Setup": " · turns every 6 h" becomes " · every 6 h from 06:00", and a
  screen with one source adds " · at 06:00" (placeholders).
- **The pause hint:** its "Unrestricted" line gains J2's reason (on Optimized the new picture
  can come hours late); placeholder text.
- **Files:** `MainActivity.kt`, `CLAUDE.md` (Settings), the skill.
- **Tablet:** the setting with the highlight and Set (`pictureAt` in the prefs, `dumpsys alarm`
  re-armed), the mode switch at Setup's top (its draft and highlight as before), Advanced with
  the daily switch on and off, font scale 2.0, landscape, TalkBack labels via `uiautomator`.
  Screenshots for the user.
- **Done when:** built, the tablet passes, the texts listed for the user.

### [x] A1. `:core`: the background, black or blurred (Opus, medium)

(`CLAUDE.md`, Next features, 0.6.0: the picture.) Plain Kotlin, tested with JUnit.
- The whole picture: `fitFrame` (F1) as it is.
- `edgesBlack(gray, w, h)`, on a small grey copy (about 200 px wide; the app decodes it with
  `inSampleSize`): true when nearly all of the outer strip (about 3% of each side) is nearly
  black. Black background then; else blurred (user, 2026-10-23 night). The thresholds are a
  night default, listed in the report.
- The blurred background as pure math if it stays small: `blurBackground(argb, w, h)`, three
  box-blur passes on a copy about 1/16 of the screen, darkened by 35%. Else it goes into A3's
  `Store`.
- Not built: `brightestPart` (Fill's part is always the middle; user, 2026-10-23 night).
- Tests:
  - `edgesBlack`: a planet on black (true), a star field with a few bright stars at the edge
    (true), a lit landscape, a nebula filling the frame, a black sky over lit ground (false), a
    thin grey frame.
  - The blur's size and darkening, an even picture stays even, the edges.
- **Done when:** the tests pass.

### [x] A2. The picking page (by day, with the user, 2026-10-22)

Done: 253 real APODs × six versions, 148 clear picks; the result and the rules tested in
`CLAUDE.md` (Next features, 0.6.0: the picture). It decided always Fill; nothing to build.

### [x] A3. `Store`: the whole picture per post, on black or blurred (Opus, medium)

After A1 (user, 2026-10-23 night: independent of J2, so the picture work lands even if J2 needs
another night; it builds on `dev` as it is, and whichever of J2 and A3 comes second rebases on
the other's `Store`).
- The per-post whole picture sits beside the part: `<source>.whole`, keyed as the part (date and
  picture URL). A new post drops it.
- F1's pref `fit` goes (never released; on test devices it is ignored).
- Each set:
  - a whole picture is drawn as F1's Fit (`setBitmap`) on black when A1's `edgesBlack` says
    so, else on the blurred background;
  - otherwise Fill at the user's part, else the middle (`setStream` with the crop, as today).
- The user's own picture (video days): Fill, centred, as now (no whole switch; user,
  2026-10-23 night).
- **Tablet:** a wide picture whole on both screens, one with black edges (a planet on black,
  made up or a real post's picture faked into `files/`) on black and one without on blur; Fill
  in the middle and at a moved part; a new post drops the toggle; `dumpsys wallpaper` and
  screenshots.
- **Done when:** the tablet passes; `CLAUDE.md`, Wallpaper.

### [x] A4. ⛶'s Whole picture toggle; Size goes (Opus, medium)

After A3 (user, 2026-10-23 night; J3 comes after it).
- ⛶ mode gets a "Whole picture" toggle (placeholder) for the shown post. While it's on, the
  frame hides and the page shows the whole picture (no preview of the background: the page's
  preview comes with Live in 0.7.0).
- It's part of the draft: ⛶ shows in `tertiary` while it differs, screen readers say "Whole
  picture, on/off, not set yet", and Set stores it.
- The Size row in Setup goes, and so do " · Fill" and " · Fit" in the summary.
- Texts are placeholders for the user's look on the tablet.
- **Files:** `MainActivity.kt`, `CLAUDE.md` (Settings: the Size row gone; Choosing the part:
  the toggle).
- **Done when:** built, the tablet passes, the texts listed for the user.

### [x] B17. The APOD-mode video notice follows `standInReady` (Opus, medium)

(User, 2026-10-24 evening, from Opus pass 18's question.) In APOD mode `VideoNotice` says "…
takes its place once it has loaded." whenever the stand-in isn't the picture's source yet, also
while it is loaded and only waits for the change time.
- Fix: branch on `standInReady`, as "More NASA pictures" mode does. "… takes its place." when
  the stand-in's picture is stored; "… takes its place once it has loaded." only when it isn't.
- "Your own picture takes its place." is unchanged. The texts are B15's, approved by the user
  2026-10-24.
- **Files:** `MainActivity.kt` (`VideoNotice`), `CLAUDE.md` (Video days on the page) if it says
  otherwise.
- **Tablet:** APOD mode on a faked video day, with NASA as the stand-in, stored and not stored
  (its prefs and file removed); "Use my picture" with a picture. One screenshot each; then the
  smoke list.
- **Done when:** built, the tablet passes.

### [x] CR2. Code review of what changed since 910bfeb, read as a whole (Opus, high)

(User, 2026-10-24 evening: before the final tablet pass, V11; CR's form.) By reading, no
device, no code changes. `git diff 910bfeb..HEAD -- core/src/main app/src/main` and the whole of
each changed file, against `CLAUDE.md`:
- `DailyJob.kt`, `AlarmReceiver.kt`, `Store.kt`, `PictureTime.kt`, `Screens.kt`,
  `Background.kt`, `JobRuns.kt`, `Post.kt`;
- `MainActivity.kt`'s scheduling calls, Setup (J3), ⛶'s toggle (A4) and the set path (A3);
- the manifest.

Each package was audited alone, and B16f1 shows that a cross-check still finds bugs. Look for:
- **The alarms and the jobs together:** every path that arms, re-arms, cancels or schedules
  (a run's start and end, a page open, Set, the switch, boot, time zone, time, update, an
  alarm); two of them interleaved; a check, retry or safety alarm lost or doubled; id 1 and
  id 2 together; a run from before an off.
- **The rules against the clock:** history-dependence; the publish time missing or in another
  zone; DST; a time zone change between runs; the stamps after Set and after the migration;
  equal columns; the stand-in; a failed set.
- **The prefs and the files disagreeing**, and what the next run does then.
- **Memory:** the whole picture's decodes (the edge copy, the blur copy, the fitted bitmap)
  on a huge or odd picture; bitmaps held or recycled.
- **Failure paths:** a `runCatching` that swallows a stop, a log or toast that says the wrong
  thing.
- **`CLAUDE.md` contradicted by the code**, or a rule written there that no code enforces.

Write `.claude/skills/build-apodroid-android/review-0.6.0-cr2.md` in CR's form: one numbered
finding each, worst first, with severity, `file:line`, the failure (steps → what goes wrong),
the fix in one or two lines, and whether it's a bug or a decision. Only what you verified, and
say how sure. Commit it, then the report commit.
- **The audit** checks the form and that the cited lines exist. A finding it thinks wrong is
  not a FIX: B18 checks each again.
- **Done when:** every file read (listed in the report), the findings committed.

### [x] B18. CR2's bugs (Opus, high)

After CR2. Its findings in `review-0.6.0-cr2.md` on `dev`. For each, check it again in the code
first. Then:
- **A bug** (the app contradicts `CLAUDE.md`, crashes, loses data, sets a wrong wallpaper,
  misses or doubles a run, or gets stuck) with a clear fix: fix it, with a test that fails on
  the old code (`:core`) or a `Device (tablet …)` bullet in your report that shows it.
  `CLAUDE.md` updated where it says what happens.
- **A decision** (a text, the UI, a rule `CLAUDE.md` set), or a fix you aren't sure of: not
  built. A `QUESTION:` line in your report, with your recommendation (code freeze: no new
  texts or settings).
- **A finding you think wrong:** say why, not built.

At most six fixes, worst first; the rest listed for the morning. None: say so and commit the
report only.
- **Files:** as each fix needs; `CLAUDE.md`; the skill's "Where things are" if a function moved.
- **Tablet:** each fixed bug's steps, then the smoke list.
- **Done when:** each finding fixed, a QUESTION, or argued wrong; the checks pass; the tablet
  passes.

### [x] V11a. Final pass 1: what the system does to the new job (Opus, high)

(User, 2026-10-24 evening: the final tablet pass, a release condition; V9 redone for the alarms.
The rows' rules: V.) The tree with B17 and B18 (or without B18 when it stayed unmerged: say
which). **Hunt, don't confirm:** go through the whole list even after a bug, with one `BUG:`
line each. For each item note the app's alarms (`dumpsys alarm | grep -B2 -A8 apodroid`: when,
both request codes), the jobs (`dumpsys jobscheduler`: id 1 6 h/6 h, id 2 pending or not), the
log (`logcat -s APODroid`: "alarms for …", runs, sets), the records (`run-as cat` the prefs) and
`dumpsys wallpaper`. Look for a crash (`exit-info`). Faked clock with `auto_time` off, back on
at the end (J2). "More NASA pictures" on with turns on one screen, unless an item says otherwise.
- **Reboot** (`adb reboot`, wait for `sys.boot_completed`): the pair armed again after
  `BOOT_COMPLETED` for the next change time; id 1 listed; no set at boot. A PIN: NOT RUN for the
  rest.
- **Time zone and clock:**
  - `cmd alarm set-timezone America/New_York`, `Asia/Tehran` and back: re-armed for the new
    zone's next change time; the status line's zone follows; nothing set twice.
  - The clock 3 h forward and back (`cmd alarm set-time`): re-armed (`TIME_SET`); nothing
    stuck.
- **Doze, Unrestricted** (`cmd deviceidle whitelist +io.github.buerlino.apodroid`, `cmd battery
  unplug`, the alarms a few minutes ahead by "New picture at" or the clock, then `dumpsys
  deviceidle force-idle`): the alarm fires in idle, its run has the network and sets.
- **Doze, Optimized** (the whitelist entry removed, `am set-standby-bucket … rare`): the alarm
  fires, and id 2 waits pending for the network. Step the clock an hour so the next alarm comes:
  it replaces the pending run (B16), and the run isn't starved. `dumpsys deviceidle step` to
  IDLE_MAINTENANCE: it runs and sets. Then undo the bucket, the whitelist as it was, `battery
  reset`, `deviceidle unforce`.
- **Battery saver** (`cmd power set-mode 1`): the alarm's run sets.
- **During a run** (a slow download: the proxy at 2000 B/s on `assets.science.nasa.gov` with a
  source's date faked back, or a big rendition):
  - `run-as … kill -9`: the safety alarm brings a run within about 15 min.
  - `am force-stop`: Android drops the alarms and the jobs. Note what re-arms them (the next
    page open, or `BOOT_COMPLETED` when the app leaves the stopped state: say which).
  - `install -r` of the same build: `MY_PACKAGE_REPLACED` re-arms; the run counts failed and
    is retried by the alarms.
  - Airplane mode on: the run fails, the alarms re-armed 15 min later, then hourly (step the
    clock); airplane off: the next alarm's run sets.
  - The daily switch off: nothing set after, both alarms and both jobs gone.
  - A page open: the run's 15-minute retry kept (B16f1).
- **Low storage during a Set** (V9's way): the setup saved, the sets from the stored pictures,
  no `.part`, the problem box.
- **Tonight's real DST change** (25 Oct 03:00 → 02:00): if the app's log or `dumpsys alarm`'s
  history from tonight is still there, what it shows; else Not tested.
- **Done when:** each item ran or is under Not tested with the reason; every setting set back.

### [x] V11d. Final pass 2: R8 and the update from 0.5.0 without opening the page (Opus, high)

(V5 and V6 redone on the new job; the rows' rules: V.) R8 builds signed with the debug key (the
skill): `v0.5.0` (a worktree of the tag) and the current tree.
- **The update, every F-Droid user's first morning:**
  1. `pm clear`, the 0.5.0 R8 build, its state seeded by `run-as`: daily on, APOD + NASA, Both,
     "Change every" 3 h, so 0.5.0's job is 3 h with a 15 min flex.
  2. The page opened once (0.5.0 sets and schedules), then Home.
  3. The 0.6.0 R8 build by `install -r`, **without opening the page**.
  4. Expect: `logcat` `MY_PACKAGE_REPLACED` (the receiver), one "Migrated from 0.5.0", the pair
     armed (`dumpsys alarm`), id 1 now 6 h / 6 h (0.5.0's 3 h job replaced), `pictureAt`
     absent and read as 06:00, no set at the update.
  5. The next alarm's run (the clock stepped to it) sets what the rules say.
  6. Then the page: the summary, the status lines, no crash.
- **The same from 0.5.0 in APOD mode** (APOD alone, Both), not opening the page: armed, no set;
  then the page.
- **The 0.6.0 R8 build:**
  - the smoke list;
  - ⛶'s "Whole picture" on a wide post (blurred) and on one with black edges (black);
  - "Save every picture" with a job run (one save logged);
  - B15's publish time read on R8 (`<source>.published` in the prefs after a pull).
- **Done when:** each item ran or is under Not tested; the tablet set back (the original app
  data restored).

### [x] V11b. Final pass 3: the rules on a faked clock (Opus, high)

(The rules' cases no device has seen; the rows' rules: V.) Debug build, `auto_time` off. Real
posts with chosen picture times and the network cut stand in for a made-up feed.
- **The window by publish time** (today's real APOD, published 00:05 US Eastern):
  - "New picture at" a few hours before it, with yesterday's on; the network cut (airplane
    mode) from the change time until after the post's time; on again within the window: set at
    the next check.
  - The same, on again only after the window: set all the same (publish time, B15).
  - A picture time whose window ends before the post: not set until the next change time.
- **The checks:** after a change time `dumpsys alarm` steps through T, T + 15 min, then hourly,
  and T + 3 h (step the clock to each).
- **A waiting screen** (NASA + Earth on one screen on a weekend: nothing new): no set at its
  change times, hourly checks armed.
- **A post without `published`** (the key removed after `am kill`): it counts as its date's
  00:00 US Eastern; the next run sets what the rules say, no crash.
- **A stopped run** (`cmd jobscheduler timeout`): the status line afterwards, the alarms 15 min
  later.
- **A Set while a run goes:** the run not stopped.
- **The stand-in:** a faked APOD video day with NASA standing in, after NASA's picture was
  already shown on that screen: not shown twice (B15).
- A failed set retried after 09:00: Not tested unless a way to fail a set exists (V3).
- **Done when:** each item ran or is under Not tested.

### [x] V11c. Final pass 4: what a user does to the new UI (Opus, high)

(V10 redone where J3, A3 and A4 changed the page; the rows' rules: V.)
- **Fast taps** (one `input` line): Set three times; the daily switch five times; "New picture
  at" changed and Set at once; the mode switch at Setup's top and Set. One set each, the
  alarms armed once at the end, no crash.
- **A rotation during Set:** its end kept (B13): the toast, the alarms, the log.
- **⛶'s "Whole picture"** on made-up pictures in `files/` (debug build): a huge PNG (8000×6000),
  a tall one (600×3000), a panorama (8000×1000), one with black edges. Each set whole: the
  background (black or blurred) as the edges say, no crash.
- **Memory:** 20 whole-picture Sets in a row, then `dumpsys meminfo io.github.buerlino.apodroid`
  (no climb against the first, no OOM in `logcat`).
- **A fresh install** (`pm clear`): the defaults (APOD mode, "New picture at" 06:00 once the
  daily switch is on, Advanced hidden while it's off); the switch on arms the pair.
- **The new Setup** at font scale 2.0 at a phone's width (`wm density 344`): the mode switch at
  the top, "New picture at", the list.
- **Done when:** each item ran or is under Not tested; the tablet set back.

### [x] B19. The job: a killed run keeps its retry; the switch's stop isn't logged as a failure (Opus, high)

(User, 2026-10-25, from V11a's findings 2 and 4; a bug fix under the freeze.)
- **A killed run keeps its retry.** V11a: `install -r` during a run killed it before its end;
  `MY_PACKAGE_REPLACED`'s `ensure` armed the clock's next check (05:24 for a 04:27 run) and
  dropped the start's pair (start + 15 min). The same for any arming in a new process (boot, a
  page open) after `kill -9`, the low-memory killer or a Recents swipe. Android re-ran job 1 by
  itself that time; a killed id 2 it doesn't. Fix: a run that started and never ended counts
  as going for `Store.nextAlarm` in every process, so B16f1's rule keeps its retry (the earlier
  of the clock's next check and `lastRunAt` + 15 min, at the earliest now). E.g. a pref noted
  in `runStarts` and removed in `ranJob` and `switchDaily`; `nextAlarm`'s `running` is true while
  it's there. **Such a run counts as failed** (user, 2026-10-25, from the day's review), like a
  stop by Android: a run that starts and finds the previous run's note still there counts that
  one in `failedRuns` (and `failedAt`) before its own work, so a crash on every run (e.g. out of
  memory decoding a huge picture, never stored, downloaded again each time) is retried 15 min
  later, then hourly, not every 15 min all day. For that, the start's pair (`DailyJob.started`,
  now always `now + firstCheck`) and `nextAlarm`'s cap while a run goes (`:core`'s
  `nextAlarm`, `running`: now always `lastRun + firstCheck`) come `retryAfter(failedRuns + 1)`
  after the start, once the killed run is counted: 15 min with no failure before, an hour after
  one or more. Counting alone changes nothing (the declutter pass of 2026-10-25).
- **The switch off mid-run** logs "Daily job failed: java.io.InterruptedIOException" with a
  stack (`DailyJob.onStartJob`): a run dropped by the switch (`runs.counts(generation)` false)
  logs one info line, "Daily job: stopped (the switch off)", no stack. A stop by Android stays a
  failure (it counts and is retried).
- **Files:** `DailyJob.kt`, `Store.kt`; `CLAUDE.md` (Background update: Retries by the alarms;
  e8709b1's note that a killed run loses its retry goes).
- **Tablet:** a run made long (V11a's way: a source's date faked back), then `install -r` of
  the same build 1–2 s in: `dumpsys alarm` shows the pair at the run's start + 15 min (or at
  once), not the clock's next check; the same with `run-as … kill -9` and a page open after it.
  The next run's start counts the killed one: `failedRuns` 1 in the prefs, and its pair an
  hour ahead (`dumpsys alarm`).
  The daily switch off 2 s into a run: the info line, no "failed". Then the smoke list.
- **Done when:** `PictureTimeTest` cases for `nextAlarm(running = true)`: no failure before, the
  cap `lastRun` + 15 min; one or more, `lastRun` + 1 h. The tablet passes.

### [x] B20. Set says when its setup couldn't be saved (Opus, medium)

(User, 2026-10-25, from V11a's finding 3; the text approved by the user the same day.) On a
full disk Android's prefs write failed too: only `apodroid.xml.bak` was left, and Set's setup
lived only in the process's memory (lost if it died first), while the toast said "Wallpaper
set". `prefs.edit(commit = true) { }` (core-ktx) drops `commit()`'s result.
- Fix: `Store.saveSetup` returns whether its commit was written (`edit()…commit()`). When it
  wasn't, Set's toast is **"Couldn't save the setup. Check the free space."** instead of the
  others, and Setup stays open (as after a failed set). The sets still run (the setup is in
  memory; V11a: they came from the stored pictures).
- **Files:** `Store.kt`, `MainActivity.kt` (Set's toast); `CLAUDE.md` (Settings, Set as
  wallpaper: the toasts).
- **Tablet:** V11a's way (`fallocate` to 0 bytes free, the checks aged 1 h): Set shows the new
  toast and Setup stays open; space freed, another Set says "Wallpaper set" and closes it. Then
  the smoke list. `/data/local/tmp/fill*` removed at the end.
- **Done when:** built, the tablet passes.

### [x] B21. One source after a day offline: the newest goes on (Opus, high)

(User, 2026-10-25, from CR2's finding 3; a change to rule 2.) With one source, the post due at a
change time never shows when a newer one, published after the window, is stored first: Los
Angeles, APOD alone, 06:00, the phone off or offline from 20:00 on day D−1 to 22:00 on day D;
the 22:00 fetch stores D's 21:05 post (after D's 09:00), nothing is set, and D−2's picture stays
until 06:00 on D+1 (also NASA alone in Europe, Earth in the Americas). Only the newest post per
source is stored, so D−1's is gone.
- **Not by post dates** (the day's review, 2026-10-25): NASA and Earth post Monday to Friday
  only, so a date rule ("the screen's post two or more days older than the newest") would put
  Monday's NASA on Monday afternoon in Europe, and Monday's Earth on Monday afternoon in Sydney
  or on Sunday evening in the Americas, every week, instead of at the next change time.
- Fix (`Store.refresh`, `:core`'s one-source path): a new post stored when the source's last
  successful check (`<source>.checkedAt`, read before this check writes it: `refresh` runs
  before `checkSite` notes it) is **more than 24 h old** is marked (`<source>.afterGap`,
  written with the post; a post stored before B21 has none, no migration). `Stored` carries
  the mark, so `dueWallpapers` takes no new argument. With one source, a marked newest post
  goes on whenever a run sees it, as a pull would; the change time kept. The mark lives with
  the post, so a set that fails or a run stopped between the fetch and the set doesn't lose it;
  once the post is on, `show()` doesn't set it again, and the next post replaces the mark (a
  same-date rewrite of the same post, a changed title or explanation, keeps it).
  Weekends and weekdays without a post aren't gaps: the checks at each change time succeed with
  nothing new and still note `checkedAt` (also when `isCurrent` skips the fetch). Never back
  holds. Turns aren't changed (their next change time moves on anyway).
- **Settled** (user, 2026-10-25, the declutter pass):
  - The 24 h decision is a `:core` function (e.g. `afterGap(lastCheck: Instant?, now: Instant)`)
    with tests (25 h marks, 23 h doesn't), so the weekend case is a unit test; `Store.refresh`
    calls it and writes the mark.
  - No `checkedAt` yet (a fresh install, a source's first check) isn't a gap: no mark. Set and
    the daily switch on put the newest on anyway; an update from 0.5.0 has `checkedAt` (0.4.0).
  - On APOD's turn on a video day the stand-in's own mark counts, as its publish time does
    (B15). A stand-in is fetched only on video days, so its first fetch each video day is
    usually marked; with one source that post went on at the change time anyway.
- **A side effect, accepted:** a phone in the RESTRICTED standby bucket (one alarm a day) can
  have gaps over 24 h every day, so its newest goes on whenever that one run comes; its timing
  is off anyway. `CLAUDE.md` says so.
- **Files:** `Store.kt` (`refresh`, `stored()`), `Screens.kt`, `ScreensTest`; `CLAUDE.md` (Turns
  and the picture time, rule 2; Next features, 0.6.0: the picture time, rule 2's line).
- **Tablet:** none of its own; V12b checks it on a faked clock.
- **Done when:** a test that fails on the old code (CR2's Los Angeles steps); tests that NASA
  Friday → Monday with daily checks and the normal evening stay unset; the gap function's tests;
  all `:core` tests pass.

### [ ] V12. The final pass again, on the tree with B19–B21 (Opus, high)

**Status (2026-10-26):** V12b, V12c and V12d passed on the tree with B19–B21 (the night of
2026-10-25; master 654848c). V12a passed the 3 items it ran (the safety alarm after `kill -9`
alone, `kill -9` of job 1, `install -r` during a run) and ran out of time for the rest: the
skill's Still untested lists them. Low storage during Set ran in B20's check and regression 10.
The rest runs as V12e and V12f (user, 2026-10-26), after B22–B24.

(User, 2026-10-25: fix A–D, then V11a–d again in full, before V7 and P.) V12a–d each run their
V11 section (V11a, V11d, V11b, V11c) on dev's tree after B19–B21 (say which of them are
merged), with these changes:
- **V12a (V11a):** Doze, Optimized with `working_set` at once (RARE's quota, about one alarm an
  hour, was seen and accepted). Instead of tonight's DST change (V11a's night is past):
  **the safety alarm after `kill -9` alone**: 15 real minutes with nothing re-arming (no page
  open, no clock step, no `run -f`), then a run from the start's pair. `install -r` and
  `kill -9` during a run: the retry kept (B19). The switch off mid-run: B19's info line. Low
  storage during Set: B20's toast, Setup open.
- **V12d (V11d):** as written, on the new tree.
- **V12b (V11b):** add B21, as V11a aged the checks: APOD alone, `checkedAt` (APOD's keys have
  no prefix) aged to 25 h, the stored post's date faked back, and a "New picture at" whose
  window ends before today's post: the next run fetches it, marks it (`afterGap`) and sets it. The same with
  `checkedAt` 1 h old: no mark, no set until the next change time. "A Set while a run
  goes": try again with a big rendition or the proxy (works only with Tailscale's VPN off on
  the tablet), else Not tested.
- **V12c (V11c):** as written.
- **Done when:** each item ran or is under Not tested; the tablet set back.

### [x] B22. The stand-in not twice with APOD alone too; the video notice says so (Opus, high)

(User, 2026-10-26, from V12b's open question; a change under the freeze.) V12b: APOD alone (no
turns), NASA for video days; on an APOD video day NASA's newest post went on although that
screen had shown it already (noted in its record's `shown`). With turns B15 already passes
APOD's turn on in that case (`Stored.isNew`); with one source `Rules.within` puts
`stored.picture(APOD)`, the stand-in, on regardless. The video notice says "… takes its place"
in both cases, also where it doesn't.
- **The rule** (`:core`, `Screens.kt`): with one source too, the stand-in (NASA's or Earth's
  post) goes on in a run only while its post isn't noted shown on that screen for its own source
  (the check `isNew` makes for turns); else APOD has nothing to show, as with "Skip it", and the
  screen keeps its picture. Once the stand-in is on it counts as shown, and keeping holds it
  there, so the day it went on doesn't change. **Set, a pull, a forgotten picture and a new
  screen put it on as now** (they put the newest on at once, whatever it was: Set sets each
  screen afresh, e.g. after a changed whole picture). The user's own picture always stands in,
  as now. A newer post of the standing-in source goes on by the usual rules (the window, B15;
  the gap mark, B21).
- **The notice** (`VideoNotice`): one `:core` check per screen where APOD is ticked, the stand-in
  shown there before and not on it now (the same check the rule uses, and B23's line). While it
  holds on every such screen the notice reuses the "Skip it" texts, without their second
  sentence (approved, user 2026-10-26: no new text): APOD mode "This APOD is a video, so your
  wallpaper stays as it was."; with "More NASA pictures" on "This APOD is a video, so it's
  skipped." (also with turns, B15's case). Both keep " Tap ▶ to watch it on NASA's site." on
  APOD's page. Otherwise as now.
- **Files:** `Screens.kt`, `ScreensTest`, `Store.kt` (the check for the page), `MainActivity.kt`
  (`VideoNotice`); `CLAUDE.md` (Turns and the picture time: the stand-in; Video days on the page).
- **Tablet:** APOD mode, "Where" Both, NASA for video days, APOD's post faked a video, both
  screens on the day before's APOD, NASA's stored post noted shown on both (`nasa.shownDate` and
  `nasa.shownUrl`, also `lock.`): `run -f 1` sets nothing; the notice "This APOD is a video, so
  your wallpaper stays as it was. Tap ▶ …" (a screenshot). The notes removed: the run sets NASA,
  the notice "… so NASA's Image of the Day takes its place. …", a second run sets nothing. Set
  with NASA already shown puts it on (one set). With "More NASA pictures" on, home APOD + Earth
  (turns), lock APOD alone, NASA noted on both: home passes to Earth (or waits), lock keeps its
  picture; the notice "This APOD is a video, so it's skipped. …". Then the smoke list.
- **Done when:** a `ScreensTest` case that fails on the old code (V12b's: APOD alone, the stand-in
  noted shown, nothing set), with the day the stand-in went on, a pull, Set and "my picture"
  unchanged; the tablet passes.

### [x] B23. No "kept" while something takes APOD's place (Opus, medium)

(User, 2026-10-26, from regression 10's wording question; under the freeze.) With "Use my
picture" stored on a faked video day the status line said "Video day, wallpaper kept" while the
box said "your own picture takes its place": `videoDayKept` is true while the screen doesn't
show the stand-in yet (until the next check or change time: minutes in Zurich, all evening in
Los Angeles).
- Fix (`:core`'s `videoDayKept`): "kept" only when nothing will take APOD's place on that
  screen: "Skip it", "Use my picture" without a picture, or the stand-in shown there before and
  not on it now (B22's check). While a stand-in is chosen and will go on (stored, or still
  loading: the box says "… takes its place once it has loaded."), the screen's line is its last
  change, "Wallpaper changed 25 Oct, 06:12" with its " · APOD" as now, or no line before the
  first set. No new text. With turns nothing changes ("Skip it" passes the turn, nothing kept).
- **Files:** `Screens.kt`, `ScreensTest`; `CLAUDE.md` (Problems and status lines: "Video day,
  wallpaper kept").
- **Tablet:** a faked APOD video day in APOD mode, the screens on the day before's APOD, each
  choice with Set: "Use my picture" with a picture, before the change time (the page open sets
  only what's due; fake the post's `published` after the window): "Wallpaper changed …", not
  "kept"; NASA stored the same; "Skip it": "Video day, wallpaper kept"; B22's already-shown
  case: "kept". A screenshot each. Then the smoke list.
- **Done when:** a test that fails on the old code (a stand-in stored, not on yet: not kept);
  the tablet passes.
- After B22 (both change `Screens.kt`).

### [x] B24. A run's bookkeeping before `jobFinished` (Opus, medium)

(User, 2026-10-26, from Opus pass 23's note; under the freeze.) `DailyJob`'s worker calls
`jobFinished` a few ms before `ended()` runs `Store.ranJob`: a process death in between leaves
`runGoing` (B19) after a good run, and the next run's start counts a failure that didn't happen
(its pair an hour ahead instead of 15 min).
- Fix (`DailyJob.kt`): the interrupt flag cleared (`Thread.interrupted()`, B16: an interrupted
  thread's `commit()` skips the prefs listeners), then the bookkeeping, `ranJob(failed)`
  (`runGoing` removed, the failures counted or cleared), under `lock` with the same guards as
  now (no off since the run's start, the switch on), then `jobFinished`, then `runs.finished`
  and `ensure` as now (`ensure` mustn't schedule a job Android still counts as running:
  scheduling an id again stops its run). A death after `ranJob` and before `jobFinished`: the
  run counts as ended; Android may re-run job 1 (harmless: it finds nothing new).
- **Files:** `DailyJob.kt`; `CLAUDE.md` (Background update, Retries by the alarms) if the order
  is named there; the skill's Where things are (`DailyJob.kt`).
- **Tablet:** a good `run -f 1`: no `runGoing`, no `failedRuns` in the prefs, armed by the clock;
  a failed one (Wi-Fi off): `failedRuns` 1, the pair 15 min after; B19's checks again
  (`install -r` and `kill -9` 1–2 s into a run made long V12b's way: the retry kept, the next
  start counts the killed run, its pair an hour ahead). Then the smoke list.
- **Done when:** built, the tablet passes.

### [x] V12e. V12a's rest 1: Doze and battery saver (Opus, high)

(User, 2026-10-26: V12a ran 3 of its items in its time box; the rest on the tree with B22–B24,
as two rows, the Doze items apart. The rows' rules: V. Before V7's session 1, or on Tue 27 by
day after V7's second session: Remaining 4j.) Of V11a's list only these, with V12's
change: **Doze, Unrestricted**; **Doze, Optimized** with `am set-standby-bucket … working_set`
at once (RARE's quota of about one alarm an hour was seen and accepted); **battery saver**
(`cmd battery unplug` first on the charging tablet). The tree: dev with B22–B24 (say which are
merged). The how-tos: the skill, Working on the phone (also "Learned in the V rows").
- **Done when:** each item ran or is under Not tested; the tablet set back (`dumpsys deviceidle
  unforce`, `battery reset`, the bucket and the whitelist as they were).

### [x] V12f. V12a's rest 2: reboot, the clock, and what happens during a run (Opus, high)

(As V12e.) Of V11a's list only these: **reboot**; **time zones** (New York, Tehran, back);
**the clock** 3 h forward and back; **during a run**, made long V12b's way ("Save every picture"
at No limit with the posts faked back, the second action sent from the tablet's shell in the
same command): `am force-stop` (say what arms the alarms again), airplane mode on (failed, 15
min, then hourly; off: the next alarm's run sets), the daily switch off (B19's "Daily job:
stopped (the switch off)", no "failed", both alarms and both jobs gone), a page open (the run's
retry kept, B16f1). `kill -9` and `install -r` during a run ran in V12a and again in B24's check.
- **Done when:** each item ran or is under Not tested; the tablet set back (`auto_time` 1,
  Europe/Zurich, Wi-Fi on, airplane mode off).

### [x] SIM. `:core`: the rules over random months (Opus, high)

(User, 2026-10-26 afternoon, Remaining 4j: the job and the rules gave 13 of the 24 fix packages
B2–B24, and every check of the rules since J1 is a hand-written case or a faked clock. Under the
freeze: test code only, no `main` code.) A seeded random test on `PictureTimeTest`'s `Phone`
(moved to a test helper both files use; `kotlin.random.Random` with fixed seeds, no new
dependency). The invariants come from `CLAUDE.md`'s rules (Turns and the picture time; Next
features, 0.6.0: the picture time), not from the code.
- **What each seed draws**, over 60 days from 20 Oct 2026 (both 2026 DST ends) or from 20 Mar
  2027 (the EU's spring change): a zone (Zurich, Los Angeles, Sydney, Helsinki, New York, Tehran,
  Kolkata); the columns (APOD alone on one or both screens, equal columns, columns that differ,
  turns of two and three); "New picture at" and "Change every"; the video-day choice (Skip it,
  NASA, Earth, my picture). Posts as the sites post them (`week()`'s times), with APOD late by
  up to 12 h on about 1 day in 10 (as in July), video days about 1 in 9, now and then a post
  without `published` and a second picture on the same date. Runs at `nextRun` with a random
  offset, plus random page opens, pulls and Sets; failed checks (offline spans from minutes to
  two days, so B21's `afterGap` mark comes into play: `Phone` stores it as `Store.refresh`
  does, from the last good check); a failed set on one screen (`failSets`); killed runs (no
  records written, the next alarm by `nextAlarm`'s `running` and `lastRun`).
- **What must hold** (a failure names the seed, the invariant and the day's runs and sets):
  1. Never back: no set of a picture of the same source older by date than the one on that
     screen.
  2. No job or page run sets the picture the screen already shows (Set, the daily switch on,
     a pull and a forgotten picture may).
  3. With turns, no post set twice on one screen (the `shown` notes), except by Set, a pull or
     a forgotten picture; a stand-in counts for its own source (B15, B22).
  4. With one source, rule 2 as `CLAUDE.md` states it: after each run that checked, the screen
     shows the newest stored post published before the last change time + 3 h, or a newer one
     marked `afterGap`, or one a pull or Set put on; a failed check moves nothing.
  5. Rule 3: a waiting screen takes the first new post of a ticked source at the first run
     that stores it.
  6. An unticked source's picture never goes on; "Skip it" never puts a stand-in on; with one
     source the stand-in not twice (B22).
  7. `nextRun` and `nextAlarm` never in the past (at once when a check is owed), never later
     than the next change time + the offset; after failed runs 15 min, then hourly.
  8. `videoDayKept` (B23) is true only on a screen whose APOD turn has nothing to put on.
- **A finding:** a failing seed that the rules as written allow is a QUESTION (the report says
  why); one they don't allow is a bug: the report gives the seed and the steps, and the test
  keeps that seed under `@Ignore("<what>, found by SIM")` so `:core:test` stays green until the
  morning's `B<n>` fixes it. Don't fix `main` code here.
- **Files:** `core/src/test/.../RulesSimulationTest.kt`, the `Phone` helper out of
  `PictureTimeTest.kt`; the skill's Where things are (a line for the new test).
- **Tablet:** none.
- **Done when:** after B23 (it tests B22's and B23's rules); at least 200 seeds in under 30 s of
  `:core:test`; each invariant shown able to fail (a deliberately broken copy of its rule in a
  throwaway run, said in the report, not committed); all `:core` tests pass.

### [ ] B25. The pause hint's unused line on Android 12; the weekday dialog's false sentence (Sonnet, medium)

(E's setup, 2026-10-29, on the API 31 emulator; user: "B25 before the tag".) On Android 12
(API 31–32) the box shows only the unused line (the battery line is 33+), and it says
`Unused app settings → "Pause or manage app if unused": off`. Android 12's App info has the
header "Unused apps" with the switch "Remove permissions and free up space" (seen on AOSP
SE1A.220826.006.A1; 13 and 14 match today's text). `MainActivity.PauseHint`: on API 31–32 the
line reads `Unused apps → "Remove permissions and free up space": off`; 33+ unchanged. The
effect sentence stays (12's hibernation also stops jobs and alarms).
**The weekday dialog** (user, 2026-10-29 evening, from the lean-texts review: "In B25, before
the tag"): it says "On weekends, Friday's pictures come back.", true for 0.5.0's rotation, false
since J1: a screen with turns never shows a picture twice (it waits, rule 3), and with NASA or
Earth alone Friday's picture simply stays. Drop that sentence; the dialog keeps "NASA Image of
the Day and Earth Observatory post on weekdays only." and its buttons.
Both are page texts, not the job or the rules: W's count stays.
- **Device (emulator, by hand):** dev's debug build on `apodroid-31` (emulator.md, The setup):
  the box's line (a screenshot for the user), App info's switch off → the line gone at the next
  resume; "More NASA pictures" on, tick NASA: the dialog's one sentence (a screenshot). On the
  tablet (15) the box is unchanged.
- **Done when:** the line on 31–32 as above (the user approves the text from the screenshot),
  33+ unchanged, `CLAUDE.md`'s pause hint names the Android 12 path; the weekday dialog without
  the weekend sentence, and `CLAUDE.md`'s weekday dialog paragraph without it.

### [ ] P. Phone pass on the FP6 (Opus, high)

Before R (user, 2026-10-19, the last pass: a bug P finds would otherwise mean redoing R's
changelog and screenshots). First the version: `versionCode` 10, `versionName` 0.6.0 in
`app/build.gradle.kts`, so the phone gets the real build (and its User-Agent). Then as 0.5.0's
WP8: the release-signed build over the user's Obtainium install (the skill, Testing an update
on the user's phone), the update keeping the settings, both modes, a Set from turns into APOD mode
with the daily switch on (the alarms re-armed in `dumpsys alarm`; the periodic job a fixed
6 h fallback since J2), what the tablet nights didn't test (V10, regression 6: "my picture" from the
picker as a huge, tiny, panorama, tall, WebP, GIF and EXIF-8 picture (Fill, centred); the
video notice's and the battery hint's "Don't show again" with its dialog and "App info"; the
long presses' clipboard; Recents swipe-away), the list at font scale 1.5
and 2.0, a lock screen with its own picture, landscape; home and lock set separately on Android
16 (M0 checked the tablet only); the shown source after a swipe-away from Recents (expected:
back to the opening page, the picture's source; after the system kills the app it stays,
`CLAUDE.md`, The title and the swipe); the
job on mobile data (B10: no "Wi-Fi only"); the title dropdown and the swipe (S6); the daily
switch at once (S4f2); "Largest original" (F6) on the phone's own gallery; the picture (A3,
A4) on the phone's screen: ⛶'s "Whole picture" toggle on a wide post (blurred) and on one with
black edges (black), Fill from the middle and a moved part;
S5's highlight on the phone's colours (faint beside the blue on the tablet's) and its headers'
screen-reader states with TalkBack ("expanded", "collapsed", "not set yet": `uiautomator`
doesn't show them).
**The picture time on the FP6** (2026-10-23; the case that started J0–J3: 0.5.0 waited until
10:22 for a 06:05 APOD): the update installed by `adb install -r` in the evening **without
opening the page**: the alarms armed (`dumpsys alarm`, `MY_PACKAGE_REPLACED`); then **one real
night**, Monday 26 to Tuesday 27 Oct or later (a weekday morning: new NASA and Earth posts), the
phone as the user leaves it (Unrestricted, `EXEMPTED`), "New picture at" 06:00: the wallpaper
changes at about 06:00–06:20 (APOD comes out at 05:05 Zurich until 1 Nov, 06:05 after), not
10:22; the morning reads `dumpsys alarm`'s history, `dumpsys jobscheduler` and the usage events
(the set came before the screen was first turned on). "New picture at" and "More NASA
pictures" in Setup, and the pause hint's new "Unrestricted" text on the phone. A **reboot with the PIN**: no
alarm armed until the first unlock (`BOOT_COMPLETED` waits for it), armed right after; a time
zone change in Settings: armed again for the new zone's 06:00. After V12a–d and V7 and their
`B<n>` fixes. Report what was tested; a bug becomes a
`B<n>` package (night.md, The morning, step 6) before R. P's install starts W: after P's
checks the phone keeps the candidate, set up as the user uses it.

### [ ] W. A week on the FP6 before the tag (Opus, medium)

(User, 2026-10-26 afternoon, Remaining 4j: no real night has run the new job yet, and the case
most European users meet every morning starts only on 1 Nov.) From P's install (Mon 26 evening)
to the morning of **Mon 2 Nov**, the FP6 runs the 0.6.0 candidate (P's release-signed build) as
the user's phone: no clock steps, no forced runs, no `pm clear`; the battery and the setup as the
user leaves them, "New picture at" 06:00. What it covers that no row did:
- **Real weekday mornings** (Tue 27 to Fri 30): the set at about 06:00–06:20 Zurich (APOD out
  at 05:05 until 1 Nov).
- **A weekend** (Sat 31, Sun 1): NASA and Earth post nothing. With turns the screen waits (rule
  3) and no picture comes back; with APOD alone the daily set.
- **The US DST end** (Sun 1 Nov, 02:00 Eastern): APOD moves to 06:05 Zurich, and `published`
  is read as US Eastern across the change (B15). The phone's own clock changed on 25 Oct, before
  the install: a DST change of the phone itself stays faked-only (Still untested).
- **Mon 2 Nov, the Zurich case** on a real clock for the first time (`CLAUDE.md`, the picture
  time): 06:00 finds yesterday's APOD on and sets nothing new, the 06:15 check sets the 06:05
  post (rule 2's window).
- Daily use as it comes: page opens at any time (they set only what's due), pulls, a reboot.
- **W's start** (with P's install): `.coord/runs/w/start.md`: the build's sha256, the setup
  (Setup's summary, the list or "Where", the video-day choice), the battery setting and the
  standby bucket (`am get-standby-bucket`), the expected sets for each morning to 2 Nov.
- **W's morning read** (each morning, about 10 min, the phone on USB): `logcat -d -s APODroid`
  (the "Daily job" lines; a release build can't be read with `run-as`, so the log, `dumpsys
  wallpaper`'s ids, `dumpsys alarm`'s history and the page's status lines are the evidence),
  `dumpsys jobscheduler` for id 1, `dumpsys usagestats` (was the set before the screen first
  went on?). Append to `.coord/runs/w/report.md`: each set's time against the expected, the
  day's verdict (PASS, FIX, STOP), anything odd. A missed day's log may have rolled over: say so.
- **The tablet**, if no package needs it: after V12e and V12f, V7's setup (turns) left running
  to 2 Nov, read with the same morning read (`run-as` works there, a debug build). A package's
  tablet check ends it (say when in the report).
- **A bug** becomes a `B<n>` as the gate says; its build replaces the candidate. A fix to the
  job or the rules restarts the count: at least two weekday mornings after the fix (from 2 Nov
  each one has APOD after 06:00), and the tag waits for them.
- **Done when:** every morning Tue 27 to Mon 2 Nov read (Mon 2 the Zurich case) with no open
  `BUG:`; the skill's Still untested updated (the real mornings, the weekend, the US DST end,
  the Zurich case out; what W couldn't see in).

### [ ] E. Android 10, 12, 13 and 14 in the emulator (Opus; E0 high, E1a–E4 medium)

(User, 2026-10-29: before the tag, a baseline for 0.7.0; Remaining 4j.) The findings, the
decisions, the setup and the packages are in `emulator.md`: the emulators `apodroid-29`, `-31`,
`-33`, `-34` (set up 2026-10-29); **E0** by day (the night shift learns an emulator), then
**E1a** and **E1b** (10), **E4** (14), **E2** (12), **E3** (13) in one night, on W's candidate as
root. Its
setup found **B25** (the pause hint's unused line on Android 12; draft in `emulator.md`).
- **Done when:** E0, E1a, E1b and E2–E4 done there (each list ran or is under Not tested, each bug a `BUG:` line);
  the skill's Still untested Android 10–14 line updated with what the emulator can't tell
  (`emulator.md`).

### [ ] R. Docs, changelog, store texts, screenshots (Opus, medium)

After P and W, on the build that passed. `changelogs/10.txt` (max 500 characters; say that a
video-day choice saved while it was greyed out in 0.5.0, e.g. "Use my picture" with turns, now
works with turns, second review); `README.md`, `full_description.txt` (APOD first, the
switch); the changelog also names Set, the daily switch, swiping and "Largest original",
the picture time and new pictures first, the whole picture (500
characters is tight: one short line each, the rest in `full_description.txt`);
`features.md` (F6 added under Save every picture) and this file ticked; the screenshots on the FP6 by the user (APOD mode
first, as `README.md` embeds `1.png`).
**The tag's build on the FP6** (2026-10-29, from E's review: once B25 or any other fix merges, the
tag ships an APK no device has run; W's candidate is 423f082's): the release build from the final
tree, release-signed by the user (the skill, Testing an update on the user's phone), installed with
`adb install -r` over W's candidate on the FP6: the update keeps the setup (Setup's summary), the
alarms armed again (`MY_PACKAGE_REPLACED` in the log, the pair in `dumpsys alarm`), the page opens
without a crash, the pause hint (if shown) with its 16 text. Before T; a failure is a `B<n>`.

### [ ] T. Commit and tag (Sonnet, low)

Only when the user says so; the user pushes.
