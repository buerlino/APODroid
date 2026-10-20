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
  (`parseApod`, `nasaPosts`, `earthPosts`), `download(url, to, userAgent)`, `renditionUrl`,
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
  `Picture` (a source's post or `Own`), `StandIn`, `Stored` (posts, files, the stand-in:
  `picture(source)`), `Record` (owner, picture, stamp, `Hold`; `forget()`), and the pure
  functions returning a `Plan` (clock, records, `sets` grouped by picture): `dueWallpapers`
  (each run), `setByHand`, `columnsChanged`, `restartAndForget`, and `videoDayKept` for the page's status line (tests in `ScreensTest`).
  `core/.../Saving.kt` (0.6.0, F5): `sourcesInUse` (what the job fetches and saves) and
  `toSave` (Save every picture's choice; tests in `SavingTest`).
  `core/.../Migration.kt` (0.6.0, M2): `migrate`, 0.5.0's prefs → 0.6.0's keys, run once by
  `Store.migrateOnce` (tests in `MigrationTest`).
- `app/.../Store.kt`: per source (`post(source)`, `imageFile(source)`; APOD's keys unprefixed
  and `apod.jpg`, the others `nasa.<key>` / `earth.<key>` and `nasa.jpg` / `earth.jpg`) prefs (also `daily`, `pauseHintHidden`; `checkedAt`, `problem`, `httpCode`,
  `wallpaperSetAt` for the problem and status lines), `update(force)` (what the page, the pull
  and the job run: refresh, noting how the check went, then set while `daily` is on), `refresh(source)`
  (the picture through `downloadPicture`: the rendition, else the plain URL), `allSources`,
  `columns`, `hasTurns`, `setAllSources`, `setColumns` (the mode and the grid; `sources` is the
  union), `record(screen)` (home unprefixed, lock `lock.`;
  `screenState(screen)`, a `ScreenState` (picture, set time, `kept`) for the page; `pageScreen`
  is APOD mode's screen), `stored()` (posts, files,
  the stand-in for `:core`), `setDue()` (`dueWallpapers`, then the sets per screen, under the
  wallpaper lock), `saveInUse()` (the job's saves, `saveEvery`), `setByHand(tab, screens)` (button, ✓), `settingChanged(Changed)` (forget,
  then `setDue`), `tabPicture(tab)`, `set(picture, screens)` (the flags per screen; a photo with
  an EXIF orientation goes upright through `setBitmap`; with `fit` the `fitted` bitmap), `cropPosition(post)`, `save` (the original, else the stored picture; returns `Saved`), `imageBounds`,
  `wallpaperFrame` (`:core`'s as a `Rect`).
- `app/.../MainActivity.kt`: the page (`Tabs`, the shown tab's `post`; `apod` decides the tabs in APOD mode;
  `screens` from `readStatus`, `chipText`, `statusLines`, `readProblem`, `VideoNotice`, `SetChoices` for the set button and ✓), with `checkPausing()` and the `PauseHint` in a `NoticeBox` below the picture;
  the settings (`Settings`; with the switch "All NASA pictures" `Grid`, `SetMenu`; `VideoDaysChoice`, the
  dropdown; `switchAllSources`, `tick(screen, source)`, `chooseVideoDays`, `canSetNow`, `setWallpaperNow(screens)`);
  `follow()` redraws while resumed when `Store.watch` sees the job set (either screen's record) or store a post.
  `app/.../DailyJob.kt`: the job (id 1; after `update`, `saveInUse` unless stopped); `ensure` reschedules it only when its timing or its network type (Wi-Fi only) changes.

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
  Each `run -f` pushes the periodic job's next window a period further; change the interval
  (or the switch) to schedule it afresh.
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

## Still untested

- 0.6.0 (the night of 2026-10-20 on the tablet, debug builds; the morning's short names not on
  a device): what V and P still have to check is listed in `plan-0.6.0.md`, V and P.
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
