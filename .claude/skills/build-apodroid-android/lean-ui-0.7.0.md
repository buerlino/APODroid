# 0.7.0: a clearer Setup and leaner texts

(User, 2026-10-29 evening: "it's not very clear that you can press the setup to decollapse the
settings"; "I don't like the many sentence-like texts on the page … more lean … applying this to
everything that is not needed".) This file is the how and the packages; the texts below are
**drafts** until the user picks them in the mockup (MK) and approves them from the tablet's
screenshots. 0.6.0 is in its code freeze, so this is 0.7.0's, built after O1–O3 (they work in
the same file, `MainActivity.kt`) and before O13, O14 and L6, which add or rewrite texts.

## Decisions (user, 2026-10-29 evening)

1. **Setup as a Settings row** ("A: a Settings row (Recommended)"; B, a card, and C, only the
   words, not taken). The same for "Advanced settings".
2. **The text rules in `CLAUDE.md`**, binding for every new or changed text from 0.7.0 on, O and
   L too ("Yes, extend CLAUDE.md (Recommended)"): `CLAUDE.md`'s "English UI, short texts" line,
   extended. This file doesn't repeat them.
3. **A mockup first** ("HTML mockup first (Recommended)"): one private page with the new Setup
   and every text old against new; the user picks there; the packages' tablet screenshots then
   confirm.
4. **The weekday dialog's false sentence before the tag** ("In B25, before the tag
   (Recommended)"): `plan-0.6.0.md`'s B25 drops "On weekends, Friday's pictures come back." and
   `CLAUDE.md`'s paragraph with it. Found by reading the rules (since J1 a screen with turns never
   shows a picture twice; with NASA or Earth alone Friday's picture simply stays), not seen on a
   device: W's first weekend is Sat 31 Oct.

## Why "Setup ▸" doesn't look tappable

`Header` and `Heading` (`MainActivity.kt`) both draw `titleSmall` in `primary` (`tertiary` while
the draft differs): "Setup ▸" looks exactly like the plain heading "Wallpaper" above it, the
only sign a small "▸", and the summary under it reads like a status line. "Advanced settings ▸"
the same.

**The row (decision 1):** like a row in Android's Settings, **no border**:
- "Setup" as the row's title (`bodyLarge`, the normal text colour, not the heading's `primary`),
  the summary under it (`bodyMedium`, `onSurfaceVariant`, as now), at the row's right end the
  glyph "▾" while closed and "▴" while open, larger than the text (about `titleLarge`); the whole
  row tappable with its ripple, at least 56 dp high.
- "▾" and "▴" because the page already uses "▾" (the dropdowns, the title), and it draws
  well on the tablet and the FP6; "⌄"/"⌃" (U+2304, U+2303) aren't in Roboto and may show as
  empty boxes or odd fallback glyphs. Whether "▴" (U+25B4) matches "▾" isn't checked: UI1 looks
  at it on the tablet and on `apodroid-29`.
- While the draft differs: the title in `tertiary` and the state "collapsed, not set yet" for
  screen readers, as today; a button, "expanded"/"collapsed", as today.
- Not changed: Setup closed at a fresh start (2026-10-20); it opens by itself while the draft
  differs.

## The texts, today → draft

What the mockup shows. **Revisits** are texts the user approved before (the date in `CLAUDE.md`);
the mockup puts each beside today's so the user decides again, nothing changes silently.

