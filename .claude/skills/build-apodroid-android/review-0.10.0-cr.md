# Code review CR (0.10.0, 2026-11-22)

By reading, correctness only (`release-0.10.0.md`, CR), winston in pipeline run 163, on `dev` at
c6faa2c. **Not as the plan asked:** the plan wants three reviewers blind to each other and a second
agent for each finding. The night's permission check refused the multi-agent workflow and this run
has no other way to start a subagent, so one reviewer (winston) read all three areas in turn and
verified each finding in a separate second pass: the code traced again from the call site, and
the one high finding checked in the built dex. The user decides whether a blind second review is
still wanted (Needs the user in the report).

**Read in full:** `DailyJob.kt`, `AlarmReceiver.kt`, `KeepRunning.kt`, `AppLog.kt`, `Drawing.kt`,
`LiveWallpaper.kt`; `:core`'s `JobRuns.kt`, `SwitchRuns.kt`, `PendingStarts.kt`, `PictureTime.kt`,
`Detector.kt`, `Turn.kt`, `Part.kt`, `Screens.kt`'s `dueWallpapers`, `Rules`, `restartAndForget`,
`keepUntilAnchor`, `videoDayKept`. **Read in part:** `Store.kt` (`migrateOnce`, `saveSetup`,
`setPart`, `switchDaily`, `startAfresh`, `runStarts`/`ranWell`/`nextAlarm`, the detector's notes,
`post`/`store`/`record`/`watch`, `refresh`, `checkSize`, `downloadOriginal`, `downloadPicture`,
`subject`/`noteSubject`, `makeVideoStill`, `update`, `saveInUse`, `setDue`, `judgeMiss`,
`checkSite`, the Live state and hand-over, `set`, `setFile`, `save`/`copyToGallery`);
`MainActivity.kt` (the companion, `onCreate` to `onStop`, `follow`, `load`, `runNext`,
`readStatus`, `readProblem`, the status lines, `show`, `switchDaily`/`switchOn`, `setAsWallpaper`,
`pickLive`, `place`, `savePicture`, `saveFallback`, `keepPart`, `frameOf`, `cropRect`, `setPart`,
`startCrop`, `Page`, `PictureSide`, `PicturePage`, `WholePreview`, `cropFrame`, `Settings`,
`setupChanged`, `PictureList`, the saved state's `toBundle`/`readDraft`); `Post.kt`'s `download`,
`firstBytes`, `originalSize`, `get`.

**Checked by a tool:** every method and class the app's own code (`io.github.buerlino.*`, `:core`
included) refers to in the debug APK's dex (`dexdump -d`), against the SDK's
`api-versions.xml`: what is newer than minSdk 29. All but one are behind an SDK check in `:app`
(lint covers `:app`): `getHistoricalProcessExitReasons` and `ApplicationExitInfo` (30, under
`@RequiresApi(R)`), `setExpedited`/`isExpeditedJob` (31), `isAutoRevokeWhitelisted` (30, under
`SDK_INT >= 31`), `getWallpaperInfo(int)` (34). The one without: finding 1.

## Findings

### 1. Android 10–12: no new picture is ever stored (high, CONFIRMED by the dex; the device's word is E10's and E12's)

**Fixed in 0.9.2** (B29, 2026-11-22; CLAUDE.md, Releases): reproduced with the released 0.9.1 on the
Android 10 emulator, fixed by a read loop, `AndroidApiTest`. The thought about `Error` in `update`'s loop is still open.

`:core`'s `firstBytes` (`Post.kt:184`, SH2b's probe, 0.9.0) reads the first bytes with
`InputStream.readNBytes(int)`, a Java 11 method Android has only from API 33. `:core` is a plain
JVM module, so Android lint's NewApi check never sees it, and D8 doesn't backport it: the debug
APK's dex calls `Ljava/io/InputStream;.readNBytes:(I)[B` directly (no `$r8$backportedMethods`).
On API 29–32 the call throws `NoSuchMethodError`, an `Error`, not an `Exception`.

The path: each new post's `refresh` → `downloadPicture` → `downloadOriginal` (`Store.kt:1081`,
the probe on every host that scales: APOD's and Earth's `content/dam`, NASA's `wp-content`
enclosures, 56 of 60 in the feed) → `originalSize` → `firstBytes`. `downloadOriginal` catches
only `IOException`, `checkSite` (`Store.kt:1655`) and `update`'s loop (`Store.kt:1336`) only
`Exception`, so the `Error` leaves `Store.update` at the first source with a new post: no picture
stored, no `<source>.problem` noted, the other sources not checked, and **no `setDue`**, so not even
the stored pictures' change times or turns are set. The job's `runCatching` takes it as a failed
run (retried 15 min later, then hourly, all day); the page's load says "Couldn't load today's
picture." on a fresh install, else shows the old picture with no problem box; Set toasts
"Couldn't set the wallpaper". A post stored by 0.8.0 hits it once more through `checkSize`
(`sizeChecked` is committed first). Since 0.9.0 (17 Nov), on every phone with Android 10, 11 or 12.
The emulator passes that saw the probe (SH4, T4, T5) ran on API 34.

