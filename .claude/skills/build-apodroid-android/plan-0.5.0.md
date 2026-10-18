# Plan for 0.5.0 (more NASA pictures of the day)

Planned 2026-10-16; rewritten 2026-10-18 by the release audit (WPR) as one path from today to
the `v0.5.0` tag. Release target: 0.5.0 (versionCode 9), no date set. Work through the packages
in order, one at a time: build, show it on the device, wait for the user. Tick them off and note
where the work differed from the plan. Each package writes its decisions into `CLAUDE.md` (the
source of truth) when it lands; this file is the how. Finished packages are one line each here;
git has their notes (`git log -p -- .claude/skills/build-apodroid-android/plan-0.5.0.md`).

## The request

The repo's first GitHub issue (2026-10-16): NASA Image of the Day and Earth Observatory Image of
the Day as further sources. First as a third choice for video days, so there's a new picture
every day; then all three taking turns every 12, 6 or 3 hours, each user-selectable. NASA
History Image of the Day is on Flickr and left out (the issue says it needs different work).

What the two sites deliver (endpoints, fields, timing, sizes): `CLAUDE.md`, Data source, Other
sources. Real responses for checks: `private/iotd-feed-2026-10-16.xml` (the NASA feed, 60
items) and `private/eo-last40-2026-10-16.json` (the 40 newest Earth Observatory posts).

## Decisions

- **Sources + taking turns, tabs, the sharper wallpaper** (user, 2026-10-16; details approved
  2026-10-17): built, in `CLAUDE.md` (The app, Settings and Tabs; Background update, Turns;
  Next features). From `features.md` only Sharper wallpaper (item 1) is in 0.5.0; Fill/Fit (2),
  Wi-Fi only (4), Share (6), Image credit (7) and Save every picture (8) are 0.6.0.
- **Custom hours**: built and dropped (user, 2026-10-17; `CLAUDE.md`, Next features). Kept from
  it: the weekday dialog.
- **Audit fixes** (2026-10-17, after WP6): WP7a–7e, the user chose them all for 0.5.0.
- **The page after the new sources** (2026-10-17, a UI review): U1–U7; the findings and the
  user's decisions are at U1.
