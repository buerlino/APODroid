# CLAUDE.md

Guidance for working in this repo. It is the source of truth for later sessions: decisions with
the date and a short "why", what was tested and rejected, and open questions. Keep it current.

## Project

An Android app that sets NASA's Astronomy Picture of the Day (APOD) as the phone's wallpaper,
once a day, on its own. It replaces the user's Tasker task `APOD` (`private/APOD.tsk.xml`,
gitignored because it holds the user's NASA API key). The app is one page: today's picture
(title, date), and a few settings below it. No other features.

App name: **APODroid** (user, 2026-10-01). applicationId and namespace `io.github.buerlino.apodroid`;
Kotlin packages `io.github.buerlino.apodroid` (app) and `io.github.buerlino.apodroid.core`.
Repo: https://github.com/buerlino/APODroid (GPLv3); local folder `FOSS_APOD`. Release APKs are
named `apodroid-vX.Y.Z.apk`.

- Detailed build plan and progress: [.claude/skills/build-apodroid-android/SKILL.md](.claude/skills/build-apodroid-android/SKILL.md).
  Where it and this file disagree, this file wins.

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
- Few libraries: kotlinx.serialization and `HttpURLConnection` (no Retrofit/OkHttp). Before any
  other dependency, check whether the JDK or Android already covers it.
