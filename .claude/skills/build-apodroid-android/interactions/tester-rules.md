# Rules for the FP6 testers (2026-11-15)

The phone is the user's own Fairphone 6 (adb serial 4d42e62f, LineageOS, Android 16), with the
release build of APODroid commit 7299474 installed (not debuggable: no run-as, no prefs reading).
The user is away and agreed to the tests; it stays awake on USB (set by the main session, which
restores that itself; don't touch `stay_on_while_plugged_in`).

## Never
- Uninstall, clear data, `pm clear`, install anything, reboot, `adb root`, change the clock, the
  time zone, locale, or other apps' settings. No force stop (`am force-stop`): it drops Live.
- Turn on "Save every picture", pick a "my picture" photo, or delete anything you didn't create.
- Touch the repo (no edits, builds, commits).
- More than one process death (`am crash io.github.buerlino.apodroid`) per 2 minutes, at most 2 in
  your whole run (two within 10 s revert Live; two crashes within 60 s mark the app bad).

## Allowed
- Taps, swipes, Back/Home/Recents keys (`adb shell input …`), rotation
  (`settings put system accelerometer_rotation 0` + `user_rotation 1`/`0`; put both back as found).
- `uiautomator dump /sdcard/ui.xml && adb pull` to find nodes by text/bounds; `adb exec-out
  screencap -p > file.png` and Read the png to see the screen (toasts show only a few seconds:
  screenshot right after the tap). Save screenshots under your own folder in the scratchpad.
- `adb logcat -d -v time -s APODroid:*` (clear with `logcat -c` only at the start of a case if you
  record the time), `dumpsys wallpaper`, `dumpsys alarm`, `dumpsys jobscheduler`, `dumpsys power`.
- Airplane mode (`cmd connectivity airplane-mode enable|disable`), always undone, network checked.
- ★ saves to the gallery: allowed; list each file you created (name + content URI) in the report,
  don't delete them.
- Android's live wallpaper picker (AOSP LiveWallpapersPicker): "Set wallpaper", then "Home
  screen" / "Lock screen" / "Home screen and lock screen".

## Start and end state (record it first, restore it last, and verify)
Record at the start: screenshots of the whole page and Setup/Advanced opened (scroll), `dumpsys
wallpaper` (which component/ids on system and lock), rotation settings, Keep running state.
Expected now: More NASA pictures on, home column NASA, lock column APOD, list order as shown,
Size Live with APODroid's live wallpaper on home and a still on the lock screen, "Change
automatically" on, the rest as you find it. At the end restore exactly that (Size Live needs one
trip through the picker: "Home screen"), and verify with a screenshot and `dumpsys wallpaper`.
If something can't be restored, say so prominently.

## The report
Write `<your file>` with: per case id: what you did, what you saw (screenshot paths, the log lines),
the expected result from the tree, and a verdict PASS / FAIL / DIFFERENT (behaves otherwise but
maybe fine) / SKIPPED (why). Then a summary: every FAIL and DIFFERENT with evidence, the SUSPECTs
from the tree confirmed or not, leftovers (gallery files, state not restored). Reply with that
summary only. Be factual: say what was actually seen, never what should have happened.
