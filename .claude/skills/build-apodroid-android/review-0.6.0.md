# Code review of 0.6.0 (CR, 2026-10-22)

By reading, no device, no code changes: every file in `core/src/main` and `app/src/main` checked
against `CLAUDE.md` (winston, pipeline run 42, on `dev` at cb0e64d). B14 checks each finding
again before building it.

Files read in full: `core/…/core/` `Post.kt`, `Screens.kt`, `Turn.kt`, `Saving.kt`,
`SwitchRuns.kt`, `JobRuns.kt`, `Frame.kt`, `Migration.kt`; `app/…/apodroid/` `MainActivity.kt`,
`Store.kt`, `DailyJob.kt`; `app/src/main/AndroidManifest.xml`; `res/xml/data_extraction_rules.xml`,
`res/mipmap-anydpi/ic_launcher.xml`, `res/drawable/ic_launcher_background.xml`;
`res/drawable/ic_launcher_foreground.xml` and `ic_launcher_monochrome.xml` (generated vector
paths: only the headers and the path structure checked, not every coordinate).

Severities: crash or data loss / wrong wallpaper or wrong state / wrong text / tidy.

## Crash or data loss

None found.

## Wrong wallpaper or wrong state

### 1. A column with turns can set a source that isn't ticked in it

- **Where:** `core/src/main/kotlin/io/github/buerlino/apodroid/core/Screens.kt:208` (and `:209`;
  `dueOn`)
- **Failure:** `nextSource(…) ?: r.owner` keeps the old owner when no ticked source has a
  picture, even when that owner isn't in the column. Then `show(r, stored.picture(r.owner))`
  sets the old owner's picture. Steps: APOD mode, Where = Both. Go offline (or NASA's and
  Earth's sites fail). Turn "More NASA pictures" on, tick NASA and Earth on both screens, untick
  APOD, tap Set. `restartAndForget` keeps the owner APOD with the new stamp and no picture, so
  `dueOn([NASA, EARTH])` takes the second branch. `nextSource` returns null, the owner stays APOD
  and APOD's picture is due with `set = true`. Result: the toast says "Wallpaper set", APOD's
  picture goes on again (in Fit too, if Size changed), and the status line says " · APOD". While
  NASA and Earth still have no picture, each new APOD post is set on screens where APOD isn't
  ticked. That contradicts "A picture goes on the screens its source is ticked for" and Set's
  "each screen gets what has a picture". A single-source column is safe: its branch at `:205`
  sets the owner to `column.single()`.
- **Fix:** when `nextSource` is null and the owner isn't in the column, use `column.first()` as
  the owner. Its picture is null, so the screen keeps its picture. Regression test: column
  `[NASA, EARTH]`, a record owned by APOD with no picture, only APOD stored → no set.
- **Bug.** Sure of the code path (traced by hand). The trigger needs both ticked sources without
  a stored picture.

### 2. Rotating (or leaving) the page during Set loses the job's fresh schedule and the toast

- **Where:** `app/src/main/kotlin/io/github/buerlino/apodroid/MainActivity.kt:773` (the steps
  after `withContext` in `setAsWallpaper`, from `:765`). The same pattern is at `:459`
  (`switchRuns`) and `:902` (`saveFallback`).
- **Failure:** Set's follow-up runs in `lifecycleScope`, which is cancelled when the activity is
  destroyed. A rotation destroys it, since the manifest declares no `configChanges`, and so does
  Back on Android 10–11. The blocking `saveSetup` + `update` on IO still finishes, but the code
  after `withContext` never runs: no `scheduleJob()`, no toast, `setting` is not reset. The new
  page's `setting` is false anyway. Steps: "More NASA pictures" on, two sources on Home, the
  daily switch on, tap Set and rotate within the second or two the set takes.
  - The clock restarted to now, but the job keeps its old windows. The new page's `onResume`
    `ensure` sees the same timing and leaves it, so the first turn ends up to one period late.
    That is what CLAUDE.md says Set's `DailyJob.schedule` prevents.
  - No toast.
  - Set is enabled again in the new page while the first Set still runs.
  - Same root cause: an on of the daily switch that is owed during a load (`switchRuns`, only in
    the old page) is lost on rotation, so `startAfresh` never runs. A pick whose copy is still
    running is lost too: `draftFallback` stays false and the choice isn't made, though the file
    is there.
- **Fix:** do the scheduling inside the IO block after `update` (JobScheduler is fine off the
  main thread) and return only what the toast needs. Or run Set's whole sequence in a
  process-wide scope.
- **Bug.** Sure (standard `lifecycleScope` behaviour). The consequence for the turn timing
  follows from `ensure`'s timing check.

### 3. After a Set in APOD mode, the list no longer starts from "Where"

- **Where:** `app/src/main/kotlin/io/github/buerlino/apodroid/MainActivity.kt:834`
  (`switchAllSources`)
