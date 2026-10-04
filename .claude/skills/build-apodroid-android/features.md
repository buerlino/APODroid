# Next features

Planned 2026-10-04 from a review of 0.1.1 for features users would expect
(`private/APODroid missing features users might expect.md`). The user chose the list below;
the decisions are in `CLAUDE.md` ([Next features](../../../CLAUDE.md#next-features)), this file
has the how. Work through it step by step, as always: one item, show it on the phone, wait.
Tick items off and note where the work differed from the plan.

Suggested order: the `:core` items first (testable without the phone), then the job and its
settings, then the small UI items. Next version: 0.2.0 (new features; confirm with the user when
releasing).

## Settings layout afterwards

```
[switch] Change wallpaper daily
         Wallpaper changed 4 Oct, 06:12        ← status line (3)
         Android pauses apps … [App info] [Don't show again]  ← only when not exempt (5)
         [switch] Wi-Fi only                   ← (4)
         [switch] Save every picture ⓘ         ← (8)
Wallpaper on   [Home | Lock | Both]
Fit            [Fill | Fit]                    ← (2)
Video days     ( ) keep  ( ) my picture
[Set as wallpaper now]
```

The three lines under the switch show only while it's on: they only affect the job. Labels are
placeholders; show the user before settling them. The defaults here (both switches off, settings
shown only while the daily switch is on, Fill/Fit not re-setting the wallpaper, hibernation
checked before any hint, version 0.2.0) were proposed and confirmed by the user, 2026-10-04.

## Items

### [ ] 1. Sharper wallpaper (rendition), original for the gallery

Today the app downloads `featured_image.file` as is: 1280 px, which Android scales up 2–3× to
fill a 1116×2484 screen.

Checked 2026-10-04 on 20 of the 100 posts in `private/last100.json` (every 5th):
- **Original:** replace `/dynamicimage/assets/` with `/content/dam/` in `featured_image.file`.
  HTTP 200 on 20 of 20, but large: usually 2–10 MB (M83 10.1 MB, Fire Rainbow 10.2 MB,
  5815 px wide), from 54 KB to 10 MB.
- **Rendition:** `featured_image.file?h=2500` → 3751×2500 (0.5 MB), 2522×2500 (0.5 MB),
  3333×2500 (0.9 MB), 3197×2500 (1.2 MB). `?w=2500` is smaller for landscape (0.3–0.9 MB) but
  too short to fill a portrait screen. The server upscales past the original (1 Oct's 1600 px
  original came back 3751 px), no worse than Android scaling it. A PNG stays PNG (2 Oct:
  `?w=1600` → 5.3 MB).

Decided (user, 2026-10-04): **the daily download is a rendition, ★ saves the original.**
- `:core`: from `featured_image.file`, build the rendition URL (`?h=<px>`, the height passed in)
  and the original URL (`content/dam`). Unit tests for both, including a file name with `%20`
  and one that already has a query string (strip it first).
- `Store`: `<px>` = the screen's long side in pixels (`WindowManager.maximumWindowMetrics`), so
  centre-crop fills it without upscaling. If the rendition fails, fall back to the plain
  `featured_image.file` (what 0.1.x downloads).
- ★: downloads the original (`content/dam`) to a `.part` file in `cacheDir`, writes it to the
  gallery, deletes it. Falls back to `files/apod.jpg` (the rendition) if the original fails.
  It now needs the network and can take a while (10 MB): the star shows it's busy (for example
  dimmed) until done, and a Toast says if it failed. The double-tap guard (`isSaved` inside the
  lock) must still hold while it's downloading.
- `decodeForScreen` and the 8 MP limit stay (a PNG rendition can still be big).
- Test on the phone: today's picture sharper as wallpaper (compare with a 0.1.2 screenshot), ★
  file in the gallery at the original size, ★ with the network off (falls back or says so).

### [ ] 2. Fill / Fit

Decided (user, 2026-10-04): a setting **Fill | Fit** (segmented button, as "Wallpaper on"),
default Fill, today's behaviour.
- Fill: as now, `setStream`, Android centre-crops.
- Fit: decode sampled to the screen size (as `decodeForScreen`), draw it centred on a black
  bitmap of the screen size, `setBitmap(bitmap, null, true, which)`. 1116×2484 ARGB is about
  11 MB, fine. Applies to the APOD and to "my picture"; to home and lock alike.
- Changing it doesn't set the wallpaper again (as "Wallpaper on"); the next change or "Set as
  wallpaper now" uses it.
- Test on the phone: a landscape and a portrait picture, home and lock, both modes.

### [ ] 3. Status line under the daily switch

Decided (user, 2026-10-04): **always shown** while the switch is on.
- Two prefs: `checkedAt` (time of the last successful check: a `refresh()` that worked, or a job
  run that found the stored APOD current) and `wallpaperSetAt` (time `setWallpaper` last
  succeeded, from the job or the button).
- Text: "Wallpaper changed 4 Oct, 06:12" normally; "Couldn't check for a new picture since
  2 Oct" when `checkedAt` is more than a day old (a single failed run then retried a few
  minutes later must not show it). Video day with "keep": still the last change, which is true.
  Before the first change: nothing.
- The page re-reads it on `onResume` and when the job stores a new APOD (as the picture's
  redraw).
