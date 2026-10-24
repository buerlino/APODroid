# Night shift

Agents work through the plan's packages while the user sleeps (user, 2026-10-19). Copied from pascaline's
pipeline (`scripts/night.sh`, `scripts/pipeline.sh` there) and cut down to this repo. Each session is fresh and no
bigger than its one task, so the usage window lasts. `CLAUDE.md` wins where this file disagrees. The scripts read
this file's sections (the queue, josh's commands, each agent's rules), so each rule is here once.

## How it works

- `scripts/night.sh` loops: it asks **josh** for the next command and runs it. josh is one Sonnet call per
  decision, with no tools, run outside the repo so `CLAUDE.md` isn't loaded; the queue, the results so far and the
  log's tail come on stdin. That's about 1 k tokens of overhead per decision instead of about 25 k for a session in
  the repo (measured 2026-10-19; pascaline's henri reads the brief himself).
- `scripts/pipeline.sh <ID>` runs one package end to end:
  1. **Bundle:** the package's sections of the plan (and of `features.md`), the plan's decisions and layout, and
     this file's rules for winston, the report and the definition of done (`.coord/runs/<n>-<id>/spec.md`).
  2. **winston** (the row's model and effort) in his own worktree `.claude/worktrees/wp-<n>-<id>`, on branch
     `wp/<id>` from `dev`, the bundle on stdin, no Agent tool. He commits and ends with the report commit. A
     device package (Agent rosie, such as M0) has **rosie** as its worker: she runs it on the tablet and commits
     the results; anything but PASS leaves it unmerged, since what follows rests on it.
  3. **The check:** the worktree is clean, then `./gradlew :core:test :app:lintRelease :app:assembleRelease` (the
     plan's build check; 2½ min from cold), under the host-cpu lock.
  4. **susanna's audit:** the diff against `dev`, the criteria and winston's report on stdin; Read, Grep and Glob
     only. Opus when winston ran Opus or the diff touches `core/src/main/`, `Store.kt` or `DailyJob.kt`, else Sonnet.
  5. **rosie on the tablet** (rows with Tablet yes, while the tablet is attached): the debug build of the audited
     commit, the package's tablet checks. PASS merges. FIX goes back to winston. STOP (Android does something the
     plan didn't expect) leaves the branch unmerged for the user. NOT RUN (no tablet, locked) merges and puts the
     checks on the morning list.
  6. A failed check, or a FIX from susanna or rosie, is a **fix round** by a fresh winston, fed only the failing
     part. At most two rounds; after that the branch stays unmerged.
  7. **Merge:** the audited commit into `dev` (`--no-ff`, in the dev worktree `.claude/worktrees/dev`), only while
     the branch still points at it. Then the worktree and the branch go.
- `scripts/pipeline.sh pass`: the **Opus pass**, susanna on Opus over the packages since the last pass, after
  every three finished packages and once at the end. Her FIX lines become fix packages; her QUEUE line reorders
  the queue.
- `scripts/pipeline.sh regression`: **rosie tests the whole app** on dev's debug build, once the night's last code
  is merged (after the last pass's fixes, and when night.sh closes): [The regression](#the-regression). Nothing to
  test (no merge since the last one) or no tablet: it says so and ends. Its bugs aren't fixed that night: the
  report drafts each as a `B<n>` package, which the morning commits for the next night (user, 2026-10-19).
- `scripts/pipeline.sh report`: the night's report for the user, `.coord/reports/<date>-night.md`.
- Nothing is pushed and master isn't touched: `dev` is master plus the night's merges. The user fast-forwards
  master after the morning review ([The morning](#the-morning)).

## The team

| Agent | Does | Model | Tools |
|---|---|---|---|
| josh | picks the next command | Sonnet, medium | none (all on stdin) |
| winston | builds a package; the fix rounds | the queue row's (the plan's) | all but Agent |
| susanna | audits a diff; the Opus pass; the report | audit Opus or Sonnet; pass Opus; report Sonnet | audit Read, Grep, Glob; else all but Agent |
| rosie | the tablet checks; a device package (M0); the regression | checks and regression Sonnet, medium; a package the row's | checks and regression all but Agent and Edit; a package all but Agent |

The agent files are `.claude/agents/<name>.md`. None of them starts another agent; at night only the pipeline
starts agents. By day each can be started by hand (`claude --agent <name>`). Every pipeline session gets
`scripts/agent-settings.json` (`./gradlew` and `adb -s HA24BZ2Y` allowed, `git push` and adb to the FP6 denied)
and no MCP servers.

Why these four (user, 2026-10-19): one worker, because the code is small (2.7 k lines) and the packages build on
each other; a reviewer who didn't write the code; the tablet at night, so fewer checks are left for the morning.
pascaline's six agents (three workers by area, an advisor, a verifier, the coordinator) weren't copied.

## Rules for a night

1. One pipeline run at a time.
2. josh decides nothing this file doesn't settle; everything else goes to the next Opus pass, whose reply is
   followed as written. Real choices go to the user as QUESTION lines, collected in the report.
3. Nobody asks the user at night. Where the plan leaves a choice, winston takes the simplest one that fits
   `CLAUDE.md` and lists it in his report under Night defaults; the morning confirms or changes it. Texts are the
   plan's placeholders; the user approves them on the device.
4. Devices: only rosie, only the tablet, only our app. Never the FP6.
5. Git: winston commits only on his `wp/*` branch, the pipeline merges only into `dev`, and susanna commits docs
   only on `dev`, in an Opus pass. Never master, never push, never force, `git branch -d` only. Starting the night
   is the user's go for these commits (2026-10-19).
6. `.coord/log.md` is the state: a RESULT line (MERGED or NOT MERGED) ends a step; a LIMIT doesn't. A new night
   resumes from the log.
7. Usage limit: the pipeline exits 75; night.sh waits 30 min and retries, at most six times, then stops. Run
   `scripts/night.sh` again to resume.
8. A step without a RESULT line (the pipeline died): run it once more; the second time it is logged SKIPPED and
   josh goes on. A worker who made no commits: NOT MERGED with the worktree kept; the next run of that package
   resumes the branch.
9. The end: the queue, the last pass and its FIX packages, the regression, the report, then SHIFT DONE. With
   `--until HH:MM` (or at the step cap minus 3) night.sh closes by itself: the last pass if a package finished since
   the last one, the regression, then the report. The closing takes up to about 3 h (the regression up to 2 h), so
   set `--until` that much before the morning.

## The queue

Tonight's (user, 2026-10-19): M1 to M4 and F2 of `plan-0.6.0.md`, with code, tests and the tablet checks, after
M0, the Android check the plan's second review put first (M1's grouping rests on it). The texts wait for the
user. F1, F3, F4 and F5 each start with a question for the user, so they stay for the day (answered 2026-10-20 and
built by day; every row below ran in the night of 2026-10-20 and is merged). "Model, effort" is the
plan's. Spec names the sections the bundle takes: `plan <key>` is the plan's `### [ ] <key>.`, `features <key>`
is `features.md`'s. Without the tablet M0 can't run, so M1 to M4 wait and only F2 runs.

The next night (user, 2026-10-20): **V1–V6**, the plan's tablet pass, rosie as each row's worker
(a device package, as M0; Tablet no, since she is the check). They need master's final tree: F3,
F1f1, F5f1 and C's text changes committed by day first (night.sh refuses an uncommitted plan and
brings `dev` up to master). The rows before them have MERGED lines in the log, so josh passes
them over; they stay because `scripts/pipeline.test.sh` resolves M0, M1 and F2. A V row that
isn't PASS stays unmerged with its results on its branch, and the rows after it still run (none
rests on another). The regression after them reruns dev's debug build; let it run.

Then (user, 2026-10-20 afternoon): **S1–S2**, a set by hand that changes the settings (the
plan's decisions). S3 dropped the same evening: **S4–S6**, settings as a draft that Set applies,
the setup and advanced areas, the title dropdown and swipe (the plan's "Set applies the
setup"). S2 was stopped by hand during its tablet check: its review run (`scripts/pipeline.sh
S2 --no-worker --tablet no`, as S4 and S5 replace what that check tests) merges it before S4
can start. S4, S4f1, V4 and the night's rows before them are merged (the night of 2026-10-20).

The next night (user, 2026-10-20 evening; the plan, Remaining 3b): **B1, S4f2, S4f3, S5, F6**; the tablet is free by
night (pascaline has it by day). **S6** waits for the user's look at S5 (its After names `the user`); V3, V5 and
V6 wait for S6, as they test the final tree. **Only APODroid's nights use the tablet** (user, 2026-10-20
evening; pascaline's nights leave HA24BZ2Y alone, the user tells them).

The night after (user, 2026-10-20 night, from that night's report; the plan, Remaining 3c): **B2, B4, B5, B6**, the
fixes the user decided after it (B3 was done by day), then **S6** (S5 approved, so it no longer waits for the user),
then the complete test on the final tree: **V3, V5, V6, V8**, and the regression. V1, V2 and V4 are merged and pass
over. V5 (the update from 0.5.0) has no After: it doesn't rest on S6, so it runs even if S6 stays unmerged.

The next night (user, 2026-10-21 morning, from that night's report; the plan, Remaining 3d): every row above is
merged except V4 (merged the night before; V8 repeated it) and V7 (by hand, not a queue row). **B10, B7, B8, B9**,
the fixes the user decided after it (B10: "Wi-Fi only" removed; B7 after it, as both change `DailyJob.kt`), then
the regression. B10's tablet check tidies the tablet first (the old test pictures, one Set). V7 waits for a night
of its own after them (the plan, V7).

By day (user, 2026-10-22, from that night's report; the plan, Remaining 3e): **B12, B11** with `scripts/pipeline.sh`,
one at a time. The night after: V7 alone, by hand (not a queue row).
Changed (user, 2026-10-22 evening; the plan, Remaining 4c): that night is **J0** by hand instead (the Android check for
the picture time, rosie, two sessions as V7's, the tablet on battery); V7 waits for J1–J3.

The same evening (user, 2026-10-21, 22:45: "the app brute test now, then V7"; the plan, Remaining 4a): **V9, V10**, the
brute test (rosie as each row's worker, as V3–V8), then **CR** (the code review, winston, no device), **B13** and
**B14** (their bugs fixed: clear bugs only, decisions as questions), then the regression (user, 22:50: as much as
fits in about 8 hours). B13 has no After, so it runs even when V9 or V10 stays unmerged (a FIX keeps a V row
unmerged; B13 reads its `BUG:` lines from the V rows' commit messages, its section says how). B14 waits for the user (22:55: the
tablet must be free by 08:00; B14 by day with `scripts/pipeline.sh B14`, after the user has read CR's findings). V7 moves to the next night.
The user read CR's findings and set B14's scope (2026-10-22 morning; the plan, B14 and Remaining 4b): B14 by day, then V7
alone the night after, by hand.

Tonight (user, 2026-10-23 night; the plan, Remaining 4e): **J1, A1, J2, A3, A4, J3**, the picture time (three rules
by the clock, one clock, the alarms retrying) and the whole picture, then the regression. J1 and A1 are `:core` only
(no tablet). J2 waits for J1; A3 for A1 only and A4 for A3 only, so the picture work lands even if J2 needs another
night (whichever of J2 and A3 comes second rebases on the other's `Store`); J3 waits for J2 and runs last (it and A4
both change the settings in `MainActivity.kt`). J2 also removes the job's note and retry rules (S4f3, B5, B7, B12);
its tablet check turns `auto_time` off for its faked clock and back on at the end (the plan, J2). J3's and A4's
texts are placeholders the user approves from the screenshots in the morning. The night is tight: J3 and the
regression may slip to Saturday. V7 follows on the night of 25 Oct, by hand.

The next night (user, 2026-10-24; the plan, Remaining 4f): every row above is merged. **B15, B16**, from two reviews of
`dev` by reading (the window by publish time, the stand-in, the video notice; the job's re-arming, one run at a time,
a stale pending run), then the regression. B16 waits for B15 (both change `PictureTime.kt` and `Store.kt`). Both
tablet checks fake the clock with `auto_time` off and back on at the end (the plan, J2). V7 follows on the night of
25 Oct, by hand.

The night after (user, 2026-10-24 evening; the plan, Remaining 4g, the release gate): right after the shift of 24 Oct,
a second one. **B17** (the APOD-mode video notice), **CR2** (a code review by reading of everything since 910bfeb),
**B18** (CR2's clear bugs, at most six; decisions are QUESTION lines: code freeze), then the final tablet pass, riskiest
first: **V11a** (the system against the new job), **V11d** (R8 and the update from 0.5.0 without opening the page),
**V11b** (the rules on a faked clock), **V11c** (the new UI), rosie as each row's worker, then the regression. The V11
rows have no After: they run after B18 also when it stays unmerged (they say which tree they tested). What doesn't fit
runs on Sunday by day. V7 follows on the night of 25 Oct, by hand.

| Step | ID | What | Agent | Model, effort | Tablet | After | Spec |
|---|---|---|---|---|---|---|---|
| 1 | M0 | Android check: home and lock set separately | rosie | opus, medium | no | — | plan M0 |
| 2 | M1 | `:core`: screens, columns, the stand-in and the hold | winston | opus, high | no | M0 | plan M1 |
| 3 | M2 | `Store`: mode, grid, a record per screen, migration, fetch rules | winston | opus, high | yes | M1 | plan M2 |
| 4 | M3 | Settings: the mode switch, the grid, the dropdown, the set menu | winston | opus, high | yes | M2 | plan M3 |
| 5 | M4 | The page: three tabs, chip, status lines, video notice, problem box | winston | opus, medium | yes | M3 | plan M4 |
| 6 | F2 | Wi-Fi only | winston | sonnet, medium | yes | — | plan F2 + features 4 |
| 7 | V1 | Tablet pass: F1, F3 and F1f1 | rosie | opus, high | no | — | plan V + plan V1 |
| 8 | V2 | Tablet pass: F4, F5 and F5f1 | rosie | opus, high | no | — | plan V + plan V2 |
| 9 | S1 | `:core` and `Store`: a set by hand changes the settings, no holds | winston | opus, high | yes | — | plan S1 |
| 10 | S2 | Settings: the list instead of the grid, the order, the daily switch off | winston | opus, medium | yes | S1 | plan S2 |
| 11 | S4 | Set applies the setup: the draft, `Store.saveSetup`, the old set paths gone | winston | opus, high | yes | S2 | plan S4 |
| 12 | B1 | The status line after Set with "Skip it" on a video day | winston | sonnet, medium | yes | S4 | plan B1 |
| 13 | S4f2 | The daily switch applies at once, both ways | winston | opus, medium | yes | B1 | plan S4f2 |
| 14 | S4f3 | Set doesn't stop a running job | winston | opus, medium | yes | S4f2 | plan S4f3 |
| 15 | S5 | Settings: Setup and Advanced settings, the highlight, "24 h", "More NASA pictures" | winston | opus, medium | yes | S4f3 | plan S5 |
| 16 | F6 | A size limit for the job's saves | winston | opus, medium | yes | S5 | plan F6 |
| 17 | B2 | Equal columns change together | winston | opus, medium | yes | — | plan B2 |
| 18 | B4 | The draft after a second start | winston | opus, medium | yes | S5 | plan B4 |
| 19 | B5 | A failed run keeps its retry | winston | opus, medium | yes | S4f3 | plan B5 |
| 20 | B6 | Quick taps on the daily switch set once | winston | opus, medium | yes | S4f2 | plan B6 |
| 21 | S6 | The picture side: the title dropdown, swipe between sources, no Share | winston | opus, high | yes | S5 | plan S6 |
| 22 | V3 | Tablet pass: M2–M4 again on the final tree | rosie | opus, high | no | S6 | plan V + plan V3 |
| 23 | V4 | Tablet pass: what the night of 2026-10-20 didn't check | rosie | opus, high | no | — | plan V + plan V4 |
| 24 | V5 | Tablet pass: the update from 0.5.0, debug builds | rosie | opus, high | no | — | plan V + plan V5 |
| 25 | V6 | Tablet pass: R8 builds | rosie | opus, high | no | S6 | plan V + plan V6 |
| 26 | V8 | Tablet pass: V1, V2 and V4 again where S4–S6 changed them | rosie | opus, high | no | S6 | plan V + plan V8 |
| 27 | B10 | Remove "Wi-Fi only" | winston | sonnet, medium | yes | — | plan B10 |
| 28 | B7 | The job keeps its retry: a stop during the saves, a Set after a failed run | winston | opus, medium | yes | B10 | plan B7 |
| 29 | B8 | The draft's picture survives a second page | winston | opus, medium | yes | B4 | plan B8 |
| 30 | B9 | A no-break space before ▾ and ▸ | winston | sonnet, medium | yes | S6 | plan B9 |
| 31 | B11 | A Set from a second page keeps the stored "my picture" | winston | opus, medium | yes | B8 | plan B11 |
| 32 | B12 | Only the first retry is waited for | winston | opus, medium | no | B7 | plan B12 |
| 33 | V9 | Brute test 1: what the system does to the app | rosie | opus, high | no | — | plan V + plan V9 |
| 34 | V10 | Brute test 2: what a user does to the app | rosie | opus, high | no | — | plan V + plan V10 |
| 35 | CR | Code review of the whole app, by reading | winston | opus, high | no | — | plan CR |
| 36 | B13 | The brute test's bugs | winston | opus, high | yes | — | plan B13 |
| 37 | B14 | The code review's bugs | winston | opus, high | yes | CR | plan B14 |
| 38 | J1 | `:core`: the picture time and the three rules | winston | opus, high | no | — | plan J1 |
| 39 | A1 | `:core`: the background, black or blurred | winston | opus, medium | no | — | plan A1 |
| 40 | J2 | `Store` and `DailyJob`: alarms at the change times, the job simplified | winston | opus, high | yes | J1 | plan J2 |
| 41 | A3 | `Store`: the whole picture per post, on black or blurred | winston | opus, medium | yes | A1 | plan A3 |
| 42 | A4 | ⛶'s Whole picture toggle; Size goes | winston | opus, medium | yes | A3 | plan A4 |
| 43 | J3 | Settings: New picture at, "More NASA pictures" into Setup, the pause hint | winston | opus, medium | yes | J2 | plan J3 |
| 44 | B15 | The window by publish time, the 09:00 check, the stand-in shown twice, the video notice | winston | opus, high | yes | — | plan B15 |
| 45 | B16 | The job: a passed check kept on re-arming, one run at a time, a stale pending run | winston | opus, high | yes | B15 | plan B16 |
| 46 | B17 | The APOD-mode video notice follows `standInReady` | winston | opus, medium | yes | — | plan B17 |
| 47 | CR2 | Code review of what changed since 910bfeb, read as a whole | winston | opus, high | no | — | plan CR2 |
| 48 | B18 | CR2's bugs | winston | opus, high | yes | CR2 | plan B18 |
| 49 | V11a | Final pass 1: what the system does to the new job | rosie | opus, high | no | — | plan V + plan V11a |
| 50 | V11d | Final pass 2: R8 and the update from 0.5.0 without opening the page | rosie | opus, high | no | — | plan V + plan V11d |
| 51 | V11b | Final pass 3: the rules on a faked clock | rosie | opus, high | no | — | plan V + plan V11b |
| 52 | V11c | Final pass 4: what a user does to the new UI | rosie | opus, high | no | — | plan V + plan V11c |

An Opus pass follows every three finished packages (MERGED or NOT MERGED; fix packages count), and the last one
comes when no queue step can run.

## Commands josh replies with

The first that applies:
Here "the last Opus pass" is the last one of **this night** (after its `## <date> night` header); an earlier
night's pass, its FIX lines and its QUEUE line count for nothing (night.sh shows only this night's).
1. The last Opus pass has a FIX line whose ID has no RESULT line yet: that FIX, in the pass's order, as
   `scripts/pipeline.sh <ID> --model <m> --effort <e> --tablet <yes|no> --branch <b> "<prompt>"`, with the FIX
   line's values and its prompt verbatim; leave out `--branch` when the line says `new`.
2. Three packages have a RESULT line since the last pass (after the last `RESULT pass <n>: done`):
   `scripts/pipeline.sh pass`.
3. The next queue step without a RESULT line whose After packages are merged (the package, or a fix of it
   `<ID>f<k>`, has a MERGED line), in the order of the last pass's QUEUE line, else the table's:
   `scripts/pipeline.sh <ID>`. A step whose After is NOT MERGED is passed over tonight, and so is one whose
   After names `the user` (it waits for the user's look in the morning; the user takes the words out).
4. No queue step can run, and no pass came after the last queue step's RESULT line: `scripts/pipeline.sh pass`
   (the last pass; its FIX lines then run by rule 1).
5. After that, when no `RESULT regression` line came after the last package's RESULT line:
   `scripts/pipeline.sh regression`.
6. After that: `scripts/pipeline.sh report`; once the report has its RESULT line, `SHIFT DONE`.

A RESULT line with LIMIT or FAILED: run the same command again. SKIPPED or NOT STARTED: never again tonight. A
regression's PASS, FIX, STOP, NOT RUN or NOTHING ends it for tonight: its bugs wait for the morning.
Reply with one command and nothing else.

## For winston

- You run headless and nobody answers. Where the plan leaves a choice, take the simplest that fits `CLAUDE.md` and
  the plan, list it under Night defaults with the alternative, and go on. What only the user can decide (a new
  dependency or permission, a change to a decision in `CLAUDE.md`, anything hard to undo): don't build it; put it
  under Needs the user and build the rest.
- The package's device bullets are rosie's: never run adb. Say in your report what she should look at beyond them.
- Texts: the plan's placeholders. List every new or changed user-visible text in your report.
- Stay in the package: its files, their tests and the docs it names. Write its decisions into `CLAUDE.md` where its
  "Done when" says (the source of truth), and into the skill's "Where things are" when files or functions change.
  Leave the plan's checkbox `[ ]`: the user ticks it after the morning review.
- Tests: new `:core` logic gets tests; a fixed bug gets a regression test that fails on the old code.
- Small commits, each with your model's trailer (`Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`, or
  `Claude Sonnet 5.5`). Leave the worktree clean.
- While you work: `./gradlew :core:test`, and `:app:compileDebugKotlin` when `app/` changed. The pipeline runs the
  full check after you.

## Your report

The last commit, with the report as its message body (`git commit --allow-empty` if nothing is left to commit).
susanna, rosie and the Opus pass read it with `git log -1`.
- **Done:** item by item against the package's "Done when".
- **Tests:** what ran, with counts.
- **Texts:** every new or changed user-visible text, for the user to approve.
- **Night defaults:** each choice the plan didn't settle, with the alternative.
- **For rosie:** what to look at on the tablet beyond the plan's device bullets.
- **Needs the user:** what you didn't build, and why.
- **Not done:** what's missing.

## Definition of done

- `./gradlew :core:test :app:lintRelease :app:assembleRelease` passes (the pipeline runs it).
- Every "Done when" line of the package is met or listed under Not done (the user's approvals belong to the morning).
- No new dependency, permission or proprietary library. `:core` stays plain Kotlin. The release build stays
  reproducible (no timestamps, no machine paths).
- New `:core` logic has tests; a fixed bug has a regression test.
- `CLAUDE.md` says what the package changed, where its "Done when" asks. No help text beyond those `CLAUDE.md` lists.
- Commits with the model's trailer, the report commit last.

## For susanna: the audit

Judge the diff against the criteria and `CLAUDE.md`; read other files only to check a claim. Look for:
- a "Done when" line neither met nor listed under Not done;
- a rule of `CLAUDE.md` broken: a new dependency or permission, a help text, a notification, network on the main
  thread, Android in `:core`;
- the plan's rules for the package (for 0.6.0: one reading of the clock; grouping by the due picture, not by the
  columns; Set applies the whole setup, no set by hand and no hold (since S4); the daily switch acts at once (from
  S4f2); the stand-in; forgetting keeps the owner and the stamp; the migration is a
  one-time write at the first start, under a lock, and sets nothing at the first run);
- logic without a test, a test that passes for the wrong reason, a fixed bug without a regression test;
- what R8 can break (serialized classes, reflection);
- files outside the package; a user-visible text missing from the report's Texts.

Reply APPROVED, or FIX with at most five findings: file:line, the concrete failure (input → wrong result), the fix.

## For rosie: the tablet checks

- Only `adb -s HA24BZ2Y`. If the tablet isn't attached, is locked with a PIN, or only the FP6 is there: NOT RUN.
- **First, what an earlier session left** (2026-10-21 night: the pipeline ends a rosie session at its timeout
  without setting the tablet back, so a cut-off V9 or V10 can leave it changed). Before noting your own state,
  compare the tablet with its normal state (V9's `settings-before.txt`, 2026-10-21) and put back what differs,
  naming each in your reply: airplane mode off (`cmd connectivity airplane-mode disable`), Wi-Fi on, battery
  saver off (`cmd power set-mode 0`, `settings put global low_power 0`), Doze not forced (`dumpsys deviceidle
  unforce`), the battery as reported (`dumpsys battery reset`), `auto_time` 1,
  the time zone Europe/Zurich (`cmd alarm set-timezone Europe/Zurich`; `auto_time_zone` stays 0), font_scale
  1.1, `wm density reset` (180) and `wm size reset`, `accelerometer_rotation` 1, `cmd uimode night no`,
  `screen_off_timeout` 300000, `http_proxy` unset (the skill's how), chain3 off, the app full screen (no split
  screen), `/data/local/tmp/fill*` removed (`df /data`), no test pictures in `Pictures/APODroid/`. Only then note
  your state as below.
- First note what you'll change: each setting you touch (`settings get` for font_scale, screen_off_timeout, wifi,
  http_proxy; `wm density`), and the app's `shared_prefs/apodroid.xml` and `files/` (with `run-as`, when a debug
  build is installed), into your folder. At the end set all of it back, and say that you did. The firewall chain
  too: off is Android's default; if you turn chain3 on, turn it off at the end (the night of 2026-10-20/21 left
  it unknown).
- Touch only APODroid and the system screens a check needs (App info, the photo picker). In
  Recents swipe away only APODroid's task; never tap or swipe on another app's screen (pascaline's, a
  browser's). If one is on top, press Home first (the night of 2026-10-20/21 dismissed a browser's task and may
  have scrolled pascaline's screen).
- Install the APK the prompt names (`install -r`). If that fails because a release-signed build is installed,
  uninstall it (the tablet only; its data is test data) and install.
- Run the package's device bullets ("Android check first", "Device (tablet …)") and what winston's report asks.
  The skill's "Working on the phone" has the how-tos; `private/wp7-handoff/` has `turn.sh`, `swap.sh`, `ui.sh`.
- Prefer text evidence: `dumpsys wallpaper`, `dumpsys jobscheduler`, `uiautomator dump`, `logcat -d -s APODroid`,
  crashes from `logcat` and `dumpsys activity exit-info`. Screenshots (`exec-out screencap -p`) go into your folder
  for the user; look at one only when the check needs eyes (each costs tokens). For texts the user must approve:
  one screenshot per screen state, named for what it shows.
- **The smoke list**, after the package's own checks on every check of winston's work (not in a device package
  such as M0, which runs 0.5.0's code): the app starts; the page loads in APOD mode and, once the switch exists,
  with the mode switch on ("All NASA pictures", "More NASA pictures" from S5); a draft change and "Set as
  wallpaper" set the wallpaper (`dumpsys wallpaper`); a forced job run
  (`cmd jobscheduler run -f io.github.buerlino.apodroid 1`, daily switch on) ends as a successful run in
  `dumpsys jobscheduler` (the log names a set, or is silent when there was nothing to set);
  `logcat` shows no crash. About 5 minutes. A failure here is a FIX even when the package's own checks passed
  (a later package broke an earlier one); say which item failed.
- At most 50 minutes, the smoke list included. No code changes, and no commits except in a device package.
- **A device package** (Agent rosie in the queue, such as M0): you're its worker. Run its steps as above. Then write
  the results where its "Done when" says (for M0, `CLAUDE.md`'s Wallpaper section), commit them on your branch with
  your model's trailer, and end with the report commit ("Your report"; Tests: what you ran on the tablet). Commit
  the results with any verdict: a STOP's evidence is what the user needs.
- Reply with the verdict alone on the first line:
  - **PASS**: everything checked works.
  - **FIX**: our app does something wrong (the steps, what happened, the logcat line); winston fixes it.
  - **STOP**: Android does something the plan didn't expect (e.g. FLAG_SYSTEM alone changes the lock screen); the
    user decides.
  - **NOT RUN**: why.

  Then one line each: `Tested:`, `Not tested:`, `For the user:` (screenshots to look at, texts to approve).

## The regression

rosie on Sonnet, after the night's last merge: the whole app on dev's debug build, so what one package broke in
another shows before the user takes `dev` into master. It doesn't replace the plan's own tablet pass (0.6.0's V:
the R8 build, the update from 0.5.0, turns over hours). The device rules are those of "For rosie: the tablet
checks" (the backup first, everything set back at the end, the tablet only); no code, no commits. At most 90
minutes; when time runs short, keep the order below and name what was left under `Not tested:`.
1. Install the APK the prompt names, run [the smoke list](#for-rosie-the-tablet-checks).
2. **What the night built:** each merged package the prompt names (its section in the plan): its device bullets
   again, on this build (not the V rows, which are tablet passes themselves, and not CR, which has no device
   bullets; 2026-10-21). What another package changed since its own check matters most.
3. **APOD mode as 0.5.0** (`CLAUDE.md`, The app; "All NASA pictures" off, or 0.5.0's settings before M3): the page
   with title, then date (no source line and no tabs with APOD alone, except the video-day source's tab on a
   video day) and the explanation ("more", the tap, the long presses); pull to refresh; ⛶ with ✓ and
   Back; ★ (then delete the saved file, the skill says how); "Where" each way with "Set as wallpaper"
   (`dumpsys wallpaper`); the daily switch on and off (`dumpsys jobscheduler`) and a forced run; a faked video day
   with each choice (the ▶ tile, the video notice and its buttons); the problem box offline (the skill: the firewall
   chain or the proxy, set back after) and its Dismiss; the battery hint.
4. **With the mode switch on** ("More NASA pictures" from S5): the three sources and their pictures (tabs; the
   title dropdown and the swipe from S6), the list ("Pictures from") with Home and Lock different and then equal,
   each applied with "Set as wallpaper" (`dumpsys wallpaper`), the mode back and forth (each keeps its settings),
   a turn ended with `turn.sh`.
5. **The edges:** a rotation and two panes, font scale 2.0 at the phone's width (`wm density 344`), `am kill` and
   reopen, no crash in `logcat` and `dumpsys activity exit-info`.

Reply with the verdict alone on the first line (PASS: nothing found; FIX: a bug in our app; STOP: Android does
something the plan didn't expect; NOT RUN: why), then `Tested:` and `Not tested:` (one line each), then one line per
bug, worst first: `BUG: <what> | steps: <…> | expected: <…> | got: <…> | evidence: <logcat line or screenshot> |
package: <the one whose code it likely is>`. Then `For the user:` (screenshots to look at).

## Opus pass

susanna on Opus, in the dev worktree; no feature code. You're the night's quality gate and make the decisions the
night can make; josh carries out your reply as written.
1. For each package in the prompt: its sections of the plan (the queue's Spec), its report (`git log -1 <merge>^2`,
   or the branch tip when unmerged), its diff (`git diff <merge>^1 <merge>`), its audit and tablet lines in
   `.coord/log.md`, and rosie's reply in its run folder (`.coord/runs/<n>-<id>/tablet-<round>.log`; for a device
   package such as M0, `worker-0.log`).
2. Run the check once on dev: `./gradlew :core:test :app:lintRelease :app:assembleRelease`, under
   `scripts/devlock.sh acquire host-cpu pass-<n> 1800`.
3. Look across the packages for what one audit misses: the pieces working together (M1's logic as M2 wires it, M3's
   settings driving M2), a decision of `CLAUDE.md` contradicted, docs that disagree with the code, a migration that
   sets something at the first run, a test that passes for the wrong reason.
4. Night defaults in the reports: keep those within the plan; the rest become QUESTION lines. A doc that disagrees
   with the code: fix the doc on dev (under `scripts/devlock.sh acquire integ pass-<n> 600`, committed with the Opus
   trailer). A STOP from rosie or an unmerged package: a FIX if the night can fix it, else a QUESTION. A device
   package's FIX or STOP (Agent rosie in the queue: M0, V1–V10): always a QUESTION, never a FIX line (2026-10-20):
   its fix package would take the row's worker, rosie, who writes no code; the morning makes it a `B<n>` package.
   The bugs V9 and V10 report are B13's, CR's findings B14's: no FIX line for them (2026-10-21).
5. Reply in exactly this form, nothing before `OPUS PASS` and nothing after NOTES. No backtick, no dollar sign and
   no double quote in a FIX line (name files and code in plain words; quote a UI text with single quotes: pass 4's
   double quotes came back escaped from josh, night.sh refused the line twice and stopped, 2026-10-20). FIX IDs are `<ID>f1`, `<ID>f2`, …; `branch` is the kept branch
   of an unmerged package, else `new`.
```
OPUS PASS <n>
VERDICT: <ID> ok | <ID> fix — <one line each>
FIX <ID>f1: model <opus|sonnet>, effort <low|medium|high>, tablet <yes|no>, branch <wp/… | new>, prompt: <one paragraph: file:line, what fails, the fix>
QUEUE: unchanged | <the remaining steps in a new order, or steps to drop>
QUESTION: <one per line: for the user, with your recommendation in one sentence>
NOTES: <at most three lines for the report>
```

## The shift report

susanna on Sonnet writes `.coord/reports/<date>-night.md` (gitignored, not committed) from the night's log, the
pass replies and the run folders. Short and plain, for the user in the morning:
1. Merged on `dev`, each with its commit and its tablet verdict. Unmerged: why, and the branch.
2. **For you this morning**, by package: the texts to approve (with the screenshots' paths), the tablet checks that
   didn't run, the QUESTION lines and the Needs the user items, each with its recommendation.
3. **Bugs from the regression** (`.coord/regressions/`; what it didn't test too): each BUG line as a draft
   `### [ ] B<n>. <what>` section and its queue row, in [The morning](#the-morning)'s step 6 form, numbered on from
   the plan's last B, the package named in the line as After; ready for the user to check and commit.
4. The Opus passes' NOTES.
5. The queue left for the next night.
6. The steps of [The morning](#the-morning), with the commands.

## Starting, watching, stopping

Before a night:
- The tablet on USB: `~/Android/Sdk/platform-tools/adb devices -l` shows `HA24BZ2Y` as `device`; screen lock None
  or Swipe; Developer options → Stay awake on; charging. Without it the night runs anyway, and the tablet checks go
  on the morning list.
- pascaline's night shares the usage window and the CPU; both can run at once, each slower.

Start, from the repo folder, in a terminal that stays open:
```
scripts/night.sh --until 07:30
```
- `scripts/night.sh --next` asks josh for the first command and prints it; nothing runs.
  `scripts/night.sh --dry-run --until 07:30` shows the closing on a simulated clock.
- Watch: `tail -f .coord/log.md`. Each run's files are in `.coord/runs/<n>-<id>/`: the bundle, winston's reply,
  the check's log, the audit, rosie's reply with her screenshots.
- Stop: Ctrl-C. A package that was running keeps its worktree and branch; the next start resumes it.
- Resume: `scripts/night.sh` again; josh continues from the log.
- One package by hand, by day or night: `scripts/pipeline.sh M3` (`--dry-run` prints its bundle; `--no-merge`
  stops before the merge; `--tablet no` skips rosie).
- Locks: `scripts/devlock.sh status`; `scripts/devlock.sh break <name>` for one whose owner is gone. A pipeline
  frees its own locks on any exit, also when a timeout or Ctrl-C kills it.
- Both scripts take stdin from `/dev/null` and every `timeout` has `-k 60` (2026-10-20: on 2026-10-19, started from
  a terminal, M0's Gradle client was stopped by the tty after BUILD SUCCESSFUL, and its timeout couldn't end it;
  the night hung 8 h on it).
- Each start checks that CLAUDE.md, the skill (the plan), the agents and the scripts have no uncommitted changes
  (the night reads master's commits only; `--dirty-ok` skips this), brings `dev` up to master and runs
  `scripts/pipeline.test.sh` (no agents). If a check fails, or a queue row doesn't resolve against the committed
  plan, the night stops before spending anything.
- To test the pipeline cheaply: `APODROID_TEST_MODEL=haiku` puts every agent on Haiku.

## The morning

1. Read `.coord/reports/<date>-night.md`.
2. What the night merged: `git log --oneline master..dev`, and `git diff master...dev`.
3. Try it: the tablet has the last package's debug build (rosie's), or build dev:
   `cd .claude/worktrees/dev && ANDROID_HOME=~/Android/Sdk ./gradlew :app:assembleDebug`.
4. Approve or change the texts and the night defaults (a day session, as before).
5. Take it into master: `git merge --ff-only dev` in the main checkout. The plan's checkboxes get ticked then.
6. A bug found in the morning, or by the night's regression (the report drafts those), becomes a package for the
   next night (pass 5, 2026-10-19): a `### [ ] B<n>. <what>`
   section in the plan, after the last package, with the steps to reproduce, what happened, what should happen,
   the files, and "Done when: a regression test that fails on the old code (or, for the page, a Device (tablet …)
   bullet that shows it), and the fix"; plus a queue row (Agent winston, the model and effort you'd pick, Tablet
   yes when the bug shows on the device, After the package that caused it, Spec `plan B<n>`). Commit both on
   master. By day, one runs at once with `scripts/pipeline.sh B<n>`, or a fix of a merged package with
   `scripts/pipeline.sh M3f1 "<the bug>"`.

An unmerged branch stays as `wp/<id>`, with its worktree, until the next run of that package (or
`git worktree remove` and `git branch -d` by hand).

## Files

- `scripts/night.sh`, `scripts/pipeline.sh` (with `pipeline-lib.sh`), `scripts/devlock.sh`,
  `scripts/agent-settings.json`; `scripts/pipeline.test.sh` checks them without starting an agent.
- `.claude/agents/josh.md`, `winston.md`, `susanna.md`, `rosie.md`.
- Gitignored, in the main checkout: `.coord/` (`log.md`, `runs/`, `passes/`, `regressions/`, `reports/`, `locks/`, `counter`,
  `night-<date>.log`, `josh-last-input.md`) and `.claude/worktrees/` (`dev` and the packages'). Each worktree gets a
  link to `private/` (gitignored as `/private`), so `SourcesTest`'s real posts and rosie's scripts are there.

## Tested (2026-10-19)

In a scratch clone, every agent on Haiku, the tablet attached:
- A package end to end: winston, the check (2 min 46 s), the audit, the merge into `dev`, the worktree and branch
  removed (3 min).
- A device package: rosie with the tablet lock and adb, her commits, PASS read, merged. rosie's check after winston:
  the debug APK built and installed with `install -r` over 0.5.0, the app opened, no crash, the settings and app
  data backed up first, PASS, merged. M0 without the tablet: NOT STARTED, nothing made.
- The Opus pass (the check on dev, its reply logged from `OPUS PASS` on) and the report (a file in `.coord/reports/`).
- josh on Sonnet, on five logs: the first step, a pass after three packages, a FIX line copied verbatim, F2 after M0
  stopped, the report at the end. All right, about 4 s each.
- The rules in `agent-settings.json`: `git push` and `git -C <dir> push` denied (the first form of the second rule
  never matched: susanna's pass flagged it, fixed), adb to the FP6 denied, adb to the tablet allowed.
- `scripts/pipeline.test.sh`: every queue row's bundle, the log helpers, the merge guard, night.sh's closing.
- night.sh itself: refused to start with an uncommitted `CLAUDE.md`; then with `--max-steps 2` the preflight, dev
  brought up to master, the last pass and the report run and their RESULT lines read, SHIFT DONE (2 min).

Not tested: the real models on real packages, a fix round, a usage limit, a merge conflict, a STOP, and a night
where josh's commands drive the loop (josh only with `--next`). The regression (added 2026-10-19 after pass 5): only
its dry run, its helpers and night.sh's closing in `pipeline.test.sh`; rosie hasn't run it.
