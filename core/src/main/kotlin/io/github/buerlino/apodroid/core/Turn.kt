package io.github.buerlino.apodroid.core

import java.time.Duration
import java.time.Instant

/** More than the job's flex (15 min, raised by Android to 5 % of the period), so a run a little early still ends a turn. */
val turnSlack: Duration = Duration.ofMinutes(30)

/**
 * Whose turn it is with two or more [ticked] sources: [current] (the one on the wallpaper)
 * until its turn, which began at [startedAt] (null: over) and lasts [length], is over, then the
 * next ticked one in order (round to itself) for which [hasPicture]; null when none has one.
 * A [startedAt] after [now] (the clock was set back) counts as over. Periodic jobs may run a little early, hence [turnSlack].
 */
fun nextTurn(
    ticked: List<Source>,
    current: Source,
    startedAt: Instant?,
    length: Duration,
    now: Instant,
    hasPicture: (Source) -> Boolean,
): Source? {
    val over = startedAt == null || startedAt > now || now >= startedAt + length - turnSlack
    if (!over && current in ticked && hasPicture(current)) return current
    val all = Source.entries
    return all.indices.map { all[(current.ordinal + 1 + it) % all.size] }.firstOrNull { it in ticked && hasPicture(it) }
}
