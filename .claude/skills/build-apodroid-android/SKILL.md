---
name: build-apodroid-android
description: Execution brief for building APODroid (io.github.buerlino.apodroid) — a native Kotlin/Jetpack Compose app that sets NASA's Astronomy Picture of the Day as the wallpaper once a day, with one page showing today's picture and its settings, distributed via F-Droid and Obtainium. Use this skill whenever working in the APODroid repo: the APOD fetch and parsing in :core, the Compose page, the wallpaper and the daily background job, building/installing on the phone, and releases. The stack (native Android, no Flutter/React Native/KMP) is already decided — do not re-open it; just execute.
---

# Build APODroid

How-tos and what's still untested. `CLAUDE.md` at the repo root is the source of truth for the
stack, the data source and the decisions. Read it first. If this skill and `CLAUDE.md` disagree,
`CLAUDE.md` wins; update this skill to match. The history is in git. The next features to build,
with a checklist: `features.md` next to this file. The work for 0.6.0: `plan-0.6.0.md` (0.4.0's and
0.5.0's, done, are in git). Night shifts (agents working through the
packages unattended): `night.md`.

## Where things are

- `core/.../Post.kt`: `Source`, `Post`, `fetchLatest(source, userAgent)`, `parseLatest`
  (`parseApod`, `nasaPosts`, `earthPosts`), `download(url, to, userAgent, limit)` (`TooLarge` over the limit, F6), `renditionUrl`,
  `originalUrl`, title, video, explanation and credit parsing (`apodPosts`, `credit`), `Post.fileName`,
  `Post.published` and `publishedAt` (B15: the publish time, `wpPublished` for WordPress's `date`). Tests with made-up data
  shaped like the real responses: APOD in `ApodTest` (also the HTTP part), NASA, Earth and the
  URLs in `SourcesTest`, whose `readsEveryRealPost` reads `private/` when it's there. Real
  responses in `private/` (`latest.json`, `last100.json`, `iotd-feed-2026-10-16.xml`,
  `eo-last40-2026-10-16.json`).
  `core/.../Frame.kt`: `wallpaperFrame`, the part of the picture the wallpaper shows, which is
  also the crop hint, and `fitFrame`, where the whole picture goes on the screen (F1's Fit; ⛶'s whole picture since A3)
  (tests in `FrameTest`; a hint widened for landscape was tried and dropped, `CLAUDE.md`,
  Wallpaper).
  `core/.../Background.kt` (0.6.0, A1): the background behind a whole picture: `edgesBlack`
  (black or blurred, from a small grey copy; `gray` for an ARGB pixel; `edgeCopySize`, the copy's size, A3), `blurSize`,
  `blurRadius`, `blurBackground` (tests in `BackgroundTest`).
  `core/.../Turn.kt`: `nextSource` (the next ticked source in order for which a test holds;
  tests in `TurnTest`; the Set clock, `isTurnOver` and `turnSlack`, went in J1).
  `core/.../PictureTime.kt` (0.6.0, J1): `PictureTime(pictureAt, everyHours)` with `hours`,
  `last`, `next`, `isChangeTime` (the change times in a zone, on the clock's hours through DST),
  `pictureWindow` (3 h), `checkGap` (24 h) and `afterGap(lastCheck, now)` (a new post stored after more than a day without a successful check, B21), `firstCheck` (15 min), `nextRun(now, zone, failures, time, turns,
  offset, waiting, lastRun)` (when the alarms should run next; at `now` for a check passed since
  `lastRun`, B16), `nextAlarm(now, zone, failures, failedAt, …, lastRun, running)` (while a run goes no later than `lastRun` + `runRetry(failures)`, B16f1, B19), `runRetry(failures)` (a run's own retry should it never end: 15 min, an hour after one or more failures, B19)
  (J2: the same, armed at any time, keeping a failed run's retry), `randomOffset` and `maxOffset`
  (tests in `PictureTimeTest`, also whole days in Zurich, Helsinki, Los Angeles, Sydney, Tehran,
  New York and UTC with a run at each `nextRun`, offline spells and pulls, B15).
  `core/.../Screens.kt` (0.6.0, M1; `Store` uses it since M2): `Screen`, `apodColumns`,
  `screensOf` and `placing` (S2: the list's change, a source on exactly its screens),
  `Picture` (a source's post or `Own`), `StandIn`, `Stored` (posts, files, the stand-in, the posts after a gap:
  `picture(source)`, `isNew(source, shown)`, `publishedBefore(source, limit, orAfterGap)`, B15, B21), `Record` (owner, picture, stamp: the change time,
  `shown`: the last post shown per source; `forget()`), and the pure functions returning a
  `Plan` (records, `sets` grouped by picture): `dueWallpapers(columns, records, stored, time,
  zone, now, pull)` (each run, J1's three rules; equal columns: the lock screen follows home's
  record and picture, B2), `waiting` (a screen with turns that found nothing new, for `nextRun`),
  `restartAndForget(before, after, records, stored, time, zone, now)` (S4, Set: the pictures of
  the screens with a column forgotten, each stamped with the current change time; a `Restart`
  with the plan and the screens whose record to `drop`, a column that was empty), and
  `videoDayKept` for the page's status line (tests in `ScreensTest`).
  `core/.../Saving.kt` (0.6.0, F5): `sourcesInUse` (what the job fetches and saves) and
  `toSave` (Save every picture's choice; tests in `SavingTest`).
  `core/.../Migration.kt` (0.6.0, M2): `migrate`, 0.5.0's prefs → 0.6.0's keys, run once by
  `Store.migrateOnce` (tests in `MigrationTest`).
- `app/.../Store.kt`: per source (`post(source)`, `imageFile(source)`; APOD's keys unprefixed
  and `apod.jpg`, the others `nasa.<key>` / `earth.<key>` and `nasa.jpg` / `earth.jpg`) prefs (also `daily`, `pauseHintHidden`; `checkedAt`, `problem`, `httpCode`, `afterGap` (B21, written by `refresh` with a new post),
  `wallpaperSetAt` for the problem and status lines), `update(force, page, show, set)` (what the
  page, the pull, Set and the job run: refresh the sources in use and `show`, noting how the
  check went, then set while `daily` is on or with `set`; returns `Updated`: what it set,
  `setFailed`, the first failure; throws only a stop), `refresh(source)`
  (the picture through `downloadPicture`: the rendition, else the plain URL), `allSources`,
  `list` (the list's columns in both modes; before the first save APOD on "Where"'s screens,
  `hasList`), `columns` (the mode's), `hasTurns`, `pictureAt` (J2, pref `pictureAt`, default
  `DEFAULT_PICTURE_AT` 6), `pictureTime()`, `offset` (pref `offset`, seconds, drawn once),
  `nextAlarm()` (when the alarms go: `:core`'s `nextAlarm` with `failedRuns`/`failedAt` and
  `lastRunAt`, not under a lock; `runGoing` counts as a run going, B19), `runStarts()` (a job run's start: `lastRunAt` and `runGoing`, B16, B19; a `runGoing` still there counts the killed run before in `failedRuns`, returns `true` then), `runRetry()` (B19) and
  `ranJob(failed)` (a job run's end: counts or clears the failures, removes `runGoing`),
  `setup` (the stored `Setup`: mode, "Where", list, interval, video days, `parts`, ⛶'s
  `Part` per source with its `whole`, `pictureAt`) and
  `saveSetup(setup, pick)` (S4, Set: one commit with the settings, `putColumns`, the parts and
  `restartAndForget`'s plan; returns whether the commit was written, B20; `draftFallbackFile` renamed to `fallbackFile` first, only with `pick`, the page's own `draftFallback` (B11); not `daily`), `setsSinceSetup()` (after Set's update: whether any run set a screen since `saveSetup`, and whether one's last set failed; Set's toast, B14),
  `switchDaily(on)` (S4f2: writes `daily` at once, clears the job's failures) and `startAfresh()` (the switch on:
  `restartAndForget` with the stored columns),
  `lastScreens(source)` (S2: an unticked row's screens, `<source>.lastScreens`, written with the list), `record(screen)` (home unprefixed, lock `lock.`;
  `screenState(screen)`, a `ScreenState` (picture, set time, `kept`) for the page; `pageScreen`
  is APOD mode's screen), `stored()` (posts, files,
  the stand-in for `:core`), `setDue()` (`dueWallpapers`, then the sets per screen, under the
  wallpaper lock), `saveInUse()` (the job's saves, `saveEvery`, `saveLimit`), `tabPicture(tab)`, `set(picture, screens)` (the flags per screen; a photo with
  an EXIF orientation goes upright through `setBitmap`; with ⛶'s whole picture (`isWhole(post)`, A3) the `whole` bitmap: `hasBlackEdges` on black, else on `blurred`), `cropPosition(post)`, `save` (the original, else the stored picture; the job's `limit`; returns `Saved`; its temp file `cache/original.part`, leftovers dropped by `dropSaveLeftovers` at the page's first start and by each save, B13), `imageBounds`,
  `wallpaperFrame` (`:core`'s as a `Rect`).
- `app/.../MainActivity.kt`: the page, one instance (`launchMode="singleTop"` in `AndroidManifest.xml`, B4; `onNewIntent` does nothing) (`PictureSide`: `SourceTitle`, the shown source's name with its dropdown, and `PicturePage`, each source's picture, in a `HorizontalPager` whose settled page is `shown` (`tab`, `showTab`), then `Caption`, the boxes and the caption of the shown `post`; `show()` loads `posts` and `pictures` of every offered source (`sources`; `apod` decides them in APOD mode), `saved` per source;
  `screens` and `stored`, the stored `Setup`, from `readStatus`, `chipText`, `statusLines`, `readProblem`, `VideoNotice`: what is set), with `checkPausing()` and the `PauseHint` in a `NoticeBox` below the picture;
  the daily switch (`daily`, at once: `switchDaily`; on runs `switchOn`, then `scheduleJob(context)` (`DailyJob.ensure`: the alarms and the fallback) after its set, as Set's; Set's and the switch-on's runs go in the companion's process-wide `runs` scope with `setting`, `switching` and `switchRuns`, so a rotation or Back keeps their end, and `redrawAfterRun` goes to the page in `live` under the starter's `id`, kept by a rotation, B13; on while a load, Set or switch-on runs: owed in `switchRuns` (`core/.../SwitchRuns.kt`, `SwitchRunsTest`), run by `runNext` after it, as `loadAgain`, unless the switch-on run went on to set, `Updated.wentToSet`: B6); the draft (S4: `where`, `list`, `interval`, `videoDays`, `parts`, `draftFallback`,
  `allSources`; `draft()`, `useDraft`, `columns`; kept by `onSaveInstanceState` with `toBundle`/`readDraft`; `pages`, the live pages: a start deletes `fallback-draft.jpg` only without a saved draft and with no other page, B8;
  ⛶ `startCrop`, ✓ `keepPart`, `draftPosition`, `storedPosition`; A4's toggle `cropWhole`, `draftWhole`, `storedWhole`); the settings (`Settings`: `Header` for "Setup" and "Advanced settings", `setupOpen`/`advancedOpen`
  in the saved state, `setupSummary` of the stored setup; the highlight: `setupChanged`, `rowChanged`, `videoDaysChanged`, `highlight`, `Segments`
  (the segmented rows), `DropdownButton`; with the switch "More NASA pictures" `PictureList`; `VideoDaysChoice`, the
  dropdown; `switchAllSources`, `place(source, screens)` (the list's tick, untick and dropdown), `chooseVideoDays`,
  all in the draft; `setAsWallpaper()`, the one button: `saveSetup`, `update(set = true)`, `setsSinceSetup()`, then the alarms and the fallback armed (`scheduleJob`) or cancelled by the stored `daily`; `draft()` with the draft's `pictureAt` (J3); `ModeSwitch` at Setup's top, `PictureAtChoice` and `hourLabel` (J3));
  `zone` and `hours24` (the status line's and the problem box's times) from `readClock`, on resume and by `clockChanged` while resumed, B13; `follow()` redraws while resumed when `Store.watch` sees the job set (either screen's record) or store a post, not while a load, a Set or the switch-on runs, once more when called during one (`followAgain`, B14); `saveFallback` copies a pick in `runs` (B14); `dropVideoDays()` and two `LaunchedEffect`s in `Settings()` put a hidden draft value back (B14).
  `app/.../DailyJob.kt` (J2): the run (`onStartJob`: ends at once while a run goes, B16; `started` notes `runStarts` and arms the alarms `Store.runRetry()` ahead (15 min, an hour after a failure, B19); a run the switch off dropped logs "stopped (the switch off)", no failure, B19; `update`, then `saveInUse` unless stopped; `jobFinished(…, false)` and `onStopJob` false, no Android retry; the interrupt flag cleared, then `ended` notes `ranJob` and calls `ensure`, unless an off came since, `stoppedBySystem` (reset at each start) counts as failed), two jobs: id 1 the 6 h fallback (`periodic`), id 2 the alarms' one-off (`runNow`, not while a run goes or while this process's own is pending, `runScheduled`; a stale pending one is replaced, B16); `ensure` arms the pair of alarms (`arm`: `setWindow` with request code 1, `setAndAllowWhileIdle` with 2, for `Store.nextAlarm(running = runs.going)`, B16f1) and schedules the fallback when missing or with another timing, not while a run goes; `cancel` (off) drops the generation and cancels both jobs and both alarms. `app/.../AlarmReceiver.kt` (J2, not exported): `ACTION_RUN` → `runNow`; `BOOT_COMPLETED`, `TIMEZONE_CHANGED`, `TIME_SET`, `MY_PACKAGE_REPLACED` → `ensure` while `daily`. The run count and the generation are `core/.../JobRuns.kt` (`JobRuns`: `start()`, null while a run goes, `counts(generation)`, `finished(generation)`, `going`, `drop()`; `JobRunsTest`).

## Working on the phone

- Build and test: `ANDROID_HOME=~/Android/Sdk ./gradlew :core:test :app:assembleDebug`.
- Real phone over USB, no emulator. If `adb devices` is empty and `lsusb` shows `18d1:4ee1` (MTP
  only), USB debugging is off or not authorised: ask the user. If adb says "no permissions" and
  `lsusb` shows `05c6:9024` (Qualcomm), the phone is in "Charging only" USB mode (so after each
  reboot): ask the user to set it to "File transfer". `adb` isn't on PATH:
  `~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk`.
  Debug and release builds are signed with different keys, so switching needs an uninstall.
- The phone (LineageOS) may block network access for a new install (`dumpsys netpolicy` shows
  the UID with `REJECT_ALL`; in the app an `UnknownHostException`). The user allows it in App
  info → Mobile data & Wi-Fi → Network access. Not every uninstall triggers it.
- Release build on the phone (R8 isn't covered by unit tests): sign the unsigned release APK
  with `~/.android/debug.keystore` via `zipalign -p 4` + `apksigner`; that installs over debug
  builds.
- Job: force a run of the fallback with `adb shell cmd jobscheduler run -f io.github.buerlino.apodroid 1`,
  of the alarms' run with `… run -f io.github.buerlino.apodroid 2` while it's pending (an alarm
  fired and it waits, e.g. for the network: with Wi-Fi off `run -f` forces it past the
  constraint, so it fails), list with `adb shell dumpsys jobscheduler | grep io.github.buerlino.apodroid` (`Last successful
  run` outlives logcat). The alarms (J2): `adb shell dumpsys alarm | grep -A3 apodroid` lists the
  pair (two `PendingIntent`s, request codes 1 and 2, the same time; `RTC_WAKEUP`); the job logs
  `Daily job: alarms for <UTC time>` each time it arms them, `Daily job: alarm, run scheduled`
  when one fires, `Daily job: jobFinished (job <id>)` at a run's end. Since J2 no run asks
  Android for a retry: after a failed run no backoff job is pending, the alarms come 15 min later. The job logs `Daily job: wallpaper set: Of(source=APOD, date=…, url=…) on HOME+LOCK`
  (`Own` for "my picture"; one entry per set), `Daily job failed: …`, `Daily job: the run before never ended
  (killed), counted as failed` and `Daily job: stopped (the switch off)` (B19) under `adb logcat -d -s APODroid`; the page the same as `Page:
  …` and `Update failed`, Set as `Set: …`, the switch as `Switch on: …`. Nothing is logged when there was nothing to set (a kept video day neither). Pull to refresh:
  `adb shell input swipe 400 300 400 1100 600` (tablet coordinates). Crashes:
  `adb logcat -d | grep 'E AndroidRuntime'` (the `I AndroidRuntime` lines are adb's `content`
  and `am` tools starting); after `logcat -c`,
  `adb shell dumpsys activity exit-info io.github.buerlino.apodroid` still lists each exit's
  reason (a crash is `CRASH`).
- Start the app with the launcher's intent, as a user does:
  `adb shell monkey -p io.github.buerlino.apodroid -c android.intent.category.LAUNCHER 1`.
  `am start -n io.github.buerlino.apodroid/.MainActivity` works too; since B4 (`singleTop`)
  either start reuses the page on top (before, mixing the two stacked a second page without the
  draft). One page: `adb shell dumpsys activity activities | grep 'Hist #.*apodroid'` shows one line.
- `am force-stop` cancels the app's jobs and both alarms; the next start (the page, or Android 15
  delivering `BOOT_COMPLETED` as the app leaves the stopped state) arms them again (pref `daily`).
  Use `am kill` to restart the app and keep them (it doesn't kill while a Set runs: `run-as
  io.github.buerlino.apodroid kill -9 <pid>`). `pm clear` cancels them and the pref.
- The hint about Android pausing the app: undo "Don't show again" with
  `adb shell run-as io.github.buerlino.apodroid sed -i '/pauseHintHidden/d' shared_prefs/apodroid.xml`
  (after `am kill`). The two settings by adb: `keep-running.md`, How to test.
- The phone's `sed` (toybox) doesn't know `\|`: use one `-e '/x/d'` per pattern. Edit prefs
  only after `am kill`, and `am kill` only kills an app in the background (press Home first);
  else the running app keeps and later writes back its old values. `adb shell` hands the
  command to the phone's shell as one line, so a `>` in a `sed` pattern becomes a redirect
  there: match the line by an address instead, `-e '/wallpaperDate/s/<today>/<yesterday>/'`.
  The phone's shell also strips double quotes, and a broken line makes Android drop the whole
  prefs file: match `name="date"` as `/name=.date/`, keep the pattern in single quotes, and
  `cat` the file before and after. The desktop shell strips those single quotes first, so a `|`
  in the pattern becomes a pipe on the phone: wrap the whole command in double quotes,
  `adb shell "run-as <pkg> sed -i -e '/x/s,a,b,' shared_prefs/apodroid.xml"`.
- Fake "nothing has worked for two days" (the problem line's "since"): offline, after `am kill`,
  `checkedAt` minus 172800000 and the stored `date` one day back (a page open on a current day
  counts as a working check and resets it).
- Fake an old day (debug builds only; `run-as` doesn't work on release builds), after `am kill`:
  `adb shell run-as io.github.buerlino.apodroid sed -i -e 's/<today>/<yesterday>/g' shared_prefs/apodroid.xml`.
  A video day the same way, by setting `isVideo` to true. To run the job on the stored picture
  (e.g. with a moved position), fake only `wallpaperDate`, then `run -f`; faking all dates moves
  `cropDate` too, which is right.
- End a turn (debug builds; since J1, 2026-10-23, the turns follow the change times, so
  `turnStartedAt` and `private/wp7-handoff/turn.sh` no longer do anything): either fake the
  clock past the next change time (`settings put global auto_time 0`, `cmd alarm set-time
  <ms>`, then `run -f`; set `auto_time` back to 1 at the end and check the clock against the
  desktop's `date`), or move a screen's record back one change: after `am kill`, read
  `wallpaperStamp` (`lock.wallpaperStamp` for the lock screen; equal columns follow home's) and
  `sed -i -e '/wallpaperStamp/s,<value>,<value - interval in ms>,'`, then `run -f`. A stamp
  that isn't a change time counts as the current one and changes nothing, so subtract exactly
  the interval (10800000 for 3 h, 86400000 for one source). A screen changes only to a source
  whose newest post it hasn't shown: delete its `shownDate`/`shownUrl` keys (`nasa.`, `earth.`
  for the others, `lock.` before them for the lock screen) to make a post count as new.
  `swap.sh debug|r8 [sed args]` (install a build, fake prefs) and `ui.sh` (the page's nodes with
  their tap coordinates: rows move when a tick changes) in `private/wp7-handoff/` still work.
  Each `run -f 1` pushes the fallback's next window a period further; since J2 nothing
  schedules it afresh (its timing never changes), the alarms are armed again by each run, Set,
  the daily switch on and each page open.
- Put a file in place (debug builds), e.g. "my picture":
  `cat x.jpg | adb shell "run-as io.github.buerlino.apodroid sh -c 'cat > files/fallback.jpg'"`.
  The R8 build keeps the data of the debug build it's installed over, so set up a state with
  `run-as` first, then install the R8 build.
- Delete a saved picture: `adb shell content delete --uri content://media/external/images/media/<id>`.
- Don't pipe Gradle into `tail` before `&& adb install`: the pipe hides a failed build and the old
  APK gets installed.
- Taps: `adb shell input tap X Y` in physical pixels (screen 1116×2484; screenshots are shown
  scaled, ×1.24). Screenshots: `adb exec-out screencap -p > file.png`. If the phone is locked,
  ask the user; don't try to unlock it.
- Devices (2026-10-16): the phone is a Fairphone 6 (`adb -s 4d42e62f`, LineageOS, Android 16,
  1116×2484), the user's own, with APODroid from Obtainium (release key) and their settings. A
  Lenovo tablet TB305FU (`adb -s HA24BZ2Y`, Android 15, 800×1340 at 180 dpi) is free for
  testing. A Samsung SM-G955F (Android 9) is below minSdk: don't use. With more than one
  attached, always pass `-s`.
- Leave the devices as found: before changing a system setting with adb (`screen_off_timeout`,
  `font_scale`, proxy, demo mode, firewall chain), read and note its value, and set it back after.
- Testing an update on the user's phone without losing their settings (2026-10-16): the R8 build
  signed with the release key goes over the Obtainium install with `adb install -r`. The user
  signs it in their own terminal (apksigner asks for the password, which never reaches Claude):
  `zipalign -p 4` the unsigned APK into `app/build/` (gitignored; Claude's `/tmp` scratchpad
  isn't visible there), then give the user the full command with absolute paths:
  `~/Android/Sdk/build-tools/37.0.0/apksigner sign --ks <keystore> --ks-key-alias apodroid --out
  <repo>/app/build/apodroid-vX.Y.Z.apk <repo>/app/build/apodroid-aligned.apk`. The keystore's
  place is in Claude's memory, not here (public repo). Afterwards the installer shows as none;
  whether Obtainium still tracks the app is unchecked.
- Slow or broken downloads (2026-10-07): a Python asyncio CONNECT proxy on the desktop that
  throttles one host (`assets.science.nasa.gov` for the picture) and passes the rest.
  `adb reverse tcp:8899 tcp:8899`, then `adb shell settings put global http_proxy 127.0.0.1:8899`.
  Force-stop the app first: a process reads the proxy when it starts. Undo:
  `settings put global http_proxy :0`, then `settings delete global http_proxy` and
  `adb reverse --remove tcp:8899`; check that `settings get global global_http_proxy_host` is
  empty. Deleting the key alone leaves the proxy active, which cuts the whole phone off once the
  proxy stops. A proxy that refuses connections can't fake offline (Android's HTTP client then
  goes direct): use the firewall chain (Store listing). The script: `proxy/proxy.py` in `private/wp7-handoff/` (run it from a folder of its own; it
  reads `rules.txt` and writes `proxy.log` there). `slow` below about 800 B/s fails the TLS handshake
  (the server gives up), so use 800 or more. A `cut` or `refuse` rule doesn't fail a check
  either (the client retries the route directly); `stall` part-way through the response does
  (`www.nasa.gov stall 10000`: the NASA feed is about 16.8 KB on the wire; 30 s read timeout).
- A made-up feed (2026-10-18, WP7c): a temporary debug build with the source's URL in `Source`
  set to `http://127.0.0.1:8897/feed.xml` and `android:usesCleartextTraffic="true"`, a
  `python3 -m http.server 8897` in a scratch folder (its log counts the requests) and `adb
  reverse tcp:8897 tcp:8897`. Copy both files aside first and back after (uncommitted work).
- Make the next download fail once (the rendition, so the plain URL follows): rule
  `<host> cutonce <bytes>`, which empties `rules.txt` after the cut (2026-10-16).
  The app pools connections: `am force-stop` after changing a rule. Don't stop the proxy with
  `pkill -f <path>` in a command that contains the path: it kills that shell too.
- A phone's width on the tablet (for font scale 2.0 checks): `wm density 344` (the FP6's 372 dp:
  1116 px at 480 dpi, measured 2026-10-18; 425 dp, used until then, was too wide and hid the
  chips wrapping), `wm density reset` after; the tablet's font scale is 1.1 (2026-10-17).
- Whether Android 15+ holds a job back for charging or idle: `Num Required Flexible
  constraints` in its `dumpsys jobscheduler` entry (0 on the tablet, 2026-10-17).
- Stop a running job as the system would: `adb shell cmd jobscheduler timeout
  io.github.buerlino.apodroid 1` (or `2`). Since J2 Android doesn't retry it (`onStopJob`
  returns false): the alarms come 15 min after the run's end (U7 saw Android's retry 6½ min late
  before).
- A full disk (tablet only, never the FP6; U7): note `df /data`, `dd` into `/data/local/tmp`
  slows to a crawl after ~25 GB, so `fallocate -l <size> /data/local/tmp/fill2` the rest at
  once, then a small file to leave about 200 KB (less than the picture to download; fake its
  source's date back first). `rm` the files after; Android clears caches meanwhile.
- Offline with the test proxy still set: the app connects to the proxy and waits for its
  timeouts (about 30 s per source) instead of failing on DNS; remove the proxy first for quick
  offline checks.
- The tablet sometimes has a VPN on (regression 4, 2026-10-21; V8 the same night still used
  the proxy): while it's active the test proxy and the firewall chain miss the app. Check the
  status bar's VPN icon or `dumpsys connectivity | grep -i vpn` first. Failed runs then come
  from Wi-Fi off with `run -f` (they fail within a second); leave the VPN as it is (not ours).
- Learned in the V rows (2026-10-21 to 25, the tablet):
  - A clock faked into the past breaks TLS after a while (the stapled OCSP: "Response is
    unreliable: its validity interval is out-of-date"): fetch only with the clock at or after
    the real time, so step it forward only (V11b).
  - A run long enough to act during it (the runs take 0.2–1.3 s on the tablet's Wi-Fi): "Save
    every picture" at No limit with the posts faked back (V12b: 3 s), and the second action sent
    from the tablet's shell in the same command, e.g. `adb shell "cmd jobscheduler run -f
    io.github.buerlino.apodroid 1; sleep 0.8; input tap X Y"`, not two adb calls.
  - `kill -9` of job 1's run: Android restarts job 1 itself within minutes (V12a: 4.5 min), so
    the alarms' own retry shows only for a killed alarm's run (id 2: fake the clock just before
    a check).
  - Battery saver on the charging tablet needs `cmd battery unplug` first (`dumpsys battery
    reset` after). A reboot: no PIN on the tablet, `wm dismiss-keyguard`, then wait for
    `sys.boot_completed`.
  - Tailscale's VPN stays VALIDATED with no network under it (airplane mode), so JobScheduler
    sees connectivity (V9).
  - Lenovo's ZuiMemoryCleaner force-stopped the cached app once after a clock step (V12d), which
    deletes the alarms and job 1 until the next start: check `dumpsys alarm` after each step.
  - `am kill` keeps the page's saved draft, so a Set after reopening saves that draft, not prefs
    pushed meanwhile. A Compose popup takes the tap that closes it: in one `input` line, a Set
    tap right after a menu choice goes to the menu.
  - Real split screen needs a second app picked in Recents (another app's screen): use `wm size
    600x1340` and back instead (V9).

## Still untested

What no device has checked yet, one line each with the row that left it open. Each row's results
are in its report commit and run folder (`.coord/runs/`, gitignored); git keeps the old, longer
list (until 2026-10-26). A V row, the regression or a phone pass removes what it closed and adds
what it leaves open.

**For 0.6.0's release gate** (`CLAUDE.md`, Next features):
- V12a's rest, as V12e and V12f after B22–B24 (V12a, 2026-10-25: 3 of its items ran in the time box):
  reboot, time zones, the clock ±3 h, Doze Unrestricted and Optimized (`working_set`), battery
  saver, `force-stop` and airplane mode during a run, a page open during a run (B16f1's retry),
  the switch off mid-run (B19's info line). Low storage during Set ran in B20's check and
  regression 10.
- V7: turns over hours on a real night, the tablet on battery.
- P on the FP6: the release build over Obtainium's 0.5.0, a real morning, a reboot with the PIN,
  and what the tablet left: "my picture" from the picker as a huge, tiny, panorama, tall, WebP,
  GIF and EXIF-8 picture (V10, V11c, V12c); the boxes' "Don't show again" with its dialog and
  "App info"; TalkBack's "not set yet" and the headers' states (V8); home and lock set apart on
  Android 16 (M0: the tablet only); "my picture" and video days on R8 (V6).
- Whether ZuiMemoryCleaner force-stops the app on a real morning without a clock step (V12d).

**The job and the rules:**
- A set failing on one screen and its retry, also after 09:00 (V3, V11b, V12b: nothing on the
  tablet makes a set fail; `ScreensTest` has the rule).
- Android's real ~10 min job deadline (V2: only `cmd jobscheduler timeout`); a frozen process
  during a run; a stop timed into a rendition download (WP8b's fix, found by reading).
- A connection cut during a Set's download (V9).
- After a reboot, the armed pair firing (V9, V11a: armed at `BOOT_COMPLETED`, not waited for).
- A real DST change (only faked clocks: V11a, V11b, V11d).
- A download that isn't a picture (`badUrl`, WP7c's made-up feed): not rerun since 0.5.0.

**The update from 0.5.0:**
- The migration race (the page and the job at once) with "Where" Home or Both (V4: Lock only).
- 0.5.0 with Earth in its sources, or with a stand-in on the wallpaper in APOD mode (V5;
  `MigrationTest` only).
- Obtainium tracking the app after an `adb install -r` of the release-signed build (WP8).

**The page:**
- A rotation during "Change picture"'s picker (V4, V8).
- The problem box's Dismiss across a faked day; each source on each screen one by one (V10).
- The toast "Setup saved. Nothing to set yet." (regression 2: toasts weren't caught then).
- Crop mode kept during a same-date pull. A day without an explanation.
- The crop on a picture the page down-samples, and on a real tall picture (none posted by
  2026-10-22); dragging the frame with TalkBack (it has no screen-reader action).

**Android:**
- Android 10–14 on any device (the tablet is 15, the FP6 16): there the system crops the stored
  file to the hint itself; 10–11's stock palette; keep running on 10–12 (`keep-running.md`).
- Dynamic colour following a set wallpaper (U1: the tablet's ZUI and the FP6's LineageOS have
  wallpaper colours off).
- TalkBack itself (the labels only by `uiautomator dump`); the themed icon in a launcher that
  shows themed icons (only as a render and in the APK).
- The real 90-day hibernation (only `cmd app_hibernation`) and archiving on Android 15+.

## Releasing (as in gridload)

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (max 500 characters).
3. Commit and tag `vX.Y.Z` only when the user asks; the user pushes. The tag builds the signed
   GitHub Release for Obtainium.
4. F-Droid rebuilds the tag and must get a byte-identical APK apart from the signature. For
   build-only changes, compare the unsigned release APK's sha256 before and after.
5. Push order: `master`, then the tag. F-Droid's bot (`UpdateCheckMode: Tags`,
   `AutoUpdateMode: Version`) finds the new tag on its own, usually within a day or two, adds a
   build to the recipe and builds it. Its `Binaries:` check needs `apodroid-vX.Y.Z.apk` in the
   GitHub Release, so the Release workflow must succeed. No merge request for a new version.

## F-Droid

Recipe `metadata/io.github.buerlino.apodroid.yml` in fdroiddata, added by merge request
https://gitlab.com/fdroid/fdroiddata/-/merge_requests/50926 (merged 2026-10-04 with 0.1.1).
`Binaries` + `AllowedAPKSigningKeys` as gridload; categories Science & Education + Wallpaper,
`NonFreeNet` for science.nasa.gov and, from 0.5.0, www.nasa.gov (user's choices). New versions need no merge request
(Releasing, step 5). Only a change to the recipe itself does: a branch in `../fdroiddata` (the
gridload fork clone) from upstream `master` (remote `upstream`), checked with `fdroid lint` and
`fdroid rewritemeta` (fdroidserver from pip in a venv); the pipeline runs `fdroid build`. If a
push is rejected with "shallow update not allowed", deepen the upstream fetch:
`git fetch --shallow-since=<date before the fork> upstream master`. Reviewer comments: Claude
drafts, the user posts.

## Store listing

`fastlane/metadata/android/en-US/`: title, short and full description, changelogs, `icon.png`,
`featureGraphic.png`, `phoneScreenshots/1.png` (the whole page on one screen; `README.md` embeds
it).
- For 0.5.0 `1.png` still shows 0.4.0's page: the user takes new ones on the FP6 after WP8 (U7,
  2026-10-18), in this order: 1. the page with tabs on NASA's tab, the "On wallpaper" chip on
  it, no box; 2. the explanation expanded; 3. the settings with the source chips (two or three
  picked) and "Change every". `README.md` embeds `1.png`, so 1 is the one that matters most.
- Screenshot with SystemUI demo mode, as in gridload: `settings put global sysui_demo_allowed 1`,
  broadcasts `enter`, `clock -e hhmm 1200`, `notifications -e visible false`,
  `network -e wifi show -e level 4 -e fully true` (without `fully` the Wi-Fi icon shows "!"),
  `battery -e level 100 -e plugged false` (else a charging phone shows ⚡),
  then `exit` and the setting back to 0. It shows 1 October's picture (landscape, so the page
  fits): debug build, `am kill`, back up `files/apod.jpg` and `shared_prefs/apodroid.xml` with
  `run-as`, write 1 October's picture, `fallback.jpg` and prefs (data from
  `private/last100.json`, `videoDays` `MINE`), block the app's network so it can't fetch today's
  (`cmd connectivity set-chain3-enabled true` + `set-package-networking-enabled false <pkg>`),
  take the shot, then restore all of it. Since 0.4.0 also set, so the page shows no box and sets
  no wallpaper: `problemNoticeKey` `network:<today>` (offline is a problem now),
  `pauseHintHidden` true, `wallpaperDate` and `daily` as the shot needs, `wallpaperSetAt` (epoch
  ms, 1 Oct 06:12 local) for the status line. On a fresh debug install the prefs and files can
  be written before the first start (`run-as <pkg> mkdir -p shared_prefs files`).
- Feature graphic: source `logo/featureGraphic.svg`, render command in its header comment.

## Icon

The user's SVGs in `logo/` (108×108: `APODroid_back.svg` black, `APODroid_front.svg` rays,
stars, "APOD"/"Droid") as `res/drawable/ic_launcher_{background,foreground}.xml`, combined in
`res/mipmap-anydpi/ic_launcher.xml`. The front's `matrix()` transforms are flattened into the
coordinates, ellipses became two arcs, the guide `<circle>` and transparent `<rect>` skipped.
Check: the drawable turned back into SVG renders identical to the original (rsvg-convert +
`magick compare`). `fastlane/.../images/icon.png` is a 512 px render of both SVGs
(`rsvg-convert` + `magick -composite`).

The themed layer `ic_launcher_monochrome.xml` (2026-10-04) is computed from
`ic_launcher_foreground.xml` with shapely (in a venv, with svgelements to flatten the curves):
the white shapes keep their alpha (faint rays 0.18, the rest opaque), merged per alpha; each
letter's black outline, grown by 0.4 units, is cut out of everything drawn before it, and the
letter's visible fill (the glyph shrunk by half the stroke) is added; simplified at 0.03,
pieces under 0.05 units² dropped, one even-odd path per piece. Using the foreground itself as
the mask turns "APOD" into blobs (the outlines become opaque too). The user's launcher (Niagara)
shows apps as dots; see the icon in App info
(`adb shell am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d package:io.github.buerlino.apodroid`).
If the SVGs change, regenerate all of these.
