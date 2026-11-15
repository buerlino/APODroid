# Interaction maps

Every way a user (or Android) can drive the app, as decision trees with the expected outcome per
leaf, and a phone test list per tree. Made 2026-11-15 by four reading agents, run on the FP6 the
same day (113 cases). `CLAUDE.md` wins where a map disagrees.

| File | Covers |
|---|---|
| [live.md](live.md) | Set × Size [Centre \| Auto \| Live] (0.7.0's [Fill \| Live]) × Android's live wallpaper picker; outside the app (Android's own wallpaper settings) |
| [setup.md](setup.md) | Setup's controls, the draft and its highlight, Set's outcomes and toasts, the daily switch |
| [page.md](page.md) | The page outside Setup: title, swipe, caption, chip, ⛶, ★, pull, the boxes, status lines, rotation, Advanced |
| [background.md](background.md) | Alarms, job runs, the rules per screen, Live's runs, D, Keep running, the detector, Save every picture |
| [tester-rules.md](tester-rules.md) | What a tester agent may and may not do on the user's FP6, the start and end state, the report |
| [tester-rules-emulator.md](tester-rules-emulator.md) | The same for the Android 14 emulator (0.8.0's night pass) |

## Using them for a test pass

1. Bring the maps up to date first (below).
2. A tester agent per map: the map's test list, `tester-rules.md`, a report per case
   (PASS / FAIL / DIFFERENT / SKIPPED, with screenshots and log lines).
3. The phone unlocked before the agents start (a PIN-locked FP6 stops every case), and
   `svc power stayon usb` during the run, restored after.
4. A FAIL or a DIFFERENT becomes a fix plan, checked by another agent before it's built.

## Keeping it current

- **Each new feature** (user, 2026-11-15): an agent adds its branches to the map it belongs to
  (a new map for a new area): the user's paths, the expected outcome, the log lines, the cases
  for the phone. In a night package winston does it (night.md, Definition of done); in day work a
  fresh agent after the feature is built, reading the code, not the plan.
- A fix that changes a leaf's outcome updates that leaf.
- Before a test pass, an agent reads `git diff <the maps' base>..HEAD` and updates what changed:
  the leaves, the test lists, the line numbers, and the base below.
- Base: **3a4db15** (the maps' line numbers; leaves read at 7299474, live.md's SPLIT at 087856c).
  The SUSPECTs' outcomes since 7299474 are below.
- 2026-11-16, for 0.8.0's emulator pass: every file:line moved to 3a4db15; the fixed SUSPECTs'
  leaves and test rows give the fixed outcome (setup D17/D20/H8, P4/P17/P29/P30; page C5, C8, S4a,
  BP2, BH4, K's frame kind; live §0, N1, T04/T16/T22–T24/T31/T34/T36); new cases: live T38–T39,
  page §4 (31–35), background E8a and P18–P22; the engine's log `Live: engine <flags> destroyed`;
  T3f1 (the subject not noted by a page open or Set within the hour) pending: setup J3, background E8a.

## The SUSPECTs and FAILs of 15 Nov and what became of them

Fixed after 7299474 (CLAUDE.md, The FP6 tests; in the commit that added these maps; live S2 in
087856c, SPLIT):

| Map, id | Found | Now |
|---|---|---|
| live S1 (T08, T28) | Back from the picker saved when home was live already | nothing saved, nothing set |
| live S2 (T09; N7, K7, K15) | equal columns with home live never offered the picker again ("once per split") | every such Set asks "Live on the lock screen too?" (live Q1–Q8): "Not now" saves, the lock screen its own still; OK the picker; Back saves nothing |
| live S3 (T22) | "Lock screen" alone: Size Live saved, "Wallpaper set" | nothing saved, the lock screen put back, toast "Live goes on the home screen. Setup not saved." |
| live S4 (T16) | the engine on the lock screen alone while home is live, kept | the lock screen with a column gets its own still |
| live S5 (T34) | Fill + Where Home from a shared engine left it on the lock screen | one shared still in the same run (`sharedLockFollows`) |
| live S7 (T31) | " · Live" in the summary without a home column | only with a home column |
| live S9 (T04) | ⛶ followed the draft's Size, ✓ the stored one | ⛶ follows the stored setup |
| live, logs (T16) | the lock-only engine logged "engine home destroyed" | the same label as at its creation |
| setup S1, S2 (D17, D20; P4, P17) | a hidden mode's edits saved unmarked | the hidden mode's stored part saved |
| setup S3 (H8) | an abandoned "my picture" pick replaced the stored one | renamed only while "Use my picture" is chosen |
| page C8, K12 | rotation lost the open explanation and ⓘ dialogs, a closed ⓘ came back | page state in the saved state |
| page BP2 | one dismissed problem across sources | per source; the first not dismissed shows |
| page BH4 | the pause hint's label wasn't Android's | "Manage app if unused" (35+), "Pause app activity if unused" (33–34) |
| page S4a | ★'s toast lost across a rotation | the save's end in `runs` |
| background N1 | an arming could replace a run's retry pair | `ensure` under the job's lock |

Accepted (CLAUDE.md, The FP6 tests): live S6 (a hand-picked engine with
Fill draws Live's old copy), S8 (not tested), the ~70 ms of black after a process death; setup's
rotation during Set (P25); background G14 (a lost Live with nothing to show keeps a stale record),
J6 (a log line only).

Open, not run (the rules forbid deleting a gallery file): page S2a, ★ says "Already saved"
for a file deleted while the page stays resumed (☆ again after Home and reopen).

**Re-run on 15 Nov:** the tablet (debug, an R8 smoke) and the FP6 (the user's release-signed
fff43e7) passed every case run; not run on either: background N1 (a race). On the tablet
only: setup S2, S3, page BP2, BH4, S4a, live T33. Details: CLAUDE.md, Left for the tag.
