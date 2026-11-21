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
| 12 | **R5** 0.10.0's texts (versionCode 15, `15.txt`), the tag | Sonnet, low | none | After 1–11 with no open `BUG:` |
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

### [ ] SIM. 20,000 seeds (Sonnet, low)

`RulesSimulationTest` with 20,000 seeds (so far 2000), by its seed property, on the PC; every
failing seed reduced to its first wrong day and reported as a `BUG:` with the seed.

### [ ] CR. Code review (Opus, high)

Three reviewers by area, each blind to the others: (a) the job, the alarms, the rules and the
detector (`DailyJob`, `AlarmReceiver`, `Store.update`/`setDue`, `:core`'s rules); (b) the
wallpaper and Live (`Store.set`, `Drawing.kt`, `LiveWallpaper`, the picker trip); (c) the page
(`MainActivity`, the draft, the saved state, ⛶). Each finding verified by a second agent before it
becomes a `BUG:`. Correctness only, no style.

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