| Where | Today | Draft | Revisit of |
|---|---|---|---|
| The daily switch | Change wallpaper automatically | Change automatically (the heading above says "Wallpaper") | 2026-10-17 |
| Status line, APOD mode or both screens alike | Wallpaper changed 29 Oct, 18:03 (· NASA) | Changed 29 Oct, 18:03 (· NASA) | 2026-10-18, 20 |
| Status lines per screen | Home screen changed 29 Oct, 18:03 · NASA / Lock screen: video day, wallpaper kept | Home: 29 Oct, 18:03 · NASA / Lock: video day, kept | 2026-10-18, 20 |
| Status line, kept | Video day, wallpaper kept | Video day, kept | 2026-10-20 |
| Setup's summary, APOD mode | APOD on both screens · at 06:00 | APOD · both screens · 06:00 | 2026-10-20 to 24 |
| Setup's summary, equal columns | APOD on both screens, NASA on both screens · every 12 h from 06:00 | APOD, NASA · both screens · 12 h from 06:00 | same |
| Setup's summary, columns differ | APOD on home screen, NASA on lock screen, Earth on both screens · every 6 h from 06:00 | Home: APOD, Earth · Lock: NASA, Earth · 6 h from 06:00 (**a code change**: `setupSummary` groups by source today, this groups by screen) | same |
| Source line (two or more sources) | Astronomy Picture of the Day · 29 October 2026 | (a) as today; (b) 29 October 2026, with the full names moved into the title's menu ("APOD · Astronomy Picture of the Day"): else they'd be left only in the problem box, and "Earth" alone is cryptic to a new user | 2026-10-20, 21 |
| Video notice, "Skip it", APOD mode | This APOD is a video, so your wallpaper stays as it was. Tap ▶ to watch it on NASA's site. To change that, choose another picture for video days below. | This APOD is a video: wallpaper unchanged. Change: Setup › If APOD is a video | 2026-10-20, 24 |
| Video notice, "Skip it", turns | This APOD is a video, so it's skipped. Tap ▶ … To change that, … | This APOD is a video: skipped. Change: Setup › If APOD is a video | same |
| Video notice, stand-in | This APOD is a video, so NASA's Image of the Day takes its place. Tap ▶ … | This APOD is a video: NASA's Image of the Day instead. | same |
| Video notice, stand-in loading | This APOD is a video. NASA's Image of the Day takes its place once it has loaded. Tap ▶ … | This APOD is a video: NASA's Image of the Day instead, once loaded. | same |
| Video notice, my picture | … so your own picture takes its place. Tap ▶ … | This APOD is a video: your picture instead. | same |
| Video notice, my picture missing | … Pick a picture below to use it on video days. | This APOD is a video: wallpaper unchanged (no picture chosen). Change: Setup › If APOD is a video | same |
| Video notice, nothing takes APOD's place (daily off, APOD unticked) | This APOD is a video. Tap ▶ … | This APOD is a video. | same |

"This APOD", not "Today's APOD": the stored post isn't today's local date everywhere (in Los
Angeles' evening it's tomorrow's). "Tap ▶ to watch it on NASA's site" goes: the tile's ▶ says it
(it's on APOD's page only). "Change: Setup › If APOD is a video" keeps the only pointer to the
setting, now inside a closed Setup.

**The problem box.** Its text gets the source's name in front unless it's APOD's with APOD
ticked alone ("Earth Observatory: …", `problemText`). The draft writes the subject once: "NASA's
site" alone, the source's name with the prefix.

| Problem | Today (APOD alone / with the prefix) | Draft (APOD alone / with the prefix) |
|---|---|---|
| Network | Couldn't reach NASA's site (since 27 Oct). Check the connection. / Earth Observatory: Couldn't reach NASA's site … | NASA's site: no connection (since 27 Oct). / Earth Observatory: no connection (since 27 Oct). |
| HTTP | NASA's site answered with an error (503). The app tries again later. / Earth Observatory: NASA's site answered … | NASA's site: error 503. Trying again later. / Earth Observatory: error 503. Trying again later. |
| HTTP, since | NASA's site has answered with an error (503) since 27 Oct. The app tries again later. | NASA's site: error 503 since 27 Oct. Trying again later. / Earth Observatory: error 503 since 27 Oct. … |
| Site changed | NASA's site has changed; nothing new since 27 Oct. An app update may be needed. | NASA's site has changed (nothing new since 27 Oct). An app update may be needed. / Earth Observatory has changed (…). … |
| Storage | Couldn't store the picture. Check the free space. | unchanged (already short; a question such as "storage full?" isn't a fact) |

All 2026-10-18 and 20; revisits.

**The Save ⓘ dialog** (2026-10-20 night; a revisit): "Each new picture is saved to
Pictures/APODroid: full size up to the largest original, else a screen-sized copy. A few hundred
MB a month or more." → "Saves each new picture to Pictures/APODroid: full size up to the
limit, else screen size. A few hundred MB a month or more."

**Unchanged** (already short): the toasts, the chip ("On wallpaper"), "Set as wallpaper", "More
NASA pictures", "Pictures from", "New picture at", "Change every", "If APOD is a video",
"Couldn't load today's picture.", the credit, the Storage problem.

