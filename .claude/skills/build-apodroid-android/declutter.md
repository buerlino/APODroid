# Declutter

A periodic pass, as in gridload (user, 2026-10-04): scan everything, report what could be
removed, rewritten or is deprecated, plus bugs and design flaws; the user decides; then work
through the checklist and tick items off.

## What to check in a pass

- **Code:** dead or duplicated code, logic repeated between `:core` and `:app`, races between
  the page and the job (both use `Store`), state the page keeps that can go stale.
- **Migrations:** remove migration code two releases after F-Droid has shipped past the version
  that needed it (`CLAUDE.md`, Stack).
- **Docs:** `CLAUDE.md` loads into every session, so keep it to current facts and decisions;
  finished-work narratives go (git keeps them). Look for stale lines, links to headings that
  moved, duplicates between `CLAUDE.md` and the skill, and UI text copied into docs (it drifts
  from the code).
- **Repo:** files nothing uses, fastlane changelogs for versions F-Droid never built, loose git
  objects (`git gc`).
- **Build/CI:** `./gradlew :core:test :app:lintDebug :app:lintAnalyzeDebug --rerun` (lint can
  repeat a stale report otherwise), Kotlin compiler warnings (lines starting `w:`),
  `--warning-mode all` (deprecations; `Configuration.setVisible` comes from a plugin, ignore),
  two clean unsigned `assembleRelease` builds from copies that include `.git` with the same
  sha256 (the APK holds the git commit), action versions in `.github/workflows/`, redundant
  `gradle.properties`.

## Pass 2026-10-04

Report: 11 tests green, no compiler warnings, lint 0 errors / 8 warnings, reproducible. Not
tested on the phone; the bugs came from reading the code.

### Bugs
- [x] 1.1 The page kept yesterday's picture after the job stored a new one in the background
  (while the star already saved the new one): redraw when the stored APOD differs.
- [x] 1.2 A failed "my picture" pick deleted the previous one: copy to `fallback.part` first.
- [x] 1.3 A quick double tap on ☆ saved two copies: `save()` checks `isSaved` inside its lock.
- [x] 1.4 Ignore a post older than the stored one (user: add the guard).

### Code simplifications
- [x] 2.1 `prefs.edit { }` and `toUri()` (6 lint warnings); core-ktx declared at 1.18.0.
- [x] 2.2 `private fun store(apod: Apod)` instead of a nullable setter.
- [x] 2.3 One `imageBounds()` for `imageType` and `decodeForScreen`.
- [x] 2.4 Drop `usesCleartextTraffic="false"` (the default for this target).

### Migrations
- [x] 3.1 Drop the 0.1.0 explanation refetch (`prefs.contains("explanation")`): 0.1.0 was on
  GitHub only, for about a day; add the removal rule to `CLAUDE.md`.

### Docs
- [x] 4.1 Stale lines: `FOSS_APOD`, the broken `#background-update-proposed` link, Save's
  ".jpg, 1280 px", answered open question 1, the skill's roadmap header and step 0 note,
  `fetchLatest()` without the User-Agent, three "State left on the phone" notes.
- [x] 4.2 Trim `CLAUDE.md` to current facts.
- [x] 4.3 Skill: how-tos plus a list of what's still untested.
- [x] 4.4 This file.

### Repo, build and CI
- [x] 5.1 Delete `changelogs/1.txt` (F-Droid starts at 0.1.1).
- [x] 5.2 Remove `android.useAndroidX=true`.
- [x] 5.3 Release workflow on `checkout@v7` / `setup-java@v6`; gridload's `test.yml`.
- [x] 5.4 Backup: `data_extraction_rules.xml` (no cloud, device transfer), as gridload.
- [x] 5.5 Monochrome icon layer (user: try the front as a mask; see the skill, Icon).
- [x] 5.6 `git gc` (286 loose objects, 3.2 MB → one 2.1 MB pack).
- [x] 5.7 `:core` tests for an incomplete download and a post without the hero marker.
- [x] Also (user): screen-reader labels for ☆/★ and ▾/▴. Huge pictures vs the job's time limit:
  left open (`CLAUDE.md`, open questions).

### Done (2026-10-04)

Verified: 13 tests green, no compiler warnings, lint 0 warnings, debug and R8 release builds,
two clean release builds byte-identical. Not tested on the phone (see the skill, Still untested).
Where the work differed from the report:
- **1.4:** the guard also requires `apod.jpg` to exist, so a lost picture is still fetched again.
- **5.5:** the plain mask merged the letters into blobs, so the outlines are cut out instead
  (grown by 0.4 units, readable at launcher size).

## Review of 0.1.2 (2026-10-04)

A report-only bug review of b243c43 (tag `v0.1.2`) before pushing. Worked through the same day;
the fixes are untested on the phone (not available).

