# APODiOS

An iPhone shortcut that sets NASA's [Astronomy Picture of the Day](https://science.nasa.gov/apod/)
(APOD) as your wallpaper every morning, on the lock screen and the home screen. On video days it
leaves your wallpaper alone.

It's the plain part of [APODroid](../README.md): one source, no settings, no page. iOS lets no
app or website set the wallpaper; only Apple's Shortcuts app can, so APODiOS is a shortcut, not
an app.

## Get it

The iCloud link will be here soon. Until then, build it yourself (below, about 10 minutes).

## Every morning

In Shortcuts: **Automation** → **+** → **Time of Day** → 07:00, **Daily** → **Run Immediately**
(turn off **Notify When Run** if you like) → **Next** → pick **APODiOS**.

APOD comes out at 00:05 US Eastern time (06:05 in Central Europe); pick a time after that where
you live.

## Build it yourself

In Shortcuts tap **+**, name it **APODiOS**, and add these actions in order (search for the word in
bold). Each action takes the result of the one above it, except where it says otherwise.

1. **Get Contents of URL**:
   `https://science.nasa.gov/wp-json/wp/v2/image-article?categories=22766&per_page=1`
2. **Get Item from List**: First Item.
3. **Get Dictionary Value**: key `content.rendered`, in **Item from List** (tap the input →
   Select Variable → tap step 2).
4. **Match Text**: pattern
   `media-detail-hero__media(?:(?!<h1)[\s\S])*?<(?:video|iframe)`
   in the Dictionary Value of step 3.
5. **If** Matches **has any value**: put **Stop This Shortcut** inside it (drag it under "If").
   This skips video days.
6. **Get Dictionary Value**, below "End If": key `featured_image.file`, in **Item from List**
   (Select Variable → step 2).
7. **Text**: the Dictionary Value of step 6 (Select Variable → step 6), then `?h=2868` right after
   it, no space. This asks for a screen-sized picture.
8. **Get Contents of URL**: the Text.
9. **Set Wallpaper Photo** (search "wallpaper"): Lock Screen and Home Screen. Tap **›** and turn
   **Show Preview** off, and **Crop to Subject** if it's there.

Run it once with **▶** and answer the questions with **Always Allow**, so the morning run doesn't
stop at them.

To check the video-day skip: put
`https://science.nasa.gov/wp-json/wp/v2/image-article?categories=22766&per_page=1&before=2026-10-17T00:00:00`
(16 Oct 2026, a video) in step 1 and run it: nothing should change. Then put the first URL back.

## If it doesn't work

- **The wallpaper step fails:** the current wallpaper must be a plain photo, not Astronomy,
  Weather, Photo Shuffle, Live Photo or a colour. Settings → Wallpaper → **Add New Wallpaper** →
  **Photos**, pick any photo, **Set as Wallpaper Pair**, then run it again.
- **"com.apple.extensionKit.errorDomain error 2":** an iOS 18 bug in the wallpaper action, often
  on every second run. Pick that new photo wallpaper in step 9 (tap the grey "Wallpaper"), and
  add a second automation 5 minutes after the first: whichever run works sets the picture.
- **Step 3 or 6 comes back empty:** split the key into two Get Dictionary Value actions
  (`content`, then `rendered`; `featured_image`, then `file`).
- **The same picture as yesterday:** APOD was late or the phone was offline at the time. The next
  morning brings the new one; ▶ fetches it now.

Tried on an iPhone XS Max with iOS 18, run by hand; the morning automation is still being tried.

## Where the picture comes from

The same as APODroid: the newest APOD post from science.nasa.gov's public WordPress API (the
official `api.nasa.gov` returns the NASA logo, so older APOD shortcuts don't work). It's not a
documented API; if NASA changes it, the shortcut stops and a new version will be linked here.

APODiOS is not made by or affiliated with NASA or Apple.
