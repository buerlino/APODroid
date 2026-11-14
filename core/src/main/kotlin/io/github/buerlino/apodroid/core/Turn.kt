package io.github.buerlino.apodroid.core

import java.time.LocalDate

/**
 * The next of [ticked] after [after] in the list's [order] (APOD → NASA → Earth by default, round
 * to [after] itself) for which [hasPicture]; null when none has one. [after] may be unticked: the
 * search goes over the whole [order] from its place (OR1).
 */
fun nextSource(
    ticked: List<Source>,
    after: Source,
    hasPicture: (Source) -> Boolean,
    order: List<Source> = Source.entries,
): Source? {
    val from = order.indexOf(after)
    return order.indices.map { order[(from + 1 + it) % order.size] }.firstOrNull { it in ticked && hasPicture(it) }
}

/**
 * The turns' order and its anchor (OR1, the list's order; `order-0.7.0.md`): each screen takes
 * its ticked sources in [order], and the change time of [from]'s "New picture at" (the anchor A)
 * is the top's turn: at a change time c the turn is the column's S[(N(c) − N(A)) mod n]
 * ([PictureTime.turn]). Without [from], N(A) = 0: RO1's rotation. The defaults are RO1's exactly.
 */
data class Rotation(val order: List<Source> = Source.entries, val from: LocalDate? = null) {
    init {
        require(order.size == Source.entries.size && order.toSet() == Source.entries.toSet()) { "order $order" }
    }

    /** [column]'s sources in the list's order. */
    fun ordered(column: List<Source>): List<Source> = order.filter { it in column }
}

/**
 * The pref `order` (OR1, Night default 3): a comma list of the sources' names, read tolerantly:
 * unknown names and duplicates dropped, missing sources appended in the default order
 * (APOD → NASA → Earth). Null or empty: the default order.
 */
fun orderOf(pref: String?): List<Source> {
    val named = pref.orEmpty().split(',').mapNotNull { name -> Source.entries.firstOrNull { it.name == name.trim() } }.distinct()
    return named + Source.entries.filter { it !in named }
}
