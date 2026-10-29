# On time on any phone (0.7.0)

(User, 2026-10-25, after a friend's Chinese phone had no "Unrestricted" battery setting; planned
again 2026-10-28, after W's tablet day: [Decisions](#decisions-user-2026-10-28). The decisions go
into `CLAUDE.md`, Next features, 0.7.0: on time on any phone; the packages write the rest there
when they land. Built after 0.6.0 ships: 0.6.0 is in its code freeze until its tag.)

## What stops the job (investigated 2026-10-25 and 2026-10-28)

Two layers, and they need different fixes.

**1. Android's own (every phone).** Since J2 a pair of alarms starts each run (`setWindow` +
`setAndAllowWhileIdle`; the first that fires schedules job 2) and the 6 h job is the fallback.
- Battery **Unrestricted**: on time (J0, V11a).
- **Optimized**: the alarm fires on time (the pair at T + 10 min in Doze, J0), but in Doze the
  run (job 2, a regular job) has no network until the next maintenance window, so it can be
  hours late (J0: up to 2½ h). The pause hint says so.
- **The pair's kind** (AOSP `AlarmManagerService`, 12, 15 and main): for an app without the
  exact-alarm permission `setAndAllowWhileIdle` is the "compat" kind
  (`FLAG_ALLOW_WHILE_IDLE_COMPAT`; `flags=0xa0` in V11a's `dumpsys alarm`): at most 7 an hour in
  Doze, a 10 s temporary allowlist that may not start a foreground service (`mOptsWithoutFgs`),
  and it counts against the bucket's alarm quota.
- **Standby buckets** (on battery; `AlarmManagerService`'s defaults): ACTIVE 720 alarms an hour,
  WORKING_SET 10, FREQUENT 2, RARE 1 (V11a: held 57 min), RESTRICTED **one a day** (Android 13+,
  about 8 days without opening the app), and one 10-min job session a day there, also while
  charging (developer.android.com, App Standby buckets). Opening the app lifts it.
- Not limited by buckets: Unrestricted apps (their alarms are `FLAG_ALLOW_WHILE_IDLE_UNRESTRICTED`,
  bucket EXEMPTED), and an exact `setExactAndAllowWhileIdle` with `SCHEDULE_EXACT_ALARM`
  (`FLAG_ALLOW_WHILE_IDLE`, which `isExemptFromAppStandby` exempts, RESTRICTED's one a day too;
  the App Standby page says "one alarm per day, either exact or inexact": unchecked, moot since
  B is dropped).
- **An expedited job runs in Doze and battery saver, with the network** (2026-10-28; Android 12+,
  `JobInfo.Builder.setExpedited(true)`; no permission, no notification). AOSP 12, 15 and main:
  `JobStatus.canRunInDoze()` and `canRunInBatterySaver()` let it run there;
  `ConnectivityController` lets it past the Doze, battery saver, app standby and background
  network blocks (`UNBYPASSABLE_EJ_BLOCKED_REASONS`); `JobServiceContext` binds it with
  `BIND_BYPASS_POWER_NETWORK_RESTRICTIONS` and holds a wake lock for it. Its quota
  (`QuotaController`, per 24 h): 30 min in ACTIVE and WORKING_SET, 10 in FREQUENT and RARE, 5 in
  RESTRICTED; within 3 min of an alarm's temporary allowlist it counts as within quota (not in
  RESTRICTED). Out of quota `schedule()` returns `RESULT_FAILURE` (`JobSchedulerService`). It is
  guaranteed 3 min (`RUNTIME_MIN_EJ_GUARANTEE_MS`), up to 10 within quota, and may be stopped
  sooner than a regular job. Google's alarm guide says the same: "To perform work while the
  device is in Doze, create an inexact alarm using `setAndAllowWhileIdle()`, and start a job from
  the alarm", naming an expedited job as the way. Android 10–11 have no expedited jobs. Android
  16 counts jobs that run beside a foreground service against the runtime quota (behaviour
  changes, all apps; seconds for our runs).
- **A foreground service** (2026-10-28): its process has the network in Doze and battery saver
  (`NetworkPolicyManager.isProcStateAllowedWhileIdleOrPowerSaveMode`: process state up to
  FOREGROUND_SERVICE) and an oom adj of about 200; it holds no wake lock by itself, and JobScheduler
  still holds a regular job in Doze (`DeviceIdleJobsController` exempts only allowlisted apps).
  It doesn't keep the app in a better bucket: `AppStandbyController` raises the bucket at a
  foreground service's start only on the app's first use ever (`if (prevBucket !=
  STANDBY_BUCKET_NEVER) return;`), though the App Standby page says a long-running one "places an
  app in the Active bucket" (unchecked: O7). So a permanent service keeps the process alive; it
  doesn't fix the buckets' alarm quota or a regular job in Doze.

**2. The phone maker's (on top of Android's).** dontkillmyapp.com ranks Huawei, Xiaomi, OnePlus
and Samsung worst (5 of 5), then Oppo, Meizu, Asus (4), Vivo, realme, Motorola, Lenovo (3).
Stock-like phones (Pixel, Nokia, the FP6) don't add a layer.
**The friend's phone** (2026-10-25, from its About phone): Honor X60, model BRC-AN00 (the China
model), MagicOS 9.0, Android 15, APODroid in use. App info has no "Unrestricted" row (the
user's report): the pause hint's battery line names a path that isn't there.
- **Xiaomi / Redmi / POCO** (MIUI, HyperOS): with **Autostart** off, a stopped app isn't started
  again by alarms, jobs or boot; plus Battery saver per app ("No restrictions") and locking in
  Recents (a locked app "will … not be cleared from the background, even if you clear recent
  apps"). No public API; a library (MIUI-autostart) can read the Autostart state. Whether a
  foreground service survives MIUI's killing: dontkillmyapp doesn't say.
- **Huawei / Honor** (EMUI, MagicOS): "App launch" → "Manage manually" with all three switches;
  PowerGenie (EMUI 9+) kills apps not on its own list, and no user setting stops it; whether it
  spares a foreground service: unknown. On MagicOS (Honor since the split from Huawei): Settings →
  Apps → Apps → the app → Battery usage details → App launch settings (or Settings → Battery, the
  app), "Manage automatically" off, then "Auto-launch", "Secondary launch" and "Run in background"
  on ("Run in background" stops the freeze with the screen off). Android's Doze allowlist is
  there as Settings → search "Battery optimisation" → "All apps" → the app → "Don't allow": the
  same exemption as "Unrestricted", so `isIgnoringBatteryOptimizations` sees it
  (sportstracklive.com's Honor guide; MagicOS 9 itself unchecked).
- **Samsung** (One UI): "Sleeping apps" and "Deep sleeping apps" after about 3 days unused (no
  alarms then); "Never sleeping apps" and "Unrestricted". Samsung's developer page: since One UI
  6.0 foreground services of apps targeting Android 14 "will be guaranteed to work as intended"
  if they follow Android 14's rules (a type and its permission).
- **Oppo / realme / OnePlus** (ColorOS, OxygenOS): "Allow background activity", "Auto launch".
- **Vivo / iQOO**: "High background power consumption", Autostart.
- **Lenovo** (ZUI, the test tablet TB305FU, Android 15; seen twice, V12d, 2026-10-25, and W, 2026-10-28):
  `ZuiMemoryCleaner[Aggressive-MemFreeCritical]` force-stopped the cached app (with F-Droid,
  the permission controller and others) 2 s after a clock step, which deleted both alarms and
  job 1 until the next page open; not seen in V11d's identical step. No 0.6.0 change (user,
  2026-10-26). Not seen in V7's real night on battery (26–27 Oct: no exit in `exit-info`).
  **On a real day (W, Wed 28 Oct 13:57, no clock step):** the same cleaner, `MemFreeCritical`,
  the tablet up 34 h with a load of about 13, stopped the app with F-Droid, the calendar provider
  and Lenovo's and Google's own services; the page had been in the background since the day
  before. The app was Unrestricted and in the EXEMPTED bucket: neither protects against it.
  Nothing ran from 13:57 until the page opened at 19:40 (NASA's 16:35 post missed the lock
  screen); the page open re-armed and set it. A force stop also deletes an exact alarm.
  `.coord/runs/w/report.md`.
  **Whom it stops** (`.coord/runs/w/tab-wed-all.txt`, read 2026-10-28): each sweep logs
  `updateStrategy use: Moderate|Aggressive …, minAdj: 701`, then `forcestop app: <package>` for
  processes of adj 915 to 999 that day (19 of them; the app at 930); when none is left: `Clean: No
  pkg can be kill with whitelist`, `Fail to get normal app to kill, check whitelist app`, `Clean:
  No pkg can be kill without whitelist`, `No app to kill, give up`. So **minAdj 701 is the floor**
  (nothing below it is touched: a foreground service at about 200, the previous app at 700,
  likely a live wallpaper) and the **whitelist only orders the kills** (whitelisted apps are checked when
  no other is left), it doesn't protect. Its whitelist provider
  (`com.lenovo.performancecenter.provider.querywhitelist`) isn't on the tablet (`Failed to find
  provider info` at each sweep), so the list is built in; whether a lock in Recents or a ZUI
  setting adds to it: unknown (O8). Lenovo's pages call the feature SuperMemCleaner (Linux PSI);
  dontkillmyapp has only a Lenovo P2 entry: the padlock in Recents makes an app "run as a
  foreground process".
- **Android's battery saver** (V12e, 2026-10-27, the tablet): it blocks an Optimized app's
  background network (netpolicy `BLOCKED`), so the alarm's run (id 2) waits, pending, until saver
  ends or the page opens; after saver went off Android still held the ready run back about 4 min
  (batching of apps outside the ACTIVE bucket). An Unrestricted app's run set at once with saver
  on. An expedited job gets past saver's block (above).

The "Unrestricted" row is often there under the maker's name ("No restrictions" on Xiaomi), but
the maker's Autostart switch stops everything the app schedules. **No app-side setting gets past
layer 2** while the app's process is dead; only the user's settings do, or a process the system
keeps alive (a running foreground service, the current live wallpaper). A force stop (the user's,
or a cleaner's) deletes the alarms and job 1: nothing starts the app again until the page opens,
a boot or an update.

### What covers what (2026-10-28)

| | Optimized in Doze or saver | RARE: 1 alarm an hour | RESTRICTED: 1 alarm a day | A cleaner stops the cached app (ZUI) | Autostart off, the process dead | Costs the user |
|---|---|---|---|---|---|---|
| Unrestricted (the pause hint) | yes | yes | yes | no (W: Unrestricted, EXEMPTED) | no | a setting in App info, the maker's path |
| The expedited run (O10) | yes, 12+, while the app is opened now and then | no | no | no | no | nothing |
| Keep running (C, O11) | the network, not the run | no | no | likely (adj ~200 < 701; O8) | likely (the process lives) | 2 install-time permissions, an active app (13+), a notification (10–12), memory |
| Exact timing (B, dropped) | the timing; the run needed its own service | yes | yes (AOSP) | no | no | special access, a setting |
| Live (D, track L) | at screen-on | — | — | likely while it doesn't force-stop (then Android drops Live to the default wallpaper; L1) | yes (the system starts it) | Live chosen in Android's picker |
| The detector (O14) | — (the pause hint) | — (the pause hint) | — (the pause hint) | tells: the alarms found gone | — (the alarms stay) | a box |

None of them gets past a force stop by the user. The expedited run lasts only until the
bucket changes: about 8 days after the last open an Optimized app is in RESTRICTED (Android
13+), its alarm comes once a day, and a set-and-forget wallpaper app is rarely opened. So
Unrestricted stays the real fix for Optimized phones, and the pause hint keeps saying why.

## Decisions (user, 2026-10-28)

After W's tablet day (the user, on seeing it: "we have to find a solution, sadly it will lead to
a persistent notification") and the investigation above:
1. **Keep running** (C, rejected on 25 Oct, now in; the user chose "Setting, default off"): a
   permanent foreground service, as a switch in Advanced settings, default off, acting at once;
   the detector's box offers it at the first missed change. Why: the cleaner stops only cached
   processes, and a foreground service's isn't cached. Not always on: every phone would pay the
   memory and the Active apps entry, also stock phones that don't need it. Not "no C": ZUI's
   cleaner would keep stopping the app. **Permissions** approved with it: `FOREGROUND_SERVICE`
   and `FOREGROUND_SERVICE_SPECIAL_USE` (normal, at install, no prompt; inert while it's off).
   The app still never asks for the notification permission (25 Oct).
2. **The expedited run replaces B** (the user chose "Expedited job, drop B"): the alarms' run
   (job 2) becomes an expedited job on Android 12+, if O6 confirms on the tablet that it gets
   the network in deep Doze as Optimized. "Exact timing", `SCHEDULE_EXACT_ALARM`,
   `FOREGROUND_SERVICE_DATA_SYNC` and the `dataSync` service per run are dropped. The buckets
   (RARE's hourly alarm, RESTRICTED's daily one) stay the pause hint's job: Unrestricted exempts
   the app. If O6 fails, B comes back to the user.
3. **Texts** (the user chose "Plain"; placeholders until approved from the tablet's
   screenshots): Advanced settings "Keep running", a switch with an ⓘ like "Save every
   picture"'s, whose dialog says "Keeps APODroid running, so the phone can't stop it between
   changes. It shows as an active app." (OK); the notification channel "Keep running", the
   notification the app's name and "Keeps the wallpaper changing on time"; a tap opens the page.
4. The detector offers "Keep running" where it offered "Exact timing" (from 1 and 2).
5. **Four more** (the user, the same day, each the recommended choice):
   - A large save stopped by the expedited job's shorter limit: **accepted** ("Accept it"): one
     job as now, a stop during the saves ends as B7 says. Not a separate job for the saves (more
     code in the job, where the bookkeeping's bugs came from).
   - Keep running is offered **only at a miss** ("Only at a miss"): no line in the pause hint by
     maker before one.
   - **O8's go is given** ("Yes, go ahead"): rosie runs it when its turn comes and names every app
     the cleaner stopped, for the user to reopen.
   - **E stays open** ("No, leave E open"): Android 10–14 ship untested for this track too.
6. **After a review of this plan** (the user, 2026-10-28 evening, each the recommended choice):
   - **The detector offers Keep running only on a force stop** ("Only on a force stop"): its
     miss also needs the alarms found gone (a force stop deletes them; a bucket that only holds
     the alarm doesn't, and Keep running wouldn't help there). Without that sign no box: the
     pause hint already covers the buckets.
   - **The pause hint's text stays** ("Keep today's text"): the expedited run helps only while the
     app is opened now and then (RESTRICTED after about 8 days).
   - **O16 only if needed** ("Skip if O8 is clear"): the week on the tablet runs only when O8's
     sweep is unclear or O15 finds something; track L needs the tablet too.
   - "Keep running" names only the new switch: `CLAUDE.md`'s section of 4 Oct on the pause hint is
     now "The pause hint". O11 waits for O4, O7 and O8, not for O10 (independent).

## The plan

### First, before the job changes again (2026-10-26)

From the report on 0.6.0's state (user, 2026-10-26): the job and the rules gave 13 of 0.6.0's
24 fix packages, and B16, B18, B19 and B24 all patched the run's bookkeeping (`runGoing`,
`lastRunAt`, `failedRuns`, the safety alarm). Before the expedited run and Keep running change
the job:
1. `declutter.md`'s code package C1–C12 (after 0.6.0 ships): O1–O3.
2. A design review of the run's bookkeeping by reading, CR's form: is there a simpler model
   that keeps every case B16–B24 fixed and fits the changes below? A finding is a package, not
   a change in passing: O4.
3. SIM (0.6.0's random test of the rules) stays the guard: every package that touches
   `Screens.kt`, `PictureTime.kt` or the run's bookkeeping runs it, and adds to its invariants
   what it changes.

### The expedited run (O10; was B)

- `DailyJob.runNow`: job 2 with `setExpedited(true)` on Android 12+ (its constraints allow it:
  any network, no delay, not periodic). Out of quota `schedule()` returns `RESULT_FAILURE`: job
  2 is then scheduled as now, a regular job. Android 10–11: as now.
- Scheduled in the alarm's temporary allowlist it counts as within quota, and a run takes
  seconds: RARE's 10 min a day is far more than the day's runs need.
- An expedited job is guaranteed 3 min (10 within quota) and may be stopped sooner than a
  regular one: a run that downloads a large original for "Save every picture" may be stopped
  during its saves. It ends as a stop does today (B7: counted as failed, the alarms retry 15 min
  later, the next job save of that post is the smaller copy). Accepted (user, 2026-10-28).
- **What it fixes, and for how long:** an Optimized phone in Doze or battery saver gets the run
  with the network on Android 12+ **while the app is opened now and then**. About 8 days after
  the last open Android 13+ puts it in RESTRICTED: one alarm a day, so the picture can be up to
  a day late, expedited or not; RARE's hourly alarm stays too. Unrestricted fixes both.
- **The pause hint's text stays** (user, 2026-10-28, "Keep today's text"): " (on Optimized, a
  new picture can come hours late)" is still its only reason for Unrestricted and still true
  from day 8. `CLAUDE.md`'s "Optimized phones get the picture late, accepted" stays, with the
  expedited run noted for an app opened now and then.

### Keep running (O11; was C)

- **The setting:** the pref `keepRunning`, default off; in Advanced settings while the daily
  switch is on, after "Save every picture"'s group; acts at once (not part of the draft), with
  an ⓘ as "Save every picture"'s (the texts: Decisions, 3).
- **The service** `KeepRunning`: `foregroundServiceType="specialUse"` with the
  `android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE` property (a free-form sentence; only Google
  Play's review reads it), not exported, `START_STICKY`. It does nothing but be a foreground
  service: no thread, no wake lock, no network. The runs stay the alarms' (job 2, expedited) and
  job 1's, in the same process: the process is alive, so the cleaner passes it over and its
  alarms stay.
- **The notification:** channel "Keep running", `IMPORTANCE_LOW` (a foreground service's
  notification must be `PRIORITY_LOW` or higher, else the system adds its own notice), silent,
  ongoing, a small icon from the logo's monochrome layer, a tap opens the page. On Android 13+
  without the notification permission nothing shows in the shade, only APODroid in Quick
  Settings' Active apps, with a Stop button (developer.android.com, Notification runtime
  permission); on 14+ a shown one can be swiped away and the service runs on (O7); on 10–12 it
  shows, and the user can turn its channel off.
- **Started** (`KeepRunning.ensure`, while `daily` and `keepRunning`): the switch turned on and
  `onResume` (the page in the foreground); the daily switch on; `BOOT_COMPLETED` and
  `MY_PACKAGE_REPLACED` (both exempt from Android 12's background-start limit, and Android 15's
  `BOOT_COMPLETED` limit names `dataSync`, `camera`, `mediaPlayback`, `phoneCall`,
  `mediaProjection`, `microphone`, not `specialUse`); each run's start in `DailyJob` (allowed only
  while the app is Unrestricted, "The user turns off battery optimizations for your app" being an
  exemption; refused otherwise, caught). **Stopped:** the switch off; the daily switch off
  (`DailyJob.cancel`).
- **Refused** (`ForegroundServiceStartNotAllowedException`, Android 12+; also possible on a
  sticky restart, as Thunderbird found): caught, logged, `stopSelf`; the next page open, boot or
  update starts it again. The page shows nothing for it.
- **Task Manager's Stop** (Android 13+): "your entire app stops", jobs and alarms stay
  (developer.android.com, Handle user-initiated stopping); the next page open starts it again (the
  switch is the user's say).
- **A force stop** (the user's, or a cleaner that ignores the service): as today; the detector.
- **Memory:** the process stays resident. O7 measures it with the page used and in the
  background; if the page's decoded pictures (up to three) stay with it, O12 lets them go while
  the page is stopped.

### The detector (O14): the page notices that the job didn't run

(User, 2026-10-25: when the page has to do the job's work, point to the setting that fixes it.)
- **A miss:** a page open's `setDue` sets a screen due by a change time T (its new stamp), T + the
  offset + 30 min has passed, and **no job run started since T** (`lastRunAt` < T). Not a miss
  when the phone booted after T (`System.currentTimeMillis() - SystemClock.elapsedRealtime()`),
  the app was updated after T (`PackageInfo.lastUpdateTime`), the daily switch was turned on
  after T (it notes `lastRunAt`), or **job 2 is pending** (2026-10-28: the alarm came and the run
  waits for the network or its quota, Android's layer, not a stopped app). A run that started and
  failed (no network, a 503) isn't a miss: the problem box covers it. A missed run with nothing to
  set shows nothing (nothing was lost).
- **And the alarms were found gone** (user, 2026-10-28 evening, "Only on a force stop"): a bucket
  can hold the alarm past T + 30 min too (RARE: 57 min in V11a), and Keep running doesn't help
  there. The sign: whoever arms first in a process (`DailyJob.ensure`, from the page's
  `onResume` or a receiver) asks for the pair's `PendingIntent` with `FLAG_NO_CREATE` before
  arming; null while `daily` is on means a force stop deleted it (a process death doesn't). A
  boot or an update explains a missing pair (their receivers find it gone too: the boot time or
  `lastUpdateTime` after `lastRunAt`); Android 15's `BOOT_COMPLETED` on leaving the stopped state
  (seen in J2) isn't a boot (`elapsedRealtime` long) and doesn't explain it. The finding is noted
  (`alarmsLostAt`) and the miss needs it after T. O7 and O8 record whether a force stop (`am
  force-stop`, Task Manager's Stop, ZUI's cleaner) leaves this sign, and the exit reasons
  (`getHistoricalProcessExitReasons`) and Android 15's `ApplicationStartInfo.wasForceStopped()`
  beside it; if the `PendingIntent` sign fails, O14 uses the one that works.
- **Without the sign no box:** a bucket's delay is the pause hint's (shown while the app isn't
  Unrestricted).
- **Shown at the first miss** (user, 2026-10-25), only while the daily switch is on: a neutral
  box with the others (problem, video notice, battery hint). "Dismiss" hides it until the next
  miss. Texts are placeholders for the user:
  - "Keep running" off: "Your phone held back the 06:00 change; it came when you opened the
    app." Buttons "Keep running" (opens Advanced settings at the switch; doesn't turn it on) and
    "Dismiss".
  - "Keep running" on (layer 2 still, user, 2026-10-25): "Your phone still stops APODroid in the
    background." and one line for the maker (O13's table by `Build.MANUFACTURER`):
    Xiaomi/Redmi/POCO "In App info, allow Autostart and set Battery saver to No restrictions.",
    Samsung "In Battery, add APODroid to Never sleeping apps.", Oppo/realme/OnePlus "In App info,
    allow background activity and auto launch.", Vivo/iQOO "In App info, allow high background
    power use and Autostart.", Huawei/Honor "In App info, Battery usage details, App launch
    settings, turn off Manage automatically and turn on all three switches.", Lenovo what O8
    finds, others "In App info, allow APODroid to run in the background." The paths are from
    dontkillmyapp.com; check them on real phones where possible. Buttons "App info" and
    "Dismiss".
- The battery line of the pause hint stays the place for Unrestricted (RESTRICTED and RARE are
  its causes); the detector doesn't repeat it.
- The prefs: `alarmsLostAt`, `missedAt` (T of the last miss) and the dismissed one.

### The pause hint per maker (O13)

(User, 2026-10-25, after the Honor X60: in 0.7.0, not as a 0.6.0 fix.) The detection is right
everywhere (`isIgnoringBatteryOptimizations`, which a maker's "Battery optimisation" list sets
too); the battery line's text is the Pixel's path ("App battery usage → Allow background usage
→ Unrestricted"). Planned:
- The line names the phone's own path by `Build.MANUFACTURER`, as the detector's texts (Honor:
  "Battery optimisation" → "Don't allow"; with the App launch line from the detector), the
  Pixel path on stock-like phones. The table of makers is one `:core` function both use.
- Beside "App info" a button that opens Android's Battery optimisation list
  (`Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`; no permission, unlike
  `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, which stays rejected), where App info has no
  Unrestricted row or always (to decide with the texts; the night's default: always).
- The texts are the user's to approve, from screenshots; the paths checked on the friend's
  Honor where possible (no adb there: by the friend's screenshots).

### D. Live (0.7.0's headline, track L) checks when the screen turns on

The current live wallpaper's service is bound by the system (AOSP 15 `WallpaperManagerService`:
`BIND_AUTO_CREATE | BIND_SHOWING_UI | BIND_FOREGROUND_SERVICE_WHILE_AWAKE | …`, rebound after a
low-memory kill), so makers rarely kill it, and the screen being on means no Doze. In the
engine's `onVisibilityChanged(true)`: when a check is owed (`:core`'s passed check since
`lastRunAt`, as `nextAlarm` knows it) and none ran in the last 15 min, run `Store.update` in the
process. So with Live a phone whose maker blocks the job still gets the picture when the user
first looks at it. The seams: [Seams with track L](#seams-with-track-l).

## Checks before building (2026-10-28; J0's way)

What only a device can tell, each a package below; a check whose answer changes the design comes
before the package it decides. One throwaway probe app (O5) serves all of them.
- **The expedited run in Doze** (O6, the tablet; decides O10, and whether B comes back): an
  Optimized probe's alarm pair → an expedited job vs a regular one, in forced deep Doze in
  WORKING_SET, RARE and RESTRICTED, under battery saver, and over a real night.
- **A foreground service's life and look** (O7, the tablet; decides O11's starts and O12): what
  the user sees (with and without the notification permission), a Recents swipe, `kill -9` (may
  a sticky restart go foreground?), Task Manager's Stop, a force stop, the starts from the
  background, boot and update, the bucket with the service running, the memory.
- **ZUI's cleaner against a foreground service** (O8, the tablet; it force-stops other apps,
  the user's go given 2026-10-28): memory pressure from a probe; the service spared, a cached app
  stopped? And what ZUI's settings and Recents offer.
- **Android 16** (O9, the FP6, with the user): O6's forced Doze and O7's look, briefly.
- **The friend's Honor X60** (O17, by the friend's screenshots): the pause hint's line, Keep
  running on, the status line's time after a morning.

