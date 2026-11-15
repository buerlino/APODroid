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

SW1 → SW2 → SW2T → D1 → VS1 → SW3 → SA2 → SA3a → SA3b → R3 → T3. One device, one pipeline
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
