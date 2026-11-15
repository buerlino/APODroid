# 0.8.0: a still from the video behind ▶ (Open question 2)

(User, 2026-10-18: "a black tile isn't nice", noted for the future; into 0.8.0, user,
2026-11-15.)

## What's known (checked 2026-10-18 and 2026-11-15)

- The 13 video posts in `private/` (24 Jun to 16 Oct 2026): 11 are NASA's own `<video>` with an
  mp4 on `assets.science.nasa.gov`, no `poster`; 2 are YouTube embeds (`<iframe>`).
- The hero holds `<source src="https://assets.science.nasa.gov/content/dam/…/<name>.mp4"
  type="video/mp4">` inside `<video>` (`private/video-day-2026-10-16.json`, read 2026-11-15).
- That mp4 is still served (2026-11-15): HTTP 200, 25,751,622 bytes, `accept-ranges: bytes`,
  `moov` at the start (2026-10-18), so `MediaMetadataRetriever` should need only the header and
  the first seconds' data. Other files' layout unknown.
- `MediaMetadataRetriever` is Android's (no library, no permission beyond `INTERNET`):
  `setDataSource(url, headers)` streams over HTTP; `getScaledFrameAtTime` (API 27+) gives a
  frame at a size.

## Decided (night defaults unless the user said so)

- **The page only, never the wallpaper** (2026-10-18's idea; a still is often not the subject).
- **NASA's mp4 only;** YouTube (about 1 in 6) keeps the black tile: `img.youtube.com` is a new
  host, not worth it alone (2026-10-18).
- **Dimmed behind ▶** (40% approved by the user, 2026-11-15 night, from VS1's screenshot), the tile's 3:2 shape filled (cropped to the middle), about 40% black over
  it so ▶ stays the thing to tap. Screen readers unchanged (the tile's label as now). No new
  text.
- **Once per post:** the frame about 3 s in (`OPTION_CLOSEST_SYNC`), at the tile's size (the
  page's width, at most 1280 px), saved to `files/video-still.jpg` with its post (date and video
  URL) noted (`videoStill.date`, `videoStill.url`); one try per post (`videoStillTried`, as
  `badUrl`): a failure is logged and the tile stays black until the next post. Off the main
  thread, when the page shows the tile (not the job: the job never needs it). A new post or an
  image day deletes the file.
- **The video's URL** is parsed in `:core` with the post (`Post.videoUrl`, the hero's first
  `<source>`'s or `<video>`'s `src` ending in `.mp4`, null otherwise), stored with the post as
  `<source>.videoUrl` (APOD only; a post stored before 0.8.0 has none: the black tile, no
  migration).

### [ ] VS1. The still behind ▶ (Opus, medium)

Read first: this file's "What's known" and "Decided" (the page only, NASA's mp4 only, dimmed,
once per post, the URL in `:core`), and release-0.8.0.md's "Rules for every 0.8.0 package".
- **`:core`:** `Post.videoUrl: String? = null` (the last field), from the hero (`Post.kt`,
  beside `isVideo`): the first `<source>`'s or `<video>`'s `src` ending in `.mp4`, both quote
  styles. Tests: the made-up heroes in `ApodTest` (~124-133, 183: single-quoted `src='…'`; add
  double-quoted ones), a `<video><source …>`, a YouTube `<iframe>` (null), an image day (null);
  the real ones with `assumeTrue` on `private/video-day-2026-10-16.json` (its
  `<source src="…CRS35-Holland30s1080p.mp4">` sits inside the hero, before `<h1`) and
  `last100.json`'s 11 video posts (`SourcesTest` ~167-170).
- **`Store`:** `putString(source.key("videoUrl"), …)` in `store()` (~750-761), the read in
  `post()` (~729-743). `videoStill(post): File?` (the file when noted for this post),
  `makeVideoStill(post)` (blocking: `MediaMetadataRetriever.setDataSource(url, mapOf("User-Agent"
  to userAgent))`, `getScaledFrameAtTime(3_000_000, OPTION_CLOSEST_SYNC, w, h)`, a JPEG via
  `.part`; `videoStillTried` noted first, the notes in one commit; the retriever released in
  `finally`). Add the `videoStill.*` keys to `Store.watch`'s list (~827-830), so the page
  redraws from the prefs. A post stored before 0.8.0 has no `videoUrl` (black, no migration);
  with D1 the next job run's fetch stores it.
- **The page:** `VideoTile` (MainActivity ~1773-1789) shows the still when there, cropped to
  3:2, about 40% black over it, ▶ over that. The try runs in the companion's process-wide `runs`
  scope (~381, as Set's), not the composable's, so a rotation during it doesn't leave "tried"
  noted with no page to show it. The decoded still is dropped in `onStop` with the pictures
  (O12, ~601-613) and drawn through `PicturePainter` (~2815), never a recycled bitmap.
- **Log:** `Video still: <ms> ms, <w>×<h>` or `Video still: couldn't …`.
- **Device** (rosie, the emulator): fake a video day as SKILL.md's "Fake an old day" says
  (`isVideo` true, today's date) plus the `videoUrl` pref with the 16 Oct mp4; no `run -f`
  after the fake (the job's fetch, since D1, overwrites it): the tile shows the still dimmed
  with ▶; a tap opens the page (the emulator's "No browser" toast is expected); a second open
  uses the file (no log line); a rotation during the first try; a YouTube-only post (no
  `videoUrl`) stays black; airplane mode at the first try: black, logged, no retry until the
  next post; the smoke list. Screenshots for the user.
- **Done when:** the tests and the device pass; `CLAUDE.md` (The app, Video days on the page;
  Open question 2 closed) and `interactions/page.md`'s video leaves.
