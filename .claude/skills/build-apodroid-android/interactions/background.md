# APODroid: background behaviour as a decision tree (commit 7299474)

> A snapshot read at 7299474 (15 Nov 2026); line numbers are of that commit (`git show 7299474:<path>`).
> The SUSPECTs below were fixed or accepted since: see [README.md](README.md). Newer changes: README, Keeping it current.

Read-only map for a phone tester. Sources: CLAUDE.md ("Background update", "Turns and the picture
time", "Traffic", "Wallpaper", "Problems and status lines"), `DailyJob.kt`, `AlarmReceiver.kt`,
`KeepRunning.kt`, `LiveWallpaper.kt`, `Store.kt`, `:core`'s `PictureTime.kt`, `Screens.kt`,
`Turn.kt`, `JobRuns.kt`, `Detector.kt`, `Live.kt`, `Saving.kt`, `PendingStarts.kt`. Every log line
below has the tag `APODroid` (`adb logcat -d -s APODroid`). Line numbers are of commit 7299474
(the working tree only differs in a dialog text and CLAUDE.md).

**Counts:** 105 leaves (A 10, B 7, C 7, D 9, E 7, F 13, G 14, H 7, I 8, J 7, K 5, L 3, M 7, N 1);
**3 SUSPECTs** (G14, J6, N1); 17 phone cases in the list at the end.

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
job 1 scheduled unless a run goes or it's there with 6 h / 6 h (`DailyJob.kt:159-167`). Every
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
  `DailyJob.kt:162`). Phone: `dumpsys jobscheduler` shows job 1 periodic 6 h, flex 6 h,
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

## C. An alarm fires (`AlarmReceiver` → `DailyJob.runNow`, `DailyJob.kt:226-265`)

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
  today's and skipped by `isCurrent`; NASA fetched, Friday's again, nothing downloaded; both
  records due nothing); second arming by the clock (B1). Phone: yes, the baseline (P2).
- **D3 A failed fetch (offline).** Pre: airplane mode, USB adb. Steps: force job 1. Expect:
  `Daily job failed: java.net.UnknownHostException…` (or similar), NASA's problem stored (APOD is
  skipped, today's), both alarm lines at start + 15 min. The page's box (open it): "NASA Image of
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

- **E1 Stored post is today's (`isCurrent`).** No fetch; `checkedAt` noted, problem cleared.
  Phone: APOD today (yes, invisible except no traffic).
- **E2 Not today's (NASA on a weekend).** Fetch the feed, the same post stored again (no
  listener fires: same values), no download. Phone: yes in D2.
- **E3 Network failure.** `<source>.problem = network`; the job's failure; page box "NASA Image
  of the Day: no connection." (" since <day>" only when `checkedAt` > 1 day old). Phone: yes (P3).
- **E4 HTTP error.** "…: error 503. Trying again later." Phone: no.
- **E5 Not an image / site changed (`badUrl`).** Phone: no (made-up feed only, WP7c).
- **E6 Storage full.** Phone: never on the FP6.
- **E7 A success after a failure.** `problem` removed: the open page's listener redraws, the box
  goes. Phone: yes (end of P3).

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

## G. Live (`Store.setDue` Live part `Store.kt:1045-1197`, `Live.kt`, `LiveWallpaper.kt`)

  PASS on the Android 14 emulator (SW2T, 2026-11-15): `Daily job: wallpaper set: … on HOME+LOCK, sideways`, `debug-set.png` turned; the hand-over's `live.sideways=true` (G2) seen with the picker, B27's keep not (G11: not a phone case).
- **G1 Is home live (`liveState`, `Store.kt:1440-1450`).** 14+: `getWallpaperInfo(FLAG_SYSTEM)`
  names APODroid's, or home's wallpaper id equals `live.homeId` (noted while it named it). Shared:
  home live and `getWallpaperId(FLAG_LOCK)` = -1. Phone: `dumpsys wallpaper`: note home's
  component and id (baseline, P1).
