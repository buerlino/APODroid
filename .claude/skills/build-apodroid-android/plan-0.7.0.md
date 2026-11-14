# 0.7.0: the night shift's plan

(Written 2026-11-13, when 0.7.0's building started; user: "prepare everything for me to start the
night shift".) 0.7.0 is planned in one file per track; this file gathers them for the pipeline
(`scripts/pipeline.sh` takes the newest `plan-*.md`): the order across the tracks, the rules
every package keeps, and the packages without a track. The queue is `night.md`'s; its Spec
names a track file's section (`on-time-0.7.0.md O1`) or one of this file's (`plan B26`).
`CLAUDE.md` wins where a track file disagrees.

## The tracks

| Track | File | Night packages | By day, with the user |
|---|---|---|---|
| On time on any phone (O) | `on-time-0.7.0.md` | O1–O4, O10–O15 | O5 (probe), O6–O8 (rosie by hand), O9, O16, O17 (FP6) |
| Live, the day's pan (L) | `live-0.7.0.md` | L3–L8 | L0 (probe), L1 (rosie by hand), L2, L9, L10 (FP6) |
| Turns as a plain rotation (RO) | `rotation-0.7.0.md` | RO1–RO3 | Open 1–3 first |
| As soon as it's out (SO) | `soon-0.7.0.md` | SO1–SO3 | Open 1–4 first |
| Bright pictures darker (BR) | `bright-0.7.0.md` | BR1–BR3 | BR0 (the picking page), Open 1–5 first |
| The list's order (OR) | `order-0.7.0.md` | OR1–OR2 | — |
| Android 10–14 (E) | `emulator.md` | — | E0 first (the pipeline on an emulator), then E1–E4 |
| Leaner texts | `lean-ui-0.7.0.md` | — (its rest is in O13, O14, L6) | — |

## For every package

- **Read your track file's text before its `## Packages`** (the findings and the decisions, with
  their reasons) before building; the bundle carries only your package's section and this
  preamble. Its Seams section says what another track expects of your code.
- **The texts** (`CLAUDE.md`, the stack): a label or a fact, not a sentence; one fact per line;
  explanations only in an ⓘ or a dialog; Android's own labels word for word. A new or changed
  text is a placeholder until the user approves it from the tablet's screenshots: list it in the
  report.
- **Settings** (`CLAUDE.md`, How the user works): only those the track decided; none beyond.
- **One tablet:** the rosie checks run one at a time; the probes and the checks by hand run by
  day, never beside a night.
- **No permission** beyond O11's two (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`),
  no dependency, `:core` plain Kotlin.
- **0.6.0's rules stand** (Background update, Turns) until RO1, SO1 and OR1 change them as
  their files say.

## The order

A row whose After names `the user` waits for a step by day (the words in brackets say which);
the user takes the words out once it's done (night.md, Commands, 3).
1. **First, for every track:** O1 (the declutter), then O2, O3 and O4 (the bookkeeping review,
   whose drafted packages `O4a`, `O4b`, … the user queues in the morning; they run before O10, O11
   and L8; not `O4f<k>`, the pipeline's name for a fix package).
2. **Ready without the user:** O13 (after O3), L4 (after O2: one drawing function; BR2 then
   builds on `Drawing.kt`, as `bright-0.7.0.md` allows), B26.
3. **After the day steps:** O10 (O6), O11 (O7, O8), then O12 (if O7 asks), O14, O15; L3 (L1), L5,
   L6 (L2), L7, L8; RO1 (rotation's Open 1–2), RO2 (Open 3), RO3; SO1 after RO1 (the same
   functions: `dueWallpapers`, `nextRun`'s hourly loop; and soon's draft rules checked by the user),
   SO2 (soon's Open 1–3), SO3; BR1 (BR0, Open 1–5), BR2, BR3 (after L7's preview); OR1
   after RO3 and SO3, then OR2.
   O15 waits for O12 or for the user dropping O12's row (O7 didn't ask for it).