- **Release audit** (user, 2026-10-18, WPR): the goal is a stable 0.5.0, after which the app
  rests for a while. The user's decisions in it: all of U1–U6 stay in 0.5.0; the four small
  findings are fixed now (WP7e's items 5–7, WP7f, WP7g); the verification passes come after the
  last code change (U7 on the tablet, WP8 on the phone).

## Packages

| WP | What | Model, effort |
|----|------|---------------|
| 1–7d, R | Done (below) | |
| 7e | Small page fixes, store texts, landscape on the tablet | Sonnet, medium |
| 7f | A storage failure is named as one | Opus, medium |
| 7g | A stopped job ends its run; a clock set backwards | Opus, medium |
| U1 | Label "Change wallpaper automatically", Material You theme | Opus, medium |
| U2 | The caption: source line, "On wallpaper" chip, explanation preview | Opus, medium |
| U3 | Source chips and the two headed sections | Opus, medium |
| U4 | "If APOD is a video" always shown; "Set as wallpaper now" always shown | Opus, high |
| U5 | Notice boxes: orange only for problems | Sonnet, medium |
| U6 | Two panes on wide screens (the last code change) | Opus, high |
| U7 | Docs, store screenshots, the tablet pass on the final tree (debug and R8), the tablet restored | Opus, high |
| G | Release gate: static audit, the start of the phone pass (done 2026-10-18) | Fable, high |
| 8a | Revert the widened crop hint (U7's landscape fix): portrait depends on the launcher | done in G |
| 8b | A stop during a rendition download ends the run (found in G by reading) | done in G |
| 8c | Chips in a grid at large fonts, segments one height (found in WP8) | done in WP8 |
| 8 | Complete verification pass on the phone (the user's own, release-signed), continued on the rebuilt APK | done 2026-10-18 |
| 9 | Commit and tag, when the user says so | Sonnet, low |

Each package runs in a fresh session started from the repo folder with its model and effort.
Run `git status` first and build on what's there (all of 0.5.0 is uncommitted); don't revert or
reformat files you didn't touch. **When a package is done (tested on the device and reported),
end the report with the command for the next one**, copied from here:

```
claude --model sonnet --effort medium "Do WP7e in .claude/skills/build-apodroid-android/plan-0.5.0.md"
claude --model opus --effort medium "Do WP7f in .claude/skills/build-apodroid-android/plan-0.5.0.md"
claude --model opus --effort medium "Do WP7g in .claude/skills/build-apodroid-android/plan-0.5.0.md"
claude --model opus --effort medium "Do U1 in .claude/skills/build-apodroid-android/plan-0.5.0.md"
claude --model opus --effort medium "Do U2 in .claude/skills/build-apodroid-android/plan-0.5.0.md"
claude --model opus --effort medium "Do U3 in .claude/skills/build-apodroid-android/plan-0.5.0.md"
claude --model opus --effort high "Do U4 in .claude/skills/build-apodroid-android/plan-0.5.0.md"
claude --model sonnet --effort medium "Do U5 in .claude/skills/build-apodroid-android/plan-0.5.0.md"
claude --model opus --effort high "Do U6 in .claude/skills/build-apodroid-android/plan-0.5.0.md"
claude --model opus --effort high "Do U7 in .claude/skills/build-apodroid-android/plan-0.5.0.md"
claude --model opus --effort high "Continue WP8 in .claude/skills/build-apodroid-android/plan-0.5.0.md: the gate (WPG) did the first items; start at 'Still to do'"
claude --model sonnet --effort low "Do WP9 in .claude/skills/build-apodroid-android/plan-0.5.0.md"
```

Devices: the skill, Working on the phone. The tablet (Lenovo, Android 15) is free for testing
and stays in test state until U7 restores it; the FP6 (Android 16) is the user's own phone with
their settings: only WP8 touches it, and only as described there. The build check:
`ANDROID_HOME=~/Android/Sdk ./gradlew :core:test :app:lintRelease :app:assembleRelease`
(`--rerun-tasks` and a `clean` to see warnings and to compare two release builds' sha256).

## Done

- **WP1** (2026-10-16): `:core` with three sources, `renditionUrl`, `originalUrl`.
- **WP2** (2026-10-16): the rendition as the daily download, ★ saves the original. Open: the
  FP6 comparison with a 0.4.0 screenshot (WP8).
- **WP3** (2026-10-16): a picture and prefs per source; NASA and Earth as video-day choices.
- **WP4** (2026-10-16): tabs. Open: a NASA tab with nothing stored (U7).
- **WP5** (2026-10-16, proposals approved 2026-10-17): tick boxes, turns, the job's period;
  then a failed check keeps the turn, a `load()` during one runs again. Edge cases accepted
  by the user (Day with three sources, changes at tick time, fewer at night, the restricted
  bucket, offline pausing the turns, an APOD video keeping one other source all day).
- **WP5b/5c** (2026-10-17): custom hours built and removed; the code is in
  `private/wip-0.5.0-with-custom-hours-2026-10-17.patch`. Kept: the weekday dialog.
- **WP5d** (2026-10-17): the open page follows the job's sets and posts (`Store.watch`).
- **WP6** (2026-10-17): version, changelog 9, texts, README, the fdroiddata branch
  `apodroid-nonfreenet-nasa-gov` in `../fdroiddata` (not pushed; WP7e amends it). The
  screenshot wasn't retaken (U7, WP8).
- **WP7** (2026-10-17): the tablet pass on the R8 and debug builds of that day's tree; all
  items tested, no failures. Its two findings became WP7g (1) and WP7e item 6 (2). The
  handoff files are in `private/wp7-handoff/` (`backup/`, `turn.sh`, `swap.sh`, `ui.sh`,
  `proxy/`, `shots/`; the skill describes them). The reboot check: the job ran by itself after
  the reboot and started the next turn.
- **WP7a** (2026-10-17): `nextTurn` in `:core` with `TurnTest`.
- **WP7b** (2026-10-18): ★ shows the shown tab's state; one save at a time, own temp files.
- **WP7c** (2026-10-18): a bad picture remembered (`badUrl`), a pull keeps its force, a page
  open skips a source checked within the hour, a bad URL is `SiteChanged`.
- **WP7d** (2026-10-18): the picture URL beside the date for the wallpaper, ★ and the crop
  (`Post.isNoted`); the migration rule in `CLAUDE.md`, Stack.
- **WPR** (2026-10-18): the release audit. No blocker; 42 tests green, `lintRelease` clean, two
  clean release builds with the same sha256 (`9bfbb5fc…9122`). It produced WP7e's items 5–7,
  WP7f and WP7g, brought `CLAUDE.md` and the skill in line with the code, and this rewrite.
  Left as is (user, 2026-10-18): a pending gallery item after death mid-save (Android expires
  it), a 0.1.1 user's `daily` after the update (its migration was removed in 0.4.0 by the rule),
  "Wallpaper on" restarting the turn (any set by the user starts a turn). Not run: an R8 build
  on a device since WP7 (U7).

## Remaining

### [x] WP7e. Small page fixes, store texts, landscape on the tablet (Sonnet, medium)

Files: `MainActivity.kt`, `Store.kt` (`watch`), `SourcesTest.kt`, `README.md`,
`fastlane/metadata/android/en-US/full_description.txt`, the fdroiddata branch, `CLAUDE.md`.
1. **The weekday dialog closes on a rotation** (activity state, not saved). Save
   `weekdayDialog` in `onSaveInstanceState`, as `tab` is.
2. **The open page doesn't show a problem the job just had.** `Store.watch` fires for sets and
   posts only. Add each source's `problem` and `httpCode` (not `checkedAt`, which changes on
   every run), so `follow()` shows or clears the box.
3. **The privacy line is inaccurate.** `full_description.txt` and `README.md` say the app
   "talks only to science.nasa.gov and www.nasa.gov". Pictures also come from
   `assets.science.nasa.gov`, and 4 of 60 NASA items from `images-assets.nasa.gov`. New line:
   "The app talks only to NASA's websites (nasa.gov)." Also the `NonFreeNet` text in the
   fdroiddata branch `apodroid-nonfreenet-nasa-gov` (WP6): amend its commit and run
   `fdroid lint` and `rewritemeta`. The user pushes it.
4. **The video-day toast** (`setWallpaperNow`, WPR 2026-10-18): "Video day: wallpaper kept"
   also when NASA or Earth is chosen for video days but nothing of theirs is stored yet. Say
   that instead, e.g. "NASA's Image of the Day hasn't loaded yet" (`Source.possessive`;
   placeholder, the user approves it). "Keep", and "my picture" without a picture, keep theirs.
5. **"8:21 PM" wraps** between the time and "PM" at font scale 2.0 on a phone's width (WP7,
   finding 2): a no-break space in the 12-hour pattern of `statusText`, `"d MMM, h:mm a"`.
6. **Two Kotlin warnings** in `SourcesTest.kt:157` (needless `!!` on `p.imageUrl` after the
   first): drop them, so a build has no `w:` lines.
- **State (2026-10-18):** items 1–6 built; `:core:test`, `lintRelease`, debug build clean, no `w:`
  lines. The fdroiddata edit (`NonFreeNet` text → "(nasa.gov)") is in its working tree, not
  amended (the commit was denied), and `fdroid` isn't installed here. The device checks below
  are not run. Later: the branch has the amended commit `2e8653f` ("NonFreeNet text names
  nasa.gov", clean tree, seen in U7); the device checks ran in U7.
- Device (tablet, debug build):
  - the dialog survives a rotation;
  - the box follows a job failure on the open page (NASA blocked by the proxy, `run -f`);
  - the toast of 4 on a faked video day with NASA chosen and the `nasa.*` prefs and `nasa.jpg`
    removed (switch off, set-now on APOD's tab);
  - the status line of 5 on a 12-hour clock at font scale 2.0 and a phone's width (`wm density
    301`): "8:21 PM" on one line, or the break before the time; settings put back after;
  - **landscape**, untested since the crop came in 0.3.0. Set a wide and a narrow picture,
    then screenshot the home screen with the tablet in portrait and in landscape. If landscape
    is badly zoomed or cut, report it with screenshots and ask the user (fix in 0.5.0, or note
    it and move it to 0.6.0).
- Done when: the six items are built and the device checks pass, the build has no `w:` lines,
  `CLAUDE.md` has 2 (The page) and the toast text, and the fdroiddata commit is amended.

### [x] WP7f. A storage failure is named as one (Opus, medium)

From the release audit (WPR, 2026-10-18; the user chose to fix it now). `checkSite` calls every
failure that isn't `SiteChanged` or `HttpStatus` the network, so when the picture can't be
written (a full disk: the `IOException` comes from the file in `download()`; or `renameTo`
failing, today `SiteChanged("Not stored")`) the box says "Couldn't reach NASA's site. Check the
connection.", and the user looks at the wrong thing.
Files: `core/.../Post.kt` (`download`), `ApodTest.kt`, `Store.kt` (`refresh`, `checkSite`,
`Problem`), `MainActivity.kt` (`problemText`, `problemKey`), `CLAUDE.md`.
- `:core`: `download()` tells the file's failures from the network's. A failure opening or
  writing `to` is thrown as a new `StorageFailed(cause)` (an `IOException`, like the other two);
  reading from the connection stays as it is. The copy loop is written out (read a chunk, then
  write it) so the two sides can be told apart; WP7g adds its interrupt check to the same loop,
  so build WP7f first.
- `Store.refresh`: a `renameTo` that fails is `StorageFailed` too, not `SiteChanged`, and sets
  no `badUrl`. `checkSite` stores `problem` "storage"; `Problem.Storage`; the page's text
  (placeholder, the user approves it on the tablet): "Couldn't store the picture. Check the
  free space.", with the source's name as for the others; `problemKey` "storage".
- Tests (`ApodTest`): a download to a file in a folder that doesn't exist is `StorageFailed`,
  and the server saw the request (so it isn't the connect); the incomplete-download and HTTP
  tests still give the network and `HttpStatus`.
- **State (2026-10-18):** built; tests (incl. the new `StorageFailed` one), `lintRelease`, debug
  build clean. The device check below is not run.
- Device (tablet, debug build; a test device, never the FP6): note `df /data`, fill the storage
  almost completely with a scratch file (`dd if=/dev/zero of=/data/local/tmp/fill bs=1m` until
  it fails, then free about 1 MB), a pull → the box "…Couldn't store the picture…", no `.part`
  left, the stored picture kept and shown; delete the scratch file → a pull stores the picture
  and clears the box; `df /data` as before.
- Done when: the tests pass, the device check shows the storage text and the recovery, and
  `CLAUDE.md` (Problems: the third typed failure; Fetch) says so.

### [x] WP7g. A stopped job ends its run; a clock set backwards (Opus, medium)

Two findings from the release audit (WPR, 2026-10-18; the user chose to fix both now). After
WP7f, whose copy loop this package extends.
Files: `DailyJob.kt`, `Store.kt` (`update`), `core/.../Post.kt` (`download`), `ApodTest.kt`,
`core/.../Turn.kt`, `TurnTest.kt`, `CLAUDE.md`.
1. **`onStopJob` doesn't stop the run** (`DailyJob.kt`; WP7, finding 1). It returns true
   (reschedule) but the thread goes on: it may set the wallpaper after Android's deadline, and
   with the process frozen it holds the refresh lock until its socket times out (seen
   2026-10-07). Fix: `onStartJob` keeps its `Thread` and `onStopJob` interrupts it.
   `Store.update` checks `Thread.currentThread().isInterrupted` before each source and before
   `setDue`, and ends the run with `InterruptedException` (a failure, so Android reschedules the
   job as now). `:core`'s `download()` checks `Thread.interrupted()` once per chunk and throws
   `InterruptedIOException` (deleting `to`), so a download ends within one read instead of at
   the 30 s timeout. The page's update (a coroutine on IO) is never interrupted: unchanged.
   Test (`ApodTest`): with the current thread interrupted, `download()` fails with
   `InterruptedIOException`, the file is gone and the server saw the request; the check clears
   the interrupt flag, so the test needn't.
2. **A clock set backwards keeps the turn** (`Turn.kt`, `nextTurn`): `turnStartedAt` then lies
   in the future, so `now >= startedAt + length - slack` stays false until the clock catches up
   (a day, if the user set it back a day). `checkedRecently` already guards `at <= now`. Fix:
   a `startedAt` after `now` counts as over, so the next source gets its turn at once and its
   set writes a fresh `turnStartedAt`. Tests (`TurnTest`): `startedAt` a minute after `now` →
   the next source; `startedAt == now` → the current one stays.
- **State (2026-10-18):** both built; tests (new `ApodTest` and `TurnTest` ones), `lintRelease`,
  debug build clean. An interrupted download notes no `problem`. Device checks not run.
- Device (tablet, debug build): for 1, APOD's picture slowed by the proxy (`assets.science.nasa.gov
  slow 8000`), a stored APOD date faked back, `run -f`, then `cmd jobscheduler timeout` a few
  seconds in: the log shows the run end within seconds ("Daily job failed: …Interrupted…"),
  no `.part` left, nothing set; the retry about 30 s later downloads afresh and sets. For 2,
  `turnStartedAt` faked an hour into the future (`run-as sed` as in the skill's End a turn,
  adding instead of subtracting) and `run -f` → the next source set once, `turnStartedAt` fresh.
- Done when: the tests pass, both device checks pass, and `CLAUDE.md` (open question 1: what a
  stop does now; Turns) and the skill's "Still untested" (the stopped-job bullet) say so.

### The page after the new sources (U1–U7)

A UI review (2026-10-17) from a read-through of `MainActivity.kt` and a screenshot of the
installed 0.4.0; the 0.5.0 page wasn't run, so findings about tabs and chips come from the code.
The user decided every finding that day and kept them all for 0.5.0 on 2026-10-18 (WPR). The
packages all edit `MainActivity.kt`, one after another; U6 is the last code change before the
verification passes.

| # | Finding | Decision (user, 2026-10-17) | Package |
|---|---------|-----------------------------|---------|
| 1 | "Change wallpaper daily" is wrong with "Change every 3 h" | Fix: rename | U1 |
| 2 | "Pictures from" and "On video days" overlap; the video section disappears at 2+ ticks | Always show, relabel | U4 |
| 3 | Which picture is on the wallpaper is hidden (grey "· Wallpaper", status line below the fold) | Chip top-left on the picture, drop the date-line text | U2 |
| 4 | With the switch on, the picture shown can only be set via ⛶ then ✓ | Button always visible | U4 |
| 5 | Tab names ambiguous ("NASA"), names inconsistent | Source line above the title; tabs stay short | U2 |
| 6 | Three orange boxes look alike, orange means failure | Colours only: order stays (user, 2026-10-16) | U5 |
| 7 | Page height jumps between tabs (picture shapes differ) | **Leave as is** | none |
| 8 | Stock Material purple, no identity | Dynamic colour (Material You) | U1 |
| 9 | Mixed widgets, flat spacing; tall source rows | Chips for sources, two headed sections | U3, U4 |
| 10 | Explanation hidden behind a small ▾ | Two-line preview | U2 |
| 11 | ☆/★ reads as "favourite" | **Leave as is** | none |
| 12 | Single column too wide on tablets and landscape | Two panes on wide screens | U6 |
| 13 | Docs, store screenshots, verification | Follows the packages | U7 |

Left as is on purpose: the picture height (7), the star glyph (11), the weekday dialog (stays a
dialog: "no text on the page"), the notice order (user, 2026-10-16), no "next change" line (job
windows are approximate, so it would be wrong).

### [x] U1. Label and theme (Opus, medium): findings 1, 8

Files: `MainActivity.kt` (`Settings`, `onCreate`), `README.md`, `full_description.txt`,
`features.md` (the mock-up), `CLAUDE.md`.
- **Fix (1):** `Text("Change wallpaper daily")` in `Settings()` becomes "Change wallpaper
  automatically". Why: with turns of 12, 6 or 3 h it isn't daily. Placeholder text; the user
  approves it when they see it. Also in `README.md` ("**Change wallpaper daily:**"),
  `full_description.txt` and the `features.md` mock-up. The pref `daily` and the code names stay.
- **Change (8):** `MaterialTheme(colorScheme = darkColorScheme())` in `onCreate` becomes
  `dynamicDarkColorScheme(this)` on `Build.VERSION.SDK_INT >= 31`, `darkColorScheme()` below
  (minSdk is 29). Always dark, as now. The notice box's orange stays hard-coded.
- **Risk to check on the tablet (Android 15), noted again by WPR:** the app sets the wallpaper,
  and dynamic colour follows the wallpaper; a changed palette is a configuration change, so a
  set may make Android recreate the activity, losing the state that lives in activity fields
  mid-action: crop mode, a running load, the toast, a ★ save (WP7b). Set the wallpaper from the
  page ("Set as wallpaper now", ✓ in crop mode) and look for a flash or recreation
  (`adb shell dumpsys activity` or the log). If it recreates: say so and propose the fix
  (handle the config change, or fall back to a fixed palette: the user decides, don't pick).
- Not testable here: Android 10–11 (the stock palette); say so.
- Done when: both changes are on the tablet, the recreation check is reported, and `CLAUDE.md`
  (Setup, the "Dark theme" line: stock → dynamic on 12+, 2026-10-17) is updated.
- **Done 2026-10-18, differed:** both changes on the tablet (debug build; the system's blue
  palette shows). The recreation check couldn't be made there: Lenovo's ZUI has wallpaper
  colours off (`ThemeOverlayController: … monet switch close`, no colour overlay enabled,
  `system_accent1_*` fixed), so a set (ticking Earth set its picture) changed no colour and the
  activity stayed the same record. Moved to WP8 on the FP6 (LineageOS has wallpaper colours).

### [x] U2. The caption (Opus, medium): findings 3, 5, 10

Files: `MainActivity.kt` (`Picture`), `CLAUDE.md`.
- **Chip (3):** a small "On wallpaper" chip at the picture's top-left (`Alignment.TopStart`,
  the dark translucent style of ⛶ and ★), shown when `onWallpaper` is true (already only with
  two or more sources). Remove `" · Wallpaper"` from the date line, so it isn't said twice. It's
  a `Text`, so screen readers read it.
- **Source line (5):** with two or more sources, a small line above the title:
  "NASA Image of the Day · 17 October 2026" (`Source.label`, `onSurfaceVariant`, `bodyMedium`);
  the date line below the title goes (video days: "… · Video"). With one source (APOD alone, no
  tabs) the caption stays as 0.4.0 has it: title, then date, so existing users see no change.
  Tabs stay "APOD | NASA | Earth". The long press on the title row (copies the link) stays on
  the whole block.
- **Explanation (10), replaces the ▾ (user, 2026-10-02 → 2026-10-17):** under the title the first
  two lines (`maxLines = 2`, ellipsis) with "more" while it's cut off (`onTextLayout`,
  `hasVisualOverflow`); no "more" and nothing to expand for a short one (Earth's one sentence).
  Tap expands, tap on the full text hides it, as today. Remove the ▾/▴ `IconButton`. Screen
  readers: `onClickLabel` "Show explanation" / "Hide explanation" on the text. Long press on the
  explanation still copies it, on the title still the link. `rememberSaveable` for `expanded`
  stays.
- Check on the tablet: a NASA tab with a long explanation, an Earth tab with a short one, APOD
  alone (no change), an APOD video day with a video-day source (tabs, source line, no ★/⛶).
- Done when: the three changes are on the tablet and `CLAUDE.md` (The app, Explanation and Tabs
  paragraphs) describes them.
- **Done 2026-10-18** (debug build on the tablet, today a real APOD video day): Earth tab (chip,
  source line, one-sentence explanation without "more"), NASA tab (its explanation fits two lines
  at the tablet's width: no "more"; a cut-off one was checked on APOD's), APOD's video tab
  (source line "… · Video", preview with "more", tap expands, tap hides, long press on the title
  copies the link and on the explanation its text), APOD alone (title, then date; no tabs, no
  chip), APOD alone with NASA for video days (tabs; the chip moved to NASA's tab after its set).
  Differed: "more" is a separate line under the preview, not inline (simpler); a tap on the title
  still toggles too; the chip doesn't show on the ▶ tile. Not checked: TalkBack reading the
  labels, a NASA explanation long enough to cut off on the tablet.

### [x] U3. Source chips and sections (Opus, medium): finding 9

Files: `MainActivity.kt` (`Settings`, `Tick`), `CLAUDE.md`. This package changes only the
widgets and the grouping; the video-day part moves in U4.
- **Chips:** replace the three `Tick` rows (full names, 72 dp each) by one row of `FilterChip`s
  ("APOD", "NASA", "Earth": `Source.tabLabel`), multi-select as `tick(source)` is today. The
  last selected chip can't be turned off (`enabled = false` on it, as the tick is now). Three
  short chips must fit on a 360 dp phone: `Row` with `weight(1f)`, or `FlowRow` if not. The
  full names stay in the status line, the problem box and the source line (U2). Remove `Tick` if
  nothing else uses it (`Choice` stays).
- **Two sections:** "Wallpaper" (the switch, the status line, "Wallpaper on", and from U4 the
  set button) and "Pictures" (the chips, "Change every", from U4 "If APOD is a video"). A
  `HorizontalDivider` and a larger gap between the two; the sub-headings stay as `Heading`.
  Proposed wording, placeholder (the user decides on the tablet): "Wallpaper on" becomes
  "Where"; the second section opens with the chips under "Pictures from" as now (no new heading
  words; if that looks redundant, say so).
- The weekday dialog fires from `tick()` as now; no change.
- Check: tick and untick each source with the switch on and off, the last chip stays on, the
  dialog still shows, the page reflows without a jump of the picture; the three chips at font
  scale 2.0 and `wm density 301`.
- Done when: the chips and sections are on the tablet and `CLAUDE.md` (The app, Settings: "a
  tick box per source" → chips; 2026-10-17) is updated.
- **Done 2026-10-18** (debug build on the tablet): chips in one `Row` with `weight(1f)` fit at
  font scale 2.0 and `wm density 301`; ticking and unticking with the switch on and off, the
  last chip greyed out, the weekday dialog on ticking NASA. Differed: a ✓ glyph on selected
  chips (as the segmented rows mark theirs); the set button moved into "Wallpaper" now (still
  only while the switch is off; U4 shows it always); "Wallpaper" is a heading of the same style
  as the sub-headings, "Pictures from" opens the second section, no "Pictures" heading (it would
  repeat). `README.md` and the `features.md` mock-up ("Wallpaper on", "tick") are left for U7.
  Not checked: TalkBack on the chips.

### [x] U4. Video-day section and set button (Opus, high): findings 2, 4

Files: `MainActivity.kt` (`Settings`, `Choice`, `setWallpaperNow`, `applyNow`'s doc),
`CLAUDE.md`. Two behaviour changes; both overturn something in `CLAUDE.md`, so write the why
there (user, 2026-10-17).
- **"If APOD is a video" always visible (2):** the heading "On video days" becomes "If APOD is a
  video" (placeholder). Shown always, not only while `apodAlone`. With two or more ticked the
  radios are disabled (greyed, the selection stays as stored so unticking back restores it) with
  one grey line under the heading: "The next source takes its turn." (placeholder; the one help
  text this adds, approved with this decision). `Store.wallpaperFile()` already ignores
  `videoDays` with turns; don't change it. `Choice` needs `enabled = false` support. This
  removes the layout jump when ticking a second source.
- **"Set as wallpaper now" always visible (4):** remove `if (!daily)` around the button in
  `Settings()`; it sets the shown tab's picture (`setWallpaperNow()` as it is) and starts that
  source's turn (`store.setWallpaper` → `startTurn`; check this still holds with the switch on).
  Place it at the end of the "Wallpaper" section (U3). Update the comments that say it's only
  there while the switch is off (`applyNow`'s doc) and `CLAUDE.md`, Settings ("Only while the
  switch is off", 2026-10-16, reversed 2026-10-17: with tabs and turns the app no longer picks
  the picture alone, so the user needs a way to set the one they're looking at).
- **Check on the tablet:** switch on, NASA tab shown, tap the button → NASA is set, the status
  line says "· NASA Image of the Day", the chip (U2) moves to NASA's tab, the turn starts; on
  APOD's video tab with turns the toast says "Video day: wallpaper kept" (as `setWallpaperNow()`
  does). Switch off: unchanged. Video-day radios: APOD alone → all enabled, two ticked → greyed.
- Done when: both changes are on the tablet with the checks above and `CLAUDE.md` has both
  reversals with their why.
- **Done 2026-10-18** (debug build on the tablet, a real APOD video day, switch on, 3 h): the
  button on Earth's tab set Earth (status line "· Earth Observatory", the chip on Earth's tab,
  `turnStartedAt` fresh; a `run -f` right after kept Earth), on NASA's tab NASA; on APOD's video
  tab "Video day: wallpaper kept" and nothing set. The video-day radios greyed with two or
  three ticked (a tap on one changed nothing), enabled with APOD alone, "Keep" still selected.
  Switch off: the button stays. Differed: "Pick picture" is greyed too with turns; `README.md`
  and `full_description.txt` say the button sets the picture shown (their "shown while the daily
  change is off" was wrong now). Still a jump at a second tick: "Change every" and the grey line
  appear (the section itself no longer does). Not checked: TalkBack on the greyed radios.

### [x] U5. Notice boxes (Sonnet, medium): finding 6

Files: `MainActivity.kt` (`NoticeBox` and its three callers), `CLAUDE.md`.
- `NoticeBox` takes the kind: the **problem** box keeps the orange fill and border (a failing app
  must look failing, `CLAUDE.md`, Problems); the **video notice** and the **battery hint** become
  neutral: `surfaceVariant` fill, `outline` border (theme colours, so they follow U1). Same
  shape, padding and buttons as now.
- The order stays problem, video notice, battery hint, between the picture and the title
  (user, 2026-10-16). Don't move any box.
- Check on the tablet: a problem box (offline, pull to refresh), the battery hint, the video
  notice on a debug-faked video day (as WP3/WP4 did, reverted). Look at the colours with U1's
  dynamic scheme: the neutral boxes must stand out against the page.
- Done when: the three boxes show as described on the tablet and `CLAUDE.md` (The app, Problems
  and Video days: "translucent orange" → orange for problems only) is updated.
- **Done 2026-10-18** (debug build on the phone HA24BZ2Y, NASA tab, dynamic scheme): `NoticeBox`
  has `problem: Boolean = false`; only the problem call passes true. Seen: the orange problem box
  (Wi-Fi off, pull to refresh) above the grey battery hint, and the hint alone: both stand out
  against the page. Not checked: the video notice on screen (no video day today; same code path
  as the hint), a light-on-dark check of other wallpaper colours, TalkBack.

### [x] U6. Two panes on wide screens (Opus, high): finding 12

The last code change before the verification passes. Files: `MainActivity.kt` (`Page`),
`CLAUDE.md`.
- At 600 dp width or more (`BoxWithConstraints` or `LocalConfiguration`), `Page()` becomes a
  `Row`: left the picture side (tabs, picture, notices, caption), right the settings; each
  scrolls on its own. Below 600 dp the single column of today. About 55/45; the picture side's
  width decides the picture's size, so a landscape phone's picture isn't taller than the screen.
- Must keep working: pull to refresh (it needs a scrolling child; put it around the picture
  side, and say if it can't work with both panes), the crop frame's drag, the weekday and
  pause-hint dialogs, `BackHandler` for crop mode, state across rotation (`tab` and, from WP7e,
  `weekdayDialog` are saved, the rest reloads in `onResume`).
- Check on the tablet in portrait and landscape (rotate with `adb shell settings put system
  accelerometer_rotation 0` and `user_rotation`; note both first and **put them back after**).
  Say if the tablet's width is below 600 dp in portrait (then only landscape shows two panes)
  and that the FP6 in landscape is WP8's.
- Done when: both layouts work on the tablet with everything in the "must keep working" list,
  and `CLAUDE.md` (The app, "One page" paragraph: two panes ≥ 600 dp, 2026-10-17) is updated.
- **Done 2026-10-18** (debug build on the tablet, a real APOD video day, APOD+NASA at 3 h):
  `BoxWithConstraints` in `Page()`; at 600 dp or more a `Row` of a `PullToRefreshBox` (weight
  0.55: tabs, picture) and a scrolling settings column (0.45) split by a `VerticalDivider`. The
  tablet is 711 dp wide in portrait, so it shows two panes in portrait and landscape; the single
  column checked at `wm density 301` (425 dp). Seen in both orientations: each pane scrolls on
  its own, pull on the picture side refreshes (spinner), on the settings side nothing; the crop
  frame drags (position stored) and Back closes it; the tab and the weekday dialog survive a
  rotation; the pause-hint dialog opens (Cancel). No crash. Differed: nothing. Noted: in the
  tablet's portrait the settings pane is 320 dp, so "Home screen" and "Lock screen" wrap to two
  lines; a portrait-shaped picture in landscape is still taller than the screen (the width
  decides, as planned). Not checked: the FP6 in landscape (WP8), TalkBack. Left on the tablet:
  `nasa.cropPosition` 0.27 (was 0.92) from the drag test; rotation (0, 0) and density (180)
  as found.

### [x] U7. Docs, store screenshots, the tablet pass on the final tree (Opus, high)

The verification pass on the tablet, after the last code change (U6), then the docs. Files:
`CLAUDE.md`, `README.md`, the skill (`SKILL.md`, "Still untested"), fastlane texts if a label
changed.
- **The build check** on the final tree: `:core:test`, `lintRelease`, no `w:` lines, two clean
  release builds with the same sha256 (reproducible; WPR's `9bfbb5fc…9122` is of the tree before
  WP7e). The R8 build signed with the debug key for the tablet (skill, Working on the phone;
  `private/wp7-handoff/apodroid-r8-debugkey.apk` is WP7's, rebuild it).
- **WP7's tablet pass again, on the final page** (debug and R8 builds): its page items, and the
  device checks of WP7a–7g and U1–U6 that touch the page (U1: whether a set recreates the page
  once dynamic colour changes) (★ across a tab switch and a rotation,
  the weekday dialog across a rotation, the box following a job failure, the storage text, a
  stopped job, the chips, the set button with the switch on, the two panes). A NASA tab with
  nothing stored (offline, `nasa.*` removed). Say what changed against what WP7 covered. The
  phone pass (WP8) follows on this page.
- `CLAUDE.md`: every package's decision is in already; read it end to end and remove
  contradictions (e.g. The app, "One page": "the settings below it"; the Releases entry for
  0.5.0 with what the UI work added; the "placeholder" marks on texts the user has approved).
  `README.md`: the settings list (labels, chips). Fastlane texts only if a label shown in them
  changed (changelog 9 stays within 500 characters).
- `fastlane/metadata/android/en-US/images/phoneScreenshots/` show the old page: **the user takes
  new ones** on the phone after WP8 (don't touch the FP6); list what to capture: the page with
  tabs (NASA tab, "On wallpaper" chip), the explanation expanded, the settings with the chips.
- Then restore the tablet (postponed from WP7, user 2026-10-17): the 0.5.0 debug build of the
  final tree with `private/wp7-handoff/backup/` written back by `run-as` (APOD+NASA at 12 h,
  `weekdayNoticeHidden` true; `nasa.savedUri` points at the tablet's own 1408 in the gallery,
  keep it), `dumpsys deviceidle whitelist +io.github.buerlino.apodroid` (the uninstall for WP7's
  0.4.0 test dropped it), screen off. Its system settings as found before WP7: `time_12_24` 24,
  `font_scale` 1.1, density 180 (`wm density reset`), `http_proxy` null, `screen_off_timeout`
  2147483647, `stay_on_while_plugged_in` 7; check them after the packages' own tests.
- Done when: the pass is reported item by item (tested, untested, failed; a failure goes back
  to its package before WP8), the docs read as current facts, the skill's "Still untested" is
  updated, and the tablet is restored.
- **Done 2026-10-18** (the tablet, debug and R8 builds of the final tree). One failure, fixed
  here (WP7g's code): a `SocketTimeoutException` is an `InterruptedIOException`, so a read timeout
  was taken for a stopped job: no problem stored (the box didn't follow the job, WP7e item 2) and
  the run ended before the other sources and the set. `Store.isStop()`; then the box followed
  the job's failure and cleared on the next run. Passed: WP7e (the dialog across rotations on
  R8, the box following the job, the "hasn't loaded yet" toast, "11:19 AM" on one line at font
  scale 2.0 and 425 dp), WP7f (a full disk: the storage text, no `.part`, the picture kept;
  after freeing, stored and cleared), WP7g (a stop ends the run within a second; the retry came
  6½ min later, batched, and downloaded afresh; `turnStartedAt` an hour ahead → Earth set at
  once, the turn fresh, held on the next run), a NASA tab with nothing stored (the problem text
  with "Try again", the set button greyed; loaded after it), the video notice on screen. On R8:
  switch on with "my picture", ticking and unticking (the source on the wallpaper unticked →
  the next one set), "Don't show again" in the weekday dialog, tabs, chip, source line,
  explanation expand and hide, the link copy, the set button per tab with the switch on, ⛶ with
  drag, ✓ and Back, ★ (the original, 2.5 MB) through a tab switch and a rotation ("Still
  saving", ★ filled after), the problem box offline and cleared online, a job run, two panes in
  both orientations and one column at 425 dp, no crash. Found, and fixed for 0.5.0 (user,
  2026-10-18): landscape (WP7e item 5) showed the portrait part's middle band zoomed; the crop
  hint was widened evenly for landscape on Android 15+ (`wallpaperHint`). Checked on debug and
  R8 builds: the frame centred, dragged left and right, the hint centred on it (limited by the
  nearer edge), portrait the chosen part as before, landscape the picture's full height.
  **Dropped again on the phone (WP8a): portrait depends on the launcher.** The "On wallpaper" chip sat inside the crop frame: now
  hidden in crop mode, back after it (both builds). The placeholder texts approved by the user.
  Not checked: U1's recreation (WP8), TalkBack, the bad picture (WP7c) again. Build: 48 tests,
  `lintRelease` clean, no `w:` lines, two clean release builds alike (`1a450e81…9882`). The
  tablet restored (backup written back, 12 h job, whitelisted, settings as found; the test save
  deleted); its wallpaper is the last test's NASA picture until the job's next run.

### [x] WPG. Release gate (Fable, high; 2026-10-18)

The final check before the tag: the static audit, then the start of WP8 on the FP6. Findings
and decisions are in `CLAUDE.md` (Wallpaper; Open questions 1; Releases) and the skill ("Still
untested").
- **Static audit, all clean:** 48 `:core` tests green (`readsEveryRealPost` ran against
  `private/`), `lintRelease` clean, no `w:` lines, two clean release builds with the same
  sha256 (`1a450e81…9882`, U7's). The three live endpoints answered as documented (18 Oct 2026:
  APOD a video day with an iframe in the hero and a `featured_image`, NASA's feed 60 items
  with the 16 Oct post, Earth's 16 Oct post); the parsers read all three. Manifest, R8 setup and
  the workflows unchanged in what matters (no new permission, `dependenciesInfo` off, the
  version check in `release.yml`); `version-control-info.textproto` holds the commit and
  `$PROJECT_DIR`, not the path, so the tag's builds match. Docs: version 0.5.0 / 9, changelog 9
  at 499 characters, labels as on the page; no stale placeholder marks.
- **The diff, read for races, turns, migrations, errors and keys:** one defect, WP8b. Noted,
  not defects: a transient non-image (an HTML page with 200) sets `badUrl` and that post is
  never downloaded again (by design, user 2026-10-17); with "Day" a turn can last 18–36 h, as
  the 6 h job's runs fall anywhere in their windows (accepted, WP5).
- **The phone:** the FP6 already had this tree's release-signed 0.5.0 (installed by the user at
  14:08, byte-identical to the fresh build outside `META-INF`), so the update over 0.4.0
  couldn't be watched, only its result (WP8, Done so far). Found there: the widened crop hint
  shifted the user's own picture left (WP8a).

### [x] WP8a. Revert the widened crop hint (done in WPG, user 2026-10-18)

`wallpaperHint` (U7) widened the hint for landscape; on the FP6 the home screen then showed the
left edge of the user's own picture, as the launcher (Niagara, `bitpit.launcher`) reports
`mWallpaperX=0.0` and Android shows a wider-than-screen hint at the launcher's offset. One crop
rectangle per set is all a normal app gets (`setStreamWithCrops` is a system API), so portrait
can't be made right on every launcher with a wider hint. Reverted: `Frame.kt` and `FrameTest.kt`
back to the committed version, `wallpaperCrop` removed from `Store.kt` (the crop is
`wallpaperFrame` again, as in 0.4.0). The reason is in `CLAUDE.md`, Wallpaper, as tested and
rejected. After the install the user's own picture needs one "Set as wallpaper now" (or a
"Where" change) to be centred again: the stored `wallpaperDate` says it's set. Build after
WP8a and WP8b: 45 tests green (the three hint tests gone), `lintRelease` clean, no `w:` lines,
two clean release builds alike (`36368950…e7ac`); `app/build/apodroid-aligned.apk` is that
build zipaligned, for the user to sign (skill, Working on the phone).

### [x] WP8b. A stop during a rendition download ends the run (done in WPG)

`download()` throws `InterruptedIOException` after `Thread.interrupted()`, which clears the
flag; `downloadPicture` caught it as an `IOException` and, for APOD and Earth, fell through to
the plain 1280 px picture, downloaded it whole (no flag left to stop it), stored it and set it,
and `update()`'s flag checks passed too. Now `e.isStop()` rethrows it, so the run ends as WP7g
meant (the `isStop()` chain through `checkSite` and `update` works by type, not the flag).
Found by reading; not reproducible on a device on purpose (a stop can't be timed into a 1 s
download). No test: `downloadPicture` is in `:app`.

### [x] WP8. Complete verification pass on the phone (Opus, high; done 2026-10-18)

(User, 2026-10-17: really see that everything works as intended.) The FP6 is the user's own
phone with their settings and APODroid 0.4.0 from Obtainium: the R8 release build signed with
the release key goes over it with `adb install -r` (skill, Working on the phone: the user signs
it in their own terminal). `run-as` doesn't work on a release build, so nothing is faked in the
prefs: only real states, adb for the jobs (`dumpsys jobscheduler`, `run -f`), screenshots,
`uiautomator dump` and logcat. Before starting, screenshot the user's settings and note every
system setting changed (font scale, …) to put it back; ask the user before anything that
changes their wallpaper for long, saves to their gallery, or needs their hands (airplane mode,
TalkBack). Report each item as tested, untested or failed, with screenshots where the page
matters. A failure goes back to its package, then U7's pass and this one again for what changed.
- The update: the settings, "my picture", the crop and the wallpaper as before; with APOD alone
  the page is 0.4.0's plus the source chips and U1–U6's changes, and nothing changes until a
  second source is ticked.
- The page, per tab: picture, the "On wallpaper" chip, source line, title, the explanation
  preview and "more", the long presses, ⛶ with ✓ and Back, ★ (the original; ask before saving),
  tap opens the page, pull to refresh, "Set as wallpaper now" with the switch on (U4), the
  Material You colours (U1).
- Sources: each chip, the last one can't be turned off, the weekday dialog, the tabs following
  the chips, "If APOD is a video" greyed with two or more (U4).
- Turns (WP5) at 3 h for a few hours of real use: one change per turn, in order, the job's
  period in `dumpsys jobscheduler`, the status line.
- Android 16's flexible job constraints (from the WP5b check, 2026-10-17): in `dumpsys
  jobscheduler`, `Num Required Flexible constraints` for the job. 0 on the tablet (Android 15).
  If it isn't 0 on the FP6, Android holds the job back until charging or idle: turns on battery
  would come late; report how late and ask the user.
- Video days with APOD alone (only if one comes; else untested): each choice, the notice.
- Problems: offline (airplane mode, by the user) with the box naming the source, the empty
  state; the battery hint and its "Don't show again" dialog only if the user wants it hidden.
- Font scale 2.0 at the phone's width: "Change every" (four segments), the source
  chips, the tabs and the 12-hour status line (WP7e). Also landscape (two panes if the phone is
  600 dp wide there, U6).
- The sharper wallpaper (WP2): the home and lock screen look sharp against a 0.4.0 screenshot;
  the download size in logcat or the file size.
- No crash: `adb logcat -d | grep 'E AndroidRuntime'` empty, `dumpsys activity exit-info`.
- The audit fixes (WP7a–7g), as far as the phone shows them without `run-as`: ★ with a tab
  switch during the save and a rotation during it (ask before saving), the weekday dialog
  across a rotation, landscape (the page, and the home screen with a wide picture: its full
  height, the portrait part unchanged; U7's fix on Android 16). The 1 h rule, the bad picture, the storage text and the stopped
  job are the tablet's (WP7c, 7f, 7g, U7).
- At the end: the system settings as found, the app's settings as the user wants them to stay.
  Update the skill's "Still untested".
- Done when: every item is reported, no failure is open, and the user has seen the report.
- **Done so far (WPG, 2026-10-18, on the build before WP8a/8b; those changed only the crop
  rectangle and the stop path, so these stand):** the update over 0.4.0 kept the settings
  (switch on, Both, APOD alone, "Use my picture" with a picture; the page as 0.4.0's plus the
  chips and U1–U6); `Num Required Flexible constraints: 0` on Android 16 (the single-source
  job: 6 h / 6 h flex, persisted, whitelisted, standby bucket exempted); two panes in landscape
  (828 dp wide); the real video day 18 Oct with "Use my picture" (the notice "…so your own
  picture is the wallpaper. Tap ▶ …", "Wallpaper changed 18 Oct, 14:11") and "Keep" (the notice
  "…stays as it was…", "Video day, wallpaper kept", the log "video, wallpaper kept", nothing
  set); back on "Use my picture" the own picture was set at once (14:23); the explanation
  preview with "more" (video tile, no ⛶/★); no `E AndroidRuntime`, exit-info shows only
  PACKAGE UPDATED and a removed task. Screenshots in Claude's scratchpad only. Not done:
  NASA/Earth as the video-day choice, chips, dialog, tabs, turns, font scale, offline, ★, the
  sharpness and the home screen in landscape.
- **Done on the rebuilt APK (2026-10-18 afternoon,** release-signed by the user, installed 14:31,
  sha256 `aedc4054…c386` the same as `app/build/apodroid-v0.5.0.apk`):
  1. "Set as wallpaper now" on the APOD tab set the own picture centred: `mCropHint` 228–732 ×
     1–1123 of the 960×1123 picture (WP8a works).
  2. Video-day choice NASA, then Earth (real video day): the tab came, the picture loaded and
     was set at once ("Page: NASA 2026-10-16, wallpaper set"), the notice ("…so NASA's Image
     of the Day is the wallpaper.", "Tap ▶" only on APOD's tab), the status line "… · NASA
     Image of the Day", the chip. Crops centred on the renditions' full height (NASA 1305–2421
     of 3726×2484, Earth 2425–3541 of 5966×2484: the sharp wallpaper). Home screen in portrait
     the picture's middle. Back on "Use my picture": set at once, centred, tabs gone.
  3. Earth tab: ⛶ drag + Back sets nothing (the drag end stores the position, as since 0.3.0),
     ⛶ + ✓ sets the moved crop; the chip hidden in crop mode; long presses copy the link and
     the explanation (Android 16's own preview); "more" expands and collapses; ▶ opens the
     browser. Pull to refresh fetches (8.3 KB received, 0 idle; nothing set for the same post).
     Chips: APOD's checked and disabled when alone; NASA ticked → the weekday dialog, which
     survives a rotation (two panes in landscape), "Change every" shows, "If APOD is a video"
     greyed with "The next source takes its turn."; Earth ticked: NASA's turn kept, no set, no
     video notice with turns. Job: Day with turns 6 h / 18 min flex, 3 h → 3 h / 15 min,
     `Num Required Flexible constraints: 0`. 12-hour status line "2:38 PM" fits at 2.0.
     ★ on NASA's tab saved the original (6000×4000, 2.5 MB), a second tap "Already saved", ★
     empty again after `content delete`; the save took < 0.3 s on Wi-Fi, so the tab switch and
     rotation *during* a save weren't reached (tablet, WP7b). Offline (airplane mode by the
     user): a page open no box (all checks current or within the hour), a pull the orange box
     "Astronomy Picture of the Day: Couldn't reach NASA's site. Check the connection." with
     Dismiss only, picture kept; Dismiss hides it, a second failed pull keeps it hidden; online
     again a pull clears it. No `E AndroidRuntime`; exit-info only PACKAGE UPDATED.
  - **Found:** (a) at font scale 1.5 and 2.0 the ticked source chips wrap mid-word ("APO/D",
    "NAS/A"; 1.3 fits): the FP6 is 372 dp wide (1116 px at 480 dpi), narrower than the 425 dp
    the tablet check assumed. (b) In landscape at font scale 1.0 the settings pane is about
    324 dp: "Home screen" wraps to two lines and its segment is taller than the other two (on
    the tablet's 320 dp pane both labels wrapped, so the row stayed even; U6 noted it).
  - **Fixed (user, 2026-10-18, WP8c; U3's chips and U6's pane):** (a) the chips in `EqualGrid`
    (a `Layout`: equal widths, as many on a line as the widest chip's max intrinsic width fits;
    a `FlowRow` with weights was tried first and still broke at 1.5, as it breaks lines by each
    chip's own width, then splits a line equally); (b) the three segmented rows
    `height(IntrinsicSize.Min)`, each button `fillMaxHeight()`. Checked on the tablet's debug
    build at the FP6's width (`wm density 344`): chips on one line at 1.0 and 1.3, two per
    line at 1.5 and 2.0, a chip still ticks and unticks; "Where" in two panes at density 170 and
    165 with font 1.0 (only "Home screen" wraps, as on the FP6) and at the tablet's own 180/1.1
    (both wrap): one height. Tablet restored (density 180, font 1.1, sources APOD,NASA), no
    crash. Build: 45 tests green, lint clean, no `w:` lines, two clean release builds alike
    (`bc160dfb…c108`), zipaligned to `app/build/apodroid-aligned.apk` for the user to sign.
    On the FP6 (release-signed, installed 17:55, sha256 `6bd7a3ac…`): chips one line at 1.0,
    two per line at 1.5 and 2.0; "Where" in landscape one height with "Home screen" wrapped; no
    crash.
  - **The turns over hours: not observed.** The FP6 was unplugged from about 15:15, and at
    17:27:16 the app was uninstalled and installed afresh (new app id 10344,
    `firstInstallTime` 17:27:16), which cleared its data: the settings, "my picture", the turn
    and the battery exemption. So whether the 17:24–17:39 run set Earth is unknown; the
    wallpaper at 17:55 had `mCropHint` 0–1116 of a 2484 px tall picture (Earth's at the
    position 0 left by the ⛶ test, so probably yes, unconfirmed). Not rerun (user, 2026-10-18):
    the tablet's turns are enough (WP5c, U7).
  - **Can't be tested here:** dynamic colour after a set (LineageOS has Monet off:
    `mIsMonetEnabled=false`, no wallpaper colours extracted); the home screen in landscape
    (Niagara stays portrait); the empty state (needs cleared data); `dumpsys diskstats` (only
    refreshed by an idle job).
- **Untested:** a tab switch or rotation during a ★ save (it took < 0.3 s; the tablet's, WP7b),
  TalkBack, the sharpness against a 0.4.0 screenshot by eye (the hint's full rendition height
  stands for it).
- **At the end:** system settings as found at the afternoon's start (`font_scale` 1.0,
  `user_rotation` 0, `accelerometer_rotation` 0, `time_12_24` 24, `screen_off_timeout` 600000,
  no proxy). The app's settings were lost in the 17:27 reinstall: the user puts them back
  ("Use my picture" with their picture, battery Unrestricted, "Pause app if unused" off).

### [x] WP9. Commit and tag (Sonnet, low)

Only when the user says so: commit, tag `v0.5.0`. The user pushes `master`, then the tag
(skill, Releasing: the Release workflow must succeed, F-Droid's bot finds the tag), then the
fdroiddata branch (WP6, amended in WP7e). After the release the user posts the reply to the
GitHub issue (drafted in WP6): "Thanks for the idea! 0.5.0 adds both: tick NASA Image of the
Day and Earth Observatory under "Pictures from". With one ticked besides APOD they can also fill
in on APOD's video days; with two or more they take turns every day, 12, 6 or 3 hours, with a
tab per source on the page. NASA History's Image of the Day is on Flickr, so it's left out for
now." The user takes the store screenshots (U7's list) and commits them when they like.
- Done when: the tag is pushed, the Release workflow has attached `apodroid-v0.5.0.apk`, and
  `CLAUDE.md`'s Releases line has the date.