- Storage: SharedPreferences for small settings, one JSON file for larger data. No database until
  it's really needed. No background work unless the feature can't work without it (this one
  can't, see [Background update](#background-update-proposed)).
- Personal data stays on the phone. Real test files go in the gitignored `private/`; tests use
  made-up data.
- English UI, short texts: one idea per line, drop what the screen already shows, explain each
  concept in one place only.

## Setup and distribution (decided, copied from gridload)

`~/Documents/source99/gridload` is a released app built exactly this way. Copy its build setup
instead of re-deriving it; its CLAUDE.md, section "Decided", explains each choice:
- Gradle wrapper, `gradle/libs.versions.toml` (Gradle 9.8.0, AGP 9.4.1, Kotlin 2.4.20, Compose
  BOM 2026.09.00), `gradle/gradle-daemon-jvm.properties` (the system `java` is 27-ea, too new;
  Gradle runs on JDK 21).
- AGP 9 has built-in Kotlin: in `:app` apply only `com.android.application` +
  `org.jetbrains.kotlin.plugin.compose`. `compileSdk 37`, `targetSdk 37`, `minSdk 29` (was 26 as gridload; raised 2026-10-01 for
  saving to the gallery without a permission, see Save), Java 17. Since minSdk ≥ 28 AGP stores
  `classes.dex` uncompressed, so the release APK grew from 1.0 to 1.76 MB with the dex slightly
  smaller; left as is (Android runs it from the APK without an extracted copy).
- Release signing from gitignored `keystore.properties` or env vars (`APODROID_KEYSTORE_FILE`,
  `_KEYSTORE_PASSWORD`, `_KEY_ALIAS`, `_KEY_PASSWORD`, renamed from gridload's `GRIDLOAD_`), unsigned without either (what F-Droid wants). `.github/workflows/release.yml`
  builds a signed APK on a `vX.Y.Z` tag and attaches it to a GitHub Release (Obtainium).
  `fastlane/metadata/android/en-US/` for F-Droid, with `changelogs/<versionCode>.txt`.
- Release build uses R8 (minify + shrinkResources). The build must be reproducible: no
  timestamps, build paths or machine-specific values in the APK; `dependenciesInfo` off. Unit
  tests don't cover R8, so test release builds on the phone.
- Android SDK in `~/Android/Sdk`. The user tests on a real phone over adb, no emulator.

Set up 2026-10-01 (step 1): copied as above, with `GRIDLOAD_` → `APODROID_` and
`gridload-` → `apodroid-` in the build and the workflow. Git branch `master` (as gridload).
Differences from gridload: `lifecycle-runtime-compose` left out until something needs it; no
permissions yet (each is added with the feature that uses it); dark theme
(`Theme.Material.NoActionBar` + Compose `darkColorScheme()`), which suits the pictures; the
icon came later (skill, step 5). versionCode 1, versionName 0.1.0. Tested: `:core:test`,
`assembleDebug` and `assembleRelease` pass; the unsigned release APK is 765 KB, and a second
build from a copy in another folder was byte-identical.

First release (user, 2026-10-01): 0.1.0, only after step 4 (the daily job), because the daily
change is the app's point. The user made the icon (`logo/`; skill, step 5). Signing: a
new keystore just for APODroid (not gridload's), made with `keytool` outside the repo; the user
sets the passwords, backs it up and adds the CI secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`). Done 2026-10-01: alias `apodroid`, PKCS12, RSA 4096, 10000 days,
one password for store and key; the secrets are set. The GitHub repo had an "Initial commit" (LICENSE only) on
`master`; local history is built on it, remote `origin`.

**0.1.0 released 2026-10-02:** tag `v0.1.0` on `6954492`; the Release workflow built and signed
`apodroid-v0.1.0.apk` (1.77 MB, signer `CN=Norman Bürli`, SHA-256
`b7dd5ace7b317f91cd347d147b88205dd9a52d1c1e36a40c5996d670b00dc538`) and published the GitHub
Release. That APK is installed on the phone and works (see the skill, step 5).
**0.1.1 tagged 2026-10-02** (versionCode 2): the large-picture fix and the explanation. F-Droid:
merge request https://gitlab.com/fdroid/fdroiddata/-/merge_requests/50926 for 0.1.1, submitted
2026-10-02, in review (skill, step 5).

## Data source (checked 2026-10-01)

**The official API is broken.** `https://api.nasa.gov/planetary/apod` (what the Tasker task
uses) returns `title: "NASA Science"` and the NASA logo
(`science.nasa.gov/wp-content/themes/nasa-child/assets/images/nasa-logo@2x.png`) as `url` and
`hdurl` for every date tried (25 Sep to 1 Oct 2026, and 1 Aug 2026). Cause: APOD moved from
`apod.nasa.gov` to `science.nasa.gov/apod` (the old pages now redirect there with 301), and the
API still scrapes the old site. So the Tasker task has probably been setting the NASA logo as the
wallpaper. `DEMO_KEY` allows 10 requests an hour (`x-ratelimit-limit: 10`).

**Source: the new site's WordPress REST API, the only one** (user, 2026-10-01; public, no key,
no auth). Why: the official API has no working data at all; the risk below is accepted.

```
GET https://science.nasa.gov/wp-json/wp/v2/image-article?categories=22766&per_page=1
```

- Category 22766 is `{"name":"APOD","slug":"apod","count":11465}`: the whole archive was migrated.
  Newest first.
- Fields used (all present on every post checked, 24 Jun to 1 Oct 2026):
  - `date`: e.g. `2026-10-01T00:05:00` (US Eastern; `date_gmt` `2026-10-01T04:05:00`). A new
    picture appears at about 04:05 UTC (06:05 in Switzerland in summer, 05:05 in winter).
  - `title.rendered`: `APOD: 2026 October 1 &#8211; Harvest Moon with Erupting Mount Etna`. Needs
    the `APOD: <date> – ` prefix stripped and HTML entities decoded (`&#8211;`, `&#8217;`, and some
    titles already have a plain `–`). One title had `-` with no space after it (`2026 July 18
    -Shadow and Rainbow`).
  - `featured_image.file`: the picture, e.g.
    `https://assets.science.nasa.gov/dynamicimage/assets/science/cds/apod/apod/2026/october/Harvest%20Moon%20...%20LD.jpg`.
  - `link`: the post's page, for "open on the web".
- `_fields=` makes the response small (510 bytes instead of 66 KB) but **drops `featured_image`**
  (`featured_image_url` survives, a 1600 px `?w=...&fit=clip&crop=...` rendition). So either
  request the full response once a day, or use `_fields=date,link,title,featured_image_url` and
  strip its query string. **Decided (step 2):** the full response (about 21 KB per post), because
  the video check needs `content` and `featured_image.file` is the plain URL.
- Image sizes for 1 Oct: `featured_image.file` without parameters → 1280×853 (115 KB);
  `https://assets.science.nasa.gov/content/dam/<same path>` → the original, 1600×1067 (1.1 MB);
  `?w=4096` → 4096×2730, but only upscaled. The file name ends in `LD`, so a larger HD original
  may exist somewhere (open question).
- **Not always a small JPEG:** on 2 Oct 2026 `featured_image.file` was `sharpless_catalog.png`,
  a 37.7 MB PNG, 4455×5592, served whole without parameters (`?w=1600` → 5.3 MB PNG, still PNG).
  The app handled it (download about 6 min on mobile data, page, wallpaper), but decoded in full
  it's a 99.6 MB bitmap, just under Android's 100 MB limit for drawing one. **Fixed (user,
  2026-10-02, for 0.1.1):** the page decodes with `inSampleSize`, halved as often as it stays
  screen-wide and to at most 8 MP (`decodeForScreen`; today: 2227×2796, about 25 MB). The
  download, the wallpaper and the saved copy stay full size.
- **Video days** (about 1 in 6 posts mention a video; e.g. 9 Sep `<video>` with an `.mp4`, many
  others link YouTube in the explanation): `featured_image` is still there, a still frame
  (`xz_and_frame.jpg`). The Tasker task skipped videos and kept the old wallpaper.
  **Detection (settled 2026-10-01, step 2):** the hero block, from `media-detail-hero__media` to
  the next `<h1` (the title), holds `<img>` on image days and `<video>` (mp4) or `<iframe>`
  (YouTube) on video days. Checked on all 100 posts 24 Jun to 1 Oct 2026: 11 video days (e.g.
  9 and 13 Sep `<video>`, 23 Aug and 29 Jul `<iframe>`), never both, never neither. YouTube
  links in the explanation (about 1 in 3 image days) are outside the hero.
  **The "still frame" isn't always one:** on 4 of those 11 days `featured_image` is a generic NASA
  image (26 Jul the NASA logo `NASA-Meatball-Feature5.png`; 24 Jun, 8 Jul, 13 Jul
  `.../cosmic-origins/images/misc/news-thumbnail.png`). So no still-frame setting (First version).
- No rate-limit headers seen. One request a day (or a few) is harmless.

Risk: this is the site's internal WordPress API, not a documented public API. The category id or
the fields could change. Keep the parsing in `:core`, tolerant (`ignoreUnknownKeys`, nullable
fields), and fail without touching the current wallpaper.

**Traffic at scale, checked 2026-10-02** (user asked: would 1000 users "DDoS" the server?): no.
`Store.isCurrent` makes `refresh()` (the one JSON GET) a once-a-day call per device; job runs and
page opens later that same day are free, no network call, once the stored APOD is current. The
`JobScheduler` job has no shared trigger time across devices (each one's period starts at its own
install/enable time, further smeared by Doze/App Standby), so there's no synchronized burst at
04:05 UTC. Back of envelope: 1000 devices × about 1 JSON request/day (~21 KB) + 1 image/day ≈
1000–2000 requests/day total, well under 1 request/s on average — the same order of magnitude as
1000 people running the old Tasker task once a day against `api.nasa.gov`. The real risk from this
endpoint is fragility (above), not load.

**User-Agent (2026-10-02, after 0.1.1):** both requests send
`APODroid/<versionName> (+https://github.com/buerlino/APODroid)`, so the site can tell this app
apart (and throttle or block just it); not needed for load, just good practice on an undocumented
endpoint. `fetchLatest` and `download` take it as a parameter (`:core` has no Android);
`Store` builds it with `versionName` read from the installed package at runtime, so the version
stays only in `app/build.gradle.kts` and no `BuildConfig` is needed. Tested: a `:core` test checks
the header arrives at a local server; `curl` with it got HTTP 200 from the real endpoint; debug
build on the phone (fresh install) fetched and showed 2 Oct's picture. The bytes the phone sends
weren't inspected.

## What the Tasker task did (for reference, not to copy)

1. Wait a random 10 to 180 s.
2. `GET api.nasa.gov/planetary/apod` (30 s timeout). If not 200: retry up to 20 times, waiting
   10, 20, 30 ... s more each time; after that a notification "ERROR: <title>".
3. If `media_type` is `image`: download `url` (not `hdurl`) to `Download/nasa_apod.jpg`, media
   scan, set it as wallpaper (Tasker "Set Wallpaper", option value 2; which screens is unclear),
   notification "Today's Astro Pic: <title>".
4. Else (video): notification "ASLOP: <title>", wallpaper unchanged.

It was started by a Tasker profile that isn't in the export (presumably once a day).

## First version (decided 2026-10-01)

- **One page.** Top: today's picture, full width, with its title and date; tap opens the APOD
  page in the browser. Below: the settings.
- **Settings** (SharedPreferences):
  - "Change wallpaper daily": a switch that schedules or cancels the job.
  - **Where:** home screen, lock screen, or both (user: a setting, not fixed). Default: both.
  - **Video days:** keep the previous picture, or use my own picture (user, 2026-10-01). Default:
    keep the previous one, as the Tasker task did. The still frame is not offered: on 4 of 11
    video days it's a generic NASA image (see Data source). "Keep the previous picture" means
    leave the wallpaper alone. "My picture" is picked once with the system photo picker
    (`PickVisualMedia`, in activity-compose; no permission) and copied to `files/fallback.jpg`;
    a button to pick or change it shows under that choice. Chosen but nothing picked → keep the
    previous one.
  - "Set as wallpaper now": a button. It uses the same two settings.
- **Fetch** in `:core`: newest post → `Apod(date, title, imageUrl, pageUrl, isVideo)`. The image
  is downloaded to the app's private files (`files/apod.jpg`, no storage permission) and shown
  from there, decoded with `BitmapFactory` (no image library). The current entry's fields in
  SharedPreferences. The page shows the still frame on video days either way; the setting only
  decides the wallpaper.
- **Wallpaper:** `WallpaperManager.setStream(stream, null, true, which)` with `FLAG_SYSTEM`,
  `FLAG_LOCK` or both; Android centre-crops the landscape pictures itself. Only change it when
  the date is new and the download is a complete, decodable image; otherwise leave the current
  wallpaper.
- **Built (step 3, 2026-10-01):** the page refreshes on each `onResume` unless the stored date is
  today's US Eastern date (APOD's). A failed refresh keeps showing the stored picture; only with
  none is there "Couldn't load today's picture." + "Try again". "Set as wallpaper now" reports
  with a Toast ("Wallpaper set", "Video today: wallpaper kept"). Video days show "· Video" after
  the date (not in the original plan; easy to drop).
