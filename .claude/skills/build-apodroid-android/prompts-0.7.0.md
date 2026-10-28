# Prompts: planning 0.7.0 (two tracks)

(User, 2026-10-28.) Two planning sessions, one per track, run while 0.6.0 finishes W, R and T.
They plan only: no code, no device. Building starts after 0.6.0 is tagged. Start each in its
own terminal from the repo root (they write different files, so they can run at the same time):

```
claude --model opus --effort high "$(sed -n '/^## Track O/,/^## Track L/p' .claude/skills/build-apodroid-android/prompts-0.7.0.md | sed '$d')"
claude --model opus --effort high "$(sed -n '/^## Track L/,$p' .claude/skills/build-apodroid-android/prompts-0.7.0.md)"
```

Delete this file once both plans are written.

## Track O: on time on any phone

You are planning 0.7.0's first track for APODroid: the wallpaper changes on time on any phone,
also when the phone's maker stops apps. Investigate, decide with the user, and write the work
packages. Plan only.

**Read first:** `CLAUDE.md` (all of it; "Next features", 0.7.0: on time on any phone, and
"Background update" matter most), `.claude/skills/build-apodroid-android/SKILL.md`,
`on-time-0.7.0.md` (the findings, plan B, the detector, the pause hint per maker, D, the checks,
Rejected), `night.md` (how a package is built, reviewed and checked: your packages must fit it),
`declutter.md`'s pass of 2026-10-25 (C1–C12), and the code of the job: `app/.../DailyJob.kt`,
`AlarmReceiver.kt`, `Store.kt`, `:core`'s `JobRuns`, `PictureTime.kt`, `Screens.kt`.
`plan-0.6.0.md` shows the package format (`### [ ] ID. Title (Model, effort)`, what it builds,
"Done when", the tablet checks) and is long: read its "Packages" table and two or three
packages, not all of it.

**What's new since on-time-0.7.0.md was written** (2026-10-28, W on the tablet): Lenovo's
ZuiMemoryCleaner force-stopped the app on an ordinary afternoon, with the app Unrestricted and
in the EXEMPTED bucket; a force stop deletes the alarms and job 1, so nothing ran for 6 hours.
It killed only cached processes (adj 915 to 999), its strategy names `minAdj: 701`, and it keeps
a whitelist. Evidence: `.coord/runs/w/report.md` (the tablet's Wednesday) and
`.coord/runs/w/tab-wed-all.txt`. The user, after seeing it: "we have to find a solution, sadly it
will lead to a persistent notification". That reopens the rejected option C (a permanent
foreground service) for the user to decide; plan B's exact alarm can't survive a force stop.

**Find out and write down, with sources:**
1. The options against each layer (Android's Doze and buckets; the makers' killers and
   Autostart): B (exact alarm + a `dataSync` service per run), C (a permanent foreground
   service: which type on Android 14+, `specialUse`'s rules outside Google Play, the 6 h limit
   on `dataSync` from Android 15, memory, what the user sees on Android 10–12 and on 13+
   without the notification permission, whether it survives a Recents swipe), the makers'
   whitelists (what puts an app on ZUI's; Xiaomi's Autostart and the like), the detector, and
   Live's screen-on check (D, the other track's). Which combination covers which layer, and
   what each costs the user (a notification, a permission, a setting).
2. What is still unknown and only a device can tell: turn each into a check package (J0's way,
   `on-time-0.7.0.md`, Checks before building), with the commands and what decides.
3. The order: the declutter (C1–C12) and the design review of the run's bookkeeping come
   first (`on-time-0.7.0.md`, First); then the checks; then the building. A check whose answer
   changes the design comes before the package it decides.

**Ask the user** (`AskUserQuestion`, a recommendation first), one decision at a time, before
writing a package that depends on it: at least C or not, and if C, whether it's a setting
("Exact timing" becomes "Keep running"?) or always on, and its notification text. Don't ask
about what `CLAUDE.md` or `on-time-0.7.0.md` already settles. The user's rule: few settings,
each explained in one sentence; no extra screens.

**Write:** `on-time-0.7.0.md` updated in place (the findings, the decisions with the date and
the user's words, Rejected updated), and its packages at its end: a table (`O1`, `O2`, …;
"What", "Model, effort") and one section per package in `plan-0.6.0.md`'s form, each small
enough for one night-shift run, with "Done when" lines a reviewer can tick and the tablet
checks rosie runs. Mark which packages need the FP6 or the user (rosie uses only the tablet).
End with **"Seams with track L"**: what Live's plan must take from this track or give to it
(the screen-on check D, the detector recommending Live, a shared service). If
`live-0.7.0.md` exists by then, read it and align.

