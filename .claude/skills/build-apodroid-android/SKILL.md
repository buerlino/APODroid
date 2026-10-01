---
name: build-apodroid-android
description: Execution brief for building APODroid (io.github.buerlino.apodroid) — a native Kotlin/Jetpack Compose app that sets NASA's Astronomy Picture of the Day as the wallpaper once a day, with one page showing today's picture and its settings, distributed via F-Droid and Obtainium. Use this skill whenever working in the FOSS_APOD repo: the APOD fetch and parsing in :core, the Compose page, the wallpaper and the daily background job, building/installing on the phone, and releases. The stack (native Android, no Flutter/React Native/KMP) is already decided — do not re-open it; just execute.
---

# Build APODroid

Roadmap, status and practical know-how. `CLAUDE.md` at the repo root is the source of truth for
the stack, the data source, decisions and open questions. Read it first. If this skill and
`CLAUDE.md` disagree, `CLAUDE.md` wins; update this skill to match. Keep finished work here only
as far as later work needs it; the history is in git.

## Settled (don't relitigate)

- **Native Android, Kotlin + Jetpack Compose**, single Activity. `:core` (plain Kotlin/JVM)
  holds the APOD client and parsing, unit-tested with made-up JSON. `:app` holds the one page,
  the wallpaper call and the background job.
- **Build setup is copied from `~/Documents/source99/gridload`**, not re-derived (see
  `CLAUDE.md`, "Setup and distribution").
- **No proprietary dependencies.** kotlinx.serialization, `HttpURLConnection`, Compose. Image
  decoding with `BitmapFactory`, not Coil/Glide.
- **Scope:** one page (today's picture + settings). No gallery, archive, favourites, accounts.

## Working on the phone

- Build and test: `ANDROID_HOME=~/Android/Sdk ./gradlew :core:test :app:assembleDebug`.
- Real phone over USB (in gridload a Fairphone 6), no emulator. If `adb devices` is empty and
  `lsusb` shows `18d1:4ee1` (MTP only), USB debugging is off or not authorised: ask the user. `adb` isn't on PATH:
  `~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk`.
  Debug and release builds are signed with different keys, so switching needs an uninstall.
- The phone (LineageOS) blocks network access for new apps (restricted networking mode,
  `dumpsys netpolicy` shows the UID with `REJECT_ALL`; in the app it's an
  `UnknownHostException`). The user allows it in App info → Mobile data & Wi-Fi → Network
  access. An uninstall gives a new UID, so it's blocked again.
- Release build on the phone (R8 isn't covered by unit tests): sign the unsigned release APK
  with `~/.android/debug.keystore` via `zipalign -p 4` + `apksigner`; that installs over debug
  builds.
- Background job: force a run with `adb shell cmd jobscheduler run -f io.github.buerlino.apodroid <jobId>`, list
  with `adb shell dumpsys jobscheduler | grep io.github.buerlino.apodroid`.
- Screenshots: `adb exec-out screencap -p > file.png`. If the phone is locked, ask the user;
  don't try to unlock it.

## Releasing (as in gridload)

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (max 500 characters).
3. Commit and tag `vX.Y.Z` only when the user asks; the user pushes. The tag builds the signed
   GitHub Release for Obtainium.
4. F-Droid rebuilds the tag and must get a byte-identical APK apart from the signature.

## Roadmap (proposed 2026-10-01, waiting for the user)

Work one step at a time; show each working on the phone, then wait. Update the status in brackets.

### Step 0: decisions [done 2026-10-01, except APOD's explanation text]

Name APODroid; WordPress API as the only source; `JobScheduler` with `INTERNET`,
`SET_WALLPAPER`, `RECEIVE_BOOT_COMPLETED`; settings for home/lock/both and for video days; no
notification. Details in `CLAUDE.md`.

### Step 1: project skeleton [done 2026-10-01]

`git init`, copy the gridload build setup (wrapper, version catalog, daemon JVM, signing,
release workflow, LICENSE), empty `:core` and `:app` with a Compose page that says hello. Build
debug and release, install on the phone.

### Step 2: fetch in `:core` [done 2026-10-01]

`fetchLatest(): Apod` (date, title, imageUrl, pageUrl, isVideo) from the WordPress API; title clean-up
(prefix, HTML entities); tests with made-up JSON shaped like the real response (including a
video day, a YouTube link in the explanation of an image day, and the odd `-Shadow` title).
Done in `core/.../Apod.kt` (`fetchLatest`, `parseLatest`); video detection in `CLAUDE.md`, Data
source. Real responses for manual checks are in `private/` (`latest.json`, `last100.json`); a
throwaway test that parsed all 100 matched an independent Python check.

### Step 3: the page [done 2026-10-01]

On open (each `onResume`): show the stored APOD, fetch unless its date is today's US Eastern
date, download the image to `files/`, show it with title and date. Tap → the APOD page in the
browser. The settings: where (home/lock/both), video days (keep previous/my picture, picked with
the photo picker), "Set as wallpaper now". `Store.kt` holds prefs, `refresh()`, the wallpaper
call and `wallpaperFile()` (which picture, or null on a video day with "keep"); step 4 reuses it.
Tested on the phone: fetch and display, browser, picker, set now with each "where", a video day
(faked by editing `isVideo` in the prefs with `run-as ... sed`) with both choices, and the R8
release build after `pm clear`. A real video day is still untested.

### Step 4: daily update [not started]

The `JobScheduler` job and the "Change wallpaper daily" switch. Test with a forced run, then
over a real day. Watch out: `dumpsys netpolicy` shows APODroid's UID blocked with
`APP_BACKGROUND` when it isn't in the foreground (restricted networking mode on the phone).
Jobs with a network constraint should be exempt while they run; check that the job really gets
the network.

### Step 5: release setup [not started]

Icon, fastlane metadata and screenshots, first tag, F-Droid recipe.
