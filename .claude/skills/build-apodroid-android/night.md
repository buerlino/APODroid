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
- `scripts/pipeline.sh report`: the night's report for the user, `.coord/reports/<date>-night.md`.
- Nothing is pushed and master isn't touched: `dev` is master plus the night's merges. The user fast-forwards
  master after the morning review ([The morning](#the-morning)).

## The team

| Agent | Does | Model | Tools |
|---|---|---|---|
| josh | picks the next command | Sonnet, medium | none (all on stdin) |
| winston | builds a package; the fix rounds | the queue row's (the plan's) | all but Agent |
| susanna | audits a diff; the Opus pass; the report | audit Opus or Sonnet; pass Opus; report Sonnet | audit Read, Grep, Glob; else all but Agent |
| rosie | the tablet checks; a device package (M0) | checks Sonnet, medium; a package the row's | checks all but Agent and Edit; a package all but Agent |

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
9. The end: the queue, the last pass and its FIX packages, the report, then SHIFT DONE. With `--until HH:MM` (or
   at the step cap minus 2) night.sh closes by itself: the last pass if a package finished since the last one,
   then the report.

## The queue

Tonight's (user, 2026-10-19): M1 to M4 and F2 of `plan-0.6.0.md`, with code, tests and the tablet checks, after
M0, the Android check the plan's second review put first (M1's grouping rests on it). The texts wait for the
user. F1, F3, F4 and F5 each start with a question for the user, so they stay for the day. "Model, effort" is the
plan's. Spec names the sections the bundle takes: `plan <key>` is the plan's `### [ ] <key>.`, `features <key>`
is `features.md`'s. Without the tablet M0 can't run, so M1 to M4 wait and only F2 runs.

| Step | ID | What | Agent | Model, effort | Tablet | After | Spec |
|---|---|---|---|---|---|---|---|
| 1 | M0 | Android check: home and lock set separately | rosie | opus, medium | no | — | plan M0 |
| 2 | M1 | `:core`: screens, columns, the stand-in and the hold | winston | opus, high | no | M0 | plan M1 |
| 3 | M2 | `Store`: mode, grid, a record per screen, migration, fetch rules | winston | opus, high | yes | M1 | plan M2 |
| 4 | M3 | Settings: the mode switch, the grid, the dropdown, the set menu | winston | opus, high | yes | M2 | plan M3 |
| 5 | M4 | The page: three tabs, chip, status lines, video notice, problem box | winston | opus, medium | yes | M3 | plan M4 |
| 6 | F2 | Wi-Fi only | winston | sonnet, medium | yes | — | plan F1–F5 + features 4 |

An Opus pass follows every three finished packages (MERGED or NOT MERGED; fix packages count), and the last one
comes when no queue step can run.

## Commands josh replies with

The first that applies:
1. The last Opus pass has a FIX line whose ID has no RESULT line yet: that FIX, in the pass's order, as
   `scripts/pipeline.sh <ID> --model <m> --effort <e> --tablet <yes|no> --branch <b> "<prompt>"`, with the FIX
   line's values and its prompt verbatim; leave out `--branch` when the line says `new`.
2. Three packages have a RESULT line since the last pass (after the last `RESULT pass <n>: done`):
   `scripts/pipeline.sh pass`.
3. The next queue step without a RESULT line whose After packages are merged (the package, or a fix of it
   `<ID>f<k>`, has a MERGED line), in the order of the last pass's QUEUE line, else the table's:
   `scripts/pipeline.sh <ID>`. A step whose After is NOT MERGED is passed over tonight.
4. No queue step can run, and no pass came after the last queue step's RESULT line: `scripts/pipeline.sh pass`
   (the last pass; its FIX lines then run by rule 1).
5. After that: `scripts/pipeline.sh report`; once the report has its RESULT line, `SHIFT DONE`.

A RESULT line with LIMIT or FAILED: run the same command again. SKIPPED or NOT STARTED: never again tonight.
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
  columns; the hold and its ends; the stand-in; forgetting keeps the owner and the stamp; the migration is a
  one-time write at the first start, under a lock, and sets nothing at the first run);
- logic without a test, a test that passes for the wrong reason, a fixed bug without a regression test;
- what R8 can break (serialized classes, reflection);
- files outside the package; a user-visible text missing from the report's Texts.

Reply APPROVED, or FIX with at most five findings: file:line, the concrete failure (input → wrong result), the fix.

## For rosie: the tablet checks

