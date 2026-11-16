# 0.8.0: the release plan (deadline 17 Nov 2026)

(User, 2026-11-15 evening: "android 10-14 for 0.9.0. the rest we start now. make a plan then
verify it. add everything now. deadline is nov 17. i want 0.8.0 done by then.") Not
`plan-0.8.0.md`: `scripts/pipeline.sh` takes the newest `plan-*.md` as "plan", and 0.7.0's done
queue rows (`plan R1`, `plan T2`, …) must keep resolving. 0.8.0's rows name their track files.

## What's in 0.8.0

| Track | What | File | State |
|---|---|---|---|
| SW | Sideways (issue #4): ⛶'s toggle (A), then a Setup switch for wide pictures (C) | `sideways-0.8.0.md` | SW1, SW2 running 15 Nov evening |
| SA | Fill's frame on the subject (issue #3): Size [Centre \| Auto \| Live], Auto the default; Advanced's fallback switch | `subject-0.8.0.md` | decided 15 Nov evening (two rounds of picks); SA2, SA3a, SA3b queued tonight |
| VS | A still from the video behind ▶ on video days (Open question 2) | `video-0.8.0.md` | queued |
| D1 | A second picture on the same date arrives at the next check (Open question 3) | this file | queued |
| R3, T3 | The release's texts; the final tablet pass on the combined tree | this file | queued |

Out: Android 10–14 on the emulator (E) moves to **0.9.0** (user, 2026-11-15). B (sideways
by a rule, no setting) only if A and C aren't enough. 0.7.0's FP6 week goes on beside it; a fix
there is 0.7.1 or folds into 0.8.0, whichever ships first.

## The two days

- **Sun 15 Nov evening:** SW1, SW2 in the pipeline (started 19:45). SA0 on the desktop by an
  agent (no device, no Gradle): the methods' frames against the old picks, and the new picking
  page. Then `APODROID_TABLET=emulator-5564 scripts/night.sh --until 05:00` (the closing takes up to
  3 h; the device is the Android 14 emulator while pascaline has the tablet): SW2T, D1, VS1, SW3
  (texts as placeholders).
- **Mon 16 Nov morning, the user (about 45 min):** SA0's picking page; the answers to
  the texts from rosie's screenshots (Sideways, SW3's switch and ⓘ, Advanced's "Auto: centre
  when unsure" and ⓘ, the video still's look); SW3's threshold and whether it applies under
  Live. (SA0's picks were done on 15 Nov evening: two rounds, decided.)
- **Mon 16 Nov day:** fixes from the morning; what the night didn't reach of SA2, SA3a, SA3b, R3.
- **Mon 16 Nov night:** T3, the final tablet pass on dev's R8 build, then the regression.
- **Tue 17 Nov:** the FP6 with the user (the release-signed build of the candidate over 0.7.0:
  Sideways on 9 Nov's picture on both screens, Fill and Live; the switch; a video day if one
  falls due, else faked on the tablet only); the tag. A part that isn't ready by Tue noon is cut
  from 0.8.0 (its rows unmerged or its switch hidden), not the date moved: the user's call.

## The order (the queue, night.md)

SW1 → SW2 → SW2T → D1 → VS1 → SW3 → SA2 → SA3a → SA3b → R3 → T3 → SW3f1 → SA3c → D1f1 (the
user's answers of 15 Nov night; T3 had started), then the night's test pass below. One device, one pipeline
at a time. D1, VS1 and SW3 each wait only for SW2 (one that isn't merged doesn't hold the others
back); SW3's texts are placeholders until the morning. SA2 waits for nothing (decided 15 Nov evening); SA3a for SA2 and
SW3, SA3b for SA3a.

## Rules for every 0.8.0 package

- `plan-0.7.0.md`'s For every package holds (the stack, no new dependency or permission, texts
  short and approved, `:core` tested, the maps updated, `CLAUDE.md` written).
- **No native library and no new dependency for the deadline** (night default, the user may
  overrule in the morning): TensorFlow Lite/LiteRT is out of 0.8.0 (F-Droid's scanner, the APK
  size and the reproducible build unchecked, SA1 skipped). A method that needs it moves to 0.9.0.
- Sideways and the subject's frame meet in one place (`Part.position` along the frame's one
  direction): SA3a notes the subject's position for the upright picture and the sideways one.
- Every new log line greppable; the debug dumps as L4's.

### [ ] D1. A second picture on the same date arrives at the next check (Opus, medium)

Read first: release-0.8.0.md's "Rules for every 0.8.0 package". Open question 3 (SIM, 2026-10-26: 143 of 408 second posts
ever stored, most at US Eastern midnight; never seen in the real feeds).
- **The cause:** `Store.checkSite` (Store.kt ~1257, called by `update` ~947) skips a source whose
  stored post is today's: `if (force || !isCurrent(source)) refresh(source)`. `isCurrent` has no
  other caller. A second post that day comes only with a pull or the next date.
- **The change:** the job's runs fetch every source in use: `checkSite`'s condition becomes
  `force || !page || !isCurrent(source)`. The page's opens keep `isCurrent` and the hour's skip.
  **Don't pass `force` from the job:** `force` is also `setDue(pull = force)` (~953), which would
  put the newest on at once, as a pull. Only the JSON; the picture is downloaded only for a new
  post (another date or picture URL), as now.
- **A failure on a day already stored** (night default; the alternative: a problem as any other
  check): with today's post stored, a fetch that fails (the network, an HTTP error,
  `SiteChanged`) is logged only (`Refresh: <source> <date> stored, check failed: …`): no
  `<source>.problem`, no failed run (no `noted(failed)`, no hourly retry), `checkedAt`
  untouched. The stored post is complete; nothing is owed. A failure with an older post stored
  stays as now.
