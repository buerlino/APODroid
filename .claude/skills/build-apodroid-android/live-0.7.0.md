# Live, the day's pan (0.7.0)

(User, 2026-10-22 evening, 0.7.0's headline; planned 2026-10-28 with the user:
[Decisions](#decisions-user-2026-10-28); reviewed by reading the same evening against `Store.kt`,
`PictureTime.kt` and `Frame.kt`: the engine draws its own copy, lost Live gets Fill at once, Set
waits for the engine after the picker, the shared engine told by the engines' notes (replaced
2026-11-14 by the system's answers, Decisions 10–12), the engine can't crash, Decision 10
(replaced 2026-11-14). Rewritten 2026-11-14 after L1's STOP on the tablet, from the user's
[Decisions after L1](#decided-after-l1-user-2026-11-14): Live is the home screen's, the lock
screen Fill when its picture differs, the stale wake frame accepted, both screens Fill when home
loses Live. What was settled before is in `CLAUDE.md`, Next
features, 0.7.0: Live, the day's pan: Size [Fill | Live] with the preview on the page, a
`WallpaperService`, no library, no permission, the position by the time of day from the picture
time, drawn when the screen turns on and nothing while it's off, the clock moves it and not the
launcher, chosen once in Android's live wallpaper picker, steps set by the job rejected. The
packages write the rest into `CLAUDE.md` when they land; this file is the how. Built after 0.6.0's
tag: 0.6.0 is in its code freeze.)

## What Android gives (investigated 2026-10-28, by reading)

No device was touched (W runs to Mon 2 Nov). Sources at the end; "AOSP" means
`frameworks/base` read on the branches named.

**A live wallpaper** is a `WallpaperService` whose `Engine` draws into a surface the system
shows behind the launcher (and the lock screen). The manifest declares the service with
`android:permission="android.permission.BIND_WALLPAPER"` (the permission the *binder* must hold,
i.e. the system; the app asks for nothing), the intent filter
`android.service.wallpaper.WallpaperService` and the meta-data `android.service.wallpaper`
pointing to an XML `<wallpaper>` (thumbnail, description, settings activity). No new permission
in the app's `<uses-permission>`s.

**Choosing it:** only Android's picker can make a live wallpaper current
(`WallpaperManager.setWallpaperComponent` is a system API behind `SET_WALLPAPER_COMPONENT`). An
app opens the picker's preview of its own engine with `ACTION_CHANGE_LIVE_WALLPAPER` and
`EXTRA_LIVE_WALLPAPER_COMPONENT` ("Directly launch live wallpaper preview, allowing the user to
immediately confirm"); `ACTION_LIVE_WALLPAPER_CHOOSER` opens the list. Which screens it goes on
(home, lock, both) is the picker's question to the user, where the picker asks it at all; the
app can't choose. The picker shows the engine with `isPreview()` true, a separate engine.
**L1 (ZUI, Android 15):** the picker (`livepicker/.LiveWallpaperChange`) asks "Home screen" or
"Home screen and lock screen", never "Lock screen" alone, and nothing while the engine is already
on both; `resolveActivity` returns null without `<queries>` while `startActivity` works.

**Home and lock, by Android version:**
- **10–13:** a live wallpaper is the home ("system") wallpaper only. AOSP 13's
  `setWallpaperComponent`: "Live wallpapers can't be specified for keyguard. If we're using a
  static system+lock image currently, migrate the system wallpaper to be a lock-only image": so
  picking Live over a shared still wallpaper leaves the old picture on the lock screen. With no
  lock picture the lock screen shows the home wallpaper, live ("New live wallpaper is also a lock
  wallpaper if nothing is set"); `clear(FLAG_LOCK)` gets there ("The home screen wallpaper will
  become visible on the lock screen"; AOSP 13 removes the lock data and tells the keyguard).
  SystemUI's `LockscreenWallpaper` draws a lock picture over it.
- **14:** `Engine.getWallpaperFlags()` and `onWallpaperFlagsChanged(which)` (API 34). A separate
  lock-screen live wallpaper was behind `persist.wm.debug.lockscreen_live_wallpaper`, **default
  false** in AOSP's `android14-release`; the commit "Remove lockscreen lwp flag" (committed
  19 Oct 2023, after 14's release) made it always on and removed SystemUI's lock picture path.
  So Android 14's first builds may not offer it; 15 and later do (AOSP main:
  `isLockscreenLiveWallpaperEnabled()` returns true).
- **Splitting (AOSP main, `WallpaperDestinationChangeHandler`):** with one live engine on both
  screens, a **still set on `FLAG_LOCK`** makes the engine home-only (`updateEngineFlags` →
  `onWallpaperFlagsChanged(FLAG_SYSTEM)`), and a still set on `FLAG_SYSTEM` makes it lock-only.
  Re-applying the same live wallpaper to home alone while it's on both rebinds it
  (`forceRebind`): two engines of the same service, one per screen. One engine on both screens
  is **one surface**: it can't show home and lock different pictures. **L1:** the split by a
  still set works as read (`Still lock` → `FLAGS 1`, the same engine, no rebind); two engines
  never happened: "Home screen" ends as one engine on both over a shared still, and also over a
  lock-only engine with a still on home.
- **16:** `WallpaperDescription` and `onCreateEngine(description)` (instances with their own
  content). Not needed here.

**Is APODroid's live wallpaper on?**
- `getWallpaperInfo(which)` (API 34) answers per screen; the caller must be able to see the
  wallpaper's package, and an app always sees its own.
- `getWallpaperInfo()` **on Android 13 and earlier returns null without `QUERY_ALL_PACKAGES`**
  (AOSP 13's `WallpaperManagerService.getWallpaperInfo`: `hasPermission(READ_WALLPAPER_INTERNAL)
  || hasPermission(QUERY_ALL_PACKAGES)`; the docs: "For devices running Android 13 or earlier,
  this method requires the QUERY_ALL_PACKAGES permission").
- `getWallpaperId(which)` needs no permission (AOSP 10 and 13). The id changes with every set or
  clear (`makeWallpaperIdLocked`), not with a rebind after a process death. So below 14 the engine
  notes the home wallpaper's id when it starts, and "Live is on" means the id is still that one.
- **L1's table** (14+): shared: `getWallpaperInfo(FLAG_SYSTEM)` names it, `(FLAG_LOCK)` null,
  the lock id -1; home only: `(SYSTEM)` names it, the lock id not -1; after a revert of home
  (two deaths in 10 s): `(SYSTEM)` null and `(LOCK)` null too, while the lock screen still shows
  the engine, which reports flags 3. An app sees its own package without `<queries>`.

**The process:**
- The system binds the current wallpaper with `BIND_AUTO_CREATE | BIND_SHOWING_UI |
  BIND_FOREGROUND_SERVICE_WHILE_AWAKE | BIND_INCLUDE_CAPABILITIES` (AOSP 10 and main). The app's
  process lives as long as it's the wallpaper, at foreground-service level while the screen is on.
- After the process dies the system rebinds it. After a low-memory kill it tries 3 times, 5 s
  apart. Dying twice within 10 s (`MIN_WALLPAPER_CRASH_TIME`) **reverts to the default
  wallpaper**. **L1:** `kill -9` rebound in 1.5 s; the low-memory killer's kill rebound 6 s
  later, still live; two deaths within 10 s or a crash loop revert **the home screen only**: the
  lock screen keeps the engine (a new one, which believes it's on both, flags 3). A throw in the
  draw shows a crash dialog first. The oom adj stays 100, the screen on or off (ZUI's cleaner
  spared it, `minAdj` 701).
- **A force stop reverts to the default wallpaper:** `ACTION_PACKAGE_RESTARTED` →
  `PackageMonitor` marks the package as disappearing (`PACKAGE_TEMPORARY_CHANGE`) →
  `doPackagesChangedLocked` → `clearWallpaperLocked`. That covers the user's App info → Force
  stop, `am force-stop`, and a maker's cleaner that force-stops (ZUI's, `on-time-0.7.0.md`).
  L1: both screens default.
- An update keeps it: the package is modified, the service still exists, so it rebinds (L1, and
  a reboot: `CREATE` 1.5 s after `boot_completed`).
- `AppStandbyController` exempts bound widgets, not wallpapers. The buckets still apply to the
  app's alarms and jobs.
- Fairphone's forum: on the FP5's Android 15 (Fairphone OS) live wallpapers reverted to the
  default within minutes; a fix is listed in FP5.VT2I.C.072.20251122 and the revert was reported
  again in 2026. The FP6 runs LineageOS: L10 looks for it.

**Drawing:**
- `SurfaceHolder.lockCanvas()` (software) or `lockHardwareCanvas()` (API 26). The engine's
  callbacks run on the app's main thread, shared with the page. **L1:** software 6–23 ms a draw
  (up to a 79 MB picture), hardware faster at up to twice the memory; mixing them on one surface
  breaks it (software → hardware aborted the process, hardware → software gives a null
  `lockCanvas()` until the process restarts).
- **L1:** the frame after a wake: the lock screen shows the frame drawn before the screen went
  off for about 0.3 s (until `VIS true`, then the new frame 10–16 ms later); home after the
  keyguard the fresh one.
- "It is very important that a wallpaper only use CPU while it is visible" (`onVisibilityChanged`).
- `onSurfaceRedrawNeeded` wants a frame at once. `setOffsetNotificationsEnabled(false)` is "a
  performance optimization" (no launcher scroll events). Zoom and touch are off by default.

**Colours:**
- `onComputeColors()` (on the main thread, may return null) and `notifyColorsChanged()`, which
  the system rate-limits to once a second.
- SystemUI's `ThemeOverlayController` (AOSP 12 and main) accepts one colour event after a wallpaper
  change from a foreground app. Later events, which includes a live engine's and a set from the
  background such as the job's, are held "until screen off". So the system's theme colours follow
  a new picture at the next screen-off and never change in front of the user, with Fill too.
- The tablet and the FP6 have wallpaper colours off (U1): only what the engine reports can be
  checked. L1: `dumpsys wallpaper` lists none on the tablet; SystemUI's log does
  (`ThemeOverlayController: onColorsChanged … which 3`).

**Memory**, decoded at the surface's height (ARGB, 4 bytes a pixel):
- FP6, 1116×2484: a 3:2 rendition 3726×2484 is 37 MB. The widest real APOD of 0.6.0's mock-up
  (Fill kept 21% of its width on a 1080×2400 screen, about 2.1:1) is 5316×2484, 53 MB.
- Tablet, 800×1340: a 3:2 picture is 2010×1340, 11 MB.
- Earth's 1440×600 isn't scaled up: 3.5 MB, drawn enlarged. A user's 12000×3000 panorama would be
  99 MB at full height, so it needs a budget (below).
- **L1** (one engine, software): a process's base about 50 MB PSS; 21 MB decoded → 70 MB, 51 MB →
  105 MB, 79 MB → 132 MB. Decodes 53–117 ms (21 MB) to 500 ms (79 MB). Budget: 50 MB a bitmap.
- **Speed:** a 3:2 picture on the FP6 travels 2610 px in 18 h, about 2.4 px a minute; with 6 h
  turns about 7 px a minute. Nothing needs animating.

## How Live fits what the app does (2026-10-28)

What decides what each screen shows stays; Live changes only how a picture reaches a screen.

| 0.6.0 | With Live |
|---|---|
| The rules, the records per screen, turns, the stand-in, Set's forgetting, the alarms, the job | Unchanged: they decide the picture; SIM still guards them |
| A still set per screen (`setStream`/`setBitmap`, home and lock apart, M0); Android keeps its own copy of the picture | On the home screen while APODroid's live wallpaper is on there: no set. Home's picture is **copied to Live's own file** (`files/live.jpg`, with its whole flag), the engine's copy as Android's is for a still set; the record (noted live) and `wallpaperSetAt` are written as after a set, and the engine redraws (a prefs listener in the same process). Elsewhere the still set as now |
| One wallpaper on both screens when both are due the same picture | One engine on both while both show home's picture; when the lock screen is due another picture it gets its still set and Android moves the engine to home (L1: `FLAGS 1`, the same engine). The lock screen never has an engine of its own (Decision 10) |
| ⛶'s part (Fill) | Fill only. In Live the clock places the frame (no drag); the stored part stays for Fill |
| The whole picture per post, on blur or black (A3) | The same per post: on a live screen the engine draws it still, with A3's drawing, now shared |
| "My picture", Fill centred | It moves too (decided) |
| Video days ("Skip it": the old picture stays) | The old picture keeps moving (its copy stays; `refresh` deleting `apod.jpg` doesn't touch it) |
| The screen turns on | The engine redraws when it becomes visible; the lock screen shows the last frame for about 0.3 s first (L1; accepted, Decision 11) |
| A newer post stored before it's due (Los Angeles: the 21:05 APOD, due at 06:00) | Not shown: the engine draws only its copy, also after a process death, a reboot or a rebind |
| Landscape: Android zooms the portrait frame's middle band (CLAUDE.md, Wallpaper) | The engine draws for its surface: in landscape the picture at the landscape height, at the same position |
| The launcher's scroll offset | Ignored (CLAUDE.md: the clock moves it) |
| The user picks another wallpaper; any wallpaper is replaced at the next change | A screen noted live that isn't live now (another wallpaper, a force stop's default) counts as a forgotten picture, so the next run (a page open too) sets Fill at once, not at the next change (decided: a force stop never cost the picture before); when home lost Live (also Android's revert after two deaths in 10 s, which leaves the engine on the lock screen), both screens get Fill (Decision 12); the page says Live isn't on, Set offers the picker |
| The status lines, the chip | From the records, as now; Setup's summary adds " · Live" |

## Decisions (user, 2026-10-28)

Asked one at a time, each with the recommendation first; every answer was the recommendation.
1. **The route: straight.** The same speed all day, from the left edge at the change time to the
   right edge 18 h later. "Slower over the middle" and "slowing over what matters" not wanted (A2
   found no rule that beats the middle; no ML library).
2. **The night: still at the right end** until the next change time, then the next picture (or
   the same one) starts at the left. Not "back to the left at 24:00", not "slowly back".
3. **Turns: each picture crosses within its own turn** (the user asked what turns meant: a
   screen with several sources and "Change every" 12, 6 or 3 h; answered with the example APOD
   06–12, NASA 12–18, Earth 18–24). A turn of 24 h crosses in 18 h, then still; a night turn
   crosses unseen. Not "one clock for the day" (a 12–18 turn would show only its middle third).
4. **A screen with a column where APODroid's live wallpaper isn't on gets Fill**, as 0.6.0: the
   lock screen on Android 10–13, a screen the user left out in Android's picker, one where the user
   picked another wallpaper since, or after a force stop. The pictures keep coming; Set offers the
   picker again. Not "leave it alone until Set". (2026-11-14: Live is home's, so home's loss
   takes the lock screen too, Decision 12.)
5. **Android 10–13's lock screen: Fill (still)**, also when both screens show the same picture.
   The user asked for the pros and cons first; answered: `clear(FLAG_LOCK)` is Android's
   documented way to show the live home wallpaper on the lock screen (AOSP 13 does it), small in
   code, but it would ship untested on 10–13 (no device; E stays the user's call) and makers' own
   lock screens may ignore it.
6. **Fill stays a still set** (CLAUDE.md's "Auto and Fill as live too"; Auto went in 0.6.0, A2).
   Asked again with the pros and cons on the user's request. Against: every user, 0.6.0's
   updaters too, would confirm Android's picker once; the still path stays anyway (those who don't,
   10–13's lock screen); a force stop, a crash loop and FP5-style reverts drop a live wallpaper to
   the default, a still one survives them; the process would stay alive holding the picture. For:
   one path, instant switching, landscape. Live only for those who choose it.
7. **The preview: in ⛶ mode.** With Live, ⛶ shows the frame where the clock has it now, and a drag
   doesn't move it. With "Whole picture" on (Fill or Live) the picture's place shows a
   screen-shaped preview, the picture on its blurred or black background, drawn by the
   wallpaper's own code (A4 left this out until Live). Nothing new outside ⛶ mode. Not a replay of
   the day's route, not a preview always shown.
8. **"My picture" moves too:** Live moves every picture except one set to "Whole picture".
9. **The detector doesn't suggest Live** (track O's recommendation, decided here): Live is a
   look, Keep running the fix. **Live doesn't change the detector** (2026-11-14, a review): it
   fires only when the alarms are gone, i.e. after a force stop, which drops Live on both
   screens, so "Live on and still a miss" can't happen and the detector needs no `Store.liveOn`.
10. ~~Different pictures on home and lock: Set offers the picker for the lock screen~~ (user,
    2026-10-28 evening; replaced 2026-11-14 by the Decision 10 below: L1 found no way to a lock
    screen engine through ZUI's picker).

### Decided after L1 (user, 2026-11-14)

From L1's STOP (rosie, the tablet, `.coord/runs/095-l1/report.md`).
10. **Home ≠ lock: Live on home, the lock screen still Fill** (replaces 2026-10-28's Decision 10).
    ZUI's picker offers only "Home screen" or "Home screen and lock screen", and "Home screen"
    still puts the one engine on both; a still set on `FLAG_LOCK` leaves it on home alone
    (`FLAGS 1`, the same engine). So Live is the home screen's: the engine draws home's picture,
    on the lock screen too while the lock screen is due the same one (one engine on both). When
    the lock screen is due another picture (different columns), or the engine isn't on it (only
    home has Live), the app sets the lock screen's still Fill itself with `FLAG_LOCK`, after the
    picker by Set's own update, later by any run. No second picker trip, no `live.lockAsked`, no
    second engine. A lock screen with a column and no home column (APOD mode's "Where" Lock):
    Fill, no picker. **The same rule on every Android 14+ picker**, also one that offers "Lock
    screen" (LineageOS's on the FP6, L2): a lock screen with an engine of its own isn't counted as
    live, so its column's next set replaces it with Fill. Changed only if L2 shows a reason: L6
    doesn't wait for L2 (user, 2026-11-14), so a finding becomes a fix package for the user to
    decide (Open).
11. **The wake frame: accepted.** The lock screen shows the frame drawn before the screen went off
    for about 0.3 s after a wake, then the engine's new one (L1, 2 of 3 captures; the lock screen
    still dim then). No redraw while the screen is off (CLAUDE.md's rule stands).
12. **Home lost Live: both screens Fill.** Two deaths within 10 s or a crash loop make Android
    revert only the home screen to the default; the lock screen keeps the engine, a new one that
    believes it's on both (flags 3; L1, step 7). The next run that finds home's record noted live
    and home not live sets **both** screens' still Fill: home's and the lock screen's records
    forgotten (the lock screen's when it has a column or the engine covered it, `live.shared`),
    every set still, the stray engine replaced by the lock screen's still set. Live is then off
    on both until the user picks it again in Android's picker (Set offers it). How the app knows:
    14+ `getWallpaperInfo(FLAG_SYSTEM)` no longer names `LiveWallpaper` (L1: null after the
    revert), below 14 `getWallpaperId(FLAG_SYSTEM)` no longer the engine's noted id; the engine's
    flags aren't trusted. The same for any loss of Live on home (another wallpaper picked, a force
    stop). Not "the lock screen keeps Live".

**Decided in this plan, not asked** (the user can change any; each has its reason):
- **Software canvas** (`lockCanvas`; L1: 6–23 ms a draw, never 30): a frame a minute needs no
  GPU, a software canvas has no texture size limit, and hardware cost up to twice the memory.
  **One canvas for an engine's life** (L1: mixing them aborts the process or gives a null canvas
  until it restarts); a null `lockCanvas()` skips the frame, logged.
- **When it draws:** at `onSurfaceRedrawNeeded` and `onSurfaceChanged`, when it becomes visible,
  when its picture changes, and once a minute while visible (only when the frame moved a pixel).
  Never while invisible (Decision 11).
- **The decode:** once per picture, at the screen's portrait height (the longer side, as
  `Store.screen`), never enlarged, within **50 MB a bitmap** (L1: about 50 MB base, 105 MB PSS with
  51 MB, 132 MB with 79 MB; a larger one decoded smaller and drawn enlarged), on a background
  thread, kept until the picture changes. Landscape draws the same decode scaled down: a rotation
  decodes nothing.
- **The engine can't crash:** decode and draw catch everything (out of memory: decode at half
  the size, then again, else draw black), since two crashes of the one process within 10 s
  revert home to the default wallpaper (Decision 12 then takes Live off both screens) and kill a
  job run in progress. L1: a throw in the draw shows a crash dialog first.
- **Colours** from a small copy of the whole picture (not the moving frame, which would recolour
  the theme all day), reported once per picture.
- **Is it on** (L1's table): home on 14+ when `getWallpaperInfo(FLAG_SYSTEM)` names
  `LiveWallpaper`; below 14 when `getWallpaperId(FLAG_SYSTEM)` is the id the engine noted at its
  start (`live.homeId`). The lock screen only while home is and the one engine is shared:
  `getWallpaperId(FLAG_LOCK)` -1 (L1: shared → -1, home only → a real id). Never below 14
  (Decision 5), never by an engine of its own (Decision 10). The engine's flags aren't used (L1:
  a stray on the lock screen reports 3). Not `QUERY_ALL_PACKAGES` (a permission).
- **Set with Live opens Android's picker when home isn't covered:** home with a column not live.
  Also, on 14+ only and at most once per split, when this Set makes the columns equal (the stored
  columns differed before it) while home is live but not shared: a home-only answer then leaves
  the lock screen Fill, with no further picker. Never below 14 (`liveShared` is never true
  there), never for the lock screen alone (no home column) or a lock column that differs, and
  never again for the same equal columns (else every Set with equal columns, APOD mode's "Where"
  Both too, would open it). The page says which screens are live.
- **After the picker, Set waits for the engine:** it notes `getWallpaperId` of both screens
  before opening the picker; when an id changed on the return, it waits up to 3 s for `liveOn`
  (14+: the system's answer; below 14: the engine's note with the new id, written
  asynchronously) before its update reads it (else a still set could replace the live wallpaper
  the user just picked). L1: picker's "Set" → `CREATE` in 0.16–0.18 s. Not live by then: the
  change wasn't APODroid's, Fill.
- **One engine on both screens draws home's picture**; when the lock screen is due another, its
  still set splits them (Decision 10, Android's own rule); the lock screen's still set comes
  before home's hand-over, so the lock screen never flashes home's new picture.
- **A screen noted live that isn't live now** gets its picture forgotten before the rules run
  (as Set forgets it), so it's due again and gets Fill at once; home's loss forgets both
  (Decision 12).
- **D starts the alarms' run** (job 2, `DailyJob.runNow`), not a second run path in the engine
  (Seams, 1). A stray engine (non-preview, 14+, home not live) counts as a check owed while a
  record is still noted live, so the lock screen's Fill (Decision 12) comes at the next screen-on,
  not the next check time; the run that handles it removes the notes, also when it found nothing
  to replace, so a stray left on a lock screen without a column doesn't start a run at every
  screen-on.

## The plan

### The position (`:core`, L3)

- **The change time T:** the screen's column's last one, `PictureTime.last(now, zone, turns)`,
  with turns when the column has two or more sources.
- **The span:** the shorter of the turn's length (`next(T) − T`) and 18 h, in real time, as
  `PictureTime` counts it: one source or 24 h turns get 18 h, every 12 h gets 12, every 6 h gets 6.
- **The position:** `p = clamp((now − T) / span, 0, 1)`. Straight (Decision 1), still at 1 after
  the span (Decision 2), each turn from 0 (Decision 3).
- **By the clock only**, as the rules: not by when the picture went on. A picture that goes on
  late (Zurich's APOD at 06:15) starts at about 0.01; one a pull puts on at 20:00 shows from about
  0.78.
- **The frame:** `wallpaperFrame(width, height, surfaceWidth, surfaceHeight, p)`, the function ⛶
  uses. A picture taller than the surface's shape moves top to bottom (none posted yet); one in
  exactly the surface's shape stands still.
- **DST:** the change times are already on the clock's hours through DST (`PictureTime`). The 18 h
  are real hours, so with "New picture at" 00:00 or 01:00 a DST night moves 24:00's right edge by
  an hour on the clock.

### The drawing: one function, three users (L4)

`Drawing.kt` in `:app` (Android's `Canvas`, so not `:core`): draw a picture onto a canvas of a
given size, either **panned** (the frame at p, filling the canvas) or **whole** (`fitFrame` on
black or on the blurred background: A3's `whole()` today). Its three users:
1. A3's still set of a whole picture, drawing into its bitmap: the same pixels as 0.6.0.
2. The engine, drawing into its surface.
3. The page's preview in ⛶ mode.

### The engine (L5)

- **`LiveWallpaper`** (`WallpaperService`), in the app's one process (no `android:process`: the
  prefs, the locks and `JobRuns` are per process; `on-time-0.7.0.md`, Seams 4).
- **The manifest:** the service `exported="true"` (the picker binds it), guarded by
  `BIND_WALLPAPER`, labelled "APODroid".
- **`res/xml/live_wallpaper.xml`:** the thumbnail is the launcher icon, the description a string
  resource (placeholder "Today's picture, moving across the screen over the day."; the app's first
  `strings.xml`), `settingsActivity` the page.
- **Which screen:** always home's picture (Decision 10), whatever its flags: on both screens,
  on home alone after a split, and as a stray on the lock screen after home's revert (L1: it
  reports flags 3 there) until Decision 12's still set replaces it. `onWallpaperFlagsChanged`
  only redraws.
- **What it draws: only Live's copy**, `files/live.jpg`, with the whole flag noted beside it
  (`live.whole`), at the turns of home's column. Never the source's file: `refresh` replaces `<source>.jpg` with
  every new post, due or not, and deletes `apod.jpg` on a video day, and `Store.set` reads the
  part and the whole flag of the post stored now. Drawing those would show a post the rules
  haven't put on (Los Angeles' 21:05 APOD, due at 06:00) after any process death, reboot or
  rebind, and nothing on a "Skip it" video day.
  - The copy is written by `setDue` when it hands home to the engine (L5): the picture's
    file (the source's or `fallback.jpg`) copied via a `.part` and renamed, then the whole flag,
    then the record noted live, under the wallpaper lock.
  - Without a copy (the picker's preview before the first hand-over, a fresh install): the
    newest stored picture of home's column's first source, else APOD's, else black. Drawn only
    then.
  - A whole post is drawn whole and still; anything else panned, also "my picture" (Decision 8).
- **Hearing about a new picture:** a prefs listener (`Store.watch`) on home's record keys
  while the engine exists, and a re-read whenever it becomes visible. The re-read covers a commit
  from an interrupted thread, which skips the listeners (B16), and a rebind after a kill (L1:
  1.5 s after `kill -9`, 6 s after the low-memory killer; the copy is still there).
- **The canvas:** software (`lockCanvas`), one for the engine's life (L1); a null canvas skips
  the frame (`Live: couldn't draw: no canvas`).
- **The decode:** on one background thread; the main thread only draws. Keyed by the copy's
  file and time, at the screen's portrait height, within `LIVE_DECODE_BUDGET` (50 MB, L1), so a
  rotation decodes nothing and the preview doesn't decode the same file twice. Released in
  `onDestroy`.
- **Nothing throws out of the engine:** decode and draw catch everything; out of memory
  decodes at half the size, then again, else draws black; a broken copy draws black. Logged
  `Live: couldn't …`. Two crashes of the one process within 10 s revert home to the default
  wallpaper (the lock screen keeps the engine; Decision 12) and kill a job run in progress (L1).
- **The colours:** computed with the decode (`WallpaperColors.fromBitmap` on a small copy of the
  whole picture), returned by `onComputeColors`, `notifyColorsChanged()` after each new picture.
- **The note** (below 14 only): a non-preview engine writes `live.homeId`,
  `getWallpaperId(FLAG_SYSTEM)`, when it starts. On 14+ the system answers (`Store.liveOn`), and
  the flags aren't noted (L1: wrong after a revert). A note left by a killed process is harmless:
  any set or clear changes the id.
- **A stray:** a non-preview engine on 14+ that finds `getWallpaperInfo(FLAG_SYSTEM)` not naming
  it is on the lock screen alone after home's revert; it still draws home's copy, logs
  `Live: home isn't live (stray on the lock screen)`, and when it becomes visible counts as a check
  owed for D (L8) while a record is noted live, so the next run sets both screens Fill (Decision
  12).
- **Logs** under `APODroid`: `Live: engine <flags|preview> created / destroyed` (the flags as
  Android reports them), `Live: drew <picture> p=<0.00> in <n> ms`, `Live: decoded <w>×<h>`.

### `Store`, Size and Set (L5, L6)

- **`Store.liveOn(HOME)`** and **`Store.liveShared`** (the one engine on the lock screen too),
  as "Is it on" says. Home: 14+ `getWallpaperInfo(FLAG_SYSTEM)` names `LiveWallpaper`; below 14
  `live.homeId` equals `getWallpaperId(FLAG_SYSTEM)`. Shared: home live and
  `getWallpaperId(FLAG_LOCK)` -1 (14+; L1's table); never below 14. There's no lock-screen engine
  of its own to ask about (Decision 10). `live.shared` notes `liveShared` at the start of every
  run (`Store.update`, before the rules) while home is live, so a re-apply or a split through the
  system's picker without a hand-over is seen; while home isn't live it keeps its last value,
  which Decision 12's lock screen without a column reads.
- **Lost Live** (a review of this plan, 2026-10-28; Decision 12, 2026-11-14): each screen's
  record notes when its picture went to the engine (`wallpaperLive`, `lock.wallpaperLive`; a still
  set removes it). Before the rules run:
  - **home's record noted live and home not live now** (another wallpaper, a force stop, Android's
    revert after two deaths in 10 s): home's record forgotten (`Record.forget()`, as Set does), and
    the lock screen's when it has a column; a lock screen without a column that the engine
    covered (`live.shared`) is due home's picture (the one set with both flags, as a shared still).
    Forgetting removes the records' live notes. Every set of the run is then still (home isn't
    live), so the stray engine on the lock screen is replaced and Live is off on both until the
    picker. `live.shared` removed.
  - **the lock screen's record noted live and not shared now** (the user set a lock wallpaper):
    it alone is forgotten, so it gets Fill (Decision 4).
  So `dueWallpapers` makes them due again and they get Fill at once, by the job or a page open.
  Else nothing would be due (the record says the picture is on) and a force stop's default
  wallpaper would stay until the next change time.
- **`setDue`'s sets** split per screen by a `:core` rule (L3), from Size, whether home is live,
  whether the engine covers the lock screen (`liveShared`), and each screen's picture after the
  plan:
  - Size Fill, or home not live: every set still, as 0.6.0. A still set replaces APODroid's
    engine like any wallpaper (a stray on the lock screen too).
  - Size Live, home live, home due a picture: the engine. No `WallpaperManager` call; the copy
    (`live.jpg` and its whole flag) is written first, then the record (noted live) and
    `wallpaperSetAt` as after a set; `setsSinceSetup` counts it. A failed copy (a full disk)
    counts as a failed set: the record stays, the retry hands it over again.
  - Size Live, home live, the lock screen due **home's picture** while shared: the engine shows
    it already; its record noted live and `wallpaperSetAt`, no set.
  - Size Live, home live, the lock screen due **another picture**, or not shared (home only):
    its still set with `FLAG_LOCK` (Decision 10; a shared engine moves to home, L1's `FLAGS 1`),
    before home's hand-over.
  - Size Live, no home column: every set still (Live is home's).
  - A screen without a column: left alone, as now (the one engine on both shows home's picture
    there), but for Decision 12's lock screen.
- **Size** (pref `size`, `FILL` by default, so 0.6.0's installs change nothing): a segmented row
  [Fill | Live] in Setup, part of the draft (the highlight, `Setup`, `saveSetup`, the saved
  state), always shown, as "Where". Placeholders: the heading "Size"; the summary " · Live".
- **Set with Live:**
  1. `saveSetup`.
  2. If home has a column and isn't live, or (14+ only, once per split) this Set makes the
     columns equal, the stored ones having differed, while home is live but not shared: note
     `getWallpaperId` of both screens and open Android's
     picker (`ACTION_CHANGE_LIVE_WALLPAPER` for `LiveWallpaper`, started directly and
     `ActivityNotFoundException` caught: L1's `resolveActivity` is null without `<queries>`;
     failing that `ACTION_LIVE_WALLPAPER_CHOOSER`; failing both a toast, placeholder "This phone
     can't show live wallpapers.", and Live acts as Fill). One picker trip per Set, for home: on
     ZUI either answer over a shared still ends with the engine on both (L1).
  3. On the return (the page's `onResume`): when an id changed, wait up to 3 s for `liveOn(HOME)`
     (this file, "After the picker, Set waits for the engine"). Then Set's `update(set = true)`:
     home to the engine, and the lock screen's still Fill when its picture differs (Decision 10);
     without a picker at once. Backing out of the picker gives Fill (Decision 4); after the
     equal-columns trip a home-only answer or a back-out leaves the lock screen Fill, and the next
     Set with the same columns opens no picker.
- **The page:** while Size is Live, the daily switch is on and home has a column but isn't live,
  a status line, placeholder "Live isn't on. Set as wallpaper turns it on." (also after Decision
  12's revert). Otherwise the status lines as now.
- **SIM:** three invariants with Size Live: a still set never goes to home while home is live, and
  goes to the lock screen only when its picture differs from home's or it isn't shared; nothing
  is handed to the engine while home isn't live; a screen noted live that lost Live is due again
  in the same run, both screens when home lost it.
- **Found by SIM in L6** (2026-11-14, built; `CLAUDE.md`, The app, Wallpaper: Live): a screen the
  engine is on whose record isn't noted live is forgotten before the rules (`unnotedLive`: after
  the picker a Set of the order alone, or one with nothing due, left the engine drawing its old
  copy), and home's hand-over while the one engine covers a lock screen of another column not due
  it forgets the lock screen and plans again (`lockFollowsHome`: else home's new picture showed
  there too).

### The preview in ⛶ mode (L7)

- With Size Live (the draft's) and the shown post's source in home's column, ⛶'s frame sits at
  the clock's place for home's column and doesn't move by drag. A minute tick moves it while open.
  A source only in the lock screen's column: ⛶ as Fill (the lock screen is Fill with its own
  picture, Decision 10).
- With "Whole picture" on (Fill or Live), the picture's place shows the screen-shaped preview: the
  screen's portrait shape, drawn by L4's function, on the blurred or black background.
- ✓, Back, the highlight and the draft as A4. Size Fill: ⛶ as now.

### D, the screen-on check (L8)

(`on-time-0.7.0.md`, D; Seams 1.) A non-preview engine that becomes visible, while the daily
switch is on, when a check is owed (`Store.nextAlarm()` ≤ now: a check time passed since
`lastRunAt`, or a failed run's retry due, B16) and no run started in the last 15 min
(`lastRunAt`): `DailyJob.runNow(context)`, the same call the alarms make. A stray engine (14+, home
not live: the lock screen after Android's revert of home) counts as a check owed, so Decision
12's Fill on both comes at the next screen-on.
- It's job 2, so `lastRunAt`, `failedRuns` (counted from its start, O4b), one run at a time, the failures and the retries are the
  job's own: no second run path.
- With O10 the job is expedited.
- The screen being on means no Doze, and the process runs at foreground-service level while awake.

## Checks before building (J0's way)

What only a device can tell. A throwaway probe (L0) serves L1 and L2. A check whose answer changes
the design comes before the package it decides.
- **The tablet (L1, rosie, Android 15, ZUI):**
  - The picker: which activity answers `ACTION_CHANGE_LIVE_WALLPAPER`, and whether it asks home,
    lock or both.
  - The engines' flags for each answer, and the split by a still set on the other screen.
    *Decides L5/L6's rule.*
  - Whether the first frame after the screen turns on is the old one. *Opens a question for the
    user if so.*
  - The draw time, software and hardware. *Decides the canvas.*
  - The memory with the widest picture. *Decides the budget.*
  - The process's oom adj with the screen on and off; `kill -9`, a crash loop, `am force-stop`,
    `install -r`, a reboot.
  - ZUI's cleaner under memory pressure (with O8). *The seam with track O.*
  - The reported colours, and `getWallpaperInfo(which)` for its own package without `<queries>`.
  - *Ran 2026-11-14 (STOP), decided by the user the same day: Decisions 10–12.*
- **The FP6 (L2, with the user, Android 16, LineageOS):** the picker and its question, the flags,
  the first frame after the screen turns on by eye. Short; the day-long revert check is L10's.
  Whether LineageOS's picker gives a reason to treat a "Lock screen" choice apart from ZUI's
  (Decision 10).
- **Untestable here:** Android 10–14 (E stays open). That's the `getWallpaperId` note path, the
  10–13 lock screen as Fill, and Android 14's first builds without a lock-screen live wallpaper.

## Rejected

- **Fill drawn by the engine** (user, 2026-10-28, Decision 6), and with it "Auto as live": Auto
  went in 0.6.0.
- **`clear(FLAG_LOCK)` on Android 10–13** for a live lock screen (user, 2026-10-28, Decision 5).
- **A route that slows over the middle or over what matters** (Decision 1); **back to the left
  at night or a slow return** (Decision 2); **one clock for turns** (Decision 3); **leaving a
  non-live screen alone** (Decision 4); **a replay or an always-shown preview** (Decision 7);
  **"my picture" still** (Decision 8); **the detector suggesting Live** (Decision 9).
- **`QUERY_ALL_PACKAGES`** for `getWallpaperInfo()` below 14: a permission, and it sees every app.
  The engine's `getWallpaperId` note does without.
- **Drawing while the screen is off** (CLAUDE.md), also to avoid the stale first frame L1 saw
  (about 0.3 s on the lock screen; user, 2026-11-14, Decision 11: accepted).
- **The launcher's scroll offset** (CLAUDE.md: the clock moves it).
- **A second process** for the engine (track O's reason: the prefs, the locks and `JobRuns` are
  per process).
- **`Store.update` in the engine** for D: a second run path beside the job's bookkeeping, where
  0.6.0's bugs came from. D schedules job 2 instead.
- **Opening the picker at every Set while a screen isn't live:** it would nag, and on 10–13
  always. Set opens it only when home, or a lock screen sharing home's column, isn't covered.
- **A second engine for the lock screen** (Decision 10 of 2026-10-28, with `live.lockAsked` and a
  second picker trip at Set; user, 2026-11-14): ZUI's picker offers no lock screen, its "Home
  screen" makes the one engine shared, and two engines never happened (L1). The lock screen with
  a picture of its own is Fill (the new Decision 10).
- **The lock screen keeping Live after home's revert** (user, 2026-11-14, Decision 12): the stray
  engine believes it's on both; both screens Fill until the picker.
- **The engine drawing the source's file** (`<source>.jpg`): it changes with every stored post
  and goes on a video day; the screen's own copy instead (a review of this plan, 2026-10-28).
- **Android 16's `WallpaperDescription`:** 16 only; not needed with one engine drawing home's
  picture.
- **A hardware canvas** (L1: faster but up to twice the memory, no gain at a frame a minute).
- **Steps set by the job** (CLAUDE.md, 2026-10-22).

## Open

- **For L2** (the FP6's picker, LineageOS, Android 16): does it ask "Lock screen" too, and does
  "Home screen" over a shared still make the engine shared, as ZUI's and AOSP's do? Decision 10's
  rule stays the same everywhere (Live is home's; a lock-screen engine of its own isn't counted
  and its column's next set replaces it) unless L2 shows a reason; L6 is built before L2 (user,
  2026-11-14), so such a finding becomes a fix package for the user to decide.
- What ZUI's "Home screen" does over a home-only engine with a still lock screen (L1 tried it over
  a shared still and over a lock-only engine): L6 looks, for Set's one picker trip when the
  columns become equal after a split; a home-only answer is accepted (the lock screen Fill).
- Android 10–14: untested (E stays the user's call), as for track O.
- Whether a live wallpaper reverts by itself on the FP6, as on the FP5 (L10).
- The texts: placeholders until the user approves them from screenshots (L5, L6, L7).
- A crash of the page or the job twice within 10 s also reverts home to the default (one
  process); only the engine's side is guarded. Accepted, as a crash loop is a bug anyway; Decision
  12 then takes Live off both screens.
- The 3 s wait after the picker: L1 measured 0.16–0.18 s to `CREATE` on 15; below 14, where the
  engine's id note is the only guard, untested (E).

## Packages

Built after 0.6.0's tag (Tue 3 Nov at the earliest), one at a time; the checks need the tablet
after W (to Mon 2 Nov) and share it with track O's (one device: one check at a time). O1–O4
(the declutter and the bookkeeping review) come first for both tracks. Each package writes its
decisions into `CLAUDE.md` when it lands. For the pipeline the rows go into `night.md` with track
O's, and the packages into a `plan-0.7.0.md` (or `pipeline-lib.sh` learns `live <key>`;
`on-time-0.7.0.md`, Seams 7). rosie uses only the tablet; **L2 and L10 need the user and the
FP6**.

| WP | What | Model, effort | Who, device | After |
|----|------|---------------|-------------|-------|
| L0 | The probe: a throwaway live wallpaper (by day, not the pipeline) | Opus, medium | a day session, no device | — |
| L1 | Android check: the live wallpaper on the tablet | Opus, medium | rosie, tablet | L0, W; beside O8 |
| L2 | Android check on the FP6: the picker and the first frame | Opus, medium | a day session, **FP6, with the user** | L0 |
| L3 | `:core`: the position by the clock, the decode size, still or engine | Opus, medium | winston, no device | O1, L1 |
| L4 | The drawing: one function for the whole picture, the engine and the preview | Opus, medium | winston, tablet | O2 |
| L5 | The engine, and `setDue` leaves a live screen to it | Opus, high | winston, tablet | L1, L3, L4 |
| L6 | Size [Fill \| Live], Set opens Android's picker, the texts | Opus, high | winston, tablet | L5 (not L2: user, 2026-11-14) |
| L7 | The preview in ⛶ mode | Opus, medium | winston, tablet | L6 |
| L8 | D: the screen-on check starts the alarms' run | Opus, medium | winston, tablet | L5, O4 (and its packages) |
| L9 | Tablet pass of the track, and two real days with Live | Opus, high | rosie, tablet | L6–L8 |
| L9a | The engine's first frame without the black (L9's `BUG:`; the user queues it or leaves it for 0.7.1) | Opus, medium | winston, tablet | L9 |
| L10 | Phone pass: Live on the FP6 for a few days | Opus, high | a day session, **FP6, the user** | L9 |

```
claude --model opus --effort medium "Do L0 in .claude/skills/build-apodroid-android/live-0.7.0.md"
ANDROID_SERIAL=HA24BZ2Y claude --agent rosie --model opus --effort medium "Do L1 in .claude/skills/build-apodroid-android/live-0.7.0.md"    # after W; beside O8 (one tablet session at a time)
claude --model opus --effort medium "Do L2 in .claude/skills/build-apodroid-android/live-0.7.0.md"    # with the user, the FP6 attached
scripts/pipeline.sh L3    # then L4, L5, L6, L7, L8 (the queue rows in night.md, once 0.7.0's queue starts)
ANDROID_SERIAL=HA24BZ2Y claude --agent rosie --model opus --effort high "Do L9 in .claude/skills/build-apodroid-android/live-0.7.0.md"
claude --model opus --effort high "Do L10 in .claude/skills/build-apodroid-android/live-0.7.0.md"    # with the user
```

**For every package with a tablet check (rosie):**
- **Never `am force-stop`** while APODroid's live wallpaper is on: Android then drops it to the
  default wallpaper (above). `am kill` doesn't kill the wallpaper's process (not in the
  background); use `adb shell run-as io.github.buerlino.apodroid kill -9 <pid>` (debug builds),
  after which Android rebinds it (1.5 s, L1). **Never a second kill within 10 s of the first**
  unless the check asks for it: Android then reverts the home screen to the default and leaves the
  engine on the lock screen (Decision 12). Wait for the rebind's `Live: engine … created` and 10 s.
- **Set the tablet's wallpaper back** at the end as it was found (noted with `dumpsys wallpaper`
  first): APODroid's still wallpaper by a Set with Size Fill, or the system's by picking it.
- **Applying Live** on the tablet:
  `adb shell am start -a android.service.wallpaper.CHANGE_LIVE_WALLPAPER --ecn android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT io.github.buerlino.apodroid/.LiveWallpaper`,
  then the picker's buttons by `ui.sh` (L1: "Set wallpaper", then "Home screen" or "Home screen
  and lock screen"; no question while the engine is already on both).
- **Provoking Decision 12's case** (home reverted, the engine left on the lock screen): with
  Live on both, `run-as … kill -9 <pid>`, wait for the rebind (`pidof`, about 1.5 s), kill the
  new pid again, all within 10 s. Expected in `dumpsys wallpaper`: home the default (`mWhich=1`),
  the lock screen still APODroid's (`mWhich=2`); logcat "Reverting to built-in wallpaper!". A kill
  also ends a job run in progress (B19's retry).
- **Reading the state:**
  - `adb shell dumpsys wallpaper` (the components, which screens; no colours on the tablet: those
    in `logcat`'s `ThemeOverlayController` lines);
  - `adb logcat -d -s APODroid` (`Live: …`);
  - `adb shell dumpsys activity processes | grep -A4 apodroid` (the oom adj);
  - `adb shell dumpsys meminfo io.github.buerlino.apodroid`;
  - the screen off and on with `adb shell input keyevent SLEEP` and `WAKEUP`, a frame right after
    with `adb shell "input keyevent WAKEUP; screencap -p /sdcard/wake.png"`.

### [ ] L0. The probe: a throwaway live wallpaper (by day, not the pipeline; Opus, medium)

J0's and O5's way: a throwaway app in `.coord/probe-l/` (gitignored), its own
`settings.gradle.kts`, built with the repo's wrapper (`./gradlew -p .coord/probe-l`), plain
Android (no Compose, no library), `minSdk` 29, `compileSdk` and `targetSdk` 37, package
`io.github.buerlino.apodroid.probe.live`. Permissions: `SET_WALLPAPER` only (for `Still`).
- **`ProbeWallpaper`**, a `WallpaperService` declared as L5 will (`BIND_WALLPAPER`, exported, the
  meta-data XML with a thumbnail and a description). Its engine:
  - draws `files/pic` (pushed with `run-as`), decoded at the surface's height (the pref `capMb`, 0
    for none), at L3's position from a start time (the pref `warp`: minutes of pan a real second,
    0 for the real clock), with the time, p and the engine's flags in large text over it (so a
    screenshot shows which frame it is);
  - takes `lockCanvas()` or `lockHardwareCanvas()` by the pref `canvas=sw|hw`;
  - redraws as L5 will (visible, the surface, each minute or each second with `warp`);
  - reports colours from the picture;
  - throws in `draw` while the pref `crash` is true.
- **It logs** to `files/log.txt` (a line each, local time with milliseconds):
  - `CREATE` (preview or not, flags on 34+, `getWallpaperId` of both screens), `FLAGS`
    (`onWallpaperFlagsChanged`), `VIS true|false`, `SURFACE <w>×<h>`, `REDRAW`;
  - `DRAW <sw|hw> <ms> p=<…>`, `DECODE <w>×<h> <ms> <MB>`, `COLORS`, `DESTROY`;
  - `/proc/self/oom_score_adj` at each `VIS`;
  - `getHistoricalProcessExitReasons` at each process start.
- **Activities** without a window, each logging what it did:
  - `Pick` fires `ACTION_CHANGE_LIVE_WALLPAPER` for the probe (logs the resolved activity or the
    exception);
  - `Info` logs `getWallpaperInfo()`, `getWallpaperInfo(FLAG_SYSTEM)` and `(FLAG_LOCK)` on 34+,
    and `getWallpaperId` of both, with no `<queries>` in the manifest;
  - `Still --es which home|lock|both` sets `files/still.jpg` with `setStream` and those flags;
  - `Set --es key value` writes a pref.
- **Done when:** `probe-live.apk` built in `.coord/probe-l/`, its `README.md` lists every
  command; no device (the trial is L1's first step).

### [ ] L1. Android check: the live wallpaper on the tablet (Opus, medium)

(Agent rosie, the tablet, after W and L0; one session of about 90 min, beside O8 but not at the
same time. Read this file's "What Android gives" first.) Note the tablet's state as "For rosie"
says, and `dumpsys wallpaper` before (W leaves APODroid's still wallpaper on it). APODroid's daily
switch off for the session, so its job doesn't set a still picture over the probe; back on at the
end. A picture: the widest real APOD in `private/pictures/` (by `imageBounds`), its rendition at
1340 high, and an 8000-px-wide panorama for the budget.
1. **Trial:** install `probe-live.apk`, push the picture, `Pick`: the log shows the resolved
   activity, the picker opens with the probe drawing (a screenshot). A probe bug: STOP.
2. **The picker:** `cmd package resolve-activity -a android.service.wallpaper.CHANGE_LIVE_WALLPAPER`
   and `-a android.service.wallpaper.LIVE_WALLPAPER_CHOOSER`. Each of the picker's screens as a
   screenshot, with its buttons' labels from `uiautomator dump`. Does it ask home, lock or both?
3. **Flags and engines:** apply each choice it offers in turn. Each time `dumpsys wallpaper`, the
   log's `CREATE`/`FLAGS`, and `Info`'s line. Then, with the probe on both: `Still --es which lock`
   (expected: `FLAGS 1`, the lock screen the still picture), apply on both again,
   `Still --es which home` (expected: `FLAGS 2`). If the picker offers home alone: on both, then
   home alone again (expected: two engines, `CREATE` with flags 1 and 2). **While one engine is
   shared** (flags 3): `Info`'s `getWallpaperInfo(FLAG_LOCK)` and `getWallpaperId(FLAG_LOCK)`
   (AOSP reads null and -1), so L5 knows how to tell shared from home only. If the picker
   offers "Lock screen" alone with the probe on home: a second engine (`CREATE` flags 2) beside
   the first (2026-10-28's Decision 10). The time from the picker's "Set" to the engine's `CREATE` (the
   note's delay, for Set's 3 s wait).
4. **The first frame:** `warp` 10 (10 min of pan a second), screen off 60 s, then `adb shell
   "input keyevent WAKEUP; screencap -p /sdcard/l1-wake.png"`, three times; then the same with the
   lock screen dismissed (`wm dismiss-keyguard`) and a screenshot of home. Does the first
   screenshot show the old time over the picture? `VIS true` → `DRAW` latency from the log.
5. **Draw time and memory:**
   - `canvas=sw` and `hw`, `warp` 0: ten draws each (screen off and on), the log's `DRAW` ms.
   - `capMb` 0 with the widest picture and with the panorama (decoded at 1340, and at 2484 to
     stand in for the FP6: the pref `height=2484`): `dumpsys meminfo` (Native Heap, Graphics,
     TOTAL PSS) and `DECODE`.
   - `hw` with the panorama: drawn, or "too large"?
6. **The process:** `dumpsys activity processes | grep -A4 probe.live` and the log's adj, with
   the screen on (home), with an app on top, and the screen off for 2 min and for 10 min.
7. **Its life:**
   - `run-as … kill -9 <pid>`: rebound (`CREATE`), the wallpaper still the probe.
   - Twice within 10 s: the default wallpaper?
   - `crash` true: the default?
   - `am force-stop`: the default (AOSP)?
   - Re-apply after each. `install -r` of the same APK: still the probe? A reboot
     (`wm dismiss-keyguard` after): still the probe, `CREATE` after the boot?
8. **ZUI's cleaner** (O8's memory pressure with O5's `.hog`, if O8 runs that day; else `.hog`
   alone to 90% of the memory): the probe as the wallpaper, the screen off; `ZuiMemoryCleaner`
   lines in `logcat`. Is the probe stopped, and is the wallpaper still the probe? Name every app
   the cleaner stopped, for the user to reopen (O8's go covers it, 2026-10-28).
9. **Colours:** `dumpsys wallpaper`'s colours for the probe before and after a new picture
   (`Set pic` …).
10. **Set back:** the probe uninstalled, the tablet's wallpaper as before (APODroid's daily switch
    on, then a Set with its setup, or the system's), the settings as found; `.coord/runs/l1/report.md`.

- **Decides:**
  - the canvas: software unless its draws exceed about 30 ms, or hardware is clearly better and
    draws the panorama;
  - the budget: the largest bitmap that leaves the process under about 150 MB with two engines;
  - whether the split works as AOSP reads, for L5's rule;
  - the first frame: if stale, a question for the user;
  - the oom adj, and whether ZUI spares a live wallpaper, for track O's seams.
- **Done when:** every step ran or says why not, the table and the recommendations written, the
  tablet set back. Verdict PASS, or STOP when Android does what this file didn't expect.
- **Result** (rosie, pipeline run 95, 2026-11-14, the tablet, Android 15, ZUI; the probe's logs,
  the dumps and the screenshots in `.coord/runs/095-l1/`, the report `report.md` there): **STOP**.
  Android's picker and its revert paths don't do what "What Android gives" read. Every step ran.
  The pictures: 2026-05-12's APOD (6090×2085, the widest of `private/pictures/`), its rendition at
  1340 high (3914×1340) and an 8000×2500 panorama made from it.
  - **1. Trial:** `Pick` opened `com.android.wallpaper.livepicker/.LiveWallpaperChange` (`PICK
    resolved=null` without `<queries>`, then `started`), the probe drawing in its preview
    (`CREATE preview=true flags=1`). No probe bug.
  - **2. The picker:** `CHANGE_LIVE_WALLPAPER` → `livepicker/.LiveWallpaperChange`,
    `LIVE_WALLPAPER_CHOOSER` → `livepicker/.LiveWallpaperActivity`. The preview: "Probe Live",
    "Set wallpaper" (and "Preview", ←). "Set wallpaper" asks **"Home screen" or "Home screen and
    lock screen"; never "Lock screen" alone.** While the probe is already on both screens it asks
    nothing and applies to both at once.
  - **3. Flags and engines** (the table below). **"Home screen" always ends as one engine on both
    screens** (`mWhich=3`, no lock wallpaper): over the shared still picture (AOSP: "also a lock
    wallpaper if nothing is set") and also with the probe on the lock screen alone and a still on
    home, where it destroyed the lock engine and made one shared one. **Two engines never
    happened**; home alone comes only from a still set on `FLAG_LOCK`. The split by a still set
    works as AOSP reads: `Still lock` → `FLAGS 1(home)`, `Still home` → `FLAGS 2(lock)`, the same
    engine, no rebind. Applying "both" over home-only also keeps the engine (`FLAGS 3`).
    **Decision 10 can't work here:** the picker offers no lock screen, and "Home screen" over a
    lock-only probe makes it shared. Set → `CREATE`: 0.16–0.18 s (Set's 3 s wait is ample).

    | State (`dumpsys wallpaper`) | Engine flags | `getWallpaperInfo()` / `(SYSTEM)` / `(LOCK)` | `getWallpaperId` home / lock |
    |---|---|---|---|
    | Shared (`mWhich=3`, no lock entry) | 3 | probe / probe / **null** | n / **-1** |
    | Home only (still on lock) | 1 | probe / probe / null | n / m |
    | Lock only (still on home) | 2 | null / null / probe | n / m |
    | After two deaths in 10 s or a crash loop: home default, lock still the probe (`mWhich=2`) | **3** (new engine) | null / null / **null** | n / m |

    So L5's rule holds while nothing reverted: shared = `(SYSTEM)` names it and the lock id is -1;
    home only = `(SYSTEM)` names it and the lock id isn't -1; lock only = `(LOCK)` names it. An app
    sees its own package without `<queries>`.
  - **4. The first frame:** on the lock screen the capture right after `WAKEUP` showed **the frame
    drawn before the screen went off** in 2 of 3 (the third capture was blank, the screen not yet
    on): "drawn 14:27:24" at 14:28:24, "drawn 14:28:29" at 14:29:28. The new frame follows
    `VIS true` by 10–16 ms, `VIS true` about 0.3 s after the wake; the lock screen is still dim
    then. Home after `wm dismiss-keyguard`: the fresh frame. **Stale for about 0.3 s: a question
    for the user** (L2 looks by eye on the FP6).
  - **5. Draw time and memory** (one engine, `capMb` 0):

    | Canvas | Picture, decoded | Draw ms | Native Heap MB | Graphics MB | TOTAL PSS MB |
    |---|---|---|---|---|---|
    | sw | widest 3914×1340 (21 MB) | 6–14 (10 wakes) | 29 | 6 | 70 |
    | sw | panorama 4288×1340 (23 MB) | 7 | 31 | 6 | 78 |
    | sw | widest 6090×2085 at `height=2484` (51 MB, not enlarged) | 20 | 58 | 6 | 105 |
    | sw | panorama 7948×2484 (79 MB) | 23 | 86 | 6 | 132 |
    | hw | widest 3914×1340 | 2–4 (10 wakes) | — | — | — |
    | hw | panorama 4288×1340 | 6 | 32 | 75 | 148 |
    | hw | panorama 7948×2484 | 1–20 | 87 | 70 | 197 |
    | hw | widest 6090×2085 | 3 | 59 | 145 | 245 |

    `hw` drew the 79 MB panorama (no "too large"), at up to twice the memory (the texture).
    Decodes: 53–117 ms (21 MB) to 500 ms (79 MB). **Mixing canvases on one surface breaks it:**
    sw → hw aborted the process (`SkiaVulkanPipeline::getFrame()`, "getFrame() called on a context
    with no surface!", rebound at once); hw → sw gives `lockCanvas()` null ("no canvas") until the
    process restarts. L5 keeps one canvas for an engine's life.
  - **6. The process:** `oom_score_adj` **100** throughout: screen on with home (`BFGS`), with an
    app on top (`BFGS`), screen off 2 min and 10 min (`IMPF`).
  - **7. Its life:** `kill -9`: rebound in 1.5 s (`CREATE`), still the probe. **Twice within
    10 s: only the home screen reverts** ("Reverting to built-in wallpaper!", home `mWhich=1`
    `SET_LIVE_TO_CLEAR`); the lock screen keeps the probe (`mWhich=2`), drawn by a new engine
    that reports flags 3 while `getWallpaperInfo(FLAG_LOCK)` is null. **`crash` true** (a throw
    in the draw): a crash dialog first ("Probe Live has stopped", App info / Close app; the
    process lives until it's closed), then the rebinds crash twice in 1.5 s and the same as two
    kills: home default, the lock screen still the probe. `am force-stop`: **both screens
    default** (AOSP). `install -r`: still the probe, rebound (`PACKAGE_UPDATE_FINISHED`, exit
    `PACKAGE_UPDATED`). Reboot: still the probe, `CREATE` 1.5 s after `boot_completed` (no exit
    reason recorded for the reboot).
  - **8. ZUI's cleaner** (`.hog`, 3000 MB, from 2.25 GB available, the screen off):
    `ZuiMemoryCleaner` (Moderate then Aggressive, `minAdj: 701`) force-stopped 20 apps and
    **spared the probe** (adj 100). At the hog's peak (2.5 GB, 176 MB available) the low-memory
    killer killed it among 27 (`oom_score_adj 100`, exit `LOW_MEMORY`); **the wallpaper stayed the
    probe**, rebound 6 s later. Stopped by the cleaner, for the user to reopen:
    `dev.imranr.obtainium.fdroid`, `com.zui.camera`, `com.android.shell`,
    `com.android.managedprovisioning`, `com.google.android.as.oss`,
    `com.android.providers.calendar`, `com.google.android.configupdater`,
    `com.google.android.apps.carrier.carrierwifi`, `com.google.android.cellbroadcastreceiver`,
    `com.google.android.partnersetup`, `com.yha.factory`,
    `com.google.android.apps.setupwizard.searchselector`, `com.google.android.packageinstaller`,
    `com.aura.oobe.lenovo`, `com.android.providers.contacts`, `com.google.android.apps.turbo`,
    `com.motorola.mobiledesktop.core`, `com.android.providers.downloads`,
    `com.google.android.permissioncontroller`, `com.tblenovo.tabpushout`. The low-memory killer
    also killed Syncthing-Fork, Tailscale, FlorisBoard, Olauncher and ZUI's launcher (they restart).
  - **9. Colours:** `dumpsys wallpaper` lists none on this build. The engine's colours reached
    SystemUI (`ThemeOverlayController: onColorsChanged … which 3`) after a new picture, and it
    applied the earlier ones (held, wallpaper colours off).
  - **10. Set back:** the probes uninstalled, APODroid's Set (APOD · both screens, daily off as
    found) put its APOD on both (one shared still, as before), its prefs restored byte for byte,
    the settings as found (`accelerometer_rotation` came back 0 after the reboot: set to 1).
  - **Recommendations:** **the canvas: software** (6–23 ms, never 30; hardware is faster but
    costs up to twice the memory and gains nothing at a frame a minute). **The budget: 50 MB a
    bitmap** (a process's base is about 50 MB PSS; two engines with 50 MB each stay near 150 MB;
    on this tablet only one engine ever runs, which with 79 MB stayed at 132 MB). **The split**
    works as AOSP reads while nothing reverted (the table); the engine shouldn't trust its flags
    after a revert (it said 3 on the lock screen alone), and L5's rule then sees the lock screen
    as not live and sets Fill there, which replaces the stray engine. **The first frame:** stale
    for about 0.3 s on the lock screen, the user's question. **The oom adj: 100**, below ZUI's 701:
    the cleaner spares a live wallpaper's process, so its alarms survive (track O: Live gives what
    Keep running gives, without the notification). **For the user (STOP):** Decision 10 (a
    second engine for the lock screen) has no way through this picker; the picker's "Home
    screen" makes it shared; a double death reverts home only.
- **Decided from it** (user, 2026-11-14): Decisions 10–12 ([Decided after L1](#decided-after-l1-user-2026-11-14)):
  Live on home, the lock screen Fill when its picture differs; the 0.3 s wake frame accepted;
  home lost Live → both screens Fill. Software canvas and 50 MB a bitmap taken into L3 and L5.

### [ ] L2. Android check on the FP6: the picker and the first frame (Opus, medium)

(A day session with **the user**, after the tag and L0; the FP6 is the user's phone; about 30 min.)
`probe-live.apk` is a separate package: APODroid's data and setup aren't touched. APODroid's
daily switch stays as the user has it; the probe goes on the home screen for the session only, and
APODroid's next change would replace it anyway.
1. Install the probe, push a picture, `Pick`: screenshots of LineageOS's picker. Does it ask home,
   lock or both? Apply "Home screen" (or as it does) over the shared still; `dumpsys wallpaper`,
   `Info` (L1's table: shared, or home alone?). Then `Still --es which lock`: `FLAGS 1`, home
   alone? If the picker offers "Lock screen": note what it does, not used (Decision 10); a reason
   to use it goes to the user.
2. The first frame: `warp` 10, the screen off 60 s, then the user wakes the phone with the power
   button and says what they see. Also with a `screencap` right after `WAKEUP`, three times. The
   tablet's 0.3 s is accepted (Decision 11); much longer goes back to the user.
3. `DRAW` ms for `sw` at 2484 high (L1 decided software); `dumpsys meminfo`.
4. Back: the user picks their wallpaper again (or APODroid's Set), the probe uninstalled.
- **Done when:** the answers written to `.coord/runs/l2/report.md`; the user has seen the first
  frame.

### [ ] L3. `:core`: the position by the clock, the decode size, still or engine (Opus, medium)

(This file, The position; `Store`, Size and Set. After O1, which moves `PictureTime`'s constants,
and L1, whose step 3 told how a shared engine is told apart (Decisions 10 and 12).)
Plain Kotlin, JUnit.
- **`livePosition(now, zone, time: PictureTime, turns: Boolean): Float`:** T =
  `time.last(now, zone, turns)`, span = the shorter of `time.next(T, zone, turns) − T` and 18 h
  (`liveSpan`), p = (now − T) / span clamped to 0..1.
- **`liveDecodeSize(width, height, screenHeight, maxBytes): Pair<Int, Int>`:** the picture's
  shape at the screen's portrait height (landscape draws it scaled down, so a rotation decodes
  nothing), never larger than the picture, smaller while width × height × 4 is over `maxBytes`
  (`LIVE_DECODE_BUDGET`, 50 MB, L1).
- **`stillOrLive`** (the name is winston's), the rule of "`Store`, Size and Set": from Size,
  whether home is live, whether the engine covers the lock screen (shared), which screens have a
  column, and each screen's picture after the plan, return per set the screens the engine takes
  (home, and the lock screen only while shared with home's picture) and those set still.
- **Lost Live** (Decision 12) as a `:core` function beside `Record.forget()`: from the records'
  live notes, home live, shared and `live.shared`, which records to forget and whether a lock
  screen without a column takes home's picture.
- **Tests:**
  - p at 06:00, 12:00, 18:00, 24:00 and 03:00 with one source (0, ⅓, ⅔, 1, 1);
  - every 12, 6 and 3 h (each turn 0 → 1), every 24 h with two sources (18 h, then still);
  - "New picture at" 00:00 and 23:00;
  - the DST nights in Zurich (spring and autumn) with 00:00 and with 3 h turns;
  - a zone change (the change times follow);
  - the decode size for a 3:2 at 2484, the widest real one, Earth's 1440×600 (not enlarged), a
    12000×3000 panorama (under the budget; the widest at 2484, 53 MB, too);
  - the rule: Size Fill (all still); Live on home only (lock still); one engine on both, the same
    picture (the engine, no still set); different pictures (lock still, before home's
    hand-over); Size Live with home not live (all still, also a lock screen the engine still
    shows); no home column (all still); a screen without a column (nothing);
  - lost Live: a record noted live whose screen isn't live is due again (its picture
    forgotten), with one source and with turns, and keeps its owner and stamp; **home lost**
    (Decision 12): both records forgotten and both sets still, with equal and with different
    columns; a lock screen without a column with `live.shared` due home's picture (one set, both
    flags), without it left alone; the lock screen alone not shared any more: only it forgotten.
- **Done when:** the tests pass; `CLAUDE.md`'s 0.7.0: Live section says the position rule; the
  skill's "Where things are".

### [ ] L4. The drawing: one function for the whole picture, the engine and the preview (Opus, medium)

(This file, The drawing. After O2, which looks ⛶'s noted part up once.) No behaviour change.
- **`Drawing.kt`** (`:app`): draws a decoded picture onto a `Canvas` of a given size, panned (the
  frame at p filling it, `wallpaperFrame`) or whole (`fitFrame` on black or the blurred
  background: today's `whole()`, `hasBlackEdges` and `blurred` move there).
- **`Store.set`'s whole picture** draws through it into its bitmap.
- **Files:** `Drawing.kt` (new), `Store.kt`; the skill's "Where things are".
- **A debug dump**, the package's first commit (before the move): in a debuggable build
  (`ApplicationInfo.FLAG_DEBUGGABLE`) `Store.set` also writes the whole picture's bitmap it sets
  to `files/debug-whole.png`. Screenshots can't compare (the status bar's clock and the
  launcher's widgets change between shots); the bitmap can. The dump stays for L5's checks.
- **Tablet:** debug builds of the first commit (before; rosie builds it at the commit winston's
  report names) and of the tip (after), the same setup and the same posts: a whole picture on
  blur and one on black set on both screens, each time `run-as … cat files/debug-whole.png`.
  `magick compare -metric AE` of before and after gives 0 for both. Then Fill at a moved part
  (as before) and the smoke list.
- **Done when:** the whole picture's bitmap is pixel-identical before and after;
  `:app:compileDebugKotlin` and the check pass; the tablet passes.

### [ ] L5. The engine, and `setDue` leaves a live screen to it (Opus, high)

(This file, The engine; `Store`, Size and Set, its first half. After L1, L3, L4.) No setting yet:
on `dev` between L5 and L6 the app treats Live as on while Android's picker put APODroid's
engine on home.
- **`LiveWallpaper.kt`**, the manifest entry, `res/xml/live_wallpaper.xml` and
  `res/values/strings.xml` (the description), as This file says. As L1 decided: a software
  canvas, one for the engine's life (a null canvas skips the frame), 50 MB a bitmap.
- **The engine draws only Live's copy** (`live.jpg` and the whole flag beside it), home's
  picture whatever its flags, and catches everything in decode and draw, as This file says; the
  stray's log line (A stray).
- **`Store.liveOn(HOME)`** and `liveShared` from the system's answers (L1's table), the
  `live.homeId` note below 14; `live.shared` from `liveShared` at each run's start while home is
  live.
- **`setDue`** with L3's rule as for Size Live: home gets its copy, then its record (noted
  `wallpaperLive`) and `wallpaperSetAt`, no `WallpaperManager` call, `setsSinceSetup` true, logged
  `wallpaper shown live: <picture> on <screens>`; the lock screen with another picture, or not
  shared: its still set with `FLAG_LOCK` first (Decision 10). **Lost Live:** before the rules run,
  L3's function forgets the records (home's loss: both, Decision 12), so the same run sets Fill.
- **Files:** `LiveWallpaper.kt` (new), `Store.kt`, `AndroidManifest.xml`, the two resources,
  `CLAUDE.md` (The app, Wallpaper: Live; Next features: the decisions of this file), the skill's
  "Where things are" and its notes on `am force-stop` with Live.
- **Tablet** (this file's notes for rosie):
  - Apply Live through the picker ("Home screen and lock screen"; APODroid's daily switch on, the
    posts faked a day back). Expected: `Live: engine 3 created`, a frame drawn.
  - `run -f 1`: the new post goes on as `shown live` (no `wallpaper set:` line), the engine
    redraws (`Live: drew`), `dumpsys wallpaper` still APODroid's live one.
  - The pan: `cmd alarm set-time` forward (auto_time 0, forward only, the skill's how) to 12:00
    and 23:30: screenshots (the frame a third and nearly all the way across).
  - A whole post (⛶'s toggle and Set): drawn whole and still on blur.
  - "My picture" on a faked video day: panned.
  - With "More NASA pictures" on and different columns: the lock screen gets its still set
    (`wallpaper set: … on LOCK`), the engine's flags change to 1 (home), home still moving.
  - `run-as … kill -9`: rebound, the same frame.
  - The low-memory killer's kill (`.hog` as L1's step 8, the screen off): the wallpaper stays
    APODroid's, rebound about 6 s later, the same frame.
  - **The copy, not the source's file** (each after `kill -9`, so nothing is left in memory):
    - a newer post stored but not due: one source, the clock after the window, a post faked
      published after it (the skill's prefs edits; `<source>.published` later than the change
      time + 3 h), a pull with the daily switch off to store it, then `kill -9`: the engine draws
      the record's picture (the log names it), not the new one;
    - a "Skip it" video day (APOD faked a video, `apod.jpg` gone after the refresh), `kill -9`:
      the old picture still drawn;
    - a reboot: the same.
  - **Lost Live:** another wallpaper picked in the system for home (or `am force-stop`, then the
    page opened): the next run (`run -f 1`, or the page open) sets Fill on both screens at once
    (`wallpaper set:`), the records no longer noted live.
  - **Home reverted, the lock screen kept** (Decision 12; "Provoking Decision 12's case" above),
    with equal columns and again with different ones (Live on home only: then the lock screen is
    already Fill, nothing stray): `dumpsys wallpaper` home default, the lock screen APODroid's;
    then WAKEUP (with L8: the stray starts the run) or `run -f 1`: both screens still Fill
    (`wallpaper set:` on both, one set when equal), `dumpsys wallpaper` without APODroid's
    component, `Live: home isn't live` logged, the records no longer noted live; a further
    `run -f 1` hands nothing to the engine. Then Live back through the picker.
  - **No crash:** `live.jpg` truncated with `run-as` and the screen off and on: black,
    `Live: couldn't …` logged, no `CRASH` in `exit-info`, the wallpaper still APODroid's.
  - The process's oom adj with the screen off (L1: 100).
  - Then the smoke list, with Live off at its start (the system's wallpaper picked) and set back
    at the end.
- **Done when:** the tablet passes; the texts listed (the description).

### [ ] L6. Size [Fill | Live], Set opens Android's picker, the texts (Opus, high)

(This file, `Store`, Size and Set, its second half. After L5. Built without waiting for L2 (user,
2026-11-14): an L2 finding on the FP6 later becomes a fix package for the user to decide.)
- **The pref `size`** (`FILL`, `LIVE`): in `Setup`, `saveSetup`, the draft and its saved state,
  the highlight; Size Fill gives all still sets (L3's rule), a still set replacing APODroid's
  engine.
- **The row [Fill | Live]** in Setup after "Where" or the list (`Segments`), always shown.
- **Setup's summary** " · Live".
- **Set's flow** as This file says: the picker when home has a column and isn't live, or, on 14+
  and once per split, when this Set makes differing stored columns equal while the lock screen
  isn't shared (never below 14, never for the lock screen alone); the
  intent started directly, the chooser or the toast on `ActivityNotFoundException`; the ids noted
  before, the wait for `liveOn(HOME)` after; the update after the return (the lock screen's still
  Fill when its picture differs, Decision 10), Fill after backing out.
- **The status line** "Live isn't on…" while it applies.
- **Files:** `MainActivity.kt`, `Store.kt`, L3's rule if it needs the Size input; SIM's invariant
  (this file; `RulesSimulationTest` with a live screen); `CLAUDE.md` (Settings: Size; Set as
  wallpaper: the picker); the skill.
- **Tablet:**
  - From 0.6.0's prefs (no `size`): Fill, nothing changes, no picker.
  - Size Live and Set: the picker opens.
    - Back out: Fill sets, the status line.
    - Again and confirm both: `shown live` on both, the summary " · Live".
  - Size Fill and Set: still sets, `dumpsys wallpaper` shows no live component.
  - Live with "Where" Home: the picker; the lock screen left alone (over a shared still it shows
    the engine, home's picture, as the shared still did).
  - Live with "Where" Lock: no picker, the lock screen Fill.
  - **Decision 10:** "More NASA pictures" on, home APOD, lock Earth, Live on none: Set opens the
    picker once; each of "Home screen" and "Home screen and lock screen" in turn: home moving
    (`shown live` on HOME), the lock screen's still Fill of Earth right after (`wallpaper set: …
    on LOCK`, the engine's flags 1); a second Set doesn't open the picker.
  - **Equal columns after the split:** the columns made equal and Set: the picker opens once (the
    lock screen not covered); "Home screen and lock screen": shared (`getWallpaperId(FLAG_LOCK)`
    -1), the lock screen's record noted live, no still set. Again from the split with "Home
    screen", and once with a back-out: note what ZUI does (Open); if it stays home only, the lock
    screen Fill of the same picture, and a second Set with the same columns opens **no** picker.
  - Equal columns from the start (APOD mode, "Where" Both), Live on home only (a still set on the
    lock screen by hand): Set opens no picker, the lock screen Fill.
  - After confirming the picker, Set's update hands the screens to the engine (`shown live`),
    no still set over the live wallpaper just picked (`dumpsys wallpaper`).
  - Another wallpaper picked in the system for home: the next `run -f 1` sets Fill on both, the
    status line says Live isn't on; Set opens the picker again.
  - Decision 12's case (provoked as above): after the run both Fill, the status line, and Set
    opens the picker.
  - The highlight and TalkBack's "not set yet" on the row.
  - Screenshots of the row, the summary, the status line and the picker's first screen for the
    user. Then the smoke list.
- **Done when:** the tablet passes; SIM's invariants hold over its seeds; the texts listed.

### [ ] L7. The preview in ⛶ mode (Opus, medium)

(This file, The preview; Decision 7. After L6.)
- **⛶ with the draft's Size Live** and the shown source in home's column: the frame at
  `livePosition` for home's column, no drag (the semantics without a drag action), moved by a
  minute tick while open; a source only on the lock screen: ⛶ as Fill (Decision 10).
- **"Whole picture" on** (Fill or Live): the screen-shaped preview drawn by L4's function in the
  picture's place, with its background.
- **✓, Back, the highlight, the draft** as A4.
- **Files:** `MainActivity.kt`; `CLAUDE.md` (Choosing the part: the preview; Live).
- **Tablet:** Size Live, ⛶ at a faked 12:00 (the frame a third across, a drag doesn't move it).
  Whole picture on a planet on black and a nebula on blur: the previews beside screenshots of the
  wallpaper after Set (the same look). Different columns: a lock-only source's ⛶ drags as Fill.
  Size Fill: ⛶ as before. Rotation closes it as now.
  Screenshots for the user; the smoke list.
- **Done when:** the tablet passes; the texts listed (none expected beyond the semantics).

### [ ] L8. D: the screen-on check starts the alarms' run (Opus, medium)

(This file, D; `on-time-0.7.0.md`, D and its Seams 1. After L5 and O4 with its packages, which may
change the run's bookkeeping; with O10 the run is expedited.)
- **In the engine's `onVisibilityChanged(true)`** (not the preview), while `daily` is on: when
  `Store.nextAlarm()` ≤ now, or the engine is a stray (14+, home not live; Decision 12) while a
  record is still noted live (`wallpaperLive` on either screen), and `lastRunAt` is older than
  15 min, `DailyJob.runNow(context)`. Logged `Live: screen on, run
  owed`. Nothing else: the run is job 2's.
- **Files:** `LiveWallpaper.kt`; `CLAUDE.md` (Background update: D); the skill.
- **Tablet:**
  - Live on.
  - **The alarms cancelled as a maker would:** `cmd appops set io.github.buerlino.apodroid
    RUN_ANY_IN_BACKGROUND ignore` ("Restricted"; noted and set back). Whether it holds the bound
    wallpaper's alarms is part of the check; else the alarms cancelled through a debug-only path
    the package adds, named in its report.
  - The clock stepped past a change time with a new post faked, the screen off. Then WAKEUP:
    `Live: screen on, run owed`, then job 2 runs and the engine shows the new picture.
  - **Not:** a second WAKEUP within 15 min; nothing owed; the daily switch off; the picker's
    preview.
  - **The stray:** Decision 12's case provoked (L5's notes), nothing owed, `lastRunAt` older than
    15 min: WAKEUP on the lock screen starts the run (`Live: screen on, run owed`), both screens
    Fill. With "Where" Home and no `live.shared` (nothing to replace on the lock screen): the first
    WAKEUP starts one run, which removes the notes; a WAKEUP 15 min later starts none.
  - Then the smoke list.
- **Done when:** the tablet passes.

### [ ] L9. Tablet pass of the track, and two real days with Live (Opus, high)

(Agent rosie, the tablet; V11c's form on the tree with L3–L8.)
- **The debug build, then R8** (sign as the skill says): L5's, L6's, L7's and L8's checks again,
  quickly.
- **The update from 0.6.0's build** with its setup: Size Fill, nothing changes.
- **Live with:**
  - turns every 6 h (the frame across within the turn at a faked 08:00 and 11:00);
  - a video day with each choice;
  - "my picture";
  - a whole post;
  - Set back to Fill;
  - a reboot;
  - `install -r` (still live);
  - L5's `kill -9` checks again (a post stored but not due, a "Skip it" video day) and lost
    Live after `am force-stop`;
  - Decision 10: different columns, home moving and the lock screen Fill with its own picture,
    across a change time (the lock screen's still set, home's hand-over, no picker);
  - Decision 12: two kills within 10 s, then both screens Fill by the next run, on R8 too;
  - landscape (the picture at the landscape height).
  - BR3: a bright picture with Live, the engine and the preview darkened (the log's ", darker ×…");
  - L6f1: the lock screen's still set failing (if it can be made to fail) holds home's hand-over;
  - B27: home and lock on one engine, lock with APOD alone on a "Skip it" video day, home's new
    picture handed over: the lock screen keeps its picture (a still from `live.jpg`).
  (The night of 14 Nov ran none of L5–L8's, BR3's, L6f1's or B27's device checks.)
- `logcat` and `exit-info` without a crash; `dumpsys meminfo` with the page open and closed.
- **Then two real days** (after the tag since 2026-11-14, the user's own use, `CLAUDE.md`, 0.7.0's release gate; not the pipeline's run) (W's way, by hand, morning reads; the tablet on battery at night as W did;
  not during O16's week): the change at 06:00 on the live screens, the frame's place at each read,
  the engine alive, `ZuiMemoryCleaner` lines, the wallpaper still APODroid's.
- The texts' screenshots in one folder for the user.
- **Done when:** every check ran on R8 too; no `BUG:` line, or each drafted as
  a package.
- **Result** (rosie, pipeline run 116, 2026-11-14, 21:59–22:23 on the tablet; dev 2706a27,
  `app-debug.apk`, then R8 zipaligned and signed with the debug key; evidence in
  `.coord/runs/116-l9/tablet/`): **one `BUG:` line, drafted as L9a**; the rest as the plan says.
  - **Debug:** APOD mode, Size Live, Set: the picker, "Home screen and lock screen": `shown live
    … on HOME+LOCK`, flags 3, p=0.89 at 22:00 (16 h of 18). `kill -9`: rebound, the same copy,
    the same p. Decision 10 (home APOD, lock Earth): no picker, `wallpaper set: EARTH … on
    LOCK; shown live: APOD … on HOME`, flags 1. BR3 (a bright 2400×1340 test picture as
    `apod.jpg`, Set): `shown live … darker ×0.70 (grey 231)`, the engine `drew … ×0.70`; ⛶ with
    Live: a drag moves nothing (the screenshots identical), "Whole picture" shows the preview
    darkened on blur (`texts/debug-crop-whole-preview-bright-darkened.png`); ✓: `Part: wallpaper
    shown live … darker ×0.70 (grey 151)`, the engine decodes 800×1340 and draws still (p=0.50).
    Decision 12: two `kill -9` 3 s apart, `Reverting to built-in wallpaper!`, `run -f 1`: `Live:
    home isn't live, Fill on HOME+LOCK`, both still. B27 with turns every 6 h (home APOD + NASA,
    lock APOD, the picker for both, shared): APOD faked as a 15 Nov video day ("Skip it") by
    the kill protocol (the engine kept its APOD copy through both kills), the clock to 06:08 on
    15 Nov: job 2 expedited, `the lock screen keeps APOD … still`, flags 1, `shown live: NASA …
    on HOME`. The pan in that 6 h turn: p=0.34 at 08:00, p=0.84 at 11:01. "My picture" (a
    faked video day, "Use my picture", "Where" Lock → Both): the picker again (the split rule),
    `shown live: Own on HOME+LOCK`, `drew Own p=0.91`; "NASA Image of the Day" instead: `shown
    live: NASA … on HOME+LOCK`.
  - **R8:** `install -r` over the live debug build: still live (`PACKAGE_UPDATE_FINISHED`),
    `MY_PACKAGE_REPLACED, arming again`, the engine drew the same p. Decision 12 by two `am
    crash` 1.7 s apart (`run-as` doesn't work on R8): `Reverting to built-in wallpaper!`, `run
    -f 1`: `home isn't live, Fill on HOME+LOCK`. Size Live and Set: the status line "Live isn't
    on. Set as wallpaper turns it on."; the picker, both: B27 again (the lock screen, APOD alone
    on the video day, keeps the engine's NASA, still). `am force-stop`: Android cleared home,
    the page's load `home isn't live, Fill on HOME+LOCK`. A reboot: still live
    (`SWITCH_WALLPAPER_UNLOCK_USER`), `BOOT_COMPLETED, arming again`, the detector excused by
    the boot. A video day with "Earth Observatory" (home APOD + NASA at NASA's turn, lock APOD):
    `wallpaper set: EARTH … on LOCK; shown live: NASA … on HOME`, no picker. Size Fill and Set:
    `Live: engine home destroyed`, both still, no live component, the summary without " ·
    Live". From the backup's state with the bright picture: the picker backed out (`back from
    the picker, nothing changed, home not live`, Fill `darker ×0.70`, the status line), then
    confirmed: `shown live … darker ×0.70`, `p=0.91 ×0.70` at 22:18; ⛶: no drag, the whole
    preview darkened (`texts/r8-crop-whole-preview-bright-darkened.png`); "Where" Lock: no
    picker, `wallpaper set: … on LOCK`, home's engine left alone.
  - **The update from 0.6.0:** not run again: no app or `:core` file changed since O15's run
    (dev 67e841d → 2706a27 changed only docs), whose R8 update from 0.6.0's R8 build set
    nothing and kept the summary (no " · Live"; `on-time-0.7.0.md`, O15).
  - **D** (L8): a WAKEUP with nothing owed started no run. The owed case wasn't provoked:
    `appops RUN_ANY_IN_BACKGROUND ignore` doesn't hold the alarms while the live wallpaper's
    process is bound (the step past 12:00 ran job 2 at once), and a force stop drops Live.
  - **Not checked:** landscape (Olauncher keeps the home screen portrait: the engine drew
    portrait); L6f1 (no way found to make the lock screen's still set fail); L5's "a post
    stored but not due" after `kill -9` (only the "Skip it" video day); D's owed case and its
    stray; the two real days (the user's, after the tag).
  - **No crash:** the only `FATAL EXCEPTION`s were the shell's own `am crash`
    (`CrashedByAdbException`); `exit-info` before the reboot: that crash, PACKAGE UPDATED,
    SIGNALED (the `kill -9`s); none after. `dumpsys meminfo` (R8, Live on): 84 MB PSS with the
    page open, 47 MB after Home; debug, page closed: 85 MB.
  - **BUG:** a newly created engine (a rebind after a process death, after `install -r`, the
    picker's first preview) draws black for 0.7–1 s with the screen on, although its picture
    is decoded within 50 ms: `Live: decoded 1327×1340 in 42 ms` at 22:01:37.528, `drew black
    (nothing)` ×4, `drew Of(APOD …)` at 22:01:38.500; the same after each kill (0.7 s) and in
    the picker (0.7 s). Likely `colorsOf` (`WallpaperColors.fromBitmap`) on the decoder
    thread before `show()`. L9a.
  - **A log oddity, no BUG:** the hand-over's first draw can log the previous picture's name
    (`drew Own p=0.91` right after `shown live: NASA …`, `decoded 1340×1340`, NASA's size):
    the engine read `live.picture` before the hand-over's commit; the key has no name, so it
    doesn't reload. The picture drawn is right.
  - **Texts** (screenshots in `.coord/runs/116-l9/tablet/texts/`): Size [Fill | Live]
    (`debug-setup-size-live-draft.png`), the status line (`r8-page-live-isnt-on-status.png`,
    `r8-page-after-picker-back-out.png`), Setup's summary with " · Live"
    (`debug-page-live-status.png`, `debug-page-d10-home-apod-lock-earth.png`), the video-day
    choices with Size Live (`r8-setup-live-video-earth-draft.png`,
    `debug-video-day-use-my-picture-draft.png`).

### [ ] L9a. The engine's first frame without the black (Opus, medium)

(From L9's `BUG:` line, 2026-11-14; queued the same night, before T2.)
- **The bug:** an engine created with the screen on (a rebind after a process death, `install
  -r`, the picker's preview) draws black for 0.7–1 s before its picture, though the decode takes
  under 50 ms (L9's result).
- **The fix (to check first):** in `LiveWallpaper.kt`'s load, show the decoded picture at once
  and compute its colours afterwards (`colorsOf` then `notifyColorsChanged` from the decoder
  thread's second step), so the first frame is the picture; the colours still once per picture.
- **Tablet:** `kill -9` with the home screen on: the log's first `drew` after `engine … created`
  names the picture (no `drew black`), within about 0.2 s of the decode; the colours still told
  (`dumpsys wallpaper`'s colours for the engine). The picker's preview the same. Then the smoke
  list.
- **Done when:** the tablet passes; no `drew black` between the decode and the picture.
- **Result** (pipeline run 117, 2026-11-14, merged 4b5f44b, tablet PASS; `.coord/runs/117-l9a/tablet-0/`):
  the picture 46–160 ms after the decode (was 0.7–1 s), the colours still told (read from SystemUI's
  dump, not `dumpsys wallpaper`). 3–4 `drew black (nothing)` frames by the surface callbacks remain in
  those ms, so the Done when's letter isn't met; rosie passed it under the 0.2 s. `install -r` and a boot
  untested.

### [ ] L10. Phone pass: Live on the FP6 for a few days (Opus, high)

(**The user**: the FP6 is the user's phone. After L9; with O17 if both are ready.)
- The release-signed build over the user's install (the skill: testing an update on the user's
  phone): Size Fill as before.
- The user turns Live on with their setup (APOD + NASA, 12 h, equal columns) through Android's
  picker, with the answer L2 found; if the user tries different columns, the lock screen Fill
  (Decision 10). Over a few days:
  - the frame's place at a few times of day;
  - the change at 06:00;
  - whether Live stays on (the FP5's revert: `dumpsys wallpaper` each day; a revert of home
    alone then shows as both screens Fill, Decision 12);
  - the first frame when the phone wakes (the user's eye; about 0.3 s accepted, Decision 11);
  - the memory;
  - the battery (`dumpsys batterystats --charged io.github.buerlino.apodroid` against a day with
    Fill).
- Then the user keeps Live or goes back to Fill.
- **Done when:** the user has seen it; the texts approved or changed; `.coord/runs/l10/report.md`.

## Seams with track O

Aligned with `on-time-0.7.0.md`'s "Seams with track L" (2026-10-28), point by point:
1. **D is built here (L8), and it's job 2.** The engine schedules the alarms' run
   (`DailyJob.runNow`) instead of running `Store.update` itself. That answers O's question: the
   run notes `lastRunAt` as any run's start does, takes `JobRuns` (one run at a time) and counts its
   failures. So the detector counts it as a run, and a Live user's miss shows only when the screen
   stayed off past the change. O4's review should keep `runNow` callable from the engine (the main
   thread, no lock held). With O10 the run is expedited.
2. **The live wallpaper keeps the process alive, but a force stop drops it.** The binding
   (`BIND_FOREGROUND_SERVICE_WHILE_AWAKE`) and the rebinds are as O read. Two more from AOSP for
   O's table:
   - a **force stop makes Android revert to the default wallpaper**, so a cleaner that
     force-stops a live wallpaper's app takes Live away, not only the alarms;
   - while the screen is off the binding's foreground level lapses, so the adj then decides.
   **L1:** the adj stays 100 with the screen on and off (`BFGS`, `IMPF`), so ZUI's cleaner
   (`minAdj` 701) spared the live probe while it force-stopped 20 apps; the low-memory killer
   killed it at the hog's peak and Android rebound it 6 s later, still the wallpaper. So with
   Live on home, Live gives what Keep running gives on ZUI, without the notification; Keep running
   stays a plain switch (3), and a phone whose cleaner force-stops anyway loses Live on both
   screens (both default) and the alarms. A double death reverts home only (Decision 12).
3. **The detector doesn't suggest Live** (user, 2026-10-28, Decision 9), and **Live doesn't
   change the detector** (2026-11-14): it fires only when the alarms are gone, a force stop, which
   drops Live on both screens, so it never sees Live on and needs no `Store.liveOn`. Keep running
   stays a plain switch, not turned off by Live.
4. **No shared service, one process:** agreed. The engine is a `WallpaperService` in the app's
   process, no `android:process`.
5. **The set paths:** agreed. `Store.update` stays the one entry point for a run; L5 changes only
   `setDue`'s last step (a live screen gets its record, not a still set), and the engine hears it
   through `Store.watch`. O's packages that touch `setDue` or `Store.set` come before L4 and L5 or
   rebase on them.
6. **Memory:** the one engine holds one decoded picture (at most 50 MB, L1) for as long as Live
   is on: about 105 MB PSS with 51 MB (L1). O12 (the page's pictures let go while stopped) helps
   both; L9 measures with the page open and closed.
7. **Order and the pipeline:** O1–O4 first (L3 after O1, L4 after O2; L6's row is a segmented row,
   so O3's dropdown doesn't block it). One tablet: L1 and O6–O8 one at a time, L1 beside O8 (its
   cleaner step), L9's two real days not during O16's week. One `plan-0.7.0.md` (or
   `pipeline-lib.sh`'s `live <key>`) and the L rows in `night.md`, as O's 7 says.
8. **Permissions:** Live adds none (`BIND_WALLPAPER` is the service's guard, not a permission the
   app holds); track O adds `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SPECIAL_USE`.
9. **rosie's habits** (new): with Live on, `am force-stop` drops the wallpaper to the default on
   both screens, two kills within 10 s drop home's (Decision 12), and `am kill` doesn't kill the
   process. O's checks that force-stop the app (O14's "`am force-stop` after a run") must run with
   Size Fill, or expect the live wallpaper gone.

Sources (read 2026-10-28):
- **AOSP `frameworks/base`** (the GitHub mirror `aosp-mirror/platform_frameworks_base`):
  - `core/java/android/service/wallpaper/WallpaperService.java` (main): `getWallpaperFlags`,
    `onWallpaperFlagsChanged`, `onVisibilityChanged`, `setOffsetNotificationsEnabled`,
    `onComputeColors`, `notifyColorsChanged` and `NOTIFY_COLORS_RATE_LIMIT_MS`,
    `onApplyWallpaper`, `onCreateEngine(WallpaperDescription)`;
  - `core/java/android/app/WallpaperManager.java` (main, android14-release):
    `ACTION_CHANGE_LIVE_WALLPAPER`, `EXTRA_LIVE_WALLPAPER_COMPONENT`, `getWallpaperInfo` and its
    docs, `clear(int)`'s docs, `isLockscreenLiveWallpaperEnabled`;
  - `core/java/android/app/wallpaper/WallpaperDescription.java` (main);
  - `services/core/java/com/android/server/wallpaper/WallpaperManagerService.java`
    (android10-release, android12-release, android13-release, android14-release, main): the bind
    flags, `tryToRebind`, `MIN_WALLPAPER_CRASH_TIME`, the low-memory rebinds,
    `doPackagesChangedLocked`, `setWallpaperComponent` (13: "Live wallpapers can't be specified
    for keyguard"), `WallpaperDestinationChangeHandler`, `forceRebind`,
    `getWallpaperInfo(WithFlags)`, `getWallpaperIdForUser`, `persist.wm.debug.lockscreen_live_wallpaper`;
  - `core/java/com/android/internal/content/PackageMonitor.java` (main): `ACTION_PACKAGE_RESTARTED`
    as `PACKAGE_TEMPORARY_CHANGE`;
  - `packages/SystemUI/src/com/android/systemui/theme/ThemeOverlayController.java` (android12-release,
    main): colour events deferred until screen off;
  - `packages/SystemUI/.../statusbar/phone/LockscreenWallpaper.java` (android13-release);
  - `apex/jobscheduler/service/java/com/android/server/usage/AppStandbyController.java` (main):
    `getAppMinBucket`, widgets only.
- The API 37 stubs in `~/Android/Sdk/platforms/android-37.0/android-stubs-src.jar` (the public
  surface).
- [Remove lockscreen lwp flag](https://android.googlesource.com/platform/frameworks/base/+/4de11add42ad%5E%21/)
  (committed 19 Oct 2023).
- XDA, [Android 14's independent lock screen live wallpaper](https://www.xda-developers.com/android-14-independent-lock-screen-live-wallpaper/)
  (off by default in the previews).
- Fairphone forum: [Live-background does not work anymore](https://forum.fairphone.com/t/live-background-does-not-work-anymore/122217)
  and [Since Android 15: wallpaper automatically changes to default image](https://forum.fairphone.com/t/since-android-15-wallpaper-automatically-changes-to-default-image/121697/2) (FP5).
- From the repo: `Frame.kt`, `Background.kt`, `Store.kt`'s set paths and `whole()`,
  `MainActivity.kt`'s crop mode, `CLAUDE.md` (0.6.0: the picture; A2's picks).
