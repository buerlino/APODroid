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

### [x] E14a. Android 14, the setup map (Sonnet, medium)

`interactions/setup.md`, section 2, the phone test list in full, in its order. Device `emulator-5564` (`apodroid-34`, Android 14), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5564` instead of 5564; the cases of the E14 packages before it ran
aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

**Checked** (rosie, pipeline run 173, 2026-11-22, `emulator-5564`, Android 14 (`apodroid-34`), dev's debug build
at 8b05841; evidence in `.coord/runs/173-e14a/tablet/`; the app was not installed at the start, a fresh install, so
the map's baseline B0 (daily on, More NASA pictures on, home NASA / lock APOD, Size Live by Android's picker, 6:00,
Skip it, Darken on) was built first from the page, which also ran P30's picker path once). The dates are 22 Nov
(APOD 22 Nov, NASA and Earth 20 Nov), the clock 12 h ("6:00 AM"). **No crash** (`logcat -b crash` empty, no
`E AndroidRuntime`; `exit-info`: force stops and kills only, by my force stops). **No FAIL, no `BUG:`.**
Toasts were never read (they aren't in the log; the `Set:` lines, the status lines and the ids stand for them).
- **PASS:** P0 (baseline, Earth's dropdown E0 "Both", W0 "Both", no "my picture": the picker opened empty, Cancel
  kept "Skip it"), P1 (`Set: … NASA … on HOME, auto 0.69 …, APOD … on LOCK, auto 0.35`, home ImageWallpaper, no
  " · Live"), P2, P3 (`NTS`, no `Set:` line, ids unchanged, Setup closed), P4 (`NTS`; after the Set, mode off showed
  Where "Home screen" highlighted with the mode switch and the Setup title tinted: a stray tap of mine spoiled the first read, so the
  Where draft was set again and read again), P5 (dialog survived a rotation, "Every 24 h from 6:00 AM, each screen
  switches to its next source in the list; a new order starts with the top one at 6:00 AM."), P6, P7 (`NASA on
  HOME, APOD on LOCK`, summary "Home: NASA, Earth · Lock: APOD, Earth · every 24 h from 6:00 AM"), P8 (the alarms
  `alarm_cancelled`, no job, the rows gone, Setup title not tinted), P9 (`EARTH … on HOME+LOCK, auto 0.25, darker
  ×0.70`, chip "On wallpaper"), P10 (`Daily job: alarms for 2026-11-23T05:04:12Z`, `Switch on: …`, "Changed 22 Nov,
  4:55 PM · Earth", New picture at 6:00 AM; Set greyed during the run not seen: the run took under a second), P11,
  P12, P13 (`APOD on LOCK` only, "Lock: 22 Nov, 4:57 PM"), P14, P15, P16 (lock has an id of its own), P17 (NASA's
  "Both" highlighted after mode on), P18 (retaken: my first pick of "Home screen" missed and a Set saved "Both";
  the second `NASA on HOME, APOD on LOCK`), P19, P20 (one `MainActivity` record), P22 (the draft was probably Darken
  off before the Recents swipe: not confirmed), P23, P24 (rotation at once; Setup closed on the rotated page, no
  `darker`), P25, P26 (alarm `16:19:12Z` = 17:19 local), P27, P28 (done in the landscape two-pane layout: in portrait the pause
  hint above the switch moves it between 1660 and 2283, the lower under the navigation bar; four taps: on, one
  `Switch on:` set, alarms and job 1 there), P29 (no picker the second time, `fallback-draft.jpg` kept, no
  `fallback.jpg` ever written), P30 (the log lines of the map, home LiveWallpaper, lock ImageWallpaper; but the
  stored Where was "Home screen" since P15/P16 saved it with the mode off, so mode off shows W0 "Both" highlighted),
  P31 (no "Wide pictures sideways", no "Centre when unsure" under any Size, "Keep pictures sharp" under Centre and
  Auto only), P32 and P34e and P33's logs (by `scripts/sh4_run.py`, `sh4/`, `sh4b/`: 4:1 galaxy Auto `, auto 1.00,
  sideways, auto turn`; horizon no `sideways`; 1500×1022 `, sharp ×2.00 (Fill ×2.35)`, Centre without `auto`, off
  without `sharp`; Centre with the pill on `, sideways` without `auto turn`; pill off upright; the pixel checks PASS), P34c
  (portrait, font scale 2.0 and landscape: the bar pinned above the navigation bar or inside the right pane, the last row
  above it), P34d (dialog "A small picture is enlarged at most 2×; the rest of the screen shows it blurred.", on
  survives a rotation, gone under Live, off again under Auto), P35, P36 (Centre no `auto`, Auto `auto 0.69` / `0.35`,
  `debug-set.png` differs).
- **DIFFERENT:** P21 (Back, then the launcher icon: the draft was dropped, Setup closed and Darken on again; the map
  expects Android 16's kept draft and says to record it).
- **SKIPPED / not run:** P34a (the update from 0.8.0's build: no such build here), P33's ⛶ steps ("✓ Sideways" shown, the pill
  off, ✓, `Part:` line: the pill's states ran by prefs only), P34 (NASA's 3000×1000 picture: no such post; NASA's real sets
  were upright), P34b (removed in 0.9.1), P2's and P10's toasts, real timing.
- **State at the end:** `pm clear` (the app was not installed at the start, so a fresh install is the found state),
  test photo and its MediaStore row deleted, `accelerometer_rotation` 1, `user_rotation` 0, `font_scale` null (deleted),
  `auto_time` 1, network on, no job or alarm of the app. The system wallpaper is ImageWallpaper (the last stills of the
  run were not restored byte for byte).

### [x] E14b. Android 14, the page map (Sonnet, medium)

`interactions/page.md`, sections 3 (the prioritized list) and 4 (0.8.0's emulator cases), with K31b (B32). Device `emulator-5564` (`apodroid-34`, Android 14), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5564` instead of 5564; the cases of the E14 packages before it ran
aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

**Checked** (rosie, pipeline run 174, 2026-11-22, `emulator-5564`, Android 14 (`apodroid-34`), dev's debug build; evidence in
`.coord/runs/174-e14b/tablet/`; the app was installed but empty (E14a's `pm clear`), so the map's S0 was built from the page
first: daily on, More NASA pictures on, home NASA, lock APOD, Earth unticked, Size Live by Android's picker (AOSP's: "Home screen"
shared one engine, lock an ImageWallpaper)). The dates: APOD 22 Nov, NASA and Earth 20 Nov, clock 12 h. **No crash**
(`logcat -b crash` empty; `exit-info`: force stops and SIGKILLs from my `kill -9` only). **No FAIL, no `BUG:`.** Toasts were
never read (the log, prefs and dumps stand for them).
- **PASS:** P-baseline, T1, L1, H1, W1–W3 (end stops), T2/T2a/T2c, C1–C3, C5, C6/C7 (the long presses ran, the clipboard
  wasn't read), C8 (explanation and credit stay open in landscape), O1 (two panes), A1, A2, O3 (the ⓘ dialog survived the
  rotation), P1 (the WebView browser opened, Back: APOD), S1a (one `EO_` row), S1b (no second row), O4 (☆ after Home and reopen),
  K1/K1a/K1b (the dragged frame was not compared to the reopened one by pixels: `cropPosition` absent after Back), K10, K11, K1c
  (`earth.cropPosition` 0.138, no log), K2 (lock id 92→93, `Part:` lines), K12, K3, K6 (no `nasa.cropPosition`), K7 (`Part: wallpaper
  shown live`, engine `p=0.50`, ids unchanged), K13, K8 (`p=0.65` again), R1, S3 (`Original failed, saving the stored picture:
  UnknownHostException`, the row written), R2/R2a (`Update failed`, each page its own text), R3, BH0–BH3 (App info and
  Battery optimisation open and Back; "Hide this hint?" Cancel keeps it), A3/A3a/A3b (`saveEvery`, `saveLimit` 50 then 20, off),
  A4, A5 (`KeepRunning` foreground, then gone; Active apps not looked at), D1 (alarms and job 1 gone, rows gone), D1a (the pull
  logged `Refresh:` only), D2 (`Switch on:`, job 1 ran, `alarms for 2026-11-23T05:05:26Z`), O5 (opens on NASA), P3a (`Video
  still: 1859 ms, 1080×722` with `comet_plunge.mp4`, the dimmed still behind ▶; after a `kill -9` no second line), P3c (offline:
  `Video still: couldn't make one of …: setDataSource failed`, after about 25 s; `videoStillTried` kept), P3d (black, no line),
  K27 (⛶ at 0.35, left of the middle), K28 (✓ untouched: nothing), K29 (`cropPosition` 0.748, `Part: … on LOCK` without `, auto`,
  `sideways=false` noted), K31 (the pill on an upright-stored part moved the frame), K31a (pill on, drag, pill off, ✓: no
  `cropPosition`, `sideways=false`, no set; pill on drag ✓ `, sideways`, `cropPosition` 0.0; pill off drag ✓ noted, no `, auto`),
  **K31b** (band ✓'d, ⛶ again, pill off, ✓: `Part: wallpaper set: … on LOCK, auto 0.35`, no `, sideways`, no `cropPosition`,
  `sideways=false`), K30/K33 (Whole ✓ without a drag: no `cropPosition`; Whole off: `, auto 0.35`; Size Centre and Set:
  no `auto`), K16/K17/K18 (the pill, the band and ✓ `, sideways` ran inside K31a/K31b on the real APOD 4:3 picture).