4. **Before the tag:** [Before the tag](#before-the-tag).

## Packages

### [ ] B26. APOD's title prefix with the day first (Sonnet, medium)

(`CLAUDE.md`, Open questions, 4.) The 29 Oct 2026 post's title was `APOD: 29 October 2026 &#8211;
The Horns of Venus`; every other post of the last 300 has the year first (`APOD: 2026 October 28
&#8211; …`, checked again 2026-11-13: 20 of 20 since 25 Oct, 29 Oct the only one). `Post.kt`'s
`titlePrefix` wants the year first, so the page showed the whole prefix that day and ★'s file
name carried it (`APOD_2026-10-29_APOD_29_October_2026_…`).
- **The fix:** `titlePrefix` also strips `APOD: <day> <Month> <year> – ` (the day optional, as
  3 Apr 2026's year-first title had none), the dash kinds and spacing as now.
- **Files:** `core/.../Post.kt`, `ApodTest.kt`.
- **Tests:** `ApodTest`'s `cleanTitle` cases: the 29 Oct title (with `&#8211;`), the day-first
  form with `–` and with `-` and no space, without the day; the year-first cases unchanged. A
  title that merely starts with a number after the prefix keeps it (`APOD: 2026 October 1 – 3
  Moons` → `3 Moons`).
- **Done when:** the tests pass and fail on the old regex; `CLAUDE.md`'s Open questions, 4
  removed and the Data source's title bullet names both orders.

### [ ] B27. A shared lock screen with nothing of its own shows home's new picture (Opus, medium)

(T1's SIM, seed 235, invariant 16; the user, 2026-11-14: fix (a), built the same night.) With Size Live and home
and lock on the one engine (`live.shared`), a lock screen whose column has nothing new to show (e.g. lock = APOD
alone, an APOD video day with "Skip it") follows home's hand-over and shows home's new picture, an unticked
source's. It should keep the picture it shows now (`CLAUDE.md`, The app, Wallpaper: Live, "A shared lock screen
follows home").
- **The fix:** in `Store.setDue`, just before home's hand-over to a shared engine, when the lock screen isn't
  due home's picture, set the lock screen's still from `live.jpg` (the picture the engine shows now), so the
  lock screen keeps it and leaves the engine; its record stays. A failed still set holds the hand-over as L6f1's
  rule does.
- **Files:** `Store.kt` (setDue, lockFollowsHome), `Live.kt` (a pure helper if the rule fits there),
  `RulesSimulationTest.kt`, `CLAUDE.md` (the lockFollowsHome paragraph: seed 235 no longer open).
- **Done when:** seed 235's test is un-ignored, out of `knownBugs`, and passes (it fails on the old code); the
  other seeds still pass; `CLAUDE.md` says what the lock screen does.

## Before the tag

### [ ] T1. SIM on the combined rules (Opus, high)

(User, 2026-11-14: everything queued.) `RulesSimulationTest` on the tree with the rotation (RO1),
as soon as it's out (SO1, the switch on and off at random), O4b's retry model, O10's run and OR1's order and anchor (invariants 11–13, `order-0.7.0.md`):
`SimPhone`'s rules and arming mirror the merged code; every invariant kept, rewritten where RO1 or
SO1 changed what it states (say which in the report); 240 seeds as 0.6.0's SIM, more if fast.
- **Files:** `RulesSimulationTest.kt` (test code only; a bug found is a `BUG:` line and a
  failing test marked `@Ignore` with its seed, not a fix).
- **Done when:** the simulation passes, or every failure is a `BUG:` line with its seed.

### [ ] R1. The release's texts (Sonnet, medium)

(User, 2026-11-14.) `app/build.gradle.kts` versionCode 11, versionName "0.7.0";
`fastlane/metadata/android/en-US/changelogs/11.txt` in 10.txt's form (one line a feature, the
features that merged: Live, the rotation, as soon as it's out, bright pictures darker, Keep
running with its two permissions, the expedited run, the detector, the pause hint per maker, the list's order (drag to sort); only
what `git log` shows merged), the fastlane `full_description.txt` and `short_description.txt`
where a feature changes what they say; `CLAUDE.md`'s Releases line for 0.7.0 left to the tag (T).
- **Done when:** the files written; `./gradlew :app:assembleRelease` builds; no tag, no push.

### [ ] T2. Final tablet pass on the combined tree (Opus, high)

(User, 2026-11-14; this file's first bullet below.) rosie, the tablet: dev's debug build, then R8
(signed as the skill says); the update from 0.6.0's build with a real setup (M2's way: nothing
changes but what 0.7.0 adds); then each track's tablet checks again, quickly: O15's list, L9's
list, RO3's faked day, SO3's afternoon, BR2's bright and dark pictures, OR2's checks (the drag, the order-only Set, "APOD stays", the faked night); the regression's APOD
mode and "More NASA pictures" lists; screenshots of every new text in one folder for the user.
- **Done when:** every check ran on R8 too; no `BUG:` line, or each drafted for the morning.


A skeleton (2026-11-13, from a review of the queue); each item becomes a package when the tracks
are in. Why now: O15 and L9 run before RO, SO and BR are merged, and RO1 and SO1 change the rules,
so nothing in the tracks checks the combined tree; 0.6.0's lesson was a real week after any change
to the job or the rules.
- **A final tablet pass** on the combined tree: R8, the update from 0.6.0, every track's checks.
- **SIM** on the combined rules (rotation, as soon as it's out).
- ~~A real week on the FP6~~ (user, 2026-11-14: after the tag, as the user uses it; a fix is 0.7.1;
  `CLAUDE.md`, 0.7.0's release gate).
- **The phone passes** O17 and L10, with soon's real afternoon (NASA on the lock screen) and
  bright's look on the FP6 (`bright-0.7.0.md`: "goes into L10 or O17").
- ~~E~~ moved to 0.8.0 (user, 2026-11-14). Was: Android 10–14 on the emulator (`CLAUDE.md`: postponed to 0.7.0; `emulator.md`'s
  "For 0.7.0" checks need an owner; `on-time-0.7.0.md`'s "E stays open" predates that).
- **The release:** versionCode 11, `changelogs/11.txt` (Keep running's two permissions), the
  fastlane texts and screenshots (0.6.0's R), the tag (T).
- **To plan or drop, the user's:** `edgesBlack`'s 97% ("revisit with Live", `CLAUDE.md`);
  TalkBack's rough spots (`SKILL.md`); `declutter.md`'s D22–D24, D26, G3–G6 (G6: the pipeline
  refusing a merged ID; josh sees only the log's last 80 RESULT lines, so after a few nights a
  done row could run again); a second picture on the same date (`CLAUDE.md`, Open questions, 3:
  "to look at in 0.7.0"; never seen in the real feeds).
- **Night defaults to confirm:** O13's Lenovo line is a placeholder until O8; O14's test of the
  waiting lock screen is rewritten by whichever of O14 and RO1 lands second.
