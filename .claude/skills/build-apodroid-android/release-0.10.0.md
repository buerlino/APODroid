# 0.10.0: the hardening release, then 1.0.0 (approved by the user, 2026-11-22)

(User, 2026-11-22, after 0.9.1: "a full test on emulator … and then also a full test on the newest
android, to really battle harden the app, i want to find any remaining bugs and then release
1.0.0".) Decided (user, 2026-11-22):
- 0.9.1 shipped first. **0.10.0** = the hardening: tests and their fixes, released. **1.0.0** =
  0.10.0's tree (with any fixes) after a real week on three devices with no `BUG:`.
- **A feature freeze until 1.0.0**: only bug fixes, no new settings or texts unless a bug needs
  one; a fix that touches the job or the rules reruns that part (0.6.0's rule). The one exception
  is LOG (below), a test tool the user approved.
- Emulators: **Android 10, 12, 13, 14 and 17** (`apodroid-29/31/33/34`, a new `apodroid-37`); 15 and
  16 are the tablet and the FP6. 17 matters most: targetSdk 37's behaviour applies only there, and
  no device runs it.
- Besides the maps on every level: a fresh **code review**, **SIM** over many more seeds, the
  **update from old versions**, **Android 17's rules by reading**.
- Run by the night shift (`scripts/night.sh`) once the user approves this file; queue rows go into
  `night.md` then.
- **The session limit** (user, 2026-11-22: no longer on the largest plan): each package names its
  model and effort; Sonnet wherever it's enough; checks by scripts (adb, `dumpsys`, pixel checks as
  SH4's `scripts/sh4_check.py`) rather than agents reading screenshots; one package per night slot,
  the queue resumable after a limit hit (night.md's rules).

## The packages, in order

| # | Package | Model | Device | Why |
|---|---|---|---|---|
| 1 | **LOG** the week's log file | Opus, medium | emulator 34 | The week needs a record; logcat keeps about an hour |
| 2 | **A17** Android 17's rules by reading | Opus, high | none | Tells E17 where to look |
| 3 | **ER** `emulator.md` for today's app, `apodroid-37` set up | Sonnet, medium | the PC | emulator.md was written for 0.6.0 |
| 4 | **SIM** 20,000 seeds | Sonnet, low | the PC | The rules gave most of the bugs; cheap |
| 5 | **CR** code review of the whole app | Opus, high (3 reviewers by area, each finding verified) | none | Bugs by reading; review-0.7.0-runs found real ones |
| 6 | **E17** Android 17: every map | Opus, medium | `apodroid-37` | The newest Android, targetSdk 37 |
| 7 | **E14** Android 14: every map | Sonnet, medium | `apodroid-34` | 0.8.0's and 0.9.0's runs ran a subset |
| 8 | **E10** Android 10: every map | Sonnet, medium | `apodroid-29` | The oldest: crop code, decoder, storage, DocumentsUI |
| 9 | **E12, E13** Android 12 and 13: emulator.md's rows that differ by version + a smoke of each map | Sonnet, medium | `apodroid-31`, `-33` | Each map in full on five levels would cost too much; the version table says what differs |
| 10 | **UP** the update: 0.5.0, 0.7.0 and 0.9.1 each straight to the candidate | Sonnet, medium | `apodroid-34`, `-37` | F-Droid users skip versions; the migrations' last real check |
| 11 | **B&lt;n&gt;** a fix package per `BUG:` | per bug | where it was found | Re-tested on the level that found it |
| 12 | **R5** 0.10.0's texts (versionCode 16, `16.txt`; 15 is 0.9.2), the tag | Sonnet, low | none | After 1–11 with no open `BUG:` |
| 13 | **WK** the real week on three devices, **CR2** (a second code review) alongside | the user; Opus, medium to read the logs; CR2 Opus, high | FP6, tablet, third | Real nights, makers' own Android |
| 14 | **R6** 1.0.0 | Sonnet, low | none | After WK: at least 7 days, the last 2 on the current build with no `BUG:` |

"Every map" = the cases of `interactions/` (setup, page, background, live) that can run on an emulator
(`tester-rules-emulator.md`); a case that needs a real maker (ZUI's cleaner and picker) is WK's.
Each E package reports PASS / DIFFERENT / FAIL per case, as T4 and T5 did.

### [ ] LOG. The week's log file (Opus, medium)

The app's own log lines (`Log.i/w/e("APODroid", …)`, every one) also go to a file, **only after
it's turned on by adb**, so no user ever sees it:
- On: `adb shell am start -n io.github.buerlino.apodroid/.MainActivity --ez weekLog true` (the
  launcher activity is exported, so a release build takes the extra); off with `false`. The pref
  `weekLog`, default off. No UI, no text, no permission.
- The file: `Download/APODroid/log.txt` through `MediaStore.Downloads` (no permission on 10+; a
  user can copy it with a file manager), each line with the local time and the process id; at
  about 1 MB the older half is dropped. Written off the main thread, never failing a run (a write
  error is dropped).
- Each process start adds one line: the version, Android's version, the standby bucket, battery
  optimisation on or off, Keep running, the stored setup in one line. So a week's file reads
  without `dumpsys`.
- Pull: `adb pull /sdcard/Download/APODroid/log.txt`.
- One `Log` helper replaces the direct calls (a mechanical change); `:core` unchanged.
- Device: on the emulator, turned on, a run, a Set, `am kill`, a reboot: the lines in the file,
  in order; off: nothing more written; with it never turned on, no file exists.

### [ ] A17. Android 17's rules by reading (Opus, high)

The behaviour changes for apps targeting API 37 and for all apps on Android 17 (the developer docs'
pages; AOSP where they're vague), checked against the code: alarms (`setWindow`,
`setAndAllowWhileIdle`), jobs (expedited, quotas, `getPendingJob`), foreground services
(`specialUse`, start from the background), `WallpaperManager` (crops, `setStream`, `setBitmap`, the
lock screen, live wallpapers and the picker), broadcasts (`BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`),
exit records, edge-to-edge, the photo picker, `MediaStore`. Output: a table in `emulator.md` as
the one for 10–14 (what differs, how likely it breaks, how E17 sees it), and a `BUG:` line for
anything that's broken by reading alone.

### [ ] ER. emulator.md for today's app (Sonnet, medium)

As `release-0.9.0.md`'s ER: E0's state (0.8.0's night already ran rosie on `emulator-5564`),
E1a–E4 on today's tree, the update from 0.9.1 (not 0.5.0), plus `apodroid-37` (the plain
`system-images;android-37.0;default;x86_64`, no Google; port 5566) set up and booted once.

**Built** (winston, pipeline run 160, 2026-11-22): `emulator.md` brought to today's tree (the packages
section: where E0 stands, the levels' table, E1a–E3's cases with 0.8.0's, 0.9.0's and the "For 0.7.0"
lines folded in; The setup: `apodroid-37`, the update from 0.9.1); `apodroid-37` installed
(`system-images;android-37.0;default;x86_64`) and created, booted once on `emulator-5566` (24 s, the
network, root, `mWidth` 4800) and stopped; the packages below, their queue rows in `night.md`.

### [ ] SIM. 20,000 seeds (Sonnet, low)

`RulesSimulationTest` with 20,000 seeds (so far 2000), by its seed property, on the PC; every
failing seed reduced to its first wrong day and reported as a `BUG:` with the seed.

### [ ] CR. Code review (Opus, high)

Three reviewers by area, each blind to the others: (a) the job, the alarms, the rules and the
detector (`DailyJob`, `AlarmReceiver`, `Store.update`/`setDue`, `:core`'s rules); (b) the
wallpaper and Live (`Store.set`, `Drawing.kt`, `LiveWallpaper`, the picker trip); (c) the page
(`MainActivity`, the draft, the saved state, ⛶). Each finding verified by a second agent before it
becomes a `BUG:`. Correctness only, no style.

Checked (pipeline run 163, 2026-11-22, on dev c6faa2c; `review-0.10.0-cr.md`): one reviewer for
all three areas, each finding verified in a second pass (the multi-agent workflow was refused that
night). Two `BUG:` lines: **Android 10–12 never stores a new picture since 0.9.0** (high; SH2b's
probe calls `InputStream.readNBytes`, API 33, in `:core`, which lint doesn't check; seen in the
dex), and ⛶'s "Whole picture" preview held while the page is stopped (low, O12). One plausible
(the page ignores a source picture's EXIF orientation; no real post seen with one).

### Rules for the E packages and UP (ER, 2026-11-22)

Each is rosie's, on the emulator the section names, one level per night:
`APODROID_TABLET=emulator-<port> scripts/night.sh` (night.md, For rosie), the emulator started
wiped by hand first (`emulator.md`, The setup's start line; `-port` by its serial) and stopped after
(`emu kill`); `interactions/tester-rules-emulator.md` with that serial in place of `emulator-5564`.
The cases: `emulator.md`'s "The levels" (that level's row, "All levels", the version table above it,
and for 17 the A17 table with "For E17, first") and the maps in `interactions/` that run on an
emulator. The build: dev's debug build, and an R8 build (signed with the debug key) for the update
cases. Each case PASS / DIFFERENT / FAIL / SKIPPED (why); a FAIL is a `BUG:` line (steps, expected,
got, evidence, the likely file), which the user turns into a `### [ ] B<n>` package here; a crash is
`exit-info` and `logcat -b crash`. Evidence in `.coord/runs/<n>-<id>/`; the package's `Checked`
paragraph under its section. Never the tablet or the FP6; real timing and makers' layers are WK's.
Done when: every case ran or is under `Not tested:`; no unexplained FAIL; the emulator stopped.

### [ ] E17. Android 17, every map (`apodroid-37`, `emulator-5566`; Opus, medium)

All four maps (setup, page, background, live) and the version rows of A17's table, the rows "For E17,
first" before the rest. Besides: the picker's forced rebind (live T05–T09, S5, S7), the expedited quota
with "As soon as it's out" in the WORKING bucket, `am memory-limiter status` and exit records after the
large-picture, Live and Keep running cases (no "MemoryLimiter"), the fetches under Certificate
Transparency (every source's `Refresh:` line, the probe, ★'s original, the video still), the detector
after a force stop (a Recents swipe and `am kill`: no box), LOG's file. The first boot ran in ER
(2026-11-22: 24 s, no app on it).

**Checked** (rosie, pipeline run 164, 2026-11-22, `emulator-5566`, Android 17 CE2A.260420.051.A3, dev's
debug build 0.9.2 at 79982b5, targetSdk 37; evidence in `.coord/runs/164-e17/tablet/`): the rows "For
E17, first" and the A17 rows below; **no FAIL, no crash** (`logcat -b crash` empty, no `E AndroidRuntime`).
The four maps in full did **not** run (the 50 min slot): see Not run.
- **Live, the picker's forced rebind** (live T01, T05–T09, T11, T12, T15, T31, T33's OK leg, T34, T36):
  PASS. AOSP 17's picker offers "Home screen", "Lock screen" and "Home screen and lock screen".
  Every pick of APODroid over a live home ("Home screen and lock screen", T11 and T33) logged
  `Live: engine 1 destroyed` then `Live: engine 3 created` (17's forced rebind, as A17 read it), a
  new wallpaper id, `back from the picker, a wallpaper changed, home live, shared` and `shown live: …
  on HOME+LOCK`. **DIFFERENT** (A17's predicted risk, accepted kind): the new engine drew black three
  times for ~200 ms before its first picture (`drew black (nothing)` 47.687–47.743, the picture
  47.845); the page was on top then. T05/T06 (Back, a tap outside) and T08 (OK › Back from the preview):
  `Set: no pick, nothing saved`, ids unchanged; T07 the question survived rotation, Home and the
  launcher; T09 "Not now" twice: each asked, `Set: Live on the lock screen not now`, lock its own still,
  one status line, summary "NASA · both screens · 6:00 AM · Live". T36 ("Lock screen" alone, runnable
  here): `Live: back from the picker, the lock screen alone`, `Set: no pick, nothing saved`, `Live: the
  engine is on the lock screen alone, Fill on LOCK`, `Set: the lock screen put back: …`, home's engine
  and id 7 kept. S7/T31: summary "APOD · lock screen · 6:00 AM" without " · Live", no dialog. S5/T34:
  one set `on HOME+LOCK, auto 0.35`, one shared still, the engine destroyed. T15: `Live: engine flags 1`.
- **Expedited quota, "As soon as it's out"**: the WORKING bucket SKIPPED: this image has app standby
  off (`mAppIdleEnabled=false`; every app EXEMPTED; `am set-standby-bucket … working_set` ignored, also
  unplugged with `app_standby_enabled`/`adaptive_battery_management_enabled` 1, put back). Seen: the
  hourly alarm (clock stepped to 10:21:50) → `job 2 starts, expedited`, the pair's second `a run is
  going`; `qc_ej_limit_working_ms=900000` (15 min, as A17 read), active 30, exempted 60, frequent and
  rare 10 min.
- **Memory limiter**: `am memory-limiter status` = `disabled` on this image, so no MemoryLimiter record
  can appear (none in `exit-info`: 3 × USER REQUESTED/FORCE STOP, 3 × SIGNALED 9 from the kills).
  Live's decodes 3213×2400 in 37–528 ms, Keep running on through the force stops and the reboot. The
  large-picture case (a 37 MB PNG) not run.
- **Certificate Transparency** (target 37): PASS. `Refresh: APOD … rendition (original 3709×2772)`,
  `Refresh: NASA 2026-11-20 rendition (original 4200×3150)`, `Refresh: EARTH 2026-11-20 original
  1440×960 (smaller than the screen)` (the probe on all three hosts), ★ saved the original (1,972,191
  bytes, `Pictures/APODroid/`, not the toast's smaller copy), the video still (a faked video day with a
  real APOD mp4) `Video still: 776 ms, 864×1080`; no `SSLHandshake` or CT line. The first try of the
  still failed once, `couldn't make one … no frame` after `MediaHTTPConnection: readAt 855754 / 32768 =>
  java.net.ProtocolException` mid-stream (the server answers that Range with 206 from the desktop): a
  transient read error, logged, tile black, as designed; the retry (after removing `videoStillTried`)
  made it. `images-assets.nasa.gov` not fetched (no such post today).
- **The detector**: PASS. A Recents swipe, then a missed change (clock to 23 Nov 06:45): the alarms
  fired, `job 2 … expedited`, no box. `am force-stop` with the process alive, then a missed change:
  `Detector: the alarms found gone, after the run of …`; with nothing due (no newer post) `Detector:
  nothing missed`, no box (the rules); with the record faked a day back, `Detector: missed the change of
  2026-11-25T05:00:00Z (Keep running on)` and the box "Your phone still stops APODroid: the 6:00 AM
  change came late." with "A force stop ends the changes until APODroid opens." and "App info". A
  reboot: `found gone (booted true …)`, nothing noted. `am kill` not run (Keep running on keeps the
  process). 17's `BOOT_COMPLETED` on leaving the stopped state: seen after each force stop's next start
  (an extra arming, harmless), and once also at a start after a `kill -9` (09:38:25; not after the
  previous one), harmless.
- **Keep running**: PASS. `isForeground=true`, `types=0x40000000` (specialUse), Quick Settings "1 app
  is active"; after `adb reboot` `BOOT_COMPLETED, arming again`, the pair armed, foreground again.
- **The rest seen**: the page in APOD mode and with "More NASA pictures"; the daily switch on (`Switch
  on: … on HOME+LOCK, auto 0.35, darker ×0.80`, the pair: request codes 1 and 2, 10 min and 1 h
  windows); each Set's still through `setBitmap` (`mCropHint` 0,0–1080,2400; `mCropHints={}`); the
  pause hint (screenshot `02-pause-hint.png`, read in fix round 1: "Android pauses apps you don't open.",
  `App info › App battery usage › "Allow background usage": on › Unrestricted (on Optimized, a new picture
  can come hours late)`, `App info › Unused app settings › "Manage app if unused": off`, buttons "App info",
  "Battery optimisation", "Don't show again": the 35+ texts of A17's row, word for word); the weekday dialog; **LOG**: on by
  `--ez weekLog true`, `Download/APODroid/log.txt` 269 lines, 8 `Process start` lines (one per process,
  the reboot's included), in order, off at the end. The fresh install logs `Migrated from 0.5.0:
  Migration(write={allSources=false}, …)` (M2's write at a fresh install, as `CLAUDE.md` says); the
  maker reads "unknown" on this image (stock texts).
- **Not run**: the four maps in full (setup, page and background beyond the items above; live T02–T04,
  T10, T13–T14, T16–T30, T32, T35, T37–T39); `am kill`'s no-box case; the WORKING-bucket quota (the
  image); the large-picture case; SH4's pixel checks (`sh4_check.py`) of Centre, Auto, Whole, Sideways
  and the limited set; M0's home/lock sequence; edge-to-edge and landscape screenshots; setup S3 (the
  photo picker); the time zone and `TIME_SET` broadcasts beyond the clock steps; `install -r` with Keep
  running on (`MY_PACKAGE_REPLACED`'s arming, the service back in the foreground; A17's Keep running and
  broadcasts rows); the pause hint's texts against 17's App info (A17's row: App info's two rows not
  captured, so 17's own labels weren't compared on the device); freeform windows; the R8
  build. The emulator is left running for the pipeline (stop: `emu kill`), the app uninstalled.

### [ ] E17a. Android 17, the setup map (Sonnet, medium)

`interactions/setup.md`, section 2, the phone test list in full, in its order. Device `emulator-5566` (`apodroid-37`, Android 17), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5566` instead of 5564; the cases E17
ran (its Checked above) aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

**Checked** (rosie, pipeline run 166, 2026-11-22, `emulator-5566`, Android 17, dev's debug build 0.9.2,
fresh `pm clear`; ~35 min; evidence in `.coord/runs/166-e17a/tablet/`: logs `pNN-log.txt`, `pNN-wp.txt`,
screenshots, prefs before/after, `crash.txt`, `exit-info.txt`): **no FAIL, no crash** (`logcat -b crash`
empty, no `E AndroidRuntime`; `exit-info` only my force stops, one `kill background`, one `remove task`).
The start state P0 describes (home NASA, lock APOD, daily on, Live on home, "Home: NASA · Lock: APOD ·
6:00 AM · Live") was built first by hand (mode on, the list, Live, AOSP 17's picker "Home screen", the
daily switch); dates in the map move on (anchor 23 Nov, post 22 Nov / NASA 20 Nov); the clock reads 12 h.
- **PASS:** P0 (summary, status lines, Earth's greyed "Both", the picker opens for "Use my picture" with
  no picture, Back keeps "Skip it"; no Keep running look), P1 (`NASA on HOME auto 0.69, APOD on LOCK auto
  0.35`, home ImageWallpaper, no ` · Live`), P2 (same `Set:` line, both ids change), P3 (no `Set:`, ids
  kept, `orderFrom` 23 Nov), P4 (Setup untinted after the mode round trip, no `Set:`, ids kept; mode off:
  Where shows the draft "Lock screen", highlighted), P5 (dialog survives rotation, "Change every" 24 h, ⓘ
  with the draft's values), P6, P7 (`orderFrom` kept, owners kept, summary "Home: NASA, Earth · Lock: APOD,
  Earth · every 24 h from 6:00 AM"), P8 (no alarm pending, only `alarm_cancelled` history), P9 (one
  `Earth on HOME+LOCK`, no `Daily job: alarms`), P10 (`Switch on: … Earth`, one status line "Changed 22 Nov,
  10:30 AM · Earth", summary as the map), P11, P12 (NASA tick, APOD dropdown and the video dropdown
  disabled), P13 (`APOD on LOCK` only, home id kept, status "Lock: 22 Nov, 10:32 AM"), P14, P15, P16 (lock
  keeps its own wallpaper, new id), P17 (the Where of mode off after the Set is the stored one, NASA's "Both"
  back in the list), P18, P19 (`am kill`: Darken still off, Setup open), P20 (one MainActivity record,
  "delivered to the running instance"), P22, P23, P24 (no `, darker`; Setup closed on the rotated page), P25
  (`darker ×0.82` and `×0.80`), P26 (`alarms for 2026-11-22T10:18:45Z` = 11:18 local), P27 (`alarms for
  2026-11-23T05:03:45Z`), P28 (3 taps from off: one `Switch on:`, alarms armed, ends on), P29 (no
  picker the second time, the draft's pick; no `fallback.jpg` ever existed, `fallback-draft.jpg` stays),
  P30 (`Live: back from the picker, a wallpaper changed, home live`, `Live: the engine is on HOME, not
  noted live`, `Set: … on LOCK; wallpaper shown live … on HOME`, summary with ` · Live`, home
  LiveWallpaper, lock ImageWallpaper; the dialog "Next, in Android's preview: Set wallpaper › Home
  screen"), P31 (no "Wide pictures sideways" and no "Centre when unsure" row under Centre, Auto or Live;
  "Keep pictures sharp" under Centre and Auto, gone under Live; landscape two panes), P32 (galaxy text:
  `auto 0.56, sideways, auto turn`, `debug-set.png`; horizon text: `auto 0.83`, no `, sideways`,
  `debug-set.png` gone), P33 (Auto: ⛶ opens with "Sideways" on; off + ✓: `Part: … auto 0.83`, no
  `sideways`, `sideways=false`; Centre: pill stays off; on + ✓: `, sideways` without `auto turn`), P34
  (NASA's 3000×1000 file upright, `auto 0.69`), P34c (UI1: the bar over the scroll, the last row "Advanced
  settings" above it in portrait and at font scale 2.0, the bar in landscape's settings pane; closed: the
  button after the Setup row), P34d (ⓘ "A small picture is enlarged at most 2×; the rest of the screen shows
  it blurred.", on kept after a rotation, gone under Live and back off under Auto), P34e (1500×1022 horizon:
  `auto 0.77, sharp ×2.00 (Fill ×2.35)` and `debug-set.png`; Centre: the same without `auto`; off: no
  ", sharp", no `debug-set.png`), P35 (three segments in portrait and in the landscape pane, none wrapping),
  P36 (`auto 0.80`, crop 833–1292 against the middle 521–980, Centre without `auto`, Auto again as the first).
- **DIFFERENT (noted, not wrong):** P21: Back, then the launcher icon, with the process alive: the
  draft is dropped (Darken on again, Setup closed); the map asks only to record it. P29: `fallback.jpg`
  "unchanged" is moot, none existed (a Set with "Skip it" renames nothing).
- **SKIPPED:** P34a (the update from 0.8.0: no 0.8.0 build on this run).
- **Method notes:** the faked posts (P32–P36) need the emulator offline (airplane mode, undone and checked):
  online, the refresh stores the real title, alt and explanation over a faked same-date post and Auto's
  turn then keeps the picture upright. A `force-stop` with Live on home drops it (Android's rule): the page
  shows "Live isn't on. Set as wallpaper turns it on." and a Set with the picker puts it back (done at the
  end). `uiautomator dump` resets `accelerometer_rotation` to 1, so a rotation case sets it to 0 again.
- **Not run / not looked at** (each a gap, not a pass): the toasts ("Wallpaper set" etc.) were not captured
  (a screenshot at P24's rotation was not opened); the pixel colours of the `tertiary` highlights (P3's
  ≡, P4, P17, P27's "Skip it" round trip) were read from the layout (`chk`/text) and from the screenshots
  of P4 and P17 (Setup title untinted, Where tinted) only; P0's Advanced › Keep running and the page's title
  and chip (P15, P9's "On wallpaper" chip was seen only as a node); P10's Set disabled during the run
  (too short to catch); P5's ⓘ was opened after the rotation back, not with the rotation; P30's closing look
  (mode off shows Where W0, Earth's greyed dropdown, order); P32's `debug-set.png` turn not looked at by the
  pixels (the log line and the file are the evidence); P31's screenshots (`p31-*.png`) taken, not opened.
  The pre-0.9.0 ids of the map (`16 Nov`) are today's 23 Nov in the logs.

### [ ] E17b. Android 17, the page map (Sonnet, medium)

`interactions/page.md`, sections 3 (the prioritized list) and 4 (0.8.0's emulator cases). Device `emulator-5566` (`apodroid-37`, Android 17), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5566` instead of 5564; the cases E17
ran (its Checked above) aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

**Checked** (rosie, pipeline run 167, 2026-11-22, `emulator-5566`, Android 17, dev's debug build 0.9.2 (the run's
`app-debug.apk`), fresh `pm clear`; evidence in `.coord/runs/167-e17b/tablet/`: screenshots, prefs before/after,
`wp-*.txt`). S0 on the emulator: daily on, More NASA on, home NASA, lock APOD, Size Auto (the state after the first
Set; no Live, as the emulator was found). **No crash, two BUG lines (a), (b)** (`logcat -b crash` empty, no `E AndroidRuntime`;
`exit-info` only my `kill -9`s and `kill background`s). Section 3 (30 steps), section 4 (31–36) as below; per case:
- **PASS:** P-baseline; T1, L1, H1 (APOD "On lock screen", NASA "On home screen", Earth none: not ticked), W1–W3 (the
  ends stop); T2/T2a/T2c (order APOD, NASA, Earth, ✓, Back dismisses, Earth chosen); C1–C3, C5; C6, C7 (the system's
  clipboard preview, no app toast; the credit's long press not run on its own); C8 (expanded, landscape and back, still
  848 px high); O1 (two panes in landscape), O2/R5 (the pull on the picture pane), A1, A2, O3 (Advanced open, the ⓘ
  dialog survived both rotations); P1 (browser, Back: APOD); S1a (one row, `EO_2026-11-20_…`), S1b ("Already saved");
  O4 (after the row's deletion Home + reopen: ☆); K1, K1a (Earth: drag, Back, ⛶ again opens at Auto's subject, not
  the dragged place); K10, K11; K1c (`earth.cropPosition` 0.62 saved, no toast, no `Part:` line); K2 (Whole
  on, ✓: `Part: wallpaper set … on LOCK`, lock id 95 → 96, L2 "Lock: 11:15 AM"); K3; K6 (with Live: the frame at p 0.29 = `Live: drew … p=0.29`, no drag, ✓ unchanged: no line), K7
  (`Part: wallpaper shown live … on HOME`, `p=0.50`, ids unchanged), K13 (Home: crop mode still open; Back closes), K8
  (`p=0.29 ×0.83` again); S3 (airplane on: "Saved a smaller copy: the original didn't load", `Original failed, saving the
  stored picture`), R2/R2a ("Earth Observatory: no connection.", NASA's and APOD's own, `Update failed`), BP1/BP2, R3
  (airplane off, ping works, Home + reopen: no problem box); BH0–BH3 (App info and Battery optimisation open and Back
  returns; Don't show again › Cancel: `pauseHintHidden` not written), **BH4**: 17's App info has "App battery usage" with
  "Allow background usage" (a switch) and "Unused app settings" › "Manage app if unused", the hint's words word for word
  (`bh-appinfo.txt`, `bh-battery.png`); A3, A3a, A3b (Save every, 50 MB, back to 20 MB, off), A4, A5, A5a (Keep running's
  ⓘ; on: `isForeground=true types=0x40000000`, off: service gone); D1 (lines, hint box and Advanced gone, alarms
  `alarm_cancelled`, job 1 and the service gone, summary without the time), D1a (a pull sets nothing, ids unchanged); D2
  (`Switch on: wallpaper set …`, `job 1 starts, not expedited`, `jobFinished`, alarms for 23 Nov 05:01Z, "Last successful
  run"), L3; the Live trip on the way (dialog "Home screen", AOSP's picker, "Home screen": `back from the picker, a
  wallpaper changed, home live`, id 98 `SET_LIVE`, lock its own still) and out of it (Size Auto and Set: the engine
  destroyed, both still); section 4: P3a (`Video still: 720 ms, 1080×608` with `eso2612b.mp4`; after a `kill -9` and
  reopen no line), P3c (airplane on: `Video still: couldn't make one of …: setDataSource failed`, after about 35 s, black
  tile; next open no line), P3d (`videoUrl` removed: black, no line); K16, K16b, K17 (`on LOCK, sideways, darker ×0.70`),
  K24, K24a–c (APOD faked "A Spiral Galaxy in Space", the text without an up word: Set
  `on LOCK, auto 0.21, sideways, auto turn`; ⛶'s pill opens on; ✓ untouched: no `Part:` line; pill off ✓: upright, `auto
  0.35`); K27 (⛶ opens at 0.35 = `subjectPosition`, 208/596 px), K28 (✓ untouched: nothing), K29 (drag ✓: `cropPosition`
  0.73, no `, auto`), K31 (undragged, pill on: the band at 0.21 = `subjectSideways`); K30/K33 (an undragged part, Size Centre then Auto and Set: Centre
  `on LOCK` without `auto`, Auto `auto 0.35`; the other half, a dragged part staying at the next Set, is under SKIPPED); K34, K34a–d (APOD faked
  1500×1022: sharp on and Set `sharp ×2.00 (Fill ×2.35)`; ⛶'s frame 36% of the picture's width, 2×, against 30.7% by
  arithmetic with it off, and by eye in the round-1 fix: `k34-on.png`'s frame 36% of the width (325 of 900 px), `k34-off.png`'s 31%
  (277 of 900), both opened and compared; drag ✓ `sharp ×2.00 (Fill ×2.35), darker ×0.98`; a 300×200
  picture: ⛶'s screenshot identical after a drag (no drag), "Whole picture" the picture 2× on the blurred strip).
  34 (setup J6) is removed by the map.
- **FAIL:** K18 and K31a (BUG (b): the same-session steps gave the expected `sideways=false`, no `, sideways`, `auto 0.35`, but a sideways-dragged position applies to the upright frame once ⛶ is reopened).
- **BUG:** (a) **The week log is lost after `pm clear`** (round 1: a FAIL-grade defect for 0.10.0's real-week log, not a DIFFERENT). Repro: run the app with the week log on so that `Download/APODroid/log.txt` has a MediaStore row, `pm clear` the app (the row stays, now owned by nobody the app can see), turn the log on again (`--ez weekLog true`): every write fails with `SQLiteConstraintException: UNIQUE constraint failed: files._data` (14 in 30 s), `log.txt` is never created, `log (1).txt` neither. LOGf1's lookup runs only for a file the app can see, so the insert collides. Details under DIFFERENT (1).
  (b) **A sideways-dragged position applies to the upright frame** (round 1: FP1's failure kind, `notedSideways` was to prevent it; by design only if the user says so). Repro: crop mode on a landscape APOD with the pill on, drag the band, ✓ (cropPosition 0.893, sideways true); open ⛶ again, turn the pill off, ✓: the upright frame is set at 0.893 (`on LOCK`, no `, auto`). Details under DIFFERENT (2).
- **DIFFERENT:** K1b and K12 (3), R1 and R2/R2a (4), P3a with larger mp4s (5), the picker's black frames (6), and:
  (1) **The week log after `pm clear`** (the app was cleared by E17a before): `Week log: on` then every
  write `Week log: couldn't write: SQLiteConstraintException: UNIQUE constraint failed: files._data` (14 times in 30 s),
  `Download/APODroid/` empty; the MediaStore row of E17a's `log.txt` is still there and the insert of `log.txt` collides
  with it instead of becoming `log (1).txt` (LOGf1's lookup runs only for a file the app can see). Off again at once. Not a
  page case; for the week-log package. (2) **K18/K31a's kind:** a position dragged sideways (cropPosition 0.893 with
  `sideways` true) stays when ⛶ is opened again and the pill turned off, and ✓ then sets the upright frame at 0.893
  (`on LOCK`, no `, auto`); the FP1 rule drops only the drag of that crop mode. A stored number "keeps it" per
  `CLAUDE.md`, so maybe as designed; a drag made sideways applying to the other axis later is what `notedSideways` was to
  prevent. (3) **K1b/K12 text:** ⛶ on Earth reopens at the subject (Auto's frame, `Subject: EARTH p=0.25`), not "the
  middle" (the map's 0.8.0 wording). (4) **R1/R2:** a pull always logs the three `Refresh: … same post / stored, nothing
  newer` lines (the map says no log; LG made them). (5) **P3a with a larger mp4:** `NoctilucentNeowise_Girotti.mp4`
  (4.8 MB) and `xz_and.mp4` (49 MB): `no frame` after `MediaHTTPConnection: readAt … => ProtocolException` (the server
  answers a Range request with 200 and the whole file, from the desktop too; E17 saw the same once); the 2.3 MB
  `eso2612b.mp4` made the still. As designed (tile black, logged). (6) The 17 picker's new engine drew black four times
  (~100 ms) after each `kill -9` rebind and after the picker (E17's DIFFERENT), seen again.
- **SKIPPED:** **S4/S4a** (a several-MB original saved and rotated "at once"): the original arrives in under a second on
  this host, ★ was filled before the new page came up; `Saved`, one row, no crash. **S2a's toast** not read (the star
  stayed `Saved` with the row deleted, as the map's SUSPECT; the next reopen shows ☆, so "Already saved" or a silent
  no-op not told apart). **K24's 3000×1000 galaxy file** (a faked text on the real 3709×2772 Jupiter instead). **R5 in
  landscape** (the pull on the pane ran in portrait only; landscape was left for time, so R5 is not done there: still open).
  **K30/K33's second half** (a dragged part staying at the next Set of Size Centre or Auto): not run, left for time, still open. **P3a's `am kill` of the second open** done as `kill -9`
  (Live's engine rebinds); setup's J6 gone.
- **End state:** as found: the app's own setup unchanged (daily on, More NASA on, home NASA, lock APOD, Size Auto, Keep
  pictures sharp off, Keep running off, Save every off 20 MB), the real APOD picture put back (the faked ones replaced
  by a download after removing `files/apod.jpg`), Earth's and APOD's parts removed, both gallery rows deleted (the media
  query empty), airplane mode off (ping works), `accelerometer_rotation` 1, `user_rotation` 0, density 420, font scale
  1.0, `auto_time` 1, the week log off, no crash. The wallpaper ids changed with every set (117/118 at the end), still
  static (no Live, as found). The emulator stays running; the app stays installed.

### [ ] E17c. Android 17, the background map (Sonnet, medium)

`interactions/background.md`, "Phone test list" (27 cases) with a stepped clock; real timing (Doze, buckets)
SKIPPED. Also `am kill` with Keep running off: no detector box (E17's Not run). Device `emulator-5566` (`apodroid-37`, Android 17), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5566` instead of 5564; the cases E17
ran (its Checked above) aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

**Checked** (rosie, pipeline run 168, 2026-11-22, `emulator-5566`, Android 17, dev's debug build 0.9.2, evidence in
`.coord/runs/168-e17c/tablet/`; ~35 min, the clock stepped forward only, set back to real time at the end):
**no crash** (`logcat -b crash` empty). Per case of `interactions/background.md`'s Phone test list:
- **PASS:** P1 (daily off: no alarm, job or service; `kill -9` once: Live kept; switch on: `the engine is on HOME, not
  noted live`, `Switch on: … on LOCK … shown live … on HOME`, job 1 at once, no `Detector: found gone`; no `nothing
  missed` line at job 1's run: that is logged only when a loss note is used up, so the map's wording is
  stale), P2, P3 (offline: `Daily job failed`, alarms +15 min, the box "NASA Image of the Day: no connection.",
  clock +15 min: `alarm, run scheduled`, job 2 pending, no APODroid alarm, Home/sleep-wake `Live: screen on, run
  owed`, airplane off: `job 2 starts, expedited`, alarms by the clock), P4 (second failure: start₂ + 1 h at start
  and end), P5 (kill: retry kept, no `Detector:` line; good run clears it), P6 (kill, `run -f 1` at 3 s and 33 s:
  `engine 1 created`, no `not live yet`/`Fill on`/`stray`, wallpaper id and component unchanged), P7, P8 (`p=0.33` at
  11:57), P9 (within 15 min, and after a 16 min step with nothing owed: no `run owed`), P10 (Keep running on:
  foreground, specialUse; `run -f 1` no `couldn't start`; off: service gone; off again at the end), P11, P12
  (periodic 6 h/6 h, persisted, CONNECTIVITY, no job 2 outside P3), P14 (the 35+ hint: battery line and "Unused app
  settings › "Manage app if unused": off", the app isn't in the idle whitelist), P15 (each `TIME_SET`/`BOOT_COMPLETED`
  re-armed at once for the same T), P17 (`force-stop`: `BOOT_COMPLETED, arming again` at the next start, `Detector:
  the alarms found gone`, job 1, `Live: home isn't live, Fill on HOME+LOCK`, `Detector: nothing missed`, no box),
  P18 (APOD mode, offline: `stored, check failed`, no `Daily job failed`, alarms the clock's next check), P19 (keys
  removed: `Subject: APOD … p=0.35 (sideways 0.21)`, once; a 4-byte `apod.jpg`: `couldn't: … Failed to create image
  decoder`, keys noted without positions), P20 (page open: the `Subject:` line), P21 (Auto: `auto 0.35`, Earth
  `auto 0.25`; the 0.55–0.65 leg not run), P22 (3000×1000 faked, galaxy text: `auto 0.58, sideways, auto turn`, the
  pixels turned, top at the right; horizon text: upright; Centre: upright), P26 (Earth 1440×960 on the lock screen:
  `auto 0.28, sharp ×2.00 (Fill ×2.50)`; `sharp` off: no ", sharp"), P27 (below), and `am kill` with Keep running
  off: the alarms survive, the clock stepped past 06:12: `job 2 … expedited`, no `Detector:` line, no box.
- **DIFFERENT:** P1's `Detector: nothing missed` (above); P24 (the page-open hook ran on APOD's 3709×2772 original:
  `rendition (original 3709×2772)`, `Subject:` after; the smaller-original replacement leg not run).
- **BUG:** the week's log writes nothing while a stale MediaStore row `Download/APODroid/log.txt` exists whose file is
  gone and which the app doesn't own (`_id=20`, size 27529, here left by an earlier install; `/sdcard/Download/APODroid/`
  empty): `find` doesn't see it, `insert` fails on every line with `SQLiteConstraintException: UNIQUE constraint
  failed: files._data` (`Week log: couldn't write`, 19 lines in 10 s, logcat only). Not the `log (1).txt` the plan
  expects. After the row was deleted (as shell) the file was made and filled. Likely `AppLog.kt`'s insert (line 88);
  rare (the row without its file), an insert that falls back to another name would cover it.
- **P27** (on this install, after the row's removal): `Week log: on`, a `Process start:` line at each new pid, the run's
  lines in order (the `Detector:` line before `Week log: on` is logcat's only); `--ez weekLog false`, a run: the file
  ends `Week log: off`; O6a (force-stop, `true`, `false`, Back, reopen from Recents, `run -f 1`): the file still ends
  `Week log: off`, logcat has the run. A fresh install and the reboot leg not run (E17 did: 8 `Process start` lines).
- **SKIPPED:** P16 (a real morning, real timing); the buckets (P14 read only).
- **Not run:** P13 (Save every picture is off here; it writes to the gallery); P23 and P25 (a made-up feed or a faked
  small original needs a debug build with another host; E17 saw the probe on all three hosts); P24's replacement leg;
  P26's second page-open check; P21's 0.55–0.65 leg; P22's ⛶-pill leg (the page map's).
- **Tester notes:** `run-as` edits made while the job's process is alive are written back over: `am kill` first. A
  faked title is replaced by the job's fetch (the job stores the real text): P22 runs offline. `adb root` makes the
  shell UID 0, which `content delete` refuses (`adb unroot` first).
- **Left as found:** the prefs file as at the start (`prefs-before.xml`), `apod.jpg` as at the start, Live off
  (the system's image wallpaper on both screens, as found), Keep running off, `weekLog` off, `auto_time` 1 and the
  clock real, airplane mode off, the stale log row removed.

### [ ] E17d. Android 17, the rest of the live map (Opus, medium)

`interactions/live.md`, section 5, the cases E17 didn't run: T02–T04, T10, T13–T14, T16–T30, T32, T35,
T37–T39. Device `emulator-5566` (`apodroid-37`, Android 17), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5566` instead of 5564; the cases E17
ran (its Checked above) aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

**Checked** (rosie, pipeline run 169, 2026-11-22, 12:08–12:33, `emulator-5566`, Android 17, dev's debug
build 0.9.2 installed with `install -r` over the data E17a–c left; evidence in `.coord/runs/169-e17d/tablet/`:
`log-all.txt` (every APODroid line), `cases.txt` (the lines per case), screenshots, prefs and `dumpsys
wallpaper` before and after): **every case of the list ran but T29 (skipped by the map's own rule); no FAIL,
no crash** (`logcat -b crash` empty, no `E AndroidRuntime`, one process, pid 23677, all along; `exit-info`
only older runs' records). The found state was the map's T0 with Size Auto and both stills; T0 was built
first (Size Live, the dialog "› Home screen", AOSP 17's picker "Home screen"; R2b: no Settings button for
APODroid in the preview). Dates: APOD 22 Nov, NASA 20 Nov; the clock reads 12 h. Toasts were not captured
(the screenshots fell before or after them): the log lines stand for them.
- **T02** PASS: no dialog, `Live: the engine is on HOME, not noted live`, `Set: wallpaper set: …APOD… on
  LOCK, …; wallpaper shown live: …NASA… on HOME`; home id 135 kept, lock a new still.
- **T03** PASS: ⛶ with Live, a drag moved nothing (no `cropPosition` noted); Whole › ✓ `Part: wallpaper shown
  live: … on HOME` (no `, darker`: the whole picture's grey under 100), `live.whole=true`, `Live: decoded
  1080×2400`; Whole off › ✓ `… on HOME, darker ×0.83`, `live.whole=false`, `decoded 3200×2400`.
- **T04** PASS: draft Auto, ⛶ opened by the stored Live: the drag left the screenshot byte-identical
  (`t04-crop-before-drag.png` = `…-after-drag.png`), ✓ logged nothing, no toast, no `cropPosition`; Size › Live
  again: Setup's title untinted (`t04-size-live-back.png`).
- **T10** PASS (from M-EQ, home live, lock a NASA still, made by T09's "Not now": `Set: Live on the lock
  screen not now`, one status line "Changed 22 Nov, 12:13 PM · NASA"): split back, Set: no dialog, lock APOD
  still (id 139), home id 135 kept.
- **T13** PASS (P2 made by T11's steps: `engine 1 destroyed`, `engine 3 created`, `… home live, shared`,
  `shown live: … on HOME+LOCK`): off: the lines gone, summary "NASA · both screens · Live", the engine stays
  (id 140, mWhich 3); on: `Switch on: wallpaper shown live: … on HOME+LOCK`, one line "Changed 22 Nov,
  12:14 PM · NASA", summary with "6:00 AM".
- **T14** PASS: pull: three `Refresh:` lines, nothing set, ids kept.
- **T16** PASS (AOSP 17's picker offers "Lock screen", started from outside the app with
  `CHANGE_LIVE_WALLPAPER`): `Live: engine 2 created`, lock id 142 the engine; at the page's return `Live: the
  engine is on the lock screen alone, Fill on LOCK`, `engine 2 destroyed`, `Page: wallpaper set: …APOD… on
  LOCK`; home id 140 kept, lock its own still.
- **T17** PASS: `Live: engine 1 destroyed`, `Set: … NASA … on HOME, auto 0.69, …, APOD … on LOCK, auto
  0.35`, both stills, summary "Home: NASA · Lock: APOD · 6:00 AM".
- **T18** PASS: APODroid picked by hand for home with Size Auto: `engine 1 created`, it drew `live.jpg`
  (NASA) after ~180 ms of `drew black (nothing)` (E17's DIFFERENT), no hand-over, no "Live isn't on"; Set:
  `engine 1 destroyed`, home a still again (id 147).
- **T19** PASS: the dialog "Next, in Android's preview: / Set wallpaper › Home screen", Cancel: `Set: the
  picker's dialog cancelled`, `Set: no pick, nothing saved`, size AUTO stored, Setup open.
- **T20** PASS: Set wallpaper › Back (the sheet) › Back: `back from the picker, nothing changed, home not
  live`, `no pick, nothing saved`; the preview drew NASA's `live.jpg` (`t20-preview.png`).
- **T21** PASS: Home, then the launcher icon brought the picker back on top; Home, Recents (APODroid's card),
  the picker again; Back: as T20; no `back from the picker` line while away.
- **T22** PASS (runnable here): "Lock screen" in the app's trip: `Live: back from the picker, the lock screen
  alone`, `Set: no pick, nothing saved`, `Live: the engine is on the lock screen alone, Fill on LOCK`, `Set:
  the lock screen put back: …APOD… on LOCK, auto 0.35`; size AUTO stored, Live highlighted in the draft
  (`t22-toast.png`, taken before the toast).
- **T23** PASS: as T16 with stored Auto: `Page: wallpaper set: …APOD… on LOCK, auto 0.35`; no "Live isn't on".
- **T24** PASS: off, on: `Switch on: wallpaper set: … on HOME, auto 0.69 …, … on LOCK, auto 0.35`, both
  stills, no dialog, no "Live isn't on".
- **T25** PASS, **DIFFERENT** in its rotation: AOSP 17's preview stays portrait (`user_rotation 1` with
  `accelerometer_rotation 0`: `mDisplayRotation=ROTATION_0` while the picker was on top and still after the
  return), so the rotation during the picker didn't happen; Home screen: `a wallpaper changed, home live`,
  lock APOD still, summary with " · Live", no "Live isn't on".
- **T26** PASS, **DIFFERENT** in the log: Wallpaper & style › On-device wallpapers › Apply on Home screen
  alone (id 157); at the page's return `Live: home isn't live, Fill on HOME+LOCK`, both screens set (ids 158,
  159; `WallpaperManagerService` `setWallpaper which = 1`, then `= 2`), status lines 12:23, "Live isn't on.
  Set as wallpaper turns it on." shown; but **no `Page: wallpaper set: …` line** (MainActivity.kt:798 logs it
  after the update). The page had just been relaunched (`AppCompatResourceOverlayPolicy … result: true`,
  "finishDrawing of relaunch" at the `am start`: the system's new wallpaper colours), so likely the load's
  coroutine ended with the destroyed page before its log line; not checked further. Outcome right, only the
  log line missing.
- **T27** PASS: "Home screen and lock screen" with stored different columns: `engine 3 created`, `… home live,
  shared`, `Live: engine flags 1`, home live, lock APOD still (id 161).
- **T28** PASS: the question › OK › Recents › swiped APODroid's task away (the picker had its own task); the
  process stayed (pid 23677); at the reopening `Live: back from the picker, nothing changed, home live`,
  `Set: no pick, nothing saved`; stored setup unchanged (home NASA, lock APOD, Live).
- **T29** SKIPPED: the map runs it only if T28 saved, and T28 saved nothing.
- **T30** PASS: the question › Recents › swipe: `Set: the picker's dialog cancelled`, `no pick, nothing saved`
  at the swipe; the reopened page: Setup closed, "Home: NASA · Lock: APOD · 6:00 AM · Live".
- **T32** PASS (A-L made by T31's steps: `Set: … APOD … on LOCK`, "APOD · lock screen · 6:00 AM"): Where ›
  Home screen, Set: no dialog, `Live: the engine is on HOME, not noted live`, `Set: wallpaper shown live:
  …APOD… on HOME, darker ×0.94`, lock id 162 kept.
- **T35** PASS, a shorter path: from T32 home was still live, so the Set back to M-DIFF opened no dialog
  (`opensPicker` false; the map's dialog follows T34's Auto, not run here): `… on LOCK; wallpaper shown live:
  …NASA… on HOME`, "Home: …NASA" / "Lock: …APOD"; New picture at 6, Change every DAY, order APOD,NASA,EARTH,
  Advanced (Save every picture, Keep running off) unchanged. `where` became HOME by T31–T32 (put back at the end).
- **T37** PASS, Q7's second outcome: equalise, the question › OK › "Home screen": `engine 1 destroyed`, `engine
  1 created`, a new home id (164), `a wallpaper changed, home live`, saved, lock a NASA still (id 165); split
  back and Set: no dialog, lock APOD still (id 166).
- **T38** PASS: Centre: no dialog, `engine 1 destroyed`, `Set: … NASA … on HOME, darker ×0.84, … APOD … on
  LOCK, darker ×0.87` without `, auto`, `size=CENTRE`; Live › the dialog "› Home screen" › OK › Home screen:
  back to T0 (home id 169 live, lock still).
- **T39** PASS (NASA's 20 Nov picture is landscape, 3200×2400 as Live decodes it, so no fake): Sideways › ✓
  `Part: wallpaper shown live: … on HOME, sideways, darker ×0.86`, `live.sideways=true`, `Live: decoded
  1800×2400` (W < H); off › ✓ no `, sideways`, `live.sideways=false`, `decoded 3200×2400`.
- **End state**: the found setup again (APOD mode's Where Both by a Set in APOD mode, then More NASA pictures,
  Size Auto, Set): the setup prefs equal to the start's (`size`, `allSources`, `where`, `home`, `lock`, `daily`,
  `order`, `pictureAt`, `interval`, `keepRunning`, `saveEvery`, `soon`, `darken`, `sharp`), both stills
  (ids 172, 173); the settings as found (rotation put back). Left: NASA 20 Nov's part now notes `whole=false`,
  `sideways=false` (was none; NASA's pictures never turn by Auto, so nothing changes), `live.jpg` NASA's.

### [ ] E17e. Android 17, the pixels and the rest (Sonnet, medium)

SH4's pixel checks (`sharp-0.9.0-sh4.md`, Testing 2; `scripts/sh4_check.py`, 1 px of tolerance at the
frame's edges, emulator.md's 17 row) on Centre, Auto, Whole, Sideways and the limited set, both screens;
the large picture (a faked post of a 37 MB PNG, `emulator.md`); `install -r` with Keep running on
(`MY_PACKAGE_REPLACED` arms, the service foreground again); setup S3 (the photo picker); M0's home/lock
sequence; edge-to-edge and landscape screenshots of the page with Setup open. Device `emulator-5566` (`apodroid-37`, Android 17), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5566` instead of 5564; the cases E17
ran (its Checked above) aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

**Checked** (rosie, pipeline run 170, 2026-11-22, 14:47–15:14, `emulator-5566`, Android 17, dev's debug build 0.9.2,
1080×2400 at 420 dpi; evidence in `.coord/runs/170-e17e/tablet/`: `sh4/` (results, PNGs), `sh4-run*.log`,
`sh4-recheck.txt`, `big2/`, `m0-*.txt`/`.png`, `s3-*.png`, `portrait-setup-open.png`, `landscape-setup-open.png`,
`alarm-after-install.txt`, `services-after-install.txt`, `exit-info-summary.txt`, before/after prefs and wallpaper):
**no FAIL, no crash** (`logcat -b crash` empty all along; `exit-info`: 15 SIGNALED = my `kill -9`s, one `kill
background`, no "MemoryLimiter", `am memory-limiter status` "disabled").
- **SH4's pixels** PASS, 159 of 159 cases (`scripts/sh4_run.py`, `sh4_check.py`, 1 px tolerance at the frame's
  edges, its pictures from `run 154`): Matrix A in full (14 pictures × Centre/Auto × switch off/on × Whole
  off/on, Both, the job; 112), Home and Lock alone, Set's path (Both, Home, Lock), the faked change time, dragged
  parts at 0 and 1, the pill both ways under Centre and Auto, the "up" text under Auto (47 more). 34 sideways
  sets (Auto's turn and the pill), 39 limited sets. Needed two changes in a **copy** of the driver (the repo's
  scripts untouched): its fake post's date 18 Nov lost to today's real post (22 Nov), so every date moved 5 days
  on (23 Nov, the turn path's stamps too); and its judge still held 0.9.0's "Centre when unsure" (the middle for a
  move under 0.2): 3 first-run FAILs (`huge_8000x6000` AUTO, `w32_900x600` AUTO Lock set, auto 0.32 and 0.44
  against 0.50) were that stale expectation, not the app (0.9.1 removed the fallback; judged again with Auto
  always the subject: 159 PASS). **DIFFERENT (script):** `scripts/sh4_run.py`'s judge needs that line changed
  (`position = raw`) and its DATE moved; not done here (no code changes).
- **The large picture** PASS: a made-up 39 MB PNG, 4455×5592 (the 2 Oct case's shape; gradient + noise), as
  APOD's stored picture: Centre job (setStream, crop hint `[970,1,3486,5592]`), Centre + Keep sharp job, Auto
  Whole + Keep sharp job and Auto Whole Set (7–15 s, `debug-set.png` read by pixels: R 0→254 across x, B 2→250
  over y 522–1878, blurred bands above and below: the whole picture right); each `Set: wallpaper set … HOME+LOCK`.
  The script's checker flagged the Whole cases (`the picture seen 540..543`): my picture's 2 random low bits
  against a gradient only 0.24 level per px; judged by the pixels by hand. No crash, no "MemoryLimiter" in
  `exit-info`, `am memory-limiter status` "disabled" on this image.
- **`install -r` with Keep running on** PASS: Keep running on (foreground, `types=0x40000000`, the process
  alive), `install -r` of dev's debug build: `Daily job: android.intent.action.MY_PACKAGE_REPLACED, arming
  again`, `alarms for 2026-11-23T05:01:22Z`, the pair of `RTC_WAKEUP` alarms (window +10 min and the
  `setAndAllowWhileIdle` one) in `dumpsys alarm`, a new process (pid 10707) with `KeepRunning` `isForeground=true`
  again, nothing set, no crash.
- **Setup S3 (P29, the photo picker)** PASS: "If APOD is a video" › "Use my picture" opened AOSP 17's
  PhotoPickerActivity; a pick copied `files/fallback-draft.jpg` (194,991 bytes); "Skip it": the dropdown "Skip
  it", Setup's title and the heading in the normal colours (`s3-skip-again.png`, the dropdown's highlight itself
  not compared by eye); Set: `Set: wallpaper set …`, `fallback.jpg` not there (the draft's pick not renamed);
  "Use my picture" again: no picker, "Change picture" beside it (the draft's pick, `s3-use-again.png`); "Skip it".
- **M0's home/lock sequence** PASS (`m0-*.txt`): Home alone from a shared wallpaper: system id 317
  `mSystemWasBoth=true`, lock id 316 `mWhich=2` (the old shared one moved to the lock screen, `wallpaper_lock*`
  files there); Lock alone: lock id 321 new, system id 320 kept; Both: one id 322 `mWhich=3`, no
  `wallpaper_lock` files, "Lock wallpaper state" empty. As M0 saw on 15; no screen blank (`m0-*.png` taken, not
  all looked at). (My first two rounds' taps missed "Lock screen" and "Both", the page scrolled: those Sets were
  Home again; the sequence above is the repeat.)
- **Edge-to-edge and landscape with Setup open** PASS (`portrait-setup-open.png`, `landscape-setup-open.png`):
  portrait: status bar and the navigation bar both drawn, the pinned "Set as wallpaper" above the navigation bar,
  last row above it; landscape (`user_rotation 1`, `ROTATION_90`, 2400×1080): two panes, the picture left, the
  settings right, the pinned Set bar inside the right pane clear of the navigation bar (on the right edge); the
  status bar's time at the left over the picture's pane.
- **End state:** the found setup (the setup prefs equal to the start's by a diff: `size` AUTO, `allSources`,
  `where`, `home`, `lock`, `daily`, `order`, `pictureAt`, `interval`, `keepRunning` off, `soon`, `darken` on,
  `sharp` off, `saveEvery`), both stills set again from the real 22 and 20 Nov posts (`Set: … NASA … HOME, auto
  0.69, darker ×0.82 …`), Keep running's service gone, rotation, font scale, density, `auto_time` as found,
  the test photo and its MediaStore row deleted, the draft file and `debug-*.png` removed. Not restored byte for
  byte: `files/apod.jpg` etc. are the real posts' again, the parts of NASA 20 Nov as E17d left them.
- **Not run:** Real timing; the pause hint's 17 App info labels (E17's); `exit-info` rows only after a `kill -9`
  here, no `am memory-limiter manual` kill during a run.

### [ ] E14. Android 14, every map (`apodroid-34`, `emulator-5564`; Sonnet, medium)

All four maps in full (0.8.0's T3 and 0.9.0's T4 and T5 ran subsets), E4's cases, the levels' row for
14: the first 14 build's lock-screen live wallpaper off, `WallpaperCropper`, TP1's ten force stops.

### [ ] E14a. Android 14, the setup map (Sonnet, medium)

`interactions/setup.md`, section 2, the phone test list in full, in its order. Device `emulator-5564` (`apodroid-34`, Android 14), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5564` instead of 5564; the cases of the E14 packages before it ran
aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

### [ ] E14b. Android 14, the page map (Sonnet, medium)

`interactions/page.md`, sections 3 (the prioritized list) and 4 (0.8.0's emulator cases), with K31b (B32). Device `emulator-5564` (`apodroid-34`, Android 14), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5564` instead of 5564; the cases of the E14 packages before it ran
aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

### [ ] E14c. Android 14, the background map (Sonnet, medium)

`interactions/background.md`, "Phone test list" with a stepped clock; real timing (Doze, buckets) SKIPPED;
`am kill` with Keep running off (no detector box); the week log over a stale `log.txt` row (B31: `pm clear`, the
file deleted beneath FUSE with `adb root`, `log (1).txt`). Device `emulator-5564` (`apodroid-34`, Android 14), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5564` instead of 5564; the cases of the E14 packages before it ran
aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

### [ ] E14d. Android 14, the live map, first half (Opus, medium)

`interactions/live.md`, section 5, T01–T20, and the 14 row's lock-screen live wallpaper (off on the first 14
build: what the picker offers, the lock screen's Fill). Device `emulator-5564` (`apodroid-34`, Android 14), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5564` instead of 5564; the cases of the E14 packages before it ran
aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

### [ ] E14e. Android 14, the live map, second half (Opus, medium)

`interactions/live.md`, section 5, T21–T39, and SPLIT's Q1–Q8. Device `emulator-5564` (`apodroid-34`, Android 14), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5564` instead of 5564; the cases of the E14 packages before it ran
aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

### [ ] E14f. Android 14, the pixels and the rest (Sonnet, medium)

SH4's pixel checks (`sharp-0.9.0-sh4.md`, Testing 2; `scripts/sh4_check.py` and `sh4_run.py` after B34) on Centre,
Auto, Whole, Sideways and the limited set, both screens; `WallpaperCropper`; TP1's ten force stops with the process
alive (the detector each time); the large picture (a faked 37 MB PNG); `install -r` with Keep running on; setup S3
(the photo picker); M0's home/lock sequence; edge-to-edge and landscape screenshots with Setup open. Device `emulator-5564` (`apodroid-34`, Android 14), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5564` instead of 5564; the cases of the E14 packages before it ran
aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

### [ ] E10. Android 10, every map (`apodroid-29`, `emulator-5554`; Sonnet, medium)

E1a and E1b with the 10 row of "The levels": the crop code's aspect fix (tall and wide pictures,
numbered rows), the decoder and EXIF (Auto's subject on an EXIF-6 and EXIF-8 photo), DocumentsUI,
scoped storage (★, LOG's file), the video still's `MediaMetadataRetriever`, Keep running in the shade
without `specialUse`, the plain job 2, Live's id path (`live.homeId`) and the lock screen's Fill,
the detector with no exit records. Every map that runs.

### [ ] E12. Android 12 (`apodroid-31`, `emulator-5558`; Sonnet, medium)

E2's cases and the 12 row of "The levels", then a smoke of each map (the cases the version table
says differ: U1's colour events after every set, B25's text, DocumentsUI, the expedited job 2 and the
fallback, the exit-record sign, Keep running's notification, Live's id path).

### [ ] E13. Android 13 (`apodroid-33`, `emulator-5562`; Sonnet, medium)

E3's cases and the 13 row, then a smoke of each map: the system photo picker, both hint lines, colour
events from the foreground app only, Keep running only in Active apps, the expedited job 2, Live's id
path.

### [ ] UP. The update from old versions (Sonnet, medium)

F-Droid users skip versions, so the migrations' last real check: each of **0.5.0, 0.7.0 and 0.9.1**
installed over fresh data on `apodroid-34` (`emulator-5564`) and again on `apodroid-37`
(`emulator-5566`; two nights), set up as a real user's (daily switch on, a setup that differs from
the defaults, 0.5.0: APOD and NASA; 0.7.0: Live on home and a changed order), run once, then the
candidate R8 build `install -r` without opening the page: `MY_PACKAGE_REPLACED` arming (the pair in
`dumpsys alarm`), the prefs by `cat` as root (what `CLAUDE.md`, Migrations, says each update does:
0.5.0 → 0.6.0 `migrateOnce`, 0.6.0 → 0.7.0 `foldRunGoing`, `Post.isNoted`, the old Size `FILL`, the
left pref keys), the wallpaper ids unchanged and nothing set, then the page opened (Setup as
before, no crash, the pause hint and the detector's box not wrongly shown), a Set, the job's `run -f`.
The old APKs: `app/build/apodroid-v0.5.0.apk`, `-v0.7.0.apk` exist; **0.9.1's must be built from its
commit and signed with the candidate's key beforehand** (the user: it isn't tagged yet), else
`install -r` refuses.

### [ ] B30. ⛶'s "Whole picture" preview let go in `onStop` (Sonnet, medium)

From CR (`review-0.10.0-cr.md`, finding 2). (B29, the CR's finding 1, shipped as 0.9.2.)
- **Steps:** open ⛶, "Whole picture" on, Home, Keep running on, `dumpsys meminfo io.github.buerlino.apodroid`
  after a while.
- **Happened:** the preview's screen-sized bitmap (about 10 MB) is held until the page resumes
  (`WholePreview`'s `produceState` in `MainActivity.kt` against `onStop`).
- **Should happen:** let go with the other pictures (O12, CLAUDE.md, The page); shown again at the resume.
- **Done when:** the fix, and a device bullet with the memory after Home, before and after.

**Checked** (rosie, pipeline run 165, fix round 1, 2026-11-22, `emulator-5566`, Android 17, debug; evidence in
`.coord/runs/165-b30/tablet-1/`): ⛶ open, "Whole picture" on, Keep running on: the native heap 29,652 KB with
the preview shown, 11,868 KB 45 s after Home (total PSS 101.6 → 80.0 MB, the page's other pictures let go too,
O12), the service alive; the resume reopened ⛶ with "✓ Whole picture" drawn again (29,692 KB), no crash; ✓ and a
Set afterwards set. Not run: a resume after the photo picker (no spinner flash), other levels.

### [ ] B31. The week log over a stale `log.txt` row (Opus, by hand, 2026-11-22)

From E17b and E17c (BUG (a)). After `pm clear` (or a reinstall) the old `log.txt`'s MediaStore row stays, owned by
nobody the app can see; LOGf1's lookup doesn't find it, the insert of `log.txt` hit `UNIQUE constraint failed:
files._data` on every line, and no file was written. **Fixed** (wp/b31, 3006f43): `:core`'s `WeekLogNames`
(`log.txt`, then `log (n).txt`, at most 10; `WeekLogTest`, 8 cases), `AppLog`'s write takes the next name on a
constraint failure and logs `Week log: log.txt held by an old row, writing log (1).txt`.
**Checked** (by hand, 2026-11-22, `emulator-5566`, Android 17, wp/b31's debug build): the stale row made as E17b
saw it (`pm clear`, the file deleted beneath FUSE as root: the row left, `owner_package_name` NULL); the log on:
`log (1).txt` written with `Week log: on`, the "held by an old row" line and `Process start:`; a force stop and a
start: the same file grew (two `Process start:` lines), no `log (2).txt`, no second "held" line.

### [ ] B32. A position dragged sideways applies to the upright frame (Opus, by hand, 2026-11-22)

From E17b (BUG (b), K18/K31a): a band dragged sideways (✓: `cropPosition` 0.893, `sideways` true), then ⛶ again,
the pill off, ✓: the upright frame was set at 0.893. **Fixed** (wp/b32, 4bdb07d): `:core`'s `Part.positionFor
(sideways)`: a position noted with an orientation applies only to it (none noted, a part from before 0.9.0: both);
`notedPosition`, ⛶'s frame (`frameOf`), ✓'s `keepPart` and `Store.set` read it; `PartTest` (the repro);
`interactions/page.md` K31, K31b.
**Checked** (by hand, 2026-11-22, `emulator-5566`, Android 17, wp/b32's debug build, today's landscape APOD,
Size Auto, subject 0.35, sideways 0.21): pill on, drag, ✓: `on HOME+LOCK, sideways`, `cropPosition` 1.0, `sideways`
true; ⛶, pill off, ✓: `on HOME+LOCK, auto 0.35`, no `cropPosition`, `sideways` false; ⛶, pill on, ✓ without a
drag: `auto 0.21, sideways`. No crash.

### [ ] B33. The page's load loses its log line when the system relaunches the page during it (Sonnet, low)

- **Steps:** E17d's T26: the system relaunches the page while its load runs.
- **Happened:** no `Page: wallpaper set` line; `lifecycleScope` cancels the rest of the run after `store.update`
  returns (the sets still happened).
- **Should happen:** the result is logged inside the IO block.
- **Done when:** the log line shows in the same case on the emulator, and the fix. Only the log line is lost.

### [ ] B34. `scripts/sh4_run.py` judges Auto with 0.9.0's Centre when unsure (Sonnet, low)

- **Happened:** the judge takes the middle for a move under 0.2 and fakes its post for 18 Nov, so every run reports
  false FAILs (E17e's 3 first-run FAILs).
- **Should happen:** the judge's Auto position is the noted subject as it is; DATE = today + 1.
- **Done when:** a rerun on the emulator with no false FAIL. Before E14f.

### [ ] CR2. A second code review, during the real week (Opus, high)

As CR (three reviewers by area, blind to each other, each finding verified by a second agent; correctness only),
on the tree the week runs. It runs alongside WK (user, 2026-11-22), so the PC works while the devices do. A
verified finding is a `B<n>` here; its fix goes into a new build on the devices and the week goes on (WK's rule).

### [ ] WK. The real week (the user; three devices, no connection to the PC)

The candidate's release-signed build installed on each, LOG turned on, the setup below applied,
then a week of real use with no adb. Each device covers a different part of the app, so together
they cover every feature that runs by itself:

| | FP6 (LineageOS, Android 16, the user's phone) | Tablet (Lenovo ZUI, Android 15) | Third (LineageOS, 14 or newer) |
|---|---|---|---|
| More NASA pictures | off (APOD mode) | on | on |
| Where / the list | Both | Home: APOD, NASA, Earth; Lock: APOD alone (columns differ) | Both: Earth, NASA, APOD (a changed order) |
| Size | Auto (Auto's turn, the subject) | Live (home live, the lock screen still: B28) | Centre |
| New picture at | 06:00 | 06:00 | 07:00 |
| As soon as it's out | on | off | off |
| Change every | — | 6 h | 12 h |
| If APOD is a video | Skip it | NASA's Image of the Day | Use my picture |
| Darken / Keep sharp | on / on | on / off | off / on |
| Save every picture | off | on, 20 MB | off |
| Keep running | off | on (ZUI's cleaner) | off |
| Battery | Unrestricted | Unrestricted | **Optimized** (the expedited run, Doze) |
| By hand once in the week | ⛶ a drag + ✓; the Sideways pill once | ★ once; pull to refresh | a force stop in Settings (the detector's box at the next page open) |

Each morning, a glance: the wallpaper changed, the status line's time. At the week's end the three
`log.txt` files are pulled (adb or a file manager), and an agent reads them against the rules
(every change time, every set, every failure and its retry); the logs are also read mid-week.
**Bugs during the week** (user, 2026-11-22): CR2 runs alongside. A `BUG:` from a device, the logs or CR2 → a
`B<n>` fix, a new release-signed build on the devices (the setup kept), and the week goes on. **1.0.0 (R6)
when** the week has run at least 7 days and the last 2 days on the current build had no `BUG:`.

## Decided (user, 2026-11-22, on this file)

1. LOG's file in `Download/APODroid/log.txt` (a file manager can copy it; no adb needed at the week's end).
2. The FP6 keeps the user's own setup (the table's column).
3. E12 and E13: the version table's rows and a smoke of each map; 17, 14 and 10 every case.
4. **The nights:** the pipeline drives one device per night (`APODROID_TABLET`), so the first night
   runs LOG, A17, ER, SIM and CR (`emulator-5564` for LOG's check); ER writes E17, E14, E10, E12, E13
   and UP as `### [ ]` sections here, with each one's serial, and their queue rows go in after ER's
   merge, one emulator level per night (`APODROID_TABLET=emulator-<port>`), 17 first.

5. **One package per map** (2026-11-22, user: "split and run now"): E17 ran a third of the maps (the
   rows "For E17, first"), since one device run has 50 min (night.md, rosie's rules) and "every map" is
   well over 100 cases. The rest as E17a–e (setup, page, background, the live map's rest, the pixels
   and the rest). E14 and E10 are split the same way (a–e) when their nights come; E12 and E13 stay
   one package each (a smoke and the version rows).
6. **The real week** (2026-11-22, user): the setup per device is WK's table (one Auto, one Live, one Centre;
   "looks good"). CR2, the second code review, runs during the week; bugs are fixed and the week goes on; 1.0.0
   after at least 7 days with the last 2 on the current build bug-free. E14 is split six ways (a–f: the live map
   in two halves); E10 the same when its night comes.