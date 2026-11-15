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
- **Dimmed behind ▶**, the tile's 3:2 shape filled (cropped to the middle), about 40% black over
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

- **`:core`:** `Post.videoUrl` from the hero (`Post.kt`, beside `isVideo`); tests with the real
  16 Oct post (`SourcesTest`, skipped without `private/`), a made-up `<video><source …>` and a
  made-up YouTube `<iframe>` (null), an image day (null).
- **`Store`:** the pref with the post; `videoStill(post): File?` (the file when noted for this
  post), `makeVideoStill(post)` (blocking: the retriever with the app's User-Agent header, the
  frame, a JPEG via `.part`, the notes in one commit; the try noted first).
- **The page:** `VideoTile` shows the still when there, dimmed, ▶ over it; asks for it once
  per post in the background and redraws when it's there; the pictures-let-go rule of O12
  (`onStop`) covers it.
- **Log:** `Video still: <ms> ms, <w>×<h>` or `Video still: couldn't …`.
- **Tablet** (rosie): fake 16 Oct's video day with the skill's recipe (its post from
  `private/video-day-2026-10-16.json`): the tile shows the still dimmed with ▶; a tap opens the
  page as before; a second open uses the file (no log line); a YouTube post (made up from it,
  the `<video>` swapped for an `<iframe>`) stays black; airplane mode at the first try: black,
  logged, no retry until the next post; the smoke list. Screenshots for the user.
- **Done when:** the tests and the tablet pass; `CLAUDE.md` (The app, Video days on the page;
  Open question 2 closed) and `page.md`'s video leaves.