Ran: `:core:test :app:lintDebug :app:lintAnalyzeDebug --rerun :app:assembleRelease` (13 tests
green, lint no issues, no `w:` lines after a forced Kotlin recompile); core-ktx 1.18.0 is what
other libraries already resolve; the monochrome icon rendered (rsvg-convert) matches the
foreground's shapes (only the soft glow at the centre is missing) and is in the R8 APK.
On the phone, the R8 build signed with the debug key: the page loads and fetches (after faking
yesterday with the debug build), ▾/▴, a quick double tap on ☆ saves one copy, "Set as wallpaper
now", a forced job run sets the wallpaper, the switch off removes the job and on runs it at once.
Not tested: see the skill, Still untested.

### Bugs
- [x] 1.1 `MainActivity.kt` `load()`: the page can show today's title and date over yesterday's
  picture. `show()` draws the stored (old) APOD, then `refresh()` waits for the lock while
  another refresh downloads (the daily job, or the old activity's after a rotation: its
  blocking refresh keeps running). That one stores today's, so this one returns false and the
  `else apod = store.apod` branch sets the new title over the old bitmap; the next `onResume`
  doesn't redraw (`store.apod == apod`, `isCurrent`). Found by reading, not reproduced. Fix:
  `changed.onSuccess { if (picture == null || store.apod != apod) show() }`. Done as proposed;
  `refresh()` no longer returns whether it changed (nothing used it any more).