## F-Droid and Obtainium

- F-Droid builds from source and has no policy against these permissions; none of them is an
  anti-feature (anti-features are tracking, ads, non-free parts and the like). No change to
  fdroiddata, reproducible builds unaffected.
- The F-Droid client and Obtainium list the new permissions at install and on the update (as
  "new permissions" in the F-Droid client); normal permissions need no prompt at update. Say it
  in 0.7.0's changelog: "Keep running (optional) needs the foreground service permissions".
- `specialUse`'s property and the Play Console's service-type declaration are Google Play's
  review only; neither applies here.

## Rejected

- **"Exact timing"** (B; user, 2026-10-28): an exact alarm (`SCHEDULE_EXACT_ALARM`, granted by
  the user) starting a `dataSync` foreground service per run. The expedited job gives the run the
  network in Doze without a permission, a setting or a notification; the exact alarm's only gain,
  escaping the buckets' alarm quota, Unrestricted already gives. `SCHEDULE_EXACT_ALARM` and
  `FOREGROUND_SERVICE_DATA_SYNC` (approved 25 Oct) aren't wanted any more.
- Keep running **always on** (user, 2026-10-28): every phone would pay, also those that don't
  need it. **No permanent service at all**: ZUI's cleaner would keep stopping the app.
- Other types for the permanent service: `dataSync` (6 h in 24 on Android 15+, and not from
  `BOOT_COMPLETED`), `systemExempted` (for apps holding an exact-alarm permission that continue
  alarms, VPNs, device admins: not this), `shortService` (about 3 min, no sticky).
