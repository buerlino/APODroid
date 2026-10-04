# CLAUDE.md

Guidance for working in this repo. It is the source of truth for later sessions: decisions with
the date and a short "why", what was tested and rejected, and open questions. Keep it to current
facts; finished-work narratives go (git keeps them).

## Project

An Android app that sets NASA's Astronomy Picture of the Day (APOD) as the phone's wallpaper,
once a day, on its own. It replaces the user's Tasker task `APOD` (`private/APOD.tsk.xml`,
gitignored because it holds the user's NASA API key). The app is one page: today's picture
(title, date), and a few settings below it. No other features.

App name: **APODroid** (user, 2026-10-01). applicationId and namespace `io.github.buerlino.apodroid`;
Kotlin packages `io.github.buerlino.apodroid` (app) and `io.github.buerlino.apodroid.core`.
Repo: https://github.com/buerlino/APODroid (GPLv3); local folder `APODroid`. Release APKs are
named `apodroid-vX.Y.Z.apk`.

- How-tos, phone testing and what's still untested: [.claude/skills/build-apodroid-android/SKILL.md](.claude/skills/build-apodroid-android/SKILL.md).
  Where it and this file disagree, this file wins.
- Declutter passes: [.claude/skills/build-apodroid-android/declutter.md](.claude/skills/build-apodroid-android/declutter.md).
- Planned features, how and checklist: [.claude/skills/build-apodroid-android/features.md](.claude/skills/build-apodroid-android/features.md)
  (decisions in [Next features](#next-features)).

## How the user works (2026-10-01)

- Simplest approach that works. No extra screens, options, layers or abstractions until they are
  needed. Don't clutter the UI or the code.
- When unsure, or when a simpler or better idea comes up, ask the user before deciding. Don't ask
  about what's settled below.
- Step by step: do one step, show it working, then wait for the next instruction.
- Commit only when asked. The user pushes; never `git push`.
- When reporting back, say what was actually tested and what wasn't.

## Stack (decided by the user, 2026-10-01)

- Native Android: Kotlin + Jetpack Compose, single Activity. No Flutter, React Native or KMP.
- No proprietary dependencies (Firebase, Play Services, analytics, ads). As few permissions as
  possible; ask the user before adding any.
- Two modules: `:core` (plain Kotlin/JVM, no Android: the logic, parsing and network code,
  unit-tested with JUnit4) and `:app` (Compose UI, depends on `:core`).
- Few libraries: kotlinx.serialization, `HttpURLConnection` (no Retrofit/OkHttp), activity-compose,
  core-ktx (declared at 1.18.0, the version other libraries already pulled in, for `prefs.edit { }`
  and `toUri()`). Before any other dependency, check whether the JDK or Android already covers it.
- Storage: SharedPreferences for small settings, one JSON file for larger data. No database until
  it's really needed. No background work unless the feature can't work without it (this one
  can't, see [Background update](#background-update)).
- Personal data stays on the phone. Real test files go in the gitignored `private/`; tests use
  made-up data.
- English UI, short texts: one idea per line, drop what the screen already shows, explain each
  concept in one place only.
- Migrations: remove migration code two releases after F-Droid has shipped past the version that
  needed it (gridload's rule, 2026-10-02).

## Setup and distribution (copied from gridload)

`~/Documents/source99/gridload` is a released app built exactly this way. Copy its build setup
instead of re-deriving it; its CLAUDE.md explains each choice.
- Gradle wrapper, `gradle/libs.versions.toml` (Gradle 9.8.0, AGP 9.4.1, Kotlin 2.4.20, Compose
  BOM 2026.09.00), `gradle/gradle-daemon-jvm.properties` (the system `java` is 27-ea, too new;
  Gradle runs on JDK 21).
- AGP 9 has built-in Kotlin: in `:app` apply only `com.android.application` +
  `org.jetbrains.kotlin.plugin.compose`. `compileSdk 37`, `targetSdk 37`, `minSdk 29` (raised
  from gridload's 26 for saving to the gallery without a permission, see [Save](#save)), Java 17.
  Since minSdk ≥ 28 AGP stores `classes.dex` uncompressed (APK 1.76 MB instead of 1.0); left as is.
- Dark theme (`Theme.Material.NoActionBar` + Compose `darkColorScheme()`), which suits the pictures.
- Backup: no cloud backup, phone-to-phone transfer allowed (`data_extraction_rules.xml`, as
  gridload; `allowBackup="false"` covers Android 10–11).
- Release signing from gitignored `keystore.properties` or env vars (`APODROID_KEYSTORE_FILE`,
  `_KEYSTORE_PASSWORD`, `_KEY_ALIAS`, `_KEY_PASSWORD`), unsigned without either (what F-Droid
  wants). Its own keystore, not gridload's (alias `apodroid`, PKCS12, RSA 4096; the user keeps
  it and the passwords); the CI secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
  `KEY_PASSWORD` are set.
- `.github/workflows/release.yml` builds a signed APK on a `vX.Y.Z` tag and attaches it to a
  GitHub Release (Obtainium); `test.yml` runs the `:core` tests on branch pushes and pull requests.
  `fastlane/metadata/android/en-US/` for F-Droid, with `changelogs/<versionCode>.txt`.
- Release build uses R8 (minify + shrinkResources). The build must be reproducible: no
  timestamps, build paths or machine-specific values in the APK; `dependenciesInfo` off. Unit
  tests don't cover R8, so test release builds on the phone.
- Android SDK in `~/Android/Sdk`. The user tests on a real phone over adb, no emulator.
- Git branch `master`, remote `origin`.

**Releases:** 0.1.0 (2026-10-02, GitHub only). 0.1.1 (versionCode 2, 2026-10-02): the
large-picture fix and the explanation; on F-Droid since merge request
https://gitlab.com/fdroid/fdroiddata/-/merge_requests/50926 was merged (2026-10-04). New
versions reach F-Droid by themselves: its bot builds each new `vX.Y.Z` tag (skill, Releasing).
0.1.2 (versionCode 3): the declutter fixes of 2026-10-04, the themed icon, screen-reader
labels, the User-Agent, the feature graphic and the fixes from the review of 0.1.2 (declutter
file). Tagged `v0.1.2` on the commit with the review fixes (moved from b243c43 before it was
pushed, user 2026-10-04). The R8 build of b243c43 was tested on the phone; the review fixes
weren't (no phone).

## Data source (checked 2026-10-01)

**The official API is broken.** `https://api.nasa.gov/planetary/apod` (what the Tasker task
uses) returns `title: "NASA Science"` and the NASA logo as `url` and `hdurl` for every date
tried. Cause: APOD moved from `apod.nasa.gov` to `science.nasa.gov/apod` (301 redirects), and the
API still scrapes the old site.

**Source: the new site's WordPress REST API, the only one** (user, 2026-10-01; public, no key,
no auth). Why: the official API has no working data at all; the risk below is accepted.

```
GET https://science.nasa.gov/wp-json/wp/v2/image-article?categories=22766&per_page=1
```

- Category 22766 is APOD (the whole archive, 11465 posts). Newest first.
- Fields used (all present on every post checked, 24 Jun to 1 Oct 2026):
  - `date`: e.g. `2026-10-01T00:05:00` (US Eastern). A new picture appears at about 00:05 US Eastern
    (04:05 UTC in summer time, 05:05 UTC in winter).
  - `title.rendered`: `APOD: 2026 October 1 &#8211; Harvest Moon with Erupting Mount Etna`. The
    `APOD: <date> – ` prefix is stripped and HTML entities decoded (`&#8211;`, `&#8217;`; some
    titles have a plain `–`, one had `-` with no space after it).
  - `featured_image.file`: the picture.
  - `link`: the post's page, for "open on the web".
  - `content.rendered`: for the video check and the explanation.
- The full response (about 21 KB per post), not `_fields=` (which drops `featured_image`).
- Image sizes for 1 Oct: `featured_image.file` → 1280×853 (115 KB);
  `https://assets.science.nasa.gov/content/dam/<same path>` → the original, 1600×1067 (1.1 MB);
  `?w=4096` only upscales. On other days the original is much larger (2026-10-04, 20 posts:
  54 KB to 10 MB, up to 5815 px), and `?h=` gives a screen-sized rendition (see Next features).
- **Not always a small JPEG:** on 2 Oct 2026 it was a 37.7 MB PNG, 4455×5592 (`?w=1600` → 5.3 MB,
  still PNG); about 6 min on mobile data. Decoded in full it's a 99.6 MB bitmap, just under
  Android's 100 MB drawing limit, so the page decodes with `inSampleSize` (`decodeForScreen`,
  user 2026-10-02). The download, the wallpaper and the saved copy stay full size.
- **Video days** (about 1 in 9 posts): `featured_image` is still there, but on 4 of 11 video days
  checked it's a generic NASA image, not a still frame. **Detection (2026-10-01):** the hero
  block, from `media-detail-hero__media` to the next `<h1` (the title), holds `<img>` on image
  days and `<video>` (mp4) or `<iframe>` (YouTube) on video days. Checked on all 100 posts 24 Jun
  to 1 Oct 2026: never both, never neither. YouTube links in the explanation (about 1 in 3 image
  days) are outside the hero.
- **Explanation:** the hero's `media-detail-hero__description` paragraph up to its first `<br>`
  (after it come site notes and "Tomorrow's picture"), tags stripped, without the label. Present
  on all 100 posts, 660 to 1323 characters.

Risk: this is the site's internal WordPress API, not a documented public API. The category id or
the fields could change. Keep the parsing in `:core`, tolerant (`ignoreUnknownKeys`, nullable
fields), and fail without touching the current wallpaper.

**Traffic (checked 2026-10-02):** one JSON GET per device per day (`Store.isCurrent`), plus the
image. The job's period starts at each device's own enable time, so there's no shared burst at
00:05 US Eastern. 1000 users ≈ 1000–2000 requests a day, like 1000 Tasker tasks against `api.nasa.gov`.
The risk is fragility, not load.

**User-Agent (2026-10-02):** both requests send
`APODroid/<versionName> (+https://github.com/buerlino/APODroid)`, so the site can tell this app
apart. `:core` takes it as a parameter; `Store` reads `versionName` from the installed package,
so the version lives only in `app/build.gradle.kts`.

The Tasker task (for reference only) fetched `api.nasa.gov` with retries, downloaded `url`,
set it as wallpaper, and on video days kept the old wallpaper.

## The app (decided 2026-10-01 unless dated)

- **One page.** Top: today's picture, full width, with its title and date; tap opens the APOD
  page in the browser. Below: the settings.
- **Settings** (SharedPreferences):
  - A switch for the daily change: schedules or cancels the job.
  - **Where:** home screen, lock screen, or both (user: a setting, not fixed). Default: both.
  - **Video days:** keep the previous picture, or use my own picture (user). Default: keep, as
    the Tasker task did. No still-frame option (often a generic NASA image). "My picture" is
    picked once with the system photo picker (`PickVisualMedia`, no permission) and copied to
    `files/fallback.jpg`; chosen but nothing picked → keep.
  - A button to set the wallpaper now, with the same two settings.
  - No help texts (user: the labels explain themselves).
- **Fetch** in `:core`: newest post → `Apod(date, title, imageUrl, pageUrl, isVideo, explanation)`.
  The image goes to `files/apod.jpg` (whatever its format; no storage permission), via a `.part`
  file, and replaces the old one only when complete and decodable. Decoded with `BitmapFactory`
  (no image library). The current entry's fields in SharedPreferences. A post older than the
  stored one is ignored (2026-10-04, a guard against a stale cache; never seen). Known gap, left
  (user, 2026-10-04): the picture is renamed a few ms before the prefs are written; death in
  between leaves today's picture under yesterday's entry until the next refresh fetches it again
  (declutter file, Review of 0.1.2, 2.2).
- **The page** refreshes on each `onResume` unless the stored APOD is today's (US Eastern); it
  also redraws when the job stored a new one in the background. A failed refresh keeps the
  stored picture; only with none is there an error with a retry button. Video days show "Video"
  after the date. The page shows the still frame on video days either way.
- **Explanation (user, 2026-10-02):** a ▾ right of the title shows it, ▴ hides it; tapping the
  title row does the same, a long press opens the APOD page. No ▾ if empty. The glyphs have
  screen-reader labels (2026-10-04).
- **Wallpaper:** `WallpaperManager.setStream(stream, null, true, which)`; Android centre-crops.
  Only changed when the date is new and the download is a complete, decodable image.
- **No notification** (user): it would need the `POST_NOTIFICATIONS` runtime permission, and the
  page shows the title.
- **Icon:** the user's SVGs in `logo/` as an adaptive icon; a monochrome layer for themed icons
  (2026-10-04) computed from the foreground (skill, Icon).

### Save

(User, 2026-10-01.) The star at the bottom right of the picture saves it to the gallery,
`Pictures/APODroid/`, named `APOD_<date>_<Title_Words>.<jpg|png>` (`Apod.fileName` in `:core`; the
date makes it unique, only letters, digits, `-`, `_`, at most 100 chars; the extension follows
the image type). It copies `files/apod.jpg` as downloaded, full size. ☆ = not saved, ★ = saved;
a second tap (or a quick double tap) doesn't save again. Text glyphs, no icon library. Written
with `MediaStore` (`IS_PENDING`), which needs no permission on Android 10+, hence `minSdk 29`
(user chose that over `WRITE_EXTERNAL_STORAGE` for Android 8–9, or a "Save as" dialog each
time). The saved date and `content://` URI are in the prefs; the star checks the URI still
exists, so deleting the file in the gallery empties it again. No star on video days. A tap that
waited for a download (shared lock, see Background update) and finds a newer APOD stored saves
nothing, says "Not saved: a new picture came in" and redraws the page (user, 2026-10-04: simpler
than a lock of its own; "Sharper wallpaper" changes `save()` anyway).

### Background update

(Decided 2026-10-01.) **`JobScheduler`** (part of Android, no library), not WorkManager (an
AndroidX dependency) or `AlarmManager` (needs our own retry and reboot handling). `DailyJob.kt`:
periodic, every ~6 hours, only with network, `setPersisted(true)`. Each run: `refresh()` unless
the stored APOD is today's, then set the wallpaper if the stored APOD's date isn't
`wallpaperDate` (the date last set, also by the set-now button). A separate date is needed
because opening the page also downloads the new picture; the job must still set it. On a video
day with "keep" nothing is saved, so switching to "my picture" later that day takes effect on
the next run. `refresh()` and `save()` hold one lock, as the page and the job can run at once.
Failures return `needsReschedule` (30 s exponential backoff). The switch's state is whether the
job is scheduled (`getPendingJob`), no pref; turning it on runs the job at once.

Permissions (user approved, 2026-10-01), all granted at install with no prompt: `INTERNET`,
`SET_WALLPAPER`, `RECEIVE_BOOT_COMPLETED` (for `setPersisted`), `ACCESS_NETWORK_STATE` (Android
14+ throws on `schedule()` for a job with a network constraint without it; no constraint would
wake and fail offline).

## Next features

(User, 2026-10-04, from a review of 0.1.1 for features users would expect; the review is
`private/APODroid missing features users might expect.md`. Not built yet; plan and checklist in
`features.md` in the skill.)

- **Sharper wallpaper:** the daily download becomes a rendition the screen's height
  (`featured_image.file?h=<px>`, 0.3–1.2 MB on the posts checked) instead of the 1280 px file.
  ★ saves the full original (`content/dam`, 2–10 MB) instead of the daily file. Why: the
  originals load on 20 of 20 posts checked, but downloading them daily costs about 10× the data.
- **Fill / Fit:** a setting next to "Wallpaper on", default Fill (today). Fit puts the whole
  picture on black.
- **Status line** under the daily switch, always while it's on: when the wallpaper last
  changed, or since when no check has worked (more than a day). Why: the endpoint is fragile and
  a failing job looks like a working one.
- **Wi-Fi only:** a switch for the job, default off.
- **Hibernation:** check on the phone whether Android pauses the unused app; only if so, a hint
  with a button to the system setting. No battery-optimisation permission. **Checked
  2026-10-04: it does, twice** (restricted bucket after 8 days, hibernation deletes the job
  after about 3 months; the job doesn't count as use). Report and options:
  [keep-running.md](.claude/skills/build-apodroid-android/keep-running.md). Not decided yet.
- **Share:** title and page link via the share sheet; not the picture file.
- **Image credit** under the date, parsed in `:core` (on all 100 posts checked).
- **Save every picture:** a switch for the job, default off, with a small ⓘ next to it: saved
  pictures are full resolution, and saving every day takes a lot of space. The only exception
  to "no help texts" (user, 2026-10-04).
- **Video marker** ▶ on video days; **selectable explanation** (copy).
- **Left out:** previous days (not one page any more), full-screen view, home-screen widget,
  Quick Settings tile (rarely used), translations (English UI stays), sharing the picture file
  (needs a `FileProvider`). Still out, as above: a notification, the still frame on video days,
  help texts, choosing the check time, `api.nasa.gov` as a fallback.

## Open questions

1. (Answered 2026-10-04: the `content/dam` originals go up to 5815 px, see Next features.)
2. Huge pictures (2 Oct 2026, 37.7 MB PNG) cost mobile data, and on a slow connection the
   download may not finish within the roughly 10 minutes Android gives a job (each retry starts
   over). Left open (user, 2026-10-04): it happened once. The planned rendition and Wi-Fi only
   switch cover most of it.
