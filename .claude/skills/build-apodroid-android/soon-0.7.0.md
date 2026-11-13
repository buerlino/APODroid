# As soon as it's out (0.7.0)

(User, 2026-11-13, after the FP6's day below: "we have to give a setting for that, otherwise I
don't think I can decide, it depends what the user wants". Planned the same day by reading, no
code. The decision is in [Decided](#decided); [Open](#open) holds the user's questions, asked
one at a time with a recommendation. The packages write their decisions into `CLAUDE.md` when
they land; this file is the how.)

## Why

**The FP6, 12–13 Nov 2026** (APOD on home, NASA on lock, "New picture at" 06:00, Zurich; read
from `dumpsys batterystats --history`, `dumpsys wallpaper`, the page's status lines and NASA's
feed; logcat had rolled over):

| Time (CET) | What happened | Why, by 0.6.0's rules |
|---|---|---|
| 12 Nov 20:57 | Set: lock got NASA's "Heart in the Sky" (out 20:14) | Set puts the newest on at once |
| 13 Nov 06:16 | Home got 13 Nov's APOD; lock unchanged | NASA's newest published before 09:00 was still "Heart in the Sky" |
| 06:30–09:13 | The window's checks, nothing new | NASA posts 14:00–22:00 GMT |
| 10:12, 16:12 | The fallback job's runs | — |
| 17:41 | NASA posted "Outbursts from a Massive Star" | Published after 06:00 + 3 h: waits for 06:09 on 14 Nov |
| 19:24 | The page fetched and showed it, set nothing | A page open sets only what's due |

The app did what `CLAUDE.md` says (rule 2, the 3-hour window by the publish time, B15). But
NASA posts 15:00–23:00 in Central Europe, always after a 06:00–09:00 window, so a screen with
NASA alone is always a weekday behind there: the user saw "an old picture from yesterday" all
day while the page showed a newer one. APOD (06:05) and Earth (06:00) fall inside the window in
Europe; in Los Angeles APOD (21:05) is the same case, which 2026-10-23 wanted at 06:00. Which is
right depends on what the user wants, so a setting (the rule of 2026-10-23: users would really
disagree and no default serves both).

## Decided

(User, 2026-11-13.)
- **A switch below "New picture at" in Setup**, part of the draft (Set applies it), shown only
  while the daily switch is on, as "New picture at". Not an entry in the time's menu: the time
  still says when turns change and when the morning change comes.
- **Default off:** 0.6.0's behaviour stays, updaters see no change.
- **On:** the 3-hour limit goes. A newer picture of the source a screen shows goes on whenever
  a check sees it (rule 2 without the window); turns still change only at the change times.
- **Hourly checks while it's on**, all day (about 24 small requests a day per source in use:
  NASA's feed 49 KB, APOD's post 21 KB; the picture only when new). A new picture goes on within
  about an hour of its release.
- **In 0.7.0**, not a 0.6.1.

## The rules with the switch on (draft, for the user to check)

1. Unchanged: at the change times a screen changes (turns as 0.7.0's rotation says,
   `rotation-0.7.0.md`).
2. **A newer picture of the source on a screen goes on as soon as a check sees it**, whenever
   it came out; with turns only the turn's source's (another source's new picture waits for its
   turn).

Everything else as now: never back, equal columns together, Set, a pull, a forgotten picture
and a new screen, the stand-in on APOD's video day (its own post is "newer" when it comes out:
with "NASA instead" and the switch on, NASA's picture goes on in APOD's turn as soon as it's
out), B22's stand-in not twice, the retries. B21's mark (after a day offline) changes nothing
while it's on (every newer post goes on anyway).

## Open

The user's, one at a time, before SO1.

1. **The label** (a label, not a sentence; placeholder): "As soon as it's out". Recommended:
   that, approved from the tablet's screenshot.
2. **The ⓘ** (one sentence, explanations only in an ⓘ). Recommended: "A new picture goes on
   within the hour after it's out, not only at the change time."
3. **Setup's summary** (the closed row: "Home: APOD · Lock: NASA · 06:00"). Recommended: "· 06:00
   and new ones at once"? or nothing (the row stays short)? To pick from a screenshot.
4. **The pause hint's Optimized line** ("a new picture can come hours late") stays as it is
   (hourly alarms in Doze still wait for the network on Optimized). Recommended: unchanged.

## What changes (by reading, 2026-11-13)

- **`:core`, `Screens.kt`:** `dueWallpapers` takes the switch (`PictureTime` or a parameter);
  `inWindow` returns `true` for the owner's newer stored post while it's on (the limit
  dropped, not moved). The one-source path and the turns path both go through `inWindow`, so
  that's the whole rule change; B22's `standInShown` check stays.
- **`:core`, `PictureTime.kt`:** `nextRun` checks hourly until the next change time while the
  switch is on (what `waiting` does today; `waiting || soon`). If RO1 lands first and removes
  `waiting`, the hourly loop stays for the switch alone.
- **`Store`:** the pref (`soon`, boolean, default false) in `Setup` and `saveSetup`;
  `nextAlarm` and `setDue` pass it.
- **The page:** the switch in Setup under "New picture at" with its ⓘ, the draft, the
  highlight ("not set yet"), hidden with the daily switch off (and back to the stored value
  while hidden, as "New picture at"), Setup's summary (Open 3).
- **`CLAUDE.md`:** Settings (the switch), Turns and the picture time (rule 2 with the switch),
  Traffic (hourly checks while on), Next features.

## Tests and checks

- `ScreensTest`: one source, switch on: NASA's post at 17:41 goes on at the next run after it
  (the FP6's 13 Nov), off: waits for 06:00; with turns, on: the turn's source's newer post goes
  on, another source's waits for its turn; never back; an APOD video day with NASA instead.
- `PictureTimeTest`: `nextRun` with the switch on checks hourly through the day in the seven
  zones (a DST day too), off unchanged.
- `RulesSimulationTest` (SIM): seeds with the switch on; the invariant "a screen shows its
  source's newest stored picture after every run" (one source), "the turn's source's newest"
  (turns).
- On the tablet (rosie): APOD home, NASA lock, the switch on, a faked NASA post in the
  afternoon: set within the hour; `dumpsys alarm` with hourly alarms after 09:00; off: none.
- On the FP6 (the user): a real afternoon with NASA on the lock screen.

## Packages

- **SO1** (`:core`): the rule and `nextRun`, the tests above.
- **SO2** (`:app`, `CLAUDE.md`): `Store`, the switch in Setup, the texts (Open 1–3).
- **SO3** (tablet): the check above.

## Seams

- **RO1** (`rotation-0.7.0.md`) changes rule 1 and removes `waiting` in the same functions:
  SO1 after RO1 (or rebased on it); the hourly loop in `nextRun` stays for the switch.
- **Track O** (`on-time-0.7.0.md`): the alarms' expedited run carries the hourly checks too;
  more runs a day for Keep running's phones (no change to its design).
- **Track L** (`live-0.7.0.md`): a picture that goes on in the afternoon starts its pan where
  the clock is (the route by the time of day), not at the left edge.
