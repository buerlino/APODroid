package io.github.buerlino.apodroid.core

import java.time.LocalDate

/**
 * ⛶'s part of a source's post: the post's [date] and picture [url], where the frame sits
 * ([position], 0–1), whether the wallpaper shows the [whole] picture instead (A3), and whether
 * the picture goes on turned 90° clockwise, [sideways] (0.8.0, SW2, issue #4: its top at the
 * screen's right; the frame then [sidewaysFrame], position 0 the bottom band).
 *
 * Two of them have three values. [sideways] (SW3): true or false as ⛶'s pill noted it (or a drag
 * since 0.9.0, [notedSideways]), null for Auto's turn ([sidewaysOf]). [position] (SA3b): the
 * frame's place when ⛶'s frame was dragged, null when it wasn't, so the stored Size decides
 * ([framePosition]: Auto's subject, else the middle), and only in the orientation noted with it
 * ([positionFor], B32); a part ✓'d only for "Whole picture" or the pill keeps following Centre or
 * Auto; one ✓'d only for "Whole picture" keeps Auto's turn too.
 */
data class Part(val date: LocalDate, val url: String?, val position: Float?, val whole: Boolean, val sideways: Boolean? = null) {
    /** The part as one line for the page's saved state ([partOf] reads it): "date|position|whole|sideways|url", "null" for a value not noted. */
    fun toText(): String = "$date|$position|$whole|$sideways|${url.orEmpty()}"
}

/** [Part.toText]'s line as a part (a position or sideways of "null": not noted); throws when it isn't one. */
fun partOf(text: String): Part {
    val (date, position, whole, sideways, url) = text.split('|', limit = 5)
    return Part(
        LocalDate.parse(date),
        url.ifEmpty { null },
        if (position == "null") null else position.toFloat(),
        whole.toBooleanStrict(),
        if (sideways == "null") null else sideways.toBooleanStrict(),
    )
}

/**
 * [post]'s part: its source's, when noted for this very post (date and picture URL, so a new
 * post drops it); null for none, which is the stored Size's frame and not whole. The one lookup
 * for the job's set and the page's draft and highlight.
 */
fun Map<Source, Part>.forPost(post: Post): Part? = this[post.source]?.takeIf { post.isNoted(it.date, it.url) }

/**
 * The part's position for the orientation the picture goes on in, [sideways] (B32, 2026-11-22): a
 * position belongs to the orientation noted with it, so in the other one it's null (Size and the
 * subject, [framePosition]); a part from before 0.9.0, a position without an orientation noted,
 * keeps its number in both (no migration). The one reading for ⛶'s frame, ✓ and the sets.
 */
fun Part.positionFor(sideways: Boolean): Float? = position?.takeIf { this.sideways == null || this.sideways == sideways }

/**
 * The position ⛶'s ✓ notes (SA3b): the frame's [dragged] place when it was [moved] since ⛶
 * opened, else the [previous] part's one for the orientation the part goes on in, [sideways]
 * ([positionFor], B32: null, not dragged or noted with the other orientation, stays null).
 */
fun notedPosition(moved: Boolean, dragged: Float, previous: Part?, sideways: Boolean): Float? =
    if (moved) dragged else previous?.positionFor(sideways)

/**
 * The orientation ⛶'s ✓ notes (0.9.0, sharp-0.9.0.md, The rules: ✓ notes the orientation with a
 * dragged position): the pill's [shown] value when it was [touched] or the frame was [moved] since
 * ⛶ opened (a drag belongs to the orientation it was made in, so a later change of Size or of
 * NASA's text can't move the noted position to the other axis), else the part's [previous] one
 * (null, Auto's turn, stays null).
 */
fun notedSideways(touched: Boolean, moved: Boolean, shown: Boolean, previous: Boolean?): Boolean? =
    if (touched || moved) shown else previous