**Rules:** 0.6.0 is in its code freeze and its week-long test W runs to Mon 2 Nov: **no code
changes, no builds, no adb on either device** (the FP6 is the user's phone; the tablet runs W).
Write only `on-time-0.7.0.md`; list changes you'd make to `CLAUDE.md` or the skill at the end of
your reply instead of making them. Don't commit. When you're done, tell the user what you
decided with them, what you found that changes the plan, the package order, and what stays open.

## Track L: Live, the day's pan

You are planning 0.7.0's headline for APODroid: Live, a live wallpaper that pans a wide picture
across the screen over the day. Investigate, decide with the user, and write the work
packages. Plan only.

**Read first:** `CLAUDE.md` (all of it; "Next features", 0.7.0: Live, the day's pan, and 0.6.0:
the picture, and "The app", Wallpaper, matter most), `.claude/skills/build-apodroid-android/SKILL.md`,
`on-time-0.7.0.md` (D: Live checks when the screen turns on; it's the other track's plan),
`night.md` (how a package is built, reviewed and checked: your packages must fit it), and the
code that draws and sets: `Store.kt`'s set paths, the whole picture and the background
(`:core`'s `Background.kt`, `wallpaperFrame`, `fitFrame`), the ⛶ crop mode in the page.
`plan-0.6.0.md` shows the package format (`### [ ] ID. Title (Model, effort)`, what it builds,
"Done when", the tablet checks) and is long: read its "Packages" table, A3's package (the
drawing) and two or three others, not all of it.

**What's decided** (`CLAUDE.md`): Size [Fill | Live] with the preview on the page (one drawing
code for the wallpaper and the preview); a `WallpaperService`, no library, no permission; the
position by the time of day from the picture time (left edge at 06:00, right edge at 24:00);
drawn when the screen turns on, nothing while it's off; the clock moves it, not the launcher;
chosen once in Android's live wallpaper picker; per screen on Android 14+ (`getWallpaperFlags`).
Rejected: steps set by the job.

**Find out and write down, with sources:**
1. How a live wallpaper fits what the app does now: a static set per screen today (home and
   lock apart, M0), the records per screen, turns and the stand-in, "my picture", the whole
   picture on blur or black, ⛶'s part. What Live replaces, what stays, and what happens when the
   user picks another wallpaper in the system (the app is no longer the live one).
2. The open questions `CLAUDE.md` lists: the route (straight, or slowing over what matters), the
   night (still at the end, or back to the start), Android 10–13's lock screen (no per-screen
   live wallpaper there), Auto and Fill as live too. For each: what Android allows, with
   sources, and a recommendation.
3. The engine: drawing (Canvas vs. a `SurfaceHolder` with hardware acceleration), memory (one
   decoded picture at screen height; a 5815 px wide one), when it redraws (screen on, the
   picture changed, the minute), battery, how the job tells the engine about a new picture,
   wallpaper colours (`onComputeColors`) and the system theme's dynamic colour.
4. What is still unknown and only a device can tell: turn each into a check package (on the
   tablet, Android 15; the FP6, 16, with the user), with the commands and what decides.
5. The order: a check whose answer changes the design comes before the package it decides;
   `:core` first (the position by the time, tested), then the engine, the picker and the
   setting, the preview, the per-screen part.

**Ask the user** (`AskUserQuestion`, a recommendation first), one decision at a time, before
writing a package that depends on it: the route, the night, the lock screen on 10–13, Auto and
Fill as live, and anything the investigation turns up. Don't ask about what `CLAUDE.md` already
settles. The user's rule: few settings, each explained in one sentence; no extra screens; texts
are the user's to approve (from screenshots, in a package's checks).

**Write:** a new `.claude/skills/build-apodroid-android/live-0.7.0.md`, shaped like
`on-time-0.7.0.md` (the findings, the decisions with the date and the user's words, the checks,
Rejected), and its packages at its end: a table (`L1`, `L2`, …; "What", "Model, effort") and
one section per package in `plan-0.6.0.md`'s form, each small enough for one night-shift run,
with "Done when" lines a reviewer can tick and the tablet checks rosie runs. Mark which packages
need the FP6 or the user (rosie uses only the tablet). End with **"Seams with track O"**: what
the on-time plan must take from this track or give to it (D's screen-on check, the detector
recommending Live, a shared service, the job's set paths both change). If `on-time-0.7.0.md`
has its packages by then, read them and align.

**Rules:** 0.6.0 is in its code freeze and its week-long test W runs to Mon 2 Nov: **no code
changes, no builds, no adb on either device** (the FP6 is the user's phone; the tablet runs W).
A throwaway prototype is not allowed either. Write only `live-0.7.0.md`; list changes you'd make
to `CLAUDE.md` or the skill at the end of your reply instead of making them. Don't commit. When
you're done, tell the user what you decided with them, what you found that changes the plan,
the package order, and what stays open.
