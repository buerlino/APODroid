# Live, the day's pan (0.7.0)

(User, 2026-10-22 evening, 0.7.0's headline; planned 2026-10-28 with the user:
[Decisions](#decisions-user-2026-10-28); reviewed by reading the same evening against `Store.kt`,
`PictureTime.kt` and `Frame.kt`: the engine draws its own copy, lost Live gets Fill at once, Set
waits for the engine after the picker, the shared engine told by the engines' notes, the engine
can't crash, Decision 10. What was settled before is in `CLAUDE.md`, Next
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
  is **one surface**: it can't show home and lock different pictures.
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

**The process:**
- The system binds the current wallpaper with `BIND_AUTO_CREATE | BIND_SHOWING_UI |
  BIND_FOREGROUND_SERVICE_WHILE_AWAKE | BIND_INCLUDE_CAPABILITIES` (AOSP 10 and main). The app's
  process lives as long as it's the wallpaper, at foreground-service level while the screen is on.
- After the process dies the system rebinds it. After a low-memory kill it tries 3 times, 5 s
  apart. Dying twice within 10 s (`MIN_WALLPAPER_CRASH_TIME`) **reverts to the default
  wallpaper**.
- **A force stop reverts to the default wallpaper:** `ACTION_PACKAGE_RESTARTED` →
  `PackageMonitor` marks the package as disappearing (`PACKAGE_TEMPORARY_CHANGE`) →
  `doPackagesChangedLocked` → `clearWallpaperLocked`. That covers the user's App info → Force
  stop, `am force-stop`, and a maker's cleaner that force-stops (ZUI's, `on-time-0.7.0.md`).
- An update keeps it: the package is modified, the service still exists, so it rebinds.
- `AppStandbyController` exempts bound widgets, not wallpapers. The buckets still apply to the
  app's alarms and jobs.
- Fairphone's forum: on the FP5's Android 15 (Fairphone OS) live wallpapers reverted to the
  default within minutes; a fix is listed in FP5.VT2I.C.072.20251122 and the revert was reported
  again in 2026. The FP6 runs LineageOS: L10 looks for it.

**Drawing:**
- `SurfaceHolder.lockCanvas()` (software) or `lockHardwareCanvas()` (API 26). The engine's
  callbacks run on the app's main thread, shared with the page.
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
  checked (`dumpsys wallpaper`).

**Memory**, decoded at the surface's height (ARGB, 4 bytes a pixel):
- FP6, 1116×2484: a 3:2 rendition 3726×2484 is 37 MB. The widest real APOD of 0.6.0's mock-up
  (Fill kept 21% of its width on a 1080×2400 screen, about 2.1:1) is 5316×2484, 53 MB.
- Tablet, 800×1340: a 3:2 picture is 2010×1340, 11 MB.
- Earth's 1440×600 isn't scaled up: 3.5 MB, drawn enlarged. A user's 12000×3000 panorama would be
  99 MB at full height, so it needs a budget (below).
- **Speed:** a 3:2 picture on the FP6 travels 2610 px in 18 h, about 2.4 px a minute; with 6 h
  turns about 7 px a minute. Nothing needs animating.

## How Live fits what the app does (2026-10-28)

What decides what each screen shows stays; Live changes only how a picture reaches a screen.

| 0.6.0 | With Live |
|---|---|
| The rules, the records per screen, turns, the stand-in, Set's forgetting, the alarms, the job | Unchanged: they decide the picture; SIM still guards them |
| A still set per screen (`setStream`/`setBitmap`, home and lock apart, M0); Android keeps its own copy of the picture | On a screen where APODroid's live wallpaper is on: no set. The picture is **copied to the screen's own file** (`files/live-home.jpg`, `live-lock.jpg`, with its whole flag), the engine's copy as Android's is for a still set; the record (noted live) and `wallpaperSetAt` are written as after a set, and the engine redraws (a prefs listener in the same process). Elsewhere the still set as now |
| One wallpaper on both screens when both are due the same picture | One engine on both while both show the same picture; when they're due different pictures the lock screen gets its still set and Android moves the engine to home |
| ⛶'s part (Fill) | Fill only. In Live the clock places the frame (no drag); the stored part stays for Fill |
| The whole picture per post, on blur or black (A3) | The same per post: on a live screen the engine draws it still, with A3's drawing, now shared |
| "My picture", Fill centred | It moves too (decided) |
| Video days ("Skip it": the old picture stays) | The old picture keeps moving (its copy stays; `refresh` deleting `apod.jpg` doesn't touch it) |
| A newer post stored before it's due (Los Angeles: the 21:05 APOD, due at 06:00) | Not shown: the engine draws only its copy, also after a process death, a reboot or a rebind |
| Landscape: Android zooms the portrait frame's middle band (CLAUDE.md, Wallpaper) | The engine draws for its surface: in landscape the picture at the landscape height, at the same position |
| The launcher's scroll offset | Ignored (CLAUDE.md: the clock moves it) |
| The user picks another wallpaper; any wallpaper is replaced at the next change | A screen noted live that isn't live now (another wallpaper, a force stop's default) counts as a forgotten picture, so the next run (a page open too) sets Fill at once, not at the next change (decided: a force stop never cost the picture before); the page says Live isn't on, Set offers the picker |
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
   picker again. Not "leave it alone until Set".
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
   look, Keep running the fix; with Live on and still a miss the box shows the maker's line.
10. **Different pictures on home and lock: Set offers the picker for the lock screen** (user,
    2026-10-28 evening, after a review of this plan found the lock screen would stay Fill for
    good, quietly dropping CLAUDE.md's "the screens can keep their own pictures"). On Android 14+
    (where the build offers a lock-screen live wallpaper; 15+ always), with Size Live, the lock
    screen's column differing from home's and the lock screen not live, Set opens Android's picker
    once more for the user to pick "Lock screen" (a second engine). Backing out leaves the lock
    screen Fill, and Set doesn't ask again until the columns change (`live.lockAsked`, the columns
    it asked for). Not "accept: the lock screen stays Fill".

**Decided in this plan, not asked** (the user can change any; each has its reason):
- **Software canvas** (`lockCanvas`) unless L1 measures a draw over about 30 ms: a frame a minute
  needs no GPU, and a software canvas has no texture size limit for a 5000-px picture.
- **When it draws:** at `onSurfaceRedrawNeeded` and `onSurfaceChanged`, when it becomes visible,
  when its picture changes, and once a minute while visible (only when the frame moved a pixel).
  Never while invisible.
- **The decode:** once per picture, at the screen's portrait height (the longer side, as
  `Store.screen`), never enlarged, within a budget of 64 MB (a larger one decoded smaller and
  drawn enlarged; L1 sets the figure), on a background thread, kept until the picture changes.
  Landscape draws the same decode scaled down: a rotation decodes nothing.
