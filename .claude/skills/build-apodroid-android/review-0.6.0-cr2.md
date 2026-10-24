# Code review of what changed since 910bfeb (CR2, 2026-10-24)

By reading, no device, no code changes (winston, pipeline run 55, on `dev` at fcd7d60): `git diff
910bfeb..HEAD -- core/src/main app/src/main` read as a whole and checked against `CLAUDE.md`. B18
checks each finding again before building anything.

Files read in full: `core/…/core/` `PictureTime.kt`, `Screens.kt`, `Background.kt`, `JobRuns.kt`,
`Turn.kt`, `Post.kt`; `app/…/apodroid/` `DailyJob.kt`, `AlarmReceiver.kt`, `Store.kt`;
`app/src/main/AndroidManifest.xml`. `MainActivity.kt`: its whole diff since 910bfeb, and in full
the parts this review covers: the scheduling calls (`onResume`, `scheduleJob`, `switchDaily`,
`switchOn`, `setAsWallpaper`, `runNext`, `load`, `follow`), Setup (J3: `Settings`, `ModeSwitch`,
`PictureAtChoice`, `setupChanged`, `setupSummary`), ⛶'s toggle (A4: `keepPart`, `startCrop`, the
whole-picture helpers, the picture's `Box`), `readStatus` and `VideoNotice` (B17). For context:
`Migration.kt`'s stamp, `PictureTimeTest.kt`'s B16f1 test.

Severities: crash or data loss / wrong wallpaper or wrong state / wrong text / tidy.

## Crash or data loss

None found.

## Wrong wallpaper or wrong state

### 1. An alarm armed during a run can fire into it and be dropped, so a run killed afterwards has no alarm left

- **Where:** `app/src/main/kotlin/io/github/buerlino/apodroid/DailyJob.kt:172` (`runNow` returns
  while a run goes and arms nothing), with
  `core/src/main/kotlin/io/github/buerlino/apodroid/core/PictureTime.kt:151` (`nextAlarm` while
  a run goes: the earlier of the clock's next check and `lastRunAt` + 15 min, at the earliest
  now).