- **G2 The hand-over (home due a new picture, Size Live, home live).** `live.part` → `live.jpg`,
  one commit with the notes (`live.whole`, `live.sideways` (SW2: the post's `Part.sideways`),
  `live.darken`, `live.picture`), home's record noted live, `wallpaperSetAt`: `wallpaper shown live:
  <picture> on HOME[, sideways][, darker ×k (grey g)]`; the engine: `Live: decoded W×H in N ms`
  (portrait-shaped when sideways: the turned picture), `Live: drew <picture> p=…`. No WallpaperManager call (home's id unchanged). Phone: Tuesday morning (F3),
  or P1's switch-on.
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
- **G7 One process death with Live on (L8f1).** Android rebinds the engine in a new process:
  `Live: engine 1 created` (new pid); home stays live by name or by the unchanged id; no `Fill on`,
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
- **G12 A failed lock set holds the hand-over (`handOverWaits`).** Unit only.
- **G13 The engine's lock-only pick (7299474, `lostLive` branch 2).** Not the phone.
- **G14 SUSPECT: a lost-Live screen with nothing to show keeps a stale record.** See SUSPECTs.

## H. D, the screen-on check (`LiveWallpaper.runIfOwed`, `Live.kt:297-301`)

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
- **J3 Gone, explained** (boot, update, job 2 pending, no run yet). `Detector: the alarms found gone
  (booted …, updated …, run pending …)`. Phone: no.
- **J4 The next ordinary run judges.** A set due by a change time after the last run, found
  gone ≥ T + off + 30 min later: `Detector: missed the change of <T> (Keep running on|off)`, the
  box; else `Detector: nothing missed`. Phone: P17 gives "nothing missed" (the Fill's stamps are
  today's 06:00, before the last run). Unit: `DetectorTest`.
- **J5 The box's texts, "Keep running" / "App info", Dismiss.** Phone: no (needs a real miss).
- **J6 SUSPECT: the daily switch turned on in a fresh process notes "alarms found gone".** See
  SUSPECTs; phone P1.
- **J7 The daily switch, either way, drops the note.** `switchDaily` → `removeLost`. (J6 is
  where it doesn't hold.)

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

---

## SUSPECTs (from reading)

### SUSPECT G14 (low): a screen whose Live is lost while its source has nothing to show keeps a stale record

- **Where:** `Store.kt:1079` forgets the records in memory (`lost.forgetting(records())`), but
  only what `dueWallpapers` returns is written (`Store.kt:1106-1113`), and `dueWallpapers` drops a
  screen whose due record equals the record it was given (`Screens.kt:256`). A forgotten screen
  with nothing to show (`Rules.within` → `show(r, null)`, `Screens.kt:323-332`) comes back equal to
  the forgotten record, so it isn't in the plan; `unnoted = lost.forget - plan.screens.keys`
  (`Store.kt:1108`) then removes only its `wallpaperLive`, and the stored record keeps naming the
  old picture.
- **Failure:** the comment at `Store.kt:1107` says this path is for a screen "the rules leave out
  (no column)", but it also catches a screen with a column and nothing to show: home live on APOD
  alone on an APOD video day with "Skip it" (apod.jpg is deleted that day), or a turns column
  with no source stored. After Android's revert (or a force stop) the screen shows Android's
  default, while the record and the status line still name the old picture; the forgotten
  record's fast path (`record.picture == null` → due at once) is lost, so a picture that becomes
  available later in the turn goes on only by rule 2's window or at the next change time.
- **Phone:** no (needs a video day and a revert). A `ScreensTest`/`LiveTest` case would show it:
  `dueWallpapers` on `lost.forgetting(records)` with `stored.picture(owner) == null` returns a plan
  without that screen.

### SUSPECT J6 (low, log only): the daily switch on in a fresh process notes "alarms found gone"

- **Where:** `MainActivity.kt:920` → `Store.switchDaily(true)` removes the detector's note
  (`Store.kt:473-480`, `removeLost`) and writes `lastRunAt` = now; then the switch-on run sets and
  calls `scheduleJob` → `DailyJob.ensure` (`MainActivity.kt:955`), whose `checkAlarms`
  (`DailyJob.kt:204-211`) runs because `armedHere` is false in a process that never armed or
  cancelled (the off happened in an earlier process). The alarms are gone because off cancelled
  them, and nothing in `alarmsLost` (`Detector.kt:266-267`) knows that: no boot, no update, no
  pending job 2, `lastRunAt` set by the switch.
- **Failure:** `Detector: the alarms found gone, after the run of <switch-on time>` and a
  `alarmsLostAt` note right after the switch dropped it (CLAUDE.md: "The daily switch, both ways,
  drops the note"). No box can follow: a miss needs a stamp after `lastRunAt` and 30 min before
  the note, impossible here; the next run logs `Detector: nothing missed`. Harmless but a false
  "found gone" line in the log a tester or the user reads. Fix idea: note the cancel in prefs, or
  set `armedHere` from `switchDaily(on)`.
- **Phone:** yes (P1).

### SUSPECT N1 (low, race): an arming on the main thread can replace a run's retry pair

- **Where:** `DailyJob.ensure` (`DailyJob.kt:159-161`) is called from `onResume`
  (`MainActivity.kt:560`) and `AlarmReceiver` (`AlarmReceiver.kt:26`) without `lock`;
  `arm(context)` reads `Store.nextAlarm()` and then sets the pair (`DailyJob.kt:175-185`). A run's
  `started()` (`DailyJob.kt:311-315`) commits `runStarts` and arms its retry under `lock` on the
  worker thread.
- **Failure:** main thread reads `nextAlarm()` with `failedRuns` 0 (the clock's next check, e.g.
  tomorrow 06:00) → the worker commits `runStarts` and arms start + 15 min → the main thread's
  `setWindow`/`setAndAllowWhileIdle` replace it with tomorrow. If the run then dies with its
  process, its retry is gone until the fallback job 1 (up to ~12 h) or a page open. The window is
  milliseconds (a page open or a `TIME_SET` exactly at a run's start). Finding 5 of
  `review-0.7.0-runs.md` looked at the arming's inputs, not this ordering.
- **Phone:** no.

---

## Phone test list (prioritized, 17 cases)

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

1. **P1, SUSPECT J6 + L1/L2 + I3 + G10.** Page open → daily switch off. Check: no APODroid alarm,
   no job, no KeepRunning service. Kill the process once (rules above); wait 1 min; `dumpsys
   wallpaper` still Live on home. `adb logcat -c`. Open the page (daily off: no `alarms for`
   line). Turn the switch on. Expect: `Live: the engine is on HOME, not noted live`, `Switch on:
   wallpaper set: Of(source=APOD, date=2026-11-15…) on LOCK; wallpaper shown live: Of(source=NASA,
   date=2026-11-13…) on HOME`, toast "Wallpaper set", `Daily job: alarms for <tomorrow 05:0x Z>`,
   job 1 runs at once (skeleton, nothing set), Keep running back if it was on. **SUSPECT check:**
   whether `Detector: the alarms found gone, after the run of …` appears after the switch-on (code:
   yes; CLAUDE.md: the switch drops the note), and then `Detector: nothing missed` at job 1's run.
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
16. **P16, next morning, no action (F1, F2, F3, G3, C4).** Mon 16 Nov: 06:00+off run sets nothing;
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
