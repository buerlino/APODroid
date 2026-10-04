# Keep working when the app isn't opened

Report of a phone test, 2026-10-04 evening. Phone: Fairphone 6, Android 16 (`BP2A.250805.005`, ROM `4.3-a16-…-official-FP6`), APODroid 0.1.2
(commit 3c5548e), R8 and debug builds.

## Summary

APODroid is meant to be installed and forgotten, and that is exactly the pattern Android punishes.
Two separate mechanisms, both confirmed on the phone:

1. **After 8 days without opening the app** (Android 13+, see Android versions), Android puts it
   in the *restricted* standby bucket.
   The daily job then also needs **charging + idle + battery not low**. The wallpaper only
   changes while the phone sits on the charger, unused.
2. **After about 3 months without opening it** (Android 12+), hibernation force-stops the app.
   That **deletes the job**. Nothing changes any more. In 0.1.2, when the user opened the app
   again, the switch showed **off**, with no explanation (since 7d73bc2 the page schedules it
   again).

The cause of both: **the daily job doesn't count as using the app.** Only opening the page does.

Both can be avoided by the user, in App info, without any permission:
- battery "Unrestricted" removes problem 1 (tested);
- "Manage app if unused" off removes problem 2 (that's what the setting is for; not tested
  end to end, see Open points).

## Finding 0: the job is not "use"

`adb shell dumpsys usagestats | grep "package=io.github.buerlino.apodroid totalTime"` prints
`lastTimeUsed`, `lastTimeVisible` and `lastTimeComponentUsed`.
- A forced job run that found nothing new (19:49): none of the three changed.
- A job run that set the wallpaper (19:58:39): `lastTimeComponentUsed` stayed at 19:57:29 (the
  last time the page was opened).
- Opening the page, and installing an update, do update them.

So neither the standby buckets nor hibernation ever see the app as used unless the page is opened.

## Problem 1: restricted bucket after 8 days

**Thresholds on this phone** (`dumpsys usagestats`, end of the dump):
```
mScreenThresholds=[0, 0, 3600000, 7200000, 21600000]
mElapsedThresholds=[0, 43200000, 86400000, 172800000, 691200000]
```
The five entries are the buckets active, working set, frequent, rare, restricted. Without use,
the app drops a bucket once *both* the elapsed time (12 h, 24 h, 48 h, **8 days**) and the
screen-on time since the last use (1 h, 2 h, 6 h) have passed. A phone that's used daily reaches
the screen-on part easily, so in practice: rare after 2 days, **restricted after 8 days**.

**What restricted does to the job** (forced with `am set-standby-bucket … restricted`, then
`dumpsys jobscheduler`, the job `JOB #u0a309/1`):
```
Required constraints: TIMING_DELAY DEADLINE CONNECTIVITY FLEXIBILITY
Dynamic constraints:  CHARGING BATTERY_NOT_LOW IDLE CONNECTIVITY
```
In the active bucket the "Dynamic constraints" line is empty. The extra ones are added by the
system, not by our `JobInfo`. IDLE means the screen has been off for a while ("device idle"),
so the job runs when the phone charges unused, typically at night.

**Who it hits:**
- Users who don't charge overnight (top-ups during the day while using the phone): the job
  practically never runs. The wallpaper stops changing after about a week.
- Users who charge overnight: it runs at night. In Europe the new APOD appears at 06:05
  (summer time). A run before that finds yesterday's picture, so the wallpaper can lag a day.
  Not measured.

**Not measured:** how often the job runs in the *rare* bucket (days 2–8). The quota line showed
`WITHIN_QUOTA` going unsatisfied right after a forced run, so the rare and restricted buckets also
limit how often it runs. We have no numbers. Also not checked: whether the dynamic
`CONNECTIVITY` means "unmetered".

**What fixes it, tested in the real Settings screen:** App info → App battery usage → Allow
background usage → **Unrestricted**. This puts the app on the Doze allowlist
(`dumpsys deviceidle whitelist` shows `user,io.github.buerlino.apodroid,10309`) and in **bucket
5, exempted**. `am set-standby-bucket … restricted` is then ignored (it stays 5), and the job
has no dynamic constraints. Back to **Optimized**: bucket 10 again. The adb equivalent,
`dumpsys deviceidle whitelist +io.github.buerlino.apodroid`, behaves the same.

