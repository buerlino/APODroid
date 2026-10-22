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
  Only APODroid's nights use the tablet HA24BZ2Y (user, 2026-10-20: pascaline's nights leave it alone).

## How the user works (2026-10-01)

- Simplest approach that works. No extra screens, layers or abstractions until they are
  needed. Don't clutter the UI or the code.
- **Settings: let users customise as much as possible** (user, 2026-10-22, from now on; it
  replaces "no extra options until they are needed"): a choice users might reasonably want
  becomes a setting, with a sensible default, in "Advanced settings" unless it belongs in
  Setup. Ask the user before adding each one; still no extra screens.
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
  concept in one place only.
- Migrations: remove migration code two releases after F-Droid has shipped past the version that
  needed it (gridload's rule, 2026-10-02). Current ones: `Post.isNoted` letting a missing
  `wallpaperUrl`, `savedUrl` or `cropUrl` (0.4.0's prefs) match by date alone (0.4.0 → 0.5.0,
  WP7d), so the update sets nothing and keeps ★ and the crop: drop that, but not the user's own
  picture's match by date, two releases after F-Droid ships past 0.5.0. 0.5.0 → 0.6.0 (M2,
  2026-10-20): `Store.migrateOnce` with `:core`'s `migrate` (`Migration.kt`, `MigrationTest`), a
  one-time write at the first `Store` use while `allSources` is missing (page or job, a fresh
  install too), under a lock shared by every `Store`, in one commit: `allSources` on unless
  `sources` is APOD alone; with it on `home`/`lock` = `sources` in the columns `where` named;
  the record copied to `lock.*` with Both and Lock, home's cleared with Lock (unknown: set by the
  first run that needs it); the owner APOD in APOD mode, the picture "own" for 0.5.0's APOD with
  no URL on an APOD video date, the stamp `turnStartedAt`. Drop it two releases after F-Droid
  ships past 0.6.0. (`Store.daily` taking its first value from `getPendingJob`, 0.1.2 → 0.2.0,
  was removed in 0.4.0.)

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

