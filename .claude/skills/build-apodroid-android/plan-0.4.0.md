# Plan for 0.4.0 (video days, pull to refresh, problems shown)

Planned 2026-10-16. Release target: 0.4.0 (versionCode 8), the same day (user). Work through
the packages in order, one at a time: build, show it on the phone, wait for the user. Tick
them off and note where the work differed from the plan. Each package writes its decisions
into `CLAUDE.md` (the source of truth) when it lands; this file is the how.

## What happened on 16 Oct 2026

The first real video day since the release (an mp4, "Chasing the International Space
Station"). Its post has **no `featured_image` at all** (key missing, `featured_media: 0`);
`featured_image_url` is NASA's generic fallback picture. In the 200 posts from 31 Mar to 16 Oct
(21 video days) it's the only one without it. Saved as `private/video-day-2026-10-16.json`.

`parseLatest` threw `"No image"`, so every refresh failed: the page silently kept 15 Oct's
picture under 15 Oct's title, and the job failed and retried all day. The wallpaper stayed,
which is right for "keep" by accident; "use my picture" would never have been set.

## Decisions (user, 2026-10-16)

- **No still frame on video days.** Video days download no picture; the page shows a dark tile
  with ▶. (Before: the still frame, often a generic NASA image, and on 16 Oct none at all.)
- **▶ opens the APOD page in the browser** (as the picture does today). No video player, no
  video URL parsing.
- **Pull down to refresh.** It always asks the site, also when the stored APOD is today's.
  When it finds a new picture and the daily switch is on, it sets the wallpaper at once.
- **Show problems** so the app doesn't look like it's working when it isn't (the site changed,
  no connection, …).
- **0.4.0 today** instead of a 0.3.2 hotfix. A hotfix isn't faster on F-Droid: its bot finds
  new tags within a day or two and builds only the newest one, so a 0.3.2 tagged a few hours
  before 0.4.0 would be skipped. The bug fixes itself with the next image day and only comes
  back on a post without `featured_image`.
- **The "Set as wallpaper now" rule in WP3.** Putting APOD back after another wallpaper: switch
  off and on again.
- **"Site changed" is covered by unit tests only**, no debug hook.

Release scope: the packages below. Of `features.md`, the status line (3) and the video marker
(9) are part of it; the rest (1, 2, 4, 6, 7, 8) move to 0.5.0 (user).

## Packages

| WP | What | Model, effort |
|----|------|---------------|
| 1 | Video days work (`:core`, `Store`, job, page) | Opus, medium |
| 2 | One update path, pull to refresh, auto-set | Opus, medium |
| 3 | "Set as wallpaper now" only when it's needed | Sonnet, medium |
| 4 | Problems and status line | Opus, high |
| 4b | Video-day notice | Sonnet, medium |
| 4c | Problems in the same box | Sonnet, medium |
| 4d | All boxes at once, battery hint in a box | Sonnet, medium |
| 5 | Release preparation (migration, version, texts) | Sonnet, medium |
| 6 | Verification pass on the phone | Opus, high |
| 7 | Commit and tag, when the user says so | Sonnet, low |

Each package runs in a fresh session started from the repo folder with its model and effort.
**When a package is done (tested on the device and reported), end the report with the command
for the next one**, copied from here:

```
claude --model opus --effort medium "Do WP2 in .claude/skills/build-apodroid-android/plan-0.4.0.md"
claude --model sonnet --effort medium "Do WP3 in .claude/skills/build-apodroid-android/plan-0.4.0.md"
claude --model opus --effort high "Do WP4 in .claude/skills/build-apodroid-android/plan-0.4.0.md"
claude --model sonnet --effort medium "Do WP4b in .claude/skills/build-apodroid-android/plan-0.4.0.md"
claude --model sonnet --effort medium "Do WP4c in .claude/skills/build-apodroid-android/plan-0.4.0.md"
claude --model sonnet --effort medium "Do WP4d in .claude/skills/build-apodroid-android/plan-0.4.0.md"
claude --model sonnet --effort medium "Do WP5 in .claude/skills/build-apodroid-android/plan-0.4.0.md"
claude --model opus --effort high "Do WP6 in .claude/skills/build-apodroid-android/plan-0.4.0.md"
claude --model sonnet --effort low "Do WP7 in .claude/skills/build-apodroid-android/plan-0.4.0.md"
```

