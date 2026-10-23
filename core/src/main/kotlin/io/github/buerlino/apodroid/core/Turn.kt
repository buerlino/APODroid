package io.github.buerlino.apodroid.core

/**
 * The next of [ticked] after [after] in order (APOD → NASA → Earth, round to [after] itself)
 * for which [hasPicture]; null when none has one.
 */
fun nextSource(ticked: List<Source>, after: Source, hasPicture: (Source) -> Boolean): Source? {
    val all = Source.entries
    return all.indices.map { all[(after.ordinal + 1 + it) % all.size] }.firstOrNull { it in ticked && hasPicture(it) }
}
