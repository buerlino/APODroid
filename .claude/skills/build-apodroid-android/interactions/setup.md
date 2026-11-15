# APODroid: Setup and "Set as wallpaper", decision tree for a phone test

> A snapshot read at 7299474 (15 Nov 2026); line numbers are of that commit (`git show 7299474:<path>`).
> The SUSPECTs below were fixed or accepted since: see [README.md](README.md). Newer changes: README, Keeping it current.

Build: release 7299474 on the FP6 (Android 16, LineageOS). Read from the code at 7299474 (the
working tree differs only in the picker dialog's text) and CLAUDE.md. Out of scope: Size **Live**
and Android's live wallpaper picker (another agent), except where Live is in the way (step P1
leaves it, P30 restores it). ⛶ (the part) only where Set touches it.

## 0. Conventions

**Abbreviations**
- `Wset` toast "Wallpaper set"; `NTS` toast "Setup saved. Nothing to set yet."; `CantSet` "Couldn't set the wallpaper"; `CantSave` "Couldn't save the setup. Check the free space.".
- Pictures as the log prints them (`Picture.Of`'s data-class toString):
  `A15` = `Of(source=APOD, date=2026-11-15, url=https://…)`, `N13` = `Of(source=NASA, date=2026-11-13, url=https://…)`,
  `E13` = `Of(source=EARTH, date=<Earth's stored date, likely 2026-11-13>, url=https://…)`.
- Each set entry may end in `, darker ×0.xx (grey nnn)` when Darken is on and the picture is bright (Store.kt:1579-1580). Treat it as optional unless the leaf is about Darken.
- Time zone assumed Europe/Zurich (CET = UTC+1 today), 24 h clock (with 12 h the page shows "6:00 AM"). `<off>` = the phone's fixed offset, 0–10 min. The alarm log prints UTC: "New picture at 06:00" → `Daily job: alarms for 2026-11-16T05:0m:ssZ`.
- `HH:mm` in status lines = the time of the set.

**Log lines (tag APODroid)**
- Set: `Daily job: alarms for <instant>` (or nothing with the daily switch off), then `Set: wallpaper set: <pic> on <HOME|LOCK|HOME+LOCK>[, <pic> on …]` (MainActivity.kt:1082-1089; the alarm line comes first). An order-alone Set that sets nothing logs no `Set:` line.
- Daily switch on: `Switch on: wallpaper set: …` (MainActivity.kt:958); off logs nothing.
- Every page resume logs `Daily job: alarms for …` (onResume → DailyJob.ensure, MainActivity.kt:560). Clear the log (`adb logcat -c`) right before the tap you measure.
- A page load that sets: `Page: wallpaper set: …`.

**adb checks** (read-only; release build, so no `run-as`, prefs not readable)
- `adb logcat -c` … `adb logcat -d -s APODroid`
- `adb shell dumpsys wallpaper` → compare "System wallpaper state" / "Lock wallpaper state": the component (ImageWallpaper vs `io.github.buerlino.apodroid/.LiveWallpaper`) and the wallpaper ids. A set changes the screen's id; "nothing set" = ids unchanged. One set with both flags = one shared wallpaper (the lock section empty / no own lock wallpaper, CLAUDE.md M0).
- `adb shell dumpsys alarm | grep -A8 io.github.buerlino.apodroid` → the pair's time (none after the daily switch off).
- `adb shell dumpsys jobscheduler | grep -A4 io.github.buerlino.apodroid` → job 1 (periodic) present while the switch is on.
- `adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml` → texts, `checked`, `enabled`, content-desc ("NASA on: Home screen", "If APOD is a video: Skip it", "New picture at: 06:00", "Picture from: …"). The Setup row is a merged node: grep its summary text. The drag handle "≡" is hidden from accessibility (MainActivity.kt:2371): locate it ~24 dp left of the "NASA"/"APOD"/"Earth" checkbox node.
- Highlight (`tertiary` colour) is not in the dump: `adb exec-out screencap -p > shot.png` and compare with the baseline shot.
- Process: `adb shell pidof io.github.buerlino.apodroid`. Pages: `adb shell dumpsys activity activities | grep -c "io.github.buerlino.apodroid/.MainActivity"` (count Hist lines).
- Drag by adb: `adb shell input swipe <x> <y> <x> <y±rowHeight> 800` on the handle (a vertical drag; the row swaps once past half its neighbour's height, MainActivity.kt:2356-2368).

**The anchor rule decides most Set outcomes (read this first).** A Set with the list shown and an order other than the stored one (or no stored anchor) writes `orderFrom` = the date of the next "New picture at" (Store.kt:372-394, PictureTime.kt:128). Until that moment A (16 Nov 06:00 for any Set today after 06:00) every Set and the daily switch on run `keepUntilAnchor` (Screens.kt:422, 446-470):
- daily on, **order alone** → each record kept, nothing set → `NTS`;
- daily on, anything else (incl. a Set that changed nothing) → each screen keeps its **owner** if it's still in its column and has a picture, and that picture is set again → `Wset`;
- the owner not in the new column, a column that was empty before, or **daily off** → the column's **top in the list's order** goes on at once.
The phone very likely already has an anchor of 16 Nov (the Set that made it Live today wrote one), and P3 below writes one for sure. **All expected outcomes assume the test runs before 16 Nov 06:00.** After A, a Set takes the clock's turn instead (RO1): for one-source columns there's no difference.
A screen whose column is empty is left alone (record, picture and status line kept).

**Baseline B0 (as given):** daily on; More NASA pictures on; list home [NASA], lock [APOD]; Earth unticked; order APOD, NASA, Earth; Size Live (home live, lock still); New picture at 06:00; As soon as it's out off; Change every hidden (24 h); If APOD is a video "Skip it"; Darken on. Summary "Home: NASA · Lock: APOD · 06:00 · Live". Today Sun 15 Nov: APOD image 15 Nov, NASA 13 Nov, Earth likely 13 Nov. No new posts until APOD 16 Nov at 06:05 CET.
Unknown, record in P0: stored "Where" (W0), Earth's greyed dropdown (E0, its lastScreens), whether "my picture" exists, whether "Don't show again" hid the weekday dialog, Keep running.

---

## 1. Decision tree

Leaf format: **id**: pre → steps → screen / log / adb. "Set" = tap "Set as wallpaper".

### A. The Setup row and its summary
- **A1** closed at a fresh start (Recents-swipe, open): row "Setup", summary under it = stored setup, "▾". B0: "Home: NASA · Lock: APOD · 06:00 · Live". Tap → opens ("▴", summary hidden). Tap again → closes.
- **A2** auto-open: any draft change that makes the draft differ opens it (LaunchedEffect, MainActivity.kt:1911); closing it by hand is allowed while highlighted; a rotation reopens it (the effect runs again on recreation).
- **A3** highlight: title "Setup" in tertiary while anything shown in Setup differs (MainActivity.kt:2122-2126): mode switch, shown mode's Where (APOD mode) or list rows / order (list mode), Size, New picture at, As soon as it's out, Change every, If APOD is a video, Darken, Wide pictures sideways (SW3). Not the daily switch, not Advanced. Changing a value and back = no highlight.
- **A4** after a Set that saved and didn't fail: Setup closes (also after `NTS`) (MainActivity.kt:1095); summary shows the new stored setup.
- **A5** summary forms (MainActivity.kt:2153-2172): "X · both screens" when both columns have the same sources; "X · home screen"/"X · lock screen" when one is empty; else "Home: X · Lock: Y"; names in the stored order; then " · every 24 h from 06:00" when a column has ≥2 (daily on), " · 06:00" otherwise, nothing with daily off; " · Live" with Size Live. "As soon as it's out", Darken and Wide pictures sideways never appear.
- **A6** summary is the stored setup: while the draft differs and Setup is closed, the summary still shows the stored one.

### B. "More NASA pictures" (mode switch, draft)
- **B1** on → off (stored on): Setup opens; the list is replaced by "Where" showing stored W0 (not highlighted); switch label + switch in tertiary; Setup highlighted. Pager offers APOD only: title "APOD" with no "▾", no swipe, no chip, caption = title then "15 November 2026" (no source line). Status lines and summary unchanged (stored). "Change every" hidden (APOD mode has no turns) and the draft's interval reverts to the stored one.
- **B2** off → on again in the same page: the list comes back exactly as the draft left it (ticks, order), the chosen pager page comes back (chosenSource kept); no highlight if nothing else differs.
- **B3** off + Set: see C1–C3. Stored order and anchor untouched (Store.kt:370, 393). **SUSPECT S2**: the draft list (even edits made before turning the switch off) IS saved (Store.kt:387) — see D20.
- **B4** on (stored off, APOD mode) → off→on: the list shows the stored list (hasList true on this phone); first time ever (no `home` pref) it would start from "Where" (MainActivity.kt:1207) — not reproducible here.
- **B5** on + Set from APOD mode: list columns go on per the anchor rule (owner not in the new column → top). E.g. stored APOD/Where=Home, draft list home [NASA], lock [APOD] → `Set: wallpaper set: N13 on HOME, A15 on LOCK`, `Wset` (lock's column was empty → top APOD, set even though APOD is already there).
- **B6** the chip, status lines and opening page follow the **stored** mode until Set (MainActivity.kt:661, 810, 833).

### C. Where (APOD mode only)
- **C1** Where "Both" + Set (from list home NASA / lock APOD): equal columns → one set `Set: wallpaper set: A15 on HOME+LOCK`, `Wset`; dumpsys: one shared wallpaper. Summary "APOD · both screens · 06:00". Status: one line "Changed 15 Nov, HH:mm" (no " · APOD": APOD alone). No chip (one source offered).
- **C2** Where "Home screen" + Set (from Both): `…A15 on HOME`, lock left alone (keeps APOD; with FLAG_SYSTEM alone Android moves the shared wallpaper to the lock screen, M0, so dumpsys now shows a lock wallpaper of its own). Summary "APOD · home screen · 06:00". Status line from home's record. Expected later: lock never changes again.
- **C3** Where "Lock screen" + Set: lock column was empty → top APOD set on LOCK; home left alone (whatever it shows). Summary "APOD · lock screen · 06:00"; status from the lock record (pageScreen LOCK, Store.kt:810).
- **C4** Where changed while hidden (change it in APOD mode draft, then mode back on): Setup NOT highlighted for it (MainActivity.kt:2124 ignores `where` in list mode) but Set saves it (Store.kt:385). **SUSPECT S1**, see D17.
- **C5** segments one height; highlight on the chosen segment while ≠ stored.

### D. The list ("Pictures from")
Ticks
- **D1** tick Earth (unticked; first source not ticked anywhere) → weekday dialog "NASA Image of the Day and Earth Observatory post on weekdays only." with "Dismiss"/"Don't show again" (unless hidden before) (MainActivity.kt:1196, 2506-2516). Earth goes on its greyed dropdown's screens E0 (Both if never set). Earth row (tick, name, dropdown) tertiary; Setup highlighted. If a column now has ≥2 → "Change every" appears with "24 h" (stored).
- **D2** "Dismiss" → dialog closes, shows again at the next such tick. "Don't show again" → never again (persisted; avoid on the user's phone).
- **D3** untick APOD (lock) with NASA on home → allowed (2 ticked); NASA's tick box becomes greyed (last ticked, MainActivity.kt:2387); lock column empty; "If APOD is a video" greyed (MainActivity.kt:1952); APOD's dropdown greyed showing "Lock screen". Tapping NASA's tick → nothing.
- **D4** re-tick APOD → back on "Lock screen"; nothing highlighted (equal to stored).
- **D5** untick NASA (home) + Set → home column empty: home left alone. `Set: wallpaper set: A15 on LOCK` only; home id unchanged; `Wset`. Summary "APOD · lock screen · 06:00". Status: "Home: <old time> · NASA" stays (record kept) and "Lock: 15 Nov, HH:mm" (no " · APOD": APOD now ticked alone). NASA page chip still "On home screen". By design (CLAUDE.md "leaves a screen … alone, with its record, so the chip still knows it"); a user may wonder why an unticked source still shows "On home screen".
- **D6** re-tick NASA (lastScreens Home) + Set → home column was empty → top NASA, new record → `…N13 on HOME, A15 on LOCK`, `Wset`.
- **D7** last ticked can't be unticked even via its row; its dropdown still moves it.
Dropdowns
- **D8** ticked NASA "Home screen ▾" → "Both": lock [APOD, NASA]; "Change every" appears; NASA row tertiary. Set → before A: home NASA kept, lock owner APOD kept → `…N13 on HOME, A15 on LOCK`, `Wset`. Summary "Home: NASA · Lock: APOD, NASA · every 24 h from 06:00". Expected later (16 Nov 06:00+off): lock top of [APOD, NASA] = APOD (16 Nov's), 17 Nov NASA.
- **D9** a choice equal to the current one → no change (place() returns, MainActivity.kt:1192).
- **D10** unticked row's dropdown is disabled (greyed) and shows lastScreens.
- **D11** moving APOD to "Home screen" while NASA is home → home [APOD, NASA], lock [] → lock left alone after Set; summary "APOD, NASA · home screen · every 24 h from 06:00"; before A home owner NASA kept.
Order (≡ drag, "Move up"/"Move down")
- **D12** drag NASA's ≡ above APOD → order NASA, APOD, Earth; both moved rows' ≡ tertiary, Setup highlighted; the pane doesn't scroll while dragging. Pager/title menu keep APOD, NASA, Earth.
- **D13** order alone + Set (daily on) → `NTS`, no `Set:` line, ids unchanged, Setup closes; anchor → 16 Nov. Summary order follows stored order (single sources: unchanged text). Expected later (turns only): the top goes on at 16 Nov 06:00.
- **D14** drag back to the stored order → no highlight.
- **D15** order + any other change + Set (daily on) → `Wset` (owners kept, set again).
- **D16** order changed, then mode off, Set (APOD mode) → order NOT saved, anchor not moved (Store.kt:370, 393); turning mode on again shows the draft order still highlighted.
- **D17 SUSPECT S1** order back + a hidden "Where" change: stored list mode; mode off → Where: pick ≠ W0 → mode on (Setup highlight off: only Where differs) → drag to change the order → Set. Doc ("a Set of the order alone … sets nothing", CLAUDE.md Turns, the anchor) expects `NTS`; code: `orderOnly` compares the whole Setup incl. `where` (Store.kt:374) → not order-alone → `Wset` (both screens set again), and the hidden Where is saved (Store.kt:385). Check afterwards: mode off (draft) → Where shows the new value un-highlighted. Contradicts CLAUDE.md:376 "Set saves, and 'Setup' highlights, only what the page shows".
- **D18** order change + Set with daily OFF → top of each column at once (Screens.kt:464-465), alarms cancelled. With home [NASA, Earth], lock [APOD, Earth], order Earth first → `Set: wallpaper set: E13 on HOME+LOCK` (one set: both due the same picture, Screens.kt:174), `Wset`, no `Daily job: alarms` line.
- **D19** TalkBack only: tick box actions "Move up" (not first row) / "Move down" (not last); on the greyed last ticked row too? (user's FP6 check; not via adb).
- **D20 SUSPECT S2** hidden list saved in APOD mode: stored APOD mode; mode on (draft) → NASA dropdown "Both" → mode off → Setup NOT highlighted (MainActivity.kt:2124 ignores list in APOD mode) → Set (`Wset`, APOD set again) → mode on (draft): NASA row shows "Both ▾" NOT highlighted = saved (Store.kt:387 `if (setup.allSources || hasList) putColumns(...)`), while an order edit is not saved (Store.kt:393). Contradicts CLAUDE.md:376.

### E. Change every (list mode, a column with ≥2, daily on)
- **E1** appears below "As soon as it's out" when a draft column gets 2 sources; label "Change every" + ⓘ; segments 24 h | 12 h | 6 h | 3 h, the stored one selected.
- **E2** ⓘ → "Every 24 h from 06:00, each screen switches to its next source in the list; a new order starts with the top one at 06:00." (draft values, MainActivity.kt:2109-2110); OK closes; survives rotation.
- **E3** pick 6 h → tertiary; untick so no column has 2 → hidden AND reverted to stored (MainActivity.kt:1905); re-tick → shows "24 h" again (choice lost; doc: intended).
- **E4** 6 h + Set → summary "… · every 6 h from 06:00"; alarms for the next 6-h change (12:00/18:00/00:00 + off) or a check in its window; before A owners kept. Expected later: turns at each 6 h change by the clock phased to A.
- **E5** daily off → hidden and reverted.

### F. New picture at (daily on)
- **F1** button "06:00 ▾", menu 00:00…23:00 with ✓; content-desc "New picture at: 06:00". Pick 07:00 → tertiary.
- **F2** 07:00 + Set → `Wset` (pictures set again), summary "… · 07:00". Alarms: `Daily job: alarms for 2026-11-16T06:0m:ssZ`, OR `alarms for <now>` followed by `Daily job: alarm, run scheduled` / `Daily job: job 2 starts expedited` when a 07:00-window check (09:15+off, 10:00+off) lies after the last job run's start (B16; CLAUDE.md "a Set that changes the picture time … costs one spare check", accepted). That run sets nothing.
- **F3** daily off → hidden and reverted to stored (MainActivity.kt:1907).
- **F4** picking an hour later than now (e.g. 23:00) → current change time = 14 Nov 23:00; tonight 23:00 is a change before A = 16 Nov 23:00 (expected later, turns only).

### G. As soon as it's out (daily on)
- **G1** switch + ⓘ "A new picture goes on within the hour after it's out, not only at the change time."; toggle → tertiary.
- **G2** on + Set → `Wset`; summary unchanged (never named); alarms for the next HH:15 + off local (hourly all day), or `<now>` when an hourly check since the last run's start is owed (likely, if the last run was ~09:00).
- **G3** off again + Set → alarms tomorrow 05:0m Z.

### H. If APOD is a video (daily on)
- **H1** dropdown "Skip it ▾" (content-desc "If APOD is a video: Skip it"); choices Skip it, NASA Image of the Day, Earth Observatory, Use my picture.
- **H2** NASA Image of the Day → tertiary; back to Skip it → no highlight.
- **H3** NASA + Set → `Wset` (today is an image day: nothing changes visually; both screens set again). Video notice not shown (not a video day).
- **H4** "Use my picture" with no `fallback.jpg` and no draft pick → the photo picker opens; Back/cancel → choice stays "Skip it", nothing highlighted (MainActivity.kt:1165-1167). If a `fallback.jpg` exists → no picker, choice becomes "Use my picture" at once with "Change picture" beside it (use this to learn whether one exists; choose "Skip it" again to undo).
- **H5** pick a photo → copied (a toast "Couldn't use that picture" if not an image); choice "Use my picture", "Change picture" button, tertiary (draftFallback).
- **H6** "Change picture" → picker → pick → still highlighted; cancel → previous pick kept.
- **H7** Use my picture + Set → `fallback-draft.jpg` renamed to `fallback.jpg`; `Wset` (records re-set; image day). Not undoable from the UI.
- **H8 SUSPECT S3** pick a photo (H5), then switch back to "Skip it" → no highlight (videoDaysChanged false, MainActivity.kt:2143), yet Set still renames the abandoned pick over `fallback.jpg` (`val pick = draftFallback`, MainActivity.kt:1047; Store.kt:364) and makes an order-only Set not order-only (Store.kt:374 `!pick`). Check: next "Use my picture" opens no picker and uses that photo. Destructive; only with the user's OK.
- **H9** greyed (not hidden) in list mode while APOD is ticked nowhere (D3).
- **H10** daily off → hidden, choice back to stored, a new pick dropped from the draft (file kept) (MainActivity.kt:1178-1183, 1906).

### I. Darken bright pictures (always shown)
- **I1** ⓘ "Darkens a bright picture a little, so the clock and widgets stay readable."; toggle off → tertiary; Setup highlighted (also with the daily switch off).
- **I2** off + Set → `Wset`; `Set:` entries have no `, darker` suffix. Visual: a bright picture is brighter than before.
- **I3** on + Set → entries carry `, darker ×0.xx (grey nnn)` for a picture whose top third is > 100 grey; else identical.

### X. Wide pictures sideways (always shown; SW3, 0.8.0; texts placeholders)
The switch after "Darken bright pictures", the pref `wideSideways`, default off; the rule `:core`'s
`sidewaysByDefault` (width ≥ 1.5 × height, by the file's upright size, `Store.kt`'s `uprightSize`), the
value used `Store.turnsSideways` (the part's noted `<source>.sideways`, else the rule while the
**stored** switch is on). Needs a 3:2 or wider picture on a screen (9 Nov's APOD faked into `files/`
with the skill's recipe), a 1.2:1 one and a portrait one for the upright cases.
- **X1** ⓘ "About Wide pictures sideways" → dialog "Turns a wide picture a quarter turn, so more of it
  fills the screen. ⛶ › Sideways changes one picture." → OK. Toggle on → label and switch tertiary,
  screen readers "On, not set yet"; Setup's title tertiary (also with the daily switch off: never
  hidden, never put back); the summary unchanged (it never names it).
- **X2** on + Set → `Wset`; each screen set afresh: a 3:2 or wider picture without a noted part
  `Set: wallpaper set: <pic> on <screens>, sideways[, darker …]` (`debug-set.png` turned); a 1.2:1, a
  portrait or square one, and "my picture", without ", sideways" (`setStream`, upright). Not "the
  order alone": a Set of the switch with a new order sets.
- **X3** a part noted by ⛶'s pill wins: one ✓'d "Sideways" off (`<source>.sideways=false`) stays
  upright with the switch on; one ✓'d on stays sideways with the switch off. An untouched pill at ✓
  leaves the key missing (the switch's default).
- **X4** off + Set → `Wset`; the wide picture upright again (no ", sideways"), a noted "on" part
  still sideways.
- **X5** with Live on home (Size Live): the hand-over of a wide picture without a noted part notes
  `live.sideways=true`, `wallpaper shown live: <pic> on HOME[, …], sideways`; the engine's `Live:
  decoded W×H` portrait-shaped.
- **X6** a rotation and the system killing the app (P19's way) keep the draft's switch and its
  highlight; a Recents swipe drops it.

### J. Size Fill (Live itself out of scope)
- **J1** from B0 (Live on home): Size "Fill" → segment tertiary, Setup highlighted → Set → no picker dialog (opensPicker false for Fill) → `Wset`; `Set: wallpaper set: N13 on HOME, A15 on LOCK`; dumpsys: home ImageWallpaper (APODroid's LiveWallpaper gone; maybe `Live: engine … destroyed`); summary "Home: NASA · Lock: APOD · 06:00" (no " · Live"); no "Live isn't on" line.
- **J2** with Fill and a column on home emptied later, the home wallpaper is just left alone.

### K. The daily switch ("Change automatically", acts at once)
- **K1** off: hides status lines, New picture at, As soon as it's out, Change every, If APOD is a video, Advanced settings; those drafts revert to stored (Setup highlight drops if they were the only change). Size, Darken, Where/list, Set stay. Summary drops the time part. No toast, no log; dumpsys alarm/jobscheduler: nothing for the app.
- **K2** Set with daily off → one-off set (keepUntilAnchor with daily false → top of each column), `Wset`, no alarm line, alarms stay cancelled.
- **K3** on: toast `Wset`, `Daily job: alarms for …` then `Switch on: wallpaper set: …` (owners kept before A, set again); status lines back; the reverted settings show the stored values; the draft's other changes stay a draft.
- **K4** Set disabled (greyed) while the switch-on run goes (~1–3 s) (MainActivity.kt:1966).
- **K5** quick taps off/on/off/on (<1 s apart) → ends on, one `Switch on:` set (B6), alarms armed; an extra set is accepted if the taps straddle the redraw.

### L. Leaving the draft
- **L1** rotation: draft, Setup/Advanced open state, chosen source, an open ⓘ or weekday dialog survive; open dropdown menus close; two panes in landscape (settings right).
- **L2 NOTE** Back, then the launcher icon: on Android 12+ Back on the root launcher activity moves the task back without finishing it (no onBackPressed override except crop, MainActivity.kt:1422), so the draft and highlight are still there. CLAUDE.md:377 "closing the app drops it" — expected only for a Recents swipe. If the draft is gone, the activity was finished (also acceptable); record which.
- **L3** swipe from Recents, reopen: draft dropped (stored values, no highlight), Setup closed, `fallback-draft.jpg` deleted, the page opens on the source on home (NASA) (MainActivity.kt:661).
- **L4** system kill (Home, `adb shell am kill io.github.buerlino.apodroid`, `pidof` empty; won't work while Keep running is on or Live's engine runs), reopen from Recents: draft restored from saved state, Setup open (it differs), parts re-read from the store.
- **L5** singleTop: page on top, `adb shell am start -n io.github.buerlino.apodroid/.MainActivity` → same page, draft kept, one MainActivity in `dumpsys activity activities`.
- **L6** stacked page: with a draft change, open another app's screen from the page (the photo picker via "Use my picture", or App info from the pause hint), then `am start -n …/.MainActivity` → a second page with the stored setup (no highlight); Back → the other app's screen → Back → the first page with its draft intact. A Set from the second page saves its own draft; the first page's highlight then compares with the new stored setup.
- **L7** Set then rotate at once: the toast still shows, Setup closes on the rotated page (B13).
- **L8** Set then Back at once: the set completes; Android may suppress the toast (app in background).

### M. Set outcomes and toasts
- **M1** `Wset`: any screen set since saveSetup (also by a page load in between).
- **M2** Set with no change (daily on, before A) → `Wset`, both screens set again (ids change), Setup stays closed.
- **M3** `NTS`: order alone (D13). Also an APOD video day with "Skip it" alone (not today).
- **M4** `CantSet`: a set threw (not provokable safely). Setup stays open.
- **M5** `CantSave`: commit failed (full disk; not testable). Setup stays open; sets still run.
- **M6** Set button greyed during Set and during the switch-on run.
- **M7** Set with crop mode open → the dragged part is kept and saved with the setup (don't run on the user's phone: changes today's part).
- **M8** Set always re-arms (`Daily job: alarms for …`) with daily on, cancels with it off.

### N. Page state after a Set
- **N1** list mode stored: title "NASA ▾" on open (home's source), menu APOD/NASA/Earth with ✓ on the shown one; swipe moves pages; chip "On home screen" (NASA page), "On lock screen" (APOD page), none on Earth; "On wallpaper" when a post is on both. Source line "NASA Image of the Day · 13 November 2026" above the title.
- **N2** APOD mode (draft or stored): title "APOD" only, no chip (chip only with ≥2 offered sources, MainActivity.kt:1508), title then date.
- **N3** status lines (daily on): list mode, different pictures → "Home: 15 Nov, HH:mm · NASA" + "Lock: 15 Nov, HH:mm · APOD"; same picture on both → one "Changed 15 Nov, HH:mm · Earth"; APOD alone ticked → no " · APOD"; a screen never set has no line; APOD mode → one line for "Where"'s screen.

---

## 2. Phone test list (chained; run all on 15 Nov after 06:30, before 16 Nov 06:00)

Each step starts where the previous ended. Before every Set: `adb logcat -c`; after: `adb logcat -d -s APODroid`, `adb shell dumpsys wallpaper` (ids), a screenshot. Always "Dismiss" the weekday dialog (never "Don't show again").

| # | Leaf | Steps | Expected |
|---|------|-------|----------|
| P0 | baseline | Open the app. Screenshot. Note summary, status lines, chip, title. Setup ▾ → note Earth's greyed dropdown (E0). Turn "More NASA pictures" off → note Where (W0) → on again (no highlight left). Advanced settings ▾ → note Keep running. "If APOD is a video" → "Use my picture": picker opens? (cancel; if it doesn't, a my picture exists: choose "Skip it" again). Close Setup. | Summary "Home: NASA · Lock: APOD · 06:00 · Live"; nothing highlighted at the end. dumpsys: home LiveWallpaper, lock ImageWallpaper. |
| P1 | J1 | Setup ▾ → Size "Fill" → Set. | No dialog; `Wset`; `Set: wallpaper set: N13 on HOME, A15 on LOCK`; home now ImageWallpaper; summary without " · Live"; Setup closed; status lines "Home: …HH:mm · NASA", "Lock: …HH:mm · APOD". |
| P2 | M2 | Set (no change). | `Wset`; same `Set:` line; both ids change. |
| P3 | D12, D13 | Setup ▾ → drag NASA's ≡ above APOD → screenshot (≡ of NASA and APOD tertiary, Setup title tertiary) → Set. | `NTS`; no `Set:` line; ids unchanged; Setup closes; anchor now 16 Nov. |
| P4 | **D17 S1** | Setup ▾ → mode off → Where: a value ≠ W0 → mode on → screenshot (Setup title NOT tertiary) → drag APOD's ≡ back to the top (APOD, NASA, Earth) → Set. Then mode off → read Where → mode on. | Doc: `NTS`. Code: `Wset` + `Set: … N13 on HOME, A15 on LOCK`; Where shows the new value without highlight (saved). Either outcome: record it. |
| P5 | D1, E1, E2, L1 | Setup ▾ → tick Earth → weekday dialog → rotate phone with the dialog open → dialog still there → Dismiss → (E0=Both) "Change every" appears "24 h" → tap its ⓘ → read → OK → rotate back. | Dialog text as D1 (absent if hidden earlier); Earth row tertiary; ⓘ text as E2; draft survives rotation. |
| P6 | E3 | "Change every" 6 h → untick Earth → re-tick Earth → Dismiss. | Change every disappears, then back on "24 h" (6 h lost). |
| P7 | D8-like | Set (Earth on E0, 24 h). | `Wset`; `…N13 on HOME, A15 on LOCK` (owners kept); summary "Home: NASA, Earth · Lock: APOD, Earth · every 24 h from 06:00" (for E0=Both); Earth page no chip. Expected later 16 Nov 06:00: home NASA, lock APOD; 17 Nov Earth. |
| P8 | F1, K1 | Setup ▾ → New picture at 08:00 (tertiary) → turn "Change automatically" off. | New picture at, As soon, Change every, If APOD is a video, Advanced, status lines vanish; Setup title not tertiary; summary "Home: NASA, Earth · Lock: APOD, Earth"; no alarm in dumpsys alarm. |
| P9 | D18, K2 | Drag Earth's ≡ to the top → Set. | `Wset`; `Set: wallpaper set: E13 on HOME+LOCK` (one shared wallpaper); no `Daily job: alarms`; Earth page chip "On wallpaper"; summary "Home: Earth, NASA · Lock: Earth, APOD". |
| P10 | K3, K4 | Turn "Change automatically" on; immediately try Set. | Set greyed during the run; `Wset`; `Daily job: alarms for 2026-11-16T05:…Z`, `Switch on: wallpaper set: E13 on HOME+LOCK`; status one line "Changed 15 Nov, HH:mm · Earth"; New picture at shows 06:00 (draft 08:00 lost); summary "Home: Earth, NASA · Lock: Earth, APOD · every 24 h from 06:00". Expected later: Earth stays to 17 Nov 06:00. |
| P11 | D-order back | Setup ▾ → untick Earth → drag Earth's ≡ to the bottom (APOD, NASA, Earth) → Set. | `Wset`; `…N13 on HOME, A15 on LOCK` (owner Earth gone → top); summary "Home: NASA · Lock: APOD · 06:00". |
| P12 | D3, D4, H9, D7 | Setup ▾ → untick APOD → screenshot → tap NASA's tick → re-tick APOD. | NASA tick greyed and unchangeable; If APOD is a video greyed; APOD dropdown greyed "Lock screen"; after re-tick nothing highlighted. No Set. |
| P13 | D5 | Untick NASA → Set. | `Wset`; `Set: wallpaper set: A15 on LOCK` only; home id unchanged; summary "APOD · lock screen · 06:00"; status "Home: <P11 time> · NASA" and "Lock: 15 Nov, HH:mm"; NASA chip still "On home screen". |
| P14 | D6 | Setup ▾ → tick NASA (goes to Home screen) → Dismiss dialog (NASA wasn't ticked) → Set. | `Wset`; `…N13 on HOME, A15 on LOCK`; summary back to "Home: NASA · Lock: APOD · 06:00". |
| P15 | B1, N2, C1 | Setup ▾ → mode off → observe pager/title/chip → Where "Both" → Set. | Title "APOD", no ▾, no chip; `Wset`; `Set: wallpaper set: A15 on HOME+LOCK`; summary "APOD · both screens · 06:00"; one status line "Changed 15 Nov, HH:mm". |
| P16 | C2 | Setup ▾ → Where "Home screen" → Set. | `Wset`; `…A15 on HOME`; dumpsys: lock now has its own wallpaper (same APOD); summary "APOD · home screen · 06:00". |
| P17 | **D20 S2** | Setup ▾ → mode on → NASA dropdown "Both" → mode off → screenshot (Setup title tertiary?) → Set → mode on → read NASA's dropdown → screenshot. | Code: Setup not tertiary before Set; `Wset` `…A15 on HOME`; after, NASA shows "Both ▾" un-highlighted (saved). Doc expects the hidden list not saved. |
| P18 | B5 | NASA dropdown "Home screen" (mode is on) → Set. | `Wset`; `…N13 on HOME, A15 on LOCK`; summary "Home: NASA · Lock: APOD · 06:00"; pager 3 sources, title "NASA ▾", chips as N1. |
| P19 | L4 | Setup ▾ → Darken off (tertiary) → Home button → `adb shell am kill io.github.buerlino.apodroid` → `pidof` empty → reopen from Recents. | Darken still off and tertiary, Setup open. (If pidof isn't empty, Keep running keeps the process: skip.) |
| P20 | L5 | With that draft, `adb shell am start -n io.github.buerlino.apodroid/.MainActivity`. | Same page, draft kept; one MainActivity record. |
| P21 | L2 NOTE | Back → launcher icon. | Expected (Android 16): draft still there. Record whether it's dropped. |
| P22 | L3 | Swipe APODroid from Recents → launcher icon. | Draft dropped: Darken on, no highlight, Setup closed, opens on NASA. |
| P23 | L6 | Setup ▾ → Darken off → "If APOD is a video" → "Use my picture" (picker opens; only if P0 found none) → `am start -n …/.MainActivity` → second page: Setup closed, nothing highlighted → Back → picker → Back → first page. | Second page = stored setup; first page keeps Darken off highlighted, choice "Skip it". Skip the step if a my picture exists. |
| P24 | I2, L7 | Set (Darken off) and rotate at once. | `Wset` toast shows; Setup closes on the rotated page; `Set:` entries without `, darker`. |
| P25 | I3 | Setup ▾ → Darken on → Set. | `Wset`; entries with `, darker ×…` when bright, else as P24. |
| P26 | F2, G1, G2 | Setup ▾ → New picture at 07:00 → As soon as it's out on → its ⓘ → OK → Set. | `Wset`; summary "Home: NASA · Lock: APOD · 07:00"; alarms at the next HH:15+off local (or `<now>` + job 2 run setting nothing). |
| P27 | G3, H2, H3 | Setup ▾ → New picture at 06:00 → As soon off → If APOD is a video "NASA Image of the Day" → back to "Skip it" (no highlight) → "Earth Observatory" → Set. | `Wset`; `Daily job: alarms for 2026-11-16T05:0…Z`; summary "… · 06:00". |
| P28 | K5 | "Change automatically": off, on, off, on in quick taps. | Ends on; one `Switch on: wallpaper set:` (an extra one accepted if the taps straddle the redraw); alarms armed. |
| P29 | **H8 S3** (optional, only with the user's OK; changes "my picture" for good) | If APOD is a video "Use my picture" → pick a photo → "Skip it" → Set → then "Use my picture" again → "Skip it". | Code: no highlight before Set, `Wset`; afterwards "Use my picture" opens no picker (the abandoned pick became `fallback.jpg`). |
| P30 | restore | Setup ▾ → mode off → Where W0 → mode on → If APOD is a video "Skip it" → Size "Live" → Set → dialog "Next, in Android's preview: Set wallpaper › Home screen  Back there: nothing changes." → OK → Android's preview "Set wallpaper" → "Home screen". | `Wset`; log `Set: Android's live wallpaper picker`, `Live: back from the picker, a wallpaper changed, home live`, `Live: the engine is on HOME, not noted live`, `Daily job: alarms for 2026-11-16T05:0…Z`, `Set: wallpaper set: A15 on LOCK; wallpaper shown live: N13 on HOME`; summary "Home: NASA · Lock: APOD · 06:00 · Live"; home LiveWallpaper, lock ImageWallpaper. Then Setup ▾: mode off shows Where W0 (if P4 showed Where isn't saved while hidden, do an APOD-mode Set of W0 before this step), Earth's greyed dropdown E0, order APOD, NASA, Earth, nothing highlighted. |
| P31 | X1, X6 | (SW3; the emulator or a phone with a 3:2 picture on a screen) Setup ▾ → "Wide pictures sideways" ⓘ → read → OK → toggle on → screenshot → rotate → screenshot. | Dialog text as X1; label and switch tertiary, Setup title tertiary, summary unchanged; after the rotation still on and tertiary. |
| P32 | X2 | Set. | `Wset`; `Set: wallpaper set: <wide pic> on …, sideways`; a 1.2:1 or portrait picture on a screen without ", sideways". |
| P33 | X3 | ⛶ on the wide picture → "✓ Sideways" shown (white) → tap it off → ✓. | `Wset`; `Part: wallpaper set: … ` without ", sideways"; `<source>.sideways=false` (debug). The next post of that source sideways again (page.md K24). |
| P34 | X4 | Setup ▾ → toggle off → Set. | `Wset`; the wide picture upright (no ", sideways"); a part ✓'d "Sideways" on still turned. Restores the switch's default. |

Restored by P30: mode on, list home NASA / lock APOD, order default, Where W0, Size Live, 06:00, As soon off, Skip it, Darken on, 24 h, daily on, Earth's and NASA's lastScreens. Not restorable: `orderFrom` (16 Nov), `lastRunAt`/`failedRuns` (the switch rewrites them), wallpaper ids, and `fallback.jpg` if P29 ran.
Expected later after P30 (not testable now): 16 Nov ~06:15+off lock gets APOD 16 Nov (if an image); home stays NASA 13 Nov (the engine) until 17 Nov 06:00, since Monday's NASA post comes after the 06:00–09:00 window.
