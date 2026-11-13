# Turns as a plain rotation (0.7.0)

(User, 2026-11-11, after the FP6's day below: "this is very confusing … if I put this settings, I
expected the APOD to return again at 18:00". Planned the same day by reading and replaying, no
code. The decision is in [Decided](#decided); the rules below are a draft for the user to check,
and [Open](#open) holds the user's questions, asked one at a time with a recommendation. The
packages write their decisions into `CLAUDE.md` when they land; this file is the how.)

## Why

**The FP6, 11 Nov 2026** (APOD and NASA on both screens; read from `dumpsys batterystats
--history`, which keeps every job run and page open for about two weeks, `dumpsys wallpaper` and
the page's status line, then replayed through `:core`'s `dueWallpapers` with the real post times
and run times):

| Time | What happened | Why, by 0.6.0's rules |
|---|---|---|
| 06:16 | APOD's 11 Nov (NGC 4449) went on | "Change every" was 24 h: the day's one change, APOD's turn |
| 07:19 | The user set "every 6 h" | The 06:00 change still current: the picture stayed |
| 12:04 | NASA's 10 Nov (CHAPEA) went on | The page open made the 12:00 change: NASA's turn, CHAPEA new to the screen |
| 18:19 | Nothing changed; hourly checks from then | APOD's turn, but NGC 4449 already shown; NASA nothing new (US Veterans Day): rule 3, the screen waits |

The app did what `CLAUDE.md` says (rule 1, "the next source with a picture it hasn't shown",
2026-10-23). The user expected APOD at 18:00, and it took a phone's history to tell why it
didn't come: the rule depends on a hidden state (what this screen has shown), so a user can't
predict it from the settings. With two sources posting about once a day, two of a 6 h setup's
four change times usually find nothing new.

## Decided

(User, 2026-11-11.)
- **A plain rotation:** at each change time the screen goes to the next source and shows its
  newest picture, also one it has shown before. "Every 6 h" with APOD and NASA means APOD,
  NASA, APOD, NASA.
- **Repeats are accepted:** today's APOD twice a day with 6 h, and on weekends Friday's NASA
  and Earth pictures at each of their turns. Why: that's what the setting reads as; the
  "hasn't shown" rule (2026-10-23) avoided repeats but made the screen unpredictable.
- **Rule 2 stays** (the 3-hour window by the publish time, B15): APOD's turn at 06:00 starts
  with yesterday's APOD, and today's (06:05 in Zurich) replaces it at about 06:15.
- Rejected: keeping the rule and saying on the page why nothing changed (a status line such as
  "Waiting for a new picture": more text, and the behaviour still surprises).

## The rules (draft, for the user to check)

1. **At [06:00], and with several sources every [24/12/6/3 h] from then, a screen changes to
   the next of its sources (APOD → NASA → Earth) and shows that source's newest picture.**
2. **A newer picture of that source that comes out in the 3 hours after the change still
   replaces it.**

What 0.6.0's rule 3 did ("with several sources and nothing new, the picture stays until one
comes out") goes: a post that comes out later waits for its source's next turn, as one outside
the window does today. One sentence for the page or an ⓘ: "Every 6 h from 06:00 the screen
switches to the next source's newest picture."

**Unchanged:**
- **One source:** its newest picture by rule 2's publish time from each change at "New picture
  at"; the newest after a day without a check (B21).
- **Never back:** an older post of the source on the screen isn't set.
- **Equal columns change together** (the lock screen takes home's record and picture, B2).
- **Set, a pull, a forgotten picture and a new screen** put the current turn's source's newest
  picture on at once, whenever it was published.
- **A source with nothing to show** (nothing stored yet; APOD's video with "Skip it", or "Use
  my picture" without one) passes the turn to the next with a picture; none: the screen keeps
  its picture.
- **The video-day stand-in** fills APOD's turn (NASA's, Earth's or the user's own picture). It
  may now repeat: NASA's picture in APOD's turn and again in NASA's (B22's "not twice" goes;
  [Open](#open), 2).
- The change times, the offset, the alarms, the retries and the fallback job
  (`PictureTime.kt`, `DailyJob`).

## Open

The user's, one at a time, before RO1.

1. **Which source a change time gets.** (a) The next after the one on, as 0.6.0 does
   (`nextSource` after `Record.owner`): a change time missed with the phone off makes one
   change, not one each, so the order shifts (off from 11:00 to 19:00: NASA at 19:00, APOD only
   at 00:00). (b) **By the clock** (recommended): each change time has a number (the day's
   change times counted from "New picture at", plus the day's number since a fixed day × the
   change times a day), and the screen's sources take turns by it, number mod the sources. With
   two sources and 6 h or 12 h APOD is always the 06:00 and 18:00 one; with 24 h they take days
   in turn. A phone off, a Set or a pull never shifts it; one clock, as 2026-10-23 wanted ("by
   the clock, not by what's on the screen"). A source with nothing to show passes to the next
   with a picture (rule 1, as above). Counting in local days on the clock's hours keeps DST
   days right (`PictureTime`'s hours). Cost: the order starts wherever the count is, not with
   APOD at the first Set; a changed list re-maps the numbers.
2. **The stand-in shown twice** (B22). Recommended: drop it; repeats are accepted now, and the
   video notice loses its "skipped" case (`standInSkipped`).
3. **The page:** whether the setup needs the one sentence above in an ⓘ beside "Change every"
   (texts are the user's: a label or a fact on the page, explanations only in an ⓘ). Recommended:
   yes, one ⓘ, since the setting's effect is what confused.

## What changes (by reading, 2026-11-11)

- **`:core`, `Screens.kt`:** `dueWallpapers`' rule 1 picks the next source with a picture
  (or, with Open 1 (b), the clock's), not `Stored.isNew`; rule 3 and `waiting` go;
  `Stored.isNew` and `standInShown` go with Open 2; `videoDayKept` loses its B22 case, and
  `standInSkipped` goes. `Record.shown` (the notes) and `Record.owner` may no longer be read
  (with Open 1 (b) the owner is the clock's): left in the prefs, no migration, as 0.6.0 left
  `turnStartedAt`.
- **`:core`, `PictureTime.kt`:** `nextRun` and `nextAlarm` lose `waiting` (no hourly checks
  while a screen waits). Fewer checks a day (CLAUDE.md, Traffic).
- **`Store`:** `nextAlarm` stops passing `waiting`; `standInSkipped` goes.
- **The page:** the video notice's B22 texts ("This APOD is a video: wallpaper unchanged." for
  a stand-in shown before); the status line's "Video day, previous kept" follows `videoDayKept`.
- **`CLAUDE.md`:** Turns and the picture time (rule 1, rule 3, the stand-in), Next features'
  0.6.0 rules (marked as replaced), the video notice, Traffic.

## Tests and checks

- `ScreensTest`: the rotation with repeats (two and three sources, 6 h and 3 h), a source with
  nothing to show passing its turn, a weekend (Friday's NASA at its turns), a video day with each
  choice, and **the FP6's day of 11 Nov** (24 h until a Set at 07:19 to 6 h): APOD's NGC 4449
  at 18:00.
- `PictureTimeTest`: the whole days in the seven zones without the waiting checks.
- `RulesSimulationTest` (SIM): the invariants that rest on "hasn't shown" and on waiting
  replaced by the rotation's (each change shows the turn's source, or the next with a picture).
- On the tablet (rosie), a faked day with APOD and NASA on both screens, 6 h: APOD at 06:00 and
  18:00, NASA at 12:00 and 00:00, nothing set in between, `dumpsys alarm` with no hourly checks
  after the window.

## Packages

Each reads this file's text above first (`plan-0.7.0.md`, For every package); the queue rows are
`night.md`'s.

### [ ] RO1. `:core`: turns as a plain rotation (Opus, high)

After Open 1 and 2 are decided (the user writes the answers into Decided). The rules above as
decided; `dueWallpapers`' rule 1 and rule 3 as What changes says, `nextRun` and `nextAlarm` without
`waiting` (keep the hourly loop's code if SO1 needs it: `soon-0.7.0.md`, Seams).
- **Files:** `Screens.kt`, `PictureTime.kt`, `ScreensTest.kt`, `PictureTimeTest.kt`,
  `RulesSimulationTest.kt`; `Store.kt` only where a `:core` signature changed (compiles).
- **Tests:** Tests and checks above, the `:core` ones; the FP6's day of 11 Nov as a test.
- **Done when:** the tests pass and the FP6's day fails on the old code; `CLAUDE.md`'s Turns and
  the picture time states the rotation's rules (0.6.0's rule 1 and rule 3 replaced, the date and
  the why), Traffic without the waiting checks.

### [ ] RO2. `Store`, the page's texts, `CLAUDE.md` for the rotation (Opus, medium)

After RO1 and Open 3. `Store` stops passing `waiting`; `standInSkipped` and its video-notice texts
go with Open 2; the ⓘ beside "Change every" if Open 3 says so (its text a placeholder).
- **Files:** `Store.kt`, `MainActivity.kt`; `CLAUDE.md` (the video notice, the status lines, Next
  features' rotation entry: built); the skill's Where things are.
- **Tablet:** a faked video day with each stand-in choice (the notice's text, the status line);
  the ⓘ (a screenshot); the smoke list.
- **Done when:** built; no text names a stand-in "skipped" unless Open 2 kept it; the tablet passes.

### [ ] RO3. Tablet check: the rotation on a faked day (Opus, medium)

rosie's device package, on dev's debug build after RO2: Tests and checks above, the tablet item
(APOD and NASA on both screens, 6 h: APOD at 06:00 and 18:00, NASA at 12:00 and 00:00, nothing
set in between; `dumpsys alarm` without hourly checks after the window), each change by the
skill's "End a turn".
- **Done when:** the results written into this file under a `## RO3's results` heading and
  committed; PASS, or the BUG lines for the morning.

## Seams

- **Track O** (`on-time-0.7.0.md`): it changes the alarms' run, not `nextRun`'s times; O14's
  test of W's tablet day ("the lock screen waiting with turns") is rewritten without waiting
  once RO1 lands.
- **Track L** (`live-0.7.0.md`): "with turns each picture crosses within its own turn" holds; a
  picture shown twice a day pans twice.
- Before or after O1–O3 (the declutter every track waits for): RO1 touches only `Screens.kt`,
  `PictureTime.kt` and their tests.
