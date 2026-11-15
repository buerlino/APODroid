# 0.8.0: the release plan (deadline 17 Nov 2026)

(User, 2026-11-15 evening: "android 10-14 for 0.9.0. the rest we start now. make a plan then
verify it. add everything now. deadline is nov 17. i want 0.8.0 done by then.") Not
`plan-0.8.0.md`: `scripts/pipeline.sh` takes the newest `plan-*.md` as "plan", and 0.7.0's done
queue rows (`plan R1`, `plan T2`, …) must keep resolving. 0.8.0's rows name their track files.

## What's in 0.8.0

| Track | What | File | State |
|---|---|---|---|
| SW | Sideways (issue #4): ⛶'s toggle (A), then a Setup switch for wide pictures (C) | `sideways-0.8.0.md` | SW1, SW2 running 15 Nov evening |
| SA | Fill's frame on the subject (issue #3): auto when it moves ≥ 0.2, a switch default on | `subject-0.8.0.md` | decided 15 Nov evening (two rounds of picks); SA2, SA3 queued tonight |
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
  `subject-0.8.0.md`'s Open 1–4; the texts from rosie's screenshots (Sideways, SW3's switch and
  ⓘ, the video still's look); SW3's threshold and whether it applies under Live.
- **Mon 16 Nov day:** fixes from the morning; SA2, SA3 (only if SA0 says build); R3.
- **Mon 16 Nov night:** T3, the final tablet pass on dev's R8 build, then the regression.
- **Tue 17 Nov:** the FP6 with the user (the release-signed build of the candidate over 0.7.0:
  Sideways on 9 Nov's picture on both screens, Fill and Live; the switch; a video day if one
  falls due, else faked on the tablet only); the tag. A part that isn't ready by Tue noon is cut
  from 0.8.0 (its rows unmerged or its switch hidden), not the date moved: the user's call.

## The order (the queue, night.md)

SW1 → SW2 → SW2T → D1 → VS1 → SW3 → (SA0's decision) SA2 → SA3 → R3 → T3. One device, one pipeline
at a time. D1, VS1 and SW3 each wait only for SW2 (one that isn't merged doesn't hold the others
back); SW3's texts are placeholders until the morning. SA2 waits for "the user" (SA0's picks and
Open 1–4); SA3 for SA2 and SW3.

## Rules for every 0.8.0 package

- `plan-0.7.0.md`'s For every package holds (the stack, no new dependency or permission, texts
  short and approved, `:core` tested, the maps updated, `CLAUDE.md` written).
- **No native library and no new dependency for the deadline** (night default, the user may
  overrule in the morning): TensorFlow Lite/LiteRT is out of 0.8.0 (F-Droid's scanner, the APK
  size and the reproducible build unchecked, SA1 skipped). A method that needs it moves to 0.9.0.
- Sideways and the subject's frame meet in one place (`Part.position` along the frame's one
  direction): SA3 notes the subject's position for the upright picture and the sideways one.
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

Read first: release-0.8.0.md's "Rules for every 0.8.0 package". After the last feature package merged (SW3, VS1, D1, and SA3
if SA is built; if SA0 says no, the user takes rows SA2 and SA3 out of the queue and SA3 out of
this row's After).
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
  post` log line, SA3's if built), the smoke list, and the regression's core paths (Set, the
  daily switch, a run by `run -f 1`, Live, ⛶ and ✓). Screenshots for the user.
- **Done when:** PASS, or each FIX as a package for Tue 17 Nov morning, before the noon cut.