- **The rules are unchanged on purpose:** `refresh` already stores a same-date post with another
  URL (it returns early only for an older date, downloads when the URL differs, marks the gap as
  any new post; ~875-896), and "never back" is by date (Screens.kt ~347). Rule 2 puts it on only
  if it was published before the change time + 3 h (`pictureWindow`), or with "As soon as it's
  out"; a later one is stored and waits for the source's next turn (with APOD alone usually
  replaced by the next date first). Say so in `CLAUDE.md`.
- **Log lines:** `Refresh: APOD 2026-11-16 same post` when the fetched post is the stored one,
  `Refresh: APOD 2026-11-16 new picture` for a same-date post with another URL.
- **The cost** in `CLAUDE.md`'s Traffic: one JSON per source in use per run (APOD 21 KB, NASA's
  feed 49 KB, Earth 0.6 KB), about 4–8 runs a day, 24 with "As soon as it's out"; written there.
- **SIM** (`RulesSimulationTest`): mirror the change in `SimPhone.checkSite`/`isCurrent`
  (~333, 357-366); the extra fetches draw from a separate `Random` (as `stopR`, `orderR`,
  `liveR`, ~173-234), so the other seeds' draws don't shift. Add a counter of second posts
  (the `-b` URLs, ~139, 148) ever stored and report it before and after (no promised number);
  no other invariant changes; the seeds as T1 ran them.
- **Tests:** SIM as above; a `:core` test where logic moves there.
- **Device** (rosie, the emulator): with today's post stored, `run -f 1` logs `same post` and
  downloads nothing; the same with airplane mode: the log's `check failed`, no problem box on
  the page, no retry armed (`dumpsys alarm`); the smoke list.
- **Done when:** the tests pass; `CLAUDE.md` (Background update, where the job's skip is
  described, and B21's "also when `isCurrent` skips the fetch" in Turns, rule 2; Traffic; Open
  question 3 closed); the map (`interactions/background.md` E1, "No fetch", and E3's problem
  box for this case).

### [ ] D1f1. Seed 1204: confirm the app, tighten invariant 12 (Opus, medium)

