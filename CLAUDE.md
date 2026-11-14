# CLAUDE.md

Guidance for working in this repo. It is the source of truth for later sessions: decisions with
the date and a short "why", what was tested and rejected, and open questions. Keep it to current
facts; finished-work narratives go (git keeps them).

## Project

An Android app that sets NASA's Astronomy Picture of the Day (APOD) as the phone's wallpaper,
once a day, on its own. It replaces the user's Tasker task `APOD` (`private/APOD.tsk.xml`,
gitignored because it holds the user's NASA API key). The app is one page: today's picture
(title, date), and a few settings below it (beside it on wide screens). No other features. From 0.5.0 also NASA Image of the
Day and Earth Observatory Image of the Day (user, 2026-10-16; [Next features](#next-features)).

App name: **APODroid** (user, 2026-10-01). applicationId and namespace
`io.github.buerlino.apodroid`; Kotlin packages `io.github.buerlino.apodroid` (app) and
`io.github.buerlino.apodroid.core`. Repo: https://github.com/buerlino/APODroid (GPLv3); local
folder `APODroid`. Release APKs are named `apodroid-vX.Y.Z.apk`.

- How-tos, phone testing and what's still untested: [.claude/skills/build-apodroid-android/SKILL.md](.claude/skills/build-apodroid-android/SKILL.md).
  Where it and this file disagree, this file wins.
- Declutter passes: [.claude/skills/build-apodroid-android/declutter.md](.claude/skills/build-apodroid-android/declutter.md).
- Planned features, how and checklist: [.claude/skills/build-apodroid-android/features.md](.claude/skills/build-apodroid-android/features.md)
  (decisions in [Next features](#next-features)).
- 0.4.0's and 0.5.0's plans (done) and the code reviews CR and CR2 (fixed): in git, deleted
  2026-10-26 (`git log --diff-filter=D -- .claude/skills/build-apodroid-android/`).
- Work for 0.6.0 (APOD first, the "More NASA pictures" switch, and the features from 0.1.1's
  review; planned 2026-10-19): [.claude/skills/build-apodroid-android/plan-0.6.0.md](.claude/skills/build-apodroid-android/plan-0.6.0.md).
- 0.7.0's night plan (2026-11-13): the tracks, the order across them and the packages without a track (B26), for
  `scripts/pipeline.sh`; the queue in `night.md`: [.claude/skills/build-apodroid-android/plan-0.7.0.md](.claude/skills/build-apodroid-android/plan-0.7.0.md).
- 0.7.0, on time on any phone (the expedited run, "Keep running", the detector; 2026-10-28): [.claude/skills/build-apodroid-android/on-time-0.7.0.md](.claude/skills/build-apodroid-android/on-time-0.7.0.md).
- 0.7.0, Live, the day's pan (the live wallpaper, Size [Fill | Live]; 2026-10-28): [.claude/skills/build-apodroid-android/live-0.7.0.md](.claude/skills/build-apodroid-android/live-0.7.0.md).
- 0.7.0, bright pictures darker (GitHub issue #2; planned 2026-11-02): [.claude/skills/build-apodroid-android/bright-0.7.0.md](.claude/skills/build-apodroid-android/bright-0.7.0.md).
- 0.7.0, turns as a plain rotation (the "hasn't shown" rule and the waiting go; planned 2026-11-11): [.claude/skills/build-apodroid-android/rotation-0.7.0.md](.claude/skills/build-apodroid-android/rotation-0.7.0.md).
- 0.7.0, "As soon as it's out" (a switch: a new picture goes on within the hour, not only in the 3-hour window; planned 2026-11-13): [.claude/skills/build-apodroid-android/soon-0.7.0.md](.claude/skills/build-apodroid-android/soon-0.7.0.md).
- 0.7.0, the list's order (drag the sources into order; after a Set of a new order the top goes on at the next "New picture at", what's on kept until then; planned 2026-11-14): [.claude/skills/build-apodroid-android/order-0.7.0.md](.claude/skills/build-apodroid-android/order-0.7.0.md).
- 0.8.0, Fill's frame on the subject (issue #3, a saliency map; planned 2026-11-02): [.claude/skills/build-apodroid-android/subject-0.8.0.md](.claude/skills/build-apodroid-android/subject-0.8.0.md).
- A clearer Setup and leaner texts (planned for 0.7.0, 2026-10-29; MK and UI1–UI3 built in 0.6.0 as U0, 2026-11-02, with ⛶'s ✓ setting again; what's left for 0.7.0 is there): [.claude/skills/build-apodroid-android/lean-ui-0.7.0.md](.claude/skills/build-apodroid-android/lean-ui-0.7.0.md).
- Night shifts (user, 2026-10-19): the agents josh, winston, susanna and rosie work through a queue of packages
  unattended (`scripts/night.sh`). How it works, the rules and the queue: [.claude/skills/build-apodroid-android/night.md](.claude/skills/build-apodroid-android/night.md).
  Only APODroid's nights use the tablet HA24BZ2Y (user, 2026-10-20: pascaline's nights leave it alone).
- **APODiOS** (user, 2026-11-01): an iPhone shortcut, not an app; recipe and link in
  [ios/README.md](ios/README.md). Why: iOS has no wallpaper API for apps or the web (a PWA can't
  set it), only Shortcuts' Set Wallpaper Photo with a daily Time of Day automation. Plain APOD on
  both screens, video days skipped by one regex equal to `:core`'s `isVideo` (283 real posts, 27
  video days, no difference), the picture `featured_image.file?h=2868`. Shared as an iCloud link
  from the user's own Apple account; no `.shortcut` file in the repo (a signed binary; "people who
  know me" carries the signer's identity). Built on the user's iPhone XS Max, iOS 18: a run by hand
  and the video test passed; a second run hit iOS 18's extensionKit error 2 (a known bug; a fresh
  photo wallpaper and a second automation 5 min later). Open: the link (waits for the phone's
  iCloud), the morning automation.

## How the user works (2026-10-01)

- Simplest approach that works. No extra screens, layers or abstractions until they are
  needed. Don't clutter the UI or the code.
- **Settings: few, each explained in one sentence** (user, 2026-10-23, from now on; it
  replaces 2026-10-22's "let users customise as much as possible", which turned every decision
  into a setting with its draft state, highlight, label and tablet checks): a setting only when
  users would really disagree and no default can serve both; anything that can't be explained
  in one sentence on the page doesn't get built. Ask the user before adding one; no extra
  screens. Setup is a draft until Set; Advanced settings act at once.
- When unsure, or when a simpler or better idea comes up, ask the user before deciding. Don't ask
  about what's settled below.
- Step by step: do one step, show it working, then wait for the next instruction.
- Commit only when asked. The user pushes; never `git push`.
- Night shifts (2026-10-19): starting `scripts/night.sh` is the go for the agents' commits on `wp/*` branches and
  their merges into `dev`, never on master and never pushed. The packages then run one after another without
  waiting; the user reviews `dev` in the morning and fast-forwards master (night.md).
- When reporting back, say what was actually tested and what wasn't.

## Stack (decided by the user, 2026-10-01)

- Native Android: Kotlin + Jetpack Compose, single Activity. No Flutter, React Native or KMP.
- No proprietary dependencies (Firebase, Play Services, analytics, ads). As few permissions as
  possible; ask the user before adding any.
- Two modules: `:core` (plain Kotlin/JVM, no Android: the logic, parsing and network code,
  unit-tested with JUnit4) and `:app` (Compose UI, depends on `:core`). No Compose UI tests
  (user, 2026-10-21: rosie's tablet checks cover the page; no new test dependencies).
- Few libraries: kotlinx.serialization, `HttpURLConnection` (no Retrofit/OkHttp), activity-compose,
  core-ktx (declared at 1.18.0, the version other libraries already pulled in, for `prefs.edit { }`
  and `toUri()`). Before any other dependency, check whether the JDK or Android already covers it.
- Storage: SharedPreferences for small settings, one JSON file for larger data. No database until
  it's really needed. No background work unless the feature can't work without it (this one
  can't, see [Background update](#background-update)).
- Personal data stays on the phone. Real test files go in the gitignored `private/`; tests use
  made-up data.
- English UI, short texts: one idea per line, drop what the screen already shows, explain each
  concept in one place only. Binding for every new or changed text from 0.7.0 on, and applied
  to the page's texts in 0.6.0 by U0 (2026-11-02; the pause hint waits for O13) (user,
  2026-10-29: "I don't like the many sentence-like texts on the page"): a label or a fact, not
  a sentence; one fact per line, numbers and names over words; explanations only in an ⓘ or a
  dialog, never on the page; Android's own labels word for word (people search for them).
- Migrations: remove migration code two releases after F-Droid has shipped past the version that
  needed it (gridload's rule, 2026-10-02). Current ones: `Post.isNoted` letting a missing
  `wallpaperUrl`, `savedUrl` or `cropUrl` (0.4.0's prefs) match by date alone (0.4.0 → 0.5.0,
  WP7d), so the update sets nothing and keeps ★ and the crop: drop that, but not the user's own
  picture's match by date, two releases after F-Droid ships past 0.5.0. 0.5.0 → 0.6.0 (M2,
  2026-10-20): `Store.migrateOnce` with `:core`'s `migrate` (`Migration.kt`, `MigrationTest`), a
  one-time write (not a read fallback: home's record is the unprefixed keys, so a read can't
  make it unknown) at the first `Store` use while `allSources` is missing (page or job, a fresh
  install too), under a lock shared by every `Store`, in one commit: `allSources` on unless
  `sources` is APOD alone; with it on `home`/`lock` = `sources` in the columns `where` named;
  the record copied to `lock.*` with Both and Lock, home's cleared with Lock (unknown: set by the
  first run that needs it); the owner APOD in APOD mode, the picture "own" for 0.5.0's APOD with
  no URL on an APOD video date, the stamp `turnStartedAt`. Drop it two releases after F-Droid
  ships past 0.6.0. 0.6.0 → 0.7.0 (O4b, 2026-11-14; night default, the alternative: accept one
  lost retry per phone): `Store.foldRunGoing`, in `migrateOnce` under the same lock, at every
  `Store` use while `runGoing` or `failedAt` is there: a `runGoing` left by 0.6.0 (the update's
  `install -r` killed a run) adds one to `failedRuns`, so its retry stays; both keys removed, in
  one commit. Drop it two releases after F-Droid ships past 0.7.0.

## Setup and distribution (copied from gridload)

`~/Documents/source99/gridload` is a released app built exactly this way. Copy its build setup
instead of re-deriving it; its CLAUDE.md explains each choice.
- Gradle wrapper, `gradle/libs.versions.toml` (Gradle 9.8.0, AGP 9.4.1, Kotlin 2.4.20, Compose
  BOM 2026.09.00), `gradle/gradle-daemon-jvm.properties` (the system `java` is 27-ea, too new;
  Gradle runs on JDK 21).
- AGP 9 has built-in Kotlin: in `:app` apply only `com.android.application` +
  `org.jetbrains.kotlin.plugin.compose`. `compileSdk 37`, `targetSdk 37`, `minSdk 29` (raised
  from gridload's 26 for saving to the gallery without a permission, see [Save](#save)), Java 17.
  Since minSdk ≥ 28 AGP stores `classes.dex` uncompressed (0.1.x: 1.76 MB instead of 1.0; 0.6.0's
  unsigned APK 2.2 MB); left as is.
- Dark theme, which suits the pictures (`Theme.Material.NoActionBar` + Compose): the system's
  colours (`dynamicDarkColorScheme`) on Android 12+, `darkColorScheme()` below (user, 2026-10-17,
  for an identity; was the stock purple). The problem box's orange stays fixed; the video notice and battery hint use theme colours (U5). Dynamic colour
  follows the wallpaper the app sets: whether a set recreates the page is unchecked (U1; the
  tablet and the FP6 both have wallpaper colours off).
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
- Android SDK in `~/Android/Sdk`. The user tests on real devices over adb; the emulator only for
  the Android versions no device has (10–14; user, 2026-10-29: E; postponed to 0.7.0, user,
  2026-11-02: release 0.6.0 as soon as possible), never for real timing (Doze, buckets, makers' cleaners).
- Git branch `master`, remote `origin`. `dev` holds the night shift's merges until the user fast-forwards master.

**Releases** (F-Droid builds only the newest tag: it ships 0.5.0, 0.3.1 and 0.3.0 and skipped
0.1.2, 0.2.0 and 0.4.0, checked 2026-10-25; its bot finds new tags by itself, skill, Releasing).
What each brought: the tags and `changelogs/<versionCode>.txt`.
- 0.1.0 (2026-10-02): GitHub only. 0.1.1 (versionCode 2, 2026-10-02): first on F-Droid.
- 0.1.2 (3), 0.2.0 (4), 0.2.1 (5): 2026-10-04. 0.3.0 (6): 2026-10-05. 0.3.1 (7): 2026-10-07.
- 0.4.0 (8): 2026-10-16. 0.5.0 (9): 2026-10-18, after a release audit and a release-signed
  phone pass on the FP6.
- 0.6.0 (10): 2026-11-02, after its release gate (Next features) and the tag's build on the FP6.

## Data source (checked 2026-10-01)

**The official API is broken.** `https://api.nasa.gov/planetary/apod` (what the Tasker task
uses) returns `title: "NASA Science"` and the NASA logo as `url` and `hdurl` for every date
tried. Cause: APOD moved from `apod.nasa.gov` to `science.nasa.gov/apod` (301 redirects), and the
API still scrapes the old site. Still so on 2026-11-01 (the explanation real, the title and
picture the logo's; an older date HTTP 500).

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
    `APOD: <date> – ` prefix, year first (`2026 October 28`) or day first (`29 October 2026`, 29 Oct 2026 only; B26), is stripped and HTML entities decoded (`&#8211;`, `&#8217;`; some
    titles have a plain `–`, one had `-` with no space after it, 3 Apr 2026 had no day number).
  - `featured_image.file`: the picture (image days only; missing on the video day 16 Oct 2026).
  - `link`: the post's page, for "open on the web".
  - `content.rendered`: for the video check and the explanation.
- The full response (about 21 KB per post), not `_fields=` (which drops `featured_image`).
- Image sizes for 1 Oct: `featured_image.file` → 1280×853 (115 KB);
  `https://assets.science.nasa.gov/content/dam/<same path>` → the original, 1600×1067 (1.1 MB);
  `?w=4096` only upscales. On other days the original is much larger (2026-10-04, 20 posts:
  54 KB to 10 MB, up to 5815 px), and `?h=` gives a screen-sized rendition (the daily download since 0.5.0, see Fetch).
- **Not always a small JPEG:** on 2 Oct 2026 it was a 37.7 MB PNG, 4455×5592 (`?w=1600` → 5.3 MB,
  still PNG); about 6 min on mobile data. Decoded in full it's a 99.6 MB bitmap, just under
  Android's 100 MB drawing limit, so the page decodes with `inSampleSize` (`decodeForScreen`,
  user 2026-10-02). Its rendition (`?h=2484`) is 8.1 MB, 1979×2484, still PNG: `fm=jpg`,
  `fmt=jpeg`, `format=jpg` and `auto=format` are ignored (CloudFront, checked 2026-10-16).
- **Video days** (about 1 in 9 posts): `featured_image` is usually there, but on 4 of 11 video
  days checked it's a generic NASA image, not a still frame, and on 16 Oct 2026 (the first real
  video day after the release) it was missing; 0.3.1 failed on it all day. So no still frame
  (user, 2026-10-16): `imageUrl` is null on video days and nothing is downloaded. **Detection (2026-10-01):** the hero
  block, from `media-detail-hero__media` to the next `<h1` (the title), holds `<img>` on image
  days and `<video>` (mp4) or `<iframe>` (YouTube) on video days. Checked on all 100 posts 24 Jun
  to 1 Oct 2026: never both, never neither. YouTube links in the explanation (about 1 in 3 image
  days) are outside the hero.
- **Explanation:** the hero's `media-detail-hero__description` paragraph up to its first `<br>`
  (after it come site notes and "Tomorrow's picture"), tags stripped, without the label. Present
  on all 100 posts, 660 to 1323 characters.
- **Credit** (0.6.0, F4, 2026-10-20; `Post.credit`, APOD only): the first hero
  `media-detail-hero__meta-row` whose `<th>` holds "Credit", as "<label>: <value>" with the
  post's own label (`Credit`, `Credit & Copyright`, `Credit & License`, `Credit and Copyright`,
  `Color Credit`; one colon), or the value as is when it has its own ("Image Credit: …",
  "Video Credit: …", 35 of 100 under a plain "Credit"). `<br>` → "; ", tags stripped, entities
  decoded, and the gaps stripped tags leave tidied (" ,", " :", " )", "( ", "; ;", ",;").
  All 100 posts have one, 12 to 254 characters (`SourcesTest.readsEveryRealPost`). NASA's feed
  names a credit in 1 of 60 items, Earth's only in `content` (about 100× the data per check):
  null for both (user, 2026-10-20).

Risk: this is the site's internal WordPress API, not a documented public API. The category id or
the fields could change. Keep the parsing in `:core`, tolerant (`ignoreUnknownKeys`, nullable
fields), and fail without touching the current wallpaper.

**Traffic (checked 2026-10-02; the checks by the clock since J2, 2026-10-23):** a check is one
JSON GET per source in use, plus the image when the post is new (`Store.isCurrent` skips a
source whose stored post is today's). The job checks at each change time ("New picture at",
with turns every "Change every" from it), 15 min after it and hourly for the 3 hours after it
and at their end (06:00, 06:15, 07:15, 08:15, 09:00; B15), and after a
failed run 15 min later, then hourly; the 6 h fallback job adds up to four more a day (no hourly
checks while a screen waits since RO1, 2026-11-14: turns don't wait). So about 4–8 checks a day
with one source, more with turns (each change time's own checks). With "As soon as it's out"
on (SO2, default off) the job checks hourly all day, about 24 checks a day per source in use
(NASA's feed 49 KB, APOD's post 21 KB; the picture only when new). Every phone of a time
zone has the same change times; a per-device offset (0–10 min, drawn once) spreads each burst
over 10 minutes. 1000 users ≈ 5000–10000 requests a day, spread over the day by the time
zones. The risk is fragility, not load. Since 0.5.0 (WP7c, 2026-10-18) NASA and Earth have no
post on weekends, so their checks repeat: a page open skips a source checked without a problem
within the hour (The page); the job's runs always check. A picture that isn't one is
downloaded once (Fetch).

**User-Agent (2026-10-02):** both requests send
`APODroid/<versionName> (+https://github.com/buerlino/APODroid)`, so the site can tell this app
apart. `:core` takes it as a parameter; `Store` reads `versionName` from the installed package,
so the version lives only in `app/build.gradle.kts`.

The Tasker task (for reference only) fetched `api.nasa.gov` with retries, downloaded `url`,
set it as wallpaper, and on video days kept the old wallpaper.

### Other sources

(Checked 2026-10-16, for 0.5.0.) Same rules as APOD: public, no key, tolerant parsing in
`:core`. Real responses in `private/` (`iotd-feed-2026-10-16.xml`, `eo-last40-2026-10-16.json`).
- **NASA Image of the Day:** the RSS feed `https://www.nasa.gov/feeds/iotd-feed/` (the old
  `lg_image_of_the_day.rss` redirects there): 49 KB, the newest 60 items, newest first. Not the
  WordPress API: www.nasa.gov's `image-article` posts have no category for it, and 2 of the 18
  newest weren't in the feed. Each item has `title` (entities such as `&#039;`, once a leading
  space), `link` (an `image-detail/` page), `description` (plain text, 97 to 607 characters: the
  explanation), `enclosure` (url, length, type: the original) and `pubDate`
  (`Fri, 16 Oct 2026 16:33 GMT`, no seconds). No CDATA. Weekdays only, posted 14:00–22:00 UTC;
  some weekdays have none. All 60 are pictures (JPEG, PNG, one WebP typed `image/jpeg`), 87 KB
  to 96.7 MB, median 4.7 MB. 56 are on `www.nasa.gov/wp-content/uploads/`, where `?h=2484` gives
  a screen-sized rendition (6000×4000 → 3726×2484, 0.8 MB; a PNG stays PNG, 13 MB); 4 on
  `images-assets.nasa.gov` (`~large.jpg`, 1920 px, `?h=` ignored).
- **Earth Observatory Image of the Day:** moved to science.nasa.gov (earthobservatory.nasa.gov
  answers with 301s), the same WordPress as APOD:
  `GET https://science.nasa.gov/wp-json/wp/v2/posts?science-org=22453&per_page=1&_fields=date,link,title,excerpt,featured_image_url`
  (taxonomy `science-org`, term 22453 `eo-image-of-the-day`, 9450 posts). 0.6 KB; 97 KB
  without `_fields`. `featured_image_url` is a thumbnail (`…_th.jpg?w=720&h=480…`); without the
  query and the `_th` it's the post's lead picture (`_th` on 40 of 40, HTTP 200 on 10 of 10).
  `excerpt`: one sentence (128 to 155 characters), the explanation. `date` US Eastern,
  00:00–00:01, Monday to Friday only. Originals (`content/dam`) 0.2–2.9 MB, 720–2160 px wide,
  mostly 1:1 to 3:2, one 1440×600; `?h=2484` 0.3–1.5 MB (upscaled). No video posts. The old
  Earth Observatory RSS redirects to a feed that mixes in Photojournal posts: not usable.
- NASA History Image of the Day is on Flickr: left out (user, 2026-10-16).
- **Parsing (2026-10-16, WP1):** `fetchLatest(source, userAgent)`. The NASA feed is read with
  regexes, as the APOD HTML is (no XML library: the JDK parser's safe settings differ on
  Android); `pubDate` (`RFC_1123_DATE_TIME`, seconds optional) becomes the US Eastern date, so a
  post at 03:00 GMT counts for the day before. Earth Observatory's picture is
  `featured_image_url` without its query and `_th`. A missing title, link, picture or a bad
  date is `SiteChanged`. All 60 feed items and 40 Earth posts in `private/` parse
  (`SourcesTest.readsEveryRealPost`, skipped without `private/`). `renditionUrl(url, h)` adds
  `?h=` on the two scaling hosts; `originalUrl(post)` is `content/dam` for APOD and Earth, the
  enclosure for NASA. File names start `APOD_`, `NASA_`, `EO_`.

## The app (decided 2026-10-01 unless dated)

- **One page.** Top: the source's name, then today's picture, full width, with its title and
  date; tap opens the APOD page in the browser. Below: the settings. From 600 dp wide (user,
  2026-10-17: one column was too wide on tablets and in landscape) two panes, about 55/45,
  each scrolling on its own: the picture side (the source's title, picture, boxes, caption) left, the settings
  right. Pull to refresh works on the picture side only. The tablet (711 dp in portrait) shows
  two panes both ways.
- **Settings** (SharedPreferences; the layout and texts approved by the user from the tablet's
  screenshots, 2026-10-20 to 24), in order: **Wallpaper**: the daily switch; the status lines
  (while the stored daily switch is on); **Setup**, a row that opens and closes the draft's
  settings, as a row in Android's Settings (U0, user, 2026-11-02, from the mockup: the old
  "Setup ▸" header looked like the heading "Wallpaper" and didn't look tappable): the title in
  `bodyLarge` and the normal text colour, the summary under it while closed (`bodyMedium`,
  `onSurfaceVariant`), "▾" (closed) or "▴" (open) at the right at 30 sp (Roboto draws them
  small), the whole row tappable with its ripple across the pane's full width (`bleed` into the
  column's padding), at least 56 dp; a button for screen readers, "expanded"/"collapsed". The
  summary is of the **stored** setup, grouped by screen (U0): "APOD, NASA · both screens"
  while both screens have the same sources (APOD mode with Both: "APOD · both screens"), "APOD ·
  home screen" or "… · lock screen" while only one has any, else "Home: APOD, Earth · Lock:
  NASA, Earth" (the sources in the list's order, OR2: "NASA, APOD · both screens"); then, while
  the daily switch is on, " · every 6 h from 06:00" while a screen
  takes turns, else " · 06:00", 12/24 h as the system says. At its top the switch **More NASA pictures**
  (Setup is the draft, Advanced acts at once), then in APOD mode **Where**, with the switch on
  **Pictures from** (the list); while the daily switch is on **New picture at**, "As soon as
  it's out" under it, then, while a column takes turns, "Change every", and "If APOD is a video";
  then, always, "Darken bright pictures". Closed at a fresh start; it
  opens by itself while the draft differs from the store in it, when "More NASA pictures" is
  turned (the area swaps), and closes after a Set that saved (also "Setup saved. Nothing to set
  yet."; a failed set leaves it open). Its state and Advanced's survive a rotation (the saved
  state). **Set as wallpaper**, always, after Setup. **Advanced settings**, the same kind of
  row (no summary), closed by default, only while the daily switch is on (empty otherwise):
  "Save every picture" with "Largest original" under it while on, then "Keep running" (O11).
  - **New picture at** (the label and the times approved by the user, 2026-10-24): a dropdown
    as "Largest original", the button "06:00 ▾" (12/24 h as the system says: "6:00 AM ▾"), a
    menu of 00:00 … 23:00 with ✓ before the chosen one, screen readers "New picture at: 06:00".
    The pref `pictureAt` (Turns and the picture time), default 06:00. Part of the draft (Set
    arms the alarms afresh); shown only while the daily switch is on, and while hidden it goes
    back to the stored one (as "If APOD is a video").
  - **As soon as it's out** (SO2, 2026-11-14; the label and the ⓘ approved by the user from
    SO3's screenshots, 2026-11-14; `soon-0.7.0.md`): a switch under "New picture at" with a
    small ⓘ ("About As soon as it's out"; its dialog "A new picture goes on within the hour
    after it's out, not only at the change time."), the pref `soon`, default off (0.6.0's
    window). On: a newer picture of the source a screen shows goes on whenever a check sees
    it, with hourly checks all day (Turns and the picture time, rule 2). Part of the draft, as
    "New picture at": shown only while the daily switch is on, back to the stored one while
    hidden, the highlight (label and switch in `tertiary`, "On, not set yet"). Setup's summary
    doesn't name it (user, 2026-11-14: the row stays short).
  - **Darken bright pictures** (BR2, 2026-11-14; the label and the ⓘ approved by the user from
    BR2's screenshots, 2026-11-14; `bright-0.7.0.md`): a switch after "If APOD is a video" with
    a small ⓘ ("About Darken bright pictures"; its dialog "Darkens a bright picture a little, so
    the clock and widgets stay readable."), the pref `darken`, default on (user, 2026-11-14:
    most people never open Setup; also for an update from 0.6.0, no migration). On: each set
    darkens a bright picture (Wallpaper, the darkening). Part of the draft, as "As soon as it's
    out" (the highlight, the saved state), but shown also while the daily switch is off (night
    default: a one-off Set darkens too, so the page shows what it uses; never hidden, so never
    put back). Set sets each screen afresh, so a change shows at once. Setup's summary doesn't
    name it (as "As soon as it's out"). Before L6's Size row; L6 puts Size before it.
  - **The highlight**: each draft setting that differs from the stored setup shows in the
    theme's `tertiary` until Set: its heading, a segmented row's chosen segment
    (`tertiaryContainer` / `onTertiaryContainer`) and the row's borders (`tertiary`), the
    list's row (tick box, name, dropdown) where that source's screens differ, a list row's "≡"
    where its place differs from the stored order (OR2), the video
    dropdown's border and text (another choice, or a newly picked "my picture" while it's
    chosen), the mode switch and its label, crop mode's "Whole picture" toggle while it differs
    (not ⛶ since U0: ✓ saves the part at once), the
    "Setup" row's title while anything in it differs (the shown mode's "Where" or list and its
    order, the
    mode switch, "New picture at", "As soon as it's out", "Change every", "If APOD is a
    video", "Darken bright pictures"; not ⛶). Not the daily
    switch, nor Advanced (they act at once). Screen readers: a highlighted control's state adds
    "not set yet" ("Selected, not set yet", "Checked, not set yet", "On, not set yet"; the rows
    "collapsed, not set yet").
  - **Set applies the setup** (user, 2026-10-20 evening; why: a change took effect at once,
    "but maybe I'm not done"). **A draft until Set:** "Where", the list (ticks, dropdowns and
    order),
    "New picture at", "As soon as it's out", "Change every", "If APOD is a video", "Darken
    bright pictures", a newly picked "my picture" and the switch "More NASA pictures" change only the page
    (`MainActivity`'s draft state; no pref is written, no set, no scheduling). **A hidden
    setting goes back to the stored one** (user, 2026-10-22: else Set saved a hidden "Change
    every", and "Setup" stayed highlighted with nothing in it highlighted): "Change every" while
    it's hidden (no column with two or more, or the daily switch off), "New picture at", "As
    soon as it's out", "If APOD is a video" and a newly picked "my picture" while the daily switch is off; so Set
    saves, and "Setup" highlights, only what the page shows. A rotation and the system killing
    the app keep the draft (`onSaveInstanceState`); closing the app drops it (a start without a
    saved draft deletes `files/fallback-draft.jpg`). **One page:** another start while the page
    is on top (the launcher, a shortcut, `am start -n`) reuses it (`launchMode="singleTop"`;
    without it a start whose intent differed from the task's first put a second page on top,
    without the draft). A start while another app's screen is on top of the page (the photo
    picker from "Change picture", App info) still stacks a second page, without the draft
    (`singleTask` would end a pick); its start keeps the draft's picture while another page is
    alive (`MainActivity`'s `pages`, `onCreate` to `onDestroy`). One draft file for both: a pick
    in the second page replaces the first page's picture; a Set renames it to `fallback.jpg`
    only when its own draft holds the pick (`draftFallback`, `Store.saveSetup`'s `pick`), else it
    leaves `fallback.jpg` and the draft file, for the page that owns it.
  - **The daily switch** ("Change automatically" since U0, 2026-11-02, under the heading
    "Wallpaper"; "Change wallpaper automatically" before, user 2026-10-17: with turns it isn't
    daily; the pref `daily`) **acts at once, both ways** (user, 2026-10-20 evening: a
    setting that changes how the whole app works must act at once): off writes `daily` and
    cancels the job and its alarms; on writes it, starts the stored setup afresh
    (`Store.startAfresh`: `restartAndForget` with the stored columns, both records' pictures
    forgotten, the change times kept), `update(page = true, show)` sets the **stored** setup
    while `daily` is still on, then the job is scheduled as after Set; the other draft changes
    stay a draft. Toasts "Wallpaper set" or "Couldn't set the wallpaper", none when nothing was
    set (the status line says it). Turned on while a Set, an update or its own run goes: one run
    after it (beside it, either run could set first and the other's toast was wrong); a page
    update meanwhile waits for it, and Set is disabled while it runs. Off is off: off drops a
    waiting run, and a running one restarts nothing and sets nothing more once `daily` is off.
    A set that has begun ends (a screen's set can take seconds with the whole picture); the
    next one doesn't start: before each set after the first, an update without Set's `set` finds
    `daily` off and ends there, what it did kept, no failure, as a run that gave up (O4c,
    2026-11-14, review-0.7.0-runs finding 2; the page's load too; the job's runs stop by
    `onStopJob` as before), so B6's last on still runs.
    What the draft's switch hid (the rows below) follows the one switch.
  - **Two kinds of state on the page:** the controls and the sources the page offers (the
    title's dropdown and the swipe) follow the draft, so turning "More NASA pictures" on offers
    and fetches all three at once (`Store.update`'s `show`); what reports what is set follows
    the stored setup (`Store.setup`): the status lines, the chip, the video notice, the problem
    box, the battery hint, the opening page. **The job follows the stored setup only:**
    `onResume`'s `DailyJob.ensure` reads `store.daily`, the stored mode and columns; only Set
    and the daily switch schedule or cancel (Set by the stored switch, after saving the draft).
    **At once:** Advanced ("Save every picture", "Largest original": they change no picture;
    "Keep running", which starts or stops its service),
    ★, and ⛶'s ✓ (U0, 2026-11-02: Choosing the part).
  - **Set as wallpaper**: one button at the end of the Wallpaper section, both modes, the daily
    switch on or off, no Home / Lock / Both menu, enabled unless a Set or the daily switch's run goes. It saves the whole
    draft in one commit (`Store.saveSetup`: the settings, the list with its `lastScreens`, the
    parts, "my picture" renamed from `fallback-draft.jpg` to `fallback.jpg`), forgets the
    records' pictures, the change times kept (`:core`'s `restartAndForget`; Turns and the
    picture time), then `update(page = true, show, set = true)` fetches what a newly offered
    source needs and sets each screen afresh (a changed whole picture or "my picture" too, the
    same picture again), also with the daily switch off (a one-off set of the same setup), then
    arms the alarms and the fallback job or cancels them as the stored daily switch says (after
    the set: a freshly scheduled fallback runs at once and would set first; `DailyJob.ensure`
    leaves a fallback with the same timing alone and stops no running run). The shown picture
    decides nothing: to put Earth on a screen, tick Earth. A source whose fetch fails: the setup
    is saved anyway and each screen gets what has a picture (Turns). Toasts, the first that
    applies: "Couldn't save the setup. Check the free space." when `saveSetup`'s commit wasn't
    written (a full disk: the setup lives only in memory until the process dies; the sets still
    run, Setup stays open); "Couldn't set the wallpaper" when a set failed (also when another
    worked); "Wallpaper set" when a screen was set since `saveSetup` (`Store.setsSinceSetup`),
    also by a page load or the job that got there before Set's own update; else "Setup saved.
    Nothing to set yet." (an APOD video with "Skip it" alone, or nothing stored and the fetch
    failed: the problem box says why). With the daily switch on the job sets the rest later. A
    rotation or Back during Set's run or the daily switch's keeps its end (B13): the runs live in
    the process, not the page; the job and the log need no page, the redraw and Setup closing go
    to the page that started the run or the one a rotation made of it, and Set stays disabled in
    every page while either runs. After Back Android suppresses the toast (the app is in the
    background and has no notification permission).
  - **More NASA pictures** (the pref `allSources`, default off: APOD mode; the name the user's,
    2026-10-20: the mockup's "Extended API mode" is a developer's word). Part of the draft:
    "Where" and the list swap at once and the page offers the new mode's sources; Set saves it
    and arms the alarms afresh (the change times follow the columns). Each mode keeps its
    settings: "Where" is APOD mode's, the list the other's (`Store.list`, kept in APOD mode
    too). The first time on, the list starts from the draft's "Where" (the list is saved only
    once it was on: `Store.hasList`), also after a Set in APOD mode changed "Where". Switching it
    off and Set leaves a screen "Where" doesn't name alone, with its record, so the chip still
    knows it.
  - **The list** (switch on; from the user's sketch, 2026-10-20): "Pictures from", a row per
    source: a tick box with its short name, "APOD", "NASA", "Earth", as in the title above the
    picture (`shortLabel`; user, 2026-10-20: the full names took three lines on the tablet), and
    a dropdown Home screen / Lock screen / Both (an outlined button "Both ▾" and a
    `DropdownMenu`, ✓ before the chosen one, as "If APOD is a video"'s); the dropdown wraps under
    the name when both don't fit on one line. A tick puts the source on its dropdown's screens,
    an untick takes it off both, a new choice for a ticked source moves it (`:core`'s `placing`,
    in the draft). An unticked row's dropdown is greyed out and shows the screens the source had
    last (`<source>.lastScreens`, written with the list by Set; the draft's while unsaved; Both
    when it never had any); ticking it puts the source there. The last ticked row can't be
    unticked (greyed out); its dropdown still moves it. Screen readers: the row "NASA" (a
    checkbox), the dropdown "NASA on: Home screen". **The order** (OR2, 2026-11-14, user: "must
    be sortable using an icon to drag up and down"; `order-0.7.0.md`): the rows stand in the
    list's order, the turns' order (Turns and the picture time), and each has a handle "≡" at its
    start, before the tick box (a text glyph in `onSurfaceVariant`, `tertiary` while the row's
    place differs from the stored order; a 48 dp target; on all three rows, ticked or not, also
    the last ticked one): a vertical drag on it (no long press; `detectVerticalDragGestures`)
    lifts the row (`zIndex`, `graphicsLayer`'s `translationY`, a shadow) and swaps it with its
    neighbour once it has passed half that row's height; the handle consumes the drag, so the pane
    doesn't scroll while a row moves and still scrolls from anywhere else. Screen readers: the
    handle is hidden, the tick box has the actions "Move up" (not on the first row) and "Move
    down" (not on the last), which move it as the drag does; ", not set yet" when the row's
    place or screens differ. Part of the draft (the saved state, `setupChanged` while the list
    shows), kept when "More NASA pictures" is turned off and on in the draft, written by Set only
    while the list shows (in APOD mode it changes nothing). The prefs `order` (a comma list of
    all three, `NASA,APOD,EARTH`; default APOD, NASA, Earth; read tolerantly, `:core`'s
    `orderOf`) and `orderFrom` (the anchor, a local date; Turns and the picture time). The
    title's dropdown, the swipe, the problem box and "Save every picture" keep APOD, NASA, Earth
    (the sources the page offers, not the turns). The prefs are the columns `home` and `lock`
    (comma lists, `Store.list`; `Store.columns` is the list with the switch on, else APOD on the
    screens "Where" names, `apodColumns`). With a column of two or more, while the daily switch
    is on, **Change every** [24 h | 12 h | 6 h | 3 h] (`Store.interval`, the pref `interval`,
    default `DAY`, 24 h) below the list, with an ⓘ ("About Change every", RO2, 2026-11-14; the
    user's Open 3: the rotation is what confused) whose dialog says "Every 6 h from 06:00, each
    screen switches to its next source in the list; a new order starts with the top one at
    06:00." with the draft's values (OR2; a placeholder until the user approves it); hiding it moves the page only after a tick. While the
    daily switch is off "Where" or the list still shows, so a one-off Set uses settings the user
    can see.
  - **Weekday dialog** (user, 2026-10-17): ticking NASA or Earth where it wasn't ticked on
    either screen says they post on weekdays only (B25: not that Friday's pictures come back on
    weekends, false since J1: a screen never shows a picture twice); "Dismiss" (again at the
    next such tick) and "Don't show again" (`weekdayNoticeHidden`). Not for the video-day choice
    or the mode switch. No text on the page.
  - **Where** (APOD mode's only; the list has its columns): home screen, lock screen, or both
    (user: a setting, not fixed). Default: both. Its segments, like "Change every"'s, share one
    height when a label wraps (in the FP6's landscape pane only "Home screen" wrapped).
  - **If APOD is a video** (one setting for both modes): a dropdown (an outlined button "Skip it
    ▾" and a `DropdownMenu`, ✓ before the chosen one), under its heading after "Where" or
    "Change every" in both modes, hidden while the daily switch is off; with the switch "More
    NASA pictures" on greyed out while APOD has no tick (so the page doesn't move), never greyed
    out otherwise. A no-break space before every "▾" (the dropdowns, the title) keeps the glyph with the last word (user, 2026-10-21: at font scale 2.0 and a
    phone's width a "▾" wrapped alone). The choices: "Skip it" (`VideoDays.KEEP`, the pref's
    name from 0.5.0's "Keep"), NASA Image of the Day, Earth Observatory (`VideoDays.NASA`,
    `.EARTH`), or "Use my picture" (user). Default: "Skip it", as the Tasker task did. The choice fills APOD's turn on every
    screen where APOD is ticked, with turns too ("Skip it": a screen with APOD alone keeps its
    picture, one with turns passes to the next source; Turns). The other source is fetched only
    on APOD video days (after APOD's refresh, which tells); its newest post is the wallpaper,
    set once per its date; choosing it in the draft on a video day fetches it for its page, and
    Set puts it on; none stored yet → kept. No still-frame option (often a generic NASA image).
    "My picture" is picked with the system photo picker (`PickVisualMedia`, no permission) and
    copied to `files/fallback-draft.jpg` (a rotation during the copy keeps the pick and the
    choice: the copy runs in the process, not the page); Set renames it to `files/fallback.jpg`. Choosing "Use
    my picture" with no picture in either file opens the picker at once and the choice is made
    only once a picture is there (backing out keeps the one before); while it's chosen, "Change
    picture" beside the dropdown ("Pick picture" for 0.5.0's choice without a picture, which
    keeps).
  - No help texts (user: the labels explain themselves), except the pause hint (Background
    update), the video-day notice, the detector's box (Background update, O14) and the ⓘs (Save every picture, Save; Change every, RO2; As soon as it's out, SO2; Darken bright pictures, BR2; Keep running, Background update).
- **Video days on the page (user, 2026-10-16):** a black 3:2 tile with ▶ instead of the picture;
  tapping opens the APOD page in the browser (no player). No ⛶, no ★. A neutral box below the
  tile (`surfaceVariant` fill, `outline` border) says what the video day does to the wallpaper;
  the text follows the stored settings (daily off / keep / my picture with or without a
  picture), also with turns. The texts (U0, user, 2026-11-02, from the mockup; short, "Tap ▶ to
  watch it on NASA's site" dropped, ▶ says it), the same in both modes: what takes APOD's place,
  "This APOD is a video: your picture instead.", "This APOD is a video: NASA's Image of the Day
  instead." once the stand-in's picture is stored (`standInReady`), "…: NASA's Image of the Day
  instead, once loaded." while it isn't; with "Skip it" "This APOD is a video: wallpaper
  unchanged. Change: Setup › If APOD is a video" in APOD mode, "This APOD is a video: skipped.
  Change: Setup › If APOD is a video" with the switch on; "Use my picture" without one "…:
  wallpaper unchanged (no picture chosen). Change: …" (with the switch on "…: skipped (no
  picture chosen). …"); with the daily switch off, or APOD ticked nowhere, only "This APOD is a
  video.". A stand-in says "instead" also when the screen has shown it before (RO2, 2026-11-14:
  it may repeat since RO1; B22's "wallpaper unchanged."/"skipped." for it and `standInSkipped`
  went).
  "Instead" (was "takes its place"), not "is the wallpaper" (user, 2026-10-24): the wallpaper
  changes at the next check or change time, e.g. all evening after a 21:05 video in Los
  Angeles. "Dismiss" hides it for that date (`videoNoticeDate`), "Don't show again" for good
  (`videoNoticeHidden`, no confirmation). The second help text besides the pause hint (the problem box is a status, not help). `refresh()` stores the
  entry without a picture and deletes the previous `apod.jpg`; `isCurrent` doesn't need one.
- **Fetch** in `:core`: newest post → `Post(source, date, title, imageUrl, pageUrl, isVideo,
  explanation, credit, published)` (`Source` APOD, NASA, EARTH; 0.5.0). `published` (B15,
  2026-10-24): when the post came out, APOD's and Earth's WordPress `date` with its time read as
  US Eastern (`2026-10-23T00:05:00` → 04:05 UTC; null when the time doesn't parse, the date still
  counts), NASA's `pubDate`; on all 200 real posts in `private/`, on the post's date. `Store`
  writes it with the post as `<source>.published` (epoch ms); a post stored before B15 has none
  and counts as published at its date's 00:00 US Eastern (`Post.publishedAt`, no migration).
  The picture is downloaded as tall as the screen (`renditionUrl(imageUrl, screen.height)`,
  0.5.0; [Sharper wallpaper](#next-features)); if that fails, APOD and Earth fall back to the
  plain `imageUrl` (1280 px for APOD), NASA fails (its plain URL is the original, up to 97 MB).
  It goes to the source's file, `files/apod.jpg`, `nasa.jpg` or `earth.jpg` (whatever its
  format; no storage permission; 0.5.0), via a `.part` file, and replaces the old one only when
  complete and decodable. One that isn't an image
  (all URLs tried) is remembered (`<source>.badUrl`, user 2026-10-17): while it's the newest
  post's, nothing is downloaded and the check fails as `SiteChanged` at once (each retry would
  otherwise fetch it again, 90 MB in the first hour for a 13 MB PNG); a stored picture clears
  it. A URL that doesn't parse is `SiteChanged` too, not the network. Decoded with `BitmapFactory`
  (no image library). The current entry's fields in SharedPreferences. A post older than the
  stored one is ignored (2026-10-04, a guard against a stale cache; never seen). Known gap, left
  (user, 2026-10-04): the picture is renamed a few ms before the prefs are written; death in
  between leaves today's picture under yesterday's entry until the next refresh fetches it again
  (declutter file, Review of 0.1.2, 2.2).
- **The page** runs `Store.update` on each `onResume` (see Background update): fetches unless
  the stored post is today's (US Eastern) or, page opens only (user, 2026-10-17; WP7c), its
  source was checked within the hour with no problem since and its picture is there (so a new
  APOD reaches an open page up to an hour late; not the job: a page open just before a new
  post would hold the wallpaper back until the job's next run) and, while the stored daily switch is on, sets a new one at once; it
  also redraws when the job stored a new one in the background, and while open it follows the
  job's sets, stored posts and problems on either screen (a prefs listener while resumed; not
  during a load, a Set or the daily switch's run, which redraw at their end: else a Set showed
  the home screen's new line beside the lock screen's old one; a change while it redraws is
  read once more). **Pull to refresh** (user,
  2026-10-16; material3's `PullToRefreshBox`) always fetches, also when it comes during a
  running update (the run after it keeps the force); a new picture is set at once while
  the stored daily switch is on, nothing is set while it's off. **A stopped page lets its pictures
  go** (O12, 2026-11-14; O7: with Keep running or Live the process lives for days): `onStop`
  recycles the decoded pictures (the composition would hold them while stopped), `onResume`'s
  load decodes them again (also while a load runs), each page keeping its height meanwhile (an
  empty box of the picture's shape); the draft, ⛶'s part, the shown source and Setup's and
  Advanced's state stay. A changed title or explanation for the same
  date is stored. A new date closes crop mode, a same-date change doesn't. A failed refresh keeps the
  stored picture; only with none is there an error with a retry button. Video days show "Video"
  after the date. The page fetches the sources it offers (`update`'s `show`: the draft's mode's,
  so a source ticked in the draft only has a picture), besides those in use.
- **Problems and status lines** (texts approved by the user, 2026-10-18 and 20):
  `:core` throws `SiteChanged` (not the JSON expected, no post, a bad date, an image day without
  a picture; `Store` adds a download that isn't an image), `HttpStatus(code)` and
  `StorageFailed` (the file can't be written or renamed, a full disk: "Couldn't store the
  picture. Check the free space.", no `badUrl`); any other failure counts as the network, a read
  timeout too (`SocketTimeoutException` is an `InterruptedIOException`, which a stop check would
  take for a stopped job: `isStop()` in `Store` tells them apart). Each `Store.update` (page,
  pull, job) stores how its check went, per source (`nasa.problem`, …): a success (a refresh, or
  today's post already stored) sets `checkedAt` and clears `problem`; a failure stores `problem`
  (and `httpCode`). One box: in APOD mode the first offered source with a problem (APOD's
  first); with the switch on (all three fetched) the shown source's, else the first ticked
  source's, named ("NASA Image of the Day: …"; its `problemNoticeKey` starts `nasa:`) unless
  it's APOD's with APOD ticked alone. The texts (U0, user, 2026-11-02, from the mockup; the
  subject once: "NASA's site" unnamed, else the source's name): "NASA's site: no connection
  since 27 Oct." ("Earth Observatory: no connection …"), "NASA's site: error 503. Trying again
  later." ("…: error 503 since 27 Oct. Trying again later."), "NASA's site has changed (nothing
  new since 27 Oct). An app update may be needed." ("Earth Observatory has changed (…). …"),
  the storage one as it was, named with a prefix. A failed set isn't a problem (the toasts
  and the job's retry cover it). While `problem` is set, it shows in the orange box below the
  picture or tile (orange for problems only: a failing app must look failing, the other boxes
  are neutral), with "since <day>" when `checkedAt` is more than a day old. Only "Dismiss": it
  hides that problem until its kind or HTTP code changes or a new day starts
  (`problemNoticeKey`, e.g. `http:503:2026-10-16`); no "Don't show again", a failing app must
  not look like a working one. **All boxes that apply show at once** (user, 2026-10-16), stacked
  below the picture or tile, above the title: problem, video notice, the detector's box, battery
  hint (Background update), each with its own buttons. The empty state shows the text above
  "Try again", no problem box, but the detector's box and the battery hint below it. So on a day whose post is stored, opening the
  page clears a pull's failure: nothing is wrong until the next picture is due. **The status
  lines**, under the switch while it's on (texts since U0, user, 2026-11-02): "Changed 16 Oct, 06:12" (`wallpaperSetAt`,
  any successful set; 12/24 h and the time zone as the system; the zone followed while the page
  is open, 12/24 h at the next resume), with " · NASA" when the wallpaper isn't APOD's or two or
  more sources are ticked (the source's short name, user, 2026-10-20: the full names wrapped) and
  " · My picture" for the user's own, in both modes; or "Video day, previous kept" (the user's
  words; `:core`'s
  `videoDayKept`: APOD ticked there, its turn, and nothing will take APOD's place there (B23,
  user, 2026-10-26): "Skip it" or "Use my picture" without one (B22's "the stand-in shown there
  before" went with RO1: it goes on again); a stand-in that will go on, stored and not set yet (until the
  next check or change time) or still loading, isn't "kept": the line stays the screen's last
  change, or none before the first set; with turns "Skip it" passes
  the turn, so nothing is kept; a record without a picture, as Set leaves it, counts when the
  screen was set before, `wallpaperSetAt`, else not: a fresh install's first video day shows no
  line); before the first set nothing. In APOD mode for the screen "Where" names (with Lock the
  `lock.*` record). With the switch on one line while both screens show the same picture, else a
  line per screen set or kept: "Home: 16 Oct, 06:12 · NASA", "Lock: video day, previous
  kept"; a screen never set has no line. The page re-reads both around each
  update and after a set.
- **The title and the swipe** (from the user's mockup of 2026-10-20 evening; the texts "APOD ▾",
  the menu's short names and "Picture from: …" approved by the user, 2026-10-21, with the
  failure text on the shown page only and a fresh pager when the list changes). The sources the page offers: with the switch "More
  NASA pictures" on always three, else (APOD mode) APOD and, on an APOD video day, the video-day
  source. Above the picture the shown source's short name ("APOD", "NASA", "Earth";
  `headlineSmall`); with two or more a "▾" and a `DropdownMenu` of the short names, ✓ before the
  shown one (screen readers "Picture from: NASA Image of the Day", a dropdown list); with one the
  name alone. The title and the picture (or the ▶ tile, with ⛶, ★ and the chip) are a
  `HorizontalPager` over those sources: a swipe shows the previous or the next, sliding with the
  finger and stopping at the ends; in crop mode the pager doesn't move (a drag moves the frame).
  Each page draws its own source's stored post, so a neighbour shows while dragging (`show()`
  loads every offered source's post and decoded picture, at most three in memory; one already
  decoded for the same date and picture URL stays). The page that settles is the shown source;
  the dropdown moves the pager (`animateScrollToPage`). The boxes and the caption below the
  pager show the shown source only, so the page below doesn't change while dragging. It opens
  on the **picture's** source (a stand-in's own page, not APOD's; the user's own picture has
  none) on the home screen, else the lock screen's, else APOD (APOD mode: the screen "Where"
  names, `pageScreen`) until the user picks one (dropdown or swipe); the pick survives rotation,
  the switch "More NASA pictures" turned off and on in the draft (user, 2026-10-21, as a
  rotation) and the system killing the app (Android's saved state, seen after `am kill`), not a
  swipe-away from Recents (P checks it on the FP6).
  The shown source drives ⛶'s part, the boxes, title, date and explanation; another one closes
  crop mode. With two or more sources a source line above the title, "NASA
  Image of the Day · 17 October 2026" (" · Video" on video days), replaces the date line below
  it, and a chip at the picture's top left (⛶'s and ★'s dark style; none on the ▶ tile, hidden
  in crop mode) marks the post on a screen, from the records (`Store.screenState`): with the
  switch on "On home screen", "On lock screen" or "On wallpaper" (both); in APOD mode "On
  wallpaper" for the screen "Where" names. With one source the caption is title, then date. The pages follow the draft (Settings, Set applies the setup); the chip and the
  opening page the stored setup. The boxes show for every source; pull refreshes every source in use and covers the picture side
  (the left pane on wide screens).
- **Credit** (approved by the user, 2026-10-20): one line under the date
  (APOD mode: title, date, credit) or, with two or more sources, under the title on APOD's page; smaller and
  greyer than the explanation (`bodySmall`, `onSurfaceVariant`), one line with an
  ellipsis while the explanation is closed, all of it while the explanation is open (a cut credit makes the caption
  expandable too). The pref `credit` is written with the post; a post stored by 0.5.0 has
  none, so nothing shows until the next post or a pull (`refresh` stores what it fetched; no
  migration).
- **Explanation (user, 2026-10-17):** its first two lines under
  the title, with "more" below while they cut it off; a tap on it or the title shows it all,
  another hides it. A short one (Earth's one sentence) has no "more" and doesn't expand. Screen
  readers: "Show explanation" / "Hide explanation" as the tap's label.
  Long press (user, 2026-10-07; the picture already opens the page): on the title block copies
  the page link, on the explanation its text. A toast confirms
  it only below Android 13, which shows its own confirmation. No `SelectionContainer`: its long
  press would select instead.
- **Wallpaper:** `WallpaperManager.setStream(stream, crop, true, which)`. `crop` is the part of
  the picture in the screen's portrait shape (`wallpaperFrame` in `:core`), by default the
  middle (2026-10-05): with `null`, Android 15+ shows the left part of a wide picture (seen on
  the phone, API 36). In landscape the system zooms the frame's middle band to the width; left
  as is. **Tested and rejected (user, 2026-10-18, WP8a):** a hint widened for landscape on
  Android 15+ (`wallpaperHint`, U7: up to the screen's landscape shape, so landscape showed the
  picture's height; right on the tablet's Olauncher). Portrait then depends on the launcher: an
  app gets one crop rectangle (per-orientation crops, `setStreamWithCrops`, are a system API),
  and inside a wider one the launcher's wallpaper offset decides what shows. The FP6's launcher
  (Niagara) reports `mWallpaperX=0.0`, so portrait showed the left edge, the user's own picture
  shifted; a launcher that scrolls the wallpaper would move it. Only the portrait frame looks
  the same everywhere. A picture with an EXIF orientation (a camera photo as "my picture") is
  decoded upright with `ImageDecoder`, halved while it still fills the screen, and set with
  `setBitmap` (user, 2026-10-05): Android 16 measures it unturned but draws it turned, so it
  came out stretched, with or without a crop. Only changed when the date is new and the
  download is a complete, decodable image.
  **The whole picture**, per post beside ⛶'s part (`<source>.whole`, keyed by the part's
  `cropDate`/`cropUrl`, so a new post drops it; `Part.whole`, saved and set by ⛶'s ✓ since U0; its
  toggle is ⛶ mode's "Whole picture"). Each set of a source's post with it: the picture decoded
  upright by `ImageDecoder` straight to `:core`'s `fitFrame` size (`setTargetSize`: one large
  bilinear shrink from the rendition's full size can make stars jagged; a smaller picture is
  scaled up; `info.size` and `setTargetSize` are the upright size on Android 10 and later,
  AOSP's `ImageDecoder.cpp`), drawn 1:1 into the frame on a bitmap of the screen's portrait size
  (about 11 MB on the FP6) and set with `setBitmap`, the hint all of it: on black when `:core`'s
  `edgesBlack` says so (an upright grey copy decoded straight to `edgeCopySize`, 200 px wide),
  else on the blurred background (the picture's middle in the screen's shape, decoded halved
  while it still covers `blurSize`, scaled to it, `blurBackground`, drawn enlarged to the screen
  with filtering). Otherwise Fill at the stored part, else the middle (`setStream` with the
  crop). The user's own picture (video days): always Fill, centred (no whole picture).
  **The darkening** (BR2, 2026-11-14; GitHub issue #2; the figures in Next features, 0.7.0:
  bright pictures darker): while "Darken bright pictures" is on (the stored `darken`), each set
  (the job's, the page's, Set's, ⛶'s ✓; the user's own picture too) first measures what the
  screen will show: the mean grey of its top third (`:core`'s `topThirdMean`, by `gray`'s
  weights) on a copy 200 px high (`Drawing.kt`'s `topThirdGrey`): for Fill the frame alone
  (`frameGrey`: `decodeFrame`, `ImageDecoder`'s crop, upright, decoded straight to that height),
  for the whole picture the bitmap as drawn, background included. `:core`'s `darkening` gives the
  factor (1 up to 100, else 100 ÷ the mean, at least ×0.7). **A factor of 1 keeps every path as
  before** (`setStream`, the EXIF photo's `setBitmap`, the whole picture), byte for byte. **Below
  1** the picture goes on through a bitmap of the screen's portrait size drawn with a `Paint`
  holding `ColorMatrixColorFilter(setScale(k, k, k, 1))` (`darkeningPaint`: R, G and B scaled
  alike, so the hues stay and black stays black), set with `setBitmap`, the hint all of it:
  Fill's frame (an EXIF photo too) decoded alone at the screen's size (`decodeFrame`: only the
  frame, since a wide panorama at the screen's height would be hundreds of MB whole) and drawn
  with `drawPanned`; the whole picture drawn as before and copied through the paint
  (`darkened`). A failed measure sets the picture as it is (logged). The log's set line ends
  ", darker ×0.78 (grey 141)" (the job's, the page's, Set's, and "Part: wallpaper set: …" for
  ⛶'s ✓). The page shows the picture as it is; ★ and "Save every picture" save the originals.
  Debuggable builds write the bitmap set to `files/debug-set.png` (deleted by a `setStream`
  set) and the whole picture before the darkening to `files/debug-whole.png` (L4), for checks
  by the pixels.
  **Home and lock set separately** (M0, 2026-10-20; tablet, Android 15; the FP6 unchecked): both
  flags make one shared wallpaper (`dumpsys wallpaper`'s "Lock wallpaper state" empty);
  `FLAG_SYSTEM` alone first moves the shared one to the lock screen and sets home anew;
  `FLAG_LOCK` alone replaces only the lock one; both flags again leave one shared wallpaper and
  no lock one. No screen went blank.
- **Choosing the part (user, 2026-10-05):** a ⛶ at the bottom left of the picture (mirroring
  the star, none on video days) shows the frame: as large as fits, the rest dimmed. It slides
  along the one direction it can move (left–right; up–down on a picture taller than the
  screen, none of 85 posts checked was). While it shows, a tap doesn't open the page; ✓ closes it
  ("Done" for screen readers) and **sets it at once** (U0, user, 2026-11-02, from the mockup's
  (a): from S4 ✓ only kept the part in the draft, and nothing on the page said that Set was
  still needed; users expected ✓ to set it). `Store.savePart` saves the part (and the whole
  picture) for that post at once, on the page's thread; then `Store.setPart`, under the
  wallpaper lock, sets the screens with a column whose record
  shows that very post again with it (one set with both flags when both do), their
  `wallpaperSetAt` moved; the records, the turns and the change times stay as they are (not Set's
  `restartAndForget`), the draft's other changes stay a draft. A post on no screen (NASA's page
  while APOD is on) or one a newer post replaced: only saved, no toast, the job uses the part
  when the post goes on. Toasts "Wallpaper set" or "Couldn't set the wallpaper" (the part stays
  saved, and ✓ with the same part tries again: `failedPart`); none when ✓ changed nothing. A
  page's draft parts are the stored ones, read again with the status (`readStatus`; second
  check of U0, 2026-11-02: a page under a stacked one kept its older part, showed ⛶ as "not set
  yet" and wrote it back with Set), and a second ✓ while the first's set runs compares with
  what the first saved (else it counted as no change and was lost); each set reads the part
  when it sets, so the last ✓ wins. ⛶ is disabled (dimmed) while Set or the daily switch's
  run goes: Set's draft, read before, would write the old part back. In the process-wide
  `runs`, as Set's (a rotation keeps its end). Back closes crop mode without keeping;
  reopening ⛶ shows the stored part. A rotation or a resize closes
  it too (the page is recreated), dropping a dragged part not yet kept with ✓, as Back does,
  and closes an open dropdown; the draft, Setup's and Advanced's state and the shown source
  survive (user, 2026-10-22: left as is). A Set while crop mode is open keeps the dragged part
  in its draft and stores it with the setup. The position (0–1) is per source for that post
  only (date and picture URL), so each new picture starts centred (a new post drops a draft
  part); the job uses only the stored one. The user's own picture (video days) is always
  centred.
  **Whole picture** (texts approved by the user, 2026-10-24): in ⛶ mode a toggle at the
  picture's top left (where the chip is otherwise; the chip's dark pill), "Whole picture", "✓ Whole
  picture" while on. While on, the frame hides (no dimming, no drag) and the page shows the
  picture as it is (no preview of the background: that comes with Live, 0.7.0). ✓ saves and
  sets it with the part (`Part.whole`, U0), Back drops it, reopening ⛶ shows the stored one; a
  Set from open crop mode stores it too, per source for that post only, as the part. The toggle
  shows in `tertiary` while it differs from the stored one (until ✓); screen readers: "Whole
  picture", a switch, "On"/"Off" with ", not set yet" while it differs.
- **No notification** (user): it would need the `POST_NOTIFICATIONS` runtime permission, and the
  page shows the title. The one exception is Keep running's (O11, Background update): a
  foreground service must have one; the permission is still never asked, so on Android 13+ it
  doesn't show in the shade, only APODroid in Quick Settings' Active apps.
- **Icon:** the user's SVGs in `logo/` as an adaptive icon; a monochrome layer for themed icons
  (2026-10-04) computed from the foreground (skill, Icon).

### Save

(User, 2026-10-01.) The star at the bottom right of the picture saves it to the gallery,
`Pictures/APODroid/`, named `APOD_<date>_<Title_Words>.<jpg|png>` (`Post.fileName` in `:core`; the
date makes it unique, only letters, digits, `-`, `_`, at most 100 chars; the extension follows
the image type). It downloads the original (`originalUrl`, to `cache/original.part`, one name
since one save runs at a time; each save and the page's first start delete a leftover, as a
save killed with the process leaves one) and copies it; if that fails, the stored picture (the
rendition), and a toast says it's a smaller copy. While it downloads, ☆ is dimmed ("Saving" for
screen readers) and taps do nothing; one save at a time in the app, so a tap after a rotation
says "Still saving", and the rotated page fills ★ when it ends. After a save each page's ★ shows its own source's
state. ☆ = not saved, ★ = saved; a second tap (or a quick double tap) doesn't save again. Text glyphs, no icon library. Written
with `MediaStore` (`IS_PENDING`), which needs no permission on Android 10+, hence `minSdk 29`
(user chose that over `WRITE_EXTERNAL_STORAGE` for Android 8–9, or a "Save as" dialog each
time). Saving after "Clear storage" (or a reinstall) adds a second copy `… (1).jpg`: the app
can't see the old, now unowned file without a permission; left (2026-10-04). The saved date,
picture URL (so a second post on the same date counts as unsaved) and `content://` URI are in the prefs; the star checks the URI still exists, so
deleting the file in the gallery empties it again. No star on video days. The
original downloads outside the refresh lock (a refresh doesn't wait for a 10 MB save); the copy
to the gallery holds it, and if a newer APOD was stored meanwhile it saves nothing, says so and
redraws the page.

**Save every picture** (0.6.0; the pref `saveEvery`, default off): a switch
in "Advanced settings" while the daily switch is on, with a small ⓘ ("About saving") that opens a
dialog: "Saves each new picture to Pictures/APODroid: full size up to the limit, else screen
size. A few hundred MB a month or more." (OK; user, 2026-10-20 night; shorter since U0,
2026-11-02). The label and ⓘ share the width the switch leaves (U0: a `Spacer` of the same
weight gave the label half of it, so it wrapped in the tablet's pane). After each job run (not the page's), also
after a failed one, not after a stop, `Store.saveInUse` saves each source in use's stored post
in order APOD → NASA → Earth, as ★ does (`save`: the original, else the stored picture). The set
in use is `:core`'s `sourcesInUse`, the one `update` fetches (the ticked sources and
the stand-in on an APOD video day); the choice is `toSave` (`SavingTest`): no video, no
post without its picture, none whose `savedDate`/`savedUrl` match. No URI check, so a picture
deleted in the gallery isn't saved again (★ still checks it). So APOD mode saves APOD, on a
video day the stand-in; "Skip it" or "my picture" saves nothing. One skipped while ★ saves
(`BUSY`) or failed is logged, doesn't retry the job, and the next run tries. A stop ends it:
`save` rethrows an interrupted download (`isStop()`) instead of falling back. **One try of the original
per post** (user, 2026-10-20, from a review by reading): a stopped save noted nothing, so
each retry of the job and each later run downloaded the original from zero (a 97 MB NASA
original on a slow connection, all day). The job notes the try first (`<source>.saveTried`,
the picture URL, committed before the download); `toSave` then says not to try it again, and
the next job save of that post saves the stored picture, the smaller copy, as after a failure
(`save(post, tryOriginal = false)`). A post with another picture URL tries its original. A save
that returns (also `BUSY`, which downloaded nothing) clears the note. ★ always tries the
original and ignores the note.
**A size limit** (user, 2026-10-20 evening, after a 208 MB NASA PNG; the label, the default and
1 MB = 1,000,000 bytes approved by the user): "Largest original" [10 MB | 20 MB | 50 MB | 100 MB | No
limit], a dropdown under "Save every picture" in "Advanced settings" while that switch is on
(indented, label then "20 MB ▾", ✓ before the chosen one; screen readers "Largest original:
20 MB"), default 20 MB, at once (not part of the draft; the pref `saveLimit`, MB, 0 = no limit;
1 MB = 1,000,000 bytes). The job only (`saveInUse` → `save(post, original, limit)`): `:core`'s
`download(…, limit)` throws `TooLarge` before reading the body when `Content-Length` is over
it, else once the bytes pass it (the `.part` is deleted); `save` logs it ("Original over the
limit") and saves the stored picture, as after a failure: no retry, `saveTried` noted as
before. NASA's enclosure `length` isn't parsed, so it isn't used. ★ ignores the limit.

### Background update

(Decided 2026-10-01; by the clock since J2, 2026-10-23: Next features, 0.6.0: the picture
time.) **Alarms at the change times start the run; a periodic job is the fallback** (J0: only a
wake-up alarm lands in Doze; part of Android, no library, no new permission; not WorkManager,
an AndroidX dependency). `DailyJob.kt` and `AlarmReceiver.kt`:
- **The alarms:** at each next run T (`Store.nextAlarm`: `:core`'s `nextAlarm`; Turns, the next
  run) a pair, `AlarmManager.setWindow(RTC_WAKEUP, T, 10 min)` and
  `setAndAllowWhileIdle(RTC_WAKEUP, T)`, two `PendingIntent`s to `AlarmReceiver` with request
  codes 1 and 2 (one code would make the second replace the first). Arming again replaces the
  pair. The first that fires (`DailyJob.runNow`) schedules a one-off job, **id 2** (the same
  service, no delay, any network, not persisted) unless a run goes in the process or this
  process scheduled it and it hasn't started (`runScheduled` and `getPendingJob`); the second
  finds it so and does nothing. A pending id 2 from before the process died (Android may
  reschedule a run that died with it, with its own backoff, and while pending it blocked the
  alarms; B16, 2026-10-24, from a review of J2) is replaced. **A failed schedule arms the retry**
  (O4a, 2026-11-14, from the review of 0.7.0's runs): when `schedule()` returns
  `RESULT_FAILURE` or throws (caught, logged), the pair is armed `runRetry` ahead (15 min, an
  hour after two or more failed runs in a row, O4b), as a run's start and an alarm landing in a
  going run do (`armRetry`),
  so while the switch is on an alarm is armed or id 2 is pending (SIM's invariant 9). Expect about T + 10 min when Unrestricted;
  Optimized: the alarm on time; on Android 12+ id 2 is an **expedited job** (O10, 2026-11-14:
  `setExpedited`, no permission, no notification), which runs in Doze and battery saver with
  the network, so an Optimized phone gets the picture on time while the app isn't in
  RESTRICTED (about 8 days after the last open); `RESULT_FAILURE` (no expedited quota left)
  schedules it again as a plain job ("Daily job: alarm, run scheduled, not expedited"), and the
  run's start logs "expedited" or "not expedited". A stop at the expedited job's limit is a stop
  (B7, retried by the alarms). Android 10–11: a plain job, the network only at Doze's next
  maintenance window (accepted).
- **A passed check kept** (B16): each run's start (either id) notes `lastRunAt` (also the
  daily switch on, whose own update sets; off removes it). A check time after it that has
  passed is armed at once, not dropped (`:core`'s `nextRun`/`nextAlarm`, `lastRun`): arming
  replaces the pair, so a page open, a Set or a `TIME_SET` (network time) between 06:07 (the
  armed check) and its alarm's delivery (up to 06:17) armed for 06:22 and the 06:07 check never
  ran; so did a run's end when the run did its update before the check. The checks are taken
  from the change time of now less the offset, so the one before's stay in view until the
  current one's offset has passed. Without a `lastRunAt` (a fresh install) nothing is owed.
- **Armed again** (`DailyJob.ensure`, while `daily` is on): after each run's end (either id),
  Set, the daily switch on, each `onResume`; at boot, after a time zone or clock change and after
  an update of the app (`AlarmReceiver` in the manifest, not exported: `BOOT_COMPLETED`,
  `TIMEZONE_CHANGED`, `TIME_SET`, `MY_PACKAGE_REPLACED`; alarms are absolute times and survive
  neither a boot nor an update; on a phone with a PIN boot's only after the first unlock).
  Seen on the tablet (rosie, J2, Android 15): `BOOT_COMPLETED` after `am kill`, likely Android
  15+ delivering it when an app leaves the stopped state (a force stop, `pm clear`, an install).
  Harmless: it only arms again (no code; B16).
- **The fallback**, **id 1**: periodic, every 6 h with a 6 h flex (0.4.0's), any network,
  `setPersisted(true)`. Its timing never changes: `ensure` compares interval and flex, so a
  0.5.0 or older 0.6.0 install's turns job (3 h, 15 min flex) is replaced once at the first
  start (scheduling runs it at once), else nothing schedules it again. Hibernation or a force
  stop deletes it (and the alarms), so each `onResume` schedules it again when `daily` is on
  (the pref, user, 2026-10-04).
- **No reschedule stops a run:** scheduling a job's id again stops its run, so neither id is
  scheduled while a run goes (`JobRuns.going`, process-wide: the page and the job share one
  process); a run's end calls `ensure`, which does what was skipped. So a Set or the daily
  switch on during a run arms the alarms and leaves the run alone.
- **One run at a time** (B16): a job of either id that starts while a run goes ends at once
  (`onStartJob` returns false, `JobRuns.start` null): both ids share the one `DailyJob`'s
  `worker` and `stoppedBySystem`, so the fallback starting beside the alarm's run made one
  `onStopJob` interrupt the other's thread, and both runs counted as failed. The going run's end
  arms again, and a check it missed comes at once (A passed check kept). `stoppedBySystem` is
  reset at each start; `onStopJob` interrupts only for the running id.
- **Retries by the alarms** (user, 2026-10-23 night): every run ends with
  `jobFinished(params, false)` and `onStopJob` returns false, for both ids, so Android keeps no
  backoff retry (in Doze it waits for a maintenance window and, pending, blocks the alarms). A
  failed run (a fetch's or a set's failure in `Updated`), one `onStopJob` stopped
  (`stoppedBySystem`) and one killed before its end count. **A run counts as failed from its
  start until it ends well** (O4b, the user's yes, 2026-11-14; was two notes, `runGoing` and
  `failedAt`, and a third kind of outcome for a killed run): its start (`Store.runStarts`) adds
  one to `failedRuns` (in a row) and notes `lastRunAt`, in one commit; a good end
  (`Store.ranWell`) removes `failedRuns`; a failed, stopped or killed run writes nothing more,
  it's counted already. The daily switch either way removes `failedRuns` too. The alarms then
  come 15 min after the failed run's **start**, then hourly (`:core`'s `retryAfter`,
  `Store.runRetry`; `nextAlarm(now, zone, failures, time, turns, offset, lastRun)`:
  the clock's next run, or with failures the earlier of it and `lastRun` + `retryAfter`, at the
  earliest now). Why from the start (user, 2026-11-14): one rule for a run that failed, was
  stopped, was killed or still goes; for a normal failure the two are seconds apart, a run
  Android stops at its 10 min limit is retried 5 min after the stop instead of 15 (an expedited
  job's 3 min stop, O10: 12 min after it). `nextAlarm` keeps that retry when armed again before
  it, by a Set or a page open; a retry whose time passed without a run, after a boot or the
  process dying, comes at once. A run that waits for the network (id 2 pending) arms nothing
  until it runs. **A run that never ends** (a frozen or killed process: `install -r`,
  `kill -9`, the low-memory killer, a Recents swipe; B16, B19): its start arms the alarms
  `runRetry` ahead (15 min, an hour after one or more failed runs in a row). It stays counted,
  so an arming during the run or after its process died (a page open, a Set, the daily switch
  on, `TIME_SET`, `TIMEZONE_CHANGED`, an update's `MY_PACKAGE_REPLACED`, a boot) keeps that
  retry: the earlier of the clock's next check and `lastRunAt` + `runRetry`, at the earliest
  now, in every process (no in-process flag: `nextAlarm()` takes no argument, so Live's engine
  can call it, finding 5 of `review-0.7.0-runs.md`). Android re-runs a killed job 1 itself, a
  killed id 2 not. The next run's start counts itself on top, so a crash on every run (out of
  memory decoding a huge picture, downloaded again each time) is retried 15 min later, then
  hourly. A killed run is no longer told apart in the log. An alarm that lands in a going run arms the
  pair `runRetry` ahead again, as the run's start did (B18: else a process killed before the
  run's end had no alarm left). A run the daily switch off dropped logs "Daily job: stopped
  (the switch off)", not a failure; a stop by Android stays a failure. A stop leaves the thread
  interrupted (`Store.update`), and an interrupted thread's `commit()` skips the prefs
  listeners, so the run clears the flag (`Thread.interrupted()`) before `ranWell` (B16). Such a
  `commit()` also clears the flag, so a stop doesn't rest on it alone (B18): `update` and
  `saveInUse` also read the job's `stoppedBySystem` before each source, each set and each save,
  a commit on the job's thread clears the flag first and then ends the run (the record of a set
  done is written, the page hears it), and a run `onStopJob` stopped saves nothing. **The
  order at a run's end** (B24, 2026-10-26): the flag cleared, `ranWell` after a good run (under
  the lock, unless an off came since or the switch is off), then `jobFinished`, then `ensure`:
  with `jobFinished` first, a death between them left a good run counted as failed, and the
  next start's pair came an hour ahead. A death after `ranWell` and before `jobFinished` counts
  the run as ended; Android may re-run job 1 (it finds nothing new).
  `ensure` stays after `jobFinished`: scheduling an id Android still counts as running stops it.
- **Off is off** (`DailyJob.cancel`): both jobs and both alarms cancelled, a running run too;
  `JobRuns.drop` bumps a generation each run takes at its start, so a run from before an off
  ends without effect (it arms and notes nothing; a lock keeps its end and the cancel apart;
  `:core`'s `JobRuns`, `JobRunsTest`).

**Keep running** (O11, 2026-11-14; the user's decision of 2026-10-28, on-time-0.7.0.md,
Decisions 1 and 3; the texts placeholders until the user approves them from the tablet's
screenshots): a switch in Advanced settings, after "Save every picture"'s group, while the daily
switch is on, the pref `keepRunning`, default off, acting at once (not part of the draft, never
highlighted), with a small ⓘ ("About keeping running"; its dialog "Keeps APODroid running, so
the phone can't stop it between changes. It shows as an active app."). Why: a maker's cleaner
force-stops cached apps (ZUI's from oom adj 701, O8: APODroid at 910 lost both alarms), and a
foreground service's process isn't cached (O8: the probe at 50–200 was spared in the same
sweep). `KeepRunning.kt`, a `Service` with `foregroundServiceType="specialUse"` (the property
`PROPERTY_SPECIAL_USE_FGS_SUBTYPE`, one sentence; only Google Play reads it), not exported: its
`onStartCommand` goes foreground (`ServiceCompat.startForeground`, the type on 34+) and is
`START_STICKY` (O7: back about 1 s after a `kill -9`, foreground again on 15); nothing else, no
thread, no wake lock, no network: the runs stay the alarms' and the fallback's in this process.
A start that crossed an off (the switch read on just before) goes foreground, then stops
itself; a stop waits while a start is pending (`:core`'s `PendingStarts`, O11's fix round 1: a
`stopService` before the start's `onStartCommand` crashes the app, "did not then call
startForeground"), the start's read of the switch and `startForegroundService` under one lock
with the stop's check and `stopService` (fix round 2: else a start read on, a stop found
nothing pending, and the two crossed); a start stays pending until its `onStartCommand`'s
`startForeground` has gone foreground or been refused (fix round 3: counted off at the top, a
stop during the notification's building still crashed the app). The notification: channel `keep`, "Keep running", `IMPORTANCE_LOW`, no badge; the text
"Keeps the wallpaper changing on time" (the header names the app), ongoing, no time, the small
icon `ic_keep_running` (the monochrome layer's paths, the inner 72 of 108 units, 24 dp), a tap
opens the page. **Started** by `KeepRunning.ensure` (while `daily` and `keepRunning`, else it
stops the service): the switch on, each `onResume` (the page in the foreground may start it:
back after Task Manager's Stop, a force stop or a refused start), the daily switch's run's end,
`BOOT_COMPLETED` and `MY_PACKAGE_REPLACED` (both may start a foreground service from the
background), and each job run's start (after `started`, outside the lock, its own catch:
`review-0.7.0-runs.md`'s finding 3; Android allows it there only while the app is Unrestricted,
O7: refused while Optimized). **Stopped:** the switch off, the daily switch off (beside
`DailyJob.cancel`). **Refused** (`ForegroundServiceStartNotAllowedException`, 12+; also a
sticky restart's `startForeground`): caught, logged ("Keep running: couldn't start: …" or
"Keep running: refused: …"), the service stops; nothing on the page. With it on the process
stays resident (O7: about 105 MB PSS after Back, the decoded pictures still in the native heap; O12: the page lets them go when it stops).

**The detector** (O14, 2026-11-14; on-time-0.7.0.md, The detector; the texts placeholders until
the user approves them from the tablet's screenshots): the page notices that the phone stopped
the app, so the change came only when something started it again. `:core`'s `Detector.kt`
(`DetectorTest`). **The sign:** a force stop deletes the alarms (`am force-stop`, O7; ZUI's
cleaner, O8, whose force stop leaves no other sign: `wasForceStopped()` false, the package not
stopped), a process's death doesn't. So the first arming in a process (`DailyJob.ensure`'s
`checkAlarms`, from the page's `onResume` or a receiver; not again in a process that armed or
cancelled them) asks for the pair's `PendingIntent`s with `FLAG_NO_CREATE`; both missing while
`daily` is on is noted (`alarmsLost`: `alarmsLostAt` and `lostAfterRun`, the last run's start)
unless a boot or an update since the alarms were armed explains it, or job 2 is pending (the pair
fired and was used up; the run waits for the network or its quota), or no run's start is noted.
Each arming notes its boot (`armedBoot`, `Settings.Global.BOOT_COUNT`, which a clock change
doesn't move; the boot's moment, `armedBootAt`, where it's missing) and its install
(`armedUpdate`, `lastUpdateTime`); none noted yet (the first start of 0.7.0) explains it too
(night default: by the arming, not by `lastRunAt` as the plan said: each run's start arms, so
it's as recent, and a clock step doesn't fake a boot). **A miss** (`missedChange`): the next
ordinary run's sets (the page's load, or the run its arming started at once for the passed
check, whichever reaches `setDue` first; not a pull's nor Set's) due by a change time T after
that last run's start, the alarms found gone at T + the offset + 30 min (`missAfter`) or later: the
latest such T is noted (`missedAt`) with "Keep running"'s state (`missedKeep`); the loss note is
used up either way (nothing to set: nothing lost). The daily switch, both ways, drops the note.
So a bucket holding an alarm (no sign), a boot, an update, a failed run (it started), a page
open before T + offset + 30 min and a pending job 2 show nothing. **The box** (neutral, with
the others, while the daily switch is on, from `missedAt` until "Dismiss", `missDismissed`, the
next miss shows again): with Keep running off at the miss "Your phone stopped APODroid: the
18:00 change came late." (the time as the system's clock; when the missed change's local date isn't today "the 18:00 change on 16 Nov came late", O14b, `:core`'s `missedWhen`, placeholder), buttons "Keep running" (opens
Advanced settings and scrolls to the switch; doesn't turn it on) and "Dismiss"; such a box hides
while Keep running is on. With it on "Your phone still stops APODroid: the 18:00 change came
late." and the maker's lines (`maker`): Xiaomi `App info › Autostart: on`, `App info › Battery
saver › No restrictions`; Samsung `Battery › Background usage limits › Never sleeping apps ›
APODroid`; Oppo `App info › Battery usage › "Allow background activity": on`, `App info ›
Battery usage › "Allow auto launch": on`; Vivo `Battery › High background power consumption ›
APODroid: on`, `App info › Autostart: on`; Huawei/Honor `App info › Battery usage details › App
launch settings › "Manage automatically": off`, `Auto-launch, Secondary launch, Run in
background: on`; Lenovo (O8: ZUI has no such setting) and stock `A force stop ends the changes
until APODroid opens.`; buttons
"App info" and "Dismiss". It doesn't suggest Live (live-0.7.0.md, Decision 9).

Each run (either id), and each page open or pull (2026-10-16, one path): `Store.update(force)`,
`refresh(source)` for each source in use unless its stored post is today's (always with
`force`), then, while `daily` is on, `setDue()`: what each screen is due by the change times
(`:core`'s `dueWallpapers`, Turns), set where it isn't what the screen's record notes; then the
job, with "Save every picture", `saveInUse` (not after a stop). **Always any network** (user,
2026-10-21: whoever wants the picture wants it at once). A page open sets only what's due by the
times (a new post outside the window waits for the next change time; no setting, user,
2026-10-23); Set and a pull set at once. **A record per screen** (0.6.0): home's the
unprefixed keys (as before), the lock screen's under `lock.`: `wallpaperSource` (whose turn,
the owner), `wallpaperPicture` (the picture's source or `own`) with `wallpaperDate` and
`wallpaperUrl` (the URL: a site may post a second picture on the same date, never seen in the
60 feed items; none matches by date), `wallpaperStamp` (the change time it belongs to), the
notes of what it showed (`shownDate`/`shownUrl` per source) and `wallpaperSetAt`. Both screens
due the same picture: one set with both flags (one wallpaper); else `FLAG_SYSTEM` and
`FLAG_LOCK` each (M0). Each screen's record is written right after its own set; a failed set
leaves it, so the retry sets that screen only. Set ("Set as wallpaper") writes the
settings (with `pictureAt`) and the records in one commit (`Store.saveSetup`), then `update(set =
true)` sets, also with `daily` off; `update` returns `Updated` (what it set, whether a set
failed, the first failure) and throws only a stop. The page reads home's record, the lock screen's
when only it has a column (APOD mode with "Where" = Lock). The sources in use (the job): the
ones ticked on either screen, and on an APOD video day the stand-in, while APOD is ticked
somewhere; the page with
the sources it offers (the draft's mode's: all three with `allSources` on). One failing doesn't stop the others, and its stored picture still counts (user, 2026-10-17,
after a code review: a failed check ended the current turn early, and a weekend shows Friday's
stored picture anyway); the set is logged "Despite the failure: …" and the alarms retry. A
separate
date is needed because a refresh with the switch off downloads without setting. On a video
day with "keep" nothing is saved, so switching to "my picture" later that day takes effect with
Set. `refresh()` and `save()` hold one lock, as the page and the job can
run at once; `setDue()`, every set and every change to the records or columns hold another (not
the refresh lock: ★ would wait seconds for a set), so a page and a job running at once set once;
the page calls them off the main thread (`Store.nextAlarm` takes neither: `onResume` and the
receiver call it on the main thread).
Set with the daily switch on arms the alarms and ensures the fallback after its set (a fresh
fallback runs at once and would set first); off cancels them. Turning the daily switch on
does the same after setting the stored setup afresh (`Store.startAfresh`); off cancels
at once. Quick taps (on, off, on while the first
switch-on still runs) set once, not twice (user, 2026-10-20 night: they set the same picture
twice, 0.6 s apart): an on while a switch-on run goes owes one run after it,
which that run pays when it got past its set's check of the switch (`Updated.wentToSet`), so
only a run that gave up (the switch off at its check) leaves the last on to run once more and
the taps never end on with nothing set (`:core`'s `SwitchRuns`, `SwitchRunsTest`). An off and on
that come after the run's set (during its redraw: taps about 0.35–1 s apart on the tablet) owe a
run that sets the same picture once more and arms the job (accepted, user, 2026-10-21: the end
state is right).

**Turns and the picture time** (per screen since 0.6.0; by the clock since 2026-10-23: the
rules of Next features, 0.6.0: the picture time; turns as a plain rotation since RO1,
2026-11-14: Next features, 0.7.0: turns as a plain rotation; in `:core`'s `PictureTime.kt` and
`Screens.kt`, `PictureTimeTest`, `ScreensTest`, SIM's `RulesSimulationTest`). **The change times:**
"New picture at" (`pictureAt`, an hour, 06:00 by default) and, for a column with two or more
(turns), every "Change every" from it (`pictureAt` + k × `interval`); a column with one source
changes at `pictureAt` only. Local time, on the clock's hours: a DST day has 23 or 25 hours
between two 06:00s; an hour that doesn't exist comes at the gap's end (02:00 on 28 Mar 2027 in
Zurich at 03:00), one that happens twice counts once, the first (`PictureTime.last`, `next`,
`isChangeTime`). **Each change time has a number** (RO1, `PictureTime.turn`): the day's change
times counted from "New picture at" (0, 1, …; one before it, 00:00 with 06:00 and 6 h, belongs
to the day before), plus the days since 1970-01-01 × the change times a day; consecutive change
times have consecutive numbers, also over DST days. Each screen's record notes the change time
it belongs to (`wallpaperStamp`), whose turn it is (`wallpaperSource`) and, per source, the last
post it showed (date and picture URL, `shownDate`/`shownUrl`, `lock.` for the lock screen; since
RO1 the rules don't read the notes, and since RO2 nothing does: still written, no migration). **The rules**
(`dueWallpapers`, every run, page or job):
1. **A change time since the record's stamp:** with one source the screen takes the change
   time and is due its newest picture by rule 2's publish time (no set when it's on already);
   with several (turns, a plain rotation since RO1, user, 2026-11-11 and 14) the turn's source
   by the clock, the change time's number mod the column's sources in order (APOD → NASA →
   Earth; since OR2 the list's order from the anchor, below), becomes the owner and its newest
   picture goes on, also one this screen has shown;
   a source with nothing to show (nothing stored, APOD's video with "Skip it" or "Use my
   picture" without one) passes the turn to the next with a picture in the list's order
   (`nextSource`), and with
   none the screen keeps its picture; it takes the change time either way. So with APOD and
   NASA every 6 h from 06:00 APOD is on at 06:00 and 18:00, NASA at 12:00 and 00:00; every
   24 h they take days in turn. A phone off, a Set or a pull never shifts the rotation; missed
   change times (the phone off) make one change, by the current one's number. Repeats are
   accepted (today's APOD twice a day with 6 h; Friday's NASA and Earth at their weekend
   turns). **Replaced** (0.6.0's rule 1 and rule 3, 2026-10-23): the next source after the owner
   with a picture the screen hasn't shown (`Stored.isNew`), and with nothing new the screen
   waited, checked hourly (`waiting`), the first new picture going on as soon as a check saw it.
   Why (user, 2026-11-11): on the FP6 (11 Nov, APOD and NASA, 6 h) APOD didn't come back at
   18:00 (its picture shown that morning, NASA with nothing new on US Veterans Day), the screen
   waited, and only the phone's history told why: the rule hung on a hidden state, so the
   settings didn't predict the screen. By the clock (the user, 2026-11-14, Open 1 (b)) rather
   than after the one on: one clock, as 2026-10-23 wanted. The FP6's day is
   `ScreensTest.theFp6sDayOf11NovPutsApodOnAt1800` (it fails on 0.6.0's rules at 18:00).
2. **The window, by the publish time** (B15, 2026-10-24): a newer picture of the owner
   published before the change time + 3 hours (`pictureWindow`; `Stored.publishedBefore`:
   `Post.publishedAt`, and a stand-in's own post too) replaces it whenever a run sees it, also
   after 09:00 (an offline morning, a 503, Doze, a set that failed and is retried later); one
   published later waits for the next change time (with turns the owner's next turn). The change
   time kept; a post published later waits for its source's next turn. With one source this is the whole rule: from each change time T (the last one ≤
   now, taken by every run) the screen shows the newest stored picture published before T + 3 h,
   whenever a run sees it, a function of the clock and the posts (a failed or offline check
   moves nothing). **After a day offline** (B21, 2026-10-25, CR2's finding 3): with one source
   a new post stored when the source's last successful check (`checkedAt`, read before this
   check notes it) is more than 24 h old (`:core`'s `afterGap`) is marked (`<source>.afterGap`,
   written with the post by `Store.refresh`; the same post again keeps it, the next post
   replaces it; a post stored before B21 has none, no migration) and goes on whenever a run
   sees it, as a pull would, the change time kept (`Stored.gaps`, `publishedBefore`'s
   `orAfterGap`): else a phone offline from 20:00 on D−1 to 22:00 on D in Los Angeles stored
   D+1's 21:05 post, set nothing, and kept D−2's picture until 06:00 on D+1 (only the newest
   post is stored, so D's was gone). No `checkedAt` yet is no gap. Weekends aren't gaps: the
   change time's checks succeed with nothing new and note `checkedAt` (also when `isCurrent`
   skips the fetch); not by post dates, which NASA's and Earth's weekends would trigger every
   Monday. On APOD's video day APOD's post and the stand-in's each have to be in the window or
   marked (as the publish time counts for both), so the stand-in's first fetch of a video day,
   usually marked, doesn't put it on in Los Angeles' normal evening. Turns ignore the mark. A
   phone in the RESTRICTED bucket (one alarm a day) can have such gaps daily: its newest goes on
   whenever that run comes (accepted). A pull, Set, a forgotten picture and a new screen put the
   newest on at once, whenever it was published; the next change time's target then decides.
   **With "As soon as it's out" on** (SO1, SO2; `dueWallpapers`' `soon`, the stored `soon`): no
   window, the owner's newer stored picture goes on whenever a run sees it, whenever it was
   published (with turns only the turn's source's: another source's waits for its turn; the
   stand-in's too, so with "NASA instead" NASA's picture goes on in APOD's turn as soon as it's
   out); B21's mark changes nothing while it's on. **Never back:** a
   picture of the same source older (by date) than the one on the screen isn't set.
A stamp that isn't a change time (M2's migrated 0.5.0 clock, 0.6.0's own Set clock, a time zone
changed since) or lies ahead (the clock set back) counts as the current change time: it takes
it; with several sources nothing is set until the next one (but by a pull), with one rule 2's
publish time decides (B15). **The stand-in** fills APOD's turn
on an APOD video day on every screen where APOD is ticked, with turns too (NASA's, Earth's or
the user's own picture; with "Skip it" APOD has nothing to show, so a screen with APOD alone
keeps its picture and one with turns passes the turn to the next with a picture); the record
keeps APOD as the owner. It may repeat (RO1, the user, 2026-11-14, Open 2): NASA's picture in
APOD's turn and again in NASA's, and with APOD alone a stand-in this screen has shown goes on
again (B15's "the turn passes on" and B22's "not twice", 2026-10-26, went). When an owner is no
longer ticked, or a forgotten picture's owner has nothing to show, it gives way to the turn's
source by the clock, else the next with a picture; with none the screen keeps its picture (an
unticked source's picture never goes on). **Set** (`restartAndForget`, then `setDue`): the records of
the screens with a column are forgotten (the picture
dropped, so it goes on again with the new setup, a changed whole picture or "my picture" too); each
screen goes to the current change time's turn by the clock in its new column (RO1; the next with
a picture when that source has none, by the new setup's "If APOD is a video": RO2's fix round), so a Set never shifts the rotation, and takes the current
change time as its stamp, so the next run doesn't move it on and the next change comes at the
next change time. The notes
stay; the change times don't move. Turning the daily switch on (`Store.startAfresh`) does
the same with the stored columns, saving nothing else. **The list's order and its anchor**
(OR1 `:core`, OR2 `Store` and the page, 2026-11-14; `order-0.7.0.md`): a column's ticked
sources in the list's order (`Store.order`), S₀ … Sₙ₋₁, the turn at change time c is
S[(N(c) − N(A)) mod n] (`Rotation`, `Rules.turnOf`): A the anchor, the "New picture at" of the
date `orderFrom`, N(A) that date's epoch day × the change times a day, from the current setup
(never stored; without `orderFrom` 0: RO1's rotation exactly, the update from 0.6.0 needs no
migration). **A Set moves the anchor** only with the list shown and an order other than the
stored one, or none stored: `orderFrom` becomes the local date of the next "New picture at"
after the Set (`PictureTime.anchorDate`, the saved setup's hour), so the top comes at the next
"New picture at" and the change times before it go on, phased to it. **Before A** a Set and the
daily switch on keep what's on (`keepUntilAnchor`): a Set of the order alone (`saveSetup`'s
`orderOnly`, `:core`'s `orderAlone`: the whole `Setup` equal but the order, the parts included, the list's columns
compared as sets, no newly picked "my picture") keeps each record with the current change time
as its stamp and sets nothing ("Setup saved. Nothing to set yet."); anything else (other
changes too, a Set that changed nothing, the switch on) keeps each screen's owner and sets its
picture again ("APOD stays", user, 2026-11-14); keep can't hold (no record, the owner unticked
or with nothing to show) or the daily switch off: the top ticked source, the next in the list's
order with a picture, at once. **From A on** they are RO1's Set above (the turn by the formula,
a Set never shifts it); `orderFrom` stays. Rule 2, "As soon as it's out", the stand-in, equal
columns and the change times as before. A screen left alone (an empty column)
keeps its record. A screen whose column was empty starts without a record (its owner is
removed): it was left alone meanwhile, so its record is stale. A screen with no record (the
lock screen after migrating "Home", a fresh install, such a column) starts on the turn's source
by the clock (RO1; was the other screen's owner), else the next with a picture, with the current
change time, and is set at once. **Equal columns** (the same sources on both screens; user,
2026-10-20 night): the lock screen takes home's record (owner, stamp, notes) and picture in
every run, so both are due one picture and get one set. So they match again after a Set from columns that differed and after a failed
set on one screen (its retry takes home's picture). Columns that differ: each screen its own
turn (the same number mod its own sources), the same change times. A changed interval or picture time takes effect with Set.
**The next run** (`nextRun(now, zone, time, turns, offset, lastRun)`, for the
alarms, with a failed run's retry added by `nextAlarm`): the earliest of the next change time; within the window 15 min after the change time,
then hourly (06:00, 06:15, 07:15, 08:15: the :05 posts of every whole-hour time zone), and the
window's end (09:00, B15: a half-hour zone's post published within it, Tehran's 08:35; three
real hours, so 02:00 CET on the EU's autumn DST night for a change at 00:00 CEST); no hourly
checks after the window (0.6.0's for a waiting screen went with RO1; the loop stays behind
`nextRun`'s `allDay`, default off: with "As soon as it's out" on, `Store.nextAlarm` passes it
and the checks go hourly until the next change time, SO1, SO2); after a failed or stopped run 15 min after
its start, then hourly (`nextAlarm`'s `failures`, the failed runs in a row, O4b). The clock's checks come a per-device offset
later (`randomOffset`, 0–10 min in whole seconds, drawn once; kept in range); the retries
count from the run's start. No model of each source's posting time. **In `Store`:** `pictureAt` (pref
`pictureAt`, default 6, also after an update from 0.5.0; part of `Setup` and `saveSetup`, the
page's draft, "New picture at" in Setup), `soon` (pref `soon`, default off, in `Setup` and
`saveSetup` too; `setDue` and `nextAlarm` pass it, SO2), the zone the system's, the offset (pref
`offset`, seconds, drawn at the first `nextAlarm`); `nextAlarm` also keeps a failed run's retry
and arms at once for a check passed since `lastRunAt` (Background update, B16). 0.6.0's `turnStartedAt` is no longer read (left in the prefs, no
migration). A video-day choice or the mode switch in the
draft while an update runs makes the page run one more after it (that one read the offered
sources before).

Seen on the tablet (Android 15, 2026-10-17): `Num Required Flexible constraints: 0` for the
job, so Android doesn't hold it back for charging or idle; the same on the FP6 (Android 16, WP8).

**The pause hint** (user, 2026-10-04; report:
[keep-running.md](.claude/skills/build-apodroid-android/keep-running.md)). Android pauses an app
that isn't opened, and the job doesn't count as use: the restricted bucket after about 8 days
(Android 13+; later or never before) and hibernation after about 3 months (12+), which deletes
the job. While the stored daily switch is on, a hint in a neutral box below the picture (user, 2026-10-16) names only what's missing, one line each:
battery Unrestricted (checked on API 33+ only, 2026-10-04 evening: on 10–12 the 8 days aren't
true) and "pause/manage app if unused" off (API 31+; on Android 12, API 31–32, its own App
info's `Unused apps → "Remove permissions and free up space": off`, B25). Under the line
"Android pauses apps you don't open." each line is a path starting where it starts ("App info ›
…", user, 2026-10-04: "Unrestricted" alone didn't say where; O13, 2026-11-13, placeholders until
the user approves them from screenshots). **The battery line per maker** (O13; `:core`'s
`maker(Build.MANUFACTURER)`, `Maker.kt`, the table the detector uses too): stock and Lenovo
Android's path, on API 35+ `App info › App battery usage › "Allow background usage": on ›
Unrestricted` (Unrestricted is greyed out until the switch is on), on 33–34 `App info › App
battery usage › Unrestricted` (no such row there, not checked on a phone), either ending with
" (on Optimized, a new picture can come hours late)" (approved by the user, 2026-10-24; Next
features, 0.6.0: the picture time); Huawei/Honor `Battery optimisation › All apps › APODroid ›
Don't allow` (MagicOS has no Unrestricted in App info: the friend's Honor X60), Xiaomi/Redmi/POCO
`App info › Battery saver › No restrictions`, Samsung `App info › Battery › Unrestricted`,
Oppo/realme/OnePlus `App info › Battery usage › "Allow background activity": on`, Vivo/iQOO
`Battery › High background power consumption › APODroid: on` (from dontkillmyapp.com and the
makers' guides, unchecked on their phones). While the battery line shows, a button "Battery
optimisation" beside "App info" opens Android's list
(`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, no permission; App info where a phone lacks it).
With "App info"
(`ACTION_APPLICATION_DETAILS_SETTINGS`) and "Don't show again". The latter asks first in a dialog
saying what will happen ("the user has all power"); the pref `pauseHintHidden` keeps it hidden
for good, also when the switch is turned on again (user: it's the user's choice; only clearing
storage brings it back). No battery-optimisation permission.

Permissions (user approved, 2026-10-01), all granted at install with no prompt: `INTERNET`,
`SET_WALLPAPER`, `RECEIVE_BOOT_COMPLETED` (for `setPersisted` and the alarms' boot receiver), `ACCESS_NETWORK_STATE` (Android
14+ throws on `schedule()` for a job with a network constraint without it; no constraint would
wake and fail offline). From 0.7.0 (user, 2026-10-28; O11): `FOREGROUND_SERVICE` and
`FOREGROUND_SERVICE_SPECIAL_USE` for Keep running (normal, at install, no prompt, inert while
it's off). `POST_NOTIFICATIONS` is still not declared.

## Next features

**0.6.0's release gate** (user, 2026-10-24 evening, from a review of the plan; **passed, 0.6.0
released 2026-11-02**): a code freeze
from 25 Oct: only bug fixes, no new texts or settings unless a bug needs one. Stable when V12a–d
(V11a–d again on the tree with B19–B21; user, 2026-10-25), V7, P, SIM and W (user, 2026-10-26) all pass on the same tree with no open `BUG:` line; a fix after V7 or P that touches the
job or the rules reruns that part; the skill's "Still untested" is updated; the release slips a
day rather than ship a job change only a unit test has seen. Why: the last complete passes
(V3–V10) ran on the tree of 21–22 Oct; since then the job was rebuilt (J1, J2, B15, B16, B16f1),
the drawing is new (A3) and Setup changed (J3, A4), and no R8 build or update from 0.5.0 ran on
the new job. **Where it stands** (2026-10-26): V11a–d's findings fixed (user, 2026-10-25: "fix
all bugs and retest"): a killed run keeps its retry and counts as failed (B19, Retries by the
alarms), Set says when its setup couldn't be saved (B20, Set as wallpaper), one source after a
day offline takes the newest post (B21, Turns, rule 2); V12b–d pass on master 654848c, V12a ran
3 of its items. B22–B24 (the stand-in not twice with APOD alone too, the notice saying so; no
"kept" while something takes APOD's place; a run's bookkeeping before `jobFinished`) merged with
tablet PASS and Opus pass 26 clean, and SIM found no bug in 240 seeds (2026-10-26, master
db78d0c; 0.6.0's versionCode 10 from 423f082). V7 (the tablet's real night on battery: two
sets, each on time, nothing twice), V12e and V12f pass (2026-10-27): all of V12 has run, no
`BUG:` line. P's install (26 Oct, 20:24): 0.6.0 over the real 0.5.0 on the FP6, the page not
opened; the migration kept the user's setup exactly (APOD + NASA on both screens, 12 h, checked
by the user in Setup) and the alarms were armed by `MY_PACKAGE_REPLACED`; the first morning set
on time (06:12, NASA's turn). **W passed** (2026-11-02): every morning Tue 27 Oct to Mon 2 Nov read on the FP6 and
the tablet, no `BUG:` line; the weekend waited, the US DST end and the Zurich case (APOD out 06:05, set at
06:12 by the first run after it) as the rules say (`.coord/runs/w/report.md`). **P passed** (2026-11-02, with the user on the FP6, no `BUG:`;
`.coord/runs/p/report.md`). B25 merged. **U0** (user, 2026-11-02: "I want a cleaner page"; the freeze lifted for
exactly these): Setup and Advanced as Settings rows, the page's texts short (picked in a mockup), ⛶'s ✓ setting
again (`Store.setPart`: a new set path beside the rules', only the screens showing that post, no record, turn or
change time touched, so W's result stands); built and checked on the tablet (`plan-0.6.0.md`, U0). **Released**
2026-11-02 (user: "release asap"): the tag's build (fa4b45c's tree, release-signed by the user) passed on the FP6
over W's candidate (the setup kept, the alarms armed again by `MY_PACKAGE_REPLACED`, nothing set, no crash), and the
user approved R's new `1.png` (`plan-0.6.0.md`, R and T). Lenovo's ZuiMemoryCleaner force-stopping the app (V12d after a clock step; W on real days, 28 Oct 13:57 and 30 Oct 01:34, Unrestricted and EXEMPTED: no alarm or job left until the page opened, 19:40 and 20:18): no 0.6.0
change (`on-time-0.7.0.md`). **More testing before the tag** (user, 2026-10-26 afternoon, from a
report on the repo's state; the plan, Remaining 4j): **W**, a week on the FP6 from P's install
to the morning of Mon 2 Nov, the tag not before Tue 3 Nov (moved up to Mon 2 Nov by the user
once W had passed). Why: no real night has run the new
job (every check since J1 used a faked or stepped clock), and the case most European users meet
each morning, APOD out after the picture time (06:05 Zurich from the US DST end on 1 Nov), only
starts then; W also sees real weekday mornings and a weekend. **SIM**, a seeded random test of
the rules over months in `:core` (test code only): the job and the rules gave 13 of the 24 fix
packages B2–B24. **E**, Android 10–14 in the emulator (user, 2026-10-29, a baseline for 0.7.0's Live and on-time
work; `emulator.md`): **postponed to 0.7.0** (user, 2026-11-02: release 0.6.0 as soon as possible), so
0.6.0 ships Android 10–14 untested on a device, an accepted risk. **Accepted, no change:** the RARE bucket's about one alarm
an hour (Android's quota; the pause hint covers Optimized); CR2's finding 4 (a Set that
changes the picture time or "Change every" costs one spare check; noting `lastRunAt` at Set
could drop an owed check); a Set tap in the same input burst as the time menu's closing goes to
the menu (Compose's popup); `edgesBlack`'s 97% kept (Earth's El Niño globe, 96.5%, gets the
blur; revisit with Live, 0.7.0).

**0.6.0: the picture time** (user, 2026-10-22, after the FP6 showed the job's 6 h rhythm;
packages J0–J3 in `plan-0.6.0.md`, before V7 and P). **Seen on the FP6** (0.5.0, battery
Unrestricted, standby bucket `EXEMPTED`, not held by Doze): the job ran every 6 h on the dot
from the install time (04:22, 10:22, 16:22), at each window's end. APOD came out at 06:05 local,
so the 04:22 run found yesterday's and the wallpaper changed only at 10:22, or at once when the
app was opened. **APOD's posting time** (`private/last100.json`, 24 Jun to 1 Oct 2026): 90 of
100 at 00:00 or 00:05 US Eastern (00:05 since 21 Jul), 10 hours late (07:14 to 16:46 Eastern;
eight of them 22–29 Jul, probably the site move). One moment worldwide, so "as soon as it's
out" is 21:05 in Los Angeles, 06:05 in Berlin, 15:05 in Sydney. Decided:
**The rules** (user, 2026-10-23 night, after three reviews: the day's version had nine new
controls and two turn clocks; simplified to three rules a user can read, one time, one clock,
decided by the clock, not by what's on the screen):
(Rule 1's "a picture it hasn't shown" and rule 3 replaced by a plain rotation by the clock,
RO1, 2026-11-14: 0.7.0: turns as a plain rotation; Turns and the picture time.)
1. **At [06:00], and with several sources every [24/12/6/3 h] from then, a screen changes: to
   its source's newest picture, or to the next source (APOD → NASA → Earth) with a picture it
   hasn't shown.**
2. **A newer picture of that source that comes out in the 3 hours after the change still
   replaces it.**
3. **With several sources and nothing new, the picture stays until one comes out.**
- What it gives: Zurich gets APOD (out at 06:05) at about 06:15, Helsinki and Athens (07:05)
  at about 07:15; Los Angeles the one from 21:05 the evening before at 06:00, Sydney the one
  from 15:05 the day before. A missed morning (the phone off, no network) or an evening Set
  changes nothing about the next morning.
- Why by the clock (the third review): the second version's rule 1, "if the newest is already
  on, the next one goes on as soon as it's out", depended on the past. A missed morning in
  Zurich put yesterday's APOD on at 06:00 and today's waited a day, every day after; an
  evening Set (or the update from 0.5.0) in Los Angeles put the 21:05 post on, so 06:00 found
  it on and the change moved to 21:05 for good.
- Why "hasn't shown": NASA and Earth post about one picture a weekday and none at weekends, so
  a plain rotation mostly showed pictures seen before (all weekend, Friday's). Every new picture
  gets on, no change shows an old one again.
- Why 3 hours (user, 2026-10-23 night): with 1 hour, zones where APOD comes out 1–3 hours after
  the picture time (Eastern Europe with 06:00) would be a day late by default.
- **New picture at:** hours only, default 06:00, a few random minutes after (0–10, fixed per
  phone: a zone's phones don't all ask at once). The rules, the checks and Set: Background
  update, Turns and the picture time.
- **The window is measured by the post's publish time, not by when a check sees it** (user,
  2026-10-24): counting "came out within 3 hours" as "seen by a check within 3 hours" lost the
  day for a post first seen after 09:00 (an Optimized phone in Doze, a 503 until then, a slow
  download), and with one source a failed check moved the stamp so a later one set nothing. By
  the publish time a screen with one source has no history.
- **After a day without a check the newest goes on** (user, 2026-10-25, B21): with one source a
  post stored more than 24 h after the source's last successful check goes on whenever a run
  sees it, as a pull would; else a day offline lost the day's picture to the next one, stored
  after the window. By the check gap, not by post dates (NASA's and Earth's weekends).
- **Los Angeles with turns, accepted** (user, 2026-10-24): an evening Set or pull there puts the
  new 21:05 APOD on; on a weekend the next morning then has nothing new, so the screen waits and
  changes the next evening (rule 3 as written), back on the change times within a day or two.
- **Retries by the alarms, not Android's backoff** (user, 2026-10-23 night): in Doze a backoff
  retry waits for a maintenance window, and while it's pending the alarms can't start a run, so
  one NASA 503 at 06:00 lost the morning. No model of each source's posting time (user,
  2026-10-23).
- **Opening the app** sets only what's due (no setting; user, 2026-10-23: one timing story).
- **Dropped** (user, 2026-10-23 night): "Turns start at" with "When I tap Set", the ↑ ↓ order
  (back as the list's drag order, 2026-11-14, OR2: Next features, 0.7.0: the list's order),
  "When nothing is new" (always keep), the "Opening the app" setting, "As soon as it's out",
  each source's expected posting time.
- **Android first** (J0, the night of 2026-10-22/23 on the tablet, on battery, deep Doze;
  `plan-0.6.0.md`, J0): only a wake-up alarm lands in Doze. A pair, `setWindow(T, 10 min)` and
  `setAndAllowWhileIdle(T)`, fired at T + 10 min three times of three; for an Unrestricted app
  the run it started had the network at once, for an Optimized one it waited for Doze's next
  maintenance window (up to hours). One-off jobs with a delay or a deadline and the periodic job
  wake nothing (they wait for a maintenance window), as 0.5.0 saw. `setAndAllowWhileIdle`
  alone has a 1 h window (measured only in the pair). So: the pair of alarms starts the run,
  the periodic job stays as the fallback, no new permission (J2).
- **Optimized phones get the picture late, accepted** (user, 2026-10-23): with the battery
  setting Optimized, Android holds the run's network in Doze until its next maintenance window,
  so the new picture can come hours after the picture time (the same morning on the tablet:
  06:29 for 06:00, but up to 2½ h in J0). The pause hint's "Unrestricted" line says so (J3's
  text). Not tried: fetching inside the alarm's 10 s allowance (`temporaryAppAllowlistDuration`).
  Since 0.7.0 (O10) on Android 12+ only while the app is in RESTRICTED or out of expedited quota:
  the alarms' run is expedited and has the network in Doze (Background update, The alarms);
  10–11 as before. The pause hint's text stays.
- **The job simplified with it** (user, 2026-10-23 night): the periodic job a plain fallback
  whose timing never changes; 0.6.0's notes during a run, retry due and failing series removed:
  fitting the alarm's run into them would have been more new logic than removing them, and
  they were where B5–B12's bugs came from.

**0.6.0: the picture** (user, 2026-10-22 evening; packages A1, A3, A4 in `plan-0.6.0.md`).
Measured on a mock-up of the 89 picture posts of 24 Jun to 1 Oct 2026 on a 1080×2400 screen
(https://claude.ai/artifact/BmiE8FnTn2UPk3tyrHjmyp, private): none is taller than a phone
screen (the tallest 0.56:1), so Fill always cuts the sides; Fill keeps 36% of the width for the
median picture (21% at the widest); Fit on black leaves a 3:2 picture a strip of 30% of the
screen's height. **The user's picks** on a picking page (https://claude.ai/artifact/EHeAzSEZHyvBd9qyo7RzA8,
private; 253 APODs of 27 Dec 2025 to 1 Oct 2026 at `?h=640` in `private/pictures/`, posts in
`private/last100.json` and `private/older200-2026-10-22.json`; six versions each; the picks in
the page's database): of 148 clear picks, Fill 133 (middle 81, brightest area 52), Fit 15 (on
blur 11, mostly text and graphics; on black 4, isolated planets and moons). Always Fill matches
90%. Rules tested against the picks, none better (2026-10-22): hard letter-like edges (the best
text stand-in) 91% on the picks it was tuned on; the share of black pixels (night skies are dark
and were filled); the top and bottom strips each at least 95% black (the user's idea: a sky
over lit ground doesn't count) 89%: it catches the isolated Saturns and moons but shows 4
pictures the user filled (a galaxy, Charon, a crescent, a nebula) whole and smaller; with the
object's width against the frame, worse (stars spread over the whole width). Middle against
brightest area: no pattern near the centre (28 to 21), and a brightest area at the picture's
edge is mostly the wrong subject (middle 22 to 6). Text can't be recognised without an ML
library (ML Kit needs Play Services; Tesseract is large). Decided:
- **Always Fill**, from the middle by default; no automatic choice (user: "the user can change
  it according to their choice anyhow"). Smart Auto, the learned model, the tuning loop, the
  cut-off slider, the zoom and the sharpness guard: dropped. F1's Size [Fill | Fit] row goes
  (Fit as a global choice dropped, user, 2026-10-22: a fitted wide picture is a thin strip);
  Size returns in 0.7.0 as [Fill | Live].
- **Whole picture, per picture**: a toggle in ⛶ mode for the shown post (text, an asteroid, a
  grid of planets), kept like ⛶'s part (per source, for that post only, part of the draft
  until Set; the job uses the stored one). Since U0 (2026-11-02) ⛶'s ✓ saves and sets both at
  once (The app, Choosing the part).
- **The background, automatic by the picture's edges** (user, 2026-10-23 night; no setting):
  black when the picture's edges are nearly black (a planet or a moon on space: the picture
  blends in), else blurred: the picture enlarged to cover the screen, blurred properly (passes
  of a box blur on a small copy, no blocky upscale) and darkened. Why not always blurred: the
  enlarged copy of a bright planet becomes a large glow behind it, not black. In `:core`
  (`Background.kt`, `BackgroundTest`; the thresholds approved by the user, 2026-10-24, from real
  sets on the tablet): `edgesBlack` on a grey copy about 200 px
  wide, true when at least 97% of the outer strip (3% of each side, rounded up, at least 1 px)
  has a grey value up to 40 of 255; `blurBackground` on the picture's middle in the screen's
  shape, decoded at `blurSize` (a quarter of the screen's width and height, 1/16 of its
  pixels): three box-blur passes each way of `blurRadius` (1/25 of the copy's shorter side,
  11 px on a 1080×2400 screen), the edge pixels extended outwards, then 35% darker, opaque.
- **Dropped** (user, 2026-10-23 night, the simplification): the Background setting [Blurred |
  Black] (the edges decide), Fill's part [Middle | Brightest area] (always the middle, ⛶ moves
  it: the picks showed no gain; a saliency map tried again for 0.8.0, issue #3), a "Whole
  picture" switch for "my picture" (Fill, centred, as now).
- Not wanted (2026-10-22): a "Colour" background, a dim setting (bright pictures alone darker
  planned for 0.7.0, issue #2), screen-aware placement (clock and dock), per-screen Size (the
  2026-10-19 decision stands); learning per user (was "after 0.6.0", gone with the smart Auto).
  The preview on the page comes with Live (0.7.0).

**0.7.0: Live, the day's pan** (user, 2026-10-22 evening, 0.7.0's headline, right after 0.6.0):
Size [Fill | Live], with the preview on the page (one drawing code). A wide picture is shown at the screen's full height and moves
across its whole width over the day (about 18 h, from the picture time: left edge at 06:00,
right edge at 24:00), so the whole picture is seen by evening. Built as a **live wallpaper**
(Android's `WallpaperService`, no library, no permission): the app draws the wallpaper itself
at the position for the time of day whenever the screen turns on, smooth, nothing drawn or set
while the screen is off. The clock moves it, not the launcher: it works on any launcher (the
FP6's Niagara scrolls vertically and reports `mWallpaperX=0.0`; a launcher's
sideways scroll is ignored). Live is the home screen's (2026-11-14, below): one engine, on the
lock screen too while that shows the same picture; on 10–13 a live wallpaper is the home
screen's only anyway. Chosen in Android's live
wallpaper picker (the app opens it; one confirmation). Rejected: steps set by the job (it jumps,
every set redraws and recolours the system's theme colours, smooth would mean dozens of sets a
day). **Decided** (user, 2026-10-28; the findings, the plan, the checks and the packages L0–L10
in [live-0.7.0.md](.claude/skills/build-apodroid-android/live-0.7.0.md)):
- **The route straight**, the same speed all day; **at night still at the right edge** until
  the next change time; **with turns each picture crosses within its own turn** (at most 18 h).
- **The position** (L3, `:core`'s `Live.kt`, `livePosition`): by the clock only, not by when the
  picture went on: T the column's current change time (`PictureTime.last`, with turns from two
  sources), the span the shorter of T to the next change time and 18 h (`liveSpan`) in real
  time, p = (now − T) / span held at 0..1 (0 the left edge). So one source or 24 h turns cross
  in 18 h, 12 h turns in 12, 6 h in 6; Zurich's DST nights with "New picture at" 00:00 move the
  right edge an hour on the clock (19:00 in spring, 17:00 in autumn). The engine decodes at the
  screen's portrait height, never enlarged, within 50,000,000 bytes (`liveDecodeSize`,
  `LIVE_DECODE_BUDGET`). Which sets go to the engine (`stillOrLive`) and what a lost Live
  forgets (`lostLive`, Decision 12 below) are `:core`'s too, for L5.
- **A screen with a column where APODroid's live wallpaper isn't on gets Fill**, as 0.6.0 (the
  lock screen on 10–13, a screen left out in the picker, another wallpaper picked, a force stop,
  which makes Android drop a live wallpaper to the default on both screens): at once, by the
  next run.
- **Android 10–13's lock screen: Fill**, not `clear(FLAG_LOCK)` (untested there).
- **Fill stays a still set**, not drawn by the engine (updaters would need the picker; a force
  stop or a crash loop drops a live wallpaper; Auto went in 0.6.0). Live only for those who
  choose it.
- **The preview in ⛶ mode:** with Live the frame where the clock has it (no drag); with "Whole
  picture" a screen-shaped preview on its background, drawn by the wallpaper's code.
- **"My picture" moves too.** **The detector doesn't suggest Live**, and Live doesn't change it
  (2026-11-14: it fires only after a force stop, which drops Live on both screens).
- **Different pictures on home and lock: Live on home, the lock screen still Fill** (user,
  2026-11-14, from L1 on the tablet; replaces 2026-10-28's second picker trip for a lock-screen
  engine). ZUI's picker offers only "Home screen" or "Home screen and lock screen", and either
  puts the one engine on both; a still set on the lock screen leaves it on home alone. So the
  engine draws home's picture, on the lock screen too while it's due the same one; when the
  lock screen is due another (different columns) or the engine isn't on it, the app sets the
  lock screen's still Fill with `FLAG_LOCK`, after the picker by Set's update, later by any run.
  No second picker, no lock-screen engine of its own, the same rule on every Android 14+ picker
  unless L2 (the FP6's picker) shows a reason.
- **The wake frame accepted** (user, 2026-11-14): the lock screen shows the last frame for about
  0.3 s after a wake before the engine redraws (L1); nothing drawn while the screen is off.
- **Home lost Live: both screens Fill** (user, 2026-11-14): two deaths within 10 s or a crash
  loop make Android revert only home to the default, the lock screen keeping the engine, which
  then believes it's on both (L1). The next run that finds home's record noted live and home not
  live (14+ `getWallpaperInfo(FLAG_SYSTEM)` not APODroid's, below 14 home's wallpaper id
  changed) sets both screens' still Fill; Live stays off until the user picks it again in the
  picker. The same for any loss on home (another wallpaper, a force stop).
- The engine draws only Live's own copy of home's picture (`live.jpg`, written when home is
  handed to it), never the source's file; a software canvas, at most 50 MB a decoded picture
  (L1). No new permission.

**0.7.0: on time on any phone** (user, 2026-10-25, after a friend's Chinese phone had no
"Unrestricted"; planned again 2026-10-28 after W's tablet day; after 0.6.0 ships, the freeze; the
findings, the plan, the checks and the packages O1–O17 in
[on-time-0.7.0.md](.claude/skills/build-apodroid-android/on-time-0.7.0.md)). Two layers stop the
job: Android's (Optimized: no network in Doze, hours late; standby buckets) and the maker's
(Xiaomi, Huawei, Samsung, Oppo, Vivo, Lenovo: Autostart, cleaners that force-stop cached apps
and the like, which no app setting gets past). Decided:
- **The expedited run** (user, 2026-10-28, replacing "Exact timing" of 2026-10-25): the alarms'
  run (job 2) is an expedited job on Android 12+ (`setExpedited`; no permission, no setting, no
  notification): AOSP lets it run in Doze and battery saver and past their network blocks, so an
  Optimized phone gets the picture on time while the app is opened now and then (about 8 days
  after the last open Android puts it in RESTRICTED: one alarm a day, up to a day late; the pause
  hint's text stays, user, 2026-10-28). To check first on the tablet (O6); if it fails, "Exact
  timing" comes back to the user. A large save it stops is accepted (B7's path). The buckets
  (RARE's alarm an hour, RESTRICTED's a day) stay the pause hint's Unrestricted.
- **Keep running** (user, 2026-10-28, after ZUI's cleaner force-stopped the Unrestricted app on
  a real afternoon: "we have to find a solution, sadly it will lead to a persistent
  notification"): a permanent foreground service (`specialUse`) that does nothing but keep the
  process out of the cleaners' reach (they stop cached processes; ZUI's only those of oom adj 701
  or more). A switch in Advanced settings, default off, acting at once, with an ⓘ: "Keeps
  APODroid running, so the phone can't stop it between changes. It shows as an active app." The
  notification (channel "Keep running"): "Keeps the wallpaper changing on time"; on Android 13+
  it shows only in Quick Settings' Active apps, since the notification permission is never asked;
  on 10–12 in the shade. Permissions approved: `FOREGROUND_SERVICE`,
  `FOREGROUND_SERVICE_SPECIAL_USE` (at install, no prompt). Texts are placeholders until approved
  from screenshots. O7 and O8 passed on the tablet (2026-11-14: foreground, listed in Active
  apps, alive after a Recents swipe and 30 min in Doze; spared by ZUI's cleaner while a cached
  app and APODroid without it were force-stopped). **Built** (O11, 2026-11-14): Background
  update, Keep running.
- **The detector:** a page open that sets a wallpaper due at a change time 30 min or more ago,
  with no job run started since (not after a boot, an update or the switch turned on since, nor
  while job 2 is pending), **and the alarms found gone** (a force stop deletes them, a bucket
  holding them doesn't; user, 2026-10-28: "Only on a force stop"), shows a box at the first miss
  pointing to "Keep running"; without that sign no box (the pause hint covers the buckets); with it
  already on, the maker's settings path for `Build.MANUFACTURER` and "App info". Keep running is
  offered only there, not by maker before a miss (user, 2026-10-28). **Built** (O14,
  2026-11-14): Background update, The detector.
- **The pause hint per maker** (after the friend's Honor X60, MagicOS 9: App info has no
  "Unrestricted"): the battery line names the phone's own path (`Build.MANUFACTURER`), and a
  button opens Android's Battery optimisation list (`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`,
  no permission). The detection (`isIgnoringBatteryOptimizations`) already works there.
- **Live (D)** checks for a new picture when the screen turns on (the system keeps the current
  live wallpaper running).
- Rejected: "Exact timing" (`SCHEDULE_EXACT_ALARM` and a `dataSync` service per run; the
  expedited job gives the network without them, user, 2026-10-28), Keep running always on (every
  phone would pay), `setAlarmClock`, `USE_EXACT_ALARM`.

**0.7.0: bright pictures darker** (user, 2026-11-02, from GitHub issue #2: "in 0.7.0"; the
findings, the questions and the draft packages BR0–BR3 in
[bright-0.7.0.md](.claude/skills/build-apodroid-android/bright-0.7.0.md)). A bright picture (a
daytime landscape, the Sun, a blue sky) washes out the clock and the widgets. The request: measure
the picture's brightness and darken it modestly (a `ColorMatrix` scale) when it's brighter than a
target; dark pictures untouched; Android's own classes, no library. It revisits 2026-10-22's "a
dim setting" (Muzei's kind: every picture dimmed by a fixed amount). Measured (the frame Fill
shows, mean grey 0–255): over 110 on 12% of A2's 253 APODs, 41% of NASA's 56 newest pictures,
62% of Earth's 40; the issue's X-59 test pilot (NASA, 26 Oct) 130, 163 in the top third. Apps
can't use Android's own wallpaper dimming (`setWallpaperDimAmount` is a system API). **Decided** (user,
2026-11-14, from BR0's 50 picks): the measure is the top third of what the screen shows (mean
grey by `gray`'s weights on a small grey copy), the factor 1 up to a target of 100, else
100 ÷ the mean, at least ×0.7 (the best fit: 23 of 50 picks exactly on the ladder, 44 within one
step; floors 0.6 and 0.5 fitted worse); a switch in Setup, default on; "my picture" and a whole
picture with its background too. **Built** (BR1): `:core`'s `Brightness.kt` (`topThirdMean`,
`darkening`, `BrightnessTest`); BR2 (2026-11-14): `Store.set` darkens (The app, Wallpaper:
the darkening) and the switch "Darken bright pictures" in Setup (The app, Settings); Live (BR3)
follows.

**0.7.0: turns as a plain rotation** (user, 2026-11-11; the rules' draft, the questions and the
packages RO1–RO3 in [rotation-0.7.0.md](.claude/skills/build-apodroid-android/rotation-0.7.0.md)).
Replaces 0.6.0's rule 1 "a picture it hasn't shown" and rule 3, the waiting: at each change time
a screen goes to the next source and shows its newest picture, also one shown before; rule 2,
the 3-hour window, stays. Why: on the FP6 (11 Nov, APOD and NASA, 6 h) APOD didn't come back at
18:00, its picture already shown that morning and NASA with nothing new, so the screen waited;
the user expected APOD ("this is very confusing"), and it took the phone's history to tell why.
Repeats accepted (today's APOD twice a day with 6 h, Friday's NASA and Earth at their weekend
turns). Decided (user, 2026-11-14): the turn's source by the clock (Open 1 (b)), B22's
stand-in not twice goes (Open 2), an ⓘ for "Change every" (Open 3, RO2). `:core`'s rules are
the rotation since RO1 (2026-11-14; Background update, Turns). **Built** (RO1 and RO2,
2026-11-14): the page's video notice lost B22's "skipped" for a stand-in shown before, and
"Change every" has its ⓘ (The app, Settings); RO3, the tablet's faked day, checks it.

**0.7.0: as soon as it's out** (user, 2026-11-13; the FP6's day, the draft rules, the questions
and the packages SO1–SO3 in [soon-0.7.0.md](.claude/skills/build-apodroid-android/soon-0.7.0.md)).
A switch below "New picture at" in Setup (part of the draft), default off: on, a newer picture
of the source a screen shows goes on whenever a check sees it (rule 2 without the 3-hour
window), with hourly checks all day; turns still change only at the change times. Why: NASA
posts 15:00–23:00 in Central Europe, after a 06:00 window, so a screen with NASA alone was a
weekday behind all day (FP6, 13 Nov: lock on 12 Nov's NASA while the page showed 13 Nov's from
17:41); Los Angeles' APOD at 21:05 is the same case, which 2026-10-23 wanted at 06:00, so users
would disagree. Decided (user, 2026-11-14): the draft rules checked, the label and the ⓘ (approved
from SO3's screenshots, 2026-11-14), Setup's summary doesn't name it, the pause hint's
Optimized line unchanged. **Built** (SO1 `:core`, SO2 the switch, `Store` and this file,
2026-11-14): The app, Settings; Turns and the picture time; Traffic. SO3, the tablet's faked
afternoon post, checks it.

**0.7.0: the list's order** (user, 2026-11-14: "The list of the sources, APOD, NASA, Earth,
must be sortable using an icon to drag up and down; once they are in the arrangement you want,
upon setting it, the wallpaper that is on top will be set at 06:00."; the rules, the night
defaults and the packages OR1–OR2 in
[order-0.7.0.md](.claude/skills/build-apodroid-android/order-0.7.0.md)). Decided (user,
2026-11-14, with susanna's review): the order is the turns' order for both screens, part of the
draft; a Set of a new order keeps what's on and the top goes on at the next "New picture at",
the change times in between still turning, phased to it; only a new order (or the first Set
with the list and no anchor) moves the anchor; "APOD stays": until then a Set without a new
order and the switch on keep the owner; with the daily switch off a Set of a new order puts the
top on at once; "Move up" / "Move down" for screen readers. This changes RO1's "a Set never
shifts the rotation" once per new order. **Built** (OR1 `:core`, OR2 `Store`, the page and this
file, 2026-11-14): The app, Settings (the list); Turns and the picture time. The texts "≡",
"Move up", "Move down" and "Change every"'s ⓘ are placeholders; whether TalkBack offers the
actions on the greyed-out last ticked row is the user's FP6 check.

**0.8.0: Fill's frame on the subject** (user, 2026-11-02, from issue #3: "for 0.8.0";
[subject-0.8.0.md](.claude/skills/build-apodroid-android/subject-0.8.0.md)). A saliency map
(what draws the eye), computed once per picture, places Fill's frame by default; ⛶ still moves
it, the middle on any failure. It revisits the smart Auto's map of what matters and Fill's part
[Middle | Brightest area] (dropped 2026-10-23: the picks showed no gain), so first a test on the
desktop against the user's picks (SA0); built only if it beats the middle. The issue's model
(Glimpse, 148 KB) needs TensorFlow Lite, a native library: the stack's rules ask the user; the
same model by hand-written inference in `:core`, or a method without a model, would need none.

**0.6.0: APOD first** (user, 2026-10-19; built 2026-10-20 to 24; the how in `plan-0.6.0.md`).
Why: the app is APODroid, so the plain APOD app stays the default; 0.5.0 couldn't put different
pictures on home and lock, an unticked source couldn't be looked at, and "my picture" for
video days worked only with APOD alone (it shows at once that it was a video day). So the
switch "More NASA pictures" (default off), the list with its two columns, and the stand-in on
every screen where APOD is ticked (The app, Settings; Turns). Rejected: a first-boot screen to
choose the mode (an extra screen; updaters never see it), a "Lock screen: one source" row under
"Where" (it covers only a fixed lock source). **Set applies the setup** (user, 2026-10-20
evening, from a mockup) replaced two tries: "Set as wallpaper changes the settings" (S1), and
before it the hold of 2026-10-19, whose rules a week's simulation showed doing what the page
never said (a held picture followed its source's newer posts, a set on Home stretched the lock
screen's turn from 6 h to 9 h). Kept after a review asked: the label "Set as wallpaper" (not
"Apply"). Every screen's due picture comes from one reading of the clock (reviews of the plan,
2026-10-19). Equal columns change together (user, 2026-10-20 night): after a Set from columns
that differed, each screen had kept its own owner and they stayed opposite for good.

**0.5.0: more NASA pictures of the day** (user, 2026-10-16, from the repo's first GitHub issue):
NASA Image of the Day and Earth Observatory Image of the Day besides APOD ([Other
sources](#other-sources)), taking turns. **Custom hours: dropped** (user, 2026-10-17, after
building and checking it): a fifth choice "Custom" in "Change every", a grid of 24 hours each
painted with a ticked source, so no turn falls at night. Why: it made the app worse and the code
unpredictable (a second one-off job at each change, holds by hand, pending changes, "Next
change"; six fixes after the first check). The code is in the gitignored
`private/wip-0.5.0-with-custom-hours-2026-10-17.patch`. The smaller idea, turns from fixed clock
times, came with 0.6.0's "New picture at". Learned on the tablet (Lenovo, Android 15): a one-off
job without a network constraint ran 45 min late with the screen on (JobScheduler batched it
until another job ran), and its delay alarm doesn't wake a sleeping device.

**From a review of 0.1.1** (user, 2026-10-04, features users would expect; the review is
`private/APODroid missing features users might expect.md`; the checklist `features.md` in the
skill). Built: the pause hint, the status line, the video marker; the sharper wallpaper (0.5.0:
the daily download a rendition the screen's height, about 10× less data than the originals,
which ★ saves); the credit (APOD only, user, 2026-10-20: NASA's feed names one in 1 of 60 items,
Earth's only in `content`, about 100× the data per check); Save every picture with "Largest
original" (every source in use, not APOD only, user, 2026-10-20: APOD only would favour it;
chosen over "what goes on a screen" as easier to keep right: no hook in every set path). Built
and removed before release: Fill / Fit (a fitted wide picture is a thin strip; Next features,
0.6.0: the picture), Wi-Fi only (user, 2026-10-21: whoever wants the picture wants it at once,
not after days of waiting for Wi-Fi), Share (with the mockup of Set applies the setup; a long
press on the title block copies the link).
- **Left out:** previous days (not one page any more), full-screen view, home-screen widget,
  Quick Settings tile (rarely used), translations (English UI stays), sharing the picture file
  (needs a `FileProvider`). Still out, as above: a notification, the still frame on video days,
  help texts, `api.nasa.gov` as a fallback. NASA History Image of the Day (on Flickr; user,
  2026-10-16).

## Open questions

1. Huge pictures (2 Oct 2026, 37.7 MB PNG) cost mobile data, and on a slow connection the
   download may not finish within the roughly 10 minutes Android gives a job. Left open (user,
   2026-10-04): it happened once; the rendition (0.5.0) covers part of it. A stop interrupts the
   run (`onStopJob`): `download()` ends within one chunk, the partial download is thrown away,
   nothing more is set, and the alarms' retry downloads afresh. An HTTP `Range` resume would keep
   it; left.
2. A picture instead of the black ▶ tile on video days (user, 2026-10-18: a black tile isn't
   nice; the user chose "note it down for future", nothing built). Checked on the 13 video
   posts in `private/` (24 Jun to 16 Oct 2026): 11 are NASA's own `<video>` with an mp4 on
   `assets.science.nasa.gov` (no `poster`), 2 are YouTube embeds. So a YouTube thumbnail
   (`img.youtube.com/vi/<id>/hqdefault.jpg`, a new host) covers about 1 in 6; not worth it
   alone. Idea: `MediaMetadataRetriever` on the mp4 URL (Android, no library, no new host),
   a frame a few seconds in, shown dimmed behind ▶ on the page only, never the wallpaper,
   black tile on any failure. The 16 Oct mp4: 25.7 MB, `accept-ranges: bytes`, `moov` at the
   start (so a few hundred KB should do); untested on the phone, other files' layout unknown.
   Fallback if it fails: a local tile (star field or logo), no network.
3. A second picture on the same date (SIM, 2026-10-26; for 0.7.0, user): the job and the page
   skip a source whose stored post is today's (`Store.isCurrent`), so a second post on that date
   arrives only with a pull or once the date is over in US Eastern time (in SIM's 240 seeds 143
   of 408 were ever stored, most at US Eastern midnight, Los Angeles' evening). Never seen in the
   real feeds; accepted for 0.6.0, to look at in 0.7.0.
