# Android 10–14 in the emulator (E)

(User, 2026-10-29: E before 0.6.0's tag, set up so 0.7.0's tracks can reuse it. No device runs
Android 10–14: the tablet is 15, the FP6 16. `CLAUDE.md`: the emulator only for the versions no
device has, never for real timing.) This file is E's how and its packages; `plan-0.6.0.md`'s E row
points here. **Brought to today's tree on 2026-11-22 (ER, `release-0.10.0.md`):** Android 10, 12, 13,
14 and 17 (`apodroid-37`, new); the packages are `release-0.10.0.md`'s E17, E14, E10, E12, E13 and UP,
the cases per level below; a bug E finds becomes a `### [ ] B<n>` there.

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
| The pause hint's checks and texts (`checkPausing`, `PauseHint`) | 31–32 show only the unused line, 33–34 both with the 33–34 battery text. **Seen, a bug:** 12's App info has "Unused apps" with the switch "Remove permissions and free up space", not "Unused app settings → Pause … if unused" (13 and 14 match the hint; 13 has "App battery usage", 12 "Battery") | **Seen (B25)** | A screenshot of the box and of App info | E2 (B25 is built: the new text), E3, E4 |
| Clipboard (`copy`) | Below 13 our toast "Copied", 13+ the system's own confirmation | Low | Screenshot after the long press | E1b, E3 |
| `PullToRefreshBox`, overscroll | Compose's overscroll is a glow on 10–11, a stretch on 12+; the pull itself the same | Low | `input swipe 540 600 540 1500 600`: "Page: …" in the log | E1b |
| `enableEdgeToEdge()`, the system bars | 15+ force edge-to-edge (already handled); on 10–14 `enableEdgeToEdge` asks for it, with a scrim behind 3-button navigation (these AVDs use 3 buttons, `navigation_mode` 0) | Low | Screenshots: nothing hidden under the status or navigation bar, in portrait and landscape (two panes) | All |
| Back (`BackHandler` in crop mode) | No `enableOnBackInvokedCallback`: 13–14 dispatch Back the old way | Low | ⛶, drag, `input keyevent KEYCODE_BACK`: crop mode closed, the part not kept | E1b, E3 |
| `ACTION_VIEW` (the picture, the title) | The AOSP images have no browser: the toast "No browser" (a real phone has one) | Low | The toast, no crash | E1b |
| `ApplicationExitInfo` (only our checks' `dumpsys activity exit-info`) | 11+; on 10 crashes only by `logcat` | — (tooling) | `logcat -d | grep 'E AndroidRuntime'` on 10 | E1b |

Android 11 (API 30) and 12L (32) aren't covered (user, 2026-10-29): for every row above they
behave like a neighbour (11: 10's fixed palette, 12's decoder and picker path; 12L: 12).

## What can differ on Android 17 (A17, by reading, 2026-11-22)

(`release-0.10.0.md`, A17: where E17 on `apodroid-37` should look.) Android 17 is API 37, stable
since 16 Jun 2026 (`android-17.0.0_r1`); the app targets 37. The FP6 (Android 16) already runs the
app at targetSdk 37, so the changes for apps targeting 36 are seen there; what's new on 17 is 17's
own behaviour for all apps and what targetSdk 37 turns on. Sources: the developer docs' "Behavior
changes: all apps" and "apps targeting Android 17", the release notes (Beta 1 to 4.1), the Network
Security Configuration and CT policy pages; AOSP `platform/frameworks/base`, branch
`android16-qpr2-release` against `android17-release` (the files below, and the aconfig flags 17
dropped, which means on for good); the SDK stubs `android-36` against `android-37.0`
(`WallpaperManager`, `AlarmManager`, `JobInfo`, `JobScheduler`, `Service`, `PendingIntent`: no
change to a method we call; `JobScheduler.getPendingJobReasonStats` and `ImageDecoder`'s default
listeners are new, unused). **No `BUG:` by reading.** A flag that was off on the FP6's 16 build
(LineageOS; its QPR level not checked) is on for good on 17, so the rows below name it.

| Android API (where in the app) | What differs on 17 | Likely to break | How E17 sees it |
|---|---|---|---|
| `setStream(stream, crop, true, which)` and `setBitmap(…, hint, true, which)` (`Store.set`, every still set) | `WallpaperCropper`: the per-orientation crop path is the only one (`multiCrop` gone, the old single-crop code removed; 15 and 16 already took it, seen). Two rounding fixes where a crop is fitted to the screen's shape: `noParallax` never removes a negative width, and `getAdjustedCrop`'s `heightToRemove` is rounded, not truncated: at most 1 px of a frame that isn't exactly the screen's shape (a picture narrower than the screen's shape, "my picture") | Low | `dumpsys wallpaper` (`mCropHints`, `mSampleSize`); SH4's pixel checks (`scripts/sh4_check.py`) on Centre, Auto, Whole, Sideways and the limited set, both screens, with 1 px of tolerance at the frame's edges |
| Home and lock apart (`FLAG_SYSTEM` alone, `FLAG_LOCK` alone, both) | Unchanged: `FLAG_SYSTEM` alone on a shared still wallpaper first moves it to the lock screen (`migrateStaticSystemToLockWallpaperLocked`; only its log text changed) | Low | M0's sequence: "Lock wallpaper state" in `dumpsys wallpaper` after each, screenshots of both screens |
| Live picked in Android's picker (`ACTION_CHANGE_LIVE_WALLPAPER`, Set's picker trip, SPLIT) | **Every pick rebinds the engine** (`setWallpaperDescription` passes `force = true`; 16 QPR2 had it behind `always_rebind_user_set_wallpaper`): picking APODroid again over a live home creates a new engine and destroys the old one, where the FP6 (16) kept the running engine (FP6 re-test, 2026-11-15). `COMMAND_REAPPLY` is then never sent by the picker (we read it below 14 only). Every pick still gives a new wallpaper id (`makeWallpaperIdLocked`, as before), so Set's "no pick" check by ids is unchanged; the new engine notes `live.homeId` at its start (`liveOn`) | Low–medium: a new engine draws black until its decode (L9a's ~70 ms), and its colours are told again | live T05–T09 and S5/S7 (re-picks): `Live: engine … destroyed` then `… created` in the log, `Live: drew …` within a second, a screenshot right after the return, `dumpsys wallpaper`'s component and id; no "Live isn't on" line after |
| `getWallpaperInfo(FLAG_LOCK)`, `getWallpaperId` (`Store.liveOn`, `lockEngine`, `liveShared`) | Unchanged: the lock screen has no record of its own while shared (info null, id −1); the info is ours while we can query our own package | Low | live S4/T16, S3/T22 if 17's picker offers "Lock screen" alone (AOSP's LiveWallpapersPicker on a plain image: which targets it offers is itself a finding) |
| The engine (`LiveWallpaper.kt`: `onSurfaceChanged`, `onVisibilityChanged`, `lockCanvas` on the main thread) | `WallpaperService.Engine` posts its surface updates as messages (was a direct call), may use a client-side surface (`relayout2`), guards the BLAST surface with a lock, and does local colour extraction only by `config_enableWallpaperLocalColorExtraction`. We draw on the main thread only | Low | background G, H: `Live: drew …` at a screen-on, a rotation and the minute tick; no `Live: couldn't …` |
| Default wallpaper until unlock (`use_default_wallpaper_until_unlocked`, new) | With the device's `config_useDefaultWallpaperUntilUnlocked` (false in AOSP) the system shows the default wallpaper until the first unlock after a boot, and the wallpaper file reads as null meanwhile. Nothing of ours runs before `BOOT_COMPLETED` (after the unlock) | Low | Not on AOSP's image; a WK device with it would show the default on the lock screen after a reboot until unlocked |
| `AlarmManager.setWindow(RTC_WAKEUP, T, 10 min)` + `setAndAllowWhileIdle` (`DailyJob.arm`) | Unchanged for `PendingIntent` alarms: `MIN_WINDOW` 10 min, the allow-while-idle "compat" quota (17's relaxed quota is only for the new `OnAlarmListener` variant). A new broadcast `ACTION_TIMEZONE_OFFSET_CHANGED` at DST transitions: not needed, our alarms are absolute times computed with the zone's rules | Low | `dumpsys alarm | grep -B2 -A8 apodroid`: two entries, request codes 1 and 2; "Daily job: alarms for …" |
| `JobScheduler`: job 2 `setExpedited`, `schedule()`'s `RESULT_FAILURE`, job 1 periodic, `getPendingJob` | 16 QPR2's flagged quota changes are on for good: an expedited job's time per 24 h in the WORKING bucket 15 min (was 30; ACTIVE 30, EXEMPTED 60, FREQUENT and RARE 10, RESTRICTED 5 as before); the reward for the user's interaction 5 s (was 15); jobs started while the app is on top count against the quota (`enforce_quota_policy_to_top_started_jobs`); thermal limits apply to jobs in a process with a foreground service (Keep running). Our run takes seconds (a 6 min PNG download is the exception), 24 a day with "As soon as it's out"; an exhausted quota already falls back to a plain job (O10). `getPendingJob`, the 150-job limit: unchanged | Low–medium (timing only; the fallback is O10's) | `dumpsys jobscheduler io.github.buerlino.apodroid` (the EJ quota lines) after a day's runs; `am set-standby-bucket io.github.buerlino.apodroid working_set`, then runs until "Daily job: alarm, run scheduled, not expedited" is logged and the run still sets |
| Keep running (`KeepRunning.kt`, `specialUse`, `START_STICKY`) | Unchanged: `SPECIAL_USE` is still in the types a `BOOT_COMPLETED` start may use (`DEFAULT_FGS_BOOT_COMPLETED_ALLOWLIST`), the background-start rules and exemptions the same. 17's background audio hardening (a while-in-use service for audio, target 37) doesn't touch us: no audio | Low | background I: the switch on, `dumpsys activity services io.github.buerlino.apodroid` (`isForeground=true`, type `specialUse`), a reboot and `install -r` with it on, Active apps in Quick Settings |
| Broadcasts (`AlarmReceiver`: `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED`) | `BOOT_COMPLETED` when an app leaves the stopped state is sent always (`maybeSendBootCompletedLocked`; 15 and 16 behind `stay_stopped`, seen on the tablet): after a force stop the page's start is followed by one more arming (harmless, B16). The others unchanged | Low | `am force-stop`, open the page: "alarms for …" twice; `adb reboot`, `install -r`, `cmd alarm set-timezone`, `cmd alarm set-time`: "alarms for …" each |
| A force stop and the detector (`DailyJob.checkAlarms`, `FLAG_NO_CREATE`; `Detector.kt`) | `forceStopPackageLocked` cancels the app's `PendingIntent`s whenever the package is stopped (17 dropped the `stay_stopped` check, which a maker could turn off on 15–16), so the pair is found gone as on the tablet and the FP6. Exit records are read on 30–34 only (`readsExitRecords`), so 17's new `REASON_OTHER` "MemoryLimiter" records and `AnrInfo` change nothing | Low | background J: `am force-stop` with the process alive, a change time missed, the page: `Detector: the alarms found gone`, the box; a Recents swipe and `am kill`: no box |
| App memory limits (all apps on 17, by the device's RAM; Beta 4: "conservative") | A process over the limit is killed (`ApplicationExitInfo` `REASON_OTHER`, description "MemoryLimiter:AnonSwap"). A run killed that way is a killed run (B19: counted failed, retried 15 min after its start). Our peaks: a huge PNG decoded subsampled, Live's decode within 50 MB (46 MB while turning), the page about 105 MB PSS with its pictures (Keep running; O12 lets them go at `onStop`) | Low–medium (limits unknown) | `am memory-limiter status`; `dumpsys activity exit-info io.github.buerlino.apodroid` after the large-picture, Live and Keep running cases: no "MemoryLimiter"; optionally `am memory-limiter manual <pid> 100` during a run: the retry 15 min after its start |
| `HttpURLConnection` (Conscrypt; `:core`'s fetches, the probe, ★'s original) and `MediaMetadataRetriever` over HTTPS (the video still) | Target 37: **Certificate Transparency on by default** and ECH (GREASE when the server offers none; only where the library supports it). All four hosts' certificates carry embedded SCTs (checked 2026-11-22 with `openssl s_client`: `science.nasa.gov` and `www.nasa.gov` Let's Encrypt, 2 each; `assets.science.nasa.gov` DigiCert, 3; `images-assets.nasa.gov` Amazon, 3). Without a log list on the device, or one older than 70 days, CT isn't enforced (the CT policy) | Low (a host that changes to a certificate without SCTs would fail as "no connection") | Every source's `Refresh:` line, the probe's `original …` lines, ★'s original (not the toast's smaller copy), the video still's `Video still: <ms> ms`; `logcat | grep -i 'SSLHandshake\|Certificate transparency'` empty |
| Configuration changes (the page, `MainActivity`) | Activities are no longer recreated for keyboard, navigation, touchscreen, colour mode and desk mode (target gating unclear in the docs); the page gets `onConfigurationChanged` and Compose its new `LocalConfiguration`. Rotation and size still recreate | Low | page C8 (rotation, the layouts); nothing else in the maps changes these |
| Large screens (target 37: `screenOrientation`, `resizeableActivity`, aspect ratios ignored at sw ≥ 600 dp, no opt-out) | The manifest sets none | None | — |
| Edge-to-edge, Back (`enableEdgeToEdge()`, `BackHandler` in crop mode) | As 16 for target 36 (forced edge-to-edge, predictive back by default; seen on the FP6); nothing new in 17 | Low | Screenshots in portrait and landscape (two panes): nothing under the bars; ⛶, drag, Back: crop mode closed, the part not kept |
| `PickVisualMedia` ("my picture") | The system photo picker as on 13+; 17 only adds opt-in customisation (`PhotoPickerUiCustomizationParams`) | Low | setup S3: a pushed picture picked, "my picture" set, `files/fallback.jpg` there |
| `MediaStore` (★ and Save every picture in `Pictures/APODroid/`, LOG's `Download/APODroid/log.txt`) | Nothing in the stubs or the docs (the stub diff is comments) | Low | ★: `content query --uri content://media/external/images/media --projection _display_name:relative_path:is_pending`; LOG on, a run: the line in `log.txt` |
| The pause hint (35+ texts: `App info › App battery usage › "Allow background usage": on › Unrestricted`, "Manage app if unused") | AOSP Settings 17's strings are the same ("App battery usage", "Allow background usage", "Unrestricted", "Unused app settings", "Manage app if unused") | Low | A screenshot of the box and of App info's two rows |
| Target 37's others: background activity starts through `IntentSender`, the lock-free `MessageQueue`, unmodifiable `static final` fields, native code loading, `setContentCaptureEnabled`, local network, widgets' bitmap limit | We start activities only from the page in the foreground (the notification's tap is the user's), reflect on nothing, load no native code, have no widget and no LAN traffic | None | — |
| Bubbles and desktop windows (17: any app can be bubbled) | The page in a small window: the panes follow the window's width; the wallpaper's shape is the display's mode (`Store.screen`), not the window, so a Set from a bubble frames as from full screen | Low | Optional: `am start --windowingMode 5` (freeform) if the image allows it, Set: the same `mCropHints` as full screen |

**For E17, first** (the rows most likely to show something): the picker's forced rebind (live T05–T09,
S5, S7), the expedited quota with "As soon as it's out" in the WORKING bucket, the memory limiter's
status and exit records after the large-picture and Live cases, the fetches under CT, the detector
after a force stop; then every map as the plan says.

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

5. **0.10.0** (user, 2026-11-22, `release-0.10.0.md`): the levels **10, 12, 13, 14 and 17**; 17 and
   14 and 10 every case, 12 and 13 the version table's rows and a smoke of each map; the update from
   0.9.1, 0.7.0 and 0.5.0 (UP). One level per night, `APODROID_TABLET=emulator-<port>`, rosie's
   checks by `interactions/tester-rules-emulator.md` (its serial per night). The old decision 3's
   W's candidate (0.6.0) is history: the candidate is dev's build.

## The setup (done 2026-10-29; `apodroid-37` added 2026-11-22)

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
  | `apodroid-37` | 17 | `emulator-5566` | CE2A.260420.051.A3 |

  **`apodroid-37`** (ER, 2026-11-22): `system-images;android-37.0;default;x86_64` (the plain image,
  no Google; revision 6, 2.5 GB installed), the same `avdmanager` line with `-n apodroid-37`
  (pixel_6, 1080×2400 at 420 dpi, 2 GB RAM, 4 cores). Booted once, wiped, with the start line below
  (`-port 5566`): `sys.boot_completed` after 24 s, the network 2 s later, `adb root` works (userdebug),
  `ro.build.version.release` 17, SDK 37, `wm size` 1080×2400, `mWidth=4800 mHeight=2400`, `cmd alarm`
  has `set-time`, `dumpsys activity exit-info` answers, Launcher3 the only launcher; the AVD's data 505 MB
  after it; stopped with `emu kill` (the process gone). No app installed on it yet.

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

**Where it stands (ER, 2026-11-22).** E0 as planned (`scripts/emulator.sh`, the `emulator` lock,
an `emulator E<n>` spec) was **never built**: 0.8.0's night ran the pipeline on the Android 14
emulator by `APODROID_TABLET=emulator-5564 scripts/night.sh` (`night.md`, For rosie), the emulator
started by hand with The setup's line, and rosie follows `interactions/tester-rules-emulator.md`.
That is the way for 0.10.0 too: one level per night, `APODROID_TABLET=emulator-<port>`, a row's
Tablet column "no", rosie's rows named by `release-0.10.0.md`'s `### [ ]` sections (E17, E14, E10,
E12, E13, UP). Not built, and not needed by them: the script, the lock (one night runs at a time),
the per-row device. **To do by hand before each night:** start that night's emulator wiped (The
setup's start line, `-wipe-data`), check `adb -s <serial> shell getprop ro.build.version.sdk`; the
pipeline does not start or stop it (stop: `emu kill`, after the night). The first boot of
`apodroid-37` ran on 2026-11-22 (below).

The rows below were written for 0.6.0 and kept as the cases per level; **what changed for today's
app** is in each level's list under "The levels" and in the rules here:
- **The build:** the candidate is the build the pipeline gives rosie (dev's debug build, and an R8
  build for the update cases: `release-0.9.0.md` T4), not W's candidate; no sha256 to check.
  Debug builds give `run-as`; the prefs and files as root otherwise (How a check runs).
- **The update** is from **0.9.1** (the F-Droid users' newest, versionCode 14), and UP adds 0.7.0
  and 0.5.0: `app/build/apodroid-v0.5.0.apk` and `-v0.7.0.apk` exist; 0.9.1's must be built from its
  commit beforehand (release-signed or debug-signed with the same key as the candidate, else
  `install -r` refuses). What the migrations and the updates do: `CLAUDE.md`, Migrations.
- **A `BUG:`** becomes a `### [ ] B<n>` package in `release-0.10.0.md` (a fix to the job or the
  rules restarts the week's count), re-tested on the level that found it; a STOP goes to the user.
- **Results** go under the package's section in `release-0.10.0.md` (`Checked`: the date, the
  pipeline run, the serial, the build, a few lines), evidence in `.coord/runs/<n>-<id>/`.
- **Reply** PASS, FIX, STOP or NOT RUN with `Tested:`, `Not tested:`, `BUG:`, `For the user:`,
  per case PASS / DIFFERENT / FAIL as T4 and T5 did.

### The levels: what to check on each (0.10.0)

Each level's list is E1a–E3's below **plus** these (each line says where it comes from). An item
needing a real maker or real timing is WK's.

**All levels (every level runs these):** LOG's file (`--ez weekLog true`, a run, `Download/APODroid/log.txt`
by `adb pull`; 10's MediaStore Downloads too); Size Auto's subject and Auto's turn (the log's
`Subject:` line, `, sideways, auto turn`); the darkened set and the limited set through `setBitmap`
(`debug-set.png` by pixels, `scripts/sh4_check.py`: the system's crop of a screen-sized bitmap);
Keep running's notification; the pause hint's per-maker path for the stock layout; the detector; Live.

| Level | Serial | What differs for today's app (beyond the rows above and E1a–E3's lists) |
|---|---|---|
| 10 (`apodroid-29`) | `emulator-5554` | **Auto's subject decoded on 10's `ImageDecoder`** (EXIF: an EXIF-6 and EXIF-8 photo noted and set at its subject; `Subject:` line, the frame's rows); **the video still's `MediaMetadataRetriever`** on 10 (a faked video day with `<source>.videoUrl` of a real NASA mp4: `Video still: <ms> ms`, the tile; a failure is logged and the tile stays black); the sideways and darkened `setBitmap` through 10's `generateCrop` (the frame's rows must survive its aspect fix: tall and wide pictures, numbered rows); Keep running's notification **in the shade** (`dumpsys activity services`: `isForeground`; `specialUse` and its permission don't exist below 34: the service must start without a crash at the switch); no `setExpedited` below 12 (job 2 a plain job, "not expedited" in the log); the detector: no exit records on 10, so only the `PendingIntent` sign (`am force-stop`, the page); Live: a live wallpaper is the home screen's only, the lock screen gets Fill (Decisions), `getWallpaperId` notes the engine's id (`live.homeId`) and `getWallpaperInfo` is null without `QUERY_ALL_PACKAGES`, so `Store.liveOn` takes the id path: the picker, a re-pick (`COMMAND_REAPPLY` unchecked), a lost Live; the pause hint: no line on 10 (nothing to name), the stock App info paths unchanged |
| 12 (`apodroid-31`) | `emulator-5558` | The colour event after **every** set (U1: the page recreated; Setup's and Advanced's state, the shown source, the draft, a Set's run still going); the pause hint's unused line alone (B25's text, built); DocumentsUI on 12 for "my picture"; job 2 **expedited** (`setExpedited` accepted, `dumpsys jobscheduler`'s expedited lines; forced Doze and battery saver with the network: `deviceidle force-idle`, `cmd power set-mode 1`; the fallback to a plain job when the quota is gone); the exit-record sign on 12 (TP1: `Detector: <n> exit records`, ten force stops with the process alive); Keep running's notification in the shade; the system's crop for the sideways and sharp sets; Live's re-pick and lock screen as on 10 (the id path below 14) |
| 13 (`apodroid-33`) | `emulator-5562` | The system photo picker; both hint lines (battery 33–34 text, "Pause app activity if unused"), colour events from the foreground app only (U1: a background set waits for sleep); Keep running's notification **only in Quick Settings' Active apps** (no permission asked); expedited job 2 and the exit-record sign as on 12; the id path for Live below 14, the lock screen Fill; the system's crop for the sideways and sharp sets |
| 14 (`apodroid-34`) | `emulator-5564` | `WallpaperCropper` (no upscale of a small crop: the sharp set's frames, a small "my picture"); `mIsLockscreenLiveWallpaperEnabled=false` on this first build (UE1A): the engine's flags, the picker offers no lock screen, Live's lock screen Fill; the exit-record sign as TP1f1 left it (ten force stops, a Recents swipe and `am kill`: no box); `getWallpaperInfo` works on 14, the `live.homeId` path as the fix of L8f1 describes; expedited job 2; Keep running (type `specialUse`, notification in Active apps); every map in full (0.8.0's and 0.9.0's runs ran a subset) |
| 17 (`apodroid-37`) | `emulator-5566` | `A17`'s table above, **first** the rows "For E17, first"; every map in full; targetSdk 37's behaviour applies only here |

### E0 (as it stood; not needed again)

The plan of 2026-10-29 for `scripts/emulator.sh` and the per-row device is kept in git
(`git log -p -- .claude/skills/build-apodroid-android/emulator.md`, before 2026-11-22). What the
nights use instead is above.

### The cases per level (written for 0.6.0; the 0.10.0 additions are the table above)

### E1a. Android 10, the wallpaper (`apodroid-29`, `emulator-5554`)

The riskiest: 10's own crop code (its aspect fix ignores the hint's top), the decoder (EXIF in the
codec) and DocumentsUI. The candidate installed fresh, the page opened once (APOD mode, title and
date, `darkColorScheme()`'s colours: a screenshot). In this order:
1. **Fill, Both** (Size Centre and Auto): Set: one shared wallpaper (no "Lock wallpaper state" id),
   `mCropHint` the frame in the screen's shape, `wallpaper` vs expected (`magick compare`), home and lock
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
6. **Sideways, darkened, sharp** (0.8.0, 0.9.0): ⛶'s Sideways pill on a wide post, Set; a bright
   picture (the darkening); Keep pictures sharp on with a small picture: each `setBitmap` set's
   `mCropHint` the whole bitmap and `wallpaper` by `sh4_check.py`'s numbers.
7. No crash (`logcat -d | grep 'E AndroidRuntime'`; no exit-info on 10).

### E1b. Android 10, the rest (`apodroid-29`, `emulator-5554`)

On a fresh start (wiped). In this order:
1. **The update from 0.9.1** (then UP's older ones): install 0.9.1's APK, open it, daily switch on,
   "Where" Both, let it set (`dumpsys wallpaper`). Home, then `install -r` the candidate **without
   opening the page**: the log's "Daily job: alarms for …" (`MY_PACKAGE_REPLACED`), the pair in
   `dumpsys alarm`, the prefs kept (`cat` as root), the wallpaper untouched (`dumpsys wallpaper`'s id
   the same). Then open the page: APOD mode, title and date (the pause hint has no line on 10). A
   stored APOD rendition smaller than the screen: the probe's `original …` line, no set.
2. **The job and the broadcasts:** the pair (`dumpsys alarm`, 10's layout), id 1 6 h/6 h
   persisted (`dumpsys jobscheduler`); `cmd jobscheduler run -f $P 1` ends as a success, job 2
   "not expedited"; `adb -s $S reboot` (wait for boot): "alarms for …" after `BOOT_COMPLETED`, no
   set at boot; `cmd alarm set-timezone America/New_York` and back (or `setprop persist.sys.timezone`
   as root if `cmd alarm` lacks it on 10): armed again; the clock 3 h forward with `auto_time` 0, then
   `auto_time` 1: armed again (`TIME_SET`).
3. **★:** save, `content query` (as in the table): `Pictures/APODroid/APOD_…`, `is_pending` 0;
   ★ filled; `content delete` it: ☆ at the next resume. LOG's file: `Download/APODroid/log.txt`.
4. **The page's rest:** long press on the title: the toast "Copied" (screenshot); pull to refresh
   (the log's "Page: …"); ⛶ then `KEYCODE_BACK`: closed, the part not kept; the picture tapped:
   "No browser"; landscape (`settings put system accelerometer_rotation 0; settings put system
   user_rotation 1`, back to 0 after): two panes, nothing under the bars; the Set bar with Setup open.
5. **Keep running, the detector, Live, the video still** as the table above says.
6. No crash (`logcat -d | grep 'E AndroidRuntime'`; no exit-info on 10).

### E4. Android 14 (`apodroid-34`, `emulator-5564`)

14's own crop class (`WallpaperCropper`: no upscale of a small crop), the lock data of 14's first
build (`mIsLockscreenLiveWallpaperEnabled=false`), `mWidth` 4800, and 34's receiver and job rules.
1. **Install and the page:** the page loads in APOD mode, no `SecurityException` (the clock
   receiver), dynamic colours (screenshot); the pause hint with both lines and the 33–34 battery
   text; "App info" opens App info; its "App battery usage" and "Unused app settings → Pause app
   activity if unused" are where the lines say (screenshots).
2. **Fill, Both; ⛶ moved; Home, then Lock, then Both:** as E1a's 1–3, with `dumpsys wallpaper`'s
   lock state and both files each time.
3. **Small and tall:** "my picture" through the photo picker (screenshot): the tiny 200×150 and
   the tall 1000×4000 on a faked video day: `wallpaper` not scaled up by the system, the home
   screen shows it filled and undistorted; the panorama; EXIF-6; the sharp set on a small post.
4. **The whole picture** on blur; sideways and darkened sets (E1a's 6).
5. **U1 on 14:** Set on the page with the page open: `am_create_activity` or `am_relaunch` for
   the page right after (screenshots before and after), the toast, Setup closed as after any Set,
   nothing lost; then a job's set in the background with the page on top: no recolour until
   `KEYCODE_SLEEP`, then after `KEYCODE_WAKEUP` the new colours and the page recreated (say which).
6. **The job:** the pair, both jobs in `dumpsys jobscheduler` (the network constraint accepted on
   34), `run -f 1` a success; a time-zone change while the page is open: the status line's time
   follows; `install -r` from 0.9.1: armed again.
7. Clipboard (no "Copied" toast on 14), no crash (`exit-info`).

### E2. Android 12 (`apodroid-31`, `emulator-5558`)

12's SystemUI recolours after every wallpaper change (U1), the unused line alone, and
DocumentsUI on 12 (R extension 1).
1. **The page and the hint:** dynamic colours; the pause hint with the unused line alone, B25's text built ("App info › Unused apps › \"Remove permissions and free up space\": off"; the old "Unused app settings" wording on 12 is a `BUG:`); App info's switch off → the line gone at the next resume (screenshots).
2. **U1 on 12:** Set on the page: recoloured and the page recreated? (as E4's 5); then a job's set
   in the background with the page on top: **12 accepts it at once**: Setup's and Advanced's state,
   the shown source and the draft after it (B13's path); a Set whose run is still going when the
   recolour comes (Save every picture at No limit makes a run long enough): the run's end and the toast.
3. **Fill, Both, and Home then Lock:** as E1a's 1 and 3 (`mWidth` 4800 here).
4. **"My picture" through DocumentsUI on 12:** the EXIF-6 JPEG and the tall picture on a faked
   video day, as E1a's 5.
5. **The job:** the pair (12's layout), `run -f 1`, job 2 expedited, no crash, "Copied" toast.

### E3. Android 13 (`apodroid-33`, `emulator-5562`)

1. **The page and the hint:** dynamic colours, both lines, App info's "App battery usage" and
   "Unused app settings" as the lines say (screenshots).
2. **"My picture" through the photo picker** (screenshot): WebP, GIF, EXIF-8 and the panorama,
   each set on a faked video day: `wallpaper` upright and screen-shaped.
3. **Fill, Both; Home then Lock:** as E1a's 1 and 3.
4. **U1 on 13:** as E4's 5 (the background set waits for the screen to sleep).
5. **The rest:** no "Copied" toast (the system's overlay), `KEYCODE_BACK` in crop mode, the job's
   pair and `run -f 1`, no crash.

## For 0.7.0 (folded into the levels' table, ER, 2026-11-22)

Keep running's notification (10–12 in the shade, 13+ only in Active apps, the service starting
without `specialUse` below 34), the expedited run (12+; the plain job on 10), the pause hint per
maker (the stock paths), the detector, Live's lock screen on 10–13 (Fill), the `getWallpaperId` path
below 14 and the colours (12 after any set, 13+ from the foreground app) are lines of "The levels"
above, with 0.8.0's and 0.9.0's (the sideways, darkened and limited sets through `setBitmap` and the
system's crop, Auto's subject decoded on 10's `ImageDecoder`, the video still on 10, TP1 on 11–14).
The first run's results are the baseline that tells a regression on 10–14 from a 0.6.0 behaviour. An
emulator never stands in for the tablet's or a phone's timing or maker layer.
