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
