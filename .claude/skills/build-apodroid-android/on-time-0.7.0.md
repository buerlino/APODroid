# On time on any phone (0.7.0)

(User, 2026-10-25, after a friend's Chinese phone had no "Unrestricted" battery setting; the
decisions are in `CLAUDE.md`, Next features, 0.7.0: on time on any phone. Built after 0.6.0
ships: 0.6.0 is in its code freeze.)

## What stops the job (investigated 2026-10-25)

Two layers, and they need different fixes.

**1. Android's own (every phone).** Since J2 a pair of alarms starts each run (`setWindow` +
`setAndAllowWhileIdle`) and the 6 h job is the fallback.
- Battery **Unrestricted**: on time (J0, V11a).
- **Optimized**: the alarm fires on time, but in Doze the run has no network until the next
  maintenance window, so it can be hours late (J0: up to 2½ h). The pause hint says so.
- Standby buckets (on battery): RARE allows about one alarm an hour (V11a: held 57 min) and
  limits the network. RESTRICTED (Android 13+, about 8 days without opening the app) allows
  **one alarm a day**, exact or not, and one 10-min job session a day, also while charging
  (developer.android.com, App Standby buckets). Opening the app lifts it.
- Not limited by buckets: apps on the Doze allowlist (= Unrestricted), and the app while it
  runs a foreground service (the active bucket).

**2. The phone maker's (on top of Android's).** dontkillmyapp.com ranks Huawei, Xiaomi, OnePlus
and Samsung worst (5 of 5), then Oppo, Meizu, Asus (4), Vivo, realme, Motorola, Lenovo (3).
Stock-like phones (Pixel, Nokia, the FP6) don't add a layer.
- **Xiaomi / Redmi / POCO** (MIUI, HyperOS): with **Autostart** off, a stopped app isn't started
  again by alarms, jobs or boot; plus Battery saver per app ("No restrictions") and locking in
  Recents. No public API; a library (MIUI-autostart) can read the Autostart state.
- **Huawei / Honor** (EMUI, MagicOS): "App launch" → "Manage manually" with all three switches;
  PowerGenie (EMUI 9+) kills apps not on its own list, and no user setting stops it.
- **Samsung** (One UI): "Sleeping apps" and "Deep sleeping apps" after about 3 days unused (no
  alarms then); "Never sleeping apps" and "Unrestricted". Samsung says foreground services that
  follow Android 14's rules work since One UI 6.0.
- **Oppo / realme / OnePlus** (ColorOS, OxygenOS): "Allow background activity", "Auto launch".
- **Vivo / iQOO**: "High background power consumption", Autostart.

The "Unrestricted" row is often there under the maker's name ("No restrictions" on Xiaomi), but
the maker's Autostart switch stops everything the app schedules. **No app-side setting gets past
layer 2** while the app's process is dead; only the user's settings do, or a process the system
keeps alive (a running foreground service, the current live wallpaper).

## The plan

### B. "Exact timing" (Advanced settings)

A switch in "Advanced settings" (acts at once, as "Save every picture"), default off. One
sentence on the page (placeholder): "Changes the wallpaper on time even when the phone saves
battery."
- **On:** if `AlarmManager.canScheduleExactAlarms()` is false (Android 14+ by default; 12–13
  grant it at install), open `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` (Android's "Alarms &
  reminders" for the app); the switch is on only once it's granted (checked at `onResume` and by
  `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`). Below Android 12 nothing to grant.
  Only this switch asks; nothing is requested while it's off.
- **The alarm:** `setExactAndAllowWhileIdle(RTC_WAKEUP, T)` at each next run (`nextAlarm`
  unchanged), besides J2's pair (kept as the fallback when the grant is revoked).
- **The run:** the receiver starts a **foreground service of type `dataSync`** (an exact alarm
  is one of Android 12+'s exemptions for starting one from the background) that runs
  `Store.update` as the job does, then stops itself. Why: in the foreground service the app is
  in the active bucket and should keep the network in Doze (music apps stream in Doze; Android's
  docs don't say it: **check first**, below). Android 15+ allows `dataSync` 6 h in 24 h (a run
  takes seconds to a minute; `onTimeout` stops it) and not from `BOOT_COMPLETED` (boot only arms
  the alarms).
- **The notification** (required for a foreground service): a quiet channel, "Getting the new
  picture", for as long as the run takes. **The app never asks for the notification
  permission** (user, 2026-10-25): on Android 13+ without it the run still works and the
  notification shows only in Android's task manager; on 10–12 it shows for those seconds.
- **Permissions** (user approved, 2026-10-25): `SCHEDULE_EXACT_ALARM` (special access, granted
  by the user once, from the switch), `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_DATA_SYNC`
  (normal, granted at install, no prompt; inert while the switch is off). Not
  `USE_EXACT_ALARM` (meant for alarm-clock and calendar apps).
- **The pause hint:** with "Exact timing" on and granted, the battery line's " (on Optimized, a
  new picture can come hours late)" goes, if the check confirms it.