Test devices (2026-10-16): the LineageOS phone (Android 16, 1116×2484) and a Lenovo tablet
TB305FU (`adb -s HA24BZ2Y`, Android 15, 800×1340 at 180 dpi; 0.3.1 from Obtainium, release
key). The Samsung SM-G955F that is sometimes attached runs Android 9, below minSdk: don't use.

Today's post is a live video day until about 04:05 UTC (06:05 in Central Europe) on 17 Oct:
do WP1 first and test it on the phone against the real post while it lasts.

### [x] WP1. Video days work (Opus, medium)

Done 2026-10-16, as planned. Tested: `:core` tests (new: video post without and with
`featured_image`, image post without it); the parser on the 200 posts 31 Mar–16 Oct (21 video
days, all without a picture); on the tablet (debug build, fresh install) with the real 16 Oct
post: the tile with ▶, "· Video", ▾ explanation, ▶ opens the browser, no `apod.jpg` and no
`imageUrl` stored; the job with "keep" (`video, wallpaper kept`, wallpaper id unchanged) and
with "my picture" (`wallpaper set`, id 41 → 42, `wallpaperDate` set; a second forced run does
nothing). Not tested: the LineageOS phone, the R8 build, an update over a 0.3.1 video-day
entry, a YouTube day (all in WP6). The tablet was left with the switch on and "Use my picture"
(15 Oct's APOD as `fallback.jpg`).

- `:core`: `Apod.imageUrl` becomes `String?`. `parseLatest` only throws for a missing image on
  image days; on video days it's ignored (always null; no still frame). Tests: a made-up post
  shaped like 16 Oct (video hero, no `featured_image`), a video day with `featured_image`
  (image URL ignored), an image day without it (still an error).
- `Store.refresh()`: a video day stores the entry without downloading and deletes `apod.jpg`
  (it would be yesterday's picture under today's entry). `isCurrent`: today's date and (video
  day or `apod.jpg` exists). The "older post" guard and the download condition must not loop on
  video days (no file is expected). Stored `imageUrl` empty → null.
- Check that `wallpaperFile()` (video → my picture or null), `setWallpaper`, `save` (no ★ on
  video days), `cropPosition` and the job (`wallpaperDate` stays unset with "keep", so each run
  checks; cheap) need no change, and `Store.apod` reads an entry stored by 0.3.1 (with a still
  frame and `isVideo` true): it just shows the tile.
- Page: when the stored APOD is a video, a dark tile (the 3:2 box the empty state uses) with a
  centred ▶ instead of the picture; tap opens `pageUrl`. No ⛶, no ★. Screen-reader label "Play
  video on the web". The title row and explanation as on image days. `show()` and the
  `picture == null` branch must tell "video" apart from "nothing stored yet" (today a video
  entry would show the empty box).
- Phone: today's real post, both video-day settings, the job forced (`run -f`), the log line
  `video, wallpaper kept`.

### [x] WP2. One update path, pull to refresh, auto-set (Opus, medium)

Done 2026-10-16. Differed from the plan: `refresh()` needed no `force` (it already fetches
whenever called and stores a changed title; the skip lives in `update`). `update` returns the
log text (`<date>, wallpaper set` / `video, wallpaper kept`, null for nothing to set); the job
logs it as `Daily job: …`, the page as `Page: …`. `setWallpaper` takes the new lock too, so the
button can't overlap a set. The pull indicator only spins for a pull, not for `onResume`'s
update. `PullToRefreshBox` needed no opt-in. Tested on the tablet (debug build, today's real
video post, "Use my picture"): pull on a current day with a faked title (spinner, real title
back, nothing set); a faked old day with the switch on (page offline shows 15 Oct, pull: 16
Oct, `Page: 2026-10-16, wallpaper set`, wallpaper id 42 → 43, `wallpaperDate` 16 Oct); the same
with the switch off (page open fetches 16 Oct, `wallpaperDate` stays, id unchanged); switch on
(job sets, id 44); a current day with only `wallpaperDate` faked: opening the page sets it, no
network needed; page open and `run -f` at the same moment, twice: one set each (id +1, job ran
in the same second per `dumpsys jobscheduler`, logged nothing). No `AndroidRuntime` lines. Not
tested: an image day (download only for a new date, crop mode kept during a pull; the next image
post is after 04:05 UTC on 17 Oct), a pull offline, the LineageOS phone, the R8 build (WP6).