- A separate job for "Save every picture"'s downloads (user, 2026-10-28): more code in the job;
  a save stopped by the expedited job's limit is accepted. Offering Keep running by maker before
  any miss (user, 2026-10-28): the detector offers it at the first miss.
- Runs inside the service (its own thread and the `WAKE_LOCK` permission): the expedited job
  already has the network in Doze and JobScheduler's wake lock.
- A second process for the service (`android:process`): the prefs, the locks and `JobRuns` are
  per process; one process stays.
- `setAlarmClock`: exempt too, but it shows an alarm icon and puts the app's time in the
  system's "next alarm" (the clock, the lock screen) every day.
- `USE_EXACT_ALARM`: for alarm-clock and calendar apps only.
- Opening the makers' hidden settings screens by class name (e.g. MIUI's Autostart activity):
  they change between versions; "App info" plus the text is enough.
- `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (a dialog that needs a permission): the pause
  hint and the Battery optimisation list instead.

Sources: developer.android.com (Restrictions on starting a foreground service from the
background; Foreground service types; Android 15's behaviour changes: the `dataSync` timeout and
`BOOT_COMPLETED`; Android 14's: dismissible notifications, exact alarms denied by default;
Android 16's: the job runtime quota; Handle
user-initiated stopping of foreground services; Notification runtime permission; Schedule
alarms; App Standby buckets); AOSP frameworks/base, read 2026-10-28 at the tags android-12.0.0_r1
and android-15.0.0_r1 and on main: `apex/jobscheduler/service/java/com/android/server/`
`alarm/AlarmManagerService.java` (`isExemptFromAppStandby`, the quotas, the flags in `set`),
`job/controllers/JobStatus.java` (`canRunInDoze`, `canRunInBatterySaver`),
`job/controllers/ConnectivityController.java` (`UNBYPASSABLE_EJ_BLOCKED_REASONS`),
`job/controllers/DeviceIdleJobsController.java`, `job/controllers/QuotaController.java` (the EJ
limits), `job/JobServiceContext.java` (the bind flags, the wake lock), `job/JobSchedulerService.java`
(`RESULT_FAILURE` out of EJ quota, the runtime limits), `usage/AppStandbyController.java`
(`FOREGROUND_SERVICE_START`); `core/java/android/net/NetworkPolicyManager.java`;
`services/core/java/com/android/server/wallpaper/WallpaperManagerService.java` (15, the bind
flags); dontkillmyapp.com (ranking, Xiaomi, Samsung, Huawei, Lenovo); developer.samsung.com
(App management: One UI 6.0 and Android 14's foreground services); Thunderbird's commit 9401de2
(a sticky restart refused); lenovo.com's tablet software page (SuperMemCleaner); sportstracklive.com's
Honor guide (MagicOS paths).

## Open

- What puts an app on ZUI's whitelist, and whether a lock in Recents spares a cached app (O8).
- Whether a sticky restart may go foreground on 15 and 16 (O7, O9).
- Huawei's PowerGenie and Xiaomi without Autostart against a foreground service: no such device;
  dontkillmyapp says nothing. The detector's maker lines cover them.
- Android 10–14 with Keep running and the expedited run (no device; E, the emulator, stays the
  user's open call, 2026-10-28): the notification on 10–12, the expedited run on 12–14.
- Whether the "Battery optimisation" button shows on every phone or only where App info has no
  Unrestricted row: the user decides from O13's screenshots (the night's default: always).

## Packages

Built after 0.6.0's tag (Tue 3 Nov at the earliest), one at a time; the checks need the tablet
after W (to Mon 2 Nov) and the FP6 after the tag. Each package writes its decisions into
`CLAUDE.md` (the source of truth) when it lands; this file is the how. For the pipeline, the
queue rows go into `night.md` and the plan file becomes `plan-0.7.0.md` with track L's (Seams,
7). Rosie uses only the tablet; O9, O16 (if it runs) and O17 need the user (O8's go is given).

| WP | What | Model, effort | Who, device | After |
|----|------|---------------|-------------|-------|
| O1 | Declutter: warnings, duplicates, wrong comments (C1–C3, C6–C12) | Opus, medium | winston, tablet | — |
| O2 | Declutter C4: ⛶'s noted part looked up once | Opus, medium | winston, tablet | O1 |
| O3 | Declutter C5: one dropdown | Opus, medium | winston, tablet | O1 |
| O4 | Design review of the run's bookkeeping, by reading | Opus, high | winston, no device | O1 |
| O5 | The probe (by day, not the pipeline) | Opus, medium | a day session, no device | — |
| O6 | Android check: the expedited run in Doze, battery saver and the buckets | Opus, medium | rosie, tablet, two sessions | O5 |
| O7 | Android check: a foreground service's life, look and memory | Opus, medium | rosie, tablet | O5 |
| O8 | Android check: ZUI's cleaner against a foreground service | Opus, medium | rosie, tablet (the user's go given) | O7 |
| O9 | Android check on the FP6: the expedited run and the service on Android 16 | Opus, medium | a day session, **FP6, with the user** | O6, O7 |
| O10 | The alarms' run as an expedited job | Opus, medium | winston, tablet | O4 (and its packages), O6 |
| O11 | Keep running: the service, the setting, its starts | Opus, high | winston, tablet | O4, O7, O8 |
| O12 | The page lets its pictures go while stopped (only if O7 asks for it) | Opus, medium | winston, tablet | O11 |
| O13 | The pause hint per maker | Opus, medium | winston, tablet | O3 |
| O14 | The detector | Opus, high | winston, tablet | O11, O13 |
| O15 | Tablet pass of the track (debug and R8, the update from 0.6.0) | Opus, high | rosie, tablet | O10–O14 |
| O16 | A week on the tablet with Keep running (W's way), only if O8 is unclear or O15 finds something | Opus, medium | morning reads by hand, tablet, **the user** | O15 |
| O17 | Phone pass: the FP6 and the friend's Honor | Opus, high | a day session, **FP6, the user**; the friend's screenshots | O15 (O16 if it runs) |

```
scripts/pipeline.sh O1    # then O2, O3, O4 (the queue rows in night.md, once 0.7.0's queue starts)
claude --model opus --effort medium "Do O5 in .claude/skills/build-apodroid-android/on-time-0.7.0.md"
ANDROID_SERIAL=HA24BZ2Y claude --agent rosie --model opus --effort medium "Do O6's first session in .claude/skills/build-apodroid-android/on-time-0.7.0.md"
ANDROID_SERIAL=HA24BZ2Y claude --agent rosie --model opus --effort medium "Do O6's second session in .claude/skills/build-apodroid-android/on-time-0.7.0.md"    # bedtime, then the morning read
ANDROID_SERIAL=HA24BZ2Y claude --agent rosie --model opus --effort medium "Do O7 in .claude/skills/build-apodroid-android/on-time-0.7.0.md"
ANDROID_SERIAL=HA24BZ2Y claude --agent rosie --model opus --effort medium "Do O8 in .claude/skills/build-apodroid-android/on-time-0.7.0.md"    # the user's go given 2026-10-28
claude --model opus --effort medium "Do O9 in .claude/skills/build-apodroid-android/on-time-0.7.0.md"    # with the user, the FP6 attached
scripts/pipeline.sh O10   # then O11, O12 (if O7 asks), O13, O14, O15
claude --model opus --effort medium "Do O16's start in .claude/skills/build-apodroid-android/on-time-0.7.0.md"    # only if O8 was unclear or O15 found something; then "Do O16's morning read" each morning
claude --model opus --effort high "Do O17 in .claude/skills/build-apodroid-android/on-time-0.7.0.md"
```

### [ ] O1. Declutter: warnings, duplicates and wrong comments (C1–C3, C6–C12) (Opus, medium)

(`declutter.md`, Pass 2026-10-25, Code; every item with its evidence:
`.coord/reports/2026-10-25-declutter.md`, section 4.) No behaviour and no text changes. The line
numbers there are master 82df16a's; B22–B24 moved the code since: find each by its name.
- **C1** lint's `UseKtx` in `Store.kt`'s whole-picture drawing: `createBitmap(w, h)` from
  core-ktx (`ARGB_8888` by default: the same bitmap). **C2** `: Unit` on `DailyJob`'s `started`
  and `ended` (`noted` too if it warns): a `return` inside `= synchronized(lock) { … }` is a
  warning now, an error from Kotlin 2.5. **C3** `VideoNotice`'s stand-in cases one `when`, the
  strings byte-identical.
- **C6** the problem's words in one place (`Store.checkSite`, `Store.problem`,
  `MainActivity.problemKey`): a `Problem.key`; the stored words don't change (a dismissed
  problem would show again). **C7** `Store.columns` from `Setup.columns`; `hasList`'s
  `prefs.contains("home")` once. **C8** one `Setup.pictureTime`. **C9** the default 06:00, the
  0..23 check and US Eastern each once, in `:core`. **C10** the small repeats in `MainActivity.kt`
  (optional, each only where it makes the code shorter). **C11** the defaults no caller uses
  (`saveInUse(stopped)`, `nextAlarm(running)`, `Part(whole)`, `VideoDays`' `source` as a getter,
  `Interval.hours` as `Int`). **C12** the six wrong comments and the test name
  `ScreensTest`'s `…ChangedFitOrOwnPictureShows`; the names `tab`, `showTab`, `tabPicture` for the
  shown source (S6 replaced the tabs).
- **Files:** `Store.kt`, `DailyJob.kt`, `MainActivity.kt`, `Frame.kt`, `Migration.kt`,
  `PictureTime.kt`, `Post.kt`, `ScreensTest.kt`; the skill's Where things are where a name
  changes.
- **Tests:** the existing ones pass unchanged; C6 a test that each problem's key is the word
  0.6.0 stores (where `Problem` lives).
- **Tablet:** the smoke list; a faked video day with each stand-in case in APOD mode and with
  "More NASA pictures" on (C3: the notice's text the same as on 0.6.0's build: screenshots side
  by side); a problem dismissed on 0.6.0's debug build stays hidden after installing this one
  (C6); a whole picture set (C1, `dumpsys wallpaper`).
- **Done when:** built; lint without `UseKtx`, the compiler without C2's warnings; the tablet
  passes.

### [ ] O2. Declutter C4: ⛶'s noted part looked up once (Opus, medium)

(`declutter.md`, C4; risk medium: the wallpaper's set path.) `Store.cropPosition` and
`Store.isWhole` re-read what `Store.parts` reads, and `MainActivity` has `position`, `whole` and
four wrappers: one lookup of a source's `Part` for a post (date and picture URL), about 25 lines
less. No behaviour change.
- **Files:** `Store.kt`, `MainActivity.kt`; the skill's Where things are.
- **Tablet:** ⛶ moved, ✓, Set: the crop as before (`dumpsys wallpaper`'s hint against 0.6.0's for
  the same position); "Whole picture" on, Set; a faked new date drops the part (⛶ centred); the
  job's set uses the stored part (`wallpaperDate` faked back, `run -f 1`); APOD and NASA on
  different screens with different parts; the smoke list.
- **Done when:** built; the tablet passes.

### [ ] O3. Declutter C5: one dropdown (Opus, medium)

(`declutter.md`, C5.) One dropdown written four times in `MainActivity.kt` ("New picture at",
the list's row, "If APOD is a video", "Largest original"; the title's menu too if it fits): one
composable, about 35 lines less. Unchanged: the outlined button with a no-break space before
"▾", ✓ before the chosen entry, the highlight while the draft differs, the screen readers'
labels ("New picture at: 06:00", "NASA on: Home screen", …), greyed out where it is now.
- **Files:** `MainActivity.kt`.
- **Tablet:** each dropdown opened and a choice made (the label, ✓, the highlight, then Set);
  `uiautomator dump`'s labels the same as on 0.6.0's build; font scale 2.0 at the phone's width
  (`wm density 344`): no "▾" alone on a line; screenshots for the user; the smoke list.
- **Done when:** built; the tablet passes.

### [ ] O4. Design review of the run's bookkeeping, by reading (Opus, high)

(This file, First, 2; CR's form.) By reading, no device, no code changes. Read whole:
`DailyJob.kt`, `AlarmReceiver.kt`, `Store.kt`'s run bookkeeping (`runStarts`, `ranJob`,
`runRetry`, `nextAlarm`, `switchDaily`, `startAfresh`, `update`'s and `saveInUse`'s stops,
`commitBeforeStop`), `:core`'s `JobRuns`, `SwitchRuns`, `PictureTime.kt` (`nextRun`,
`nextAlarm`, `runRetry`), `MainActivity`'s scheduling calls and `switchOn`; `CLAUDE.md`'s
Background update; `plan-0.6.0.md`'s B5, B7, B12, B16, B18, B19 and B24 (the cases each fixed);
SIM's invariants (`RulesSimulationTest`).
- **The question:** is there a simpler model (fewer prefs, fewer paths that arm, fewer flags)
  that keeps every case B5–B24 fixed and fits what comes: job 2 expedited (O10: a schedule that
  can fail for quota and falls back; a run stopped sooner), Keep running's start at a run's start
  (O11), the detector reading `lastRunAt` and job 2's pending state (O14), Live's screen-on run
  (D, track L: is it a run for `lastRunAt`)?
- **Write** `.claude/skills/build-apodroid-android/review-0.7.0-runs.md`: a table of the cases
  (the case, what fixes it now: the pref, the path, the line); the findings in CR's form, worst
  first (severity, `file:line`, the failure: steps → what goes wrong, the fix in a line or two,
  bug or decision); then the simpler model if there is one: what it removes, how each case stays
  fixed, which SIM invariant guards it, and the packages it would take, each drafted as
  `### [ ] O4f<k>.` for the user's morning (they run before O10). Only what you verified; say how
  sure. Commit it, then the report commit.