- **One page.** Top: the source's name, then today's picture, full width, with its title and
  date; tap opens the APOD page in the browser. Below: the settings. From 600 dp wide (U6, 2026-10-18; the user decided
  it 2026-10-17: one column was too wide on tablets and in landscape) two panes, about 55/45,
  each scrolling on its own: the picture side (the source's title, picture, boxes, caption) left, the settings
  right. Pull to refresh works on the picture side only. The tablet (711 dp in portrait) shows
  two panes both ways.
- **Settings** (SharedPreferences; 0.6.0's layout built in M3, 2026-10-20, its texts and the
  night's defaults approved by the user the same day from the tablet's screenshots; the order
  and the list since S2, Set applies the setup since S4, 2026-10-20; S5's grouping, highlight
  and texts, 2026-10-20 night, approved by the user after it with the night's defaults, from the
  tablet's screenshots): **Wallpaper**: the daily
  switch; the status lines (while the stored daily switch is on); **Setup ▸/▾**, a header that
  opens and closes the draft's settings (a button for screen readers, "expanded"/"collapsed";
  closed: a summary of the **stored** setup under it, "APOD on both screens" in APOD mode (or "home screen", "lock screen", as the stored "Where" says), else
  "NASA on home screen, Earth on both screens", then " · turns every 6 h" while a screen takes
  turns and the daily switch is on, then " · Fill" or " · Fit"): in APOD mode **Where**, with
  the switch "More NASA pictures" on **Pictures from** (the list) and, while the daily switch
  is on and a column takes turns, "Change every"; "Size"; while the daily switch is on "If APOD
  is a video". Closed at a fresh start; it opens by itself while the draft differs from the
  store in it, when "More NASA pictures" is turned (the area swaps), and closes after a Set
  that saved (also "Setup saved. Nothing to set yet."; a failed set leaves it open). Its state
  and Advanced's survive a rotation (the saved state). **Set as wallpaper**, always, after
  Setup. **Advanced settings ▸/▾**, closed by default: while the daily switch is on
  "Save every picture" (F5) with "Largest original" under it while on (F6); then, in both modes and states, the switch "More
  NASA pictures" (no divider since S5).
  - **The highlight** (S5): each draft setting that differs from the stored setup shows in the
    theme's `tertiary` until Set: its heading, a segmented row's chosen segment
    (`tertiaryContainer` / `onTertiaryContainer`) and the row's borders (`tertiary`), the
    list's row (tick box, name, dropdown) where that source's screens differ, the video
    dropdown's border and text (another choice, or a newly picked "my picture" while it's
    chosen), the mode switch and its label, ⛶ (or ✓) while the shown post's draft part differs
    from the stored one, the "Setup" header while anything in it differs (the shown mode's
    "Where" or list, "Change every", "Size", "If APOD is a video"; not ⛶), "Advanced settings"
    while the mode does. Not the daily switch or "Save every picture" (they act
    at once). Screen readers: a highlighted control's state adds "not set yet" ("Selected, not
    set yet", "Checked, not set yet", "On, not set yet"; the headers "collapsed, not set
    yet").
  - **Set applies the setup** (user, 2026-10-20 evening; S4; why: a change took effect at once,
    "but maybe I'm not done"; Next features, 0.6.0). **A draft
    until Set:** "Where", the list (ticks and dropdowns), "Change every",
    "Size", "If APOD is a video", a newly picked "my picture", ⛶'s part and the switch "More
    NASA pictures" change only the page (`MainActivity`'s draft state; no pref is written, no
    set, no scheduling). **A hidden setting goes back to the stored one** (user, 2026-10-22,
    B14, from the code review's finding 7: Set saved a hidden "Change every", and "Setup" stayed
    highlighted with nothing in it highlighted): "Change every" while it's hidden (no column with
    two or more, or the daily switch off), "If APOD is a video" and a newly picked "my picture"
    while the daily switch is off; so Set saves, and "Setup" highlights, only what the page
    shows. A rotation and the system killing the app keep the draft
    (`onSaveInstanceState`); closing the app drops it (a start without a saved draft deletes
    `files/fallback-draft.jpg`). Another start while the page is on top (the launcher, a
    shortcut, `am start -n`) reuses it, so the draft stays: one page (`launchMode="singleTop"`;
    B4, 2026-10-21: without it a start whose intent differed from the task's first put a second
    page on top, without the draft, and deleted the draft's picture). A start while another
    app's screen is on top of the page (the photo picker from "Change picture", App info) still
    stacks a second page, without the draft (`singleTask` would end a pick); its start keeps
    the draft's picture while another page is alive (user, 2026-10-21; B8: `MainActivity`'s
    `pages`, `onCreate` to `onDestroy`; it deleted the picture the page below still used, the
    night's pass 8). One file for both: a pick in the second page replaces the first page's
    picture; a Set renames it to `fallback.jpg` only when its own draft holds the pick (`draftFallback`, `Store.saveSetup`'s `pick`; user, 2026-10-22, B11: B8 renamed whatever was there, so a Set in a second page without a pick replaced the stored "my picture" with the first page's unset one). A Set without a pick leaves `fallback.jpg` and the draft file, for the page that owns it. **The daily switch acts at once, both ways** (S4f2; user,
    2026-10-20 evening: a setting that changes how the whole app works must act at once; S4 had
    it in the draft): off writes `daily` and cancels the job; on writes it, starts the stored
    setup afresh (`Store.startAfresh`: `restartAndForget` with the stored columns, the clock
    restarts, both records forgotten, as 0.5.0's switch did), `update(page = true, show)` sets
    the **stored** setup while `daily` is still on, then the job is scheduled as after Set; the
    other draft changes stay a draft. Toasts "Wallpaper set" or "Couldn't set the wallpaper",
    none when nothing was set (the status line says it). Turned on while a Set, an update or its
    own run goes: one run after it (S4f2's fix round, from the audit: beside it, either run
    could set first and the other's toast said nothing or "Nothing to set yet"); a page update
    meanwhile waits for it, and Set is disabled while it runs. Off is off: off drops a waiting
    run, and a running one restarts nothing and sets nothing more once `daily` is off. What the draft's switch hid (the
    rows below) follows the one switch. **Two kinds of state on the
    page:** the controls and the sources the page offers (the title's dropdown and the swipe; the tabs until S6) follow the draft, so
    turning "More NASA pictures" on offers and fetches all three at once (`Store.update`'s
    `show`); what reports what is set follows the stored setup (`Store.setup`): the status
    lines, the chip, the video notice, the problem box, the battery hint, the opening page. **The
    job follows the stored setup only:** `onResume`'s `DailyJob.ensure` reads `store.daily`, the stored mode and columns; only Set and the daily
    switch schedule or cancel (Set by the stored switch, after saving the draft). **At once, as before:** "Save every picture" (it
    changes no picture) and ★.
  - **Set as wallpaper**: one button at the end of the Wallpaper section, both modes, the daily
    switch on or off, no Home / Lock / Both menu, enabled unless a Set or the daily switch's run goes. It saves the whole
    draft in one commit (`Store.saveSetup`: the settings, the list with its `lastScreens`, the
    parts, "my picture" renamed from `fallback-draft.jpg` to `fallback.jpg`), restarts the
    clock and forgets the records (`:core`'s `restartAndForget`; Turns), then
    `update(page = true, show, set = true)` fetches what a newly offered source needs and sets
    each screen afresh (a changed Fit or "my picture" too, the same picture again), also with
    the daily switch off (a one-off set of the same setup), then schedules
    or cancels the job as the stored daily switch says (not part of the draft; after the set: a job without turns runs at
    once and would set first; with turns `DailyJob.schedule` afresh, so the period starts with
    the restarted clock, else `ensure`). The shown picture decides nothing: to put Earth on a screen,
    tick Earth (0.5.0's "set the shown picture" dropped). A source whose fetch fails: the setup
    is saved anyway and each screen gets what has a picture (Turns). Toasts: "Wallpaper set"
    when a screen was set, also by a page load or the job that set the saved setup before Set's
    own update got there (B14, CR 5: Set said "Nothing to set yet"; `Store.setsSinceSetup`, each
    screen's last set since `saveSetup`); "Setup saved. Nothing to set yet." when none was (an APOD video with
    "Skip it" alone, or nothing stored and the fetch failed: the problem box says why);
    "Couldn't set the wallpaper" when a set failed (also when another worked). With the daily
    switch on the job sets the rest later. A rotation or Back during Set's run or the daily
    switch's keeps its end (B13, 2026-10-22: the brute test's V10 found the job not scheduled
    afresh, no toast, no log line after a rotation): the runs live in the process, not the page;
    the job and the log need no page, the redraw and Setup closing go to the page that started
    the run or the one a rotation made of it, and Set stays disabled in every page while either
    runs. The toast needs no page either, but after Back Android suppresses it (the app is in the
    background and has no notification permission; tablet, B13).
  - A switch for the daily change ("Change wallpaper automatically", user 2026-10-17: with
    turns it isn't daily): at once both ways since S4f2 (above); the pref `daily`.
  - **More NASA pictures** (was "All NASA pictures" until S5: user, 2026-10-20 evening, after a
    review called the mockup's "Extended API mode" a developer's word; 0.6.0, M3; the pref `allSources`, default off: APOD mode). Part of
    the draft: "Where" and the list swap at once and the page offers the new mode's sources;
    Set saves it and reschedules the job (its period follows the mode). Each mode keeps its
    settings: "Where" is APOD mode's, the list the other's (`Store.list`, kept in APOD mode
    too). The first time on, the list starts from the draft's "Where" (the list is saved only
    once it was on: `Store.hasList`), also after a Set in APOD mode changed "Where" (B14, CR 3:
    the draft's list kept the old "Where"'s). Switching it off and Set leaves a screen "Where" doesn't
    name alone, with its record, so the chip still knows it.
  - **The list** (switch on; S2, 2026-10-20, from the user's sketch; replaced M3's grid of a
    tick box per screen): "Pictures from", a row per source: a tick box with its short name,
    "APOD", "NASA", "Earth", as in the title above the picture (`shortLabel`; user, 2026-10-20: the full names took
    three lines on the tablet), and a dropdown Home screen / Lock screen / Both (an outlined
    button "Both ▾" and a `DropdownMenu`, ✓ before the chosen one, as "If APOD is a video"'s);
    the dropdown wraps under the name when both don't fit on one line. A tick puts the source
    on its dropdown's screens, an untick takes it off both, a new choice for a ticked source
    moves it (`:core`'s `placing`, in the draft). An unticked row's dropdown is
    greyed out and shows the screens the source had last (`<source>.lastScreens`, written with
    the list by Set; the draft's while unsaved; Both when it never had any); ticking it puts the source there. The last ticked
    row can't be unticked (greyed out); its dropdown still moves it. Screen readers: the row
    "NASA" (a checkbox), the dropdown "NASA on: Home screen". The prefs are the columns `home`
    and `lock` (comma lists, `Store.list`; `Store.columns` is the list with the switch on, else
    APOD on the screens "Where" names, `apodColumns`). With a column of two or more, while the
    daily switch is on, **Change every** [24 h | 12 h | 6 h | 3 h] (`Store.interval`, default
    24 h, the pref `DAY`; "Day" until S5) below the list; hiding it moves the page only after a tick. 0.5.0's chips and M3's grid are gone.
    While the daily switch is off "Where" or the list still shows (S4; S2 hid them), so a
    one-off Set uses settings the user can see.
  - **Weekday dialog** (user, 2026-10-17): ticking NASA or Earth where it wasn't ticked on
    either screen says they post on weekdays only and Friday's pictures come back on weekends;
    "Dismiss" (again at the next such tick) and "Don't show again" (`weekdayNoticeHidden`). Not
    for the video-day choice or the mode switch. No text on the page.
  - **Where** (was "Wallpaper on" until U3): home screen, lock screen, or both (user: a setting,
    not fixed), APOD mode's only (the list has its columns). Default: both. Its segments, like "Change every"'s, share one
    height when a label wraps (WP8, 2026-10-18: in the FP6's landscape pane only "Home screen"
    wrapped and its segment stood taller).
  - **Size** [Fill | Fit] (0.6.0, F1, 2026-10-20; the pref `fit`, default Fill; the heading
    "Size" (was "Fit") and the choices approved by the user in C, 2026-10-20): under "Where" or "Change every". One
    setting for both screens, so one picture is still one set. Fill: the screen's shape cut from
    the picture (⛶ chooses where). Fit: all of it, as large as fits, centred on black
    (Wallpaper). No ⛶ with Fit; choosing Fit closes crop mode. Part of the draft: Set puts the
    same pictures on again with the new size (`restartAndForget`; a screen left alone keeps its
    picture as it was set).
  - **If APOD is a video** (was "On video days"; one setting for both modes): a dropdown (M3,
    2026-10-20: a `DropdownMenu` under an outlined button "Skip it ▾", ✓ before the chosen
    one; were four radios), under its heading after "Size" in both modes (S2; M3 had it under
    APOD's grid row), hidden while the daily switch is off; with the switch "More NASA pictures"
    on greyed out while APOD has no tick (so the page doesn't move). Never greyed out otherwise (0.5.0 greyed it with two or more ticked, under a grey line).
    At font scale 2.0 and a phone's width its "▾" wrapped alone under "NASA Image of the Day"
    (V3, 2026-10-21): a no-break space before every "▾" and "▸" (the dropdowns, the title, the
    headers) keeps the glyph with the last word (user, 2026-10-21; B9).
    The choices: "Skip it" (`VideoDays.KEEP`; was "Keep the previous picture"), NASA Image of
    the Day, Earth Observatory (0.5.0, WP3; `VideoDays.NASA`, `.EARTH`), or "Use my picture"
    (user). Default: "Skip it", as the Tasker task did. The choice fills APOD's turn on every
    screen where APOD is ticked, with turns too ("Skip it": a screen with APOD alone keeps its
    picture, one with turns passes to the next source; Turns). The other source is fetched only
    on APOD video days (after APOD's refresh, which tells); its newest post is the wallpaper,
    set once per its date; choosing it in the draft on a video day fetches it for its page, and
    Set puts it on; none stored yet → kept. No still-frame option (often a generic NASA image).
    "My picture" is picked with the system photo picker (`PickVisualMedia`, no permission) and
    copied to `files/fallback-draft.jpg` (a rotation during the copy keeps the pick and the
    choice, B14: the copy runs in the process, not the page); Set renames it to `files/fallback.jpg`. Choosing "Use
    my picture" with no picture in either file opens the picker at once and the choice is made
    only once a picture is there (backing out keeps the one before); while it's chosen, "Change
    picture" beside the dropdown ("Pick picture" for 0.5.0's choice without a picture, which
    keeps).
  - No help texts (user: the labels explain themselves), except the pause hint (Background
    update), the video-day notice and the ⓘ (Save every picture, Save).
- **Video days on the page (user, 2026-10-16):** a black 3:2 tile with ▶ instead of the picture;
  tapping opens the APOD page in the browser (no player). No ⛶, no ★. A box below the tile
  (user, 2026-10-16; neutral since U5, 2026-10-18: `surfaceVariant` fill, `outline` border) says why the wallpaper didn't change
  and what to do; the text follows the settings (daily off / keep / my picture with or without a
  picture). With the switch on (M4; also with turns) it says what takes APOD's place ("This
  APOD is a video, so your own picture takes its place.", "… so NASA's Image of the Day takes
  its place." or "… takes its place once it has loaded.", "… so it's skipped." with "Skip it";
  approved by the user, 2026-10-20), and with APOD ticked nowhere only "This APOD is a video.". "Dismiss" hides it for that date (`videoNoticeDate`), "Don't show again" for good
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
  post would hold the wallpaper back until the job's next run) and, while the stored daily switch is on, sets a new one at once; it
  also redraws when the job stored a new one in the background, and while open it follows the
  job's sets, stored posts and problems (a prefs listener while resumed, WP5d; either screen's record since M4;
  not during a load, a Set or the daily switch's run, which redraw at their end, B14: during a
  Set the home screen's new line showed beside the lock screen's old one; a change while it
  redraws is read once more, CR 4). **Pull to refresh** (user,
  2026-10-16; material3's `PullToRefreshBox`) always fetches, also when it comes during a
  running update (the run after it keeps the force); a new picture is set at once while
  the stored daily switch is on, nothing is set while it's off. A changed title or explanation for the same
  date is stored. A new date closes crop mode, a same-date change doesn't. A failed refresh keeps the
  stored picture; only with none is there an error with a retry button. Video days show "Video"
  after the date. The page fetches the sources it offers (`update`'s `show`: the draft's mode's,
  so a source ticked in the draft only has a picture), besides those in use.
- **Problems and status line (2026-10-16; texts approved by the user, 2026-10-18):**
  `:core` throws `SiteChanged` (not the JSON expected, no post, a bad date, an image day without
  a picture; `Store` adds a download that isn't an image) `HttpStatus(code)` and `StorageFailed` (the file can't be written or renamed, a full disk:
  "Couldn't store the picture. Check the free space.", no `badUrl`; WP7f); any other
  failure counts as the network, a read timeout too (U7, 2026-10-18: `SocketTimeoutException` is
  an `InterruptedIOException`, which WP7g's stop check first took for a stopped job, so a timeout
  stored no problem and ended the run; `isStop()` in `Store` tells them apart). Each `Store.update` (page, pull, job) stores how its check
  went: a success (a refresh, or today's APOD already stored) sets `checkedAt` and clears
  `problem`; a failure stores `problem` (and `httpCode`). Per source (WP3: `nasa.problem`, …);
  one box: in APOD mode the first offered source with a problem (APOD's first); with the switch on (all
  three fetched) the shown source's, else the first ticked source's (M4; approved by the user,
  2026-10-20; the second case untested until V), prefixed with its name
  ("NASA Image of the Day: …"; its `problemNoticeKey` starts `nasa:`) unless it's APOD's with
  APOD ticked alone. The video-day
  texts (user approved, 2026-10-16): "This APOD is a video, so NASA's Image of the Day is the
  wallpaper." (or "… becomes the wallpaper once it has loaded."), with "Keep" "To change that,
  choose another picture for video days below.", and the status line "Wallpaper changed
  16 Oct, 20:43 · NASA" when the wallpaper isn't APOD's or two or more sources are ticked (the
  source's short name since 2026-10-20, user: the full names wrapped in the tablet's settings
  pane and would on the phone). A failed set isn't a problem (the
  toasts and the job's retry cover it). While `problem` is set, it shows in the orange box
  below the picture or tile (user, 2026-10-16; orange for problems only since U5, 2026-10-18: a failing app must look failing, the other boxes are neutral), with "since <day>" when `checkedAt` is more than a day old. Only "Dismiss":
  it hides that problem until its kind or HTTP code changes or a new day starts
  (`problemNoticeKey`, e.g. `http:503:2026-10-16`); no "Don't show again", a failing app must
  not look like a working one. **All boxes that apply show at once** (user, 2026-10-16), stacked
  below the picture or tile, above the title: problem, video notice, battery hint (Background
  update), each with its own buttons. The empty state shows the text above "Try again", no
  problem box, but the battery hint below it. So on a day whose APOD is stored, opening the page clears a pull's failure:
  nothing is wrong until the next picture is due. Under the switch, while it's on:
  "Wallpaper changed 16 Oct, 06:12" (`wallpaperSetAt`, any successful set; 12/24 h and the time zone
  as the system; the zone followed while the page is open, 12/24 h at the next resume, B13: V9
  saw the old zone until the process restarted), or "Video day, wallpaper kept" (`:core`'s `videoDayKept`: APOD ticked there, its turn,
  nothing standing in; with turns "Skip it" passes the turn, so nothing is kept; a record without a
  picture, as Set leaves it, counts when the screen was set before, `wallpaperSetAt`, else not: a
  fresh install's first video day shows no line, B1); before the
  first set nothing. In APOD mode for the screen "Where" names (with Lock the `lock.*` record).
  With the switch on (M4) one line while both screens show the same picture, else a line per
  screen set or kept: "Home screen changed 16 Oct, 06:12 · NASA", "Lock screen: video day,
  wallpaper kept"; the user's own picture " · My picture"; a screen never set has no line
  (approved by the user, 2026-10-20). " · My picture" in APOD mode too (user, 2026-10-20 night;
  B3: it had none there, while a stand-in had " · NASA"). The page re-reads both
  around each update and after a set.
- **The title and the swipe** (S6, 2026-10-21, from the user's mockup of 2026-10-20 evening;
  tabs until then: 0.5.0's WP4, 0.6.0's M4; the texts "APOD ▾", the menu's short names and
  "Picture from: …", and the night's defaults (the name alone with one source, the failure
  text on the shown page only, a fresh pager when the list changes) approved by the user,
  2026-10-21 morning, from the tablet's screenshots). The sources the page offers: with the switch "More
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
  rotation) and the system killing the app (Android's saved state: seen after `am kill`, tablet,
  2026-10-20), not a swipe-away from Recents (to check in P on the FP6; user, 2026-10-20).
  The shown source drives ⛶'s part, the boxes, title, date and explanation; another one closes
  crop mode. With two or more sources (U2, 2026-10-17) a source line above the title, "NASA
  Image of the Day · 17 October 2026" (" · Video" on video days), replaces the date line below
  it, and a chip at the picture's top left (⛶'s and ★'s dark style; none on the ▶ tile, hidden
  in crop mode) marks the post on a screen, from the records (`Store.screenState`): with the
  switch on "On home screen", "On lock screen" or "On wallpaper" (both); in APOD mode "On
  wallpaper" for the screen "Where" names (M4). With one source the caption is 0.4.0's: title,
  then date. The pages follow the draft (Settings, Set applies the setup); the chip and the
  opening page the stored setup. The boxes show for every source (the video notice's "Tap ▶ …"
  only on APOD's page, user); pull refreshes every source in use and covers the picture side
  (the left pane on wide screens).
- **Credit** (F4, 2026-10-20; approved by the user in C the same day): one line under the date
  (APOD mode: title, date, credit) or, with two or more sources, under the title on APOD's page; smaller and
  greyer than the explanation (`bodySmall`, `onSurfaceVariant`), one line with an
  ellipsis while the explanation is closed (C; was two), all of it while the explanation is open (a cut credit makes the caption
  expandable too). The pref `credit` is written with the post; a post stored by 0.5.0 has
  none, so nothing shows until the next post or a pull (`refresh` stores what it fetched; no
  migration).
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
  download is a complete, decodable image. **Fit** (F1): the picture decoded upright by
  `ImageDecoder` straight to `:core`'s `fitFrame` size (`setTargetSize`; F1f1, user,
  2026-10-20, from a review by reading: Fill's sample rule left a wide picture at the
  rendition's full size, 3726×2484, 37 MB, for one bilinear shrink of about 3.3×, which can
  make stars jagged; a smaller picture is scaled up), drawn 1:1 into the frame on a black
  bitmap of the screen's portrait size (about 11 MB on the FP6) and set with `setBitmap`, the
  hint all of it. `info.size` and `setTargetSize` are the upright size on Android 10 and later
  (AOSP's `ImageDecoder.cpp`; an EXIF-turned "my picture" with Fit is checked in V1).
  **Home and lock set separately** (M0, 2026-10-20; tablet, Android 15; the FP6 unchecked):
  both flags make one shared wallpaper (`dumpsys wallpaper`: `id=154: mWhich=3`, "Lock
  wallpaper state" empty); `FLAG_SYSTEM` alone first moves the shared one to the lock screen
  and sets home anew (`System id=155: mWhich=1`, `Lock id=154: mWhich=2`: home NASA, lock
  still APOD); `FLAG_LOCK` alone replaces only the lock one (`Lock id=156: mWhich=2`, home
  still 155); both flags again leave one shared wallpaper and no lock one (`id=157:
  mWhich=3`, "Lock wallpaper state" empty). No screen went blank.
- **No Share** (S6, 2026-10-21; dropped with the mockup of Set applies the setup, user,
  2026-10-20 evening): F3's ⤴ beside ★ and on the ▶ tile, which sent "<title>\n<page link>" to
  the share sheet, is gone with its code. Long press on the title block still copies the link.
- **Choosing the part (user, 2026-10-05):** a ⛶ at the bottom left of the picture (mirroring
  the star, none on video days) shows the frame: as large as fits, the rest dimmed. It slides
  along the one direction it can move (left–right; up–down on a picture taller than the
  screen, none of 85 posts checked was). While it shows, a tap doesn't open the page; ✓ closes it
  and keeps the part in the draft ("Done" for screen readers; S4, 2026-10-20: ✓ no longer sets
  the wallpaper, as it did since 0.3.1); Back closes it without keeping; reopening ⛶ shows the
  draft's part. A rotation or a resize closes it too (the page is recreated), dropping a dragged
  part not yet kept with ✓, as Back does, and closes an open dropdown; the draft, Setup's and
  Advanced's state and the shown source survive (user, 2026-10-22: left as is; V9, V10). Set stores it (a Set while crop mode is open keeps the dragged part, as ✓
  would). The position (0–1) is per source for that post only (date and picture URL), so each new
  picture starts centred (a new post drops a draft part); the job uses only the stored one. The user's own picture (video days) is always centred.
- **No notification** (user): it would need the `POST_NOTIFICATIONS` runtime permission, and the
  page shows the title.
- **Icon:** the user's SVGs in `logo/` as an adaptive icon; a monochrome layer for themed icons
  (2026-10-04) computed from the foreground (skill, Icon).

### Save

(User, 2026-10-01.) The star at the bottom right of the picture saves it to the gallery,
`Pictures/APODroid/`, named `APOD_<date>_<Title_Words>.<jpg|png>` (`Post.fileName` in `:core`; the
date makes it unique, only letters, digits, `-`, `_`, at most 100 chars; the extension follows
the image type). It downloads the original (`originalUrl`, to `cache/original.part`, one name since one save runs at a time; a save killed with the
process left one for good until B13, so each save and the page's first start delete
leftovers) and
copies it; if that fails, the stored picture (the rendition), and a toast says it's a smaller
copy (0.5.0). While it downloads, ☆ is dimmed ("Saving" for screen readers) and taps do
nothing; one save at a time in the app, so a tap after a rotation says "Still saving" (WP7b,
2026-10-18), and the rotated page fills ★ when it ends. After a save each page's ★ shows its own source's
state. ☆ = not saved, ★ = saved; a second tap (or a quick double tap) doesn't save again. Text glyphs, no icon library. Written
with `MediaStore` (`IS_PENDING`), which needs no permission on Android 10+, hence `minSdk 29`
(user chose that over `WRITE_EXTERNAL_STORAGE` for Android 8–9, or a "Save as" dialog each
time). The saved date, picture URL (so a second post on the same date counts as unsaved; WP7d,
2026-10-18) and `content://` URI are in the prefs; the star checks the URI still exists, so
deleting the file in the gallery empties it again. No star on video days. The
original downloads outside the refresh lock (a refresh doesn't wait for a 10 MB save); the copy
to the gallery holds it, and if a newer APOD was stored meanwhile it saves nothing, says so and
redraws the page.

**Save every picture** (0.6.0, F5, 2026-10-20; the pref `saveEvery`, default off): a switch
in "Advanced settings" while the daily switch is on, with a small ⓘ ("About saving") that opens a
dialog: "Each new picture is saved to Pictures/APODroid: full size up to the largest original,
else a screen-sized copy. A few hundred MB a month or more." (OK; user, 2026-10-20 night, after
F6: C's "Each new picture is saved full size…" isn't true under the limit; B3). After each job run (not the page's), also
after a failed one, not after a stop, `Store.saveInUse` saves each source in use's stored post
in order APOD → NASA → Earth, as ★ does (`save`: the original, else the stored picture). The set
in use is `:core`'s `sourcesInUse`, the one `update` fetches (the ticked sources and
the stand-in on an APOD video day); the choice is `toSave` (`SavingTest`): no video, no
post without its picture, none whose `savedDate`/`savedUrl` match. No URI check, so a picture
deleted in the gallery isn't saved again (★ still checks it). So APOD mode saves APOD, on a
video day the stand-in; "Skip it" or "my picture" saves nothing. One skipped while ★ saves
(`BUSY`) or failed is logged, doesn't retry the job, and the next run tries. A stop ends it:
`save` rethrows an interrupted download (`isStop()`) instead of falling back. Built and
unit-tested; the device checks wait for the tablet (V). **One try of the original
per post** (user, 2026-10-20, from a review by reading; F5f1): a stopped save noted nothing, so
each retry of the job and each later run downloaded the original from zero (a 97 MB NASA
original on a slow connection, all day). The job notes the try first (`<source>.saveTried`,
the picture URL, committed before the download); `toSave` then says not to try it again, and
the next job save of that post saves the stored picture, the smaller copy, as after a failure
(`save(post, tryOriginal = false)`). A post with another picture URL tries its original. A save
that returns (also `BUSY`, which downloaded nothing) clears the note. ★ always tries the
original and ignores the note.
**A size limit** (user, 2026-10-20 evening, after a 208 MB NASA PNG; F6, built the same night,
tablet PASS; the label, the default and 1 MB = 1,000,000 bytes approved by the user after it): "Largest original" [10 MB | 20 MB | 50 MB | 100 MB | No
limit], a dropdown under "Save every picture" in "Advanced settings" while that switch is on
(indented, label then "20 MB ▾", ✓ before the chosen one; screen readers "Largest original:
20 MB"), default 20 MB, at once (not part of the draft; the pref `saveLimit`, MB, 0 = no limit;
1 MB = 1,000,000 bytes). The job only (`saveInUse` → `save(post, original, limit)`): `:core`'s
`download(…, limit)` throws `TooLarge` before reading the body when `Content-Length` is over
it, else once the bytes pass it (the `.part` is deleted); `save` logs it ("Original over the
limit") and saves the stored picture, as after a failure: no retry, `saveTried` noted as
before. NASA's enclosure `length` isn't parsed, so it isn't used. ★ ignores the limit.

### Background update

(Decided 2026-10-01; the timing changes with the picture time, 2026-10-22, Next features.) **`JobScheduler`** (part of Android, no library), not WorkManager (an
AndroidX dependency) or `AlarmManager` (needs our own retry and reboot handling). `DailyJob.kt`:
periodic, every ~6 hours (with turns: Turns), only with network, `setPersisted(true)`. **Always any network** (B10); a `wifiOnly` key in a dev build's prefs is ignored. Each run, and each page
open or pull (2026-10-16, one path): `Store.update(force)`, `refresh(source)` for each source
in use unless its stored post is today's (always with `force`), then, while `daily` is on,
`setDue()`: what each screen is due (`:core`'s `dueWallpapers`, Turns), set where it isn't
what the screen's record notes. **A record per screen** (0.6.0, M2, 2026-10-20): home's the
unprefixed keys (as before), the lock screen's under `lock.`: `wallpaperSource` (whose turn,
the owner), `wallpaperPicture` (the picture's source or `own`) with `wallpaperDate` and
`wallpaperUrl` (the URL since WP7d, 2026-10-18: a site may post a second picture on the same
date, never seen in the 60 feed items; none matches by date), `wallpaperStamp` (the turn it
belongs to) and `wallpaperSetAt`. Both screens
due the same picture: one set with both flags (one wallpaper); else `FLAG_SYSTEM` and
`FLAG_LOCK` each (M0). Each screen's record is written right after its own set; a failed set
leaves it, so the retry sets that screen only. Set ("Set as wallpaper", S4) writes the
settings, the clock and the records in one commit (`Store.saveSetup`), then `update(set =
true)` sets, also with `daily` off; `update` returns `Updated` (what it set, whether a set
failed, the first failure) and throws only a stop. The page reads home's record, the lock screen's
when only it has a column (APOD mode with "Where" = Lock). The sources in use (the job): the
ones ticked on either screen, and on an APOD video day the stand-in, while APOD is ticked
somewhere; the page with
the sources it offers (the draft's mode's: all three with `allSources` on). One failing doesn't stop the others, and its stored picture still counts (user, 2026-10-17,
after a code review: a failed check ended the current turn early, and a weekend shows Friday's
stored picture anyway); the set is logged "Despite the failure: …" and the job retries. A
separate
date is needed because a refresh with the switch off downloads without setting. On a video
day with "keep" nothing is saved, so switching to "my picture" later that day takes effect with
Set (S4). `refresh()` and `save()` hold one lock, as the page and the job can
run at once; `setDue()`, every set and every change to the records or columns hold another (not
the refresh lock: ★ would wait seconds for a set), so a page and a job running at once set once;
the page calls them off the main thread.
Failures return `needsReschedule` (30 s exponential backoff). Set with the daily switch on
schedules the job after its set: with turns afresh (`DailyJob.schedule`: the period starts with
the restarted clock; `ensure` would keep the old windows and the first turn would end up to a
period late), otherwise `ensure` (a fresh one without turns runs at once and would set first; with the
timing unchanged it schedules nothing and the job keeps its window: B14 on the tablet, +6 h
after a run; whether Android keeps the window when the timing changes, V9's case, is P's); off
cancels it. Turning the daily switch on (S4f2) does the same after setting the stored setup
afresh (`Store.startAfresh`); off cancels at once. Quick taps (on, off, on while the first
switch-on still runs) set once, not twice (user, 2026-10-20 night; B6, 2026-10-21: S4f2 set
the same picture twice, 0.6 s apart): an on while a switch-on run goes owes one run after it,
which that run pays when it got past its set's check of the switch (`Updated.wentToSet`), so
only a run that gave up (the switch off at its check) leaves the last on to run once more and
the taps never end on with nothing set (`:core`'s `SwitchRuns`, `SwitchRunsTest`). An off and on
that come after the run's set (during its redraw: taps about 0.35–1 s apart on the tablet) owe a
run that sets the same picture once more and schedules the job (B6's fix round: the off had
cancelled it; accepted, user, 2026-10-21: the end state is right). **No reschedule stops a running run** (user, 2026-10-20 evening; S4f3):
scheduling the job's id again stops a run, so a Set with turns during a run delayed its saves
to the next run (an `ensure` with another timing too). While a run goes (`DailyJob`'s count of
runs, process-wide: the page and the job share one process), `schedule` and `ensure` only note
what's wanted (afresh wins), and the run does it after `jobFinished`, by the stored setup then
and only while `daily` is on. Off still cancels at once, a running run too, and drops the note.
Accepted: the process dying before the run's end loses the note; `onResume`'s `ensure` covers a
changed timing, but a fresh period for the same timing is lost (turns stay on their clock, the
job may come up to a period off). **A failed run keeps its backoff retry** (user, 2026-10-20
night; B5: S4f3 let a reschedule noted during a failed run replace the retry, so with turns
the retry waited up to a period; B7, user, 2026-10-21, from the night's pass 8 and regression
4): the note waits for the retry, **one retry at most**: the retry's end does it, good or failed
(a source that keeps failing, e.g. a picture that isn't one all weekend, would otherwise hold
back a Set's period or a new interval for days; the cost: a failed retry's own backoff is
replaced by the fresh schedule, the next period retries). A run `onStopJob` stopped counts as
failed (`stoppedBySystem`), also during the saves after a good update, so a noted reschedule
doesn't replace the system's retry. After a failed run, until the next run starts, a retry is
due: a `schedule` or `ensure` then (a Set just after the run; with turns it restarted the
period, so the failing source waited up to a period) is noted as during a run and done at the
retry's end; the process dying loses it, as a note. Only the first retry of a failing series
is waited for (user, 2026-10-21; B12: B7 kept a retry due after a failed retry
with nothing noted, so while a source kept failing every Set waited for the next retry, which
Android delays twice as long after each failure, up to 5 h): after a failed retry no retry is
due, and a later `schedule` or `ensure` is done at once (it replaces the job's own backoff with a
fresh schedule: the next period retries, and Set's own update is a try). A series ends only
with a good run (B12f1: B12 let the next failed run after a failed retry start a new series, so
while a source kept failing a retry was due every other run and a Set then waited for the
growing backoff): its later failed runs leave no retry due either; the first failed run after a
good one (or after a fresh process or an off) starts a new series. Off (`cancel`, `JobRuns.drop`) leaves no
retry due and no note, and a run started before it ends without effect (a generation `drop`
bumps, taken by each run at its start), so the next on schedules at once (`:core`'s `JobRuns`,
`JobRunsTest`). The switch's state is the pref `daily` (user, 2026-10-04; until 0.1.2 it was
`getPendingJob`, no pref): hibernation or a force stop deletes the job, so each `onResume`
schedules it again when `daily` is on and the job is missing or has another timing
(`DailyJob.ensure`; scheduling the same id again would stop a running job).

**Turns** (0.5.0, WP5; proposed 2026-10-16, approved by the user 2026-10-17; per screen since
0.6.0, M1/M2, 2026-10-20, the rules in `Screens.kt`). A column with two or more takes turns on
**one shared clock**, `turnStartedAt`, read once per run for both screens: a screen whose
`wallpaperStamp` isn't the clock's value is due its next source in order (APOD → NASA → Earth,
round to itself) with a stored picture, and takes the value. The clock restarts (written
first) when the turn is `interval` old (less 30 min: the job may run up to its flex early),
never for an early change (the owner unticked, or with nothing to show: no picture yet, an
APOD video with "Keep"), which moves the owner but not the stamp, so it ends with the turn.
When no ticked source has a picture the screen keeps its picture, and an owner no longer ticked
there gives way to the column's first (B14, CR 1: it stayed and its picture was set, after a Set
to NASA and Earth while both failed, on a screen where APOD wasn't ticked any more). A
newer post of the owner is set at once and keeps its turn. **The stand-in** fills APOD's turn
on an APOD video day on every screen where APOD is ticked, with turns too (NASA's, Earth's or
the user's own picture; with "Keep" a screen with APOD alone keeps its picture, one with turns
passes to the next source); the record keeps APOD as the owner. **Set** (S4, 2026-10-20;
`:core`'s `restartAndForget`, then `setDue`): the
clock restarts (to now); the records of the screens with a column are forgotten (picture
dropped, owner kept) and those on the current turn take the new stamp (one still due a retry
keeps its old stamp, so it moves on), so each screen gets what the setup says afresh, the same
picture again after a changed Fit or "my picture". Turning the daily switch on (S4f2,
`Store.startAfresh`) does the same with the stored columns, saving nothing else. A screen left
alone (an empty column) keeps its record. A screen whose column was empty starts without a record (its owner is removed):
it was left alone meanwhile, so its record is stale. A screen with no record (the lock screen
after migrating "Home", a fresh install, such a column) starts on the other screen's owner if
ticked there, else the first ticked source with a picture. **Equal columns** (the same
sources on both screens; B2, 2026-10-20 night): the lock screen takes home's owner and stamp,
in every run (`dueWallpapers`), so both are due one picture and get one set; home with no
record starts on the lock screen's owner. So they match again after a Set from columns that
differed and after a failed set on one screen (its retry takes home's source, not its own
next one). Columns that differ: each screen its own owner, one clock. A changed interval takes effect
with Set. The job: with turns (a column with two or more, `Store.hasTurns`), period `min(interval, 6 h)` with a 15 min flex (Android raises it to 5 % of the period:
18 min at 6 h), so turns come at even times; such a job doesn't run at once when scheduled, so
switching on offline waits up to a period. With one source 0.4.0's job (6 h, flex 6 h).
A `turnStartedAt` in the future (the clock set back) counts as a turn that is over
(WP7g; on the tablet, U7: the next source set at once, the turn fresh). `run -f` runs push the job's next window one period further each (seen: +21 h after 7 runs);
Set schedules it afresh while a column takes turns, else only when its timing changes (S4f1). A video-day choice or the mode switch
in the draft while an update runs makes the page run one more after it (that one read the
offered sources before).

Seen on the tablet (Android 15, 2026-10-17): `Num Required Flexible constraints: 0` for the
job, so Android doesn't hold it back for charging or idle; the same on the FP6 (Android 16, WP8).

**Keep running** (user, 2026-10-04; report:
[keep-running.md](.claude/skills/build-apodroid-android/keep-running.md)). Android pauses an app
that isn't opened, and the job doesn't count as use: the restricted bucket after about 8 days
(Android 13+; later or never before) and hibernation after about 3 months (12+), which deletes
the job. While the stored daily switch is on, a hint in a neutral box (U5) below the picture (user, 2026-10-16) names only what's missing, one line each:
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

**0.6.0: the picture time** (user, 2026-10-22, after the FP6 showed the job's 6 h rhythm;
packages J0–J3 in `plan-0.6.0.md`, before V7 and P). **Seen on the FP6** (0.5.0, battery
Unrestricted, standby bucket `EXEMPTED`, not held by Doze): the job ran every 6 h on the dot
from the install time (04:22, 10:22, 16:22), at each window's end. APOD came out at 06:05 local,
so the 04:22 run found yesterday's and the wallpaper changed only at 10:22, or at once when the
app was opened. **APOD's posting time** (`private/last100.json`, 24 Jun to 1 Oct 2026): 90 of
100 at 00:00 or 00:05 US Eastern (00:05 since 21 Jul), 10 hours late (07:14 to 16:46 Eastern;
eight of them 22–29 Jul, probably the site move). One moment worldwide, so "as soon as it's
out" is 21:05 in Los Angeles, 06:05 in Berlin, 15:05 in Sydney. Decided:
- **New picture at** [As soon as it's out | 00:00 … 23:00], default **06:00** (Advanced
  settings, a dropdown as "Largest original"). At that local time the newest post goes on; if
  it's already on (today's isn't out yet: Europe, 05:05–06:05 local), the next one goes on as
  soon as it's out; a late post is retried until it is. So a new picture every morning in
  every zone. A few random minutes after the time, so a zone's phones don't all ask at once
  (it replaces the job's spread from each device's own enable time, Traffic).
- **Turns start at** [00:00 … 23:00 | When I tap Set], default **06:00**, under "Change every":
  turns on fixed times (every 6 h: 06, 12, 18, 00; every 24 h a new source each morning).
  "When I tap Set" is 0.6.0's clock (Turns).
- **Opening the app** [The wallpaper waits | Sets the new picture at once], default **waits**
  (the user: as the turns, on fixed times): the page fetches and shows the new post, the
  wallpaper changes at its time. "Set as wallpaper" and a pull set at once, as now.
- **Android first** (J0): 0.5.0's tablet work saw a one-off job's delay alarm not wake a
  sleeping device; the FP6's periodic job runs on time only at its window's end, which wakes it.
  A night on the tablet on battery measures which timed run lands (JobScheduler with and without
  a deadline, `AlarmManager`'s inexact wake-up alarms; no exact-alarm permission without asking).
- Built in 0.6.0 (user: before P, the release waits for it); V7 runs on the new job.

**0.6.0: APOD first** (user, 2026-10-19; the logic and `Store` built in M1–M2, the settings in M3, the page in M4, by the night shift of 2026-10-20, approved by the user that morning; F1, F3, F4 and F5 by day the same day, with two fixes from their review (F1f1, F5f1); the early look on the tablet (C) on the user's go-ahead, then the tablet pass by night (V1–V6, and V8: V1, V2 and V4 again where S4–S6 changed them; user, 2026-10-20 night: a complete test on the final tree before the stable release; B2, B4–B6, S6, V3, V5, V6 and V8 passed the night of 2026-10-20/21, B7–B9 from its report), turns over hours (V7, a night of its own), the FP6 (P); the how, the layout and the order in `plan-0.6.0.md`).
Why: the app is APODroid, so the plain APOD app stays the default; 0.5.0 couldn't put different
pictures on home and lock, an unticked source couldn't be looked at, and "my picture" for
video days worked only with APOD alone (it shows at once that it was a video day).
- **A switch "All NASA pictures"** ("More NASA pictures" since S5), at the end of the settings, default off. Off: the app with
  APOD alone, as now. On: always three sources to browse (tabs until S6, then the title's
  dropdown and the swipe), and a list replaces the chips and "Where": a row
  per source, a tick box and a dropdown Home screen / Lock screen / Both (S2; M3 had a grid,
  a tick box for Home and for Lock). Each mode keeps its own settings; the list
  starts from "Where" the first time. From 0.5.0: anything but APOD alone → on (0.5.0 allows
  NASA or Earth alone). Rejected: a
  first-boot screen (an extra screen; updaters never see it), a "Lock screen: one source" row.
- **The columns** (the list's screens per screen): equal columns are one wallpaper (as today), an empty column leaves that screen
  alone, a column with two or more takes turns. One shared clock for both screens.
- **Video days:** the stand-in takes APOD's place on every screen where APOD is ticked, with
  turns too. Four choices in both modes, never greyed: "Skip it" (was "Keep"), NASA, Earth, my
  picture. A dropdown after "Size" in both modes (S2).
- **Set applies the setup** (user, 2026-10-20 evening, from a mockup; built in S4; S5 and S6
  follow: "Setup" and "Advanced settings", the highlight, "24 h", "More NASA pictures", the title
  dropdown and the swipe, no Share). Every setting, ⛶'s part, "my picture" and the mode switch
  are a draft until "Set as wallpaper", one button in both modes, which saves it all, restarts
  the clock, forgets the records and sets both screens afresh (The app, Settings; Turns). Why:
  a change took effect at once, "but maybe I'm not done"; one system for both modes. It
  replaced the afternoon's "Set as wallpaper changes the settings" (S1), which had replaced the
  hold of 2026-10-19: a simulation of the hold's rules over a week showed what the page never
  said (a held picture followed its source's newer posts, a set on Home stretched the lock
  screen's turn from 6 h to 9 h). The shown picture decides nothing (0.5.0 could set it): to
  put Earth on a screen, tick Earth. Kept after a review asked: the label "Set as wallpaper"
  (not "Apply"). The daily switch was in the draft too; after seeing S4 the user took it out
  (2026-10-20 evening; built in S4f2: it acts at once, on sets the stored setup afresh). S4's texts approved the same evening: "Set as
  wallpaper", ✓'s screen-reader label "Done", the toast "Setup saved. Nothing to set yet."
- Both screens due the same picture: one set with both flags; else each screen on its own
  (grouped by the due picture: with columns that differ the screens may show one picture or
  two). **Equal columns change together** (user, 2026-10-20 night; built in B2): with the same
  sources on both screens the lock screen follows home's owner and stamp in every run, so both
  show one picture, also while one screen's set is retried. Why: after a Set from columns
  that differed, each screen kept its own owner and they stayed opposite for good (home NASA
  and lock APOD, then the other way round). Every screen's due picture comes from
  one reading of the clock (reviews of the plan, 2026-10-19). The clock restarts only when the
  turn ends or on Set, not when one screen changes early, so the screens stay in step. Each
  screen's record notes its turn, so a failed set on one screen is retried without moving the
  other on again; Set stamps only the screens on the current turn. Forgetting a record (Set)
  keeps whose turn it is and the stamp, so the current source's picture goes on again; a
  screen with no record starts on the other screen's source if ticked there, else the first
  ticked one with a picture.
- Fill / Fit (0.1.1's review) is one setting for both screens. F1–F5's decisions: below, From a
  review of 0.1.1.
- The mode switch is part of the setup: Set applies it and reschedules the job.
- The page fetches the sources it offers (with the switch on in the draft all three); the job only what the screens need (the
  ticked ones, the stand-in while APOD is ticked somewhere). APOD mode
  with "Where" = Lock reads the lock screen's record. The chip
  and the opening page follow the picture's source (a stand-in's page), not whose turn it is.
- Home and lock set separately: checked on the tablet only first (M0); the FP6 (Android 16)
  sees it in the final pass (user).
- Migration (built, M2, 2026-10-20; Stack, Migrations): a one-time write at 0.6.0's first
  start, not a read fallback (home's record is the unprefixed keys, so a read can't make it
  unknown). 0.5.0's `sources` and `where` become the mode and the columns; the wallpaper record
  is copied to the lock screen with Both and Lock, and home's is cleared with Lock; with Home
  the lock screen is unknown (the app never set it). Under a lock (the page and the job may
  both start first); 0.5.0's own picture (APOD's, no URL, on a video date) becomes "own"; in
  APOD mode the owner is always APOD (0.5.0 noted a stand-in as the wallpaper's source). A
  fresh install runs it too and only writes the mode.

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
  start at fixed clock times (00:00, 06:00, …), no grid; planned since 2026-10-22 (the picture
  time, 0.6.0: "Turns start at"). Learned on the tablet
  (Lenovo, Android 15): a one-off job without a network constraint ran 45 min late with the
  screen on (JobScheduler batched it until another job ran), and its delay alarm doesn't wake a
  sleeping device.
- **Tabs above the picture:** a segmented row, one tab per source in use, opening on the one on
  the wallpaper; each with its own picture, ⛶, ★ and explanation. Replaced in 0.6.0 by the
  title's dropdown and the swipe (S6; The app).
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
  picture on black; no ⛶ with Fit (user, 2026-10-05). One setting for both screens
  (2026-10-19). Part of the setup since S4: Set puts the pictures on again with the new size
  (2026-10-20 evening; was "sets at once", 2026-10-20, and "the next change uses it",
  2026-10-04).
- **Wi-Fi only:** built (F2) and removed before release (B10; user, 2026-10-21: whoever wants
  the picture wants it at once, not after days of waiting for Wi-Fi).
- **Share:** title and page link via the share sheet (built in F3, the glyph ⤴ beside ★);
  removed in S6 with the mockup of Set applies the setup (user, 2026-10-20 evening).
- **Image credit** under the date, parsed in `:core` (on all 100 posts checked). APOD only
  (user, 2026-10-20): NASA's feed names a credit in 1 of 60 items (the rest only on each image
  page), Earth's is in `content` only (each check 0.6 KB → about 61 KB) and mostly "NASA Earth
  Observatory".
- **Save every picture** (built, F5; Save): a switch for the job, default off, with a small ⓘ next to it: saved
  pictures are full resolution, and saving every day takes a lot of space (user, 2026-10-04).
  **Every source in use, not APOD only** (user, 2026-10-20: APOD only would favour it): after
  each run the job saves each in-use source's stored post (the same set it fetches: the ticked
  ones and the stand-in on an APOD video day) unless it's a video or
  already saved; chosen over "what goes on a screen" as easier to keep right (no hook in every
  set path, no state for a set but unsaved picture). As ★ does: the original, else the stored
  picture (bounded data: a 97 MB NASA original is tried once, after a stop too from F5f1). The
  job remembers what it saved (date and URL), not whether the file is still there, so a picture
  deleted in the gallery isn't saved again. **Largest original** (F6; Save): the job doesn't
  download an original over the limit (default 20 MB) and saves the stored picture instead.
- **Left out:** previous days (not one page any more), full-screen view, home-screen widget,
  Quick Settings tile (rarely used), translations (English UI stays), sharing the picture file
  (needs a `FileProvider`). Still out, as above: a notification, the still frame on video days,
  help texts, choosing the check time, `api.nasa.gov` as a fallback. NASA History Image of the
  Day (on Flickr; user, 2026-10-16).

## Open questions

1. Huge pictures (2 Oct 2026, 37.7 MB PNG) cost mobile data, and on a slow connection the
   download may not finish within the roughly 10 minutes Android gives a job (each retry starts
   over). Left open (user, 2026-10-04): it happened once. The rendition (0.5.0) covers part of it. Seen on 2026-10-07 (`cmd jobscheduler timeout`): a stopped job's
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
