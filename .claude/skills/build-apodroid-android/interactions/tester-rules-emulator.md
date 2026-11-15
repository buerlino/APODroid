# Rules for the emulator testers (0.8.0's night test pass, 2026-11-15)

The device is the Android 14 emulator `emulator-5564` (AVD `apodroid-34`, 1080×2400, 420 dpi,
started with `-gpu host` as `emulator.md` says; never HA24BZ2Y, which is pascaline's, never the
FP6). It is ours: nothing on it is the user's. dev's debug build is installed (debuggable:
`run-as` and the prefs work). Every `adb` call names the serial: `adb -s emulator-5564 …`.

## Allowed (unlike `tester-rules.md`'s FP6 rules)
- Installing dev's debug APK the prompt names (`install -r`), `pm clear`, `am force-stop`,
  `am crash`, `adb root`, `run-as io.github.buerlino.apodroid` (the prefs, `files/`), the debug
  dumps (`files/debug-set.png`, `files/debug-whole.png`).
- The clock: `adb root`, `settings put global auto_time 0`, `date <MMDDhhmmYYYY.ss>`; set back
  (`auto_time 1`) and checked at the end of the case that changed it. The time zone likewise.
- Faking posts and days as SKILL.md says ("Fake an old day", "End a turn").
- Airplane mode, a firewall chain or a proxy for offline cases: always undone, network checked.
- ★ and "Save every picture": saves allowed; delete what you created at the end.
- Taps, swipes, keys, rotation (`accelerometer_rotation 0`, `user_rotation 1`/`0`, set back),
  `wm density 344` and font scale 2.0 (set back), `uiautomator dump`, `screencap`, `logcat -s
  APODroid:*`, `dumpsys wallpaper|alarm|jobscheduler|power|activity`.
- Android's live wallpaper picker (AOSP's on the emulator).

## Never
- Touch the repo: no edits, builds, commits (the APK is built for you).
- Another device, `adb` without `-s emulator-5564`, `adb emu kill` or restarting the emulator
  (say NOT RUN if it hangs: the main session restarts it).
- Real timing (Doze, standby buckets, makers' cleaners): those cases are SKIPPED, "real timing".

## Start and end state
At the start: screenshots of the page with Setup and Advanced open, `dumpsys wallpaper`, the
prefs file (`run-as … cat shared_prefs/*.xml`, saved to your folder). At the end: the same
setup again (Setup, Size, the daily switch, Live on home if it was), checked with a screenshot
and `dumpsys wallpaper`; the clock, the zone, rotation, density, font scale and network as
found. If a state can't be restored, say so first in the report.

## The report
Write `.coord/test-pass-0.8.0/<map>.md` (screenshots in `.coord/test-pass-0.8.0/<map>/`): per
case id what you did, what you saw (screenshots, log lines), the expected result from the map
and `CLAUDE.md`, and a verdict PASS / FAIL / DIFFERENT (behaves otherwise, maybe fine) /
SKIPPED (why). Then the summary: each FAIL and DIFFERENT with steps, expected, got, evidence
and the likely file; crashes (`logcat -b crash`, `dumpsys activity exit-info`); leftovers.
Reply with the summary only. Be factual: what was seen, never what should have happened.