## Problem 2: hibernation after about 3 months

**Threshold:** not read from the phone. `device_config list permissions` has no override, so
it's AOSP's default (90 days without use; the check runs about daily in the permission
controller). `device_config get app_hibernation app_hibernation_enabled` is `null`, i.e. the
default, which is on (hibernation is on, see below).

**What it does** (simulated for this app only:
`adb shell cmd app_hibernation set-state io.github.buerlino.apodroid true`):
- `dumpsys package … | grep stopped=` → `stopped=true` (as after "Force stop").
- The job is gone (`dumpsys jobscheduler | grep -c "JOB #u0a309/1"` → 0). A stopped app gets
  no jobs, and the stop deletes the scheduled ones.
- Opening the app un-hibernates it (`get-state` → false), but the job stays gone. In 0.1.2 the
  page then showed the switch **off** (its state was `getPendingJob`): the wallpaper silently
  stopped weeks ago, and the user had to notice and turn the switch on again. Since 7d73bc2 the
  page schedules it again (option 3).

The same happens on any "Force stop" in App info, and on `pm clear`/"Clear storage".

**What fixes it:** App info → Unused app settings → **Manage app if unused** off (labels on this
phone; on stock Android 12–14 it's "Pause app activity if unused"). Its summary here reads
"Remove permissions, delete temporary files, stop notifications, and archive the app". The
setting is the app op `AUTO_REVOKE_PERMISSIONS_IF_UNUSED`
(`adb shell appops get io.github.buerlino.apodroid AUTO_REVOKE_PERMISSIONS_IF_UNUSED`, now
`default`). An app reads it with `packageManager.isAutoRevokeWhitelisted()` (API 30+, true = the
user turned it off) or androidx `PackageManagerCompat.getUnusedAppRestrictionsStatus()`
(`DISABLED` = exempt).

## Android versions

minSdk is 29. Read from AOSP's `AppStandbyController` and `Settings` for Android 10–15 and
`main` (android.googlesource.com, declutter pass of 2026-10-04 evening; no phone with 10–12):
- **Restricted bucket:** Android 10 has none (rare is the last bucket). Android 11 has it but
  switched off by default (`DEFAULT_ENABLE_RESTRICTED_BUCKET = 0`; 30 days when on). Android 12:
  after 45 days. Android 13+: after 8 days. Hence the app checks battery only on API 33+.
- **"Unrestricted"** doesn't exist in App info on 10–11; there it's Battery optimization → Not
  optimized, in a list of all apps. `isIgnoringBatteryOptimizations` is false by default on all
  versions.
- **Hibernation** exists from Android 12 (API 31). Android 11 only resets runtime permissions of
  unused apps; APODroid has none, so 11 is not affected. Android 10 has neither.

## Settings quirk on this phone

App info → App battery usage showed "Allow background usage" **off**, and its sub-page had
neither Optimized nor Unrestricted selected. The system state was the default, though:
`RUN_ANY_IN_BACKGROUND` allow, not on the allowlist, job `readyNotRestrictedInBg: true`. Turning
the switch on showed "Optimized" with no change in the system. So the Settings app displays an
app that never had a battery mode chosen as "off". A hint text must not say "turn on background
usage". It must name the target state, **Unrestricted**. It's now set to Optimized explicitly
(system state as before).

More, from the declutter pass the same evening:
- After `adb install -r` the battery page showed "Allow background usage" off again, though it
  had been set to Optimized. Its radio buttons (Optimized, Unrestricted) are disabled until that
  switch is on, so the user turns it on first.
- App info didn't redraw "Manage app if unused" after a second tap; a freshly opened App info
  showed it right.
- The Settings screen writes the op's *uid* mode (`appops get` → `Uid mode: … ignore`; turned
  back on, it stays an explicit `allow` where it had been unset), while How to test sets the
  package mode. Both work, and the app reads them alike.

## Options for the fix

