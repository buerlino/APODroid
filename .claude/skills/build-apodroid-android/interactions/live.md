# APODroid: Set as wallpaper × Size [Fill | Live] × Android's live wallpaper picker

> A snapshot read at 7299474 (15 Nov 2026); line numbers are of that commit (`git show 7299474:<path>`).
> The SUSPECTs below were fixed or accepted since: see [README.md](README.md). Newer changes: README, Keeping it current.

What happens for each user path, read from master 7299474 (what the phone runs; the working tree
differs only by the dialog's last line). Read-only. No build, no device touched.
Phone: Fairphone 6, LineageOS, Android 16 (API 36), so every 14+ branch applies and every
below-14 branch is left out.

Sources read: CLAUDE.md (Settings: Size, Set as wallpaper with "The picker" and "Back without a pick",
the daily switch, Wallpaper: Live, Is it on, Lost Live, The engine on the lock screen alone, A shared
lock screen follows home, B27/B28, Choosing the part: With Live, D, Next features 0.7.0: Live);
`MainActivity.kt` (setAsWallpaper 1039–1109, pickLive 1124–1154, openLivePicker 432–443,
PickerDialog 2493–2502, onCreate/onDestroy/onResume 468–570, load 673–704, switchDaily/switchOn
918–968, statusLines 798, setupSummary 2153–2172, ⛶ 1301–1340, 1512, 1801); `Store.kt`
(saveSetup 363–403, setPart 428–454, update 925–951, setDue 1040–1197, opensPicker 1287–1290,
liveMissing 1297–1298, wallpaperIds 1301–1304, awaitLive 1313–1319, copyForLive 1359–1373,
liveFallback 1380–1383, liveState 1440–1450, lockEngine 1468–1475); `LiveWallpaper.kt`
(onCreate 94–109, onVisibilityChanged 140–154, checkStray 188–193, runIfOwed 201–210, reload 216–260);
`core/.../Live.kt` (stillOrLive 87–115, lostLive 164–185, lockFollowsHome 200–205, handOverWaits
215–216, unnotedLive 229–235, equalColumns 238–241, opensPicker 259–263); `core/.../Screens.kt`
(Plan.sets 170–175, restartAndForget 408–430, keepUntilAnchor 446+).

---

## 0. How Set runs now (7299474), in one paragraph

Tap "Set as wallpaper" → `setting = true` → `Store.opensPicker(draft)` (`:core` opensPicker with the
**stored** columns as "before", the **draft's** columns and Size, and whether home is live now):
- **No picker** (Size Fill; or no home column; or home live and not (14+, lock not shared, draft
  columns equal, stored columns differ)): `saveSetup` (records of screens with a column forgotten,
  their `wallpaperLive` notes removed, Store.kt:792) → `update(set = true)` → `setDue` → toast.
- **Picker**: `pickLive` (MainActivity.kt:1124): `homeWasLive` read, then the **dialog** (process-wide
  `pickerAsk`): "Next, in Android's preview:\nSet wallpaper › Home screen" (or "› Home screen and lock
  screen" when the **draft's** columns are equal); **the phone's build also shows "\n\nBack there:
  nothing changes."** Cancel / system Back / tap outside / the page destroyed for good → `false` →
  logs `Set: the picker's dialog cancelled`, `Set: no pick, nothing saved`, nothing saved, no toast.
  OK → wallpaper ids of both screens noted → picker started only if the page is RESUMED (else
  `Set: no picker opened, Fill` and Set **goes on**) → `Set: Android's live wallpaper picker` → wait
  for the activity result (or any page's `onResume`) → `awaitLive`: `changed` = an id differs; if
  changed, up to 3 s for home live; logs `Live: back from the picker, <a wallpaper changed|nothing
  changed>, home <live|not live>[, shared]` → returns `changed || homeWasLive`. False → `Set: no pick,
  nothing saved` (no toast, draft and highlight stay). True → `saveSetup` → `update(set = true)` →
  toast. After any Set that got to `saveSetup`: `scheduleJob` (daily on: `Daily job: alarms for …`)
  or `DailyJob.cancel` (daily off).

Inside `setDue` (Store.kt:1040), in this order: `lostLive` (Live.kt:164: home noted live and home not
live → forget home (+lock with a column) → `Live: home isn't live, Fill on HOME+LOCK`; **else** the
engine on the lock screen alone while home isn't live → forget LOCK (lock column) or HOME + lock takes
home's (no lock column) → `Live: the engine is on the lock screen alone, Fill on …`; else lock noted
live and not shared → forget LOCK) → `unnotedLive` (Size Live and home live: home with a column and not
noted, lock with a column, shared and not noted → `Live: the engine is on HOME[+LOCK], not noted live`)
→ the rules' plan (one set with both flags when both screens are due the same picture) →
`lockFollowsHome`/B27 → `stillOrLive` (Size Live and home live: home's set to the engine; the lock
screen's to the engine only while shared **and** columns equal, else still `FLAG_LOCK` first) → the
lock-screen still sets, then the hand-over. Log line of the run:
`Set: wallpaper set: <A> on LOCK; wallpaper shown live: <B> on HOME` where a picture prints as
`Of(source=NASA, date=2026-11-13, url=https://…)`, optionally with `, darker ×0.78 (grey 141)`.

---

## 1. Definitions used in the leaves

**Setups (the draft Set saves)**
- **A-H** APOD mode, Where Home: columns home [APOD], lock [].
- **A-L** APOD mode, Where Lock: home [], lock [APOD].
- **A-B** APOD mode, Where Both: home [APOD], lock [APOD].
- **M-EQ** More NASA pictures, equal columns (e.g. NASA on Both).
- **M-DIFF** More NASA pictures, different columns (phone now: home [NASA], lock [APOD]).
- **M-HE** More NASA pictures, home column empty (every ticked source "Lock screen").

**States before Set (what the phone shows)**
- **P0** Live nowhere: both screens APODroid stills (Fill).
- **P1** Live on home only: APODroid's engine on home, lock screen its own still (the phone now).
- **P2** Live shared: one engine on both screens (`getWallpaperId(FLAG_LOCK)` = -1).
- **P3** APODroid's engine on the lock screen alone, home a still.
- **P4** Another app's wallpaper on home (P4a: picked while Fill, records not noted live; P4b: picked
  while home was APODroid's Live, home's record still noted live).

**Pruning (combinations whose outcome can't differ)**
1. **A-B ≡ M-EQ** and **A-L ≡ M-HE** for everything here: the code only looks at the columns map
   (`equalColumns`, `columns[HOME].isEmpty()`), never at the mode (Live.kt:95, 238–241, 259–263).
   Kept separately only where the page texts differ (status line, summary).
2. **Size Fill never opens the dialog or the picker** in any state (Live.kt:260): one leaf per
   relevant start state (N1–N3, N11).
3. **Dialog Cancel ≡ system Back ≡ tap outside**: all call `ask.answer.complete(false)`
   (MainActivity.kt:2496–2498). Kept as three leaves only because they are three taps to check.
4. **Daily switch off ≡ on for the Set/picker flow**: `opensPicker` ignores it, `update(set = true)`
   sets either way (Store.kt:947); only the status lines (hidden), the summary (no time), Advanced
   (hidden) and the job (cancelled) differ. One leaf (N10), plus P3 below.
5. **P4a ≡ P0** for Set (home not live, nothing noted live). **P4b** never reaches Set with the daily
   switch on: the page's own load at `onResume` already sets Fill on both (X5). With it off, ≡ P0.
6. **P3 with the daily switch on** doesn't survive opening the app: the page's load runs `setDue`,
   whose `lostLive` replaces it (X8). So "Set from P3" only with the daily switch off (K17, K18).
7. **P2 never opens the picker** (Live.kt:262 needs `!shared`; Live.kt:261 needs home not live).
8. **P1 opens the picker only when the draft makes different stored columns equal** (Live.kt:262);
   any other P1 draft is the no-picker branch (N4, N6, N7, N9).
9. **"Back from the choice sheet"** ≡ staying in the preview (the sheet is a dialog inside the preview
   activity; confirm on the phone); the outcome is decided by how the preview is left.
10. Below-14 paths (`lockShares` false, the engine noting its id, R2a's re-apply) are out: the FP6 is 16.

**Standard checks (refer to them as C1–C5)**
- **C1 log**: `adb logcat -v time -s APODroid:*` (start before each case; the engine logs under the
  same tag).
- **C2 screens**: `adb shell dumpsys wallpaper` → read the "System wallpaper state" and "Lock wallpaper
  state" blocks: the component (APODroid's `io.github.buerlino.apodroid/.LiveWallpaper` vs
  `ImageWallpaper`), the id, `mWhich`. An empty/absent lock block = the lock screen shares home's
  wallpaper (P2 when home is APODroid's). Check the exact field names once in T01.
- **C3 prefs** (debuggable build only; `run-as` fails on a release build):
  `adb shell run-as io.github.buerlino.apodroid cat shared_prefs/apodroid.xml | grep -E 'name="(size|allSources|where|home|lock|daily|wallpaperLive|lock\.wallpaperLive|live\.(shared|homeId|picture|whole|darken)|wallpaperPicture|lock\.wallpaperPicture|wallpaperDate|lock\.wallpaperDate)"'`
- **C4 process**: `adb shell pidof io.github.buerlino.apodroid` (alive or not, pid changed or not).
- **C5 page**: status lines under "Change automatically", Setup row title colour (tertiary =
  highlighted) and summary, Setup open/closed, Size segment highlight, the toast.

Setup summary formats (stored setup, MainActivity.kt:2153): "Home: NASA · Lock: APOD · 06:00 · Live"
(M-DIFF), "NASA · both screens · 06:00 · Live" (M-EQ), "APOD · home screen · …", "APOD · lock screen ·
…", "APOD · both screens · …"; without the time while the daily switch is off; " · Live" only with
stored Size Live. Status lines (daily on only): "Home: 15 Nov, 11:02 · NASA" / "Lock: 15 Nov, 11:02 ·
APOD", one line "Changed 15 Nov, 11:02 · NASA" when both screens' records name the same picture
(APOD mode: one line for the screen "Where" names, no " · APOD" suffix); plus "Live isn't on. Set as
wallpaper turns it on." while stored Size Live, a home column, home not live (Store.kt:1298).

---

## 2. The decision tree

```
Set as wallpaper
├─ draft Size Fill ──────────────────────────────────── no dialog, no picker → N1 N2 N3 N11
└─ draft Size Live
   ├─ draft home column empty (A-L, M-HE) ───────────── no dialog → N8 (SUSPECT)
   ├─ home live now
   │  ├─ shared (P2) ─────────────────────────────────── no dialog → N5, N6
   │  └─ not shared (P1)
   │     ├─ draft columns equal, stored differ ───────── dialog "…Home screen and lock screen"
   │     │   ├─ dialog Cancel/Back/outside/rotate/leave/swipe → D1–D6
   │     │   └─ OK → picker → K13 (Back: SUSPECT) K14 (both) K15 (home) K16 (lock)
   │     │                     L5 (swipe from Recents: SUSPECT)
   │     └─ otherwise ──────────────────────────────────── no dialog → N4, N7 (SUSPECT), N9, N10
   └─ home not live (P0, P3, P4a)
      ├─ dialog text: draft columns equal → "…Home screen and lock screen", else "…Home screen"
      ├─ dialog Cancel/Back/outside/rotate/leave/swipe ─ D1–D6, D8
      └─ OK → picker
         ├─ M-DIFF:  K1 back, K2 sheet-back, K3 home, K4 lock (SUSPECT), K5 both
         ├─ M-EQ/A-B: K6 both, K7 home (SUSPECT), K8 lock (SUSPECT), K9 back
         ├─ A-H:     K10 home, K11 both, K12 lock
         ├─ from P3 (daily off): K17 home, K18 back
         └─ leaving the picker: L1–L4, L6, L7
Afterwards: A1–A10      Outside the app: X1–X9
```

---

## 3. Leaves

Format: **id** — preconditions | steps | expected screen | expected log | adb check. "Set" = tap
"Set as wallpaper". "Toast WS" = "Wallpaper set".

### N: Set without the dialog

**N1** Size Fill from P1 (M-DIFF, the phone now).
- Steps: Setup › Size › Fill; Set.
- Screen: Toast WS. Setup closes; summary "Home: NASA · Lock: APOD · 06:00" (no " · Live"). Home: NASA
  13 Nov still (Fill, the stored ⛶ part); lock: APOD 15 Nov still. Status lines Home/Lock, no "Live
  isn't on".
- Log: `Set: wallpaper set: Of(source=NASA, date=2026-11-13, …) on HOME, Of(source=APOD, date=2026-11-15, …) on LOCK`
  (order HOME, LOCK), `Live: engine home destroyed`, `Daily job: alarms for …`. No `not noted live`
  (Size Fill, Live.kt:230).
- adb: C2 both ImageWallpaper, two ids; C3 `size=FILL`, no `wallpaperLive` keys.
- Next job: at 06:00 + offset tomorrow the lock screen's APOD 16 Nov (out 06:05 Zurich) at the 06:15
  check, still; home NASA unchanged unless NASA posted before 09:00.

**N2** Size Fill from P2 (M-EQ, both live).
- Steps: Size › Fill; Set.
- Screen: Toast WS; one set with both flags: both screens the same still; one status line "Changed …
  · NASA".
- Log: `Set: wallpaper set: Of(source=NASA…) on HOME+LOCK`, `Live: engine home destroyed`.
- adb: C2 one ImageWallpaper, lock block empty (shared still).

**N3** Size Fill from P3 (daily switch off, so P3 survived; M-DIFF).
- Steps: Size › Fill; Set.
- Screen: Toast WS; both stills; the engine gone from the lock screen.
- Log: `Live: the engine is on the lock screen alone, Fill on LOCK` (lostLive's second branch runs for
  Fill too: Store.kt:1054, Live.kt:178), then `Set: wallpaper set: … on HOME, … on LOCK`.
- adb: C2 no APODroid component anywhere.

**N4** Size Live from P1, draft unchanged or with other non-column changes (the phone now).
- Steps: Set (nothing changed).
- Screen: no dialog. Toast WS. Setup stays closed (it closes after any good Set). Home: engine, NASA
  13 Nov panning; lock: APOD 15 Nov still (set again: a short flicker). Status Home/Lock with the new
  time.
- Log: `Live: the engine is on HOME, not noted live` (the Set forgot the record, Store.kt:792, so
  `unnotedLive` fires), `Set: wallpaper set: Of(source=APOD…) on LOCK; wallpaper shown live:
  Of(source=NASA…) on HOME`, `Live: decoded …` only if `live.jpg`'s key changed (same file rewritten:
  its time changes, so yes), `Live: drew Of(source=NASA…) p=0.xx …`.
- adb: C2 home APODroid component, lock ImageWallpaper (own id); C3 `wallpaperLive=true`,
  `lock.wallpaperLive` absent, `live.shared=false`.

**N5** Size Live from P2 (M-EQ), Set unchanged.
- Screen: no dialog, Toast WS, both screens the engine; one status line "Changed … · NASA".
- Log: `Live: the engine is on HOME+LOCK, not noted live`, `Set: wallpaper shown live: Of(source=NASA…) on HOME+LOCK`.
- adb: C2 home APODroid, lock block empty; C3 both `wallpaperLive=true`, `live.shared=true`.

**N6** Size Live from P2, draft splits the columns (M-EQ → M-DIFF).
- Screen: no dialog (draft columns differ), Toast WS; home engine NASA, lock still APOD (B28).
- Log: `Live: the engine is on HOME+LOCK, not noted live` (shared at the run's start), `Set: wallpaper
  set: Of(source=APOD…) on LOCK; wallpaper shown live: Of(source=NASA…) on HOME`, engine `Live: engine
  flags 1`.
- adb: C2 lock now its own ImageWallpaper id. C3 `live.shared` still true until the next run's
  `noteShared` writes false (cosmetic).

**N7** (SUSPECT) Size Live, P1 with **equal** stored columns (after K7, K13 or K15: home engine, lock a
still of the same picture), Set again.
- Screen: no dialog, Toast WS, lock stays a still forever; nothing in APODroid offers the lock screen
  the engine again until the columns are split and made equal again.
- Log: `Live: the engine is on HOME, not noted live`, `Set: wallpaper set: … on LOCK; wallpaper shown live: … on HOME`.
- SUSPECT: Live.kt:262 `!equalColumns(before)`: "once per split". After K13 (a plain Back) or K7
  (the "wrong" answer) the user can't get both screens live from the app. Workaround: X3.

**N8** (SUSPECT) Size Live with no home column (A-L or M-HE), any state.
- Steps: Where › Lock screen (or every source on "Lock screen"); Size › Live; Set.
- Screen: no dialog. Toast WS. Lock: APOD still. Home: left alone (keeps its record and whatever it
  shows: if APODroid's engine was on home it stays there, still panning the old `live.jpg`). Summary
  "APOD · lock screen · 06:00 · Live". **No "Live isn't on" line** and no hint anywhere that Live does
  nothing here.
- Log: `Set: wallpaper set: Of(source=APOD…) on LOCK` only.
- SUSPECT: Live.kt:260 (no picker without a home column), Store.kt:1298 (`liveMissing` needs a home
  column), MainActivity.kt:2170 (" · Live" from the stored Size alone): a setting that is saved and
  shown but has no effect, silently.

**N9** Size Live, A-H, P1 (home live), Set.
- Screen: no dialog (draft columns not equal). Toast WS. Home engine APOD; lock untouched.
- Log: `Live: the engine is on HOME, not noted live`, `Set: wallpaper shown live: Of(source=APOD…) on HOME`.

**N10** Daily switch off, Size Live, P1, Set.
- Screen: as N4 but no status lines, Advanced hidden, summary without " · 06:00"; Toast WS.
- Log: as N4, no `Daily job: alarms`; the job and alarms cancelled (`DailyJob.cancel`).
- Next job: none; the engine keeps panning the same picture by the clock; no screen-on run (D needs
  `daily`).

**N11** (SUSPECT) Size Fill with A-H while one engine is shared on both (after K11 or K14's APOD-mode
state), i.e. the lock screen has no column but shows APODroid's engine.
- Steps: Where › Home screen; Size › Fill; Set.
- Screen (by the code): Toast WS. Home: APOD still (`FLAG_SYSTEM` alone). The lock screen: if Android
  moves the shared engine to the lock screen on a home-only set (as M0 saw for a shared still), the
  lock screen keeps APODroid's engine, panning; it is replaced only by the next run (next app open,
  next screen-on when no run started in 15 min, the next change time), which then puts **home's still
  on the lock screen too** although "Where" is Home only.
- Log of Set: `Set: wallpaper set: Of(source=APOD…) on HOME`; nothing about the lock screen (its
  record is kept, still noted live; at the run's start home was live and shared, so `lostLive` found
  nothing). Next run: `Live: home isn't live (stray on the lock screen)` (engine, at a lock-screen
  view), maybe `Live: screen on, run owed`, then `Live: the lock screen isn't live, Fill on HOME, the
  lock screen takes home's` (the "why" text is computed from `lock.wallpaperLive`=true, so it says
  "the lock screen isn't live", not "the engine is on the lock screen alone") and `… wallpaper set: … on HOME+LOCK`.
- SUSPECT: Store.kt:1054 `lockEngine` is read before the sets (home still live then), Live.kt:95
  (Fill: the lock screen, without a column, gets no set), so the Set itself leaves a live lock screen.
  Also Store.kt:1063–1067: the log's "why" mislabels this case.

### D: the dialog (Size Live, the dialog opened; any start that opens it)

**D1** Cancel.
- Screen: dialog gone; no toast; Set button enabled again; Setup stays open (it opened itself while
  the draft differs) with "Setup" and the changed rows (Size, the list rows) in tertiary. Nothing on
  either screen changes.
- Log: `Set: the picker's dialog cancelled`, `Set: no pick, nothing saved`.
- adb: C2 and C3 unchanged.
- Text to note: the phone (7299474) shows "Next, in Android's preview:\nSet wallpaper › Home screen\n\nBack
  there: nothing changes." (or "… › Home screen and lock screen" when the draft's columns are equal).
  The working tree drops the last line.

**D2** System Back on the dialog. = D1.

**D3** Tap outside the dialog. = D1.

**D4** Rotate the phone with the dialog open.
- Screen: the page is recreated; the dialog shows again (process-wide `pickerAsk`, same page id); the
  draft, Setup open, the highlight kept; Set still disabled. Then OK continues to the picker from the
  new page (its launcher gets the result); Cancel = D1.
- Log: nothing until the answer.

**D5** Home button with the dialog open, then back via the launcher icon (D5a) or Recents (D5b).
- Screen: the same page resumes (singleTop) with the dialog still open; answer it as D1/OK.
- Log: nothing at Home/return (no `pickerBack` exists yet; onResume only cancels the dialog of a page
  that's gone).

**D6** Swipe APODroid away in Recents with the dialog open.
- D6a process survives (Keep running on, or the home engine keeps it bound; check C4): at the swipe
  `Set: the picker's dialog cancelled`, `Set: no pick, nothing saved` (onDestroy, not a configuration
  change, MainActivity.kt:515). Reopening: a fresh page with the **stored** setup (the draft is lost),
  Setup closed, nothing set.
- D6b process killed: no log line; reopening = a cold start with the stored setup. If Live was on home,
  the engine rebinds in a new process (`Live: engine 1 created`, L8f1's grace).

**D8** (SUSPECT, timing-dependent) OK, then rotate or press Home within the fraction of a second before
the picker starts.
- By the code: `live[page]` is missing or not RESUMED → `Set: no picker opened, Fill` → `pickLive`
  returns true (MainActivity.kt:1146) → Size Live saved, both screens set still, Toast WS, "Live isn't
  on" line: exactly the old outcome 7299474 meant to remove for a back-out.
- SUSPECT: MainActivity.kt:1144–1146. Hard to hit by hand; optional.

### K: in the picker (after OK)

**K1** From P0 (or P4a), M-DIFF, dialog "› Home screen": Back from the preview.
- Screen: back in APODroid; no toast; draft and highlight as before (Size Live tertiary). Screens
  unchanged.
- Log: `Set: Android's live wallpaper picker`, `Live: engine preview created`, `Live: decoded …`,
  `Live: drew <name> p=…` (preview), `Live: engine preview destroyed`, `Live: back from the picker,
  nothing changed, home not live`, `Set: no pick, nothing saved`.
- Note the preview: it draws `live.jpg` (the last hand-over) if it exists, else the stored home
  column's first source's picture. See X4 (SUSPECT S6).

**K2** Same, "Set wallpaper" → the choice sheet → Back (sheet closes) → Back from the preview. = K1.

**K3** Same, "Set wallpaper" → "Home screen".
- Screen: back in APODroid; Toast WS; Setup closes; summary "Home: NASA · Lock: APOD · 06:00 · Live";
  home engine NASA panning; lock APOD still; status Home/Lock, no "Live isn't on".
- Log: `Live: engine 1 created` (`wallpaperFlags` 1), `Live: back from the picker, a wallpaper
  changed, home live`, `Live: the engine is on HOME, not noted live`, `Set: wallpaper set:
  Of(source=APOD…) on LOCK; wallpaper shown live: Of(source=NASA…) on HOME`, `Daily job: alarms for …`.
- adb: C2 home APODroid, lock its own still; C3 `size=LIVE`, `wallpaperLive=true`, `live.homeId` = home's id.

**K4** (SUSPECT) Same, "Set wallpaper" → "Lock screen".
- Screen: about 3 s wait after the return; Toast WS; Setup closes; Size Live **saved**; home NASA
  still, lock APOD still (the engine on the lock screen replaced); "Live isn't on. Set as wallpaper
  turns it on." under the status lines; summary ends " · Live".
- Log: `Live: engine 2 created`, possibly `Live: home isn't live (stray on the lock screen)`,
  `Live: back from the picker, a wallpaper changed, home not live`, `Live: the engine is on the lock
  screen alone, Fill on LOCK`, `Set: wallpaper set: Of(source=NASA…) on HOME, Of(source=APOD…) on
  LOCK`, `Live: engine home destroyed`.
- adb: C2 no APODroid component; C3 `size=LIVE`.
- SUSPECT: MainActivity.kt:1149 (`changed` true for any id change) and 1103: the wrong answer gives
  "Wallpaper set", Size Live saved and Fill set, the outcome the commit removed for a back-out; the
  user has to tap Set again to get the dialog. Arguably it should count as a back-out (nothing saved)
  or say "Live isn't on" in the toast.

**K5** Same, "Set wallpaper" → "Home screen and lock screen" (columns differ; the dialog said "Home screen").
- Screen: Toast WS; home engine NASA; lock APOD still (B28: the engine leaves the lock screen); the
  lock screen may show NASA for a second before its still lands.
- Log: `Live: engine 3 created`, `Live: back from the picker, a wallpaper changed, home live, shared`,
  `Live: the engine is on HOME+LOCK, not noted live`, `Set: wallpaper set: Of(source=APOD…) on LOCK;
  wallpaper shown live: Of(source=NASA…) on HOME`, `Live: engine flags 1`.
- adb: C2 lock its own still id; C3 `live.shared=true` until the next run (cosmetic).

**K6** From P0, M-EQ or A-B, dialog "› Home screen and lock screen": "Home screen and lock screen".
- Screen: Toast WS; both screens the engine, same picture; one status line.
- Log: `Live: engine 3 created`, `… home live, shared`, `Live: the engine is on HOME+LOCK, not noted
  live`, `Set: wallpaper shown live: Of(…) on HOME+LOCK`.

**K7** (SUSPECT) Same start, "Home screen".
- Screen: Toast WS; home engine; lock a still of the same picture. From now on no Set opens the
  picker for the lock screen (N7).
- Log: `Live: engine 1 created`, `… a wallpaper changed, home live`, `Live: the engine is on HOME, not
  noted live`, `Set: wallpaper set: Of(…) on LOCK; wallpaper shown live: Of(…) on HOME`.
- SUSPECT: Live.kt:262 as N7.

**K8** (SUSPECT) Same start, "Lock screen".
- Screen: as K4 but one still with both flags (equal columns): both screens the same still; "Live
  isn't on".
- Log: `Live: the engine is on the lock screen alone, Fill on LOCK`, `Set: wallpaper set: Of(…) on HOME+LOCK`.
- SUSPECT: as K4.

**K9** Same start, Back from the preview. = K1 (home wasn't live).

**K10** From P0, A-H (no lock column), "Home screen".
- Screen: Toast WS; home engine APOD; lock untouched (whatever it was). One status line "Changed …".
- Log: `Live: the engine is on HOME, not noted live`, `Set: wallpaper shown live: Of(source=APOD…) on HOME`.

**K11** Same, "Home screen and lock screen".
- Screen: Toast WS; the engine on both, the lock screen showing home's APOD too (it has no column,
  so APODroid leaves the engine there).
- Log: `… home live, shared`, `Live: the engine is on HOME, not noted live` (the lock screen has no
  column, so it isn't in `unnotedLive`), `wallpaper shown live: … on HOME`.
- adb: C3 `live.shared=true` (kept: Decision 12 later puts home's still on the lock screen when home
  is lost).

**K12** Same, "Lock screen".
- Screen: Toast WS; both screens APOD still (one set with both flags: the lock screen "takes
  home's" although Where is Home); "Live isn't on".
- Log: `Live: the engine is on the lock screen alone, Fill on HOME, the lock screen takes home's`,
  `Set: wallpaper set: Of(source=APOD…) on HOME+LOCK`.
- Note (by design, 7299474's lostLive): APODroid writes the lock screen under Where Home; the user's
  own lock wallpaper was already gone (the picker replaced it).

**K13** (SUSPECT) From P1 (the phone now), draft makes the columns equal (NASA on Both), dialog
"› Home screen and lock screen" → OK → **Back from the preview**.
- Screen (by the code): back in APODroid → **Toast WS**, Setup closes, the equal columns **saved**
  (summary "NASA · both screens · 06:00 · Live"), home engine NASA, lock NASA **still**. The phone's
  dialog promised "Back there: nothing changes." After this no Set offers the lock screen the engine
  again (N7).
- Log: `Set: Android's live wallpaper picker`, preview engine lines, `Live: back from the picker,
  nothing changed, home live`, `Live: the engine is on HOME, not noted live`, `Set: wallpaper set:
  Of(source=NASA…) on LOCK; wallpaper shown live: Of(source=NASA…) on HOME`. No `Set: no pick`.
- SUSPECT: MainActivity.kt:1149 `return changed || homeWasLive`: whenever the picker opens with home
  live (only this equalising case), every exit but the dialog's Cancel saves and sets. With the phone's
  dialog text (MainActivity.kt:2500 in 7299474) it contradicts the screen. CLAUDE.md documents it
  ("re-picking a live home may keep its id, that Set goes on"), but a plain Back can't be told from a
  re-pick here because a re-pick of "Home screen" changes home's id on AOSP (a new id per
  `setWallpaperComponent`), so the guard may be unnecessary; check K15's ids.
  Also: Cancel in this case drops the whole Set, so the column change can only be saved by going
  through the picker.

**K14** Same start, "Set wallpaper" → "Home screen and lock screen".
- Screen: Toast WS; both screens the engine NASA; one status line "Changed … · NASA"; summary "NASA ·
  both screens · 06:00 · Live".
- Log: `Live: engine 3 created` (or a re-apply), `Live: back from the picker, a wallpaper changed,
  home live, shared`, `Live: the engine is on HOME+LOCK, not noted live`, `Set: wallpaper shown live:
  Of(source=NASA…) on HOME+LOCK`.
- adb: C2 lock block empty; C3 `live.shared=true`, both `wallpaperLive=true`.

**K15** Same start, "Set wallpaper" → "Home screen".
- Screen: Toast WS; home engine; lock a still NASA; N7 thereafter.
- Log: `Live: back from the picker, a wallpaper changed|nothing changed, home live` (note which: it
  tells whether AOSP gives home a new id on a re-pick; either way Set goes on), then as N7.

**K16** Same start, "Set wallpaper" → "Lock screen".
- Screen: Toast WS; a second engine lands on the lock screen, then Set's update replaces it with a
  still NASA (the lock screen's record was forgotten by Set, so it's due). Home engine.
- Log: `Live: engine 2 created`, `… a wallpaper changed, home live`, `Live: the engine is on HOME, not
  noted live`, `Set: wallpaper set: Of(source=NASA…) on LOCK; wallpaper shown live: … on HOME`, the
  second engine's `Live: engine home destroyed`.

**K17** From P3 with the daily switch off (lock: APODroid's engine alone, home a still), Size Live,
M-DIFF, dialog "› Home screen" → OK → "Home screen".
- Screen: Toast WS; home engine NASA; the lock screen's engine replaced by APOD still (Set forgot its
  record). No status lines (daily off).
- Log: `… a wallpaper changed, home live`, `Live: the engine is on HOME, not noted live`, `Set: wallpaper
  set: … on LOCK; wallpaper shown live: … on HOME`.

**K18** Same start, Back from the preview.
- Screen: nothing saved, nothing set; the lock screen keeps APODroid's engine (daily off: no run).
- Log: `… nothing changed, home not live`, `Set: no pick, nothing saved`.

### L: leaving the picker

**L1** In the preview press Home, then the APODroid launcher icon.
- Expected: APODroid's task comes back with the picker's preview on top (the picker runs in
  APODroid's task by startActivityForResult). No log until the picker is left. Finish with Back → K1
  (or K13 when home was live).
- If instead APODroid's page shows (the picker was in its own task): `onResume` completes the wait →
  `Live: back from the picker, nothing changed, …` at once. Note which.

**L2** Same, return via Recents. = L1.

**L3** Rotate in the preview, then pick (e.g. "Home screen").
- Expected: on return the page is recreated in the new orientation; the result reaches the new page;
  K3's outcome, the toast, Setup closes on the new page.

**L4** Swipe APODroid away in Recents while the preview is open, home **not** live before.
- D-a process survives (C4): no log at the swipe (the dialog was already answered; `pickerBack` isn't
  completed by onDestroy). Set waits with `setting` true. Reopen APODroid: the new page's `onResume`
  completes the wait → `Live: back from the picker, nothing changed, home not live`, `Set: no pick,
  nothing saved`; the page is fresh (stored setup, draft lost).
- D-b process killed: nothing saved; cold start.

**L5** (SUSPECT) Same, home **live** before (K13's start: equalising draft).
- D-a process survives: on reopening, the abandoned draft is saved and set: `Live: back from the
  picker, nothing changed, home live`, then `Set: wallpaper set: … on LOCK; wallpaper shown live: … on
  HOME`, Toast WS, the stored columns now equal (the new page reads them: summary "NASA · both screens
  · …"). The user swiped the app away and gets a changed setup.
- D-b killed: nothing saved.
- SUSPECT: MainActivity.kt:556 (any page's onResume completes the old Set's wait) with
  MainActivity.kt:1149 (`homeWasLive`).

**L6** Swipe away while the preview is open, then don't reopen the app (process alive).
- Expected: Set stays pending (`setting`, `picking` true) in the process; the job still runs on the
  stored setup; nothing visible. Only the next app open resolves it (L4/L5).

**L7** Observation: after "Set wallpaper" › an answer, does the picker return to APODroid or go to the
launcher? If to the launcher, APODroid's activity result arrives only when the app is opened again:
until then nothing is saved, and a job run meanwhile uses the old Size (accepted race in CLAUDE.md).
Check the gap between `Live: engine 1 created` and `Live: back from the picker` in C1.

### A: afterwards

**A1** Daily switch off with Live on (any of P1/P2).
- Screen: status lines and Advanced vanish; summary loses the time; the engine stays and keeps panning;
  nothing set. Log: none from Set; alarms cancelled.

**A2** Daily switch on again (Live stored, home live).
- Screen: Toast WS; the same pictures handed over again (startAfresh forgets both records).
- Log: `Live: the engine is on HOME[+LOCK], not noted live`, `Switch on: wallpaper set: … on LOCK;
  wallpaper shown live: … on HOME` (or `shown live: … on HOME+LOCK`), `Daily job: alarms for …`.

**A3** Daily switch on with stored Live and home **not** live (after K4).
- Screen: no dialog (the switch never opens the picker); Toast WS; both stills; "Live isn't on" stays.
- Log: `Switch on: wallpaper set: … on HOME, … on LOCK`.

**A4** Pull to refresh (Live on, nothing new).
- Screen: spinner, no toast, nothing set. Log: no `Page:` line unless a newer post was stored (then
  `Page: wallpaper shown live: …` for home's source / `wallpaper set: … on LOCK`).

**A5** ⛶ on the NASA page (home's source) with Size Live (draft and stored).
- Screen: the frame stands where the engine has it (p from the clock), no drag, it moves once a minute.
  "Whole picture" on → the whole preview (spinner, then the screen-shaped picture); ✓ → Toast WS, the
  home engine redraws whole. ✓ with nothing changed → no toast.
- Log: `Part: wallpaper shown live: Of(source=NASA…) on HOME`, `Part: NASA 2026-11-13 set on HOME`,
  `Live: decoded …`, `Live: drew …`.

**A6** ⛶ on the APOD page (lock's source only): drag works (Fill); ✓ after a drag → Toast WS, lock
still at the new part. Log `Part: wallpaper set: Of(source=APOD…) on LOCK`.

**A7** (SUSPECT) Draft Size Fill (not Set yet) while stored Live and home live; ⛶ on the NASA page.
- Screen: the frame **drags** (⛶ follows the draft's Size, MainActivity.kt:1801); ✓ → Toast WS, but the
  home screen doesn't change position: `setPart` uses the stored Size Live and hands the picture to the
  engine, which pans by the clock (Store.kt:439). The dragged part is saved (used later by Fill).
- Log: `Part: wallpaper shown live: Of(source=NASA…) on HOME`.
- SUSPECT: draft Size vs stored Size mismatch: "Wallpaper set" for a change that isn't visible.

**A8** "Live isn't on. Set as wallpaper turns it on." rules: shown only while the daily switch is on,
the **stored** Size is Live, the stored home column isn't empty and home isn't live (Store.kt:1298);
re-read at each resume, load end and Set end. Not shown for A-L/M-HE (N8) nor with daily off.

**A9** The next job run from the phone's current state (T0): Monday 16 Nov, 06:00 + offset, then
06:15 + offset: lock column APOD → APOD 16 Nov (out 06:05 CET) set still on the lock screen at the 06:15
check (`Daily job: wallpaper set: Of(source=APOD, date=2026-11-16…) on LOCK`); home NASA: newest NASA
published before 09:00 → none new (NASA posts 15:00–23:00 CET), so no hand-over; a NASA post later
waits for Tuesday 06:00 (Soon off).

**A10** D, the screen-on check: when the home engine becomes visible and a check is owed (or the engine
is a stray while a record is noted live) and no run started in the last 15 min: `Live: screen on, run
owed`, then `Daily job: job 2 starts …`.

### X: outside the app (Android's own wallpaper settings)

**X1** Stored Live, home not live ("Live isn't on" shown), pick APODroid's live wallpaper by hand for
"Home screen", return to APODroid.
- Screen: on return the page's load hands over: no toast, the status line "Live isn't on" goes, home
  shows home's current picture via the engine.
- Log: `Live: engine 1 created`, `Live: the engine is on HOME, not noted live`, `Page: wallpaper shown
  live: … on HOME`.

**X2** Same, "Home screen and lock screen", M-DIFF.
- Screen/log: as X1 plus the lock screen gets its own still back (`Page: wallpaper set: … on LOCK;
  wallpaper shown live: … on HOME`, `Live: the engine is on HOME+LOCK, not noted live`).

**X3** Same with M-EQ (the workaround for N7): both screens become the engine at the next page load
(`Live: the engine is on LOCK, not noted live` or HOME+LOCK).

**X4** (SUSPECT) Stored Size **Fill**, pick APODroid's live wallpaper by hand ("Home screen").
- Screen: the system's preview and then home draw `live.jpg`, the copy of the **last** Live hand-over
  (possibly days or weeks old), or, without one, the home column's first stored picture. APODroid never
  hands over (Size Fill), shows no status line; the engine stays until home's next change time sets a
  still over it.
- Log: `Live: engine 1 created`, `Live: drew <live.picture> …`; no `not noted live`.
- SUSPECT: LiveWallpaper.kt:225–229 draws `live.jpg` whenever it exists; `live.jpg` is never deleted
  (Store.kt:1322, no delete anywhere); Live.kt:230 (no hand-over with Fill). The same stale copy shows
  in Set's own picker preview (K1), while CLAUDE.md says the preview "draws the stored setup's home
  picture".

**X5** Stored Live on home (P1, daily on), pick **another** wallpaper for "Home screen", return.
- Screen: at the page's load, both screens are set back to APODroid stills at once (Decision 12),
  overwriting the wallpaper just picked; "Live isn't on" appears; no toast.
- Log: `Live: home isn't live, Fill on HOME+LOCK`, `Page: wallpaper set: … on HOME, … on LOCK`.
- By design (CLAUDE.md, Decision 12); note it for the user.

**X6** Stored Fill, pick another wallpaper: nothing until home's next change time, which sets
APODroid's still again.

**X7** (SUSPECT) Stored Live, P1 (home live), pick APODroid's live wallpaper by hand for "Lock screen".
- Screen: a second engine on the lock screen draws **home's** copy (NASA) with home's pan, while the
  lock screen's record and status line say APOD. APODroid leaves it: no run replaces it until the lock
  screen's next due set (its next change time, or a Set).
- Log: `Live: engine 2 created`; nothing from the page's load.
- SUSPECT: Live.kt:178 (`lockEngine && !homeLive`: only while home isn't live), Store.kt:1054;
  `LiveTest.theEngineOnTheLockScreenIsLeftWhileHomeIsLive` asserts this. `unnotedLive` doesn't catch
  it either (Live.kt:233 needs `shared`).

**X8** Stored Live, home **not** live (after K4), daily on, pick APODroid's live wallpaper by hand for
"Lock screen", return.
- Screen: at the page's load the lock screen's engine is replaced by its APOD still; "Live isn't on"
  stays.
- Log: `Live: engine 2 created`, `Live: the engine is on the lock screen alone, Fill on LOCK`, `Page:
  wallpaper set: Of(source=APOD…) on LOCK`.

**X9** As X8 with the daily switch off: nothing replaces it (the page's update ends before `setDue`,
Store.kt:947) until Set (K17/K18) or the switch on.

---

## 4. SUSPECTs

| # | Leaf | What | Evidence |
|---|---|---|---|
| S1 | K13, L5 | With home live before (the equalising case), Back from the preview, or a Recents swipe followed by reopening the app, saves and sets the draft ("Wallpaper set"); the phone's dialog says "Back there: nothing changes." | MainActivity.kt:1149 `changed \|\| homeWasLive`; :556 onResume completes the old wait; :2500 (7299474 text) |
| S2 | N7, K7, K15 | Equal columns with the lock screen still are never offered the picker again ("once per split"); after S1's Back or a "Home screen" answer the user can't get both screens live from the app; Cancel drops the whole Set | core Live.kt:262 `!equalColumns(before)` |
| S3 | K4, K8 | "Lock screen" in the picker: Size Live saved, Fill set, "Wallpaper set", "Live isn't on": the outcome 7299474 removed for a back-out | MainActivity.kt:1149, 1103; Store.kt:1318 |
| S4 | X7 | APODroid's engine picked for the lock screen alone while home is live: kept, drawing home's picture with home's pan; the status line names the lock screen's own picture | core Live.kt:178, 233; Store.kt:1054; LiveTest |
| S5 | N11 | Size Fill with Where Home while the engine is shared: the Set leaves the engine on the lock screen; the next run then puts home's still on the lock screen (Where Home); its log says "the lock screen isn't live" | Store.kt:1054 (read before the sets), core Live.kt:95; Store.kt:1063–1067 |
| S6 | X4, K1 | The engine (and Set's picker preview) draws `live.jpg`, the last hand-over, possibly stale; never deleted; with Fill a hand-picked engine is never handed over | LiveWallpaper.kt:225–229; Store.kt:1322; core Live.kt:230 |
| S7 | N8 | Size Live with no home column: saved, " · Live" in the summary, no picker, no status line, no effect | core Live.kt:260; Store.kt:1298; MainActivity.kt:2170 |
| S8 | D8 | The picker not opened after OK (page not resumed: a rotation or Home right after OK): Set goes on, Size Live saved, Fill set, "Wallpaper set" | MainActivity.kt:1144–1146 |
| S9 | A7 | ⛶ follows the draft's Size, ✓ sets by the stored Size: draft Fill + stored Live drags, "Wallpaper set", nothing moves | MainActivity.kt:1801 vs Store.kt:439 |

---

## 5. Test list for the FP6 (35 cases, in order; each starts where the previous ended)

Start state **T0** (the phone now): More NASA pictures on; list: NASA ticked "Home screen", APOD ticked
"Lock screen", Earth unticked; Size Live; Live on home (NASA 13 Nov), lock APOD 15 Nov still; daily
switch on. Before T01 note "New picture at", "Change every", the list's order (≡ handles) and Advanced
switches; T35 must end with all of them unchanged. Run C1 throughout.

How to equalise (used several times): Setup › Pictures from › NASA's dropdown › Both, then untick APOD.
How to split back: NASA's dropdown › Home screen, then tick APOD (its dropdown shows "Lock screen").

| T | Leaf | Steps | Expect | Ends in |
|---|---|---|---|---|
| T01 | baseline | C2, C3 (note whether `run-as` works), C4, C5 | summary "Home: NASA · Lock: APOD · 06:00 · Live", status Home/Lock, no "Live isn't on"; C2 home APODroid, lock own still | T0 |
| T02 | N4 | Set (no change) | no dialog, Toast WS, `Live: the engine is on HOME, not noted live`, `…wallpaper set: …APOD… on LOCK; wallpaper shown live: …NASA… on HOME` | T0 |
| T03 | A5 | NASA page › ⛶ (frame fixed, no drag; wait a minute: it may move a pixel) › Whole picture › ✓; then ⛶ › Whole picture off › ✓ | each ✓ Toast WS, `Part: wallpaper shown live: … on HOME`; home whole, then back | T0 |
| T04 | A7 (S9) | Size › Fill (don't Set); NASA page › ⛶ › drag › ✓; then ⛶ › drag back to the middle › ✓; Size › Live | ⛶ drags; Toast WS; home doesn't move; `Part: wallpaper shown live`; Size highlight gone at the end | T0 (NASA's stored part ≈ middle) |
| T05 | D1 | equalise; Set | dialog "…Set wallpaper › Home screen and lock screen" (+ "Back there: nothing changes."); Cancel → no toast, `Set: the picker's dialog cancelled`, `Set: no pick, nothing saved`, Setup open, NASA/APOD rows tertiary | T0 + equal draft |
| T06 | D2, D3 | Set › system Back; Set › tap outside | each as T05 | same |
| T07 | D4, D5 | Set › rotate (dialog again) › rotate back › Home › launcher icon (dialog still) › Home › Recents › APODroid (dialog still) › Cancel | dialog survives all; as T05 at the end | same |
| T08 | K13 (S1) | Set › OK › preview › Back | by the code: Toast WS, Setup closes, summary "NASA · both screens · 06:00 · Live", lock NASA **still**; `Live: back from the picker, nothing changed, home live`, no `no pick` | M-EQ, home live, lock still |
| T09 | N7 (S2) | Set | no dialog, Toast WS, lock still | same |
| T10 | N6-like | split back; Set | no dialog, Toast WS, lock APOD still | T0 |
| T11 | K14 | equalise; Set › OK › Set wallpaper › Home screen and lock screen | Toast WS, both live NASA, one status line, `…home live, shared`, `shown live: … on HOME+LOCK`; C2 lock block empty | P2, M-EQ |
| T12 | N5 | Set | no dialog, Toast WS, `shown live: … on HOME+LOCK` | P2 |
| T13 | A1, A2 | daily switch off (C5, C2: engine stays); on | off: lines gone; on: Toast WS, `Switch on: wallpaper shown live: … on HOME+LOCK` | P2 |
| T14 | A4 | pull to refresh | no toast, nothing set | P2 |
| T15 | N6 | split back; Set | no dialog, Toast WS, lock APOD still, `Live: engine flags 1` | T0 |
| T16 | X7 (S4) | Android's wallpaper settings › Live wallpapers › APODroid › Set wallpaper › Lock screen; return to APODroid; lock the phone and look | lock shows NASA panning (second engine) while status says Lock: APOD; no APODroid log beyond `Live: engine 2 created`. Then Set → lock APOD still again | T0 |
| T17 | N1 | Size › Fill; Set | Toast WS, both stills, summary without " · Live", `Live: engine home destroyed` | P0 Fill |
| T18 | X4 (S6) | Android's settings › APODroid live › Home screen (note what the preview draws); return; then Set | no hand-over, no status line; engine draws `live.jpg` (`Live: drew Of(source=NASA…)`); Set (Fill) → home still again | P0 Fill |
| T19 | D1 | Size › Live; Set; Cancel | dialog "…› Home screen"; as T05 | P0, draft Live |
| T20 | K1, K2 | Set › OK › Set wallpaper › (sheet) Back › Back from preview | no toast, `…nothing changed, home not live`, `Set: no pick, nothing saved`; note the preview's picture | same |
| T21 | L1, L2, L7 | Set › OK › Home › launcher icon (picker on top?) › Home › Recents › APODroid › Back | note whether the picker comes back; ends as T20; no `back from the picker` line while away | same |
| T22 | K4 (S3) | Set › OK › Set wallpaper › Lock screen | ~3 s, Toast WS, both stills, "Live isn't on", summary " · Live", `the engine is on the lock screen alone, Fill on LOCK` | P0, stored Live, liveMissing |
| T23 | X8 | Android's settings › APODroid live › Lock screen; return | page load: `Live: the engine is on the lock screen alone, Fill on LOCK`, `Page: wallpaper set: …APOD… on LOCK`; "Live isn't on" stays | same |
| T24 | A3 | daily switch off, on | Toast WS, both stills, no dialog; "Live isn't on" stays | same |
| T25 | L3, K3 | Set › dialog "› Home screen" › OK › rotate › Set wallpaper › Home screen; rotate back | Toast WS, Setup closes, home live NASA, lock APOD still, "Live isn't on" gone | T0 |
| T26 | X5 | Android's settings › any built-in still › Set wallpaper › Home screen; return | at once both APODroid stills, `Live: home isn't live, Fill on HOME+LOCK`, "Live isn't on" | P0, stored Live |
| T27 | K5 | Set › OK › Set wallpaper › Home screen and lock screen | Toast WS, home live, lock APOD still, `…home live, shared`, `Live: engine flags 1` | T0 |
| T28 | L5 (S1) | equalise; Set › OK › preview › Recents › swipe APODroid away; C4; wait 10 s; open APODroid | process alive: Toast WS, equal columns saved (summary "NASA · both screens…"); killed: nothing saved, T0 | M-EQ or T0 |
| T29 | split | only if T28 saved: split back; Set | as T10 | T0 |
| T30 | D6 | equalise; Set › dialog › Recents › swipe; C4; reopen | alive: `the picker's dialog cancelled` at the swipe; reopened page shows the stored setup, Setup closed | T0 |
| T31 | N8 (S7) | More NASA pictures off; Where › Lock screen; Size Live; Set | no dialog, Toast WS, lock APOD still, home keeps the engine (NASA, old copy); summary "APOD · lock screen · 06:00 · Live"; no "Live isn't on" | A-L |
| T32 | N9 | Where › Home screen; Set | no dialog, Toast WS, `shown live: …APOD… on HOME`, lock unchanged | A-H, home live |
| T33 | K14 (A-B) | Where › Both; Set › dialog "› Home screen and lock screen" › OK › Set wallpaper › Home screen and lock screen | Toast WS, both live APOD, `shown live: … on HOME+LOCK` | A-B, P2 |
| T34 | N11 (S5) | Where › Home screen; Size › Fill; Set; C2; lock the phone, wake it, look at the lock screen; reopen APODroid | Set: `wallpaper set: … on HOME` only; C2: does the lock screen keep APODroid's engine? Then at the next screen-on or app open `Live: the lock screen isn't live, Fill on HOME, the lock screen takes home's`, both APOD still | A-H Fill, P0 |
| T35 | restore | More NASA pictures on (the list returns: NASA Home screen, APOD Lock screen); Size › Live; Set › dialog "› Home screen" › OK › Set wallpaper › Home screen | Toast WS; C2 home APODroid, lock own still; C5 summary "Home: NASA · Lock: APOD · 06:00 · Live", status Home/Lock, no "Live isn't on"; compare the notes from T01 (New picture at, Change every, order, Advanced) | **T0** |

Not in the list (lower value or hard to trigger): D8 (S8, a race), K11/K12 (A-H answers), K16, K17/K18
and X9 (need the daily switch off with a lock-only engine), X1–X3 (hand-picked Live while stored Live),
A10 (needs an owed check).