BUG: Android 10–12 never stores a new picture since 0.9.0 (SH2b's probe calls the API-33 `InputStream.readNBytes`) | steps: Android 10, 11 or 12 (`apodroid-29` or `-31`), install the candidate, open the page (or let the job run) on a day with a post not stored yet | expected: the post's picture downloaded and set ("Refresh: APOD <date> original …" or "… rendition (original …)") | got: `java.lang.NoSuchMethodError: No virtual method readNBytes(I)[B in class Ljava/io/InputStream;` in "Update failed:" / "Daily job failed:", nothing stored or set, the run retried hourly | evidence: `dexdump -d` of the debug APK, `invoke-virtual … Ljava/io/InputStream;.readNBytes:(I)[B` in `firstBytes`; `api-versions.xml`: `readNBytes(I)[B` since 33 | package: SH2b (`core/…/Post.kt` `firstBytes`)

The fix (for its B package, not built here): read up to `length` bytes with a loop over
`read(buffer, off, len)` (or Kotlin's own reading), which every API level has; a `:core` test
that fails on the old code is hard (the JVM has the method), so the B package might add a check
over the built dex like the one above, or a unit test that `:core`'s main sources call none of
`readNBytes`, `readAllBytes`, `transferTo`. Worth a thought there too: `update`'s loop and
`checkSite` catch `Exception`, so any `Error` thrown while one source refreshes (this one; an
`OutOfMemoryError` in the subject's decode is caught inside `noteSubject`) skips the other
sources and every set; CLAUDE.md accepts a failure every run only for a set's own decode (Retries
by the alarms).

### 2. ⛶'s "Whole picture" preview keeps its bitmap while the page is stopped (low, CONFIRMED by reading)

O12 (CLAUDE.md, The page: a stopped page lets its pictures go): `onStop` (`MainActivity.kt:670`)
recycles `pictures` and the video still, because the composition holds them while stopped.
`WholePreview`'s own screen-sized bitmap (`produceState`, `MainActivity.kt:2126`, about 10 MB on
a 1080×2400 screen) is released only by `awaitDispose`, which needs a recomposition or the page's
end. Steps: ⛶, turn "Whole picture" on, press Home (or let the screen go off) with Keep running or
Live on: the preview stays in memory until the page resumes or is destroyed, possibly days.
Nothing wrong is shown; memory only, and only in that state.

BUG: ⛶'s "Whole picture" preview isn't let go in `onStop` (O12) | steps: open ⛶, "Whole picture" on, Home, Keep running on, `dumpsys meminfo io.github.buerlino.apodroid` after a while | expected: the decoded pictures let go as for the page's own (O12) | got: the preview's screen-sized bitmap held until the page resumes | evidence: by reading, `WholePreview`'s `produceState` vs `onStop` | package: L7 (`MainActivity.kt` `WholePreview`, `onStop`)

### 3. The page and ⛶ ignore a source picture's EXIF orientation (low, PLAUSIBLE: no real post seen with one)

The page decodes the stored picture with `BitmapFactory` (`decodeForScreen`, `MainActivity.kt:1024`),
which ignores EXIF orientation, while every set (`ImageDecoder`, `needsTurning`, `uprightSize`), the
subject and `uprightSizes` (the pill, the sharp frame) use the upright picture. For a NASA or Earth
JPEG with orientation 5–8 the page shows the picture turned a quarter, ⛶'s frame is drawn on the
unturned picture along the wrong axis, and with "Keep pictures sharp" `cropRect` scales the
upright size's frame onto the unturned picture. CLAUDE.md handles EXIF only for "my picture", which
the page doesn't show. None of the JPEGs in `private/` (the APOD renditions, `?h=640`) has an
orientation tag; NASA's enclosures (camera photos among them) weren't checked. Not a `BUG:` line
until a real post with one is seen; the B package, if any, would decode the page's picture with
`ImageDecoder` as `Drawing.kt` does.

## Refuted in the second pass (not bugs)

- An old run beside a new one after a quick off and on (`JobRuns.drop`) shares `stoppedBySystem`
  with it: the old run's update may still set, but only what's due by the same stored setup;
  B19 and O4c already accept a run from before an off finishing a set it began.
- `LiveWallpaper.reload` between `copyForLive`'s rename and its commit reads the new file with the
  old notes (whole, sideways, darkening): the commit changes the key, so the next listener call
  decodes it again; a process death in between keeps old notes until the next hand-over
  (narrow, self-healing at the next set).
- `show()` decodes outside the refresh lock, so a rename between reading the post and decoding can
  show the new picture under the old title for one draw: the next `isStale()` sees the stored post
  changed and shows again.
- The draft's hidden settings put back (`LaunchedEffect`s in `Settings`): a Set tapped in the same
  frame as the change that hides one could save it; not reachable by one tap (the hiding change and
  Set are separate taps).
- `readDraft` drops the whole draft when one part line doesn't parse: the lines are written by
  `Part.toText` in the same build; a parse failure means a saved state from another version,
  where the stored setup is the right fallback.
