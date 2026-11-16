# APODroid's page outside Setup: decision tree for a phone test

> Read at 7299474 (15 Nov 2026), checked against 3a4db15 (16 Nov: 0.8.0); line numbers are of 3a4db15.
> The SUSPECTs below were fixed or accepted since (README.md): their quoted code is 7299474's, the fixed
> outcome is in the leaf or the test list. Newer changes: README, Keeping it current.

Read from commit 7299474, line numbers of 3a4db15. Files: `app/src/main/kotlin/io/github/buerlino/apodroid/`
`MainActivity.kt` (MA), `Store.kt` (ST), `DailyJob.kt` (DJ), `KeepRunning.kt` (KR),
`LiveWallpaper.kt` (LW). Spec: CLAUDE.md, "The app" and "Background update".

Status per leaf: **NOW** (testable today on the FP6), **NOT NOW** (needs a video day, a failing
site, a force stop, an empty store or an irreversible tap), **SUSPECT** (the code looks wrong or
surprising for a user; reason and evidence given).

## 0. The phone as found (S0) and the toolkit

Expected S0 (from the brief): FP6, Android 16, release build of 7299474, portrait. Daily switch
"Change automatically" on; "More NASA pictures" on; home column NASA, lock column APOD; Size Live
(APODroid's engine on home, the lock screen a still APOD). Sunday 15 Nov 2026: APOD's post an image
of 15 Nov, NASA's newest 13 Nov, Earth's newest probably Fri 13 Nov (it posts weekdays only).

Record before anything (case P1 below):
- Advanced settings: "Save every picture" on/off, "Largest original" value, "Keep running" on/off.
- Whether a pause hint, problem box or detector box shows; the status lines' exact text.
- `adb shell dumpsys wallpaper > s0-wallpaper.txt` (home: `mWallpaperComponent` must name
  `io.github.buerlino.apodroid/.LiveWallpaper`; note both wallpaper ids and the lock screen's state).
- `adb shell content query --uri content://media/external/images/media --projection _id:_display_name:relative_path --where "relative_path='Pictures/APODroid/'"`
  (the gallery's APODroid files; every new row a test adds must be deleted at the end).
- `adb shell settings get global airplane_mode_on`, `adb shell settings get system accelerometer_rotation`,
  `adb shell settings get system user_rotation` (restore these exactly).
- `adb shell dumpsys activity services io.github.buerlino.apodroid | grep -E "ServiceRecord|isForeground"` (Keep running's service).
- `adb shell dumpsys alarm | grep -A4 io.github.buerlino.apodroid` and
  `adb shell dumpsys jobscheduler io.github.buerlino.apodroid | grep -E "JOB #|Pending|Ready"`.

Toolkit:
- **Logs:** don't `logcat -c` (the user's week of logs). Before each case
  `T=$(adb shell date +'%m-%d %H:%M:%S.000')`; after it `adb logcat -d -v time -T "$T" -s APODroid`.
- **Every page resume** (open, back from the browser, App info, Gallery) logs
  `Daily job: alarms for <instant>` (MA:624 → DJ:189) while the daily switch is on: the next check,
  normally tomorrow's 06:00 + the phone's 0–10 min offset (`2026-11-16T05:0x:xxZ` in CET), hourly with
  "As soon as it's out" on. A resume that sets nothing logs nothing else (no `Page:` line).
- **UI tree:** `adb exec-out uiautomator dump /dev/tty`. The title is a content-desc
  (`Picture from: NASA Image of the Day`), not text; ⛶ is `Choose the wallpaper's part`, ✓ `Done`,
  ★ `Save` / `Saved` / `Saving`, the toggle in crop mode `Whole picture`. "more" is hidden from
  accessibility (MA:1931): check it on a screenshot. The explanation node's text is always the whole
  text; its bounds' height tells two lines from all.
- **Taps:** `adb shell input tap X Y` from the dump's bounds; swipe `input swipe X1 Y X2 Y 300`;
  long press `input swipe X Y X Y 900`; pull to refresh `input swipe 540 500 540 1600 600` with the
  page scrolled to its top.
- **Screens:** `adb exec-out screencap -p > x.png`. Toasts don't show in dumps: watch the phone or
  screencap within 2 s. Home screen: `input keyevent 3`, screencap, back with
  `am start -n io.github.buerlino.apodroid/.MainActivity`. The lock screen only if the tester can
  unlock (else: its wallpaper id in `dumpsys wallpaper`).
- **Airplane mode** (network failures): needs adb over **USB** (wireless adb drops).
  `adb shell cmd connectivity airplane-mode enable`; check `adb shell ping -c1 -W2 science.nasa.gov`
  fails (if Wi-Fi stays on, Android remembered "Wi-Fi in airplane mode": also `adb shell svc wifi disable`).
  Undo: `cmd connectivity airplane-mode disable` (and `svc wifi enable` if used); the ping must work.
  Keep it short: a job run meanwhile fails and is retried 15 min later (self-healing).
- **Rotation:** `adb shell settings put system accelerometer_rotation 0; adb shell settings put system user_rotation 1`
  (landscape), `user_rotation 0` (portrait); restore both values from S0 at the end.
- **Never** tap Quick Settings' "Active apps › Stop" (Android's Task Manager stops the app like a
  force stop: alarms gone, Live dropped on both screens), never "Hide" in the pause hint's dialog nor
  "Don't show again" on a box (only clearing storage brings them back).

## 1. The decision tree

### T. The title above the picture (MA:1648-1675)

Precondition for all: S0, the page open, three sources offered (the switch "More NASA pictures" on),
so the title is "<name> ▾" and a dropdown.

- **T1 Open fresh, opening page.** Close the page with Back, start it again. NOW.
  Expected: it opens on **NASA** (home's picture's source; `shown`, MA:725-729, with the stored switch
  on home first, then lock). Title "NASA ▾"; chip "On home screen"; source line "NASA Image of the Day
  · 13 November 2026". Log: `Daily job: alarms for …` only. Check: dump, content-desc
  `Picture from: NASA Image of the Day`.
- **T2 Tap the title.** NOW. A menu "APOD", "NASA", "Earth" in that order (never the list's order),
  "✓" before NASA. Check: dump shows three menu items.
  - **T2a Choose Earth.** The pager slides (through NASA's neighbour) to Earth; title "Earth ▾"; no
    chip; source line "Earth Observatory · <date>"; the boxes and caption are Earth's. No log.
  - **T2b Choose the shown one (NASA).** Menu closes, nothing moves (MA:1668).
  - **T2c Tap outside / Back.** Menu closes, nothing else (Back doesn't leave the page).
  - **T2d Open the menu, rotate.** The menu closes (plain `remember`, MA:1651); the shown source stays.
- **T3 One source offered** (APOD mode, no video day): the title "APOD" without ▾, not tappable.
  NOT NOW here (needs the draft's switch off: Setup's agent).

### W. The swipe (pager, MA:1604-1642)

Pages are always APOD, NASA, Earth (index 0, 1, 2).
- **W1 Swipe left on NASA** (finger right to left). NOW. Title and picture slide to Earth with the
  finger; on release Earth settles and becomes the shown source (`chosenSource`). Boxes and caption
  change only once it settles (they're outside the pager).
- **W2 Swipe left on Earth.** NOW. Stops at the end (no wrap).
- **W3 Swipe right to APOD; right again.** NOW. APOD shows, chip "On lock screen", credit line under
  the title; at APOD a further swipe right does nothing.
- **W4 Half a swipe, release.** NOW. Snaps back to the page it started on; the caption never changed.
- **W5 Chosen source survives a rotation** (O-group) and the system killing the app; a Back + restart
  forgets it (T1 again). NOW (rotation); `am kill` while in the background is optional.
- **W6 In crop mode the pager doesn't move** (`userScrollEnabled = !cropping`, MA:1630): see K10.

### P. The picture tap (MA:1733)

- **P1 Tap the picture** (not in crop mode). NOW. The browser opens the post's page: APOD →
  `science.nasa.gov/…apod…`, NASA → `www.nasa.gov/image-detail/…`, Earth → its science.nasa.gov page.
  Back returns to the page on the same source. Log on return: `Daily job: alarms for …`. Check:
  `adb shell dumpsys activity activities | grep -E "topResumedActivity"` names the browser.
- **P2 No browser installed:** toast "No browser". NOT NOW (needs a phone without one).
- **P3 Video day:** a black 3:2 tile with ▶ (`Play video on the web`), tap opens the page; no ⛶, ★,
  chip. NOT NOW (APOD is an image today).
  - **P3a NASA's mp4** (VS1; `apod.videoUrl` set): the first open shows black, then the still
    (a frame ~3 s in, cropped to 3:2, 40% black over it, ▶ over that). Log once:
    `Video still: <ms> ms, <w>×<h>`; `files/video-still.jpg` and the prefs `videoStill.date`,
    `videoStill.url`, `videoStillTried` written. Screen readers unchanged. A second open, a
    rotation, a swipe: the file, no log line. NOT NOW (fake: SKILL.md's "Fake an old day" plus the
    `videoUrl` pref; no `run -f` after it).
  - **P3b A rotation during the first try:** the try goes on (process-wide `runs`); the rotated
    page shows the still when it's made (the prefs listener). NOT NOW.
  - **P3c The try fails** (airplane mode, a 404, no frame): black, `Video still: couldn't make one
    of <url>: …`, `videoStillTried` noted; no new try on later opens until the next post. NOT NOW.
  - **P3d YouTube day or a post stored before 0.8.0** (no `videoUrl`): black, no try, no log. NOT
    NOW.
  - **P3e The next post** (an image day or another video): `refresh` deletes the still and its
    notes; a new video tries once. NOT NOW.
  - **P3f The page stops** (Home, Back): the still is recycled with the pictures (O12) and decoded
    from the file at the next open. NOT NOW.
- **P4 Tap in crop mode:** nothing opens (the modifier is the frame's, MA:1730-1732). NOW (K-group).

### C. The caption (MA:1845-1937)

With three sources: a source line "<full name> · d MMMM yyyy" (" · Video" on a video day), then the
title, then (APOD only) the credit, then the explanation (2 lines, "more" when cut).
- **C1 Read it on each page.** NOW. APOD: "Astronomy Picture of the Day · 15 November 2026", the title
  without the "APOD: …" prefix, a grey one-line credit (ellipsis when long), the explanation's first two
  lines. NASA and Earth: no credit line.
- **C2 Tap "more" or the explanation** (APOD, cut). NOW. The whole explanation and the whole credit
  show; "more" goes. Tap again: back to two lines. Check: the explanation node's height in the dump
  grows and shrinks; "more" only on a screenshot.
- **C3 Tap the title block** (source line, title, credit) while expandable. NOW. Same toggle as C2.
- **C4 An explanation that fits two lines** (likely Earth's one sentence only on a wide pane; on the
  FP6's portrait it may well take three lines and show "more"). NOW: note which. If it fits: no "more",
  a tap does nothing (a ripple only).
- **C5 Swipe away and back after expanding.** NOW. Still expanded (since C8's fix one page state,
  `expandedPost` = "SOURCE:date", MA:1855; 7299474 collapsed it). Expanding another page's
  explanation collapses the first.
- **C6 Long press the title block.** NOW. Copies the post's page link. Android 16 shows its own
  clipboard overlay (bottom left, a preview of the link); the app shows no toast (only below 13,
  MA:1560). The click doesn't toggle. Check: the overlay on a screenshot within 2 s
  (`adb shell cmd clipboard get-primary-clip` if that build has it). No log.
- **C7 Long press the explanation.** NOW. Copies the explanation's text, same overlay. A long press on
  the credit copies the **link** (the credit is inside the title block).
- **C8 Rotate with the explanation open.** NOW. **SUSPECT:** it comes back collapsed. On the FP6 the
  rotation crosses 600 dp, so portrait and landscape compose different branches of `Page()`
  (MA:1572 one column vs MA:1580-1591 two panes) and `rememberSaveable`'s position key differs: the
  saved `expanded` (MA:1855) isn't found. The same loses an open ⓘ dialog (MA:2310, 2366, 2715) and
  the pause hint's "Hide this hint?" dialog (MA:2859) on rotation. **Fixed:** `openInfo`,
  `expandedPost`, `pauseConfirm` in the saved state (MA:583-587): expected open / the dialog still
  there after each rotation, both ways. The user may expect what Setup's and Advanced's open state do (they survive: saved in
  `onSaveInstanceState`, MA:583-587).

### H. The chip (MA:908-918, 1707, 1741-1752)

From the stored records (`Store.screenState`); with three sources only; hidden in crop mode.
- **H1 NASA page:** "On home screen". **APOD page:** "On lock screen". **Earth page:** no chip. NOW.
- **H2 Both screens on the same post:** "On wallpaper". NOT NOW (needs equal columns).
- **H3 In crop mode** the chip's place holds the "Whole picture" toggle. NOW (K-group). The top
  right holds "Sideways" on a landscape picture (K16), so the chip's corner stays the top left.

### K. ⛶ crop mode (MA:1450-1547, 1709-1818, 2033-2136; ST:413-465)

⛶ bottom left of the picture (not on video days); disabled (dimmed) while Set or the daily switch's
run goes (MA:1802). Which kind of frame: **Live's** when the **stored** Size is Live, Live is on home
(no "Live isn't on" line) and the shown source is in the stored home column (MA:2039; S9's fix, was
the draft's): today NASA. **Draggable Fill** otherwise: APOD (lock) and
Earth (on no screen).

Fill sources (APOD, Earth):
- **K1 Tap ⛶ on Earth.** NOW. The frame (the screen's portrait shape, white border, the rest dimmed)
  at the stored part's dragged position, else where the stored Size puts it (Auto: the subject,
  SA3b, K27; Centre: the middle); ⛶ becomes ✓ (`Done`); the chip's place shows "Whole picture"; the
  pager is locked. No log.
  - **K1a Drag sideways.** The frame follows along the long side and stops at the ends; vertical drags
    still scroll the page.
  - **K1b Back.** Crop mode closes, the dragged part dropped; ⛶ again opens at the stored place
    (none dragged: Size's, K27). No log, nothing set.
  - **K1c ✓ after a drag, Earth on no screen.** Crop mode closes; the part saved at once
    (`savePart`), `Store.setPart` finds no screen showing Earth: **no toast and no log line at all**
    (MA:1522, 1527-1530). ⛶ again opens where it was left. Doc-intended ("only saved, no toast"), but
    the user gets no sign that ✓ did anything. Leaves a stored part for Earth's 13 Nov post (harmless:
    Earth is on no screen; a new Earth post drops it).
  - **K1d ✓ without a change.** Crop mode closes, nothing saved, no toast (MA:1501). Without a drag
    the frame where it opened counts as no change (SA3b, K28).
- **K2 ⛶ on APOD (lock, still), "Whole picture" on, ✓.** NOW, reversible by K3.
  Toggle: text "✓ Whole picture" in the theme's tertiary colour (differs from stored), the picture's
  place shows a spinner, then the screen-shaped preview on black or blurred, darkened if bright and
  "Darken bright pictures" is on. ✓: crop mode closes, toast **"Wallpaper set"**, the "Lock:" status
  line's time moves to now. Logs: `Part: wallpaper set: Of(source=APOD, date=2026-11-15, url=…) on LOCK`
  (+ `, darker ×0.xx (grey nnn)` when darkened), then `Part: APOD 2026-11-15 set on LOCK`.
  Check: `dumpsys wallpaper`: the lock screen's id changed; home's component still `.LiveWallpaper` and
  its id unchanged. Lock screen (if unlockable): the whole picture.
- **K3 ⛶ on APOD again, "Whole picture" off, ✓.** NOW. Opens with "✓ Whole picture" (stored); off →
  tertiary again; ✓ → "Wallpaper set", logs as K2 without the whole picture; the lock screen back to
  Fill at the stored place (the middle unless the user had dragged one). Restores S0's look exactly.
- **K4 ⛶ on APOD, drag, ✓.** NOW but **not reversible by hand** (the middle can't be hit exactly):
  sets the lock screen at once ("Wallpaper set", `Part: wallpaper set: … on LOCK`). The part lasts until
  APOD's next post (~06:05 on 16 Nov). Only with the user's OK; else covered by K2/K3 (same set path).
- **K5 ✓ whose set fails:** toast "Couldn't set the wallpaper", `Part: set failed: …`; ✓ again with the
  same part retries (`failedPart`). NOT NOW (needs a failing `WallpaperManager`).

Live source (NASA on home):
- **K6 Tap ⛶ on NASA.** NOW. The frame stands where the engine draws it now (`livePosition`, MA:2043):
  no drag (a drag does nothing, the page may scroll vertically); it moves on each minute. Compare with
  the engine's last `Live: drew <picture> p=0.NN …` line (LW:381) and a home-screen screencap: the
  frame's left edge ≈ p × (picture width − frame width), from 06:00 (p=0) to 24:00 (p=1).
  - **K6a ✓ without a change.** Crop mode closes, no toast, no log (position = stored, MA:1501).
  - **K6b Back.** Closes, nothing.
- **K7 ⛶ on NASA, "Whole picture" on, ✓.** NOW, reversible by K8. Toast "Wallpaper set"; logs
  `Part: wallpaper shown live: Of(source=NASA, date=2026-11-13, url=…) on HOME[, darker ×…]` and
  `Part: NASA 2026-11-13 set on HOME`; then the engine `Live: decoded …` and `Live: drew … p=0.50 …`
  (whole: centred, still). No `WallpaperManager` set: `dumpsys wallpaper` home id and component
  unchanged, the lock screen untouched. Home screen: the whole picture, not moving. "Home:" status
  line time moves.
- **K8 ⛶ on NASA, "Whole picture" off, ✓.** NOW. "Wallpaper set"; the same two `Part:` lines; the
  engine decodes again and draws `p=0.NN` (panning again). Back to S0's look.
- **K9 Live frame while Size isn't Live in the draft** (Setup's agent changes Size): ⛶ on NASA becomes
  a draggable Fill frame. Out of scope here.

Crop mode and the rest of the page:
- **K10 Swipe in crop mode.** NOW. The pager doesn't move (on APOD/Earth a horizontal drag moves the
  frame instead).
- **K11 Title dropdown in crop mode, choose another source.** NOW. Crop mode closes without keeping
  (`showSource`, MA:995), the new source shows.
- **K12 Rotate in crop mode.** NOW. Crop mode closes, a dragged part not kept with ✓ is dropped
  (`cropping` isn't saved); shown source, Setup's and Advanced's state stay.
- **K13 Home key in crop mode, then back.** NOW. Crop mode is still open after the picture is decoded
  again (O12: an empty box of the picture's height meanwhile), the dragged position kept.
- **K14 ⛶ while Set or the daily switch's run goes:** dimmed, a tap does nothing. NOW only briefly
  (during D2's run, ~1 s): optional.
- **K15 ★ in crop mode:** still works (MA:1819). NOW, optional.

Sideways (SW2, 0.8.0; `cropSideways`, the pill in `PicturePage`, `pictureFrame`, `Store.setFile`'s
sideways branch). Needs a landscape picture on the shown page (today's, or 9 Nov's APOD faked into
`files/` with the skill's recipe) and a portrait or square one for K16a. Sideways turns the picture
90° clockwise: its top at the screen's right; the band is position 0 at the picture's bottom.
- **K16 ⛶ on a landscape picture.** The pill "Sideways" at the picture's top right (the chip's dark
  pill), white while it equals what the stored part puts on (SW3: the noted value, else the stored
  "Wide pictures sideways" rule, K24); "Whole picture" stays at the top left. Screen
  readers: "Sideways", a switch, "Off". No log.
  - **K16a ⛶ on a portrait or square picture:** no "Sideways" pill (`sidewaysHelps`: wider than high,
    by the file's upright size, `uprightSize`, SW3).
  - **K16b Tap "Sideways".** "✓ Sideways" in tertiary (", not set yet"); the frame becomes a band of the
    screen's shape turned (a horizontal band across the full width on a normal wide picture, its
    height = the picture's width × screen width ÷ screen height: 3630×2408 on 1080×2408 gives 1628
    of 2408 rows), at the stored
    position (the middle by default). The page's picture itself stays upright.
  - **K16c Drag the band (Fill).** Up and down (a drag down lowers it, toward position 0, the
    picture's bottom); horizontal drags do nothing (on a panorama whose turned picture is taller than
    the screen: a strip moving left to right). Vertical drags outside the band also move it (the
    whole picture is the drag area, as K1a's).
- **K17 Sideways on, ✓, Size Centre or Auto.** Toast "Wallpaper set". Log: `Part: wallpaper set: Of(source=…)
  on <screens>, sideways[, darker ×0.xx (grey nnn)]`, then `Part: <source> <date> set on <screens>`.
  `files/debug-set.png` (debug build) is portrait, the screen's size, the picture turned clockwise
  (the original's top at the right, its bottom band at position 0 at the left edge of the turned
  picture). Both screens on the post: one set with both flags (`dumpsys wallpaper`: one shared
  wallpaper, no lock wallpaper of its own); the lock screen alone (APOD mode, Where Lock): only the
  lock id changes. The prefs: `<source>.sideways=true` beside `cropPosition` and `whole`.
  - **K17a Sideways + Whole picture on, ✓.** The preview (spinner, then the screen-shaped picture)
    drawn turned on its background (black, or the turned picture's middle blurred); the set the same
    (`debug-whole.png` and `debug-set.png` turned), ", sideways" in the log.
  - **K17b A bright landscape picture sideways** ("Darken bright pictures" on): `, sideways, darker
    ×0.xx (grey nnn)`; `debug-set.png` turned and darker.
- **K18 ⛶ again, "✓ Sideways" (stored, white), tap it off, ✓.** "Wallpaper set"; the log without
  ", sideways"; set by `setStream` again (an upright, not darkened Fill: `debug-set.png` deleted).
  `<source>.sideways=false`.
- **K19 ✓ with only Sideways changed** counts as a change (sets); ✓ with it as stored and the band
  not moved: nothing (K1d).
- **K20 Back in crop mode with Sideways changed:** dropped; ⛶ again opens with the stored one.
  **Rotation** (K12): the same, crop mode closed.
- **K21 Sideways on, then Set as wallpaper with crop mode open:** the draft keeps it (`keepPart`) and
  Set stores it with the setup (`<source>.sideways=true`), its set turned (`Set: wallpaper set: … ,
  sideways`).
- **K22 With Live** (the stored Size Live, Live on home, the source in home's column; K6): the band at
  the clock's position (`livePosition`), no drag, moved by the minute tick; ✓ hands it over:
  `Part: wallpaper shown live: Of(…) on HOME, sideways[, darker …]`, `live.sideways=true`; the
  engine's `Live: decoded W×H` portrait-shaped (W < H: the turned picture at the screen's height),
  `Live: drew … p=0.NN`; the home screen pans across the turned picture (a 3:2 picture about a third
  of its turned width over the day). Details: live.md, ⛶ with Live.
- **K23 A new post of that source** (a faked date): the part drops (keyed by `cropDate`/`cropUrl`),
  the new picture goes on upright; ⛶ opens with "Sideways" off (with "Wide pictures sideways" on
  and a 3:2 or wider new picture: sideways, K24).

With "Wide pictures sideways" (SW3, 0.8.0; setup.md X; `turnsSideways`, `cropSidewaysTouched`): the
pill opens on the value the set uses, the part's noted one, else, while the **stored** switch is on,
`sidewaysByDefault` (3:2 or wider). The draft's switch doesn't change ⛶ until Set.
- **K24 The switch stored on, ⛶ on a 3:2 or wider picture without a noted part:** "✓ Sideways"
  white (it's what's on), the band. On a 1.2:1 picture: "Sideways" off, white.
  - **K24a ✓ untouched** (the band dragged or not): the part keeps no `<source>.sideways` key (null,
    the switch's default); a moved band sets as K1d with ", sideways".
  - **K24b Tap it off, ✓:** "Wallpaper set", upright (`setStream`, no ", sideways"),
    `<source>.sideways=false`: this picture only; the next post (K23) sideways again by the rule.
  - **K24c Tap it off and on again, ✓:** nothing set (the value used unchanged), but
    `<source>.sideways=true` noted (a touched pill notes its value), so the switch off later keeps
    this picture sideways.
- **K25 The switch stored off, a part ✓'d on:** stays sideways (setup.md X4). The draft's switch
  on, not Set: ⛶ still opens as stored (night default: ✓ sets by the stored setup).
- **K26 A part's position with the switch:** its number stays when the switch turns the picture
  sideways, so the band stands at that position along the other axis (night default).
With Size Auto (SA3b, 0.8.0; `startCrop`, `frameOf`, `cropMoved`, `:core`'s `framePosition`,
`notedPosition`, `Part.kt`): ⛶ opens where the screen has the frame, by the **stored** Size, the
sideways value used and the stored "Centre when unsure" (setup.md J6; Advanced's A7 before UI1); `Part.position` stays null until
the frame is dragged. Set up: a picture whose noted subject moves the frame 0.2 or more
(`<source>.subjectPosition` in the prefs, the `Subject:` line), the stored Size Auto.
- **K27 ⛶ on the subject picture, no stored part.** The frame at the subject (the `Subject:` line's
  `p=`), not the middle; on a picture whose subject moves less than 0.2, the middle (A7 off: at it).
  Stored Size Centre: the middle. Size Live with the source in home's column: Live's frame (K6).
- **K28 ✓ without a drag.** Crop mode closes, nothing saved (no `cropDate` change), no toast, no log:
  the frame stood where the screen has it ("no change").
- **K29 Drag, ✓.** "Wallpaper set", `Part: wallpaper set: …` without ", auto …" (the part's
  position wins over Auto); `<source>.cropPosition` noted. ⛶ again opens at the dragged place.
  - **K29a A drag that ends exactly where Auto had it:** saved (`cropPosition` noted), no set, no
    toast (as K24c).
- **K30 "Whole picture" on, ✓ without a drag; then Setup › Size Centre, Set.** The part keeps no
  `<source>.cropPosition` (null: undragged): the whole picture goes on. "Whole picture" off and ✓ again
  (still no drag): Fill at the middle with Centre, at the subject with Auto (`, auto 0.xx` in the
  set's line). A part ✓'d by 0.7.0 keeps its number (no migration).
- **K31 The pill on an undragged frame** (a landscape picture): the band jumps to the sideways
  subject's position (`subjectSideways`, the middle without one or with Centre); tap it back: the
  upright one. After a drag (K29) the pill keeps the number (K26).
- **K32 Rotate in crop mode:** crop mode closes (K12); an undragged part stays undragged. A Set with
  crop mode open (K21) stores the undragged part with the setup: `cropPosition` absent.
- **K33 A dragged part, then Size Centre ↔ Auto and Set:** the part's position stays (the drag wins);
  an undragged one (K30) follows Size.
- **Checked** (SW2T, rosie, 2026-11-15, the Android 14 emulator `emulator-5564`, 1080×2400, the debug build of dev b5857b9; 9 Nov's APOD at `?h=2400`, 3618×2400, and 31 Jan 2026's bright one faked into `files/apod.jpg`, today's 1920×2400 for the portrait case; evidence in `.coord/runs/126-sw2t/tablet/`): K16, K16a–c, K17, K17a, K17b, K18, K20,
  K21, K22, K23 PASS (K19's no-change half not run). On AOSP 14 `dumpsys wallpaper` always lists a
  "Lock wallpaper state" (id 1, `mWhich=1`, an empty crop, there since boot): a set with both flags
  shows as the system wallpaper's `mWhich=3`, `mSystemWasBoth=true`. With Live the pills overlap the
  band's top edge when it stands near p=1 (cosmetic, for the user's look).

### S. ★ save to the gallery (MA:1381-1418, 1819-1836; ST:1864-1961)

☆ bottom right (not on video days); saves the **original** (`originalUrl`: APOD/Earth `content/dam`,
NASA the feed's enclosure, up to 97 MB, no size limit for ★) to `Pictures/APODroid/`
`APOD_|NASA_|EO_<date>_<Title_Words>.<jpg|png>`. Use **Earth** first (originals 0.2–2.9 MB). Before ★
on NASA check its size on the computer:
`curl -s https://www.nasa.gov/feeds/iotd-feed/ | grep -o '<enclosure[^>]*>' | head -1`.
- **S1 Tap ☆ on Earth.** NOW. ☆ dims ("Saving") during the download; toast **"Saved to
  Pictures/APODroid"**; ★ filled ("Saved"). No log line on success. Check: the content query lists a
  new `EO_2026-11-13_….jpg` (note its `_id` for deletion).
  - **S1a Quick double tap on ☆** (instead of S1's single tap). One save, one toast, one row (the second
    tap returns silently while `saving`, MA:1382).
  - **S1b Tap ★ again.** Toast "Already saved", nothing downloaded, no new row.
- **S2 Delete the saved file in the gallery** (Files/Gallery app, or
  `adb shell content delete --uri content://media/external/images/media/<_id>`; or
  `adb shell rm "/sdcard/Pictures/APODroid/<name>"`), then come back to the page. NOW. On resume ★
  turns ☆ (`isSaved` queries the URI, ST:1865-1874; re-read at the load's end, MA:764).
  - **S2a SUSPECT, deleted while the page stays resumed** (adb delete with the page in front, or split
    screen): ★ stays filled and a tap says "Already saved" though there is no file. The page re-reads
    `saved` only on load, show, ✓ and the prefs watcher (MA:801-804, 1383; ST:877-881 has no MediaStore
    key). Check: delete with adb while the page is in front, tap ★: "Already saved"; Home + reopen: ☆.
- **S3 ☆ in airplane mode.** NOW (airplane mode). The original fails, the stored rendition is copied:
  toast **"Saved a smaller copy: the original didn't load"**, ★ filled. Log (W):
  `Original failed, saving the stored picture: java.net.UnknownHostException: …`. A new row (smaller
  file); delete it afterwards. Note: while that copy exists, ★ never offers the original again.
- **S4 Rotate during the download, then tap ☆ in the new page.** NOW if the download outlasts a
  rotation (NASA's or APOD's original of several MB; check size first). The new page's ☆ is **not**
  dimmed (its `saving` is fresh); a tap: toast "Still saving" (`Saved.BUSY`, process-wide flag,
  ST:1887). When the download ends the new page's ★ fills (watcher on `savedUri`).
  - **S4a SUSPECT: no result toast after a rotation or Back during the save.** The save's end runs in
    the page's `lifecycleScope` (MA:1393), cancelled by the rotation: "Saved to Pictures/APODroid",
    "Saved a smaller copy…" and "Couldn't save the picture" never show, although the file is written.
    ✓ and Set use the process-wide `runs` for exactly this (MA:1517, 1165; "a rotation keeps its
    end"). **Fixed:** the save runs in `runs` (MA:1393): the result toast shows after a rotation (and
    after Back, unless Android suppresses a background toast); ★ fills in the new page.
- **S5 "Not saved: a new picture came in"** (a newer post stored during the download) and **"Couldn't
  save the picture"** (MediaStore failure). NOT NOW.
- **S6 Saving after "Clear storage" adds "… (1).jpg".** NOT NOW (clearing data is forbidden).

### R. Pull to refresh (MA:920-923, 739-770, 1571-1593; ST:1127-1153)

Portrait: the whole page (picture and settings) is in the pull box; landscape: the left pane only.
A pull always fetches the three offered sources (APOD too, `force`), downloads only a new picture, and
sets a newer picture of each screen's source at once while the daily switch is on.
- **R1 Pull on NASA's page, network on, nothing new (Sunday).** NOW. The spinner shows and goes;
  nothing changes; no toast; **no APODroid log line** (a pull that sets nothing logs nothing).
- **R2 Pull in airplane mode.** NOW. Spinner, then an orange box under the picture:
  **"NASA Image of the Day: no connection."** (named: two sources ticked, MA:856; no "since" as the last
  good check is recent). The stored pictures stay. Log (W): `Update failed: <UnknownHostException …>`.
  All three sources now have a stored problem.
  - **R2a Swipe to APOD:** "Astronomy Picture of the Day: no connection."; **Earth:** "Earth
    Observatory: no connection." (each page shows the shown source's problem first, MA:835-840).
- **R3 Network back, return to the page or pull.** NOW. The boxes go (APOD's check succeeds without a
  fetch, its post is today's; NASA's and Earth's fetch again). No log unless something is set.
- **R4 Pull while the page's own load runs** (pull right after opening): the spinner stays until the
  forced run after it ends (MA:767). NOW, hard to time; optional.
- **R5 Pull in landscape on the right pane:** nothing (the settings pane has no pull). NOW (O-group).
- **R6 Pull with the daily switch off:** fetches, sets nothing. NOW, optional (during D1).

### B. The boxes below the picture (MA:1861-1870, 1980-2004, 2801-2916)

Order: problem, video notice, detector, pause hint; each with its own buttons.

Problem box (orange):
- **BP1 Dismiss** (after R2, on NASA's page). NOW. The box hides; the key
  `nasa:network:2026-11-15` is stored; another failing pull of NASA today keeps it hidden.
- **BP2 SUSPECT: one dismissed problem at a time across sources.** NOW (airplane mode). After BP1,
  swipe to APOD: its box shows (another key); Dismiss; swipe back to NASA: **NASA's box shows again**.
  `problemNoticeKey` is a single slot overwritten by each Dismiss (MA:1128-1138, ST:750-758), so with
  "More NASA pictures" on a user who dismisses on two pages sees the first come back. CLAUDE.md says a
  dismissed problem stays hidden "until its kind or HTTP code changes or a new day starts".
  **Fixed:** one key per source (`<source>.problemNoticeKey`, ST:756), and the box takes the first
  candidate not dismissed (MA:840): NASA's stays hidden.
- **BP3 "… since <day>"**, HTTP "error 503 … Trying again later.", "… has changed … An app update may
  be needed.", the storage text. NOT NOW (need a day-old failure, a 5xx, a site change, a full disk).
- **BP4 A new day shows a dismissed problem again** (the key holds the date). NOT NOW.

Video notice (neutral): **BV1–BV6** (the texts per setting, Dismiss for the date, Don't show again for
good). NOT NOW (APOD is an image today; "Don't show again" is irreversible anyway).

Detector box (neutral, O14): **BD1** "Your phone stopped APODroid: the 06:00 change came late." with
"Keep running" (opens Advanced and scrolls to the switch, doesn't turn it on) and "Dismiss"; **BD2**
with Keep running on: "Your phone still stops APODroid: …" plus the stock line "A force stop ends the
changes until APODroid opens." and "App info". NOT NOW: needs a force stop before a change time and a
page open 30+ min after it (06:00 + offset + 30 min), and a force stop drops Live on both screens.

Pause hint (neutral; while the daily switch is on, unless hidden for good; depends on the FP6's
battery setting and "Manage app if unused"):
- **BH0 Is it there?** NOW. If the battery is Unrestricted and the unused-app setting off, no box: BH1–3
  are NOT NOW (changing those settings is off limits).
- **BH1 "App info".** Opens APODroid's App info; Back returns (log: the resume's `Daily job: alarms…`).
- **BH2 "Battery optimisation"** (only when the battery line shows). Opens Android's battery
  optimisation list (`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`), else App info. Back returns.
- **BH3 "Don't show again" → dialog "Hide this hint?"** with "If you don't open APODroid, after about 8
  days …" / "… after about 3 months …"; **Cancel only** (Hide is irreversible). The box stays.
  - **BH3a Rotate with the dialog open:** it stays (C8's fix).
- **BH4 SUSPECT (text, only if the unused line shows):** "App info › Unused app settings › "Pause or
  manage app if unused": off" (MA:2863) matches no Android version word for word (Android 13–14 "Pause
  app activity if unused", 15–16 "Manage app if unused"), against CLAUDE.md's "Android's own labels
  word for word". **Fixed:** "Manage app if unused" on API 35+, "Pause app activity if unused" on 33–34
  (MA:2863); the emulator (API 34) shows the latter.

### L. The status lines (MA:866-900, 2174-2178)

Under "Change automatically" while it's on, from the stored records.
- **L1 Read them.** NOW. Home and lock show different pictures, so two lines:
  "Home: 15 Nov, HH:mm · NASA" and "Lock: 15 Nov, HH:mm · APOD" (the time of each screen's last set or
  hand-over, 24 h or "h:mm AM" as the system's clock; " · APOD" because APOD isn't ticked alone). No
  "Live isn't on…" line (home is live).
- **L2 After K2/K3 or K7/K8:** that screen's time moves to now.
- **L3 After D2:** both times move to now.
- **L4 "Changed …"** (one line, both screens the same picture), "Video day, previous kept",
  "Live isn't on. Set as wallpaper turns it on." NOT NOW.
- **L5 The system's 12/24 h switched while the page is open:** follows at the next resume. NOT NOW
  (an Android setting).

### E. The empty state (MA:1691-1701)

- **E1 No stored post for the shown source:** a spinner while loading, then the problem's text or
  "Couldn't load today's picture." with "Try again"; only the detector box and pause hint below. NOT
  NOW (all three sources have a stored post; clearing data is forbidden).

### O. Rotation, two panes, background (MA:525-665, 1565-1596)

- **O1 Rotate to landscape.** NOW. Two panes (~55/45): picture side left, settings right, each
  scrolling alone; the shown source, Setup's and Advanced's open state kept; crop mode and open menus
  close; the explanation and an open ⓘ dialog stay (C8's fix). Log: `Daily job: alarms for …`.
- **O2 Pull on the left pane in landscape:** R1's result; on the right pane: nothing (R5).
- **O3 Back to portrait.** One column again, same state rules.
- **O4 Home key, then back (O12).** NOW. The pictures are let go at onStop and decoded again; the page
  keeps its height (no jump), no crash. Optional check: `adb shell dumpsys meminfo io.github.buerlino.apodroid`
  (native heap) drops after Home with the page stopped.
- **O5 Back, start again.** NOW. A fresh page: opens on home's source (T1), the draft and the chosen
  source dropped.

### A. Advanced settings (MA:2230-2258, 2303-2359, 2710-2760; KR)

Shown only while the daily switch is on; closed by default; act at once (no highlight).
- **A1 Tap "Advanced settings".** NOW. Opens ("▴"): "Save every picture" + ⓘ + switch, then (if on)
  "Largest original 20 MB ▾", then "Keep running" + ⓘ + switch (two switches: "Centre when unsure"
  moved to Setup, UI1, setup.md J6). Tap again: closes.
- **A2 ⓘ of "Save every picture".** NOW. Dialog "Saves each new picture to Pictures/APODroid: full
  size up to the limit, else screen size. A few hundred MB a month or more." OK, Back or a tap outside
  closes; the switch doesn't toggle.
- **A3 "Save every picture" on.** NOW, put back at once. Nothing is saved now (the job's next run
  saves: careful, the fallback job may run any time, and **D2's ON starts it at once**: keep it off
  during D). "Largest original" row appears.
  - **A3a "Largest original" → "50 MB".** Menu 10 MB / 20 MB / 50 MB / 100 MB / No limit, ✓ before the
    current; the button shows "50 MB ▾" at once. Put back as found.
  - **A3b Off.** The row hides. No log either way.
- **A4 ⓘ of "Keep running".** NOW. Dialog "Keeps APODroid running, so the phone can't stop it between
  changes. It shows as an active app."
- **A5 "Keep running" on** (if found off). NOW. The service goes foreground: `dumpsys activity services`
  shows `KeepRunning` with `isForeground=true`; Quick Settings shows "1 active app" with APODroid
  (Android 13+ without the notification permission: no shade notification). No log on success
  (`Keep running: couldn't start: …` / `refused: …` on a refusal). **Don't tap "Stop" there.**
  - **A5a Off.** The service stops (no `ServiceRecord`), Active apps no longer lists it. Leave as found.
- **A6 Rotate with Advanced open:** stays open (saved state).
- **A7, A8** gone with UI1: "Centre when unsure" is Setup's, part of the draft (setup.md J6).

### D. "Change automatically" (MA:1015-1077, 2167-2173; ST:484-490; DJ:295-308)

- **D1 Turn it off.** NOW (D2 restores). At once: the status lines, "Advanced settings", the pause hint
  and the detector box disappear; Setup's summary loses " · 06:00"; the chip and pictures stay; the
  wallpapers are untouched (Live keeps panning on home). Keep running's service stops if on. **No log
  line.** Check: `dumpsys alarm | grep apodroid` none, `dumpsys jobscheduler io.github.buerlino.apodroid`
  no job 1/2, `dumpsys activity services` no KeepRunning.
  - **D1a A pull or a page open while off:** fetches, sets nothing (no `Page:` line).
- **D2 Turn it on.** NOW. Both screens are set afresh with the stored setup (the same pictures):
  toast **"Wallpaper set"**; status lines with both times now. Logs, in about this order:
  `Live: the engine is on HOME, not noted live` (startAfresh dropped the live note, ST:838),
  `Daily job: alarms for 2026-11-16T05:0x:xxZ` (or the next hour with "As soon as it's out"),
  `Switch on: wallpaper set: Of(source=APOD, date=2026-11-15, …) on LOCK[, darker…]; wallpaper shown live: Of(source=NASA, date=2026-11-13, …) on HOME[, darker…]`,
  then the freshly scheduled fallback runs at once: `Daily job: job 1 starts, not expedited`,
  `Daily job: alarms for <now + 15 min>`, `Daily job: jobFinished (job 1)`,
  `Daily job: alarms for 2026-11-16T05:0x:xxZ`. Check: `dumpsys wallpaper` home still
  `.LiveWallpaper` (id unchanged), lock id changed; alarms and job 1 back; KeepRunning foreground
  again if it was on. With "Save every picture" on, job 1 would also save APOD's and NASA's posts to
  the gallery (`Daily job: saved: …`): keep it off here.
  - **D2a On while a load runs** (tap right after opening): the switch's run waits for the load, then
    runs once (B6). Optional.
  - **D2b Quick on/off/on taps:** one set, not two (`switchRuns`). Optional (several re-sets).
  - **D2c Set and ⛶ are disabled (dimmed) while the switch-on run goes** (~1 s).
- **D3 The switch with Size Live and home not live** ("Live isn't on…" line, Fill on home). NOT NOW.

## 2. Counts

- Leaves: 111 (T 7, W 6, P 4, C 8, H 3, K 22, S 10, R 7, BP 4, BV 6, BD 2, BH 6, L 5, E 1, O 5,
  A 9, D 7); SW2 added K16–K23 (15 with their sub-leaves), checked on the tablet, not in the list
  below (it is the FP6's of 15 Nov); SA3b K27–K33 (8 with K29a) and A7–A8, for the emulator.
- Testable now: 89 (5 of them, BH1–BH4 and BH3a, only if the pause hint shows on the FP6; K4 only
  with the user's OK, it isn't reversible by hand). Not testable now: 22 (video day: P3, BV1–6;
  detector: BD1–2; failing site, set or disk: P2, K5, S5, BP3; time or new day: BP4, L5; empty store or
  Clear storage: E1, S6; another setup or a lost Live: T3, H2, K9, L4, D3).
- SUSPECT: 5 (C8, S2a, S4a, BP2, BH4); K1c is a UX note (doc-intended). C8, S4a, BP2, BH4 fixed since
  (README); S2a open.

## 3. Prioritized phone test list (≤30, each from the previous end state)

Start: S0, portrait, page closed. End: S0 again (pictures, Live on home, settings, gallery, Android
settings). Deletions and restores are part of the list.

1. **P-baseline.** Record S0 (section 0): Advanced's three values, boxes, status lines, `dumpsys
   wallpaper`, gallery rows, airplane/rotation settings, KeepRunning, alarms, jobs. Open the page.
2. **T1** opens on NASA, chip "On home screen", **L1** two status lines, **H1** on all three pages via
   **W1–W3** (swipe left to Earth, right to NASA and APOD, end stops).
3. **T2/T2a/T2c** dropdown from APOD: order and ✓, choose Earth, then open and dismiss with Back.
4. **C1–C3, C5** on APOD (swipe there): source line, credit, "more", expand/collapse by the
   explanation and by the title block; swipe away and back: still expanded (C5).
5. **C6, C7** long presses on APOD (link, explanation, credit copies the link); no app toast.
6. **C8 (fixed)**: expand APOD's explanation, rotate to landscape: still expanded. **O1, O2/R5** two
   panes (the emulator: 411 dp wide in portrait, about 900 dp in landscape: two panes), pull on each
   pane; **A1, A2** open Advanced, open the ⓘ, rotate back to portrait: Advanced open, the ⓘ dialog
   still open (**O3**).
7. **P1** tap APOD's picture: browser; Back: APOD still shown; `Daily job: alarms …` logged.
8. **S1a** quick double tap ☆ on Earth: one toast, one `EO_…` row; **S1b** tap again: "Already saved".
9. **S2a (SUSPECT)**: with the page in front, delete that row by adb; tap ★: "Already saved"?
   Then Home + reopen (**O4**): ☆, no crash.
10. **K1, K1a, K1b** ⛶ on Earth: drag, Back, ⛶ again opens at the middle.
11. **K10, K11** in crop mode on Earth: swipe does nothing; title → APOD closes crop mode.
12. **K1c** ⛶ on Earth, drag, ✓: no toast, no log; ⛶ again: where left. (Drag back to about the
    middle and ✓ to tidy up.)
13. **K2** ⛶ on APOD, "Whole picture" on (tertiary, preview), ✓: "Wallpaper set", `Part:` lines,
    lock id changed, **L2** "Lock:" time now.
14. **K12** ⛶ on APOD (opens "✓ Whole picture"), rotate: crop mode closed; rotate back.
15. **K3** ⛶ on APOD, "Whole picture" off, ✓: "Wallpaper set", back to Fill. (S0's look restored.)
16. **K6, K6a** ⛶ on NASA: Live frame at the clock, no drag, compare with `Live: drew … p=`; ✓
    without change: nothing.
17. **K7** NASA "Whole picture" on, ✓: "Wallpaper set", `Part: wallpaper shown live …`, engine
    `p=0.50`, home screencap whole and still, wallpaper ids unchanged.
18. **K13** ⛶ on NASA, Home key, back: crop mode still open; Back closes it.
19. **K8** NASA "Whole picture" off, ✓: panning again (`p=0.NN`). (S0's look restored.)
20. **R1** pull on NASA's page: spinner, no change, no log.
21. Airplane mode on (USB adb). **S3** ☆ on Earth: "Saved a smaller copy…", the W log line.
22. **R2, R2a** pull: "NASA Image of the Day: no connection.", W `Update failed`; swipe to APOD and
    Earth: their own texts.
23. **BP1, BP2 (fixed)**: Dismiss on Earth, swipe to APOD (shows), Dismiss, swipe to Earth: stays
    hidden. Swipe to NASA (shows): Dismiss.
24. Airplane mode off (undo, ping works). **R3** Home + reopen: all boxes gone.
25. **S4, S4a (fixed)** only if an original is several MB (check size): ☆ on it, rotate at once, tap
    ☆ in the new page: "Still saving"; at the end ★ fills and "Saved to Pictures/APODroid" shows. (Else skip.)
26. **BH0–BH3** if the pause hint shows: App info → Back; Battery optimisation → Back; Don't show
    again → Cancel (**BH4** compare the wording with App info). Else note "no hint".
27. **A3, A3a, A3b** Save every on, Largest original 50 MB, back to S0's value, Save every back to
    S0's state (off before step 29). **A4, A5, A5a** Keep running ⓘ; on/off with dumpsys and Active
    apps; leave it as in S0.
28. **D1** daily switch off: lines, Advanced, boxes gone, no alarms/jobs/service, no log; **D1a**
    pull: nothing set.
29. **D2** daily switch on: "Wallpaper set", the log sequence, job 1 runs at once, alarms for tomorrow,
    Live still on home, KeepRunning back if it was on; **L3** both times now.
30. **Restore and verify S0:** delete every gallery row the tests added (S1's if not deleted in step 9,
    S3's, S4's) and re-run the content query against S0's list; restore `accelerometer_rotation` and
    `user_rotation`; airplane mode as in S0; Advanced's three values as in S0; `dumpsys wallpaper`:
    home `.LiveWallpaper`, lock a still APOD in Fill; the alarms armed for tomorrow; **O5** Back and
    reopen: opens on NASA. Leftovers to report: Earth's stored part for its 13 Nov post (step 12),
    `problemNoticeKey` for today (step 23: a real problem of that kind today stays hidden on that
    source's page until midnight), the moved status-line times.

## 4. 0.8.0's cases for the emulator (after the list above; debug build, `run-as`)

31. **P3a, P3c, P3d** (VS1): fake an APOD video day (SKILL.md's "Fake an old day", `isVideo` true,
    `videoUrl` = 16 Oct's mp4); open: `Video still: <ms> ms, <w>×<h>`, the dimmed still behind ▶; a
    second open after `am kill`: no line. **P3c** airplane mode on a fresh fake (`videoStillTried`
    removed): `Video still: couldn't make one of …`, black tile, no try at the next open. **P3d**
    `videoUrl` removed: black, no line. Restore the real post with a pull.
32. **K16, K16b, K17, K18** (SW2): a landscape picture faked; the pill, the band, ✓ `…, sideways`, off
    again. **K24, K24a–c** with "Wide pictures sideways" stored on (setup.md P31–P32 first).
33. **K27, K28, K29, K31** (SA3b): a picture whose `Subject:` line's `p=` is ≥ 0.2 off the middle,
    Size Auto stored: ⛶ opens at it; ✓ untouched: nothing; drag ✓: `cropPosition`, no `, auto`; the
    pill on an undragged frame jumps to `subjectSideways`.
34. **setup.md J6** (UI1; was A7, A8): Setup's "Centre when unsure" off + Set: `autoFallback=false`,
    a picture with `p=0.55`–`0.65` `, auto 0.6x`; on again + Set.
35. **K30, K33**: an undragged Whole part, Size Centre ↔ Auto and Set: the undragged frame follows
    Size, a dragged one stays.
