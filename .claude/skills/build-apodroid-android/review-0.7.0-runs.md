# Review of the run's bookkeeping (O4, 2026-11-13)

By reading, no device, no code changes (winston, pipeline run 77, on `dev` at b6bc1d0). The
question (`on-time-0.7.0.md`, O4): is there a simpler model (fewer prefs, fewer paths that arm,
fewer flags) that keeps every case B5–B24 fixed and fits O10 (job 2 expedited), O11 (Keep
running's start at a run's start), O14 (the detector) and D (Live's screen-on run, L8)?

**Short answer.** The model is correct for every case I traced; no finding loses a picture on
today's phones. A model with two fewer prefs exists: count a run as failed at its start and clear
the count at a good end (O4b). It removes `runGoing`, `failedAt` and `nextAlarm`'s `running`, and
makes the killed run an ordinary failure. The cost is a decision: the retry is measured from the
run's start, not its end. The paths that arm (three) and the flags in the process (`JobRuns`,
`runScheduled`, `SwitchRuns`) stay: each one fixes a case nothing else covers. Two findings are
real gaps, both narrow: a failed `schedule()` in `runNow` leaves no alarm (it matters for O10 and
O14), and the switch-on run sets the second screen after an off.

**Read in full:** `app/…/DailyJob.kt`, `app/…/AlarmReceiver.kt`, `core/…/JobRuns.kt`,
`core/…/SwitchRuns.kt`, `core/…/PictureTime.kt`, `core/src/test/…/RulesSimulationTest.kt` (SIM).
**Read in part:** `Store.kt`'s `switchDaily`, `startAfresh`, `runStarts`, `runRetry`, `ranJob`,
`nextAlarm`, `daily`, `update`, `isStopped`, `commitBeforeStop`, `saveInUse`, `setDue`,
`checkSite`, `set`, `Updated`. `MainActivity.kt`'s companion (`runs`, `setting`, `switching`,
`switchRuns`, `scheduleJob`, `runAllNext`), `onResume`, `runNext`, `switchDaily`, `switchOn` and
`setAsWallpaper`'s run. `PictureTimeTest`'s armings (lines 180–380). `Post.kt`'s download
timeouts. The manifest's receiver. `CLAUDE.md`'s Background update and the daily switch.
`plan-0.6.0.md`'s B5, B6, B7, B12, B13, B16, B18, B19 and B24, in full from cc4a9ab^ (D21 cut them
to one line). CR2's findings 1–2 (`review-0.6.0-cr2.md` at 592c0e7). `on-time-0.7.0.md`'s plan,
O10, O11, O14 and its Seams with track L.

## The cases and what fixes each now

Lines are `dev` b6bc1d0's. "SIM" names the invariant that guards the case, or says that SIM
doesn't model it.