- **Failure:** B16f1 keeps a retry for a run killed before its end by never arming later than
  15 min after its start. But an earlier alarm still replaces the run's own pair, and when it
  fires during the run, `runNow` drops it (`runs.going`) and arms nothing. Steps (Zurich, offset
  7 min, the 06:07 check): the fallback (id 1) starts at 06:00:30; `started()` notes
  `lastRunAt` and arms 06:15:30. At 06:02 the user opens the page, so `onResume`'s `ensure` arms
  `min(06:07, 06:15:30)` = 06:07 and replaces that pair (a Set, the daily switch on or a
  `TIME_SET` does the same). The run is still downloading at 06:07: both alarms fire, `runNow`
  sees `runs.going` and returns. The user swipes the app from Recents at 06:09, the process
  dies with the run, and no alarm is armed. Nothing runs until the 6 h fallback's next window
  or the next page open, so the morning's picture can come hours late. This is B16f1's own case
  (a run killed mid-run, e.g. swiped from Recents) through a different door. The same happens
  without a page open when the run lasts past its 15 min alarm: that alarm fires into the run
  too. `PictureTimeTest.aReArmDuringARunKeepsItsRetry` asserts both arms (`:300` "an earlier
  check stays the alarm", `:302` "at once (the floor)"), but no test covers what happens when
  they fire while the run still goes. A run that reaches its end is fine: the end arms again,
  and the dropped check is owed (B16).
- **Fix:** in `runNow`'s branch for a run that goes, arm the pair `firstCheck` ahead
  (`arm(context, now + firstCheck)`, as `started` does) before returning; the run's end replaces
  it.
- **Bug.** Sure of the path (traced through `nextAlarm`, `arm`, `runNow`; the test pins both
  arms). The trigger is narrow: an arming during a run, an alarm landing in the run, then the
  process dying before the run ends.

### 2. A stop during a set is swallowed: the run goes on with the other screen's set and "Save every picture"

- **Where:** `app/src/main/kotlin/io/github/buerlino/apodroid/Store.kt:743` (the record's
  `commit()` after each set in `setDue`), with
  `app/src/main/kotlin/io/github/buerlino/apodroid/DailyJob.kt:71` (`stopped` read only from the
  thrown exception).
- **Failure:** `onStopJob` interrupts the worker. A set (`WallpaperManager` over binder) ignores
  the interrupt, and with the whole picture (three decodes, then `setBitmap`'s PNG compression)
  it lasts seconds. The `commit()` right after it then runs on an interrupted thread.
  `SharedPreferencesImpl.commit` waits on a `CountDownLatch`, whose `await()` throws
  `InterruptedException` at once and **clears the flag**. `commit` catches that and returns
  false without calling the listeners (the effect B16's comment at `DailyJob.kt:80` describes).
  From then on the thread looks as if nothing stopped it:
  - `setDue`'s loop goes on to the other screen's set (it has no stop check of its own);
  - `update` returns normally, so `stopped` is false and `saveInUse` runs. With "Save every
    picture" on, the job downloads originals (up to "Largest original": 20 MB by default, or no
    limit) after Android stopped it, outside the job's protection, against `CLAUDE.md`'s "not
    after a stop" (Save, Background update);
  - the open page doesn't hear the record write (no listener), so its status line and chip stay
    old until the next resume or redraw.
  `ended(failed = stoppedBySystem)` still counts the run as failed, so the retry comes. Steps:
  "Save every picture" on, two screens due different pictures; the network drops (JobScheduler
  stops a job whose network constraint fails) while the home screen is being set.
- **Fix:** `DailyJob.kt:71`: `val stopped = stoppedBySystem || …`. In `setDue`, stop after a set
  whose record `commit()` returned false (re-interrupt the thread, or check a stop flag before
  each set).
- **Bug.** Fairly sure (about 80%): it follows from AOSP's `SharedPreferencesImpl.commit` and
  the JDK's `CountDownLatch.await`, as I remember them. Not seen on a device. The trigger needs
  a stop within the seconds a set takes.

### 3. (low) With one source, the post due at a change time never shows if a newer one is stored first

- **Where:** `core/src/main/kotlin/io/github/buerlino/apodroid/core/Screens.kt:287` (`within`,
  one source), `:298` (`inWindow` judges the newest stored post only).
- **Failure:** only each source's newest post is stored. If the first run after a change time T
  sees a post published after T + 3 h, that post replaces the one due at T in storage. The newest
  then waits for the next change time, and the screen keeps an older picture meanwhile. Steps
  (Los Angeles, APOD alone, 06:00): the phone is off or offline from 20:00 on day D−1 to 22:00 on
  day D (a flight). At 22:00 the fetch stores D's 21:05 post, published after D's 09:00, so
  nothing is set. The screen shows D−2's picture until 06:00 on D+1, and D−1's post, due at 06:00
  on D, never shows. The same goes for NASA alone in Europe (every NASA post is after 09:00) and
  Earth in the Americas. A gap of less than a day is safe, because some run (a check or the
  fallback) stores the due post before the next one comes out. `CLAUDE.md`'s rule ("the newest
  stored picture published before T + 3 h") is kept to the letter: the newest stored picture
  just isn't that post any more.
- **Fix:** accept (a day-long gap), or set the newest anyway when the screen's picture of the
  source is more than a day older than the newest stored post's date.
- **Decision.** Sure of the path. A rare trigger.

## Wrong text

None found. Checked: the Set and switch-on toasts against `setsSinceSetup` and `Updated`, the job's
log lines, the Setup summary (stored setup, `hours24`), "New picture at"'s labels and screen-reader
text, the whole-picture toggle's state description, and B17's video notice against `standInReady`.

## Tidy

### 4. A Set that changes "New picture at" or "Change every" starts a spare run at once

- **Where:** `core/src/main/kotlin/io/github/buerlino/apodroid/core/PictureTime.kt:113` (a check
  after `lastRunAt` that has passed is owed), reached through
  `app/src/main/kotlin/io/github/buerlino/apodroid/Store.kt:402` from Set's `scheduleJob`.
- **Failure:** the owed check uses the new clock's checks. Steps: the last run's start is 06:10
  ("New picture at" 06:00). At 10:00 the user sets 08:00. With an offset of 7 min, the new clock's
  08:07, 08:22 and 09:22 checks are after `lastRunAt` and passed, so the alarms come at once. The run fetches each
  source in use whose post isn't today's (NASA and Earth before their US afternoon post) and sets
  nothing, because Set has just stamped every screen. One spare check per such Set.
- **Fix:** accept. Noting `lastRunAt` at Set would drop a check owed before it: Set's update
  (`page = true`) skips a source checked within the hour, so it can miss the post that check
  was for.
- **Decision** (harmless as it is). Sure of the path.

## Checked and found right

- **The alarms and the jobs:** each path that arms, re-arms or cancels: a run's start
  (`started`, under `lock`), its end (`ended` → `ensure`, after `jobFinished` so the fallback
  isn't rescheduled under itself), `onResume`, Set, the switch on and off, the receiver's boot,
  time zone, `TIME_SET` and package-replaced broadcasts (exempt from the implicit-broadcast
  limits or sent to the app itself, and the system reaches a non-exported receiver), and the alarm's `runNow`. `cancel` comes after the `daily` write
  under the same `lock` as a run's start and end, so a run never arms after an off. Id 1 and id 2:
  one run at a time, a stale id 2 replaced, `onStopJob` only for the running id. A check during a
  run is owed at its end (B16). The only gap found is finding 1.
- **The rules against the clock:** DST (gap and repeated hours, the three real hours), a time
  zone change (old stamps aren't change times → current), the clock set back (stamp and
  `lastRunAt` ahead), the stamps after Set (current change time of the new `PictureTime`) and
  after the migration (`turnStartedAt`, not a change time), equal columns (lock follows home's
  record; one set with both flags), the stand-in (its own publish time, the B15 note), a failed
  set (record unwritten, retried; the stamp kept, so `waiting` checks hourly), never back. The
  publish time: APOD's and Earth's `date` read as US Eastern, NASA's `pubDate` with its zone, a
  post from before B15 at its date's 00:00 Eastern.
- **Memory (the whole picture):** the edge copy is decoded straight to at most 200 px wide; the
  blur copy is sampled by the middle frame (a 3726×2484 rendition decodes at 931×621 for a
  1080×2400 screen) and drawn to a quarter-size copy; the picture is decoded straight to the
  frame's size. Peak is about two screen-sized ARGB bitmaps (about 20 MB on the FP6). The blur and
  the picture are recycled, and the result is left to the GC after `setBitmap`, as
  `decodeUpright`'s is.
- **Prefs and files:** `Stored.files` from the files, `isWhole`/`cropPosition` keyed by the
  part's post, `published` written or removed with each post, `saveSetup`'s rename before
  `restartAndForget`. The known gap (picture renamed before the prefs) is unchanged.
- **`CLAUDE.md` against the code:** Background update (alarms, passed checks, armed again, the
  fallback, one run at a time, retries by the alarms, off is off), Turns and the picture time,
  the checks, Settings' "New picture at" and the highlight, ⛶'s "Whole picture", Wallpaper's
  whole-picture drawing, Fetch's `published`: no contradiction found besides what findings 1
  and 2 say.