- Only `adb -s HA24BZ2Y`. If the tablet isn't attached, is locked with a PIN, or only the FP6 is there: NOT RUN.
- First note what you'll change: each setting you touch (`settings get` for font_scale, screen_off_timeout, wifi,
  http_proxy; `wm density`), and the app's `shared_prefs/apodroid.xml` and `files/` (with `run-as`, when a debug
  build is installed), into your folder. At the end set all of it back, and say that you did.
- Install the APK the prompt names (`install -r`). If that fails because a release-signed build is installed,
  uninstall it (the tablet only; its data is test data) and install.
- Run the package's device bullets ("Android check first", "Device (tablet …)") and what winston's report asks.
  The skill's "Working on the phone" has the how-tos; `private/wp7-handoff/` has `turn.sh`, `swap.sh`, `ui.sh`.
- Prefer text evidence: `dumpsys wallpaper`, `dumpsys jobscheduler`, `uiautomator dump`, `logcat -d -s APODroid`,
  crashes from `logcat` and `dumpsys activity exit-info`. Screenshots (`exec-out screencap -p`) go into your folder
  for the user; look at one only when the check needs eyes (each costs tokens). For texts the user must approve:
  one screenshot per screen state, named for what it shows.
- At most 45 minutes. No code changes, and no commits except in a device package.
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

## Opus pass

susanna on Opus, in the dev worktree; no feature code. You're the night's quality gate and make the decisions the
night can make; josh carries out your reply as written.
1. For each package in the prompt: its sections of the plan (the queue's Spec), its report (`git log -1 <merge>^2`,
   or the branch tip when unmerged), its diff (`git diff <merge>^1 <merge>`), its audit and tablet lines in
   `.coord/log.md`, and rosie's reply in its run folder (`.coord/runs/<n>-<id>/tablet-<round>.log`).
2. Run the check once on dev: `./gradlew :core:test :app:lintRelease :app:assembleRelease`, under
   `scripts/devlock.sh acquire host-cpu pass-<n> 1800`.
3. Look across the packages for what one audit misses: the pieces working together (M1's logic as M2 wires it, M3's
   settings driving M2), a decision of `CLAUDE.md` contradicted, docs that disagree with the code, a migration that
   sets something at the first run, a test that passes for the wrong reason.
4. Night defaults in the reports: keep those within the plan; the rest become QUESTION lines. A doc that disagrees
   with the code: fix the doc on dev (under `scripts/devlock.sh acquire integ pass-<n> 600`, committed with the Opus
   trailer). A STOP from rosie or an unmerged package: a FIX if the night can fix it, else a QUESTION.
5. Reply in exactly this form, nothing before `OPUS PASS` and nothing after NOTES. No backtick and no dollar sign in
   a FIX line (name files and code in plain words). FIX IDs are `<ID>f1`, `<ID>f2`, …; `branch` is the kept branch
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
3. The Opus passes' NOTES.
4. The queue left for the next night.
5. The steps of [The morning](#the-morning), with the commands.

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
- Locks: `scripts/devlock.sh status`; `scripts/devlock.sh break <name>` for one whose owner is gone.
- Each start brings `dev` up to master and runs `scripts/pipeline.test.sh` first (no agents); if a queue row doesn't
  resolve against the committed plan, the night stops before spending anything.
- To test the pipeline cheaply: `APODROID_TEST_MODEL=haiku` puts every agent on Haiku.

## The morning

1. Read `.coord/reports/<date>-night.md`.
2. What the night merged: `git log --oneline master..dev`, and `git diff master...dev`.
3. Try it: the tablet has the last package's debug build (rosie's), or build dev:
   `cd .claude/worktrees/dev && ANDROID_HOME=~/Android/Sdk ./gradlew :app:assembleDebug`.
4. Approve or change the texts and the night defaults (a day session, as before).
5. Take it into master: `git merge --ff-only dev` in the main checkout. The plan's checkboxes get ticked then.

An unmerged branch stays as `wp/<id>`, with its worktree, until the next run of that package (or
`git worktree remove` and `git branch -d` by hand).

## Files

- `scripts/night.sh`, `scripts/pipeline.sh` (with `pipeline-lib.sh`), `scripts/devlock.sh`,
  `scripts/agent-settings.json`; `scripts/pipeline.test.sh` checks them without starting an agent.
- `.claude/agents/josh.md`, `winston.md`, `susanna.md`, `rosie.md`.
- Gitignored, in the main checkout: `.coord/` (`log.md`, `runs/`, `passes/`, `reports/`, `locks/`, `counter`,
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

Not tested: the real models on real packages, a fix round, a usage limit, a merge conflict, a STOP, and
night.sh's loop beyond one decision (`--next`; the closing only as a dry run).
