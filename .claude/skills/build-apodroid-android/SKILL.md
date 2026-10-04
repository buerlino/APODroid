---
name: build-apodroid-android
description: Execution brief for building APODroid (io.github.buerlino.apodroid) — a native Kotlin/Jetpack Compose app that sets NASA's Astronomy Picture of the Day as the wallpaper once a day, with one page showing today's picture and its settings, distributed via F-Droid and Obtainium. Use this skill whenever working in the APODroid repo: the APOD fetch and parsing in :core, the Compose page, the wallpaper and the daily background job, building/installing on the phone, and releases. The stack (native Android, no Flutter/React Native/KMP) is already decided — do not re-open it; just execute.
---

# Build APODroid

How-tos and what's still untested. `CLAUDE.md` at the repo root is the source of truth for the
stack, the data source and the decisions. Read it first. If this skill and `CLAUDE.md` disagree,
`CLAUDE.md` wins; update this skill to match. The history is in git. The next features to build,
with a checklist: `features.md` next to this file.

## Where things are

- `core/.../Apod.kt`: `fetchLatest(userAgent)`, `parseLatest`, `download(url, to, userAgent)`,
  title, video and explanation parsing, `Apod.fileName`. Tests with made-up JSON shaped like the
  real response. Real responses for manual checks in `private/` (`latest.json`, `last100.json`).
- `app/.../Store.kt`: prefs, `refresh()`, `wallpaperFile()` (which picture, or null on a video
  day with "keep"), `setWallpaper`, `save`, `imageBounds`.
- `app/.../MainActivity.kt`: the page. `app/.../DailyJob.kt`: the job (id 1).

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
  run` outlives logcat). The job logs `Daily job: <date>, wallpaper set` (or `video, wallpaper
  kept`) and `Daily job failed` under `adb logcat -d -s APODroid`. Crashes:
  `adb logcat -d | grep AndroidRuntime`.
- `am force-stop` cancels the app's jobs (the switch then shows off); use `am kill` to restart
  the app and keep the job. `pm clear` cancels it too.
- Fake an old day (debug builds only; `run-as` doesn't work on release builds), after `am kill`:
  `adb shell run-as io.github.buerlino.apodroid sed -i -e 's/<today>/<yesterday>/g' shared_prefs/apodroid.xml`.
  A video day the same way, by setting `isVideo` to true.
- Delete a saved picture: `adb shell content delete --uri content://media/external/images/media/<id>`.
- Don't pipe Gradle into `tail` before `&& adb install`: the pipe hides a failed build and the old
  APK gets installed.
- Taps: `adb shell input tap X Y` in physical pixels (screen 1116×2484; screenshots are shown
  scaled, ×1.24). Screenshots: `adb exec-out screencap -p > file.png`. If the phone is locked,
  ask the user; don't try to unlock it.

## Still untested

- A real video day (only faked via prefs), with both video-day settings; no star then.
- A day without an explanation (no ▾).
- Installing through Obtainium. (`adb install -r` over the old version keeps the job: tested
  2026-10-04.)
- A huge picture on an R8 build (on the debug build 2026-10-04: the 37 MB PNG set as wallpaper
  in about 3 s, the page shows it).
- The review fixes of 0.1.2 (`declutter.md`): page and job fetching at once ended right in 4
  runs, rotation and a kill during a load recover, but on Wi-Fi the download always finished
  before the race window, so neither the redraw path nor ☆ during a download ("Not saved: a new
  picture came in") was really hit.
- A reboot (the persisted job surviving it).
- The themed icon in a launcher that shows themed icons (Niagara doesn't, App info shows the
  normal icon); only checked as a render and in the APK.
- TalkBack itself (the labels are in the accessibility tree: `uiautomator dump`, 2026-10-04).

Tested 2026-10-04 (full phone pass): offline with and without a stored picture, the switch
turned on offline (the job waits, then sets the wallpaper), faked video days with both settings
and a picked picture, font scale 2.0 (usable), landscape (the picture fills the width; scroll to
the title). Saving after "Clear storage" (or a reinstall) adds a second copy `… (1).jpg`: the
app can't see the old, now unowned file without a permission; left. The app stopping when
it isn't opened: `keep-running.md`.

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
`NonFreeNet` for science.nasa.gov (user's choices). New versions need no merge request
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
- Screenshot with SystemUI demo mode, as in gridload: `settings put global sysui_demo_allowed 1`,
  broadcasts `enter`, `clock -e hhmm 1200`, `notifications -e visible false`,
  `network -e wifi show -e level 4 -e fully true` (without `fully` the Wi-Fi icon shows "!"),
  then `exit` and the setting back to 0. It shows 1 October's picture (landscape, so the page
  fits): debug build, `am kill`, back up `files/apod.jpg` and `shared_prefs/apodroid.xml` with
  `run-as`, write 1 October's picture, `fallback.jpg` and prefs (data from
  `private/last100.json`, `videoDays` `MINE`), block the app's network so it can't fetch today's
  (`cmd connectivity set-chain3-enabled true` + `set-package-networking-enabled false <pkg>`),
  take the shot, then restore all of it.
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