- **The audit** checks the form and that the cited lines exist.
- **Done when:** every file read (listed in the report), the review committed.

### [ ] O5. The probe (by day, not the pipeline; Opus, medium)

J0's way: a throwaway app in `.coord/probe-o/` (gitignored), with its own
`settings.gradle.kts`, built with the repo's wrapper (`./gradlew -p .coord/probe-o`), plain
Android (no Compose, no library), `targetSdk` and `compileSdk` 37, `minSdk` 29, as the app. One
code, three installs by `-Pprobe=`: `io.github.buerlino.apodroid.probe.ej` (its alarms' job
expedited), `.reg` (regular, the control) and `.hog` (only its Hog screen is used).
- **`Arm`** (an activity with no window, so the probe leaves its stopped state): `am start -n
  <pkg>/io.github.buerlino.apodroid.probe.Arm --es at "HH:MM,…"` arms, for each local time T,
  the pair the app uses (`setWindow(RTC_WAKEUP, T, 10 min)` and `setAndAllowWhileIdle(RTC_WAKEUP,
  T)`, one request code each per T). The receiver logs `ALARM` and schedules a one-off job (no
  delay, any network; `setExpedited(true)` in `.ej`), logging `schedule()`'s result; on
  `RESULT_FAILURE` it schedules the job regular and says so.
