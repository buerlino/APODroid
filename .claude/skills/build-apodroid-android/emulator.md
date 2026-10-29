# Android 10–14 in the emulator (E)

(User, 2026-10-29: E before 0.6.0's tag, set up so 0.7.0's tracks can reuse it. No device runs
Android 10–14: the tablet is 15, the FP6 16. `CLAUDE.md`: the emulator only for the versions no
device has, never for real timing.) This file is E's how and its packages; `plan-0.6.0.md`'s E row
points here. The decisions go into `CLAUDE.md` when E0 lands; a bug E finds becomes a `B<n>` in
`plan-0.6.0.md` under the release gate (a fix to the job or the rules restarts W's count).

## What can differ on Android 10–14 (investigated 2026-10-29)

The app's own version checks are five (`grep -rn SDK_INT app/src`): dynamic colour (31+), the pause
hint's two lines (battery 33+, unused 31+) and its 35+ battery text, the "Copied" toast (below 33).
The risk is the same call behaving differently. Sources: AOSP (`aosp-mirror/platform_frameworks_base`,
the `android10-release` to `android14-release` branches), androidx `ActivityResultContracts.kt`,
the developer docs' behaviour-change pages; "seen" means on these emulators, 2026-10-29.

| Android API (where in the app) | What differs on 10–14 | Likely to break | How a check sees it | Package |
|---|---|---|---|---|
| `WallpaperManager.setStream(stream, crop, true, which)`, Fill (`Store.set`) | **The system crops the stored file itself** (`wallpaper_orig` → `wallpaper`): `generateCrop` in `WallpaperManagerService` (10–13), in a new `WallpaperCropper.java` (14). 10: scales the crop to the desired height `mHeight` ("accept whatever width we end up with"), never widens it to `mWidth`; **its aspect fix sets `cropHint.bottom = width × screen aspect` when the scaled width falls under the display's width, ignoring the hint's `top`**: a frame that doesn't start at the top (a tall picture, ⛶ moved down, or "my picture" centred) can come out cut short, by our frame's rounding. 14: scales only when the crop is higher than `mHeight` (a small picture is stored small, the renderer enlarges it); `mWidth` only in a max-texture fallback. 15+ (seen on our devices) crop per orientation. Seen: `mHeight` 2400 on all four; `mWidth` 2400 on 10 right after boot, 4800 on 12–14 once Launcher3 ran. | **Medium** for tall or tiny pictures (mostly "my picture": no post was taller than the screen); low for wide posts | `dumpsys wallpaper` (`mCropHint`, `mWidth`/`mHeight`); as root, pull `/data/system/users/0/wallpaper_orig` and `wallpaper` and compare `wallpaper` with the original cut at `mCropHint` and scaled to `mHeight` (`magick compare -metric RMSE`); the home screen's screenshot shows only the frame, undistorted | E1a, E4; one Fill each in E2, E3 |
| Home and lock apart (`FLAG_SYSTEM`, `FLAG_LOCK`) | `FLAG_SYSTEM` alone on a shared wallpaper first moves it to the lock screen (`migrateSystemToLockWallpaperLocked`, since 7); 10–13 and 14 without the lock-screen live wallpaper draw the lock picture in SystemUI (`LockscreenWallpaper`). Seen on the 34 image (UE1A, Aug 2023): `mIsLockscreenLiveWallpaperEnabled=false`, and "Lock wallpaper state" lists an entry before any set. M0 saw only Android 15. | Low–medium | "Lock wallpaper state" in `dumpsys wallpaper` (an id of its own or none), the files `wallpaper_lock_orig`/`wallpaper_lock`, screenshots of the home and the lock screen | All |
| `setBitmap(bitmap, hint, true, which)`: ⛶'s whole picture, an EXIF photo | The same crop path (the hint is the whole bitmap, screen-sized) | Low | The whole picture on blur and on black: `wallpaper` equals the set bitmap's size, screenshots | E1a, E4 |
| `ImageDecoder` with `setTargetSize`/`setTargetSampleSize` and EXIF (`decodeUpright`, `whole`, `hasBlackEdges`, `blurred`) | Two implementations: 10's codec applies the EXIF orientation itself (SkAndroidCodec, "respect"), 11+ swap the size for the origin in hwui's `ImageDecoder.cpp` (the app's own comment in `Store.kt`); only 15 and 16 were checked | Medium for a camera photo as "my picture" on 10 | An EXIF-6 (and EXIF-8) photo as "my picture": `wallpaper` upright and in the screen's shape, nothing stretched | E1a, E3 |
| `BitmapFactory` (`imageBounds`, `decodeForScreen`'s `inSampleSize`) | Nothing known; formats as on 15 (JPEG, PNG, WebP, GIF's first frame) | Low | The page shows each picture | All |
| `MediaStore` with `IS_PENDING`, `RELATIVE_PATH`, `VOLUME_EXTERNAL_PRIMARY` (★, Save every picture) | 10 is scoped storage's first release (the reason for `minSdk 29`); an app reads its own rows without a permission; a second save of the same name becomes `… (1).jpg` | Low–medium on 10 | ★: `content query --uri content://media/external/images/media --projection _display_name:relative_path:is_pending` shows `Pictures/APODroid/`, `is_pending` 0; delete it: ☆ again | E1b, E2 |
| `AlarmManager.setWindow(RTC_WAKEUP, T, 10 min)` + `setAndAllowWhileIdle` (`DailyJob.arm`) | 12+ stretch a window under 10 min to 10 (ours is 10); the idle alarms' limits differ per version (10–11 about one per 9 min in Doze, 12+ the "compat" kind, 7 an hour; `on-time-0.7.0.md`): timing only. `dumpsys alarm`'s layout differs before 12 | Low (arming); timing untestable here | `dumpsys alarm | grep -B2 -A8 apodroid`: two entries, request codes 1 and 2, the same time; the log's "Daily job: alarms for …" | All |
| `JobScheduler`: id 1 periodic 6 h/6 h `setPersisted`, id 2 one-off, `NETWORK_TYPE_ANY`, `getPendingJob` | 14 throws on a network job without `ACCESS_NETWORK_STATE` for apps targeting 34+ (declared) | Low | `dumpsys jobscheduler | grep -A20 apodroid`; `cmd jobscheduler run -f … 1` ends as a successful run | All |
| Manifest broadcasts `BOOT_COMPLETED`, `TIMEZONE_CHANGED`, `TIME_SET`, `MY_PACKAGE_REPLACED` (`AlarmReceiver`) | All four reach a manifest receiver on 8+ (the implicit-broadcast exemptions; `MY_PACKAGE_REPLACED` is explicit). The `BOOT_COMPLETED` the tablet sends when an app leaves the stopped state is 15's: on 10–14 a force stop leaves no alarm until the page opens | Low | `adb reboot`, `cmd alarm set-timezone`, `cmd alarm set-time`, `install -r`: "alarms for …" each time | E1b, E4 |
| `registerReceiver(clockChanged, …)` without an export flag (`MainActivity.onResume`) | 14 requires `RECEIVER_EXPORTED`/`NOT_EXPORTED` for apps targeting 34+, except for system-only broadcasts (these two are) | Low | The page opens on 14 without a `SecurityException`; a time-zone change while it's open updates the status line | E4 |
| `PickVisualMedia` (the photo picker for "my picture") | androidx: the system picker on 13+, on 11–12 only with the R extension ≥ 2 (MediaProvider's mainline update), else Play services' fallback picker (`ACTION_SYSTEM_FALLBACK_PICK_IMAGES`), else `ACTION_OPEN_DOCUMENT` with `image/*` (no GMS step in `createIntent`). Seen: 10 has no picker (DocumentsUI), the 12 image R extension 1 (DocumentsUI), 13 and 14 the system picker. A de-Googled 10–12 phone (LineageOS: F-Droid's users) gets DocumentsUI too | Medium (another screen; the copy from a document URI in `runs`) | Pick a pushed picture in DocumentsUI (10, 12) and the photo picker (13, 14): "my picture" set, `files/fallback.jpg` there | E1a, E2, E3, E4 |
| Dynamic colour (`dynamicDarkColorScheme` on 31+, `darkColorScheme()` below) and **U1** | 10–11 the fixed palette. SystemUI's `ThemeOverlayController`: **12 accepts one colour event after any `ACTION_WALLPAPER_CHANGED`**, so every set of ours recolours the system and recreates open activities; **13+ only after a set by the foreground app** (`EXTRA_FROM_FOREGROUND_APP`): a job's set in the background waits until the screen goes to sleep (`onFinishedGoingToSleep` replays it). Never checked (U1: the tablet's ZUI and the FP6's LineageOS have wallpaper colours off) | Medium: the page recreated during or right after a Set (B13 keeps a run's end through a rotation, the same path) | `logcat -b events -d | grep am_create_activity` around a Set; Setup's state, the draft, the toast; the colours before and after (screenshots) | E2, E3, E4 |
| The pause hint's checks and texts (`checkPausing`, `PauseHint`) | 31–32 show only the unused line, 33–34 both with the 33–34 battery text. **Seen, a bug:** 12's App info has "Unused apps" with the switch "Remove permissions and free up space", not "Unused app settings → Pause … if unused" (13 and 14 match the hint; 13 has "App battery usage", 12 "Battery") | **Seen (B25)** | A screenshot of the box and of App info | E2 (today's text; B25's own check has the new one), E3, E4 |
| Clipboard (`copy`) | Below 13 our toast "Copied", 13+ the system's own confirmation | Low | Screenshot after the long press | E1b, E3 |
| `PullToRefreshBox`, overscroll | Compose's overscroll is a glow on 10–11, a stretch on 12+; the pull itself the same | Low | `input swipe 540 600 540 1500 600`: "Page: …" in the log | E1b |
| `enableEdgeToEdge()`, the system bars | 15+ force edge-to-edge (already handled); on 10–14 `enableEdgeToEdge` asks for it, with a scrim behind 3-button navigation (these AVDs use 3 buttons, `navigation_mode` 0) | Low | Screenshots: nothing hidden under the status or navigation bar, in portrait and landscape (two panes) | All |
| Back (`BackHandler` in crop mode) | No `enableOnBackInvokedCallback`: 13–14 dispatch Back the old way | Low | ⛶, drag, `input keyevent KEYCODE_BACK`: crop mode closed, the part not kept | E1b, E3 |
| `ACTION_VIEW` (the picture, the title) | The AOSP images have no browser: the toast "No browser" (a real phone has one) | Low | The toast, no crash | E1b |
| `ApplicationExitInfo` (only our checks' `dumpsys activity exit-info`) | 11+; on 10 crashes only by `logcat` | — (tooling) | `logcat -d | grep 'E AndroidRuntime'` on 10 | E1b |

Android 11 (API 30) and 12L (32) aren't covered (user, 2026-10-29): for every row above they
behave like a neighbour (11: 10's fixed palette, 12's decoder and picker path; 12L: 12).

## Decisions (user, 2026-10-29)

1. **The levels: 29, 31, 33, 34** (Android 10, 12, 13, 14), plain images without Google
   ("29, 31, 33, 34 (Recommended)"). Each tests something the others don't: 10 the oldest crop
   code, decoder and storage, DocumentsUI and the fixed palette; 12 the colour event after any set,
   the unused line alone (B25), DocumentsUI on 12; 13 the system picker, both hint lines, colour
   events from the foreground app only; 14 `WallpaperCropper`, the lock data of 14's first build,
   and 0.7.0's `getWallpaperFlags`. No Google APIs image: the backport picker needs a Play services
   module the emulator can't update, and F-Droid's users on LineageOS get DocumentsUI anyway.
2. **The night shift, after a day package E0** (the user: "night shift now, but we have to
   consider that pascaline is also running now, will the cpu handle it? and then we only have the
   tablet to test if needed?"; then "E0 by day, then E1–E4 one night (Recommended)"). Measured
   beside pascaline's night: an emulator idles at about 0.85 of one core of 16 and 2.2 GB RAM, and
   boots in 18–28 s (55 s wiped). It never touches the tablet, which stays W's to Mon 2 Nov; E0
   keeps E's rows out of the tablet regression for that reason.
3. **W's candidate, as root** ("W's candidate + adb root (Recommended)"):
   `app/build/apodroid-v0.6.0.apk` (sha256 `21e494b613337e531b434c0ffc55f8a77007f68b5ac1fe397e48ba98c0295430`,
   built from 423f082; since then only SIM's test code changed): the bytes that ship, R8
   included, until B25 merges; after it the tag's build is checked by R (`plan-0.6.0.md`: installed
   over W's candidate on the FP6 before T). `run-as` doesn't work on it; `adb root` does on these images. No build for E.
   `app/build/apodroid-v0.5.0.apk` (the same key) gives the update from 0.5.0 on Android 10.
4. **B25 before the tag** ("B25 before the tag (Recommended)"): the pause hint's unused line on
   Android 12 (the draft below). A page text, not the job or the rules: W's count stays.

## The setup (done 2026-10-29)

- **SDK packages:** `emulator` 37.2.13 and `system-images;android-{29,31,33,34};default;x86_64`
  (downloads: emulator 0.35 GB, images 0.66–0.72 GB each, 3.1 GB in all; installed in about 80 s
  for three). `yes | ~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager --install "emulator"
  "system-images;android-29;default;x86_64" …`. `avdmanager`'s "Could not load devices from
  …/devices.xml" is harmless.
- **AVDs:** `echo no | ~/Android/Sdk/cmdline-tools/latest/bin/avdmanager create avd -n apodroid-<api>
  -k "system-images;android-<api>;default;x86_64" -d pixel_6`: 1080×2400 at 420 dpi, 2 GB RAM,
  4 cores, a 10 GB data partition (sparse), 3-button navigation. In `~/.android/avd/`.
- **Disk:** about 17 GB: emulator 0.9 GB, images 3.2 (29), 4.2 (31), 4.2 (33), 1.7 GB (34), the
  AVDs 0.4–1.1 GB each after a boot. 674 GB were free after.
- **The serials:** one port per level, so a serial names its level and a stray emulator can't pass
  for another: port = 5554 + 2 × (api − 29).

  | AVD | Android | Serial | Build |
  |---|---|---|---|
  | `apodroid-29` | 10 | `emulator-5554` | QSR1.210820.001 |
  | `apodroid-31` | 12 | `emulator-5558` | SE1A.220826.006.A1 |
  | `apodroid-33` | 13 | `emulator-5562` | TE1A.220922.034 |
  | `apodroid-34` | 14 | `emulator-5564` | UE1A.230829.036.A1 |

- **Start** (one line; the shell must have the desktop's `DISPLAY`; `-wipe-data` gives a fresh
  phone, the packages' start):
  ```
  api=29; port=$((5554 + 2*(api-29))); setsid nohup ~/Android/Sdk/emulator/emulator -avd apodroid-$api -port $port -no-window -gpu host -no-audio -no-boot-anim -no-snapshot -wipe-data -timezone Europe/Zurich < /dev/null > /tmp/emulator-$api.log 2>&1 &
  until [ "$(timeout 5 ~/Android/Sdk/platform-tools/adb -s emulator-$port shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do sleep 2; done
  until timeout 5 ~/Android/Sdk/platform-tools/adb -s emulator-$port shell ping -c1 -W2 science.nasa.gov >/dev/null 2>&1; do sleep 2; done
  ```
  Booted in 16 s (29), 25 s (31), 28 s (33), 26 s (34) without `-wipe-data`; 55 s wiped (34). The
  network comes a few seconds after `sys.boot_completed` (33 once had no DNS right after it): wait
  for the ping. `adb -s emulator-<port> emu avd name` says which AVD it is.
- **Stop:** `timeout 30 ~/Android/Sdk/platform-tools/adb -s emulator-<port> emu kill` (2 s); then
  `ps -C qemu-system-x86_64-headless` is empty. The AVD's data stays until the next `-wipe-data`.
- **What failed:** SwiftShader (`-gpu swiftshader_indirect`, also `-gpu guest`, `-feature -Vulkan`,
  with a window) segfaults 15–40 s after the start, in SwiftShader's `libGLESv2.so` (a render
  thread's JIT code; gdb, 2026-10-29). `-gpu host` (the Radeon, through XWayland's GLX) works,
  headless too, but **needs `DISPLAY`**: without it "Failed to open display 0" and the emulator
  hangs without a framebuffer (kill it). So the emulator runs only while the desktop session is
  logged in, as the nights already need. `pgrep -f qemu` matches the shell running it; use
  `ps -C qemu-system-x86_64-headless`.
- **Seen in the images:** `adb root` works (userdebug); the time zone follows `-timezone`
  (without it the host's, `Europe/Vaduz`); `cmd alarm set-time` and `set-timezone` exist on 34
  (on 29 check `cmd alarm help` first; else `date` as root); `screen_off_timeout` is "never";
  no browser in any image; Launcher3 everywhere.

## How a check runs on an emulator

The skill's "Working on the phone" holds, with these differences. `S=emulator-<port>`, every call
`~/Android/Sdk/platform-tools/adb -s $S …`; never a bare `adb`, never `HA24BZ2Y` or `4d42e62f`.
`P=io.github.buerlino.apodroid`, `D=/data/data/$P`.
- **First:** `adb -s $S root` (adbd restarts: `adb -s $S wait-for-device`). `sha256sum` the APK
  against the decision's, copy it into the run folder, `install` it. Screen on and unlocked:
  `input keyevent KEYCODE_WAKEUP; wm dismiss-keyguard`.
- **Prefs and files as root, keeping their owner** (a new file would belong to root and the app
  couldn't write it): Home, `am kill $P`, then edit through a copy and write the copy back into
  the same file: `adb -s $S shell "sed -e '/name=.date/s,<today>,<yesterday>,' $D/shared_prefs/apodroid.xml > /data/local/tmp/p.xml && cat /data/local/tmp/p.xml > $D/shared_prefs/apodroid.xml"`;
  `cat` the file before and after. The skill's `sed` rules hold (toybox, quoting). A picture:
  `cat x.jpg | adb -s $S shell "cat > $D/files/fallback.jpg"` (the file must exist already, or
  `chown` it to the app's uid after, `stat -c %u $D/files`).
- **The system's wallpaper files** (root): `adb -s $S pull /data/system/users/0/wallpaper_orig`,
  `wallpaper` (the crop the system made), `wallpaper_lock_orig`, `wallpaper_lock`. Expected crop:
  the app's file (`$D/files/apod.jpg`, or the set bitmap's size for a whole picture) cut at
  `dumpsys wallpaper`'s `mCropHint` and scaled to `mHeight`:
  `magick orig.jpg -crop WxH+X+Y +repage -resize x2400 expected.png; magick compare -metric RMSE expected.png wallpaper null:`
  (normalised under about 0.02 passes; say the number). The crop's shape must be the screen's,
  1080:2400 within 1%.
- **Screens:** home `input keyevent KEYCODE_HOME`, then `exec-out screencap -p`; the lock screen
  `input keyevent KEYCODE_SLEEP; sleep 1; input keyevent KEYCODE_WAKEUP; sleep 1` and the
  screenshot (no PIN, so `wm dismiss-keyguard` after). Taps in pixels of 1080×2400:
  `private/wp7-handoff/ui.sh $S` lists the nodes.
- **Test pictures** (made-up, into the run folder; `magick`): a panorama 6000×1000 with numbered
  bands, a tall 1000×4000 with numbered rows, a tiny 200×150, a WebP, a GIF, a JPEG with EXIF
  orientation 6 and one with 8 (`magick in.jpg -orient RightTop out.jpg`, `-orient LeftBottom`;
  the pixels unturned, so a wrong decoder shows it). Push to `/sdcard/Pictures/` and make the media
  store see them: `am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures/<name>`
  (deprecated, still delivered on these images; else `adb -s $S reboot`).
- **Offline:** `svc wifi disable; svc data disable` (and `enable` after); the clock as the skill
  says (`auto_time` 0, `cmd alarm set-time`, forward only).
- **Colour events (U1):** `logcat -b events -d | grep -e am_create_activity -e am_relaunch` and
  `logcat -d | grep -i ThemeOverlayController`.
- **Crashes:** `logcat -d | grep 'E AndroidRuntime'`; `dumpsys activity exit-info $P` from 11.
- **Leave it:** results into the run folder; `emu kill` at the end. The next package's start
  wipes it; nothing on the emulator needs setting back.

## What the emulator can't tell

These stay under the skill's Still untested, by name:
- **Real timing:** Doze over a real night, standby buckets as Android assigns them, battery saver
  on a real day (the emulator does forced states only: `deviceidle force-idle`, `am
  set-standby-bucket`, `cmd battery unplug`; never for timing, `CLAUDE.md`).
- **The makers' layers on 10–14**, most real phones of those versions: Samsung's One UI, Xiaomi,
  Oppo, Huawei and Honor have their own wallpaper and lock-screen code, App info paths and
  cleaners. AOSP's `WallpaperManagerService`, Launcher3 and SystemUI only.
- **A real launcher's wallpaper scrolling** (Launcher3's only) and a real screen's colours.
- **Real network loss** (a dead spot, a captive portal, slow mobile data); only Wi-Fi and data
  switched off.
- **Play services' backport picker** on 10–12 phones with Google.
- **TalkBack** (not in AOSP images; labels by `uiautomator dump` only).
- **Memory and speed of a real low-end 10–12 phone** (the AVD has 2 GB, x86_64, the host's CPU).
- **Hibernation's 90 days**, a real DST change of the phone's clock.

## Found while setting up: B25

The pause hint's unused line on Android 12 (seen on `apodroid-31`): `plan-0.6.0.md`'s B25 (queue
row 66), committed 2026-10-29. Its check is by hand on `apodroid-31` (The setup's start line).

## Packages

E0 by day; then E1a–E4 one night, riskiest first. **Each package installs W's candidate on a
wiped emulator** (How a check runs). A `BUG:` line becomes a `B<n>` in `plan-0.6.0.md` (a fix to
the job or the rules restarts W's count); a STOP (Android does what nobody expected) goes to the
user.

| WP | What | Model, effort | Who, device |
|----|------|---------------|-------------|
| E0 | The night shift on an emulator: `scripts/emulator.sh`, the pipeline, rosie, the queue rows | Opus, high | a day session, by hand; one boot of `apodroid-29` |
| E1a | Android 10, the wallpaper: the crop, home and lock, the whole picture, decoder and EXIF through DocumentsUI | Opus, medium | rosie, `apodroid-29` (`emulator-5554`), by night |
| E1b | Android 10, the rest: the update from 0.5.0, the job and the broadcasts, storage, the page | Opus, medium | rosie, `apodroid-29` (`emulator-5554`), by night |
| E4 | Android 14: `WallpaperCropper`, home and lock without the lock-screen live wallpaper, the receivers and jobs of 34 | Opus, medium | rosie, `apodroid-34` (`emulator-5564`), by night |
| E2 | Android 12: colour events after every set (U1), the unused line alone (today's text), DocumentsUI on 12 | Opus, medium | rosie, `apodroid-31` (`emulator-5558`), by night |
| E3 | Android 13: the system picker, both hint lines, colour events from the foreground only | Opus, medium | rosie, `apodroid-33` (`emulator-5562`), by night |

```
claude --model opus --effort high "Do E0 in .claude/skills/build-apodroid-android/emulator.md"    # by day; the user commits it
scripts/night.sh --until 07:30    # E1a, E1b, E4, E2, E3 (the queue rows E0 adds), the Opus passes, no tablet regression
```
By hand instead (a day, one at a time): `scripts/emulator.sh start apodroid-29`, then
`ANDROID_SERIAL=emulator-5554 claude --agent rosie --model opus --effort medium "Do E1a in .claude/skills/build-apodroid-android/emulator.md"` (after E0: rosie may use the apodroid emulators); E1b likewise, after a fresh `start`.

**For every E package** (rosie, as "For rosie" in night.md says, with these changes): the
device is the package's emulator, never the tablet or the FP6; start it wiped (E0's script,
or the start line), `adb root`, install W's candidate (sha256 checked). No backup and no
setting back on the emulator (it's wiped at the next start), but `emu kill` at the end. About 45
minutes of checks; when time runs short keep the order and name the rest under `Not tested:`.
Evidence as text first (`dumpsys`, the pulled files' `magick compare` numbers, the log), a
screenshot for each wallpaper and each text the user approves, into the run folder. The smoke
list doesn't apply (no build of ours is new); its items are in each package. Reply as night.md
says (PASS, FIX, STOP, NOT RUN; `Tested:`, `Not tested:`, one `BUG:` line each, `For the user:`)
and write the package's results under its section here (`Results (<date>)`: a few lines and the
run folder), committed as a device package's are.

### [ ] E0. The night shift on an emulator (Opus, high)

(By day, a session by hand: it changes the night's own scripts, so it can't run in one. User,
2026-10-29: "E0 by day, then E1–E4 one night"; E1 split in two after a review the same evening.) Small and only what E needs; 0.7.0 reuses it.
1. **`scripts/emulator.sh`** `start|stop|serial apodroid-<api>`: the port by the formula, the start
   line of The setup (`-wipe-data` on `start`), detached (`setsid`, stdin `/dev/null`, the log in
   `.coord/emulator-<api>.log`), waits for `sys.boot_completed` and the ping (at most 4 min, else
   exit 1 and kill it), prints the serial; refuses without `DISPLAY` (the reason in The setup)
   and when another `apodroid-*` emulator runs; `stop` is `emu kill` and waits for the process.
2. **The lock:** `devlock.sh` learns the resource `emulator` (one emulator at a time, any
   pipeline or day session).
3. **The spec:** `pipeline-lib.sh`'s `spec_text` learns `emulator <key>` (this file's `### [ ]
   <key>.` section); `pipeline.sh` passes this file. `pipeline.test.sh` resolves the E rows.
4. **The device per row:** a rosie row whose Spec is `emulator E<n>` runs on the AVD its section's
   heading names (`apodroid-<api>`, a field of the heading or the table's "Who, device"; pick one,
   one rule): `pipeline.sh` takes the `emulator` lock, starts it, sets `ANDROID_SERIAL` to its
   serial for rosie instead of `$TABLET`, skips the debug build (E installs W's candidate:
   the prompt names it), and stops the emulator and frees the lock on every exit (the trap). No
   `adb -s HA24BZ2Y` happens in such a run.
5. **The regression stays off:** a merge of an emulator row doesn't count for the tablet
   regression (`merged_since_regression`), so a night of E rows ends with "regression: NOTHING"
   and W's tablet is left alone. Its RESULT line says `(emulator <avd> PASS)`.
6. **rosie and the rules:** `rosie.md`'s "The tablet only" and night.md's rule 4 and "For rosie"
   first bullet allow "the device `ANDROID_SERIAL` names: the tablet, or an `apodroid-*` emulator
   in an emulator row; never the FP6"; the emulator rows follow this file's "For every E package"
   (no tablet backup). `agent-settings.json` allows `Bash(adb -s emulator-55*:*)`; the FP6 deny
   stays.
7. **The queue rows** (night.md; Tablet no, rosie is the check):
   `| 67 | E1a | Android 10 in the emulator: the wallpaper | rosie | opus, medium | no | — | emulator E1a |`,
   `| 68 | E1b | Android 10 in the emulator: the rest | rosie | opus, medium | no | — | emulator E1b |`, then
   E4, E2, E3 (69–71) in that order. Rows 66+ by night once E0 is committed.
8. **Docs:** `CLAUDE.md` (Setup and distribution: the emulators, the serials, `DISPLAY`; the
   decisions above), the skill ("Working on the phone": a line pointing here; Devices: the four
   AVDs), night.md's "How it works" and Files.
- **Check:** `scripts/pipeline.test.sh` passes; `scripts/pipeline.sh E1a --dry-run` names
  `apodroid-29`, the spec and no tablet; `scripts/emulator.sh start apodroid-29` from the night's
  terminal boots and prints `emulator-5554`, a second `start` refuses, `stop` ends it; a
  regression dry run on a log with only an emulator row's merge says NOTHING.
- **Done when:** 1–8 done and checked as above; no app code changed; the user commits it (night.sh
  refuses uncommitted scripts and skill files).

### [ ] E1a. Android 10, the wallpaper (`apodroid-29`, `emulator-5554`; Opus, medium)

The riskiest: 10's own crop code (its aspect fix ignores the hint's top), the decoder (EXIF in the
codec) and DocumentsUI. The candidate installed fresh, the page opened once (APOD mode, title and
date, `darkColorScheme()`'s colours: a screenshot). In this order:
1. **Fill, Both:** Set: one shared wallpaper (no "Lock wallpaper state" id), `mCropHint` the
   frame in the screen's shape, `wallpaper` vs expected (`magick compare`), home and lock
   screenshots show the frame undistorted.
2. **⛶ moved:** drag the frame to the left edge, ✓, Set: `mCropHint` moved, `wallpaper` matches.
3. **Home, then Lock** ("Where", Set each): home alone keeps the old picture on the lock screen
   (moved to `wallpaper_lock*`), Lock alone changes only the lock screen; both screenshots; then
   Both again: one shared, the lock data gone (as M0 saw on 15).
4. **The whole picture:** ⛶ → "Whole picture" → ✓ → Set on a wide post: blurred background
   (`wallpaper` is 1080×2400, a screenshot). Then black edges: as root replace `files/apod.jpg`
   with a made-up planet on black of the same size (the post's `cropDate`/`cropUrl` kept), Set:
   black around it.
5. **"My picture" through DocumentsUI:** "If APOD is a video" → "Use my picture": DocumentsUI
   opens (not a photo picker; screenshot), pick the EXIF-6 JPEG: `files/fallback.jpg` there. Fake
   a video day (Home, `am kill`, `isVideo` true and the date one day back as the skill says), Set:
   `wallpaper` upright, screen-shaped, not stretched. Again with the tall 1000×4000 (centred: the
   frame's top is not 0, **10's aspect fix**: the crop must keep the frame's height, rows
   numbered so a cut shows), the tiny 200×150, the panorama, EXIF-8. When time runs short, the
   tall picture and EXIF-6 first.
6. No crash (`logcat -d | grep 'E AndroidRuntime'`; no exit-info on 10).
- **Done when:** each item ran or is under `Not tested:`; each wallpaper item has its `mCropHint`,
  the compare number and a screenshot; every failure a `BUG:` line; results written here; the
  emulator stopped.

### [ ] E1b. Android 10, the rest (`apodroid-29`, `emulator-5554`; Opus, medium)

The update from 0.5.0, the job and the broadcasts, storage and the page, on a fresh start (wiped).
In this order:
1. **The update from 0.5.0:** install `app/build/apodroid-v0.5.0.apk`, open it, daily switch on,
   "Where" Both, let it set (`dumpsys wallpaper`). Home, then `install -r` the candidate **without
   opening the page**: the log's "Daily job: alarms for …" (`MY_PACKAGE_REPLACED`), the pair in
   `dumpsys alarm`, the prefs migrated (`allSources` false, `home`/`lock` columns; `cat` as root),
   the wallpaper untouched (`dumpsys wallpaper`'s id the same). Then open the page: APOD mode,
   title and date, no box but the problem box if offline (the pause hint has no line on 10).
2. **The job and the broadcasts:** the pair (`dumpsys alarm`, 10's layout), id 1 6 h/6 h
   persisted (`dumpsys jobscheduler`); `cmd jobscheduler run -f $P 1` ends as a success;
   `adb -s $S reboot` (wait for boot): "alarms for …" after `BOOT_COMPLETED`, no set at boot;
   `cmd alarm set-timezone America/New_York` and back (or `setprop persist.sys.timezone` as
   root if `cmd alarm` lacks it on 10): armed again; the clock 3 h forward with `auto_time` 0,
   then `auto_time` 1: armed again (`TIME_SET`).
3. **★:** save, `content query` (as in the table): `Pictures/APODroid/APOD_…`, `is_pending` 0;
   ★ filled; `content delete` it: ☆ at the next resume.
4. **The page's rest:** long press on the title: the toast "Copied" (screenshot); pull to refresh
   (the log's "Page: …"); ⛶ then `KEYCODE_BACK`: closed, the part not kept; the picture tapped:
   "No browser"; landscape (`settings put system accelerometer_rotation 0; settings put system
   user_rotation 1`, back to 0 after): two panes, nothing under the bars (screenshot).
5. No crash (`logcat -d | grep 'E AndroidRuntime'`; no exit-info on 10).
- **Done when:** as E1a's (the wallpaper's numbers only where an item sets one).

### [ ] E4. Android 14 (`apodroid-34`, `emulator-5564`; Opus, medium)

14's own crop class (`WallpaperCropper`: no upscale of a small crop), the lock data of 14's first
build (`mIsLockscreenLiveWallpaperEnabled=false`), `mWidth` 4800, and 34's receiver and job rules.
1. **Install and the page:** fresh install, the page loads in APOD mode, no `SecurityException`
   (the clock receiver), dynamic colours (screenshot); the pause hint with both lines and the
   33–34 battery text; "App info" opens App info; its "App battery usage" and "Unused app
   settings → Pause app activity if unused" are where the lines say (screenshots).
2. **Fill, Both; ⛶ moved; Home, then Lock, then Both:** as E1a's 1–3, with `dumpsys wallpaper`'s
   lock state and both files each time.
3. **Small and tall:** "my picture" through the photo picker (screenshot): the tiny 200×150 and
   the tall 1000×4000 on a faked video day: `wallpaper` not scaled up by the system (14 scales only
   larger crops), the home screen shows it filled and undistorted (screenshot); the panorama;
   EXIF-6.
4. **The whole picture** on blur.
5. **U1 on 14:** Set on the page with the page open: `am_create_activity` or `am_relaunch` for
   the page right after (recoloured: screenshot before and after), the toast, Setup closed as
   after any Set, nothing lost; then a job's set in the background (daily on, a change time
   passed: the skill's "End a turn", `run -f`) with the page on top: no recolour until
   `KEYCODE_SLEEP`, then after `KEYCODE_WAKEUP` the new colours and the page recreated (say
   which).
6. **The job:** the pair, both jobs in `dumpsys jobscheduler` (the network constraint accepted on
   34), `run -f 1` a success; a time-zone change while the page is open: the status line's
   time follows; `install -r`: armed again.
7. Clipboard (no "Copied" toast on 14), no crash (`exit-info`).
- **Done when:** as E1a's.

### [ ] E2. Android 12 (`apodroid-31`, `emulator-5558`; Opus, medium)

12's SystemUI recolours after every wallpaper change (U1), the unused line alone, and
DocumentsUI on 12 (R extension 1).
1. **The page and the hint:** dynamic colours (screenshot); the pause hint with the unused line
   alone, today's text (B25's known bug: no new `BUG:` line; B25's own check has its new text);
   App info's "Remove permissions and free up space" off → the line gone at the next resume
   (screenshots).
2. **U1 on 12:** Set on the page: recoloured and the page recreated? (as E4's 5); then a job's set
   in the background with the page on top: **12 accepts it at once** (the page recreated while
   open, no screen-off needed): Setup's and Advanced's state, the shown source and the draft after
   it (B13's path); a Set whose run is still going when the recolour comes (Save every picture at
   No limit makes a run long enough, the skill's V-row trick): the run's end and the toast.
3. **Fill, Both, and Home then Lock:** as E1a's 1 and 3 (`mWidth` 4800 here).
4. **"My picture" through DocumentsUI on 12:** the EXIF-6 JPEG and the tall picture on a faked
   video day, as E1a's 5.
5. **The job:** the pair (12's layout), `run -f 1`, no crash (`exit-info`), "Copied" toast.
- **Done when:** as E1a's; the skill's U1 line can say what 12 does.

### [ ] E3. Android 13 (`apodroid-33`, `emulator-5562`; Opus, medium)

The system photo picker, both hint lines with 13's App info, colour events from the foreground app
only.
1. **The page and the hint:** dynamic colours, both lines (33–34 battery text), App info's "App
   battery usage" and "Unused app settings" as the lines say (screenshots).
2. **"My picture" through the photo picker** (screenshot): WebP, GIF, EXIF-8 and the panorama,
   each set on a faked video day: `wallpaper` upright and screen-shaped.
3. **Fill, Both; Home then Lock:** as E1a's 1 and 3.
4. **U1 on 13:** as E4's 5 (the background set waits for the screen to sleep).
5. **The rest:** no "Copied" toast (the system's overlay), `KEYCODE_BACK` in crop mode, the job's
   pair and `run -f 1`, no crash.
- **Done when:** as E1a's.

## For 0.7.0

Lines tracks O and L can take into their plans (not edited here). E0's script and the four AVDs
serve them unchanged; an emulator never stands in for the tablet's or a phone's timing or maker
layer.
- **O (Keep running's look):** the notification without `POST_NOTIFICATIONS` on 10–12 (in the
  shade: `apodroid-29`, `apodroid-31`) and on 13+ only in Quick Settings' Active apps (`apodroid-33`,
  `apodroid-34`): screenshots of both, `dumpsys activity services` (`isForeground`, the type),
  O7's step 1 on each level. On 10–13 `FOREGROUND_SERVICE_SPECIAL_USE` and the type
  `specialUse` don't exist (34+): the service must start there without them (a crash would show at
  the switch).
- **O (the expedited run, 12+):** O10's job 2 `setExpedited` on `apodroid-31`, `-33`, `-34`:
  accepted by `schedule()`, runs (`dumpsys jobscheduler`'s expedited lines) in forced deep Doze
  and battery saver (`deviceidle force-idle`, `cmd power set-mode 1`) with the network, Optimized;
  and on `apodroid-29` the plain job (no `setExpedited` below 12: the version check). The real
  night stays O6's (the tablet).
- **O (the pause hint per maker, O13):** the stock paths of 10–14 (12's per B25, 13–14 as seen;
  10–11 have no hibernation line), `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` opening on each.
- **O (the detector, O14):** a force stop's signs on 10–14 (`am force-stop`, then the page:
  alarms gone; `ApplicationExitInfo` from 11 only, none on 10).
- **L (Live's lock screen on 10–13):** a live wallpaper is the home screen's only; picking Live
  over a shared still picture leaves the old picture on the lock screen, and the lock screen gets
  Fill (Decisions): `apodroid-29`, `-31`, `-33` with `dumpsys wallpaper` and screenshots.
- **L (`getWallpaperFlags` on 14):** `apodroid-34` is 14's first build (UE1A, Aug 2023) with the
  lock-screen live wallpaper off (`mIsLockscreenLiveWallpaperEnabled=false`): the engine's flags
  there, and that the picker offers no lock screen; a newer 14 build (QPR) isn't available as a
  plain image, so 14 with the flag on stays the tablet's (15) analogue.
- **L (the `getWallpaperId` note path below 14, `getWallpaperInfo` null on 10–13 without
  `QUERY_ALL_PACKAGES`):** the engine's checks on `apodroid-29`, `-31`, `-33`.
- **L (colours, U1):** Live's reported colours on 12 (recolours after any set) and 13+ (foreground
  only): E2's and E3's results are the baseline.
- **The baseline:** E1–E4's results (this file) tell a 0.7.0 regression on 10–14 from a 0.6.0
  behaviour; rerun the package of a level whose code path a 0.7.0 package changes.