- Move the job's `update()` into `Store.update(force: Boolean)`: refresh unless current (or
  always with `force`), then, **when `daily` is on** and the stored date isn't `wallpaperDate`,
  set `wallpaperFile()`. The job and the page both call it, so opening the page on a new day
  also sets the wallpaper (today it only downloads it; the job sets it hours later). Only one
  set at a time: the check of `wallpaperDate` and the set under one lock (the page and the job
  can run at once; see the 0.2.0 race in `declutter.md`). Not the refresh lock: holding it
  during a set would make ★ wait for seconds.
- `refresh(force)`: with `force` it fetches the JSON even when current; it downloads only for a
  new date or a missing file (unchanged). A changed title or explanation for the same date is
  stored.
- Page: `PullToRefreshBox` (material3 1.4.0, already a dependency; may need
  `@OptIn(ExperimentalMaterial3Api::class)`) around the scrolling column. Pull →
  `update(force = true)`; the indicator spins while it runs; `load()` keeps its guard against
  two at once. Crop mode is left as is during a refresh unless the date changes (`show()`
  closes it then, as now).
- With the switch off nothing is set (the user sets it).
- Phone: pull on a current day (fetch seen in logcat, no download), a faked old day with the
  switch on (new picture and wallpaper set, `wallpaperDate` updated), the same with the switch
  off (picture only), pull while the job runs (`run -f` at the same time: one set).

### [x] WP3. "Set as wallpaper now" only when it's needed (Sonnet, medium)

Done 2026-10-16, as planned. Feedback is the existing toasts (the user decides on the phone).
`applyNow()` in `MainActivity` (not an image day for the video-day settings, where nothing
changes); `Store.forgetWallpaper()` clears `wallpaperDate`, then the switch runs `load()`. Tested
on the tablet (debug build, real 16 Oct video post, "Use my picture"): button gone with the
switch on, shown with it off; "Wallpaper on" changed with the switch on set the wallpaper (id
48 → 49, home only); switch off and on set it again (49 → 50); no crash. Not tested: picking or
changing "my picture", the "Use my picture" choice, an image day, the LineageOS phone.

Decided (user, 2026-10-16). Today the button is the only way to:
1. set a picture with the daily switch off;
2. apply a changed "Wallpaper on" (it waits for the next date);
3. apply a newly picked or changed "my picture", or keep → my picture, on a video day;
4. put APOD back after the user or another app changed the wallpaper.
With the switch on, WP2 covers new pictures and ✓ covers the crop.

Rule: **while the switch is on, the app keeps the wallpaper current and the button is gone;
while it's off, the button shows.**
- Switch on: changing "Wallpaper on", picking or changing "my picture" or choosing "Use my
  picture" (only on a video day; else nothing to do) sets the wallpaper at once. Choosing
  "Keep" on a video day sets nothing (the previous picture is already gone).
- Turning the switch on sets today's picture at once, also when it was set before (clear
  `wallpaperDate`; the scheduled job runs at once anyway). That covers case 4: switch off and
  on, or use the button while it's off.
- ✓ in crop mode still sets the wallpaper in both states.
- Feedback: the existing toasts ("Wallpaper set", "Couldn't set the wallpaper") for the
  settings too, or the status line (WP4); decide with the user on the phone.
- `features.md` item 2 (Fill/Fit "doesn't set the wallpaper again") then follows the same rule.
- Rejected: showing the button only when the wallpaper differs from what the app would set
  (`WallpaperManager.getWallpaperId` tracking): more state, and other apps' changes are hard to
  tell apart.

### [x] WP4. Problems and status line (Opus, high)

