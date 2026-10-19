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
- Work for 0.5.0 (more NASA pictures of the day, from the first GitHub issue; 2026-10-16),
  packages with model and effort: [.claude/skills/build-apodroid-android/plan-0.5.0.md](.claude/skills/build-apodroid-android/plan-0.5.0.md)
  (custom hours dropped, 2026-10-17: [Next features](#next-features)).
- Work for 0.6.0 (APOD first, the "All NASA pictures" switch, and the features from 0.1.1's
  review; planned 2026-10-19): [.claude/skills/build-apodroid-android/plan-0.6.0.md](.claude/skills/build-apodroid-android/plan-0.6.0.md).
- Night shifts (user, 2026-10-19): the agents josh, winston, susanna and rosie work through a queue of packages
  unattended (`scripts/night.sh`). How it works, the rules and the queue: [.claude/skills/build-apodroid-android/night.md](.claude/skills/build-apodroid-android/night.md).

## How the user works (2026-10-01)

- Simplest approach that works. No extra screens, options, layers or abstractions until they are
  needed. Don't clutter the UI or the code.
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
  needed it (gridload's rule, 2026-10-02). Current ones: `Post.isNoted` letting a missing
  `wallpaperUrl`, `savedUrl` or `cropUrl` (0.4.0's prefs) match by date alone (0.4.0 → 0.5.0,
  WP7d), so the update sets nothing and keeps ★ and the crop: drop that, but not the user's own
  picture's match by date, two releases after F-Droid ships past 0.5.0. (`Store.daily` taking its
  first value from `getPendingJob`, 0.1.2 → 0.2.0, was removed in 0.4.0.)

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
- Android SDK in `~/Android/Sdk`. The user tests on a real phone over adb, no emulator.
- Git branch `master`, remote `origin`. `dev` holds the night shift's merges until the user fast-forwards master.

**Releases** (F-Droid builds only the newest tag: it skipped 0.1.2 and 0.2.0 and ships 0.2.1,
checked 2026-10-07; its bot finds new tags by itself, skill, Releasing):
- 0.1.0 (2026-10-02): GitHub only.
- 0.1.1 (versionCode 2, 2026-10-02): the large-picture fix and the explanation; first on F-Droid.
- 0.1.2 (versionCode 3, 2026-10-04): declutter fixes, themed icon, screen-reader labels,
  User-Agent, feature graphic.
- 0.2.0 (versionCode 4, 2026-10-04): keep running.
- 0.2.1 (versionCode 5, 2026-10-04): the hint names the path to each setting in App info.
- 0.3.0 (versionCode 6, 2026-10-05): choosing the part (⛶), centred by default, camera photos
  upright. The planned features follow in 0.4.0 (user).
- 0.3.1 (versionCode 7, 2026-10-07): long press copies the link or the explanation, ✓ sets the
  chosen part, Back leaves crop mode, screen-reader labels for the long presses.
- 0.4.0 (versionCode 8, 2026-10-16): video days without a still frame (a ▶ tile; the 16 Oct post
  had no picture and broke 0.3.1), pull to refresh, the wallpaper set when the page opens, the
  button only while the switch is off, notes for problems and video days, the status line.
- 0.5.0 (versionCode 9, 2026-10-18, tagged; release audit WPR 2026-10-18 in
  `plan-0.5.0.md`): NASA Image of the Day and Earth Observatory, turns, tabs, the sharper
  wallpaper (rendition; ★ saves the original). From the UI review (U1–U6): the system's colours,
  source chips in two headed sections, the source line, "On wallpaper" chip and two-line
  explanation, "Set as wallpaper now" always shown, neutral boxes besides the problem box, two
  panes on wide screens. Tablet pass on debug and R8 builds: U7, 2026-10-18, with the chip
  hidden in crop mode (its landscape crop hint was dropped on the phone, WP8a). Release gate
  (WPG, 2026-10-18): static audit clean. Phone pass (WP8, 2026-10-18) on the FP6, release-signed:
  passed after WP8c (chips in a grid at large fonts, segments one height).

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

Risk: this is the site's internal WordPress API, not a documented public API. The category id or
the fields could change. Keep the parsing in `:core`, tolerant (`ignoreUnknownKeys`, nullable
fields), and fail without touching the current wallpaper.

**Traffic (checked 2026-10-02):** one JSON GET per device per day (`Store.isCurrent`), plus the
image. The job's period starts at each device's own enable time, so there's no shared burst at
00:05 US Eastern. 1000 users ≈ 1000–2000 requests a day, like 1000 Tasker tasks against
`api.nasa.gov`. The risk is fragility, not load. Since 0.5.0 (WP7c, 2026-10-18) NASA and
Earth have no post on weekends, so their checks repeat: a page open skips a source checked
without a problem within the hour (The page); the job (3–6 h apart) and its retries always
check. A picture that isn't one is downloaded once (Fetch).

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

- **One page.** Top: today's picture, full width, with its title and date; tap opens the APOD
  page in the browser. Below: the settings. From 600 dp wide (U6, 2026-10-18; the user decided
  it 2026-10-17: one column was too wide on tablets and in landscape) two panes, about 55/45,
  each scrolling on its own: the picture side (tabs, picture, boxes, caption) left, the settings
  right. Pull to refresh works on the picture side only. The tablet (711 dp in portrait) shows
  two panes both ways.
- **Settings** (SharedPreferences), in two sections split by a divider (U3, 2026-10-18;
  texts approved by the user, 2026-10-18): **Wallpaper** (the switch, the status line, **Where**, the set button)
  and **Pictures from** (the source chips, "Change every", video days).
  - A switch for the daily change ("Change wallpaper automatically", user 2026-10-17: with
    turns it isn't daily): schedules or cancels the job.
  - **Pictures from** (0.5.0, WP5; proposed 2026-10-16, approved by the user
    2026-10-17): a row of filter chips, "APOD | NASA | Earth" with ✓ on the selected ones (U3,
    2026-10-18; were tick boxes with the full names, 72 dp each), equal widths, as many on a line as
    the widest fits (`EqualGrid`, WP8, 2026-10-18: from font scale 1.5 on the FP6's 372 dp the
    ticked labels broke inside the word; now two per line there) (`Store.sources`, pref `sources`
    = `APOD,NASA`). Default APOD alone, so an update changes nothing; the last chip is greyed out. With two or more,
    **Change every** [Day | 12 h | 6 h | 3 h] (`Store.interval`, default Day) and they take
    turns (Background update, Turns).
  - **Weekday dialog** (user, 2026-10-17): ticking NASA or Earth says they post on weekdays only
    and Friday's pictures come back on weekends; "Dismiss" (again at the next such tick) and
    "Don't show again" (`weekdayNoticeHidden`). Not for the video-day choice. No text on the page.
  - **Where** (was "Wallpaper on" until U3): home screen, lock screen, or both (user: a setting,
    not fixed). Default: both. Its segments, like the tabs' and "Change every"'s, share one
    height when a label wraps (WP8, 2026-10-18: in the FP6's landscape pane only "Home screen"
    wrapped and its segment stood taller).
  - **If APOD is a video** (was "On video days", shown only with APOD
    ticked alone until U4): always shown (user, 2026-10-17, U4: it overlapped "Pictures from"
    and vanished at a second tick, moving the page). With two or more ticked the choices are
    greyed out, the stored one stays selected for when APOD is alone again, under one grey line
    "The next source takes its turn." (the one help text this adds): with turns a
    video APOD skips its turn, and the video notice and "Video day, wallpaper kept" don't
    show. The choices: keep the previous picture, NASA Image of the Day, Earth Observatory (0.5.0,
    WP3; `VideoDays.NASA`, `.EARTH`), or use my own picture (user). Default: keep, as
    the Tasker task did. The other source is fetched only on APOD video days (after APOD's
    refresh, which tells); its newest post is the wallpaper, set once per its date; choosing it
    on a video day fetches and sets it at once; none stored yet → kept. No still-frame option (often a generic NASA image). "My picture" is
    picked once with the system photo picker (`PickVisualMedia`, no permission) and copied to
    `files/fallback.jpg`; chosen but nothing picked → keep.
  - **Set as wallpaper now**, at the end of the Wallpaper section, with the same two settings:
    the shown tab's picture, which starts that source's turn. **Always shown** (user,
    2026-10-17, U4; was only while the switch is off, user 2026-10-16): with tabs and turns the
    app no longer picks the one picture alone, so the user needs a way to set the one they're
    looking at (before, only ⛶ then ✓). On APOD's video tab with turns it sets nothing ("Video
    day: wallpaper kept"). While the switch is on, changing "Where", picking or changing "my
    picture" or choosing "Use my picture" (on a video day) also sets it at once; turning the
    switch on sets today's picture again (`forgetWallpaper`), which also puts APOD back after
    another wallpaper. "Keep" on a video day sets nothing.
  - No help texts (user: the labels explain themselves), except the pause hint (Background
    update), the video-day notice, the grey line under "If APOD is a video" with turns (U4) and
    the planned ⓘ (Save every picture).
- **Video days on the page (user, 2026-10-16):** a black 3:2 tile with ▶ instead of the picture;
  tapping opens the APOD page in the browser (no player). No ⛶, no ★. A box below the tile
  (user, 2026-10-16; neutral since U5, 2026-10-18: `surfaceVariant` fill, `outline` border) says why the wallpaper didn't change
  and what to do; the text follows the settings (daily off / keep / my picture with or without a
  picture). "Dismiss" hides it for that date (`videoNoticeDate`), "Don't show again" for good
  (`videoNoticeHidden`, no confirmation). The second help text besides the pause hint (the problem box is a status, not help). `refresh()` stores the
  entry without a picture and deletes the previous `apod.jpg`; `isCurrent` doesn't need one.
- **Fetch** in `:core`: newest post → `Post(source, date, title, imageUrl, pageUrl, isVideo,
  explanation)` (`Source` APOD, NASA, EARTH; 0.5.0).
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
  post would hold the wallpaper back until the job's next run) and, while the switch is on, sets a new one at once; it
  also redraws when the job stored a new one in the background, and while open it follows the
  job's sets, stored posts and problems (a prefs listener while resumed; WP5d). **Pull to refresh** (user,
  2026-10-16; material3's `PullToRefreshBox`) always fetches, also when it comes during a
  running update (the run after it keeps the force); a new picture is set at once while
  the switch is on, nothing is set while it's off. A changed title or explanation for the same
  date is stored. A new date closes crop mode, a same-date change doesn't. A failed refresh keeps the
  stored picture; only with none is there an error with a retry button. Video days show "Video"
  after the date. "Set as wallpaper now" on a video day with NASA or Earth chosen and nothing of
  theirs stored says "NASA's Image of the Day hasn't loaded yet".
- **Problems and status line (2026-10-16; texts approved by the user, 2026-10-18):**
  `:core` throws `SiteChanged` (not the JSON expected, no post, a bad date, an image day without
  a picture; `Store` adds a download that isn't an image) `HttpStatus(code)` and `StorageFailed` (the file can't be written or renamed, a full disk:
  "Couldn't store the picture. Check the free space.", no `badUrl`; WP7f); any other
  failure counts as the network, a read timeout too (U7, 2026-10-18: `SocketTimeoutException` is
  an `InterruptedIOException`, which WP7g's stop check first took for a stopped job, so a timeout
  stored no problem and ended the run; `isStop()` in `Store` tells them apart). Each `Store.update` (page, pull, job) stores how its check
  went: a success (a refresh, or today's APOD already stored) sets `checkedAt` and clears
  `problem`; a failure stores `problem` (and `httpCode`). Per source (WP3: `nasa.problem`, …);
  one box, the first source in use with a problem (APOD's first), prefixed with its name
  ("NASA Image of the Day: …"; its `problemNoticeKey` starts `nasa:`) unless it's APOD's with
  APOD ticked alone. The video-day
  texts (user approved, 2026-10-16): "This APOD is a video, so NASA's Image of the Day is the
  wallpaper." (or "… becomes the wallpaper once it has loaded."), with "Keep" "To change that,
  choose another picture for video days below.", and the status line "Wallpaper changed
  16 Oct, 20:43 · NASA Image of the Day" when the wallpaper isn't APOD's or two or more sources
  are ticked. A failed set isn't a problem (the
  toasts and the job's retry cover it). While `problem` is set, it shows in the orange box
  below the picture or tile (user, 2026-10-16; orange for problems only since U5, 2026-10-18: a failing app must look failing, the other boxes are neutral), with "since <day>" when `checkedAt` is more than a day old. Only "Dismiss":
  it hides that problem until its kind or HTTP code changes or a new day starts
  (`problemNoticeKey`, e.g. `http:503:2026-10-16`); no "Don't show again", a failing app must
  not look like a working one. **All boxes that apply show at once** (user, 2026-10-16), stacked
  below the picture or tile, above the title: problem, video notice, battery hint (Background
  update), each with its own buttons. The empty state shows the text above "Try again", no
  problem box, but the battery hint below it. So on a day whose APOD is stored, opening the page clears a pull's failure:
  nothing is wrong until the next picture is due. Under the switch, while it's on:
  "Wallpaper changed 16 Oct, 06:12" (`wallpaperSetAt`, any successful set; 12/24 h as the
  system), or "Video day, wallpaper kept"; before the first set nothing. The page re-reads both
  around each update and after a set.
- **Tabs (0.5.0, WP4):** with two or more sources in use (the ticked ones; with APOD alone, on a
  video day also the video-day source), a segmented row above the picture
  ("APOD | NASA | Earth"), opening on the source on the wallpaper (else the first) until the user
  picks one; the pick survives rotation, not a restart. The shown tab drives the picture, ⛶,
  ★, title, date and explanation; switching closes crop mode. With tabs (U2, 2026-10-17) a
  source line above the title, "NASA Image of the Day · 17 October 2026" (" · Video" on video
  days), replaces the date line below it, and an "On wallpaper" chip at the picture's top left
  (⛶'s and ★'s dark style; none on the ▶ tile, hidden in crop mode) marks the post that's on the wallpaper. Tabs stay
  short; with one source the caption is 0.4.0's: title, then date. "Set as wallpaper now" and ✓ set
  the shown tab's picture (APOD's video tab: what the video-day setting says; with turns
  nothing). The boxes show
  on every tab (the video notice's "Tap ▶ …" only on APOD's, user); pull refreshes every source in use.
- **Explanation (user, 2026-10-17, U2; was a ▾ beside the title):** its first two lines under
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
- **Choosing the part (user, 2026-10-05):** a ⛶ at the bottom left of the picture (mirroring
  the star, none on video days) shows the frame: as large as fits, the rest dimmed. It slides
  along the one direction it can move (left–right; up–down on a picture taller than the
  screen, none of 85 posts checked was). While it shows, a tap doesn't open the page; ✓ sets the
  wallpaper at once, as "Set as wallpaper now" does (user, 2026-10-07); Back closes it without
  setting. The position (0–1) is in the prefs for that post only (date and picture URL), so each new
  picture starts centred; the job uses it too. The user's own picture (video days) is always centred.
- **No notification** (user): it would need the `POST_NOTIFICATIONS` runtime permission, and the
  page shows the title.
- **Icon:** the user's SVGs in `logo/` as an adaptive icon; a monochrome layer for themed icons
  (2026-10-04) computed from the foreground (skill, Icon).

### Save

(User, 2026-10-01.) The star at the bottom right of the picture saves it to the gallery,
`Pictures/APODroid/`, named `APOD_<date>_<Title_Words>.<jpg|png>` (`Post.fileName` in `:core`; the
date makes it unique, only letters, digits, `-`, `_`, at most 100 chars; the extension follows
the image type). It downloads the original (`originalUrl`, to a temp file of its own in `cacheDir`) and
copies it; if that fails, the stored picture (the rendition), and a toast says it's a smaller
copy (0.5.0). While it downloads, ☆ is dimmed ("Saving" for screen readers) and taps do
nothing; one save at a time in the app, so a tap after a rotation says "Still saving" (WP7b,
2026-10-18), and the rotated page fills ★ when it ends. After a save ★ shows the shown tab's
state, not the saved one's. ☆ = not saved, ★ = saved; a second tap (or a quick double tap) doesn't save again. Text glyphs, no icon library. Written
with `MediaStore` (`IS_PENDING`), which needs no permission on Android 10+, hence `minSdk 29`
(user chose that over `WRITE_EXTERNAL_STORAGE` for Android 8–9, or a "Save as" dialog each
time). The saved date, picture URL (so a second post on the same date counts as unsaved; WP7d,
2026-10-18) and `content://` URI are in the prefs; the star checks the URI still exists, so
deleting the file in the gallery empties it again. No star on video days. The
original downloads outside the refresh lock (a refresh doesn't wait for a 10 MB save); the copy
to the gallery holds it, and if a newer APOD was stored meanwhile it saves nothing, says so and
redraws the page.

### Background update

(Decided 2026-10-01.) **`JobScheduler`** (part of Android, no library), not WorkManager (an
AndroidX dependency) or `AlarmManager` (needs our own retry and reboot handling). `DailyJob.kt`:
periodic, every ~6 hours (with turns: Turns), only with network, `setPersisted(true)`. Each run, and each page
open or pull (2026-10-16, one path): `Store.update(force)`, `refresh(source)` for each source
in use unless its stored post is today's (always with `force`), then, while `daily` is on, set
`wallpaperFile()` if its source, date and picture URL aren't `wallpaperSource` +
`wallpaperDate` + `wallpaperUrl` (the post last set, also by the set-now button; 0.5.0, WP3: a
missing `wallpaperSource` is APOD, so 0.4.0's `wallpaperDate` carries over; the user's own
picture counts as APOD's, with no URL; the URL since WP7d, 2026-10-18: a site may post a second
picture on the same date, never seen in the 60 feed items). The sources in
use: the ticked ones, and with APOD alone on an APOD video day the one chosen for video days.
One failing doesn't stop the others, and its stored picture still counts (user, 2026-10-17,
after a code review: a failed check ended the current turn early, and a weekend shows Friday's
stored picture anyway); the set is logged "Despite the failure: …" and the job retries. A
separate
date is needed because a refresh with the switch off downloads without setting. On a video
day with "keep" nothing is saved, so switching to "my picture" later that day takes effect on
the next run or page open. `refresh()` and `save()` hold one lock, as the page and the job can
run at once; the `wallpaperDate` check and every set hold another (not the refresh lock: ★
would wait seconds for a set), so a page and a job running at once set once.
Failures return `needsReschedule` (30 s exponential backoff). Turning the switch on runs the job
at once. The switch's state is the pref `daily` (user, 2026-10-04; until 0.1.2 it was
`getPendingJob`, no pref): hibernation or a force stop deletes the job, so each `onResume`
schedules it again when `daily` is on and the job is missing or has another timing
(`DailyJob.ensure`; scheduling the same id again would stop a running job).

**Turns** (0.5.0, WP5; proposed 2026-10-16, approved by the user 2026-10-17). With two or
more ticked, `wallpaperFile()` is the source whose turn it is: the one on the wallpaper until
`turnStartedAt` is the interval old (less 30 min: the job may run up to its flex early), or at
once when it's unticked or has nothing to show; then the next ticked one in order (APOD → NASA
→ Earth, round to itself) with a stored picture; an APOD video has none. A newer post of the source on the wallpaper is set at once and keeps its turn. A turn
starts when the source changes, with any set by the user (button, ✓, a setting; `startTurn`),
when the switch goes on (`forgetWallpaper`), and when a second source is ticked (so ticking
doesn't end the current turn). Changing the interval waits for the next run. The job: with
turns, period `min(interval, 6 h)` with a 15 min flex (Android raises it to 5 % of the period:
18 min at 6 h), so turns come at even times; such a job doesn't run at once when scheduled, so
switching on offline waits up to a period. With one source 0.4.0's job (6 h, flex 6 h).
A `turnStartedAt` in the future (the clock set back) counts as a turn that is over
(WP7g; on the tablet, U7: the next source set at once, the turn fresh). `run -f` runs push the job's next window one period further each (seen: +21 h after 7 runs);
changing the interval or the switch schedules it afresh. A tick or a video-day choice while
an update runs makes the page run one more after it (that one read the settings before).

Seen on the tablet (Android 15, 2026-10-17): `Num Required Flexible constraints: 0` for the
job, so Android doesn't hold it back for charging or idle; the same on the FP6 (Android 16, WP8).

**Keep running** (user, 2026-10-04; report:
[keep-running.md](.claude/skills/build-apodroid-android/keep-running.md)). Android pauses an app
that isn't opened, and the job doesn't count as use: the restricted bucket after about 8 days
(Android 13+; later or never before) and hibernation after about 3 months (12+), which deletes
the job. While the switch is on, a hint in a neutral box (U5) below the picture (user, 2026-10-16) names only what's missing, one line each:
battery Unrestricted (checked on API 33+ only, 2026-10-04 evening: on 10–12 the 8 days aren't
true) and "pause/manage app if unused" off (API 31+). Each line is the path in App info (user,
2026-10-04: "Unrestricted" alone didn't say where): on API 35+ the battery line says to tap
"Allow background usage" and turn it on first (Unrestricted is greyed out until then), on 33–34
it doesn't (no such row there, not checked on a phone). With "App info"
(`ACTION_APPLICATION_DETAILS_SETTINGS`) and "Don't show again". The latter asks first in a dialog
saying what will happen ("the user has all power"); the pref `pauseHintHidden` keeps it hidden
for good, also when the switch is turned on again (user: it's the user's choice; only clearing
storage brings it back). No battery-optimisation permission.

Permissions (user approved, 2026-10-01), all granted at install with no prompt: `INTERNET`,
`SET_WALLPAPER`, `RECEIVE_BOOT_COMPLETED` (for `setPersisted`), `ACCESS_NETWORK_STATE` (Android
14+ throws on `schedule()` for a job with a network constraint without it; no constraint would
wake and fail offline).

## Next features

**0.6.0: APOD first** (user, 2026-10-19; not built; the how and the layout in `plan-0.6.0.md`).
Why: the app is APODroid, so the plain APOD app stays the default; 0.5.0 couldn't put different
pictures on home and lock, an unticked source couldn't be looked at, and "my picture" for
video days worked only with APOD alone (it shows at once that it was a video day).
- **A switch "All NASA pictures"**, at the end of the settings, default off. Off: the app with
  APOD alone, as now. On: always three tabs, and a grid replaces the chips and "Where": a row
  per source, a tick box for Home and for Lock. Each mode keeps its own settings; the grid
  starts from "Where" the first time. From 0.5.0: two or more ticked → on. Rejected: a
  first-boot screen (an extra screen; updaters never see it), a "Lock screen: one source" row.
- **The grid:** equal columns are one wallpaper (as today), an empty column leaves that screen
  alone, a column with two or more takes turns. One shared clock for both screens.
- **Video days:** the stand-in takes APOD's place on every screen where APOD is ticked, with
  turns too. Four choices in both modes, never greyed: "Skip it" (was "Keep"), NASA, Earth, my
  picture. A dropdown, under APOD's grid row with the switch on.
- **"Set as wallpaper"** stays; with the switch on it asks Home screen / Lock screen / Both.
  A picture set by hand stays until that screen's next normal change (its turn ends, or on a
  one-source screen that source posts); a newer post of its own source is set and keeps the
  hold; unticking a source on that screen ends it.
- Both screens due the same picture: one set with both flags; else each screen on its own (not
  by equal columns: a hold on one screen must survive). Every screen's due picture comes from
  one reading of the clock (reviews of the plan, 2026-10-19). The clock restarts only when the
  turn ends or on a set by hand, not when one screen changes early, so the screens stay in
  step; a second tick restarts it only when no column took turns before.
- The mode switch while the daily switch is on works like a tick: what changes is set at once,
  and every hold is cleared. A held APOD whose next post is a video follows the video-day rule.
- With the switch on the page fetches all three; the job only what the screens need. The chip
  and the opening tab follow the picture's source (a stand-in's tab), not whose turn it is.
- Home and lock set separately: checked on the tablet only first (M0); the FP6 (Android 16)
  sees it in the final pass (user).
- Migration (move to Stack when built): a one-time write at 0.6.0's first start, not a read
  fallback (home's record is the unprefixed keys, so a read can't make it unknown). 0.5.0's
  `sources` and `where` become the mode and the columns; the wallpaper record is copied to the
  lock screen with Both and Lock, and home's is cleared with Lock; with Home the lock screen is
  unknown (the app never set it).

**0.5.0: more NASA pictures of the day** (user, 2026-10-16, from the repo's first GitHub issue;
packages in `plan-0.5.0.md` in the skill, with the details still only proposed). NASA Image of
the Day and Earth Observatory Image of the Day besides APOD ([Other sources](#other-sources)):
- **Sources + taking turns** (built, WP5; The app, Settings; Background update, Turns): a tick
  box per source. With two or more ticked, "Change every" [Day | 12 h | 6 h | 3 h]: they take
  turns, and an APOD video day skips APOD's turn. With APOD ticked alone, "If APOD is a video" also
  offers the other two (the issue's first idea).
  Ticking NASA or Earth says in a dialog that they post on weekdays only (The app, Settings).
- **Custom hours: dropped** (user, 2026-10-17, after building it in WP5b and checking it in
  WP5c). It was a fifth choice "Custom" in "Change every": a grid of 24 hours, each painted with
  a ticked source, so no turn falls at night. Why dropped: it made the app worse and the code
  unpredictable (a second one-off job at each change, holds by hand, pending changes, "Next
  change"; six fixes after the first check). The app stays simple: turns only. The code is in the
  gitignored `private/wip-0.5.0-with-custom-hours-2026-10-17.patch` (all of 0.5.0's work as of
  that day, uncommitted). If turns at night bother users later, the smaller idea is turns that
  start at fixed clock times (00:00, 06:00, …), no grid; not planned. Learned on the tablet
  (Lenovo, Android 15): a one-off job without a network constraint ran 45 min late with the
  screen on (JobScheduler batched it until another job ran), and its delay alarm doesn't wake a
  sleeping device.
- **Tabs above the picture:** a segmented row, one tab per source in use, opening on the one on
  the wallpaper; each with its own picture, ⛶, ★ and explanation.
- **Sharper wallpaper** (below) is part of it, built (WP2; The app, Fetch and Save): NASA Image
  of the Day originals are 4.7 MB typically and up to 97 MB.

**From a review of 0.1.1** (user, 2026-10-04, for features users would expect; the review is
`private/APODroid missing features users might expect.md`; plan and checklist in `features.md`
in the skill). Keep running, the status line and the video marker are built (Background update,
The app). Sharper wallpaper is in 0.5.0; the rest moved to 0.6.0 (user, 2026-10-16).

- **Sharper wallpaper** (built for 0.5.0): the daily download is a rendition the screen's
  height (`?h=<px>`, 0.3–1.2 MB on the posts checked) instead of the 1280 px file; ★ saves the
  original (`content/dam`, 2–10 MB). Why: the originals load on 20 of 20 posts checked, but
  downloading them daily costs about 10× the data.
- **Fill / Fit:** a setting next to "Where", default Fill (today). Fit puts the whole
  picture on black; no ⛶ with Fit (user, 2026-10-05).
- **Wi-Fi only:** a switch for the job, default off.
- **Share:** title and page link via the share sheet; not the picture file.
- **Image credit** under the date, parsed in `:core` (on all 100 posts checked).
- **Save every picture:** a switch for the job, default off, with a small ⓘ next to it: saved
  pictures are full resolution, and saving every day takes a lot of space (user, 2026-10-04).
- **Left out:** previous days (not one page any more), full-screen view, home-screen widget,
  Quick Settings tile (rarely used), translations (English UI stays), sharing the picture file
  (needs a `FileProvider`). Still out, as above: a notification, the still frame on video days,
  help texts, choosing the check time, `api.nasa.gov` as a fallback. NASA History Image of the
  Day (on Flickr; user, 2026-10-16).

## Open questions

1. Huge pictures (2 Oct 2026, 37.7 MB PNG) cost mobile data, and on a slow connection the
   download may not finish within the roughly 10 minutes Android gives a job (each retry starts
   over). Left open (user, 2026-10-04): it happened once. The planned rendition and Wi-Fi only
   switch cover most of it. Seen on 2026-10-07 (`cmd jobscheduler timeout`): a stopped job's
   partial download is thrown away and the retry starts from zero. Since WP7g a stop interrupts
   the run (`onStopJob`): `download()` ends within one chunk, `Store.update` sets nothing more
   (tablet, U7: the run ended within a second, no `.part` left; the retry came 6½ min later, as
   Android batched it, and downloaded afresh). A stop during APOD's or Earth's rendition download
   ends the run too (WP8b, 2026-10-18, found by reading: `Thread.interrupted()` clears the
   flag, so `downloadPicture` fell through to the plain 1280 px picture, downloaded it whole and
   set it). An HTTP `Range` resume would keep it; left.
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