- **Explanation (user, 2026-10-02, for 0.1.1):** a ▾ (text glyph) right of the title shows
  APOD's explanation below it, ▴ hides it; tapping the title row does the same, a long press on
  it opens the APOD page (so does tapping the picture, as before). `Apod.explanation` in `:core`:
  the hero's `media-detail-hero__description` paragraph up to its first `<br>` (after it come
  site notes and "Tomorrow's picture"), tags stripped, without the "Explanation:" label. Checked
  on all 100 posts 24 Jun to 1 Oct 2026: always present, 660 to 1323 characters. No ▾ if empty.
  An entry stored by 0.1.0 has no explanation, so the page fetches the JSON again once.
- **No help texts for the settings** (user, 2026-10-01: they explain themselves). Labels only.
- **No notification** (user, 2026-10-01). Why: it would need the `POST_NOTIFICATIONS` runtime
  permission on Android 13+, and the page shows the title.

### Save (user, 2026-10-01; built)

The star at the bottom right of the picture saves it to the gallery: `Pictures/APODroid/`, named
`APOD_<date>_<Title_Words>.jpg` (`Apod.fileName` in `:core`; the date makes it unique, only
letters, digits, `-`, `_`, at most 100 chars). Outlined ☆ = not saved, filled ★ = today's picture
is saved; a second tap says "Already saved". The star is a text glyph (no icon library). Written
with `MediaStore` (`IS_PENDING`), which needs no permission on Android 10+, hence `minSdk 29`
(user chose that over `WRITE_EXTERNAL_STORAGE` for Android 8–9, or a "Save as" dialog each
time). The saved date and `content://` URI are in the prefs; the star checks the URI still
exists, so deleting the file in the gallery empties it again. It saves `files/apod.jpg` as
downloaded (1280 px). No star on video days (the still frame is often a generic NASA image).

