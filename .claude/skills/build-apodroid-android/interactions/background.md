# APODroid: background behaviour as a decision tree

> Read at 7299474 (15 Nov 2026), checked against 3a4db15 (16 Nov: 0.8.0); line numbers are of 3a4db15.
> The SUSPECTs below were fixed or accepted since (README.md): their quoted code is 7299474's, the fixed
> outcome is in the leaf or the test list. Newer changes: README, Keeping it current.

Read-only map for a phone tester. Sources: CLAUDE.md ("Background update", "Turns and the picture
time", "Traffic", "Wallpaper", "Problems and status lines"), `DailyJob.kt`, `AlarmReceiver.kt`,
`KeepRunning.kt`, `LiveWallpaper.kt`, `Store.kt`, `:core`'s `PictureTime.kt`, `Screens.kt`,
`Turn.kt`, `JobRuns.kt`, `Detector.kt`, `Live.kt`, `Saving.kt`, `PendingStarts.kt`. Every log line
below has the tag `APODroid` (`adb logcat -d -s APODroid`). Line numbers are of 3a4db15.

**Counts:** 105 leaves (A 10, B 7, C 7, D 9, E 7, F 13, G 14, H 7, I 8, J 7, K 5, L 3, M 7, N 1);
**3 SUSPECTs** (G14, J6, N1; N1 and J6 fixed since); 0.8.0 added E1b, E3b, E8, E8a, F14–F16; 0.9.0
(SH2b) E9, E9a; 26 phone cases in the list at the end (P18–P22 0.8.0's, P23–P26 0.9.0's).

## The phone (FP6, LineageOS, Android 16, release build of 7299474)

- More NASA pictures on; columns home = [NASA], lock = [APOD]: **one source per screen**, so each
  screen changes only at "New picture at" (assume 06:00; read Setup's summary "… · 06:00"),
  rule 2's window 06:00–09:00 unless "As soon as it's out" is on (read Setup; it isn't in the
  summary). The columns differ: each screen has its own record and its own sets.
- Size Live: home's set goes to the engine (hand-over), the lock screen's is a still `FLAG_LOCK`
  set. With different columns the picker put the engine on "Home screen" only, so the lock
  screen has its own wallpaper and `liveShared` is false (`getWallpaperId(FLAG_LOCK)` ≠ -1).
- Today, Sunday 15 Nov 2026: NASA's newest post is Friday's (none at weekends), APOD's is today's
  (out 00:05 US Eastern = 06:05 CET). Zone presumably Europe/Zurich (UTC+1): 06:00 local = 05:00Z.
- Not readable: the prefs (no run-as). Everything is judged from logcat, `dumpsys` and the page.

### Reading the state without the prefs

| What | Command | Look for |
|---|---|---|
| The pair of alarms | `adb shell dumpsys alarm \| grep -B2 -A8 'buerlino.apodroid'` | two `RTC_WAKEUP` alarms for `io.github.buerlino.apodroid.RUN` (codes 1, 2) at the same `origWhen`, one with `window=+10m0s` |
| Jobs | `adb shell dumpsys jobscheduler io.github.buerlino.apodroid` | job `/1`: periodic `+6h0m0s` flex `+6h0m0s`, persisted, CONNECTIVITY; job `/2` only while pending (expedited) |
| Live | `adb shell dumpsys wallpaper` | home (system) component `io.github.buerlino.apodroid/…LiveWallpaper` and its id; lock: an ImageWallpaper with its own id |
| Keep running | `adb shell dumpsys activity services io.github.buerlino.apodroid` | `…/.KeepRunning` with `isForeground=true` (only while on) |
| Battery | `adb shell dumpsys deviceidle whitelist \| grep apodroid`; `adb shell am get-standby-bucket io.github.buerlino.apodroid` | user whitelist = Unrestricted; bucket |
| The process | `adb shell pidof io.github.buerlino.apodroid` | a new pid after a death |
| Clock and zone | `adb shell date; adb shell getprop persist.sys.timezone` | (read only, never set) |

The device's offset (0–10 min, drawn once) shows in the first `Daily job: alarms for …` after a
good run: that time minus the next change time (e.g. `2026-11-16T05:04:37Z` → 4 min 37 s for
06:00 CET). Write it down; the expectations below use `+off`.

---

## A. Arming the alarms (`DailyJob.ensure`, `arm`, `Store.nextAlarm`)

`ensure` = `checkAlarms` (first arming in a process only, J) → `arm(nextAlarm())` → the fallback
job 1 scheduled unless a run goes or it's there with 6 h / 6 h (`DailyJob.kt:163-172`); since N1's
fix all under the job's `lock`, `daily` read again there (off: nothing). Every
`arm` logs `Daily job: alarms for <instant>` and notes the boot and install (`noteArmed`).

- **A1 Page open (onResume), daily on.** Pre: daily on. Steps: open the page (or bring it to the
  front). Expect: `Daily job: alarms for T` with T per B; `dumpsys alarm` shows the pair at T.
  Phone: yes.
- **A2 Page open, daily off.** No `ensure`, no log; `KeepRunning.ensure` stops the service.
  Phone: only inside L1/P1.
- **A3 A run's start (`started`, under `lock`).** Unless an off came since or daily is off:
  `failedRuns`+1, `lastRunAt`=now (one commit), the pair at now + `runRetry` (15 min with
  `failedRuns` now 1, 1 h when ≥ 2): `Daily job: alarms for <start+15m>` right after `Daily job:
  job N starts…`. Phone: yes (any forced run).
- **A4 A run's end (`ended`, after `jobFinished`).** `ensure` again: after a good run by the
  clock (or now for a check passed during the run, B5); after a failed/stopped run
  min(clock, start + 15 min / 1 h). Second `Daily job: alarms for …` after `Daily job:
  jobFinished (job N)`. Phone: yes.
- **A5 `BOOT_COMPLETED`.** `Daily job: android.intent.action.BOOT_COMPLETED, arming again`, then
  `KeepRunning.ensure`. On Android 15+ also when the app leaves the stopped state (after a force
  stop). Phone: only with the user's OK for a reboot; else seen after G6/P17's force stop.
- **A6 `TIMEZONE_CHANGED` / `TIME_SET`.** `Daily job: android.intent.action.TIME_SET, arming
  again`. Phone: not by the tester (no clock changes); an NTP step may log it by itself, look
  in the log.
- **A7 `MY_PACKAGE_REPLACED`.** Arms and starts Keep running. Phone: no (no install). P passed
  on 0.6.0.
- **A8 Any broadcast with daily off.** Returns at `AlarmReceiver.kt:24`; nothing logged.
- **A9 The fallback job 1.** Scheduled by `ensure` when missing (hibernation, force stop, off/on;
  "scheduling runs it at once") or with other timing; never while a run goes (`runs.going`,
  `DailyJob.kt:167`). Phone: `dumpsys jobscheduler` shows job 1 periodic 6 h, flex 6 h,
  persisted.
- **A10 A failed fallback schedule.** `ensure` false: the page logs `Couldn't schedule the daily
  job again`, the run's end `Daily job: couldn't schedule the fallback after the run`, Set/switch
  toast "Couldn't schedule the daily change". Phone: no (can't make `schedule()` fail).

## B. When the next alarm is (`:core` `nextRun`, `nextAlarm`; `PictureTime.kt:159-218`)

- **B1 One source per column (the phone).** From the current change time C (the last pictureAt
  ≤ now − off): C, C+15m, C+1h15, C+2h15, C+3h (window end), next C, each +off; the first after
  now. Sunday afternoon, soon off: `Daily job: alarms for 2026-11-16T05:0x:xxZ` (06:00 CET +off).
  Phone: yes. Unit: `PictureTimeTest.theChecksAfterAChangeTime`, `zurichInWinterGetsApodAt0615`.
- **B2 A column with turns.** The same for every change time pictureAt + k × interval
  (`hasTurns` is any column ≥ 2; both screens use the union). Not on the phone. Unit:
  `turnsAreNotCheckedAfterTheWindow`, `aMondayInZurichEvery6HoursIsAPlainRotation`.
- **B3 "As soon as it's out" on.** Hourly from C+15m until the next change time (+off). Phone:
  only if the user has it on (then the next alarm is within the hour). Unit:
  `allDayChecksHourlyUntilTheNextChangeTime`.
- **B4 Failed runs in a row (`failedRuns` ≥ 1).** min(clock, max(lastRunAt + 15 min (1 run) or
  1 h (≥ 2), now)). Phone: yes (D3, D4). Unit: `aFailedRunIsRetriedIn15MinutesThenHourly`.
- **B5 A check passed since `lastRunAt` and no run since (B16).** The alarm at now (fires at once,
  job 2 runs). Phone: hard to provoke without a delayed alarm; seen in D3 as "the retry due".
  Unit: `aPageOpenBeforeTheAlarmCameKeepsTheCheck`, `aRunThatStartedBeforeTheCheckAndEndedAfterIt`.
- **B6 An arming during a run (page, Set, broadcast).** Keeps the start's retry (B16f1): the
  pair stays ≤ start + 15 min. Phone: open the page during a slow run (only with a long
  download; rarely). Unit: `aReArmDuringARunKeepsItsRetry`.
- **B7 `lastRunAt` ahead of now (clock set back).** Nothing owed; the retry may be late by the
  step. Phone: no.

## C. An alarm fires (`AlarmReceiver` → `DailyJob.runNow`, `DailyJob.kt:231-270`)

- **C1 Daily off.** Returns, nothing logged.
- **C2 A run goes in this process.** The pair at now + `runRetry`: `Daily job: alarms for …`,
  `Daily job: alarm, a run is going`. Phone: only with a run longer than its 15 min retry; no.
- **C3 Job 2 scheduled by this process and still pending** (the second alarm of the pair, or the
  network missing). `Daily job: alarm, a run is pending`, nothing armed. Phone: yes inside D3
  (airplane mode) via H4.
- **C4 Expedited schedule works (12+).** `Daily job: alarm, run scheduled`, then `Daily job: job 2
  starts, expedited`. Phone: yes, the natural retry alarm in D3, or tomorrow morning (P16).
- **C5 Expedited refused (no quota).** `Daily job: alarm, not expedited: RESULT_FAILURE`, `…run
  scheduled, not expedited`, `job 2 starts, not expedited`. Phone: no.
- **C6 Neither schedules (O4a).** `Daily job: alarm, couldn't schedule the run: …` and the pair
  15 min (or 1 h) ahead. Phone: no.
- **C7 A pending job 2 from a dead process (B16).** `runScheduled` is false in the new process, so
  the alarm schedules job 2 again (replaces it). Phone: only by luck.

## D. A job run (either id; `DailyJob.onStartJob`, `DailyJob.kt:59-106`)

Log skeleton of every run: `Daily job: job N starts[, expedited|, not expedited]` → (A3)
`Daily job: alarms for <+15m|+1h>` → [`Keep running: couldn't start: …`] → [`Daily job: <what it
set>`] → [`Daily job failed: …`] → [`Daily job: saved: …`] → `Daily job: jobFinished (job N)` →
(A4) `Daily job: alarms for …`.

- **D1 A second run while one goes (B16).** `Daily job: job N ends at once, a run goes`; the going
  run's end arms (a check it missed comes at once). Phone: no (can't start job 2 while forcing
  job 1). Unit: `JobRunsTest.aSecondRunWhileOneGoesDoesntStart`.
