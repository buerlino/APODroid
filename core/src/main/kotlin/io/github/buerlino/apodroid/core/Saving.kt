package io.github.buerlino.apodroid.core

/**
 * The sources in use, in order: the ones ticked on any screen ([columns]), the source of each
 * picture held by hand ([held]), and on an APOD video day ([apodIsVideo]) the stand-in's
 * [standIn] while APOD is ticked or held. What the job fetches and "Save every picture" saves.
 */
fun sourcesInUse(columns: Map<Screen, List<Source>>, held: List<Source>, standIn: Source?, apodIsVideo: Boolean): List<Source> {
    val ticked = columns.values.flatten() + held
    return Source.entries.filter { it in ticked || it == standIn && apodIsVideo && Source.APOD in ticked }
}

/** A post "Save every picture" saves, and whether to try its [original] first or save the stored picture. */
data class ToSave(val post: Post, val original: Boolean)

/**
 * What "Save every picture" saves after a job run: the stored post of each of [inUse], in order
 * (APOD → NASA → Earth), unless it's a video, its picture isn't there, or [saved] notes it (the
 * date and picture URL of the source's last save, as ★'s). The original is tried once per post:
 * not when [tried] holds its picture URL (a run stopped during that download, which would start
 * from zero each time), then the stored picture.
 */
fun toSave(inUse: List<Source>, stored: Stored, saved: Map<Source, Picture.Of>, tried: Map<Source, String>): List<ToSave> =
    Source.entries.filter { it in inUse }.mapNotNull { source ->
        stored.post(source)?.takeIf { post ->
            // On an APOD video day APOD's picture is the stand-in's: that one is saved as its own source's.
            (stored.picture(source) as? Picture.Of)?.source == source &&
                saved[source].let { it == null || !post.isNoted(it.date, it.url) }
        }?.let { ToSave(it, tried[source] != it.imageUrl) }
    }