- **The engine can't crash:** decode and draw catch everything (out of memory: decode at half
  the size, then again, else draw black), since two crashes of the one process within 10 s
  revert to the default wallpaper and kill a job run in progress.
- **Colours** from a small copy of the whole picture (not the moving frame, which would recolour
  the theme all day), reported once per picture.
- **Is it on:** each non-preview engine notes the screens its flags name (`live.home`,
  `live.lock`; below 14 home only, with `getWallpaperId(FLAG_SYSTEM)`), and the note is checked
  against the system: on 14+ home's by `getWallpaperInfo(FLAG_SYSTEM)` naming `LiveWallpaper`,
  the lock screen's by `getWallpaperInfo(FLAG_LOCK)` naming it (its own engine) or, while one
  engine is shared, by home's (AOSP: `getWallpaperInfo(FLAG_LOCK)` is null while the lock screen
  shares home's wallpaper: the lock map is empty); below 14 by the id. The lock screen never
  below 14 (Decision 5). Not `QUERY_ALL_PACKAGES` (a permission).
- **Set with Live opens Android's picker when Live is on none of the screens with a column**
  (except the lock screen alone below 14, where Live is Fill), and once per setup for the lock
  screen with a column of its own (Decision 10). A user who chose "Home screen" with equal
  columns isn't asked again at every Set; the page says which screens are live.