- **D2 A good run, nothing new (the phone on Sunday).** Steps: `adb shell cmd jobscheduler run -f
  io.github.buerlino.apodroid 1`. Expect: the skeleton with no "Daily job: <set>" line (APOD is
  today's, fetched since D1: `Refresh: APOD <date> same post`, nothing downloaded; NASA fetched,
  Friday's again, `Refresh: NASA <Friday> stored, nothing newer` (LG: `same post` only for today's
  stored post), nothing downloaded; both records due nothing); second arming by the clock (B1). Phone: yes, the baseline (P2).
- **D3 A failed fetch (offline).** Pre: airplane mode, USB adb. Steps: force job 1. Expect:
  `Daily job failed: java.net.UnknownHostException…` (or similar), NASA's problem stored (APOD,
  today's, only logged since D1: `Refresh: APOD <date> stored, check failed: …`, E3b), both alarm lines at start + 15 min. The page's box (open it): "NASA Image of
  the Day: no connection." Phone: yes (P3).
- **D4 Two failed runs in a row.** The second start arms +1 h (`failedRuns` 2), its end
  min(clock, start₂ + 1 h). Phone: yes (P4).
- **D5 Stopped by Android (`onStopJob`).** Interrupted; counted failed; no saves; retry 15 min /
  1 h from the start. Phone: no (`cmd jobscheduler timeout` exists but needs a run long enough;
  tablet-checked in V2).
- **D6 Killed mid-run (B19).** Stays counted (its start's +1); its start's pair stays; any arming
  in a new process keeps the retry. Phone: the run is too quick to kill mid-way; the "kept
  retry" half is testable after D3/D4 (P5).
- **D7 The daily switch off during a run.** `Daily job: stopped (the switch off)` (not a
  failure), nothing armed after. Phone: no (too quick).
- **D8 Live settling (L8f1).** A run within 15 s of a process start that created a non-preview
  engine, when a screen noted live only looks not live: `Live: HOME+LOCK not live yet, the
  engine's process N s old: nothing set`, `Daily job failed: …LiveSettling…`, retry 15 min. With
  home's id unchanged (G5) it doesn't fire. Phone: yes as a negative (P6).
- **D9 A set fails.** The record stays; the retry sets that screen only; `Despite the failure: …`
  when another set worked. Phone: no (nothing makes a set fail).

## E. The fetch and the problem notes (`Store.update` → `checkSite` → `refresh`)

- **E1 Stored post is today's (`isCurrent`).** A page open or Set: no fetch; `checkedAt` noted,
  problem cleared. The job's run (D1, 2026-11-15): the JSON fetched, the picture not downloaded,
  `Refresh: APOD <date> same post`, `checkedAt` noted, problem cleared. Phone: yes, `run -f 1`
  with today's post stored: `same post`, no download (the emulator, D1).
- **E1b A second post on the same date (D1).** The job's next run fetches it: another picture
  URL, so downloaded and stored (marked after a gap as any new post), `Refresh: APOD <date> new
  picture`. It goes on by rule 2: published before the change time + 3 h, or with "As soon as
  it's out", at once; later, at the source's next turn (APOD alone: usually the next date first).
  A pull gets it too; a page open doesn't (its `isCurrent` skip). Phone: no (never seen in the
  real feeds). Unit: SIM's `secondsStored`.
- **E2 Not today's (NASA on a weekend).** Fetch the feed, the same post stored again (no
  listener fires: same values), no download; `Refresh: NASA <its date> stored, nothing newer` (LG; the job's, a
  pull's, and a page open's after the hour). Phone: yes in D2.
- **E3 Network failure.** `<source>.problem = network`; the job's failure; page box "NASA Image
  of the Day: no connection." (" since <day>" only when `checkedAt` > 1 day old). Phone: yes (P3).
- **E3b Any failure of a stored day's check (D1, night default).** The job's run with today's
  post stored and complete: a failed fetch (network, HTTP, `SiteChanged`, the second picture's
  download) logs `Refresh: APOD <date> stored, check failed: …` only: no `<source>.problem`, no
  box on the page, not the run's failure (no retry armed for it), `checkedAt` untouched. Phone:
  yes, the emulator: airplane mode, `run -f 1` with only today's APOD in use: `check failed`, no
  box, `dumpsys alarm` the clock's next check, not start + 15 min.