1. **Hint + button, no permission (recommended).** While the daily switch is on and the app
   isn't exempt from both, one short line and a button that opens App info
   (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`, `package:` URI). Both settings live there.
   - Checks, on each `onResume` so the hint goes away once done:
     `getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)`
     (true = Unrestricted) and, on API 30+, `packageManager.isAutoRevokeWhitelisted()`
     (on 29: treat as exempt).
   - The battery setting is three levels deep (App battery usage → Allow background usage →
     Unrestricted). The text should name both settings briefly. Labels differ between Android
     versions and ROMs, so don't copy them verbatim. Show the user.
   - The alternatives for the button are worse:
     - `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` opens a list of all apps.
     - `IntentCompat.createManageUnusedAppRestrictionsIntent` only covers hibernation.
2. **`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` + the system dialog.** One tap for problem 1, but it's
   a new permission (granted at install). `CLAUDE.md` ruled it out (2026-10-04), unless the user
   reopens it. `features.md` says F-Droid flags it. That's not verified, so check before arguing
   either way. It doesn't help with hibernation.
3. **Remember the switch in a pref** and schedule the job again in `onCreate` when the pref is on
   but `getPendingJob` is null. Small; it repairs the state as soon as the user opens the app
   after a hibernation or force stop. It changed the decision "the switch's state is whether the
   job is scheduled, no pref" (the user agreed, below). It doesn't help a user who never opens
   the app.
4. **Status line** (`features.md`, item 3): makes a stalled job visible, but only to someone who
   opens the page. Opening the page also resets both timers.
5. Out of scope: a live wallpaper (`WallpaperService`, exempt while active, but a rewrite of how
   the picture is shown), a foreground service or notification (`CLAUDE.md`: no notification).

Recommendation: 1, plus 3 if the user agrees. Then the hint covers new installs, and 3 repairs
the switch for anyone who got hibernated anyway.

**Decided and built (user, 2026-10-04):** 1 and 3, and the hint can be hidden for good after a
warning dialog. Unlike option 1, hibernation is checked from API 31 and battery from API 33
(Android versions). See `CLAUDE.md` and `features.md`, item 5.

## Open points

- Whether "Unrestricted" alone also prevents hibernation. Unknown; probably not, they're separate
  systems. Testing it would need `device_config` thresholds that hibernate *every* unused app on
  the phone, so don't do that on the user's phone. Show the hint until both are set.
- The real 90-day hibernation run (the permission controller's job), as opposed to the `cmd`
  simulation. The simulation calls the same service, so the result should be the same.
- Archiving (in the "Manage app if unused" summary): Android 15+ can archive unused apps, but
  only through an installer that supports it (Play). Not checked for F-Droid or Obtainium installs.
- How often the job runs in the rare bucket, and when exactly in the restricted one.

## How to test (adb)

`A=~/Android/Sdk/platform-tools/adb; P=io.github.buerlino.apodroid`
- Bucket: `$A shell am get-standby-bucket $P` (10 active, 20 working set, 30 frequent, 40 rare,
  45 restricted, 5 exempted). Force: `$A shell am set-standby-bucket $P restricted`. Opening the
  page sets it back to active.
- Job constraints: `$A shell dumpsys jobscheduler | grep -A50 "JOB #u0a309/1" | grep -E
  "Dynamic constraints|Unsatisfied|readyNotRestrictedInBg"` (the uid `u0a309` changes after a
  reinstall).
- Unrestricted without the UI: `$A shell dumpsys deviceidle whitelist +$P` (and `-$P` to undo).
- Hibernation: `$A shell cmd app_hibernation set-state $P true`, then `get-state`,
  `dumpsys package $P | grep stopped=`, and the job count. Opening the app undoes it, but the job
  must be switched on again.
- "Manage app if unused" from adb: `$A shell appops set $P AUTO_REVOKE_PERMISSIONS_IF_UNUSED
  ignore` (off) / `default`.
- Use: `$A shell dumpsys usagestats | grep "package=$P totalTime"`.
- `am force-stop` and `pm clear` also delete the job; use `am kill` to restart the app.
- Restore after testing: bucket via opening the app, allowlist `-$P`, battery back to
  Optimized, "Manage app if unused" on, `pauseHintHidden` removed (skill, Working on the phone),
  the switch on.