**Not here:**
- **The pause hint and its Hide dialog: O13's** (the pause hint per maker rewrites it anyway;
  one rewrite, lean from the start, Android's and the makers' labels word for word: 13 and 14
  "Pause app activity if unused", 12 "Remove permissions and free up space" (B25), the FP6's 16
  "Manage app if unused"). The mockup still shows a lean draft beside today's, because the user
  approved " (on Optimized, a new picture can come hours late)" on 2026-10-24 and chose "Keep
  today's text" on 2026-10-28 (`on-time-0.7.0.md`): the pick there tells O13.
- **The weekday dialog:** B25 (decision 4).

## Packages

| WP | What | Model, effort | Who, device |
|----|------|---------------|-------------|
| MK | The mockup: Setup's row and every text above, old against new, for the user's picks | Opus, medium | a day session with the user; no device, no code (an HTML artifact) |
| UI1 | Setup and Advanced as Settings rows | Opus, medium | winston, tablet; after O3 |
| UI2 | The daily switch, the status lines, Setup's summary (by screen: code), the source line (or the title's menu) | Opus, medium | winston, tablet; after UI1 |
| UI3 | The video notice, the problem box, the Save ⓘ dialog | Sonnet, medium | winston, tablet; after UI1 |

### [ ] MK. The mockup (Opus, medium)

(By day, with the user; allowed in 0.6.0's freeze: no code, no device.) One private HTML
artifact in the page's dark look at a phone's width (the colours from a tablet screenshot in
`.coord/runs/`), built from this file:
- **Setup:** the page's Wallpaper section with Setup's row closed, open, and closed with a draft
  that differs (`tertiary`); "Advanced settings" the same; today's header beside it.
- **Every row of the tables above**, today's beside the draft, in the box or line it lives in,
  with a pick per row (today's, the draft, or the user's own words), the revisits marked with
  their date; the source line's (a) and (b); the pause hint's lean draft beside today's, for O13.
- The picks kept in the page (the artifact's own storage), so the user can come back; then
  written into this file's tables (one "Picked" column) and the drafts updated.
- **Done when:** the user picked every row; this file says what was picked, with the date.

### [ ] UI1. Setup and Advanced as Settings rows (Opus, medium)

(After O3: O1–O3 clean up `MainActivity.kt` first.) `Header` becomes the row of "The row"
above; "Setup" and "Advanced settings" use it; nothing else changes (the summary's text is
UI2's).
- **Tablet:** screenshots for the user: Setup closed, open, closed with a draft that differs; in
  portrait and landscape (two panes), at font scale 1.0 and 2.0 (the summary wraps under the
  title, the glyph stays at the right); `uiautomator dump`: a button, "collapsed"/"expanded",
  ", not set yet"; "▴" drawn (not a box), also on `apodroid-29` (`emulator.md`).
- **Done when:** built; the tablet passes; the user approved the look from the screenshots;
  `CLAUDE.md`'s Settings paragraph describes the row.

### [ ] UI2. The lines under the switch, Setup's summary, the source line (Opus, medium)

(After UI1.) The texts as picked in MK: the daily switch's label, `statusLines`, `setupSummary`
(grouped by screen when the columns differ: a small code change, the cases as today: APOD mode,
equal columns, columns that differ, the daily switch off, turns or not; 12/24 h as the system
says), and the source line (or, with (b), the full names in the title's menu, its screen
reader labels unchanged).
- **Tablet:** screenshots of each case (APOD mode; the switch on with equal and with different
  columns; a screen kept on a faked video day), portrait and landscape, font scale 2.0.
- **Done when:** built; the texts are MK's picks; `CLAUDE.md` quotes them; the tablet passes.

### [ ] UI3. The video notice, the problem box, the Save ⓘ dialog (Sonnet, medium)

(After UI1.) The texts as picked in MK; `VideoNotice` and `problemText` keep every case they have
today (the stand-in ready, loading, skipped by B22; my picture with and without one; the prefix
rule; "since"). No logic changes.
- **Tablet:** a faked video day in APOD mode and with turns, each video-day choice; offline (the
  network problem, with "since" faked as the skill says); an HTTP problem if one can be faked,
  else listed under Not tested; the Save ⓘ dialog. Screenshots, font scale 2.0.
- **Done when:** built; the texts are MK's picks; `CLAUDE.md` quotes them; the tablet passes.
