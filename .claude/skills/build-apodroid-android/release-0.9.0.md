# 0.9.0: the release plan (no date yet)

(User, 2026-11-16: "can you make a plan and add it to the 0.9.0 docs". What was moved to 0.9.0
before: Android 10–14, E (user, 2026-11-15); TP1 and the log lines (user, 2026-11-16).) Not
`plan-0.9.0.md`: `scripts/pipeline.sh` takes the newest `plan-*.md` as "plan", and 0.7.0's done
queue rows (`plan R1`, …) must keep resolving. 0.9.0's rows name their track files, as 0.8.0's
did. No queue rows are in `night.md` yet: they go in when the user says start.

## What's in 0.9.0

| Track | What | File | State |
|---|---|---|---|
| S, EF1 | Sharp pictures: a limit on Fill's enlargement; Earth's picture link | `sharp-0.9.0.md` | planned 2026-11-16; its questions open there |
| E, ER | Android 10–14 on the emulators | `emulator.md` | **moved to 0.10.0** (user, 2026-11-17) |
| TP1 | The detector's sign on Android 11–14 | `release-0.8.0.md`, TP1 | the idea approved (user, 2026-11-17: "everything except the emulation step"); built (merged with tablet SKIP), its Android 14 check in T4, 11–13 with E in 0.10.0 |
| SZ1 | An ⓘ for Size (Auto's turn on the page) | this file | approved with its text (user, 2026-11-17) |
| EF1f1, UI1f1 | Earth's swap from a thumbnail; the detector's Keep running behind the Set bar | `sharp-0.9.0.md`, this file | from Opus pass 50 (2026-11-17) |
| LG | The three log lines of Open question 4 | this file | ready |
| R4, T4 | The release's texts; the final pass on the combined tree | this file | after the rest |

## The order

(User, 2026-11-17: "can we do everything except the emulation step, move that to 0.10.0".)
1. `sharp-0.9.0.md`'s packages in its order (EF1, SH0, SH1, SH2a, SH2b, SH2c, SH4).
2. **LG**, **EF1f1**, **UI1f1**, **TP1** (its device check on `emulator-5564`, Android 14 only).
3. **R4**, **T4** (on `emulator-5564`: the tablet isn't attached), then the FP6 with the user's
   release-signed build, then the tag.
4. **E** and **ER** (Android 10–14 on the emulators): **0.10.0**. So 0.9.0 ships Android 10–13
   untested on a device, as 0.6.0 to 0.8.0 did, and 14 only by the emulator's checks.

## Rules for every 0.9.0 package

As 0.8.0's (`release-0.8.0.md`, "Rules for every 0.8.0 package"): one package, one branch,
`CLAUDE.md` updated in the same package, its map updated (Definition of done), no new
dependency or permission, texts as placeholders until the user approves them. A device check
runs on the tablet when it's free, else on `emulator-5564` (Android 14), never on the FP6 by
night.

### [ ] LG. Three log lines (Sonnet, medium)

From `CLAUDE.md`, Open question 4 (user, 2026-11-16: for 0.9.0). Log lines only; nothing set,
saved or shown changes.
- A whole picture's set line doesn't end ", auto <p>" (it has no frame; nor ", auto 0.50" for
  the fallback's middle): only a Fill frame Auto placed by a subject notes it.
- `Refresh: <source> <date> same post` only when the stored post is today's; otherwise
  `Refresh: <source> <date> stored, nothing newer` (NASA's and Earth's weekends).
- Turning the daily switch on in a fresh process doesn't log `Detector: the alarms found gone`
  (no box followed it; the switch's own arming explains it, as a boot does).
- `CLAUDE.md`: Open question 4 closed. Tablet or emulator: the three lines in `logcat` after a
  whole-picture Set, a page open on a Saturday (faked clock) and the switch on after `am kill`.

### [ ] ER. `emulator.md` for the 0.10.0 tree (moved to 0.10.0, user, 2026-11-17; Opus, high)

`emulator.md` was written for 0.6.0 (W's candidate, the update from 0.5.0, bugs into
`plan-0.6.0.md`). Bring it to the 0.9.0 tree, without running the checks:
- E0: what 0.8.0's night already did (`APODROID_TABLET=emulator-5564 scripts/night.sh`, rosie on
  the Android 14 emulator) and what's still missing for the other levels (`apodroid-29`, `-31`,
  `-33`).
- E1a–E4 install dev's build and test the update from 0.8.0 (not 0.5.0); a `BUG:` becomes a
  `### [ ]` package in this file.
- Fold in its "For 0.7.0" lines (Keep running's notification on 10–12, the expedited run on 12+,
  the pause hint per maker, the detector, Live's lock screen on 10–13, the `getWallpaperId` path
  below 14, the colours) and 0.8.0's: the sideways and the darkened set through `setBitmap` and
  the system's crop on 10–13, Auto's subject decoded on 10's `ImageDecoder` (EXIF), the video
  still's `MediaMetadataRetriever` on 10; 0.9.0's sharp-picture set and TP1 on 11–14.
- The queue rows for `night.md`, ready for the user's start.

### [ ] UI1f1. The detector's Keep running button lands behind the Set bar (Sonnet, medium)

From Opus pass 50 (2026-11-17), a 0.8.0 bug: `MainActivity.kt` around line 2266, the detector's
Keep running button sets `showKeepRunning`, and a `LaunchedEffect` then calls
`keepRow.bringIntoView()`. Keep running is the last row of the scroll before UI1's bar spacer.
With Setup open, `bringIntoView` scrolls only far enough to put the row at the bottom edge of the
scroll viewport, and since UI1 that edge is covered by `SetBar`, so the switch lands behind the
bar. Fix it by asking for a rect that reaches the bar's height below the row while Setup is open
(`keepRow.bringIntoView` with a `Rect` from 0,0 to the row's size plus `barHeight`), so the row
stops above the bar; with Setup closed, leave it as it is. No new text. Device (emulator): with
the detector's box faked and Setup open, in one column and in two panes (landscape), tap Keep
running; Advanced opens and the switch shows fully above the bar.

### [ ] TP1. The detector's sign on Android 11–14 (Opus, medium)

`release-0.8.0.md`'s TP1, its idea approved (user, 2026-11-17): on API 30–34 a
`getHistoricalProcessExitReasons` entry with `REASON_USER_REQUESTED` newer than the last arming
counts as "the alarms gone" (beside the `PendingIntent`s' `FLAG_NO_CREATE` check); 35+ and 29 as
now. Read that section and O14 in `CLAUDE.md` (The detector) first. `:core` holds the decision
(tested: the record's time against the arming's, the API levels); `DailyJob.checkAlarms` reads
the records. `CLAUDE.md`'s "Android 10–14's gap" says what's fixed and that 11–13 wait for E
(0.10.0). The map `background.md` (the detector's leaves). Device (emulator, Android 14): `am
force-stop`, a change time faked past, then a page open, ten times: the box each time
(`Detector: the alarms found gone` in the log).

### [ ] SZ1. An ⓘ for Size (Sonnet, medium)

(User, 2026-11-17, after a Sonnet review of the Size modes: Centre, Auto and Live each have their
reason, but Auto's turn shows nowhere on the page, so one APOD turned and a similar one upright, or
NASA's never turned, looks like a bug.) A small ⓘ after the heading "Size", as "Change every"'s
(`ChangeEveryHeading`: the heading, the highlight unchanged, then "ⓘ" in `onSurfaceVariant`, its
screen-reader name "About Size", `openInfo`, so a rotation keeps the dialog), with an `AlertDialog`
and OK. Its text, **approved by the user (2026-11-17), word for word**, one line each:

```
Centre: the picture's middle.
Auto: the frame on the subject; a wide APOD goes on sideways unless its text says it has a top.
Live: a wide picture moves across the home screen over the day.
⛶ changes one picture.
```

Nothing else changes. `CLAUDE.md` (The app, Settings: Size; the list of ⓘs under "No help texts"),
the map (`interactions/setup.md`: the ⓘ opens, closes, survives a rotation). Device (emulator):
Setup open, tap the ⓘ beside Size: the dialog with the four lines; rotate: it stays; OK closes it;
TalkBack names it "About Size".

### [ ] R4. The release's texts (Sonnet, medium)

versionCode 13, versionName 0.9.0, `changelogs/13.txt` (Keep pictures sharp, Auto turning sky
pictures by NASA's text, "Wide pictures sideways" gone into Auto, the original downloaded when it's
smaller than the screen, Earth's link, the detector on 11–14) and the store description where a
feature changed it. `CLAUDE.md`: E (Android 10–14 on the emulators) moves to 0.10.0 (user,
2026-11-17) wherever it says 0.9.0 (Setup and distribution, Next features). All texts for the
user's approval. **Approved** (user, 2026-11-17): `changelogs/13.txt` and the store description as
written (c997e5b); the line "The stopped-app note now works on Android 11–14 too" stays if T4's
check of TP1 passes, else it becomes "should now notice" before the tag.

### [ ] T4. The final pass on the combined tree (rosie; Opus, high)

As 0.8.0's T3, on `emulator-5564` (Android 14; the tablet when attached): the maps' cases touched
since 0.8.0, an R8 build installed over v0.8.0's (the update keeps the setup; a stored Earth
thumbnail and an APOD rendition handled as `sharp-0.9.0.md` says), the smoke list.
Also the device bullets of LG, EF1f1, UI1f1 and TP1 (Opus pass 52: the night merged them with
tablet SKIP, none run on a device): LG's three `logcat` lines, EF1f1's faked `earth.thumbFallback`
swap (the screens set again with the real picture, at its subject), UI1f1's Keep running above the
Set bar in one column and two panes, TP1's ten force stops with the process alive (the exit-record
line each time; a Recents swipe and `am kill` none); SZ1's ⓘ (the four lines, a rotation, TalkBack).
