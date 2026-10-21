---
name: build-apodroid-android
description: Execution brief for building APODroid (io.github.buerlino.apodroid) — a native Kotlin/Jetpack Compose app that sets NASA's Astronomy Picture of the Day as the wallpaper once a day, with one page showing today's picture and its settings, distributed via F-Droid and Obtainium. Use this skill whenever working in the APODroid repo: the APOD fetch and parsing in :core, the Compose page, the wallpaper and the daily background job, building/installing on the phone, and releases. The stack (native Android, no Flutter/React Native/KMP) is already decided — do not re-open it; just execute.
---

# Build APODroid

How-tos and what's still untested. `CLAUDE.md` at the repo root is the source of truth for the
stack, the data source and the decisions. Read it first. If this skill and `CLAUDE.md` disagree,
`CLAUDE.md` wins; update this skill to match. The history is in git. The next features to build,
with a checklist: `features.md` next to this file. The work for 0.6.0: `plan-0.6.0.md` (0.5.0's
and 0.4.0's, done: `plan-0.5.0.md`, `plan-0.4.0.md`). Night shifts (agents working through the
packages unattended): `night.md`.

## Where things are

- `core/.../Post.kt`: `Source`, `Post`, `fetchLatest(source, userAgent)`, `parseLatest`
  (`parseApod`, `nasaPosts`, `earthPosts`), `download(url, to, userAgent, limit)` (`TooLarge` over the limit, F6), `renditionUrl`,
  `originalUrl`, title, video, explanation and credit parsing (`apodPosts`, `credit`), `Post.fileName`. Tests with made-up data
  shaped like the real responses: APOD in `ApodTest` (also the HTTP part), NASA, Earth and the
  URLs in `SourcesTest`, whose `readsEveryRealPost` reads `private/` when it's there. Real
  responses in `private/` (`latest.json`, `last100.json`, `iotd-feed-2026-10-16.xml`,
  `eo-last40-2026-10-16.json`).
  `core/.../Frame.kt`: `wallpaperFrame`, the part of the picture the wallpaper shows, which is
  also the crop hint, and `fitFrame`, where the whole picture goes on the screen with Fit (F1)
  (tests in `FrameTest`; a hint widened for landscape was tried and dropped, `CLAUDE.md`,
  Wallpaper).
  `core/.../Turn.kt`: `isTurnOver`,
  `nextSource` (the next ticked source with a picture) and `turnSlack` (tests in `TurnTest`).
  `core/.../Screens.kt` (0.6.0, M1; `Store` uses it since M2): `Screen`, `apodColumns`,
  `screensOf` and `placing` (S2: the list's change, a source on exactly its screens),
  `Picture` (a source's post or `Own`), `StandIn`, `Stored` (posts, files, the stand-in:
  `picture(source)`), `Record` (owner, picture, stamp; `forget()`), and the pure
  functions returning a `Plan` (clock, records, `sets` grouped by picture): `dueWallpapers`
  (each run; equal columns: the lock screen follows home's owner, B2), `restartAndForget(before, after, …)` (S4, Set: the clock restarts, the records of
  the screens with a column forgotten; a `Restart` with the plan and the screens whose record to
  `drop`, a column that was empty), and `videoDayKept` for the page's status line (tests in `ScreensTest`).
  `core/.../Saving.kt` (0.6.0, F5): `sourcesInUse` (what the job fetches and saves) and
  `toSave` (Save every picture's choice; tests in `SavingTest`).
  `core/.../Migration.kt` (0.6.0, M2): `migrate`, 0.5.0's prefs → 0.6.0's keys, run once by
  `Store.migrateOnce` (tests in `MigrationTest`).
- `app/.../Store.kt`: per source (`post(source)`, `imageFile(source)`; APOD's keys unprefixed
  and `apod.jpg`, the others `nasa.<key>` / `earth.<key>` and `nasa.jpg` / `earth.jpg`) prefs (also `daily`, `pauseHintHidden`; `checkedAt`, `problem`, `httpCode`,
  `wallpaperSetAt` for the problem and status lines), `update(force, page, show, set)` (what the
  page, the pull, Set and the job run: refresh the sources in use and `show`, noting how the
  check went, then set while `daily` is on or with `set`; returns `Updated`: what it set,
  `setFailed`, the first failure; throws only a stop), `refresh(source)`
  (the picture through `downloadPicture`: the rendition, else the plain URL), `allSources`,
  `list` (the list's columns in both modes; before the first save APOD on "Where"'s screens,
  `hasList`), `columns` (the mode's), `hasTurns`, `setup` (the stored `Setup`: mode,
  "Where", list, interval, fit, video days, `parts`, ⛶'s `Part` per source) and
  `saveSetup(setup)` (S4, Set: one commit with the settings, `putColumns`, the parts and
  `restartAndForget`'s plan; `draftFallbackFile` renamed to `fallbackFile` first; not `daily`),
  `switchDaily(on)` (S4f2: writes `daily` at once) and `startAfresh()` (the switch on:
  `restartAndForget` with the stored columns),
  `lastScreens(source)` (S2: an unticked row's screens, `<source>.lastScreens`, written with the list), `record(screen)` (home unprefixed, lock `lock.`;
  `screenState(screen)`, a `ScreenState` (picture, set time, `kept`) for the page; `pageScreen`
  is APOD mode's screen), `stored()` (posts, files,
  the stand-in for `:core`), `setDue()` (`dueWallpapers`, then the sets per screen, under the
  wallpaper lock), `saveInUse()` (the job's saves, `saveEvery`, `saveLimit`), `tabPicture(tab)`, `set(picture, screens)` (the flags per screen; a photo with
  an EXIF orientation goes upright through `setBitmap`; with `fit` the `fitted` bitmap), `cropPosition(post)`, `save` (the original, else the stored picture; the job's `limit`; returns `Saved`), `imageBounds`,
  `wallpaperFrame` (`:core`'s as a `Rect`).
- `app/.../MainActivity.kt`: the page, one instance (`launchMode="singleTop"` in `AndroidManifest.xml`, B4; `onNewIntent` does nothing) (`PictureSide`: `SourceTitle`, the shown source's name with its dropdown, and `PicturePage`, each source's picture, in a `HorizontalPager` whose settled page is `shown` (`tab`, `showTab`), then `Caption`, the boxes and the caption of the shown `post`; `show()` loads `posts` and `pictures` of every offered source (`sources`; `apod` decides them in APOD mode), `saved` per source;
  `screens` and `stored`, the stored `Setup`, from `readStatus`, `chipText`, `statusLines`, `readProblem`, `VideoNotice`: what is set), with `checkPausing()` and the `PauseHint` in a `NoticeBox` below the picture;
  the daily switch (`daily`, at once: `switchDaily`; on runs `switchOn`, then `scheduleJob()` after its set, as Set's; on while a load, Set or switch-on runs: owed in `switchRuns` (`core/.../SwitchRuns.kt`, `SwitchRunsTest`), run by `runNext` after it, as `loadAgain`, unless the switch-on run went on to set, `Updated.wentToSet`: B6); the draft (S4: `where`, `list`, `interval`, `fit`, `videoDays`, `parts`, `draftFallback`,
  `allSources`; `draft()`, `useDraft`, `columns`; kept by `onSaveInstanceState` with `toBundle`/`readDraft`;
  ✓ `keepPart`, `draftPosition`, `storedPosition`); the settings (`Settings`: `Header` for "Setup" and "Advanced settings", `setupOpen`/`advancedOpen`
  in the saved state, `setupSummary` of the stored setup; the highlight: `setupChanged`, `rowChanged`, `videoDaysChanged`, `highlight`, `Segments`
  (the segmented rows), `DropdownButton`; with the switch "More NASA pictures" `PictureList`; `VideoDaysChoice`, the
  dropdown; `switchAllSources`, `place(source, screens)` (the list's tick, untick and dropdown), `chooseVideoDays`,
  all in the draft; `setAsWallpaper()`, the one button: `saveSetup`, `update(set = true)`, then the job scheduled (`scheduleJob`) or cancelled by the stored `daily`);
  `follow()` redraws while resumed when `Store.watch` sees the job set (either screen's record) or store a post.
  `app/.../DailyJob.kt`: the job (id 1; after `update`, `saveInUse` unless stopped); `ensure` reschedules it only when its timing changes; while a run goes or a failed run's retry is due `schedule` and `ensure` are only noted and `rescheduleNoted` does them at the end of a run that didn't fail or of the retry (one retry at most, B5, B7); a run `onStopJob` stopped counts as failed (`stoppedBySystem`); `cancel` stops it at once, drops the note and the retry due (S4f3, B7); the bookkeeping is `core/.../JobRuns.kt` (`JobRuns`, `Reschedule`; `JobRunsTest`): `started()` returns the generation `finished(generation, failed)` takes, `drop()` bumps it.

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
- Job: force a run with `adb shell cmd jobscheduler run -f io.github.buerlino.apodroid 1`, list
  with `adb shell dumpsys jobscheduler | grep io.github.buerlino.apodroid` (`Last successful
  run` outlives logcat). The job logs `Daily job: <SOURCE> <date>, wallpaper set` (or `video, wallpaper
  kept`; SOURCE is APOD also for "my picture") and `Daily job failed` under `adb logcat -d -s APODroid`; the page the same as `Page:
  …` and `Update failed`. Nothing is logged when there was nothing to set. Pull to refresh:
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
- `am force-stop` cancels the app's jobs; opening the page schedules it again (pref `daily`).
  Use `am kill` to restart the app and keep the job. `pm clear` cancels it and the pref.
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
- End a turn (debug builds, 2026-10-16): after `am kill`, read `turnStartedAt`, then
  `sed -i -e '/turnStartedAt/s,<value>,<value - 10800000>,'` (3 h back) and `run -f`. A script
  that does it: `turn.sh [ms back]` in `private/wp7-handoff/` (gitignored; `W=<s>` waits longer, e.g. 45 for a
  read timeout), with `swap.sh debug|r8 [sed args]` (install a build, fake prefs) and `ui.sh` (the
  page's nodes with their tap coordinates: rows move when a tick changes).
  Each `run -f` pushes the periodic job's next window a period further; Set (with turns,
  any Set; without, a changed timing) schedules it afresh.
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
  io.github.buerlino.apodroid 1`. Its retry may come minutes late (6½ min on the tablet, U7:
  Android batches ready jobs); `Last successful run` in `dumpsys jobscheduler` shows it.
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

## Still untested

- 0.6.0 (the night of 2026-10-20 on the tablet, debug builds; the morning's short names not on a
  device yet; F1, F3 and F1f1 in V1, F4, F5 and F5f1 in V2): what V1–V7 and P still have to check is
  listed in `plan-0.6.0.md`, V and P. Each V row updates this bullet with what it checked.
  V1 (2026-10-20, debug build of dev, passed): Fit with a wide picture (APOD's real
  1787×1340, Earth's real map 1942×1340, a made-up star field 3726×2484) and a tall one (a
  made-up 600×2000 in `files/apod.jpg`), with the grid (Both columns one set, `mWhich=3`;
  home APOD and lock Earth set separately) and in APOD mode ("Where" Both one set, Lock and
  Home only their screen); Fit's hint is the whole 800×1340 bitmap, Fill's the centred crop
  again; each Fill/Fit change and the mode switch with the daily switch on set at once and
  `turnStartedAt` stayed; no ⛶ with Fit, and Fit closes an open crop mode. A made-up JPEG with
  EXIF orientation 6 as "my picture" (a faked APOD video day) with Fit: upright, the circle
  round. Share: "<title>\n<page link>" in the share sheet's preview from APOD's and NASA's
  tabs and from the ▶ tile (NASA has no video posts, so the tile is APOD's only). Seen only
  for the user's eyes: the star field with Fit shows faint dots where Fill shows crisp stars
  (a 4.7× shrink of 1–3 px stars; a made-up field, not a real APOD), and the lock screen only
  in grid mode (APOD mode's by `dumpsys wallpaper`). Screenshots in the run's `tablet/`.
  V2 (2026-10-20, debug build of dev, passed): the credit, today's real one ("Credit &
  Copyright: Aurelia Degli Esposti", stored by a pull) and the longest of the 100 (2026-09-04's,
  faked by prefs), in APOD mode and on APOD's tab, the explanation closed (one line, an
  ellipsis) and open (all of it); none on NASA's tab. Save every picture with `run -f`: a stop
  (`cmd jobscheduler timeout`) during APOD's original (1.1 MB, throttled to 2000 B/s by the
  proxy) left no `.part` in `cache/` and no gallery row, `saveTried` held the URL, the retry
  30 s later saved the stored picture (170775 bytes, "rendition"; no new connection to the
  host) and cleared the note; a second run saved nothing; the file deleted in the gallery
  wasn't saved again; a faked APOD video day with NASA standing in saved NASA's original
  (a `nasa.saveTried` of another URL didn't stop it) and not APOD's (its saved note cleared
  first); the grid with APOD, NASA and Earth ticked saved APOD's and Earth's originals (a
  `saveTried` of another URL again), NASA's already saved. The ⓘ dialog's text as approved.
  Not checked: a stop during NASA's or Earth's original (the same code path), the job's
  real ~10 min deadline (only `timeout`). Screenshots in the run's `tablet/`.
  V4 (2026-10-20, debug build of dev, passed): only NASA failing (the proxy stalling
  `www.nasa.gov`, a pull with "All NASA pictures" on): `nasa.problem` network, the box "NASA
  Image of the Day: Couldn't reach NASA's site. Check the connection." on APOD's, NASA's and
  Earth's (unticked) tabs. The job's sources (the proxy's log, hosts with a rule): Home APOD +
  Earth, Lock Earth connected only to science.nasa.gov and its assets, `nasa.checkedAt` untouched;
  the stand-in on home's turning column (APOD's turn, a faked APOD video day, NASA standing in):
  only `www.nasa.gov`, NASA's picture set on home alone (owner APOD, stamp and `turnStartedAt`
  kept, the lock screen untouched), the notice "… so NASA's Image of the Day takes its place.".
  The migration race: a 0.5.0 state by `run-as` (APOD, "Where" = Lock), `run -f` and `am start`
  in one shell line: one "Migrated from 0.5.0" (on the page's thread; the job's run succeeded
  2 s later), home's record moved to `lock.*`, every other key and value the same. The open page
  followed a job run that set only the lock screen (NASA's newer post; the page skipped it by
  the hour rule): "Lock screen changed …" and NASA's title and chip "On lock screen" updated,
  home's id unchanged. The picker in grid mode on a faked video day with no `fallback.jpg`:
  "Use my picture" opened it at once; Back kept NASA and set nothing; reopened, rotated to
  landscape while open, then a pick: `MINE`, the file copied, home set at once (`mWhich=1`, the
  record "own", no hold, the clock kept), "Change picture" beside the dropdown; "Change picture"
  replaced the file and set home again. The chip in APOD mode with "Where" = Lock shows only with
  tabs (as built): on a faked video day with NASA standing in, "On wallpaper" on NASA's tab, the
  page opening there, only the lock screen set; on a plain day no chip and the status line from
  the lock screen's record. Not checked: a rotation during "Change picture"'s picker, backing out
  of it, the picker in APOD mode, the race for "Where" Home or Both (Lock is the case that
  copies), an upgrade from a real 0.5.0 APK (the state was made by `run-as`). Evidence in the
  run's `tablet/`.
  V3 (2026-10-21 night, debug build of dev d2bf03c, after S6 and B1–B6, passed): Set in both
  modes with the daily switch on and off: a draft (Earth ticked, Fit, "Where", the mode) wrote
  no pref and set nothing; Set saved it, restarted `turnStartedAt` and set each screen afresh
  (grid: APOD on both, one set `mWhich=3`; APOD mode "Where" Home only home, Both one set; with
  the switch off a one-off set with Fit, no job); the job's next run in the same turn set
  nothing, a turn later home Earth and lock NASA, each by itself. The daily switch: off with a
  Fit draft cancelled the job and kept the draft; on with a Fill draft set the stored Fit
  (`mCropHint` the whole 800×1340) and scheduled the job, Fill still the draft. The title
  dropdown (✓ before the shown one) and the swipe with each source's own post, stopping at the
  ends: grid mode three sources, APOD mode on a NASA stand-in day APOD and NASA, a plain day the
  name alone. A faked APOD video day (`isVideo`), home APOD + NASA taking turns, lock APOD alone:
  NASA, Earth and "my picture" on both screens with owner APOD (stamps kept), "Skip it" passed
  home's turn to NASA (owner NASA, stamp kept) and kept the lock screen ("Lock screen: video
  day, wallpaper kept"); APOD mode "Where" Both: each stand-in set, "Skip it" set nothing. The
  page opened on the stand-in's page, the notices as approved. B1: APOD mode, "Skip it", switch
  off and on: the records forgotten, "Video day, wallpaper kept" shows; with turns on home: home
  NASA, "Home screen changed … · NASA" and the lock screen's kept line. Set and a Fit tap in
  one `input` line: Set saved "Where" only, Setup closed with its header in `tertiary`, Fit
  still the draft. Pull to refresh with APOD's `date` faked a day back (the page open skipped
  it by the hour rule): fetched, `date` back to today, the picture set (`Page: wallpaper
  set`). Landscape: two panes, the grid draft kept. Font scale 2.0 at `wm density 344`: the
  list (dropdowns wrap under the names), "Change every", Fill/Fit, the status lines with the
  short names, the credit with its ellipsis; the video dropdown's "▾" wraps to a second line
  under "NASA Image of the Day" (for the user's eyes). No crash (`exit-info`: force stops and
  kills only). Not checked: a set failing on one screen and its retry (nothing on the tablet
  makes a set fail; `ScreensTest` covers the rule). Screenshots in the run's `tablet/`.
  V5 (2026-10-21 night, the update from a real 0.5.0: a debug build of `v0.5.0` under the dev
  debug build 752ae8e, passed): nine states, `where` Home, Lock, Both × `sources` APOD, NASA,
  APOD+NASA, each from `pm clear` with only `daily`, `sources` and `where` seeded, 0.5.0's page
  setting the wallpaper itself (its own record), then 0.6.0 by `install -r` (`tablet/v5.sh`).
  Each logged one "Migrated from 0.5.0" and wrote what `CLAUDE.md` says: `allSources` false for
  APOD alone, else true with `home`/`lock` = `sources` in the columns `where` names (the other
  `""`); Both copied the record to `lock.*`, Home left `lock.*` unwritten, Lock moved it there and
  removed home's keys; `wallpaperPicture` the noted source, `wallpaperStamp` = `turnStartedAt`
  (APOD alone has none in 0.5.0, so none); APOD mode's owner APOD, the others 0.5.0's (NASA, its
  first turn's). Nothing else changed. The first page open and a `run -f` after it set nothing
  (`dumpsys wallpaper` ids the same, no set logged, the job's run successful, no pref changed by
  the job). The page: APOD mode with the name alone and the summary "APOD on <home screen | lock
  screen | both screens>"; switch on with the title dropdown, opening on NASA (the picture's
  source) with the chip "On home screen", "On lock screen" or "On wallpaper" as `where` said,
  the summary "APOD on …, NASA on … · turns every 24 h" for two, the status line per screen.
  A tenth: APOD alone, Both, `MINE` with `fallback.jpg`, the record faked to 0.5.0's own
  picture (APOD video date, no URL): `wallpaperPicture` and `lock.wallpaperPicture` "own",
  nothing set, the notice "… so your own picture is the wallpaper.", "· My picture". Not
  checked: Earth (0.5.0's handling is NASA's), a 0.5.0 state whose `wallpaperSource` is a
  stand-in in APOD mode (rule unit-tested in `MigrationTest`), a release build (`run-as`).
  V6 (2026-10-21 night, R8 builds signed with the debug key: `v0.5.0` and dev c9c5a28, passed):
  the update, once: 0.5.0's state seeded by `run-as` (`daily`, APOD+NASA, Both), the 0.5.0 R8
  page set NASA on both, then 0.6.0 R8 by `install -r`: one "Migrated from 0.5.0", the page on
  NASA with "On wallpaper", the summary "APOD on both screens, NASA on both screens · turns every
  24 h · Fill", the status line "… · NASA"; a `run -f` succeeded and set nothing. On the 0.6.0
  R8 build: "More NASA pictures" off in the draft and Set put APOD on both (one set, `mWhich=3`;
  "APOD on both screens · Fill"); a pull stored the credit ("Credit & Copyright: Aurelia Degli
  Esposti" under the date); Fit and Set: the hint the whole 800×1340, no ⛶, "· Fit"; the switch
  on with Fill and Set: APOD on both, "· APOD"; the swipe APOD → NASA → Earth, stopping at Earth,
  and back; the title dropdown with ✓ before the shown one. "Save every picture" with the default
  20 MB and a `run -f`: APOD's original saved (1100460 bytes), NASA's 208 MB PNG refused by its
  `Content-Length` and the stored picture saved ("Original over the limit, saving the stored
  picture", "NASA 2026-10-20 rendition", 826248 bytes); the run successful. ★ on NASA's page
  with "Largest original" 10 MB and a 27 MB original faked into `nasa.imageUrl` (2026-08's
  `55457472091-12a8c7edb0-o.jpg`, by the debug build, then the R8 build over it): "Saving",
  then "Saved", the gallery row 27060691 bytes (the original). The test's gallery files deleted.
  No crash (`logcat`, `exit-info`: kills and updates only). Seen: R8 renames the exception class
  in that log line (`m91: Larger than 20000000 bytes: 208563652`; logcat only, the page shows
  nothing of it). Not checked on R8: "my picture", video days, the lock-only and split columns
  (V3 on debug), Wi-Fi only (V/F2 on debug), the ⓘ dialog; ★ with an original between 10 and
  20 MB (only 27 MB, over both limits). Evidence in the run's `tablet/`.
  V8 (2026-10-21 night, debug build of dev a06ea0b, V1, V2 and V4 again after S4–S6, passed):
  Fit by Set only: APOD mode "Where" Both with APOD's wide 1787×1340 (one set, `mWhich=3`, the
  hint the whole 800×1340, black above and below on home and lock); a made-up JPEG stored
  1600×900 with EXIF orientation 6 as "my picture" on a faked APOD video day: upright and tall
  (900×1600) on both screens, thin black bars at the sides; "More NASA pictures" with home APOD
  (the video day, my picture) and lock Earth (wide): two sets (`mWhich=1` and `2`), each Fit.
  No ⛶ with Fit, no ⤴ anywhere. The credit with S6's title and swipe: today's real one and a
  made-up 254 characters (the longest's length) by prefs, in APOD mode (under the date) and with
  "More NASA pictures" (under the title on APOD's page), one line with an ellipsis while the
  explanation is closed, all of it while open; none on NASA's page. Save every picture with
  `run -f`: home APOD, lock NASA + Earth saved APOD's original (1100460 bytes), NASA's stored
  copy (its 208 MB PNG over the default 20 MB: "Original over the limit…") and Earth's original
  (799397); "No limit" and a 27 MB original faked into `nasa.imageUrl`, the proxy at 100 KB/s, a
  `timeout` 8 s in: no `.part`, no gallery row, `nasa.saveTried` the URL; the next run saved the
  stored copy (826248, "rendition") and cleared the note; 10 MB: "Larger than 10000000 bytes:
  27060691", the stored copy; "No limit": the original (27060691). The ⓘ dialog: B3's text word
  for word. The test files deleted from the gallery. The picker in the draft (APOD mode, a faked
  video day, no `fallback.jpg`): "Use my picture" opened it at once; Back kept "Skip it"; opened
  again, rotated to landscape and back while open, a pick: `fallback-draft.jpg` (the picked
  file's bytes), `videoDays` still `KEEP`, the notice still Skip's, "Change picture" beside the
  dropdown; "Change picture" opened the picker, Back kept the draft; Set: `fallback.jpg`, the
  draft file gone, `MINE`, "own" on both; another pick then Back (the app closed) and a start:
  the draft file deleted, `fallback.jpg` kept. Only NASA failing (the proxy stalling
  `www.nasa.gov`, a pull, "More NASA pictures" on): "NASA Image of the Day: Couldn't reach NASA's
  site. Check the connection." on APOD's, NASA's and Earth's pages after a swipe. The chip in
  APOD mode with "Where" = Lock: a plain day has one source, so no chip (as built); a faked video
  day with NASA standing in: the page opened on NASA's page with "On wallpaper", only the lock
  screen set, "Wallpaper changed … · NASA". The open page followed a job run that changed only
  the lock screen (the turn clock faked to end 100 s after the page opened, nothing set at the
  open; `run -f`: NASA on lock, home's id unchanged): "Lock screen changed 21 Oct, 02:53 · NASA"
  and the chip "On lock screen" on NASA's page, Earth's gone. The smoke list: both modes load,
  draft changes and Set set (`dumpsys wallpaper`), the forced runs ended with `jobFinished`, no
  crash (`exit-info`: force stops and kills only). Not checked: Fit in APOD mode with "Where"
  Home or Lock alone, a rotation during "Change picture"'s picker, the screen readers' "not set
  yet", a real tall post with Fit (only "my picture" is tall). Left after the restore: the
  tablet's wallpaper is APOD with Fit on home and NASA with Fit on lock, not APOD with Fill on
  both (the prefs and files are as before, so their records name APOD; the next Set puts it
  right). Evidence in the run's `tablet/`.
  Regression 2 (2026-10-20 night, dev 0d6df20, after S4f1, passed) didn't check: the open page
  following a job that changes only the lock screen (job and page share one process), a set
  that fails on one screen and its retry (nothing makes a set fail), the "Skip it" toast (rosie
  can't catch toasts), "Wi-Fi only" with the daily switch off (hidden). Share on the ▶ tile
  wasn't opened (S6 removes Share). Found: B1.
- 0.5.0 on the tablet (U7, 2026-10-18, debug and R8 builds of the tree before WP8a/8b): the
  device checks of WP7e–7g and the page of U1–U6 pass, after one fix (a read timeout was taken
  for a stopped job: `CLAUDE.md`, Problems; the chip hidden in crop mode added and checked
  after). Not rerun since their packages: the bad picture (WP7c, a made-up feed) and WP7a–7d's
  own checks (debug builds only; the 1 h rule seen in passing). Not on any device: TalkBack on
  the chips, the greyed radios and the explanation. WP8b's stop fix (a stop during a rendition
  download) found by reading only: on a device a stop can't be timed into the download.
- The FP6 (WP8, 2026-10-18, the release-signed final build): the update over 0.4.0 kept the
  settings; `Num Required Flexible constraints: 0` (Android 16); two panes in landscape (828 dp);
  the video day with each choice; the own picture centred; tabs, chip, ⛶ with ✓ and Back, long
  presses, "more", pull, ★ (the original), the chips with the weekday dialog across a rotation,
  the job's periods, offline with the box, font scale 1.5 and 2.0 (the chips two per line,
  WP8c), "Where" one height in landscape; no crash. Not there: turns over hours (the tablet's
  only), a tab switch or rotation during a save (too quick), the empty state, the home screen
  in landscape (Niagara stays portrait).
- Dynamic colour (U1) changing with a set wallpaper: the tablet's ZUI keeps a fixed palette
  (wallpaper colours off) and the FP6's LineageOS has Monet off (`mIsMonetEnabled=false`), so
  whether a set recreates the page is unchecked;
  Android 10–11 (the stock palette) not on any device.
- 0.4.0 on the LineageOS phone beyond the update and a quick pass (2026-10-16: the release-signed
  build over Obtainium's 0.3.1 on the real video day, the boxes offline; WP6's full pass ran on
  the tablet).
- Crop mode kept during a same-date pull. (A picture download failing, the box and the kept
  picture: tested in 0.5.0's WP7.)
- A day without an explanation (no preview).
- Updating through Obtainium (0.2.0 was installed with it on the phone, 2026-10-04; `adb
  install -r` over the old version keeps the job). That install blocks debug builds: switching
  needs an uninstall, which loses the user's settings, so ask first.
- The review fixes of 0.1.2 (`declutter.md`) on an R8 build or below API 36: the redraw path
  and ☆ during a download were tested on 2026-10-07 on a debug build (through the throttling
  proxy).
- A job stopped by the system (`cmd jobscheduler timeout`): while the process lives the run
  goes on and finishes (WP7, 2026-10-17); frozen, it holds the lock until its socket times out
  and the retry downloads from zero (2026-10-07; `CLAUDE.md`, open question 1). Since WP7g a
  stop interrupts the run (tablet, U7: ends within a second); a frozen process not checked.
- The themed icon in a launcher that shows themed icons (Niagara doesn't, App info shows the
  normal icon); only checked as a render and in the APK.
- TalkBack itself (the labels are in the accessibility tree: `uiautomator dump`, 2026-10-04).
- Keep running (`keep-running.md`) on Android 10–12 (no such phone; AOSP source only), the real
  90-day hibernation (only `cmd app_hibernation` simulated), and archiving on Android 15+.
- The crop (2026-10-05) on a real tall picture (only a made-up 400×1200 one swapped into
  `files/apod.jpg`), on a picture the page down-samples, with a real camera photo as "my
  picture" (only made-up JPEGs with EXIF orientation 6), with TalkBack (dragging has no
  screen-reader action), and on Android 10–14 (there the system crops the stored file to the
  hint itself). Tested on API 36, debug and R8 builds: `dumpsys wallpaper` shows the hint as
  `mCropHint`, centred, dragged and through the daily job; the home screen matched the chosen
  part; an EXIF-rotated "my picture" is upright and centred (`declutter.md`, crop pass). On a
  tall picture the frame is taller than the page, so only one of its edges shows while
  dragging; left as is, as such pictures are rare.
- The `setWallpaper` race fixed in 0.2.0 (`declutter.md`, evening pass, 1.1): found by reading,
  not reproduced.

Tested 2026-10-04 (full phone pass): offline with and without a stored picture, the switch
turned on offline (the job waits, then sets the wallpaper), faked video days with both settings
and a picked picture, font scale 2.0 (usable), landscape (the picture fills the width; scroll to
the title). Saving after "Clear storage" (or a reinstall) adds a second copy `… (1).jpg`: the
app can't see the old, now unowned file without a permission; left. The app stopping when
it isn't opened: `keep-running.md`; its hint, App info, the dialog, the real Settings screens,
hibernation and force stop on the R8 build of 7d73bc2 (`declutter.md`, evening pass).

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