- **The job** logs `JOB`: `isExpeditedJob()`, `isDeviceIdleMode`, `isPowerSaveMode`, the bucket
  (`UsageStatsManager.getAppStandbyBucket()`), the battery level and plugged state, the minutes
  since its alarm, and an HTTPS `HEAD https://science.nasa.gov/` (the code or the exception).
- **`Keep`**, the service as O11 will build it: `specialUse` with the property, `START_STICKY`,
  the channel "Keep running" (`IMPORTANCE_LOW`) and the plain text, `ServiceCompat.startForeground`
  in a try that logs the exception; logs `onCreate`, `onStartCommand` (a null intent: a sticky
  restart), `onTaskRemoved`, `onDestroy`. `KeepOn` and `KeepOff` (no window) start and stop it;
  `Main` (a window with the log's last lines) gives the probe a task in Recents. With the pref
  `keepTry` (`KeepOn --ez try true`) the alarm's receiver, the job and a receiver for
  `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED` each try to start `Keep` and log the outcome.
- **At each process start** it logs `getHistoricalProcessExitReasons` (the last three: reason,
  description, time), on Android 15 `ApplicationStartInfo.wasForceStopped()`, and whether the
  armed alarm's `PendingIntent` still exists (`FLAG_NO_CREATE`, before anything arms): how a
  force stop is labelled and which sign it leaves (O14's detector).
- **`Hog`** (`.hog`, a window, so it is the top app): allocates memory in 32 MB steps a second
  (`ByteBuffer.allocateDirect`, every page written), shows the total, frees it all when it
  finishes; `--ei maxMb`.
- **The log:** `files/log.txt`, a line each, local time with seconds; `adb shell run-as <pkg> cat
  files/log.txt`. Permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`,
  `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`; it never asks for notifications.
- **Done when:** `probe-ej.apk`, `probe-reg.apk`, `probe-hog.apk` built in `.coord/probe-o/`, and
  its `README.md` lists the commands; no device (the trial is O6's first step).

### [ ] O6. Android check: the expedited run in Doze, battery saver and the buckets (Opus, medium)

(Agent rosie, the tablet, after W; decides O10, and whether B comes back to the user.) J0's form:
two sessions, the probes `.ej` and `.reg`, both Optimized (not on `deviceidle whitelist`).
- **Session 1 (by day, about 45 min):** the tablet's state noted (night.md, For rosie). Install
  both. **Trial:** plugged, screen on, arm both 3 min ahead: `ALARM` and `JOB` with `http=200` in
  each, `.ej`'s `JOB` expedited. A probe bug: STOP (the probe is fixed by day). Then on battery
  (`cmd battery unplug`), the screen off (`input keyevent 223`), each case armed 4 min ahead and
  read after its T + 12 min:
  (a) `am set-standby-bucket <pkg> working_set`, `dumpsys deviceidle force-idle`;
  (b) `rare`, force-idle; (c) `restricted`, force-idle (its alarm may be held a day: note it);
  (d) `working_set`, battery saver (`cmd power set-mode 1`), not idle.
  Between cases `deviceidle unforce`, `cmd power set-mode 0`. After the cases `dumpsys
  jobscheduler`'s expedited quota lines for both probes.
- **Session 2 (a real night):** at bedtime both in `rare`, armed for about 2 h ahead, 04:00 and
  06:00; `logcat -G 16M`; the user unplugs the tablet. In the morning: both logs, `dumpsys
  deviceidle`, `dumpsys alarm` (the history), `dumpsys jobscheduler`, `dumpsys battery`.
- **Expected** (AOSP): `.ej`'s job within about a minute of its alarm with `http=200` in (a),
  (b), (d) and each night T; `.reg`'s waits for a maintenance window or saver's end (the
  control, as J0 and V12e saw). In (c) the alarm itself is held (one a day): note when it comes
  and whether `.ej`'s job then runs at once with the network. That case is the common one for an
  Optimized app not opened for a week: it bounds what O10 fixes (The expedited run).
- **Report** `.coord/runs/o6/report.md`: a table probe × case × T → the alarm's time, the job's,
  idle, the bucket, the network. Uninstall both probes; set the tablet back.
- **Verdict:** PASS when `.ej` ran with the network within about a minute of its alarm in (a),
  (b) and (d) and on the night; STOP otherwise (B comes back to the user). (c) is reported, not
  judged: how late RESTRICTED's alarm came, and the job after it.
- **Done when:** both sessions ran, the table and the verdict written, the probes uninstalled,
  the tablet set back.

### [ ] O7. Android check: a foreground service's life, look and memory (Opus, medium)

(Agent rosie, the tablet; decides O11's starts and whether O12 runs.) The probe `.reg`
(Optimized) unless said; screenshots into the run folder for the user. About 60 min; if time runs
short, 1–4 and 8 first.
1. **The look:** `am start -n <pkg>/.KeepOn`: `startForeground ok` in the log; `dumpsys activity
   services <pkg>`: `isForeground=true`, the type `0x40000000`; the oom adj (`cat
   /proc/<pid>/oom_score_adj`, expected about 200). Screenshots: the shade pulled down, Quick
   Settings with its Active apps entry and its dialog. Then `pm grant <pkg>
   android.permission.POST_NOTIFICATIONS`: the notification in the shade (screenshot), swiped away
   (Android 14+: dismissible?), the service still foreground; `pm revoke` after.
2. **A Recents swipe:** `Main` opened, Home, the probe's task swiped away in Recents: the service
   stays (`dumpsys`), `onTaskRemoved` logged.
3. **`kill -9`** (`run-as <pkg> kill -9 <pid>`): does it come back (a null `onStartCommand`), after
   how long, and may it go foreground (`startForeground ok` or the exception)?
4. **Task Manager's Stop** (`cmd activity stop-app <pkg>`) with an alarm armed 5 min ahead: the
   process gone, the alarm still fires (`ALARM`), the service doesn't come back by itself. Then
   **`am force-stop`**: the alarm gone too (`dumpsys alarm`). After each, open `Main`: the logged
   signs (the `PendingIntent`, the exit reason, `wasForceStopped`), for O14.
5. **Starts from the background** (`KeepOn --ez try true`, then `KeepOff`): from the alarm's
   receiver and from the job, Optimized (expected refused, the exception logged) and with `cmd
   deviceidle whitelist +<pkg>` (Unrestricted: expected allowed; the entry removed after); from
   `BOOT_COMPLETED` (reboot, `wm dismiss-keyguard`, wait for `sys.boot_completed`) and
   `MY_PACKAGE_REPLACED` (`install -r` the same APK).
6. **The bucket:** with the service running, `am get-standby-bucket` before and an hour after
   (expected: it decays as without it); `am set-standby-bucket <pkg> rare`, force-idle: does the
   service's process lift the bucket's alarm quota or Doze's hold on a regular job (expected: no
   to both; the network yes)?
7. **Survives what:** with the service running, the tablet on battery and the screen off for
   30 min: still foreground (`dumpsys`)?
8. **Memory, the real app** (0.6.0's or master's debug build, the daily switch on, "More NASA
   pictures" on): the page used (all three sources swiped), Home: `dumpsys meminfo
   io.github.buerlino.apodroid` (TOTAL PSS, Native Heap, Graphics); Back (the page destroyed):
   again; `am kill`, `run -f 1` (a bare process during a run, after it): again. And the probe's
   bare process with `Keep` alone.
- **Report:** a table per step; O12 runs when Home's PSS is more than 30 MB above Back's.
- **Verdict:** PASS when 1, 2 and 7 hold (foreground from the page, listed in Active apps, alive
  after a swipe and 30 min); STOP otherwise (the user decides whether Keep running is worth it).
- **Done when:** the steps ran, the report written, the probe uninstalled, the tablet set back.

### [ ] O8. Android check: ZUI's cleaner against a foreground service (Opus, medium)

(Agent rosie, the tablet, by day. The memory pressure makes the cleaner force-stop other apps
on the tablet, as on 28 Oct, and they lose their alarms until opened: the user's go given,
2026-10-28, "Yes, go ahead". Decides whether Keep running helps on ZUI.)
- **Read-only first:** Settings → Apps → APODroid (App info, Battery, anything named auto-start,
  background running or app management), Settings → Battery: screenshots; `pm list packages |
  grep -i -e lenovo -e zui`; long-press APODroid's card in Recents: a lock? (screenshot of the
  menu).
- **The sweep:** `.reg` with `Keep` running; `.ej` opened with `Main` and left cached (Home; adj
  900 or more), no service: the control; APODroid's debug build cached, the daily switch on.
  `logcat -c`, `logcat -G 16M`. Open `.hog` (`--ei maxMb 3000`) and let it allocate until logcat
  shows `ZuiMemoryCleaner … forcestop app` for the control, or free memory falls under about 150
  MB (`/proc/meminfo`'s `MemAvailable`); finish the hog.
- **Read:** `logcat -d | grep -e ZuiMemoryCleaner -e 'Force stopping' -e am_kill -e
  lowmemorykiller`: which of `.reg` (the service), `.ej` (cached) and APODroid were force-stopped,
  killed by the low-memory killer (alarms stay) or spared; `dumpsys alarm | grep -e probe -e
  apodroid`; each probe's exit reasons, `wasForceStopped` and its `PendingIntent` sign (logged at
  its next start: open each probe's `Main` after the sweep), for O14.
- **If Recents has a lock:** lock `.ej`'s task, `KeepOff` on `.reg`, sweep again: does the lock
  spare a cached app?
- **Expected:** the service's process spared, a cached app force-stopped in the same sweep (if
  the low-memory killer took the cached ones first, say so: then only "the service spared" is
  shown).
- **Verdict:** PASS when `.reg` with `Keep` was spared while a cached app was force-stopped; STOP
  when the service's process was force-stopped (Keep running doesn't help on ZUI: the user
  decides about O11).
- **Done when:** the report names every app the cleaner stopped (the user reopens them), the
  probes uninstalled, the tablet set back.

### [ ] O9. Android check on the FP6: the expedited run and the service on Android 16 (Opus, medium)

(A day session **with the user**, the FP6 attached, after W and 0.6.0's tag; Android 16,
LineageOS. Confirms O6 and O7 on 16; a different answer is a STOP for the user.) Touch only the
probes; nothing of the user's.
- O6's session 1, cases (a) and (b) only (`cmd battery unplug` while on USB; `dumpsys battery
  reset` after).
- O7's steps 1 (the look: screenshots), 2 (a Recents swipe) and 3 (`kill -9`).
- Android 16's runtime quota: an `.ej` job while `Keep` runs (the job's time in the log).
- **Done when:** the user saw the results; `.coord/runs/o9/report.md`; the probes uninstalled,
  the phone as found.

### [ ] O10. The alarms' run as an expedited job (Opus, medium)

(This file, The expedited run. After O4's packages the user picked, and O6's PASS.)
- `DailyJob.runNow`: job 2 with `setExpedited(true)` on Android 12+; `RESULT_FAILURE` (no
  expedited quota left): scheduled again without it, logged ("Daily job: alarm, run scheduled,
  not expedited"); Android 10–11 as now. The run's start logs whether it is expedited
  (`params.isExpeditedJob`, 12+).
- Nothing else in the job changes unless O4 says so. A stop by the expedited job's limit ends as a
  stop does (B7).
- The pause hint's text stays as it is (Decisions, 6).
- **Files:** `DailyJob.kt`; `CLAUDE.md` (Background update, The alarms: on 12+ an Optimized
  phone's run is expedited and has the network while the app isn't in RESTRICTED, 10–11 as
  before; Next features, 0.6.0: "Optimized phones get the picture late, accepted" with that
  note); the skill's Where things are (`DailyJob.kt`).
- **Tablet:** Optimized (the whitelist entry removed), on battery, `rare`, force-idle, a change
  time a few minutes ahead ("New picture at" or a stepped clock, forward only): the alarm's run
  sets within about a minute of the alarm (`logcat -s APODroid`: "alarm, run scheduled", the start
  expedited, "wallpaper set"); battery saver the same; Unrestricted as before; `dumpsys
  jobscheduler` shows job 2 expedited while pending; the smoke list. The bucket, whitelist,
  battery and Doze set back.
- **Done when:** built; the tablet passes.

### [ ] O11. Keep running: the service, the setting, its starts (Opus, high)

(This file, Keep running; Decisions 1 and 3. After O4's packages the user picked, and O7's and
O8's PASS or the user's word after a STOP; not after O10, which it doesn't need.)
- **Manifest:** `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`; `<service
  android:name=".KeepRunning" android:exported="false" android:foregroundServiceType="specialUse">`
  with `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
  android:value="…">` (one sentence: keeps the app's process so its wallpaper alarms aren't
  cleared by memory cleaners; a user setting, off by default).
- **`KeepRunning.kt`:** `onStartCommand`: `ServiceCompat.startForeground` (the `specialUse` type
  on 34+) in a try; a refusal logged and `stopSelf`; `START_STICKY`; nothing else. `ensure(context)`
  starts it while `daily` and `keepRunning` (`startForegroundService`, the refusal caught and
  logged), else stops it.
- **The notification:** channel id `keep`, name "Keep running", `IMPORTANCE_LOW`, no badge; the
  text "Keeps the wallpaper changing on time"; a small icon from `logo/`'s monochrome layer as a
  24 dp vector; ongoing; `contentIntent` the page (`FLAG_IMMUTABLE`).
- **`Store`:** `keepRunning` (the pref, default false).
- **The page:** Advanced settings, while the daily switch is on, after "Save every picture"'s
  group: the switch "Keep running" with an ⓘ ("About keeping running" for screen readers) and
  its dialog (Decisions, 3); acts at once: on → `ensure`, off → stop. `onResume` → `ensure`
  beside `DailyJob.ensure`. The daily switch on → `ensure` after its run; off → stop (beside
  `DailyJob.cancel`).
- **`AlarmReceiver`:** `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED` → `ensure` too, while `daily`.
  **`DailyJob`:** a run's start → `ensure` (allowed while Unrestricted; a refusal caught, logged
  once a run).
- **Files:** `AndroidManifest.xml`, `KeepRunning.kt` (new), `Store.kt`, `MainActivity.kt`,
  `AlarmReceiver.kt`, `DailyJob.kt`, a drawable; `CLAUDE.md` (Settings' Advanced, "No
  notification" with its exception, Background update, Permissions, Next features 0.7.0); the
  skill's Where things are.
- **Tablet:** on → `dumpsys activity services io.github.buerlino.apodroid` shows `KeepRunning`
  foreground; Quick Settings' Active apps lists APODroid (screenshot); nothing in the shade; with
  `pm grant … POST_NOTIFICATIONS` the notification (screenshot; a tap opens the page), revoked
  after; the ⓘ's dialog (screenshot); off → gone; the daily switch off → gone, on → back;
  `run-as … kill -9` → as O7 found; a reboot → running; `install -r` → running; a Recents swipe →
  running; `cmd activity stop-app` → gone, the alarms there, the next page open → back; a forced
  run (`run -f 1`) with it on sets as before; font scale 2.0 at the phone's width: the row
  (screenshot); the smoke list.
- **Done when:** built; the tablet passes; the texts and the two permissions listed.

### [ ] O12. The page lets its pictures go while stopped (Opus, medium; only if O7 asks for it)

(O7's memory: Home's PSS more than 30 MB above Back's. With Keep running, and with Live, the
process stays alive for days.) When the page stops (`onStop`) it drops its decoded pictures (up
to three) and decodes them again at `onStart`, as `show()` does. Kept: the draft, crop mode's
state, the shown source, Setup's and Advanced's state (B13's rules for a rotation stay).
- **Files:** `MainActivity.kt`.
- **Tablet:** O7's step 8 again: Home's PSS within 30 MB of Back's; back on the page the pictures
  show at once, ⛶'s dragged part and the draft kept; a rotation as before; the smoke list.
- **Done when:** built; measured; the tablet passes.

### [ ] O13. The pause hint per maker (Opus, medium)

(This file, The pause hint per maker. After O3.)
- **`:core`:** the maker from `Build.MANUFACTURER` (Xiaomi/Redmi/POCO, Samsung,
  Oppo/realme/OnePlus, Vivo/iQOO, Huawei/Honor, Lenovo, else stock), case-insensitive, with
  tests on real strings ("Xiaomi", "HONOR", "samsung", "OnePlus", "LENOVO", "Fairphone",
  "Google"). The detector (O14) uses it too.
- **The battery line** per maker (placeholders from this file's paths; Honor: "Battery
  optimisation" → "Don't allow"; Lenovo: what O8 found; stock: as now), and beside "App info" a
  button "Battery optimisation" opening `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`
  (always: the night's default; the user decides from the screenshots).
- **The texts lean** (`CLAUDE.md`'s short-texts rule, binding since 2026-10-29): O13 is the pause
  hint's only rewrite (`lean-ui-0.7.0.md` leaves the hint and its Hide dialog to it), with
  Android's and the makers' labels word for word. Whether " (on Optimized, a new picture can
  come hours late)" stays (decided 2026-10-28, "Keep today's text") is revisited in
  `lean-ui-0.7.0.md`'s mockup; O13 follows what the user picks there.
- **Files:** a `:core` file and its test, `MainActivity.kt`; `CLAUDE.md` (Background update, The
  pause hint); the skill's Where things are.
- **Tablet:** the hint on the tablet (Lenovo's line; screenshot), the new button opens the list
  (screenshot), "App info" as before; the other makers' lines listed in the report as text for the
  user; the smoke list.
- **Done when:** built; the tests pass; the tablet passes; the texts listed.

### [ ] O14. The detector (Opus, high)

(This file, The detector. After O11 and O13.)
- **`:core`:** whether a page open's set is a miss (this file's rule: the set's stamp T, now, the
  offset, `lastRunAt`, the boot time, the update time, job 2 pending, `alarmsLostAt`), with
  tests: each exclusion; a bucket's held alarm (no `alarmsLostAt`: none); W's tablet day (the
  lock screen waiting with turns, the new stamp 18:00,
  `lastRunAt` 13:34, the page at 19:40: a miss); a page at 06:20 with a 7 min offset (T + offset +
  30 min not passed: none); a phone off overnight (booted after T: none); a run that started and
  failed (none).
- **`DailyJob.ensure`:** before arming, the `PendingIntent` sign (or the one O7 and O8 found
  working), noted as `alarmsLostAt` unless a boot or an update since `lastRunAt` explains it.
- **`Store`:** the page's `update` notes a miss (`missedAt`, T) when one of its sets is one; the
  dismissed T.
- **The box** (neutral, with the others; while the daily switch is on; from `missedAt` until
  dismissed): the texts and buttons of this file's detector by Keep running off or on (O13's
  maker line); "Keep running" opens Advanced settings at the switch.
- **Files:** a `:core` function and its test, `DailyJob.kt`, `Store.kt`, `MainActivity.kt`;
  `CLAUDE.md` (The app, the boxes; Background update); the skill's Where things are. SIM: unchanged (no rule changes);
  run it.
- **Tablet:** Keep running off: `am force-stop` after a run, the clock stepped (forward only) past
  a change time + offset + 31 min with something new due, the page opened: the box (screenshot);
  "Keep running" opens Advanced at the switch; Dismiss hides it until the next miss. Keep running
  on, the same steps: the maker's line (Lenovo). With Size Fill (a force stop drops a live
  wallpaper to the default; `live-0.7.0.md`). No box: a reboot after T; a page open before T +
  offset + 30 min; job 2 pending (Wi-Fi off when its alarm fires, then the page); a held alarm
  without a force stop (`am set-standby-bucket … rare`, the clock stepped past T + offset + 31 min
  before the alarm fires, the alarm still in `dumpsys alarm`, then the page). The clock and
  `auto_time` set back; the smoke list.
- **Done when:** built; the tests pass; the tablet passes; the texts listed.

### [ ] O15. Tablet pass of the track (Opus, high)

(Agent rosie, the tablet; V11a's and V11c's form, on the tree with O10–O14.) Debug, then the R8
build (sign as the skill says); the update from 0.6.0's build with its setup (Keep running off
afterwards, nothing new running); then Keep running on: the expedited run in force-idle and
under saver (O10's checks), the service's starts (O11's checks), the detector (O14's), the
pause hint (O13's), a Set and the daily switch with the service on, `am kill` and a reboot;
`logcat` and `exit-info` without a crash. The texts' screenshots in one folder for the user.
- **Done when:** every check ran on R8 too; no `BUG:` line, or each one drafted as a package.

### [ ] O16. A week on the tablet with Keep running (Opus, medium)

(Only when O8's sweep was unclear, e.g. the low-memory killer took the cached apps before the
cleaner acted, or O15 finds something about the service; user, 2026-10-28: "Skip if O8 is
clear". Track L needs the tablet too.) (W's way, by hand, morning reads; **the user** keeps the tablet on battery at night and plugs it
in by day as W did.) Keep running on, Optimized for the first half of the week and Unrestricted
for the second; `logcat -G 16M`. Each morning: the sets on time (`dumpsys wallpaper`, the log), the
service alive, `ZuiMemoryCleaner` lines and whom they stopped, `exit-info`, `dumpsys alarm` (the
pair there), the bucket. A force stop by the cleaner with the service running is a STOP for the
user.
- **Done when:** seven mornings read, `.coord/runs/o16/report.md`, a verdict per day.

### [ ] O17. Phone pass: the FP6 and the friend's Honor (Opus, high)

(**The user**: the FP6 is the user's phone; the Honor the friend's, by screenshots.) The
release-signed build over the user's install (the skill: testing an update on the user's phone),
Keep running off by default; the user turns it on for a few days: the Active apps entry, a
morning's set on time, the memory; then the user's choice to keep it on or off. The friend: the
pause hint's Honor line and its paths followed (screenshots), Keep running on, the status line's
time after a morning.
- **Done when:** the user has seen both; the texts approved or changed; the report in
  `.coord/runs/o17/`.

## Seams with track L

Aligned with `live-0.7.0.md`'s "Seams with track O" (2026-10-28).
1. **D, the screen-on check, is track L's (L8), and it's job 2:** the engine's
   `onVisibilityChanged(true)`, when a check is owed (`Store.nextAlarm()` ≤ now) and no run started
   in the last 15 min, calls `DailyJob.runNow`, as an alarm does, not `Store.update`. So the run
   notes `lastRunAt` and takes `JobRuns` as any run (the detector counts it; a Live user's miss
   shows only when the screen stayed off past the change), and with O10 it's expedited. O4 keeps
   `runNow` callable from the engine (the main thread, no lock held).
2. **The live wallpaper keeps the process alive:** the system binds it with
   `BIND_FOREGROUND_SERVICE_WHILE_AWAKE` and rebinds it after a low-memory kill (AOSP 15), so
   ZUI's cleaner (minAdj 701) very likely spares it while the screen is on. But **a force stop
   makes Android drop a live wallpaper to the default** (AOSP: `ACTION_PACKAGE_RESTARTED` →
   `clearWallpaperLocked`), and with the screen off the binding's foreground level lapses. So Keep
   running still matters with Live unless L1 shows the adj stays low with the screen off. L1
   reads the engine's oom adj and runs ZUI's cleaner against a live probe; O8's sweep can include
   L's probe if it exists by then.
3. **The detector and Live** (user, 2026-10-28, `live-0.7.0.md` Decision 9): the detector doesn't
   suggest Live (Live is a look, Keep running the fix); with APODroid the current live wallpaper
   and still a miss, it shows the maker's line. "APODroid is the live wallpaper" is L5's
   `Store.liveOn`, not `getWallpaperInfo()`: below Android 14 that returns null without
   `QUERY_ALL_PACKAGES`. Keep running stays a plain switch, not turned off by Live.
4. **No shared service:** Keep running is a foreground service that does nothing; Live's engine a
   `WallpaperService`; the runs stay JobScheduler's. All in the app's one process: L must not use
   `android:process` (the prefs, the locks and `JobRuns` are per process).
5. **The set paths:** this track changes how a run starts (O10) and keeps the process (O11);
   L changes how a picture reaches the screen. `Store.update` stays the one entry point for a run;
   the engine learns of a new picture through the prefs listener (`Store.watch`) in the same
   process.
6. **Memory:** with Keep running or Live the process stays for days: O12 (the page's pictures
   let go while stopped) serves both; L's engine holds its own decoded picture.
7. **Order and the pipeline:** O1–O4 come first for both tracks (O2 is the part lookup L's
   drawing uses, O3 the dropdown L's Size row uses). The tablet is one device: O6–O8 and L's
   checks one at a time. The pipeline takes the newest `plan-*.md`: when building starts, a
   `plan-0.7.0.md` gathers both tracks' packages (or `pipeline-lib.sh`'s `spec_text` learns
   `on-time <key>` and `live <key>`), and `night.md` gets the O and L rows.
8. **Permissions:** this track adds `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SPECIAL_USE`
   (O11); L adds none (CLAUDE.md: a `WallpaperService`, no permission).
9. **Force stops in checks:** with APODroid's live wallpaper on, `am force-stop` drops it to the
   default and `am kill` doesn't kill the process. This track's checks that force-stop the app
   (O14) run with Size Fill.
