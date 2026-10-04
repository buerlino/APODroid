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