| Case | What fixes it now (the pref, the path, the line) | SIM |
|---|---|---|
| B5, B7b: an arming after a failed run (a Set, a page open, a broadcast) keeps its retry | `failedRuns`, `failedAt` written by `Store.ranJob` (`Store.kt:456`); `:core`'s `nextAlarm`, `failedAt` + `retryAfter` (`PictureTime.kt:180`) | 7 |
| B7a: a stop by Android, also during the saves, counts as failed | `onStopJob` sets `stoppedBySystem` (`DailyJob.kt:98`); `failed = failure != null \|\| stoppedBySystem` (`DailyJob.kt:86`) | not modelled |
| B7c, B19b: off is off: a run from before an off ends without effect, leaves no retry and isn't logged as a failure | `JobRuns`' generation (`JobRuns.kt:46`); `cancel` drops it (`DailyJob.kt:219`); `started`, `noted` and `ended` check it (`DailyJob.kt:236`, `:249`, `:262`), as does the log line (`DailyJob.kt:71`); `Store.switchDaily` removes `failedRuns`, `failedAt` and `runGoing` (`Store.kt:356`) | `JobRunsTest`; SIM's switch off has no run going |
| B12: a failing series is retried after 15 min, then hourly, and never holds a Set back | `retryAfter` (`PictureTime.kt:141`); `jobFinished(params, false)` (`DailyJob.kt:88`); `onStopJob` returns false (`DailyJob.kt:100`) | 7 (the `runEnd` check, `RulesSimulationTest.kt:510`) |
| B6: quick on, off, on taps set once | `SwitchRuns` (`SwitchRuns.kt:10`); `MainActivity.switchDaily` (`:753`), `switchOn`'s `ended` (`:787`), `runNext` (`:554`) | `SwitchRunsTest` |
| B13: Set's and the switch's runs keep their end, the scheduling too, over a rotation or Back | the process-wide `runs` (`MainActivity.kt:293`); `scheduleJob` after the set (`:873`, `:789`) | not modelled |
| B16 finding 1: a passed check kept when something arms again | `lastRunAt` (`Store.kt:438`); `nextRun`'s owed check (`PictureTime.kt:135`) | 7 (`clockCheck`) |
| B16 finding 2: one run at a time | `JobRuns.start` (`JobRuns.kt:22`); `onStartJob` ends a second job at once (`DailyJob.kt:57`) | `JobRunsTest`; not in SIM (its runs are instant) |
| B16 finding 3: a stale pending id 2 (Android's own reschedule after a death) replaced | `runScheduled` (`DailyJob.kt:129`, set `:198`, cleared `:56`, read `:191`) | not modelled |
| B16, B16f1: a run killed mid-run is retried: its start arms the pair `runRetry` ahead, and no arming during the run moves the pair later | `started` (`DailyJob.kt:238`); `nextAlarm`'s `running` branch (`PictureTime.kt:179`, from `DailyJob.kt:153`) | 7 (the `runGoing` branch, `RulesSimulationTest.kt:505`) |
| B16 finding 4: the interrupt flag cleared before `ranJob`, so the page's listener hears it | `Thread.interrupted()` (`DailyJob.kt:83`) | not modelled |
| J2: nothing schedules a job's id while its run goes (that would stop the run) | `ensure` skips the fallback while `runs.going` (`DailyJob.kt:140`); `runNow` (`DailyJob.kt:183`) | not modelled |
| B18 (CR2's 1): an alarm landing in a run arms the pair `runRetry` ahead again | `runNow`'s going branch (`DailyJob.kt:186`) | not modelled |
| B18 (CR2's 2): a stop read from the job's flag, not from the interrupt alone | `isStopped` (`Store.kt:761`); `commitBeforeStop` (`Store.kt:767`); checks before each source, set and save (`Store.kt:740`, `:748`, `:839`, `:793`); the lambdas (`DailyJob.kt:67`, `:77`); `stopped` after the update (`DailyJob.kt:75`) | not modelled |
| B19: a killed run keeps its retry in a new process and counts as failed at the next start | `runGoing` written by `runStarts` (`Store.kt:439`), removed by `ranJob` (`:457`) and `switchDaily` (`:358`); counted by `runStarts` (`Store.kt:432`); read by `Store.nextAlarm` (`Store.kt:479`); `runRetry` (`PictureTime.kt:153`) | 7, SIM's kills (`RulesSimulationTest.kt:476`) |
| B24: the bookkeeping before `jobFinished` | `noted` → `jobFinished` → `ended` (`DailyJob.kt:86`–`90`) | SIM's `jobRun` follows the order |

B8–B11, B14, B15, B17 and B20–B23 are not the bookkeeping (the draft, the UI, the rules, the
texts). B15's 09:00 check is `nextRun`'s `last + pictureWindow` (`PictureTime.kt:128`, SIM 7).

**What SIM covers and what it doesn't.** SIM checks only invariant 7 on the bookkeeping: every
arming equals CLAUDE.md's checks, retries and kills (`RulesSimulationTest.kt:496`), and after a
failed run it is no later than its retry. SIM's runs are instant, so it has no concurrency
(`JobRuns`), no alarm landing in a run (B18), no stop by Android (B7a), no pending id 2
(`runScheduled`), no failed `schedule()` and no page runs racing the switch. `JobRunsTest`,
`SwitchRunsTest` and the tablet cover those.

## Findings

Severities as CR's: crash or data loss / wrong wallpaper or wrong state / wrong text / tidy.

### Crash or data loss

None found.

### Wrong wallpaper or wrong state

#### 1. A failed `schedule()` in `runNow` leaves no alarm and nothing pending

- **Where:** `app/src/main/kotlin/io/github/buerlino/apodroid/DailyJob.kt:199` (`runNow`: the else
  branch at `:200` only logs; a thrown exception from `schedule()` isn't caught either).
- **Failure:** the alarm fires and has used up the pair. `runNow` asks for job 2. If
  `schedule()` returns `RESULT_FAILURE` or throws, nothing is armed and nothing is pending. Nothing
  runs until job 1's next window (every 6 h with a 6 h flex, so up to about 12 h), a page open, a
  boot or a clock change. Today a regular one-off job almost never fails to schedule. Two things
  that come make it matter:
  - **O10:** `schedule()` of an expedited job fails by design when the quota is out. The plan's
    fallback to a regular job covers `RESULT_FAILURE`, but a fallback that also fails, or one
    that throws, lands here.
  - **O14:** the detector reads "the alarms found gone" as a force stop. After this path the pair
    is gone without one. Steps: the 06:07 alarm fires, `schedule()` fails, the page opens at
    06:50. The page's set is due at T = 06:00, `lastRunAt` < T, job 2 isn't pending, and the
    sign reads gone after T. The box offers Keep running for a stop that never happened.
- **Fix:** on a failed or thrown `schedule()`, arm the pair `runRetry` ahead, as the going branch
  does (`DailyJob.kt:186`). One helper `armRetry` for the three direct retry arms (`:186`,
  `:238` and this one).
- **Bug.** Sure of the path. Today it's rare.

#### 2. The switch-on run sets the second screen after an off

- **Where:** `app/src/main/kotlin/io/github/buerlino/apodroid/Store.kt:749` (`update` reads
  `daily` once, before `setDue`), with `Store.kt:839` (`setDue`'s loop checks only
  `isStopped`), with `MainActivity.kt:780` (`switchOn` passes no `stopped`).
- **Failure:** `CLAUDE.md` (The daily switch): "a running one restarts nothing and sets nothing
  more once `daily` is off". `switchOn`'s KDoc says the same ("sets only while it is still on").
  Steps: home and lock are due different pictures (columns that differ, or one screen's record
  forgotten). The switch goes on, its run's `setDue` sets home (about 0.5 s, seconds with the
  whole picture). The switch goes off during that set. The loop goes on and sets the lock
  screen, and writes its record, with the switch off. The job's runs don't have this problem:
  `cancel` makes `onStopJob` set the flag, and the loop stops before the next set. The page's
  `load()` (`MainActivity.kt:534`) has the same gap.
- **Fix:** in `setDue`, before each set after the first, stop the loop (return what was done,
  not throw) when the update has no `set` and `daily` is off. Then `Updated.wentToSet = false`
  (it gave up), so B6's last on still runs. The cost is accepted already: for on, off, on during
  the first set, the owed run sets home again ("An off and on that come after the run's set …
  sets the same picture once more", accepted 2026-10-21).
- **Bug against CLAUDE.md, narrow**: an off within the seconds of the first screen's set. The
  alternative is a decision: the text says "finishes the set it's in, on both screens".

### Seams with what comes (decisions for the packages that build them)

#### 3. A run's start has no catch-all: O11's start there must catch for itself

- **Where:** `app/src/main/kotlin/io/github/buerlino/apodroid/DailyJob.kt:65` (`started(…)` and
  `Store(this)` at `:64` run before the `runCatching` at `:67`).
- **Failure (O11 built without its own catch):** O11 starts `KeepRunning` at each run's start.
  `startForegroundService` throws `ForegroundServiceStartNotAllowedException` when Android refuses
  it, which the plan expects while the app isn't Unrestricted. If that call sits in `started()` or
  anywhere before `:67`, the exception kills the process after `runStarts` has noted `runGoing`.
  Every run then dies at its start, is counted as killed and is retried an hour later
  (`runRetry`), and nothing is ever set.
- **Fix:** O11 calls it after `started()`, outside `lock`, in its own `try`. The plan's "a refusal
  caught" carries all the weight here, so O11's tablet check should include a refusal
  (Optimized, a run forced with `run -f 2`).
- **Decision for O11.** Sure of the placement. Whether Android refuses there I didn't check.

#### 4. The stop flag is shared across an off

- **Where:** `app/src/main/kotlin/io/github/buerlino/apodroid/DailyJob.kt:61` (`stoppedBySystem
  = false` at each start), with `core/…/JobRuns.kt:48` (`drop` sets the count to 0).
- **Failure:** after an off, a run from before it may still be ending: `cancel` interrupted it,
  and it may be inside a set. An on that schedules job 1, which runs at once, can then start a
  new run beside it. The new start resets `stoppedBySystem`, which the old run's `stopped`
  lambda reads, and overwrites `worker` and `workerJob`. I found no wrong effect. The old run
  still ends at its next stop check through its interrupt: `commitBeforeStop` captures the flag
  (`Store.kt:768`), the download checks it, and the wallpaper lock orders the switch-on's set
  after it. It is fragile, though, and O10's expedited job is stopped sooner and more often.
- **Fix:** one stop flag per run, held with its job id and thread. `onStopJob` sets the flag of
  the run with its id. `worker`, `workerJob` and the reset go.
- **Tidy.** Sure of the path, and sure nothing goes wrong today.

#### 5. `nextAlarm(running)` repeats the `runGoing` pref, and Live's seam needs it without the argument

- **Where:** `app/src/main/kotlin/io/github/buerlino/apodroid/Store.kt:475` and `:479`, called
  from `DailyJob.kt:153` with `runs.going`.
- **Failure:** none. Every run that counts writes `runGoing` in `started()` (`DailyJob.kt:237`)
  before anything could arm with `running = true`. Two tiny windows differ: between
  `runs.start()` and the thread's `started()`, and between `noted()` (which removes `runGoing`)
  and `ended()`. In both the result is a spare alarm that `started()` or `ended()` replaces at
  once. After an off, `runs.going` is false anyway (`drop`). Track L's seam 1 has the engine call
  "`Store.nextAlarm()` ≤ now", which doesn't compile today: the engine can't see `runs`.
- **Fix:** drop the argument; the pref decides (O4b, or alone if O4b isn't taken).
- **Tidy.** Sure.

## The simpler model

### What it is: a run counts as failed from its start until it ends well

Today four prefs carry a run: `lastRunAt` (its start), `runGoing` (it hasn't ended),
`failedRuns` (failed runs in a row) and `failedAt` (the last failed run's end). A killed run is a
third kind of outcome, with its own path: `runStarts` reads the leftover `runGoing` and counts
it, and `nextAlarm` has a branch for it. In O4b's model:
- **A run's start** (`runStarts`): `failedRuns + 1` and `lastRunAt` = now, in one commit. The run
  counts as failed until it says otherwise.
- **A good end** (`ranWell`, before `jobFinished` as B24 wants): `failedRuns` removed. A failed or
  stopped end writes nothing: it's counted already. A killed run is the same as a failed one:
  nothing at its end.
- **`:core`'s `nextAlarm(now, zone, failures, time, turns, offset, waiting, lastRun)`:** the
  clock's next run (`nextRun`, owed checks since `lastRun` included), or, with `failures > 0`, the
  earlier of that and `lastRun + retryAfter(failures)`, at the earliest now. `runRetry` becomes
  `retryAfter(failedRuns)`.

**What it removes:** the prefs `runGoing` and `failedAt`; `nextAlarm`'s `running` and `failedAt`
parameters and its third branch; `nextRun`'s `failures` parameter (only tests use it); the
killed-run check in `runStarts`; `ranJob`'s failed branch; two lines in `switchDaily`. SIM's
expected arming goes from three branches to two (`RulesSimulationTest.kt:504`–`507`). It also
removes finding 5. Paths that arm and in-process flags: unchanged (see below).

**What changes, the decision:** a failed run's retry is measured from its start, not its end.
For a normal failure (no network, a 503, a 30 s read timeout per source) the two are seconds
apart. For a run Android stops at its limit, the retry comes earlier: 5 min after a regular job's
10 min stop instead of 15, and 12 min after an expedited job's 3 min stop (O10). The run's own
start pair already uses the start: that's B16f1's and B19's rule. So after O4b one rule holds,
"retried `retryAfter` after its start", whether the run failed, was stopped, was killed or still
goes. The log line "the run before never ended (killed)" goes: a killed run is no longer told
apart from a failed one.

**The update from 0.6.0:** a `runGoing` left by 0.6.0 (the update's `install -r` killed a run:
V11a's case) is counted once (`failedRuns + 1`, `runGoing` and `failedAt` removed), a migration
under `CLAUDE.md`'s rule. Night default; the alternative is to accept it. A killed id 2 then
waits for the clock's next check (at most an hour within the window), once per phone, at most.

### How each case stays fixed

| Case | In O4b's model | Guarded by |
|---|---|---|
| B5, B7b | `failedRuns > 0` and `lastRunAt`: every arming keeps `lastRunAt + retryAfter`. Same instant as today but for the run's length | SIM 7 (rewritten), `PictureTimeTest.aLaterArmingKeepsTheRetry` (from `lastRun`) |
| B7a | unchanged: a stopped run doesn't call `ranWell`, so it stays counted | — |
| B7c, B19b | unchanged: the generation; `switchDaily` removes `failedRuns` | `JobRunsTest` |
| B12 | `retryAfter(failedRuns)`: 15 min after the first failure, hourly after more | SIM 7 (`runEnd`) |
| B6, B13 | unchanged (page only) | `SwitchRunsTest` |
| B16 finding 1 | unchanged (`lastRunAt`, `nextRun`) | SIM 7, `PictureTimeTest` |
| B16 findings 2, 3, 4 and J2's rule | unchanged (`JobRuns`, `runScheduled`, `Thread.interrupted()`) | `JobRunsTest`, the tablet |
| B16f1 | during a run `failedRuns` ≥ 1, so every arming is no later than `lastRunAt + retryAfter(failedRuns)`, the same instant as today's `lastRun + runRetry(before)` | SIM 7, `PictureTimeTest.aReArmDuringARunKeepsItsRetry` (without `running`) |
| B18 | unchanged (the going branch arms `now + retryAfter(failedRuns)`) | the tablet |
| B19 | a killed run stays counted (it never reached `ranWell`). An arming in a new process gets the same `lastRunAt + retryAfter` as today; the next start adds itself, so `retryAfter(n + 2)`, the same as today's count of the killed run plus `runRetry` | SIM's kills and 7, `PictureTimeTest.aKilledRunKeepsItsRetryAndCounts` (rewritten) |
| B24 | `ranWell` before `jobFinished`. A death between them counts a good run as failed, as B24 found, so the order stays | — |

Traced by hand, case by case, against today's formula. The only instant that moves is the retry
after a failed end (from its end to its start).

### What stays, and why it can't go

- **Three paths that arm:** `ensure` (the clock, from `onResume`, Set, the switch on, the
  receiver's broadcasts and a run's end); the start's pair (`DailyJob.kt:238`); the going branch
  (`DailyJob.kt:186`, B18). The going branch can't call `nextAlarm`: when a run outlasts its
  retry, or a check passed during it, `nextAlarm` says now, and the alarm would fire into the run
  again and again until it ends. The start's pair could go through `nextAlarm` (same safety, one
  spare alarm when the clock's check comes first); not worth a change. O4a folds the three direct
  retry arms into one helper.
- **`JobRuns`:** one run at a time (B16 finding 2), no schedule while a run goes (J2), off's
  generation (B7c). Each one is a case.
- **`runScheduled`:** without it the pair's second alarm would schedule id 2 again while the first
  alarm's job is about to start, and stop it. Without the "this process" part, a stale pending id
  2 would block the alarms (B16 finding 3). `getPendingJob` alone can't tell the two apart.
- **`SwitchRuns`:** B6. Page-only, main thread, separate from the job.

### What comes

- **O10 (job 2 expedited):** fits as is. `runScheduled` stays true across the expedited try and
  the regular fallback. A stop by the shorter limit is B7a's path: counted, retried, and the next
  job save of that post is the smaller copy (`saveTried`). It needs O4a (finding 1), so a fallback
  that fails still leaves a pair. With O4b the retry after a 3 min stop comes 12 min after the
  stop (the decision above).
- **O11 (Keep running at a run's start):** finding 3: after `started()`, outside `lock`, in its
  own `try`. One more thing for O11's memory check: with the process kept for days, the
  in-process state (`JobRuns`, `runScheduled`) lives that long too. A run thread that never
  returned would block every later run in that process: `runs.going` stays true, `runNow` only
  arms again, and `ensure` never schedules the fallback. I found no path that hangs (the download
  has 15 s connect and 30 s read timeouts, `Post.kt:175`; a stop interrupts it), so this is a
  note, not a finding.
- **O14 (the detector):** `lastRunAt` means what the detector needs: a job run started (or the
  switch went on) at that time. That holds in O4b's model too. **The pair is armed at every
  moment while `daily` is on, with two exceptions**, verified through every arming and firing
  path above: (a) the pair fired and id 2 is pending (`runNow` arms nothing, `DailyJob.kt:191`,
  `:199`), which O14 already excludes; (b) a failed `schedule()` (finding 1, fixed by O4a).
  Otherwise a run's start arms (`:238`), an alarm landing in a run arms (`:186`), a run's end arms
  (`:264`), and a death between `noted` and `ended` still has the start's pair or B18's. Between
  the switch's off and its next on nothing is armed, but `switchDaily(on)` notes `lastRunAt`, so
  O14's "the switch on after T" covers a missing pair at the first arming of that process. So
  once O4a lands, "the pair gone while job 2 isn't pending and `daily` is on" really means
  someone removed it. SIM can guard that (O4a's invariant 9).
- **D (Live's screen-on run, L8): yes, a run for `lastRunAt`, through `runNow` as the seam
  says.** It then notes `lastRunAt`, so (1) the checks before it are no longer owed (it did
  them), (2) the detector doesn't count a miss (the job did run), and (3) a failure counts in
  `failedRuns` and is retried. A direct `Store.update` from the engine would break all three.
  `runNow` is callable from the main thread with no lock held. Its going branch takes `lock`,
  which the worker holds only for a prefs commit and an arming. The engine's "a check is owed"
  needs `Store.nextAlarm()` without `running` (finding 5). While id 2 is pending each screen-on
  returns at `runNow`'s pending check (cheap). In a new process (`runScheduled` false), each
  screen-on replaces the pending id 2: harmless as today, but L8's check should look at the log
  for repeated "alarm, run scheduled" lines while id 2 waits.

## Packages (for the user's morning; before O10, O11 and L8)

Recommended order O4a, O4b, O4c, with O4d optional. Each is independent of the others, apart from
the line numbers.
Since b6bc1d0, O13 and L4 moved them (Opus pass 29): `Store.kt`'s by −11 from its imports on
(`nextAlarm` at 464, `update`'s `daily` check at 738), `MainActivity.kt`'s by +6 from line 266
on; `DailyJob.kt`'s are unchanged.

### [ ] O4a. The alarms never lost while the switch is on (Opus, medium)

(`review-0.7.0-runs.md`, finding 1; for O10 and O14.)
- `DailyJob.runNow`: when `schedule()` returns `RESULT_FAILURE` or throws (caught, logged),
  `runScheduled` false and the pair armed `Store.runRetry` ahead. One helper (`armRetry`) for the
  three direct retry arms (`runNow`'s going branch, `started`, this one).
- **SIM:** `SimPhone` models id 2. An alarm while offline leaves it pending until online; there is
  a failed `schedule()` now and then (1 in 50 alarms). New invariant 9: while `daily` is on and the
  phone is on, after every event an alarm is armed or id 2 is pending. It must fail with the old
  `runNow` mirrored (say so in the report) and pass with the fix.
- **Files:** `DailyJob.kt`, `RulesSimulationTest.kt`; `CLAUDE.md` (Background update, The alarms:
  a failed schedule arms the retry); the skill's Where things are (`DailyJob.kt`).
- **Tablet:** a failed schedule can't be forced from adb. The smoke list, and an alarm's run as
  before (`logcat -s APODroid`: "alarm, run scheduled", the start's pair, the end's arming).
- **Done when:** built; SIM's invariant 9 checked more than 100 times and passing; the tablet
  passes.

### [ ] O4b. A run counts as failed from its start until it ends well (Opus, high)

(`review-0.7.0-runs.md`, The simpler model; finding 5. **A decision first:** the retry measured
from the run's start, not its end. Without the user's yes, only finding 5 is built: `nextAlarm`
without `running`, no behaviour change.)
- **`:core`:** `nextAlarm(now, zone, failures, time, turns, offset, waiting, lastRun)`: the clock's
  run, or with `failures > 0` the earlier of it and `lastRun + retryAfter(failures)`, at the
  earliest now. `retryAfter` public (`runRetry` goes). `nextRun` without `failures`.
- **`Store`:** `runStarts()`: `failedRuns + 1`, `lastRunAt` now, one commit. `ranWell()` (was
  `ranJob`): removes `failedRuns`, only after a good run. `runRetry()`: `retryAfter(failedRuns)`.
  `nextAlarm()` without arguments. `switchDaily` without `runGoing` and `failedAt`. A one-time
  fold of 0.6.0's `runGoing` (`failedRuns + 1`; `runGoing` and `failedAt` removed), a migration
  listed in `CLAUDE.md` (night default; the alternative: accept one lost retry per phone).
- **`DailyJob`:** `noted` calls `ranWell()` only when the run didn't fail; `arm(context)` without
  `runs.going`; the killed-run log line goes.
- **Tests:** `PictureTimeTest`'s armings rewritten to the new signature, each with the same
  instant as before except after a failed end (from `lastRun`; list each changed expectation in
  the report). A new case: a run stopped 10 min after its start is retried 15 min after its start.
  SIM's `jobRun`, `arm` and `switchDaily` mirror the model; invariant 7's expected arming has two
  branches. The update's fold in a `:core` test if it lives there, else a tablet bullet.
- **Files:** `PictureTime.kt`, `PictureTimeTest.kt`, `RulesSimulationTest.kt`, `Store.kt`,
  `DailyJob.kt`; `CLAUDE.md` (Background update: Retries by the alarms, A run that never ends,
  the order at a run's end; Migrations); the skill's Where things are (`nextAlarm`, `runStarts`,
  `ranWell`, `runRetry`).
- **Tablet:** a good `run -f 1`: no `failedRuns`, no `runGoing`, no `failedAt` in the prefs, armed
  by the clock. A failed one (Wi-Fi off): `failedRuns` 1, the pair 15 min after its start. B19's
  checks (`install -r` and `run-as … kill -9` 1–2 s into a run made long V12b's way): the pair at
  the start + 15 min; the next start's pair an hour ahead; `failedRuns` 2 there. The switch off
  and on clears `failedRuns`. The update from 0.6.0's debug build with `runGoing` left by an
  `install -r` mid-run: `failedRuns` 1 and no `runGoing` after it. The smoke list.
- **Done when:** the user's decision taken; built; the tests and SIM pass; the tablet passes.

### [ ] O4c. The switch-on run stops between sets once the switch is off (Opus, medium)

(`review-0.7.0-runs.md`, finding 2.)
- `Store.setDue`: before each set after the first, when the update has no `set` and `daily` is
  off, the loop ends: what was done is returned, nothing is thrown. `Updated.wentToSet` is false
  then (it gave up: B6's last on runs again). The job's runs are unchanged (their stop is
  `onStopJob`'s).
- **Files:** `Store.kt`; `CLAUDE.md` (The daily switch: what "sets nothing more" means for a set
  that has begun).
- **Tablet:** "More NASA pictures" on, home and lock with different sources, a whole picture on
  one (a set of seconds). The switch on, then off within about 1 s: logcat shows one screen set,
  `dumpsys wallpaper` the other unchanged, no toast "Couldn't set". On, off, on within about 1 s:
  both set at the end, at most one screen twice. The smoke list.
- **Done when:** built; the tablet passes.

### [ ] O4d. One stop flag per run (Opus, medium; optional)

(`review-0.7.0-runs.md`, finding 4. No behaviour change; for O10's stops.)
- `DailyJob`: a run's job id, thread and stop flag in one object made at its start. `onStopJob`
  sets the flag of the run with its job id. `stoppedBySystem`, `worker`, `workerJob` and the reset
  go. `update`'s and `saveInUse`'s `stopped` read the run's own flag.
- **Files:** `DailyJob.kt`; the skill's Where things are.
- **Tablet:** `cmd jobscheduler timeout io.github.buerlino.apodroid 2` during a run made long:
  counted as failed, the pair 15 min after. The switch off during a run: "stopped (the switch
  off)". Off and on during a long set: the old run ends without effect, the new one sets once. The
  smoke list.
- **Done when:** built; the tablet passes.

## How sure

- Findings 1, 3 and 5, and the claim that the pair is always armed but for (a) and (b): sure of
  the paths, read line by line. Finding 1's frequency today is unknown and likely near zero.
- Finding 2: sure of the path (`update` reads `daily` once, `setDue` doesn't read it). It needs
  an off within the seconds of the first screen's set.
- Finding 4: sure of the path and that nothing goes wrong today.
- O4b's equivalence: traced by hand against today's `nextAlarm` for each case; not compiled or
  run. The "same instant" claims for B16f1 and B19 follow from `runRetry(n) = retryAfter(n + 1)`
  (`PictureTime.kt:153`) and the count taken one step earlier.
- Not checked: whether Android refuses a foreground service start from a job's thread on the
  tablet (O7, O11); whether `schedule()` of an expedited job can throw rather than return failure
  (O6, O10); `PendingIntent.FLAG_NO_CREATE` after a fired, unrearmed pair (O7, O8, O14).
