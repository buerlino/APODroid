package io.github.buerlino.apodroid.core

/*
 * From 0.5.0's prefs to 0.6.0's (CLAUDE.md, Stack: drop two releases after F-Droid ships past
 * 0.6.0). A one-time write at 0.6.0's first start, not a fallback on each read: home's record is
 * the unprefixed keys, so a read couldn't make it unknown with "Where" = Lock, and columns read
 * from "Where" each time would make a later "Where" change in APOD mode change the grid too.
 */

/** The value of the picture key ([Record.picture]) for the user's own picture; else a [Source]'s name. */
const val OWN_PICTURE = "own"

/** What [migrate] writes (a [String], [Long] or [Boolean] per key) and removes, in one edit. */
data class Migration(val write: Map<String, Any>, val remove: Set<String>)

/**
 * 0.5.0's [old] values (`sources`, `where`, `wallpaperSource`, `wallpaperDate`, `wallpaperUrl`,
 * `wallpaperSetAt`, `turnStartedAt`, the numbers as text; any may be missing) → 0.6.0's keys.
 * [apodVideo]: the stored APOD post of `wallpaperDate` is a video.
 *
 * - `allSources` (the mode switch): on unless `sources` is APOD alone (missing: APOD alone, as
 *   0.5.0 read it). With it on, the columns `home` and `lock`: `sources` in those "Where" names
 *   (Both: both); with it off none (the grid starts from "Where" when the switch first goes on).
 * - The record (home's: the unprefixed keys; lock's: `lock.`), when 0.5.0 noted one: **Both**
 *   copies it to the lock screen, **Home** leaves the lock screen unknown (the app never set
 *   it), **Lock** copies it to the lock screen and clears home's (unknown).
 * - Its owner (`wallpaperSource`, whose turn): APOD in APOD mode, whatever 0.5.0 noted (with
 *   NASA standing in on a video day it noted NASA); with the switch on 0.5.0's (APOD when
 *   missing, as 0.5.0 read it). Its picture (`wallpaperPicture`): [OWN_PICTURE] when 0.5.0
 *   noted APOD with no URL on an APOD video date (the user's own picture counted as APOD's),
 *   else the noted source; none when 0.5.0 had forgotten it (no `wallpaperDate`). Its stamp
 *   (`wallpaperStamp`): `turnStartedAt`. No hold.
 * - A fresh install has none of these: only `allSources` = false, which keeps it from running again.
 */
fun migrate(old: Map<String, String>, apodVideo: Boolean): Migration {
    val names = old["sources"]?.split(',').orEmpty()
    val sources = Source.entries.filter { it.name in names }.ifEmpty { listOf(Source.APOD) }
    val on = sources != listOf(Source.APOD)
    val where = old["where"] ?: "BOTH"
    val home = where != "LOCK"
    val lock = where != "HOME"
    val write = mutableMapOf<String, Any>("allSources" to on)
    val remove = mutableSetOf<String>()
    if (on) {
        val ticked = sources.joinToString(",") { it.name }
        write["home"] = if (home) ticked else ""
        write["lock"] = if (lock) ticked else ""
    }
    val noted = old["wallpaperSource"]?.takeIf { s -> Source.entries.any { it.name == s } }
    val date = old["wallpaperDate"]
    if (noted == null && date == null) return Migration(write, remove)
    val url = old["wallpaperUrl"]?.takeIf { it.isNotEmpty() }
    val source = noted ?: Source.APOD.name
    val record = mutableMapOf<String, Any>("wallpaperSource" to if (on) source else Source.APOD.name)
    if (date != null) {
        record["wallpaperPicture"] = if (source == Source.APOD.name && url == null && apodVideo) OWN_PICTURE else source
        record["wallpaperDate"] = date
        if (url != null) record["wallpaperUrl"] = url
    }
    old["turnStartedAt"]?.toLongOrNull()?.takeIf { it > 0 }?.let { record["wallpaperStamp"] = it }
    old["wallpaperSetAt"]?.toLongOrNull()?.takeIf { it > 0 }?.let { record["wallpaperSetAt"] = it }
    if (lock) record.forEach { (key, value) -> write["lock.$key"] = value }
    if (!home) remove += homeRecordKeys
    else {
        write += record
        // An empty URL (never written by 0.5.0, which removed it) would not match by date.
        if (url == null && "wallpaperUrl" in old) remove += "wallpaperUrl"
    }
    return Migration(write, remove)
}

/** Home's record: 0.5.0's keys and 0.6.0's new ones (the picture, the stamp, the hold). */
val homeRecordKeys = setOf(
    "wallpaperSource", "wallpaperPicture", "wallpaperDate", "wallpaperUrl", "wallpaperStamp", "wallpaperSetAt",
    "hold", "holdPost", "holdOwnerPost",
)
