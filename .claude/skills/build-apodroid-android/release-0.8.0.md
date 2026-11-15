# 0.8.0: the release plan (deadline 17 Nov 2026)

(User, 2026-11-15 evening: "android 10-14 for 0.9.0. the rest we start now. make a plan then
verify it. add everything now. deadline is nov 17. i want 0.8.0 done by then.") Not
`plan-0.8.0.md`: `scripts/pipeline.sh` takes the newest `plan-*.md` as "plan", and 0.7.0's done
queue rows (`plan R1`, `plan T2`, …) must keep resolving. 0.8.0's rows name their track files.

## What's in 0.8.0

| Track | What | File | State |
|---|---|---|---|
| SW | Sideways (issue #4): ⛶'s toggle (A), then a Setup switch for wide pictures (C) | `sideways-0.8.0.md` | SW1, SW2 running 15 Nov evening |
| SA | Fill's frame on the subject (issue #3), only if SA0 beats the middle | `subject-0.8.0.md` | SA0 tonight, the user's picks 16 Nov morning |
| VS | A still from the video behind ▶ on video days (Open question 2) | `video-0.8.0.md` | queued |
| D1 | A second picture on the same date arrives at the next check (Open question 3) | this file | queued |
| R3, T3 | The release's texts; the final tablet pass on the combined tree | this file | queued |

Out: Android 10–14 on the emulator (E) moves to **0.9.0** (user, 2026-11-15). B (sideways
by a rule, no setting) only if A and C aren't enough. 0.7.0's FP6 week goes on beside it; a fix
there is 0.7.1 or folds into 0.8.0, whichever ships first.

## The two days

- **Sun 15 Nov evening:** SW1, SW2 in the pipeline (started 19:45). SA0 on the desktop by an
  agent (no device, no Gradle): the methods' frames against the old picks, and the new picking
  page. Then `scripts/night.sh --until 07:30`: D1, VS1, SW3 (texts as placeholders).
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

SW1 → SW2 → D1 → VS1 → SW3 → (SA0's decision) SA2 → SA3 → R3 → T3. One tablet, one pipeline at
a time. SA0 runs beside them (desktop only). SW3 waits for nothing but SW2; its texts are
placeholders until the morning. SA2 waits for "the user" (SA0's picks and Open 1–4).

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

Open question 3 (SIM, 2026-10-26: 143 of 408 second posts ever stored, most at US Eastern
midnight; never seen in the real feeds). Cause: every run skips a source whose stored post is
today's (`Store.isCurrent`, `Store.update`'s `if (force || !isCurrent(source)) refresh(source)`),
so a second post that day comes only with a pull or the next date.
- **The job's runs fetch every source in use** (not the page's opens: they keep `isCurrent` and
  the hour's skip). Only the JSON; the picture is downloaded only when the post is new (another
  date or picture URL), as now. Night default; the alternative: only the change-time runs fetch
  (the window's hourly ones skip).
- **The cost** in `CLAUDE.md`'s Traffic: one JSON per source in use per run (APOD 21 KB, NASA's
  feed 49 KB, Earth 0.6 KB), about 4–8 runs a day, 24 with "As soon as it's out"; written there.
- **The rules:** a second post on the same date with another picture URL is a new post
  (`isNoted` by URL); check that `refresh` stores it (not "older than the stored one": the same
  date) and that rule 2 puts it on by its publish time (a later `published` on the same date).
  "Never back" is by date, so it doesn't block it.
- **Tests:** `:core` where the logic is (`refresh`'s same-date decision if it moves there);
  SIM (`RulesSimulationTest`): its second-post count (stored ever) rises to nearly all of them,
  no other invariant changes; the seeds as T1 ran them.
- **Done when:** the tests pass; `CLAUDE.md` (Traffic, The page, Open question 3 closed); the
  maps (`background.md`: a job run with today's post stored fetches; the log).

### [ ] R3. The release's texts: versionCode 12, the changelog (Sonnet, medium)

After the last feature package merged (SW3, VS1, D1, SA3 if built).
- `app/build.gradle.kts`: versionCode 12, versionName 0.8.0.
- `fastlane/metadata/android/en-US/changelogs/12.txt`: short lines, what a user sees (Sideways,
  the switch, the video still, the subject frame if built), under F-Droid's 500 characters.
- The store description (`full_description.txt`) where a feature changes what it says.
- `CLAUDE.md`: Releases (0.8.0's line after the tag, not now), E's 0.9.0 move, the skill's
  "Still untested".
- **Done when:** the texts listed for the user; the Gradle check passes.

### [ ] T3. Final tablet pass on the combined tree (rosie; Opus, high)

dev's R8 release build (unsigned, as T2), over 0.7.0's build (`private/0.7.0-test/`): the setup
kept, the alarms armed by `MY_PACKAGE_REPLACED`, nothing set. Then each 0.8.0 feature's tablet
bullets once more on R8 (SW2's, SW3's, VS1's, D1's log line, SA3's if built), the smoke list,
and the regression's core paths (Set, the daily switch, a run by `run -f 1`, Live, ⛶ and ✓).
Screenshots for the user. **Done when:** PASS, or each FIX as a package for Monday night.