### Risks
- [x] 2.1 `Store.save()` shares the lock with `refresh()`: a tap on ☆ during a long download
  (the 37 MB PNG) gives no feedback for minutes, then saves the new picture instead of the one
  shown. Already so in 0.1.1. No deadlock (no nested locks; the `return` inside the inline
  `synchronized` releases it). Fix: pass the shown date to `save()` and skip if it differs, or
  give `save()` its own lock (it only guards the double tap). Done: `save(date)` (user's choice).
- [x] 2.2 `Store.refresh()` renames the picture before `store(latest)`: death in between leaves
  today's file under yesterday's title (and a save's name) until the next refresh. A window of
  milliseconds. Left (user, 2026-10-04): the next refresh (next page open, or the job run
  again) sees yesterday's date stored and downloads today's again, which repairs it; a ☆ tap
  waits for that and saves nothing (2.1). A real fix needs dated file names, more code than the
  bug is worth; swapping the two steps would be worse (today's date stored, never fetched).

### Build and release
- [x] 3.1 `master` (b243c43) is already on GitHub, only the tag isn't: fixes can't go into
  b243c43 any more. Either move the unpushed `v0.1.2` tag to a new commit (and update the
  fdroiddata recipe's hash) or ship them as 0.1.3. The tag moves (user).
- [x] 3.2 Push order: the recipe's `Binaries:` needs `apodroid-v0.1.2.apk` on GitHub, so push
  the tag, wait for the Release workflow, then push the fdroiddata branch (remote still has the
  0.1.1 recipe, 80afecc61; local 48874ac7b has the right hash). Moot: the merge request was
  merged with 0.1.1 the same day, and F-Droid's bot picks up the tag itself.

### Docs
- [x] 4.1 "About 04:05 UTC" (`CLAUDE.md` Data source and Traffic, `README.md`) holds only in US
  summer time; from 1 Nov it's 05:05 UTC. Say "00:05 US Eastern".
- [x] 4.2 `CLAUDE.md`: `test.yml` runs on branch pushes and pull requests, not "every push" (tags
  go to `release.yml`). `Apod.kt` `fileName` KDoc example drops "Erupting". Both fixed.

## Pass 2026-10-04, evening (after 7d73bc2)

A report-only pass before the 0.2.0 work continues: a check of 7d73bc2 (keep running), then
the full list above. The user decided on each item; done below.

Ran: `clean :core:test :app:lintDebug :app:lintAnalyzeDebug :app:assembleDebug
:app:assembleRelease --rerun-tasks --warning-mode all`: 13 tests green, lint "No issues found",
no `w:` lines, the only deprecation is `Configuration.setVisible` (plugin, ignored). Two clean
unsigned `assembleRelease` builds from copies with `.git`: same sha256 (`a58b674b…`, also the
same as the repo's own build). Actions `checkout@v7` / `setup-java@v6` and Gradle 9.8.0 are the
latest. AOSP's `AppStandbyController` and `Settings` for Android 10–15 and `main`, read from
android.googlesource.com (thresholds below).

On the phone (Fairphone 6, Android 16), the R8 build of 7d73bc2 signed with the debug key, over
the debug build: hint with both settings missing; App info opens; "Don't show again" → dialog,
rotated to landscape and back with the dialog open (it stays), Cancel keeps the hint; battery
set to Unrestricted in the real Settings screens → the hint names only the other setting;
"Manage app if unused" turned off in App info → the hint is gone; `cmd app_hibernation
set-state true` and `am force-stop` each delete the job, opening the page brings it back (switch
on, the job ran at once); switch off + force stop + reopen → stays off, no job; Hide in the
dialog → hidden, also after a force stop and reopen. Restored afterwards: debug build again,
battery Optimized and "Manage app if unused" on (both in Settings), `pauseHintHidden` removed
(hint visible), switch on, job scheduled, auto-rotate on. Not tested: TalkBack itself (only the
`uiautomator` tree), Android 10–12 (no such phone; AOSP source only), a reboot.

### Checked in 7d73bc2, no change needed
- **`Store.daily` writing on first read:** acceptable. Its only reader is
  `MainActivity.onCreate` (main thread); `DailyJob` doesn't read it, so nothing reads it before
  the page. A 0.1.2 user whose job was already deleted (hibernation, force stop) before the
  update gets `daily=false`, which is what 0.1.2's switch showed anyway. An explicit
  `migrate()` call would be more code for the same thing. Its removal date: 4.3.
- **`onResume` rescheduling:** `switchDaily`, `onResume` and `load()`'s start all run on the main
  thread, so they can't interleave. It schedules only when `daily` is on and the job is missing:
  never twice (a second `schedule()` would replace the job and restart its period) and never
  against the switch (tested, above). The job's first run overlapping `load()`: both call
  `refresh()` under the lock; the second makes one extra JSON request (21 KB) and no second
  download. That was already so when turning the switch on; left.
- **Skipping API 30 for hibernation:** right. Android 11 only resets runtime permissions of
  unused apps, and APODroid has none. `isAutoRevokeWhitelisted` is what androidx's
  `getUnusedAppRestrictionsStatus` uses on 31+. (The battery half on 10–12 is wrong: 1.2.)
- **Dialog and rotation:** `rememberSaveable` holds; the first composition happens after
  `onResume` (the window attaches then), so `batteryLimited`/`mayHibernate` are already set
  when the saved state is restored.
- **Hint hides when the switch is off:** yes (`daily && …`).
- **Semantics:** the two lines are separate text nodes, the buttons are clickable nodes named
  by their text, the dialog has title, text and two buttons. Fine; TalkBack not tried.

### Bugs
- [x] 1.1 `Store.setWallpaper` stores `wallpaperDate = apod?.date` *after* `setStream`, which
  takes seconds on a big picture. A "Set as wallpaper now" tap while the page's (or the job's)
  refresh is storing a new APOD sets yesterday's picture but records today's date; the job then
  skips today, and the wallpaper stays old until tomorrow. Found by reading, not reproduced;
  in the code since 0.1.0. Fix: read the date before opening the file (`val date = apod?.date`
  first line of `setWallpaper`). If the rename lands in between, the new picture is set under
  the old date and the job sets it once more, which is harmless.
- [x] 1.2 The battery half of the hint is wrong on Android 10–12. AOSP's restricted bucket:
  Android 10 has none (rare is the last bucket); Android 11 has it switched off by default
  (`DEFAULT_ENABLE_RESTRICTED_BUCKET = 0`) and at 30 days when on; Android 12 at 45 days; 8
  days only from Android 13. Yet `isIgnoringBatteryOptimizations` is false by default on all of
  them, so 10–12 users see "Set battery use to Unrestricted" and a dialog promising "after about
  8 days … only while the phone charges". On 10–11 there's also no "Unrestricted" in App info
  (it's "Battery optimization → Not optimized", in a list of all apps). Fix (recommended):
  check battery only on API 33+, as hibernation only on 31+. Alternative: from 31 with "after a
  week or more" in the dialog (true on 12 as well, vaguer on 13+).
- [x] 1.3 The hint's buttons sit 12 dp right of the text above (measured: text at x=48 px,
  "App info" at x=84 px): `TextButton`'s content padding. Fix: `Modifier.offset(x = (-12).dp)`
  on the button `Row`, so the button text lines up and the touch target stays.
- [x] 1.4 Hint text against the text rules and the real screens: "Set battery use to
  Unrestricted and turn off pausing when unused." is two ideas on one line. On this phone the
  settings are "Allow background usage" (a switch that must be turned on before Unrestricted can
  be picked; tested) and "Manage app if unused"; "pausing when unused" is stock Android 12–14's
  wording. Fix: one line per missing setting, naming the target state, e.g. "Battery:
  Unrestricted" / "Pause or manage app if unused: off". Show the user on the phone first.

### Code
- [x] 2.1 `PauseHint` has the same three-way `when` twice (hint line and dialog text). One `when`
  returning both texts (or two short lists joined) halves it. Do it together with 1.2/1.4,
  which change the texts anyway.
- [x] 2.2 `android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS` is written out because
  the `Settings()` composable shadows the class. Import the constant instead.

### Migrations
- [x] 3.1 `Store.daily`'s fallback to `getPendingJob` (0.1.2 → 0.2.0) is the only migration code.
  Keep it; remove it in the second release after F-Droid has shipped 0.2.0 (listed in 4.3).

### Docs
- [x] 4.1 `CLAUDE.md` Next features: "Not built yet" is stale (keep running is built), and the
  built "Keep running" paragraph is a current fact, not a plan. Fix: move it to The app (or
  Background update) as a short bullet, say "Not built yet, except keep running" or drop the
  sentence, and keep only the decisions (hint only for what's missing, App info, hidden for good
  after the dialog, no permission).
- [x] 4.2 "No help texts" is no longer true: `CLAUDE.md` The app ("No help texts (user: the
  labels explain themselves)"), Next features ("The only exception to 'no help texts'") and
  `features.md` item 8 ("The one exception"). The pause hint is a second exception. Fix: one line
  in The app: no help texts, except the pause hint and the planned ⓘ; drop the other two claims.
- [x] 4.3 `CLAUDE.md` Stack, Migrations: add the list the rule now needs, with one entry:
  `Store.daily` from `getPendingJob`, remove from the second release after 0.2.0 on F-Droid.
- [x] 4.4 `CLAUDE.md` Releases: "the review fixes weren't (no phone)" is stale; the phone pass
  of 2026-10-04 tested most of them (skill, Still untested). Fix: point to the skill instead.
- [x] 4.5 `CLAUDE.md` Open questions: question 1 is answered; drop it (renumber 2).
- [x] 4.6 Untested items in two places: `features.md` item 5 ends with its own "Not tested" list,
  which the skill's Still untested doesn't have. Fix: move them to the skill and update with
  this pass (R8 build and the real Settings screens now tested; left: Android 10–12, TalkBack,
  the real 90-day hibernation, archiving).
- [x] 4.7 `keep-running.md` is partly stale and misses today's findings. Stale: "the switch
  **off** (its state is `getPendingJob`)" (Problem 2) and option 3 ("ask the user") read as
  current; "replaces the guesses in features.md, item 5" (gone). Add: the AOSP thresholds per
  version (1.2); the battery page showed "Allow background usage" off again after `adb install
  -r` (it had been set to Optimized), and its radio buttons are disabled until the switch is on;
  App info didn't redraw "Manage app if unused" after a second tap (a fresh App info showed it
  right); the Settings screen writes the op's *uid* mode (`appops get` → `Uid mode: … ignore`),
  where How to test sets the package mode (both work). Restore after testing: add "Manage app if
  unused" on and removing `pauseHintHidden`.
- [x] 4.8 UI text copied into docs: `CLAUDE.md` Save quotes the Toast "Not saved: a new picture
  came in". Fix: "says so". Button labels ("App info", "Set as wallpaper now") stay, as how-tos
  need them.
- [x] 4.9 Skill, Where things are: add `daily` and `pauseHintHidden` to `Store.kt`, and
  `checkPausing`/`PauseHint` to `MainActivity.kt`.

### Repo, build and CI
- [x] 5.1 131 loose git objects (620 KB) since the last `git gc`. Optional: `git gc`.
- Nothing else: no unused files, changelogs 2 and 3 are versions F-Droid ships (0.1.2 is
  tagged on GitHub; its bot builds it), `gradle.properties` has only `jvmargs` and the code
  style, workflows are on the latest actions.

### Done (2026-10-04, evening)

Verified, released as 0.2.0: clean `:core:test :app:lintDebug :app:lintAnalyzeDebug
:app:assembleDebug :app:assembleRelease --rerun-tasks --warning-mode all`: 13 tests green, lint
"No issues found", no `w:` lines. On the phone, debug build: the hint shows "Battery:
Unrestricted" and "Pause or manage app if unused: off" on separate lines, "App info" starts at
x=48 px like the text above it. R8 build of 0.2.0 (debug key): same hint, App info opens, the
dialog's text for both settings, Cancel, "Set as wallpaper now" sets it, a forced job run ends
with `jobFinished`, no crash. Not tested: the hint with only one setting missing (same code
path as before), and 1.1 (not reproduced). Store screenshot retaken: byte-identical to the old
one once demo mode shows a full battery, so unchanged (with the hint, the page no longer fits on
one screen; user: without it).
Where the work differed from the report:
- **1.2:** decided by the user: battery checked on API 33+ only.
- **1.4:** decided by the user: one line per missing setting, the report's wording.
- **4.1:** keep running moved to `CLAUDE.md`, Background update; Next features says it's built.
- **4.3:** decided by the user; the list is in `CLAUDE.md`, Stack. **5.1:** `git gc` run.
- Also (user): README links F-Droid and, with the store description, mentions the hint.