- **After the picker, Set waits for the engine:** it notes `getWallpaperId` of both screens
  before opening the picker; when an id changed on the return, it waits up to 3 s for an
  engine's note matching the new id before its update reads `liveOn` (else a still set could
  replace the live wallpaper the user just picked, as the note is written asynchronously; below
  14 nothing else tells). No note by then: the change wasn't APODroid's, Fill.
- **One engine on both screens draws home's picture**; when the lock screen is due another, its
  still set splits them (Decision 4, Android's own rule), and Decision 10 offers the lock
  screen its own engine.
- **A screen noted live that isn't live now** gets its picture forgotten before the rules run
  (as Set forgets it), so it's due again and gets Fill at once.
- **D starts the alarms' run** (job 2, `DailyJob.runNow`), not a second run path in the engine
  (Seams, 1).

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
- **Which screen:** on 14+ from `getWallpaperFlags()`: home when `FLAG_SYSTEM` is in it, else
  lock. Below 14, home. `onWallpaperFlagsChanged` reloads.
- **What it draws: only its screen's copy**, `files/live-home.jpg` or `live-lock.jpg` (one
  engine on both: home's), with the whole flag noted beside it (`live.<screen>.whole`), at the
  turns of that screen's column. Never the source's file: `refresh` replaces `<source>.jpg` with
  every new post, due or not, and deletes `apod.jpg` on a video day, and `Store.set` reads the
  part and the whole flag of the post stored now. Drawing those would show a post the rules
  haven't put on (Los Angeles' 21:05 APOD, due at 06:00) after any process death, reboot or
  rebind, and nothing on a "Skip it" video day.
  - The copy is written by `setDue` when it hands a screen to the engine (L5): the picture's
    file (the source's or `fallback.jpg`) copied via a `.part` and renamed, then the whole flag,
    then the record noted live, under the wallpaper lock.
  - Without a copy (the picker's preview before the first hand-over, a fresh install): the
    newest stored picture of the column's first source, else APOD's, else black. Drawn only
    then.
  - A whole post is drawn whole and still; anything else panned, also "my picture" (Decision 8).
- **Hearing about a new picture:** a prefs listener (`Store.watch`) on the screen's record keys
  while the engine exists, and a re-read whenever it becomes visible. The re-read covers a commit
  from an interrupted thread, which skips the listeners (B16).
- **The decode:** on one background thread; the main thread only draws. Keyed by the copy's
  file and time, at the screen's portrait height, so a rotation decodes nothing and the
  preview doesn't decode the same file twice. Released in `onDestroy`.
- **Nothing throws out of the engine:** decode and draw catch everything; out of memory
  decodes at half the size, then again, else draws black; a broken copy draws black. Logged
  `Live: couldn't …`. Two crashes of the one process within 10 s revert to the default
  wallpaper and kill a job run in progress (AOSP, above).
- **The colours:** computed with the decode (`WallpaperColors.fromBitmap` on a small copy of the
  whole picture), returned by `onComputeColors`, `notifyColorsChanged()` after each new picture.
- **The note:** a non-preview engine writes the screens its flags name (`live.home`,
  `live.lock`, with `getWallpaperId` of each, `live.homeId`, `live.lockId`) when it starts and
  at `onWallpaperFlagsChanged`, and removes its screens in `onDestroy`. A note left by a killed
  process is caught by the check against the system (`Store.liveOn`).
- **Logs** under `APODroid`: `Live: engine <home|lock|both|preview> created / destroyed`,
  `Live: drew <picture> p=<0.00> in <n> ms`, `Live: decoded <w>×<h>`.

### `Store`, Size and Set (L5, L6)

- **`Store.liveOn(screen)`** and **`Store.liveShared`** (one engine on both): the engine's note
  checked against the system, as "Is it on" says. 14+: home when `getWallpaperInfo(FLAG_SYSTEM)`
  names `LiveWallpaper` and the note has home; the lock screen when `getWallpaperInfo(FLAG_LOCK)`
  names it, or when it's null, home is live and home's engine noted both flags (shared). Below
  14: home when `live.homeId` equals `getWallpaperId(FLAG_SYSTEM)`; lock never. L1 confirms
  what `getWallpaperInfo(FLAG_LOCK)` returns while shared.
- **Lost Live** (a review of this plan, 2026-10-28): each screen's record notes when its picture
  went to the engine (`wallpaperLive`, `lock.wallpaperLive`; a still set removes it). Before the
  rules run, a record noted live whose screen isn't live now is forgotten (`Record.forget()`, as
  Set does), so `dueWallpapers` makes it due again and it gets Fill at once, by the job or a page
  open. Else nothing would be due (the record says the picture is on) and a force stop's default
  wallpaper would stay until the next change time.
- **`setDue`'s sets** split per screen by a `:core` rule (L3), from Size, which screens are live,
  whether one engine covers both, and each screen's picture after the plan:
  - Size Fill: every set still, as 0.6.0. A still set replaces APODroid's engine like any
    wallpaper.
  - Size Live, a live screen: the engine. No `WallpaperManager` call; the copy
    (`live-<screen>.jpg` and its whole flag) is written first, then the record (noted live) and
    `wallpaperSetAt` as after a set; `setsSinceSetup` counts it. A failed copy (a full disk)
    counts as a failed set: the record stays, the retry hands it over again.
  - Size Live, a screen with a column that isn't live: the still set (Decision 4).
  - One engine on both screens: it shows home's picture. If the lock screen's picture differs, the
    lock screen gets its still set, which splits them.
  - A screen without a column: left alone, as now (one engine on both may show home's picture
    there).
- **Size** (pref `size`, `FILL` by default, so 0.6.0's installs change nothing): a segmented row
  [Fill | Live] in Setup, part of the draft (the highlight, `Setup`, `saveSetup`, the saved
  state), always shown, as "Where". Placeholders: the heading "Size"; the summary " · Live".
- **Set with Live:**
  1. `saveSetup`.
  2. If Live is on none of the screens with a column (and not the lock screen alone below 14),
     or, on 14+, the lock screen has a column differing from home's, isn't live and wasn't asked
     for these columns (Decision 10, `live.lockAsked`): note `getWallpaperId` of both screens and
     open Android's picker (`ACTION_CHANGE_LIVE_WALLPAPER` for `LiveWallpaper`; without it
     `ACTION_LIVE_WALLPAPER_CHOOSER`; without either a toast, placeholder "This phone can't show
     live wallpapers.", and Live acts as Fill). One picker trip per Set: the home screen's first
     when neither is live, the lock screen's at the next Set.
  3. On the return (the page's `onResume`): when an id changed, wait up to 3 s for an engine's
     note with the new id (this file, "After the picker, Set waits for the engine"). Then Set's
     `update(set = true)`; without a picker at once. Backing out of the picker gives Fill
     (Decision 4).
- **The page:** while Size is Live, the daily switch is on and Live is on none of the screens with
  a column, a status line, placeholder "Live isn't on. Set as wallpaper turns it on." Otherwise
  the status lines as now.
- **SIM:** two invariants: with Size Live, a screen the engine covers never gets a still set,
  but the lock screen of a shared engine when its picture differs; and a screen noted live that
  lost Live is due again in the same run.

### The preview in ⛶ mode (L7)

- With Size Live (the draft's), ⛶'s frame sits at the clock's place for the shown post's screen
  (home if its source is in home's column, else lock, else one source) and doesn't move by drag. A
  minute tick moves it while open.
- With "Whole picture" on (Fill or Live), the picture's place shows the screen-shaped preview: the
  screen's portrait shape, drawn by L4's function, on the blurred or black background.
- ✓, Back, the highlight and the draft as A4. Size Fill: ⛶ as now.

### D, the screen-on check (L8)

(`on-time-0.7.0.md`, D; Seams 1.) A non-preview engine that becomes visible, while the daily
switch is on, when a check is owed (`Store.nextAlarm()` ≤ now: a check time passed since
`lastRunAt`, or a failed run's retry due, B16) and no run started in the last 15 min
(`lastRunAt`): `DailyJob.runNow(context)`, the same call the alarms make.
- It's job 2, so `lastRunAt`, `runGoing`, one run at a time, the failures and the retries are the
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
- **The FP6 (L2, with the user, Android 16, LineageOS):** the picker and its question, the flags,
  the first frame after the screen turns on by eye. Short; the day-long revert check is L10's.
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
- **Drawing while the screen is off** (CLAUDE.md), also to avoid a stale first frame: if L1 or L2
  sees one, the user decides.
- **The launcher's scroll offset** (CLAUDE.md: the clock moves it).
- **A second process** for the engine (track O's reason: the prefs, the locks and `JobRuns` are
  per process).
- **`Store.update` in the engine** for D: a second run path beside the job's bookkeeping, where
  0.6.0's bugs came from. D schedules job 2 instead.
- **Opening the picker at every Set while a screen isn't live:** it would nag the user who chose
  "Home screen", and on 10–13 always. Decision 10 asks for the lock screen once per setup.
- **The lock screen Fill for good with different pictures** (user, 2026-10-28 evening, Decision
  10).
- **The engine drawing the source's file** (`<source>.jpg`): it changes with every stored post
  and goes on a video day; the screen's own copy instead (a review of this plan, 2026-10-28).
- **Android 16's `WallpaperDescription`:** 16 only; per-screen engines do it from 14 on.
- **A hardware canvas by default**, unless L1 measures the software one too slow.
- **Steps set by the job** (CLAUDE.md, 2026-10-22).

## Open

- The first frame after the screen turns on (L1, L2): if it's the old position for a moment, the
  user decides between accepting it and something else (not drawing while off is CLAUDE.md's).
- Whether ZUI's picker and LineageOS's ask home, lock or both (L1, L2). If a picker never asks,
  Live goes where it puts it; Decision 4 covers the rest.
- Android 10–14: untested (E stays the user's call), as for track O.
- Whether a live wallpaper reverts by itself on the FP6, as on the FP5 (L10).
- The texts: placeholders until the user approves them from screenshots (L5, L6, L7).
- The decode budget (64 MB) and the canvas, until L1.
- What `getWallpaperInfo(FLAG_LOCK)` returns while one engine is shared (AOSP: null), and whether
  the engine's flags note is enough to tell shared from home only (L1, step 3).
- A crash of the page or the job twice within 10 s also reverts Live to the default (one
  process); only the engine's side is guarded. Accepted, as a crash loop is a bug anyway.
- The 3 s wait for the engine's note after the picker (below 14 the only guard): untested there
  (E); L1 measures how long the note takes on 15.

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
| L6 | Size [Fill \| Live], Set opens Android's picker, the texts | Opus, high | winston, tablet | L5, L2 |
| L7 | The preview in ⛶ mode | Opus, medium | winston, tablet | L6 |
| L8 | D: the screen-on check starts the alarms' run | Opus, medium | winston, tablet | L5, O4 (and its packages) |
| L9 | Tablet pass of the track, and two real days with Live | Opus, high | rosie, tablet | L6–L8 |
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
  after which Android rebinds it.
- **Set the tablet's wallpaper back** at the end as it was found (noted with `dumpsys wallpaper`
  first): APODroid's still wallpaper by a Set with Size Fill, or the system's by picking it.
- **Applying Live** on the tablet:
  `adb shell am start -a android.service.wallpaper.CHANGE_LIVE_WALLPAPER --ecn android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT io.github.buerlino.apodroid/.LiveWallpaper`,
  then the picker's buttons by `ui.sh` (L1 notes their labels).
- **Reading the state:**
  - `adb shell dumpsys wallpaper` (the components, which screens, the colours);
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
   the first (Decision 10). The time from the picker's "Set" to the engine's `CREATE` (the
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

### [ ] L2. Android check on the FP6: the picker and the first frame (Opus, medium)

(A day session with **the user**, after the tag and L0; the FP6 is the user's phone; about 30 min.)
`probe-live.apk` is a separate package: APODroid's data and setup aren't touched. APODroid's
daily switch stays as the user has it; the probe goes on the home screen for the session only, and
APODroid's next change would replace it anyway.
1. Install the probe, push a picture, `Pick`: screenshots of LineageOS's picker. Does it ask home,
   lock or both? Apply to home only if it asks, else as it does; `dumpsys wallpaper`, `Info`.
2. The first frame: `warp` 10, the screen off 60 s, then the user wakes the phone with the power
   button and says what they see. Also with a `screencap` right after `WAKEUP`, three times.
3. `DRAW` ms for `sw` and `hw` at 2484 high; `dumpsys meminfo`.
4. Back: the user picks their wallpaper again (or APODroid's Set), the probe uninstalled.
- **Done when:** the answers written to `.coord/runs/l2/report.md`; the user has seen the first
  frame.

### [ ] L3. `:core`: the position by the clock, the decode size, still or engine (Opus, medium)

(This file, The position; `Store`, Size and Set. After O1, which moves `PictureTime`'s constants,
and L1, whose step 3 decides how a shared engine is told apart, which `stillOrLive` takes.)
Plain Kotlin, JUnit.
- **`livePosition(now, zone, time: PictureTime, turns: Boolean): Float`:** T =
  `time.last(now, zone, turns)`, span = the shorter of `time.next(T, zone, turns) − T` and 18 h
  (`liveSpan`), p = (now − T) / span clamped to 0..1.
- **`liveDecodeSize(width, height, screenHeight, maxBytes): Pair<Int, Int>`:** the picture's
  shape at the screen's portrait height (landscape draws it scaled down, so a rotation decodes
  nothing), never larger than the picture, smaller while width × height × 4 is over `maxBytes`
  (`LIVE_DECODE_BUDGET`, 64 MB until L1).
- **`stillOrLive`** (the name is winston's), the rule of "`Store`, Size and Set": from Size, which
  screens the engine covers, whether one engine covers both, and each screen's picture after the
  plan, return per set the screens the engine takes and those set still.
- **Tests:**
  - p at 06:00, 12:00, 18:00, 24:00 and 03:00 with one source (0, ⅓, ⅔, 1, 1);
  - every 12, 6 and 3 h (each turn 0 → 1), every 24 h with two sources (18 h, then still);
  - "New picture at" 00:00 and 23:00;
  - the DST nights in Zurich (spring and autumn) with 00:00 and with 3 h turns;
  - a zone change (the change times follow);
  - the decode size for a 3:2 at 2484, the widest real one, Earth's 1440×600 (not enlarged), a
    12000×3000 panorama (under the budget);
  - the rule: Size Fill (all still); Live on home only (lock still); one engine on both, the same
    picture (the engine, no still set); different pictures (lock still); two engines, different
    pictures (both the engine); a screen without a column (nothing); Live on no screen (all
    still);
  - lost Live: a record noted live whose screen isn't live is due again (its picture
    forgotten), with one source and with turns, and keeps its owner and stamp.
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
on `dev` between L5 and L6 the app treats Live as on wherever Android's picker put APODroid's
engine.
- **`LiveWallpaper.kt`**, the manifest entry, `res/xml/live_wallpaper.xml` and
  `res/values/strings.xml` (the description), as This file says. The canvas and the budget as L1
  decided.
- **The engine draws only its screen's copy** (`live-home.jpg`, `live-lock.jpg` and the whole
  flag beside it) and catches everything in decode and draw, as This file says.
- **`Store.liveOn(screen)`** and `liveShared`, from the engines' notes checked against the
  system (L1's step 3 decides the shared case).
- **`setDue`** with L3's rule as for Size Live: a live screen gets its copy, then its record
  (noted `wallpaperLive`) and `wallpaperSetAt`, no `WallpaperManager` call, `setsSinceSetup`
  true, logged `wallpaper shown live: <picture> on <screens>`; one engine on both with different
  pictures: the lock screen's still set. **Lost Live:** a record noted live on a screen not live
  now is forgotten before the rules run, so the same run sets Fill.
- **Files:** `LiveWallpaper.kt` (new), `Store.kt`, `AndroidManifest.xml`, the two resources,
  `CLAUDE.md` (The app, Wallpaper: Live; Next features: the decisions of this file), the skill's
  "Where things are" and its notes on `am force-stop` with Live.
- **Tablet** (this file's notes for rosie):
  - Apply Live on both through the picker (APODroid's daily switch on, the posts faked a day
    back). Expected: `Live: engine both created`, a frame drawn.
  - `run -f 1`: the new post goes on as `shown live` (no `wallpaper set:` line), the engine
    redraws (`Live: drew`), `dumpsys wallpaper` still APODroid's live one.
  - The pan: `cmd alarm set-time` forward (auto_time 0, forward only, the skill's how) to 12:00
    and 23:30: screenshots (the frame a third and nearly all the way across).
  - A whole post (⛶'s toggle and Set): drawn whole and still on blur.
  - "My picture" on a faked video day: panned.
  - With "More NASA pictures" on and different columns: the lock screen gets its still set
    (`wallpaper set: … on LOCK`), the log's flags change to home.
  - `run-as … kill -9`: rebound, the same frame.
  - **The copy, not the source's file** (each after `kill -9`, so nothing is left in memory):
    - a newer post stored but not due: one source, the clock after the window, a post faked
      published after it (the skill's prefs edits; `<source>.published` later than the change
      time + 3 h), a pull with the daily switch off to store it, then `kill -9`: the engine draws
      the record's picture (the log names it), not the new one;
    - a "Skip it" video day (APOD faked a video, `apod.jpg` gone after the refresh), `kill -9`:
      the old picture still drawn;
    - a reboot: the same.
  - **Lost Live:** another wallpaper picked in the system (or `am force-stop`, then the page
    opened): the next run (`run -f 1`, or the page open) sets Fill on that screen at once
    (`wallpaper set:`), the record no longer noted live.
  - **No crash:** `live-home.jpg` truncated with `run-as` and the screen off and on: black,
    `Live: couldn't …` logged, no `CRASH` in `exit-info`, the wallpaper still APODroid's.
  - The process's oom adj with the screen off.
  - Then the smoke list, with Live off at its start (the system's wallpaper picked) and set back
    at the end.
- **Done when:** the tablet passes; the texts listed (the description).

### [ ] L6. Size [Fill | Live], Set opens Android's picker, the texts (Opus, high)

(This file, `Store`, Size and Set, its second half. After L5 and L2.)
- **The pref `size`** (`FILL`, `LIVE`): in `Setup`, `saveSetup`, the draft and its saved state,
  the highlight; Size Fill gives all still sets (L3's rule), a still set replacing APODroid's
  engine.
- **The row [Fill | Live]** in Setup after "Where" or the list (`Segments`), always shown.
- **Setup's summary** " · Live".
- **Set's flow** as This file says: the picker when Live is on none of the screens with a column
  (not the lock screen alone below 14), and once per setup for the lock screen with its own
  column (Decision 10, `live.lockAsked`); the chooser or the toast without it; the ids noted
  before, the wait for the engine's note after; the update after the return, Fill after backing
  out.
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
  - Live with "Where" Home: the picker; the lock screen left alone.
  - **Decision 10:** "More NASA pictures" on, home APOD, lock Earth, Live on home: Set opens the
    picker for the lock screen; "Lock screen" there: two engines (`Live: engine lock created`),
    each screen its own picture moving. Again with a back-out: the lock screen Fill, and a
    second Set doesn't open the picker; a changed column does.
  - After confirming the picker, Set's update hands the screens to the engine (`shown live`),
    no still set over the live wallpaper just picked (`dumpsys wallpaper`).
  - Another wallpaper picked in the system: the next `run -f 1` sets Fill there, the status line
    says Live isn't on.
  - The highlight and TalkBack's "not set yet" on the row.
  - Screenshots of the row, the summary, the status line and the picker's first screen for the
    user. Then the smoke list.
- **Done when:** the tablet passes; SIM's invariants hold over its seeds; the texts listed.

### [ ] L7. The preview in ⛶ mode (Opus, medium)

(This file, The preview; Decision 7. After L6.)
- **⛶ with the draft's Size Live:** the frame at `livePosition` for the shown post's screen (this
  file's rule), no drag (the semantics without a drag action), moved by a minute tick while open.
- **"Whole picture" on** (Fill or Live): the screen-shaped preview drawn by L4's function in the
  picture's place, with its background.
- **✓, Back, the highlight, the draft** as A4.
- **Files:** `MainActivity.kt`; `CLAUDE.md` (Choosing the part: the preview; Live).
- **Tablet:** Size Live, ⛶ at a faked 12:00 (the frame a third across, a drag doesn't move it).
  Whole picture on a planet on black and a nebula on blur: the previews beside screenshots of the
  wallpaper after Set (the same look). Size Fill: ⛶ as before. Rotation closes it as now.
  Screenshots for the user; the smoke list.
- **Done when:** the tablet passes; the texts listed (none expected beyond the semantics).

### [ ] L8. D: the screen-on check starts the alarms' run (Opus, medium)

(This file, D; `on-time-0.7.0.md`, D and its Seams 1. After L5 and O4 with its packages, which may
change the run's bookkeeping; with O10 the run is expedited.)
- **In the engine's `onVisibilityChanged(true)`** (not the preview), while `daily` is on: when
  `Store.nextAlarm()` ≤ now and `lastRunAt` is older than 15 min, `DailyJob.runNow(context)`.
  Logged `Live: screen on, run owed`. Nothing else: the run is job 2's.
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
  - Decision 10's two engines with different pictures;
  - landscape (the picture at the landscape height).
- `logcat` and `exit-info` without a crash; `dumpsys meminfo` with the page open and closed.
- **Then two real days** (W's way, by hand, morning reads; the tablet on battery at night as W did;
  not during O16's week): the change at 06:00 on the live screens, the frame's place at each read,
  the engine alive, `ZuiMemoryCleaner` lines, the wallpaper still APODroid's.
- The texts' screenshots in one folder for the user.
- **Done when:** every check ran on R8 too, the two days read; no `BUG:` line, or each drafted as
  a package.

### [ ] L10. Phone pass: Live on the FP6 for a few days (Opus, high)

(**The user**: the FP6 is the user's phone. After L9; with O17 if both are ready.)
- The release-signed build over the user's install (the skill: testing an update on the user's
  phone): Size Fill as before.
- The user turns Live on with their setup (APOD + NASA, 12 h) through Android's picker. Over a few
  days:
  - the frame's place at a few times of day;
  - the change at 06:00;
  - whether Live stays on (the FP5's revert: `dumpsys wallpaper` each day);
  - the first frame when the phone wakes (the user's eye);
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
   L1 reads the adj with the screen off and runs ZUI's cleaner against a live probe; O8 can
   include the probe in its sweep if L0 is built by then. With Live as the wallpaper, Keep running
   still matters while the screen is off, unless L1 shows the adj stays low.
3. **The detector doesn't suggest Live** (user, 2026-10-28, Decision 9). For "APODroid is the
   current live wallpaper", **use `Store.liveOn` (L5), not `getWallpaperInfo()`**: below 14 that
   returns null without `QUERY_ALL_PACKAGES`. Keep running stays a plain switch, not turned off by
   Live.
4. **No shared service, one process:** agreed. The engine is a `WallpaperService` in the app's
   process, no `android:process`.
5. **The set paths:** agreed. `Store.update` stays the one entry point for a run; L5 changes only
   `setDue`'s last step (a live screen gets its record, not a still set), and the engine hears it
   through `Store.watch`. O's packages that touch `setDue` or `Store.set` come before L4 and L5 or
   rebase on them.
6. **Memory:** the engine holds one decoded picture per engine (L3's budget) for as long as Live
   is on. O12 (the page's pictures let go while stopped) helps both; L1 and L9 measure with the page
   open and closed.
7. **Order and the pipeline:** O1–O4 first (L3 after O1, L4 after O2; L6's row is a segmented row,
   so O3's dropdown doesn't block it). One tablet: L1 and O6–O8 one at a time, L1 beside O8 (its
   cleaner step), L9's two real days not during O16's week. One `plan-0.7.0.md` (or
   `pipeline-lib.sh`'s `live <key>`) and the L rows in `night.md`, as O's 7 says.
8. **Permissions:** Live adds none (`BIND_WALLPAPER` is the service's guard, not a permission the
   app holds); track O adds `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SPECIAL_USE`.
9. **rosie's habits** (new): with Live on, `am force-stop` drops the wallpaper to the default and
   `am kill` doesn't kill the process. O's checks that force-stop the app (O14's "`am force-stop`
   after a run") must run with Size Fill, or expect the live wallpaper gone.

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