Done 2026-10-16. Differed from the plan: a check that finds today's APOD stored counts as a
success (as `features.md` item 3 had it), so the job keeps `checkedAt` fresh every 6 h and one
failed run can't show "since"; the flip side is that opening the page on such a day clears a
failed pull's line. "Not an image" after a download is `SiteChanged`. Failures are `IOException`
subclasses, so nothing else changed. Texts as planned, "since" worked into each sentence
(`problemText` in `MainActivity`); the problem line in the error colour. Tested: `:core` tests
(each kind; a refused connection and a short download are neither typed kind); on the tablet
(debug build, real 16 Oct video post): status line after switching off and on ("Wallpaper
changed 16 Oct, 17:25"), "Keep" → "Video today, wallpaper kept"; offline (firewall chain) pull
→ "Couldn't reach NASA's site. Check the connection."; stored date and `checkedAt` faked back →
"… since 14 Oct …"; the empty state with the same text above "Try again"; the job offline
(`run -f`) stores `problem`; online again, a pull clears it. No `AndroidRuntime` lines. Not
tested: an HTTP error and "site changed" on the device (unit tests only; the CONNECT proxy
can't inject a status), a throttled or cut picture download (a video day, nothing to download;
WP6), the LineageOS phone, the R8 build. The tablet is left as before (switch on, "Use my
picture", Lock screen).

Merges `features.md` item 3 (status line), with the cause added.
- `:core`: typed failures, so the app can name them: `SiteChanged` (JSON not as expected, no
  post, bad date, an image day without a picture), `HttpStatus(code)` (any non-200), the rest
  is a network failure (`UnknownHostException`, `ConnectException`, timeouts, a short
  download). Tests for each.
- `Store`: on each check (page or job) store `checkedAt` (last success), `problem` (kind and
  HTTP code, or none) and `wallpaperSetAt` (last successful set). A success clears `problem`.
- Page, one line under the date, only while `problem` is set (placeholder texts; show the
  user):
  - network: "Couldn't reach NASA's site. Check the connection."
  - HTTP: "NASA's site answered with an error (503). The app tries again later."
  - site changed: "NASA's site has changed. An app update may be needed."
  - with `checkedAt` more than a day old, "since 14 Oct" is added.
  Pull to refresh with a problem: the line updates, nothing else.
- The empty state (nothing stored) shows the same text above "Try again".
- Status line under the daily switch (always while it's on): "Wallpaper changed 16 Oct,
  06:12"; on a video day with "keep": "Video today, wallpaper kept". Before the first change:
  nothing. The "couldn't check since" part of item 3 is the problem line above (one place).
- The page re-reads both on `onResume`, after its own update and when the job stored something.
- Picture saving and setting keep their toasts; they're the user's own action.
- Phone: offline (firewall chain, skill, Store listing), the picture host throttled or cut
  (the proxy), `checkedAt` faked two days back. "Site changed" can't be faked on the phone
  without a debug hook: unit tests only (user).

### [x] WP4b. Video-day notice (Sonnet, medium)

Done 2026-10-16, as planned (`VideoNotice` in `MainActivity`, prefs in `Store`, drafts kept as
the texts). Tested on the tablet (debug build, real 16 Oct post, switch on, "Use my picture" with
a picture): the box under the tile in orange; Dismiss hid it and stored `videoNoticeDate`; after
deleting the pref and a restart it was back. Not tested: the other three texts, "Don't show
again", a new video date after Dismiss, image days, font scale 2.0, TalkBack labels, the
LineageOS phone.

Added 2026-10-16 (user), after WP4. On video days the page says why the wallpaper didn't
change and what the user can do. WP4's status line "Video today, wallpaper kept" under the
switch stays (user).
- **Where and how:** a box directly below the ▶ tile, above the title row (user). Translucent
  orange fill with a solid orange border (user), small rounded corners, normal text colour.
  Only while the stored APOD is a video. WP4's problem line stays where it is.
- **Texts** (drafts, show the user on the device), by state:
  - switch on, "Keep": "Today's APOD is a video, so your wallpaper stays as it was. Tap ▶ to
    watch it on NASA's site. To use your own picture on video days, choose "Use my picture"
    below."
  - switch on, "Use my picture" with a picture: "Today's APOD is a video, so your own picture
    is the wallpaper. Tap ▶ to watch it on NASA's site."
  - switch on, "Use my picture" but none picked (that acts as keep): the first two sentences of
    "Keep", then "Pick a picture below to use it on video days."
  - switch off: "Today's APOD is a video. Tap ▶ to watch it on NASA's site."
  The text follows the settings at once (it's read from the same state the page already has).
- **Two buttons** (user): "Dismiss" hides it for that date only (pref `videoNoticeDate`; back
  on the next video day); "Don't show again" hides it for good (pref `videoNoticeHidden`, like
  `pauseHintHidden`; only clearing storage brings it back). No confirmation dialog (it's only an
  explanation; ask the user on the device if one is wanted).
- `CLAUDE.md`: the notice in "Video days on the page", with the user's decisions; it's the
  second help text besides the pause hint (the "No help texts" bullet).
- Device: the real 16 Oct post while it lasts (until about 04:05 UTC on 17 Oct), else faked
  (`isVideo` true, skill). Each of the four states; Dismiss (gone, back after faking another
  video date); Don't show again (gone for good, also on a new video date); image days show no
  box; font scale 2.0 and the tablet's narrow width; TalkBack labels in `uiautomator dump`.
  Undo for testing (after `am kill`): delete the two prefs with `sed -i -e '/videoNotice/d'`.

### [x] WP4c. Problems in the same box (Sonnet, medium)

Done 2026-10-16, as planned (`NoticeBox` shared with `VideoNotice`; also on image days, below the
picture). Tested on the tablet (debug build, real 16 Oct video post): airplane mode + pull → the
problem box replaces the video notice; Dismiss stored `problemNoticeKey` `network:2026-10-16` and
the video notice showed instead; no `AndroidRuntime` from the app. Not tested: a new day showing
it again, online pull clearing it, "since", the empty state, font scale 2.0, HTTP/site kinds.
After Dismiss the video notice shows while the problem lasts (one box at a time, the problem's
turn is over).

Added 2026-10-16 (user), after WP4b was done. WP4's problems get WP4b's orange box.
- **Same box** (user, 2026-10-16): WP4's problem line (network, HTTP, site
  changed, with "since …") moves from under the date into the box, same place and look, its
  text unchanged. Only "Dismiss" there, no "Don't show again" (a failing app must not look like
  a working one): it hides that problem until its kind or HTTP code changes or a new day
  starts (pref `problemNoticeKey`, e.g. `http:503:2026-10-16`). It still goes away by itself
  on a successful check. The empty state (nothing stored) keeps WP4's text above "Try again",
  no box.
- **One box at a time** (user): a problem wins over the video notice; the video notice comes
  back once a check works.
- `CLAUDE.md`: where problems show (the page bullets and Background update / status line).
- Device (debug build): offline via the firewall chain (skill, Store listing) on the video day:
  the problem box replaces the video notice; Dismiss hides it; still offline, a new day faked
  shows it again; online, a pull clears it and the video notice is back (unless dismissed for
  that date); the "since" text with `checkedAt` faked two days back; the empty state still the
  plain text above "Try again"; font scale 2.0. Undo for testing (after `am kill`):
  `sed -i -e '/problemNotice/d'`.

### [x] WP4d. All boxes at once, battery hint in a box (Sonnet, medium)

Done 2026-10-16, as planned (`NoticeBox` takes a buttons lambda and a content slot; `BatteryNotice`
checks the conditions and is used on the page and in the empty state). Tested on the tablet (debug
build, real 16 Oct video post, switch on): video notice and battery hint stacked in that order;
"App info" opens the settings. Not tested: all three boxes with a problem, the dialog, the hint
going after fixing the battery setting, an image day, the empty state, font scale 2.0, the
LineageOS phone.

Added 2026-10-16 (user), after WP4c was done. Why: as transparent as possible, every choice in
the user's hands ("if they don't want to see the battery info any more, it's their issue").
- **All boxes at once** (user, 2026-10-16; replaces WP4c's "one box at a time"): every box that
  applies shows, stacked below the picture or ▶ tile and above the title, in this order: problem,
  video notice, battery hint. Same `NoticeBox` look for all. Each keeps its own buttons and
  hiding rules: problem "Dismiss" (`problemNoticeKey`), video notice "Dismiss" and "Don't show
  again" (`videoNoticeDate`, `videoNoticeHidden`), battery hint below.
- **Battery hint in a box** (user): `PauseHint` moves from under the switch into a `NoticeBox`.
  Text, conditions (switch on, something missing, `pauseHintHidden` not set) and buttons
  unchanged: "App info" and "Don't show again", which still asks first in the dialog (the
  user didn't ask to drop it). No "Dismiss" (user). `NoticeBox` takes a `String` now; the
  hint has a lead line and one line per missing setting: pass them joined with line breaks, or
  give `NoticeBox` a content slot, whichever is simpler. With nothing stored yet (the empty
  state), the hint box still shows, below "Try again".
- `CLAUDE.md`: the page bullets (one box at a time → all boxes, the order), Keep running ("a hint
  under it" → in the box), the help-texts line. The skill's "Where things are" (`PauseHint`
  under the switch).
- Device (debug build, tablet): switch on, battery not Unrestricted, the real or a faked video
  day, offline (firewall chain): three boxes in order; each button works (App info opens it,
  the dialog's Cancel/Hide); fix the battery setting → that box goes on the next `onResume`;
  image day: only the boxes that apply; empty state; font scale 2.0 (buttons wrap?). Undo for
  testing (after `am kill`): `sed -i -e '/pauseHintHidden/d' -e '/videoNotice/d' -e '/problemNotice/d'`.

### [x] WP5. Release preparation (Sonnet, medium)

Done 2026-10-16. Migration removed, `versionCode 8` / `0.4.0`, `changelogs/8.txt` (383
characters, draft: show the user), README, `full_description.txt`, `CLAUDE.md` (Migrations,
button rule, Releases, Next features), `features.md` (3 and 9 ticked). Tested: `:core:test`
green; the unsigned release APK built twice (with `clean`), same sha256 `7167116e…`. Not done:
`phoneScreenshots/1.png` (needs the phone; the page changed visibly with the boxes, so the user
decides), WP4d is still open and its texts in `CLAUDE.md`/README describe it as planned (the
battery hint "below the picture"); nothing installed on a device.

- Remove the `Store.daily` migration (first read from `getPendingJob`), due in 0.4.0
  (`CLAUDE.md`, Migrations).
- `versionCode 8`, `versionName "0.4.0"`; `changelogs/8.txt` (max 500 characters; show the
  user).
- `CLAUDE.md`: video days (no still frame, the 16 Oct post), pull to refresh, the button rule,
  problems and status line, Releases; remove the done items from Next features. Skill:
  `features.md` (3 and 9 done here, next version for the rest), "Still untested".
- `README.md` and `full_description.txt`: video days, pull to refresh, the button.
- `phoneScreenshots/1.png` if the page changed visibly (skill, Store listing).
- The unsigned release APK builds reproducibly (two builds, same sha256).

### [x] WP6. Verification pass on the phone (Opus, high)

Done 2026-10-16 on the tablet only (the LineageOS phone wasn't attached): the R8 build of the
release candidate (unsigned sha256 `7167116e…`, signed with the debug key), the debug build for
faking. No code changed. Tested:
- `:core:test` green (20 tests); the parser on the 200 newest posts (fresh download) and on
  `last100.json`: no error, 21 video days, every image day with a picture and every post with
  an explanation. One title keeps its prefix: 3 Apr 2026 "APOD: 2026 April – Caught in the Web"
  (no day number; `titlePrefix` needs one). Older than 0.1.0's data, not a 0.4.0 change.
- Update from 0.3.1 (debug, fresh install; it showed "Couldn't load" on the real 16 Oct post):
  switch on, then a faked 0.3.1 video entry (13 Sep, still frame as `apod.jpg`), offline,
  R8 installed over it: the ▶ tile with "· Video", switch still on, the job still scheduled
  (migration removal), all three boxes in order. Online pull: 16 Oct stored, `apod.jpg` and
  `imageUrl` gone, `video, wallpaper kept` from page and job, wallpaper ids unchanged.
- Video days (R8): ▶ opens the post in the browser (mp4; YouTube 23 Aug faked offline on the
  debug build); all four notice texts; "Use my picture" with none picked sets nothing; picking
  sets at once (id +1, both screens); "Lock screen" with the switch on sets at once; "Keep" sets
  nothing; "Use my picture" again sets at once; switch off: button back, settings set nothing,
  the button sets; switch on sets again, one set, job scheduled. Debug build: switch off on a
  new day fetches without setting (`wallpaperDate` stays), then switch on with "Keep": `video,
  wallpaper kept` from page and job, status "Video today, wallpaper kept".
- Image days (R8, faked as today's): page open sets it (crop hint centred), ⛶ drag left and ✓
  (`mCropHint` 0–764), ★ with a quick double tap and a third tap: one file, "Already saved",
  ▾ explanation, long press copies the link and the explanation (system confirmation), tap
  hides. The 37.7 MB PNG (2 Oct): shown, set in about 3 s, ★ saved it as `.png` (37.7 MB).
- Pull (R8): current day (spinner, real entry stored, nothing set); offline with `checkedAt`
  two days back: "Couldn't reach NASA's site since 14 Oct." box, a pull keeps it, Dismiss hides
  it; online pull and `run -f` at the same moment: one set (job's `Daily job: 2026-10-16,
  wallpaper set`), box gone.
- Debug build: video notice back on a new video date after Dismiss; "Don't show again" holds
  across one; the empty state offline (problem text above "Try again", battery box below), online
  "Try again" loads; battery dialog Cancel keeps the hint, Hide removes it; font scale 2.0
  (boxes and buttons fit); labels in `uiautomator dump` ("Play video on the web", …).
- No crash or ANR: `dumpsys activity exit-info` lists only kills and updates (logcat was
  cleared during the pass).

Seen, left for the user: offline with an older video entry stored, the notice says "Today's APOD
is a video" and the status line "Video today, wallpaper kept" for a past post (the problem box
shows above it).

Not tested: the LineageOS phone; anything with a picture download, as today's post is a video
(throttled or cut download, `cmd jobscheduler timeout` mid-download, crop mode kept during a
same-date pull): the next image post is after 04:05 UTC on 17 Oct; an HTTP error and "site
changed" on a device (unit tests only, as planned); a reboot. The tablet is left with the R8
build (debug key), switch on, "Use my picture" (15 Oct's picture), Both, font scale 1.1 as
before.

On the R8 build of the release candidate (skill: signing it with the debug key installs it
over the debug build), the debug build where `run-as` is needed for faking. Report each item
as tested, untested, or failed.
- `:core:test` green; the parser on all 200 recent posts (`private/last100.json` and a fresh
  download) without an error, the 21 video days found.
- Video days: the real 16 Oct post if still live, else faked (`isVideo` true, `apod.jpg`
  gone); mp4 and YouTube both just open the page; "keep" and "my picture" through the job
  and the page; an entry stored by 0.3.1 on a video day (install 0.3.1, fake it, update).
- Image days unchanged: picture, ⛶ drag and ✓, ★ save and re-save guard, explanation and
  long presses, a large picture.
- Pull to refresh: current day, new day with the switch on and off, offline, during a job run.
- The button rule (WP3): shown only while off; each setting applies at once while on; switch
  on sets now.
- Problems: offline, throttled picture download, a job timeout mid-download
  (`cmd jobscheduler timeout`), the "since" text after two faked days; all clear after a
  success.
- The job: forced run, a new day faked, `dumpsys jobscheduler` shows it scheduled; the
  migration removed (switch state kept over an update from 0.3.1).
- Crashes: `adb logcat -d | grep AndroidRuntime` empty after the pass.
- Update the skill's "Still untested".

**Final check (2026-10-16, after WP6).** Fixed WP6's two findings:
- `titlePrefix` takes the day as optional (3 Apr 2026 → "Caught in the Web"; a test; all 200
  newest posts cleaned).
- The video texts no longer say "today", as the stored post may be an older one while offline:
  "This APOD is a video …", status line and toast "Video day, wallpaper kept".

Also removed the stale migration sentence from `CLAUDE.md`, Background update. Tested:
`:core:test` green; two clean unsigned release builds identical (sha256 `f1747d35…`; the
earlier `7167116e…` was before these fixes); on the tablet, debug and then R8 (debug key): the
new texts in the box and the status line, an online pull, no `AndroidRuntime` lines. Not
tested: the offline older-video-day case itself (only the wording changed).

**On the FP6 (LineageOS, Android 16), 2026-10-16.** The R8 build signed with the release key
(by the user) installed with `adb install -r` over 0.3.1 from Obtainium, which was stuck on 15
Oct: settings and the job kept, the page fetched the real 16 Oct video post and set "my picture"
(`Page: 2026-10-16, wallpaper set`, wallpaper id 185 → 186), the video notice and "Wallpaper
changed 16 Oct, 18:37"; a pull (nothing to set, nothing logged); ▶ opened the browser. Then the
debug build (uninstall, the user's settings lost, agreed): the new `phoneScreenshots/1.png` (1
Oct, demo mode, app offline, `problemNoticeKey` and `pauseHintHidden` set so no box shows);
offline the problem box and the battery hint on an image day, and all three boxes on a faked
video day. Then the release-signed 0.4.0 again, fresh: it fetched today's post. No
`E AndroidRuntime` lines. The user sets the phone up again (switch, my picture, battery).

### [x] WP7. Commit and tag (Sonnet, low)

Only when the user says so: commit, tag `v0.4.0`. The user pushes `master`, then the tag
(skill, Releasing).