- **Doesn't help** against layer 2 (Autostart off etc.): the process isn't started. The detector
  says so (below).

### The detector: the page notices that the job didn't run

(User, 2026-10-25: when the page has to do the job's work, point to the setting that fixes it.)
- **A miss:** a page open's `setDue` sets a screen due by a change time T (its stamp), T + the
  offset + 30 min has passed, and **no job run started since T** (`lastRunAt` < T). Not a miss
  when the phone booted after T (`System.currentTimeMillis() - SystemClock.elapsedRealtime()`),
  the app was updated after T (`PackageInfo.lastUpdateTime`), or the daily switch was turned on
  after T (it notes `lastRunAt`). A run that started and failed (no network, a 503) isn't a
  miss: the problem box covers it.
- **Shown at the first miss** (user, 2026-10-25), only while the daily switch is on: a neutral
  box with the others (problem, video notice, battery hint). "Dismiss" hides it until the next
  miss. Texts are placeholders for the user:
  - "Exact timing" off: "Your phone held back the 06:00 change; it came when you opened the
    app." Buttons "Exact timing" (opens Advanced settings at the switch) and "Dismiss".
  - "Exact timing" on (layer 2, user, 2026-10-25): "Your phone still stops APODroid in the
    background." and one line for the maker (`Build.MANUFACTURER`): Xiaomi/Redmi/POCO "In App
    info, allow Autostart and set Battery saver to No restrictions.", Samsung "In Battery, add
    APODroid to Never sleeping apps.", Oppo/realme/OnePlus "In App info, allow background
    activity and auto launch.", Vivo/iQOO "In App info, allow high background power use and
    Autostart.", Huawei/Honor "In Battery, App launch, set APODroid to Manage manually with all
    switches on.", others "In App info, allow APODroid to run in the background." The paths are
    from dontkillmyapp.com; check them on real phones where possible. Buttons "App info" and
    "Dismiss".
- The pref: `missedAt` (T of the last miss) and the dismissed one; nothing else stored.

### D. Live (0.7.0's headline) checks when the screen turns on

The current live wallpaper's service is bound by the system, so makers rarely kill it, and the
screen being on means no Doze. In the engine's `onVisibilityChanged(true)`: when a check is
owed (`:core`'s passed check since `lastRunAt`, as `nextAlarm` knows it) and none ran in the last
15 min, run `Store.update` in the process. So with Live a phone whose maker blocks the job still
gets the picture when the user first looks at it. To decide with Live's plan: whether the
detector then recommends Live.

## Checks before building (J0's way, on the tablet, then the FP6)

1. With the grant: `setExactAndAllowWhileIdle` in deep Doze (`force-idle`), on battery,
   Optimized, in buckets `working_set`, `rare` and `restricted`: on time? (RARE's quota, V11a.)
2. From that alarm, `startForegroundService` of a `dataSync` service on Android 15 (tablet) and
   16 (FP6): allowed, no `ForegroundServiceStartNotAllowedException`.
3. In that service, in deep Doze, Optimized: does the fetch have the network? (The deciding
   question: without it, B only fixes the timing, not Optimized's hours.)
4. Without the notification permission (Android 13+): the run works, nothing in the shade.
5. A Recents swipe and `am force-stop`: what survives (the exact alarm after a swipe; nothing
   after a force stop, as now).
6. The detector: alarms cancelled (`am force-stop` overnight), a page open after the change
   time: the box; a boot after the change time: no box.
7. If the friend's phone can be lent (or adb over Wi-Fi): Autostart off, then on, with "Exact
   timing" on.

## F-Droid and Obtainium

- F-Droid builds from source and has no policy against these permissions; none of them is an
  anti-feature (anti-features are tracking, ads, non-free parts and the like). No change to
  fdroiddata, reproducible builds unaffected.
- The F-Droid client and Obtainium list the new permissions at install and on the update (as
  "new permissions" in the F-Droid client); normal and special-access permissions don't need a
  prompt at update. Say it in 0.7.0's changelog: "Exact timing (optional) needs the alarms
  permission".
- Google Play's rules on `USE_EXACT_ALARM` and `specialUse` don't apply, and neither is used.

## Rejected

- **A permanent foreground service with a permanent notification** (C): the strongest hack on
  makers' phones, but a notification for good, more memory, and Android 14+ has no fitting type
  but `specialUse` (user, 2026-10-25: B and D instead).
- `setAlarmClock`: exempt too, but it shows an alarm icon and puts the app's time in the
  system's "next alarm" (the clock, the lock screen) every day.
- `USE_EXACT_ALARM`: for alarm-clock and calendar apps only.
- Opening the makers' hidden settings screens by class name (e.g. MIUI's Autostart activity):
  they change between versions; "App info" plus the text is enough.

Sources: developer.android.com (Restrictions on starting a foreground service from the
background; Schedule exact alarms are denied by default, Android 14; Foreground service types;
Foreground service timeouts; App Standby buckets); dontkillmyapp.com (ranking, Xiaomi, Samsung,
Huawei).