### Background update (decided 2026-10-01)

Daily changes can't work without background work. **`JobScheduler`** (part of Android, no
library), not WorkManager (an AndroidX dependency) or `AlarmManager` (needs our own retry and
reboot handling). A periodic job, every ~6 hours, only with network; each run asks for the newest
post (small JSON) and downloads and sets the image only when the date is new. That replaces the
Tasker retry loop (the job's own backoff) and the random delay, and picks up the new picture
within a few hours of 04:05 UTC. `setPersisted(true)` keeps it across reboots.

Permissions (user approved, 2026-10-01), all granted at install with no prompt:
- `INTERNET`
- `SET_WALLPAPER`
- `RECEIVE_BOOT_COMPLETED` (for `setPersisted`)
- `ACCESS_NETWORK_STATE` (user approved 2026-10-01, step 4): Android 14+ throws a
  `SecurityException` on `schedule()` for a job with a network constraint without it. The
  alternative, no network constraint, would wake and fail offline and use up retries.

**Built (step 4, 2026-10-01):** `DailyJob.kt`. Each run: `refresh()` unless the stored APOD is
today's, then set the wallpaper if the stored APOD's date isn't `wallpaperDate` (the date last
set, saved by `setWallpaper`, also by "Set as wallpaper now"). A separate date is needed because
opening the page also downloads the new picture; the job must still set it. On a video day with
"keep" nothing is saved, so switching to "my picture" later that day takes effect on the next run.
`refresh()` holds a lock, as the page and the job can run it at once. Failures return
`needsReschedule` (the job's backoff, 30 s exponential). The switch's state is whether the job is
scheduled (`getPendingJob`), no pref; `pm clear` cancels the job and the switch shows off.
Turning it on runs the job right away (the first period starts now), so today's picture is set
at once.

## Open questions

1. (Answered 2026-10-02: the explanation shows on demand, see First version.)
2. Is there a higher resolution than the 1600 px original (the `LD` file name)? Check later; not
   needed for the first version.
3. Huge pictures (2 Oct 2026, 37.7 MB PNG, see Data source) cost mobile data. The crash risk is
   fixed; limiting the download isn't decided.