- **DIFFERENT:** R1 (the pull logs `Refresh: … same post` / `stored, nothing newer`, the map says "no log": LG's lines, as in
  E14a), S2a (the map's SUSPECT: after the row was deleted by adb ★ stayed filled, a tap saved nothing, Home and reopen: ☆),
  BP2 (Dismiss on APOD, Earth, then NASA: APOD's page shows NASA's box, the next candidate not dismissed, as CLAUDE.md says; each
  source's own Dismiss held).
- **Not run:** S4/S4a (no original of several MB here), P2 (a browser exists), O2/R5 (a pull on each pane in landscape; left for time, not run),
  K24, K24a–c and K34, K34a–d (faked 3000×1000 and 1500×1022 pictures: E14a ran them with `sh4_run.py`, P32, P33, P34e), K16b,
  K26 (a part from before 0.9.0), K27's "Centre keeps the middle" beyond K30's Set, real timing. P3e (the next post), P3b.
- **State at the end:** same setup as built (home NASA, lock APOD, Live on home (id 90 shared engine), `size` LIVE, daily on, Keep
  running off, Save every off, 20 MB), gallery row of S3 deleted, `accelerometer_rotation` 1, `user_rotation` 0, `auto_time` 1,
  airplane mode off, Wi-Fi on. Not restored: the NASA and APOD parts (`apod.cropPosition` 0.729, `sideways` false).


### [x] E14c. Android 14, the background map (Sonnet, medium)

`interactions/background.md`, "Phone test list" with a stepped clock; real timing (Doze, buckets) SKIPPED;
`am kill` with Keep running off (no detector box); the week log over a stale `log.txt` row (B31: `pm clear`, the
file deleted beneath FUSE with `adb root`, `log (1).txt`). Device `emulator-5564` (`apodroid-34`, Android 14), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5564` instead of 5564; the cases of the E14 packages before it ran
aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

**Checked** (rosie, pipeline run 176, 2026-11-22, `emulator-5564`, Android 14 (`apodroid-34`), dev's debug build; evidence in
`.coord/runs/176-e14c/tablet/`; start: home NASA, lock APOD, Live on home (id 109), daily on, Keep running off, 6:00, E14b's end state; the
clock stepped once (+15 min, `auto_time 0`, then back to 1), airplane mode on and off). **No crash** (`logcat -b crash` empty).
- **BUG:** after `am force-stop` (P7's page open, then P17's case) Android 14 turns home into the ImageWallpaper but keeps its wallpaper id
  (`dumpsys wallpaper`: id 109, component `ImageWallpaper`, was `LiveWallpaper`), so `homeLive` (the noted `live.homeId` equals the id) still says
  Live is on: `run -f 1` logged no `Live: home isn't live, Fill on HOME+LOCK`, no "Live isn't on. Set as wallpaper turns it on." line
  (`p17-no-live-line.png`), the record stayed noted live, and Set with Size Live offered no picker (the page believed Live was on; I
  went through Size Centre and Set to get a picker). Expected (P17): Fill on both, the status line. Likely `Store.kt`'s `liveState`/`homeLive`
  (L8f1's id rule); the id alone doesn't tell a force stop's revert on 14. Detector lines were right: `the alarms found gone` by the exit record.
- **PASS:** P1 (daily off: no alarms, no job, no service; `kill -9`: Live kept, no `alarms for` line while off; on: `Switch on: wallpaper
  set: … LOCK; wallpaper shown live: … HOME`, `alarms for`, job 1 ran, no `Detector:` line after it (J6)), P2 (`not expedited`, alarms
  by the clock, no set), P3 (offline: `Daily job failed`, retry alarm at +15 min by a stepped clock (`TIME_SET, arming again`),
  `alarm, run scheduled`, job 2 pending with CONNECTIVITY, `Live: screen on, run owed`, airplane off: `job 2 starts, expedited`, no
  failure), P4 (second failure: `alarms for` start+1 h), P5 (`kill -9`, page open: `alarms for` the kept retry, no `Detector:` line;
  a good run afterwards, `failedRuns` gone), P6 (`kill -9`, runs at 4 s and 34 s: new pid, `engine 3 created`, `decoded`, `drew … p=0.79`, ids and
  component unchanged; ~70 ms of black at the start, accepted), P7, P8 (`drew p=0.77` about once a minute, p matches the clock), P9
  (a wake within 15 min: only `drew`), P10 (`isForeground=true`, no `couldn't start`, off: service gone), P11 (status lines "Home: …
  · NASA", "Lock: … · APOD", chip "On home screen"), P12 (job 1 periodic, CONNECTIVITY; job 2 only in P3), P13 (Save every on: one
  `saved: APOD 2026-11-22 original, NASA 2026-11-20 original`, nothing on the second run; the rows deleted, switch off), P15 (no
  `arming again` by itself except my own `TIME_SET`), P19 (a fresh install: `Subject: APOD 2026-11-22 p=0.35 (sideways 0.21) in 298 ms`,
  none at the next process start), P27 (fresh: no `log.txt`; on: `Week log: on` and `Process start: APODroid 0.9.2, Android 14 …`; the run,
  a `kill -9` with a new pid's `Process start` and the engine's lines in order; `weekLog false`: last line `Week log: off`, a later run not in the
  file), **B31** (`pm clear`, `log.txt` removed beneath FUSE with `adb root`: `Week log: log.txt held by an old row, writing log (1).txt`, the lines in
  `log (1).txt`, the next process start wrote the same file, no `log (2).txt`), the `am kill` case (it kills nothing while the wallpaper binds
  the process, pid unchanged; with `kill -9` and Keep running off no `missed`/box keys, no detector box).
- **DIFFERENT:** P3's orange box not seen (E14b's Dismiss on NASA's `network` problem the same day still held, `nasa.problemNoticeKey`; the
  problem itself was stored), P18 (APOD's `stored, check failed: UnknownHostException` and no download ran, but NASA is also in use so
  the run failed as the map says for a stored-day check only with APOD alone), P5's last step (the airplane toggle took 8 s).
- **SKIPPED / not run:** P14 (the pause hint's lines read only as page text), P16 (real timing), O6a's Recents reopen (the card tap
  didn't reopen the page; the file did end with `Week log: off` and a later run wrote nothing), P18's APOD-alone half (would need
  Setup changes and a new Live picker), P20–P26 (faked posts and fake feeds; E14a ran P32–P34 with `sh4_run.py`), a reboot in P27,
  Doze and buckets (real timing).
- **State at the end:** the setup of the start again (home NASA, lock APOD, Live on home, id 116, lock 117, `size` LIVE, daily on, Keep running off, Save every
  off); to get there I did `pm clear` (B31), restored the prefs file from the start (minus the live notes and `weekLog`) and chose "Home screen" in the picker;
  `auto_time` 1, airplane off, Wi-Fi on; gallery test rows deleted. Left: `Download/APODroid/log (1).txt` (its MediaStore row is the app's).


### [x] E14d. Android 14, the live map, first half (Opus, medium)

`interactions/live.md`, section 5, T01–T20, and the 14 row's lock-screen live wallpaper (off on the first 14
build: what the picker offers, the lock screen's Fill). Device `emulator-5564` (`apodroid-34`, Android 14), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5564` instead of 5564; the cases of the E14 packages before it ran
aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

**Checked** (rosie, pipeline run 177, 2026-11-22, 20:04–20:17, `emulator-5564`, Android 14 (`apodroid-34`), dev's debug build,
the APK the prompt named already installed (same sha256); evidence in `.coord/runs/177-e14d/tablet/` (`log-all.txt` is C1
for the whole run). The start state was T0 as E14b left it: More NASA pictures on, NASA "Home screen", APOD "Lock screen",
Earth unticked, Size Live, Live on home (id 116, NASA 20 Nov), lock an APOD 22 Nov still (own id), daily on; New picture at
6:00 AM, no "Change every" (one source per column), order APOD, NASA, Earth, Save every picture and Keep running off. One
process (pid 27789) the whole run. **No crash** (`logcat -b crash` empty; `exit-info`'s newest rows are an earlier run's).
**No FAIL, no `BUG:`.** Toasts were not read (not in the log; the `Set:` lines, the ids and the prefs stand for them). The
emulator has no keyguard (lock screen "None"), so the lock screen was looked at once with `locksettings set-disabled false`,
set back to true.
- **PASS:** T01 (summary "Home: NASA · Lock: APOD · 6:00 AM · Live", two status lines, no "Live isn't on"; C2 home
  APODroid, lock its own id; `run-as` works), T02 (no dialog; `Live: the engine is on HOME, not noted live`, `Set: wallpaper
  set: …APOD… on LOCK, darker ×0.87 …; wallpaper shown live: …NASA… on HOME`, lock id 117→118), T03 (the frame didn't move
  on a drag, screenshots byte-identical; Whole ✓ `Part: wallpaper shown live: … on HOME`, `live.whole=true`, engine
  `decoded 1080×2400`, `p=0.50`; off ✓ the same line with `darker ×0.83`, `live.whole=false`), T04 (draft Auto: ⛶ by the
  stored Live, no drag, ✓ no log; back to Live: the Setup title not tinted), T05 (the question's text exactly as the map,
  "Not now" and "OK", no Cancel; Back: `Set: the picker's dialog cancelled`, `Set: no pick, nothing saved`, Setup open, the
  title and the APOD and NASA rows tinted, nothing saved), T06, T07 (the question after a rotation, the rotation back, Home
  and the launcher icon, Home and Recents; Back at the end: the two lines), T08 (`Live: back from the picker, nothing
  changed, home live`, `Set: no pick, nothing saved`, nothing saved), T09 (each: `Set: Live on the lock screen not now`,
  lock a NASA still with its own id 119, then 120; summary "NASA · both screens · 6:00 AM · Live", one line "Changed …
  · NASA"; the second Set asked again), T10 (no dialog, lock an APOD still, id 121), T11 (`Live: back from the picker, a
  wallpaper changed, home live, shared`, `Set: wallpaper shown live: … on HOME+LOCK`; C2 one wallpaper id 122 `mWhich=3`,
  the lock block gone; `lock.wallpaperLive` and `live.shared` true; one status line), T12 (no dialog, `shown live: … on
  HOME+LOCK`), T13 (off: the status lines gone, the engine stays; on: `Switch on: wallpaper shown live: … on HOME+LOCK`,
  job 1 ran), T14 (the pull logs `Refresh:` lines only, nothing set), T17 (`Live: engine 3 destroyed`, `Set: … NASA … on
  HOME, auto 0.69 …, APOD … on LOCK, auto 0.35 …`, both ImageWallpaper, summary without " · Live"), T18 (Android's Live
  Wallpapers › APODroid › Home screen: the preview and the new engine drew `live.jpg`, `Of(source=NASA, …) p=0.79 ×0.83`;
  back in the app no set, no "Live isn't on", the status lines unchanged; Set put home's still back, `auto 0.69`), T19
  (dialog "Next, in Android's preview:\nSet wallpaper › Home screen", Cancel and OK; Cancel: the two lines, Size AUTO kept),
  T20 (the sheet's Back, then the preview's Back: `Live: back from the picker, nothing changed, home not live`, `Set: no
  pick, nothing saved`; the preview drew `live.jpg`, NASA p=0.79).
- **The 14 row, the lock-screen live wallpaper** (`mIsLockscreenLiveWallpaperEnabled=false` throughout): AOSP's picker
  offers "Home screen" and "Home screen and lock screen", never "Lock screen" (`t11-sheet.png`); "Home screen and lock
  screen" gives one wallpaper on both (`mWhich=3`, no lock block); with the columns apart the lock screen gets its own still
  (`FLAG_LOCK`, "Lock wallpaper state" with its own id and `mWallpaperComponent=null`), and the keyguard shows it: APOD's
  Jupiter as Fill (`lock14-split.png`). No `Live: engine flags` line all run (the engine logged `engine 3 created`).
- **DIFFERENT:** T15 (split back and Set: no dialog, lock an APOD still id 123, `live.shared=false`, as expected, but no
  `Live: engine flags 1` line: this build gives the engine no flags change, as the 14 row says). The picker's preview
  engine drew black for about 0.2 s before its first picture (`drew black (nothing)`, T08, T18), as L9a accepts.
- **SKIPPED:** T16 (AOSP 14's picker has no "Lock screen" answer, as the list allows); the toasts.
- **State at the end:** T0 again (`Set: … APOD … on LOCK; wallpaper shown live: … NASA … on HOME`, home id 129
  APODroid, lock id 130 its own still; summary "Home: NASA · Lock: APOD · 6:00 AM · Live", no "Live isn't on"); the prefs
  the same as at the start but for ids, stamps and times (`prefs-before.xml`, `prefs-end.xml`); rotation, density, font
  scale, `auto_time`, the zone, the network and `locksettings` as found. A leftover `livepicker` task from an earlier run
  stays in Recents (not touched).

### [x] E14e. Android 14, the live map, second half (Opus, medium)

`interactions/live.md`, section 5, T21–T39, and SPLIT's Q1–Q8. Device `emulator-5564` (`apodroid-34`, Android 14), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5564` instead of 5564; the cases of the E14 packages before it ran
aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

**Checked** (rosie, pipeline run 178, 2026-11-22, 20:26–20:45, `emulator-5564`, Android 14 (`apodroid-34`), dev's debug build,
the APK the prompt named already installed (same sha256); evidence in `.coord/runs/178-e14e/tablet/` (`log-all.txt` is C1
for the whole run). The start state was T0 as E14d left it (home id 129 APODroid's engine, NASA 20 Nov; lock id 130 an APOD
22 Nov still; summary "Home: NASA · Lock: APOD · 6:00 AM · Live"). One process (pid 27789) the whole run. **No crash** of
the app (`logcat -b crash` names none; one crash of `uiautomator`'s own dump, pid 3866; `exit-info`'s newest rows are
E14d's). **One FAIL** (T21). Toasts were not read (the `Set:` lines, the ids and the prefs stand for them). The Q cases E14d
ran (Q1, Q2 as T09; Q4 as T05–T07; Q5 as T11; Q6 as T08) weren't run again. T21 started from P0 Auto, draft Live (Size
Auto, Set; two extra Sets of the same setup ran by mis-taps on the way, the same stills set again).
- **FAIL, T21** (L1, L2, L7). Set › OK › the preview › Home › the launcher icon: the picker doesn't come back; the icon
  stacks a second page over it in the same task (Hist: MainActivity, LiveWallpaperChange, MainActivity, as CLAUDE.md says
  for a start over another app's screen), and that page's `onResume` ends the picker's wait at once, while the picker is
  still open under it: `Live: back from the picker, nothing changed, home not live`, `Set: no pick, nothing saved`. Home ›
  Recents › APODroid › Back then finished the second page and showed the preview again; Back from it: the first page,
  nothing more logged, Size AUTO stored (`t21-*.png`). **Then a pick is lost** (the same steps, then Set wallpaper › Home
  screen in the preview that comes back): APODroid's engine on home (id 135, `Live: engine 3 created`) with nothing saved
  and nothing handed over: stored Size AUTO, the engine drawing the old `live.jpg`, no toast, no line after the pick, the
  status lines unchanged (`t21b-after-pick.png`); X4's state (S6) until the next still set (here the next Set).
  BUG: a page started over the picker (the launcher icon while the picker is open) ends Set's wait for the picker in its
  `onResume`, so a pick made after returning to the picker isn't saved: home live, Size not saved, no hand-over, no toast
  (MainActivity's `onResume` fallback for the picker's return).
- **PASS:** T24 (stored Auto: off removed the status lines; on: `Switch on: wallpaper set: … NASA … on HOME, auto 0.69 …,
  APOD … on LOCK …`, both ImageWallpaper, no dialog, no "Live isn't on"), T25 (dialog "…› Home screen", OK, rotated to
  landscape with the preview open (the preview stayed portrait), Home screen, rotated back: `Live: back from the picker, a
  wallpaper changed, home live`, `Live: the engine is on HOME, not noted live`, `Set: … APOD … on LOCK; wallpaper shown
  live: … NASA … on HOME`; home id 143 APODroid, lock 144 own; Setup closed, summary " · Live", no "Live isn't on"), T26
  (Android's Wallpapers app (`com.android.wallpaperpicker`, via `SET_WALLPAPER`) › its one built-in still › Set wallpaper ›
  Home screen: `Live: engine 3 destroyed`, `Live: home isn't live, Fill on HOME+LOCK`, `Page: wallpaper set: … NASA … on
  HOME …, APOD … on LOCK`, "Live isn't on. Set as wallpaper turns it on."; the launcher showed NASA's still (`t26-home.png`);
  home's id stayed 146 after the page's set, the lock screen's became 147), T28 (equalised; the question; OK; Recents ›
  swiped APODroid's task, the picker in it: the process alive (27789), nothing logged at the swipe; reopened after 10 s:
  `Live: back from the picker, nothing changed, home live`, `Set: no pick, nothing saved`, the stored home NASA, lock APOD,
  summary with " · Live"), T29 not needed (T28 saved nothing), T30 (equalised; the question; Recents › swipe: `Set: the
  picker's dialog cancelled`, `Set: no pick, nothing saved` at the swipe; reopened: Setup closed, the stored setup), T31
  (APOD mode, Where Lock screen: no dialog, `Set: wallpaper set: … APOD … on LOCK`, lock id 151, home's engine id 148 kept;
  summary "APOD · lock screen · 6:00 AM", no " · Live", no "Live isn't on"), T32 (Where Home screen: no dialog, `Live: the
  engine is on HOME, not noted live`, `Set: wallpaper shown live: … APOD … on HOME`, lock 151 unchanged, summary "APOD ·
  home screen · 6:00 AM · Live"), T33 (Q3, Q5 in A-B: the question "Live on the lock screen too?…"; Not now: `Set: Live on
  the lock screen not now`, `Set: … APOD … on LOCK …; wallpaper shown live: … APOD … on HOME`, lock id 152, one line
  "Changed 22 Nov, 8:39 PM", summary "APOD · both screens · 6:00 AM · Live"; OK › Home screen and lock screen: `Live: back
  from the picker, a wallpaper changed, home live, shared`, `Live: the engine is on HOME+LOCK, not noted live`, `Set:
  wallpaper shown live: … APOD … on HOME+LOCK`, one wallpaper id 153 `mWhich=3`, no lock block, `lock.wallpaperLive` and
  `live.shared` true), T34 (Where Home screen, Size Auto: `Live: engine 3 destroyed`, `Set: wallpaper set: … APOD … on
  HOME+LOCK, auto 0.35, darker ×0.80 …`, one shared ImageWallpaper id 154, no engine), T35 (More NASA pictures on: the list
  NASA Home screen, APOD Lock screen; Live; dialog "…› Home screen" › OK › Home screen: home 155 APODroid, lock 156 own, the
  status lines Home/Lock; the prefs as at the start but for `where`, HOME since T31–T34, put back to BOTH below), T37 (Q7:
  equalised; the question › OK › Home screen: **home's id changed** (155 → 157): `Live: engine 3 destroyed`/`created`, `…
  a wallpaper changed, home live`, `Set: … NASA … on LOCK …; wallpaper shown live: … NASA … on HOME`, lock a NASA still of
  its own (158), one line "Changed … · NASA"; split back and Set: no dialog, lock an APOD still (159)), T38 (Centre: no
  dialog, `Live: engine 3 destroyed`, `Set: wallpaper set: … NASA … on HOME, darker ×0.84 …, APOD … on LOCK, darker ×0.87
  …` without `, auto`, `size=CENTRE`; then APOD mode Where Both, Set (to put `where` back), then More NASA pictures on,
  Live, the dialog › OK › Home screen: T0), T39 (A5s, NASA's 3200×2400 picture, landscape, no fake needed: ⛶ › Sideways ›
  ✓: `Part: wallpaper shown live: … on HOME, sideways, darker ×0.86 …`, `live.sideways=true`, `Live: decoded 1800×2400`;
  off › ✓: no `, sideways`, `live.sideways=false`, `decoded 3200×2400`).
- **DIFFERENT:** T27 (`Live: back from the picker, a wallpaper changed, home live, shared`, home 148 live, lock its own APOD
  still 149 as expected, but no `Live: engine flags 1`: as E14d's T15, this build gives the engine no flags change).
- **SKIPPED:** T22, T23, T36 and Q8 (AOSP 14's live picker offers "Home screen" and "Home screen and lock screen" only, as
  E14d saw; the Wallpapers app's "Lock screen" is for its stills); the toasts.
- **State at the end:** T0 again (home id 163 APODroid's engine, NASA; lock id 164 an APOD still; summary "Home: NASA ·
  Lock: APOD · 6:00 AM · Live", no "Live isn't on"); the prefs as at the start but for ids, stamps and times and one new key,
  `nasa.sideways=false` (T39's ✓ off noted it; NASA's pictures never turn by Auto's turn, so nothing changes; left);
  rotation (`accelerometer_rotation` 1, `user_rotation` 0), density, `auto_time`, the zone and the network as found
  (`prefs-before.xml`, `prefs-end.xml`, `wallpaper-*.txt`).

### [x] E14f. Android 14, the pixels and the rest (Sonnet, medium)

SH4's pixel checks (`sharp-0.9.0-sh4.md`, Testing 2; `scripts/sh4_check.py` and `sh4_run.py` after B34) on Centre,
Auto, Whole, Sideways and the limited set, both screens; `WallpaperCropper`; TP1's ten force stops with the process
alive (the detector each time); the large picture (a faked 37 MB PNG); `install -r` with Keep running on; setup S3
(the photo picker); M0's home/lock sequence; edge-to-edge and landscape screenshots with Setup open. Device `emulator-5564` (`apodroid-34`, Android 14), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5564` instead of 5564; the cases of the E14 packages before it ran
aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

**Checked** (rosie, pipeline run 179, 2026-11-22, `emulator-5564`, Android 14 (`apodroid-34`), dev's debug build 0.9.2 (the APK of
the run, `install -r` once); evidence in `.coord/runs/179-e14f/tablet/`: `sh4a/`, `sh4b/`, `big/` (results, PNGs), `tp1.log`,
`install-log.txt`, `alarm-after-install.txt`, `svc-*.txt`, `s3-*.png`, `m0b-*`, `portrait-setup-open.png`,
`landscape-setup-open.png`, `backup/`). **No crash** (`logcat -b crash` empty, `exit-info` no CRASH). **One FAIL-by-script, judged
by hand PASS** (the large picture, below). No `BUG:`.
- **SH4's pixels** PASS, 151 of 151 (`scripts/sh4_run.py`, `sh4_check.py` as in the repo, 1 px tolerance; Matrix A in full: 14 pictures ×
  Centre/Auto × switch off/on × Whole off/on, Both, the job, 112; then the 39 more: Home and Lock alone, Set's path, the faked change
  time, dragged parts at 0 and 1, the pill both ways, the "up" text). Sideways, limited (`sharp ×2.00`), Whole and Centre/Auto
  sets all matched the expected layout and the set's log note. **`WallpaperCropper`:** the `setStream` cases' crop hints equal Fill's
  frame, the bitmap cases (no upscale of a small crop) read back by the grid at the expected scale; no deviation seen.
- **TP1's ten force stops** PASS (process alive, page open, Home, `am force-stop`, page open, no clock step): 10 of 10
  `Detector: stopped by the user since the arming (exit record)` and `Detector: the alarms found gone`, each with `reason 10 [FORCE STOP]`
  in the exit records (`subreason=21 (FORCE STOP)` in `exit-info`), then `alarms for 2026-11-23T05:05:26Z` and `nothing missed` (no
  change was due, so no box; the box after a missed change was not run again: E14-before/T4 ran it).
- **The large picture** PASS: a made-up 45.6 MB PNG, 4455×5592 (gradient + 4 bits of noise; the 2 Oct case's shape), APOD's stored picture:
  Centre job, Centre + Keep sharp job (both PASS by the script), Auto Whole + Keep sharp by the job and by Set (the script's
  checker FAILed these two: my noise against a gradient of 0.057 level per px, as in E17e; read by hand: R 7→247 across x, B 5→250
  over y 522–1878, blurred bands above and below, `debug-set.png` 1080×2400). Each `Set: wallpaper set … HOME+LOCK`, no crash.
- **`install -r` with Keep running on** PASS (the pref set by sed after a force stop: the page start made `KeepRunning` foreground,
  `types=40000000`): `install -r` → `MY_PACKAGE_REPLACED, arming again`, `alarms for 2026-11-23T05:05:26Z`, the pair of `RTC_WAKEUP` alarms
  (window +10 m and the `setAndAllowWhileIdle` one) in `dumpsys alarm`, a new process (20848) with `KeepRunning` `isForeground=true`
  again, nothing set.
- **Setup S3 (the photo picker)** PASS: "If APOD is a video" › "Use my picture" opened AOSP's PhotoPickerActivity; the pick copied
  `files/fallback-draft.jpg` (157,321 bytes = the test photo); "Skip it" and Set: `Set: wallpaper set … HOME+LOCK`, no `fallback.jpg`;
  "Use my picture" again: no picker, "Change picture" shown; back to "Skip it".
- **M0's home/lock sequence** PASS (`m0b-*.txt`): from one shared wallpaper, Home alone: system id 326 `mWhich=1`, lock id 325 `mWhich=2`
  (the old shared one moved to the lock screen); Lock alone: lock id 327 new, system 326 kept; Both: one id 328 `mWhich=3`, "Lock
  wallpaper state" empty. Set lines `on HOME`, `on LOCK`, `on HOME+LOCK`. (A first round's taps missed the Where segment and set Both
  three times; `m0-*`, not counted.)
- **Edge-to-edge and landscape with Setup open** PASS: portrait `portrait-setup-open.png` (status and navigation bars drawn, the pinned bar
  above the navigation bar); landscape (`user_rotation 1`, 2400×1080) two panes, the picture left, the settings right, the Set bar pinned
  inside the right pane clear of the navigation bar on the right edge (looked at).
- **Not run:** the box after a missed change (stepped clock; ran in T4/TP1f1), the toasts, real timing.
- **State at the end:** the prefs and `apod.jpg`, `nasa.jpg`, `live.jpg` put back from the start's copy (`size` LIVE, `allSources`,
  home NASA, lock APOD, `daily`, `keepRunning` off); the test photo, `fallback-draft.jpg` and `debug-*.png` removed; rotation, `auto_time`
  as found; the app force-stopped (its alarms arm at the next start). **Not restored: Live on home** (it needs Android's picker; home and lock
  are one ImageWallpaper, id 328, the setup says Size Live, so the page shows "Live isn't on" until a Set picks it).

### [x] E10. Android 10, every map (`apodroid-29`, `emulator-5554`; Sonnet, medium)

E1a and E1b with the 10 row of "The levels": the crop code's aspect fix (tall and wide pictures,
numbered rows), the decoder and EXIF (Auto's subject on an EXIF-6 and EXIF-8 photo), DocumentsUI,
scoped storage (★, LOG's file), the video still's `MediaMetadataRetriever`, Keep running in the shade
without `specialUse`, the plain job 2, Live's bound engine (B39: below 14 the id isn't read) and the lock screen's Fill,
the detector with no exit records. Every map that runs.

### [x] E10a. Android 10, the setup map (Sonnet, medium)

`interactions/setup.md`, section 2, the phone test list in full, in its order. Device `emulator-5554` (`apodroid-29`, Android 10), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5554` instead of 5564; the cases of the E10 packages before it ran
aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

**Checked** (rosie, pipeline run 181, 2026-11-23, 00:57–01:35, `emulator-5554`, Android 10 (`apodroid-29`, QSR1.210820.001),
dev's debug build 0.9.2 (the run's APK, `install -r` once); evidence in `.coord/runs/181-e10a/tablet/` (screenshots `p<n>-*.png`,
`sh4/` results and PNGs, `backup/`, wallpaper dumps). The app was not installed at the start (a fresh install), so the map's baseline B0
(daily on, More NASA pictures on, home NASA / lock APOD, Size Live by AOSP's picker, 6:00 AM, Skip it, Darken on) was built from the
page first (it also ran P30's picker path once: dialog, AOSP's "Set wallpaper" › "Home screen", the log lines of the map). Dates: APOD
22 Nov, NASA and Earth 20 Nov, 12 h clock. No crash (`logcat -b crash` empty; Android 10 has no `exit-info`). Toasts were never read
(not in the log: the `Set:` lines, the status lines and the wallpaper ids stand for them).
- **BUG:** Keep pictures sharp's limited set draws the frame wrongly on Android 10 when the frame is a crop of a wide picture. Steps:
  Setup Size Centre or Auto, "Keep pictures sharp" on, a 3:2 APOD (made-up `w32_1500x1022` or `w32_900x600`, Fill ×2.35 / ×4) set by Set
  (`scripts/sh4_run.py` copy, path `set`). Expected: `, sharp ×2.00 (Fill ×2.35)` and `files/debug-set.png` with the frame at twice the
  file's scale. Got: the set is logged as expected, but `debug-set.png` is white except a strip about 120 px wide at the left edge (the
  grid there at a period of 12–19 px, not 32), blurred bands above and below; `sh4_check` FAIL (scale −1.30 / −2.02, the picture seen at
  540..541). The same pictures set without the switch (Auto and Centre) PASS, portrait `p23_400x600` with the switch (Fill ×4) PASS, and the
  1764×1200 picture didn't bite (no `sharp` in the line). The same cases PASS on Android 14 (E14a, E14f), so the suspect is 10's `ImageDecoder`
  with `setTargetSize` and then a crop (`Drawing.kt`'s `limitedPicture`, "the `limitedLayout`'s frame decoded alone, upright, straight to
  its dest size"); the made-up pictures are PNG. Not narrowed further (time).
- **PASS:** P0 (baseline; Earth's greyed dropdown "Both" (E0), stored Where "Both" (W0), no "my picture": AOSP's DocumentsUI opened empty,
  Back kept "Skip it"; Keep running off), P1 (`NASA on HOME, auto 0.69, APOD on LOCK, auto 0.35`, no dialog, home ImageWallpaper, no " · Live"),
  P2, P3 (no `Set:` line, ids unchanged, Setup closed; the handles and the Setup title tertiary, pixel-checked), P4 (Setup title untinted
  after mode on; no `Set:` line; mode off showed Where "Home screen" highlighted), P5 (the dialog survived a rotation; ⓘ "Every 24 h from
  6:00 AM, each screen switches to its next source in the list; a new order starts with the top one at 6:00 AM."), P6 (6 h back to 24 h),
  P7 (summary "Home: NASA, Earth · Lock: APOD, Earth · every 24 h from 6:00 AM"), P8 (rows gone, no alarm, no job, Setup title untinted,
  summary "Home: NASA, Earth · Lock: APOD, Earth"), P9 (`EARTH … on HOME+LOCK, auto 0.25`, one shared wallpaper, no alarms line, chip "On
  wallpaper"), P10 (`Switch on: … EARTH … on HOME+LOCK`, "Changed 23 Nov, 1:06 AM · Earth", alarms 05:07:59Z; the tap on Set during the run did
  nothing: the run took under a second, so "Set greyed" wasn't seen), P11, P12 (NASA's tick unchanged, APOD's dropdown greyed; the video
  dropdown's greyed state not inspected), P13 (`APOD on LOCK` only, home id unchanged), P14, P15 (`APOD on HOME+LOCK`, no chip), P16 (lock got
  its own id), P17 (NASA "Both" shown after mode on; Setup untinted before the Set), P18, P19 (Recents card and reopened page: Darken off tinted,
  Setup tinted), P20 (one `MainActivity` record, draft kept), P22, P24 (`Set:` without `darker`; Setup closed on the rotated page), P25, P26
  (alarm 01:22:59 = the next HH:15 + offset), P27 (Setup untinted after "Skip it" again), P28 (four quick taps: one `Switch on:` set, six
  alarm lines, job 1), P29 (DocumentsUI, a pushed JPEG in Downloads picked: `fallback-draft.jpg` only, `fallback.jpg` never written; "Use my
  picture" again: no picker), P30 (as the map; but the stored Where was "Home screen" since P15/P16 saved it with the mode off, so mode off
  shows W0 "Both" highlighted, as on Android 14), P31 (no "Wide pictures sideways", no "Centre when unsure" under any Size; "Keep pictures
  sharp" under Centre and Auto, gone under Live), P32 and P33 and P34e's logs by `sh4_run.py`'s Set path (4:1 galaxy Auto `, auto 1.00,
  sideways, auto turn`; horizon text no `sideways`; 900×600 galaxy Centre upright, Auto `, auto 0.25, sideways, auto turn`, pill off upright,
  Centre with the pill on `, sideways` without `auto turn`; 1500×1022 Auto without the switch `auto 1.00`, no `sharp`; the pixel checks PASS),
  P34c (font scale 2.0: the bar pinned above the navigation bar, the last row above it; landscape two panes as P34d's screenshots; closed:
  the button after the Setup row), P34d (dialog "A small picture is enlarged at most 2×; the rest of the screen shows it blurred."; on:
  tertiary, kept after a rotation; gone under Live, off again under Auto), P35 (three segments, none wrapping, portrait and landscape),
  P36 (Auto `auto 0.69` / `0.35`, `mCropHint` home 1463..2543; Centre no `auto`, 1060..2140; Auto again as the first).
- **FAIL:** P34e (the limited set's pixels, the BUG above); its Centre and off steps PASS (Centre without `auto`, off without `sharp`).
- **DIFFERENT:** P21 (Back, then the launcher: the draft was dropped, Setup closed, Darken on; as the map expects for 14 and 17).
  P23 (the picker is in APODroid's own task on Android 10: `am start -n` with DocumentsUI on top only brought the task forward ("intent
  delivered to the top-most instance"), no second page appeared, so the second-page steps couldn't run; Back from the picker returned
  to the first page with Darken off highlighted and "Skip it").
- **SKIPPED / not run:** P34a (the update from 0.8.0: no such build here), P33's ⛶ steps ("✓ Sideways" shown, the pill tapped off, ✓, the
  `Part:` line: the pill's states ran by prefs only), P34 (NASA's 3000×1000 post: no such post), P34b (removed in 0.9.1), P32/P33/P34e by the
  job path (see the next line), P10's Set-greyed and the toasts, real timing. **The job path of `sh4_run.py` doesn't run here as written:**
  on 10 `run-as … kill -9` does nothing and `am force-stop` deletes job 1, and with the page started first the run set nothing
  ("no set logged"); so P32, P33 and P34e ran by Set's path instead (the same drawing code; the job's own set paths ran in P1–P30).
- **State at the end:** `pm clear` (the app was not installed at the start), the test photo and its MediaStore row deleted,
  `accelerometer_rotation` 1, `user_rotation` 0, `font_scale` deleted (null), `auto_time` 1, network on, no alarm or job of the app;
  `screen_off_timeout` untouched. The system wallpaper is an ImageWallpaper (the last still of the run).

### [x] E10b. Android 10, the page map (Sonnet, medium)

`interactions/page.md`, sections 3 (the prioritized list) and 4 (0.8.0's emulator cases), with K31b (B32); on 10:
★ through `MediaStore` (scoped storage, S4a), the video still's `MediaMetadataRetriever` (a faked video day with a real
NASA mp4 as `<source>.videoUrl`: `Video still: <ms> ms`, else logged and black), no pause hint line (nothing to name). Device `emulator-5554` (`apodroid-29`, Android 10), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5554` instead of 5564; the cases of the E10 packages before it ran
aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

**Checked** (rosie, pipeline run 183, 2026-11-23, 09:11–09:36 by the emulator's clock, `emulator-5554`, Android 10 (`apodroid-29`), dev's debug build 0.9.2;
evidence in `.coord/runs/183-e10b/tablet/`: screenshots, `backup/` (prefs and `dumpsys wallpaper` of the built S0). The app was installed and
never started, so the map's S0 was built from the page: daily on, More NASA pictures on, home NASA, lock APOD, Earth unticked, Size Live by AOSP's
picker (dialog, "Set wallpaper" › "Home screen"; engine on home, id 114, the lock screen a still). Dates: APOD 23 Nov (portrait 2160×2700), NASA 20 Nov
(4200×3150), Earth 23 Nov (1440×960), 12 h clock. **No crash** (`logcat -b crash` empty). **No FAIL, no `BUG:`.** Toasts were never read (log, prefs and
dumps stand for them). Android 10 has no `exit-info`; `run-as … kill -9` does nothing there, so the prefs edits (faked video day) used `adb root`'s `kill -9`
twice, 12 s apart, as SKILL.md says for Live.
- **PASS:** P-baseline, T1, L1, H1 (no chip on Earth, not in use), W1–W3 (end stops), T2/T2a/T2c, C1–C3, C5 (explanation 2 lines → all → 2, by the
  explanation and by the title block; still open after a swipe away and back), C6/C7 (long presses ran, clipboard not read, no toast check), O1 (two panes
  in landscape), A1, A2 (Advanced open in landscape, closed ⓘ "Saves each new picture to Pictures/APODroid: …"), O3 (the ⓘ dialog stayed open across
  the rotation), P1 (the WebView browser opened, Back: APOD, `Daily job: alarms for 2026-11-24T05:05:20Z`), **S1a** (a double tap on Earth's ☆: one row
  `EO_2026-11-23_…jpg` in `Pictures/APODroid/` through `MediaStore`, scoped storage, no permission), **S1b** (no second row), O4 (☆ after Home and reopen),
  K1/K1a/K1b (Back kept no `cropPosition`), K10, K11 (title → APOD closed crop mode), K1c (`earth.cropPosition` 0.009, no log), K2 (lock id 116→117,
  `Part: wallpaper set: … on LOCK`), K12 (crop mode closed by the rotation), K3 (lock id 118), K6 (✓ without change: no `Part:` line), K7 (`Part: wallpaper
  shown live … on HOME`, engine `p=0.50`, ids 114/118 unchanged), K13 (⛶ open after Home and back, Back closes it), K8 (`darker ×0.83`, `p=0.18`
  again), R1, **S3** offline (`Original failed, saving the stored picture: UnknownHostException`, the row written), R2/R2a ("Earth Observatory: no connection.",
  `Update failed`, APOD's page "Astronomy Picture of the Day: no connection."), R3 (no box after Home and reopen, network back, ping works), BH0–BH3
  (**no pause hint on Android 10**: nothing to name, as the plan says), A3/A3a/A3b (`saveEvery`, `saveLimit` 50, 20, off), A4 (ⓘ dialog), A5 (Keep running on:
  `KeepRunning` foreground, notification channel `keep` importance 2 posted, so it shows in the shade on 10 without `specialUse`; off: gone), D1 (alarms,
  job 1, service and the rows gone, no log), D1a (the pull logged `Refresh:` only), D2 (`Switch on:` set, `Daily job: job 1 starts` … `jobFinished (job 1)`,
  alarms for 2026-11-24, Live still on home, id 114), **P3a** (a faked video day with `videoUrl` = `…/september/NoctilucentNeowise_Girotti.mp4`, 4.8 MB:
  `Video still: 486 ms, 864×1080`, the dimmed still behind ▶ on APOD's tile, `files/video-still.jpg` 71 KB, `videoStill.date/url` noted; after a kill no second
  line, none at the next open either), **P3c** (offline on a fresh fake: `Video still: couldn't make one of …: java.lang.RuntimeException: setDataSource failed:
  status = 0x80000000`, after about 30 s; `videoStillTried` kept, no new try at the next open), **P3d** (`videoUrl` removed: black tile, no line; a pull restored
  `isVideo` false), K27 (⛶ on NASA opened at 0.69 = `subjectPosition`, Size Auto), K28 (✓ untouched: no `Part:` line), K29 (drag ✓: `nasa.cropPosition`
  0.175, `Part: … on HOME` without `, auto`, `sideways=false` noted), K31a (pill on, drag, pill off, ✓: the stored 0.175 kept, no set), K16/K17/K18 (pill on, band
  dragged down, ✓: `Part: … on HOME, sideways, darker ×0.88 (grey 113)`, `sideways=true`, `cropPosition` 0.0), **K31b** (the band ✓'d, ⛶ again, pill off, ✓:
  `Part: wallpaper set: … on HOME, auto 0.69`, no `, sideways`, no `cropPosition`, `sideways=false`: B32 holds on 10), K30/K33 (Whole ✓ without a drag: no
  `cropPosition`; Whole off: `, auto 0.69`; Size Centre and Set: no `auto`, `mCropHint` 1060..2140 = the middle), K34 (the ⛶ frame on Earth's 1440×960 picture with
  "Keep pictures sharp" on is wider than with it off, 270 → 338 of 900 px on the screenshots `k34-off.png`/`k34-on.png`).
- **DIFFERENT:** R1 (the pull logs `Refresh: … same post` / `stored, nothing newer`, the map says "no log": LG's lines, as on 14), S2a (as on 14: after the
  row was deleted by adb ★ stayed filled and a tap saved nothing, no row; after Home and reopen ☆), BP1/BP2 (Dismiss on APOD, Earth, NASA: APOD's page shows
  NASA's box, the next candidate not dismissed, as CLAUDE.md says; each source's own Dismiss held, no box after the last), S4/S4a (a 14.5 MB NASA original saved in
  under 2 s on the emulator's network, so the "Still saving" window wasn't there; the ★ was filled after a rotation at once, row `NASA_…jpg` 14,472,481 B),
  P3a with the first mp4 tried (`…/september/xz_and.mp4`, 49 MB): `Video still: couldn't make one of …: java.io.IOException: no frame` (logged, tile black,
  as the package says); the second file gave a frame, so this one is the file or Android 10's decoder, not the code path (not narrowed).
- **Not run / SKIPPED:** O2/R5 (a pull on each pane in landscape), C8's expanded explanation in landscape (the left pane's node wasn't found in the dump; the
  credit stayed whole), A2's "Advanced stays open" after the rotation back to portrait (it was open in landscape and closed in portrait, not read again), P2 (a
  browser exists), S4's "Still saving" toast, K16b (left for time, not run), K24 and K24a–c (Auto's turn on a faked 3000×1000: E10a ran them as P32/P33), K26 (no part from before 0.9.0),
  K34a–d beyond the frame (the faked 1500×1022 and 300×200 pictures, the drag ✓ `, sharp ×2.00`, the Whole preview at 2×: E10a's P34e BUG (Keep pictures sharp's limited set
  white on 10) was found before this run and B37 (merged into dev, 48ca5b7's parent) is its fix, so the bug is fixed in code but not yet re-tested on 10 here
  (B37's own device check re-ran P34e on 10, PASS, Opus pass 66); Earth's real
  1440×960 file is on no screen here, so no set bit), P3b and P3e (left for time, not run), real timing.
- **Not done (round 1):** O2/R5's landscape pulls, K16b, P3b and P3e were left for time, not for an Android 10 reason; the package is therefore not done and
  stays unticked. A later pass runs these four and K34a–d once B37 is re-tested on 10.
- **State at the end:** same setup as built (home NASA, lock APOD, Size Live by the picker again (ids 131/132, engine on home), daily on, More NASA pictures on, "Keep
  pictures sharp" off, Keep running off, Save every off, 20 MB), gallery rows of S1a, S3 and S4 deleted, `accelerometer_rotation` 1, `user_rotation` 0,
  `auto_time` 1, `font_scale` 1.0 (as found), airplane mode off, network checked, `debug-*.png` and the video fake removed, Earth's stored part removed;
  `screen_off_timeout` untouched (2147483647 as found). The app is installed (it was at the start) and not force-stopped.

### [x] E10c. Android 10, the background map (Sonnet, medium)

`interactions/background.md`, "Phone test list" with a stepped clock; real timing (Doze, buckets) SKIPPED;
`am kill` with Keep running off (no detector box); the week log over a stale `log.txt` row (B31). On 10: job 2 a plain
job ("not expedited"), Keep running's notification in the shade without `specialUse` (the switch starts it without a
crash, `isForeground`), the detector with no exit records (only the `PendingIntent` sign after `am force-stop`); P17
also records whether a force stop keeps home's wallpaper id (`dumpsys wallpaper` before and after; B35: below 14 the id
is Live's only sign). Device `emulator-5554` (`apodroid-29`, Android 10), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5554` instead of 5564; the cases of the E10 packages before it ran
aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

**Checked** (rosie, pipeline run 184, 2026-11-23, 09:41–09:58 by the emulator's clock (the clock was stepped forward twice and set back), `emulator-5554`,
Android 10 (`apodroid-29`), the installed debug build 0.9.2 (E10b's; no `install -r`); evidence in `.coord/runs/184-e10c/tablet/` (`p<n>*.log`, `backup/`
with the start's prefs and dumps, `wallpaper-p17-*.txt`, `p17-intents.txt`, screenshots). Start state as E10b left it (home NASA, lock APOD, Size Live by the
picker, daily on, More NASA pictures on, Keep running off, `weekLog` unset). No crash (`logcat -b crash` empty). The `p<n>` ids are the list's cases.
- **BUG:** B35 holds on Android 10: a force stop keeps home's wallpaper id and the app still believes Live is on. Steps: Live on home (id 131), `am force-stop`
  (Keep running on, then off, three times: ids 131, 134, 135 stayed), `dumpsys wallpaper`: home is now `com.android.systemui.ImageWallpaper` with the same id;
  the page opens, job 1 runs: no `Live: home isn't live, Fill on HOME+LOCK`, no "Live isn't on. Set as wallpaper turns it on." line, Setup's summary still ends
  " · Live", and a Set (`Set: wallpaper set: … on LOCK; wallpaper shown live: Of(source=NASA …) on HOME`, logged after `Live: the engine is on HOME, not noted
  live`) handed home's picture to an engine that isn't there: home shows AOSP's default. Expected (P17): Fill on both, the status line. Likely `Store.liveOn` /
  `:core`'s `homeLive(named, id, noted)` (`id == noted` below 14 and, as the package says, on 14: B35's fix is open, `night.md` row 103). Restore needed the
  picker each time (done).
- **BUG:** the week log over a stale row (B31) writes nothing on Android 10. Steps: `--ez weekLog true`, a run, `--ez weekLog false`; `rm` the file and set the row's
  `owner_package_name` to another package (`content update … --bind owner_package_name:s:com.example.old`: a row an old install left); `kill -9` (a new process),
  weekLog on. Got: the process start line is written to logcat only; MediaProvider logs `UNIQUE constraint failed: files._data` for each `log.txt` insert (14 times in
  a minute, the same on each line) and no `log (1).txt` is made, no `Week log: log.txt held by an old row …`, no `couldn't write`. Cause by reading: on Android 10
  MediaProvider catches the constraint error and `ContentResolver.insert` returns null instead of throwing, and `WeekLogNames.make` returns null at once for a null
  (`WeekLog.kt`: "Null when [insert] returns null"), so only a thrown `SQLiteConstraintException` (Android 17's, where B31 was found) goes on to the next name;
  `AppLog.make` also stops trying after a null unless the name was the tenth. Expected: `log (1).txt`. The stale-row case with the app's own row (the file `rm`'d
  only) wrote `log.txt` again through the same row, no error.
- **PASS:** P0 (baseline: pair of alarms 06:05:20 at the offset 320 s, job 1 PERIODIC +6h flex +6h PERSISTED CONNECTIVITY, Live on home id 131, lock its own id 132,
  Keep running off), P2 (`job 1 starts`, `alarms for start+15m`, same post / nothing newer, `jobFinished`, `alarms for 2026-11-24T05:05:20Z` = `dumpsys alarm`; job
  1 still periodic), P3 (airplane on, `Refresh: APOD … stored, check failed: UnknownHostException`, `Daily job failed`, alarms start+15 min twice; the retry alarm
  by a clock step of +15 min: `alarm, run scheduled`, job 2 pending with CONNECTIVITY unsatisfied, **no** APODroid alarm, the second alarm `alarm, a run is
  pending`; airplane off: `job 2 starts` within 5 s, no failure, `alarms for` +1 h at its start (it was the second failed run in a row), the clock's next check at its
  end), P4 (two forced failures: the second `alarms for start+1h`), P5 (`kill -9` as root with Keep running off: a new pid, `alarms for` the retry kept (`09:58:40Z`, not
  tomorrow's), no `Detector:` line; airplane off, a forced run: alarms by the clock again), P6's equivalent on 10 (engine `home created`, `decoded`, `drew … p=0.22 ×0.83`,
  id 131 and the component unchanged, no `not live yet`, no `Fill on`), P7 (page open: `alarms for 2026-11-24T05:05:20Z`, the pair in `dumpsys alarm`), P8 (`drew
  Of(source=NASA …) p=0.22` at 10:00 = (10:00 − 06:00) ÷ 18 h, lock still APOD), P9 (Home within 15 min of a run: no `screen on, run owed`), P10 (Keep running on:
  `KeepRunning` `isForeground=true foregroundId=1`, channel `keep`, `NotificationRecord` importance 2 posted, so it is in the shade on 10 without `specialUse`; a forced
  run: no `couldn't start`; a kill while on: back foreground within 15 s; off: the service and the notification gone), P11 (status lines "Home: 23 Nov, 10:03 AM · NASA",
  "Lock: … · APOD", chip "On home screen"), P12 (job 1 periodic, no job 2 outside P3), P15 (4 `TIME_SET, arming again`, each followed by `alarms for` the same T or the
  owed check), P17 up to the wallpaper (see the BUG and DIFFERENT), P19 (APOD's subject keys removed, `run -f 1`: `Subject: APOD 2026-11-23 p=0.91 in 115 ms`, a second run
  none; also after a force stop), P20 (keys removed, the page opened: the job the open started logged `Subject: APOD … p=0.91 in 80 ms`; the page's own and the job's
  line can't be told apart here), P24's probe half (`sizeChecked` removed: `Refresh: APOD 2026-11-23 rendition (original 2160×2700)`, the key noted again), P26's first
  half (`subjectSharp` removed: `Subject: … p=0.91`, the key noted again; the 2160×2700 file doesn't bite, so no `(sharp …)`), P27 (fresh install state: no
  `Download/APODroid`; `--ez weekLog true` while the page was on top: `Week log: on`, the process-start line with `Android 10 (API 29)`, `Keep running on`, the run's
  lines in order, the same as logcat's; a `kill -9` and Keep running's restart: a `Process start:` line with the new pid; `--ez weekLog false`, a run: the file ends with
  `Week log: off`, the run's lines only in logcat).
- **DIFFERENT:** P2/P3 ("not expedited"): on Android 10 `job 1 starts` and `alarm, run scheduled` carry no ", not expedited" (`DailyJob.kt` writes it from API 31, the
  plain job being the only kind below 12; the package text and the map's line are for 12+). P17: `am force-stop` on 10 deletes the two alarms, **keeps** the two broadcast
  `PendingIntent`s (`dumpsys activity intents` before and after: `#1`, `#2` `broadcastIntent` there, `p17-intents.txt`) and drops Live to the default, so the page open found
  nothing gone: no `Detector:` line, no box (also with the clock stepped to 24 Nov 06:50 after a force stop: no line, no box), job 1 runs at once, the alarms armed again. The
  gap the plan expected (10–14: the `PendingIntent` sign only until system_server's GC; no exit records on 10). Not narrowed: earlier lines in the buffer (09:08–09:10,
  E10b's run) show `Detector: the alarms found gone` after force stops on 10, right after the install; this run's three force stops never did. P3 (the orange box): not seen,
  `problemNoticeKey` was still E10b's dismissed `nasa:network:2026-11-23` (a Dismiss holds for the same kind and day, as `CLAUDE.md` says), so the box stayed hidden.
- **SKIPPED / not run:** P16 (a real morning: real timing); **left for time, not for an Android 10 reason** (so the package is not done and stays unticked): P13 (Save every
  picture is off; E10b ran A3 and the saves), P14 (no pause hint on 10; E10b's BH0–BH3), P18's APOD-only half (APOD mode would change the setup: only the stored-day failure
  line `Refresh: APOD … stored, check failed: …` was seen, in a run that failed through NASA as in P3), P19's broken-`apod.jpg` half, P20's separate page line, P21 (an edit of
  `size` and the records: the first try left a `apodroid.xml.bak` in `shared_prefs`, SharedPreferences' own backup name, which Android restored over the edit at the next
  start, so the run was P2's: nothing to judge; not repeated, and B35 would mask it after a force stop), P22, P23, P24's replacement half, P25, P26's `sharp` bite (a faked
  1500×1022 picture: E10a's P34e/B37 is the open item), P27's O6a (the Recents path), real timing, the toasts.
- **State at the end:** the prefs equal the start's but the timestamps, `live.homeId` (139) and `weekLog` false; Live on home again by the picker (home id 139, the lock
  screen its own still, id 133), daily on, More NASA pictures on, Keep running off, Save every off; the stale row and the empty `Download/APODroid` folder removed; the clock
  back by `auto_time` 1 (09:57, the host's), airplane mode off, network checked, rotation, font scale (1.0), density (420) and `screen_off_timeout` untouched; no alarm of the
  app lost (the pair at 24 Nov 06:05:20, job 1 periodic). The app is running, not force-stopped.

### [x] E10d. Android 10, the live map, first half (Opus, medium)

`interactions/live.md`, section 5, T01–T20, and the 10 row: Live is the home screen's only, the lock screen
gets Fill; `getWallpaperInfo` is null, so `Store.liveOn` takes the id path (`live.homeId`): the picker, a re-pick
(`COMMAND_REAPPLY`, unchecked), a lost Live. Device `emulator-5554` (`apodroid-29`, Android 10), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5554` instead of 5564; the cases of the E10 packages before it ran
aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

**Checked** (rosie, pipeline run 185, 2026-11-23, 10:01–10:12 by the emulator's clock, `emulator-5554`, Android 10 (`apodroid-29`), dev's
debug build 0.9.2 (the run's APK, already installed: the same SHA-256, not reinstalled); evidence in `.coord/runs/185-e10d/tablet/` (screenshots
`t<nn>-*.png`, `x-repick-*`, `x-lost-*`, `backup/` with the prefs and `dumpsys wallpaper` of the start, `wallpaper-end.txt`, `apodroid-end.xml`).
The start was the map's T0 as E10c left it (home NASA live, engine id 139 = `live.homeId`, lock APOD still 133, Size Live, daily on, 6:00 AM,
one source per screen so no "Change every", order APOD, NASA, Earth, Save every and Keep running off). `run-as` works. **No crash**
(`logcat -b crash` empty). Toasts were never read (the `Set:`/`Part:` lines and the ids stand for them). **On 10 the picker's sheet offers
"Home screen" and "Home screen and lock screen" only** (AOSP's LiveWallpaperPreview; no "Lock screen" alone), and SPLIT's question is 14+ only.
- **BUG:** re-picking APODroid's live wallpaper on Android 10 drops Live. Steps: T0 (home live, `live.homeId` 156); Home; Android's
  live wallpaper chooser (`am start -a android.service.wallpaper.LIVE_WALLPAPER_CHOOSER`) › APODroid › Set wallpaper › Home screen; then any
  run (`cmd jobscheduler run -f … 1`, or the page's load when the chooser returns to it). Expected (CLAUDE.md, Is it on, R2a): the same live
  wallpaper picked again stays Live. Got: home's id 156 → 158, the running engine kept (no `Live: engine home created`), **no
  `Live: engine re-applied`** (WallpaperManagerService only logs `bindWallpaperComponentLocked` for the same component; `COMMAND_REAPPLY`
  seems to be Android 11's), `live.homeId` stays 156, so the next run logs `Live: home isn't live, Fill on HOME+LOCK` and sets both stills
  (ids 159/160), the engine destroyed, "Live isn't on" on the page (`x-repick-2.log`, `x-repick-2-wallpaper-after-repick.txt`; the first time,
  from the page, `x-repick-1.log`). The likely file: `Store.liveOn` / `:core`'s `homeLive` below 11 (a new id with the engine still bound;
  note that package visibility starts with 11, so on 10 `getWallpaperInfo(FLAG_SYSTEM)` might name the component: not checked).
- **PASS:** T01 (summary "Home: NASA · Lock: APOD · 6:00 AM · Live", "Home: …· NASA", "Lock: … · APOD", no "Live isn't on"; C2 home APODroid,
  lock its own still), T02 (no dialog; `Live: the engine is on HOME, not noted live`, `Set: wallpaper set: …APOD… on LOCK; wallpaper shown
  live: …NASA… on HOME, darker ×0.83`; home id kept 139, lock 133 → 140), T03 (⛶ frame at p 0.22 as the engine's, Whole ✓: `Part: wallpaper
  shown live: … on HOME`, `Live: decoded 1080×2400`, `live.whole` true, `live.darken` 1.0; Whole off ✓: the same line with `darker ×0.83`,
  decoded 3200×2400, `live.whole` false), T04 (draft Auto: ⛶ still Live's frame, a drag moved nothing (the screenshots byte-identical), ✓ no
  log, no `cropPosition`; back to Live: the Setup title untinted), T10 (split back, Set: no dialog, lock APOD still 143, home live kept),
  T13 (in T0, not P2: off: the lines and the app's alarms gone, the engine kept; on: `Switch on: wallpaper set: …APOD… on LOCK; wallpaper
  shown live: …NASA… on HOME`, job 1 ran), T14 (in T0: two pulls, `Refresh:` lines only, nothing set), T15 (Set in T0: no dialog, lock APOD
  still; no `Live: engine flags` line, as on Android 14's AOSP build: the engine is never on the lock screen on 10), T17 (Auto, Set: `Live:
  engine home destroyed`, `… on HOME, auto 0.69, … on LOCK, auto 0.91`, both ImageWallpaper (home a new id 146), summary without " · Live"),
  T18 (Android's chooser › APODroid › Set wallpaper › Home screen with the stored Auto: `Live: engine home created`, the engine drew `live.jpg`
  (NASA, `p=0.23 ×0.83`), no hand-over, no "Live isn't on"; the engine noted `live.homeId` 148; Set (Auto): home a still again), T19 (dialog
  "Next, in Android's preview:\nSet wallpaper › Home screen", Cancel/OK; Cancel: `Set: the picker's dialog cancelled`, `Set: no pick, nothing
  saved`, Setup open, Size Live still the draft, `size` AUTO stored), T20 (Set › OK › Set wallpaper › the sheet's Back › Back: `Live: back from
  the picker, nothing changed, home not live`, `Set: no pick, nothing saved`; the preview drew `live.jpg`, NASA 20 Nov).
- **The 10 row:** **the picker** PASS (Set › OK › Set wallpaper › Home screen: `Live: engine home created`, `Live: back from the picker, a
  wallpaper changed, home live`, the hand-over on HOME, the lock screen APOD still; `live.homeId` = home's new id each time: 151, 156, 161,
  166). **A re-pick** FAIL (the BUG above). **A lost Live** PASS (Android's Wallpapers app › its built-in still › Set wallpaper › Home screen,
  page closed: `Live: engine home destroyed`; at the page's open `Live: home isn't live, Fill on HOME+LOCK`, `Page: wallpaper set: …NASA… on
  HOME, …APOD… on LOCK`, "Live isn't on. Set as wallpaper turns it on."; `x-lost-live.log`). That still set on home alone left Android 10's
  "Lock wallpaper state" empty (one shared still) until the app's run set the lock screen's own.
- **DIFFERENT:** T05 and T09 (Android 10, no SPLIT question: with the draft's columns equal and home live Set saved at once, `Set: wallpaper
  set: …NASA… on LOCK; wallpaper shown live: …NASA… on HOME`, summary "NASA · both screens · 6:00 AM · Live", one line "Changed … · NASA",
  lock a NASA still with its own id 141; a second Set the same, no dialog, lock 142: so T09's end state, without the question, as
  `opensPicker` says below 14).
- **SKIPPED:** T06, T07, T08 (the question doesn't exist below 14), T11 and T12 (the engine shared on both screens is 14+; on 10 Set never
  opens the picker with home live), T16 (AOSP 10's picker has no "Lock screen" answer, seen in its sheet), real timing.
- **State at the end:** T0 again by Set › OK › Home screen (home live id 166 = `live.homeId`, lock APOD still 167, Size Live, daily on, the
  setup's prefs as at the start, NASA's part as at the start); `auto_time` 1, `font_scale` 1.0, rotation 1/0, airplane mode off, all
  untouched; the app not force-stopped.

### [x] E10e. Android 10, the live map, second half (Opus, medium)

`interactions/live.md`, section 5, T21–T39, and SPLIT's Q1–Q8 (the 14+ cases SKIPPED with why); T21 on B36's
fix (Set keeps waiting while a stacked page opens over the picker). Device `emulator-5554` (`apodroid-29`, Android 10), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5554` instead of 5564; the cases of the E10 packages before it ran
aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

**Checked** (rosie, pipeline run 191, 2026-11-23, 14:49–15:02 by the emulator's clock, `emulator-5554`, Android 10 (`apodroid-29`), dev's
debug build 0.9.2 with B39 (the run's APK, already installed: the same SHA-256 `7b272f8a…`, not reinstalled); evidence in
`.coord/runs/191-e10e/tablet/` (screenshots `t<nn>-*.png`, `e10e-apodroid.log` the app's whole log, `backup/` with the start's prefs and
`dumpsys wallpaper`, `apodroid-end.xml`, `wallpaper-end.txt`). **The start wasn't T0:** home a still (327), lock 328, Size Live stored, no
process ("Live isn't on"), so T0 was set first by Set › OK › Set wallpaper › Home screen (home live 329 = `live.homeId`, lock APOD still 330).
T01's notes: 6:00 AM, one source per screen (no "Change every"), order APOD, NASA, Earth, "As soon as it's out" off, "Darken" on, Save every
and Keep running off. **No crash** (`logcat -b crash` empty), no FAIL, **no `BUG:`**. Toasts weren't read (the `Set:`/`Part:` lines and the
ids stand for them). As E10d saw, Android 10's sheet offers "Home screen" and "Home screen and lock screen" only, Set never opens the picker
while home is live, and SPLIT's question is 14+ (`opensPicker`'s `LOCK_TOO`).
- **PASS:** T21 on B36's fix (from P0 Auto, draft Live: Set › OK › Home › the launcher icon (`MAIN`/`LAUNCHER`, `NEW_TASK|RESET_TASK`) put a
  **second page over the picker** (task 146: page, `LiveWallpaperChange`, page) › Home › Recents › APODroid (the stacked page resumed) ›
  Back (the picker on top again): no `back from the picker` line through all of it; then Set wallpaper › Home screen: `Live: back from the
  picker, a wallpaper changed, home live`, the hand-over on HOME, lock APOD still, `live.homeId` 335, Setup's summary with " · Live"),
  T24 (P0 Auto: the daily switch off, on: `Switch on: wallpaper set: …NASA… on HOME, auto 0.69, …APOD… on LOCK, auto 0.91`, both
  ImageWallpaper, no dialog, no "Live isn't on"), T25 (dialog "Next, in Android's preview:\nSet wallpaper › Home screen" › OK › rotated in the
  preview (the preview engine destroyed and created again) › Set wallpaper › Home screen › back upright: home live NASA, lock APOD still,
  "Live isn't on" gone; R2b: the preview has no Settings entry, here and in every other picker case), T26 (Android's Wallpapers app (the
  `SET_WALLPAPER` chooser › com.android.wallpaperpicker) › its built-in still › Set wallpaper › Home screen; back to the page: `Live: home
  isn't live, Fill on HOME+LOCK`, `Page: wallpaper set: …NASA… on HOME, …APOD… on LOCK`, "Live isn't on"; as E10d's lost Live, this time
  with the page in the background), T31 (APOD mode, Where › Lock screen, Size Live: no dialog, `Set: wallpaper set: …APOD… on LOCK`, home's
  engine kept, summary "APOD · lock screen · 6:00 AM", one line "Changed …", no "Live isn't on"), T32 (Where › Home screen: no dialog,
  `Set: wallpaper shown live: …APOD… on HOME`, lock unchanged 348, "APOD · home screen · 6:00 AM · Live"), T35 (the list back (NASA Home
  screen, APOD Lock screen), Live, Set › the dialog › OK › Home screen: T0, summary "Home: NASA · Lock: APOD · 6:00 AM · Live", both status
  lines, T01's notes unchanged), T38 (Centre: no dialog, `Live: engine home destroyed`, `Set: wallpaper set: …NASA… on HOME, …APOD… on LOCK`
  without `, auto`, home's crop the middle 1060–2140 of 3200, `size=CENTRE`; then Live › the dialog › OK › Home screen: T0, home 357), T39
  (NASA's 3200×2400 picture, no fake needed: ⛶ › Sideways › ✓: `Part: wallpaper shown live: …NASA… on HOME, sideways, darker ×0.86`,
  `live.sideways=true`, `Live: decoded 1800×2400`; ⛶ › Sideways off › ✓: no `, sideways`, `darker ×0.83`, `live.sideways=false`, decoded
  3200×2400).
- **PASS in Android 10's form** (the question is 14+, so the same exits from the plain dialog and the picker, from P0 with Size Live stored):
  T30 (Set › the dialog › Recents › swipe: `Set: the picker's dialog cancelled`, `Set: no pick, nothing saved` at the swipe; the swipe killed
  the process (pid 4427 gone); the reopened page (6503) a cold start with the stored setup, Setup closed, nothing set), T28 (Set › OK › the
  preview › Recents › swipe: killed, no line, nothing saved or set (ids 344/345 kept); reopened 10 s later: a cold start, `size` LIVE as
  stored, "Live isn't on"). T29 doesn't apply (T28 saved nothing).
- **DIFFERENT** (as Android 10 should be: the engine is never on the lock screen): T27 ("Home screen and lock screen": home live, the lock
  screen an APOD still 347 by the same Set, `live.shared` false; no `…, shared`, no `Live: engine flags 1`), T33 (Where › Both with home
  live: no question, the Set saved at once as "Not now" would: `Set: wallpaper set: …APOD… on LOCK; wallpaper shown live: …APOD… on HOME`,
  lock an APOD still 349, one line "Changed …", "APOD · both screens · 6:00 AM · Live"; a second Set the same, lock 350; Q5's OK branch
  can't be reached), T34 (Where › Home screen, Auto: `Live: engine home destroyed`, `Set: wallpaper set: …APOD… on HOME, auto 0.91`, the
  lock screen kept its own APOD still 350: nothing shared to follow, so `sharedLockFollows` has nothing to do; no APODroid engine).
- **T37's Android 10 form, the re-pick (B39's fix):** from T0, Home › Android's chooser (`LIVE_WALLPAPER_CHOOSER`) › APODroid › Set wallpaper
  › Home screen: home's id 352 → 354, the engine kept (no `Live: engine home created`, no `engine re-applied`), then `cmd jobscheduler run -f
  … 1`: `Refresh:` lines only, nothing set, home still APODroid's, `wallpaperLive` true, `live.homeId` 354. **E10d's BUG is fixed on 10.**
- **SKIPPED:** T22 and T36 (Android 10's sheet has no "Lock screen"; T36 also needs the question), T23 (Android's settings › APODroid live ›
  Lock screen: the same sheet, no such answer), T37 as written (the question, 14+; its 10 form above), Q1, Q2, Q4, Q5, Q6, Q7, Q8 (the
  question "Live on the lock screen too?" is 14+, `opensPicker`'s `LOCK_TOO`: on 10 a Set with equal columns and home live saves without it,
  T33 and E10d's T05/T09; Q8 also needs "Lock screen"), Q3 (the question; its setup A-B ran as T33), real timing.
- **Seen, not a bug:** once, as the preview engine was destroyed after a pick, `W Live: couldn't draw: java.lang.IllegalArgumentException`
  after Android's `queueBuffer … failed (No such device)`: a draw on the preview's surface while it went; caught and logged, home's engine
  drew on.
- **State at the end:** T0 (home live NASA 357 = `live.homeId`, lock APOD still 360, Size Live, daily on); APOD mode's hidden "Where", HOME
  after T32–T34, put back to BOTH by two Sets (APOD mode, Both; then the list); the setup's prefs (`size`, `allSources`, `where`, `home`,
  `lock`, `daily`, `pictureAt`, `interval`, `order`, `soon`, `darken`, `sharp`, `saveEvery`, `keepRunning`, NASA's part) as at the start;
  `auto_time` 1, Europe/Zurich, `font_scale` 1.0, rotation 1/0 (T25's landscape set back), density 420, airplane mode off; the app alive.

### [x] E10f. Android 10, the pixels and the rest (Sonnet, medium)

SH4's pixel checks (`sharp-0.9.0-sh4.md`, Testing 2; `scripts/sh4_check.py` and `sh4_run.py`) on Centre,
Auto, Whole, Sideways and the limited set, both screens, through 10's `generateCrop` (its aspect fix: tall and wide
pictures with numbered rows, the frame's rows must survive); Auto's subject on 10's `ImageDecoder` with an EXIF-6 and
an EXIF-8 photo (`Subject:` line, the frame's rows); the large picture (a faked 37 MB PNG); `install -r` with Keep
running on; setup S3 (the photo picker, DocumentsUI on 10); M0's home/lock sequence; landscape screenshots with Setup
open. Device `emulator-5554` (`apodroid-29`, Android 10), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5554` instead of 5564; the cases of the E10 packages before it ran
aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

**Checked** (rosie, pipeline run 186, 2026-11-23, 10:13–10:34 by the emulator's clock, `emulator-5554`, Android 10 (`apodroid-29`), dev's debug build 0.9.2,
`install -r` over E10b's install; evidence in `.coord/runs/186-e10f/tablet/`: `sh4/`, `exif/`, `large/`, `m0/` (results.jsonl, the pulled `debug-set.png`
in `png/`, the system's cropped wallpaper JPEGs in `wp/`), screenshots, `backup/` (the start's prefs, `files/`, wallpaper dump). **No crash**
(`logcat -b crash` empty), **no FAIL of the app, no `BUG:`.** The pixel cases ran by Set's path (the job path of `sh4_run.py` doesn't run on 10, E10a): a copy of
the scripts in the scratchpad with `am force-stop` for the kill, two extra tall pictures (600×2400, 1000×3000: none of `sh4_pictures.py`'s is taller than the
screen) and, for the plain `setStream` cases, a read of Android 10's cropped wallpaper (`/data/system/users/0/wallpaper`, a JPEG written by `generateCrop`, via
`adb root`, decoded on the host by a Java `ImageIO` helper; the source rows and columns from a line fitted to the B and R gradients).
- **PASS:** the pixels, 31 cases of `sh4_run.py`, all PASS by its own judge: Centre and Auto on 2:3 (400×600), 0.45 (2160×4800), tall (600×2400, 1000×3000),
  3:2 (1500×1022, 900×600), 4:1 (2400×600), square 1200 (the plain path: crop hint = the frame, no `debug-set.png`); Whole (1500×1022, 400×600, 600×2400);
  the limited set (400×600 Centre and Auto `sharp ×2.00 (Fill ×4.00)`, 900×600 Centre, 640×480 `Fill ×5.00`, 1200×300 sideways `Fill ×3.60`, 1500×1022 Auto
  `, auto 1.00, sharp ×2.00 (Fill ×2.35)` with the grid at twice the file's scale: **B37's case, PASS on 10 again**); Auto's turn (`auto 0.00, sideways, auto turn`,
  4:1 `auto 1.00, sideways, auto turn`), the pill on under Centre (`, sideways`), dragged parts at 0.0 and 1.0; Home alone, Lock alone and Both.
  **The aspect fix of `generateCrop`:** the system's cropped wallpaper is 1080×2400 in every plain case and shows the frame's own rows and columns: wide
  pictures all rows and the middle columns (400×600: columns 64..334 of 400, rows −1..599; 1500×1022: 520..979, rows −1..1019; 2400×600: 1064..1334; 900×600:
  314..584), tall ones all columns and the middle rows (600×2400: rows 533..1866 of 2400, the frame is 533..1867; 1000×3000: 389..2608, the frame 389..2611), the
  0.45 picture (2160×4800) all of it within 0.4%, the square 330..868 of 1200 (the frame 330..870).
- **PASS:** Auto's subject on 10's `ImageDecoder` with EXIF photos (JPEG q 100 made from the numbered picture, turned and tagged 6 and 8; upright 1200×800 and
  800×1200): `Subject: APOD 2026-11-24 p=1.00 (sideways 0.29) (sharp 1.00) in 247 ms` for both landscape photos and `p=0.79 in 126 ms` for both portrait ones
  (so the photos were decoded upright, EXIF 6 and 8 alike); the sets `auto 0.29, sideways, auto turn` and `auto 0.79`, crop hint 204..744 × 0..1200 for the
  portrait ones. The frame's rows: portrait columns 207..733 of 800, all rows; the sideways band (its edge rows 701..188 of 800, the same as a no-EXIF JPEG of
  the same content, 700..188, where the frame is 726..186: the 4% deviation is the lossy JPEG source's chroma, not the orientation). `sh4_check.py` FAILs
  these (scale 2.03 for 2.0, frame off by 12–30 px) for the same reason, also for the no-EXIF JPEG control: its gradients need a lossless source. DIFFERENT
  only in that: the cases are PASS by the control.
- **PASS:** the large picture (a faked PNG 4455×5592, 49 MB, random noise; decodes to 99.6 MB): Centre, Auto (`Subject: … p=0.52 in 461 ms`, `auto 0.52`) and
  Auto with the switch and Whole (`debug-whole.png`, `debug-set.png` written) set on HOME+LOCK, no out-of-memory, no crash. The pixel judge doesn't apply (noise).
- **PASS:** `install -r` with Keep running on (switch on in Advanced: `KeepRunning` `isForeground=true`, channel `keep`): after the install the service is foreground again
  (a new record), `MY_PACKAGE_REPLACED, arming again`, `alarms for 2026-11-24T05:05:20Z`, the pair in `dumpsys alarm`, job 1 present, `keepRunning` and `daily` kept,
  no crash.
- **PASS:** setup S3 (P29): "If APOD is a video" › "Use my picture" opened AOSP's DocumentsUI (`com.android.documentsui/.picker.PickActivity`, "Downloads", "IMAGES ON
  DOWNLOADS"); the pushed JPEG picked: `fallback-draft.jpg` written (862,105 B), `fallback.jpg` not; back to "Skip it" and Set: `fallback.jpg` still absent; "Use my
  picture" again: no picker, "Change picture" shown (the draft's pick); "Skip it" again.
- **PASS:** M0's home/lock sequence on 10 (`dumpsys wallpaper` after each Set): Both → one shared wallpaper (id 208, no lock one); Home alone → home 209, the lock screen
  got the shared one (id 208); Lock alone → lock 210, home still 209; Both again → one shared (211), no lock one. As on Android 15 (CLAUDE.md, Wallpaper). No screen blank.
- **PASS:** landscape (2400×1080) with Setup open: two panes, the picture left, "Wallpaper", "Change automatically", "Setup ▴", "More NASA pictures", "Where" and the
  pinned "Set as wallpaper" bar right (`land-setup-open-1.png`, `-2.png`).
- **Not run:** the job path of `sh4_run.py` (E10a: doesn't run on 10), the update from 0.8.0, Live (E10d/E10e), real timing, toasts.
- **State at the end:** the start's prefs and `files/` put back (`apod.jpg`, `earth.jpg`, `nasa.jpg`, `live.jpg`), Live on home picked again by AOSP's picker (home NASA, lock APOD,
  Size Live, daily on, More NASA pictures on, Keep running off, ids 215/216; the page shows no "Live isn't on"), `fallback-draft.jpg`, `debug-*.png`, the test photo and its
  MediaStore row removed, `accelerometer_rotation` 1, `user_rotation` 0, `auto_time` 1, `font_scale` 1.0, airplane mode off.

### [x] E10g. Android 10, the cases E10b and E10c left (Sonnet, medium)

E10b's left cases (O2/R5's landscape pulls, K16b, P3b, P3e, K34a–d) and E10c's (P13, P14, P18's APOD-alone half,
P19's broken-file half, P21–P23, P25, P26's sharp bite, P27's O6a), on B38's and B39's tree. Device `emulator-5554` (`apodroid-29`, Android 10), dev's debug build, at most 50 min. The rules:
`interactions/tester-rules-emulator.md` with the serial `emulator-5554` instead of 5564; the cases of the E10 packages before it ran
aren't run again. Results under its own "Checked" here, per case PASS /
FAIL / DIFFERENT / SKIPPED; a FAIL as a `BUG:` line. **Done when** every case of the list ran or is
listed under Not run with why (a case left for time is not done: say so first in the report).

**Checked** (rosie, pipeline run 192, 2026-11-23, 15:10–15:30 by the emulator's clock, `emulator-5554`, Android 10 (`apodroid-29`), dev's debug build with B38 and B39
(`install -r` of the run's APK, SHA-256 7b272f8a…); evidence in `.coord/runs/192-e10g/tablet/`: `backup/` (the start's and end's prefs and `dumpsys wallpaper`), screenshots
`k16b-*`, `k34*`, `p3b-*`, `p26-*`, `p22-galaxy-debug-set.png`). Start state as E10d/E10e left it (home NASA live, lock APOD still, Size Live, daily on, More NASA pictures
on, Keep running off, Save every off, "Keep pictures sharp" off). **No crash** (`logcat -b crash` empty), **no FAIL, no `BUG:`.** Toasts were never read. Several cases need
Size Auto and a stored picture of my own, so the run set Size Auto by Set (Live off), did the cases, and picked Live again in AOSP's picker at the end.
- **PASS:** O2 and R5 (landscape 2400×1080: a pull on the left pane logged `Refresh:` for the three sources, a pull on the right pane nothing, the pinned bar and the pane unmoved),
  K16b (NASA's 4200×3150 post, Size Auto: "✓ Sideways" in tertiary, a horizontal band across the full width, the page's picture upright, `k16b-sideways.png`), K34 (APOD faked 1500×1022 with
  "Keep pictures sharp" stored on: ⛶'s frame 325 of 900 px on the screenshot = 36% of the width, Fill's would be 31%; the Setup draft still showed the older state after `am kill`, the saved
  state), **K34a** (300×200: no frame outline, a drag moved nothing, ✓ no `Part:` line, no `cropPosition`), K34b (undragged ✓: no `Part:` line; a drag then ✓: `Part: wallpaper set: … on LOCK,
  sharp ×2.00 (Fill ×2.35)`, `cropPosition` 0.2326, no `, auto`), **K34c** (Whole picture on the 300×200 picture: about 55% of the preview's width, i.e. 600 of 1080 px = 2×, `k34c2-small.png`; on
  the 1500×1022 picture the preview is the plain fit, the limit doesn't bite), K34d (stored Size Live, `sharp` true, NASA on home: the frame 364 of 1080 px = Fill's 33.7%, two screenshots byte-identical
  after a drag, ✓ no `Part:` line, no `cropPosition`), P3b (4 of 5 tries failed, see DIFFERENT; the successful one, rotation set right after the start: one `Video still: 329 ms, 864×1080`, a non-black
  tile after the rotation), P3e (a faked older stored date and `isVideo` true, `run -f 1`: `Refresh: APOD … rendition`, `video-still.jpg`, `videoStill.date/url` and `videoUrl` gone, `isVideo` false,
  `videoStillTried` kept; a second faked video day with another mp4 (`Auroras_Esa.mp4`): `Video still: 941 ms, 1080×608` once, none at the next open), P13 (`saveEvery` on, a new post faked: `Daily job: saved:
  APOD 2026-11-23 original`, one row `APOD_2026-11-23_…jpg` in `Pictures/APODroid/`; a second forced run: nothing; row deleted, switch off), P14 (no pause hint on the page; the app is not on
  `deviceidle whitelist`, bucket 50), **P18's APOD-alone half** (APOD mode by Set: `run -f 1` online `Refresh: APOD … same post`, no download, no set; airplane on: `Refresh: APOD 2026-11-23 stored, check
  failed: UnknownHostException`, `jobFinished`, no `Daily job failed`, the alarms the clock's next check 24 Nov 06:05:20, no `problem` key, no box on the page), **P19's broken-file half** (`apod.jpg` = 7 bytes, subject keys
  removed: `Subject: APOD 2026-11-23 couldn't: …ImageDecoder$DecodeException … Input contained an error`, `subjectDate` noted without a position; the real file back, the keys removed, a page open: `Subject: … p=1.00`),
  P21 (Size Auto, the records forgotten: `Daily job: wallpaper set: … NASA … on HOME, auto 0.69`, `… APOD … on LOCK, auto 0.91`, later `auto 0.51`, `auto 0.43`, `auto 0.39` for the faked pictures; no 0.55–0.65
  picture existed, 0.69 is the nearest), P22 (3000×1000 APOD on the lock screen, the job with airplane mode on so that its refresh didn't restore the real title: the galaxy text `… auto 0.43, sideways, auto turn`
  and `debug-set.png` 1080×2400 written; the horizon text `, auto 0.51` upright, no `debug-set.png`; Size Centre no `auto`, upright; the part's pill noted off: upright), **P23** (the large original: `Refresh: APOD
  2026-11-23 rendition (original 2160×2700)`, `apod.jpg` 1920×2400, as tall as the screen; the small original by the page's hook: the stored post's `imageUrl` faked to 7 Nov's `GerlacheKocherMassif_Sans.jpg`,
  `sizeChecked` removed, a page open: `Refresh: APOD 2026-11-23 original 1500×1022 (smaller than the screen)`, `stored picture replaced by the original`, `Subject: … p=0.39`, `apod.jpg` 1500×1022, a second open
  none of it), **P25** (a faked `imageUrl` `…/Nonexistent_P25.jpg`, hook at a page open: `Refresh: APOD 2026-11-23 probe failed: …HttpStatus: HTTP 404`, the subject and the page's set went on), **P26's sharp bite** (APOD faked 1500×1022,
  `sharp` true, Size Auto, the lock record forgotten, `run -f 1`: `Daily job: wallpaper set: … on LOCK, auto 0.85, sharp ×2.00 (Fill ×2.35)`, `debug-set.png` 1080×2400 with the picture's
  background colour in the band rows and the frame bright: not white, so B37 holds in the job path; `subjectSharp` removed: `Subject: APOD … p=0.85 (sideways 0.40) (sharp 0.85)` once, none at the second open; `sharp` false: no `, sharp`),
  P27's O6a (`am force-stop`, `--ez weekLog true` as the task's first start, `--ez weekLog false` while the page was on top (delivered to the running top-most instance), Back, the Recents card tapped, a `run -f 1`:
  `weekLog` stays false, `Download/APODroid/log.txt` ends with `Week log: off` and holds none of the run's lines, logcat has them).
- **DIFFERENT:** P3a/P3b's frame on Android 10: for the same 4.8 MB `NoctilucentNeowise_Girotti.mp4`, `MediaMetadataRetriever` gave a frame on 3 tries (326–826 ms) and `StagefrightMetadataRetriever: all codecs failed
  to extract frame` (`IOException: no frame`, logged as `Video still: couldn't make one`, the tile black) on 9, with or without a rotation, so the software decoder here is flaky, not the code path (as E10b's first file); the
  rotation race of P3b was one start plus `settings put system user_rotation 1` about 0.3 s later, so the rotation may have come after the try's first steps. P22 (the job path): as the package says
  the job's refresh restores the real title for the same date, so the faked text only reached the set with airplane mode on (a failed check of a stored day, as D1); with a stored part whose pill was noted off the
  galaxy text stayed upright (my first run, K29's `sideways=false` left in the prefs).
- **Not run:** P24 and E9a's real feed (the 7 Nov post can't be served: the hook with a faked `imageUrl` stands for it), the picture of P22's `debug-set.png` turned was not looked at pixel by pixel (size only),
  P14's "unused apps" line (none on 10), the toasts, real timing.
- **State at the end:** Live on home picked again by AOSP's picker (home NASA, lock APOD still, Size Live, daily on, home id 382→ the app's `live.homeId` 382 then a restart, summary "… · Live", no "Live isn't on"), More NASA pictures
  on with the same list (APOD Lock, NASA Home, Earth unticked), Save every and Keep running off, "Keep pictures sharp" off, `weekLog` false and `Download/APODroid/` empty, the gallery without APODroid rows, `files/`
  as at the start (the fakes, `debug-*.png`, `video-still.jpg` removed; `apod.jpg` is the real 1920×2400 rendition, it was 400×600 at the start), the prefs equal the start's but `live.homeId`, `cropDate`/`cropUrl` (APOD's stored part), the
  `videoStillTried` key removed; `accelerometer_rotation` 1, `user_rotation` 0, `auto_time` 1, `font_scale` 1.0, density 420, airplane mode off, network pinged. The app is running, not force-stopped.

### [ ] E12. Android 12 (`apodroid-31`, `emulator-5558`; Sonnet, medium)

E2's cases and the 12 row of "The levels", then a smoke of each map (the cases the version table
says differ: U1's colour events after every set, B25's text, DocumentsUI, the expedited job 2 and the
fallback, the exit-record sign, Keep running's notification, Live's id path).

### [ ] E13. Android 13 (`apodroid-33`, `emulator-5562`; Sonnet, medium)

E3's cases and the 13 row, then a smoke of each map: the system photo picker, both hint lines, colour
events from the foreground app only, Keep running only in Active apps, the expedited job 2, Live's id
path.

### [ ] UP. The update from old versions (Sonnet, medium; on `emulator-5564` only, user, 2026-11-23; E12 and E13 after 0.10.0)

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
**Android 10 fails** (E10c, pipeline run 184, 2026-11-23, `emulator-5554`, wp/e10c unmerged; evidence
`.coord/runs/184-e10c/tablet/p27-stale-evidence.txt`): MediaProvider logs `UNIQUE constraint failed: files._data` and
returns null from `insert` instead of throwing, so `WeekLogNames.make` stops at the null: no `log (1).txt`, nothing
written. Open: a null insert taken as a taken name (the next one tried), `AppLog.kt`'s `make` and `WeekLog.kt`'s `make`.

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

### [ ] B35. A force stop on Android 14 (14+; 10–13 by B39) keeps home's wallpaper id, so `homeLive` still says Live is on (Opus, medium)

- **Steps:** E14c's P17: Android 14 emulator, Size Live, home live, `am force-stop`, then a run (`run -f 1`) and the page.
- **Happened:** home becomes the ImageWallpaper but keeps its id (109), so `homeLive`'s id rule (`Live.kt`, L8f1's fix
  round 1) still says Live is on: no `Live: home isn't live, Fill on HOME+LOCK`, no "Live isn't on" line, the record stays
  noted live, Set with Size Live opens no picker.
- **Should happen:** the lost Live is found (Decision 12): both screens Fill, the status line, Set opens the picker.
- **First, the evidence** (user, 2026-11-22): L8f1's kill in the split state redone on the tablet (Android 15, ZUI),
  logging what `getWallpaperInfo(FLAG_SYSTEM)` returns while `dumpsys` shows `CONNECTION_TRY_TO_REBIND` (L8f1's
  records show the component still APODroid's in `dumpsys` and the id kept, not what the app read), and whether the
  new process creates a non-preview engine. Then the fix by it: **null** → trust the id only while `getWallpaperInfo`
  names nothing (Opus pass 63); **another component** → trust the id only while a non-preview engine runs in this
  process (a kill rebinds and starts one, a force stop doesn't), the grace as before. Either way L8f1's case stays live.
- **Below 14** the id is the only sign: whether a force stop keeps the id there too, E10's P17 tells (a BUG line there
  extends this package). **It does** (E10c, pipeline run 184, 2026-11-23, `emulator-5554`, wp/e10c unmerged): three
  force stops, home `ImageWallpaper` each time with its id kept (131, 134, 135), no Fill, no "Live isn't on" line, and a
  Set logged `wallpaper shown live … on HOME` to no engine. So the fix needs a sign besides the id on 10–13 too
  (`getWallpaperInfo(FLAG_SYSTEM)` naming another component, or no non-preview engine in this process), and P17 again on 10.
- **Files:** `core/.../Live.kt` (`homeLive`), `Store.liveOn`/`liveState`, `LiveWallpaper.kt` if the engine rule, `LiveTest`.
- **Done when:** a test that fails on the old code (the kept id with another component), the fix, the tablet's L8f1
  kill still Live, P17 again on the Android 14 emulator.
- **Built** (pipeline run 193, merged 4ece090, 2026-11-23; Opus pass 70): the engine rule on 14+, `homeLive(named, id,
  noted, engine, idCounts)`: the kept id counts only while a non-preview engine is bound in this process
  (`LiveTest.fourteenAKeptIdWithNoEngineIsNotLive`), and the line `Live: home's id kept, Android names …, engine bound|none in
  this process`. Not the plan's **null** branch: after a force stop `getWallpaperInfo(FLAG_SYSTEM)` names nothing too (the
  default ImageWallpaper), so it couldn't tell one (rosie, the Android 14 emulator: `Android names null, engine none`, id 413
  kept, then `Fill on HOME+LOCK`, the status line, Set's picker and Live back: P17 PASS). The emulator's `kill -9` stayed
  live, but by name (`getWallpaperInfo` still named APODroid's), so the id-with-engine path never ran on a device.
  **Open:** the tablet's L8f1 kill (split state, ZUI, Android 15), the case whose `CONNECTION_TRY_TO_REBIND` left Android
  naming nothing for 11 min: if no engine is bound in the new process meanwhile, B35 sets Fill on both again (regression
  12); the new log line says which. P17 on Android 10 is B39's.

### [ ] B36. A second page stacked over the picker ends Set's wait (Sonnet, medium)

- **Steps:** E14e's T21: Set with Size Live opens Android's picker; tap the launcher icon while it's open.
- **Happened:** a second page stacks; its `onResume` completes `pickerBack` (`MainActivity.kt`), so Set ends "no pick,
  nothing saved" while the picker is still open. A pick then is lost: the engine on home, Size AUTO still stored, the old
  `live.jpg`, no toast.
- **Should happen** (user, 2026-11-22): Set keeps waiting until the picker returns.
- **Fix:** `onResume` ends the wait only in the page that started Set, or one a rotation made of it (the same page key in
  `live`); a stacked page doesn't. `registerForActivityResult` brings the return to that page; the fallback stays for a
  start from the background. CLAUDE.md (Set as wallpaper, The picker: "a page's `onResume` ends the wait as a fallback")
  and `interactions/live.md`'s T21.
- **Done when:** the fix and T21 again on the night's emulator (the code isn't version-specific).

### [x] B37. Keep pictures sharp's limited set comes out white on Android 10 (Sonnet, medium)

- **Steps:** E10a's P34e: Android 10 emulator, Size Centre or Auto, "Keep pictures sharp" on, a 3:2 picture whose
  limit bites (Fill would enlarge it over 2×), Set.
- **Happened:** `files/debug-set.png` is white but for a strip about 120 px wide; the set's log line as expected
  (", sharp ×2.00 (Fill ×…)"). Android 14 and 17 pass the same case.
- **Should happen:** the limited frame drawn into its dest on the blurred or black background, as on 14.
- **Fix (Opus pass 65; user, 2026-11-23):** `Drawing.kt`'s `limitedPicture` never decodes above the file's size: when
  the scale is over 1, decode only the frame's crop at the file's own size (no `setTargetSize`) and let the filtered
  `drawBitmap` into dest enlarge it. Suspected, not proven: Android 10's `ImageDecoder` mishandles `setTargetSize`
  larger than the file together with `setCrop`. Check the same pattern elsewhere (`decodeFrame`, `wholePicture`'s
  upscale of a small picture) and say in the report.
- **Done when:** the fix, P34e again on `emulator-5554` (`sh4_run.py`'s Set path; its job path doesn't work on 10),
  and SH4's limited cases unchanged on 14 by reading (the look the same).
- **Checked** (pipeline run 182, merged 48ca5b7, 2026-11-23, `emulator-5554`): P34e's 1500×1022 under Auto, Centre and
  Home `, sharp ×2.00 (Fill ×2.35)`, the frame at ×2 on the blurred bands, no white strip; 900×600, 400×600 and Auto
  sideways pass; the switch off unchanged; Whole on 900×600 and 640×480 passes (`drawWhole`'s enlarging
  `setTargetSize` shows no fault on 10). Not run: SH4's limited cases on 14 (by reading: nothing changes at scale ≤ 1).
  `sh4_run.py`'s fake doesn't work on 10 (`run-as … kill -9` does nothing; rosie ran a copy with `am force-stop`).

### [ ] B38. The week log's stale-row fallback writes nothing on Android 10 (Sonnet, low)

- **Steps:** Android 10 emulator, the week log off, `rm` `Download/APODroid/log.txt`, give its MediaStore row another
  owner, `kill -9` the app, switch the log on. (E10c's P27.)
- **Happened:** MediaProvider logs `UNIQUE constraint failed: files._data` and returns **null** from `insert` instead
  of throwing, so `WeekLogNames.make` stops at that null: no `log (1).txt`, no log line, nothing written.
- **Should happen** (B31): a taken name tries the next one, `log (1).txt`, `log (2).txt`, ...
- **Fix:** in `WeekLog.kt` and `AppLog.kt`, treat a null insert like a taken name and try the next one.
- **Files:** `app/.../WeekLog.kt`, `app/.../AppLog.kt`, `core/.../WeekLogNames.kt` if it holds the loop, `WeekLogTest`.
- **Done when:** a `WeekLogTest` case that fails on the old code (a null insert), the fix, and P27's stale-row steps
  again on `emulator-5554`.

### [ ] B39. Android 10–13: a re-pick gives home a new id (Live taken as lost), a force stop keeps it (Live taken as on) (Opus, medium)

- **Steps:** Android 10 emulator, Live on home. Pick APODroid again in Android's own chooser
  (`LIVE_WALLPAPER_CHOOSER` › APODroid › Set wallpaper › Home screen), then any run (`cmd jobscheduler run -f
  io.github.buerlino.apodroid 1`) or reopen the page. (E10d's `x-repick` steps.)
- **Happened:** home's wallpaper id changes (156 → 158) and the same engine stays bound; no `COMMAND_REAPPLY` arrives
  (no `Live: engine re-applied`). `live.homeId` stays 156, so the next run logs `Live: home isn't live, Fill on
  HOME+LOCK`, sets stills, destroys the engine, and the page says "Live isn't on." Live the user just picked is gone.
- **Should happen:** the re-pick stays Live.
- **Fix:** in `Store.liveOn` below 14, while a non-preview engine in this process is still bound and has not been
  destroyed since `live.homeId` was noted, take home's new id as live and note it. Do it together with **B35's second
  sign on Android 10–13** (pass 66; E10c: a force stop keeps the id 131/134/135, so `getWallpaperInfo(FLAG_SYSTEM)`
  naming another component, or no non-preview engine in this process, must also count): both decide `homeLive` on 10–13.
  The engine-bound rule serves both. CLAUDE.md's "Is it on" already says Android 10 sends no `COMMAND_REAPPLY`
  (doc fix 8b0d92b).
- **Files:** `core/.../Live.kt` (`homeLive`), `Store.liveOn`/`liveState`, `LiveWallpaper.kt` (the engine's bound state), `LiveTest`.
- **Scope** (user, 2026-11-23): below 14 only; B35 keeps 14+ with its tablet check, and takes this engine sign if
  the tablet shows it's needed there.
- **Done when:** a `:core` test of the rule that fails on the old code (a new id with the engine bound; a kept id with
  no engine), the fix, E10d's re-pick steps, E10c's P17 and a `kill -9` with Live on (still Live) again on
  `emulator-5554`.

### [ ] B40. `scripts/sh4_run.py` works on Android 10 (Sonnet, low)

- **Steps:** run `sh4_run.py`'s job path on the Android 10 emulator.
- **Happened:** `run-as ... kill -9` does nothing on 10, and `am force-stop` deletes job 1, so rosie ran a scratchpad
  copy (E10a, E10f, regression 19).
- **Should happen:** the script's fake and kill step works on 10 (`kill -9` as root, or the Set path), unchanged on 14 and 17.
- **Files:** `scripts/sh4_run.py` (and `sh4_check.py` if the EXIF JPEG chroma FAIL needs the control inside it).
- **Done when:** the script's 31 pixel cases run on `emulator-5554` and `emulator-5564` without a scratchpad copy.

### [x] L8T. L8f1's kill on the tablet after B35 (Sonnet, medium)

The real-device case behind L8f1 (regression 12), on B35's tree (user, 2026-11-23, Opus pass 70): HA24BZ2Y
(Android 15, ZUI), the split state (Live on home, a still on the lock screen), `lastRunAt` over 15 min old, one
`run-as … kill -9`, then a run (`run -f 1`) 30 s and 5 min after. Read B35's log line `Live: home's id kept, Android
names …, engine bound|none`. **Done when:** Live still on home after both runs (PASS); with `engine none` a `BUG:` line
(a fix package then holds such a run while the id is kept). B35's rule kept (user, 2026-11-23).
Also noted (user, 2026-11-23): the video still's "no frame" on the Android 10 emulator (E10g, 9 of 12) is left as is
in the freeze, seen on the emulator only.

**Result (rosie, 2026-11-23, HA24BZ2Y, Android 15 ZUI, the dev debug build, run 194): PASS.** Split state built by prefs
(More NASA pictures on, home APOD+NASA, lock APOD, 6 h, Size Live, daily on) and Android's picker by adb ("Home screen"),
then `run -f 1` (home live, id 1605; lock its own still, 1606). At 19:17:04 `lastRunAt` was 17.5 min old; one
`run-as … kill -9` (pid 17561 → 17913, rebound at once). B35's line: `Live: home's id kept, Android names null, engine
bound in this process`. The run 30 s after (19:17:38) and 5 min after (19:22:16): `same post` for APOD and NASA, no set,
no `Live: home isn't live`, no `HOLDS`; `dumpsys wallpaper` after each: home 1605 `LiveWallpaper`, lock 1606 still;
`wallpaperLive` still true; no crash. `engine none` did not occur, so no `BUG:` line. B35's rule stands. Evidence:
`.coord/runs/194-l8t/tablet/` (dumps, `log-run30.txt`, `log-run5m.txt`, `logcat-all.txt`). Not run: the smoke list's
Set (not a winston check); the kill was one, never a second within 10 s.

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