- Covers a broken endpoint, an app that Android has stopped (5), and no network for days.
- Test: fake `checkedAt` two days back with `run-as sed` (skill, Fake an old day).

### [ ] 4. Wi-Fi only

Decided (user, 2026-10-04): a switch, **default off** (today's behaviour).
- On: the job uses `NETWORK_TYPE_UNMETERED` instead of `NETWORK_TYPE_ANY` (`DailyJob.kt`);
  toggling it reschedules the job if it's on. The page and the buttons still use any network
  (the user's own action).
- Answers the mobile-data half of open question 2 (huge pictures) without limiting picture size.
- Test: `dumpsys jobscheduler` shows the constraint; with Wi-Fi off the job waits.

### [x] 5. Keep working when the app isn't opened

Built 2026-10-04 (findings and adb recipes: `keep-running.md`; decisions: `CLAUDE.md`, Next
features). Differs from the plan above it: the button opens App info, not
`createManageUnusedAppRestrictionsIntent` (both settings live there), and battery
"Unrestricted" is checked too.
- `MainActivity.checkPausing()` on each `onResume`: `isIgnoringBatteryOptimizations` and, on
  API 31+, `isAutoRevokeWhitelisted`. The hint (`PauseHint`) names only what's missing.
- "Don't show again" → dialog with what will happen → pref `pauseHintHidden`.
- Pref `daily` (`Store.daily`), and `onResume` schedules the job again when it's missing.
- Tested on the phone (debug build, adb for the two settings): hint with both missing, battery
  only, none (gone); App info opens; dialog Cancel and Hide, hidden after a restart; the
  migration writes `daily=true` from the scheduled job; hibernation (`set-state true`) and
  force stop delete the job, opening the page brings it back with the switch on; switch off
  stays off after a restart. Not tested: the two settings changed in the real Settings
  screens with this build, an R8 build, Android 10–11 (no hibernation check there).

### [ ] 6. Share

Decided (user, 2026-10-04): share **title and page link** with `ACTION_SEND` (text), through
the system share sheet. No `FileProvider`, no picture file.
- Text: `<title>\n<page link>`.
- Where: a text glyph next to the ★ at the bottom right of the picture, also on video days (the
  link works). There's no clear share glyph in Unicode; show the user two or three candidates on
  the phone. Screen-reader label "Share".

### [ ] 7. Image credit

Decided (user, 2026-10-04): show the credit under the date.

Checked on the 100 posts in `private/last100.json`: every post has a
`media-detail-hero__meta-row` whose `<th>` contains "Credit". The label varies: `Credit` 45,
`Credit &amp; Copyright:` 27, `Credit &amp; Copyright` 14, `Credit:` 11, `Credit &amp;  License`
1, `Credit and Copyright:` 1, `Color Credit:` 1 (28 Sep). The value is the `<td>` after it:
4 to 251 characters, names in `<a>` tags; long ones have several parts joined by `<br>` (tags
stripped they run together: "…NSF/AURAImage Processing: …") and sometimes start with
"Image Credit:" or "Video Credit:".
- `:core`: `Apod.credit`: the first meta row whose `<th>` contains "Credit", `<td>` text with
  tags stripped, `<br>` → "; ", entities decoded, whitespace collapsed. Empty if missing (show
  nothing). Tests with made-up rows for each label shape and a `<br>` case; check against all
  100 posts in `private/`.
- Page: one line under the date, at most two lines with an ellipsis; the full text when the
  explanation is open. Show the user on the phone with a short and the longest credit.
- An entry stored by 0.1.x has no credit: show nothing until the next day's post (no refetch;
  see the migration rule in `CLAUDE.md`).

### [ ] 8. Save every picture

Decided (user, 2026-10-04): a switch under the daily switch, **default off**.
- On: after a successful run, the job also saves today's picture (as ★: the original, see 1)
  unless it's already saved or it's a video day. Builds a local APOD archive in
  `Pictures/APODroid/`.
- Info button (user, 2026-10-04): a **small** ⓘ (text glyph) right of the switch's label.
  Tapping it shows a short text: saved pictures are full resolution, and saving every day can
  take a lot of space (originals are 2–10 MB, so about 100–300 MB a month). A small dialog or a
  Toast; show the user. Screen-reader label "About saving". The one exception to "no help texts".
- The job's time limit (about 10 minutes) now covers two downloads; if the original's download
  fails, the wallpaper is still set and the next run tries saving again.

### [ ] 9. Video marker

A ▶ (text glyph) centred on the picture on video days, so it's clear that tapping it opens the
video on the web. Screen-reader label "Play video on the web" (or keep it out of the tree and
let the picture's label say it).

### [ ] 10. Selectable explanation

Wrap the explanation `Text` in `SelectionContainer` so it can be copied. Check that tapping the
title row still toggles it and that selecting text doesn't open the page.

## Left out (user, 2026-10-04)

Previous days, full-screen view, home-screen widget, Quick Settings tile, translations, sharing
the picture file. Reasons in `CLAUDE.md`.

## Not found in the review

Both "rough edges" the review lists (screen-reader labels for ☆/★ and ▾/▴, a failed pick of
"my picture" losing the old one) were already fixed in 0.1.2 (`declutter.md`, pass 2026-10-04).
