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
| 13 | **WK** the real week on three devices | the user; Opus, medium to read the logs | FP6, tablet, third | Real nights, makers' own Android |
| 14 | **R6** 1.0.0 | Sonnet, low | none | After WK with no `BUG:` |

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

### [ ] E14. Android 14, every map (`apodroid-34`, `emulator-5564`; Sonnet, medium)

All four maps in full (0.8.0's T3 and 0.9.0's T4 and T5 ran subsets), E4's cases, the levels' row for
14: the first 14 build's lock-screen live wallpaper off, `WallpaperCropper`, TP1's ten force stops.

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
(every change time, every set, every failure and its retry). `BUG:` → a fix, a 0.10.x, and the
week's count starts again only when the fix touches the job or the rules.

## Decided (user, 2026-11-22, on this file)

1. LOG's file in `Download/APODroid/log.txt` (a file manager can copy it; no adb needed at the week's end).
2. The FP6 keeps the user's own setup (the table's column).
3. E12 and E13: the version table's rows and a smoke of each map; 17, 14 and 10 every case.
4. **The nights:** the pipeline drives one device per night (`APODROID_TABLET`), so the first night
   runs LOG, A17, ER, SIM and CR (`emulator-5564` for LOG's check); ER writes E17, E14, E10, E12, E13
   and UP as `### [ ]` sections here, with each one's serial, and their queue rows go in after ER's
   merge, one emulator level per night (`APODROID_TABLET=emulator-<port>`), 17 first.