- **Failure:** the draft's `list` is set only in `onCreate` (`useDraft`, `:339`), `place` and
  `switchAllSources`. In APOD mode, Set doesn't save the list (`Store.saveSetup`, `Store.kt:250`),
  so `store.list` follows the new stored "Where", but the draft's `list` keeps the old one. Steps:
  fresh 0.6.0 (no list saved), Where = Both. Set Where = Home screen, tap Set. Then, in the same
  page life, turn "More NASA pictures" on. `list == store.list` compares `{HOME:[APOD],
  LOCK:[APOD]}` with `{HOME:[APOD], LOCK:[]}`, which is false, so the list shows "APOD: Both"
  instead of starting from Where (Home screen), highlighted as changed. A Set then puts APOD on
  the lock screen too. CLAUDE.md says: "The first time on, the list starts from the draft's
  'Where'".
- **Fix:** after a Set that saved, `if (!store.hasList) list = store.list`. Or note in the draft
  whether the list was edited, and compare against that instead of `store.list`.
- **Bug.** Sure. A rotation in between doesn't help: the saved state keeps the stale list.

### 4. (low) The page can miss a later change while it follows an earlier one

- **Where:** `app/src/main/kotlin/io/github/buerlino/apodroid/MainActivity.kt:371` (`follow`)
- **Failure:** `follow` returns while a follow is active. The active one read the prefs
  (`readStatus`) before it suspended in `show()` or `readSaved()`, so a record or a problem the
  job writes during that suspension isn't drawn. Example: APOD mode, page open. The job stores a
  new post, then `setDue` writes the records of screens that need no set (`write(plan)`, right
  after the refresh with nothing in between), while the page decodes the new picture. The chip
  and the status lines stay stale until the next event or `onResume`. The code comment's "once
  is enough" holds only when nothing arrives during the suspension. The set itself takes seconds,
  so the usual last write (the set's record) mostly comes after `show()` ends.
- **Fix:** when a follow is active, note that one more is owed and run it at its end (as
  `loadAgain` does).
- **Bug** (the page should follow the job's sets). Plausible: it depends on timing, not seen.

## Wrong text

### 5. Set during a load or a job run says "Setup saved. Nothing to set yet." though it was set

- **Where:** `app/src/main/kotlin/io/github/buerlino/apodroid/MainActivity.kt:757` (Set waits
  only for `setting`/`switching`; the button at `:1441` likewise). `:426`: `load` doesn't wait
  for `setting`.
- **Failure:** with the daily switch on, a page load (`onResume`, a pull, choosing "More NASA
  pictures" or a video-day choice, all of which call `load`) or a job run that reaches `setDue`
  after Set's `saveSetup` commit sets the new setup and writes the records. Set's own `setDue`
  then finds nothing due, and its toast says "Setup saved. Nothing to set yet." instead of
  "Wallpaper set". A failed set in the other run isn't reported as "Couldn't set the wallpaper"
  either. Steps: open the page on a slow connection so `onResume`'s load is still fetching,
  change Size, tap Set. The load's `setDue` (lock order: `saveSetup` first, then the load's
  `setDue`) sets Fit, and Set's toast says nothing was set. `onResume`'s `DailyJob.ensure` during
  a Set without turns does the same: a fresh job runs at once.
- **Fix:** Set waits for a running load, like `switchOn`: if `loading`, note a Set and start it
  from `runNext`. And `load` waits while `setting` (`if (loading || switching || setting)`). The
  job case is rare; it could report the screens whose record this Set's restart stamped and that
  a set reached since.
- **Bug** (CLAUDE.md: "Wallpaper set" when a screen was set). Sure for the page's load. The job
  case needs a run going at the moment of the Set.

## Tidy

### 6. ★'s and the job's original downloads can be left in the cache for good

- **Where:** `app/src/main/kotlin/io/github/buerlino/apodroid/Store.kt:774` (`save`)
- **Failure:** each save makes a new `File.createTempFile("original…", ".part", cacheDir)` and
  deletes it in `finally`. A process death during the download (the system's low-memory kill,
  a force stop, an app update) leaves it behind with a new random name each time: up to the
  limit for the job, or a whole original for ★ (NASA's go up to 97 MB, one was 208 MB). Nothing
  deletes `original*.part`. Android clears `cacheDir` only under storage pressure. The fixed
  names (`<source>.part`, `fallback.part`) are overwritten next time, so they're fine.
- **Fix:** one fixed name per source (`cacheDir/<source>-original.part`; one save at a time
  anyway), or delete `original*.part` at `Store`'s first use.
- **Bug** (storage leak, no `CLAUDE.md` rule). Sure.

### 7. "Setup" is highlighted (and opens by itself) for settings it doesn't show

- **Where:** `app/src/main/kotlin/io/github/buerlino/apodroid/MainActivity.kt:1476`
  (`setupChanged`, through `:1478`)
- **Failure:** `interval != stored.interval` and `videoDaysChanged()` count even while "Change
  every" is hidden (APOD mode, no column with two, the daily switch off) and while "If APOD is a
  video" is hidden (the daily switch off). Steps: the daily switch on, the list with two on Home,
  choose "6 h" in "Change every", then untick one (or turn the daily switch off). "Setup" stays
  in `tertiary` and opens by itself, but nothing in it is highlighted. Set then saves the hidden
  interval.
- **Fix:** count `interval` only while "Change every" shows, and the video choice only while the
  daily switch is on. Or reset a hidden draft value to the stored one.
- **Decision** (what the highlight covers; CLAUDE.md names "Change every" and "If APOD is a
  video" without saying "while shown"). Sure of the code.