(User, 2026-11-15 night: "redo" D1's widened invariant; the more maintainable way, recommended
and taken: keep the app, make the test's exception exact.) D1 widened SIM's invariant 12 (a Set
of the order alone sets only the owner's picture) to also accept `leads[screen]?.picture`, the
kept record's own picture, for seed 1204 of 2000 (D1's report: the picker shared the engine,
`unnotedLive` forgot the lock record, and with equal columns the lock screen got home's kept APOD
05-16 while APOD's newest was the 05-17 video with "Skip it").
Read first: CLAUDE.md, Turns ("Equal columns": the lock screen takes home's record and picture)
and Wallpaper: Live ("The engine on a screen not noted live"); `RulesSimulationTest`'s invariant
12 (`orderAlone`, ~1036-1056 on dev; `leads` ~1020, `equalColumns()` ~437); D1's commit message
(`git log -1 f269339^2`). Seed 1204 runs only with `SIM_SEEDS=2000` or `SIM_ONLY=1204` (~96-99).
- **Replay** seed 1204 (`SIM_SEEDS`/`SIM_ONLY` as the test reads them) with D1's widening
  removed and trace the failing Set: each screen's record before and after, the columns, what
  `unnotedLive` forgot, what was set.
- **If** the lock screen got exactly the picture home's record names in that same run with
  equal columns: replace the widening with that exact exception, `screen == LOCK &&
  equalColumns() && picture == records[HOME]?.picture` (with equal columns `leads[LOCK]` is
  already `prior[HOME]`, so this narrows D1's to equal columns and home's record after the run),
  with a comment naming CLAUDE.md's rule; SIM 240 seeds
  both tests and `SIM_SEEDS=2000` pass. No app code changes.
- **Else** (another picture, or columns that differ): don't change the app tonight; put seed
  1204 in `knownBugs` with its `@Ignore`d test, restore invariant 12 as before D1, and report
  the trace as a QUESTION.
- **Done when:** the tests pass; the report names which case it was.

### [ ] R3. The release's texts: versionCode 12, the changelog (Sonnet, medium)

Read first: release-0.8.0.md's "Rules for every 0.8.0 package". After the last feature package merged (SW3, VS1, D1, SA3b).
- `app/build.gradle.kts`: versionCode 12 (13 if a 0.7.1 took 12 first: check the tags),
  versionName 0.8.0.
- `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`: short lines, what a user sees
  (Sideways, the switch, the video still, the second post on a date, the subject frame if
  built), under F-Droid's 500 characters (`11.txt` is 454 bytes).
- The store description (`full_description.txt`) where a feature changes what it says.
- `CLAUDE.md`: E's move to 0.9.0 (Setup and distribution, the emulator line; Next features);
  Releases gets 0.8.0's line after the tag, not now; the skill's "Still untested".
- **Done when:** the texts listed for the user; the Gradle check passes.

### [ ] T3. Final device pass on the combined tree (rosie, the emulator; Opus, high)

Read first: release-0.8.0.md's "Rules for every 0.8.0 package".
- **The update from 0.7.0:** dev's R8 release build signed with the debug key, installed with
  `install -r` over 0.7.0's R8 build from the tag `v0.7.0`, also signed with the debug key (as
  O15 and T2 did; `.coord/runs/115-o15/` and SKILL.md's R8 recipe). Not `private/0.7.0-test/`:
  it is release-signed (the user's key) and can't be replaced by a debug-signed build. The
  setup kept, the alarms armed by `MY_PACKAGE_REPLACED`, nothing set. The release-signed update
  is the user's, on the FP6 on 17 Nov.
- Then each 0.8.0 feature's device bullets once more on R8 (SW2's, SW3's, VS1's, D1's `same
  post` log line, SA3a's and SA3b's), the smoke list, and the regression's core paths (Set, the
  daily switch, a run by `run -f 1`, Live, ⛶ and ✓). Screenshots for the user.
- **Done when:** PASS, or each FIX as a package for Tue 17 Nov morning, before the noon cut.
- **Checked** (T3, rosie, 2026-11-15, 22:36–23:00; the tablet HA24BZ2Y not run: the Android 14
  emulator `emulator-5564`; dev 93917b7's R8 build and v0.7.0's, both signed with the debug key;
  pictures faked into `files/apod.jpg` by `adb root` with the stored date and URL kept, so a job's
  check is `same post`; evidence in `.coord/runs/134-t3/tablet/`): **FIX, one finding (T3f1
  below)**; everything else passed, no crash (`exit-info`: no CRASH).
  - The update: 0.7.0 (home APOD + NASA every 6 h, lock APOD, Fill, daily on, set) → `install -r`
    of 0.8.0: the prefs byte for byte but `armedUpdate`, `Daily job: MY_PACKAGE_REPLACED, arming
    again`, the pair at 00:09:15 as before, the wallpaper id unchanged, no set line; Setup shows
    Auto selected and unhighlighted (`size` still `FILL`). **But** no subject noted at the page
    open, and Set then set the middle (T3f1).
  - SW3: the switch on, Set: 3:2 (3618×2400) `, sideways` on HOME+LOCK in one set; 1.2:1 and a
    portrait one upright; ⛶ the pill on; off ✓ upright (`sideways=false`, no `cropPosition`); a
    new post (part dropped) sideways again; the pill off-on ✓ saved `sideways=true` without a
    set; the switch off and Set: still `, sideways`, a new post then upright; a rotation kept the
    draft's switch and the highlight. With Live (the picker's "Home screen", the dialog "Set
    wallpaper › Home screen" for columns that differ): `wallpaper shown live … on HOME, sideways`,
    `live.sideways=true`, `Live: decoded 1592×2400`, the lock screen its own still `on LOCK,
    sideways`. R8 writes no `debug-set.png` (not debuggable), so the turn is by the log only.
  - SA3a: 2026-01-09 `Subject: … p=0.78 (sideways 0.48)`, `auto 0.78`, crop 702–1086;
    2026-03-23 `p=0.57`, `auto 0.50`, crop 701–1300; Advanced's switch off: `auto 0.57`, crop
    805–1404; Centre and Set: crop 701–1300, `size=CENTRE`, no `, auto` (the same figures as
    SA3a's debug run). Live → Auto and Set: the engine destroyed, one still on both.
  - SA3b: ⛶ opened at the subject (0.57); ✓ without a drag: nothing logged, no part; a drag
    and ✓: `cropPosition` 0.136, crop 190–789, no `, auto`; Whole ✓ without a drag: `whole=true`,
    no `cropPosition`, the whole picture on its blur; Whole off ✓, Centre, Set: the middle; a
    rotation in ⛶ closed it, no crash. Screenshots of Advanced and the ⓘs in the folder.
  - VS1: `isVideo` and the 16 Oct mp4 faked: `Video still: 868 ms, 1080×720`, the tile dimmed
    with ▶; a second open after `am kill`: no log line.
  - D1: `run -f 1` with today's post stored `Refresh: APOD 2026-11-15 same post` (each run).
  - The core paths and the smoke list: Set in both modes (APOD mode `where=BOTH`, set id 79),
    the daily switch off (the pair cancelled) and on (`Switch on: wallpaper set`, armed), `run
    -f 1` with and without Live (`jobFinished`, nothing set), Live, ⛶ and ✓.
  - Not run: real timing, Doze, ZUI's picker and cleaner (not on the emulator); a drag under
    Live (my swipe scrolled the page; the band stood at p=0.82); TalkBack; font scale 2.0; the
    release-signed update (the user's, FP6, 17 Nov).
  - Seen, not a FIX: the set line of a whole picture still notes `, auto 0.57` (it has no frame;
    as SA3b's `, auto 0.50` for a centred fallback: the log only).

### [ ] T3f1. Auto's subject at a page open or Set within the hour (Opus, medium)

From T3 (rosie, 2026-11-15, R8 on the emulator, `.coord/runs/134-t3/tablet/f1-update-subject-log.txt`).
- **The bug:** `Store.update` (~1140) skips `checkSite` for a page open, a pull-free Set or the
  daily switch's run whose source was checked within the hour (`checkedRecently`), so
  `noteSubject` (SA3a's call in `checkSite`'s `else`) never runs there. After the update from
  0.7.0 (no `subject*` keys), a page open and Set within the hour of the last check noted nothing,
  and Set's still went on at the middle with Size Auto: `Set: wallpaper set: … on HOME+LOCK`
  (no `, auto`) for today's APOD whose subject is 0.13 (a pull then logged `Subject: APOD
  2026-11-15 p=0.13`, and the next Set `, auto 0.13`). The job's next run notes it but sets
  nothing (the record is current), so the screen keeps the middle until the next change time or
  post. CLAUDE.md (Wallpaper: Auto, the subject) says the page open covers the update.
- **The fix:** note the subject also where the page skips the check:
  `if (force || !page || !checkedRecently(source)) checkSite(source, force, page) else noteSubject(source)`
  (`noteSubject` returns at once when noted, the post is a video or the file is missing; it
  holds the refresh lock and throws nothing).
- **Device** (rosie, the emulator): the update as T3 (v0.7.0's R8, a Set, `install -r` of the
  fix's R8): the page open logs `Subject: APOD …` (and NASA's, Earth's when offered); Set with
  nothing changed logs `, auto <p>` when the subject moves 0.2 or more; the smoke list.
- **Done when:** the Gradle check, the device pass; CLAUDE.md's sentence names the skip within
  the hour.

### [ ] UI1. Setup's last changes from the FP6 (Opus, medium)

(User, 2026-11-16 morning, testing 0.8.0 on the FP6.) Read first: release-0.8.0.md's "Rules for
every 0.8.0 package"; CLAUDE.md, The app: Settings (Setup's rows, "A hidden setting goes back to
the stored one", the highlight, Set as wallpaper, Advanced), "Auto: centre when unsure" (SA3b) and
"Wide pictures sideways" (SW3, SW3f1); `interactions/setup.md` (X, J, the Set rows) and
`page.md`'s Advanced cases. MainActivity ~2210-2240 (the rows, the Set button, Advanced).
- **"Auto: centre when unsure" moves into Setup** (out of Advanced), right after "Wide pictures
  sideways", as a switch with its ⓘ (the dialog text unchanged). The label without the colon:
  **"Centre when unsure"** (placeholder: the user disliked "Auto: …"; the label for the user's
  approval from the screenshot). It becomes **part of the draft** like every Setup row (Setup is a
  draft until Set): `Setup.autoFallback`, `saveSetup` writes the pref `autoFallback` (same key,
  default on, no migration), the saved state, the highlight ("On, not set yet"), the Setup row's
  highlight, `orderAlone` comparing it with the rest of `Setup`. Set sets each screen afresh, so
  a change shows at once. **Shown only while the draft's Size is Auto**; while hidden it goes back
  to the stored value (CLAUDE.md's rule). Advanced keeps "Save every picture" (with "Largest
  original") and "Keep running".
- **"Wide pictures sideways" hidden, not greyed, while the draft's Size is Live** (replaces SW3f1's
  greyed row); hidden, it goes back to the stored value. The stored value still counts as SW3f1
  says (`sidewaysOf`: never under the stored Live).
- **Set as wallpaper pinned at the bottom** while Setup is open: a bar at the bottom of the
  screen (one column) or of the settings pane (two panes), over the scrolling content, which
  gets bottom padding of the bar's height so its last row can scroll clear; the navigation bar's
  inset respected; the theme's surface behind the button. While Setup is closed the button stays
  in its place after Setup, as now. Enabled and disabled exactly as now; one button, not two
  (screen readers find it once). No new text.
- **Tests:** a `:core` test only where logic moves there (`orderAlone` with the new field).
- **Device** (rosie, the emulator): Setup open, the bar at the bottom while scrolling, the last
  row reachable above it, in portrait, the landscape two-pane layout and at font scale 2.0;
  Setup closed: the button in its place, no bar. Size Centre and Live: no "Centre when unsure";
  Auto: shown; its change highlights it and Setup's title, Set saves it (`autoFallback` in the
  prefs) and sets at once; turned and then Size Live: the row gone and the draft back to the
  stored value (Setup's title not highlighted). Live: "Wide pictures sideways" gone; Auto: back.
  A rotation keeps the draft. Advanced: two switches. The smoke list. Screenshots for the user.
- **Done when:** the Gradle check, the device pass; CLAUDE.md (Settings' order, the
  `Setup`/draft list, the highlight list, SW3's and SA3b's paragraphs, Set as wallpaper's place,
  Advanced's contents, the ⓘ list), the maps (setup.md X5, J, the Set rows; page.md's Advanced).

### [ ] FP1. ⛶'s frame dragged sideways kept upright (Opus, medium)

From the user's FP6 test (2026-11-16 morning, 0.8.0 release build over 0.7.0; the debug run
`.coord/test-pass-0.8.0/debug-auto.md`). Read first: release-0.8.0.md's "Rules for every 0.8.0
package"; CLAUDE.md, Choosing the part (SA3b's undragged part, SW2's Sideways pill).
- **The bug:** in ⛶ with the Sideways pill on, a vertical drag on the picture (the band's own
  gesture, or a page scroll started on the picture) moves the band and sets `cropMoved`
  (MainActivity `cropFrame` ~2111-2128); turning the pill off keeps `cropPosition` and
  `cropMoved` (~1789 only re-places an undragged frame), so ✓'s `keepPart` (~1456,
  `notedPosition`) notes the sideways band's position as the upright frame's. Seen: the FP6's
  Set with Size Auto went on at 0.557 (`mCropHint` 861–1977 of 2663) instead of the subject 0.17,
  no `, auto`; ⛶ opened there after.
- **The fix:** a drag belongs to the orientation it was made in. When the pill changes
  orientation, `cropMoved = false` and `cropPosition = frameOf(post, the part without a
  position, the new orientation)`, so ✓ notes the previous stored position (null: Size and the
  subject decide) unless the frame is dragged again in the new orientation. Whole picture
  unchanged. No new text.
- **Tests:** none in `:core` unless logic moves there.
- **Device** (rosie, the emulator, Size Auto, a landscape picture under 3:2 with a subject off the
  middle): ⛶, pill on, drag the band, pill off, ✓: no `cropPosition` in the prefs, the next Set
  logs `, auto <p>`; pill on, drag, ✓: `cropPosition` noted, `, sideways`; pill off, drag, ✓:
  the upright drag noted. The smoke list.
- **Done when:** the Gradle check, the device pass; CLAUDE.md (Choosing the part: a drag belongs
  to its orientation); `interactions/page.md`'s ⛶ cases.

## The night's test pass (user, 2026-11-15 night: "test and then fix any issues")

After `night.sh`'s SHIFT DONE (its regression and report done), by the main session, on the
emulator `emulator-5564` (Android 14) and dev's debug build; nothing pushed, master untouched.
1. **The maps current:** one reading agent compares `interactions/*.md` with
   `git diff 7299474..dev` (0.8.0's packages added their leaves; SW3f1, SA3c, D1f1 too) and adds
   what's missing, with phone cases; committed on dev under `scripts/devlock.sh acquire integ
   maps 600`.
2. **Testers, one map at a time** (one device): setup, page, live, background; rosie by hand,
   each the map's whole phone case list that an Android 14 emulator can run, at most about
   75 min each, each holding `scripts/devlock.sh acquire tablet tester-<map> 1800` (released at
   its end), so no TP run's device check overlaps a tester (pipeline.sh merges with NOT RUN when
   the lock stays busy). The rules: `tester-rules.md` adapted
   (`interactions/tester-rules-emulator.md`, written by the main session before the first tester):
   the emulator is ours, so installs, `pm clear`, `adb root`, `run-as`, the clock stepped and set
   back, force stops and ★ saves are allowed; no real timing (Doze, buckets); the state left
   as found for the next tester (Setup and the wallpaper as the start screenshots). Reports with
   PASS / FAIL / DIFFERENT / SKIPPED per case to `.coord/test-pass-0.8.0/<map>.md`.
3. **Each FAIL or DIFFERENT** that is our bug: a fix package (`TP<n>`, its section below this
   one, `### [ ] TP<n>. <title> (<model>, <effort>)` under this `##`, Read first lines, the
   case ids, a regression test where `:core`), checked by a second agent before it runs, a queue
   row in night.md (the next Step number, Agent winston, Tablet yes, Spec `release-0.8.0.md
   TP<n>`; `scripts/pipeline.test.sh` resolves it), then `APODROID_TABLET=emulator-5564
   scripts/pipeline.sh TP<n>` by hand (winston, susanna, rosie on the emulator; the default is
   the tablet), only while no tester holds the device; merged on dev; the map's failed cases run
   again after. A finding that
   needs the user's decision (a text, a rule) becomes a QUESTION for the morning, not a fix.
4. **Last:** an R8 smoke of the final dev (T3's recipe: R8 debug-signed over v0.7.0's), and the
   morning report `.coord/test-pass-0.8.0/report.md`: what ran, what failed, what was fixed,
   the questions.
- **Ran** (2026-11-16, 00:23–02:10, dev c3cff29 debug and its R8, `.coord/test-pass-0.8.0/`): the
  maps brought to 3a4db15 (wp/maps-080, merged); setup 27 PASS, page 30 PASS, live 33 PASS (4
  SKIPPED: AOSP 14's preview has no "Lock screen" alone), background 16 PASS and 1 DIFFERENT (the
  detector, TP1 below), the R8 smoke (the update from 0.7.0 with T3f1, SW3f1, the smoke list)
  PASS; no crash. T3f1 (T3's FIX) merged before the testers (#138).

### [ ] TP1. The detector's sign on Android 11–14 (Opus, medium) — 0.9.0

From the background tester (2026-11-16, map background.md J2, P17's sign; the emulator, Android
14): after `am force-stop` and a page open, no `Detector: the alarms found gone` in 7 of 10 tries,
though `dumpsys alarm` listed no APODroid alarm each time. **Why** (AOSP read 2026-11-16,
android14-release `ActivityManagerService.forceStopPackageLocked`): on 14 a force stop cancels a
package's `PendingIntent`s only `if (packageName == null || uninstalling)`; the records live on
(weakly held) until system_server's GC, so `DailyJob.checkAlarms`' `FLAG_NO_CREATE` (DailyJob.kt
~209-216) finds them. On 15 and 16 they go too (`clearPendingIntentsForStoppedApp`, the
`stay_stopped` flag, on in ap3a, ap4a, bp1a): so the tablet and the FP6 are right. Alarms go on
every version. So on Android 10–14 the detector (O14) mostly stays silent after a force stop.
Not a 0.8.0 regression; Android 10–14 moved to 0.9.0 (E).
- **The idea** (to decide): on API 30–34 count also a `getHistoricalProcessExitReasons` entry
  with `REASON_USER_REQUESTED` (a force stop's, also `pm clear`'s) newer than the last arming
  (`armedAt`) as "the alarms gone"; on 35+ `ApplicationStartInfo.wasForceStopped()` adds nothing
  the `PendingIntent`s don't show (ZUI's cleaner set neither, O8). A force stop with no process
  alive leaves no exit record on 14: accepted. API 29: as now.
- **Decided** (user, 2026-11-16): 0.9.0, with E, which can test it on 10–14; 0.8.0 ships the
  detector as 0.7.0 did. The changelog `12.txt` and the store description approved as written;
  the open explanation kept across a swipe; the log oddities (CLAUDE.md, Open question 4) for 0.9.0.