- **E4 HTTP error.** "…: error 503. Trying again later." Phone: no.
- **E5 Not an image / site changed (`badUrl`).** Phone: no (made-up feed only, WP7c).
- **E6 Storage full.** Phone: never on the FP6.
- **E7 A success after a failure.** `problem` removed: the open page's listener redraws, the box
  goes. Phone: yes (end of P3).
- **E8 The subject noted (SA3a, 0.8.0).** `refresh` (at its start and end, every source in use)
  and a page open or Set that skips today's stored post or a check within the hour (T3f1): when the stored post's picture exists and no
  subject is noted for its date and picture URL (a new download, a fake, the update from 0.7.0, a
  death before the note), `noteSubject` decodes a copy (≥ 512 px on the shorter side, upright),
  computes `:core`'s `subjectPosition` upright and (a landscape picture) sideways (SA3c:
  `sidewaysSubjectPosition`, the decoded pixels turned clockwise, their own copy), and notes
  `<source>.subjectDate`, `subjectUrl`, `subjectPosition`, `subjectSideways` in one edit: `Subject:
  APOD 2026-11-16 p=0.78 (sideways 0.50) in 140 ms` (no "(sideways …)" for a portrait or square
  one). Once per post: the next runs log nothing. A picture that won't decode (or out of memory):
  `Subject: APOD 2026-11-16 couldn't: …`, the date and URL noted without positions (not tried
  again), the refresh goes on; its sets the middle. No `Subject:` line on a video day ("my
  picture" has none). **E8b (SH2c, 0.9.0):** the same note also holds the positions for "Keep
  pictures sharp"'s limited frame where the limit would bite for the file (whatever the switch says):
  `<source>.subjectPositionSharp`, `subjectSidewaysSharp` (`:core`'s `limitedSubjectPosition`, 0.5
  when no axis moves), and `<source>.subjectSharp=true`; the line adds ` (sharp 0.81, sideways
  0.50)` when there are any. A post noted by 0.8.0 (no `subjectSharp`) is noted again once (one
  more `Subject:` line after the update). Phone (the emulator): a fake into `files/` (the skill), the
  `<source>.subject*` keys removed, `run -f 1`: the line; a broken picture: `couldn't`. SA3c: a
  landscape picture's sideways position is its turned picture's own (`sidewaysSubjectPosition`);
  with Size Auto and the picture turned (⛶'s pill, or Auto's turn since 0.9.0, F15), the next set's band (`files/debug-set.png`) and
  ⛶'s frame stand at the line's "(sideways …)" (through `framePosition`).
  - **E8a (T3f1, fixed at 28b46c7):** the update from 0.7.0 (no `subject*` keys), a page open within the hour of the last
  check (no fetch): the `Subject:` line all the same; Set then logs `, auto <p>` when the subject
  moves the frame off the middle (before, Set went on at the middle until a pull or the next post).
  Phone (the emulator): v0.7.0's R8, a Set, `install -r` of the fix, open the page within the hour.
- **E9 The original's size (SH2b, 0.9.0).** Every download of a new picture in `refresh` (another
  date or picture URL, a missing file, Earth's thumbnail retried) first probes the original
  (`originalUrl`: APOD's and Earth's `content/dam`, NASA's enclosure): a GET with `Range:
  bytes=0-65535` (206, or a 200 read only that far), a second with `bytes=0-524287` only for a
  JPEG whose frame lies past the first 64 KB (`:core`'s `originalSize`, `probeMore`). No probe when
  the host doesn't scale (NASA's `images-assets.nasa.gov`: the rendition is the original).
  - **E9a The original smaller than the screen** (`takesOriginal`: `max(sw ÷ w, sh ÷ h) ≥ 1`, w×h as
    its header names it, the screen's portrait size): the original downloaded instead of the
    rendition (`.part`, decodable, renamed), `Refresh: APOD 2026-11-07 original 1500×1022 (smaller
    than the screen)`; `apod.jpg` is 1500×1022. Its download failing (not a stop, not storage):
    `… original 1500×1022 failed, the rendition instead: …`, then the rendition as before.
  - **E9b The original covers the screen:** `Refresh: APOD <date> rendition (original 4000×3000)`,
    the screen-tall rendition as before.
  - **E9c The probe failed** (the network, an HTTP error, a raw space in the URL) or found no size
    (a TIFF): `… probe failed: …` (`… probe failed: no size in the original's first bytes`), the
    rendition as before; nothing else changes (no problem note, no failed run: the rendition's own
    download decides). An Earth picture that's missing: the probe's `HTTP 404`, then EF1's thumbnail.
  - **E9c1 The real picture replaces a stored thumbnail** (EF1f1: `earth.thumbFallback` noted for
    the URL, the next refresh downloads the real file): the screens showing that post and its
    subject note are forgotten, the next run sets the real picture at its own subject; log
    `Refresh: EARTH <date> real picture instead of the thumbnail`. Phone case: fake the note with
    the thumbnail on the lock screen, refresh, the real picture is set. With the daily switch off
    only the subject note goes and the records stay (nothing would set the real picture, so the chip
    and Live's note must not be lost); Set or turning the switch on puts the real picture on.
  - Each such download notes `<source>.sizeChecked` (the picture URL), so E9d isn't owed.
  - **E9d A post stored by 0.8.0** (the rendition, no `<source>.sizeChecked` for its URL): checked
    once, at a refresh that finds the same post (the job's run, a pull, a page open after the hour)
    or a page open or Set that skips the check (T3f1's hooks, `noteStored`): `sizeChecked`
    committed first, then the probe (E9a–c's lines); when the original is smaller, it replaces
    `apod.jpg` (the same post and record: nothing is set, the screens keep the rendition until their
    next set), the subject's note dropped and noted again (`Subject: …` follows), `Refresh: APOD
    <date> stored picture replaced by the original`. A failed download: `… couldn't store the
    original: …`, the rendition kept; never a failure of the refresh. Not while
    `earth.thumbFallback` names the post's URL (each refresh tries the real picture anyway). Not
    again for that post (the next runs log nothing of it); a stop during it isn't retried either.

## F. What each screen gets (`dueWallpapers`, `Screens.kt:230-363`)

Phone-relevant leaves are F1–F4; the rest are unit-covered and need a faked clock, a video day
or days of time.

- **F1 One source, a change time since the record's stamp.** The screen takes C and gets its
  newest picture published before C + 3 h (`publishedBefore`). Phone: Monday 16 Nov 06:00+off:
  home (NASA) nothing (Friday's is on), lock (APOD) nothing yet (APOD out 06:05). Next-morning
  check (P16). Unit: `oneSourceGoesToItsNewestPictureAtTheChangeTime`.
- **F2 Rule 2: a newer picture published in the window.** Lock's APOD of 16 Nov, out 06:05 CET:
  set at the 06:15+off check: `Daily job: wallpaper set: Of(source=APOD, date=2026-11-16, url=…)
  on LOCK`, via job 2 expedited (P16). Unit: `aNewerPictureOfTheSourceThatWentOnReplacesItInTheWindow`.
- **F3 Published after the window.** NASA's Monday post (15:00–23:00 CET) waits for Tue 06:00
  (soon off): no home hand-over on Monday. Tuesday 06:00+off: `…wallpaper shown live: Of(source=NASA,
  date=2026-11-16…) on HOME`. Next-days check. Unit: `aPicturePublishedAfterTheWindowWaitsForTheNextChangeTime`.
- **F4 "As soon as it's out" on.** Then NASA's Monday post goes on home at the first hourly
  check after it's out. Only if the user has it on. Unit: `ScreensTest` with `soon`, SIM `…WithSoon`.
- **F5 After a day offline (B21).** One source: a post stored > 24 h after the last good check
  goes on at once. Phone: no (a day offline). Unit: `afterADayOfflineTheNewestGoesOnWhenARunSeesIt`.
- **F6 Turns by the clock (RO1, OR1's anchor).** Not on the phone (no column of two). Unit:
  `severalSourcesGoToTheTurnsSourceByTheClock`, `theFp6sDayOf11NovPutsApodOnAt1800`.
- **F7 Nothing to show passes the turn / keeps the picture.** Unit: `aSourceWithNothingToShowPassesTheTurn`.
- **F8 APOD video day: the stand-in or "Skip it".** Lock (APOD alone): with "Skip it" kept, page
  "Lock: video day, previous kept". Phone: only on a real video day. Unit: `theStandInWithApodAlone`.
- **F9 Never back.** Unit: `aScreenNeverGoesBackToAnOlderPicture`.
- **F10 Equal columns change together.** Not the phone. Unit: `equalColumnsAreOneSet`.
- **F11 A stamp that isn't a change time / in the future.** Unit: `aMigratedStampCountsAsTheCurrentChangeTime`.
- **F12 A screen without a record.** Starts on the turn's source, set at once. Unit:
  `aScreenWithNoRecordStartsOnTheTurnsSource`.
- **F13 A pull / Set / forgotten picture.** Newest at once (page side; other agents).
- **F14 A sideways part (SW2, 0.8.0).** The job's set of a post whose stored part is sideways
  (`<source>.sideways`, keyed by `cropDate`/`cropUrl`) goes on turned clockwise, through a bitmap
  (`setBitmap`, never `setStream`): `Daily job: wallpaper set: Of(…) on <screens>, sideways[,
  darker ×k (grey g)]`, `files/debug-set.png` turned (debug build). Phone (tablet): ✓ a sideways
  part (page.md K17), then `adb shell cmd jobscheduler run -f io.github.buerlino.apodroid 1` after
  forgetting the record (or at the next change time): set sideways again. A new post of that source
  (a faked date) drops the part: the next set upright, no ", sideways".
  PASS on the Android 14 emulator (SW2T, 2026-11-15; the tablet HA24BZ2Y not run): `run -f 1`
  with the record faked a day back gave `Daily job: wallpaper set: … on HOME+LOCK, sideways`,
  `debug-set.png` turned; a faked new post went on upright.
- **F15 Auto's turn (0.9.0, SH2a; was SW3's "Wide pictures sideways", removed).** Each set of a
  source's post (the job's, the page's, Set's, ✓'s, the hand-over's `live.sideways`, so B27's keep)
  turns it when its part notes `<source>.sideways=true`, or notes nothing (the key missing, also a
  part from 0.7.0) while the **stored** Size is Auto, the source is APOD, the picture file's upright
  size (`uprightSize`, EXIF's quarter turns swapped) is landscape (W > H) and the stored post's text
  has no up (`:core`'s `sidewaysOf`, `hasUp` of `title`, `<source>.alt`, `explanation`;
  `Store.turnsSideways`). `<source>.alt` is written with each stored post (a post stored before
  0.9.0 has none until this run's fetch stores it again, D1). So with Auto a new APOD galaxy post goes
  on sideways at its change time with no ⛶ at all: `Daily job: wallpaper set: Of(…) on <screens>,
  sideways, auto turn`; a horizon text, a portrait post, NASA's and Earth's, Centre's and Live's
  upright; "my picture" never. The pref `wideSideways` is no longer read. Unit:
  `FrameTest.sidewaysOfsTruthTable`, `sidewaysOfReadsOnlyWhatItNeeds`, `autosTurnByTheText`,
  `theUprightSizeSwapsOnlyForAQuarterTurn`, `UpWordsTest`, `PartTest.checkNotesTheOrientationWithADrag`.
  Phone (the emulator): Size Auto stored, APOD 3000×1000 faked as a new post with the galaxy text
  (setup.md X), `run -f 1`: `, sideways, auto turn`; the horizon text: upright.
- **F17 Keep pictures sharp (0.9.0, SH2c; setup.md Y).** Each still set (the job's, the page's,
  Set's, ✓'s; not Live's hand-over, B27's keep or any screen under the stored Size Live) while the
  stored `sharp` is on: after the orientation and the position, `:core`'s `limitBites` on the file's
  upright size; biting, `limitedPicture` (the background, the frame decoded alone at its dest size,
  turned when sideways), measured as drawn, `setBitmap`. The job's line: `Daily job: wallpaper set:
  Of(…) on <screens>[, auto 0.xx][, sideways…], sharp ×2.00 (Fill ×2.43)[, darker ×k (grey g)]`;
  the whole picture's `, sharp ×2.00 (whole ×n.nn)`. Not biting: every path as before. Unit:
  `LimitTest` (the layout), `SharpTest`. Phone (the emulator): setup.md P34e, then `run -f 1` with
  the record forgotten: the same line from `Daily job:`.
- **F16 Auto's frame (SA3a, 0.8.0).** Each still set (the job's, the page's, Set's, ✓'s) places
  the frame by `:core`'s `framePosition`: the post's ⛶ part, else with Size Auto (the default; a
  stored 0.7.0 `FILL` reads as Auto) the noted subject for the set's
  orientation (E8; sideways the `subjectSideways`) as it is, however small the move (0.9.1: no fallback); else (Centre, and under Live the screens left
  still) the middle. The measure (darkening) uses the same frame. The log line notes it: `Daily
  job: wallpaper set: Of(…) on <screens>, auto 0.78[, sideways][, darker …]`. Live's hand-over and
  pan unchanged. Unit: `SubjectTest.framePositionTakesThePartThenAutoThenTheMiddle`,
  `LiveTest.sizeOfReadsTheStoredNameAndAnUpdatesFillAsAuto`. Phone (the emulator): a picture with
  `importance_pull` ≥ 0.75 or ≤ 0.25 and one at 0.55–0.65 faked (both `, auto` since 0.9.1), `run -f 1`: the first `, auto` at
  its subject (the crop in `dumpsys wallpaper`, or `debug-set.png` when darkened or sideways),
  the second `, auto 0.5x–0.6x` (0.9.1: no fallback to the middle; no `, auto` only when the position is 0.50).

## G. Live (`Store.setDue` Live part `Store.kt:1247-1407`, `Live.kt`, `LiveWallpaper.kt`)
- **G1 Is home live (`liveState`, `Store.kt:1676-1686`).** 14+: `getWallpaperInfo(FLAG_SYSTEM)`
  names APODroid's, or home's wallpaper id equals `live.homeId` (noted while it named it). Shared:
  home live and `getWallpaperId(FLAG_LOCK)` = -1. Phone: `dumpsys wallpaper`: note home's
  component and id (baseline, P1).
- **G2 The hand-over (home due a new picture, Size Live, home live).** `live.part` → `live.jpg`,
  one commit with the notes (`live.whole`, `live.sideways` (SW2: the post's `Part.sideways`),
  `live.darken`, `live.picture`), home's record noted live, `wallpaperSetAt`: `wallpaper shown live:
  <picture> on HOME[, sideways][, darker ×k (grey g)]`; the engine: `Live: decoded W×H in N ms`
  (portrait-shaped when sideways: the turned picture), `Live: drew <picture> p=…`. No WallpaperManager call (home's id unchanged). Phone: Tuesday morning (F3),
  or P1's switch-on.
  Sideways: PASS on the Android 14 emulator (SW2T, 2026-11-15; the tablet not run): after the
  picker's "Home screen and lock screen", `wallpaper shown live … on HOME+LOCK, sideways`,
  `live.sideways=true`, `Live: decoded 1592×2400`.
- **G3 The lock screen's still set with home live, columns differ.** `FLAG_LOCK` set first,
  `wallpaper set: … on LOCK`; home's engine untouched (home's id unchanged, component still
  APODroid's). Phone: Monday 06:15 (P16) and P1's switch-on.
- **G4 The pan.** Home one source: p = (now − today's 06:00) / 18 h, held at 1 after 24:00 until
  the next 06:00. Phone: go home from an app, `Live: drew … p=0.xx` matches (P8). Unit:
  `LiveTest.oneSourceCrossesFrom0600To2400ThenStandsStill`.
- **G5 The engine's draws.** On surface changes, `onSurfaceRedrawNeeded`, each visibility, a copy
  change (prefs listener), and a minute tick only when the frame moved a pixel. Phone: yes (P8).
- **G6 Lost Live: home noted live and not live** (another wallpaper, a force stop, Android's
  revert). Next run: `Live: home isn't live, Fill on HOME+LOCK`, both set still, status line
  "Live isn't on. Set as wallpaper turns it on.". Phone: only with a force stop and its restore
  through Set's picker (P17, optional, ask the user). Unit: `homeLostLiveForgetsBothAndSetsBothStill`.
  A force stop keeps home's id (Android 14: 109, Android 10: 131) and leaves no engine in the new
  process, so home's kept id counts only while a non-preview engine is bound there (B35 on 14+, B39
  on 10–13); on 14+ a line `Live: home's id kept, Android names <component|null>, engine none in this
  process` comes first (once per process for each answer). Unit: `fourteenAKeptIdWithNoEngineIsNotLive`.
- **G7 One process death with Live on (L8f1).** Android rebinds the engine in a new process:
  `Live: engine 1 created` (new pid); home stays live by name or by the unchanged id with the
  engine bound in the new process (B35; on 14+ `Live: home's id kept, Android names …, engine
  bound in this process` while Android doesn't name it); no `Fill on`,
  no `stray` line, not then, not after the 15 s grace, not at the next screen-on. Phone: **yes,
  key test on Android 16** (P6). Unit: `aKillInTheSplitStateAfterTheGraceIsNoLoss`,
  `homesUnchangedIdKeepsItLive`.
- **G8 Two deaths within 10 s → Android reverts home (Decision 12).** Fill on both after the grace.
  Phone: no (destroys Live). Unit: `decision12sRevertAfterTheGraceIsFillOnBoth`.
- **G9 A stray engine on the lock screen.** `Live: home isn't live (stray on the lock screen)`,
  then D's run sets Fill. Phone: no.
- **G10 The engine on a screen not noted live (`unnotedLive`).** After a picker: forgotten and
  handed over in the same run, `Live: the engine is on HOME, not noted live`. Phone: seen in P1's
  switch-on (Set's picker is the other agents').
- **G11 A shared lock screen (B27, B28, `lockFollowsHome`).** Not the phone (not shared). Unit:
  `LiveTest` B28 cases, SIM seed 235. B27's keep (the lock screen keeps what the engine draws, a
  still `setFile`) honours `live.sideways` (SW2) with `live.whole` and `live.darken`: a sideways
  copy is kept turned, at the clock's position (`liveAt`), its log `Live: the lock screen keeps
  <picture>, still`; the set itself logs nothing more (no record written). Not the phone.
  B27's keep with a sideways copy: not run in SW2T (the emulator, 2026-11-15; unit and reading only).
- **G12 A failed lock set holds the hand-over (`handOverWaits`).** Unit only.
- **G13 The engine's lock-only pick (7299474, `lostLive` branch 2).** Not the phone.
- **G14 SUSPECT: a lost-Live screen with nothing to show keeps a stale record.** See SUSPECTs.

## H. D, the screen-on check (`LiveWallpaper.runIfOwed`, `Live.kt:332-336`)

- **H1 Within 15 min of a run's start.** Nothing. Phone: yes (P9).
- **H2 Nothing owed** (next alarm in the future, not a stray). Nothing. Phone: yes (P9).
- **H3 A check owed (`nextAlarm() ≤ now`).** `Live: screen on, run owed`, then C4's lines. Phone:
  via the retry in airplane mode (P3).
- **H4 Owed while job 2 waits for the network.** `Live: screen on, run owed`, `Daily job: alarm, a
  run is pending`. Phone: yes (P3).
- **H5 A stray with a record noted live.** A run → Fill on both. Phone: no.
- **H6 Daily off.** Nothing. Phone: inside P1 (no line while off).
- **H7 The preview engine.** Nothing. Phone: other agents' picker test.

## I. Keep running (`KeepRunning.kt`, `PendingStarts.kt`)

- **I1 On (daily on).** Foreground service, channel `keep` "Keep running", text "Keeps the
  wallpaper changing on time"; on 13+ only in Quick Settings' Active apps (no notification
  permission). Phone: `dumpsys activity services` (P10).
- **I2 Turned off (Advanced).** Stopped at once. Phone: yes (P10, restore).
- **I3 Daily off / on.** Off stops it; the switch-on's end starts it. Phone: P1.
- **I4 A run's start calls `ensure`.** Unrestricted: allowed (no log); Optimized: `Keep running:
  couldn't start: …ForegroundServiceStartNotAllowedException`. With it off: a `stopService`, no
  log. Phone: every forced run (P2).
- **I5 Sticky restart after a process death.** Foreground again (no `Keep running: refused`).
  Phone: P6 if it's on.
- **I6 Stop while a start is pending.** Unit: `PendingStartsTest`.
- **I7 Boot / update start.** Phone: no.
- **I8 Task Manager's Stop.** Back at the next page open. Phone: not in the list (unclear what
  LineageOS's Stop does to the alarms and the bound engine).

## J. The detector (`DailyJob.checkAlarms`, `Store.noteAlarmsGone`, `judgeMiss`, `Detector.kt`)

- **J1 First arming in a process, the alarms there** (a process death, not a force stop). No
  `Detector:` line. Phone: yes (P5/P6).
- **J2 Gone, unexplained** (a force stop). `Detector: the alarms found gone, after the run of
  <lastRunAt>`. Phone: only P17.
- **J2a The PendingIntents there, an exit record** (API 30–34 only, TP1: a force stop there
  leaves the `PendingIntent`s until system_server's GC). A `REASON_USER_REQUESTED` exit after the
  last arming (`armedAt`) whose description is a force stop's ("stop <package> due to …") or
  whose summary names the subreason `subreason=21 (FORCE STOP)` (TP1f1):
  `Detector: stopped by the user since the arming (exit record)`, then J2's or J3's line. Before
  it, on 30–34 with the `PendingIntent`s there, always `Detector: <n> exit records, armed
  <armedAt>; <time> reason <r> <description> (force stop); …` (the newest three; TP1f1, to see
  what Android returns). A record
  older than the arming, a Recents swipe ("remove task"), `am kill` or a cleaner's
  `killBackgroundProcesses` ("kill background", "kill all background"): only the records' line;
  29 and 35+: no `Detector:` line (J1). Phone: the Android 14 emulator, `am force-stop`
  with the process alive, a change time faked past, a page open, ten times: J2's line each time
  (TP1f1, run 156: six by the record, four with the `PendingIntent`s already gone, J2).
  Unit: `DetectorTest`.
- **J3 Gone, explained** (boot, update, job 2 pending, no run yet). `Detector: the alarms found gone
  (booted …, updated …, run pending …)`. Phone: no.
- **J4 The next ordinary run judges.** A set due by a change time after the last run, found
  gone ≥ T + off + 30 min later: `Detector: missed the change of <T> (Keep running on|off)`, the
  box; else `Detector: nothing missed`. Phone: P17 gives "nothing missed" (the Fill's stamps are
  today's 06:00, before the last run). Unit: `DetectorTest`.
- **J5 The box's texts, "Keep running" / "App info", Dismiss.** Phone: no (needs a real miss).
- **J6 The daily switch turned on in a fresh process doesn't log "alarms found gone".** Fixed by
  LG (`DailyJob.switchedOn`: its own arming explains it); phone P1.
- **J7 The daily switch, either way, drops the note.** `switchDaily` → `removeLost`. (J6's log line
  went with LG.)

## K. Save every picture (`Store.saveInUse`, after each job run)

- **K1 Off.** Nothing.
- **K2 On, posts not saved yet.** After the run, in order APOD → NASA → Earth (in use: APOD,
  NASA): `Daily job: saved: APOD 2026-11-15 original, NASA 2026-11-13 original` (or `…
  rendition`); files in `Pictures/APODroid/`. Also after a failed run, not after a stop.
- **K3 Already saved (★ or a run).** Nothing, no line.
- **K4 A stopped original (`saveTried`) / over the limit (`TooLarge`).** The stored picture next
  time / at once: `Original over the limit, saving the stored picture: …`. Unit: `SavingTest`.
- **K5 ★ saving meanwhile.** `BUSY`, logged in the saved line as `busy`; next run tries.
- Phone: only read-only (P13): if the switch is already on, a forced run logs K2 or K3. Don't
  turn it on without the user (it writes to the gallery).

## L. Off is off (`DailyJob.cancel`, `Store.switchDaily`)

- **L1 Off.** Both jobs and both alarms cancelled, a run dropped, Keep running stopped:
  `dumpsys alarm` shows no APODroid alarm, `dumpsys jobscheduler` no job, no KeepRunning service.
  No log line of its own. Phone: P1.
- **L2 On.** `startAfresh` (records forgotten), one update setting the stored setup: `Switch on:
  wallpaper set: Of(source=APOD, …) on LOCK; wallpaper shown live: Of(source=NASA, …) on HOME`
  (with `Live: the engine is on HOME, not noted live` before it), toast "Wallpaper set", then
  `Daily job: alarms for …`, job 1 scheduled and run at once (its own skeleton, nothing set),
  Keep running back. Phone: P1.
- **L3 Quick taps on/off/on (`SwitchRuns`).** One set. Unit: `SwitchRunsTest` (UI agents).

## M. How the page shows background results

- **M1 Status lines.** Columns differ, so one line per screen: "Home: 13 Nov, hh:mm · NASA",
  "Lock: 15 Nov, hh:mm · APOD" (`wallpaperSetAt` of each record). While resumed a prefs
  listener (records, `wallpaperSetAt`, posts, problems, `missedAt`) redraws. Phone: yes.
- **M2 The problem box.** Orange, the shown source's or the first ticked with a problem; goes
  when the job clears it (E7). Phone: P3.
- **M3 "Live isn't on…"** while Size Live, home has a column and isn't live. Re-read only with
  `readStatus` (a run's record change or the next resume), not when Live drops by itself.
  Phone: P17.
- **M4 The detector's box.** Phone: no.
- **M5 The pause hint.** Only what's missing: battery line on 33+ unless Unrestricted, "Pause app
  activity if unused" on 31+. Phone: read-only.
- **M6 The page's own load.** Sets only what's due; skips a source checked within the hour with
  no problem (`checkedRecently`); a failure doesn't touch the wallpaper. Phone: implicit.
- **M7 A redraw is held while a load, Set or the switch's run goes** (they redraw at their end).
  Phone: implicit.

## N. Concurrency

- **N1 SUSPECT: `ensure` from the main thread isn't under `DailyJob.lock`.** See SUSPECTs.

## O. The week's log (LOG; `AppLog.kt`, `MainActivity.weekLog`)

Not a user path: turned on and off only by adb, no UI, no text.
- **O1 Never turned on.** Every line to logcat only (tag `APODroid`, as before); no
  `Download/APODroid/log.txt` exists. Phone: P27.
- **O2 On** (`am start -n io.github.buerlino.apodroid/.MainActivity --ez weekLog true`; a start
  while the page is on top goes to `onNewIntent`, singleTop): the pref `weekLog` on, then
  `Week log: on` and the process-start line (`Process start: APODroid <version>, Android <release>
  (API <n>), <maker> <model>, bucket <ACTIVE|…>, battery optimisation on|off, Keep running on|off,
  setup: daily on|off, list|APOD mode, home … lock …, every <n> h from <hh>:00, as soon as it's out
  …, size …, video …, darken …, sharp …, order …, parts <n>`), both in logcat and in the file. From
  then every line also goes to the file, `yyyy-MM-dd HH:mm:ss.SSS <pid> <I|W|E> <message>` (a
  throwable's stack under it), in the calls' order (one writer thread). A rotation doesn't turn it
  again (only a start without saved state). Phone: P27.
- **O3 Each process start** while on (`App.onCreate`: the page, a job run, an alarm, a boot, the
  engine, Keep running's restart): the process-start line, with the new pid. Phone: P27.
- **O4 At about 1 MB** the older half is dropped (from a line's start) before the next line. Unit:
  none (app code). Phone: no (a week's file is far smaller).
- **O5 A write fails** (the file deleted in a file manager, storage full): logcat `Week log:
  couldn't write: …`, the line dropped, the run unaffected; the next line makes the file again.
  Phone: no.
- **O6 Off** (`--ez weekLog false`): `Week log: off` is the file's last line; nothing more written,
  the file kept. Phone: P27.
  - **O6a Off, then Back and the page reopened from Recents** (on was the start that made the task,
    so its base intent still holds `weekLog=true`): the relaunch has
    `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY` and is ignored, the log stays off; no new line in the
    file. Phone: P27.
- **O7 Reinstall or "Clear storage":** the file isn't the app's any more (MediaStore's owner), so
  the next line on makes `log (1).txt` beside it. Phone: no.

---

## SUSPECTs (from reading)

### SUSPECT G14 (low): a screen whose Live is lost while its source has nothing to show keeps a stale record

- **Where:** `Store.kt:1282` forgets the records in memory (`lost.forgetting(records())`), but
  only what `dueWallpapers` returns is written (`Store.kt:1313-1320`), and `dueWallpapers` drops a
  screen whose due record equals the record it was given (`Screens.kt:256`). A forgotten screen
  with nothing to show (`Rules.within` → `show(r, null)`, `Screens.kt:323-332`) comes back equal to
  the forgotten record, so it isn't in the plan; `unnoted = lost.forget - plan.screens.keys`
  (`Store.kt:1315`) then removes only its `wallpaperLive`, and the stored record keeps naming the
  old picture.
- **Failure:** the comment at `Store.kt:1314` says this path is for a screen "the rules leave out
  (no column)", but it also catches a screen with a column and nothing to show: home live on APOD
  alone on an APOD video day with "Skip it" (apod.jpg is deleted that day), or a turns column
  with no source stored. After Android's revert (or a force stop) the screen shows Android's
  default, while the record and the status line still name the old picture; the forgotten
  record's fast path (`record.picture == null` → due at once) is lost, so a picture that becomes
  available later in the turn goes on only by rule 2's window or at the next change time.
- **Phone:** no (needs a video day and a revert). A `ScreensTest`/`LiveTest` case would show it:
  `dueWallpapers` on `lost.forgetting(records)` with `stored.picture(owner) == null` returns a plan
  without that screen.

### J6, fixed by LG (`DailyJob.switchedOn`; was a low, log-only SUSPECT): the daily switch on in a fresh process notes "alarms found gone"

- **Where:** `MainActivity.kt:1029` → `Store.switchDaily(true)` removes the detector's note
  (`Store.kt:484-491`, `removeLost`) and writes `lastRunAt` = now; then the switch-on run sets and
  calls `scheduleJob` → `DailyJob.ensure` (`MainActivity.kt:1064`), whose `checkAlarms`
  (`DailyJob.kt:209-216`) runs because `armedHere` is false in a process that never armed or
  cancelled (the off happened in an earlier process). The alarms are gone because off cancelled
  them, and nothing in `alarmsLost` (`Detector.kt:35-36`) knows that: no boot, no update, no
  pending job 2, `lastRunAt` set by the switch.
- **Failure:** `Detector: the alarms found gone, after the run of <switch-on time>` and a
  `alarmsLostAt` note right after the switch dropped it (CLAUDE.md: "The daily switch, both ways,
  drops the note"). No box can follow: a miss needs a stamp after `lastRunAt` and 30 min before
  the note, impossible here; the next run logs `Detector: nothing missed`. Harmless but a false
  "found gone" line in the log a tester or the user reads. Fix idea: note the cancel in prefs, or
  set `armedHere` from `switchDaily(on)`.
- **Phone:** yes (P1).

### SUSPECT N1 (low, race): an arming on the main thread can replace a run's retry pair

- **Where:** `DailyJob.ensure` (`DailyJob.kt:163-166`) is called from `onResume`
  (`MainActivity.kt:624`) and `AlarmReceiver` (`AlarmReceiver.kt:26`) without `lock`;
  `arm(context)` reads `Store.nextAlarm()` and then sets the pair (`DailyJob.kt:180-190`). A run's
  `started()` (`DailyJob.kt:316-320`) commits `runStarts` and arms its retry under `lock` on the
  worker thread.
- **Failure:** main thread reads `nextAlarm()` with `failedRuns` 0 (the clock's next check, e.g.
  tomorrow 06:00) → the worker commits `runStarts` and arms start + 15 min → the main thread's
  `setWindow`/`setAndAllowWhileIdle` replace it with tomorrow. If the run then dies with its
  process, its retry is gone until the fallback job 1 (up to ~12 h) or a page open. The window is
  milliseconds (a page open or a `TIME_SET` exactly at a run's start). Finding 5 of
  `review-0.7.0-runs.md` looked at the arming's inputs, not this ordering.
- **Fixed:** `ensure` runs under the job's `lock` (`DailyJob.kt:163`), as `started()`.
- **Phone:** no (a race).

---

## Phone test list (prioritized, 27 cases)

On the Android 14 emulator (tester-rules-emulator.md): `run-as` reads the prefs, `adb root` kills,
the clock may be stepped and set back; P16 (a real morning) SKIPPED, real timing; P14's buckets read
only. P18–P22 are 0.8.0's, P23–P25 0.9.0's (SH2b), for the emulator.

General rules: USB adb (airplane mode cuts wireless adb). `adb logcat -c` is fine before a case;
`adb logcat -G 16M` once if the buffer rolls (harmless). Never change the clock, the zone, clear
data, uninstall or reboot. **Process death:** with "Rooted debugging" on, `adb root` then `adb
shell kill -9 $(adb shell pidof io.github.buerlino.apodroid)`; without root `adb shell am crash
io.github.buerlino.apodroid` (`am kill` can't: the wallpaper binding keeps the process
perceptible). **At most one death per 2 minutes:** two within 10 s make Android revert home's live
wallpaper (Decision 12, destroys Live), two crashes within 60 s mark the app "bad" (no background
starts until opened). After any death: `dumpsys wallpaper` must still name APODroid's engine on
home; if not, stop and restore Live with Set as wallpaper → the dialog → "Home screen".

Record first (P0): `dumpsys alarm`, `dumpsys jobscheduler`, `dumpsys wallpaper` (home component
and id, lock id), `dumpsys activity services` (Keep running on?), Setup's summary (06:00? "As soon
as it's out"?), Advanced (Save every picture, Keep running), the page's status lines. These are
"as found".

1. **P1, J6 + L1/L2 + I3 + G10.** Page open → daily switch off. Check: no APODroid alarm,
   no job, no KeepRunning service. Kill the process once (rules above); wait 1 min; `dumpsys
   wallpaper` still Live on home. `adb logcat -c`. Open the page (daily off: no `alarms for`
   line). Turn the switch on. Expect: `Live: the engine is on HOME, not noted live`, `Switch on:
   wallpaper set: Of(source=APOD, date=2026-11-15…) on LOCK; wallpaper shown live: Of(source=NASA,
   date=2026-11-13…) on HOME`, toast "Wallpaper set", `Daily job: alarms for <tomorrow 05:0x Z>`,
   job 1 runs at once (skeleton, nothing set), Keep running back if it was on. **J6 check (fixed by LG):** no `Detector: the alarms found gone` after the switch-on, and no `Detector:` line at job 1's run either (the switch on drops the loss note, so `judgeMiss` has nothing to judge; E17c, Opus pass 60).
   No detector box on the page either way. Restores itself (both screens set again with the same
   pictures; the status lines' times move).
2. **P2, baseline good run (D2, A3, A4, B1, E1, E2, I4).** `cmd jobscheduler run -f
   io.github.buerlino.apodroid 1`. Expect the skeleton: `job 1 starts, not expedited`, `alarms for
   <start+15m>`, no set line, `jobFinished (job 1)`, `alarms for <next check>` = what B1 gives
   (tomorrow 06:00 +off, or the next hour with "As soon as it's out" on); `dumpsys alarm` agrees;
   job 1 still periodic 6 h/6 h. Keep running: no `couldn't start` while Unrestricted. Nothing
   changes on the screens. No restore needed.
3. **P3, offline failure, the retry by the alarms, D's owed run (D3, E3, M2, C3, C4, H3, H4, E7).**
   Airplane on (`adb shell cmd connectivity airplane-mode enable`; check Wi-Fi is really off).
   Force job 1. Expect `Daily job failed: …` (a network exception), alarms at start + 15 min
   twice. Open the page: orange box "NASA Image of the Day: no connection."; screens unchanged.
   Leave the page (another app in front). At start + 15 (to +25) the retry alarm fires: `Daily job:
   alarm, run scheduled`; `dumpsys jobscheduler` shows job 2 pending (expedited, CONNECTIVITY
   unsatisfied); `dumpsys alarm` shows **no** APODroid alarm (documented: a pending run arms
   nothing). Press Home: `Live: screen on, run owed`, `Daily job: alarm, a run is pending`. Airplane
   off: within seconds `job 2 starts, expedited`, no failure, `alarms for <clock>`. Open the page:
   the box is gone. Restores itself.
4. **P4, two failures → 1 h (D4, B4).** Airplane on; force job 1 twice (wait for the first's
   `jobFinished`). The second: `alarms for <start₂+1h>` at its start and min(clock, start₂+1h) at
   its end. Keep airplane on for P5.
5. **P5, a kept retry in a new process (D6/B19, J1).** Continue from P4 (≥ 2 min after any earlier
   death). Kill the process once. Open the page: `Daily job: alarms for <start₂+1h>` (the retry
   kept, not tomorrow's), **no** `Detector:` line. Airplane off; force job 1: a good run, alarms by
   the clock again. Restored.
6. **P6, process death with Live on, Android 16 (G7, D8, H, I5).** The key L8f1 check on this
   phone. Note `dumpsys wallpaper` home id. Home screen showing. Kill once. Within 10 s force job
   1; again after 30 s (by then > 15 min since P5's run? if not, D stays quiet, fine). Expect: a new
   pid, `Live: engine 1 created`, `Live: decoded …`, `Live: drew … p=…`; no `not live yet`, no `Fill
   on`, no `stray`; home's id and component unchanged; status lines unchanged; Keep running (if on)
   foreground again within ~1 min, no `Keep running: refused`. If `Live: HOME+LOCK not live yet …`
   appears: note whether `dumpsys wallpaper` names APODroid and whether the id changed, then watch
   the 15 min retry: a `Fill on HOME+LOCK` there is regression 12 on Android 16 (BUG; restore Live
   through Set's picker).
7. **P7, page open arms (A1).** Open the page: `Daily job: alarms for T`, T = the clock's next
   check (as P2's end). `dumpsys alarm`: two alarms at T, one with a 10 min window.
8. **P8, the pan and the draws (G4, G5).** From an app, Home. `Live: drew Of(source=NASA, …) p=x`
   with x ≈ (now − 06:00)/18 h (16:00 → 0.56). Stay on home 2–3 min: a `drew` line only when the
   frame moved a pixel. Lock screen: still APOD (no engine draw on lock).
9. **P9, D quiet (H1, H2).** Within 15 min of a run: Home from an app → no `screen on, run owed`.
   After 15 min with nothing owed (P2's state): none either.
10. **P10, Keep running (I1, I2, I4).** If off: Advanced → on → `isForeground=true`, Quick
    Settings' Active apps lists APODroid; force job 1 → no `couldn't start`; turn off → the
    service gone. If on: off then on. Restore as found.
11. **P11, status lines and chip after a set (M1).** After P1: "Home: 15 Nov, hh:mm · NASA",
    "Lock: 15 Nov, hh:mm · APOD"; the chip "On home screen" on NASA's page, "On lock screen" on
    APOD's.
12. **P12, the fallback job's shape (A9).** `dumpsys jobscheduler`: job 1 periodic +6h, flex +6h,
    persisted, CONNECTIVITY; no job 2 outside P3.
13. **P13, Save every picture (K2/K3), read-only.** Only if already on: P2's run logs `Daily job:
    saved: …` once per new post, nothing on a second forced run. If off, skip (ask the user
    before turning it on: it writes to the gallery).
14. **P14, the pause hint (M5), read-only.** Battery line absent while Unrestricted (`deviceidle
    whitelist` lists the app); "unused apps" line per App info.
15. **P15, broadcasts by themselves (A6), read-only.** Search the log for `arming again` lines
    (an NTP `TIME_SET` may come by itself) and check each re-armed the same T.
16. **P16, next morning, no action (F1, F2, F3, G3, C4). Real timing: SKIPPED on the emulator.** Mon 16 Nov: 06:00+off run sets nothing;
    06:15+off (or the first check after 06:05): `job 2 starts, expedited`, `Daily job: wallpaper
    set: Of(source=APOD, date=2026-11-16…) on LOCK`; home's engine untouched (id unchanged), no
    hand-over (NASA's Monday post waits for Tuesday unless "As soon as it's out" is on); page
    "Lock: 16 Nov, 06:1x · APOD". Tuesday 06:00+off: `wallpaper shown live: Of(source=NASA,
    date=2026-11-16…) on HOME` (if NASA posted Monday).
17. **P17, optional, ask the user first: a force stop (G6, J2, J4, M3, A5).** `adb shell am
    force-stop io.github.buerlino.apodroid`. Android drops Live (default wallpaper on home, maybe
    both). Open the page: `Detector: the alarms found gone, after the run of <lastRunAt>` (maybe a
    `BOOT_COMPLETED, arming again` line: Android 15+ on leaving the stopped state), job 1 scheduled
    and run at once, `Live: home isn't live, Fill on HOME+LOCK`, both screens Fill, `Detector:
    nothing missed`, status line "Live isn't on. Set as wallpaper turns it on.", no detector box.
    Restore: Set as wallpaper → the dialog → "Home screen" in the picker; `dumpsys wallpaper` home
    = APODroid's engine, lock still; the status line gone; Keep running back if it was on.

18. **P18, a stored day's check (E1, E3b; D1).** Today's APOD stored (or faked with its date and
    URL kept: the skill's recipe), `run -f 1`: `Refresh: APOD <date> same post`, no download, no set.
    Airplane on, `run -f 1` with only APOD in use (APOD mode): `Refresh: APOD <date> stored, check
    failed: …`, `jobFinished`, no `Daily job failed`, `dumpsys alarm` the clock's next check (not
    start + 15 min), no box on the page. Airplane off.
19. **P19, the subject noted (E8).** `run-as` remove APOD's `subjectDate`, `subjectUrl`, `subjectPosition`, `subjectSideways` (unprefixed; NASA's `nasa.…`);
    `run -f 1`: `Subject: APOD <date> p=0.xx[ (sideways 0.yy)] in N ms`; again: no line. A broken
    `files/apod.jpg` (a few bytes) with the keys removed: `Subject: APOD <date> couldn't: …`, keys
    noted without positions. Restore with a pull.
20. **P20, E8a (T3f1, fixed).** Keys removed, page open within the hour of the last check: the
    `Subject:` line; Set then `, auto <p>` when the subject isn't at 0.50.
21. **P21, Auto's frame (F16).** Size Auto stored, a picture with `p` off the middle, the
    record forgotten (`wallpaperDate` removed), `run -f 1`: `Daily job: wallpaper set: … , auto
    0.xx`; a picture with `p` 0.55–0.65: `, auto 0.6x` (0.9.1: no fallback).
22. **P22, sideways by the job (F14, F15).** Size Auto stored, APOD 3000×1000 faked as a new post
    with the galaxy text (setup.md X), `run -f 1`: `… , sideways, auto turn`; the horizon text:
    upright; a ⛶ part noted off: upright; Size Centre: upright.

23. **P23, E9a and E9b (SH2b).** Fresh data (or APOD's keys and `apod.jpg` removed), the 7 Nov
    post served (the skill's fake feed, or a real day whose original is under 2484 px high),
    `run -f 1`: `Refresh: APOD 2026-11-07 original 1500×1022 (smaller than the screen)`, `apod.jpg`
    1500×1022 (`run-as … cat files/apod.jpg` and its header). A post with a large original: `…
    rendition (original W×H)`, `apod.jpg` as tall as the screen.
24. **P24, E9d (SH2b).** The 0.8.0 state: 7 Nov's rendition as `apod.jpg` (`?h=<screen height>`),
    `sizeChecked` removed, the page opened (within the hour or with today's post: the skip hook):
    `… original 1500×1022 (smaller …)`, `… stored picture replaced by the original`, then
    `Subject: APOD 2026-11-07 …`; `sizeChecked` = the URL; a second open logs none of it.
25. **P25, E9c (SH2b).** Airplane mode between the feed and the probe is hard to time: instead
    a faked `imageUrl` whose original answers 404 (APOD) at a pull: `… probe failed: …HTTP 404`,
    then the rendition's own outcome.

26. **P26, E8b and F17 (SH2c).** APOD 1500×1022 faked, `subjectSharp` removed: a page open logs
    `Subject: APOD … p=… (sharp 0.xx)`; a second open nothing. `sharp=true`, Size Auto, the record
    forgotten, `run -f 1`: `Daily job: wallpaper set: … , sharp ×2.00 (Fill ×2.43)`, `debug-set.png`
    with the bands; `sharp=false`: no ", sharp".

27. **P27, O1–O3, O6 (LOG).** Fresh install: `adb shell ls /sdcard/Download/APODroid/` has no
    `log.txt`. `am start -n io.github.buerlino.apodroid/.MainActivity --ez weekLog true`, then a
    `run -f 1`, a Set, `am kill` (the page in the background) and a page open, a reboot:
    `adb pull /sdcard/Download/APODroid/log.txt`: `Week log: on`, a `Process start: …` line at
    each new pid, the run's, Set's and the boot's lines in order, the same as logcat's. `--ez
    weekLog false`, a run: the file ends with `Week log: off`. Then (O6a) with on as the task's first
    start (`am force-stop`, then the `--ez weekLog true` start, then `false` while the page is on
    top): Back, reopen APODroid from Recents, a `run -f 1`: the file still ends with `Week log:
    off` (logcat has the run's lines, the file none).

End state check (after the last case): `dumpsys alarm` has the pair at the clock's next check,
job 1 periodic, `dumpsys wallpaper` home = APODroid's engine and lock its own still, Keep running
as found, airplane mode off, Setup and Advanced as found.

## What only unit tests (or nothing) cover here

Turns and the anchor (`ScreensTest`, `TurnTest`, SIM), video days and stand-ins (`ScreensTest`),
B21's gap (`ScreensTest`, `PictureTimeTest`), DST and zones (`PictureTimeTest`, `LiveTest`),
retries and owed checks (`PictureTimeTest`), one run at a time and off (`JobRunsTest`), Keep
running's pending starts (`PendingStartsTest`), the detector's judgement (`DetectorTest`), Live's
split, loss, settle, picker and D rules (`LiveTest`), saves (`SavingTest`), all of it together
over random months (`RulesSimulationTest`). Nothing covers G14 or N1.
